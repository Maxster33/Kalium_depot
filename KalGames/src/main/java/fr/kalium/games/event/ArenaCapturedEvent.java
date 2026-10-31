package fr.kalium.games.event;

import fr.kalium.games.model.Arena;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.BoundingBox;

/**
 * 1.29.0 : zone d'une arene capturee avec succes (modele enregistre), sur le thread principal. Les plugins de jeu
 * peuvent y relever ce que le modele ne garde pas (entites...) et l'ecrire dans l'arene (points, reglages) ; ils
 * enregistrent eux-memes (KalGames.repository().save()). Ex. KG_Parkour 1.8.0 : mobs sans IA -> cibles de la masse.
 */
public final class ArenaCapturedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Arena arena;
    private final World world;
    private final BoundingBox zone;
    private final Player player;

    public ArenaCapturedEvent(Arena arena, World world, BoundingBox zone, Player player) {
        this.arena = arena;
        this.world = world;
        this.zone = zone;
        this.player = player;
    }

    public Arena arena() {
        return arena;
    }

    /** Monde de construction ou la zone a ete capturee. */
    public World world() {
        return world;
    }

    /** Zone capturee (deux coins inclus : blocs entiers). */
    public BoundingBox zone() {
        return zone;
    }

    /** Moderateur qui a lance la capture, null s'il s'est deconnecte entre-temps. */
    public Player player() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
