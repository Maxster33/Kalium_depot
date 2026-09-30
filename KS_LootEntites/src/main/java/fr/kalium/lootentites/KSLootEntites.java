package fr.kalium.lootentites;

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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_LootEntites (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md, Maxster33, 25/09/2026). But : nerfer les
 * fermes AFK, valoriser la découverte et le farm manuel.
 *
 * Réductions et remplacements : à toutes les morts (fermes comprises). Ajouts : seulement si un joueur tue ; Butin
 * (Looting) ne change pas les pourcentages. Les potions ajoutées sont dans KS_LootPotions, sauf celle du tirage de
 * l'endermite (un seul tirage de 6 objets).
 *
 * 1.3.0 (LeKiwi06, catégorie 1 « Contenu survie ») : un seul système de fiole, celui de KS_FioleExp ; le Warden et
 * l'endermite ne créent plus l'ancienne fiole « niveau N » de ce plugin (rien sans KS_FioleExp). Les anciennes fioles
 * déjà en jeu restent utilisables (onBottle).
 */
public final class KSLootEntites extends JavaPlugin implements Listener {

    /** Fleurs d'un bloc de haut (hors wither rose et torchflower, a 1 % chacune avec la pitcher plant). */
    private static final List<Material> FLOWERS = List.of(
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET,
            Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
            Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY);

    /** 1.2.0 (Maxster33) : un tirage de fiole au Warden, niveaux et poids (avant : 10 % d'une fiole 10 à 50). */
    private static final int[] WARDEN_LEVELS = {10, 15, 20, 30, 40};
    /** 1.4.0 (LeKiwi06) : fioles 50 % plus communes, 15 % au lieu de 10 % (poids x3 : 105 sur 700 ; mêmes proportions). */
    private static final int[] WARDEN_WEIGHTS = {45, 30, 18, 9, 3};
    /** 1.2.1 (Maxster33) : ligne « rien » du tirage (1.2.1 : 315 sur 350 ; 1.4.0 : 595 sur 700). */
    private static final int WARDEN_RIEN = 595;

    /**
     * 1.4.0 (LeKiwi06) : le catalyseur de sculk du Warden est remplacé par un bloc au hasard des blocs naturels du biome
     * Deep Dark (sans la cité antique) ; minerais extrêmement rares (poids 1 contre 100 pour les autres blocs).
     */
    private static final Map<Material, Integer> DEEP_DARK = new LinkedHashMap<>();

    static {
        for (Material bloc : List.of(Material.SCULK, Material.SCULK_VEIN, Material.SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_CATALYST, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
                Material.TUFF, Material.GRAVEL, Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE)) {
            DEEP_DARK.put(bloc, 100);
        }
        for (Material minerai : List.of(Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_DIAMOND_ORE)) {
            DEEP_DARK.put(minerai, 1);
        }
    }

    private NamespacedKey xpLevelKey;

    @Override
    public void onEnable() {
        xpLevelKey = new NamespacedKey(this, "xp_level");
        getServer().getPluginManager().registerEvents(this, this);
        if (!fioleExpActif()) {
            getLogger().warning("KS_FioleExp absent : le Warden et l'endermite ne donnent pas de fiole d'expérience.");
        }
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
                ItemStack loot = byPlayer && chance(0.50) ? endermiteLoot() : null;
                if (loot != null) {
                    drops.add(loot);
                }
            }
            case WARDEN -> {
                for (ItemStack stack : drops) {
                    if (stack.getType() == Material.SCULK_CATALYST) {
                        stack.setType(blocDeepDark());
                        stack.setAmount(1);
                    }
                }
                int level = byPlayer ? wardenLevel() : 0;
                if (level > 0 && fioleExpActif()) {
                    drops.add(fr.kalium.fioleexp.KSFioleExp.creerFioleNiveaux(level));
                }
            }
            // Objet du plugin KS_EstomacGardien (ignore s'il n'est pas active).
            case ELDER_GUARDIAN -> {
                if (byPlayer && estomacGardienActif() && chance(0.50)) {
                    drops.add(fr.kalium.estomacgardien.KSEstomacGardien.creerEstomac());
                }
            }
            default -> {
            }
        }
        // Tous les mobs qui donnent de la chair putrefiee : chaque chair a 50 % de chance de devenir un os.
        fleshToBones(drops);
    }

    private boolean fioleExpActif() {
        return getServer().getPluginManager().isPluginEnabled("KS_FioleExp");
    }

    private boolean estomacGardienActif() {
        return getServer().getPluginManager().isPluginEnabled("KS_EstomacGardien");
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

    /** Un des 6 objets, a chances egales (null : fiole sans KS_FioleExp, 1.3.0). */
    private ItemStack endermiteLoot() {
        return switch (random().nextInt(6)) {
            case 0 -> new ItemStack(Material.BUDDING_AMETHYST);
            case 1 -> new ItemStack(Material.SHULKER_SHELL);
            case 2 -> basicPotion(PotionType.REGENERATION);
            // 1.2.0 (Maxster33) : nouvelle fiole de KS_FioleExp (points en description) ; 1.3.0 : plus d'ancienne fiole.
            case 3 -> fioleExpActif() ? fr.kalium.fioleexp.KSFioleExp.creerFioleNiveaux(10) : null;
            case 4 -> new ItemStack(Material.CHORUS_FRUIT);
            default -> new ItemStack(Material.ENDER_PEARL, 8);
        };
    }

    /** Niveau de la fiole du Warden, 0 pour « rien » : un tirage pondere (1.4.0 : 10 : 45, 15 : 30, 20 : 18, 30 : 9,
     * 40 : 3, rien : 595, sur 700). */
    private static int wardenLevel() {
        int total = WARDEN_RIEN;
        for (int weight : WARDEN_WEIGHTS) {
            total += weight;
        }
        int roll = random().nextInt(total);
        for (int i = 0; i < WARDEN_LEVELS.length; i++) {
            roll -= WARDEN_WEIGHTS[i];
            if (roll < 0) {
                return WARDEN_LEVELS[i];
            }
        }
        return 0;
    }

    /** 1.4.0 : un bloc du Deep Dark, tirage pondere (DEEP_DARK). */
    private static Material blocDeepDark() {
        int total = DEEP_DARK.values().stream().mapToInt(Integer::intValue).sum();
        int roll = random().nextInt(total);
        for (Map.Entry<Material, Integer> entree : DEEP_DARK.entrySet()) {
            roll -= entree.getValue();
            if (roll < 0) {
                return entree.getKey();
            }
        }
        return Material.SCULK;
    }

    private static ItemStack basicPotion(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    // ------------------------------------------------------------------ anciennes fioles d'experience a niveau (1.3.0 : lecture seule)

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
