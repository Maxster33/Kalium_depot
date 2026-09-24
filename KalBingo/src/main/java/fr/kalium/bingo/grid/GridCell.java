package fr.kalium.bingo.grid;

/**
 * Une case de la grille Bingo : un objectif assigne. Le champ {@code validated} n'est PAS utilise :
 * la validation se fait par equipe (BingoGame.teamProgress, voir ObjectiveValidationTask), un
 * booleen unique par case ne pouvant pas representer plusieurs equipes.
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
