package fr.kalium.anticheat;

import fr.kalium.anticheat.Alertes.Alerte;
import fr.kalium.anticheat.Suspensions.Suspension;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Interface staff (cahier, catégorie 6 ; rubrique « Modération » de /menu et /anticheat) : alertes récentes, joueurs
 * avec alertes, recherche d'un joueur (alertes, invsee, ecsee, suspendre / lever), suspendus, morts d'entités
 * importantes, journal des consultations. 1.2.0 : « Tous les joueurs » (liste en têtes, Joueurs) et claims d'un joueur.
 * 1.3.0 : fiche d'un joueur : économie, maisons, jetons et récompenses (Infos), téléportation du staff à un claim, une
 * maison ou un coffre de mort.
 */
final class Menus {

    private static final int PAR_PAGE = 10;
    /** 1.3.0 : ventes, achats et échanges récents montrés dans « Économie ». */
    private static final int RECENTS = 5;

    private final KSAntiCheat plugin;
    private final Lang lang;
    private final Gui gui;
    /** Retour vers la rubrique « Modération » de KLM_Menu (si le menu a été ouvert de là). */
    private final Map<UUID, Consumer<Player>> retours = new HashMap<>();
    /** 1.2.0 : retour de la fiche d'un joueur vers la liste en têtes (si la fiche a été ouverte de là). */
    private final Map<UUID, Consumer<Player>> retoursFiche = new HashMap<>();

