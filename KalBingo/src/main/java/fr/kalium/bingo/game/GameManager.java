package fr.kalium.bingo.game;

import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.grid.GridGenerator;
import fr.kalium.bingo.grid.ObjectiveLibrary;
import fr.kalium.bingo.world.InstanceWorldManager;
import fr.kalium.bingo.world.InstanceWorldPreparer;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Gere le cycle de vie des parties de Bingo (section 1) :
 *  - creation/preparation des instances (etape 1), une instance par EQUIPE
 *    (1 a 4 equipes, 1 a 4 joueurs par equipe - confirme par l'utilisateur)
 *  - attribution des equipes aux instances (etape 2)
 *  - generation de la grille d'objectifs de la partie (section 2, etape 4
 *    de l'ordre de priorite - voir assignGrid)
 *  - suivi des deconnexions pendant une partie en cours (detection
 *    seulement - l'action a prendre en cas d'abandon n'est pas encore
 *    definie par le cahier des charges, voir config.yml)
 *
 * Ne gere PAS encore : validation des objectifs, victoire, chronometre
 * visible, transfert reseau (voir fr.kalium.bingo.network) - briques
 * suivantes de l'ordre de priorite (section 16). La formation des equipes
 * elle-meme (choix manuel / equipe aleatoire) a lieu cote kal-games, en
 * amont : ce gestionnaire recoit des equipes deja formees.
 *
 * activeGames survit desormais a un redemarrage/crash du serveur (voir GamePersistence et
 * restoreGame ci-dessous) - demande explicite de l'utilisateur, 23/09/2026. Portee volontairement
 * limitee aux parties DEJA LANCEES (IN_PROGRESS) : une salle d'attente pas encore lancee (choix des
 * equipes en cours, voir PartyManager/BingoParty) repart de zero si le serveur redemarre avant le
 * lancement - choix confirme via AskUserQuestion.
 */
public class GameManager {

    private final Logger logger;
    private final InstanceWorldManager worldManager;
    private final InstanceWorldPreparer instanceWorldPreparer;
    private final ObjectiveLibrary objectiveLibrary;
    private final GridGenerator gridGenerator;

    /** Reglages issus de config.yml (section "teams" / "game" / "instances" / "grid"), voir BingoPlugin. */
    private final int maxTeams;
    private final int maxTeamSize;
    private final Duration defaultDuration;
    private final String worldNamePrefix;
    private final Duration abandonAfter;
    private final int maxSimultaneousGames;
    private final int gridSize;

    private final Map<String, BingoGame> activeGames = new HashMap<>();

    /** Pour chaque joueur actuellement deconnecte en cours de partie : instant de deconnexion. */
    private final Map<UUID, Instant> disconnectedSince = new HashMap<>();

    public GameManager(Logger logger, InstanceWorldManager worldManager, InstanceWorldPreparer instanceWorldPreparer,
                        ObjectiveLibrary objectiveLibrary, GridGenerator gridGenerator,
                        int maxTeams, int maxTeamSize, Duration defaultDuration,
                        String worldNamePrefix, Duration abandonAfter, int maxSimultaneousGames, int gridSize) {
        this.logger = logger;
        this.worldManager = worldManager;
        this.instanceWorldPreparer = instanceWorldPreparer;
        this.objectiveLibrary = objectiveLibrary;
        this.gridGenerator = gridGenerator;
        this.maxTeams = maxTeams;
        this.maxTeamSize = maxTeamSize;
        this.defaultDuration = defaultDuration;
        this.worldNamePrefix = worldNamePrefix;
        this.abandonAfter = abandonAfter;
        this.maxSimultaneousGames = maxSimultaneousGames;
        this.gridSize = Math.max(1, gridSize);
    }

    /**
     * Duree par defaut (config.yml : game.default-duration-seconds, 1h).
     * Ne sert QUE de repli si aucune duree n'est transmise pour une partie
     * donnee (ex. commande admin de test) : le reglage modifiable par les
     * admins vivra cote kal-games (parametres du mini-jeu, meme principe
     * que les autres mini-jeux) et sera transmis pour chaque partie - voir
     * @createGame. Ce plugin n'impose donc plus de plafond local.
     */
    public Duration getDefaultDuration() {
        return defaultDuration;
    }

