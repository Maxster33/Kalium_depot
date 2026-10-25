package fr.kalium.coffrerenforce;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Le stock d'un coffre renforcé, gardé dans le bloc lui-même (données du coffre) : l'objet stocké (un exemplaire) et
 * le nombre d'objets en réserve.
 *
 * Les 27 cases du vrai coffre servent de tampon pour la redstone : jamais ouvertes par un joueur, elles contiennent
 * une partie du stock, pour que les entonnoirs puissent en prendre ou en mettre et qu'un comparateur lise le
 * remplissage. Stock total = réserve + objets du tampon. Après chaque passage d'entonnoir (et chaque action du menu),
 * le tampon est recalculé :
 * - assez d'objets pour que le comparateur donne le signal du coffre entier (0 vide, 15 à 1 000 stacks) ;
 * - assez peu de place libre pour qu'un entonnoir ne dépasse jamais 1 000 stacks.
 */
final class Stockage {

    /** Capacité : 1 000 stacks de l'objet stocké. */
    static final int STACKS = 1000;
    private static final int CASES = 27;

    private final NamespacedKey cle;
    private final NamespacedKey cleObjet;
    private final NamespacedKey cleReserve;

    Stockage(NamespacedKey cle, NamespacedKey cleObjet, NamespacedKey cleReserve) {
        this.cle = cle;
        this.cleObjet = cleObjet;
        this.cleReserve = cleReserve;
    }

    /** Stock : objet stocké (un exemplaire, null si vide) et nombre total. */
    record Stock(ItemStack modele, long total) {

        int pile() {
            return modele == null ? 64 : modele.getMaxStackSize();
        }

        /** Nombre maximal pour cet objet (ou pour l'objet donné si le coffre est vide). */
        static long capacite(ItemStack objet) {
            return (long) STACKS * objet.getMaxStackSize();
        }

        long place() {
            return modele == null ? Long.MAX_VALUE : capacite(modele) - total;
        }

        long stacks() {
            return modele == null ? 0 : (total + pile() - 1) / pile();
        }
    }

    // ------------------------------------------------------------------ reconnaître

    boolean est(Block bloc) {
        return bloc != null && bloc.getType() == Material.CHEST && bloc.getState(false) instanceof Chest coffre
                && coffre.getPersistentDataContainer().has(cle);
    }

    /** Le bloc d'un inventaire (entonnoir, ouverture) s'il s'agit d'un coffre renforcé, sinon null. */
    Block bloc(Inventory inventaire) {
        if (inventaire == null || !(inventaire.getHolder(false) instanceof Chest coffre)
                || !coffre.getPersistentDataContainer().has(cle)) {
            return null;
        }
        return coffre.getBlock();
    }

    /** Un coffre renforcé vient d'être posé. */
    void marquer(Block bloc) {
        if (bloc.getState() instanceof Chest coffre) {
            coffre.getPersistentDataContainer().set(cle, PersistentDataType.BYTE, (byte) 1);
            coffre.update(true, false);
        }
    }

    /** Objets acceptés : tout, sauf les shulkers et les sacs qui contiennent quelque chose. */
    static boolean accepte(ItemStack objet) {
        if (objet == null || objet.getType().isAir()) {
            return false;
        }
        if (!objet.hasItemMeta()) {
            return true;
        }
        if (objet.getItemMeta() instanceof BundleMeta sac && sac.hasItems()) {
            return false;
        }
        return !(objet.getItemMeta() instanceof BlockStateMeta meta && meta.hasBlockState()
                && meta.getBlockState() instanceof ShulkerBox shulker && !shulker.getInventory().isEmpty());
    }

    // ------------------------------------------------------------------ lire et écrire

    /** Lecture seule (texte visé, entonnoirs) : rien n'est modifié. */
    Stock apercu(Block bloc) {
        if (!(bloc.getState(false) instanceof Chest coffre)) {
            return new Stock(null, 0);
        }
        PersistentDataContainer donnees = coffre.getPersistentDataContainer();
        ItemStack modele = modele(donnees);
        long total = donnees.getOrDefault(cleReserve, PersistentDataType.LONG, 0L);
        for (ItemStack objet : coffre.getBlockInventory().getContents()) {
            if (objet == null || objet.getType().isAir()) {
                continue;
            }
            if (modele == null && accepte(objet)) {
                modele = objet.asOne();
            }
            if (modele != null && modele.isSimilar(objet)) {
                total += objet.getAmount();
            }
        }
        return new Stock(total == 0 ? null : modele, total);
    }

    /**
     * Stock à jour avant une modification : les objets du tampon sont comptés ; un objet étranger (posé là par
     * autre chose qu'un entonnoir) est rejeté au-dessus du coffre.
     */
    Stock lire(Block bloc) {
        if (!(bloc.getState(false) instanceof Chest coffre)) {
            return new Stock(null, 0);
        }
        PersistentDataContainer donnees = coffre.getPersistentDataContainer();
        ItemStack modele = modele(donnees);
        long total = donnees.getOrDefault(cleReserve, PersistentDataType.LONG, 0L);
        Inventory tampon = coffre.getBlockInventory();
        for (int place = 0; place < tampon.getSize(); place++) {
            ItemStack objet = tampon.getItem(place);
            if (objet == null || objet.getType().isAir()) {
                continue;
            }
            if (modele == null && accepte(objet)) {
                modele = objet.asOne();
            }
            if (modele != null && modele.isSimilar(objet)) {
                total += objet.getAmount();
            } else {
                tampon.setItem(place, null);
                bloc.getWorld().dropItemNaturally(bloc.getLocation().add(0.5, 1.1, 0.5), objet);
            }
        }
        return new Stock(total == 0 ? null : modele, total);
    }

