package fr.kalium.bedrockbreaker;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.Set;

/**
 * 1.1.0 (LeKiwi06, 26/10/2026) : régions WorldGuard où le Bedrock Breaker ne fonctionne pas (île centrale de l'End :
 * portail de sortie et portails d'accès).
 *
 * Classe chargée seulement si WorldGuard est activé (elle utilise ses classes et celles de WorldEdit).
 */
final class RegionsWorldGuard {

    private RegionsWorldGuard() {
    }

    /** Vrai si la position est dans l'une de ces régions (ids WorldGuard, en minuscules). */
    static boolean dans(Location position, Set<String> ids) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(position.getWorld()));
        if (regions == null) {
            return false;
        }
        for (ProtectedRegion region : regions.getApplicableRegions(BukkitAdapter.asBlockVector(position))) {
            if (ids.contains(region.getId())) {
                return true;
            }
        }
        return false;
    }
}
