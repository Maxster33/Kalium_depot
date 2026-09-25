package fr.kalium.lootpotions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_LootPotions (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md, Maxster33, 25/09/2026) : toutes les potions à
 * ramasser, sur les blocs cassés par un joueur et les mobs tués par un joueur (Butin sans effet). Potion basique :
 * potion à boire, niveau 1, durée normale (les joueurs peuvent la modifier à l'alambic). La potion du tirage de
 * l'endermite est dans KS_LootEntites.
 */
public final class KSLootPotions extends JavaPlugin implements Listener {

    /** Potions basiques obtenables en survie (sans la chance, rendue rare par l'émeraude deepslate). */
    private static final List<PotionType> RANDOM_POOL = List.of(
            PotionType.SWIFTNESS, PotionType.SLOWNESS, PotionType.LEAPING, PotionType.STRENGTH, PotionType.HEALING,
            PotionType.HARMING, PotionType.POISON, PotionType.REGENERATION, PotionType.WEAKNESS,
            PotionType.INVISIBILITY, PotionType.WATER_BREATHING, PotionType.FIRE_RESISTANCE, PotionType.NIGHT_VISION,
            PotionType.SLOW_FALLING, PotionType.TURTLE_MASTER, PotionType.WIND_CHARGED, PotionType.WEAVING,
            PotionType.OOZING, PotionType.INFESTED);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private static boolean chance(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    private static PotionType randomPotion() {
        return RANDOM_POOL.get(ThreadLocalRandom.current().nextInt(RANDOM_POOL.size()));
    }

    static ItemStack basicPotion(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    // ------------------------------------------------------------------ blocs

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        PotionType potion = switch (event.getBlockState().getType()) {
            case DEEPSLATE_EMERALD_ORE -> !event.getItems().isEmpty() && chance(0.10) ? PotionType.LUCK : null;
            case CHORUS_PLANT -> chance(0.01) ? PotionType.SLOW_FALLING : null;
            case NETHER_WART -> chance(0.05) ? randomPotion() : null;
            default -> null;
        };
        if (potion != null) {
            Location center = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
            center.getWorld().dropItemNaturally(center, basicPotion(potion));
        }
    }

    // ------------------------------------------------------------------ mobs

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity.getKiller() == null) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        switch (entity.getType()) {
            case VEX -> add(drops, 0.10, PotionType.INFESTED);
            case PHANTOM -> add(drops, 0.10, PotionType.SLOW_FALLING);
            case PILLAGER -> {
                add(drops, 0.10, PotionType.WEAKNESS);
                if (((Raider) entity).isPatrolLeader() && chance(0.01)) {
                    drops.add(basicPotion(randomPotion()));
                }
            }
            case STRIDER -> add(drops, 0.01, PotionType.FIRE_RESISTANCE);
            case RABBIT -> add(drops, 0.01, PotionType.LEAPING);
            case SLIME -> {
                if (((Slime) entity).getSize() >= 4) {
                    add(drops, 0.10, PotionType.INFESTED);
                }
            }
            case BREEZE -> add(drops, 0.10, PotionType.WIND_CHARGED);
            case ALLAY -> add(drops, 0.10, PotionType.HEALING);
            case SPIDER -> add(drops, 0.01, PotionType.WEAVING);
            case CAVE_SPIDER -> add(drops, 0.01, PotionType.POISON);
            case GUARDIAN -> add(drops, 0.10, PotionType.WATER_BREATHING);
            case ELDER_GUARDIAN -> add(drops, 1.0, PotionType.WATER_BREATHING);
            case GHAST -> add(drops, 0.10, PotionType.REGENERATION);
            case WANDERING_TRADER -> add(drops, 1.0, PotionType.INVISIBILITY);
            case BLAZE -> add(drops, 0.10, PotionType.STRENGTH);
            case HORSE, DONKEY, MULE -> add(drops, 0.10, PotionType.SWIFTNESS);
            default -> {
            }
        }
    }

    private static void add(List<ItemStack> drops, double probability, PotionType type) {
        if (chance(probability)) {
            drops.add(basicPotion(type));
        }
    }
}
