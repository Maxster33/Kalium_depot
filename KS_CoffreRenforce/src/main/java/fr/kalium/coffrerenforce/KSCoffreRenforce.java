package fr.kalium.coffrerenforce;

import fr.kalium.coffrerenforce.Stockage.Stock;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
 *
 * Le bloc est un vrai coffre (visible sur Bedrock), l'objet un coffre nommé : aucune apparence à ajouter sur le proxy.
 * Autres plugins : creerCoffre() (KS_Crafts) ; estCoffreRenforce, objet, nombre, retirer, ajouter (boutiques).
 */
public final class KSCoffreRenforce extends JavaPlugin implements Listener {

    private static KSCoffreRenforce instance;
    private NamespacedKey cle;
    private Lang lang;
    private Stockage stockage;
    private MenuCoffre menu;
    /** Coffres touchés par un entonnoir pendant ce tick, remis en ordre au tick suivant. */
    private final Set<Block> aRanger = new LinkedHashSet<>();

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

    // ------------------------------------------------------------------ l'objet

    /** Un coffre renforcé (objet). Nécessite que le plugin soit activé. */
    public static ItemStack creerCoffre() {
        ItemStack objet = new ItemStack(Material.CHEST);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(instance.lang.c("objet.nom", "<gold>Coffre renforcé")
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(Contenant.lignes(instance.lang.c("objet.description",
                "<gray>Stocke jusqu'à 1 000 stacks d'un seul objet. Se remplit et se vide par entonnoir.")).stream()
                .map(l -> l.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
        meta.getPersistentDataContainer().set(instance.cle, PersistentDataType.BYTE, (byte) 1);
        objet.setItemMeta(meta);
        instance.lang.saveIfNeeded();
        return objet;
    }

    private boolean estObjet(ItemStack objet) {
        return objet != null && objet.getType() == Material.CHEST && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(cle);
    }

    // ------------------------------------------------------------------ API (boutiques)

    public static boolean estCoffreRenforce(Block bloc) {
        return instance != null && instance.stockage.est(bloc);
    }

    /** Objet stocké (un exemplaire), ou null si le coffre est vide. */
    public static ItemStack objet(Block bloc) {
        Stock stock = instance.stockage.apercu(bloc);
        return stock.modele() == null ? null : stock.modele().clone();
    }

    /** Nombre d'objets stockés. */
    public static long nombre(Block bloc) {
        return instance.stockage.apercu(bloc).total();
    }

    /** Retire jusqu'à n objets du coffre ; renvoie le nombre retiré (à donner au joueur). */
    public static long retirer(Block bloc, long n) {
        Stock stock = instance.stockage.lire(bloc);
        long pris = Math.max(0, Math.min(n, stock.total()));
        if (pris > 0) {
            instance.stockage.ecrire(bloc, new Stock(stock.modele(), stock.total() - pris));
        }
        return pris;
    }

    /** Ajoute jusqu'à n exemplaires de l'objet ; renvoie le nombre ajouté (0 : autre objet, refusé ou plein). */
    public static long ajouter(Block bloc, ItemStack objet, long n) {
        if (!Stockage.accepte(objet)) {
            return 0;
        }
        Stock stock = instance.stockage.lire(bloc);
        ItemStack modele = stock.modele() == null ? objet.asOne() : stock.modele();
        if (!modele.isSimilar(objet)) {
            return 0;
        }
        long mis = Math.max(0, Math.min(n, Stock.capacite(modele) - stock.total()));
        if (mis > 0) {
            instance.stockage.ecrire(bloc, new Stock(modele, stock.total() + mis));
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
        Block bloc = stockage.bloc(event.getInventory());
        if (bloc == null && event.getInventory().getHolder(false) instanceof org.bukkit.block.DoubleChest dc) {
            // Coffre double formé malgré tout (déjà là avant le plugin, par exemple) : séparé, puis le menu.
            for (org.bukkit.inventory.InventoryHolder cote : new org.bukkit.inventory.InventoryHolder[]{
                    dc.getLeftSide(false), dc.getRightSide(false)}) {
                if (cote instanceof org.bukkit.block.Chest moitie && stockage.est(moitie.getBlock())) {
                    bloc = moitie.getBlock();
                }
            }
            if (bloc != null) {
                stockage.isolerAutour(bloc);
            }
        }
        if (bloc == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getPlayer() instanceof Player joueur && joueur.getGameMode() != GameMode.SPECTATOR) {
            Block ouvert = bloc;
            Bukkit.getScheduler().runTask(this, () -> menu.ouvrir(joueur, ouvert));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCasser(BlockBreakEvent event) {
        Block bloc = event.getBlock();
        if (!stockage.est(bloc)) {
            return;
        }
        Stock stock = stockage.lire(bloc);
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

    // ------------------------------------------------------------------ entonnoirs

    /** Un entonnoir ne met dans le coffre que l'objet stocké (n'importe quel objet accepté s'il est vide). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntonnoir(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (destination.getType() != InventoryType.CHEST) {
            return;
        }
        Block bloc = stockage.bloc(destination);
        if (bloc == null) {
            return;
        }
        ItemStack objet = event.getItem();
        ItemStack modele = stockage.apercu(bloc).modele();
        if (!Stockage.accepte(objet) || (modele != null && !modele.isSimilar(objet))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntonnoirPasse(InventoryMoveItemEvent event) {
        for (Inventory inventaire : new Inventory[]{event.getSource(), event.getDestination()}) {
            if (inventaire.getType() == InventoryType.CHEST) {
                Block bloc = stockage.bloc(inventaire);
                if (bloc != null && aRanger.add(bloc) && aRanger.size() == 1) {
                    Bukkit.getScheduler().runTask(this, this::ranger);
                }
            }
        }
    }

    /** Tampon recalculé (voir Stockage) pour chaque coffre touché par un entonnoir. */
    private void ranger() {
        for (Block bloc : List.copyOf(aRanger)) {
            if (stockage.est(bloc)) {
                stockage.ecrire(bloc, stockage.lire(bloc));
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
            Block bloc = joueur.getTargetBlockExact(5);
            if (bloc == null || !stockage.est(bloc)) {
                continue;
            }
            Stock stock = stockage.apercu(bloc);
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
