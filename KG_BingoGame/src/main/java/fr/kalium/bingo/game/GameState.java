package fr.kalium.bingo.game;

/**
 * Etats d'une partie de Bingo, tels que decrits dans la section 1 du cahier
 * des charges (to_do_list_Kal_Games_Bingo.txt).
 */
public enum GameState {
    /** Instances en cours de creation/preparation, pas encore annoncee comme disponible. */
    PREPARING,
    /** Partie annoncee disponible cote kal-games, fenetre de matchmaking en cours. */
    WAITING_FOR_PLAYERS,
    /** Compte a rebours de debut lance, joueurs teleportes, en attente du go. */
    STARTING,
    /** Partie en cours : validations, chronometre actif. */
    IN_PROGRESS,
    /** Partie terminee : plus aucune validation possible, resultat calcule. */
    FINISHED,
    /** Nettoyage/suppression des instances en cours. */
    CLEANING_UP
}
