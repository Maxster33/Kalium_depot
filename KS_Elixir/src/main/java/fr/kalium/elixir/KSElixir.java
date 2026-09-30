package fr.kalium.elixir;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KS_Elixir (cahier des charges : catégorie 1 « Contenu survie », LeKiwi06, 29/09/2026) : élixirs qui condensent 8
 * potions en une seule bouteille.
 *
 * - Recette en anneau : 8 potions (à boire, version allongée, niveau I) autour d'un ingrédient rare au centre.
 * - Élixir : potion à boire, un seul effet, particules masquées, aspect enchanté, couleur de la potion vanilla de
 *   l'effet (Super Gâteau : rose), nom coloré non italique, marqué (id).
 * - Livre de recettes : une recette est débloquée dès que le joueur obtient un de ses ingrédients.
 * - Un élixir ne peut pas être mis dans un alambic (choix de LeKiwi06 : pas d'élixir jetable ni persistant).
 *
 * Élixir du Fantôme : la fiole de 10 niveaux de KS_FioleExp (recette ignorée sans ce plugin). Élixir de Fortune : le
 * bloc d'émeraude compressé tier 3 de KS_Economy (pas encore créé : recette ignorée, l'élixir existe déjà pour
 * /kaliumgive). Autres plugins : creerElixir(id), idElixir(objet), ids() (KS_KaliumGive).
 */
public final class KSElixir extends JavaPlugin implements Listener {

    /** Un élixir : effet, niveau (0 = I), durée en ticks, couleur de la bouteille (null : celle de l'effet). */
    private record Elixir(String nom, TextColor couleurNom, PotionEffectType effet, int niveau, int duree,
                          Color couleur) {
    }

    private static final int MINUTE = 20 * 60;

    /** id -> élixir, dans l'ordre du cahier des charges. */
    private static final Map<String, Elixir> ELIXIRS = new LinkedHashMap<>();

    static {
        ELIXIRS.put("super_gateau", new Elixir("Super Gâteau", TextColor.fromHexString("#FF69B4"),
                PotionEffectType.SATURATION, 0, 30 * MINUTE, Color.fromRGB(16738740)));
        ELIXIRS.put("chauve_souris", new Elixir("Élixir de chauve-souris", NamedTextColor.DARK_BLUE,
                PotionEffectType.NIGHT_VISION, 0, 60 * MINUTE, null));
        ELIXIRS.put("anguille", new Elixir("Élixir d'anguille", TextColor.fromHexString("#87CEEB"),
                PotionEffectType.WATER_BREATHING, 0, 60 * MINUTE, null));
        ELIXIRS.put("ignifugation", new Elixir("Élixir d'ignifugation", NamedTextColor.GOLD,
                PotionEffectType.FIRE_RESISTANCE, 0, 60 * MINUTE, null));
        ELIXIRS.put("plume", new Elixir("Élixir de Plume", NamedTextColor.LIGHT_PURPLE,
                PotionEffectType.SLOW_FALLING, 0, 30 * MINUTE, null));
        ELIXIRS.put("phenix", new Elixir("Élixir de Phénix", NamedTextColor.RED,
                PotionEffectType.REGENERATION, 0, 10 * MINUTE, null));
        ELIXIRS.put("fantome", new Elixir("Élixir du Fantôme", NamedTextColor.GRAY,
                PotionEffectType.INVISIBILITY, 0, 60 * MINUTE, null));
        ELIXIRS.put("titan", new Elixir("Élixir de Titan", NamedTextColor.GOLD,
                PotionEffectType.STRENGTH, 0, 60 * MINUTE, null));
        ELIXIRS.put("vent", new Elixir("Élixir du Vent", NamedTextColor.AQUA,
                PotionEffectType.SPEED, 0, 60 * MINUTE, null));
        // Saut amélioré II : seule exception à « pas de niveau 2 » avec la Chance III.
        ELIXIRS.put("rebond", new Elixir("Élixir de Rebond", NamedTextColor.GREEN,
                PotionEffectType.JUMP_BOOST, 1, 60 * MINUTE, null));
        ELIXIRS.put("fortune", new Elixir("Élixir de Fortune", NamedTextColor.DARK_GREEN,
                PotionEffectType.LUCK, 2, 10 * MINUTE, null));
    }

    private static NamespacedKey marqueur;

