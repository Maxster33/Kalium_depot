package fr.kalium.hyperboost;

import fr.kalium.menu.api.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Fireworks;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_Hyperboost (demande de LeKiwi06, 01-02/11/2026, cahier local Blocs_Cultures_Objets.md, partie 5) : la fusée
 * Hyperboost.
 *
 * - Recette : 1 TNT au centre, 8 fusées de feu d'artifice de niveau 3 autour (niveau vérifié à la préparation ;
 *   recette refusée à l'automate de craft, qui ne permet pas cette vérification).
 * - Utilisation (clic droit) : seulement en conduisant une monture (cheval, âne, mule, cheval squelette ou zombie,
 *   chameau, chameau momifié, happy ghast) ; pas en élytres. Consommée. Pendant 10 s, la monture va à 40 blocs/s
 *   au plus (Poussee : calcul d'après le code du jeu 26.3).
 * - La vitesse est montée par des modificateurs d'attributs temporaires (jamais enregistrés avec l'entité), retirés
 *   à la fin, quand le joueur descend ou se déconnecte, et à l'arrêt du plugin : la monture revient exactement à
 *   ses attributs d'avant, même après un arrêt brutal du serveur.
 * - Anti-triche : métadonnée « ks_hyperboost » (date de fin de la poussée + marge, en ms) lue par KS_AntiCheat, et
 *   permissions grim.nosetback et grim.nosetback.<vérification> pendant la même durée (GrimAC renvoie le joueur en
 *   arrière et éjecte alors les passagers ; KS_AntiCheat 1.5.0 annule aussi ses signalements).
 * - Base de l'objet : étoile de feu d'artifice (ni posée, ni lancée, ni chargée dans une arbalète) avec l'apparence
 *   d'une fusée (item_model) et l'aspect enchanté ; apparence Bedrock : geyser-bedrock.
 *
 * Autres plugins : creerHyperboost(), estHyperboost(objet).
 */
public final class KSHyperboost extends JavaPlugin implements Listener {

