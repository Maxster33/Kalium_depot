package fr.kalium.decapitator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * /tetes (opérateurs, choix de LeKiwi06 pour valider les textures en jeu) : toutes les têtes, 45 par page ; clic sur
 * une tête = une tête dans l'inventaire (le reste tombe au sol si l'inventaire est plein). Rien ne peut être déposé
 * dans le menu.
 */
final class MenuTetes implements Listener {

    private static final int PAR_PAGE = 45;
    private static final int PRECEDENTE = 45;
    private static final int SUIVANTE = 53;

    /** Page affichée, gardée par l'inventaire du menu. */
    private record Page(int numero) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    static void ouvrir(Player joueur, int numero) {
        List<ItemStack> tetes = KSDecapitator.toutes();
        int pages = Math.max(1, (tetes.size() + PAR_PAGE - 1) / PAR_PAGE);
        int page = Math.max(0, Math.min(numero, pages - 1));
        Inventory menu = Bukkit.createInventory(new Page(page), 54,
                Component.text("Têtes (" + tetes.size() + ") - page " + (page + 1) + "/" + pages));
        for (int i = 0; i < PAR_PAGE && page * PAR_PAGE + i < tetes.size(); i++) {
            menu.setItem(i, tetes.get(page * PAR_PAGE + i));
        }
        if (page > 0) {
            menu.setItem(PRECEDENTE, bouton("Page précédente"));
        }
        if (page < pages - 1) {
            menu.setItem(SUIVANTE, bouton("Page suivante"));
        }
        joueur.openInventory(menu);
    }

    private static ItemStack bouton(String texte) {
        ItemStack fleche = new ItemStack(Material.ARROW);
        ItemMeta meta = fleche.getItemMeta();
        meta.displayName(Component.text(texte, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        fleche.setItemMeta(meta);
        return fleche;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Page page)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player joueur) || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int case_ = event.getRawSlot();
        if (case_ == PRECEDENTE && page.numero() > 0) {
            ouvrir(joueur, page.numero() - 1);
        } else if (case_ == SUIVANTE) {
            ouvrir(joueur, page.numero() + 1);
        } else if (case_ < PAR_PAGE && event.getCurrentItem() != null) {
            for (ItemStack reste : joueur.getInventory().addItem(event.getCurrentItem().clone()).values()) {
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Page) {
            event.setCancelled(true);
        }
    }
}
