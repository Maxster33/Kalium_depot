package fr.kalium.bingo.network;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Client HTTP vers KaliumRelay (plugin Velocity separe, sur le proxy) - COMPLEMENT au canal
 * BungeeCord habituel (voir AssignmentService), pas un remplacement : un appel HTTP direct ne
 * depend d'aucun joueur connecte, ni ici ni sur kal-games, contrairement au canal reseau du jeu
 * (Forward), qui a besoin d'un joueur present sur le serveur CIBLE pour livrer un message - limite
 * du protocole Minecraft lui-meme, responsable du bug ou un second joueur qui rejoignait restait
 * bloque puis etait expulse (voir JOURNAL.md, correctif qui a introduit cette classe). Le canal
 * BungeeCord reste actif en parallele comme filet de securite si le relais n'est pas configure
 * (relay-url vide) ou momentanement injoignable.
 */
public final class RelayClient {

    private final JavaPlugin plugin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public RelayClient(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Bloquant - a appeler UNIQUEMENT depuis un thread asynchrone (jamais le thread principal du serveur). */
    public String fetch(UUID playerId) {
        String url = plugin.getConfig().getString("network.relay-url", "");
        if (url == null || url.isBlank()) {
            return null;
        }
        String token = plugin.getConfig().getString("network.relay-token", "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/assignment/" + playerId))
                    .header("X-Kalium-Relay-Token", token)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200 ? response.body() : null;
        } catch (IOException | InterruptedException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            return null; // pas grave : le canal BungeeCord habituel prend le relais (voir AssignmentService)
        }
    }

    /**
     * Enregistre aupres du relais qu'un joueur est desormais EN PARTIE sur ce serveur (voir
     * KaliumRelay/PlayerChooseInitialServerEvent) : demande explicite de l'utilisateur, "si un
     * joueur est deconnecte durant une partie et qu'il se reconnecte avant la fin de la partie il
     * faut que le proxy le renvoi directement sur la partie" - a appeler quand le joueur est
     * teleporte dans son instance (voir PartyStarter). Bloquant - a appeler UNIQUEMENT depuis un
     * thread asynchrone (meme convention que fetch() ci-dessus). Echoue silencieusement si
     * relay-url est vide/injoignable : le joueur sera simplement route normalement (retour sur
     * kal-games) s'il se deconnecte/reconnecte, comme avant cette fonctionnalite.
     */
    public void registerActiveGame(UUID playerId) {
        String url = plugin.getConfig().getString("network.relay-url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        String token = plugin.getConfig().getString("network.relay-token", "");
        String selfServerName = plugin.getConfig().getString("network.self-server-name", "kixster");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/active-game/" + playerId))
                    .header("X-Kalium-Relay-Token", token)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(selfServerName, StandardCharsets.UTF_8))
                    .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().warning("[KG_BingoGame] Impossible d'enregistrer la partie active aupres du relais pour "
                    + playerId + " : " + e.getMessage());
        }
    }

    /**
     * Retire l'enregistrement "en partie" d'un joueur aupres du relais - a appeler en fin de partie
     * (voir GameEndService) pour qu'une FUTURE reconnexion (une fois la partie terminee) suive a
     * nouveau le routage normal (kal-games) plutot que d'etre renvoyee vers Kixster indefiniment.
     * Bloquant - meme convention que registerActiveGame ci-dessus. Echoue silencieusement (log
     * seulement) : au pire, l'entree sera quand meme purgee par la purge de securite du relais
     * (6h, voir KaliumRelay/ActiveGameRegistry).
     */
    public void clearActiveGame(UUID playerId) {
        String url = plugin.getConfig().getString("network.relay-url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        String token = plugin.getConfig().getString("network.relay-token", "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/active-game/" + playerId))
                    .header("X-Kalium-Relay-Token", token)
                    .timeout(Duration.ofSeconds(5))
                    .DELETE()
                    .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().warning("[KG_BingoGame] Impossible de retirer la partie active aupres du relais pour "
                    + playerId + " : " + e.getMessage());
        }
    }

    /**
     * Publie sur le relais qu'une partie en salle d'attente est FERMEE (demarree ou annulee) - voir
     * PartyStatusNotifier (0.1.19). Cle "party-closed-&lt;gameId&gt;" sur l'endpoint generique
     * /assignment/ du relais (stockage cle -&gt; texte, lu une fois, expire au bout de 2 min) :
     * kal-games l'interroge regulierement pour chacune des parties qu'il liste encore (voir
     * BingoPartyManager.pollClosedParties cote KalGames). Bloquant - thread asynchrone uniquement.
     */
    public void postPartyClosed(String gameId) {
        String url = plugin.getConfig().getString("network.relay-url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        String token = plugin.getConfig().getString("network.relay-token", "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/assignment/party-closed-" + gameId))
                    .header("X-Kalium-Relay-Token", token)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString("closed", StandardCharsets.UTF_8))
                    .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().warning("[KG_BingoGame] Impossible de publier la fermeture de la partie '" + gameId
                    + "' sur le relais : " + e.getMessage());
        }
    }
}
