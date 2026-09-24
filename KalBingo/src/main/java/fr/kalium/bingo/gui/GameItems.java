package fr.kalium.bingo.gui;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Objet remis a chaque joueur en arrivant sur la carte de sa partie (une fois la salle d'attente
 * quittee - voir PartyStarter, qui retire la Nether Star de LobbyItems au meme moment) : un
 * papier dont le clic droit ouvre GameMenu (liste des objectifs, temps restant, progression de
 * chaque equipe) - demande explicite de l'utilisateur.
 *
 * A la difference de LobbyItems (Nether Star totalement verrouillee, y compris deplacement dans
 * l'inventaire) : demande explicite de l'utilisateur, "qui ne soit pas bloque de maniere a
 * pouvoir liberer la case si on le souhaite mais qu'on ne puisse pas le jeter" - donc SEUL le
 * drop (PlayerDropItemEvent, voir GameItemListener) est empeche, l'objet peut etre deplace/
 * range librement dans l'inventaire comme n'importe quel autre item.
 */
public final class GameItems {

    private static final int SLOT = 8;

    private final JavaPlugin plugin;
    private final NamespacedKey key;

    public GameItems(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "game-item");
    }

    private ItemStack menuItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lObjectifs");
        meta.setLore(java.util.List.of("§7Clic droit : liste des objectifs,", "§7temps restant, progression des équipes."));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "game-menu");
        item.setItemMeta(meta);
        return item;
    }

    /** Donne l'objet s'il n'est pas deja present quelque part dans l'inventaire (evite les doublons). */
    public void give(Player player) {
        var inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            if (isOurs(stack)) {
                return;
            }
        }
        if (inventory.getItem(SLOT) == null) {
            inventory.setItem(SLOT, menuItem());
        } else {
            inventory.addItem(menuItem());
        }
    }

    /**
     * Retire l'objet "Objectifs" de l'inventaire s'il est present (n'importe quelle case, meme
     * raison que LobbyItems.remove : le joueur a pu le deplacer). A appeler en fin de partie
     * (voir GameEndService) - le menu n'a plus de sens une fois la partie terminee.
     */
    public void remove(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isOurs(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
    }

    public boolean isOurs(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return "game-menu".equals(item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }
}
