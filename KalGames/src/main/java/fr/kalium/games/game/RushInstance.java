package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.model.RushLayout;
import fr.kalium.games.model.RushLayout.Shop;
import fr.kalium.games.model.RushLayout.Team;
import fr.kalium.games.world.Template;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Moteur du Rush (1.11.0, etape 2 - cahier des charges de l'utilisateur, voir JOURNAL.md) : chaque
 * equipe defend un lit ; ressources, ponts, equipement, destruction des lits adverses ; la derniere
 * equipe en vie gagne.
 *
 * Salle d'attente : chaque joueur choisit son equipe (bleue, rouge, jaune, verte - selon l'arene) ;
 * une equipe pleine ne peut plus etre rejointe. Partie publique : lancement automatique des que le
 * minimum est atteint ; partie privee : lancee par l'hote. Les joueurs sans equipe au lancement sont
 * places dans les equipes les moins remplies.
 *
 * Lits : un lit est pose dans chaque base au lancement. N'importe qui peut casser n'importe quel lit
 * (choix de l'utilisateur). Chaque joueur a un point de reapparition lie a UN lit precis (celui de sa
 * base au depart) ; un clic droit sur n'importe quel lit le deplace sur ce lit. Un lit casse, ramasse
 * ou replace = un lit different : le point de reapparition qui y etait lie est perdu. Tous les lits
 * s'autodetruisent apres bed-destroy-minutes (les lits ne peuvent plus etre poses ensuite).
 *
 * Mort (sans ecran de mort : les degats mortels sont interceptes, voir RushListener) : tout
 * l'inventaire tombe au sol (choix de l'utilisateur), le joueur passe spectateur (vol) ; si son point
 * de reapparition est toujours sur un lit, il revient apres respawn-seconds (compte a rebours
 * visible), sinon il est elimine. Deconnexion ou depart (/hub, /lobby) = elimine.
 */
public final class RushInstance extends GameInstance {

    /** Etiquette des PNJ marchands (valeur = cle du Shop). */
    public static NamespacedKey shopKey(KalGames plugin) {
        return new NamespacedKey(plugin, "rush_shop");
    }

    /** Un lit connu de la partie (ses deux moities) et le proprietaire s'il s'agit d'un lit de base. */
    private record BedInfo(int id, long foot, long head, Location spawn, Team base) {
    }

    private final List<Team> teams = new ArrayList<>();
    private final int teamSize;

    private final Map<UUID, Team> teamOf = new HashMap<>();
    private final Set<UUID> greeted = new HashSet<>();

    private final Set<UUID> alive = new HashSet<>();
    private final Map<UUID, Integer> respawnIn = new LinkedHashMap<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private final Map<UUID, Location> deathSpots = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, Long> lastAttackAt = new HashMap<>();

    private final Map<Integer, BedInfo> beds = new HashMap<>();
    private final Map<Long, Integer> bedAt = new HashMap<>();
    private final Map<Team, Integer> baseBed = new EnumMap<>(Team.class);
    private final Map<UUID, Integer> spawnBed = new HashMap<>();
    private int nextBedId = 1;
    private boolean bedsDestroyed;

    private final Set<Team> teamsAtStart = EnumSet.noneOf(Team.class);
    private final Set<Team> teamsOut = EnumSet.noneOf(Team.class);
    private int elapsed;
    private int bronzeTicks;
    private int silverTicks;
    private int goldTicks;
    private boolean finished;

