package fr.kalium.observateur;

import fr.kalium.menu.api.Lang;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** La longue-vue « Observer un joueur » (reperee par une marque invisible, pas par son nom). */
final class ObserverItem {

    private final NamespacedKey key;
    private final Lang lang;

    ObserverItem(JavaPlugin plugin, Lang lang) {
        this.key = new NamespacedKey(plugin, "observateur");
        this.lang = lang;
    }

    ItemStack create() {
        ItemStack item = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.c("objet.nom", "<gold><!italic>Observer un joueur"));
        meta.lore(List.of(lang.c("objet.aide", "<gray><!italic>Clic droit : choisir un joueur à observer")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean isOurs(ItemStack item) {
        return item != null && item.getType() == Material.SPYGLASS && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    boolean has(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isOurs(item)) {
                return true;
            }
        }
        return false;
    }

    /** Clic droit avec la longue-vue : la liste des joueurs (au lieu du zoom). */
    Listener listener(ObservationMenu menu) {
        return new Listener() {
            @EventHandler
            public void onUse(PlayerInteractEvent event) {
                if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                    return;
                }
                if (!isOurs(event.getItem())) {
                    return;
                }
                event.setCancelled(true);
                Player player = event.getPlayer();
                if (!player.hasPermission("kgbingoobservateur.use")) {
                    return;
                }
                menu.openList(player);
            }
        };
    }
}
