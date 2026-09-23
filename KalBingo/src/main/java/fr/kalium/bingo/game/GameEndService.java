package fr.kalium.bingo.game;

import fr.kalium.bingo.gui.LobbyItems;
import fr.kalium.bingo.network.AssignmentService;
import fr.kalium.bingo.network.RelayClient;
import fr.kalium.bingo.persistence.GamePersistence;
import fr.kalium.bingo.world.LobbySlots;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.logging.Logger;

/**
 * Fin de partie (section 1 etapes 11/12, section 5) - TROIS declencheurs :
 *
 *  - TIMEOUT (voir tick(), appele periodiquement par BingoPlugin) : "la partie doit se terminer
 *    a la fin du temps reglementaire".
 *
 *  - VICTOIRE (voir checkWin(), appele par ObjectiveValidationTask juste apres qu'une case
 *    vient d'etre validee, AVANT que le prochain tick() de timeout ne puisse s'executer) : "si
 *    une equipe rempli tous les objectifs".
 *
 *  - ABANDON (voir tick() / endByNoPlayers()), AJOUTE le 23/09/2026 : "il faudrait que la partie
 *    se termine si personne n'est connecté dessus depuis 10 minutes" (game.no-players-abandon-
 *    after-seconds) - distinct du suivi PAR JOUEUR de GameManager.hasAbandoned (qui ne declenche
 *    toujours aucune action, voir sa javadoc) : ici c'est la partie ENTIERE qui se termine des que
 *    plus AUCUN joueur n'est connecte a l'une de ses instances pendant ce delai. Personne n'etant
 *    en ligne au moment ou ce declencheur agit (sinon il ne se serait pas declenche), il n'y a ni
 *    message ni salle d'attente a proposer - juste le nettoyage habituel (relais, mondes, fichier
 *    de sauvegarde).
 *
 * TIMEOUT et VICTOIRE partagent DESORMAIS exactement le meme traitement cote joueur (unifies le
 * 23/09/2026, demande explicite de l'utilisateur qui a decrit une fin de partie generique : "à la
 * fin d'une partie on clear les inventaires des joueurs, on leur redonne une netherstar pour
 * qu'ils puissent retourner au hub kalgames (via un menu) et on efface les maps" - voir
 * finishAndSendToLobby ci-dessous) : inventaire ENTIEREMENT vide (armure et main secondaire
 * comprises, pas seulement les objets de ce plugin), teleportation vers la salle d'attente Bingo
 * (LobbySlots, pas kal-games), nether star qui ouvre desormais un MENU de retour (voir
 * fr.kalium.bingo.gui.PostGameMenu / LobbyProtectionListener - remplace le depart immediat au clic
 * utilise jusqu'ici), avec un delai de grace (game.post-game-lobby-timeout-seconds, 10 min par
 * defaut) avant expulsion automatique (voir sweepLingering()). Seul le message affiche differe
 * (victoire/defaite vs temps ecoule). Un depart volontaire (bouton du menu, voir
 * PostGameMenu/leaveVoluntarily) renvoie immediatement vers kal-games.
 *
 * Dans les trois cas : l'enregistrement "en partie" aupres du relais (RelayClient.clearActiveGame)
 * est leve pour CHAQUE membre de la partie (en ligne ou non), la sauvegarde de la partie est
 * supprimee (GamePersistence.delete - une partie terminee ne doit plus jamais etre restauree au
 * redemarrage suivant), et la suppression effective des mondes d'instance (GameManager.cleanupGame,
 * "efface les maps") est DIFFEREE de quelques secondes (game.end-cleanup-delay-seconds) pour
 * laisser le temps au transfert (teleportation) de sortir effectivement les joueurs du monde avant
 * que InstanceWorldManager.deleteInstanceWorld() ne tente de le decharger.
 */
public final class GameEndService {

    private final Logger logger;
    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final LobbySlots lobbySlots;
    private final LobbyItems lobbyItems;
    private final RelayClient relayClient;
    private final AssignmentService assignmentService;
    private final GamePersistence gamePersistence;
    private final PlayerResetService playerReset;
    private final long cleanupDelayTicks;
    private final Duration postGameLobbyTimeout;
    private final Duration noPlayersAbandonAfter;

    /** Joueur actuellement dans la salle d'attente POST-partie (victoire OU temps ecoule) -> instant
     *  limite au-dela duquel il est expulse automatiquement (voir sweepLingering()). */
    private final Map<UUID, Instant> lingeringDeadlines = new HashMap<>();
    /** gameId de la partie post-partie dont chaque joueur "lingering" provient - pour liberer
     *  l'emplacement de salle d'attente (LobbySlots) une fois qu'il ne reste plus personne. */
    private final Map<UUID, String> lingeringGameId = new HashMap<>();

