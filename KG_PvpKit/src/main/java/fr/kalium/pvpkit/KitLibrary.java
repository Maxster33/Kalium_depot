package fr.kalium.pvpkit;

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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Bibliotheque des kits du PvP Kit (plugins/KG_PvpKit/kits.yml), reprise de KalGames 1.21.0.
 *
 * Premier demarrage : les kits de KalGames (plugins/KalGames/kits.yml) sont recopies. Ceux qui venaient de PlayerKits2
 * sont convertis (contenu relu une derniere fois dans le dossier de PlayerKits2) : PlayerKits2 n'est plus necessaire.
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

    // ------------------------------------------------------------------ chargement / sauvegarde

    public void load() {
        kits.clear();
        if (!file.exists()) {
            migrateFromKalGames();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s != null) {
                Kit kit = new Kit(id, s.getString("display", id));
                read(kit, s);
                kits.put(id, kit);
            }
        }
    }

    /** Premier demarrage : recopie les kits de KalGames (voir la description de la classe). */
    private void migrateFromKalGames() {
        File folder = plugin.getDataFolder().getParentFile();
        File old = new File(folder, "KalGames/kits.yml");
        if (!old.exists()) {
            plugin.getLogger().info("Aucun kit a reprendre de KalGames : bibliotheque vide.");
            save();
            return;
        }
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(new File(folder, "KalGames/config.yml"));
        File pk2Folder = new File(folder, oldConfig.getString("kits.playerkits2-folder", "PlayerKits2/kits"));
        YamlConfiguration config = YamlConfiguration.loadConfiguration(old);
        ConfigurationSection section = config.getConfigurationSection("kits");
        int copied = 0;
        List<String> lost = new ArrayList<>();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Kit kit = new Kit(id, s.getString("display", id));
                String source = s.getString("source", "inventory");
                if (source.startsWith("playerkits2:")) {
                    readPk2(kit, new File(pk2Folder, source.substring("playerkits2:".length()) + ".yml"));
                } else {
                    read(kit, s);
                }
                if (kit.empty()) {
                    lost.add(id);
                    continue;
                }
                kits.put(id, kit);
                copied++;
            }
        }
        save();
        plugin.getLogger().info(copied + " kit(s) repris de KalGames (plugins/KalGames/kits.yml)."
                + (lost.isEmpty() ? "" : " Kit(s) vide(s) ou introuvable(s), non repris : " + String.join(", ", lost) + "."));
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String base = "kits." + kit.id();
            config.set(base + ".display", kit.display());
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
            if (!kit.auto().isEmpty()) {
                config.set(base + ".auto", kit.auto());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer kits.yml : " + e.getMessage());
        }
    }

    private void read(Kit kit, ConfigurationSection s) {
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
        List<?> auto = s.getList("auto");
        if (auto != null) {
            for (Object o : auto) {
                if (o instanceof ItemStack item) {
                    kit.auto().add(item);
                }
            }
        }
    }

    /** Ancien kit PlayerKits2 (KalGames) : contenu lu une derniere fois dans son fichier. */
    private void readPk2(Kit kit, File f) {
        if (!f.exists()) {
            plugin.getLogger().warning("Kit " + kit.id() + " : fichier PlayerKits2 introuvable (" + f.getPath() + ").");
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            ItemStack item = config.getItemStack("items." + key + ".original");
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (config.getBoolean("items." + key + ".offhand", false) && kit.offhand() == null) {
                kit.offhand(item);
                continue;
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
    }

    // ------------------------------------------------------------------ creation / suppression

    /** exclude : objets a ignorer (objets verrouilles du hub, boussole...). */
    public Kit createFromInventory(String id, String display, Player player, Predicate<ItemStack> exclude) {
        String key = id.toLowerCase(Locale.ROOT);
        Kit kit = new Kit(key, display);
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

    /** Remplace tout l'inventaire du joueur par le kit (deja declasse si besoin, voir Downgrade). */
    public void apply(Player player, Kit kit) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armor[i] = kit.armor()[i] == null ? null : kit.armor()[i].clone();
        }
        inventory.setArmorContents(armor);
        inventory.setItemInOffHand(kit.offhand() == null ? new ItemStack(Material.AIR) : kit.offhand().clone());
        for (Map.Entry<Integer, ItemStack> entry : kit.slots().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().clone());
        }
        for (ItemStack item : kit.auto()) {
            if (!inventory.addItem(item.clone()).isEmpty()) {
                plugin.getLogger().fine("Kit " + kit.id() + " : inventaire plein pour " + player.getName());
            }
        }
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
        return lines;
    }
}
