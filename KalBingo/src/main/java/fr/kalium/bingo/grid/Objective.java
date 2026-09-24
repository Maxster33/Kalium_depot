package fr.kalium.bingo.grid;

import org.bukkit.Material;

/**
 * Un objectif de grille Bingo, tel que decrit section 2 du cahier des charges : "la grille doit
 * permettre de definir pour chaque case : l'item demande, eventuellement sa quantite, les
 * conditions particulieres de validation si elles sont necessaires".
 *
 * Le champ {@code condition} est une donnee libre, affichee dans le menu Objectifs mais PAS
 * interpretee par la validation automatique (ObjectiveValidationTask ne verifie que l'item et la
 * quantite).
 */
public record Objective(Material material, int quantity, Difficulty difficulty, String condition) {

    public Objective {
        if (material == null) {
            throw new IllegalArgumentException("Un objectif doit avoir un item (material).");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("La quantite d'un objectif doit etre >= 1.");
        }
        if (difficulty == null) {
            difficulty = Difficulty.MEDIUM;
        }
        if (condition == null) {
            condition = "";
        }
    }

    public boolean hasCondition() {
        return !condition.isBlank();
    }
}
