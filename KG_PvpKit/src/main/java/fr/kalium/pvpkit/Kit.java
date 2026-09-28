package fr.kalium.pvpkit;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kit de combat : armure, main secondaire, objets a une case precise ou a placer automatiquement.
 * Repris de KalGames 1.21.0 ; plus de kit relie a PlayerKits2 (les anciens sont convertis, voir KitLibrary).
 */
public final class Kit {

    private final String id;
    private String display;
    /** boots, leggings, chestplate, helmet. */
    private final ItemStack[] armor = new ItemStack[4];
    private ItemStack offhand;
    /** Objets sans case precise (kits convertis depuis PlayerKits2) : ajoutes a la suite dans l'inventaire. */
    private final List<ItemStack> auto = new ArrayList<>();
    private final Map<Integer, ItemStack> slots = new LinkedHashMap<>();

    public Kit(String id, String display) {
        this.id = id;
        this.display = display;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public void display(String value) {
        this.display = value;
    }

    public ItemStack[] armor() {
        return armor;
    }

    public ItemStack offhand() {
        return offhand;
    }

    public void offhand(ItemStack value) {
        this.offhand = value;
    }

    public List<ItemStack> auto() {
        return auto;
    }

    public Map<Integer, ItemStack> slots() {
        return slots;
    }

    public boolean empty() {
        for (ItemStack item : armor) {
            if (item != null) {
                return false;
            }
        }
        return offhand == null && auto.isEmpty() && slots.isEmpty();
    }
}
