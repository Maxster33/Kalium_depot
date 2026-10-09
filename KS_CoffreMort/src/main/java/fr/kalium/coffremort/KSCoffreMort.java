package fr.kalium.coffremort;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_CoffreMort (cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort », LeKiwi06, 03/10/2026 et
 * 08/10/2026) : serveur Event.
 *
 * - À la mort, les objets du joueur et la moitié de son expérience (« une bouteille d'xp custom ») vont dans un coffre
 *   posé sur le bloc d'air ou de fluide le plus proche (dans la lave : à la place du bloc de lave ; dans le vide : sur
 *   la couche constructible la plus basse, aux coordonnées de la mort). L'autre moitié de l'expérience est perdue.
 * - Le coffre n'est ouvrable et cassable que par le mort, pendant 15 minutes ; ensuite il disparaît avec son contenu.
 *   3 coffres actifs au plus par joueur : le 4e supprime le plus ancien. Le contenu est gardé par le plugin (le bloc
 *   n'est qu'un repère) : ni entonnoir ni explosion ne peut le vider.
 * - Mort dans le claim d'un autre joueur (1.0.1 : et non dans un des siens, correction de LeKiwi06), ou dans une zone
 *   protégée où il ne peut ni casser ni poser : le contenu va dans /rewards (KS_RewardsGUI), avec un message.
 * - /coffres (et bouton du menu) : coordonnées et temps restant de ses coffres (visibles sans jeton), contenu, et
 *   récupération certaine : le contenu part dans /rewards et le coffre disparaît, avec le badge de la mort (gratuit,
 *   une fois tous les N jours réels selon son niveau, délai tenu par joueur) sinon avec un jeton de la mort (KS_Jetons).
 */
public final class KSCoffreMort extends JavaPlugin implements Listener {

    /** Position d'un coffre de mort. */
    private record Pos(String monde, int x, int y, int z) {
        static Pos de(Block bloc) {
            return new Pos(bloc.getWorld().getName(), bloc.getX(), bloc.getY(), bloc.getZ());
        }
    }

    /** Contenu d'un coffre de mort, ouvert par son propriétaire (on ne peut qu'y prendre). */
    private record Vue(String id) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** Aperçu du contenu depuis /coffres (rien ne se prend). */
    private record Apercu() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** Un coffre de mort posé. */
    private static final class Coffre {
        String id;
        UUID joueur;
        String nom;
        Pos pos;
        long expiration;
        /** Le coffre a pris la place d'une source d'eau (remise quand il disparaît). */
        boolean eau;
        Inventory contenu;
    }

    private static final int CASES = 54;
    /** Piles par récompense de /rewards : elle doit tenir dans un inventaire, même partiellement rempli. */
    private static final int PILES_PAR_RECOMPENSE = 27;
    private static final long JOUR = 24L * 3600_000L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final BlockFace[] COTES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    /** 1.0.2 (modération) : un coffre de mort actif : position, heure de sa disparition, piles d'objets qu'il contient. */
    public record CoffreActif(Location position, long expiration, int piles) {
    }

    private static KSCoffreMort instance;

