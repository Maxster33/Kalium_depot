package fr.kalium.cuisine;

import fr.kalium.effets.KSEffets;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * KS_Cuisine (cahier des charges : Atouts_Alcools_Aliments.md, LeKiwi06, 28/10/2026, partie 3.4) : serveur Kixster,
 * étape 3.
 *
 * Valeurs de LeKiwi06 en cuisses, ici en points du jeu (1 cuisse = 2 points) :
 * - Pomme d'amour (pomme + 8 sucres) : comme une pomme ; mobs et joueurs amoureux ({@link Amour}).
 * - Cookie à la baie sucrée (cookie + 3 baies sucrées) : 6 / 1,2, apprivoiseur 3 min ({@link Cookies}).
 * - Cookie à la baie lumineuse (cookie + 3 baies lumineuses) : 2 / 0,4, lumière 3 min ({@link Cookies}).
 * - Sandwich jambon-beurre (pain + côtelette cuite + seau de lait, le seau revient) : 15 / 20,8.
 * - Brunch (côtelette crue + œuf) : 10 / 7.
 * - Blé soufflé (blé au four ou au fumoir) : 1 / 1, se mange en 0,8 s (base : kelp séché, image du blé).
 * - Bol de céréales (seau de lait + 8 blés soufflés, le seau revient) : 10 / 10, remet à zéro le temps sans dormir
 *   (plus de phantoms) ; n'enlève pas les effets (base : tarte à la citrouille, image du ragoût de champignons).
 *
 * Autres plugins : objet(id), ids(), idObjet(objet), estBebe(mob).
 */
public final class KSCuisine extends JavaPlugin implements Listener {

    /** Un aliment : nom, couleur du nom, base, image (null : celle de la base), nourriture, saturation. */
    record Aliment(String nom, TextColor couleur, Material base, String image, int nourriture, float saturation,
                   String description) {
    }

    static final String POMME = "pomme_amour";
    static final String COOKIE_BAIE = "cookie_baie_sucree";
    static final String COOKIE_LUMIERE = "cookie_baie_lumineuse";
    static final String SANDWICH = "sandwich";
    static final String BRUNCH = "brunch";
    static final String BLE = "ble_souffle";
    static final String BOL = "bol_cereales";

    static final Map<String, Aliment> ALIMENTS = new LinkedHashMap<>();

    static {
        ALIMENTS.put(POMME, new Aliment("Pomme d'amour", TextColor.color(0xFF4F7B), Material.APPLE, null, 4, 2.4f,
                "Clic droit sur un mob : il tombe amoureux"));
        ALIMENTS.put(COOKIE_BAIE, new Aliment("Cookie à la baie sucrée", TextColor.color(0xD2263B), Material.COOKIE,
                null, 6, 1.2f, "Les animaux craintifs ne te fuient plus"));
        ALIMENTS.put(COOKIE_LUMIERE, new Aliment("Cookie à la baie lumineuse", TextColor.color(0xF2B33D), Material.COOKIE,
                null, 2, 0.4f, "Tu éclaires autour de toi"));
        ALIMENTS.put(SANDWICH, new Aliment("Sandwich jambon-beurre", TextColor.color(0xE8C07A), Material.BREAD, null,
                15, 20.8f, null));
        ALIMENTS.put(BRUNCH, new Aliment("Brunch", TextColor.color(0xF0A060), Material.COOKED_PORKCHOP, null, 10, 7f,
                null));
        ALIMENTS.put(BLE, new Aliment("Blé soufflé", TextColor.color(0xE3C565), Material.DRIED_KELP, "wheat", 1, 1f,
                null));
        ALIMENTS.put(BOL, new Aliment("Bol de céréales", TextColor.color(0xF5EBD0), Material.PUMPKIN_PIE,
                "mushroom_stew", 10, 10f, "Remet à zéro ton temps sans dormir"));
    }

    private static KSCuisine instance;

    private Lang lang;
    private NamespacedKey cleObjet;
    private Amour amour;

    /** Recettes ajoutées : clé -> ingrédients, pour le livre de recettes. */
    private final Map<NamespacedKey, List<RecipeChoice>> recettes = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        lang = new Lang(this);
        cleObjet = new NamespacedKey(this, "aliment");
        recettes();