    /** Pour chaque partie EN COURS actuellement sans AUCUN joueur connecte : instant depuis lequel
     *  c'est le cas (voir tick() / endByNoPlayers()). Retire des qu'au moins un joueur reconnecte. */
    private final Map<String, Instant> emptySince = new HashMap<>();

    public GameEndService(Logger logger, JavaPlugin plugin, GameManager gameManager, LobbySlots lobbySlots,
                           LobbyItems lobbyItems, RelayClient relayClient,
                           AssignmentService assignmentService, GamePersistence gamePersistence,
                           PlayerResetService playerReset,
                           Duration cleanupDelay, Duration postGameLobbyTimeout, Duration noPlayersAbandonAfter) {
        this.logger = logger;
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.lobbySlots = lobbySlots;
        this.lobbyItems = lobbyItems;
        this.relayClient = relayClient;
        this.assignmentService = assignmentService;
        this.gamePersistence = gamePersistence;
        this.playerReset = playerReset;
        this.cleanupDelayTicks = Math.max(20L, cleanupDelay.getSeconds() * 20L);
        this.postGameLobbyTimeout = postGameLobbyTimeout;
        this.noPlayersAbandonAfter = noPlayersAbandonAfter;
    }

    /** A appeler periodiquement (voir BingoPlugin) : declenche la fin par timeout/abandon des
     *  parties en cours, puis verifie les expulsions de la salle d'attente post-partie. */
    public void tick() {
        for (BingoGame game : gameManager.getActiveGames()) {
            if (game.getState() != GameState.IN_PROGRESS) {
                continue;
            }
            if (game.isTimeUp()) {
                emptySince.remove(game.getGameId());
                endByTimeout(game);
                continue;
            }
            checkEmptiness(game);
        }
        sweepLingering();
    }

    private void checkEmptiness(BingoGame game) {
        String gameId = game.getGameId();
        if (game.hasAnyConnectedPlayer()) {
            emptySince.remove(gameId);
            return;
        }
        Instant since = emptySince.computeIfAbsent(gameId, id -> Instant.now());
        if (Duration.between(since, Instant.now()).compareTo(noPlayersAbandonAfter) >= 0) {
            emptySince.remove(gameId);
            endByNoPlayers(game);
        }
    }

    /**
     * A appeler par AbandonService juste apres qu'un joueur vient d'abandonner (voir
     * BingoInstance.markAbandoned) : si plus AUCUN joueur n'est desormais actif sur cette partie
     * (tous ont abandonne/deconnecte), inutile d'attendre game.no-players-abandon-after-seconds -
     * la partie se termine tout de suite, plutot que de laisser tourner une partie vide pendant
     * jusqu'a 10 minutes suite a un abandon deliberement complet.
     */
    public void checkImmediateAbandonment(BingoGame game) {
        if (game.getState() == GameState.IN_PROGRESS && !game.hasAnyConnectedPlayer()) {
            emptySince.remove(game.getGameId());
            endByNoPlayers(game);
        }
    }

    /**
     * A appeler par AbandonService juste apres un abandon (0.1.18) - demande explicite de
     * l'utilisateur : "si tous les membres d'une équipe ont abandonné, fin de la partie", precisee
     * via AskUserQuestion : la partie ne s'arrete QUE si cet abandon ne laisse plus qu'UNE SEULE
     * equipe encore en jeu (4 equipes dont 1 abandonne entierement : les 3 autres continuent), avec
     * un message neutre (pas de gagnant declare). Une equipe "en jeu" = au moins un membre qui n'a
     * pas abandonne - une simple deconnexion ne compte pas (reconnexion possible).
     *
     * Partie a une seule equipe : rien ici, c'est checkImmediateAbandonment (plus aucun joueur actif)
     * qui la termine - il n'y a personne a qui envoyer un message ou une salle d'attente.
     */
    public void checkTeamAbandonment(BingoGame game) {
        if (game.getState() != GameState.IN_PROGRESS || game.getInstances().size() < 2) {
            return;
        }
        int teamsStillPlaying = 0;
        int abandonedTeam = -1;
        for (BingoInstance instance : game.getInstances()) {
            if (instance.isFullyAbandoned()) {
                abandonedTeam = instance.getTeam().getTeamNumber();
            } else {
                teamsStillPlaying++;
            }
        }
        if (abandonedTeam != -1 && teamsStillPlaying <= 1) {
            emptySince.remove(game.getGameId());
            endByTeamAbandon(game, abandonedTeam);
        }
    }

