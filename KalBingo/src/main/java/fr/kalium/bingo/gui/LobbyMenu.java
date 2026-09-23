package fr.kalium.bingo.gui;

import fr.kalium.bingo.world.LobbyCaptureService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Menu graphique (/menu), reserve aux operateurs (permission bingo.admin - voir
 * command.MenuCommand), pour capturer et modifier la salle d'attente Bingo sans avoir a
 * taper les sous-commandes texte de /bingoadmin lobby ... . Demande explicite de
 * l'utilisateur ("je ne comprends pas l'utilisation de la commande"). Reprend exactement les
 * memes actions, via LobbyCaptureService (partagee avec BingoAdminCommand, pas de logique
 * dupliquee) : rien de nouveau n'est possible ici qui ne l'etait deja en commande, seule la
 * presentation change.
 */
public final class LobbyMenu implements Listener {

    private static final String TITLE = "§8Salle d'attente Bingo";
    private static final int SLOT_TP = 0;
    private static final int SLOT_CORNER1 = 2;
    private static final int SLOT_CORNER2 = 3;
    private static final int SLOT_SPAWN = 4;
    private static final int SLOT_CAPTURE = 6;
    private static final int SLOT_INFO = 8;

    private final LobbyCaptureService captureService;

    public LobbyMenu(LobbyCaptureService captureService) {
        this.captureService = captureService;
    }

    public void open(Player player) {
        player.openInventory(build(player));
    }

    private Inventory build(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 9, TITLE);
        holder.inventory = inv;

        inv.setItem(SLOT_TP, item(Material.COMPASS, "§bAller dans la salle d'attente",
                "§7Vous téléporte dans le monde de la",
                "§7salle d'attente pour construire ou modifier."));

        inv.setItem(SLOT_CORNER1, cornerItem("Coin 1", captureService.isPos1Set(player)));
        inv.setItem(SLOT_CORNER2, cornerItem("Coin 2", captureService.isPos2Set(player)));
        inv.setItem(SLOT_SPAWN, cornerItem("Point d'apparition", captureService.isSpawnSet(player)));

        boolean ready = captureService.isPos1Set(player) && captureService.isPos2Set(player)
                && captureService.isSpawnSet(player);
        inv.setItem(SLOT_CAPTURE, item(ready ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                ready ? "§aCapturer la salle d'attente" : "§cCapturer la salle d'attente",
                ready ? "§7Capture le cuboïde entre les 2 coins et le" : "§7Définissez d'abord le coin 1, le coin 2",
                ready ? "§7republie sur tous les emplacements." : "§7et le point d'apparition ci-dessus."));

        boolean defined = captureService.isTemplateDefined();
        inv.setItem(SLOT_INFO, item(Material.BOOK, "§7Infos",
                "§7Salle d'attente : " + (defined ? "§adéfinie" : "§cnon définie"),
                "§7Emplacements configurés : §f" + captureService.slotCount()));

        return inv;
    }

    private ItemStack cornerItem(String label, boolean set) {
        return item(set ? Material.LIME_DYE : Material.GRAY_DYE,
                (set ? "§a" : "§c") + label + (set ? " ✔" : " ✘"),
                "§7Clique pour définir à ta position",
                "§7actuelle (dans la salle d'attente).",
                set ? "§8Recapturable à tout moment." : "");
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof Holder)) {
            return; // clic dans l'inventaire du joueur, pas dans le menu
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        switch (event.getSlot()) {
            case SLOT_TP -> {
                player.closeInventory();
                captureService.teleportToLobby(player);
                return;
            }
            case SLOT_CORNER1 -> captureService.setCorner1(player);
            case SLOT_CORNER2 -> captureService.setCorner2(player);
            case SLOT_SPAWN -> captureService.setSpawn(player);
            case SLOT_CAPTURE -> captureService.capture(player);
            case SLOT_INFO -> captureService.sendInfo(player);
            default -> {
                return;
            }
        }
        // Rafraichit le menu pour refleter le nouvel etat (coin defini, capture reussie, ...).
        player.openInventory(build(player));
    }

    /** Marqueur permettant de reconnaitre de maniere fiable les clics dans CE menu. */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
