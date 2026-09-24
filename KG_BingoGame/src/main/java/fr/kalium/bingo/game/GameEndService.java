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
import fr.kalium.bingo.score.ScoreEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // ------------------------------------------------------------------ 0.3.0 : nulle, modes, classements

    /** Resultat d'une partie terminee normalement (0.3.0). */
    public enum Outcome {
        /** Une equipe gagne (bingos demandes, grille complete, meilleur score au chrono, ou derniere en jeu). */
        WIN,
        /** Meilleurs scores egaux a la fin du chrono : compte comme une nulle pour les points, mais s'appelle
         *  "Egalite" (precision de LeKiwi06 : "ce n'est pas la meme chose semantiquement"). */
        EGALITE,
        /** Nulle acceptee par vote. */
        NULLE
    }

    private DrawVoteService drawVotes;
    /** Blackout : derniere heure de jeu pour laquelle une nulle a ete proposee automatiquement. */
    private final Map<String, Long> autoDrawHour = new HashMap<>();
    /** "gameId:equipe" des equipes dont l'abandon complet a deja ete traite. */
    private final java.util.Set<String> abandonedTeamsHandled = new java.util.HashSet<>();

    public void setDrawVotes(DrawVoteService drawVotes) {
        this.drawVotes = drawVotes;
        drawVotes.setOnDrawAccepted(game -> finish(game, Outcome.NULLE, -1, "Nulle acceptée par toutes les équipes."));
    }

    /** A appeler periodiquement (voir BingoPlugin) : fin au chrono, abandons par deconnexion, nulle automatique du
     *  blackout, fin sans joueur, votes, puis expulsions de la salle d'attente post-partie. */
    public void tick() {
        for (BingoGame game : gameManager.getActiveGames()) {
            if (game.getState() != GameState.IN_PROGRESS) {
                continue;
            }
            checkDisconnectionAbandons(game);
            if (game.getState() != GameState.IN_PROGRESS || game.isFrozen()) {
                continue;
            }
            if (game.isTimeUp()) {
                emptySince.remove(game.getGameId());
                endByTimeout(game);
                continue;
            }
            if (game.getSettings().isBlackout() && drawVotes != null) {
                // Demande explicite de LeKiwi06 : "proposition de match nul automatique a 1h de jeu en blackout,
                // nouvelle proposition toutes les 1 heure".
                long hours = game.getElapsed().toHours();
                if (hours >= 1 && hours > autoDrawHour.getOrDefault(game.getGameId(), 0L)) {
                    autoDrawHour.put(game.getGameId(), hours);
                    drawVotes.proposeAutomatic(game, hours + " h de jeu");
                }
            }
            checkEmptiness(game);
        }
        if (drawVotes != null) {
            drawVotes.tick();
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
     * 0.3.0 - demande explicite de LeKiwi06 : un joueur deconnecte depuis game.disconnect-abandon-seconds (10 min)
     * a abandonne DEFINITIVEMENT (meme consequence qu'un abandon volontaire, voir AbandonService). S'applique aussi
     * au joueur expulse pour inactivite (voir InactivityService).
     */
    private void checkDisconnectionAbandons(BingoGame game) {
        boolean any = false;
        for (BingoInstance instance : game.getInstances()) {
            for (UUID playerId : instance.getTeam().getPlayers()) {
                if (instance.hasAbandoned(playerId) || instance.isPlayerConnected(playerId)
                        || !gameManager.hasAbandoned(playerId)) {
                    continue;
                }
                instance.markAbandoned(playerId);
                gameManager.onPlayerReconnect(playerId); // oublie le compte a rebours
                playerReset.resetOrDefer(playerId);
                clearActiveGameAsync(playerId);
                String name = Bukkit.getOfflinePlayer(playerId).getName();
                broadcast(game, Component.text((name != null ? name : "Un joueur") + " (équipe " + instance.getTeam().getTeamNumber()
                        + ") a abandonné : déconnecté depuis trop longtemps.", NamedTextColor.GRAY));
                any = true;
            }
        }
        if (any) {
            checkTeamAbandonment(game);
            checkImmediateAbandonment(game);
        }
    }

    /**
     * A appeler par AbandonService juste apres qu'un joueur vient d'abandonner (voir
     * BingoInstance.markAbandoned) : si plus AUCUN joueur n'est desormais actif sur cette partie
     * (tous ont abandonne/deconnecte), inutile d'attendre game.no-players-abandon-after-seconds -
     * la partie se termine tout de suite.
     */
    public void checkImmediateAbandonment(BingoGame game) {
        if (game.getState() == GameState.IN_PROGRESS && !game.hasAnyConnectedPlayer()) {
            emptySince.remove(game.getGameId());
            endByNoPlayers(game);
        }
    }

    /**
     * Apres un abandon (0.3.0 - demande explicite de LeKiwi06) : une equipe dont TOUS les membres ont abandonne
     * perd et sera classee derniere quel que soit son score (ses joueurs gardent les points de l'equipe).
     * - Il reste plusieurs equipes : la partie continue et une nulle est proposee a toutes ("fair-play").
     * - Il n'en reste qu'une : la partie est arretee et cette equipe vote la nulle ; si elle refuse, le score
     *   s'applique (elle gagne, les autres ayant abandonne).
     */
    public void checkTeamAbandonment(BingoGame game) {
        if (game.getState() != GameState.IN_PROGRESS || game.getInstances().size() < 2) {
            return;
        }
        List<Integer> playing = new ArrayList<>();
        List<Integer> newlyAbandoned = new ArrayList<>();
        for (BingoInstance instance : game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            if (!instance.isFullyAbandoned()) {
                playing.add(team);
            } else if (abandonedTeamsHandled.add(game.getGameId() + ":" + team)) {
                newlyAbandoned.add(team);
            }
        }
        if (newlyAbandoned.isEmpty()) {
            return;
        }
        for (int team : newlyAbandoned) {
            broadcast(game, Component.text("L'équipe " + team + " a abandonné : elle perd la partie et sera classée dernière.",
                    NamedTextColor.RED));
        }
        if (playing.size() >= 2 && drawVotes != null) {
            drawVotes.proposeAutomatic(game, "Abandon de l'équipe " + newlyAbandoned.get(0));
        } else if (playing.size() == 1) {
            int last = playing.get(0);
            if (drawVotes == null) {
                finish(game, Outcome.WIN, last, "Toutes les autres équipes ont abandonné.");
                return;
            }
            game.setFrozen(true);
            drawVotes.proposeFinal(game, last, accepted -> {
                if (game.getState() != GameState.IN_PROGRESS) {
                    return;
                }
                if (accepted) {
                    finish(game, Outcome.NULLE, -1, "Toutes les autres équipes ont abandonné : nulle acceptée.");
                } else {
                    finish(game, Outcome.WIN, last, "Toutes les autres équipes ont abandonné.");
                }
            });
        }
    }

    /**
     * A appeler par ObjectiveValidationTask juste apres une validation (0.3.0) : en mode bingos, la 1re equipe qui
     * atteint le nombre de bingos demande gagne ; en blackout, la 1re qui valide toute la grille.
     */
    public void checkWin(BingoGame game, int teamNumber) {
        if (game.getState() != GameState.IN_PROGRESS || game.isFrozen() || game.getGrid() == null) {
            return;
        }
        BingoSettings settings = game.getSettings();
        if (settings.isBlackout()) {
            int total = game.getGrid().getSize() * game.getGrid().getSize();
            if (game.countValidated(teamNumber) >= total) {
                finish(game, Outcome.WIN, teamNumber, "L'équipe " + teamNumber + " a rempli toute la grille en premier.");
            }
        } else if (game.getScoreEngine().bingoCount(teamNumber) >= settings.bingosRequired()) {
            finish(game, Outcome.WIN, teamNumber, "L'équipe " + teamNumber + " a achevé ses " + settings.bingosRequired()
                    + " bingos en premier.");
        }
    }

    /** Chrono ecoule (mode bingos) : le meilleur score gagne ; meilleurs scores egaux = Egalite. Les equipes qui
     *  ont abandonne ne peuvent pas gagner. */
    private void endByTimeout(BingoGame game) {
        double best = -1;
        List<Integer> top = new ArrayList<>();
        for (BingoInstance instance : game.getInstances()) {
            if (instance.isFullyAbandoned()) {
                continue;
            }
            int team = instance.getTeam().getTeamNumber();
            double score = game.score(team);
            if (score > best + 1e-9) {
                best = score;
                top.clear();
                top.add(team);
            } else if (Math.abs(score - best) < 1e-9) {
                top.add(team);
            }
        }
        if (top.size() == 1) {
            finish(game, Outcome.WIN, top.get(0), "Temps écoulé : l'équipe " + top.get(0) + " a le meilleur score.");
        } else {
            finish(game, Outcome.EGALITE, -1, "Temps écoulé : égalité au score entre les équipes "
                    + top.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", ")) + " !");
        }
    }

    /**
     * Fin de partie avec resultat (0.3.0) : calcule les points finaux et les classements, les affiche a tous, puis
     * traitement commun (inventaires, salle d'attente, nettoyage).
     *
     * Victoire : bonus de victoire pour l'equipe gagnante, puis classement CUMULE - chaque equipe gagne ses points
     * + ceux de toutes les equipes classees derriere elle (demande explicite de LeKiwi06). Ordre : gagnante, puis
     * les autres par score, les equipes ayant abandonne toujours en dernier.
     * Egalite / nulle : chaque equipe ne garde que ses propres points d'equipe.
     */
    public void finish(BingoGame game, Outcome outcome, int winner, String reason) {
        if (game.getState() != GameState.IN_PROGRESS) {
            return;
        }
        if (drawVotes != null) {
            drawVotes.forget(game);
        }
        autoDrawHour.remove(game.getGameId());
        game.setFrozen(false);
        var engine = game.getScoreEngine();
        boolean win = outcome == Outcome.WIN;

        List<BingoInstance> order = new ArrayList<>(game.getInstances());
        order.sort((a, b) -> {
            int ta = a.getTeam().getTeamNumber();
            int tb = b.getTeam().getTeamNumber();
            if (win && ta == winner) {
                return -1;
            }
            if (win && tb == winner) {
                return 1;
            }
            if (a.isFullyAbandoned() != b.isFullyAbandoned()) {
                return a.isFullyAbandoned() ? 1 : -1;
            }
            return Double.compare(engine.teamScore(tb), engine.teamScore(ta));
        });
        Map<Integer, Double> own = new HashMap<>();
        for (BingoInstance instance : order) {
            int team = instance.getTeam().getTeamNumber();
            own.put(team, engine.teamScore(team, win && team == winner));
        }

        List<Component> summary = new ArrayList<>();
        String title = switch (outcome) {
            case WIN -> "Victoire de l'équipe " + winner;
            case EGALITE -> "Égalité";
            case NULLE -> "Match nul";
        };
        summary.add(Component.text("===== " + title + " =====", NamedTextColor.GOLD));
        summary.add(Component.text(reason, NamedTextColor.YELLOW));
        summary.add(Component.text(win ? "Classement des équipes (points + ceux des équipes derrière) :"
                : "Points des équipes (chacune garde ses propres points) :", NamedTextColor.AQUA));
        StringBuilder log = new StringBuilder("[KG_BingoGame] Partie '" + game.getGameId() + "' terminee (" + title + ") :");
        for (int i = 0; i < order.size(); i++) {
            BingoInstance instance = order.get(i);
            int team = instance.getTeam().getTeamNumber();
            double mine = own.get(team);
            double behind = 0;
            if (win) {
                for (int j = i + 1; j < order.size(); j++) {
                    behind += own.get(order.get(j).getTeam().getTeamNumber());
                }
            }
            String line = (win ? (i + 1) + ". " : "- ") + "Équipe " + team + " : " + ScoreEngine.format(mine + behind) + " pts"
                    + (behind > 0 ? " (" + ScoreEngine.format(mine) + " + " + ScoreEngine.format(behind) + ")" : "")
                    + (instance.isFullyAbandoned() ? " — abandon" : "");
            summary.add(Component.text(line, win && team == winner ? NamedTextColor.GREEN : NamedTextColor.WHITE));
            log.append(" equipe ").append(team).append('=').append(ScoreEngine.format(mine + behind));
        }
        List<String> solos = new ArrayList<>();
        List<Map.Entry<String, Double>> soloScores = new ArrayList<>();
        for (BingoInstance instance : game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            for (UUID playerId : instance.getTeam().getPlayers()) {
                String name = Bukkit.getOfflinePlayer(playerId).getName();
                soloScores.add(Map.entry(name != null ? name : playerId.toString().substring(0, 8),
                        engine.soloScore(team, playerId, win && team == winner)));
            }
        }
        soloScores.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (Map.Entry<String, Double> entry : soloScores) {
            solos.add(entry.getKey() + " " + ScoreEngine.format(entry.getValue()));
        }
        summary.add(Component.text("Points solo : " + String.join(", ", solos), NamedTextColor.GRAY));
        logger.info(log + " ; solo : " + String.join(", ", solos));

        finishAndSendToLobby(game, team -> switch (outcome) {
            case WIN -> team == winner
                    ? Component.text("Victoire ! Votre équipe remporte la partie.", NamedTextColor.GREEN)
                    : Component.text("L'équipe " + winner + " remporte la partie.", NamedTextColor.YELLOW);
            case EGALITE -> Component.text("Égalité ! La partie est terminée.", NamedTextColor.YELLOW);
            case NULLE -> Component.text("Match nul : la partie est terminée.", NamedTextColor.YELLOW);
        }, summary);
    }

    private void broadcast(BingoGame game, Component message) {
        for (BingoInstance instance : game.getInstances()) {
            for (UUID playerId : instance.getTeam().getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline() && !instance.hasAbandoned(playerId)) {
                    player.sendMessage(message);
                }
            }
        }
    }

    /**
     * Traitement COMMUN a la fin par victoire et par timeout (voir javadoc de la classe) : vide
     * entierement l'inventaire de chaque joueur en ligne, l'envoie dans la salle d'attente Bingo
     * avec la nether star (menu de retour), et programme le nettoyage differe. `messageForTeam`
     * ne fait varier que le texte affiche (victoire/defaite vs temps ecoule).
     */
    private void finishAndSendToLobby(BingoGame game, IntFunction<Component> messageForTeam, List<Component> summary) {
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
                    summary.forEach(player::sendMessage);
                    // Inventaire vide + point de spawn supprime (0.1.18, voir PlayerResetService).
                    playerReset.reset(player);
                    if (spawn != null) {
                        anyoneLingering = true;
                        // 0.3.0 : invincible des AVANT la teleportation et pendant tout le sejour en salle
                        // d'attente post-partie (demande explicite de LeKiwi06, 24/09/2026 : "rendre les
                        // joueurs invincibles dans le tp back lobby du fin de partie pour eviter la mort
                        // imprevue" - voir LobbyProtectionListener.onAnyDamage).
                        lingeringDeadlines.put(playerId, deadline);
                        lingeringGameId.put(playerId, game.getGameId());
                        makeSafe(player);
                        player.teleport(spawn);
                        lobbyItems.givePostGame(player);
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

    /** Retire tout ce qui pourrait encore tuer le joueur apres la teleportation (feu, chute en cours,
     *  poison/wither...) et remet vie et nourriture au maximum (0.3.0). */
    private static void makeSafe(Player player) {
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setVelocity(new org.bukkit.util.Vector());
        for (var effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        var maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        player.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
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
        logger.info("[KG_BingoGame] Partie '" + game.getGameId() + "' terminee (abandonnée, plus aucun joueur connecté depuis "
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
