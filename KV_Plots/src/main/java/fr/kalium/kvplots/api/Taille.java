package fr.kalium.kvplots.api;

/** Tailles de plot (pas de petit plot pour le moment). */
public enum Taille {
    MOYEN("moyen"),
    GRAND("grand");

    public final String nom;

    Taille(String nom) {
        this.nom = nom;
    }

    public static Taille depuis(String texte) {
        for (Taille t : values()) {
            if (t.nom.equalsIgnoreCase(texte)) return t;
        }
        return null;
    }
}
