package fr.kalium.rewardsgui;

import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 1.5.0 (LeKiwi06, 11/10/2026) : « j'aimerai qu'on puisse fusionner 2 coffres de récompenses dans l'enclume ce qui
 * additionnerait leurs valeurs, et couterait un nombre de point d'xp égale au nouveau montant en emeraude du coffres [...]
 * le coffre s'appellera "coffre de récompense fusionné", ça permettrait de stacker pleins de petits coffres pour essayer
 * d'avoir une grosse récompense sur la longueur ». Ses réponses : coût en points d'expérience, entre parenthèses sur le
 * bouton « Valider » ; « ne pas accepter les coffres déjà ouvert » ; contenu : « Nouveau tirage ».
 *
 * 1.7.0 (LeKiwi06, 25/10/2026) : le menu de fusion est remplacé par l'enclume en coffre de KS_Enclume 1.4.0 (accroupi +
 * clic sur une enclume), qui sert aussi d'enclume ordinaire ; les coffres y sont posés dans les cases. Cette classe ne
 * garde que les règles de la fusion, appelées par KS_Enclume (KSRewardsGUI.refusFusion, coutFusion, fusionner) :
 * - refusés : coffre déjà ouvert, coffre sans valeur moyenne connue, coffre inconnu de ce serveur ;
 * - coût : valeur du coffre fusionné (somme des deux valeurs moyennes), arrondie à l'unité supérieure, en points
 *   d'expérience (pas en niveaux), retirés par KS_Enclume ;
 * - les deux coffres disparaissent avec leur contenu ; le coffre fusionné reçoit un nouveau contenu, tiré comme une
 *   seule récompense de la valeur totale (Tables).
 */
final class Fusion {

    private final KSRewardsGUI plugin;
    private final Lang lang;
    private final Tables tables;

    Fusion(KSRewardsGUI plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.tables = new Tables(plugin);
    }

    /** 1.6.0 : tables de fusion.yml, aussi pour les coffres du parrainage (KSRewardsGUI.deposerCoffre). */
    Tables tables() {
        return tables;
    }

    static int cout(double valeur) {
        return (int) Math.max(1, Math.ceil(valeur - 1e-9));
    }

    double valeur(String a, String b) {
        return plugin.coffres().valeur(a) + plugin.coffres().valeur(b);
    }

    /** Pourquoi ces deux coffres ne peuvent pas être fusionnés, ou null s'ils le peuvent. */
    Component refus(String a, String b) {
        if (!tables.disponibles()) {
            return lang.c("fusion.indisponible", "<red>La fusion des coffres n'est pas disponible ici.");
        }
        if (a == null || b == null) {
            return lang.c("fusion.refus-autre", "<red>Un coffre de récompense ne se fusionne qu'avec un autre.");
        }
        if (a.equals(b)) {
            return lang.c("fusion.refus-meme", "<red>C'est deux fois le même coffre.");
        }
        Component refus = plugin.coffres().refusFusion(a);
        return refus != null ? refus : plugin.coffres().refusFusion(b);
    }

    /**
     * Fusionne deux coffres acceptés (les points d'expérience sont retirés par l'appelant) : renvoie le coffre fusionné,
     * ou null si la fusion est impossible (rien n'est alors changé).
     */
    ItemStack fusionner(Player joueur, String a, String b) {
        if (refus(a, b) != null) {
            return null;
        }
        double valeur = valeur(a, b);
        List<Map<String, Object>> contenu = tables.tirer(valeur);
        if (contenu.isEmpty()) {
            return null;
        }
        return plugin.coffres().fusionner(joueur, a, b, valeur, cout(valeur), contenu);
    }
}
