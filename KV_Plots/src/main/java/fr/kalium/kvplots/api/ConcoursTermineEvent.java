package fr.kalium.kvplots.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/** 1.5.0 (catégorie 4 « Récompenses ») : votes d'un concours clos ; classement final (total des notes, puis moyenne). */
public final class ConcoursTermineEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final KanvasPlots.ConcoursInfo concours;
    private final List<KanvasPlots.PlotInfo> classement;

    public ConcoursTermineEvent(KanvasPlots.ConcoursInfo concours, List<KanvasPlots.PlotInfo> classement) {
        this.concours = concours;
        this.classement = classement;
    }

    public KanvasPlots.ConcoursInfo concours() {
        return concours;
    }

    public List<KanvasPlots.PlotInfo> classement() {
        return classement;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
