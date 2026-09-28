package fr.kalium.kgbuildbattle;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 0.2.0 - nombre de joueurs en Build Battle sur Kanvas, pour le bouton du Build Battle dans le menu de kal-games (demande de
 * LeKiwi06, 28/09/2026 : « pour chaque bouton visant a rejoindre un jeu, le plugin doit afficher combien il y a de
 * joueurs dedans »). KV_BuildBattle 0.3.6 le publie toutes les 5 s sur le relais (cle « compteur-buildbattle », lue une fois) ;
 * on le lit toutes les 5 s. Sans nouvelle valeur depuis 30 s (relais ou Kanvas arretes) : inconnu (-1), rien
 * n'est affiche sur le bouton.
 */
final class RelayCounter {

    private static final long STALE_MILLIS = 30_000L;

    private final JavaPlugin plugin;
    private final String key;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private volatile int value = -1;
    private volatile long updatedAt;

    RelayCounter(JavaPlugin plugin, String key) {
        this.plugin = plugin;
        this.key = key;
        Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 40L, 100L);
    }

    /** Nombre de joueurs, ou -1 si inconnu. */
    int value() {
        return System.currentTimeMillis() - updatedAt > STALE_MILLIS ? -1 : value;
    }

    private void poll() {
        String url = plugin.getConfig().getString("relay-url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        String token = plugin.getConfig().getString("relay-token", "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/assignment/" + key))
                    .header("X-Kalium-Relay-Token", token)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    try {
                        value = Math.max(0, Integer.parseInt(response.body().trim()));
                        updatedAt = System.currentTimeMillis();
                    } catch (NumberFormatException ignored) {
                        // valeur illisible : on garde l'ancienne
                    }
                }
            }).exceptionally(e -> null); // relais injoignable : nouvel essai dans 5 s
        } catch (IllegalArgumentException e) {
            // adresse du relais invalide : deja signale par les autres fonctions du relais
        }
    }
}
