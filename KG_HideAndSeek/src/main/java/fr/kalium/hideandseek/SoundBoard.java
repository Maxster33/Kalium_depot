package fr.kalium.hideandseek;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Soundboard des hiders : liste de sons commune a toutes les maps (sounds.yml, reglable par un moderateur).
 * 0.2.0 : plus de sons choisis dans la barre d'objets (un seul objet ouvre le menu des sons) ; le fichier favorites.yml
 * de la 0.1.0 n'est plus lu.
 */
public final class SoundBoard {

    /** Un son : identifiant Minecraft (ex. entity.cat.ambient), nom affiche, objet qui sert d'icone. */
    public record Entry(String key, String label, Material icon) {
    }

    private final KGHideAndSeek plugin;
    private final File file;
    private final List<Entry> entries = new ArrayList<>();

    public SoundBoard(KGHideAndSeek plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sounds.yml");
    }

    public void load() {
        entries.clear();
        if (!file.exists()) {
            entries.addAll(defaults());
            save();
        } else {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (Map<?, ?> map : config.getMapList("sounds")) {
                Object key = map.get("key");
                Object label = map.get("label");
                Object icon = map.get("icon");
                if (key == null || key.toString().isBlank()) {
                    continue;
                }
                Material material = icon == null ? null : Material.matchMaterial(icon.toString());
                if (material == null || material.isAir() || !material.isItem()) {
                    material = Material.NOTE_BLOCK;
                }
                entries.add(new Entry(key.toString(), label == null ? key.toString() : label.toString(), material));
            }
        }
    }

    /** Sons livres avec le jeu (demande de LeKiwi06, 06/10/2026). */
    private static List<Entry> defaults() {
        return List.of(
                new Entry("entity.cat.ambient", "Chat", Material.STRING),
                new Entry("entity.pig.ambient", "Cochon", Material.PORKCHOP),
                new Entry("entity.chicken.ambient", "Poule", Material.FEATHER),
                new Entry("entity.villager.ambient", "Villageois", Material.EMERALD),
                new Entry("entity.creeper.primed", "Creeper", Material.GUNPOWDER),
                new Entry("entity.enderman.ambient", "Enderman", Material.ENDER_PEARL),
                new Entry("entity.ghast.ambient", "Ghast", Material.GHAST_TEAR),
                new Entry("block.bell.use", "Cloche", Material.BELL),
                new Entry("entity.player.burp", "Rot", Material.COOKED_BEEF),
                new Entry("block.wooden_door.open", "Porte", Material.OAK_DOOR),
                new Entry("entity.firework_rocket.blast", "Feu d'artifice", Material.FIREWORK_ROCKET),
                new Entry("block.anvil.land", "Enclume", Material.ANVIL));
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Entry entry : entries) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", entry.key());
            map.put("label", entry.label());
            map.put("icon", entry.icon().name());
            list.add(map);
        }
        config.set("sounds", list);
        write(config, file);
    }

    private void write(YamlConfiguration config, File target) {
        try {
            plugin.getDataFolder().mkdirs();
            config.save(target);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer " + target.getName() + " : " + e.getMessage());
        }
    }

    public List<Entry> all() {
        return entries;
    }

    public Entry get(String key) {
        for (Entry entry : entries) {
            if (entry.key().equals(key)) {
                return entry;
            }
        }
        return null;
    }

    public void add(Entry entry) {
        entries.add(entry);
        save();
    }

    public void remove(String key) {
        entries.removeIf(entry -> entry.key().equals(key));
        save();
    }
}
