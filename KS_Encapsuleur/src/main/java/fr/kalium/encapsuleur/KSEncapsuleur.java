package fr.kalium.encapsuleur;

import fr.kalium.encapsuleur.Analyse.Bilan;
import fr.kalium.encapsuleur.Analyse.Zone;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Encapsuleur (LeKiwi06, 01-02/11/2026, cahier « Blocs, cultures et objets », section 3) : un objet à usage unique
 * qui emporte une construction (64 × 64 × 128 au plus) et la repose ailleurs.
 *
 * - Craft (enregistré ici) : un crafter au centre, 8 shulkers améliorées vides autour (KS_ShulkerAmelioree).
 * - Sélection (choix de LeKiwi06 le 02/11) : encapsuleur en main, clic gauche sur un bloc = coin 1, clic droit = coin 2 ;
 *   le contour s'affiche en particules ; accroupi + clic droit ouvre le menu (Encapsuler, Effacer la sélection).
 * - Encapsuler : seulement dans des claims dont le joueur est le propriétaire ; les blocs qui resteront sur place
 *   (contenants pleins, objets posés dans un bloc, blocs incassables) sont annoncés dans un menu d'avertissement (Voir
 *   la liste, Valider, Annuler). L'encapsuleur devient alors la « Construction encapsulée » (choix de LeKiwi06) et la
 *   zone est vidée petit à petit (voir {@link Chantiers}). Les entités restent sur place.
 * - Poser (choix de LeKiwi06) : clic droit sur un bloc avec la construction : un aperçu (faux blocs vus du seul joueur,
 *   Java et Bedrock) se cale sur ce bloc, face avant vers le joueur ; menu pour tourner, retourner en miroir, décaler,
 *   valider ou annuler (voir {@link Apercu}). Seuls l'air, l'eau, l'herbe et la neige sont remplacés, dans des claims où
 *   le joueur a les droits ; sinon la pose est refusée et les blocs gênants sont montrés. La construction est consommée
 *   et posée petit à petit.
 *
 * L'objet est un bloc de structure (que seuls les opérateurs peuvent poser) à l'aspect enchanté, visible sur Bedrock :
 * aucune apparence à ajouter sur le proxy.
 */
public final class KSEncapsuleur extends JavaPlugin implements Listener {

    static final int MAX_X = 64;
    static final int MAX_Y = 128;
    static final int MAX_Z = 64;

    private NamespacedKey cleObjet;
    private NamespacedKey cleId;
    private NamespacedKey cleRecette;
    Lang lang;
    Chantiers chantiers;
    Apercu apercu;
    private final Map<UUID, Block[]> selections = new HashMap<>();
    /** Blocs restants que le joueur a vus et validés (la liste est refaite à la validation). */
    private final Map<UUID, Set<Block>> avertis = new HashMap<>();

