package fr.kalium.parkour;

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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Parcours (course a pied chronometree avec points de controle), repris TEL QUEL de RaceInstance (KalGames 1.19.1),
 * sans la partie « bateau » (inactive depuis KalGames 1.17.0, la course de bateau etant dans KG_BoatRace). Aucun
 * changement de comportement ; textes : ceux du lang.yml de KalGames (race.*).
 *
 * 1.1.0 (demande de LeKiwi06, 26/09/2026 ; cahier des charges, "Chrono et fin de partie" et "Anti-collision") :
 * chrono qui s'allonge a chaque point de controle (remplace le temps maximum fixe entre deux points), a zero le joueur
 * "tombe au temps" (spectateur) ; pas de collision entre coureurs ; a moins de 3 blocs, chaque coureur voit ses
 * adversaires proches sous forme de bottes en cuir colorees (voir ProximityGhosts).
 *
 * 1.2.0 (demande de LeKiwi06, 04/10/2026) : nouveau bareme. Points de base de chaque section selon sa position dans le
 * parcours (1, 1, 3, 3, 3, 5, 5, 7, 7, 7, 10), multiplicateurs additifs (1er a valider, sans chute, temps), +25 au 1er
 * arrive ; rien de ce qui est « en 1er » en solo. Points decimaux, credites a chaque section (voir scoreSection).
 */
public final class ParkourInstance extends GameInstance {

    private static final int GRACE_SECONDS = 30;
    /** 1.2.0 : points de base des sections selon leur position dans le parcours (au-dela : comme la derniere). */
    private static final int[] BASE_POINTS = {1, 1, 3, 3, 3, 5, 5, 7, 7, 7, 10};

    private static final class Racer {
        int next;
        Location lastCheckpoint;
        Location previous;
        boolean finished;
        /** Elimine (temps maximum depasse entre deux points de controle). */
        boolean out;
        long finishMillis;
        int rank;
        /** Debut de la course (ou du tour d'entrainement) de ce joueur. */
        long runStart;
        /** Dernier point de controle atteint (ou depart). */
        long segmentStart;
        /** 1.1.0 : fin du chrono (le joueur tombe au temps a cet instant) ; 0 = pas de chrono. */
        long deadline;
        /** Entrainement : meilleur temps de la session, -1 si aucun. */
        long bestRun = -1;
        int runs;
        /** Jusqu'a cet instant, un message d'action bar (point de controle, retour...) reste affiche. */
        long noticeUntil;
        /** 1.2.0 : points de la course (bareme), comptes ou non pour les classements. */
        double points;
        /** 1.2.0 : retour au point de controle depuis la derniere section validee (plus de bonus « sans chute »). */
        boolean fell;
    }

    /** Parcours en partie privee : sans limite de temps, sans points ni classement. */
    private final boolean training;
    private final int privateCap;
    private final Map<UUID, Racer> racers = new LinkedHashMap<>();
    /** Joueurs en cours de teleportation par le jeu (retour au point de controle...). */
    private final java.util.Set<UUID> released = new java.util.HashSet<>();
    private final List<UUID> finishOrder = new ArrayList<>();
    /** 1.2.0 : sections deja validees par un coureur (bonus du 1er a valider). */
    private final java.util.BitSet sectionsTaken = new java.util.BitSet();
    /** Nombre de coureurs arrives (ne diminue pas si un arrive quitte la partie). */
    private int finishedCount;
    private long startMillis;
    private int graceLeft = -1;
    private int elapsed;
    /** 1.1.0 : anti-collision et bottes de proximite. */
    private final ProximityGhosts ghosts = new ProximityGhosts();

