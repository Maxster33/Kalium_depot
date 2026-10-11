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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * /tetes : toutes les têtes, 45 par page. Rien ne peut être déposé dans le menu.
 *
 * 1.2.0 (LeKiwi06, 11/10/2026) : ouvert à tous les joueurs, pour savoir quelles têtes ils ont déjà obtenues (voir
 * Collection). Une tête déjà eue est montrée telle quelle, avec « Obtenue » ; une tête jamais eue par une vitre grise
 * à son nom, avec « Pas encore obtenue ». Le titre donne le compte du joueur.
 * Opérateurs (ks.decapitator.tetes, choix de LeKiwi06 du 30/09/2026 pour valider les textures en jeu) : toutes les
 * têtes sont montrées ; clic sur une tête = une tête dans l'inventaire (le reste tombe au sol s'il est plein).
 */
final class MenuTetes implements Listener {

    private static final String PERMISSION = "ks.decapitator.tetes";
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
        List<String> ids = KSDecapitator.ids();
        KSDecapitator.collection().regarder(joueur);
        Set<String> obtenues = KSDecapitator.collection().de(joueur.getUniqueId());
        boolean toutes = joueur.hasPermission(PERMISSION);
        long compte = ids.stream().filter(obtenues::contains).count();
        int pages = Math.max(1, (ids.size() + PAR_PAGE - 1) / PAR_PAGE);
        int page = Math.max(0, Math.min(numero, pages - 1));
        Inventory menu = Bukkit.createInventory(new Page(page), 54,
                Component.text("Têtes " + compte + "/" + ids.size() + " - page " + (page + 1) + "/" + pages));
        for (int i = 0; i < PAR_PAGE && page * PAR_PAGE + i < ids.size(); i++) {
            String id = ids.get(page * PAR_PAGE + i);
            boolean obtenue = obtenues.contains(id);
            menu.setItem(i, obtenue || toutes ? tete(id, obtenue) : manquante(id));
        }
        if (page > 0) {
            menu.setItem(PRECEDENTE, bouton("Page précédente"));
        }
        if (page < pages - 1) {
            menu.setItem(SUIVANTE, bouton("Page suivante"));
        }
        joueur.openInventory(menu);
    }

    private static Component etat(boolean obtenue) {
        return Component.text(obtenue ? "Obtenue" : "Pas encore obtenue", obtenue ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);
    }

    /** La tête, avec son état en dernière ligne de description. */
    private static ItemStack tete(String id, boolean obtenue) {
        ItemStack tete = KSDecapitator.creerTete(id);
        ItemMeta meta = tete.getItemMeta();
        List<Component> description = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        description.add(etat(obtenue));
        meta.lore(description);
        tete.setItemMeta(meta);
        return tete;
    }

    /** Tête jamais eue : une vitre grise à son nom. */
    private static ItemStack manquante(String id) {
        ItemStack vitre = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = vitre.getItemMeta();
        meta.displayName(Component.text(KSDecapitator.nom(id), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(etat(false)));
        vitre.setItemMeta(meta);
        return vitre;
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
        List<String> ids = KSDecapitator.ids();
        int numero = page.numero() * PAR_PAGE + case_;
        if (case_ == PRECEDENTE && page.numero() > 0) {
            ouvrir(joueur, page.numero() - 1);
        } else if (case_ == SUIVANTE) {
            ouvrir(joueur, page.numero() + 1);
        } else if (case_ < PAR_PAGE && numero < ids.size() && joueur.hasPermission(PERMISSION)) {
            for (ItemStack reste : joueur.getInventory().addItem(KSDecapitator.creerTete(ids.get(numero))).values()) {
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
