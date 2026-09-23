package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Objets verrouilles du plugin : menu des mini-jeux (hub), menu de la partie, dernier checkpoint. */
public final class ItemService {

    public static final String GAMES = "games";
    public static final String GAME = "game";
    public static final String CHECKPOINT = "checkpoint";

    private final KalGames plugin;
    private final NamespacedKey key;

    public ItemService(KalGames plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "item");
    }

    private ItemStack build(String kind, String configPath, Material fallback, String nameKey, String nameDef, String[] loreDefs) {
        Material material = Items.material(plugin.getConfig().getString(configPath + ".material"), fallback);
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < loreDefs.length; i++) {
            lore.add(plugin.t(nameKey + ".lore" + i, loreDefs[i]));
        }
        ItemStack item = Items.named(material, plugin.t(nameKey + ".name", nameDef), lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, kind);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack gamesItem() {
        return build(GAMES, "items.games", Material.NETHER_STAR, "item.games",
                "<gold><bold>Mini-jeux", new String[]{"<gray>Clic droit pour choisir", "<gray>un mini-jeu."});
    }

    public ItemStack gameMenuItem() {
        return build(GAME, "items.game-menu", Material.NETHER_STAR, "item.game",
                "<gold><bold>Menu de la partie", new String[]{"<gray>Clic droit : équipes, file,", "<gray>quitter la partie."});
    }

    public ItemStack checkpointItem() {
        return build(CHECKPOINT, "items.checkpoint", Material.LIME_DYE, "item.checkpoint",
                "<green><bold>Dernier point de contrôle", new String[]{"<gray>Clic droit pour y retourner."});
    }

    /** Type d'objet du plugin (GAMES, GAME, CHECKPOINT) ou null. */
    public String kind(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public boolean isOurs(ItemStack item) {
        return kind(item) != null;
    }

    public int slot(String path, int def) {
        int slot = plugin.getConfig().getInt(path, def);
        return slot < 0 || slot > 8 ? def : slot;
    }
}
