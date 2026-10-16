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
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Entity;
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
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
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
 */
public final class KSTrains extends JavaPlugin implements Listener {

    private static final double PORTEE_MENU = 24;

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
    private Foyers foyers;
    private Entonnoirs entonnoirs;
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
        lang = new Lang(this);
        foyers = new Foyers(this);
        entonnoirs = new Entonnoirs(this, foyers);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(foyers, this);
        getServer().getPluginManager().registerEvents(entonnoirs, this);
        for (World monde : Bukkit.getWorlds()) {
            for (Minecart wagon : monde.getEntitiesByClass(Minecart.class)) {
                arriver(wagon);
            }
        }
        getServer().getScheduler().runTaskTimer(this, this::rouler, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, this::surveiller, 20L, 20L);
        creerLocomotive();
        creerControleur();
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        foyers.toutFermer();
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
     * casque, comparateur, capteur sculk calibré, lampe à redstone, ampoule de cuivre, levier). Nécessite que le plugin
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

    /** Un distributeur poserait un wagonnet ordinaire (ou un seul four) : la locomotive et le contrôleur y restent. */
    @EventHandler(ignoreCancelled = true)
    public void onDistribuer(BlockDispenseEvent event) {
        if (estLocomotive(event.getItem()) || estControleur(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** 1.1.0 : un wagon contrôleur cassé redonne un wagon contrôleur (le jeu lâcherait un wagonnet). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLacher(EntityDropItemEvent event) {
        if (estControleur(event.getEntity()) && event.getItemDrop().getItemStack().getType() == Material.MINECART
                && !estControleur(event.getItemDrop().getItemStack())) {
            event.getItemDrop().setItemStack(creerControleur());
        }
    }

    // ------------------------------------------------------------------ trains : enregistrement

    /** Wagons qui peuvent faire partie d'un train (pas de TNT, bloc de commande ni spawner). */
    private static boolean accepte(Entity entite) {
        return entite instanceof RideableMinecart || entite instanceof StorageMinecart
                || entite instanceof HopperMinecart || entite instanceof PoweredMinecart;
    }

    private boolean estLoco(Entity wagon) {
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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || (!locomotive && !estControleur(event.getItem()))) {
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        if (event.getHand() == EquipmentSlot.HAND && event.getClickedBlock() != null) {
            if (locomotive) {
                poser(event.getPlayer(), event.getClickedBlock(), event.getItem());
            } else {
                poserControleur(event.getPlayer(), event.getClickedBlock(), event.getItem());
            }
        }
    }

    /** 1.1.0 : le wagon contrôleur se pose comme un wagonnet, marqué. */
    private void poserControleur(Player joueur, Block rail, ItemStack objet) {
        if (!(rail.getBlockData() instanceof Rail donnees)) {
            return;
        }
        double hauteur = donnees.getShape().name().startsWith("ASCENDING") ? 0.5625 : 0.0625;
        Location place = rail.getLocation().add(0.5, hauteur, 0.5);
        if (!rail.getWorld().getNearbyEntities(place, 0.5, 0.5, 0.5, e -> e instanceof Minecart).isEmpty()) {
            Contenant.message(joueur, t("poser.occupe", "<red>Il y a déjà un wagonnet sur ces rails"), true);
            return;
        }
        place.setYaw(joueur.getLocation().getYaw());
        rail.getWorld().spawn(place, RideableMinecart.class,
                w -> w.getPersistentDataContainer().set(cleControleur, PersistentDataType.BYTE, (byte) 1));
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
        double espacement = reglage("espacement", 1.1);
        List<Location> places = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double recul = i * espacement;
            Block bloc = rail.getRelative((int) -Math.round(recul * dx), 0, (int) -Math.round(recul * dz));
            if (!(bloc.getBlockData() instanceof Rail donnees) || donnees.getShape() != droit) {
                Contenant.message(joueur, t("poser.rails", "<red>Pose-la sur 3 rails droits, dans le sens des rails"),
                        true);
                return;
            }
            Location place = rail.getLocation().add(0.5 - recul * dx, 0.0625, 0.5 - recul * dz);
            place.setDirection(new Vector(dx, 0, dz));
            places.add(place);
        }
        BoundingBox zone = BoundingBox.of(places.get(0), places.get(2)).expand(0.6, 0.5, 0.6);
        if (!rail.getWorld().getNearbyEntities(zone, e -> e instanceof Minecart).isEmpty()) {
            Contenant.message(joueur, t("poser.occupe", "<red>Il y a déjà un wagonnet sur ces rails"), true);
            return;
        }
        List<Minecart> wagons = new ArrayList<>();
        for (Location place : places) {
            wagons.add(rail.getWorld().spawn(place, PoweredMinecart.class,
                    w -> w.getPersistentDataContainer().set(cleLoco, PersistentDataType.BYTE, (byte) 1)));
        }
        former(wagons);
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            objet.setAmount(objet.getAmount() - 1);
        }
        rail.getWorld().playSound(places.get(0), Sound.BLOCK_ANVIL_PLACE, 0.6f, 1.4f);
        Contenant.message(joueur, t("poser.ok",
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
            Contenant.message(joueur, t("atteler.verrou", "<red>Seul le propriétaire du verrou attelle ce wagon"), true);
            return;
        }
        if (!surRail(wagon)) {
            Contenant.message(joueur, t("atteler.rails", "<red>Le wagon doit être sur des rails"), true);
            return;
        }
        Selection selection = selections.get(joueur.getUniqueId());
        if (selection == null || selection.fin() < tick || !selection.wagon().isValid()
                || selection.wagon().getUniqueId().equals(wagon.getUniqueId())) {
            selections.put(joueur.getUniqueId(), new Selection(wagon, tick + 300));
            Contenant.message(joueur, t("atteler.choisi", "<yellow>Clique avec la chaîne sur le wagon à atteler"), false);
            return;
        }
        selections.remove(joueur.getUniqueId());
        Minecart a = selection.wagon();
        if (!a.getWorld().equals(wagon.getWorld()) || a.getLocation().distanceSquared(wagon.getLocation()) > 6.25) {
            Contenant.message(joueur, t("atteler.loin", "<red>Les deux wagons doivent se toucher presque"), true);
            return;
        }
        Train ta = trainDe(a);
        Train tb = trainDe(wagon);
        if (ta != null && ta == tb) {
            Contenant.message(joueur, t("atteler.meme", "<red>Ces wagons sont déjà dans le même train"), true);
            return;
        }
        if (!arrete(a) || !arrete(wagon)) {
            Contenant.message(joueur, t("atteler.arret", "<red>Les wagons doivent être à l'arrêt"), true);
            return;
        }
        List<Minecart> la = ta == null ? List.of(a) : ta.wagons;
        List<Minecart> lb = tb == null ? List.of(wagon) : tb.wagons;
        boolean aTete = same(la.get(0), a);
        boolean aQueue = same(la.get(la.size() - 1), a);
        boolean bTete = same(lb.get(0), wagon);
        boolean bQueue = same(lb.get(lb.size() - 1), wagon);
        if (!(aTete || aQueue) || !(bTete || bQueue)) {
            Contenant.message(joueur, t("atteler.bout", "<red>On n'attelle qu'au bout d'un train"), true);
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
        Component refus = refusControleur(resultat);
        if (refus != null) {
            Contenant.message(joueur, refus, true);
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
        Contenant.message(joueur, t("atteler.ok", "<green>Attelé : train de <n> wagons", "n",
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
            Contenant.message(joueur, t("deteler.arret", "<red>Arrête le train d'abord"), true);
            return;
        }
        if (rang < 1 || rang >= train.wagons.size()) {
            return;
        }
        Minecart wagon = train.wagons.get(rang);
        retirer(train);
        former(new ArrayList<>(train.wagons.subList(0, rang)));
        former(new ArrayList<>(train.wagons.subList(rang, train.wagons.size())));
        wagon.getWorld().dropItemNaturally(wagon.getLocation(), new ItemStack(Material.IRON_CHAIN));
        wagon.getWorld().playSound(wagon.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 1f);
        Contenant.message(joueur, t("deteler.ok", "<green>Wagon dételé"), false);
    }

    /** Un wagon attelé ne se casse pas (il faut le dételer d'abord) : pas de train coupé en roulant. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFrapper(VehicleDamageEvent event) {
        if (trainDe(event.getVehicle()) != null) {
            event.setCancelled(true);
            if (event.getAttacker() instanceof Player joueur) {
                Contenant.message(joueur, t("casser", "<red>Dételle ce wagon (cisailles) pour le casser"), true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCasser(VehicleDestroyEvent event) {
        if (trainDe(event.getVehicle()) != null) {
            event.setCancelled(true);
        }
    }

    /** Les wagons d'un train ne sont ni poussés ni ne poussent (le plugin les place). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPousser(VehicleEntityCollisionEvent event) {
        if (trainDe(event.getVehicle()) != null || trainDe(event.getEntity()) != null) {
            event.setCancelled(true);
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

    private double vitesseMax(Train train) {
        int traction = traction(train);
        if (traction == 0) {
            return 0;
        }
        double vitesse = reglage("vitesse-max", 1.0)
                - reglage("perte-par-wagon", 0.1) * Math.max(0, train.wagons.size() - traction);
        return Math.max(0, vitesse);
    }

    /** 1.1.0 : touches tenues au dernier envoi, par joueur (pour reconnaître un nouvel appui). */
    private final Map<UUID, Touches> touches = new HashMap<>();

    private record Touches(boolean avancer, boolean reculer) {
    }

    /**
     * 1.1.0 : à bord du wagon contrôleur, un appui sur avancer monte d'un niveau, un appui sur reculer descend d'un
     * niveau ; à l'arrêt, reculer passe en marche arrière. Tenir la touche ne répète pas (choix de LeKiwi06).
     */
    @EventHandler
    public void onTouches(PlayerInputEvent event) {
        Player joueur = event.getPlayer();
        Input nouvelles = event.getInput();
        Touches avant = touches.put(joueur.getUniqueId(), new Touches(nouvelles.isForward(), nouvelles.isBackward()));
        boolean avancer = nouvelles.isForward() && (avant == null || !avant.avancer());
        boolean reculer = nouvelles.isBackward() && (avant == null || !avant.reculer());
        if (avancer == reculer || joueur.getVehicle() == null || !estControleur(joueur.getVehicle())) {
            return;
        }
        Train train = trainDe(joueur.getVehicle());
        if (train == null || !train.complet()) {
            return;
        }
        train.niveau = avancer ? Math.min(NIVEAUX, train.niveau + 1) : Math.max(-1, train.niveau - 1);
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
            arreter(train, t("arret.carburant", "<yellow>Le train s'arrête : plus assez de carburant"));
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

    /** 1.1.0 : niveau, vitesse voulue et vitesse actuelle (blocs/s), au-dessus de la barre d'objets du conducteur. */
    private void tableau(Player pilote, Train train, double max) {
        String actuelle = bs(train.vitesse);
        if (train.niveau == 0) {
            pilote.sendActionBar(train.vitesse > 0
                    ? t("pilote.freine", "<gray>Arrêt demandé <dark_gray>— actuel <a> blocs/s", "a", actuelle)
                    : t("pilote.arret", "<gray>Arrêt <dark_gray>— avancer : niveau 1, reculer : marche arrière"));
        } else if (train.niveau < 0) {
            pilote.sendActionBar(t("pilote.arriere", "<yellow>Marche arrière : <v> bloc/s <dark_gray>— actuel <a>",
                    "v", bs(reglage("marche-arriere-max", 0.05)), "a", actuelle));
        } else {
            double voulue = train.niveau * reglage("palier", 0.25);
            pilote.sendActionBar(voulue > max
                    ? t("pilote.plafond", "<green>Niveau <n>/4 <gray>: plafonné à <v> blocs/s <dark_gray>— actuel <a>",
                    "n", train.niveau, "v", bs(max), "a", actuelle)
                    : t("pilote.niveau", "<green>Niveau <n>/4 <gray>: <v> blocs/s <dark_gray>— actuel <a>",
                    "n", train.niveau, "v", bs(voulue), "a", actuelle));
        }
    }

    /**
     * 1.1.0 : carburant brûlé par bloc parcouru selon le niveau choisi (demande de LeKiwi06 : croisière au niveau 2 ;
     * niveau 1 -25 %, niveau 3 +25 %, niveau 4 +50 %). Marche arrière, et freinage jusqu'à l'arrêt (niveau 0) : comme
     * le niveau 1 (choix de Claude).
     */
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
        double espacement = reglage("espacement", 1.1);
        for (Train train : List.copyOf(trains.values())) {
            if (!train.complet()) {
                continue;
            }
            double max = vitesseMax(train);
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
            if (parcouru > 0) {
                for (Minecart wagon : train.wagons) {
                    if (wagon instanceof PoweredMinecart four && foyers.alimente(four)) {
                        foyers.consommer(four, parcouru * consommation(train.niveau));
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
            train.vitesse = train.vitesse < voulue ? Math.min(voulue, train.vitesse + reglage("acceleration", 0.02))
                    : Math.max(voulue, train.vitesse - reglage("freinage", 0.05));
            Vector deplacement = ici.clone().subtract(avant);
            train.avancer(ici, train.wagons.size() * espacement + 3);
            if (train.vitesse > 0) {
                train.arret = null;
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
            if (tick % Entonnoirs.RYTHME == 0) {
                entonnoirs.transferer(train);
            }
        }
    }

    /** Place chaque wagon derrière celui qui mène, sur son trajet : poussé vers sa place, ou replacé s'il en est loin. */
    private void placer(Train train, Vector devant, double espacement) {
        World monde = train.monde();
        Vector precedente = devant;
        for (int k = 1; k < train.wagons.size(); k++) {
            Minecart wagon = train.suiveur(k);
            Vector cible = train.point(devant, k * espacement);
            if (cible == null) {
                wagon.setVelocity(new Vector());
                continue;
            }
            Vector ecart = cible.clone().subtract(wagon.getLocation().toVector());
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
        train.niveau = 0;
        train.vitesse = 0;
        train.bloque = 0;
        if (roulait) {
            annoncer(train, raison);
        }
    }

    /** Message aux joueurs proches de la tête ou à bord. */
    private void annoncer(Train train, Component texte) {
        Location tete = train.tete().getLocation();
        for (Player joueur : train.monde().getPlayers()) {
            if (joueur.getLocation().distanceSquared(tete) < 32 * 32
                    || (joueur.getVehicle() != null && parWagon.containsKey(joueur.getVehicle().getUniqueId()))) {
                Contenant.message(joueur, texte, true);
            }
        }
    }

    /** Chaque seconde : fours allumés selon leur carburant, chunks gardés chargés autour des trains qui roulent. */
    private void surveiller() {
        selections.values().removeIf(selection -> selection.fin() < tick);
        for (Train train : List.copyOf(trains.values())) {
            if (!train.complet()) {
                continue;
            }
            for (Minecart wagon : train.wagons) {
                if (wagon instanceof PoweredMinecart four) {
                    foyers.allumer(four, foyers.alimente(four));
                }
            }
            Set<Long> voulus = new HashSet<>();
            if (train.vitesse > 0 || tick - train.arreteDepuis < 1200) {
                for (Minecart wagon : train.wagons) {
                    voulus.add(chunk(wagon.getLocation()));
                }
                Location devant = train.tete().getLocation().add(train.direction().multiply(16));
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
            Contenant.message(joueur, t("menu.perdu", "<red>Ce train a changé ou est trop loin"), true);
            Contenant.fermer(joueur);
            return null;
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
        Contenant menu = Contenant.pour(joueur, t("menu.titre", "Train de <n> wagons", "n", n), new Marque(id));
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
                t("menu.info-regle", "<gray>Fours alimentés : 5 (locomotive)"),
                t("menu.info-regle-2", "<gray>ou 10 wagons chacun"))), null);
        menu.poser(menu.bas(4), train.niveau != 0
                ? Contenant.objet(Material.RED_CONCRETE, t("menu.arret", "<red>Arrêter le train"), List.of())
                : Contenant.objet(Material.LIME_CONCRETE, t("menu.marche", "<green>Mettre en marche"),
                List.of(t("menu.marche-ligne", "<gray>Niveau 1 (5 blocs/s), vers l'avant"))), j -> basculer(j, id, p));
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
            nom = t("wagon.coffre", "<white><n>. Coffre", "n", numero);
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
        if (cliquable) {
            lignes.add(wagon instanceof StorageMinecart ? t("wagon.clic-coffre", "<gray>Clic : réglages, dételer")
                    : t("wagon.clic", "<gray>Clic : sa page (dételer)"));
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
        } else {
            menu.poser(3, icone(train, rang, false), null);
        }
        int coupe = Math.max(1, rang);
        menu.poser(4, Contenant.objet(Material.SHEARS, t("page.deteler", "<yellow>Dételer ici"), rang == 0
                ? List.of(t("page.deteler-tete", "<gray>Le sépare du wagon suivant"))
                : List.of(t("page.deteler-ligne-1", "<gray>Ce wagon et ceux de derrière"),
                t("page.deteler-ligne-2", "<gray>forment un autre train"))), j -> {
            Train actuel = trainDuMenu(j, id);
            if (actuel != null) {
                deteler(j, actuel, coupe);
                Contenant.fermer(j);
            }
        });
        menu.poser(5, Contenant.objet(Material.OAK_DOOR, t("page.retour", "<white>Retour"), List.of()),
                j -> menu(j, id, page));
        menu.ouvrir(joueur);
    }

    /** 1.2.0 : change l'entrée ou la sortie d'un wagon à coffre (verrouillé : son propriétaire seulement). */
    private void reglerCoffre(Player joueur, UUID id, int rang, int page, boolean entree) {
        Train train = trainDuMenu(joueur, id);
        if (train == null || rang >= train.wagons.size()
                || !(train.wagons.get(rang) instanceof StorageMinecart coffre)) {
            return;
        }
        if (!maitreDuVerrou(joueur, coffre)) {
            Contenant.message(joueur, t("page.verrou", "<red>Seul le propriétaire du verrou règle ce coffre"), true);
            return;
        }
        if (entree) {
            entonnoirs.basculerEntree(coffre);
        } else {
            entonnoirs.basculerSortie(coffre);
        }
        pageWagon(joueur, id, rang, page);
    }

    private void basculer(Player joueur, UUID id, int page) {
        Train train = trainDuMenu(joueur, id);
        if (train == null) {
            return;
        }
        if (pilote(train) != null) {
            Contenant.message(joueur, t("menu.pilote", "<red>Ce train est conduit depuis son wagon contrôleur"), true);
        } else if (train.niveau != 0) {
            train.niveau = 0;
            Contenant.message(joueur, t("menu.arrete", "<yellow>Le train freine"), false);
        } else if (vitesseMax(train) <= 0) {
            Contenant.message(joueur, t("menu.pas-de-traction",
                    "<red>Pas assez de carburant ou de fours pour ce train"), true);
        } else {
            train.niveau = 1;
            Contenant.message(joueur, t("menu.demarre", "<green>Le train démarre"), false);
        }
        menu(joueur, id, page);
    }
}
