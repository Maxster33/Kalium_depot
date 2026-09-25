package fr.kalium.lootentites;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_LootEntites (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md, Maxster33, 25/09/2026). But : nerfer les
 * fermes AFK, valoriser la découverte et le farm manuel.
 *
 * Réductions et remplacements : à toutes les morts (fermes comprises). Ajouts : seulement si un joueur tue ; Butin
 * (Looting) ne change pas les pourcentages. Les potions ajoutées sont dans KS_LootPotions, sauf celle du tirage de
 * l'endermite (un seul tirage de 6 objets).
 */
public final class KSLootEntites extends JavaPlugin implements Listener {

    /** Fleurs d'un bloc de haut (hors wither rose et torchflower, a 1 % chacune avec la pitcher plant). */
    private static final List<Material> FLOWERS = List.of(
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET,
            Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
            Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY);

    private static final int[] WARDEN_LEVELS = {10, 20, 30, 40, 50};

    private NamespacedKey xpLevelKey;

    @Override
    public void onEnable() {
        xpLevelKey = new NamespacedKey(this, "xp_level");
        getServer().getPluginManager().registerEvents(this, this);
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    private static boolean chance(double probability) {
        return random().nextDouble() < probability;
    }

    // ------------------------------------------------------------------ morts

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        boolean byPlayer = entity.getKiller() != null;
        EntityType type = entity.getType();

        switch (type) {
            case IRON_GOLEM -> ironGolem(drops);
            case ZOMBIFIED_PIGLIN -> divide(drops, 2);
            case WITCH -> divide(drops, 5);
            case SQUID, GLOW_SQUID -> drops.forEach(stack -> stack.setAmount(stack.getAmount() * 2));
            case WITHER_SKELETON -> {
                drops.removeIf(stack -> stack.getType() == Material.WITHER_SKELETON_SKULL);
                drops.stream().filter(stack -> stack.getType() == Material.COAL)
                        .forEach(stack -> stack.setAmount(stack.getAmount() * 2));
                if (byPlayer && chance(0.10)) {
                    drops.add(new ItemStack(Material.WITHER_ROSE));
                }
            }
            // Seul un capitaine (patrouille ou raid) lache une fiole sinistre.
            case PILLAGER -> drops.removeIf(stack -> stack.getType() == Material.OMINOUS_BOTTLE);
            case ENDERMITE -> {
                if (byPlayer && chance(0.50)) {
                    drops.add(endermiteLoot());
                }
            }
            case WARDEN -> {
                if (byPlayer && chance(0.10)) {
                    drops.add(xpBottle(WARDEN_LEVELS[random().nextInt(WARDEN_LEVELS.length)]));
                }
            }
            default -> {
            }
        }
        // Tous les mobs qui donnent de la chair putrefiee : chaque chair a 50 % de chance de devenir un os.
        fleshToBones(drops);
    }

    private static void ironGolem(List<ItemStack> drops) {
        int flowers = 0;
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : drops) {
            if (stack.getType() == Material.IRON_INGOT) {
                result.add(new ItemStack(Material.IRON_NUGGET, stack.getAmount()));
            } else if (stack.getType() == Material.POPPY) {
                flowers += stack.getAmount();
            } else {
                result.add(stack);
            }
        }
        for (int i = 0; i < half(flowers, 2); i++) {
            result.add(new ItemStack(randomFlower()));
        }
        drops.clear();
        drops.addAll(result);
    }

    /** 1 % wither rose, 1 % pitcher plant, 1 % torchflower, sinon une des 12 autres fleurs d'un bloc de haut. */
    private static Material randomFlower() {
        double roll = random().nextDouble();
        if (roll < 0.01) {
            return Material.WITHER_ROSE;
        }
        if (roll < 0.02) {
            return Material.PITCHER_PLANT;
        }
        if (roll < 0.03) {
            return Material.TORCHFLOWER;
        }
        return FLOWERS.get(random().nextInt(FLOWERS.size()));
    }

    /** Divise une quantite, le reste etant arrondi au hasard (ex. 1 / 2 : 1 une fois sur deux). */
    private static int half(int amount, int divisor) {
        int result = amount / divisor;
        if (random().nextInt(divisor) < amount % divisor) {
            result++;
        }
        return result;
    }

    private static void divide(List<ItemStack> drops, int divisor) {
        for (ItemStack stack : drops) {
            stack.setAmount(half(stack.getAmount(), divisor));
        }
        drops.removeIf(stack -> stack.getAmount() <= 0);
    }

    private static void fleshToBones(List<ItemStack> drops) {
        int bones = 0;
        for (ItemStack stack : drops) {
            if (stack.getType() != Material.ROTTEN_FLESH) {
                continue;
            }
            int flesh = 0;
            for (int i = 0; i < stack.getAmount(); i++) {
                if (chance(0.50)) {
                    bones++;
                } else {
                    flesh++;
                }
            }
            stack.setAmount(flesh);
        }
        drops.removeIf(stack -> stack.getAmount() <= 0);
        if (bones > 0) {
            drops.add(new ItemStack(Material.BONE, bones));
        }
    }

    /** Un des 6 objets, a chances egales. */
    private ItemStack endermiteLoot() {
        return switch (random().nextInt(6)) {
            case 0 -> new ItemStack(Material.BUDDING_AMETHYST);
            case 1 -> new ItemStack(Material.SHULKER_SHELL);
            case 2 -> basicPotion(PotionType.REGENERATION);
            case 3 -> xpBottle(10);
            case 4 -> new ItemStack(Material.CHORUS_FRUIT);
            default -> new ItemStack(Material.ENDER_PEARL, 8);
        };
    }

    private static ItemStack basicPotion(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    // ------------------------------------------------------------------ fioles d'experience a niveau

    /** Fiole d'experience vanilla marquee : lancee, elle donne l'XP pour passer du niveau 0 au niveau indique. */
    private ItemStack xpBottle(int level) {
        ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = bottle.getItemMeta();
        meta.displayName(Component.text("Fiole d'expérience (niveau " + level + ")").decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(xpLevelKey, PersistentDataType.INTEGER, level);
        bottle.setItemMeta(meta);
        return bottle;
    }

    /** Points d'experience pour aller du niveau 0 au niveau donne (formule vanilla). */
    static int pointsForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBottle(ExpBottleEvent event) {
        ThrownExpBottle bottle = event.getEntity();
        ItemStack item = bottle.getItem();
        if (!item.hasItemMeta()) {
            return;
        }
        Integer level = item.getItemMeta().getPersistentDataContainer().get(xpLevelKey, PersistentDataType.INTEGER);
        if (level != null) {
            event.setExperience(pointsForLevel(level));
        }
    }
}
