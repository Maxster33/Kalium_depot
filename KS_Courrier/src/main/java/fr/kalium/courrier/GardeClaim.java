package fr.kalium.courrier;

import fr.kalium.claim.api.GardienClaim;
import fr.kalium.courrier.Donnees.Boite;
import org.bukkit.Chunk;

import java.util.UUID;

/**
 * KS_Claim 1.6.0 : le claim qui porte la boîte aux lettres de son propriétaire ne peut être ni supprimé ni vendu
 * (LeKiwi06, 02/11/2026). Chargé seulement si KS_Claim est actif.
 */
final class GardeClaim implements GardienClaim {

    private final Donnees donnees;

    GardeClaim(Donnees donnees) {
        this.donnees = donnees;
    }

    @Override
    public String refus(UUID proprio, Chunk chunk) {
        Boite b = donnees.boite(proprio);
        if (b == null || !b.monde().equals(chunk.getWorld().getUID()) || b.x() >> 4 != chunk.getX()
                || b.z() >> 4 != chunk.getZ()) {
            return null;
        }
        return "Ce claim porte ta boîte aux lettres : casse-la d'abord (ton courrier est gardé).";
    }
}