    /** Recettes ajoutées : clé -> ingrédients (anneau, centre), pour le livre de recettes. */
    private final Map<NamespacedKey, List<RecipeChoice>> recettes = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "elixir");
        getServer().getPluginManager().registerEvents(this, this);

        anneau("super_gateau", new RecipeChoice.MaterialChoice(Material.CAKE),
                potions(PotionType.HEALING, PotionType.STRONG_HEALING));
        anneau("chauve_souris", potions(PotionType.LONG_NIGHT_VISION), new RecipeChoice.MaterialChoice(Material.ECHO_SHARD));
        anneau("anguille", potions(PotionType.LONG_WATER_BREATHING),
                new RecipeChoice.MaterialChoice(Material.NAUTILUS_SHELL));
        anneau("ignifugation", potions(PotionType.LONG_FIRE_RESISTANCE), new RecipeChoice.MaterialChoice(Material.BLUE_ICE));
        anneau("plume", potions(PotionType.LONG_SLOW_FALLING), new RecipeChoice.MaterialChoice(Material.SHULKER_SHELL));
        anneau("phenix", potions(PotionType.LONG_REGENERATION), new RecipeChoice.MaterialChoice(Material.RESPAWN_ANCHOR));
        if (getServer().getPluginManager().isPluginEnabled("KS_FioleExp")) {
            anneau("fantome", potions(PotionType.LONG_INVISIBILITY),
                    new RecipeChoice.ExactChoice(fr.kalium.fioleexp.KSFioleExp.creerFioleNiveaux(10)));
        } else {
            getLogger().warning("KS_FioleExp absent : recette de l'Élixir du Fantôme ignorée.");
        }
        anneau("titan", potions(PotionType.LONG_STRENGTH), new RecipeChoice.MaterialChoice(Material.GOLD_BLOCK));
        anneau("vent", potions(PotionType.LONG_SWIFTNESS), new RecipeChoice.MaterialChoice(
                Arrays.stream(Material.values()).filter(m -> !m.isLegacy() && m.name().endsWith("_HARNESS")).toList()));
        anneau("rebond", potions(PotionType.LONG_LEAPING), new RecipeChoice.MaterialChoice(Material.SLIME_BLOCK));
        getLogger().warning("KS_Economy absent (bloc d'émeraude compressé tier 3) : recette de l'Élixir de Fortune ignorée.");

        getLogger().info(recettes.size() + " recettes d'élixirs ajoutées.");
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
    }

    @Override
    public void onDisable() {
        recettes.keySet().forEach(Bukkit::removeRecipe);
    }

    // ------------------------------------------------------------------ objets (autres plugins)

    /** Un élixir (id : voir ids()), ou null si l'id est inconnu. Nécessite que le plugin soit activé. */
    public static ItemStack creerElixir(String id) {
        Elixir elixir = ELIXIRS.get(id);
        if (elixir == null) {
            return null;
        }
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(elixir.effet(), elixir.duree(), elixir.niveau(), false, false, true), true);
        meta.setColor(elixir.couleur() != null ? elixir.couleur() : elixir.effet().getColor());
        meta.setEnchantmentGlintOverride(true);
        meta.displayName(Component.text(elixir.nom(), elixir.couleurNom()).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    /** Id de l'élixir, ou null si ce n'est pas un élixir du plugin. */
    public static String idElixir(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(marqueur, PersistentDataType.STRING);
    }

    /** Ids de tous les élixirs. */
    public static List<String> ids() {
        return List.copyOf(ELIXIRS.keySet());
    }

    // ------------------------------------------------------------------ recettes

    /** Potions à boire vanilla acceptées (ex. vision nocturne allongée). */
    private static RecipeChoice potions(PotionType... types) {
        List<ItemStack> potions = new ArrayList<>();
        for (PotionType type : types) {
            ItemStack potion = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            meta.setBasePotionType(type);
            potion.setItemMeta(meta);
            potions.add(potion);
        }
        return new RecipeChoice.ExactChoice(potions);
    }

    /** 8 ingrédients en anneau autour du centre. */
    private void anneau(String id, RecipeChoice autour, RecipeChoice centre) {
        NamespacedKey cle = new NamespacedKey(this, id);
        ShapedRecipe recette = new ShapedRecipe(cle, creerElixir(id));
        recette.shape("AAA", "ACA", "AAA");
        recette.setIngredient('A', autour);
        recette.setIngredient('C', centre);
        Bukkit.addRecipe(recette);
        recettes.put(cle, List.of(autour, centre));
    }

    // ------------------------------------------------------------------ livre de recettes

    private void debloquer(Player joueur, ItemStack obtenu) {
        if (obtenu == null || obtenu.getType().isAir()) {
            return;
        }
        recettes.forEach((cle, ingredients) -> {
            if (!joueur.hasDiscoveredRecipe(cle) && ingredients.stream().anyMatch(choix -> choix.test(obtenu))) {
                joueur.discoverRecipe(cle);
            }
        });
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

    // ------------------------------------------------------------------ pas d'élixir dans un alambic

    private static boolean estElixir(ItemStack item) {
        return idElixir(item) != null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory haut = event.getView().getTopInventory();
        if (haut.getType() != InventoryType.BREWING || event.getClickedInventory() == null) {
            return;
        }
        boolean refus;
        if (event.getClickedInventory() == haut) {
            refus = estElixir(event.getCursor())
                    || (event.getClick() == ClickType.NUMBER_KEY
                    && estElixir(event.getWhoClicked().getInventory().getItem(event.getHotbarButton())))
                    || (event.getClick() == ClickType.SWAP_OFFHAND
                    && estElixir(event.getWhoClicked().getInventory().getItemInOffHand()));
        } else {
            refus = event.isShiftClick() && estElixir(event.getCurrentItem());
        }
        if (refus) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        int tailleHaut = event.getView().getTopInventory().getSize();
        if (event.getView().getTopInventory().getType() == InventoryType.BREWING && estElixir(event.getOldCursor())
                && event.getRawSlots().stream().anyMatch(caseBrute -> caseBrute < tailleHaut)) {
            event.setCancelled(true);
        }
    }

    /** Entonnoirs. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.BREWING && estElixir(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Filet de sécurité : un élixir déjà dans un alambic n'est jamais transformé. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        for (int i = 0; i < 3; i++) {
            if (estElixir(event.getContents().getItem(i))) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
