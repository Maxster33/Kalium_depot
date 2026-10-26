package fr.kalium.crafts;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KS_Crafts - crafts du serveur Event (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md, Maxster33, 25/09/2026).
 * Les crafts « 8 + 1 » sont en anneau autour de l'objet central, les autres sans forme (réponse de Maxster33).
 * Contient aussi le remplacement de la verrue du Nether par le bloc de verrue dans les briques rouges du Nether.
 * 1.4.0 : le Bedrock Breaker (objet et utilisation) est dans KS_BedrockBreaker ; seule sa recette reste ici.
 * 1.5.0 : nouvelle recette du Bedrock Breaker (voir onEnable).
 * 1.7.0 : les recettes des spawners prennent les têtes de KS_Decapitator (voir onEnable).
 * 1.10.0 (LeKiwi06) : plus de brassage au bloc de verrue (« une erreur ») et verrue du Nether refusée à l'alambic :
 * les potions ne viennent plus que du butin et de /rewards ; boîte de shulker fabriquée avec un coffre de l'Ender.
 * 1.12.0 (LeKiwi06) : une pièce d'armure fondue au four donne un lingot, plus une pépite (voir fonteDesArmures).
 * 1.13.0 (LeKiwi06) : recette du bateau à jukebox de KS_JukeboxBoat (voir onEnable).
 * 1.14.0 (LeKiwi06) : recette du bateau à coffre et jukebox de KS_JukeboxBoat 1.1.0 (voir onEnable).
 * 1.14.1 : cette recette n'est plus en double (la liste des bateaux du jeu contient aussi les bateaux à coffre).
 * 1.15.0 (LeKiwi06) : recette du verrou de véhicule de KS_VerrouVehicule (voir onEnable).
 * 1.16.0 (LeKiwi06) : recette de la locomotive de KS_Trains (voir onEnable).
 * 1.17.0 (LeKiwi06) : recette du wagon contrôleur de KS_Trains (voir onEnable et onPreparerControleur).
 * 1.18.0 (LeKiwi06) : recette du wagon jukebox de KS_Trains (voir onEnable).
 * 1.20.0 (LeKiwi06) : 9 os donnent 3 blocs d'os sans passer par la poudre d'os (voir onEnable).
 * 1.20.2 (LeKiwi06) : le livre de recettes montre le wagon contrôleur avec ses 5 pièces différentes (voir onEnable).
 * 1.21.1 (LeKiwi06) : recette des blocs de cuivre en blocs de cuivre brut retirée (voir onEnable).
 */
public final class KSCrafts extends JavaPlugin implements Listener {

    private final List<NamespacedKey> added = new ArrayList<>();
    /** 1.20.2 : recettes valables mais retirées du livre de recettes (voir le wagon contrôleur). */
    private final java.util.Set<NamespacedKey> cachees = new java.util.HashSet<>();

    /** Ingrédients de la Clé de l'End (craft sans forme, un de chaque). */
    private static final List<Material> CLE_INGREDIENTS = List.of(
            Material.HEART_OF_THE_SEA, Material.WITHER_SKELETON_SKULL, Material.TOTEM_OF_UNDYING,
            Material.ENCHANTED_GOLDEN_APPLE, Material.CALIBRATED_SCULK_SENSOR, Material.SPONGE,
            Material.NETHERITE_INGOT, Material.BELL, Material.CREAKING_HEART);