    /**
     * Cree une partie et prepare une instance par EQUIPE, toutes avec la
     * meme seed (section 0 : "toutes les instances d'une meme partie
     * utilisent exactement la meme seed").
     *
     * @param gameId   identifiant unique de la partie
     * @param seed     seed partagee par toutes les instances
     * @param duration duree de la partie, telle que decidee/configuree cote
     *                 kal-games pour cette partie (pas de plafond impose ici)
     * @param teams    equipes deja formees (1 a maxTeams equipes, chacune de
     *                 1 a maxTeamSize joueurs), dans l'ordre recu
     */
    public BingoGame createGame(String gameId, long seed, Duration duration, List<List<UUID>> teams) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Duree de partie invalide.");
        }
        if (activeGames.containsKey(gameId)) {
            throw new IllegalArgumentException("Une partie '" + gameId + "' existe deja.");
        }
        if (activeGames.size() >= maxSimultaneousGames) {
            throw new IllegalStateException(
                    "Nombre maximum de parties Bingo simultanees atteint (" + maxSimultaneousGames
                            + ", voir instances.max-simultaneous-games).");
        }
        if (teams == null || teams.isEmpty()) {
            throw new IllegalArgumentException("Une partie doit contenir au moins une equipe.");
        }
        if (teams.size() > maxTeams) {
            throw new IllegalArgumentException(
                    "Trop d'equipes (" + teams.size() + " > " + maxTeams + ", voir teams.max-teams).");
        }

        Set<UUID> seenPlayers = new HashSet<>();
        for (List<UUID> team : teams) {
            if (team == null || team.isEmpty()) {
                throw new IllegalArgumentException("Une equipe ne peut pas etre vide.");
            }
            if (team.size() > maxTeamSize) {
                throw new IllegalArgumentException(
                        "Equipe trop nombreuse (" + team.size() + " > " + maxTeamSize + ", voir teams.max-team-size).");
            }
            for (UUID playerId : team) {
                if (!seenPlayers.add(playerId)) {
                    throw new IllegalArgumentException(
                            "Le joueur " + playerId + " apparait dans plusieurs equipes (section 7 : un joueur = une seule instance).");
                }
            }
        }

        BingoGame game = new BingoGame(gameId, seed, duration);

        int index = 1;
        for (List<UUID> teamPlayers : teams) {
            BingoTeam team = new BingoTeam(index, teamPlayers);
            String worldName = worldNamePrefix + gameId + "_" + index;
            BingoInstance instance = new BingoInstance(worldName, team);
            game.addInstance(instance);
            index++;
        }

        activeGames.put(gameId, game);
        return game;
    }

    /**
     * Etape 1 : genere effectivement les mondes de toutes les instances de la partie - ou plutot
     * RECLAME ceux deja pre-generes en cascade des la creation de la salle d'attente (voir
     * InstanceWorldPreparer), avec repli sur la generation synchrone habituelle pour une instance
     * pas encore prete (hote ayant lance la partie avant la fin de la pre-generation).
     */
    public void prepareInstances(BingoGame game) {
        for (BingoInstance instance : game.getInstances()) {
            World world = instanceWorldPreparer.claim(instance.getInstanceId());
            if (world == null) {
                world = worldManager.createInstanceWorld(instance.getInstanceId(), game.getSeed());
                // Pas pret a temps : le terrain autour du spawn est genere en arriere-plan (0.1.18).
                instanceWorldPreparer.pregenerateChunks(world, null);
            }
            instance.setWorld(world);
            Location spawn = worldManager.getNaturalSpawn(world);
            instance.setSpawnLocation(spawn);
        }
        instanceWorldPreparer.forget(game.getGameId());
    }

    /**
     * Reconstruit une partie EN COURS depuis la persistance (voir GamePersistence), apres un
     * redemarrage/crash du serveur (demande explicite de l'utilisateur, 23/09/2026 : "la partie
     * doit continuer meme si le serveur est redémarré ou si il crash"). Recree les instances en
     * RECHARGEANT les mondes existants sur le disque (InstanceWorldManager.createInstanceWorld
     * charge un monde deja present au lieu d'en generer un nouveau - meme appel que pour une
     * creation normale, voir sa javadoc), sans repasser par createGame (pas de nouvelle seed a
     * tirer, pas de verification de limite de parties simultanees : cette partie existait deja
     * avant l'arret, elle ne compte pas comme une partie EN PLUS). Ne restaure PAS la grille ni la
     * progression : a la charge de l'appelant (voir GamePersistence.loadAll), qui a acces aux types
     * du package grid.
     */
    public BingoGame restoreGame(String gameId, long seed, Duration duration, List<RestoredInstance> instances) {
        if (activeGames.containsKey(gameId)) {
            throw new IllegalStateException("Une partie '" + gameId + "' existe deja.");
        }
        BingoGame game = new BingoGame(gameId, seed, duration);
        for (RestoredInstance restored : instances) {
            BingoTeam team = new BingoTeam(restored.teamNumber(), restored.players());
            BingoInstance instance = new BingoInstance(restored.worldName(), team);
            World world = worldManager.createInstanceWorld(restored.worldName(), seed);
            worldManager.loadDimensionsIfPresent(restored.worldName(), seed); // 0.1.22 : Nether / End de l'equipe
            instance.setWorld(world);
            instance.setSpawnLocation(worldManager.getNaturalSpawn(world));
            game.addInstance(instance);
        }
        activeGames.put(gameId, game);
        return game;
    }

    /** Une instance telle que persistee par GamePersistence - voir restoreGame ci-dessus. */
    public record RestoredInstance(int teamNumber, List<UUID> players, String worldName) {
    }

    /**
     * Etape 5 (section 1) / section 2 : genere la grille d'objectifs de la partie et l'attache
     * au BingoGame (une seule grille, partagee par toutes les equipes - voir BingoGrid). A
     * appeler apres createGame/prepareInstances (voir PartyStarter).
     *
     * @throws IllegalStateException si objectives.yml ne contient pas assez d'objectifs pour
     *                                la taille de grille configuree (grid.size)
     */
    public void assignGrid(BingoGame game) {
        BingoGrid grid = gridGenerator.generate(gridSize, objectiveLibrary.all());
        game.setGrid(grid);
    }

    /** Etape 12 : suppression des instances utilisees en fin de partie. */
    public void cleanupGame(String gameId) {
        BingoGame game = activeGames.get(gameId);
        if (game == null) {
            return;
        }
        // 0.1.22 : la cascade de pre-generation (Nether / End) peut encore tourner - elle est arretee.
        instanceWorldPreparer.cancel(gameId);
        for (BingoInstance instance : game.getInstances()) {
            worldManager.deleteInstanceWorld(instance.getInstanceId());
        }
        activeGames.remove(gameId);
    }

    /**
     * Partie EN COURS et instance a laquelle appartient ce monde (overworld, Nether ou End d'une equipe -
     * 0.1.22), ou null.
     */
    public InstanceRef findInstanceByWorld(World world) {
        if (world == null) {
            return null;
        }
        String base = InstanceWorldManager.baseNameOf(world.getName());
        for (BingoGame game : activeGames.values()) {
            for (BingoInstance instance : game.getInstances()) {
                if (instance.getInstanceId().equals(base)) {
                    return new InstanceRef(game, instance);
                }
            }
        }
        return null;
    }

    public record InstanceRef(BingoGame game, BingoInstance instance) {
    }

    /** true si ce monde est l'overworld, le Nether ou l'End de cette instance (0.1.22). */
    public static boolean belongsTo(World world, BingoInstance instance) {
        return world != null && instance != null
                && InstanceWorldManager.baseNameOf(world.getName()).equals(instance.getInstanceId());
    }

    /** Nether / End de l'instance, genere a la demande s'il n'est pas encore pret (0.1.22). */
    public World dimensionOf(BingoGame game, BingoInstance instance, World.Environment environment) {
        return worldManager.createDimension(instance.getInstanceId(), environment, game.getSeed());
    }

    public BingoGame getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public List<BingoGame> getActiveGames() {
        return new ArrayList<>(activeGames.values());
    }

    /**
     * Cherche, parmi TOUTES les parties en cours, celle a laquelle ce joueur appartient (une
     * partie EN COURS, deja lancee - pas une BingoParty en salle d'attente, voir PartyManager).
     * Utilise pour distinguer une reconnexion en cours de partie (le joueur garde son instance)
     * d'une toute nouvelle arrivee (voir PlayerConnectListener).
     */
    public Optional<BingoGame> findGameOf(UUID playerId) {
        for (BingoGame game : activeGames.values()) {
            Optional<BingoInstance> instance = game.findInstanceOf(playerId);
            // Un joueur ayant volontairement abandonne (voir AbandonService/BingoInstance.
            // markAbandoned, 24/09/2026) reste dans le roster de son equipe mais ne doit PLUS
            // jamais etre traite comme "encore dans la partie" - sinon une reconnexion ulterieure
            // (PlayerConnectListener) le renverrait a tort dans une partie qu'il a quittee pour de bon.
            if (instance.isPresent() && !instance.get().hasAbandoned(playerId)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    // --- Suivi des deconnexions pendant une partie en cours ---

    public void onPlayerDisconnect(UUID playerId) {
        disconnectedSince.put(playerId, Instant.now());
    }

    public void onPlayerReconnect(UUID playerId) {
        disconnectedSince.remove(playerId);
    }

    /**
     * A appeler periodiquement (tache planifiee) pour detecter les
     * abandons : au-dela du delai configure (reconnect-during-game.abandon-after-seconds)
     * de deconnexion consecutive pendant une partie EN COURS, le joueur est
     * considere comme ayant abandonne. L'action a prendre en cas d'abandon
     * n'est pas encore precisee dans le cahier des charges - a valider
     * avant de l'implementer (voir config.yml).
     */
    public boolean hasAbandoned(UUID playerId) {
        Instant since = disconnectedSince.get(playerId);
        if (since == null) {
            return false;
        }
        return Duration.between(since, Instant.now()).compareTo(abandonAfter) >= 0;
    }
}
