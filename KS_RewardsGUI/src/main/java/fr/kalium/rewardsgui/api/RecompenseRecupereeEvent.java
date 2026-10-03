package fr.kalium.rewardsgui.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 1.0.1 (catégorie 6, anti-triche : revente suspecte) : un joueur vient de récupérer une récompense (/rewards) :
 * origine (Kal-Games, Kanvas...), raison, objets reçus (copies), points reçus, date d'envoi de la récompense.
 */
public final class RecompenseRecupereeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player joueur;
    private final String origine;
    private final String raison;
    private final List<ItemStack> objets;
    private final long points;
    private final long dateEnvoi;

    public RecompenseRecupereeEvent(Player joueur, String origine, String raison, List<ItemStack> objets, long points,
                                    long dateEnvoi) {
        this.joueur = joueur;
        this.origine = origine;
        this.raison = raison;
        this.objets = List.copyOf(objets.stream().map(ItemStack::clone).toList());
        this.points = points;
        this.dateEnvoi = dateEnvoi;
    }

    public Player joueur() {
        return joueur;
    }

    public String origine() {
        return origine;
    }

    public String raison() {
        return raison;
    }

    public List<ItemStack> objets() {
        return objets;
    }

    public long points() {
        return points;
    }

    public long dateEnvoi() {
        return dateEnvoi;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