    /**
     * 1.9.0 (LeKiwi06 : « ajoute les recettes de tous les crafts custom qui n'en ont pas encore, elles se débloquent quand
     * on récupère au moins un des éléments du craft ») : chaque recette ajoutée -> ses ingrédients (avant : seulement la
     * Clé de l'End, le Bedrock Breaker, les spawners et le Changeur de Biome).
     */
    private final Map<NamespacedKey, List<RecipeChoice>> ingredients = new java.util.LinkedHashMap<>();

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
        // 1.21.1 (LeKiwi06) : plus de recette « 4 blocs de cuivre (toutes les versions) -> 4 blocs de cuivre brut ».
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
        // 1.20.0 (LeKiwi06) : 9 os -> 3 blocs d'os, sans passer par la poudre d'os (même quantité qu'en vanilla).
        shapeless("bone_block_from_bones", new ItemStack(Material.BONE_BLOCK, 3), repeat(new RecipeChoice.MaterialChoice(Material.BONE), 9));

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
        // KS_ItemSimple ; sans forme (seuls les crafts « 8 + 1 » sont en anneau).
        // 1.7.0 (LeKiwi06, catégorie 1 « Contenu survie ») : les têtes « Steve » de KS_ItemSimple sont remplacées par
        // les têtes de KS_Decapitator, toutes variantes, états et bébés du mob acceptés ; zombie, squelette et creeper :
        // tête vanilla (recette d'avant) ou tête de KS_Decapitator (2e recette « _tete »).
        if (actif("KS_ItemSimple") && actif("KS_Spawners")) {
            spawner("zombi", new RecipeChoice.MaterialChoice(Material.ZOMBIE_HEAD));
            spawner("squelette", new RecipeChoice.MaterialChoice(Material.SKELETON_SKULL));
            spawner("creeper", new RecipeChoice.MaterialChoice(Material.CREEPER_HEAD));
            if (actif("KS_Decapitator")) {
                spawnerTete("zombi_tete", "zombi", EntityType.ZOMBIE);
                spawnerTete("squelette_tete", "squelette", EntityType.SKELETON);
                spawnerTete("creeper_tete", "creeper", EntityType.CREEPER);
                spawnerTete("araignee", "araignee", EntityType.SPIDER);
                spawnerTete("blaze", "blaze", EntityType.BLAZE);
                spawnerTete("mouton", "mouton", EntityType.SHEEP);
                spawnerTete("vache", "vache", EntityType.COW);
                spawnerTete("poule", "poule", EntityType.CHICKEN);
            } else {
                getLogger().warning("KS_Decapitator absent : crafts des spawners avec une tête de mob ignorés.");
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

        // 1.13.0 (LeKiwi06) : bateau à jukebox (KS_JukeboxBoat), sans forme : un bateau ou un radeau sans coffre, de
        // n'importe quel bois, + un jukebox ; le bateau à jukebox est du même bois.
        if (actif("KS_JukeboxBoat")) {
            for (Material bateau : org.bukkit.Tag.ITEMS_BOATS.getValues()) {
                // 1.14.1 : la liste des bateaux du jeu contient aussi les bateaux à coffre (recette plus bas).
                if (org.bukkit.Tag.ITEMS_CHEST_BOATS.isTagged(bateau)) {
                    continue;
                }
                shapeless("bateau_jukebox_" + bateau.getKey().getKey(),
                        fr.kalium.jukeboxboat.KSJukeboxBoat.creerBateau(bateau),
                        List.of(new RecipeChoice.MaterialChoice(bateau), new RecipeChoice.MaterialChoice(Material.JUKEBOX)));
            }
            // 1.14.0 (LeKiwi06) : bateau à coffre et jukebox (KS_JukeboxBoat 1.1.0) : un bateau à coffre + un jukebox.
            for (Material bateau : org.bukkit.Tag.ITEMS_CHEST_BOATS.getValues()) {
                shapeless("bateau_coffre_jukebox_" + bateau.getKey().getKey(),
                        fr.kalium.jukeboxboat.KSJukeboxBoat.creerBateau(bateau),
                        List.of(new RecipeChoice.MaterialChoice(bateau), new RecipeChoice.MaterialChoice(Material.JUKEBOX)));
            }
        } else {
            getLogger().warning("KS_JukeboxBoat absent : craft du bateau à jukebox ignoré.");
        }

        // 1.15.0 (LeKiwi06) : verrou de véhicule (KS_VerrouVehicule) : un crochet au centre, 4 lingots de fer en croix.
        if (actif("KS_VerrouVehicule")) {
            ShapedRecipe verrou = new ShapedRecipe(key("verrou_vehicule"),
                    fr.kalium.verrouvehicule.KSVerrouVehicule.creerVerrou());
            verrou.shape(" F ", "FCF", " F ");
            verrou.setIngredient('F', Material.IRON_INGOT);
            verrou.setIngredient('C', Material.TRIPWIRE_HOOK);
            add(verrou);
        } else {
            getLogger().warning("KS_VerrouVehicule absent : craft du verrou de véhicule ignoré.");
        }

        // 1.16.0 (LeKiwi06) : locomotive (KS_Trains) : 3 wagonnets à four sur une même ligne.
        if (actif("KS_Trains")) {
            // 1.18.0 (LeKiwi06) : wagon jukebox (KS_Trains 1.3.0), sans forme : un wagonnet + un jukebox.
            shapeless("wagon_jukebox", fr.kalium.trains.KSTrains.creerJukebox(),
                    List.of(new RecipeChoice.MaterialChoice(Material.MINECART),
                            new RecipeChoice.MaterialChoice(Material.JUKEBOX)));
            ShapedRecipe locomotive = new ShapedRecipe(key("locomotive"), fr.kalium.trains.KSTrains.creerLocomotive());
            locomotive.shape("FFF");
            locomotive.setIngredient('F', Material.FURNACE_MINECART);
            add(locomotive);
            // 1.17.0 (LeKiwi06) : wagon contrôleur (KS_Trains 1.1.0) : un wagonnet au centre et, en forme de casque
            // (rangée du haut et côtés de celle du wagonnet), un comparateur, un capteur sculk calibré, une lampe à
            // redstone, une ampoule de cuivre (toutes) et un levier, dans n'importe quel ordre, chacun une fois
            // (vérifié dans onPreparerControleur). 1.19.0 (LeKiwi06 : « par cohérence, remplacer le copper bulb par un
            // note block », le klaxon par défaut du contrôleur étant un sifflet en bloc musical) : bloc musical au lieu
            // de l'ampoule de cuivre.
            ShapedRecipe controleur = new ShapedRecipe(key("wagon_controleur"),
                    fr.kalium.trains.KSTrains.creerControleur());
            controleur.shape("AAA", "AWA");
            controleur.setIngredient('A', new RecipeChoice.MaterialChoice(piecesControleur()));
            controleur.setIngredient('W', Material.MINECART);
            add(controleur);
            // 1.20.2 (LeKiwi06, 23/10/2026 : « visuellement c'est buggué [...] j'aimerais que le visuel du craft soit
            // réparé ») : le livre de recettes faisait défiler les 5 pièces dans chaque case de la recette ci-dessus (une
            // même liste pour les 5 cases), d'où par moments 5 fois la même pièce à l'écran. Cette recette « dans
            // n'importe quel ordre » reste valable mais n'est plus montrée dans le livre (CACHEES) ; le livre montre à
            // la place la même recette aux pièces placées (comparateur, capteur, lampe ; bloc musical, wagonnet,
            // levier), qu'on peut aussi poser ailleurs dans le casque.
            ShapedRecipe controleurOrdre = new ShapedRecipe(key("wagon_controleur_ordre"),
                    fr.kalium.trains.KSTrains.creerControleur());
            controleurOrdre.shape("CSL", "NWR");
            controleurOrdre.setIngredient('C', Material.COMPARATOR);
            controleurOrdre.setIngredient('S', Material.CALIBRATED_SCULK_SENSOR);
            controleurOrdre.setIngredient('L', Material.REDSTONE_LAMP);
            controleurOrdre.setIngredient('N', Material.NOTE_BLOCK);
            controleurOrdre.setIngredient('W', Material.MINECART);
            controleurOrdre.setIngredient('R', Material.LEVER);
            add(controleurOrdre);
            ingredients.remove(key("wagon_controleur"));
            cachees.add(key("wagon_controleur"));
        } else {
            getLogger().warning("KS_Trains absent : crafts de la locomotive et du wagon contrôleur ignorés.");
        }

        // 1.21.0 (LeKiwi06, 25/10/2026) : coffre renforcé (KS_CoffreRenforce) : un coffre ordinaire au centre (pas un
        // coffre renforcé ni un coffre renommé), 4 blocs de fer sur les côtés, 4 fragments de spawner aux coins.
        if (actif("KS_ItemSimple") && actif("KS_CoffreRenforce")) {
            ShapedRecipe coffre = new ShapedRecipe(key("coffre_renforce"),
                    fr.kalium.coffrerenforce.KSCoffreRenforce.creerCoffre());
            coffre.shape("FIF", "ICI", "FIF");
            coffre.setIngredient('F', new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerFragmentSpawner()));
            coffre.setIngredient('I', Material.IRON_BLOCK);
            coffre.setIngredient('C', new RecipeChoice.ExactChoice(new ItemStack(Material.CHEST)));
            add(coffre);
        } else {
            getLogger().warning("KS_ItemSimple ou KS_CoffreRenforce absent : craft du coffre renforcé ignoré.");
        }

        // 1.8.0 (LeKiwi06) : réparation de la tête de wither squelette (KS_Decapitator 1.1.0), bloc de charbon de bois.
        TeteWither teteWither = new TeteWither(this);
        teteWither.enregistrer();
        getServer().getPluginManager().registerEvents(teteWither, this);

        // Verrue du Nether : remplacee par le bloc de verrue (briques rouges), craft 9 verrues -> bloc retire.
        Bukkit.removeRecipe(NamespacedKey.minecraft("red_nether_bricks"));
        Bukkit.removeRecipe(NamespacedKey.minecraft("nether_wart_block"));
        ShapedRecipe bricks = new ShapedRecipe(key("red_nether_bricks"), new ItemStack(Material.RED_NETHER_BRICKS));
        bricks.shape("WB", "BW");
        bricks.setIngredient('W', Material.NETHER_WART_BLOCK);
        bricks.setIngredient('B', Material.NETHER_BRICK);
        add(bricks);

        // 1.10.0 (LeKiwi06) : boite de shulker avec un coffre de l'Ender a la place du coffre, pour casser la
        // production automatique des boites (meme forme que la recette vanilla, qui est retiree).
        Bukkit.removeRecipe(NamespacedKey.minecraft("shulker_box"));
        ShapedRecipe shulker = new ShapedRecipe(key("shulker_box"), new ItemStack(Material.SHULKER_BOX));
        shulker.shape("S", "E", "S");
        shulker.setIngredient('S', Material.SHULKER_SHELL);
        shulker.setIngredient('E', Material.ENDER_CHEST);
        add(shulker);

        fonteDesArmures();

        getLogger().info(added.size() + " crafts ajoutés.");
        // Joueurs déjà connectés (rechargement du plugin) qui ont un ingrédient d'une recette du livre.
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
    }

