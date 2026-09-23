package fr.kalium.core.claims;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Systeme de claim de chunks : un joueur peut reclamer les chunks dans lesquels il se trouve
 * (limite par son nombre d'"emplacements"/claim slots), y beneficier d'une protection, et definir
 * l'un de ses chunks comme "base" (point de teleportation via le menu).
 * <p>
 * Stockage autonome : un seul fichier {@code plugins/KaliumCore/claims.yml} (pas de base de donnees
 * externe), avec l'index chunk -&gt; proprietaire et les donnees par joueur (emplacements, base).
 */
public final class ClaimsService {

    private final JavaPlugin plugin;
    private final File file;

    private final Map<UUID, PlayerClaims> players = new ConcurrentHashMap<>();
    /** Cle de chunk ("world:x:z") -> proprietaire. */
    private final Map<String, UUID> claims = new ConcurrentHashMap<>();
    /** Proprietaire -> ensemble de ses chunks (derive de "claims", tenu a jour en parallele). */
    private final Map<UUID, Set<String>> byOwner = new ConcurrentHashMap<>();

    public enum ClaimResult { OK, ALREADY_CLAIMED, NO_SLOTS, DISABLED }

    public enum UnclaimResult { OK, NOT_CLAIMED, NOT_OWNER, DISABLED }

    public ClaimsService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "claims.yml");
    }

    // ------------------------------------------------------------------ cle de chunk

    public static String key(Chunk chunk) {
        return key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static String key(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    // ------------------------------------------------------------------ reglages (config.yml)

    public boolean masterEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.enabled", true);
    }

    public boolean claimActionEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.claim-action", true);
    }

    public void claimActionEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.claim-action", value);
    }

    public boolean unclaimActionEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.unclaim-action", true);
    }

    public void unclaimActionEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.unclaim-action", value);
    }

    // La protection etait a l'origine un seul interrupteur ; elle est maintenant scindee en 4 volets
    // independants (casse, depot, utilisation, pvp), chacun activable/desactivable separement depuis
    // Parametres > Claims > Options.
    public boolean breakProtectionEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.protection-break", true);
    }

    public void breakProtectionEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.protection-break", value);
    }

    public boolean placeProtectionEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.protection-place", true);
    }

    public void placeProtectionEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.protection-place", value);
    }

    public boolean interactProtectionEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.protection-interact", true);
    }

    public void interactProtectionEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.protection-interact", value);
    }

    public boolean pvpProtectionEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.protection-pvp", true);
    }

    public void pvpProtectionEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.protection-pvp", value);
    }

    public boolean homeEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.home-chunk", true);
    }

    public void homeEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.home-chunk", value);
    }

    public boolean itemConversionEnabled() {
        return plugin.getConfig().getBoolean("modules.claims.item-conversion", true);
    }

    public void itemConversionEnabled(boolean value) {
        plugin.getConfig().set("modules.claims.item-conversion", value);
    }

    public int defaultSlots() {
        return Math.max(0, plugin.getConfig().getInt("modules.claims.default-slots", 6));
    }

    public void defaultSlots(int value) {
        plugin.getConfig().set("modules.claims.default-slots", Math.max(0, value));
        plugin.saveConfig();
    }

    // ------------------------------------------------------------------ acces joueur / emplacements

    public PlayerClaims data(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerClaims(k, defaultSlots()));
    }

    public int used(UUID id) {
        Set<String> owned = byOwner.get(id);
        return owned == null ? 0 : owned.size();
    }

    public int total(UUID id) {
        return data(id).slotsTotal();
    }

    public int available(UUID id) {
        return Math.max(0, total(id) - used(id));
    }

    public void adjustSlots(UUID id, int delta) {
        PlayerClaims data = data(id);
        data.slotsTotal(data.slotsTotal() + delta);
        save();
    }

    // ------------------------------------------------------------------ proprietaire d'un chunk

    public UUID ownerOf(Chunk chunk) {
        return claims.get(key(chunk));
    }

    // ------------------------------------------------------------------ claim / unclaim

    public ClaimResult claim(Player player) {
        if (!claimActionEnabled()) {
            return ClaimResult.DISABLED;
        }
        Chunk chunk = player.getLocation().getChunk();
        String key = key(chunk);
        if (claims.containsKey(key)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        UUID id = player.getUniqueId();
        if (available(id) <= 0) {
            return ClaimResult.NO_SLOTS;
        }
        claims.put(key, id);
        byOwner.computeIfAbsent(id, k -> new HashSet<>()).add(key);

        PlayerClaims data = data(id);
        if (homeEnabled() && data.home() == null) {
            // Le premier chunk reclame devient automatiquement la base, comme demande.
            data.home(key);
        }
        save();
        return ClaimResult.OK;
    }

    public UnclaimResult unclaim(Player player) {
        if (!unclaimActionEnabled()) {
            return UnclaimResult.DISABLED;
        }
        Chunk chunk = player.getLocation().getChunk();
        String key = key(chunk);
        UUID owner = claims.get(key);
        if (owner == null) {
            return UnclaimResult.NOT_CLAIMED;
        }
        UUID id = player.getUniqueId();
        if (!owner.equals(id)) {
            return UnclaimResult.NOT_OWNER;
        }
        claims.remove(key);
        Set<String> owned = byOwner.get(id);
        if (owned != null) {
            owned.remove(key);
        }
        PlayerClaims data = data(id);
        if (key.equals(data.home())) {
            // Pas de reassignation automatique : le joueur redefinit sa base manuellement si besoin.
            data.home(null);
        }
        save();
        return UnclaimResult.OK;
    }

    /** true si le chunk courant du joueur est reclame par lui-meme. */
    public boolean ownsCurrentChunk(Player player) {
        return player.getUniqueId().equals(ownerOf(player.getLocation().getChunk()));
    }

    // ------------------------------------------------------------------ base ("chunk principal")

    public boolean setHome(Player player) {
        if (!homeEnabled()) {
            return false;
        }
        Chunk chunk = player.getLocation().getChunk();
        String key = key(chunk);
        if (!player.getUniqueId().equals(claims.get(key))) {
            return false;
        }
        data(player.getUniqueId()).home(key);
        save();
        return true;
    }

    public boolean isHome(Player player) {
        String home = data(player.getUniqueId()).home();
        return home != null && home.equals(key(player.getLocation().getChunk()));
    }

    public Location homeLocation(UUID id) {
        PlayerClaims data = players.get(id);
        if (data == null || data.home() == null) {
            return null;
        }
        return locationFromKey(data.home());
    }

    private Location locationFromKey(String key) {
        String[] parts = key.split(":", 3);
        if (parts.length != 3) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);
            int blockX = chunkX * 16 + 8;
            int blockZ = chunkZ * 16 + 8;
            int y = world.getHighestBlockYAt(blockX, blockZ) + 1;
            return new Location(world, blockX + 0.5, y, blockZ + 0.5);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ jeton d'emplacement (conversion)

    /** Convertit un emplacement libre en objet transportable (donne au joueur). true si succes. */
    public boolean convertSlotToItem(Player player) {
        UUID id = player.getUniqueId();
        if (available(id) <= 0) {
            return false;
        }
        PlayerClaims data = data(id);
        data.slotsTotal(data.slotsTotal() - 1);
        save();
        giveOrDrop(player, ClaimToken.create(plugin));
        return true;
    }

    /** Consomme un jeton dans l'inventaire du joueur et lui rend un emplacement. true si succes. */
    public boolean convertItemToSlot(Player player) {
        int slot = findTokenSlot(player);
        if (slot < 0) {
            return false;
        }
        // On ecrit explicitement via setItem (et updateInventory) plutot que de muter l'ItemStack
        // renvoye par getContents() en place : cette mutation directe ne se repercute pas toujours
        // cote client tant qu'un vrai setItem/update n'a pas ete declenche.
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.getAmount() <= 1) {
            player.getInventory().setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            player.getInventory().setItem(slot, stack);
        }
        player.updateInventory();
        adjustSlots(player.getUniqueId(), 1);
        return true;
    }

    private int findTokenSlot(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (ClaimToken.isToken(contents[i], plugin)) {
                return i;
            }
        }
        return -1;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack over : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), over);
        }
    }

    // ------------------------------------------------------------------ chargement / sauvegarde

    public void load() {
        players.clear();
        claims.clear();
        byOwner.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(uuidStr);
                    ConfigurationSection s = playersSection.getConfigurationSection(uuidStr);
                    if (s == null) {
                        continue;
                    }
                    PlayerClaims data = new PlayerClaims(id, s.getInt("slots", defaultSlots()));
                    data.home(s.getString("home", null));
                    players.put(id, data);
                } catch (IllegalArgumentException ignored) {
                    // UUID invalide : ligne ignoree.
                }
            }
        }

        ConfigurationSection claimsSection = config.getConfigurationSection("claims");
        if (claimsSection != null) {
            for (String chunkKey : claimsSection.getKeys(false)) {
                String ownerStr = claimsSection.getString(chunkKey);
                if (ownerStr == null) {
                    continue;
                }
                try {
                    UUID owner = UUID.fromString(ownerStr);
                    claims.put(chunkKey, owner);
                    byOwner.computeIfAbsent(owner, k -> new HashSet<>()).add(chunkKey);
                } catch (IllegalArgumentException ignored) {
                    // UUID invalide : ligne ignoree.
                }
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        Map<UUID, PlayerClaims> playersCopy = new HashMap<>(players);
        for (PlayerClaims data : playersCopy.values()) {
            String base = "players." + data.uuid();
            OfflinePlayer offline = Bukkit.getOfflinePlayer(data.uuid());
            if (offline.getName() != null) {
                config.set(base + ".name", offline.getName());
            }
            config.set(base + ".slots", data.slotsTotal());
            if (data.home() != null) {
                config.set(base + ".home", data.home());
            }
        }
        for (Map.Entry<String, UUID> entry : claims.entrySet()) {
            config.set("claims." + entry.getKey(), entry.getValue().toString());
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer claims.yml : " + e.getMessage());
        }
    }
}