        Cookies cookies = new Cookies(this);
        amour = new Amour(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(cookies, this);
        getServer().getPluginManager().registerEvents(amour, this);
        KSEffets.declarer(Cookies.EFFET_APPRIVOISEUR, Component.text("Apprivoiseur", NamedTextColor.RED),
                objet(COOKIE_BAIE));
        KSEffets.declarer(Cookies.EFFET_LUMIERE, Component.text("Lumière", NamedTextColor.GOLD), objet(COOKIE_LUMIERE));
        KSEffets.declarer(Amour.EFFET_AMOUREUX, Component.text("Amoureux", NamedTextColor.LIGHT_PURPLE), objet(POMME));
        cookies.demarrer();
        amour.demarrer();
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        recettes.keySet().forEach(Bukkit::removeRecipe);
        instance = null;
    }

    static KSCuisine instance() {
        return instance;
    }

    Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    // ------------------------------------------------------------------ objets (autres plugins)

    /** Un aliment (ids : voir ids()), ou null si l'id est inconnu. */
    public static ItemStack objet(String id) {
        Aliment a = ALIMENTS.get(id);
        if (a == null) {
            return null;
        }
        ItemStack item = new ItemStack(a.base());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(a.nom(), a.couleur()).decoration(TextDecoration.ITALIC, false));
        if (a.description() != null) {
            meta.lore(List.of(Component.text(a.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        }
        if (id.equals(POMME)) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.getPersistentDataContainer().set(instance.cleObjet, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        if (a.image() != null) {
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", a.image()));
        }
        item.setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(a.nourriture()).saturation(a.saturation()));
        return item;
    }

    public static List<String> ids() {
        return List.copyOf(ALIMENTS.keySet());
    }

    /** Id de l'aliment, ou null si ce n'est pas un aliment du plugin. */
    public static String idObjet(ItemStack item) {
        if (item == null || !item.hasItemMeta() || instance == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(instance.cleObjet, PersistentDataType.STRING);
    }

    /** Vrai pour un bébé né d'une pomme d'amour qui n'a pas encore grandi (mini-mob ; KS_Decapitator). */
    public static boolean estBebe(org.bukkit.entity.Entity mob) {
        return Amour.estMini(mob);
    }

    // ------------------------------------------------------------------ manger

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onManger(PlayerItemConsumeEvent event) {
        if (BOL.equals(idObjet(event.getItem()))) {
            // Plus de phantoms : comme si le joueur venait de dormir. Les effets restent (choix de LeKiwi06).
            event.getPlayer().setStatistic(Statistic.TIME_SINCE_REST, 0);
        }
    }

    // ------------------------------------------------------------------ recettes

    private void recettes() {
        RecipeChoice pomme = RecipeChoice.exactChoice(new ItemStack(Material.APPLE));
        RecipeChoice sucre = new RecipeChoice.MaterialChoice(Material.SUGAR);
        NamespacedKey cle = new NamespacedKey(this, POMME);
        ShapedRecipe r = new ShapedRecipe(cle, objet(POMME));
        r.shape("SSS", "SPS", "SSS");
        r.setIngredient('S', sucre);
        r.setIngredient('P', pomme);
        ajouter(r, cle, List.of(pomme, sucre));

        RecipeChoice cookie = RecipeChoice.exactChoice(new ItemStack(Material.COOKIE));
        sansForme(COOKIE_BAIE, List.of(cookie, baies(Material.SWEET_BERRIES), baies(Material.SWEET_BERRIES),
                baies(Material.SWEET_BERRIES)));
        sansForme(COOKIE_LUMIERE, List.of(cookie, baies(Material.GLOW_BERRIES), baies(Material.GLOW_BERRIES),
                baies(Material.GLOW_BERRIES)));

        RecipeChoice lait = new RecipeChoice.MaterialChoice(Material.MILK_BUCKET);
        sansForme(SANDWICH, List.of(RecipeChoice.exactChoice(new ItemStack(Material.BREAD)),
                RecipeChoice.exactChoice(new ItemStack(Material.COOKED_PORKCHOP)), lait));
        List<ItemStack> oeufs = new ArrayList<>();
        for (Material m : List.of(Material.EGG, Material.BROWN_EGG, Material.BLUE_EGG)) {
            oeufs.add(new ItemStack(m));
        }
        sansForme(BRUNCH, List.of(RecipeChoice.exactChoice(new ItemStack(Material.PORKCHOP)),
                RecipeChoice.exactChoice(oeufs)));

        RecipeChoice ble = RecipeChoice.exactChoice(new ItemStack(Material.WHEAT));
        cle = new NamespacedKey(this, BLE);
        ajouter(new FurnaceRecipe(cle, objet(BLE), ble, 0.1f, 200), cle, List.of(ble));
        cle = new NamespacedKey(this, BLE + "_fumoir");
        ajouter(new SmokingRecipe(cle, objet(BLE), ble, 0.1f, 100), cle, List.of(ble));

        RecipeChoice souffle = RecipeChoice.exactChoice(objet(BLE));
        cle = new NamespacedKey(this, BOL);
        r = new ShapedRecipe(cle, objet(BOL));
        r.shape("BBB", "BLB", "BBB");
        r.setIngredient('B', souffle);
        r.setIngredient('L', lait);
        ajouter(r, cle, List.of(souffle, lait));
    }

    private static RecipeChoice baies(Material m) {
        return new RecipeChoice.MaterialChoice(m);
    }

    private void sansForme(String id, List<RecipeChoice> ingredients) {
        NamespacedKey cle = new NamespacedKey(this, id);
        ShapelessRecipe r = new ShapelessRecipe(cle, objet(id));
        ingredients.forEach(r::addIngredient);
        ajouter(r, cle, ingredients);
    }

    private void ajouter(Recipe recette, NamespacedKey cle, List<RecipeChoice> ingredients) {
        Bukkit.addRecipe(recette);
        recettes.put(cle, ingredients);
    }

    private String espace() {
        return getName().toLowerCase(Locale.ROOT);
    }

    private boolean notre(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().getKeys().stream()
                .anyMatch(c -> c.getNamespace().equals(espace()));
    }

    /**
     * Les aliments du plugin ne servent pas dans les recettes du jeu (pomme d'amour en pomme d'or, blé soufflé en bloc de
     * kelp...) ni dans les autres recettes, sauf le blé soufflé dans le bol de céréales.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreparer(PrepareItemCraftEvent event) {
        Recipe recette = event.getRecipe();
        if (recette == null || (recette instanceof Keyed k && k.getKey().getNamespace().equals(espace()))) {
            return;
        }
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (notre(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ pas d'aliment dans un alambic

    /** La pomme d'amour ne doit pas finir en manzana (KS_Alcools accepte toute pomme) : rien du plugin à l'alambic. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClicAlambic(InventoryClickEvent event) {
        Inventory haut = event.getView().getTopInventory();
        if (haut.getType() != InventoryType.BREWING || event.getClickedInventory() == null) {
            return;
        }
        boolean refus;
        if (event.getClickedInventory() == haut) {
            refus = notre(event.getCursor())
                    || (event.getClick() == ClickType.NUMBER_KEY
                    && notre(event.getWhoClicked().getInventory().getItem(event.getHotbarButton())))
                    || (event.getClick() == ClickType.SWAP_OFFHAND
                    && notre(event.getWhoClicked().getInventory().getItemInOffHand()));
        } else {
            refus = event.isShiftClick() && notre(event.getCurrentItem());
        }
        if (refus) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlisserAlambic(InventoryDragEvent event) {
        int taille = event.getView().getTopInventory().getSize();
        if (event.getView().getTopInventory().getType() == InventoryType.BREWING && notre(event.getOldCursor())
                && event.getRawSlots().stream().anyMatch(c -> c < taille)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntonnoirAlambic(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.BREWING && notre(event.getItem())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ livre de recettes

    private void debloquer(Player joueur, ItemStack obtenu) {
        if (obtenu == null || obtenu.getType().isAir()) {
            return;
        }
        List<NamespacedKey> nouvelles = new ArrayList<>();
        recettes.forEach((cle, ingredients) -> {
            if (!joueur.hasDiscoveredRecipe(cle) && ingredients.stream().anyMatch(choix -> choix.test(obtenu))) {
                nouvelles.add(cle);
            }
        });
        if (!nouvelles.isEmpty()) {
            joueur.discoverRecipes(nouvelles);
        }
    }

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
