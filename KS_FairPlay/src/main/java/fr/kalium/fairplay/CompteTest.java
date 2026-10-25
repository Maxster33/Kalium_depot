package fr.kalium.fairplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.UUID;

/**
 * « Moi de test » (KS_MondeTest 1.1.0, demande de LeKiwi06 du 25/10/2026 : « faire la distinction du moi de Kixster et
 * du moi du monde de test ») : dans le monde de test, KS_MondeTest pose sur le joueur la metadonnee « ks_compte »,
 * l'identifiant de son compte de test (UUID de version 3 tire du sien). Les donnees d'un joueur qui s'y trouve sont
 * rangees sous cet identifiant : son compte de Kixster n'est jamais touche. Sans KS_MondeTest : rien ne change.
 * Meme classe dans chaque plugin concerne (aucune dependance entre plugins).
 */
final class CompteTest {

    private CompteTest() {
    }

    /** Compte a utiliser pour ce joueur : son compte de test s'il est dans le monde de test, sinon le sien. */
    static UUID de(UUID joueur) {
        if (joueur == null) {
            return null;
        }
        Player p = Bukkit.getPlayer(joueur);
        if (p != null) {
            for (MetadataValue v : p.getMetadata("ks_compte")) {
                try {
                    return UUID.fromString(v.asString());
                } catch (IllegalArgumentException ignored) {
                    // valeur illisible : compte normal
                }
            }
        }
        return joueur;
    }

    /** Le joueur est-il dans le monde de test ? */
    static boolean enTest(Player joueur) {
        return joueur != null && joueur.hasMetadata("ks_compte");
    }

    /** Est-ce un compte de test (a ne pas montrer dans les listes de joueurs) ? */
    static boolean estTest(UUID compte) {
        return compte != null && compte.version() == 3;
    }
}
