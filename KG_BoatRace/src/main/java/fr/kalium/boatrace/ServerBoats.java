package fr.kalium.boatrace;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.6.0 (essai, reglage « bedrock-server-boat », desactive par defaut) : bateau calcule par le serveur pour les joueurs
 * Bedrock (cahier des charges, « Equite Java / Bedrock »).
 *
 * En bateau, c'est le jeu du joueur qui calcule le mouvement : un joueur Bedrock roule avec la physique de Bedrock, que
 * Geyser transmet telle quelle (journal du 24/09 au 08/10/2026 : tours 8 a 15 % plus rapides qu'en Java). Ici le joueur
 * Bedrock ne pilote plus un vrai bateau : il est assis sur un porte-armure invisible (qu'un joueur ne peut pas piloter),
 * et le serveur calcule a chaque tick, a partir de ses touches (avancer, reculer, gauche, droite), le meme mouvement que
 * le jeu Java (AbstractBoat : frottement du sol, rotation, poussee, gravite, collisions avec les blocs). Un vrai bateau
 * vide (la « coque ») suit cette position : c'est lui que l'on voit, et que regarde le hors-piste.
 *
 * Un joueur passe toujours en premiere place d'un vehicule (donc pilote) : impossible de le laisser dans un vrai bateau
 * sans que son jeu le pilote, d'ou le siege a part.
 *
 * Le bateau calcule ne heurte que les blocs, jamais une entite (les autres bateaux compris).
 *
 * Limites connues : la direction reagit avec le retard de la connexion du joueur ; pas d'animation des rames ; l'eau est
 * simplifiee (flottaison seulement) ; les blocs speciaux (sable des ames, miel, slime) n'ont pas leur effet.
 */
final class ServerBoats implements Runnable {

    /** Marques des entites creees ici (ServerBoatListener). */
    static final String HULL_TAG = "kg_boatrace_hull";
    static final String SEAT_TAG = "kg_boatrace_seat";

    /** Boite d'un bateau (1,375 x 0,5625) et gravite, comme dans le jeu Java. */
    private static final double HALF_WIDTH = 1.375 / 2;
    private static final double HEIGHT = 0.5625;
    private static final double GRAVITY = 0.04;
    private static final double EPSILON = 1.0E-7;

    private static final class Sim {
        final Player player;
        final ArmorStand seat;
        final Entity hull;
        double x;
        double y;
        double z;
        float yaw;
        double vx;
        double vy;
        double vz;
        /** Vitesse de rotation (degres par tick), freinee comme la vitesse. */
        float deltaRotation;

        Sim(Player player, ArmorStand seat, Entity hull, Location at) {
            this.player = player;
            this.seat = seat;
            this.hull = hull;
            this.x = at.getX();
            this.y = at.getY();
            this.z = at.getZ();
            this.yaw = at.getYaw();
        }
    }

    private final Plugin plugin;
    private final World world;
    /** Hauteur du siege au-dessus du fond du bateau (reglage « bedrock-seat-height », en centimetres). */
    private final double seatHeight;
    private final Map<UUID, Sim> sims = new HashMap<>();
    private BukkitTask task;

    ServerBoats(Plugin plugin, World world, double seatHeight) {
        this.plugin = plugin;
        this.world = world;
        this.seatHeight = seatHeight;
    }

    void start() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this, 1L, 1L);
        }
    }

    /** Fin de la course : plus aucun bateau calcule. */
    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Sim sim : sims.values()) {
            discard(sim);
        }
        sims.clear();
    }

    /** Installe le joueur dans un bateau calcule par le serveur, a l'arret, a cet endroit. */
    void mount(Player player, Location where, EntityType type) {
        release(player.getUniqueId());
        Location at = where.clone();
        at.setPitch(0);
        Entity hull;
        try {
            hull = world.spawnEntity(at, type);
        } catch (IllegalArgumentException e) {
            hull = world.spawnEntity(at, EntityType.OAK_BOAT);
        }
        hull.setPersistent(false);
        hull.setGravity(false); // immobile entre deux positions donnees par le calcul
        hull.setInvulnerable(true);
        hull.addScoreboardTag(HULL_TAG);
        ArmorStand seat = world.spawn(at.clone().add(0, seatHeight, 0), ArmorStand.class, stand -> {
            stand.setMarker(true); // aucune boite de collision
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.addScoreboardTag(SEAT_TAG);
        });
        seat.addPassenger(player);
        sims.put(player.getUniqueId(), new Sim(player, seat, hull, at));
    }

    /** Coque (vrai bateau vide) du joueur, ou null s'il n'a pas de bateau calcule. */
    Entity hull(UUID id) {
        Sim sim = sims.get(id);
        return sim == null ? null : sim.hull;
    }

    /** Le joueur a fini, abandonne ou repart d'un point de controle : son bateau calcule disparait. */
    void release(UUID id) {
        Sim sim = sims.remove(id);
        if (sim != null) {
            discard(sim);
        }
    }

    private static void discard(Sim sim) {
        sim.seat.remove();
        sim.hull.remove();
    }

    @Override
    public void run() {
        for (Iterator<Sim> iterator = sims.values().iterator(); iterator.hasNext(); ) {
            Sim sim = iterator.next();
            if (!sim.player.isOnline() || !sim.seat.isValid() || !sim.hull.isValid() || sim.player.getVehicle() != sim.seat) {
                discard(sim); // descendu (retour au point de controle, arrivee, depart) : la course le reinstalle au besoin
                iterator.remove();
                continue;
            }
            step(sim, sim.player.getCurrentInput());
            Location at = new Location(world, sim.x, sim.y, sim.z, sim.yaw, 0);
            sim.hull.teleport(at);
            sim.seat.teleport(at.clone().add(0, seatHeight, 0), TeleportFlag.EntityState.RETAIN_PASSENGERS);
            // Le serveur n'envoie la position d'une entite que tous les 3 ticks (a 140 km/h : 6 blocs d'un coup), sauf
            // si une de ses donnees vient de changer : une donnee sans effet visible est donc changee a chaque tick,
            // pour que le joueur voie son bateau bouger a chaque tick.
            sim.hull.setFreezeTicks(sim.hull.getFreezeTicks() == 0 ? 1 : 0);
            sim.seat.setFreezeTicks(sim.seat.getFreezeTicks() == 0 ? 1 : 0);
        }
    }

    // ------------------------------------------------------------------ physique (reprise du jeu Java, AbstractBoat)

    /** Un tick : frottement et gravite (floatBoat), touches du joueur (controlBoat), deplacement (move). */
    private void step(Sim sim, Input input) {
        BoundingBox box = box(sim.x, sim.y, sim.z);
        double water = waterLevel(box);
        double buoyancy = 0;
        float friction;
        if (!Double.isNaN(water)) {
            buoyancy = (water - sim.y) / HEIGHT;
            friction = 0.9F;
        } else {
            float ground = groundFriction(box);
            friction = ground > 0 ? ground : 0.9F; // en l'air : 0,9
        }
        sim.vx *= friction;
        sim.vz *= friction;
        sim.vy -= GRAVITY;
        sim.deltaRotation *= friction;
        if (buoyancy > 0) {
            sim.vy = (sim.vy + buoyancy * (GRAVITY / 0.65)) * 0.75;
        }

        boolean left = input.isLeft();
        boolean right = input.isRight();
        boolean forward = input.isForward();
        boolean backward = input.isBackward();
        if (left) {
            sim.deltaRotation--;
        }
        if (right) {
            sim.deltaRotation++;
        }
        float thrust = 0;
        if (right != left && !forward && !backward) {
            thrust += 0.005F;
        }
        sim.yaw = Location.normalizeYaw(sim.yaw + sim.deltaRotation);
        if (forward) {
            thrust += 0.04F;
        }
        if (backward) {
            thrust -= 0.005F;
        }
        double radians = Math.toRadians(sim.yaw);
        sim.vx += Math.sin(-radians) * thrust;
        sim.vz += Math.cos(radians) * thrust;

        move(sim, box);
    }

    /** Deplacement arrete par les blocs, axe par axe (Y, puis le plus grand de X et Z, comme le jeu). */
    private void move(Sim sim, BoundingBox box) {
        double dx = sim.vx;
        double dy = sim.vy;
        double dz = sim.vz;
        if (Double.isNaN(dx + dy + dz)) {
            sim.vx = sim.vy = sim.vz = 0;
            return;
        }
        List<BoundingBox> blocks = blockBoxes(box.clone().expandDirectional(dx, dy, dz));
        if (dy != 0) {
            dy = clip(blocks, box, dy, 1);
            box.shift(0, dy, 0);
        }
        boolean zFirst = Math.abs(dx) < Math.abs(dz);
        if (zFirst && dz != 0) {
            dz = clip(blocks, box, dz, 2);
            box.shift(0, 0, dz);
        }
        if (dx != 0) {
            dx = clip(blocks, box, dx, 0);
            box.shift(dx, 0, 0);
        }
        if (!zFirst && dz != 0) {
            dz = clip(blocks, box, dz, 2);
        }
        sim.x += dx;
        sim.y += dy;
        sim.z += dz;
        // Un choc annule la vitesse sur l'axe touche.
        if (dx != sim.vx) {
            sim.vx = 0;
        }
        if (dy != sim.vy) {
            sim.vy = 0;
        }
        if (dz != sim.vz) {
            sim.vz = 0;
        }
    }

    private static BoundingBox box(double x, double y, double z) {
        return new BoundingBox(x - HALF_WIDTH, y, z - HALF_WIDTH, x + HALF_WIDTH, y + HEIGHT, z + HALF_WIDTH);
    }

    private static double min(BoundingBox box, int axis) {
        return axis == 0 ? box.getMinX() : axis == 1 ? box.getMinY() : box.getMinZ();
    }

    private static double max(BoundingBox box, int axis) {
        return axis == 0 ? box.getMaxX() : axis == 1 ? box.getMaxY() : box.getMaxZ();
    }

    /** Deplacement possible sur un axe (0 = X, 1 = Y, 2 = Z) avant de toucher un des blocs. */
    private static double clip(List<BoundingBox> blocks, BoundingBox box, double move, int axis) {
        int a = (axis + 1) % 3;
        int b = (axis + 2) % 3;
        for (BoundingBox block : blocks) {
            if (max(block, a) <= min(box, a) + EPSILON || min(block, a) >= max(box, a) - EPSILON
                    || max(block, b) <= min(box, b) + EPSILON || min(block, b) >= max(box, b) - EPSILON) {
                continue; // pas sur le trajet
            }
            if (move > 0 && min(block, axis) >= max(box, axis) - EPSILON) {
                move = Math.min(move, Math.max(0, min(block, axis) - max(box, axis)));
            } else if (move < 0 && max(block, axis) <= min(box, axis) + EPSILON) {
                move = Math.max(move, Math.min(0, max(block, axis) - min(box, axis)));
            }
        }
        return move;
    }

    /** Boites de collision des blocs de cette zone (un bloc de plus vers le bas : barrieres hautes de 1,5 bloc). */
    private List<BoundingBox> blockBoxes(BoundingBox area) {
        List<BoundingBox> result = new ArrayList<>();
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(area.getMinY()) - 1);
        int maxY = Math.min(world.getMaxHeight() - 1, (int) Math.floor(area.getMaxY()));
        for (int x = (int) Math.floor(area.getMinX()); x <= (int) Math.floor(area.getMaxX()); x++) {
            for (int z = (int) Math.floor(area.getMinZ()); z <= (int) Math.floor(area.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    addBoxes(world.getBlockAt(x, y, z), result);
                }
            }
        }
        return result;
    }

    private static void addBoxes(Block block, List<BoundingBox> result) {
        Material type = block.getType();
        if (type.isAir() || block.isPassable()) {
            return;
        }
        if (type.isOccluding()) { // bloc plein
            result.add(new BoundingBox(block.getX(), block.getY(), block.getZ(), block.getX() + 1, block.getY() + 1, block.getZ() + 1));
            return;
        }
        for (BoundingBox part : block.getCollisionShape().getBoundingBoxes()) {
            result.add(part.shift(block.getX(), block.getY(), block.getZ()));
        }
    }

    /**
     * Frottement du sol : moyenne de la glissance des blocs touches juste sous la coque (les blocs des coins du
     * pourtour ne comptent pas, ceux des bords seulement a la hauteur de la coque : meme parcours que le jeu). 0 = en
     * l'air. Glace compacte 0,98 ; glace bleue 0,989 ; bloc ordinaire 0,6.
     */
    private float groundFriction(BoundingBox box) {
        BoundingBox under = new BoundingBox(box.getMinX(), box.getMinY() - 0.001, box.getMinZ(), box.getMaxX(), box.getMinY(), box.getMaxZ());
        int x0 = (int) Math.floor(under.getMinX()) - 1;
        int x1 = (int) Math.ceil(under.getMaxX()) + 1;
        int y0 = Math.max(world.getMinHeight(), (int) Math.floor(under.getMinY()) - 1);
        int y1 = Math.min(world.getMaxHeight(), (int) Math.ceil(under.getMaxY()) + 1);
        int z0 = (int) Math.floor(under.getMinZ()) - 1;
        int z1 = (int) Math.ceil(under.getMaxZ()) + 1;
        float sum = 0;
        int count = 0;
        List<BoundingBox> parts = new ArrayList<>();
        for (int x = x0; x < x1; x++) {
            for (int z = z0; z < z1; z++) {
                int edge = (x == x0 || x == x1 - 1 ? 1 : 0) + (z == z0 || z == z1 - 1 ? 1 : 0);
                if (edge == 2 || !world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = y0; y < y1; y++) {
                    if (edge > 0 && (y == y0 || y == y1 - 1)) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.LILY_PAD) {
                        continue;
                    }
                    parts.clear();
                    addBoxes(block, parts);
                    for (BoundingBox part : parts) {
                        if (part.overlaps(under)) {
                            sum += block.getType().getSlipperiness();
                            count++;
                            break;
                        }
                    }
                }
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    /** Niveau de l'eau dans laquelle trempe le fond de la coque, ou NaN hors de l'eau. */
    private double waterLevel(BoundingBox box) {
        int y = (int) Math.floor(box.getMinY());
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return Double.NaN;
        }
        double level = Double.NaN;
        for (int x = (int) Math.floor(box.getMinX()); x < (int) Math.ceil(box.getMaxX()); x++) {
            for (int z = (int) Math.floor(box.getMinZ()); z < (int) Math.ceil(box.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                double height = waterHeight(world.getBlockAt(x, y, z));
                if (height > 0 && box.getMinY() < y + height) {
                    level = Double.isNaN(level) ? y + height : Math.max(level, y + height);
                }
            }
        }
        return level;
    }

    private static double waterHeight(Block block) {
        BlockData data = block.getBlockData();
        if (block.getType() == Material.WATER) {
            if (block.getRelative(0, 1, 0).getType() == Material.WATER) {
                return 1;
            }
            int level = data instanceof Levelled levelled ? levelled.getLevel() : 0;
            return level == 0 || level >= 8 ? 8 / 9.0 : (8 - level) / 9.0;
        }
        return data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged() ? 8 / 9.0 : 0;
    }
}
