package fr.kalium.core.market;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marqueur d'inventaire pour les pages type "coffre" du commerce (Marche, Vente flash, Mes ventes,
 * Resultats de recherche) : purement decoratif/navigable, aucun objet du joueur n'y transite jamais
 * (voir MarketGuiListener) - la mise en vente se fait via l'objet tenu en main, pas par
 * glisser-deposer dans ces ecrans. Chaque ecran est atteint depuis le menu Dialog "Commerce"
 * (voir MarketGui#openHub) et non plus via des onglets internes.
 */
public final class MarketGuiHolder implements InventoryHolder {

    public enum Screen { MARCHE, VENTE_FLASH, MES_VENTES, RECHERCHE }

    private final Screen screen;
    private final int page;
    /** Mot-cle de recherche (ecran RECHERCHE uniquement, null sinon). */
    private final String keyword;
    private Inventory inventory;

    public MarketGuiHolder(Screen screen, int page, String keyword) {
        this.screen = screen;
        this.page = page;
        this.keyword = keyword;
    }

    public Screen screen() {
        return screen;
    }

    public int page() {
        return page;
    }

    public String keyword() {
        return keyword;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
