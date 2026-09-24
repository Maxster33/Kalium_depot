package fr.kalium.bingo.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Clic droit sur l'objet "Objectifs" (GameItems) -&gt; ouvre GameMenu ; empeche UNIQUEMENT de le
 * jeter (PlayerDropItemEvent) - demande explicite de l'utilisateur : contrairement a
 * LobbyItems/LobbyProtectionListener (objet de la salle d'attente totalement verrouille), celui-ci
 * reste deplacable/rangeable librement dans l'inventaire ("de manière à pouvoir libérer la case si
 * on le souhaite mais qu'on ne puisse pas le jeter").
 *
 * 0.3.0 - demande explicite de LeKiwi06 (24/09/2026) : "assure moi qu'on ne peut pas crafter avec le papier
 * qui sert d'interface". Le papier ne peut plus quitter l'inventaire du joueur : ni grille d'artisanat (2x2
 * ou etabli), ni coffre, four, villageois, table de cartographie... (toute case du haut d'un inventaire
 * ouvert), ni cadre / allay / autre entite, ni perdu a la mort. Filet de securite : un artisanat qui le
 * contiendrait quand meme n'a pas de resultat. Il reste deplacable librement dans l'inventaire du joueur.
 */
public final class GameItemListener implements Listener {

    private final GameItems gameItems;
    private final GameMenu gameMenu;

    public GameItemListener(GameItems gameItems, GameMenu gameMenu) {
        this.gameItems = gameItems;
        this.gameMenu = gameMenu;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !gameItems.isOurs(event.getItem())) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            gameMenu.open(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (gameItems.isOurs(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Empeche de poser le papier dans une case du HAUT de l'inventaire ouvert (grille 2x2 comprise). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        boolean clickedTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);
        if (clickedTop) {
            ItemStack incoming = switch (event.getClick()) {
                case NUMBER_KEY -> event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                case SWAP_OFFHAND -> event.getWhoClicked().getInventory().getItemInOffHand();
                default -> event.getCursor();
            };
            if (gameItems.isOurs(incoming)) {
                event.setCancelled(true);
            }
            return;
        }
        // Clic-maj depuis l'inventaire du joueur : envoie l'objet dans l'inventaire du haut, sauf quand seul
        // l'inventaire du joueur est ouvert (type CRAFTING) : il reste alors entre barre d'action et reserve.
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && top.getType() != InventoryType.CRAFTING
                && gameItems.isOurs(event.getCurrentItem())) {
            event.setCancelled(true);
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK && gameItems.isOurs(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!gameItems.isOurs(event.getOldCursor())) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Filet de securite : aucun resultat d'artisanat si le papier est dans la grille. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (gameItems.isOurs(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    /** Pas de papier dans un cadre, ni donne a un allay / un villageois (clic droit sur une entite). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack hand = event.getHand() == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItemInMainHand();
        if (gameItems.isOurs(hand)) {
            event.setCancelled(true);
        }
    }

    /** Jamais lache a la mort (si keepInventory venait a manquer) ; rendu a la reapparition. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(gameItems::isOurs);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        var player = event.getPlayer();
        for (ItemStack item : player.getInventory().getContents()) {
            if (gameItems.isOurs(item)) {
                return;
            }
        }
        if (gameItems.isInGame(player)) {
            gameItems.give(player);
        }
    }
}
