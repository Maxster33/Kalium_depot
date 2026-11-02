package fr.kalium.contrats.api;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * KS_Contrats 1.1.0 : dépôt d'objets dans la boîte aux lettres d'un compte (service enregistré par KS_Courrier).
 * Sert aux demandes publiques : objets livrés au demandeur, récompense en objets au livreur, récompense rendue à
 * l'annulation ou à l'expiration.
 */
public interface Livraison {

    /**
     * Dépose ces objets, comme un colis, dans le courrier de ce compte (compte de test compris), même s'il n'a pas de
     * boîte aux lettres en ce moment : il les relèvera quand il en aura une.
     *
     * @param nomCompte  pseudo du destinataire (historique)
     * @param expediteur nom affiché comme expéditeur (ex. « Demandes publiques »)
     * @param titre      objet du colis (ex. « Demande n°12 livrée par Kiwi »)
     */
    void deposer(UUID compte, String nomCompte, String expediteur, String titre, List<ItemStack> objets);
}
