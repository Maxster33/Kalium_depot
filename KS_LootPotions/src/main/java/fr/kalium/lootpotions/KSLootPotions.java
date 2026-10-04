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

    private static boolean mure(org.bukkit.block.data.BlockData donnees) {
        return donnees instanceof org.bukkit.block.data.Ageable age && age.getAge() >= age.getMaximumAge();
    }

    /**
     * Vrai si le minerai casse a lache autre chose que lui-meme. Quand le bloc tombe tel quel (Toucher de soie, ou
     * les 2 minerais de KS_LootBlocs), pas de potion : sinon on reposerait le meme minerai pour en tirer des potions
     * a volonte.
     */
    private static boolean mine(BlockDropItemEvent event) {
        Material minerai = event.getBlockState().getType();
        return !event.getItems().isEmpty()
                && event.getItems().stream().noneMatch(item -> item.getItemStack().getType() == minerai);
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
            // 1.1.3 (LeKiwi06) : 100 % (avant : 10 %), seulement si le minerai a vraiment ete mine
            case DEEPSLATE_EMERALD_ORE -> mine(event) ? PotionType.LUCK : null;
            case CHORUS_PLANT -> chance(0.01) ? PotionType.SLOW_FALLING : null;
            // 1.1.2 (LeKiwi06) : la verrue est de nouveau cultivable (KS_LootBlocs 1.3.0) : seulement une verrue
            // mure, sinon poser puis casser une verrue donnerait des potions a volonte
            case NETHER_WART -> mure(event.getBlockState().getBlockData()) && chance(0.05) ? randomPotion() : null;
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
        // 1.1.1 (LeKiwi06) : pas de potion sur les mobs de spawner (comme KS_Decapitator) : sinon une ferme à spawner
        // (blazes, araignées...) donnerait des potions à l'infini.
        if (entity instanceof Player || entity.getKiller() == null
                || entity.getEntitySpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        switch (entity.getType()) {
            case VEX -> add(drops, 0.10, PotionType.INFESTED);
            case PHANTOM -> add(drops, 0.10, PotionType.SLOW_FALLING);
            // Capitaine seulement (1.1.0) : les pillards ordinaires n'ont plus de potion.
            case PILLAGER -> {
                if (((Raider) entity).isPatrolLeader()) {
                    add(drops, 0.10, PotionType.WEAKNESS);
                    add(drops, 0.01, randomPotion());
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
