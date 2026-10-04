package fr.kalium.lootcoffres;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * KS_LootCoffres (demande de Maxster33, 28/09/2026) : loots des coffres de structures du serveur Event.
 *
 * Principe : le loot vanilla (tables de la 26.2) est généré normalement, puis modifié juste avant d'être mis dans le
 * coffre (LootGenerateEvent) ou éjecté par le coffre-fort (BlockDispenseLootEvent). Les changements de poids sont
 * faits par remplacement, avec des probabilités calculées pour donner exactement les nouveaux poids (détail dans
 * JOURNAL.md) ; tout ce qui n'est pas cité reste vanilla. Seuls les coffres pas encore ouverts sont concernés.
 */
public final class KSLootCoffres extends JavaPlugin implements Listener {

    /**
     * Cité antique : 5 à 10 tirages, totem au poids 1 sur 168 (voir ancientCity). Trésor enfoui : même chance par
     * coffre, 1 - moyenne de (167/168)^n pour n de 5 à 10 (environ 4,4 %).
     */
    private static final double TOTEM_TRESOR;

    static {
        double sum = 0;
        for (int n = 5; n <= 10; n++) {
            sum += Math.pow(167.0 / 168.0, n);
        }
        TOTEM_TRESOR = 1 - sum / 6;
    }

    /** Objets du tirage « expérience, ficelle, flèches... » de l'avant-poste (seul tirage qui les contient). */
    private static final Set<Material> AVANT_POSTE_TIRAGE = Set.of(Material.EXPERIENCE_BOTTLE, Material.STRING,
            Material.ARROW, Material.TRIPWIRE_HOOK, Material.IRON_INGOT, Material.ENCHANTED_BOOK);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(String.format("Totem du trésor enfoui : %.2f %% par coffre", TOTEM_TRESOR * 100));
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    private static boolean chance(double probability) {
        return random().nextDouble() < probability;
    }

    /** Entier au hasard entre min et max compris. */
    private static int entre(int min, int max) {
        return random().nextInt(min, max + 1);
    }

    private static String cle(LootTable table) {
        NamespacedKey key = table == null ? null : table.getKey();
        return key == null || !key.getNamespace().equals(NamespacedKey.MINECRAFT) ? "" : key.getKey();
    }

    // ------------------------------------------------------------------ coffres

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChest(LootGenerateEvent event) {
        List<ItemStack> loot = new ArrayList<>(event.getLoot());
        switch (cle(event.getLootTable())) {
            case "chests/ancient_city" -> ancientCity(loot);
            case "chests/bastion_bridge", "chests/bastion_hoglin_stable", "chests/bastion_other" ->
                    sansModeleNetherite(loot);
            case "chests/bastion_treasure" -> {
                sansModeleNetherite(loot);
                bastionTreasure(loot);
            }
            case "chests/buried_treasure" -> {
                if (chance(TOTEM_TRESOR)) {
                    loot.add(new ItemStack(Material.TOTEM_OF_UNDYING));
                }
            }
            case "chests/end_city_treasure" -> endCity(loot);
            case "chests/nether_bridge" -> netherBridge(loot);
            case "chests/pillager_outpost" -> pillagerOutpost(loot);
            case "chests/woodland_mansion" -> woodlandMansion(loot);
            default -> {
                return;
            }
        }
        event.setLoot(loot);
    }

