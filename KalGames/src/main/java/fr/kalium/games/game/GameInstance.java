package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.world.Template;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Une partie : une copie de l'arene collee dans le monde des instances.
 * Cette classe gere ce qui est commun a tous les mini-jeux : joueurs, gradins, file d'attente des
 * parties publiques, salon des parties privees, suivi des blocs modifies et nettoyage.
 * Les moteurs (PvP, courses...) fournissent les regles du match.
 */
public abstract class GameInstance {

    public enum Phase { PREPARING, WAITING, STARTING, VOTE, COUNTDOWN, RUNNING, ENDING, CLOSED }

    /** Intervalle du tick de l'instance, en ticks serveur. */
    public static final int TICK_INTERVAL = 2;
    private static final int TICKS_PER_SECOND = 20;

    protected final KalGames plugin;
    private final String id;
    private final Minigame minigame;
    private final Arena arena;
    private final Template template;
    private final boolean publicGame;
    protected final Map<String, Object> options;
    private final int slot;
    protected final World world;
    protected final int slotX;
    protected final int slotZ;
    protected final int offX;
    protected final int offZ;
    protected final int minX;
    protected final int maxX;
    protected final int minY;
    protected final int maxY;
    protected final int minZ;
    protected final int maxZ;

    protected Phase phase = Phase.PREPARING;
    protected final Set<UUID> members = new LinkedHashSet<>();
    /** Parties publiques : ordre de passage. */
    protected final List<UUID> queue = new ArrayList<>();
    /** Joueurs du match en cours. */
    protected final Set<UUID> participants = new LinkedHashSet<>();
    /** Joueurs en mode spectateur : ne participent pas au match, libres de leurs mouvements (vol). */
    protected final Set<UUID> spectators = new LinkedHashSet<>();
    /**
     * Participants qui ont termine (arrivee, elimination...) alors que le match continue pour les autres :
     * mode spectateur (vol libre) jusqu'a la fin du match, comme les vrais spectateurs.
     */
    protected final Set<UUID> matchSpectators = new LinkedHashSet<>();
    protected UUID host;
    protected String code = "";
    protected boolean listed;
    protected int secondsLeft;

    private final Map<Long, BlockData> originals = new HashMap<>();
    protected final Set<Long> placed = new HashSet<>();
    private int tickAccumulator;
    private int emptySeconds;
    private boolean closing;

    // 1.26.0 - relance en salle d'attente (demande de LeKiwi06, 20/10/2026 : « au lieu d'un totem pour relancer dans le
    // lobby, laisse nous dans la salle d'attente du jeu avec un bouton quitter la partie, et un timer de 15 secondes qui
    // relance pour les joueurs qui n'ont pas quitté la partie »). Parties privées seulement.
    /** Secondes avant la relance automatique (0 = pas de compte à rebours). */
    private int relance;
    /** Compte à rebours arrêté (pas assez de joueurs, ou l'hôte change les réglages) : reprend dès que possible. */
    private boolean relanceEnAttente;
    /** L'hôte change les réglages : pas de reprise automatique tant qu'il n'a pas fini. */
    private boolean relanceSuspendue;
    private net.kyori.adventure.bossbar.BossBar barreRelance;
    /** Points de chaque participant au début de la manche (résumé de la manche). */
    private final Map<UUID, Double> pointsAvant = new HashMap<>();
    /** Résumé de la dernière manche : joueurs et points gagnés, du meilleur au moins bon. */
    private List<Resultat> resume = List.of();

    /** 1.26.0 : un joueur et ses points gagnés pendant la dernière manche. */
    public record Resultat(UUID uuid, String nom, double points) {
    }

