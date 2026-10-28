package fr.kalium.atouts;

import fr.kalium.effets.KSEffets;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Atouts (cahier des charges : Atouts_Alcools_Aliments.md, LeKiwi06, 28/10/2026, partie 3.5) : serveur Kixster,
 * étape 4.
 *
 * - Craft : 8 ressources autour de l'équipement, à l'établi (pied de lapin autour de bottes : bottes de ski ; poisson-
 *   globe autour d'un casque : casque poiscaille ; crème de magma autour de bottes : bottes coupe-feu).
 * - Chaque craft ajoute 1 heure, décomptée seulement quand l'objet est porté, sans plafond. Un autre atout remplace le
 *   précédent (son temps est perdu).
 * - Pendant l'atout, l'objet prend l'apparence (le vrai type) de bottes en cuir, d'une tête de gardien ou de bottes en
 *   cuivre, en gardant ses caractéristiques ({@link Atouts}) ; fin de l'atout : il reprend son type.
 * - Effets : bottes de ski = marcher sur la poudreuse (le jeu le permet aux bottes en cuir) ; casque poiscaille =
 *   vision nocturne tant que la tête est sous l'eau ; bottes coupe-feu = aucun dégât des blocs de magma.
 * - Temps restant : dans la description de l'objet (mise à jour chaque minute) et dans /effects (KS_Effets).
 */
public final class KSAtouts extends JavaPlugin implements Listener {

    /** Vision nocturne du casque poiscaille : 15 s renouvelées chaque seconde (pas de clignotement sous 10 s). */
    private static final int VISION = 15 * 20;

    private Lang lang;

    /** Recettes : clé -> atout. */
    private final Map<NamespacedKey, Atouts.Atout> recettes = new LinkedHashMap<>();
    /** Secondes portées depuis la dernière écriture dans l'objet, par joueur et emplacement. */
    private final Map<UUID, int[]> portees = new HashMap<>();
    /** Joueurs à qui le casque poiscaille donne la vision nocturne. */
    private final Set<UUID> voyants = new HashSet<>();

    @Override
    public void onEnable() {
        lang = new Lang(this);
        Atouts.init(this);
        recettes();
        getServer().getPluginManager().registerEvents(this, this);
        for (Atouts.Atout a : Atouts.ATOUTS.values()) {
            String id = "ks_atouts:" + a.id();
            KSEffets.declarer(id, Component.text(a.nom(), NamedTextColor.AQUA), new ItemStack(a.apparence()));
            KSEffets.fournisseur(id, joueur -> restantPorte(joueur, a));
        }
        Bukkit.getScheduler().runTaskTimer(this, this::seconde, 20L, 20L);
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::ecrire);
        recettes.keySet().forEach(Bukkit::removeRecipe);
    }

    Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    // ------------------------------------------------------------------ recettes

    private void recettes() {
        List<Material> bottes = new ArrayList<>(Tag.ITEMS_FOOT_ARMOR.getValues());
        List<Material> casques = new ArrayList<>(Tag.ITEMS_HEAD_ARMOR.getValues());
        // Un casque déjà sous atout est une tête : acceptée, puis vérifiée par ses marques.
        casques.add(Material.PLAYER_HEAD);
        for (Atouts.Atout a : Atouts.ATOUTS.values()) {
            NamespacedKey cle = new NamespacedKey(this, a.id());
            ShapedRecipe r = new ShapedRecipe(cle, apercu(a));
            r.shape("RRR", "RER", "RRR");
            r.setIngredient('R', new RecipeChoice.MaterialChoice(a.ressource()));
            r.setIngredient('E', new RecipeChoice.MaterialChoice(a.emplacement() == EquipmentSlot.FEET ? bottes : casques));
            Bukkit.addRecipe(r);
            recettes.put(cle, a);
        }
    }

    /** Résultat affiché dans le livre de recettes (le vrai résultat est calculé sur l'équipement posé). */
    private static ItemStack apercu(Atouts.Atout a) {
        ItemStack item = new ItemStack(a.apparence());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(a.nom(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Ajoute 1 h d'atout à ton équipement", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /** Résultat réel : l'équipement du centre avec l'atout (rien si ce n'est pas une pièce acceptée). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreparer(PrepareItemCraftEvent event) {
        Recipe recette = event.getRecipe();
        if (!(recette instanceof ShapedRecipe forme) || !recettes.containsKey(forme.getKey())) {
            return;
        }
        Atouts.Atout a = recettes.get(forme.getKey());
        CraftingInventory grille = event.getInventory();
        ItemStack[] cases = grille.getMatrix();
        ItemStack centre = cases.length == 9 ? cases[4] : null;
        grille.setResult(Atouts.accepte(centre, a) ? Atouts.poser(centre, a) : null);
    }

    // ------------------------------------------------------------------ décompte et effets

    /** Pièce portée à l'emplacement de l'atout, si elle porte cet atout. */
    private static ItemStack porte(Player joueur, Atouts.Atout a) {
        PlayerInventory inv = joueur.getInventory();
        ItemStack item = a.emplacement() == EquipmentSlot.FEET ? inv.getBoots() : inv.getHelmet();
        return Atouts.atout(item) == a ? item : null;
    }

    private int restantPorte(Player joueur, Atouts.Atout a) {
        ItemStack item = porte(joueur, a);
        if (item == null) {
            return 0;
        }
        int[] s = portees.get(joueur.getUniqueId());
        int deja = s == null ? 0 : s[a.emplacement() == EquipmentSlot.FEET ? 0 : 1];
        return Math.max(1, Atouts.restant(item) - deja);
    }

    /** Chaque seconde : temps porté (écrit dans l'objet chaque minute), vision nocturne du casque poiscaille. */
    private void seconde() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            int[] s = portees.computeIfAbsent(joueur.getUniqueId(), u -> new int[2]);
            PlayerInventory inv = joueur.getInventory();
            compter(joueur, inv.getBoots(), s, 0, EquipmentSlot.FEET);
            compter(joueur, inv.getHelmet(), s, 1, EquipmentSlot.HEAD);
            boolean vision = porte(joueur, Atouts.ATOUTS.get(Atouts.POISCAILLE)) != null && joueur.isUnderWater();
            if (vision) {
                joueur.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, VISION, 0, true, false, true));
                voyants.add(joueur.getUniqueId());
            } else if (voyants.remove(joueur.getUniqueId())) {
                PotionEffect actuelle = joueur.getPotionEffect(PotionEffectType.NIGHT_VISION);
                if (actuelle != null && actuelle.getDuration() <= VISION) {
                    joueur.removePotionEffect(PotionEffectType.NIGHT_VISION);
                }
            }
        }
    }

    private void compter(Player joueur, ItemStack item, int[] s, int i, EquipmentSlot emplacement) {
        Atouts.Atout a = Atouts.atout(item);
        if (a == null || a.emplacement() != emplacement) {
            s[i] = 0;
            return;
        }
        if (++s[i] < 60 && s[i] < Atouts.restant(item)) {
            return;
        }
        int secondes = s[i];
        s[i] = 0;
        // L'objet porté est modifié sur place (pas de nouveau son d'équipement).
        if (!Atouts.decompter(item, secondes)) {
            ItemStack fini = Atouts.retirer(item);
            if (emplacement == EquipmentSlot.FEET) {
                joueur.getInventory().setBoots(fini);
            } else {
                joueur.getInventory().setHelmet(fini);
            }
            joueur.sendActionBar(t("atout.fin", "<gray>L'atout <aqua><nom></aqua> est terminé.", "nom", a.nom()));
        }
    }

    /** Écrit dans les objets portés le temps compté depuis la dernière minute (déconnexion, arrêt). */
    private void ecrire(Player joueur) {
        int[] s = portees.remove(joueur.getUniqueId());
        if (s == null) {
            return;
        }
        PlayerInventory inv = joueur.getInventory();
        if (s[0] > 0 && Atouts.atout(inv.getBoots()) != null && !Atouts.decompter(inv.getBoots(), s[0])) {
            inv.setBoots(Atouts.retirer(inv.getBoots()));
        }
        if (s[1] > 0 && Atouts.atout(inv.getHelmet()) != null && !Atouts.decompter(inv.getHelmet(), s[1])) {
            inv.setHelmet(Atouts.retirer(inv.getHelmet()));
        }
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        ecrire(event.getPlayer());
        voyants.remove(event.getPlayer().getUniqueId());
    }

    /** Bottes coupe-feu : aucun dégât des blocs de magma. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMagma(EntityDamageEvent event) {
        if (event.getDamageSource().getDamageType() == org.bukkit.damage.DamageType.HOT_FLOOR
                && event.getEntity() instanceof Player joueur
                && porte(joueur, Atouts.ATOUTS.get(Atouts.COUPE_FEU)) != null) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ livre de recettes

    private void debloquer(Player joueur, ItemStack obtenu) {
        if (obtenu == null || obtenu.getType().isAir()) {
            return;
        }
        recettes.forEach((cle, a) -> {
            if (obtenu.getType() == a.ressource() && !joueur.hasDiscoveredRecipe(cle)) {
                joueur.discoverRecipe(cle);
            }
        });
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
