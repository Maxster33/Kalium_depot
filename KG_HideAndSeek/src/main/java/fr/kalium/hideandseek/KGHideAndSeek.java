package fr.kalium.hideandseek;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;
import static fr.kalium.games.model.SettingSpec.text;

/**
 * KG_HideAndSeek : le Hide and Seek de Kal-Games (demande de LeKiwi06, 06/10/2026), un plugin par jeu (REGLES.md, regle
 * 2.2), comme KG_PvpKit, KG_BoatRace et KG_Parkour. S'appuie sur le moteur de parties de KalGames (arenes, files
 * d'attente, parties publiques et privees) : ce plugin enregistre le type « HIDE_AND_SEEK » (reglages, points d'arene,
 * moteur, formulaire de partie privee) et garde ses propres donnees : blocs de chaque map (blocks.yml), sons du
 * soundboard (sounds.yml), sons favoris des joueurs (favorites.yml). Textes : lang.yml de KalGames, cles « hns.* ».
 */
public final class KGHideAndSeek extends JavaPlugin implements Listener {

    /** Equipe du tableau des scores des hiders : pseudo masque au-dessus de la tete, pas de bousculade. */
    private static final String TEAM = "kg_hns";

    private static KGHideAndSeek instance;

    private KalGames games;
    private MapBlocks blocks;
    private SoundBoard sounds;
    private HideItems items;
    private HideMenus menus;
    private NamespacedKey heartsKey;
    private NamespacedKey absorptionKey;

    public static KGHideAndSeek get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        games = (KalGames) getServer().getPluginManager().getPlugin("KalGames");
        heartsKey = new NamespacedKey(this, "seeker_hearts");
        absorptionKey = new NamespacedKey(this, "seeker_absorption");
        blocks = new MapBlocks(this);
        blocks.load();
        sounds = new SoundBoard(this);
        sounds.load();
        items = new HideItems(this, games);
        menus = new HideMenus(this, games);
        dropTeam();
        getServer().getPluginManager().registerEvents(this, this);

