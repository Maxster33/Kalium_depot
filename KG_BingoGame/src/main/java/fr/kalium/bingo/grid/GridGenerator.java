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
     * 0.3.0 - composition choisie par l'hote (demande explicite de LeKiwi06, 24/09/2026 : "il peut choisir pour
     * chaque categorie combien il en veut dans la composition") : tire au hasard, sans repetition, le nombre
     * demande d'objectifs de chaque difficulte, puis les place au hasard sur la grille. Si la composition ne
     * correspond pas au nombre de cases (reglage incoherent), la composition par defaut est utilisee.
     *
     * @throws IllegalStateException si objectives.yml ne contient pas assez d'objectifs d'une difficulte
     */
    public BingoGrid generate(int size, List<Objective> pool, fr.kalium.bingo.game.BingoSettings settings) {
        int needed = size * size;
        if (settings == null || settings.total() != needed) {
            settings = fr.kalium.bingo.game.BingoSettings.defaults();
        }
        if (settings.total() != needed) {
            return generate(size, pool); // grille d'une autre taille que 5x5 : tirage sans composition
        }
        Random random = new Random();
        List<Objective> chosen = new ArrayList<>(needed);
        pick(chosen, pool, Difficulty.EASY, settings.easy(), random);
        pick(chosen, pool, Difficulty.MEDIUM, settings.medium(), random);
        pick(chosen, pool, Difficulty.HARD, settings.hard(), random);
        pick(chosen, pool, Difficulty.EXTREME, settings.extreme(), random);
        Collections.shuffle(chosen, random);
        List<GridCell> cells = new ArrayList<>(needed);
        for (Objective objective : chosen) {
            cells.add(new GridCell(objective));
        }
        return new BingoGrid(size, cells);
    }

    private static void pick(List<Objective> into, List<Objective> pool, Difficulty difficulty, int count, Random random) {
        if (count <= 0) {
            return;
        }
        List<Objective> candidates = new ArrayList<>();
        for (Objective objective : pool) {
            if (objective.difficulty() == difficulty) {
                candidates.add(objective);
            }
        }
        if (candidates.size() < count) {
            throw new IllegalStateException("Pas assez d'objectifs de difficulte " + difficulty.label() + " dans objectives.yml ("
                    + count + " demandes, " + candidates.size() + " disponibles).");
        }
        Collections.shuffle(candidates, random);
        into.addAll(candidates.subList(0, count));
    }

    /**
     * Tirage sans composition (avant 0.3.0) : {@code size} x {@code size} objectifs au hasard, toutes difficultes.
     *
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
