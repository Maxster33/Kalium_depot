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

    public static final MinigameType PVP_KIT = register(new MinigameType("PVP_KIT", "PvP Kit", true,
            "Combat d'équipes (jusqu'à 4) avec vote du kit. Dernière équipe en vie.",
            List.of(
                    integer("vote-seconds", "Durée du vote de kit (s)", 60, 10, 180, "Le vote se termine plus tôt si tout le monde a voté."),
                    integer("countdown-seconds", "Compte à rebours avant combat (s)", 3, 0, 10, "0 = combat immediat."),
                    integer("end-delay-seconds", "Délai après la victoire (s)", 5, 1, 30, "Avant le retour en tribune."),
                    integer("points-win", "Points par victoire", 1, 0, 50, "Points de base d'un joueur gagnant."),
                    integer("points-bonus", "Bonus par joueur d'écart", 2, 0, 50, "Ajouté par joueur d'écart quand l'équipe gagnante est en infériorité."),
                    integer("public-team-size", "Partie publique : joueurs par équipe", 1, 1, 4, "1 = chacun pour soi."),
                    integer("public-min-teams", "Partie publique : équipes minimum", 2, 2, 4, "Nombre d'équipes pour lancer un match."),
                    integer("public-max-teams", "Partie publique : équipes maximum", 4, 2, 4, "Un match démarre dès que ce nombre est atteint."),
                    integer("public-gather-seconds", "Partie publique : attente avant lancement (s)", 30, 5, 180, "Délai dès que le minimum est atteint."),
                    integer("prewarm-arenas", "Copies de chaque arène préchargées au démarrage", 0, 0, 10, "Collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
                    integer("max-private-games", "Parties privées simultanées maximum", 0, 0, 40, "0 = pas de limite."),
                    bool("break-map", "Casser les blocs de la carte", false, "Non : seuls les blocs posés pendant le match sont cassables. Tout est restauré à la fin."),
                    bool("bedrock-option", "Option PvP Bedrock (Haste) proposée", true, "Propose la case Haste dans les parties privées."),
                    bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
            List.of(
                    single("stands", "Gradins (tribune)", true, "Où attendent les spectateurs et les éliminés."),
                    single("spawn-a", "Départ équipe A", true, "Regarde vers le centre."),
                    single("spawn-b", "Départ équipe B", true, "Regarde vers le centre."),
                    single("spawn-c", "Départ équipe C", false, "Optionnel : active la 3e équipe."),
                    single("spawn-d", "Départ équipe D", false, "Optionnel : active la 4e équipe."))));

    public static final MinigameType RUSH = register(new MinigameType("RUSH", "Rush", true,
            "Chaque équipe défend son lit. Récupérez des ressources, construisez des ponts, achetez de l'équipement et détruisez le lit adverse. La dernière équipe en vie gagne.",
            List.of(
                    integer("team-size", "Joueurs maximum par équipe", 4, 1, 4, "Une équipe pleine ne peut plus être rejointe."),
                    integer("min-players", "Partie publique : joueurs minimum", 2, 2, 16, "Pour lancer le compte à rebours (au moins deux équipes non vides)."),
                    integer("gather-seconds", "Partie publique : attente avant lancement (s)", 30, 5, 180, "Délai dès que le minimum est atteint."),
                    integer("respawn-seconds", "Délai de réapparition (s)", 3, 0, 30, "Compte à rebours visible avant de revenir sur son lit."),
                    integer("bed-destroy-minutes", "Autodestruction des lits (min)", 15, 0, 120, "Après ce temps de partie, tous les lits sont détruits : plus aucune réapparition. 0 = jamais."),
                    integer("bronze-interval-ticks", "Bronze : un objet toutes les N ticks", 10, 1, 1200, "Par base. 20 ticks = 1 seconde : 10 = 2 par seconde."),
                    integer("silver-interval-ticks", "Silver : un objet toutes les N ticks", 20, 1, 1200, "Par base. 20 = 1 par seconde."),
                    integer("gold-interval-ticks", "Gold : un objet toutes les N ticks", 60, 1, 1200, "Par base. 60 = 1 toutes les 3 secondes."),
                    integer("end-delay-seconds", "Délai après la victoire (s)", 10, 1, 60, "Avant le retour au hub."),
                    integer("points-win", "Points par victoire", 1, 0, 50, "Points de chaque joueur de l'équipe gagnante (classements)."),
                    integer("prewarm-arenas", "Copies de chaque arène préchargées au démarrage", 0, 0, 10, "Collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
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

    public static final MinigameType BUILD_BATTLE = register(new MinigameType("BUILD_BATTLE", "Build Battle", false,
            "Chaque joueur construit sur un thème, puis les constructions sont votées. Moteur à venir : la configuration est déjà disponible.",
            List.of(
                    integer("build-seconds", "Durée de construction (s)", 300, 60, 1800, ""),
                    integer("vote-seconds", "Durée du vote (s)", 30, 10, 120, ""),
                    integer("plot-size", "Taille d'un terrain (blocs)", 24, 8, 64, ""),
                    text("themes", "Thèmes (séparés par des virgules)", "Château,Pirate,Espace,Ferme,Volcan", ""),
                    integer("min-players", "Joueurs minimum", 2, 2, 16, ""),
                    integer("max-players", "Joueurs maximum", 12, 2, 32, "")),
            List.of(
                    single("stands", "Salle d'attente", true, ""),
                    list("plots", "Terrains de construction", true, "Un point central par terrain.")),
            List.of()));



    // 1.17.0 : moteurs des types fournis par KalGames. Les autres plugins (KG_BoatRace, KG_Parkour depuis 1.20.0...)
    // enregistrent leurs propres types avec register(), moteur compris.
    static {
        PVP_KIT.engine(fr.kalium.games.game.PvpInstance::new);
        RUSH.engine(fr.kalium.games.game.RushInstance::new).prewarmAllowed(true);
        PVP_KIT.prewarmAllowed(true);
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
