package fr.kalium.boatrace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.3.0 : anti-collision entre bateaux (cahier des charges, point 5 ; piste retenue le 24/09/2026 d'apres les videos
 * Boatlabs). En bateau, c'est le jeu du joueur qui calcule le mouvement et les collisions avec les entites qu'il
 * connait : pour chaque coureur Java, les vrais bateaux ET pilotes des adversaires sont donc CACHES (il ne peut plus
 * les heurter), et remplaces par une copie visuelle sans aucune collision (entites d'affichage) : une coque, la tete
 * du joueur et son pseudo, qui suivent sa position.
 *
 * Joueurs Bedrock (Geyser) : les entites d'affichage y passent mal ; ils continuent de voir les vrais bateaux (donc de
 * les heurter de leur cote) : a tester, voir le cahier des charges. Les copies ne comptent jamais comme hors-piste
 * (le hors-piste ne regarde que les blocs du monde).
 *
 * Limite : un joueur cache disparait aussi de la liste des joueurs (Tab) du coureur pendant la course.
 */
final class CollisionShield {

    private record Copy(BlockDisplay hull, ItemDisplay head, TextDisplay name) {
        List<Entity> all() {
            return List.of(hull, head, name);
        }

        void remove() {
            hull.remove();
            head.remove();
            name.remove();
        }
    }

    private final Plugin plugin;
    private final World world;
    private final Material hullMaterial;
    /** Coureurs encore proteges (en course). */
    private final Set<UUID> active = new HashSet<>();
    private final Map<UUID, Copy> copies = new HashMap<>();
    /** Bateau actuel de chaque coureur (un nouveau bateau est cree apres une chute). */
    private final Map<UUID, Entity> boats = new HashMap<>();

    CollisionShield(Plugin plugin, World world, String boatType) {
        this.plugin = plugin;
        this.world = world;
        Material hull = Material.matchMaterial(boatType.toUpperCase(java.util.Locale.ROOT).replace("_CHEST_BOAT", "_SLAB")
                .replace("_BOAT", "_SLAB").replace("_CHEST_RAFT", "_SLAB").replace("_RAFT", "_SLAB"));
        this.hullMaterial = hull != null && hull.isBlock() && hull.createBlockData() instanceof Slab ? hull : Material.OAK_SLAB;
    }

    static boolean bedrock(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }

    /** Debut de la course : chaque coureur est protege et recoit sa copie visuelle. */
    void start(Iterable<Player> racers) {
        for (Player player : racers) {
            active.add(player.getUniqueId());
            copies.put(player.getUniqueId(), spawnCopy(player));
        }
        for (UUID viewer : active) {
            for (UUID other : active) {
                if (!viewer.equals(other)) {
                    hideFrom(viewer, other);
                }
            }
        }
    }

    private Copy spawnCopy(Player owner) {
        Location at = owner.getLocation();
        BlockDisplay hull = world.spawn(at, BlockDisplay.class, display -> {
            Slab slab = (Slab) hullMaterial.createBlockData();
            slab.setType(Slab.Type.BOTTOM);
            display.setBlock(slab);
            // Coque d'environ 1,4 x 0,5 x 2 blocs, centree sous le joueur (l'orientation suit celle du bateau).
            display.setTransformation(new Transformation(new Vector3f(-0.7f, -0.05f, -1.0f), new AxisAngle4f(),
                    new Vector3f(1.4f, 1.0f, 2.0f), new AxisAngle4f()));
            prepare(display);
        });
        ItemDisplay head = world.spawn(at, ItemDisplay.class, display -> {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            if (skull.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(owner);
                skull.setItemMeta(meta);
            }
            display.setItemStack(skull);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            display.setTransformation(new Transformation(new Vector3f(0f, 0.9f, 0f), new AxisAngle4f(),
                    new Vector3f(0.8f, 0.8f, 0.8f), new AxisAngle4f()));
            prepare(display);
        });
        TextDisplay name = world.spawn(at, TextDisplay.class, display -> {
            display.text(Component.text(owner.getName(), NamedTextColor.WHITE));
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(new Transformation(new Vector3f(0f, 1.7f, 0f), new AxisAngle4f(),
                    new Vector3f(1f, 1f, 1f), new AxisAngle4f()));
            prepare(display);
        });
        return new Copy(hull, head, name);
    }

