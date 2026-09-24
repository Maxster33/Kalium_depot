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
                        integer("points-win", "Points du 1er (2e = -1, 3e = -2)", 3, 0, 50, "Points attribués au podium."),
                        integer("points-checkpoint", "Points par point de contrôle atteint", 0, 0, 50, "Gagnés à chaque point de contrôle."),
                        integer("record-max-lap-seconds", "Meilleurs temps : tours enregistrés jusqu'à (s)", 45, 0, 600,
                                "1.1.0 : un tour plus long n'entre pas dans les meilleurs temps. 0 = tous les tours."),
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