    Menus(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private ActionButton retour(Consumer<Player> vers) {
        return gui.button(t("menu.retour", "<gray>Retour"), null, vers::accept);
    }

    // ------------------------------------------------------------------ accueil

    void memoriserRetour(Player staff, Consumer<Player> retourModeration) {
        if (retourModeration != null) {
            retours.put(staff.getUniqueId(), retourModeration);
        }
    }

    void accueil(Player staff, Consumer<Player> retourModeration) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        if (retourModeration != null) {
            retours.put(staff.getUniqueId(), retourModeration);
        }
        long jour = System.currentTimeMillis() - 24L * 3600_000;
        long recentes = plugin.alertes().recentes(5000).stream()
                .filter(a -> a.date >= jour && a.gravite != Alertes.Gravite.ACTION).count();
        List<Component> corps = List.of(t("menu.resume", "<white>Alertes des dernières 24 h : <yellow><n></yellow> ; "
                + "suspendus : <red><s>", "n", recentes, "s", plugin.suspensions().toutes().size()));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("menu.bouton-alertes", "<yellow>Alertes récentes"), null, p -> alertesRecentes(p)));
        boutons.add(gui.button(t("menu.bouton-tous", "<green>Tous les joueurs"), null,
                p -> plugin.joueurs().ouvrir(p, this::accueil)));
        boutons.add(gui.button(t("menu.bouton-joueurs", "<white>Joueurs avec alertes"), null, p -> joueurs(p, 0)));
        boutons.add(gui.button(t("menu.bouton-chercher", "<white>Chercher un joueur"), null, p -> chercher(p, false, false)));
        boutons.add(gui.button(t("menu.bouton-suspendus", "<red>Suspendus"), null, this::suspendus));
        boutons.add(gui.button(t("menu.bouton-minage", "<gold>Minage (x-ray)"), null, p -> minage(p, null)));
        boutons.add(gui.button(t("menu.bouton-morts", "<white>Morts d'entités"), null, this::morts));
        boutons.add(gui.button(t("menu.bouton-consultations", "<white>Journal invsee / ecsee"), null, this::consultations));
        Consumer<Player> versModeration = retours.get(staff.getUniqueId());
        if (versModeration != null) {
            boutons.add(retour(versModeration));
        }
        gui.open(staff, t("menu.titre", "<dark_red><bold>Anti-triche"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void accueil(Player staff) {
        accueil(staff, null);
    }

    // ------------------------------------------------------------------ alertes

    private void alertesRecentes(Player staff) {
        List<Alerte> liste = plugin.alertes().recentes(15);
        List<Component> corps = new ArrayList<>();
        Map<UUID, String> joueurs = new java.util.LinkedHashMap<>();
        for (Alerte a : liste) {
            corps.add(Component.text(a.nom + " - " + Alertes.ligne(a)));
            joueurs.putIfAbsent(a.joueur, a.nom);
        }
        if (corps.isEmpty()) {
            corps.add(t("menu.aucune-alerte", "<gray>Aucune alerte."));
        }
        List<ActionButton> boutons = new ArrayList<>();
        joueurs.forEach((uuid, nom) -> boutons.add(gui.button(Component.text(nom), null,
                p -> joueur(p, plugin.getServer().getOfflinePlayer(uuid)))));
        boutons.add(retour(this::accueil));
        gui.open(staff, t("menu.titre-alertes", "<yellow><bold>Alertes récentes"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void joueurs(Player staff, int page) {
        List<Map.Entry<UUID, String>> liste = new ArrayList<>(plugin.alertes().joueurs().entrySet());
        int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = p * PAR_PAGE; i < Math.min(liste.size(), (p + 1) * PAR_PAGE); i++) {
            UUID uuid = liste.get(i).getKey();
            int n = plugin.alertes().duJoueur(uuid).size();
            boutons.add(gui.button(Component.text(liste.get(i).getValue() + " (" + n + ")"), null,
                    j -> joueur(j, plugin.getServer().getOfflinePlayer(uuid))));
        }
        if (p > 0) {
            boutons.add(gui.button(t("menu.precedent", "<yellow>Page précédente"), null, j -> joueurs(j, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("menu.suivant", "<yellow>Page suivante"), null, j -> joueurs(j, p + 1)));
        }
        boutons.add(retour(this::accueil));
        gui.open(staff, t("menu.titre-joueurs", "<white><bold>Joueurs avec alertes"),
                List.of(liste.isEmpty() ? t("menu.aucune-alerte", "<gray>Aucune alerte.")
                        : t("menu.joueurs-aide", "<gray>Le plus récent d'abord ; entre parenthèses : nombre d'alertes.")),
                List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    /** Recherche d'un joueur ; directInv / ender : ouvre directement son inventaire ou son coffre de l'Ender. */
    void chercher(Player staff, boolean directInv, boolean ender) {
        ActionButton valider = gui.form(t("menu.bouton-valider", "<green>Valider"), null, (p, vue) -> {
            OfflinePlayer cible = KSAntiCheat.connu(vue.getText("pseudo"));
            if (cible == null) {
                gui.notice(p, t("menu.titre", "<dark_red><bold>Anti-triche"),
                        t("menu.inconnu", "<red>Joueur inconnu (il doit être déjà venu sur le serveur)."),
                        q -> chercher(q, directInv, ender));
                lang.saveIfNeeded();
                return;
            }
            if (directInv) {
                plugin.inventaires().ouvrir(p, cible, ender);
            } else {
                joueur(p, cible);
            }
        });
        Consumer<Player> versModeration = retours.get(staff.getUniqueId());
        ActionButton retour = directInv && versModeration != null ? retour(versModeration) : retour(this::accueil);
        gui.open(staff, directInv ? (ender ? t("menu.titre-ecsee", "<dark_purple><bold>EcSee")
                        : t("menu.titre-invsee", "<aqua><bold>Invsee")) : t("menu.titre-chercher", "<white><bold>Chercher un joueur"),
                List.of(), List.of(gui.text("pseudo", t("menu.champ-pseudo", "Pseudo du joueur"), "", 16)),
                List.of(valider, retour), gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ joueur

    /** Fiche d'un joueur ; « Retour » ramène à l'accueil. */
    void joueur(Player staff, OfflinePlayer cible) {
        joueur(staff, cible, null);
    }

    /** 1.2.0 : fiche d'un joueur ouverte depuis la liste en têtes : « Retour » y ramène (retour null : accueil). */
    void joueur(Player staff, OfflinePlayer cible, Consumer<Player> retour) {
        if (retour == null) {
            retoursFiche.remove(staff.getUniqueId());
        } else {
            retoursFiche.put(staff.getUniqueId(), retour);
        }
        fiche(staff, cible);
    }

    private void fiche(Player staff, OfflinePlayer cible) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        UUID uuid = cible.getUniqueId();
        String nom = cible.getName() == null ? "?" : cible.getName();
        List<Alerte> alertes = plugin.alertes().duJoueur(uuid);
        Suspension s = plugin.suspensions().suspension(uuid);
        List<Component> corps = new ArrayList<>();
        corps.add(t("joueur.etat", "<white><nom> <gray>- <etat> - <n> alerte(s)", "nom", nom,
                "etat", cible.isOnline() ? "en ligne" : "hors ligne",
                "n", alertes.stream().filter(a -> a.gravite != Alertes.Gravite.ACTION).count()));
        if (s != null) {
            corps.add(t("joueur.suspendu", "<red>Suspendu le <date> (<par>) : <raison>", "date",
                    Alertes.DATE.format(Instant.ofEpochMilli(s.date)), "par", s.par, "raison", s.raison));
        }
        for (int i = 0; i < Math.min(8, alertes.size()); i++) {
            corps.add(Component.text(Alertes.ligne(alertes.get(i))));
        }
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("joueur.bouton-invsee", "<aqua>Inventaire"), null, p -> plugin.inventaires().ouvrir(p, cible, false)));
        boutons.add(gui.button(t("joueur.bouton-ecsee", "<dark_purple>Coffre de l'Ender"), null,
                p -> plugin.inventaires().ouvrir(p, cible, true)));
        if (Claims.disponible()) {
            List<Claims.Ligne> claims = Claims.de(uuid);
            boutons.add(gui.button(t("joueur.bouton-claims", "<green>Claims (<n>)", "n", claims == null ? "?" : claims.size()),
                    null, p -> claims(p, cible, 0)));
        }
        // 1.3.0 (LeKiwi06) : économie, maisons, jetons et récompenses.
        if (Infos.economieDisponible()) {
            boutons.add(gui.button(t("joueur.bouton-economie", "<gold>Économie"), null, p -> economie(p, cible)));
        }
        boutons.add(gui.button(t("joueur.bouton-maisons", "<green>Maisons"), null, p -> maisons(p, cible)));
        if (Infos.jetonsDisponibles()) {
            boutons.add(gui.button(t("joueur.bouton-jetons", "<yellow>Jetons et récompenses"), null, p -> jetons(p, cible)));
        }
        if (s == null) {
            boutons.add(gui.button(t("joueur.bouton-suspendre", "<red>Suspendre"), null, p -> suspendre(p, cible)));
        } else {
            boutons.add(gui.button(t("joueur.bouton-lever", "<green>Lever la suspension"), null, p -> gui.confirm(p,
                    t("joueur.titre-lever", "<green><bold>Lever la suspension"),
                    t("joueur.lever-texte", "<white><nom> pourra de nouveau se connecter à Event.", "nom", nom),
                    q -> {
                        plugin.suspensions().lever(uuid, q.getName());
                        fiche(q, cible);
                    }, q -> fiche(q, cible))));
        }
        boutons.add(gui.button(t("joueur.bouton-bannir", "<dark_red>Bannir de KaLium"), null, p -> bannir(p, cible)));
        // 1.0.2 (LeKiwi06) : historique complet (alertes et actions du staff), dès la première entrée.
        if (!alertes.isEmpty()) {
            boutons.add(gui.button(t("joueur.bouton-historique", "<yellow>Historique des alertes"), null,
                    p -> toutes(p, cible, 0)));
        }
        boutons.add(retour(retoursFiche.getOrDefault(staff.getUniqueId(), this::accueil)));
        gui.open(staff, t("joueur.titre", "<dark_red><bold><nom>", "nom", nom), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void toutes(Player staff, OfflinePlayer cible, int page) {
        List<Alerte> alertes = plugin.alertes().duJoueur(cible.getUniqueId());
        int pages = Math.max(1, (alertes.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<Component> corps = new ArrayList<>();
        corps.add(t("historique.entete", "<gray>Page <page> / <pages> - <n> entrée(s), la plus récente d'abord. "
                + "<red>[GRAVE]</red> : suspension automatique ; <aqua>[STAFF]</aqua> : action du staff.", "page", p + 1,
                "pages", pages, "n", alertes.size()));
        for (int i = p * PAR_PAGE; i < Math.min(alertes.size(), (p + 1) * PAR_PAGE); i++) {
            Alerte a = alertes.get(i);
            corps.add(Component.text(Alertes.ligne(a), a.gravite == Alertes.Gravite.GRAVE
                    ? net.kyori.adventure.text.format.NamedTextColor.RED : a.gravite == Alertes.Gravite.ACTION
                    ? net.kyori.adventure.text.format.NamedTextColor.AQUA : net.kyori.adventure.text.format.NamedTextColor.WHITE));
        }
        List<ActionButton> boutons = new ArrayList<>();
        if (p > 0) {
            boutons.add(gui.button(t("menu.precedent", "<yellow>Page précédente"), null, j -> toutes(j, cible, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("menu.suivant", "<yellow>Page suivante"), null, j -> toutes(j, cible, p + 1)));
        }
        boutons.add(retour(j -> fiche(j, cible)));
        gui.open(staff, t("joueur.titre-historique", "<yellow><bold>Historique de <nom>", "nom",
                cible.getName() == null ? "?" : cible.getName()), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    /** 1.2.0 : claims possédés par le joueur (SimpleClaimSystem), 10 par page : nom et position. 1.3.0 : s'y téléporter. */
    private void claims(Player staff, OfflinePlayer cible, int page) {
        String nom = cible.getName() == null ? "?" : cible.getName();
        List<Claims.Ligne> liste = Claims.de(cible.getUniqueId());
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        if (liste == null) {
            corps.add(t("claims.illisibles", "<red>Claims illisibles (SimpleClaimSystem ne répond pas)."));
        } else if (liste.isEmpty()) {
            corps.add(t("claims.aucun", "<gray>Aucun claim."));
        } else {
            int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
            int p = Math.max(0, Math.min(page, pages - 1));
            corps.add(t("claims.entete-2", "<gray>Page <page> / <pages> - <n> claim(s) : nom, puis position (centre du "
                    + "chunk). Clique sur un claim pour t'y téléporter.", "page", p + 1, "pages", pages, "n", liste.size()));
            for (int i = p * PAR_PAGE; i < Math.min(liste.size(), (p + 1) * PAR_PAGE); i++) {
                Claims.Ligne claim = liste.get(i);
                corps.add(t("claims.ligne", "<white><nom> <gray>- <position>", "nom", claim.nom(),
                        "position", claim.position()));
                if (claim.arrivee() != null) {
                    boutons.add(gui.button(Component.text(claim.nom()), Component.text(claim.position()),
                            j -> teleporter(j, claim.arrivee(), "claim « " + claim.nom() + " » de " + nom)));
                }
            }
            if (p > 0) {
                boutons.add(gui.button(t("menu.precedent", "<yellow>Page précédente"), null, j -> claims(j, cible, p - 1)));
            }
            if (p < pages - 1) {
                boutons.add(gui.button(t("menu.suivant", "<yellow>Page suivante"), null, j -> claims(j, cible, p + 1)));
            }
        }
        boutons.add(retour(j -> fiche(j, cible)));
        gui.open(staff, t("claims.titre", "<green><bold>Claims de <nom>", "nom", nom), corps, List.of(), boutons,
                gui.close(), 2);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ 1.3.0 : économie, maisons, jetons et récompenses

    /** Téléporte le staff (regard gardé) ; chaque téléportation est notée dans la console. */
    private void teleporter(Player staff, Location lieu, String quoi) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        if (lieu == null || !lieu.isWorldLoaded()) {
            staff.sendMessage(t("tp.echec", "<red>Téléportation impossible (monde non chargé)."));
            lang.saveIfNeeded();
            return;
        }
        Location arrivee = lieu.clone();
        arrivee.setYaw(staff.getLocation().getYaw());
        arrivee.setPitch(staff.getLocation().getPitch());
        plugin.getLogger().info(staff.getName() + " se téléporte : " + quoi + " (" + Infos.position(lieu) + ").");
        staff.teleportAsync(arrivee).thenAccept(reussi -> {
            staff.sendMessage(reussi ? t("tp.fait", "<green>Téléporté : <quoi>.", "quoi", quoi)
                    : t("tp.refus", "<red>Téléportation refusée."));
            lang.saveIfNeeded();
        });
    }

    /** Un titre, puis ses lignes (ou « Rien. »). */
    private void section(List<Component> corps, Component titre, List<String> lignes) {
        corps.add(titre);
        if (lignes.isEmpty()) {
            corps.add(t("fiche.rien", "<gray>Rien."));
        }
        for (String ligne : lignes) {
            corps.add(Component.text(ligne));
        }
    }

    /** Solde, magasin, ventes de ses boutiques, derniers achats (KS_Economy 1.4.1) et derniers /echange (Echanges). */
    private void economie(Player staff, OfflinePlayer cible) {
        String nom = cible.getName() == null ? "?" : cible.getName();
        List<Component> corps = new ArrayList<>();
        Infos.Economie eco = Infos.economie(cible.getUniqueId(), RECENTS);
        if (eco == null) {
            corps.add(t("eco.illisible", "<red>KS_Economy est trop ancien (1.4.1 nécessaire) : solde et ventes illisibles."));
        } else {
            for (String ligne : eco.resume()) {
                corps.add(Component.text(ligne));
            }
            section(corps, t("eco.ventes", "<yellow>Dernières ventes de ses boutiques"), eco.ventes());
            section(corps, t("eco.achats", "<yellow>Derniers achats en boutique"), eco.achats());
        }
        section(corps, t("eco.echanges", "<yellow>Derniers /echange <gray>(suivis depuis KS_AntiCheat 1.3.0)"),
                plugin.echanges().derniers(cible.getUniqueId(), RECENTS));
        gui.open(staff, t("eco.titre", "<gold><bold>Économie de <nom>", "nom", nom), corps, List.of(),
                List.of(retour(j -> fiche(j, cible))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    /** Lit, maison du spawn et emplacements (KS_Teleport 1.0.1) : position de chacun, et s'y téléporter. */
    private void maisons(Player staff, OfflinePlayer cible) {
        String nom = cible.getName() == null ? "?" : cible.getName();
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        Map<String, Location> lieux = Infos.maisons(cible);
        if (lieux.isEmpty()) {
            corps.add(t("maisons.aucune", "<gray>Aucune destination enregistrée (ni lit, ni maison du spawn, ni emplacement)."));
        } else {
            corps.add(t("maisons.aide", "<gray>Clique sur une destination pour t'y téléporter. Le lit est son point de "
                    + "réapparition enregistré."));
        }
        lieux.forEach((quoi, lieu) -> {
            corps.add(Component.text(quoi + " : " + Infos.position(lieu)));
            boutons.add(gui.button(Component.text(quoi), Component.text(Infos.position(lieu)),
                    j -> teleporter(j, lieu, quoi.toLowerCase(java.util.Locale.ROOT) + " de " + nom)));
        });
        boutons.add(retour(j -> fiche(j, cible)));
        gui.open(staff, t("maisons.titre", "<green><bold>Maisons de <nom>", "nom", nom), corps, List.of(), boutons,
                gui.close(), 2);
        lang.saveIfNeeded();
    }

    /**
     * Jetons et badges (KS_Jetons), récompenses en attente (KS_RewardsGUI 1.4.1 ; les 10 plus récentes), coffres de mort
     * (KS_CoffreMort 1.0.2).
     */
    private void jetons(Player staff, OfflinePlayer cible) {
        String nom = cible.getName() == null ? "?" : cible.getName();
        UUID uuid = cible.getUniqueId();
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        List<String> jetons = Infos.jetons(uuid);
        if (jetons != null) {
            section(corps, t("jetons.special", "<yellow>Inventaire spécial : jetons, puis badges portés"), jetons);
        }
        List<String> recompenses = Infos.recompenses(uuid);
        if (recompenses != null) {
            int n = recompenses.size();
            section(corps, t("jetons.recompenses", "<yellow>Récompenses en attente dans /rewards : <n>", "n", n),
                    n > PAR_PAGE ? recompenses.subList(n - PAR_PAGE, n) : recompenses);
        }
        List<Infos.CoffreMort> coffres = Infos.coffresDeMort(uuid);
        if (coffres != null) {
            List<String> lignes = new ArrayList<>();
            for (int i = 0; i < coffres.size(); i++) {
                Infos.CoffreMort coffre = coffres.get(i);
                String quoi = "Coffre de mort " + (i + 1);
                lignes.add(quoi + " : " + coffre.texte());
                boutons.add(gui.button(Component.text(quoi), Component.text(coffre.texte()),
                        j -> teleporter(j, coffre.position().clone().add(0, 1, 0), "coffre de mort de " + nom)));
            }
            section(corps, t("jetons.coffres", "<yellow>Coffres de mort actifs <gray>(clique pour t'y téléporter)"), lignes);
        } else if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("KS_CoffreMort")) {
            corps.add(t("jetons.coffres-illisibles", "<red>KS_CoffreMort est trop ancien (1.0.2 nécessaire) : coffres de mort "
                    + "illisibles."));
        }
        boutons.add(retour(j -> fiche(j, cible)));
        gui.open(staff, t("jetons.titre", "<yellow><bold>Jetons de <nom>", "nom", nom), corps, List.of(), boutons,
                gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void suspendre(Player staff, OfflinePlayer cible) {
        String nom = cible.getName() == null ? "?" : cible.getName();
        ActionButton valider = gui.form(t("joueur.bouton-suspendre-ok", "<red>Suspendre"), null, (p, vue) -> {
            String raison = vue.getText("raison") == null ? "" : vue.getText("raison").trim();
            plugin.suspensions().suspendre(cible.getUniqueId(), nom, raison.isEmpty() ? "sans raison précisée" : raison,
                    p.getName());
            fiche(p, cible);
        });
        gui.open(staff, t("joueur.titre-suspendre", "<red><bold>Suspendre <nom>", "nom", nom),
                List.of(t("joueur.suspendre-aide", "<gray>Il ne pourra plus se connecter à Event (message : « Une erreur "
                        + "inhabituelle est survenue, contacte le staff. »), jusqu'à ce que le staff lève la suspension.")),
                List.of(gui.text("raison", t("joueur.champ-raison", "Raison (pour le staff)"), "", 100)),
                List.of(valider, retour(p -> fiche(p, cible))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    /** Étape 5 : bannissement de tout KaLium (LibertyBans, par le proxy), avec une raison et une confirmation. */
    private void bannir(Player staff, OfflinePlayer cible) {
        String nom = cible.getName() == null ? "?" : cible.getName();
        ActionButton valider = gui.form(t("joueur.bouton-bannir-ok", "<dark_red>Bannir de KaLium"), null, (p, vue) -> {
            String raison = vue.getText("raison") == null ? "" : vue.getText("raison").trim();
            String motif = raison.isEmpty() ? "Anti-triche" : raison;
            gui.confirm(p, t("joueur.titre-bannir-confirmer", "<dark_red><bold>Bannir <nom> ?", "nom", nom),
                    t("joueur.bannir-confirmer", "<white><nom> ne pourra plus se connecter à aucun serveur de KaLium. "
                            + "<gray>Raison : <raison>", "nom", nom, "raison", motif),
                    q -> plugin.bannirDeKalium(nom, motif, q.getName(), ok -> {
                        if (ok) {
                            plugin.alertes().noter(cible.getUniqueId(), nom, "Banni de KaLium",
                                    "par " + q.getName() + " : " + motif);
                        }
                        gui.notice(q, t("joueur.titre-bannir", "<dark_red><bold>Bannir de KaLium"), ok
                                        ? t("joueur.banni", "<green><nom> est banni de KaLium.", "nom", nom)
                                        : t("joueur.ban-echec", "<red>Échec : relais injoignable, relay-token vide ou "
                                        + "LibertyBans absent du proxy."),
                                r -> fiche(r, cible));
                        lang.saveIfNeeded();
                    }), q -> fiche(q, cible));
            lang.saveIfNeeded();
        });
        gui.open(staff, t("joueur.titre-bannir", "<dark_red><bold>Bannir de KaLium"),
                List.of(t("joueur.bannir-aide", "<gray>Bannissement de tous les serveurs de KaLium (LibertyBans, sur le "
                        + "proxy), sans durée. À utiliser quand le joueur n'est pas clean ; sinon : lever la suspension.")),
                List.of(gui.text("raison", t("joueur.champ-raison-ban", "Raison du bannissement"), "", 150)),
                List.of(valider, retour(p -> fiche(p, cible))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ listes

    private void suspendus(Player staff) {
        Map<UUID, Suspension> liste = plugin.suspensions().toutes();
        List<ActionButton> boutons = new ArrayList<>();
        liste.forEach((uuid, s) -> boutons.add(gui.button(Component.text(s.nom), t("suspendus.info", "<gray><raison>",
                "raison", s.raison), p -> joueur(p, plugin.getServer().getOfflinePlayer(uuid)))));
        boutons.add(retour(this::accueil));
        gui.open(staff, t("suspendus.titre", "<red><bold>Suspendus"), List.of(liste.isEmpty()
                        ? t("suspendus.aucun", "<gray>Aucun joueur suspendu.")
                        : t("suspendus.aide", "<gray>Choisis un joueur : alertes, inventaire, lever la suspension.")),
                List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void morts(Player staff) {
        List<Component> corps = new ArrayList<>();
        for (String l : plugin.morts().dernieres(15)) {
            corps.add(Component.text(l));
        }
        if (corps.isEmpty()) {
            corps.add(t("morts.aucune", "<gray>Aucune mort d'entité importante depuis le démarrage (journal complet : "
                    + "plugins/KS_AntiCheat/morts.log)."));
        }
        gui.open(staff, t("morts.titre", "<white><bold>Morts d'entités importantes"), corps, List.of(),
                List.of(retour(this::accueil)), gui.close(), 1);
        lang.saveIfNeeded();
    }

    /** Onglet « Minage » (rubrique Modération) : filons rares cachés par joueur, 24 dernières heures. */
    void minage(Player staff, Consumer<Player> retourModeration) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        memoriserRetour(staff, retourModeration);
        List<Component> corps = new ArrayList<>();
        corps.add(t("minage.aide-2", "<gray>Par joueur (24 h) : roche minée, puis chaque minerai : total, cachés (sans "
                + "air, eau ni lave d'origine autour), pourcentage de la roche. Classés par minerais rares cachés (diamant, "
                + "émeraude, débris) pour 1 000 blocs de roche."));
        List<String> lignes = plugin.minage().resume24h();
        for (int i = 0; i < Math.min(15, lignes.size()); i++) {
            corps.add(Component.text(lignes.get(i)));
        }
        if (lignes.isEmpty()) {
            corps.add(t("minage.aucun-2", "<gray>Aucun minage ces 24 dernières heures."));
        }
        List<ActionButton> boutons = new ArrayList<>();
        Consumer<Player> versModeration = retours.get(staff.getUniqueId());
        boutons.add(versModeration != null ? retour(versModeration) : retour(this::accueil));
        gui.open(staff, t("minage.titre", "<gold><bold>Minage (x-ray)"), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void consultations(Player staff) {
        List<Component> corps = new ArrayList<>();
        for (String l : plugin.inventaires().dernieresConsultations(15)) {
            corps.add(Component.text(l));
        }
        if (corps.isEmpty()) {
            corps.add(t("consultations.aucune", "<gray>Aucune consultation pour l'instant."));
        }
        gui.open(staff, t("consultations.titre", "<white><bold>Journal invsee / ecsee"), corps, List.of(),
                List.of(retour(this::accueil)), gui.close(), 1);
        lang.saveIfNeeded();
    }
}
