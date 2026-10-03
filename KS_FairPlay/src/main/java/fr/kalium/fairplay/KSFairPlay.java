package fr.kalium.fairplay;

import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.DecoratedPot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_FairPlay (cahier des charges : catégorie 5 « FairPlay », LeKiwi06, validé le 30/09/2026) : serveur Event.
 *
 * - Mini-cartes : code « fair » de Xaero envoyé à la connexion (vue des grottes, carte du Nether comprise, et radar
 *   d'entités coupés ; carte de surface et points de repère gardés). JourneyMap : son plugin serveur ; Legacy
 *   Freecam : son plugin serveur (tout ce qui est « déloyal » interdit par défaut).
 * - Limite globale de 10 par jour (remise à zéro à minuit, heure de Paris) : ouvrir un contenant au butin non généré
 *   (coffres, tonneaux, wagonnets de mineshaft, distributeurs des temples ; pas les pots ni les coffres forts),
 *   casser un spawner naturel (pas ceux posés par KS_Spawners), brosser un bloc suspect. Limite atteinte : refus, bloc
 *   intact, « Limite atteinte, reviens demain ».
 * - Protection tant que le butin n'est pas généré (spawner naturel : tant qu'il n'est pas cassé) : incassable par un
 *   joueur sans autorisation du jour, par les explosions, le feu, les pistons ; bloc suspect qui ne tombe pas ; pas
 *   d'entonnoir (ni wagonnet à entonnoir) ; wagonnet de mineshaft indestructible sans autorisation.
 * - Gardien ancien (LeKiwi06, 03/10/2026 : son butin rare ne tombe que s'il est tué par un joueur) : le tuer compte
 *   1 pour le tueur ; un joueur qui a atteint sa limite ne peut plus le frapper (corps à corps, flèche, trident,
 *   potion, TNT qu'il a allumée...). Les gardiens normaux ne sont pas concernés (fermes).
 * - Chance III ou plus (Élixir de Fortune) : ouvertures sans limite, qui ne comptent pas.
 * - Créatif : ni limite ni compte.
 */
public final class KSFairPlay extends JavaPlugin implements Listener {

    /** Code de Xaero's Minimap / World Map : mode « fair-play » (pas de vue des grottes ni de radar d'entités). */
    private static final String CODE_XAERO = "§f§a§i§r§x§a§e§r§o";
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private Lang lang;
    private File fichier;
    private NamespacedKey spawnerPose;
    private LocalDate jour;
    private final Map<UUID, Integer> compteurs = new HashMap<>();
    /** Blocs suspects déjà comptés aujourd'hui (joueur + bloc) : un brossage interrompu ne compte qu'une fois. */
    private final Set<String> brossesAujourdhui = new HashSet<>();
    /** Dernier tick compté par joueur : un coffre double (deux moitiés) ne compte qu'une fois. */
    private final Map<UUID, Integer> dernierTick = new HashMap<>();
    private final Map<UUID, Long> dernierMessage = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        fichier = new File(getDataFolder(), "compteurs.yml");
        // Marqueur des spawners posés par un joueur (KS_Spawners) : ceux-là ne sont pas « naturels ».
        spawnerPose = new NamespacedKey("ks_spawners", "pose");
        charger();
        getServer().getPluginManager().registerEvents(this, this);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        sauver();
    }

    private int limite() {
        return Math.max(0, getConfig().getInt("limite-par-jour", 10));
    }

    // ------------------------------------------------------------------ mini-cartes

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!getConfig().getBoolean("xaero-fair", true)) {
            return;
        }
        Player joueur = event.getPlayer();
        // Un peu après l'arrivée : le mod du joueur doit être prêt à lire le message.
        getServer().getScheduler().runTaskLater(this, () -> {
            if (joueur.isOnline()) {
                joueur.sendMessage(lang.c("connexion", "<dark_gray>Mini-cartes : vue des grottes et radar d'entités "
                        + "désactivés (fair-play).").append(Component.text(CODE_XAERO)));
                lang.saveIfNeeded();
            }
        }, 40L);
    }

    // ------------------------------------------------------------------ compteurs du jour

    private void verifierJour() {
        LocalDate aujourdhui = LocalDate.now(PARIS);
        if (!aujourdhui.equals(jour)) {
            jour = aujourdhui;
            compteurs.clear();
            brossesAujourdhui.clear();
            sauver();
        }
    }

    private static boolean chanceIII(Player joueur) {
        PotionEffect chance = joueur.getPotionEffect(PotionEffectType.LUCK);
        return chance != null && chance.getAmplifier() >= 2;
    }

    /** Créatif, ou Chance III : ni limite ni compte. */
    private static boolean libre(Player joueur) {
        return joueur.getGameMode() == GameMode.CREATIVE || chanceIII(joueur);
    }

    /** Le joueur peut-il encore ouvrir / casser / brosser aujourd'hui ? Sinon : message, et false. */
    private boolean autorise(Player joueur) {
        if (libre(joueur)) {
            return true;
        }
        verifierJour();
        if (compteurs.getOrDefault(joueur.getUniqueId(), 0) < limite()) {
            return true;
        }
        // Un message toutes les 3 secondes au plus (coups répétés sur un gardien, clics sur un coffre).
        long maintenant = System.currentTimeMillis();
        Long avant = dernierMessage.get(joueur.getUniqueId());
        if (avant != null && maintenant - avant < 3000) {
            return false;
        }
        dernierMessage.put(joueur.getUniqueId(), maintenant);
        joueur.sendMessage(lang.c("limite-3", "<red>Exploration journalière : limite atteinte, reviens demain.",
                "limite", limite()));
        lang.saveIfNeeded();
        return false;
    }

    /** Une ouverture / casse / brossage de plus pour aujourd'hui (rien en créatif ni sous Chance III). */
    private void compter(Player joueur) {
        if (joueur.getGameMode() == GameMode.CREATIVE) {
            // 1.0.1 : dit clairement que rien n'est compté (avant : aucun message, on croyait le compteur absent).
            Component creatif = lang.c("creatif", "<gray>Créatif : non compté (passe en survie pour tester la limite).");
            joueur.sendActionBar(creatif);
            joueur.sendMessage(creatif);
            lang.saveIfNeeded();
            return;
        }
        if (chanceIII(joueur)) {
            Component libre = lang.c("chance", "<green>Chance III : ouverture libre, elle ne compte pas.");
            joueur.sendActionBar(libre);
            joueur.sendMessage(libre);
            lang.saveIfNeeded();
            return;
        }
        int tick = Bukkit.getCurrentTick();
        Integer avant = dernierTick.put(joueur.getUniqueId(), tick);
        if (avant != null && avant == tick) {
            return;
        }
        verifierJour();
        int n = compteurs.merge(joueur.getUniqueId(), 1, Integer::sum);
        sauver();
        // 1.0.2 (LeKiwi06) : texte court, sans détailler ce qui compte (« pour ne pas trop spoil les changements »).
        Component compte = lang.c("compte-3", "<yellow>Exploration journalière : <n> / <limite>", "n", n,
                "limite", limite());
        // Barre d'action et tchat (demande de LeKiwi06 : « pour ceux qui perdent le fil »).
        joueur.sendActionBar(compte);
        joueur.sendMessage(compte);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ ce qui compte

    /** Contenant (ou bloc suspect) dont le butin n'est pas encore généré ; pas les pots décorés (hors liste). */
    private static boolean nonGenere(BlockState etat) {
        return etat instanceof Lootable l && !(etat instanceof DecoratedPot) && l.hasLootTable();
    }

    private static boolean suspect(Material type) {
        return type == Material.SUSPICIOUS_SAND || type == Material.SUSPICIOUS_GRAVEL;
    }

    private boolean spawnerNaturel(Block bloc) {
        return bloc.getType() == Material.SPAWNER && bloc.getState() instanceof CreatureSpawner s
                && !s.getPersistentDataContainer().has(spawnerPose, PersistentDataType.STRING);
    }

    /** Bloc protégé : butin non généré ou spawner naturel. */
    private boolean protege(Block bloc) {
        Material type = bloc.getType();
        if (type == Material.SPAWNER) {
            return spawnerNaturel(bloc);
        }
        if (!(suspect(type) || type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL
                || type == Material.DISPENSER || type == Material.DROPPER || type == Material.HOPPER
                || type.name().endsWith("SHULKER_BOX"))) {
            return false;
        }
        return nonGenere(bloc.getState(false));
    }

    private static boolean wagonnetNonGenere(Entity entite) {
        return entite instanceof StorageMinecart w && w.hasLootTable();
    }

    // ------------------------------------------------------------------ ouverture, brossage, casse

    /** Ouvrir un contenant au butin non généré, ou commencer à brosser un bloc suspect : refusé si limite atteinte. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block bloc = event.getClickedBlock();
        boolean brossage = suspect(bloc.getType());
        if (brossage && (event.getItem() == null || event.getItem().getType() != Material.BRUSH)) {
            return;
        }
        // Accroupi avec un objet en main : le jeu pose l'objet au lieu d'ouvrir le contenant.
        if (!brossage && event.getPlayer().isSneaking() && event.getItem() != null) {
            return;
        }
        if (!protege(bloc) || bloc.getType() == Material.SPAWNER) {
            return;
        }
        if (!autorise(event.getPlayer())) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    /** Brossage commencé (pas annulé) : compté une fois par bloc et par jour (le butin sort au premier coup). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractCompte(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.useItemInHand() == org.bukkit.event.Event.Result.DENY
                || event.getItem() == null || event.getItem().getType() != Material.BRUSH) {
            return;
        }
        Block bloc = event.getClickedBlock();
        if (!suspect(bloc.getType()) || !nonGenere(bloc.getState(false))) {
            return;
        }
        verifierJour();
        String cle = event.getPlayer().getUniqueId() + "@" + bloc.getWorld().getName() + ":" + bloc.getX() + ":"
                + bloc.getY() + ":" + bloc.getZ();
        if (brossesAujourdhui.add(cle)) {
            compter(event.getPlayer());
        }
    }

    /** Coffre de wagonnet (mineshaft) au butin non généré : refusé si limite atteinte. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntite(PlayerInteractEntityEvent event) {
        if (wagonnetNonGenere(event.getRightClicked()) && !autorise(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * 1.0.1 : contenant au butin non généré ouvert (clic pas refusé) : compté si, au tick suivant, son butin est sorti
     * (avant : à l'événement de génération du butin, qui ne comptait pas). Un coffre double = 1.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onOuvertureCompte(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            return;
        }
        Block bloc = event.getClickedBlock();
        Player joueur = event.getPlayer();
        if (suspect(bloc.getType()) || bloc.getType() == Material.SPAWNER || !protege(bloc)
                || (joueur.isSneaking() && event.getItem() != null)) {
            return;
        }
        getServer().getScheduler().runTask(this, () -> {
            if (joueur.isOnline() && !nonGenere(bloc.getState(false))) {
                compter(joueur);
            }
        });
    }

    /** 1.0.1 : coffre de wagonnet ouvert : compté si son butin est sorti. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOuvertureWagonnet(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof StorageMinecart wagonnet) || !wagonnet.hasLootTable()) {
            return;
        }
        Player joueur = event.getPlayer();
        getServer().getScheduler().runTask(this, () -> {
            if (joueur.isOnline() && (!wagonnet.isValid() || !wagonnet.hasLootTable())) {
                compter(joueur);
            }
        });
    }

    /** Casser : refusé sans autorisation du jour ; un spawner naturel cassé compte. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (protege(event.getBlock()) && !autorise(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Spawner naturel cassé, ou contenant au butin non généré cassé (son butin tombe) : compté. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakCompte(BlockBreakEvent event) {
        Block bloc = event.getBlock();
        if (spawnerNaturel(bloc) || (!suspect(bloc.getType()) && protege(bloc))) {
            compter(event.getPlayer());
        }
    }

    // ------------------------------------------------------------------ gardien ancien

    /** Joueur à l'origine d'un dégât : direct, projectile (flèche, trident, potion), TNT allumée, nuage de potion. */
    private static Player auteur(Entity source) {
        if (source instanceof Player joueur) {
            return joueur;
        }
        if (source instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player joueur) {
            return joueur;
        }
        if (source instanceof org.bukkit.entity.TNTPrimed tnt && tnt.getSource() instanceof Player joueur) {
            return joueur;
        }
        if (source instanceof org.bukkit.entity.AreaEffectCloud nuage && nuage.getSource() instanceof Player joueur) {
            return joueur;
        }
        return null;
    }

    /** Limite atteinte : impossible de frapper un gardien ancien. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageGardien(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != org.bukkit.entity.EntityType.ELDER_GUARDIAN) {
            return;
        }
        Player joueur = auteur(event.getDamager());
        if (joueur != null && !autorise(joueur)) {
            event.setCancelled(true);
        }
    }

    /** Gardien ancien tué par un joueur (son butin rare tombe) : compté pour le tueur. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathGardien(EntityDeathEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.ELDER_GUARDIAN && event.getEntity().getKiller() != null) {
            compter(event.getEntity().getKiller());
        }
    }

    // ------------------------------------------------------------------ protections

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::protege);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::protege);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (protege(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::protege)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::protege)) {
            event.setCancelled(true);
        }
    }

    /** Bloc suspect au butin non généré : il ne tombe pas (en tombant, il se casse et le butin est perdu). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChange(EntityChangeBlockEvent event) {
        if (suspect(event.getBlock().getType()) && event.getTo() != event.getBlock().getType()
                && nonGenere(event.getBlock().getState(false))) {
            event.setCancelled(true);
        }
    }

    /** Entonnoir sous / à côté d'un contenant au butin non généré : il ne le voit pas. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHopperSearch(HopperInventorySearchEvent event) {
        Block cible = event.getSearchBlock();
        if (event.getInventory() != null && protege(cible)) {
            event.setInventory(null);
        }
    }

    /** Wagonnet à entonnoir (ou autre transfert automatique) depuis un contenant au butin non généré : refusé. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        InventoryHolder source = event.getSource().getHolder(false);
        if (source instanceof Lootable l && !(source instanceof DecoratedPot) && l.hasLootTable()) {
            event.setCancelled(true);
        }
    }

    /** Wagonnet de mineshaft au butin non généré : indestructible sans autorisation (explosions comprises). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (wagonnetNonGenere(event.getVehicle())
                && !(event.getAttacker() instanceof Player joueur && autorise(joueur))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (wagonnetNonGenere(event.getVehicle())
                && !(event.getAttacker() instanceof Player joueur && autorise(joueur))) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        LocalDate aujourdhui = LocalDate.now(PARIS);
        jour = aujourdhui;
        if (!aujourdhui.toString().equals(yaml.getString("jour", ""))) {
            return;
        }
        var section = yaml.getConfigurationSection("joueurs");
        if (section != null) {
            for (String cle : section.getKeys(false)) {
                try {
                    compteurs.put(UUID.fromString(cle), section.getInt(cle));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Compteur ignoré : " + cle);
                }
            }
        }
        brossesAujourdhui.addAll(yaml.getStringList("brosses"));
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("jour", jour == null ? "" : jour.toString());
        compteurs.forEach((uuid, n) -> yaml.set("joueurs." + uuid, n));
        yaml.set("brosses", List.copyOf(brossesAujourdhui));
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer compteurs.yml : " + e.getMessage());
        }
    }
}
