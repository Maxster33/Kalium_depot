package fr.kalium.tableau;

import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * Valeurs lues dans les autres plugins (chacun facultatif : null s'il manque ou s'il est trop ancien).
 */
final class Sources {

    private Sources() {
    }

    /** Score du joueur (KS_Economy). */
    static Long score(UUID joueur) {
        if (!Bukkit.getPluginManager().isPluginEnabled("KS_Economy")) {
            return null;
        }
        try {
            return fr.kalium.economy.KSEconomy.solde(joueur);
        } catch (LinkageError | RuntimeException e) {
            return null;
        }
    }

    /** Exploration journalière : { ouvertures du jour, limite } (KS_FairPlay 1.0.3). */
    static int[] exploration(UUID joueur) {
        if (!Bukkit.getPluginManager().isPluginEnabled("KS_FairPlay")) {
            return null;
        }
        try {
            if (!(Bukkit.getPluginManager().getPlugin("KS_FairPlay") instanceof fr.kalium.fairplay.KSFairPlay fp)) {
                return null;
            }
            return new int[]{fp.ouverturesDuJour(joueur), fp.limiteParJour()};
        } catch (LinkageError | RuntimeException e) {
            return null;
        }
    }

    /** Nombre de claims (SimpleClaimSystem). */
    static Integer claims(UUID joueur) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SimpleClaimSystem")) {
            return null;
        }
        try {
            fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                    Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
            return fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.getAPI().getPlayerClaimsCount(joueur);
        } catch (LinkageError | RuntimeException e) {
            return null;
        }
    }
}
