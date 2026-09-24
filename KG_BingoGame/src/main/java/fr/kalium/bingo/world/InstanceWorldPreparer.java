package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
 * 0.6.0 - FILE D'ATTENTE UNIQUE POUR TOUT LE SERVEUR (demande de LeKiwi06, 24/09/2026 : "le bingo lag [...]
 * on aimerait pouvoir faire plusieurs games en simultanée sans que ça crash" ; "ralentir la vitesse de
 * génération, pas grave si la game met 5 minutes a se lancer, on veux que ce soit fluide"). Avant, chaque
 * partie avait sa propre cascade et chaque monde pre-generait 4 chunks a la fois : 4 parties = jusqu'a 48
 * mondes et 192 chunks generes en parallele. Desormais, toutes parties confondues :
 *  - un seul monde est cree a la fois, espace de instances.pregeneration-stagger-seconds du precedent ; les
 *    overworlds (indispensables au lancement) passent toujours avant les Nether / End ;
 *  - le terrain est genere a un debit total limite (instances.pregeneration-chunks-per-second) avec au plus
 *    instances.pregeneration-max-chunks-in-flight chunks en cours ; terrain des overworlds d'abord.
 * Le lancement d'une partie attend que les overworlds de ses equipes et leur terrain soient prets (voir
 * mapsReady / PartyStarter) au lieu de generer les mondes manquants d'un coup.
 *
 * TERRAIN (0.1.18) : creer le monde ne genere que la petite zone de spawn - le reste du terrain etait
 * genere a la volee pendant que les joueurs exploraient, d'ou le lag signale par l'utilisateur. Des qu'un
 * monde est cree, les chunks dans un rayon de instances.pregeneration-radius-blocks (200 par defaut) autour
 * de son spawn sont generes a leur tour, du centre vers l'exterieur, en asynchrone (getChunkAtAsync). Si la
 * partie demarre avant la fin (Nether / End), la generation continue simplement en arriere-plan.
 */
public final class InstanceWorldPreparer {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final InstanceWorldManager worldManager;
    private final String worldNamePrefix;
    private final long staggerTicks;
    private final int radiusChunks;
    private final double chunksPerTick;
    private final int maxChunksInFlight;

    /** Mondes deja pre-generes, en attente d'etre reclames par le vrai lancement de la partie (voir
     *  claim ci-dessous) - nom du monde -> World. Un monde reclame est retire de ce cache. */
    private final Map<String, World> ready = new HashMap<>();

    /** gameId des parties annulees (voir cancel()) AVANT la fin de leur pre-generation : les mondes
     *  restant a generer pour ce gameId sont supprimes des qu'ils sont prets plutot que reclames. */
    private final Set<String> cancelledGameIds = new HashSet<>();

    /** Mondes a creer, toutes parties confondues : les overworlds passent toujours avant les Nether / End. */
    private final Deque<WorldTask> overworldQueue = new ArrayDeque<>();
    private final Deque<WorldTask> dimensionQueue = new ArrayDeque<>();

    /** Terrains en cours de pre-generation, dans l'ordre d'arrivee (les overworlds sont servis en premier). */
    private final List<ChunkJob> jobs = new ArrayList<>();

    /** Suivi des overworlds pour le lancement (voir mapsReady) : terrain en cours, termine, ou creation echouee. */
    private final Map<String, ChunkJob> overworldJobs = new HashMap<>();
    private final Set<String> terrainDone = new HashSet<>();
    private final Set<String> failed = new HashSet<>();

    private long tick;
    private long nextCreationTick;
    private double chunkCredit;
    private int chunksInFlight;

