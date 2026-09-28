package fr.kalium.estomacgardien;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_EstomacGardien (demande de Maxster33, 28/09/2026) : objet « Estomac du gardien », avec l'image du sac noir.
 * Clic droit : l'objet est consommé et donne son contenu (inventaire plein : le reste tombe au sol).
 *
 * L'objet de base est un livre de connaissances (ni sac, ni bloc, ni ingrédient de craft) dont le clic droit vanilla
 * est annulé ; seule l'image (item_model) est celle du sac noir. Autres plugins : creerEstomac() (ex. KS_LootEntites).
 */
public final class KSEstomacGardien extends JavaPlugin implements Listener {

    private static final List<Material> CORAUX = List.of(
            Material.TUBE_CORAL, Material.BRAIN_CORAL, Material.BUBBLE_CORAL, Material.FIRE_CORAL, Material.HORN_CORAL,
            Material.TUBE_CORAL_FAN, Material.BRAIN_CORAL_FAN, Material.BUBBLE_CORAL_FAN, Material.FIRE_CORAL_FAN,
            Material.HORN_CORAL_FAN,
            Material.TUBE_CORAL_BLOCK, Material.BRAIN_CORAL_BLOCK, Material.BUBBLE_CORAL_BLOCK,
            Material.FIRE_CORAL_BLOCK, Material.HORN_CORAL_BLOCK);

    private static final List<Material> ALGUES = List.of(Material.KELP, Material.SEAGRASS);

    /** Tous les bateaux et radeaux, avec ou sans coffre. */
    private static final List<Material> BATEAUX = Arrays.stream(Material.values())
            .filter(m -> !m.isLegacy() && m.isItem())
            .filter(m -> m.name().endsWith("_BOAT") || m.name().endsWith("_RAFT"))
            .toList();

    private static NamespacedKey cle;

    @Override
    public void onEnable() {
        cle = new NamespacedKey(this, "estomac");
        getServer().getPluginManager().registerEvents(this, this);
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    /** Entier au hasard entre min et max compris. */
    private static int entre(int min, int max) {
        return random().nextInt(min, max + 1);
    }

    // ------------------------------------------------------------------ objet

    /** Un Estomac du gardien. Nécessite que le plugin soit activé. */
    public static ItemStack creerEstomac() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Estomac du gardien", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft("black_bundle"));
        meta.getPersistentDataContainer().set(cle, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean estEstomac(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cle, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null || !estEstomac(event.getItem())) {
            return;
        }
        // Annule l'effet vanilla du livre de connaissances.
        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItem(hand);
        if (!estEstomac(inHand)) {
            return;
        }
        inHand.setAmount(inHand.getAmount() - 1);
        player.getInventory().setItem(hand, inHand.getAmount() > 0 ? inHand : null);
        for (ItemStack reste : player.getInventory().addItem(contenu().toArray(new ItemStack[0])).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), reste);
        }
    }

    // ------------------------------------------------------------------ contenu

    /**
     * Tirage 1 (1 tirage) : 10 % casque en diamant enchanté niveau 30 à 50.
     * Tirage 2 (2 tirages, poids) : oeuf de tortue 3, bateau 3, corail x2-3 10, algue x2-3 10, trident enchanté niveau
     * 10 à 29 3, armure de nautile en diamant 1, coeur de la mer 1.
     */
    static List<ItemStack> contenu() {
        List<ItemStack> items = new ArrayList<>();
        if (random().nextDouble() < 0.10) {
            items.add(enchanter(new ItemStack(Material.DIAMOND_HELMET), entre(30, 50)));
        }
        for (int i = 0; i < 2; i++) {
            items.add(tirage2());
        }
        return items;
    }

    private static ItemStack tirage2() {
        int roll = random().nextInt(31);
        if ((roll -= 3) < 0) {
            return new ItemStack(Material.TURTLE_EGG);
        }
        if ((roll -= 3) < 0) {
            return new ItemStack(BATEAUX.get(random().nextInt(BATEAUX.size())));
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(CORAUX.get(random().nextInt(CORAUX.size())), entre(2, 3));
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(ALGUES.get(random().nextInt(ALGUES.size())), entre(2, 3));
        }
        if ((roll -= 3) < 0) {
            return enchanter(new ItemStack(Material.TRIDENT), entre(10, 29));
        }
        if ((roll -= 1) < 0) {
            return new ItemStack(Material.DIAMOND_NAUTILUS_ARMOR);
        }
        return new ItemStack(Material.HEART_OF_THE_SEA);
    }

    /** Comme la fonction vanilla enchant_with_levels (enchantements « #on_random_loot »). */
    private static ItemStack enchanter(ItemStack item, int niveaux) {
        return Bukkit.getItemFactory().enchantWithLevels(item, niveaux,
                RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                        .getTag(EnchantmentTagKeys.ON_RANDOM_LOOT),
                random());
    }
}
