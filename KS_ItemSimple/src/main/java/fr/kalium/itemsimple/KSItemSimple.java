package fr.kalium.itemsimple;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KS_ItemSimple (demande de Maxster33, 29/09/2026) : objets du serveur Event qui n'ont pas d'autre fonction que le craft.
 *
 * - Fragment de Spawner : image d'un fragment de disque ; lâché par les spawners naturels (KS_LootBlocs).
 * - Cœur de Spawner : image de l'ancre de réapparition, ne peut pas être posé.
 * - 1.1.0 (LeKiwi06, 30/09/2026) : les 5 têtes « Steve » (araignée, blaze, mouton, vache, poule) sont retirées, les
 *   têtes de mobs sont maintenant celles de KS_Decapitator.
 *
 * Fragment et cœur sont des livres de connaissances (ni bloc, ni ingrédient vanilla) dont seule l'image change
 * (item_model) et dont le clic droit vanilla est annulé. Tous s'empilent par 64 et portent un marqueur (id de l'objet).
 * Autres plugins : creerFragmentSpawner(), creerCoeurSpawner(), idObjet(objet) (KS_Crafts, KS_LootBlocs,
 * KS_KaliumGive).
 */
public final class KSItemSimple extends JavaPlugin implements Listener {

    private static NamespacedKey marqueur;

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "objet");
        getServer().getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------ objets

    /** Un Fragment de Spawner. Nécessite que le plugin soit activé. */
    public static ItemStack creerFragmentSpawner() {
        return objet(new ItemStack(Material.KNOWLEDGE_BOOK), "fragment_spawner", "Fragment de Spawner",
                "disc_fragment_5");
    }

    /** Un Cœur de Spawner. Nécessite que le plugin soit activé. */
    public static ItemStack creerCoeurSpawner() {
        return objet(new ItemStack(Material.KNOWLEDGE_BOOK), "coeur_spawner", "Cœur de Spawner", "respawn_anchor");
    }

    private static ItemStack objet(ItemStack item, String id, String nom, String modele) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(nom, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        if (modele != null) {
            meta.setItemModel(NamespacedKey.minecraft(modele));
        }
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    /** Id de l'objet (fragment_spawner, coeur_spawner), ou null si ce n'est pas un objet du plugin. */
    public static String idObjet(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(marqueur, PersistentDataType.STRING);
    }

    // ------------------------------------------------------------------ aucune autre fonction

    /** Livre de connaissances : clic droit vanilla annulé (il serait consommé). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && event.getItem() != null && event.getItem().getType() == Material.KNOWLEDGE_BOOK
                && idObjet(event.getItem()) != null) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }
}
