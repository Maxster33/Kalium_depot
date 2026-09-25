package fr.kalium.kvplots;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /plot reserver|tp|liste|info|editeur (les menus viendront avec KV_Menu). */
final class CommandePlot implements TabExecutor {

    private final KVPlots plugin;

    CommandePlot(KVPlots plugin) {
        this.plugin = plugin;
    }

    private static String nom(UUID u) {
        String n = Bukkit.getOfflinePlayer(u).getName();
        return n == null ? "?" : n;
    }

    private void aide(CommandSender s, String label) {
        s.sendMessage("§6Plots de Kanvas :");
        s.sendMessage("§e/" + label + " reserver <moyen|grand> §7- le plot libre où tu te tiens, sinon le plus proche du centre");
        s.sendMessage("§e/" + label + " liste §7- tes plots (créateur ou éditeur)");
        s.sendMessage("§e/" + label + " tp [numéro] §7- te téléporter à l'un de tes plots");
        s.sendMessage("§e/" + label + " info §7- le plot où tu te tiens");
        s.sendMessage("§e/" + label + " editeur <ajouter|retirer> <pseudo> §7- sur ton plot");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (args.length == 0) {
            aide(joueur, label);
            return true;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "reserver", "réserver" -> reserver(joueur, args);
                case "liste" -> liste(joueur);
                case "tp" -> tp(joueur, args);
                case "info" -> info(joueur);
                case "editeur", "éditeur" -> editeur(joueur, args);
                default -> aide(joueur, label);
            }
        } catch (KVPlots.Refus refus) {
            joueur.sendMessage("§c" + refus.getMessage());
        }
        return true;
    }

    private void reserver(Player joueur, String[] args) throws KVPlots.Refus {
        Taille taille = args.length > 1 ? Taille.depuis(args[1]) : null;
        if (taille == null) throw new KVPlots.Refus("Précise la taille : /plot reserver <moyen|grand>");
        Plot p = plugin.reserver(joueur, taille);
        joueur.sendMessage("§aPlot " + taille.nom + " n°" + p.id + " réservé !");
    }

    private void liste(Player joueur) {
        List<Plot> liste = plugin.plots().duJoueur(joueur.getUniqueId());
        if (liste.isEmpty()) {
            joueur.sendMessage("§7Tu n'as aucun plot. §e/plot reserver <moyen|grand>");
            return;
        }
        joueur.sendMessage("§6Tes plots :");
        for (int i = 0; i < liste.size(); i++) {
            Plot p = liste.get(i);
            String role = p.createur.equals(joueur.getUniqueId()) ? "créateur" : "éditeur, plot de " + nom(p.createur);
            joueur.sendMessage("§e" + (i + 1) + ". §fn°" + p.id + " §7(" + p.taille.nom + ", " + role + ")");
        }
    }

    private void tp(Player joueur, String[] args) throws KVPlots.Refus {
        List<Plot> liste = plugin.plots().duJoueur(joueur.getUniqueId());
        if (liste.isEmpty()) throw new KVPlots.Refus("Tu n'as aucun plot.");
        int n = 1;
        if (args.length > 1) {
            try {
                n = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                n = 0;
            }
        }
        if (n < 1 || n > liste.size()) throw new KVPlots.Refus("Numéro entre 1 et " + liste.size() + " (voir /plot liste).");
        plugin.teleporter(joueur, liste.get(n - 1));
    }

    private Plot plotIci(Player joueur) throws KVPlots.Refus {
        if (!joueur.getWorld().equals(plugin.monde())) throw new KVPlots.Refus("Tu n'es pas dans le monde des plots.");
        Plot p = plugin.plotEn(joueur.getLocation().getBlockX(), joueur.getLocation().getBlockZ());
        if (p == null) throw new KVPlots.Refus("Tu n'es sur aucun plot réservé.");
        return p;
    }

    private void info(Player joueur) throws KVPlots.Refus {
        Plot p = plotIci(joueur);
        joueur.sendMessage("§6Plot n°" + p.id + " §7(" + p.taille.nom + ", "
                + (p.etat == Plot.Etat.VALIDE ? "validé" : "en travaux") + ")");
        joueur.sendMessage("§eCréateur : §f" + nom(p.createur));
        joueur.sendMessage("§eÉditeurs : §f" + (p.editeurs.isEmpty() ? "aucun"
                : p.editeurs.stream().map(CommandePlot::nom).collect(Collectors.joining(", "))));
    }

    private void editeur(Player joueur, String[] args) throws KVPlots.Refus {
        if (args.length < 3) throw new KVPlots.Refus("/plot editeur <ajouter|retirer> <pseudo>");
        Plot p = plotIci(joueur);
        if (!p.createur.equals(joueur.getUniqueId())) {
            throw new KVPlots.Refus("Seul le créateur du plot gère ses éditeurs.");
        }
        OfflinePlayer cible = Bukkit.getPlayerExact(args[2]);
        if (cible == null) cible = Bukkit.getOfflinePlayerIfCached(args[2]);
        if (cible == null) throw new KVPlots.Refus("Joueur inconnu : " + args[2] + " (il doit s'être déjà connecté).");
        UUID u = cible.getUniqueId();
        if (u.equals(p.createur)) throw new KVPlots.Refus("Tu es déjà le créateur de ce plot.");
        switch (args[1].toLowerCase()) {
            case "ajouter" -> {
                if (!p.editeurs.add(u)) throw new KVPlots.Refus(nom(u) + " est déjà éditeur de ce plot.");
                p.historiqueEditeurs.add(u);
                joueur.sendMessage("§a" + nom(u) + " est maintenant éditeur du plot n°" + p.id + ".");
            }
            case "retirer" -> {
                if (!p.editeurs.remove(u)) throw new KVPlots.Refus(nom(u) + " n'est pas éditeur de ce plot.");
                joueur.sendMessage("§a" + nom(u) + " n'est plus éditeur du plot n°" + p.id + ".");
            }
            default -> throw new KVPlots.Refus("/plot editeur <ajouter|retirer> <pseudo>");
        }
        plugin.plots().sauver();
        plugin.majRegion(p);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String debut = args[args.length - 1].toLowerCase();
        List<String> choix = switch (args.length) {
            case 1 -> List.of("reserver", "liste", "tp", "info", "editeur");
            case 2 -> switch (args[0].toLowerCase()) {
                case "reserver", "réserver" -> List.of("moyen", "grand");
                case "editeur", "éditeur" -> List.of("ajouter", "retirer");
                default -> List.of();
            };
            case 3 -> args[0].toLowerCase().startsWith("edit") || args[0].toLowerCase().startsWith("édit")
                    ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
            default -> List.of();
        };
        return choix.stream().filter(c -> c.toLowerCase().startsWith(debut)).toList();
    }
}
