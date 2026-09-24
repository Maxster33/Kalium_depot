package fr.kalium.bingo.game;

import fr.kalium.bingo.gui.GameItems;
import fr.kalium.bingo.gui.LobbyItems;
import fr.kalium.bingo.network.PartyStatusNotifier;
import fr.kalium.bingo.network.RelayClient;
import fr.kalium.bingo.persistence.GamePersistence;
import fr.kalium.bingo.world.InstanceWorldPreparer;
import fr.kalium.bingo.world.LobbySlots;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Fait passer une partie de la salle d'attente (BingoParty, equipes choisies) aux
 * instances de jeu reelles (BingoGame, un monde par equipe).
 *
 * Declenchement par l'hote (menu PartyMenu, bouton "Lancer la partie") ou par un operateur
 * (/bingoadmin start &lt;gameId&gt;, voir BingoAdminCommand - MEME regle que l'hote, demande
 * explicite de l'utilisateur : "respecte le délai comme l'hôte"). Un delai minimum (10s * nombre
 * d'equipes - voir PartyCountdownService) doit s'etre ecoule depuis la creation de la salle
 * d'attente avant qu'une partie puisse reellement demarrer, le temps que la pre-generation en
 * cascade des mondes ait une chance de finir. Si ce delai n'est pas encore ecoule, start() ne
 * rate pas : il programme le demarrage automatique et affiche un chronometre (voir
 * Result.scheduled()).
 */
public final class PartyStarter {

    public enum Failure {
        UNKNOWN_PARTY, NO_TEAMS, CREATE_FAILED
    }

    public record Result(BingoGame game, Failure failure, String message, boolean scheduled) {
        public boolean ok() {
            return failure == null;
        }

        public static Result immediate(BingoGame game) {
            return new Result(game, null, null, false);
        }

        public static Result scheduled(String message) {
            return new Result(null, null, message, true);
        }

        public static Result failure(Failure failure, String message) {
            return new Result(null, failure, message, false);
        }
    }

    private final Logger logger;
    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final PartyManager partyManager;
    private final LobbySlots lobbySlots;
    private final LobbyItems lobbyItems;
    private final GameItems gameItems;
    private final RelayClient relayClient;
    private final GamePersistence gamePersistence;
    private final PartyStatusNotifier partyStatusNotifier;
    private final PartyCountdownService countdownService;
    private final PlayerResetService playerReset;
    private final InstanceWorldPreparer worldPreparer;

    public PartyStarter(Logger logger, JavaPlugin plugin, GameManager gameManager, PartyManager partyManager,
                         LobbySlots lobbySlots, LobbyItems lobbyItems, GameItems gameItems, RelayClient relayClient,
                         GamePersistence gamePersistence, PartyStatusNotifier partyStatusNotifier,
                         PartyCountdownService countdownService, PlayerResetService playerReset,
                         InstanceWorldPreparer worldPreparer) {
        this.logger = logger;
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.partyManager = partyManager;
        this.lobbySlots = lobbySlots;
        this.lobbyItems = lobbyItems;
        this.gameItems = gameItems;
        this.relayClient = relayClient;
        this.gamePersistence = gamePersistence;
        this.partyStatusNotifier = partyStatusNotifier;
        this.countdownService = countdownService;
        this.playerReset = playerReset;
        this.worldPreparer = worldPreparer;
    }

    public Result start(String gameId) {
        BingoParty party = partyManager.get(gameId);
        if (party == null) {
            return Result.failure(Failure.UNKNOWN_PARTY, "Aucune partie en attente '" + gameId + "'.");
        }
        List<List<UUID>> teams = party.teamsForGameCreation();
        if (teams.isEmpty()) {
            return Result.failure(Failure.NO_TEAMS, "Aucun joueur n'a encore choisi d'équipe.");
        }

        if (!countdownService.isReady(party)) {
            boolean justScheduled = countdownService.scheduleStart(party, () -> start(gameId));
            String message = justScheduled
                    ? "§eDélai de sécurité en cours (pré-génération des mondes) : la partie démarrera "
                            + "automatiquement dans " + formatSeconds(countdownService.remaining(party)) + "."
                    : "§eLa partie démarrera automatiquement dans " + formatSeconds(countdownService.remaining(party)) + ".";
            return Result.scheduled(message);
        }

        // 0.6.0 : attend que les maps des equipes soient pretes (overworld + terrain) au lieu de generer d'un
        // coup celles qui manquent (plusieurs secondes de blocage par map) - demande de LeKiwi06 : "pas grave si
        // la game met 5 minutes a se lancer, on veux que ce soit fluide".
        if (!worldPreparer.mapsReady(gameId, teams.size())) {
            worldPreparer.ensureQueued(gameId, party.getSeed(), teams.size());
            countdownService.waitForMaps(party,
                    () -> worldPreparer.mapsReady(gameId, party.teamsForGameCreation().size()),
                    () -> worldPreparer.progressPercent(gameId, party.teamsForGameCreation().size()),
                    () -> start(gameId));
            return Result.scheduled("§eMaps en préparation (" + worldPreparer.progressPercent(gameId, teams.size())
                    + " %) : la partie démarrera automatiquement dès qu'elles seront prêtes.");
        }

        BingoGame game;
        try {
            game = gameManager.createGame(party.getGameId(), party.getSeed(), party.getDuration(), teams);
            game.setSettings(party.getSettings()); // 0.3.0 - avant assignGrid (composition de la grille)
            gameManager.prepareInstances(game);
            gameManager.assignGrid(game);
        } catch (RuntimeException e) {
            logger.warning("[KG_BingoGame] Impossible de démarrer la partie '" + gameId + "' : " + e.getMessage());
            return Result.failure(Failure.CREATE_FAILED, e.getMessage());
        }

        Player carrier = null;
        for (BingoInstance instance : game.getInstances()) {
            for (UUID playerId : instance.getTeam().getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.teleport(instance.getSpawnLocation());
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL); // 0.3.1 : survie sur la map
                    // Le point d'apparition sur la map devient le point de spawn du joueur (0.1.18,
                    // demande explicite : il reapparaissait sur le modele de la salle d'attente).
                    playerReset.setGameSpawn(player, instance.getSpawnLocation());
                    instance.setPlayerConnected(playerId, true);
                    // La salle d'attente est quittee pour de bon : la Nether Star ne sert plus a
                    // rien sur la carte de jeu (demande explicite de l'utilisateur, elle restait
                    // affichee et inutilisable - "il n'y a pas de partie en attente"). Remplacee
                    // par le papier "Objectifs" (GameMenu).
                    lobbyItems.remove(player);
                    // 0.1.19 : vie et nourriture au maximum + kit de depart (demande explicite de
                    // l'utilisateur, voir StarterKit) - AVANT le papier Objectifs (emplacement 8).
                    StarterKit.prepare(player);
                    gameItems.give(player);
                    player.sendMessage("§6Partie lancée : §e" + game.getSettings().describe()); // 0.3.0
                    if (carrier == null) {
                        carrier = player; // voir PartyStatusNotifier - n'importe quel joueur en ligne suffit
                    }
                }
                // Enregistre le joueur aupres du relais HTTP comme "en partie sur ce serveur"
                // (meme si hors ligne au moment du lancement) - demande explicite de l'utilisateur :
                // une reconnexion doit etre renvoyee directement sur la partie, pas sur kal-games.
                // Appel HTTP bloquant (voir RelayClient) : jamais sur le thread principal.
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> relayClient.registerActiveGame(playerId));
            }
        }

        game.start();
        // Premiere sauvegarde de la partie EN COURS (voir GamePersistence) : des ce point, elle
        // survivra a un redemarrage/crash du serveur (demande explicite de l'utilisateur, 23/09/2026).
        gamePersistence.save(game);
        lobbySlots.release(gameId);
        partyManager.remove(gameId);
        // Previent kal-games que cette partie a demarre : elle ne doit plus apparaitre comme
        // "encore en salle d'attente, a rejoindre" dans son menu - AJOUTE le 24/09/2026, voir
        // PartyStatusNotifier.
        partyStatusNotifier.notifyClosed(gameId, carrier);
        // Nettoyage defensif : un compte a rebours ne devrait normalement plus etre actif ici (voir
        // scheduleStart(), qui se retire de lui-meme juste avant d'appeler start() a nouveau), mais
        // couvre le cas d'un demarrage manuel (/bingoadmin start) pendant qu'un compte a rebours
        // etait deja en cours pour ce gameId.
        countdownService.cancel(gameId);
        return Result.immediate(game);
    }

    private static String formatSeconds(Duration duration) {
        long seconds = duration.isNegative() ? 0L : duration.getSeconds() + (duration.getNano() > 0 ? 1 : 0);
        return seconds + "s";
    }
}
