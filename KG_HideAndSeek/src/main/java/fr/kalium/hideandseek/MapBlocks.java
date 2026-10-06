package fr.kalium.hideandseek;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocs que les hiders peuvent imiter, map par map (blocks.yml : identifiant de l'arene -> liste de blocs), regles par un
 * moderateur. Ce sont aussi les blocs qui coutent un demi-coeur au seeker qui se trompe.
 */
public final class MapBlocks {

    /** Motif de refus d'un bloc dans la liste d'une map. */
    public enum Refusal { NOT_BLOCK, NO_ITEM, FAMILY, PASSABLE }

    private final KGHideAndSeek plugin;
    private final File file;
    private final Map<String, List<Material>> byArena = new LinkedHashMap<>();

    public MapBlocks(KGHideAndSeek plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "blocks.yml");
    }

    public void load() {
        byArena.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection arenas = config.getConfigurationSection("arenas");
        if (arenas == null) {
            return;
        }
        for (String id : arenas.getKeys(false)) {
            List<Material> list = new ArrayList<>();
            for (String name : arenas.getStringList(id)) {
                Material material = Material.matchMaterial(name);
                if (material != null && material.isBlock() && !list.contains(material)) {
                    list.add(material);
                }
            }
            byArena.put(id, list);
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, List<Material>> entry : byArena.entrySet()) {
            List<String> names = new ArrayList<>();
            for (Material material : entry.getValue()) {
                names.add(material.name());
            }
            config.set("arenas." + entry.getKey(), names);
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer blocks.yml : " + e.getMessage());
        }
    }

    /** Blocs de la map (liste vide : map pas encore reglee). */
    public List<Material> of(String arenaId) {
        return byArena.getOrDefault(arenaId, List.of());
    }

    /** Ajoute le bloc a la map ; false s'il y etait deja. */
    public boolean add(String arenaId, Material material) {
        List<Material> list = byArena.computeIfAbsent(arenaId, id -> new ArrayList<>());
        if (list.contains(material)) {
            return false;
        }
        list.add(material);
        save();
        return true;
    }

    public void remove(String arenaId, Material material) {
        List<Material> list = byArena.get(arenaId);
        if (list != null && list.remove(material)) {
            save();
        }
    }

    /**
     * Le bloc peut-il entrer dans la liste d'une map ? Acceptes : tout bloc d'une seule case sur lequel on bute, plein ou
     * non (enclume, composteur, table d'enchantement, pot decoratif...). Refuses : dalles, escaliers, barrieres, portails,
     * murets, vitres et barreaux (ils se raccordent aux voisins), portes et lits (deux cases), blocs traversables (fleurs,
     * herbes, torches). Le bloc doit aussi exister en objet : le hider le porte sur la tete.
     * where : un endroit quelconque du monde (necessaire pour lire la forme du bloc). Renvoie null si le bloc est accepte.
     */
    public static Refusal refusal(Material material, Location where) {
        if (material == null || !material.isBlock() || material.isAir()) {
            return Refusal.NOT_BLOCK;
        }
        if (!material.isItem()) {
            return Refusal.NO_ITEM;
        }
        if (Tag.SLABS.isTagged(material) || Tag.STAIRS.isTagged(material) || Tag.FENCES.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material) || Tag.WALLS.isTagged(material) || Tag.DOORS.isTagged(material)
                || Tag.BEDS.isTagged(material) || Tag.BARS.isTagged(material) || material.name().endsWith("GLASS_PANE")) {
            return Refusal.FAMILY;
        }
        boolean passable;
        try {
            passable = material.createBlockData().getCollisionShape(where).getBoundingBoxes().isEmpty();
        } catch (RuntimeException e) {
            passable = !material.isSolid();
        }
        return passable ? Refusal.PASSABLE : null;
    }
}
