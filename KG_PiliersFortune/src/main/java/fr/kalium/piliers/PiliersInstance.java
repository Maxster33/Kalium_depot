package fr.kalium.piliers;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.world.Template;
import fr.kalium.scoreboards.data.StatsService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Les piliers de la Fortune (demande de Maxster33, 06/10/2026). De 4 a 8 joueurs, chacun sur un pilier en pierre.
 * <ul>
 *   <li>Depart : vie, faim et saturation pleines, experience a 0, inventaire vide, mode survie ; immobiles pendant un
 *       decompte de 5 s affiche au milieu de l'ecran (COUNTDOWN).</li>
 *   <li>Partie (RUNNING, 10 min au plus) : un objet au hasard toutes les 5 s a chaque joueur en vie (pas le meme pour
 *       tous) ; inventaire plein : l'objet tombe a ses pieds. Barre du bas : temps restant et « Joueurs en vie : a/n ».</li>
 *   <li>Elimination : chute sous la couche -64, mort, ou depart (deconnexion, /hub). L'elimine passe en mode spectateur
 *       jusqu'a la fin. Fin : un seul joueur en vie (il gagne), plus personne, ou temps ecoule (egalite des survivants).</li>
 *   <li>Points : +2 par minute complete en vie, +5 par joueur elimine par soi : tue, ou elimine dans les 10 s apres un
 *       coup, une explosion d'un explosif qu'on a pose, une brulure par un feu ou une lave qu'on a pose, ou un passage
 *       dans l'eau qu'on a posee. En fin de partie : x1 pour le premier elimine, x2 pour le deuxieme... ; gagnant
 *       seul : x3 ; survivants a egalite : x1. Pas de regle commune « moyenne des joueurs
 *       classes en dessous » (decision de Maxster33).</li>
 * </ul>
 * Les blocs poses ou casses et les objets au sol sont remis en etat par le moteur de KalGames a la fin de la partie.
 */
public final class PiliersInstance extends GameInstance {

    /** Dernier coup recu d'un autre joueur. */
    private record Hit(UUID attacker, long at, boolean water) {
    }

    /** Poseur d'un bloc et heure de pose (l'eau ne compte que peu apres la pose de sa source). */
    private record Source(UUID owner, long placedAt) {
    }

    /** Envoi d'un distributeur ou oeuf d'apparition : joueur et tick du serveur. */
    private record Pending(UUID owner, int tick) {
    }

    private final KGPiliersFortune piliers;

    private final Set<UUID> alive = new LinkedHashSet<>();
    private final List<UUID> eliminated = new ArrayList<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Location> pillarOf = new HashMap<>();
    private final Map<UUID, Integer> minutes = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Hit> lastHit = new HashMap<>();
    private final Set<UUID> greeted = new java.util.HashSet<>();
    /** Poseur de chaque bloc pose pendant la partie (explosifs, sources de lave et d'eau, feu), et de ce qui en decoule. */
    private final Map<Long, Source> owners = new HashMap<>();
    /** Poseur des explosifs devenus entites (TNT amorcee, cristal de l'End, wagonnet de TNT). */
    private final Map<UUID, UUID> entityOwners = new HashMap<>();
    /** Poseur du dernier feu ou de la derniere lave qui a brule le joueur. */
    private final Map<UUID, UUID> burnBy = new HashMap<>();
    /** Distributeurs et droppers : joueur qui y a mis chaque sorte d'objet. */
    private final Map<Long, Map<org.bukkit.Material, UUID>> loaded = new HashMap<>();
    /** Cases ou un distributeur vient d'envoyer un objet, et a qui il est. */
    private final Map<Long, Pending> pending = new HashMap<>();
    private Pending lastEgg;
    private int startCount;
    private int elapsed;
    private int itemTimer;

