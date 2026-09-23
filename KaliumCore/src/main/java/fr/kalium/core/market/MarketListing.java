package fr.kalium.core.market;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Une annonce de vente posee par un joueur dans le systeme de commerce (onglet "Ventes"). */
public final class MarketListing {

    private final long id;
    private final UUID seller;
    private final String sellerName;
    private final ItemStack item;
    private final int price;
    private final long listedAt;
    private final long expiresAt;

    public MarketListing(long id, UUID seller, String sellerName, ItemStack item, int price,
                          long listedAt, long expiresAt) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.listedAt = listedAt;
        this.expiresAt = expiresAt;
    }

    public long id() {
        return id;
    }

    public UUID seller() {
        return seller;
    }

    public String sellerName() {
        return sellerName;
    }

    public ItemStack item() {
        return item;
    }

    public int price() {
        return price;
    }

    public long listedAt() {
        return listedAt;
    }

    public long expiresAt() {
        return expiresAt;
    }
}
