package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pre-genere les mondes d'instance d'une partie DES LA CREATION DE LA SALLE D'ATTENTE (avant meme
 * que les equipes ne soient choisies, et bien avant le lancement reel de la partie - voir
 * PartyStarter) - demande explicite de l'utilisateur, 23/09/2026 : "a chaque partie il faut
 * générer de nouvelles maps donc il serait préférable de générer les maps directement après la
 * création de la partie et en cascade pour éviter les lags".
 *
 * Le nom de chaque monde (bingo_&lt;gameId&gt;_&lt;n&gt;) et la seed partagee sont deja connus des
 * la creation de la BingoParty (roster/equipes pas encore necessaires, voir GameManager.createGame
 * qui ne depend lui non plus d'aucune donnee joueur pour nommer/generer une instance) : rien
 * n'empeche de generer les mondes par avance, PENDANT que les joueurs choisissent encore leur
 * equipe dans la salle d'attente.
 *
 * Genere les mondes UN PAR UN, espaces de instances.pregeneration-stagger-seconds (au lieu de tous
 * generer d'un coup au lancement comme avant - voir InstanceWorldManager, "couteux en CPU/disque
 * au lancement d'une partie") : etale la charge sur plusieurs secondes/minutes plutot que de la
 * concentrer sur un seul tick.
 *
 * Repli automatique si l'hote lance la partie avant la fin de la pre-generation en cascade (voir
 * claim() / GameManager.prepareInstances) : generation synchrone classique pour les instances pas
 * encore pretes, comme avant cette classe - le lancement d'une partie n'attend donc jamais la
 * pre-generation.
 *
 * TERRAIN (0.1.18) : creer le monde ne genere que la petite zone de spawn - le reste du terrain etait
 * genere a la volee pendant que les joueurs exploraient, d'ou le lag signale par l'utilisateur ("les
 * maps ne sont toujours pas générées à l'avance, il faut que la génération des maps commence dès
 * l'arrivée dans la salle d'attente"). Des qu'un monde est cree, les chunks dans un rayon de
 * instances.pregeneration-radius-blocks (200 par defaut, choisi via AskUserQuestion) autour de son
 * spawn sont generes a leur tour, du centre vers l'exterieur, en asynchrone (API Paper
 * getChunkAtAsync) et au plus CHUNKS_IN_FLIGHT a la fois pour ne pas saturer le serveur. Si la
 * partie demarre avant la fin, la generation continue simplement en arriere-plan.
 *
 * Limite connue, non encore traitee (meme categorie que InstanceWorldManager) : la pre-generation
 * ne tient PAS compte de instances.max-simultaneous-games (ce garde-fou s'applique au LANCEMENT
 * reel d'une partie, voir GameManager.createGame) - plusieurs salles d'attente formees en meme
 * temps peuvent donc chacune declencher leur propre pre-generation en parallele.
 */
public final class InstanceWorldPreparer {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final InstanceWorldManager worldManager;
    private final String worldNamePrefix;
    private final long staggerTicks;
    private final int radiusChunks;

    /** Nombre maximum de chunks demandes en meme temps par monde (voir pregenerateChunks). */
    private static final int CHUNKS_IN_FLIGHT = 4;

    /** Mondes deja pre-generes, en attente d'etre reclames par le vrai lancement de la partie (voir
     *  claim ci-dessous) - nom du monde -> World. Un monde reclame est retire de ce cache. */
    private final Map<String, World> ready = new HashMap<>();

    /** gameId des parties annulees (voir cancel()) AVANT la fin de leur pre-generation : les mondes
     *  restant a generer pour ce gameId sont supprimes des qu'ils sont prets plutot que reclames. */
    private final Set<String> cancelledGameIds = new HashSet<>();

    public InstanceWorldPreparer(JavaPlugin plugin, Logger logger, InstanceWorldManager worldManager,
                                  String worldNamePrefix, Duration stagger, int radiusBlocks) {
        this.plugin = plugin;
        this.logger = logger;
        this.worldManager = worldManager;
        this.worldNamePrefix = worldNamePrefix;
        this.staggerTicks = Math.max(1L, stagger.getSeconds() * 20L);
        this.radiusChunks = radiusBlocks <= 0 ? 0 : (radiusBlocks + 15) / 16;
    }

    /** A appeler UNE SEULE FOIS, des la creation d'une nouvelle BingoParty (voir PartyManager). */
    public void startPreGeneration(String gameId, long seed, int teamCount) {
        generateNext(gameId, seed, teamCount, 1);
    }

    /**
     * Cascade (0.1.22) : d'abord les overworlds de toutes les equipes (indispensables au lancement), puis leurs
     * Nether, puis leurs End - chaque monde espace du delai configure. Le Nether et l'End de chaque equipe sont
     * generes avec la meme seed que son overworld (demande explicite de l'utilisateur : "le nether et l'end
     * doivent etre generes comme l'overworld (chacun le sien)"). La cascade continue apres le lancement de la
     * partie ; si un joueur prend un portail avant que "son" Nether / End soit pret, il est genere a ce moment-la
     * (voir DimensionPortalListener).
     */
    private void generateNext(String gameId, long seed, int teamCount, int index) {
        if (index > teamCount * 3) {
            return;
        }
        // Le premier monde est genere presque immediatement (1 tick, juste le temps de sortir de
        // l'appel reseau en cours) ; les suivants sont espaces du delai configure.
        long delay = index == 1 ? 1L : staggerTicks;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cancelledGameIds.contains(gameId)) {
                return; // partie annulee / terminee entre-temps : n'en genere pas plus
            }
            int team = (index - 1) % teamCount + 1;
            int step = (index - 1) / teamCount; // 0 = overworld, 1 = nether, 2 = end
            String worldName = worldNamePrefix + gameId + "_" + team;
            try {
                if (step == 0) {
                    World world = worldManager.createInstanceWorld(worldName, seed);
                    if (cancelledGameIds.contains(gameId)) {
                        // Annulee PENDANT cette generation : supprime immediatement plutot que de
                        // laisser un monde orphelin sur le disque.
                        worldManager.deleteInstanceWorld(worldName);
                    } else {
                        ready.put(worldName, world);
                        pregenerateChunks(world, gameId);
                    }
                } else {
                    World.Environment environment = step == 1 ? World.Environment.NETHER : World.Environment.THE_END;
                    World world = worldManager.createDimension(worldName, environment, seed);
                    if (step == 1) {
                        // Autour de l'equivalent Nether du spawn de l'overworld (coordonnees divisees par 8).
                        World overworld = Bukkit.getWorld(worldName);
                        Location spawn = overworld != null ? overworld.getSpawnLocation() : world.getSpawnLocation();
                        pregenerateChunks(world, gameId, (spawn.getBlockX() / 8) >> 4, (spawn.getBlockZ() / 8) >> 4);
                    } else {
                        pregenerateChunks(world, gameId, 0, 0); // ile principale de l'End
                    }
                }
            } catch (RuntimeException e) {
                logger.warning("[KG_BingoGame] Pré-génération du monde '" + worldName + "' (étape " + step + ") échouée : " + e.getMessage());
            }
            generateNext(gameId, seed, teamCount, index + 1);
        }, delay);
    }

    /**
     * Genere le terrain autour du spawn de ce monde (voir en-tete de classe). Public : egalement
     * appele par GameManager.prepareInstances pour un monde cree au lancement faute d'avoir ete
     * pret a temps. gameId peut etre null (pas de suivi d'annulation dans ce cas).
     */
    public void pregenerateChunks(World world, String gameId) {
        Location spawn = world.getSpawnLocation();
        pregenerateChunks(world, gameId, spawn.getBlockX() >> 4, spawn.getBlockZ() >> 4);
    }

    /** Idem autour d'un chunk donne (Nether / End, 0.1.22). */
    public void pregenerateChunks(World world, String gameId, int centerX, int centerZ) {
        if (radiusChunks <= 0) {
            return;
        }
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{centerX, centerZ});
        for (int r = 1; r <= radiusChunks; r++) { // anneaux successifs : les chunks proches du spawn d'abord
            for (int dx = -r; dx <= r; dx++) {
                queue.add(new int[]{centerX + dx, centerZ - r});
                queue.add(new int[]{centerX + dx, centerZ + r});
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                queue.add(new int[]{centerX - r, centerZ + dz});
                queue.add(new int[]{centerX + r, centerZ + dz});
            }
        }
        new ChunkJob(world, gameId, queue).pump();
    }

    /** Une pre-generation de terrain en cours pour UN monde. Toujours manipule sur le thread principal. */
    private final class ChunkJob {
        private final World world;
        private final String worldName;
        private final String gameId;
        private final Deque<int[]> queue;
        private final int total;
        private final long startedAt = System.currentTimeMillis();
        private int inFlight;
        private int done;
        private boolean stopped;

        ChunkJob(World world, String gameId, Deque<int[]> queue) {
            this.world = world;
            this.worldName = world.getName();
            this.gameId = gameId;
            this.queue = queue;
            this.total = queue.size();
            logger.info("[KG_BingoGame] Pré-génération du terrain de '" + worldName + "' : " + total + " chunks (rayon "
                    + radiusChunks * 16 + " blocs).");
        }

        void pump() {
            while (!stopped && inFlight < CHUNKS_IN_FLIGHT && !queue.isEmpty()) {
                if (Bukkit.getWorld(worldName) == null || (gameId != null && cancelledGameIds.contains(gameId))) {
                    stopped = true; // monde supprime (partie annulee/terminee) : on arrete
                    queue.clear();
                    return;
                }
                int[] chunk = queue.poll();
                inFlight++;
                world.getChunkAtAsync(chunk[0], chunk[1], true).whenComplete((c, error) -> {
                    if (Bukkit.isPrimaryThread()) {
                        onChunkDone();
                    } else {
                        Bukkit.getScheduler().runTask(plugin, this::onChunkDone);
                    }
                });
            }
        }

        private void onChunkDone() {
            inFlight--;
            done++;
            if (!stopped && done == total) {
                logger.info("[KG_BingoGame] Terrain de '" + worldName + "' pré-généré (" + total + " chunks en "
                        + (System.currentTimeMillis() - startedAt) / 1000 + " s).");
                return;
            }
            pump();
        }
    }

    /**
     * Reclame un monde pre-genere pour cette instance (voir GameManager.prepareInstances) - null
     * s'il n'est pas encore pret, auquel cas l'appelant doit generer ce monde lui-meme (repli
     * synchrone habituel).
     */
    public World claim(String worldName) {
        return ready.remove(worldName);
    }

    /**
     * Partie reellement lancee (voir GameManager.prepareInstances, appele une fois toutes les
     * instances reclamees/generees) : plus besoin de suivre une eventuelle annulation, libere la
     * memoire.
     */
    public void forget(String gameId) {
        cancelledGameIds.remove(gameId);
    }

    /**
     * Partie annulee AVANT son lancement (voir PartyCanceller) : empeche toute generation restante
     * et supprime immediatement les mondes deja pre-generes pour ce gameId (jamais reclames par une
     * vraie instance de jeu, autrement ils auraient deja ete retires de `ready` par claim()).
     */
    public void cancel(String gameId) {
        cancelledGameIds.add(gameId);
        String prefix = worldNamePrefix + gameId + "_";
        Iterator<Map.Entry<String, World>> it = ready.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, World> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                worldManager.deleteInstanceWorld(entry.getKey());
                it.remove();
            }
        }
    }
}
