package fr.kalium.contacts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Liaison avec le proxy : les données des contacts (amis, demandes, blocages, réglages) et la présence des joueurs sont
 * gardées par KaliumRelay (1.6.0), seul endroit qui voit tous les serveurs. Chaque demande est un POST
 * /contacts/&lt;action&gt; (jeton du relais), fait hors du fil principal ; la suite est rejouée sur le fil principal.
 */
final class Relais {

    /** Ami : serveur vide = hors ligne (ou en mode invisible). */
    record Ami(UUID id, String nom, String serveur) {
        boolean enLigne() {
            return !serveur.isEmpty();
        }
    }

    record Personne(UUID id, String nom) {
    }

    /** Réponse du relais : « ok » ou « ok:détail », sinon « err:code ». */
    record Reponse(boolean ok, String detail, List<String> lignes) {

        /** Partie n° i du détail (séparé par « : »), ou vide. */
        String partie(int i) {
            String[] parties = detail.split(":", -1);
            return i < parties.length ? parties[i] : "";
        }
    }

    /** Tout ce que le menu affiche pour un joueur. */
    static final class Liste {
        String monServeur = "";
        boolean invisible;
        boolean notifications = true;
        String mp = "all";
        /** 1.1.0 (groupe de jeu) : suivre le chef d'office (auto) ou sur proposition (ask). */
        String suivre = "auto";
        /** 1.1.0 (groupe de jeu) : invitations de tout le monde (all) ou des amis seulement (friends). */
        String invitations = "all";
        final List<Ami> amis = new ArrayList<>();
        final List<Personne> recues = new ArrayList<>();
        final List<Personne> envoyees = new ArrayList<>();
        final List<Personne> bloques = new ArrayList<>();

        Ami ami(UUID id) {
            for (Ami ami : amis) {
                if (ami.id().equals(id)) {
                    return ami;
                }
            }
            return null;
        }
    }

    private final KlmContacts plugin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    Relais(KlmContacts plugin) {
        this.plugin = plugin;
    }

    /**
     * Envoie une demande au relais pour ce joueur. {@code suite} reçoit la réponse sur le fil principal, ou null si le
     * relais est injoignable ou n'est pas configuré (le joueur est alors déjà prévenu).
     */
    void appeler(Player joueur, String action, Map<String, String> parametres, Consumer<Reponse> suite) {
        appeler(joueur, action, parametres, suite, false);
    }

    /** Comme {@link #appeler}, sans rien dire au joueur si le relais est indisponible (demande faite en arrière-plan). */
    void appelerEnSilence(Player joueur, String action, Map<String, String> parametres) {
        appeler(joueur, action, parametres, reponse -> { }, true);
    }

    /** 1.2.0 - demande en arrière-plan dont on attend la réponse (null si le relais est indisponible). */
    void appelerEnSilence(Player joueur, String action, Map<String, String> parametres, Consumer<Reponse> suite) {
        appeler(joueur, action, parametres, suite, true);
    }