    private static NamespacedKey marqueur;
    private NamespacedKey cleRecette;
    private Lang lang;
    private final Map<UUID, Poussee> poussees = new HashMap<>();

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "hyperboost");
        cleRecette = new NamespacedKey(this, "fusee_hyperboost");
        lang = new Lang(this);
        getServer().getPluginManager().registerEvents(this, this);

        ShapedRecipe recette = new ShapedRecipe(cleRecette, creerHyperboost());
        recette.shape("FFF", "FTF", "FFF");
        recette.setIngredient('F', new RecipeChoice.MaterialChoice(Material.FIREWORK_ROCKET));
        recette.setIngredient('T', Material.TNT);
        Bukkit.addRecipe(recette);
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        for (Poussee p : new ArrayList<>(poussees.values())) {
            p.arreter(false);
        }
        poussees.clear();
        Bukkit.removeRecipe(cleRecette);
    }

    Lang lang() {
        return lang;
    }

    void oublier(UUID joueur) {
        poussees.remove(joueur);
    }

    // ------------------------------------------------------------------ objet (autres plugins)

    /** Une fusée Hyperboost. Nécessite que le plugin soit activé. */
    public static ItemStack creerHyperboost() {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        KSHyperboost plugin = JavaPlugin.getPlugin(KSHyperboost.class);
        meta.displayName(plugin.lang.c("objet.nom", "<light_purple>Fusée Hyperboost")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                plugin.lang.c("objet.description-1", "<gray>Sur une monture : clic droit").decoration(TextDecoration.ITALIC, false),
                plugin.lang.c("objet.description-2", "<gray>40 blocs/s pendant 10 s").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "firework_rocket"));
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 16);
        return item;
    }

    /** Vrai si l'objet est une fusée Hyperboost du plugin. */
    public static boolean estHyperboost(ItemStack item) {
        return item != null && marqueur != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(marqueur, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------------ utilisation

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!estHyperboost(item)) {
            return;
        }
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        Entity monture = joueur.getVehicle();
        if (joueur.isGliding() || monture == null || !estMonture(monture)) {
            joueur.sendActionBar(lang.c("refus.pas-de-monture", "<red>À utiliser en conduisant une monture"));
            lang.saveIfNeeded();
            return;
        }
        if (monture.getPassengers().isEmpty() || !monture.getPassengers().get(0).equals(joueur)) {
            joueur.sendActionBar(lang.c("refus.pas-conducteur", "<red>Seul le conducteur peut l'utiliser"));
            lang.saveIfNeeded();
            return;
        }
        if (monture instanceof AbstractHorse cheval && (!cheval.isTamed() || estVide(cheval.getInventory().getSaddle()))) {
            joueur.sendActionBar(lang.c("refus.sans-selle", "<red>Ta monture doit être sellée"));
            lang.saveIfNeeded();
            return;
        }
        if (poussees.containsKey(joueur.getUniqueId())) {
            joueur.sendActionBar(lang.c("refus.deja", "<yellow>Hyperboost déjà en cours"));
            lang.saveIfNeeded();
            return;
        }
        // Un seul objet consommé, dans la main qui l'a utilisé.
        EquipmentSlot main = event.getHand() == null ? EquipmentSlot.HAND : event.getHand();
        ItemStack tenu = joueur.getInventory().getItem(main);
        if (!estHyperboost(tenu)) {
            return;
        }
        if (joueur.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            tenu.setAmount(tenu.getAmount() - 1);
            joueur.getInventory().setItem(main, tenu.getAmount() <= 0 ? null : tenu);
        }
        Poussee poussee = new Poussee(this, joueur, (org.bukkit.entity.LivingEntity) monture);
        poussees.put(joueur.getUniqueId(), poussee);
        poussee.demarrer();
    }

    /** Montures conduites par le joueur dont on sait calculer la vitesse (lamas exclus : ils ne se dirigent pas). */
    static boolean estMonture(Entity e) {
        return (e instanceof AbstractHorse && !(e instanceof Llama)) || e instanceof HappyGhast;
    }

    private static boolean estVide(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        Poussee p = poussees.get(event.getEntity().getUniqueId());
        if (p != null) {
            p.arreter(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Poussee p = poussees.get(event.getPlayer().getUniqueId());
        if (p != null) {
            p.arreter(false);
        }
    }

    // ------------------------------------------------------------------ objet protégé

    /** Pas de lancement par un distributeur (l'étoile serait simplement éjectée). */
    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (estHyperboost(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * Établi : la recette n'accepte que des fusées de niveau 3 ; la fusée Hyperboost ne sert d'ingrédient à aucune
     * autre recette (une étoile de feu d'artifice entre dans les fusées vanilla).
     */
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        Recipe recette = event.getRecipe();
        boolean notre = recette instanceof ShapedRecipe s && s.getKey().equals(cleRecette);
        for (ItemStack ingredient : inv.getMatrix()) {
            if (estVide(ingredient)) {
                continue;
            }
            if (estHyperboost(ingredient) || (notre && ingredient.getType() == Material.FIREWORK_ROCKET
                    && niveau(ingredient) != 3)) {
                inv.setResult(null);
                return;
            }
        }
    }

    /** Automate de craft : il ne passe pas par la vérification du niveau des fusées. */
    @EventHandler(ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (event.getRecipe() instanceof ShapedRecipe s && s.getKey().equals(cleRecette)) {
            event.setCancelled(true);
            return;
        }
        if (event.getBlock().getState() instanceof org.bukkit.block.Crafter crafter) {
            for (ItemStack ingredient : crafter.getInventory().getContents()) {
                if (estHyperboost(ingredient)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private static int niveau(ItemStack fusee) {
        Fireworks f = fusee.getData(DataComponentTypes.FIREWORKS);
        return f == null ? 1 : f.flightDuration();
    }

    // ------------------------------------------------------------------ livre de recettes

    private void debloquer(Player joueur, ItemStack obtenu) {
        if (obtenu != null && (obtenu.getType() == Material.TNT || obtenu.getType() == Material.FIREWORK_ROCKET)
                && !joueur.hasDiscoveredRecipe(cleRecette)) {
            joueur.discoverRecipe(cleRecette);
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
