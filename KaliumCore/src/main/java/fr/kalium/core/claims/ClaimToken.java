package fr.kalium.core.claims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Objet representant un emplacement de claim converti (voir ClaimsService.convertSlotToItem /
 * convertItemToSlot). Empilable (chaque exemplaire vaut 1 emplacement), pense pour circuler via le
 * futur systeme de commerce.
 */
public final class ClaimToken {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ClaimToken() {
    }

    private static NamespacedKey key(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "claim_token");
    }

    public static ItemStack create(JavaPlugin plugin) {
        String materialName = plugin.getConfig().getString("modules.claims.token.material", "PAPER");
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir() || !material.isItem()) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(noItalic(MM.deserialize(
                plugin.getConfig().getString("modules.claims.token.name", "<gold><bold>Jeton d'emplacement"))));
        List<Component> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("modules.claims.token.lore")) {
            lore.add(noItalic(MM.deserialize(line)));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isToken(ItemStack item, JavaPlugin plugin) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }

    private static Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
