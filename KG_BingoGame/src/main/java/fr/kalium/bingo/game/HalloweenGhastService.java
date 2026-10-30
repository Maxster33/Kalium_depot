package fr.kalium.bingo.game;

import fr.kalium.bingo.gui.GameMenu;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * 0.13.0 - ghast d'Halloween (demande de LeKiwi06, 30/10/2026 ; cahier local Halloween_Bingo_Ghast.md, valide le
 * meme jour) : « Un ghast est présent dans chaque partie ; si vous le tuez en lui renvoyant sa boule, cela valide une
 * case de votre grille. »
 *
 * - Un ghast par equipe (chaque equipe a son monde), pose au lancement de la partie a un endroit tire au hasard dans
 *   un rayon de {@code halloween.ghast.rayon} blocs (500) autour du point de depart, 20 blocs au-dessus du sol. Il ne
 *   peut pas disparaitre (persistant). Seulement pour les parties lancees entre {@code halloween.ghast.debut} et
 *   {@code halloween.ghast.fin} inclus (heure de Paris).
 * - Il ne meurt que de sa boule renvoyee par un joueur de l'equipe (au coup, ou avec un projectile : le jeu donne
 *   alors la boule au tireur) : tout autre degat est annule ({@code /kill} reste possible pour un admin). Une fois
 *   tue, il ne revient pas ; pas de butin (la larme et la poudre sont des objectifs de la grille).
 * - Le tueur donne a son equipe un joker : dans le papier Objectifs, l'objet « Joker du ghast » ouvre la grille et la
 *   case non validee choisie est validee comme un objet trouve (points, bonus « en 1er », bingos, chrono), voir
 *   ObjectiveValidationTask.validate. Echap = plus tard, le joker reste ; perdu a la fin de la partie.
 * - Reperage : nom au-dessus de lui et barre de boss pour les joueurs a moins de 96 blocs (pas de surbrillance, que
 *   les joueurs Bedrock ne voient pas).
 *
 * Etat garde dans le monde de l'equipe (donnees du monde, sauvegardees avec lui) pour survivre a un redemarrage : le
 * ghast a-t-il ete pose, combien de jokers restent.
 */
public final class HalloweenGhastService implements Listener {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final double BAR_RANGE = 96;

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final ObjectiveValidationTask validation;
    private final NamespacedKey ghastKey;
    private final NamespacedKey placedKey;
    private final NamespacedKey jokersKey;
    private final Random random = new Random();
    private final BossBar bar = BossBar.bossBar(Component.text("Ghast d'Halloween", NamedTextColor.GOLD), 1f,
            BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    /** Joueurs qui voient la barre de boss en ce moment. */
    private final Map<UUID, Boolean> viewers = new HashMap<>();
    private GameMenu gameMenu;

    public HalloweenGhastService(JavaPlugin plugin, GameManager gameManager, ObjectiveValidationTask validation) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.validation = validation;
        this.ghastKey = new NamespacedKey(plugin, "ghast_halloween");
        this.placedKey = new NamespacedKey(plugin, "ghast_halloween_pose");
        this.jokersKey = new NamespacedKey(plugin, "jokers_halloween");
    }

    public void setGameMenu(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    /** Toutes les secondes : pose les ghasts des parties qui n'en ont pas encore, barre de boss. */
    public void tick() {
        for (BingoGame game : gameManager.getActiveGames()) {
            if (game.getState() != GameState.IN_PROGRESS || !inPeriod(game)) {
                continue;
            }
            for (BingoInstance instance : game.getInstances()) {
                World world = instance.getWorld();
                if (world != null && instance.getSpawnLocation() != null && !world.getPersistentDataContainer().has(placedKey)) {
                    world.getPersistentDataContainer().set(placedKey, PersistentDataType.BYTE, (byte) 1);
                    place(game, instance);
                }
            }
        }
        updateBar();
    }

    /** Partie lancee pendant l'evenement (dates de config.yml, incluses, heure de Paris). */
    private boolean inPeriod(BingoGame game) {
        if (game.getStartedAt() == null) {
            return false;
        }
        LocalDate day = game.getStartedAt().atZone(PARIS).toLocalDate();
        LocalDate start = date("halloween.ghast.debut", "2026-10-31");
        LocalDate end = date("halloween.ghast.fin", "2026-11-14");
        return start != null && end != null && !day.isBefore(start) && !day.isAfter(end);
    }

    private LocalDate date(String path, String fallback) {
        String text = plugin.getConfig().getString(path, fallback);
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            plugin.getLogger().warning("[KG_BingoGame] config.yml : " + path + " = '" + text + "' n'est pas une date (aaaa-mm-jj).");
            return null;
        }
    }

