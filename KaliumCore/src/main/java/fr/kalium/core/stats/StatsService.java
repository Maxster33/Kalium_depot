package fr.kalium.core.stats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suivi et persistance des statistiques par joueur.
 * <p>
 * Stockage : un fichier YAML par joueur dans {@code plugins/KaliumCore/playerdata/<uuid>.yml} -
 * autonome, sans base de donnees externe, pour rester facilement transportable sur un serveur
 * temporaire (copier/coller le dossier du plugin suffit).
 */
public final class StatsService {

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final List<StatSection> sections = new ArrayList<>();

    public StatsService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
    }

    // ------------------------------------------------------------------ sections (ecran Statistiques)

    public void registerSection(StatSection section) {
        sections.add(section);
    }

    public List<StatSection> sections() {
        return sections;
    }

    // ------------------------------------------------------------------ cycle de vie joueur

    public void onJoin(Player player) {
        UUID id = player.getUniqueId();
        names.put(id, player.getName());
        load(id);
        sessionStart.put(id, System.currentTimeMillis());
    }

    public void onQuit(Player player) {
        UUID id = player.getUniqueId();
        flushPlaytime(id);
        save(id);
        sessionStart.remove(id);
        cache.remove(id);
        names.remove(id);
    }

    /** A appeler periodiquement : ajoute le temps de jeu ecoule depuis le dernier flush, sans deconnecter personne. */
    public void tick() {
        for (UUID id : new ArrayList<>(sessionStart.keySet())) {
            flushPlaytime(id);
        }
    }

    public void saveDirty() {
        for (UUID id : new ArrayList<>(dirty)) {
            save(id);
        }
    }

    /** Sauvegarde immediate de tous les joueurs en ligne (utilise a l'arret du plugin). */
    public void saveAllOnline(Collection<? extends Player> players) {
        for (Player player : players) {
            flushPlaytime(player.getUniqueId());
            save(player.getUniqueId());
        }
    }

    private void flushPlaytime(UUID id) {
        Long start = sessionStart.get(id);
        if (start == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsedSeconds = Math.max(0, (now - start) / 1000L);
        if (elapsedSeconds > 0) {
            get(id).addPlaytime(elapsedSeconds);
            dirty.add(id);
        }
        sessionStart.put(id, now);
    }

    // ------------------------------------------------------------------ acces

    public PlayerStats get(UUID id) {
        PlayerStats existing = cache.get(id);
        return existing != null ? existing : load(id);
    }

    /** Stats a jour pour l'affichage (inclut le temps de jeu de la session en cours, pas encore sauvegarde). */
    public PlayerStats liveStats(Player player) {
        flushPlaytime(player.getUniqueId());
        return get(player.getUniqueId());
    }

    public void markDirty(UUID id) {
        dirty.add(id);
    }

    // ------------------------------------------------------------------ chargement / sauvegarde

    private PlayerStats load(UUID id) {
        File file = new File(folder, id + ".yml");
        PlayerStats stats = new PlayerStats(id);
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            stats.playtimeSeconds(config.getLong("playtime-seconds", 0));
            stats.levelsSpent(config.getInt("levels-spent", 0));
            stats.blocksBroken(config.getLong("blocks-broken", 0));
            stats.blocksPlaced(config.getLong("blocks-placed", 0));
            stats.monstersKilled(config.getLong("monsters-killed", 0));
        }
        cache.put(id, stats);
        return stats;
    }

    public void save(UUID id) {
        PlayerStats stats = cache.get(id);
        if (stats == null) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        String name = names.get(id);
        if (name != null) {
            config.set("name", name);
        }
        config.set("playtime-seconds", stats.playtimeSeconds());
        config.set("levels-spent", stats.levelsSpent());
        config.set("blocks-broken", stats.blocksBroken());
        config.set("blocks-placed", stats.blocksPlaced());
        config.set("monsters-killed", stats.monstersKilled());
        try {
            folder.mkdirs();
            config.save(new File(folder, id + ".yml"));
            dirty.remove(id);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer les statistiques de "
                    + (name != null ? name : id) + " : " + e.getMessage());
        }
    }
}
