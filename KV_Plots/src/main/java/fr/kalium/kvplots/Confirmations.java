package fr.kalium.kvplots;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Actions à confirmer dans la minute (« /plot supprimer » puis « /plot supprimer confirmer »). */
final class Confirmations {

    private static final long DELAI_MS = 60_000L;

    private record Demande(String action, int plot, long heure) {}

    private final Map<UUID, Demande> demandes = new HashMap<>();

    /** Note la demande ; elle sera confirmée par {@link #confirmer} dans la minute. */
    void demander(UUID joueur, String action, int plot) {
        demandes.put(joueur, new Demande(action, plot, System.currentTimeMillis()));
    }

    /** Vrai (et la demande est consommée) si le joueur a demandé cette action sur ce plot il y a moins d'une minute. */
    boolean confirmer(UUID joueur, String action, int plot) {
        Demande d = demandes.remove(joueur);
        return d != null && d.action().equals(action) && d.plot() == plot
                && System.currentTimeMillis() - d.heure() < DELAI_MS;
    }
}
