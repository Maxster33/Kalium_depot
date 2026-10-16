package fr.kalium.trains;

import fr.kalium.menu.api.Contenant;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Carburant des wagonnets à four d'un train (cahier, règles 6 et 7) : chaque four a une réserve (en blocs à parcourir)
 * et une case de carburant. 1 charbon = 80 blocs : un carburant vaut sa durée de cuisson (ticks) / 20 blocs. Un seau
 * de lave mis dans la case donne 1 000 blocs et y laisse le seau vide, qui bloque la case jusqu'à ce qu'on le retire.
 */
final class Foyers implements Listener {

    /** Case du carburant dans l'interface d'un four (petit coffre d'une rangée). */
    static final int CASE = 4;
    private static final int CASE_RESERVE = 3;

    private final KSTrains plugin;
    private final NamespacedKey cleReserve;
    private final NamespacedKey cleCarburant;

    /** Interfaces ouvertes, par four (plusieurs joueurs partagent la même). */
    private final Map<UUID, Inventory> ouvertes = new HashMap<>();

    Foyers(KSTrains plugin) {
        this.plugin = plugin;
        this.cleReserve = new NamespacedKey(plugin, "reserve");
        this.cleCarburant = new NamespacedKey(plugin, "carburant");
    }

    /** Interface d'un four : reconnue par son détenteur. */
    static final class Foyer implements InventoryHolder {
        final UUID four;
        Inventory inventaire;

