package fr.kalium.games.bingo;

import fr.kalium.games.KalGames;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 1.9.0 - bouton « Rejouer » du Bingo (demande de LeKiwi06, 09/10/2026). La partie se termine sur le serveur Bingo :
 * dans la salle d'attente d'apres-partie, KG_BingoGame donne l'objet « Rejouer » (30 s). Quand un joueur clique,
 * KG_BingoGame depose les reglages de la partie terminee sur le relais (cle « bingo-rejouer-&lt;uuid&gt; ») et renvoie
 * le joueur ici ; a son arrivee, cette cle est lue :
 * - personne n'a encore relance cette partie : une nouvelle partie est creee avec les memes reglages (equipes, taille,
 *   duree, mode, grille ; nouvelle seed), il en est l'hote ;
 * - quelqu'un l'a deja relancee et elle est encore en salle d'attente : il la rejoint.
 * Dans les deux cas il est renvoye aussitot sur le serveur Bingo, comme apres une creation par le menu. La creation
 * respecte les limites habituelles (2 parties par heure et par joueur, plafonds d'equipes et de duree).
 */
final class BingoReplay implements Listener {

    /** Delai avant la lecture du relais : apres la remise a zero du joueur par KalGames a son arrivee au hub. */
    private static final long DELAY_TICKS = 20L;
    private static final long FORGET_MILLIS = 15L * 60 * 1000;

    private record Relaunch(String gameId, long at) {
    }

    private final KGBingo plugin;
    private final KalGames kg;
    private final BingoMenus menus;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    /** Partie terminee -> partie recreee par le premier qui a clique sur « Rejouer ». */
    private final Map<String, Relaunch> relaunched = new HashMap<>();

    BingoReplay(KGBingo plugin, KalGames kg, BingoMenus menus) {
        this.plugin = plugin;
        this.kg = kg;
        this.menus = menus;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        String url = plugin.getConfig().getString("bingo.relay-url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/assignment/bingo-rejouer-" + id))
                .header("X-Kalium-Relay-Token", plugin.getConfig().getString("bingo.relay-token", ""))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        return; // ce joueur ne vient pas de cliquer sur « Rejouer »
                    }
                    String body = response.body();
                    Bukkit.getScheduler().runTask(plugin, () -> replay(id, body));
                }).exceptionally(e -> null), DELAY_TICKS);
    }

    private void replay(UUID id, String body) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return;
        }
        Map<String, String> keys = new HashMap<>();
        for (String line : body.split("\n")) {
            int index = line.indexOf('=');
            if (index > 0) {
                keys.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
            }
        }
        String ended = keys.get("ended");
        if (ended == null || ended.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        relaunched.values().removeIf(relaunch -> now - relaunch.at() > FORGET_MILLIS);

        BingoPartyManager parties = plugin.parties();
        Relaunch relaunch = relaunched.get(ended);
        BingoParty existing = relaunch == null ? null : parties.byGameId(relaunch.gameId());
        if (existing != null) {
            // Deja relancee et encore en salle d'attente : on la rejoint (refus si elle est pleine).
            if (parties.join(player, existing.code()) == null) {
                player.sendMessage(kg.prefix().append(menus.t("bingo.replay-full",
                        "<red>La partie relancée est complète.")));
                return;
            }
            player.sendMessage(kg.prefix().append(menus.t("bingo.joined", "<green>Partie rejointe, transfert en cours…")));
            parties.transferToBingo(player);
            return;
        }
        long wait = parties.creationWaitSeconds(player);
        if (wait > 0) {
            player.sendMessage(BingoPartyManager.creationWaitText(wait));
            return;
        }
        BingoParty party = parties.create(player, number(keys.get("teamCount"), 2), number(keys.get("teamSize"), 4),
                Duration.ofSeconds(Math.max(60, number(keys.get("duration"), 3600))));
        party.setRules(keys.getOrDefault("rules", ""));
        relaunched.put(ended, new Relaunch(party.gameId(), now));
        player.sendMessage(kg.prefix().append(menus.t("bingo.replay-created",
                "<green>Partie Bingo relancée (<teams> équipe(s) x <size> joueur(s), <minutes> min). "
                        + "Code à partager : <white><bold><code></bold>",
                "teams", party.teamCount(), "size", party.teamSize(),
                "minutes", party.duration().toMinutes(), "code", party.code())));
        parties.transferToBingo(player);
    }

    private static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
