package fr.kalium.games.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arene d'un mini-jeu. Les positions sont exprimees dans l'espace de creation
 * (coordonnees du monde ou le moderateur a construit) ; elles sont decalees a
 * l'instanciation selon l'emplacement de la partie.
 */
public final class Arena {

    private final String id;
    private final String minigameId;
    private String display;
    private boolean hasTemplate;
    /** Zone suggeree pour la capture : "monde;x1;y1;z1;x2;y2;z2" (optionnel). */
    private String suggestedArea = "";
    private final Map<String, Pos> points = new LinkedHashMap<>();
    private final Map<String, List<Pos>> lists = new LinkedHashMap<>();
    private final Map<String, List<ItemStack>> itemLists = new LinkedHashMap<>();
    private final Map<String, Object> settings = new LinkedHashMap<>();

    public Arena(String id, String minigameId, String display) {
        this.id = id;
        this.minigameId = minigameId;
        this.display = display;
    }

    public String id() {
        return id;
    }

    public String minigameId() {
        return minigameId;
    }

    public String display() {
        return display;
    }

    public void display(String value) {
        this.display = value;
    }

    public boolean hasTemplate() {
        return hasTemplate;
    }

    public void hasTemplate(boolean value) {
        this.hasTemplate = value;
    }

    public String suggestedArea() {
        return suggestedArea;
    }

    public void suggestedArea(String value) {
        this.suggestedArea = value == null ? "" : value;
    }

    public Map<String, Pos> points() {
        return points;
    }

    public Map<String, List<Pos>> lists() {
        return lists;
    }

    public Map<String, List<ItemStack>> itemLists() {
        return itemLists;
    }

    public Map<String, Object> settings() {
        return settings;
    }

    public Pos point(String key) {
        return points.get(key);
    }

    public List<Pos> list(String key) {
        return lists.getOrDefault(key, List.of());
    }

    public int getInt(String key, int fallback) {
        Object value = settings.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    /** Libelles des elements obligatoires encore manquants (vide = arene complete). */
    public List<String> missing(MinigameType type) {
        List<String> missing = new ArrayList<>();
        if (!hasTemplate) {
            missing.add("Modèle de l'arène (capture de la zone)");
        }
        for (PointSpec spec : type.points()) {
            if (!spec.required()) {
                continue;
            }
            boolean present = spec.list() ? !list(spec.key()).isEmpty() : point(spec.key()) != null;
            if (!present) {
                missing.add(spec.label());
            }
        }
        if (type == MinigameType.RUSH) {
            missing.addAll(RushLayout.missing(this)); // 1.11.0 : bases jaune/verte toutes deux completes ou vides
        }
        for (MinigameType.ItemListSpec spec : type.itemLists()) {
            List<ItemStack> items = itemLists.get(spec.key());
            if (items == null || items.isEmpty()) {
                missing.add(spec.label());
            }
        }
        return missing;
    }

    public boolean ready(MinigameType type) {
        return missing(type).isEmpty();
    }
}
