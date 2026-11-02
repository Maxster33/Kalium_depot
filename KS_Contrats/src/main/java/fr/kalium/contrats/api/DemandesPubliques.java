package fr.kalium.contrats.api;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * KS_Contrats 1.1.0 : demandes publiques (cahier local Blocs_Cultures_Objets.md, partie 9, LeKiwi06 du 02/11/2026).
 * Service enregistré par KS_Contrats, utilisé par la boîte aux lettres (KS_Courrier) : une demande ne se livre que
 * depuis une boîte aux lettres.
 */
public interface DemandesPubliques {

    /** Liste des demandes ouvertes, avec le bouton « Livrer » (le joueur est à sa boîte) ; retour : bouton Retour. */
    void ouvrirDepuisBoite(Player joueur, Consumer<Player> retour);
}
