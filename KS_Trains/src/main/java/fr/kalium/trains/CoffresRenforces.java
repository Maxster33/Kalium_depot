package fr.kalium.trains;

import fr.kalium.coffrerenforce.KSCoffreRenforce;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 1.5.0 (LeKiwi06, 27/10/2026 : le minecart avec coffre renforcé de KS_CoffreRenforce 1.1.0 sert de wagon). Son stock
 * (jusqu'à 1 000 stacks d'un seul objet) n'est qu'en partie dans ses 27 cases : les entonnoirs du train passent par
 * KS_CoffreRenforce pour en prendre et y mettre. Sans KS_CoffreRenforce, rien ici n'est appelé (ses classes ne sont
 * jamais chargées).
 */
final class CoffresRenforces {

    private static boolean actif;

    private CoffresRenforces() {
    }

    static void activer(boolean present) {
        actif = present;
    }

    static boolean est(Entity wagon) {
        return actif && KSCoffreRenforce.estCoffreRenforce(wagon);
    }

    /** Coffre renforcé ou véhicule avec coffre renforcé (objet) : jamais un carburant de four. */
    static boolean objetRenforce(ItemStack objet) {
        return actif && KSCoffreRenforce.estObjetRenforce(objet);
    }

    /** Objet du minecart avec coffre renforcé (wagon brisé). */
    static ItemStack creerMinecart() {
        return KSCoffreRenforce.creerMinecart();
    }

    /** Objet stocké (un exemplaire), ou null si le coffre est vide. */
    static ItemStack objet(Entity wagon) {
        return KSCoffreRenforce.objet(wagon);
    }

    /** Nombre d'objets stockés, « 12 345 ». */
    static String nombre(Entity wagon) {
        return String.format(java.util.Locale.ROOT, "%,d", KSCoffreRenforce.nombre(wagon)).replace(',', ' ');
    }

    /** Un objet du coffre renforcé passe dans l'entonnoir, s'il y a de la place. */
    static boolean prendreUn(Entity wagon, Inventory entonnoir) {
        ItemStack objet = KSCoffreRenforce.objet(wagon);
        if (objet == null || !entonnoir.addItem(objet.asOne()).isEmpty()) {
            return false;
        }
        if (KSCoffreRenforce.retirer(wagon, 1) == 1) {
            return true;
        }
        entonnoir.removeItem(objet.asOne());
        return false;
    }

    /** Le premier objet de l'entonnoir que le coffre renforcé accepte y passe (un exemplaire). */
    static boolean donnerUn(Inventory entonnoir, Entity wagon) {
        for (int place = 0; place < entonnoir.getSize(); place++) {
            ItemStack objet = entonnoir.getItem(place);
            if (objet == null || objet.isEmpty()) {
                continue;
            }
            if (KSCoffreRenforce.ajouter(wagon, objet, 1) == 1) {
                objet.setAmount(objet.getAmount() - 1);
                entonnoir.setItem(place, objet.isEmpty() ? null : objet);
                return true;
            }
        }
        return false;
    }
}
