package fr.kalium.ecextension;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * KS_EC_Extension (demande de Maxster33, 28/09/2026) : le coffre de l'Ender a 6 lignes, comme un grand coffre.
 * Les 3 lignes du haut sont le coffre de l'Ender vanilla ; les 3 du bas (l'extension) sont bloquées (image de
 * barrière) et se débloquent une case à la fois en déposant une Clé de l'End dessus (la clé est consommée).
 *
 * Stockage : l'extension et les cases débloquées sont dans les données du joueur (PersistentDataContainer), le haut
 * reste le coffre de l'Ender vanilla. Le contenu est copié à l'ouverture et réécrit à la fermeture.
 *
 * 1.1.0 : fonctions pour l'ecsee de KS_AntiCheat (coffreOuvert, fermerCoffre, extension, casesDebloquees,
 * ecrireExtension, imageCaseBloquee) : l'ecsee travaille sur le vrai coffre, joueur empêché de l'ouvrir pendant ce
 * temps (avant : il travaillait sur sa copie en même temps, ce qui dupliquait les objets).
 *
 * Clé de l'End : livre de connaissances (aucun craft, pas de bloc) avec l'image de la clé des épreuves sinistre, qui
 * ne peut donc pas ouvrir les coffres-forts ; son clic droit vanilla est annulé. Autres plugins : creerCle()
 * (KS_Crafts, KS_KaliumGive).
 */
public final class KSECExtension extends JavaPlugin implements Listener {

    private static final int TAILLE = 27;

    private static NamespacedKey cleCle;
    private static NamespacedKey verrou;
    private static NamespacedKey contenu;
    private static NamespacedKey debloquees;

    /** Interface ouverte : joueur propriétaire et cases débloquées (bit i = case i de l'extension). */
    private static final class Coffre implements InventoryHolder {
        private final UUID joueur;
        private int masque;
        private Inventory inventaire;

        private Coffre(UUID joueur, int masque) {
            this.joueur = joueur;
            this.masque = masque;
        }

