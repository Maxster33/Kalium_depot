package fr.kalium.alcools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.potion.PotionMix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Les alcools (cahier, partie 3.2) : objets et mélanges d'alambic.
 *
 * Un alcool est une potion sans type de base (aucune recette d'alambic du jeu ne le reconnaît : pas de souffle de
 * dragon, pas de verrue...), marquée « alcool » (id) et « variante » (normal, long, fort). Ses mélanges sont ajoutés
 * par Paper : eau + patate empoisonnée = alcool neutre ; alcool neutre + ingrédient = alcool ; redstone (8 min),
 * glowstone (niveau II, 1 min 30, pas pour la bière ni le tabascoolisé), poudre à canon (jetable). L'alcool neutre n'a
 * aucun mélange de durée ni de version jetable (base des autres, combustible).
 */
final class Alcools {

    static final String NEUTRE = "neutre";
    static final String VODKA = "vodka";
    static final String TABASCOOLISE = "tabascoolise";
    static final String RHUM = "rhum";

    /** Durée de base, redstone, glowstone (comme les potions du jeu). */
    enum Variante {
        NORMAL("normal", 180), LONG("long", 480), FORT("fort", 90);

        final String cle;
        final int secondes;

        Variante(String cle, int secondes) {
            this.cle = cle;
            this.secondes = secondes;
        }

        static Variante de(String cle) {
            for (Variante v : values()) {
                if (v.cle.equals(cle)) {
                    return v;
                }
            }
            return null;
        }
    }

    /**
     * Un alcool : nom, couleur, ingrédient ajouté à l'alcool neutre (à l'eau pour l'alcool neutre), effet du jeu (null :
     * effet custom de KS_Alcools ou aucun), version niveau II à la glowstone.
     */
    record Alcool(String id, String nom, Color couleur, Material ingredient, PotionEffectType effet, boolean fort) {
    }

    static final Map<String, Alcool> ALCOOLS = new LinkedHashMap<>();

    static {
        ajouter(new Alcool(NEUTRE, "Alcool neutre", Color.fromRGB(0xE3EEF2), Material.POISONOUS_POTATO, null, false));
        ajouter(new Alcool("tequila", "Tequila", Color.fromRGB(0xE8C547), Material.CACTUS_FLOWER,
                PotionEffectType.RESISTANCE, true));
        ajouter(new Alcool(VODKA, "Vodka", Color.fromRGB(0xDCE6F0), Material.POTATO, null, true));
        ajouter(new Alcool("hydromel", "Hydromel", Color.fromRGB(0xF2A93B), Material.HONEY_BOTTLE,
                PotionEffectType.HASTE, true));
        ajouter(new Alcool("manzana", "Manzana", Color.fromRGB(0x9BC53D), Material.APPLE,
                PotionEffectType.HEALTH_BOOST, true));
        ajouter(new Alcool("biere", "Bière", Color.fromRGB(0xD9902B), Material.WHEAT,
                PotionEffectType.DOLPHINS_GRACE, false));
        ajouter(new Alcool(TABASCOOLISE, "Tabascoolisé", Color.fromRGB(0xC8261E), Material.MAGMA_CREAM, null, false));
        // 1.1.0 : rhum (étape 5) : musique d'un disque autour du buveur (Rhum) ; pas de niveau II.
        ajouter(new Alcool(RHUM, "Rhum", Color.fromRGB(0x8B4513), Material.SUGAR_CANE, null, false));
    }

    private static void ajouter(Alcool alcool) {
        ALCOOLS.put(alcool.id(), alcool);
    }

    /**
     * Combustible de l'alcool neutre (composant du jeu « cooking_fuel », sans méthode dans l'API de Paper) : 10 000 ticks
     * dans un four (50 cuissons), moitié moins dans un fumoir ou un haut fourneau (qui cuisent deux fois plus vite :
     * 50 cuissons aussi), comme le charbon (1 600) et le seau de lave (20 000) du jeu. Avec ce composant, le four,
     * les entonnoirs et la case du combustible acceptent l'alcool neutre comme un combustible du jeu.
     */
    private static final String COMBUSTIBLE = "minecraft:potion[minecraft:cooking_fuel={burn_time:{type:\"minecraft:div\","
            + "left:10000,right:{type:\"minecraft:conditional\",condition:\"minecraft:block/fast_cooking\","
            + "on_false:\"minecraft:cooking/normal_burn_time_reduction_factor\","
            + "on_true:\"minecraft:cooking/fast_burn_time_reduction_factor\"}},"
            + "speed_multiplier:\"minecraft:cooking/speed_default\"}]";
    /** Repli si le jeu refuse le calcul ci-dessus : durée fixe (le fumoir et le haut fourneau sont corrigés au four). */
    private static final String COMBUSTIBLE_FIXE = "minecraft:potion[minecraft:cooking_fuel={burn_time:10000,"
            + "speed_multiplier:\"minecraft:cooking/speed_default\"}]";