    public InstanceWorldPreparer(JavaPlugin plugin, Logger logger, InstanceWorldManager worldManager,
                                  String worldNamePrefix, Duration stagger, int radiusBlocks,
                                  int chunksPerSecond, int maxChunksInFlight) {
        this.plugin = plugin;
        this.logger = logger;
        this.worldManager = worldManager;
        this.worldNamePrefix = worldNamePrefix;
        this.staggerTicks = Math.max(1L, stagger.getSeconds() * 20L);
        this.radiusChunks = radiusBlocks <= 0 ? 0 : (radiusBlocks + 15) / 16;
        this.chunksPerTick = Math.max(1, chunksPerSecond) / 20.0;
        this.maxChunksInFlight = Math.max(1, maxChunksInFlight);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** A appeler UNE SEULE FOIS, des la creation d'une nouvelle BingoParty (voir PartyManager). */
    public void startPreGeneration(String gameId, long seed, int teamCount) {
        // Ordre (0.1.22) : overworlds de toutes les equipes, puis leurs Nether, puis leurs End.
        for (int team = 1; team <= teamCount; team++) {
            overworldQueue.add(new WorldTask(gameId, seed, team, World.Environment.NORMAL));
        }
        for (World.Environment environment : new World.Environment[] {World.Environment.NETHER, World.Environment.THE_END}) {
            for (int team = 1; team <= teamCount; team++) {
                dimensionQueue.add(new WorldTask(gameId, seed, team, environment));
            }
        }
    }

    /**
     * Garde-fou (0.6.0) : met en file les overworlds des equipes 1 a teamCount qui ne sont ni crees, ni en
     * attente, ni en echec - sinon le lancement, qui attend ses maps (voir mapsReady), attendrait indefiniment.
     */
    public void ensureQueued(String gameId, long seed, int teamCount) {
        for (int team = 1; team <= teamCount; team++) {
            String name = worldName(gameId, team);
            int t = team;
            boolean known = ready.containsKey(name) || failed.contains(name)
                    || overworldQueue.stream().anyMatch(task -> task.gameId().equals(gameId) && task.team() == t);
            if (!known) {
                overworldQueue.add(new WorldTask(gameId, seed, team, World.Environment.NORMAL));
            }
        }
    }

    /** Une creation de monde en attente. */
    private record WorldTask(String gameId, long seed, int team, World.Environment environment) {
    }

    private String worldName(String gameId, int team) {
        return worldNamePrefix + gameId + "_" + team;
    }

    private void tick() {
        tick++;
        if (tick >= nextCreationTick) {
            WorldTask task = nextWorldTask();
            if (task != null) {
                create(task);
                nextCreationTick = tick + staggerTicks;
            }
        }
        pumpChunks();
    }

    private WorldTask nextWorldTask() {
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            while (!queue.isEmpty()) {
                WorldTask task = queue.poll();
                if (!cancelledGameIds.contains(task.gameId())) {
                    return task;
                }
            }
        }
        return null;
    }

    /**
     * Cree un monde de la file. Le Nether et l'End de chaque equipe sont generes avec la meme seed que son
     * overworld (demande explicite de l'utilisateur : "le nether et l'end doivent etre generes comme
     * l'overworld (chacun le sien)"). Si un joueur prend un portail avant que "son" Nether / End soit pret,
     * il est genere a ce moment-la (voir DimensionPortalListener).
     */
    private void create(WorldTask task) {
        String gameId = task.gameId();
        String worldName = worldName(gameId, task.team());
        try {
            if (task.environment() == World.Environment.NORMAL) {
                World world = worldManager.createInstanceWorld(worldName, task.seed());
                if (cancelledGameIds.contains(gameId)) {
                    // Annulee PENDANT cette generation : supprime immediatement plutot que de
                    // laisser un monde orphelin sur le disque.
                    worldManager.deleteInstanceWorld(worldName);
                } else {
                    ready.put(worldName, world);
                    ChunkJob job = pregenerateChunks(world, gameId);
                    if (job != null) {
                        overworldJobs.put(worldName, job);
                    } else {
                        terrainDone.add(worldName);
                    }
                }
            } else {
                World world = worldManager.createDimension(worldName, task.environment(), task.seed());
                if (task.environment() == World.Environment.NETHER) {
                    // Autour de l'equivalent Nether du spawn de l'overworld (coordonnees divisees par 8).
                    World overworld = Bukkit.getWorld(worldName);
                    Location spawn = overworld != null ? overworld.getSpawnLocation() : world.getSpawnLocation();
                    pregenerateChunks(world, gameId, (spawn.getBlockX() / 8) >> 4, (spawn.getBlockZ() / 8) >> 4, false);
                } else {
                    pregenerateChunks(world, gameId, 0, 0, false); // ile principale de l'End
                }
            }
        } catch (RuntimeException e) {
            logger.warning("[KG_BingoGame] Pré-génération du monde '" + worldName + "' (" + task.environment()
                    + ") échouée : " + e.getMessage());
            if (task.environment() == World.Environment.NORMAL) {
                failed.add(worldName); // le lancement ne l'attend pas : repli de GameManager.prepareInstances
            }
        }
    }

