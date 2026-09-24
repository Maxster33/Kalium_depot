package fr.kalium.bingo.grid;

/**
 * Niveau de difficulte d'un objectif de grille Bingo (section 2 et section 1 etape 11 du
 * cahier des charges : "chaque item vaut un nombre de point selon sa difficulte, indique par
 * un fond de couleur sur l'item dans la carte : bleu = facile, jaune = moyen, orange =
 * difficile, rouge = extreme").
 *
 * La difficulte donne la couleur du nom de l'objectif dans le menu Objectifs (voir GameMenu). Elle
 * n'intervient PAS dans le score (1 point par objectif quelle que soit la difficulte, voir
 * BingoGame.score) : les points par difficulte ne sont pas encore definis (section 14 du cahier
 * des charges : "systeme de points" a definir avant l'ouverture).
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD,
    EXTREME
}
