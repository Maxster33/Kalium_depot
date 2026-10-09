package fr.kalium.kgbuildbattle;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.ReplayService;
import net.kyori.adventure.text.Component;

/**
 * 0.4.0 - bouton « Rejouer » du Build Battle (LeKiwi06, 09/10/2026). La partie se termine sur Kanvas : à l'annonce
 * des résultats, KV_BuildBattle dépose les réglages de la partie sur le relais pour chaque joueur (clé
 * « buildbattle-rejouer-<uuid> »). Au retour du joueur sur kal-games, cette clé est lue ici et le joueur reçoit
 * l'objet « Rejouer » de KalGames (30 s) :
 * - file publique : clic = retour dans la file de la même taille d'équipes ;
 * - partie privée : le premier qui clique recrée la partie (même tempo, même taille d'équipes, même mode des thèmes)
 *   et en devient l'hôte ; l'objet des autres devient « Rejoindre la partie de X ».
 */
final class Rejouer implements Listener {

    /** Délai avant la lecture du relais : après la remise à zéro du joueur par KalGames à son arrivée au hub. */
    private static final long DELAI_TICKS = 20L;

    private final KGBuildBattle plugin;
    private final Parties parties;
    private final Menus menus;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    /** Une proposition par partie terminée, partagée par ses joueurs (identifiant de la partie -> proposition). */
    private final Map<String, Proposition> propositions = new HashMap<>();

    Rejouer(KGBuildBattle plugin, Parties parties, Menus menus) {
        this.plugin = plugin;
        this.parties = parties;
        this.menus = menus;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        String url = plugin.getConfig().getString("relay-url", "");
        if (url == null || url.isBlank()) return;
        UUID id = e.getPlayer().getUniqueId();
        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create(url + "/assignment/buildbattle-rejouer-" + id))
                .header("X-Kalium-Relay-Token", plugin.getConfig().getString("relay-token", ""))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                http.sendAsync(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenAccept(r -> {
                    if (r.statusCode() != 200) return; // pas de partie de Build Battle terminée à l'instant
                    String reglages = r.body();
                    Bukkit.getScheduler().runTask(plugin, () -> proposer(id, reglages));
                }).exceptionally(ex -> null), DELAI_TICKS);
    }

    private void proposer(UUID id, String texte) {
        Player joueur = Bukkit.getPlayer(id);
        if (joueur == null || !(Bukkit.getPluginManager().getPlugin("KalGames") instanceof KalGames kalGames)) return;
        Map<String, String> cles = new HashMap<>();
        for (String ligne : texte.split("\n")) {
            int i = ligne.indexOf('=');
            if (i > 0) cles.put(ligne.substring(0, i).trim(), ligne.substring(i + 1).trim());
        }
        String partie = cles.get("partie");
        if (partie == null || partie.isBlank()) return;
        long maintenant = System.currentTimeMillis();
        propositions.values().removeIf(p -> maintenant - p.creee > 10 * 60_000L);
        Proposition proposition = propositions.computeIfAbsent(partie, k -> {
            int taille;
            try {
                taille = Integer.parseInt(cles.get("tailleEquipes"));
            } catch (RuntimeException ex) {
                taille = 1;
            }
            return new Proposition("prive".equals(cles.get("type")), Tempo.lire(cles.get("tempo")),
                    Math.max(1, Math.min(4, taille)), Boolean.parseBoolean(cles.get("themesEcrits")));
        });
        kalGames.replay().give(joueur, proposition, false);
    }

    /** Ce que fait l'objet « Rejouer » pour les joueurs d'une même partie terminée. */
    private final class Proposition implements ReplayService.Replay {

        final long creee = System.currentTimeMillis();
        final boolean privee;
        final Tempo tempo;
        final int tailleEquipes;
        final boolean themesEcrits;
        /** Partie privée recréée par le premier qui a cliqué. */
        Partie nouvelle;

        Proposition(boolean privee, Tempo tempo, int tailleEquipes, boolean themesEcrits) {
            this.privee = privee;
            this.tempo = tempo;
            this.tailleEquipes = tailleEquipes;
            this.themesEcrits = themesEcrits;
        }

        /** La partie recréée, tant qu'elle est encore en salle d'attente (listée sur kal-games). */
        private Partie enAttente() {
            return nouvelle != null && parties.existe(nouvelle.id) ? nouvelle : null;
        }

        @Override
        public String hostName() {
            Partie p = enAttente();
            return p == null ? null : p.nomHote;
        }

        @Override
        public Component play(Player joueur) {
            if (!privee) {
                parties.envoyerPublic(joueur, tailleEquipes);
                return null;
            }
            Partie p = enAttente();
            if (p != null) {
                if (!parties.rejoindre(joueur, p)) {
                    return menus.t("rejouer.complete", "<red>Cette partie est complète.");
                }
                parties.envoyerPrive(joueur, p);
                return null;
            }
            nouvelle = parties.creer(joueur, tempo, tailleEquipes, themesEcrits);
            joueur.sendMessage(menus.t("rejouer.creee", "<green>Partie privée créée : code <white><code></white>.",
                    "code", nouvelle.code));
            parties.envoyerPrive(joueur, nouvelle);
            return null;
        }

        @Override
        public boolean canWatch() {
            return false;
        }

        @Override
        public Component watch(Player joueur) {
            return menus.t("rejouer.pas-de-spectateur", "<red>Le Build Battle ne se regarde pas en spectateur.");
        }
    }
}
