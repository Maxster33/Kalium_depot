package fr.kalium.economy.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 1.1.5 (catégorie 6, anti-triche : revente suspecte) : un /echange vient d'aboutir. Le joueur A a donné objetsA et
 * pointsA à B, qui lui a donné objetsB et pointsB (copies : modifier les listes ne change rien).
 */
public final class EchangeTermineEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player a;
    private final Player b;
    private final List<ItemStack> objetsA;
    private final List<ItemStack> objetsB;
    private final long pointsA;
    private final long pointsB;

    public EchangeTermineEvent(Player a, Player b, List<ItemStack> objetsA, List<ItemStack> objetsB, long pointsA,
                               long pointsB) {
        this.a = a;
        this.b = b;
        this.objetsA = List.copyOf(objetsA.stream().map(ItemStack::clone).toList());
        this.objetsB = List.copyOf(objetsB.stream().map(ItemStack::clone).toList());
        this.pointsA = pointsA;
        this.pointsB = pointsB;
    }

    public Player a() {
        return a;
    }

    public Player b() {
        return b;
    }

    public List<ItemStack> objetsA() {
        return objetsA;
    }

    public List<ItemStack> objetsB() {
        return objetsB;
    }

    public long pointsA() {
        return pointsA;
    }

    public long pointsB() {
        return pointsB;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
