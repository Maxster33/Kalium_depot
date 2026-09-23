package fr.kalium.games.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bibliotheque de kits. Un kit peut venir de PlayerKits2 (le fichier du kit est relu : une seule source
 * de verite avec /kit) ou etre enregistre depuis l'inventaire d'un moderateur.
 */
public final class KitLibrary {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitLibrary(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
    }

    // ------------------------------------------------------------------ acces

    public Collection<Kit> all() {
        return kits.values();
    }

    public Kit get(String id) {
        return id == null ? null : kits.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String id) {
        return get(id) != null;
    }

    public File pk2Folder() {
        String path = plugin.getConfig().getString("kits.playerkits2-folder", "PlayerKits2/kits");
        return new File(plugin.getDataFolder().getParentFile(), path);
    }

    /** Noms des kits PlayerKits2 disponibles (fichiers .yml du dossier). */
    public List<String> pk2Available() {
        List<String> names = new ArrayList<>();
        File[] files = pk2Folder().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                names.add(name.substring(0, name.length() - 4));
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    // ------------------------------------------------------------------ chargement / sauvegarde

    public void load() {
        kits.clear();
        boolean fresh = !file.exists();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Kit kit = new Kit(id, s.getString("display", id), s.getString("source", Kit.SOURCE_INVENTORY));
                if (kit.pk2Name() != null) {
                    loadPk2(kit);
                } else {
                    loadInventory(kit, s);
                }
                kits.put(id, kit);
            }
        }
        if (fresh) {
            bootstrapDefaults();
        }
    }

    /** Premier demarrage : importe les 4 kits du PvP Kit s'ils existent dans PlayerKits2. */
    private void bootstrapDefaults() {
        List<String> available = pk2Available();
        for (String wanted : List.of("GapSpeed2", "NetheriteP4", "MaceCrossbow", "SpamDistance")) {
            for (String name : available) {
                if (name.equalsIgnoreCase(wanted)) {
                    importPk2(name);
                }
            }
        }
        save();
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String base = "kits." + kit.id();
            config.set(base + ".display", kit.display());
            config.set(base + ".source", kit.source());
            if (kit.pk2Name() == null) {
                for (Map.Entry<Integer, ItemStack> entry : kit.slots().entrySet()) {
                    config.set(base + ".slots." + entry.getKey(), entry.getValue());
                }
                for (int i = 0; i < kit.armor().length; i++) {
                    if (kit.armor()[i] != null) {
                        config.set(base + ".armor." + i, kit.armor()[i]);
                    }
                }
                if (kit.offhand() != null) {
                    config.set(base + ".offhand", kit.offhand());
                }
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer kits.yml : " + e.getMessage());
        }
    }

    private void loadInventory(Kit kit, ConfigurationSection s) {
        kit.clearContents();
        ConfigurationSection slots = s.getConfigurationSection("slots");
        if (slots != null) {
            for (String key : slots.getKeys(false)) {
                ItemStack item = slots.getItemStack(key);
                try {
                    if (item != null) {
                        kit.slots().put(Integer.parseInt(key), item);
                    }
                } catch (NumberFormatException ignored) {
                    // cle invalide : ignoree
                }
            }
        }
        ConfigurationSection armor = s.getConfigurationSection("armor");
        if (armor != null) {
            for (int i = 0; i < 4; i++) {
                kit.armor()[i] = armor.getItemStack(String.valueOf(i));
            }
        }
        kit.offhand(s.getItemStack("offhand"));
    }

    /** Relit le fichier PlayerKits2 du kit et reconstruit son contenu. */
    private void loadPk2(Kit kit) {
        kit.clearContents();
        kit.broken(false);
        File f = new File(pk2Folder(), kit.pk2Name() + ".yml");
        if (!f.exists()) {
            kit.broken(true);
            plugin.getLogger().warning("Kit " + kit.id() + " : fichier PlayerKits2 introuvable (" + f.getName() + ").");
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) {
            kit.broken(true);
            return;
        }
        for (String key : items.getKeys(false)) {
            ItemStack item = config.getItemStack("items." + key + ".original");
            if (item == null || item.getType().isAir()) {
                continue;
            }
            place(kit, item, config.getBoolean("items." + key + ".offhand", false));
        }
    }

    private void place(Kit kit, ItemStack item, boolean offhand) {
        if (offhand && kit.offhand() == null) {
            kit.offhand(item);
            return;
        }
        EquipmentSlot slot = item.getType().getEquipmentSlot();
        int index = switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> -1;
        };
        if (index >= 0 && kit.armor()[index] == null) {
            kit.armor()[index] = item;
        } else {
            kit.auto().add(item);
        }
    }

    // ------------------------------------------------------------------ creation / suppression

    public Kit importPk2(String pk2Name) {
        String id = pk2Name.toLowerCase(Locale.ROOT);
        Kit kit = new Kit(id, pk2Name, Kit.SOURCE_PK2_PREFIX + pk2Name);
        loadPk2(kit);
        kits.put(id, kit);
        save();
        return kit;
    }

    public Kit createFromInventory(String id, String display, Player player) {
        return createFromInventory(id, display, player, item -> false);
    }

    /** exclude : objets a ignorer (objets verrouilles du hub, boussole...). */
    public Kit createFromInventory(String id, String display, Player player, java.util.function.Predicate<ItemStack> exclude) {
        String key = id.toLowerCase(Locale.ROOT);
        Kit kit = new Kit(key, display, Kit.SOURCE_INVENTORY);
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir() && !exclude.test(item)) {
                kit.slots().put(slot, item.clone());
            }
        }
        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < 4 && i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir() && !exclude.test(armor[i])) {
                kit.armor()[i] = armor[i].clone();
            }
        }
        ItemStack off = inventory.getItemInOffHand();
        if (!off.getType().isAir() && !exclude.test(off)) {
            kit.offhand(off.clone());
        }
        kits.put(key, kit);
        save();
        return kit;
    }

    public boolean delete(String id) {
        boolean removed = kits.remove(id.toLowerCase(Locale.ROOT)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    // ------------------------------------------------------------------ application

    /** Remplace tout l'inventaire du joueur par le kit. */
    public void apply(Player player, Kit kit) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(cloneAll(kit.armor()));
        inventory.setItemInOffHand(kit.offhand() == null ? new ItemStack(Material.AIR) : kit.offhand().clone());
        for (Map.Entry<Integer, ItemStack> entry : kit.slots().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().clone());
        }
        for (ItemStack item : kit.auto()) {
            Map<Integer, ItemStack> leftover = inventory.addItem(item.clone());
            if (!leftover.isEmpty()) {
                plugin.getLogger().fine("Kit " + kit.id() + " : inventaire plein pour " + player.getName());
            }
        }
    }

    private ItemStack[] cloneAll(ItemStack[] source) {
        ItemStack[] copy = Arrays.copyOf(source, source.length);
        for (int i = 0; i < copy.length; i++) {
            if (copy[i] != null) {
                copy[i] = copy[i].clone();
            }
        }
        return copy;
    }

    // ------------------------------------------------------------------ apercu

    /** Resume lisible du contenu du kit (objets regroupes), pour l'info-bulle du vote. */
    public List<Component> summary(Kit kit, int maxLines) {
        Map<Material, Integer> counts = new LinkedHashMap<>();
        Map<Material, ItemStack> samples = new LinkedHashMap<>();
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack armor : kit.armor()) {
            if (armor != null) {
                all.add(armor);
            }
        }
        if (kit.offhand() != null) {
            all.add(kit.offhand());
        }
        all.addAll(kit.slots().values());
        all.addAll(kit.auto());
        for (ItemStack item : all) {
            counts.merge(item.getType(), item.getAmount(), Integer::sum);
            samples.putIfAbsent(item.getType(), item);
        }
        List<Component> lines = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            if (shown >= maxLines) {
                lines.add(Component.text("… et " + (counts.size() - shown) + " autre(s) objet(s)", NamedTextColor.DARK_GRAY));
                break;
            }
            ItemStack sample = samples.get(entry.getKey());
            Component line = Component.text("• ", NamedTextColor.DARK_GRAY)
                    .append(Component.translatable(sample.translationKey(), NamedTextColor.GRAY));
            if (entry.getValue() > 1) {
                line = line.append(Component.text(" x" + entry.getValue(), NamedTextColor.WHITE));
            }
            lines.add(line);
            shown++;
        }
        if (kit.broken()) {
            lines.add(Component.text("Kit introuvable : vérifiez le fichier PlayerKits2.", NamedTextColor.RED));
        }
        return lines;
    }
}