    @Override
    public void onDisable() {
        added.forEach(Bukkit::removeRecipe);
    }

    // ------------------------------------------------------------------ fonte des armures

    private static final Map<Material, Material> LINGOTS = Map.of(Material.IRON_NUGGET, Material.IRON_INGOT,
            Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.COPPER_NUGGET, Material.COPPER_INGOT);

    /** Casque, plastron, jambières ou bottes. */
    private static boolean armure(Material objet) {
        String nom = objet.name();
        return nom.endsWith("_HELMET") || nom.endsWith("_CHESTPLATE") || nom.endsWith("_LEGGINGS") || nom.endsWith("_BOOTS");
    }

    /**
     * 1.12.0 (LeKiwi06, 11/10/2026 : « j'aimerai également que le fait de faire fondre des pieces d'armure donne un lingot
     * et pas une pépite, ça redonnerai de l'utilité a cette fonctionnalité »). Le jeu a une recette de four et une de
     * haut fourneau par métal (fer, or, cuivre), qui change en pépite les outils, les armes, les armures et l'armure
     * de cheval. Chacune est remplacée par deux recettes de même durée et de même expérience : les pièces d'armure
     * (casque, plastron, jambières, bottes ; cotte de mailles comprise) donnent un lingot, le reste garde sa pépite.
     * Les recettes sont lues dans le jeu, pas recopiées : un objet ajouté par une version future suit sa recette.
     */
    private void fonteDesArmures() {
        List<org.bukkit.inventory.CookingRecipe<?>> recettes = new ArrayList<>();
        Bukkit.recipeIterator().forEachRemaining(r -> {
            if ((r instanceof org.bukkit.inventory.FurnaceRecipe || r instanceof org.bukkit.inventory.BlastingRecipe)
                    && r instanceof org.bukkit.inventory.CookingRecipe<?> cuisson
                    && NamespacedKey.MINECRAFT.equals(cuisson.getKey().getNamespace())
                    && LINGOTS.containsKey(cuisson.getResult().getType())
                    && cuisson.getInputChoice() instanceof RecipeChoice.MaterialChoice choix
                    && choix.getChoices().stream().anyMatch(KSCrafts::armure)) {
                recettes.add(cuisson);
            }
        });
        for (org.bukkit.inventory.CookingRecipe<?> recette : recettes) {
            List<Material> entrees = ((RecipeChoice.MaterialChoice) recette.getInputChoice()).getChoices();
            List<Material> armures = entrees.stream().filter(KSCrafts::armure).toList();
            List<Material> autres = entrees.stream().filter(m -> !armure(m)).toList();
            Material pepite = recette.getResult().getType();
            String nom = recette.getKey().getKey();
            Bukkit.removeRecipe(recette.getKey());
            if (!autres.isEmpty()) {
                add(cuisson(recette, key(nom), new ItemStack(pepite), autres));
            }
            add(cuisson(recette, key(nom + "_armure"), new ItemStack(LINGOTS.get(pepite)), armures));
        }
        if (recettes.isEmpty()) {
            getLogger().warning("Fonte des armures : aucune recette du jeu trouvée, rien n'est changé.");
        } else {
            getLogger().info("Fonte des armures : " + recettes.size() + " recettes du jeu remplacées (lingot pour une pièce"
                    + " d'armure).");
        }
    }

