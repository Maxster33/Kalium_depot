package fr.kalium.scoreboards.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.kalium.scoreboards.Category;
import fr.kalium.scoreboards.KGScoreBoards;
import fr.kalium.scoreboards.data.StatsService;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 1.7.0 : API HTTP en lecture pour le bot Discord (KLM_DiscordBot, cahier des charges § 2). KG_ScoreBoards reste le
 * seul interlocuteur du bot : l'API expose les classements, les archives et le journal des parties, sans logique
 * propre a Discord (les statistiques calculees sont faites par le bot).
 *
 * <ul>
 *   <li>Desactivee tant que {@code api.port} vaut 0 ou que {@code api.token} est vide (valeurs par defaut).</li>
 *   <li>Chaque requete doit porter l'en-tete {@code Authorization: Bearer <api.token>} ; le jeton n'est jamais
 *       affiche dans la console.</li>
 *   <li>Fils du serveur HTTP a part : les donnees en memoire de StatsService sont lues sur le fil principal
 *       (callSyncMethod, 5 s au plus), le journal est lu directement dans les fichiers.</li>
 * </ul>
 *
 * Routes (GET) : {@code /api/v1/status}, {@code /api/v1/rankings/<jeu>}, {@code /api/v1/archives},
 * {@code /api/v1/archives/<archive>/<jeu>}, {@code /api/v1/players/<uuid ou pseudo>}, {@code /api/v1/events}.
 */
public final class StatsApi {

    /** Requetes par minute et par adresse, au-dela : 429. */
    private static final int RATE_PER_MINUTE = 120;

    private final KGScoreBoards plugin;
    private final JournalReader journal;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private final Map<String, int[]> rate = new ConcurrentHashMap<>();
    private volatile long rateMinute;
    private HttpServer server;
    private ExecutorService executor;
    private byte[] token;

    public StatsApi(KGScoreBoards plugin) {
        this.plugin = plugin;
        this.journal = new JournalReader(new File(plugin.getDataFolder(), "journal"));
    }

