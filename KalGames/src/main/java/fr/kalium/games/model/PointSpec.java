package fr.kalium.games.model;

import java.util.List;

/**
 * Point d'arene a definir (une position unique ou une liste de positions).
 * itemList = liste d'objets (ex. butin) enregistree depuis l'inventaire du moderateur.
 *
 * group (1.11.0, optionnel) : les points d'un meme groupe sont reunis dans un sous-menu de l'arene
 * (ex. "Base bleue" pour Rush, ~11 points par base) au lieu d'etre tous listes a la suite.
 *
 * perPoint (1.18.0, listes seulement) : reglages propres a CHAQUE point de la liste (ex. temps ajoute, difficulte d'un
 * checkpoint du Parcours), edites depuis la fiche du point et enregistres avec l'arene. Vide = aucun.
 */
public record PointSpec(String key, String label, boolean list, boolean required, String help, String group,
                        List<SettingSpec> perPoint) {

    public static PointSpec single(String key, String label, boolean required, String help) {
        return new PointSpec(key, label, false, required, help, null, List.of());
    }

    public static PointSpec list(String key, String label, boolean required, String help) {
        return new PointSpec(key, label, true, required, help, null, List.of());
    }

    public PointSpec inGroup(String group) {
        return new PointSpec(key, label, list, required, help, group, perPoint);
    }

    /** 1.18.0 : reglages propres a chaque point de la liste. */
    public PointSpec withPointSettings(List<SettingSpec> settings) {
        return new PointSpec(key, label, list, required, help, group, List.copyOf(settings));
    }
}
