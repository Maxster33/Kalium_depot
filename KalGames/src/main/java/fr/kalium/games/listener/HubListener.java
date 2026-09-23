package fr.kalium.games.listener;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.game.ItemService;
import fr.kalium.games.game.RaceInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Locale;
import java.util.Set;

/**
 * Objets verrouilles (menu des mini-jeux, menu de la partie, checkpoint) et protection du hub.
 * La protection ne concerne que les joueurs hors partie ; hub.protect = false la desactive.
 */
public final class HubListener implements Listener {

    /** Kits geres uniquement via le vote en partie (PlayerKits2 ne sert que de bibliotheque, voir KitLibrary). */
    private static final Set<String> BLOCKED_COMMANDS = Set.of("kit", "kits");

    private final KalGames plugin;

    public HubListener(KalGames plugin) {
        this.plugin = plugin;
    }

    /** Partie prete dans laquelle se trouve le joueur (null : hub, ou arene encore en preparation). */
    private GameInstance activeGame(Player player) {
        GameInstance game = plugin.instances().of(player);
        return game != null && game.ready() ? game : null;
    }

    private boolean protectedHub(Player player) {
        return plugin.getConfig().getBoolean("hub.protect", true)
                && activeGame(player) == null
                && !player.hasPermission("kalgames.bypass");
    }

    // ------------------------------------------------------------------ commandes

    /**
     * /kit (et /kits) donnent directement n'importe quel kit PlayerKits2, en dehors du vote et des regles d'une
     * partie : desactives pour les joueurs (le contenu des kits n'est utilise que comme bibliotheque, voir
     * KitLibrary). kalgames.bypass (staff) n'est pas concerne.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("kalgames.bypass")) {
            return;
        }
        String message = event.getMessage().substring(1);
        int space = message.indexOf(' ');
        String label = space < 0 ? message : message.substring(0, space);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        if (BLOCKED_COMMANDS.contains(label.toLowerCase(Locale.ROOT))) {
            event.setCancelled(true);
            plugin.tell(player, "cmd.kit-blocked",
                    "<red>Cette commande est désactivée : choisissez votre kit depuis le vote en partie.");
        }
    }

    // ------------------------------------------------------------------ objets

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        Player player = event.getPlayer();
        String kind = plugin.items().kind(event.getItem());
        if (kind != null && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            switch (kind) {
                case ItemService.GAMES, ItemService.GAME -> plugin.menus().openMenuFor(player);
                case ItemService.CHECKPOINT -> {
                    GameInstance game = plugin.instances().of(player);
                    if (game instanceof RaceInstance race) {
                        race.useCheckpointItem(player);
                    }
                }
                default -> {
                }
            }
            return;
        }
        if (kind != null) {
            event.setCancelled(true);
            return;
        }
        if (action == Action.PHYSICAL) {
            if (protectedHub(player) && plugin.hub().isHubWorld(player.getWorld())) {
                event.setCancelled(true);
            }
            return;
        }
        GameInstance game = activeGame(player);
        if (game != null) {
            if (game.frozen(player)) {
                event.setCancelled(true);
                return;
            }
            Block clicked = event.getClickedBlock();
            if (action == Action.RIGHT_CLICK_BLOCK && clicked != null) {
                if (game.canInteract(player)) {
                    // Portes, leviers... : l'etat d'origine est memorise puis restaure en fin de match.
                    game.track(clicked);
                    game.track(clicked.getRelative(BlockFace.UP));
                    game.track(clicked.getRelative(BlockFace.DOWN));
                } else {
                    event.setCancelled(true);
                }
            } else if (game.inventoryLocked(player) && event.getItem() != null) {
                event.setCancelled(true);
            }
            return;
        }
        if (protectedHub(player) && plugin.hub().isHubWorld(player.getWorld()) && event.getClickedBlock() != null
                && action == Action.RIGHT_CLICK_BLOCK && isBlocked(event.getClickedBlock().getType())) {
            event.setCancelled(true);
        }
    }

    private boolean isBlocked(org.bukkit.Material material) {
        String name = material.name();
        return name.endsWith("_BED") || name.contains("CHEST") || name.contains("SHULKER") || name.equals("BARREL")
                || name.equals("FURNACE") || name.equals("BLAST_FURNACE") || name.equals("SMOKER") || name.equals("ANVIL")
                || name.equals("HOPPER") || name.equals("DROPPER") || name.equals("DISPENSER") || name.equals("BREWING_STAND")
                || name.equals("ENDER_CHEST") || name.equals("LECTERN") || name.equals("COMPOSTER");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.items().isOurs(event.getItemDrop().getItemStack()) || lockedInventory(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (plugin.items().isOurs(event.getMainHandItem()) || plugin.items().isOurs(event.getOffHandItem()) || lockedInventory(player)) {
            event.setCancelled(true);
        }
    }

    private boolean lockedInventory(Player player) {
        if (player.hasPermission("kalgames.bypass") && plugin.instances().of(player) == null) {
            return false;
        }
        GameInstance game = activeGame(player);
        if (game != null) {
            return game.inventoryLocked(player);
        }
        return plugin.getConfig().getBoolean("hub.protect", true) && plugin.hub().isHubWorld(player.getWorld());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.items().isOurs(event.getCurrentItem()) || plugin.items().isOurs(event.getCursor())
                || (event.getClick().isKeyboardClick() && event.getHotbarButton() >= 0
                && plugin.items().isOurs(player.getInventory().getItem(event.getHotbarButton())))) {
            event.setCancelled(true);
            return;
        }
        if (lockedInventory(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && (plugin.items().isOurs(event.getOldCursor()) || lockedInventory(player))) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ protection du hub

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (protectedHub(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        if (protectedHub(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (protectedHub(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (protectedHub(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player && protectedHub(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (protectedHub(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (protectedHub(player) && plugin.hub().isHubWorld(player.getWorld())
                && event.getRightClicked() instanceof org.bukkit.entity.Hanging) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && activeGame(player) == null
                && plugin.getConfig().getBoolean("hub.protect", true)
                && (plugin.hub().isHubWorld(player.getWorld()) || player.getWorld() == plugin.worlds().world())) {
            event.setCancelled(true);
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                player.teleport(plugin.hub().hubLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && activeGame(player) == null
                && plugin.getConfig().getBoolean("hub.protect", true) && plugin.hub().isHubWorld(player.getWorld())) {
            event.setCancelled(true);
        }
    }
}
