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

    /** Ancien objet "Mini-jeux" du hub (1.16.0 : fourni par KG_Menu) - reconnu pour les objets encore en inventaire. */
    public static final String GAMES = "games";
    public static final String GAME = "game";
    public static final String CHECKPOINT = "checkpoint";
    /** 1.24.0 : objet « Rejouer » donne quelques secondes au hub apres un match (voir ReplayService). */
    public static final String REPLAY = "replay";

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

    public ItemStack gameMenuItem() {
        return build(GAME, "items.game-menu", Material.NETHER_STAR, "item.game",
                "<gold><bold>Menu de la partie", new String[]{"<gray>Clic droit : équipes, file,", "<gray>quitter la partie."});
    }

    public ItemStack checkpointItem() {
        return build(CHECKPOINT, "items.checkpoint", Material.LIME_DYE, "item.checkpoint",
                "<green><bold>Dernier point de contrôle", new String[]{"<gray>Clic droit pour y retourner."});
    }

    /**
     * 1.24.0 : objet « Rejouer ». hostName = pseudo de celui qui a deja relance la partie privee (l'objet devient
     * « Rejoindre la partie de X »), ou null.
     */
    public ItemStack replayItem(String hostName) {
        Material material = Items.material(plugin.getConfig().getString("replay.material"), Material.TOTEM_OF_UNDYING);
        Component name = hostName == null
                ? plugin.t("item.replay.name", "<green><bold>Rejouer")
                : plugin.t("item.replay.join-name", "<green><bold>Rejoindre la partie de <player>", "player", hostName);
        List<Component> lore = new ArrayList<>();
        lore.add(hostName == null
                ? plugin.t("item.replay.lore0", "<gray>Clic droit : relancer une partie")
                : plugin.t("item.replay.join-lore0", "<gray>Clic droit : entrer dans la partie"));
        lore.add(hostName == null
                ? plugin.t("item.replay.lore1", "<gray>avec les mêmes réglages.")
                : plugin.t("item.replay.join-lore1", "<gray>qui vient d'être relancée."));
        ItemStack item = Items.named(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, REPLAY);
        item.setItemMeta(meta);
        return item;
    }

    /** Type d'objet du plugin (GAMES, GAME, CHECKPOINT, REPLAY) ou null. */
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
