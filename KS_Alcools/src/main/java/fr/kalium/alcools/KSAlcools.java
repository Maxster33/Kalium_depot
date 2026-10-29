package fr.kalium.alcools;

import fr.kalium.effets.KSEffets;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_Alcools (cahier des charges : Atouts_Alcools_Aliments.md, LeKiwi06, 28/10/2026, parties 3.2 et 3.3) : serveur
 * Kixster, étape 2 (partie sans risque).
 *
 * - Alcools à l'alambic ({@link Alcools}) : alcool neutre, tequila, vodka, hydromel, manzana, bière, tabascoolisé ;
 *   effets et nausée ({@link Boire}) ; effets custom tenus par KS_Effets.
 * - Alcool neutre comme combustible des fours, fumoirs et hauts fourneaux ({@link Carburant}) ; trains : KS_Trains.
 * - Cocktail Molotov ({@link Molotov}), bandage, torche augmentée ({@link Torches}), à l'établi.
 * - 1.1.0 : rhum ({@link Rhum}) : musique d'un disque autour du buveur (étape 5). Le Shéjiǔ viendra plus tard (étape 6).
 *
 * Autres plugins : alcool(id, variante, jetable), ids(), idAlcool(objet), estAlcoolNeutre(objet), creerMolotov(),
 * creerBandage(), creerTorche(nombre).
 */
public final class KSAlcools extends JavaPlugin implements Listener {

    private static KSAlcools instance;

    private Lang lang;
    private NamespacedKey cleMolotov;
    private NamespacedKey cleBandage;
    private NamespacedKey cleTorche;
    private boolean scs;
    private Rhum rhum;

    /** Recettes ajoutées : clé -> ingrédients, pour le livre de recettes. */
    private final Map<NamespacedKey, List<RecipeChoice>> recettes = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        lang = new Lang(this);
        Alcools.init(this);
        cleMolotov = new NamespacedKey(this, "molotov");
        cleBandage = new NamespacedKey(this, "bandage");
        cleTorche = new NamespacedKey(this, "torche_augmentee");
        scs = getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem");
        if (!scs) {
            getLogger().warning("SimpleClaimSystem absent : Molotov et torche augmentée agissent aussi dans les claims.");
        }

        Alcools.melanges(this);
        recettes();
        effets();

        Boire boire = new Boire(this);
        Carburant carburant = new Carburant(this);
        Torches torches = new Torches(this);
        rhum = new Rhum(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(boire, this);
        getServer().getPluginManager().registerEvents(carburant, this);
        getServer().getPluginManager().registerEvents(new Molotov(this), this);
        getServer().getPluginManager().registerEvents(torches, this);
        getServer().getPluginManager().registerEvents(rhum, this);
        boire.demarrer();
        carburant.demarrer();
        torches.demarrer();
        rhum.demarrer();
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        if (rhum != null) {
            rhum.toutArreter();
        }
        Alcools.retirerMelanges();
        recettes.keySet().forEach(Bukkit::removeRecipe);
        instance = null;
    }

    static KSAlcools instance() {
        return instance;
    }

    Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    /** Vrai si le chunk est dans le claim d'un autre joueur (SimpleClaimSystem ; joueur null : tout claim). */
    boolean claimRefuse(Chunk chunk, UUID joueur) {
        return scs && ProtectionClaims.refuse(chunk, joueur);
    }

    // ------------------------------------------------------------------ effets custom (KS_Effets)

    private void effets() {
        KSEffets.declarer(Boire.EFFET_VODKA, Component.text("Vodka", NamedTextColor.AQUA),
                Alcools.creer(Alcools.VODKA, Alcools.Variante.NORMAL, false));
        KSEffets.declarer(Boire.EFFET_VODKA_2, Component.text("Vodka II", NamedTextColor.AQUA),
                Alcools.creer(Alcools.VODKA, Alcools.Variante.FORT, false));
        KSEffets.declarer(Boire.EFFET_TABASCO, Component.text("Tabascoolisé", NamedTextColor.RED),
                Alcools.creer(Alcools.TABASCOOLISE, Alcools.Variante.NORMAL, false));
        KSEffets.declarer(Rhum.EFFET_RHUM, Component.text("Rhum", NamedTextColor.GOLD),
                Alcools.creer(Alcools.RHUM, Alcools.Variante.NORMAL, false));
    }

    // ------------------------------------------------------------------ objets (autres plugins)

    /** Un alcool (ids : voir ids()), ou null si la combinaison n'existe pas. Variante : normal, long, fort. */
    public static ItemStack alcool(String id, String variante, boolean jetable) {
        return Alcools.creer(id, Alcools.Variante.de(variante), jetable);
    }

