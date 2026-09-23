package fr.kalium.bingo.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Genere une grille aleatoire a partir de la liste d'objectifs valides (section 2 : "le systeme
 * doit permettre de generer une grille aleatoire a partir d'une liste d'objectifs valides" /
 * "les objectifs doivent pouvoir etre differents d'une partie a l'autre si la generation
 * aleatoire est activee"). Seule la generation aleatoire SANS repetition est implementee ici -
 * aucun mode "grille fixe" n'a ete demande explicitement, pour ne pas anticiper une regle non
 * validee (section 14 : "objectifs fixes ou aleatoires" reste "a definir avant l'ouverture").
 */
public final class GridGenerator {

    /**
     * @param size taille de la grille (size x size cases)
     * @param pool liste des objectifs disponibles (objectives.yml, voir ObjectiveLibrary)
     * @throws IllegalStateException si le pool ne contient pas assez d'objectifs distincts
     */
    public BingoGrid generate(int size, List<Objective> pool) {
        int needed = size * size;
        if (pool.size() < needed) {
            throw new IllegalStateException("Pas assez d'objectifs disponibles pour une grille " + size + "x" + size
                    + " (" + needed + " necessaires, " + pool.size() + " disponibles dans objectives.yml).");
        }
        List<Objective> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random());
        List<GridCell> cells = new ArrayList<>(needed);
        for (int i = 0; i < needed; i++) {
            cells.add(new GridCell(shuffled.get(i)));
        }
        return new BingoGrid(size, cells);
    }
}