    /** Enregistre le stock : réserve dans les données du coffre, le reste dans le tampon. */
    void ecrire(Block bloc, Stock stock) {
        if (!(bloc.getState() instanceof Chest coffre)) {
            return;
        }
        long total = stock.modele() == null ? 0 : Math.max(0, stock.total());
        int pile = stock.pile();
        long dansTampon = tampon(total, pile);
        PersistentDataContainer donnees = coffre.getPersistentDataContainer();
        if (total == 0) {
            donnees.remove(cleObjet);
            donnees.remove(cleReserve);
        } else {
            donnees.set(cleObjet, PersistentDataType.BYTE_ARRAY, stock.modele().asOne().serializeAsBytes());
            donnees.set(cleReserve, PersistentDataType.LONG, total - dansTampon);
        }
        // update() remet aussi les objets de la copie : le tampon est donc écrit après, dans le vrai coffre.
        coffre.update(true, false);
        ItemStack[] contenu = new ItemStack[CASES];
        for (int place = 0; place < CASES && dansTampon > 0; place++) {
            int n = (int) Math.min(pile, dansTampon);
            contenu[place] = stock.modele().asQuantity(n);
            dansTampon -= n;
        }
        coffre.getBlockInventory().setContents(contenu);
    }

    private ItemStack modele(PersistentDataContainer donnees) {
        byte[] octets = donnees.get(cleObjet, PersistentDataType.BYTE_ARRAY);
        if (octets == null) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(octets);
        } catch (RuntimeException illisible) {
            return null;
        }
    }

    // ------------------------------------------------------------------ tampon et comparateur

    /** Nombre d'objets à laisser dans le tampon pour ce stock. */
    static long tampon(long total, int pile) {
        long capacite = (long) STACKS * pile;
        long plein = (long) CASES * pile;
        if (total <= 0) {
            return 0;
        }
        if (total >= capacite) {
            return Math.min(total, plein);
        }
        // Signal voulu, comme un coffre normal mais sur 1 000 stacks : 1 + 14 x remplissage (arrondi en dessous).
        int signal = (int) (total * 14 / capacite) + 1;
        long n = Math.max(1, ((signal - 1) * plein + 13) / 14);
        while (n < plein && signalTampon(n, pile) < signal) {
            n++;
        }
        while (n > 1 && signalTampon(n - 1, pile) >= signal) {
            n--;
        }
        // Place libre du tampon = place libre du coffre (au plus), pour qu'un entonnoir ne dépasse pas 1 000 stacks.
        n = Math.max(n, plein - (capacite - total));
        return Math.min(n, total);
    }

    /** Signal du comparateur pour un tampon de n objets, calculé comme le jeu (en float). */
    static int signalTampon(long n, int pile) {
        float remplissage = 0f;
        long pleines = n / pile;
        for (long i = 0; i < pleines; i++) {
            remplissage += (float) pile / (float) pile;
        }
        if (n % pile > 0) {
            remplissage += (float) (n % pile) / (float) pile;
        }
        remplissage /= CASES;
        return (int) Math.floor(remplissage * 14f) + (remplissage > 0f ? 1 : 0);
    }

    // ------------------------------------------------------------------ jamais de coffre double

    private static final List<BlockFace> COTES = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
            BlockFace.WEST);

    /**
     * Un coffre a été posé : si un coffre renforcé (ce bloc ou un voisin) s'est collé à un autre coffre, les deux
     * redeviennent des coffres simples.
     */
    void isolerAutour(Block bloc) {
        isoler(bloc);
        for (BlockFace cote : COTES) {
            isoler(bloc.getRelative(cote));
        }
    }

    private void isoler(Block bloc) {
        if (!est(bloc) || !(bloc.getState(false) instanceof Chest coffre)) {
            return;
        }
        InventoryHolder double_ = coffre.getInventory().getHolder(false);
        if (double_ instanceof DoubleChest dc) {
            for (InventoryHolder cote : new InventoryHolder[]{dc.getLeftSide(false), dc.getRightSide(false)}) {
                if (cote instanceof Chest moitie) {
                    simple(moitie.getBlock());
                }
            }
        }
        simple(bloc);
        // Un voisin qui se croit encore collé à ce coffre (moitié de coffre double sans l'autre moitié).
        for (BlockFace cote : COTES) {
            Block voisin = bloc.getRelative(cote);
            if (voisin.getType() == Material.CHEST && voisin.getBlockData() instanceof org.bukkit.block.data.type.Chest d
                    && d.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE
                    && voisin.getState(false) instanceof Chest c && !(c.getInventory().getHolder(false) instanceof DoubleChest)) {
                simple(voisin);
            }
        }
    }

    private static void simple(Block bloc) {
        if (bloc.getBlockData() instanceof org.bukkit.block.data.type.Chest d
                && d.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
            d.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
            bloc.setBlockData(d, false);
        }
    }
}
