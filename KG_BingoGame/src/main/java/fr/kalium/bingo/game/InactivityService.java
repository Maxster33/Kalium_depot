package fr.kalium.bingo.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inactivite en partie (0.3.0) - demande explicite de LeKiwi06, 24/09/2026 : un joueur qui ne fait AUCUNE action
 * pendant 5 minutes consecutives est expulse pour inactivite ; commence alors le compte a rebours d'un joueur
 * deconnecte (10 min, voir GameEndService.checkDisconnectionAbandons), au bout duquel c'est un abandon definitif.
 * "Si le joueur craft ou tue des mobs en etant statique, ce n'est pas de l'inactivite" : toute action compte
 * (se deplacer ou tourner la tete, casser / poser, interagir, cliquer dans un inventaire, fabriquer, frapper,
 * manger, ecrire dans le tchat).
 */
public final class InactivityService implements Listener {

    private final GameManager gameManager;
    private final Duration kickAfter;
    private final Map<UUID, Instant> lastAction = new ConcurrentHashMap<>();

    public InactivityService(GameManager gameManager, Duration kickAfter) {
        this.gameManager = gameManager;
        this.kickAfter = kickAfter;
    }

    private void touch(Player player) {
        lastAction.put(player.getUniqueId(), Instant.now());
    }

    /** A appeler chaque seconde. */
    public void tick() {
        Instant now = Instant.now();
        for (BingoGame game : gameManager.getActiveGames()) {
            if (game.getState() != GameState.IN_PROGRESS || game.isFrozen()) {
                continue;
            }
            for (BingoInstance instance : game.getInstances()) {
                for (UUID playerId : instance.getTeam().getPlayers()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || !player.isOnline() || instance.hasAbandoned(playerId)) {
                        continue;
                    }
                    Instant last = lastAction.computeIfAbsent(playerId, id -> now);
                    if (Duration.between(last, now).compareTo(kickAfter) >= 0) {
                        lastAction.remove(playerId);
                        player.kick(Component.text("Expulsé pour inactivité (" + kickAfter.toMinutes() + " min sans action).\n"
                                + "Reconnectez-vous dans les 10 minutes pour ne pas abandonner la partie.", NamedTextColor.YELLOW));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        lastAction.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        var from = event.getFrom();
        var to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()
                || from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {
            touch(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            touch(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            touch(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            touch(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent event) {
        touch(event.getPlayer());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        lastAction.put(event.getPlayer().getUniqueId(), Instant.now());
    }
}
