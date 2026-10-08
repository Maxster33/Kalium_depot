package fr.kalium.teleport;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Région WorldGuard du spawn et claims de SimpleClaimSystem (maison du spawn), comme pour les magasins de KS_Economy.
 *
 * Classe chargée seulement si WorldGuard et SimpleClaimSystem sont activés (elle utilise leurs classes).
 */
final class Zones {

    private Zones() {
    }

    /** L'endroit est-il dans la région (quelle que soit la hauteur) ? */
    static boolean dansRegion(Location lieu, String region) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(lieu.getWorld()));
        ProtectedRegion zone = regions == null ? null : regions.getRegion(region);
        if (zone == null) {
            return false;
        }
        int y = (zone.getMinimumPoint().y() + zone.getMaximumPoint().y()) / 2;
        return zone.contains(lieu.getBlockX(), y, lieu.getBlockZ());
    }

    /** L'endroit est-il dans un claim dont ce joueur est le propriétaire ? */
    static boolean dansSonClaim(Location lieu, UUID joueur) {
        // SCS 1.13.1 ne crée pas son API lui-même (sans effet si c'est déjà fait).
        SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
        Claim claim = SimpleClaimSystemAPI_Provider.getAPI().getClaimAtChunk(lieu.getChunk());
        return claim != null && joueur.equals(claim.getUUID());
    }
}
