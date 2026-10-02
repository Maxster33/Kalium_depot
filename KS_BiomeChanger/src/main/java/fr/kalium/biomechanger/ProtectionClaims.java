package fr.kalium.biomechanger;

import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.1.0 (cahier des charges : catégorie 3 « Claims », LeKiwi06, 30/09/2026) : le Changeur de Biome est refusé si la
 * zone modifiée touche un claim de SimpleClaimSystem dont le joueur n'est ni le propriétaire ni un membre.
 *
 * Classe chargée seulement si SimpleClaimSystem est activé (elle utilise ses classes).
 */
final class ProtectionClaims {

    private ProtectionClaims() {
    }

    /** Vrai si l'une des cellules de 4 x 4 x 4 blocs (qx, qy, qz) est dans un claim où le joueur n'est pas membre. */
    static boolean touche(World monde, List<int[]> cellules, Player joueur) {
        Set<Long> vus = new HashSet<>();
        for (int[] cellule : cellules) {
            int chunkX = cellule[0] >> 2;
            int chunkZ = cellule[2] >> 2;
            if (!vus.add(((long) chunkX << 32) ^ (chunkZ & 0xffffffffL))) {
                continue;
            }
            Claim claim = SimpleClaimSystemAPI_Provider.getAPI().getClaimAtChunk(monde.getChunkAt(chunkX, chunkZ));
            // Le propriétaire fait partie des membres d'un claim de SCS.
            if (claim != null && !claim.isMember(joueur.getUniqueId())) {
                return true;
            }
        }
        return false;
    }
}
