package fr.kalium.lootblocs;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_LootBlocs (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md, Maxster33, 25/09/2026) :
 * - minerais de fer et d'or : autant de minerai brut que le cuivre (2 à 5, Fortune comme en vanilla) ;
 * - autres minerais : 1 % de chance que le drop soit remplacé par 2 blocs du minerai cassé (jamais avec Toucher de soie) ;
 * - feuilles (cassées ou dégradées) : pousses ×2 sur chêne noir, chêne pâle et acacia ; pommes sur toutes les feuilles
 *   (taux vanilla), chaque pomme : 1 % pomme dorée, 0,01 % pomme dorée enchantée ;
 * - verrue du Nether : plus aucun drop de verrue.
 * Les potions (émeraude deepslate, chorus, verrue) sont dans KS_LootPotions.
 */
public final class KSLootBlocs extends JavaPlugin implements Listener {

    private static final Map<Material, Material> IRON_GOLD = Map.of(
            Material.IRON_ORE, Material.RAW_IRON, Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON,
            Material.GOLD_ORE, Material.RAW_GOLD, Material.DEEPSLATE_GOLD_ORE, Material.RAW_GOLD);

    private static final Set<Material> DOUBLE_ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE);

    private static final Map<Material, Material> DOUBLE_SAPLINGS = Map.of(
            Material.DARK_OAK_LEAVES, Material.DARK_OAK_SAPLING,
            Material.PALE_OAK_LEAVES, Material.PALE_OAK_SAPLING,
            Material.ACACIA_LEAVES, Material.ACACIA_SAPLING);

    /** Chances vanilla selon le niveau de Fortune (0 a 3). */
    private static final double[] SAPLING_CHANCE = {1 / 20.0, 1 / 16.0, 1 / 12.0, 1 / 10.0};
    private static final double[] APPLE_CHANCE = {1 / 200.0, 1 / 180.0, 1 / 160.0, 1 / 120.0};

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    // ------------------------------------------------------------------ blocs casses par un joueur

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        Material type = event.getBlockState().getType();
        List<Item> items = event.getItems();
        if (items.isEmpty()) {
            return;
        }
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        boolean silk = tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
        int fortune = Math.min(3, tool.getEnchantmentLevel(Enchantment.FORTUNE));

        if (type == Material.NETHER_WART) {
            items.removeIf(item -> item.getItemStack().getType() == Material.NETHER_WART);
        } else if (IRON_GOLD.containsKey(type) && !silk) {
            Material raw = IRON_GOLD.get(type);
            for (Item item : items) {
                if (item.getItemStack().getType() == raw) {
                    item.setItemStack(new ItemStack(raw, oreCount(fortune)));
                }
            }
        } else if (DOUBLE_ORES.contains(type) && !silk && random().nextDouble() < 0.01) {
            items.get(0).setItemStack(new ItemStack(type, 2));
            items.subList(1, items.size()).clear();
        } else if (Tag.LEAVES.isTagged(type)) {
            List<ItemStack> extra = new ArrayList<>();
            leafExtras(type, fortune, containsLeafBlock(items, type), extra);
            for (Item item : items) {
                List<ItemStack> rolled = appleRoll(item.getItemStack());
                item.setItemStack(rolled.get(0));
                extra.addAll(rolled.subList(1, rolled.size()));
            }
            drop(event.getBlock().getLocation(), extra);
        }
    }

    /** Minerai brut « comme le cuivre » : 2 a 5, puis bonus de Fortune vanilla des minerais. */
    private static int oreCount(int fortune) {
        int count = 2 + random().nextInt(4);
        if (fortune > 0) {
            count *= Math.max(0, random().nextInt(fortune + 2) - 1) + 1;
        }
        return count;
    }

    // ------------------------------------------------------------------ feuilles

    private static boolean containsLeafBlock(Collection<Item> items, Material leaves) {
        return items.stream().anyMatch(item -> item.getItemStack().getType() == leaves);
    }

    /** Pousse supplementaire (x2) et pomme sur les feuilles qui n'en donnent pas en vanilla. */
    private static void leafExtras(Material leaves, int fortune, boolean leafBlockDropped, List<ItemStack> extra) {
        if (leafBlockDropped) {
            return; // cisailles / Toucher de soie : pas de pousse ni de pomme, comme en vanilla
        }
        Material sapling = DOUBLE_SAPLINGS.get(leaves);
        if (sapling != null && random().nextDouble() < SAPLING_CHANCE[fortune]) {
            extra.add(new ItemStack(sapling));
        }
        if (leaves != Material.OAK_LEAVES && leaves != Material.DARK_OAK_LEAVES
                && random().nextDouble() < APPLE_CHANCE[fortune]) {
            extra.addAll(appleRoll(new ItemStack(Material.APPLE)));
        }
    }

    /** Chaque pomme : 0,01 % pomme doree enchantee, 1 % pomme doree, sinon pomme. Renvoie au moins une pile. */
    private static List<ItemStack> appleRoll(ItemStack stack) {
        if (stack.getType() != Material.APPLE) {
            return List.of(stack);
        }
        int apples = 0;
        int golden = 0;
        int enchanted = 0;
        for (int i = 0; i < stack.getAmount(); i++) {
            double roll = random().nextDouble();
            if (roll < 0.0001) {
                enchanted++;
            } else if (roll < 0.0101) {
                golden++;
            } else {
                apples++;
            }
        }
        List<ItemStack> result = new ArrayList<>();
        if (enchanted > 0) {
            result.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, enchanted));
        }
        if (golden > 0) {
            result.add(new ItemStack(Material.GOLDEN_APPLE, golden));
        }
        if (apples > 0) {
            result.add(new ItemStack(Material.APPLE, apples));
        }
        return result;
    }

    private static void drop(Location location, List<ItemStack> stacks) {
        Location center = location.clone().add(0.5, 0.5, 0.5);
        for (ItemStack stack : stacks) {
            if (stack != null && stack.getAmount() > 0) {
                center.getWorld().dropItemNaturally(center, stack);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack stack : block.getDrops()) {
            drops.addAll(appleRoll(stack));
        }
        leafExtras(type, 0, false, drops);
        event.setCancelled(true);
        block.setType(Material.AIR, true);
        drop(block.getLocation(), drops);
    }

    // ------------------------------------------------------------------ verrue cassee autrement que par un joueur

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakByBlock(BlockBreakBlockEvent event) {
        if (event.getBlock().getType() == Material.NETHER_WART) {
            event.getDrops().removeIf(stack -> stack.getType() == Material.NETHER_WART);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        clearWart(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        clearWart(event.blockList());
    }

    private static void clearWart(List<Block> blocks) {
        blocks.removeIf(block -> {
            if (block.getType() == Material.NETHER_WART) {
                block.setType(Material.AIR, false);
                return true;
            }
            return false;
        });
    }
}
