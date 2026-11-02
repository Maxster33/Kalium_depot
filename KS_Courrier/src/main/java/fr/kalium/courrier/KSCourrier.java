package fr.kalium.courrier;

import fr.kalium.courrier.Donnees.Envoi;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * KS_Courrier (cahier local Blocs_Cultures_Objets.md, partie 9 ; choix de LeKiwi06 du 02/11/2026) : boîte aux lettres
 * de Kixster.
 *
 * - Boîte aux lettres : bloc custom de KS_Blocs, une par joueur, dans un de ses claims ({@link Boites}).
 * - Lettres (livre écrit + enveloppe) et colis (27 cases + colis vide) envoyés à un joueur qui a sa boîte, livrés même
 *   hors ligne ; frais selon la distance entre les deux boîtes ({@link Poste}) ; l'expéditeur est écrit sur l'objet.
 * - Aucune duplication : le contenu reste au serveur, l'objet scellé ne porte que son numéro ({@link Donnees}).
 * - Historique des envois et des réceptions ; /courrier historique pseudo pour les admins (modération).
 * - Demandes publiques de KS_Contrats 1.1.0 : livrées depuis la boîte ; dépôts du serveur ({@link LivraisonCourrier}).
 * - KS_Claim 1.6.0 : le claim de la boîte ne peut être ni supprimé ni vendu ({@link GardeClaim}).
 * - Monde de test : courrier du « moi de test », séparé de la survie (CompteTest).
 * Pas encore : Père Noël (événement de décembre, à préciser), journal hebdomadaire (pas pour le moment).
 */
public final class KSCourrier extends JavaPlugin implements Listener, TabExecutor {

    private Lang lang;
    private Gui gui;
    private Donnees donnees;
    private Objets objets;
    private Poste poste;
    private Menus menus;

    @Override
    public void onEnable() {
        lang = new Lang(this);
        gui = new Gui(this, lang);
        donnees = new Donnees(this);
        objets = new Objets(this);
        poste = new Poste(this, donnees, objets);
        menus = new Menus(this, donnees, poste, objets);
        Boites boites = new Boites(this, donnees);
        boolean boiteActive = boites.declarer(objets);
        objets.ajouterRecettes(boiteActive ? fr.kalium.blocs.KSBlocs.objet(Objets.ID_BOITE, 1) : null);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(boites, this);
        getServer().getPluginManager().registerEvents(menus, this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Contrats")) {
            getServer().getServicesManager().register(fr.kalium.contrats.api.Livraison.class,
                    new LivraisonCourrier(poste), this, ServicePriority.Normal);
        }
        if (getServer().getPluginManager().isPluginEnabled("KS_Claim")) {
            getServer().getServicesManager().register(fr.kalium.claim.api.GardienClaim.class, new GardeClaim(donnees),
                    this, ServicePriority.Normal);
        }
        getCommand("courrier").setExecutor(this);
        getCommand("courrier").setTabCompleter(this);
        lang.saveIfNeeded();
        getLogger().info(donnees.boites.size() + " boites aux lettres, " + donnees.envois.size() + " envois.");
    }

    @Override
    public void onDisable() {
        if (menus != null) {
            menus.fermerTout();
        }
        if (objets != null) {
            objets.retirerRecettes();
        }
        getServer().getServicesManager().unregisterAll(this);
        if (donnees != null) {
            donnees.sauver();
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

    // ------------------------------------------------------------------ objets

    /** Clic droit avec une lettre ou un colis scellé : l'ouvrir. */
    @EventHandler(priority = EventPriority.LOW)
    public void onUtiliser(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack o = event.getItem();
        String id = objets.envoi(o);
        if (id == null || event.getHand() == null) {
            return;
        }
        fr.kalium.blocs.TypeBloc bloc = event.getClickedBlock() == null ? null
                : fr.kalium.blocs.KSBlocs.type(event.getClickedBlock());
        if (bloc != null && Objets.ID_BOITE.equals(bloc.id())) {
            return; // clic sur la boîte aux lettres : son menu (Boites)
        }
        event.setCancelled(true);
        poste.ouvrir(event.getPlayer(), event.getHand(), o, id);
    }

    /** Les objets du courrier (base : papier) ne servent dans aucune recette (livre, carte...). */
    @EventHandler
    public void onPreparer(PrepareItemCraftEvent event) {
        for (ItemStack o : event.getInventory().getMatrix()) {
            if (objets.marque(o) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    /** Courrier en attente, à la connexion. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) {
                return;
            }
            List<Envoi> recus = donnees.recus(Poste.compte(p));
            if (!recus.isEmpty()) {
                p.sendMessage(lang.c("connexion.recus", "<gold>[Courrier] <n> envoi<s> t'attend<ent> dans ta boîte aux "
                        + "lettres.", "n", recus.size(), "s", recus.size() > 1 ? "s" : "", "ent",
                        recus.size() > 1 ? "ent" : ""));
                lang.saveIfNeeded();
            }
        }, 80L);
    }

    // ------------------------------------------------------------------ commande (admins)

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("historique")) {
            joueur.sendMessage(lang.c("commande.usage", "<gray>/courrier historique <pseudo> : courrier d'un joueur."));
            return true;
        }
        OfflinePlayer o = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (o == null) {
            joueur.sendMessage(lang.c("refus.pseudo-inconnu", "<red>Joueur inconnu : « <pseudo> ».", "pseudo", args[1]));
            return true;
        }
        UUID compte = args.length > 2 && args[2].equalsIgnoreCase("test") ? CompteTest.compteDeTest(o.getUniqueId())
                : o.getUniqueId();
        menus.historique(joueur, compte, o.getName() == null ? args[1] : o.getName(), 0, null);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return "historique".startsWith(args[0].toLowerCase()) ? List.of("historique") : List.of();
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        return args.length == 3 ? List.of("test") : List.of();
    }
}
