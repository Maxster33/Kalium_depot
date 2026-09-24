package fr.kalium.games.bingo;

import fr.kalium.games.KalGames;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande minimale de creation/jointure de parties Bingo.
 *
 * Commande texte gardee en parallele du menu "Bingo" du hub (PlayerMenus.openBingoMenu, demande
 * explicite de l'utilisateur : "tout en laissant les commandes actuelles fonctionnelles"). Meme
 * logique (BingoPartyManager) ; /bingo create utilise les valeurs par defaut de config.yml
 * (equipes, duree), le menu laisse l'hote les choisir.
 */
public final class BingoCommand implements CommandExecutor {

    private final KalGames plugin;
    private final BingoPartyManager parties;

    public BingoCommand(KalGames plugin, BingoPartyManager parties) {
        this.plugin = plugin;
        this.parties = parties;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Réservé aux joueurs.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§7Usage: /" + label + " <create|join <code>>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> {
                BingoParty party = parties.create(player);
                player.sendMessage("§aPartie Bingo créée. Code à partager : §f§l" + party.code());
                parties.transferToBingo(player);
            }
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage("§7Usage: /" + label + " join <code>");
                    return true;
                }
                BingoParty party = parties.join(player, args[1]);
                if (party == null) {
                    player.sendMessage("§cCode invalide, partie pleine ou introuvable.");
                    return true;
                }
                player.sendMessage("§aPartie rejointe, transfert en cours…");
                parties.transferToBingo(player);
            }
            default -> player.sendMessage("§7Usage: /" + label + " <create|join <code>>");
        }
        return true;
    }
}
