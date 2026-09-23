package fr.kalium.bingo.command;

import fr.kalium.bingo.game.BingoGame;
import fr.kalium.bingo.game.GameManager;
import fr.kalium.bingo.game.PartyStarter;
import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.grid.GridCell;
import fr.kalium.bingo.grid.ObjectiveLibrary;
import fr.kalium.bingo.world.LobbyCaptureService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commandes d'administration minimales de KalBingo (section 11 du cahier des charges :
 * "ne creer que les commandes reellement necessaires") :
 *  - definition/mise a jour de la salle d'attente : selection "comme pour les arenes sur
 *    kal-games" (demande explicite de l'utilisateur), un coin = la position actuelle du
 *    joueur, recapturable a tout moment. Gardees pour les habitues des commandes texte,
 *    mais l'utilisateur a demande en plus un menu graphique equivalent (/menu, voir
 *    gui.LobbyMenu) - la logique est partagee via LobbyCaptureService pour ne pas dupliquer ;
 *  - demarrage MANUEL d'une partie en attente (placeholder de test, voir PartyStarter -
 *    le vrai declenchement viendra avec l'etape interface/chronometre, pas encore traitee) ;
 *  - grille (section 2) : "grid reload" recharge objectives.yml depuis le disque sans
 *    redemarrer le serveur, "grid show &lt;gameId&gt;" affiche en chat la grille generee pour
 *    une partie EN COURS - utile pour verifier la generation en attendant l'interface/carte
 *    custom (etape suivante de l'ordre de priorite, pas encore traitee).
 */
public class BingoAdminCommand implements CommandExecutor {

    private final LobbyCaptureService lobbyCaptureService;
    private final PartyStarter partyStarter;
    private final GameManager gameManager;
    private final ObjectiveLibrary objectiveLibrary;

    public BingoAdminCommand(LobbyCaptureService lobbyCaptureService, PartyStarter partyStarter,
                              GameManager gameManager, ObjectiveLibrary objectiveLibrary) {
        this.lobbyCaptureService = lobbyCaptureService;
        this.partyStarter = partyStarter;
        this.gameManager = gameManager;
        this.objectiveLibrary = objectiveLibrary;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("start")) {
            PartyStarter.Result result = partyStarter.start(args[1]);
            if (result.scheduled()) {
                // Un operateur respecte le meme delai minimum que l'hote (demande explicite de
                // l'utilisateur : "respecte le délai comme l'hôte") - pas un echec, la partie
                // demarrera automatiquement.
                sender.sendMessage(result.message());
            } else if (result.ok()) {
                sender.sendMessage("§aPartie '" + args[1] + "' démarrée (" + result.game().getInstances().size() + " équipe(s)).");
            } else {
                sender.sendMessage("§c" + (result.message() != null ? result.message() : result.failure().name()));
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("grid")) {
            handleGrid(sender, label, args);
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("lobby")) {
            sender.sendMessage("§7Usage: /" + label + " lobby <tp|pos1|pos2|spawn|capture|info>");
            sender.sendMessage("§7Usage: /" + label + " start <gameId>");
            sender.sendMessage("§7Usage: /" + label + " grid <reload|show <gameId>>");
            sender.sendMessage("§7Astuce : /menu propose un menu graphique équivalent pour la salle d'attente.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande reservee aux joueurs (necessite une position).");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "tp" -> lobbyCaptureService.teleportToLobby(player);
            case "pos1" -> lobbyCaptureService.setCorner1(player);
            case "pos2" -> lobbyCaptureService.setCorner2(player);
            case "spawn" -> lobbyCaptureService.setSpawn(player);
            case "capture" -> lobbyCaptureService.capture(player);
            case "info" -> lobbyCaptureService.sendInfo(player);
            default -> player.sendMessage("§7Usage: /" + label + " lobby <tp|pos1|pos2|spawn|capture|info>");
        }
        return true;
    }

    private void handleGrid(CommandSender sender, String label, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
            objectiveLibrary.reload();
            sender.sendMessage("§a" + objectiveLibrary.size() + " objectif(s) rechargé(s) depuis objectives.yml.");
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("show")) {
            BingoGame game = gameManager.getGame(args[2]);
            if (game == null) {
                sender.sendMessage("§cAucune partie en cours '" + args[2] + "'.");
                return;
            }
            BingoGrid grid = game.getGrid();
            if (grid == null) {
                sender.sendMessage("§cCette partie n'a pas (encore) de grille générée.");
                return;
            }
            sender.sendMessage("§7Grille " + grid.getSize() + "x" + grid.getSize() + " de la partie '" + args[2] + "' :");
            int index = 0;
            for (GridCell cell : grid.getCells()) {
                index++;
                var objective = cell.getObjective();
                sender.sendMessage("§7 " + index + ". §f" + objective.material() + " §7x" + objective.quantity()
                        + " §8[" + objective.difficulty() + "]"
                        + (objective.hasCondition() ? " §7(" + objective.condition() + ")" : ""));
            }
            return;
        }
        sender.sendMessage("§7Usage: /" + label + " grid <reload|show <gameId>>");
    }
}
