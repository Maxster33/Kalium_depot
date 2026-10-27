package fr.kalium.contrats;

import fr.kalium.economy.api.ContratsEchange;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * KS_Contrats (demande de LeKiwi06 du 27/10/2026) : « /contrat sur Kixster ; il sert à avoir une trace et qu'un admin
 * puisse trancher en cas de non-respect de l'engagement ». Choix validés et limites : JOURNAL.md.
 *
 * - /contrat : menus en coffre (Mes contrats, Créer, Propositions, Terminés) ; /contrat admin : litiges.
 * - Bouton « Contrats » du menu de Kixster (KS_Menu).
 * - Message à la connexion quand un contrat demande une réponse (et messages reçus hors ligne).
 * - Livraisons par /echange (KS_Economy 1.12.0) : service ContratsEchange.
 * - Monde de test : contrats et garanties du « moi de test » (CompteTest), séparés de la survie.
 */
public final class KSContrats extends JavaPlugin implements Listener, TabExecutor {

    private Lang lang;
    private Gui gui;
    private Registre registre;
    private Menus menus;

    @Override
    public void onEnable() {
        lang = new Lang(this);
        gui = new Gui(this, lang);
        registre = new Registre(this);
        menus = new Menus(this, registre);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getServicesManager().register(ContratsEchange.class, registre, this, ServicePriority.Normal);
        getCommand("contrat").setExecutor(this);
        getCommand("contrat").setTabCompleter(this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "contrats", lang.c("bouton.nom", "<gold><bold>Contrats"),
                    lang.c("bouton.description", "<gray>Tes engagements avec les autres joueurs"), 15,
                    p -> menus.accueil(p, 0));
        }
        // Périodes, expirations, bilans sans réponse ; messages en attente d'un joueur qui change de monde.
        getServer().getScheduler().runTaskTimer(this, () -> {
            registre.tic();
            getServer().getOnlinePlayers().forEach(registre::distribuer);
            registre.sauverSiBesoin();
            lang.saveIfNeeded();
        }, 200L, 600L);
        lang.saveIfNeeded();
        getLogger().info(registre.contrats.size() + " contrats chargés.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (registre != null) {
            registre.sauver();
        }
    }

    Lang lang() {
        return lang;
    }

    Gui gui() {
        return gui;
    }

    Menus menus() {
        return menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("admin") && joueur.hasPermission("kscontrats.admin")) {
            menus.admin(joueur, 0, null);
        } else {
            menus.accueil(joueur, 0);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && sender.hasPermission("kscontrats.admin") && "admin".startsWith(args[0].toLowerCase())
                ? List.of("admin") : List.of();
    }

    /** Messages reçus hors ligne, contrats qui attendent une réponse, litiges pour les admins. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!joueur.isOnline()) {
                return;
            }
            registre.distribuer(joueur);
            int n = registre.attention(Registre.compte(joueur));
            if (n > 0) {
                joueur.sendMessage(lang.c("connexion.attention", "<gold>Contrats : <n> demande<s> ta réponse. "
                        + "<yellow><u>/contrat</u>", "n", n, "s", n > 1 ? "nt" : "")
                        .clickEvent(ClickEvent.runCommand("/contrat")));
            }
            int litiges = registre.litiges();
            if (litiges > 0 && joueur.hasPermission("kscontrats.admin")) {
                joueur.sendMessage(lang.c("connexion.litiges", "<light_purple>[Contrats] <n> litige<s> à trancher. "
                        + "<yellow><u>/contrat admin</u>", "n", litiges, "s", litiges > 1 ? "s" : "")
                        .clickEvent(ClickEvent.runCommand("/contrat admin")));
            }
            registre.sauverSiBesoin();
            lang.saveIfNeeded();
        }, 60L);
    }
}