    /** La même recette de four ou de haut fourneau (durée, expérience, groupe, catégorie), avec ces entrées et ce résultat. */
    private static org.bukkit.inventory.CookingRecipe<?> cuisson(org.bukkit.inventory.CookingRecipe<?> modele,
            NamespacedKey cle, ItemStack resultat, List<Material> entrees) {
        RecipeChoice choix = new RecipeChoice.MaterialChoice(entrees);
        org.bukkit.inventory.CookingRecipe<?> recette = modele instanceof org.bukkit.inventory.BlastingRecipe
                ? new org.bukkit.inventory.BlastingRecipe(cle, resultat, choix, modele.getExperience(), modele.getCookingTime())
                : new org.bukkit.inventory.FurnaceRecipe(cle, resultat, choix, modele.getExperience(), modele.getCookingTime());
        recette.setGroup(modele.getGroup());
        recette.setCategory(modele.getCategory());
        return recette;
    }

    // ------------------------------------------------------------------ verrue du Nether refusee a l'alambic

    /**
     * 1.10.0 (LeKiwi06) : la verrue du Nether redevient cultivable (KS_LootBlocs 1.3.0) mais ne se brasse plus : sans
     * elle, aucune potion etrange, donc aucune potion de base a l'alambic. Les potions trouvees se modifient
     * toujours (redstone, glowstone, poudre a canon...). La verrue ne peut pas etre posee dans un alambic (clic,
     * glisser, entonnoir) ; par securite, un brassage a la verrue est annule.
     */
    private static boolean verrue(ItemStack item) {
        return item != null && item.getType() == Material.NETHER_WART;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory() instanceof BrewerInventory)) {
            return;
        }
        boolean alambic = event.getClickedInventory() instanceof BrewerInventory;
        boolean refuse;
        if (alambic) {
            refuse = verrue(event.getCursor())
                    || (event.getClick() == ClickType.NUMBER_KEY
                            && verrue(event.getWhoClicked().getInventory().getItem(event.getHotbarButton())))
                    || (event.getClick() == ClickType.SWAP_OFFHAND
                            && verrue(event.getWhoClicked().getInventory().getItemInOffHand()));
        } else {
            refuse = event.isShiftClick() && verrue(event.getCurrentItem());
        }
        if (refuse) {
            event.setCancelled(true);
            event.getWhoClicked().sendActionBar(Component.text("La verrue du Nether ne se brasse plus.",
                    NamedTextColor.RED));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory() instanceof BrewerInventory top && verrue(event.getOldCursor())
                && event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewHopper(InventoryMoveItemEvent event) {
        if (event.getDestination() instanceof BrewerInventory && verrue(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (verrue(event.getContents().getIngredient())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ livre de recettes

    /**
     * Livre de recettes : comme une recette vanilla, une recette du plugin est débloquée quand le joueur obtient l'un de
     * ses ingrédients (objet qui arrive dans son inventaire). Une recette de plugin n'apparaît dans le livre que si
     * elle est débloquée pour le joueur ; une fois débloquée, elle le reste.
     * Clé de l'End depuis 1.3.0 ; Bedrock Breaker depuis 1.4.0 ; spawners et Changeur de Biome depuis 1.6.0 ;
     * 1.9.0 : toutes les recettes du plugin (n'importe lequel de leurs ingrédients, objets custom reconnus exactement).
     */
    private void debloquer(Player player, ItemStack obtenu) {
        if (obtenu == null || obtenu.getType().isAir()) {
            return;
        }
        ingredients.forEach((recette, choix) -> {
            if (!player.hasDiscoveredRecipe(recette) && choix.stream().anyMatch(c -> c.test(obtenu))) {
                decouvrir(player, recette);
            }
        });
    }

    private void decouvrir(Player player, NamespacedKey recette) {
        if (added.contains(recette) && !player.hasDiscoveredRecipe(recette)) {
            player.discoverRecipe(recette);
        }
    }

    /** Joueur qui a déjà un ingrédient (obtenu avant cette version, ou au démarrage du plugin). */
    private void livreDeRecettes(Player player) {
        for (NamespacedKey cachee : cachees) {
            if (player.hasDiscoveredRecipe(cachee)) {
                player.undiscoverRecipe(cachee);
            }
        }
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

    NamespacedKey key(String id) {
        return new NamespacedKey(this, id);
    }

    boolean actif(String plugin) {
        return getServer().getPluginManager().isPluginEnabled(plugin);
    }

    /** 1.6.0 : 7 Fragments de Spawner + 1 Cœur de Spawner + la tête -> spawner (id : zombi, squelette...). */
    private void spawner(String id, RecipeChoice tete) {
        shapeless("spawner_" + id, fr.kalium.spawners.KSSpawners.creerSpawner(id), concat(
                repeat(new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerFragmentSpawner()), 7),
                List.of(new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerCoeurSpawner()), tete)));
    }

    /** 1.7.0 : spawner (id de KS_Spawners) avec n'importe quelle tête de KS_Decapitator de la créature. */
    private void spawnerTete(String recette, String id, EntityType creature) {
        List<ItemStack> tetes = fr.kalium.decapitator.KSDecapitator.tetesDe(creature);
        if (tetes.isEmpty()) {
            getLogger().warning("Aucune tête de " + creature.getKey().getKey() + " dans KS_Decapitator : craft ignoré.");
            return;
        }
        shapeless("spawner_" + recette, fr.kalium.spawners.KSSpawners.creerSpawner(id), concat(
                repeat(new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerFragmentSpawner()), 7),
                List.of(new RecipeChoice.ExactChoice(fr.kalium.itemsimple.KSItemSimple.creerCoeurSpawner()),
                        new RecipeChoice.ExactChoice(tetes))));
    }

    void add(org.bukkit.inventory.Recipe recipe) {
        Bukkit.addRecipe(recipe);
        NamespacedKey cle = ((org.bukkit.Keyed) recipe).getKey();
        added.add(cle);
        // 1.9.0 : ingrédients, pour débloquer la recette dans le livre dès qu'on en obtient un.
        List<RecipeChoice> choix = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shaped) {
            shaped.getChoiceMap().values().stream().filter(java.util.Objects::nonNull).forEach(choix::add);
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            choix.addAll(shapeless.getChoiceList());
        } else if (recipe instanceof org.bukkit.inventory.CookingRecipe<?> cuisson) {
            choix.add(cuisson.getInputChoice());
        }
        ingredients.put(cle, choix);
    }

    /** 1.17.0 : les 5 pièces du wagon contrôleur. 1.19.0 : bloc musical au lieu de l'ampoule de cuivre. */
    private static List<Material> piecesControleur() {
        return List.of(Material.COMPARATOR, Material.CALIBRATED_SCULK_SENSOR, Material.REDSTONE_LAMP,
                Material.NOTE_BLOCK, Material.LEVER);
    }

    /**
     * 1.17.0 : wagon contrôleur seulement si chacune des 5 pièces est là une fois (pas 5 comparateurs).
     * 1.20.1 (LeKiwi06, 23/10/2026 : « on peut le crafter avec 5 fois le même item au lieu des 5 items différents ») :
     * la recette est reconnue par sa clé quel que soit son type (avant : seulement si le jeu la rendait en ShapedRecipe),
     * et le contrôle est refait au moment de prendre le wagon (onCrafterControleur) et dans un crafter (onCrafterBloc),
     * qui ne passait pas par ici.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPreparerControleur(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (estControleur(event.getRecipe()) && !piecesValides(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    /** 1.20.1 : prise du wagon refusée si les 5 pièces ne sont pas toutes différentes (clic, Maj + clic, Bedrock). */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterControleur(org.bukkit.event.inventory.CraftItemEvent event) {
        if (estControleur(event.getRecipe()) && !piecesValides(event.getInventory().getMatrix())) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
        }
    }

    /** 1.20.1 : un crafter (établi automatique) ne fabrique pas un wagon contrôleur avec de mauvaises pièces. */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterBloc(org.bukkit.event.block.CrafterCraftEvent event) {
        if (!estControleur(event.getRecipe())) {
            return;
        }
        if (!(event.getBlock().getState() instanceof org.bukkit.block.Crafter crafter)
                || !piecesValides(crafter.getInventory().getContents())) {
            event.setCancelled(true);
        }
    }

    private boolean estControleur(org.bukkit.inventory.Recipe recette) {
        return recette instanceof org.bukkit.Keyed k && k.getKey().equals(key("wagon_controleur"));
    }

    /** Les 5 pièces du contrôleur, chacune une fois, et rien d'autre que le wagonnet. */
    private static boolean piecesValides(ItemStack[] matrice) {
        java.util.Set<Material> vues = java.util.EnumSet.noneOf(Material.class);
        List<Material> pieces = piecesControleur();
        for (ItemStack objet : matrice) {
            if (objet == null || objet.isEmpty() || objet.getType() == Material.MINECART) {
                continue;
            }
            if (!pieces.contains(objet.getType()) || !vues.add(objet.getType())) {
                return false;
            }
        }
        return vues.size() == pieces.size();
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
