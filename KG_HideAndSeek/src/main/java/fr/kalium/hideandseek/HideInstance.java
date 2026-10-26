package fr.kalium.hideandseek;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.world.Template;
import fr.kalium.scoreboards.data.StatsService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Axis;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.CopperGolemStatue;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hide and Seek (demande de LeKiwi06, 06/10/2026). Les hiders imitent un bloc du decor de la map, les seekers doivent
 * les trouver avant la fin du chrono.
 * <ul>
 *   <li>Hider en mouvement : joueur normal, visible, qui porte son bloc sur la tete (visible aussi des joueurs
 *       Bedrock). Immobile quelques secondes : il devient un vrai bloc pose dans la copie de l'arene, et il est cache a
 *       tous les autres joueurs ; il redevient un joueur des qu'il bouge. 0.2.0 : le hider solide se tient DEBOUT SUR
 *       son bloc (il le voit sous ses pieds et n'est plus repousse par lui), au lieu d'etre dedans.</li>
 *   <li>Seeker : 5 coeurs, main nue. Un coup sur un hider (joueur ou bloc) l'elimine ; un coup sur un bloc du decor
 *       d'un type de la liste de la map coute un demi-coeur. A 0 coeur : retour dans la salle des seekers.</li>
 *   <li>Deroulement : cachette (phase COUNTDOWN, seekers enfermes dans leur salle), recherche (RUNNING), fin (ENDING).</li>
 *   <li>Deconnexion : le joueur garde son role et peut revenir jusqu'a la fin ; aucun point pendant l'absence.</li>
 * </ul>
 * Les points sont credites a la fin de la partie (regle commune : ses points + la moyenne de ceux des joueurs classes en
 * dessous).
 */
public final class HideInstance extends GameInstance {

    public enum Winner { NONE, HIDERS, SEEKERS }

    /** Vie d'un seeker : 5 coeurs. */
    static final double SEEKER_HEALTH = 10.0;
    /** Absorption en reserve : 3 coeurs au plus. */
    static final double SEEKER_ABSORPTION = 6.0;
    /** Evasion : Vitesse V pendant 2 secondes. */
    private static final int ESCAPE_TICKS = 40;
    private static final int ESCAPE_AMPLIFIER = 4;
    /** Un hider solide redevient mobile au-dela de cet ecart avec le centre de sa case. */
    private static final double SOLID_SLACK = 0.4;
    /** Demi-largeur d'un joueur : son emprise au sol, centree sur la case, va de 0,2 a 0,8. */
    private static final double HALF_WIDTH = 0.3;
    /** Taille d'un joueur, et place minimale au-dessus du bloc pour y tenir rapetisse (taille 0,0625 au plus petit). */
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double MIN_ROOM = 0.2;
    /** Objet « Orienter le bloc » : ordre des orientations (clic droit). */
    private static final List<BlockFace> TURN = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN);
    private static final List<BlockFace> TURN_FINE = List.of(BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST,
            BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST);

    /** Un hider en vie. */
    public static final class Hider {
        final UUID uuid;
        Material block;
        /** Etat du bloc imite (orientation, posture), choisi avec l'objet « Orienter le bloc » ; remis a zero a chaque changement de bloc. */
        BlockData state;
        /** Le hider a deja choisi son bloc lui-meme (le delai entre deux changements ne compte qu'ensuite). */
        boolean chosen;
        long lastChange;
        Location lastPos;
        int stillTicks;
        /** Case occupee par le hider solide (null : en mouvement), son contenu d'origine et la position du hider. */
        Block cell;
        BlockData cellOriginal;
        Location snap;
        /** Position du hider juste avant de devenir solide, et s'il a ete rapetisse pour tenir sous un plafond bas. */
        Location ground;
        boolean shrunk;
        long nextSound;
        /** Evasions gagnees (une tous les N points) et utilisees ; fin de l'evasion en cours (ticks). */
        int escapesEarned;
        int escapesUsed;
        long escapeUntil;
        int survived;
        /** Derniere seconde de recherche ou un seeker libre etait a portee d'ecoute ; points de survie pas encore credites. */
        int lastNear;
        double surviveAcc;

        Hider(UUID uuid) {
            this.uuid = uuid;
        }

        public Material block() {
            return block;
        }

        boolean solid() {
            return cell != null;
        }
    }

    /** Un seeker (de depart, ou hider elimine devenu seeker). */
    static final class Seeker {
        final UUID uuid;
        /** Secondes restantes dans la salle des seekers (0 : libre). */
        int jail;
        long lastClick;
        long lastRightClick;
        long lastDirectHit;

        Seeker(UUID uuid) {
            this.uuid = uuid;
        }
    }

    /** Etat garde d'un joueur deconnecte en cours de partie. */
    private record Absent(Location location, double health, double absorption, boolean inRoom) {
    }

    private record PendingSound(long due, UUID hider) {
    }

    private final KGHideAndSeek hns;
    private final Template template;
    private final int privateCap;
    private final boolean becomeSeeker;

    private final Set<UUID> volunteers = new HashSet<>();
    private final Set<UUID> greeted = new HashSet<>();
    private final Map<UUID, Hider> hiders = new LinkedHashMap<>();
    private final Map<UUID, Seeker> seekers = new LinkedHashMap<>();
    private final Map<UUID, Absent> absent = new HashMap<>();
    /** Tous les joueurs qui ont commence la partie : nom, points, role de depart, hiders trouves. */
    private final Map<UUID, String> names = new LinkedHashMap<>();
    private final Map<UUID, Double> points = new HashMap<>();
    private final Map<UUID, String> startRoles = new HashMap<>();
    private final Map<UUID, Integer> finds = new HashMap<>();
    private final List<PendingSound> pendingSounds = new ArrayList<>();
    /** Tableau de droite (blocs encore en vie) de chaque joueur, et celui qu'il avait avant la partie. */
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Scoreboard> previousBoards = new HashMap<>();
    private List<Material> pool = List.of();
    /** Nombre de chaque bloc de la liste dans la map, et multiplicateur de points de survie qui en decoule (rarete). */
    private final Map<Material, Integer> mapCounts = new HashMap<>();
    private final Map<Material, Double> rarity = new HashMap<>();
    /** Petite partie : personne ne sort du batiment central (zone entre ses deux coins), derniere position dedans. */
    private BoundingBox building;
    private final Map<UUID, Location> lastInside = new HashMap<>();
    private long ticks;
    private int elapsed;
    private int found;
    private Winner winner = Winner.NONE;

    public HideInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                        boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        this.hns = KGHideAndSeek.get();
        this.template = template;
        this.privateCap = Math.max(minPlayers(), Math.min(maxPlayers(), optionInt("maxPlayers", maxPlayers())));
        boolean byDefault = minigame.getBool("eliminated-becomes-seeker", false);
        this.becomeSeeker = publicGame ? byDefault : optionBool("becomeSeeker", byDefault);
    }

    private int setting(String key, int fallback) {
        return minigame().getInt(key, fallback);
    }

    private int minPlayers() {
        return Math.max(2, Math.min(16, setting("min-players", 3)));
    }

    private int maxPlayers() {
        return Math.max(minPlayers(), Math.min(16, setting("max-players", 16)));
    }

    private boolean playing() {
        return phase == Phase.COUNTDOWN || phase == Phase.RUNNING;
    }

    // ------------------------------------------------------------------ acces (menus, ecouteurs)

    public Hider hider(UUID uuid) {
        return hiders.get(uuid);
    }

    public List<Material> pool() {
        return pool;
    }

    public boolean becomeSeeker() {
        return becomeSeeker;
    }

    /** Le joueur a-t-il un role dans la partie en cours (fin de partie comprise) ? */
    public boolean inMatch(UUID uuid) {
        return matchInProgress() && (hiders.containsKey(uuid) || seekers.containsKey(uuid));
    }

    /** Un joueur deconnecte en cours de partie, qui peut encore revenir. */
    public boolean awaits(UUID uuid) {
        return playing() && !closing() && absent.containsKey(uuid);
    }

    /** Joueur connecte, dans la partie, et pas en train de revenir. */
    private boolean present(UUID uuid) {
        return members.contains(uuid) && !absent.containsKey(uuid) && Bukkit.getPlayer(uuid) != null;
    }

    private Player presentPlayer(UUID uuid) {
        return present(uuid) ? Bukkit.getPlayer(uuid) : null;
    }

    // ------------------------------------------------------------------ points d'arene

    private Location point(String key) {
        Pos pos = arena().point(key);
        return pos == null ? stands() : loc(pos);
    }

    private Location hiderSpawn() {
        return point("hider-spawn");
    }

    private Location seekerRoom() {
        return point("seeker-room");
    }

    /** En petite partie (enfermee dans le batiment), les seekers partent du depart des hiders. */
    private Location seekerSpawn() {
        return building != null ? hiderSpawn() : point("seeker-spawn");
    }

    /**
     * Petite partie (0.3.0, demande de LeKiwi06) : jusqu'a « building-max-players » joueurs au lancement, tout le monde
     * reste dans le batiment central, entre les points « Batiment : coin 1 / 2 ». null : partie normale, ou coins non
     * poses.
     */
    private BoundingBox buildingZone(int players) {
        Pos one = arena().point("building-1");
        Pos two = arena().point("building-2");
        if (one == null || two == null || players > setting("building-max-players", 4)) {
            return null;
        }
        Location a = loc(one);
        Location b = loc(two);
        BoundingBox zone = new BoundingBox(Math.min(a.getBlockX(), b.getBlockX()), Math.min(a.getBlockY(), b.getBlockY()),
                Math.min(a.getBlockZ(), b.getBlockZ()), Math.max(a.getBlockX(), b.getBlockX()) + 1,
                Math.max(a.getBlockY(), b.getBlockY()) + 1, Math.max(a.getBlockZ(), b.getBlockZ()) + 1);
        // Les joueurs sortis sont ramenes au besoin au depart des hiders : il doit etre dans la zone, sinon ils y seraient
        // renvoyes sans fin.
        if (!zone.contains(hiderSpawn().toVector())) {
            hns.getLogger().warning("Arène " + arena().id() + " : le départ des hiders n'est pas entre les coins du bâtiment, petite partie sans enfermement.");
            return null;
        }
        return zone;
    }

    // ------------------------------------------------------------------ admission

    @Override
    protected Component canAdmit(Player player) {
        if (awaits(player.getUniqueId())) {
            return null;
        }
        if (hns.blocks().of(arena().id()).isEmpty()) {
            return t("hns.no-blocks", "<red>Cette map n'a aucun bloc réglé pour les hiders : prévenez un modérateur.");
        }
        if (!isPublic() && members.size() >= privateCap) {
            return t("join.full", "<red>La partie est complète.");
        }
        return null;
    }

    @Override
    protected void onMemberJoined(Player player) {
        if (awaits(player.getUniqueId())) {
            // Retour d'un joueur deconnecte : il reprend son role, pas une place dans la file.
            queue.remove(player.getUniqueId());
        }
    }

    @Override
    protected void onArrived(Player player) {
        UUID uuid = player.getUniqueId();
        hns.clearState(player);
        if (awaits(uuid)) {
            Absent back = absent.remove(uuid);
            participants.add(uuid);
            plugin.later(1L, () -> restore(player, back));
            return;
        }
        if ((phase == Phase.WAITING || phase == Phase.STARTING) && greeted.add(uuid)) {
            player.sendMessage(plugin.prefix().append(t("hns.welcome",
                    "<gray>Hide and Seek : les hiders imitent un bloc du décor, les seekers doivent les trouver. Pour être seeker, ouvrez le menu de la partie.")));
        }
    }

    // ------------------------------------------------------------------ demarrage

    @Override
    protected int minParticipants() {
        return minPlayers();
    }

    @Override
    protected int maxParticipants() {
        return maxPlayers();
    }

    @Override
    protected int gatherSeconds() {
        return setting("public-gather-seconds", 30);
    }

    @Override
    protected List<UUID> pickParticipants(List<UUID> queue) {
        return new ArrayList<>(queue.subList(0, Math.min(queue.size(), maxPlayers())));
    }

    @Override
    protected Component validatePrivateStart() {
        if (hns.blocks().of(arena().id()).isEmpty()) {
            return t("hns.no-blocks", "<red>Cette map n'a aucun bloc réglé pour les hiders : prévenez un modérateur.");
        }
        if (members.size() < minPlayers()) {
            return t("hns.need-players", "<red>Il faut au moins <n> joueurs.", "n", minPlayers());
        }
        return null;
    }

    public boolean volunteer(UUID uuid) {
        return volunteers.contains(uuid);
    }

    /** Bouton « Je veux être seeker » (pendant l'attente) : inverse le choix du joueur. */
    public void toggleVolunteer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!volunteers.remove(uuid)) {
            volunteers.add(uuid);
            player.sendMessage(plugin.prefix().append(t("hns.volunteer-on",
                    "<green>Vous êtes volontaire pour être seeker. <gray>S'il y a trop de volontaires, un tirage décide.")));
        } else {
            player.sendMessage(plugin.prefix().append(t("hns.volunteer-off", "<gray>Vous n'êtes plus volontaire pour être seeker.")));
        }
    }

    private void resetState() {
        hiders.clear();
        seekers.clear();
        absent.clear();
        names.clear();
        points.clear();
        startRoles.clear();
        finds.clear();
        pendingSounds.clear();
        mapCounts.clear();
        rarity.clear();
        building = null;
        lastInside.clear();
        ticks = 0;
        elapsed = 0;
        found = 0;
        winner = Winner.NONE;
    }

    @Override
    protected void beginMatch() {
        resetState();
        pool = new ArrayList<>(hns.blocks().of(arena().id()));
        List<UUID> order = new ArrayList<>(participants);
        if (pool.isEmpty() || order.size() < 2) {
            broadcast(pool.isEmpty()
                    ? t("hns.no-blocks", "<red>Cette map n'a aucun bloc réglé pour les hiders : prévenez un modérateur.")
                    : t("hns.aborted", "<yellow>Partie annulée : il ne reste pas assez de joueurs."));
            endMatch();
            return;
        }
        // Seekers : 1 pour N joueurs (arrondi au-dessus), les volontaires d'abord, completes au hasard.
        Collections.shuffle(order);
        List<UUID> candidates = new ArrayList<>();
        for (UUID uuid : order) {
            if (volunteers.contains(uuid)) {
                candidates.add(uuid);
            }
        }
        for (UUID uuid : order) {
            if (!volunteers.contains(uuid)) {
                candidates.add(uuid);
            }
        }
        building = buildingZone(order.size());
        computeRarity();
        int per = Math.max(2, setting("players-per-seeker", 5));
        int count = Math.max(1, Math.min(order.size() - 1, (order.size() + per - 1) / per));
        Set<UUID> picked = new HashSet<>(candidates.subList(0, count));

        phase = Phase.COUNTDOWN;
        secondsLeft = Math.max(1, setting("hide-seconds", 30));
        List<String> seekerNames = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            names.put(uuid, player.getName());
            points.put(uuid, 0.0);
            if (picked.contains(uuid)) {
                Seeker seeker = new Seeker(uuid);
                seekers.put(uuid, seeker);
                startRoles.put(uuid, "seeker");
                seekerNames.add(player.getName());
                setupSeeker(player, true);
                title(Set.of(uuid), t("hns.role-seeker", "<red><bold>Seeker"),
                        t("hns.role-seeker-sub", "<gray>Les hiders se cachent : <white><s></white> s", "s", secondsLeft), 5, 60, 10);
            } else {
                Hider hider = new Hider(uuid);
                hider.block = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
                hider.state = hider.block.createBlockData();
                hiders.put(uuid, hider);
                startRoles.put(uuid, "hider");
                setupHider(player, hider, hiderSpawn());
                title(Set.of(uuid), t("hns.role-hider", "<green><bold>Hider"),
                        t("hns.role-hider-sub", "<gray>Cachez-vous : <white><s></white> s", "s", secondsLeft), 5, 60, 10);
                plugin.later(10L, () -> {
                    if (player.isOnline() && phase == Phase.COUNTDOWN && hiders.containsKey(uuid)) {
                        hns.menus().openBlocks(player, this);
                    }
                });
            }
        }
        Collections.sort(seekerNames);
        broadcast(t("hns.start", "<gold>Hide and Seek : <white><hiders></white> hider(s), seeker(s) : <red><names></red>.",
                "hiders", hiders.size(), "names", String.join(", ", seekerNames)));
        broadcastTo(hiders.keySet(), t("hns.hider-help",
                "<gray>Vous portez votre bloc sur la tête. Restez immobile <white><s></white> s pour devenir ce bloc ; bougez pour redevenir un joueur.",
                "s", Math.max(1, setting("solid-seconds", 3))));
        if (building != null) {
            broadcast(t("hns.building-start", "<yellow>Petite partie : tout le monde reste dans le bâtiment."));
        }
    }

    /**
     * Rarete des blocs (0.3.0, demande de LeKiwi06 : valoriser les blocs rares) : chaque bloc de la liste est compte dans
     * la map ; multiplicateur des points de survie de x1 pour le plus courant jusqu'a x(1 + bonus) pour le plus rare,
     * sur une echelle logarithmique (un bloc 10 fois plus rare qu'un autre gagne autant d'ecart qu'un bloc 10 fois plus
     * rare que lui). Un bloc absent de la map compte comme present une fois.
     */
    private void computeRarity() {
        int[] counts = hns.blockCounts(template, this::countBlocks);
        double low = Double.MAX_VALUE;
        double high = 0;
        for (Material material : pool) {
            int count = counts[material.ordinal()];
            mapCounts.put(material, count);
            low = Math.min(low, Math.log(Math.max(1, count)));
            high = Math.max(high, Math.log(Math.max(1, count)));
        }
        double bonus = Math.max(0, setting("rarity-bonus-percent", 100)) / 100.0;
        for (Material material : pool) {
            double log = Math.log(Math.max(1, mapCounts.get(material)));
            rarity.put(material, high > low ? 1 + bonus * (high - log) / (high - low) : 1);
        }
    }

    /** Compte les blocs de la copie de l'arene (avant que les hiders n'y posent les leurs), par photo de chaque chunk. */
    private int[] countBlocks() {
        int[] counts = new int[Material.values().length];
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                ChunkSnapshot snapshot = world.getChunkAt(cx, cz).getChunkSnapshot(false, false, false);
                for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++) {
                    for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++) {
                        for (int y = minY; y <= maxY; y++) {
                            counts[snapshot.getBlockType(x & 15, y, z & 15).ordinal()]++;
                        }
                    }
                }
            }
        }
        return counts;
    }

    /** Multiplicateur de points de survie du bloc (rarete dans la map). */
    public double rarity(Material material) {
        return rarity.getOrDefault(material, 1.0);
    }

    /** Nombre de blocs de ce type dans la map. */
    public int mapCount(Material material) {
        return mapCounts.getOrDefault(material, 0);
    }

    /** Etat d'un hider : mode aventure, bloc sur la tete, barre d'objets, pseudo masque et pas de bousculade. */
    private void setupHider(Player player, Hider hider, Location to) {
        plugin.hub().resetPlayer(player, GameMode.ADVENTURE);
        hns.clearState(player);
        hns.joinTeam(player);
        hns.hideFromLocatorBar(player);
        giveHotbar(player, hider);
        if (to != null) {
            player.teleport(to);
        }
        hider.lastPos = player.getLocation();
        hider.stillTicks = 0;
    }

    /**
     * Barre d'objets du hider (0.2.0 : un seul objet pour le soundboard) : sons, changement de bloc, evasions en reserve,
     * et (0.3.0) « Orienter le bloc » si le bloc imite peut se tourner ou changer de posture ; bloc sur la tete.
     */
    public void giveHotbar(Player player, Hider hider) {
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(0, hns.items().soundsMenu());
        inventory.setItem(1, hns.items().blockChanger(hider.block));
        inventory.setItem(2, hns.items().escape(escapeEvery(), escapesLeft(hider)));
        BlockData probe = hider.block.createBlockData();
        boolean pose = probe instanceof CopperGolemStatue;
        inventory.setItem(3, pose || turn(probe) != null ? hns.items().stateChanger(pose) : null);
        inventory.setHelmet(new ItemStack(hider.block));
    }

    /**
     * Etat d'un seeker : mode aventure, main nue, 5 coeurs, reserve d'absorption vide. waiting : il attend dans la salle
     * des seekers (cachette, ou retour a 0 coeur), dans l'obscurite pour ne pas observer la map.
     */
    private void setupSeeker(Player player, boolean waiting) {
        plugin.hub().resetPlayer(player, GameMode.ADVENTURE);
        hns.clearState(player);
        hns.applySeekerHearts(player);
        hns.hideFromLocatorBar(player);
        player.setHealth(SEEKER_HEALTH);
        darken(player, waiting);
        player.teleport(waiting ? seekerRoom() : seekerSpawn());
    }

    /** Obscurite d'un seeker qui attend dans sa salle (0.2.0 : sinon il voit la map et les hiders qui se cachent). */
    private static void darken(Player player, boolean on) {
        if (on) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    private void startSeek() {
        phase = Phase.RUNNING;
        secondsLeft = Math.max(10, setting("seek-seconds", 300));
        elapsed = 0;
        for (Seeker seeker : seekers.values()) {
            Player player = presentPlayer(seeker.uuid);
            if (player != null) {
                darken(player, false);
                player.teleport(seekerSpawn());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
            }
        }
        title(participants, t("hns.seek-title", "<red><bold>Les seekers arrivent !"),
                t("hns.seek-sub", "<gray>Recherche : <white><time></white>", "time", clock(secondsLeft)), 5, 50, 10);
        checkEnd();
    }

    // ------------------------------------------------------------------ boucle

    @Override
    protected void matchTick() {
        ticks += TICK_INTERVAL;
        confineAll();
        int solidTicks = Math.max(1, setting("solid-seconds", 3)) * 20;
        for (Hider hider : new ArrayList<>(hiders.values())) {
            Player player = presentPlayer(hider.uuid);
            if (player == null) {
                continue;
            }
            Location now = player.getLocation();
            if (hider.solid()) {
                if (movedFrom(hider.snap, now)) {
                    unsolidify(hider, player);
                }
                continue;
            }
            if (hider.lastPos == null || hider.lastPos.getWorld() != now.getWorld() || hider.lastPos.distanceSquared(now) > 0.0004) {
                hider.lastPos = now;
                hider.stillTicks = 0;
                continue;
            }
            hider.stillTicks += TICK_INTERVAL;
            if (hider.stillTicks >= solidTicks && !solidify(hider, player)) {
                hider.stillTicks = 0;
            }
        }
        // Sons automatiques : les hiders sonnent l'un apres l'autre.
        Iterator<PendingSound> iterator = pendingSounds.iterator();
        while (iterator.hasNext()) {
            PendingSound pending = iterator.next();
            if (pending.due() > ticks) {
                continue;
            }
            iterator.remove();
            Hider hider = hiders.get(pending.hider());
            Player player = hider == null ? null : presentPlayer(hider.uuid);
            if (player != null) {
                playAt(hider, player, minigame().getText("auto-sound", "entity.villager.ambient"));
            }
        }
    }

    @Override
    protected void matchSecond() {
        switch (phase) {
            case COUNTDOWN -> {
                secondsLeft--;
                refreshSolid();
                refreshBoards();
                if (secondsLeft <= 0) {
                    startSeek();
                } else {
                    checkEnd();
                }
            }
            case RUNNING -> {
                secondsLeft--;
                elapsed++;
                survivalPoints();
                jailSecond();
                autoSound();
                refreshSolid();
                refreshBoards();
                if (secondsLeft <= 0) {
                    finish(Winner.HIDERS);
                    return;
                }
                if (secondsLeft == 60 || secondsLeft == 30 || secondsLeft == 10) {
                    broadcast(t("hns.time-left", "<gray>Fin de la recherche dans <white><s></white> s.", "s", secondsLeft));
                }
                checkEnd();
            }
            case ENDING -> {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    endMatch();
                }
            }
            default -> {
            }
        }
    }

    /** Petite partie : les hiders et les seekers libres ne sortent pas du batiment (ramenes a leur derniere position dedans). */
    private void confineAll() {
        if (building == null || !playing()) {
            return;
        }
        for (Hider hider : hiders.values()) {
            Player player = hider.solid() ? null : presentPlayer(hider.uuid);
            if (player != null) {
                confine(player);
            }
        }
        for (Seeker seeker : seekers.values()) {
            Player player = phase == Phase.RUNNING && seeker.jail <= 0 ? presentPlayer(seeker.uuid) : null;
            if (player != null) {
                confine(player);
            }
        }
    }

    private void confine(Player player) {
        Location at = player.getLocation();
        if (at.getWorld() == world && building.contains(at.toVector())) {
            lastInside.put(player.getUniqueId(), at);
            return;
        }
        Location back = lastInside.get(player.getUniqueId());
        Location to = (back == null ? hiderSpawn() : back).clone();
        to.setYaw(at.getYaw());
        to.setPitch(at.getPitch());
        player.teleport(to);
        player.sendActionBar(t("hns.building-out", "<red>Petite partie : restez dans le bâtiment."));
    }

    /**
     * Points de survie : comptes seulement pendant la recherche, pour les hiders en vie et connectes. 0.3.0 (demande de
     * LeKiwi06, 26/10/2026) : ils dependent des seekers. Seeker libre a portee d'ecoute des sons (« Portee des sons ») :
     * de x1 a la limite de la portee jusqu'a x(1 + bonus) au contact. Plus aucun seeker a portee depuis
     * « near-window-seconds » (la premiere minute de recherche comptant comme un passage) : aucun point ; entre les deux,
     * x1. Les points s'accumulent chaque seconde et sont credites a chaque intervalle de survie. Multiplies aussi par la
     * rarete du bloc imite (voir computeRarity).
     */
    private void survivalPoints() {
        int interval = Math.max(1, setting("survive-interval-seconds", 20));
        double perSecond = setting("points-survive", 1) / (double) interval;
        double range = hearingRange();
        int window = Math.max(1, setting("near-window-seconds", 60));
        double bonus = Math.max(0, setting("near-bonus-percent", 200)) / 100.0;
        for (Hider hider : hiders.values()) {
            Player player = presentPlayer(hider.uuid);
            if (player == null) {
                continue;
            }
            hider.survived++;
            double distance = nearestSeeker(player.getLocation());
            if (distance <= range) {
                hider.lastNear = elapsed;
            }
            if (elapsed - hider.lastNear <= window) {
                double factor = distance <= range ? 1 + bonus * (1 - distance / range) : 1;
                hider.surviveAcc += perSecond * factor * rarity(hider.block);
            }
            if (hider.survived % interval == 0) {
                addPoints(hider.uuid, hider.surviveAcc);
                hider.surviveAcc = 0;
            }
        }
    }

    private void jailSecond() {
        for (Seeker seeker : seekers.values()) {
            if (seeker.jail <= 0) {
                continue;
            }
            seeker.jail--;
            Player player = presentPlayer(seeker.uuid);
            if (seeker.jail == 0 && player != null) {
                darken(player, false);
                player.teleport(seekerSpawn());
                player.sendMessage(plugin.prefix().append(t("hns.seeker-back", "<green>Vous repartez avec 5 cœurs.")));
            }
        }
    }

    /** Son automatique : toutes les N secondes de recherche, chaque hider sonne, dans un ordre tire au hasard. */
    private void autoSound() {
        int interval = setting("auto-sound-seconds", 30);
        if (interval <= 0 || elapsed % interval != 0) {
            return;
        }
        List<UUID> order = new ArrayList<>();
        for (Hider hider : hiders.values()) {
            if (present(hider.uuid)) {
                order.add(hider.uuid);
            }
        }
        Collections.shuffle(order);
        int gap = Math.max(0, setting("auto-sound-gap-ticks", 15));
        long due = ticks;
        for (UUID uuid : order) {
            pendingSounds.add(new PendingSound(due, uuid));
            due += gap;
        }
    }

    /** Chaque seconde : les hiders solides restent caches (joueurs arrives entre-temps sur le serveur). */
    private void refreshSolid() {
        for (Hider hider : hiders.values()) {
            Player player = hider.solid() ? presentPlayer(hider.uuid) : null;
            if (player != null) {
                hns.conceal(player);
            }
        }
    }

    /**
     * Tableau de droite (0.3.0, demande de LeKiwi06 : aider les seekers) : blocs imites par les hiders encore en vie et
     * connectes, avec leur nombre, montre a tous les joueurs de la partie pendant la cachette et la recherche. Tableau
     * propre a chaque joueur (comme la course de bateau) : l'equipe des hiders y est recopiee, sinon leur pseudo se
     * verrait a travers les murs.
     */
    private void refreshBoards() {
        Map<Material, Integer> counts = new LinkedHashMap<>();
        int total = 0;
        for (Hider hider : hiders.values()) {
            if (present(hider.uuid)) {
                counts.merge(hider.block, 1, Integer::sum);
                total++;
            }
        }
        List<Map.Entry<Material, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : sorted) {
            lines.add(t("hns.board-line", "<white><n>× <gray><block>", "n", entry.getValue(),
                    "block", Component.translatable(entry.getKey().translationKey())));
        }
        Component title = t("hns.board-title", "<gold><bold>Blocs en vie <gray>(<n>)", "n", total);
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || player.getWorld() != world) {
                continue;
            }
            Scoreboard board = boards.computeIfAbsent(uuid, u -> Bukkit.getScoreboardManager().getNewScoreboard());
            hns.copyTeam(board);
            Objective old = board.getObjective("kghns");
            if (old != null) {
                old.unregister();
            }
            Objective objective = board.registerNewObjective("kghns", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
            int score = 15;
            int index = 0;
            for (Component line : lines) {
                if (score <= 0) {
                    break;
                }
                org.bukkit.scoreboard.Score entry = objective.getScore("l" + index++);
                entry.customName(line);
                entry.setScore(score--);
            }
            if (player.getScoreboard() != board) {
                previousBoards.putIfAbsent(uuid, player.getScoreboard());
                player.setScoreboard(board);
            }
        }
    }

    /** Rend a un joueur le tableau qu'il avait avant la partie. */
    private void restoreBoard(UUID uuid) {
        Scoreboard previous = previousBoards.remove(uuid);
        Scoreboard board = boards.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && board != null && player.getScoreboard() == board) {
            player.setScoreboard(previous != null ? previous : Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void restoreAllBoards() {
        for (UUID uuid : new ArrayList<>(boards.keySet())) {
            restoreBoard(uuid);
        }
        previousBoards.clear();
        boards.clear();
    }

    // ------------------------------------------------------------------ solidite

    private static boolean movedFrom(Location snap, Location now) {
        if (snap == null || snap.getWorld() != now.getWorld()) {
            return true;
        }
        double dx = now.getX() - snap.getX();
        double dz = now.getZ() - snap.getZ();
        return dx * dx + dz * dz > SOLID_SLACK * SOLID_SLACK || Math.abs(now.getY() - snap.getY()) > 0.3;
    }

    /** Formes de collision du bloc, ramenees a sa case (de 0 a 1 ; donnees en coordonnees du monde selon les versions). */
    private static List<BoundingBox> shape(Block block) {
        return relative(block.getCollisionShape().getBoundingBoxes(), block);
    }

    /**
     * Case ou poser le bloc du hider immobile : celle de ses pieds s'il se tient au bas d'une case (sol plein, sable des
     * ames...). 0.3.0 : ou la case juste au-dessus d'une barriere, d'un muret ou d'un portillon sur lequel il se tient
     * (on s'y tient une demi-case plus haut que leur dessin : le bloc est pose sur le haut visible). null : pieds a
     * mi-hauteur d'une case (dalle, escalier...), le bloc flotterait.
     */
    private Block footCell(Location at) {
        Block cell = world.getBlockAt(at.getBlockX(), (int) Math.floor(at.getY() + 0.5), at.getBlockZ());
        if (Math.abs(at.getY() - cell.getY()) <= 0.2) {
            return cell;
        }
        Block low = world.getBlockAt(at.getBlockX(), (int) Math.floor(at.getY()), at.getBlockZ());
        double below = 0;
        for (BoundingBox box : shape(low.getRelative(BlockFace.DOWN))) {
            below = Math.max(below, box.getMaxY());
        }
        return low.isEmpty() && below > 1.2 && Math.abs(at.getY() - (low.getY() - 1 + below)) <= 0.2 ? low : null;
    }

    /**
     * Le hider immobile devient son bloc : un vrai bloc est pose dans sa case (contenu d'origine garde, remis des qu'il
     * bouge) et il est cache a tous les autres joueurs. 0.2.0 : le hider est place DEBOUT SUR son bloc, au centre : il
     * le voit sous ses pieds, et son jeu ne le repousse plus hors du bloc (en 0.1.0 il restait dedans, avec de l'air
     * envoye a son seul jeu : il etait parfois repousse et redevenait mobile). Sous un plafond bas il s'y tient couche
     * (comportement du jeu). Renvoie false si la case ne convient pas.
     */
    private boolean solidify(Hider hider, Player player) {
        Block cell = footCell(player.getLocation());
        boolean taken = false;
        boolean onHider = false;
        for (Hider other : hiders.values()) {
            taken |= other != hider && cell != null && cell.equals(other.cell);
            onHider |= other != hider && cell != null && cell.getRelative(BlockFace.DOWN).equals(other.cell);
        }
        if (cell == null || taken || !inBounds(cell.getX(), cell.getY(), cell.getZ())) {
            player.sendActionBar(t("hns.solid-refused", "<red>Impossible de devenir solide ici."));
            return false;
        }
        // 0.3.0 : jamais sur le bloc d'un autre hider ni dans la case d'un autre joueur (cache ou non). Sinon deux hiders
        // montaient aussi haut qu'ils voulaient en devenant solides l'un sur l'autre a tour de role.
        BoundingBox space = BoundingBox.of(cell);
        for (Player other : world.getPlayers()) {
            onHider |= !other.equals(player) && other.getGameMode() != GameMode.SPECTATOR && other.getBoundingBox().overlaps(space);
        }
        if (onHider) {
            player.sendActionBar(t("hns.solid-on-player", "<red>Impossible de devenir solide sur un autre joueur."));
            return false;
        }
        // 0.3.0 : la case doit etre vide. Avant, une fleur, une herbe, une trappe ou un tapis y etait remplace par le bloc
        // du hider (et disparaissait du decor).
        if (!cell.isEmpty()) {
            player.sendActionBar(t("hns.solid-occupied", "<red>Impossible de devenir solide ici : la case doit être vide (pas de fleur, d'herbe, de trappe, de tapis...)."));
            return false;
        }
        // 0.3.0 : sous un plafond bas (dalle ou trappe du haut...), le hider est rapetisse pour tenir sur son bloc ; refus
        // seulement s'il ne reste presque aucune place.
        double top = standHeight(relative(hider.state.getCollisionShape(cell.getLocation()).getBoundingBoxes(), cell));
        if (roomAbove(cell, top) < MIN_ROOM) {
            player.sendActionBar(t("hns.solid-no-room", "<red>Pas assez de place au-dessus de vous pour devenir solide."));
            return false;
        }
        hider.ground = player.getLocation();
        BlockData original = cell.getBlockData();
        trackOriginal(cell.getX(), cell.getY(), cell.getZ(), original);
        cell.setBlockData(hider.state.clone(), false);
        hider.cell = cell;
        hider.cellOriginal = original;
        standOn(hider, player);
        hns.conceal(player);
        player.sendActionBar(t("hns.solid-on-2", "<green>Vous êtes solide : votre bloc est sous vos pieds. <gray>Bougez pour redevenir un joueur."));
        return true;
    }

    /**
     * Place le hider solide debout sur son bloc, au centre de la case, a la hauteur ou il s'y tient vraiment. 0.3.0 : on
     * ne compte que les parties du bloc sous l'emprise du joueur : dans un composteur (troue sur le dessus), il se tient
     * sur le fond, pas sur les bords (avant, il y tombait et redevenait mobile aussitot : jamais solide).
     */
    private void standOn(Hider hider, Player player) {
        Block cell = hider.cell;
        double top = standHeight(shape(cell));
        Location at = player.getLocation();
        hider.snap = new Location(world, cell.getX() + 0.5, cell.getY() + top, cell.getZ() + 0.5, at.getYaw(), at.getPitch());
        // 0.3.0 : plafond trop bas pour un joueur debout sur le bloc : rapetisse a la place disponible (attribut de taille).
        double room = roomAbove(cell, top);
        hider.shrunk = room < PLAYER_HEIGHT;
        hns.scale(player, hider.shrunk ? Math.max(0.0625, (room - 0.05) / PLAYER_HEIGHT) : 1);
        // Le bloc est envoye au joueur AVANT son deplacement (la pose du vrai bloc n'est diffusee qu'en fin de tick) :
        // sinon son jeu le ferait tomber a travers un bloc qu'il ne connait pas encore.
        player.sendBlockChange(cell.getLocation(), cell.getBlockData());
        player.teleport(hider.snap);
    }

    /** Formes de collision ramenees a la case (de 0 a 1), comme shape(Block). */
    private static List<BoundingBox> relative(java.util.Collection<BoundingBox> boxes, Block block) {
        List<BoundingBox> list = new ArrayList<>();
        for (BoundingBox box : boxes) {
            list.add(box.getMaxY() > 2 ? box.clone().shift(-block.getX(), -block.getY(), -block.getZ()) : box);
        }
        return list;
    }

    /** Hauteur (dans la case) ou un joueur centre se tient sur le bloc : seules comptent les parties sous son emprise. */
    private static double standHeight(List<BoundingBox> boxes) {
        double top = 0;
        double min = 0.5 - HALF_WIDTH;
        double max = 0.5 + HALF_WIDTH;
        for (BoundingBox box : boxes) {
            if (box.getMinX() < max && box.getMaxX() > min && box.getMinZ() < max && box.getMaxZ() > min) {
                top = Math.max(top, box.getMaxY());
            }
        }
        return top > 1.5 ? 1.0 : top;
    }

    /** Place libre au-dessus d'un joueur centre debout a la hauteur top de la case (plafond cherche sur 3 cases). */
    private static double roomAbove(Block cell, double top) {
        double min = 0.5 - HALF_WIDTH;
        double max = 0.5 + HALF_WIDTH;
        double ceiling = top + PLAYER_HEIGHT;
        for (int dy = 1; dy <= 3; dy++) {
            for (BoundingBox box : shape(cell.getRelative(0, dy, 0))) {
                double bottom = dy + box.getMinY();
                if (bottom >= top && box.getMinX() < max && box.getMaxX() > min && box.getMinZ() < max && box.getMaxZ() > min) {
                    ceiling = Math.min(ceiling, bottom);
                }
            }
        }
        return ceiling - top;
    }

    /** Le hider redevient un joueur visible : sa case retrouve son contenu d'origine. */
    private void unsolidify(Hider hider, Player player) {
        Block cell = hider.cell;
        if (cell == null) {
            return;
        }
        cell.setBlockData(hider.cellOriginal, false);
        hider.cell = null;
        hider.cellOriginal = null;
        hider.snap = null;
        hider.stillTicks = 0;
        if (player != null) {
            if (hider.shrunk && player.isOnline() && hider.ground != null) {
                // Rapetisse sous un plafond bas : il reprend sa place d'avant AVANT de retrouver sa taille, sinon il
                // grandirait dans le plafond.
                Location back = hider.ground.clone();
                back.setYaw(player.getLocation().getYaw());
                back.setPitch(player.getLocation().getPitch());
                player.teleport(back);
            }
            hns.scale(player, 1);
            hns.reveal(player);
            hider.lastPos = player.getLocation();
        }
        hider.shrunk = false;
    }

    // ------------------------------------------------------------------ actions du hider

    /** Clic droit sur un objet de la barre du hider. */
    public void useItem(Player player, String kind) {
        Hider hider = hiders.get(player.getUniqueId());
        if (hider == null || !playing()) {
            return;
        }
        if (kind.equals(HideItems.SOUNDS)) {
            hns.menus().openSounds(player, this);
        } else if (kind.equals(HideItems.BLOCK)) {
            hns.menus().openBlocks(player, this);
        } else if (kind.equals(HideItems.ESCAPE)) {
            escape(player, hider);
        } else if (kind.equals(HideItems.STATE)) {
            changeState(player, hider, player.isSneaking());
        }
    }

    /**
     * Objet « Orienter le bloc » (0.3.0, demande de LeKiwi06 : la statue du golem de cuivre n'etait ni dans le bon sens
     * ni dans la bonne posture) : clic droit, orientation suivante ; accroupi + clic droit, posture suivante (statue du
     * golem de cuivre ; les autres blocs tournent). Garde pour les prochaines fois ou le hider devient solide.
     */
    private void changeState(Player player, Hider hider, boolean pose) {
        BlockData data = hider.state.clone();
        Component done;
        if (pose && data instanceof CopperGolemStatue statue) {
            CopperGolemStatue.Pose[] poses = CopperGolemStatue.Pose.values();
            CopperGolemStatue.Pose next = poses[(statue.getCopperGolemPose().ordinal() + 1) % poses.length];
            statue.setCopperGolemPose(next);
            String name = next.name().toLowerCase(java.util.Locale.ROOT);
            done = t("hns.state-pose", "<gray>Posture : <white><pose>", "pose", t("hns.pose." + name, poseName(next)));
        } else {
            Component turned = turn(data);
            if (turned == null) {
                player.sendActionBar(t("hns.state-none", "<red>Ce bloc ne peut pas être tourné."));
                return;
            }
            done = t("hns.state-facing", "<gray>Orientation : <white><dir>", "dir", turned);
        }
        hider.state = data;
        if (hider.solid()) {
            hider.cell.setBlockData(data.clone(), false);
            standOn(hider, player); // la forme peut changer avec l'orientation
        }
        player.sendActionBar(done);
    }

    /** Tourne le bloc d'un cran (orientation, rotation fine ou axe) ; renvoie le nom de la nouvelle orientation, ou null. */
    private Component turn(BlockData data) {
        if (data instanceof Directional directional) {
            List<BlockFace> faces = new ArrayList<>();
            for (BlockFace face : TURN) {
                if (directional.getFaces().contains(face)) {
                    faces.add(face);
                }
            }
            if (faces.size() > 1) {
                BlockFace next = faces.get((faces.indexOf(directional.getFacing()) + 1) % faces.size());
                directional.setFacing(next);
                return faceName(next);
            }
        }
        if (data instanceof Rotatable rotatable) {
            BlockFace next = TURN_FINE.get((TURN_FINE.indexOf(rotatable.getRotation()) + 1) % TURN_FINE.size());
            rotatable.setRotation(next);
            return faceName(next);
        }
        if (data instanceof Orientable orientable && orientable.getAxes().size() > 1) {
            List<Axis> axes = new ArrayList<>(orientable.getAxes());
            axes.sort(Comparator.naturalOrder());
            Axis next = axes.get((axes.indexOf(orientable.getAxis()) + 1) % axes.size());
            orientable.setAxis(next);
            String name = next.name().toLowerCase(java.util.Locale.ROOT);
            return t("hns.axis." + name, switch (next) {
                case X -> "est-ouest";
                case Y -> "debout";
                case Z -> "nord-sud";
            });
        }
        return null;
    }

    private Component faceName(BlockFace face) {
        String name = switch (face) {
            case NORTH -> "nord";
            case EAST -> "est";
            case SOUTH -> "sud";
            case WEST -> "ouest";
            case UP -> "vers le haut";
            case DOWN -> "vers le bas";
            case NORTH_EAST -> "nord-est";
            case SOUTH_EAST -> "sud-est";
            case SOUTH_WEST -> "sud-ouest";
            case NORTH_WEST -> "nord-ouest";
            default -> face.name().toLowerCase(java.util.Locale.ROOT);
        };
        return t("hns.face." + face.name().toLowerCase(java.util.Locale.ROOT), name);
    }

    private static String poseName(CopperGolemStatue.Pose pose) {
        return switch (pose) {
            case STANDING -> "debout";
            case SITTING -> "assis";
            case RUNNING -> "en course";
            case STAR -> "en étoile";
        };
    }

    /** Secondes d'attente avant le prochain changement de bloc (0 : possible tout de suite). */
    public int blockWait(Hider hider) {
        if (!hider.chosen) {
            return 0;
        }
        long ready = hider.lastChange + Math.max(0, setting("block-change-seconds", 30)) * 20L;
        return ready <= ticks ? 0 : (int) Math.ceil((ready - ticks) / 20.0);
    }

    /** Le hider choisit son bloc dans le menu. Renvoie null si le changement est fait, sinon le motif du refus. */
    public Component changeBlock(Player player, Material material) {
        Hider hider = hiders.get(player.getUniqueId());
        if (hider == null || !playing() || !pool.contains(material)) {
            return t("hns.block-closed", "<red>Vous ne pouvez pas changer de bloc maintenant.");
        }
        int wait = blockWait(hider);
        if (wait > 0) {
            return t("hns.block-wait", "<red>Prochain changement de bloc dans <s> s.", "s", wait);
        }
        hider.chosen = true;
        hider.lastChange = ticks;
        if (hider.block == material) {
            return null;
        }
        hider.block = material;
        hider.state = material.createBlockData();
        giveHotbar(player, hider);
        if (hider.solid()) {
            hider.cell.setBlockData(hider.state.clone(), false);
            standOn(hider, player); // le dessus du nouveau bloc n'est pas forcement a la meme hauteur
        }
        return null;
    }

    /** Portee d'ecoute des sons des hiders (« Portee des sons », 30 blocs par defaut). */
    private double hearingRange() {
        return Math.max(16, setting("sound-range", 30));
    }

    private void playAt(Hider hider, Player player, String sound) {
        Location spot = hider.solid() ? hider.cell.getLocation().add(0.5, 0.5, 0.5) : player.getEyeLocation();
        // Un son s'entend a 16 blocs par point de volume : 0.2.0, portee reglable (30 blocs par defaut).
        float volume = (float) (hearingRange() / 16);
        world.playSound(spot, sound, SoundCategory.MASTER, volume, 1f);
    }

    /** Soundboard : joue le son a l'endroit du hider ; rapporte des points si un seeker est assez pres. */
    public void playBoard(Player player, String key) {
        Hider hider = hiders.get(player.getUniqueId());
        if (hider == null || !playing()) {
            return;
        }
        SoundBoard.Entry entry = hns.sounds().get(key);
        if (entry == null) {
            player.sendActionBar(t("hns.sound-gone", "<red>Ce son n'existe plus."));
            return;
        }
        if (ticks < hider.nextSound) {
            player.sendActionBar(t("hns.sound-wait", "<red>Prochain son dans <s> s.",
                    "s", (int) Math.ceil((hider.nextSound - ticks) / 20.0)));
            return;
        }
        hider.nextSound = ticks + Math.max(0, setting("sound-cooldown-seconds", 3)) * 20L;
        playAt(hider, player, entry.key());
        // 0.3.0 : chaque son entendu compte (plus de delai de 10 s entre deux sons comptes).
        double value = setting("points-sound-hundredths", 25) / 100.0;
        if (phase == Phase.RUNNING && value > 0 && seekerNear(player.getLocation())) {
            addPoints(hider.uuid, value);
            player.sendActionBar(t("hns.sound-points", "<gold>+<points> pt <gray>(son entendu par un seeker)",
                    "points", StatsService.formatPoints(value)));
        }
    }

    /** Un seeker libre est-il a portee d'ecoute ? */
    private boolean seekerNear(Location spot) {
        return nearestSeeker(spot) <= Math.max(1, setting("sound-points-range", 24));
    }

    /** Distance du seeker libre (connecte, hors de la salle) le plus proche ; Double.MAX_VALUE s'il n'y en a aucun. */
    private double nearestSeeker(Location spot) {
        double best = Double.MAX_VALUE;
        for (Seeker seeker : seekers.values()) {
            Player player = presentPlayer(seeker.uuid);
            if (player != null && seeker.jail <= 0 && player.getWorld() == spot.getWorld()) {
                best = Math.min(best, player.getLocation().distance(spot));
            }
        }
        return best;
    }

    /** Une evasion de plus tous les N points gagnes dans la partie (0.3.0 : elles s'accumulent). */
    private int escapeEvery() {
        return Math.max(1, setting("escape-points", 5));
    }

    private static int escapesLeft(Hider hider) {
        return Math.max(0, hider.escapesEarned - hider.escapesUsed);
    }

    private void refreshEscape(Player player, Hider hider) {
        player.getInventory().setItem(2, hns.items().escape(escapeEvery(), escapesLeft(hider)));
    }

    /** Evasion : une de la reserve (0.3.0 : plus de recharge de 60 s ; refusee seulement pendant l'evasion en cours). */
    private void escape(Player player, Hider hider) {
        int left = escapesLeft(hider);
        if (left <= 0) {
            player.sendActionBar(t("hns.escape-none", "<red>Aucune évasion en réserve : une de plus tous les <points> points.",
                    "points", escapeEvery()));
            return;
        }
        if (ticks < hider.escapeUntil) {
            player.sendActionBar(t("hns.escape-active", "<red>Évasion déjà en cours."));
            return;
        }
        hider.escapesUsed++;
        hider.escapeUntil = ticks + ESCAPE_TICKS;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ESCAPE_TICKS, ESCAPE_AMPLIFIER, false, false, true));
        refreshEscape(player, hider);
        player.sendActionBar(t("hns.escape-used-left", "<light_purple>Évasion ! <gray>Il en reste <white><n></white>.", "n", left - 1));
    }

    private void addPoints(UUID uuid, double value) {
        if (value <= 0) {
            return;
        }
        double total = points.merge(uuid, value, Double::sum);
        Hider hider = hiders.get(uuid);
        int earned = (int) Math.floor(total / escapeEvery() + 1e-6);
        if (hider != null && earned > hider.escapesEarned) {
            hider.escapesEarned = earned;
            Player player = presentPlayer(uuid);
            if (player != null) {
                refreshEscape(player, hider);
                player.sendMessage(plugin.prefix().append(t("hns.escape-gained",
                        "<light_purple>Nouvelle évasion ! <gray>En réserve : <white><n></white>. Clic droit sur la patte de lapin (Vitesse V, 2 s).",
                        "n", escapesLeft(hider))));
            }
        }
    }

    // ------------------------------------------------------------------ coups des seekers

    /** Le seeker vient de faire un clic droit : le mouvement de bras qui suit n'est pas un coup. */
    public void noteRightClick(Player player) {
        Seeker seeker = seekers.get(player.getUniqueId());
        if (seeker != null) {
            seeker.lastRightClick = System.currentTimeMillis();
        }
    }

    private boolean canHit(Seeker seeker) {
        return seeker != null && phase == Phase.RUNNING && seeker.jail <= 0 && present(seeker.uuid);
    }

    /** Coup direct d'un joueur sur un autre (toujours sans degats) : un seeker qui touche un hider l'elimine. */
    public void directHit(Player attacker, Player victim) {
        Seeker seeker = seekers.get(attacker.getUniqueId());
        Hider hider = hiders.get(victim.getUniqueId());
        if (hider == null || !canHit(seeker) || !present(hider.uuid)) {
            return;
        }
        seeker.lastDirectHit = System.currentTimeMillis();
        find(attacker, hider);
    }

    /**
     * Clic gauche d'un seeker (mouvement de bras) : on cherche le bloc vise. Un hider solide est elimine ; un bloc du
     * decor d'un type de la liste de la map coute un demi-coeur ; les autres blocs ne coutent rien.
     */
    public void swing(Player player) {
        Seeker seeker = seekers.get(player.getUniqueId());
        if (!canHit(seeker)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - seeker.lastRightClick < 200 || now - seeker.lastDirectHit < 300 || now - seeker.lastClick < 250) {
            return;
        }
        AttributeInstance range = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        Location eye = player.getEyeLocation();
        RayTraceResult hit = world.rayTrace(eye, eye.getDirection(), range == null ? 4.5 : range.getValue(),
                FluidCollisionMode.NEVER, false, 0.0,
                entity -> entity instanceof Player other && !other.equals(player) && player.canSee(other)
                        && other.getGameMode() != GameMode.SPECTATOR);
        if (hit == null || hit.getHitEntity() != null || hit.getHitBlock() == null) {
            // Rien de vise, ou un joueur : le coup direct est traite a part (directHit).
            return;
        }
        seeker.lastClick = now;
        Block block = hit.getHitBlock();
        for (Hider hider : hiders.values()) {
            if (block.equals(hider.cell)) {
                find(player, hider);
                return;
            }
        }
        if (pool.contains(block.getType())) {
            mistake(player, seeker);
        }
    }

    /** Erreur : un demi-coeur perdu, pris d'abord sur l'absorption. A 0 coeur : retour dans la salle des seekers. */
    private void mistake(Player player, Seeker seeker) {
        player.playHurtAnimation(0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);
        double absorption = player.getAbsorptionAmount();
        if (absorption >= 1) {
            player.setAbsorptionAmount(absorption - 1);
        } else {
            double health = player.getHealth() - 1;
            if (health <= 0) {
                seeker.jail = Math.max(0, setting("seeker-respawn-seconds", 15));
                player.setAbsorptionAmount(0);
                player.setHealth(SEEKER_HEALTH);
                if (seeker.jail > 0) {
                    darken(player, true);
                    player.teleport(seekerRoom());
                    player.sendMessage(plugin.prefix().append(t("hns.seeker-out",
                            "<red>Plus de cœurs ! <gray>Vous repartez dans <white><s></white> s.", "s", seeker.jail)));
                }
                return;
            }
            player.setHealth(health);
        }
        player.sendActionBar(t("hns.mistake", "<red>Ce n'était qu'un bloc : un demi-cœur perdu."));
    }

    /** Hider trouve : le seeker recupere ses 5 coeurs et gagne de l'absorption selon la vie qu'il avait. */
    private void find(Player seekerPlayer, Hider hider) {
        int hearts = (int) Math.floor(seekerPlayer.getHealth() / 2.0);
        seekerPlayer.setHealth(SEEKER_HEALTH);
        if (hearts >= 3) {
            seekerPlayer.setAbsorptionAmount(Math.min(SEEKER_ABSORPTION, seekerPlayer.getAbsorptionAmount() + (hearts - 2) * 2.0));
        }
        seekerPlayer.playSound(seekerPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        found++;
        finds.merge(seekerPlayer.getUniqueId(), 1, Integer::sum);
        addPoints(seekerPlayer.getUniqueId(), setting("points-find", 6));
        broadcast(t("hns.found", "<gray><hider> <red>a été trouvé par <white><seeker></white>.",
                "hider", names.getOrDefault(hider.uuid, "?"), "seeker", seekerPlayer.getName()));
        eliminate(hider);
        checkEnd();
    }

    /** Le hider n'est plus en jeu : spectateur, ou seeker apres un passage dans la salle (reglage de la partie). */
    private void eliminate(Hider hider) {
        Player player = Bukkit.getPlayer(hider.uuid);
        unsolidify(hider, player);
        hiders.remove(hider.uuid);
        if (player == null || !present(hider.uuid)) {
            return;
        }
        hns.clearState(player);
        if (becomeSeeker) {
            Seeker seeker = new Seeker(hider.uuid);
            seeker.jail = Math.max(0, setting("seeker-respawn-seconds", 15));
            seekers.put(hider.uuid, seeker);
            setupSeeker(player, seeker.jail > 0);
            title(Set.of(hider.uuid), t("hns.found-title", "<red><bold>Trouvé !"),
                    t("hns.found-seeker-sub", "<gray>Vous devenez seeker dans <white><s></white> s", "s", seeker.jail), 5, 50, 10);
        } else {
            becomeMatchSpectator(player);
            title(Set.of(hider.uuid), t("hns.found-title", "<red><bold>Trouvé !"),
                    t("hns.found-spectator-sub", "<gray>Vous regardez la fin de la partie"), 5, 50, 10);
        }
    }

    // ------------------------------------------------------------------ fin de partie

    /** Plus aucun hider en jeu et connecte : les seekers gagnent. Plus aucun seeker connecte : les hiders gagnent. */
    private void checkEnd() {
        if (!playing()) {
            return;
        }
        boolean anyHider = false;
        for (UUID uuid : hiders.keySet()) {
            anyHider |= present(uuid);
        }
        boolean anySeeker = false;
        for (UUID uuid : seekers.keySet()) {
            anySeeker |= present(uuid);
        }
        if (!anyHider) {
            finish(Winner.SEEKERS);
        } else if (!anySeeker) {
            finish(Winner.HIDERS);
        }
    }

    private void finish(Winner result) {
        if (!playing()) {
            return;
        }
        winner = result;
        boolean searched = phase == Phase.RUNNING;
        phase = Phase.ENDING;
        secondsLeft = Math.max(1, setting("end-delay-seconds", 8));
        pendingSounds.clear();
        if (result == Winner.HIDERS && searched) {
            for (UUID uuid : hiders.keySet()) {
                if (present(uuid)) {
                    addPoints(uuid, setting("points-survivor", 8));
                }
            }
        }
        if (result == Winner.SEEKERS && found > 0) {
            for (UUID uuid : seekers.keySet()) {
                if (present(uuid)) {
                    addPoints(uuid, setting("points-all-found", 5));
                }
            }
        }
        // Tout le monde redevient un joueur visible ; les blocs des hiders solides sont retires.
        for (Hider hider : hiders.values()) {
            unsolidify(hider, Bukkit.getPlayer(hider.uuid));
        }
        for (UUID uuid : names.keySet()) {
            Player player = presentPlayer(uuid);
            if (player != null) {
                hns.clearState(player);
                darken(player, false);
            }
        }
        restoreAllBoards();
        Component headline = result == Winner.HIDERS
                ? t("hns.win-hiders", "<green><bold>Les hiders gagnent !")
                : t("hns.win-seekers", "<red><bold>Les seekers gagnent !");
        List<String> survivors = new ArrayList<>();
        for (UUID uuid : hiders.keySet()) {
            survivors.add(names.getOrDefault(uuid, "?"));
        }
        Collections.sort(survivors);
        Component detail = result == Winner.HIDERS
                ? t("hns.win-hiders-sub", "<gray>En vie : <white><names>", "names", String.join(", ", survivors))
                : t("hns.win-seekers-sub", "<gray>Tous les hiders ont été trouvés");
        title(members, headline, detail, 5, 70, 15);
        broadcast(headline);
        results();
    }

    /**
     * Classement de fin de partie et credit des points : regle commune (KG_BoatRace 1.5.0) : chacun recoit ses points +
     * la moyenne de ceux des joueurs classes en dessous. Tous les joueurs qui ont commence la partie sont classes, meme
     * deconnectes ou partis (avec les points gagnes jusque-la).
     */
    private void results() {
        List<UUID> ordered = new ArrayList<>(names.keySet());
        ordered.sort(Comparator.comparingDouble((UUID uuid) -> -points.getOrDefault(uuid, 0.0)));
        Map<UUID, Double> credited = new HashMap<>();
        double below = 0;
        for (int i = ordered.size() - 1; i >= 0; i--) {
            int count = ordered.size() - 1 - i;
            double own = points.getOrDefault(ordered.get(i), 0.0);
            credited.put(ordered.get(i), Math.round((own + (count > 0 ? below / count : 0)) * 100) / 100.0);
            below += own;
        }
        broadcast(t("hns.results", "<gold><bold>Classement aux points"));
        List<Map<String, Object>> logged = new ArrayList<>();
        int rank = 1;
        for (UUID uuid : ordered) {
            double own = Math.round(points.getOrDefault(uuid, 0.0) * 100) / 100.0;
            double total = credited.get(uuid);
            String name = names.get(uuid);
            broadcast(t("hns.results-line", "<gray><rank>. <white><name></white> <dark_gray>- <gold><points> pts <gray>(crédité <white><total></white>)",
                    "rank", rank, "name", name, "points", StatsService.formatPoints(own), "total", StatsService.formatPoints(total)));
            boolean counted = credit(uuid, name, total);
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("player", uuid.toString());
            one.put("name", name);
            one.put("role", startRoles.get(uuid));
            one.put("rank", rank);
            one.put("finds", finds.getOrDefault(uuid, 0));
            one.put("survived", hiders.containsKey(uuid));
            one.put("points", own);
            one.put("credited", total);
            one.put("counted", counted);
            logged.add(one);
            rank++;
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("winner", winner.name().toLowerCase(java.util.Locale.ROOT));
        fields.put("seekSeconds", elapsed);
        fields.put("found", found);
        fields.put("becomeSeeker", becomeSeeker);
        fields.put("results", logged);
        plugin.ranking().log(minigame().id(), "match", fields);
    }

    /**
     * Credite les points au classement (KG_ScoreBoards, points decimaux) et note l'attribution dans le journal des
     * parties (evenement « points », comme KalGames : /classements verifier retrouve les points non comptes).
     */
    private boolean credit(UUID uuid, String name, double value) {
        if (value <= 0) {
            return false;
        }
        Player player = Bukkit.getPlayer(uuid);
        boolean operator = player != null ? plugin.scores().excluded(player)
                : Bukkit.getOfflinePlayer(uuid).isOp() && plugin.getConfig().getBoolean("stats.exclude-operators", true);
        String reason = operator ? "operateur" : !ranked() ? "partie-non-classee" : null;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("player", uuid.toString());
        fields.put("name", name);
        fields.put("platform", uuid.getMostSignificantBits() == 0 ? "bedrock" : "java");
        fields.put("points", value);
        fields.put("counted", reason == null);
        fields.put("reason", reason);
        plugin.ranking().log(minigame().id(), "points", fields);
        if (reason != null) {
            return false;
        }
        plugin.ranking().stats().addPoints(minigame().id(), uuid, name, value);
        plugin.ranking().boards().refreshSoon();
        if (player != null) {
            player.sendMessage(plugin.prefix().append(t("hns.credited", "<gold>+<points> point(s) au classement",
                    "points", StatsService.formatPoints(value))));
        }
        return true;
    }

    // ------------------------------------------------------------------ depart / retour d'un joueur

    @Override
    protected void onMemberLeft(UUID uuid, boolean wasParticipant, boolean disconnected) {
        greeted.remove(uuid);
        restoreBoard(uuid);
        Player player = Bukkit.getPlayer(uuid);
        Hider hider = hiders.get(uuid);
        Seeker seeker = seekers.get(uuid);
        if (!playing() || (hider == null && seeker == null)) {
            volunteers.remove(uuid);
            if (player != null) {
                hns.clearState(player);
            }
            return;
        }
        if (hider != null) {
            unsolidify(hider, player);
        }
        String name = names.getOrDefault(uuid, "Un joueur");
        if (disconnected) {
            // Il garde son role et peut revenir jusqu'a la fin de la partie (aucun point pendant l'absence).
            boolean inRoom = seeker != null && (phase == Phase.COUNTDOWN || seeker.jail > 0);
            absent.put(uuid, new Absent(player == null ? null : player.getLocation(),
                    player == null ? SEEKER_HEALTH : player.getHealth(), player == null ? 0 : player.getAbsorptionAmount(), inRoom));
            broadcast(t("hns.disconnected", "<gray><name> s'est déconnecté : il peut revenir jusqu'à la fin de la partie.", "name", name));
        } else {
            hiders.remove(uuid);
            seekers.remove(uuid);
            broadcast(t("hns.left", "<gray><name> <red>a quitté la partie.", "name", name));
        }
        if (player != null) {
            hns.clearState(player);
        }
        checkEnd();
    }

    /** Retour d'un joueur deconnecte : il reprend son role la ou il etait. */
    private void restore(Player player, Absent back) {
        UUID uuid = player.getUniqueId();
        if (!player.isOnline() || !members.contains(uuid) || !playing()) {
            return;
        }
        Hider hider = hiders.get(uuid);
        Seeker seeker = seekers.get(uuid);
        if (hider != null) {
            setupHider(player, hider, back.location() == null ? hiderSpawn() : back.location());
        } else if (seeker != null) {
            boolean waiting = phase == Phase.COUNTDOWN || seeker.jail > 0;
            setupSeeker(player, waiting);
            if (!waiting && !back.inRoom() && back.location() != null) {
                player.teleport(back.location());
            }
            player.setHealth(Math.max(1, Math.min(SEEKER_HEALTH, back.health())));
            player.setAbsorptionAmount(Math.max(0, Math.min(SEEKER_ABSORPTION, back.absorption())));
        } else {
            return;
        }
        broadcast(t("hns.returned", "<gray><name> est de retour.", "name", player.getName()));
    }

    @Override
    protected void onMatchReset() {
        restoreAllBoards();
        resetState();
        volunteers.clear();
        pool = List.of();
    }

    @Override
    protected void onClose() {
        for (Hider hider : hiders.values()) {
            unsolidify(hider, null);
        }
        restoreAllBoards();
        Set<UUID> known = new HashSet<>(names.keySet());
        known.addAll(members);
        for (UUID uuid : known) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                hns.clearState(player);
            }
        }
    }

    // ------------------------------------------------------------------ regles

    @Override
    public void handleDeath(Player victim, Player killer) {
        // Personne ne prend de degats dans ce jeu.
    }

    @Override
    public void handleVoid(Player player) {
        UUID uuid = player.getUniqueId();
        if (isMatchSpectator(uuid)) {
            return;
        }
        if (playing()) {
            Hider hider = hiders.get(uuid);
            Seeker seeker = seekers.get(uuid);
            if (hider != null) {
                unsolidify(hider, player);
                player.teleport(hiderSpawn());
                hider.lastPos = player.getLocation();
                return;
            }
            if (seeker != null) {
                player.teleport(phase == Phase.COUNTDOWN || seeker.jail > 0 ? seekerRoom() : seekerSpawn());
                return;
            }
        }
        arriveInStands(player);
    }

    @Override
    public boolean canBuild(Player player) {
        return false;
    }

    /** Portes, leviers... : utilisables par les joueurs de la partie (changements restaures a la fin). */
    @Override
    public boolean canInteract(Player player) {
        UUID uuid = player.getUniqueId();
        return playing() && (hiders.containsKey(uuid) || seekers.containsKey(uuid));
    }

    @Override
    public boolean canBreak(Player player, Block block) {
        return false;
    }

    @Override
    public boolean takesDamage(Player player) {
        return false;
    }

    @Override
    public boolean canFight(Player attacker, Player victim) {
        return false;
    }

    @Override
    public boolean inventoryLocked(Player player) {
        return true;
    }

    // ------------------------------------------------------------------ menu de la partie

    @Override
    public List<Component> menuInfo(Player player) {
        List<Component> lines = new ArrayList<>();
        lines.add(becomeSeeker
                ? t("hns.info-seeker", "<gray>Hider éliminé : <white>devient seeker")
                : t("hns.info-spectator", "<gray>Hider éliminé : <white>spectateur"));
        return lines;
    }

    @Override
    public List<MenuAction> menuActions(Player player) {
        List<MenuAction> actions = new ArrayList<>();
        if ((phase == Phase.WAITING || phase == Phase.STARTING) && members.contains(player.getUniqueId())) {
            Component label = volunteers.contains(player.getUniqueId())
                    ? t("hns.volunteer-yes", "<green>✔ Je veux être seeker")
                    : t("hns.volunteer-no", "<yellow>Je veux être seeker");
            actions.add(new MenuAction(label, p -> {
                toggleVolunteer(p);
                plugin.menus().openGameMenu(p);
            }));
        }
        return actions;
    }

    // ------------------------------------------------------------------ affichage

    private static String clock(int seconds) {
        int value = Math.max(0, seconds);
        return value / 60 + ":" + (value % 60 < 10 ? "0" : "") + value % 60;
    }

    @Override
    protected Component statusFor(Player player) {
        UUID uuid = player.getUniqueId();
        if (playing()) {
            Hider hider = hiders.get(uuid);
            Seeker seeker = seekers.get(uuid);
            String score = StatsService.formatPoints(Math.round(points.getOrDefault(uuid, 0.0) * 100) / 100.0);
            if (phase == Phase.COUNTDOWN && (hider != null || seeker != null)) {
                return hider != null
                        ? t("hns.status-hide", "<green>Cachez-vous : <white><s></white> s <dark_gray>| <gray><state>", "s", secondsLeft,
                        "state", hider.solid() ? "solide" : "en mouvement")
                        : t("hns.status-wait", "<red>Les hiders se cachent : <white><s></white> s", "s", secondsLeft);
            }
            if (hider != null) {
                return t("hns.status-hider", "<green>Hider <dark_gray>| <white><time></white> <dark_gray>| <gold><points> pts <dark_gray>| <gray><state>",
                        "time", clock(secondsLeft), "points", score, "state", hider.solid() ? "solide" : "en mouvement");
            }
            if (seeker != null && seeker.jail > 0) {
                return t("hns.status-jail", "<red>Retour dans <white><s></white> s", "s", seeker.jail);
            }
            if (seeker != null) {
                return t("hns.status-seeker", "<red>Seeker <dark_gray>| <white><time></white> <dark_gray>| <gold><points> pts <dark_gray>| <gray>hiders : <white><n>",
                        "time", clock(secondsLeft), "points", score, "n", hiders.size());
            }
        }
        if (isPublic()) {
            return queueStatus(player);
        }
        if (phase == Phase.WAITING) {
            return t("hns.status-lobby", "<gold>Partie privée <white><code></white> <dark_gray>- <gray>en attente de l'hôte", "code", code);
        }
        return null;
    }
}
