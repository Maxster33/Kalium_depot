package fr.kalium.crafts;

import io.papermc.paper.potion.PotionMix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * KS_Crafts - crafts du serveur Event (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md, Maxster33, 25/09/2026).
 * Les crafts « 8 + 1 » sont en anneau autour de l'objet central, les autres sans forme (réponse de Maxster33).
 * Contient aussi le Bedrock Breaker (usage unique : un clic droit sur un bloc de bedrock le retire) et le
 * remplacement de la verrue du Nether par le bloc de verrue (briques rouges du Nether, potion étrange à l'alambic).
 */
public final class KSCrafts extends JavaPlugin implements Listener {

    private NamespacedKey breakerKey;
    private final List<NamespacedKey> added = new ArrayList<>();

    @Override
    public void onEnable() {
        breakerKey = new NamespacedKey(this, "bedrock_breaker");
        getServer().getPluginManager().registerEvents(this, this);

        ring("red_sand", Material.SAND, new RecipeChoice.MaterialChoice(Material.ORANGE_DYE), new ItemStack(Material.RED_SAND, 8));
        shapeless("sand_from_sandstone", new ItemStack(Material.SAND, 4), new RecipeChoice.MaterialChoice(
                Material.SANDSTONE, Material.CHISELED_SANDSTONE, Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE));
        shapeless("red_sand_from_red_sandstone", new ItemStack(Material.RED_SAND, 4), new RecipeChoice.MaterialChoice(
                Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE, Material.CUT_RED_SANDSTONE, Material.SMOOTH_RED_SANDSTONE));
        shapeless("raw_iron_block", new ItemStack(Material.RAW_IRON_BLOCK, 4), repeat(new RecipeChoice.MaterialChoice(Material.IRON_BLOCK), 4));
        shapeless("raw_gold_block", new ItemStack(Material.RAW_GOLD_BLOCK, 4), repeat(new RecipeChoice.MaterialChoice(Material.GOLD_BLOCK), 4));
        shapeless("raw_copper_block", new ItemStack(Material.RAW_COPPER_BLOCK, 4), repeat(new RecipeChoice.MaterialChoice(
                Material.COPPER_BLOCK, Material.EXPOSED_COPPER, Material.WEATHERED_COPPER, Material.OXIDIZED_COPPER,
                Material.WAXED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER, Material.WAXED_WEATHERED_COPPER,
                Material.WAXED_OXIDIZED_COPPER), 4));
        ring("bedrock_breaker", Material.TNT, new RecipeChoice.MaterialChoice(Material.WOODEN_HOE), bedrockBreaker());
        shapeless("calcite", new ItemStack(Material.CALCITE, 9), concat(
                repeat(new RecipeChoice.MaterialChoice(Material.DIORITE), 5),
                repeat(new RecipeChoice.MaterialChoice(Material.QUARTZ_BLOCK), 4)));
        ring("crying_obsidian", Material.OBSIDIAN, new RecipeChoice.MaterialChoice(Material.GHAST_TEAR), new ItemStack(Material.CRYING_OBSIDIAN, 8));
        shapeless("reinforced_deepslate", new ItemStack(Material.REINFORCED_DEEPSLATE, 1), concat(
                repeat(new RecipeChoice.MaterialChoice(Material.BONE_BLOCK), 2),
                repeat(new RecipeChoice.MaterialChoice(Material.DEEPSLATE), 2)));
        shapeless("pitcher_pod", new ItemStack(Material.PITCHER_POD, 4), repeat(new RecipeChoice.MaterialChoice(Material.PITCHER_PLANT), 2));
        shapeless("torchflower_seeds", new ItemStack(Material.TORCHFLOWER_SEEDS, 4), repeat(new RecipeChoice.MaterialChoice(Material.TORCHFLOWER), 2));
        coral("tube", Material.TUBE_CORAL, Material.TUBE_CORAL_BLOCK);
        coral("brain", Material.BRAIN_CORAL, Material.BRAIN_CORAL_BLOCK);
        coral("bubble", Material.BUBBLE_CORAL, Material.BUBBLE_CORAL_BLOCK);
        coral("fire", Material.FIRE_CORAL, Material.FIRE_CORAL_BLOCK);
        coral("horn", Material.HORN_CORAL, Material.HORN_CORAL_BLOCK);
        ItemStack light = parse("minecraft:light[minecraft:block_state={level:\"15\"}]");
        if (light != null) {
            light.setAmount(4);
            shapeless("light_15", light, concat(
                    repeat(new RecipeChoice.MaterialChoice(Material.GLASS), 4),
                    repeat(new RecipeChoice.MaterialChoice(Material.GLOWSTONE), 4),
                    List.of(new RecipeChoice.MaterialChoice(Material.BLAZE_ROD))));
        }
        ItemStack frame = parse("minecraft:item_frame[minecraft:entity_data={id:\"minecraft:item_frame\",Invisible:1b}]");
        if (frame != null) {
            ring("invisible_item_frame", Material.STICK, new RecipeChoice.MaterialChoice(Material.PHANTOM_MEMBRANE), frame);
        }
        ring("verdant_froglight", Material.MAGMA_BLOCK, new RecipeChoice.MaterialChoice(Material.GREEN_DYE), new ItemStack(Material.VERDANT_FROGLIGHT, 4));
        ring("ochre_froglight", Material.MAGMA_BLOCK, new RecipeChoice.MaterialChoice(Material.ORANGE_DYE), new ItemStack(Material.OCHRE_FROGLIGHT, 4));
        ring("pearlescent_froglight", Material.MAGMA_BLOCK, new RecipeChoice.MaterialChoice(Material.PURPLE_DYE), new ItemStack(Material.PEARLESCENT_FROGLIGHT, 4));

        // Verrue du Nether : remplacee par le bloc de verrue (briques rouges), craft 9 verrues -> bloc retire.
        Bukkit.removeRecipe(NamespacedKey.minecraft("red_nether_bricks"));
        Bukkit.removeRecipe(NamespacedKey.minecraft("nether_wart_block"));
        ShapedRecipe bricks = new ShapedRecipe(key("red_nether_bricks"), new ItemStack(Material.RED_NETHER_BRICKS));
        bricks.shape("WB", "BW");
        bricks.setIngredient('W', Material.NETHER_WART_BLOCK);
        bricks.setIngredient('B', Material.NETHER_BRICK);
        add(bricks);

        // Alambic : bouteille d'eau + bloc de verrue -> potion etrange.
        ItemStack awkward = new ItemStack(Material.POTION);
        PotionMeta awkwardMeta = (PotionMeta) awkward.getItemMeta();
        awkwardMeta.setBasePotionType(PotionType.AWKWARD);
        awkward.setItemMeta(awkwardMeta);
        Bukkit.getPotionBrewer().addPotionMix(new PotionMix(key("awkward_from_nether_wart_block"), awkward,
                PotionMix.createPredicateChoice(item -> item != null && item.getType() == Material.POTION
                        && item.getItemMeta() instanceof PotionMeta meta && meta.getBasePotionType() == PotionType.WATER),
                new RecipeChoice.MaterialChoice(Material.NETHER_WART_BLOCK)));

        getLogger().info(added.size() + " crafts ajoutés.");
    }

    @Override
    public void onDisable() {
        added.forEach(Bukkit::removeRecipe);
        Bukkit.getPotionBrewer().removePotionMix(key("awkward_from_nether_wart_block"));
    }

    // ------------------------------------------------------------------ recettes

    /** Objet décrit au format des commandes de Minecraft ; null (recette ignorée, avertissement) si refusé. */
    private ItemStack parse(String description) {
        try {
            return Bukkit.getItemFactory().createItemStack(description);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Objet refusé par le serveur, recette ignorée : " + description + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(this, id);
    }

    private void add(org.bukkit.inventory.Recipe recipe) {
        Bukkit.addRecipe(recipe);
        added.add(((org.bukkit.Keyed) recipe).getKey());
    }

    /** 8 objets en anneau autour de l'objet central. */
    private void ring(String id, Material around, RecipeChoice center, ItemStack result) {
        ShapedRecipe recipe = new ShapedRecipe(key(id), result);
        recipe.shape("AAA", "ACA", "AAA");
        recipe.setIngredient('A', around);
        recipe.setIngredient('C', center);
        add(recipe);
    }

    private void shapeless(String id, ItemStack result, RecipeChoice choice) {
        shapeless(id, result, List.of(choice));
    }

    private void shapeless(String id, ItemStack result, List<RecipeChoice> choices) {
        ShapelessRecipe recipe = new ShapelessRecipe(key(id), result);
        choices.forEach(recipe::addIngredient);
        add(recipe);
    }

    private void coral(String id, Material coral, Material block) {
        shapeless(id + "_coral_block", new ItemStack(block), repeat(new RecipeChoice.MaterialChoice(coral), 4));
    }

    private static List<RecipeChoice> repeat(RecipeChoice choice, int count) {
        List<RecipeChoice> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(choice);
        }
        return list;
    }

    @SafeVarargs
    private static List<RecipeChoice> concat(List<RecipeChoice>... parts) {
        List<RecipeChoice> list = new ArrayList<>();
        for (List<RecipeChoice> part : parts) {
            list.addAll(part);
        }
        return list;
    }

    // ------------------------------------------------------------------ Bedrock Breaker

    private ItemStack bedrockBreaker() {
        ItemStack item = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Bedrock Breaker").decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(breakerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isBreaker(ItemStack item) {
        return item != null && item.getType() == Material.WOODEN_HOE && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(breakerKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUseBreaker(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.BEDROCK
                || event.getHand() == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = event.getHand() == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        if (!isBreaker(hand)) {
            return;
        }
        event.setCancelled(true);
        block.setType(Material.AIR);
        hand.setAmount(hand.getAmount() - 1);
    }
}