    /** Demarre l'API si elle est configuree ; sinon ne fait rien. */
    public void start() {
        int port = plugin.getConfig().getInt("api.port", 0);
        String secret = plugin.getConfig().getString("api.token", "");
        if (port <= 0) {
            return;
        }
        if (secret == null || secret.isBlank()) {
            plugin.getLogger().warning("API du bot Discord : api.port est réglé mais api.token est vide, API désactivée.");
            return;
        }
        token = secret.trim().getBytes(StandardCharsets.UTF_8);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 16);
        } catch (IOException e) {
            plugin.getLogger().warning("API du bot Discord : port " + port + " indisponible (" + e.getMessage() + ").");
            return;
        }
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "KG_ScoreBoards-api");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/api/v1/", this::handle);
        server.start();
        plugin.getLogger().info("API du bot Discord à l'écoute sur le port " + port + ".");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    // ------------------------------------------------------------------ requetes

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!allowed(exchange)) {
                send(exchange, 429, Map.of("error", "trop de requêtes"));
                return;
            }
            if (!authorized(exchange)) {
                send(exchange, 401, Map.of("error", "jeton manquant ou invalide"));
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, Map.of("error", "méthode non autorisée"));
                return;
            }
            String path = exchange.getRequestURI().getRawPath().substring("/api/v1/".length());
            List<String> parts = new ArrayList<>();
            for (String part : path.split("/")) {
                if (!part.isEmpty()) {
                    parts.add(URLDecoder.decode(part, StandardCharsets.UTF_8));
                }
            }
            Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
            Object body = route(parts, query);
            if (body == null) {
                send(exchange, 404, Map.of("error", "introuvable"));
            } else {
                send(exchange, 200, body);
            }
        } catch (java.util.concurrent.TimeoutException e) {
            send(exchange, 503, Map.of("error", "serveur occupé, réessayer"));
        } catch (Exception e) {
            plugin.getLogger().warning("API du bot Discord : erreur sur " + exchange.getRequestURI().getPath() + " : " + e);
            send(exchange, 500, Map.of("error", "erreur interne"));
        }
    }

    private Object route(List<String> parts, Map<String, String> query) throws Exception {
        if (parts.isEmpty()) {
            return null;
        }
        return switch (parts.get(0)) {
            case "status" -> parts.size() == 1 ? onMain(this::status) : null;
            case "rankings" -> parts.size() == 2 ? onMain(() -> ranking(parts.get(1), query)) : null;
            case "archives" -> parts.size() == 1 ? onMain(this::archives)
                    : parts.size() == 3 ? onMain(() -> archive(parts.get(1), parts.get(2), query)) : null;
            case "players" -> parts.size() == 2 ? onMain(() -> player(parts.get(1))) : null;
            case "events" -> parts.size() == 1 ? events(query) : null;
            default -> null;
        };
    }

    private Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("version", plugin.getPluginMeta().getVersion());
        out.put("time", now());
        out.put("month", plugin.stats().monthKey());
        out.put("online", Bukkit.getOnlinePlayers().size());
        List<Map<String, Object>> games = new ArrayList<>();
        for (Category category : plugin.categories()) {
            games.add(game(category));
        }
        out.put("games", games);
        return out;
    }

    private Map<String, Object> ranking(String gameId, Map<String, String> query) {
        Category category = plugin.category(gameId);
        if (category == null) {
            return null;
        }
        boolean month = "month".equals(query.get("period"));
        boolean lap = "lap".equals(query.get("sort"));
        List<StatsService.Row> rows = lap ? plugin.stats().lapRanking(gameId, month) : plugin.stats().ranking(gameId, month);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("game", game(category));
        out.put("period", month ? "month" : "general");
        out.put("month", plugin.stats().monthKey());
        out.put("sort", lap ? "lap" : "points");
        out.put("total", rows.size());
        out.put("rows", rows(rows, limit(query, 10, 100)));
        out.put("time", now());
        return out;
    }

    private List<Map<String, Object>> archives() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (StatsService.Archive archive : plugin.stats().archives()) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("id", archive.id());
            one.put("month", archive.month());
            one.put("label", archive.label());
            one.put("archivedAt", Instant.ofEpochMilli(archive.archivedAt()).toString());
            one.put("games", new ArrayList<>(plugin.stats().archivedMinigames(archive.id())));
            out.add(one);
        }
        return out;
    }

    private Map<String, Object> archive(String archiveId, String gameId, Map<String, String> query) {
        boolean known = false;
        for (StatsService.Archive archive : plugin.stats().archives()) {
            if (archive.id().equals(archiveId)) {
                known = true;
                break;
            }
        }
        if (!known || !plugin.stats().archivedMinigames(archiveId).contains(gameId)) {
            return null;
        }
        boolean lap = "lap".equals(query.get("sort"));
        List<StatsService.Row> rows = lap ? plugin.stats().archivedLaps(archiveId, gameId) : plugin.stats().archived(archiveId, gameId);
        Category category = plugin.category(gameId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("archive", archiveId);
        out.put("game", category == null ? Map.of("id", gameId, "name", gameId, "kind", "POINTS") : game(category));
        out.put("sort", lap ? "lap" : "points");
        out.put("total", rows.size());
        out.put("rows", rows(rows, limit(query, 10, 1000)));
        return out;
    }

    /** Joueur par UUID ou par pseudo (sans tenir compte des majuscules) : points, temps et rang dans chaque jeu. */
    private Map<String, Object> player(String who) {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(who);
        } catch (IllegalArgumentException ignored) {
            // pseudo
        }
        String name = null;
        List<Map<String, Object>> games = new ArrayList<>();
        for (Category category : plugin.categories()) {
            Map<String, Object> one = new LinkedHashMap<>();
            for (boolean month : new boolean[]{false, true}) {
                List<StatsService.Row> ranking = plugin.stats().ranking(category.id(), month);
                List<StatsService.Row> laps = category.kind() == Category.Kind.LAP ? plugin.stats().lapRanking(category.id(), month) : List.of();
                if (uuid == null) {
                    uuid = findByName(ranking, who);
                }
                if (uuid == null) {
                    uuid = findByName(laps, who);
                }
                int rank = indexOf(ranking, uuid);
                int lapRank = indexOf(laps, uuid);
                if (rank < 0 && lapRank < 0) {
                    continue;
                }
                StatsService.Row row = rank >= 0 ? ranking.get(rank) : laps.get(lapRank);
                name = row.name();
                Map<String, Object> period = new LinkedHashMap<>();
                period.put("rank", rank < 0 ? null : rank + 1);
                period.put("of", ranking.size());
                period.put("points", row.points());
                period.put("bestMs", row.bestMs() < 0 ? null : row.bestMs());
                period.put("bestLapMs", row.bestLapMs() < 0 ? null : row.bestLapMs());
                period.put("lapRank", lapRank < 0 ? null : lapRank + 1);
                one.put(month ? "month" : "general", period);
            }
            if (!one.isEmpty()) {
                Map<String, Object> entry = new LinkedHashMap<>(game(category));
                entry.putAll(one);
                games.add(entry);
            }
        }
        if (uuid == null || name == null) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("uuid", uuid.toString());
        out.put("name", name);
        out.put("platform", uuid.getMostSignificantBits() == 0 ? "bedrock" : "java");
        out.put("month", plugin.stats().monthKey());
        out.put("games", games);
        out.put("time", now());
        return out;
    }

    private Map<String, Object> events(Map<String, String> query) throws IOException {
        JournalReader.Page page = journal.read(query.get("after"), limit(query, 500, 2000));
        List<JsonObject> events = new ArrayList<>();
        for (JournalReader.Line line : page.lines()) {
            try {
                JsonObject object = JsonParser.parseString(line.json()).getAsJsonObject();
                object.addProperty("_id", line.id());
                events.add(object);
            } catch (RuntimeException ignored) {
                // ligne illisible : ignoree (le curseur avance quand meme)
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("events", events);
        out.put("cursor", page.cursor());
        out.put("more", page.more());
        return out;
    }

    // ------------------------------------------------------------------ outils

    private static UUID findByName(List<StatsService.Row> rows, String name) {
        for (StatsService.Row row : rows) {
            if (row.name() != null && row.name().equalsIgnoreCase(name)) {
                return row.uuid();
            }
        }
        return null;
    }

    private static int indexOf(List<StatsService.Row> rows, UUID uuid) {
        if (uuid == null) {
            return -1;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).uuid().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    private static Map<String, Object> game(Category category) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", category.id());
        out.put("name", PlainTextComponentSerializer.plainText().serialize(category.name()));
        out.put("kind", category.kind().name());
        return out;
    }

    private static List<Map<String, Object>> rows(List<StatsService.Row> rows, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < limit; i++) {
            StatsService.Row row = rows.get(i);
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("rank", i + 1);
            one.put("uuid", row.uuid().toString());
            one.put("name", row.name());
            one.put("points", row.points());
            one.put("bestMs", row.bestMs() < 0 ? null : row.bestMs());
            one.put("bestLapMs", row.bestLapMs() < 0 ? null : row.bestLapMs());
            out.add(one);
        }
        return out;
    }

    private static int limit(Map<String, String> query, int def, int max) {
        try {
            return Math.max(1, Math.min(max, Integer.parseInt(query.getOrDefault("limit", String.valueOf(def)))));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(key, value);
        }
        return out;
    }

    private String now() {
        return OffsetDateTime.now(plugin.stats().zone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** Execute sur le fil principal (les classements en memoire n'y sont modifies que la), 5 s au plus. */
    private <T> T onMain(Callable<T> task) throws Exception {
        if (Bukkit.isPrimaryThread()) {
            return task.call();
        }
        return Bukkit.getScheduler().callSyncMethod(plugin, task).get(5, TimeUnit.SECONDS);
    }

    private boolean authorized(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return false;
        }
        byte[] given = header.substring("Bearer ".length()).trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(given, token);
    }

    /** Limite simple : RATE_PER_MINUTE requetes par minute et par adresse. */
    private boolean allowed(HttpExchange exchange) {
        long minute = System.currentTimeMillis() / 60000;
        if (minute != rateMinute) {
            rate.clear();
            rateMinute = minute;
        }
        String address = exchange.getRemoteAddress().getAddress().getHostAddress();
        int[] count = rate.computeIfAbsent(address, k -> new int[1]);
        synchronized (count) {
            return ++count[0] <= RATE_PER_MINUTE;
        }
    }

    private void send(HttpExchange exchange, int code, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
