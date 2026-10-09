package fr.kalium.rewards;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Envoi des récompenses vers Event : message au format de KS_RewardsGUI, déposé dans la boîte « event » de KaliumRelay
 * 1.3.0. Les messages attendent dans plugins/KG_Rewards/envois.yml jusqu'à ce que le relais les accepte (nouvel essai
 * toutes les 15 secondes) : rien n'est perdu si le relais ou le proxy est arrêté.
 */
final class Envois {

    private final KGRewards plugin;
    private final File fichier;
    private final List<String> enAttente = new ArrayList<>();
    /** 1.1.0 : première ligne d'un message en attente destiné à une autre boîte que celle de config.yml. */
    private static final String MARQUE_BOITE = "#boite:";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    Envois(KGRewards plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "envois.yml");
        enAttente.addAll(YamlConfiguration.loadConfiguration(fichier).getStringList("en-attente"));
    }

    /** Prépare et met en file une récompense (rien si le contenu est vide). */
    void envoyer(UUID joueur, String nom, String raison, List<Map<String, Object>> contenu) {
        envoyer(joueur, nom, raison, contenu, null);
    }

    /** 1.1.0 : boite non nulle = boîte aux lettres de ce message (essais vers un autre serveur), sinon celle de config.yml. */
    void envoyer(UUID joueur, String nom, String raison, List<Map<String, Object>> contenu, String boite) {
        if (contenu.isEmpty()) {
            plugin.getLogger().info("Récompense vide (niveau non configuré) : " + nom + " - " + raison);
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("joueur", joueur.toString());
        yaml.set("nom", nom);
        yaml.set("origine", plugin.getConfig().getString("origine", "Kal-Games"));
        yaml.set("raison", raison);
        yaml.set("date", System.currentTimeMillis());
        yaml.set("contenu", new ArrayList<>(contenu));
        // 1.2.0 : valeur moyenne du niveau, affichée sur le coffre par KS_RewardsGUI 1.3.0.
        if (contenu instanceof Butin.Contenu tire && tire.valeur > 0) {
            yaml.set("valeur", tire.valeur);
        }
        synchronized (this) {
            enAttente.add((boite == null ? "" : MARQUE_BOITE + boite + "\n") + yaml.saveToString());
            sauver();
        }
        plugin.getLogger().info("Récompense pour " + nom + " : " + raison);
    }

    synchronized int enAttente() {
        return enAttente.size();
    }

    /** Hors du fil principal : envoie les messages en attente, dans l'ordre, jusqu'au premier échec. */
    void vider() {
        String url = plugin.getConfig().getString("relay-url", "");
        String jeton = plugin.getConfig().getString("relay-token", "");
        if (url.isBlank() || jeton.isBlank()) {
            return;
        }
        String base = (url.endsWith("/") ? url.substring(0, url.length() - 1) : url) + "/mail/";
        String boiteParDefaut = plugin.getConfig().getString("boite", "event");
        while (true) {
            String message;
            synchronized (this) {
                if (enAttente.isEmpty()) {
                    return;
                }
                message = enAttente.get(0);
            }
            String boite = boiteParDefaut;
            String corps = message;
            if (message.startsWith(MARQUE_BOITE) && message.indexOf('\n') > 0) {
                boite = message.substring(MARQUE_BOITE.length(), message.indexOf('\n')).trim();
                corps = message.substring(message.indexOf('\n') + 1);
            }
            try {
                HttpResponse<String> reponse = http.send(HttpRequest.newBuilder(URI.create(base + boite))
                        .timeout(Duration.ofSeconds(10)).header("X-Kalium-Relay-Token", jeton)
                        .POST(HttpRequest.BodyPublishers.ofString(corps)).build(), HttpResponse.BodyHandlers.ofString());
                if (reponse.statusCode() != 200) {
                    plugin.getLogger().warning("Relais : réponse " + reponse.statusCode() + " à l'envoi d'une récompense.");
                    return;
                }
            } catch (IOException | InterruptedException e) {
                return;
            }
            synchronized (this) {
                enAttente.remove(0);
                sauver();
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("en-attente", enAttente);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer envois.yml : " + e.getMessage());
        }
    }
}
