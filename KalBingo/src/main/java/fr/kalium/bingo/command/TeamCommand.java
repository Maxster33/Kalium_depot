package fr.kalium.bingo.command;

import fr.kalium.bingo.game.BingoParty;
import fr.kalium.bingo.game.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Assignation d'equipe dans la salle d'attente Bingo - RESERVEE A L'HOTE (0.1.9, demande
 * explicite de l'utilisateur : "le créateur de la partie doit assigner les équipes aux joueurs
 * lui même"). Remplace la version precedente ou chaque joueur choisissait sa propre equipe
 * (manuel ou aleatoire) : ce choix n'est plus laisse aux joueurs eux-memes, seul l'hote peut
 * desormais assigner une equipe, via le menu (PartyMenu, objet nether star - clic pour faire
 * passer un joueur a l'equipe suivante) ou cette commande (assignation directe a une equipe
 * precise). Validation partagee avec PartyMenu (BingoParty.trySetTeam), pas de logique dupliquee.
 */
public final class TeamCommand implements CommandExecutor {

    private final PartyManager parties;

    public TeamCommand(PartyManager parties) {
        this.parties = parties;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Réservé aux joueurs.");
            return true;
        }
        BingoParty party = parties.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cVous n'êtes dans aucune partie Bingo en attente.");
            return true;
        }
        if (!party.isHost(player.getUniqueId())) {
            player.sendMessage("§cSeul l'hôte de la partie peut assigner les équipes (utilisez le menu de la partie).");
            return true;
        }
        if (args.length != 2) {
            player.sendMessage("§7Usage: /" + label + " <joueur> <1-" + party.getTeamCount() + ">");
            return true;
        }
        UUID target = resolvePlayer(args[0]);
        if (target == null || !party.getExpectedRoster().contains(target)) {
            player.sendMessage("§cJoueur introuvable dans cette partie.");
            return true;
        }
        int teamNumber;
        try {
            teamNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§7Usage: /" + label + " <joueur> <1-" + party.getTeamCount() + ">");
            return true;
        }

        BingoParty.TeamJoinResult result = party.trySetTeam(target, teamNumber);
        String targetName = nameOf(target);
        switch (result) {
            case OK -> player.sendMessage("§a" + targetName + " → équipe " + teamNumber + ".");
            case TEAM_FULL -> player.sendMessage("§cCette équipe est déjà complète (" + party.getTeamSize() + " max).");
            case INVALID_TEAM -> player.sendMessage("§cÉquipe invalide (1 à " + party.getTeamCount() + ").");
        }
        return true;
    }

    private UUID resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    private String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "?" : name;
    }
}
