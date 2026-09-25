package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.model.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Passerelle vers le datapack "minigames" existant (classement general) : execute des commandes
 * configurables dans config.yml (scoring.bridge.*). Placeholders : %player%, %points%, %obj%, %minigame%.
 */
public final class ScoreBridge {

    private final KalGames plugin;

    public ScoreBridge(KalGames plugin) {
        this.plugin = plugin;
    }

    private String objective(Minigame minigame) {
        String format = plugin.getConfig().getString("scoring.objective-format", "%minigame%_victoires");
        return format.replace("%minigame%", minigame.id());
    }

    private void run(List<String> commands, String player, int points, Minigame minigame) {
        for (String command : commands) {
            String line = command
                    .replace("%player%", player)
                    .replace("%points%", String.valueOf(points))
                    .replace("%obj%", objective(minigame))
                    .replace("%minigame%", minigame.id());
            if (line.startsWith("/")) {
                line = line.substring(1);
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
            } catch (Exception e) {
                plugin.getLogger().warning("Commande de score en échec (" + line + ") : " + e.getMessage());
            }
        }
    }

    /** Les points d'un operateur ne comptent pas pour les classements (stats.exclude-operators, actif par defaut). */
    public boolean excluded(Player player) {
        return player.isOp() && plugin.getConfig().getBoolean("stats.exclude-operators", true);
    }

    /**
     * Points gagnes par un joueur : enregistres dans les classements de KalGames (general et du mois), puis
     * transmis au datapack. ranked = false : la partie ne compte pas (entrainement...).
     */
    public void award(Player player, Minigame minigame, int points, boolean ranked) {
        award(player, minigame, points, ranked, null);
    }

    /**
     * 1.19.0 : version reliee a une partie (identifiant de match dans le journal). Renvoie true si les points ont ete
     * comptes. Chaque attribution, comptee ou non, est enregistree dans le journal de KG_ScoreBoards (evenement
     * « points », avec la raison si elle n'est pas comptee) : la commande /classements verifier de KG_ScoreBoards
     * retrouve ainsi les points non comptes des derniers jours et permet de les crediter.
     */
    public boolean award(GameInstance game, Player player, int points) {
        return award(player, game.minigame(), points, game.ranked(player), game);
    }

    private boolean award(Player player, Minigame minigame, int points, boolean ranked, GameInstance game) {
        if (points <= 0) {
            return false;
        }
        String reason = excluded(player) ? "operateur" : !ranked ? "partie-non-classee" : null;
        log(minigame, "points", player, game, reason, "points", points);
        if (reason != null) {
            return false;
        }
        plugin.stats().addPoints(minigame.id(), player.getUniqueId(), player.getName(), points);
        plugin.ranking().boards().refreshSoon();
        if (!plugin.getConfig().getBoolean("scoring.bridge.enabled", true)) {
            return true; // comptes ; passerelle vers le datapack desactivee
        }
        run(plugin.getConfig().getStringList("scoring.bridge.win-commands"), player.getName(), points, minigame);
        return true;
    }

    /** 1.19.0 : evenement du journal des parties de KG_ScoreBoards (voir award). */
    private void log(Minigame minigame, String type, Player player, GameInstance game, String reason, String valueKey, Object value) {
        java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();
        fields.put("match", game == null ? null : game.matchId());
        fields.put("arena", game == null ? null : game.arena().id());
        fields.put("public", game == null ? null : game.isPublic());
        fields.put("player", player.getUniqueId().toString());
        fields.put("name", player.getName());
        fields.put("platform", player.getUniqueId().getMostSignificantBits() == 0 ? "bedrock" : "java");
        fields.put(valueKey, value);
        fields.put("counted", reason == null);
        fields.put("reason", reason);
        plugin.ranking().log(minigame.id(), type, fields);
    }

    /** 1.19.0 : temps d'une course reliee a une partie, enregistre dans le journal (evenement « time »). */
    public boolean recordTime(GameInstance game, Player player, long millis) {
        boolean ranked = game.ranked(player);
        String reason = excluded(player) ? "operateur" : !ranked ? "partie-non-classee" : null;
        log(game.minigame(), "time", player, game, reason, "millis", millis);
        return recordTime(player, game.minigame(), millis, ranked);
    }

    /** Temps d'une course : garde le meilleur temps du joueur. Renvoie true si c'est son record personnel. */
    public boolean recordTime(Player player, Minigame minigame, long millis, boolean ranked) {
        if (!ranked || excluded(player)) {
            return false;
        }
        boolean record = plugin.stats().recordTime(minigame.id(), player.getUniqueId(), player.getName(), millis);
        plugin.ranking().boards().refreshSoon();
        return record;
    }

    /** Temps d'un tour (courses de bateau) : garde le meilleur temps sur 1 tour. Renvoie true si c'est son record personnel. */
    public boolean recordLap(Player player, Minigame minigame, long millis, boolean ranked) {
        if (!ranked || excluded(player)) {
            return false;
        }
        boolean record = plugin.stats().recordLap(minigame.id(), player.getUniqueId(), player.getName(), millis);
        plugin.ranking().boards().refreshSoon();
        return record;
    }

    /** Deconnexion en plein combat (sanction geree par le datapack). */
    public void combatLeave(String playerName, Minigame minigame) {
        if (!plugin.getConfig().getBoolean("scoring.bridge.enabled", true)) {
            return;
        }
        run(plugin.getConfig().getStringList("scoring.bridge.combat-leave-commands"), playerName, 0, minigame);
    }
}
