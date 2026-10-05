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

    /*
     * 0.8.5 - RESERVE DE MONDES (demande de Maxster33, 03/10/2026) : la creation d'un monde gele le serveur, ce qui
     * genait les joueurs deja en partie. Regles demandees :
     *  - 2 mondes d'avance (overworld + Nether + End + terrain), crees UNIQUEMENT quand aucune partie n'est en cours ;
     *  - une partie creee alors qu'aucune partie n'est en cours recoit des mondes neufs (comme avant : ne gene
     *    personne) et la reserve est gardee pour les parties simultanees ;
     *  - une partie creee pendant qu'une autre est en cours prend des mondes de la reserve (memes seed pour toutes ses
     *    equipes) ; s'il en manque (3-4 equipes), les autres sont crees comme avant.
     * Noms : <prefixe>reserve-<uuid>_<n> (le monde garde ce nom pendant la partie). La reserve n'est pas gardee d'un
     * demarrage a l'autre (mondes restants supprimes au demarrage).
     *
     * 0.10.0 - LOTS DE MONDES (demande de Maxster33, 05/10/2026 : deux parties a deux jours d'ecart sur la meme seed ;
     * en 0.8.5 un monde ajoute a la reserve reprenait la seed du monde restant, qui pouvait donc revenir indefiniment) :
     *  - la reserve est faite de LOTS : un lot = une seed neuve, jamais rejouee ; instances.reserve-lots-4-teams lots
     *    de 4 mondes (parties de 2 a 4 equipes) et instances.reserve-lots-solo lots de 1 monde (parties a 1 equipe),
     *    3 et 3 par defaut, prepares des le demarrage (mondes de reserve d'avant le redemarrage supprimes d'abord) ;
     *  - toute partie prend un lot (meme si aucune autre partie n'est en cours) ; une partie a 2 ou 3 equipes prend un
     *    lot de 4 et les mondes en trop sont supprimes tout de suite (choix de Maxster33 : jamais deux parties sur la
     *    meme seed) ; equipes restees vides au lancement et partie annulee : mondes supprimes aussi ;
     *  - le lot n'est remplace qu'a la fin de la partie qui l'utilise, une fois ses mondes effaces du disque (en
     *    arriere-plan, sans lag : choix de Maxster33) ;
     *  - pendant une partie : pas de creation de monde de reserve (petit gel du serveur), seulement le terrain des
     *    mondes deja crees, au debit reduit instances.reserve-chunks-per-second-during-games (choix de Maxster33) ;
     *  - a instances.max-simultaneous-games parties en cours (4) : reserve en pause (ni remplacement, ni terrain) ;
     *  - aucun lot libre : mondes crees pour la partie, avec la seed tiree par kal-games (comme avant).
     */
    private static final int FULL_LOT_SIZE = 4;

    /** 0.10.0 : un lot de mondes de reserve partageant une seed neuve (1 monde pour le solo, 4 sinon). */
    private record Lot(long seed, List<String> worlds) {
    }

    private final int fullLots;
    private final int soloLots;
    private final double reserveChunksPerTickDuringGames;
    private double reserveChunkCredit;
    /** Lots libres (en file ou crees), dans l'ordre de creation. */
    private final List<Lot> freeLots = new ArrayList<>();
    /** gameId -> lot pris par cette partie : remplace a la fin de la partie (voir refillReserve). */
    private final Map<String, Lot> usedLots = new HashMap<>();
    /** "gameId:equipe" -> overworld de reserve attribue a cette equipe. */
    private final Map<String, String> assigned = new HashMap<>();
    private java.util.function.IntSupplier gamesInProgress = () -> 0;
    private int pauseAtGames = Integer.MAX_VALUE;
    /** 0.10.0 : la reserve ne se remplit qu'apres le nettoyage du demarrage (voir cleanLeftoverReserves). */
    private boolean reserveStarted;

    /**
     * A appeler une fois GameManager construit : nombre de parties EN COURS ; a {@code pauseAtGames} parties en cours,
     * la reserve est en pause (0.10.0).
     */
    public void setGamesInProgress(java.util.function.IntSupplier gamesInProgress, int pauseAtGames) {
        this.gamesInProgress = gamesInProgress;
        this.pauseAtGames = Math.max(1, pauseAtGames);
    }

    private boolean gameInProgress() {
        return gamesInProgress.getAsInt() > 0;
    }

    private boolean reservePaused() {
        return gamesInProgress.getAsInt() >= pauseAtGames;
    }

    /**
     * 0.8.5 : supprime les mondes de reserve d'avant le redemarrage (a appeler apres la restauration des parties).
     * 0.10.0 : la preparation des lots commence ensuite, une fois ces mondes effaces du disque.
     */
    public void cleanLeftoverReserves() {
        worldManager.deleteLeftovers(reservePrefix());
        reserveStarted = true;
    }

    private String reservePrefix() {
        return worldNamePrefix + "reserve-";
    }

    public InstanceWorldPreparer(JavaPlugin plugin, Logger logger, InstanceWorldManager worldManager,
                                  String worldNamePrefix, Duration stagger, int radiusBlocks,
                                  int chunksPerSecond, int maxChunksInFlight,
                                  int fullLots, int soloLots, int reserveChunksPerSecondDuringGames) {
        this.fullLots = Math.max(0, fullLots);
        this.soloLots = Math.max(0, soloLots);
        this.reserveChunksPerTickDuringGames = Math.max(1, reserveChunksPerSecondDuringGames) / 20.0;
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

    /**
     * A appeler UNE SEULE FOIS, des la creation d'une nouvelle BingoParty (voir PartyManager).
     *
     * @return la seed a utiliser pour cette partie : celle du lot de reserve pris (0.8.5, 0.10.0), sinon {@code seed}
     */
    public long startPreGeneration(String gameId, long seed, int teamCount) {
        // 0.10.0 : toute partie prend un lot libre (lot solo a 1 equipe, lot de 4 sinon), meme sans autre partie en cours.
        Lot lot = teamCount >= 1 && teamCount <= FULL_LOT_SIZE ? bestFreeLot(teamCount == 1 ? 1 : FULL_LOT_SIZE) : null;
        if (lot != null) {
            freeLots.remove(lot);
            usedLots.put(gameId, lot);
            seed = lot.seed();
            Map<String, Integer> teamOf = new HashMap<>();
            for (int team = 1; team <= lot.worlds().size(); team++) {
                String name = lot.worlds().get(team - 1);
                if (team <= teamCount) {
                    assigned.put(gameId + ":" + team, name);
                    teamOf.put(name, team);
                } else {
                    discardWorld(name); // monde en trop du lot : supprime, sa seed ne doit pas resservir
                }
            }
            // Mondes du lot encore en file (overworld, Nether, End) : ils deviennent des taches de la partie, servies
            // avant la reserve et creees meme pendant une partie en cours (comme les mondes crees pour une partie).
            for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
                List<WorldTask> moved = new ArrayList<>();
                queue.removeIf(task -> {
                    Integer team = teamOf.get(task.name());
                    if (task.isReserve() && team != null) {
                        moved.add(new WorldTask(gameId, task.seed(), team, task.environment(), task.name()));
                        return true;
                    }
                    return false;
                });
                queue.addAll(moved);
            }
            logger.info("[KG_BingoGame] Partie '" + gameId + "' : lot de réserve de " + lot.worlds().size()
                    + " monde(s) attribué(s) (" + teamCount + " équipe(s), seed=" + seed + ").");
        } else {
            logger.info("[KG_BingoGame] Partie '" + gameId + "' : aucun lot de réserve libre, mondes créés pour la partie.");
        }
        // Ordre (0.1.22) : overworlds de toutes les equipes, puis leurs Nether, puis leurs End.
        for (int team = 1; team <= teamCount; team++) {
            if (!assigned.containsKey(gameId + ":" + team)) {
                overworldQueue.add(new WorldTask(gameId, seed, team, World.Environment.NORMAL, worldName(gameId, team)));
            }
        }
        for (World.Environment environment : new World.Environment[] {World.Environment.NETHER, World.Environment.THE_END}) {
            for (int team = 1; team <= teamCount; team++) {
                if (!assigned.containsKey(gameId + ":" + team)) {
                    dimensionQueue.add(new WorldTask(gameId, seed, team, environment, worldName(gameId, team)));
                }
            }
        }
        return seed;
    }

    /** 0.10.0 : lot libre de cette taille le plus avance (terrains prets, puis mondes crees), le plus ancien a egalite. */
    private Lot bestFreeLot(int size) {
        Lot best = null;
        int bestScore = -1;
        for (Lot lot : freeLots) {
            if (lot.worlds().size() != size) {
                continue;
            }
            int score = 0;
            for (String name : lot.worlds()) {
                score += terrainDone.contains(name) ? 2 : ready.containsKey(name) ? 1 : 0;
            }
            if (score > bestScore) {
                best = lot;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 0.10.0 : complete la reserve, un lot a la fois (une seed neuve par lot), jusqu'a fullLots lots de 4 et soloLots
     * lots solo, en comptant les lots pris par des parties pas encore terminees. Attend que les mondes supprimes soient
     * effaces du disque, et ne fait rien a pauseAtGames parties en cours. Les mondes eux-memes ne sont crees qu'en
     * l'absence de partie en cours (voir nextWorldTask).
     */
    private void refillReserve() {
        if (!reserveStarted || reservePaused() || worldManager.pendingDeletions() > 0) {
            return;
        }
        int solo = countLots(1);
        int full = countLots(FULL_LOT_SIZE);
        int size;
        if (solo < soloLots && (solo <= full || full >= fullLots)) {
            size = 1;
        } else if (full < fullLots) {
            size = FULL_LOT_SIZE;
        } else {
            return;
        }
        long seed = new java.util.Random().nextLong();
        String base = reservePrefix() + java.util.UUID.randomUUID() + "_";
        List<String> worlds = new ArrayList<>();
        for (int n = 1; n <= size; n++) {
            String name = base + n;
            worlds.add(name);
            overworldQueue.add(new WorldTask(null, seed, 0, World.Environment.NORMAL, name));
            dimensionQueue.add(new WorldTask(null, seed, 0, World.Environment.NETHER, name));
            dimensionQueue.add(new WorldTask(null, seed, 0, World.Environment.THE_END, name));
        }
        freeLots.add(new Lot(seed, worlds));
        logger.info("[KG_BingoGame] Réserve : préparation d'un lot " + (size == 1 ? "solo" : "de " + size + " mondes")
                + " (seed=" + seed + ") - lots solo " + countLots(1) + "/" + soloLots + ", lots de " + FULL_LOT_SIZE + " "
                + countLots(FULL_LOT_SIZE) + "/" + fullLots + ".");
    }

    /** Lots de cette taille, libres ou pris par une partie pas encore terminee. */
    private int countLots(int size) {
        int count = 0;
        for (Lot lot : freeLots) {
            count += lot.worlds().size() == size ? 1 : 0;
        }
        for (Lot lot : usedLots.values()) {
            count += lot.worlds().size() == size ? 1 : 0;
        }
        return count;
    }

    /** true si ce monde (overworld de reserve) fait partie d'un lot libre. */
    private boolean isFreeReserve(String name) {
        for (Lot lot : freeLots) {
            if (lot.worlds().contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 0.10.0 : supprime un monde de reserve jamais joue (monde en trop d'un lot, equipe restee vide, partie annulee,
     * lot en echec) : retire de la file et du suivi, efface du disque s'il existe deja (avec son Nether et son End).
     */
    private void discardWorld(String name) {
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            queue.removeIf(task -> name.equals(task.name()));
        }
        ready.remove(name);
        overworldJobs.remove(name);
        terrainDone.remove(name);
        failed.remove(name);
        worldManager.deleteInstanceWorld(name);
    }

    /** Nom de l'overworld de cette equipe : monde de reserve attribue (0.8.5), sinon &lt;prefixe&gt;&lt;gameId&gt;_&lt;n&gt;. */
    public String instanceWorldName(String gameId, int team) {
        return worldName(gameId, team);
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
                    || overworldQueue.stream().anyMatch(task -> gameId.equals(task.gameId()) && task.team() == t);
            if (!known) {
                overworldQueue.add(new WorldTask(gameId, seed, team, World.Environment.NORMAL, name));
            }
        }
    }

    /** Une creation de monde en attente. 0.8.5 : {@code name} = overworld concerne ; gameId null = reserve. */
    private record WorldTask(String gameId, long seed, int team, World.Environment environment, String name) {
        boolean isReserve() {
            return gameId == null;
        }
    }

    private String worldName(String gameId, int team) {
        String reserved = assigned.get(gameId + ":" + team);
        return reserved != null ? reserved : worldNamePrefix + gameId + "_" + team;
    }

    private void tick() {
        tick++;
        if (tick % 20 == 0) {
            refillReserve(); // 0.8.5
        }
        if (tick >= nextCreationTick) {
            WorldTask task = nextWorldTask();
            if (task != null) {
                create(task);
                nextCreationTick = tick + staggerTicks;
            }
        }
        pumpChunks();
    }

    /**
     * Prochaine creation : mondes des parties d'abord (overworlds puis Nether / End), puis la reserve (0.8.5) - jamais
     * pendant qu'une partie est en cours (les taches de reserve attendent alors dans la file).
     */
    private WorldTask nextWorldTask() {
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            Iterator<WorldTask> it = queue.iterator();
            while (it.hasNext()) {
                WorldTask task = it.next();
                if (task.isReserve()) {
                    continue;
                }
                it.remove();
                if (!cancelledGameIds.contains(task.gameId())) {
                    return task;
                }
            }
        }
        if (gameInProgress()) {
            return null;
        }
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            Iterator<WorldTask> it = queue.iterator();
            while (it.hasNext()) {
                WorldTask task = it.next();
                if (!task.isReserve()) {
                    continue;
                }
                it.remove();
                // Overworld de reserve pas encore cree mais deja attribue : il est cree quand meme (la partie l'attend) ;
                // Nether / End d'un monde de reserve : toujours crees (memes noms que l'overworld).
                return task;
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
        String worldName = task.name(); // 0.8.5 : nom porte par la tache (monde de reserve possible)
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
                        worldManager.placeSpawnOnLand(world, 0); // 0.8.5 : pas de pre-generation : point de depart seul
                        terrainDone.add(worldName);
                    }
                }
            } else {
                if (Bukkit.getWorld(worldName) == null) {
                    return; // 0.8.5 : overworld deja supprime (partie terminee) ou jamais cree : pas de Nether / End orphelin
                }
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
                if (task.isReserve()) {
                    // 0.10.0 : tout le lot est abandonne (mondes supprimes) ; il sera remplace au prochain remplissage.
                    for (Lot lot : new ArrayList<>(freeLots)) {
                        if (lot.worlds().contains(worldName)) {
                            freeLots.remove(lot);
                            lot.worlds().forEach(this::discardWorld);
                        }
                    }
                }
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
    /**
     * 0.7.1 : chunks en cours = somme des demandes en attente des terrains ENCORE actifs. En 0.6.0 / 0.7.0 c'etait un
     * compteur global : les demandes d'une map supprimee en cours de route (partie annulee) ne se terminaient jamais
     * et bloquaient ce compteur au maximum : plus aucun terrain n'avancait (partie bloquee a 0 %, 25/09/2026).
     */
    private int chunksInFlight() {
        int total = 0;
        for (ChunkJob job : jobs) {
            total += job.pending();
        }
        return total;
    }

    private void pumpChunks() {
        // 0.7.2 : d'abord retirer les terrains des maps supprimees (partie annulee / terminee) et ceux qui sont finis,
        // AVANT de compter les demandes en cours. En 0.7.1 un terrain supprime n'etait marque qu'en cherchant le
        // prochain chunk, ce qui n'arrivait plus justement parce que ses demandes perdues occupaient toutes les places :
        // blocage a 0 %.
        for (ChunkJob job : jobs) {
            job.checkStopped();
        }
        jobs.removeIf(job -> job.stopped || (job.queue.isEmpty() && job.pending() == 0));
        chunkCredit = Math.min(chunkCredit + chunksPerTick, Math.max(1.0, chunksPerTick));
        // 0.10.0 : pendant une partie, le terrain de la reserve avance a un debit reduit (credit separe).
        reserveChunkCredit = Math.min(reserveChunkCredit + reserveChunksPerTickDuringGames,
                Math.max(1.0, reserveChunksPerTickDuringGames));
        boolean slowReserve = gameInProgress();
        while (chunkCredit >= 1.0 && chunksInFlight() < maxChunksInFlight) {
            ChunkJob job = nextJob();
            if (job == null) {
                return;
            }
            if (slowReserve && job.isFreeReserve()) {
                if (reserveChunkCredit < 1.0) {
                    return;
                }
                reserveChunkCredit -= 1.0;
            }
            job.requestNext();
            chunkCredit -= 1.0;
        }
    }

    /** Prochain terrain a servir : les overworlds d'abord, puis Nether / End, dans l'ordre d'arrivee. */
    private ChunkJob nextJob() {
        // 0.8.5 : terrains des parties avant ceux de la reserve. 0.10.0 : reserve en pause a pauseAtGames parties.
        for (boolean reserveJobs : reservePaused() ? new boolean[] {false} : new boolean[] {false, true}) {
            for (ChunkJob job : jobs) {
                if (job.overworld && !job.queue.isEmpty() && job.isFreeReserve() == reserveJobs) {
                    return job;
                }
            }
            for (ChunkJob job : jobs) {
                if (!job.queue.isEmpty() && job.isFreeReserve() == reserveJobs) {
                    return job;
                }
            }
        }
        return null;
    }

    /** 0.7.2 : une demande de chunk sans reponse depuis ce delai est oubliee (ne bloque plus jamais la file). */
    private static final long CHUNK_TIMEOUT_MS = 60_000L;

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
        /** Instants d'envoi des demandes de chunks pas encore terminees (voir pending()). */
        private final Deque<Long> sentAt = new ArrayDeque<>();
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

        /** 0.8.5 : terrain d'un monde de reserve encore libre (servi apres ceux des parties). */
        boolean isFreeReserve() {
            return InstanceWorldPreparer.this.isFreeReserve(InstanceWorldManager.baseNameOf(worldName));
        }

        /** Monde supprime ou partie annulee / terminee : on arrete ce terrain. */
        void checkStopped() {
            if (!stopped && (Bukkit.getWorld(worldName) == null || (gameId != null && cancelledGameIds.contains(gameId)))) {
                stopped = true;
                queue.clear();
            }
        }

        /** Demandes en cours, sans compter celles restees sans reponse depuis plus de CHUNK_TIMEOUT_MS. */
        int pending() {
            long limit = System.currentTimeMillis() - CHUNK_TIMEOUT_MS;
            while (!sentAt.isEmpty() && sentAt.peekFirst() < limit) {
                sentAt.pollFirst();
                done++; // oubliee : comptee comme faite (sinon le terrain ne serait jamais « termine »)
                checkComplete();
            }
            return sentAt.size();
        }

        void requestNext() {
            int[] chunk = queue.poll();
            sentAt.addLast(System.currentTimeMillis());
            world.getChunkAtAsync(chunk[0], chunk[1], true).whenComplete((c, error) -> {
                if (Bukkit.isPrimaryThread()) {
                    onChunkDone();
                } else {
                    Bukkit.getScheduler().runTask(plugin, this::onChunkDone);
                }
            });
        }

        private void onChunkDone() {
            if (sentAt.pollFirst() == null) {
                return; // demande deja oubliee (et comptee) : rien a faire
            }
            done++;
            checkComplete();
        }

        private boolean completed;

        private void checkComplete() {
            if (!stopped && !completed && done >= total) {
                completed = true;
                if (overworld) {
                    // 0.8.5 : point de depart sur la terre ferme (voir FixedSpawnGenerator), avant que le monde soit
                    // declare pret.
                    worldManager.placeSpawnOnLand(world, radiusChunks * 16);
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
        // 0.8.5 : mondes de reserve attribues - utilises : plus suivis ici. 0.10.0 : equipes restees vides : mondes
        // supprimes (plus remis en reserve : leur seed est celle de la partie).
        Iterator<Map.Entry<String, String>> reserved = assigned.entrySet().iterator();
        while (reserved.hasNext()) {
            Map.Entry<String, String> entry = reserved.next();
            if (!entry.getKey().startsWith(gameId + ":")) {
                continue;
            }
            int team = Integer.parseInt(entry.getKey().substring(gameId.length() + 1));
            String name = entry.getValue();
            if (team > usedTeams) {
                discardWorld(name);
            } else {
                overworldJobs.remove(name);
                terrainDone.remove(name);
                failed.remove(name);
            }
            reserved.remove();
        }
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            queue.removeIf(task -> gameId.equals(task.gameId()) && task.team() > usedTeams);
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
        // 0.10.0 : mondes de reserve attribues a une partie jamais lancee : supprimes (avant : remis en reserve). Ceux
        // d'une partie lancee ne sont plus ici (voir forget) : GameManager.cleanupGame les supprime.
        Iterator<Map.Entry<String, String>> reserved = assigned.entrySet().iterator();
        while (reserved.hasNext()) {
            Map.Entry<String, String> entry = reserved.next();
            if (entry.getKey().startsWith(gameId + ":")) {
                discardWorld(entry.getValue());
                reserved.remove();
            }
        }
        // 0.10.0 : le lot de la partie est libere : un lot neuf le remplace une fois ses mondes effaces du disque.
        if (usedLots.remove(gameId) != null) {
            logger.info("[KG_BingoGame] Partie '" + gameId + "' terminée ou annulée : son lot de réserve sera remplacé.");
        }
        for (Deque<WorldTask> queue : List.of(overworldQueue, dimensionQueue)) {
            queue.removeIf(task -> gameId.equals(task.gameId()));
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
