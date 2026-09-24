package fr.kalium.games.listener;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Iterator;

/** Regles des parties : blocs (suivis puis restaures), degats, deplacements, vehicules. */
public final class GameListener implements Listener {

    private final KalGames plugin;

    public GameListener(KalGames plugin) {
        this.plugin = plugin;
    }

    private boolean inGameWorld(Location location) {
        return location.getWorld() != null && location.getWorld() == plugin.worlds().world();
    }

    private boolean bypass(Player player) {
        return player.hasPermission("kalgames.bypass") && plugin.instances().of(player) == null;
    }

    // ------------------------------------------------------------------ blocs

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!inGameWorld(block.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(player);
        if (game == null) {
            if (!bypass(player)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!game.canBuild(player) || !game.inBounds(block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof BlockMultiPlaceEvent multi) {
            for (BlockState state : multi.getReplacedBlockStates()) {
                if (!game.inBounds(state.getX(), state.getY(), state.getZ())) {
                    event.setCancelled(true);
                    return;
                }
            }
            for (BlockState state : multi.getReplacedBlockStates()) {
                game.trackOriginal(state.getX(), state.getY(), state.getZ(), state.getBlockData());
                game.markPlaced(state.getBlock());
            }
        } else {
            BlockState replaced = event.getBlockReplacedState();
            game.trackOriginal(replaced.getX(), replaced.getY(), replaced.getZ(), replaced.getBlockData());
            game.markPlaced(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!inGameWorld(block.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(player);
        if (game == null) {
            if (!bypass(player)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!game.canBreak(player, block)) {
            event.setCancelled(true);
            return;
        }
        // Les blocs accroches (torches, portes, plantes hautes, rails...) disparaissent avec celui-ci.
        game.track(block);
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN, org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST}) {
            game.track(block.getRelative(face));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLaunch(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player shooter) {
            GameInstance game = plugin.instances().of(shooter);
            if (game != null && game.frozen(shooter)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        GameInstance game = plugin.instances().of(event.getPlayer());
        if (game != null && (game.frozen(event.getPlayer()) || game.inventoryLocked(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlock();
        if (!inGameWorld(block.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(event.getPlayer());
        if (game == null || !game.canBuild(event.getPlayer()) || !game.inBounds(block.getX(), block.getY(), block.getZ())) {
            if (game != null || !bypass(event.getPlayer())) {
                event.setCancelled(true);
            }
            return;
        }
        game.track(block);
        game.markPlaced(block);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block block = event.getBlock();
        if (!inGameWorld(block.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(event.getPlayer());
        if (game == null || !game.canBuild(event.getPlayer()) || !game.inBounds(block.getX(), block.getY(), block.getZ())) {
            if (game != null || !bypass(event.getPlayer())) {
                event.setCancelled(true);
            }
            return;
        }
        game.track(block);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!inGameWorld(event.getLocation())) {
            return;
        }
        handleExplosion(event.getLocation(), event.blockList());
        event.setYield(0f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!inGameWorld(event.getBlock().getLocation())) {
            return;
        }
        handleExplosion(event.getBlock().getLocation(), event.blockList());
        event.setYield(0f);
    }

    private void handleExplosion(Location origin, java.util.List<Block> blocks) {
        GameInstance game = plugin.instances().at(origin);
        if (game == null || !hasBuilder(game)) {
            // Hors combat : aucune destruction.
            blocks.clear();
            return;
        }
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!game.inBounds(block.getX(), block.getY(), block.getZ())) {
                iterator.remove();
            } else {
                game.track(block);
            }
        }
    }

    /** Les explosions ne detruisent des blocs que pendant un combat (un joueur peut construire). */
    private boolean hasBuilder(GameInstance game) {
        for (Player player : game.onlineMembers()) {
            if (game.canBuild(player)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFlow(BlockFromToEvent event) {
        Block to = event.getToBlock();
        if (!inGameWorld(to.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().at(to.getLocation());
        if (game == null || !game.inBounds(to.getX(), to.getY(), to.getZ())) {
            event.setCancelled(true);
            return;
        }
        game.track(to);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onForm(BlockFormEvent event) {
        Block block = event.getBlock();
        if (!inGameWorld(block.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().at(block.getLocation());
        if (game == null || !game.inBounds(block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            return;
        }
        game.track(block);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpread(BlockSpreadEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGrow(BlockGrowEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        if (inGameWorld(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFertilize(BlockFertilizeEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFade(BlockFadeEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBurn(BlockBurnEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onIgnite(BlockIgniteEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeaves(LeavesDecayEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (inGameWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallingBlock(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        if (event.getEntity() instanceof Player || !inGameWorld(block.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().at(block.getLocation());
        if (game == null || !game.inBounds(block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            return;
        }
        game.track(block);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent event) {
        if (inGameWorld(event.getLocation()) && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.DEFAULT) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ degats

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !inGameWorld(victim.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(victim);
        if (game == null || !game.takesDamage(victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Player attacker = null;
        if (damager instanceof Player player) {
            attacker = player;
        } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null || !inGameWorld(event.getEntity().getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(attacker);
        if (game == null) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player victim) {
            if (!game.canFight(attacker, victim)) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inGameWorld(player.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(player);
        if (game == null || !game.takesDamage(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inGameWorld(player.getLocation())) {
            return;
        }
        GameInstance game = plugin.instances().of(player);
        if (game == null || game.inventoryLocked(player)) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ deplacements / vehicules

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) {
            return;
        }
        Player player = event.getPlayer();
        GameInstance game = plugin.instances().of(player);
        if (game != null && game.frozen(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());
            event.setTo(to);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player player) {
            GameInstance game = plugin.instances().of(player);
            if (game != null && game.racing(player.getUniqueId())) { // 1.17.0 : crochet generique
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (inGameWorld(event.getVehicle().getLocation())) {
            event.setCancelled(true);
        }
    }
}
