package fr.kalium.games.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Petits utilitaires d'objets. */
public final class Items {

    private Items() {
    }

    public static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name));
        List<Component> lines = new ArrayList<>();
        for (Component line : lore) {
            lines.add(plain(line));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    public static Material material(String name, Material fallback) {
        Material material = name == null ? null : Material.matchMaterial(name);
        if (material == null || material.isAir() || !material.isItem()) {
            return fallback;
        }
        return material;
    }
}
