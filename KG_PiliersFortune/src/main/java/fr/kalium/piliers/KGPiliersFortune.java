package fr.kalium.piliers;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fr.kalium.games.model.PointSpec.list;
import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;

/**
 * KG_PiliersFortune : « Les piliers de la Fortune » de Kal-Games (demande de Maxster33, 06/10/2026), un plugin par jeu
 * (REGLES.md, regle 2.2), comme KG_Pong et KG_HideAndSeek. S'appuie sur le moteur de parties de KalGames (arenes
 * pre-generees, files d'attente, parties publiques et privees, blocs et objets au sol remis en etat a la fin) : ce plugin
 * enregistre le type « PILIERS_FORTUNE » (reglages, points d'arene, moteur) et ecoute :
 * <ul>
 *   <li>les coups entre joueurs, et le poseur des explosifs, du feu, de la lave et de l'eau (credit des eliminations) ;</li>
 *   <li>le feu et les pistons, que KalGames bloque dans tout le monde des parties : autorises ici, pendant une partie
 *       de ce jeu et dans son arene, chaque bloc touche etant suivi pour la remise en etat ;</li>
 *   <li>la pose d'un bloc contre une barriere, refusee.</li>
 * </ul>
 * Textes : lang.yml de KalGames, cles « pf.* ».
 */