        MinigameType type = new MinigameType("HIDE_AND_SEEK", "Hide and Seek",
                "Les hiders imitent un bloc du décor et se cachent ; les seekers doivent tous les trouver avant la fin du chrono.",
                List.of(
                        integer("hide-seconds", "Temps de cachette (s)", 30, 5, 180, "Les seekers restent enfermés dans leur salle pendant ce temps."),
                        integer("seek-seconds", "Durée de la recherche (s)", 300, 60, 1800, "Au moins un hider en vie à la fin : les hiders gagnent."),
                        integer("players-per-seeker", "Joueurs pour 1 seeker", 5, 2, 16, "5 : 1 seeker jusqu'à 5 joueurs, 2 jusqu'à 10, 3 jusqu'à 15, 4 à 16. Les volontaires d'abord."),
                        integer("min-players", "Joueurs minimum", 3, 2, 16, "Pour lancer une partie."),
                        integer("max-players", "Joueurs maximum", 16, 3, 16, "Par partie."),
                        integer("public-gather-seconds", "Attente avant lancement (s)", 30, 5, 180, "Partie publique : délai dès que le minimum est atteint."),
                        integer("solid-seconds", "Immobile avant solide (s)", 3, 1, 30, "Un hider immobile devient son bloc ; il redevient un joueur dès qu'il bouge."),
                        integer("block-change-seconds", "Délai entre 2 blocs (s)", 30, 0, 300, "Entre deux changements de bloc d'un hider."),
                        integer("sound-cooldown-seconds", "Délai du soundboard (s)", 3, 0, 60, "Entre deux sons joués par un hider."),
                        integer("auto-sound-seconds", "Son automatique : intervalle (s)", 30, 0, 300, "Chaque hider sonne à cet intervalle pendant la recherche. 0 = jamais."),
                        integer("auto-sound-gap-ticks", "Son automatique : écart (ticks)", 15, 0, 100, "Entre deux hiders. 20 ticks = 1 seconde : 15 = 0,75 s."),
                        text("auto-sound", "Son automatique", "entity.villager.ambient", "Identifiant Minecraft du son (villageois par défaut)."),
                        integer("escape-points", "Évasion : points requis", 5, 0, 100, "Points gagnés dans la partie pour obtenir l'évasion (Vitesse V, 2 s)."),
                        integer("escape-cooldown-seconds", "Évasion : recharge (s)", 60, 5, 600, "Entre deux évasions."),
                        integer("seeker-respawn-seconds", "Retour d'un seeker (s)", 15, 0, 120, "Temps passé dans la salle des seekers à 0 cœur, ou pour un hider devenu seeker."),
                        bool("eliminated-becomes-seeker", "Hider éliminé : devient seeker", false, "Non : il devient spectateur. Toujours appliqué en partie publique ; choix de l'hôte en partie privée."),
                        integer("survive-interval-seconds", "Survie : intervalle (s)", 20, 5, 300, "Un hider en vie gagne des points à cet intervalle, pendant la recherche."),
                        integer("points-survive", "Survie : points", 1, 0, 100, "Points gagnés à chaque intervalle de survie."),
                        integer("points-survivor", "Hider en vie à la fin", 8, 0, 100, "Points de chaque hider encore en vie à la fin du chrono."),
                        integer("points-find", "Seeker : points par hider", 6, 0, 100, "Pour chaque hider éliminé."),
                        integer("points-all-found", "Seekers : tous trouvés", 5, 0, 100, "Pour chaque seeker si tous les hiders sont trouvés avant la fin."),
                        integer("points-sound-hundredths", "Soundboard : centièmes de point", 25, 0, 1000, "Points par son du soundboard, en centièmes : 25 = 0,25 point."),
                        integer("sound-points-seconds", "Soundboard : intervalle compté (s)", 10, 0, 300, "Un seul son rapporte des points pendant cet intervalle."),
                        integer("sound-points-range", "Soundboard : distance (blocs)", 24, 1, 128, "Le son ne rapporte des points que si un seeker est à cette distance au plus."),
                        integer("end-delay-seconds", "Délai après la fin (s)", 8, 1, 30, "Avant le retour au hub."),
                        integer("prewarm-arenas", "Arènes préchargées", 0, 0, 10, "Copies de chaque arène, collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
                        integer("max-private-games", "Parties privées simultanées maximum", 0, 0, 40, "0 = pas de limite."),
                        bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
                List.of(
                        single("stands", "Tribune", true, "Où attendent les joueurs avant la partie."),
                        single("hider-spawn", "Départ des hiders", true, "Où les hiders apparaissent au début de la cachette."),
                        single("seeker-room", "Salle des seekers", true, "Pièce fermée : les seekers y attendent pendant la cachette, et y reviennent à 0 cœur."),
                        single("seeker-spawn", "Départ des seekers", true, "À la sortie de la salle : où les seekers sont lâchés.")))
                .engine(HideInstance::new)
                .prewarmAllowed(true)
                .createForm(new CreateForm())
                .adminInfo(minigame -> {
                    int unset = 0;
                    for (Arena arena : games.repository().arenasOf(minigame.id())) {
                        if (blocks.of(arena.id()).isEmpty()) {
                            unset++;
                        }
                    }
                    List<Component> lines = new ArrayList<>();
                    lines.add(games.t("hns.admin-sounds-count", "<gray>Sons du soundboard : <white><n></white>", "n", sounds.all().size()));
                    if (unset > 0) {
                        lines.add(games.t("hns.admin-maps-unset", "<red>Maps sans bloc réglé : <white><n></white> <gray>(injouables)", "n", unset));
                    }
                    return lines;
                })
                .adminAction(new MinigameType.AdminAction(games.t("hns.admin-blocks", "<aqua>Blocs des maps"), menus::openMaps))
                .adminAction(new MinigameType.AdminAction(games.t("hns.admin-sounds", "<aqua>Sons du soundboard"), menus::openSoundsAdmin));
        MinigameType.register(type);
        getLogger().info("Hide and Seek enregistré auprès de KalGames (" + sounds.all().size() + " son(s)).");
    }

    /**
     * A l'arret, Paper desactive ce plugin AVANT KalGames : les parties de Hide and Seek en cours sont fermees ici, tant
     * que le code de ce plugin est encore disponible (comme KG_PvpKit).
     */
    @Override
    public void onDisable() {
        if (games != null && games.instances() != null) {
            for (GameInstance game : new ArrayList<>(games.instances().all())) {
                if (game instanceof HideInstance) {
                    games.instances().close(game, null);
                }
            }
        }
        dropTeam();
        instance = null;
    }

    public MapBlocks blocks() {
        return blocks;
    }

    public SoundBoard sounds() {
        return sounds;
    }

    public HideItems items() {
        return items;
    }

    public HideMenus menus() {
        return menus;
    }

    private HideInstance gameOf(Player player) {
        return games.instances().of(player) instanceof HideInstance game ? game : null;
    }

    // ------------------------------------------------------------------ etat des joueurs

    private Team team() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = main.getTeam(TEAM);
        if (team == null) {
            team = main.registerNewTeam(TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        return team;
    }

    /** L'equipe est enregistree avec le monde : on la supprime au demarrage et a l'arret pour n'y oublier personne. */
    private void dropTeam() {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM);
        if (team != null) {
            team.unregister();
        }
    }

    void joinTeam(Player player) {
        team().addEntry(player.getName());
    }

    /** Cache le hider (devenu un bloc) a tous les autres joueurs du serveur. */
    void conceal(Player hider) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(hider) && other.canSee(hider)) {
                other.hidePlayer(this, hider);
            }
        }
    }

    void reveal(Player hider) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(hider)) {
                other.showPlayer(this, hider);
            }
        }
    }

    /** Seeker : 5 coeurs au plus, et une reserve d'absorption de 3 coeurs (vide au depart). */
    void applySeekerHearts(Player player) {
        setModifier(player, Attribute.MAX_HEALTH, heartsKey, HideInstance.SEEKER_HEALTH - 20.0);
        setModifier(player, Attribute.MAX_ABSORPTION, absorptionKey, HideInstance.SEEKER_ABSORPTION);
    }

    private void setModifier(Player player, Attribute attribute, NamespacedKey key, double amount) {
        AttributeInstance value = player.getAttribute(attribute);
        if (value == null) {
            return;
        }
        value.removeModifier(key);
        value.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    /**
     * Retire tout ce que ce jeu a pose sur le joueur : coeurs de seeker, absorption, equipe des hiders, invisibilite pour
     * les autres. Sans effet sur un joueur qui n'a rien de tout cela (appele aussi a chaque arrivee sur le serveur).
     */
    void clearState(Player player) {
        player.setAbsorptionAmount(0);
        AttributeInstance absorption = player.getAttribute(Attribute.MAX_ABSORPTION);
        if (absorption != null) {
            absorption.removeModifier(absorptionKey);
        }
        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null && health.getModifier(heartsKey) != null) {
            health.removeModifier(heartsKey);
            if (!player.isDead()) {
                player.setHealth(health.getValue());
            }
        }
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM);
        if (team != null) {
            team.removeEntry(player.getName());
        }
        reveal(player);
    }

    // ------------------------------------------------------------------ ecouteurs

    /** Retour d'un joueur deconnecte en cours de partie : il reprend son role (possible jusqu'a la fin de la partie). */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        clearState(player);
        for (GameInstance game : new ArrayList<>(games.instances().all())) {
            if (game instanceof HideInstance hide && hide.awaits(player.getUniqueId())) {
                Component refusal = games.instances().joinInstance(player, hide);
                if (refusal != null) {
                    player.sendMessage(games.prefix().append(refusal));
                }
                return;
            }
        }
    }

    /** Aucun degat entre joueurs ; un seeker qui touche un hider en mouvement l'elimine. */
    @EventHandler(priority = EventPriority.LOW)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        HideInstance game = gameOf(attacker);
        if (game == null) {
            return;
        }
        event.setCancelled(true);
        if (game == gameOf(victim)) {
            game.directHit(attacker, victim);
        }
    }

    /** Clic gauche d'un seeker : en mode aventure, le jeu n'envoie que le mouvement de bras. */
    @EventHandler
    public void onSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        HideInstance game = gameOf(event.getPlayer());
        if (game != null) {
            game.swing(event.getPlayer());
        }
    }

    /** Objets de la barre du hider (clic droit) ; clics des seekers. Passe avant l'ecouteur de KalGames. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        HideInstance game = gameOf(player);
        if (game == null) {
            return;
        }
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            game.swing(player);
            return;
        }
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        game.noteRightClick(player);
        String kind = items.kind(event.getItem());
        if (kind == null) {
            return;
        }
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        Block clicked = event.getClickedBlock();
        if (action == Action.RIGHT_CLICK_BLOCK && clicked != null && clicked.getType().isInteractable()) {
            // Porte, levier... : c'est le bloc qui sert, pas l'objet tenu en main.
            return;
        }
        event.setCancelled(true);
        game.useItem(player, kind);
    }

    /** La vie d'un seeker ne remonte pas toute seule. */
    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            HideInstance game = gameOf(player);
            if (game != null && game.inMatch(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------ creation d'une partie privee

    /** Formulaire de creation d'une partie privee : nombre de joueurs et sort des hiders elimines. */
    private final class CreateForm implements MinigameType.CreateForm {

        @Override
        public List<DialogInput> inputs(Minigame minigame, List<Arena> arenas, fr.kalium.games.gui.Gui gui) {
            int min = Math.max(2, Math.min(16, minigame.getInt("min-players", 3)));
            int max = Math.max(min, Math.min(16, minigame.getInt("max-players", 16)));
            List<DialogInput> inputs = new ArrayList<>();
            if (max > min) {
                inputs.add(gui.number("maxPlayers", games.t("menu.create-max", "Joueurs maximum"), min, max, max, 1));
            }
            inputs.add(gui.toggle("becomeSeeker", games.t("hns.create-become-seeker", "Hider éliminé : devient seeker"),
                    minigame.getBool("eliminated-becomes-seeker", false)));
            return inputs;
        }

        @Override
        public void read(io.papermc.paper.dialog.DialogResponseView view, Map<String, Object> options) {
            Float max = view.getFloat("maxPlayers");
            if (max != null) {
                options.put("maxPlayers", Math.round(max));
            }
            Boolean become = view.getBoolean("becomeSeeker");
            if (become != null) {
                options.put("becomeSeeker", become);
            }
        }
    }
}
