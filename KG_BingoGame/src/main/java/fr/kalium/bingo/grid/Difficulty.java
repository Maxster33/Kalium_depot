package fr.kalium.bingo.grid;

/**
 * Niveau de difficulte d'un objectif de grille Bingo (section 2 et section 1 etape 11 du
 * cahier des charges : "chaque item vaut un nombre de point selon sa difficulte, indique par
 * un fond de couleur sur l'item dans la carte : bleu = facile, jaune = moyen, orange =
 * difficile, rouge = extreme").
 *
 * La difficulte donne la couleur du nom de l'objectif dans le menu Objectifs (voir GameMenu) et, depuis
 * la 0.3.0, ses points (voir ci-dessous).
 */
public enum Difficulty {
    // 0.3.0 - bareme demande par LeKiwi06 (24/09/2026) : valeur de l'objectif, bonus de la 1re equipe a le
    // valider, bonus de victoire (ajoute sur chaque objectif valide par l'equipe gagnante). Les bonus sont
    // ajoutes AVANT les coefficients des bingos (voir fr.kalium.bingo.score.ScoreEngine).
    // 0.8.0 : bareme double (equilibrage des jeux, LeKiwi06 26/09/2026 : 30 min a fond = autant de points qu'a la
    // course de bateau ; mesure : le Bingo rapportait environ 2 fois moins).
    EASY("Facile", 2, 0, 2),
    MEDIUM("Normal", 6, 2, 4),
    HARD("Difficile", 10, 4, 6),
    EXTREME("Extrême", 20, 6, 10);

    private final String label;
    private final int points;
    private final int firstBonus;
    private final int winBonus;

    Difficulty(String label, int points, int firstBonus, int winBonus) {
        this.label = label;
        this.points = points;
        this.firstBonus = firstBonus;
        this.winBonus = winBonus;
    }

    /** Nom affiche en jeu. */
    public String label() {
        return label;
    }

    public int points() {
        return points;
    }

    public int firstBonus() {
        return firstBonus;
    }

    public int winBonus() {
        return winBonus;
    }

    /** true pour les objectifs difficiles et extremes (bingo "uniquement difficile / extreme"). */
    public boolean isHardOrAbove() {
        return this == HARD || this == EXTREME;
    }
}
