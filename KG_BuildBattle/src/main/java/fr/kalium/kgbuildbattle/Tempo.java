package fr.kalium.kgbuildbattle;

/** Durées de construction (LeKiwi06, 27/09/2026). La file publique est toujours en NORMAL. */
enum Tempo {
    FAST("Fast", 3),
    NORMAL("Normal", 5),
    LONGUE("Longue", 10),
    EXTRA("Extra", 30);

    final String nom;
    final int minutes;

    Tempo(String nom, int minutes) {
        this.nom = nom;
        this.minutes = minutes;
    }

    String libelle() {
        return nom + " (" + minutes + " min)";
    }

    static Tempo lire(String s) {
        for (Tempo t : values()) if (t.name().equalsIgnoreCase(s)) return t;
        return NORMAL;
    }
}
