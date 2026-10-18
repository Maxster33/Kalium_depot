package fr.kalium.trains;

import fr.kalium.menu.api.Contenant;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.3.0 (LeKiwi06 : « il faudrait aussi un conteneur sur le minecart contrôleur pour y mettre les cornes de chèvre,
 * histoire d'avoir des klaxons » ; réponse : 9 cases, corne choisie ; maquette validée).
 *
 * Contenant de 2 rangées : en haut, 9 vraies cases où l'on pose des cornes de chèvre (rien d'autre n'y entre) ; en bas,
 * sous chaque corne, une vitre : clic = cette corne devient le klaxon (vitre verte). À bord du wagon contrôleur, la
 * touche de saut fait sonner la corne choisie, entendue de loin comme une corne du jeu (portée et attente du jeu).
 * Les cornes sont enregistrées dans le wagon ; elles tombent s'il est cassé.
 */
final class Klaxons implements Listener {

    private static final int CASES = 9;

    private final KSTrains plugin;
    private final NamespacedKey cleCornes;
    private final NamespacedKey cleChoix;
    private final Map<UUID, Inventory> ouverts = new HashMap<>();
    /** Tick avant lequel le klaxon d'un contrôleur ne sonne pas de nouveau (attente de la corne). */
    private final Map<UUID, Long> attente = new HashMap<>();

    Klaxons(KSTrains plugin) {
        this.plugin = plugin;
        this.cleCornes = new NamespacedKey(plugin, "cornes");
        this.cleChoix = new NamespacedKey(plugin, "corne-choisie");
    }

    static final class Boite implements InventoryHolder {
        final UUID controleur;
        Inventory inventaire;

        Boite(UUID controleur) {
            this.controleur = controleur;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    static boolean corne(ItemStack objet) {
        return objet != null && objet.getType() == Material.GOAT_HORN;
    }

    // ------------------------------------------------------------------ cornes enregistrées

    ItemStack[] cornes(Entity controleur) {
        Inventory ouvert = ouverts.get(controleur.getUniqueId());
        ItemStack[] cornes = new ItemStack[CASES];
        if (ouvert != null) {
            for (int i = 0; i < CASES; i++) {
                cornes[i] = ouvert.getItem(i);
            }
            return cornes;
        }
        byte[] donnees = controleur.getPersistentDataContainer().get(cleCornes, PersistentDataType.BYTE_ARRAY);
        if (donnees != null) {
            ItemStack[] lues = ItemStack.deserializeItemsFromBytes(donnees);
            System.arraycopy(lues, 0, cornes, 0, Math.min(CASES, lues.length));
        }
        return cornes;
    }

    private void enregistrer(Entity controleur, Inventory inventaire) {
        ItemStack[] cornes = new ItemStack[CASES];
        for (int i = 0; i < CASES; i++) {
            ItemStack objet = inventaire.getItem(i);
            cornes[i] = objet == null ? ItemStack.empty() : objet;
        }
        controleur.getPersistentDataContainer().set(cleCornes, PersistentDataType.BYTE_ARRAY,
                ItemStack.serializeItemsAsBytes(cornes));
    }

    /**
     * 1.4.8 (LeKiwi06 : « quand on met une corne elle n'est plus prise en compte, ça garde le klaxon de base ») : la
     * corne choisie ; si sa case est vide (corne posée dans une autre case sans cliquer sa vitre, ou retirée), la
     * première corne posée ; -1 sans aucune corne (sifflet du train).
     */
    int choisie(Entity controleur, ItemStack[] cornes) {
        int choix = choix(controleur);
        if (corne(cornes[choix])) {
            return choix;
        }
        for (int i = 0; i < CASES; i++) {
            if (corne(cornes[i])) {
                return i;
            }
        }
        return -1;
    }

    /** 1.4.8 : instrument d'une corne, lu dans son composant du jeu (sinon dans ses métadonnées), ou null. */
    private static MusicInstrument instrument(ItemStack corne) {
        if (!corne(corne)) {
            return null;
        }
        MusicInstrument instrument = corne.getData(io.papermc.paper.datacomponent.DataComponentTypes.INSTRUMENT);
        if (instrument == null && corne.getItemMeta() instanceof MusicInstrumentMeta meta) {
            instrument = meta.getInstrument();
        }
        return instrument;
    }

    int choix(Entity controleur) {
        Integer choix = controleur.getPersistentDataContainer().get(cleChoix, PersistentDataType.INTEGER);
        return choix == null ? 0 : Math.max(0, Math.min(CASES - 1, choix));
    }

    /** Contrôleur cassé : ses cornes tombent. */
    void lacher(Entity controleur) {
        oublier(controleur.getUniqueId());
        for (ItemStack corne : cornes(controleur)) {
            if (corne != null && !corne.isEmpty()) {
                controleur.getWorld().dropItemNaturally(controleur.getLocation(), corne);
            }
        }
        controleur.getPersistentDataContainer().remove(cleCornes);
    }

    // ------------------------------------------------------------------ klaxon

    /**
     * 1.4.3 (LeKiwi06 : « mettre un klaxon par défaut en note block qui ressemble à un sifflet de train ») : accord de
     * 3 notes de flûte graves (fa dièse mineur), rejoué 6 fois en 1,2 seconde pour un coup de sifflet tenu. 1.4.6
     * (LeKiwi06 : « il manque un temps de pause pour vraiment avoir le tchou-tchou et pas tchouuuuu ») : deux coups
     * courts (3 fois en 0,3 seconde chacun) séparés d'une pause (0,45 seconde entre leurs débuts).
     */
    private static final float[] ACCORD = {0.5f, 0.595f, 0.749f};
    private static final int[] TEMPS = {0, 3, 6, 15, 18, 21};

    /** Touche de saut à bord : la corne choisie sonne (sans corne : sifflet du train). */
    void sonner(Player conducteur, Entity controleur) {
        long tick = Bukkit.getCurrentTick();
        Long fin = attente.get(controleur.getUniqueId());
        if (fin != null && tick < fin) {
            return;
        }
        int choisie = choisie(controleur, cornes(controleur));
        ItemStack corne = choisie < 0 ? null : cornes(controleur)[choisie];
        MusicInstrument instrument = instrument(corne);
        if (instrument == null) {
            siffler(controleur);
            attente.put(controleur.getUniqueId(), tick + TEMPS[TEMPS.length - 1] + 10);
            return;
        }
        sonner(controleur.getLocation(), instrument.getSound(), 1f);
        attente.put(controleur.getUniqueId(), tick + (long) Math.ceil(instrument.getDuration() * 20));
    }

    /** 1.4.3 : sifflet par défaut. */
    private void siffler(Entity controleur) {
        for (int temps : TEMPS) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!controleur.isValid()) {
                    return;
                }
                for (float note : ACCORD) {
                    sonner(controleur.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, note);
                }
            }, temps);
        }
    }

    /**
     * 1.4.6 (LeKiwi06 : « on n'entend pas le klaxon d'assez loin, le but c'est de prévenir d'un danger ») : avant, son
     * attaché au wagon contrôleur, que le jeu d'un joueur ignore quand ce wagon est trop loin pour lui être affiché.
     * Maintenant joué pour chaque joueur (voir KSTrains.entendre) : plein volume jusqu'à `son.plein-klaxon` (128 blocs),
     * puis baissant jusqu'à `son.portee-klaxon` (256). Catégorie « jukebox / blocs musicaux », comme une corne du jeu
     * (1.4.7, LeKiwi06 ; en 1.4.6 « créatures amicales »).
     */
    private void sonner(org.bukkit.Location lieu, Sound son, float hauteur) {
        plugin.entendre(lieu, son, org.bukkit.SoundCategory.RECORDS, 1.0, plugin.getConfig().getDouble("son.plein-klaxon", 128),
                plugin.getConfig().getDouble("son.portee-klaxon", 256), hauteur);
    }

    // ------------------------------------------------------------------ contenant

    void ouvrir(Player joueur, Entity controleur) {
        Inventory inventaire = ouverts.get(controleur.getUniqueId());
        if (inventaire == null) {
            Boite boite = new Boite(controleur.getUniqueId());
            inventaire = Bukkit.createInventory(boite, 2 * CASES, plugin.t("klaxon.titre", "Klaxons du train"));
            boite.inventaire = inventaire;
            ItemStack[] cornes = cornes(controleur);
            for (int i = 0; i < CASES; i++) {
                inventaire.setItem(i, cornes[i] == null || cornes[i].isEmpty() ? null : cornes[i]);
            }
            ouverts.put(controleur.getUniqueId(), inventaire);
            dessiner(inventaire, choisie(controleur, cornes(controleur)));
        }
        joueur.openInventory(inventaire);
    }

    /** Rangée du bas : vitre verte sous la corne choisie, grise sous les autres cornes, sombre sous une case vide. */
    private void dessiner(Inventory inventaire, int choix) {
        for (int i = 0; i < CASES; i++) {
            boolean corne = corne(inventaire.getItem(i));
            ItemStack vitre;
            if (i == choix && corne) {
                vitre = Contenant.objet(Material.LIME_STAINED_GLASS_PANE, plugin.t("klaxon.choisie",
                        "<green>Klaxon : cette corne"), List.of(plugin.t("klaxon.saut",
                        "<gray>À bord : touche de saut")));
            } else if (corne) {
                vitre = Contenant.objet(Material.GRAY_STAINED_GLASS_PANE, plugin.t("klaxon.choisir",
                        "<white>Choisir cette corne"), List.of());
            } else {
                vitre = Contenant.objet(Material.BLACK_STAINED_GLASS_PANE, plugin.t("klaxon.vide",
                        "<gray>Pose une corne de chèvre au-dessus"), List.of());
            }
            inventaire.setItem(CASES + i, vitre);
        }
    }

    private Entity controleur(Boite boite) {
        return Bukkit.getEntity(boite.controleur);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCliquer(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Boite boite)) {
            return;
        }
        Inventory haut = event.getView().getTopInventory();
        int place = event.getRawSlot();
        if (place >= CASES && place < 2 * CASES) {
            event.setCancelled(true);
            int corne = place - CASES;
            Entity controleur = controleur(boite);
            if (controleur != null && corne(haut.getItem(corne))) {
                controleur.getPersistentDataContainer().set(cleChoix, PersistentDataType.INTEGER, corne);
                dessiner(haut, corne);
            }
            return;
        }
        if (place >= 0 && place < CASES) {
            ItemStack pose = event.getClick() == ClickType.NUMBER_KEY
                    ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton())
                    : event.getClick() == ClickType.SWAP_OFFHAND
                    ? event.getWhoClicked().getInventory().getItemInOffHand() : event.getCursor();
            if (pose != null && !pose.isEmpty() && !corne(pose)) {
                event.setCancelled(true);
                return;
            }
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            ItemStack objet = event.getCurrentItem();
            if (corne(objet)) {
                for (int i = 0; i < CASES; i++) {
                    if (haut.getItem(i) == null || haut.getItem(i).isEmpty()) {
                        haut.setItem(i, objet.clone());
                        event.setCurrentItem(null);
                        break;
                    }
                }
            }
        } else if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        apresChangement(boite);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlisser(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Boite boite)) {
            return;
        }
        for (int place : event.getRawSlots()) {
            if (place < 2 * CASES && (place >= CASES || !corne(event.getOldCursor()))) {
                event.setCancelled(true);
                return;
            }
        }
        apresChangement(boite);
    }

    private void apresChangement(Boite boite) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity controleur = controleur(boite);
            if (controleur != null && boite.inventaire != null) {
                enregistrer(controleur, boite.inventaire);
                dessiner(boite.inventaire, choisie(controleur, cornes(controleur)));
            }
        });
    }

    @EventHandler
    public void onFermer(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Boite boite)) {
            return;
        }
        Entity controleur = controleur(boite);
        if (controleur != null) {
            enregistrer(controleur, boite.inventaire);
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (boite.inventaire.getViewers().isEmpty()) {
                ouverts.remove(boite.controleur);
            }
        });
    }

    void oublier(UUID controleur) {
        Inventory inventaire = ouverts.remove(controleur);
        if (inventaire != null) {
            Entity entite = Bukkit.getEntity(controleur);
            if (entite != null) {
                enregistrer(entite, inventaire);
            }
            for (var spectateur : new ArrayList<>(inventaire.getViewers())) {
                spectateur.closeInventory();
            }
        }
    }

    void toutFermer() {
        for (UUID controleur : List.copyOf(ouverts.keySet())) {
            oublier(controleur);
        }
    }

    /** Pour le menu : vide si aucune corne. */
    boolean aDesCornes(Entity controleur) {
        for (ItemStack corne : cornes(controleur)) {
            if (corne(corne)) {
                return true;
            }
        }
        return false;
    }
}
