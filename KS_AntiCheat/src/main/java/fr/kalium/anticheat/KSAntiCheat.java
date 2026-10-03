package fr.kalium.anticheat;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KS_AntiCheat (cahier des charges : catégorie 6 « Anti-triche », LeKiwi06, validé le 30/09/2026) : serveur Event.
 *
 * Étape 1 (03/10/2026) : alertes (historique, staff connecté), interface staff (rubrique « Modération » de /menu :
 * Anti-triche, Invsee, EcSee ; /anticheat, /invsee, /ecsee), suspension (connexion refusée, message neutre) et levée,
 * invsee / ecsee en ligne et hors ligne avec journal, morts d'entités importantes.
 * Étapes suivantes : détections (GrimAC, x-ray, macros, AFK), duplication, revente suspecte, bannissement réseau.
 *
 * Permission : ksanticheat.staff (opérateurs par défaut).
 */
public final class KSAntiCheat extends JavaPlugin {

    static final String PERMISSION_STAFF = "ksanticheat.staff";

    private Lang lang;
    private Gui gui;
    private Alertes alertes;
    private Suspensions suspensions;
    private Inventaires inventaires;
    private Morts morts;
    private Menus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        alertes = new Alertes(this);
        suspensions = new Suspensions(this);
        inventaires = new Inventaires(this);
        morts = new Morts(this);
        menus = new Menus(this);
        getServer().getPluginManager().registerEvents(suspensions, this);
        getServer().getPluginManager().registerEvents(inventaires, this);
        getServer().getPluginManager().registerEvents(morts, this);
        for (String commande : new String[]{"anticheat", "invsee", "ecsee"}) {
            getCommand(commande).setExecutor(this);
        }
        // Rubrique « Modération » de /menu (KLM_Menu 2.7.0).
        var services = getServer().getServicesManager();
        services.register(MenuSection.class, MenuSection.moderation(this, "anticheat", PERMISSION_STAFF, 10,
                lang.c("moderation.anticheat", "<dark_red>Anti-triche"),
                lang.c("moderation.anticheat-info", "<gray>Alertes, joueurs suspects, suspensions."), menus::accueil),
                this, ServicePriority.Normal);
        services.register(MenuSection.class, MenuSection.moderation(this, "invsee", PERMISSION_STAFF, 20,
                lang.c("moderation.invsee", "<aqua>Invsee"),
                lang.c("moderation.invsee-info", "<gray>Inventaire d'un joueur (en ligne ou hors ligne)."),
                (p, retour) -> {
                    menus.memoriserRetour(p, retour);
                    menus.chercher(p, true, false);
                }), this, ServicePriority.Normal);
        services.register(MenuSection.class, MenuSection.moderation(this, "ecsee", PERMISSION_STAFF, 30,
                lang.c("moderation.ecsee", "<dark_purple>EcSee"),
                lang.c("moderation.ecsee-info", "<gray>Coffre de l'Ender d'un joueur (en ligne ou hors ligne)."),
                (p, retour) -> {
                    menus.memoriserRetour(p, retour);
                    menus.chercher(p, true, true);
                }), this, ServicePriority.Normal);
        getServer().getScheduler().runTaskTimer(this, alertes::sauver, 20L * 30, 20L * 30);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        if (inventaires != null) {
            inventaires.toutFermer();
        }
        if (alertes != null) {
            alertes.sauver();
        }
    }

    Lang lang() {
        return lang;
    }

    Gui gui() {
        return gui;
    }

    Alertes alertes() {
        return alertes;
    }

    Suspensions suspensions() {
        return suspensions;
    }

    Inventaires inventaires() {
        return inventaires;
    }

    Morts morts() {
        return morts;
    }

    static boolean staff(Player joueur) {
        return joueur.isOp() || joueur.hasPermission(PERMISSION_STAFF);
    }

    /** Joueur en ligne, ou déjà venu sur le serveur (hors ligne). */
    static OfflinePlayer connu(String pseudo) {
        if (pseudo == null || pseudo.isBlank()) {
            return null;
        }
        Player enLigne = Bukkit.getPlayerExact(pseudo.trim());
        if (enLigne != null) {
            return enLigne;
        }
        OfflinePlayer hors = Bukkit.getOfflinePlayerIfCached(pseudo.trim());
        return hors != null && hors.hasPlayedBefore() ? hors : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (!staff(staff)) {
            staff.sendMessage(lang.c("commande.refus", "<red>Commande réservée au staff."));
            lang.saveIfNeeded();
            return true;
        }
        String nom = command.getName();
        OfflinePlayer cible = args.length > 0 ? connu(args[0]) : null;
        if (args.length > 0 && cible == null) {
            staff.sendMessage(lang.c("commande.inconnu", "<red>Joueur inconnu (il doit être déjà venu sur le serveur)."));
            lang.saveIfNeeded();
            return true;
        }
        switch (nom) {
            case "invsee" -> {
                if (cible == null) {
                    menus.chercher(staff, true, false);
                } else {
                    inventaires.ouvrir(staff, cible, false);
                }
            }
            case "ecsee" -> {
                if (cible == null) {
                    menus.chercher(staff, true, true);
                } else {
                    inventaires.ouvrir(staff, cible, true);
                }
            }
            default -> {
                if (cible == null) {
                    menus.accueil(staff, null);
                } else {
                    menus.joueur(staff, cible);
                }
            }
        }
        return true;
    }
}
