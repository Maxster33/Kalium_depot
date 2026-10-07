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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

    /** Un hider en vie. */
    public static final class Hider {
        final UUID uuid;
        Material block;
        /** Le hider a deja choisi son bloc lui-meme (le delai entre deux changements ne compte qu'ensuite). */
        boolean chosen;
        long lastChange;
        Location lastPos;
        int stillTicks;
        /** Case occupee par le hider solide (null : en mouvement), son contenu d'origine et la position du hider. */
        Block cell;
        BlockData cellOriginal;
        Location snap;
        long nextSound;
        long nextSoundPoints;
        boolean escapeUnlocked;
        long escapeReady;
        int survived;

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
    private List<Material> pool = List.of();
    private long ticks;
    private int elapsed;
    private int found;
    private Winner winner = Winner.NONE;

    public HideInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                        boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        this.hns = KGHideAndSeek.get();
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

    private Location seekerSpawn() {
        return point("seeker-spawn");
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

    /** Barre d'objets du hider (0.2.0 : un seul objet pour le soundboard) : sons, changement de bloc, evasion ; bloc sur la tete. */
    public void giveHotbar(Player player, Hider hider) {
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(0, hns.items().soundsMenu());
        inventory.setItem(1, hns.items().blockChanger(hider.block));
        inventory.setItem(2, hns.items().escape(setting("escape-points", 5), setting("escape-cooldown-seconds", 60)));
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

    /** Points de survie : comptes seulement pendant la recherche, pour les hiders en vie et connectes. */
    private void survivalPoints() {
        int interval = Math.max(1, setting("survive-interval-seconds", 20));
        for (Hider hider : hiders.values()) {
            if (!present(hider.uuid)) {
                continue;
            }
            hider.survived++;
            if (hider.survived % interval == 0) {
                addPoints(hider.uuid, setting("points-survive", 1));
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

    // ------------------------------------------------------------------ solidite

    private static boolean movedFrom(Location snap, Location now) {
        if (snap == null || snap.getWorld() != now.getWorld()) {
            return true;
        }
        double dx = now.getX() - snap.getX();
        double dz = now.getZ() - snap.getZ();
        return dx * dx + dz * dz > SOLID_SLACK * SOLID_SLACK || Math.abs(now.getY() - snap.getY()) > 0.3;
    }

    /** Une case peut recevoir un hider solide si rien n'y bloque le passage (air, herbe, fleur, eau, tapis fin). */
    private static boolean replaceable(Block block) {
        return block.isEmpty() || block.isPassable() || block.getBoundingBox().getHeight() <= 0.2;
    }

    /**
     * Le hider immobile devient son bloc : un vrai bloc est pose dans sa case (contenu d'origine garde, remis des qu'il
     * bouge) et il est cache a tous les autres joueurs. 0.2.0 : le hider est place DEBOUT SUR son bloc, au centre : il
     * le voit sous ses pieds, et son jeu ne le repousse plus hors du bloc (en 0.1.0 il restait dedans, avec de l'air
     * envoye a son seul jeu : il etait parfois repousse et redevenait mobile). Sous un plafond bas il s'y tient couche
     * (comportement du jeu). Renvoie false si la case ne convient pas.
     */
    private boolean solidify(Hider hider, Player player) {
        Location at = player.getLocation();
        Block cell = world.getBlockAt(at.getBlockX(), (int) Math.floor(at.getY() + 0.5), at.getBlockZ());
        boolean taken = false;
        for (Hider other : hiders.values()) {
            taken |= other != hider && cell.equals(other.cell);
        }
        // Les pieds doivent etre au bas de la case (pas sur une dalle ou un escalier) : sinon le bloc flotterait.
        boolean level = Math.abs(at.getY() - cell.getY()) <= 0.2;
        if (taken || !level || !inBounds(cell.getX(), cell.getY(), cell.getZ()) || !replaceable(cell)) {
            player.sendActionBar(t("hns.solid-refused", "<red>Impossible de devenir solide ici."));
            return false;
        }
        if (!cell.getRelative(BlockFace.UP).isPassable()) {
            player.sendActionBar(t("hns.solid-no-room", "<red>Pas assez de place au-dessus de vous pour devenir solide."));
            return false;
        }
        BlockData original = cell.getBlockData();
        trackOriginal(cell.getX(), cell.getY(), cell.getZ(), original);
        cell.setBlockData(hider.block.createBlockData(), false);
        hider.cell = cell;
        hider.cellOriginal = original;
        standOn(hider, player);
        hns.conceal(player);
        player.sendActionBar(t("hns.solid-on-2", "<green>Vous êtes solide : votre bloc est sous vos pieds. <gray>Bougez pour redevenir un joueur."));
        return true;
    }

    /** Place le hider solide debout sur son bloc, au centre de la case (a la hauteur reelle du dessus du bloc). */
    private void standOn(Hider hider, Player player) {
        Block cell = hider.cell;
        double top = 0;
        for (BoundingBox box : cell.getCollisionShape().getBoundingBoxes()) {
            top = Math.max(top, box.getMaxY());
        }
        if (top > 2) {
            top -= cell.getY(); // forme donnee en coordonnees du monde
        }
        if (top <= 0 || top > 1.5) {
            top = 1.0;
        }
        Location at = player.getLocation();
        hider.snap = new Location(world, cell.getX() + 0.5, cell.getY() + top, cell.getZ() + 0.5, at.getYaw(), at.getPitch());
        // Le bloc est envoye au joueur AVANT son deplacement (la pose du vrai bloc n'est diffusee qu'en fin de tick) :
        // sinon son jeu le ferait tomber a travers un bloc qu'il ne connait pas encore.
        player.sendBlockChange(cell.getLocation(), cell.getBlockData());
        player.teleport(hider.snap);
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
            hns.reveal(player);
            hider.lastPos = player.getLocation();
        }
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
        }
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
        giveHotbar(player, hider);
        if (hider.solid()) {
            hider.cell.setBlockData(material.createBlockData(), false);
            standOn(hider, player); // le dessus du nouveau bloc n'est pas forcement a la meme hauteur
        }
        return null;
    }

    private void playAt(Hider hider, Player player, String sound) {
        Location spot = hider.solid() ? hider.cell.getLocation().add(0.5, 0.5, 0.5) : player.getEyeLocation();
        // Un son s'entend a 16 blocs par point de volume : 0.2.0, portee reglable (30 blocs par defaut).
        float volume = Math.max(1f, setting("sound-range", 30) / 16f);
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
        double value = setting("points-sound-hundredths", 25) / 100.0;
        if (phase == Phase.RUNNING && value > 0 && ticks >= hider.nextSoundPoints && seekerNear(player.getLocation())) {
            hider.nextSoundPoints = ticks + Math.max(0, setting("sound-points-seconds", 10)) * 20L;
            addPoints(hider.uuid, value);
            player.sendActionBar(t("hns.sound-points", "<gold>+<points> pt <gray>(son entendu par un seeker)",
                    "points", StatsService.formatPoints(value)));
        }
    }

    /** Un seeker libre est-il a portee d'ecoute ? */
    private boolean seekerNear(Location spot) {
        double range = Math.max(1, setting("sound-points-range", 24));
        for (Seeker seeker : seekers.values()) {
            Player player = presentPlayer(seeker.uuid);
            if (player != null && seeker.jail <= 0 && player.getWorld() == spot.getWorld()
                    && player.getLocation().distanceSquared(spot) <= range * range) {
                return true;
            }
        }
        return false;
    }

    private void escape(Player player, Hider hider) {
        if (!hider.escapeUnlocked) {
            player.sendActionBar(t("hns.escape-locked", "<red>Évasion disponible à <points> points.", "points", setting("escape-points", 5)));
            return;
        }
        if (ticks < hider.escapeReady) {
            player.sendActionBar(t("hns.escape-wait", "<red>Évasion disponible dans <s> s.",
                    "s", (int) Math.ceil((hider.escapeReady - ticks) / 20.0)));
            return;
        }
        hider.escapeReady = ticks + Math.max(1, setting("escape-cooldown-seconds", 60)) * 20L;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ESCAPE_TICKS, ESCAPE_AMPLIFIER, false, false, true));
        player.sendActionBar(t("hns.escape-used", "<light_purple>Évasion !"));
    }

    private void addPoints(UUID uuid, double value) {
        if (value <= 0) {
            return;
        }
        double total = points.merge(uuid, value, Double::sum);
        Hider hider = hiders.get(uuid);
        if (hider != null && !hider.escapeUnlocked && total >= setting("escape-points", 5)) {
            hider.escapeUnlocked = true;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(plugin.prefix().append(t("hns.escape-ready",
                        "<light_purple>Évasion débloquée : <gray>clic droit sur la patte de lapin (Vitesse V, 2 s).")));
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
        resetState();
        volunteers.clear();
        pool = List.of();
    }

    @Override
    protected void onClose() {
        for (Hider hider : hiders.values()) {
            unsolidify(hider, null);
        }
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
