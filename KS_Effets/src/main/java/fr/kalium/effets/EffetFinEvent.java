package fr.kalium.effets;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Un effet custom à minuteur vient de se terminer pour un joueur en ligne. Le plugin qui l'a déclaré arrête alors ce
 * qu'il faisait (musique, lumière...). Un effet terminé hors ligne n'existe pas : le minuteur ne s'écoule qu'en jeu.
 */
public final class EffetFinEvent extends Event {

    public enum Cause {
        /** Le temps est écoulé. */
        EXPIRE,
        /** Retiré par un plugin ({@link KSEffets#retirer}). */
        RETIRE,
        /** Mort du joueur (réglage effacer-a-la-mort). */
        MORT,
        /** Lait bu (réglage effacer-au-lait). */
        LAIT
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player joueur;
    private final String id;
    private final Cause cause;

    EffetFinEvent(Player joueur, String id, Cause cause) {
        this.joueur = joueur;
        this.id = id;
        this.cause = cause;
    }

    public Player getJoueur() {
        return joueur;
    }

    /** Identifiant de l'effet, ex. « ks_alcools:rhum ». */
    public String getId() {
        return id;
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
