package fr.kalium.scoreboards.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Textes du plugin (format MiniMessage). Chaque texte est declare dans le code avec sa valeur par
 * defaut ; lang.yml se remplit automatiquement et peut etre edite pour changer les messages.
 */
public final class Lang {

    private final JavaPlugin plugin;
    private final File file;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private YamlConfiguration config;
    private boolean dirty;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lang.yml");
        load();
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
        dirty = false;
    }

    public void saveIfNeeded() {
        if (!dirty) {
            return;
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer lang.yml : " + e.getMessage());
        }
    }

    public String raw(String key, String def) {
        String value = config.getString(key);
        if (value == null) {
            config.set(key, def);
            dirty = true;
            return def;
        }
        return value;
    }

    /** Texte MiniMessage avec placeholders : paires nom, valeur (Component ou tout autre objet). */
    public Component c(String key, String def, Object... pairs) {
        return mm.deserialize(raw(key, def), resolvers(pairs));
    }

    /** Texte MiniMessage libre (non declare dans lang.yml), ex. un nom d'affichage saisi par un moderateur. */
    public Component parse(String text) {
        return mm.deserialize(text == null ? "" : text);
    }

    private TagResolver resolvers(Object... pairs) {
        List<TagResolver> resolvers = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String name = String.valueOf(pairs[i]);
            Object value = pairs[i + 1];
            if (value instanceof Component component) {
                resolvers.add(Placeholder.component(name, component));
            } else {
                resolvers.add(Placeholder.unparsed(name, String.valueOf(value)));
            }
        }
        return TagResolver.resolver(resolvers);
    }
}
