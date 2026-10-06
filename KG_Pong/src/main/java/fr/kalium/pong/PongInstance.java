package fr.kalium.pong;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.util.Items;
import fr.kalium.games.world.Template;
import fr.kalium.scoreboards.data.StatsService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pong (demande de LeKiwi06, 06/10/2026). Deux joueurs flottent au-dessus du terrain et le regardent de dessus ; chacun
 * deplace une raquette faite de vrais blocs (clic gauche : changer de sens ; clic droit : avancer, maintenu : en
 * continu). La balle est un Sulfur Cube qui a avale un bloc de glace.
 * <ul>
 *   <li>Le plugin calcule lui-meme la trajectoire (rebonds sur les murs, renvoi selon le point d'impact sur la raquette
 *       et son mouvement, acceleration a chaque renvoi) et deplace le cube a chaque tick : le cube n'a ni intelligence
 *       ni physique propre, c'est le plugin qui decide ou il est.</li>
 *   <li>Le terrain est repere par deux coins ; sa longueur est le plus grand cote. « l » va du fond du joueur 1 au fond
 *       du joueur 2, « w » le long des raquettes.</li>
 *   <li>Deroulement : compte a rebours (COUNTDOWN, les raquettes bougent deja), echanges (RUNNING), fin (ENDING).</li>
 * </ul>
 * Les points sont credites a la fin de la partie (regle commune : ses points + la moyenne de ceux des joueurs classes en
 * dessous).
 */
public final class PongInstance extends GameInstance {

    /** Demi-largeur de la balle pour les calculs (le Sulfur Cube fait 0,98 bloc de large). */
    private static final double R = 0.5;
    /** Renvoi : angle au bord de la raquette, supplement d'une raquette en mouvement, angle maximal. */
    private static final double EDGE_ANGLE = Math.toRadians(55);
    private static final double MOVE_ANGLE = Math.toRadians(15);
    private static final double LIMIT_ANGLE = Math.toRadians(65);
    private static final double SERVE_ANGLE = Math.toRadians(25);
    /** La balle avance par pas de cette longueur au plus (aucun rebond manque, meme a grande vitesse). */
    private static final double SUBSTEP = 0.2;
    /** Compte a rebours du debut de partie, en secondes. */
    private static final int COUNTDOWN_SECONDS = 3;
    /** Clic droit maintenu : le jeu renvoie le clic toutes les 4 ticks ; des clics a 6 ticks d'ecart au plus se suivent. */
    private static final int HOLD_GAP = 6;
    /** Nombre de clics qui se suivent a partir duquel le bouton est considere comme maintenu (avant : une case par clic). */
    private static final int HOLD_STREAK = 3;
    /** La raquette continue pendant ce nombre de ticks apres le dernier clic recu. */
    private static final int HOLD_KEEP = 5;
    /** Un clic gauche arrive par deux evenements (bras + interaction) : un seul compte. */
    private static final int TOGGLE_GAP = 2;
    /** Une raquette est « en mouvement » si elle a bouge dans ces derniers ticks. */
    private static final int MOVE_MEMORY = 4;

    /** Un joueur et sa raquette. */
    private static final class Side {
        final int index;
        final UUID uuid;
        final String name;
        /** Case de la raquette le long du terrain, plan de sa face avant et sens vers le centre (+1 ou -1). */
        int cell;
        double face;
        int toward;
        /** Premiere case occupee par la raquette, le long de « w ». */
        int pos;
        /** Sens choisi (+1 ou -1 le long de « w ») et sens qui correspond au haut de l'ecran du joueur. */
        int dir;
        int up;
        long lastUse = -1000;
        int streak;
        long holdUntil = -1000;
        long lastToggle = -1000;
        long lastMove = -1000;
        int lastMoveDir;
        double carry;
        int score;
        BlockData block;
        BlockData[] originals;
        Location view;

        Side(int index, UUID uuid, String name) {
            this.index = index;
            this.uuid = uuid;
            this.name = name;
        }
    }

    private final KGPong pong;

    // Terrain (calcule une fois, a partir des points de l'arene).
    private String geometryError;
    private boolean axisX;
    private boolean flip;
    private int lMin;
    private int lMax;
    private int wMin;
    private int lenL;
    private int lenW;
    private int floorY;
    private int barLen;
    private int barOffset;

