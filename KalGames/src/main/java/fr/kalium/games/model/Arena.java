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

    // ------------------------------------------------------------------ 1.18.0 : edition d'une liste de points

    /**
     * Reglages propres a chaque point d'une liste (voir PointSpec.perPoint), dans le meme ordre que la liste :
     * cle de la liste -> un dictionnaire par point. Toujours tenu a la meme taille que la liste par les methodes
     * ci-dessous (insertion, suppression...), pour que les reglages suivent leur point.
     */
    private final Map<String, List<Map<String, Object>>> pointSettings = new LinkedHashMap<>();

    public Map<String, List<Map<String, Object>>> pointSettings() {
        return pointSettings;
    }

    /** Reglages du point n° index (0 = premier) de la liste ; dictionnaire modifiable, vide si aucun. */
    public Map<String, Object> pointSettings(String key, int index) {
        List<Map<String, Object>> all = pointSettings.computeIfAbsent(key, k -> new ArrayList<>());
        while (all.size() <= index) {
            all.add(new LinkedHashMap<>());
        }
        return all.get(index);
    }

    /** Valeur d'un reglage du point n° index, ou null si non defini (l'appelant applique sa valeur par defaut). */
    public Object pointSetting(String key, int index, String setting) {
        List<Map<String, Object>> all = pointSettings.get(key);
        return all == null || index < 0 || index >= all.size() ? null : all.get(index).get(setting);
    }

    private List<Pos> editableList(String key) {
        return lists.computeIfAbsent(key, k -> new ArrayList<>());
    }

    /** Insere un point a la position index (0 = en tete ; taille = a la fin), avec des reglages vides. */
    public void insertPoint(String key, int index, Pos pos) {
        List<Pos> list = editableList(key);
        int at = Math.max(0, Math.min(index, list.size()));
        // Aligne d'abord la taille des reglages sur celle de la liste (anciennes arenes sans reglages).
        List<Map<String, Object>> all = pointSettings.computeIfAbsent(key, k -> new ArrayList<>());
        while (all.size() < list.size()) {
            all.add(new LinkedHashMap<>());
        }
        while (all.size() > list.size()) {
            all.remove(all.size() - 1);
        }
        list.add(at, pos);
        all.add(at, new LinkedHashMap<>());
    }

    /** Remplace la position du point n° index (ses reglages sont gardes). */
    public void replacePoint(String key, int index, Pos pos) {
        List<Pos> list = editableList(key);
        if (index >= 0 && index < list.size()) {
            list.set(index, pos);
        }
    }

    /** Supprime le point n° index et ses reglages. */
    public void removePoint(String key, int index) {
        List<Pos> list = editableList(key);
        if (index < 0 || index >= list.size()) {
            return;
        }
        list.remove(index);
        List<Map<String, Object>> all = pointSettings.get(key);
        if (all != null && index < all.size()) {
            all.remove(index);
        }
    }

    /** Vide la liste et ses reglages. */
    public void clearPoints(String key) {
        lists.remove(key);
        pointSettings.remove(key);
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