public final class KGPiliersFortune extends JavaPlugin implements Listener {

    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST};

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
                        integer("kill-credit-seconds", "Chute créditée pendant (s)", 10, 1, 60, "Un joueur éliminé est compté au dernier joueur qui l'a frappé (ou dont l'explosif, le feu, la lave ou l'eau l'a touché) dans ce délai."),
                        integer("points-minute", "Points par minute en vie", 1, 0, 50, "À chaque minute complète passée en vie."),
                        integer("points-kill", "Points par joueur éliminé", 5, 0, 50, "Tué ou tombé à cause d'un autre joueur."),
                        integer("winner-multiplier", "Multiplicateur du gagnant", 3, 1, 10, "Seul joueur en vie à la fin. Les éliminés : x1 pour le premier, x2 pour le deuxième..."),
                        integer("points-divisor", "Diviseur des points", 3, 1, 10, "Le score final (après multiplicateur) est divisé par ce nombre avant d'être crédité."),
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

    /** Partie de ce jeu en cours (phase de jeu) dont l'arene contient ce bloc, sinon null. */
    private PiliersInstance runningAt(Block block) {
        if (block == null || block.getWorld() != games.worlds().world()) {
            return null;
        }
        return games.instances().at(block.getLocation()) instanceof PiliersInstance game && game.running()
                && game.inBounds(block.getX(), block.getY(), block.getZ()) ? game : null;
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

    private static boolean isFire(Material material) {
        return material == Material.FIRE || material == Material.SOUL_FIRE;
    }

    // ------------------------------------------------------------------ barrieres

    /** Aucun bloc ne peut etre pose contre une barriere (demande de Maxster33, 06/10/2026). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlaceAgainstBarrier(BlockPlaceEvent event) {
        if (event.getBlockAgainst().getType() == Material.BARRIER && gameOf(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ poseurs

    /** Poseur de chaque bloc (TNT, lit, ancre de reapparition...). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        PiliersInstance game = gameOf(event.getPlayer());
        if (game == null || !game.running()) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        game.owner(event.getBlock(), uuid);
        if (event instanceof BlockMultiPlaceEvent multi) {
            for (BlockState state : multi.getReplacedBlockStates()) {
                game.owner(state.getBlock(), uuid);
            }
        }
    }

    /** Source de lave ou d'eau posee au seau. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        PiliersInstance game = gameOf(event.getPlayer());
        if (game != null && game.running()) {
            game.owner(event.getBlock(), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        PiliersInstance game = gameOf(event.getPlayer());
        if (game != null && game.running()) {
            game.owner(event.getBlock(), null);
        }
    }

    /** La lave et l'eau qui coulent gardent le poseur de leur source. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        PiliersInstance game = runningAt(event.getToBlock());
        if (game != null) {
            game.owner(event.getToBlock(), game.owner(event.getBlock()));
        }
    }

    /** Cristal de l'End, wagonnet de TNT. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        PiliersInstance game = player == null ? null : gameOf(player);
        if (game != null && game.running()) {
            game.entityOwner(event.getEntity(), player.getUniqueId());
        }
    }

    /** Poseur d'un explosif devenu entite : TNT amorcee (poseur du bloc de TNT), cristal, wagonnet de TNT. */
    private UUID explosiveOwner(PiliersInstance game, Entity damager) {
        UUID owner = game.entityOwner(damager);
        if (owner == null && damager instanceof TNTPrimed tnt && tnt.getOrigin() != null) {
            owner = game.owner(tnt.getOrigin().getBlock());
        }
        return owner;
    }

    // ------------------------------------------------------------------ credit des eliminations

    /** Coup d'un joueur, ou explosion d'un explosif pose par un joueur. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        PiliersInstance game = gameOf(victim);
        if (game == null) {
            return;
        }
        Entity damager = event.getDamager();
        Player attacker = attackerOf(damager);
        if (attacker != null) {
            game.noteHit(victim, attacker.getUniqueId());
        } else if (damager instanceof TNTPrimed || damager instanceof EnderCrystal || damager instanceof ExplosiveMinecart) {
            game.noteHit(victim, explosiveOwner(game, damager));
        }
    }

    /** Explosion d'un bloc (lit, ancre de reapparition), feu, feu de camp, lave : credit au poseur. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHitByBlock(EntityDamageByBlockEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        PiliersInstance game = gameOf(victim);
        if (game == null) {
            return;
        }
        Block block = event.getDamager();
        if (block == null && event.getDamagerBlockState() != null) {
            block = event.getDamagerBlockState().getBlock();
        }
        switch (event.getCause()) {
            case BLOCK_EXPLOSION -> game.noteHit(victim, game.owner(block));
            case FIRE, LAVA, CAMPFIRE -> game.noteBurn(victim, game.owner(block));
            default -> {
            }
        }
    }

    /** Le joueur brule encore apres le feu ou la lave : le poseur reste credite. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireTick(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK && event.getEntity() instanceof Player victim) {
            PiliersInstance game = gameOf(victim);
            if (game != null) {
                game.noteFireTick(victim);
            }
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

    // ------------------------------------------------------------------ feu (debloque, demande de Maxster33)

    /** Depart de feu (briquet, boule de feu, lave...) : autorise et suivi ; le feu garde le poseur de sa source. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onIgnite(BlockIgniteEvent event) {
        PiliersInstance game = runningAt(event.getBlock());
        if (game == null) {
            return;
        }
        event.setCancelled(false);
        game.track(event.getBlock());
        UUID owner = null;
        if (event.getPlayer() != null) {
            owner = event.getPlayer().getUniqueId();
        } else if (event.getIgnitingEntity() != null && attackerOf(event.getIgnitingEntity()) != null) {
            owner = attackerOf(event.getIgnitingEntity()).getUniqueId();
        } else if (event.getIgnitingBlock() != null) {
            owner = game.owner(event.getIgnitingBlock());
        }
        game.owner(event.getBlock(), owner);
    }

    /** Propagation du feu. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpread(BlockSpreadEvent event) {
        if (!isFire(event.getNewState().getType())) {
            return;
        }
        PiliersInstance game = runningAt(event.getBlock());
        if (game == null) {
            return;
        }
        event.setCancelled(false);
        game.track(event.getBlock());
        game.owner(event.getBlock(), game.owner(event.getSource()));
    }

    /** Bloc detruit par le feu (et ce qui y est accroche). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBurn(BlockBurnEvent event) {
        PiliersInstance game = runningAt(event.getBlock());
        if (game == null) {
            return;
        }
        event.setCancelled(false);
        game.track(event.getBlock());
        for (BlockFace face : FACES) {
            game.track(event.getBlock().getRelative(face));
        }
    }

    /** Le feu s'eteint (KalGames empeche sinon tout bloc de disparaitre, le feu ne s'eteindrait jamais). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFade(BlockFadeEvent event) {
        if (!isFire(event.getBlock().getType())) {
            return;
        }
        PiliersInstance game = runningAt(event.getBlock());
        if (game != null) {
            event.setCancelled(false);
            game.track(event.getBlock());
        }
    }

    // ------------------------------------------------------------------ pistons (debloques, demande de Maxster33)

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        allowPiston(event, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        allowPiston(event, event.getBlocks());
    }

    /**
     * Autorise le piston si toutes les cases touchees (piston, tete, blocs pousses ou tires et leur arrivee) sont dans
     * l'arene d'une partie en cours ; chaque case est suivie pour la remise en etat.
     */
    private void allowPiston(BlockPistonEvent event, List<Block> moved) {
        Block piston = event.getBlock();
        PiliersInstance game = runningAt(piston);
        if (game == null) {
            return;
        }
        BlockFace direction = event.getDirection();
        List<Block> touched = new ArrayList<>();
        touched.add(piston);
        touched.add(piston.getRelative(direction));
        touched.add(piston.getRelative(direction.getOppositeFace()));
        for (Block block : moved) {
            touched.add(block);
            touched.add(block.getRelative(direction));
            touched.add(block.getRelative(direction.getOppositeFace()));
        }
        for (Block block : touched) {
            if (!game.inBounds(block.getX(), block.getY(), block.getZ())) {
                return; // hors de l'arene : reste bloque
            }
        }
        event.setCancelled(false);
        for (Block block : touched) {
            game.track(block);
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
