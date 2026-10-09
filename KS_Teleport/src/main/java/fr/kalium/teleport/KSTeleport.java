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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
        /** Emplacement (0, 1, 2...) -> magnétite enregistrée. */
        final TreeMap<Integer, Pos> emplacements = new TreeMap<>();
        /** Emplacement -> date à laquelle il a été libéré. */
        final Map<Integer, Long> liberes = new HashMap<>();
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
    /** Magnétites enregistrées -> joueurs qui les ont dans un emplacement. */
    private final Map<Pos, Set<UUID>> magnetites = new HashMap<>();
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

    private Fiche fiche(UUID joueur) {
        return fiches.computeIfAbsent(joueur, u -> new Fiche());
    }

    /**
     * 1.0.1 (modération : fiche d'un joueur de KS_AntiCheat 1.3.0) : destinations enregistrées de ce joueur, en ligne ou
     * hors ligne, dans l'ordre : « Maison du spawn », puis « Emplacement N » (dessus de la magnétite). Lecture seule ;
     * une destination dont le monde n'est pas chargé est ignorée. Le lit n'est pas ici (il se lit dans Paper).
     */
    public static Map<String, Location> destinations(UUID joueur) {
        Map<String, Location> r = new java.util.LinkedHashMap<>();
        Fiche fiche = instance.fiches.get(joueur);
        if (fiche == null) {
            return r;
        }
        if (fiche.maison != null && fiche.maison.isWorldLoaded()) {
            r.put("Maison du spawn", fiche.maison.clone());
        }
        fiche.emplacements.forEach((numero, pos) -> {
            World monde = Bukkit.getWorld(pos.monde());
            if (monde != null) {
                r.put("Emplacement " + (numero + 1), new Location(monde, pos.x() + 0.5, pos.y() + 1, pos.z() + 0.5));
            }
        });
        return r;
    }

    private long delai() {
        return Math.max(1, getConfig().getLong("delai-minutes", 60)) * 60_000L;
    }

    private long attenteDepart() {
        return Math.max(0, getConfig().getLong("attente-secondes", 5));
    }

    private long attenteEmplacement() {
        return Math.max(0, getConfig().getLong("emplacement-libere-minutes", 15)) * 60_000L;
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
            joueur.sendMessage(t("maison.perdue", "<red>Ta maison n'est plus dans un de tes claims de la zone du spawn. "
                    + "<gray>Replace-la avec <white>/maison position</white>."));
            return;
        }
        demander(joueur, fiche.maison);
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
        Pos pos = fiche(id).emplacements.get(numero);
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
        int emplacements = KSJetons.valeurBadge(id, KSJetons.Badge.LOCALISATION);
        if (emplacements == 0 && fiche.emplacements.isEmpty()) {
            corps.add(t("menu.sans-badge", "<gray>Emplacements : aucun. Un badge de localisation en donne, pour y "
                    + "enregistrer des jetons de localisation."));
        }
        for (int i = 0; i < Math.max(emplacements, fiche.emplacements.isEmpty() ? 0
                : fiche.emplacements.lastKey() + 1); i++) {
            Pos pos = fiche.emplacements.get(i);
            int numero = i;
            if (pos == null) {
                long attente = fiche.liberes.getOrDefault(i, 0L) + attenteEmplacement() - System.currentTimeMillis();
                if (i < emplacements) {
                    corps.add(attente > 0
                            ? t("menu.emplacement-attente", "<white><n>. <gray>libéré, réutilisable dans <temps>", "n",
                            i + 1, "temps", chrono(attente))
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
            boutons.add(gui.button(t("menu.liberer", "<red>Libérer l'emplacement <n>", "n", i + 1), null,
                    p -> gui.confirm(p, titre(), t("menu.liberer-confirmer", "<white>Libérer l'emplacement <n> ? Le "
                                    + "jeton de localisation n'est pas rendu, et l'emplacement ne pourra resservir que "
                                    + "dans <minutes> minutes.", "n", numero + 1, "minutes",
                            attenteEmplacement() / 60_000L), q -> liberer(q, numero), this::ouvrir)));
        }
        gui.open(joueur, titre(), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ maison du spawn

    private void maison(Player joueur, String action) {
        UUID id = joueur.getUniqueId();
        Fiche fiche = fiche(id);
        boolean creer = action.equals("create");
        if (!creer && !action.equals("position")) {
            joueur.sendMessage(fiche.maison == null
                    ? t("maison.aide", "<gray>Maison du spawn : dans un de tes claims de la zone du spawn, "
                    + "<white>/maison create</white> (<prix> points). Elle te donne <white>/spawn</white>.", "prix",
                    prixMaison())
                    : t("maison.aide-faite", "<gray>Tu as une maison au spawn : <white>/spawn</white> t'y emmène. "
                    + "<white>/maison position</white> la replace là où tu es."));
            return;
        }
        if (creer && fiche.maison != null) {
            joueur.sendMessage(t("maison.deja", "<red>Tu as déjà une maison au spawn. <gray>/maison position la replace "
                    + "là où tu es."));
            return;
        }
        if (!creer && fiche.maison == null) {
            joueur.sendMessage(t("maison.aucune", "<red>Tu n'as pas encore de maison au spawn. <gray>Dans un de tes "
                    + "claims de la zone du spawn : <white>/maison create</white> (<prix> points).", "prix", prixMaison()));
            return;
        }
        if (!maisonValide(id, joueur.getLocation())) {
            joueur.sendMessage(t("maison.lieu", "<red>Place-toi dans un de tes claims de la zone du spawn."));
            return;
        }
        if (!creer) {
            fiche.maison = joueur.getLocation();
            sauver();
            joueur.sendMessage(t("maison.replacee", "<green>Ta maison du spawn est maintenant ici."));
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
            p.sendMessage(t("maison.creee", "<green>Maison du spawn déclarée : <white>/spawn</white> t'y emmène."));
            lang.saveIfNeeded();
        }, null);
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

    /** Clic droit avec un jeton de localisation lié : proposer de l'enregistrer dans un emplacement libre. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJeton(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Pos pos = magnetite(event.getItem());
        if (pos == null) {
            return;
        }
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        if (refusEnregistrement(joueur, pos)) {
            lang.saveIfNeeded();
            return;
        }
        gui.confirm(joueur, titre(), t("jeton.confirmer", "<white>Enregistrer cette magnétite (<gold><monde></gold> <x> "
                        + "<y> <z>) dans un emplacement libre ? <gray>Le jeton est utilisé.", "monde",
                nomMonde(pos.monde()), "x", pos.x(), "y", pos.y(), "z", pos.z()), p -> enregistrer(p, pos), null);
        lang.saveIfNeeded();
    }

    /** Premier emplacement utilisable (donné par le badge, vide, plus en attente après avoir été libéré), ou -1. */
    private int emplacementLibre(UUID joueur) {
        Fiche fiche = fiche(joueur);
        long maintenant = System.currentTimeMillis();
        for (int i = 0; i < KSJetons.valeurBadge(joueur, KSJetons.Badge.LOCALISATION); i++) {
            if (!fiche.emplacements.containsKey(i)
                    && fiche.liberes.getOrDefault(i, 0L) + attenteEmplacement() <= maintenant) {
                return i;
            }
        }
        return -1;
    }

    /** Envoie la raison du refus et renvoie vrai si le jeton ne peut pas être enregistré maintenant. */
    private boolean refusEnregistrement(Player joueur, Pos pos) {
        UUID id = joueur.getUniqueId();
        World monde = Bukkit.getWorld(pos.monde());
        if (monde == null || monde.getBlockAt(pos.x(), pos.y(), pos.z()).getType() != Material.LODESTONE) {
            joueur.sendMessage(t("jeton.cassee", "<red>La magnétite de ce jeton a été cassée : il ne peut plus servir."));
            return true;
        }
        if (fiche(id).emplacements.containsValue(pos)) {
            joueur.sendMessage(t("jeton.deja", "<red>Cette magnétite est déjà dans un de tes emplacements."));
            return true;
        }
        if (KSJetons.valeurBadge(id, KSJetons.Badge.LOCALISATION) == 0) {
            joueur.sendMessage(t("jeton.sans-badge", "<red>Il te faut un badge de localisation (rangé dans tes badges) "
                    + "pour avoir des emplacements."));
            return true;
        }
        if (emplacementLibre(id) < 0) {
            joueur.sendMessage(t("jeton.plein", "<red>Aucun emplacement libre pour le moment. <gray>/home"));
            return true;
        }
        return false;
    }

    private void enregistrer(Player joueur, Pos pos) {
        UUID id = joueur.getUniqueId();
        ItemStack main = joueur.getInventory().getItemInMainHand();
        if (!pos.equals(magnetite(main)) || refusEnregistrement(joueur, pos)) {
            lang.saveIfNeeded();
            return;
        }
        int numero = emplacementLibre(id);
        main.setAmount(main.getAmount() - 1);
        joueur.getInventory().setItemInMainHand(main.getAmount() > 0 ? main : null);
        Fiche fiche = fiche(id);
        fiche.emplacements.put(numero, pos);
        fiche.liberes.remove(numero);
        magnetites.computeIfAbsent(pos, p -> new HashSet<>()).add(id);
        sauver();
        joueur.sendMessage(t("jeton.enregistre", "<green>Magnétite enregistrée dans l'emplacement <n>. <gray>/home pour "
                + "t'y téléporter.", "n", numero + 1));
        lang.saveIfNeeded();
    }

    private void liberer(Player joueur, int numero) {
        UUID id = joueur.getUniqueId();
        Fiche fiche = fiche(id);
        Pos pos = fiche.emplacements.remove(numero);
        if (pos != null) {
            fiche.liberes.put(numero, System.currentTimeMillis());
            Set<UUID> joueurs = magnetites.get(pos);
            if (joueurs != null) {
                joueurs.remove(id);
                if (joueurs.isEmpty()) {
                    magnetites.remove(pos);
                }
            }
            sauver();
        }
        ouvrir(joueur);
    }

    // ------------------------------------------------------------------ magnétites indestructibles

    private boolean enregistree(Block bloc) {
        return !magnetites.isEmpty() && bloc.getType() == Material.LODESTONE && magnetites.containsKey(Pos.de(bloc));
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
                ConfigurationSection emplacements = s.getConfigurationSection("emplacements");
                if (emplacements != null) {
                    for (String numero : emplacements.getKeys(false)) {
                        Pos pos = new Pos(emplacements.getString(numero + ".monde", ""), emplacements.getInt(numero + ".x"),
                                emplacements.getInt(numero + ".y"), emplacements.getInt(numero + ".z"));
                        fiche.emplacements.put(Integer.parseInt(numero), pos);
                        magnetites.computeIfAbsent(pos, p -> new HashSet<>()).add(id);
                    }
                }
                ConfigurationSection liberes = s.getConfigurationSection("liberes");
                if (liberes != null) {
                    for (String numero : liberes.getKeys(false)) {
                        fiche.liberes.put(Integer.parseInt(numero), liberes.getLong(numero));
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
        long limiteEmplacement = System.currentTimeMillis() - attenteEmplacement();
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
            fiche.emplacements.forEach((numero, pos) -> {
                yaml.set(chemin + "emplacements." + numero + ".monde", pos.monde());
                yaml.set(chemin + "emplacements." + numero + ".x", pos.x());
                yaml.set(chemin + "emplacements." + numero + ".y", pos.y());
                yaml.set(chemin + "emplacements." + numero + ".z", pos.z());
            });
            fiche.liberes.forEach((numero, date) -> {
                if (date > limiteEmplacement) {
                    yaml.set(chemin + "liberes." + numero, date);
                }
            });
        });
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer teleport.yml : " + e.getMessage());
        }
    }
}
