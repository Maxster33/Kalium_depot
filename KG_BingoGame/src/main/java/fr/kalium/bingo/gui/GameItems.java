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

    /**
     * 0.4.0 : carte de la grille (voir fr.kalium.bingo.map), fournie par BingoPlugin : objet carte de la partie du
     * joueur (null s'il n'est pas en partie) et numero de cette carte.
     */
    private java.util.function.Function<Player, ItemStack> mapItem = p -> null;
    private java.util.function.ToIntFunction<Player> mapId = p -> -1;

    public void setMapSupplier(java.util.function.Function<Player, ItemStack> mapItem, java.util.function.ToIntFunction<Player> mapId) {
        this.mapItem = mapItem;
        this.mapId = mapId;
    }

    /**
     * Donne le papier "Objectifs" s'il n'est pas deja dans l'inventaire (evite les doublons) et, depuis la 0.4.0, la
     * carte de la grille : en main secondaire si elle est libre, sinon dans l'inventaire. Une carte d'une autre
     * partie (ou d'avant un redemarrage du serveur) est remplacee.
     */
    public void give(Player player) {
        var inventory = player.getInventory();
        boolean hasMenu = false;
        boolean hasMap = false;
        int currentMap = mapId.applyAsInt(player);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            String kind = kindOf(stack);
            if ("game-menu".equals(kind)) {
                hasMenu = true;
            } else if ("game-map".equals(kind)) {
                if (!hasMap && stack.getItemMeta() instanceof org.bukkit.inventory.meta.MapMeta meta
                        && meta.hasMapView() && meta.getMapView() != null && meta.getMapView().getId() == currentMap) {
                    hasMap = true;
                } else {
                    inventory.setItem(slot, null);
                }
            }
        }
        if (!hasMenu) {
            if (inventory.getItem(SLOT) == null) {
                inventory.setItem(SLOT, menuItem());
            } else {
                inventory.addItem(menuItem());
            }
        }
        if (!hasMap) {
            ItemStack map = mapItem.apply(player);
            if (map != null) {
                ItemMeta meta = map.getItemMeta();
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "game-map");
                map.setItemMeta(meta);
                if (inventory.getItemInOffHand().getType().isAir()) {
                    inventory.setItemInOffHand(map);
                } else {
                    inventory.addItem(map);
                }
            }
        }
    }

    private String kindOf(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
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

    /** Repond a "ce joueur est-il dans une partie en cours ?" pour GameItemListener (0.3.0, branche par BingoPlugin). */
    private java.util.function.Predicate<Player> inGame = p -> false;

    public void setInGameCheck(java.util.function.Predicate<Player> inGame) {
        this.inGame = inGame;
    }

    public boolean isInGame(Player player) {
        return inGame.test(player);
    }

    public boolean isOurs(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        String kind = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return "game-menu".equals(kind) || "game-map".equals(kind); // 0.4.0 : la carte est verrouillee comme le papier
    }
}
