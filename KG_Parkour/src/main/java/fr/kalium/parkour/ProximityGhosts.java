package fr.kalium.parkour;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.1.0 - demande de LeKiwi06 (26/09/2026) : "enlever les collisions entre joueurs et nous rendre invisible avec des
 * bottes en cuir colorees si on est trop proche d'un autre adversaire (3 blocks de distance)" ; choix : invisible
 * SEULEMENT pour l'adversaire proche (les spectateurs voient tout le monde normalement).
 *
 * - Anti-collision : les coureurs sont places dans une equipe "sans collision" du tableau principal (comme les
 *   bateaux de KG_BoatRace) : ni le serveur ni le jeu des joueurs ne les poussent plus les uns contre les autres.
 * - Proximite : Paper ne sait pas rendre un joueur invisible pour un seul autre joueur. Quand deux coureurs sont a
 *   moins de 3 blocs, chacun CACHE l'autre (hidePlayer) et voit a sa place un porte-armure invisible (marqueur, sans
 *   collision ni prise) qui ne porte que des bottes en cuir, a la couleur de ce coureur, et qui suit sa position.
 *   Ce porte-armure n'est montre qu'aux coureurs proches (invisible par defaut pour tous les autres).
 */
final class ProximityGhosts {

    private static final String TEAM = "kgparkour_nocol";
    /** Couleurs des bottes, une par coureur de la partie (dans l'ordre d'arrivee dans la course). */
    private static final List<Color> COLORS = List.of(
            Color.fromRGB(0xE53935), Color.fromRGB(0x1E88E5), Color.fromRGB(0x43A047), Color.fromRGB(0xFDD835),
            Color.fromRGB(0x8E24AA), Color.fromRGB(0xFB8C00), Color.fromRGB(0x00ACC1), Color.fromRGB(0xF06292),
            Color.fromRGB(0x6D4C41), Color.fromRGB(0xFFFFFF), Color.fromRGB(0x212121), Color.fromRGB(0x7CB342),
            Color.fromRGB(0x3949AB), Color.fromRGB(0xC0CA33), Color.fromRGB(0x546E7A), Color.fromRGB(0xD81B60));

    private final JavaPlugin plugin = JavaPlugin.getPlugin(KGParkour.class);
    private final Map<UUID, Color> colors = new HashMap<>();
    private final Map<UUID, ArmorStand> ghosts = new HashMap<>();
    /** Pour chaque coureur (qui regarde) : les adversaires qu'il voit en ce moment sous forme de bottes. */
    private final Map<UUID, Set<UUID>> hidden = new HashMap<>();
    private final Set<String> teamEntries = new HashSet<>();

    // ------------------------------------------------------------------ anti-collision

    private Team team() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = main.getTeam(TEAM);
        if (team == null) {
            team = main.registerNewTeam(TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }

    /** Un coureur entre dans la course : sans collision, et une couleur de bottes. */
    void join(Player player) {
        team().addEntry(player.getName());
        teamEntries.add(player.getName());
        colors.computeIfAbsent(player.getUniqueId(), id -> COLORS.get(colors.size() % COLORS.size()));
    }

    /** Un coureur ne court plus (arrivee, elimination, depart) : visible de tous, bottes retirees. */
    void leave(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        // Plus vu en bottes par personne...
        for (Map.Entry<UUID, Set<UUID>> entry : hidden.entrySet()) {
            if (entry.getValue().remove(uuid)) {
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer != null && player != null) {
                    viewer.showPlayer(plugin, player);
                }
            }
        }
        // ... et ne voit plus personne en bottes.
        Set<UUID> seen = hidden.remove(uuid);
        if (seen != null && player != null) {
            for (UUID target : seen) {
                Player other = Bukkit.getPlayer(target);
                if (other != null) {
                    player.showPlayer(plugin, other);
                }
                ArmorStand ghost = ghosts.get(target);
                if (ghost != null) {
                    player.hideEntity(plugin, ghost);
                }
            }
        }
        ArmorStand ghost = ghosts.remove(uuid);
        if (ghost != null) {
            ghost.remove();
        }
        if (player != null && teamEntries.remove(player.getName())) {
            team().removeEntry(player.getName());
        }
    }

    // ------------------------------------------------------------------ proximite

    /**
     * A chaque tick : pour chaque paire de coureurs en course dans ce monde, bottes seules a moins de "radius" blocs,
     * joueur normal au-dela.
     */
    void tick(Collection<Player> runners, World world, double radius) {
        double radiusSquared = radius * radius;
        for (Player viewer : runners) {
            Set<UUID> seen = hidden.computeIfAbsent(viewer.getUniqueId(), id -> new HashSet<>());
            for (Player other : runners) {
                if (other == viewer) {
                    continue;
                }
                boolean near = viewer.getWorld() == world && other.getWorld() == world
                        && viewer.getLocation().distanceSquared(other.getLocation()) < radiusSquared;
                boolean isHidden = seen.contains(other.getUniqueId());
                if (near && !isHidden) {
                    viewer.hidePlayer(plugin, other);
                    viewer.showEntity(plugin, ghost(other));
                    seen.add(other.getUniqueId());
                } else if (!near && isHidden) {
                    viewer.showPlayer(plugin, other);
                    ArmorStand ghost = ghosts.get(other.getUniqueId());
                    if (ghost != null) {
                        viewer.hideEntity(plugin, ghost);
                    }
                    seen.remove(other.getUniqueId());
                }
            }
        }
        // Les bottes suivent leur coureur.
        for (Player runner : runners) {
            ArmorStand ghost = ghosts.get(runner.getUniqueId());
            if (ghost != null && ghost.isValid()) {
                Location at = runner.getLocation();
                at.setPitch(0);
                ghost.teleport(at);
            }
        }
    }

    private ArmorStand ghost(Player runner) {
        ArmorStand existing = ghosts.get(runner.getUniqueId());
        if (existing != null && existing.isValid()) {
            return existing;
        }
        Location at = runner.getLocation();
        at.setPitch(0);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(colors.getOrDefault(runner.getUniqueId(), COLORS.get(0)));
        boots.setItemMeta(meta);
        ArmorStand ghost = runner.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setVisibleByDefault(false);
            stand.setPersistent(false);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setBasePlate(false);
            stand.setItem(EquipmentSlot.FEET, boots);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                try {
                    stand.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
                } catch (IllegalArgumentException ignored) {
                    // emplacement sans objet pour un porte-armure (ex. corps d'un animal)
                }
            }
        });
        ghosts.put(runner.getUniqueId(), ghost);
        return ghost;
    }

    /** Fin de la course : tout le monde visible, bottes supprimees, equipe videe. */
    void clear() {
        for (UUID uuid : new HashSet<>(hidden.keySet())) {
            leave(uuid);
        }
        for (UUID uuid : new HashSet<>(ghosts.keySet())) {
            leave(uuid);
        }
        for (String name : new HashSet<>(teamEntries)) {
            team().removeEntry(name);
        }
        teamEntries.clear();
        colors.clear();
    }
}
