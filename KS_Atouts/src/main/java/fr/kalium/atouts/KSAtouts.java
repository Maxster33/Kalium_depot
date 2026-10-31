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
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
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
import java.util.function.Predicate;

/**
 * KS_Atouts (cahier des charges : Atouts_Alcools_Aliments.md, LeKiwi06, 28/10/2026, partie 3.5) : serveur Kixster,
 * étape 4.
 *
 * - Craft : 8 ressources autour de l'équipement, à l'établi (pied de lapin autour de bottes : bottes de ski ; poisson-
 *   globe autour d'un casque : casque poiscaille ; crème de magma autour de bottes : bottes coupe-feu ; bloc de miel
 *   autour d'un casque : casque ventouse, ajouté le 29/10).
 * - Chaque craft ajoute 1 heure, décomptée seulement quand l'objet est porté, sans plafond. Un autre atout remplace le
 *   précédent (son temps est perdu).
 * - Pendant l'effet de l'atout (1.3.0, LeKiwi06, 30/10/2026 : seulement pendant l'effet, plus pendant tout l'atout),
 *   l'objet porté prend l'apparence (le vrai type) de bottes en cuir, d'une tête de gardien, de bottes en cuivre, d'un
 *   bloc de miel ou d'un plastron en or, en gardant ses caractéristiques ({@link Atouts}) ; le reste du temps, et dès
 *   qu'il n'est plus porté, il a son type d'origine. Un objet sous atout ne se pose pas comme un bloc et ne sert dans
 *   aucun autre craft.
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
    /** 1.3.0 : l'apparence reste 1 s après la fin de l'effet (pas de clignotement au bord de l'eau ou du magma). */
    private static final int APRES = 20;
    /**
     * 1.3.0 : poudreuse cherchée autour des bottes de ski (blocs) : sur les côtés, en dessous (en chute : plus bas). Le jeu
     * ne laisse marcher dessus qu'avec de vraies bottes en cuir, côté client : elles doivent arriver avant le pas.
     */
    private static final int SKI_COTE = 2;
    private static final int SKI_DESSOUS = 3;
    private static final int SKI_CHUTE = 12;
    private static final EquipmentSlot[] EMPLACEMENTS = {EquipmentSlot.FEET, EquipmentSlot.HEAD, EquipmentSlot.CHEST};

    private Lang lang;

    /** Recettes : clé -> atout. */
    private final Map<NamespacedKey, Atouts.Atout> recettes = new LinkedHashMap<>();
    /** Secondes portées depuis la dernière écriture dans l'objet, par joueur et emplacement. */
    private final Map<UUID, int[]> portees = new HashMap<>();
    /** Joueurs à qui le casque poiscaille donne la vision nocturne. */
    private final Set<UUID> voyants = new HashSet<>();
    /** Joueurs collés au plafond par le casque ventouse. */
    private final Set<UUID> colles = new HashSet<>();
    /** 1.3.0 : dernier tick où l'effet de l'atout avait lieu, par joueur et emplacement. */
    private final Map<UUID, long[]> effets = new HashMap<>();
    private long tick;

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
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            ventouses();
            apparences();
        }, 1L, 1L);
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::ecrire);
        Bukkit.getOnlinePlayers().forEach(this::aspectsDOrigine);
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
        // 1.3.0 : un casque sous atout n'est plus une tête ou un bloc de miel hors de l'effet (il n'est alors pas porté).
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
     * casque poiscaille ; objets qui ne sont plus portés remis à leur aspect d'origine (au cas où un chemin aurait échappé
     * aux événements).
     */
    private void seconde() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            ranger(joueur);
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
            joueur.getEquipment().setItem(emplacement, Atouts.retirer(item), true);
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
        aspectsDOrigine(event.getPlayer());
        effets.remove(event.getPlayer().getUniqueId());
        voyants.remove(event.getPlayer().getUniqueId());
        if (colles.contains(event.getPlayer().getUniqueId())) {
            decoller(event.getPlayer());
        }
    }

    // ------------------------------------------------------------------ apparence pendant l'effet (1.3.0)

    /**
     * Chaque tick : la pièce portée prend l'apparence de son atout pendant l'effet (et encore 1 s après), son type
     * d'origine le reste du temps. Échangée sans son d'équipement.
     */
    private void apparences() {
        tick++;
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            long[] derniers = effets.computeIfAbsent(joueur.getUniqueId(), u -> new long[3]);
            for (EquipmentSlot emplacement : EMPLACEMENTS) {
                ItemStack item = joueur.getInventory().getItem(emplacement);
                Atouts.Atout a = Atouts.atout(item);
                if (a == null || a.emplacement() != emplacement) {
                    continue;
                }
                int i = indice(emplacement);
                if (enEffet(joueur, a)) {
                    derniers[i] = tick;
                }
                // Rien à changer : le même objet revient (déjà de ce type, plastron déjà en or...).
                ItemStack voulu = derniers[i] > 0 && tick - derniers[i] <= APRES ? Atouts.activer(item)
                        : Atouts.desactiver(item);
                if (voulu != item) {
                    joueur.getEquipment().setItem(emplacement, voulu, true);
                }
            }
        }
    }

    /**
     * L'effet de l'atout a-t-il lieu : bottes de ski près de la poudreuse (avant le pas), casque poiscaille près de
     * l'eau, bottes coupe-feu sur un bloc de magma, casque ventouse collé au plafond, plastron dans le Nether.
     */
    private boolean enEffet(Player joueur, Atouts.Atout a) {
        return switch (a.id()) {
            case Atouts.SKI -> pres(joueur, d -> d.getMaterial() == Material.POWDER_SNOW, SKI_COTE,
                    joueur.getFallDistance() > 1 ? SKI_CHUTE : SKI_DESSOUS);
            // 1.4.0 : près de l'eau, comme les bottes de ski (la vision nocturne reste sous l'eau).
            case Atouts.POISCAILLE -> joueur.isUnderWater() || pres(joueur, KSAtouts::eau, SKI_COTE,
                    joueur.getFallDistance() > 1 ? SKI_CHUTE : SKI_DESSOUS);
            case Atouts.COUPE_FEU -> pres(joueur, d -> d.getMaterial() == Material.MAGMA_BLOCK, 0, 0.2);
            case Atouts.VENTOUSE -> colles.contains(joueur.getUniqueId());
            case Atouts.ANTI_RADIATION -> joueur.getWorld().getEnvironment() == World.Environment.NETHER;
            default -> false;
        };
    }

    /** Bloc d'eau : eau, colonne de bulles, plantes aquatiques, bloc immergé (dalle, escalier, barrière...). */
    private static boolean eau(BlockData d) {
        return switch (d.getMaterial()) {
            case WATER, BUBBLE_COLUMN, KELP, KELP_PLANT, SEAGRASS, TALL_SEAGRASS -> true;
            default -> d instanceof Waterlogged w && w.isWaterlogged();
        };
    }

    /** Un bloc voulu dans la boîte du joueur élargie sur les côtés et vers le bas ? */
    private static boolean pres(Player joueur, Predicate<BlockData> voulu, double cote, double dessous) {
        World monde = joueur.getWorld();
        BoundingBox b = joueur.getBoundingBox();
        int x1 = (int) Math.floor(b.getMinX() - cote);
        int x2 = (int) Math.floor(b.getMaxX() + cote - 1.0E-3);
        int z1 = (int) Math.floor(b.getMinZ() - cote);
        int z2 = (int) Math.floor(b.getMaxZ() + cote - 1.0E-3);
        int y1 = Math.max(monde.getMinHeight(), (int) Math.floor(b.getMinY() - dessous));
        int y2 = Math.min(monde.getMaxHeight() - 1, (int) Math.floor(b.getMaxY() - 1.0E-3));
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    if (voulu.test(monde.getBlockData(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Pièces portées remises à leur aspect d'origine (déconnexion, arrêt : l'objet garde son atout). */
    private void aspectsDOrigine(Player joueur) {
        for (EquipmentSlot emplacement : EMPLACEMENTS) {
            ItemStack item = joueur.getInventory().getItem(emplacement);
            if (Atouts.actif(item)) {
                joueur.getEquipment().setItem(emplacement, Atouts.desactiver(item), true);
            }
        }
    }

    /**
     * Un objet qui n'est plus porté reprend son aspect d'origine : inventaire (hors armure, gérée chaque tick), main
     * secondaire, curseur, conteneur ouvert.
     */
    private void ranger(Player joueur) {
        PlayerInventory inv = joueur.getInventory();
        ItemStack[] rangement = inv.getStorageContents();
        for (int i = 0; i < rangement.length; i++) {
            if (Atouts.actif(rangement[i])) {
                inv.setItem(i, Atouts.desactiver(rangement[i]));
            }
        }
        if (Atouts.actif(inv.getItemInOffHand())) {
            inv.setItemInOffHand(Atouts.desactiver(inv.getItemInOffHand()));
        }
        if (Atouts.actif(joueur.getItemOnCursor())) {
            joueur.setItemOnCursor(Atouts.desactiver(joueur.getItemOnCursor()));
        }
        Inventory haut = joueur.getOpenInventory().getTopInventory();
        if (!(haut instanceof PlayerInventory)) {
            ItemStack[] contenu = haut.getContents();
            for (int i = 0; i < contenu.length; i++) {
                if (Atouts.actif(contenu[i])) {
                    haut.setItem(i, Atouts.desactiver(contenu[i]));
                }
            }
        }
    }

    /** Pièce active déplacée à la souris (retirée, mise dans un coffre...) : rangée au tick suivant. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClic(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player joueur
                && (Atouts.actif(event.getCurrentItem()) || Atouts.actif(event.getCursor()))) {
            Bukkit.getScheduler().runTask(this, () -> ranger(joueur));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGlisser(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player joueur && Atouts.actif(event.getOldCursor())) {
            Bukkit.getScheduler().runTask(this, () -> ranger(joueur));
        }
    }

    /** Pièce active lâchée, ou tombée à la mort : aspect d'origine. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMort(PlayerDeathEvent event) {
        event.getDrops().replaceAll(Atouts::desactiver);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onObjetAuSol(ItemSpawnEvent event) {
        Item objet = event.getEntity();
        if (Atouts.actif(objet.getItemStack())) {
            objet.setItemStack(Atouts.desactiver(objet.getItemStack()));
        }
    }

    // ------------------------------------------------------------------ casque ventouse

    /** Chaque tick : collage quand la tête touche un plafond, décollage en sneakant ou hors de dessous le plafond. */
    private void ventouses() {
        Atouts.Atout ventouse = Atouts.ATOUTS.get(Atouts.VENTOUSE);
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            // 1.3.0 : le casque n'est un bloc de miel que collé ; il colle avec son aspect d'origine.
            boolean porte = porte(joueur, ventouse) != null;
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
        // 1.3.0 : pièce active passée de l'armure à la main (clic droit avec un autre casque...) : rangée.
        if (Atouts.actif(event.getNewItemStack())) {
            Player joueur = event.getPlayer();
            Bukkit.getScheduler().runTask(this, () -> ranger(joueur));
        }
    }
}