    /**
     * Genere le terrain autour du spawn de ce monde (voir en-tete de classe). Public : egalement
     * appele par GameManager.prepareInstances pour un monde cree au lancement faute d'avoir ete
     * pret a temps. gameId peut etre null (pas de suivi d'annulation dans ce cas).
     *
     * @return la pre-generation ajoutee a la file, ou null si le rayon est nul
     */
    public ChunkJob pregenerateChunks(World world, String gameId) {
        Location spawn = world.getSpawnLocation();
        return pregenerateChunks(world, gameId, spawn.getBlockX() >> 4, spawn.getBlockZ() >> 4, true);
    }

    private ChunkJob pregenerateChunks(World world, String gameId, int centerX, int centerZ, boolean overworld) {
        if (radiusChunks <= 0) {
            return null;
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
        ChunkJob job = new ChunkJob(world, gameId, queue, overworld);
        jobs.add(job);
        return job;
    }

    /** Demande de nouveaux chunks dans la limite du debit et du nombre de chunks en cours (voir en-tete). */
    private void pumpChunks() {
        chunkCredit = Math.min(chunkCredit + chunksPerTick, Math.max(1.0, chunksPerTick));
        while (chunkCredit >= 1.0 && chunksInFlight < maxChunksInFlight) {
            ChunkJob job = nextJob();
            if (job == null) {
                return;
            }
            job.requestNext();
            chunkCredit -= 1.0;
        }
    }

    /** Prochain terrain a servir : les overworlds d'abord, puis Nether / End, dans l'ordre d'arrivee. */
    private ChunkJob nextJob() {
        jobs.removeIf(ChunkJob::exhausted);
        for (ChunkJob job : jobs) {
            if (job.overworld) {
                return job;
            }
        }
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    /** Une pre-generation de terrain pour UN monde. Toujours manipule sur le thread principal. */
    public final class ChunkJob {
        private final World world;
        private final String worldName;
        private final String gameId;
        private final Deque<int[]> queue;
        private final boolean overworld;
        private final int total;
        private final long startedAt = System.currentTimeMillis();
        private int done;
        private boolean stopped;

        ChunkJob(World world, String gameId, Deque<int[]> queue, boolean overworld) {
            this.world = world;
            this.worldName = world.getName();
            this.gameId = gameId;
            this.queue = queue;
            this.overworld = overworld;
            this.total = queue.size();
            logger.info("[KG_BingoGame] Pré-génération du terrain de '" + worldName + "' : " + total + " chunks (rayon "
                    + radiusChunks * 16 + " blocs).");
        }

        /** true s'il n'y a plus rien a demander (tout demande, ou monde supprime / partie annulee). */
        boolean exhausted() {
            if (!stopped && (Bukkit.getWorld(worldName) == null || (gameId != null && cancelledGameIds.contains(gameId)))) {
                stopped = true; // monde supprime (partie annulee/terminee) : on arrete
                queue.clear();
            }
            return queue.isEmpty();
        }

        void requestNext() {
            int[] chunk = queue.poll();
            chunksInFlight++;
            world.getChunkAtAsync(chunk[0], chunk[1], true).whenComplete((c, error) -> {
                if (Bukkit.isPrimaryThread()) {
                    onChunkDone();
                } else {
                    Bukkit.getScheduler().runTask(plugin, this::onChunkDone);
                }
            });
        }

        private void onChunkDone() {
            chunksInFlight--;
            done++;
            if (!stopped && done == total) {
                if (overworld) {
                    terrainDone.add(worldName);
                }
                logger.info("[KG_BingoGame] Terrain de '" + worldName + "' pré-généré (" + total + " chunks en "
                        + (System.currentTimeMillis() - startedAt) / 1000 + " s).");
            }
        }
    }

    /**
     * true quand les overworlds des equipes 1 a teamCount de cette partie sont crees ET leur terrain
     * pre-genere (voir PartyStarter : le lancement attend). Un overworld dont la creation a echoue ne
     * bloque pas le lancement (GameManager.prepareInstances le genere alors lui-meme).
     */
    public boolean mapsReady(String gameId, int teamCount) {
        for (int team = 1; team <= teamCount; team++) {
            String name = worldName(gameId, team);
            if (failed.contains(name)) {
                continue;
            }
            if (!ready.containsKey(name) || !terrainDone.contains(name)) {
                return false;
            }
        }
        return true;
    }

    /** Avancement (0 a 100) de la preparation des overworlds des equipes 1 a teamCount (voir mapsReady). */
    public int progressPercent(String gameId, int teamCount) {
        if (teamCount <= 0) {
            return 100;
        }
        int perWorld = radiusChunks <= 0 ? 1 : (2 * radiusChunks + 1) * (2 * radiusChunks + 1);
        long done = 0;
        for (int team = 1; team <= teamCount; team++) {
            String name = worldName(gameId, team);
            if (failed.contains(name) || terrainDone.contains(name)) {
                done += perWorld;
            } else {
                ChunkJob job = overworldJobs.get(name);
                if (job != null) {
                    done += Math.min(perWorld, job.done);
                }
            }
        }
        return (int) Math.min(100, done * 100 / ((long) perWorld * teamCount));
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
     * Partie reellement lancee avec usedTeams equipes (voir GameManager.prepareInstances, appele une fois
     * toutes les instances reclamees/generees) : plus besoin de suivre une eventuelle annulation. 0.6.0 :
     * les mondes des equipes restees vides (salle prevue pour plus d'equipes) ne sont plus generes, et ceux
     * deja crees sont supprimes tout de suite - moins de charge pour le serveur.
     */
    public void forget(String gameId, int usedTeams) {
        cancelledGameIds.remove(gameId);
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            queue.removeIf(task -> task.gameId().equals(gameId) && task.team() > usedTeams);
        }
        String prefix = worldNamePrefix + gameId + "_";
        Iterator<Map.Entry<String, World>> it = ready.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, World> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                worldManager.deleteInstanceWorld(entry.getKey()); // jamais reclame : equipe restee vide
                it.remove();
            }
        }
        forgetTracking(prefix);
    }

    /**
     * Partie annulee AVANT son lancement (voir PartyCanceller), ou terminee (GameManager.cleanupGame) :
     * empeche toute generation restante et supprime immediatement les mondes deja pre-generes pour ce
     * gameId (jamais reclames par une vraie instance de jeu, autrement ils auraient deja ete retires de
     * `ready` par claim()).
     */
    public void cancel(String gameId) {
        cancelledGameIds.add(gameId);
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            queue.removeIf(task -> task.gameId().equals(gameId));
        }
        String prefix = worldNamePrefix + gameId + "_";
        Iterator<Map.Entry<String, World>> it = ready.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, World> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                worldManager.deleteInstanceWorld(entry.getKey());
                it.remove();
            }
        }
        forgetTracking(prefix);
    }

    private void forgetTracking(String prefix) {
        overworldJobs.keySet().removeIf(name -> name.startsWith(prefix));
        terrainDone.removeIf(name -> name.startsWith(prefix));
        failed.removeIf(name -> name.startsWith(prefix));
    }
}
