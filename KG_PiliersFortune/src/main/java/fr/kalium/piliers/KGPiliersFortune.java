package fr.kalium.piliers;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.kalium.games.model.PointSpec.list;
import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;

/**
 * KG_PiliersFortune : « Les piliers de la Fortune » de Kal-Games (demande de Maxster33, 06/10/2026), un plugin par jeu
 * (REGLES.md, regle 2.2), comme KG_Pong et KG_HideAndSeek. S'appuie sur le moteur de parties de KalGames (arenes
 * pre-generees, files d'attente, parties publiques et privees, blocs et objets au sol remis en etat a la fin) : ce plugin
 * enregistre le type « PILIERS_FORTUNE » (reglages, points d'arene, moteur) et suit les coups entre joueurs (credit des
 * chutes). Textes : lang.yml de KalGames, cles « pf.* ».
 */
public final class KGPiliersFortune extends JavaPlugin implements Listener {

    private static KGPiliersFortune instance;

    private KalGames games;
    private final RandomItems items = new RandomItems();

    public static KGPiliersFortune get() {
        return instance;
    }

    RandomItems items() {
        return items;
    }

    @Override
    public void onEnable() {
        instance = this;
        games = (KalGames) getServer().getPluginManager().getPlugin("KalGames");
        getServer().getPluginManager().registerEvents(this, this);

        MinigameType type = new MinigameType("PILIERS_FORTUNE", "Les piliers de la Fortune",
                "Chacun sur son pilier, un objet au hasard toutes les 5 secondes : faites tomber les autres. Tomber, c'est être éliminé.",
                List.of(
                        integer("min-players", "Joueurs minimum", 4, 2, 8, "Pour lancer une partie (publique ou privée)."),
                        integer("max-players", "Joueurs maximum", 8, 2, 8, "Limité au nombre de piliers de l'arène."),
                        integer("duration-seconds", "Durée maximale (s)", 600, 60, 3600, "Fin de la partie si plusieurs joueurs sont encore en vie : égalité."),
                        integer("countdown-seconds", "Décompte de départ (s)", 5, 1, 30, "Les joueurs restent immobiles sur leur pilier pendant ce temps."),
                        integer("item-interval-seconds", "Objet aléatoire toutes les (s)", 5, 1, 60, "Chaque joueur en vie reçoit un objet tiré au hasard ; inventaire plein : l'objet tombe au sol."),
                        integer("void-y", "Couche d'élimination", -64, -128, 320, "Un joueur qui tombe en dessous de cette hauteur est éliminé."),
                        integer("kill-credit-seconds", "Chute créditée pendant (s)", 10, 1, 60, "Un joueur éliminé est compté au dernier joueur qui l'a frappé dans ce délai."),
                        integer("points-minute", "Points par minute en vie", 1, 0, 50, "À chaque minute complète passée en vie."),
                        integer("points-kill", "Points par joueur éliminé", 5, 0, 50, "Tué ou tombé après un coup."),
                        integer("winner-multiplier", "Multiplicateur du gagnant", 3, 1, 10, "Seul joueur en vie à la fin. Les éliminés : x1 pour le premier, x2 pour le deuxième..."),
                        integer("public-gather-seconds", "Attente avant lancement (s)", 20, 3, 180, "Partie publique : délai dès que le minimum de joueurs est atteint."),
                        integer("end-delay-seconds", "Délai après la fin (s)", 8, 1, 30, "Avant le retour au hub."),
                        integer("prewarm-arenas", "Arènes préchargées", 8, 0, 10, "Copies de chaque arène, collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
                        integer("max-private-games", "Parties privées simultanées maximum", 0, 0, 40, "0 = pas de limite."),
                        bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
                List.of(
                        single("stands", "Zone d'attente", true, "Où attendent les joueurs avant la partie."),
                        list("pillars", "Piliers (départs)", true, "Un point par pilier, debout au sommet du pilier en pierre. Un joueur par pilier, tirés au hasard : 8 piliers pour 8 joueurs.")))
                .engine(PiliersInstance::new)
                .prewarmAllowed(true)
                .createForm(new CreateForm());
        MinigameType.register(type);
        getLogger().info("Les piliers de la Fortune enregistrés auprès de KalGames.");
    }

    /**
     * A l'arret, Paper desactive ce plugin AVANT KalGames : les parties en cours sont fermees ici, tant que le code de ce
     * plugin est encore disponible (comme KG_Pong).
     */
    @Override
    public void onDisable() {
        if (games != null && games.instances() != null) {
            for (GameInstance game : new ArrayList<>(games.instances().all())) {
                if (game instanceof PiliersInstance) {
                    games.instances().close(game, null);
                }
            }
        }
        instance = null;
    }

    private PiliersInstance gameOf(Player player) {
        return games.instances().of(player) instanceof PiliersInstance game ? game : null;
    }

    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    // ------------------------------------------------------------------ ecouteurs

    /** Dernier coup recu d'un autre joueur : sert a crediter une chute (« fait tomber »). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attackerOf(event.getDamager());
        PiliersInstance game = gameOf(victim);
        if (attacker != null && game != null) {
            game.noteHit(victim, attacker);
        }
    }

    /**
     * KalGames annule tout coup d'un joueur sur une creature : ici, les joueurs en vie peuvent frapper les creatures de
     * leur partie (celles des oeufs d'apparition).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHitMob(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player || !event.isCancelled()) {
            return;
        }
        Player attacker = attackerOf(event.getDamager());
        PiliersInstance game = attacker == null ? null : gameOf(attacker);
        if (game != null && game.canHitMob(attacker, event.getEntity())) {
            event.setCancelled(false);
        }
    }

    // ------------------------------------------------------------------ creation d'une partie privee

    /** Formulaire de creation d'une partie privee : rien a choisir, a part l'arene. */
    private static final class CreateForm implements MinigameType.CreateForm {

        @Override
        public List<DialogInput> inputs(Minigame minigame, List<Arena> arenas, fr.kalium.games.gui.Gui gui) {
            return new ArrayList<>();
        }

        @Override
        public void read(io.papermc.paper.dialog.DialogResponseView view, Map<String, Object> options) {
        }
    }
}
