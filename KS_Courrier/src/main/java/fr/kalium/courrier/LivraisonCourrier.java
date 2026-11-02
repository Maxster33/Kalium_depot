package fr.kalium.courrier;

import fr.kalium.contrats.api.Livraison;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/** KS_Contrats 1.1.0 : demandes publiques livrées dans les boîtes aux lettres. Chargé seulement si KS_Contrats est actif. */
final class LivraisonCourrier implements Livraison {

    private final Poste poste;

    LivraisonCourrier(Poste poste) {
        this.poste = poste;
    }

    @Override
    public void deposer(UUID compte, String nomCompte, String expediteur, String titre, List<ItemStack> objets) {
        poste.deposer(compte, nomCompte, expediteur, titre, objets);
    }
}
