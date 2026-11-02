package fr.kalium.encapsuleur;

import org.bukkit.block.BlockFace;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;

import java.util.List;

/**
 * Une construction tournée et retournée : comme le jeu (StructureTemplate), le miroir d'abord puis la rotation autour
 * de l'origine ; les positions sont ensuite décalées pour rester dans une boîte qui commence à (0, 0, 0).
 */
record Forme(int sx, int sy, int sz, StructureRotation rotation, Mirror miroir) {

    static final List<BlockFace> CAP = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    /** Rotation qui amène la direction a sur la direction b (directions horizontales). */
    static StructureRotation entre(BlockFace a, BlockFace b) {
        int k = Math.floorMod(CAP.indexOf(b) - CAP.indexOf(a), 4);
        return tours(k);
    }

    static StructureRotation tours(int k) {
        return switch (Math.floorMod(k, 4)) {
            case 1 -> StructureRotation.CLOCKWISE_90;
            case 2 -> StructureRotation.CLOCKWISE_180;
            case 3 -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };
    }

    static int quarts(StructureRotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> 0;
        };
    }

    private int[] brut(int x, int z) {
        if (miroir == Mirror.LEFT_RIGHT) {
            z = -z;
        } else if (miroir == Mirror.FRONT_BACK) {
            x = -x;
        }
        return switch (rotation) {
            case CLOCKWISE_90 -> new int[]{-z, x};
            case CLOCKWISE_180 -> new int[]{-x, -z};
            case COUNTERCLOCKWISE_90 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    /** Position dans la boîte tournée d'un bloc de la construction (x, y, z relatifs). */
    int[] position(int x, int y, int z) {
        int[] a = brut(0, 0);
        int[] b = brut(sx - 1, sz - 1);
        int[] p = brut(x, z);
        return new int[]{p[0] - Math.min(a[0], b[0]), y, p[1] - Math.min(a[1], b[1])};
    }

    /** Largeur (x) de la boîte tournée. */
    int tx() {
        return quarts(rotation) % 2 == 0 ? sx : sz;
    }

    int tz() {
        return quarts(rotation) % 2 == 0 ? sz : sx;
    }

    /** Une direction de la construction après rotation (le miroir LEFT_RIGHT inverse le nord et le sud). */
    BlockFace tourner(BlockFace cap) {
        if (miroir == Mirror.LEFT_RIGHT && (cap == BlockFace.NORTH || cap == BlockFace.SOUTH)) {
            cap = cap.getOppositeFace();
        } else if (miroir == Mirror.FRONT_BACK && (cap == BlockFace.EAST || cap == BlockFace.WEST)) {
            cap = cap.getOppositeFace();
        }
        return CAP.get(Math.floorMod(CAP.indexOf(cap) + quarts(rotation), 4));
    }
}
