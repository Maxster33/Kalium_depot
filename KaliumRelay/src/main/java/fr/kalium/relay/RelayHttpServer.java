package fr.kalium.relay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Petit serveur HTTP (JDK uniquement, aucune dependance supplementaire) qui sert de boite aux
 * lettres entre KalGames et KalBingo : KalGames y DEPOSE (POST) l'affectation d'un joueur au
 * moment ou il le transfere vers Bingo ; KalBingo la RECUPERE (GET, qui consomme l'entree) des
 * que le joueur se connecte chez lui. La meme boite aux lettres sert aussi au signal "partie
 * fermee" (cle "party-closed-<gameId>", depose par KalBingo, lu par KalGames).
 *
 * Complete le canal BungeeCord/Velocity (sous-canaux KalBingoAssignRequest/Response, "Forward"),
 * garde en parallele comme filet de securite : ce canal a besoin qu'un joueur QUELCONQUE soit connecte sur le
 * serveur CIBLE pour qu'un message puisse etre livre (limite du protocole Minecraft lui-meme, pas
 * du code) - en test a 2 comptes, si les deux se retrouvent sur Kixster en meme temps, kal-games
 * tombe a 0 joueur et l'affectation ne peut plus etre livree, quel que soit le nombre de
 * reessais. Un appel HTTP direct entre les deux serveurs (via ce relais, sur le proxy) ne depend
 * d'aucun joueur, ni sur kal-games ni sur Kixster.
 */
final class RelayHttpServer {

    /** Nettoyage de securite : une affectation deposee et jamais reclamee (joueur deconnecte
     *  avant d'arriver sur Bingo, par exemple) ne doit pas rester en memoire indefiniment. */
    private static final long ENTRY_TTL_MILLIS = 2 * 60 * 1000L;

    private record Entry(String body, long storedAt) { }

    private final RelayConfig config;
    private final Logger logger;
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    /** Registre "joueur en partie" (reconnexion en cours de partie) - voir ActiveGameRegistry. */
    private final ActiveGameRegistry activeGameRegistry;
    private HttpServer server;
    private ScheduledExecutorService cleaner;

    RelayHttpServer(RelayConfig config, Logger logger, ActiveGameRegistry activeGameRegistry) {
        this.config = config;
        this.logger = logger;
        this.activeGameRegistry = activeGameRegistry;
    }

    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/assignment/", this::handleAssignment);
        server.createContext("/active-game/", this::handleActiveGame);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleAtFixedRate(this::sweep, 1, 1, TimeUnit.MINUTES);
    }

    void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (cleaner != null) {
            cleaner.shutdownNow();
        }
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().storedAt() > ENTRY_TTL_MILLIS);
        activeGameRegistry.sweep();
    }

    private void handleAssignment(HttpExchange exchange) {
        try {
            String playerId = authorizedPlayerId(exchange);
            if (playerId == null) {
                return; // authorizedPlayerId a deja repondu (401/400)
            }
            switch (exchange.getRequestMethod()) {
                case "POST" -> {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    store.put(playerId, new Entry(body, System.currentTimeMillis()));
                    respond(exchange, 204, "");
                }
                case "GET" -> {
                    Entry entry = store.remove(playerId);
                    respond(exchange, entry == null ? 404 : 200, entry == null ? "" : entry.body());
                }
                default -> respond(exchange, 405, "");
            }
        } catch (Exception e) {
            logger.warn("[KaliumRelay] Erreur sur une requete relais (assignment) : " + e.getMessage());
            respond(exchange, 500, "");
        }
    }

    /**
     * POST (corps = nom du serveur, ex. "kixster") : enregistre le joueur comme EN PARTIE sur ce
     * serveur - appele par KalBingo au lancement d'une partie (voir RelayClient.registerActiveGame
     * / PartyStarter cote KalBingo). DELETE : retire l'enregistrement - appele par KalBingo en fin
     * de partie et a l'abandon d'un joueur (voir ActiveGameRegistry). Lu par
     * KaliumRelay.onChooseInitialServer, directement en memoire.
     */
    private void handleActiveGame(HttpExchange exchange) {
        try {
            String playerIdRaw = authorizedPlayerId(exchange);
            if (playerIdRaw == null) {
                return;
            }
            UUID playerId;
            try {
                playerId = UUID.fromString(playerIdRaw);
            } catch (IllegalArgumentException e) {
                respond(exchange, 400, "");
                return;
            }
            switch (exchange.getRequestMethod()) {
                case "POST" -> {
                    String serverName = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (serverName.isBlank()) {
                        respond(exchange, 400, "");
                        return;
                    }
                    activeGameRegistry.set(playerId, serverName);
                    respond(exchange, 204, "");
                }
                case "DELETE" -> {
                    activeGameRegistry.clear(playerId);
                    respond(exchange, 204, "");
                }
                default -> respond(exchange, 405, "");
            }
        } catch (Exception e) {
            logger.warn("[KaliumRelay] Erreur sur une requete relais (active-game) : " + e.getMessage());
            respond(exchange, 500, "");
        }
    }

    /** Verifie le jeton et extrait l'UUID (brut, non parse) depuis la fin du chemin ; repond
     *  directement (401/400) et renvoie null si la requete doit s'arreter la. */
    private String authorizedPlayerId(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("X-Kalium-Relay-Token");
        if (token == null || !token.equals(config.token())) {
            respond(exchange, 401, "");
            return null;
        }
        String path = exchange.getRequestURI().getPath();
        String playerId = path.substring(path.lastIndexOf('/') + 1);
        if (playerId.isBlank()) {
            respond(exchange, 400, "");
            return null;
        }
        return playerId;
    }

    private void respond(HttpExchange exchange, int status, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        } catch (IOException ignored) {
            // rien de plus a faire, la connexion est probablement deja coupee
        }
    }
}
