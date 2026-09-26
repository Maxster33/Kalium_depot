package fr.kalium.bingo.network;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 0.8.0 - resultats des parties envoyes au hub (kal-games) pour les classements (demande de LeKiwi06, 26/09/2026 :
 * brancher les points du Bingo sur les classements de KG_ScoreBoards).
 *
 * A la fin d'une partie, ses resultats (une ligne par joueur : uuid;pseudo;points) sont publies sur le relais HTTP
 * (cle « bingo-results-&lt;gameId&gt; », entree lue une seule fois, expiree au bout de 2 min), puis republies toutes
 * les minutes pendant 6 h, comme la fermeture de partie (PartyStatusNotifier) : le hub les lit quand il le peut et ne
 * credite qu'une fois (voir BingoResults cote KG_Bingo). Gardes dans results-outbox.yml pour survivre a un
 * redemarrage de ce serveur.
 */
public final class ResultsOutbox {

    private static final long REPUBLISH_FOR_MILLIS = 6L * 60 * 60 * 1000;
    private static final long REPUBLISH_INTERVAL_TICKS = 20L * 60;

    private record Pending(String body, long at) {
    }

    private final JavaPlugin plugin;
    private final RelayClient relayClient;
    private final File file;
    /** gameId -&gt; resultats a publier. Manipule sur le thread principal. */
    private final Map<String, Pending> pending = new HashMap<>();

    public ResultsOutbox(JavaPlugin plugin, RelayClient relayClient) {
        this.plugin = plugin;
        this.relayClient = relayClient;
        this.file = new File(plugin.getDataFolder(), "results-outbox.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::republish, 20L * 10, REPUBLISH_INTERVAL_TICKS);
    }

    /** Resultats d'une partie terminee : publies tout de suite, puis republies (voir la classe). */
    public void add(String gameId, String body) {
        pending.put(gameId, new Pending(body, System.currentTimeMillis()));
        save();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> relayClient.postResults(gameId, body));
    }

    private void republish() {
        long now = System.currentTimeMillis();
        if (pending.values().removeIf(p -> now - p.at() > REPUBLISH_FOR_MILLIS)) {
            save();
        }
        if (pending.isEmpty()) {
            return;
        }
        Map<String, String> copy = new HashMap<>();
        pending.forEach((id, p) -> copy.put(id, p.body()));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> copy.forEach(relayClient::postResults));
    }

    private void load() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = yml.getConfigurationSection("pending");
        if (s == null) {
            return;
        }
        for (String id : s.getKeys(false)) {
            pending.put(id, new Pending(s.getString(id + ".body", ""), s.getLong(id + ".at")));
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        pending.forEach((id, p) -> {
            yml.set("pending." + id + ".body", p.body());
            yml.set("pending." + id + ".at", p.at());
        });
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[KG_BingoGame] Impossible d'enregistrer results-outbox.yml : " + e.getMessage());
        }
    }
}