    /**
     * Jambières en diamant : niveau 30 à 50, comme le plastron des bastions, les bottes des cités de l'End et le
     * casque de l'Estomac du gardien (1.1.0, LeKiwi06 ; 1.0.0 : 10 à 32). Pomme dorée enchantée : poids divisé par 2 et totem
     * au même poids. Vanilla : pomme 1 sur 84 ; voulu : tous les autres poids x2 (168 au total), pomme 1, totem 1.
     * Les autres objets gardent donc leur chance (2w/168 = w/84) : il suffit qu'une pomme sur deux devienne un totem.
     */
    private static void ancientCity(List<ItemStack> loot) {
        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            if (stack.getType() == Material.DIAMOND_LEGGINGS && !stack.getEnchantments().isEmpty()) {
                loot.set(i, reEnchanter(stack, 30, 50));
            } else if (stack.getType() == Material.ENCHANTED_GOLDEN_APPLE && chance(0.50)) {
                loot.set(i, new ItemStack(Material.TOTEM_OF_UNDYING));
            }
        }
    }

    /** Les 4 coffres de bastion : plus de modèle de forge (amélioration en netherite). */
    private static void sansModeleNetherite(List<ItemStack> loot) {
        loot.removeIf(stack -> stack.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    }

    /**
     * Salle au trésor : les pièces en diamant enchantées (la table vanilla a aussi une version sans enchantement de
     * chacune, inchangée) : plastron niveau 30 à 50, épée, lance, casque, jambières, bottes niveau 1 à 29.
     */
    private static void bastionTreasure(List<ItemStack> loot) {
        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            if (stack.getEnchantments().isEmpty()) {
                continue;
            }
            switch (stack.getType()) {
                case DIAMOND_CHESTPLATE -> loot.set(i, reEnchanter(stack, 30, 50));
                case DIAMOND_SWORD, DIAMOND_SPEAR, DIAMOND_HELMET, DIAMOND_LEGGINGS, DIAMOND_BOOTS ->
                        loot.set(i, reEnchanter(stack, 1, 29));
                default -> {
                }
            }
        }
    }

    /** Cité de l'End : bottes en diamant niveau 30 à 50 ; épée, lance, plastron, casque, jambières niveau 1 à 29. */
    private static void endCity(List<ItemStack> loot) {
        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            switch (stack.getType()) {
                case DIAMOND_BOOTS -> loot.set(i, reEnchanter(stack, 30, 50));
                case DIAMOND_SWORD, DIAMOND_SPEAR, DIAMOND_CHESTPLATE, DIAMOND_HELMET, DIAMOND_LEGGINGS ->
                        loot.set(i, reEnchanter(stack, 1, 29));
                default -> {
                }
            }
        }
    }

    /**
     * Forteresse du Nether, tirage principal (poids total vanilla 78) : ajout du crâne de wither squelette (poids 2)
     * et du débris antique (poids 2), soit 4 sur 82 : chaque objet du tirage est remplacé avec cette chance.
     * Quantités : lingot d'or 3 à 5, lingot de fer 5 à 11, diamant 2 à 4.
     */
    private static void netherBridge(List<ItemStack> loot) {
        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            if (stack.getType() == Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE) {
                continue; // autre tirage
            }
            if (chance(4.0 / 82.0)) {
                loot.set(i, new ItemStack(chance(0.50) ? Material.WITHER_SKELETON_SKULL : Material.ANCIENT_DEBRIS));
                continue;
            }
            switch (stack.getType()) {
                case GOLD_INGOT -> stack.setAmount(entre(3, 5));
                case IRON_INGOT -> stack.setAmount(entre(5, 11));
                case DIAMOND -> stack.setAmount(entre(2, 4));
                default -> {
                }
            }
        }
    }

    /** Avant-poste : fiole sinistre ajoutée (poids 3) au tirage de l'expérience (poids total vanilla 22) : 3 sur 25. */
    private static void pillagerOutpost(List<ItemStack> loot) {
        for (int i = 0; i < loot.size(); i++) {
            if (AVANT_POSTE_TIRAGE.contains(loot.get(i).getType()) && chance(3.0 / 25.0)) {
                loot.set(i, new ItemStack(Material.OMINOUS_BOTTLE));
            }
        }
    }

    /** Manoir : livres enchantés au niveau 30 à 50 (au lieu d'un enchantement au hasard). */
    private static void woodlandMansion(List<ItemStack> loot) {
        for (int i = 0; i < loot.size(); i++) {
            if (loot.get(i).getType() == Material.ENCHANTED_BOOK) {
                loot.set(i, enchanter(new ItemStack(Material.BOOK), entre(30, 50)));
            }
        }
    }

    // ------------------------------------------------------------------ coffres-forts des chambres d'épreuve

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVault(BlockDispenseLootEvent event) {
        String table = cle(event.getLootTable());
        boolean ominous;
        if (table.equals("chests/trial_chambers/reward")) {
            ominous = false;
        } else if (table.equals("chests/trial_chambers/reward_ominous")) {
            ominous = true;
        } else {
            return;
        }
        List<ItemStack> loot = new ArrayList<>(event.getDispensedLoot());
        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            if (stack.getType() == Material.OMINOUS_BOTTLE) {
                // Les fioles ne viennent que du tirage commun. Gardée avec la chance p, sinon remplacée par un autre
                // objet du tirage commun (poids vanilla, fiole exclue).
                if (!ominous && !chance(25.0 / 48.0)) {
                    loot.set(i, communAutre());
                } else if (ominous && !chance(15.0 / 29.0)) {
                    loot.set(i, communSinistreAutre());
                }
            } else if (ominous && stack.getType() == Material.ENCHANTED_BOOK) {
                ventRafaleAleatoire(stack);
            }
        }
        event.setDispensedLoot(loot);
    }

    /**
     * Coffre-fort, tirage commun : fiole sinistre poids 2 → 1 (total 25 → 24). Vanilla : fiole 2/25 ; gardée avec
     * p = 25/48 : 2/25 x 25/48 = 1/24. Les autres (poids total 23) : w/25 + 2/25 x 23/48 x w/23 = w/24.
     */
    private static ItemStack communAutre() {
        return tirer(List.of(
                new Entree(4, () -> new ItemStack(Material.ARROW, entre(2, 8))),
                new Entree(4, () -> fleche(PotionType.POISON, entre(2, 8))),
                new Entree(4, () -> new ItemStack(Material.EMERALD, entre(2, 4))),
                new Entree(3, () -> new ItemStack(Material.WIND_CHARGE, entre(1, 3))),
                new Entree(3, () -> new ItemStack(Material.IRON_INGOT, entre(1, 4))),
                new Entree(3, () -> new ItemStack(Material.HONEY_BOTTLE, entre(1, 2))),
                new Entree(1, () -> new ItemStack(Material.WIND_CHARGE, entre(4, 12))),
                new Entree(1, () -> new ItemStack(Material.DIAMOND, entre(1, 2)))));
    }

    /**
     * Coffre-fort sinistre, tirage commun : tous les poids x2 sauf la fiole (total 15 → 29). Vanilla : fiole 1/15 ;
     * gardée avec p = 15/29 : 1/29. Les autres (poids total 14) : w/15 + 1/15 x 14/29 x w/14 = 2w/29.
     */
    private static ItemStack communSinistreAutre() {
        return tirer(List.of(
                new Entree(5, () -> new ItemStack(Material.EMERALD, entre(4, 10))),
                new Entree(4, () -> new ItemStack(Material.WIND_CHARGE, entre(8, 12))),
                new Entree(3, () -> fleche(PotionType.STRONG_SLOWNESS, entre(4, 12))),
                new Entree(2, () -> new ItemStack(Material.DIAMOND, entre(2, 3)))));
    }

    /** Coffre-fort sinistre, tirage rare : livre Rafale de vent au niveau 1, 2 ou 3 (seul livre qui l'a). */
    private static void ventRafaleAleatoire(ItemStack book) {
        if (!(book.getItemMeta() instanceof EnchantmentStorageMeta meta) || !meta.hasStoredEnchant(Enchantment.WIND_BURST)) {
            return;
        }
        meta.removeStoredEnchant(Enchantment.WIND_BURST);
        meta.addStoredEnchant(Enchantment.WIND_BURST, entre(1, 3), true);
        book.setItemMeta(meta);
    }

    private static ItemStack fleche(PotionType type, int amount) {
        ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta meta = (PotionMeta) arrow.getItemMeta();
        meta.setBasePotionType(type);
        arrow.setItemMeta(meta);
        return arrow;
    }

    private record Entree(int poids, Supplier<ItemStack> objet) {
    }

    private static ItemStack tirer(List<Entree> entrees) {
        int roll = random().nextInt(entrees.stream().mapToInt(Entree::poids).sum());
        for (Entree entree : entrees) {
            roll -= entree.poids();
            if (roll < 0) {
                return entree.objet().get();
            }
        }
        throw new IllegalStateException();
    }

    // ------------------------------------------------------------------ enchantements

    /** Comme la fonction vanilla enchant_with_levels (enchantements « #on_random_loot »). */
    private static ItemStack enchanter(ItemStack item, int niveaux) {
        return Bukkit.getItemFactory().enchantWithLevels(item, niveaux,
                RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                        .getTag(EnchantmentTagKeys.ON_RANDOM_LOOT),
                random());
    }

    /** Retire les enchantements et réenchante au niveau min à max, en gardant l'usure de l'objet. */
    private static ItemStack reEnchanter(ItemStack stack, int min, int max) {
        ItemStack copy = stack.clone();
        copy.removeEnchantments();
        int damage = copy.getItemMeta() instanceof Damageable d ? d.getDamage() : 0;
        ItemStack result = enchanter(copy, entre(min, max));
        if (damage > 0 && result.getItemMeta() instanceof Damageable meta) {
            meta.setDamage(damage);
            result.setItemMeta((ItemMeta) meta);
        }
        return result;
    }
}
