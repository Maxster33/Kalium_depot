package fr.kalium.kvplots;

/** Tailles de plot (pas de petit plot pour le moment). */
enum Taille {
    MOYEN("moyen"),
    GRAND("grand");

    final String nom;

    Taille(String nom) {
        this.nom = nom;
    }

    static Taille depuis(String texte) {
        for (Taille t : values()) {
            if (t.nom.equalsIgnoreCase(texte)) return t;
        }
        return null;
    }
}
