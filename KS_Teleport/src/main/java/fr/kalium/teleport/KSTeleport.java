package fr.kalium.teleport;

import fr.kalium.jetons.KSJetons;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * KS_Teleport (cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort », LeKiwi06, 03/10/2026 et
 * 08/10/2026) : serveur Event.
 *
 * - Destinations : son lit (/home bed, par défaut), sa maison du spawn (/spawn, après /maison create dans un de ses
 *   claims de la région du spawn, 500 émeraudes), et les emplacements donnés par le badge de localisation (niveau N :
 *   N emplacements), où l'on enregistre des jetons de localisation.
 * - Une téléportation gratuite par heure ; le badge de téléportation en stocke 1 à 5 de plus, chacune avec son délai
 *   d'une heure. Ensuite : un jeton de téléportation de l'inventaire spécial (sans badge, il relance le délai de la
 *   téléportation gratuite à 1 heure) ; sans jeton : payer le prix d'un jeton (20 émeraudes), puis partir.
 * - Avant de partir : 5 secondes sans bouger, annulées par un dégât. Entre dimensions : permis si la dimension est
 *   ouverte (KS_Dimensions).
 * - Jeton de localisation : craft d'une boussole liée à une magnétite entourée de 8 blocs de cuivre ciré ; clic droit :
 *   enregistre la magnétite dans un emplacement libre (refusé si elle a été cassée). Une magnétite enregistrée par au
 *   moins un joueur est indestructible. Un emplacement libéré ne se réutilise que 15 minutes plus tard.
 * - 1.1.0 (LeKiwi06, 11/10/2026) : un joueur qui se connecte pour la première fois est posé au point d'apparition
 *   du monde exact (/setworldspawn), au lieu du bloc le plus haut de la colonne choisi par le jeu (toit du spawn).
 * - 1.3.0 (LeKiwi06, 13/10/2026) : /setspawn, une boussole liée à une magnétite en main : la boussole est consommée
 *   et le point de /spawn passe au-dessus de cette magnétite (dans un de ses claims de la zone du spawn), qui devient
 *   indestructible. /maison position n'existe plus. Le claim qui porte la maison ne compte plus dans les claims du
 *   joueur (KS_Claim 1.4.0, qui lit maisonDansSonClaim).
 * - 1.4.0 (LeKiwi06, 18/10/2026) : les emplacements sont les cases de localisation de l'inventaire spécial de
 *   KS_Jetons 2.1.0 : un jeton de localisation posé dans une case = une destination ; retiré, il est rendu et la case
 *   attend 24 h (KS_Jetons). Les emplacements enregistrés avant sont repris en jetons posés dans les mêmes cases.
 *   /bed = /home bed.
 */
public final class KSTeleport extends JavaPlugin implements Listener {

    /** Position d'une magnétite. */
    private record Pos(String monde, int x, int y, int z) {
        static Pos de(Block bloc) {
            return new Pos(bloc.getWorld().getName(), bloc.getX(), bloc.getY(), bloc.getZ());
        }
    }

    /** Ce que le plugin retient d'un joueur. */
    private static final class Fiche {
        /** Dates des téléportations gratuites dont le délai court encore (millisecondes). */
        final List<Long> utilisees = new ArrayList<>();
        Location maison;
        /** 1.3.0 : magnétite du point de /spawn placé par /setspawn (null : point de /maison create). */
        Pos magnetiteMaison;
    }

    /** Ce qui paie une téléportation. */
    private enum Moyen { GRATUIT, JETON, ACHAT }

    /** Téléportation qui attend la fin du délai sans bouger. */
    private record Depart(Location destination, Moyen moyen, Location origine, BukkitTask tache) {
    }

    private static final List<Material> CUIVRES = List.of(Material.WAXED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER,
            Material.WAXED_WEATHERED_COPPER, Material.WAXED_OXIDIZED_COPPER);

    private static KSTeleport instance;

