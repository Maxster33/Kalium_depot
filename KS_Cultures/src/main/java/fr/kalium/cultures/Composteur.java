package fr.kalium.cultures;

import fr.kalium.menu.api.Contenant;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Interface du composteur (cahier local, parties 10, 11 et 13) : clic droit main vide sur un composteur (sauf plein :
 * on y prend sa poudre d'os comme en vanilla). Menu en coffre :
 *
 * <ul>
 *   <li>en haut, ce que le joueur porte et que le composteur accepte : fleurs (1 à 3 graines chacune), fruits des
 *       végétaux découverts (1 graine chacun), graines (à stocker dans le composteur) ;</li>
 *   <li>en bas : graines stockées (menu pour les reprendre), mélange de graines (dès 9 sortes stockées : 1 de chacune
 *       des 9 sortes les plus nombreuses -> 1 mélange), fermer.</li>
 * </ul>
 *
 * Le stockage est propre à chaque composteur (données du chunk), 64 graines au plus par sorte ; il tombe au sol si le
 * composteur est cassé, explosé ou brûlé ; un piston ne pousse pas un composteur qui stocke des graines.
 */
final class Composteur implements Listener {

    private static final int PAR_SORTE = 64;
    private static final int SORTES_DU_MELANGE = 9;
    private static final Set<Material> GRAINES_DU_JEU = Set.of(Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS,
            Material.MELON_SEEDS, Material.PUMPKIN_SEEDS, Material.TORCHFLOWER_SEEDS, Material.PITCHER_POD);

    /** Menu ouvert : composteur et page (accueil ou graines stockées). */
    private record Marque(String page, Block composteur) {
    }

    private enum Sorte { FLEUR, FRUIT, GRAINE }

    private final KSCultures plugin;

    Composteur(KSCultures plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ ouverture

    @EventHandler(priority = EventPriority.HIGH)
    public void onClic(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || b == null || b.getType() != Material.COMPOSTER
                || event.getHand() != EquipmentSlot.HAND || event.getItem() != null
                || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        if (b.getBlockData() instanceof Levelled l && l.getLevel() >= l.getMaximumLevel()) {
            return; // poudre d'os prête : vanilla
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        accueil(event.getPlayer(), b);
    }

    private boolean present(Player joueur, Block b) {
        return b.getType() == Material.COMPOSTER && joueur.getWorld().equals(b.getWorld())
                && joueur.getLocation().distanceSquared(b.getLocation().add(0.5, 0.5, 0.5)) < 64;
    }

    // ------------------------------------------------------------------ ce que le joueur porte

    /** Ce que fait le composteur d'un objet (null : rien). */
    private Sorte sorte(Player joueur, ItemStack o) {
        if (o == null || o.getType().isAir()) {
            return null;
        }
        String id = KSCultures.id(o);
        if (id == null) {
            if (o.getType() == Material.KNOWLEDGE_BOOK) {
                return null;
            }
            if (Fleurs.graineDe(o.getType()) != null) {
                return Sorte.FLEUR;
            }
            return GRAINES_DU_JEU.contains(o.getType()) ? Sorte.GRAINE : null;
        }
        if (Fleurs.GRAINES.containsKey(id) || Vegetal.parGraine(id) != null || id.equals(Vegetal.RIZ.fruit())) {
            return Sorte.GRAINE;
        }
        Vegetal v = Vegetal.parFruit(id);
        return v != null && v.aDesGraines() ? Sorte.FRUIT : null;
    }

    /** Les objets du joueur que le composteur accepte : clé -> nombre (dans l'ordre de l'inventaire). */
    private Map<String, Integer> portes(Player joueur) {
        Map<String, Integer> res = new LinkedHashMap<>();
        for (ItemStack o : joueur.getInventory().getStorageContents()) {
            if (sorte(joueur, o) != null) {
                res.merge(KSCultures.cle(o), o.getAmount(), Integer::sum);
            }
        }
        return res;
    }

    /** Retire jusqu'à n objets de cette clé de l'inventaire ; renvoie le nombre retiré. */
    private int retirer(Player joueur, String cle, int n) {
        int reste = n;
        ItemStack[] contenu = joueur.getInventory().getStorageContents();
        for (int i = 0; i < contenu.length && reste > 0; i++) {
            ItemStack o = contenu[i];
            if (sorte(joueur, o) == null || !cle.equals(KSCultures.cle(o))) {
                continue;
            }
            int pris = Math.min(reste, o.getAmount());
            o.setAmount(o.getAmount() - pris);
            reste -= pris;
        }
        joueur.getInventory().setStorageContents(contenu);
        return n - reste;
    }

    private static void donner(Player joueur, String cle, int n) {
        while (n > 0) {
            int pile = Math.min(n, 64);
            ItemStack o = KSCultures.objetCle(cle, pile);
            if (o == null) {
                return;
            }
            for (ItemStack reste : joueur.getInventory().addItem(o).values()) {
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
            }
            n -= pile;
        }
    }

    // ------------------------------------------------------------------ menus

    private void accueil(Player joueur, Block b) {
        Contenant menu = Contenant.pour(joueur, plugin.t("menu.titre", "Composteur"), new Marque("accueil", b));
        int place = 0;
        for (Map.Entry<String, Integer> e : portes(joueur).entrySet()) {
            if (place >= Contenant.PAR_PAGE) {
                break;
            }
            ItemStack modele = KSCultures.objetCle(e.getKey(), Math.min(64, e.getValue()));
            if (modele == null) {
                continue;
            }
            Sorte s = sorte(joueur, modele);
            List<Component> lignes = new ArrayList<>();
            lignes.add(plugin.t("menu.tu-en-as", "<gray>Tu en as <white><n>", "n", e.getValue()));
            String cle = e.getKey();
            java.util.function.Consumer<Player> action = null;
            if (s == Sorte.FLEUR) {
                lignes.addAll(Contenant.lignes(plugin.t("menu.fleur", "<yellow>Clic : tout changer en graines (1 à 3 "
                        + "par fleur)")));
                action = p -> composterFleurs(p, b, cle);
            } else if (s == Sorte.FRUIT) {
                Vegetal v = Vegetal.parFruit(cle);
                if (plugin.decouvert(joueur, v)) {
                    lignes.addAll(Contenant.lignes(plugin.t("menu.fruit", "<yellow>Clic : tout changer en graines (1 "
                            + "par fruit)")));
                    action = p -> composterFruits(p, b, cle);
                } else {
                    lignes.addAll(Contenant.lignes(plugin.t("menu.fruit-inconnu", "<red>Récolte d'abord ce végétal mûr "
                            + "pour en tirer des graines")));
                }
            } else {
                int stocke = stock(b).getOrDefault(cle, 0);
                lignes.add(plugin.t("menu.stock", "<gray>Dans le composteur : <white><n>/<max>", "n", stocke,
                        "max", PAR_SORTE));
                if (stocke < PAR_SORTE) {
                    lignes.addAll(Contenant.lignes(plugin.t("menu.stocker", "<yellow>Clic : les stocker dans le "
                            + "composteur")));
                    action = p -> stocker(p, b, cle);
                } else {
                    lignes.add(plugin.t("menu.plein", "<red>Le composteur en est plein"));
                }
            }
            menu.poser(place++, Contenant.objet(modele, null, lignes), action);
        }
        if (place == 0) {
            menu.poser(22, Contenant.objet(Material.COMPOSTER, plugin.t("menu.rien", "<gray>Rien à composter"),
                    Contenant.lignes(plugin.t("menu.rien-aide", "<gray>Apporte des fleurs, des graines ou les fruits "
                            + "des végétaux que tu as découverts"))), null);
        }
        menu.barre();
        menu.poser(menu.bas(3), aide(), null);
        Map<String, Integer> stock = stock(b);
        menu.poser(menu.bas(4), Contenant.objet(Material.CHEST, plugin.t("menu.stockees", "<gold>Graines stockées"),
                List.of(plugin.t("menu.stockees-sortes", "<gray><n> sortes", "n", stock.size()),
                        plugin.t("menu.stockees-clic", "<yellow>Clic : voir, reprendre"))),
                p -> stockees(p, b));
        menu.poser(menu.bas(5), melange(stock), stock.size() >= SORTES_DU_MELANGE ? p -> faireMelange(p, b) : null);
        menu.poser(menu.bas(6), Contenant.objet(Material.BARRIER, plugin.t("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        menu.ouvrir(joueur);
    }

    private ItemStack aide() {
        List<Component> lignes = new ArrayList<>();
        lignes.addAll(Contenant.lignes(plugin.t("menu.aide-1", "<gray>Fleur : 1 à 3 graines, qui poussent sur la "
                + "terre labourée.")));
        lignes.addAll(Contenant.lignes(plugin.t("menu.aide-2", "<gray>Végétal récolté mûr une fois : ses fruits "
                + "donnent des graines.")));
        lignes.addAll(Contenant.lignes(plugin.t("menu.aide-3", "<gray>9 sortes de graines stockées : un mélange "
                + "de graines, qui fait pousser un végétal au hasard.")));
        return Contenant.objet(Material.BOOK, plugin.t("menu.aide", "<yellow>Aide"), lignes);
    }

    private ItemStack melange(Map<String, Integer> stock) {
        ItemStack o = KSCultures.objet(KSCultures.MELANGE, 1);
        List<Component> lignes = new ArrayList<>();
        if (stock.size() >= SORTES_DU_MELANGE) {
            lignes.addAll(Contenant.lignes(plugin.t("menu.melange-clic", "<yellow>Clic : 1 graine de 9 sortes "
                    + "-> 1 mélange")));
            ItemMeta meta = o.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            o.setItemMeta(meta);
        } else {
            lignes.add(plugin.t("menu.melange-manque", "<red>Il faut 9 sortes (<n>/9)", "n", stock.size()));
        }
        return Contenant.objet(o, plugin.t("menu.melange", "<gold>Mélange de graines"), lignes);
    }

    private void stockees(Player joueur, Block b) {
        if (!present(joueur, b)) {
            Contenant.fermer(joueur);
            return;
        }
        Contenant menu = Contenant.pour(joueur, plugin.t("menu.stockees-titre", "Graines stockées"),
                new Marque("stock", b));
        int place = 0;
        for (Map.Entry<String, Integer> e : stock(b).entrySet()) {
            ItemStack modele = KSCultures.objetCle(e.getKey(), Math.min(64, e.getValue()));
            if (modele == null || place >= Contenant.PAR_PAGE) {
                continue;
            }
            String cle = e.getKey();
            menu.poser(place++, Contenant.objet(modele, null, List.of(
                    plugin.t("menu.stock", "<gray>Dans le composteur : <white><n>/<max>", "n", e.getValue(),
                            "max", PAR_SORTE),
                    plugin.t("menu.reprendre", "<yellow>Clic : les reprendre"))), p -> reprendre(p, b, cle));
        }
        menu.barre();
        menu.poser(menu.bas(4), Contenant.objet(Material.OAK_DOOR, plugin.t("menu.retour", "<yellow>Retour"),
                List.of()), p -> accueil(p, b));
        menu.ouvrir(joueur);
    }

    // ------------------------------------------------------------------ actions

    private void composterFleurs(Player joueur, Block b, String cle) {
        if (!present(joueur, b)) {
            Contenant.fermer(joueur);
            return;
        }
        Material fleur = Material.matchMaterial(cle);
        String graine = fleur == null ? null : Fleurs.graineDe(fleur);
        int n = graine == null ? 0 : retirer(joueur, cle, Integer.MAX_VALUE);
        int graines = 0;
        for (int i = 0; i < n; i++) {
            graines += 1 + ThreadLocalRandom.current().nextInt(3);
        }
        donner(joueur, graine == null ? "" : graine, graines);
        Contenant.message(joueur, plugin.t("message.graines", "<green>+<n> graines", "n", graines), false);
        accueil(joueur, b);
    }

    private void composterFruits(Player joueur, Block b, String cle) {
        Vegetal v = Vegetal.parFruit(cle);
        if (!present(joueur, b) || v == null || !plugin.decouvert(joueur, v)) {
            Contenant.fermer(joueur);
            return;
        }
        int n = retirer(joueur, cle, Integer.MAX_VALUE);
        donner(joueur, v.graine(), n);
        Contenant.message(joueur, plugin.t("message.graines", "<green>+<n> graines", "n", n), false);
        accueil(joueur, b);
    }

    private void stocker(Player joueur, Block b, String cle) {
        if (!present(joueur, b)) {
            Contenant.fermer(joueur);
            return;
        }
        Map<String, Integer> stock = stock(b);
        int place = PAR_SORTE - stock.getOrDefault(cle, 0);
        int n = place <= 0 ? 0 : retirer(joueur, cle, place);
        if (n > 0) {
            stock.merge(cle, n, Integer::sum);
            ecrire(b, stock);
        }
        Contenant.message(joueur, plugin.t("message.stockees", "<green><n> graines stockées", "n", n), n == 0);
        accueil(joueur, b);
    }

    private void reprendre(Player joueur, Block b, String cle) {
        if (!present(joueur, b)) {
            Contenant.fermer(joueur);
            return;
        }
        Map<String, Integer> stock = stock(b);
        Integer n = stock.remove(cle);
        if (n != null) {
            ecrire(b, stock);
            donner(joueur, cle, n);
        }
        stockees(joueur, b);
    }

    private void faireMelange(Player joueur, Block b) {
        if (!present(joueur, b)) {
            Contenant.fermer(joueur);
            return;
        }
        Map<String, Integer> stock = stock(b);
        if (stock.size() < SORTES_DU_MELANGE) {
            accueil(joueur, b);
            return;
        }
        // les 9 sortes les plus nombreuses : le joueur garde plus longtemps ses sortes rares
        List<String> pris = stock.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(SORTES_DU_MELANGE).map(Map.Entry::getKey).toList();
        for (String c : pris) {
            int n = stock.get(c) - 1;
            if (n <= 0) {
                stock.remove(c);
            } else {
                stock.put(c, n);
            }
        }
        ecrire(b, stock);
        donner(joueur, KSCultures.MELANGE, 1);
        Contenant.message(joueur, plugin.t("message.melange", "<green>+1 mélange de graines"), false);
        accueil(joueur, b);
    }

    // ------------------------------------------------------------------ stockage (données du chunk)

    private NamespacedKey cle(Block b) {
        return new NamespacedKey(plugin, "composteur_" + b.getX() + "_" + b.getY() + "_" + b.getZ());
    }

    private static NamespacedKey cleGraine(String cle) {
        return cle.startsWith("minecraft:") ? NamespacedKey.fromString(cle) : new NamespacedKey("ks_cultures", cle);
    }

    /** Graines stockées dans ce composteur : clé -> nombre. */
    private Map<String, Integer> stock(Block b) {
        Map<String, Integer> res = new LinkedHashMap<>();
        PersistentDataContainer c = b.getChunk().getPersistentDataContainer()
                .get(cle(b), PersistentDataType.TAG_CONTAINER);
        if (c != null) {
            for (NamespacedKey k : c.getKeys()) {
                Integer n = c.get(k, PersistentDataType.INTEGER);
                if (n != null && n > 0) {
                    res.put(k.getNamespace().equals("minecraft") ? k.asString() : k.getKey(), n);
                }
            }
        }
        return res;
    }

    private void ecrire(Block b, Map<String, Integer> stock) {
        PersistentDataContainer chunk = b.getChunk().getPersistentDataContainer();
        if (stock.isEmpty()) {
            chunk.remove(cle(b));
            return;
        }
        PersistentDataContainer c = chunk.getAdapterContext().newPersistentDataContainer();
        stock.forEach((k, n) -> c.set(cleGraine(k), PersistentDataType.INTEGER, n));
        chunk.set(cle(b), PersistentDataType.TAG_CONTAINER, c);
    }

    /** Le composteur disparaît : ses graines tombent au sol. */
    private void vider(Block b) {
        Map<String, Integer> stock = stock(b);
        if (stock.isEmpty()) {
            return;
        }
        b.getChunk().getPersistentDataContainer().remove(cle(b));
        stock.forEach((k, n) -> {
            while (n > 0) {
                int pile = Math.min(n, 64);
                ItemStack o = KSCultures.objetCle(k, pile);
                if (o == null) {
                    break;
                }
                b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), o);
                n -= pile;
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCasse(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.COMPOSTER) {
            vider(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFeu(BlockBurnEvent event) {
        if (event.getBlock().getType() == Material.COMPOSTER) {
            vider(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().stream().filter(b -> b.getType() == Material.COMPOSTER).forEach(this::vider);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent event) {
        event.blockList().stream().filter(b -> b.getType() == Material.COMPOSTER).forEach(this::vider);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> b.getType() == Material.COMPOSTER && !stock(b).isEmpty())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> b.getType() == Material.COMPOSTER && !stock(b).isEmpty())) {
            event.setCancelled(true);
        }
    }
}
