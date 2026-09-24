package fr.kalium.boatrace;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.world.Template;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Course de bateau (points de controle, tours). 1.0.0 : reprise telle quelle de RaceInstance de KalGames 1.16.0
 * (partie « bateau » seulement : le Parcours reste dans KalGames pour l'instant), sans changement de comportement.
 * Les textes restent ceux du lang.yml de KalGames (cles race.*).
 */
public final class BoatRaceInstance extends GameInstance {

    private static final int GRACE_SECONDS = 30;

    private static final class Racer {
        int lap;
        int next;
        Location lastCheckpoint;
        Location previous;
        boolean finished;
        long finishMillis;
        int rank;
        int remountCooldown;
        /** Debut de la course de ce joueur. */
        long runStart;
        /** Debut du tour en cours (depart de la course, puis chaque passage de la ligne d'arrivee). */
        long lapStart;
        /** Jusqu'a cet instant, un message d'action bar (point de controle, retour...) reste affiche. */
        long noticeUntil;
    }

    /** Nombre de tours maximal d'une course. */
    static final int MAX_LAPS = 40;
    static final int DEFAULT_PUBLIC_LAPS = 3;

    private final int laps;
    private final int privateCap;
    private final Map<UUID, Racer> racers = new LinkedHashMap<>();
    /** Joueurs dont le bateau ne doit plus etre verrouille (retour checkpoint, changement de partie). */
    private final Set<UUID> released = new HashSet<>();
    private final List<UUID> finishOrder = new ArrayList<>();
    /** Nombre de coureurs arrives (ne diminue pas si un arrive quitte la partie). */
    private int finishedCount;
    private int graceLeft = -1;
    private int elapsed;

    public BoatRaceInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                            boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        // Les parties publiques ont toujours le meme nombre de tours (public-laps, 3 par defaut) ; l'hote d'une
        // partie privee choisit librement (jusqu'a 40).
        this.laps = Math.max(1, Math.min(MAX_LAPS, publicGame
                ? minigame.getInt("public-laps", DEFAULT_PUBLIC_LAPS)
                : optionInt("laps", minigame.getInt("laps", DEFAULT_PUBLIC_LAPS))));
        int max = Math.min(minigame.getInt("max-players", 12), Math.max(1, arena.list("start-grid").size()));
        this.privateCap = Math.max(1, Math.min(max, optionInt("maxPlayers", max)));
    }

    public int laps() {
        return laps;
    }

    // ------------------------------------------------------------------ admission / demarrage

    @Override
    protected Component canAdmit(Player player) {
        if (!isPublic() && members.size() >= privateCap) {
            return t("join.full", "<red>La partie est complète.");
        }
        return null;
    }

    @Override
    protected void onArrived(Player player) {
        if (isPublic() && phase != Phase.RUNNING) {
            player.sendMessage(plugin.prefix().append(t("race.public-welcome",
                    "<gray>Vous êtes dans la file d'attente. La course démarre dès qu'il y a assez de joueurs.")));
        } else if (!isPublic() && phase == Phase.WAITING) {
            player.sendMessage(plugin.prefix().append(t("race.private-welcome",
                    "<gray>Partie privée <white><code></white>. L'hôte lance la course depuis le menu.", "code", code)));
        }
    }

    @Override
    protected int minParticipants() {
        return Math.max(1, minigame().getInt("min-players", 1));
    }

    @Override
    protected int maxParticipants() {
        return Math.max(1, Math.min(minigame().getInt("max-players", 12), Math.max(1, arena().list("start-grid").size())));
    }

    @Override
    protected int gatherSeconds() {
        return minigame().getInt("gather-seconds", 20);
    }

    @Override
    protected List<UUID> pickParticipants(List<UUID> queue) {
        return new ArrayList<>(queue.subList(0, Math.min(queue.size(), maxParticipants())));
    }

    @Override
    protected Component validatePrivateStart() {
        if (members.isEmpty()) {
            return t("race.no-players", "<red>Aucun joueur.");
        }
        return null;
    }

    @Override
    protected void beginMatch() {
        racers.clear();
        finishOrder.clear();
        finishedCount = 0;
        graceLeft = -1;
        elapsed = 0;
        phase = Phase.COUNTDOWN;
        secondsLeft = Math.max(0, minigame().getInt("countdown-seconds", 5));

        List<Pos> grid = arena().list("start-grid");
        Pos start = arena().point("start");
        int index = 0;
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Location spot;
            if (!grid.isEmpty()) {
                spot = loc(grid.get(index % grid.size()));
            } else if (start != null) {
                spot = loc(start);
            } else {
                spot = stands();
            }
            index++;
            Racer racer = new Racer();
            racer.lastCheckpoint = spot.clone();
            racer.previous = spot.clone();
            racers.put(uuid, racer);

            plugin.hub().resetPlayer(player, GameMode.ADVENTURE);
            player.teleport(spot);
            plugin.hub().giveInGameItems(player, false);
        }
        broadcastTo(participants, t("race.start", "<gold><mode> : <white><n></white> participant(s), <white><laps></white> tour(s).",
                "mode", "Course de bateau", "n", racers.size(), "laps", laps));
        if (secondsLeft == 0) {
            go();
        } else {
            showCountdown();
        }
    }

    private void showCountdown() {
        title(participants, Component.text(secondsLeft, NamedTextColor.GOLD),
                t("race.countdown-sub", "<gray>Préparez-vous"), 0, 22, 0);
    }

    private void go() {
        phase = Phase.RUNNING;
        long startMillis = System.currentTimeMillis();
        elapsed = 0;
        for (Racer racer : racers.values()) {
            racer.runStart = startMillis;
            racer.lapStart = startMillis;
        }
        title(participants, t("race.go-title", "<green><bold>Partez !"), Component.empty(), 0, 20, 10);
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1f);
            mountBoat(player, player.getLocation());
        }
    }

    private void mountBoat(Player player, Location where) {
        EntityType type;
        try {
            type = EntityType.valueOf(minigame().getText("boat-type", "OAK_BOAT").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            type = EntityType.OAK_BOAT;
        }
        Entity boat;
        try {
            boat = world.spawnEntity(where, type);
        } catch (IllegalArgumentException e) {
            boat = world.spawnEntity(where, EntityType.OAK_BOAT);
        }
        boat.setPersistent(false);
        boat.addPassenger(player);
    }

    // ------------------------------------------------------------------ boucle

    private Location target(Racer racer) {
        List<Pos> checkpoints = arena().list("checkpoints");
        if (racer.next < checkpoints.size()) {
            return loc(checkpoints.get(racer.next));
        }
        Pos finish = arena().point("finish");
        return finish == null ? null : loc(finish);
    }

    /** Deuxieme point de la ligne d'arrivee (pitstop), si configure et si le coureur vise la ligne d'arrivee. */
    private Location pitstop(Racer racer) {
        if (racer.next < arena().list("checkpoints").size()) {
            return null;
        }
        Pos pit = arena().point("finish-pit");
        return pit == null ? null : loc(pit);
    }

    @Override
    protected void matchTick() {
        if (phase != Phase.RUNNING) {
            return;
        }
        int radius = minigame().getInt("checkpoint-radius", 8);
        int configuredVoid = minigame().getInt("void-y", -64);
        int voidY = configuredVoid <= -64 ? minY - 5 : configuredVoid;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Racer> entry : new ArrayList<>(racers.entrySet())) {
            Racer racer = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || racer.finished || player.getWorld() != world) {
                continue;
            }
            Location current = player.getLocation();
            // Si le dernier point de controle est lui-meme sous la limite, on retombe sur la limite automatique (pas de boucle).
            double limit = racer.lastCheckpoint != null && racer.lastCheckpoint.getY() < voidY ? minY - 5 : voidY;
            if (current.getY() < limit) {
                respawn(player, racer);
                continue;
            }
            if (racer.remountCooldown > 0) {
                racer.remountCooldown--;
            } else if (!player.isInsideVehicle()) {
                mountBoat(player, current);
                racer.remountCooldown = 15;
            }
            Location target = target(racer);
            boolean atTarget = target != null && distanceToSegment(racer.previous, current, target) <= radius;
            if (!atTarget) {
                // Ligne d'arrivee a deux points (pitstop) : franchir l'un ou l'autre suffit.
                Location pit = pitstop(racer);
                atTarget = pit != null && distanceToSegment(racer.previous, current, pit) <= radius;
            }
            if (atTarget) {
                reached(player, racer);
            }
            racer.previous = current;
            if (!racer.finished && now >= racer.noticeUntil) {
                player.sendActionBar(racerStatus(racer, now));
            }
        }
    }

    /** Ligne d'action bar d'un coureur : chrono, tour, points de controle atteints. */
    private Component racerStatus(Racer racer, long now) {
        List<Pos> checkpoints = arena().list("checkpoints");
        long run = Math.max(0, now - racer.runStart);
        return t("race.status", "<gold><time> <dark_gray>| <gray>Tour <white><lap>/<laps></white> <dark_gray>| <gray>Contrôle <white><cp>/<total></white>",
                "time", formatTenths(run), "lap", racer.lap + 1, "laps", laps,
                "cp", racer.next, "total", checkpoints.size());
    }

    private void reached(Player player, Racer racer) {
        List<Pos> checkpoints = arena().list("checkpoints");
        if (racer.next < checkpoints.size()) {
            racer.lastCheckpoint = loc(checkpoints.get(racer.next));
            racer.next++;
            racer.noticeUntil = System.currentTimeMillis() + 1500;
            int points = ranked(player) ? Math.max(0, minigame().getInt("points-checkpoint", 0)) : 0;
            if (points > 0) {
                plugin.scores().award(player, minigame(), points, true);
            }
            player.sendActionBar(points > 0
                    ? t("race.checkpoint-points", "<green>Point de contrôle <white><n>/<total></white> <gold>+<points> pt(s)",
                            "n", racer.next, "total", checkpoints.size(), "points", points)
                    : t("race.checkpoint", "<green>Point de contrôle <white><n>/<total>",
                            "n", racer.next, "total", checkpoints.size()));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
            return;
        }
        racer.lap++;
        recordLap(player, racer);
        if (racer.lap >= laps) {
            racerFinished(player, racer);
        } else {
            racer.next = 0;
            racer.noticeUntil = System.currentTimeMillis() + 1500;
            racer.lastCheckpoint = loc(arena().point("finish"));
            player.sendActionBar(t("race.lap", "<green>Tour <white><n>/<total>", "n", racer.lap + 1, "total", laps));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        }
    }

    /** Temps du tour qui vient d'etre termine : classement des meilleurs temps sur 1 tour. */
    private void recordLap(Player player, Racer racer) {
        long now = System.currentTimeMillis();
        long lapTime = now - racer.lapStart;
        racer.lapStart = now;
        if (plugin.scores().recordLap(player, minigame(), lapTime, ranked(player))) {
            player.sendMessage(plugin.prefix().append(t("race.lap-record",
                    "<light_purple>Nouveau record personnel sur 1 tour : <white><time></white> !", "time", formatTime(lapTime))));
        } else {
            player.sendMessage(plugin.prefix().append(t("race.lap-time", "<gray>Tour <white><n></white> : <white><time></white>",
                    "n", racer.lap, "time", formatTime(lapTime))));
        }
    }

    private void racerFinished(Player player, Racer racer) {
        racer.finished = true;
        racer.finishMillis = System.currentTimeMillis() - racer.runStart;
        finishOrder.add(player.getUniqueId());
        racer.rank = ++finishedCount;
        int points = Math.max(0, minigame().getInt("points-win", 3) - (racer.rank - 1));
        broadcast(t("race.finished", "<aqua><name></aqua> <green>termine <white>n°<rank></white> en <white><time></white>.",
                "name", player.getName(), "rank", racer.rank, "time", formatTime(racer.finishMillis)));
        // Ce sont les temps sur 1 tour (enregistres a chaque tour) qui sont classes, pas le temps total.
        if (points > 0 && ranked(player)) {
            plugin.scores().award(player, minigame(), points, true);
            player.sendMessage(plugin.prefix().append(t("race.points", "<gold>+<points> point(s)", "points", points)));
        }
        player.showTitle(net.kyori.adventure.title.Title.title(
                t("race.finish-title", "<gold><bold>Arrivée !"),
                t("race.finish-sub", "<white>n°<rank> - <time>", "rank", racer.rank, "time", formatTime(racer.finishMillis))));
        becomeMatchSpectator(player);
        if (racer.rank == 1 && racers.size() > 1 && graceLeft < 0) {
            graceLeft = GRACE_SECONDS;
            broadcast(t("race.grace", "<yellow>Fin de la course dans <white><s></white> s.", "s", GRACE_SECONDS));
        }
        if (allDone()) {
            finishRace();
        }
    }

    @Override
    public void releaseHold(UUID uuid) {
        released.add(uuid);
    }

    private void respawn(Player player, Racer racer) {
        Location spot = racer.lastCheckpoint.clone();
        released.add(player.getUniqueId());
        player.leaveVehicle();
        player.teleport(spot);
        released.remove(player.getUniqueId());
        player.setFallDistance(0);
        player.setVelocity(new Vector());
        racer.previous = spot.clone();
        racer.noticeUntil = System.currentTimeMillis() + 1500;
        racer.remountCooldown = 0;
        player.sendActionBar(t("race.respawn", "<yellow>Retour au dernier point de contrôle."));
    }

    @Override
    public void useCheckpointItem(Player player) {
        Racer racer = racers.get(player.getUniqueId());
        if (racer != null && phase == Phase.RUNNING && !racer.finished) {
            respawn(player, racer);
        }
    }

    private boolean allDone() {
        for (Racer racer : racers.values()) {
            if (!racer.finished) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void matchSecond() {
        switch (phase) {
            case COUNTDOWN -> {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    go();
                } else {
                    showCountdown();
                }
            }
            case RUNNING -> {
                elapsed++;
                int limit = timeLimit();
                if (graceLeft > 0) {
                    graceLeft--;
                    if (graceLeft == 0) {
                        finishRace();
                        return;
                    }
                }
                if (limit > 0 && elapsed >= limit) {
                    broadcast(t("race.time-up", "<yellow>Temps écoulé !"));
                    finishRace();
                }
            }
            case ENDING -> {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    endMatch();
                }
            }
            default -> {
            }
        }
    }

    /**
     * Temps limite (secondes) de la course. Une partie privee de plus de tours que la reference (public-laps)
     * a un temps limite proportionnel : 40 tours ne tiennent pas dans le temps prevu pour 3.
     */
    private int timeLimit() {
        int limit = minigame().getInt("time-limit-seconds", 600);
        if (limit > 0) {
            int reference = Math.max(1, Math.min(MAX_LAPS, minigame().getInt("public-laps", DEFAULT_PUBLIC_LAPS)));
            if (laps > reference) {
                limit = (int) Math.min(Integer.MAX_VALUE, (long) limit * laps / reference);
            }
        }
        return limit;
    }

    private void finishRace() {
        if (phase != Phase.RUNNING) {
            return;
        }
        phase = Phase.ENDING;
        secondsLeft = 6;
        List<Map.Entry<UUID, Racer>> unfinished = new ArrayList<>();
        for (Map.Entry<UUID, Racer> entry : racers.entrySet()) {
            if (!entry.getValue().finished) {
                unfinished.add(entry);
            }
        }
        unfinished.sort(Comparator
                .comparingInt((Map.Entry<UUID, Racer> e) -> -e.getValue().lap)
                .thenComparingInt(e -> -e.getValue().next)
                .thenComparingDouble(e -> {
                    Player player = Bukkit.getPlayer(e.getKey());
                    Location target = target(e.getValue());
                    if (player == null || target == null || player.getWorld() != world) {
                        return Double.MAX_VALUE;
                    }
                    double distance = player.getLocation().distance(target);
                    Location pit = pitstop(e.getValue());
                    return pit == null || pit.getWorld() != player.getWorld() ? distance : Math.min(distance, player.getLocation().distance(pit));
                }));

        broadcast(t("race.results", "<gold><bold>Résultats"));
        int position = 1;
        for (UUID uuid : finishOrder) {
            Racer racer = racers.get(uuid);
            broadcast(t("race.result-line", "<gray><rank>. <white><name></white> <dark_gray>- <aqua><time>",
                    "rank", position, "name", nameOf(uuid), "time", formatTime(racer.finishMillis)));
            position++;
        }
        // Les coureurs qui n'ont pas fini a temps comptent quand meme dans le classement : ils recoivent des points
        // de classement (comme un coureur arrive, en continuant la numerotation des rangs).
        for (Map.Entry<UUID, Racer> entry : unfinished) {
            Racer racer = entry.getValue();
            racer.rank = position;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                int points = ranked(player) ? Math.max(0, minigame().getInt("points-win", 3) - (racer.rank - 1)) : 0;
                if (points > 0) {
                    plugin.scores().award(player, minigame(), points, true);
                    player.sendMessage(plugin.prefix().append(t("race.points", "<gold>+<points> point(s)", "points", points)));
                }
            }
            broadcast(t("race.result-dnf", "<gray><rank>. <white><name></white> <dark_gray>- <red>non arrivé",
                    "rank", position, "name", nameOf(entry.getKey())));
            position++;
        }
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            Racer racer = racers.get(uuid);
            if (player != null && racer != null && !racer.finished) {
                becomeMatchSpectator(player);
            }
        }
    }

    private String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "?" : name;
    }

    private static String formatTime(long millis) {
        return fr.kalium.scoreboards.data.StatsService.formatTime(millis);
    }

    private static String formatTenths(long millis) {
        long minutes = millis / 60000;
        long seconds = millis / 1000 % 60;
        long tenths = millis / 100 % 10;
        return String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, tenths);
    }

    private static double distanceToSegment(Location a, Location b, Location p) {
        double abx = b.getX() - a.getX();
        double aby = b.getY() - a.getY();
        double abz = b.getZ() - a.getZ();
        double lengthSquared = abx * abx + aby * aby + abz * abz;
        double t = 0;
        if (lengthSquared > 1.0E-6) {
            t = ((p.getX() - a.getX()) * abx + (p.getY() - a.getY()) * aby + (p.getZ() - a.getZ()) * abz) / lengthSquared;
            t = Math.max(0, Math.min(1, t));
        }
        double dx = a.getX() + abx * t - p.getX();
        double dy = a.getY() + aby * t - p.getY();
        double dz = a.getZ() + abz * t - p.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ------------------------------------------------------------------ depart de joueurs / regles

    @Override
    protected void onMemberLeft(UUID uuid, boolean wasParticipant, boolean disconnected) {
        Racer racer = racers.remove(uuid);
        released.remove(uuid);
        finishOrder.remove(uuid);
        if (racer != null && phase == Phase.RUNNING) {
            if (racers.isEmpty() || allDone()) {
                plugin.later(1L, this::finishRace);
            }
        } else if (racer != null && phase == Phase.COUNTDOWN && racers.isEmpty()) {
            plugin.later(1L, this::abortCountdown);
        }
    }

    private void abortCountdown() {
        if (phase == Phase.COUNTDOWN) {
            endMatch();
        }
    }

    @Override
    protected void onMatchReset() {
        racers.clear();
        finishOrder.clear();
        finishedCount = 0;
        graceLeft = -1;
    }

    @Override
    public void handleDeath(Player victim, Player killer) {
        Racer racer = racers.get(victim.getUniqueId());
        if (racer != null && phase == Phase.RUNNING && !racer.finished) {
            plugin.later(1L, () -> respawn(victim, racer));
        }
    }

    @Override
    public void handleVoid(Player player) {
        Racer racer = racers.get(player.getUniqueId());
        if (racer != null && phase == Phase.RUNNING && !racer.finished) {
            respawn(player, racer);
        } else if (!isMatchSpectator(player.getUniqueId())) {
            // Deja en mode spectateur (arrivee) : vol libre, on ne le recadre plus.
            arriveInStands(player);
        }
    }

    @Override
    public boolean canBuild(Player player) {
        return false;
    }

    @Override
    public boolean canInteract(Player player) {
        return racing(player.getUniqueId());
    }

    @Override
    public boolean canBreak(Player player, Block block) {
        return false;
    }

    @Override
    public boolean takesDamage(Player player) {
        return false;
    }

    @Override
    public boolean canFight(Player attacker, Player victim) {
        return false;
    }

    @Override
    public boolean frozen(Player player) {
        return phase == Phase.COUNTDOWN && participants.contains(player.getUniqueId());
    }

    @Override
    public boolean inventoryLocked(Player player) {
        return true;
    }

    /** Le joueur est-il en course (bateau verrouille : pas de sortie) ? */
    @Override
    public boolean racing(UUID uuid) {
        Racer racer = racers.get(uuid);
        return racer != null && !racer.finished && phase == Phase.RUNNING && !released.contains(uuid);
    }

    @Override
    protected Component statusFor(Player player) {
        Racer racer = racers.get(player.getUniqueId());
        if (racer != null && phase == Phase.RUNNING && !racer.finished) {
            long now = System.currentTimeMillis();
            return now < racer.noticeUntil ? null : racerStatus(racer, now);
        }
        if (isPublic()) {
            return queueStatus(player);
        }
        if (phase == Phase.WAITING) {
            return t("race.status-lobby", "<gold>Partie privée <white><code></white> <dark_gray>- <gray>en attente de l'hôte", "code", code);
        }
        return null;
    }
}
