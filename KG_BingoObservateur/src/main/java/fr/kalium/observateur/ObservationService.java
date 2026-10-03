package fr.kalium.observateur;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import fr.kalium.bingo.BingoPlugin;
import fr.kalium.bingo.game.GameManager;
import fr.kalium.bingo.game.GameState;
import fr.kalium.menu.api.Lang;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Observation en spectateur (voir KGBingoObservateur). Tout se passe sur le thread principal.
 */
final class ObservationService implements Listener {

    /** Etat d'un operateur avant sa premiere observation : on l'y ramene a la fin. */
    private record Saved(Location location, GameMode mode) {
    }

    private static final String LOBBY_WORLD = "bingo_lobby";

    private final JavaPlugin plugin;
    private final BingoPlugin bingo;
    private final Lang lang;
    private final Map<UUID, Saved> saved = new HashMap<>();
    /** Operateur -&gt; joueur dont il voit par les yeux (vue « dans ses yeux » encore active). */
    private final Map<UUID, UUID> eyes = new HashMap<>();
    /** Operateurs deplaces par ce plugin en ce moment : l'arret de la camera n'est alors pas un choix du joueur. */
    private final Set<UUID> moving = new HashSet<>();

    ObservationService(JavaPlugin plugin, BingoPlugin bingo, Lang lang) {
        this.plugin = plugin;
        this.bingo = bingo;
        this.lang = lang;
    }

    boolean isObserving(Player player) {
        return saved.containsKey(player.getUniqueId());
    }

    /** Observe {@code target} : vue libre a cote de lui, ou dans ses yeux. */
    void observe(Player op, Player target, boolean throughEyes) {
        if (op.equals(target)) {
            op.sendMessage(lang.c("msg.soi-meme", "<red>Vous ne pouvez pas vous observer vous-même."));
            return;
        }
        UUID opId = op.getUniqueId();
        saved.putIfAbsent(opId, new Saved(op.getLocation().clone(), op.getGameMode()));
        eyes.remove(opId);
        moving.add(opId);
        try {
            op.setSpectatorTarget(null);
            op.setGameMode(GameMode.SPECTATOR);
            op.teleport(target.getLocation());
        } finally {
            moving.remove(opId);
        }
        if (throughEyes) {
            attachLater(op, target);
            op.sendMessage(lang.c("msg.yeux", "<green>Vous voyez par les yeux de <white><name></white>. "
                    + "<gray>Maj pour vous détacher ; /observer quitter pour revenir.", "name", target.getName()));
        } else {
            op.sendMessage(lang.c("msg.libre", "<green>Vous observez <white><name></white> en spectateur. "
                    + "<gray>Clic gauche sur lui pour voir par ses yeux ; /observer quitter pour revenir.", "name", target.getName()));
        }
    }

    /** Attache la camera de l'operateur au joueur, quelques ticks plus tard (le temps que la teleportation aboutisse). */
    private void attachLater(Player op, Player target) {
        UUID opId = op.getUniqueId();
        UUID targetId = target.getUniqueId();
        eyes.put(opId, targetId);
        moving.add(opId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Player o = Bukkit.getPlayer(opId);
                Player t = Bukkit.getPlayer(targetId);
                if (o == null || t == null || !targetId.equals(eyes.get(opId)) || o.getGameMode() != GameMode.SPECTATOR) {
                    return;
                }
                if (!o.getWorld().equals(t.getWorld()) || o.getLocation().distanceSquared(t.getLocation()) > 64 * 64) {
                    o.teleport(t.getLocation());
                }
                o.setSpectatorTarget(t);
            } finally {
                moving.remove(opId);
            }
        }, 3L);
    }

    /** Fin de l'observation : position et mode de jeu d'avant (salle d'attente si son monde n'existe plus). */
    void stop(Player op, net.kyori.adventure.text.Component reason) {
        UUID opId = op.getUniqueId();
        Saved before = saved.remove(opId);
        eyes.remove(opId);
        if (before == null) {
            return;
        }
        moving.add(opId);
        try {
            op.setSpectatorTarget(null);
            Location back = before.location();
            if (back == null || !back.isWorldLoaded()) {
                World lobby = Bukkit.getWorld(LOBBY_WORLD);
                back = lobby != null ? lobby.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
            }
            op.teleport(back);
            op.setGameMode(before.mode());
        } finally {
            moving.remove(opId);
        }
        if (reason != null) {
            op.sendMessage(reason);
        }
    }

    void restoreAll() {
        for (UUID opId : List.copyOf(saved.keySet())) {
            Player op = Bukkit.getPlayer(opId);
            if (op != null) {
                stop(op, null);
            }
        }
        saved.clear();
        eyes.clear();
    }

    /**
     * Chaque seconde : un operateur dans un monde de partie qui n'est plus EN COURS est ramene (la partie vient de se
     * terminer : KG_BingoGame supprime ses mondes quelques secondes plus tard et ne le pourrait pas avec quelqu'un
     * dedans).
     */
    void tick() {
        GameManager games = bingo.getGameManager();
        for (UUID opId : List.copyOf(saved.keySet())) {
            Player op = Bukkit.getPlayer(opId);
            if (op == null) {
                continue;
            }
            World world = op.getWorld();
            String name = world.getName();
            if (!name.startsWith("bingo_") || name.equals(LOBBY_WORLD)) {
                continue;
            }
            GameManager.InstanceRef ref = games == null ? null : games.findInstanceByWorld(world);
            if (ref == null || ref.game().getState() != GameState.IN_PROGRESS) {
                stop(op, lang.c("msg.partie-finie", "<yellow>La partie observée est terminée : retour à votre position."));
            }
        }
    }

    // ------------------------------------------------------------------ evenements

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (saved.containsKey(id)) {
            stop(player, null); // reconnexion a sa position d'avant, dans son mode de jeu d'avant
        }
        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(eyes.entrySet())) {
            if (entry.getValue().equals(id)) {
                eyes.remove(entry.getKey());
                Player op = Bukkit.getPlayer(entry.getKey());
                if (op != null) {
                    op.sendMessage(lang.c("msg.cible-partie", "<yellow><name> s'est déconnecté. <gray>/observer pour choisir un autre joueur.",
                            "name", player.getName()));
                }
            }
        }
    }

    /** Joueur suivi « dans ses yeux » qui change de monde (portail, teleportation) : la camera le suit. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null || to.getWorld().equals(event.getFrom().getWorld())) {
            return;
        }
        follow(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetDeath(PlayerDeathEvent event) {
        for (UUID opId : followersOf(event.getPlayer().getUniqueId())) {
            moving.add(opId); // la camera se detache a la mort : ce n'est pas un choix de l'operateur
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetRespawn(PlayerRespawnEvent event) {
        follow(event.getPlayer());
    }

    private void follow(Player target) {
        for (UUID opId : followersOf(target.getUniqueId())) {
            Player op = Bukkit.getPlayer(opId);
            if (op != null) {
                attachLater(op, target);
            }
        }
    }

    private List<UUID> followersOf(UUID targetId) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : eyes.entrySet()) {
            if (entry.getValue().equals(targetId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /** L'operateur quitte lui-meme la vue « dans ses yeux » (Maj) : la camera ne le rattache plus. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
        UUID opId = event.getPlayer().getUniqueId();
        if (!moving.contains(opId)) {
            eyes.remove(opId);
        }
    }
}
