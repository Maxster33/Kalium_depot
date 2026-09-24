package fr.kalium.bingo.network;

import fr.kalium.bingo.game.BingoGame;
import fr.kalium.bingo.game.BingoInstance;
import fr.kalium.bingo.game.BingoParty;
import fr.kalium.bingo.game.GameManager;
import fr.kalium.bingo.game.GameState;
import fr.kalium.bingo.game.PartyManager;
import fr.kalium.bingo.game.PlayerResetService;
import fr.kalium.bingo.gui.GameItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Declenche la demande d'affectation (AssignmentService) a chaque arrivee de joueur SANS partie
 * en cours, et suit les deconnexions dans la salle d'attente - INCHANGE.
 *
 * Depuis la reconnexion en cours de partie (demande explicite de l'utilisateur : "si un joueur
 * est deconnecte durant une partie et qu'il se reconnecte avant la fin de la partie il faut que
 * le proxy le renvoi directement sur la partie") : verifie d'abord si le joueur appartient a une
 * partie DEJA LANCEE (GameManager.findGameOf) avant de faire quoi que ce soit d'autre. Si oui,
 * c'est une reconnexion en jeu, pas un nouvel arrivant - on ne doit surtout PAS passer par
 * AssignmentService.requestAssignment : la BingoParty (salle d'attente) de ce joueur a deja ete
 * consommee/supprimee au lancement de la partie (voir PartyStarter), donc kal-games repondrait
 * "aucune partie" et handleNoGameFound le renverrait a tort vers kal-games. Le routage initial
 * (proxy -&gt; directement Kixster plutot que kal-games) est gere cote KaliumRelay
 * (PlayerChooseInitialServerEvent) : ce listener-ci se contente de replacer le joueur dans l'etat
 * "connecte" de son instance - Bukkit restaure de lui-meme sa position dans le monde d'instance
 * (jamais supprime pendant que la partie est en cours, voir GameManager.cleanupGame), aucune
 * teleportation manuelle necessaire ici.
 *
 * Ne considere une reconnexion "en jeu" que si la partie trouvee est encore GameState.IN_PROGRESS
 * (AJOUTE avec GameEndService, 23/09/2026) : une partie qui vient de se terminer (victoire ou
 * temps ecoule) reste quelques secondes dans GameManager.getActiveGames() le temps que le
 * nettoyage differe s'execute (voir GameEndService.scheduleCleanup) - sans cette verification, un
 * joueur qui se reconnecterait pendant cette fenetre serait a tort traite comme "encore en
 * partie" (message de reconnexion, papier Objectifs redonne) alors que la partie est deja finie.
 */
public final class PlayerConnectListener implements Listener {

    private final PartyManager partyManager;
    private final GameManager gameManager;
    private final AssignmentService assignmentService;
    private final GameItems gameItems;
    private final PlayerResetService playerReset;

    public PlayerConnectListener(PartyManager partyManager, GameManager gameManager,
                                  AssignmentService assignmentService, GameItems gameItems,
                                  PlayerResetService playerReset) {
        this.partyManager = partyManager;
        this.gameManager = gameManager;
        this.assignmentService = assignmentService;
        this.gameItems = gameItems;
        this.playerReset = playerReset;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 0.1.18 : partie terminee pendant que ce joueur etait hors ligne -> inventaire vide et point
        // de spawn supprime maintenant, avant toute autre chose (voir PlayerResetService).
        playerReset.applyPending(player);

        Optional<BingoGame> activeGame = gameManager.findGameOf(playerId)
                .filter(game -> game.getState() == GameState.IN_PROGRESS);
        if (activeGame.isPresent()) {
            BingoInstance instance = activeGame.get().findInstanceOf(playerId).orElseThrow();
            gameManager.onPlayerReconnect(playerId);
            instance.setPlayerConnected(playerId, true);
            // Joueur hors ligne au lancement : son point de spawn n'a pas pu etre pose a ce
            // moment-la (voir PartyStarter) - on s'en assure ici. Un lit deja pose sur SA map est
            // conserve (on ne remplace que si le spawn actuel n'est pas sur cette map).
            var currentSpawn = player.getRespawnLocation();
            if (currentSpawn == null || currentSpawn.getWorld() == null
                    || !GameManager.belongsTo(currentSpawn.getWorld(), instance)) {
                playerReset.setGameSpawn(player, instance.getSpawnLocation());
            }
            gameItems.give(player); // au cas ou l'inventaire ait ete reinitialise entre-temps
            player.setGameMode(org.bukkit.GameMode.SURVIVAL); // 0.3.1
            player.sendMessage("§aReconnexion à votre partie Bingo en cours.");
            return;
        }

        assignmentService.requestAssignment(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        Optional<BingoGame> activeGame = gameManager.findGameOf(playerId)
                .filter(game -> game.getState() == GameState.IN_PROGRESS);
        if (activeGame.isPresent()) {
            BingoInstance instance = activeGame.get().findInstanceOf(playerId).orElseThrow();
            gameManager.onPlayerDisconnect(playerId);
            instance.setPlayerConnected(playerId, false);
            return;
        }

        BingoParty party = partyManager.partyOf(playerId);
        if (party != null) {
            party.markDisconnected(playerId);
        }
    }
}
