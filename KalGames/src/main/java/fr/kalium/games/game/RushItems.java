package fr.kalium.games.game;

import fr.kalium.games.model.RushLayout.Shop;
import fr.kalium.games.model.RushLayout.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Objets et echanges du Rush (1.11.0, etape 2 - cahier des charges de l'utilisateur, voir aussi
 * echanges_PNJ_Rush.txt) : monnaies (Bronze / Silver / Gold) et les 45 echanges des 7 PNJ.
 *
 * Les PNJ de toutes les equipes proposent les memes echanges ; seuls le verre teinte (Macon), les
 * pieces d'armure en cuir (Armurier) et le lit (Trader #2) prennent la couleur de l'equipe de
 * l'ACHETEUR (le menu d'echange est construit pour chaque joueur au moment ou il l'ouvre).
 * Echanges illimites (aucune limite d'utilisation).
 */
public final class RushItems {

    private RushItems() {
    }

    // ------------------------------------------------------------------ monnaies

    public static ItemStack bronze(int amount) {
        return currency(Material.BRICK, "Bronze", TextColor.color(0xCD7F32), amount);
    }

    public static ItemStack silver(int amount) {
        return currency(Material.IRON_INGOT, "Silver", NamedTextColor.GRAY, amount);
    }

    public static ItemStack gold(int amount) {
        return currency(Material.GOLD_INGOT, "Gold", NamedTextColor.GOLD, amount);
    }

    private static ItemStack currency(Material material, String name, TextColor color, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------------ couleurs d'equipe

    public static NamedTextColor textColor(Team team) {
        return switch (team) {
            case BLUE -> NamedTextColor.BLUE;
            case RED -> NamedTextColor.RED;
            case YELLOW -> NamedTextColor.YELLOW;
            case GREEN -> NamedTextColor.GREEN;
        };
    }

    public static DyeColor dye(Team team) {
        return switch (team) {
            case BLUE -> DyeColor.BLUE;
            case RED -> DyeColor.RED;
            case YELLOW -> DyeColor.YELLOW;
            case GREEN -> DyeColor.LIME;
        };
    }

    public static Material bed(Team team) {
        return switch (team) {
            case BLUE -> Material.BLUE_BED;
            case RED -> Material.RED_BED;
            case YELLOW -> Material.YELLOW_BED;
            case GREEN -> Material.LIME_BED;
        };
    }

    public static Material glass(Team team) {
        return switch (team) {
            case BLUE -> Material.BLUE_STAINED_GLASS;
            case RED -> Material.RED_STAINED_GLASS;
            case YELLOW -> Material.YELLOW_STAINED_GLASS;
            case GREEN -> Material.LIME_STAINED_GLASS;
        };
    }

    public static Component teamName(Team team) {
        return Component.text(team.display(), textColor(team));
    }

    // ------------------------------------------------------------------ fabrication

    private static ItemStack item(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    private static ItemStack enchanted(Material material, Object... enchants) {
        ItemStack item = new ItemStack(material);
        for (int i = 0; i + 1 < enchants.length; i += 2) {
            item.addUnsafeEnchantment((Enchantment) enchants[i], (Integer) enchants[i + 1]);
        }
        return item;
    }

    private static ItemStack book(Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack leather(Material material, Team team) {
        ItemStack item = enchanted(material, Enchantment.PROTECTION, 2);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        Color color = dye(team).getColor();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack carroteur() {
        ItemStack item = enchanted(Material.CARROT_ON_A_STICK, Enchantment.KNOCKBACK, 2);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Carroteur3000", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static MerchantRecipe trade(ItemStack result, ItemStack... costs) {
        MerchantRecipe recipe = new MerchantRecipe(result, 0, Integer.MAX_VALUE, false);
        recipe.setIgnoreDiscounts(true);
        for (ItemStack cost : costs) {
            recipe.addIngredient(cost);
        }
        return recipe;
    }

    private static ItemStack emerald(int amount) {
        return item(Material.EMERALD, amount);
    }

    /** Echanges d'un PNJ, construits pour un acheteur de l'equipe donnee. */
    public static List<MerchantRecipe> recipes(Shop shop, Team buyer) {
        List<MerchantRecipe> list = new ArrayList<>();
        switch (shop) {
            case TRADER -> {
                list.add(trade(silver(1), bronze(10)));
                list.add(trade(gold(1), silver(3)));
                list.add(trade(emerald(1), gold(10)));
            }
            case TRADER2 -> {
                list.add(trade(item(Material.COOKED_BEEF, 2), bronze(4)));
                list.add(trade(item(Material.ANVIL, 1), silver(31), emerald(1)));
                list.add(trade(item(Material.GOLDEN_APPLE, 1), silver(3)));
                list.add(trade(item(bed(buyer), 1), emerald(30)));
            }
            case RUSHER -> {
                list.add(trade(enchanted(Material.GOLDEN_SWORD, Enchantment.SHARPNESS, 1), silver(5)));
                list.add(trade(enchanted(Material.GOLDEN_SWORD, Enchantment.SHARPNESS, 2), silver(10)));
                list.add(trade(enchanted(Material.GOLDEN_SWORD, Enchantment.SHARPNESS, 2, Enchantment.KNOCKBACK, 1), silver(15)));
                list.add(trade(enchanted(Material.GOLDEN_SWORD, Enchantment.SHARPNESS, 4), emerald(1)));
                list.add(trade(enchanted(Material.GOLDEN_SWORD, Enchantment.SHARPNESS, 4, Enchantment.KNOCKBACK, 1), emerald(3)));
                list.add(trade(item(Material.TNT, 1), silver(10)));
                list.add(trade(item(Material.FLINT_AND_STEEL, 1), gold(2)));
                list.add(trade(item(Material.CHEST, 1), bronze(32)));
            }
            case ARCHER -> {
                list.add(trade(enchanted(Material.BOW, Enchantment.POWER, 1), gold(3)));
                list.add(trade(enchanted(Material.BOW, Enchantment.POWER, 1, Enchantment.INFINITY, 1), gold(7)));
                list.add(trade(enchanted(Material.BOW, Enchantment.POWER, 2, Enchantment.INFINITY, 1), gold(13)));
                list.add(trade(item(Material.ARROW, 1), emerald(1)));
                list.add(trade(book(Enchantment.FLAME, 1), emerald(3)));
                list.add(trade(book(Enchantment.INFINITY, 1), emerald(3)));
            }
            case MACON -> {
                list.add(trade(item(Material.RED_SAND, 4), bronze(1)));
                list.add(trade(item(Material.GLOWSTONE, 2), bronze(4)));
                list.add(trade(item(Material.END_STONE, 1), bronze(4)));
                list.add(trade(enchanted(Material.WOODEN_PICKAXE, Enchantment.EFFICIENCY, 1), bronze(4)));
                list.add(trade(enchanted(Material.STONE_PICKAXE, Enchantment.EFFICIENCY, 2), silver(2)));
                list.add(trade(enchanted(Material.IRON_PICKAXE, Enchantment.EFFICIENCY, 3), gold(1)));
                list.add(trade(item(glass(buyer), 2), bronze(4)));
            }
            case ARMURIER -> {
                list.add(trade(leather(Material.LEATHER_BOOTS, buyer), bronze(8)));
                list.add(trade(leather(Material.LEATHER_LEGGINGS, buyer), bronze(8)));
                list.add(trade(leather(Material.LEATHER_HELMET, buyer), bronze(8)));
                list.add(trade(enchanted(Material.CHAINMAIL_CHESTPLATE, Enchantment.PROTECTION, 1), silver(3)));
                list.add(trade(enchanted(Material.CHAINMAIL_CHESTPLATE, Enchantment.PROTECTION, 2), silver(7)));
                list.add(trade(enchanted(Material.CHAINMAIL_CHESTPLATE, Enchantment.PROTECTION, 3), silver(12)));
            }
            case SECRET -> {
                list.add(trade(item(Material.EXPERIENCE_BOTTLE, 1), silver(1)));
                list.add(trade(carroteur(), emerald(3)));
                list.add(trade(item(Material.SNOWBALL, 16), silver(8)));
                list.add(trade(item(Material.ENDER_CHEST, 1), emerald(2)));
                list.add(trade(item(Material.COBWEB, 1), gold(3)));
            }
        }
        return list;
    }
}
