package fr.kalium.games.model;

/** Description d'un reglage editable dans le menu Parametres. */
public record SettingSpec(String key, String label, Kind kind, Object def, double min, double max, double step, String help) {

    public enum Kind { INT, BOOL, TEXT }

    public static SettingSpec integer(String key, String label, int def, int min, int max, String help) {
        return new SettingSpec(key, label, Kind.INT, def, min, max, 1, help);
    }

    public static SettingSpec bool(String key, String label, boolean def, String help) {
        return new SettingSpec(key, label, Kind.BOOL, def, 0, 0, 0, help);
    }

    public static SettingSpec text(String key, String label, String def, String help) {
        return new SettingSpec(key, label, Kind.TEXT, def, 0, 0, 0, help);
    }
}
