package fr.kalium.parkour;

import fr.kalium.games.model.MinigameType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static fr.kalium.games.model.PointSpec.list;
import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;

/**
 * KG_Parkour : le Parcours de Kal-Games, sorti de KalGames (REGLES.md, regle 2.2 ; cahier des charges dans
 * CAHIER_DES_CHARGES.md), comme la course de bateau (KG_BoatRace). S'appuie sur le moteur de parties de KalGames
 * (arenes, files d'attente, parties publiques et privees) : ce plugin n'enregistre que le type « PARKOUR » (reglages,
 * points d'arene, moteur, option d'entrainement).
 *
 * Le nom du type, les reglages et les points d'arene ne changent pas : les mini-jeux et arenes deja configures dans
 * KalGames (minigames.yml, arenas.yml) sont repris tels quels, ainsi que les classements (meme identifiant de mini-jeu).
 */
public final class KGParkour extends JavaPlugin {

    @Override
    public void onEnable() {
        MinigameType type = new MinigameType("PARKOUR", "Parcours",
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
                        single("finish", "Arrivée", true, "Zone d'arrivée.")))
                .engine(ParkourInstance::new)
                .ranking(fr.kalium.scoreboards.Category.Kind.TIME)
                .createToggle(new MinigameType.CreateToggle("training",
                        "Mode entraînement (sans limite de temps, sans points ni classement)"))
                .prewarmAllowed(true);
        MinigameType.register(type);
        getLogger().info("Parcours enregistré auprès de KalGames.");
    }

    /**
     * A l'arret, Paper desactive ce plugin AVANT KalGames (qui en depend dans l'autre sens) : les parcours en cours sont
     * fermes ici, tant que le code de ce plugin est encore disponible, plutot que par KalGames juste apres.
     */
    @Override
    public void onDisable() {
        fr.kalium.games.KalGames games = (fr.kalium.games.KalGames) getServer().getPluginManager().getPlugin("KalGames");
        if (games == null || games.instances() == null) {
            return;
        }
        for (fr.kalium.games.game.GameInstance instance : new java.util.ArrayList<>(games.instances().all())) {
            if (instance instanceof ParkourInstance) {
                games.instances().close(instance, null);
            }
        }
    }
}
