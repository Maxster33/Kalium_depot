package fr.kalium.pvpkit;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Declassement du kit (demande de LeKiwi06, 28/09/2026) : chaque joueur peut jouer avec un kit affaibli pour gagner plus
 * de points. Chaque niveau retire un niveau a chaque enchantement des objets (un enchantement tombe a 0 disparait) et
 * 20 % (reglable) des consommables, compte par sorte d'objet sur tout le kit (niveau 4 : il en reste 20 %). Regles
 * particulieres des objets en un seul exemplaire (totem, pomme de Notch, potions) : voir reduceConsumables.
 */
public final class Downgrade {

    /** Consommables qui ne se mangent / boivent pas (le reste est detecte par le composant « consumable »). */
    private static final Set<Material> OTHER_CONSUMABLES = Set.of(
            Material.ENDER_PEARL, Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW,
            Material.TOTEM_OF_UNDYING, Material.WIND_CHARGE, Material.FIREWORK_ROCKET, Material.SNOWBALL, Material.EGG,
            Material.EXPERIENCE_BOTTLE, Material.SPLASH_POTION, Material.LINGERING_POTION);

    private Downgrade() {
    }

    /** Aliments, potions, perles, fleches, totems, charges de vent, fusees... (pas les blocs). */
    public static boolean consumable(ItemStack item) {
        return OTHER_CONSUMABLES.contains(item.getType()) || item.hasData(DataComponentTypes.CONSUMABLE);
    }

    /** Copie du kit au niveau de declassement donne (0 = copie identique). */
    public static Kit apply(Kit kit, int level, int consumablePercentPerLevel) {
        Kit copy = new Kit(kit.id(), kit.display());
        for (int i = 0; i < 4; i++) {
            copy.armor()[i] = weaken(kit.armor()[i], level);
        }
        copy.offhand(weaken(kit.offhand(), level));
        for (Map.Entry<Integer, ItemStack> entry : kit.slots().entrySet()) {
            copy.slots().put(entry.getKey(), weaken(entry.getValue(), level));
        }
        for (ItemStack item : kit.auto()) {
            copy.auto().add(weaken(item, level));
        }
        if (level > 0) {
            reduceConsumables(copy, level, Math.max(0, 100 - consumablePercentPerLevel * level));
        }
        return copy;
    }

    private static ItemStack weaken(ItemStack item, int level) {
        if (item == null) {
            return null;
        }
        ItemStack copy = item.clone();
        if (level <= 0 || !copy.hasItemMeta()) {
            return copy;
        }
        ItemMeta meta = copy.getItemMeta();
        for (Map.Entry<Enchantment, Integer> entry : new LinkedHashMap<>(meta.getEnchants()).entrySet()) {
            meta.removeEnchant(entry.getKey());
            if (entry.getValue() - level > 0) {
                meta.addEnchant(entry.getKey(), entry.getValue() - level, true);
            }
        }
        if (meta instanceof EnchantmentStorageMeta book) {
            for (Map.Entry<Enchantment, Integer> entry : new LinkedHashMap<>(book.getStoredEnchants()).entrySet()) {
                book.removeStoredEnchant(entry.getKey());
                if (entry.getValue() - level > 0) {
                    book.addStoredEnchant(entry.getKey(), entry.getValue() - level, true);
                }
            }
        }
        copy.setItemMeta(meta);
        return copy;
    }

    /**
     * Consommables : ranges par objet identique (une potion de vitesse et une potion de force sont deux sortes
     * differentes). Regles des objets presents en un seul exemplaire (demande de LeKiwi06, 28/09/2026) :
     * <ul>
     *   <li>totem d'immortalite : retire a partir du niveau 2 ;</li>
     *   <li>pomme de Notch : retiree au niveau 4 ;</li>
     *   <li>potion (a boire, jetable, persistante) : jamais retiree, chaque effet perd un niveau par niveau de
     *       declassement, sans descendre sous le niveau I (vitesse V au declassement 4 = vitesse I).</li>
     * </ul>
     * Tout le reste (et ces objets en plusieurs exemplaires) : on garde keepPercent % (arrondi au plus proche), en
     * retirant d'abord les derniers objets.
     */
    private static void reduceConsumables(Kit kit, int level, int keepPercent) {
        List<List<ItemStack>> groups = new ArrayList<>();
        List<ItemStack> all = new ArrayList<>();
        if (kit.offhand() != null) {
            all.add(kit.offhand());
        }
        all.addAll(kit.slots().values());
        all.addAll(kit.auto());
        for (ItemStack item : all) {
            if (!consumable(item)) {
                continue;
            }
            List<ItemStack> group = null;
            for (List<ItemStack> candidate : groups) {
                if (candidate.get(0).isSimilar(item)) {
                    group = candidate;
                    break;
                }
            }
            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }
            group.add(item);
        }
        for (List<ItemStack> stacks : groups) {
            int total = 0;
            for (ItemStack stack : stacks) {
                total += stack.getAmount();
            }
            int keep;
            Material type = stacks.get(0).getType();
            if (total == 1 && type == Material.TOTEM_OF_UNDYING) {
                keep = level >= 2 ? 0 : 1;
            } else if (total == 1 && type == Material.ENCHANTED_GOLDEN_APPLE) {
                keep = level >= 4 ? 0 : 1;
            } else if (total == 1 && POTIONS.contains(type)) {
                weakenPotion(stacks.get(0), level);
                keep = 1;
            } else {
                keep = (int) Math.round(total * keepPercent / 100.0);
            }
            int remove = total - keep;
            for (int i = stacks.size() - 1; i >= 0 && remove > 0; i--) {
                ItemStack stack = stacks.get(i);
                int taken = Math.min(remove, stack.getAmount());
                stack.setAmount(stack.getAmount() - taken); // 0 = objet vide, retire ci-dessous
                remove -= taken;
            }
        }
        kit.slots().values().removeIf(item -> item.getAmount() <= 0 || item.getType().isAir());
        kit.auto().removeIf(item -> item.getAmount() <= 0 || item.getType().isAir());
        if (kit.offhand() != null && (kit.offhand().getAmount() <= 0 || kit.offhand().getType().isAir())) {
            kit.offhand(null);
        }
    }

    private static final Set<Material> POTIONS = Set.of(Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION);

    /** Chaque effet de la potion perd level niveaux (minimum : niveau I) ; la couleur de la potion est gardee. */
    private static void weakenPotion(ItemStack potion, int level) {
        if (level <= 0 || !(potion.getItemMeta() instanceof PotionMeta meta)) {
            return;
        }
        List<PotionEffect> effects = new ArrayList<>();
        if (meta.getBasePotionType() != null) {
            effects.addAll(meta.getBasePotionType().getPotionEffects());
        }
        effects.addAll(meta.getCustomEffects());
        org.bukkit.Color color = meta.computeEffectiveColor();
        meta.setBasePotionType(null);
        meta.clearCustomEffects();
        for (PotionEffect effect : effects) {
            meta.addCustomEffect(effect.withAmplifier(Math.max(0, effect.getAmplifier() - level)), true);
        }
        meta.setColor(color);
        potion.setItemMeta(meta);
    }
}