    public ParkourInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                           boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        this.training = !publicGame && options.get("training") instanceof Boolean flag && flag;
        int max = minigame.getInt("max-players", 8);
        this.privateCap = Math.max(1, Math.min(max, optionInt("maxPlayers", max)));
    }

    public boolean training() {
        return training;
    }

    /** Les points et temps comptent pour les classements ? */
    @Override
    public boolean ranked() {
        return !training && super.ranked();
    }

    public int privateCap() {
        return privateCap;
    }

    // ------------------------------------------------------------------ menu de la partie (KalGames 1.20.0)

    @Override
    public List<Component> menuInfo(Player player) {
        return training
                ? List.of(t("game.body-training", "<green>Mode entraînement : sans limite de temps, ni points, ni classement."))
                : List.of();
    }

    @Override
    public List<MenuAction> menuActions(Player player) {
        if (!training) {
            return List.of();
        }
        return List.of(new MenuAction(t("game.training-restart", "<green>Recommencer depuis le départ"), this::restartTraining));
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
        return Math.max(1, minigame().getInt("max-players", 8));
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
        sectionsTaken.clear();
        finishedCount = 0;
        graceLeft = -1;
        elapsed = 0;
        phase = Phase.COUNTDOWN;
        secondsLeft = Math.max(0, minigame().getInt("countdown-seconds", 5));

        Pos start = arena().point("start");
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Location spot = start != null ? loc(start) : stands();
            Racer racer = new Racer();
            racer.lastCheckpoint = spot.clone();
            racer.previous = spot.clone();
            racers.put(uuid, racer);

            plugin.hub().resetPlayer(player, GameMode.ADVENTURE);
            player.teleport(spot);
            plugin.hub().giveInGameItems(player, true);
            ghosts.join(player);
        }
        broadcastTo(participants, t("race.start", "<gold><mode> : <white><n></white> participant(s), <white><laps></white> tour(s).",
                "mode", "Parcours", "n", racers.size(), "laps", 1));
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
        startMillis = System.currentTimeMillis();
        elapsed = 0;
        long chrono = training ? 0L : Math.max(0, minigame().getInt("chrono-start-seconds", 30)) * 1000L;
        for (Racer racer : racers.values()) {
            racer.runStart = startMillis;
            racer.segmentStart = startMillis;
            racer.deadline = chrono > 0 ? startMillis + chrono : 0L;
        }
        title(participants, t("race.go-title", "<green><bold>Partez !"), Component.empty(), 0, 20, 10);
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1f);
            }
        }
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

    @Override
    protected void matchTick() {
        if (phase == Phase.COUNTDOWN || phase == Phase.RUNNING) {
            tickGhosts();
        }
        if (phase != Phase.RUNNING) {
            return;
        }
        int radius = minigame().getInt("checkpoint-radius", 3);
        int configuredVoid = minigame().getInt("void-y", -64);
        int voidY = configuredVoid <= -64 ? minY - 5 : configuredVoid;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Racer> entry : new ArrayList<>(racers.entrySet())) {
            Racer racer = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || racer.finished || racer.out || player.getWorld() != world) {
                continue;
            }
            if (racer.deadline > 0 && now >= racer.deadline) {
                eliminate(player, racer);
                continue;
            }
            Location current = player.getLocation();
            // Si le dernier point de controle est lui-meme sous la limite, on retombe sur la limite automatique (pas de boucle).
            double limit = racer.lastCheckpoint != null && racer.lastCheckpoint.getY() < voidY ? minY - 5 : voidY;
            if (current.getY() < limit) {
                respawn(player, racer);
                continue;
            }
            Location target = target(racer);
            if (target != null && distanceToSegment(racer.previous, current, target) <= radius) {
                reached(player, racer);
            }
            racer.previous = current;
            if (!racer.finished && !racer.out && now >= racer.noticeUntil) {
                player.sendActionBar(racerStatus(racer, now));
            }
        }
    }

    /** Coureurs encore en course (bottes de proximite), aussi pendant le compte a rebours. */
    private void tickGhosts() {
        int radius = minigame().getInt("ghost-radius", 3);
        List<Player> runners = new ArrayList<>();
        if (radius > 0) {
            for (Map.Entry<UUID, Racer> entry : racers.entrySet()) {
                Racer racer = entry.getValue();
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && !racer.finished && !racer.out && player.getWorld() == world) {
                    runners.add(player);
                }
            }
        }
        ghosts.tick(runners, world, radius);
    }

    /**
     * 1.1.0 : chrono a zero, le joueur "tombe au temps" (cahier des charges, point 2) : il passe spectateur ; ce n'est
     * pas un abandon, il a fini sa partie.
     */
    private void eliminate(Player player, Racer racer) {
        racer.out = true;
        ghosts.leave(player.getUniqueId());
        broadcast(t("race.chrono-out-broadcast", "<aqua><name></aqua> <red>tombe au temps.",
                "name", player.getName()));
        player.showTitle(net.kyori.adventure.title.Title.title(
                t("race.chrono-out-title", "<red><bold>Temps écoulé"),
                t("race.chrono-out-sub", "<gray>Tu as fini ta partie : regarde les autres coureurs.")));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
        player.leaveVehicle();
        becomeMatchSpectator(player);
        if (allDone()) {
            finishRace();
        }
    }

    /** Ligne d'action bar d'un coureur : chrono, points de controle atteints, temps restant. */
    private Component racerStatus(Racer racer, long now) {
        List<Pos> checkpoints = arena().list("checkpoints");
        long run = Math.max(0, now - racer.runStart);
        Component base = t(training ? "race.status-training" : "race.status-parkour",
                training ? "<green>Entraînement <dark_gray>| <gold>⏱ <time> <dark_gray>| <gray>Checkpoint <white><cp>/<total></white>"
                        : "<gold>⏱ <time> <dark_gray>| <gray>Checkpoint <white><cp>/<total></white>",
                "time", formatTenths(run), "cp", racer.next, "total", checkpoints.size());
        if (racer.deadline <= 0) {
            return base;
        }
        long left = Math.max(0, racer.deadline - now);
        return base.append(t(left < 10_000L ? "race.status-chrono-low" : "race.status-chrono",
                left < 10_000L ? " <dark_gray>| <gray>Chrono <red><left></red>" : " <dark_gray>| <gray>Chrono <white><left></white>",
                "left", formatClock((left + 999) / 1000)));
    }

    private void reached(Player player, Racer racer) {
        List<Pos> checkpoints = arena().list("checkpoints");
        if (racer.next < checkpoints.size()) {
            long now = System.currentTimeMillis();
            int section = racer.next;
            SectionScore score = scoreSection(racer, section, now - racer.segmentStart);
            racer.lastCheckpoint = facing(loc(checkpoints.get(racer.next)), player);
            racer.next++;
            racer.segmentStart = now;
            racer.noticeUntil = racer.segmentStart + 1500;
            // 1.1.0 : chaque point de controle ajoute du temps au chrono (CP 1 a 6 : +30 s, ensuite +60 s par defaut).
            long added = 0;
            if (racer.deadline > 0) {
                int lateFrom = Math.max(1, minigame().getInt("chrono-late-from-checkpoint", 7));
                added = Math.max(0, racer.next >= lateFrom
                        ? minigame().getInt("chrono-add-late-seconds", 60)
                        : minigame().getInt("chrono-add-seconds", 30));
                racer.deadline += added * 1000L;
            }
            // 1.2.0 : bareme de la section. Attribution toujours transmise (journal), comptee ou non selon la partie et
            // le joueur ; en entrainement (ni points ni classement) rien n'est affiche.
            racer.points += score.points();
            credit(player, score.points(), journal(section, score));
            String detail = detail(score);
            Component notice = score.points() > 0 && !training
                    ? t("race.checkpoint-score", "<green>Point de contrôle <white><n>/<total></white> <gold>+<points> pts<gray><detail>",
                            "n", racer.next, "total", checkpoints.size(), "points", fmt(score.points()),
                            "detail", detail.isEmpty() ? "" : " (" + detail + ")")
                    : t("race.checkpoint", "<green>Point de contrôle <white><n>/<total>",
                            "n", racer.next, "total", checkpoints.size());
            if (added > 0) {
                notice = notice.append(t("race.checkpoint-chrono", " <dark_gray>| <aqua>+<s> s", "s", added));
            }
            player.sendActionBar(notice);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
            return;
        }
        if (training) {
            trainingLap(player, racer);
            return;
        }
        racerFinished(player, racer);
    }

    // ------------------------------------------------------------------ entrainement

    private Location trainingStart() {
        Pos start = arena().point("start");
        return start != null ? loc(start) : stands();
    }

    /** Un joueur (re)commence le parcours d'entrainement depuis le depart. */
    private void startTraining(Player player) {
        UUID uuid = player.getUniqueId();
        Location spot = trainingStart();
        long now = System.currentTimeMillis();
        Racer racer = new Racer();
        racer.lastCheckpoint = spot.clone();
        racer.previous = spot.clone();
        racer.runStart = now;
        racer.segmentStart = now;
        Racer old = racers.put(uuid, racer);
        if (old != null) {
            racer.bestRun = old.bestRun;
            racer.runs = old.runs;
        }
        participants.add(uuid);
        plugin.hub().resetPlayer(player, GameMode.ADVENTURE);
        player.teleport(spot);
        plugin.hub().giveInGameItems(player, true);
        ghosts.join(player);
        if (old == null) {
            player.sendMessage(plugin.prefix().append(t("race.training-welcome",
                    "<green>Entraînement : pas de limite de temps, ni points, ni classement. Chute = retour au dernier point de contrôle ; à l'arrivée vous repartez du début.")));
        }
    }

    /** Menu de la partie : repartir du debut. */
    public void restartTraining(Player player) {
        if (training && phase == Phase.RUNNING && racers.containsKey(player.getUniqueId())) {
            startTraining(player);
        }
    }

    private void trainingLap(Player player, Racer racer) {
        long time = System.currentTimeMillis() - racer.runStart;
        racer.runs++;
        boolean best = racer.bestRun < 0 || time < racer.bestRun;
        if (best) {
            racer.bestRun = time;
        }
        player.showTitle(net.kyori.adventure.title.Title.title(
                t("race.training-finish", "<gold><bold>Arrivée !"),
                t(best ? "race.training-best" : "race.training-time", best ? "<white><time> <green>(meilleur temps)" : "<white><time>",
                        "time", formatTime(time))));
        player.sendMessage(plugin.prefix().append(t("race.training-result",
                "<gray>Tour d'entraînement n°<n> : <white><time></white> <dark_gray>(meilleur : <best>)",
                "n", racer.runs, "time", formatTime(time), "best", formatTime(racer.bestRun))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        Racer fresh = new Racer();
        Location spot = trainingStart();
        fresh.lastCheckpoint = spot.clone();
        fresh.previous = spot.clone();
        fresh.runStart = System.currentTimeMillis();
        fresh.segmentStart = fresh.runStart;
        fresh.bestRun = racer.bestRun;
        fresh.runs = racer.runs;
        racers.put(player.getUniqueId(), fresh);
        released.add(player.getUniqueId());
        player.teleport(spot);
        released.remove(player.getUniqueId());
        player.setFallDistance(0);
        player.setVelocity(new Vector());
    }

    @Override
    public void arriveInStands(Player player) {
        if (!training) {
            super.arriveInStands(player);
            return;
        }
        if (phase == Phase.WAITING) {
            phase = Phase.RUNNING;
            startMillis = System.currentTimeMillis();
            elapsed = 0;
        }
        if (phase == Phase.RUNNING) {
            startTraining(player);
        } else {
            super.arriveInStands(player);
        }
    }

    private void racerFinished(Player player, Racer racer) {
        racer.finished = true;
        ghosts.leave(player.getUniqueId());
        long now = System.currentTimeMillis();
        racer.finishMillis = now - racer.runStart;
        finishOrder.add(player.getUniqueId());
        racer.rank = ++finishedCount;
        // 1.2.0 : l'arrivee valide la derniere section ; le 1er arrive recoit en plus un bonus tel quel (jamais en solo).
        int section = arena().list("checkpoints").size();
        SectionScore score = scoreSection(racer, section, now - racer.segmentStart);
        int bonus = racer.rank == 1 && racers.size() > 1 ? Math.max(0, minigame().getInt("points-first-finish", 25)) : 0;
        double points = Math.round((score.points() + bonus) * 100) / 100.0;
        racer.points += points;
        broadcast(t("race.finished", "<aqua><name></aqua> <green>termine <white>n°<rank></white> en <white><time></white>.",
                "name", player.getName(), "rank", racer.rank, "time", formatTime(racer.finishMillis)));
        if (plugin.scores().recordTime(this, player, racer.finishMillis)) {
            player.sendMessage(plugin.prefix().append(t("race.record", "<light_purple>Nouveau record personnel : <white><time></white> !",
                    "time", formatTime(racer.finishMillis))));
        }
        Map<String, Object> fields = journal(section, score);
        fields.put("finishBonus", bonus);
        credit(player, points, fields);
        if (points > 0) {
            List<String> why = new ArrayList<>();
            if (score.points() > 0) {
                String detail = detail(score);
                why.add("arrivée " + fmt(score.base()) + (detail.isEmpty() ? "" : " " + detail));
            }
            if (bonus > 0) {
                why.add("1er arrivé +" + bonus);
            }
            player.sendMessage(plugin.prefix().append(t("race.finish-score",
                    "<gold>+<points> pts <gray>(<detail>) - total <white><total></white> pts",
                    "points", fmt(points), "detail", String.join(" ; ", why), "total", fmt(racer.points))));
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

    // ------------------------------------------------------------------ bareme (1.2.0)

    /** 1.2.0 : points d'une section (du point precedent a ce point de controle, ou a l'arrivee) et leur detail. */
    private record SectionScore(double points, int base, double multiplier, boolean first, boolean clean,
                                double timeCoefficient, long millis) {
    }

    /** Coefficient d'un reglage en dixiemes ou en centiemes (unit = 10 ou 100), jamais sous x1. */
    private double coefficient(String key, int def, int unit) {
        return Math.max(unit, minigame().getInt(key, def)) / (double) unit;
    }

    /**
     * Points de base d'une section (0 = premiere) : reglage « points » du point de controle, sinon selon sa position
     * (BASE_POINTS). L'arrivee compte comme la section qui suit le dernier point de controle ; au-dela du tableau elle
     * ne rapporte rien (reste le bonus du 1er arrive).
     */
    private int basePoints(int section, int checkpoints) {
        if (section >= checkpoints) {
            return section < BASE_POINTS.length ? BASE_POINTS[section] : 0;
        }
        if (arena().pointSetting("checkpoints", section, "points") instanceof Number own && own.intValue() > 0) {
            return own.intValue();
        }
        return BASE_POINTS[Math.min(section, BASE_POINTS.length - 1)];
    }

    /** Temps du bonus d'une section, en secondes (0 = pas de bonus de temps). */
    private int bonusSeconds(int section, int checkpoints) {
        if (section >= checkpoints) {
            return Math.max(0, minigame().getInt("finish-time-seconds", 0));
        }
        return arena().pointSetting("checkpoints", section, "time-seconds") instanceof Number own ? Math.max(0, own.intValue()) : 0;
    }

    /**
     * 1.2.0 : bareme d'une section (demande de LeKiwi06, 04/10/2026). Points de base selon la section, puis les
     * multiplicateurs, ADDITIFS comme pour la course de bateau (x1,5 et x1,5 = x2) : 1er a valider la section (jamais
     * en solo), section sans chute (aucun retour au point de controle), temps de la section sous le temps du bonus
     * (x1,25 juste en dessous, jusqu'a x1,75 a la moitie de ce temps ou moins).
     */
    private SectionScore scoreSection(Racer racer, int section, long millis) {
        int checkpoints = arena().list("checkpoints").size();
        int base = basePoints(section, checkpoints);
        boolean first = !training && racers.size() > 1 && !sectionsTaken.get(section);
        sectionsTaken.set(section);
        boolean clean = !racer.fell;
        racer.fell = false;
        double timeCoefficient = 1;
        int seconds = bonusSeconds(section, checkpoints);
        if (seconds > 0 && millis < seconds * 1000L) {
            double min = coefficient("time-coef-min-x100", 125, 100);
            double max = Math.max(min, coefficient("time-coef-max-x100", 175, 100));
            double ratio = Math.min(1, (seconds * 1000L - millis) / (seconds * 500.0));
            timeCoefficient = Math.round((min + (max - min) * ratio) * 100) / 100.0;
        }
        double multiplier = 1 + (first ? coefficient("first-coef-x10", 15, 10) - 1 : 0)
                + (clean ? coefficient("clean-coef-x10", 15, 10) - 1 : 0) + (timeCoefficient - 1);
        multiplier = Math.round(multiplier * 100) / 100.0;
        double points = Math.round(base * multiplier * 100) / 100.0;
        return new SectionScore(points, base, multiplier, first, clean, timeCoefficient, millis);
    }

    /** Detail des multiplicateurs d'une section, ex. « x2,25 : 1er, sans chute, temps x1,25 » (vide si x1). */
    private static String detail(SectionScore score) {
        if (score.multiplier() <= 1) {
            return "";
        }
        List<String> why = new ArrayList<>();
        if (score.first()) {
            why.add("1er");
        }
        if (score.clean()) {
            why.add("sans chute");
        }
        if (score.timeCoefficient() > 1) {
            why.add("temps x" + fmt(score.timeCoefficient()));
        }
        return "x" + fmt(score.multiplier()) + " : " + String.join(", ", why);
    }

    /** Detail du bareme d'une section pour le journal de KG_ScoreBoards (section : 1 = premiere). */
    private static Map<String, Object> journal(int section, SectionScore score) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("section", section + 1);
        fields.put("sectionMillis", score.millis());
        fields.put("base", score.base());
        fields.put("multiplier", score.multiplier());
        fields.put("first", score.first());
        fields.put("clean", score.clean());
        fields.put("timeCoefficient", score.timeCoefficient());
        return fields;
    }

    /**
     * 1.2.0 : credite des points decimaux (ScoreBridge.award de KalGames ne prend que des entiers) : meme evenement
     * « points » du journal de KG_ScoreBoards, compte ou non selon la partie et le joueur, avec le detail du bareme.
     * Renvoie true si les points ont ete comptes. La passerelle vers le datapack (points entiers) n'est pas appelee,
     * comme pour la course de bateau.
     */
    private boolean credit(Player player, double points, Map<String, Object> detail) {
        if (points <= 0) {
            return false;
        }
        String reason = plugin.scores().excluded(player) ? "operateur" : !ranked(player) ? "partie-non-classee" : null;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("player", player.getUniqueId().toString());
        fields.put("name", player.getName());
        fields.put("platform", player.getUniqueId().getMostSignificantBits() == 0 ? "bedrock" : "java");
        fields.put("points", points);
        fields.put("counted", reason == null);
        fields.put("reason", reason);
        fields.putAll(detail);
        plugin.ranking().log(minigame().id(), "points", fields);
        if (reason != null) {
            return false;
        }
        plugin.ranking().stats().addPoints(minigame().id(), player.getUniqueId(), player.getName(), points);
        plugin.ranking().boards().refreshSoon();
        return true;
    }

    private static String fmt(double value) {
        return fr.kalium.scoreboards.data.StatsService.formatPoints(value);
    }

    @Override
    public void releaseHold(UUID uuid) {
        released.add(uuid);
    }

    /**
     * 1.19.1 (KalGames) : le retour au point de controle garde l'orientation de la camera du joueur au moment ou il
     * l'a passe, et non celle enregistree avec l'arene.
     */
    private static Location facing(Location spot, Player player) {
        spot.setYaw(player.getLocation().getYaw());
        spot.setPitch(player.getLocation().getPitch());
        return spot;
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
        racer.fell = true;
        racer.noticeUntil = System.currentTimeMillis() + 1500;
        player.sendActionBar(t("race.respawn", "<yellow>Retour au dernier point de contrôle."));
    }

    /** Objet "dernier checkpoint" du parcours. */
    @Override
    public void useCheckpointItem(Player player) {
        Racer racer = racers.get(player.getUniqueId());
        if (racer != null && phase == Phase.RUNNING && !racer.finished && !racer.out) {
            respawn(player, racer);
        }
    }

    private boolean allDone() {
        for (Racer racer : racers.values()) {
            if (!racer.finished && !racer.out) {
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
                if (training) {
                    return;
                }
                elapsed++;
                int limit = minigame().getInt("time-limit-seconds", 600);
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

    private void finishRace() {
        if (phase != Phase.RUNNING) {
            return;
        }
        phase = Phase.ENDING;
        secondsLeft = 6;
        ghosts.clear();
        List<Map.Entry<UUID, Racer>> unfinished = new ArrayList<>();
        for (Map.Entry<UUID, Racer> entry : racers.entrySet()) {
            if (!entry.getValue().finished) {
                unfinished.add(entry);
            }
        }
        unfinished.sort(Comparator
                .comparingInt((Map.Entry<UUID, Racer> e) -> -e.getValue().next)
                .thenComparingDouble(e -> {
                    Player player = Bukkit.getPlayer(e.getKey());
                    Location target = target(e.getValue());
                    if (player == null || target == null || player.getWorld() != world) {
                        return Double.MAX_VALUE;
                    }
                    return player.getLocation().distance(target);
                }));

        broadcast(t("race.results", "<gold><bold>Résultats"));
        int position = 1;
        for (UUID uuid : finishOrder) {
            Racer racer = racers.get(uuid);
            String name = nameOf(uuid);
            broadcast(t("race.result-line-points", "<gray><rank>. <white><name></white> <dark_gray>- <aqua><time> <gray>(<points> pts)",
                    "rank", position, "name", name, "time", formatTime(racer.finishMillis), "points", fmt(racer.points)));
            position++;
        }
        // Les coureurs qui n'ont pas fini a temps (elimines ou temps ecoule) restent classes a la suite des arrives.
        // 1.2.0 : plus de points de podium ; chacun garde les points de ses sections, deja credites.
        for (Map.Entry<UUID, Racer> entry : unfinished) {
            Racer racer = entry.getValue();
            racer.rank = position;
            broadcast(racer.out
                    ? t("race.result-chrono-out-points", "<gray><rank>. <white><name></white> <dark_gray>- <red>tombé au temps <gray>(<points> pts)",
                            "rank", position, "name", nameOf(entry.getKey()), "points", fmt(racer.points))
                    : t("race.result-dnf-points", "<gray><rank>. <white><name></white> <dark_gray>- <red>non arrivé <gray>(<points> pts)",
                            "rank", position, "name", nameOf(entry.getKey()), "points", fmt(racer.points)));
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

    private static String formatClock(long seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
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
        ghosts.leave(uuid);
        finishOrder.remove(uuid);
        if (training) {
            return;
        }
        if (racer != null && phase == Phase.RUNNING) {
            if (racers.isEmpty() || allDone()) {
                plugin.later(1L, this::finishRace);
            }
        } else if (racer != null && (phase == Phase.COUNTDOWN) && racers.isEmpty()) {
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
        ghosts.clear();
        racers.clear();
        finishOrder.clear();
        sectionsTaken.clear();
        finishedCount = 0;
        graceLeft = -1;
    }

    @Override
    public void handleDeath(Player victim, Player killer) {
        Racer racer = racers.get(victim.getUniqueId());
        if (racer != null && phase == Phase.RUNNING && !racer.finished && !racer.out) {
            plugin.later(1L, () -> respawn(victim, racer));
        }
    }

    @Override
    public void handleVoid(Player player) {
        Racer racer = racers.get(player.getUniqueId());
        if (racer != null && phase == Phase.RUNNING && !racer.finished && !racer.out) {
            respawn(player, racer);
        } else if (!isMatchSpectator(player.getUniqueId())) {
            // Deja en mode spectateur (arrivee ou elimination) : vol libre, on ne le recadre plus.
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

    /** Le joueur est-il en course ? */
    @Override
    public boolean racing(UUID uuid) {
        Racer racer = racers.get(uuid);
        return racer != null && !racer.finished && !racer.out && phase == Phase.RUNNING && !released.contains(uuid);
    }

    @Override
    protected Component statusFor(Player player) {
        UUID uuid = player.getUniqueId();
        Racer racer = racers.get(uuid);
        if (racer != null && phase == Phase.RUNNING && !racer.finished && !racer.out) {
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
