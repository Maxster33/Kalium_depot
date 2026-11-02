package fr.kalium.shulkeramelioree;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Le contenu d'une shulker améliorée posée : 54 cases (un coffre double), gardées dans le bloc lui-même.
 *
 * Comme pour le coffre renforcé, les 27 cases de la vraie boîte de shulker servent de tampon pour la redstone : jamais
 * ouvertes par un joueur, elles contiennent une partie du contenu, pour que les entonnoirs puissent en prendre ou en
 * mettre et qu'un comparateur lise le remplissage. Données du bloc :
 * - « reserve » : les 54 cases, moins ce qui est dans le tampon ;
 * - « carte » : pour chaque case du tampon, la case des 54 dont elle contient une partie (-1 : aucune).
 * Contenu = réserve + tampon. Après chaque passage d'entonnoir (et chaque action dans le menu), le tampon est recalculé
 * pour que le comparateur donne le signal d'un coffre double qui aurait ces 54 cases.
 */
final class Contenu {

    static final int CASES = 54;
    static final int TAMPON = 27;

    private final NamespacedKey cle;
    private final NamespacedKey cleReserve;
    private final NamespacedKey cleCarte;

    Contenu(NamespacedKey cle, NamespacedKey cleReserve, NamespacedKey cleCarte) {
        this.cle = cle;
        this.cleReserve = cleReserve;
        this.cleCarte = cleCarte;
    }

    // ------------------------------------------------------------------ reconnaître

    boolean est(Block bloc) {
        return bloc != null && Tag.SHULKER_BOXES.isTagged(bloc.getType()) && bloc.getState(false) instanceof ShulkerBox s
                && s.getPersistentDataContainer().has(cle);
    }

    /** Objets refusés dans une shulker améliorée : toutes les boîtes de shulker, même vides. */
    static boolean refuse(ItemStack objet) {
        return objet != null && Tag.SHULKER_BOXES.isTagged(objet.getType());
    }

    static boolean vide(ItemStack[] cases) {
        for (ItemStack objet : cases) {
            if (!rien(objet)) {
                return false;
            }
        }
        return true;
    }

    static boolean rien(ItemStack objet) {
        return objet == null || objet.isEmpty();
    }

    // ------------------------------------------------------------------ lire et écrire

    /**
     * Les 54 cases. Avec un lieu de rejet, ce qui ne trouve pas de place (ou un objet refusé posé là par autre chose
     * qu'un entonnoir) y tombe ; sans lieu (aperçu, entonnoirs), il est seulement ignoré.
     */
    ItemStack[] lire(Block bloc, Location rejet) {
        if (!(bloc.getState(false) instanceof ShulkerBox shulker)) {
            return new ItemStack[CASES];
        }
        PersistentDataContainer donnees = shulker.getPersistentDataContainer();
        ItemStack[] cases = cases(donnees.get(cleReserve, PersistentDataType.BYTE_ARRAY));
        int[] carte = donnees.get(cleCarte, PersistentDataType.INTEGER_ARRAY);
        Inventory tampon = shulker.getInventory();
        List<ItemStack> surplus = new ArrayList<>();
        for (int j = 0; j < tampon.getSize(); j++) {
            ItemStack objet = tampon.getItem(j);
            if (rien(objet)) {
                continue;
            }
            int i = carte != null && j < carte.length ? carte[j] : -1;
            if (i >= 0 && i < CASES && !refuse(objet) && (rien(cases[i]) || cases[i].isSimilar(objet))) {
                int total = (rien(cases[i]) ? 0 : cases[i].getAmount()) + objet.getAmount();
                int pile = objet.getMaxStackSize();
                cases[i] = objet.asQuantity(Math.min(total, pile));
                if (total > pile) {
                    surplus.add(objet.asQuantity(total - pile));
                }
            } else {
                surplus.add(objet.clone());
            }
        }
        for (ItemStack objet : surplus) {
            ItemStack reste = refuse(objet) ? objet : ranger(cases, objet);
            if (reste != null && rejet != null) {
                rejet.getWorld().dropItemNaturally(rejet, reste);
            }
        }
        return cases;
    }

