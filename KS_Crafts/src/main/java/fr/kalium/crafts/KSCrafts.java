package fr.kalium.crafts;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.potion.PotionMix;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KS_Crafts - crafts du serveur Event (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md, Maxster33, 25/09/2026).
 * Les crafts « 8 + 1 » sont en anneau autour de l'objet central, les autres sans forme (réponse de Maxster33).
 * Contient aussi le remplacement de la verrue du Nether par le bloc de verrue (briques rouges du Nether, potion
 * étrange à l'alambic).
 * 1.4.0 : le Bedrock Breaker (objet et utilisation) est dans KS_BedrockBreaker ; seule sa recette reste ici.
 * 1.5.0 : nouvelle recette du Bedrock Breaker (voir onEnable).
 */
public final class KSCrafts extends JavaPlugin implements Listener {

    private final List<NamespacedKey> added = new ArrayList<>();

    /** Ingrédients de la Clé de l'End (craft sans forme, un de chaque). */
    private static final List<Material> CLE_INGREDIENTS = List.of(
            Material.HEART_OF_THE_SEA, Material.WITHER_SKELETON_SKULL, Material.TOTEM_OF_UNDYING,
            Material.ENCHANTED_GOLDEN_APPLE, Material.CALIBRATED_SCULK_SENSOR, Material.SPONGE,
            Material.NETHERITE_INGOT, Material.BELL, Material.CREAKING_HEART);

    /** Ingrédients du Bedrock Breaker (1.5.0 ; en 1.4.0 : TNT et houe en diamant). */
    private static final List<Material> BREAKER_INGREDIENTS = List.of(Material.BLAZE_POWDER, Material.FIRE_CHARGE,
            Material.TNT_MINECART, Material.END_CRYSTAL, Material.RESPAWN_ANCHOR);

    /** Recettes du livre de recettes débloquées à l'obtention d'un de leurs ingrédients (id -> ingrédients). */
    private static final Map<String, List<Material>> LIVRE = Map.of(
            "cle_de_l_end", CLE_INGREDIENTS,
            "bedrock_breaker", BREAKER_INGREDIENTS);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        ring("red_sand", Material.SAND, new RecipeChoice.MaterialChoice(Material.ORANGE_DYE), new ItemStack(Material.RED_SAND, 8));
        shapeless("sand_from_sandstone", new ItemStack(Material.SAND, 4), new RecipeChoice.MaterialChoice(
                Material.SANDSTONE, Material.CHISELED_SANDSTONE, Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE));
        shapeless("red_sand_from_red_sandstone", new ItemStack(Material.RED_SAND, 4), new RecipeChoice.MaterialChoice(
                Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE, Material.CUT_RED_SANDSTONE, Material.SMOOTH_RED_SANDSTONE));
        shapeless("raw_iron_block", new ItemStack(Material.RAW_IRON_BLOCK, 4), repeat(new RecipeChoice.MaterialChoice(Material.IRON_BLOCK), 4));
        shapeless("raw_gold_block", new ItemStack(Material.RAW_GOLD_BLOCK, 4), repeat(new RecipeChoice.MaterialChoice(Material.GOLD_BLOCK), 4));
        shapeless("raw_copper_block", new ItemStack(Material.RAW_COPPER_BLOCK, 4), repeat(new RecipeChoice.MaterialChoice(
                Material.COPPER_BLOCK, Material.EXPOSED_COPPER, Material.WEATHERED_COPPER, Material.OXIDIZED_COPPER,
                Material.WAXED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER, Material.WAXED_WEATHERED_COPPER,
                Material.WAXED_OXIDIZED_COPPER), 4));
        shapeless("calcite", new ItemStack(Material.CALCITE, 9), concat(
                repeat(new RecipeChoice.MaterialChoice(Material.DIORITE), 5),
                repeat(new RecipeChoice.MaterialChoice(Material.QUARTZ_BLOCK), 4)));
        ring("crying_obsidian", Material.OBSIDIAN, new RecipeChoice.MaterialChoice(Material.GHAST_TEAR), new ItemStack(Material.CRYING_OBSIDIAN, 8));
        shapeless("reinforced_deepslate", new ItemStack(Material.REINFORCED_DEEPSLATE, 1), concat(
                repeat(new RecipeChoice.MaterialChoice(Material.BONE_BLOCK), 2),
                repeat(new RecipeChoice.MaterialChoice(Material.DEEPSLATE), 2)));
        shapeless("pitcher_pod", new ItemStack(Material.PITCHER_POD, 4), repeat(new RecipeChoice.MaterialChoice(Material.PITCHER_PLANT), 2));
        shapeless("torchflower_seeds", new ItemStack(Material.TORCHFLOWER_SEEDS, 4), repeat(new RecipeChoice.MaterialChoice(Material.TORCHFLOWER), 2));
        coral("tube", Material.TUBE_CORAL, Material.TUBE_CORAL_BLOCK);
        coral("brain", Material.BRAIN_CORAL, Material.BRAIN_CORAL_BLOCK);
        coral("bubble", Material.BUBBLE_CORAL, Material.BUBBLE_CORAL_BLOCK);
        coral("fire", Material.FIRE_CORAL, Material.FIRE_CORAL_BLOCK);
        coral("horn", Material.HORN_CORAL, Material.HORN_CORAL_BLOCK);
        ItemStack light = parse("minecraft:light[minecraft:block_state={level:\"15\"}]");
        if (light != null) {
            light.setAmount(4);
            shapeless("light_15", light, concat(
                    repeat(new RecipeChoice.MaterialChoice(Material.GLASS), 4),
                    repeat(new RecipeChoice.MaterialChoice(Material.GLOWSTONE), 4),
                    List.of(new RecipeChoice.MaterialChoice(Material.BLAZE_ROD))));
        }
        ItemStack frame = parse("minecraft:item_frame[minecraft:entity_data={id:\"minecraft:item_frame\",Invisible:1b}]");
        if (frame != null) {
            ring("invisible_item_frame", Material.STICK, new RecipeChoice.MaterialChoice(Material.PHANTOM_MEMBRANE), frame);
        }
        ring("verdant_froglight", Material.MAGMA_BLOCK, new RecipeChoice.MaterialChoice(Material.GREEN_DYE), new ItemStack(Material.VERDANT_FROGLIGHT, 4));
        ring("ochre_froglight", Material.MAGMA_BLOCK, new RecipeChoice.MaterialChoice(Material.ORANGE_DYE), new ItemStack(Material.OCHRE_FROGLIGHT, 4));
        ring("pearlescent_froglight", Material.MAGMA_BLOCK, new RecipeChoice.MaterialChoice(Material.PURPLE_DYE), new ItemStack(Material.PEARLESCENT_FROGLIGHT, 4));

        // Clé de l'End (1.1.0) : objet de KS_EC_Extension (softdepend), sans forme.
        if (getServer().getPluginManager().isPluginEnabled("KS_EC_Extension")) {
            shapeless("cle_de_l_end", fr.kalium.ecextension.KSECExtension.creerCle(),
                    CLE_INGREDIENTS.stream().map(m -> (RecipeChoice) new RecipeChoice.MaterialChoice(m)).toList());
        } else {
            getLogger().warning("KS_EC_Extension absent : craft de la Clé de l'End ignoré.");
        }

        // Bedrock Breaker : objet de KS_BedrockBreaker (softdepend). 1.5.0 (Maxster33) : poudre de blaze aux 4 coins,
        // charge de feu en haut, wagonnets à TNT à gauche et à droite, cristal de l'End au centre, ancre de
        // réapparition en bas (1.4.0 : 8 TNT autour d'une houe en diamant).
        if (getServer().getPluginManager().isPluginEnabled("KS_BedrockBreaker")) {
            ShapedRecipe breaker = new ShapedRecipe(key("bedrock_breaker"),
                    fr.kalium.bedrockbreaker.KSBedrockBreaker.creerBreaker());
            breaker.shape("PFP", "WCW", "PAP");
            breaker.setIngredient('P', Material.BLAZE_POWDER);
            breaker.setIngredient('F', Material.FIRE_CHARGE);
            breaker.setIngredient('W', Material.TNT_MINECART);
            breaker.setIngredient('C', Material.END_CRYSTAL);
            breaker.setIngredient('A', Material.RESPAWN_ANCHOR);
            add(breaker);
        } else {
            getLogger().warning("KS_BedrockBreaker absent : craft du Bedrock Breaker ignoré.");
        }

        // 1.6.0 (Maxster33) : spawners (KS_Spawners) et Changeur de Biome (KS_BiomeChanger), avec les objets de
        // KS_ItemSimple ; sans forme (seuls les crafts « 8 + 1 » sont en anneau). Les têtes d'araignée, de blaze, de
        // mouton, de vache et de poule sont les têtes « Steve » nommées de KS_ItemSimple.
        if (actif("KS_ItemSimple") && actif("KS_Spawners")) {
            spawner("zombi", new RecipeChoice.MaterialChoice(Material.ZOMBIE_HEAD));
            spawner("squelette", new RecipeChoice.MaterialChoice(Material.SKELETON_SKULL));
            spawner("creeper", new RecipeChoice.MaterialChoice(Material.CREEPER_HEAD));
            for (String animal : fr.kalium.itemsimple.KSItemSimple.TETES.keySet()) {
                spawner(animal, new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerTete(animal)));
            }
        } else {
            getLogger().warning("KS_ItemSimple ou KS_Spawners absent : crafts des spawners ignorés.");
        }
        if (actif("KS_ItemSimple") && actif("KS_BiomeChanger")) {
            shapeless("changeur_biome", fr.kalium.biomechanger.KSBiomeChanger.creerChangeur(), concat(
                    repeat(new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerFragmentSpawner()), 4),
                    List.of(new RecipeChoice.MaterialChoice(Material.MOSS_BLOCK),
                            new RecipeChoice.MaterialChoice(Material.POWDER_SNOW_BUCKET),
                            new RecipeChoice.MaterialChoice(Material.OPEN_EYEBLOSSOM),
                            new RecipeChoice.MaterialChoice(Material.SNIFFER_EGG),
                            new RecipeChoice.MaterialChoice(Material.BROWN_MUSHROOM))));
        } else {
            getLogger().warning("KS_ItemSimple ou KS_BiomeChanger absent : craft du Changeur de Biome ignoré.");
        }

        // Verrue du Nether : remplacee par le bloc de verrue (briques rouges), craft 9 verrues -> bloc retire.
        Bukkit.removeRecipe(NamespacedKey.minecraft("red_nether_bricks"));
        Bukkit.removeRecipe(NamespacedKey.minecraft("nether_wart_block"));
        ShapedRecipe bricks = new ShapedRecipe(key("red_nether_bricks"), new ItemStack(Material.RED_NETHER_BRICKS));
        bricks.shape("WB", "BW");
        bricks.setIngredient('W', Material.NETHER_WART_BLOCK);
        bricks.setIngredient('B', Material.NETHER_BRICK);
        add(bricks);

        // Alambic : bouteille d'eau + bloc de verrue -> potion etrange.
        ItemStack awkward = new ItemStack(Material.POTION);
        PotionMeta awkwardMeta = (PotionMeta) awkward.getItemMeta();
        awkwardMeta.setBasePotionType(PotionType.AWKWARD);
        awkward.setItemMeta(awkwardMeta);
        Bukkit.getPotionBrewer().addPotionMix(new PotionMix(key("awkward_from_nether_wart_block"), awkward,
                PotionMix.createPredicateChoice(item -> item != null && item.getType() == Material.POTION
                        && item.getItemMeta() instanceof PotionMeta meta && meta.getBasePotionType() == PotionType.WATER),
                new RecipeChoice.MaterialChoice(Material.NETHER_WART_BLOCK)));

        getLogger().info(added.size() + " crafts ajoutés.");
        // Joueurs déjà connectés (rechargement du plugin) qui ont un ingrédient d'une recette du livre.
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
    }

    @Override
    public void onDisable() {
        added.forEach(Bukkit::removeRecipe);
        Bukkit.getPotionBrewer().removePotionMix(key("awkward_from_nether_wart_block"));
    }

    // ------------------------------------------------------------------ livre de recettes

    /**
     * Livre de recettes : comme une recette vanilla, une recette de LIVRE est débloquée quand le joueur obtient l'un de
     * ses ingrédients (objet qui arrive dans son inventaire). Une recette de plugin n'apparaît dans le livre que si
     * elle est débloquée pour le joueur ; une fois débloquée, elle le reste.
     * Clé de l'End depuis 1.3.0 ; Bedrock Breaker depuis 1.4.0 (ses nouveaux ingrédients depuis 1.5.0).
     * 1.6.0 (Maxster33) : un Fragment de Spawner débloque les recettes des spawners et du Changeur de Biome (le fragment
     * est reconnu à son marqueur : c'est un livre de connaissances, comme d'autres objets custom).
     */
    private void debloquer(Player player, ItemStack obtenu) {
        Material type = obtenu.getType();
        LIVRE.forEach((id, ingredients) -> {
            if (ingredients.contains(type)) {
                decouvrir(player, key(id));
            }
        });
        if (type == Material.KNOWLEDGE_BOOK && actif("KS_ItemSimple")
                && "fragment_spawner".equals(fr.kalium.itemsimple.KSItemSimple.idObjet(obtenu))) {
            for (NamespacedKey recette : added) {
                if (recette.getKey().startsWith("spawner_") || recette.getKey().equals("changeur_biome")) {
                    decouvrir(player, recette);
                }
            }
        }
    }

    private void decouvrir(Player player, NamespacedKey recette) {
        if (added.contains(recette) && !player.hasDiscoveredRecipe(recette)) {
            player.discoverRecipe(recette);
        }
    }

    /** Joueur qui a déjà un ingrédient (obtenu avant cette version, ou au démarrage du plugin). */
    private void livreDeRecettes(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                debloquer(player, item);
            }
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

    // ------------------------------------------------------------------ recettes

    /** Objet décrit au format des commandes de Minecraft ; null (recette ignorée, avertissement) si refusé. */
    private ItemStack parse(String description) {
        try {
            return Bukkit.getItemFactory().createItemStack(description);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Objet refusé par le serveur, recette ignorée : " + description + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(this, id);
    }

    private boolean actif(String plugin) {
        return getServer().getPluginManager().isPluginEnabled(plugin);
    }

    /** 1.6.0 : 7 Fragments de Spawner + 1 Cœur de Spawner + la tête -> spawner (id : zombi, squelette...). */
    private void spawner(String id, RecipeChoice tete) {
        shapeless("spawner_" + id, fr.kalium.spawners.KSSpawners.creerSpawner(id), concat(
                repeat(new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerFragmentSpawner()), 7),
                List.of(new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerCoeurSpawner()), tete)));
    }

    private void add(org.bukkit.inventory.Recipe recipe) {
        Bukkit.addRecipe(recipe);
        added.add(((org.bukkit.Keyed) recipe).getKey());
    }

    /** 8 objets en anneau autour de l'objet central. */
    private void ring(String id, Material around, RecipeChoice center, ItemStack result) {
        ShapedRecipe recipe = new ShapedRecipe(key(id), result);
        recipe.shape("AAA", "ACA", "AAA");
        recipe.setIngredient('A', around);
        recipe.setIngredient('C', center);
        add(recipe);
    }

    private void shapeless(String id, ItemStack result, RecipeChoice choice) {
        shapeless(id, result, List.of(choice));
    }

    private void shapeless(String id, ItemStack result, List<RecipeChoice> choices) {
        ShapelessRecipe recipe = new ShapelessRecipe(key(id), result);
        choices.forEach(recipe::addIngredient);
        add(recipe);
    }

    private void coral(String id, Material coral, Material block) {
        shapeless(id + "_coral_block", new ItemStack(block), repeat(new RecipeChoice.MaterialChoice(coral), 4));
    }

    private static List<RecipeChoice> repeat(RecipeChoice choice, int count) {
        List<RecipeChoice> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(choice);
        }
        return list;
    }

    @SafeVarargs
    private static List<RecipeChoice> concat(List<RecipeChoice>... parts) {
        List<RecipeChoice> list = new ArrayList<>();
        for (List<RecipeChoice> part : parts) {
            list.addAll(part);
        }
        return list;
    }
}
