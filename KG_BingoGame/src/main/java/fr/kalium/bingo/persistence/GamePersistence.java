package fr.kalium.bingo.persistence;

import fr.kalium.bingo.game.BingoGame;
import fr.kalium.bingo.game.BingoInstance;
import fr.kalium.bingo.game.GameManager;
import fr.kalium.bingo.game.GameState;
import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.grid.Difficulty;
import fr.kalium.bingo.grid.GridCell;
import fr.kalium.bingo.grid.Objective;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Sauvegarde/restaure sur disque l'etat des parties EN COURS (grille, progression, equipes,
 * instances, chronometre - voir GameManager, portee volontairement limitee aux parties deja
 * LANCEES) pour qu'elles survivent a un redemarrage ou un crash du serveur - demande explicite de
 * l'utilisateur, 23/09/2026 : "la partie doit continuer meme si le serveur est redémarré ou si il
 * crash".
 *
 * Un fichier YAML par partie, dans le dossier de donnees du plugin (games/&lt;gameId&gt;.yml) -
 * meme approche que config.yml/objectives.yml, pas de dependance supplementaire. Sauvegarde
 * SYNCHRONE (fichier minuscule, meme convention que plugin.saveConfig() ailleurs dans ce projet) :
 *  - a chaque nouvelle case validee (voir ObjectiveValidationTask)
 *  - au lancement de la partie (voir PartyStarter)
 *  - periodiquement, en filet de securite (voir BingoPlugin, saveAll) - couvre le cas rare ou une
 *    sauvegarde ponctuelle n'aurait pas eu le temps de s'executer avant un crash.
 * Le fichier est supprime des la fin reelle de la partie (voir GameEndService.scheduleCleanup),
 * pour ne jamais tenter de restaurer une partie deja terminee.
 *
 * Chronometre : le temps RESTANT est sauvegarde (pas l'instant de depart absolu) - au rechargement,
 * BingoGame.restoreInProgress() reconstruit un depart fictif tel que le temps restant reprenne
 * EXACTEMENT ou il en etait a la derniere sauvegarde (le chronometre est mis en pause pendant que
 * le serveur est eteint, choix confirme via AskUserQuestion le 23/09/2026).
 */
public final class GamePersistence {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final GameManager gameManager;
    private final File dataDir;

    public GamePersistence(JavaPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gameManager = gameManager;
        this.dataDir = new File(plugin.getDataFolder(), "games");
    }

    private File fileFor(String gameId) {
        return new File(dataDir, gameId + ".yml");
    }

    /** Sauvegarde une partie - ne fait rien si elle n'est pas (ou plus) IN_PROGRESS. */
    public void save(BingoGame game) {
        if (game.getState() != GameState.IN_PROGRESS) {
            return;
        }
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            logger.warning("[KG_BingoGame] Impossible de créer le dossier de sauvegarde des parties (" + dataDir + ").");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("game-id", game.getGameId());
        yaml.set("seed", game.getSeed());
        yaml.set("duration-seconds", game.getDuration().getSeconds());
        yaml.set("remaining-seconds", game.getRemaining().getSeconds());
        yaml.set("settings", game.getSettings().encode()); // 0.3.0
        yaml.set("elapsed-seconds", game.getElapsed().getSeconds()); // 0.3.0 (blackout : pas de temps restant)

        List<Map<String, Object>> instances = new ArrayList<>();
        for (BingoInstance instance : game.getInstances()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("team", instance.getTeam().getTeamNumber());
            map.put("world", instance.getInstanceId());
            List<String> players = new ArrayList<>();
            for (UUID id : instance.getTeam().getPlayers()) {
                players.add(id.toString());
            }
            map.put("players", players);
            instances.add(map);
        }
        yaml.set("instances", instances);

        BingoGrid grid = game.getGrid();
        if (grid != null) {
            yaml.set("grid.size", grid.getSize());
            List<Map<String, Object>> cells = new ArrayList<>();
            for (GridCell cell : grid.getCells()) {
                Objective objective = cell.getObjective();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("material", objective.material().name());
                map.put("quantity", objective.quantity());
                map.put("difficulty", objective.difficulty().name());
                map.put("condition", objective.condition());
                cells.add(map);
            }
            yaml.set("grid.cells", cells);

            int total = grid.getSize() * grid.getSize();
            for (BingoInstance instance : game.getInstances()) {
                int team = instance.getTeam().getTeamNumber();
                List<Integer> validated = new ArrayList<>();
                for (int i = 0; i < total; i++) {
                    if (game.isValidated(team, i)) {
                        validated.add(i);
                    }
                }
                yaml.set("progress." + team, validated);
            }
            // 0.3.0 : journal ORDONNE des validations (equipe;case;joueur) - le rejouer redonne exactement les
            // memes points (1re equipe, bingos, points solo, voir ScoreEngine). "progress" reste ecrit pour
            // pouvoir revenir a une version precedente.
            List<String> log = new ArrayList<>();
            for (var v : game.getScoreEngine().log()) {
                log.add(v.team() + ";" + v.cell() + ";" + (v.player() == null ? "" : v.player()));
            }
            yaml.set("validations", log);
        }

        try {
            yaml.save(fileFor(game.getGameId()));
        } catch (IOException e) {
            logger.warning("[KG_BingoGame] Impossible de sauvegarder la partie '" + game.getGameId() + "' : " + e.getMessage());
        }
    }

    /** Sauvegarde toutes les parties actuellement IN_PROGRESS - voir BingoPlugin (filet de securite periodique). */
    public void saveAll(List<BingoGame> games) {
        for (BingoGame game : games) {
            save(game);
        }
    }

    /** A appeler des la fin REELLE d'une partie (voir GameEndService.scheduleCleanup) : une partie
     *  terminee ne doit plus jamais etre restauree au redemarrage suivant. */
    public void delete(String gameId) {
        File file = fileFor(gameId);
        if (file.exists() && !file.delete()) {
            logger.warning("[KG_BingoGame] Impossible de supprimer le fichier de sauvegarde de la partie '" + gameId + "'.");
        }
    }

    /**
     * Recharge toutes les parties sauvegardees (voir BingoPlugin, appele depuis ServerLoadEvent -
     * memes contraintes que LobbySlots.init(), la creation de monde est interdite pendant la phase
     * STARTUP). A appeler UNE SEULE FOIS, avant que des joueurs ne puissent se reconnecter.
     */
    public void loadAll() {
        if (!dataDir.exists()) {
            return;
        }
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }
        int restored = 0;
        for (File file : files) {
            try {
                restoreOne(file);
                restored++;
            } catch (RuntimeException e) {
                logger.severe("[KG_BingoGame] Impossible de restaurer la partie depuis '" + file.getName() + "' : " + e.getMessage());
            }
        }
        if (restored > 0) {
            logger.info("[KG_BingoGame] " + restored + " partie(s) restaurée(s) après redémarrage/crash.");
        }
    }

    private void restoreOne(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String gameId = yaml.getString("game-id");
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalStateException("champ 'game-id' manquant");
        }
        long seed = yaml.getLong("seed");
        long durationSeconds = yaml.getLong("duration-seconds");
        long remainingSeconds = yaml.getLong("remaining-seconds");

        List<GameManager.RestoredInstance> instances = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList("instances")) {
            int team = toInt(raw.get("team"));
            String world = String.valueOf(raw.get("world"));
            List<UUID> players = new ArrayList<>();
            Object playersRaw = raw.get("players");
            if (playersRaw instanceof List<?> list) {
                for (Object p : list) {
                    players.add(UUID.fromString(String.valueOf(p)));
                }
            }
            instances.add(new GameManager.RestoredInstance(team, players, world));
        }
        if (instances.isEmpty()) {
            throw new IllegalStateException("aucune instance dans la sauvegarde");
        }

        BingoGame game = gameManager.restoreGame(gameId, seed, Duration.ofSeconds(durationSeconds), instances);
        game.setSettings(fr.kalium.bingo.game.BingoSettings.parse(yaml.getString("settings")));

        int gridSize = yaml.getInt("grid.size");
        List<Map<?, ?>> rawCells = yaml.getMapList("grid.cells");
        if (gridSize > 0 && !rawCells.isEmpty()) {
            List<GridCell> cells = new ArrayList<>();
            for (Map<?, ?> raw : rawCells) {
                String materialName = String.valueOf(raw.get("material"));
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    throw new IllegalStateException("item inconnu dans la grille sauvegardée : '" + materialName + "'");
                }
                int quantity = toInt(raw.get("quantity"));
                Difficulty difficulty;
                try {
                    difficulty = Difficulty.valueOf(String.valueOf(raw.get("difficulty")));
                } catch (IllegalArgumentException e) {
                    difficulty = Difficulty.MEDIUM;
                }
                Object conditionRaw = raw.get("condition");
                String condition = conditionRaw == null ? "" : String.valueOf(conditionRaw);
                cells.add(new GridCell(new Objective(material, quantity, difficulty, condition)));
            }
            game.setGrid(new BingoGrid(gridSize, cells));

            List<String> log = yaml.getStringList("validations");
            ConfigurationSection progress = yaml.getConfigurationSection("progress");
            if (!log.isEmpty()) {
                for (String entry : log) {
                    String[] parts = entry.split(";", -1);
                    UUID player = parts.length > 2 && !parts[2].isBlank() ? UUID.fromString(parts[2]) : null;
                    game.markValidated(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), player);
                }
            } else if (progress != null) {
                // Sauvegarde d'avant 0.3.0 : ordre et joueurs inconnus.
                for (String teamKey : progress.getKeys(false)) {
                    int team = Integer.parseInt(teamKey);
                    for (int index : progress.getIntegerList(teamKey)) {
                        game.markValidated(team, index, null);
                    }
                }
            }
        }

        game.restoreInProgress(Duration.ofSeconds(remainingSeconds));
        if (game.getSettings().isBlackout()) {
            game.restoreElapsed(Duration.ofSeconds(yaml.getLong("elapsed-seconds"))); // 0.3.0
        }
        logger.info("[KG_BingoGame] Partie '" + gameId + "' restaurée (temps restant : " + remainingSeconds + "s).");
    }

    private int toInt(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(raw));
    }
}
