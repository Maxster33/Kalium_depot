package fr.kalium.coffremort;

import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Claims de SimpleClaimSystem à l'emplacement d'un coffre de mort.
 *
 * Classe chargée seulement si SimpleClaimSystem est activé (elle utilise ses classes).
 */
final class ProtectionClaims {

    private ProtectionClaims() {
    }

    private static Claim claim(Block bloc) {
        // SCS 1.13.1 ne crée pas son API lui-même (sans effet si c'est déjà fait).
        SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
        return SimpleClaimSystemAPI_Provider.getAPI().getClaimAtChunk(bloc.getChunk());
    }

    /** Vrai si le bloc est dans le claim d'un autre joueur (1.0.1 : même s'il en est membre). */
    static boolean dUnAutre(Block bloc, Player joueur) {
        Claim claim = claim(bloc);
        return claim != null && !joueur.getUniqueId().equals(claim.getUUID());
    }
}
