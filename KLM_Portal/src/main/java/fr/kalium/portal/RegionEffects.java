package fr.kalium.portal;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Effets de zone (KLM_Portal 1.2.0 - demande de LeKiwi06, 26/09/2026 : "rajoute le speed et le jump boost pour tous
 * dans la region lobby", auparavant donnes a la connexion par ConditionalEvents). Tant qu'un joueur est dans une
 * region de "region-effects", il a ses effets (duree infinie, sans particules) ; en sortant, ils lui sont retires.
 *
 * Sans memoire : a chaque verification, un effet configure quelque part, de duree infinie, que le joueur a sans etre
 * dans une region qui le donne, est retire (couvre aussi un redemarrage ou une region retiree de la config). Les
 * effets a duree limitee (potions bues...) ne sont jamais touches.
 */
final class RegionEffects {

    private record Zone(String region, String world, Map<PotionEffectType, Integer> effects) {
    }

    private final KlmPortal plugin;
    private final List<Zone> zones = new ArrayList<>();
    private final Set<PotionEffectType> managed = new HashSet<>();

    RegionEffects(KlmPortal plugin) {
        this.plugin = plugin;
    }

    void load() {
        zones.clear();
        managed.clear();
        ConfigurationSection all = plugin.getConfig().getConfigurationSection("region-effects");
        if (all == null) {
            return;
        }
        for (String region : all.getKeys(false)) {
            String world = all.getString(region + ".world", "");
            ConfigurationSection effects = all.getConfigurationSection(region + ".effects");
            if (world.isBlank() || effects == null) {
                plugin.getLogger().warning("Effets de la région " + region + " ignorés : \"world\" ou \"effects\" manquant.");
                continue;
            }
            Map<PotionEffectType, Integer> map = new HashMap<>();
            for (String name : effects.getKeys(false)) {
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
                if (type == null) {
                    plugin.getLogger().warning("Effet inconnu dans region-effects." + region + " : " + name);
                    continue;
                }
                map.put(type, Math.max(0, Math.min(255, effects.getInt(name))));
            }
            zones.add(new Zone(region, world, map));
            managed.addAll(map.keySet());
        }
        plugin.getLogger().info(zones.size() + " région(s) à effets chargée(s).");
    }

    /** Appele toutes les secondes : donne ou retire les effets selon la position de chaque joueur. */
    void tick() {
        if (managed.isEmpty()) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Map<PotionEffectType, Integer> wanted = wantedAt(player.getLocation());
            for (Map.Entry<PotionEffectType, Integer> entry : wanted.entrySet()) {
                PotionEffect current = player.getPotionEffect(entry.getKey());
                if (current == null || current.getAmplifier() != entry.getValue() || !current.isInfinite()) {
                    player.addPotionEffect(new PotionEffect(entry.getKey(), PotionEffect.INFINITE_DURATION,
                            entry.getValue(), false, false, true));
                }
            }
            for (PotionEffectType type : managed) {
                if (!wanted.containsKey(type)) {
                    PotionEffect current = player.getPotionEffect(type);
                    if (current != null && current.isInfinite()) {
                        player.removePotionEffect(type);
                    }
                }
            }
        }
    }

    /** Effets des regions qui contiennent ce point (le plus fort l'emporte si deux regions donnent le meme). */
    private Map<PotionEffectType, Integer> wantedAt(Location location) {
        Map<PotionEffectType, Integer> wanted = new HashMap<>();
        RegionManager manager = null;
        BlockVector3 block = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        for (Zone zone : zones) {
            if (!zone.world().equals(location.getWorld().getName())) {
                continue;
            }
            if (manager == null) {
                manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get(BukkitAdapter.adapt(location.getWorld()));
                if (manager == null) {
                    return wanted;
                }
            }
            ProtectedRegion region = manager.getRegion(zone.region());
            if (region != null && region.contains(block)) {
                zone.effects().forEach((type, level) -> wanted.merge(type, level, Math::max));
            }
        }
        return wanted;
    }
}
