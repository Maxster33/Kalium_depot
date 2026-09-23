package fr.kalium.games.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Definition d'un mini-jeu (persistee dans minigames.yml). */
public final class Minigame {

    private final String id;
    private String display;
    private String description = "";
    private MinigameType type;
    private boolean enabled = true;
    private boolean publicEnabled = true;
    private boolean privateEnabled = true;
    private final List<String> kits = new ArrayList<>();
    private final Map<String, Object> settings = new LinkedHashMap<>();

    public Minigame(String id, String display, MinigameType type) {
        this.id = id;
        this.display = display;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public void display(String value) {
        this.display = value;
    }

    public String description() {
        return description;
    }

    public void description(String value) {
        this.description = value == null ? "" : value;
    }

    public MinigameType type() {
        return type;
    }

    public void type(MinigameType value) {
        this.type = value;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean value) {
        this.enabled = value;
    }

    public boolean publicEnabled() {
        return publicEnabled;
    }

    public void publicEnabled(boolean value) {
        this.publicEnabled = value;
    }

    public boolean privateEnabled() {
        return privateEnabled;
    }

    public void privateEnabled(boolean value) {
        this.privateEnabled = value;
    }

    public List<String> kits() {
        return kits;
    }

    public Map<String, Object> settings() {
        return settings;
    }

    /** Un mini-jeu est jouable si active, avec un moteur de jeu. */
    public boolean playable() {
        return enabled && type.playable();
    }

    public int getInt(String key, int fallback) {
        Object value = settings.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        SettingSpec spec = type.setting(key);
        if (spec != null && spec.def() instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    public boolean getBool(String key, boolean fallback) {
        Object value = settings.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        SettingSpec spec = type.setting(key);
        if (spec != null && spec.def() instanceof Boolean bool) {
            return bool;
        }
        return fallback;
    }

    public String getText(String key, String fallback) {
        Object value = settings.get(key);
        if (value instanceof String text) {
            return text;
        }
        SettingSpec spec = type.setting(key);
        if (spec != null && spec.def() instanceof String text) {
            return text;
        }
        return fallback;
    }
}