    /** Enregistre les 54 cases : réserve et carte dans les données du bloc, une partie dans le tampon. */
    void ecrire(Block bloc, ItemStack[] cases) {
        if (!(bloc.getState() instanceof ShulkerBox shulker)) {
            return;
        }
        ItemStack[] contenu = new ItemStack[CASES];
        for (int i = 0; i < CASES && i < cases.length; i++) {
            contenu[i] = rien(cases[i]) ? null : cases[i].clone();
        }
        ItemStack[] tampon = new ItemStack[TAMPON];
        int[] carte = new int[TAMPON];
        repartir(contenu, tampon, carte);
        ItemStack[] reserve = contenu.clone();
        for (int j = 0; j < TAMPON; j++) {
            if (carte[j] >= 0) {
                ItemStack reste = reserve[carte[j]];
                int n = reste.getAmount() - tampon[j].getAmount();
                reserve[carte[j]] = n <= 0 ? null : reste.asQuantity(n);
            }
        }
        PersistentDataContainer donnees = shulker.getPersistentDataContainer();
        donnees.set(cle, PersistentDataType.BYTE, (byte) 1);
        if (vide(contenu)) {
            donnees.remove(cleReserve);
            donnees.remove(cleCarte);
        } else {
            donnees.set(cleReserve, PersistentDataType.BYTE_ARRAY, octets(reserve));
            donnees.set(cleCarte, PersistentDataType.INTEGER_ARRAY, carte);
        }
        // update() remet aussi les objets de la copie : le tampon est donc écrit après, dans la vraie boîte.
        shulker.update(true, false);
        if (bloc.getState(false) instanceof ShulkerBox vraie) {
            vraie.getInventory().setContents(tampon);
        }
    }

    /** Une shulker améliorée vient d'être cassée : bloc vidé et démarqué (le jeu ne lâche plus rien de son contenu). */
    void vider(Block bloc) {
        if (!(bloc.getState() instanceof ShulkerBox shulker)) {
            return;
        }
        PersistentDataContainer donnees = shulker.getPersistentDataContainer();
        donnees.remove(cle);
        donnees.remove(cleReserve);
        donnees.remove(cleCarte);
        shulker.update(true, false);
        if (bloc.getState(false) instanceof ShulkerBox vraie) {
            vraie.getInventory().clear();
        }
    }

    // ------------------------------------------------------------------ cases

    /**
     * Range un objet dans les 54 cases (d'abord sur les piles identiques entamées, puis dans une case vide) ; renvoie
     * ce qui ne tient pas, ou null.
     */
    static ItemStack ranger(ItemStack[] cases, ItemStack objet) {
        int reste = objet.getAmount();
        int pile = objet.getMaxStackSize();
        for (int i = 0; i < cases.length && reste > 0; i++) {
            if (!rien(cases[i]) && cases[i].isSimilar(objet) && cases[i].getAmount() < pile) {
                int n = Math.min(reste, pile - cases[i].getAmount());
                cases[i] = cases[i].asQuantity(cases[i].getAmount() + n);
                reste -= n;
            }
        }
        for (int i = 0; i < cases.length && reste > 0; i++) {
            if (rien(cases[i])) {
                int n = Math.min(reste, pile);
                cases[i] = objet.asQuantity(n);
                reste -= n;
            }
        }
        return reste > 0 ? objet.asQuantity(reste) : null;
    }

    /** Est-ce que l'objet tient en entier dans les 54 cases ? */
    static boolean tient(ItemStack[] cases, ItemStack objet) {
        int reste = objet.getAmount();
        int pile = objet.getMaxStackSize();
        for (ItemStack dedans : cases) {
            if (rien(dedans)) {
                reste -= pile;
            } else if (dedans.isSimilar(objet)) {
                reste -= Math.max(0, pile - dedans.getAmount());
            }
            if (reste <= 0) {
                return true;
            }
        }
        return false;
    }

    static byte[] octets(ItemStack[] cases) {
        ItemStack[] copie = new ItemStack[cases.length];
        for (int i = 0; i < cases.length; i++) {
            copie[i] = rien(cases[i]) ? ItemStack.empty() : cases[i];
        }
        return ItemStack.serializeItemsAsBytes(copie);
    }