    private void appeler(Player joueur, String action, Map<String, String> parametres, Consumer<Reponse> suite,
                         boolean silence) {
        String url = plugin.getConfig().getString("relay-url", "");
        String jeton = plugin.getConfig().getString("relay-token", "");
        UUID id = joueur.getUniqueId();
        if (url == null || url.isBlank() || jeton == null || jeton.isBlank()) {
            if (!silence) {
                plugin.dire(joueur, "indisponible", "<red>Contacts indisponibles pour le moment.");
            }
            suite.accept(null);
            return;
        }
        StringBuilder corps = new StringBuilder("player=").append(id).append('\n');
        parametres.forEach((cle, valeur) -> corps.append(cle).append('=')
                .append(valeur == null ? "" : valeur.replace('\n', ' ').replace('\r', ' ')).append('\n'));
        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create(url + "/contacts/" + action))
                .header("X-Kalium-Relay-Token", jeton)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(corps.toString(), StandardCharsets.UTF_8))
                .build();
        http.sendAsync(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((reponse, erreur) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player present = Bukkit.getPlayer(id);
                    if (present == null) {
                        return;
                    }
                    if (erreur != null || reponse.statusCode() != 200) {
                        plugin.getLogger().warning("Relais : demande « " + action + " » refusée ou injoignable ("
                                + (erreur != null ? erreur.getMessage() : "code " + reponse.statusCode()) + ").");
                        if (!silence) {
                            plugin.dire(present, "indisponible", "<red>Contacts indisponibles pour le moment.");
                        }
                        suite.accept(null);
                        return;
                    }
                    suite.accept(lire(reponse.body()));
                }));
    }

    private static Reponse lire(String corps) {
        String[] lignes = corps.split("\n");
        String premiere = lignes.length == 0 ? "" : lignes[0].trim();
        boolean ok = premiere.equals("ok") || premiere.startsWith("ok:");
        int deuxPoints = premiere.indexOf(':');
        List<String> suite = new ArrayList<>();
        for (int i = 1; i < lignes.length; i++) {
            suite.add(lignes[i]);
        }
        return new Reponse(ok, deuxPoints < 0 ? "" : premiere.substring(deuxPoints + 1), suite);
    }

    /** 1.1.0 - membre d'un groupe de jeu : serveur vide = hors ligne. */
    record Membre(UUID id, String nom, String serveur, boolean chef) {
    }

    /** 1.1.0 - groupe de jeu du joueur (aucun membre = pas de groupe) et invitation en attente. */
    static final class Groupe {
        final List<Membre> membres = new ArrayList<>();
        boolean jeSuisChef;
        int maximum = 8;
        /** Pseudo de celui qui m'invite dans son groupe, ou null. */
        String invitePar;
        /** 1.2.0 - partie proposée (celle du chef, ou invitation d'un joueur) : pseudo et nom du jeu, ou null. */
        String partiePar;
        String partieJeu = "";

        boolean existe() {
            return !membres.isEmpty();
        }

        Membre chef() {
            for (Membre membre : membres) {
                if (membre.chef()) {
                    return membre;
                }
            }
            return null;
        }
    }

    /** 1.1.0 - état du groupe de jeu du joueur, ou null si indisponible. */
    void groupe(Player joueur, Consumer<Groupe> suite) {
        appeler(joueur, "group", Map.of(), reponse -> {
            if (reponse == null || !reponse.ok()) {
                suite.accept(null);
                return;
            }
            Groupe groupe = new Groupe();
            for (String ligne : reponse.lignes()) {
                String[] c = ligne.split("\t", -1);
                try {
                    switch (c[0]) {
                        case "G" -> {
                            groupe.jeSuisChef = Boolean.parseBoolean(c[2]);
                            groupe.maximum = Integer.parseInt(c[3].trim());
                        }
                        case "P" -> groupe.membres.add(new Membre(UUID.fromString(c[1]), c[2], c[3], Boolean.parseBoolean(c[4].trim())));
                        case "I" -> groupe.invitePar = c[2].trim();
                        case "O" -> {
                            groupe.partiePar = c[1].trim();
                            groupe.partieJeu = c.length > 2 ? c[2].trim() : "";
                        }
                        default -> {
                        }
                    }
                } catch (RuntimeException e) {
                    // ligne incomplète : ignorée
                }
            }
            suite.accept(groupe);
        });
    }

    /** Liste complète du joueur (amis et leur serveur, demandes, bloqués, réglages), ou null si indisponible. */
    void lister(Player joueur, Consumer<Liste> suite) {
        appeler(joueur, "list", Map.of(), reponse -> {
            if (reponse == null || !reponse.ok()) {
                suite.accept(null);
                return;
            }
            Liste liste = new Liste();
            for (String ligne : reponse.lignes()) {
                String[] c = ligne.split("\t", -1);
                try {
                    switch (c[0]) {
                        case "M" -> liste.monServeur = c.length > 1 ? c[1] : "";
                        case "S" -> {
                            if (c.length < 3) {
                                break;
                            }
                            switch (c[1]) {
                                case "invisible" -> liste.invisible = Boolean.parseBoolean(c[2]);
                                case "notify" -> liste.notifications = Boolean.parseBoolean(c[2]);
                                case "mp" -> liste.mp = c[2];
                                case "follow" -> liste.suivre = c[2];
                                case "invites" -> liste.invitations = c[2];
                                default -> {
                                }
                            }
                        }
                        case "F" -> liste.amis.add(new Ami(UUID.fromString(c[1]), c[2], c.length > 3 ? c[3] : ""));
                        case "R" -> liste.recues.add(new Personne(UUID.fromString(c[1]), c[2]));
                        case "O" -> liste.envoyees.add(new Personne(UUID.fromString(c[1]), c[2]));
                        case "B" -> liste.bloques.add(new Personne(UUID.fromString(c[1]), c[2]));
                        default -> {
                        }
                    }
                } catch (RuntimeException e) {
                    // ligne incomplète : ignorée
                }
            }
            // Amis en ligne d'abord, puis par ordre alphabétique.
            liste.amis.sort((a, b) -> a.enLigne() != b.enLigne() ? (a.enLigne() ? -1 : 1)
                    : a.nom().compareToIgnoreCase(b.nom()));
            suite.accept(liste);
        });
    }
}
