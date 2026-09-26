package fr.kalium.kvplots;

import fr.kalium.kvplots.api.KanvasPlots.Refus;
import fr.kalium.kvplots.api.Taille;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /plot reserver|tp|liste|info|editeur|reset|supprimer (les mêmes actions existent dans le menu de KV_Menu). */
final class CommandePlot implements TabExecutor {

    private final KVPlots plugin;

    CommandePlot(KVPlots plugin) {
        this.plugin = plugin;
    }

    private static String nom(UUID u) {
        return KVPlots.nom(u);
    }

    private void aide(CommandSender s, String label) {
        s.sendMessage("§6Plots de Kanvas :");
        s.sendMessage("§e/" + label + " reserver <moyen|grand> §7- le plot libre où tu te tiens, sinon le plus proche du centre");
        s.sendMessage("§e/" + label + " liste §7- tes plots (créateur ou éditeur)");
        s.sendMessage("§e/" + label + " tp [numéro] §7- te téléporter à l'un de tes plots");
        s.sendMessage("§e/" + label + " info §7- le plot où tu te tiens");
        s.sendMessage("§e/" + label + " editeur <ajouter|retirer> <pseudo> §7- sur ton plot");
        s.sendMessage("§e/" + label + " valider §7- figer ton plot fini : il pourra être noté, sa place se libère");
        s.sendMessage("§e/" + label + " rouvrir §7- rouvrir ton plot validé (il faut une place libre)");
        s.sendMessage("§e/" + label + " reset §7- remettre ton plot à zéro (il reste à toi)");
        s.sendMessage("§e/" + label + " supprimer §7- effacer ton plot et libérer la place");
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
                case "valider" -> etat(joueur, args, true);
                case "rouvrir" -> etat(joueur, args, false);
                case "reset" -> travaux(joueur, args, false);
                case "supprimer" -> travaux(joueur, args, true);
                default -> aide(joueur, label);
            }
        } catch (Refus refus) {
            joueur.sendMessage("§c" + refus.getMessage());
        }
        return true;
    }

    private void reserver(Player joueur, String[] args) throws Refus {
        Taille taille = args.length > 1 ? Taille.depuis(args[1]) : null;
        if (taille == null) throw new Refus("Précise la taille : /plot reserver <moyen|grand>");
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

    private void tp(Player joueur, String[] args) throws Refus {
        List<Plot> liste = plugin.plots().duJoueur(joueur.getUniqueId());
        if (liste.isEmpty()) throw new Refus("Tu n'as aucun plot.");
        int n = 1;
        if (args.length > 1) {
            try {
                n = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                n = 0;
            }
        }
        if (n < 1 || n > liste.size()) throw new Refus("Numéro entre 1 et " + liste.size() + " (voir /plot liste).");
        plugin.teleporter(joueur, liste.get(n - 1));
    }

    private Plot plotIci(Player joueur) throws Refus {
        if (!joueur.getWorld().equals(plugin.monde())) throw new Refus("Tu n'es pas dans le monde des plots.");
        Plot p = plugin.plotEn(joueur.getLocation().getBlockX(), joueur.getLocation().getBlockZ());
        if (p == null) throw new Refus("Tu n'es sur aucun plot réservé.");
        return p;
    }

    /** /plot reset|supprimer [confirmer] sur le plot où l'on se tient (créateur ; staff : n'importe quel plot). */
    private void travaux(Player joueur, String[] args, boolean suppression) throws Refus {
        Plot p = plotIci(joueur);
        String action = suppression ? "supprimer" : "reset";
        boolean confirme = args.length > 1 && args[1].equalsIgnoreCase("confirmer")
                && plugin.confirmations().confirmer(joueur.getUniqueId(), action, p.id);
        if (suppression) {
            plugin.verifierTravaux(joueur, p, "supprimer", "supprimé");
        } else {
            plugin.verifierTravaux(joueur, p, "remettre à zéro", "remis à zéro");
        }
        if (!confirme) {
            plugin.confirmations().demander(joueur.getUniqueId(), action, p.id);
            joueur.sendMessage(suppression
                    ? "§cSupprimer le plot n°" + p.id + " : tout ce qui y est construit sera effacé et la place libérée."
                    : "§cRemettre à zéro le plot n°" + p.id + " : tout ce qui y est construit sera effacé (le plot reste à toi).");
            joueur.sendMessage("§eConfirmer dans la minute : /plot " + action + " confirmer");
            return;
        }
        if (suppression) {
            plugin.supprimer(joueur, p);
            joueur.sendMessage("§eSuppression du plot n°" + p.id + " en cours...");
        } else {
            plugin.remettreAZero(joueur, p);
            joueur.sendMessage("§eRemise à zéro du plot n°" + p.id + " en cours...");
        }
    }

    /** /plot valider|rouvrir [confirmer] sur le plot où l'on se tient (créateur). */
    private void etat(Player joueur, String[] args, boolean valider) throws Refus {
        Plot p = plotIci(joueur);
        String action = valider ? "valider" : "rouvrir";
        boolean confirme = args.length > 1 && args[1].equalsIgnoreCase("confirmer")
                && plugin.confirmations().confirmer(joueur.getUniqueId(), action, p.id);
        if (!confirme) {
            if (!p.createur.equals(joueur.getUniqueId())) throw new Refus("Seul le créateur du plot peut le " + action + ".");
            plugin.confirmations().demander(joueur.getUniqueId(), action, p.id);
            joueur.sendMessage(valider
                    ? "§eValider le plot n°" + p.id + " : il sera figé (plus aucune modification) et pourra être noté ; sa place se libère."
                    : "§eRouvrir le plot n°" + p.id + " : il reprend une place " + p.taille.nom + " ; ses votes sont gardés, on pourra revoter une fois revalidé.");
            joueur.sendMessage("§eConfirmer dans la minute : /plot " + action + " confirmer");
            return;
        }
        if (valider) {
            plugin.valider(joueur, p);
            joueur.sendMessage("§aPlot n°" + p.id + " validé : il peut maintenant être noté !");
        } else {
            plugin.rouvrir(joueur, p);
            joueur.sendMessage("§aPlot n°" + p.id + " rouvert : tu peux de nouveau y construire.");
        }
    }

    private void info(Player joueur) throws Refus {
        Plot p = plotIci(joueur);
        joueur.sendMessage("§6Plot n°" + p.id + " §7(" + p.taille.nom + ", "
                + (p.etat == Plot.Etat.VALIDE ? "validé" : "en travaux") + ")");
        joueur.sendMessage("§eCréateur : §f" + nom(p.createur));
        joueur.sendMessage("§ePoints : §f" + p.points() + " §7(" + p.votes.size() + " vote" + (p.votes.size() > 1 ? "s" : "")
                + (p.votes.isEmpty() ? "" : ", moyenne " + String.format(java.util.Locale.FRANCE, "%.1f", p.moyenne()) + "/5") + ")");
        joueur.sendMessage("§eÉditeurs : §f" + (p.editeurs.isEmpty() ? "aucun"
                : p.editeurs.stream().map(CommandePlot::nom).collect(Collectors.joining(", "))));
    }

    private void editeur(Player joueur, String[] args) throws Refus {
        if (args.length < 3) throw new Refus("/plot editeur <ajouter|retirer> <pseudo>");
        Plot p = plotIci(joueur);
        OfflinePlayer cible = Bukkit.getPlayerExact(args[2]);
        if (cible == null) cible = Bukkit.getOfflinePlayerIfCached(args[2]);
        if (cible == null) throw new Refus("Joueur inconnu : " + args[2] + " (il doit s'être déjà connecté).");
        UUID u = cible.getUniqueId();
        switch (args[1].toLowerCase()) {
            case "ajouter" -> {
                plugin.ajouterEditeur(joueur, p, u);
                joueur.sendMessage("§a" + nom(u) + " est maintenant éditeur du plot n°" + p.id + ".");
            }
            case "retirer" -> {
                plugin.retirerEditeur(joueur, p, u);
                joueur.sendMessage("§a" + nom(u) + " n'est plus éditeur du plot n°" + p.id + ".");
            }
            default -> throw new Refus("/plot editeur <ajouter|retirer> <pseudo>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String debut = args[args.length - 1].toLowerCase();
        List<String> choix = switch (args.length) {
            case 1 -> List.of("reserver", "liste", "tp", "info", "editeur", "valider", "rouvrir", "reset", "supprimer");
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
