package fr.kalium.pong;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import fr.kalium.games.util.Items;
import io.papermc.paper.event.entity.SulfurCubeSwallowItemEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;
import static fr.kalium.games.model.SettingSpec.text;

/**
 * KG_Pong : le Pong de Kal-Games (demande de LeKiwi06, 06/10/2026), un plugin par jeu (REGLES.md, regle 2.2), comme
 * KG_HideAndSeek et KG_PvpKit. S'appuie sur le moteur de parties de KalGames (arenes, files d'attente, parties
 * publiques et privees) : ce plugin enregistre le type « PONG » (reglages, points d'arene, moteur) et ecoute les clics
 * des joueurs. Textes : lang.yml de KalGames, cles « pong.* ».
 */
public final class KGPong extends JavaPlugin implements Listener {

    private static KGPong instance;

    private KalGames games;
    private NamespacedKey ballKey;

    public static KGPong get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        games = (KalGames) getServer().getPluginManager().getPlugin("KalGames");
        ballKey = new NamespacedKey(this, "ball");
        getServer().getPluginManager().registerEvents(this, this);

        MinigameType type = new MinigameType("PONG", "Pong",
                "Pong à deux joueurs, vu de dessus : chacun déplace sa raquette pour renvoyer la balle. Le premier à 5 points gagne.",
                List.of(
                        integer("points-to-win", "Points pour gagner", 5, 1, 21, "Le premier joueur à ce score gagne la partie."),
                        integer("bar-length", "Raquette : longueur (blocs)", 5, 1, 15, "Réduite si le terrain est trop étroit."),
                        integer("bar-offset", "Raquette : distance du fond", 2, 1, 10, "Cases libres entre le fond du terrain et la raquette."),
                        integer("bar-speed", "Raquette : vitesse (blocs/s)", 10, 2, 20, "Clic droit maintenu."),
                        text("bar-block-1", "Raquette du joueur 1 : bloc", "blue_concrete", "Identifiant Minecraft du bloc."),
                        text("bar-block-2", "Raquette du joueur 2 : bloc", "red_concrete", "Identifiant Minecraft du bloc."),
                        integer("ball-speed", "Balle : vitesse de départ", 8, 2, 30, "En blocs par seconde, à chaque service."),
                        integer("ball-speed-gain", "Balle : gain par renvoi", 5, 0, 50, "En dixièmes de bloc par seconde : 5 = +0,5 bloc/s à chaque renvoi."),
                        integer("ball-speed-max", "Balle : vitesse maximale", 18, 2, 40, "En blocs par seconde."),
                        text("ball-block", "Balle : bloc avalé", "packed_ice", "Bloc visible dans le Sulfur Cube (glace compactée par défaut)."),
                        integer("serve-delay-seconds", "Délai avant le service (s)", 2, 0, 10, "Après chaque point, la balle attend au centre."),
                        integer("points-goal", "Points par but marqué", 1, 0, 50, "Points de classement pour chaque but marqué."),
                        integer("points-win", "Points du vainqueur", 3, 0, 50, "Points de classement ajoutés au gagnant (pas en cas d'abandon adverse)."),
                        integer("public-gather-seconds", "Attente avant lancement (s)", 10, 3, 180, "Partie publique : délai dès que deux joueurs sont là."),
                        integer("end-delay-seconds", "Délai après la fin (s)", 6, 1, 30, "Avant le retour au hub."),
                        integer("prewarm-arenas", "Arènes préchargées", 0, 0, 10, "Copies de chaque arène, collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
                        integer("max-private-games", "Parties privées simultanées maximum", 0, 0, 40, "0 = pas de limite."),
                        bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
                List.of(
                        single("stands", "Tribune", true, "Où attendent les joueurs avant la partie."),
                        single("corner-1", "Terrain : coin 1", true, "Debout sur le sol du terrain, dans un coin, à l'intérieur des murs. La raquette du joueur 1 est de ce côté."),
                        single("corner-2", "Terrain : coin 2", true, "Le coin opposé, à l'intérieur des murs. La raquette du joueur 2 est de ce côté."),
                        single("view-1", "Vue du joueur 1", true, "En l'air au-dessus du terrain : le joueur 1 y flotte et regarde vers le bas. La direction regardée en posant le point donne le haut de son écran."),
                        single("view-2", "Vue du joueur 2", true, "En l'air au-dessus du terrain : le joueur 2 y flotte et regarde vers le bas. La direction regardée en posant le point donne le haut de son écran.")))
                .engine(PongInstance::new)
                .prewarmAllowed(true)
                .createForm(new CreateForm());
        MinigameType.register(type);
        getLogger().info("Pong enregistré auprès de KalGames.");
    }

    /**
     * A l'arret, Paper desactive ce plugin AVANT KalGames : les parties de Pong en cours sont fermees ici, tant que le
     * code de ce plugin est encore disponible (comme KG_HideAndSeek).
     */
    @Override
    public void onDisable() {
        if (games != null && games.instances() != null) {
            for (GameInstance game : new ArrayList<>(games.instances().all())) {
                if (game instanceof PongInstance) {
                    games.instances().close(game, null);
                }
            }
        }
        instance = null;
    }

    private PongInstance gameOf(Player player) {
        return games.instances().of(player) instanceof PongInstance game ? game : null;
    }

    // ------------------------------------------------------------------ balle et objets

    void markBall(Entity entity) {
        entity.getPersistentDataContainer().set(ballKey, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean isBall(Entity entity) {
        return entity.getPersistentDataContainer().has(ballKey, PersistentDataType.BYTE);
    }

    /** Objet tenu en main : il montre le sens choisi (clic gauche pour l'inverser, clic droit pour bouger). */
    ItemStack paddle(boolean up) {
        return Items.named(up ? Material.LIME_DYE : Material.RED_DYE,
                up ? games.t("pong.item-up", "<green><bold>Sens : monter") : games.t("pong.item-down", "<red><bold>Sens : descendre"),
                List.of(games.t("pong.item-lore1", "<gray>Clic gauche : changer de sens."),
                        games.t("pong.item-lore2", "<gray>Clic droit : déplacer la raquette."),
                        games.t("pong.item-lore3", "<gray>Clic droit maintenu : en continu.")));
    }

    // ------------------------------------------------------------------ ecouteurs

    /** Clic gauche dans le vide : en mode aventure, le jeu n'envoie que le mouvement de bras. */
    @EventHandler
    public void onSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        PongInstance game = gameOf(event.getPlayer());
        if (game != null) {
            game.leftClick(event.getPlayer());
        }
    }

    /** Clic gauche : changer de sens ; clic droit : deplacer la raquette. Passe avant l'ecouteur de KalGames. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        PongInstance game = gameOf(player);
        if (game == null || !game.controls(player.getUniqueId())) {
            return;
        }
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            game.leftClick(player);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            game.rightClick(player);
        }
    }

    /** Un joueur en partie flotte au-dessus du terrain : il ne peut pas couper son vol. */
    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        PongInstance game = gameOf(event.getPlayer());
        if (game != null && !event.isFlying() && game.frozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** La balle ne peut etre ni tondue, ni ramassee au seau, ni nourrie d'un autre bloc, ni blessee. */
    @EventHandler(ignoreCancelled = true)
    public void onBallInteract(PlayerInteractEntityEvent event) {
        if (isBall(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBallSwallow(SulfurCubeSwallowItemEvent event) {
        if (isBall(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBallDamage(EntityDamageEvent event) {
        if (isBall(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ creation d'une partie privee

    /** Formulaire de creation d'une partie privee : rien a choisir (toujours 2 joueurs), a part l'arene. */
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
