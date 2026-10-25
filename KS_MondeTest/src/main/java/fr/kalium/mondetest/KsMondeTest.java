package fr.kalium.mondetest;

import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * KS_MondeTest 1.0.0 (demande de LeKiwi06, 25/10/2026) : « j'utilisais Event comme serveur test avant, mais je ne l'ai
 * plus. Peux-tu me creer une dimension separee dans Kixster ou je peux tester tout ca en crea, sans risquer de me give
 * par accident des choses sur la survie ? » ; « fais comme pour Kal-Games et Kanvas en separant tout, y compris le mode
 * operateur et le gamemode » ; boutons « Monde de test » / « Retourner sur Kixster » dans le menu des admins.
 *
 * Deux cotes sur Kixster : la survie (« kixster », tous les mondes sauf celui de test) et le monde de test (« test »,
 * plat, cree par le plugin). Meme principe que KLM_Espaces (Kal-Games / Kanvas), mais au changement de monde :
 * - a chaque passage d'un cote a l'autre (quel qu'en soit le moyen), l'etat du cote quitte est sauve et celui du cote
 *   d'arrivee rendu (Etats) ; le cote des donnees en cours est marque dans le joueur (« ks_mondetest:cote ») ;
 * - statut d'operateur separe : de cote test, op seulement pour ceux que LeKiwi06 designe ; de cote survie, le statut
 *   d'avant est rendu ;
 * - joueurs de l'autre cote caches, tchat et messages (arrivee, depart, mort, progres) separes ;
 * - on n'ENTRE dans le monde de test que par /mondetest ou le bouton ; on en sort par n'importe quel moyen ;
 * - dans le monde de test : inventaire garde a la mort, portails et points de reapparition refuses, plugins de valeur
 *   coupes (commandes et coffres de KS_Economy, KS_Jetons, KS_RewardsGUI...) ; une commande qui vise un joueur de
 *   l'autre cote est refusee des deux cotes.
 * Les autres plugins lisent le cote d'un joueur dans sa metadonnee « klm_espace » (comme avec KLM_Espaces) : KLM_Menu
 * 2.12.0 cache ainsi les interfaces des plugins rattaches a la survie (config.yml de KLM_Menu, « espaces-interfaces »).
 *
 * 1.1.0 (LeKiwi06, 25/10/2026 : « il faut vraiment scinder les 2, mis a part pour les plugins qui doivent marcher sur
 * les 2 mondes, mais en faisant la distinction du moi de Kixster et du moi du monde de test ») : « moi de test ». Dans
 * le monde de test, le joueur porte la metadonnee « ks_compte », identifiant de son compte de test (UUID de version 3
 * tire du sien, toujours le meme) : KS_Economy, KS_Jetons, KS_Teleport, KS_RewardsGUI, KS_FairPlay et KS_Fly y rangent
 * ses donnees (solde, jetons, maisons, recompenses, exploration, vol), son compte de Kixster n'est jamais touche ;
 * KLM_Statistiques et KS_Parrainage n'y comptent pas le temps de jeu. Progres, recettes et statistiques vanilla
 * n'avancent pas dans le monde de test.
 */
public final class KsMondeTest extends JavaPlugin implements Listener {

    static final String KIXSTER = "kixster";
    static final String TEST = "test";

    /** Un joueur autorise dans le monde de test, et s'il y est operateur. */
    record Acces(String pseudo, boolean op) {
    }

    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Lang lang;
    private Etats etats;
    private NamespacedKey cleCote;
    private NamespacedKey etoileKsMenu;
    private World monde;
    private final Map<UUID, Acces> acces = new ConcurrentHashMap<>();
    /** Cote des donnees de chaque joueur connecte (lu aussi hors du fil principal, par le tchat). */
    private final Map<UUID, String> coteDe = new ConcurrentHashMap<>();
    /** Joueurs que le plugin fait entrer en ce moment (seule entree permise dans le monde de test). */
    private final Set<UUID> entreesAutorisees = new HashSet<>();
    /** Position quittee lors du dernier changement de cote (pour revenir au meme endroit). */
    private final Map<UUID, Location> departs = new HashMap<>();
    /** Moyen du prochain changement de cote, pour le journal. */
    private final Map<UUID, String> moyens = new ConcurrentHashMap<>();
    private YamlConfiguration registre;
    private File fichierRegistre;
    private File fichierAcces;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        etats = new Etats(this);
        cleCote = new NamespacedKey(this, "cote");
        etoileKsMenu = new NamespacedKey("ks_menu", "objet_menu");
        fichierAcces = new File(getDataFolder(), "acces.yml");
        fichierRegistre = new File(getDataFolder(), "registre.yml");
        chargerAcces();
        registre = YamlConfiguration.loadConfiguration(fichierRegistre);
        creerMonde();
        getServer().getPluginManager().registerEvents(this, this);
        declarerBoutons();
        verifierKlmMenu();
        for (Player joueur : getServer().getOnlinePlayers()) { // rechargement a chaud (deconseille)
            arrivee(joueur);
        }
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        // Rien a sauver : les donnees du cote ou est chaque joueur sont dans ses donnees de joueur (serveur), celles
        // de l'autre cote deja dans etats/.
        entreesAutorisees.clear();
    }

    private void creerMonde() {
        String nom = getConfig().getString("monde", "Kixster Test");
        monde = Bukkit.getWorld(nom);
        if (monde == null) {
            monde = new WorldCreator(nom).type(WorldType.FLAT).generateStructures(false).createWorld();
        }
        if (monde == null) {
            getLogger().severe("Monde de test « " + nom + " » impossible a creer : plugin inactif.");
            return;
        }
        // Inventaire garde a la mort : pas de coffre de mort (KS_CoffreMort), rien envoye dans /rewards.
        monde.setGameRule(GameRules.KEEP_INVENTORY, true);
        int bordure = getConfig().getInt("bordure", 500);
        if (bordure > 0) {
            WorldBorder b = monde.getWorldBorder();
            b.setCenter(monde.getSpawnLocation());
            b.setSize(bordure);
        }
        getLogger().info("Monde de test : " + monde.getName() + " (bordure " + bordure + ", " + acces.size()
                + " joueur(s) autorise(s)).");
    }

    /** KLM_Menu 2.12.0 est necessaire pour cacher dans /menu les interfaces de la survie (KS_Menu, KS_Economy...). */
    private void verifierKlmMenu() {
        Plugin menu = getServer().getPluginManager().getPlugin("KLM_Menu");
        String version = menu == null ? "" : menu.getPluginMeta().getVersion();
        String[] p = version.split("\\.");
        boolean ok = false;
        try {
            ok = Integer.parseInt(p[0]) > 2 || (Integer.parseInt(p[0]) == 2 && Integer.parseInt(p[1]) >= 12);
        } catch (RuntimeException ignored) {
            // version illisible
        }
        if (!ok || menu.getConfig().getConfigurationSection("espaces-interfaces") == null) {
            getLogger().warning("KLM_Menu " + version + " sans « espaces-interfaces » : les interfaces des plugins de la "
                    + "survie restent visibles dans /menu depuis le monde de test (KLM_Menu 2.12.0 et sa cle "
                    + "espaces-interfaces sont necessaires, voir le JOURNAL de KS_MondeTest).");
        }
    }

    // ------------------------------------------------------------------ cotes

    /** Cote d'un monde : le monde de test, ou la survie (tous les autres). */
    String cote(World w) {
        return monde != null && w != null && w.getName().equals(monde.getName()) ? TEST : KIXSTER;
    }

    private boolean enTest(Player joueur) {
        return TEST.equals(cote(joueur.getWorld()));
    }

    /** Cote des donnees en cours du joueur (marque dans le joueur ; sans marque : la survie). */
    private String marque(Player joueur) {
        String c = joueur.getPersistentDataContainer().get(cleCote, PersistentDataType.STRING);
        return TEST.equals(c) ? TEST : KIXSTER;
    }

    /** Cote d'un joueur, connecte (son monde) ou non (registre). */
    private String coteDuJoueur(OfflinePlayer joueur) {
        Player p = joueur.getPlayer();
        if (p != null) {
            return coteDe.getOrDefault(p.getUniqueId(), cote(p.getWorld()));
        }
        return registre.getString(joueur.getUniqueId() + ".cote", KIXSTER);
    }

    private World survie() {
        return Bukkit.getWorlds().get(0);
    }

    private GameMode modeArrivee() {
        try {
            return GameMode.valueOf(getConfig().getString("mode-a-l-arrivee", "CREATIVE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GameMode.CREATIVE;
        }
    }

    private boolean opEnTest(UUID joueur) {
        Acces a = acces.get(joueur);
        return a != null && a.op();
    }

    /**
     * Passage d'un cote a l'autre : etat du cote quitte sauve, etat du cote d'arrivee rendu (inventaire vide la premiere
     * fois), statut d'operateur du cote d'arrivee, puis visibilite et tchat.
     */
    private void basculer(Player joueur, String vers, Location depart, String moyen) {
        UUID id = joueur.getUniqueId();
        String de = marque(joueur);
        if (de.equals(vers)) {
            appliquerCote(joueur);
            return;
        }
        // Rend l'objet tenu au curseur et la grille d'artisanat a l'inventaire ; KS_EC_Extension reecrit son extension.
        joueur.closeInventory();
        if (!etats.sauver(joueur, de, depart) && TEST.equals(vers)) {
            // Jamais l'inventaire de survie dans le monde de test : retour immediat (la marque reste « kixster »).
            joueur.sendMessage(lang.c("erreur-sauvegarde", "<red>Ton inventaire de survie n'a pas pu être mis de côté : "
                    + "entrée annulée."));
            journal(joueur, "entree annulee (sauvegarde impossible)");
            joueur.teleport(depart != null ? depart : survie().getSpawnLocation());
            return;
        }
        if (!etats.rendre(joueur, vers)) {
            Etats.vider(joueur, TEST.equals(vers) ? modeArrivee() : GameMode.SURVIVAL);
        }
        joueur.getPersistentDataContainer().set(cleCote, PersistentDataType.STRING, vers);
        if (TEST.equals(vers)) {
            registre.set(id + ".op-a-rendre", true);
            joueur.setOp(opEnTest(id));
        } else {
            joueur.setOp(etats.op(KIXSTER, id));
            registre.set(id + ".op-a-rendre", null);
        }
        appliquerCote(joueur);
        joueur.saveData();
        journal(joueur, (TEST.equals(vers) ? "entree" : "sortie") + " (" + moyen + ")");
        joueur.sendMessage(TEST.equals(vers)
                ? lang.c("entree", "<green>Monde de test : inventaire, coffre de l'Ender, expérience et statut "
                + "d'opérateur séparés de la survie.")
                : lang.c("sortie", "<green>Retour sur Kixster : ton inventaire de survie t'est rendu."));
    }

    /** 1.1.0 : identifiant du compte de test d'un joueur (« moi de test ») : UUID de version 3, toujours le meme. */
    static UUID compteTest(UUID joueur) {
        return UUID.nameUUIDFromBytes(("ks_mondetest:" + joueur).getBytes(StandardCharsets.UTF_8));
    }

    /** Cote des donnees connu : metadonnee, registre, op du monde de test, visibilite. */
    private void appliquerCote(Player joueur) {
        UUID id = joueur.getUniqueId();
        String c = marque(joueur);
        coteDe.put(id, c);
        joueur.setMetadata("klm_espace", new FixedMetadataValue(this, c));
        if (TEST.equals(c)) {
            joueur.setMetadata("ks_compte", new FixedMetadataValue(this, compteTest(id).toString()));
        } else {
            joueur.removeMetadata("ks_compte", this);
        }
        if (TEST.equals(c)) {
            joueur.setOp(opEnTest(id));
        }
        registre.set(id + ".pseudo", joueur.getName());
        registre.set(id + ".cote", c);
        sauverRegistre();
        for (Player autre : getServer().getOnlinePlayers()) {
            if (autre == joueur) {
                continue;
            }
            if (c.equals(coteDe.getOrDefault(autre.getUniqueId(), KIXSTER))) {
                joueur.showPlayer(this, autre);
                autre.showPlayer(this, joueur);
            } else {
                joueur.hidePlayer(this, autre);
                autre.hidePlayer(this, joueur);
            }
        }
    }

    /** Arrivee (connexion ou rechargement) : donnees du cote de son monde. */
    private void arrivee(Player joueur) {
        UUID id = joueur.getUniqueId();
        String vers = cote(joueur.getWorld());
        String moyen = moyens.remove(id);
        if (!marque(joueur).equals(vers)) {
            basculer(joueur, vers, null, moyen != null ? moyen : "connexion");
            return;
        }
        if (KIXSTER.equals(vers) && registre.getBoolean(id + ".op-a-rendre", false)) {
            // Arret imprevu juste apres une entree : les donnees du joueur sont celles de la survie, pas son statut d'op.
            joueur.setOp(etats.op(KIXSTER, id));
            registre.set(id + ".op-a-rendre", null);
            journal(joueur, "statut d'operateur de survie retabli (arret imprevu du serveur)");
        }
        appliquerCote(joueur);
    }

    // ------------------------------------------------------------------ entree / sortie

    private void entrer(Player joueur, String moyen) {
        UUID id = joueur.getUniqueId();
        if (monde == null) {
            joueur.sendMessage(lang.c("indisponible", "<red>Le monde de test n'est pas disponible."));
            return;
        }
        if (enTest(joueur)) {
            joueur.sendMessage(lang.c("deja-dedans", "<yellow>Tu es déjà dans le monde de test."));
            return;
        }
        if (!acces.containsKey(id)) {
            joueur.sendMessage(lang.c("acces-refuse", "<red>Le monde de test est réservé aux testeurs."));
            journal(joueur, "refus : entree sans acces");
            return;
        }
        Location destination = etats.position(TEST, id);
        if (destination == null || !TEST.equals(cote(destination.getWorld()))) {
            destination = monde.getSpawnLocation().add(0.5, 0, 0.5);
        }
        joueur.closeInventory();
        joueur.leaveVehicle();
        joueur.eject();
        moyens.put(id, moyen);
        entreesAutorisees.add(id);
        try {
            if (!joueur.teleport(destination)) {
                moyens.remove(id);
                joueur.sendMessage(lang.c("teleportation-impossible", "<red>Téléportation impossible pour l'instant."));
            }
        } finally {
            entreesAutorisees.remove(id);
        }
    }

    private void sortir(Player joueur, String moyen) {
        if (!enTest(joueur)) {
            joueur.sendMessage(lang.c("pas-dedans", "<yellow>Tu n'es pas dans le monde de test."));
            return;
        }
        Location destination = etats.position(KIXSTER, joueur.getUniqueId());
        if (destination == null || TEST.equals(cote(destination.getWorld()))) {
            destination = survie().getSpawnLocation().add(0.5, 0, 0.5);
        }
        joueur.closeInventory();
        joueur.leaveVehicle();
        joueur.eject();
        moyens.put(joueur.getUniqueId(), moyen);
        if (!joueur.teleport(destination)) {
            moyens.remove(joueur.getUniqueId());
            joueur.sendMessage(lang.c("teleportation-impossible", "<red>Téléportation impossible pour l'instant."));
        }
    }

    // ------------------------------------------------------------------ boutons du menu (rubrique Moderation)

    private void declarerBoutons() {
        getServer().getServicesManager().register(MenuSection.class, bouton("monde-de-test",
                () -> lang.c("bouton.entrer", "<green><bold>Monde de test"),
                () -> lang.c("bouton.entrer-info", "<gray>Monde en créatif, séparé de la survie."),
                p -> !enTest(p) && acces.containsKey(p.getUniqueId()), p -> entrer(p, "bouton")), this, ServicePriority.Normal);
        getServer().getServicesManager().register(MenuSection.class, bouton("retour-kixster",
                () -> lang.c("bouton.sortir", "<gold><bold>Retourner sur Kixster"),
                () -> lang.c("bouton.sortir-info", "<gray>Retrouver ton inventaire de survie."),
                this::enTest, p -> sortir(p, "bouton")), this, ServicePriority.Normal);
    }

    private MenuSection bouton(String id, java.util.function.Supplier<Component> titre,
                               java.util.function.Supplier<Component> description,
                               java.util.function.Predicate<Player> visible, Consumer<Player> action) {
        Plugin moi = this;
        return new MenuSection() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Plugin owner() {
                return moi;
            }

            @Override
            public Component title() {
                return titre.get();
            }

            @Override
            public Component description() {
                return description.get();
            }

            @Override
            public Material icon() {
                return Material.GRASS_BLOCK;
            }

            @Override
            public Audience audience() {
                return Audience.ADMINS;
            }

            @Override
            public boolean moderation() {
                return true;
            }

            @Override
            public int order() {
                return 1;
            }

            @Override
            public boolean visibleTo(Player player) {
                return visible.test(player);
            }

            @Override
            public void open(Player player, Consumer<Player> back) {
                action.accept(player);
            }
        };
    }

    // ------------------------------------------------------------------ connexion, changement de monde

    /** Hors du fil principal : un joueur sans acces qui s'etait deconnecte dans le monde de test arrive en survie. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        UUID id = event.getConnection().getProfile().getId();
        if (id == null || !TEST.equals(cote(event.getSpawnLocation().getWorld())) || acces.containsKey(id)) {
            return;
        }
        Location position = etats.position(KIXSTER, id);
        if (position == null || TEST.equals(cote(position.getWorld()))) {
            position = survie().getSpawnLocation().add(0.5, 0, 0.5);
        }
        event.setSpawnLocation(position);
        moyens.put(id, "connexion sans acces");
    }

    /** Avant les autres plugins : donnees du bon cote avant qu'ils donnent leurs objets. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        arrivee(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        coteDe.remove(id);
        departs.remove(id);
        moyens.remove(id);
        // 1.1.0 : metadonnees retirees apres les autres plugins (ils sauvent encore le compte de test pendant le depart).
        Player joueur = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            if (!joueur.isOnline()) {
                joueur.removeMetadata("klm_espace", this);
                joueur.removeMetadata("ks_compte", this);
            }
        });
    }

    // ------------------------------------------------------------------ progres, recettes, statistiques (1.1.0)

    /** Rien n'avance dans le monde de test : le moi de Kixster garde ses progres, recettes et statistiques. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCritere(com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent event) {
        if (enTest(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRecette(org.bukkit.event.player.PlayerRecipeDiscoverEvent event) {
        if (enTest(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStatistique(org.bukkit.event.player.PlayerStatisticIncrementEvent event) {
        if (enTest(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Seule entree dans le monde de test : /mondetest ou le bouton (toute autre teleportation vers lui est refusee). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location vers = event.getTo();
        if (vers == null || !TEST.equals(cote(vers.getWorld())) || TEST.equals(cote(event.getFrom().getWorld()))
                || entreesAutorisees.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(lang.c("entree-refusee",
                "<red>On n'entre dans le monde de test que par son bouton (rubrique Modération du menu)."));
        journal(event.getPlayer(), "refus : teleportation vers le monde de test (" + event.getCause() + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportFait(PlayerTeleportEvent event) {
        Location vers = event.getTo();
        if (vers != null && !cote(vers.getWorld()).equals(cote(event.getFrom().getWorld()))) {
            UUID id = event.getPlayer().getUniqueId();
            departs.put(id, event.getFrom().clone());
            moyens.putIfAbsent(id, "teleportation " + event.getCause().name().toLowerCase(Locale.ROOT));
        }
    }

    /** Tout changement de cote (bouton, /spawn, /home, /tp, reapparition...) echange les donnees. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player joueur = event.getPlayer();
        String vers = cote(joueur.getWorld());
        if (vers.equals(cote(event.getFrom()))) {
            return;
        }
        String moyen = moyens.remove(joueur.getUniqueId());
        basculer(joueur, vers, departs.remove(joueur.getUniqueId()), moyen != null ? moyen : "changement de monde");
    }

    // ------------------------------------------------------------------ dans le monde de test

    /** Avant KS_CoffreMort (HIGHEST) : rien ne tombe, aucun coffre de mort, rien vers /rewards. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (enTest(event.getPlayer())) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    /** Mort dans le monde de test : reapparition sur place ; jamais de reapparition dans le monde de test sinon. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (monde == null) {
            return;
        }
        if (enTest(event.getPlayer())) {
            if (!TEST.equals(cote(event.getRespawnLocation().getWorld()))) {
                event.setRespawnLocation(monde.getSpawnLocation().add(0.5, 0, 0.5));
            }
        } else if (TEST.equals(cote(event.getRespawnLocation().getWorld()))) {
            event.setRespawnLocation(survie().getSpawnLocation().add(0.5, 0, 0.5));
        }
    }

    /** Lit, ancre, /spawnpoint dans le monde de test : refuses (sinon une mort en survie y ramenerait). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSetSpawn(PlayerSetSpawnEvent event) {
        Location l = event.getLocation();
        if (l != null && TEST.equals(cote(l.getWorld()))) {
            event.setCancelled(true);
        }
    }

    /** Un portail du monde de test menerait au Nether ou a l'End de la survie (objets lances compris). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (TEST.equals(cote(event.getFrom().getWorld()))) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(lang.c("portail", "<red>Pas de portail dans le monde de test."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (TEST.equals(cote(event.getFrom().getWorld()))) {
            event.setCancelled(true);
        }
    }

    /** Coffres des plugins coupes (depot d'emeraudes, inventaire de jetons, recompenses, invsee...). */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player joueur) || !enTest(joueur)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder(false);
        if (holder == null) {
            return;
        }
        String plugin;
        try {
            plugin = JavaPlugin.getProvidingPlugin(holder.getClass()).getName();
        } catch (RuntimeException e) {
            return; // coffre vanilla
        }
        if (pluginCoupe(plugin)) {
            event.setCancelled(true);
            refuser(joueur, plugin);
        }
    }

    /**
     * L'etoile de KS_Menu s'ouvre meme quand le clic est annule : si KS_Menu est dans « plugins-coupes », son menu est
     * referme aussitot dans le monde de test (1.1.0 : KS_Menu n'y est plus par defaut, il masque lui-meme ses boutons
     * de la survie).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack objet = event.getItem();
        if (objet == null || !objet.hasItemMeta() || !enTest(event.getPlayer())
                || !objet.getItemMeta().getPersistentDataContainer().has(etoileKsMenu) || !pluginCoupe("KS_Menu")) {
            return;
        }
        Player joueur = event.getPlayer();
        joueur.closeDialog();
        joueur.closeInventory();
        Bukkit.getScheduler().runTask(this, () -> joueur.closeDialog());
        refuser(joueur, "KS_Menu");
    }

    private boolean pluginCoupe(String nom) {
        for (String p : getConfig().getStringList("plugins-coupes")) {
            if (p.equalsIgnoreCase(nom)) {
                return true;
            }
        }
        return false;
    }

    private boolean dansListe(String cle, String... noms) {
        for (String c : getConfig().getStringList(cle)) {
            for (String nom : noms) {
                if (c.equalsIgnoreCase(nom)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void refuser(Player joueur, String quoi) {
        joueur.sendMessage(lang.c("coupe", "<red>Coupé dans le monde de test : <white><quoi>", "quoi", quoi));
        journal(joueur, "refus : " + quoi);
    }

    /**
     * Commandes : dans le monde de test, celles des plugins coupes et la liste « commandes-coupees » (op, whitelist...)
     * sont refusees, ainsi que les selecteurs qui atteignent les autres mondes (@a, @e... sans distance) et
     * « execute in ». Des deux cotes : une commande qui nomme un joueur de l'autre cote est refusee (/give, /tp, /msg,
     * /invsee...). Les commandes « commandes-libres » (ex. /global) echappent a ce filtre.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandeTapee(PlayerCommandPreprocessEvent event) {
        Player joueur = event.getPlayer();
        String texte = event.getMessage();
        if (monde == null || !texte.startsWith("/")) {
            return;
        }
        String[] mots = texte.substring(1).trim().split("\\s+");
        String label = mots[0].toLowerCase(Locale.ROOT);
        String nom = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;
        Command commande = Bukkit.getCommandMap().getCommand(label);
        String vrai = commande == null ? nom : commande.getName().toLowerCase(Locale.ROOT);
        if (dansListe("commandes-libres", nom, vrai)) {
            return;
        }
        String cote = coteDe.getOrDefault(joueur.getUniqueId(), cote(joueur.getWorld()));
        if (TEST.equals(cote)) {
            String raison = null;
            if (dansListe("commandes-coupees", nom, vrai)) {
                raison = "/" + nom;
            } else if (commande instanceof PluginIdentifiableCommand pic && pluginCoupe(pic.getPlugin().getName())) {
                raison = pic.getPlugin().getName();
            } else {
                for (int i = 1; i < mots.length && raison == null; i++) {
                    String mot = mots[i].toLowerCase(Locale.ROOT);
                    if (mot.startsWith("@") && mot.length() > 1 && mot.charAt(1) != 's' && !mot.contains("distance=")
                            && !mot.contains("dx=") && !mot.contains("dy=") && !mot.contains("dz=")) {
                        raison = mots[i] + " (vise aussi la survie ; ajoute distance=)";
                    } else if (vrai.equals("execute") && mot.equals("in")) {
                        raison = "execute in";
                    }
                }
            }
            if (raison != null) {
                event.setCancelled(true);
                refuser(joueur, raison);
                return;
            }
        }
        for (int i = 1; i < mots.length; i++) {
            OfflinePlayer cible = Bukkit.getPlayerExact(mots[i]);
            if (cible == null) {
                cible = Bukkit.getOfflinePlayerIfCached(mots[i]);
            }
            if (cible != null && !cible.getUniqueId().equals(joueur.getUniqueId()) && !cote.equals(coteDuJoueur(cible))) {
                event.setCancelled(true);
                joueur.sendMessage(lang.c("autre-cote", "<red><joueur> est de l'autre côté (survie / monde de test) : "
                        + "commande refusée.", "joueur", mots[i]));
                journal(joueur, "refus : " + texte);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ tchat et messages

    /** Tchat du serveur : seulement les joueurs du meme cote (la console voit tout). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String c = coteDe.getOrDefault(event.getPlayer().getUniqueId(), KIXSTER);
        event.viewers().removeIf(lecteur -> lecteur instanceof Player p
                && !c.equals(coteDe.getOrDefault(p.getUniqueId(), KIXSTER)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinMessage(PlayerJoinEvent event) {
        Component message = event.joinMessage();
        event.joinMessage(null);
        diffuser(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuitMessage(PlayerQuitEvent event) {
        Component message = event.quitMessage();
        event.quitMessage(null);
        diffuser(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeathMessage(PlayerDeathEvent event) {
        Component message = event.deathMessage();
        event.deathMessage(null);
        diffuser(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Component message = event.message();
        event.message(null);
        diffuser(event.getPlayer(), message);
    }

    private void diffuser(Player auteur, Component message) {
        if (message == null) {
            return;
        }
        getServer().getConsoleSender().sendMessage(message);
        String c = coteDe.getOrDefault(auteur.getUniqueId(), cote(auteur.getWorld()));
        for (Player lecteur : getServer().getOnlinePlayers()) {
            if (c.equals(coteDe.getOrDefault(lecteur.getUniqueId(), cote(lecteur.getWorld())))) {
                lecteur.sendMessage(message);
            }
        }
    }

    // ------------------------------------------------------------------ commande /mondetest

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player joueur)) {
                sender.sendMessage("/mondetest ajouter <joueur> [op] | retirer <joueur> | liste");
                return true;
            }
            if (enTest(joueur)) {
                sortir(joueur, "commande");
            } else {
                entrer(joueur, "commande");
            }
            return true;
        }
        if (!sender.hasPermission("ksmondetest.admin")) {
            sender.sendMessage(lang.c("admin-seulement", "<red>Réservé aux administrateurs du monde de test."));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("liste")) {
            List<String> noms = new ArrayList<>();
            acces.values().forEach(a -> noms.add(a.pseudo() + (a.op() ? " (op)" : "")));
            sender.sendMessage(lang.c("liste", "<gold>Testeurs (<count>) : <white><noms>", "count", noms.size(),
                    "noms", noms.isEmpty() ? "-" : String.join(", ", noms)));
            return true;
        }
        if ((!action.equals("ajouter") && !action.equals("retirer")) || args.length < 2) {
            sender.sendMessage(lang.c("usage", "<gray>/mondetest ajouter <joueur> [op] | retirer <joueur> | liste"));
            return true;
        }
        OfflinePlayer cible = Bukkit.getPlayerExact(args[1]);
        if (cible == null) {
            cible = Bukkit.getOfflinePlayerIfCached(args[1]);
        }
        if (cible == null) {
            sender.sendMessage(lang.c("inconnu", "<red>Joueur inconnu (jamais venu sur Kixster) : <white><joueur>",
                    "joueur", args[1]));
            return true;
        }
        String pseudo = cible.getName() != null ? cible.getName() : args[1];
        Player enLigne = cible.getPlayer();
        if (action.equals("ajouter")) {
            boolean op = args.length > 2 && args[2].equalsIgnoreCase("op");
            acces.put(cible.getUniqueId(), new Acces(pseudo, op));
            sauverAcces();
            if (enLigne != null && enTest(enLigne)) {
                enLigne.setOp(op);
            }
            sender.sendMessage(lang.c("ajoute", "<green><joueur> a accès au monde de test<op>.", "joueur", pseudo,
                    "op", op ? lang.c("ajoute-op", " (opérateur dans le monde de test)") : Component.empty()));
            getLogger().info(sender.getName() + " : acces au monde de test pour " + pseudo + (op ? " (op)" : ""));
            journalTexte(sender.getName() + " | acces donne a " + pseudo + (op ? " (op)" : ""));
        } else {
            if (acces.remove(cible.getUniqueId()) == null) {
                sender.sendMessage(lang.c("pas-dans-liste", "<yellow><joueur> n'avait pas accès.", "joueur", pseudo));
                return true;
            }
            sauverAcces();
            if (enLigne != null && enTest(enLigne)) {
                sortir(enLigne, "acces retire");
            }
            sender.sendMessage(lang.c("retire", "<green><joueur> n'a plus accès au monde de test.", "joueur", pseudo));
            getLogger().info(sender.getName() + " : acces au monde de test retire a " + pseudo);
            journalTexte(sender.getName() + " | acces retire a " + pseudo);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ksmondetest.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("ajouter", "retirer", "liste");
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("liste")) {
            List<String> noms = new ArrayList<>();
            if (args[0].equalsIgnoreCase("retirer")) {
                acces.values().forEach(a -> noms.add(a.pseudo()));
            } else {
                getServer().getOnlinePlayers().forEach(p -> noms.add(p.getName()));
            }
            return noms;
        }
        return args.length == 3 && args[0].equalsIgnoreCase("ajouter") ? List.of("op") : List.of();
    }

    // ------------------------------------------------------------------ fichiers

    private void chargerAcces() {
        acces.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichierAcces);
        for (String cle : yaml.getKeys(false)) {
            try {
                acces.put(UUID.fromString(cle), new Acces(yaml.getString(cle + ".pseudo", "?"),
                        yaml.getBoolean(cle + ".op", false)));
            } catch (IllegalArgumentException ignored) {
                // ligne illisible : ignoree
            }
        }
    }

    private void sauverAcces() {
        YamlConfiguration yaml = new YamlConfiguration();
        acces.forEach((id, a) -> {
            yaml.set(id + ".pseudo", a.pseudo());
            yaml.set(id + ".op", a.op());
        });
        try {
            getDataFolder().mkdirs();
            yaml.save(fichierAcces);
        } catch (IOException e) {
            getLogger().warning("acces.yml non ecrit : " + e.getMessage());
        }
    }

    private void sauverRegistre() {
        try {
            getDataFolder().mkdirs();
            registre.save(fichierRegistre);
        } catch (IOException e) {
            getLogger().warning("registre.yml non ecrit : " + e.getMessage());
        }
    }

    private void journal(Player joueur, String quoi) {
        journalTexte(joueur.getName() + " | " + quoi);
    }

    /** journal.log : entrees, sorties (et par quel moyen), refus, acces donnes ou retires. */
    private void journalTexte(String ligne) {
        File f = new File(getDataFolder(), "journal.log");
        f.getParentFile().mkdirs();
        try (PrintWriter out = new PrintWriter(new FileWriter(f, StandardCharsets.UTF_8, true))) {
            out.println(LocalDateTime.now(ZoneId.of("Europe/Paris")).format(HORODATAGE) + " | " + ligne);
        } catch (IOException e) {
            getLogger().warning("journal.log non ecrit : " + e.getMessage());
        }
    }
}
