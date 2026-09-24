package fr.kalium.scoreboards;

import net.kyori.adventure.text.Component;

/**
 * Un classement (1.0.0) : un mini-jeu de KalGames, et bientot les classements du Bingo. Fourni par le plugin qui
 * l'utilise (voir KGScoreBoards.addCategories) : KG_ScoreBoards ne connait pas les jeux eux-memes.
 *
 * @param id   identifiant stable (cle dans stats.yml, boards.yml et les archives)
 * @param name nom affiche
 * @param kind temps affiche a cote des points
 */
public record Category(String id, Component name, Kind kind) {

    /** Temps affiche a cote des points (comme dans KalGames 1.13.0, selon le type de mini-jeu). */
    public enum Kind {
        /** Points seulement. */
        POINTS,
        /** Points + meilleur temps de course (parcours). */
        TIME,
        /** Points + meilleur temps sur 1 tour, et classement des temps sur 1 tour (course de bateau). */
        LAP
    }
}
