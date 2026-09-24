package fr.kalium.bingo.game;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;

/**
 * Filet de securite du point de spawn en partie (voir PlayerResetService.setGameSpawn) - AJOUTE en
 * 0.1.18. Le point de spawn pose au lancement suffit dans le cas normal, mais le jeu peut le
 * remplacer : un joueur qui dort dans un lit sur sa map le deplace sur ce lit, et si ce lit est
 * ensuite detruit, le jeu renvoie le joueur au spawn du monde par defaut du serveur - c'est-a-dire
 * sur le modele de la salle d'attente, exactement le probleme signale par l'utilisateur.
 *
 * Ici : si un joueur d'une partie EN COURS reapparait ailleurs que sur SA map d'instance, il est
 * renvoye sur le point d'apparition de cette map. Un lit ou une ancre valide sur sa propre map
 * reste respecte.
 */
public final class GameRespawnListener implements Listener {

    private final GameManager gameManager;

    public GameRespawnListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Optional<BingoGame> game = gameManager.findGameOf(event.getPlayer().getUniqueId())
                .filter(g -> g.getState() == GameState.IN_PROGRESS);
        if (game.isEmpty()) {
            return;
        }
        Optional<BingoInstance> instance = game.get().findInstanceOf(event.getPlayer().getUniqueId());
        if (instance.isEmpty() || instance.get().getWorld() == null || instance.get().getSpawnLocation() == null) {
            return;
        }
        Location target = event.getRespawnLocation();
        if (target == null || target.getWorld() == null || !GameManager.belongsTo(target.getWorld(), instance.get())) {
            event.setRespawnLocation(instance.get().getSpawnLocation());
        }
    }
}
