package fr.kalium.marteau;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DamageResistant;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * KS_Marteau (LeKiwi06, 20/11/2026, cahier local Marteau.md validé le 20/11) : le marteau de Kixster.
 *
 * - Objet : une vraie masse du jeu (MACE) marquée par le plugin, pour garder le coup écrasé du jeu sans le recoder.
 *   Composants posés par le plugin : outil (règles de la pioche du même niveau, 1 point de solidité par bloc),
 *   solidité maximale (6 fois la pioche : 9 366 en diamant, 12 186 en netherite), dégâts (6, ou 7 en netherite),
 *   résistance au feu et à la lave (netherite), réparation par un noyau lourd seulement (remis à neuf).
 * - Cœur de Marteau : noyau lourd entouré de 8 blocs de diamant ; il ne se pose pas.
 * - Marteau : bloc de diamant, Cœur, bloc de diamant en haut, 2 bâtons en manche (comme une pioche).
 * - Marteau en netherite : table de forge, modèle d'amélioration en netherite + Marteau + bloc de netherite.
 * - Minage en 3 × 3 et cuisson des minerais par Aura de feu : classe Minage. Table d'enchantement : Table. Enclume et
 *   forge : Enclume. Magnétisme : Magnetisme (enregistré au démarrage par Demarrage).
 */
public final class KSMarteau extends JavaPlugin implements Listener {

    /** Les deux versions du marteau (seules versions qui existent, cahier § 3). */
    public enum Niveau {
        DIAMANT("diamant", "Marteau", 1561 * 6, 5.0, 8.0f,
                BlockTypeTagKeys.INCORRECT_FOR_DIAMOND_TOOL, Tag.INCORRECT_FOR_DIAMOND_TOOL),
        NETHERITE("netherite", "Marteau en netherite", 2031 * 6, 6.0, 9.0f,
                BlockTypeTagKeys.INCORRECT_FOR_NETHERITE_TOOL, Tag.INCORRECT_FOR_NETHERITE_TOOL);

        final String id;
        final String nom;
        final int solidite;
        /** Bonus de dégâts de la main (1) : 5 -> 6 au total, comme la masse ; 6 -> 7 en netherite. */
        final double degats;
        final float vitesse;
        final TagKey<BlockType> mauvaisOutil;
        final Tag<Material> mauvaisOutilMateriaux;

        Niveau(String id, String nom, int solidite, double degats, float vitesse, TagKey<BlockType> mauvaisOutil,
               Tag<Material> mauvaisOutilMateriaux) {
            this.id = id;
            this.nom = nom;
            this.solidite = solidite;
            this.degats = degats;
            this.vitesse = vitesse;
            this.mauvaisOutil = mauvaisOutil;
            this.mauvaisOutilMateriaux = mauvaisOutilMateriaux;
        }

        static Niveau deId(String id) {
            for (Niveau n : values()) {
                if (n.id.equals(id)) {
                    return n;
                }
            }
            return null;
        }
    }

    private static NamespacedKey cleMarteau;
    private static NamespacedKey cleCoeur;
    private NamespacedKey recetteCoeur;
    private NamespacedKey recetteMarteau;
    private NamespacedKey recetteNetherite;
    private static Enchantment magnetisme;

    @Override
    public void onEnable() {
        cleMarteau = new NamespacedKey(this, "marteau");
        cleCoeur = new NamespacedKey(this, "coeur_de_marteau");
        recetteCoeur = new NamespacedKey(this, "coeur_de_marteau");
        recetteMarteau = new NamespacedKey(this, "marteau");
        recetteNetherite = new NamespacedKey(this, "marteau_netherite");
        magnetisme = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Demarrage.MAGNETISME);
        if (magnetisme == null) {
            getLogger().severe("Enchantement Magnétisme absent du registre : le plugin doit être chargé comme plugin "
                    + "Paper (paper-plugin.yml) pour l'enregistrer au démarrage.");
        }

