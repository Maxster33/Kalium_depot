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
        if (points <= 0 || !ranked || excluded(player)) {
            return;
        }
        plugin.stats().addPoints(minigame.id(), player.getUniqueId(), player.getName(), points);
        plugin.boards().refreshSoon();
        if (!plugin.getConfig().getBoolean("scoring.bridge.enabled", true)) {
            return;
        }
        run(plugin.getConfig().getStringList("scoring.bridge.win-commands"), player.getName(), points, minigame);
    }

    /** Temps d'une course : garde le meilleur temps du joueur. Renvoie true si c'est son record personnel. */
    public boolean recordTime(Player player, Minigame minigame, long millis, boolean ranked) {
        if (!ranked || excluded(player)) {
            return false;
        }
        boolean record = plugin.stats().recordTime(minigame.id(), player.getUniqueId(), player.getName(), millis);
        plugin.boards().refreshSoon();
        return record;
    }

    /** Temps d'un tour (courses de bateau) : garde le meilleur temps sur 1 tour. Renvoie true si c'est son record personnel. */
    public boolean recordLap(Player player, Minigame minigame, long millis, boolean ranked) {
        if (!ranked || excluded(player)) {
            return false;
        }
        boolean record = plugin.stats().recordLap(minigame.id(), player.getUniqueId(), player.getName(), millis);
        plugin.boards().refreshSoon();
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
