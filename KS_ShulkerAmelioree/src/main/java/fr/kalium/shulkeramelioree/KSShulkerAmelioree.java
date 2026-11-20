package fr.kalium.shulkeramelioree;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.util.BoundingBox;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * KS_ShulkerAmelioree (LeKiwi06, 01-02/11/2026, cahier « Blocs, cultures et objets », section 4) : une boîte de shulker
 * qui contient autant qu'un coffre double (54 cases), sur Kixster.
 *
 * - Craft (enregistré ici) : une boîte de shulker vide au centre, 4 gabarits d'amélioration en netherite en + autour ;
 *   la couleur de la boîte est gardée.
 * - Gérée comme le coffre renforcé (KS_CoffreRenforce) : le contenu est rangé par le plugin dans le bloc (voir
 *   {@link Contenu}) ; un clic droit ouvre un menu de 54 cases (voir {@link MenuShulker}) ; les entonnoirs la remplissent
 *   et la vident, un comparateur donne le signal d'un coffre double.
 * - Cassée, elle redonne la shulker améliorée avec tout son contenu (gardé dans l'objet) ; les explosions, le wither et
 *   les pistons ne la détruisent pas ; un distributeur ne la pose pas. Objet détruit au sol (lave, feu, cactus) : son
 *   contenu tombe, comme pour une boîte de shulker du jeu.
 * - Aucune boîte de shulker à l'intérieur, même vide.
 * - Décraft (enregistré ici) : une boîte de shulker ordinaire vide, seule dans la grille, donne 2 carapaces de shulker
 *   et le coffre de l'Ender reste dans la grille (sa recette KS_Crafts 1.10.0 en utilise un). La shulker améliorée ne
 *   se décrafte pas.
 *
 * Le bloc est une vraie boîte de shulker et l'objet une boîte de shulker nommée, à l'aspect enchanté : visibles sur
 * Bedrock, aucune apparence à ajouter sur le proxy.
 * Autres plugins (encapsuleur) : estShulkerAmelioree, creerShulker, estVide, contenu.
 */
public final class KSShulkerAmelioree extends JavaPlugin implements Listener {

    private static KSShulkerAmelioree instance;
    private NamespacedKey cle;
    private NamespacedKey cleContenu;
    private NamespacedKey cleRecette;
    private NamespacedKey cleDecraft;
    private Lang lang;
    private Contenu contenu;
    private MenuShulker menu;
    /** Shulkers touchées par un entonnoir pendant ce tick, remises en ordre au tick suivant. */
    private final Set<Block> aRanger = new LinkedHashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        cle = new NamespacedKey(this, "shulker");
        cleContenu = new NamespacedKey(this, "contenu");
        cleRecette = new NamespacedKey(this, "shulker_amelioree");
        cleDecraft = new NamespacedKey(this, "decraft_shulker");
        lang = new Lang(this);
        contenu = new Contenu(cle, new NamespacedKey(this, "reserve"), new NamespacedKey(this, "carte"));
        menu = new MenuShulker(this, lang, contenu);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menu, this);
        // 1.0.1 : menus d'une shulker retirée sans être cassée (encapsuleur, commande...) fermés aussitôt.
        Bukkit.getScheduler().runTaskTimer(this, menu::fermerPerimes, 10L, 10L);
        recettes();
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        menu.fermerTout();
        ranger();
        Bukkit.removeRecipe(cleRecette);
        Bukkit.removeRecipe(cleDecraft);
    }

    // ------------------------------------------------------------------ les objets

    /** Une shulker améliorée vide (violette, la couleur de la boîte du jeu). Nécessite que le plugin soit activé. */
    public static ItemStack creerShulker() {
        return creerShulker(Material.SHULKER_BOX);
    }

    /** Une shulker améliorée vide, de la couleur de cette boîte de shulker. */
    public static ItemStack creerShulker(Material boite) {
        return instance.objet(Tag.SHULKER_BOXES.isTagged(boite) ? boite : Material.SHULKER_BOX,
                new ItemStack[Contenu.CASES], null);
    }

    /** L'objet, avec son contenu (54 cases) et son nom (null : nom par défaut). */
    private ItemStack objet(Material type, ItemStack[] cases, Component nom) {
        ItemStack objet = new ItemStack(type);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom != null ? nom
                : lang.c("objet.nom", "<gold>Shulker améliorée").decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = new ArrayList<>(Contenant.lignes(lang.c("objet.description",
                "<gray>Contient autant qu'un coffre double. Se remplit et se vide par entonnoir.")));
        int occupees = 0;
        List<ItemStack> apercu = new ArrayList<>();
        for (ItemStack dedans : cases) {
            if (!Contenu.rien(dedans)) {
                occupees++;
                if (apercu.size() < 5) {
                    apercu.add(dedans);
                }
            }
        }
        lignes.add(lang.c("objet.cases", "<gray><n> / <max> cases occupées", "n", occupees, "max", Contenu.CASES));
        for (ItemStack dedans : apercu) {
            lignes.add(lang.c("objet.ligne", "<white><objet> x<n>", "objet", dedans.effectiveName(),
                    "n", dedans.getAmount()));
        }
        if (occupees > apercu.size()) {
            lignes.add(lang.c("objet.autres", "<gray>et <n> autre(s)...", "n", occupees - apercu.size()));
        }
        meta.lore(lignes.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
        meta.getPersistentDataContainer().set(cle, PersistentDataType.BYTE, (byte) 1);
        if (occupees > 0) {
            meta.getPersistentDataContainer().set(cleContenu, PersistentDataType.BYTE_ARRAY, Contenu.octets(cases));
        }
        meta.setEnchantmentGlintOverride(true);
        objet.setItemMeta(meta);
        lang.saveIfNeeded();
        return objet;
    }

    private boolean marque(ItemStack objet) {
        return objet != null && Tag.SHULKER_BOXES.isTagged(objet.getType()) && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(cle);
    }

    private ItemStack[] casesDe(ItemStack objet) {
        return Contenu.cases(objet.getItemMeta().getPersistentDataContainer().get(cleContenu,
                PersistentDataType.BYTE_ARRAY));
    }

    // ------------------------------------------------------------------ API (encapsuleur)

    public static boolean estShulkerAmelioree(ItemStack objet) {
        return instance != null && instance.marque(objet);
    }

    public static boolean estShulkerAmelioree(Block bloc) {
        return instance != null && instance.contenu.est(bloc);
    }

    /** Une shulker améliorée (objet) vide ; false pour un autre objet. */
    public static boolean estVide(ItemStack objet) {
        return estShulkerAmelioree(objet) && Contenu.vide(instance.casesDe(objet));
    }

    /** Copie des 54 cases d'une shulker améliorée (objet), null pour les cases vides ; null pour un autre objet. */
    public static ItemStack[] contenu(ItemStack objet) {
        return estShulkerAmelioree(objet) ? instance.casesDe(objet) : null;
    }

    /** Copie des 54 cases d'une shulker améliorée posée (menu ouvert compris) ; null pour un autre bloc. */
    public static ItemStack[] contenu(Block bloc) {
        if (!estShulkerAmelioree(bloc)) {
            return null;
        }
        instance.menu.sauverMaintenant(bloc);
        return instance.contenu.lire(bloc, null);
    }

    // ------------------------------------------------------------------ poser, ouvrir, casser

    /** Après les protections : le contenu de l'objet passe dans le bloc. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPoser(BlockPlaceEvent event) {
        ItemStack objet = event.getItemInHand();
        Block bloc = event.getBlockPlaced();
        if (!marque(objet) || !Tag.SHULKER_BOXES.isTagged(bloc.getType())) {
            return;
        }
        menu.oublier(bloc); // 1.0.1 : un menu resté sur cette place (shulker retirée juste avant) ne sert plus
        ItemStack[] cases = casesDe(objet);
        contenu.ecrire(bloc, cases);
        // Au cas où le bloc du jeu serait écrit après l'événement : on vérifie au tick suivant.
        Bukkit.getScheduler().runTask(this, () -> {
            if (Tag.SHULKER_BOXES.isTagged(bloc.getType()) && !contenu.est(bloc)) {
                ItemStack[] tout = contenu.lire(bloc, null);
                for (ItemStack dedans : cases) {
                    if (!Contenu.rien(dedans)) {
                        ItemStack reste = Contenu.ranger(tout, dedans);
                        if (reste != null) {
                            bloc.getWorld().dropItemNaturally(MenuShulker.dessus(bloc), reste);
                        }
                    }
                }
                contenu.ecrire(bloc, tout);
            }
        });
    }

    /**
     * 1.0.1 : clic droit sur la shulker améliorée posée : le menu s'ouvre directement (par l'ouverture du jeu annulée,
     * le couvercle ne s'animait pas : retour de LeKiwi06 du 20/11). Respecte les protections (clic refusé par un claim :
     * rien) et le jeu : accroupi avec un objet en main, l'objet sert ; 1.0.2 : couvercle gêné par un bloc, elle ne
     * s'ouvre pas, comme une boîte du jeu (sauf si son menu est déjà ouvert par un autre joueur, couvercle levé).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClicBloc(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.useInteractedBlock() == Event.Result.DENY
                || !contenu.est(event.getClickedBlock())) {
            return;
        }
        Player joueur = event.getPlayer();
        if (joueur.getGameMode() == GameMode.SPECTATOR || (joueur.isSneaking()
                && (!joueur.getInventory().getItemInMainHand().isEmpty()
                || !joueur.getInventory().getItemInOffHand().isEmpty()))) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // l'autre main : un seul menu
        }
        Block bloc = event.getClickedBlock();
        if (!menu.ouvert(bloc) && couvercleGene(bloc)) {
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> menu.ouvrir(joueur, bloc));
    }

    /**
     * Comme ShulkerBoxBlock.canOpen du jeu : le couvercle a besoin de la moitié du bloc voisin dans la direction de la
     * boîte ; une forme de collision de ce bloc dans cette moitié l'en empêche (les entités sont ignorées).
     */
    private static boolean couvercleGene(Block bloc) {
        BlockFace face = bloc.getBlockData() instanceof Directional d ? d.getFacing() : BlockFace.UP;
        Block voisin = bloc.getRelative(face);
        double e = 1.0E-6;
        BoundingBox zone = switch (face) {
            case DOWN -> new BoundingBox(e, 0.5 + e, e, 1 - e, 1 - e, 1 - e);
            case NORTH -> new BoundingBox(e, e, 0.5 + e, 1 - e, 1 - e, 1 - e);
            case SOUTH -> new BoundingBox(e, e, e, 1 - e, 1 - e, 0.5 - e);
            case WEST -> new BoundingBox(0.5 + e, e, e, 1 - e, 1 - e, 1 - e);
            case EAST -> new BoundingBox(e, e, e, 0.5 - e, 1 - e, 1 - e);
            default -> new BoundingBox(e, e, e, 1 - e, 0.5 - e, 1 - e); // UP
        };
        for (BoundingBox forme : voisin.getCollisionShape().getBoundingBoxes()) {
            if (forme.overlaps(zone)) {
                return true;
            }
        }
        return false;
    }

    /** La vraie boîte ne s'ouvre jamais : le menu à la place (ouverture par un autre chemin que le clic). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOuvrir(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof ShulkerBox shulker)
                || !contenu.est(shulker.getBlock())) {
            return;
        }
        event.setCancelled(true);
        if (event.getPlayer() instanceof Player joueur && joueur.getGameMode() != GameMode.SPECTATOR) {
            Block bloc = shulker.getBlock();
            Bukkit.getScheduler().runTask(this, () -> menu.ouvrir(joueur, bloc));
        }
    }

    /**
     * Cassée (après les protections) : menus fermés, contenu lu puis retiré du bloc (le jeu ne lâche plus rien), et la
     * shulker améliorée tombe avec son contenu et son nom. En créatif, une shulker vide ne lâche rien (comme le jeu).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCasser(BlockBreakEvent event) {
        Block bloc = event.getBlock();
        if (!contenu.est(bloc)) {
            return;
        }
        menu.fermerTout(bloc);
        Location dessus = bloc.getLocation().add(0.5, 0.5, 0.5);
        ItemStack[] cases = contenu.lire(bloc, dessus);
        Component nom = bloc.getState(false) instanceof ShulkerBox s ? s.customName() : null;
        Material type = bloc.getType();
        contenu.vider(bloc);
        event.setDropItems(false);
        aRanger.remove(bloc);
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && Contenu.vide(cases)) {
            return;
        }
        bloc.getWorld().dropItemNaturally(dessus, objet(type, cases, nom));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionEntite(EntityExplodeEvent event) {
        event.blockList().removeIf(contenu::est);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionBloc(BlockExplodeEvent event) {
        event.blockList().removeIf(contenu::est);
    }

    /** Wither, dragon de l'Ender... : la shulker améliorée n'est pas détruite. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntiteBloc(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Player) && contenu.est(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Un piston casserait la boîte (le jeu la lâcherait avec seulement le tampon) : il ne bouge pas. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonPousse(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(contenu::est)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonTire(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(contenu::est)) {
            event.setCancelled(true);
        }
    }

    /** Un distributeur poserait une boîte de shulker ordinaire (contenu perdu) : la shulker améliorée y reste. */
    @EventHandler(ignoreCancelled = true)
    public void onDistribuer(BlockDispenseEvent event) {
        if (marque(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Objet détruit au sol (lave, feu, cactus, explosion) : son contenu tombe, comme une boîte de shulker du jeu. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onObjetDetruit(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item entite) || !marque(entite.getItemStack())) {
            return;
        }
        // Comme le jeu : health = (int) (health - dégâts), l'objet disparaît à 0.
        if ((int) (entite.getHealth() - event.getFinalDamage()) > 0) {
            return;
        }
        ItemStack objet = entite.getItemStack();
        ItemStack[] cases = casesDe(objet);
        if (Contenu.vide(cases)) {
            return;
        }
        entite.setItemStack(objet(objet.getType(), new ItemStack[Contenu.CASES], objet.getItemMeta().displayName()));
        for (ItemStack dedans : cases) {
            if (!Contenu.rien(dedans)) {
                entite.getWorld().dropItem(entite.getLocation(), dedans);
            }
        }
    }

    // ------------------------------------------------------------------ entonnoirs

    /**
     * Un entonnoir (ou un dropper) ne met dans la shulker que ce qui tient dans ses 54 cases, jamais une boîte de
     * shulker ; il n'y touche pas pendant qu'un joueur a son menu ouvert.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntonnoir(InventoryMoveItemEvent event) {
        Block source = bloc(event.getSource());
        if (source != null && menu.ouvert(source)) {
            event.setCancelled(true);
            return;
        }
        Block destination = bloc(event.getDestination());
        if (destination == null) {
            return;
        }
        ItemStack objet = event.getItem();
        if (menu.ouvert(destination) || Contenu.refuse(objet)
                || !Contenu.tient(contenu.lire(destination, null), objet)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntonnoirPasse(InventoryMoveItemEvent event) {
        for (Inventory inventaire : new Inventory[]{event.getSource(), event.getDestination()}) {
            Block bloc = bloc(inventaire);
            if (bloc != null && aRanger.add(bloc) && aRanger.size() == 1) {
                Bukkit.getScheduler().runTask(this, this::ranger);
            }
        }
    }

    /** La shulker améliorée d'un inventaire, sinon null. */
    private Block bloc(Inventory inventaire) {
        if (inventaire != null && inventaire.getHolder(false) instanceof ShulkerBox shulker
                && contenu.est(shulker.getBlock())) {
            return shulker.getBlock();
        }
        return null;
    }

    /** Tampon recalculé pour chaque shulker touchée par un entonnoir. */
    private void ranger() {
        for (Block bloc : List.copyOf(aRanger)) {
            if (contenu.est(bloc) && !menu.ouvert(bloc)) {
                contenu.ecrire(bloc, contenu.lire(bloc, MenuShulker.dessus(bloc)));
            }
        }
        aRanger.clear();
    }

    // ------------------------------------------------------------------ crafts

    private void recettes() {
        RecipeChoice boites = new RecipeChoice.MaterialChoice(new ArrayList<>(Tag.SHULKER_BOXES.getValues()));
        ShapedRecipe amelioration = new ShapedRecipe(cleRecette, new ItemStack(Material.SHULKER_BOX));
        amelioration.shape(" G ", "GSG", " G ");
        amelioration.setIngredient('G', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        amelioration.setIngredient('S', boites);
        Bukkit.addRecipe(amelioration);
        ShapelessRecipe decraft = new ShapelessRecipe(cleDecraft, new ItemStack(Material.SHULKER_SHELL, 2));
        decraft.addIngredient(boites);
        Bukkit.addRecipe(decraft);
    }

    private boolean estRecette(Recipe recette, NamespacedKey cle) {
        return recette instanceof Keyed k && k.getKey().equals(cle);
    }

    /** Boîte de shulker ordinaire sans rien dedans. */
    private boolean boiteVide(ItemStack objet) {
        if (objet == null || !Tag.SHULKER_BOXES.isTagged(objet.getType()) || marque(objet)) {
            return false;
        }
        return !(objet.getItemMeta() instanceof BlockStateMeta meta && meta.hasBlockState()
                && meta.getBlockState() instanceof ShulkerBox boite && !boite.getInventory().isEmpty());
    }

    /** Résultat d'un de nos crafts pour cette grille (null : refusé). */
    private ItemStack resultat(Recipe recette, ItemStack[] grille) {
        if (estRecette(recette, cleRecette)) {
            ItemStack boite = grille.length > 4 ? grille[4] : null;
            return boiteVide(boite) ? creerShulker(boite.getType()) : null;
        }
        int place = seul(grille);
        return place >= 0 && boiteVide(grille[place]) ? new ItemStack(Material.SHULKER_SHELL, 2) : null;
    }

    /** Case du seul objet de la grille, ou -1 (grille vide ou plusieurs objets). */
    private static int seul(ItemStack[] grille) {
        int place = -1;
        for (int i = 0; i < grille.length; i++) {
            if (!Contenu.rien(grille[i])) {
                if (place >= 0) {
                    return -1;
                }
                place = i;
            }
        }
        return place;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreparerCraft(PrepareItemCraftEvent event) {
        Recipe recette = event.getRecipe();
        if (estRecette(recette, cleRecette) || estRecette(recette, cleDecraft)) {
            event.getInventory().setResult(resultat(recette, event.getInventory().getMatrix()));
        }
    }

    /** Prise du résultat : vérifiée à nouveau ; pour le décraft, le coffre de l'Ender revient dans la grille. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Recipe recette = event.getRecipe();
        boolean decraft = estRecette(recette, cleDecraft);
        if (!decraft && !estRecette(recette, cleRecette)) {
            return;
        }
        ItemStack[] grille = event.getInventory().getMatrix();
        if (resultat(recette, grille) == null || !(event.getWhoClicked() instanceof Player joueur)) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
            return;
        }
        if (decraft) {
            int place = seul(grille);
            ItemStack boite = grille[place].clone();
            InventoryView vue = event.getView();
            // Le jeu retire la boîte de la grille après l'événement : le coffre de l'Ender y est posé au tick suivant.
            Bukkit.getScheduler().runTask(this, () -> rendreCoffre(joueur, vue, place, boite));
        }
    }

    private static void rendreCoffre(Player joueur, InventoryView vue, int place, ItemStack boite) {
        ItemStack coffre = new ItemStack(Material.ENDER_CHEST);
        if (joueur.isOnline() && joueur.getOpenInventory() == vue
                && vue.getTopInventory() instanceof CraftingInventory grille) {
            ItemStack[] matrice = grille.getMatrix();
            if (place < matrice.length && boite.isSimilar(matrice[place])) {
                // Craft pas fait (Maj + clic, inventaire plein) : rien à rendre.
                return;
            }
            if (place < matrice.length && Contenu.rien(matrice[place])) {
                matrice[place] = coffre;
                grille.setMatrix(matrice);
                return;
            }
        }
        for (ItemStack reste : joueur.getInventory().addItem(coffre).values()) {
            joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
        }
    }

    /** Crafter : mêmes résultats ; pour le décraft, le coffre de l'Ender reste dans sa case. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        Recipe recette = event.getRecipe();
        boolean decraft = estRecette(recette, cleDecraft);
        if ((!decraft && !estRecette(recette, cleRecette))
                || !(event.getBlock().getState(false) instanceof org.bukkit.block.Crafter crafter)) {
            return;
        }
        Inventory cases = crafter.getInventory();
        ItemStack resultat = resultat(recette, cases.getContents());
        if (resultat == null) {
            event.setCancelled(true);
            return;
        }
        event.setResult(resultat);
        if (!decraft) {
            return;
        }
        int place = seul(cases.getContents());
        Block bloc = event.getBlock();
        ItemStack boite = cases.getItem(place).clone();
        Bukkit.getScheduler().runTask(this, () -> {
            ItemStack coffre = new ItemStack(Material.ENDER_CHEST);
            if (bloc.getState(false) instanceof org.bukkit.block.Crafter apres) {
                ItemStack dedans = apres.getInventory().getItem(place);
                if (boite.isSimilar(dedans)) {
                    return;
                }
                if (Contenu.rien(dedans)) {
                    apres.getInventory().setItem(place, coffre);
                    return;
                }
            }
            bloc.getWorld().dropItemNaturally(bloc.getLocation().add(0.5, 1.1, 0.5), coffre);
        });
    }
}
