package fr.kalium.kvmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.kvplots.api.KanvasPlots;
import fr.kalium.kvplots.api.KanvasPlots.PlotInfo;
import fr.kalium.kvplots.api.KanvasPlots.Refus;
import fr.kalium.kvplots.api.Taille;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * KV_Menu (1.0.0) : menus du serveur Kanvas (demande de LeKiwi06, 26/09/2026 : « une interface au lieu de juste avoir
 * les commandes »). Hiérarchie des interfaces : KLM_Menu (catalogue) -> KV_Menu -> actions de KV_Plots.
 *
 * - Accueil : voter pour le plot où l'on se trouve (s'il est noté ou notable), réserver un plot moyen / grand,
 *   mes plots.
 * 1.1.0 : votes, validation / réouverture, points dans la fiche ; l'étoile laisse la place aux terracottas.
 * 1.2.0 : visites (au hasard, par joueur avec les têtes, liste de tous les plots), titre et description.
 * - Mes plots : un bouton par plot (créateur ou éditeur) -> fiche du plot : téléportation, éditeurs, remise à zéro,
 *   suppression (créateur).
 * - Ouverture : étoile du Nether (emplacement 4), /kanvas (/kv, /plots) et le catalogue de KLM_Menu (entrée « Kanvas »).
 */
public final class KvMenu extends JavaPlugin {

