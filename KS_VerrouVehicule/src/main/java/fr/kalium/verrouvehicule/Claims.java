package fr.kalium.verrouvehicule;

import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Claims de SimpleClaimSystem. Classe chargée seulement si SimpleClaimSystem est activé (elle utilise ses classes).
 */
final class Claims {

    private Claims() {
    }

    private static Claim claim(Location lieu) {
        // SCS 1.13.1 ne crée pas son API lui-même (sans effet si c'est déjà fait).
        SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
        return SimpleClaimSystemAPI_Provider.getAPI().getClaimAtChunk(lieu.getChunk());
    }

    /** Vrai si le lieu est dans un claim dont le joueur est le propriétaire ou un membre (SCS compte le propriétaire). */
    static boolean membre(Player joueur, Location lieu) {
        Claim claim = claim(lieu);
        return claim != null && claim.isMember(joueur.getUniqueId());
    }

    /** Vrai si le lieu est dans un claim dont le joueur n'est ni le propriétaire ni un membre. */
    static boolean chezUnAutre(Player joueur, Location lieu) {
        Claim claim = claim(lieu);
        return claim != null && !claim.isMember(joueur.getUniqueId());
    }
}
