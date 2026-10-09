package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.economy.Magasins.Magasin;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.1.0 - /magasin (créer, agrandir, position, fiche), catalogue des magasins (menu Économie) et gestion à distance des
 * boutiques (contenu ouvert dans une copie ; boutique verrouillée pendant ce temps ; chunk chargé).
 * 1.1.2 : boutiques par leur nom (couleur selon l'état : rupture, coffre plein, fermée), renommer, fermer / rouvrir,
 * suppression : panneau retiré et rendu, place prise 3 h.
 * 1.1.3 : le catalogue ne montre pas son propre magasin ; « Signaler le magasin » ; /magasin signalements (staff).
 * 1.2.0 : statistiques de ventes (magasin, boutique) ; catalogue classé par ventes de la semaine, « Rechercher un
 * objet » et « Favoris » ; magasin en favori.
 * 1.5.0 (LeKiwi06, 09/10/2026) : menus en coffres (voir Menus) : mon magasin, mes boutiques (l'objet vendu par case),
 * gestion d'une boutique, catalogue (tête du propriétaire par case), magasin d'un joueur, favoris. Restent en fenêtre
 * de Gui les saisies de texte : nom et description du magasin, nom d'une boutique.
 */
final class MenuMagasin implements Listener, TabExecutor {

    /** Contenu d'une boutique ouvert à distance : copie, recopiée dans le contenant à la fermeture. */
    private static final class Gestion implements InventoryHolder {
        Boutique boutique;
        Inventory copie;
        Chunk chunk;

        @Override
        public Inventory getInventory() {
            return copie;
        }
    }

    private final KSEconomy plugin;
    private final Magasins magasins;
    private final Boutiques boutiques;
    private final Lang lang;
    private final Gui gui;
    private final Menus menus;
    private final Map<UUID, Gestion> gestions = new HashMap<>();

    MenuMagasin(KSEconomy plugin, Magasins magasins, Boutiques boutiques) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.boutiques = boutiques;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.menus = plugin.menus();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    /** Refus : message court, l'écran ouvert ne change pas. */
    private void refus(Player joueur, Component texte) {
        menus.message(joueur, texte, true);
    }

    /** Réussite : retour à « Mon magasin », puis message court. */
    private void fait(Player joueur, Component texte) {
        ouvrir(joueur);
        menus.message(joueur, texte, false);
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase();
        switch (action) {
            case "create", "creer", "créer" -> creer(joueur);
            case "agrandir" -> agrandir(joueur);
            case "position" -> position(joueur);
            case "signalements" -> {
                if (Signalements.staff(joueur)) {
                    plugin.signalements().ouvrirStaff(joueur, false, 0, null);
                } else {
                    ouvrir(joueur);
                }
            }
            default -> ouvrir(joueur);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> choix = new ArrayList<>(List.of("create", "agrandir", "position"));
        if (sender instanceof Player p && Signalements.staff(p)) {
            choix.add("signalements");
        }
        choix.removeIf(c -> !c.startsWith(args[0].toLowerCase()));
        return choix;
    }

    // ------------------------------------------------------------------ mon magasin

    /** Bouton « Magasins » de la barre d'actions : le catalogue. */
    private void boutonCatalogue(Contenant c) {
        c.poser(c.bas(4), Menus.objet(Material.OAK_HANGING_SIGN, t("magasin.bouton-catalogue", "<gold>Magasins"),
                t("magasin.catalogue-info", "<gray>Les magasins des autres joueurs")), p -> catalogue(p, 0));
    }

    /** 1.5.0 : coffre ; sans magasin : comment en créer un ; sinon sa fiche et ses actions. */
    void ouvrir(Player joueur) {
        Magasin m = magasins.magasins.get(joueur.getUniqueId());
        if (m == null) {
            Contenant c = menus.menu(joueur, 4, t("magasin.titre-coffre", "<dark_gray>Mon magasin"), "magasin-aucun");
            c.poser(11, Menus.objet(Material.PAPER, t("magasin.aucun", "<white>Tu n'as pas encore de magasin."),
                    t("magasin.aide-creer", "<gray>Place-toi dans un de tes claims de la zone des magasins, puis "
                            + "« Créer mon magasin » (ou /magasin create) : <prix> ou <blocs> blocs d'émeraude "
                            + "compressés tier 3.", "prix", KSEconomy.points(magasins.prixCreation()), "blocs",
                            magasins.blocsCreation())), null);
            c.poser(15, Menus.objet(Material.EMERALD_BLOCK, t("magasin.bouton-creer", "<green>Créer mon magasin"),
                    t("magasin.creer-info", "<gray>Sur le claim où tu te trouves")), this::creer);
            boutonCatalogue(c);
            menus.sortie(c, null);
            c.ouvrir(joueur);
            lang.saveIfNeeded();
            return;
        }
        List<Boutique> mes = magasins.boutiquesDe(joueur.getUniqueId());
        Contenant c = menus.menu(joueur, 5, t("magasin.titre-coffre", "<dark_gray>Mon magasin"), "magasin");
        List<Component> fiche = new ArrayList<>();
        if (!m.description.isBlank()) {
            fiche.addAll(Menus.lignes(t("magasin.description", "<gray><description>", "description", m.description)));
        }
        fiche.addAll(Menus.lignes(
                t("magasin.fiche-boutiques", "<white>Boutiques : <nombre> / <max>", "nombre", mes.size(), "max",
                        magasins.boutiquesMax(m)),
                t("magasin.fiche-claims", "<white>Claims : <claims>", "claims", m.claims.size()),
                stats(plugin.ventes().magasin(joueur.getUniqueId()))));
        int attente = magasins.placesEnAttente(joueur.getUniqueId());
        if (attente > 0) {
            fiche.addAll(Menus.lignes(t("magasin.places-attente", "<gold><attente> place(s) encore prise(s) par des "
                            + "boutiques supprimées : prochaine libre dans <delai>.", "attente", attente,
                    "delai", Boutiques.duree(magasins.avantLiberation(joueur.getUniqueId())))));
        }
        c.poser(4, Contenant.objet(Menus.tete(joueur.getUniqueId()), t("magasin.nom-coffre", "<gold><nom>", "nom", m.nom),
                fiche), null);
        long aRecuperer = mes.stream().mapToLong(b -> b.pointsEnAttente).sum();
        c.poser(19, Menus.objet(Material.CHEST, t("magasin.bouton-boutiques", "<white>Mes boutiques"),
                t("magasin.boutiques-info", "<gray>Stock, nom, fermeture, points à récupérer"),
                aRecuperer > 0 ? t("magasin.a-recuperer", "<yellow><points> à récupérer", "points",
                        KSEconomy.points(aRecuperer)) : null), p -> mesBoutiques(p, 0));
        c.poser(21, Menus.objet(Material.NAME_TAG, t("magasin.bouton-fiche", "<white>Nom et description"),
                t("magasin.fiche-info", "<gray>Ce que voient les autres joueurs dans le catalogue")), this::fiche);
        c.poser(23, Menus.objet(Material.COMPASS, t("magasin.bouton-position", "<white>Position ici"),
                t("magasin.position-info", "<gray>La position de ton magasin dans le catalogue devient l'endroit où "
                        + "tu te trouves")), this::position);
        c.poser(25, Menus.objet(Material.GOLDEN_SHOVEL, t("magasin.bouton-agrandir", "<white>Agrandir ici"),
                t("magasin.agrandir-info", "<gray>Ajoute le claim où tu te trouves : <prix>, +1 boutique", "prix",
                        KSEconomy.points(magasins.prixAgrandissement()))), this::agrandir);
        menus.aide(c, t("magasin.aide-titre", "<aqua>Créer une boutique"),
                t("magasin.aide-2", "<gray>Pose un panneau sur un coffre (en cuivre compris), un tonneau ou une "
                        + "shulker dans un claim du magasin pour créer une boutique. /magasin agrandir dans un autre de "
                        + "tes claims de la zone : <prix>, +1 boutique.", "prix",
                        KSEconomy.points(magasins.prixAgrandissement())));
        boutonCatalogue(c);
        menus.sortie(c, null);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Ce chunk est-il un claim du joueur dans la zone des magasins ? Message d'erreur, ou null. */
    private Component refusZone(Player joueur, Chunk chunk) {
        if (!joueur.getUniqueId().equals(Magasins.proprioDuClaim(chunk))) {
            return t("magasin.pas-ton-claim", "<red>Place-toi dans un de tes claims.");
        }
        if (!magasins.dansLaZone(chunk)) {
            return t("magasin.hors-zone", "<red>Ce claim n'est pas dans la zone des magasins.");
        }
        return null;
    }

    private void creer(Player joueur) {
        if (magasins.magasins.containsKey(joueur.getUniqueId())) {
            refus(joueur, t("magasin.deja", "<red>Tu as déjà un magasin (un seul par joueur)."));
            return;
        }
        Chunk chunk = joueur.getLocation().getChunk();
        Component refus = refusZone(joueur, chunk);
        if (refus == null && magasins.magasinDuChunk(chunk) != null) {
            refus = t("magasin.chunk-pris", "<red>Ce claim fait déjà partie d'un magasin.");
        }
        if (refus != null) {
            refus(joueur, refus);
            return;
        }
        ItemStack bloc = KSEconomy.creerBloc(3);
        int blocs = Boutiques.compter(joueur.getInventory().getStorageContents(), bloc);
        Contenant c = menus.menu(joueur, 4, t("magasin.titre-creer-coffre", "<dark_gray>Créer mon magasin"),
                "magasin-creer");
        c.poser(4, Menus.objet(Material.PAPER, t("magasin.creer-nom", "<gold>Créer mon magasin"),
                t("magasin.creer-ici", "<white>Le magasin est créé sur ce claim. Choisis ton paiement.")), null);
        c.poser(11, Menus.objet(Material.EMERALD, t("magasin.payer-points", "<green>Payer <prix>", "prix",
                        KSEconomy.points(magasins.prixCreation())),
                t("eco.solde", "<white>Solde : <green><solde>", "solde",
                        KSEconomy.points(KSEconomy.solde(joueur.getUniqueId())))),
                p -> payerCreation(p, chunk, false));
        c.poser(15, Menus.objet(Menus.telQuel(bloc, magasins.blocsCreation()),
                t("magasin.payer-blocs", "<green>Payer <blocs> blocs tier 3", "blocs", magasins.blocsCreation()),
                t("magasin.blocs-possedes", "<white>Dans ton inventaire : <blocs>", "blocs", blocs)),
                p -> payerCreation(p, chunk, true));
        menus.sortie(c, this::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private void payerCreation(Player joueur, Chunk chunk, boolean enBlocs) {
        if (magasins.magasins.containsKey(joueur.getUniqueId()) || refusZone(joueur, chunk) != null
                || magasins.magasinDuChunk(chunk) != null) {
            refus(joueur, t("magasin.plus-possible", "<red>Impossible de créer le magasin ici."));
            return;
        }
        if (enBlocs) {
            ItemStack modele = KSEconomy.creerBloc(3);
            if (Boutiques.compter(joueur.getInventory().getStorageContents(), modele) < magasins.blocsCreation()) {
                refus(joueur, t("magasin.pas-assez-blocs", "<red>Il te faut <blocs> blocs d'émeraude compressés tier 3.",
                        "blocs", magasins.blocsCreation()));
                return;
            }
            Boutiques.piles(modele, magasins.blocsCreation()).forEach(p -> joueur.getInventory().removeItem(p));
        } else if (!KSEconomy.debiter(joueur.getUniqueId(), magasins.prixCreation())) {
            refus(joueur, t("magasin.solde", "<red>Solde insuffisant : il faut <prix>.", "prix",
                    KSEconomy.points(magasins.prixCreation())));
            return;
        }
        Magasin m = new Magasin();
        m.proprio = joueur.getUniqueId();
        m.nom = nomLibre("Magasin de " + joueur.getName());
        m.position = joueur.getLocation();
        m.claims.add(Magasins.cleChunk(chunk));
        magasins.magasins.put(m.proprio, m);
        magasins.sauver();
        fait(joueur, t("magasin.cree-court", "<green>Magasin créé ! Pose un panneau sur un contenant de ce claim."));
    }

    private String nomLibre(String voulu) {
        String nom = voulu.length() > 32 ? voulu.substring(0, 32) : voulu;
        int n = 2;
        String essai = nom;
        while (nomPris(essai, null)) {
            essai = (nom.length() > 28 ? nom.substring(0, 28) : nom) + " " + n++;
        }
        return essai;
    }

    private boolean nomPris(String nom, UUID sauf) {
        for (Magasin m : magasins.magasins.values()) {
            if (!m.proprio.equals(sauf) && m.nom.equalsIgnoreCase(nom)) {
                return true;
            }
        }
        return false;
    }

    private void agrandir(Player joueur) {
        Magasin m = magasins.magasins.get(joueur.getUniqueId());
        if (m == null) {
            refus(joueur, t("magasin.aucun", "<white>Tu n'as pas encore de magasin."));
            return;
        }
        Chunk chunk = joueur.getLocation().getChunk();
        Component refus = refusZone(joueur, chunk);
        if (refus == null && magasins.magasinDuChunk(chunk) != null) {
            refus = t("magasin.chunk-pris", "<red>Ce claim fait déjà partie d'un magasin.");
        }
        if (refus != null) {
            refus(joueur, refus);
            return;
        }
        menus.confirmer(joueur, t("magasin.titre-agrandir-coffre", "<dark_gray>Agrandir mon magasin"),
                t("magasin.agrandir-texte", "<white>Ajouter ce claim au magasin (+1 boutique) pour <yellow><prix></yellow> ?",
                        "prix", KSEconomy.points(magasins.prixAgrandissement())),
                p -> {
                    if (magasins.magasinDuChunk(chunk) != null || refusZone(p, chunk) != null) {
                        ouvrir(p);
                        refus(p, t("magasin.agrandir-impossible", "<red>Impossible d'ajouter ce claim au magasin."));
                        return;
                    }
                    if (!KSEconomy.debiter(p.getUniqueId(), magasins.prixAgrandissement())) {
                        ouvrir(p);
                        refus(p, t("magasin.solde", "<red>Solde insuffisant : il faut <prix>.", "prix",
                                KSEconomy.points(magasins.prixAgrandissement())));
                        return;
                    }
                    m.claims.add(Magasins.cleChunk(chunk));
                    m.agrandissements++;
                    magasins.sauver();
                    fait(p, t("magasin.agrandi", "<green>Claim ajouté au magasin : <max> boutiques au plus.", "max",
                            magasins.boutiquesMax(m)));
                }, this::ouvrir);
    }

    private void position(Player joueur) {
        Magasin m = magasins.magasins.get(joueur.getUniqueId());
        if (m == null) {
            refus(joueur, t("magasin.aucun", "<white>Tu n'as pas encore de magasin."));
            return;
        }
        if (!m.claims.contains(Magasins.cleChunk(joueur.getLocation().getChunk()))) {
            refus(joueur, t("magasin.position-hors", "<red>Place-toi dans un claim de ton magasin."));
            return;
        }
        m.position = joueur.getLocation();
        magasins.sauver();
        menus.message(joueur, t("magasin.position-ok", "<green>Position du magasin enregistrée."), false);
    }

    /** Nom et description : saisie de texte, donc une fenêtre de Gui (ouverte depuis le coffre). */
    private void fiche(Player joueur) {
        Magasin m = magasins.magasins.get(joueur.getUniqueId());
        if (m == null) {
            ouvrir(joueur);
            return;
        }
        ActionButton enregistrer = gui.form(t("magasin.enregistrer", "<green>Enregistrer"), null, (p, vue) -> {
            String nom = vue.getText("nom") == null ? "" : vue.getText("nom").trim();
            String description = vue.getText("description") == null ? "" : vue.getText("description").trim();
            if (nom.isEmpty() || nom.length() > 32 || nomPris(nom, p.getUniqueId())) {
                gui.notice(p, t("magasin.titre", "<gold><bold>Magasin"),
                        t("magasin.nom-pris", "<red>Nom vide ou déjà pris."), this::fiche);
                return;
            }
            m.nom = nom;
            m.description = description.length() > 100 ? description.substring(0, 100) : description;
            magasins.sauver();
            ouvrir(p);
        });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("magasin.titre-fiche", "<gold><bold>Nom et description"), List.of(),
                    List.of(gui.text("nom", t("magasin.champ-nom", "Nom (32 caractères)"), m.nom, 32),
                            gui.text("description", t("magasin.champ-description", "Description (100 caractères)"),
                                    m.description, 100)),
                    List.of(enregistrer, gui.button(t("magasin.retour", "<gray>Retour"), null, this::ouvrir)),
                    gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    // ------------------------------------------------------------------ mes boutiques

    /** 1.2.0 : statistiques de ventes (lots, semaine, gains). */
    private Component stats(Ventes.Stats st) {
        return t("ventes.stats", "<gray>Ventes : <white><lots7></white> lot(s) cette semaine, <white><lots></white> au total "
                        + "(<ventes> vente(s)) ; reçu : <white><points></white><objets>", "lots7", st.lots7j(), "lots", st.lots(),
                "ventes", st.ventes(), "points", KSEconomy.points(st.points()),
                "objets", st.objets() > 0 ? " et " + st.objets() + " objet(s) en paiement" : "");
    }

    /** 1.5.0 : coffre ; une boutique par case (l'objet vendu, nom coloré selon l'état), 45 par page. */
    private void mesBoutiques(Player joueur, int page) {
        List<Boutique> mes = magasins.boutiquesDe(joueur.getUniqueId());
        int pages = Math.max(1, (mes.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant c = menus.menu(joueur, 6, t("magasin.titre-boutiques-coffre", "<dark_gray>Mes boutiques"),
                "mes-boutiques");
        for (int i = p * Contenant.PAR_PAGE; i < Math.min(mes.size(), (p + 1) * Contenant.PAR_PAGE); i++) {
            Boutique b = mes.get(i);
            c.poser(i - p * Contenant.PAR_PAGE, boutiques.icone(b,
                    b.pointsEnAttente > 0 ? t("magasin.a-recuperer", "<yellow><points> à récupérer", "points",
                            KSEconomy.points(b.pointsEnAttente)) : null,
                    t("magasin.clic-gerer", "<dark_gray>Clic : gérer")), j -> gererBoutique(j, b));
        }
        if (mes.isEmpty()) {
            c.poser(22, Menus.objet(Material.PAPER, t("magasin.aucune-boutique", "<gray>Aucune boutique pour l'instant."),
                    t("magasin.aide-panneau", "<dark_gray>Pose un panneau sur un coffre, un tonneau ou une shulker "
                            + "dans un claim de ton magasin.")), null);
        }
        menus.pages(c, p, pages, this::mesBoutiques);
        menus.sortie(c, this::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Gestion d'une boutique par son propriétaire (clic sur son panneau, ou depuis « Mes boutiques »). */
    void gererBoutique(Player joueur, Boutique b) {
        if (!magasins.boutiques.containsKey(b.id)) {
            ouvrir(joueur);
            return;
        }
        Contenant c = menus.menu(joueur, 5, t("magasin.titre-gerer-coffre", "<dark_gray>Ma boutique"), "ma-boutique");
        c.poser(4, boutiques.icone(b,
                t("magasin.points-attente", "<white>Points en attente : <yellow><points>", "points",
                        KSEconomy.points(b.pointsEnAttente)), stats(plugin.ventes().boutique(b.id))), null);
        c.poser(19, Menus.objet(Material.CHEST, t("magasin.bouton-stock", "<white>Gérer le stock"),
                t("magasin.stock-info", "<gray>Ouvre le contenant de la boutique, d'où que tu sois")),
                p -> ouvrirStock(p, b));
        c.poser(20, Menus.objet(Material.NAME_TAG, t("magasin.bouton-renommer", "<white>Renommer"),
                t("magasin.renommer-info", "<gray>Le nom écrit sur le panneau et dans les menus")),
                p -> renommer(p, b));
        if (b.fermee) {
            c.poser(21, Contenant.objet(Material.LIME_DYE, t("magasin.bouton-rouvrir", "<green>Rouvrir la boutique"),
                    List.of()), p -> fermer(p, b, false));
        } else {
            c.poser(21, Menus.objet(Material.RED_DYE, t("magasin.bouton-fermer", "<gold>Fermer temporairement"),
                    t("magasin.fermer-info", "<gray>Plus aucun achat jusqu'à la réouverture")), p -> fermer(p, b, true));
        }
        if (b.pointsEnAttente > 0) {
            c.poser(23, Menus.objet(Material.EMERALD, t("magasin.bouton-points", "<green>Récupérer les points"),
                    t("magasin.a-recuperer", "<yellow><points> à récupérer", "points",
                            KSEconomy.points(b.pointsEnAttente))), p -> {
                long points = b.pointsEnAttente;
                b.pointsEnAttente = 0;
                magasins.sauver();
                KSEconomy.crediter(p.getUniqueId(), points);
                gererBoutique(p, b);
                menus.message(p, t("magasin.points-recuperes", "<green>+ <points>", "points", KSEconomy.points(points)),
                        false);
            });
        }
        c.poser(25, Menus.objet(Material.LAVA_BUCKET, t("magasin.bouton-supprimer", "<red>Supprimer la boutique"),
                t("magasin.supprimer-info", "<gray>Le panneau est retiré et rendu ; une confirmation est demandée")),
                p -> menus.confirmer(p, t("magasin.titre-supprimer-coffre", "<dark_gray>Supprimer la boutique"),
                        t("magasin.supprimer-texte-2", "<white>Le panneau est retiré et rendu ; le contenu du contenant "
                                + "et les points en attente te restent. <gold>La place de cette boutique reste prise "
                                + "<delai> (pour la fermer un moment, utilise plutôt « Fermer temporairement »).",
                                "delai", Boutiques.duree(magasins.delaiSuppressionMs())),
                        q -> {
                            if (magasins.boutiques.containsKey(b.id)) {
                                boutiques.supprimer(b, q, true);
                            }
                            mesBoutiques(q, 0);
                        }, q -> gererBoutique(q, b)));
        menus.sortie(c, p -> mesBoutiques(p, 0));
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** 1.1.2 : nom de la boutique (20 caractères ; vide : nom de l'objet vendu), affiché sur le panneau et les menus. */
    private void renommer(Player joueur, Boutique b) {
        ActionButton enregistrer = gui.form(t("magasin.enregistrer", "<green>Enregistrer"), null, (p, vue) -> {
            if (magasins.boutiques.containsKey(b.id)) {
                b.nom = Boutiques.nettoyerNom(vue.getText("nom"));
                if (b.nom != null && b.nom.equals(Boutiques.texteObjet(b.objet))) {
                    b.nom = null;
                }
                magasins.sauver();
                boutiques.ecrirePanneau(b);
            }
            gererBoutique(p, b);
        });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("magasin.titre-renommer", "<gold><bold>Nom de la boutique"),
                    List.of(t("magasin.renommer-aide", "<gray>Vide : le nom de l'objet vendu.")),
                    List.of(gui.text("nom", t("boutique.champ-nom-boutique", "Nom de la boutique (20 caractères)"),
                            Boutiques.couper(Boutiques.nomBoutique(b), Magasins.NOM_MAX), Magasins.NOM_MAX)),
                    List.of(enregistrer, gui.button(t("magasin.retour", "<gray>Retour"), null, p -> gererBoutique(p, b))),
                    gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    /** 1.1.2 : fermeture temporaire (aucun achat ; le panneau affiche « Fermée ») ou réouverture. */
    private void fermer(Player joueur, Boutique b, boolean fermee) {
        if (magasins.boutiques.containsKey(b.id)) {
            b.fermee = fermee;
            magasins.sauver();
            boutiques.ecrirePanneau(b);
        }
        gererBoutique(joueur, b);
    }

    /** Contenu de la boutique, ouvert de n'importe où : copie verrouillée, recopiée à la fermeture. */
    private void ouvrirStock(Player joueur, Boutique b) {
        if (magasins.verrouillee(b)) {
            refus(joueur, t("boutique.verrouillee", "<red>Le propriétaire gère cette boutique : réessaie dans un instant."));
            return;
        }
        Chunk chunk = b.panneau.getChunk();
        chunk.addPluginChunkTicket(plugin);
        Inventory stock = Magasins.inventaire(Magasins.contenantDu(b.panneau.getBlock()));
        if (stock == null) {
            chunk.removePluginChunkTicket(plugin);
            refus(joueur, t("achat.ferme", "<red>Boutique fermée (contenant disparu)."));
            return;
        }
        // Le contenant ne doit pas être ouvert par quelqu'un d'autre en ce moment.
        if (!stock.getViewers().isEmpty()) {
            chunk.removePluginChunkTicket(plugin);
            refus(joueur, t("magasin.contenant-ouvert", "<red>Quelqu'un regarde ce contenant : réessaie dans un instant."));
            return;
        }
        Gestion g = new Gestion();
        g.boutique = b;
        g.chunk = chunk;
        g.copie = Bukkit.createInventory(g, stock.getSize(), t("magasin.titre-stock", "Stock de la boutique"));
        g.copie.setContents(copieContenu(stock.getContents()));
        magasins.verrous.add(Magasins.cleVerrou(b));
        gestions.put(joueur.getUniqueId(), g);
        // 1.5.0 : ouvert depuis un clic dans un coffre de menu : après le clic (voir Contenant).
        Contenant.apres(() -> joueur.openInventory(g.copie));
    }

    private static ItemStack[] copieContenu(ItemStack[] contenu) {
        ItemStack[] copie = new ItemStack[contenu.length];
        for (int i = 0; i < contenu.length; i++) {
            copie[i] = contenu[i] == null ? null : contenu[i].clone();
        }
        return copie;
    }

    private void fermerGestion(Player joueur) {
        Gestion g = gestions.remove(joueur.getUniqueId());
        if (g == null) {
            return;
        }
        Inventory stock = Magasins.inventaire(Magasins.contenantDu(g.boutique.panneau.getBlock()));
        if (stock != null) {
            stock.setContents(copieContenu(g.copie.getContents()));
        } else {
            // Contenant disparu entre-temps : le contenu revient au propriétaire.
            for (ItemStack objet : g.copie.getContents()) {
                if (objet != null) {
                    for (ItemStack reste : joueur.getInventory().addItem(objet).values()) {
                        joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
                    }
                }
            }
        }
        magasins.verrous.remove(Magasins.cleVerrou(g.boutique));
        boutiques.actualiserPanneaux(Magasins.contenantDu(g.boutique.panneau.getBlock()));
        g.chunk.removePluginChunkTicket(plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Gestion && event.getPlayer() instanceof Player joueur) {
            fermerGestion(joueur);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fermerGestion(event.getPlayer());
    }

    /** Arrêt du serveur : les gestions en cours sont recopiées. */
    void toutFermer() {
        for (UUID joueur : List.copyOf(gestions.keySet())) {
            Player p = Bukkit.getPlayer(joueur);
            if (p != null) {
                fermerGestion(p);
                p.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------ catalogue

    /** L'objet d'un magasin dans une liste : la tête de son propriétaire. */
    private ItemStack iconeMagasin(Magasin m, Component nom, int lotsSemaine) {
        List<Component> lignes = new ArrayList<>(Menus.lignes(
                t("catalogue.info", "<gray>de <proprio>", "proprio", Boutiques.nomJoueur(m.proprio)),
                t("catalogue.ventes", "<gray><lots> lot(s) vendu(s) cette semaine", "lots", lotsSemaine),
                m.description.isBlank() ? null
                        : t("magasin.description", "<gray><description>", "description", m.description),
                t("catalogue.boutiques", "<white><nombre> boutique(s)", "nombre",
                        magasins.boutiquesDe(m.proprio).size()),
                t("catalogue.clic", "<dark_gray>Clic : voir ses boutiques")));
        return Contenant.objet(Menus.tete(m.proprio), nom, lignes);
    }

    /** 1.5.0 : coffre ; un magasin par case (tête du propriétaire), classés par ventes de la semaine, 45 par page. */
    void catalogue(Player joueur, int page) {
        List<Magasin> liste = new ArrayList<>(magasins.magasins.values());
        // 1.1.3 : son propre magasin n'apparaît pas (il est dans « Mon magasin »).
        liste.removeIf(m -> m.proprio.equals(joueur.getUniqueId()));
        // 1.2.0 : classement : les magasins qui ont le plus vendu ces 7 derniers jours d'abord.
        java.util.Map<UUID, Integer> semaine = plugin.ventes().lotsDeLaSemaine();
        liste.sort(java.util.Comparator.comparingInt((Magasin m) -> -semaine.getOrDefault(m.proprio, 0))
                .thenComparing(m -> m.nom.toLowerCase(java.util.Locale.ROOT)));
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant c = menus.menu(joueur, 6, t("catalogue.titre-coffre", "<dark_gray>Magasins"), "catalogue");
        for (int i = p * Contenant.PAR_PAGE; i < Math.min(liste.size(), (p + 1) * Contenant.PAR_PAGE); i++) {
            Magasin m = liste.get(i);
            c.poser(i - p * Contenant.PAR_PAGE, iconeMagasin(m, t("catalogue.nom", "<gold><rang>. <nom>", "rang", i + 1,
                    "nom", m.nom), semaine.getOrDefault(m.proprio, 0)), j -> magasinPublic(j, m.proprio, 0));
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("catalogue.vide", "<gray>Aucun magasin pour l'instant."),
                    List.of()), null);
        }
        menus.pages(c, p, pages, this::catalogue);
        c.poser(c.bas(2), Menus.objet(Material.SPYGLASS, t("catalogue.bouton-recherche", "<green>Rechercher un objet"),
                t("catalogue.recherche-info", "<gray>Toutes les boutiques, avec des filtres")),
                j -> plugin.recherche().ouvrir(j, Recherche.Criteres.parDefaut()));
        c.poser(c.bas(3), Contenant.objet(Material.NETHER_STAR, t("catalogue.bouton-favoris", "<yellow>Favoris (<n>)", "n",
                plugin.favoris().de(joueur.getUniqueId()).size()), List.of()), this::favoris);
        c.poser(c.bas(4), Menus.objet(Material.CHEST, t("catalogue.mon-magasin", "<white>Mon magasin"),
                t("catalogue.mon-magasin-info", "<gray>Tes boutiques, ta fiche, tes points à récupérer")),
                this::ouvrir);
        menus.sortie(c, plugin.menuEconomie()::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** 1.5.0 : coffre ; les boutiques d'un magasin (l'objet vendu), fiche, favori et signalement dans la barre. */
    private void magasinPublic(Player joueur, UUID proprio, int page) {
        Magasin m = magasins.magasins.get(proprio);
        if (m == null) {
            catalogue(joueur, 0);
            return;
        }
        if (proprio.equals(joueur.getUniqueId())) {
            ouvrir(joueur);
            return;
        }
        List<Boutique> liste = magasins.boutiquesDe(proprio);
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant c = menus.menu(joueur, 6, t("catalogue.titre-magasin", "<dark_gray><nom>", "nom", Boutiques.couper(m.nom, 28)),
                "magasin:" + proprio);
        for (int i = p * Contenant.PAR_PAGE; i < Math.min(liste.size(), (p + 1) * Contenant.PAR_PAGE); i++) {
            Boutique b = liste.get(i);
            c.poser(i - p * Contenant.PAR_PAGE, boutiques.icone(b, t("catalogue.clic-acheter", "<dark_gray>Clic : acheter")),
                    j -> boutiques.ouvrirAchat(j, b, q -> magasinPublic(q, proprio, p)));
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("catalogue.sans-boutique", "<gray>Aucune boutique pour "
                    + "l'instant."), List.of()), null);
        }
        menus.pages(c, p, pages, (j, n) -> magasinPublic(j, proprio, n));
        List<Component> fiche = new ArrayList<>(Menus.lignes(
                t("catalogue.proprio", "<white>Magasin de <proprio>", "proprio", Boutiques.nomJoueur(proprio)),
                t("catalogue.ventes", "<gray><lots> lot(s) vendu(s) cette semaine", "lots",
                        plugin.ventes().magasin(proprio).lots7j()),
                m.description.isBlank() ? null
                        : t("magasin.description", "<gray><description>", "description", m.description)));
        if (m.position != null) {
            Location l = m.position;
            fiche.add(t("catalogue.position", "<gray>Position : x <x>, y <y>, z <z>", "x", l.getBlockX(), "y",
                    l.getBlockY(), "z", l.getBlockZ()));
        }
        c.poser(c.bas(3), Contenant.objet(Menus.tete(proprio), t("magasin.nom-coffre", "<gold><nom>", "nom", m.nom),
                fiche), null);
        boolean favori = plugin.favoris().contient(joueur.getUniqueId(), Favoris.magasin(proprio));
        c.poser(c.bas(4), Contenant.objet(favori ? Material.NETHER_STAR : Material.FIREWORK_STAR,
                favori ? t("favoris.retirer", "<yellow>Retirer des favoris")
                        : t("favoris.ajouter", "<yellow>Ajouter aux favoris"), List.of()), j -> {
            plugin.favoris().basculer(j.getUniqueId(), Favoris.magasin(proprio));
            magasinPublic(j, proprio, p);
        });
        menus.sortie(c, j -> catalogue(j, 0));
        c.poser(c.bas(6), Menus.objet(Material.REDSTONE_TORCH, t("catalogue.bouton-signaler", "<red>Signaler le magasin"),
                t("catalogue.signaler-info", "<gray>Arnaque, contenu inapproprié, thème...")),
                j -> plugin.signalements().signalerMagasin(j, m, q -> magasinPublic(q, proprio, p)));
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** 1.2.0 : favoris du joueur (magasins et boutiques) ; ceux qui n'existent plus sont retirés. 1.5.0 : coffre. */
    private void favoris(Player joueur) {
        Contenant c = menus.menu(joueur, 6, t("favoris.titre-coffre", "<dark_gray>Favoris"), "favoris");
        java.util.Map<UUID, Integer> semaine = plugin.ventes().lotsDeLaSemaine();
        int place = 0;
        for (String cle : plugin.favoris().de(joueur.getUniqueId())) {
            if (place >= Contenant.PAR_PAGE) {
                break;
            }
            if (cle.startsWith("m:")) {
                UUID proprio;
                try {
                    proprio = UUID.fromString(cle.substring(2));
                } catch (IllegalArgumentException e) {
                    plugin.favoris().retirer(joueur.getUniqueId(), cle);
                    continue;
                }
                Magasin m = magasins.magasins.get(proprio);
                if (m == null) {
                    plugin.favoris().retirer(joueur.getUniqueId(), cle);
                    continue;
                }
                c.poser(place++, iconeMagasin(m, t("favoris.magasin", "<gold>Magasin : <white><nom>", "nom", m.nom),
                        semaine.getOrDefault(proprio, 0)), j -> magasinPublic(j, proprio, 0));
            } else if (cle.startsWith("b:")) {
                Boutique b = magasins.boutiques.get(cle.substring(2));
                if (b == null) {
                    plugin.favoris().retirer(joueur.getUniqueId(), cle);
                    continue;
                }
                c.poser(place++, boutiques.icone(b, t("catalogue.clic-acheter", "<dark_gray>Clic : acheter")),
                        j -> boutiques.ouvrirAchat(j, b, this::favoris));
            }
        }
        if (place == 0) {
            c.poser(22, Menus.objet(Material.PAPER, t("favoris.aucun", "<gray>Aucun favori"),
                    t("favoris.aucun-info", "<dark_gray>Étoile « Ajouter aux favoris » dans un magasin ou une "
                            + "boutique.")), null);
        }
        menus.sortie(c, j -> catalogue(j, 0));
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }
}