    private Lang lang;
    private Gui gui;
    private KanvasPlots plots;
    private TetesJoueurs tetes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        plots = getServer().getServicesManager().load(KanvasPlots.class);
        if (plots == null) {
            getLogger().severe("API de KV_Plots introuvable : KV_Menu désactivé.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getServicesManager().register(MenuSection.class,
                MenuSection.of(this, "kanvas", MenuSection.Audience.PLAYERS,
                        t("klm.home", "<gold><bold>Kanvas"), t("klm.home-tip", "<gray>Plots de construction : réserver, mes plots, éditeurs."),
                        this::accueil),
                this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(new ObjetMenu(this, lang, p -> accueil(p, null)), this);
        tetes = new TetesJoueurs(plots, lang, this::plotsJoueur);
        getServer().getPluginManager().registerEvents(tetes, this);
        lang.saveIfNeeded();
    }

    KanvasPlots plots() {
        return plots;
    }

    @Override
    public void onDisable() {
        if (lang != null) lang.saveIfNeeded();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            accueil(joueur, null);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    private Component t(String key, String def, Object... pairs) {
        return lang.c(key, def, pairs);
    }

    private static String nom(UUID u) {
        String n = Bukkit.getOfflinePlayer(u).getName();
        return n == null ? "?" : n;
    }

    private String nomTaille(Taille taille) {
        return lang.raw("taille." + taille.nom, taille == Taille.GRAND ? "grand" : "moyen");
    }

    /** Affiche le refus dans un petit message, puis revient à l'écran indiqué. */
    private void refus(Player joueur, Refus refus, Consumer<Player> retour) {
        gui.notice(joueur, t("refus.title", "<red><bold>Impossible"), Component.text(refus.getMessage()), retour::accept);
    }

    private ActionButton retour(Consumer<Player> retour) {
        return gui.button(t("menu.back", "<gray>Retour"), null, retour::accept);
    }

    // ------------------------------------------------------------------ accueil

    public void accueil(Player joueur, Consumer<Player> back) {
        Consumer<Player> ici = p -> accueil(p, back);
        UUID u = joueur.getUniqueId();
        List<ActionButton> boutons = new ArrayList<>();
        for (Taille taille : Taille.values()) {
            int occupes = plots.occupes(u, taille), places = plots.places(u, taille);
            String cle = taille == Taille.GRAND ? "home.reserve-large" : "home.reserve-medium";
            Component libelle = taille == Taille.GRAND
                    ? t(cle, "<green>Réserver un grand plot <gray>(<n>/<max>)", "n", occupes, "max", places)
                    : t(cle, "<green>Réserver un plot moyen <gray>(<n>/<max>)", "n", occupes, "max", places);
            boutons.add(gui.button(libelle,
                    t("home.reserve-tip", "<gray>Le plot libre où tu te tiens, sinon le plus proche du centre."),
                    p -> confirmerReservation(p, taille, ici)));
        }
        List<PlotInfo> mes = plots.plotsDe(u);
        if (!mes.isEmpty()) {
            boutons.add(gui.button(t("home.my-plots", "<gold>Mes plots <gray>(<n>)", "n", mes.size()),
                    t("home.my-plots-tip", "<gray>Téléportation, éditeurs, validation."), p -> mesPlots(p, ici)));
        }
        boutons.add(gui.button(t("home.visit", "<aqua>Visiter les plots"),
                t("home.visit-tip", "<gray>Au hasard, par joueur, ou toute la liste."), p -> visites(p, ici)));
        PlotInfo sous = plots.plotEn(joueur.getLocation());
        if (sous != null && plots.peutVoter(u, sous.id())) {
            int note = plots.note(u, sous.id());
            boutons.add(0, gui.button(note == 0 ? t("home.vote", "<gold><bold>Voter pour ce plot")
                            : t("home.revote", "<gold><bold>Voter pour ce plot <gray>(ta note : <note>/5)", "note", note),
                    t("home.vote-tip", "<gray>Note le plot où tu te trouves, de 1 à 5."), p -> vote(p, sous.id(), ici)));
        }
        if (back != null) boutons.add(retour(back));
        List<Component> corps = new ArrayList<>();
        corps.add(t("home.body", "<gray>Construis en créatif sur tes plots, seul ou avec des éditeurs."));
        if (sous != null) {
            corps.add(t("home.here", "<gray>Tu es sur le plot n°<id> de <owner>.", "id", sous.id(), "owner", nom(sous.createur())));
        }
        gui.open(joueur, t("home.title", "<gold><bold>Kanvas"), corps, List.of(), boutons, null, 1);
        lang.saveIfNeeded();
    }

    private void confirmerReservation(Player joueur, Taille taille, Consumer<Player> retour) {
        gui.confirm(joueur, t("reserve.title", "<green><bold>Réserver un plot <size>", "size", nomTaille(taille)),
                t("reserve.body", "<gray>Tu prendras le plot libre où tu te tiens, sinon le plus proche du centre de Kanvas."),
                p -> {
                    try {
                        PlotInfo plot = plots.reserver(p, taille);
                        p.sendMessage(t("reserve.done", "<green>Plot <size> n°<id> réservé !", "size", nomTaille(taille), "id", plot.id()));
                    } catch (Refus r) {
                        refus(p, r, retour);
                    }
                },
                retour::accept);
    }

    // ------------------------------------------------------------------ vote

    /** Noter (ou renoter) le plot : 5 boutons, de 1 (rouge) à 5 (vert foncé). */
    private void vote(Player joueur, int id, Consumer<Player> retour) {
        PlotInfo plot = plots.plot(id);
        if (plot == null || !plots.peutVoter(joueur.getUniqueId(), id)) {
            retour.accept(joueur);
            return;
        }
        String[] couleurs = {"<red>", "<gold>", "<yellow>", "<green>", "<dark_green>"};
        List<ActionButton> boutons = new ArrayList<>();
        for (int n = 1; n <= 5; n++) {
            int note = n;
            boutons.add(gui.button(t("vote.button-" + n, couleurs[n - 1] + "<bold>" + n + "/5"), null, p -> {
                try {
                    plots.voter(p, id, note);
                    p.sendMessage(t("vote.done", "<green>Tu as donné <note>/5 au plot n°<id>.", "note", note, "id", id));
                } catch (Refus r) {
                    refus(p, r, retour);
                }
            }));
        }
        boutons.add(retour(retour));
        int actuelle = plots.note(joueur.getUniqueId(), id);
        gui.open(joueur, t("vote.title", "<gold><bold>Noter le plot n°<id>", "id", id),
                List.of(t("vote.owner", "<gray>Plot de <white><owner>", "owner", nom(plot.createur())),
                        actuelle == 0 ? t("vote.none", "<gray>Tu ne l'as pas encore noté.")
                                : t("vote.current", "<gray>Ta note actuelle : <white><note>/5 <gray>(un nouveau vote la remplace).",
                                        "note", actuelle)),
                List.of(), boutons, null, 5);
    }

    // ------------------------------------------------------------------ visites

    private static final int PLOTS_PAR_PAGE = 20;

    /** Texte d'un joueur avec les codes couleur « & ». */
    private static Component lore(String brut) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(brut == null ? "" : brut);
    }

    /** « Titre » ou « Plot n°X », puis taille, créateur, état et points. */
    private Component libellePlot(PlotInfo plot) {
        Component nomPlot = plot.titre().isEmpty() ? t("list.untitled", "<yellow>Plot n°<id>", "id", plot.id())
                : lore(plot.titre()).append(t("list.number", " <dark_gray>n°<id>", "id", plot.id()));
        return nomPlot.append(t("list.details", " <gray>(<size>, <owner>, <state>)",
                "size", nomTaille(plot.taille()), "owner", nom(plot.createur()),
                "state", plot.valide() ? t("list.validated", "<green><points> pts", "points", plot.points())
                        : t("list.building", "<white>en travaux")));
    }

    private void visites(Player joueur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> visites(p, retour);
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("visit.random", "<gold><bold>Au hasard : un plot à noter"),
                t("visit.random-tip", "<gray>Un plot validé que tu n'as pas encore noté."), p -> {
                    PlotInfo plot = plots.hasardANoter(p.getUniqueId());
                    if (plot == null) {
                        gui.notice(p, t("visit.random-none-title", "<gold><bold>Bravo !"),
                                t("visit.random-none", "<gray>Tu as déjà noté tous les plots validés que tu peux noter."), ici::accept);
                        return;
                    }
                    try {
                        plots.visiter(p, plot.id());
                    } catch (Refus r) {
                        refus(p, r, ici);
                    }
                }));
        boutons.add(gui.button(t("visit.players", "<yellow>Par joueur"),
                t("visit.players-tip", "<gray>Les têtes des joueurs : clique pour voir leurs plots."), p -> tetes.ouvrir(p, 0, ici)));
        boutons.add(gui.button(t("visit.all", "<yellow>Tous les plots"),
                t("visit.all-tip", "<gray>Explore la liste de tous les plots."), p -> tousLesPlots(p, 0, ici)));
        boutons.add(retour(retour));
        gui.open(joueur, t("visit.title", "<gold><bold>Visiter les plots"),
                List.of(t("visit.body", "<gray>Découvre les constructions des autres joueurs et note-les.")), List.of(), boutons, null, 1);
    }

    /** Plots d'un joueur (depuis les têtes). */
    private void plotsJoueur(Player joueur, UUID createur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> plotsJoueur(p, createur, retour);
        List<ActionButton> boutons = new ArrayList<>();
        for (PlotInfo plot : plots.plotsDuCreateur(createur)) {
            boutons.add(gui.button(libellePlot(plot), plot.description().isEmpty() ? null : lore(plot.description()),
                    p -> fiche(p, plot.id(), ici)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("player.title", "<gold><bold>Plots de <name>", "name", nom(createur)),
                List.of(), List.of(), boutons, null, 1);
    }

    /** Tous les plots, par pages de 20. */
    private void tousLesPlots(Player joueur, int page, Consumer<Player> retour) {
        List<PlotInfo> tous = plots.tousLesPlots();
        int pages = Math.max(1, (tous.size() + PLOTS_PAR_PAGE - 1) / PLOTS_PAR_PAGE);
        int n = Math.max(0, Math.min(page, pages - 1));
        Consumer<Player> ici = p -> tousLesPlots(p, n, retour);
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = n * PLOTS_PAR_PAGE; i < Math.min(tous.size(), (n + 1) * PLOTS_PAR_PAGE); i++) {
            PlotInfo plot = tous.get(i);
            boutons.add(gui.button(libellePlot(plot), plot.description().isEmpty() ? null : lore(plot.description()),
                    p -> fiche(p, plot.id(), ici)));
        }
        if (n > 0) boutons.add(gui.button(t("list.previous", "<yellow>Page précédente"), null, p -> tousLesPlots(p, n - 1, retour)));
        if (n < pages - 1) boutons.add(gui.button(t("list.next", "<yellow>Page suivante"), null, p -> tousLesPlots(p, n + 1, retour)));
        boutons.add(retour(retour));
        gui.open(joueur, t("list.title", "<gold><bold>Tous les plots <gray>(<page>/<pages>)", "page", n + 1, "pages", pages),
                List.of(tous.isEmpty() ? t("list.empty", "<gray>Aucun plot pour le moment.")
                        : t("list.body", "<gray><n> plot(s). Clique sur un plot pour sa fiche.", "n", tous.size())),
                List.of(), boutons, null, 1);
    }

    /** Titre et description (créateur) : deux champs, vides = retirés. */
    private void formulaireLore(Player joueur, int id, Consumer<Player> retour) {
        PlotInfo plot = plots.plot(id);
        if (plot == null) {
            retour.accept(joueur);
            return;
        }
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.form(t("lore.save", "<green>Enregistrer"), null, (p, vue) -> {
            try {
                plots.definirLore(p, id, vue.getText("titre"), vue.getText("description"));
                retour.accept(p);
            } catch (Refus r) {
                refus(p, r, q -> formulaireLore(q, id, retour));
            }
        }));
        boutons.add(retour(retour));
        gui.open(joueur, t("lore.title", "<gold><bold>Titre et description du plot n°<id>", "id", id),
                List.of(t("lore.body", "<gray>Couleurs avec & (ex. &6doré, &bbleu). Titre : <tmax> caractères, description : <dmax>. Laisse vide pour retirer.",
                        "tmax", KanvasPlots.TITRE_MAX, "dmax", KanvasPlots.DESCRIPTION_MAX)),
                List.of(gui.text("titre", t("lore.field-title", "Titre"), plot.titre(), KanvasPlots.TITRE_MAX * 3),
                        gui.text("description", t("lore.field-description", "Description"), plot.description(),
                                KanvasPlots.DESCRIPTION_MAX * 3)),
                boutons, null, 1);
    }

    // ------------------------------------------------------------------ mes plots

    private void mesPlots(Player joueur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> mesPlots(p, retour);
        List<ActionButton> boutons = new ArrayList<>();
        for (PlotInfo plot : plots.plotsDe(joueur.getUniqueId())) {
            boutons.add(gui.button(libellePlot(plot), null, p -> fiche(p, plot.id(), ici)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("my.title", "<gold><bold>Mes plots"),
                List.of(t("my.body", "<gray>Tes plots, comme créateur ou comme éditeur.")), List.of(), boutons, null, 1);
    }

    private void fiche(Player joueur, int id, Consumer<Player> retour) {
        PlotInfo plot = plots.plot(id);
        if (plot == null) {
            retour.accept(joueur);
            return;
        }
        Consumer<Player> ici = p -> fiche(p, id, retour);
        List<Component> corps = new ArrayList<>();
        if (!plot.titre().isEmpty()) corps.add(lore(plot.titre()));
        if (!plot.description().isEmpty()) corps.add(t("plot.description", "<gray>« <text><gray> »", "text", lore(plot.description())));
        corps.add(t("plot.owner", "<gray>Créateur : <white><owner>", "owner", nom(plot.createur())));
        corps.add(plot.editeurs().isEmpty() ? t("plot.no-editors", "<gray>Éditeurs : <white>aucun")
                : t("plot.editors", "<gray>Éditeurs : <white><names>", "names",
                        String.join(", ", plot.editeurs().stream().map(KvMenu::nom).toList())));
        corps.add(plot.enPreparation() ? t("plot.state-preparing", "<gray>État : <yellow>en préparation")
                : plot.valide() ? t("plot.state-validated", "<gray>État : <green>validé")
                : t("plot.state-building", "<gray>État : <white>en travaux"));
        corps.add(t("plot.points", "<gray>Points : <white><points> <gray>(<votes> vote(s)<avg>)", "points", plot.points(),
                "votes", plot.votes(), "avg", plot.votes() == 0 ? ""
                        : String.format(java.util.Locale.FRANCE, ", moyenne %.1f/5", plot.moyenne())));
        List<ActionButton> boutons = new ArrayList<>();
        if (plots.peutVoter(joueur.getUniqueId(), id)) {
            int note = plots.note(joueur.getUniqueId(), id);
            boutons.add(gui.button(note == 0 ? t("plot.vote", "<gold><bold>Noter ce plot")
                    : t("plot.revote", "<gold><bold>Noter ce plot <gray>(ta note : <note>/5)", "note", note), null, p -> vote(p, id, ici)));
        }
        boutons.add(gui.button(t("plot.tp", "<aqua>Se téléporter"), null, p -> {
            try {
                plots.visiter(p, id);
            } catch (Refus r) {
                refus(p, r, ici);
            }
        }));
        if (plot.createur().equals(joueur.getUniqueId())) {
            boutons.add(gui.button(t("plot.lore-button", "<yellow>Titre et description"),
                    t("plot.lore-tip", "<gray>Donne un nom et une histoire à ta construction."), p -> formulaireLore(p, id, ici)));
            boutons.add(gui.button(t("plot.editors-button", "<yellow>Éditeurs"),
                    t("plot.editors-tip", "<gray>Ajouter ou retirer des joueurs qui construisent avec toi."),
                    p -> editeurs(p, id, ici)));
        }
        if (plot.createur().equals(joueur.getUniqueId()) && !plot.enPreparation()) {
            if (!plot.valide()) {
                boutons.add(gui.button(t("plot.validate", "<green>Valider le plot"),
                        t("plot.validate-tip", "<gray>Plot fini : il est figé, peut être noté, et sa place se libère."),
                        p -> gui.confirm(p, t("plot.validate-title", "<green><bold>Valider le plot n°<id>", "id", id),
                                t("plot.validate-body", "<gray>Le plot sera figé (plus aucune modification) et les autres joueurs pourront le noter. Sa place se libère ; tu pourras le rouvrir plus tard s'il te reste une place."),
                                q -> {
                                    try {
                                        plots.valider(q, id);
                                        q.sendMessage(t("plot.validate-done", "<green>Plot n°<id> validé : il peut maintenant être noté !", "id", id));
                                    } catch (Refus r) {
                                        refus(q, r, ici);
                                    }
                                }, ici::accept)));
            } else {
                boutons.add(gui.button(t("plot.reopen", "<yellow>Rouvrir le plot"),
                        t("plot.reopen-tip", "<gray>Pour le modifier : il reprend une place. Les votes sont gardés."),
                        p -> gui.confirm(p, t("plot.reopen-title", "<yellow><bold>Rouvrir le plot n°<id>", "id", id),
                                t("plot.reopen-body", "<gray>Le plot reprend une place <size>. Ses votes sont gardés ; il ne pourra pas être noté avant d'être revalidé.", "size", nomTaille(plot.taille())),
                                q -> {
                                    try {
                                        plots.rouvrir(q, id);
                                        q.sendMessage(t("plot.reopen-done", "<green>Plot n°<id> rouvert : tu peux de nouveau y construire.", "id", id));
                                    } catch (Refus r) {
                                        refus(q, r, ici);
                                    }
                                }, ici::accept)));
            }
        }
        boolean staff = joueur.hasPermission("kvplots.admin");
        if ((plot.createur().equals(joueur.getUniqueId()) && !plot.valide()) || staff) {
            boutons.add(gui.button(t("plot.reset", "<gold>Remettre à zéro"),
                    t("plot.reset-tip", "<gray>Efface tout ce qui est construit. Le plot reste à toi."),
                    p -> gui.confirm(p, t("plot.reset-title", "<gold><bold>Remettre à zéro le plot n°<id>", "id", id),
                            t("plot.reset-body", "<gray>Tout ce qui est construit sur ce plot sera effacé. Le plot reste réservé."),
                            q -> {
                                try {
                                    plots.remettreAZero(q, id);
                                    q.sendMessage(t("plot.reset-started", "<yellow>Remise à zéro du plot n°<id> en cours...", "id", id));
                                } catch (Refus r) {
                                    refus(q, r, ici);
                                }
                            }, ici::accept)));
            boutons.add(gui.button(t("plot.delete", "<red>Supprimer le plot"),
                    t("plot.delete-tip", "<gray>Efface tout et libère la place."),
                    p -> gui.confirm(p, t("plot.delete-title", "<red><bold>Supprimer le plot n°<id>", "id", id),
                            t("plot.delete-body", "<gray>Tout ce qui est construit sera effacé et la place sera libérée. Impossible d'annuler."),
                            q -> {
                                try {
                                    plots.supprimer(q, id);
                                    q.sendMessage(t("plot.delete-started", "<yellow>Suppression du plot n°<id> en cours...", "id", id));
                                } catch (Refus r) {
                                    refus(q, r, ici);
                                }
                            }, ici::accept)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("plot.title", "<gold><bold>Plot n°<id> <gray>(<size>)", "id", id, "size", nomTaille(plot.taille())),
                corps, List.of(), boutons, null, 1);
    }

    // ------------------------------------------------------------------ éditeurs

    private void editeurs(Player joueur, int id, Consumer<Player> retour) {
        PlotInfo plot = plots.plot(id);
        if (plot == null) {
            retour.accept(joueur);
            return;
        }
        Consumer<Player> ici = p -> editeurs(p, id, retour);
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.form(t("editors.add", "<green>Ajouter l'éditeur saisi"),
                t("editors.add-tip", "<gray>Le joueur doit s'être déjà connecté à Kanvas."), (p, vue) -> {
                    String pseudo = vue.getText("pseudo");
                    pseudo = pseudo == null ? "" : pseudo.trim();
                    OfflinePlayer cible = Bukkit.getPlayerExact(pseudo);
                    if (cible == null && !pseudo.isEmpty()) cible = Bukkit.getOfflinePlayerIfCached(pseudo);
                    try {
                        if (cible == null) throw new Refus("Joueur inconnu : « " + pseudo + " » (il doit s'être déjà connecté).");
                        plots.ajouterEditeur(p, id, cible.getUniqueId());
                        editeurs(p, id, retour);
                    } catch (Refus r) {
                        refus(p, r, ici);
                    }
                }));
        for (UUID editeur : plot.editeurs()) {
            String n = nom(editeur);
            boutons.add(gui.button(t("editors.remove", "<red>Retirer <name>", "name", n), null,
                    p -> gui.confirm(p, t("editors.remove-title", "<red><bold>Retirer <name>", "name", n),
                            t("editors.remove-body", "<gray><name> ne pourra plus construire sur ce plot.", "name", n),
                            q -> {
                                try {
                                    plots.retirerEditeur(q, id, editeur);
                                    editeurs(q, id, retour);
                                } catch (Refus r) {
                                    refus(q, r, ici);
                                }
                            },
                            ici::accept)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("editors.title", "<yellow><bold>Éditeurs du plot n°<id>", "id", id),
                List.of(plot.editeurs().isEmpty() ? t("editors.none", "<gray>Aucun éditeur pour le moment.")
                        : t("editors.body", "<gray>Les éditeurs construisent avec toi. Ils ne pourront pas voter pour ce plot.")),
                List.of(gui.text("pseudo", t("editors.field", "Pseudo du joueur"), "", 16)),
                boutons, null, 1);
    }
}
