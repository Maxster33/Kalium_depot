package fr.kalium.coffrerenforce;

import fr.kalium.coffrerenforce.Stockage.Stock;
import fr.kalium.coffrerenforce.Stockage.SurBloc;
import fr.kalium.coffrerenforce.Stockage.SurVehicule;
import fr.kalium.coffrerenforce.Stockage.Support;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * KS_CoffreRenforce (LeKiwi06, 25/10/2026 : « ma propre version du drawer : le coffre renforcé ») : un coffre qui
 * stocke jusqu'à 1 000 stacks d'un seul objet, sur Kixster.
 *
 * - Craft (KS_Crafts) : un coffre au centre, 4 blocs de fer sur les côtés, 4 fragments de spawner aux coins.
 * - Clic droit : un menu en coffre (objet, quantité, retirer 1 / 1 stack / remplir l'inventaire, tout déposer ; un
 *   clic sur un objet de son inventaire le dépose). Les claims s'appliquent comme pour un coffre normal (le menu ne
 *   s'ouvre que si le coffre aurait pu s'ouvrir).
 * - Redstone : les entonnoirs le remplissent et le vident, un comparateur donne son remplissage (voir {@link Stockage}).
 * - Jamais de coffre double : un coffre posé à côté reste un coffre simple.
 * - Cassable seulement vide ; les explosions ne le détruisent pas. Une fois vide, il accepte n'importe quel objet.
 * - Refuse les shulkers et les sacs qui contiennent quelque chose.
 * - En le visant, la barre au-dessus des objets affiche son contenu.
 * - 1.1.0 (LeKiwi06, 27/10/2026 : « peux-tu faire le bateau et le minecart avec coffre renforcé ») : bateau (radeau)
 *   et minecart avec coffre renforcé, craftés comme les vanilla (bateau ou minecart + coffre renforcé) ; même menu,
 *   mêmes entonnoirs ; cassables seulement avec 27 stacks ou moins dedans (le contenu tombe au sol). Le minecart sert
 *   de wagon dans un train de KS_Trains (API à entité ci-dessous).
 *
 * Le bloc est un vrai coffre (visible sur Bedrock), l'objet un coffre nommé : aucune apparence à ajouter sur le proxy.
 * Les véhicules sont de vrais bateaux et minecarts à coffre, leurs objets des objets vanilla nommés : idem.
 * Autres plugins : creerCoffre() (KS_Crafts) ; estCoffreRenforce, objet, nombre, retirer, ajouter (boutiques, et
 * pour un véhicule : KS_Trains) ; creerMinecart (KS_Trains).
 */
public final class KSCoffreRenforce extends JavaPlugin implements Listener {

    /** 1.1.0 (LeKiwi06 : « il est cassable s'il contient moins de 27 stacks ») : ce que contient un coffre normal. */
    static final int STACKS_CASSABLE = 27;

