package fr.kalium.kgbuildbattle;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * File publique et parties privées du Build Battle côté kal-games. Rien ne se joue ici : le joueur est envoyé tout de
 * suite sur Kanvas, où il attend dans la salle d'attente de sa partie (LeKiwi06, 27/09/2026). Son affectation
 * (file publique et taille d'équipe, ou partie privée et ses réglages) est déposée sur le relais HTTP juste avant,
 * sous la clé « buildbattle-<uuid> », puis relue par KV_BuildBattle à son arrivée.
 */
final class Parties {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final KGBuildBattle plugin;
    private final Map<String, Partie> parId = new HashMap<>();
    private final Map<String, Partie> parCode = new HashMap<>();
    private final SecureRandom hasard = new SecureRandom();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    Parties(KGBuildBattle plugin) {
        this.plugin = plugin;
    }

    int equipesMax() {
        return Math.max(2, Math.min(8, plugin.getConfig().getInt("equipes-max", 8)));
    }

    Partie creer(Player hote, Tempo tempo, int tailleEquipes, boolean themesEcrits) {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(ALPHABET.charAt(hasard.nextInt(ALPHABET.length())));
            code = sb.toString();
        } while (parCode.containsKey(code));
        Partie p = new Partie(UUID.randomUUID().toString(), code, hote.getUniqueId(), hote.getName(), tempo,
                Math.max(1, Math.min(4, tailleEquipes)), equipesMax(), themesEcrits);
        parId.put(p.id, p);
        parCode.put(code, p);
        return p;
    }

    Partie parCode(String code) {
        return code == null ? null : parCode.get(code.trim().toUpperCase());
    }

    /** 0.4.0 : la partie est-elle encore listée (salle d'attente, pas encore lancée sur Kanvas) ? */
    boolean existe(String id) {
        return parId.containsKey(id);
    }

    /** Parties privées encore listées, les plus récentes d'abord. */
    List<Partie> listees(int limite) {
        return parId.values().stream().sorted(Comparator.comparing((Partie p) -> p.creee).reversed())
                .limit(limite).collect(Collectors.toList());
    }

    /** Ajoute le joueur à la partie ; false si elle est pleine. */
    boolean rejoindre(Player joueur, Partie p) {
        if (p.joueurs.contains(joueur.getUniqueId())) return true;
        if (p.joueurs.size() >= p.places()) return false;
        p.joueurs.add(joueur.getUniqueId());
        return true;
    }

    /** File publique : tempo normal, équipes de la taille choisie. */
    void envoyerPublic(Player joueur, int tailleEquipes) {
        envoyer(joueur, "type=public\n"
                + "tailleEquipes=" + Math.max(1, Math.min(4, tailleEquipes)) + "\n"
                + "equipesMax=" + equipesMax() + "\n"
                + "tempo=" + Tempo.NORMAL.name() + "\n"
                + "themesEcrits=false\n");
    }

    void envoyerPrive(Player joueur, Partie p) {
        envoyer(joueur, "type=prive\n"
                + "partie=" + p.id + "\n"
                + "code=" + p.code + "\n"
                + "hote=" + p.hote + "\n"
                + "tailleEquipes=" + p.tailleEquipes + "\n"
                + "equipesMax=" + p.equipesMax + "\n"
                + "tempo=" + p.tempo.name() + "\n"
                + "themesEcrits=" + p.themesEcrits + "\n");
    }

    /**
     * Dépose l'affectation sur le relais, PUIS envoie le joueur sur Kanvas (canal BungeeCord « Connect ») : sans
     * affectation, Kanvas ne saurait pas où le mettre, donc pas d'envoi si le relais ne répond pas.
     */
    private void envoyer(Player joueur, String affectation) {
        String url = plugin.getConfig().getString("relay-url", "");
        if (url == null || url.isBlank()) {
            joueur.sendMessage("§cBuild Battle indisponible : relais non configuré (relay-url).");
            return;
        }
        String jeton = plugin.getConfig().getString("relay-token", "");
        UUID id = joueur.getUniqueId();
        joueur.sendMessage("§7Envoi vers le Build Battle...");
        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create(url + "/assignment/buildbattle-" + id))
                .header("X-Kalium-Relay-Token", jeton)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(affectation, StandardCharsets.UTF_8))
                .build();
        http.sendAsync(requete, HttpResponse.BodyHandlers.discarding()).whenComplete((reponse, erreur) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) return;
                    if (erreur != null || reponse.statusCode() / 100 != 2) {
                        plugin.getLogger().warning("Relais : affectation de " + p.getName() + " refusée ou injoignable ("
                                + (erreur != null ? erreur.getMessage() : "code " + reponse.statusCode()) + ").");
                        p.sendMessage("§cImpossible de rejoindre le Build Battle pour l'instant (relais injoignable).");
                        return;
                    }
                    connecter(p);
                }));
    }

    private void connecter(Player joueur) {
        ByteArrayOutputStream octets = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(octets)) {
            out.writeUTF("Connect");
            out.writeUTF(plugin.getConfig().getString("serveur", "Kanvas"));
        } catch (IOException e) {
            return;
        }
        joueur.sendPluginMessage(plugin, "BungeeCord", octets.toByteArray());
    }

    /**
     * Toutes les 5 s : retire de la liste les parties privées lancées ou annulées sur Kanvas (clé
     * « buildbattle-fermee-<id> » déposée par KV_BuildBattle), et celles listées depuis trop longtemps.
     */
    void verifierFermees() {
        long expiration = plugin.getConfig().getLong("expiration-liste-minutes", 30);
        if (expiration > 0) {
            Instant limite = Instant.now().minus(Duration.ofMinutes(expiration));
            for (Partie p : List.copyOf(parId.values())) {
                if (p.creee.isBefore(limite)) retirer(p.id);
            }
        }
        String url = plugin.getConfig().getString("relay-url", "");
        if (url == null || url.isBlank() || parId.isEmpty()) return;
        String jeton = plugin.getConfig().getString("relay-token", "");
        for (String id : List.copyOf(parId.keySet())) {
            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/assignment/buildbattle-fermee-" + id))
                    .header("X-Kalium-Relay-Token", jeton)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            http.sendAsync(requete, HttpResponse.BodyHandlers.discarding()).thenAccept(r -> {
                if (r.statusCode() == 200) Bukkit.getScheduler().runTask(plugin, () -> retirer(id));
            }).exceptionally(e -> null); // relais momentanément injoignable : nouvel essai au prochain tour
        }
    }

    void retirer(String id) {
        Partie p = parId.remove(id);
        if (p != null) parCode.remove(p.code);
    }
}
