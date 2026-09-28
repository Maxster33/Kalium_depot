package fr.kalium.menu.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * 2.4.0 - un objet d'interface de la barre d'objets (boussole, comparateur, etoile de KG_Menu ou de KV_Menu...), connu
 * de KLM_Menu pour la commande /menu (demande de LeKiwi06, 28/09/2026) :
 * - « /menu » ouvre l'interface du serveur ; s'il y en a plusieurs, un menu permet de choisir ;
 * - « /menu off » retire tous ces objets de la barre, « /menu on » les redonne.
 *
 * Declaration, au demarrage du plugin :
 * <pre>
 * getServer().getServicesManager().register(InterfaceItem.class, item, this, ServicePriority.Normal);
 * </pre>
 * Le plugin qui donne lui-meme son objet doit s'abstenir quand le joueur a masque ses objets
 * ({@code KlmMenu.itemsHidden(player)}).
 */
public interface InterfaceItem {

    /** Plugin qui fournit l'objet. */
    Plugin owner();

    /** Identifiant stable (ex. "navigation", "informations", "kalgames"). */
    String id();

    /** Nom affiche dans le menu de choix de /menu. */
    Component name();

    /** Ordre dans le menu de choix (plus petit = plus haut). */
    default int order() {
        return 100;
    }

    /** Cette interface est-elle proposee a ce joueur par /menu (la ou il se trouve) ? */
    boolean available(Player player);

    /** Ouvre l'interface (comme un clic droit sur l'objet). */
    void open(Player player);

    /**
     * Case de la barre d'objets (0 a 8) ou l'objet serait donne a ce joueur maintenant, ou -1 s'il n'en recoit pas
     * (desactive sur ce serveur, hors de la zone...). Sert a /menu on pour verifier que la case est libre.
     */
    int slot(Player player);

    /** Est-ce cet objet ? (pour le retirer avec /menu off) */
    boolean isItem(ItemStack item);

    /** Donne l'objet a sa case (appele par /menu on, cases deja verifiees). */
    void give(Player player);
}
