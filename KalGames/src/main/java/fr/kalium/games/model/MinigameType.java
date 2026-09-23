package fr.kalium.games.model;

import java.util.List;

import static fr.kalium.games.model.PointSpec.list;
import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;
import static fr.kalium.games.model.SettingSpec.text;

/** Types de mini-jeux : reglages, points d'arene a definir et disponibilite du moteur. */
public enum MinigameType {

    PVP_KIT("PvP Kit", true,
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
                    single("spawn-d", "Départ équipe D", false, "Optionnel : active la 4e équipe."))),

    PARKOUR("Parcours", true,
            "Course chronométrée avec points de contrôle. Le premier à l'arrivée gagne.",
            List.of(
                    integer("prewarm-arenas", "Copies de chaque arène préchargées au démarrage", 0, 0, 10, "Collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
                    integer("max-private-games", "Parties privées simultanées maximum", 0, 0, 40, "0 = pas de limite."),
                    integer("checkpoint-timeout-seconds", "Temps maximum entre deux points de contrôle (s)", 300, 0, 3600,
                            "Un joueur qui n'atteint pas le suivant à temps est éliminé. 0 = pas de limite."),
                    integer("points-checkpoint", "Points par point de contrôle atteint", 1, 0, 50, "Gagnés à chaque point de contrôle (course uniquement, pas en entraînement)."),
                    integer("points-win", "Points du 1er (2e = -1, 3e = -2)", 3, 0, 50, "Points attribués au podium."),
                    integer("void-y", "Hauteur de chute (Y absolu)", 1, -64, 320, "Sous cette hauteur : retour au dernier point de contrôle. -64 = automatique (juste sous l'arène)."),
                    integer("time-limit-seconds", "Temps limite de la course (s)", 0, 0, 7200, "Fin de la course pour tout le monde. 0 = pas de limite globale."),
                    integer("countdown-seconds", "Compte à rebours (s)", 5, 0, 15, "Avant le départ."),
                    integer("checkpoint-radius", "Rayon des points de contrôle", 3, 1, 12, "En blocs."),
                    integer("min-players", "Joueurs minimum", 1, 1, 16, "Pour lancer une partie publique."),
                    integer("max-players", "Joueurs maximum", 8, 1, 32, "Places par course."),
                    integer("gather-seconds", "Attente avant lancement (s)", 20, 5, 180, "Partie publique."),
                    bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
            List.of(
                    single("stands", "Gradins (attente)", true, "Où attendent les joueurs."),
                    single("start", "Départ", true, "Position de départ de tous les joueurs."),
                    list("checkpoints", "Points de contrôle (dans l'ordre)", false, "Ajoutez-les dans l'ordre du parcours."),
                    single("finish", "Arrivée", true, "Zone d'arrivée."))),

    BOAT_RACE("Course de bateau", true,
            "Course en bateau avec points de contrôle et tours. Le premier à finir les tours gagne.",
            List.of(
                    integer("max-private-games", "Parties privées simultanées maximum", 4, 0, 40, "0 = pas de limite."),
                    integer("public-laps", "Nombre de tours (parties publiques)", 3, 1, 40, "Fixe pour toutes les parties publiques."),
                    integer("laps", "Nombre de tours par défaut (parties privées)", 3, 1, 40, "L'hôte peut choisir jusqu'à 40 tours."),
                    text("boat-type", "Type de bateau", "OAK_BOAT", "Ex. OAK_BOAT, BIRCH_BOAT, CHERRY_BOAT."),
                    integer("points-win", "Points du 1er (2e = -1, 3e = -2)", 3, 0, 50, "Points attribués au podium."),
                    integer("points-checkpoint", "Points par point de contrôle atteint", 0, 0, 50, "Gagnés à chaque point de contrôle."),
                    integer("time-limit-seconds", "Temps limite (s)", 600, 30, 3600, "Fin de la course pour tout le monde."),
                    integer("countdown-seconds", "Compte à rebours (s)", 5, 0, 15, "Avant le départ."),
                    integer("checkpoint-radius", "Rayon des points de contrôle et de la ligne d'arrivée", 8, 1, 15, "En blocs (rayon de détection autour du point)."),
                    integer("void-y", "Hauteur de chute (Y absolu)", -64, -64, 320, "Sous cette hauteur : retour au dernier point de contrôle. -64 = automatique (juste sous l'arène)."),
                    integer("min-players", "Joueurs minimum", 2, 1, 16, "Pour lancer une partie publique."),
                    integer("max-players", "Joueurs maximum", 12, 1, 32, "Places sur la grille (la grille de départ doit contenir autant de positions)."),
                    integer("gather-seconds", "Attente avant lancement (s)", 20, 5, 180, "Partie publique."),
                    bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
            List.of(
                    single("stands", "Gradins (attente)", true, "Où attendent les joueurs."),
                    list("start-grid", "Grille de départ (une place par bateau)", true, "Ajoutez une position par participant."),
                    list("checkpoints", "Points de contrôle (dans l'ordre)", false, "Ajoutez-les dans l'ordre du circuit."),
                    single("finish", "Ligne d'arrivée", true, "Franchie à chaque tour."),
                    single("finish-pit", "Ligne d'arrivée : 2e point (pitstop)", false, "Optionnel : franchir l'un ou l'autre des deux points de la ligne d'arrivée valide le tour."))),

    RUSH("Rush", true,
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
            RushLayout.points()),

    HUNGER_GAMES("Hunger Games", false,
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
            List.of(new ItemListSpec("loot", "Butin des coffres"))),

    MANHUNT("Manhunt", false,
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
            List.of()),

    BUILD_BATTLE("Build Battle", false,
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
            List.of());

    /** Liste d'objets (butin...) editee depuis l'inventaire du moderateur. */
    public record ItemListSpec(String key, String label) {
    }

    private final String display;
    private final boolean playable;
    private final String description;
    private final List<SettingSpec> settings;
    private final List<PointSpec> points;
    private final List<ItemListSpec> itemLists;

    MinigameType(String display, boolean playable, String description, List<SettingSpec> settings, List<PointSpec> points) {
        this(display, playable, description, settings, points, List.of());
    }

    MinigameType(String display, boolean playable, String description, List<SettingSpec> settings,
                 List<PointSpec> points, List<ItemListSpec> itemLists) {
        this.display = display;
        this.playable = playable;
        this.description = description;
        this.settings = settings;
        this.points = points;
        this.itemLists = itemLists;
    }

    public String display() {
        return display;
    }

    /** Vrai si un moteur de jeu existe (sinon : configuration seulement). */
    public boolean playable() {
        return playable;
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
