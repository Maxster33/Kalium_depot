package fr.kalium.core.market;

import org.bukkit.inventory.ItemStack;

/** Une ligne de l'historique d'achats/ventes d'un joueur (voir MarketService.history / recordTransaction). */
public final class TransactionRecord {

    public enum Type { VENTE, ACHAT, VENTE_FLASH }

    private final Type type;
    private final ItemStack item;
    private final int price;
    private final String counterpart;
    private final long at;

    public TransactionRecord(Type type, ItemStack item, int price, String counterpart, long at) {
        this.type = type;
        this.item = item;
        this.price = price;
        this.counterpart = counterpart;
        this.at = at;
    }

    public Type type() {
        return type;
    }

    public ItemStack item() {
        return item;
    }

    public int price() {
        return price;
    }

    public String counterpart() {
        return counterpart;
    }

    public long at() {
        return at;
    }
}
