package fr.kalium.bingo.command;

import fr.kalium.bingo.game.DrawVoteService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /bingonulle [proposer|oui|non] (0.3.0) : proposer une nulle, ou voter. Les votes se font normalement en
 * cliquant sur [Oui] / [Non] dans le tchat (voir DrawVoteService), la proposition depuis le menu Objectifs.
 */
public final class DrawCommand implements CommandExecutor {

    private final DrawVoteService drawVotes;

    public DrawCommand(DrawVoteService drawVotes) {
        this.drawVotes = drawVotes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande réservée aux joueurs.");
            return true;
        }
        String action = args.length == 0 ? "proposer" : args[0].toLowerCase();
        switch (action) {
            case "oui" -> drawVotes.castBallot(player, true);
            case "non" -> drawVotes.castBallot(player, false);
            case "proposer" -> drawVotes.propose(player);
            default -> player.sendMessage("§cUsage : /" + label + " [proposer|oui|non]");
        }
        return true;
    }
}