    public PiliersInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                           boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        this.piliers = KGPiliersFortune.get();
    }

    private int setting(String key, int fallback) {
        return minigame().getInt(key, fallback);
    }

    private boolean playing() {
        return phase == Phase.COUNTDOWN || phase == Phase.RUNNING;
    }

    private List<Pos> pillars() {
        return arena().list("pillars");
    }

    private int duration() {
        return Math.max(60, setting("duration-seconds", 600));
    }

    // ------------------------------------------------------------------ admission

    @Override
    protected Component canAdmit(Player player) {
        if (!isPublic() && members.size() >= maxParticipants()) {
            return t("join.full", "<red>La partie est complète.");
        }
        return null;
    }

    @Override
    protected void onArrived(Player player) {
        if ((phase == Phase.WAITING || phase == Phase.STARTING) && greeted.add(player.getUniqueId())) {
            player.sendMessage(plugin.prefix().append(t("pf.welcome",
                    "<gray>Les piliers de la Fortune : un objet au hasard toutes les <white><s></white> s. Faites tomber les autres, ne tombez pas !",
                    "s", Math.max(1, setting("item-interval-seconds", 5)))));
        }
    }

    // ------------------------------------------------------------------ demarrage

    @Override
    protected int minParticipants() {
        return Math.max(2, Math.min(maxParticipants(), setting("min-players", 4)));
    }

    @Override
    protected int maxParticipants() {
        return Math.max(2, Math.min(pillars().size(), Math.min(8, setting("max-players", 8))));
    }

    @Override
    protected int gatherSeconds() {
        return setting("public-gather-seconds", 20);
    }

    @Override
    protected List<UUID> pickParticipants(List<UUID> queue) {
        return new ArrayList<>(queue.subList(0, Math.min(queue.size(), maxParticipants())));
    }

    @Override
    protected Component validatePrivateStart() {
        if (pillars().size() < 2) {
            return t("pf.no-pillars", "<red>Cette arène n'a pas assez de piliers : prévenez un modérateur.");
        }
        int online = onlineMembers().size();
        if (online < minParticipants()) {
            return t("pf.need-players", "<red>Il faut au moins <white><n></white> joueurs.", "n", minParticipants());
        }
        if (online > pillars().size()) {
            return t("pf.too-many", "<red>Trop de joueurs : <white><n></white> piliers dans cette arène.", "n", pillars().size());
        }
        return null;
    }

    private void resetState() {
        alive.clear();
        eliminated.clear();
        names.clear();
        pillarOf.clear();
        minutes.clear();
        kills.clear();
        lastHit.clear();
        owners.clear();
        entityOwners.clear();
        burnBy.clear();
        loaded.clear();
        pending.clear();
        lastEgg = null;
        startCount = 0;
        elapsed = 0;
        itemTimer = 0;
    }

    @Override
    protected void beginMatch() {
        resetState();
        List<Player> players = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        List<Pos> spots = new ArrayList<>(pillars());
        if (players.size() < 2 || players.size() > spots.size()) {
            broadcast(t("pf.aborted", "<yellow>Partie annulée : nombre de joueurs ou de piliers insuffisant."));
            endMatch();
            return;
        }
        Collections.shuffle(spots);
        phase = Phase.COUNTDOWN;
        secondsLeft = Math.max(1, setting("countdown-seconds", 5));
        startCount = players.size();
        allowFireSpread();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            UUID uuid = player.getUniqueId();
            Location spot = loc(spots.get(i));
            alive.add(uuid);
            names.put(uuid, player.getName());
            pillarOf.put(uuid, spot);
            minutes.put(uuid, 0);
            kills.put(uuid, 0);
            // Vie, faim et saturation pleines, experience a 0, inventaire vide (resetPlayer), mode survie.
            plugin.hub().resetPlayer(player, GameMode.SURVIVAL);
            player.teleport(spot);
        }
        showCountdown();
        broadcast(t("pf.start", "<gold>Les piliers de la Fortune : <white><n></white> joueurs. Dernier en vie gagne !", "n", startCount));
    }

    /**
     * KalGames regle la propagation du feu a 0 dans le monde des parties (le feu ne s'y propage jamais) : elle est remise
     * a sa valeur normale. Sans effet sur les autres jeux : KalGames y annule tout depart et toute propagation de feu,
     * seul ce plugin les autorise, dans ses propres parties (voir KGPiliersFortune).
     */
    private void allowFireSpread() {
        try {
            Integer normal = world.getGameRuleDefault(org.bukkit.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
            if (normal != null && !normal.equals(world.getGameRuleValue(org.bukkit.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER))) {
                world.setGameRule(org.bukkit.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, normal);
            }
        } catch (RuntimeException e) {
            piliers.getLogger().warning("Propagation du feu non rétablie : " + e.getMessage());
        }
    }

    /** Chiffre du decompte au milieu de l'ecran. */
    private void showCountdown() {
        title(members, t("pf.countdown", "<gold><bold><s>", "s", secondsLeft),
                t("pf.countdown-sub", "<gray>Préparez-vous…"), 0, 25, 0);
    }

    // ------------------------------------------------------------------ deroulement

    @Override
    protected void matchSecond() {
        switch (phase) {
            case COUNTDOWN -> {
                secondsLeft--;
                if (secondsLeft > 0) {
                    showCountdown();
                    return;
                }
                phase = Phase.RUNNING;
                secondsLeft = duration();
                elapsed = 0;
                itemTimer = Math.max(1, setting("item-interval-seconds", 5));
                title(members, t("pf.go", "<green><bold>C'est parti !"), Component.empty(), 0, 20, 10);
            }
            case RUNNING -> {
                elapsed++;
                secondsLeft--;
                if (--itemTimer <= 0) {
                    itemTimer = Math.max(1, setting("item-interval-seconds", 5));
                    giveItems();
                }
                if (elapsed % 60 == 0) {
                    for (UUID uuid : alive) {
                        minutes.merge(uuid, 1, Integer::sum);
                    }
                }
                if (secondsLeft <= 0) {
                    finish();
                }
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

    /** Un objet different, tire au hasard, a chaque joueur en vie ; inventaire plein : l'objet tombe a ses pieds. */
    private void giveItems() {
        for (UUID uuid : alive) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            ItemStack item = piliers.items().random(world);
            for (ItemStack rest : player.getInventory().addItem(item).values()) {
                world.dropItemNaturally(player.getLocation(), rest);
            }
        }
    }

    /** Chute : verifiee a chaque tick de l'instance (le moteur n'appelle handleVoid que sous le bas de l'arene). */
    @Override
    protected void matchTick() {
        if (phase != Phase.RUNNING) {
            return;
        }
        int voidY = setting("void-y", -64);
        for (UUID uuid : new ArrayList<>(alive)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || player.getWorld() != world) {
                continue;
            }
            if (player.getLocation().getY() < voidY) {
                eliminate(uuid, null);
                toSpectator(player);
            } else {
                checkWater(player);
            }
        }
    }

    // ------------------------------------------------------------------ eliminations

    /**
     * Coup recu d'un autre joueur, directement ou par ce qu'il a pose (explosif, feu, lave, eau) : ecouteur de
     * KGPiliersFortune et chute dans l'eau (matchTick).
     */
    void noteHit(Player victim, UUID attacker) {
        noteHit(victim, attacker, false);
    }

    private void noteHit(Player victim, UUID attacker, boolean water) {
        if (attacker != null && phase == Phase.RUNNING && !victim.getUniqueId().equals(attacker)
                && alive.contains(victim.getUniqueId()) && participants.contains(attacker)) {
            lastHit.put(victim.getUniqueId(), new Hit(attacker, System.currentTimeMillis(), water));
        }
    }

    // ------------------------------------------------------------------ poseurs (explosifs, feu, lave, eau)

    boolean running() {
        return phase == Phase.RUNNING;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static long key(Block block) {
        return key(block.getX(), block.getY(), block.getZ());
    }

    /** Joueur qui a pose le bloc (ou la source dont il provient : lave et eau qui coulent, feu qui se propage). */
    UUID owner(Block block) {
        Source source = block == null ? null : owners.get(key(block));
        return source == null ? null : source.owner();
    }

    /** Note le poseur d'un bloc, pose maintenant (null : bloc sans poseur connu, l'ancien est oublie). */
    void owner(Block block, UUID uuid) {
        if (uuid == null) {
            owners.remove(key(block));
        } else if (inBounds(block.getX(), block.getY(), block.getZ())) {
            owners.put(key(block), new Source(uuid, System.currentTimeMillis()));
        }
    }

    /** La lave et l'eau qui coulent gardent le poseur et l'heure de pose de leur source. */
    void copyOwner(Block from, Block to) {
        Source source = owners.get(key(from));
        if (source == null) {
            owners.remove(key(to));
        } else if (inBounds(to.getX(), to.getY(), to.getZ())) {
            owners.put(key(to), source);
        }
    }

    /** Joueurs en vie connectes. */
    List<Player> alivePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : alive) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    boolean isAlive(Player player) {
        return alive.contains(player.getUniqueId());
    }

    // Distributeurs et droppers : qui y a mis quel objet (demande de Maxster33, 06/10/2026).

    /** Le joueur a mis cet objet dans le distributeur ou le dropper. */
    void noteLoaded(Block container, org.bukkit.Material material, UUID uuid) {
        if (uuid != null && inBounds(container.getX(), container.getY(), container.getZ())) {
            loaded.computeIfAbsent(key(container), k -> new HashMap<>()).put(material, uuid);
        }
    }

    /** Joueur qui a mis cet objet dans le distributeur ou le dropper (le dernier, s'ils sont plusieurs). */
    UUID loader(Block container, org.bukkit.Material material) {
        Map<org.bukkit.Material, UUID> byItem = loaded.get(key(container));
        return byItem == null ? null : byItem.get(material);
    }

    /** Un distributeur vient d'envoyer un objet de ce joueur vers cette case : l'entite qui y apparait est a lui. */
    void notePending(Block block, UUID uuid) {
        pending.put(key(block), new Pending(uuid, Bukkit.getCurrentTick()));
    }

    /** Joueur d'un envoi de distributeur vers cette case, pendant le tick de l'envoi et le suivant. */
    UUID pending(Block block) {
        Pending entry = pending.get(key(block));
        return entry != null && Bukkit.getCurrentTick() - entry.tick() <= 1 ? entry.owner() : null;
    }

    /** Le joueur vient d'utiliser un oeuf d'apparition. */
    void noteEgg(Player player) {
        lastEgg = new Pending(player.getUniqueId(), Bukkit.getCurrentTick());
    }

    /** Joueur qui vient d'utiliser un oeuf, a 8 blocs au plus de la creature apparue (ce tick ou le suivant). */
    UUID eggUser(Location spawned) {
        if (lastEgg == null || Bukkit.getCurrentTick() - lastEgg.tick() > 1) {
            return null;
        }
        Player player = Bukkit.getPlayer(lastEgg.owner());
        return player != null && player.getWorld() == spawned.getWorld()
                && player.getLocation().distanceSquared(spawned) <= 64 ? lastEgg.owner() : null;
    }

    UUID entityOwner(Entity entity) {
        return entityOwners.get(entity.getUniqueId());
    }

    void entityOwner(Entity entity, UUID uuid) {
        if (uuid != null) {
            entityOwners.put(entity.getUniqueId(), uuid);
        }
    }

    /** Brulure par un feu ou une lave : le poseur reste credite tant que le joueur brule (degats « en feu »). */
    void noteBurn(Player victim, UUID owner) {
        if (owner == null) {
            burnBy.remove(victim.getUniqueId());
            return;
        }
        burnBy.put(victim.getUniqueId(), owner);
        noteHit(victim, owner);
    }

    void noteFireTick(Player victim) {
        noteHit(victim, burnBy.get(victim.getUniqueId()));
    }

    /**
     * Le joueur est dans de l'eau posee par un autre joueur : le poseur est credite si la chute suit (demandes de
     * Maxster33, 06/10/2026) :
     * <ul>
     *   <li>seulement dans les 10 s qui suivent la pose de la source (`water-credit-seconds`) : plus tard, l'eau ne
     *       compte plus pour personne ;</li>
     *   <li>un vrai coup recu ensuite (fleche, explosion...) l'emporte : l'eau ne le remplace pas tant qu'il compte
     *       (`kill-credit-seconds`).</li>
     * </ul>
     */
    private void checkWater(Player player) {
        long now = System.currentTimeMillis();
        long sourceWindow = Math.max(0, setting("water-credit-seconds", 10)) * 1000L;
        Location at = player.getLocation();
        for (Block block : new Block[]{at.getBlock(), at.clone().add(0, 1, 0).getBlock()}) {
            if (block.getType() != org.bukkit.Material.WATER) {
                continue;
            }
            Source source = owners.get(key(block));
            if (source == null || now - source.placedAt() > sourceWindow) {
                continue;
            }
            Hit current = lastHit.get(player.getUniqueId());
            long hitWindow = Math.max(1, setting("kill-credit-seconds", 10)) * 1000L;
            if (current != null && !current.water() && now - current.at() <= hitWindow) {
                return; // un coup plus recent que l'eau compte deja
            }
            noteHit(player, source.owner(), true);
            return;
        }
    }

    /** Joueur credite de l'elimination : le tueur s'il y en a un, sinon le dernier joueur qui l'a frappe dans le delai. */
    private UUID creditFor(UUID victim, Player killer) {
        if (killer != null && !killer.getUniqueId().equals(victim) && participants.contains(killer.getUniqueId())) {
            return killer.getUniqueId();
        }
        Hit hit = lastHit.get(victim);
        long window = Math.max(1, setting("kill-credit-seconds", 10)) * 1000L;
        if (hit != null && System.currentTimeMillis() - hit.at() <= window) {
            return hit.attacker();
        }
        return null;
    }

    private void eliminate(UUID victim, Player killer) {
        if (phase != Phase.RUNNING || !alive.remove(victim)) {
            return;
        }
        eliminated.add(victim);
        UUID credited = creditFor(victim, killer);
        String name = names.getOrDefault(victim, "?");
        if (credited != null) {
            kills.merge(credited, 1, Integer::sum);
            broadcast(t("pf.eliminated-by", "<gray><victim> <red>a été éliminé par <white><killer></white>. <gray>(<n> en vie)",
                    "victim", name, "killer", names.getOrDefault(credited, "?"), "n", alive.size()));
        } else {
            broadcast(t("pf.eliminated", "<gray><victim> <red>est éliminé. <gray>(<n> en vie)", "victim", name, "n", alive.size()));
        }
        if (alive.size() <= 1) {
            finish();
        }
    }

    /** L'elimine regarde la suite en mode spectateur, au-dessus de son pilier. */
    private void toSpectator(Player player) {
        becomeMatchSpectator(player);
        Location spot = pillarOf.get(player.getUniqueId());
        player.teleport(spot != null ? spot : stands());
    }

    @Override
    public void handleDeath(Player victim, Player killer) {
        eliminate(victim.getUniqueId(), killer);
    }

    /** Apres une mort : mode spectateur si la partie continue, sinon zone d'attente. */
    @Override
    public void onRespawned(Player player) {
        if (matchInProgress() && participants.contains(player.getUniqueId())) {
            toSpectator(player);
        } else {
            arriveInStands(player);
        }
    }

    @Override
    public void handleVoid(Player player) {
        UUID uuid = player.getUniqueId();
        if (isMatchSpectator(uuid)) {
            return; // deja spectateur (vol libre)
        }
        if (alive.contains(uuid) && playing()) {
            // Encore au-dessus de la couche d'elimination : la chute continue (voir matchTick).
            return;
        }
        arriveInStands(player);
    }

    /** Un joueur qui quitte ou se deconnecte en cours de partie est elimine (ses points jusque-la restent). */
    @Override
    protected void onMemberLeft(UUID uuid, boolean wasParticipant, boolean disconnected) {
        greeted.remove(uuid);
        if (!alive.contains(uuid)) {
            return;
        }
        if (phase == Phase.COUNTDOWN) {
            alive.remove(uuid);
            eliminated.add(uuid);
            if (alive.size() <= 1) {
                finish();
            }
            return;
        }
        eliminate(uuid, null);
    }

    // ------------------------------------------------------------------ fin et points

    private void finish() {
        if (!playing()) {
            return;
        }
        phase = Phase.ENDING;
        secondsLeft = Math.max(1, setting("end-delay-seconds", 8));
        UUID winner = alive.size() == 1 ? alive.iterator().next() : null;
        if (winner != null) {
            Component headline = t("pf.win", "<green><bold><name> gagne !", "name", names.getOrDefault(winner, "?"));
            title(members, headline, t("pf.win-sub", "<gray>Dernier joueur en vie"), 5, 70, 15);
            broadcast(headline);
        } else if (alive.size() > 1) {
            title(members, t("pf.draw", "<yellow><bold>Égalité !"),
                    t("pf.draw-sub", "<gray><n> joueurs encore en vie", "n", alive.size()), 5, 70, 15);
            broadcast(t("pf.draw-chat", "<yellow>Temps écoulé : égalité entre les <white><n></white> joueurs encore en vie.", "n", alive.size()));
        } else {
            broadcast(t("pf.nobody", "<yellow>Plus personne en vie : pas de gagnant."));
        }
        results(winner);
    }

    /**
     * Points de base (minutes en vie + eliminations), puis multiplicateur de fin de partie : x(rang d'elimination) pour
     * les elimines, x3 pour le gagnant seul, x1 pour les survivants a egalite. Credites au classement du jeu.
     */
    private void results(UUID winner) {
        int perMinute = setting("points-minute", 2);
        int perKill = setting("points-kill", 5);
        Map<UUID, Integer> multiplier = new LinkedHashMap<>();
        for (int i = 0; i < eliminated.size(); i++) {
            multiplier.put(eliminated.get(i), i + 1);
        }
        for (UUID uuid : alive) {
            multiplier.put(uuid, uuid.equals(winner) ? Math.max(1, setting("winner-multiplier", 3)) : 1);
        }
        Map<UUID, Double> base = new HashMap<>();
        Map<UUID, Double> total = new HashMap<>();
        for (UUID uuid : multiplier.keySet()) {
            double points = minutes.getOrDefault(uuid, 0) * perMinute + kills.getOrDefault(uuid, 0) * perKill;
            base.put(uuid, points);
            total.put(uuid, points * multiplier.get(uuid));
        }
        List<UUID> order = new ArrayList<>(multiplier.keySet());
        // Classement : points finaux, puis le dernier elimine (ou le survivant) devant.
        Collections.reverse(order);
        order.sort((a, b) -> Double.compare(total.get(b), total.get(a)));
        broadcast(t("pf.results", "<gold><bold>Classement aux points"));
        List<Map<String, Object>> logged = new ArrayList<>();
        int rank = 1;
        for (UUID uuid : order) {
            String name = names.getOrDefault(uuid, "?");
            broadcast(t("pf.results-line",
                    "<gray><rank>. <white><name></white> <dark_gray>- <gold><total> pts <gray>(<base> x<mult>)",
                    "rank", rank, "name", name, "total", StatsService.formatPoints(total.get(uuid)),
                    "base", StatsService.formatPoints(base.get(uuid)), "mult", multiplier.get(uuid)));
            boolean counted = credit(uuid, name, total.get(uuid));
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("player", uuid.toString());
            one.put("name", name);
            one.put("rank", rank);
            one.put("minutes", minutes.getOrDefault(uuid, 0));
            one.put("kills", kills.getOrDefault(uuid, 0));
            one.put("eliminated", eliminated.indexOf(uuid) + 1);
            one.put("base", base.get(uuid));
            one.put("multiplier", multiplier.get(uuid));
            one.put("credited", total.get(uuid));
            one.put("counted", counted);
            logged.add(one);
            rank++;
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("players", startCount);
        fields.put("winner", winner == null ? null : winner.toString());
        fields.put("survivors", alive.size());
        fields.put("seconds", elapsed);
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
            player.sendMessage(plugin.prefix().append(t("pf.credited", "<gold>+<points> point(s) au classement",
                    "points", StatsService.formatPoints(value))));
        }
        return true;
    }

    @Override
    protected void onMatchReset() {
        resetState();
    }

    @Override
    protected void onClose() {
        resetState();
    }

    // ------------------------------------------------------------------ regles

    private boolean fighting(Player player) {
        return phase == Phase.RUNNING && alive.contains(player.getUniqueId());
    }

    /** Un joueur en vie peut frapper les creatures de sa partie (oeufs d'apparition). */
    boolean canHitMob(Player attacker, Entity entity) {
        return fighting(attacker) && contains(entity.getLocation());
    }

    /** Immobile sur son pilier pendant le decompte (il peut tourner la tete). */
    @Override
    public boolean frozen(Player player) {
        return phase == Phase.COUNTDOWN && alive.contains(player.getUniqueId());
    }

    @Override
    public boolean canBuild(Player player) {
        return fighting(player);
    }

    @Override
    public boolean canInteract(Player player) {
        return fighting(player);
    }

    @Override
    public boolean canBreak(Player player, Block block) {
        return fighting(player) && inBounds(block.getX(), block.getY(), block.getZ());
    }

    @Override
    public boolean takesDamage(Player player) {
        return fighting(player);
    }

    @Override
    public boolean canFight(Player attacker, Player victim) {
        return fighting(attacker) && fighting(victim);
    }

    @Override
    public boolean inventoryLocked(Player player) {
        return !fighting(player);
    }

    // ------------------------------------------------------------------ affichage

    @Override
    public List<Component> menuInfo(Player player) {
        return List.of(t("pf.info", "<gray>De <white><min></white> à <white><max></white> joueurs, <white><m></white> min au plus",
                "min", minParticipants(), "max", maxParticipants(), "m", duration() / 60));
    }

    /** Barre du bas : temps restant et joueurs en vie. */
    @Override
    protected Component statusFor(Player player) {
        if (matchInProgress() && startCount > 0) {
            int remaining = phase == Phase.COUNTDOWN ? duration() : phase == Phase.RUNNING ? Math.max(0, secondsLeft) : 0;
            return t("pf.status", "<gold>Temps restant : <white><time></white> <dark_gray>| <gray>Joueurs en vie : <white><alive>/<total>",
                    "time", String.format("%d:%02d", remaining / 60, remaining % 60), "alive", alive.size(), "total", startCount);
        }
        if (isPublic()) {
            return queueStatus(player);
        }
        if (phase == Phase.WAITING) {
            return t("pf.status-lobby", "<gold>Partie privée <white><code></white> <dark_gray>- <gray>en attente de l'hôte (<n>/<max>)",
                    "code", code, "n", members.size(), "max", maxParticipants());
        }
        return null;
    }
}
