package fr.kalium.kvplots;

import fr.kalium.kvplots.api.Taille;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Un plot réservé. Un grand plot occupe 4 cases à partir de (colonne, ligne) vers +x et +z. */
final class Plot {

    enum Etat { TRAVAUX, VALIDE }

    /** Travaux sur le terrain en cours (repris au démarrage s'ils ont été interrompus). */
    enum Chantier { AUCUN, FUSION, REMISE_A_ZERO, SUPPRESSION }

    final int id;
    final Taille taille;
    final int colonne, ligne;
    final UUID createur;
    final long creation;
    /** Éditeurs actuels. */
    final Set<UUID> editeurs = new LinkedHashSet<>();
    /** Tous les éditeurs passés et actuels (un ancien éditeur ne pourra pas voter). */
    final Set<UUID> historiqueEditeurs = new LinkedHashSet<>();
    Etat etat = Etat.TRAVAUX;
    Chantier chantier = Chantier.AUCUN;

    Plot(int id, Taille taille, int colonne, int ligne, UUID createur, long creation) {
        this.id = id;
        this.taille = taille;
        this.colonne = colonne;
        this.ligne = ligne;
        this.createur = createur;
        this.creation = creation;
    }

    List<Grille.Case> cases() {
        List<Grille.Case> liste = new ArrayList<>();
        int n = taille == Taille.GRAND ? 2 : 1;
        for (int dc = 0; dc < n; dc++) {
            for (int dl = 0; dl < n; dl++) liste.add(new Grille.Case(colonne + dc, ligne + dl));
        }
        return liste;
    }

    boolean peutConstruire(UUID joueur) {
        return createur.equals(joueur) || editeurs.contains(joueur);
    }
}
