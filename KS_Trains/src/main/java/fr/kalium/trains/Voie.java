package fr.kalium.trains;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.RedstoneRail;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * 1.3.2 (LeKiwi06 : « il faut également que le train se fasse ralentir dans les virages » ; réponses : -25 % de sa
 * vitesse, sauf au niveau 1 ; il anticipe, et une diagonale ne compte que pour un virage tant qu'il y reste).
 *
 * Lecture de la voie : rail sous un wagon, rail courbe, et marche le long des rails devant le train (bloc par bloc, en
 * suivant les deux sorties de chaque rail, pentes comprises) pour trouver un virage sur la distance de freinage.
 */
final class Voie {

    private Voie() {
    }

    /** Rail sous un wagon (bloc du wagon, ou celui du dessous), ou null. */
    static Block rail(Entity wagon) {
        Block bloc = wagon.getLocation().getBlock();
        if (Tag.RAILS.isTagged(bloc.getType())) {
            return bloc;
        }
        Block dessous = bloc.getRelative(BlockFace.DOWN);
        return Tag.RAILS.isTagged(dessous.getType()) ? dessous : null;
    }

    /** 1.3.5 : rail propulseur allumé. */
    static boolean propulseurAllume(Block rail) {
        return rail != null && rail.getBlockData() instanceof RedstoneRail propulseur
                && rail.getType() == Material.POWERED_RAIL && propulseur.isPowered();
    }

    /** 1.3.5 : rail propulseur éteint. */
    static boolean propulseurEteint(Block rail) {
        return rail != null && rail.getBlockData() instanceof RedstoneRail propulseur
                && rail.getType() == Material.POWERED_RAIL && !propulseur.isPowered();
    }

    static boolean virage(Block rail) {
        return rail != null && rail.getBlockData() instanceof Rail donnees && switch (donnees.getShape()) {
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> true;
            default -> false;
        };
    }

    /** Les deux sorties d'un rail : {dx, dy, dz} (dy = 1 vers le haut d'une pente). */
    private static int[][] sorties(Rail.Shape forme) {
        return switch (forme) {
            case NORTH_SOUTH -> new int[][]{{0, 0, -1}, {0, 0, 1}};
            case EAST_WEST -> new int[][]{{1, 0, 0}, {-1, 0, 0}};
            case ASCENDING_EAST -> new int[][]{{-1, 0, 0}, {1, 1, 0}};
            case ASCENDING_WEST -> new int[][]{{1, 0, 0}, {-1, 1, 0}};
            case ASCENDING_NORTH -> new int[][]{{0, 0, 1}, {0, 1, -1}};
            case ASCENDING_SOUTH -> new int[][]{{0, 0, -1}, {0, 1, 1}};
            case SOUTH_EAST -> new int[][]{{0, 0, 1}, {1, 0, 0}};
            case SOUTH_WEST -> new int[][]{{0, 0, 1}, {-1, 0, 0}};
            case NORTH_WEST -> new int[][]{{0, 0, -1}, {-1, 0, 0}};
            case NORTH_EAST -> new int[][]{{0, 0, -1}, {1, 0, 0}};
        };
    }

    /**
     * Vrai si un rail courbe se trouve sur les {@code blocs} prochains rails devant {@code depart}, en partant dans la
     * direction {@code sens} (direction de la marche).
     */
    static boolean virageDevant(Block depart, Vector sens, int blocs) {
        Block ici = depart;
        int[] arrivee = null;
        for (int pas = 0; pas < blocs && ici != null; pas++) {
            if (!(ici.getBlockData() instanceof Rail donnees)) {
                return false;
            }
            int[][] sorties = sorties(donnees.getShape());
            int[] sortie;
            if (arrivee == null) {
                // Premier rail : la sortie dans le sens de la marche.
                double a = sorties[0][0] * sens.getX() + sorties[0][2] * sens.getZ();
                double b = sorties[1][0] * sens.getX() + sorties[1][2] * sens.getZ();
                sortie = a >= b ? sorties[0] : sorties[1];
            } else {
                // Rails suivants : la sortie qui n'est pas celle par où l'on arrive.
                boolean premiere = sorties[0][0] == -arrivee[0] && sorties[0][2] == -arrivee[2];
                sortie = premiere ? sorties[1] : sorties[0];
            }
            Block suivant = ici.getRelative(sortie[0], sortie[1], sortie[2]);
            if (!Tag.RAILS.isTagged(suivant.getType())) {
                suivant = suivant.getRelative(BlockFace.DOWN);
                if (!Tag.RAILS.isTagged(suivant.getType())) {
                    return false;
                }
            }
            if (virage(suivant)) {
                return true;
            }
            arrivee = sortie;
            ici = suivant;
        }
        return false;
    }
}
