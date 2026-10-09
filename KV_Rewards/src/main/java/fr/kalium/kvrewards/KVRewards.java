package fr.kalium.kvrewards;

import fr.kalium.kvplots.api.ConcoursTermineEvent;
import fr.kalium.kvplots.api.NoteRecueEvent;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KV_Rewards (cahier des charges : catégorie 4 « Récompenses », LeKiwi06, validé le 30/09/2026) : paliers, tops et
 * prestige des notes reçues sur Kanvas (KV_Plots 1.5.0), récompenses des concours de build ; tirées dans des tables de
 * butin (Butin) et envoyées vers Event (Envois -> KaliumRelay -> KS_RewardsGUI).
 *
 * - /kvrewards : progression du joueur ; /kvrewards admin : tables de butin (opérateurs, kvrewards.admin). Aussi ouvert
 *   par le bouton « Récompenses » de KLM_Menu.
 */
public final class KVRewards extends JavaPlugin implements Listener {

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
        getCommand("kvrewards").setExecutor(this);
        // Bouton « Récompenses » du comparateur « Informations » de KLM_Menu 2.6.0.
        getServer().getServicesManager().register(fr.kalium.menu.api.Recompenses.class, new fr.kalium.menu.api.Recompenses() {
            public org.bukkit.plugin.Plugin owner() {
                return KVRewards.this;
            }

            public void ouvrir(Player joueur) {
                KVRewards.this.ouvrir(joueur);
            }
        }, this, org.bukkit.plugin.ServicePriority.Normal);
        if (getConfig().getString("relay-token", "").isBlank()) {
            getLogger().warning("relay-token vide dans config.yml : les récompenses attendent dans envois.yml.");
        }
        // Une fois le serveur démarré (KV_Plots chargé) : point de départ, puis fin de mois (chaque minute) et tops
        // permanents (toutes les 5 min).
        getServer().getScheduler().runTask(this, () -> {
            moteur.initialiserSiBesoin();
            getServer().getScheduler().runTaskTimer(this, moteur::verifierMois, 20L * 30, 20L * 60);
            getServer().getScheduler().runTaskTimer(this, moteur::verifierTopsPermanents, 20L * 60, 20L * 60 * 5);
        });
        getServer().getScheduler().runTaskTimerAsynchronously(this, envois::vider, 20L * 15, 20L * 15);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
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
        return joueur.hasPermission("kvrewards.admin");
    }

    @EventHandler
    public void onNote(NoteRecueEvent event) {
        moteur.noteRecue(event.batisseurs());
    }

    @EventHandler
    public void onConcours(ConcoursTermineEvent event) {
        moteur.concoursTermine(event.concours().theme(), event.classement());
    }

    /** Interface Récompenses (bouton de KLM_Menu, /kvrewards) : progression ; bouton admin pour les opérateurs. */
    public void ouvrir(Player joueur) {
        menuJoueur.ouvrir(joueur);
    }

    void ouvrirAdmin(Player joueur) {
        menuAdmin.ouvrir(joueur);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("test") && sender.hasPermission("kvrewards.admin")) {
            tester(sender, args);
            return true;
        }
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

    /**
     * 1.1.0 (LeKiwi06, 09/10/2026 : « donne moi 10 récompenses de chaque dans mon reward sur kixster pour tester ») :
     * /kvrewards test <joueur> <période> <niveau | tout> [nombre] [boîte] (admins et console) tire des récompenses dans les
     * tables de butin et les envoie comme de vraies récompenses, dans la boîte de config.yml ou dans celle indiquée
     * (ex. kixster). Aucun palier ni top n'est marqué comme atteint.
     */
    private void tester(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Usage : /kvrewards test <joueur> <mois | permanent | concours> <niveau | tout> [nombre] [boîte]");
            return;
        }
        OfflinePlayer cible = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (cible == null) {
            sender.sendMessage("Joueur inconnu de ce serveur : " + args[1]);
            return;
        }
        String periode = args[2].toLowerCase();
        if (!Butin.PERIODES.contains(periode)) {
            sender.sendMessage("Période inconnue : " + args[2] + " (mois | permanent | concours)");
            return;
        }
        Map<String, String> noms = Butin.niveauxDe(periode);
        List<String> niveaux = args[3].equalsIgnoreCase("tout") ? new ArrayList<>(noms.keySet())
                : List.of(args[3].toLowerCase());
        if (!noms.keySet().containsAll(niveaux)) {
            sender.sendMessage("Niveau inconnu : " + args[3] + ". Niveaux : " + String.join(", ", noms.keySet()) + ", tout");
            return;
        }
        int nombre = 1;
        if (args.length > 4) {
            try {
                nombre = Math.max(1, Math.min(100, Integer.parseInt(args[4])));
            } catch (NumberFormatException e) {
                sender.sendMessage("Nombre invalide : " + args[4] + " (1 à 100)");
                return;
            }
        }
        String boite = args.length > 5 ? args[5].toLowerCase() : getConfig().getString("boite", "event");
        if (!boite.matches("[a-z0-9_-]{1,32}")) {
            sender.sendMessage("Boîte invalide : " + args[5]);
            return;
        }
        String nom = cible.getName() == null ? args[1] : cible.getName();
        int envoyees = 0;
        for (String niveau : niveaux) {
            for (int i = 1; i <= nombre; i++) {
                List<Map<String, Object>> contenu = butin.tirer(periode, niveau, 1);
                if (!contenu.isEmpty()) {
                    envoyees++;
                }
                envois.envoyer(cible.getUniqueId(), nom, "Essai " + i + " - " + noms.get(niveau) + " ("
                        + Moteur.nomPeriode(periode) + ")", contenu, boite);
            }
        }
        sender.sendMessage(envoyees + " récompense(s) d'essai pour " + nom + ", boîte « " + boite + " » (envoi dans les "
                + "15 secondes ; niveaux vides ignorés).");
    }
}
