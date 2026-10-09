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
 * Étape 2 : détections (Detections : GrimAC, macros, AFK ; Minage : x-ray), suspension automatique des infractions
 * graves, onglet « Minage » de la rubrique Modération.
 * Étape 3 (duplication par identifiant) : reportée par LeKiwi06 le 03/10/2026.
 * Étape 4 : revente suspecte (Revente).
 * Étape 5 : « Bannir de KaLium » (LibertyBans sur le proxy, par KaliumRelay 1.4.0) ; règle tntExplosionDropDecay
 * activée dans les mondes d'Event (fermes à TNT moins efficaces, choix de LeKiwi06).
 * 1.2.0 (LeKiwi06, 09/10/2026) : « Joueurs » dans la rubrique Modération : tous les joueurs, même hors ligne, en têtes
 * dans un coffre (Joueurs) ; la fiche d'un joueur liste aussi ses claims (Claims, SimpleClaimSystem).
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
    private Joueurs joueurs;
    private Detections detections;
    private Minage minage;
    private Revente revente;

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
        joueurs = new Joueurs(this);
        detections = new Detections(this);
        minage = new Minage(this);
        getServer().getPluginManager().registerEvents(detections, this);
        getServer().getPluginManager().registerEvents(minage, this);
        // Étape 4 : revente suspecte (signaux de KS_Economy 1.1.5 et KS_RewardsGUI 1.0.1 s'ils sont là).
        revente = new Revente(this);
        getServer().getPluginManager().registerEvents(revente, this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Economy")) {
            try {
                getServer().getPluginManager().registerEvents(new Revente.EcouteEchanges(revente), this);
            } catch (LinkageError e) {
                getLogger().warning("KS_Economy trop ancien (1.1.5 nécessaire) : /echange non suivi.");
            }
        }
        if (getServer().getPluginManager().isPluginEnabled("KS_RewardsGUI")) {
            try {
                getServer().getPluginManager().registerEvents(new Revente.EcouteRecompenses(revente), this);
            } catch (LinkageError e) {
                getLogger().warning("KS_RewardsGUI trop ancien (1.0.1 nécessaire) : /rewards non suivi.");
            }
        }
        // Une fois tous les plugins démarrés : signalements de GrimAC.
        getServer().getScheduler().runTask(this, detections::brancherGrim);
        // Étape 5 : fermes à TNT moins efficaces (règle vanilla, environ divisé par 4 ; réglage tnt-drop-decay).
        if (getConfig().getBoolean("tnt-drop-decay", true)) {
            getServer().getScheduler().runTask(this, () -> getServer().getWorlds().forEach(w -> {
                if (!Boolean.TRUE.equals(w.getGameRuleValue(org.bukkit.GameRule.TNT_EXPLOSION_DROP_DECAY))) {
                    w.setGameRule(org.bukkit.GameRule.TNT_EXPLOSION_DROP_DECAY, true);
                    getLogger().info("Règle tntExplosionDropDecay activée dans " + w.getName() + ".");
                }
            }));
        }
        getServer().getPluginManager().registerEvents(suspensions, this);
        getServer().getPluginManager().registerEvents(inventaires, this);
        getServer().getPluginManager().registerEvents(morts, this);
        getServer().getPluginManager().registerEvents(joueurs, this);
        for (String commande : new String[]{"anticheat", "invsee", "ecsee"}) {
            getCommand(commande).setExecutor(this);
        }
        // Rubrique « Modération » de /menu (KLM_Menu 2.7.0).
        var services = getServer().getServicesManager();
        services.register(MenuSection.class, MenuSection.moderation(this, "joueurs", PERMISSION_STAFF, 5,
                lang.c("moderation.joueurs", "<green>Joueurs"),
                lang.c("moderation.joueurs-info", "<gray>Tous les joueurs, même hors ligne : inventaire, coffre de l'Ender, "
                        + "claims, alertes."), (p, retour) -> joueurs.ouvrir(p, retour)), this, ServicePriority.Normal);
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
        services.register(MenuSection.class, MenuSection.moderation(this, "minage", PERMISSION_STAFF, 40,
                lang.c("moderation.minage", "<gold>Minage (x-ray)"),
                lang.c("moderation.minage-info", "<gray>Filons rares cachés trouvés par joueur (24 dernières heures)."),
                menus::minage), this, ServicePriority.Normal);
        getServer().getScheduler().runTaskTimer(this, () -> {
            alertes.sauver();
            minage.sauver();
        }, 20L * 30, 20L * 30);
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
        if (minage != null) {
            minage.sauver();
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

    Menus menus() {
        return menus;
    }

    Joueurs joueurs() {
        return joueurs;
    }

    Morts morts() {
        return morts;
    }

    Minage minage() {
        return minage;
    }

    /**
     * Étape 5 : bannit de tout KaLium (LibertyBans sur le proxy) par KaliumRelay 1.4.0 (POST /ban), hors du fil
     * principal ; resultat : vrai si le proxy a lancé le bannissement.
     */
    void bannirDeKalium(String pseudo, String raison, String staff, java.util.function.Consumer<Boolean> resultat) {
        String url = getConfig().getString("relay-url", "");
        String jeton = getConfig().getString("relay-token", "");
        if (url.isBlank() || jeton.isBlank()) {
            resultat.accept(false);
            return;
        }
        String corps = pseudo + "\n" + raison.replace('\n', ' ') + "\n" + staff;
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            boolean ok;
            try {
                var reponse = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
                                java.net.URI.create((url.endsWith("/") ? url.substring(0, url.length() - 1) : url) + "/ban"))
                        .timeout(java.time.Duration.ofSeconds(10)).header("X-Kalium-Relay-Token", jeton)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(corps)).build(),
                        java.net.http.HttpResponse.BodyHandlers.discarding());
                ok = reponse.statusCode() == 200;
            } catch (java.io.IOException | InterruptedException e) {
                ok = false;
            }
            boolean fin = ok;
            getServer().getScheduler().runTask(this, () -> {
                getLogger().info("Bannissement de KaLium de " + pseudo + " par " + staff + " : "
                        + (fin ? "lancé" : "échec") + " (" + raison + ")");
                resultat.accept(fin);
            });
        });
    }

    /** Infraction grave : alerte grave et suspension automatique. */
    void infractionGrave(java.util.UUID joueur, String nom, String type, String detail) {
        alertes.ajouter(joueur, nom, type, Alertes.Gravite.GRAVE, detail);
        if (!suspensions.suspendu(joueur)) {
            suspensions.suspendre(joueur, nom, type + " : " + detail, "automatique");
        }
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
