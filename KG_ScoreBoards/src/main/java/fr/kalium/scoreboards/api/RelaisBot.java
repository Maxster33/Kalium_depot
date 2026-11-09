package fr.kalium.scoreboards.api;

import fr.kalium.scoreboards.KGScoreBoards;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 1.10.0 : API du bot Discord par la boite aux lettres de KaliumRelay (KaliumRelay 1.11.0). Kanvas n'accepte que ses
 * ports attribues et le bot (Dishost) ne peut pas le joindre : c'est donc KG_ScoreBoards qui va chercher, chaque
 * seconde, les demandes deposees par le bot dans la boite « scoreboards » du relais, et depose chaque reponse dans la
 * boite « bot ». Meme reponses que l'API HTTP (StatsApi.repondre).
 *
 * <p>Message de demande : le chemin et la requete (« /api/v1/rankings/pvpkit?period=month »). Message de reponse :
 * trois parties separees par un saut de ligne : id de la demande, code HTTP, JSON.</p>
 *
 * <p>Adresse et jeton du relais : {@code api.relay-url} / {@code api.relay-token} ; vides = ceux de KLM_Contacts sur
 * le meme serveur (relay-url / relay-token). Sans jeton : desactive.</p>
 */
public final class RelaisBot {

    /** Reponse plus grosse que la boite du relais (256 Ko) : remplacee par une erreur 413. */
    private static final int MAX_REPONSE = 250 * 1024;

    private final KGScoreBoards plugin;
    private final StatsApi api;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final AtomicBoolean enCours = new AtomicBoolean();
    private String url;
    private String jeton;
    private BukkitTask tache;
    private boolean injoignable;

    public RelaisBot(KGScoreBoards plugin, StatsApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    public void start() {
        url = plugin.getConfig().getString("api.relay-url", "");
        jeton = plugin.getConfig().getString("api.relay-token", "");
        if (url == null || url.isBlank() || jeton == null || jeton.isBlank()) {
            File contacts = new File(plugin.getDataFolder().getParentFile(), "KLM_Contacts/config.yml");
            if (contacts.isFile()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(contacts);
                if (url == null || url.isBlank()) {
                    url = config.getString("relay-url", "");
                }
                if (jeton == null || jeton.isBlank()) {
                    jeton = config.getString("relay-token", "");
                }
            }
        }
        if (url == null || url.isBlank() || jeton == null || jeton.isBlank()) {
            plugin.getLogger().info("API du bot Discord par le relais : désactivée (api.relay-url / api.relay-token vides).");
            return;
        }
        url = url.trim().replaceAll("/+$", "");
        jeton = jeton.trim();
        tache = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::relever, 40L, 20L);
        plugin.getLogger().info("API du bot Discord par le relais : demandes relevées chaque seconde.");
    }

    public void stop() {
        if (tache != null) {
            tache.cancel();
            tache = null;
        }
    }

    private void relever() {
        if (!enCours.compareAndSet(false, true)) {
            return;
        }
        try {
            HttpResponse<String> liste = http.send(requete("/mail/scoreboards").GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (liste.statusCode() != 200) {
                signaler("réponse " + liste.statusCode());
                return;
            }
            if (injoignable) {
                injoignable = false;
                plugin.getLogger().info("API du bot Discord par le relais : relais de nouveau joignable.");
            }
            for (String ligne : liste.body().split("\n")) {
                int tab = ligne.indexOf('\t');
                if (tab > 0) {
                    traiter(ligne.substring(0, tab), new String(Base64.getDecoder().decode(ligne.substring(tab + 1).trim()),
                            StandardCharsets.UTF_8).trim());
                }
            }
        } catch (Exception e) {
            signaler(e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " : " + e.getMessage()));
        } finally {
            enCours.set(false);
        }
    }

    private void traiter(String id, String demande) throws Exception {
        int question = demande.indexOf('?');
        String chemin = question < 0 ? demande : demande.substring(0, question);
        String requete = question < 0 ? null : demande.substring(question + 1);
        StatsApi.Reponse reponse = api.repondre(chemin, requete);
        String message = id + "\n" + reponse.code() + "\n" + reponse.json();
        if (message.getBytes(StandardCharsets.UTF_8).length > MAX_REPONSE) {
            message = id + "\n413\n{\"error\":\"réponse trop grosse, demander moins de lignes\"}";
        }
        HttpResponse<Void> depot = http.send(requete("/mail/bot")
                        .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.discarding());
        if (depot.statusCode() != 200) {
            signaler("réponse non déposée (" + depot.statusCode() + ")");
            return; // la demande reste : nouvel essai a la prochaine releve
        }
        http.send(requete("/mail/scoreboards/" + URLEncoder.encode(id, StandardCharsets.UTF_8)).DELETE().build(),
                HttpResponse.BodyHandlers.discarding());
    }

    private HttpRequest.Builder requete(String chemin) {
        return HttpRequest.newBuilder(URI.create(url + chemin))
                .timeout(Duration.ofSeconds(10))
                .header("X-Kalium-Relay-Token", jeton);
    }

    /** Un seul avertissement tant que le relais reste injoignable (pas un par seconde). */
    private void signaler(String detail) {
        if (!injoignable) {
            injoignable = true;
            plugin.getLogger().warning("API du bot Discord par le relais : relais injoignable (" + detail + ").");
        }
    }
}
