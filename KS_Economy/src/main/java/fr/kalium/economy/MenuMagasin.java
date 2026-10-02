package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.economy.Magasins.Magasin;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
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
 */
final class MenuMagasin implements Listener, TabExecutor {

    private static final int PAR_PAGE = 10;

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
    private final Map<UUID, Gestion> gestions = new HashMap<>();

    MenuMagasin(KSEconomy plugin, Magasins magasins, Boutiques boutiques) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.boutiques = boutiques;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private void message(Player joueur, Component texte) {
        gui.notice(joueur, t("magasin.titre", "<gold><bold>Magasin"), texte, this::ouvrir);
        lang.saveIfNeeded();
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
        choix.removeIf(c -> !c.startsWith(args[0].toLowerCase()));
        return choix;
    }

    // ------------------------------------------------------------------ mon magasin

    void ouvrir(Player joueur) {
        Magasin m = magasins.magasins.get(joueur.getUniqueId());
        if (m == null) {
            gui.open(joueur, t("magasin.titre", "<gold><bold>Magasin"),
                    List.of(t("magasin.aucun", "<white>Tu n'as pas encore de magasin."),
                            t("magasin.aide-creer", "<gray>Place-toi dans un de tes claims de la zone des magasins, puis "
                                    + "« Créer mon magasin » (ou /magasin create) : <prix> ou <blocs> blocs d'émeraude "
                                    + "compressés tier 3.", "prix", KSEconomy.points(magasins.prixCreation()), "blocs",
                                    magasins.blocsCreation())),
                    List.of(), List.of(gui.button(t("magasin.bouton-creer", "<green>Créer mon magasin"), null,
                            this::creer)), gui.close(), 1);
            lang.saveIfNeeded();
            return;
        }
        List<Boutique> mes = magasins.boutiquesDe(joueur.getUniqueId());
        List<Component> corps = new ArrayList<>();
        corps.add(t("magasin.nom", "<white><nom>", "nom", m.nom));
        if (!m.description.isBlank()) {
            corps.add(t("magasin.description", "<gray><description>", "description", m.description));
        }
        corps.add(t("magasin.boutiques", "<white>Boutiques : <nombre> / <max> ; claims : <claims>", "nombre", mes.size(),
                "max", magasins.boutiquesMax(m), "claims", m.claims.size()));
        int attente = magasins.placesEnAttente(joueur.getUniqueId());
        if (attente > 0) {
            corps.add(t("magasin.places-attente", "<gold><attente> place(s) encore prise(s) par des boutiques "
                    + "supprimées : prochaine libre dans <delai>.", "attente", attente,
                    "delai", Boutiques.duree(magasins.avantLiberation(joueur.getUniqueId()))));
        }
        corps.add(t("magasin.aide-2", "<gray>Pose un panneau sur un coffre (en cuivre compris), un tonneau ou une "
                + "shulker dans un claim du magasin pour créer une boutique. /magasin agrandir dans un autre de tes "
                + "claims de la zone : <prix>, +1 boutique.", "prix", KSEconomy.points(magasins.prixAgrandissement())));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("magasin.bouton-boutiques", "<white>Mes boutiques"), null, p -> mesBoutiques(p, 0)));
        boutons.add(gui.button(t("magasin.bouton-fiche", "<white>Nom et description"), null, this::fiche));
        boutons.add(gui.button(t("magasin.bouton-position", "<white>Position ici"), null, this::position));
        boutons.add(gui.button(t("magasin.bouton-agrandir", "<white>Agrandir ici"), null, this::agrandir));
        gui.open(joueur, t("magasin.titre", "<gold><bold>Magasin"), corps, List.of(), boutons, gui.close(), 2);
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
            message(joueur, t("magasin.deja", "<red>Tu as déjà un magasin (un seul par joueur)."));
            return;
        }
        Chunk chunk = joueur.getLocation().getChunk();
        Component refus = refusZone(joueur, chunk);
        if (refus == null && magasins.magasinDuChunk(chunk) != null) {
            refus = t("magasin.chunk-pris", "<red>Ce claim fait déjà partie d'un magasin.");
        }
        if (refus != null) {
            message(joueur, refus);
            return;
        }
        int blocs = Boutiques.compter(joueur.getInventory().getStorageContents(), KSEconomy.creerBloc(3));
        gui.open(joueur, t("magasin.titre-creer", "<gold><bold>Créer mon magasin"),
                List.of(t("magasin.creer-texte", "<white>Le magasin est créé sur ce claim. Paiement : <gray>(solde : "
                        + "<solde> ; blocs tier 3 : <blocs>)", "solde", KSEconomy.points(KSEconomy.solde(joueur.getUniqueId())),
                        "blocs", blocs)),
                List.of(), List.of(
                        gui.button(t("magasin.payer-points", "<green>Payer <prix>", "prix",
                                KSEconomy.points(magasins.prixCreation())), null, p -> payerCreation(p, chunk, false)),
                        gui.button(t("magasin.payer-blocs", "<green>Payer <blocs> blocs tier 3", "blocs",
                                magasins.blocsCreation()), null, p -> payerCreation(p, chunk, true))),
                gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void payerCreation(Player joueur, Chunk chunk, boolean enBlocs) {
        if (magasins.magasins.containsKey(joueur.getUniqueId()) || refusZone(joueur, chunk) != null
                || magasins.magasinDuChunk(chunk) != null) {
            message(joueur, t("magasin.plus-possible", "<red>Impossible de créer le magasin ici."));
            return;
        }
        if (enBlocs) {
            ItemStack modele = KSEconomy.creerBloc(3);
            if (Boutiques.compter(joueur.getInventory().getStorageContents(), modele) < magasins.blocsCreation()) {
                message(joueur, t("magasin.pas-assez-blocs", "<red>Il te faut <blocs> blocs d'émeraude compressés tier 3.",
                        "blocs", magasins.blocsCreation()));
                return;
            }
            Boutiques.piles(modele, magasins.blocsCreation()).forEach(p -> joueur.getInventory().removeItem(p));
        } else if (!KSEconomy.debiter(joueur.getUniqueId(), magasins.prixCreation())) {
            message(joueur, t("magasin.solde", "<red>Solde insuffisant : il faut <prix>.", "prix",
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
        message(joueur, t("magasin.cree", "<green>Magasin créé ! Pose un panneau sur un contenant de ce claim pour créer "
                + "ta première boutique."));
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
            message(joueur, t("magasin.aucun", "<white>Tu n'as pas encore de magasin."));
            return;
        }
        Chunk chunk = joueur.getLocation().getChunk();
        Component refus = refusZone(joueur, chunk);
        if (refus == null && magasins.magasinDuChunk(chunk) != null) {
            refus = t("magasin.chunk-pris", "<red>Ce claim fait déjà partie d'un magasin.");
        }
        if (refus != null) {
            message(joueur, refus);
            return;
        }
        gui.confirm(joueur, t("magasin.titre-agrandir", "<gold><bold>Agrandir mon magasin"),
                t("magasin.agrandir-texte", "<white>Ajouter ce claim au magasin (+1 boutique) pour <yellow><prix></yellow> ?",
                        "prix", KSEconomy.points(magasins.prixAgrandissement())),
                p -> {
                    if (magasins.magasinDuChunk(chunk) != null || refusZone(p, chunk) != null) {
                        message(p, t("magasin.agrandir-impossible", "<red>Impossible d'ajouter ce claim au magasin."));
                        return;
                    }
                    if (!KSEconomy.debiter(p.getUniqueId(), magasins.prixAgrandissement())) {
                        message(p, t("magasin.solde", "<red>Solde insuffisant : il faut <prix>.", "prix",
                                KSEconomy.points(magasins.prixAgrandissement())));
                        return;
                    }
                    m.claims.add(Magasins.cleChunk(chunk));
                    m.agrandissements++;
                    magasins.sauver();
                    message(p, t("magasin.agrandi", "<green>Claim ajouté au magasin : <max> boutiques au plus.", "max",
                            magasins.boutiquesMax(m)));
                }, this::ouvrir);
        lang.saveIfNeeded();
    }

    private void position(Player joueur) {
        Magasin m = magasins.magasins.get(joueur.getUniqueId());
        if (m == null) {
            message(joueur, t("magasin.aucun", "<white>Tu n'as pas encore de magasin."));
            return;
        }
        if (!m.claims.contains(Magasins.cleChunk(joueur.getLocation().getChunk()))) {
            message(joueur, t("magasin.position-hors", "<red>Place-toi dans un claim de ton magasin."));
            return;
        }
        m.position = joueur.getLocation();
        magasins.sauver();
        message(joueur, t("magasin.position-ok", "<green>Position du magasin enregistrée."));
    }

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
        gui.open(joueur, t("magasin.titre-fiche", "<gold><bold>Nom et description"), List.of(),
                List.of(gui.text("nom", t("magasin.champ-nom", "Nom (32 caractères)"), m.nom, 32),
                        gui.text("description", t("magasin.champ-description", "Description (100 caractères)"),
                                m.description, 100)),
                List.of(enregistrer, gui.button(t("magasin.retour", "<gray>Retour"), null, this::ouvrir)), gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ mes boutiques

    private Component offre(Boutique b) {
        return Boutiques.lot(b.quantite, b.objet).append(Component.text(" contre ")).append(boutiques.prix(b))
                .append(Component.text(" - ")).append(boutiques.etatTexte(b));
    }

    /** Ligne d'une boutique dans une liste : nom, offre et état. */
    private Component ligne(Boutique b) {
        return Component.text(Boutiques.nomBoutique(b) + " : ").append(offre(b));
    }

    private void mesBoutiques(Player joueur, int page) {
        List<Boutique> mes = magasins.boutiquesDe(joueur.getUniqueId());
        int pages = Math.max(1, (mes.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = p * PAR_PAGE; i < Math.min(mes.size(), (p + 1) * PAR_PAGE); i++) {
            Boutique b = mes.get(i);
            Component l = ligne(b);
            if (b.pointsEnAttente > 0) {
                l = l.append(t("magasin.points-a-recuperer", "<yellow> (<points> à récupérer)", "points",
                        KSEconomy.points(b.pointsEnAttente)));
            }
            corps.add(l);
            boutons.add(gui.button(boutiques.boutonBoutique(b), boutiques.etatTexte(b), j -> gererBoutique(j, b)));
        }
        if (mes.isEmpty()) {
            corps.add(t("magasin.aucune-boutique", "<gray>Aucune boutique pour l'instant."));
        }
        if (p > 0) {
            boutons.add(gui.button(t("magasin.precedent", "<yellow>Page précédente"), null, j -> mesBoutiques(j, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("magasin.suivant", "<yellow>Page suivante"), null, j -> mesBoutiques(j, p + 1)));
        }
        boutons.add(gui.button(t("magasin.retour", "<gray>Retour"), null, this::ouvrir));
        gui.open(joueur, t("magasin.titre-boutiques", "<gold><bold>Mes boutiques"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    /** Gestion d'une boutique par son propriétaire (clic sur son panneau, ou depuis « Mes boutiques »). */
    void gererBoutique(Player joueur, Boutique b) {
        if (!magasins.boutiques.containsKey(b.id)) {
            ouvrir(joueur);
            return;
        }
        List<Component> corps = List.of(t("achat.nom", "<gold><bold><nom>", "nom", Boutiques.nomBoutique(b)), offre(b),
                t("magasin.points-attente", "<white>Points en attente : <yellow><points>", "points",
                        KSEconomy.points(b.pointsEnAttente)));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("magasin.bouton-stock", "<white>Gérer le stock"), null, p -> ouvrirStock(p, b)));
        boutons.add(gui.button(t("magasin.bouton-renommer", "<white>Renommer"), null, p -> renommer(p, b)));
        boutons.add(b.fermee
                ? gui.button(t("magasin.bouton-rouvrir", "<green>Rouvrir la boutique"), null, p -> fermer(p, b, false))
                : gui.button(t("magasin.bouton-fermer", "<gold>Fermer temporairement"),
                        t("magasin.fermer-info", "<gray>Plus aucun achat jusqu'à la réouverture"), p -> fermer(p, b, true)));
        if (b.pointsEnAttente > 0) {
            boutons.add(gui.button(t("magasin.bouton-points", "<green>Récupérer les points"), null, p -> {
                long points = b.pointsEnAttente;
                b.pointsEnAttente = 0;
                magasins.sauver();
                KSEconomy.crediter(p.getUniqueId(), points);
                gererBoutique(p, b);
            }));
        }
        boutons.add(gui.button(t("magasin.bouton-supprimer", "<red>Supprimer la boutique"), null,
                p -> gui.confirm(p, t("magasin.titre-supprimer", "<red><bold>Supprimer la boutique"),
                        t("magasin.supprimer-texte-2", "<white>Le panneau est retiré et rendu ; le contenu du contenant "
                                + "et les points en attente te restent. <gold>La place de cette boutique reste prise "
                                + "<delai> (pour la fermer un moment, utilise plutôt « Fermer temporairement »).",
                                "delai", Boutiques.duree(magasins.delaiSuppressionMs())),
                        q -> {
                            if (magasins.boutiques.containsKey(b.id)) {
                                boutiques.supprimer(b, q, true);
                            }
                            mesBoutiques(q, 0);
                        }, q -> gererBoutique(q, b))));
        gui.open(joueur, t("magasin.titre-gerer", "<gold><bold>Ma boutique"), corps, List.of(), boutons, gui.close(), 1);
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
        gui.open(joueur, t("magasin.titre-renommer", "<gold><bold>Nom de la boutique"),
                List.of(t("magasin.renommer-aide", "<gray>Vide : le nom de l'objet vendu.")),
                List.of(gui.text("nom", t("boutique.champ-nom-boutique", "Nom de la boutique (20 caractères)"),
                        Boutiques.couper(Boutiques.nomBoutique(b), Magasins.NOM_MAX), Magasins.NOM_MAX)),
                List.of(enregistrer, gui.button(t("magasin.retour", "<gray>Retour"), null, p -> gererBoutique(p, b))),
                gui.close(), 1);
        lang.saveIfNeeded();
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
            message(joueur, t("boutique.verrouillee", "<red>Le propriétaire gère cette boutique : réessaie dans un instant."));
            return;
        }
        Chunk chunk = b.panneau.getChunk();
        chunk.addPluginChunkTicket(plugin);
        Inventory stock = Magasins.inventaire(Magasins.contenantDu(b.panneau.getBlock()));
        if (stock == null) {
            chunk.removePluginChunkTicket(plugin);
            message(joueur, t("achat.ferme", "<red>Boutique fermée (contenant disparu)."));
            return;
        }
        // Le contenant ne doit pas être ouvert par quelqu'un d'autre en ce moment.
        if (!stock.getViewers().isEmpty()) {
            chunk.removePluginChunkTicket(plugin);
            message(joueur, t("magasin.contenant-ouvert", "<red>Quelqu'un regarde ce contenant : réessaie dans un instant."));
            return;
        }
        Gestion g = new Gestion();
        g.boutique = b;
        g.chunk = chunk;
        g.copie = Bukkit.createInventory(g, stock.getSize(), t("magasin.titre-stock", "Stock de la boutique"));
        g.copie.setContents(copieContenu(stock.getContents()));
        magasins.verrous.add(Magasins.cleVerrou(b));
        gestions.put(joueur.getUniqueId(), g);
        joueur.openInventory(g.copie);
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

    void catalogue(Player joueur, int page) {
        List<Magasin> liste = new ArrayList<>(magasins.magasins.values());
        int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = p * PAR_PAGE; i < Math.min(liste.size(), (p + 1) * PAR_PAGE); i++) {
            Magasin m = liste.get(i);
            boutons.add(gui.button(Component.text(m.nom), t("catalogue.info", "<gray>de <proprio>", "proprio",
                    Boutiques.nomJoueur(m.proprio)), j -> magasinPublic(j, m.proprio)));
        }
        if (p > 0) {
            boutons.add(gui.button(t("magasin.precedent", "<yellow>Page précédente"), null, j -> catalogue(j, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("magasin.suivant", "<yellow>Page suivante"), null, j -> catalogue(j, p + 1)));
        }
        boutons.add(gui.button(t("catalogue.mon-magasin", "<white>Mon magasin"), null, this::ouvrir));
        List<Component> corps = liste.isEmpty() ? List.of(t("catalogue.vide", "<gray>Aucun magasin pour l'instant."))
                : List.of(t("catalogue.aide", "<gray>Choisis un magasin pour voir ses boutiques et acheter à distance."));
        gui.open(joueur, t("catalogue.titre", "<gold><bold>Magasins"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void magasinPublic(Player joueur, UUID proprio) {
        Magasin m = magasins.magasins.get(proprio);
        if (m == null) {
            catalogue(joueur, 0);
            return;
        }
        List<Boutique> liste = magasins.boutiquesDe(proprio);
        List<Component> corps = new ArrayList<>();
        corps.add(t("catalogue.proprio", "<white>Magasin de <proprio>", "proprio", Boutiques.nomJoueur(proprio)));
        if (!m.description.isBlank()) {
            corps.add(t("magasin.description", "<gray><description>", "description", m.description));
        }
        if (m.position != null) {
            Location l = m.position;
            corps.add(t("catalogue.position", "<gray>Position : x <x>, y <y>, z <z>", "x", l.getBlockX(), "y",
                    l.getBlockY(), "z", l.getBlockZ()));
        }
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = 0; i < liste.size(); i++) {
            Boutique b = liste.get(i);
            corps.add(ligne(b));
            boutons.add(gui.button(boutiques.boutonBoutique(b), boutiques.etatTexte(b), j -> boutiques.ouvrirAchat(j, b)));
        }
        boutons.add(gui.button(t("magasin.retour", "<gray>Retour"), null, j -> catalogue(j, 0)));
        gui.open(joueur, Component.text(m.nom), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }
}