        private boolean debloquee(int index) {
            return (masque & (1 << index)) != 0;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    @Override
    public void onEnable() {
        cleCle = new NamespacedKey(this, "cle_de_l_end");
        verrou = new NamespacedKey(this, "case_bloquee");
        contenu = new NamespacedKey(this, "extension");
        debloquees = new NamespacedKey(this, "cases_debloquees");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Coffre coffre
                    && coffre.joueur.equals(player.getUniqueId())) {
                sauver(player, coffre);
                player.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------ Clé de l'End

    /** Une Clé de l'End. Nécessite que le plugin soit activé. */
    public static ItemStack creerCle() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Clé de l'End", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft("ominous_trial_key"));
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(cleCle, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean estCle(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleCle, PersistentDataType.BYTE);
    }

    /** Annule l'effet vanilla du livre de connaissances. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && estCle(event.getItem())) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        }
    }

    // ------------------------------------------------------------------ ouverture et fermeture

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST
                || !(event.getPlayer() instanceof Player player)
                || !event.getInventory().equals(player.getEnderChest())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                ouvrir(player);
            }
        });
    }

    private void ouvrir(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        Coffre coffre = new Coffre(player.getUniqueId(), data.getOrDefault(debloquees, PersistentDataType.INTEGER, 0));
        Inventory inventaire = Bukkit.createInventory(coffre, 2 * TAILLE, Component.translatable("container.enderchest"));
        coffre.inventaire = inventaire;
        ItemStack[] haut = player.getEnderChest().getContents();
        ItemStack[] bas = lireExtension(player);
        for (int i = 0; i < TAILLE; i++) {
            inventaire.setItem(i, i < haut.length ? haut[i] : null);
            inventaire.setItem(TAILLE + i, coffre.debloquee(i) ? bas[i] : caseBloquee());
        }
        player.openInventory(inventaire);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Coffre coffre && event.getPlayer() instanceof Player player
                && coffre.joueur.equals(player.getUniqueId())) {
            sauver(player, coffre);
        }
    }

    private static ItemStack[] lireExtension(Player player) {
        byte[] bytes = player.getPersistentDataContainer().get(contenu, PersistentDataType.BYTE_ARRAY);
        ItemStack[] bas = bytes == null ? new ItemStack[0] : ItemStack.deserializeItemsFromBytes(bytes);
        return Arrays.copyOf(bas, TAILLE);
    }

    /** Haut : coffre de l'Ender vanilla ; bas : cases débloquées dans les données du joueur. */
    private static void sauver(Player player, Coffre coffre) {
        Inventory inventaire = coffre.inventaire;
        ItemStack[] haut = new ItemStack[TAILLE];
        ItemStack[] bas = new ItemStack[TAILLE];
        for (int i = 0; i < TAILLE; i++) {
            haut[i] = inventaire.getItem(i);
            ItemStack item = coffre.debloquee(i) ? inventaire.getItem(TAILLE + i) : null;
            bas[i] = item == null ? ItemStack.empty() : item;
        }
        player.getEnderChest().setContents(haut);
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(contenu, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(bas));
        data.set(debloquees, PersistentDataType.INTEGER, coffre.masque);
    }

    // ------------------------------------------------------------------ 1.1.0 : pour l'anti-triche (ecsee)

    /**
     * 1.1.0 (KS_AntiCheat, correctif de duplication) : le joueur a-t-il son coffre de l'Ender ouvert (copie de ce
     * plugin) ? Pendant qu'il est ouvert, le vrai coffre ne doit pas être modifié par quelqu'un d'autre.
     */
    public static boolean coffreOuvert(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof Coffre coffre
                && coffre.joueur.equals(player.getUniqueId());
    }

    /** 1.1.0 : ferme le coffre de l'Ender du joueur s'il est ouvert (sa copie est enregistrée d'abord). */
    public static void fermerCoffre(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof Coffre coffre
                && coffre.joueur.equals(player.getUniqueId())) {
            sauver(player, coffre);
            player.closeInventory();
        }
    }

    /** 1.1.0 : les 27 cases de l'extension (null : vide). */
    public static ItemStack[] extension(Player player) {
        ItemStack[] bas = lireExtension(player);
        for (int i = 0; i < bas.length; i++) {
            if (bas[i] != null && bas[i].isEmpty()) {
                bas[i] = null;
            }
        }
        return bas;
    }

    /** 1.1.0 : cases débloquées (bit i = case i de l'extension). */
    public static int casesDebloquees(Player player) {
        return player.getPersistentDataContainer().getOrDefault(debloquees, PersistentDataType.INTEGER, 0);
    }

    /** 1.1.0 : réécrit l'extension (les cases bloquées restent vides). */
    public static void ecrireExtension(Player player, ItemStack[] contenuExtension) {
        int masque = casesDebloquees(player);
        ItemStack[] bas = new ItemStack[TAILLE];
        for (int i = 0; i < TAILLE; i++) {
            ItemStack item = (masque & (1 << i)) != 0 && i < contenuExtension.length ? contenuExtension[i] : null;
            bas[i] = item == null ? ItemStack.empty() : item;
        }
        player.getPersistentDataContainer().set(contenu, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(bas));
    }

    /** 1.1.0 : image d'une case bloquée (pour une vue du staff). */
    public static ItemStack imageCaseBloquee() {
        return caseBloquee();
    }

    // ------------------------------------------------------------------ cases bloquées

    private static ItemStack caseBloquee() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Case bloquée", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Dépose une Clé de l'End ici pour la débloquer.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(verrou, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** Index dans l'extension (0 à 26) si la case brute est une case bloquée de l'interface, sinon -1. */
    private static int caseBloqueeIndex(Coffre coffre, int rawSlot) {
        if (rawSlot < TAILLE || rawSlot >= 2 * TAILLE) {
            return -1;
        }
        int index = rawSlot - TAILLE;
        return coffre.debloquee(index) ? -1 : index;
    }

    private static void debloquer(Player player, Coffre coffre, int index) {
        coffre.masque |= 1 << index;
        coffre.inventaire.setItem(TAILLE + index, null);
        player.getPersistentDataContainer().set(debloquees, PersistentDataType.INTEGER, coffre.masque);
    }

    /** Retire une clé de l'objet tenu par la souris. */
    private static ItemStack moinsUne(ItemStack cursor) {
        if (cursor.getAmount() <= 1) {
            return null;
        }
        ItemStack reste = cursor.clone();
        reste.setAmount(cursor.getAmount() - 1);
        return reste;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Coffre coffre)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Double-clic : ne jamais ramasser les images des cases bloquées.
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && event.getCursor() != null
                && event.getCursor().getType() == Material.BARRIER) {
            event.setCancelled(true);
            return;
        }
        int index = caseBloqueeIndex(coffre, event.getRawSlot());
        if (index < 0) {
            return;
        }
        event.setCancelled(true);
        ItemStack cursor = event.getCursor();
        if ((event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) && estCle(cursor)) {
            debloquer(player, coffre, index);
            event.getView().setCursor(moinsUne(cursor));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Coffre coffre)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean bloquee = event.getRawSlots().stream().anyMatch(raw -> caseBloqueeIndex(coffre, raw) >= 0);
        if (!bloquee) {
            return;
        }
        event.setCancelled(true);
        // Clé déposée sur une seule case (petit mouvement de souris pendant le clic) : même effet qu'un clic.
        if (event.getRawSlots().size() == 1 && estCle(event.getOldCursor())) {
            int index = caseBloqueeIndex(coffre, event.getRawSlots().iterator().next());
            Bukkit.getScheduler().runTask(this, () -> {
                ItemStack cursor = player.getItemOnCursor();
                if (player.getOpenInventory().getTopInventory() == coffre.inventaire && !coffre.debloquee(index)
                        && estCle(cursor)) {
                    debloquer(player, coffre, index);
                    player.setItemOnCursor(moinsUne(cursor));
                }
            });
        }
    }
}
