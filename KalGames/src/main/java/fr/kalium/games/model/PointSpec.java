package fr.kalium.games.model;

/**
 * Point d'arene a definir (une position unique ou une liste de positions).
 * itemList = liste d'objets (ex. butin) enregistree depuis l'inventaire du moderateur.
 *
 * group (1.11.0, optionnel) : les points d'un meme groupe sont reunis dans un sous-menu de l'arene
 * (ex. "Base bleue" pour Rush, ~11 points par base) au lieu d'etre tous listes a la suite.
 */
public record PointSpec(String key, String label, boolean list, boolean required, String help, String group) {

    public static PointSpec single(String key, String label, boolean required, String help) {
        return new PointSpec(key, label, false, required, help, null);
    }

    public static PointSpec list(String key, String label, boolean required, String help) {
        return new PointSpec(key, label, true, required, help, null);
    }

    public PointSpec inGroup(String group) {
        return new PointSpec(key, label, list, required, help, group);
    }
}