    /**
     * A appeler par ObjectiveValidationTask juste apres qu'une case vient d'etre validee pour
     * cette equipe : verifie si elle a rempli TOUTE la grille et, si oui, termine la partie par
     * victoire.
     */
    public void checkWin(BingoGame game, int teamNumber) {
        if (game.getState() != GameState.IN_PROGRESS || game.getGrid() == null) {
            return;
        }
        int total = game.getGrid().getSize() * game.getGrid().getSize();
        if (total > 0 && game.countValidated(teamNumber) >= total) {
            endByWin(game, teamNumber);
        }
    }

    private void endByTimeout(BingoGame game) {
        if (game.getState() != GameState.IN_PROGRESS) {
            return; // deja termine entre-temps (victoire) - evite un double declenchement
        }
        logger.info("[KalBingo] Partie '" + game.getGameId() + "' terminee (temps écoulé).");
        finishAndSendToLobby(game, team -> Component.text("Temps écoulé ! La partie est terminée.", NamedTextColor.YELLOW));
    }

    private void endByWin(BingoGame game, int winningTeam) {
        logger.info("[KalBingo] Partie '" + game.getGameId() + "' terminee (équipe " + winningTeam + " a rempli la grille).");
        finishAndSendToLobby(game, team -> team == winningTeam
                ? Component.text("Bravo, votre équipe a rempli la grille ! Partie terminée.", NamedTextColor.GREEN)
                : Component.text("L'équipe " + winningTeam + " a rempli la grille en premier. Partie terminée.", NamedTextColor.YELLOW));
    }

    private void endByTeamAbandon(BingoGame game, int abandonedTeam) {
        logger.info("[KalBingo] Partie '" + game.getGameId() + "' terminee (l'equipe " + abandonedTeam
                + " a entierement abandonne, il ne reste qu'une equipe en jeu).");
        finishAndSendToLobby(game, team -> Component.text("L'équipe " + abandonedTeam
                + " a entièrement abandonné. La partie est terminée.", NamedTextColor.YELLOW));
    }