    public RushInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                        boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        int count = RushLayout.teamCount(arena);
        for (Team team : Team.values()) {
            if (teams.size() < count) {
                teams.add(team);
            }
        }
        this.teamSize = Math.max(1, Math.min(4, minigame.getInt("team-size", 4)));
    }

    // ------------------------------------------------------------------ equipes

    public List<Team> teams() {
        return teams;
    }

    public int teamSize() {
        return teamSize;
    }

    public int capacity() {
        return teams.size() * teamSize;
    }

    public Team teamOf(UUID uuid) {
        return teamOf.get(uuid);
    }

    public List<UUID> membersOf(Team team) {
        List<UUID> list = new ArrayList<>();
        for (UUID uuid : members) {
            if (teamOf.get(uuid) == team) {
                list.add(uuid);
            }
        }
        return list;
    }

    /** Les equipes ne se choisissent que dans la salle d'attente. */
    public boolean teamsOpen() {
        return !finished && (phase == Phase.WAITING || phase == Phase.STARTING);
    }

    public Component chooseTeam(Player player, Team team) {
        UUID uuid = player.getUniqueId();
        if (!teamsOpen() || !members.contains(uuid)) {
            return t("rush.team-locked", "<red>Les équipes ne se choisissent que dans la salle d'attente.");
        }
        if (!teams.contains(team)) {
            return t("rush.team-invalid", "<red>Cette équipe n'existe pas sur cette arène.");
        }
        if (teamOf.get(uuid) == team) {
            return null;
        }
        if (membersOf(team).size() >= teamSize) {
            return t("rush.team-full", "<red>Cette équipe est complète.");
        }
        teamOf.put(uuid, team);
        broadcast(t("rush.team-joined", "<gray><name> a rejoint l'équipe <team>.", "name", player.getName(),
                "team", RushItems.teamName(team)));
        return null;
    }

    /** Repartit les joueurs sans equipe dans les equipes les moins remplies (resultat sans modifier l'etat). */
    private Map<UUID, Team> plannedTeams(List<UUID> players) {
        Map<UUID, Team> plan = new HashMap<>();
        Map<Team, Integer> counts = new EnumMap<>(Team.class);
        for (Team team : teams) {
            counts.put(team, 0);
        }
        for (UUID uuid : players) {
            Team team = teamOf.get(uuid);
            if (team != null && counts.containsKey(team)) {
                plan.put(uuid, team);
                counts.merge(team, 1, Integer::sum);
            }
        }
        for (UUID uuid : players) {
            if (plan.containsKey(uuid)) {
                continue;
            }
            Team best = null;
            for (Team team : teams) {
                int count = counts.get(team);
                if (count < teamSize && (best == null || count < counts.get(best))) {
                    best = team;
                }
            }
            if (best != null) {
                plan.put(uuid, best);
                counts.merge(best, 1, Integer::sum);
            }
        }
        return plan;
    }

    private static int distinctTeams(Map<UUID, Team> plan) {
        return new HashSet<>(plan.values()).size();
    }

    // ------------------------------------------------------------------ admission / salle d'attente

    @Override
    protected Component canAdmit(Player player) {
        if (finished || matchInProgress()) {
            return t("rush.started", "<red>Cette partie a déjà commencé.");
        }
        if (members.size() >= capacity()) {
            return t("join.full", "<red>La partie est complète.");
        }
        return null;
    }

    /** Parties publiques : peut encore accueillir un nouveau joueur (sinon une nouvelle partie est ouverte). */
    public boolean openForNewPlayers() {
        return !closing() && !finished && !matchInProgress() && members.size() < capacity();
    }

    @Override
    protected void onArrived(Player player) {
        UUID uuid = player.getUniqueId();
        if (!teamsOpen() || !greeted.add(uuid)) {
            return;
        }
        player.sendMessage(plugin.prefix().append(isPublic()
                ? t("rush.welcome-public", "<gray>Choisissez votre équipe (menu de la partie). La partie démarre dès qu'il y a assez de joueurs.")
                : t("rush.welcome-private", "<gray>Partie privée <white><code></white>. Choisissez votre équipe ; l'hôte lance la partie.", "code", code)));
        plugin.later(10L, () -> {
            if (player.isOnline() && members.contains(uuid) && teamsOpen() && teamOf.get(uuid) == null) {
                plugin.menus().openRushTeams(player, this);
            }
        });
    }

    @Override
    protected int minParticipants() {
        return Math.max(2, minigame().getInt("min-players", 2));
    }

    @Override
    protected int maxParticipants() {
        return capacity();
    }

    @Override
    protected int gatherSeconds() {
        return minigame().getInt("gather-seconds", 30);
    }

    @Override
    protected List<UUID> pickParticipants(List<UUID> queue) {
        List<UUID> players = new ArrayList<>(queue.subList(0, Math.min(queue.size(), capacity())));
        if (distinctTeams(plannedTeams(players)) < 2) {
            broadcast(t("rush.need-teams", "<yellow>Il faut au moins deux équipes différentes pour lancer la partie : changez d'équipe."));
            return List.of();
        }
        return players;
    }

    @Override
    protected Component validatePrivateStart() {
        List<UUID> online = new ArrayList<>();
        for (Player player : onlineMembers()) {
            online.add(player.getUniqueId());
        }
        if (distinctTeams(plannedTeams(online)) < 2) {
            return t("rush.need-teams-private", "<red>Il faut au moins deux équipes non vides.");
        }
        return null;
    }

    // ------------------------------------------------------------------ lancement

    @Override
    protected void beginMatch() {
        List<UUID> players = new ArrayList<>(participants);
        teamOf.putAll(plannedTeams(players));
        teamsAtStart.clear();
        for (UUID uuid : players) {
            Team team = teamOf.get(uuid);
            if (team != null) {
                teamsAtStart.add(team);
            }
        }
        elapsed = 0;
        bedsDestroyed = false;
        for (Team team : teamsAtStart) {
            placeBaseBed(team);
            spawnShops(team);
        }
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            Team team = teamOf.get(uuid);
            if (player == null || team == null) {
                continue;
            }
            player.getEnderChest().clear();
            plugin.hub().resetPlayer(player, GameMode.SURVIVAL);
            player.teleport(spawnOf(team));
            alive.add(uuid);
            Integer bed = baseBed.get(team);
            if (bed != null) {
                spawnBed.put(uuid, bed);
            }
        }
        phase = Phase.RUNNING;
        StringBuilder line = new StringBuilder();
        for (Team team : teamsAtStart) {
            line.append("<").append(team.key()).append(">").append(team.display())
                    .append("</").append(team.key()).append("> (").append(membersOf(team).size()).append(")  ");
        }
        broadcast(t("rush.start", "<gold>Rush ! <teams>", "teams", plugin.lang().parse(line.toString().trim())));
        title(participants, t("rush.start-title", "<gold><bold>Rush !"), t("rush.start-sub", "<gray>Défendez votre lit"), 5, 50, 10);
        int minutes = minigame().getInt("bed-destroy-minutes", 15);
        if (minutes > 0) {
            broadcast(t("rush.beds-timer", "<gray>Les lits s'autodétruiront après <white><m></white> minute(s) de partie.", "m", minutes));
        }
    }

    private Location spawnOf(Team team) {
        Pos pos = arena().point(RushLayout.spawnKey(team));
        return pos == null ? stands() : loc(pos);
    }

    // ------------------------------------------------------------------ lits

    private static long key(Block block) {
        return block.getBlockKey();
    }

    private static BlockFace facingFromYaw(float yaw) {
        int r = Math.round((((yaw % 360) + 360) % 360) / 90f) % 4;
        return switch (r) {
            case 0 -> BlockFace.SOUTH;
            case 1 -> BlockFace.WEST;
            case 2 -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private void placeBaseBed(Team team) {
        Pos pos = arena().point(RushLayout.bedKey(team));
        if (pos == null) {
            return;
        }
        Location location = loc(pos);
        Block foot = location.getBlock();
        BlockFace facing = facingFromYaw(pos.yaw());
        Block head = foot.getRelative(facing);
        track(foot);
        track(head);
        Material material = RushItems.bed(team);
        Bed footData = (Bed) material.createBlockData();
        footData.setFacing(facing);
        footData.setPart(Bed.Part.FOOT);
        Bed headData = (Bed) material.createBlockData();
        headData.setFacing(facing);
        headData.setPart(Bed.Part.HEAD);
        foot.setBlockData(footData, false);
        head.setBlockData(headData, false);
        BedInfo info = register(foot, head, team);
        baseBed.put(team, info.id());
    }

    private BedInfo register(Block foot, Block head, Team base) {
        int id = nextBedId++;
        Location spawn = foot.getLocation().add(0.5, 0.5625, 0.5);
        if (foot.getBlockData() instanceof Bed bed) {
            spawn.setDirection(new Vector(bed.getFacing().getModX(), 0, bed.getFacing().getModZ()));
        }
        BedInfo info = new BedInfo(id, key(foot), key(head), spawn, base);
        beds.put(id, info);
        bedAt.put(info.foot(), id);
        bedAt.put(info.head(), id);
        return info;
    }

    /** Id du lit a cette position (un lit inconnu - decor de la carte, lit pose - est enregistre au passage). */
    private Integer bedIdAt(Block block) {
        Integer id = bedAt.get(key(block));
        if (id != null) {
            return id;
        }
        if (!Tag.BEDS.isTagged(block.getType()) || !(block.getBlockData() instanceof Bed bed)) {
            return null;
        }
        Block foot = bed.getPart() == Bed.Part.FOOT ? block : block.getRelative(bed.getFacing().getOppositeFace());
        Block head = bed.getPart() == Bed.Part.FOOT ? block.getRelative(bed.getFacing()) : block;
        if (!Tag.BEDS.isTagged(foot.getType()) || !Tag.BEDS.isTagged(head.getType())) {
            return null;
        }
        return register(foot, head, null).id();
    }

    /** Un lit vient d'etre pose par un joueur (appele un tick apres la pose, voir RushListener). */
    public void bedPlaced(Block block) {
        if (phase == Phase.RUNNING && Tag.BEDS.isTagged(block.getType())) {
            bedIdAt(block);
        }
    }

    /** Pose d'un lit : refusee apres l'autodestruction des lits. */
    public Component bedPlacementRefusal() {
        if (bedsDestroyed) {
            return t("rush.beds-gone-place", "<red>Les lits se sont autodétruits : plus aucun lit ne peut être posé.");
        }
        return null;
    }

    /** Un lit a disparu (casse, explosion) : les points de reapparition qui y etaient lies sont perdus. */
    public void bedRemoved(Block block, Player by) {
        Integer id = bedAt.get(key(block));
        if (id == null) {
            return;
        }
        BedInfo info = beds.remove(id);
        if (info == null) {
            return;
        }
        bedAt.remove(info.foot());
        bedAt.remove(info.head());
        if (info.base() != null && phase == Phase.RUNNING) {
            Team team = info.base();
            Component name = RushItems.teamName(team);
            if (by != null) {
                broadcast(t("rush.bed-broken-by", "<yellow>Le lit de l'équipe <team> a été détruit par <white><name></white> !",
                        "team", name, "name", by.getName()));
            } else {
                broadcast(t("rush.bed-broken", "<yellow>Le lit de l'équipe <team> a été détruit !", "team", name));
            }
            title(new HashSet<>(membersOf(team)), t("rush.bed-lost-title", "<red><bold>Lit détruit !"),
                    t("rush.bed-lost-sub", "<gray>Vous ne réapparaîtrez plus sur ce lit"), 5, 50, 10);
            for (Player player : onlineMembers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.4f);
            }
        }
    }

    /** Clic droit sur un lit : le point de reapparition du joueur passe sur ce lit. */
    public void setSpawnOnBed(Player player, Block block) {
        if (phase != Phase.RUNNING || !alive.contains(player.getUniqueId())) {
            return;
        }
        Integer id = bedIdAt(block);
        if (id == null) {
            return;
        }
        spawnBed.put(player.getUniqueId(), id);
        player.sendMessage(plugin.prefix().append(t("rush.spawn-set", "<green>Point de réapparition défini sur ce lit.")));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.6f);
    }

    public boolean isBed(Block block) {
        return Tag.BEDS.isTagged(block.getType());
    }

    private boolean hasValidSpawn(UUID uuid) {
        Integer id = spawnBed.get(uuid);
        return id != null && beds.containsKey(id);
    }

    private void destroyAllBeds() {
        for (BedInfo info : new ArrayList<>(beds.values())) {
            for (long k : new long[]{info.foot(), info.head()}) {
                Block block = world.getBlockAtKey(k);
                if (Tag.BEDS.isTagged(block.getType())) {
                    track(block);
                    block.setType(Material.AIR, false);
                }
            }
        }
        beds.clear();
        bedAt.clear();
        bedsDestroyed = true;
        broadcast(t("rush.beds-destroyed", "<red><bold>Tous les lits se sont autodétruits !</bold></red> <gray>Plus aucune réapparition."));
        title(participants, t("rush.beds-destroyed-title", "<red><bold>Lits détruits !"), t("rush.beds-destroyed-sub", "<gray>Plus aucune réapparition"), 5, 60, 10);
        for (Player player : onlineMembers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1f);
        }
    }

    // ------------------------------------------------------------------ PNJ marchands

    private void spawnShops(Team team) {
        NamespacedKey key = shopKey(plugin);
        for (Shop shop : Shop.values()) {
            Pos pos = arena().point(RushLayout.npcKey(team, shop));
            if (pos == null) {
                continue;
            }
            world.spawn(loc(pos), Villager.class, villager -> {
                villager.setAI(false);
                villager.setGravity(false); // ne tombe pas si le bloc en dessous est casse
                villager.setInvulnerable(true);
                villager.setSilent(true);
                villager.setCollidable(false);
                villager.setPersistent(false);
                villager.setRemoveWhenFarAway(false);
                villager.setProfession(profession(shop));
                villager.customName(Component.text(shop.display(), NamedTextColor.GOLD));
                villager.setCustomNameVisible(true);
                villager.getPersistentDataContainer().set(key, PersistentDataType.STRING, shop.key());
            });
        }
    }

    private static Villager.Profession profession(Shop shop) {
        return switch (shop) {
            case TRADER -> Villager.Profession.CARTOGRAPHER;
            case TRADER2 -> Villager.Profession.BUTCHER;
            case RUSHER -> Villager.Profession.WEAPONSMITH;
            case ARCHER -> Villager.Profession.FLETCHER;
            case MACON -> Villager.Profession.MASON;
            case ARMURIER -> Villager.Profession.ARMORER;
            case SECRET -> Villager.Profession.CLERIC;
        };
    }

    /** Ouvre les echanges d'un PNJ (couleurs de l'equipe de l'acheteur). */
    public void openShop(Player player, String shopKey) {
        if (phase != Phase.RUNNING || !alive.contains(player.getUniqueId())) {
            return;
        }
        Shop shop = null;
        for (Shop candidate : Shop.values()) {
            if (candidate.key().equals(shopKey)) {
                shop = candidate;
            }
        }
        Team team = teamOf.get(player.getUniqueId());
        if (shop == null || team == null) {
            return;
        }
        Merchant merchant = Bukkit.createMerchant(Component.text(shop.display(), NamedTextColor.DARK_GRAY));
        merchant.setRecipes(RushItems.recipes(shop, team));
        player.openMerchant(merchant, true);
    }

    // ------------------------------------------------------------------ monnaies

    @Override
    protected void matchTick() {
        if (phase != Phase.RUNNING) {
            return;
        }
        bronzeTicks += TICK_INTERVAL;
        silverTicks += TICK_INTERVAL;
        goldTicks += TICK_INTERVAL;
        int bronze = Math.max(1, minigame().getInt("bronze-interval-ticks", 10));
        int silver = Math.max(1, minigame().getInt("silver-interval-ticks", 20));
        int gold = Math.max(1, minigame().getInt("gold-interval-ticks", 60));
        while (bronzeTicks >= bronze) {
            bronzeTicks -= bronze;
            generate(RushItems.bronze(1));
        }
        while (silverTicks >= silver) {
            silverTicks -= silver;
            generate(RushItems.silver(1));
        }
        while (goldTicks >= gold) {
            goldTicks -= gold;
            generate(RushItems.gold(1));
        }
    }

    private void generate(ItemStack stack) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Team team : teamsAtStart) {
            Pos a = arena().point(RushLayout.generatorKey(team, 1));
            Pos b = arena().point(RushLayout.generatorKey(team, 2));
            if (a == null || b == null) {
                continue;
            }
            double x = Math.min(a.x(), b.x()) + random.nextDouble() * Math.max(0.01, Math.abs(a.x() - b.x()));
            double z = Math.min(a.z(), b.z()) + random.nextDouble() * Math.max(0.01, Math.abs(a.z() - b.z()));
            double y = Math.max(a.y(), b.y()) + 0.2;
            Location location = new Location(world, x + offX, y, z + offZ);
            Item item = world.dropItem(location, stack.clone());
            item.setVelocity(new Vector());
        }
    }

    // ------------------------------------------------------------------ morts et reapparitions

    /** Degat recu d'un joueur (pour attribuer une chute ou une mort a son dernier agresseur). */
    public void recordAttack(Player victim, Player attacker) {
        if (attacker != null && !attacker.equals(victim)) {
            lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
            lastAttackAt.put(victim.getUniqueId(), System.currentTimeMillis());
        }
    }

    private Player recentAttacker(Player victim) {
        UUID uuid = lastAttacker.get(victim.getUniqueId());
        Long at = lastAttackAt.get(victim.getUniqueId());
        if (uuid == null || at == null || System.currentTimeMillis() - at > 10_000L) {
            return null;
        }
        return Bukkit.getPlayer(uuid);
    }

    public boolean isAlive(UUID uuid) {
        return alive.contains(uuid);
    }

    /** Mort interceptee (degat mortel, chute) : pas d'ecran de mort. */
    public void kill(Player victim, Player killer, boolean fell) {
        if (phase != Phase.RUNNING || !alive.contains(victim.getUniqueId())) {
            return;
        }
        Location where = victim.getLocation();
        processDeath(victim, killer, fell);
        Location target = fell ? spawnOf(teamOf.getOrDefault(victim.getUniqueId(), Team.BLUE)).add(0, 6, 0) : where;
        applyDeadState(victim, target);
    }

    /** Mort "vanilla" (cas rare, ex. /kill) : meme traitement, puis reapparition forcee au tick suivant. */
    @Override
    public void handleDeath(Player victim, Player killer) {
        if (phase != Phase.RUNNING || !alive.contains(victim.getUniqueId())) {
            return;
        }
        deathSpots.put(victim.getUniqueId(), victim.getLocation());
        processDeath(victim, killer, false);
        plugin.later(1L, () -> {
            if (victim.isOnline() && victim.isDead()) {
                victim.spigot().respawn();
            }
        });
    }

    /** Point de reapparition "vanilla" apres une mort non interceptee (voir ConnectionListener). */
    public Location deathSpot(Player player) {
        Location spot = deathSpots.get(player.getUniqueId());
        return spot != null ? spot : stands();
    }

    public void onVanillaRespawn(Player player) {
        UUID uuid = player.getUniqueId();
        Location spot = deathSpots.remove(uuid);
        if (!members.contains(uuid)) {
            return;
        }
        if (alive.contains(uuid)) {
            return; // deja revenu entre-temps
        }
        if (matchInProgress()) {
            applyDeadState(player, spot != null ? spot : stands());
        } else {
            arriveInStands(player);
        }
    }

    private void processDeath(Player victim, Player killer, boolean fell) {
        UUID uuid = victim.getUniqueId();
        alive.remove(uuid);
        if (killer == null || killer.equals(victim)) {
            killer = recentAttacker(victim);
        }
        // Tout l'inventaire tombe au sol (choix de l'utilisateur) - sauf chute dans le vide.
        if (!fell) {
            Location where = victim.getLocation();
            for (ItemStack stack : victim.getInventory().getContents()) {
                if (stack != null && !stack.getType().isAir()) {
                    world.dropItemNaturally(where, stack);
                }
            }
        }
        victim.getInventory().clear();
        lastAttacker.remove(uuid);
        lastAttackAt.remove(uuid);
        if (killer != null) {
            broadcast(t("rush.kill", "<gray><victim> <red>a été tué par <white><killer></white>.",
                    "victim", victim.getName(), "killer", killer.getName()));
        } else {
            broadcast(t("rush.death", "<gray><victim> <red>est mort.", "victim", victim.getName()));
        }
        if (hasValidSpawn(uuid)) {
            respawnIn.put(uuid, Math.max(0, minigame().getInt("respawn-seconds", 3)));
        } else {
            eliminate(victim, t("rush.no-bed", "<red>Votre point de réapparition n'est plus sur un lit : vous êtes éliminé."));
        }
        checkTeams();
    }

    private void applyDeadState(Player player, Location target) {
        plugin.hub().resetPlayer(player, GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        if (target != null) {
            player.teleport(target);
        }
        Integer seconds = respawnIn.get(player.getUniqueId());
        if (seconds != null) {
            showRespawnCountdown(player, seconds);
            if (seconds <= 0) {
                respawnIn.remove(player.getUniqueId());
                tryRespawn(player.getUniqueId());
            }
        }
    }

    private void showRespawnCountdown(Player player, int seconds) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                t("rush.dead-title", "<red><bold>Mort !"),
                t("rush.respawn-sub", "<gray>Réapparition dans <white><s></white> s", "s", seconds),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(1100), java.time.Duration.ZERO)));
    }

    private void tryRespawn(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !members.contains(uuid) || phase != Phase.RUNNING) {
            return;
        }
        if (!hasValidSpawn(uuid)) {
            eliminate(player, t("rush.bed-gone", "<red>Le lit de votre point de réapparition a disparu : vous êtes éliminé."));
            checkTeams();
            return;
        }
        BedInfo bed = beds.get(spawnBed.get(uuid));
        plugin.hub().resetPlayer(player, GameMode.SURVIVAL);
        player.teleport(bed.spawn());
        alive.add(uuid);
        player.showTitle(net.kyori.adventure.title.Title.title(t("rush.respawned-title", "<green><bold>De retour !"), Component.empty(),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(1200), java.time.Duration.ofMillis(300))));
    }

    private void eliminate(Player player, Component reason) {
        UUID uuid = player.getUniqueId();
        alive.remove(uuid);
        respawnIn.remove(uuid);
        eliminated.add(uuid);
        player.sendMessage(plugin.prefix().append(reason));
        player.showTitle(net.kyori.adventure.title.Title.title(t("rush.eliminated-title", "<red><bold>Éliminé"),
                t("rush.eliminated-sub", "<gray>Vous regardez la fin de la partie"),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(250), java.time.Duration.ofMillis(2500), java.time.Duration.ofMillis(500))));
        broadcast(t("rush.eliminated", "<gray><name> <red>est éliminé.", "name", player.getName()));
    }

    @Override
    public void handleVoid(Player player) {
        UUID uuid = player.getUniqueId();
        if (phase == Phase.RUNNING && alive.contains(uuid)) {
            kill(player, null, true);
            return;
        }
        if (!matchInProgress()) {
            arriveInStands(player);
        }
        // Mort en attente de reapparition / elimine : spectateur, il vole ou il veut.
    }

    /** Equipes encore en jeu (au moins un joueur vivant ou en attente de reapparition). */
    private Set<Team> teamsInGame() {
        Set<Team> set = EnumSet.noneOf(Team.class);
        for (UUID uuid : alive) {
            Team team = teamOf.get(uuid);
            if (team != null) {
                set.add(team);
            }
        }
        for (UUID uuid : respawnIn.keySet()) {
            Team team = teamOf.get(uuid);
            if (team != null) {
                set.add(team);
            }
        }
        return set;
    }

    private void checkTeams() {
        if (phase != Phase.RUNNING) {
            return;
        }
        Set<Team> inGame = teamsInGame();
        for (Team team : teamsAtStart) {
            if (!inGame.contains(team) && teamsOut.add(team)) {
                broadcast(t("rush.team-out", "<gold>L'équipe <team> est éliminée !", "team", RushItems.teamName(team)));
            }
        }
        if (inGame.size() <= 1) {
            finish(inGame.isEmpty() ? null : inGame.iterator().next());
        }
    }

    private void finish(Team winner) {
        finished = true;
        phase = Phase.ENDING;
        secondsLeft = Math.max(1, minigame().getInt("end-delay-seconds", 10));
        respawnIn.clear();
        if (winner == null) {
            broadcast(t("rush.draw", "<yellow>Partie terminée : plus aucune équipe en jeu."));
            title(new HashSet<>(members), t("rush.draw-title", "<yellow>Égalité"), Component.empty(), 5, 50, 10);
            return;
        }
        int points = minigame().getInt("points-win", 1);
        List<String> names = new ArrayList<>();
        for (UUID uuid : membersOf(winner)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !participants.contains(uuid)) {
                continue;
            }
            names.add(player.getName());
            plugin.scores().award(this, player, points);
        }
        broadcast(t("rush.win", "<green>Victoire de l'équipe <team> ! <gray>(<names>) <gold>+<points> point(s)</gold> chacun.",
                "team", RushItems.teamName(winner), "names", String.join(", ", names), "points", points));
        title(new HashSet<>(members), t("rush.win-title", "<green><bold>Victoire !"),
                t("rush.win-sub", "<white>Équipe <team>", "team", RushItems.teamName(winner)), 5, 70, 15);
    }

    // ------------------------------------------------------------------ boucle

    @Override
    protected void matchSecond() {
        if (phase == Phase.ENDING) {
            secondsLeft--;
            if (secondsLeft <= 0) {
                endMatch();
            }
            return;
        }
        if (phase != Phase.RUNNING) {
            return;
        }
        elapsed++;
        int bedSeconds = minigame().getInt("bed-destroy-minutes", 15) * 60;
        if (bedSeconds > 0 && !bedsDestroyed) {
            if (elapsed == bedSeconds - 60) {
                broadcast(t("rush.beds-warning", "<red>Les lits s'autodétruiront dans 1 minute !"));
            }
            if (elapsed >= bedSeconds) {
                destroyAllBeds();
            }
        }
        for (UUID uuid : new ArrayList<>(respawnIn.keySet())) {
            int left = respawnIn.get(uuid) - 1;
            Player player = Bukkit.getPlayer(uuid);
            if (left <= 0) {
                respawnIn.remove(uuid);
                tryRespawn(uuid);
            } else {
                respawnIn.put(uuid, left);
                if (player != null) {
                    showRespawnCountdown(player, left);
                }
            }
        }
    }

    // ------------------------------------------------------------------ departs

    @Override
    protected void onMemberLeft(UUID uuid, boolean wasParticipant, boolean disconnected) {
        greeted.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getEnderChest().clear();
        }
        if (!wasParticipant || !matchInProgress()) {
            teamOf.remove(uuid);
            return;
        }
        boolean wasAlive = alive.remove(uuid);
        if (wasAlive && player != null) {
            Location where = player.getLocation();
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && !stack.getType().isAir()) {
                    world.dropItemNaturally(where, stack);
                }
            }
            player.getInventory().clear();
        }
        boolean wasInGame = wasAlive || respawnIn.remove(uuid) != null;
        spawnBed.remove(uuid);
        eliminated.add(uuid);
        if (wasInGame && phase == Phase.RUNNING) {
            String name = player != null ? player.getName() : "Un joueur";
            broadcast(t("rush.left", "<gray><name> <red>a quitté la partie : éliminé.", "name", name));
            checkTeams();
        }
    }

    @Override
    protected void onMatchReset() {
        alive.clear();
        respawnIn.clear();
        spawnBed.clear();
        beds.clear();
        bedAt.clear();
        baseBed.clear();
        deathSpots.clear();
        lastAttacker.clear();
        lastAttackAt.clear();
    }

    @Override
    protected void onClose() {
        for (Player player : onlineMembers()) {
            player.getEnderChest().clear();
        }
    }

    // ------------------------------------------------------------------ regles

    @Override
    public boolean canBuild(Player player) {
        return phase == Phase.RUNNING && alive.contains(player.getUniqueId());
    }

    @Override
    public boolean canInteract(Player player) {
        return canBuild(player);
    }

    @Override
    public boolean canBreak(Player player, Block block) {
        // 1.12.1 : tout bloc de l'arene est cassable (demande explicite de l'utilisateur : "l'option protection
        // casse est inutile") - la carte est remise a l'identique du modele apres chaque partie (voir ArenaPool) -
        // SAUF les zones d'apparition des monnaies ("seule la zone d'apparition des monnaies doit etre protegee").
        return canBuild(player) && inBounds(block.getX(), block.getY(), block.getZ()) && !inCurrencyZone(block);
    }

    /** Les explosions (TNT) : meme regle que la casse a la main. */
    public boolean explosionMayBreak(Block block) {
        return inBounds(block.getX(), block.getY(), block.getZ()) && !inCurrencyZone(block);
    }

    /**
     * Bloc d'une zone d'apparition des monnaies (entre les deux coins d'une base, sol juste en dessous compris) :
     * protege contre la casse (1.12.1, demande explicite de l'utilisateur).
     */
    public boolean inCurrencyZone(Block block) {
        for (Team team : teams) {
            Pos a = arena().point(RushLayout.generatorKey(team, 1));
            Pos b = arena().point(RushLayout.generatorKey(team, 2));
            if (a == null || b == null) {
                continue;
            }
            int minX = (int) Math.floor(Math.min(a.x(), b.x()) + offX);
            int maxX = (int) Math.floor(Math.max(a.x(), b.x()) + offX);
            int minZ = (int) Math.floor(Math.min(a.z(), b.z()) + offZ);
            int maxZ = (int) Math.floor(Math.max(a.z(), b.z()) + offZ);
            int minY = (int) Math.floor(Math.min(a.y(), b.y())) - 1;
            int maxY = (int) Math.floor(Math.max(a.y(), b.y()));
            if (block.getX() >= minX && block.getX() <= maxX && block.getZ() >= minZ && block.getZ() <= maxZ
                    && block.getY() >= minY && block.getY() <= maxY) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean takesDamage(Player player) {
        return phase == Phase.RUNNING && alive.contains(player.getUniqueId());
    }

    @Override
    public boolean canFight(Player attacker, Player victim) {
        if (phase != Phase.RUNNING) {
            return false;
        }
        UUID a = attacker.getUniqueId();
        UUID v = victim.getUniqueId();
        return alive.contains(a) && alive.contains(v) && teamOf.get(a) != teamOf.get(v);
    }

    @Override
    public boolean inventoryLocked(Player player) {
        return !(phase == Phase.RUNNING && alive.contains(player.getUniqueId()));
    }

    // ------------------------------------------------------------------ affichage

    @Override
    protected Component statusFor(Player player) {
        UUID uuid = player.getUniqueId();
        if (teamsOpen()) {
            Team team = teamOf.get(uuid);
            Component teamPart = team == null
                    ? t("rush.status-no-team", "<red>Aucune équipe (menu de la partie)")
                    : t("rush.status-team", "<gray>Équipe <team>", "team", RushItems.teamName(team));
            String state;
            if (!isPublic()) {
                state = "partie privée " + code + " - en attente de l'hôte";
            } else if (phase == Phase.STARTING) {
                state = "début dans " + secondsLeft + " s";
            } else {
                state = "en attente de joueurs (minimum " + minParticipants() + ")";
            }
            return t("rush.status-lobby", "<team> <dark_gray>| <white><n>/<cap></white> <gray>joueurs <dark_gray>| <gray><state>",
                    "team", teamPart, "n", members.size(), "cap", capacity(), "state", state);
        }
        if (phase != Phase.RUNNING) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        for (Team team : teamsAtStart) {
            int in = 0;
            for (UUID member : alive) {
                if (teamOf.get(member) == team) {
                    in++;
                }
            }
            for (UUID member : respawnIn.keySet()) {
                if (teamOf.get(member) == team) {
                    in++;
                }
            }
            Integer bed = baseBed.get(team);
            boolean bedUp = bed != null && beds.containsKey(bed);
            String color = team.key();
            line.append("<").append(color).append(">").append(team.display()).append("</").append(color).append("> ")
                    .append(bedUp ? "<green>✔</green>" : "<red>✘</red>").append(" <white>").append(in).append("</white>   ");
        }
        int bedSeconds = minigame().getInt("bed-destroy-minutes", 15) * 60;
        if (bedsDestroyed) {
            line.append("<dark_gray>| <red>Lits détruits");
        } else if (bedSeconds > 0) {
            int left = Math.max(0, bedSeconds - elapsed);
            line.append(String.format("<dark_gray>| <gray>Lits : <white>%d:%02d", left / 60, left % 60));
        }
        return plugin.lang().parse(line.toString().trim());
    }
}
