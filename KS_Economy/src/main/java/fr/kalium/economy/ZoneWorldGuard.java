package fr.kalium.economy;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;

/** 1.1.0 : région WorldGuard des magasins. Classe chargée seulement si WorldGuard est activé. */
final class ZoneWorldGuard {

    private ZoneWorldGuard() {
    }

    /** La colonne (x, z) est-elle dans la région (quelle que soit la hauteur dans la région) ? */
    static boolean contient(World monde, String region, int x, int z) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(monde));
        ProtectedRegion zone = regions == null ? null : regions.getRegion(region);
        if (zone == null) {
            return false;
        }
        int y = (zone.getMinimumPoint().y() + zone.getMaximumPoint().y()) / 2;
        return zone.contains(x, y, z);
    }
}
