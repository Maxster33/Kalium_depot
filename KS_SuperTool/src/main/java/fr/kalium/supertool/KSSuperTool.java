package fr.kalium.supertool;

import fr.kalium.supertool.Outil.Machine;
import fr.kalium.supertool.Outil.Niveau;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DamageResistant;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
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
 * KS_SuperTool (LeKiwi06, 20/11/2026, cahiers locaux Marteau.md et Outils_3x3.md, validés le 20/11) : les machines de
 * Kixster, outils qui travaillent en 3 × 3. Ancien nom KS_Marteau (jamais déployé).
 *
 * - Foreuse (ex-marteau) : une vraie masse du jeu (coup écrasé du jeu) avec les règles de minage de la pioche ;
 *   Abatteuse, Excavatrice, Laboureuse : la vraie hache, pelle, houe du jeu (vitesse et dégâts du jeu, Bedrock compris).
 * - Deux niveaux, diamant et netherite (table de forge : modèle + bloc de netherite) ; solidité 6 fois l'outil du jeu
 *   (9 366 / 12 186), 1 point par bloc ; réparation par un noyau lourd (remis à neuf), un autre outil identique ou
 *   Raccommodage, jamais par matériau.
 * - Cœur de machine : noyau lourd entouré de 8 blocs de diamant ; il ne se pose pas. Dans chaque craft, il est juste
 *   au-dessus des deux bâtons, des blocs de diamant complètent la forme de l'outil du jeu.
 * - Classes : Minage (3 × 3 en cassant, récolte, Aura de feu), Clic (3 × 3 au clic droit), Table (table
 *   d'enchantement), Enclume (enclume, forge, protections), Magnetisme (enchantement enregistré par Demarrage).
 */
public final class KSSuperTool extends JavaPlugin implements Listener {

    private static NamespacedKey cleOutil;
    private static NamespacedKey cleCoeur;
    private static Enchantment magnetisme;
    private final List<NamespacedKey> recettes = new ArrayList<>();
    private NamespacedKey recetteNetherite;

    @Override
    public void onEnable() {
        cleOutil = new NamespacedKey(this, "outil");
        cleCoeur = new NamespacedKey(this, "coeur_de_machine");
        recetteNetherite = new NamespacedKey(this, "netherite");
        magnetisme = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Demarrage.MAGNETISME);
        if (magnetisme == null) {
            getLogger().severe("Enchantement Magnétisme absent du registre : le plugin doit être chargé comme plugin "
                    + "Paper (paper-plugin.yml) pour l'enregistrer au démarrage.");
        }
        boolean claims = getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem");
        getServer().getPluginManager().registerEvents(this, this);
        boolean cultures = culturesDisponibles();
        getServer().getPluginManager().registerEvents(new Minage(this, cultures), this);
        getServer().getPluginManager().registerEvents(new Clic(claims), this);
        getServer().getPluginManager().registerEvents(new Table(), this);
        getServer().getPluginManager().registerEvents(new Enclume(recetteNetherite), this);
        new Magnetisme(this, claims).demarrer();
        ajouterRecettes();
        getLogger().info("Machines prêtes (claims " + (claims ? "SimpleClaimSystem" : "absents") + ", cultures "
                + (cultures ? "KS_Cultures" : "du jeu seulement") + ").");
    }

    /** KS_Cultures 1.1.0 ou plus (API de récolte) est activé. */
    private boolean culturesDisponibles() {
        if (!getServer().getPluginManager().isPluginEnabled("KS_Cultures")) {
            return false;
        }
        try {
            Class.forName("fr.kalium.cultures.KSCultures").getMethod("recolter", org.bukkit.entity.Player.class,
                    org.bukkit.block.Block.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            getLogger().warning("KS_Cultures trop ancien (1.1.0 requise) : la Laboureuse ne récolte que les cultures "
                    + "du jeu.");
            return false;
        }
    }

    @Override
    public void onDisable() {
        recettes.forEach(Bukkit::removeRecipe);
    }

    static Enchantment magnetisme() {
        return magnetisme;
    }

    // ------------------------------------------------------------------ recettes

    private void ajouterRecettes() {
        ShapedRecipe coeur = new ShapedRecipe(new NamespacedKey(this, "coeur_de_machine"), creerCoeur());
        coeur.shape("DDD", "DND", "DDD");
        coeur.setIngredient('D', Material.DIAMOND_BLOCK);
        // Un noyau lourd ordinaire (un Cœur de machine, lui aussi un noyau lourd, ne doit pas servir).
        coeur.setIngredient('N', new RecipeChoice.ExactChoice(new ItemStack(Material.HEAVY_CORE)));
        ajouter(coeur);

        // Le Cœur juste au-dessus des deux bâtons ; blocs de diamant pour compléter la forme de l'outil du jeu.
        ajouter(machine(Machine.FOREUSE, "DCD", " B ", " B "));
        ajouter(machine(Machine.ABATTEUSE, "DC", "DB", " B"));
        ajouter(machine(Machine.EXCAVATRICE, "C", "B", "B"));
        ajouter(machine(Machine.LABOUREUSE, "DC", " B", " B"));

        // Base : n'importe quelle masse, hache, pelle ou houe en diamant ; la classe Enclume ne donne un résultat que
        // pour une machine en diamant (usée et enchantée comprise, ce qu'une recette exacte refuserait).
        ajouter(new SmithingTransformRecipe(recetteNetherite, creer(new Outil(Machine.FOREUSE, Niveau.NETHERITE)),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.MaterialChoice(Material.MACE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL,
                        Material.DIAMOND_HOE),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_BLOCK)));
    }

    private ShapedRecipe machine(Machine m, String... forme) {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(this, m.id), creer(new Outil(m, Niveau.DIAMANT)));
        r.shape(forme);
        if (String.join("", forme).indexOf('D') >= 0) {
            r.setIngredient('D', Material.DIAMOND_BLOCK);
        }
        r.setIngredient('C', new RecipeChoice.ExactChoice(creerCoeur()));
        r.setIngredient('B', Material.STICK);
        return r;
    }

    private void ajouter(org.bukkit.inventory.Recipe r) {
        Bukkit.addRecipe(r);
        recettes.add(((org.bukkit.Keyed) r).getKey());
    }

    // ------------------------------------------------------------------ objets

    private static Component texte(String s, NamedTextColor couleur) {
        return Component.text(s, couleur).decoration(TextDecoration.ITALIC, false);
    }

    /** Un Cœur de machine. */
    public static ItemStack creerCoeur() {
        ItemStack item = new ItemStack(Material.HEAVY_CORE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte("Cœur de machine", NamedTextColor.WHITE));
        meta.lore(List.of(texte("Sert à fabriquer les machines", NamedTextColor.GRAY)));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(cleCoeur, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean estCoeur(ItemStack item) {
        return item != null && item.getType() == Material.HEAVY_CORE && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleCoeur, PersistentDataType.BYTE);
    }

    /**
     * Objet par son identifiant (KS_KaliumGive, récompenses) : coeur_de_machine, foreuse, foreuse_netherite,
     * abatteuse... ; null si inconnu. Appelé par réflexion (plugin Paper : ses classes ne sont pas visibles des plugins
     * Bukkit).
     */
    public static ItemStack creer(String id) {
        if ("coeur_de_machine".equals(id)) {
            return creerCoeur();
        }
        Outil o = Outil.deId(id);
        return o == null ? null : creer(o);
    }

    /** Identifiants de creer(String), pour KS_KaliumGive. */
    public static List<String> ids() {
        List<String> l = new ArrayList<>(List.of("coeur_de_machine"));
        for (Machine m : Machine.values()) {
            for (Niveau n : Niveau.values()) {
                l.add(new Outil(m, n).id());
            }
        }
        return l;
    }

    /** Une machine neuve. */
    static ItemStack creer(Outil outil) {
        ItemStack item = new ItemStack(outil.base());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte(outil.nom(), NamedTextColor.WHITE));
        meta.getPersistentDataContainer().set(cleOutil, PersistentDataType.STRING, outil.id());
        item.setItemMeta(meta);
        appliquer(item, outil);
        return item;
    }

    /** La machine que cet objet est, ou null. */
    static Outil outil(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        Outil o = Outil.deId(item.getItemMeta().getPersistentDataContainer().get(cleOutil, PersistentDataType.STRING));
        return o != null && o.base() == item.getType() ? o : null;
    }

    /** Machine en diamant -> en netherite : enchantements, nom donné et usure gardés, comme la forge du jeu. */
    static ItemStack ameliorer(ItemStack objet, Outil diamant) {
        Outil netherite = new Outil(diamant.machine(), Niveau.NETHERITE);
        ItemStack item = objet.withType(netherite.base());
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        Component nom = meta.displayName();
        if (nom == null || nom.equals(texte(diamant.nom(), NamedTextColor.WHITE))) {
            meta.displayName(texte(netherite.nom(), NamedTextColor.WHITE));
        }
        meta.getPersistentDataContainer().set(cleOutil, PersistentDataType.STRING, netherite.id());
        item.setItemMeta(meta);
        appliquer(item, netherite);
        return item;
    }

    /** Composants : solidité, pas de réparation par matériau ; Foreuse : outil, dégâts, feu. Description. */
    private static void appliquer(ItemStack item, Outil outil) {
        Niveau n = outil.niveau();
        item.setData(DataComponentTypes.MAX_DAMAGE, n.solidite);
        // Ni baguette de breeze (masse), ni diamant, ni lingot de netherite : un noyau lourd à l'enclume (classe Enclume).
        item.unsetData(DataComponentTypes.REPAIRABLE);
        if (outil.machine() == Machine.FOREUSE) {
            Registry<BlockType> blocs = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK);
            item.setData(DataComponentTypes.TOOL, Tool.tool()
                    .defaultMiningSpeed(1.0f)
                    .damagePerBlock(1)
                    .canDestroyBlocksInCreative(true)
                    // Comme une pioche du jeu : d'abord les blocs trop durs pour ce niveau (pas de butin), puis la pioche.
                    .addRule(Tool.rule(blocs.getTag(n.mauvaisOutil), null, TriState.FALSE))
                    .addRule(Tool.rule(blocs.getTag(BlockTypeTagKeys.MINEABLE_PICKAXE), n.vitesseForeuse, TriState.TRUE))
                    .build());
            item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes()
                    .addModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                            NamespacedKey.minecraft("base_attack_damage"), n.degatsForeuse,
                            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                    .addModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                            NamespacedKey.minecraft("base_attack_speed"), -3.4,
                            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                    .build());
            if (n == Niveau.NETHERITE) {
                item.setData(DataComponentTypes.DAMAGE_RESISTANT, DamageResistant.damageResistant(
                        RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE)
                                .getTag(DamageTypeTagKeys.IS_FIRE)));
            }
        }
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(texte(outil.machine().usage, NamedTextColor.GRAY));
        if (outil.machine().clicDroit != null) {
            lore.add(texte(outil.machine().clicDroit, NamedTextColor.GRAY));
        }
        lore.add(texte("Accroupi : un seul bloc", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    // ------------------------------------------------------------------ Cœur

    /** Le Cœur de machine est un noyau lourd : posé, il redeviendrait un noyau lourd ordinaire. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPose(BlockPlaceEvent event) {
        if (estCoeur(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }
}