    private Lang lang;
    private Gui gui;
    private File fichier;
    private NamespacedKey clePoints;
    /** Coffres actifs, du plus ancien au plus récent. */
    private final Map<String, Coffre> coffres = new LinkedHashMap<>();
    private final Map<Pos, Coffre> parPosition = new HashMap<>();
    /** Dernière récupération gratuite avec le badge de la mort, par joueur (millisecondes). */
    private final Map<UUID, Long> badgeUtilise = new HashMap<>();
    private boolean aSauver;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        clePoints = new NamespacedKey(this, "points");
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "coffres.yml");
        charger();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("coffres").setExecutor(this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "coffres", lang.c("bouton.nom", "<dark_red><bold>Coffres de mort"),
                    lang.c("bouton.description", "<gray>Où sont tes coffres de mort, et les récupérer"), 45, this::ouvrir);
        }
        getServer().getScheduler().runTaskTimer(this, this::expirer, 20L, 20L);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = joueur.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof Vue || holder instanceof Apercu) {
                joueur.closeInventory();
            }
        }
        sauver();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private long duree() {
        return Math.max(1, getConfig().getLong("duree-minutes", 15)) * 60_000L;
    }

    private int maximum() {
        return Math.max(1, getConfig().getInt("maximum", 3));
    }

    // ------------------------------------------------------------------ mort

    /** Après les autres plugins (ils ont fini de modifier les objets lâchés). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMort(PlayerDeathEvent event) {
        Player joueur = event.getPlayer();
        List<ItemStack> objets = new ArrayList<>();
        if (!event.getKeepInventory()) {
            for (ItemStack objet : event.getDrops()) {
                if (objet != null && !objet.isEmpty()) {
                    objets.add(objet.clone());
                }
            }
        }
        // La moitié de l'expérience est gardée, en fiole ; l'autre est perdue (aucune orbe).
        int points = event.getKeepLevel() ? 0 : joueur.calculateTotalExperiencePoints() / 2;
        if (points > 0) {
            objets.add(fiole(points));
        }
        if (objets.isEmpty()) {
            return;
        }
        Block bloc = emplacement(joueur.getLocation());
        Component raison = bloc == null
                ? t("mort.aucune-place", "<gold>Aucune place pour ton coffre de mort ici")
                : claimDunAutre(bloc, joueur) ? t("mort.claim-autre", "<gold>Tu es mort dans le claim d'un autre joueur")
                : protege(bloc, joueur) ? t("mort.protege", "<gold>Tu es mort dans une zone protégée") : null;
        boolean range;
        if (raison != null && versRewards(joueur, objets, "Mort du " + LocalDateTime.now(ZoneId.of("Europe/Paris"))
                .format(DATE))) {
            joueur.sendMessage(raison.append(t("mort.rewards", "<gold> : tes objets t'attendent dans <white>/rewards<gold>.")));
            getLogger().info(joueur.getName() + " : mort, " + objets.size() + " pile(s) envoyée(s) dans /rewards.");
            range = true;
        } else if (bloc != null) {
            poser(joueur, bloc, objets);
            range = true;
        } else {
            // Ni coffre ni /rewards : la mort reste vanilla (objets au sol), seule l'expérience suit la règle.
            range = false;
        }
        if (!event.getKeepLevel()) {
            event.setDroppedExp(0);
            if (points > 0 && !range) {
                event.getDrops().add(fiole(points));
            }
        }
        if (range) {
            event.getDrops().clear();
        }
        lang.saveIfNeeded();
    }

    /**
     * Bloc d'air ou de fluide le plus proche de la mort (la lave est remplacée) ; hors du monde (vide, plafond) : la
     * couche constructible la plus proche, aux mêmes coordonnées. Null si rien n'est libre autour.
     */
    private Block emplacement(Location mort) {
        World monde = mort.getWorld();
        int x = mort.getBlockX();
        int z = mort.getBlockZ();
        int y = Math.max(monde.getMinHeight(), Math.min(monde.getMaxHeight() - 1, mort.getBlockY()));
        int rayon = Math.max(0, Math.min(16, getConfig().getInt("rayon-recherche", 6)));
        Block meilleur = null;
        int distance = Integer.MAX_VALUE;
        for (int dx = -rayon; dx <= rayon; dx++) {
            for (int dz = -rayon; dz <= rayon; dz++) {
                if (!monde.isChunkLoaded((x + dx) >> 4, (z + dz) >> 4)) {
                    continue;
                }
                for (int dy = -rayon; dy <= rayon; dy++) {
                    int d = dx * dx + dy * dy + dz * dz;
                    if (d >= distance || y + dy < monde.getMinHeight() || y + dy >= monde.getMaxHeight()) {
                        continue;
                    }
                    Block bloc = monde.getBlockAt(x + dx, y + dy, z + dz);
                    if ((bloc.getType().isAir() || bloc.isLiquid()) && monde.getWorldBorder().isInside(bloc.getLocation())) {
                        meilleur = bloc;
                        distance = d;
                    }
                }
            }
        }
        return meilleur;
    }

    /** 1.0.1 : les claims des autres restent sans coffre de mort (dans les siens, le coffre est posé). */
    private boolean claimDunAutre(Block bloc, Player joueur) {
        return getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem")
                && ProtectionClaims.dUnAutre(bloc, joueur);
    }

    /** Zone où le joueur ne peut ni casser ni poser (région WorldGuard). */
    private boolean protege(Block bloc, Player joueur) {
        return getServer().getPluginManager().isPluginEnabled("WorldGuard") && ProtectionWorldGuard.protege(bloc, joueur);
    }

    /** Dépose les objets dans /rewards, par lots qui tiennent dans un inventaire. Faux si KS_RewardsGUI est absent. */
    private boolean versRewards(Player joueur, List<ItemStack> objets, String raison) {
        if (!getServer().getPluginManager().isPluginEnabled("KS_RewardsGUI")) {
            return false;
        }
        int lots = (objets.size() + PILES_PAR_RECOMPENSE - 1) / PILES_PAR_RECOMPENSE;
        for (int lot = 0; lot < lots; lot++) {
            List<ItemStack> partie = objets.subList(lot * PILES_PAR_RECOMPENSE,
                    Math.min(objets.size(), (lot + 1) * PILES_PAR_RECOMPENSE));
            if (!fr.kalium.rewardsgui.KSRewardsGUI.deposer(joueur.getUniqueId(), joueur.getName(), "Coffre de mort",
                    lots > 1 ? raison + " (" + (lot + 1) + "/" + lots + ")" : raison, partie)) {
                // Rien n'est perdu : le lot refusé et les suivants tombent aux pieds du joueur (ne devrait pas arriver).
                for (ItemStack reste : objets.subList(lot * PILES_PAR_RECOMPENSE, objets.size())) {
                    joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
                }
                getLogger().warning("Dépôt dans /rewards refusé pour " + joueur.getName() + " : objets lâchés au sol.");
                return true;
            }
        }
        return true;
    }

    private void poser(Player joueur, Block bloc, List<ItemStack> objets) {
        List<Coffre> siens = coffresDe(joueur.getUniqueId());
        while (siens.size() >= maximum()) {
            Coffre ancien = siens.remove(0);
            retirer(ancien);
            joueur.sendMessage(t("mort.ancien", "<red>Ton plus ancien coffre de mort (<x> <y> <z>) a disparu : "
                    + "<n> coffres au plus.", "x", ancien.pos.x(), "y", ancien.pos.y(), "z", ancien.pos.z(), "n", maximum()));
        }
        Coffre coffre = new Coffre();
        coffre.id = UUID.randomUUID().toString();
        coffre.joueur = joueur.getUniqueId();
        coffre.nom = joueur.getName();
        coffre.pos = Pos.de(bloc);
        coffre.expiration = System.currentTimeMillis() + duree();
        coffre.eau = bloc.getType() == Material.WATER && bloc.getBlockData() instanceof Levelled niveau
                && niveau.getLevel() == 0;
        coffre.contenu = inventaire(coffre.id);
        for (ItemStack objet : objets) {
            for (ItemStack reste : coffre.contenu.addItem(objet).values()) {
                bloc.getWorld().dropItemNaturally(bloc.getLocation().add(0.5, 0.5, 0.5), reste);
            }
        }
        bloc.setType(Material.CHEST, false);
        BlockData donnees = bloc.getBlockData();
        if (donnees instanceof org.bukkit.block.data.type.Chest chest) {
            chest.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
            chest.setWaterlogged(coffre.eau);
            bloc.setBlockData(chest, false);
        }
        coffres.put(coffre.id, coffre);
        parPosition.put(coffre.pos, coffre);
        sauverBientot();
        joueur.sendMessage(t("mort.coffre", "<gold>Ton coffre de mort est en <white><x> <y> <z></white> (<monde>). "
                        + "<gray>Toi seul peux l'ouvrir ou le casser ; il disparaît dans <minutes> minutes. <white>/coffres",
                "x", coffre.pos.x(), "y", coffre.pos.y(), "z", coffre.pos.z(), "monde", nomMonde(coffre.pos.monde()),
                "minutes", duree() / 60_000L));
        getLogger().info(joueur.getName() + " : coffre de mort en " + coffre.pos + " (" + objets.size() + " pile(s)).");
    }

    private Inventory inventaire(String id) {
        return Bukkit.createInventory(new Vue(id), CASES, t("coffre.titre", "Coffre de mort"));
    }

    private List<Coffre> coffresDe(UUID joueur) {
        List<Coffre> liste = new ArrayList<>();
        for (Coffre coffre : coffres.values()) {
            if (coffre.joueur.equals(joueur)) {
                liste.add(coffre);
            }
        }
        return liste;
    }

    /**
     * 1.0.2 (modération : fiche d'un joueur de KS_AntiCheat 1.3.0) : coffres de mort actifs de ce joueur, en ligne ou
     * hors ligne, du plus ancien au plus récent. Lecture seule ; un coffre dont le monde n'est pas chargé est ignoré.
     */
    public static List<CoffreActif> coffresActifs(UUID joueur) {
        List<CoffreActif> liste = new ArrayList<>();
        for (Coffre coffre : instance.coffresDe(joueur)) {
            org.bukkit.World monde = Bukkit.getWorld(coffre.pos.monde());
            if (monde != null) {
                liste.add(new CoffreActif(new Location(monde, coffre.pos.x() + 0.5, coffre.pos.y(), coffre.pos.z() + 0.5),
                        coffre.expiration, contenu(coffre).size()));
            }
        }
        return liste;
    }

    private static List<ItemStack> contenu(Coffre coffre) {
        List<ItemStack> objets = new ArrayList<>();
        for (ItemStack objet : coffre.contenu.getContents()) {
            if (objet != null && !objet.isEmpty()) {
                objets.add(objet.clone());
            }
        }
        return objets;
    }

    private String nomMonde(String monde) {
        World w = Bukkit.getWorld(monde);
        if (w == null) {
            return monde;
        }
        return w.getEnvironment() == World.Environment.NETHER ? "Nether"
                : w.getEnvironment() == World.Environment.THE_END ? "End" : "monde normal";
    }

    /** Le coffre disparaît : contenu oublié, fenêtres fermées, bloc retiré (chunk chargé pour cela s'il le faut). */
    private void retirer(Coffre coffre) {
        coffres.remove(coffre.id);
        parPosition.remove(coffre.pos);
        List<HumanEntity> spectateurs = new ArrayList<>(coffre.contenu.getViewers());
        coffre.contenu.clear();
        if (!spectateurs.isEmpty() && isEnabled()) {
            getServer().getScheduler().runTask(this, () -> spectateurs.forEach(HumanEntity::closeInventory));
        }
        World monde = Bukkit.getWorld(coffre.pos.monde());
        if (monde != null) {
            Pos pos = coffre.pos;
            boolean eau = coffre.eau;
            monde.getChunkAtAsync(pos.x() >> 4, pos.z() >> 4).thenAccept(chunk -> {
                Block bloc = monde.getBlockAt(pos.x(), pos.y(), pos.z());
                if (bloc.getType() == Material.CHEST && !parPosition.containsKey(pos)) {
                    bloc.setType(eau ? Material.WATER : Material.AIR);
                }
            });
        }
        sauverBientot();
    }

    /** Chaque seconde : les coffres dont le temps est écoulé disparaissent avec leur contenu. */
    private void expirer() {
        long maintenant = System.currentTimeMillis();
        for (Coffre coffre : new ArrayList<>(coffres.values())) {
            if (maintenant < coffre.expiration) {
                continue;
            }
            retirer(coffre);
            getLogger().info("Coffre de mort de " + coffre.nom + " en " + coffre.pos + " : temps écoulé.");
            Player joueur = Bukkit.getPlayer(coffre.joueur);
            if (joueur != null) {
                joueur.sendMessage(t("coffre.expire", "<red>Ton coffre de mort en <x> <y> <z> a disparu.", "x",
                        coffre.pos.x(), "y", coffre.pos.y(), "z", coffre.pos.z()));
                lang.saveIfNeeded();
            }
        }
    }

    // ------------------------------------------------------------------ fiole d'expérience

    /** Fiole d'expérience qui rend exactement ces points quand elle est lancée. */
    private ItemStack fiole(int points) {
        ItemStack fiole = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = fiole.getItemMeta();
        meta.displayName(Component.text("Fiole d'expérience (" + points + (points > 1 ? " points)" : " point)"))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("La moitié de ton expérience, sauvée à ta mort", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(clePoints, PersistentDataType.INTEGER, points);
        fiole.setItemMeta(meta);
        return fiole;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFiole(ExpBottleEvent event) {
        ItemStack objet = event.getEntity().getItem();
        if (objet == null || !objet.hasItemMeta()) {
            return;
        }
        Integer points = objet.getItemMeta().getPersistentDataContainer().get(clePoints, PersistentDataType.INTEGER);
        if (points != null) {
            event.setExperience(points);
        }
    }

    // ------------------------------------------------------------------ le bloc

    /** Clic droit sur un coffre de mort : le plugin ouvre lui-même le contenu, pour son propriétaire seulement. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onOuvrir(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || parPosition.isEmpty()) {
            return;
        }
        Coffre coffre = parPosition.get(Pos.de(event.getClickedBlock()));
        if (coffre == null) {
            return;
        }
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player joueur = event.getPlayer();
        if (coffre.joueur.equals(joueur.getUniqueId())) {
            joueur.openInventory(coffre.contenu);
        } else {
            joueur.sendMessage(t("coffre.autre", "<red>Ce coffre de mort est à <nom>.", "nom", coffre.nom));
            lang.saveIfNeeded();
        }
    }

    /** Cassé par son propriétaire : le contenu tombe ; par un autre : refusé. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCasser(BlockBreakEvent event) {
        Coffre coffre = parPosition.isEmpty() ? null : parPosition.get(Pos.de(event.getBlock()));
        if (coffre == null) {
            return;
        }
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        if (!coffre.joueur.equals(joueur.getUniqueId())) {
            joueur.sendMessage(t("coffre.autre", "<red>Ce coffre de mort est à <nom>.", "nom", coffre.nom));
            lang.saveIfNeeded();
            return;
        }
        Location centre = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack objet : contenu(coffre)) {
            centre.getWorld().dropItemNaturally(centre, objet);
        }
        retirer(coffre);
        getLogger().info(joueur.getName() + " casse son coffre de mort en " + coffre.pos + ".");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionBloc(BlockExplodeEvent event) {
        if (!parPosition.isEmpty()) {
            event.blockList().removeIf(bloc -> parPosition.containsKey(Pos.de(bloc)));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionEntite(EntityExplodeEvent event) {
        if (!parPosition.isEmpty()) {
            event.blockList().removeIf(bloc -> parPosition.containsKey(Pos.de(bloc)));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntite(EntityChangeBlockEvent event) {
        if (!parPosition.isEmpty() && parPosition.containsKey(Pos.de(event.getBlock()))) {
            event.setCancelled(true);
        }
    }

    /** Un coffre posé contre un coffre de mort s'y collerait (coffre double). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPoser(BlockPlaceEvent event) {
        if (parPosition.isEmpty() || event.getBlock().getType() != Material.CHEST) {
            return;
        }
        for (BlockFace cote : COTES) {
            if (parPosition.containsKey(Pos.de(event.getBlock().getRelative(cote)))) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(t("coffre.colle", "<red>Pas de coffre contre un coffre de mort."));
                lang.saveIfNeeded();
                return;
            }
        }
    }

    /** Le bloc reste vide : aucun entonnoir n'y met ni n'en sort rien. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntonnoir(InventoryMoveItemEvent event) {
        if (parPosition.isEmpty()) {
            return;
        }
        Location source = event.getSource().getLocation();
        Location destination = event.getDestination().getLocation();
        if ((source != null && parPosition.containsKey(Pos.de(source.getBlock())))
                || (destination != null && parPosition.containsKey(Pos.de(destination.getBlock())))) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ le contenu

    /** Dans un coffre de mort on ne fait que prendre ; dans un aperçu, rien. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClic(InventoryClickEvent event) {
        Inventory haut = event.getView().getTopInventory();
        if (haut.getHolder() instanceof Apercu) {
            event.setCancelled(true);
            return;
        }
        if (!(haut.getHolder() instanceof Vue) || !(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        int brut = event.getRawSlot();
        ItemStack entrant = null;
        if (brut >= 0 && brut < haut.getSize()) {
            if (event.getClick() == ClickType.NUMBER_KEY) {
                entrant = joueur.getInventory().getItem(event.getHotbarButton());
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                entrant = joueur.getInventory().getItemInOffHand();
            } else {
                entrant = event.getCursor();
            }
        } else if (event.isShiftClick()) {
            entrant = event.getCurrentItem();
        }
        if (entrant != null && !entrant.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        sauverBientot();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlisser(InventoryDragEvent event) {
        Inventory haut = event.getView().getTopInventory();
        if (haut.getHolder() instanceof Apercu) {
            event.setCancelled(true);
        } else if (haut.getHolder() instanceof Vue) {
            for (int brut : event.getRawSlots()) {
                if (brut < haut.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /** Coffre vidé : il disparaît. */
    @EventHandler
    public void onFermer(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Vue vue)) {
            return;
        }
        Coffre coffre = coffres.get(vue.id());
        if (coffre == null) {
            return;
        }
        if (coffre.contenu.isEmpty()) {
            retirer(coffre);
            event.getPlayer().sendMessage(t("coffre.vide", "<green>Coffre de mort vidé."));
            lang.saveIfNeeded();
        } else {
            sauverBientot();
        }
    }

    // ------------------------------------------------------------------ /coffres

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    private Component titre() {
        return t("menu.titre", "<dark_red><bold>Coffres de mort");
    }

    private boolean jetons() {
        return getServer().getPluginManager().isPluginEnabled("KS_Jetons");
    }

    /** Temps avant que le badge de la mort du joueur soit de nouveau utilisable ; 0 : disponible ; -1 : pas de badge. */
    private long attenteBadge(UUID joueur) {
        if (!jetons()) {
            return -1;
        }
        int jours = fr.kalium.jetons.KSJetons.valeurBadge(joueur, fr.kalium.jetons.KSJetons.Badge.MORT);
        if (jours <= 0) {
            return -1;
        }
        Long dernier = badgeUtilise.get(joueur);
        return dernier == null ? 0 : Math.max(0, dernier + jours * JOUR - System.currentTimeMillis());
    }

    private long jetonsDeLaMort(UUID joueur) {
        return jetons() ? fr.kalium.jetons.KSJetons.nombre(joueur, fr.kalium.jetons.KSJetons.Type.MORT) : 0;
    }

    private static String temps(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s >= 86400) {
            return s / 86400 + " j " + (s % 86400) / 3600 + " h";
        }
        if (s >= 3600) {
            return s / 3600 + " h " + (s % 3600) / 60 + " min";
        }
        return s / 60 + " min " + s % 60 + " s";
    }

    /** Ses coffres de mort : où ils sont, le temps qu'il reste, les voir, les récupérer. */
    public void ouvrir(Player joueur) {
        UUID id = joueur.getUniqueId();
        List<Coffre> siens = coffresDe(id);
        long maintenant = System.currentTimeMillis();
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        if (siens.isEmpty()) {
            corps.add(t("menu.aucun", "<gray>Aucun coffre de mort actif. À ta mort, tes objets et la moitié de ton "
                    + "expérience vont dans un coffre que toi seul peux ouvrir ou casser, pendant <minutes> minutes.",
                    "minutes", duree() / 60_000L));
        } else {
            corps.add(t("menu.aide", "<gray>Va les chercher avant la fin du temps, ou récupère leur contenu dans "
                    + "/rewards avec un badge ou un jeton de la mort."));
        }
        for (int i = 0; i < siens.size(); i++) {
            Coffre coffre = siens.get(i);
            int n = i + 1;
            corps.add(t("menu.ligne", "<white><n>. <gold><monde></gold> <x> <y> <z> <gray>: disparaît dans "
                            + "<white><temps></white>, <piles> pile(s)", "n", n, "monde", nomMonde(coffre.pos.monde()),
                    "x", coffre.pos.x(), "y", coffre.pos.y(), "z", coffre.pos.z(),
                    "temps", temps(coffre.expiration - maintenant), "piles", contenu(coffre).size()));
            boutons.add(gui.button(t("menu.voir", "<white>Voir le coffre <n>", "n", n), null, p -> apercu(p, coffre.id)));
            boutons.add(gui.button(t("menu.recuperer", "<green>Récupérer le coffre <n>", "n", n), null,
                    p -> confirmer(p, coffre.id)));
        }
        if (jetons()) {
            long attente = attenteBadge(id);
            corps.add(attente < 0 ? t("menu.badge-aucun", "<gold>Badge de la mort</gold> <gray>: aucun")
                    : attente == 0 ? t("menu.badge-pret", "<gold>Badge de la mort</gold> <gray>: <green>disponible")
                    : t("menu.badge-attente", "<gold>Badge de la mort</gold> <gray>: disponible dans <white><temps>",
                    "temps", temps(attente)));
            corps.add(t("menu.jetons", "<aqua>Jetons de la mort</aqua> <gray>: <white><n>", "n", jetonsDeLaMort(id)));
        }
        gui.open(joueur, titre(), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void apercu(Player joueur, String id) {
        Coffre coffre = coffres.get(id);
        if (coffre == null || !coffre.joueur.equals(joueur.getUniqueId())) {
            ouvrir(joueur);
            return;
        }
        Inventory vue = Bukkit.createInventory(new Apercu(), CASES, t("coffre.apercu", "Coffre de mort (aperçu)"));
        vue.setContents(coffre.contenu.getContents().clone());
        for (int i = 0; i < vue.getSize(); i++) {
            ItemStack objet = vue.getItem(i);
            vue.setItem(i, objet == null ? null : objet.clone());
        }
        joueur.openInventory(vue);
        lang.saveIfNeeded();
    }

    private void confirmer(Player joueur, String id) {
        Coffre coffre = coffres.get(id);
        if (coffre == null || !coffre.joueur.equals(joueur.getUniqueId())) {
            ouvrir(joueur);
            return;
        }
        UUID uuid = joueur.getUniqueId();
        boolean badge = attenteBadge(uuid) == 0;
        long jetons = jetonsDeLaMort(uuid);
        if (!badge && jetons < 1) {
            gui.notice(joueur, titre(), t("menu.rien", "<red>Il te faut un jeton de la mort, ou un badge de la mort "
                    + "disponible. Sinon, va chercher ton coffre en <x> <y> <z>.", "x", coffre.pos.x(), "y",
                    coffre.pos.y(), "z", coffre.pos.z()), this::ouvrir);
            lang.saveIfNeeded();
            return;
        }
        Component moyen = badge ? t("menu.avec-badge", "<gold>ton badge de la mort</gold> (gratuit)")
                : t("menu.avec-jeton", "<aqua>1 jeton de la mort</aqua> (tu en as <n>)", "n", jetons);
        gui.confirm(joueur, titre(), t("menu.confirmer", "<white>Le contenu de ce coffre part dans <light_purple>/rewards"
                + "</light_purple> et le coffre disparaît. Tu utilises <moyen>.", "moyen", moyen),
                p -> recuperer(p, id), this::ouvrir);
        lang.saveIfNeeded();
    }

    /** Récupération certaine : contenu dans /rewards, coffre retiré ; badge de la mort d'abord, sinon un jeton. */
    private void recuperer(Player joueur, String id) {
        Coffre coffre = coffres.get(id);
        UUID uuid = joueur.getUniqueId();
        if (coffre == null || !coffre.joueur.equals(uuid)) {
            ouvrir(joueur);
            return;
        }
        boolean badge = attenteBadge(uuid) == 0;
        if (!badge && jetonsDeLaMort(uuid) < 1) {
            ouvrir(joueur);
            return;
        }
        List<ItemStack> objets = contenu(coffre);
        if (objets.isEmpty()) {
            // Rien à récupérer : ni badge ni jeton n'est utilisé.
            retirer(coffre);
            ouvrir(joueur);
            return;
        }
        if (!versRewards(joueur, objets, "Coffre récupéré le " + LocalDateTime.now(ZoneId.of(
                "Europe/Paris")).format(DATE))) {
            gui.notice(joueur, titre(), t("menu.sans-rewards", "<red>Les récompenses sont indisponibles : coffre gardé."),
                    this::ouvrir);
            lang.saveIfNeeded();
            return;
        }
        if (badge) {
            badgeUtilise.put(uuid, System.currentTimeMillis());
        } else {
            fr.kalium.jetons.KSJetons.consommer(uuid, fr.kalium.jetons.KSJetons.Type.MORT, 1);
        }
        retirer(coffre);
        getLogger().info(joueur.getName() + " récupère son coffre de mort en " + coffre.pos + " avec "
                + (badge ? "son badge de la mort." : "un jeton de la mort."));
        gui.notice(joueur, titre(), t("menu.recupere", "<green>Le contenu de ton coffre t'attend dans <white>/rewards<green>."),
                this::ouvrir);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection badges = yaml.getConfigurationSection("badge");
        if (badges != null) {
            for (String cle : badges.getKeys(false)) {
                try {
                    badgeUtilise.put(UUID.fromString(cle), badges.getLong(cle));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Délai de badge ignoré : " + cle);
                }
            }
        }
        ConfigurationSection section = yaml.getConfigurationSection("coffres");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            try {
                Coffre coffre = new Coffre();
                coffre.id = id;
                coffre.joueur = UUID.fromString(s.getString("joueur", ""));
                coffre.nom = s.getString("nom", "?");
                coffre.pos = new Pos(s.getString("monde", ""), s.getInt("x"), s.getInt("y"), s.getInt("z"));
                coffre.expiration = s.getLong("expiration");
                coffre.eau = s.getBoolean("eau");
                coffre.contenu = inventaire(id);
                for (String donnees : s.getStringList("contenu")) {
                    coffre.contenu.addItem(ItemStack.deserializeBytes(Base64.getDecoder().decode(donnees)));
                }
                coffres.put(id, coffre);
                parPosition.put(coffre.pos, coffre);
            } catch (RuntimeException e) {
                getLogger().warning("Coffre de mort illisible, ignoré : " + id + " (" + e.getMessage() + ")");
            }
        }
    }

    /** Enregistre au tick suivant (une seule fois, même après plusieurs changements). */
    private void sauverBientot() {
        if (aSauver) {
            return;
        }
        if (!isEnabled()) {
            sauver();
            return;
        }
        aSauver = true;
        getServer().getScheduler().runTask(this, () -> {
            aSauver = false;
            sauver();
        });
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        badgeUtilise.forEach((joueur, date) -> yaml.set("badge." + joueur, date));
        for (Coffre coffre : coffres.values()) {
            String chemin = "coffres." + coffre.id + ".";
            yaml.set(chemin + "joueur", coffre.joueur.toString());
            yaml.set(chemin + "nom", coffre.nom);
            yaml.set(chemin + "monde", coffre.pos.monde());
            yaml.set(chemin + "x", coffre.pos.x());
            yaml.set(chemin + "y", coffre.pos.y());
            yaml.set(chemin + "z", coffre.pos.z());
            yaml.set(chemin + "expiration", coffre.expiration);
            yaml.set(chemin + "eau", coffre.eau);
            List<String> donnees = new ArrayList<>();
            for (ItemStack objet : contenu(coffre)) {
                donnees.add(Base64.getEncoder().encodeToString(objet.serializeAsBytes()));
            }
            yaml.set(chemin + "contenu", donnees);
        }
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer coffres.yml : " + e.getMessage());
        }
    }
}