    protected GameInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                           boolean publicGame, Map<String, Object> options, int slot) {
        this.plugin = plugin;
        this.id = id;
        this.minigame = minigame;
        this.arena = arena;
        this.template = template;
        this.publicGame = publicGame;
        this.options = options;
        this.slot = slot;
        this.world = plugin.worlds().world();
        this.slotX = plugin.worlds().slotX(slot);
        this.slotZ = plugin.worlds().slotZ(slot);
        this.offX = slotX - template.originX();
        this.offZ = slotZ - template.originZ();
        this.minX = slotX;
        this.maxX = slotX + template.sizeX() - 1;
        this.minY = template.originY();
        this.maxY = template.originY() + template.sizeY() - 1;
        this.minZ = slotZ;
        this.maxZ = slotZ + template.sizeZ() - 1;
    }

    // ------------------------------------------------------------------ acces

    public String id() {
        return id;
    }

    public Minigame minigame() {
        return minigame;
    }

    public Arena arena() {
        return arena;
    }

    public Template template() {
        return template;
    }

    public boolean isPublic() {
        return publicGame;
    }

    public Phase phase() {
        return phase;
    }

    public int slot() {
        return slot;
    }

    public int slotX() {
        return slotX;
    }

    public int slotZ() {
        return slotZ;
    }

    public World world() {
        return world;
    }

    public String code() {
        return code;
    }

    public void code(String value) {
        this.code = value;
    }

    public UUID host() {
        return host;
    }

    public void host(UUID value) {
        this.host = value;
    }

    public boolean listed() {
        return listed;
    }

    public void listed(boolean value) {
        this.listed = value;
    }

    public Set<UUID> members() {
        return members;
    }

    public List<UUID> queue() {
        return queue;
    }

    public Set<UUID> participants() {
        return participants;
    }

    public boolean isMember(UUID id) {
        return members.contains(id);
    }

    public boolean isParticipant(UUID id) {
        return participants.contains(id);
    }

    // ------------------------------------------------------------------ spectateurs

    public Set<UUID> spectators() {
        return spectators;
    }

    public boolean isSpectator(UUID id) {
        return spectators.contains(id);
    }

    /** Ajoute/retire un spectateur (suivi gere par InstanceManager, qui teleporte et change le mode de jeu). */
    void addSpectator(UUID uuid) {
        spectators.add(uuid);
    }

    void removeSpectator(UUID uuid) {
        spectators.remove(uuid);
    }

    public List<Player> onlineSpectators() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    /** Un participant qui a termine sa partie (voir {@link #becomeMatchSpectator}), en attendant la fin du match. */
    public boolean isMatchSpectator(UUID id) {
        return matchSpectators.contains(id);
    }

    /**
     * Un participant a termine (arrivee, elimination...) mais le match continue pour les autres : il bascule en
     * mode spectateur (vol libre), sans etre deplace : son point d'apparition reste l'endroit ou il a termine sa
     * partie, jusqu'a la fin du match (voir {@link #arriveInStands}, qui remet l'etat normal et retire ce statut).
     */
    protected void becomeMatchSpectator(Player player) {
        matchSpectators.add(player.getUniqueId());
        plugin.hub().applySpectatorState(player);
    }

    /** Ce type de partie compte-t-il pour les classements (hors cas particuliers de joueurs) ? */
    public boolean ranked() {
        return publicGame || plugin.getConfig().getBoolean("stats.count-private-games", true);
    }

    /**
     * Les points et temps de CE joueur comptent-ils pour les classements ? Non pour les operateurs, et en partie
     * privee seulement pour les premieres parties du jour (stats.private-daily-limit, 5 par defaut, par jeu).
     */
    public boolean ranked(Player player) {
        if (!ranked() || plugin.scores().excluded(player)) {
            return false;
        }
        // 1.19.0 : plus de limite de parties privees classees par jour (decision de LeKiwi06 du 24/09/2026, obsolete
        // avec les nouveaux baremes) : toute partie classee compte, publique ou privee, solo compris.
        return true;
    }

    /** Appele au lancement de chaque match : compte les parties privees du jour et previent les joueurs concernes. */
    /** 1.19.0 : identifiant unique du match en cours (journal de KG_ScoreBoards). */
    private String matchId = java.util.UUID.randomUUID().toString();

    public String matchId() {
        return matchId;
    }

    private void prepareRanking() {
        matchId = java.util.UUID.randomUUID().toString();
        // 1.26.0 : points de chacun au début de la manche (résumé de la manche) ; plus de compte à rebours.
        relance = 0;
        relanceEnAttente = false;
        relanceSuspendue = false;
        cacherBarre();
        pointsAvant.clear();
        for (UUID uuid : participants) {
            pointsAvant.put(uuid, pointsDe(uuid));
            Player participant = Bukkit.getPlayer(uuid);
            if (participant != null) {
                plugin.items().removeStandsItems(participant);
            }
        }
        boolean ranked = ranked();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (ranked && plugin.scores().excluded(player)) {
                player.sendMessage(plugin.prefix().append(t("rank.op-notice",
                        "<gray>Mode opérateur : vos points et temps <white>ne comptent pas</white> pour le classement.")));
                continue;
            }
        }
    }

    /** 1.26.0 : copie des réglages de la partie (« Changer les réglages » de l'hôte). */
    public Map<String, Object> optionsCopie() {
        return new HashMap<>(options);
    }

    /** 1.26.0 : nombre de joueurs maximum d'un match (menus : « Joueurs : 3/6 »). */

    public int capacite() {
        return maxParticipants();
    }

    // ------------------------------------------------------------------ 1.26.0 : relance en salle d'attente

    private boolean relanceActive() {
        return !publicGame && plugin.getConfig().getBoolean("relance.active", true);
    }

    private int secondesRelance() {
        return Math.max(5, plugin.getConfig().getInt("relance.secondes", 15));
    }

    /** Secondes avant la relance automatique, 0 si aucun compte à rebours (menus). */
    public int relance() {
        return phase == Phase.WAITING ? relance : 0;
    }

    /** La relance attend des joueurs (ou la fin des réglages de l'hôte). */
    public boolean relanceEnAttente() {
        return phase == Phase.WAITING && relanceEnAttente;
    }

    /** Résumé de la dernière manche (vide avant la première). */
    public List<Resultat> resume() {
        return resume;
    }

    /** Démarre (ou reprend) le compte à rebours de la relance. */
    public void lancerCompteARebours() {
        if (!relanceActive() || closing) {
            return;
        }
        relanceEnAttente = false;
        relanceSuspendue = false;
        relance = secondesRelance();
        afficherBarre();
    }

    /** L'hôte change les réglages : le compte à rebours s'arrête jusqu'à ce qu'il ait fini. */
    public void suspendreRelance() {
        if (!relanceActive() || phase != Phase.WAITING || (relance <= 0 && !relanceEnAttente)) {
            return;
        }
        relance = 0;
        relanceEnAttente = true;
        relanceSuspendue = true;
        cacherBarre();
        broadcast(t("relance.suspendue", "<yellow>L'hôte change les réglages : la relance attend."));
    }

    /** L'hôte a renoncé à changer les réglages : le compte à rebours reprend. */
    public void reprendreRelance() {
        if (relanceSuspendue && phase == Phase.WAITING) {
            lancerCompteARebours();
        }
    }

    private void afficherBarre() {
        if (relance <= 0) {
            cacherBarre();
            return;
        }
        Component titre = t("relance.barre", "<green>Nouvelle partie dans <white><s></white> s <dark_gray>- <gray>Quitter : objet rouge de la barre",
                "s", relance);
        float progression = Math.max(0f, Math.min(1f, relance / (float) secondesRelance()));
        if (barreRelance == null) {
            barreRelance = net.kyori.adventure.bossbar.BossBar.bossBar(titre, progression,
                    net.kyori.adventure.bossbar.BossBar.Color.GREEN, net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS);
        } else {
            barreRelance.name(titre);
            barreRelance.progress(progression);
        }
        for (Player player : onlineMembers()) {
            player.showBossBar(barreRelance);
        }
        for (Player player : onlineSpectators()) {
            player.showBossBar(barreRelance);
        }
    }

    private void cacherBarre() {
        if (barreRelance == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(barreRelance);
        }
    }

    /** Une seconde de la salle d'attente : compte à rebours, puis relance avec les joueurs restés. */
    private void secondeRelance() {
        if (relance <= 0) {
            return;
        }
        relance--;
        if (relance > 0) {
            afficherBarre();
            return;
        }
        cacherBarre();
        Component erreur = members.isEmpty() ? t("relance.vide", "<red>Personne dans la partie.") : validatePrivateStart();
        if (erreur != null) {
            relanceEnAttente = true;
            broadcast(t("relance.attente", "<yellow>Pas assez de joueurs pour relancer : la partie reprendra dès qu'un joueur la rejoindra.")
                    .append(Component.newline()).append(erreur));
            return;
        }
        relanceEnAttente = false;
        participants.clear();
        participants.addAll(members);
        prepareRanking();
        beginMatch();
    }

    /**
     * Fin d'une manche d'une partie privée : tout le monde reste dans la salle d'attente ; les spectateurs y entrent aussi
     * (« Passer spectateur » pour seulement regarder la suivante) ; résumé de la manche ; compte à rebours de la relance.
     */
    private void apresManche() {
        List<Resultat> resultats = new ArrayList<>();
        for (Map.Entry<UUID, Double> avant : pointsAvant.entrySet()) {
            double maintenant = pointsDe(avant.getKey());
            String nom = Bukkit.getOfflinePlayer(avant.getKey()).getName();
            resultats.add(new Resultat(avant.getKey(), nom == null ? "?" : nom, Math.max(0, maintenant - avant.getValue())));
        }
        resultats.sort((a, b) -> Double.compare(b.points(), a.points()));
        resume = List.copyOf(resultats);
        pointsAvant.clear();
        for (Player spectateur : onlineSpectators()) {
            Component refus = plugin.instances().spectatorToPlayer(spectateur, this);
            if (refus != null) {
                spectateur.sendMessage(plugin.prefix().append(refus));
            }
        }
        lancerCompteARebours();
        // Le menu de la partie s'ouvre : résumé, « Quitter la partie », « Passer spectateur »...
        for (Player player : onlineMembers()) {
            plugin.later(10L, () -> {
                if (player.isOnline() && plugin.instances().of(player) == this && phase == Phase.WAITING) {
                    plugin.menus().openCurrent(player);
                }
            });
        }
    }

    private double pointsDe(UUID uuid) {
        try {
            fr.kalium.scoreboards.data.StatsService.Row row = plugin.stats().playerRow(minigame.id(), uuid);
            return row == null ? 0 : row.points();
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    public boolean isHost(UUID id) {
        return id != null && id.equals(host);
    }

    public boolean matchInProgress() {
        return phase == Phase.VOTE || phase == Phase.COUNTDOWN || phase == Phase.RUNNING || phase == Phase.ENDING;
    }

    public Object option(String key) {
        return options.get(key);
    }

    public int optionInt(String key, int fallback) {
        Object value = options.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public boolean optionBool(String key, boolean fallback) {
        Object value = options.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    // ------------------------------------------------------------------ geometrie

    /** Position d'un point d'arene dans l'instance. */
    public Location loc(Pos pos) {
        return pos.at(world, offX, offZ);
    }

    public Location stands() {
        Pos pos = arena.point("stands");
        if (pos == null) {
            return new Location(world, (minX + maxX) / 2.0 + 0.5, maxY, (minZ + maxZ) / 2.0 + 0.5);
        }
        return loc(pos);
    }

    public boolean contains(Location location) {
        return location.getWorld() == world && inBounds(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /** Vrai si la position est dans la zone de l'emplacement (arene + marge). */
    public boolean nearby(Location location, int margin) {
        return location.getWorld() == world
                && location.getBlockX() >= minX - margin && location.getBlockX() <= maxX + margin
                && location.getBlockZ() >= minZ - margin && location.getBlockZ() <= maxZ + margin;
    }

    // ------------------------------------------------------------------ messages

    protected Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

    public void broadcast(Component message) {
        Component full = plugin.prefix().append(message);
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(full);
            }
        }
    }

    public void broadcastTo(Set<UUID> targets, Component message) {
        Component full = plugin.prefix().append(message);
        for (UUID uuid : targets) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(full);
            }
        }
    }

    public void title(Set<UUID> targets, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Title value = Title.title(title, subtitle, Title.Times.times(
                Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L)));
        for (UUID uuid : targets) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showTitle(value);
            }
        }
    }

    public List<Player> onlineMembers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ arrivee / depart

    /** Le collage de l'arene est termine : les joueurs deja inscrits sont envoyes dans les gradins. */
    public void onPasted() {
        if (phase != Phase.PREPARING) {
            return;
        }
        phase = Phase.WAITING;
        for (Player player : onlineMembers()) {
            arriveInStands(player);
        }
        onReady();
    }

    public boolean ready() {
        return phase != Phase.PREPARING && phase != Phase.CLOSED;
    }

    /** Ajoute un joueur. Renvoie null si tout va bien, sinon le motif du refus. */
    public Component admit(Player player) {
        if (phase == Phase.CLOSED || closing) {
            return t("join.closed", "<red>Cette partie est terminée.");
        }
        Component refusal = canAdmit(player);
        if (refusal != null) {
            return refusal;
        }
        members.add(player.getUniqueId());
        if (publicGame && !queue.contains(player.getUniqueId())) {
            queue.add(player.getUniqueId());
        }
        emptySeconds = 0;
        onMemberJoined(player);
        if (ready()) {
            arriveInStands(player);
        } else {
            player.sendMessage(plugin.prefix().append(t("join.preparing", "<gray>Préparation de l'arène…")));
        }
        // 1.26.0 : relance arrêtée faute de joueurs : elle reprend dès qu'il y en a assez.
        if (relanceEnAttente && !relanceSuspendue && phase == Phase.WAITING && validatePrivateStart() == null) {
            lancerCompteARebours();
            broadcast(t("relance.reprise", "<green>Assez de joueurs : nouvelle partie dans <white><s></white> s.", "s", relance));
        } else if (relance > 0 && barreRelance != null) {
            player.showBossBar(barreRelance);
        }
        return null;
    }

    /** Retire un joueur (sans le teleporter : c'est le gestionnaire qui s'en charge). */
    public void release(UUID uuid, boolean disconnected) {
        if (!members.remove(uuid)) {
            return;
        }
        Player leaving = Bukkit.getPlayer(uuid);
        if (leaving != null && barreRelance != null) {
            leaving.hideBossBar(barreRelance); // 1.26.0
        }
        boolean wasParticipant = participants.contains(uuid);
        queue.remove(uuid);
        matchSpectators.remove(uuid);
        onMemberLeft(uuid, wasParticipant, disconnected);
        participants.remove(uuid);
        if (uuid.equals(host)) {
            host = members.isEmpty() ? null : members.iterator().next();
            if (host != null && !publicGame) {
                Player newHost = Bukkit.getPlayer(host);
                if (newHost != null) {
                    newHost.sendMessage(plugin.prefix().append(t("host.new", "<yellow>Vous êtes maintenant l'hôte de la partie.")));
                }
            }
        }
    }

    /** Envoie un joueur dans les gradins avec l'etat "spectateur d'attente". */
    public void arriveInStands(Player player) {
        matchSpectators.remove(player.getUniqueId());
        plugin.hub().applyStandsState(player);
        player.teleport(stands());
        onArrived(player);
        if (!publicGame) {
            // 1.26.0 : « Quitter la partie » et, entre deux manches, « Passer spectateur » dans la barre d'objets.
            plugin.items().giveStandsItems(player, phase == Phase.WAITING && minigame.getBool("allow-spectate", true));
        }

    }

    // ------------------------------------------------------------------ crochets des moteurs

    protected Component canAdmit(Player player) {
        return null;
    }

    /** L'arene est en place (les joueurs deja inscrits ont ete envoyes dans les gradins). */
    protected void onReady() {
    }

    protected void onMemberJoined(Player player) {
    }

    /** Le joueur vient d'arriver dans les gradins (arrivee, ou retour apres un match). */
    protected void onArrived(Player player) {
    }

    protected abstract void onMemberLeft(UUID uuid, boolean wasParticipant, boolean disconnected);

    protected abstract int minParticipants();

    protected abstract int maxParticipants();

    protected abstract int gatherSeconds();

    /** Parties publiques : choisit les joueurs du prochain match dans la file. */
    protected abstract List<UUID> pickParticipants(List<UUID> queue);

    /** Parties privees : null si le match peut demarrer, sinon le motif. */
    protected abstract Component validatePrivateStart();

    /** Lance le match avec {@link #participants}. */
    protected abstract void beginMatch();

    /** Appele chaque seconde pendant un match. */
    protected abstract void matchSecond();

    /** Appele a chaque tick de l'instance (rapide) pendant un match. */
    protected void matchTick() {
    }

    /** Un participant vient de mourir (evenement de mort). */
    public abstract void handleDeath(Player victim, Player killer);

    /** Le joueur est tombe sous l'arene. */
    public abstract void handleVoid(Player player);

    public abstract boolean canBuild(Player player);

    /** Peut utiliser les blocs (portes, leviers...) : les changements sont suivis puis restaures. */
    public abstract boolean canInteract(Player player);

    public abstract boolean canBreak(Player player, Block block);

    public abstract boolean takesDamage(Player player);

    public abstract boolean canFight(Player attacker, Player victim);

    /**
     * 1.17.0 : crochets generiques pour les jeux fournis par d'autres plugins (KG_BoatRace...), a la place des tests
     * « instanceof RaceInstance » du hub et des ecouteurs.
     * Le joueur est-il en course (vehicule verrouille : pas de sortie, pas de renvoi en tribune a la reapparition) ?
     */
    public boolean racing(UUID uuid) {
        return false;
    }

    /** Objet « dernier point de controle » utilise (clic droit). */
    public void useCheckpointItem(Player player) {
    }

    /** Le joueur va etre deplace (changement de partie...) : ne plus verrouiller son vehicule. */
    public void releaseHold(UUID uuid) {
    }

    /**
     * 1.20.0 : bouton propose dans le menu de la partie par le jeu lui-meme (ex. « Recommencer depuis le départ » du
     * Parcours, dans KG_Parkour), a la place des tests « instanceof RaceInstance » du menu.
     */
    public record MenuAction(Component label, java.util.function.Consumer<Player> action) {
    }

    /** 1.20.0 : lignes d'information du jeu dans le menu de la partie (ex. « Mode entraînement »). */
    public List<Component> menuInfo(Player player) {
        return List.of();
    }

    /** 1.20.0 : boutons du jeu dans le menu de la partie. */
    public List<MenuAction> menuActions(Player player) {
        return List.of();
    }

    /**
     * 1.22.0 : le joueur vient de reapparaitre apres une mort dans cette partie (hors course, voir racing). Par defaut :
     * retour dans les gradins ; le PvP Kit (KG_PvpKit) le met en spectateur si le match continue.
     */
    public void onRespawned(Player player) {
        arriveInStands(player);
    }

    /** 1.22.0 : commande « /kalgames vote » : ouvre le vote du jeu s'il en a un (PvP Kit), sinon renvoie false. */
    public boolean openVote(Player player) {
        return false;
    }

    public boolean frozen(Player player) {
        return false;
    }

    /** L'inventaire est verrouille (gradins, attente...). */
    public abstract boolean inventoryLocked(Player player);

    /** Nettoyage propre aux moteurs avant fermeture. */
    protected void onClose() {
    }

    /** Retour au salon apres un match (votes remis a zero...). */
    protected void onMatchReset() {
    }

    /** Cases supplementaires de l'action bar des joueurs (file, etc.). */
    protected Component statusFor(Player player) {
        return null;
    }

    // ------------------------------------------------------------------ demarrage

    /** Parties privees : demande de l'hote. Renvoie null si OK, sinon un motif. */
    public Component requestStart(Player requester) {
        if (publicGame) {
            return t("start.public", "<red>Les parties publiques démarrent automatiquement.");
        }
        if (!isHost(requester.getUniqueId())) {
            return t("start.host-only", "<red>Seul l'hôte peut lancer la partie.");
        }
        if (phase != Phase.WAITING) {
            return t("start.not-waiting", "<red>La partie est déjà en cours.");
        }
        Component error = validatePrivateStart();
        if (error != null) {
            return error;
        }
        participants.clear();
        participants.addAll(members);
        prepareRanking();
        beginMatch();
        return null;
    }

    private List<UUID> onlineQueue() {
        List<UUID> list = new ArrayList<>();
        for (UUID uuid : queue) {
            if (Bukkit.getPlayer(uuid) != null && members.contains(uuid)) {
                list.add(uuid);
            }
        }
        return list;
    }

    private void tickPublicQueue() {
        List<UUID> online = onlineQueue();
        int min = minParticipants();
        if (phase == Phase.WAITING) {
            if (online.size() >= min) {
                phase = Phase.STARTING;
                secondsLeft = online.size() >= maxParticipants() ? 3 : gatherSeconds();
                broadcast(t("queue.gather", "<green>Assez de joueurs ! Le match démarre dans <white><s></white> s.", "s", secondsLeft));
            }
        } else if (phase == Phase.STARTING) {
            if (online.size() < min) {
                phase = Phase.WAITING;
                broadcast(t("queue.cancelled", "<yellow>Pas assez de joueurs : le lancement est annulé."));
                return;
            }
            if (online.size() >= maxParticipants() && secondsLeft > 5) {
                secondsLeft = 5;
            }
            secondsLeft--;
            if (secondsLeft > 0 && (secondsLeft <= 3 || secondsLeft == 10 || secondsLeft == 20 || secondsLeft == 30)) {
                broadcast(t("queue.countdown", "<gray>Match dans <white><s></white> s…", "s", secondsLeft));
            }
            if (secondsLeft <= 0) {
                List<UUID> picked = pickParticipants(online);
                if (picked.size() < min) {
                    phase = Phase.WAITING;
                    return;
                }
                queue.removeAll(picked);
                participants.clear();
                participants.addAll(picked);
                prepareRanking();
                beginMatch();
            }
        }
    }

    // ------------------------------------------------------------------ boucle

    /** Appele toutes les {@link #TICK_INTERVAL} ticks par le gestionnaire. */
    public final void tick() {
        if (phase == Phase.CLOSED || phase == Phase.PREPARING) {
            return;
        }
        // Chute sous l'arene (joueurs de l'instance).
        for (Player player : onlineMembers()) {
            if (player.getWorld() == world && player.getLocation().getY() < minY - 12) {
                handleVoid(player);
            }
        }
        if (phase == Phase.VOTE || phase == Phase.COUNTDOWN || phase == Phase.RUNNING) {
            matchTick();
        }
        tickAccumulator += TICK_INTERVAL;
        if (tickAccumulator < TICKS_PER_SECOND) {
            return;
        }
        tickAccumulator -= TICKS_PER_SECOND;
        second();
    }

    private void second() {
        if (members.isEmpty()) {
            emptySeconds++;
            if (emptySeconds >= (publicGame ? 30 : 5)) {
                plugin.instances().close(this, null);
            }
            return;
        }
        emptySeconds = 0;

        if (publicGame && (phase == Phase.WAITING || phase == Phase.STARTING)) {
            tickPublicQueue();
        }
        if (phase == Phase.VOTE || phase == Phase.COUNTDOWN || phase == Phase.RUNNING || phase == Phase.ENDING) {
            matchSecond();
        }
        if (phase == Phase.WAITING && relanceActive()) {
            secondeRelance(); // 1.26.0
        }
        for (Player player : onlineMembers()) {
            Component status = statusFor(player);
            if (status != null) {
                player.sendActionBar(status);
            }
        }
    }


    /** Texte de la file d'attente d'une partie publique (null si le joueur n'est pas concerne). */
    protected final Component queueStatus(Player player) {
        UUID uuid = player.getUniqueId();
        int position = queuePosition(uuid);
        if (position > 0) {
            String state = switch (phase) {
                case WAITING -> "en attente de joueurs (" + onlineQueue().size() + "/" + minParticipants() + ")";
                case STARTING -> "match dans " + secondsLeft + " s";
                default -> "match en cours";
            };
            return t("queue.status", "<gold>File d'attente <white><pos>/<total></white> <dark_gray>- <gray><state>",
                    "pos", position, "total", queue.size(), "state", state);
        }
        if (!participants.contains(uuid)) {
            return t("queue.spectating", "<gray>Vous regardez la partie. Rejoignez la file depuis le menu.");
        }
        return null;
    }

    /** Position (1 = prochain) d'un joueur dans la file, ou 0. */
    public int queuePosition(UUID uuid) {
        int index = queue.indexOf(uuid);
        return index < 0 ? 0 : index + 1;
    }

    public boolean queued(UUID uuid) {
        return queue.contains(uuid);
    }

    /** Parties publiques : entrer / sortir de la file. */
    public void setQueued(UUID uuid, boolean value) {
        if (!publicGame || !members.contains(uuid)) {
            return;
        }
        if (value) {
            if (!queue.contains(uuid) && !participants.contains(uuid)) {
                queue.add(uuid);
            }
        } else {
            queue.remove(uuid);
        }
    }

    // ------------------------------------------------------------------ fin de match / nettoyage

    /** Fin de match : arene restauree, tout le monde dans les gradins, participants reinscrits en file. */
    protected void endMatch() {
        restoreBlocks();
        clearEntities();
        List<UUID> back = new ArrayList<>(participants);
        participants.clear();
        matchSpectators.clear();
        onMatchReset();
        phase = Phase.WAITING;
        secondsLeft = 0;
        for (Player player : onlineMembers()) {
            arriveInStands(player);
        }
        if (publicGame) {
            for (UUID uuid : back) {
                if (members.contains(uuid) && !queue.contains(uuid)) {
                    queue.add(uuid);
                }
            }
        }
        if (relanceActive()) {
            // 1.26.0 : partie privée : on reste dans la salle d'attente, relance après relance.secondes (15 s).
            plugin.later(1L, () -> {
                if (!closing && phase == Phase.WAITING) {
                    apresManche();
                }
            });
            return;
        }
        if (plugin.getConfig().getBoolean("instances.close-after-match", true)) {
            // Fin de partie : les joueurs sont renvoyes au hub ; l'arene, remise en etat, est gardee de cote pour la
            // partie suivante (pas de rechargement, donc pas de lag).
            plugin.later(1L, () -> {
                if (!closing) {
                    // 1.24.0 : joueurs et spectateurs presents a la fin du match, pour l'objet « Rejouer ».
                    List<Player> players = onlineMembers();
                    List<Player> watchers = onlineSpectators();
                    plugin.instances().close(this, plugin.t("game.finished-hub",
                            "<gray>Partie terminée. Vous êtes renvoyé au hub."));
                    plugin.replay().offerAfterMatch(this, players, watchers);
                }
            });
        }
    }

    public void track(Block block) {
        if (!inBounds(block.getX(), block.getY(), block.getZ())) {
            return;
        }
        originals.putIfAbsent(key(block.getX(), block.getY(), block.getZ()), block.getBlockData());
    }

    /** Enregistre l'etat d'origine d'une case (si elle n'a pas deja ete enregistree). */
    public void trackOriginal(int x, int y, int z, BlockData original) {
        if (inBounds(x, y, z)) {
            originals.putIfAbsent(key(x, y, z), original);
        }
    }

    public void markPlaced(Block block) {
        placed.add(key(block.getX(), block.getY(), block.getZ()));
    }

    public boolean isPlaced(Block block) {
        return placed.contains(key(block.getX(), block.getY(), block.getZ()));
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int keyX(long key) {
        return (int) (key >> 38) << 6 >> 6;
    }

    private static int keyZ(long key) {
        return (int) (key >> 12 & 0x3FFFFFF) << 6 >> 6;
    }

    private static int keyY(long key) {
        return (int) (key & 0xFFF) << 20 >> 20;
    }

    protected void restoreBlocks() {
        for (Map.Entry<Long, BlockData> entry : originals.entrySet()) {
            long key = entry.getKey();
            world.getBlockAt(keyX(key), keyY(key), keyZ(key)).setBlockData(entry.getValue(), false);
        }
        originals.clear();
        placed.clear();
    }

    /** Supprime les entites non-joueurs (objets, fleches, bateaux...) de tout l'emplacement de la partie. */
    protected void clearEntities() {
        int cell = Math.max(1, plugin.worlds().spacing() >> 4);
        for (int cx = slotX >> 4; cx < (slotX >> 4) + cell; cx++) {
            for (int cz = slotZ >> 4; cz < (slotZ >> 4) + cell; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                Chunk chunk = world.getChunkAt(cx, cz);
                for (Entity entity : chunk.getEntities()) {
                    if (!(entity instanceof Player)) {
                        entity.remove();
                    }
                }
            }
        }
    }

    /** L'arene va servir a une autre partie : blocs modifies restaures, entites supprimees. */
    public void restoreForReuse() {
        restoreBlocks();
        clearEntities();
    }

    /** Fermeture (appelee par le gestionnaire, une seule fois). */
    public final void shutdown() {
        if (closing) {
            return;
        }
        closing = true;
        phase = Phase.CLOSED;
        cacherBarre(); // 1.26.0
        onClose();
        clearEntities();
    }

    public boolean closing() {
        return closing;
    }
}