    private static KSCoffreRenforce instance;
    private NamespacedKey cle;
    private Lang lang;
    private Stockage stockage;
    private MenuCoffre menu;
    /** Coffres touchés par un entonnoir pendant ce tick, remis en ordre au tick suivant. */
    private final Set<Support> aRanger = new LinkedHashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        cle = new NamespacedKey(this, "coffre");
        lang = new Lang(this);
        stockage = new Stockage(cle, new NamespacedKey(this, "objet"), new NamespacedKey(this, "reserve"));
        menu = new MenuCoffre(this, lang, stockage);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getScheduler().runTaskTimer(this, this::afficherVises, 20L, 10L);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        ranger();
    }

    // ------------------------------------------------------------------ les objets

    /** Un coffre renforcé (objet). Nécessite que le plugin soit activé. */
    public static ItemStack creerCoffre() {
        return instance.objet(Material.CHEST, instance.lang.c("objet.nom", "<gold>Coffre renforcé"));
    }

    /** 1.1.0 : bateau (ou radeau) avec coffre renforcé, du bois du bateau à coffre donné. */
    public static ItemStack creerBateau(Material bateau) {
        return instance.objet(bateau, bateau == Material.BAMBOO_CHEST_RAFT
                ? instance.lang.c("objet.radeau", "<gold>Radeau avec coffre renforcé")
                : instance.lang.c("objet.bateau", "<gold>Bateau avec coffre renforcé"));
    }

    /** 1.1.0 : minecart avec coffre renforcé. */
    public static ItemStack creerMinecart() {
        return instance.objet(Material.CHEST_MINECART,
                instance.lang.c("objet.minecart", "<gold>Minecart avec coffre renforcé"));
    }

    private ItemStack objet(Material type, Component nom) {
        ItemStack objet = new ItemStack(type);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        meta.lore(Contenant.lignes(lang.c("objet.description",
                "<gray>Stocke jusqu'à 1 000 stacks d'un seul objet. Se remplit et se vide par entonnoir.")).stream()
                .map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
        meta.getPersistentDataContainer().set(cle, PersistentDataType.BYTE, (byte) 1);
        objet.setItemMeta(meta);
        lang.saveIfNeeded();
        return objet;
    }

    private boolean marque(ItemStack objet) {
        return objet != null && objet.hasItemMeta() && objet.getItemMeta().getPersistentDataContainer().has(cle);
    }

    private boolean estObjet(ItemStack objet) {
        return objet != null && objet.getType() == Material.CHEST && marque(objet);
    }

    /** 1.1.0 : objet bateau, radeau ou minecart avec coffre renforcé. */
    private boolean estVehiculeObjet(ItemStack objet) {
        return objet != null && (objet.getType() == Material.CHEST_MINECART
                || Tag.ITEMS_CHEST_BOATS.isTagged(objet.getType())) && marque(objet);
    }

    // ------------------------------------------------------------------ API (boutiques, KS_Trains)

    public static boolean estCoffreRenforce(Block bloc) {
        return instance != null && instance.stockage.est(bloc);
    }

    /** 1.1.0 : bateau ou minecart avec coffre renforcé. */
    public static boolean estCoffreRenforce(Entity vehicule) {
        return instance != null && instance.stockage.est(vehicule);
    }

    /** Objet stocké (un exemplaire), ou null si le coffre est vide. */
    public static ItemStack objet(Block bloc) {
        return objet(new SurBloc(bloc));
    }

    public static ItemStack objet(Entity vehicule) {
        return objet(new SurVehicule(vehicule));
    }

    private static ItemStack objet(Support support) {
        Stock stock = instance.stockage.apercu(support);
        return stock.modele() == null ? null : stock.modele().clone();
    }

    /** Nombre d'objets stockés. */
    public static long nombre(Block bloc) {
        return instance.stockage.apercu(new SurBloc(bloc)).total();
    }

    public static long nombre(Entity vehicule) {
        return instance.stockage.apercu(new SurVehicule(vehicule)).total();
    }

    /** Retire jusqu'à n objets du coffre ; renvoie le nombre retiré (à donner au joueur). */
    public static long retirer(Block bloc, long n) {
        return retirer(new SurBloc(bloc), n);
    }

    public static long retirer(Entity vehicule, long n) {
        return retirer(new SurVehicule(vehicule), n);
    }

    private static long retirer(Support support, long n) {
        Stock stock = instance.stockage.lire(support);
        long pris = Math.max(0, Math.min(n, stock.total()));
        if (pris > 0) {
            instance.stockage.ecrire(support, new Stock(stock.modele(), stock.total() - pris));
        }
        return pris;
    }

    /** Ajoute jusqu'à n exemplaires de l'objet ; renvoie le nombre ajouté (0 : autre objet, refusé ou plein). */
    public static long ajouter(Block bloc, ItemStack objet, long n) {
        return ajouter(new SurBloc(bloc), objet, n);
    }

    public static long ajouter(Entity vehicule, ItemStack objet, long n) {
        return ajouter(new SurVehicule(vehicule), objet, n);
    }

    private static long ajouter(Support support, ItemStack objet, long n) {
        if (!Stockage.accepte(objet)) {
            return 0;
        }
        Stock stock = instance.stockage.lire(support);
        ItemStack modele = stock.modele() == null ? objet.asOne() : stock.modele();
        if (!modele.isSimilar(objet)) {
            return 0;
        }
        long mis = Math.max(0, Math.min(n, Stock.capacite(modele) - stock.total()));
        if (mis > 0) {
            instance.stockage.ecrire(support, new Stock(modele, stock.total() + mis));
        }
        return mis;
    }

    // ------------------------------------------------------------------ poser, ouvrir, casser

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPoser(BlockPlaceEvent event) {
        Block bloc = event.getBlockPlaced();
        if (bloc.getType() != Material.CHEST) {
            return;
        }
        if (estObjet(event.getItemInHand())) {
            stockage.marquer(bloc);
        }
        // Les coffres voisins se collent après l'événement : on les sépare au tick suivant.
        Bukkit.getScheduler().runTask(this, () -> stockage.isolerAutour(bloc));
    }

    /** Le vrai coffre ne s'ouvre jamais : le menu à la place (seulement si le coffre aurait pu s'ouvrir). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOuvrir(InventoryOpenEvent event) {
        Support support = stockage.support(event.getInventory());
        if (support == null && event.getInventory().getHolder(false) instanceof org.bukkit.block.DoubleChest dc) {
            // Coffre double formé malgré tout (déjà là avant le plugin, par exemple) : séparé, puis le menu.
            for (org.bukkit.inventory.InventoryHolder cote : new org.bukkit.inventory.InventoryHolder[]{
                    dc.getLeftSide(false), dc.getRightSide(false)}) {
                if (cote instanceof org.bukkit.block.Chest moitie && stockage.est(moitie.getBlock())) {
                    support = new SurBloc(moitie.getBlock());
                }
            }
            if (support instanceof SurBloc s) {
                stockage.isolerAutour(s.bloc());
            }
        }
        if (support == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getPlayer() instanceof Player joueur && joueur.getGameMode() != GameMode.SPECTATOR) {
            Support ouvert = support;
            Bukkit.getScheduler().runTask(this, () -> menu.ouvrir(joueur, ouvert));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCasser(BlockBreakEvent event) {
        Block bloc = event.getBlock();
        if (!stockage.est(bloc)) {
            return;
        }
        Stock stock = stockage.lire(new SurBloc(bloc));
        if (stock.total() > 0) {
            event.setCancelled(true);
            Component texte = lang.c("message.casser-plein",
                    "<red>Vide le coffre renforcé avant de le casser (<n> x <objet> dedans).",
                    "n", nombre(stock.total()), "objet", stock.modele().effectiveName());
            event.getPlayer().sendMessage(texte);
            Contenant.message(event.getPlayer(), texte, true);
            lang.saveIfNeeded();
            return;
        }
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            bloc.getWorld().dropItemNaturally(bloc.getLocation().add(0.5, 0.5, 0.5), creerCoffre());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionEntite(EntityExplodeEvent event) {
        event.blockList().removeIf(stockage::est);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionBloc(BlockExplodeEvent event) {
        event.blockList().removeIf(stockage::est);
    }

    /** Wither, dragon de l'Ender... : le coffre renforcé n'est pas détruit. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntiteBloc(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Player) && stockage.est(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ véhicules (1.1.0)

    /**
     * Craft « comme le vanilla » : la recette du jeu d'un bateau à coffre (de chaque bois, radeau compris) ou d'un
     * minecart à coffre, faite avec un coffre renforcé, donne le véhicule avec coffre renforcé (avant : un véhicule à
     * coffre ordinaire, et le coffre renforcé était perdu). Un véhicule avec coffre renforcé ne sert d'ingrédient à
     * aucune recette (bateau à jukebox de KS_JukeboxBoat compris : le coffre renforcé serait perdu).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreparerCraft(PrepareItemCraftEvent event) {
        ItemStack resultat = craftCorrige(event.getRecipe(), event.getInventory().getMatrix());
        if (resultat != null) {
            event.getInventory().setResult(resultat.isEmpty() ? null : resultat);
        }
    }

    /** Même chose dans un crafter (établi automatique), qui ne passe pas par onPreparerCraft. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (!(event.getBlock().getState(false) instanceof org.bukkit.block.Crafter crafter)) {
            return;
        }
        ItemStack resultat = craftCorrige(event.getRecipe(), crafter.getInventory().getContents());
        if (resultat == null) {
            return;
        }
        if (resultat.isEmpty()) {
            event.setCancelled(true);
        } else {
            event.setResult(resultat);
        }
    }

    /** Résultat corrigé, objet vide si le craft est refusé, null si ce craft n'est pas concerné. */
    private ItemStack craftCorrige(Recipe recette, ItemStack[] matrice) {
        boolean coffre = false;
        for (ItemStack objet : matrice) {
            if (estVehiculeObjet(objet)) {
                return ItemStack.empty();
            }
            coffre |= estObjet(objet);
        }
        if (!coffre || !(recette instanceof Keyed k) || !k.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) {
            return null;
        }
        Material type = recette.getResult().getType();
        if (type == Material.CHEST_MINECART) {
            return creerMinecart();
        }
        return Tag.ITEMS_CHEST_BOATS.isTagged(type) ? creerBateau(type) : null;
    }

    /** Un distributeur poserait un véhicule à coffre ordinaire : le véhicule avec coffre renforcé y reste. */
    @EventHandler(ignoreCancelled = true)
    public void onDistribuer(BlockDispenseEvent event) {
        if (estVehiculeObjet(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Après les protections (KS_Claim rend la pose possible partout, en priorité HIGHEST). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPoserVehicule(EntityPlaceEvent event) {
        Player joueur = event.getPlayer();
        EquipmentSlot main = event.getHand();
        if (joueur != null && main != null && estVehiculeObjet(joueur.getInventory().getItem(main))
                && (event.getEntity() instanceof org.bukkit.entity.ChestBoat
                || event.getEntity() instanceof org.bukkit.entity.minecart.StorageMinecart)) {
            stockage.marquer(event.getEntity());
        }
    }

    /** Plus de 27 stacks dedans : le véhicule ne se casse pas (coups, explosions). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCasserVehicule(VehicleDestroyEvent event) {
        if (!stockage.est(event.getVehicle())) {
            return;
        }
        Stock stock = stockage.lire(new SurVehicule(event.getVehicle()));
        if (stock.stacks() <= STACKS_CASSABLE) {
            return;
        }
        event.setCancelled(true);
        if (event.getAttacker() instanceof Player joueur) {
            Component texte = lang.c("message.casser-vehicule",
                    "<red>Vide-le jusqu'à <max> stacks avant de le casser (<stacks> stacks de <objet> dedans).",
                    "max", nombre(STACKS_CASSABLE), "stacks", nombre(stock.stacks()),
                    "objet", stock.modele().effectiveName());
            Contenant.message(joueur, texte, true);
            lang.saveIfNeeded();
        }
    }

    /**
     * Le véhicule se casse pour de bon : tout son stock tombe au sol (le jeu, lui, ne lâcherait que le tampon) ; le
     * tampon vidé, le jeu ne lâche plus rien du coffre.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehiculeCasse(VehicleDestroyEvent event) {
        if (!stockage.est(event.getVehicle())) {
            return;
        }
        SurVehicule support = new SurVehicule(event.getVehicle());
        Stock stock = stockage.lire(support);
        stockage.ecrire(support, new Stock(null, 0));
        Location lieu = event.getVehicle().getLocation();
        long reste = stock.total();
        while (reste > 0) {
            int n = (int) Math.min(reste, stock.pile());
            lieu.getWorld().dropItemNaturally(lieu, stock.modele().asQuantity(n));
            reste -= n;
        }
    }

    /** Le véhicule cassé redonne le véhicule avec coffre renforcé (avec son nom s'il a été renommé). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLacherVehicule(EntityDropItemEvent event) {
        Entity vehicule = event.getEntity();
        if (!stockage.est(vehicule)) {
            return;
        }
        Item lache = event.getItemDrop();
        Material type = lache.getItemStack().getType();
        ItemStack objet;
        if (vehicule instanceof Boat bateau && type == bateau.getBoatMaterial()) {
            objet = creerBateau(type);
        } else if (vehicule instanceof Minecart wagon && type == wagon.getMinecartMaterial()) {
            objet = creerMinecart();
        } else if (type == Material.CHEST) {
            // Au cas où le jeu lâcherait le coffre à part.
            objet = creerCoffre();
        } else {
            return;
        }
        Component nom = vehicule.customName();
        if (nom != null && type != Material.CHEST) {
            ItemMeta meta = objet.getItemMeta();
            meta.displayName(nom);
            objet.setItemMeta(meta);
        }
        lache.setItemStack(objet);
    }

    // ------------------------------------------------------------------ entonnoirs

    /** Un entonnoir ne met dans le coffre que l'objet stocké (n'importe quel objet accepté s'il est vide). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntonnoir(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (destination.getType() != InventoryType.CHEST) {
            return;
        }
        Support support = stockage.support(destination);
        if (support == null) {
            return;
        }
        ItemStack objet = event.getItem();
        ItemStack modele = stockage.apercu(support).modele();
        if (!Stockage.accepte(objet) || (modele != null && !modele.isSimilar(objet))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntonnoirPasse(InventoryMoveItemEvent event) {
        for (Inventory inventaire : new Inventory[]{event.getSource(), event.getDestination()}) {
            if (inventaire.getType() == InventoryType.CHEST) {
                Support support = stockage.support(inventaire);
                if (support != null && aRanger.add(support) && aRanger.size() == 1) {
                    Bukkit.getScheduler().runTask(this, this::ranger);
                }
            }
        }
    }

    /** Tampon recalculé (voir Stockage) pour chaque coffre touché par un entonnoir. */
    private void ranger() {
        for (Support support : List.copyOf(aRanger)) {
            if (stockage.est(support)) {
                stockage.ecrire(support, stockage.lire(support));
            }
        }
        aRanger.clear();
    }

    // ------------------------------------------------------------------ contenu affiché en le visant

    private void afficherVises() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            InventoryType ouvert = joueur.getOpenInventory().getType();
            if ((ouvert != InventoryType.CRAFTING && ouvert != InventoryType.CREATIVE)
                    || joueur.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            Support support = vise(joueur);
            if (support == null) {
                continue;
            }
            Stock stock = stockage.apercu(support);
            joueur.sendActionBar(stock.modele() == null
                    ? lang.c("vise.vide", "<gold>Coffre renforcé</gold> <gray>: vide (0 / <max> stacks)",
                            "max", nombre(Stockage.STACKS))
                    : lang.c("vise.plein",
                            "<gold>Coffre renforcé</gold> <gray>:</gray> <aqua><n> x <objet></aqua> <gray>(<stacks> / <max> stacks)",
                            "n", nombre(stock.total()), "objet", stock.modele().effectiveName(),
                            "stacks", nombre(stock.stacks()), "max", nombre(Stockage.STACKS)));
        }
        lang.saveIfNeeded();
    }

    /** Coffre renforcé visé à 5 blocs : un bloc, ou 1.1.0 un bateau ou un minecart. */
    private Support vise(Player joueur) {
        Block bloc = joueur.getTargetBlockExact(5);
        if (bloc != null && stockage.est(bloc)) {
            return new SurBloc(bloc);
        }
        Entity entite = joueur.getTargetEntity(5);
        return entite != null && stockage.est(entite) ? new SurVehicule(entite) : null;
    }

    /** 12345 -> « 12 345 ». */
    static String nombre(long n) {
        String chiffres = Long.toString(n);
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < chiffres.length(); i++) {
            if (i > 0 && (chiffres.length() - i) % 3 == 0) {
                texte.append(' ');
            }
            texte.append(chiffres.charAt(i));
        }
        return texte.toString();
    }
}
