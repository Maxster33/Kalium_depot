package fr.kalium.economy.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 1.12.0 (LeKiwi06, 27/10/2026, /contrat de KS_Contrats : « on va aussi modifier le /echange pour pouvoir sélectionner
 * un contrat quand on échange ») : contrats que deux joueurs peuvent livrer par /echange.
 *
 * Déclaré par KS_Contrats dans le ServicesManager de Bukkit ; sans lui, /echange n'a pas de bouton « Contrat ».
 * Quand un contrat est choisi dans un échange, chacun ne peut poser que les objets et les points que sa partie doit
 * encore livrer à l'autre (LeKiwi06 : « pour éviter les petits malins qui profiteraient d'un échange banal pour
 * discrètement valider une clause de contrat »).
 */
public interface ContratsEchange {

    /** Contrats actifs que ces deux joueurs ont ensemble, chacun d'un côté différent (identifiants). */
    List<String> communs(Player a, Player b);

    /** Objet de menu du contrat vu par ce joueur (son logo, son nom, ce qui reste à livrer) ; null : plus valable. */
    ItemStack icone(String contrat, Player pour);

    /**
     * Objets que le donneur pose pour le receveur, triés : acceptés (dus par sa partie, dans ce qui reste à livrer) et
     * refusés (le reste, quantités en trop comprises). null : le contrat n'est plus valable entre eux.
     */
    Tri trier(String contrat, Player donneur, Player receveur, List<ItemStack> objets);

    /** Points que la partie du donneur doit encore au receveur ; -1 : le contrat n'est plus valable entre eux. */
    long pointsMax(String contrat, Player donneur, Player receveur);

    /** Échange conclu : ces objets et ces points ont été livrés au titre du contrat. */
    void livrer(String contrat, Player donneur, Player receveur, List<ItemStack> objets, long points);

    record Tri(List<ItemStack> acceptes, List<ItemStack> refuses) {
    }
}
