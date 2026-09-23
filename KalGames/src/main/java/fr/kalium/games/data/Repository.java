package fr.kalium.games.data;

import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import fr.kalium.games.model.Pos;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Persistance des mini-jeux (minigames.yml) et des arenes (arenas.yml). */
public final class Repository {

    private final JavaPlugin plugin;
    private final File minigamesFile;
    private final File arenasFile;
    private final Map<String, Minigame> minigames = new LinkedHashMap<>();
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public Repository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.minigamesFile = new File(plugin.getDataFolder(), "minigames.yml");
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
    }

    // ------------------------------------------------------------------ acces

    public Collection<Minigame> minigames() {
        return minigames.values();
    }

    public Minigame minigame(String id) {
        return id == null ? null : minigames.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Arena> arenas() {
        return arenas.values();
    }

    public Arena arena(String id) {
        return id == null ? null : arenas.get(id.toLowerCase(Locale.ROOT));
    }

    public List<Arena> arenasOf(String minigameId) {
        List<Arena> result = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.minigameId().equals(minigameId)) {
                result.add(arena);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ creation / suppression

    public Minigame createMinigame(String id, String display, MinigameType type) {
        Minigame minigame = new Minigame(id.toLowerCase(Locale.ROOT), display, type);
        minigames.put(minigame.id(), minigame);
        save();
        return minigame;
    }

    public void deleteMinigame(String id) {
        minigames.remove(id);
        arenas.values().removeIf(arena -> arena.minigameId().equals(id));
        save();
    }

    public Arena createArena(String id, String minigameId, String display) {
        Arena arena = new Arena(id.toLowerCase(Locale.ROOT), minigameId, display);
        arenas.put(arena.id(), arena);
        save();
        return arena;
    }

    public void deleteArena(String id) {
        arenas.remove(id);
        save();
    }

    // ------------------------------------------------------------------ chargement

    public void load(KitLibrary kitLibrary) {
        minigames.clear();
        arenas.clear();
        boolean fresh = !minigamesFile.exists();

        YamlConfiguration mg = YamlConfiguration.loadConfiguration(minigamesFile);
        ConfigurationSection mgSection = mg.getConfigurationSection("minigames");
        if (mgSection != null) {
            for (String id : mgSection.getKeys(false)) {
                ConfigurationSection s = mgSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                MinigameType type;
                try {
                    type = MinigameType.valueOf(s.getString("type", "PVP_KIT").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Mini-jeu " + id + " : type inconnu, ignore.");
                    continue;
                }
                Minigame minigame = new Minigame(id, s.getString("display", id), type);
                minigame.description(s.getString("description", ""));
                minigame.enabled(s.getBoolean("enabled", true));
                minigame.publicEnabled(s.getBoolean("public", true));
                minigame.privateEnabled(s.getBoolean("private", true));
                minigame.kits().addAll(s.getStringList("kits"));
                ConfigurationSection settings = s.getConfigurationSection("settings");
                if (settings != null) {
                    for (String key : settings.getKeys(false)) {
                        minigame.settings().put(key, settings.get(key));
                    }
                }
                minigames.put(id, minigame);
            }
        }

        YamlConfiguration ar = YamlConfiguration.loadConfiguration(arenasFile);
        ConfigurationSection arSection = ar.getConfigurationSection("arenas");
        if (arSection != null) {
            for (String id : arSection.getKeys(false)) {
                ConfigurationSection s = arSection.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Arena arena = new Arena(id, s.getString("minigame", ""), s.getString("display", id));
                arena.hasTemplate(s.getBoolean("template", false));
                arena.suggestedArea(s.getString("suggested-area", ""));
                ConfigurationSection points = s.getConfigurationSection("points");
                if (points != null) {
                    for (String key : points.getKeys(false)) {
                        Pos pos = Pos.parse(points.getString(key));
                        if (pos != null) {
                            arena.points().put(key, pos);
                        }
                    }
                }
                ConfigurationSection lists = s.getConfigurationSection("lists");
                if (lists != null) {
                    for (String key : lists.getKeys(false)) {
                        List<Pos> values = new ArrayList<>();
                        for (String text : lists.getStringList(key)) {
                            Pos pos = Pos.parse(text);
                            if (pos != null) {
                                values.add(pos);
                            }
                        }
                        arena.lists().put(key, values);
                    }
                }
                ConfigurationSection itemLists = s.getConfigurationSection("itemlists");
                if (itemLists != null) {
                    for (String key : itemLists.getKeys(false)) {
                        List<ItemStack> items = new ArrayList<>();
                        List<?> raw = itemLists.getList(key);
                        if (raw != null) {
                            for (Object o : raw) {
                                if (o instanceof ItemStack item) {
                                    items.add(item);
                                }
                            }
                        }
                        arena.itemLists().put(key, items);
                    }
                }
                ConfigurationSection settings = s.getConfigurationSection("settings");
                if (settings != null) {
                    for (String key : settings.getKeys(false)) {
                        arena.settings().put(key, settings.get(key));
                    }
                }
                arenas.put(id, arena);
            }
        }

        if (fresh) {
            bootstrapDefaults(kitLibrary);
        }
    }

    /**
     * Premier demarrage : recree le PvP Kit du datapack existant (kits, points de depart et tribune).
     * L'arene n'a pas encore de modele : il faut capturer la zone depuis le menu Parametres.
     */
    private void bootstrapDefaults(KitLibrary kitLibrary) {
        Minigame pvp = new Minigame("pvpkit", "<red><bold>PvP Kit", MinigameType.PVP_KIT);
        pvp.description("Affrontez d'autres joueurs avec un kit voté. La dernière équipe en vie gagne.");
        for (Kit kit : kitLibrary.all()) {
            pvp.kits().add(kit.id());
        }
        minigames.put(pvp.id(), pvp);

        // Points de l'arene tribune (departs A a D, gradins) tels que definis sur le serveur de test.
        Arena arena = new Arena("tribune", "pvpkit", "Arène Tribune");
        arena.points().put("stands", new Pos(988.49, 28.5, 1027.93, 177.9f, 29.8f));
        arena.points().put("spawn-a", new Pos(951.5, 1, 1001.5, -90, 0));
        arena.points().put("spawn-b", new Pos(1026.5, 2, 1001.5, 90, 0));
        arena.points().put("spawn-c", new Pos(989.5, 2, 1027.5, 180, 0));
        arena.points().put("spawn-d", new Pos(989.5, 2, 976.5, 0, 0));
        String world = plugin.getConfig().getString("hub.world", "Kal-Games");
        arena.suggestedArea(world + ";1051;-4;1049;932;41;954");
        arenas.put(arena.id(), arena);
        save();
    }

    // ------------------------------------------------------------------ sauvegarde

    public void save() {
        YamlConfiguration mg = new YamlConfiguration();
        for (Minigame minigame : minigames.values()) {
            String base = "minigames." + minigame.id();
            mg.set(base + ".display", minigame.display());
            mg.set(base + ".description", minigame.description());
            mg.set(base + ".type", minigame.type().name());
            mg.set(base + ".enabled", minigame.enabled());
            mg.set(base + ".public", minigame.publicEnabled());
            mg.set(base + ".private", minigame.privateEnabled());
            mg.set(base + ".kits", minigame.kits());
            for (Map.Entry<String, Object> entry : minigame.settings().entrySet()) {
                mg.set(base + ".settings." + entry.getKey(), entry.getValue());
            }
        }
        YamlConfiguration ar = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String base = "arenas." + arena.id();
            ar.set(base + ".minigame", arena.minigameId());
            ar.set(base + ".display", arena.display());
            ar.set(base + ".template", arena.hasTemplate());
            ar.set(base + ".suggested-area", arena.suggestedArea());
            for (Map.Entry<String, Pos> entry : arena.points().entrySet()) {
                ar.set(base + ".points." + entry.getKey(), entry.getValue().serialize());
            }
            for (Map.Entry<String, List<Pos>> entry : arena.lists().entrySet()) {
                List<String> values = new ArrayList<>();
                for (Pos pos : entry.getValue()) {
                    values.add(pos.serialize());
                }
                ar.set(base + ".lists." + entry.getKey(), values);
            }
            for (Map.Entry<String, List<ItemStack>> entry : arena.itemLists().entrySet()) {
                ar.set(base + ".itemlists." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Object> entry : arena.settings().entrySet()) {
                ar.set(base + ".settings." + entry.getKey(), entry.getValue());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            mg.save(minigamesFile);
            ar.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer minigames.yml / arenas.yml : " + e.getMessage());
        }
    }
}
