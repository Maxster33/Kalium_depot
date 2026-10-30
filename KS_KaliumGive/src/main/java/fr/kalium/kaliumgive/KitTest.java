package fr.kalium.kaliumgive;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.TransmuteRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 1.12.0 (LeKiwi06, 30/10/2026 : « créer moi des kits de test avec la commande /kalium give, il faut que ça me donne un
 * coffre dans lequel il y a des shulker [...] renommés par le nom du plugin [...] il me faut les items finis, et de quoi
 * faire leurs crafts ») : id kit_test.
 *
 * Un coffre de boîtes de shulker, une par plugin KS_ (nom sans « KS_ » : Trains, Alcools...), avec les objets finis du
 * plugin puis les ingrédients d'une fois chacune de ses recettes. Tout est lu au moment de la commande :
 * - objets finis : les id_custom de /kaliumgive (sauf les 332 têtes) et le résultat de chaque recette qui porte une
 *   marque d'un plugin KS_ (le plugin de la marque, ex. la locomotive de KS_Crafts va dans Trains) ;
 * - ingrédients : chaque recette du serveur dont le résultat porte la marque d'un plugin KS_, ou dont la clé est celle
 *   d'un plugin KS_ (crafts de KS_Crafts, atouts de KS_Atouts...) ; une case qui accepte plusieurs objets donne le
 *   premier (ex. un seul bois pour un bateau) ;
 * - KS_Alcools : alambic (mélanges sans liste dans l'API de Paper) : chaque alcool dans chaque variante, et les
 *   ingrédients écrits ici (à tenir à jour avec Alcools.java de KS_Alcools).
 * Une boîte pleine (27 cases) : la suite dans une autre, « Alcools (2/3) » ; un coffre plein : un autre coffre.
 */
final class KitTest {

    private static final int CASES = 27;

    private KitTest() {
    }

    /** Objets finis et ingrédients d'un plugin. */
    private static final class Kit {
        final List<ItemStack> finis = new ArrayList<>();
        final List<ItemStack> ingredients = new ArrayList<>();
    }

    /** Les coffres du kit (un seul tant qu'il y a 27 boîtes au plus). objets : plugin -> objets de /kaliumgive. */
    static List<ItemStack> coffres(Map<String, List<ItemStack>> objets) {
        // Espace de noms (marques, clés de recettes) -> plugin KS_ activé.
        Map<String, String> plugins = new HashMap<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p.isEnabled() && p.getName().startsWith("KS_")) {
                plugins.put(p.getName().toLowerCase(Locale.ROOT), p.getName());
            }
        }
        Map<String, Kit> kits = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        objets.forEach((plugin, items) -> items.forEach(i -> ajouterFini(kits.computeIfAbsent(plugin, k -> new Kit()), i)));

        Bukkit.recipeIterator().forEachRemaining(recette -> {
            if (!(recette instanceof Keyed cle)) {
                return;
            }
            ItemStack resultat = recette.getResult();
            String marque = proprietaire(resultat, plugins);
            String plugin = marque != null ? marque : plugins.get(cle.getKey().getNamespace());
            if (plugin == null) {
                return;
            }
            Kit kit = kits.computeIfAbsent(plugin, k -> new Kit());
            // Sans marque, le résultat est un objet du jeu ou un aperçu (atouts) : seulement les ingrédients.
            if (marque != null) {
                ajouterFini(kit, resultat);
            }
            for (ItemStack ingredient : ingredients(recette)) {
                ajouterIngredient(kit, ingredient);
            }
        });

        if (plugins.containsValue("KS_Alcools")) {
            alcools(kits.computeIfAbsent("KS_Alcools", k -> new Kit()));
        }

        List<ItemStack> boites = new ArrayList<>();
        kits.forEach((plugin, kit) -> boites.addAll(boites(plugin, kit)));
        List<ItemStack> coffres = new ArrayList<>();
        int nombre = (boites.size() + CASES - 1) / CASES;
        for (int i = 0; i < nombre; i++) {
            coffres.add(contenant(Material.CHEST, nombre == 1 ? "Kit de test" : "Kit de test (" + (i + 1) + "/" + nombre + ")",
                    boites.subList(i * CASES, Math.min(boites.size(), (i + 1) * CASES))));
        }
        return coffres;
    }

    /** Plugin KS_ de la première marque de l'objet, ou null (objet du jeu). */
    private static String proprietaire(ItemStack item, Map<String, String> plugins) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        for (NamespacedKey cle : item.getItemMeta().getPersistentDataContainer().getKeys()) {
            String plugin = plugins.get(cle.getNamespace());
            if (plugin != null) {
                return plugin;
            }
        }
        return null;
    }

    /** Une fois chaque case de la recette. */
    private static List<ItemStack> ingredients(Recipe recette) {
        List<RecipeChoice> cases = new ArrayList<>();
        if (recette instanceof ShapedRecipe forme) {
            Map<Character, RecipeChoice> choix = forme.getChoiceMap();
            for (String ligne : forme.getShape()) {
                for (char c : ligne.toCharArray()) {
                    cases.add(choix.get(c));
                }
            }
        } else if (recette instanceof ShapelessRecipe sansForme) {
            cases.addAll(sansForme.getChoiceList());
        } else if (recette instanceof CookingRecipe<?> cuisson) {
            cases.add(cuisson.getInputChoice());
        } else if (recette instanceof StonecuttingRecipe tailleuse) {
            cases.add(tailleuse.getInputChoice());
        } else if (recette instanceof SmithingTransformRecipe forge) {
            cases.add(forge.getTemplate());
            cases.add(forge.getBase());
            cases.add(forge.getAddition());
        } else if (recette instanceof TransmuteRecipe transmutation) {
            cases.add(transmutation.getInput());
            cases.add(transmutation.getMaterial());
        }
        List<ItemStack> items = new ArrayList<>();
        for (RecipeChoice c : cases) {
            ItemStack item = exemple(c);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /** Premier objet accepté par la case (null : case vide). */
    private static ItemStack exemple(RecipeChoice choix) {
        if (choix == null) {
            return null;
        }
        try {
            ItemStack item = choix instanceof RecipeChoice.ExactChoice exact ? exact.getChoices().getFirst()
                    : choix.getItemStack();
            if (item == null || item.getType().isAir()) {
                return null;
            }
            return item.asQuantity(1);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Objet fini : une seule fois (même objet donné par /kaliumgive et par sa recette). */
    private static void ajouterFini(Kit kit, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        for (ItemStack deja : kit.finis) {
            if (deja.isSimilar(item)) {
                return;
            }
        }
        kit.finis.add(item.clone());
    }

    /** Ingrédient : les quantités des recettes s'additionnent. */
    private static void ajouterIngredient(Kit kit, ItemStack item) {
        for (ItemStack deja : kit.ingredients) {
            if (deja.isSimilar(item)) {
                deja.setAmount(deja.getAmount() + item.getAmount());
                return;
            }
        }
        kit.ingredients.add(item.clone());
    }

    /**
     * Alcools de KS_Alcools : chacun dans chaque variante (normal, long, fort ; à boire et jetable), puis les ingrédients
     * de l'alambic : 3 fioles d'eau, l'ingrédient de chaque alcool (pomme de terre empoisonnée pour l'alcool neutre),
     * redstone (long), glowstone (fort), poudre à canon (jetable), poudre de blaze (carburant de l'alambic).
     */
    private static void alcools(Kit kit) {
        for (String id : fr.kalium.alcools.KSAlcools.ids()) {
            for (boolean jetable : new boolean[]{false, true}) {
                for (String variante : List.of("normal", "long", "fort")) {
                    ajouterFini(kit, fr.kalium.alcools.KSAlcools.alcool(id, variante, jetable));
                }
            }
        }
        ItemStack eau = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) eau.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        eau.setItemMeta(meta);
        eau.setAmount(3);
        ajouterIngredient(kit, eau);
        for (Material m : List.of(Material.POISONOUS_POTATO, Material.CACTUS_FLOWER, Material.POTATO,
                Material.HONEY_BOTTLE, Material.APPLE, Material.WHEAT, Material.MAGMA_CREAM, Material.SUGAR_CANE,
                Material.REDSTONE, Material.GLOWSTONE_DUST, Material.GUNPOWDER, Material.BLAZE_POWDER)) {
            ajouterIngredient(kit, new ItemStack(m));
        }
    }

    /** Les boîtes d'un plugin : objets finis puis ingrédients, en piles de leur taille maximale. */
    private static List<ItemStack> boites(String plugin, Kit kit) {
        List<ItemStack> piles = new ArrayList<>();
        for (List<ItemStack> liste : List.of(kit.finis, kit.ingredients)) {
            for (ItemStack item : liste) {
                int reste = item.getAmount();
                int max = Math.max(1, item.getMaxStackSize());
                while (reste > 0) {
                    piles.add(item.asQuantity(Math.min(reste, max)));
                    reste -= max;
                }
            }
        }
        String nom = plugin.startsWith("KS_") ? plugin.substring(3) : plugin;
        List<ItemStack> boites = new ArrayList<>();
        int nombre = (piles.size() + CASES - 1) / CASES;
        for (int i = 0; i < nombre; i++) {
            boites.add(contenant(Material.SHULKER_BOX, nombre == 1 ? nom : nom + " (" + (i + 1) + "/" + nombre + ")",
                    piles.subList(i * CASES, Math.min(piles.size(), (i + 1) * CASES))));
        }
        return boites;
    }

    /** Coffre ou boîte de shulker nommé, avec ce contenu (gardé quand on le pose). */
    private static ItemStack contenant(Material type, String nom, List<ItemStack> contenu) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(nom, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.CONTAINER, ItemContainerContents.containerContents(new ArrayList<>(contenu)));
        return item;
    }
}
