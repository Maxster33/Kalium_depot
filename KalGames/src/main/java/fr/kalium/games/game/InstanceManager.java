package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import fr.kalium.games.world.Template;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Cree, suit et ferme les parties. */
public final class InstanceManager {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** Resultat d'une creation : soit la partie, soit un motif de refus. */
    public record Result(GameInstance instance, Component error) {
        public boolean ok() {
            return instance != null;
        }
    }

    private final KalGames plugin;
    private final Map<String, GameInstance> instances = new LinkedHashMap<>();
    private final Map<UUID, GameInstance> byPlayer = new HashMap<>();
    /** Joueurs qui regardent une partie en mode spectateur, sans y participer. */
    private final Map<UUID, GameInstance> spectating = new HashMap<>();
    private final Map<String, GameInstance> byCode = new HashMap<>();
    private final Map<String, GameInstance> halls = new HashMap<>();
    /** Arene utilisee par chaque partie en cours (identifiant de la partie -> copie). */
    private final Map<String, ArenaPool.Cell> cells = new HashMap<>();
    private final ArenaPool pool;
    private final Random random = new Random();
    private int counter;
    private BukkitTask task;

    public InstanceManager(KalGames plugin) {
        this.plugin = plugin;
        this.pool = new ArenaPool(plugin);
    }

    /** Pre-genere les arenes des mini-jeux qui le demandent (prewarm-arenas), une fois le serveur demarre. */
    public void prewarm() {
        pool.ensurePrewarm();
    }

