package fr.kalium.games.model;

import java.util.List;

import static fr.kalium.games.model.PointSpec.list;
import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;
import static fr.kalium.games.model.SettingSpec.text;

/**
 * Types de mini-jeux : reglages, points d'arene a definir et moteur du jeu.
 *
 * 1.17.0 : n'est plus une enumeration fermee mais un registre ouvert : un plugin de jeu separe (KG_BoatRace...)
 * enregistre son type (reglages, points d'arene, moteur) avec register(). Les types restants ici sont ceux dont le
 * moteur est encore dans KalGames (ou sans moteur).
 */
public final class MinigameType {

    public static final MinigameType RUSH = register(new MinigameType("RUSH", "Rush", true,
            "Chaque équipe défend son lit. Récupérez des ressources, construisez des ponts, achetez de l'équipement et détruisez le lit adverse. La dernière équipe en vie gagne.",
            List.of(
                    integer("team-size", "Joueurs maximum par équipe", 4, 1, 4, "Une équipe pleine ne peut plus être rejointe."),
                    integer("min-players", "Partie publique : joueurs minimum", 2, 2, 16, "Pour lancer le compte à rebours (au moins deux équipes non vides)."),
                    integer("gather-seconds", "Attente avant lancement (s)", 30, 5, 180, "Partie publique : délai dès que le minimum est atteint."),
                    integer("respawn-seconds", "Délai de réapparition (s)", 3, 0, 30, "Compte à rebours visible avant de revenir sur son lit."),
                    integer("bed-destroy-minutes", "Autodestruction des lits (min)", 15, 0, 120, "Après ce temps de partie, tous les lits sont détruits : plus aucune réapparition. 0 = jamais."),
                    integer("bronze-interval-ticks", "Bronze : ticks par objet", 10, 1, 1200, "Un objet toutes les N ticks, par base. 20 ticks = 1 seconde : 10 = 2 par seconde."),
                    integer("silver-interval-ticks", "Silver : ticks par objet", 20, 1, 1200, "Un objet toutes les N ticks, par base. 20 = 1 par seconde."),
                    integer("gold-interval-ticks", "Gold : ticks par objet", 60, 1, 1200, "Un objet toutes les N ticks, par base. 60 = 1 toutes les 3 secondes."),
                    integer("end-delay-seconds", "Délai après la victoire (s)", 10, 1, 60, "Avant le retour au hub."),
                    integer("points-win", "Points par victoire", 1, 0, 50, "Points de chaque joueur de l'équipe gagnante (classements)."),
                    integer("prewarm-arenas", "Arènes préchargées", 0, 0, 10, "Copies de chaque arène, collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
                    integer("max-private-games", "Parties privées simultanées maximum", 0, 0, 40, "0 = pas de limite."),
                    bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
            RushLayout.points()));

    public static final MinigameType HUNGER_GAMES = register(new MinigameType("HUNGER_GAMES", "Hunger Games", false,
            "Dernier survivant, coffres de butin et bordure qui se réduit. Moteur à venir : la configuration est déjà disponible.",
            List.of(
                    integer("min-players", "Joueurs minimum", 2, 2, 48, ""),
                    integer("max-players", "Joueurs maximum", 24, 2, 48, ""),
                    integer("grace-seconds", "Période sans PvP (s)", 30, 0, 300, ""),
                    integer("border-start", "Bordure initiale (blocs)", 200, 50, 2000, ""),
                    integer("border-end", "Bordure finale (blocs)", 20, 5, 200, ""),
                    integer("border-shrink-after-seconds", "Début de la réduction (s)", 300, 30, 1800, ""),
                    integer("chest-refill-minutes", "Recharge des coffres (min)", 5, 0, 30, "0 = jamais.")),
            List.of(
                    single("stands", "Salle d'attente", true, ""),
                    single("center", "Centre de l'arène", true, ""),
                    list("spawns", "Plateformes de départ", true, "Une par joueur maximum.")),
            List.of(new ItemListSpec("loot", "Butin des coffres"))));

    public static final MinigameType MANHUNT = register(new MinigameType("MANHUNT", "Manhunt", false,
            "Un speedrunner contre des chasseurs qui suivent sa boussole. Moteur à venir : la configuration est déjà disponible.",
            List.of(
                    integer("hunter-count", "Nombre de chasseurs", 1, 1, 8, ""),
                    integer("head-start-seconds", "Avance du speedrunner (s)", 30, 0, 120, ""),
                    integer("compass-update-seconds", "Actualisation de la boussole (s)", 5, 1, 30, ""),
                    bool("generate-world", "Générer un monde vierge", true, "Sinon utilise l'arène modèle."),
                    text("world-seed", "Graine du monde", "", "Vide = aléatoire."),
                    integer("min-players", "Joueurs minimum", 2, 2, 16, ""),
                    integer("max-players", "Joueurs maximum", 8, 2, 16, "")),
            List.of(
                    single("stands", "Salle d'attente", true, ""),
                    single("runner-spawn", "Départ du speedrunner", false, "Si l'arène modèle est utilisée."),
                    single("hunter-spawn", "Départ des chasseurs", false, "Si l'arène modèle est utilisée.")),
            List.of()));

    // 1.21.0 : emplacement « Build Battle » (sans moteur) retire : le jeu est dans KG_BuildBattle / KV_BuildBattle. Un
    // mini-jeu de ce type deja enregistre est garde de cote dans minigames.yml (jamais perdu), comme un type absent.

    // 1.22.0 : le PvP Kit (type PVP_KIT, kits compris) est dans KG_PvpKit, qui enregistre son type au demarrage.

    // 1.17.0 : moteurs des types fournis par KalGames. Les autres plugins (KG_BoatRace, KG_Parkour depuis 1.20.0,
    // KG_PvpKit depuis 1.22.0...) enregistrent leurs propres types avec register(), moteur compris.
    static {
        RUSH.engine(fr.kalium.games.game.RushInstance::new).prewarmAllowed(true);
        // 1.23.0 : 2 joueurs au moins (deux equipes) ; au plus, la taille des equipes x le plus grand nombre de bases.
        RUSH.playerRange((minigame, arenas) -> {
            int teams = 2;
            for (Arena arena : arenas) {
                teams = Math.max(teams, RushLayout.teamCount(arena));
            }
            return new int[]{2, teams * Math.max(1, Math.min(4, minigame.getInt("team-size", 4)))};
        });
    }

    /**
     * 1.23.0 (demande de Maxster33, 06/10/2026) : nombre de joueurs possibles dans une partie de ce jeu, {minimum,
     * maximum}, affiche au survol du jeu dans le menu de Kal-Games ; null = rien d'affiche. arenas = arenes utilisables
     * du mini-jeu (certains jeux sont limites par leur arene).
     */
    @FunctionalInterface
    public interface PlayerRange {
        int[] of(Minigame minigame, List<Arena> arenas);
    }

    /** Par defaut : les reglages « min-players » et « max-players » du jeu, s'il les a tous les deux. */
    private static int[] settingsRange(Minigame minigame, List<Arena> arenas) {
        MinigameType type = minigame.type();
        if (type.setting("min-players") == null || type.setting("max-players") == null) {
            return null;
        }
        return new int[]{minigame.getInt("min-players", 1), minigame.getInt("max-players", 1)};
    }

    /** Liste d'objets (butin...) editee depuis l'inventaire du moderateur. */
    public record ItemListSpec(String key, String label) {
    }

    /**
     * Option numerique proposee a l'hote a la creation d'une partie privee (ex. nombre de tours) : cle dans les
     * options de la partie, bornes, et reglage du mini-jeu qui donne la valeur par defaut.
     */
    public record CreateOption(String key, String label, int min, int max, String defaultSetting, int fallback) {
    }

    /**
     * 1.20.0 : case a cocher proposee a l'hote a la creation d'une partie privee (ex. « Mode entraînement » du
     * Parcours, dans KG_Parkour) : cle (booleen) dans les options de la partie, libelle (MiniMessage).
     */
    public record CreateToggle(String key, String label) {
    }

    /**
     * 1.22.0 : formulaire de creation d'une partie privee entierement fourni par le jeu (ex. equipes, manches et choix
     * du kit du PvP Kit, dans KG_PvpKit), a la place de « Joueurs maximum » et des options / cases ci-dessus. Le choix
     * de l'arene et « Afficher la partie dans la liste » restent geres par KalGames.
     */
    public interface CreateForm {
        /** Champs du formulaire (arenas : arenes utilisables de ce mini-jeu). */
        List<io.papermc.paper.registry.data.dialog.input.DialogInput> inputs(Minigame minigame, List<Arena> arenas,
                                                                          fr.kalium.games.gui.Gui gui);

        /** Lit les champs remplis et les range dans les options de la partie. */
        void read(io.papermc.paper.dialog.DialogResponseView view, java.util.Map<String, Object> options);
    }

    /**
     * 1.22.0 : bouton ajoute par le jeu a sa page « Informations &gt; Parametres » (ex. « Kits du mini-jeu » du PvP Kit,
     * dans KG_PvpKit). KalGames verifie que le joueur est moderateur avant d'executer l'action.
     */
    public record AdminAction(net.kyori.adventure.text.Component label,
                              java.util.function.BiConsumer<org.bukkit.entity.Player, Minigame> action) {
    }

    /** Fabrique d'une partie de ce type (le moteur du jeu). */
    @FunctionalInterface
    public interface GameFactory {
        fr.kalium.games.game.GameInstance create(fr.kalium.games.KalGames plugin, String id, Minigame minigame, Arena arena,
                                                  fr.kalium.games.world.Template template, boolean publicGame,
                                                  java.util.Map<String, Object> options, int slot);
    }

    /** Registre des types (classe interne : initialisee avant le premier register(), quel que soit l'ordre). */
    private static final class Registry {
        static final java.util.Map<String, MinigameType> TYPES = new java.util.LinkedHashMap<>();
        static final List<java.util.function.Consumer<MinigameType>> LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    /**
     * Enregistre (ou remplace, meme nom) un type de mini-jeu. 1.17.0 : utilise par les plugins de jeu separes
     * (KG_BoatRace...) a leur demarrage ; les mini-jeux de ce type deja enregistres dans minigames.yml sont alors
     * rattaches (voir Repository).
     */
    public static MinigameType register(MinigameType type) {
        Registry.TYPES.put(type.name, type);
        for (java.util.function.Consumer<MinigameType> listener : Registry.LISTENERS) {
            listener.accept(type);
        }
        return type;
    }

    /** Appele a chaque enregistrement de type (voir register). */
    public static void onRegister(java.util.function.Consumer<MinigameType> listener) {
        Registry.LISTENERS.add(listener);
    }

    /** Tous les types connus, dans l'ordre d'enregistrement. */
    public static List<MinigameType> values() {
        return List.copyOf(Registry.TYPES.values());
    }

    /** Type de ce nom (ex. "BOAT_RACE") ; IllegalArgumentException s'il n'est pas (encore) enregistre. */
    public static MinigameType valueOf(String name) {
        MinigameType type = name == null ? null : Registry.TYPES.get(name.toUpperCase(java.util.Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Type de mini-jeu inconnu : " + name);
        }
        return type;
    }

    private final String name;
    private final String display;
    private final boolean configurable;
    private final String description;
    private final List<SettingSpec> settings;
    private final List<PointSpec> points;
    private final List<ItemListSpec> itemLists;
    private GameFactory engine;
    private fr.kalium.scoreboards.Category.Kind ranking = fr.kalium.scoreboards.Category.Kind.POINTS;
    private final List<CreateOption> createOptions = new java.util.ArrayList<>();
    private final List<CreateToggle> createToggles = new java.util.ArrayList<>();
    private boolean prewarmAllowed;
    private CreateForm createForm;
    private final List<AdminAction> adminActions = new java.util.ArrayList<>();
    private java.util.function.Function<Minigame, List<net.kyori.adventure.text.Component>> adminInfo = minigame -> List.of();
    private PlayerRange playerRange = MinigameType::settingsRange;

    private MinigameType(String name, String display, boolean configurable, String description, List<SettingSpec> settings,
                         List<PointSpec> points) {
        this(name, display, configurable, description, settings, points, List.of());
    }

    private MinigameType(String name, String display, boolean configurable, String description, List<SettingSpec> settings,
                         List<PointSpec> points, List<ItemListSpec> itemLists) {
        this.name = name;
        this.display = display;
        this.configurable = configurable;
        this.description = description;
        this.settings = settings;
        this.points = points;
        this.itemLists = itemLists;
    }

    /** Type fourni par un autre plugin (le moteur est donne ensuite avec engine()). */
    public MinigameType(String name, String display, String description, List<SettingSpec> settings, List<PointSpec> points) {
        this(name.toUpperCase(java.util.Locale.ROOT), display, true, description, settings, points, List.of());
    }

    public MinigameType engine(GameFactory factory) {
        this.engine = factory;
        return this;
    }

    /** Genre de classement (points, temps de parcours, meilleur tour) dans KG_ScoreBoards. */
    public MinigameType ranking(fr.kalium.scoreboards.Category.Kind kind) {
        this.ranking = kind;
        return this;
    }

    public MinigameType createOption(CreateOption option) {
        this.createOptions.add(option);
        return this;
    }

    public MinigameType createToggle(CreateToggle toggle) {
        this.createToggles.add(toggle);
        return this;
    }

    /** 1.22.0 : formulaire de creation d'une partie privee fourni par le jeu (voir CreateForm). */
    public MinigameType createForm(CreateForm form) {
        this.createForm = form;
        return this;
    }

    /** 1.22.0 : bouton du jeu dans sa page de parametres (voir AdminAction). */
    public MinigameType adminAction(AdminAction action) {
        this.adminActions.add(action);
        return this;
    }

    /** 1.22.0 : lignes d'information du jeu dans sa page de parametres (ex. nombre de kits proposes au vote). */
    public MinigameType adminInfo(java.util.function.Function<Minigame, List<net.kyori.adventure.text.Component>> info) {
        this.adminInfo = info;
        return this;
    }

    /** 1.23.0 : nombre de joueurs possibles, quand les reglages min-players / max-players ne suffisent pas. */
    public MinigameType playerRange(PlayerRange range) {
        this.playerRange = range;
        return this;
    }

    /** 1.23.0 : {minimum, maximum} de joueurs, ou null (rien a afficher). */
    public int[] playerRange(Minigame minigame, List<Arena> arenas) {
        try {
            int[] range = playerRange == null ? null : playerRange.of(minigame, arenas);
            return range == null || range.length < 2 || range[0] < 1 || range[1] < range[0] ? null : range;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Vrai si des copies d'arene peuvent etre collees a l'avance (reglage "prewarm-arenas"). */
    public MinigameType prewarmAllowed(boolean value) {
        this.prewarmAllowed = value;
        return this;
    }

    /** Nom technique, enregistre dans minigames.yml (ex. "PVP_KIT"). */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String display() {
        return display;
    }

    /** Vrai si un moteur de jeu existe (sinon : configuration seulement). */
    public boolean playable() {
        return configurable && engine != null;
    }

    public GameFactory engine() {
        return engine;
    }

    public fr.kalium.scoreboards.Category.Kind ranking() {
        return ranking;
    }

    public List<CreateOption> createOptions() {
        return createOptions;
    }

    public List<CreateToggle> createToggles() {
        return createToggles;
    }

    /** null : formulaire standard (joueurs maximum, options et cases declarees). */
    public CreateForm createForm() {
        return createForm;
    }

    public List<AdminAction> adminActions() {
        return adminActions;
    }

    public List<net.kyori.adventure.text.Component> adminInfo(Minigame minigame) {
        return adminInfo.apply(minigame);
    }

    public boolean prewarmAllowed() {
        return prewarmAllowed;
    }

    public String description() {
        return description;
    }

    public List<SettingSpec> settings() {
        return settings;
    }

    public List<PointSpec> points() {
        return points;
    }

    public List<ItemListSpec> itemLists() {
        return itemLists;
    }

    public SettingSpec setting(String key) {
        for (SettingSpec spec : settings) {
            if (spec.key().equals(key)) {
                return spec;
            }
        }
        return null;
    }
}