    /** Ids de tous les alcools. */
    public static List<String> ids() {
        return List.copyOf(Alcools.ALCOOLS.keySet());
    }

    /** Id de l'alcool, ou null si ce n'est pas un alcool du plugin. */
    public static String idAlcool(ItemStack item) {
        return Alcools.id(item);
    }

    /** Vrai pour l'alcool neutre à boire (le combustible). */
    public static boolean estAlcoolNeutre(ItemStack item) {
        return Alcools.estNeutre(item);
    }

    public static ItemStack creerMolotov() {
        return Molotov.creer(instance.cleMolotov);
    }

    /**
     * Bandage : se mange en 0,8 s (base : kelp séché, même vitesse sur Java et Bedrock ; image du papier), même
     * rassasié, ne nourrit pas, rend 3 points de vie. Pile de 64.
     */
    public static ItemStack creerBandage() {
        ItemStack item = new ItemStack(Material.DRIED_KELP);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Bandage", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Rend 1,5 cœur", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(instance.cleBandage, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "paper"));
        item.setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(0).saturation(0f).canAlwaysEat(true));
        item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(0.8f)
                .animation(ItemUseAnimation.EAT));
        return item;
    }

    /** Torches augmentées (le craft en donne 4). */
    public static ItemStack creerTorche(int nombre) {
        ItemStack item = new ItemStack(Material.TORCH, nombre);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Torche augmentée", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Éclaire sur 15 blocs autour d'elle", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(instance.cleTorche, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean estMolotov(ItemStack item) {
        return marque(item, cleMolotov);
    }

    boolean estBandage(ItemStack item) {
        return marque(item, cleBandage);
    }

    boolean estTorche(ItemStack item) {
        return marque(item, cleTorche);
    }

    private static boolean marque(ItemStack item, NamespacedKey cle) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cle, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------------ bandage

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onManger(PlayerItemConsumeEvent event) {
        if (!estBandage(event.getItem())) {
            return;
        }
        Player joueur = event.getPlayer();
        double max = joueur.getAttribute(Attribute.MAX_HEALTH).getValue();
        joueur.setHealth(Math.min(max, joueur.getHealth() + 3));
    }

    // ------------------------------------------------------------------ recettes

    private void recettes() {
        RecipeChoice neutre = RecipeChoice.exactChoice(Alcools.creer(Alcools.NEUTRE, Alcools.Variante.NORMAL, false));
        RecipeChoice papier = new RecipeChoice.MaterialChoice(Material.PAPER);

        NamespacedKey cle = new NamespacedKey(this, "molotov");
        ShapelessRecipe molotov = new ShapelessRecipe(cle, creerMolotov());
        molotov.addIngredient(neutre);
        molotov.addIngredient(papier);
        ajouter(molotov, cle, List.of(neutre, papier));

        cle = new NamespacedKey(this, "bandage");
        ShapedRecipe bandage = new ShapedRecipe(cle, creerBandage());
        bandage.shape("PPP", "PAP", "PPP");
        bandage.setIngredient('P', papier);
        bandage.setIngredient('A', neutre);
        ajouter(bandage, cle, List.of(neutre, papier));

        cle = new NamespacedKey(this, "torche_augmentee");
        RecipeChoice torche = new RecipeChoice.MaterialChoice(Material.TORCH);
        ShapelessRecipe torches = new ShapelessRecipe(cle, creerTorche(4));
        torches.addIngredient(torche);
        torches.addIngredient(papier);
        torches.addIngredient(neutre);
        ajouter(torches, cle, List.of(neutre, papier, torche));
    }

    private void ajouter(Recipe recette, NamespacedKey cle, List<RecipeChoice> ingredients) {
        Bukkit.addRecipe(recette);
        recettes.put(cle, ingredients);
    }

    /**
     * Les objets du plugin (alcools, bandage, torche augmentée...) ne servent pas dans les recettes du jeu : un bandage
     * (kelp séché) ne fait pas de bloc de kelp, une torche augmentée pas de lanterne.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreparer(PrepareItemCraftEvent event) {
        Recipe recette = event.getRecipe();
        if (recette == null || (recette instanceof Keyed k && k.getKey().getNamespace().equals(getNamespace()))) {
            return;
        }
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().getKeys().stream()
                    .anyMatch(c -> c.getNamespace().equals(getNamespace()))) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    private String getNamespace() {
        return getName().toLowerCase(java.util.Locale.ROOT);
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
