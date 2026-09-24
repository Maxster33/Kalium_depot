package fr.kalium.bingo.command;

import fr.kalium.bingo.gui.LobbyMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /menu : ouvre le menu graphique de la salle d'attente. Reserve aux operateurs (permission
 * bingo.admin, la meme que /bingoadmin - demande explicite de l'utilisateur : "uniquement
 * pour les operateurs"). Double verification ici (en plus du "permission:" de plugin.yml)
 * pour un message d'erreur en francais coherent avec le reste du plugin plutot que le
 * message par defaut de Bukkit.
 */
public class MenuCommand implements CommandExecutor {

    private final LobbyMenu lobbyMenu;

    public MenuCommand(LobbyMenu lobbyMenu) {
        this.lobbyMenu = lobbyMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande reservee aux joueurs.");
            return true;
        }
        if (!player.hasPermission("bingo.admin")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser ce menu.");
            return true;
        }
        lobbyMenu.open(player);
        return true;
    }
}
