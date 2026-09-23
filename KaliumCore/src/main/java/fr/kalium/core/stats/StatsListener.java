package fr.kalium.core.stats;

import fr.kalium.core.KaliumCore;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** Alimente StatsService a partir des evenements Bukkit correspondant aux 5 statistiques de base. */
public final class StatsListener implements Listener {

    private final KaliumCore plugin;

    public StatsListener(KaliumCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.stats().onJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.stats().onQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.stats().get(id).incrementBlocksBroken();
        plugin.stats().markDirty(id);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.stats().get(id).incrementBlocksPlaced();
        plugin.stats().markDirty(id);
    }

    /** "Niveaux depenses" : toute diminution du niveau d'XP (enchantements, enclume, reparation...). */
    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent event) {
        int diff = event.getOldLevel() - event.getNewLevel();
        if (diff > 0) {
            UUID id = event.getPlayer().getUniqueId();
            plugin.stats().get(id).addLevelsSpent(diff);
            plugin.stats().markDirty(id);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }
        UUID id = killer.getUniqueId();
        plugin.stats().get(id).incrementMonstersKilled();
        plugin.stats().markDirty(id);
    }
}
