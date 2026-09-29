package fr.kalium.biomechanger;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Demande de Maxster33 (29/09/2026) : le Changeur de Biome ne fonctionne pas dans les zones protégées par WorldGuard.
 * Choix de Maxster33 : refusé dès que la zone modifiée touche une région WorldGuard (hors région globale), même si le
 * joueur en est membre ou propriétaire.
 *
 * Classe chargée seulement si WorldGuard est activé (elle utilise ses classes et celles de WorldEdit).
 */
final class ProtectionWorldGuard {

    private ProtectionWorldGuard() {
    }

    /** Vrai si l'une des cellules de 4 x 4 x 4 blocs (qx, qy, qz) touche une région WorldGuard. */
    static boolean touche(World monde, List<int[]> cellules) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(monde));
        if (regions == null || cellules.isEmpty()) {
            return false;
        }
        // 1) Régions qui touchent la boîte englobante (rapide) ; aucune : rien n'est protégé.
        int[] min = cellules.get(0).clone();
        int[] max = cellules.get(0).clone();
        for (int[] c : cellules) {
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], c[i]);
                max[i] = Math.max(max[i], c[i]);
            }
        }
        List<ProtectedRegion> proches = new ArrayList<>();
        for (ProtectedRegion region : regions.getApplicableRegions(cuboide(min, max))) {
            if (!ProtectedRegion.GLOBAL_REGION.equals(region.getId())) {
                proches.add(region);
            }
        }
        if (proches.isEmpty()) {
            return false;
        }
        // 2) Cellule par cellule : une sphère ne touche pas forcément une région proche des coins de sa boîte.
        for (int[] c : cellules) {
            if (!cuboide(c, c).getIntersectingRegions(proches).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Région temporaire couvrant les cellules min à max (bornes comprises). */
    private static ProtectedCuboidRegion cuboide(int[] min, int[] max) {
        return new ProtectedCuboidRegion("ks_biomechanger_zone", true,
                BlockVector3.at(min[0] * 4, min[1] * 4, min[2] * 4),
                BlockVector3.at(max[0] * 4 + 3, max[1] * 4 + 3, max[2] * 4 + 3));
    }
}
