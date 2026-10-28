package fr.kalium.alcools;

import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import java.util.UUID;

/**
 * Claims de SimpleClaimSystem : le Molotov et la torche augmentée n'agissent pas dans le claim d'un autre joueur.
 *
 * Classe chargée seulement si SimpleClaimSystem est activé (elle utilise ses classes).
 */
final class ProtectionClaims {

    private ProtectionClaims() {
    }

    /** Vrai si le chunk est dans un claim dont le joueur n'est pas membre (joueur null : tout claim). */
    static boolean refuse(Chunk chunk, UUID joueur) {
        // SCS 1.13.1 ne crée pas son API lui-même (sans effet si c'est déjà fait).
        SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
        Claim claim = SimpleClaimSystemAPI_Provider.getAPI().getClaimAtChunk(chunk);
        // Le propriétaire fait partie des membres d'un claim de SCS.
        return claim != null && (joueur == null || !claim.isMember(joueur));
    }
}
