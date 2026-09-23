package fr.kalium.bingo.grid;

import java.util.List;

/**
 * Une grille Bingo generee : carree, {@code size} x {@code size} cases (section 2 : "la taille
 * de la grille doit etre configurable", voir config.yml "grid.size").
 *
 * UNE SEULE grille par partie (BingoGame), partagee par toutes les equipes/instances. La section
 * 14 du cahier des charges liste "generation identique ou differente entre les joueurs" parmi
 * les points "a definir avant l'ouverture" : ce choix (grille commune) est une hypothese de
 * travail raisonnable (format Bingo competitif standard : toutes les equipes visent la meme
 * grille) mais N'EST PAS une decision finale validee - a confirmer/ajuster si besoin.
 */
public final class BingoGrid {

    private final int size;
    private final List<GridCell> cells; // ordre ligne par ligne : index = row * size + col

    public BingoGrid(int size, List<GridCell> cells) {
        if (size < 1) {
            throw new IllegalArgumentException("Taille de grille invalide : " + size);
        }
        if (cells.size() != size * size) {
            throw new IllegalArgumentException(
                    "Nombre de cases (" + cells.size() + ") incoherent avec la taille " + size + "x" + size);
        }
        this.size = size;
        this.cells = List.copyOf(cells);
    }

    public int getSize() {
        return size;
    }

    public List<GridCell> getCells() {
        return cells;
    }

    public GridCell cellAt(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IndexOutOfBoundsException("Case (" + row + "," + col + ") hors grille " + size + "x" + size);
        }
        return cells.get(row * size + col);
    }
}
