package fr.kalium.rewards;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.scoreboards.api.PeriodeClotureeEvent;
import fr.kalium.scoreboards.api.PointsAjoutesEvent;
import fr.kalium.scoreboards.api.Prestiges;
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
        // Bouton « Récompenses » du comparateur « Informations » de KLM_Menu 2.6.0.
        getServer().getServicesManager().register(fr.kalium.menu.api.Recompenses.class, new fr.kalium.menu.api.Recompenses() {
            public org.bukkit.plugin.Plugin owner() {
                return KGRewards.this;
            }

            public void ouvrir(Player joueur) {
                KGRewards.this.ouvrir(joueur);
            }
        }, this, org.bukkit.plugin.ServicePriority.Normal);
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
        if (args.length > 0 && args[0].equalsIgnoreCase("rattrapage") && sender.hasPermission("kgrewards.admin")) {
            rattraper(sender, args);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("test") && sender.hasPermission("kgrewards.admin")) {
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
     * /kgrewards test <joueur> <période> <niveau | tout> [nombre] [boîte] (admins et console) tire des récompenses dans les
     * tables de butin et les envoie comme de vraies récompenses, dans la boîte de config.yml ou dans celle indiquée
     * (ex. kixster). Aucun palier ni top n'est marqué comme atteint.
     */
    /**
     * 1.3.0 (LeKiwi06, 09/10/2026 : « à l'ouverture chaque joueur gagne d'un coup tous les coffres qu'il aurait dû gagner
     * depuis le début des scores », puis « fais un gros coffre pour rattraper tous les petits ») :
     * /kgrewards rattrapage <boîte> affiche ce que contient plugins/KG_Rewards/rattrapage.yml (un gros coffre par joueur :
     * valeur moyenne, tirages, fréquences des pools ; fichier calculé hors du serveur à partir des scores) ;
     * /kgrewards rattrapage <boîte> confirmer tire et envoie un coffre par joueur, puis range le fichier sous le nom
     * rattrapage-envoye.yml : le rattrapage ne peut donc partir qu'une fois. Aucun palier ni top n'est modifié.
     */
    private void rattraper(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].toLowerCase().matches("[a-z0-9_-]{1,32}")) {
            sender.sendMessage("Usage : /kgrewards rattrapage <boîte> [confirmer]");
            return;
        }
        String boite = args[1].toLowerCase();
        java.io.File fichier = new java.io.File(getDataFolder(), "rattrapage.yml");
        if (!fichier.isFile()) {
            sender.sendMessage("Aucun rattrapage à envoyer (pas de rattrapage.yml, ou déjà envoyé).");
            return;
        }
        org.bukkit.configuration.ConfigurationSection joueurs = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(fichier).getConfigurationSection("joueurs");
        if (joueurs == null || joueurs.getKeys(false).isEmpty()) {
            sender.sendMessage("rattrapage.yml ne contient aucun joueur.");
            return;
        }
        boolean envoyer = args.length > 2 && args[2].equalsIgnoreCase("confirmer");
        int envoyes = 0;
        double total = 0;
        for (String cle : joueurs.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection j = joueurs.getConfigurationSection(cle);
            java.util.UUID uuid;
            try {
                uuid = java.util.UUID.fromString(cle);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("Joueur illisible ignoré : " + cle);
                continue;
            }
            String nom = j.getString("nom", "?");
            double valeur = j.getDouble("valeur");
            int tirages = j.getInt("tirages");
            int coffres = j.getInt("coffres");
            Map<String, Integer> frequences = new java.util.LinkedHashMap<>();
            org.bukkit.configuration.ConfigurationSection f = j.getConfigurationSection("frequences");
            if (f != null) {
                f.getKeys(false).forEach(pool -> frequences.put(pool, f.getInt(pool)));
            }
            total += valeur;
            if (!envoyer) {
                sender.sendMessage(nom + " : 1 gros coffre, valeur moyenne " + Math.round(valeur) + " émeraudes, " + tirages
                        + " piles (remplace " + coffres + " coffres)");
                continue;
            }
            List<Map<String, Object>> contenu = butin.tirer(tirages, frequences, valeur);
            if (contenu.isEmpty()) {
                sender.sendMessage(nom + " : coffre vide (pools introuvables), rien envoyé.");
                continue;
            }
            envois.envoyer(uuid, nom, "Rattrapage : " + coffres + " récompenses gagnées depuis le début des scores",
                    contenu, boite);
            envoyes++;
        }
        if (!envoyer) {
            sender.sendMessage(joueurs.getKeys(false).size() + " joueur(s), " + Math.round(total) + " émeraudes de valeur "
                    + "moyenne au total. Pour envoyer : /kgrewards rattrapage " + boite + " confirmer");
            return;
        }
        java.io.File fait = new java.io.File(getDataFolder(), "rattrapage-envoye.yml");
        if (!fichier.renameTo(fait)) {
            getLogger().severe("Rattrapage envoyé, mais rattrapage.yml n'a pas pu être renommé : le retirer à la main.");
        }
        sender.sendMessage(envoyes + " gros coffre(s) de rattrapage envoyé(s) vers la boîte « " + boite + " ».");
        getLogger().info("Rattrapage envoyé par " + sender.getName() + " : " + envoyes + " coffre(s), boîte " + boite + ".");
    }

    private void tester(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Usage : /kgrewards test <joueur> <semaine | mois | permanent> <niveau | tout> [nombre] [boîte]");
            return;
        }
        OfflinePlayer cible = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (cible == null) {
            sender.sendMessage("Joueur inconnu de ce serveur : " + args[1]);
            return;
        }
        String periode = args[2].toLowerCase();
        if (!Butin.PERIODES.contains(periode)) {
            sender.sendMessage("Période inconnue : " + args[2] + " (semaine | mois | permanent)");
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
