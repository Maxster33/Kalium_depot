package fr.kalium.rewards;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.scoreboards.api.PeriodeClotureeEvent;
import fr.kalium.scoreboards.api.PointsAjoutesEvent;
import fr.kalium.scoreboards.api.Prestiges;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KG_Rewards (cahier des charges : catégorie 4 « Récompenses », LeKiwi06, validé le 30/09/2026) : paliers, tops et
 * prestige des mini-jeux de kal-games (scores de KG_ScoreBoards, Build Battle compris) ; les récompenses sont tirées dans
 * des tables de butin (Butin) et envoyées vers Event (Envois -> KaliumRelay -> KS_RewardsGUI).
 *
 * - /kgrewards : progression du joueur (paliers, prestige) ; /kgrewards admin : tables de butin (opérateurs,
 *   kgrewards.admin). Aussi ouvert par le bouton « Récompenses » de KLM_Menu (ouvrir).
 */
public final class KGRewards extends JavaPlugin implements Listener {

    private Lang lang;
    private Gui gui;
    private Butin butin;
    private Envois envois;
    private Moteur moteur;
    private MenuJoueur menuJoueur;
    private MenuAdmin menuAdmin;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        butin = new Butin(getDataFolder());
        envois = new Envois(this);
        moteur = new Moteur(this, butin, envois);
        menuAdmin = new MenuAdmin(this, butin);
        menuJoueur = new MenuJoueur(this, moteur);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menuAdmin, this);
        getCommand("kgrewards").setExecutor(this);
        Prestiges.fournisseur(moteur::prestige);
        if (getConfig().getString("relay-token", "").isBlank()) {
            getLogger().warning("relay-token vide dans config.yml : les récompenses attendent dans envois.yml.");
        }
        // Une fois le serveur démarré (KG_ScoreBoards chargé) : point de départ, puis tops permanents toutes les 5 min.
        getServer().getScheduler().runTask(this, () -> {
            moteur.initialiserSiBesoin();
            getServer().getScheduler().runTaskTimer(this, moteur::verifierTopsPermanents, 20L * 60, 20L * 60 * 5);
        });
        getServer().getScheduler().runTaskTimerAsynchronously(this, envois::vider, 20L * 15, 20L * 15);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        Prestiges.fournisseur(null);
        if (moteur != null) {
            moteur.sauver();
        }
    }

    Lang lang() {
        return lang;
    }

    Gui gui() {
        return gui;
    }

    boolean estAdmin(Player joueur) {
        return joueur.hasPermission("kgrewards.admin");
    }

    @EventHandler
    public void onPoints(PointsAjoutesEvent event) {
        moteur.pointsAjoutes(event.minigame(), event.joueur(), event.nom());
    }

    @EventHandler
    public void onPeriode(PeriodeClotureeEvent event) {
        moteur.periodeCloturee(event.type(), event.classements());
    }

    /** Interface Récompenses (bouton de KLM_Menu, /kgrewards) : progression ; bouton admin pour les opérateurs. */
    public void ouvrir(Player joueur) {
        menuJoueur.ouvrir(joueur);
    }

    void ouvrirAdmin(Player joueur) {
        menuAdmin.ouvrir(joueur);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("admin") && estAdmin(joueur)) {
            menuAdmin.ouvrir(joueur);
        } else {
            menuJoueur.ouvrir(joueur);
        }
        return true;
    }
}
