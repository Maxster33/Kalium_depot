package fr.kalium.core.market;

import org.bukkit.inventory.ItemStack;

/**
 * Une vente flash programmee par un operateur (voir MarketAdminMenu.openScheduleFlashSale) : un lot
 * fixe (l'objet tenu en main par l'operateur, avec sa quantite, sert de "modele" pour chaque unite
 * vendue) propose a prix reduit pendant une fenetre de temps donnee, en stock limite.
 */
public final class FlashSale {

    public enum Status { UPCOMING, ACTIVE, SOLD_OUT, EXPIRED }

    private final long id;
    private final ItemStack item;
    private final int price;
    private final int stock;
    private int purchased;
    private final long startAt;
    private final long endAt;
    private final String createdBy;

    public FlashSale(long id, ItemStack item, int price, int stock, long startAt, long endAt, String createdBy) {
        this.id = id;
        this.item = item;
        this.price = price;
        this.stock = stock;
        this.purchased = 0;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdBy = createdBy;
    }

    public long id() {
        return id;
    }

    public ItemStack item() {
        return item;
    }

    public int price() {
        return price;
    }

    public int stock() {
        return stock;
    }

    public int purchased() {
        return purchased;
    }

    public void purchased(int value) {
        purchased = value;
    }

    public long startAt() {
        return startAt;
    }

    public long endAt() {
        return endAt;
    }

    public String createdBy() {
        return createdBy;
    }

    public int remaining() {
        return Math.max(0, stock - purchased);
    }

    public Status status(long now) {
        if (remaining() <= 0) {
            return Status.SOLD_OUT;
        }
        if (now < startAt) {
            return Status.UPCOMING;
        }
        if (now > endAt) {
            return Status.EXPIRED;
        }
        return Status.ACTIVE;
    }
}
