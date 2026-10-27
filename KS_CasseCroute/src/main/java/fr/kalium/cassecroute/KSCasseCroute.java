package fr.kalium.cassecroute;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * KS_CasseCroute (demande de LeKiwi06, 27/10/2026) : le Casse-croûte doré.
 *
 * - Recette sans forme : pomme d'or, carotte dorée, pissenlit doré, tranche de pastèque scintillante, tournesol, bol.
 * - Aliment : 7 points de nourriture, 5 de saturation, mangeable même rassasié ; donne Absorption II (4 cœurs),
 *   Célérité II et Vitesse I pendant 5 minutes. Pile de 16, bol non rendu.
 * - Apparence : ragoût suspect (item_model) avec l'aspect enchanté ; base : tarte à la citrouille (aliment sans autre
 *   usage vanilla ; un livre de connaissances ne peut pas être mangé). Apparence Bedrock : geyser-bedrock.
 * - Livre de recettes : la recette est débloquée dès que le joueur obtient un des ingrédients.
 *
 * Autres plugins : creerCasseCroute(), estCasseCroute(objet).
 */
public final class KSCasseCroute extends JavaPlugin implements Listener {

    private static final int DUREE = 5 * 60 * 20;

    private static final List<Material> INGREDIENTS = List.of(Material.GOLDEN_APPLE, Material.GOLDEN_CARROT,
            Material.GOLDEN_DANDELION, Material.GLISTERING_MELON_SLICE, Material.SUNFLOWER, Material.BOWL);

    private static NamespacedKey marqueur;
    private NamespacedKey cleRecette;

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "casse_croute");
        cleRecette = new NamespacedKey(this, "casse_croute_dore");
        getServer().getPluginManager().registerEvents(this, this);

        ShapelessRecipe recette = new ShapelessRecipe(cleRecette, creerCasseCroute());
        INGREDIENTS.forEach(recette::addIngredient);
        Bukkit.addRecipe(recette);
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
    }

    @Override
    public void onDisable() {
        Bukkit.removeRecipe(cleRecette);
    }

    // ------------------------------------------------------------------ objet (autres plugins)

    /** Un Casse-croûte doré. Nécessite que le plugin soit activé. */
    public static ItemStack creerCasseCroute() {
        ItemStack item = new ItemStack(Material.PUMPKIN_PIE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Casse-croûte doré", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "suspicious_stew"));
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 16);
        item.setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(7).saturation(5f).canAlwaysEat(true));
        item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(1.6f)
                .animation(ItemUseAnimation.EAT)
                .addEffect(ConsumeEffect.applyStatusEffects(List.of(
                        new PotionEffect(PotionEffectType.ABSORPTION, DUREE, 1),
                        new PotionEffect(PotionEffectType.HASTE, DUREE, 1),
                        new PotionEffect(PotionEffectType.SPEED, DUREE, 0)), 1f)));
        return item;
    }

    /** Vrai si l'objet est un Casse-croûte doré du plugin. */
    public static boolean estCasseCroute(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(marqueur, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------------ livre de recettes

    private void debloquer(Player joueur, ItemStack obtenu) {
        if (obtenu != null && INGREDIENTS.contains(obtenu.getType()) && !joueur.hasDiscoveredRecipe(cleRecette)) {
            joueur.discoverRecipe(cleRecette);
        }
    }

    /** Joueur qui a déjà un ingrédient (obtenu avant l'installation, ou au démarrage du plugin). */
    private void livreDeRecettes(Player joueur) {
        for (ItemStack item : joueur.getInventory().getContents()) {
            debloquer(joueur, item);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        livreDeRecettes(event.getPlayer());
    }

    @EventHandler
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        debloquer(event.getPlayer(), event.getNewItemStack());
    }
}