        Foyer(UUID four) {
            this.four = four;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    // ------------------------------------------------------------------ valeurs des carburants

    /**
     * Durée de cuisson d'un objet en ticks (valeurs du jeu), 0 si ce n'est pas un carburant. Un carburant du jeu absent
     * de la table vaut 300 ticks (la plupart sont des objets en bois).
     */
    static int cuisson(Material m) {
        switch (m) {
            case LAVA_BUCKET: return 20000;
            case COAL_BLOCK: return 16000;
            case DRIED_KELP_BLOCK: return 4001;
            case BLAZE_ROD: return 2400;
            case COAL: case CHARCOAL: return 1600;
            case STICK: case BOWL: case DEAD_BUSH: case AZALEA: case FLOWERING_AZALEA: return 100;
            case BAMBOO: case SCAFFOLDING: return 50;
            default: break;
        }
        String nom = m.name();
        if (nom.endsWith("_BOAT") || nom.endsWith("_RAFT")) {
            return 1200;
        }
        if (nom.endsWith("_HANGING_SIGN")) {
            return 800;
        }
        if (nom.endsWith("_SIGN") || Tag.WOODEN_DOORS.isTagged(m) || nom.startsWith("WOODEN_")) {
            return 200;
        }
        if (Tag.WOODEN_SLABS.isTagged(m)) {
            return 150;
        }
        if (Tag.SAPLINGS.isTagged(m) || Tag.WOOL.isTagged(m) || Tag.WOODEN_BUTTONS.isTagged(m)) {
            return 100;
        }
        if (Tag.WOOL_CARPETS.isTagged(m)) {
            return 67;
        }
        return m.isFuel() ? 300 : 0;
    }

    static boolean carburant(ItemStack objet) {
        return objet != null && !objet.isEmpty() && cuisson(objet.getType()) > 0;
    }

    // ------------------------------------------------------------------ réserve et case

    double reserve(PoweredMinecart four) {
        Double reserve = four.getPersistentDataContainer().get(cleReserve, PersistentDataType.DOUBLE);
        return reserve == null ? 0 : reserve;
    }

    private void reserve(PoweredMinecart four, double blocs) {
        four.getPersistentDataContainer().set(cleReserve, PersistentDataType.DOUBLE, Math.max(0, blocs));
    }

    /** Objet de la case (l'interface ouverte fait foi). */
    ItemStack caseCarburant(PoweredMinecart four) {
        Inventory ouverte = ouvertes.get(four.getUniqueId());
        if (ouverte != null) {
            return ouverte.getItem(CASE);
        }
        byte[] donnees = four.getPersistentDataContainer().get(cleCarburant, PersistentDataType.BYTE_ARRAY);
        return donnees == null ? null : ItemStack.deserializeBytes(donnees);
    }

    private void caseCarburant(PoweredMinecart four, ItemStack objet) {
        Inventory ouverte = ouvertes.get(four.getUniqueId());
        if (ouverte != null) {
            ouverte.setItem(CASE, objet);
        }
        enregistrer(four, objet);
    }

    private void enregistrer(Entity four, ItemStack objet) {
        PersistentDataContainer donnees = four.getPersistentDataContainer();
        if (objet == null || objet.isEmpty()) {
            donnees.remove(cleCarburant);
        } else {
            donnees.set(cleCarburant, PersistentDataType.BYTE_ARRAY, objet.serializeAsBytes());
        }
    }

    /** Vrai si le four a une réserve, ou un carburant brûlable dans sa case (pas un seau vide). */
    boolean alimente(PoweredMinecart four) {
        return reserve(four) > 0 || carburant(caseCarburant(four));
    }

    /**
     * Le train a parcouru {@code blocs} : la réserve baisse ; vide, le four brûle un objet de sa case (seau de lave :
     * le seau vide reste dans la case).
     */
    void consommer(PoweredMinecart four, double blocs) {
        double reserve = reserve(four) - blocs;
        while (reserve <= 0) {
            ItemStack objet = caseCarburant(four);
            if (!carburant(objet)) {
                reserve = 0;
                break;
            }
            reserve += cuisson(objet.getType()) / 20.0;
            if (objet.getType() == Material.LAVA_BUCKET) {
                objet = new ItemStack(Material.BUCKET);
            } else {
                objet = objet.clone();
                objet.setAmount(objet.getAmount() - 1);
            }
            caseCarburant(four, objet.isEmpty() ? null : objet);
        }
        reserve(four, reserve);
        Inventory ouverte = ouvertes.get(four.getUniqueId());
        if (ouverte != null) {
            ouverte.setItem(CASE_RESERVE, infoReserve(four));
        }
    }

    /**
     * 1.2.0 : un wagon à entonnoir verse un seau de lave : accepté seulement si la réserve du four est vide (règle du
     * cahier) ; la réserve prend 1 000 blocs.
     */
    boolean verserLave(PoweredMinecart four) {
        if (reserve(four) > 0) {
            return false;
        }
        reserve(four, cuisson(Material.LAVA_BUCKET) / 20.0);
        Inventory ouverte = ouvertes.get(four.getUniqueId());
        if (ouverte != null) {
            ouverte.setItem(CASE_RESERVE, infoReserve(four));
        }
        return true;
    }

    /**
     * 1.2.0 : un wagon à entonnoir donne un carburant (un seul, pas un seau de lave) : il va dans la case, si elle est
     * vide ou contient le même carburant avec de la place.
     */
    boolean ajouter(PoweredMinecart four, ItemStack objet) {
        if (!carburant(objet) || objet.getType() == Material.LAVA_BUCKET) {
            return false;
        }
        ItemStack dedans = caseCarburant(four);
        ItemStack un = objet.clone();
        un.setAmount(1);
        if (dedans == null || dedans.isEmpty()) {
            caseCarburant(four, un);
            return true;
        }
        if (dedans.isSimilar(un) && dedans.getAmount() < dedans.getMaxStackSize()) {
            ItemStack plus = dedans.clone();
            plus.setAmount(dedans.getAmount() + 1);
            caseCarburant(four, plus);
            return true;
        }
        return false;
    }

    /** Four allumé (fumée, face éclairée) tant qu'il est alimenté ; il ne pousse pas de lui-même (poussée nulle). */
    void allumer(PoweredMinecart four, boolean allume) {
        four.setPushX(0);
        four.setPushZ(0);
        four.setFuel(allume ? 60 : 0);
    }

    // ------------------------------------------------------------------ interface

    void ouvrir(Player joueur, PoweredMinecart four) {
        Inventory inventaire = ouvertes.get(four.getUniqueId());
        if (inventaire == null) {
            Foyer foyer = new Foyer(four.getUniqueId());
            inventaire = Bukkit.createInventory(foyer, 9, plugin.t("four.titre", "Four du train"));
            foyer.inventaire = inventaire;
            for (int place = 0; place < 9; place++) {
                inventaire.setItem(place, Contenant.decor());
            }
            inventaire.setItem(CASE, caseCarburant(four));
            ouvertes.put(four.getUniqueId(), inventaire);
            inventaire.setItem(CASE_RESERVE, infoReserve(four));
        }
        joueur.openInventory(inventaire);
    }

    private ItemStack infoReserve(PoweredMinecart four) {
        long blocs = Math.round(reserve(four));
        return Contenant.objet(Material.FURNACE, plugin.t("four.reserve", "<gold>Réserve : <blocs> blocs", "blocs", blocs),
                List.of(plugin.t("four.regle-1", "<gray>1 charbon = 80 blocs"),
                        plugin.t("four.regle-2", "<gray>Carburant dans la case de droite")));
    }

    private PoweredMinecart four(Foyer foyer) {
        Entity entite = Bukkit.getEntity(foyer.four);
        return entite instanceof PoweredMinecart four ? four : null;
    }

    /** Seule la case du carburant se remplit ou se vide, et seulement avec un carburant. */
    @EventHandler(ignoreCancelled = true)
    public void onCliquer(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Foyer foyer)) {
            return;
        }
        int place = event.getRawSlot();
        if (place >= 0 && place < 9) {
            if (place != CASE) {
                event.setCancelled(true);
                return;
            }
            ItemStack pose = event.getClick() == ClickType.NUMBER_KEY
                    ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton())
                    : event.getClick() == ClickType.SWAP_OFFHAND
                    ? event.getWhoClicked().getInventory().getItemInOffHand() : event.getCursor();
            if (pose != null && !pose.isEmpty() && !carburant(pose)) {
                event.setCancelled(true);
                return;
            }
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // Clic avec Maj depuis l'inventaire du joueur : seulement un carburant, vers la case.
            event.setCancelled(true);
            ItemStack objet = event.getCurrentItem();
            Inventory haut = event.getView().getTopInventory();
            ItemStack dedans = haut.getItem(CASE);
            if (!carburant(objet)) {
                return;
            }
            if (dedans == null || dedans.isEmpty()) {
                haut.setItem(CASE, objet.clone());
                event.setCurrentItem(null);
            } else if (dedans.isSimilar(objet)) {
                int place2 = Math.min(objet.getAmount(), dedans.getMaxStackSize() - dedans.getAmount());
                if (place2 <= 0) {
                    return;
                }
                dedans.setAmount(dedans.getAmount() + place2);
                objet.setAmount(objet.getAmount() - place2);
                event.setCurrentItem(objet.isEmpty() ? null : objet);
            }
        } else if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        apresChangement(foyer);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlisser(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Foyer foyer)) {
            return;
        }
        for (int place : event.getRawSlots()) {
            if (place < 9 && (place != CASE || !carburant(event.getOldCursor()))) {
                event.setCancelled(true);
                return;
            }
        }
        apresChangement(foyer);
    }

    /** La case est enregistrée dans le four au tick suivant (après que le clic a eu lieu). */
    private void apresChangement(Foyer foyer) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PoweredMinecart four = four(foyer);
            if (four != null && foyer.inventaire != null) {
                enregistrer(four, foyer.inventaire.getItem(CASE));
            }
        });
    }

    @EventHandler
    public void onFermer(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Foyer foyer)) {
            return;
        }
        PoweredMinecart four = four(foyer);
        if (four != null) {
            enregistrer(four, foyer.inventaire.getItem(CASE));
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (foyer.inventaire.getViewers().isEmpty()) {
                ouvertes.remove(foyer.four);
            }
        });
    }

    /** Four retiré du monde : son interface se ferme (le contenu de la case est déjà enregistré). */
    void oublier(UUID four) {
        Inventory inventaire = ouvertes.remove(four);
        if (inventaire != null) {
            Entity entite = Bukkit.getEntity(four);
            if (entite != null) {
                enregistrer(entite, inventaire.getItem(CASE));
            }
            for (var spectateur : List.copyOf(inventaire.getViewers())) {
                spectateur.closeInventory();
            }
        }
    }

    /** Arrêt du plugin : chaque interface ouverte est enregistrée et fermée. */
    void toutFermer() {
        for (UUID four : List.copyOf(ouvertes.keySet())) {
            oublier(four);
        }
    }
}