        boolean claims = getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Minage(), this);
        getServer().getPluginManager().registerEvents(new Table(), this);
        getServer().getPluginManager().registerEvents(new Enclume(recetteNetherite), this);
        new Magnetisme(this, claims).demarrer();
        ajouterRecettes();
        getLogger().info("Marteau prêt (claims " + (claims ? "SimpleClaimSystem" : "absents") + ").");
    }

    @Override
    public void onDisable() {
        Bukkit.removeRecipe(recetteCoeur);
        Bukkit.removeRecipe(recetteMarteau);
        Bukkit.removeRecipe(recetteNetherite);
    }

    static Enchantment magnetisme() {
        return magnetisme;
    }

    // ------------------------------------------------------------------ recettes

    private void ajouterRecettes() {
        ShapedRecipe coeur = new ShapedRecipe(recetteCoeur, creerCoeur());
        coeur.shape("DDD", "DND", "DDD");
        coeur.setIngredient('D', Material.DIAMOND_BLOCK);
        // Un noyau lourd ordinaire (un Cœur de Marteau, lui aussi un noyau lourd, ne doit pas servir).
        coeur.setIngredient('N', new RecipeChoice.ExactChoice(new ItemStack(Material.HEAVY_CORE)));
        Bukkit.addRecipe(coeur);

        ShapedRecipe marteau = new ShapedRecipe(recetteMarteau, creerMarteau(Niveau.DIAMANT));
        marteau.shape("DCD", " B ", " B ");
        marteau.setIngredient('D', Material.DIAMOND_BLOCK);
        marteau.setIngredient('C', new RecipeChoice.ExactChoice(creerCoeur()));
        marteau.setIngredient('B', Material.STICK);
        Bukkit.addRecipe(marteau);

        // Base : n'importe quelle masse ; la classe Enclume ne donne un résultat que pour un marteau en diamant (usé et
        // enchanté compris, ce qu'une recette exacte refuserait) et calcule elle-même le marteau en netherite.
        SmithingTransformRecipe netherite = new SmithingTransformRecipe(recetteNetherite, creerMarteau(Niveau.NETHERITE),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.MaterialChoice(Material.MACE),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_BLOCK));
        Bukkit.addRecipe(netherite);
    }

    // ------------------------------------------------------------------ objets

    private static Component texte(String s, NamedTextColor couleur) {
        return Component.text(s, couleur).decoration(TextDecoration.ITALIC, false);
    }

    /** Un Cœur de Marteau. */
    public static ItemStack creerCoeur() {
        ItemStack item = new ItemStack(Material.HEAVY_CORE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte("Cœur de Marteau", NamedTextColor.WHITE));
        meta.lore(List.of(texte("Sert à fabriquer le Marteau", NamedTextColor.GRAY)));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(cleCoeur, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean estCoeur(ItemStack item) {
        return item != null && item.getType() == Material.HEAVY_CORE && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleCoeur, PersistentDataType.BYTE);
    }

    /** Un marteau neuf de ce niveau. */
    public static ItemStack creerMarteau(Niveau niveau) {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte(niveau.nom, NamedTextColor.WHITE));
        meta.getPersistentDataContainer().set(cleMarteau, PersistentDataType.STRING, niveau.id);
        item.setItemMeta(meta);
        appliquer(item, niveau);
        return item;
    }

    /** Niveau du marteau, ou null si l'objet n'en est pas un. */
    public static Niveau niveau(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) {
            return null;
        }
        return Niveau.deId(item.getItemMeta().getPersistentDataContainer().get(cleMarteau, PersistentDataType.STRING));
    }

    /** Marteau en diamant -> marteau en netherite : enchantements, nom donné et usure gardés, comme la forge du jeu. */
    static ItemStack ameliorer(ItemStack marteau) {
        ItemStack item = marteau.clone();
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        Component nom = meta.displayName();
        if (nom == null || nom.equals(texte(Niveau.DIAMANT.nom, NamedTextColor.WHITE))) {
            meta.displayName(texte(Niveau.NETHERITE.nom, NamedTextColor.WHITE));
        }
        meta.getPersistentDataContainer().set(cleMarteau, PersistentDataType.STRING, Niveau.NETHERITE.id);
        item.setItemMeta(meta);
        appliquer(item, Niveau.NETHERITE);
        return item;
    }

    /** Composants du niveau : outil, solidité, dégâts, feu, description. */
    private static void appliquer(ItemStack item, Niveau niveau) {
        Registry<BlockType> blocs = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK);
        item.setData(DataComponentTypes.TOOL, Tool.tool()
                .defaultMiningSpeed(1.0f)
                .damagePerBlock(1)
                .canDestroyBlocksInCreative(true)
                // Comme une pioche du jeu : d'abord les blocs trop durs pour ce niveau (pas de butin), puis la pioche.
                .addRule(Tool.rule(blocs.getTag(niveau.mauvaisOutil), null, TriState.FALSE))
                .addRule(Tool.rule(blocs.getTag(BlockTypeTagKeys.MINEABLE_PICKAXE), niveau.vitesse, TriState.TRUE))
                .build());
        item.setData(DataComponentTypes.MAX_DAMAGE, niveau.solidite);
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes()
                .addModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(NamespacedKey.minecraft("base_attack_damage"),
                        niveau.degats, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                .addModifier(Attribute.ATTACK_SPEED, new AttributeModifier(NamespacedKey.minecraft("base_attack_speed"),
                        -3.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                .build());
        // La masse se répare avec une baguette de breeze : pas le marteau. Il se répare par un noyau lourd (remis à neuf,
        // classe Enclume), un autre marteau du même niveau et Raccommodage (réponse de LeKiwi06 du 20/11).
        item.unsetData(DataComponentTypes.REPAIRABLE);
        if (niveau == Niveau.NETHERITE) {
            item.setData(DataComponentTypes.DAMAGE_RESISTANT, DamageResistant.damageResistant(
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).getTag(DamageTypeTagKeys.IS_FIRE)));
        }
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(texte("Mine en 3 × 3 ce qu'une pioche mine", NamedTextColor.GRAY));
        lore.add(texte("Accroupi : un seul bloc", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    // ------------------------------------------------------------------ Cœur

    /** Le Cœur de Marteau est un noyau lourd : posé, il redeviendrait un noyau lourd ordinaire. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPose(BlockPlaceEvent event) {
        if (estCoeur(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }
}
