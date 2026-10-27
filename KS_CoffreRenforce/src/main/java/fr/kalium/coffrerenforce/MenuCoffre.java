package fr.kalium.coffrerenforce;

import fr.kalium.coffrerenforce.Stockage.Stock;
import fr.kalium.coffrerenforce.Stockage.Support;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menu d'un coffre renforcé (maquette validée par LeKiwi06 le 25/10/2026), coffre de 3 rangées :
 * - en haut au centre : l'objet stocké, sa quantité et son nombre de stacks (vitre blanche « Vide ») ;
 * - au milieu : l'objet x1 « Retirer 1 », l'objet x64 « Retirer 1 stack », un coffre « Remplir mon inventaire »,
 *   un entonnoir « Tout déposer » (coffre vide : l'objet tenu en main choisit ce qu'il stocke) ;
 * - en bas : livre d'aide, barrière « Fermer ».
 * Un clic sur un objet de son inventaire (en bas de l'écran) le dépose.
 * 1.1.0 : le même menu pour le bateau et le minecart avec coffre renforcé (seule l'aide change).
 */
final class MenuCoffre implements Listener {

    private final JavaPlugin plugin;
    private final Lang lang;
    private final Stockage stockage;
    /** Coffre renforcé dont chaque joueur a le menu ouvert. */
    private final Map<UUID, Support> ouverts = new HashMap<>();

    MenuCoffre(JavaPlugin plugin, Lang lang, Stockage stockage) {
        this.plugin = plugin;
        this.lang = lang;
        this.stockage = stockage;
    }

    /** Ouvre le menu, ou le remplit à nouveau s'il est déjà ouvert. */
    void ouvrir(Player joueur, Support support) {
        if (!joueur.isOnline()) {
            return;
        }
        if (!stockage.est(support)) {
            ouverts.remove(joueur.getUniqueId());
            Contenant.fermer(joueur);
            return;
        }
        Stock stock = stockage.lire(support);
        Contenant c = Contenant.pour(joueur, 3, lang.c("menu.titre", "Coffre renforcé"), support.marque());
        for (int place = 0; place < c.taille(); place++) {
            c.poser(place, Contenant.decor(), null);
        }
        ItemStack modele = stock.modele();
        if (modele == null) {
            c.poser(4, Contenant.objet(Material.WHITE_STAINED_GLASS_PANE, lang.c("menu.vide", "<white>Vide"),
                    Contenant.lignes(lang.c("menu.vide-info",
                            "<gray>Accepte n'importe quel objet, jusqu'à 1 000 stacks."))), null);
        } else {
            c.poser(4, Contenant.objet(modele, null, List.of(
                    lang.c("menu.quantite", "<white>Quantité : <aqua><n>", "n", KSCoffreRenforce.nombre(stock.total())),
                    lang.c("menu.stacks", "<gray><stacks> / <max> stacks", "stacks",
                            KSCoffreRenforce.nombre(stock.stacks()), "max", KSCoffreRenforce.nombre(Stockage.STACKS)))),
                    null);
            c.poser(10, Contenant.objet(modele.asQuantity(1), lang.c("menu.retirer-1", "<yellow>Retirer 1"), List.of()),
                    j -> retirer(j, support, 1));
            c.poser(11, Contenant.objet(modele.asQuantity(modele.getMaxStackSize()),
                    lang.c("menu.retirer-stack", "<yellow>Retirer 1 stack"), List.of()),
                    j -> retirer(j, support, modele.getMaxStackSize()));
            c.poser(12, Contenant.objet(Material.CHEST, lang.c("menu.remplir", "<yellow>Remplir mon inventaire"),
                    Contenant.lignes(lang.c("menu.remplir-info",
                            "<gray>Autant que ton inventaire peut en prendre."))),
                    j -> retirer(j, support, Long.MAX_VALUE));
        }
        List<Component> deposer = new ArrayList<>(Contenant.lignes(lang.c("menu.deposer-info",
                "<gray>Dépose tous les objets identiques de ton inventaire.")));
        if (modele == null) {
            deposer.addAll(Contenant.lignes(lang.c("menu.deposer-vide",
                    "<gray>Coffre vide : l'objet tenu en main choisit ce qu'il stocke.")));
        }
        deposer.addAll(Contenant.lignes(lang.c("menu.deposer-clic",
                "<gray>Ou clique un objet de ton inventaire pour le déposer.")));
        c.poser(14, Contenant.objet(Material.HOPPER, lang.c("menu.deposer", "<green>Tout déposer"), deposer),
                j -> toutDeposer(j, support));
        c.barre();
        List<Component> aide = new ArrayList<>();
        aide.addAll(Contenant.lignes(lang.c("menu.aide-stock", "<gray>Stocke jusqu'à 1 000 stacks d'un seul objet.")));
        if (support instanceof Stockage.SurVehicule) {
            aide.addAll(Contenant.lignes(lang.c("menu.aide-redstone-vehicule",
                    "<gray>Les entonnoirs le remplissent et le vident.")));
            aide.addAll(Contenant.lignes(lang.c("menu.aide-regles-vehicule",
                    "<gray>Il ne se casse qu'avec <max> stacks ou moins dedans : son contenu tombe alors au sol.",
                    "max", KSCoffreRenforce.nombre(KSCoffreRenforce.STACKS_CASSABLE))));
        } else {
            aide.addAll(Contenant.lignes(lang.c("menu.aide-redstone",
                    "<gray>Les entonnoirs le remplissent et le vident ; un comparateur donne son remplissage.")));
            aide.addAll(Contenant.lignes(lang.c("menu.aide-regles",
                    "<gray>Jamais de coffre double. Il ne se casse que vide, et redevient libre une fois vide.")));
        }
        aide.addAll(Contenant.lignes(lang.c("menu.aide-refus",
                "<gray>Refuse les shulkers et les sacs qui contiennent quelque chose.")));
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, lang.c("menu.aide", "<gold>Coffre renforcé"), aide), null);
        c.poser(c.bas(5), Contenant.objet(Material.BARRIER, lang.c("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        ouverts.put(joueur.getUniqueId(), support);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ retirer

    private void retirer(Player joueur, Support support, long voulu) {
        if (!stockage.est(support)) {
            Contenant.fermer(joueur);
            return;
        }
        Stock stock = stockage.lire(support);
        if (stock.modele() == null) {
            message(joueur, lang.c("message.vide", "<red>Le coffre renforcé est vide."), true);
            ouvrir(joueur, support);
            return;
        }
        long n = Math.min(voulu, Math.min(stock.total(), place(joueur.getInventory(), stock.modele())));
        if (n <= 0) {
            message(joueur, lang.c("message.inventaire-plein", "<red>Ton inventaire est plein."), true);
            return;
        }
        stockage.ecrire(support, new Stock(stock.modele(), stock.total() - n));
        donner(joueur, stock.modele(), n);
        message(joueur, lang.c("message.retire", "<green><n> x <objet> retiré(s).", "n", KSCoffreRenforce.nombre(n),
                "objet", stock.modele().effectiveName()), false);
        ouvrir(joueur, support);
    }

    /** Combien d'exemplaires de l'objet tiennent dans l'inventaire (sans la main secondaire ni l'armure). */
    private static long place(PlayerInventory inventaire, ItemStack modele) {
        long place = 0;
        for (ItemStack objet : inventaire.getStorageContents()) {
            if (objet == null || objet.getType().isAir()) {
                place += modele.getMaxStackSize();
            } else if (modele.isSimilar(objet)) {
                place += Math.max(0, modele.getMaxStackSize() - objet.getAmount());
            }
        }
        return place;
    }

    private static void donner(Player joueur, ItemStack modele, long n) {
        List<ItemStack> piles = new ArrayList<>();
        while (n > 0) {
            int pile = (int) Math.min(n, modele.getMaxStackSize());
            piles.add(modele.asQuantity(pile));
            n -= pile;
        }
        for (ItemStack reste : joueur.getInventory().addItem(piles.toArray(new ItemStack[0])).values()) {
            joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
        }
    }

    // ------------------------------------------------------------------ déposer

    private void toutDeposer(Player joueur, Support support) {
        if (!stockage.est(support)) {
            Contenant.fermer(joueur);
            return;
        }
        Stock stock = stockage.lire(support);
        ItemStack modele = stock.modele();
        if (modele == null) {
            ItemStack main = joueur.getInventory().getItemInMainHand();
            if (main.getType().isAir()) {
                message(joueur, lang.c("message.tenir",
                        "<red>Tiens en main l'objet à stocker, ou clique-le dans ton inventaire."), true);
                return;
            }
            if (!Stockage.accepte(main)) {
                message(joueur, refus(), true);
                return;
            }
            modele = main.asOne();
        }
        PlayerInventory inventaire = joueur.getInventory();
        long place = Stock.capacite(modele) - stock.total();
        long mis = 0;
        ItemStack[] contenu = inventaire.getStorageContents();
        for (int i = 0; i < contenu.length && mis < place; i++) {
            ItemStack objet = contenu[i];
            if (objet != null && modele.isSimilar(objet)) {
                int n = (int) Math.min(objet.getAmount(), place - mis);
                mis += n;
                objet.setAmount(objet.getAmount() - n);
                contenu[i] = objet.getAmount() <= 0 ? null : objet;
            }
        }
        if (mis == 0) {
            message(joueur, place <= 0 ? plein() : lang.c("message.rien",
                    "<red>Tu n'as pas de <objet> dans ton inventaire.", "objet", modele.effectiveName()), true);
            return;
        }
        inventaire.setStorageContents(contenu);
        stockage.ecrire(support, new Stock(modele, stock.total() + mis));
        deposes(joueur, modele, mis);
        ouvrir(joueur, support);
    }

    /** Clic sur un objet de son inventaire, menu ouvert : l'objet est déposé (au tick suivant, après le clic). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClicInventaire(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        Support support = ouverts.get(joueur.getUniqueId());
        if (support == null || Contenant.ouvert(joueur, support.marque()) == null
                || event.getClickedInventory() != event.getView().getBottomInventory()) {
            return;
        }
        event.setCancelled(true);
        int place = event.getSlot();
        ItemStack clique = event.getCurrentItem();
        if (clique == null || clique.getType().isAir()) {
            return;
        }
        ItemStack attendu = clique.clone();
        Bukkit.getScheduler().runTask(plugin, () -> deposerCase(joueur, support, place, attendu));
    }

    private void deposerCase(Player joueur, Support support, int place, ItemStack attendu) {
        if (!joueur.isOnline() || Contenant.ouvert(joueur, support.marque()) == null) {
            return;
        }
        if (!stockage.est(support)) {
            Contenant.fermer(joueur);
            return;
        }
        ItemStack objet = joueur.getInventory().getItem(place);
        if (objet == null || !objet.equals(attendu)) {
            return;
        }
        if (!Stockage.accepte(objet)) {
            message(joueur, refus(), true);
            return;
        }
        Stock stock = stockage.lire(support);
        ItemStack modele = stock.modele() == null ? objet.asOne() : stock.modele();
        if (!modele.isSimilar(objet)) {
            message(joueur, lang.c("message.autre-objet", "<red>Ce coffre renforcé ne stocke que : <objet>.",
                    "objet", modele.effectiveName()), true);
            return;
        }
        long n = Math.min(objet.getAmount(), Stock.capacite(modele) - stock.total());
        if (n <= 0) {
            message(joueur, plein(), true);
            return;
        }
        objet.setAmount((int) (objet.getAmount() - n));
        joueur.getInventory().setItem(place, objet.getAmount() <= 0 ? null : objet);
        stockage.ecrire(support, new Stock(modele, stock.total() + n));
        deposes(joueur, modele, n);
        ouvrir(joueur, support);
    }

    // ------------------------------------------------------------------ messages

    private void deposes(Player joueur, ItemStack modele, long n) {
        message(joueur, lang.c("message.depose", "<green><n> x <objet> déposé(s).", "n", KSCoffreRenforce.nombre(n),
                "objet", modele.effectiveName()), false);
    }

    private Component refus() {
        return lang.c("message.refuse", "<red>Les shulkers et les sacs qui contiennent quelque chose sont refusés.");
    }

    private Component plein() {
        return lang.c("message.plein", "<red>Le coffre renforcé est plein (<max> stacks).", "max",
                KSCoffreRenforce.nombre(Stockage.STACKS));
    }

    private void message(Player joueur, Component texte, boolean refus) {
        Contenant.message(joueur, texte, refus);
        lang.saveIfNeeded();
    }

    @EventHandler
    public void onFermer(InventoryCloseEvent event) {
        Support support = ouverts.get(event.getPlayer().getUniqueId());
        if (support != null && event.getPlayer() instanceof Player joueur
                && Contenant.ouvert(joueur, support.marque()) != null) {
            ouverts.remove(joueur.getUniqueId());
        }
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        ouverts.remove(event.getPlayer().getUniqueId());
    }
}
