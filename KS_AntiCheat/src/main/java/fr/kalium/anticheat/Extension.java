package fr.kalium.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 1.1.1 : accès à l'extension du coffre de l'Ender de KS_EC_Extension 1.1.0 (facultatif : sans lui, ou trop ancien, le
 * coffre de l'Ender n'a que ses 27 cases vanilla).
 */
final class Extension {

    private static final int EC = 27;

    private Extension() {
    }

    static boolean active() {
        if (!Bukkit.getPluginManager().isPluginEnabled("KS_EC_Extension")) {
            return false;
        }
        try {
            fr.kalium.ecextension.KSECExtension.class.getMethod("ecrireExtension", Player.class, ItemStack[].class);
            return true;
        } catch (NoSuchMethodException | LinkageError e) {
            return false;
        }
    }

    static boolean ouvert(Player p) {
        try {
            return active() && fr.kalium.ecextension.KSECExtension.coffreOuvert(p);
        } catch (LinkageError e) {
            return false;
        }
    }

    /** Ferme (en l'enregistrant) le coffre de l'Ender du joueur s'il est ouvert. */
    static void fermer(Player p) {
        try {
            if (active()) {
                fr.kalium.ecextension.KSECExtension.fermerCoffre(p);
            }
        } catch (LinkageError e) {
            // sans KS_EC_Extension : rien à fermer
        }
    }

    static ItemStack[] lire(Player p) {
        try {
            ItemStack[] bas = fr.kalium.ecextension.KSECExtension.extension(p);
            return java.util.Arrays.copyOf(bas, EC);
        } catch (LinkageError e) {
            return new ItemStack[EC];
        }
    }

    static int masque(Player p) {
        try {
            return fr.kalium.ecextension.KSECExtension.casesDebloquees(p);
        } catch (LinkageError e) {
            return 0;
        }
    }

    static void ecrire(Player p, ItemStack[] bas) {
        try {
            fr.kalium.ecextension.KSECExtension.ecrireExtension(p, bas);
        } catch (LinkageError e) {
            Bukkit.getLogger().warning("[KS_AntiCheat] Extension du coffre de l'Ender non écrite : " + e);
        }
    }

    static ItemStack image() {
        try {
            return fr.kalium.ecextension.KSECExtension.imageCaseBloquee();
        } catch (LinkageError e) {
            return new ItemStack(Material.BARRIER);
        }
    }
}