    private void place(BingoGame game, BingoInstance instance) {
        World world = instance.getWorld();
        Location spawn = instance.getSpawnLocation();
        double radius = Math.max(0, plugin.getConfig().getDouble("halloween.ghast.rayon", 500));
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = radius * Math.sqrt(random.nextDouble()); // tirage uniforme dans le disque
        int x = (int) Math.floor(spawn.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(spawn.getZ() + Math.sin(angle) * distance);
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            plugin.getLogger().warning("[KG_BingoGame] " + world.getName() + " est en paisible : le jeu y retire les ghasts.");
        }
        // Chunk charge (genere si besoin) sans bloquer le serveur ; la suite tourne sur le thread principal.
        world.getChunkAtAsync(x >> 4, z >> 4, true).thenAccept(chunk -> {
            if (game.getState() != GameState.IN_PROGRESS) {
                return;
            }
            int y = Math.min(world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING) + 20, world.getMaxHeight() - 8);
            world.spawn(new Location(world, x + 0.5, y, z + 0.5), Ghast.class, ghast -> {
                ghast.getPersistentDataContainer().set(ghastKey, PersistentDataType.BYTE, (byte) 1);
                ghast.customName(Component.text("Ghast d'Halloween", NamedTextColor.GOLD));
                ghast.setCustomNameVisible(true);
                ghast.setPersistent(true);
                ghast.setRemoveWhenFarAway(false);
            });
            plugin.getLogger().info("[KG_BingoGame] Ghast d'Halloween pose dans " + world.getName() + " en " + x + " " + y + " " + z + ".");
            tellTeam(instance, Component.text("Un ghast d'Halloween rôde à moins de " + (int) radius
                    + " blocs du départ. Tuez-le en lui renvoyant sa boule : un joker pour votre grille !", NamedTextColor.GOLD));
        }).exceptionally(error -> {
            plugin.getLogger().warning("[KG_BingoGame] Ghast d'Halloween non pose dans " + world.getName() + " : " + error);
            return null;
        });
    }

    public boolean isOurs(Entity entity) {
        return entity instanceof Ghast && entity.getPersistentDataContainer().has(ghastKey);
    }

    /** Tueur du ghast : joueur qui a renvoye la boule (le jeu lui en donne la propriete), sinon null. */
    private static Player reflector(DamageSource source) {
        if (source != null && source.getDirectEntity() instanceof LargeFireball && source.getCausingEntity() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!isOurs(event.getEntity()) || event.getCause() == EntityDamageEvent.DamageCause.KILL) {
            return;
        }
        if (reflector(event.getDamageSource()) == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isOurs(event.getEntity())) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        Player killer = reflector(event.getDamageSource());
        GameManager.InstanceRef ref = gameManager.findInstanceByWorld(event.getEntity().getWorld());
        if (killer == null || ref == null || ref.game().getState() != GameState.IN_PROGRESS
                || !ref.instance().isAssignedTo(killer) || ref.instance().hasAbandoned(killer.getUniqueId())) {
            return;
        }
        World world = ref.instance().getWorld();
        world.getPersistentDataContainer().set(jokersKey, PersistentDataType.INTEGER, jokers(ref.instance()) + 1);
        int team = ref.instance().getTeam().getTeamNumber();
        boolean solo = ref.instance().getTeam().getPlayers().size() <= 1;
        broadcast(ref.game(), TeamStyle.name(team)
                .append(Component.text(" tue le ghast d'Halloween", NamedTextColor.GOLD))
                .append(Component.text(solo ? "" : " (" + killer.getName() + ")", NamedTextColor.GRAY))
                .append(Component.text(" : un joker !", NamedTextColor.LIGHT_PURPLE)));
        tellTeam(ref.instance(), Component.text("Joker : ouvrez le papier Objectifs et choisissez une case à valider.",
                NamedTextColor.LIGHT_PURPLE));
        for (UUID id : ref.instance().getTeam().getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }

    /** Jokers non utilises de l'equipe. */
    public int jokers(BingoInstance instance) {
        World world = instance.getWorld();
        if (world == null) {
            return 0;
        }
        return world.getPersistentDataContainer().getOrDefault(jokersKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * Utilise un joker de l'equipe du joueur sur cette case (choisie dans GameMenu, grille du joker).
     *
     * @return message d'erreur, ou null si la case est validee
     */
    public String useJoker(Player player, int cellIndex) {
        Optional<BingoGame> gameOpt = gameManager.findGameOf(player.getUniqueId());
        if (gameOpt.isEmpty() || gameOpt.get().getState() != GameState.IN_PROGRESS || gameOpt.get().getGrid() == null) {
            return "Aucune partie en cours.";
        }
        BingoGame game = gameOpt.get();
        if (game.isFrozen() || (game.getSettings().isChrono() && game.isTimeUp())) {
            return "La partie est en pause ou terminée.";
        }
        Optional<BingoInstance> instanceOpt = game.findInstanceOf(player.getUniqueId());
        if (instanceOpt.isEmpty() || instanceOpt.get().hasAbandoned(player.getUniqueId())) {
            return "Vous ne jouez pas dans cette partie.";
        }
        BingoInstance instance = instanceOpt.get();
        int left = jokers(instance);
        if (left <= 0) {
            return "Votre équipe n'a pas de joker.";
        }
        int team = instance.getTeam().getTeamNumber();
        if (cellIndex < 0 || cellIndex >= game.getGrid().getCells().size() || game.isValidated(team, cellIndex)) {
            return "Cette case est déjà validée.";
        }
        // Joker retire AVANT la validation : elle peut terminer la partie (victoire) et supprimer le monde.
        instance.getWorld().getPersistentDataContainer().set(jokersKey, PersistentDataType.INTEGER, left - 1);
        if (!validation.validate(game, team, cellIndex, player, true)) {
            instance.getWorld().getPersistentDataContainer().set(jokersKey, PersistentDataType.INTEGER, left);
            return "Cette case est déjà validée.";
        }
        return null;
    }

    /** Clic sur l'objet « Joker du ghast » du papier Objectifs (GameMenuListener annule deja le clic). */
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (gameMenu == null || !(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Object holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof GameMenu.GameMenuHolder && event.getSlot() == GameMenu.JOKER_SLOT) {
            Bukkit.getScheduler().runTask(plugin, () -> gameMenu.openJoker(player));
        } else if (holder instanceof GameMenu.JokerMenuHolder) {
            event.setCancelled(true);
            if (event.getSlot() == GameMenu.CLOSE_SLOT) {
                Bukkit.getScheduler().runTask(plugin, () -> gameMenu.open(player));
                return;
            }
            int cell = gameMenu.cellAt(player, event.getSlot());
            if (cell < 0) {
                return;
            }
            String error = useJoker(player, cell);
            if (error != null) {
                player.sendActionBar(Component.text(error, NamedTextColor.RED));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        }
    }

    @EventHandler
    public void onMenuDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GameMenu.JokerMenuHolder) {
            event.setCancelled(true);
        }
    }

    /** Barre de boss pour les joueurs a moins de BAR_RANGE blocs d'un ghast d'Halloween vivant. */
    private void updateBar() {
        Map<UUID, Boolean> near = new HashMap<>();
        for (BingoGame game : gameManager.getActiveGames()) {
            if (game.getState() != GameState.IN_PROGRESS) {
                continue;
            }
            for (BingoInstance instance : game.getInstances()) {
                for (UUID id : instance.getTeam().getPlayers()) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null && player.isOnline() && !instance.hasAbandoned(id)
                            && player.getWorld().equals(instance.getWorld()) && ghastNear(player)) {
                        near.put(id, true);
                    }
                }
            }
        }
        for (Iterator<UUID> it = viewers.keySet().iterator(); it.hasNext(); ) {
            UUID id = it.next();
            if (!near.containsKey(id)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.hideBossBar(bar);
                }
                it.remove();
            }
        }
        for (UUID id : near.keySet()) {
            if (viewers.put(id, true) == null) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.showBossBar(bar);
                }
            }
        }
    }

    private boolean ghastNear(Player player) {
        for (Entity entity : player.getNearbyEntities(BAR_RANGE, BAR_RANGE, BAR_RANGE)) {
            if (isOurs(entity) && !entity.isDead()) {
                return true;
            }
        }
        return false;
    }

    private static void tellTeam(BingoInstance instance, Component message) {
        for (UUID id : instance.getTeam().getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline() && !instance.hasAbandoned(id)) {
                player.sendMessage(message);
            }
        }
    }

    private static void broadcast(BingoGame game, Component message) {
        for (BingoInstance instance : game.getInstances()) {
            tellTeam(instance, message);
        }
    }
}
