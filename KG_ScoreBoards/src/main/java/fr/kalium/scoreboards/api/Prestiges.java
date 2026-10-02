package fr.kalium.scoreboards.api;

import java.util.UUID;
import java.util.function.BiFunction;

/**
 * 1.8.0 (catégorie 4 « Récompenses ») : niveau de prestige affiché dans les classements, « Pseudo (prestige x) ». Le
 * niveau est fourni par KG_Rewards (prestige séparé par grille : chaque mini-jeu).
 */
public final class Prestiges {

    private static volatile BiFunction<String, UUID, Integer> fournisseur;

    private Prestiges() {
    }

    /** KG_Rewards : niveau de prestige d'un joueur pour un mini-jeu (0 : aucun). null retire le fournisseur. */
    public static void fournisseur(BiFunction<String, UUID, Integer> f) {
        fournisseur = f;
    }

    /** Nom affiché : « Pseudo (prestige x) » si le joueur a un prestige pour ce mini-jeu, sinon « Pseudo ». */
    public static String nom(String minigame, UUID joueur, String nom) {
        BiFunction<String, UUID, Integer> f = fournisseur;
        if (f == null || minigame == null || joueur == null) {
            return nom;
        }
        Integer niveau;
        try {
            niveau = f.apply(minigame, joueur);
        } catch (RuntimeException e) {
            return nom;
        }
        return niveau == null || niveau <= 0 ? nom : nom + " (prestige " + niveau + ")";
    }
}
