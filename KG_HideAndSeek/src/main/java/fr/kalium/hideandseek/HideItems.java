package fr.kalium.hideandseek;

import fr.kalium.games.KalGames;
import fr.kalium.games.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Objets de la barre du hider : soundboard, changement de bloc, evasion. Chaque objet porte son role
 * (clic droit : voir KGHideAndSeek.onInteract).
 */
public final class HideItems {

    public static final String SOUNDS = "sounds";
    public static final String BLOCK = "block";
    public static final String ESCAPE = "escape";

    private final KalGames games;
    private final NamespacedKey key;

    public HideItems(KGHideAndSeek plugin, KalGames games) {
        this.games = games;
        this.key = new NamespacedKey(plugin, "item");
    }

    private ItemStack tagged(Material material, Component name, List<Component> lore, String kind) {
        ItemStack item = Items.named(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, kind);
        item.setItemMeta(meta);
        return item;
    }

    /** Role de l'objet (SOUNDS, BLOCK, ESCAPE) ou null. */
    public String kind(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public ItemStack soundsMenu() {
        return tagged(Material.NOTE_BLOCK, games.t("hns.item-soundboard", "<gold><bold>Soundboard"),
                List.of(games.t("hns.item-soundboard-lore", "<gray>Clic droit : choisir un son à jouer.")), SOUNDS);
    }

    public ItemStack blockChanger(Material current) {
        Material icon = current != null && current.isItem() ? current : Material.GRASS_BLOCK;
        return tagged(icon, games.t("hns.item-block", "<aqua><bold>Changer de bloc"),
                List.of(games.t("hns.item-block-lore", "<gray>Clic droit : choisir le bloc que vous imitez.")), BLOCK);
    }

    public ItemStack escape(int points, int cooldown) {
        return tagged(Material.RABBIT_FOOT, games.t("hns.item-escape", "<light_purple><bold>Évasion"),
                List.of(games.t("hns.item-escape-lore1", "<gray>Clic droit : Vitesse V pendant 2 secondes."),
                        games.t("hns.item-escape-lore2", "<gray>Dès <white><points></white> points, puis toutes les <white><s></white> s.",
                                "points", points, "s", cooldown)), ESCAPE);
    }
}
