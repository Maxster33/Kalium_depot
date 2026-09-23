package fr.kalium.games.command;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.game.PvpInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /kalgames (alias /kg) et /hub. */
public final class KalGamesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_SUBS = List.of("jeux", "hub", "rejoindre", "quitter", "lobby", "lancer", "vote");
    private static final List<String> ADMIN_SUBS = List.of("admin", "reload", "fermer");

    private final KalGames plugin;

    public KalGamesCommand(KalGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hub")) {
            if (sender instanceof Player player) {
                plugin.hub().sendToHub(player);
            } else {
                sender.sendMessage("Cette commande est réservée aux joueurs.");
            }
            return true;
        }

        String sub = args.length == 0 ? "jeux" : args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            if (!sender.hasPermission("kalgames.admin")) {
                plugin.tell(sender, "cmd.no-permission", "<red>Permission insuffisante.");
                return true;
            }
            plugin.reloadAll();
            plugin.tell(sender, "admin.reloaded", "<green>Configuration rechargée.");
            return true;
        }
        if (sub.equals("fermer")) {
            if (!sender.hasPermission("kalgames.admin")) {
                plugin.tell(sender, "cmd.no-permission", "<red>Permission insuffisante.");
                return true;
            }
            if (args.length < 2) {
                plugin.tell(sender, "cmd.close-usage", "<gray>Usage : /<label> fermer <id de la partie>", "label", label);
                return true;
            }
            GameInstance target = plugin.instances().all().stream().filter(g -> g.id().equalsIgnoreCase(args[1])).findFirst().orElse(null);
            if (target == null) {
                plugin.tell(sender, "cmd.close-unknown", "<red>Partie introuvable.");
            } else {
                plugin.instances().close(target, plugin.t("game.closed-by-admin", "<yellow>La partie a été fermée par un modérateur."));
                plugin.tell(sender, "cmd.close-done", "<green>Partie fermée.");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        switch (sub) {
            case "jeux", "menu" -> plugin.menus().openMenuFor(player);
            case "hub", "quitter" -> plugin.hub().sendToHub(player);
            case "lobby" -> plugin.connectLobby(player);
            case "admin" -> {
                if (plugin.isAdmin(player)) {
                    plugin.admin().openHome(player);
                } else {
                    plugin.tell(player, "cmd.no-permission", "<red>Permission insuffisante.");
                }
            }
            case "rejoindre" -> {
                if (args.length < 2) {
                    plugin.tell(player, "cmd.join-usage", "<gray>Usage : /<label> rejoindre <code>", "label", label);
                    return true;
                }
                Component error = plugin.instances().joinPrivate(player, args[1]);
                if (error != null) {
                    player.sendMessage(plugin.prefix().append(error));
                }
            }
            case "lancer" -> {
                GameInstance game = plugin.instances().of(player);
                if (game == null) {
                    plugin.tell(player, "cmd.not-in-game", "<red>Vous n'êtes dans aucune partie.");
                    return true;
                }
                Component error = game.requestStart(player);
                if (error != null) {
                    player.sendMessage(plugin.prefix().append(error));
                }
            }
            case "vote" -> {
                GameInstance game = plugin.instances().of(player);
                if (game instanceof PvpInstance pvp) {
                    plugin.menus().openVote(player, pvp);
                } else {
                    plugin.tell(player, "cmd.no-vote", "<red>Aucun vote de kit en cours.");
                }
            }
            default -> plugin.tell(player, "cmd.usage", "<gray>Usage : /<label> <jeux|hub|rejoindre|quitter|lobby|lancer|vote>", "label", label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("kalgames") || args.length != 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(PLAYER_SUBS);
        if (sender.hasPermission("kalgames.admin")) {
            options.addAll(ADMIN_SUBS);
        }
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                matches.add(option);
            }
        }
        return matches;
    }
}
