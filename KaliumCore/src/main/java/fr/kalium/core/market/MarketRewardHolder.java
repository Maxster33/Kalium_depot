package fr.kalium.core.market;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Marqueur d'inventaire pour une page du coffre de recompenses personnel d'un joueur (voir
 * MarketService#rewardPageItems/#reconcileRewardPage). Permet a MarketGuiListener de distinguer ce
 * coffre (ou l'on ne doit jamais pouvoir deposer un objet, seulement en retirer) des ecrans de
 * navigation du commerce, et de savoir quelle tranche de la liste de recompenses reconcilier a la
 * fermeture (le coffre n'est pas une case fixe : c'est une liste paginee, voir MarketService).
 */
public final class MarketRewardHolder implements InventoryHolder {

    private final UUID owner;
    private final int page;
    private Inventory inventory;

    public MarketRewardHolder(UUID owner, int page) {
        this.owner = owner;
        this.page = page;
    }

    public UUID owner() {
        return owner;
    }

    public int page() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
