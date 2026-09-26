package fr.kalium.games.bingo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.kalium.scoreboards.Category;
import fr.kalium.scoreboards.KGScoreBoards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 1.5.0 - points du Bingo credites dans les classements de KG_ScoreBoards (demande de LeKiwi06, 26/09/2026).
 *
 * Le jeu tourne sur le serveur Bingo (KG_BingoGame) ; a la fin d'une partie, il publie ses resultats sur le relais
 * HTTP (cle « bingo-results-&lt;gameId&gt; », une ligne par joueur : uuid;pseudo;points) et les republie toutes les
 * minutes pendant 6 h. Ici : chaque partie qui quitte la liste des parties en attente (demarree ou annulee) est notee
 * « resultats attendus » (bingo-awaiting.yml, garde 24 h) ; le relais est interroge toutes les 30 s pour ces parties ;
 * les points sont credites UNE seule fois (la partie sort alors de la liste : les republications suivantes ne sont
 * plus lues et expirent sur le relais).
 *
 * Comme les autres jeux : classement « bingo » (general et du mois), points des operateurs non comptes
 * (stats.exclude-operators de KalGames), chaque attribution ecrite dans le journal des parties (evenement « points »,
 * counted / reason) : /classements verifier et crediter fonctionnent aussi pour le Bingo.
 */
final class BingoResults {

    static final String CATEGORY = "bingo";
    private static final long AWAIT_MILLIS = 24L * 60 * 60 * 1000;

    private final KGBingo plugin;
    private final KGScoreBoards ranking;
    private final boolean excludeOperators;
    private final File file;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    /** gameId -&gt; instant ou la partie a quitte la liste d'attente. Thread principal. */
    private final Map<String, Long> awaiting = new HashMap<>();

    BingoResults(KGBingo plugin, KGScoreBoards ranking, boolean excludeOperators) {
        this.plugin = plugin;
        this.ranking = ranking;
        this.excludeOperators = excludeOperators;
        this.file = new File(plugin.getDataFolder(), "bingo-awaiting.yml");
        load();
        ranking.addCategories(id -> CATEGORY.equals(id)
                ? new Category(CATEGORY, Component.text("Bingo", NamedTextColor.GOLD, TextDecoration.BOLD), Category.Kind.POINTS)
                : null, () -> List.of(CATEGORY));
        Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 20L * 15, 20L * 30);
    }

    /** La partie a quitte la liste d'attente (demarree ou annulee) : ses resultats sont attendus. */
    void await(String gameId) {
        if (gameId != null && !gameId.isBlank() && awaiting.putIfAbsent(gameId, System.currentTimeMillis()) == null) {
            save();
        }
    }

    private void poll() {
        long now = System.currentTimeMillis();
        if (awaiting.values().removeIf(at -> now - at > AWAIT_MILLIS)) {
            save();
        }
        String url = plugin.getConfig().getString("bingo.relay-url", "");
        if (url == null || url.isBlank() || awaiting.isEmpty()) {
            return;
        }
        String token = plugin.getConfig().getString("bingo.relay-token", "");
        for (String gameId : List.copyOf(awaiting.keySet())) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/assignment/bingo-results-" + gameId))
                        .header("X-Kalium-Relay-Token", token)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Bukkit.getScheduler().runTask(plugin, () -> credit(gameId, response.body()));
                    }
                }).exceptionally(e -> null); // relais injoignable : on reessaiera au prochain tour
            } catch (Exception e) {
                plugin.getLogger().warning("[Bingo] Erreur en interrogeant le relais (résultats) : " + e.getMessage());
                return;
            }
        }
    }

    private void credit(String gameId, String body) {
        if (awaiting.remove(gameId) == null) {
            return; // deja credite
        }
        save();
        int count = 0;
        for (String line : body.split("\n")) {
            String[] parts = line.trim().split(";");
            if (parts.length < 3) {
                continue;
            }
            UUID uuid;
            double points;
            try {
                uuid = UUID.fromString(parts[0]);
                points = Double.parseDouble(parts[2]);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (points <= 0) {
                continue;
            }
            String name = parts[1];
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String reason = excludeOperators && player.isOp() ? "operateur" : null;
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("match", gameId);
            fields.put("arena", null);
            fields.put("public", false);
            fields.put("player", uuid.toString());
            fields.put("name", name);
            fields.put("platform", uuid.getMostSignificantBits() == 0 ? "bedrock" : "java");
            fields.put("points", points);
            fields.put("counted", reason == null);
            fields.put("reason", reason);
            ranking.log(CATEGORY, "points", fields);
            if (reason == null) {
                ranking.stats().addPoints(CATEGORY, uuid, name, points);
                count++;
            }
        }
        ranking.boards().refreshSoon();
        plugin.getLogger().info("[Bingo] Partie " + gameId + " : points crédités dans les classements (" + count + " joueur(s)).");
    }

    private void load() {
        ConfigurationSection s = YamlConfiguration.loadConfiguration(file).getConfigurationSection("awaiting");
        if (s != null) {
            for (String id : s.getKeys(false)) {
                awaiting.put(id, s.getLong(id));
            }
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        awaiting.forEach((id, at) -> yml.set("awaiting." + id, at));
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[Bingo] Impossible d'enregistrer bingo-awaiting.yml : " + e.getMessage());
        }
    }
}
