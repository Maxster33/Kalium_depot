package fr.kalium.marteau;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Magnétisme (cahier § 3 et 6) : un marteau tenu en main (ou dans l'autre main) avec Magnétisme attire vers le joueur
 * les objets au sol dans un rayon de 3 / 5 / 7 blocs (niveaux I à III). Pas les orbes d'XP ; pas les objets situés
 * dans un claim où le joueur n'a pas les droits ; pas les objets qu'il ne peut pas encore ramasser (délai du jeu après
 * un lancer, objet réservé à un autre joueur).
 *
 * Tâche répétée : KS_MondeTest ne la coupe pas lui-même, elle demande à PhaseTest si le plugin est ouvert dans le monde
 * du joueur.
 */
final class Magnetisme {

    private static final long PERIODE = 4L;

    private final Plugin plugin;
    private final boolean claims;

    Magnetisme(Plugin plugin, boolean claims) {
        this.plugin = plugin;
        this.claims = claims;
    }

    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::attirer, PERIODE, PERIODE);
    }

    static int rayon(int niveau) {
        return 1 + 2 * niveau;
    }

    private void attirer() {
        Enchantment m = KSMarteau.magnetisme();
        if (m == null) {
            return;
        }
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            if (joueur.getGameMode() == GameMode.SPECTATOR || joueur.isDead()) {
                continue;
            }
            int niveau = Math.max(niveau(joueur.getInventory().getItemInMainHand(), m),
                    niveau(joueur.getInventory().getItemInOffHand(), m));
            if (niveau <= 0 || !PhaseTest.ouvert("KS_Marteau", joueur.getWorld())) {
                continue;
            }
            int r = rayon(niveau);
            Location centre = joueur.getLocation().add(0, 0.5, 0);
            UUID id = joueur.getUniqueId();
            for (Item objet : joueur.getWorld().getNearbyEntitiesByType(Item.class, centre, r)) {
                if (objet.isDead() || objet.getPickupDelay() > 0 || !objet.canPlayerPickup()
                        || (objet.getOwner() != null && !objet.getOwner().equals(id))) {
                    continue;
                }
                Location lieu = objet.getLocation();
                if (lieu.distanceSquared(centre) > (double) r * r) {
                    continue;
                }
                if (claims && ProtectionClaims.refuse(lieu.getChunk(), id)) {
                    continue;
                }
                Vector vers = centre.toVector().subtract(lieu.toVector());
                double distance = vers.length();
                if (distance < 0.6) {
                    continue; // déjà au contact : le jeu le ramasse
                }
                objet.setVelocity(vers.multiply(Math.min(0.5, 0.15 + distance * 0.06) / distance));
            }
        }
    }

    private static int niveau(ItemStack objet, Enchantment m) {
        return KSMarteau.niveau(objet) != null ? objet.getEnchantmentLevel(m) : 0;
    }
}
