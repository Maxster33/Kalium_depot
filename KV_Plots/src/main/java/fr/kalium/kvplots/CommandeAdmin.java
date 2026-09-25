package fr.kalium.kvplots;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.kalium.kvplots.api.KanvasPlots.Refus;

/**
 * /kvadmin generer [confirmer] : génère la grille de plots (tuile de référence recopiée sur toute la zone).
 * /kvadmin reset|supprimer <numéro> [confirmer] : remet à zéro / supprime n'importe quel plot, même validé.
 */
final class CommandeAdmin implements CommandExecutor {

    private static final long DELAI_CONFIRMATION_MS = 60_000L;

    private final KVPlots plugin;
    private long demande;

    CommandeAdmin(KVPlots plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command command, String label, String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("supprimer"))) {
            travaux(s, label, args);
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("generer")) {
            s.sendMessage("§e/" + label + " generer §7- générer la grille de plots (confirmation demandée)");
            s.sendMessage("§e/" + label + " reset <numéro> §7- remettre un plot à zéro (même validé)");
            s.sendMessage("§e/" + label + " supprimer <numéro> §7- effacer un plot et libérer la place (même validé)");
            return true;
        }
        if (plugin.tuile() == null) {
            s.sendMessage("§cModèle de terrain absent (modele-tuile.yml) : voir la console.");
            return true;
        }
        if (plugin.generationEnCours()) {
            s.sendMessage("§cUne génération est déjà en cours (avancement dans la console).");
            return true;
        }
        Grille g = plugin.grille();
        int[] zone = plugin.zoneGrille();
        boolean confirme = args.length > 1 && args[1].equalsIgnoreCase("confirmer")
                && System.currentTimeMillis() - demande < DELAI_CONFIRMATION_MS;
        if (!confirme) {
            demande = System.currentTimeMillis();
            s.sendMessage("§6Génération de la grille dans le monde " + plugin.monde().getName() + " :");
            s.sendMessage("§7Plots : colonnes " + g.colonneMin + " à " + g.colonneMax + ", lignes " + g.ligneMin + " à "
                    + g.ligneMax + " (" + (g.colonneMax - g.colonneMin + 1) * (g.ligneMax - g.ligneMin + 1) + " plots moyens).");
            s.sendMessage("§7Zone : x " + zone[0] + " à " + zone[2] + ", z " + zone[1] + " à " + zone[3]
                    + " (routes extérieures comprises).");
            s.sendMessage("§cTout ce qui se trouve dans cette zone sera remplacé (sauf les plots réservés) !");
            s.sendMessage("§eConfirmer dans la minute : /" + label + " generer confirmer");
            return true;
        }
        demande = 0;
        s.sendMessage("§aGénération lancée (avancement dans la console).");
        plugin.generer(() -> s.sendMessage("§aGénération de la grille terminée."));
        return true;
    }

    private void travaux(CommandSender s, String label, String[] args) {
        if (!(s instanceof Player joueur)) {
            s.sendMessage("Commande réservée aux joueurs (opérateurs).");
            return;
        }
        String action = args[0].toLowerCase();
        Plot p = null;
        try {
            if (args.length > 1) p = plugin.plots().parId(Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            p = null;
        }
        if (p == null) {
            s.sendMessage("§cNuméro de plot inconnu. /" + label + " " + action + " <numéro>");
            return;
        }
        boolean suppression = action.equals("supprimer");
        try {
            boolean confirme = args.length > 2 && args[2].equalsIgnoreCase("confirmer")
                    && plugin.confirmations().confirmer(joueur.getUniqueId(), "admin-" + action, p.id);
            if (!confirme) {
                plugin.verifierTravaux(joueur, p, suppression ? "supprimer" : "remettre à zéro",
                        suppression ? "supprimé" : "remis à zéro");
                plugin.confirmations().demander(joueur.getUniqueId(), "admin-" + action, p.id);
                s.sendMessage("§c" + (suppression ? "Supprimer" : "Remettre à zéro") + " le plot n°" + p.id + " de "
                        + KVPlots.nom(p.createur) + (p.etat == Plot.Etat.VALIDE ? " (validé)" : "") + " ?");
                s.sendMessage("§eConfirmer dans la minute : /" + label + " " + action + " " + p.id + " confirmer");
                return;
            }
            if (suppression) {
                plugin.supprimer(joueur, p);
            } else {
                plugin.remettreAZero(joueur, p);
            }
            s.sendMessage("§eTravaux lancés sur le plot n°" + p.id + ".");
        } catch (Refus r) {
            s.sendMessage("§c" + r.getMessage());
        }
    }
}
