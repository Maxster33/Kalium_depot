package fr.kalium.atouts;

import fr.kalium.effets.KSEffets;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
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
import org.bukkit.util.BoundingBox;

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
 *   globe autour d'un casque : casque poiscaille ; crème de magma autour de bottes : bottes coupe-feu ; bloc de miel
 *   autour d'un casque : casque ventouse, ajouté le 29/10).
 * - Chaque craft ajoute 1 heure, décomptée seulement quand l'objet est porté, sans plafond. Un autre atout remplace le
 *   précédent (son temps est perdu).
 * - Pendant l'atout, l'objet prend l'apparence (le vrai type) de bottes en cuir, d'une tête de gardien, de bottes en
 *   cuivre ou d'un bloc de miel, en gardant ses caractéristiques ({@link Atouts}) ; fin de l'atout : il reprend son
 *   type. Un objet sous atout ne se pose pas comme un bloc et ne sert dans aucun autre craft.
 * - Effets : bottes de ski = marcher sur la poudreuse (le jeu le permet aux bottes en cuir) ; casque poiscaille =
 *   vision nocturne tant que la tête est sous l'eau ; bottes coupe-feu = aucun dégât des blocs de magma ; casque
 *   ventouse = la tête qui touche un plafond (bloc solide, hors de l'eau et de la lave, pas en sneakant) y reste
 *   collée par une lévitation invisible ; le joueur glisse au plafond, retombe en sneakant ou en sortant de dessous.
 * - Temps restant : dans la description de l'objet (mise à jour chaque minute) et dans /effects (KS_Effets).
 * - 1.2.0 (maléfices du Nether, cahier Malefices_Nether.md, LeKiwi06, 30/10/2026) : plastron anti-radiation, 8 blocs
 *   d'or autour d'un plastron ; apparence plastron en or ; 20 minutes par craft, décomptées seulement quand il est
 *   porté dans le Nether ; KS_MaleficesNether divise alors par 2 la durée de ses maléfices ({@link #porteAtout}).
 */
public final class KSAtouts extends JavaPlugin implements Listener {

    /** Vision nocturne du casque poiscaille : 15 s renouvelées chaque seconde (pas de clignotement sous 10 s). */
    private static final int VISION = 15 * 20;
    /** Lévitation du casque ventouse : 15 s renouvelées chaque seconde tant que le joueur est collé. */
    private static final int LEVITE = 15 * 20;
    /** Écart toléré entre le haut de la tête et le plafond : contact, puis maintien (retard de la position). */
    private static final double CONTACT = 0.06;
    private static final double MAINTIEN = 0.15;

    private Lang lang;

    /** Recettes : clé -> atout. */
    private final Map<NamespacedKey, Atouts.Atout> recettes = new LinkedHashMap<>();
    /** Secondes portées depuis la dernière écriture dans l'objet, par joueur et emplacement. */
    private final Map<UUID, int[]> portees = new HashMap<>();
    /** Joueurs à qui le casque poiscaille donne la vision nocturne. */
    private final Set<UUID> voyants = new HashSet<>();
    /** Joueurs collés au plafond par le casque ventouse. */
    private final Set<UUID> colles = new HashSet<>();

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
        Bukkit.getScheduler().runTaskTimer(this, this::ventouses, 1L, 1L);
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::ecrire);
        for (UUID id : new ArrayList<>(colles)) {
            Player joueur = Bukkit.getPlayer(id);
            if (joueur != null) {
                decoller(joueur);
            }
        }
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
        List<Material> plastrons = new ArrayList<>(Tag.ITEMS_CHEST_ARMOR.getValues());
        // Un casque déjà sous atout est une tête ou un bloc de miel : accepté, puis vérifié par ses marques.
        casques.add(Material.PLAYER_HEAD);
        casques.add(Material.HONEY_BLOCK);
        for (Atouts.Atout a : Atouts.ATOUTS.values()) {
            NamespacedKey cle = new NamespacedKey(this, a.id());
            ShapedRecipe r = new ShapedRecipe(cle, apercu(a));
            r.shape("RRR", "RER", "RRR");
            r.setIngredient('R', new RecipeChoice.MaterialChoice(a.ressource()));
            r.setIngredient('E', new RecipeChoice.MaterialChoice(switch (a.emplacement()) {
                case FEET -> bottes;
                case CHEST -> plastrons;
                default -> casques;
            }));
            Bukkit.addRecipe(r);
            recettes.put(cle, a);
        }
    }

    /** Résultat affiché dans le livre de recettes (le vrai résultat est calculé sur l'équipement posé). */
    private static ItemStack apercu(Atouts.Atout a) {
        ItemStack item = new ItemStack(a.apparence());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(a.nom(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(a.nether() ? "Ajoute " + a.parCraft() / 60 + " min d'atout, comptées dans le Nether"
                : "Ajoute 1 h d'atout à ton équipement", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Résultat réel : l'équipement du centre avec l'atout (rien si ce n'est pas une pièce acceptée). Un objet sous atout
     * ailleurs (autre craft, ou autour de l'équipement) ne donne rien : un casque ventouse n'est pas un bloc de miel.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreparer(PrepareItemCraftEvent event) {
        Recipe recette = event.getRecipe();
        if (recette == null) {
            return;
        }
        CraftingInventory grille = event.getInventory();
        ItemStack[] cases = grille.getMatrix();
        Atouts.Atout a = recette instanceof ShapedRecipe forme ? recettes.get(forme.getKey()) : null;
        for (int i = 0; i < cases.length; i++) {
            if ((a == null || i != 4) && Atouts.atout(cases[i]) != null) {
                grille.setResult(null);
                return;
            }
        }
        if (a == null) {
            return;
        }
        ItemStack centre = cases.length == 9 ? cases[4] : null;
        grille.setResult(Atouts.accepte(centre, a) ? Atouts.poser(centre, a) : null);
    }

    /** Fabricant (crafter) : rien avec un objet sous atout dans sa grille. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFabricant(CrafterCraftEvent event) {
        if (!(event.getBlock().getState(false) instanceof Crafter fabricant)) {
            return;
        }
        for (ItemStack item : fabricant.getInventory().getContents()) {
            if (Atouts.atout(item) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Un objet sous atout (tête, bloc de miel) ne se pose pas : posé, il perdrait ses caractéristiques. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPoser(BlockPlaceEvent event) {
        if (Atouts.atout(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ décompte et effets

    /** Pièce portée à l'emplacement de l'atout, si elle porte cet atout. */
    private static ItemStack porte(Player joueur, Atouts.Atout a) {
        PlayerInventory inv = joueur.getInventory();
        ItemStack item = switch (a.emplacement()) {
            case FEET -> inv.getBoots();
            case CHEST -> inv.getChestplate();
            default -> inv.getHelmet();
        };
        return Atouts.atout(item) == a ? item : null;
    }

    /** Case du temps porté d'un emplacement (bottes, casque, plastron). */
    private static int indice(EquipmentSlot emplacement) {
        return switch (emplacement) {
            case FEET -> 0;
            case CHEST -> 2;
            default -> 1;
        };
    }

    /**
     * 1.2.0 (KS_MaleficesNether) : le joueur porte-t-il une pièce avec cet atout (« anti_radiation »...) ? Un atout
     * porté a toujours du temps (il se retire à la fin de son temps).
     */
    public static boolean porteAtout(Player joueur, String id) {
        Atouts.Atout a = Atouts.ATOUTS.get(id);
        return a != null && porte(joueur, a) != null;
    }

    private int restantPorte(Player joueur, Atouts.Atout a) {
        ItemStack item = porte(joueur, a);
        if (item == null) {
            return 0;
        }
        int[] s = portees.get(joueur.getUniqueId());
        int deja = s == null ? 0 : s[indice(a.emplacement())];
        return Math.max(1, Atouts.restant(item) - deja);
    }

    /**
     * Chaque seconde : temps porté (écrit dans l'objet chaque minute), lévitation du casque ventouse, vision nocturne du
     * casque poiscaille.
     */
    private void seconde() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            int[] s = portees.computeIfAbsent(joueur.getUniqueId(), u -> new int[3]);
            PlayerInventory inv = joueur.getInventory();
            compter(joueur, inv.getBoots(), s, 0, EquipmentSlot.FEET);
            compter(joueur, inv.getHelmet(), s, 1, EquipmentSlot.HEAD);
            compter(joueur, inv.getChestplate(), s, 2, EquipmentSlot.CHEST);
            if (colles.contains(joueur.getUniqueId()) && leviteVentouse(joueur)) {
                joueur.addPotionEffect(levitation());
            }
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
        // 1.2.0 : le plastron anti-radiation ne s'use que dans le Nether.
        if (a.nether() && joueur.getWorld().getEnvironment() != World.Environment.NETHER) {
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
            switch (emplacement) {
                case FEET -> joueur.getInventory().setBoots(fini);
                case CHEST -> joueur.getInventory().setChestplate(fini);
                default -> joueur.getInventory().setHelmet(fini);
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
        if (s.length > 2 && s[2] > 0 && Atouts.atout(inv.getChestplate()) != null
                && !Atouts.decompter(inv.getChestplate(), s[2])) {
            inv.setChestplate(Atouts.retirer(inv.getChestplate()));
        }
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        ecrire(event.getPlayer());
        voyants.remove(event.getPlayer().getUniqueId());
        if (colles.contains(event.getPlayer().getUniqueId())) {
            decoller(event.getPlayer());
        }
    }

    // ------------------------------------------------------------------ casque ventouse

    /** Chaque tick : collage quand la tête touche un plafond, décollage en sneakant ou hors de dessous le plafond. */
    private void ventouses() {
        Atouts.Atout ventouse = Atouts.ATOUTS.get(Atouts.VENTOUSE);
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            ItemStack casque = joueur.getInventory().getHelmet();
            boolean porte = casque != null && casque.getType() == ventouse.apparence() && Atouts.atout(casque) == ventouse;
            if (colles.contains(joueur.getUniqueId())) {
                // Lévitation retirée par le jeu (lait, mort) ou remplacée : décollé.
                if (!porte || !peutColler(joueur) || !plafond(joueur, MAINTIEN) || !leviteVentouse(joueur)) {
                    decoller(joueur);
                }
            } else if (porte && peutColler(joueur) && !joueur.isOnGround() && plafond(joueur, CONTACT)
                    && joueur.getPotionEffect(PotionEffectType.LEVITATION) == null) {
                joueur.addPotionEffect(levitation());
                colles.add(joueur.getUniqueId());
            }
        }
    }

    /** Ni sneak, ni eau ou lave, ni véhicule, élytre, vol ou spectateur. */
    private static boolean peutColler(Player joueur) {
        return !joueur.isDead() && !joueur.isSneaking() && !joueur.isInWater() && !joueur.isInLava()
                && !joueur.isInsideVehicle() && !joueur.isGliding() && !joueur.isFlying()
                && joueur.getGameMode() != GameMode.SPECTATOR;
    }

    /** Un bloc qui arrête la tête juste au-dessus d'elle (fine tranche au-dessus de la boîte du joueur). */
    private static boolean plafond(Player joueur, double ecart) {
        BoundingBox b = joueur.getBoundingBox();
        BoundingBox tranche = new BoundingBox(b.getMinX() + 0.01, b.getMaxY(), b.getMinZ() + 0.01,
                b.getMaxX() - 0.01, b.getMaxY() + ecart, b.getMaxZ() - 0.01);
        return joueur.getWorld().hasCollisionsIn(tranche);
    }

    /** Lévitation de la ventouse : niveau I, sans particules ni icône. */
    private static PotionEffect levitation() {
        return new PotionEffect(PotionEffectType.LEVITATION, LEVITE, 0, true, false, false);
    }

    /** La lévitation actuelle est-elle celle de la ventouse (une autre, de shulker par exemple, n'est pas touchée) ? */
    private static boolean leviteVentouse(Player joueur) {
        PotionEffect actuelle = joueur.getPotionEffect(PotionEffectType.LEVITATION);
        return actuelle != null && !actuelle.hasParticles() && !actuelle.hasIcon() && actuelle.getAmplifier() == 0
                && actuelle.getDuration() <= LEVITE;
    }

    private void decoller(Player joueur) {
        colles.remove(joueur.getUniqueId());
        if (leviteVentouse(joueur)) {
            joueur.removePotionEffect(PotionEffectType.LEVITATION);
        }
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
