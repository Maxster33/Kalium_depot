package fr.kalium.anticheat;

import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 1.2.0 (LeKiwi06, 09/10/2026 : « voir [...] leurs liste de claim ») : claims d'un joueur, en ligne ou hors ligne, lus
 * dans SimpleClaimSystem (facultatif : sans lui, la fiche d'un joueur n'a pas de bouton « Claims »). Lecture seule.
 */
final class Claims {

    /**
     * Un claim : nom et position (« Overworld, x 120 z -40 », centre du chunk, comme dans KS_Claim). 1.3.0 : arrivee, le
     * point du claim enregistré par SimpleClaimSystem (là où il a été créé), pour s'y téléporter ; null s'il manque.
     */
    record Ligne(String nom, String position, Location arrivee) {
    }

    private Claims() {
    }

    static boolean disponible() {
        return Bukkit.getPluginManager().isPluginEnabled("SimpleClaimSystem");
    }

    /** Claims possédés par ce joueur, triés par id ; null si SimpleClaimSystem est absent ou ne répond pas. */
    static List<Ligne> de(UUID joueur) {
        if (!disponible()) {
            return null;
        }
        try {
            // SCS 1.13.1 ne crée pas son API lui-même (sans effet si c'est déjà fait), voir KS_Claim 1.1.1.
            SimpleClaimSystemAPI_Provider.initialize((SimpleClaimSystem) Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
            List<Claim> claims = new ArrayList<>();
            for (Claim claim : SimpleClaimSystemAPI_Provider.getAPI().getAllClaims()) {
                if (joueur.equals(claim.getUUID())) {
                    claims.add(claim);
                }
            }
            claims.sort(Comparator.comparingInt(Claim::getId));
            List<Ligne> lignes = new ArrayList<>();
            for (Claim claim : claims) {
                Location arrivee = claim.getLocation();
                lignes.add(new Ligne(claim.getName(), position(claim),
                        arrivee == null || !arrivee.isWorldLoaded() ? null : arrivee.clone()));
            }
            return lignes;
        } catch (LinkageError | RuntimeException e) {
            Bukkit.getLogger().warning("[KS_AntiCheat] Claims illisibles dans SimpleClaimSystem : " + e);
            return null;
        }
    }

    private static String position(Claim claim) {
        if (claim.getChunks().isEmpty()) {
            return "?";
        }
        Chunk chunk = claim.getChunks().iterator().next();
        String monde = switch (chunk.getWorld().getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Overworld";
        };
        return monde + ", x " + (chunk.getX() * 16 + 8) + " z " + (chunk.getZ() * 16 + 8);
    }
}
