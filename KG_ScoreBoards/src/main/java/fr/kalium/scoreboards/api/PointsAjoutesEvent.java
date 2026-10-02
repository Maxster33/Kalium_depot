package fr.kalium.scoreboards.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/** 1.8.0 (catégorie 4 « Récompenses ») : des points viennent d'être ajoutés à un joueur pour un mini-jeu (KG_Rewards). */
public final class PointsAjoutesEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String minigame;
    private final UUID joueur;
    private final String nom;
    private final double points;

    public PointsAjoutesEvent(String minigame, UUID joueur, String nom, double points) {
        this.minigame = minigame;
        this.joueur = joueur;
        this.nom = nom;
        this.points = points;
    }

    public String minigame() {
        return minigame;
    }

    public UUID joueur() {
        return joueur;
    }

    public String nom() {
        return nom;
    }

    public double points() {
        return points;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