    private Lang lang;
    private Gui gui;
    private File fichier;
    private NamespacedKey cleRecette;
    private NamespacedKey cleMonde;
    private NamespacedKey cleX;
    private NamespacedKey cleY;
    private NamespacedKey cleZ;
    private final Map<UUID, Fiche> fiches = new HashMap<>();
    /** 1.4.0 : emplacements lus dans un teleport.yml de la 1.3.0 ou avant, à reprendre dans KS_Jetons au démarrage. */
    private final Map<UUID, TreeMap<Integer, Pos>> anciensEmplacements = new HashMap<>();
    /** Magnétites des jetons de localisation posés (KS_Jetons) -> joueurs qui les ont dans une case. */
    private final Map<Pos, Set<UUID>> magnetites = new HashMap<>();
    /** 1.3.0 : magnétites des points de /spawn (/setspawn) -> joueurs dont c'est le point. */
    private final Map<Pos, Set<UUID>> magnetitesMaison = new HashMap<>();
    private final Map<UUID, Depart> departs = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        cleRecette = new NamespacedKey(this, "jeton_localisation");
        cleMonde = new NamespacedKey(this, "monde");
        cleX = new NamespacedKey(this, "x");
        cleY = new NamespacedKey(this, "y");
        cleZ = new NamespacedKey(this, "z");
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "teleport.yml");
        charger();
        ShapedRecipe recette = new ShapedRecipe(cleRecette, KSJetons.creer(KSJetons.Type.LOCALISATION));
        recette.shape("CCC", "CBC", "CCC");
        recette.setIngredient('C', new RecipeChoice.MaterialChoice(CUIVRES));
        recette.setIngredient('B', Material.COMPASS);
        getServer().addRecipe(recette);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("home").setExecutor(this);
        getCommand("spawn").setExecutor(this);
        getCommand("maison").setExecutor(this);
        getCommand("setspawn").setExecutor(this);
        getCommand("bed").setExecutor(this);
        // 1.4.0 : les emplacements sont les cases de localisation de l'inventaire spécial (KS_Jetons 2.1.0).
        KSJetons.validateurLocalisation(this::refusJeton);
        KSJetons.ecouterLocalisations(joueur -> reconstruireMagnetites());
        reprendreEmplacements();
        reconstruireMagnetites();
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "teleport", lang.c("bouton.nom", "<green><bold>Téléportations"),
                    lang.c("bouton.description", "<gray>Ton lit, ta maison du spawn, tes emplacements"), 41, this::ouvrir);
        }
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        getServer().removeRecipe(cleRecette);
        sauver();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    /** 1.5.0 : dans le monde de test (KS_MondeTest), la fiche du compte de test du joueur. */
    private Fiche fiche(UUID joueur) {
        return fiches.computeIfAbsent(CompteTest.de(joueur), u -> new Fiche());
    }

    /**
     * 1.0.1 (modération : fiche d'un joueur de KS_AntiCheat 1.3.0) : destinations enregistrées de ce joueur, en ligne ou
     * hors ligne, dans l'ordre : « Maison du spawn », puis « Emplacement N » (dessus de la magnétite). Lecture seule ;
     * une destination dont le monde n'est pas chargé est ignorée. Le lit n'est pas ici (il se lit dans Paper).
     */
    public static Map<String, Location> destinations(UUID joueur) {
        Map<String, Location> r = new java.util.LinkedHashMap<>();
        Fiche fiche = instance.fiches.get(joueur);
        if (fiche != null && fiche.maison != null && fiche.maison.isWorldLoaded()) {
            r.put("Maison du spawn", fiche.maison.clone());
        }
        // 1.4.0 : jetons de localisation posés dans les cases de l'inventaire spécial.
        KSJetons.localisations(joueur).forEach((numero, jeton) -> {
            Pos pos = instance.magnetite(jeton);
            World monde = pos == null ? null : Bukkit.getWorld(pos.monde());
            if (monde != null) {
                r.put("Emplacement " + (numero + 1), new Location(monde, pos.x() + 0.5, pos.y() + 1, pos.z() + 0.5));
            }
        });
        return r;
    }

    /**
     * 1.3.0 (KS_Claim 1.4.0) : la maison du spawn de ce joueur, en ligne ou hors ligne, si elle est dans un de ses
     * claims de la zone du spawn (le claim qui la porte ne compte alors plus dans ses claims) ; sinon null.
     */
    public static Location maisonDansSonClaim(UUID joueur) {
        Fiche fiche = instance == null ? null : instance.fiches.get(joueur);
        return fiche != null && fiche.maison != null && fiche.maison.isWorldLoaded()
                && instance.maisonValide(joueur, fiche.maison) ? fiche.maison.clone() : null;
    }

    private long delai() {
        return Math.max(1, getConfig().getLong("delai-minutes", 60)) * 60_000L;
    }

    private long attenteDepart() {
        return Math.max(0, getConfig().getLong("attente-secondes", 5));
    }

    private long prixMaison() {
        return Math.max(0, getConfig().getLong("maison.prix", 500));
    }

    private String region() {
        return getConfig().getString("maison.region", "zone_spawn");
    }

    private static String chrono(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return s >= 3600 ? s / 3600 + " h " + (s % 3600) / 60 + " min" : s / 60 + " min " + s % 60 + " s";
    }

    private String nomMonde(String monde) {
        World w = Bukkit.getWorld(monde);
        if (w == null) {
            return monde;
        }
        return w.getEnvironment() == World.Environment.NETHER ? "Nether"
                : w.getEnvironment() == World.Environment.THE_END ? "End" : "monde normal";
    }

    private Component titre() {
        return t("menu.titre", "<green><bold>Téléportations");
    }

    // ------------------------------------------------------------------ téléportations gratuites

    /** Téléportations gratuites stockables : 1, plus celles du badge de téléportation porté. */
    private static int capacite(UUID joueur) {
        return 1 + KSJetons.valeurBadge(joueur, KSJetons.Badge.TP);
    }

    /** Oublie les téléportations dont le délai est fini ; renvoie celles qui restent disponibles. */
    private int gratuites(UUID joueur) {
        Fiche fiche = fiche(joueur);
        long limite = System.currentTimeMillis() - delai();
        fiche.utilisees.removeIf(date -> date <= limite);
        return Math.max(0, capacite(joueur) - fiche.utilisees.size());
    }

    /** Temps avant la prochaine téléportation gratuite (0 : une est disponible). */
    private long prochaineGratuite(UUID joueur) {
        if (gratuites(joueur) > 0) {
            return 0;
        }
        Fiche fiche = fiche(joueur);
        // La capacité a pu baisser (badge retiré) : il faut que assez de délais finissent.
        List<Long> dates = new ArrayList<>(fiche.utilisees);
        dates.sort(null);
        int aFinir = dates.size() - capacite(joueur) + 1;
        return Math.max(0, dates.get(Math.max(0, Math.min(dates.size() - 1, aFinir - 1))) + delai()
                - System.currentTimeMillis());
    }

    private boolean economie() {
        return getServer().getPluginManager().isPluginEnabled("KS_Economy");
    }

    /** Ce qui paierait une téléportation maintenant : gratuite, sinon un jeton, sinon l'achat d'un jeton ; null : rien. */
    private Moyen moyen(UUID joueur) {
        if (gratuites(joueur) > 0) {
            return Moyen.GRATUIT;
        }
        if (KSJetons.nombre(joueur, KSJetons.Type.TP) > 0) {
            return Moyen.JETON;
        }
        return KSJetons.prix(KSJetons.Type.TP) > 0 && economie() ? Moyen.ACHAT : null;
    }

    /** Fait payer la téléportation au moment de partir. Faux si ce n'est plus possible. */
    private boolean payer(Player joueur, Moyen moyen) {
        UUID id = joueur.getUniqueId();
        Fiche fiche = fiche(id);
        long maintenant = System.currentTimeMillis();
        if (moyen == Moyen.GRATUIT) {
            if (gratuites(id) < 1) {
                return false;
            }
            fiche.utilisees.add(maintenant);
            return true;
        }
        if (moyen == Moyen.JETON) {
            if (!KSJetons.consommer(id, KSJetons.Type.TP, 1)) {
                return false;
            }
        } else {
            long prix = KSJetons.prix(KSJetons.Type.TP);
            if (prix <= 0 || !economie() || !fr.kalium.economy.KSEconomy.debiter(id, prix)) {
                return false;
            }
            getLogger().info(joueur.getName() + " paie " + prix + " points pour une téléportation.");
        }
        // Sans badge, un jeton relance le délai de la téléportation gratuite à 1 heure ; avec un badge, les délais des
        // téléportations stockées ne sont pas touchés.
        if (KSJetons.niveauBadge(id, KSJetons.Badge.TP) == 0) {
            fiche.utilisees.clear();
            fiche.utilisees.add(maintenant);
        }
        return true;
    }

    // ------------------------------------------------------------------ départ

    /** Dimension où l'on ne peut pas aller : ses portails sont fermés dans KS_Dimensions (rester dedans est permis). */
    private boolean dimensionFermee(Player joueur, World destination) {
        if (destination.equals(joueur.getWorld())) {
            return false;
        }
        Plugin dimensions = getServer().getPluginManager().getPlugin("KS_Dimensions");
        if (dimensions == null || !dimensions.isEnabled()) {
            return false;
        }
        return (destination.getEnvironment() == World.Environment.NETHER
                && !dimensions.getConfig().getBoolean("nether-portals-enabled", true))
                || (destination.getEnvironment() == World.Environment.THE_END
                && !dimensions.getConfig().getBoolean("end-portals-enabled", true));
    }

    /** Demande de téléportation : choisit ce qui la paie (confirmation pour un achat), puis lance l'attente. */
    private void demander(Player joueur, Location destination) {
        if (destination == null || destination.getWorld() == null) {
            return;
        }
        if (dimensionFermee(joueur, destination.getWorld())) {
            joueur.sendMessage(t("tp.dimension", "<red>Cette dimension est fermée pour le moment."));
            return;
        }
        UUID id = joueur.getUniqueId();
        Moyen moyen = moyen(id);
        if (moyen == null) {
            joueur.sendMessage(t("tp.attendre", "<red>Prochaine téléportation gratuite dans <temps>. <gray>Un jeton de "
                    + "téléportation permet de partir tout de suite.", "temps", chrono(prochaineGratuite(id))));
            return;
        }
        if (moyen == Moyen.ACHAT) {
            long prix = KSJetons.prix(KSJetons.Type.TP);
            gui.confirm(joueur, titre(), t("tp.payer", "<white>Prochaine téléportation gratuite dans <temps>. Payer "
                            + "<gold><prix> points</gold> pour un jeton de téléportation et partir maintenant ?", "temps",
                    chrono(prochaineGratuite(id)), "prix", prix), p -> lancer(p, destination, Moyen.ACHAT), null);
            return;
        }
        lancer(joueur, destination, moyen);
    }

    private void lancer(Player joueur, Location destination, Moyen moyen) {
        annuler(joueur, null);
        long secondes = attenteDepart();
        if (secondes == 0) {
            partir(joueur, destination, moyen);
            return;
        }
        BukkitTask tache = getServer().getScheduler().runTaskLater(this, () -> {
            departs.remove(joueur.getUniqueId());
            if (joueur.isOnline() && !joueur.isDead()) {
                partir(joueur, destination, moyen);
            }
        }, secondes * 20L);
        departs.put(joueur.getUniqueId(), new Depart(destination, moyen, joueur.getLocation(), tache));
        joueur.sendMessage(t("tp.attente", "<green>Téléportation dans <secondes> secondes : ne bouge pas.", "secondes",
                secondes));
    }

    private void partir(Player joueur, Location destination, Moyen moyen) {
        if (dimensionFermee(joueur, destination.getWorld())) {
            joueur.sendMessage(t("tp.dimension", "<red>Cette dimension est fermée pour le moment."));
            return;
        }
        if (!payer(joueur, moyen)) {
            joueur.sendMessage(moyen == Moyen.ACHAT ? t("tp.solde", "<red>Tu n'as pas assez de points pour un jeton de "
                    + "téléportation.") : t("tp.plus", "<red>Téléportation annulée : ce qui devait la payer n'est plus là."));
            return;
        }
        sauver();
        joueur.teleportAsync(destination).thenAccept(reussi -> {
            if (reussi) {
                joueur.sendMessage(moyen == Moyen.GRATUIT ? t("tp.fait-gratuit", "<green>Téléporté. <gray>Téléportations "
                                + "gratuites restantes : <n>.", "n", gratuites(joueur.getUniqueId()))
                        : t("tp.fait-jeton", "<green>Téléporté avec un jeton de téléportation."));
            }
        });
    }

    private void annuler(Player joueur, Component message) {
        Depart depart = departs.remove(joueur.getUniqueId());
        if (depart != null) {
            depart.tache().cancel();
            if (message != null) {
                joueur.sendMessage(message);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBouger(PlayerMoveEvent event) {
        if (departs.isEmpty()) {
            return;
        }
        Depart depart = departs.get(event.getPlayer().getUniqueId());
        if (depart != null && (!event.getTo().getWorld().equals(depart.origine().getWorld())
                || event.getTo().distanceSquared(depart.origine()) > 0.25)) {
            annuler(event.getPlayer(), t("tp.bouge", "<red>Tu as bougé : téléportation annulée."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDegat(EntityDamageEvent event) {
        if (!departs.isEmpty() && event.getEntity() instanceof Player joueur) {
            annuler(joueur, t("tp.degat", "<red>Tu as pris un dégât : téléportation annulée."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        annuler(event.getPlayer(), null);
    }

    /**
     * 1.1.0 (LeKiwi06, 11/10/2026) : première connexion. Le jeu pose un nouveau joueur sur le bloc le plus haut de
     * la colonne, près du point d'apparition du monde : sur Kixster, le toit du spawn (Y 118 à 123 pour un point en
     * Y 73), d'où des morts de chute. On le pose au point exact, s'il y a la place de s'y tenir.
     */
    @EventHandler
    public void onArrivee(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        if (joueur.hasPlayedBefore()) {
            return;
        }
        Location arrivee = arrivee(joueur);
        if (arrivee != null) {
            joueur.teleport(arrivee);
        }
    }

    /**
     * 1.2.0 (LeKiwi06, 11/10/2026 : « pour le toit du spawn je ne sais pas comment fix le soucis ») : un joueur qui
     * réapparaît sans lit ni ancre (mort, retour de l'End) est posé par le jeu comme un nouveau joueur, sur le bloc le
     * plus haut : il réapparaît maintenant au point d'apparition exact du monde, lui aussi.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onReapparition(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()
                || !event.getRespawnLocation().getWorld().equals(getServer().getWorlds().get(0))) {
            return;
        }
        Location arrivee = arrivee(event.getPlayer());
        if (arrivee != null) {
            event.setRespawnLocation(arrivee);
        }
    }

    /** Le point d'apparition exact du monde principal (centre du bloc), ou null s'il n'y a pas la place de s'y tenir. */
    private Location arrivee(Player joueur) {
        Location point = getServer().getWorlds().get(0).getSpawnLocation();
        Block pieds = point.getBlock();
        if (!pieds.isPassable() || !pieds.getRelative(0, 1, 0).isPassable()) {
            getLogger().warning("Point d'apparition du monde bouché (" + pieds.getX() + " " + pieds.getY() + " "
                    + pieds.getZ() + ") : " + joueur.getName() + " reste là où le jeu l'a posé.");
            return null;
        }
        return new Location(point.getWorld(), pieds.getX() + 0.5, pieds.getY(), pieds.getZ() + 0.5, point.getYaw(),
                point.getPitch());
    }

    // ------------------------------------------------------------------ destinations

    private void versLit(Player joueur) {
        Location lit = joueur.getRespawnLocation();
        if (lit == null) {
            joueur.sendMessage(t("lit.aucun", "<red>Tu n'as pas de lit (ou il a été cassé)."));
            return;
        }
        demander(joueur, lit);
    }

    private boolean maisonValide(UUID joueur, Location lieu) {
        return lieu != null && lieu.getWorld() != null && zones()
                && Zones.dansRegion(lieu, region()) && Zones.dansSonClaim(lieu, joueur);
    }

    private boolean zones() {
        return getServer().getPluginManager().isPluginEnabled("WorldGuard")
                && getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem");
    }

    private void versSpawn(Player joueur) {
        Fiche fiche = fiche(joueur.getUniqueId());
        if (fiche.maison == null) {
            joueur.sendMessage(t("maison.aucune", "<red>Tu n'as pas encore de maison au spawn. <gray>Dans un de tes "
                    + "claims de la zone du spawn : <white>/maison create</white> (<prix> points).", "prix", prixMaison()));
            return;
        }
        if (!maisonValide(joueur.getUniqueId(), fiche.maison)) {
            joueur.sendMessage(t("maison.perdue-setspawn", "<red>Ta maison n'est plus dans un de tes claims de la zone du "
                    + "spawn. <gray>Replace son point avec <white>/setspawn</white>, une boussole liée à une magnétite en "
                    + "main."));
            return;
        }
        Location arrivee = fiche.maison;
        if (fiche.magnetiteMaison != null) {
            // 1.3.0 : point placé par /setspawn : au-dessus de la magnétite, si elle est toujours là.
            arrivee = surMagnetite(joueur, fiche.magnetiteMaison);
            if (arrivee == null) {
                return;
            }
        }
        demander(joueur, arrivee);
    }

    /** Dessus de la magnétite, si elle est toujours là et que la place est libre. */
    private Location surMagnetite(Player joueur, Pos pos) {
        World monde = Bukkit.getWorld(pos.monde());
        if (monde == null) {
            joueur.sendMessage(t("emplacement.monde", "<red>Ce monde n'est pas disponible."));
            return null;
        }
        Block bloc = monde.getBlockAt(pos.x(), pos.y(), pos.z());
        if (bloc.getType() != Material.LODESTONE) {
            joueur.sendMessage(t("emplacement.cassee", "<red>Cette magnétite n'existe plus."));
            return null;
        }
        if (!bloc.getRelative(0, 1, 0).isPassable() || !bloc.getRelative(0, 2, 0).isPassable()) {
            joueur.sendMessage(t("emplacement.bouche", "<red>Il n'y a pas la place d'arriver au-dessus de cette magnétite."));
            return null;
        }
        Location arrivee = bloc.getLocation().add(0.5, 1, 0.5);
        arrivee.setYaw(joueur.getLocation().getYaw());
        arrivee.setPitch(joueur.getLocation().getPitch());
        return arrivee;
    }

    private void versEmplacement(Player joueur, int numero) {
        UUID id = joueur.getUniqueId();
        Pos pos = magnetite(KSJetons.localisations(id).get(numero));
        if (pos == null || numero >= KSJetons.valeurBadge(id, KSJetons.Badge.LOCALISATION)) {
            ouvrir(joueur);
            return;
        }
        Location arrivee = surMagnetite(joueur, pos);
        if (arrivee != null) {
            demander(joueur, arrivee);
        }
    }

    // ------------------------------------------------------------------ commandes et menu

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        switch (command.getName()) {
            case "spawn" -> versSpawn(joueur);
            case "maison" -> maison(joueur, args.length == 0 ? "" : args[0].toLowerCase());
            case "setspawn" -> placerSpawn(joueur);
            // 1.4.0 (LeKiwi06, 18/10/2026) : /bed = /home bed.
            case "bed" -> versLit(joueur);
            default -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("bed")) {
                    versLit(joueur);
                } else {
                    ouvrir(joueur);
                }
            }
        }
        lang.saveIfNeeded();
        return true;
    }

    /** /home : ce que le joueur peut utiliser pour partir, et ses destinations. */
    public void ouvrir(Player joueur) {
        UUID id = joueur.getUniqueId();
        Fiche fiche = fiche(id);
        List<Component> corps = new ArrayList<>();
        int gratuites = gratuites(id);
        corps.add(gratuites > 0
                ? t("menu.gratuites", "<gray>Téléportations gratuites : <white><n> sur <max>", "n", gratuites, "max",
                capacite(id))
                : t("menu.gratuites-attente", "<gray>Téléportations gratuites : <white>0 sur <max></white>, la prochaine "
                + "dans <white><temps>", "max", capacite(id), "temps", chrono(prochaineGratuite(id))));
        long prix = KSJetons.prix(KSJetons.Type.TP);
        corps.add(t("menu.jetons", "<aqua>Jetons de téléportation</aqua> <gray>: <white><n></white><prix>", "n",
                KSJetons.nombre(id, KSJetons.Type.TP), "prix", prix > 0 ? " (sans jeton : " + prix + " points)" : ""));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("menu.lit", "<white>Mon lit"), null, this::versLit));
        boutons.add(gui.button(t("menu.spawn", "<white>Ma maison du spawn"), null, this::versSpawn));
        // 1.4.0 : les emplacements sont les cases de localisation de l'inventaire spécial (KS_Jetons 2.1.0).
        int emplacements = KSJetons.valeurBadge(id, KSJetons.Badge.LOCALISATION);
        Map<Integer, ItemStack> poses = KSJetons.localisations(id);
        if (emplacements == 0 && poses.isEmpty()) {
            corps.add(t("menu.sans-badge", "<gray>Emplacements : aucun. Un badge de localisation en donne, pour y "
                    + "enregistrer des jetons de localisation."));
        }
        int fin = Math.max(emplacements, poses.isEmpty() ? 0 : ((TreeMap<Integer, ItemStack>) poses).lastKey() + 1);
        for (int i = 0; i < fin; i++) {
            Pos pos = magnetite(poses.get(i));
            int numero = i;
            if (pos == null) {
                long attente = KSJetons.attenteLocalisation(id, i);
                if (i < emplacements) {
                    corps.add(attente > 0
                            ? t("menu.emplacement-attente-24h", "<white><n>. <gray>libre, reprend un jeton dans <temps>",
                            "n", i + 1, "temps", chrono(attente))
                            : t("menu.emplacement-libre", "<white><n>. <gray>libre", "n", i + 1));
                }
                continue;
            }
            boolean actif = i < emplacements;
            corps.add(t(actif ? "menu.emplacement" : "menu.emplacement-inactif", actif
                            ? "<white><n>. <gold><monde></gold> <x> <y> <z>"
                            : "<white><n>. <gold><monde></gold> <x> <y> <z> <red>(niveau du badge trop bas)", "n", i + 1,
                    "monde", nomMonde(pos.monde()), "x", pos.x(), "y", pos.y(), "z", pos.z()));
            if (actif) {
                boutons.add(gui.button(t("menu.aller", "<white>Emplacement <n>", "n", i + 1), null,
                        p -> versEmplacement(p, numero)));
            }
        }
        if (emplacements > 0 || !poses.isEmpty()) {
            corps.add(t("menu.localisations-info", "<gray>Pour ajouter ou libérer un emplacement, pose ou reprends son "
                    + "jeton de localisation dans tes localisations (/jetons) ; une case vidée attend <heures> h.",
                    "heures", KSJetons.delaiLocalisationMs() / 3_600_000L));
            boutons.add(gui.button(t("menu.bouton-localisations", "<aqua>Mes localisations"), null,
                    KSJetons::ouvrirSpecial));
        }
        gui.open(joueur, titre(), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ maison du spawn

    private void maison(Player joueur, String action) {
        UUID id = joueur.getUniqueId();
        Fiche fiche = fiche(id);
        // 1.3.0 : plus de /maison position, le point de /spawn se déplace avec /setspawn.
        if (!action.equals("create")) {
            joueur.sendMessage(fiche.maison == null
                    ? t("maison.aide", "<gray>Maison du spawn : dans un de tes claims de la zone du spawn, "
                    + "<white>/maison create</white> (<prix> points). Elle te donne <white>/spawn</white>.", "prix",
                    prixMaison())
                    : t("maison.aide-setspawn", "<gray>Tu as une maison au spawn : <white>/spawn</white> t'y emmène. "
                    + "Pour déplacer ce point : <white>/setspawn</white>, une boussole liée à une magnétite en main "
                    + "(magnétite dans un de tes claims de la zone du spawn)."));
            return;
        }
        if (fiche.maison != null) {
            joueur.sendMessage(t("maison.deja-setspawn", "<red>Tu as déjà une maison au spawn (une seule par joueur). "
                    + "<gray>/setspawn déplace son point de /spawn."));
            return;
        }
        if (!maisonValide(id, joueur.getLocation())) {
            joueur.sendMessage(t("maison.lieu", "<red>Place-toi dans un de tes claims de la zone du spawn."));
            return;
        }
        long prix = prixMaison();
        gui.confirm(joueur, t("maison.titre", "<green><bold>Maison du spawn"), t("maison.confirmer", "<white>Déclarer ta "
                + "maison ici pour <gold><prix> points</gold> ? <gray>/spawn t'y emmènera.", "prix", prix), p -> {
            Fiche f = fiche(p.getUniqueId());
            if (f.maison != null || !maisonValide(p.getUniqueId(), p.getLocation())) {
                p.sendMessage(t("maison.lieu", "<red>Place-toi dans un de tes claims de la zone du spawn."));
                return;
            }
            if (prix > 0 && (!economie() || !fr.kalium.economy.KSEconomy.debiter(p.getUniqueId(), prix))) {
                p.sendMessage(t("maison.solde", "<red>Il te faut <prix> points.", "prix", prix));
                return;
            }
            f.maison = p.getLocation();
            sauver();
            getLogger().info(p.getName() + " déclare sa maison du spawn pour " + prix + " points.");
            p.sendMessage(t("maison.creee-setspawn", "<green>Maison du spawn déclarée : <white>/spawn</white> t'y emmène. "
                    + "<gray>/setspawn, une boussole liée à une magnétite en main, déplace ce point."));
            lang.saveIfNeeded();
        }, null);
    }

    /**
     * 1.3.0 (LeKiwi06, 13/10/2026) : /setspawn, une boussole liée à une magnétite dans la main : la boussole est
     * consommée et le point de /spawn de la maison passe au-dessus de cette magnétite, qui doit être dans un de ses
     * claims de la zone du spawn. Remplace /maison position.
     */
    private void placerSpawn(Player joueur) {
        UUID id = joueur.getUniqueId();
        Fiche fiche = fiche(id);
        if (fiche.maison == null) {
            joueur.sendMessage(t("maison.aucune", "<red>Tu n'as pas encore de maison au spawn. <gray>Dans un de tes "
                    + "claims de la zone du spawn : <white>/maison create</white> (<prix> points).", "prix", prixMaison()));
            return;
        }
        ItemStack main = joueur.getInventory().getItemInMainHand();
        Location cible = main.getType() == Material.COMPASS && main.getItemMeta() instanceof CompassMeta meta
                && meta.hasLodestone() ? meta.getLodestone() : null;
        if (cible == null || cible.getWorld() == null) {
            joueur.sendMessage(t("setspawn.boussole", "<red>Tiens en main une boussole liée à une magnétite. <gray>Clic "
                    + "droit sur la magnétite avec une boussole pour la lier."));
            return;
        }
        // La zone d'abord : le chunk d'une magnétite lointaine n'est pas chargé pour rien.
        if (!maisonValide(id, cible)) {
            joueur.sendMessage(t("setspawn.lieu", "<red>La magnétite de cette boussole n'est pas dans un de tes claims de "
                    + "la zone du spawn."));
            return;
        }
        Block bloc = cible.getBlock();
        if (bloc.getType() != Material.LODESTONE) {
            joueur.sendMessage(t("setspawn.cassee", "<red>La magnétite de cette boussole n'existe plus."));
            return;
        }
        Pos pos = Pos.de(bloc);
        if (pos.equals(fiche.magnetiteMaison)) {
            joueur.sendMessage(t("setspawn.deja", "<red>Ton point de /spawn est déjà au-dessus de cette magnétite."));
            return;
        }
        main.setAmount(main.getAmount() - 1);
        joueur.getInventory().setItemInMainHand(main.getAmount() > 0 ? main : null);
        oublierMagnetiteMaison(id, fiche);
        fiche.magnetiteMaison = pos;
        magnetitesMaison.computeIfAbsent(pos, p -> new HashSet<>()).add(id);
        fiche.maison = bloc.getLocation().add(0.5, 1, 0.5);
        fiche.maison.setYaw(joueur.getLocation().getYaw());
        fiche.maison.setPitch(joueur.getLocation().getPitch());
        sauver();
        getLogger().info(joueur.getName() + " place son point de /spawn au-dessus de la magnétite " + pos.x() + " "
                + pos.y() + " " + pos.z() + ".");
        joueur.sendMessage(t("setspawn.fait", "<green>Ton point de /spawn est maintenant au-dessus de la magnétite "
                + "<white><x> <y> <z></white>. <gray>La boussole est utilisée ; la magnétite est indestructible tant "
                + "qu'elle sert de point de /spawn.", "x", pos.x(), "y", pos.y(), "z", pos.z()));
    }

    /** La magnétite du point de /spawn de ce joueur ne l'est plus (elle redevient cassable si personne d'autre ne l'a). */
    private void oublierMagnetiteMaison(UUID joueur, Fiche fiche) {
        if (fiche.magnetiteMaison == null) {
            return;
        }
        Set<UUID> joueurs = magnetitesMaison.get(fiche.magnetiteMaison);
        if (joueurs != null) {
            joueurs.remove(joueur);
            if (joueurs.isEmpty()) {
                magnetitesMaison.remove(fiche.magnetiteMaison);
            }
        }
        fiche.magnetiteMaison = null;
    }

    // ------------------------------------------------------------------ jeton de localisation

    /** Magnétite d'un jeton de localisation lié, ou null. */
    private Pos magnetite(ItemStack objet) {
        if (KSJetons.type(objet) != KSJetons.Type.LOCALISATION) {
            return null;
        }
        PersistentDataContainer pdc = objet.getItemMeta().getPersistentDataContainer();
        String monde = pdc.get(cleMonde, PersistentDataType.STRING);
        Integer x = pdc.get(cleX, PersistentDataType.INTEGER);
        Integer y = pdc.get(cleY, PersistentDataType.INTEGER);
        Integer z = pdc.get(cleZ, PersistentDataType.INTEGER);
        return monde == null || x == null || y == null || z == null ? null : new Pos(monde, x, y, z);
    }

    private ItemStack jeton(Pos pos) {
        ItemStack objet = KSJetons.creer(KSJetons.Type.LOCALISATION);
        ItemMeta meta = objet.getItemMeta();
        List<Component> description = new ArrayList<>(meta.lore());
        description.add(0, Component.text("Magnétite : " + nomMonde(pos.monde()) + " " + pos.x() + " " + pos.y() + " "
                + pos.z(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(description);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(cleMonde, PersistentDataType.STRING, pos.monde());
        pdc.set(cleX, PersistentDataType.INTEGER, pos.x());
        pdc.set(cleY, PersistentDataType.INTEGER, pos.y());
        pdc.set(cleZ, PersistentDataType.INTEGER, pos.z());
        objet.setItemMeta(meta);
        return objet;
    }

    /** Craft : le résultat porte la magnétite de la boussole ; boussole non liée ou magnétite cassée : rien. */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe recette) || !recette.getKey().equals(cleRecette)) {
            return;
        }
        ItemStack resultat = null;
        for (ItemStack objet : event.getInventory().getMatrix()) {
            if (objet != null && objet.getType() == Material.COMPASS && objet.getItemMeta() instanceof CompassMeta meta
                    && meta.hasLodestone() && meta.getLodestone() != null && meta.getLodestone().getWorld() != null) {
                Block bloc = meta.getLodestone().getBlock();
                if (bloc.getType() == Material.LODESTONE) {
                    resultat = jeton(Pos.de(bloc));
                }
            }
        }
        event.getInventory().setResult(resultat);
    }

    /**
     * 1.4.0 : clic droit avec un jeton de localisation : il ne s'utilise plus en main, il se pose dans une case de
     * localisation de l'inventaire spécial (KS_Jetons 2.1.0), qui s'ouvre.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJeton(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || KSJetons.type(event.getItem()) != KSJetons.Type.LOCALISATION) {
            return;
        }
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        joueur.sendMessage(t("jeton.poser", "<gray>Pose ce jeton dans une case de <white>tes localisations</white> : sa "
                + "magnétite deviendra une destination de /home."));
        KSJetons.ouvrirSpecial(joueur);
        lang.saveIfNeeded();
    }

    /**
     * 1.4.0 : vérification de KS_Jetons quand un joueur pose un jeton de localisation dans une case : jeton lié,
     * magnétite toujours là, pas déjà dans une autre de ses cases. Renvoie le refus, ou null.
     */
    private Component refusJeton(Player joueur, ItemStack objet) {
        Pos pos = magnetite(objet);
        if (pos == null) {
            return t("jeton.non-lie", "<red>Ce jeton n'est lié à aucune magnétite.");
        }
        World monde = Bukkit.getWorld(pos.monde());
        if (monde == null || monde.getBlockAt(pos.x(), pos.y(), pos.z()).getType() != Material.LODESTONE) {
            return t("jeton.cassee", "<red>La magnétite de ce jeton a été cassée : il ne peut plus servir.");
        }
        for (ItemStack pose : KSJetons.localisations(joueur.getUniqueId()).values()) {
            if (pos.equals(magnetite(pose))) {
                return t("jeton.deja", "<red>Cette magnétite est déjà dans un de tes emplacements.");
            }
        }
        return null;
    }

    /** 1.4.0 : magnétites protégées = celles des jetons posés dans les cases de localisation de tous les joueurs. */
    private void reconstruireMagnetites() {
        magnetites.clear();
        KSJetons.toutesLocalisations().forEach((joueur, cases) -> cases.values().forEach(jeton -> {
            Pos pos = magnetite(jeton);
            if (pos != null) {
                magnetites.computeIfAbsent(pos, p -> new HashSet<>()).add(joueur);
            }
        }));
    }

    /**
     * 1.4.0 : les emplacements de la 1.3.0 et avant (jeton utilisé, magnétite retenue ici) deviennent des jetons posés
     * dans les mêmes cases de l'inventaire spécial ; une case déjà prise : le jeton est rendu au joueur.
     */
    private void reprendreEmplacements() {
        if (anciensEmplacements.isEmpty()) {
            return;
        }
        int poses = 0;
        int rendus = 0;
        for (Map.Entry<UUID, TreeMap<Integer, Pos>> e : anciensEmplacements.entrySet()) {
            for (Map.Entry<Integer, Pos> emplacement : e.getValue().entrySet()) {
                ItemStack jeton = jeton(emplacement.getValue());
                if (KSJetons.poserLocalisation(e.getKey(), emplacement.getKey(), jeton)) {
                    poses++;
                } else {
                    KSJetons.rendre(e.getKey(), jeton);
                    rendus++;
                }
            }
        }
        anciensEmplacements.clear();
        sauver();
        getLogger().info("Emplacements repris dans l'inventaire spécial : " + poses + " jeton(s) posé(s), " + rendus
                + " rendu(s) à leur joueur.");
    }

    // ------------------------------------------------------------------ magnétites indestructibles

    private boolean enregistree(Block bloc) {
        if (bloc.getType() != Material.LODESTONE || (magnetites.isEmpty() && magnetitesMaison.isEmpty())) {
            return false;
        }
        Pos pos = Pos.de(bloc);
        if (magnetites.containsKey(pos)) {
            return true;
        }
        // 1.3.0 : magnétite d'un point de /spawn, tant qu'elle est dans un claim de son joueur (zone du spawn) : un
        // claim supprimé ou vendu ne garde pas un bloc incassable.
        Set<UUID> joueurs = magnetitesMaison.get(pos);
        if (joueurs != null) {
            for (UUID joueur : joueurs) {
                if (maisonValide(joueur, bloc.getLocation())) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCasser(BlockBreakEvent event) {
        if (enregistree(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(t("magnetite.protegee", "<red>Cette magnétite sert de destination à un joueur : "
                    + "elle est indestructible tant qu'elle est enregistrée."));
            lang.saveIfNeeded();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionBloc(BlockExplodeEvent event) {
        event.blockList().removeIf(this::enregistree);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionEntite(EntityExplodeEvent event) {
        event.blockList().removeIf(this::enregistree);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntite(EntityChangeBlockEvent event) {
        if (enregistree(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonPousse(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::enregistree)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonTire(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::enregistree)) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection section = yaml.getConfigurationSection("joueurs");
        if (section == null) {
            return;
        }
        for (String cle : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(cle);
                ConfigurationSection s = section.getConfigurationSection(cle);
                Fiche fiche = new Fiche();
                fiche.utilisees.addAll(s.getLongList("utilisees"));
                try {
                    fiche.maison = s.getLocation("maison");
                } catch (RuntimeException e) {
                    getLogger().warning("Maison du spawn illisible (monde absent ?) : " + cle);
                }
                ConfigurationSection magnetite = s.getConfigurationSection("maison-magnetite");
                if (magnetite != null && fiche.maison != null) {
                    fiche.magnetiteMaison = new Pos(magnetite.getString("monde", ""), magnetite.getInt("x"),
                            magnetite.getInt("y"), magnetite.getInt("z"));
                    magnetitesMaison.computeIfAbsent(fiche.magnetiteMaison, p -> new HashSet<>()).add(id);
                }
                // 1.3.0 et avant : emplacements retenus ici, repris en jetons posés dans KS_Jetons (reprendreEmplacements).
                ConfigurationSection emplacements = s.getConfigurationSection("emplacements");
                if (emplacements != null) {
                    for (String numero : emplacements.getKeys(false)) {
                        Pos pos = new Pos(emplacements.getString(numero + ".monde", ""), emplacements.getInt(numero + ".x"),
                                emplacements.getInt(numero + ".y"), emplacements.getInt(numero + ".z"));
                        anciensEmplacements.computeIfAbsent(id, u -> new TreeMap<>()).put(Integer.parseInt(numero), pos);
                    }
                }
                fiches.put(id, fiche);
            } catch (RuntimeException e) {
                getLogger().warning("Joueur illisible dans teleport.yml, ignoré : " + cle + " (" + e.getMessage() + ")");
            }
        }
    }

    private void sauver() {
        long limite = System.currentTimeMillis() - delai();
        YamlConfiguration yaml = new YamlConfiguration();
        fiches.forEach((joueur, fiche) -> {
            String chemin = "joueurs." + joueur + ".";
            List<Long> utilisees = new ArrayList<>(fiche.utilisees);
            utilisees.removeIf(date -> date <= limite);
            if (!utilisees.isEmpty()) {
                yaml.set(chemin + "utilisees", utilisees);
            }
            if (fiche.maison != null) {
                yaml.set(chemin + "maison", fiche.maison);
            }
            if (fiche.magnetiteMaison != null) {
                yaml.set(chemin + "maison-magnetite.monde", fiche.magnetiteMaison.monde());
                yaml.set(chemin + "maison-magnetite.x", fiche.magnetiteMaison.x());
                yaml.set(chemin + "maison-magnetite.y", fiche.magnetiteMaison.y());
                yaml.set(chemin + "maison-magnetite.z", fiche.magnetiteMaison.z());
            }
        });
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer teleport.yml : " + e.getMessage());
        }
    }
}
