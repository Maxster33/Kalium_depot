package fr.kalium.piliers;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Objets aleatoires des piliers de la Fortune (decision de Maxster33, 06/10/2026) : tous les objets de survie, oeufs
 * d'apparition compris. Chaque objet a la meme chance de sortir ; un seul exemplaire a la fois.
 * <ul>
 *   <li>Exclus : objets de commande, de structure ou de debogage (jamais obtenus en survie), blocs qu'on ne peut pas
 *       recuperer en survie (bedrock, cadre de portail de l'End, generateurs...), les oeufs du Wither et de l'Ender
 *       Dragon (boss qui detruisent l'arene : choix technique signale), et le livre enchante (demande de Maxster33).</li>
 *   <li>Potions, potions jetables, persistantes, fleches a effet : effet tire au hasard (sinon une simple fiole d'eau).</li>
 * </ul>
 * La liste est calculee une fois par monde (objets actives par les options du monde seulement).
 */
final class RandomItems {

    private static final Set<Material> EXCLUDED = EnumSet.of(
            Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART, Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.JIGSAW,
            Material.TEST_BLOCK, Material.TEST_INSTANCE_BLOCK, Material.BARRIER, Material.LIGHT, Material.DEBUG_STICK,
            Material.KNOWLEDGE_BOOK, Material.BEDROCK, Material.BUDDING_AMETHYST, Material.CHORUS_PLANT,
            Material.DIRT_PATH, Material.END_PORTAL_FRAME, Material.FARMLAND, Material.FROGSPAWN,
            Material.PETRIFIED_OAK_SLAB, Material.REINFORCED_DEEPSLATE, Material.SPAWNER, Material.TRIAL_SPAWNER,
            Material.VAULT, Material.SUSPICIOUS_SAND, Material.SUSPICIOUS_GRAVEL,
            Material.WITHER_SPAWN_EGG, Material.ENDER_DRAGON_SPAWN_EGG,
            Material.ENCHANTED_BOOK);

    private World world;
    private List<Material> pool = List.of();
    private List<PotionType> potions = List.of();

    @SuppressWarnings("deprecation")
    private void build(World target) {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isItem() || material.isAir() || EXCLUDED.contains(material)
                    || material.name().startsWith("INFESTED_")) {
                continue;
            }
            try {
                if (!target.isEnabled(material.asItemType())) {
                    continue; // objet d'une fonctionnalite experimentale non activee
                }
            } catch (RuntimeException e) {
                continue;
            }
            materials.add(material);
        }
        List<PotionType> types = new ArrayList<>();
        for (PotionType type : Registry.POTION) {
            if (type != PotionType.WATER && target.isEnabled(type)) {
                types.add(type);
            }
        }
        this.world = target;
        this.pool = materials;
        this.potions = types;
    }

    /** Un objet tire au hasard (un seul exemplaire). */
    ItemStack random(World target) {
        if (target != world || pool.isEmpty()) {
            build(target);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Material material = pool.get(random.nextInt(pool.size()));
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof PotionMeta meta && !potions.isEmpty()) {
            meta.setBasePotionType(potions.get(random.nextInt(potions.size())));
            item.setItemMeta(meta);
        }
        return item;
    }

    int size() {
        return pool.size();
    }
}
