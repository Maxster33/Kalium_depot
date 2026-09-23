package fr.kalium.bingo.grid;

/**
 * Niveau de difficulte d'un objectif de grille Bingo (section 2 et section 1 etape 11 du
 * cahier des charges : "chaque item vaut un nombre de point selon sa difficulte, indique par
 * un fond de couleur sur l'item dans la carte : bleu = facile, jaune = moyen, orange =
 * difficile, rouge = extreme").
 *
 * Seule la difficulte elle-meme est geree ici (donnee sur l'objectif). Le calcul des points et
 * l'affichage de la couleur sur la carte custom relevent d'etapes ulterieures de l'ordre de
 * priorite (section 16 : validation/interface pour la couleur, fin de partie/classement pour
 * le systeme de points) - pas encore implementes, volontairement, pour ne pas anticiper des
 * regles de scoring non validees (section 14 : "systeme de points" a definir avant l'ouverture).
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD,
    EXTREME
}
