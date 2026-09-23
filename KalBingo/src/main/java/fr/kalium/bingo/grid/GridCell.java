package fr.kalium.bingo.grid;

/**
 * Une case de la grille Bingo : un objectif assigne. Le champ {@code validated} existe des
 * maintenant car il fait partie integrante de la notion de "case de grille", mais RIEN ne le
 * fait encore passer a {@code true} : la detection automatique de validation (section 3 du
 * cahier des charges) est une etape ulterieure de l'ordre de priorite (section 16), pas encore
 * implementee. Ce champ reste donc inerte pour l'instant.
 */
public final class GridCell {

    private final Objective objective;
    private boolean validated = false;

    public GridCell(Objective objective) {
        this.objective = objective;
    }

    public Objective getObjective() {
        return objective;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }
}
