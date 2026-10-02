package fr.kalium.kvplots.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.UUID;

/** 1.5.0 (catégorie 4 « Récompenses ») : un plot vient d'être noté ; bâtisseurs = créateur + éditeurs (KV_Rewards). */
public final class NoteRecueEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int plot;
    private final List<UUID> batisseurs;

    public NoteRecueEvent(int plot, List<UUID> batisseurs) {
        this.plot = plot;
        this.batisseurs = batisseurs;
    }

    public int plot() {
        return plot;
    }

    public List<UUID> batisseurs() {
        return batisseurs;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
