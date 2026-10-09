package fr.kalium.rewardsgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 1.4.0 (LeKiwi06, 09/10/2026 : « j'aimerais rendre les interfaces plus jolies, et selon moi ça passe par davantage
 * d'interfaces de type contenant quand c'est possible au lieu des boutons ») : menu de type contenant.
 *
 * Un coffre de 1 à 6 rangées ; chaque case porte un objet (son nom et ses lignes de description disent ce que fait le
 * clic) et, au besoin, une action. Rien ne se prend ni ne se dépose : tous les clics sont annulés, celui sur une case à
 * action la déclenche. Le même menu peut être vidé et rempli à nouveau sans se refermer (le curseur ne bouge pas).
 *
 * Écrit pour être repris tel quel par les autres menus (à déplacer dans la boîte à outils de KLM_Menu quand un deuxième
 * plugin en aura besoin).
 */
final class Contenant implements InventoryHolder {

    private final Inventory inventaire;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
    /** Ce que le menu affiche (pour le reconnaître et le remplir à nouveau au lieu d'en ouvrir un autre). */
    final Object marque;

    Contenant(int rangees, Component titre, Object marque) {
        this.inventaire = Bukkit.createInventory(this, Math.max(1, Math.min(6, rangees)) * 9, titre);
        this.marque = marque;
    }

    @Override
    public Inventory getInventory() {
        return inventaire;
    }

    /** Le menu de ce type déjà ouvert par le joueur, ou null. */
    static Contenant ouvert(Player joueur, Object marque) {
        return joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant c && marque.equals(c.marque)
                ? c : null;
    }

    void vider() {
        inventaire.clear();
        actions.clear();
    }

    /** Pose un objet dans une case, avec l'action de son clic (null : case de décor). */
    void poser(int place, ItemStack objet, Consumer<Player> action) {
        inventaire.setItem(place, objet);
        if (action == null) {
            actions.remove(place);
        } else {
            actions.put(place, action);
        }
    }

    void ouvrir(Player joueur) {
        if (joueur.getOpenInventory().getTopInventory() != inventaire) {
            joueur.openInventory(inventaire);
        }
    }

    /** Un objet de menu : nom et lignes sans italique, sans les attributs du jeu (dégâts, etc.). */
    static ItemStack objet(Material materiau, Component nom, List<Component> lignes) {
        ItemStack objet = new ItemStack(materiau);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lignes.forEach(l -> lore.add(l.decoration(TextDecoration.ITALIC, false)));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        objet.setItemMeta(meta);
        return objet;
    }

    /** Case de décor : vitre sans nom. */
    static ItemStack decor() {
        return objet(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
    }

    /** Écoute commune à tous les menus de ce type (à enregistrer une fois). */
    static final class Ecoute implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getView().getTopInventory().getHolder() instanceof Contenant menu)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != menu.inventaire || !(event.getWhoClicked() instanceof Player joueur)) {
                return;
            }
            Consumer<Player> action = menu.actions.get(event.getSlot());
            if (action != null) {
                joueur.playSound(joueur.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                action.accept(joueur);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getView().getTopInventory().getHolder() instanceof Contenant) {
                event.setCancelled(true);
            }
        }
    }
}
