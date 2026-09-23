package fr.kalium.core.claims;

import fr.kalium.core.KaliumCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applique la protection des chunks reclames : casse/pose de blocs, utilisation des blocs
 * interactifs (coffres, etabli, enclume...) et degats PvP contre le proprietaire dans son chunk.
 * Chaque volet est desactivable independamment depuis les Parametres (voir ClaimsService).
 */
public final class ClaimsListener implements Listener {

    /** Blocs consideres "utilisables" (stockage, artisanat...) bloques pour les non-proprietaires. */
    private static final Set<Material> PROTECTED_INTERACTIONS = buildProtectedMaterials();

    private final KaliumCore plugin;
    private final Map<UUID, Long> lastWarn = new ConcurrentHashMap<>();

    public ClaimsListener(KaliumCore plugin) {
        this.plugin = plugin;
    }

    private boolean bypass(Player player) {
        return player.hasPermission("kaliumcore.claims.bypass");
    }

    /** Anti-spam : un message de protection par joueur au maximum toutes les 2 secondes. */
    private boolean throttled(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastWarn.put(player.getUniqueId(), now);
        return previous != null && now - previous < 2000L;
    }

    private void warn(Player player, String key, UUID owner) {
        if (throttled(player)) {
            return;
        }
        String ownerName = plugin.getServer().getOfflinePlayer(owner).getName();
        player.sendMessage(plugin.mm().deserialize(plugin.msg(key),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed(
                        "owner", ownerName != null ? ownerName : "?")));
    }

    // ------------------------------------------------------------------ blocs

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.modules().isEnabled("claims") || !plugin.claims().breakProtectionEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (bypass(player)) {
            return;
        }
        UUID owner = plugin.claims().ownerOf(event.getBlock().getChunk());
        if (owner != null && !owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player, "claims-protected-build", owner);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.modules().isEnabled("claims") || !plugin.claims().placeProtectionEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (bypass(player)) {
            return;
        }
        UUID owner = plugin.claims().ownerOf(event.getBlock().getChunk());
        if (owner != null && !owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player, "claims-protected-build", owner);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (!plugin.modules().isEnabled("claims") || !plugin.claims().interactProtectionEnabled()) {
            return;
        }
        if (!PROTECTED_INTERACTIONS.contains(event.getClickedBlock().getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (bypass(player)) {
            return;
        }
        UUID owner = plugin.claims().ownerOf(event.getClickedBlock().getChunk());
        if (owner != null && !owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player, "claims-protected-interact", owner);
        }
    }

    // ------------------------------------------------------------------ pvp

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.modules().isEnabled("claims") || !plugin.claims().pvpProtectionEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId()) || bypass(attacker)) {
            return;
        }
        UUID owner = plugin.claims().ownerOf(victim.getLocation().getChunk());
        // "Ce joueur (le proprietaire) beneficie d'une protection pvp dans son chunk" : seule la
        // victime-proprietaire est protegee, pas de restriction PvP generale sur un chunk qui ne
        // lui appartient pas.
        if (owner != null && owner.equals(victim.getUniqueId())) {
            event.setCancelled(true);
            warn(attacker, "claims-protected-pvp", owner);
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ liste des blocs proteges

    private static Set<Material> buildProtectedMaterials() {
        Set<Material> set = ConcurrentHashMap.newKeySet();
        for (Material material : Material.values()) {
            if (!material.isBlock()) {
                continue;
            }
            String name = material.name();
            if (name.endsWith("_BED") || name.contains("CHEST") || name.contains("SHULKER_BOX")
                    || name.equals("BARREL") || name.contains("FURNACE") || name.equals("SMOKER")
                    || name.equals("ANVIL") || name.equals("CHIPPED_ANVIL") || name.equals("DAMAGED_ANVIL")
                    || name.equals("HOPPER") || name.equals("DROPPER") || name.equals("DISPENSER")
                    || name.equals("BREWING_STAND") || name.equals("ENDER_CHEST") || name.equals("LECTERN")
                    || name.equals("COMPOSTER") || name.equals("CRAFTING_TABLE") || name.equals("ENCHANTING_TABLE")
                    || name.equals("GRINDSTONE") || name.equals("SMITHING_TABLE") || name.equals("CARTOGRAPHY_TABLE")
                    || name.equals("LOOM") || name.equals("STONECUTTER") || name.equals("BEACON")
                    || name.equals("JUKEBOX") || name.equals("NOTE_BLOCK") || name.equals("RESPAWN_ANCHOR")
                    || name.equals("CAULDRON") || name.equals("CAMPFIRE") || name.equals("SOUL_CAMPFIRE")) {
                set.add(material);
            }
        }
        return set;
    }
}
