package fr.kalium.relay;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre "ce joueur est actuellement EN PARTIE sur tel serveur" - AJOUTE pour la reconnexion en
 * cours de partie (demande explicite de l'utilisateur, 23/09/2026 : "si un joueur est deconnecte
 * durant une partie et qu'il se reconnecte avant la fin de la partie il faut que le proxy le
 * renvoi directement sur la partie"). Alimente par KalBingo (POST/DELETE HTTP, voir
 * RelayHttpServer) au lancement d'une partie ; lu directement en memoire (meme JVM, pas de HTTP
 * necessaire) par KaliumRelay.onChooseInitialServer (PlayerChooseInitialServerEvent).
 *
 * Contrairement au store d'affectations (RelayHttpServer.Entry, purge apres 2 minutes - une
 * affectation non reclamee est une anomalie), une entree ici est censee vivre potentiellement
 * plusieurs heures (toute la duree d'une partie Bingo, 1h par defaut - voir KalBingo config.yml
 * game.default-duration-seconds). Les entrees sont retirees par KalBingo en fin de partie (DELETE,
 * voir RelayClient.clearActiveGame / GameEndService, depuis KalBingo 0.1.12) ou quand un joueur
 * abandonne ; la purge de securite ci-dessous (6h) ne sert que de filet si ce retrait echoue (relais
 * injoignable, crash du serveur Bingo...).
 */
final class ActiveGameRegistry {

    /** Purge de securite tres large (pas une vraie expiration de partie, juste un garde-fou memoire). */
    private static final long ENTRY_TTL_MILLIS = 6L * 60 * 60 * 1000; // 6h

    private record Entry(String serverName, long storedAt) { }

    private final Map<UUID, Entry> active = new ConcurrentHashMap<>();

    void set(UUID playerId, String serverName) {
        active.put(playerId, new Entry(serverName, System.currentTimeMillis()));
    }

    void clear(UUID playerId) {
        active.remove(playerId);
    }

    Optional<String> get(UUID playerId) {
        Entry entry = active.get(playerId);
        return entry == null ? Optional.empty() : Optional.of(entry.serverName());
    }

    void sweep() {
        long now = System.currentTimeMillis();
        active.entrySet().removeIf(e -> now - e.getValue().storedAt() > ENTRY_TTL_MILLIS);
    }
}