    /** 54 cases lues (null : vides) ; données absentes ou illisibles : 54 cases vides. */
    static ItemStack[] cases(byte[] octets) {
        ItemStack[] cases = new ItemStack[CASES];
        if (octets == null) {
            return cases;
        }
        try {
            ItemStack[] lues = ItemStack.deserializeItemsFromBytes(octets);
            for (int i = 0; i < CASES && i < lues.length; i++) {
                cases[i] = rien(lues[i]) ? null : lues[i];
            }
        } catch (RuntimeException illisible) {
            // 54 cases vides
        }
        return cases;
    }

    // ------------------------------------------------------------------ tampon et comparateur

    /**
     * Choisit le tampon : les plus grosses piles d'abord (entières, puis une partie de la suivante), jusqu'à la moitié
     * du remplissage des 54 cases : 27 cases à moitié du remplissage de 54 donnent le même signal. Le signal exact (le
     * jeu calcule en float) est ensuite ajusté objet par objet.
     */
    static void repartir(ItemStack[] cases, ItemStack[] tampon, int[] carte) {
        Arrays.fill(tampon, null);
        Arrays.fill(carte, -1);
        int voulu = signal(cases);
        if (voulu == 0) {
            return;
        }
        List<Integer> ordre = new ArrayList<>();
        double somme = 0;
        for (int i = 0; i < cases.length; i++) {
            if (!rien(cases[i])) {
                ordre.add(i);
                somme += fraction(cases[i]);
            }
        }
        ordre.sort(Comparator.comparingDouble((Integer i) -> -fraction(cases[i])).thenComparingInt(i -> i));
        double cible = somme / 2;
        double pris = 0;
        int n = 0;
        while (n < TAMPON && n < ordre.size()) {
            ItemStack pile = cases[ordre.get(n)];
            if (pris + fraction(pile) <= cible + 1e-9) {
                tampon[n] = pile.clone();
                carte[n] = ordre.get(n);
                pris += fraction(pile);
                n++;
                continue;
            }
            int partie = (int) Math.ceil((cible - pris) * pile.getMaxStackSize() - 1e-9);
            if (partie > 0) {
                tampon[n] = pile.asQuantity(Math.min(partie, pile.getAmount()));
                carte[n] = ordre.get(n);
                n++;
            }
            break;
        }
        // Ajustement : un objet de plus ou de moins dans la dernière case du tampon, sans jamais changer de sens.
        int sens = 0;
        for (int essai = 0; essai < 10_000; essai++) {
            int s = signal(tampon);
            if (s == voulu) {
                return;
            }
            int veut = s < voulu ? 1 : -1;
            if (sens != 0 && veut != sens) {
                return;
            }
            sens = veut;
            if (veut > 0) {
                if (n > 0 && tampon[n - 1].getAmount() < cases[carte[n - 1]].getAmount()) {
                    tampon[n - 1] = tampon[n - 1].asQuantity(tampon[n - 1].getAmount() + 1);
                } else if (n < TAMPON && n < ordre.size()) {
                    tampon[n] = cases[ordre.get(n)].asOne();
                    carte[n] = ordre.get(n);
                    n++;
                } else {
                    return;
                }
            } else if (n > 0 && tampon[n - 1].getAmount() > 1) {
                tampon[n - 1] = tampon[n - 1].asQuantity(tampon[n - 1].getAmount() - 1);
            } else if (n > 1) {
                n--;
                tampon[n] = null;
                carte[n] = -1;
            } else {
                return;
            }
        }
    }

    private static double fraction(ItemStack pile) {
        return pile.getAmount() / (double) pile.getMaxStackSize();
    }

    /** Signal d'un comparateur pour ces cases, calculé comme le jeu (en float, Mth.lerpDiscrete). */
    static int signal(ItemStack[] cases) {
        float remplissage = 0f;
        for (ItemStack objet : cases) {
            if (!rien(objet)) {
                remplissage += (float) objet.getAmount() / (float) objet.getMaxStackSize();
            }
        }
        remplissage /= cases.length;
        return (int) Math.floor(remplissage * 14f) + (remplissage > 0f ? 1 : 0);
    }
}
