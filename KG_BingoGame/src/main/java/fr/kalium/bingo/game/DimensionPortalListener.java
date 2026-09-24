package fr.kalium.bingo.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.logging.Logger;

/**
 * Portails des mondes de partie (AJOUTE en 0.1.22 - demande explicite de l'utilisateur : "pour bingo, le nether
 * et l'end doivent etre generes comme l'overworld (chacun le sien)").
 *
 * Sans ce listener, un portail pris dans un monde de partie menait au Nether / a l'End PAR DEFAUT du serveur,
 * partages par tout le monde. Ici chaque equipe a les siens (meme seed que son overworld, voir
 * InstanceWorldManager.createDimension) :
 * - portail du Nether : overworld de l'equipe <-> Nether de l'equipe, coordonnees divisees / multipliees par 8
 *   comme en vanilla (le serveur cherche un portail existant pres de la destination ou en construit un) ;
 * - portail de l'End (overworld) : plateforme d'arrivee de l'End de l'equipe (100, 49, 0) ;
 * - portail de sortie de l'End : point de reapparition du joueur s'il est sur les mondes de son equipe, sinon
 *   le point d'apparition de la partie.
 * Les objets et creatures qui passent un portail suivent les memes regles.
 * Si le Nether / l'End de l'equipe n'est pas encore pret (pre-generation en cascade pas terminee), il est
 * genere a ce moment-la.
 */
public final class DimensionPortalListener implements Listener {

    private static final Location END_PLATFORM = new Location(null, 100.5, 49, 0.5, 90f, 0f);

    private final Logger logger;
    private final GameManager gameManager;

    public DimensionPortalListener(Logger logger, GameManager gameManager) {
        this.logger = logger;
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        PortalType type = switch (event.getCause()) {
            case NETHER_PORTAL -> PortalType.NETHER;
            case END_PORTAL -> PortalType.ENDER;
            default -> null;
        };
        if (type == null) {
            return;
        }
        Location to = destination(event.getFrom(), type, event.getPlayer());
        if (to != null) {
            event.setTo(to);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        PortalType type = event.getPortalType();
        if (type != PortalType.NETHER && type != PortalType.ENDER) {
            return;
        }
        Location to = destination(event.getFrom(), type, null);
        if (to != null) {
            event.setTo(to);
        }
    }

    /** Destination dans les mondes de l'equipe, ou null si le depart n'est pas un monde de partie. */
    private Location destination(Location from, PortalType type, Player player) {
        if (from == null || from.getWorld() == null) {
            return null;
        }
        GameManager.InstanceRef ref = gameManager.findInstanceByWorld(from.getWorld());
        if (ref == null || ref.instance().getWorld() == null) {
            return null;
        }
        World overworld = ref.instance().getWorld();
        World.Environment here = from.getWorld().getEnvironment();
        try {
            if (type == PortalType.NETHER) {
                if (here == World.Environment.NORMAL) {
                    World nether = gameManager.dimensionOf(ref.game(), ref.instance(), World.Environment.NETHER);
                    return scaled(from, nether, 1.0 / 8.0);
                }
                if (here == World.Environment.NETHER) {
                    return scaled(from, overworld, 8.0);
                }
                return null;
            }
            // Portail de l'End
            if (here == World.Environment.NORMAL) {
                World end = gameManager.dimensionOf(ref.game(), ref.instance(), World.Environment.THE_END);
                Location platform = END_PLATFORM.clone();
                platform.setWorld(end);
                buildEndPlatform(platform);
                return platform;
            }
            if (here == World.Environment.THE_END) {
                if (player != null) {
                    Location respawn = player.getRespawnLocation();
                    if (respawn != null && GameManager.belongsTo(respawn.getWorld(), ref.instance())) {
                        return respawn;
                    }
                }
                Location spawn = ref.instance().getSpawnLocation();
                return spawn != null ? spawn : overworld.getSpawnLocation();
            }
        } catch (RuntimeException e) {
            logger.warning("[KG_BingoGame] Portail vers le Nether/End de '" + ref.instance().getInstanceId() + "' : " + e.getMessage());
        }
        return null;
    }

    private static Location scaled(Location from, World target, double factor) {
        double x = from.getX() * factor;
        double z = from.getZ() * factor;
        double border = target.getWorldBorder().getSize() / 2 - 16;
        x = Math.max(-border, Math.min(border, x));
        z = Math.max(-border, Math.min(border, z));
        double y = Math.max(target.getMinHeight() + 1, Math.min(target.getLogicalHeight() - 1, from.getY()));
        return new Location(target, x, y, z, from.getYaw(), from.getPitch());
    }

    /** Plateforme d'obsidienne 5 x 5 de l'End (comme en vanilla) avec 3 blocs d'air au-dessus. */
    private static void buildEndPlatform(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1;
        int cz = center.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(cx + dx, cy, cz + dz).setType(Material.OBSIDIAN, false);
                for (int dy = 1; dy <= 3; dy++) {
                    world.getBlockAt(cx + dx, cy + dy, cz + dz).setType(Material.AIR, false);
                }
            }
        }
    }
}
