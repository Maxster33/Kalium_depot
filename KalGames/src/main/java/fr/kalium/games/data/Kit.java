package fr.kalium.games.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Kit de combat : armure, main secondaire, objets a placer automatiquement ou a un slot precis. */
public final class Kit {

    public static final String SOURCE_INVENTORY = "inventory";
    public static final String SOURCE_PK2_PREFIX = "playerkits2:";

    private final String id;
    private String display;
    private String source;
    /** boots, leggings, chestplate, helmet. */
    private final ItemStack[] armor = new ItemStack[4];
    private ItemStack offhand;
    private final List<ItemStack> auto = new ArrayList<>();
    private final Map<Integer, ItemStack> slots = new LinkedHashMap<>();
    private boolean broken;

    public Kit(String id, String display, String source) {
        this.id = id;
        this.display = display;
        this.source = source;
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

    public String source() {
        return source;
    }

    public void source(String value) {
        this.source = value;
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

    public boolean broken() {
        return broken;
    }

    public void broken(boolean value) {
        this.broken = value;
    }

    public void clearContents() {
        for (int i = 0; i < armor.length; i++) {
            armor[i] = null;
        }
        offhand = null;
        auto.clear();
        slots.clear();
    }

    public String pk2Name() {
        return source.startsWith(SOURCE_PK2_PREFIX) ? source.substring(SOURCE_PK2_PREFIX.length()) : null;
    }
}
