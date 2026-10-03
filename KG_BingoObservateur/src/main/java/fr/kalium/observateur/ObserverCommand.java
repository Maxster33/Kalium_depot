package fr.kalium.observateur;

import fr.kalium.menu.api.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /observer [pseudo|quitter] - voir KGBingoObservateur. */
final class ObserverCommand implements TabExecutor {

    private final ObservationService service;
    private final ObservationMenu menu;
    private final ObserverItem item;
    private final Lang lang;

    ObserverCommand(ObservationService service, ObservationMenu menu, ObserverItem item, Lang lang) {
        this.service = service;
        this.menu = menu;
        this.item = item;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Réservé aux joueurs.");
            return true;
        }
        if (args.length == 0) {
            if (service.isObserving(player)) {
                menu.openList(player); // en spectateur, la longue-vue ne peut pas servir
                return true;
            }
            if (item.has(player)) {
                player.sendMessage(lang.c("msg.deja-objet", "<gray>Vous avez déjà la longue-vue : clic droit pour choisir un joueur."));
            } else {
                player.getInventory().addItem(item.create()).values()
                        .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
                player.sendMessage(lang.c("msg.objet", "<green>Longue-vue « Observer un joueur » reçue : clic droit pour choisir un joueur."));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("quitter")) {
            if (service.isObserving(player)) {
                service.stop(player, lang.c("msg.fin", "<green>Observation terminée : retour à votre position."));
            } else {
                player.sendMessage(lang.c("msg.personne", "<gray>Vous n'observez personne."));
            }
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(lang.c("msg.introuvable", "<red>Joueur introuvable : <name>.", "name", args[0]));
            return true;
        }
        service.observe(player, target, false);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            if ("quitter".startsWith(start)) {
                result.add("quitter");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(sender) && online.getName().toLowerCase(Locale.ROOT).startsWith(start)) {
                    result.add(online.getName());
                }
            }
        }
        return result;
    }
}
