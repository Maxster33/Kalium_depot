package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.24.0 - bouton « Rejouer » (demande de LeKiwi06, 09/10/2026 : « pour tous les mini-jeux : faire un bouton à la fin
 * de la partie pour relancer une partie avec les mêmes paramètres », « proposer ça aux joueurs pendant 30 secondes »,
 * « aussi aux joueurs qui étaient en train de regarder la partie en spectateur »).
 *
 * Un objet « Rejouer » est donne dans la barre du hub pendant replay.seconds (30 s) :
 * - partie publique : clic = retour dans la file du meme jeu ;
 * - partie privee : le premier qui clique recree la partie (meme jeu, meme arene, memes reglages) et en devient
 *   l'hote ; l'objet des autres devient « Rejoindre la partie de X » ;
 * - spectateur de la partie precedente : petit menu « Jouer » / « Regarder ».
 *
 * Ce que fait l'objet est decrit par un {@link Replay} : celui des jeux du moteur est ici ({@link EngineReplay}) ; un
 * plugin dont le jeu se termine ailleurs (KG_BuildBattle) fournit le sien et appelle {@link #give}.
 */
public final class ReplayService {

    /** Ce que propose l'objet « Rejouer » d'une partie terminee (partage par tous les joueurs de cette partie). */
    public interface Replay {

        /** Pseudo de celui qui a deja recree la partie privee (l'objet devient « Rejoindre la partie de X »), ou null. */
        String hostName();

        /** Le joueur veut rejouer : null si c'est fait, sinon le motif du refus. */
        Component play(Player player);

        /** Y a-t-il une partie a regarder maintenant (sinon le bouton « Regarder » est grise) ? */
        boolean canWatch();

        /** Le joueur veut regarder la nouvelle partie : null si c'est fait, sinon le motif du refus. */
        Component watch(Player player);
    }

    private static final class Holder {
        final Replay replay;
        final boolean spectator;
        BukkitTask expiry;

        Holder(Replay replay, boolean spectator) {
            this.replay = replay;
            this.spectator = spectator;
        }
    }

    private final KalGames plugin;
    private final Map<UUID, Holder> holders = new HashMap<>();

    public ReplayService(KalGames plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("replay.enabled", true);
    }

    private int seconds() {
        return Math.max(5, plugin.getConfig().getInt("replay.seconds", 30));
    }

    // ------------------------------------------------------------------ don de l'objet

    /** Fin d'un match du moteur : les joueurs et les spectateurs, deja renvoyes au hub, recoivent l'objet. */
    public void offerAfterMatch(GameInstance ended, Collection<Player> players, Collection<Player> watchers) {
        if (!enabled() || (players.isEmpty() && watchers.isEmpty())) {
            return;
        }
        Replay replay = new EngineReplay(ended);
        for (Player player : players) {
            give(player, replay, false);
        }
        for (Player player : watchers) {
            give(player, replay, true);
        }
    }

    /** Donne l'objet « Rejouer » a un joueur du hub, pour replay.seconds. Sans effet s'il est deja en partie. */
    public void give(Player player, Replay replay, boolean spectator) {
        if (!enabled() || !player.isOnline()
                || plugin.instances().of(player) != null || plugin.instances().spectatorOf(player) != null) {
            return;
        }
        forget(player.getUniqueId());
        removeItems(player);
        int slot = freeSlot(player);
        if (slot < 0) {
            return;
        }
        Holder holder = new Holder(replay, spectator);
        holders.put(player.getUniqueId(), holder);
        player.getInventory().setItem(slot, plugin.items().replayItem(replay.hostName()));
        int seconds = seconds();
        plugin.tell(player, "replay.offered",
                "<green>Envie de rejouer ? <gray>Utilisez l'objet <white>Rejouer</white> de votre barre (<s> s).", "s", seconds);
        holder.expiry = plugin.later(seconds * 20L, () -> {
            if (holders.remove(player.getUniqueId(), holder) && player.isOnline()) {
                removeItems(player);
            }
        });
    }

    /** Case de l'objet : replay.slot (6) si elle est libre, sinon la premiere case libre de la barre, sinon -1. */
    private int freeSlot(Player player) {
        int wanted = plugin.items().slot("replay.slot", 6);
        if (empty(player.getInventory().getItem(wanted))) {
            return wanted;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (empty(player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private boolean isReplayItem(ItemStack item) {
        return ItemService.REPLAY.equals(plugin.items().kind(item));
    }

    private void removeItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isReplayItem(contents[slot])) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    /** Le joueur n'a plus de proposition (deconnexion, objet utilise). */
    public void forget(UUID uuid) {
        Holder holder = holders.remove(uuid);
        if (holder != null && holder.expiry != null) {
            holder.expiry.cancel();
        }
    }

    /** L'objet des autres joueurs de la meme partie change de nom (« Rejoindre la partie de X »). */
    private void refresh(Replay replay, UUID except, Component notice) {
        String hostName = replay.hostName();
        for (Map.Entry<UUID, Holder> entry : holders.entrySet()) {
            if (entry.getValue().replay != replay || entry.getKey().equals(except)) {
                continue;
            }
            Player other = Bukkit.getPlayer(entry.getKey());
            if (other == null) {
                continue;
            }
            // Seulement si l'objet est encore dans sa barre (sinon il est entre dans une autre partie).
            boolean replaced = false;
            for (int slot = 0; slot < 9; slot++) {
                if (isReplayItem(other.getInventory().getItem(slot))) {
                    other.getInventory().setItem(slot, plugin.items().replayItem(hostName));
                    replaced = true;
                }
            }
            if (replaced && notice != null) {
                other.sendMessage(plugin.prefix().append(notice));
            }
        }
    }

    // ------------------------------------------------------------------ clic sur l'objet

    /** Clic droit avec l'objet « Rejouer ». */
    public void use(Player player) {
        Holder holder = holders.get(player.getUniqueId());
        if (holder == null) {
            // Objet perime (delai passe, redemarrage...) : il est retire.
            removeItems(player);
            plugin.tell(player, "replay.expired", "<red>Il est trop tard pour rejouer cette partie.");
            return;
        }
        if (!holder.spectator) {
            play(player, holder);
            return;
        }
        Replay replay = holder.replay;
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(plugin.gui().button(plugin.t("replay.play", "<green>Jouer"),
                plugin.t("replay.play-tip", "<gray>Entrer dans la nouvelle partie comme joueur."), p -> {
                    Holder current = holders.get(p.getUniqueId());
                    if (current == holder) {
                        play(p, holder);
                    }
                }));
        boolean watchable = replay.canWatch();
        buttons.add(plugin.gui().button(
                watchable ? plugin.t("replay.watch", "<aqua>Regarder") : plugin.t("replay.watch-off", "<dark_gray>Regarder"),
                watchable ? plugin.t("replay.watch-tip", "<gray>Regarder la nouvelle partie en spectateur.")
                        : plugin.t("replay.watch-off-tip", "<gray>Personne n'a encore relancé la partie."), p -> {
                    Holder current = holders.get(p.getUniqueId());
                    if (current != holder) {
                        return;
                    }
                    Component error = replay.watch(p);
                    if (error != null) {
                        p.sendMessage(plugin.prefix().append(error));
                        return;
                    }
                    forget(p.getUniqueId());
                    removeItems(p);
                }));
        plugin.gui().open(player, plugin.t("replay.title", "<green><bold>Rejouer"),
                List.of(plugin.t("replay.body", "<gray>Vous regardiez la partie précédente. Jouer ou regarder la suivante ?")),
                buttons);
    }

    private void play(Player player, Holder holder) {
        Replay replay = holder.replay;
        String before = replay.hostName();
        Component error = replay.play(player);
        if (error != null) {
            player.sendMessage(plugin.prefix().append(error));
            return;
        }
        forget(player.getUniqueId());
        removeItems(player);
        String after = replay.hostName();
        if (after != null && !after.equals(before)) {
            refresh(replay, player.getUniqueId(), plugin.t("replay.relaunched",
                    "<yellow><white><player></white> a relancé la partie : utilisez l'objet <white>Rejoindre</white> pour le suivre.",
                    "player", after));
        }
    }

    // ------------------------------------------------------------------ jeux du moteur

    /** « Rejouer » d'une partie du moteur de KalGames : meme mini-jeu, meme arene, memes reglages. */
    private final class EngineReplay implements Replay {

        private final String minigameId;
        private final String arenaId;
        private final boolean publicGame;
        private final Map<String, Object> options;
        /** Partie privee recreee par le premier qui a clique. */
        private GameInstance created;
        private String creator;

        EngineReplay(GameInstance ended) {
            this.minigameId = ended.minigame().id();
            this.arenaId = ended.arena().id();
            this.publicGame = ended.isPublic();
            this.options = new HashMap<>(ended.options);
        }

        private GameInstance current() {
            return created != null && !created.closing() ? created : null;
        }

        @Override
        public String hostName() {
            GameInstance game = current();
            if (game == null) {
                return null;
            }
            Player host = game.host() == null ? null : Bukkit.getPlayer(game.host());
            return host != null ? host.getName() : creator;
        }

        private Arena sameArena(Minigame minigame) {
            for (Arena arena : plugin.instances().usableArenas(minigame)) {
                if (arena.id().equals(arenaId)) {
                    return arena;
                }
            }
            return null;
        }

        @Override
        public Component play(Player player) {
            InstanceManager manager = plugin.instances();
            Minigame minigame = plugin.repository().minigame(minigameId);
            if (minigame == null) {
                return plugin.t("game.unavailable", "<red>Ce mini-jeu n'est pas disponible.");
            }
            if (publicGame) {
                // Rush : une partie publique par arene (la meme arene si elle existe encore).
                Arena arena = minigame.type() == MinigameType.RUSH ? sameArena(minigame) : null;
                return arena != null ? manager.joinPublicArena(player, minigame, arena) : manager.joinPublic(player, minigame);
            }
            GameInstance game = current();
            if (game != null) {
                return manager.joinPrivate(player, game.code());
            }
            // Arene retiree entre-temps : createPrivate en prend une autre au hasard.
            InstanceManager.Result result = manager.createPrivate(player, minigame, arenaId, new HashMap<>(options));
            if (!result.ok()) {
                return result.error();
            }
            created = result.instance();
            creator = player.getName();
            player.sendMessage(plugin.prefix().append(plugin.t("menu.create-done",
                    "<green>Partie créée. Code : <white><bold><code></bold></white> <gray>(à partager avec vos amis).",
                    "code", created.code())));
            return null;
        }

        private GameInstance watchable() {
            if (!publicGame) {
                return current();
            }
            Minigame minigame = plugin.repository().minigame(minigameId);
            if (minigame == null) {
                return null;
            }
            Arena arena = minigame.type() == MinigameType.RUSH ? sameArena(minigame) : null;
            GameInstance hall = arena != null ? plugin.instances().arenaHall(minigame, arena) : plugin.instances().hall(minigameId);
            return hall == null || hall.closing() ? null : hall;
        }

        @Override
        public boolean canWatch() {
            return watchable() != null;
        }

        @Override
        public Component watch(Player player) {
            GameInstance game = watchable();
            if (game == null) {
                return publicGame
                        ? plugin.t("replay.no-public", "<red>Aucune partie publique n'est en cours pour l'instant.")
                        : plugin.t("replay.not-created", "<red>Personne n'a encore relancé la partie.");
            }
            return plugin.instances().joinSpectator(player, game);
        }
    }
}