    private static NamespacedKey cleAlcool;
    private static NamespacedKey cleVariante;
    private static boolean combustibleSignale;

    private static final List<NamespacedKey> MELANGES = new ArrayList<>();

    private Alcools() {
    }

    static void init(KSAlcools plugin) {
        cleAlcool = new NamespacedKey(plugin, "alcool");
        cleVariante = new NamespacedKey(plugin, "variante");
    }

    // ------------------------------------------------------------------ objets

    /** Un alcool (null si la combinaison n'existe pas : alcool neutre jetable ou allongé, niveau II refusé). */
    static ItemStack creer(String id, Variante variante, boolean jetable) {
        Alcool a = ALCOOLS.get(id);
        if (a == null || variante == null || (variante == Variante.FORT && !a.fort())
                || (id.equals(NEUTRE) && (jetable || variante != Variante.NORMAL))) {
            return null;
        }
        ItemStack item = id.equals(NEUTRE) ? avecCombustible() : new ItemStack(Material.POTION);
        if (jetable) {
            item = item.withType(Material.SPLASH_POTION);
        }
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(null);
        meta.setColor(a.couleur());
        if (a.effet() != null) {
            meta.addCustomEffect(new PotionEffect(a.effet(), variante.secondes * 20,
                    variante == Variante.FORT ? 1 : 0), true);
        }
        meta.displayName(Component.text(jetable ? a.nom() + " jetable" : a.nom(), TextColor.color(a.couleur().asRGB()))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = lignes(a, variante);
        if (!lignes.isEmpty()) {
            meta.lore(lignes);
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(cleAlcool, PersistentDataType.STRING, id);
        pdc.set(cleVariante, PersistentDataType.STRING, variante.cle);
        item.setItemMeta(meta);
        if (a.effet() == null) {
            // Effet custom ou aucun : la ligne « Aucun effet » du jeu est remplacée par la description.
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                    .addHiddenComponents(DataComponentTypes.POTION_CONTENTS).build());
        }
        return item;
    }

    /** Description des alcools sans effet du jeu (le jeu affiche lui-même les autres effets). */
    private static List<Component> lignes(Alcool a, Variante v) {
        List<Component> lignes = new ArrayList<>();
        switch (a.id()) {
            case NEUTRE -> {
                lignes.add(ligne("Aucun effet", NamedTextColor.GRAY));
                lignes.add(ligne("Base des alcools, à l'alambic", NamedTextColor.DARK_GRAY));
                lignes.add(ligne("Combustible : 50 cuissons", NamedTextColor.DARK_GRAY));
            }
            case VODKA -> {
                lignes.add(ligne((v == Variante.FORT ? "Lévitation II (" : "Lévitation (") + duree(v.secondes) + ")",
                        NamedTextColor.BLUE));
                lignes.add(ligne("En pause quand tu es accroupi", NamedTextColor.DARK_GRAY));
            }
            case TABASCOOLISE -> {
                lignes.add(ligne("Antigel (" + duree(v.secondes) + ")", NamedTextColor.BLUE));
                lignes.add(ligne("Ni gel ni dégâts de la poudreuse", NamedTextColor.DARK_GRAY));
            }
            case RHUM -> {
                lignes.add(ligne("Musique (" + duree(v.secondes) + ")", NamedTextColor.BLUE));
                lignes.add(ligne("Un disque au hasard, entendu à 5 blocs", NamedTextColor.DARK_GRAY));
            }
            default -> {
            }
        }
        return lignes;
    }

    private static Component ligne(String texte, NamedTextColor couleur) {
        return Component.text(texte, couleur).decoration(TextDecoration.ITALIC, false);
    }

    /** « 3:00 », comme le jeu. */
    static String duree(int secondes) {
        return secondes / 60 + ":" + String.format("%02d", secondes % 60);
    }

    /** Potion avec le composant de combustible (potion simple si le jeu refuse le composant : signalé une fois). */
    private static ItemStack avecCombustible() {
        try {
            return Bukkit.getItemFactory().createItemStack(COMBUSTIBLE);
        } catch (IllegalArgumentException ignore) {
            // Repli ci-dessous.
        }
        try {
            return Bukkit.getItemFactory().createItemStack(COMBUSTIBLE_FIXE);
        } catch (IllegalArgumentException erreur) {
            if (!combustibleSignale) {
                combustibleSignale = true;
                KSAlcools.instance().getLogger().warning("Composant cooking_fuel refusé : l'alcool neutre ne sera pas "
                        + "un combustible des fours (" + erreur.getMessage() + ")");
            }
            return new ItemStack(Material.POTION);
        }
    }

    /** Id de l'alcool, ou null si ce n'est pas un alcool du plugin. */
    static String id(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(cleAlcool, PersistentDataType.STRING);
    }

    static Variante variante(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return Variante.de(item.getItemMeta().getPersistentDataContainer().get(cleVariante, PersistentDataType.STRING));
    }

    static boolean estNeutre(ItemStack item) {
        return item != null && item.getType() == Material.POTION && NEUTRE.equals(id(item));
    }

    // ------------------------------------------------------------------ alambic

    /** Ajoute tous les mélanges d'alambic. */
    static void melanges(KSAlcools plugin) {
        Alcool neutre = ALCOOLS.get(NEUTRE);
        melange(plugin, "neutre", RecipeChoice.predicateChoice(Alcools::eau, eau()), neutre.ingredient(),
                creer(NEUTRE, Variante.NORMAL, false));
        for (Alcool a : ALCOOLS.values()) {
            if (a.id().equals(NEUTRE)) {
                continue;
            }
            melange(plugin, a.id(), choix(NEUTRE, Variante.NORMAL, false), a.ingredient(),
                    creer(a.id(), Variante.NORMAL, false));
            for (boolean jetable : new boolean[]{false, true}) {
                String suffixe = jetable ? "_jetable" : "";
                melange(plugin, a.id() + "_long" + suffixe, choix(a.id(), Variante.NORMAL, jetable), Material.REDSTONE,
                        creer(a.id(), Variante.LONG, jetable));
                if (a.fort()) {
                    melange(plugin, a.id() + "_fort" + suffixe, choix(a.id(), Variante.NORMAL, jetable),
                            Material.GLOWSTONE_DUST, creer(a.id(), Variante.FORT, jetable));
                }
            }
            for (Variante v : Variante.values()) {
                if (v != Variante.FORT || a.fort()) {
                    melange(plugin, a.id() + "_" + v.cle + "_poudre", choix(a.id(), v, false), Material.GUNPOWDER,
                            creer(a.id(), v, true));
                }
            }
        }
        plugin.getLogger().info(MELANGES.size() + " mélanges d'alambic ajoutés.");
    }

    static void retirerMelanges() {
        MELANGES.forEach(Bukkit.getPotionBrewer()::removePotionMix);
        MELANGES.clear();
    }

    private static void melange(KSAlcools plugin, String nom, RecipeChoice entree, Material ingredient,
                                ItemStack resultat) {
        NamespacedKey cle = new NamespacedKey(plugin, nom);
        Bukkit.getPotionBrewer().addPotionMix(new PotionMix(cle, resultat, entree,
                new RecipeChoice.MaterialChoice(ingredient)));
        MELANGES.add(cle);
    }

    /** Un alcool précis dans une case de l'alambic (reconnu par ses marques, pas par son nom). */
    private static RecipeChoice choix(String id, Variante variante, boolean jetable) {
        Material type = jetable ? Material.SPLASH_POTION : Material.POTION;
        Predicate<ItemStack> test = item -> item.getType() == type && id.equals(id(item)) && variante(item) == variante;
        return RecipeChoice.predicateChoice(test, creer(id, variante, jetable));
    }

    /** Fiole d'eau du jeu (exemple affiché par la recette). */
    private static ItemStack eau() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        item.setItemMeta(meta);
        return item;
    }

    /** Fiole d'eau du jeu (pas un alcool). */
    private static boolean eau(ItemStack item) {
        if (item.getType() != Material.POTION || id(item) != null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta instanceof PotionMeta potion && potion.getBasePotionType() == PotionType.WATER
                && !potion.hasCustomEffects();
    }
}