    private final Side[] sides = new Side[2];
    private final java.util.Set<UUID> greeted = new java.util.HashSet<>();
    private BukkitTask task;
    private long tick;
    private SulfurCube ball;
    private double bl;
    private double bw;
    private double vl;
    private double vw;
    private double speed;
    private boolean moving;
    private long serveAt;
    private int serveToward;
    private int rallies;
    private double sentL = Double.NaN;
    private double sentW = Double.NaN;
    private Side winner;
    private boolean forfeit;

    public PongInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                        boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        this.pong = KGPong.get();
        computeGeometry();
    }

    private int setting(String key, int fallback) {
        return minigame().getInt(key, fallback);
    }

    private boolean playing() {
        return phase == Phase.COUNTDOWN || phase == Phase.RUNNING;
    }

    private Side side(UUID uuid) {
        for (Side side : sides) {
            if (side != null && side.uuid.equals(uuid)) {
                return side;
            }
        }
        return null;
    }

    private Side other(Side side) {
        return sides[1 - side.index];
    }

    /** Le joueur tient-il une raquette dans la partie en cours ? */
    public boolean controls(UUID uuid) {
        return playing() && side(uuid) != null;
    }

    // ------------------------------------------------------------------ terrain

    private void computeGeometry() {
        Pos c1 = arena().point("corner-1");
        Pos c2 = arena().point("corner-2");
        if (c1 == null || c2 == null || arena().point("view-1") == null || arena().point("view-2") == null) {
            geometryError = "points";
            return;
        }
        Location a = loc(c1);
        Location b = loc(c2);
        int sizeX = Math.abs(a.getBlockX() - b.getBlockX()) + 1;
        int sizeZ = Math.abs(a.getBlockZ() - b.getBlockZ()) + 1;
        axisX = sizeX >= sizeZ;
        int la = axisX ? a.getBlockX() : a.getBlockZ();
        int lb = axisX ? b.getBlockX() : b.getBlockZ();
        int wa = axisX ? a.getBlockZ() : a.getBlockX();
        int wb = axisX ? b.getBlockZ() : b.getBlockX();
        lMin = Math.min(la, lb);
        lMax = Math.max(la, lb);
        wMin = Math.min(wa, wb);
        lenL = lMax - lMin + 1;
        lenW = Math.abs(wa - wb) + 1;
        flip = la > lb;
        floorY = a.getBlockY();
        if (lenL < 9 || lenW < 3) {
            geometryError = "size";
            return;
        }
        barLen = Math.max(1, Math.min(lenW - 2, setting("bar-length", 5)));
        barOffset = Math.max(1, Math.min((lenL - 5) / 2, setting("bar-offset", 2)));
    }

    private Component geometryMessage() {
        return "size".equals(geometryError)
                ? t("pong.arena-small", "<red>Le terrain de cette arène est trop petit (9 x 3 cases au moins) : prévenez un modérateur.")
                : t("pong.arena-points", "<red>Il manque des points à cette arène : prévenez un modérateur.");
    }

    private Block cellBlock(int l, int w) {
        int lw = flip ? lMax - l : lMin + l;
        int ww = wMin + w;
        return world.getBlockAt(axisX ? lw : ww, floorY, axisX ? ww : lw);
    }

    private Location ballLocation() {
        double lw = flip ? (lMax + 1) - bl : lMin + bl;
        double ww = wMin + bw;
        return new Location(world, axisX ? lw : ww, floorY, axisX ? ww : lw);
    }

    /** Vue du joueur : le point pose par le moderateur, regard a la verticale vers le bas. */
    private Location view(int index) {
        Location location = loc(arena().point(index == 0 ? "view-1" : "view-2"));
        location.setPitch(90f);
        return location;
    }

    /** Sens (le long de « w ») qui correspond au haut de l'ecran d'un joueur qui regarde vers le bas depuis ce point. */
    private int upSign(Location view) {
        double yaw = Math.toRadians(view.getYaw());
        double along = axisX ? Math.cos(yaw) : -Math.sin(yaw);
        return along < -0.3 ? -1 : 1;
    }

    // ------------------------------------------------------------------ admission

    @Override
    protected Component canAdmit(Player player) {
        if (!isPublic() && members.size() >= 2) {
            return t("join.full", "<red>La partie est complète.");
        }
        return null;
    }

    @Override
    protected void onArrived(Player player) {
        if ((phase == Phase.WAITING || phase == Phase.STARTING) && greeted.add(player.getUniqueId())) {
            player.sendMessage(plugin.prefix().append(t("pong.welcome",
                    "<gray>Pong : clic gauche pour changer le sens de votre raquette, clic droit pour la déplacer (maintenez pour continuer). Premier à <white><n></white> points.",
                    "n", target())));
        }
    }

    // ------------------------------------------------------------------ demarrage

    @Override
    protected int minParticipants() {
        return 2;
    }

    @Override
    protected int maxParticipants() {
        return 2;
    }

    @Override
    protected int gatherSeconds() {
        return setting("public-gather-seconds", 10);
    }

    @Override
    protected List<UUID> pickParticipants(List<UUID> queue) {
        return new ArrayList<>(queue.subList(0, Math.min(queue.size(), 2)));
    }

    @Override
    protected Component validatePrivateStart() {
        if (geometryError != null) {
            return geometryMessage();
        }
        if (members.size() != 2) {
            return t("pong.need-players", "<red>Il faut 2 joueurs.");
        }
        return null;
    }

    private int target() {
        return Math.max(1, setting("points-to-win", 5));
    }

    private void resetState() {
        stopTask();
        removeBall();
        sides[0] = null;
        sides[1] = null;
        tick = 0;
        moving = false;
        serveAt = 0;
        rallies = 0;
        winner = null;
        forfeit = false;
    }

    @Override
    protected void beginMatch() {
        resetState();
        List<Player> players = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        if (geometryError != null || players.size() != 2) {
            broadcast(geometryError != null ? geometryMessage()
                    : t("pong.aborted", "<yellow>Partie annulée : il faut 2 joueurs."));
            endMatch();
            return;
        }
        Collections.shuffle(players);
        phase = Phase.COUNTDOWN;
        secondsLeft = COUNTDOWN_SECONDS;
        for (int i = 0; i < 2; i++) {
            Player player = players.get(i);
            Side side = new Side(i, player.getUniqueId(), player.getName());
            side.cell = i == 0 ? barOffset : lenL - 1 - barOffset;
            side.face = i == 0 ? barOffset + 1 : lenL - 1 - barOffset;
            side.toward = i == 0 ? 1 : -1;
            side.pos = (lenW - barLen) / 2;
            side.view = view(i);
            side.up = upSign(side.view);
            side.dir = side.up;
            Material fallback = i == 0 ? Material.BLUE_CONCRETE : Material.RED_CONCRETE;
            Material chosen = Items.material(minigame().getText(i == 0 ? "bar-block-1" : "bar-block-2", ""), fallback);
            side.block = (chosen.isBlock() ? chosen : fallback).createBlockData();
            sides[i] = side;
            placeBar(side);
            setupPlayer(player, side);
        }
        resetBall();
        serveToward = ThreadLocalRandom.current().nextInt(2);
        spawnBall();
        task = Bukkit.getScheduler().runTaskTimer(pong, this::fastTick, 1L, 1L);
        title(participants, t("pong.start-title", "<gold><bold>Pong"),
                t("pong.start-sub", "<gray>Premier à <white><n></white> points", "n", target()), 5, 40, 10);
        broadcast(t("pong.start", "<gold>Pong : <blue><p1></blue> <gray>contre</gray> <red><p2></red>.",
                "p1", sides[0].name, "p2", sides[1].name));
    }

    /** Etat d'un joueur : mode aventure, en vol au-dessus du terrain, regard vers le bas, objet de sens en main. */
    private void setupPlayer(Player player, Side side) {
        plugin.hub().resetPlayer(player, GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.teleport(side.view);
        player.setFlying(true);
        giveItems(player, side);
        player.getInventory().setHeldItemSlot(0);
    }

    /** Toute la barre d'objets porte le meme objet : un clic droit compte quelle que soit la case choisie. */
    private void giveItems(Player player, Side side) {
        PlayerInventory inventory = player.getInventory();
        ItemStack item = pong.paddle(side.dir == side.up);
        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, item);
        }
    }

    // ------------------------------------------------------------------ raquettes

    private void placeBar(Side side) {
        side.originals = new BlockData[lenW];
        for (int w = 0; w < lenW; w++) {
            Block block = cellBlock(side.cell, w);
            side.originals[w] = block.getBlockData();
            trackOriginal(block.getX(), block.getY(), block.getZ(), side.originals[w]);
        }
        for (int w = side.pos; w < side.pos + barLen; w++) {
            cellBlock(side.cell, w).setBlockData(side.block, false);
        }
    }

    private void removeBar(Side side) {
        if (side == null || side.originals == null) {
            return;
        }
        for (int w = side.pos; w < side.pos + barLen; w++) {
            cellBlock(side.cell, w).setBlockData(side.originals[w], false);
        }
        side.originals = null;
    }

    /** Avance la raquette d'une case dans le sens choisi ; false si elle bute contre le mur. */
    private boolean step(Side side) {
        int next = side.pos + side.dir;
        if (next < 0 || next > lenW - barLen || side.originals == null) {
            return false;
        }
        int leave = side.dir > 0 ? side.pos : side.pos + barLen - 1;
        int enter = side.dir > 0 ? side.pos + barLen : side.pos - 1;
        cellBlock(side.cell, leave).setBlockData(side.originals[leave], false);
        cellBlock(side.cell, enter).setBlockData(side.block, false);
        side.pos = next;
        side.lastMove = tick;
        side.lastMoveDir = side.dir;
        return true;
    }

    /** Clic gauche : inverse le sens de la raquette. */
    public void leftClick(Player player) {
        Side side = side(player.getUniqueId());
        if (side == null || !playing() || tick - side.lastToggle < TOGGLE_GAP) {
            return;
        }
        side.lastToggle = tick;
        side.dir = -side.dir;
        giveItems(player, side);
        player.sendActionBar(status(side));
    }

    /**
     * Clic droit : chaque clic avance la raquette d'une case ; a partir du 3e clic rapproche (bouton maintenu, le jeu
     * repete le clic), elle avance en continu a sa vitesse reglee.
     */
    public void rightClick(Player player) {
        Side side = side(player.getUniqueId());
        if (side == null || !playing() || tick == side.lastUse) {
            return;
        }
        side.streak = tick - side.lastUse <= HOLD_GAP ? side.streak + 1 : 1;
        side.lastUse = tick;
        if (side.streak < HOLD_STREAK) {
            side.carry = 0;
            step(side);
            return;
        }
        if (tick > side.holdUntil) {
            side.carry = 1.0;
        }
        side.holdUntil = tick + HOLD_KEEP;
    }

    private void moveBar(Side side, double perTick) {
        if (tick > side.holdUntil) {
            return;
        }
        side.carry += perTick;
        while (side.carry >= 1.0) {
            side.carry -= 1.0;
            if (!step(side)) {
                side.carry = 0;
                break;
            }
        }
    }

    // ------------------------------------------------------------------ balle

    private void resetBall() {
        bl = lenL / 2.0;
        bw = lenW / 2.0;
        vl = 0;
        vw = 0;
        speed = setting("ball-speed", 8) / 20.0;
        moving = false;
    }

    private void spawnBall() {
        ItemStack swallowed = new ItemStack(Items.material(minigame().getText("ball-block", ""), Material.PACKED_ICE));
        ball = world.spawn(ballLocation(), SulfurCube.class, cube -> {
            cube.setAI(false);
            cube.setGravity(false);
            cube.setInvulnerable(true);
            cube.setCollidable(false);
            cube.setPersistent(false);
            cube.setRemoveWhenFarAway(false);
            cube.setWander(false);
            cube.setAdult();
            pong.markBall(cube);
            try {
                if (cube.getEquipment() != null) {
                    cube.getEquipment().setItem(EquipmentSlot.BODY, swallowed);
                }
            } catch (RuntimeException e) {
                pong.getLogger().warning("Bloc de la balle non posé : " + e.getMessage());
            }
        });
        sentL = bl;
        sentW = bw;
    }

    private void removeBall() {
        if (ball != null) {
            ball.remove();
            ball = null;
        }
    }

    /** Service : la balle part du centre vers le joueur qui vient d'encaisser (au hasard pour le premier service). */
    private void serve() {
        double angle = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * SERVE_ANGLE;
        Side to = sides[serveToward];
        vl = -to.toward * speed * Math.cos(angle);
        vw = speed * Math.sin(angle);
        moving = true;
        serveAt = 0;
    }

    private void fastTick() {
        if (closing() || !playing() || sides[0] == null || sides[1] == null) {
            return;
        }
        tick++;
        double barPerTick = Math.max(2, setting("bar-speed", 10)) / 20.0;
        moveBar(sides[0], barPerTick);
        moveBar(sides[1], barPerTick);
        if (phase == Phase.RUNNING) {
            if (!moving && serveAt > 0 && tick >= serveAt) {
                serve();
            }
            if (moving) {
                simulate(barPerTick);
            }
        }
        if (playing()) {
            driveBall();
        }
    }

    /** Avance la balle d'un tick, par petits pas : murs, raquettes, buts. */
    private void simulate(double barPerTick) {
        int steps = Math.max(1, (int) Math.ceil(speed / SUBSTEP));
        double part = 1.0 / steps;
        for (int i = 0; i < steps; i++) {
            double stepL = vl * part;
            bl += stepL;
            bw += vw * part;
            if (bw < R) {
                bw = 2 * R - bw;
                vw = Math.abs(vw);
            } else if (bw > lenW - R) {
                bw = 2 * (lenW - R) - bw;
                vw = -Math.abs(vw);
            }
            for (Side side : sides) {
                // depth : distance du centre de la balle devant la face de la raquette (negatif : derriere la face).
                double depth = side.toward * (bl - side.face);
                if (depth <= -1 - R) {
                    goal(side);
                    return;
                }
                boolean across = bw > side.pos - R && bw < side.pos + barLen + R;
                if (!across || depth >= R) {
                    continue;
                }
                double approach = -side.toward * stepL;
                if (approach > 0 && depth + approach >= R - 1.0E-9) {
                    bounce(side);
                } else {
                    slideOff(side, barPerTick);
                }
            }
        }
    }

    /** Renvoi par la face avant : l'angle depend du point d'impact et du mouvement de la raquette ; la balle accelere. */
    private void bounce(Side side) {
        bl = side.face + side.toward * R;
        double middle = side.pos + barLen / 2.0;
        double angle = Math.max(-1, Math.min(1, (bw - middle) / (barLen / 2.0 + R))) * EDGE_ANGLE;
        if (tick - side.lastMove <= MOVE_MEMORY) {
            angle += side.lastMoveDir * MOVE_ANGLE;
        }
        angle = Math.max(-LIMIT_ANGLE, Math.min(LIMIT_ANGLE, angle));
        double max = Math.max(setting("ball-speed", 8), setting("ball-speed-max", 18)) / 20.0;
        speed = Math.min(max, speed + setting("ball-speed-gain", 5) / 200.0);
        vl = side.toward * speed * Math.cos(angle);
        vw = speed * Math.sin(angle);
        rallies++;
    }

    /**
     * La balle touche le bout de la raquette (elle est deja passee devant la face, ou la raquette vient de glisser sur
     * elle) : elle est poussee de cote, le point n'est pas sauve. Coincee contre un mur, elle passe derriere.
     */
    private void slideOff(Side side, double barPerTick) {
        boolean low = bw < side.pos + barLen / 2.0;
        double out = low ? side.pos - R : side.pos + barLen + R;
        if (out < R || out > lenW - R) {
            bl = side.face - side.toward * (1 + R);
            return;
        }
        bw = out;
        double push = Math.max(Math.abs(vw), barPerTick * 1.2);
        vw = low ? -push : push;
    }

    private void goal(Side conceded) {
        Side scorer = other(conceded);
        scorer.score++;
        resetBall();
        Component score = t("pong.score", "<blue><p1></blue> <white><s1> - <s2></white> <red><p2></red>",
                "p1", sides[0].name, "s1", sides[0].score, "p2", sides[1].name, "s2", sides[1].score);
        if (scorer.score >= target()) {
            finish(scorer, false);
            return;
        }
        title(members, t("pong.goal", "<gold><name> marque !", "name", scorer.name), score, 3, 25, 8);
        serveToward = conceded.index;
        serveAt = tick + Math.max(1, setting("serve-delay-seconds", 2) * 20L);
    }

    /** Place le cube la ou le calcul dit que la balle se trouve (recree s'il a disparu). */
    private void driveBall() {
        if (ball == null || !ball.isValid()) {
            removeBall();
            spawnBall();
            return;
        }
        if (bl == sentL && bw == sentW) {
            return;
        }
        ball.teleport(ballLocation());
        sentL = bl;
        sentW = bw;
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    // ------------------------------------------------------------------ boucle lente et fin

    @Override
    protected void matchSecond() {
        switch (phase) {
            case COUNTDOWN -> {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    phase = Phase.RUNNING;
                    serveAt = tick + Math.max(1, setting("serve-delay-seconds", 2) * 20L);
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

    private void finish(Side won, boolean byForfeit) {
        if (!playing()) {
            return;
        }
        winner = won;
        forfeit = byForfeit;
        phase = Phase.ENDING;
        secondsLeft = Math.max(1, setting("end-delay-seconds", 6));
        moving = false;
        stopTask();
        Component headline = t("pong.win", "<green><bold><name> gagne !", "name", won.name);
        Component detail = t("pong.score", "<blue><p1></blue> <white><s1> - <s2></white> <red><p2></red>",
                "p1", sides[0].name, "s1", sides[0].score, "p2", sides[1].name, "s2", sides[1].score);
        title(members, headline, detail, 5, 70, 15);
        broadcast(byForfeit ? t("pong.win-forfeit", "<green><name> gagne par abandon.", "name", won.name) : headline);
        results();
    }

    /**
     * Classement de fin de partie et credit des points : regle commune (KG_BoatRace 1.5.0) : chacun recoit ses points +
     * la moyenne de ceux des joueurs classes en dessous. Le gagnant est classe premier. Points : un par but marque, plus
     * le bonus du vainqueur (pas en cas d'abandon adverse).
     */
    private void results() {
        Side lost = other(winner);
        double winPoints = winner.score * setting("points-goal", 1) + (forfeit ? 0 : setting("points-win", 3));
        double lostPoints = lost.score * setting("points-goal", 1);
        Map<Side, Double> own = new LinkedHashMap<>();
        own.put(winner, winPoints);
        own.put(lost, lostPoints);
        Map<Side, Double> credited = new HashMap<>();
        credited.put(winner, winPoints + lostPoints);
        credited.put(lost, lostPoints);
        broadcast(t("pong.results", "<gold><bold>Classement aux points"));
        List<Map<String, Object>> logged = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Side, Double> entry : own.entrySet()) {
            Side side = entry.getKey();
            double total = credited.get(side);
            broadcast(t("pong.results-line", "<gray><rank>. <white><name></white> <dark_gray>- <gold><points> pts <gray>(crédité <white><total></white>)",
                    "rank", rank, "name", side.name, "points", StatsService.formatPoints(entry.getValue()),
                    "total", StatsService.formatPoints(total)));
            boolean counted = credit(side.uuid, side.name, total);
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("player", side.uuid.toString());
            one.put("name", side.name);
            one.put("rank", rank);
            one.put("goals", side.score);
            one.put("points", entry.getValue());
            one.put("credited", total);
            one.put("counted", counted);
            logged.add(one);
            rank++;
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("winner", winner.uuid.toString());
        fields.put("forfeit", forfeit);
        fields.put("rallies", rallies);
        fields.put("ticks", tick);
        fields.put("results", logged);
        plugin.ranking().log(minigame().id(), "match", fields);
    }

    /**
     * Credite les points au classement (KG_ScoreBoards, points decimaux) et note l'attribution dans le journal des
     * parties (evenement « points », comme KalGames : /classements verifier retrouve les points non comptes).
     */
    private boolean credit(UUID uuid, String name, double value) {
        if (value <= 0) {
            return false;
        }
        Player player = Bukkit.getPlayer(uuid);
        boolean operator = player != null ? plugin.scores().excluded(player)
                : Bukkit.getOfflinePlayer(uuid).isOp() && plugin.getConfig().getBoolean("stats.exclude-operators", true);
        String reason = operator ? "operateur" : !ranked() ? "partie-non-classee" : null;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("player", uuid.toString());
        fields.put("name", name);
        fields.put("platform", uuid.getMostSignificantBits() == 0 ? "bedrock" : "java");
        fields.put("points", value);
        fields.put("counted", reason == null);
        fields.put("reason", reason);
        plugin.ranking().log(minigame().id(), "points", fields);
        if (reason != null) {
            return false;
        }
        plugin.ranking().stats().addPoints(minigame().id(), uuid, name, value);
        plugin.ranking().boards().refreshSoon();
        if (player != null) {
            player.sendMessage(plugin.prefix().append(t("pong.credited", "<gold>+<points> point(s) au classement",
                    "points", StatsService.formatPoints(value))));
        }
        return true;
    }

    // ------------------------------------------------------------------ depart d'un joueur

    /** Un joueur qui quitte ou se deconnecte en cours de partie abandonne : l'autre gagne. */
    @Override
    protected void onMemberLeft(UUID uuid, boolean wasParticipant, boolean disconnected) {
        greeted.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        Side side = side(uuid);
        if (player != null && side != null) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        if (side != null && playing()) {
            finish(other(side), true);
        }
    }

    @Override
    protected void onMatchReset() {
        removeBar(sides[0]);
        removeBar(sides[1]);
        resetState();
    }

    @Override
    protected void onClose() {
        removeBar(sides[0]);
        removeBar(sides[1]);
        for (Side side : sides) {
            Player player = side == null ? null : Bukkit.getPlayer(side.uuid);
            if (player != null && members.contains(side.uuid)) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }
        resetState();
    }

    // ------------------------------------------------------------------ regles

    @Override
    public void handleDeath(Player victim, Player killer) {
        // Personne ne prend de degats dans ce jeu.
    }

    @Override
    public void handleVoid(Player player) {
        Side side = side(player.getUniqueId());
        if (side != null && matchInProgress()) {
            player.teleport(side.view);
            player.setFlying(true);
            return;
        }
        arriveInStands(player);
    }

    /** Un joueur en partie reste a son point de vue (il peut tourner la tete). */
    @Override
    public boolean frozen(Player player) {
        return matchInProgress() && side(player.getUniqueId()) != null;
    }

    @Override
    public boolean canBuild(Player player) {
        return false;
    }

    @Override
    public boolean canInteract(Player player) {
        return false;
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
    public boolean inventoryLocked(Player player) {
        return true;
    }

    // ------------------------------------------------------------------ affichage

    @Override
    public List<Component> menuInfo(Player player) {
        return List.of(t("pong.info-target", "<gray>Premier à <white><n></white> points", "n", target()));
    }

    private Component status(Side side) {
        return t("pong.status", "<blue><p1></blue> <white><s1> - <s2></white> <red><p2></red> <dark_gray>| <gray>Sens : <white><dir>",
                "p1", sides[0].name, "s1", sides[0].score, "p2", sides[1].name, "s2", sides[1].score,
                "dir", side.dir == side.up ? "monter" : "descendre");
    }

    @Override
    protected Component statusFor(Player player) {
        if (matchInProgress() && sides[0] != null && sides[1] != null) {
            Side side = side(player.getUniqueId());
            if (side != null && playing()) {
                return status(side);
            }
            return t("pong.score", "<blue><p1></blue> <white><s1> - <s2></white> <red><p2></red>",
                    "p1", sides[0].name, "s1", sides[0].score, "p2", sides[1].name, "s2", sides[1].score);
        }
        if (isPublic()) {
            return queueStatus(player);
        }
        if (phase == Phase.WAITING) {
            return t("pong.status-lobby", "<gold>Partie privée <white><code></white> <dark_gray>- <gray>en attente de l'hôte", "code", code);
        }
        return null;
    }
}
