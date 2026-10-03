package fr.kalium.crafts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 1.8.0 (LeKiwi06, 03/10/2026) : réparation de la tête de wither squelette de KS_Decapitator 1.1.0 (sale, endommagée,
 * désactivée ; un défaut par craft, dans n'importe quel ordre ; réparée des 3 : vrai crâne de wither squelette) et bloc
 * de charbon de bois.
 *
 * - Désactivée (réactiver) : la tête au centre, autour en alternance 4 blocs d'émeraude compressés tier 2 (KS_Economy)
 *   et 4 fioles d'expérience de 15 niveaux exactement (KS_FioleExp) ; les deux façons (blocs aux coins ou aux côtés).
 * - Sale (nettoyer) : 8 pinceaux non endommagés autour.
 * - Endommagée (réparer) : en alternance 4 lingots de netherite et 4 blocs de charbon de bois ; les deux façons.
 * - Bloc de charbon de bois : 9 charbons de bois ; apparence d'un bloc de charbon ; ingrédient (et combustible) : ne se
 *   pose pas ; se redéfait en 9 charbons de bois ; jamais pris pour un bloc de charbon dans une autre recette.
 *
 * Les recettes sont des formes (affichées dans le livre de recettes) ; les ingrédients exacts et le résultat sont
 * vérifiés à chaque craft.
 */
final class TeteWither implements Listener {

    private final KSCrafts plugin;
    private final NamespacedKey cleCharbon;
    /** Recette -> défaut qu'elle répare. */
    private final Map<NamespacedKey, String> reparations = new HashMap<>();
    /** Recette -> vérification des 8 ingrédients autour : coins, côtés. */
    private final Map<NamespacedKey, Predicate<ItemStack>[]> verifs = new HashMap<>();
    private NamespacedKey bloc;
    private NamespacedKey defaire;

    TeteWither(KSCrafts plugin) {
        this.plugin = plugin;
        this.cleCharbon = new NamespacedKey(plugin, "bloc_charbon_de_bois");
    }

    /** Un bloc de charbon de bois. */
    ItemStack creerBlocCharbonDeBois() {
        ItemStack item = new ItemStack(Material.COAL_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Bloc de charbon de bois", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(cleCharbon, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean estBlocCharbonDeBois(ItemStack item) {
        return item != null && item.getType() == Material.COAL_BLOCK && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleCharbon, PersistentDataType.BYTE);
    }

    void enregistrer() {
        // Bloc de charbon de bois : 9 charbons de bois, et l'inverse.
        bloc = plugin.key("bloc_charbon_de_bois");
        ShapedRecipe r = new ShapedRecipe(bloc, creerBlocCharbonDeBois());
        r.shape("CCC", "CCC", "CCC");
        r.setIngredient('C', Material.CHARCOAL);
        plugin.add(r);
        defaire = plugin.key("charbon_de_bois_du_bloc");
        ShapelessRecipe d = new ShapelessRecipe(defaire, new ItemStack(Material.CHARCOAL, 9));
        d.addIngredient(new RecipeChoice.ExactChoice(creerBlocCharbonDeBois()));
        plugin.add(d);

        if (!plugin.actif("KS_Decapitator")) {
            plugin.getLogger().warning("KS_Decapitator absent : crafts de réparation de la tête de wither squelette ignorés.");
            return;
        }
        Predicate<ItemStack> pinceau = i -> i != null && i.getType() == Material.BRUSH
                && !(i.getItemMeta() instanceof Damageable dm && dm.hasDamage() && dm.getDamage() > 0);
        Predicate<ItemStack> netherite = i -> i != null && i.getType() == Material.NETHERITE_INGOT && !i.hasItemMeta();
        Predicate<ItemStack> charbon = this::estBlocCharbonDeBois;
        reparation("tete_wither_nettoyer", "sale", Material.BRUSH, Material.BRUSH, pinceau, pinceau);
        reparation("tete_wither_reparer_a", "endommagee", Material.NETHERITE_INGOT, Material.COAL_BLOCK, netherite, charbon);
        reparation("tete_wither_reparer_b", "endommagee", Material.COAL_BLOCK, Material.NETHERITE_INGOT, charbon, netherite);
        if (plugin.actif("KS_Economy") && plugin.actif("KS_FioleExp")) {
            ItemStack fiole15 = fr.kalium.fioleexp.KSFioleExp.creerFioleNiveaux(15);
            Predicate<ItemStack> blocTier2 = i -> fr.kalium.economy.KSEconomy.tierBloc(i) == 2;
            Predicate<ItemStack> fiole = i -> i != null && i.isSimilar(fiole15);
            reparation("tete_wither_reactiver_a", "desactivee", Material.EMERALD_BLOCK, Material.EXPERIENCE_BOTTLE,
                    blocTier2, fiole);
            reparation("tete_wither_reactiver_b", "desactivee", Material.EXPERIENCE_BOTTLE, Material.EMERALD_BLOCK,
                    fiole, blocTier2);
        } else {
            plugin.getLogger().warning("KS_Economy ou KS_FioleExp absent : craft « réactiver » de la tête de wither "
                    + "squelette ignoré.");
        }
    }

    /** Forme : la tête au centre, coins et côtés ; résultat réel calculé au moment du craft. */
    @SuppressWarnings("unchecked")
    private void reparation(String id, String defaut, Material coins, Material cotes, Predicate<ItemStack> okCoins,
                            Predicate<ItemStack> okCotes) {
        NamespacedKey cle = plugin.key(id);
        ShapedRecipe r = new ShapedRecipe(cle, new ItemStack(Material.WITHER_SKELETON_SKULL));
        r.shape("ABA", "BTB", "ABA");
        r.setIngredient('A', coins);
        r.setIngredient('B', cotes);
        r.setIngredient('T', Material.PLAYER_HEAD);
        plugin.add(r);
        reparations.put(cle, defaut);
        verifs.put(cle, new Predicate[]{okCoins, okCotes});
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        Recipe recette = event.getRecipe();
        NamespacedKey cle = recette instanceof org.bukkit.Keyed k ? k.getKey() : null;
        ItemStack[] grille = inv.getMatrix();
        // Le bloc de charbon de bois n'est jamais un bloc de charbon dans une autre recette.
        boolean contientBloc = false;
        for (ItemStack i : grille) {
            if (estBlocCharbonDeBois(i)) {
                contientBloc = true;
                break;
            }
        }
        String defaut = cle == null ? null : reparations.get(cle);
        if (contientBloc && defaut == null && !defaire.equals(cle)) {
            inv.setResult(null);
            return;
        }
        if (defaut == null) {
            return;
        }
        // Réparation : grille 3 x 3 exacte (coins 0 2 6 8, côtés 1 3 5 7, tête 4).
        if (grille.length != 9) {
            inv.setResult(null);
            return;
        }
        Predicate<ItemStack>[] v = verifs.get(cle);
        boolean ok = true;
        for (int i : new int[]{0, 2, 6, 8}) {
            ok &= v[0].test(grille[i]);
        }
        for (int i : new int[]{1, 3, 5, 7}) {
            ok &= v[1].test(grille[i]);
        }
        ItemStack resultat = ok ? fr.kalium.decapitator.KSDecapitator.reparer(grille[4], defaut) : null;
        inv.setResult(resultat);
    }

    /** Le bloc de charbon de bois ne se pose pas. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (estBlocCharbonDeBois(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Le bloc de charbon de bois ne se pose pas : c'est un ingrédient.",
                    NamedTextColor.GRAY));
        }
    }
}
