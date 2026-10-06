package fr.kalium.piliers;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.Material;
import org.bukkit.block.data.Directional;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
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
import java.util.HashMap;
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
    /** Contenu d'un distributeur ou d'un dropper a son ouverture, par joueur. */
    private final Map<UUID, Map<Material, Integer>> opened = new HashMap<>();

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
                        integer("min-players", "Joueurs minimum", 3, 2, 8, "Pour lancer une partie (publique ou privée)."),
                        integer("max-players", "Joueurs maximum", 8, 2, 8, "Limité au nombre de piliers de l'arène."),
                        integer("duration-seconds", "Durée maximale (s)", 600, 60, 3600, "Fin de la partie si plusieurs joueurs sont encore en vie : égalité."),
                        integer("countdown-seconds", "Décompte de départ (s)", 5, 1, 30, "Les joueurs restent immobiles sur leur pilier pendant ce temps."),
                        integer("item-interval-seconds", "Objet aléatoire toutes les (s)", 5, 1, 60, "Chaque joueur en vie reçoit un objet tiré au hasard ; inventaire plein : l'objet tombe au sol."),
                        integer("void-y", "Couche d'élimination", -64, -128, 320, "Un joueur qui tombe en dessous de cette hauteur est éliminé."),
                        integer("water-credit-seconds", "Eau créditée pendant (s)", 10, 0, 60, "L'eau posée par un joueur ne lui fait créditer une chute que pendant ce délai après la pose de la source."),
                        integer("kill-credit-seconds", "Chute créditée pendant (s)", 10, 1, 60, "Un joueur éliminé est compté au dernier joueur qui l'a frappé (ou dont l'explosif, le feu, la lave ou l'eau l'a touché) dans ce délai."),
                        integer("time-interval-seconds", "Points de temps toutes les (s)", 30, 5, 600, "Chaque joueur en vie gagne des points de temps à cet intervalle."),
                        integer("points-time-hundredths", "Points de temps (centièmes)", 25, 0, 1000, "Points gagnés à chaque intervalle en vie, en centièmes : 25 = 0,25 point."),
                        integer("points-kill", "Points par joueur éliminé", 7, 0, 50, "Tué ou tombé à cause d'un autre joueur."),
                        integer("winner-multiplier-tenths", "Bonus du gagnant (dixièmes)", 15, 10, 50, "Multiplie en plus le score du dernier joueur en vie s'il gagne avant la fin du temps : 15 = x1,5."),
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

    /** Pas de source d'eau ni de lave versee contre une barriere (demande de Maxster33, 06/10/2026). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketAgainstBarrier(PlayerBucketEmptyEvent event) {
        if (event.getBlockClicked().getType() == Material.BARRIER && event.getBucket() != Material.POWDER_SNOW_BUCKET
                && gameOf(event.getPlayer()) != null) {
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
            game.copyOwner(event.getBlock(), event.getToBlock());
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

    /** Bloc casse sous les pieds d'un joueur : la chute est creditee a celui qui l'a casse (demande de Maxster33). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakUnderFeet(BlockBreakEvent event) {
        Player breaker = event.getPlayer();
        PiliersInstance game = gameOf(breaker);
        if (game == null || !game.running()) {
            return;
        }
        Block block = event.getBlock();
        for (Player player : game.alivePlayers()) {
            if (!player.equals(breaker) && standsOn(player, block)) {
                game.noteHit(player, breaker.getUniqueId(), Component.text("bloc cassé sous ses pieds"));
            }
        }
    }

    /** Le joueur se tient sur ce bloc (n'importe quelle case sous ses pieds, il peut etre a cheval sur plusieurs). */
    private static boolean standsOn(Player player, Block block) {
        if (player.getWorld() != block.getWorld()) {
            return false;
        }
        BoundingBox box = player.getBoundingBox();
        return block.getY() == (int) Math.floor(box.getMinY() - 0.05)
                && block.getX() >= (int) Math.floor(box.getMinX()) && block.getX() <= (int) Math.floor(box.getMaxX() - 1.0E-6)
                && block.getZ() >= (int) Math.floor(box.getMinZ()) && block.getZ() <= (int) Math.floor(box.getMaxZ() - 1.0E-6);
    }

    /** Oeuf d'apparition utilise par un joueur en vie : la creature qui apparait est a lui (creeper). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEggUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.getType().name().endsWith("_SPAWN_EGG") || event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        PiliersInstance game = gameOf(event.getPlayer());
        if (game != null && game.running() && game.isAlive(event.getPlayer())) {
            game.noteEgg(event.getPlayer());
        }
    }

    // Distributeurs et droppers : l'objet envoye revient a celui qui l'a mis dedans (demande de Maxster33).

    private static boolean isLauncher(Inventory inventory) {
        return inventory.getType() == InventoryType.DISPENSER || inventory.getType() == InventoryType.DROPPER;
    }

    private static Map<Material, Integer> count(Inventory inventory) {
        Map<Material, Integer> counts = new HashMap<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                counts.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return counts;
    }

    /** Contenu du distributeur a l'ouverture, compare a la fermeture : ce que le joueur y a ajoute est a lui. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLauncherOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isLauncher(event.getInventory()) && gameOf(player) != null) {
            opened.put(player.getUniqueId(), count(event.getInventory()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLauncherClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Map<Material, Integer> before = opened.remove(player.getUniqueId());
        Location location = event.getInventory().getLocation();
        PiliersInstance game = gameOf(player);
        if (before == null || location == null || game == null || !game.running() || !isLauncher(event.getInventory())) {
            return;
        }
        for (Map.Entry<Material, Integer> entry : count(event.getInventory()).entrySet()) {
            if (entry.getValue() > before.getOrDefault(entry.getKey(), 0)) {
                game.noteLoaded(location.getBlock(), entry.getKey(), player.getUniqueId());
            }
        }
    }

    /** Un dropper (ou un entonnoir) qui remplit un distributeur transmet le joueur de l'objet. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLauncherFeed(InventoryMoveItemEvent event) {
        Location from = event.getSource().getLocation();
        Location to = event.getDestination().getLocation();
        if (from == null || to == null || !isLauncher(event.getDestination())) {
            return;
        }
        PiliersInstance game = runningAt(to.getBlock());
        if (game != null) {
            game.noteLoaded(to.getBlock(), event.getItem().getType(), game.loader(from.getBlock(), event.getItem().getType()));
        }
    }

    /** Envoi : l'entite qui apparait devant (fleche, TNT, boule de feu...), la lave ou l'eau versee sont au joueur. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        PiliersInstance game = runningAt(block);
        if (game == null || !(block.getBlockData() instanceof Directional directional)) {
            return;
        }
        UUID owner = game.loader(block, event.getItem().getType());
        if (owner == null) {
            return;
        }
        Block front = block.getRelative(directional.getFacing());
        game.notePending(front, owner);
        game.notePending(block, owner);
        Material type = event.getItem().getType();
        if (type == Material.WATER_BUCKET || type == Material.LAVA_BUCKET || type.name().endsWith("_BUCKET")
                && type != Material.BUCKET && type != Material.MILK_BUCKET && type != Material.POWDER_SNOW_BUCKET) {
            game.owner(front, owner);
        }
    }

    /** Entite apparue : envoyee par un distributeur, ou creature d'un oeuf d'apparition. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        PiliersInstance game = runningAt(entity.getLocation().getBlock());
        if (game == null) {
            return;
        }
        UUID owner = game.pending(entity.getLocation().getBlock());
        if (owner == null && event instanceof CreatureSpawnEvent spawn
                && spawn.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            owner = game.eggUser(entity.getLocation());
        }
        game.entityOwner(entity, owner);
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
            // « coup » au corps a corps ; sinon le nom du projectile (« Flèche », « Boule de neige »...).
            game.noteHit(victim, attacker.getUniqueId(), damager instanceof Player ? Component.text("coup") : damager.name());
        } else {
            // Explosif pose, creature sortie d'un oeuf (toutes, demande de Maxster33), projectile ou potion envoyes par
            // un distributeur : le joueur a qui l'entite est rattachee. Nom traduit de l'entite (« TNT », « Creeper »...).
            UUID owner = explosiveOwner(game, damager);
            Component how = damager.name();
            if (damager instanceof Projectile projectile) {
                if (owner == null && projectile.getShooter() instanceof Entity shooter) {
                    owner = game.entityOwner(shooter); // fleche d'un squelette sorti d'un oeuf...
                    how = shooter.name().append(Component.text(" : ")).append(damager.name());
                } else if (projectile.getShooter() instanceof BlockProjectileSource) {
                    how = damager.name().append(Component.text(" (distributeur)"));
                }
            }
            game.noteHit(victim, owner, how);
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
            case BLOCK_EXPLOSION -> game.noteHit(victim, game.owner(block), Component.text("explosion"));
            case FIRE, CAMPFIRE -> game.noteBurn(victim, game.owner(block), Component.text("feu"));
            case LAVA -> game.noteBurn(victim, game.owner(block), Component.text("lave"));
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
        Entity igniter = event.getIgnitingEntity();
        Block source = event.getIgnitingBlock();
        if (event.getPlayer() != null) {
            owner = event.getPlayer().getUniqueId();
        } else if (igniter != null && attackerOf(igniter) != null) {
            owner = attackerOf(igniter).getUniqueId();
        } else if (igniter != null && game.entityOwner(igniter) != null) {
            owner = game.entityOwner(igniter); // boule de feu envoyee par un distributeur
        } else if (source != null && (source.getType() == Material.DISPENSER || game.pending(event.getBlock()) != null)) {
            owner = game.pending(event.getBlock()); // briquet dans un distributeur
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
