package fr.kalium.games.listener;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.game.PvpInstance;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Arrivee sur Kal-Games (hub), depart, mort et reapparition. */
public final class ConnectionListener implements Listener {

    private final KalGames plugin;

    public ConnectionListener(KalGames plugin) {
        this.plugin = plugin;
    }

    /** Les joueurs arrivent directement dans le hub (evite de voir l'ancienne position). */
    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (!plugin.getConfig().getBoolean("hub.teleport-on-join", true)) {
            return;
        }
        try {
            event.setSpawnLocation(plugin.hub().hubLocation());
        } catch (Exception e) {
            plugin.getLogger().fine("Point d'arrivée non appliqué : " + e.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("hub.teleport-on-join", true)) {
            return;
        }
        plugin.later(1L, () -> {
            if (player.isOnline() && plugin.instances().of(player) == null) {
                plugin.hub().sendToHub(player);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.instances().leave(player, true);
        plugin.instances().leaveSpectator(player, true);
        plugin.menus().forget(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        GameInstance game = plugin.instances().of(victim);
        boolean inGameWorld = victim.getWorld() == plugin.worlds().world();
        if (game == null && !inGameWorld && !plugin.hub().isHubWorld(victim.getWorld())) {
            return;
        }
        event.deathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        if (game != null) {
            game.handleDeath(victim, victim.getKiller());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameInstance game = plugin.instances().of(player);
        if (game instanceof fr.kalium.games.game.RushInstance rush && game.ready()) {
            // Rush (1.11.0) : mort non interceptee (cas rare) -> retour a l'endroit de la mort, en spectateur.
            event.setRespawnLocation(rush.deathSpot(player));
            plugin.later(1L, () -> {
                if (player.isOnline() && plugin.instances().of(player) == rush) {
                    rush.onVanillaRespawn(player);
                }
            });
            return;
        }
        if (game != null && game.ready()) {
            event.setRespawnLocation(game.stands());
            if (game.racing(player.getUniqueId())) { // 1.17.0 : crochet generique (KG_BoatRace...)
                return;
            }
            plugin.later(1L, () -> {
                GameInstance current = plugin.instances().of(player);
                if (player.isOnline() && current == game) {
                    if (game instanceof PvpInstance pvp) {
                        pvp.onRespawned(player);
                    } else {
                        game.arriveInStands(player);
                    }
                }
            });
            return;
        }
        if (plugin.hub().isHubWorld(player.getWorld()) || player.getWorld() == plugin.worlds().world()) {
            event.setRespawnLocation(plugin.hub().hubLocation());
            plugin.later(1L, () -> {
                if (player.isOnline() && plugin.instances().of(player) == null) {
                    plugin.hub().sendToHub(player);
                }
            });
        }
    }
}
