package fr.kalium.economy;

import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Chunk;

import java.util.UUID;

/** 1.1.0 : claims de SimpleClaimSystem (magasins). Classe chargée seulement si SimpleClaimSystem est activé. */
final class ClaimsSCS {

    private ClaimsSCS() {
    }

    static UUID proprio(Chunk chunk) {
        // 1.1.1 : SCS 1.13.1 ne crée pas son API lui-même (sans effet si KS_Claim l'a déjà fait).
        SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                org.bukkit.Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
        Claim claim = SimpleClaimSystemAPI_Provider.getAPI().getClaimAtChunk(chunk);
        return claim == null ? null : claim.getUUID();
    }
}