    /**
     * Traitement COMMUN a la fin par victoire et par timeout (voir javadoc de la classe) : vide
     * entierement l'inventaire de chaque joueur en ligne, l'envoie dans la salle d'attente Bingo
     * avec la nether star (menu de retour), et programme le nettoyage differe. `messageForTeam`
     * ne fait varier que le texte affiche (victoire/defaite vs temps ecoule).
     */
    private void finishAndSendToLobby(BingoGame game, IntFunction<Component> messageForTeam) {
        game.setState(GameState.FINISHED);
        Location spawn = lobbySlots.reserve(game.getGameId());
        Instant deadline = Instant.now().plus(postGameLobbyTimeout);
        boolean anyoneLingering = false;
        for (BingoInstance instance : game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            Component message = messageForTeam.apply(team);
            for (UUID playerId : instance.getTeam().getPlayers()) {
                if (instance.hasAbandoned(playerId)) {
                    // Deja remis a zero et renvoye vers kal-games au moment de son abandon (voir
                    // AbandonService) : surtout ne pas le rapatrier dans la salle d'attente.
                    clearActiveGameAsync(playerId);
                    continue;
                }
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                    // Inventaire vide + point de spawn supprime (0.1.18, voir PlayerResetService).
                    playerReset.reset(player);
                    if (spawn != null) {
                        anyoneLingering = true;
                        player.teleport(spawn);
                        lobbyItems.givePostGame(player);
                        lingeringDeadlines.put(playerId, deadline);
                        lingeringGameId.put(playerId, game.getGameId());
                    } else {
                        // Aucune salle d'attente disponible (toutes occupees / pas de modele capture) :
                        // repli sur kal-games plutot que de laisser le joueur bloque sans nether star.
                        player.sendMessage(Component.text("Salle d'attente indisponible, retour à kal-games.", NamedTextColor.RED));
                        assignmentService.sendBackToKalGames(player);
                    }
                } else {
                    // Hors ligne : remise a zero a sa prochaine connexion (voir PlayerResetService).
                    playerReset.resetOrDefer(playerId);
                }
                // Partie vraiment terminee pour ce joueur : une FUTURE reconnexion doit suivre le
                // routage normal (kal-games), pas etre renvoyee ici indefiniment (voir RelayClient).
                clearActiveGameAsync(playerId);
            }
        }
        if (!anyoneLingering) {
            // Personne n'a ete place dans la salle d'attente post-partie : l'emplacement reserve
            // ci-dessus ne serait sinon jamais libere (voir releaseSlotIfEmpty).
            lobbySlots.release(game.getGameId());
        }
        scheduleCleanup(game.getGameId());
    }

    /**
     * Fin par abandon (plus aucun joueur connecte depuis game.no-players-abandon-after-seconds) -
     * AUCUN joueur n'est en ligne a cet instant (garanti par checkEmptiness ci-dessus, appele juste
     * avant sur le meme thread) : ni message, ni salle d'attente, juste le nettoyage habituel.
     */
    private void endByNoPlayers(BingoGame game) {
        if (game.getState() != GameState.IN_PROGRESS) {
            return;
        }
        game.setState(GameState.FINISHED);
        logger.info("[KalBingo] Partie '" + game.getGameId() + "' terminee (abandonnée, plus aucun joueur connecté depuis "
                + noPlayersAbandonAfter.toMinutes() + " min).");
        for (BingoInstance instance : game.getInstances()) {
            for (UUID playerId : instance.getTeam().getPlayers()) {
                clearActiveGameAsync(playerId);
                if (!instance.hasAbandoned(playerId)) {
                    playerReset.resetOrDefer(playerId); // 0.1.18 - tous hors ligne ici, voir PlayerResetService
                }
            }
        }
        scheduleCleanup(game.getGameId());
    }

    private void clearActiveGameAsync(UUID playerId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> relayClient.clearActiveGame(playerId));
    }

    /** Differe la suppression des mondes d'instance pour laisser le temps au transfert (teleportation
     *  vers la salle d'attente) de sortir effectivement les joueurs du monde ; supprime aussi la
     *  sauvegarde de la partie (GamePersistence) - elle ne doit plus jamais etre restauree. */
    private void scheduleCleanup(String gameId) {
        gamePersistence.delete(gameId);
        Bukkit.getScheduler().runTaskLater(plugin, () -> gameManager.cleanupGame(gameId), cleanupDelayTicks);
    }

    /**
     * Expulsion automatique apres game.post-game-lobby-timeout-seconds (10 min par defaut) -
     * demande explicite de l'utilisateur : "si ils reste trop longtemps dans la salle d'attente
     * apres la partie alors ils sont expulsés (apres 10 minutes)". Appelee depuis tick() ci-dessus.
     */
    private void sweepLingering() {
        if (lingeringDeadlines.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        var iterator = lingeringDeadlines.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now.isBefore(entry.getValue())) {
                continue;
            }
            UUID playerId = entry.getKey();
            iterator.remove();
            expelLingering(playerId);
        }
    }

    private void expelLingering(UUID playerId) {
        String gameId = lingeringGameId.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("Vous êtes resté trop longtemps dans la salle d'attente, retour à kal-games.", NamedTextColor.YELLOW));
            lobbyItems.remove(player);
            assignmentService.sendBackToKalGames(player);
        }
        releaseSlotIfEmpty(gameId);
    }

    /** true si ce joueur est actuellement dans la salle d'attente post-partie (voir
     *  LobbyProtectionListener / fr.kalium.bingo.gui.PostGameMenu : le clic sur la nether star y
     *  ouvre le menu de retour plutot que PartyMenu, qui echouerait avec "aucune partie en attente"). */
    public boolean isLingering(UUID playerId) {
        return lingeringDeadlines.containsKey(playerId);
    }

    /** Depart VOLONTAIRE de la salle d'attente post-partie (bouton du menu de retour, voir
     *  fr.kalium.bingo.gui.PostGameMenu) - demande explicite de l'utilisateur : "peuvent partir
     *  quand ils le souhaitent". */
    public void leaveVoluntarily(Player player) {
        UUID playerId = player.getUniqueId();
        lingeringDeadlines.remove(playerId);
        String gameId = lingeringGameId.remove(playerId);
        if (gameId == null) {
            return;
        }
        lobbyItems.remove(player);
        assignmentService.sendBackToKalGames(player);
        releaseSlotIfEmpty(gameId);
    }

    /** Libere l'emplacement de salle d'attente reserve pour cette partie une fois qu'il ne reste
     *  plus personne en attente (parti volontairement ou expulse) - permet a l'emplacement d'etre
     *  reutilise par une AUTRE partie qui se terminerait ensuite. */
    private void releaseSlotIfEmpty(String gameId) {
        if (gameId == null || lingeringGameId.containsValue(gameId)) {
            return;
        }
        lobbySlots.release(gameId);
    }
}
