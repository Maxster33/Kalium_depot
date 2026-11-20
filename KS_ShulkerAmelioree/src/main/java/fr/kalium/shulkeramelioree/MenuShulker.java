package fr.kalium.shulkeramelioree;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Le menu d'une shulker améliorée : un coffre de 6 rangées (54 cases) qui montre son contenu, où l'on range et prend
 * comme dans un coffre double. Deux joueurs qui ouvrent la même shulker partagent le même menu (comme un coffre).
 *
 * Pendant qu'il est ouvert, le menu est le contenu : il est écrit dans le bloc au tick qui suit chaque clic, et à la
 * fermeture (déconnexion et mort comprises) ; les entonnoirs attendent qu'il soit fermé ; une shulker cassée ferme
 * d'abord le menu (voir KSShulkerAmelioree). Aucune boîte de shulker n'y entre, même vide.
 */
final class MenuShulker implements Listener {

    /** Menu ouvert sur une shulker améliorée posée. */
    static final class Vue implements InventoryHolder {

        private final Block bloc;
        private Inventory inventaire;

        private Vue(Block bloc) {
            this.bloc = bloc;
        }

        Block bloc() {
            return bloc;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    private final JavaPlugin plugin;
    private final Lang lang;
    private final Contenu contenu;
    private final Map<Block, Vue> vues = new HashMap<>();
    private final Set<Vue> aSauver = new LinkedHashSet<>();

    MenuShulker(JavaPlugin plugin, Lang lang, Contenu contenu) {
        this.plugin = plugin;
        this.lang = lang;
        this.contenu = contenu;
    }

    static Location dessus(Block bloc) {
        return bloc.getLocation().add(0.5, 1.1, 0.5);
    }

    boolean ouvert(Block bloc) {
        return vues.containsKey(bloc);
    }

    void ouvrir(Player joueur, Block bloc) {
        if (!joueur.isOnline() || !contenu.est(bloc)) {
            return;
        }
        Vue vue = vues.get(bloc);
        if (vue == null) {
            // Remise en ordre avant d'ouvrir : ce qui n'a pas de place (ne devrait pas arriver) tombe au-dessus.
            ItemStack[] cases = contenu.lire(bloc, dessus(bloc));
            contenu.ecrire(bloc, cases);
            vue = new Vue(bloc);
            Component titre = bloc.getState(false) instanceof ShulkerBox s && s.customName() != null ? s.customName()
                    : lang.c("menu.titre", "Shulker améliorée");
            vue.inventaire = Bukkit.createInventory(vue, Contenu.CASES, titre);
            vue.inventaire.setContents(cases);
            vues.put(bloc, vue);
            if (bloc.getState(false) instanceof ShulkerBox s) {
                s.open();
            }
            lang.saveIfNeeded();
        }
        joueur.openInventory(vue.inventaire);
    }

    /** Ferme le menu de cette shulker pour tout le monde ; son contenu est alors écrit dans le bloc. */
    void fermerTout(Block bloc) {
        Vue vue = vues.get(bloc);
        if (vue == null) {
            return;
        }
        for (HumanEntity joueur : new ArrayList<>(vue.inventaire.getViewers())) {
            joueur.closeInventory();
        }
        if (vues.get(bloc) == vue) {
            terminer(vue);
        }
    }

    void fermerTout() {
        for (Block bloc : new ArrayList<>(vues.keySet())) {
            fermerTout(bloc);
        }
    }

    /**
     * 1.0.1 : la shulker n'est plus là (retirée sans BlockBreakEvent : encapsuleur, commande, WorldEdit...) : son menu
     * n'est plus le contenu de rien. Il est fermé pour tout le monde sans être écrit nulle part, et plus aucun clic n'y
     * passe : ce qu'il montrait est parti avec le bloc (l'encapsuleur ne prend qu'une shulker vide), rien ne peut en
     * sortir une deuxième fois.
     */
    private boolean perime(Vue vue) {
        return !contenu.est(vue.bloc);
    }

    void fermerPerimes() {
        for (Vue vue : new ArrayList<>(vues.values())) {
            if (perime(vue)) {
                oublier(vue.bloc);
            }
        }
    }

    /**
     * Menu de cette place oublié et fermé pour tout le monde SANS être écrit dans le bloc : shulker retirée, ou nouvelle
     * shulker posée à la même place (son contenu vient de son objet, jamais d'un ancien menu).
     */
    void oublier(Block bloc) {
        Vue vue = vues.remove(bloc);
        if (vue == null) {
            return;
        }
        aSauver.remove(vue);
        for (HumanEntity joueur : new ArrayList<>(vue.inventaire.getViewers())) {
            joueur.closeInventory();
        }
    }

    /** Contenu écrit maintenant (API : lecture d'une shulker dont le menu est ouvert). */
    void sauverMaintenant(Block bloc) {
        Vue vue = vues.get(bloc);
        if (vue != null) {
            sauver(vue);
        }
    }

    private void terminer(Vue vue) {
        sauver(vue);
        vues.remove(vue.bloc);
        aSauver.remove(vue);
        if (vue.bloc.getState(false) instanceof ShulkerBox s) {
            s.close();
        }
    }

    /** Le menu devient le contenu du bloc ; une boîte de shulker entrée malgré tout tombe au-dessus. */
    private void sauver(Vue vue) {
        if (!contenu.est(vue.bloc)) {
            return;
        }
        ItemStack[] cases = vue.inventaire.getContents();
        for (int i = 0; i < cases.length; i++) {
            if (Contenu.refuse(cases[i])) {
                vue.bloc.getWorld().dropItemNaturally(dessus(vue.bloc), cases[i]);
                cases[i] = null;
                vue.inventaire.setItem(i, null);
            }
        }
        contenu.ecrire(vue.bloc, cases);
    }

    private void sauverBientot(Vue vue) {
        if (aSauver.add(vue) && aSauver.size() == 1) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Vue v : List.copyOf(aSauver)) {
                    if (vues.get(v.bloc) == v) {
                        sauver(v);
                    }
                }
                aSauver.clear();
            });
        }
    }

    // ------------------------------------------------------------------ clics

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClic(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Vue vue)) {
            return;
        }
        if (perime(vue)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> oublier(vue.bloc));
            return;
        }
        Inventory haut = event.getView().getTopInventory();
        boolean dansLeMenu = event.getClickedInventory() == haut;
        ItemStack entrant = null;
        if (dansLeMenu) {
            if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
                entrant = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                entrant = event.getWhoClicked().getInventory().getItemInOffHand();
            } else {
                entrant = event.getCursor();
            }
        } else if (event.isShiftClick()) {
            entrant = event.getCurrentItem();
        }
        if (Contenu.refuse(entrant)) {
            event.setCancelled(true);
            refus(event.getWhoClicked());
            return;
        }
        sauverBientot(vue);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlisser(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Vue vue)) {
            return;
        }
        if (perime(vue)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> oublier(vue.bloc));
            return;
        }
        int taille = event.getView().getTopInventory().getSize();
        boolean dansLeMenu = event.getRawSlots().stream().anyMatch(place -> place < taille);
        if (dansLeMenu && Contenu.refuse(event.getOldCursor())) {
            event.setCancelled(true);
            refus(event.getWhoClicked());
            return;
        }
        sauverBientot(vue);
    }

    private void refus(HumanEntity qui) {
        if (qui instanceof Player joueur) {
            Contenant.message(joueur, lang.c("message.refuse",
                    "<red>Aucune boîte de shulker dans une shulker améliorée, même vide."), true);
            lang.saveIfNeeded();
        }
    }

    /** Dernier joueur parti : contenu écrit, menu oublié, couvercle refermé. */
    @EventHandler
    public void onFermer(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Vue vue) || vues.get(vue.bloc) != vue) {
            return;
        }
        if (vue.inventaire.getViewers().stream().allMatch(j -> j.equals(event.getPlayer()))) {
            terminer(vue);
        } else {
            sauverBientot(vue);
        }
    }
}
