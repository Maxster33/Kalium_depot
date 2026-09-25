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
 *
 * 1.1.0 (etape 1 du cahier des charges) : vitesse en km/h, seuil d'enregistrement des meilleurs tours (45 s),
 * passages aux checkpoints mesures (temps, place, vitesse d'entree).
 *
 * 1.2.0 (etape 2) : classement en direct (tableau lateral), ecarts a chaque checkpoint (avec le coureur juste devant
 * et avec son propre tour precedent), donnees de chaque tour et de chaque course envoyees au journal de
 * KG_ScoreBoards (graphiques et futur bot Discord).
 *
 * 1.3.0 (etapes 3 et 7) : bareme de points (remplace les points du podium), detection du hors-piste (bateau qui
 * touche autre chose que les blocs de piste), anti-collision entre bateaux (CollisionShield), tableau lateral aere
 * avec le meilleur tour de la course.
 *
 * 1.4.0 : anti-collision etendu aux joueurs Bedrock ; hors-piste verifie sur tout le trajet et contact lateral compte
 * seulement s'il ralentit le bateau ; record personnel du joueur dans son tableau lateral.
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
        /** 1.1.0 : vitesse lissee (km/h), calculee a partir du deplacement entre deux passages de la boucle. */
        double speedKmh;
        /**
         * 1.1.0 : passages aux checkpoints du tour en cours, dans l'ordre (temps depuis le debut du tour, place au
         * passage, vitesse d'entree). Donnees des futurs classements en direct, ecarts et graphiques (cahier des
         * charges, etape 1).
         */
        final List<Split> splits = new ArrayList<>();
        /** Passages du tour precedent (ecart avec son propre passage precedent : etape 2). */
        List<Split> previousLapSplits = List.of();
        /** 1.2.0 : instant (ms) de chaque passage, par (tour, checkpoint) : ecarts et classement en direct. */
        final Map<Long, Long> passAt = new java.util.HashMap<>();
        String name = "?";
        /** 1.3.0 : hors-piste pendant le tour en cours. */
        boolean lapOffTrack;
        /** 1.4.0 : position du bateau au controle precedent et vitesse instantanee (hors-piste). */
        Location boatBefore;
        double instantKmh;
        /** 1.3.0 : tours propres d'affilee depuis la derniere serie validee, et series validees d'affilee. */
        int cleanStreak;
        int series;
        /** 1.3.0 : points de la course (somme des tours) et nombre de hors-piste. */
        double points;
        int offTracks;
    }

    /** Passage a un checkpoint : temps depuis le debut du tour (ms), place au passage, vitesse d'entree (km/h). */
    record Split(int checkpoint, long lapMillis, int place, double speedKmh) {
    }

    /** 1.2.0 : identifiant unique de la course en cours (journal de KG_ScoreBoards). */
    private String matchId = "";
    /** 1.2.0 : classement en direct (tableau lateral propre a la course) et tableaux des joueurs a leur rendre. */
    private final Map<UUID, org.bukkit.scoreboard.Scoreboard> boards = new java.util.HashMap<>();
    private final Map<UUID, org.bukkit.scoreboard.Scoreboard> previousBoards = new java.util.HashMap<>();

    /** 1.3.0 : anti-collision (null si desactive) et blocs de piste (hors-piste = tout autre bloc touche). */
    private CollisionShield shield;
    private Set<org.bukkit.Material> trackBlocks = Set.of();
    /** 1.3.0 : meilleur tour de la course en cours (tableau lateral). */
    private String bestLapName;
    private long bestLapMillis = -1;

    private static long key(int lap, int cp) {
        return ((long) lap << 32) | cp;
    }

    /** Nombre de passages deja enregistres a chaque (tour, checkpoint) : donne la place au passage. */
    private final Map<Long, Integer> passages = new java.util.HashMap<>();

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
        passages.clear();
        matchId = UUID.randomUUID().toString();
        bestLapName = null;
        bestLapMillis = -1;
        trackBlocks = parseTrackBlocks(minigame().getText("track-blocks", "PACKED_ICE,BLUE_ICE"));
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
            racer.name = player.getName();
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
        // 1.3.0 : anti-collision (reglage, active par defaut).
        if (minigame().getBool("anti-collision", true) && racers.size() > 1) {
            List<Player> online = new ArrayList<>();
            for (UUID uuid : racers.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    online.add(player);
                }
            }
            shield = new CollisionShield(org.bukkit.plugin.java.JavaPlugin.getPlugin(KGBoatRace.class), world,
                    minigame().getText("boat-type", "OAK_BOAT"));
            shield.start(online);
        }
    }

    /** 1.3.0 : blocs de piste (reglage « track-blocks », noms de blocs separes par des virgules). */
    private static Set<org.bukkit.Material> parseTrackBlocks(String text) {
        Set<org.bukkit.Material> result = new HashSet<>();
        for (String part : text.split(",")) {
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(part.trim());
            if (material != null && material.isBlock()) {
                result.add(material);
            }
        }
        if (result.isEmpty()) {
            result.add(org.bukkit.Material.PACKED_ICE);
            result.add(org.bukkit.Material.BLUE_ICE);
        }
        return result;
    }

    /**
     * 1.3.0 : hors-piste (cahier des charges) : le bateau touche un bloc autre qu'un bloc de piste (glace compacte ou
     * glace bleue par defaut) d'une facon qui le freine : sous lui (sol, eau...) ou sur ses cotes (mur, bordure). En
     * l'air (saut), rien n'est compte. Une seule fois par tour : le tour n'est plus « propre ».
     */
    private void checkOffTrack(Player player, Racer racer) {
        if (!(player.getVehicle() instanceof org.bukkit.entity.Boat boat)) {
            racer.instantKmh = 0;
            return;
        }
        org.bukkit.util.BoundingBox box = boat.getBoundingBox();
        Location now = boat.getLocation();
        Location before = racer.boatBefore;
        racer.boatBefore = now.clone();
        double dx = 0;
        double dz = 0;
        double dy = 0;
        if (before != null && before.getWorld() == now.getWorld()) {
            dx = now.getX() - before.getX();
            dy = now.getY() - before.getY();
            dz = now.getZ() - before.getZ();
        }
        double distance = Math.sqrt(dx * dx + dz * dz);
        double instant = distance * 20.0 / GameInstance.TICK_INTERVAL * 3.6;
        boolean slowed = racer.instantKmh > 15 && instant < racer.instantKmh * 0.85;
        racer.instantKmh = instant;
        if (racer.lapOffTrack || distance > 20) {
            return; // deja compte pour ce tour, ou teleportation
        }
        // 1.4.0 : tout le trajet depuis le controle precedent est verifie (tous les 0,4 bloc), et plus seulement la
        // position actuelle : a 140 km/h le bateau parcourt ~4 blocs entre deux controles.
        int steps = Math.max(1, (int) Math.ceil(distance / 0.4));
        boolean touching = false;
        for (int i = 0; i < steps && !touching; i++) {
            double back = (double) i / steps; // 0 = position actuelle, vers 1 = position precedente
            org.bukkit.util.BoundingBox at = box.clone().shift(-dx * back, -dy * back, -dz * back);
            touching = groundOffTrack(at) || (slowed && sideContact(at));
        }
        if (touching) {
            racer.lapOffTrack = true;
            racer.offTracks++;
            racer.noticeUntil = System.currentTimeMillis() + 1500;
            player.sendActionBar(t("race.off-track", "<red><bold>Hors-piste !</bold> <gray>Bonus « tour propre » perdu pour ce tour."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
        }
    }

    /** Sous le bateau (coins et centre, juste sous la coque) : un bloc qui n'est pas un bloc de piste. En l'air : rien. */
    private boolean groundOffTrack(org.bukkit.util.BoundingBox box) {
        double y = box.getMinY() - 0.05;
        double[][] under = {{box.getMinX() + 0.1, box.getMinZ() + 0.1}, {box.getMaxX() - 0.1, box.getMinZ() + 0.1},
                {box.getMinX() + 0.1, box.getMaxZ() - 0.1}, {box.getMaxX() - 0.1, box.getMaxZ() - 0.1}, {box.getCenterX(), box.getCenterZ()}};
        for (double[] point : under) {
            org.bukkit.block.Block block = world.getBlockAt((int) Math.floor(point[0]), (int) Math.floor(y), (int) Math.floor(point[1]));
            if (!block.getType().isAir() && !trackBlocks.contains(block.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sur les cotes : un bloc solide autre qu'un bloc de piste (mur, bordure) contre la coque. 1.4.0 : ne compte que si
     * le bateau vient de ralentir (regle du cahier des charges : un contact qui freine) - un simple frolement non.
     */
    private boolean sideContact(org.bukkit.util.BoundingBox box) {
        double margin = 0.12;
        org.bukkit.util.BoundingBox outer = box.clone().expand(margin, 0, margin);
        int minX = (int) Math.floor(outer.getMinX());
        int maxX = (int) Math.floor(outer.getMaxX());
        int minZ = (int) Math.floor(outer.getMinZ());
        int maxZ = (int) Math.floor(outer.getMaxZ());
        int yLow = (int) Math.floor(box.getMinY() + 0.2);
        int yHigh = (int) Math.floor(box.getMinY() + 0.5);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int by = yLow; by <= yHigh; by++) {
                    org.bukkit.block.Block block = world.getBlockAt(bx, by, bz);
                    if (block.isPassable() || trackBlocks.contains(block.getType())) {
                        continue;
                    }
                    for (org.bukkit.util.BoundingBox part : block.getCollisionShape().getBoundingBoxes()) {
                        if (part.shift(bx, by, bz).overlaps(outer)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
        if (shield != null) {
            shield.tick();
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
            updateSpeed(racer, current);
            checkOffTrack(player, racer);
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

    /**
     * 1.1.0 : vitesse en km/h a partir du deplacement horizontal depuis le passage precedent de la boucle
     * (GameInstance.TICK_INTERVAL ticks), lissee pour eviter les sauts d'affichage. 1 bloc = 1 metre.
     */
    private static void updateSpeed(Racer racer, Location current) {
        if (racer.previous == null || racer.previous.getWorld() != current.getWorld()) {
            return;
        }
        double dx = current.getX() - racer.previous.getX();
        double dz = current.getZ() - racer.previous.getZ();
        double metersPerSecond = Math.sqrt(dx * dx + dz * dz) * 20.0 / GameInstance.TICK_INTERVAL;
        double kmh = metersPerSecond * 3.6;
        if (kmh > 400) {
            return; // teleportation (retour au checkpoint...) : pas une vraie vitesse
        }
        racer.speedKmh = racer.speedKmh * 0.6 + kmh * 0.4;
    }

    /** Ligne d'action bar d'un coureur : chrono, tour, points de controle atteints. */
    private Component racerStatus(Racer racer, long now) {
        List<Pos> checkpoints = arena().list("checkpoints");
        long run = Math.max(0, now - racer.runStart);
        return t("race.status-speed", "<gold><time> <dark_gray>| <aqua><speed> km/h <dark_gray>| <gray>Tour <white><lap>/<laps></white> <dark_gray>| <gray>Contrôle <white><cp>/<total></white>",
                "time", formatTenths(run), "speed", Math.round(racer.speedKmh), "lap", racer.lap + 1, "laps", laps,
                "cp", racer.next, "total", checkpoints.size());
    }

    private void reached(Player player, Racer racer) {
        List<Pos> checkpoints = arena().list("checkpoints");
        if (racer.next < checkpoints.size()) {
            Split split = recordSplit(racer, racer.next);
            racer.lastCheckpoint = loc(checkpoints.get(racer.next));
            racer.next++;
            racer.noticeUntil = System.currentTimeMillis() + 2500;
            player.sendActionBar(t("race.checkpoint", "<green>Point de contrôle <white><n>/<total>",
                            "n", racer.next, "total", checkpoints.size())
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(gapLine(racer, split)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
            return;
        }
        Split finishSplit = recordSplit(racer, checkpoints.size()); // la ligne d'arrivee compte comme dernier passage du tour
        Component finishGap = gapLine(racer, finishSplit);
        racer.lap++;
        recordLap(player, racer);
        racer.previousLapSplits = List.copyOf(racer.splits);
        racer.splits.clear();
        if (racer.lap >= laps) {
            racerFinished(player, racer);
        } else {
            racer.next = 0;
            racer.noticeUntil = System.currentTimeMillis() + 1500;
            racer.lastCheckpoint = loc(arena().point("finish"));
            racer.noticeUntil = System.currentTimeMillis() + 2500;
            player.sendActionBar(t("race.lap", "<green>Tour <white><n>/<total>", "n", racer.lap + 1, "total", laps)
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(finishGap));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        }
    }

    /** 1.1.0 : passage au checkpoint n° cp (ou a la ligne d'arrivee) du tour en cours. */
    private Split recordSplit(Racer racer, int cp) {
        long key = key(racer.lap, cp);
        int place = passages.merge(key, 1, Integer::sum);
        long now = System.currentTimeMillis();
        racer.passAt.put(key, now);
        Split split = new Split(cp, now - racer.lapStart, place, racer.speedKmh);
        racer.splits.add(split);
        return split;
    }

    /**
     * 1.2.0 : ecarts au passage d'un checkpoint (ou de la ligne d'arrivee, cp = nombre de checkpoints) : avec le coureur
     * passe juste avant (meme tour, meme checkpoint) et avec son propre passage au tour precedent.
     */
    private Component gapLine(Racer racer, Split split) {
        long key = key(racer.lap, split.checkpoint());
        long mine = racer.passAt.get(key);
        Component ahead;
        if (split.place() == 1) {
            ahead = t("race.gap-leader", "<gold>P1 <gray>en tête");
        } else {
            long best = Long.MIN_VALUE;
            for (Racer other : racers.values()) {
                Long at = other == racer ? null : other.passAt.get(key);
                if (at != null && at <= mine && at > best) {
                    best = at;
                }
            }
            ahead = best == Long.MIN_VALUE
                    ? t("race.gap-place", "<gold>P<place>", "place", split.place())
                    : t("race.gap-ahead", "<gold>P<place> <red>+<gap> <gray>sur le précédent", "place", split.place(), "gap", seconds(mine - best));
        }
        Split before = null;
        for (Split previous : racer.previousLapSplits) {
            if (previous.checkpoint() == split.checkpoint()) {
                before = previous;
            }
        }
        if (before == null) {
            return ahead;
        }
        long delta = split.lapMillis() - before.lapMillis();
        return ahead.append(delta <= 0
                ? t("race.gap-self-better", " <dark_gray>| <green>-<gap> <gray>vs tour préc.", "gap", seconds(-delta))
                : t("race.gap-self-worse", " <dark_gray>| <red>+<gap> <gray>vs tour préc.", "gap", seconds(delta)));
    }

    private static String seconds(long millis) {
        return String.format(Locale.ROOT, "%d.%02d", millis / 1000, millis / 10 % 100);
    }

    /** 1.3.0 : points d'un tour et leur detail (affiche au joueur). */
    record LapScore(double points, double base, double multiplier, boolean clean, int seriesBonus, boolean lead,
                    double timeCoefficient, String detail) {
    }

    private double coefficient(String key, int defX10) {
        return Math.max(10, minigame().getInt(key, defX10)) / 10.0;
    }

    /**
     * 1.3.0 : bareme d'un tour (cahier des charges). D'abord les points : 1 par tour (+1 si tour sans hors-piste, +n a
     * chaque serie de 3 tours propres d'affilee, n = numero de la serie ; un hors-piste remet la serie a zero), puis
     * les multiplicateurs, ADDITIFS (x1,5 et x1,5 = x2) : chrono du tour (paliers reglables) et tour en tete (tous les
     * checkpoints du tour passes en 1er).
     */
    private LapScore scoreLap(Racer racer, long lapTime) {
        boolean clean = !racer.lapOffTrack;
        double base = minigame().getInt("points-lap", 1) + (clean ? minigame().getInt("points-clean-lap", 1) : 0);
        int seriesBonus = 0;
        if (clean) {
            racer.cleanStreak++;
            if (racer.cleanStreak >= Math.max(1, minigame().getInt("series-length", 3))) {
                racer.series++;
                seriesBonus = racer.series;
                racer.cleanStreak = 0;
            }
        } else {
            racer.cleanStreak = 0;
            racer.series = 0;
        }
        base += seriesBonus;
        double timeCoefficient = 1;
        for (int tier = 4; tier >= 1; tier--) { // du palier le plus rapide au plus lent
            int seconds = minigame().getInt("tier" + tier + "-seconds", new int[]{0, 45, 40, 35, 30}[tier]);
            if (seconds > 0 && lapTime < seconds * 1000L) {
                timeCoefficient = coefficient("tier" + tier + "-coef-x10", new int[]{0, 15, 20, 30, 50}[tier]);
                break;
            }
        }
        boolean lead = !racer.splits.isEmpty();
        for (Split split : racer.splits) {
            lead &= split.place() == 1;
        }
        double multiplier = 1 + (timeCoefficient - 1) + (lead ? coefficient("lead-coef-x10", 15) - 1 : 0);
        double points = Math.round(base * multiplier * 100) / 100.0;
        racer.points += points;
        racer.lapOffTrack = false;
        StringBuilder detail = new StringBuilder(fmt(base) + " pt");
        if (!clean) {
            detail.append(", hors-piste");
        }
        if (seriesBonus > 0) {
            detail.append(", série +").append(seriesBonus);
        }
        if (multiplier > 1) {
            detail.append(" x").append(fmt(multiplier));
            List<String> why = new ArrayList<>();
            if (timeCoefficient > 1) {
                why.add("chrono x" + fmt(timeCoefficient));
            }
            if (lead) {
                why.add("en tête");
            }
            detail.append(" : ").append(String.join(", ", why));
        }
        return new LapScore(points, base, multiplier, clean, seriesBonus, lead, timeCoefficient, detail.toString());
    }

    private static String fmt(double value) {
        return fr.kalium.scoreboards.data.StatsService.formatPoints(value);
    }

    /** 1.2.0 : Bedrock (Geyser / Floodgate : identifiant commencant par des zeros) ou Java. */
    private static String platform(UUID uuid) {
        return uuid.getMostSignificantBits() == 0 ? "bedrock" : "java";
    }

    /**
     * 1.2.0 : ligne « lap » du journal de KG_ScoreBoards : un tour termine, avec chaque passage (checkpoint, temps
     * depuis le debut du tour, place, vitesse d'entree en km/h ; le dernier = ligne d'arrivee).
     */
    private void logLap(Player player, Racer racer, long lapTime, boolean recorded, LapScore score) {
        List<Map<String, Object>> splits = new ArrayList<>();
        for (Split split : racer.splits) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("cp", split.checkpoint());
            one.put("ms", split.lapMillis());
            one.put("place", split.place());
            one.put("kmh", Math.round(split.speedKmh() * 10) / 10.0);
            splits.add(one);
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId);
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("player", player.getUniqueId().toString());
        fields.put("name", player.getName());
        fields.put("platform", platform(player.getUniqueId()));
        fields.put("ranked", ranked(player));
        fields.put("lap", racer.lap);
        fields.put("laps", laps);
        fields.put("lapMillis", lapTime);
        fields.put("recorded", recorded);
        fields.put("checkpoints", arena().list("checkpoints").size());
        fields.put("splits", splits);
        fields.put("points", score.points());
        fields.put("base", score.base());
        fields.put("multiplier", score.multiplier());
        fields.put("clean", score.clean());
        fields.put("seriesBonus", score.seriesBonus());
        fields.put("lead", score.lead());
        fields.put("timeCoefficient", score.timeCoefficient());
        plugin.ranking().log(minigame().id(), "lap", fields);
    }

    /** Temps du tour qui vient d'etre termine : classement des meilleurs temps sur 1 tour. */
    private void recordLap(Player player, Racer racer) {
        long now = System.currentTimeMillis();
        long lapTime = now - racer.lapStart;
        racer.lapStart = now;
        if (bestLapMillis < 0 || lapTime < bestLapMillis) {
            bestLapMillis = lapTime;
            bestLapName = player.getName();
        }
        LapScore score = scoreLap(racer, lapTime);
        player.sendMessage(plugin.prefix().append(t("race.lap-points",
                "<gold>+<points> pt(s) <dark_gray>(<detail>) <gray>- total <white><total></white> pts",
                "points", fr.kalium.scoreboards.data.StatsService.formatPoints(score.points()), "detail", score.detail(),
                "total", fr.kalium.scoreboards.data.StatsService.formatPoints(racer.points))));
        // 1.1.0 : seuls les tours sous le seuil (45 s par defaut, reglable) comptent pour les meilleurs temps.
        int maxSeconds = minigame().getInt("record-max-lap-seconds", 45);
        logLap(player, racer, lapTime, maxSeconds <= 0 || lapTime <= maxSeconds * 1000L, score);
        if (maxSeconds > 0 && lapTime > maxSeconds * 1000L) {
            player.sendMessage(plugin.prefix().append(t("race.lap-time-unrecorded",
                    "<gray>Tour <white><n></white> : <white><time></white> <dark_gray>(non enregistré : plus de <max> s)",
                    "n", racer.lap, "time", formatTime(lapTime), "max", maxSeconds)));
            return;
        }
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
        if (shield != null) {
            shield.release(player.getUniqueId());
        }
        broadcast(t("race.finished", "<aqua><name></aqua> <green>termine <white>n°<rank></white> en <white><time></white>.",
                "name", player.getName(), "rank", racer.rank, "time", formatTime(racer.finishMillis)));
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
                updateBoard();
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

        if (shield != null) {
            shield.stop();
            shield = null;
        }
        // 1.3.0 : Grand Prix (nombre de tours du reglage « gp-laps », 40 par defaut) termine en entier : bonus sur le
        // total de la course.
        int gpLaps = minigame().getInt("gp-laps", 40);
        for (Racer racer : racers.values()) {
            if (gpLaps > 0 && laps >= gpLaps && racer.finished) {
                racer.points = Math.round(racer.points * coefficient("gp-coef-x10", 15) * 100) / 100.0;
            }
        }
        Map<UUID, Double> cumulative = cumulativePoints();
        logRace(unfinished, cumulative);
        broadcast(t("race.results", "<gold><bold>Résultats"));
        int position = 1;
        for (UUID uuid : finishOrder) {
            Racer racer = racers.get(uuid);
            broadcast(t("race.result-line-points", "<gray><rank>. <white><name></white> <dark_gray>- <aqua><time> <gray>(<points> pts)",
                    "rank", position, "name", nameOf(uuid), "time", formatTime(racer.finishMillis), "points", fmt(racer.points)));
            position++;
        }
        for (Map.Entry<UUID, Racer> entry : unfinished) {
            Racer racer = entry.getValue();
            racer.rank = position;
            broadcast(t("race.result-dnf-points", "<gray><rank>. <white><name></white> <dark_gray>- <red>non arrivé <gray>(<points> pts)",
                    "rank", position, "name", nameOf(entry.getKey()), "points", fmt(racer.points)));
            position++;
        }
        // Classement aux points : points de la course, et entre parentheses le cumul credite (ses points + ceux de tous
        // les joueurs en dessous dans ce classement) - meme regle que le Bingo, recompense la meilleure course.
        broadcast(t("race.results-points", "<gold><bold>Classement aux points"));
        int line = 1;
        for (Map.Entry<UUID, Double> entry : cumulative.entrySet()) {
            Racer racer = racers.get(entry.getKey());
            broadcast(t("race.points-line", "<gray><rank>. <white><name></white> <dark_gray>- <gold><points> pts <gray>(cumul <white><total></white>)",
                    "rank", line++, "name", nameOf(entry.getKey()), "points", fmt(racer.points), "total", fmt(entry.getValue())));
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && entry.getValue() > 0 && ranked(player) && !plugin.scores().excluded(player)) {
                plugin.ranking().stats().addPoints(minigame().id(), player.getUniqueId(), player.getName(), entry.getValue());
                plugin.ranking().boards().refreshSoon();
                player.sendMessage(plugin.prefix().append(t("race.points-credited", "<gold>+<points> point(s) au classement",
                        "points", fmt(entry.getValue()))));
            }
        }
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            Racer racer = racers.get(uuid);
            if (player != null && racer != null && !racer.finished) {
                becomeMatchSpectator(player);
            }
        }
    }

    /**
     * 1.3.0 : classement aux points (points de la course decroissants) avec, pour chacun, le cumul credite : ses points
     * + ceux de tous les joueurs en dessous.
     */
    private Map<UUID, Double> cumulativePoints() {
        List<Map.Entry<UUID, Racer>> ordered = new ArrayList<>(racers.entrySet());
        ordered.sort(Comparator.comparingDouble((Map.Entry<UUID, Racer> e) -> -e.getValue().points));
        Map<UUID, Double> result = new LinkedHashMap<>();
        double below = 0;
        for (int i = ordered.size() - 1; i >= 0; i--) {
            below += ordered.get(i).getValue().points;
            result.put(ordered.get(i).getKey(), Math.round(below * 100) / 100.0);
        }
        Map<UUID, Double> inOrder = new LinkedHashMap<>();
        for (Map.Entry<UUID, Racer> entry : ordered) {
            inOrder.put(entry.getKey(), result.get(entry.getKey()));
        }
        return inOrder;
    }

    /** 1.2.0 : ligne « race » du journal de KG_ScoreBoards : resultat de la course (arrives puis non arrives). */
    private void logRace(List<Map.Entry<UUID, Racer>> unfinished, Map<UUID, Double> cumulative) {
        List<Map<String, Object>> results = new ArrayList<>();
        int rank = 1;
        for (UUID uuid : finishOrder) {
            results.add(result(uuid, racers.get(uuid), rank++, true, cumulative));
        }
        for (Map.Entry<UUID, Racer> entry : unfinished) {
            results.add(result(entry.getKey(), entry.getValue(), rank++, false, cumulative));
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId);
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("laps", laps);
        fields.put("checkpoints", arena().list("checkpoints").size());
        fields.put("results", results);
        plugin.ranking().log(minigame().id(), "race", fields);
    }

    private Map<String, Object> result(UUID uuid, Racer racer, int rank, boolean finished, Map<UUID, Double> cumulative) {
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("player", uuid.toString());
        one.put("name", racer == null ? nameOf(uuid) : racer.name);
        one.put("platform", platform(uuid));
        one.put("rank", rank);
        one.put("finished", finished);
        one.put("totalMillis", finished && racer != null ? racer.finishMillis : null);
        one.put("lapsDone", racer == null ? 0 : racer.lap);
        one.put("points", racer == null ? 0 : racer.points);
        one.put("cumulative", cumulative.getOrDefault(uuid, 0.0));
        one.put("offTracks", racer == null ? 0 : racer.offTracks);
        return one;
    }

    // ------------------------------------------------------------------ 1.2.0 : classement en direct

    /** Ordre en direct : arrives (ordre d'arrivee), puis tours, checkpoints et distance au prochain point. */
    private List<UUID> liveOrder() {
        List<UUID> order = new ArrayList<>(finishOrder);
        List<Map.Entry<UUID, Racer>> running = new ArrayList<>();
        for (Map.Entry<UUID, Racer> entry : racers.entrySet()) {
            if (!entry.getValue().finished) {
                running.add(entry);
            }
        }
        running.sort(Comparator
                .comparingInt((Map.Entry<UUID, Racer> e) -> -e.getValue().lap)
                .thenComparingInt(e -> -e.getValue().next)
                .thenComparingDouble(e -> {
                    Player player = Bukkit.getPlayer(e.getKey());
                    Location target = target(e.getValue());
                    return player == null || target == null || player.getWorld() != world
                            ? Double.MAX_VALUE : player.getLocation().distance(target);
                }));
        for (Map.Entry<UUID, Racer> entry : running) {
            order.add(entry.getKey());
        }
        return order;
    }

    /** Ecart d'un coureur avec le premier, au dernier point de passage commun (ou tours de retard). */
    private Component leaderGap(Racer racer, Racer leader) {
        if (racer == leader) {
            return Component.empty();
        }
        if (racer.finished) {
            return Component.text(" " + formatTime(racer.finishMillis), NamedTextColor.GRAY);
        }
        int cps = arena().list("checkpoints").size() + 1;
        int behind = (leader.lap * cps + leader.next - (racer.lap * cps + racer.next)) / cps;
        if (behind >= 1) {
            return Component.text(" +" + behind + " t", NamedTextColor.RED);
        }
        long last = racer.next > 0 ? key(racer.lap, racer.next - 1)
                : racer.lap > 0 ? key(racer.lap - 1, cps - 1) : -1;
        Long mine = last < 0 ? null : racer.passAt.get(last);
        Long his = last < 0 ? null : leader.passAt.get(last);
        return mine == null || his == null ? Component.empty() : Component.text(" +" + seconds(mine - his), NamedTextColor.RED);
    }

    /**
     * Met a jour le tableau lateral de la course (chaque seconde). 1.4.0 : un tableau PAR JOUEUR : memes lignes pour
     * tous (positions, meilleur tour de la course) + son record personnel (objectif a battre).
     */
    private void updateBoard() {
        if (!minigame().getBool("live-ranking", true) || racers.isEmpty()) {
            return;
        }
        int leaderLap = 0;
        List<UUID> order = liveOrder();
        Racer leader = order.isEmpty() ? null : racers.get(order.get(0));
        if (leader != null) {
            leaderLap = Math.min(laps, leader.lap + (leader.finished ? 0 : 1));
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        int position = 1;
        for (UUID uuid : order) {
            if (position > 10) {
                break;
            }
            Racer racer = racers.get(uuid);
            if (racer == null) {
                continue;
            }
            lines.add(Component.text(position + ". ", NamedTextColor.GOLD)
                    .append(Component.text(racer.name, racer.finished ? NamedTextColor.GREEN : NamedTextColor.WHITE))
                    .append(leaderGap(racer, leader)));
            position++;
        }
        if (bestLapName != null) {
            lines.add(Component.empty());
            lines.add(t("race.board-best", "<gray>Meilleur tour :"));
            lines.add(Component.text(" " + bestLapName + " ", NamedTextColor.WHITE)
                    .append(Component.text(formatTime(bestLapMillis), NamedTextColor.AQUA)));
        }
        Component title = t("race.board-title", "<gold><bold>Course <gray>- tour <white><lap>/<laps>", "lap", leaderLap, "laps", laps);
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || player.getWorld() != world) {
                continue;
            }
            List<Component> mine = new ArrayList<>(lines);
            fr.kalium.scoreboards.data.StatsService.Row row = plugin.ranking().stats().playerRow(minigame().id(), uuid);
            long pb = row == null ? -1 : row.bestLapMs();
            mine.add(Component.empty());
            mine.add(t("race.board-pb", "<gray>Ton record :"));
            mine.add(pb < 0
                    ? t("race.board-pb-none", " <dark_gray>aucun (tour de moins de <max> s)", "max", minigame().getInt("record-max-lap-seconds", 45))
                    : Component.text(" " + formatTime(pb), NamedTextColor.LIGHT_PURPLE));
            org.bukkit.scoreboard.Scoreboard board = boards.computeIfAbsent(uuid, u -> Bukkit.getScoreboardManager().getNewScoreboard());
            org.bukkit.scoreboard.Objective old = board.getObjective("kgboatrace");
            if (old != null) {
                old.unregister();
            }
            org.bukkit.scoreboard.Objective objective = board.registerNewObjective("kgboatrace", org.bukkit.scoreboard.Criteria.DUMMY, title);
            objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
            objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
            int score = 15;
            int index = 0;
            for (Component line : mine) {
                if (score <= 0) {
                    break;
                }
                org.bukkit.scoreboard.Score entry = objective.getScore("l" + index++);
                entry.customName(line);
                entry.setScore(score--);
            }
            if (player.getScoreboard() != board) {
                previousBoards.putIfAbsent(uuid, player.getScoreboard());
                player.setScoreboard(board);
            }
        }
    }

    /** Rend a un joueur le tableau qu'il avait avant la course. */
    private void restoreBoard(UUID uuid) {
        org.bukkit.scoreboard.Scoreboard previous = previousBoards.remove(uuid);
        org.bukkit.scoreboard.Scoreboard board = boards.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && board != null && player.getScoreboard() == board) {
            player.setScoreboard(previous != null ? previous : Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void restoreAllBoards() {
        for (UUID uuid : new ArrayList<>(boards.keySet())) {
            restoreBoard(uuid);
        }
        previousBoards.clear();
        boards.clear();
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
        restoreBoard(uuid);
        if (shield != null) {
            shield.release(uuid);
        }
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

    /** Fermeture de la partie (joueurs renvoyes au hub sans passer par onMemberLeft) : tableaux rendus. */
    @Override
    protected void onClose() {
        restoreAllBoards();
        if (shield != null) {
            shield.stop();
            shield = null;
        }
    }

    private void abortCountdown() {
        if (phase == Phase.COUNTDOWN) {
            endMatch();
        }
    }

    @Override
    protected void onMatchReset() {
        restoreAllBoards();
        if (shield != null) {
            shield.stop();
            shield = null;
        }
        racers.clear();
        finishOrder.clear();
        passages.clear();
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