    private static void prepare(Display display) {
        display.setPersistent(false);
        display.setVisibleByDefault(false); // montree seulement aux adversaires Java (voir hideFrom)
        display.setTeleportDuration(2); // deplacement fluide entre deux mises a jour
        display.setInterpolationDuration(2);
    }

    /** Le coureur viewer ne voit plus le vrai bateau / pilote de other, mais sa copie. */
    private void hideFrom(UUID viewerId, UUID otherId) {
        Player viewer = Bukkit.getPlayer(viewerId);
        Player other = Bukkit.getPlayer(otherId);
        if (viewer == null || bedrock(viewerId)) {
            return; // Bedrock : voit les vrais bateaux (voir en-tete)
        }
        if (other != null) {
            viewer.hideEntity(plugin, other);
            Entity boat = boats.get(otherId);
            if (boat != null) {
                viewer.hideEntity(plugin, boat);
            }
        }
        Copy copy = copies.get(otherId);
        if (copy != null) {
            for (Entity part : copy.all()) {
                viewer.showEntity(plugin, part);
            }
        }
    }

    /** viewer voit de nouveau le vrai bateau / pilote de other, plus sa copie. */
    private void revealTo(UUID viewerId, UUID otherId) {
        Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer == null) {
            return;
        }
        Player other = Bukkit.getPlayer(otherId);
        if (other != null) {
            viewer.showEntity(plugin, other);
        }
        Entity boat = boats.get(otherId);
        if (boat != null && boat.isValid()) {
            viewer.showEntity(plugin, boat);
        }
        Copy copy = copies.get(otherId);
        if (copy != null) {
            for (Entity part : copy.all()) {
                viewer.hideEntity(plugin, part);
            }
        }
    }

    /**
     * A chaque passage de la boucle : suit le bateau actuel de chaque coureur (un nouveau bateau apres une chute est
     * cache a son tour) et deplace sa copie.
     */
    void tick() {
        for (UUID id : active) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            Entity vehicle = player.getVehicle();
            Entity known = boats.get(id);
            if (vehicle != null && vehicle != known) {
                boats.put(id, vehicle);
                for (UUID viewer : active) {
                    Player v = Bukkit.getPlayer(viewer);
                    if (!viewer.equals(id) && v != null && !bedrock(viewer)) {
                        v.hideEntity(plugin, vehicle);
                    }
                }
            }
            Copy copy = copies.get(id);
            if (copy == null) {
                continue;
            }
            Location at = (vehicle != null ? vehicle.getLocation() : player.getLocation()).clone();
            at.setPitch(0);
            if (vehicle == null) {
                at.setYaw(player.getLocation().getYaw());
            }
            for (Entity part : copy.all()) {
                part.teleport(at);
            }
        }
    }

    /** Un coureur a fini, abandonne ou quitte la partie : plus de protection pour lui ni contre lui. */
    void release(UUID id) {
        if (!active.remove(id)) {
            return;
        }
        for (UUID other : new ArrayList<>(active)) {
            revealTo(other, id); // les autres le revoient
            revealTo(id, other); // il revoit les autres
        }
        Copy copy = copies.remove(id);
        if (copy != null) {
            copy.remove();
        }
        boats.remove(id);
    }

    /** Fin de la course : tout est rendu visible, copies supprimees. */
    void stop() {
        for (UUID id : new ArrayList<>(active)) {
            release(id);
        }
        for (Copy copy : copies.values()) {
            copy.remove();
        }
        copies.clear();
        boats.clear();
        active.clear();
    }
}
