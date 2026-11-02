package fr.kalium.repeteurwifi;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** SimpleClaimSystem, seulement s'il est activé (comme KS_Encapsuleur). */
final class Claims {

    private Claims() {
    }

    /** Hors claim : tout le monde ; dans un claim : propriétaire, membres et ks_repeteurwifi.admin. */
    static boolean peutRegler(Player joueur, Block b) {
        if (joueur.hasPermission("ks_repeteurwifi.admin") || !Bukkit.getPluginManager().isPluginEnabled("SimpleClaimSystem")) {
            return true;
        }
        try {
            // SCS 1.13.1 ne crée pas son API lui-même (sans effet si un autre plugin l'a déjà fait).
            fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                    Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
            fr.xyness.SCS.API.SimpleClaimSystemAPI api = fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.getAPI();
            fr.xyness.SCS.Types.Claim claim = api.getClaimAtChunk(b.getChunk());
            return claim == null || joueur.getUniqueId().equals(claim.getUUID()) || api.isClaimMember(claim, joueur);
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }
}