    @Override
    public void onEnable() {
        cleObjet = new NamespacedKey(this, "objet");
        cleId = new NamespacedKey(this, "id");
        cleRecette = new NamespacedKey(this, "encapsuleur");
        lang = new Lang(this);
        chantiers = new Chantiers(this);
        apercu = new Apercu(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(chantiers, this);
        getServer().getPluginManager().registerEvents(apercu, this);
        ShapedRecipe recette = new ShapedRecipe(cleRecette, encapsuleur());
        recette.shape("SSS", "SCS", "SSS");
        recette.setIngredient('S', new RecipeChoice.MaterialChoice(new ArrayList<>(Tag.SHULKER_BOXES.getValues())));
        recette.setIngredient('C', Material.CRAFTER);
        Bukkit.addRecipe(recette);
        chantiers.reprendre();
        getServer().getScheduler().runTaskTimer(this, chantiers::tick, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, chantiers::noterAvancement, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::contours, 10L, 10L);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        apercu.toutAnnuler();
        chantiers.arreter();
        Bukkit.removeRecipe(cleRecette);
    }

    // ------------------------------------------------------------------ objets

    ItemStack encapsuleur() {
        ItemStack objet = objet(lang.c("objet.nom", "<gold>Encapsuleur"), Contenant.lignes(lang.c("objet.description",
                "<gray>Clic gauche : coin 1. Clic droit : coin 2. Accroupi + clic droit : encapsuler la zone "
                        + "(64 x 64 x 128 au plus). Usage unique.")));
        ItemMeta meta = objet.getItemMeta();
        meta.getPersistentDataContainer().set(cleObjet, PersistentDataType.STRING, "encapsuleur");
        objet.setItemMeta(meta);
        return objet;
    }

    ItemStack construction(String id, Zone zone, int blocs) {
        List<Component> lignes = new ArrayList<>();
        lignes.add(lang.c("construction.taille", "<gray>Taille : <white><x> x <y> x <z>",
                "x", zone.sx(), "y", zone.sy(), "z", zone.sz()));
        lignes.add(lang.c("construction.blocs", "<gray>Blocs : <white><n>", "n", blocs));
        lignes.addAll(Contenant.lignes(lang.c("construction.description",
                "<gray>Clic droit sur un bloc pour la poser (aperçu, puis menu pour la tourner et valider).")));
        ItemStack objet = objet(lang.c("construction.nom", "<gold>Construction encapsulée"), lignes);
        ItemMeta meta = objet.getItemMeta();
        meta.getPersistentDataContainer().set(cleObjet, PersistentDataType.STRING, "construction");
        meta.getPersistentDataContainer().set(cleId, PersistentDataType.STRING, id);
        objet.setItemMeta(meta);
        return objet;
    }

    private ItemStack objet(Component nom, List<Component> lignes) {
        ItemStack objet = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        meta.lore(lignes.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        objet.setItemMeta(meta);
        lang.saveIfNeeded();
        return objet;
    }

    String sorte(ItemStack objet) {
        if (objet == null || objet.getType() != Material.STRUCTURE_BLOCK || !objet.hasItemMeta()) {
            return null;
        }
        return objet.getItemMeta().getPersistentDataContainer().get(cleObjet, PersistentDataType.STRING);
    }

    String idDe(ItemStack objet) {
        return objet.getItemMeta().getPersistentDataContainer().get(cleId, PersistentDataType.STRING);
    }

    /** Retire la construction de cet id de l'inventaire du joueur ; faux si elle n'y est plus. */
    boolean consommer(Player joueur, String id) {
        ItemStack[] contenu = joueur.getInventory().getContents();
        for (int i = 0; i < contenu.length; i++) {
            if ("construction".equals(sorte(contenu[i])) && id.equals(idDe(contenu[i]))) {
                joueur.getInventory().setItem(i, null);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ craft

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreparer(PrepareItemCraftEvent event) {
        if (estRecette(event.getRecipe())) {
            event.getInventory().setResult(valide(event.getInventory().getMatrix()) ? encapsuleur() : null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (estRecette(event.getRecipe()) && !valide(event.getInventory().getMatrix())) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (estRecette(event.getRecipe())
                && event.getBlock().getState(false) instanceof org.bukkit.block.Crafter crafter) {
            if (valide(crafter.getInventory().getContents())) {
                event.setResult(encapsuleur());
            } else {
                event.setCancelled(true);
            }
        }
    }

    private boolean estRecette(Recipe recette) {
        return recette instanceof Keyed k && k.getKey().equals(cleRecette);
    }

    /** 8 shulkers améliorées vides autour du crafter (une boîte ordinaire ou pleine ne donne rien). */
    private boolean valide(ItemStack[] grille) {
        if (grille.length < 9) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (i != 4 && !Integration.shulkerVide(grille[i])) {
                return false;
            }
        }
        return true;
    }

    /** Un opérateur en créatif poserait le bloc de structure : jamais. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPoserObjet(BlockPlaceEvent event) {
        if (sorte(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ clics

    @EventHandler(priority = EventPriority.HIGH)
    public void onClic(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            if (sorte(event.getItem()) != null) {
                event.setCancelled(true);
            }
            return;
        }
        String sorte = sorte(event.getItem());
        if (sorte == null) {
            return;
        }
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        Action action = event.getAction();
        boolean droit = action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
        if (sorte.equals("construction")) {
            if (droit) {
                apercu.clic(joueur, event.getItem(), idDe(event.getItem()), event.getClickedBlock(),
                        event.getBlockFace());
            }
            return;
        }
        if (droit && joueur.isSneaking()) {
            menuEncapsuler(joueur);
            return;
        }
        Block bloc = event.getClickedBlock();
        if (bloc == null) {
            return;
        }
        Block[] coins = selections.computeIfAbsent(joueur.getUniqueId(), u -> new Block[2]);
        int n = action == Action.LEFT_CLICK_BLOCK ? 0 : 1;
        if (bloc.equals(coins[n])) {
            return;
        }
        coins[n] = bloc;
        if (coins[1 - n] != null && !coins[1 - n].getWorld().equals(bloc.getWorld())) {
            coins[1 - n] = null;
        }
        message(joueur, lang.c("message.coin", "<green>Coin <n> : <x> <y> <z>", "n", n + 1, "x", bloc.getX(),
                "y", bloc.getY(), "z", bloc.getZ()), false);
        if (coins[0] != null && coins[1] != null) {
            Zone zone = Zone.de(coins[0], coins[1]);
            message(joueur, trop(zone) ? tropGrand(zone) : lang.c("message.zone",
                    "<green>Zone : <x> x <y> x <z>. Accroupi + clic droit pour encapsuler.",
                    "x", zone.sx(), "y", zone.sy(), "z", zone.sz()), trop(zone));
        }
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        selections.remove(event.getPlayer().getUniqueId());
        avertis.remove(event.getPlayer().getUniqueId());
    }

    private static boolean trop(Zone zone) {
        return zone.sx() > MAX_X || zone.sy() > MAX_Y || zone.sz() > MAX_Z;
    }

    private Component tropGrand(Zone zone) {
        return lang.c("message.trop-grand", "<red>Zone trop grande : <x> x <y> x <z> (64 x 128 x 64 au plus).",
                "x", zone.sx(), "y", zone.sy(), "z", zone.sz());
    }

    /** Contour de la sélection, en particules, pour son seul joueur (encapsuleur en main). */
    private void contours() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            Block[] coins = selections.get(joueur.getUniqueId());
            if (coins == null || !"encapsuleur".equals(sorte(joueur.getInventory().getItemInMainHand()))) {
                continue;
            }
            Block a = coins[0] != null ? coins[0] : coins[1];
            Block b = coins[1] != null ? coins[1] : coins[0];
            if (a == null || !a.getWorld().equals(joueur.getWorld())) {
                continue;
            }
            Zone zone = Zone.de(a, b);
            Particle.DustOptions couleur = new Particle.DustOptions(trop(zone) ? Color.RED : Color.AQUA, 1f);
            double x1 = zone.x1(), y1 = zone.y1(), z1 = zone.z1();
            double x2 = zone.x2() + 1, y2 = zone.y2() + 1, z2 = zone.z2() + 1;
            double[][] aretes = {
                    {x1, y1, z1, x2, y1, z1}, {x1, y2, z1, x2, y2, z1}, {x1, y1, z2, x2, y1, z2}, {x1, y2, z2, x2, y2, z2},
                    {x1, y1, z1, x1, y2, z1}, {x2, y1, z1, x2, y2, z1}, {x1, y1, z2, x1, y2, z2}, {x2, y1, z2, x2, y2, z2},
                    {x1, y1, z1, x1, y1, z2}, {x2, y1, z1, x2, y1, z2}, {x1, y2, z1, x1, y2, z2}, {x2, y2, z1, x2, y2, z2}};
            Location oeil = joueur.getLocation();
            for (double[] ar : aretes) {
                double longueur = Math.max(Math.abs(ar[3] - ar[0]), Math.max(Math.abs(ar[4] - ar[1]),
                        Math.abs(ar[5] - ar[2])));
                for (double t = 0; t <= longueur; t += 1) {
                    double f = longueur == 0 ? 0 : t / longueur;
                    Location p = new Location(joueur.getWorld(), ar[0] + (ar[3] - ar[0]) * f,
                            ar[1] + (ar[4] - ar[1]) * f, ar[2] + (ar[5] - ar[2]) * f);
                    if (p.distanceSquared(oeil) < 96 * 96) {
                        joueur.spawnParticle(Particle.DUST, p, 1, couleur);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ encapsuler

    private void menuEncapsuler(Player joueur) {
        Block[] coins = selections.get(joueur.getUniqueId());
        if (coins == null || coins[0] == null || coins[1] == null) {
            message(joueur, lang.c("message.sans-zone",
                    "<red>Choisis d'abord les deux coins : clic gauche puis clic droit sur un bloc."), true);
            return;
        }
        Zone zone = Zone.de(coins[0], coins[1]);
        Contenant c = Contenant.pour(joueur, 3, lang.c("menu.titre", "Encapsuleur"), "encapsuleur");
        for (int i = 0; i < c.taille(); i++) {
            c.poser(i, Contenant.decor(), null);
        }
        c.poser(4, Contenant.objet(Material.STRUCTURE_BLOCK, lang.c("menu.zone", "<gold>Zone choisie"), List.of(
                lang.c("menu.taille", "<gray>Taille : <white><x> x <y> x <z>", "x", zone.sx(), "y", zone.sy(),
                        "z", zone.sz()),
                lang.c("menu.max", "<gray>Au plus : 64 x 128 x 64")
        )), null);
        c.poser(11, Contenant.objet(Material.LIME_CONCRETE, lang.c("menu.encapsuler", "<green>Encapsuler"),
                Contenant.lignes(lang.c("menu.encapsuler-info",
                        "<gray>Emporte les blocs de la zone. Les contenants pleins restent sur place."))),
                j -> encapsuler(j, false));
        c.poser(15, Contenant.objet(Material.BARRIER, lang.c("menu.effacer", "<red>Effacer la sélection"), List.of()),
                j -> {
                    selections.remove(j.getUniqueId());
                    Contenant.fermer(j);
                });
        c.barre();
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, lang.c("menu.aide", "<gold>Encapsuleur"),
                Contenant.lignes(lang.c("menu.aide-texte",
                        "<gray>Seulement dans tes claims. Les entités restent sur place. Usage unique."))), null);
        c.poser(c.bas(5), Contenant.objet(Material.BARRIER, lang.c("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Vérifications, avertissement si des blocs restent, puis encapsulage. */
    void encapsuler(Player joueur, boolean confirme) {
        Block[] coins = selections.get(joueur.getUniqueId());
        if (coins == null || coins[0] == null || coins[1] == null) {
            Contenant.fermer(joueur);
            return;
        }
        if (!"encapsuleur".equals(sorte(joueur.getInventory().getItemInMainHand()))) {
            message(joueur, lang.c("message.tenir", "<red>Tiens l'encapsuleur en main."), true);
            Contenant.fermer(joueur);
            return;
        }
        Zone zone = Zone.de(coins[0], coins[1]);
        if (trop(zone)) {
            message(joueur, tropGrand(zone), true);
            return;
        }
        if (!toutAuProprio(zone, joueur)) {
            message(joueur, lang.c("message.claim",
                    "<red>Toute la zone doit être dans des claims dont tu es le propriétaire."), true);
            return;
        }
        if (chantiers.protege(coins[0]) || chantiers.protege(coins[1])) {
            message(joueur, lang.c("message.chantier", "<red>Un chantier est en cours ici, attends qu'il finisse."),
                    true);
            return;
        }
        Bilan bilan = Analyse.analyser(zone);
        if (bilan.emportes == 0) {
            message(joueur, lang.c("message.vide", "<red>Rien à emporter dans cette zone."), true);
            return;
        }
        Set<Block> restants = new HashSet<>();
        bilan.restent.values().forEach(restants::addAll);
        Set<Block> vus = avertis.getOrDefault(joueur.getUniqueId(), Set.of());
        if (!restants.isEmpty() && !(confirme && vus.containsAll(restants))) {
            avertis.put(joueur.getUniqueId(), restants);
            Menus.avertissement(this, joueur, bilan);
            return;
        }
        avertis.remove(joueur.getUniqueId());
        faire(joueur, zone, bilan);
    }

    private boolean toutAuProprio(Zone zone, Player joueur) {
        for (int cx = zone.x1() >> 4; cx <= zone.x2() >> 4; cx++) {
            for (int cz = zone.z1() >> 4; cz <= zone.z2() >> 4; cz++) {
                if (!joueur.getUniqueId().equals(Integration.proprio(zone.monde().getChunkAt(cx, cz)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void faire(Player joueur, Zone zone, Bilan bilan) {
        String id = UUID.randomUUID().toString();
        World monde = zone.monde();
        Structure construction = Bukkit.getStructureManager().createStructure();
        construction.fill(new Location(monde, zone.x1(), zone.y1(), zone.z1()),
                new org.bukkit.util.BlockVector(zone.sx(), zone.sy(), zone.sz()), false);
        StringBuilder custom = new StringBuilder();
        List<int[]> positionsCustom = new ArrayList<>();
        for (int y = zone.y1(); y <= zone.y2(); y++) {
            for (int x = zone.x1(); x <= zone.x2(); x++) {
                for (int z = zone.z1(); z <= zone.z2(); z++) {
                    String c = Integration.custom(monde.getBlockAt(x, y, z));
                    if (c != null) {
                        int[] p = {x - zone.x1(), y - zone.y1(), z - zone.z1()};
                        positionsCustom.add(p);
                        custom.append(custom.isEmpty() ? "" : ";").append(p[0]).append(',').append(p[1]).append(',')
                                .append(p[2]).append(',').append(c);
                    }
                }
            }
        }
        int[] restants = bilan.restantsRelatifs.stream().mapToInt(Integer::intValue).toArray();
        // Face avant : le côté de la zone tourné vers le joueur.
        BlockFace avant = joueur.getFacing().getOppositeFace();
        Constructions.noter(construction, restants, custom.toString(), avant.name(),
                joueur.getUniqueId().toString(), bilan.emportes);
        try {
            Constructions.sauver(this, id, construction);
        } catch (IOException erreur) {
            getLogger().severe("Construction " + id + " non enregistrée : " + erreur);
            message(joueur, lang.c("message.erreur", "<red>Erreur : rien n'a été encapsulé."), true);
            return;
        }
        joueur.getInventory().setItemInMainHand(construction(id, zone, bilan.emportes));
        selections.remove(joueur.getUniqueId());
        getLogger().info(joueur.getName() + " a encapsulé " + bilan.emportes + " blocs (" + id + ") dans "
                + monde.getName() + " de " + zone.x1() + " " + zone.y1() + " " + zone.z1() + " à " + zone.x2() + " "
                + zone.y2() + " " + zone.z2() + ".");
        chantiers.retirer(id, zone, restants, positionsCustom);
        Contenant.fermer(joueur);
        message(joueur, lang.c("message.encapsule", "<green>Construction encapsulée : <n> blocs.",
                "n", bilan.emportes), false);
    }

    void message(Player joueur, Component texte, boolean refus) {
        Contenant.message(joueur, texte, refus);
        joueur.sendMessage(texte);
        lang.saveIfNeeded();
    }

    PersistentDataContainer donnees(ItemStack objet) {
        return objet.getItemMeta().getPersistentDataContainer();
    }
}
