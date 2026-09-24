package fr.kalium.bingo.game;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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

    /**
     * 0.3.0 : keepInventory GARANTI pour les joueurs d'une partie en cours, sans dependre de la regle de jeu du
     * monde - en test (24/09/2026) la regle n'etait pas active sur les maps : LeKiwi06 l'avait activee a la main
     * sur la sienne, Maxster33 non et a perdu son inventaire. Demande explicite : "activer le keep inventory sur
     * les mondes des joueurs quand la partie demarre" (overworld, Nether et End de l'equipe).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        boolean inGame = gameManager.findGameOf(event.getPlayer().getUniqueId())
                .filter(g -> g.getState() == GameState.IN_PROGRESS).isPresent();
        if (!inGame) {
            return;
        }
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
    }
}
