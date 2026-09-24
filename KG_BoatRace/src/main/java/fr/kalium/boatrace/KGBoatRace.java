package fr.kalium.boatrace;

import fr.kalium.games.model.MinigameType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static fr.kalium.games.model.PointSpec.list;
import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;
import static fr.kalium.games.model.SettingSpec.text;

/**
 * KG_BoatRace : la course de bateau de Kal-Games, sortie de KalGames (REGLES.md, regle 2.2 ; cahier des charges dans
 * CAHIER_DES_CHARGES.md). S'appuie sur le moteur de parties de KalGames (arenes, files d'attente, parties publiques et
 * privees) : ce plugin n'enregistre que le type « BOAT_RACE » (reglages, points d'arene, moteur).
 *
 * Le nom du type ne change pas (« BOAT_RACE ») : les mini-jeux et arenes deja configures dans KalGames
 * (minigames.yml, arenas.yml) sont repris tels quels, ainsi que les classements (meme identifiant de mini-jeu).
 */
public final class KGBoatRace extends JavaPlugin {

    @Override
    public void onEnable() {
        MinigameType type = new MinigameType("BOAT_RACE", "Course de bateau",
                "Course en bateau avec points de contrôle et tours. Le premier à finir les tours gagne.",
                List.of(
                        integer("max-private-games", "Parties privées simultanées maximum", 4, 0, 40, "0 = pas de limite."),
                        integer("public-laps", "Nombre de tours (parties publiques)", 3, 1, 40, "Fixe pour toutes les parties publiques."),
                        integer("laps", "Nombre de tours par défaut (parties privées)", 3, 1, 40, "L'hôte peut choisir jusqu'à 40 tours."),
                        text("boat-type", "Type de bateau", "OAK_BOAT", "Ex. OAK_BOAT, BIRCH_BOAT, CHERRY_BOAT."),
                        // 1.3.0 : bareme (remplace les points du podium et par checkpoint). Coefficients en dixiemes : 15 = x1,5.
                        integer("points-lap", "Barème : points par tour", 1, 0, 50, "Avant les multiplicateurs."),
                        integer("points-clean-lap", "Barème : bonus tour sans hors-piste", 1, 0, 50, "Avant les multiplicateurs."),
                        integer("series-length", "Barème : tours propres d'affilée pour une série", 3, 1, 10,
                                "Chaque série validée rapporte +n (n = numéro de la série d'affilée) ; un hors-piste remet à zéro."),
                        integer("tier1-seconds", "Chrono palier 1 : tour en moins de (s)", 45, 0, 600, "0 = palier désactivé."),
                        integer("tier1-coef-x10", "Chrono palier 1 : coefficient x10", 15, 10, 100, "15 = x1,5."),
                        integer("tier2-seconds", "Chrono palier 2 : tour en moins de (s)", 40, 0, 600, "0 = palier désactivé."),
                        integer("tier2-coef-x10", "Chrono palier 2 : coefficient x10", 20, 10, 100, "20 = x2."),
                        integer("tier3-seconds", "Chrono palier 3 : tour en moins de (s)", 35, 0, 600, "0 = palier désactivé."),
                        integer("tier3-coef-x10", "Chrono palier 3 : coefficient x10", 30, 10, 100, "30 = x3."),
                        integer("tier4-seconds", "Chrono palier 4 : tour en moins de (s)", 30, 0, 600, "0 = palier désactivé."),
                        integer("tier4-coef-x10", "Chrono palier 4 : coefficient x10", 50, 10, 100, "50 = x5."),
                        integer("lead-coef-x10", "Tour en tête : coefficient x10", 15, 10, 100,
                                "Tous les checkpoints du tour passés en 1er. Coefficients additifs : x1,5 et x1,5 = x2."),
                        integer("gp-laps", "Grand Prix : nombre de tours", 40, 0, 40, "Course de ce nombre de tours terminée : bonus sur le total. 0 = jamais."),
                        integer("gp-coef-x10", "Grand Prix : coefficient x10", 15, 10, 100, "15 = x1,5 sur le total de la course."),
                        text("track-blocks", "Blocs de piste (hors-piste = tout autre bloc touché)", "PACKED_ICE,BLUE_ICE",
                                "Noms de blocs séparés par des virgules."),
                        bool("anti-collision", "Anti-collision entre bateaux", true,
                                "Les adversaires sont remplacés par des copies sans collision (joueurs Java ; Bedrock : vrais bateaux)."),
                        integer("record-max-lap-seconds", "Meilleurs temps : tours enregistrés jusqu'à (s)", 45, 0, 600,
                                "1.1.0 : un tour plus long n'entre pas dans les meilleurs temps. 0 = tous les tours."),
                        integer("time-limit-seconds", "Temps limite (s)", 600, 30, 3600, "Fin de la course pour tout le monde."),
                        integer("countdown-seconds", "Compte à rebours (s)", 5, 0, 15, "Avant le départ."),
                        integer("checkpoint-radius", "Rayon des points de contrôle et de la ligne d'arrivée", 8, 1, 15, "En blocs (rayon de détection autour du point)."),
                        integer("void-y", "Hauteur de chute (Y absolu)", -64, -64, 320, "Sous cette hauteur : retour au dernier point de contrôle. -64 = automatique (juste sous l'arène)."),
                        integer("min-players", "Joueurs minimum", 2, 1, 16, "Pour lancer une partie publique."),
                        integer("max-players", "Joueurs maximum", 12, 1, 32, "Places sur la grille (la grille de départ doit contenir autant de positions)."),
                        integer("gather-seconds", "Attente avant lancement (s)", 20, 5, 180, "Partie publique."),
                        bool("live-ranking", "Classement en direct (tableau latéral)", true,
                                "1.2.0 : classement de la course et écarts avec le premier, mis à jour chaque seconde."),
                        bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
                List.of(
                        single("stands", "Gradins (attente)", true, "Où attendent les joueurs."),
                        list("start-grid", "Grille de départ (une place par bateau)", true, "Ajoutez une position par participant."),
                        list("checkpoints", "Points de contrôle (dans l'ordre)", false, "Ajoutez-les dans l'ordre du circuit."),
                        single("finish", "Ligne d'arrivée", true, "Franchie à chaque tour."),
                        single("finish-pit", "Ligne d'arrivée : 2e point (pitstop)", false, "Optionnel : franchir l'un ou l'autre des deux points de la ligne d'arrivée valide le tour.")))
                .engine(BoatRaceInstance::new)
                .ranking(fr.kalium.scoreboards.Category.Kind.LAP)
                .createOption(new MinigameType.CreateOption("laps", "Nombre de tours (1 à 40)", 1, BoatRaceInstance.MAX_LAPS,
                        "laps", BoatRaceInstance.DEFAULT_PUBLIC_LAPS))
                .prewarmAllowed(false);
        MinigameType.register(type);
        getLogger().info("Course de bateau enregistrée auprès de KalGames.");
    }

    /**
     * A l'arret, Paper desactive ce plugin AVANT KalGames (qui en depend dans l'autre sens) : les courses en cours sont
     * fermees ici, tant que le code de ce plugin est encore disponible, plutot que par KalGames juste apres.
     */
    @Override
    public void onDisable() {
        fr.kalium.games.KalGames games = (fr.kalium.games.KalGames) getServer().getPluginManager().getPlugin("KalGames");
        if (games == null || games.instances() == null) {
            return;
        }
        for (fr.kalium.games.game.GameInstance instance : new java.util.ArrayList<>(games.instances().all())) {
            if (instance instanceof BoatRaceInstance) {
                games.instances().close(instance, null);
            }
        }
    }
}
