package fr.kalium.bingo.game;

import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.grid.GridCell;
import fr.kalium.bingo.grid.Objective;
import fr.kalium.bingo.persistence.GamePersistence;
import fr.kalium.bingo.score.ScoreEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.UUID;

/**
 * Detection automatique de validation des objectifs (section 3 du cahier des charges, jamais
 * implementee jusqu'ici - voir GridCell.validated, reste inerte, remplace par
 * BingoGame.teamProgress). Choix confirme par l'utilisateur (AskUserQuestion, 23/09/2026) : une
 * case se valide des qu'UN membre de l'equipe possede, a lui seul, la quantite requise de l'item
 * dans son inventaire (pas de somme entre plusieurs inventaires, pas de depot manuel dans un
 * coffre) ; une fois validee, elle reste acquise pour le reste de la partie meme si l'objet est
 * ensuite perdu/depense (pas de reverification - voir BingoGame.markValidated, qui ignore les
 * cases deja validees).
 *
 * Tourne cote serveur sur le thread principal (acces necessaire a l'API Bukkit : inventaires des
 * joueurs), declenchee periodiquement par BingoPlugin (voir onEnable). Volontairement simple :
 * une case = un material + une quantite - le champ Objective.condition n'est PAS interprete, comme
 * partout ailleurs dans ce plugin pour l'instant (voir Objective.java).
 *
 * Appelle GameEndService.checkWin() juste apres CHAQUE nouvelle validation (section 5, demande
 * explicite de l'utilisateur, 23/09/2026 : "si une equipe rempli tous les objectifs alors tous
 * les joueurs sont renvoyés en salle d'attente...") - des qu'une equipe termine la grille, la
 * partie est terminee immediatement (voir le "break outer" ci-dessous, qui arrete d'y traiter
 * d'autres cases une fois que ce tick l'a constatee).
 */
public final class ObjectiveValidationTask {

    private final GameManager gameManager;
    private final GameEndService gameEndService;
    private final GamePersistence gamePersistence;
    private final fr.kalium.bingo.gui.GameItems gameItems;

    public ObjectiveValidationTask(GameManager gameManager, GameEndService gameEndService, GamePersistence gamePersistence,
                                   fr.kalium.bingo.gui.GameItems gameItems) {
        this.gameManager = gameManager;
        this.gameEndService = gameEndService;
        this.gamePersistence = gamePersistence;
        this.gameItems = gameItems;
    }

    public void tick() {
        for (BingoGame game : gameManager.getActiveGames()) {
            if (game.getState() != GameState.IN_PROGRESS || game.isFrozen()) {
                continue;
            }
            BingoGrid grid = game.getGrid();
            if (grid == null) {
                continue;
            }
            List<GridCell> cells = grid.getCells();
            outer:
            for (BingoInstance instance : game.getInstances()) {
                int team = instance.getTeam().getTeamNumber();
                for (int i = 0; i < cells.size(); i++) {
                    if (game.isValidated(team, i)) {
                        continue;
                    }
                    Objective objective = cells.get(i).getObjective();
                    Player holder = holderOf(instance, objective);
                    if (holder == null) {
                        continue;
                    }
                    var result = game.markValidated(team, i, holder.getUniqueId());
                    if (result != null) {
                        announce(game, team, holder, objective, result);
                        // Sauvegarde la progression AVANT de verifier la victoire (voir GamePersistence) :
                        // si checkWin termine la partie a l'instant, GameEndService supprimera de toute
                        // facon ce fichier juste apres (scheduleCleanup) - sauvegarder d'abord est sans
                        // consequence et couvre le cas (bien plus frequent) ou la partie continue.
                        gamePersistence.save(game);
                        gameEndService.checkWin(game, team);
                        if (game.getState() != GameState.IN_PROGRESS) {
                            break outer; // partie terminee (victoire) - inutile de continuer a valider
                        }
                    }
                }
            }
        }
    }

    /** Membre CONNECTE (et n'ayant pas abandonne, voir AbandonService) de l'equipe qui possede, a lui seul,
     *  la quantite requise - null si aucun (0.3.0 : le joueur est retenu pour les annonces et les points solo). */
    private Player holderOf(BingoInstance instance, Objective objective) {
        for (UUID playerId : instance.getTeam().getPlayers()) {
            if (instance.hasAbandoned(playerId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && countMaterial(player, objective.material()) >= objective.quantity()) {
                return player;
            }
        }
        return null;
    }

    private int countMaterial(Player player, Material material) {
        int total = 0;
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            // 0.3.0 : le papier "Objectifs" (interface) ne compte pas pour l'objectif PAPER.
            if (item != null && item.getType() == material && !gameItems.isOurs(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * Annonces dans le tchat de TOUS les joueurs de la partie (0.3.0 - demande explicite de LeKiwi06,
     * 24/09/2026 : "equipe X valide cet item en 1er (pseudo du joueur) + X points !", "equipe X valide un
     * bingo (tous les participants) + X points !").
     */
    private void announce(BingoGame game, int team, Player holder, Objective objective, ScoreEngine.Result result) {
        Component item = Component.translatable(objective.material().translationKey())
                .color(NamedTextColor.WHITE);
        if (objective.quantity() > 1) {
            item = Component.text(objective.quantity() + " ", NamedTextColor.WHITE).append(item);
        }
        Component line = Component.text("Équipe " + team + " valide ", NamedTextColor.GREEN)
                .append(item)
                .append(Component.text(result.first() ? " en 1er" : "", NamedTextColor.GOLD))
                .append(Component.text(" (" + holder.getName() + ") ", NamedTextColor.GRAY))
                .append(Component.text("+" + ScoreEngine.format(result.itemPoints()) + " points !", NamedTextColor.YELLOW));
        broadcast(game, line);
        for (ScoreEngine.BingoEvent bingo : result.bingos()) {
            StringBuilder names = new StringBuilder();
            for (UUID id : bingo.participants()) {
                String name = Bukkit.getOfflinePlayer(id).getName();
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(name != null ? name : "?");
            }
            String detail = bingo.first() && bingo.hardOnly() ? " en 1er, difficile"
                    : bingo.first() ? " en 1er" : bingo.hardOnly() ? " difficile" : "";
            Component bingoLine = Component.text("Équipe " + team + " valide un bingo : " + bingo.name(), NamedTextColor.AQUA)
                    .append(Component.text(detail, NamedTextColor.GOLD))
                    .append(Component.text(" (" + names + ") ", NamedTextColor.GRAY))
                    .append(bingo.gain() > 0
                            ? Component.text("+" + ScoreEngine.format(bingo.gain()) + " points !", NamedTextColor.YELLOW)
                            : Component.text("(pas de bonus)", NamedTextColor.DARK_GRAY));
            broadcast(game, bingoLine);
        }
    }

    private void broadcast(BingoGame game, Component message) {
        for (BingoInstance instance : game.getInstances()) {
            for (UUID playerId : instance.getTeam().getPlayers()) {
                if (instance.hasAbandoned(playerId)) {
                    continue;
                }
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
    }
}
