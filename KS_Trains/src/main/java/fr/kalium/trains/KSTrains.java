package fr.kalium.trains;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Display;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Trains (demande de LeKiwi06, 16/10/2026 : « un système pour créer de longs trains en faisant en sorte qu'ils se
 * suivent de façon fluide »). Cahier des charges validé le 16/10/2026 ; étape 1 :
 *
 * - Locomotive : objet (wagonnet à four brillant, recette dans KS_Crafts : 3 wagonnets à four en ligne). Clic droit
 *   sur un rail : 3 wagonnets à four attelés, posés sur 3 rails droits dans le sens du regard.
 * - Atteler : chaîne en main, clic droit sur un wagon puis sur un autre wagon proche, au bout d'un train (ou seul).
 *   Dételer : cisailles sur un wagon (il est séparé de celui de devant), ou dans le menu du train.
 * - Rouler : la tête est poussée le long des rails (physique du jeu) ; chaque autre wagon est placé à chaque tick sur
 *   le trajet de la tête, à `espacement` blocs du précédent. Vitesse maximale : 1 bloc/tick tant que la traction suffit
 *   (fours alimentés : 5 par four de la locomotive, 10 par autre four), 0,1 de moins par wagon en trop.
 * - Carburant : voir Foyers (1 charbon = 80 blocs, case unique, seau de lave).
 * - Menu du train (coffre) : accroupi + clic droit sur un four de la locomotive.
 * - Un train qui roule garde ses chunks chargés (et encore une minute après son arrêt).
 *
 * La composition est enregistrée dans les wagons (identifiant du train, rang, taille) : un train se reforme quand tous
 * ses wagons sont chargés, arrêté.
 *
 * 1.1.0 (étape 2) : wagon contrôleur (recette dans KS_Crafts 1.17.0), un par train, à moins de 5 wagons de la
 * locomotive. Niveaux de vitesse (demande de LeKiwi06) : 0 arrêt, 1 à 4 en avant (5 blocs/s chacun, plafonnés par la
 * vitesse maximale du train, 20 blocs/s), -1 marche arrière (1 bloc/s). Le niveau 2 est la vitesse de croisière :
 * consommation de carburant par bloc parcouru x0,75 au niveau 1, x1 au niveau 2, x1,25 au niveau 3, x1,5 au niveau 4. À bord, un appui sur avancer monte d'un niveau,
 * un appui sur reculer descend d'un niveau (à l'arrêt : marche arrière) ; le train passe d'un niveau à l'autre en
 * accélérant ou freinant en douceur ; le conducteur descend, le train garde son niveau. En marche arrière, le dernier
 * wagon mène et les autres suivent son trajet.
 *
 * 1.2.0 (étape 3) : wagons à entonnoir et réglages entrée / sortie des wagons à coffre (voir Entonnoirs) ; wagon à
 * coffre verrouillé (KS_VerrouVehicule) : attelé et réglé par son propriétaire seulement (ou un opérateur), il n'est
 * plus ancré quand il est dans un train (KS_VerrouVehicule 1.1.0).
 *
 * 1.3.0 (demandes de LeKiwi06 après ses tests) : collisions (un wagonnet, un joueur ou un animal est repoussé par le
 * train ; deux trains qui se heurtent : celui qui roule s'arrête), wagons plus écartés (1,4 bloc) et arrêtés net quand on
 * les dételle, carburant des entonnoirs mieux réparti (Entonnoirs), chargement agrandi pour Java et fumée de la
 * locomotive (Decor), klaxons du wagon contrôleur (Klaxons), wagon jukebox (Jukebox), boussole du train (Boussoles).
 *
 * 1.4.0 (propositions de Claude acceptées par LeKiwi06 le 17/10/2026) : propriétaire du train (celui qui pose la
 * locomotive) et ses amis de KLM_Contacts ont seuls accès au train (les autres montent comme passagers), 64 wagons au
 * plus, un seul train chargé par joueur (le dernier lancé, tant qu'il roule, zone la plus petite possible), boutons
 * niveau / klaxon dans le menu, départ automatique quand le rail propulseur est alimenté, autonomie affichée au
 * conducteur, nom du train (étiquette), démêlage d'un trajet tassé, réglage `decor.actif`.
 */
public final class KSTrains extends JavaPlugin implements Listener {

    private static final double PORTEE_MENU = 24;
    /** 1.4.8 : longueur maximale du nom d'un wagon. */
    private static final int NOM_WAGON_MAX = 32;
    private NamespacedKey cleNomWagon;

    /** 1.1.0 : niveaux de vitesse en avant (1 à 4 ; 4 = 20 blocs/s, la vitesse maximale). */
    private static final int NIVEAUX = 4;

    private static KSTrains instance;

    private NamespacedKey cleLocomotive;
    private NamespacedKey cleTrain;
    private NamespacedKey cleRang;
    private NamespacedKey cleTaille;
    private NamespacedKey cleLoco;
    /** 1.1.0 : objet « Wagon contrôleur », et wagon contrôleur posé. */
    private NamespacedKey cleControleurObjet;
    private NamespacedKey cleControleur;
    private final NamespacedKey cleVerrou = NamespacedKey.fromString("ks_verrouvehicule:proprietaire");
    private Lang lang;
    /** 1.4.8 : fenêtres de saisie (nom d'un wagon). */
    private fr.kalium.menu.api.Gui gui;
    private Foyers foyers;
    private Entonnoirs entonnoirs;
    private Decor decor;
    private Klaxons klaxons;
    private Jukebox jukebox;
    private Boussoles boussoles;
    /** 1.4.0 : propriétaire du train (dans ses fours de locomotive), son pseudo, et nom du train. */
    private NamespacedKey cleProprio;
    private NamespacedKey cleProprioNom;
    private NamespacedKey cleNomTrain;
    private boolean contacts;
    /** 1.4.0 : dernier train mis en route par chaque propriétaire (le seul dont les chunks restent chargés). */
    private final Map<UUID, UUID> dernierLance = new HashMap<>();
    /** 1.3.4 : compteur kilométrique (blocs) et carburant brûlé (blocs de réserve) d'un four de locomotive. */
    private NamespacedKey cleCompteur;
    private NamespacedKey cleBrule;
    /** 1.3.0 : objet « Wagon jukebox », et wagon jukebox posé. */
    private NamespacedKey cleJukeboxObjet;
    private NamespacedKey cleJukebox;
    private long tick;

    private final Map<UUID, Train> trains = new HashMap<>();
    /** Wagon -> train (trains complets seulement). */
    private final Map<UUID, UUID> parWagon = new HashMap<>();
    /** Trains dont tous les wagons ne sont pas chargés : wagons présents par rang. */
    private final Map<UUID, Map<Integer, Minecart>> attente = new HashMap<>();
    /** Premier wagon choisi avec une chaîne, par joueur. */
    private final Map<UUID, Selection> selections = new HashMap<>();

    private record Selection(Minecart wagon, long fin) {
    }

    /** Ce qu'affiche un menu : le train (son identifiant change à chaque attelage ou dételage). */
    private record Marque(UUID train) {
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        cleLocomotive = new NamespacedKey(this, "locomotive");
        cleTrain = new NamespacedKey(this, "train");
        cleRang = new NamespacedKey(this, "rang");
        cleTaille = new NamespacedKey(this, "taille");
        cleLoco = new NamespacedKey(this, "loco");
        cleControleurObjet = new NamespacedKey(this, "controleur-objet");
        cleControleur = new NamespacedKey(this, "controleur");
        cleJukeboxObjet = new NamespacedKey(this, "jukebox-objet");
        cleCompteur = new NamespacedKey(this, "compteur");
        cleProprio = new NamespacedKey(this, "proprietaire");
        cleProprioNom = new NamespacedKey(this, "proprietaire-nom");
        cleNomTrain = new NamespacedKey(this, "nom");
        contacts = getServer().getPluginManager().isPluginEnabled("KLM_Contacts");
        CoffresRenforces.activer(getServer().getPluginManager().isPluginEnabled("KS_CoffreRenforce"));
        cleBrule = new NamespacedKey(this, "brule");
        cleJukebox = new NamespacedKey(this, "jukebox");
        lang = new Lang(this);
        gui = new fr.kalium.menu.api.Gui(this, lang);
        cleNomWagon = new NamespacedKey(this, "nom-wagon");
        foyers = new Foyers(this);
        entonnoirs = new Entonnoirs(this, foyers);
        decor = new Decor(this);
        klaxons = new Klaxons(this);
        jukebox = new Jukebox(this, entonnoirs);
        boussoles = new Boussoles(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(foyers, this);
        getServer().getPluginManager().registerEvents(entonnoirs, this);
        getServer().getPluginManager().registerEvents(klaxons, this);
        for (World monde : Bukkit.getWorlds()) {
            for (Minecart wagon : monde.getEntitiesByClass(Minecart.class)) {
                arriver(wagon);
            }
        }
        getServer().getScheduler().runTaskTimer(this, this::rouler, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, this::surveiller, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::fumer, 4L, 4L);
        getServer().getScheduler().runTaskTimer(this, this::musique, 10L, 10L);
        getServer().getScheduler().runTaskTimer(this, boussoles::mettreAJour, 40L, 40L);
        getServer().getScheduler().runTaskTimer(this, () -> boussoles.enregistrer(false), 1200L, 1200L);
        creerLocomotive();
        creerControleur();
        creerJukebox();
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        foyers.toutFermer();
        klaxons.toutFermer();
        boussoles.enregistrer(true);
        for (World monde : Bukkit.getWorlds()) {
            monde.removePluginChunkTickets(this);
        }
        lang.saveIfNeeded();
    }

    Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private double reglage(String cle, double defaut) {
        return getConfig().getDouble(cle, defaut);
    }

    // ------------------------------------------------------------------ objet

    /** Une locomotive. Nécessite que le plugin soit activé. */
    public static ItemStack creerLocomotive() {
        KSTrains p = instance;
        ItemStack objet = new ItemStack(Material.FURNACE_MINECART);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(p.t("objet.nom", "<white>Locomotive").decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = new ArrayList<>();
        for (Component ligne : List.of(
                p.t("objet.ligne-1", "<gray>3 wagonnets à four attelés"),
                p.t("objet.ligne-2", "<gray>Clic droit sur 3 rails droits,"),
                p.t("objet.ligne-3", "<gray>dans le sens où tu regardes"))) {
            lignes.add(ligne.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lignes);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(p.cleLocomotive, PersistentDataType.BYTE, (byte) 1);
        objet.setItemMeta(meta);
        return objet;
    }

    public static boolean estLocomotive(ItemStack objet) {
        return instance != null && objet != null && objet.getType() == Material.FURNACE_MINECART && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(instance.cleLocomotive, PersistentDataType.BYTE);
    }

    /**
     * 1.1.0 : un wagon contrôleur (wagonnet brillant ; recette dans KS_Crafts : wagonnet au centre et, en forme de
     * casque, comparateur, capteur sculk calibré, lampe à redstone, ampoule de cuivre, levier ; KS_Crafts 1.19.0 : bloc
     * musical au lieu de l'ampoule). Nécessite que le plugin
     * soit activé.
     */
    public static ItemStack creerControleur() {
        KSTrains p = instance;
        ItemStack objet = new ItemStack(Material.MINECART);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(p.t("controleur.nom", "<white>Wagon contrôleur").decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = new ArrayList<>();
        for (Component ligne : List.of(
                p.t("controleur.ligne-1", "<gray>À atteler à moins de 5 wagons"),
                p.t("controleur.ligne-2", "<gray>de la locomotive (un par train)"),
                p.t("controleur.ligne-3", "<gray>À bord : avancer / reculer"),
                p.t("controleur.ligne-4", "<gray>change le niveau de vitesse"))) {
            lignes.add(ligne.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lignes);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(p.cleControleurObjet, PersistentDataType.BYTE, (byte) 1);
        objet.setItemMeta(meta);
        return objet;
    }

    public static boolean estControleur(ItemStack objet) {
        return instance != null && objet != null && objet.getType() == Material.MINECART && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(instance.cleControleurObjet,
                PersistentDataType.BYTE);
    }

    private boolean estControleur(Entity wagon) {
        return wagon instanceof RideableMinecart
                && wagon.getPersistentDataContainer().has(cleControleur, PersistentDataType.BYTE);
    }

    /** 1.3.0 : un wagon jukebox (wagonnet brillant ; recette dans KS_Crafts : wagonnet + jukebox). */
    public static ItemStack creerJukebox() {
        KSTrains p = instance;
        ItemStack objet = new ItemStack(Material.MINECART);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(p.t("jukebox-objet.nom", "<white>Wagon jukebox").decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = new ArrayList<>();
        for (Component ligne : List.of(
                p.t("jukebox-objet.ligne-1", "<gray>Joue les disques du coffre"),
                p.t("jukebox-objet.ligne-2", "<gray>attelé juste devant lui"),
                p.t("jukebox-objet.ligne-3", "<gray>Clic droit : lecture, aléatoire,"),
                p.t("jukebox-objet.ligne-4", "<gray>pause"))) {
            lignes.add(ligne.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lignes);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(p.cleJukeboxObjet, PersistentDataType.BYTE, (byte) 1);
        objet.setItemMeta(meta);
        return objet;
    }

    public static boolean estJukebox(ItemStack objet) {
        return instance != null && objet != null && objet.getType() == Material.MINECART && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(instance.cleJukeboxObjet,
                PersistentDataType.BYTE);
    }

    boolean estJukebox(Entity wagon) {
        return wagon instanceof RideableMinecart
                && wagon.getPersistentDataContainer().has(cleJukebox, PersistentDataType.BYTE);
    }

    /** Un distributeur poserait un wagonnet ordinaire (ou un seul four) : la locomotive et le contrôleur y restent. */
    @EventHandler(ignoreCancelled = true)
    public void onDistribuer(BlockDispenseEvent event) {
        if (estLocomotive(event.getItem()) || estControleur(event.getItem()) || estJukebox(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** 1.1.0 : un wagon contrôleur cassé redonne un wagon contrôleur (le jeu lâcherait un wagonnet). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLacher(EntityDropItemEvent event) {
        ItemStack lache = event.getItemDrop().getItemStack();
        if (lache.getType() != Material.MINECART || estControleur(lache) || estJukebox(lache)) {
            return;
        }
        if (estControleur(event.getEntity())) {
            event.getItemDrop().setItemStack(creerControleur());
        } else if (estJukebox(event.getEntity())) {
            event.getItemDrop().setItemStack(creerJukebox());
        }
    }

    // ------------------------------------------------------------------ trains : enregistrement

    /** Wagons qui peuvent faire partie d'un train (pas de TNT, bloc de commande ni spawner). */
    private static boolean accepte(Entity entite) {
        return entite instanceof RideableMinecart || entite instanceof StorageMinecart
                || entite instanceof HopperMinecart || entite instanceof PoweredMinecart;
    }

    boolean estLoco(Entity wagon) {
        return wagon.getPersistentDataContainer().has(cleLoco, PersistentDataType.BYTE);
    }

    Train trainDe(Entity wagon) {
        UUID id = parWagon.get(wagon.getUniqueId());
        return id == null ? null : trains.get(id);
    }

    /** Enregistre ces wagons comme un nouveau train (2 wagons au moins ; un wagon seul redevient un wagonnet). */
    private Train former(List<Minecart> wagons) {
        if (wagons.size() < 2) {
            wagons.forEach(this::effacer);
            return null;
        }
        UUID id = UUID.randomUUID();
        for (int i = 0; i < wagons.size(); i++) {
            PersistentDataContainer donnees = wagons.get(i).getPersistentDataContainer();
            donnees.set(cleTrain, PersistentDataType.STRING, id.toString());
            donnees.set(cleRang, PersistentDataType.INTEGER, i);
            donnees.set(cleTaille, PersistentDataType.INTEGER, wagons.size());
        }
        return inscrire(id, wagons);
    }

    private Train inscrire(UUID id, List<Minecart> wagons) {
        Train train = new Train(id, wagons);
        train.arreteDepuis = tick;
        trains.put(id, train);
        for (Minecart wagon : wagons) {
            parWagon.put(wagon.getUniqueId(), id);
        }
        return train;
    }

    private void effacer(Minecart wagon) {
        PersistentDataContainer donnees = wagon.getPersistentDataContainer();
        donnees.remove(cleTrain);
        donnees.remove(cleRang);
        donnees.remove(cleTaille);
        parWagon.remove(wagon.getUniqueId());
        if (wagon instanceof PoweredMinecart four) {
            foyers.allumer(four, false);
        }
        // 1.3.0 : hors d'un train, plus de décor ni de musique.
        decor.retirer(wagon);
        if (estJukebox(wagon)) {
            jukebox.couper(wagon);
        }
    }

    private void retirer(Train train) {
        trains.remove(train.id);
        for (Minecart wagon : train.wagons) {
            parWagon.remove(wagon.getUniqueId());
        }
        liberer(train);
    }

    @EventHandler
    public void onArriver(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Minecart wagon) {
            getServer().getScheduler().runTask(this, () -> arriver(wagon));
        }
    }

    /** Wagon chargé : il attend les autres wagons de son train, qui se reforme (arrêté) quand tous sont là. */
    private void arriver(Minecart wagon) {
        String texte = wagon.getPersistentDataContainer().get(cleTrain, PersistentDataType.STRING);
        Integer rang = wagon.getPersistentDataContainer().get(cleRang, PersistentDataType.INTEGER);
        Integer taille = wagon.getPersistentDataContainer().get(cleTaille, PersistentDataType.INTEGER);
        if (texte == null || rang == null || taille == null || !wagon.isValid()
                || parWagon.containsKey(wagon.getUniqueId())) {
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(texte);
        } catch (IllegalArgumentException e) {
            return;
        }
        Map<Integer, Minecart> presents = attente.computeIfAbsent(id, k -> new HashMap<>());
        presents.put(rang, wagon);
        if (presents.size() < taille) {
            return;
        }
        List<Minecart> wagons = new ArrayList<>();
        for (int i = 0; i < taille; i++) {
            Minecart present = presents.get(i);
            if (present == null || !present.isValid()) {
                return;
            }
            wagons.add(present);
        }
        attente.remove(id);
        inscrire(id, wagons);
    }

    /**
     * Wagon retiré du monde. Chunk déchargé : le train attend que tous ses wagons soient de nouveau chargés. Wagon
     * détruit (vide, /kill...) : le train est coupé en deux à sa place.
     */
    @EventHandler
    public void onPartir(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof Minecart wagon)) {
            return;
        }
        decor.retirer(wagon);
        if (estJukebox(wagon)) {
            jukebox.interrompre(wagon);
        }
        if (wagon instanceof PoweredMinecart) {
            foyers.oublier(wagon.getUniqueId());
        }
        Train train = trainDe(wagon);
        if (train == null) {
            for (Map<Integer, Minecart> presents : attente.values()) {
                presents.values().removeIf(present -> present.getUniqueId().equals(wagon.getUniqueId()));
            }
            return;
        }
        retirer(train);
        int rang = train.rang(wagon);
        boolean decharge = event.getCause() == EntityRemoveEvent.Cause.UNLOAD;
        if (decharge) {
            Map<Integer, Minecart> presents = new HashMap<>();
            for (int i = 0; i < train.wagons.size(); i++) {
                if (i != rang && train.wagons.get(i).isValid()) {
                    presents.put(i, train.wagons.get(i));
                }
            }
            attente.put(train.id, presents);
        } else {
            former(valides(train.wagons.subList(0, Math.max(0, rang))));
            former(valides(train.wagons.subList(rang + 1, train.wagons.size())));
        }
    }

    private static List<Minecart> valides(List<Minecart> wagons) {
        List<Minecart> liste = new ArrayList<>();
        for (Minecart wagon : wagons) {
            if (wagon.isValid()) {
                liste.add(wagon);
            }
        }
        return liste;
    }

    // ------------------------------------------------------------------ poser la locomotive

    /**
     * Après KS_Claim (même priorité, plugin activé après lui), qui rend les wagonnets posables partout : ici, la pose
     * du wagonnet à four du jeu est refusée et remplacée par les 3 fours de la locomotive.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPoser(PlayerInteractEvent event) {
        boolean locomotive = estLocomotive(event.getItem());
        boolean controleur = estControleur(event.getItem());
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || (!locomotive && !controleur && !estJukebox(event.getItem()))) {
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        if (event.getHand() == EquipmentSlot.HAND && event.getClickedBlock() != null) {
            if (locomotive) {
                poser(event.getPlayer(), event.getClickedBlock(), event.getItem());
            } else {
                poserControleur(event.getPlayer(), event.getClickedBlock(), event.getItem(),
                        controleur ? cleControleur : cleJukebox);
            }
        }
    }

    /** 1.1.0 : le wagon contrôleur (1.3.0 : ou le wagon jukebox, avec un jukebox affiché) se pose, marqué. */
    private void poserControleur(Player joueur, Block rail, ItemStack objet, NamespacedKey marque) {
        if (!(rail.getBlockData() instanceof Rail donnees)) {
            return;
        }
        double hauteur = donnees.getShape().name().startsWith("ASCENDING") ? 0.5625 : 0.0625;
        Location place = rail.getLocation().add(0.5, hauteur, 0.5);
        if (!rail.getWorld().getNearbyEntities(place, 0.5, 0.5, 0.5, e -> e instanceof Minecart).isEmpty()) {
            message(joueur, t("poser.occupe", "<red>Il y a déjà un wagonnet sur ces rails"), true);
            return;
        }
        place.setYaw(joueur.getLocation().getYaw());
        rail.getWorld().spawn(place, RideableMinecart.class, w -> {
            w.getPersistentDataContainer().set(marque, PersistentDataType.BYTE, (byte) 1);
            if (marque.equals(cleJukebox)) {
                w.setDisplayBlockData(Material.JUKEBOX.createBlockData());
            }
        });
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            objet.setAmount(objet.getAmount() - 1);
        }
        rail.getWorld().playSound(place, Sound.BLOCK_METAL_PLACE, 1f, 1f);
    }

    private void poser(Player joueur, Block rail, ItemStack objet) {
        if (!Tag.RAILS.isTagged(rail.getType())) {
            return;
        }
        // Direction du regard, ramenée à l'axe le plus proche.
        double yaw = Math.toRadians(joueur.getLocation().getYaw());
        double vx = -Math.sin(yaw);
        double vz = Math.cos(yaw);
        int dx = Math.abs(vx) >= Math.abs(vz) ? (int) Math.signum(vx) : 0;
        int dz = dx == 0 ? (int) Math.signum(vz) : 0;
        Rail.Shape droit = dx != 0 ? Rail.Shape.EAST_WEST : Rail.Shape.NORTH_SOUTH;
        double espacement = reglage("espacement", 1.4);
        // 1.3.0 : tous les rails sous les 3 fours (4 avec l'écartement de 1,4 bloc).
        int rails = (int) Math.round(2 * espacement) + 1;
        for (int i = 0; i < rails; i++) {
            Block bloc = rail.getRelative(-i * dx, 0, -i * dz);
            if (!(bloc.getBlockData() instanceof Rail donnees) || donnees.getShape() != droit) {
                message(joueur, t("poser.rails-n", "<red>Pose-la sur <n> rails droits, dans le sens des rails",
                        "n", rails), true);
                return;
            }
        }
        List<Location> places = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double recul = i * espacement;
            Location place = rail.getLocation().add(0.5 - recul * dx, 0.0625, 0.5 - recul * dz);
            place.setDirection(new Vector(dx, 0, dz));
            places.add(place);
        }
        BoundingBox zone = BoundingBox.of(places.get(0), places.get(2)).expand(0.6, 0.5, 0.6);
        if (!rail.getWorld().getNearbyEntities(zone, e -> e instanceof Minecart).isEmpty()) {
            message(joueur, t("poser.occupe", "<red>Il y a déjà un wagonnet sur ces rails"), true);
            return;
        }
        List<Minecart> wagons = new ArrayList<>();
        for (Location place : places) {
            wagons.add(rail.getWorld().spawn(place, PoweredMinecart.class,
                    w -> w.getPersistentDataContainer().set(cleLoco, PersistentDataType.BYTE, (byte) 1)));
        }
        Train nouveau = former(wagons);
        if (nouveau != null) {
            reclamer(nouveau, joueur);
        }
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            objet.setAmount(objet.getAmount() - 1);
        }
        rail.getWorld().playSound(places.get(0), Sound.BLOCK_ANVIL_PLACE, 0.6f, 1.4f);
        message(joueur, t("poser.ok",
                "<green>Locomotive posée : carburant par clic droit, menu par accroupi + clic droit"), false);
    }

    // ------------------------------------------------------------------ atteler, dételer, interactions

    private static boolean chaine(ItemStack objet) {
        return objet != null && (objet.getType() == Material.IRON_CHAIN || objet.getType().name().endsWith("COPPER_CHAIN"));
    }

    /**
     * Avant les protections et KS_VerrouVehicule : chaîne (atteler), cisailles (dételer), four d'un train (clic droit :
     * carburant ; accroupi sur la locomotive : menu du train).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCliquer(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Minecart refuse && !accepte(refuse)
                && chaine(event.getPlayer().getInventory().getItemInMainHand())) {
            // 1.3.3 (LeKiwi06 : « les wagons à TNT ne peuvent pas être mis dans une configuration de train ») : refusés
            // depuis la 1.0.0, maintenant avec un message (TNT, bloc de commande, spawner).
            event.setCancelled(true);
            if (event.getHand() == EquipmentSlot.HAND) {
                message(event.getPlayer(), t("atteler.refuse", "<red>Ce wagon ne s'attelle pas à un train"), true);
            }
            return;
        }
        if (!(event.getRightClicked() instanceof Minecart wagon) || !accepte(wagon)) {
            return;
        }
        Player joueur = event.getPlayer();
        boolean main = event.getHand() == EquipmentSlot.HAND;
        ItemStack objet = joueur.getInventory().getItemInMainHand();
        if (chaine(objet)) {
            event.setCancelled(true);
            if (main) {
                atteler(joueur, wagon, objet);
            }
            return;
        }
        Train train = trainDe(wagon);
        // 1.4.0 : les autres joueurs ne font que monter dans les wagonnets simples (voyageurs).
        boolean voyageur = wagon instanceof RideableMinecart && !estControleur(wagon) && !estJukebox(wagon)
                && !joueur.isSneaking() && objet.getType() != Material.SHEARS;
        if (train != null && !voyageur && !acces(joueur, train)) {
            event.setCancelled(true);
            if (main) {
                refuser(joueur, train);
            }
            return;
        }
        if (objet.getType() == Material.COMPASS && estControleur(wagon)) {
            // 1.3.0 : boussole du train.
            event.setCancelled(true);
            if (main) {
                boussoles.lier(joueur, wagon, objet, train == null ? null : nomTrain(train));
            }
            return;
        }
        if (objet.getType() == Material.NAME_TAG && train != null && estLoco(wagon)) {
            // 1.4.0 : nom du train.
            event.setCancelled(true);
            if (main) {
                nommer(joueur, train, objet);
            }
            return;
        }
        if (estJukebox(wagon) && objet.getType() != Material.SHEARS && !joueur.isSneaking()) {
            // 1.3.0 : on ne monte pas dans le wagon jukebox ; clic droit : lecture, aléatoire, pause.
            event.setCancelled(true);
            if (main) {
                message(joueur, jukebox.suivant(wagon), false);
            }
            return;
        }
        if (train == null) {
            return;
        }
        if (objet.getType() == Material.SHEARS) {
            event.setCancelled(true);
            if (main) {
                deteler(joueur, train, Math.max(1, train.rang(wagon)));
            }
        } else if (wagon instanceof PoweredMinecart four) {
            event.setCancelled(true);
            if (main) {
                if (joueur.isSneaking() && estLoco(four)) {
                    menu(joueur, train.id, 0);
                } else {
                    foyers.ouvrir(joueur, four);
                }
            }
        } else if (estControleur(wagon) && joueur.isSneaking()) {
            // 1.1.0 : accroupi + clic droit sur le wagon contrôleur : menu du train (sans accroupi : on y monte).
            event.setCancelled(true);
            if (main) {
                menu(joueur, train.id, 0);
            }
        }
    }

    /**
     * 1.2.0 : vrai si le wagon n'a pas de verrou (KS_VerrouVehicule), ou si le joueur en est le propriétaire ou peut
     * le contourner (opérateur).
     */
    private boolean maitreDuVerrou(Player joueur, Entity wagon) {
        String proprietaire = cleVerrou == null ? null
                : wagon.getPersistentDataContainer().get(cleVerrou, PersistentDataType.STRING);
        return proprietaire == null || proprietaire.equals(joueur.getUniqueId().toString())
                || joueur.hasPermission("ksverrouvehicule.contourner");
    }

    private boolean arrete(Minecart wagon) {
        Train train = trainDe(wagon);
        return train != null ? train.vitesse <= 0 : wagon.getVelocity().lengthSquared() < 0.01;
    }

    private static boolean surRail(Entity wagon) {
        Block bloc = wagon.getLocation().getBlock();
        return Tag.RAILS.isTagged(bloc.getType()) || Tag.RAILS.isTagged(bloc.getRelative(BlockFace.DOWN).getType());
    }

    private void atteler(Player joueur, Minecart wagon, ItemStack chaine) {
        if (!maitreDuVerrou(joueur, wagon)) {
            message(joueur, t("atteler.verrou", "<red>Seul le propriétaire du verrou attelle ce wagon"), true);
            return;
        }
        if (!surRail(wagon)) {
            message(joueur, t("atteler.rails", "<red>Le wagon doit être sur des rails"), true);
            return;
        }
        Selection selection = selections.get(joueur.getUniqueId());
        if (selection == null || selection.fin() < tick || !selection.wagon().isValid()
                || selection.wagon().getUniqueId().equals(wagon.getUniqueId())) {
            selections.put(joueur.getUniqueId(), new Selection(wagon, tick + 300));
            message(joueur, t("atteler.choisi", "<yellow>Clique avec la chaîne sur le wagon à atteler"), false);
            return;
        }
        selections.remove(joueur.getUniqueId());
        Minecart a = selection.wagon();
        if (!a.getWorld().equals(wagon.getWorld()) || a.getLocation().distanceSquared(wagon.getLocation()) > 6.25) {
            message(joueur, t("atteler.loin", "<red>Les deux wagons doivent se toucher presque"), true);
            return;
        }
        Train ta = trainDe(a);
        Train tb = trainDe(wagon);
        if (ta != null && ta == tb) {
            message(joueur, t("atteler.meme", "<red>Ces wagons sont déjà dans le même train"), true);
            return;
        }
        if (!arrete(a) || !arrete(wagon)) {
            message(joueur, t("atteler.arret", "<red>Les wagons doivent être à l'arrêt"), true);
            return;
        }
        List<Minecart> la = ta == null ? List.of(a) : ta.wagons;
        List<Minecart> lb = tb == null ? List.of(wagon) : tb.wagons;
        boolean aTete = same(la.get(0), a);
        boolean aQueue = same(la.get(la.size() - 1), a);
        boolean bTete = same(lb.get(0), wagon);
        boolean bQueue = same(lb.get(lb.size() - 1), wagon);
        if (!(aTete || aQueue) || !(bTete || bQueue)) {
            message(joueur, t("atteler.bout", "<red>On n'attelle qu'au bout d'un train"), true);
            return;
        }
        List<Minecart> resultat = new ArrayList<>();
        if (la.size() == 1 && lb.size() > 1) {
            // Wagon seul : il se met du côté où il est attelé.
            if (bQueue) {
                resultat.addAll(lb);
                resultat.add(a);
            } else {
                resultat.add(a);
                resultat.addAll(lb);
            }
        } else if (aQueue) {
            resultat.addAll(la);
            resultat.addAll(bTete ? lb : inverse(lb));
        } else {
            resultat.addAll(bQueue ? lb : inverse(lb));
            resultat.addAll(la);
        }
        // 1.2.1 (LeKiwi06 : « à un moment mon train a changé de sens en attachant plusieurs wagons ») : en joignant deux
        // trains, l'ordre suivait le premier wagon cliqué et pouvait retourner le train de la locomotive. Le nouveau
        // train garde maintenant le sens du morceau qui a une locomotive (sinon du plus long).
        List<Minecart> reference = aLocomotive(la) ? la : aLocomotive(lb) ? lb : la.size() >= lb.size() ? la : lb;
        if (reference.size() > 1 && rangDans(resultat, reference.get(0)) > rangDans(resultat, reference.get(1))) {
            Collections.reverse(resultat);
        }
        // 1.4.0 : on n'attelle qu'à un train auquel on a accès, et 64 wagons au plus.
        for (Train morceau : new Train[]{ta, tb}) {
            if (morceau != null && refuser(joueur, morceau)) {
                return;
            }
        }
        int maximum = getConfig().getInt("wagons-max", 64);
        if (resultat.size() > maximum) {
            message(joueur, t("atteler.trop", "<red>Un train a <n> wagons au plus", "n", maximum), true);
            return;
        }
        Component refus = refusControleur(resultat);
        if (refus != null) {
            message(joueur, refus, true);
            return;
        }
        if (ta != null) {
            retirer(ta);
        }
        if (tb != null) {
            retirer(tb);
        }
        Train train = former(resultat);
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            chaine.setAmount(chaine.getAmount() - 1);
        }
        wagon.getWorld().playSound(wagon.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
        message(joueur, t("atteler.ok", "<green>Attelé : train de <n> wagons", "n",
                train == null ? 0 : train.wagons.size()), false);
    }

    /**
     * 1.1.0 : un seul wagon contrôleur par train, à moins de 5 wagons de la locomotive (écart de rang avec le four de
     * locomotive le plus proche : 4 au plus). Sans locomotive dans le train, pas d'écart à respecter.
     */
    private Component refusControleur(List<Minecart> wagons) {
        int controleur = -1;
        for (int i = 0; i < wagons.size(); i++) {
            if (estControleur(wagons.get(i))) {
                if (controleur >= 0) {
                    return t("atteler.deux-controleurs", "<red>Un seul wagon contrôleur par train");
                }
                controleur = i;
            }
        }
        if (controleur < 0) {
            return null;
        }
        int ecart = Integer.MAX_VALUE;
        for (int i = 0; i < wagons.size(); i++) {
            if (estLoco(wagons.get(i))) {
                ecart = Math.min(ecart, Math.abs(i - controleur));
            }
        }
        if (ecart != Integer.MAX_VALUE && ecart >= 5) {
            return t("atteler.controleur-loin", "<red>Le wagon contrôleur doit être à moins de 5 wagons de la locomotive");
        }
        return null;
    }

    private boolean aLocomotive(List<Minecart> wagons) {
        for (Minecart wagon : wagons) {
            if (estLoco(wagon)) {
                return true;
            }
        }
        return false;
    }

    private static int rangDans(List<Minecart> wagons, Minecart wagon) {
        for (int i = 0; i < wagons.size(); i++) {
            if (same(wagons.get(i), wagon)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean same(Entity a, Entity b) {
        return a.getUniqueId().equals(b.getUniqueId());
    }

    private static List<Minecart> inverse(List<Minecart> liste) {
        List<Minecart> copie = new ArrayList<>(liste);
        Collections.reverse(copie);
        return copie;
    }

    /** Sépare le wagon {@code rang} (et ceux de derrière) du wagon de devant ; la chaîne tombe. */
    private void deteler(Player joueur, Train train, int rang) {
        if (train.vitesse > 0) {
            message(joueur, t("deteler.arret", "<red>Arrête le train d'abord"), true);
            return;
        }
        if (rang < 1 || rang >= train.wagons.size()) {
            return;
        }
        Minecart wagon = train.wagons.get(rang);
        retirer(train);
        former(new ArrayList<>(train.wagons.subList(0, rang)));
        former(new ArrayList<>(train.wagons.subList(rang, train.wagons.size())));
        // 1.3.0 : les wagons dételés ne gardent aucune vitesse (avant : ils pouvaient rouler sans fin).
        for (Minecart detache : train.wagons) {
            detache.setVelocity(new Vector());
        }
        wagon.getWorld().dropItemNaturally(wagon.getLocation(), new ItemStack(Material.IRON_CHAIN));
        wagon.getWorld().playSound(wagon.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 1f);
        message(joueur, t("deteler.ok", "<green>Wagon dételé"), false);
    }

    /**
     * Un wagon attelé ne se casse pas (il faut le dételer d'abord) : pas de train coupé en roulant. 1.3.1 (LeKiwi06 :
     * « j'arrive à casser les wagons four et le wagon contrôleur ») : KS_Claim 1.3.0 lève, en priorité HIGHEST, tout
     * refus de casser un wagonnet sans inventaire frappé par un joueur (four, contrôleur, jukebox) ; ces écoutes passent
     * maintenant après lui (même priorité, KS_Trains activé après KS_Claim) et refusent de nouveau.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFrapper(VehicleDamageEvent event) {
        if (trainDe(event.getVehicle()) != null) {
            event.setCancelled(true);
            if (event.getAttacker() instanceof Player joueur) {
                message(joueur, t("casser", "<red>Dételle ce wagon (cisailles) pour le casser"), true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCasser(VehicleDestroyEvent event) {
        if (trainDe(event.getVehicle()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * 1.3.1 (LeKiwi06 : « quand je casse un wagon, le modèle reste là où il était ») : un décor qui descend de son wagon
     * (wagon cassé ou retiré, passagers éjectés) disparaît ; il est remis chaque seconde aux wagons des trains.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDecorDescend(EntityDismountEvent event) {
        Entity modele = event.getEntity();
        if (decor.estDecor(modele)) {
            getServer().getScheduler().runTask(this, () -> {
                if (modele.isValid() && !modele.isInsideVehicle()) {
                    modele.remove();
                }
            });
        }
    }

    /** 1.3.0 : contrôleur cassé (dételé) : ses cornes tombent ; wagon jukebox cassé : son disque tombe. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDetruit(VehicleDestroyEvent event) {
        Entity wagon = event.getVehicle();
        if (estControleur(wagon)) {
            klaxons.lacher(wagon);
            boussoles.oublier(wagon.getUniqueId());
        } else if (estJukebox(wagon)) {
            jukebox.lacher(wagon);
        }
    }

    /** 1.3.0 : personne ne monte dans un wagon jukebox (seul son décor y est posé). */
    @EventHandler(ignoreCancelled = true)
    public void onMonter(VehicleEnterEvent event) {
        if (estJukebox(event.getVehicle()) && !decor.estDecor(event.getEntered())) {
            event.setCancelled(true);
        } else if (estControleur(event.getVehicle()) && event.getEntered() instanceof Player joueur
                && refuser(joueur, trainDe(event.getVehicle()))) {
            // 1.4.0 : seuls le propriétaire et ses amis conduisent.
            event.setCancelled(true);
        }
    }

    /** 1.4.0 : coffres et entonnoirs d'un train : ouverts par le propriétaire et ses amis seulement. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onOuvrirWagon(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (event.getInventory().getHolder(false) instanceof Minecart wagon && event.getPlayer() instanceof Player joueur
                && refuser(joueur, trainDe(wagon))) {
            event.setCancelled(true);
        }
    }

    /**
     * 1.4.0 (« il faut protéger tous les contenants du train ») : un entonnoir posé au sol ou un wagonnet à entonnoir
     * hors du train ne vide pas un wagon de train (les entonnoirs du train, eux, passent par Entonnoirs).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onViderWagon(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        if (event.getSource().getHolder(false) instanceof Minecart wagon && trainDe(wagon) != null) {
            event.setCancelled(true);
        }
    }

    /** 1.4.0 (conduite sur Bedrock) : à bord du wagon contrôleur, clic droit main vide : menu du train. */
    @EventHandler(priority = EventPriority.LOW)
    public void onClicABord(PlayerInteractEvent event) {
        Player joueur = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || joueur.getVehicle() == null
                || !estControleur(joueur.getVehicle()) || !joueur.getInventory().getItemInMainHand().isEmpty()
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Train train = trainDe(joueur.getVehicle());
        if (train != null) {
            event.setCancelled(true);
            menu(joueur, train.id, 0);
        }
    }

    /**
     * Les wagons d'un train ne poussent pas et ne sont pas poussés par le jeu (le plugin les place). 1.3.2 (LeKiwi06 :
     * « quand le train est arrêté, il faut que les entités le traversent normalement ») : rien d'autre ici ; un train
     * qui roule percute ce qu'il touche (percuter).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPousser(VehicleEntityCollisionEvent event) {
        if (trainDe(event.getVehicle()) != null || trainDe(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    /** 1.3.2 : dernier tick où une entité a été projetée par un train (une fois par demi-seconde au plus). */
    private final Map<UUID, Long> projetees = new HashMap<>();

    /**
     * 1.3.2 (LeKiwi06 : « un train qui collisionne une entité sur sa route la fait voler en l'air, comme un dragon quand
     * il nous pousse ; plus il va vite, plus il projette loin ; à vitesse max il propulse comme le dragon ; à l'exception
     * des wagons qui se brisent, et les autres trains déraillent (tous les wagons se décrochent et les locomotives se
     * cassent) »). Chaque tick, train qui roule : ce qui touche un de ses wagons (et ce qui est devant le wagon qui
     * mène, sur la distance du tick) est percuté. Passagers, décor, spectateurs et sièges invisibles ne comptent pas.
     */
    private void percuter(Train train) {
        World monde = train.monde();
        Set<UUID> vues = new HashSet<>();
        Vector devant = train.direction().multiply(train.vitesse + 0.3);
        for (Minecart wagon : train.wagons) {
            BoundingBox zone = wagon.getBoundingBox().expand(0.25, 0.1, 0.25);
            if (wagon == train.meneur()) {
                zone = zone.expandDirectional(devant);
            }
            for (Entity touche : monde.getNearbyEntities(zone)) {
                if (!vues.add(touche.getUniqueId()) || train.wagons.contains(touche) || decor.estDecor(touche)
                        || touche.isInsideVehicle() || touche instanceof Display) {
                    continue;
                }
                if (touche instanceof Minecart autre) {
                    Train autreTrain = trainDe(autre);
                    if (autreTrain != null) {
                        derailler(autreTrain);
                        ralentir(train);
                    } else if (aDuContenu(autre)) {
                        // 1.3.3 (LeKiwi06 : « pour tous les wagons sur les rails avec un contenu, il faut les propulser
                        // en l'air comme si c'étaient des entités au lieu de les briser ») ; vide : il se brise.
                        if (projeter(train, wagon, autre)) {
                            ralentir(train);
                        }
                    } else if (briser(autre)) {
                        ralentir(train);
                    }
                } else if (touche instanceof LivingEntity vivant && !(vivant instanceof ArmorStand support
                        && support.isMarker()) && !(vivant instanceof Player joueur
                        && joueur.getGameMode() == GameMode.SPECTATOR)) {
                    if (projeter(train, wagon, vivant)) {
                        ralentir(train);
                    }
                }
            }
        }
    }

    /**
     * Poussée du dragon (lue dans le code du jeu 26.3, EnderDragon.knockBack) : 4 x écart / distance² à l'horizontale,
     * depuis le centre du wagon ; multipliée par vitesse / vitesse maximale (1 à 20 blocs/s). 1.4.1 : montée renforcée.
     */
    private boolean projeter(Train train, Minecart wagon, Entity vivant) {
        Long avant = projetees.get(vivant.getUniqueId());
        if (avant != null && tick - avant < 10) {
            return false;
        }
        projetees.put(vivant.getUniqueId(), tick);
        double force = Math.min(1, train.vitesse / Math.max(0.01, reglage("vitesse-max", 1.0)));
        double dx = vivant.getX() - wagon.getX();
        double dz = vivant.getZ() - wagon.getZ();
        double carre = Math.max(dx * dx + dz * dz, 0.1);
        // 1.4.1 (LeKiwi06 : « le train ne repousse pas correctement, j'aimerais qu'il envoie très haut comme le
        // dragon ») : le dragon pousse à chaque tick tant qu'on est sous ses ailes (la montée de 0,2 s'additionne) ;
        // le train ne pousse qu'une fois : montée de `percussion.hauteur` (3 blocs/tick : 40 blocs de haut, demandé par
        // LeKiwi06) à pleine vitesse, 0,4 au pas ; poussée horizontale du dragon plafonnée à `percussion.horizontal` (3 blocs/tick).
        Vector horizontal = new Vector(dx / carre * 4 * force, 0, dz / carre * 4 * force);
        double plafond = reglage("percussion.horizontal", 3.0);
        if (horizontal.length() > plafond) {
            horizontal.normalize().multiply(plafond);
        }
        double haut = 0.4 + (reglage("percussion.hauteur", 3.0) - 0.4) * force;
        Vector actuelle = vivant.getVelocity();
        vivant.setVelocity(new Vector(actuelle.getX() + horizontal.getX(), Math.max(actuelle.getY(), 0) + haut,
                actuelle.getZ() + horizontal.getZ()));
        return true;
    }

    /**
     * 1.3.3 (LeKiwi06 : « le train qui percute doit ralentir de 10 % de sa vitesse, sauf si ça le fait descendre sous la
     * vitesse 1 ») : à chaque percussion (entité projetée, wagonnet brisé ou projeté, train déraillé).
     */
    private void ralentir(Train train) {
        double plancher = reglage("palier", 0.25);
        if (train.vitesse > plancher) {
            train.vitesse = Math.max(plancher, train.vitesse * (1 - reglage("percussion.ralentissement", 0.10)));
        }
    }

    /**
     * 1.3.3 : wagonnet libre « avec un contenu » : coffre ou entonnoir non vide, passager, cornes du contrôleur ou disque
     * du wagon jukebox. Il est projeté au lieu d'être brisé.
     */
    private boolean aDuContenu(Minecart wagon) {
        if (wagon instanceof InventoryHolder detenteur && !detenteur.getInventory().isEmpty()) {
            return true;
        }
        for (Entity passager : wagon.getPassengers()) {
            if (!decor.estDecor(passager)) {
                return true;
            }
        }
        return (estControleur(wagon) && klaxons.aDesCornes(wagon)) || (estJukebox(wagon) && jukebox.disque(wagon) != null);
    }

    /**
     * 1.3.2 : un wagonnet libre percuté se brise (contenu et objet au sol), sauf si une protection l'en empêche. Vrai
     * s'il s'est brisé.
     */
    private boolean briser(Minecart wagon) {
        if (wagon instanceof CommandMinecart || wagon instanceof SpawnerMinecart || !wagon.isValid()) {
            return false;
        }
        VehicleDestroyEvent casse = new VehicleDestroyEvent(wagon,
                org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.GENERIC).build(), null);
        getServer().getPluginManager().callEvent(casse);
        if (casse.isCancelled()) {
            return false;
        }
        Location lieu = wagon.getLocation();
        // 1.5.0 : minecart avec coffre renforce (vide : sinon il est projete) : redonne le meme minecart.
        ItemStack objet = estControleur(wagon) ? creerControleur() : estJukebox(wagon) ? creerJukebox()
                : CoffresRenforces.est(wagon) ? CoffresRenforces.creerMinecart()
                : new ItemStack(wagon.getMinecartMaterial());
        wagon.getWorld().dropItemNaturally(lieu, objet);
        if (wagon instanceof InventoryHolder detenteur) {
            for (ItemStack contenu : detenteur.getInventory().getContents()) {
                if (contenu != null && !contenu.isEmpty()) {
                    wagon.getWorld().dropItemNaturally(lieu, contenu);
                }
            }
            detenteur.getInventory().clear();
        }
        if (wagon instanceof PoweredMinecart four) {
            ItemStack carburant = foyers.caseCarburant(four);
            if (carburant != null && !carburant.isEmpty()) {
                wagon.getWorld().dropItemNaturally(lieu, carburant);
            }
            foyers.oublier(four.getUniqueId());
        }
        wagon.getWorld().playSound(lieu, Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
        wagon.remove();
        return true;
    }

    /** 1.3.2 : déraillement : tous les wagons sont dételés et arrêtés, les fours de locomotive se brisent. */
    private void derailler(Train train) {
        if (!trains.containsKey(train.id)) {
            return;
        }
        annoncer(train, t("arret.deraillement", "<red>Déraillement !"));
        retirer(train);
        for (Minecart wagon : train.wagons) {
            effacer(wagon);
            wagon.setVelocity(new Vector());
            if (estLoco(wagon)) {
                briser(wagon);
            }
        }
    }

    // ------------------------------------------------------------------ rouler

    /** Traction : 5 par four alimenté de la locomotive, 10 par autre four alimenté. */
    private int traction(Train train) {
        int traction = 0;
        for (Minecart wagon : train.wagons) {
            if (wagon instanceof PoweredMinecart four && foyers.alimente(four)) {
                traction += estLoco(four) ? 5 : 10;
            }
        }
        return traction;
    }

    /**
     * 1.3.2 : vrai si un wagon du train est sur un rail courbe, ou si un virage arrive devant le wagon qui mène avant
     * que le train ait eu le temps de freiner jusqu'à {@code ralentie} (distance de freinage + le chemin d'un tick).
     */
    private boolean enVirage(Train train, double ralentie, double freinage) {
        for (Minecart wagon : train.wagons) {
            if (Voie.virage(Voie.rail(wagon))) {
                return true;
            }
        }
        double v = train.vitesse;
        if (v <= ralentie || freinage <= 0) {
            return false;
        }
        double distance = (v * v - ralentie * ralentie) / (2 * freinage) + v + 1;
        Block rail = Voie.rail(train.meneur());
        return rail != null && Voie.virageDevant(rail, train.direction(), (int) Math.min(128, Math.ceil(distance)));
    }

    /**
     * 1.3.5 (LeKiwi06 : « pour redémarrer sur un rail propulseur, il faut alimenter le rail, sinon la locomotive refuse
     * de passer en vitesse 1 ») : vrai si un wagon du train est sur un rail propulseur éteint.
     */
    private boolean surPropulseurEteint(Train train) {
        for (Minecart wagon : train.wagons) {
            if (Voie.propulseurEteint(Voie.rail(wagon))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1.3.8 : direction de l'avant du train à ce wagon. 1.4.2 : axe du wagon de derrière au wagon de devant (le wagon
     * lui-même aux deux bouts), pente comprise, pour orienter et pencher son modèle.
     */
    private static Vector versLAvant(Train train, Minecart wagon) {
        int rang = train.rang(wagon);
        if (train.wagons.size() < 2 || rang < 0) {
            return null;
        }
        Minecart devant = rang > 0 ? train.wagons.get(rang - 1) : wagon;
        Minecart derriere = rang < train.wagons.size() - 1 ? train.wagons.get(rang + 1) : wagon;
        Vector avant = devant.getLocation().toVector().subtract(derriere.getLocation().toVector());
        return avant.lengthSquared() < 1e-6 ? null : avant;
    }

    // ------------------------------------------------------------------ 1.4.0 : propriétaire, accès, nom

    /** Propriétaire du train (premier four de locomotive qui en a un), ou null. */
    private UUID proprietaire(Train train) {
        for (Minecart wagon : train.wagons) {
            String texte = wagon.getPersistentDataContainer().get(cleProprio, PersistentDataType.STRING);
            if (texte != null && estLoco(wagon)) {
                try {
                    return UUID.fromString(texte);
                } catch (IllegalArgumentException ignore) {
                    // illisible : on continue
                }
            }
        }
        return null;
    }

    private String nomProprietaire(Train train) {
        for (Minecart wagon : train.wagons) {
            String nom = wagon.getPersistentDataContainer().get(cleProprioNom, PersistentDataType.STRING);
            if (nom != null && estLoco(wagon)) {
                return nom;
            }
        }
        return "?";
    }

    /** Le joueur devient propriétaire : enregistré dans chaque four de locomotive du train. */
    private void reclamer(Train train, Player joueur) {
        for (Minecart wagon : train.wagons) {
            if (estLoco(wagon)) {
                wagon.getPersistentDataContainer().set(cleProprio, PersistentDataType.STRING, joueur.getUniqueId().toString());
                wagon.getPersistentDataContainer().set(cleProprioNom, PersistentDataType.STRING, joueur.getName());
            }
        }
    }

    /**
     * Le joueur a-t-il accès au train ? Propriétaire, ses amis de KLM_Contacts, opérateurs ; train sans propriétaire
     * (posé avant la 1.4.0, ou sans locomotive) : libre jusqu'à ce que quelqu'un ouvre son menu.
     */
    private boolean acces(Player joueur, Train train) {
        if (train == null || joueur.isOp()) {
            return true;
        }
        UUID proprio = proprietaire(train);
        if (proprio == null || proprio.equals(joueur.getUniqueId())) {
            return true;
        }
        if (contacts) {
            try {
                return AmisContacts.amis(joueur.getUniqueId(), proprio);
            } catch (LinkageError e) {
                return false;
            }
        }
        return false;
    }

    /** Refus d'accès : message (« Ce train appartient à … »). Vrai si l'accès est refusé. */
    private boolean refuser(Player joueur, Train train) {
        if (acces(joueur, train)) {
            return false;
        }
        message(joueur, t("acces.refuse", "<red>Ce train appartient à <p>", "p", nomProprietaire(train)), true);
        return true;
    }

    /** Nom du train (étiquette posée sur sa locomotive), ou null. */
    private Component nomTrain(Train train) {
        for (Minecart wagon : train.wagons) {
            String nom = wagon.getPersistentDataContainer().get(cleNomTrain, PersistentDataType.STRING);
            if (nom != null && estLoco(wagon)) {
                return Component.text(nom);
            }
        }
        return null;
    }

    /** Étiquette renommée sur un four de la locomotive : nom du train, affiché au-dessus de sa tête. */
    private void nommer(Player joueur, Train train, ItemStack etiquette) {
        net.kyori.adventure.text.Component nom = etiquette.getItemMeta() == null ? null
                : etiquette.getItemMeta().displayName();
        if (nom == null) {
            message(joueur, t("nom.vide", "<red>Renomme d'abord l'étiquette (enclume)"), true);
            return;
        }
        String texte = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(nom);
        boolean premier = true;
        for (Minecart wagon : train.wagons) {
            if (estLoco(wagon)) {
                wagon.getPersistentDataContainer().set(cleNomTrain, PersistentDataType.STRING, texte);
                wagon.customName(premier ? Component.text(texte) : null);
                wagon.setCustomNameVisible(premier);
                premier = false;
            }
        }
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            etiquette.setAmount(etiquette.getAmount() - 1);
        }
        message(joueur, t("nom.ok", "<green>Train nommé : <n>", "n", texte), false);
    }

    /**
     * 1.4.0 (démêlage) : prolonge le trajet du train au-delà de son dernier point, en suivant les rails dans la direction
     * qui s'éloigne du wagon qui mène (celle des derniers points, sinon l'inverse de la dernière marche).
     */
    private void prolonger(Train train, double longueur) {
        if (train.chemin.size() < 1) {
            return;
        }
        boolean avant = train.sens > 0;
        Vector fin = avant ? train.chemin.getLast() : train.chemin.getFirst();
        Vector precedent = train.chemin.size() > 1
                ? (avant ? train.chemin.get(train.chemin.size() - 2) : train.chemin.get(1)) : fin;
        Vector sens = fin.clone().subtract(precedent).setY(0);
        if (sens.lengthSquared() < 0.09) {
            Vector marche = train.dernierSens != null ? train.dernierSens : train.direction();
            sens = marche.clone().multiply(-1).setY(0);
        }
        if (sens.lengthSquared() < 1e-6) {
            return;
        }
        Block rail = train.monde().getBlockAt(fin.getBlockX(), fin.getBlockY(), fin.getBlockZ());
        if (!Tag.RAILS.isTagged(rail.getType())) {
            rail = rail.getRelative(BlockFace.DOWN);
        }
        for (Vector point : Voie.suivre(rail, sens, (int) Math.ceil(longueur))) {
            if (avant) {
                train.chemin.addLast(point);
            } else {
                train.chemin.addFirst(point);
            }
        }
    }

    /** 1.3.4 : ajoute une quantité à un compteur enregistré dans un wagon de locomotive. */
    private static void ajouter(Entity wagon, NamespacedKey cle, double quantite) {
        Double avant = wagon.getPersistentDataContainer().get(cle, PersistentDataType.DOUBLE);
        wagon.getPersistentDataContainer().set(cle, PersistentDataType.DOUBLE, (avant == null ? 0 : avant) + quantite);
    }

    private static double lireCompteur(Entity wagon, NamespacedKey cle) {
        Double valeur = wagon.getPersistentDataContainer().get(cle, PersistentDataType.DOUBLE);
        return valeur == null ? 0 : valeur;
    }

    /**
     * 1.3.4 (LeKiwi06 : « mettre un son reconnaissable pour la locomotive » ; réponse : tchou-tchou à vapeur) : quand
     * le train démarre, coup de sifflet (deux notes de flûte du jeu) ; pendant qu'il roule, souffle de vapeur rythmé, de
     * plus en plus rapide avec la vitesse (toutes les 12 ticks au pas, toutes les 3 ticks à 20 blocs/s). Le son sort du
     * premier four de locomotive du train (trains sans locomotive : rien). 1.4.3 (LeKiwi06 : « le son ne s'entend pas
     * d'assez loin ») : portée réglable, en blocs (`son.portee-sifflet` 128, `son.portee-souffle` 64 ; avant 40 et 16) ;
     * le jeu fait porter un son à 16 blocs x son volume, au-delà de 1 il ne joue pas plus fort. 1.4.5 (LeKiwi06 : « le
     * bruit de train est trop fort maintenant, il faudrait le remettre au volume où il était précédemment, mais le rendre
     * audible de très loin ») : volume et portée séparés (voir {@link #entendre}) : de près, volume d'avant (sifflet 1,
     * souffle 0,5 ; `son.volume-sifflet`, `son.volume-souffle`), qui baisse jusqu'à 0 à la portée.
     */
    private void vapeur(Train train) {
        Minecart locomotive = null;
        for (Minecart wagon : train.wagons) {
            if (estLoco(wagon)) {
                locomotive = wagon;
                break;
            }
        }
        if (locomotive == null || train.vitesse <= 0) {
            return;
        }
        Location lieu = locomotive.getLocation();
        if (train.dernierSouffle == 0 || tick - train.dernierSouffle > 40) {
            // Démarrage (ou reprise après un arrêt) : sifflet.
            double volume = reglage("son.volume-sifflet", 1.0);
            double portee = reglage("son.portee-sifflet", 128);
            // 1.4.7 (LeKiwi06 : « seul le son du train doit être dans créatures amicales : sifflet, cornes, etc. doivent
            // être joués dans jukebox et blocs musicaux ») : sifflet en « jukebox / blocs musicaux ».
            entendre(lieu, Sound.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.RECORDS, volume, 0, portee, 0.84f);
            entendre(lieu, Sound.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.RECORDS, volume, 0, portee, 1.06f);
            train.dernierSouffle = tick;
            return;
        }
        double force = Math.min(1, train.vitesse / Math.max(0.01, reglage("vitesse-max", 1.0)));
        long intervalle = Math.round(12 - 9 * force);
        if (tick - train.dernierSouffle >= intervalle) {
            entendre(lieu, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, reglage("son.volume-souffle", 0.5), 0,
                    reglage("son.portee-souffle", 64), (tick / intervalle) % 2 == 0 ? 0.65f : 0.8f);
            train.dernierSouffle = tick;
        }
    }

    /**
     * 1.4.5 : son du train joué pour chaque joueur à moins de {@code portee} blocs, à {@code volume} de près, baissant
     * jusqu'à 0 à la portée. Le jeu ne sait pas faire porter loin un son qui n'est pas au volume maximal (sa portée est
     * 16 blocs x volume, et il atténue en ligne droite jusqu'à elle) : le son part à 4 blocs au plus du joueur, dans la
     * direction du train, avec le volume qui donne à son oreille celui voulu. 1.4.6 : plein volume jusqu'à
     * {@code plein} blocs (klaxon ; 0 sinon). 1.4.7 : catégorie de son choisie par l'appelant (souffle de vapeur :
     * créatures amicales ; sifflet et klaxon : jukebox / blocs musicaux).
     */
    void entendre(Location lieu, Sound son, SoundCategory categorie, double volume, double plein, double portee,
            float hauteur) {
        for (Player joueur : lieu.getWorld().getPlayers()) {
            Location oreille = joueur.getEyeLocation();
            double distance = oreille.distance(lieu);
            if (distance >= portee) {
                continue;
            }
            double voulu = volume * Math.min(1, (portee - distance) / Math.max(1e-6, portee - plein));
            double ecart = Math.min(distance, 4);
            Location source = distance > 4
                    ? oreille.clone().add(lieu.toVector().subtract(oreille.toVector()).multiply(ecart / distance))
                    : lieu;
            joueur.playSound(source, son, categorie, (float) Math.min(1, voulu / (1 - ecart / 16)),
                    hauteur);
        }
    }

    /** 1.3.1 : 1, ou traction / wagons quand le train est en surcharge (il accélère et freine plus lentement). */
    private double inertie(Train train) {
        int traction = traction(train);
        int wagons = train.wagons.size();
        return traction <= 0 || wagons <= traction ? 1 : (double) traction / wagons;
    }

    private double vitesseMax(Train train) {
        int traction = traction(train);
        if (traction == 0) {
            return 0;
        }
        // 1.4.3 (LeKiwi06, sur tableau comparatif : « proportionnel au carré ») : vitesse maximale x (traction /
        // wagons)² quand le train a plus de wagons que sa traction (avant : -0,1 bloc/tick par wagon en trop, qui
        // faisait passer d'un coup de l'arrêt à la pleine vitesse en ajoutant un seul four).
        double part = Math.min(1, (double) traction / train.wagons.size());
        double vitesse = reglage("vitesse-max", 1.0) * part * part;
        // 1.4.3 (LeKiwi06 : « il faut qu'il refuse d'avancer s'il ne peut pas aller à au moins 3 blocs par seconde ») :
        // en dessous, pas de traction (départ refusé, ou freinage jusqu'à l'arrêt).
        return vitesse < reglage("vitesse-min", 0.15) ? 0 : vitesse;
    }

    /**
     * 1.4.3 (accord de LeKiwi06) : niveau payé en carburant : le niveau choisi, ou celui de la vitesse que le train peut
     * vraiment atteindre s'il est plafonné (plafonné à 6 blocs/s au niveau 4 : payé au niveau 2). Au moins le niveau 1.
     */
    private int niveauAtteint(Train train, double max) {
        int possible = (int) Math.ceil(max / reglage("palier", 0.25) - 1e-6);
        return Math.max(1, Math.min(Math.max(1, train.niveau), possible));
    }

    /** 1.1.0 : touches tenues au dernier envoi, par joueur (pour reconnaître un nouvel appui). */
    private final Map<UUID, Touches> touches = new HashMap<>();

    private record Touches(boolean avancer, boolean reculer, boolean saut) {
    }

    /**
     * 1.1.0 : à bord du wagon contrôleur, un appui sur avancer monte d'un niveau, un appui sur reculer descend d'un
     * niveau ; à l'arrêt, reculer passe en marche arrière. Tenir la touche ne répète pas (choix de LeKiwi06).
     */
    @EventHandler
    public void onTouches(PlayerInputEvent event) {
        Player joueur = event.getPlayer();
        Input nouvelles = event.getInput();
        Touches avant = touches.put(joueur.getUniqueId(), new Touches(nouvelles.isForward(), nouvelles.isBackward(),
                nouvelles.isJump()));
        boolean avancer = nouvelles.isForward() && (avant == null || !avant.avancer());
        boolean reculer = nouvelles.isBackward() && (avant == null || !avant.reculer());
        boolean saut = nouvelles.isJump() && (avant == null || !avant.saut());
        if (joueur.getVehicle() == null || !estControleur(joueur.getVehicle())) {
            return;
        }
        if (saut) {
            // 1.3.0 : klaxon.
            klaxons.sonner(joueur, joueur.getVehicle());
        }
        if (avancer == reculer) {
            return;
        }
        Train train = trainDe(joueur.getVehicle());
        if (train == null || !train.complet()) {
            return;
        }
        changerNiveau(joueur, train, avancer ? 1 : -1);
    }

    /** Niveau +1 ou -1 (contrôleur, ou boutons du menu en 1.4.0). */
    private void changerNiveau(Player joueur, Train train, int pas) {
        int nouveau = pas > 0 ? Math.min(NIVEAUX, train.niveau + 1) : Math.max(-1, train.niveau - 1);
        if (train.niveau == 0 && nouveau != 0 && train.vitesse <= 0 && surPropulseurEteint(train)) {
            // 1.3.5 : pour repartir d'un rail propulseur éteint, il faut l'alimenter.
            message(joueur, t("propulseur.repartir",
                    "<red>Rail propulseur éteint : alimente-le pour repartir"), true);
            return;
        }
        train.niveau = nouveau;
        train.reprise = 0;
        tableau(joueur, train, vitesseMax(train));
    }

    /** 1.1.0 : conducteur du train, assis dans son wagon contrôleur, ou null. */
    private Player pilote(Train train) {
        for (Minecart wagon : train.wagons) {
            if (estControleur(wagon) && !wagon.getPassengers().isEmpty()
                    && wagon.getPassengers().get(0) instanceof Player joueur) {
                return joueur;
            }
        }
        return null;
    }

    /**
     * Vitesse voulue (négative : marche arrière), selon le niveau (1.1.0) : niveau x `palier` (5 blocs/s) en avant,
     * plafonné par la vitesse maximale du train ; `marche-arriere-max` (1 bloc/s) en arrière. Sans traction, le train
     * s'arrête.
     */
    private double cible(Train train, double max) {
        if (train.niveau != 0 && max <= 0) {
            // 1.3.6 (LeKiwi06 : « un train qui n'a plus de carburant ralentit à la même vitesse qu'un train qui
            // freine ») : il freine jusqu'à l'arrêt (avant : arrêt net), puis repasse au niveau 0 avec le message.
            if (train.vitesse <= 0) {
                arreter(train, t("arret.carburant", "<yellow>Le train s'arrête : plus assez de carburant"));
            }
            return 0;
        }
        Player pilote = pilote(train);
        if (pilote != null && tick % 10 == 0) {
            tableau(pilote, train, max);
        }
        if (train.niveau > 0) {
            return Math.min(train.niveau * reglage("palier", 0.25), max);
        }
        return train.niveau < 0 ? -reglage("marche-arriere-max", 0.05) : 0;
    }

    /**
     * 1.4.3 (LeKiwi06 : « quand un texte vient remplacer celui qui indique la vitesse de la locomotive, il faut le
     * laisser afficher 3 secondes avant de remettre l'ancien, sinon on n'a pas le temps de lire ») : fin de l'affichage
     * du dernier message de chaque joueur ; le tableau du conducteur attend jusque-là.
     */
    private final Map<UUID, Long> messages = new HashMap<>();

    /** Message court au-dessus de la barre d'objets (KLM_Menu), gardé 3 secondes avant le retour du tableau. */
    void message(Player joueur, Component texte, boolean refus) {
        messages.put(joueur.getUniqueId(), System.currentTimeMillis() + 3000);
        Contenant.message(joueur, texte, refus);
    }

    /** 1.1.0 : niveau, vitesse voulue et vitesse actuelle (blocs/s), au-dessus de la barre d'objets du conducteur. */
    private void tableau(Player pilote, Train train, double max) {
        Long fin = messages.get(pilote.getUniqueId());
        if (fin != null) {
            if (System.currentTimeMillis() < fin) {
                return;
            }
            messages.remove(pilote.getUniqueId());
        }
        String actuelle = bs(train.vitesse);
        Component autonomie = autonomie(train);
        if (train.niveau == 0) {
            pilote.sendActionBar((train.vitesse > 0
                    ? t("pilote.freine", "<gray>Arrêt demandé <dark_gray>— actuel <a> blocs/s", "a", actuelle)
                    : t("pilote.arret", "<gray>Arrêt <dark_gray>— avancer : niveau 1, reculer : marche arrière"))
                    .append(autonomie));
        } else if (train.niveau < 0) {
            pilote.sendActionBar(t("pilote.arriere", "<yellow>Marche arrière : <v> bloc/s <dark_gray>— actuel <a>",
                    "v", bs(reglage("marche-arriere-max", 0.05)), "a", actuelle).append(autonomie));
        } else {
            double voulue = train.niveau * reglage("palier", 0.25);
            pilote.sendActionBar((voulue > max
                    ? t("pilote.plafond", "<green>Niveau <n>/4 <gray>: plafonné à <v> blocs/s <dark_gray>— actuel <a>",
                    "n", train.niveau, "v", bs(max), "a", actuelle)
                    : t("pilote.niveau", "<green>Niveau <n>/4 <gray>: <v> blocs/s <dark_gray>— actuel <a>",
                    "n", train.niveau, "v", bs(voulue), "a", actuelle)).append(autonomie));
        }
    }

    /**
     * 1.4.0 : autonomie au niveau actuel (blocs) : celle du four alimenté le moins rempli (carburant de sa case compris),
     * puisque chaque four alimenté brûle la même distance.
     */
    private Component autonomie(Train train) {
        double plusPetite = Double.MAX_VALUE;
        for (Minecart wagon : train.wagons) {
            if (wagon instanceof PoweredMinecart four && foyers.alimente(four)) {
                plusPetite = Math.min(plusPetite, foyers.charge(four));
            }
        }
        if (plusPetite == Double.MAX_VALUE) {
            return t("pilote.sans-carburant", "<red> — sans carburant");
        }
        long blocs = Math.round(plusPetite / (consommation(niveauAtteint(train, vitesseMax(train))) * surcharge(train)));
        return t("pilote.autonomie", "<dark_gray> — autonomie <gray><b> blocs", "b", blocs);
    }

    /**
     * 1.1.0 : carburant brûlé par bloc parcouru selon le niveau choisi (demande de LeKiwi06 : croisière au niveau 2 ;
     * niveau 1 -25 %, niveau 3 +25 %, niveau 4 +50 %). Marche arrière, et freinage jusqu'à l'arrêt (niveau 0) : comme
     * le niveau 1 (choix de Claude).
     */
    /**
     * 1.4.3 (LeKiwi06 : « à partir de 8 wagons, chaque nouveau wagon ajouté fait consommer davantage tous les fours du
     * train », puis « rendre le nombre de fours plus significatif » et « si le train est moins chargé, les fours
     * consomment moins » ; modèle choisi sur tableau comparatif : M3 « charge de traction », sous-charge 0,5,
     * surcharge 30 %). Charge = wagons / traction. Charge <= 1 : chaque four consomme racine(charge), au moins 0,5
     * (`charge.plancher`) ; charge > 1 : racine(charge) x (1 + 30 % x wagons en trop / fours alimentés)
     * (`charge.taux`). Le nombre de fours le plus économe à pleine vitesse est toujours celui juste nécessaire
     * (3 jusqu'à 15 wagons, puis un de plus tous les 10 wagons).
     */
    private double surcharge(Train train) {
        int traction = traction(train);
        if (traction <= 0) {
            return 1;
        }
        int wagons = train.wagons.size();
        double charge = (double) wagons / traction;
        if (charge <= 1) {
            return Math.max(reglage("charge.plancher", 0.5), Math.sqrt(charge));
        }
        int fours = 0;
        for (Minecart wagon : train.wagons) {
            if (wagon instanceof PoweredMinecart four && foyers.alimente(four)) {
                fours++;
            }
        }
        return Math.sqrt(charge) * (1 + reglage("charge.taux", 0.30) * (wagons - traction) / Math.max(1, fours));
    }

    private double consommation(int niveau) {
        return switch (niveau) {
            case 2 -> reglage("consommation.niveau-2", 1.0);
            case 3 -> reglage("consommation.niveau-3", 1.25);
            case 4 -> reglage("consommation.niveau-4", 1.5);
            default -> reglage("consommation.niveau-1", 0.75);
        };
    }

    /** Vitesse en blocs par seconde, sans décimale inutile. */
    private static String bs(double blocsParTick) {
        double v = blocsParTick * 20;
        return Math.abs(v - Math.round(v)) < 0.05 ? String.valueOf(Math.round(v)) : String.format("%.1f", v);
    }

    /**
     * Chaque tick : vitesse, carburant, wagon qui mène (la tête, ou le dernier en marche arrière) poussé le long des
     * rails, autres wagons placés sur son trajet.
     */
    private void rouler() {
        tick++;
        double espacement = reglage("espacement", 1.4);
        for (Train train : List.copyOf(trains.values())) {
            if (!train.complet()) {
                continue;
            }
            double max = vitesseMax(train);
            if (train.reprise != 0 && train.niveau == 0 && train.vitesse <= 0 && max > 0 && !surPropulseurEteint(train)) {
                // 1.4.0 (départ en gare) : rail propulseur alimenté sous un train qu'il avait arrêté : il repart.
                train.niveau = train.reprise;
                train.reprise = 0;
                annoncer(train, t("arret.repart", "<green>Le train repart"));
            }
            boolean roulait = train.vitesse > 0;
            double voulue = cible(train, max) * train.sens;
            if (voulue < 0) {
                // Changer de sens : freiner jusqu'à l'arrêt, puis l'autre bout du train mène.
                if (train.vitesse <= 0) {
                    train.sens = -train.sens;
                    train.reconstruire();
                    train.arret = null;
                    train.surRail = null;
                    voulue = -voulue;
                } else {
                    voulue = 0;
                }
            }
            Minecart meneur = train.meneur();
            Vector ici = meneur.getLocation().toVector();
            Vector avant = train.chemin.isEmpty() ? ici : train.bout();
            double parcouru = train.vitesse > 0 ? ici.distance(avant) : 0;
            if (train.vitesse > 0 && !surRail(meneur)) {
                // Bout des rails : le wagon qui mène revient sur le dernier rail, le train s'arrête.
                if (train.surRail != null) {
                    meneur.teleport(train.surRail, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                }
                meneur.setVelocity(new Vector());
                arreter(train, t("arret.bout", "<yellow>Le train s'arrête : bout des rails"));
                voulue = 0;
                ici = meneur.getLocation().toVector();
                parcouru = 0;
            } else if (surRail(meneur)) {
                train.surRail = meneur.getLocation();
            }
            // 1.3.5 (LeKiwi06 : « un train sur un rail propulseur allumé ne consomme pas de carburant ; s'il passe sur un
            // rail propulseur éteint, il diminue de 1 bloc/s en vitesse ») : rail sous le wagon qui mène.
            Block railMeneur = Voie.rail(meneur);
            boolean allume = Voie.propulseurAllume(railMeneur);
            boolean eteint = Voie.propulseurEteint(railMeneur);
            if (eteint && train.vitesse > 0 && !railMeneur.equals(train.dernierRail)) {
                // Chaque rail éteint franchi : -1 bloc/s (20 rails arrêtent un train lancé à 20 blocs/s).
                train.vitesse = Math.max(0, train.vitesse - reglage("propulseur.freinage", 0.05));
                if (train.vitesse <= 0) {
                    int avantArret = train.niveau;
                    arreter(train, t("arret.propulseur", "<yellow>Le train s'arrête : rail propulseur éteint"));
                    // 1.4.0 : il repartira seul à ce niveau quand le rail sera alimenté (départ en gare).
                    train.reprise = avantArret;
                    voulue = 0;
                }
            }
            train.dernierRail = railMeneur;
            if (eteint) {
                // Pas de réaccélération sur un rail éteint.
                voulue = Math.min(voulue, train.vitesse);
            }
            double surcharge = parcouru > 0 ? surcharge(train) : 1;
            double parBloc = consommation(train.niveau > 0 ? niveauAtteint(train, max) : train.niveau);
            if (parcouru > 0) {
                for (Minecart wagon : train.wagons) {
                    if (wagon instanceof PoweredMinecart four && foyers.alimente(four) && !allume) {
                        double blocs = parcouru * parBloc * surcharge;
                        foyers.consommer(four, blocs);
                        if (estLoco(four)) {
                            ajouter(four, cleBrule, blocs);
                        }
                    }
                    if (estLoco(wagon)) {
                        ajouter(wagon, cleCompteur, parcouru);
                    }
                }
            }
            if (train.vitesse > 0.04 && parcouru < 0.01) {
                if (++train.bloque > 20) {
                    arreter(train, t("arret.obstacle", "<yellow>Le train s'arrête : obstacle"));
                    voulue = 0;
                }
            } else {
                train.bloque = 0;
            }
            // 1.3.1 (LeKiwi06 : « une centaine de blocs pour atteindre sa vitesse maximum, et pour freiner une
            // cinquantaine », plus lent encore en surcharge) : 0,005 et 0,01 bloc/tick par tick de 0 à 1 bloc/tick
            // (100 et 50 blocs), multipliés par traction / wagons quand le train a plus de wagons que sa traction.
            vapeur(train);
            double charge = inertie(train);
            // 1.3.2 : virages : -25 % (sauf niveau 1 et marche arrière), anticipé sur la distance de freinage ; une
            // diagonale (virages qui se suivent) reste un seul ralentissement.
            if (train.niveau >= 2 && voulue > 0) {
                double ralentie = voulue * (1 - reglage("virage.reduction", 0.25));
                if (enVirage(train, ralentie, reglage("freinage", 0.01) * charge)) {
                    voulue = ralentie;
                }
            }
            train.vitesse = train.vitesse < voulue
                    ? Math.min(voulue, train.vitesse + reglage("acceleration", 0.005) * charge)
                    : Math.max(voulue, train.vitesse - reglage("freinage", 0.01) * charge);
            Vector deplacement = ici.clone().subtract(avant);
            train.avancer(ici, train.wagons.size() * espacement + 3);
            if (train.vitesse > 0) {
                train.arret = null;
                train.places.clear();
                train.arreteDepuis = tick;
                meneur.setVelocity(train.direction().multiply(train.vitesse));
            } else {
                if (train.arret == null) {
                    train.arret = meneur.getLocation();
                }
                meneur.setVelocity(new Vector());
                if (meneur.getLocation().distanceSquared(train.arret) > 0.0025) {
                    meneur.teleport(train.arret, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                }
                deplacement = new Vector();
            }
            placer(train, ici.clone().add(deplacement), espacement);
            if (!roulait && train.vitesse > 0) {
                // 1.4.0 : le dernier train mis en route par son propriétaire garde ses chunks chargés.
                UUID proprio = proprietaire(train);
                if (proprio != null) {
                    dernierLance.put(proprio, train.id);
                }
            }
            if (train.vitesse > 0) {
                train.dernierSens = train.direction();
            }
            if (train.vitesse > 0) {
                // 1.3.7 : modèles agrandis qui suivent les pentes.
                for (Minecart wagon : train.wagons) {
                    decor.orienter(wagon, versLAvant(train, wagon));
                }
            }
            if (train.vitesse > 0 && trains.containsKey(train.id)) {
                percuter(train);
            }
            if (tick % Entonnoirs.RYTHME == 0) {
                entonnoirs.transferer(train);
            }
        }
    }

    /** Place chaque wagon derrière celui qui mène, sur son trajet : poussé vers sa place, ou replacé s'il en est loin. */
    private void placer(Train train, Vector devant, double espacement) {
        boolean prolonge = false;
        World monde = train.monde();
        Vector precedente = devant;
        for (int k = 1; k < train.wagons.size(); k++) {
            Minecart wagon = train.suiveur(k);
            Vector cible = train.point(devant, k * espacement);
            boolean arrete = train.vitesse <= 0;
            if (arrete) {
                // 1.4.4 (LeKiwi06 : « l'arrêt en pente en marche arrière produit le même souci qu'il y avait avant en
                // marche avant », wagons entassés et blocs qui bougent, en reculant en montée) : train arrêté, chaque
                // wagon est maintenu à la place qu'il avait au moment de l'arrêt, sans dépendre du trajet (en marche
                // arrière, juste après le changement de sens, le trajet est refait sur la position des wagons et peut
                // être trop court ou tassé : des wagons n'étaient plus retenus ou étaient remis au mauvais endroit).
                Location place = train.places.computeIfAbsent(wagon.getUniqueId(), u -> wagon.getLocation());
                if (wagon.getLocation().distanceSquared(place) > 0.0025) {
                    wagon.teleport(place, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                }
                wagon.setVelocity(new Vector());
                precedente = place.toVector();
                continue;
            }
            if (cible == null && !prolonge) {
                // 1.4.0 (démêlage) : trajet trop court (wagons tassés ou trop écartés) : prolongé le long des rails.
                prolonge = true;
                prolonger(train, (train.wagons.size() - k) * espacement + 2);
                cible = train.point(devant, k * espacement);
            }
            if (cible == null) {
                wagon.setVelocity(new Vector());
                continue;
            }
            Vector ecart = cible.clone().subtract(wagon.getLocation().toVector());
            // 1.3.9 (LeKiwi06, capture : « mon train s'est arrêté en pente, et du coup il s'est entassé ») : sur une
            // pente, le jeu pousse chaque wagonnet vers le bas (0,0078 bloc/tick à chaque tick, lu dans le code du jeu
            // 26.3, NewMinecartBehavior.calculateSlopeSpeed ; sans lien avec la gravité). Train arrêté : chaque wagon est
            // remis à sa place dès qu'il s'en écarte de plus de 0,05 bloc (avant : seulement poussé vers elle, il glissait).
            if (ecart.lengthSquared() > 2.25) {
                Location place = cible.toLocation(monde);
                Vector sens = precedente.clone().subtract(cible);
                if (sens.lengthSquared() > 1e-6) {
                    place.setDirection(sens);
                    place.setPitch(0);
                }
                wagon.teleport(place, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                wagon.setVelocity(new Vector());
            } else {
                wagon.setVelocity(ecart);
            }
            precedente = cible;
        }
    }

    private void arreter(Train train, Component raison) {
        boolean roulait = train.niveau != 0 || train.vitesse > 0;
        train.reprise = 0;
        train.niveau = 0;
        train.vitesse = 0;
        train.bloque = 0;
        if (roulait) {
            annoncer(train, raison);
        }
    }

    /** Message aux joueurs proches de la tête ou à bord. */
    private void annoncer(Train train, Component texte) {
        Component nom = nomTrain(train);
        if (nom != null) {
            texte = t("annonce.nom", "<white><nom> : ", "nom", nom).append(texte);
        }
        Location tete = train.tete().getLocation();
        for (Player joueur : train.monde().getPlayers()) {
            if (joueur.getLocation().distanceSquared(tete) < 32 * 32
                    || (joueur.getVehicle() != null && parWagon.containsKey(joueur.getVehicle().getUniqueId()))) {
                message(joueur, texte, true);
            }
        }
    }

    /** Chaque seconde : fours allumés selon leur carburant, chunks gardés chargés autour des trains qui roulent. */
    private void surveiller() {
        long maintenant = System.currentTimeMillis();
        messages.values().removeIf(fin -> fin <= maintenant);
        selections.values().removeIf(selection -> selection.fin() < tick);
        projetees.values().removeIf(quand -> tick - quand > 20);
        for (Train train : List.copyOf(trains.values())) {
            if (!train.complet()) {
                continue;
            }
            for (Minecart wagon : train.wagons) {
                boolean alimente = wagon instanceof PoweredMinecart four && foyers.alimente(four);
                if (wagon instanceof PoweredMinecart four) {
                    foyers.allumer(four, alimente);
                }
                decor.entretenir(wagon, versLAvant(train, wagon));
                if (estControleur(wagon)) {
                    boussoles.noter(wagon);
                }
            }
            Set<Long> voulus = new HashSet<>();
            // 1.4.0 (LeKiwi06 : « les joueurs ne peuvent avoir qu'un train qui charge simultanément, et il faut que la
            // zone chargée soit la plus petite possible ») : seulement le dernier train lancé par son propriétaire, tant
            // qu'il roule : les chunks de ses wagons et celui où sera le wagon qui mène dans une seconde. Train sans
            // propriétaire : rien.
            UUID proprio = proprietaire(train);
            if (train.vitesse > 0 && proprio != null && train.id.equals(dernierLance.get(proprio))) {
                for (Minecart wagon : train.wagons) {
                    voulus.add(chunk(wagon.getLocation()));
                }
                Location devant = train.meneur().getLocation().add(train.direction().multiply(train.vitesse * 20 + 1));
                voulus.add(chunk(devant));
            }
            World monde = train.monde();
            for (long cle : train.chunks) {
                if (!voulus.contains(cle)) {
                    monde.removePluginChunkTicket((int) (cle >> 32), (int) cle, this);
                }
            }
            for (long cle : voulus) {
                if (!train.chunks.contains(cle)) {
                    monde.addPluginChunkTicket((int) (cle >> 32), (int) cle, this);
                }
            }
            train.chunks.clear();
            train.chunks.addAll(voulus);
        }
    }

    /** 1.3.0 : toutes les 4 ticks, le four du milieu de chaque locomotive alimentée fume (feu de camp). */
    private void fumer() {
        for (Train train : trains.values()) {
            if (!train.complet()) {
                continue;
            }
            for (int i = 1; i < train.wagons.size() - 1; i++) {
                Minecart wagon = train.wagons.get(i);
                if (wagon instanceof PoweredMinecart four && estLoco(four) && estLoco(train.wagons.get(i - 1))
                        && estLoco(train.wagons.get(i + 1)) && foyers.alimente(four)) {
                    decor.fumer(four);
                }
            }
        }
    }

    /** 1.3.0 : toutes les 10 ticks, les wagons jukebox des trains. */
    private void musique() {
        for (Train train : List.copyOf(trains.values())) {
            if (!train.complet()) {
                continue;
            }
            for (int i = 0; i < train.wagons.size(); i++) {
                if (estJukebox(train.wagons.get(i))) {
                    jukebox.avancer(train, i);
                }
            }
        }
    }

    private static long chunk(Location lieu) {
        return ((long) (lieu.getBlockX() >> 4) << 32) | ((lieu.getBlockZ() >> 4) & 0xffffffffL);
    }

    private void liberer(Train train) {
        World monde = train.monde();
        for (long cle : train.chunks) {
            monde.removePluginChunkTicket((int) (cle >> 32), (int) cle, this);
        }
        train.chunks.clear();
    }

    // ------------------------------------------------------------------ menu du train

    /** Le train de ce menu, s'il existe encore et que le joueur en est assez près. */
    private Train trainDuMenu(Player joueur, UUID id) {
        Train train = trains.get(id);
        if (train == null || !train.complet() || !train.monde().equals(joueur.getWorld())
                || train.tete().getLocation().distanceSquared(joueur.getLocation()) > PORTEE_MENU * PORTEE_MENU) {
            message(joueur, t("menu.perdu", "<red>Ce train a changé ou est trop loin"), true);
            Contenant.fermer(joueur);
            return null;
        }
        if (refuser(joueur, train)) {
            Contenant.fermer(joueur);
            return null;
        }
        if (proprietaire(train) == null && aLocomotive(train.wagons)) {
            // 1.4.0 (choix de LeKiwi06) : train posé avant la 1.4.0 : le premier qui ouvre son menu en devient
            // propriétaire.
            reclamer(train, joueur);
            message(joueur, t("acces.reclame", "<green>Ce train est maintenant à toi"), false);
        }
        return train;
    }

    private void menu(Player joueur, UUID id, int page) {
        Train train = trainDuMenu(joueur, id);
        if (train == null) {
            return;
        }
        int n = train.wagons.size();
        int pages = (n + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE;
        int p = Math.max(0, Math.min(page, pages - 1));
        Component nom = nomTrain(train);
        Contenant menu = Contenant.pour(joueur, nom == null ? t("menu.titre", "Train de <n> wagons", "n", n)
                : t("menu.titre-nom", "<nom> — <n> wagons", "nom", nom, "n", n), new Marque(id));
        for (int k = 0; k < Contenant.PAR_PAGE; k++) {
            int rang = p * Contenant.PAR_PAGE + k;
            if (rang >= n) {
                break;
            }
            menu.poser(k, icone(train, rang, true), j -> pageWagon(j, id, rang, p));
        }
        menu.barre();
        if (p > 0) {
            menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, t("menu.precedente", "<white>Page précédente"),
                    List.of()), j -> menu(j, id, p - 1));
        }
        if (p < pages - 1) {
            menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, t("menu.suivante", "<white>Page suivante"),
                    List.of()), j -> menu(j, id, p + 1));
        }
        int traction = traction(train);
        double max = vitesseMax(train);
        menu.poser(menu.bas(3), Contenant.objet(Material.COMPASS, t("menu.info", "<white>Traction : <n> / <t> wagons",
                "n", n, "t", traction), List.of(
                train.niveau > 0 ? t("menu.info-niveau", "<gray>Niveau : <n>/4", "n", train.niveau)
                        : train.niveau < 0 ? t("menu.info-niveau-recul", "<gray>Niveau : marche arrière")
                        : t("menu.info-niveau-arret", "<gray>Niveau : arrêt"),
                t("menu.info-max", "<gray>Vitesse max : <v> blocs/s", "v", bs(max)),
                train.sens > 0 || train.vitesse <= 0
                        ? t("menu.info-vitesse", "<gray>Vitesse : <v> blocs/s", "v", bs(train.vitesse))
                        : t("menu.info-recul", "<gray>Marche arrière : <v> blocs/s", "v", bs(train.vitesse)),
                t("menu.info-proprio", "<gray>Propriétaire : <p> (et ses amis)", "p", nomProprietaire(train)),
                t("menu.info-regle", "<gray>Fours alimentés : 5 (locomotive)"),
                t("menu.info-regle-2", "<gray>ou 10 wagons chacun"))), null);
        // 1.3.4 (LeKiwi06 : « un compteur kilométrique dans l'interface de la locomotive pour voir la distance
        // parcourue », avec le carburant consommé) : distance de la locomotive (son four le plus avancé) et carburant
        // brûlé par ses fours, en équivalent charbon (1 charbon = 80 blocs de réserve).
        double distance = 0;
        double brule = 0;
        for (Minecart wagon : train.wagons) {
            if (estLoco(wagon)) {
                distance = Math.max(distance, lireCompteur(wagon, cleCompteur));
                brule += lireCompteur(wagon, cleBrule);
            }
        }
        if (aLocomotive(train.wagons)) {
            menu.poser(menu.bas(2), Contenant.objet(Material.CLOCK, t("menu.compteur", "<white>Compteur : <d>", "d",
                    distance < 1000 ? t("menu.blocs", "<b> blocs", "b", Math.round(distance))
                            : t("menu.km", "<k> km", "k", String.format("%.1f", distance / 1000))), List.of(
                    t("menu.brule", "<gray>Carburant brûlé : <c> charbons", "c", Math.round(brule / 80)),
                    t("menu.brule-ligne", "<gray>(équivalent, fours de la locomotive)"))), null);
        }
        menu.poser(menu.bas(4), train.niveau != 0
                ? Contenant.objet(Material.RED_CONCRETE, t("menu.arret", "<red>Arrêter le train"), List.of())
                : Contenant.objet(Material.LIME_CONCRETE, t("menu.marche", "<green>Mettre en marche"),
                List.of(t("menu.marche-ligne", "<gray>Niveau 1 (5 blocs/s), vers l'avant"))), j -> basculer(j, id, p));
        // 1.4.0 (secours pour Bedrock) : niveau - / +, klaxon.
        menu.poser(menu.bas(1), Contenant.objet(Material.RED_DYE, t("menu.niveau-moins", "<white>Niveau -"),
                List.of(t("menu.niveau-moins-ligne", "<gray>À l'arrêt : marche arrière"))), j -> boutonNiveau(j, id, p, -1));
        menu.poser(menu.bas(6), Contenant.objet(Material.LIME_DYE, t("menu.niveau-plus", "<white>Niveau +"),
                List.of(t("menu.niveau-plus-ligne", "<gray>5 blocs/s par niveau"))), j -> boutonNiveau(j, id, p, 1));
        for (Minecart wagon : train.wagons) {
            if (estControleur(wagon)) {
                menu.poser(menu.bas(7), Contenant.objet(Material.GOAT_HORN, t("menu.klaxon", "<white>Klaxon"),
                        List.of(t("menu.klaxon-sifflet", "<gray>Corne choisie, sinon sifflet du train"))), j -> {
                    Train actuel = trainDuMenu(j, id);
                    if (actuel != null && actuel.wagons.contains(wagon)) {
                        klaxons.sonner(j, wagon);
                    }
                });
                break;
            }
        }
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        menu.ouvrir(joueur);
    }

    private ItemStack icone(Train train, int rang, boolean cliquable) {
        Minecart wagon = train.wagons.get(rang);
        Material materiau;
        Component nom;
        List<Component> lignes = new ArrayList<>();
        int numero = rang + 1;
        if (wagon instanceof PoweredMinecart four) {
            materiau = Material.FURNACE_MINECART;
            nom = estLoco(four) ? t("wagon.loco", "<white><n>. Four de la locomotive", "n", numero)
                    : t("wagon.four", "<white><n>. Four", "n", numero);
            lignes.add(t("wagon.reserve", "<gray>Réserve : <b> blocs", "b", Math.round(foyers.reserve(four))));
            lignes.add(foyers.alimente(four) ? t("wagon.alimente", "<green>Alimenté")
                    : t("wagon.vide", "<red>Sans carburant"));
        } else if (wagon instanceof StorageMinecart coffre) {
            materiau = Material.CHEST_MINECART;
            if (CoffresRenforces.est(coffre)) {
                // 1.5.0 : minecart avec coffre renforce (KS_CoffreRenforce) et son contenu.
                nom = t("wagon.coffre-renforce", "<white><n>. Coffre renforcé", "n", numero);
                ItemStack stocke = CoffresRenforces.objet(coffre);
                lignes.add(stocke == null ? t("wagon.coffre-renforce-vide", "<gray>Vide")
                        : t("wagon.coffre-renforce-contenu", "<gray><q> x <o>", "q", CoffresRenforces.nombre(coffre),
                        "o", stocke.effectiveName()));
            } else {
                nom = t("wagon.coffre", "<white><n>. Coffre", "n", numero);
            }
            lignes.add(entonnoirs.entree(coffre) ? t("wagon.entree-oui", "<green>Entrée : autorisée")
                    : t("wagon.entree-non", "<red>Entrée : refusée"));
            lignes.add(entonnoirs.sortie(coffre) ? t("wagon.sortie-oui", "<green>Sortie : autorisée")
                    : t("wagon.sortie-non", "<red>Sortie : refusée"));
            String nomVerrou = coffre.getPersistentDataContainer().get(
                    NamespacedKey.fromString("ks_verrouvehicule:nom"), PersistentDataType.STRING);
            if (cleVerrou != null && coffre.getPersistentDataContainer().has(cleVerrou, PersistentDataType.STRING)) {
                lignes.add(t("wagon.verrou", "<gray>Verrouillé par <p>", "p", nomVerrou == null ? "?" : nomVerrou));
            }
        } else if (wagon instanceof HopperMinecart entonnoir) {
            materiau = Material.HOPPER_MINECART;
            nom = t("wagon.entonnoir", "<white><n>. Entonnoir", "n", numero);
            int objets = 0;
            for (ItemStack objet : entonnoir.getInventory().getContents()) {
                objets += objet == null ? 0 : objet.getAmount();
            }
            lignes.add(t("wagon.entonnoir-objets", "<gray><o> objets dedans", "o", objets));
            lignes.add(t("wagon.entonnoir-regle-1", "<gray>Prend dans les 3 wagons derrière,"));
            lignes.add(t("wagon.entonnoir-regle-2", "<gray>donne aux 3 wagons devant"));
        } else if (estJukebox(wagon)) {
            materiau = Material.JUKEBOX;
            nom = t("wagon.jukebox", "<white><n>. Wagon jukebox", "n", numero);
            lignes.add(jukebox.nomMode(jukebox.mode(wagon)));
            ItemStack disque = jukebox.enCours(wagon);
            lignes.add(disque == null ? t("wagon.jukebox-silence", "<gray>Aucun disque en cours")
                    : t("wagon.jukebox-disque", "<gray>En cours : <d>", "d",
                    Jukebox.chanson(disque).getDescription()));
        } else if (estControleur(wagon)) {
            materiau = Material.COMPARATOR;
            nom = t("wagon.controleur", "<white><n>. Wagon contrôleur", "n", numero);
            Entity passager = wagon.getPassengers().isEmpty() ? null : wagon.getPassengers().get(0);
            lignes.add(passager instanceof Player p ? t("wagon.conducteur", "<gray>Conducteur : <p>", "p", p.getName())
                    : t("wagon.sans-conducteur", "<gray>Personne à bord"));
        } else {
            materiau = Material.MINECART;
            nom = t("wagon.wagonnet", "<white><n>. Wagonnet", "n", numero);
            Entity passager = wagon.getPassengers().isEmpty() ? null : wagon.getPassengers().get(0);
            lignes.add(passager instanceof Player p ? t("wagon.passager", "<gray>Passager : <p>", "p", p.getName())
                    : t("wagon.sans-passager", "<gray>Aucun passager"));
        }
        // 1.4.8 (LeKiwi06 : « qu'on puisse renommer, leur donner des noms dans cette interface ») : le nom du wagon en
        // titre, son type et son numéro en première ligne.
        String perso = nomWagon(wagon);
        if (perso != null) {
            lignes.add(0, Component.text(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(nom), net.kyori.adventure.text.format.NamedTextColor.GRAY));
            nom = t("wagon.nomme", "<white><nom>", "nom", perso);
        }
        if (cliquable) {
            lignes.add(contenu(wagon) ? t("wagon.clic-contenu", "<gray>Clic : contenu, nom, dételer")
                    : estJukebox(wagon) || estControleur(wagon)
                    ? t("wagon.clic-reglages-nom", "<gray>Clic : réglages, nom, dételer")
                    : t("wagon.clic-nom", "<gray>Clic : nom, dételer"));
        }
        return Contenant.objet(materiau, nom, lignes);
    }

    private void pageWagon(Player joueur, UUID id, int rang, int page) {
        Train train = trainDuMenu(joueur, id);
        if (train == null || rang >= train.wagons.size()) {
            return;
        }
        Contenant menu = Contenant.pour(joueur, 1, t("page.titre", "Wagon <n>", "n", rang + 1), new Marque(id));
        menu.barre();
        Minecart wagon = train.wagons.get(rang);
        if (wagon instanceof StorageMinecart coffre) {
            // 1.2.0 : réglages du coffre pour les entonnoirs du train.
            menu.poser(1, icone(train, rang, false), null);
            menu.poser(2, entonnoirs.entree(coffre)
                    ? Contenant.objet(Material.LIME_DYE, t("page.entree-oui", "<green>Entrée autorisée"),
                    List.of(t("page.entree-clic", "<gray>Clic : refuser (les entonnoirs"), t("page.entree-clic-2",
                            "<gray>du train n'y déposent plus)")))
                    : Contenant.objet(Material.RED_DYE, t("page.entree-non", "<red>Entrée refusée"),
                    List.of(t("page.entree-clic-oui", "<gray>Clic : autoriser"))),
                    j -> reglerCoffre(j, id, rang, page, true));
            menu.poser(3, entonnoirs.sortie(coffre)
                    ? Contenant.objet(Material.LIME_DYE, t("page.sortie-oui", "<green>Sortie autorisée"),
                    List.of(t("page.sortie-clic", "<gray>Clic : refuser (les entonnoirs"), t("page.sortie-clic-2",
                            "<gray>du train n'y prennent plus)")))
                    : Contenant.objet(Material.RED_DYE, t("page.sortie-non", "<red>Sortie refusée"),
                    List.of(t("page.sortie-clic-oui", "<gray>Clic : autoriser"))),
                    j -> reglerCoffre(j, id, rang, page, false));
        } else if (estJukebox(wagon)) {
            // 1.3.0 : disque en cours et modes (maquette validée par LeKiwi06).
            ItemStack disque = jukebox.enCours(wagon);
            menu.poser(1, disque == null
                    ? Contenant.objet(Material.JUKEBOX, t("page.jukebox-silence", "<gray>Aucun disque en cours"),
                    List.of(t("page.jukebox-coffre", "<gray>Disques : coffre juste devant")))
                    : Contenant.objet(disque, Jukebox.chanson(disque).getDescription().color(
                    net.kyori.adventure.text.format.NamedTextColor.GREEN), List.of(t("page.jukebox-en-cours",
                    "<gray>En cours, rendu au coffre à la fin"))), null);
            byte mode = jukebox.mode(wagon);
            menu.poser(2, Contenant.objet(mode == Jukebox.LECTURE ? Material.LIME_DYE : Material.GRAY_DYE,
                    t("page.jukebox-lecture", "<white>Lecture"), List.of(t("page.jukebox-lecture-ligne",
                    "<gray>Dans l'ordre du coffre, en boucle"))), j -> modeJukebox(j, id, rang, page, Jukebox.LECTURE));
            menu.poser(3, Contenant.objet(mode == Jukebox.ALEATOIRE ? Material.LIME_DYE : Material.GRAY_DYE,
                    t("page.jukebox-aleatoire", "<white>Aléatoire"), List.of(t("page.jukebox-aleatoire-ligne",
                    "<gray>Un disque au hasard"))), j -> modeJukebox(j, id, rang, page, Jukebox.ALEATOIRE));
            menu.poser(4, Contenant.objet(mode == Jukebox.PAUSE ? Material.LIME_DYE : Material.GRAY_DYE,
                    t("page.jukebox-pause", "<white>Pause"), List.of(t("page.jukebox-pause-ligne",
                    "<gray>Le disque revient au coffre"))), j -> modeJukebox(j, id, rang, page, Jukebox.PAUSE));
        } else {
            menu.poser(3, icone(train, rang, false), null);
            if (estControleur(wagon)) {
                // 1.3.0 : klaxons du contrôleur.
                menu.poser(2, Contenant.objet(Material.GOAT_HORN, t("page.klaxons", "<white>Klaxons"),
                        List.of(t("page.klaxons-ligne", "<gray>Cornes de chèvre ; à bord :"),
                                t("page.klaxons-ligne-2", "<gray>touche de saut"))), j -> {
                    Train actuel = trainDuMenu(j, id);
                    if (actuel != null && rang < actuel.wagons.size()) {
                        Minecart controleur = actuel.wagons.get(rang);
                        Contenant.apres(() -> klaxons.ouvrir(j, controleur));
                    }
                });
            }
        }
        int coupe = Math.max(1, rang);
        int caseDeteler = estJukebox(wagon) ? 5 : 4;
        menu.poser(caseDeteler, Contenant.objet(Material.SHEARS, t("page.deteler", "<yellow>Dételer ici"), rang == 0
                ? List.of(t("page.deteler-tete", "<gray>Le sépare du wagon suivant"))
                : List.of(t("page.deteler-ligne-1", "<gray>Ce wagon et ceux de derrière"),
                t("page.deteler-ligne-2", "<gray>forment un autre train"))), j -> {
            Train actuel = trainDuMenu(j, id);
            if (actuel != null) {
                deteler(j, actuel, coupe);
                Contenant.fermer(j);
            }
        });
        menu.poser(caseDeteler + 1, Contenant.objet(Material.OAK_DOOR, t("page.retour", "<white>Retour"), List.of()),
                j -> menu(j, id, page));
        if (contenu(wagon)) {
            // 1.4.8 (LeKiwi06 : « qu'on puisse accéder à leurs inventaires aussi ») : coffre, entonnoir, four (carburant).
            menu.poser(6, Contenant.objet(Material.CHEST, t("page.contenu", "<white>Ouvrir le contenu"),
                    List.of(wagon instanceof PoweredMinecart ? t("page.contenu-four", "<gray>Case du carburant")
                            : t("page.contenu-ligne", "<gray>Comme un clic droit dessus"))),
                    j -> ouvrirContenu(j, id, rang));
        }
        String perso = nomWagon(wagon);
        menu.poser(7, Contenant.objet(Material.NAME_TAG, t("page.renommer", "<white>Renommer"),
                List.of(perso == null ? t("page.renommer-aucun", "<gray>Sans nom")
                        : t("page.renommer-actuel", "<gray>Nom : <nom>", "nom", perso))),
                j -> renommer(j, id, rang, page));
        menu.ouvrir(joueur);
    }

    /** 1.4.8 : wagon dont le contenu s'ouvre depuis sa page (coffre, entonnoir, four). */
    private static boolean contenu(Minecart wagon) {
        return wagon instanceof StorageMinecart || wagon instanceof HopperMinecart || wagon instanceof PoweredMinecart;
    }

    /** 1.4.8 : nom donné au wagon dans le menu du train, ou null. */
    private String nomWagon(Entity wagon) {
        String nom = wagon.getPersistentDataContainer().get(cleNomWagon, PersistentDataType.STRING);
        return nom == null || nom.isBlank() ? null : nom;
    }

    /**
     * 1.4.8 : ouvre le contenu d'un wagon depuis sa page. Wagon verrouillé (KS_VerrouVehicule) : son propriétaire
     * seulement, comme un clic droit.
     */
    private void ouvrirContenu(Player joueur, UUID id, int rang) {
        Train train = trainDuMenu(joueur, id);
        if (train == null || rang >= train.wagons.size()) {
            return;
        }
        Minecart wagon = train.wagons.get(rang);
        if (!maitreDuVerrou(joueur, wagon)) {
            message(joueur, t("page.contenu-verrou", "<red>Seul le propriétaire du verrou ouvre ce wagon"), true);
            return;
        }
        if (wagon instanceof PoweredMinecart four) {
            Contenant.apres(() -> foyers.ouvrir(joueur, four));
        } else if (wagon instanceof org.bukkit.inventory.InventoryHolder contenant) {
            Contenant.apres(() -> joueur.openInventory(contenant.getInventory()));
        }
    }

    /** 1.4.8 : fenêtre de saisie du nom d'un wagon (vide : sans nom), puis retour à sa page. */
    private void renommer(Player joueur, UUID id, int rang, int page) {
        Train train = trainDuMenu(joueur, id);
        if (train == null || rang >= train.wagons.size()) {
            return;
        }
        Minecart wagon = train.wagons.get(rang);
        String actuel = nomWagon(wagon);
        fr.kalium.menu.api.Gui.Click retour = j -> pageWagon(j, id, rang, page);
        io.papermc.paper.registry.data.dialog.ActionButton valider = gui.form(
                t("nom-wagon.valider", "<green>Valider"), null, (j, vue) -> {
                    Train actuelTrain = trainDuMenu(j, id);
                    if (actuelTrain == null || !actuelTrain.wagons.contains(wagon)) {
                        return;
                    }
                    String texte = vue.getText("nom") == null ? "" : vue.getText("nom").trim();
                    if (texte.length() > NOM_WAGON_MAX) {
                        texte = texte.substring(0, NOM_WAGON_MAX);
                    }
                    if (texte.isEmpty()) {
                        wagon.getPersistentDataContainer().remove(cleNomWagon);
                        message(j, t("nom-wagon.retire", "<yellow>Wagon sans nom"), false);
                    } else {
                        wagon.getPersistentDataContainer().set(cleNomWagon, PersistentDataType.STRING, texte);
                        message(j, t("nom-wagon.ok", "<green>Wagon nommé : <nom>", "nom", texte), false);
                    }
                    pageWagon(j, id, rang, page);
                });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("nom-wagon.titre", "<gold>Nom du wagon <n>", "n", rang + 1),
                    List.of(t("nom-wagon.aide", "<gray>Vide : le wagon n'a plus de nom.")),
                    List.of(gui.text("nom", t("nom-wagon.champ", "Nom du wagon"), actuel == null ? "" : actuel,
                            NOM_WAGON_MAX)),
                    List.of(valider, gui.button(t("nom-wagon.retour", "<gray>Retour"), null, retour)),
                    gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    /** 1.3.0 : mode du wagon jukebox choisi dans sa page. */
    private void modeJukebox(Player joueur, UUID id, int rang, int page, byte mode) {
        Train train = trainDuMenu(joueur, id);
        if (train == null || rang >= train.wagons.size() || !estJukebox(train.wagons.get(rang))) {
            return;
        }
        jukebox.mode(train.wagons.get(rang), mode);
        message(joueur, jukebox.nomMode(mode), false);
        pageWagon(joueur, id, rang, page);
    }

    /** 1.2.0 : change l'entrée ou la sortie d'un wagon à coffre (verrouillé : son propriétaire seulement). */
    private void reglerCoffre(Player joueur, UUID id, int rang, int page, boolean entree) {
        Train train = trainDuMenu(joueur, id);
        if (train == null || rang >= train.wagons.size()
                || !(train.wagons.get(rang) instanceof StorageMinecart coffre)) {
            return;
        }
        if (!maitreDuVerrou(joueur, coffre)) {
            message(joueur, t("page.verrou", "<red>Seul le propriétaire du verrou règle ce coffre"), true);
            return;
        }
        if (entree) {
            entonnoirs.basculerEntree(coffre);
        } else {
            entonnoirs.basculerSortie(coffre);
        }
        pageWagon(joueur, id, rang, page);
    }

    /** 1.4.0 : boutons niveau du menu (refusés si un autre joueur conduit depuis le contrôleur). */
    private void boutonNiveau(Player joueur, UUID id, int page, int pas) {
        Train train = trainDuMenu(joueur, id);
        if (train == null) {
            return;
        }
        Player pilote = pilote(train);
        if (pilote != null && !pilote.equals(joueur)) {
            message(joueur, t("menu.pilote", "<red>Ce train est conduit depuis son wagon contrôleur"), true);
        } else {
            changerNiveau(joueur, train, pas);
        }
        menu(joueur, id, page);
    }

    private void basculer(Player joueur, UUID id, int page) {
        Train train = trainDuMenu(joueur, id);
        if (train == null) {
            return;
        }
        train.reprise = 0;
        Player conducteur = pilote(train);
        if (conducteur != null && !conducteur.equals(joueur)) {
            message(joueur, t("menu.pilote", "<red>Ce train est conduit depuis son wagon contrôleur"), true);
        } else if (train.niveau != 0) {
            train.niveau = 0;
            message(joueur, t("menu.arrete", "<yellow>Le train freine"), false);
        } else if (train.vitesse <= 0 && surPropulseurEteint(train)) {
            message(joueur, t("propulseur.repartir",
                    "<red>Rail propulseur éteint : alimente-le pour repartir"), true);
        } else if (vitesseMax(train) <= 0) {
            message(joueur, t("menu.pas-de-traction",
                    "<red>Pas assez de carburant ou de fours pour ce train"), true);
        } else {
            train.niveau = 1;
            message(joueur, t("menu.demarre", "<green>Le train démarre"), false);
        }
        menu(joueur, id, page);
    }
}
