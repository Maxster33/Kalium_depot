package fr.kalium.bingo.grid;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Charge la liste d'objectifs utilisables pour generer les grilles Bingo depuis un fichier
 * externe (objectives.yml, dans le dossier de donnees du plugin), PAS depuis config.yml ni le
 * code du plugin (section 2 : "la grille doit etre configurable sans devoir modifier le plugin
 * principal", "prevoir un systeme permettant de modifier facilement les listes d'items").
 *
 * Le fichier livre par defaut avec le plugin ne contient que des exemples clairement marques
 * comme tels (voir son en-tete) : la liste finale n'est pas encore validee (section 13 du
 * cahier des charges) et ne doit pas etre inventee ici - seul le format/systeme est fourni.
 */
public final class ObjectiveLibrary {

    private final JavaPlugin plugin;
    private final File file;
    private List<Objective> objectives = List.of();

    public ObjectiveLibrary(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "objectives.yml");
    }

    /** Copie le fichier d'exemple par defaut si absent, puis (re)charge la liste depuis le disque. */
    public void load() {
        if (!file.exists()) {
            plugin.saveResource("objectives.yml", false);
        }
        reload();
    }

    /** Recharge la liste depuis le disque sans toucher au fichier (pour /bingoadmin grid reload). */
    public void reload() {
        Logger logger = plugin.getLogger();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<?> rawList = yaml.getList("objectives");
        List<Objective> loaded = new ArrayList<>();
        if (rawList == null) {
            logger.warning("[KG_BingoGame] objectives.yml : cle 'objectives' absente ou vide.");
            this.objectives = List.of();
            return;
        }
        int index = 0;
        for (Object raw : rawList) {
            index++;
            ConfigurationSection section = toSection(raw);
            if (section == null) {
                logger.warning("[KG_BingoGame] objectives.yml : entree #" + index + " ignoree (format invalide).");
                continue;
            }
            Objective objective = parseEntry(section, index, logger);
            if (objective != null) {
                loaded.add(objective);
            }
        }
        this.objectives = Collections.unmodifiableList(loaded);
        logger.info("[KG_BingoGame] " + this.objectives.size() + " objectif(s) charge(s) depuis objectives.yml.");
    }

    /** SnakeYAML rend generalement chaque entree de liste comme un Map (pas directement une ConfigurationSection). */
    private ConfigurationSection toSection(Object raw) {
        if (raw instanceof ConfigurationSection direct) {
            return direct;
        }
        if (raw instanceof java.util.Map<?, ?> map) {
            YamlConfiguration section = new YamlConfiguration();
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                section.set(String.valueOf(entry.getKey()), entry.getValue());
            }
            return section;
        }
        return null;
    }

    private Objective parseEntry(ConfigurationSection section, int index, Logger logger) {
        String materialName = section.getString("material");
        if (materialName == null || materialName.isBlank()) {
            logger.warning("[KG_BingoGame] objectives.yml : entree #" + index + " ignoree (champ 'material' manquant).");
            return null;
        }
        Material material = Material.matchMaterial(materialName.trim());
        if (material == null) {
            logger.warning("[KG_BingoGame] objectives.yml : entree #" + index + " ignoree (item inconnu : '" + materialName + "').");
            return null;
        }
        int quantity = Math.max(1, section.getInt("quantity", 1));
        String difficultyName = section.getString("difficulty", "MEDIUM");
        Difficulty difficulty;
        try {
            difficulty = Difficulty.valueOf(difficultyName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("[KG_BingoGame] objectives.yml : entree #" + index + " (" + materialName
                    + ") - difficulte '" + difficultyName + "' inconnue, MEDIUM utilise par defaut.");
            difficulty = Difficulty.MEDIUM;
        }
        String condition = section.getString("condition", "");
        try {
            return new Objective(material, quantity, difficulty, condition);
        } catch (IllegalArgumentException e) {
            logger.warning("[KG_BingoGame] objectives.yml : entree #" + index + " ignoree (" + e.getMessage() + ").");
            return null;
        }
    }

    public List<Objective> all() {
        return objectives;
    }

    public int size() {
        return objectives.size();
    }
}
