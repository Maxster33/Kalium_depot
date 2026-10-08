package fr.kalium.boatrace;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

/**
 * 1.6.0 : regles des bateaux calcules par le serveur (ServerBoats). Le siege d'un coureur n'est pas un « vehicule » pour
 * le serveur : la sortie du bateau, que KalGames interdit en course (VehicleExitEvent), est donc interdite ici aussi.
 * Personne ne monte dans la coque (vrai bateau vide) : son jeu se mettrait a la piloter.
 */
final class ServerBoatListener implements Listener {

    private final KalGames games;

    ServerBoatListener(KalGames games) {
        this.games = games;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && event.getDismounted().getScoreboardTags().contains(ServerBoats.SEAT_TAG)) {
            GameInstance game = games.instances().of(player);
            if (game != null && game.racing(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnter(VehicleEnterEvent event) {
        if (event.getVehicle().getScoreboardTags().contains(ServerBoats.HULL_TAG)) {
            event.setCancelled(true);
        }
    }
}
