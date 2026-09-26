package fr.kalium.bingo.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

/**
 * Chronometre de la partie + scores de chaque equipe, affiches en PERMANENCE au-dessus de la barre
 * de vie (action bar - meme mecanisme que RaceInstance/GameInstance cote KalGames pour un affichage
 * persistant, voir player.sendActionBar) - AJOUTE le 24/09/2026, demande explicite de
 * l'utilisateur : "le chrono de la partie doit être affiché sur l'écran au dessus de la barre de
 * vie, avec les scores actuels de chaque équipe."
 *
 * Reenvoye periodiquement (voir tick(), appele par BingoPlugin) plutot qu'une seule fois : l'action
 * bar de Minecraft s'efface d'elle-meme au bout de quelques secondes si elle n'est pas rafraichie.
 */
public final class GameHudService {

    private final GameManager gameManager;

    public GameHudService(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void tick() {
        for (BingoGame game : gameManager.getActiveGames()) {
            if (game.getState() != GameState.IN_PROGRESS) {
                continue;
            }
            Component message = build(game);
            for (BingoInstance instance : game.getInstances()) {
                for (UUID playerId : instance.getTeam().getPlayers()) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.sendActionBar(message);
                    }
                }
            }
        }
    }

    private Component build(BingoGame game) {
        // 0.3.0 : blackout = temps de jeu ecoule (pas de chrono) ; mode bingos = temps restant.
        boolean blackout = game.getSettings().isBlackout();
        Component message = Component.text(blackout ? "Blackout " + format(game.getElapsed()) : format(game.getRemaining()),
                NamedTextColor.AQUA);
        // 0.8.1 : a partir de 3 equipes, format court (la ligne etait illisible a 4 equipes - LeKiwi06, 26/09/2026) :
        // « ⏱ 45m12s | A:42pts·1/3 | B:30pts·0/3 | C:18pts·0/3 | D:7pts·0/3 » (la barre d'action n'a qu'une ligne).
        if (game.getInstances().size() >= 3) {
            for (BingoInstance instance : game.getInstances()) {
                int team = instance.getTeam().getTeamNumber();
                message = message.append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(TeamStyle.letter(team) + ":", TeamStyle.color(team)))
                        .append(Component.text(fr.kalium.bingo.score.ScoreEngine.format(game.score(team)) + "pts", NamedTextColor.WHITE))
                        .append(blackout || game.getScoreEngine() == null ? Component.empty()
                                : Component.text("·" + game.getScoreEngine().bingoCount(team) + "/" + game.getSettings().bingosRequired(),
                                NamedTextColor.GRAY));
            }
            return message;
        }
        for (BingoInstance instance : game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            message = message.append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Équipe " + TeamStyle.letter(team) + " : ", TeamStyle.color(team)))
                    .append(Component.text(fr.kalium.bingo.score.ScoreEngine.format(game.score(team)) + " pts", NamedTextColor.WHITE))
                    .append(blackout || game.getScoreEngine() == null ? Component.empty()
                            : Component.text(" (" + game.getScoreEngine().bingoCount(team) + "/" + game.getSettings().bingosRequired()
                            + " bingos)", NamedTextColor.GRAY));
        }
        return message;
    }

    private String format(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return h > 0 ? String.format("⏱ %dh%02dm%02ds", h, m, s) : String.format("⏱ %dm%02ds", m, s);
    }
}