    /**
     * Restaure les copies d'arenes deja collees, sauvegardees au dernier arret propre du serveur : a appeler juste
     * apres {@code worlds.init()} si {@code worlds.cleanRestart()} est vrai, avant {@link #prewarm()} (qui complete
     * ensuite normalement si des copies manquent encore).
     */
    public void restoreCells() {
        pool.loadCells();
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (GameInstance instance : new ArrayList<>(instances.values())) {
                try {
                    instance.tick();
                } catch (Throwable t) {
                    plugin.getLogger().severe("Erreur dans la partie " + instance.id() + " : " + t);
                    t.printStackTrace();
                    try {
                        close(instance, plugin.t("game.error", "<red>Une erreur est survenue : la partie est fermée."));
                    } catch (Throwable inner) {
                        plugin.getLogger().severe("Fermeture impossible : " + inner);
                        instances.remove(instance.id());
                    }
                }
            }
        }, GameInstance.TICK_INTERVAL, GameInstance.TICK_INTERVAL);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        closeAll(null);
        // Sauvegarde les copies d'arenes encore pretes (arret propre) avant de tout oublier.
        pool.saveCells();
        pool.shutdown();
    }

    public void closeAll(Component message) {
        for (GameInstance instance : new ArrayList<>(instances.values())) {
            close(instance, message);
        }
    }

    // ------------------------------------------------------------------ acces

    public Collection<GameInstance> all() {
        return instances.values();
    }

    public GameInstance of(Player player) {
        return byPlayer.get(player.getUniqueId());
    }

    public GameInstance of(UUID uuid) {
        return byPlayer.get(uuid);
    }

    public GameInstance spectatorOf(Player player) {
        return spectating.get(player.getUniqueId());
    }

    public GameInstance spectatorOf(UUID uuid) {
        return spectating.get(uuid);
    }

    public GameInstance byCode(String code) {
        return code == null ? null : byCode.get(code.trim().toUpperCase(Locale.ROOT));
    }

    public GameInstance hall(String minigameId) {
        return halls.get(minigameId);
    }

    public GameInstance at(Location location) {
        if (location.getWorld() != plugin.worlds().world()) {
            return null;
        }
        int slot = plugin.worlds().slotAt(location.getBlockX(), location.getBlockZ());
        if (slot < 0) {
            return null;
        }
        for (GameInstance instance : instances.values()) {
            if (instance.slot() == slot) {
                return instance;
            }
        }
        return null;
    }

    /** Parties privees visibles pour un mini-jeu (ouvertes et non pleines). */
    public List<GameInstance> listedPrivate(String minigameId) {
        List<GameInstance> list = new ArrayList<>();
        for (GameInstance instance : instances.values()) {
            if (!instance.isPublic() && instance.listed() && instance.minigame().id().equals(minigameId)
                    && !instance.closing()) {
                list.add(instance);
            }
        }
        return list;
    }

    /** Parties de ce mini-jeu qu'on peut regarder (partie publique en cours + parties privees visibles), spectateur autorise. */
    public List<GameInstance> spectatable(String minigameId) {
        List<GameInstance> list = new ArrayList<>();
        // Parties publiques en cours (une seule par mini-jeu en general ; Rush : une par arene, voir joinPublicArena).
        for (GameInstance instance : instances.values()) {
            if (instance.isPublic() && !instance.closing() && instance.ready() && !instance.members().isEmpty()
                    && instance.minigame().id().equals(minigameId) && instance.minigame().getBool("allow-spectate", true)) {
                list.add(instance);
            }
        }
        for (GameInstance instance : instances.values()) {
            if (instance.isPublic() || !instance.listed() || instance.closing() || !instance.ready() || instance.members().isEmpty()
                    || !instance.minigame().id().equals(minigameId) || !instance.minigame().getBool("allow-spectate", true)) {
                continue;
            }
            list.add(instance);
        }
        return list;
    }

    public int playersIn(String minigameId) {
        int count = 0;
        for (GameInstance instance : instances.values()) {
            if (instance.minigame().id().equals(minigameId)) {
                count += instance.members().size();
            }
        }
        return count;
    }

    public int gamesOf(String minigameId) {
        int count = 0;
        for (GameInstance instance : instances.values()) {
            if (instance.minigame().id().equals(minigameId)) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ creation

    private Arena pickArena(Minigame minigame, String wanted) {
        List<Arena> ready = new ArrayList<>();
        for (Arena arena : plugin.repository().arenasOf(minigame.id())) {
            if (arena.ready(minigame.type()) && plugin.templates().exists(arena.id())) {
                if (wanted != null && arena.id().equals(wanted)) {
                    return arena;
                }
                ready.add(arena);
            }
        }
        if (ready.isEmpty()) {
            return null;
        }
        // Une arene dont une copie est deja prete est preferee (demarrage immediat, sans chargement).
        List<Arena> warm = new ArrayList<>();
        for (Arena arena : ready) {
            if (pool.hasReady(arena.id())) {
                warm.add(arena);
            }
        }
        List<Arena> choices = warm.isEmpty() ? ready : warm;
        return choices.get(random.nextInt(choices.size()));
    }

    /** Arenes utilisables pour ce mini-jeu. */
    public List<Arena> usableArenas(Minigame minigame) {
        List<Arena> ready = new ArrayList<>();
        for (Arena arena : plugin.repository().arenasOf(minigame.id())) {
            if (arena.ready(minigame.type()) && plugin.templates().exists(arena.id())) {
                ready.add(arena);
            }
        }
        return ready;
    }

    private Result create(Minigame minigame, Arena arena, boolean publicGame, Map<String, Object> options) {
        if (!plugin.worlds().ready()) {
            return new Result(null, plugin.t("game.no-world", "<red>Le monde des parties n'est pas disponible."));
        }
        if (arena == null) {
            return new Result(null, plugin.t("game.no-arena", "<red>Aucune arène complète n'est disponible pour ce mini-jeu."));
        }
        Template template = plugin.templates().get(arena.id());
        if (template == null) {
            return new Result(null, plugin.t("game.no-template", "<red>L'arène n'a pas de modèle enregistré."));
        }
        // Une arene plus grande que la limite ne garantirait plus la distance minimale entre deux parties.
        int room = plugin.worlds().spacing() - Math.max(0, plugin.getConfig().getInt("instances.min-distance", 1000));
        if (template.sizeX() > room || template.sizeZ() > room) {
            plugin.getLogger().warning("Arène " + arena.id() + " trop grande (" + template.sizeX() + " x " + template.sizeZ()
                    + ", maximum " + room + " avec instances.min-distance) : partie non créée.");
            return new Result(null, plugin.t("game.too-big", "<red>Cette arène est trop grande pour la distance minimale entre parties (voir arenas.max-size)."));
        }
        if (instances.size() >= plugin.worlds().maxSlots()) {
            return new Result(null, plugin.t("game.full", "<red>Toutes les parties sont occupées, réessayez dans un instant."));
        }
        // Une arene deja collee (gardee de cote) est reutilisee : aucun chargement.
        ArenaPool.Cell cell = pool.acquire(arena, template);
        boolean recycled = cell != null;
        if (cell == null) {
            cell = pool.create(arena, minigame, template);
        }
        if (cell == null) {
            return new Result(null, plugin.t("game.full", "<red>Toutes les parties sont occupées, réessayez dans un instant."));
        }
        String id = "g" + (++counter);
        GameInstance instance;
        MinigameType type = minigame.type();
        try {
            instance = switch (type) {
                case PVP_KIT -> new PvpInstance(plugin, id, minigame, arena, template, publicGame, options, cell.slot);
                case PARKOUR, BOAT_RACE -> new RaceInstance(plugin, id, minigame, arena, template, publicGame, options, cell.slot);
                case RUSH -> new RushInstance(plugin, id, minigame, arena, template, publicGame, options, cell.slot);
                default -> null;
            };
        } catch (RuntimeException e) {
            giveUp(cell, recycled);
            plugin.getLogger().severe("Création de la partie impossible : " + e);
            return new Result(null, plugin.t("game.error-create", "<red>Création impossible (voir la console)."));
        }
        if (instance == null) {
            giveUp(cell, recycled);
            return new Result(null, plugin.t("game.no-engine", "<red>Ce mini-jeu n'est pas encore jouable."));
        }
        instances.put(id, instance);
        cells.put(id, cell);
        cell.owner = instance;
        final GameInstance created = instance;
        if (cell.pasted) {
            // Arene deja en place : la partie est prete apres un tick (l'hote et le code sont alors renseignes).
            plugin.later(1L, () -> {
                if (!created.closing()) {
                    created.onPasted();
                }
            });
        }
        // Sinon le collage est en cours : la reserve previent la partie quand il est termine.
        return new Result(instance, null);
    }

    /** La partie n'a pas pu etre creee : la copie retourne dans la reserve (ou est supprimee si elle vient d'etre collee). */
    private void giveUp(ArenaPool.Cell cell, boolean recycled) {
        cell.owner = null;
        if (recycled) {
            return;
        }
        pool.discard(cell);
    }

    /** Cree une partie privee et y place l'hote. */
    public Result createPrivate(Player host, Minigame minigame, String arenaId, Map<String, Object> options) {
        Component blocked = blockedReason(host, minigame);
        if (blocked != null) {
            return new Result(null, blocked);
        }
        if (!minigame.privateEnabled()) {
            return new Result(null, plugin.t("game.private-disabled", "<red>Les parties privées sont désactivées pour ce mini-jeu."));
        }
        int privateLimit = minigame.getInt("max-private-games", 0);
        if (privateLimit > 0) {
            int running = 0;
            for (GameInstance other : instances.values()) {
                if (!other.isPublic() && !other.closing() && other.minigame().id().equals(minigame.id())) {
                    running++;
                }
            }
            if (running >= privateLimit) {
                return new Result(null, plugin.t("game.private-limit",
                        "<red>Trop de parties privées en cours pour ce mini-jeu (maximum <n>). Réessayez dans un instant ou rejoignez la partie publique.",
                        "n", privateLimit));
            }
        }
        Arena arena = pickArena(minigame, arenaId);
        Result result = create(minigame, arena, false, options);
        if (!result.ok()) {
            return result;
        }
        GameInstance instance = result.instance();
        instance.host(host.getUniqueId());
        instance.listed(options.get("listed") instanceof Boolean b && b);
        String code;
        do {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (byCode.containsKey(code));
        instance.code(code);
        byCode.put(code, instance);
        Component refusal = enter(host, instance);
        if (refusal != null) {
            close(instance, null);
            return new Result(null, refusal);
        }
        return result;
    }

    /** Entre dans la partie publique du mini-jeu (la cree au besoin). */
    public Component joinPublic(Player player, Minigame minigame) {
        Component blocked = blockedReason(player, minigame);
        if (blocked != null) {
            return blocked;
        }
        if (!minigame.publicEnabled()) {
            return plugin.t("game.public-disabled", "<red>Les parties publiques sont désactivées pour ce mini-jeu.");
        }
        if (minigame.type() == MinigameType.RUSH) {
            // Rush : une partie publique par arene (arene au hasard si le joueur ne l'a pas choisie).
            Arena arena = pickArena(minigame, null);
            if (arena == null) {
                return plugin.t("game.no-arena", "<red>Aucune arène complète n'est disponible pour ce mini-jeu.");
            }
            return joinPublicArena(player, minigame, arena);
        }
        GameInstance hall = halls.get(minigame.id());
        if (hall == null || hall.closing()) {
            Result result = create(minigame, pickArena(minigame, null), true, new HashMap<>());
            if (!result.ok()) {
                return result.error();
            }
            hall = result.instance();
            halls.put(minigame.id(), hall);
        }
        return enter(player, hall);
    }

    /**
     * Rush (1.11.0) : UNE partie publique par arene - choix de l'utilisateur. Le joueur rejoint la partie
     * publique encore ouverte sur cette arene ; si elle a demarre (ou est pleine), une nouvelle est ouverte.
     */
    public Component joinPublicArena(Player player, Minigame minigame, Arena arena) {
        Component blocked = blockedReason(player, minigame);
        if (blocked != null) {
            return blocked;
        }
        if (!minigame.publicEnabled()) {
            return plugin.t("game.public-disabled", "<red>Les parties publiques sont désactivées pour ce mini-jeu.");
        }
        String key = arenaHallKey(minigame, arena);
        GameInstance hall = halls.get(key);
        if (hall == null || hall.closing() || (hall instanceof RushInstance rush && !rush.openForNewPlayers())) {
            Result result = create(minigame, arena, true, new HashMap<>());
            if (!result.ok()) {
                return result.error();
            }
            hall = result.instance();
            halls.put(key, hall);
        }
        return enter(player, hall);
    }

    /** Partie publique encore ouverte sur cette arene (Rush), ou null. */
    public GameInstance arenaHall(Minigame minigame, Arena arena) {
        GameInstance hall = halls.get(arenaHallKey(minigame, arena));
        return hall == null || hall.closing() ? null : hall;
    }

    private static String arenaHallKey(Minigame minigame, Arena arena) {
        return minigame.id() + "@" + arena.id();
    }

    public Component joinPrivate(Player player, String code) {
        GameInstance instance = byCode(code);
        if (instance == null || instance.isPublic() || instance.closing()) {
            return plugin.t("join.unknown-code", "<red>Aucune partie ne correspond à ce code.");
        }
        Component blocked = blockedReason(player, instance.minigame());
        if (blocked != null) {
            return blocked;
        }
        return enter(player, instance);
    }

    public Component joinInstance(Player player, GameInstance instance) {
        if (instance.closing()) {
            return plugin.t("join.closed", "<red>Cette partie est terminée.");
        }
        return enter(player, instance);
    }

    /** Regarde une partie en cours (publique ou privee) sans y participer : mode spectateur, vol libre. */
    public Component joinSpectator(Player player, GameInstance instance) {
        if (byPlayer.containsKey(player.getUniqueId())) {
            return plugin.t("spectate.leave-first", "<red>Quittez votre partie en cours avant de regarder une autre partie.");
        }
        if (instance.closing()) {
            return plugin.t("join.closed", "<red>Cette partie est terminée.");
        }
        if (!instance.ready()) {
            return plugin.t("spectate.not-ready", "<red>L'arène n'est pas encore prête, réessayez dans un instant.");
        }
        if (!instance.minigame().getBool("allow-spectate", true)) {
            return plugin.t("spectate.disabled", "<red>Le mode spectateur est désactivé pour ce mini-jeu.");
        }
        GameInstance current = spectating.get(player.getUniqueId());
        if (current == instance) {
            return plugin.t("join.already", "<yellow>Vous êtes déjà dans cette partie.");
        }
        if (current != null) {
            detachSpectator(player.getUniqueId(), current);
        }
        spectating.put(player.getUniqueId(), instance);
        instance.addSpectator(player.getUniqueId());
        plugin.hub().applySpectatorState(player);
        // Point de spawn du spectateur = celui des gradins ; libre ensuite de voler ou il veut.
        player.teleport(instance.stands());
        return null;
    }

    /** Regarde une partie privee (non listee) avec son code. */
    public Component joinSpectatorByCode(Player player, String code) {
        GameInstance instance = byCode(code);
        if (instance == null || instance.isPublic() || instance.closing()) {
            return plugin.t("join.unknown-code", "<red>Aucune partie ne correspond à ce code.");
        }
        return joinSpectator(player, instance);
    }

    private Component blockedReason(Player player, Minigame minigame) {
        if (!minigame.playable()) {
            return plugin.t("game.unavailable", "<red>Ce mini-jeu n'est pas disponible.");
        }
        return null;
    }

    private Component enter(Player player, GameInstance instance) {
        GameInstance current = byPlayer.get(player.getUniqueId());
        if (current == instance) {
            return plugin.t("join.already", "<yellow>Vous êtes déjà dans cette partie.");
        }
        if (current instanceof RaceInstance race) {
            race.releaseHold(player.getUniqueId());
        }
        Component refusal = instance.admit(player);
        if (refusal != null) {
            return refusal;
        }
        if (current != null) {
            detach(player.getUniqueId(), current, false);
        }
        byPlayer.put(player.getUniqueId(), instance);
        return null;
    }

    // ------------------------------------------------------------------ depart / fermeture

    private void detach(UUID uuid, GameInstance instance, boolean disconnected) {
        byPlayer.remove(uuid, instance);
        instance.release(uuid, disconnected);
    }

    /** Retire le joueur de sa partie (sans le teleporter). */
    public void leave(Player player, boolean disconnected) {
        GameInstance instance = byPlayer.get(player.getUniqueId());
        if (instance != null) {
            detach(player.getUniqueId(), instance, disconnected);
        }
    }

    private void detachSpectator(UUID uuid, GameInstance instance) {
        spectating.remove(uuid, instance);
        instance.removeSpectator(uuid);
    }

    /** Arrete de regarder une partie et renvoie au hub (sauf a la deconnexion : le joueur part deja). */
    public void leaveSpectator(Player player, boolean disconnected) {
        GameInstance instance = spectating.get(player.getUniqueId());
        if (instance == null) {
            return;
        }
        detachSpectator(player.getUniqueId(), instance);
        if (!disconnected && player.isOnline()) {
            plugin.hub().sendSpectatorToHub(player);
        }
    }

    /** Ferme une partie : joueurs renvoyes au hub, arene liberee. */
    public void close(GameInstance instance, Component message) {
        if (instance.closing()) {
            return;
        }
        for (Player player : instance.onlineMembers()) {
            byPlayer.remove(player.getUniqueId(), instance);
            if (message != null) {
                player.sendMessage(plugin.prefix().append(message));
            }
            plugin.hub().sendToHub(player);
        }
        for (UUID uuid : new ArrayList<>(instance.members())) {
            byPlayer.remove(uuid, instance);
        }
        for (Player player : instance.onlineSpectators()) {
            detachSpectator(player.getUniqueId(), instance);
            if (message != null) {
                player.sendMessage(plugin.prefix().append(message));
            }
            plugin.hub().sendSpectatorToHub(player);
        }
        for (UUID uuid : new ArrayList<>(instance.spectators())) {
            spectating.remove(uuid, instance);
        }
        instance.shutdown();
        instances.remove(instance.id());
        byCode.remove(instance.code());
        halls.values().removeIf(hall -> hall == instance);
        ArenaPool.Cell cell = cells.remove(instance.id());
        if (cell != null) {
            // L'arene est remise a l'etat d'origine et gardee de cote pour la partie suivante.
            pool.release(cell, instance);
        }
    }

    /** Ferme toutes les parties d'un mini-jeu (apres une modification importante). */
    public void closeAllOf(String minigameId, Component message) {
        for (GameInstance instance : new ArrayList<>(instances.values())) {
            if (instance.minigame().id().equals(minigameId)) {
                close(instance, message);
            }
        }
        pool.purgeMinigame(minigameId);
    }

    /**
     * Modele d'arene recapture (1.12.2) : les copies LIBRES de l'ancien modele sont supprimees tout de suite (blocs
     * effaces) ; celles des parties en cours restent jusqu'a la fin de la partie, puis sont supprimees a leur tour
     * (ArenaPool.release : le modele a change). De nouvelles copies sont pre-generees si le mini-jeu en demande.
     */
    public void retireArenaCopies(String arenaId) {
        pool.purgeArena(arenaId);
        if (plugin.isEnabled()) {
            plugin.later(20L, pool::ensurePrewarm);
        }
    }

    public void closeAllOfArena(String arenaId, Component message) {
        for (GameInstance instance : new ArrayList<>(instances.values())) {
            if (instance.arena().id().equals(arenaId)) {
                close(instance, message);
            }
        }
        // Arene modifiee ou supprimee : les copies gardees de cote sont perimees, elles sont regenerees.
        pool.purgeArena(arenaId);
        if (plugin.isEnabled()) {
            plugin.later(20L, pool::ensurePrewarm);
        }
    }
}
