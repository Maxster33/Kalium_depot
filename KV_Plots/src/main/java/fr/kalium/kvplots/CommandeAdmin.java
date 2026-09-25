package fr.kalium.kvplots;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /kvadmin generer [confirmer] : génère la grille de plots (tuile de référence recopiée sur toute la zone). */
final class CommandeAdmin implements CommandExecutor {

    private static final long DELAI_CONFIRMATION_MS = 60_000L;

    private final KVPlots plugin;
    private long demande;

    CommandeAdmin(KVPlots plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("generer")) {
            s.sendMessage("§e/" + label + " generer §7- générer la grille de plots (confirmation demandée)");
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
}
