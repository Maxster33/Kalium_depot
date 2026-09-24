package fr.kalium.bingo.game;

import fr.kalium.bingo.network.AssignmentService;
import fr.kalium.bingo.network.RelayClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Abandon volontaire et DEFINITIF d'une partie par un joueur (menu Objectifs -&gt; objet
 * "Abandonner la partie", confirmation demandee - voir fr.kalium.bingo.gui.AbandonConfirmMenu) -
 * AJOUTE le 24/09/2026, demande explicite de l'utilisateur : "il faut ... un objet qui permet
 * d'abandonner la partie ... etes-vous sur de vouloir abandonner ? (attention vous ne pourrez pas
 * revenir) ... le joueur peut repondre oui et quitter la partie."
 *
 * Contrairement a une simple deconnexion (reconnexion normale possible, voir
 * PlayerConnectListener), un abandon est PERMANENT : le joueur est marque comme tel sur son
 * instance (BingoInstance.markAbandoned) et ne sera plus jamais traite comme "encore dans la
 * partie" (voir GameManager.findGameOf), meme s'il se reconnecte au serveur par la suite. Le reste
 * de son equipe et les autres equipes continuent normalement - un abandon n'affecte QUE le joueur
 * qui l'a demande.
 */
public final class AbandonService {

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final PlayerResetService playerReset;
    private final RelayClient relayClient;
    private final AssignmentService assignmentService;
    private final GameEndService gameEndService;

    public AbandonService(JavaPlugin plugin, GameManager gameManager, PlayerResetService playerReset, RelayClient relayClient,
                           AssignmentService assignmentService, GameEndService gameEndService) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.playerReset = playerReset;
        this.relayClient = relayClient;
        this.assignmentService = assignmentService;
        this.gameEndService = gameEndService;
    }

    public void abandon(Player player) {
        UUID playerId = player.getUniqueId();
        Optional<BingoGame> gameOpt = gameManager.findGameOf(playerId);
        if (gameOpt.isEmpty()) {
            player.sendMessage(Component.text("Aucune partie Bingo en cours pour vous.", NamedTextColor.RED));
            return;
        }
        BingoGame game = gameOpt.get();
        Optional<BingoInstance> instanceOpt = game.findInstanceOf(playerId);
        if (instanceOpt.isEmpty()) {
            return;
        }
        instanceOpt.get().markAbandoned(playerId);

        // Inventaire ENTIEREMENT vide + point de spawn supprime (0.1.18, demande explicite de
        // l'utilisateur - remplace l'ancien retrait du seul papier "Objectifs").
        playerReset.reset(player);
        player.sendMessage(Component.text("Vous avez abandonné la partie.", NamedTextColor.RED));
        assignmentService.sendBackToKalGames(player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> relayClient.clearActiveGame(playerId));

        // Si, avec cet abandon, plus AUCUN joueur n'est actif sur cette partie (tous ont
        // abandonne/deconnecte), inutile d'attendre game.no-players-abandon-after-seconds : la
        // partie se termine tout de suite (voir GameEndService.checkImmediateAbandonment).
        // 0.1.18 : si cet abandon vide entierement son equipe ET qu'il ne reste plus qu'une seule
        // equipe en jeu, la partie se termine (voir GameEndService.checkTeamAbandonment).
        gameEndService.checkTeamAbandonment(game);
        gameEndService.checkImmediateAbandonment(game);
    }
}
