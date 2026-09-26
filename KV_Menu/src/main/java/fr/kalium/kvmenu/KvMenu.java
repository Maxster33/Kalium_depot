package fr.kalium.kvmenu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.kvplots.api.KanvasPlots;
import fr.kalium.kvplots.api.KanvasPlots.ConcoursInfo;
import fr.kalium.kvplots.api.KanvasPlots.PhaseConcours;
import fr.kalium.kvplots.api.KanvasPlots.PlotInfo;
import fr.kalium.kvplots.api.KanvasPlots.Refus;
import fr.kalium.kvplots.api.KanvasPlots.SignalementInfo;
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
 * 1.3.0 : signalements (formulaire, poudre de blaze du mode vote, écrans du staff) ; concours de build.
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
        getServer().getPluginManager().registerEvents(new PoudreSignalement(), this);
        getServer().getServicesManager().register(MenuSection.class,
                MenuSection.of(this, "kanvas-concours", MenuSection.Audience.ADMINS,
                        t("klm.contest", "<light_purple>Kanvas : concours de build"),
                        t("klm.contest-tip", "<gray>Lancer, modifier, modérer le concours de build."),
                        this::gererConcours),
                this, ServicePriority.Normal);
        getServer().getServicesManager().register(MenuSection.class,
                MenuSection.of(this, "kanvas-signalements", MenuSection.Audience.ADMINS,
                        t("klm.reports", "<light_purple>Kanvas : signalements"),
                        t("klm.reports-tip", "<gray>Plots signalés par les joueurs : détail et actions."),
                        (p, back) -> adminSignalements(p, false, 0, back)),
                this, ServicePriority.Normal);
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
        ConcoursInfo enCours = plots.concoursActuel();
        boutons.add(gui.button(enCours == null ? t("home.contest", "<light_purple>Concours de build")
                        : t("home.contest-on", "<light_purple><bold>Concours de build <gray>: <theme>", "theme", lore(enCours.theme())),
                t("home.contest-tip", "<gray>Participer, voir les participants, anciens concours."), p -> concours(p, ici)));
        boutons.add(gui.button(t("home.visit", "<aqua>Visiter les plots"),
                t("home.visit-tip", "<gray>Au hasard, par joueur, ou toute la liste."), p -> visites(p, ici)));
        if (staff(joueur)) {
            boutons.add(gui.button(t("home.reports", "<light_purple>Signalements <gray>(<n> à traiter)", "n", plots.signalements(false).size()),
                    t("home.reports-tip", "<gray>Réservé au staff."), p -> adminSignalements(p, false, 0, ici)));
        }
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

    // ------------------------------------------------------------------ concours de build

    private static final long HEURE = 3_600_000L, JOUR = 24 * HEURE;

    private Component phase(ConcoursInfo c) {
        return switch (c.phase()) {
            case EN_COURS -> t("contest.phase-building", "<green>construction en cours");
            case VOTES -> t("contest.phase-votes", "<gold>votes en cours");
            case TERMINE -> t("contest.phase-done", "<gray>terminé");
            case ANNULE -> t("contest.phase-cancelled", "<red>annulé");
        };
    }

    /** Menu du concours (joueurs), et « Gérer le concours » pour le staff. */
    private void concours(Player joueur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> concours(p, retour);
        ConcoursInfo c = plots.concoursActuel();
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        if (c == null) {
            corps.add(t("contest.none", "<gray>Aucun concours de build pour le moment. Reviens bientôt !"));
        } else {
            corps.add(t("contest.theme", "<gray>Thème : <white><theme>", "theme", lore(c.theme())));
            corps.add(t("contest.size", "<gray>Plots : <white><size>", "size", nomTaille(c.taille())));
            corps.add(t("contest.state", "<gray>État : <state>", "state", phase(c)));
            long reste = (c.phase() == PhaseConcours.EN_COURS ? c.fin() : c.finVotes()) - System.currentTimeMillis();
            corps.add(c.phase() == PhaseConcours.EN_COURS
                    ? t("contest.ends", "<gray>Fin dans : <white><time>", "time", plots.duree(reste))
                    : t("contest.votes-end", "<gray>Fin des votes dans : <white><time>", "time", plots.duree(reste)));
            corps.add(t("contest.count", "<gray>Participants : <white><n>", "n", c.participants()));
            PlotInfo mien = plots.participation(joueur.getUniqueId());
            if (mien == null && c.phase() == PhaseConcours.EN_COURS) {
                boutons.add(gui.button(t("contest.join", "<green><bold>Participer"),
                        t("contest.join-tip", "<gray>Un plot <size> en plus, pour la durée du concours.", "size", nomTaille(c.taille())),
                        p -> gui.confirm(p, t("contest.join-title", "<green><bold>Participer au concours"),
                                t("contest.join-body", "<gray>Tu reçois un plot <size> en plus (il ne prend pas de place) pour construire sur le thème « <theme> ». Tu seras téléporté dessus.",
                                        "size", nomTaille(c.taille()), "theme", lore(c.theme())),
                                q -> {
                                    try {
                                        PlotInfo plot = plots.participer(q);
                                        q.sendMessage(t("contest.joined", "<green>Tu participes au concours ! Ton plot : n°<id>.", "id", plot.id()));
                                    } catch (Refus r) {
                                        refus(q, r, ici);
                                    }
                                }, ici::accept)));
            }
            if (mien != null) {
                boutons.add(gui.button(t("contest.my-plot", "<aqua>Mon plot du concours <gray>(n°<id>)", "id", mien.id()), null, p -> {
                    try {
                        plots.visiter(p, mien.id());
                    } catch (Refus r) {
                        refus(p, r, ici);
                    }
                }));
                if (c.phase() == PhaseConcours.EN_COURS) {
                    boutons.add(gui.button(t("contest.leave", "<red>Annuler ma participation"), null,
                            p -> gui.confirm(p, t("contest.leave-title", "<red><bold>Retirer ta participation"),
                                    t("contest.leave-body", "<gray>Êtes-vous sûr de vouloir retirer votre participation ? Cela supprimera votre plot."),
                                    q -> {
                                        try {
                                            plots.annulerParticipation(q);
                                            q.sendMessage(t("contest.left", "<yellow>Participation retirée : ton plot du concours est supprimé."));
                                        } catch (Refus r) {
                                            refus(q, r, ici);
                                        }
                                    }, ici::accept)));
                }
            }
            boutons.add(gui.button(t("contest.participants", "<yellow>Participants <gray>(<n>)", "n", c.participants()),
                    t("contest.participants-tip", "<gray>Voir leurs plots et s'y téléporter."), p -> participants(p, c.id(), ici)));
        }
        if (!plots.anciensConcours().isEmpty()) {
            boutons.add(gui.button(t("contest.past", "<gray>Anciens concours"), null, p -> anciensConcours(p, ici)));
        }
        if (staff(joueur)) {
            boutons.add(gui.button(t("contest.manage", "<light_purple>Gérer le concours"), t("home.reports-tip", "<gray>Réservé au staff."),
                    p -> gererConcours(p, ici)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("contest.title", "<light_purple><bold>Concours de build"), corps, List.of(), boutons, null, 1);
    }

    /** Plots d'un concours ; classés (1., 2., ...) une fois les votes ouverts. */
    private void participants(Player joueur, int id, Consumer<Player> retour) {
        ConcoursInfo c = plots.concours(id);
        if (c == null) {
            retour.accept(joueur);
            return;
        }
        Consumer<Player> ici = p -> participants(p, id, retour);
        boolean classe = c.phase() != PhaseConcours.EN_COURS;
        List<PlotInfo> liste = plots.plotsDuConcours(id);
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = 0; i < liste.size(); i++) {
            PlotInfo plot = liste.get(i);
            Component libelle = classe ? t("contest.rank", "<gold><rank>. ", "rank", i + 1).append(libellePlot(plot)) : libellePlot(plot);
            boutons.add(gui.button(libelle, plot.description().isEmpty() ? null : lore(plot.description()), p -> fiche(p, plot.id(), ici)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("contest.participants-title", "<light_purple><bold>Concours : <theme>", "theme", lore(c.theme())),
                List.of(liste.isEmpty() ? t("contest.no-participant", "<gray>Aucun participant pour le moment.")
                        : classe ? t("contest.ranking", "<gray>Classement : total des points, puis moyenne.")
                        : t("contest.participants-body", "<gray>Clique sur un plot pour sa fiche et t'y téléporter.")),
                List.of(), boutons, null, 1);
    }

    private void anciensConcours(Player joueur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> anciensConcours(p, retour);
        List<ActionButton> boutons = new ArrayList<>();
        for (ConcoursInfo c : plots.anciensConcours()) {
            List<PlotInfo> classement = plots.plotsDuConcours(c.id());
            Component vainqueur = classement.isEmpty() ? t("contest.past-nobody", "<gray>aucun participant")
                    : t("contest.past-winner", "<gray>vainqueur : <white><name>", "name", nom(classement.get(0).createur()));
            boutons.add(gui.button(lore(c.theme()).append(t("contest.past-details", " <gray>(<date>, <n> participant(s), ",
                            "date", DATE.format(Instant.ofEpochMilli(c.debut())), "n", c.participants())).append(vainqueur)
                            .append(Component.text(")")), null, p -> participants(p, c.id(), ici)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("contest.past-title", "<light_purple><bold>Anciens concours"), List.of(), List.of(), boutons, null, 1);
    }

    // --- staff ---

    private List<io.papermc.paper.registry.data.dialog.input.DialogInput> champsDuree(String cle, Component libelle,
                                                                                     int joursMax, int joursInitial, int heuresInitial) {
        return List.of(gui.number(cle + "_jours", libelle.append(t("contest.days", " : jours")), 0, joursMax, joursInitial, 1),
                gui.number(cle + "_heures", libelle.append(t("contest.hours", " : heures")), 0, 23, heuresInitial, 1));
    }

    private static long lireDuree(io.papermc.paper.dialog.DialogResponseView vue, String cle) {
        Float j = vue.getFloat(cle + "_jours"), h = vue.getFloat(cle + "_heures");
        return Math.round(j == null ? 0 : j) * JOUR + Math.round(h == null ? 0 : h) * HEURE;
    }

    private void gererConcours(Player joueur, Consumer<Player> retour) {
        if (!staff(joueur)) return;
        Consumer<Player> ici = p -> gererConcours(p, retour);
        ConcoursInfo c = plots.concoursActuel();
        List<ActionButton> boutons = new ArrayList<>();
        if (c == null) {
            List<io.papermc.paper.registry.data.dialog.input.DialogInput> champs = new ArrayList<>();
            champs.add(gui.text("theme", t("contest.field-theme", "Thème (couleurs avec &)"), "", 64));
            champs.add(gui.choice("taille", t("contest.field-size", "Taille des plots"), List.of("MOYEN", "GRAND"),
                    List.of(t("taille.moyen-label", "Moyen (49 x 49)"), t("taille.grand-label", "Grand (107 x 107)")), "MOYEN"));
            champs.addAll(champsDuree("duree", t("contest.field-duration", "Durée du concours"), 30, 7, 0));
            champs.addAll(champsDuree("votes", t("contest.field-votes", "Durée des votes"), 14, 2, 0));
            boutons.add(gui.form(t("contest.start", "<green><bold>Lancer le concours"), null, (p, vue) -> {
                try {
                    plots.lancerConcours(p, vue.getText("theme"), "GRAND".equals(vue.getText("taille")) ? Taille.GRAND : Taille.MOYEN,
                            lireDuree(vue, "duree"), lireDuree(vue, "votes"));
                    retour.accept(p);
                } catch (Refus r) {
                    refus(p, r, ici);
                }
            }));
            boutons.add(retour(retour));
            gui.open(joueur, t("contest.new-title", "<light_purple><bold>Nouveau concours de build"),
                    List.of(t("contest.new-body", "<gray>Le concours s'ouvre dès le lancement ; à la fin, les plots sont figés et les votes s'ouvrent pour la durée choisie.")),
                    champs, boutons, null, 1);
            return;
        }
        boolean construction = c.phase() == PhaseConcours.EN_COURS;
        boutons.add(gui.button(t("contest.edit", "<yellow>Modifier"), t("contest.edit-tip", "<gray>Thème, fin du concours, durée des votes."),
                p -> modifierConcours(p, ici)));
        boutons.add(gui.button(t("contest.moderate", "<yellow>Modérer les plots <gray>(<n>)", "n", c.participants()), null,
                p -> modererConcours(p, c.id(), ici)));
        if (construction) {
            boutons.add(gui.button(t("contest.end-now", "<gold>Terminer maintenant (ouvrir les votes)"), null,
                    p -> gui.confirm(p, t("contest.end-now-title", "<gold><bold>Terminer la construction"),
                            t("contest.end-now-body", "<gray>Les plots du concours sont figés et les votes s'ouvrent pour <time>.", "time", plots.duree(c.dureeVotes())),
                            q -> action(q, () -> plots.terminerConcours(q), ici), ici::accept)));
        } else {
            boutons.add(gui.button(t("contest.close-votes", "<gold>Clore les votes maintenant"), null,
                    p -> gui.confirm(p, t("contest.close-votes-title", "<gold><bold>Clore les votes"),
                            t("contest.close-votes-body", "<gray>Le classement est arrêté et annoncé ; les plots sont gardés dans « Anciens concours »."),
                            q -> action(q, () -> plots.cloreVotes(q), ici), ici::accept)));
        }
        boutons.add(gui.button(t("contest.cancel", "<red>Annuler le concours"), null,
                p -> gui.confirm(p, t("contest.cancel-title", "<red><bold>Annuler le concours"),
                        t("contest.cancel-body", "<gray>Tous les plots du concours seront supprimés. Impossible d'annuler."),
                        q -> action(q, () -> plots.annulerConcours(q), ici), ici::accept)));
        boutons.add(retour(retour));
        long reste = (construction ? c.fin() : c.finVotes()) - System.currentTimeMillis();
        gui.open(joueur, t("contest.manage-title", "<light_purple><bold>Gérer le concours"),
                List.of(t("contest.theme", "<gray>Thème : <white><theme>", "theme", lore(c.theme())),
                        t("contest.state", "<gray>État : <state>", "state", phase(c)),
                        construction ? t("contest.ends", "<gray>Fin dans : <white><time>", "time", plots.duree(reste))
                                : t("contest.votes-end", "<gray>Fin des votes dans : <white><time>", "time", plots.duree(reste)),
                        t("contest.count", "<gray>Participants : <white><n>", "n", c.participants())),
                List.of(), boutons, null, 1);
    }

    private void action(Player joueur, ActionStaff action, Consumer<Player> ensuite) {
        try {
            action.faire();
            ensuite.accept(joueur);
        } catch (Refus r) {
            refus(joueur, r, ensuite);
        }
    }

    private void modifierConcours(Player joueur, Consumer<Player> retour) {
        ConcoursInfo c = plots.concoursActuel();
        if (c == null || !staff(joueur)) {
            retour.accept(joueur);
            return;
        }
        boolean construction = c.phase() == PhaseConcours.EN_COURS;
        List<io.papermc.paper.registry.data.dialog.input.DialogInput> champs = new ArrayList<>();
        champs.add(gui.text("theme", t("contest.field-theme", "Thème (couleurs avec &)"), c.theme(), 64));
        if (construction) champs.addAll(champsDuree("duree", t("contest.field-end", "Fin du concours dans (0 = inchangé)"), 30, 0, 0));
        champs.addAll(champsDuree("votes", construction ? t("contest.field-votes-change", "Durée des votes (0 = inchangée)")
                : t("contest.field-votes-end", "Fin des votes dans (0 = inchangé)"), 14, 0, 0));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.form(t("lore.save", "<green>Enregistrer"), null, (p, vue) -> {
            try {
                plots.modifierConcours(p, vue.getText("theme"), construction ? lireDuree(vue, "duree") : 0, lireDuree(vue, "votes"));
                retour.accept(p);
            } catch (Refus r) {
                refus(p, r, retour);
            }
        }));
        boutons.add(retour(retour));
        gui.open(joueur, t("contest.edit-title", "<light_purple><bold>Modifier le concours"), List.of(), champs, boutons, null, 1);
    }

    private void modererConcours(Player joueur, int id, Consumer<Player> retour) {
        Consumer<Player> ici = p -> modererConcours(p, id, retour);
        List<ActionButton> boutons = new ArrayList<>();
        for (PlotInfo plot : plots.plotsDuConcours(id)) {
            boutons.add(gui.button(libellePlot(plot), null, p -> modererPlot(p, plot.id(), ici)));
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("contest.moderate-title", "<light_purple><bold>Plots du concours"), List.of(), List.of(), boutons, null, 1);
    }

    private void modererPlot(Player joueur, int id, Consumer<Player> retour) {
        PlotInfo plot = plots.plot(id);
        if (plot == null || !staff(joueur)) {
            retour.accept(joueur);
            return;
        }
        Consumer<Player> ici = p -> modererPlot(p, id, retour);
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("admin.tp", "<aqua>Se téléporter au plot"), null, p -> {
            try {
                plots.visiter(p, id);
            } catch (Refus r) {
                refus(p, r, ici);
            }
        }));
        boutons.add(gui.button(t("admin.sheet", "<yellow>Fiche du plot"), null, p -> fiche(p, id, ici)));
        boutons.add(gui.button(t("admin.reset", "<red>Remettre le plot à zéro"), null,
                p -> gui.confirm(p, t("admin.reset-title", "<red><bold>Remettre à zéro le plot n°<plot>", "plot", id),
                        t("admin.reset-body", "<gray>Tout ce qui est construit sur ce plot sera effacé. Impossible d'annuler."),
                        q -> action(q, () -> plots.remettreAZero(q, id), retour), ici::accept)));
        boutons.add(gui.button(t("contest.exclude", "<red>Exclure du concours"), t("contest.exclude-tip", "<gray>Le plot est supprimé."),
                p -> gui.confirm(p, t("contest.exclude-title", "<red><bold>Exclure le plot n°<plot>", "plot", id),
                        t("contest.exclude-body", "<gray>Le plot de <owner> est supprimé et retiré du concours. Impossible d'annuler.", "owner", nom(plot.createur())),
                        q -> action(q, () -> plots.exclureDuConcours(q, id), retour), ici::accept)));
        boutons.add(retour(retour));
        gui.open(joueur, t("contest.moderate-plot", "<light_purple><bold>Plot n°<id> du concours", "id", id),
                List.of(t("plot.owner", "<gray>Créateur : <white><owner>", "owner", nom(plot.createur()))), List.of(), boutons, null, 1);
    }

    // ------------------------------------------------------------------ signalements

    private static final int SIGNALEMENTS_PAR_PAGE = 15;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Paris"));

    private boolean staff(Player joueur) {
        return joueur.hasPermission("kvplots.admin");
    }

    /** Formulaire de signalement : raisons à cocher, « Autre » en texte libre. retour null = « Fermer ». */
    private void signaler(Player joueur, int id, Consumer<Player> retour) {
        PlotInfo plot = plots.plot(id);
        if (plot == null || !plots.peutSignaler(joueur.getUniqueId(), id)) {
            if (retour != null) retour.accept(joueur);
            return;
        }
        List<String> raisons = plots.raisonsSignalement();
        List<io.papermc.paper.registry.data.dialog.input.DialogInput> champs = new ArrayList<>();
        for (int i = 0; i < raisons.size(); i++) champs.add(gui.toggle("r" + i, Component.text(raisons.get(i)), false));
        champs.add(gui.text("autre", t("report.other", "Autre (précise)"), "", 200));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.form(t("report.send", "<red><bold>Envoyer le signalement"), null, (p, vue) -> {
            List<String> choisies = new ArrayList<>();
            for (int i = 0; i < raisons.size(); i++) {
                if (Boolean.TRUE.equals(vue.getBoolean("r" + i))) choisies.add(raisons.get(i));
            }
            try {
                plots.signaler(p, id, choisies, vue.getText("autre"));
                gui.notice(p, t("report.done-title", "<green><bold>Merci !"),
                        t("report.done", "<gray>Ton signalement a été envoyé au staff."), q -> {
                            if (retour != null) retour.accept(q);
                        });
            } catch (Refus r) {
                refus(p, r, q -> signaler(q, id, retour));
            }
        }));
        if (retour != null) boutons.add(retour(retour));
        gui.open(joueur, t("report.title", "<red><bold>Signaler le plot n°<id>", "id", id),
                List.of(t("report.body", "<gray>Plot de <white><owner><gray>. Coche les raisons, ou précise dans « Autre ».",
                        "owner", nom(plot.createur()))),
                champs, boutons, null, 1);
    }

    /** Staff : signalements non traités (ou classés), par pages. */
    private void adminSignalements(Player joueur, boolean classes, int page, Consumer<Player> retour) {
        if (!staff(joueur)) return;
        List<SignalementInfo> liste = plots.signalements(classes);
        int pages = Math.max(1, (liste.size() + SIGNALEMENTS_PAR_PAGE - 1) / SIGNALEMENTS_PAR_PAGE);
        int n = Math.max(0, Math.min(page, pages - 1));
        Consumer<Player> ici = p -> adminSignalements(p, classes, n, retour);
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = n * SIGNALEMENTS_PAR_PAGE; i < Math.min(liste.size(), (n + 1) * SIGNALEMENTS_PAR_PAGE); i++) {
            SignalementInfo s = liste.get(i);
            boutons.add(gui.button(t("admin.item", "<yellow>n°<id> <gray>· plot n°<plot> · <author> · <date>", "id", s.id(),
                            "plot", s.plot(), "author", nom(s.auteur()), "date", DATE.format(Instant.ofEpochMilli(s.date()))),
                    Component.text(String.join(", ", s.raisons()) + (s.autre().isEmpty() ? "" : " « " + s.autre() + " »")),
                    p -> adminDetail(p, s.id(), ici)));
        }
        if (n > 0) boutons.add(gui.button(t("list.previous", "<yellow>Page précédente"), null, p -> adminSignalements(p, classes, n - 1, retour)));
        if (n < pages - 1) boutons.add(gui.button(t("list.next", "<yellow>Page suivante"), null, p -> adminSignalements(p, classes, n + 1, retour)));
        boutons.add(gui.button(classes ? t("admin.show-open", "<aqua>Voir les signalements à traiter")
                : t("admin.show-closed", "<aqua>Voir les signalements classés"), null, p -> adminSignalements(p, !classes, 0, retour)));
        if (retour != null) boutons.add(retour(retour));
        gui.open(joueur, classes ? t("admin.title-closed", "<light_purple><bold>Signalements classés")
                        : t("admin.title", "<light_purple><bold>Signalements à traiter <gray>(<n>)", "n", liste.size()),
                List.of(liste.isEmpty() ? t("admin.empty", "<gray>Aucun signalement.")
                        : t("admin.body", "<gray>Clique sur un signalement pour le détail et les actions.")),
                List.of(), boutons, null, 1);
    }

    /** Staff : détail d'un signalement et actions (téléportation, fiche, classer, dévalider, remettre à zéro). */
    private void adminDetail(Player joueur, int sid, Consumer<Player> retour) {
        SignalementInfo s = plots.signalement(sid);
        if (s == null || !staff(joueur)) {
            retour.accept(joueur);
            return;
        }
        Consumer<Player> ici = p -> adminDetail(p, sid, retour);
        PlotInfo plot = plots.plot(s.plot());
        List<Component> corps = new ArrayList<>();
        corps.add(plot == null ? t("admin.plot-gone", "<gray>Plot n°<plot> : <red>supprimé", "plot", s.plot())
                : t("admin.plot", "<gray>Plot n°<plot> de <white><owner> <gray>(<state>)", "plot", s.plot(),
                        "owner", nom(plot.createur()), "state", plot.valide() ? "validé" : "en travaux"));
        corps.add(t("admin.author", "<gray>Signalé par <white><author> <gray>le <date>", "author", nom(s.auteur()),
                "date", DATE.format(Instant.ofEpochMilli(s.date()))));
        if (!s.raisons().isEmpty()) corps.add(t("admin.reasons", "<gray>Raisons : <white><reasons>", "reasons", String.join(", ", s.raisons())));
        if (!s.autre().isEmpty()) corps.add(t("admin.other", "<gray>Autre : <white><text>", "text", s.autre()));
        if (s.classe()) {
            corps.add(t("admin.closed", "<green>Classé par <by> : <action>", "by", s.traitePar() == null ? "?" : nom(s.traitePar()),
                    "action", s.action()));
        }
        List<ActionButton> boutons = new ArrayList<>();
        if (plot != null) {
            boutons.add(gui.button(t("admin.tp", "<aqua>Se téléporter au plot"), null, p -> {
                try {
                    plots.visiter(p, s.plot());
                } catch (Refus r) {
                    refus(p, r, ici);
                }
            }));
            boutons.add(gui.button(t("admin.sheet", "<yellow>Fiche du plot"), null, p -> fiche(p, s.plot(), ici)));
        }
        if (!s.classe()) {
            boutons.add(gui.button(t("admin.dismiss", "<gray>Classer sans suite"), null, p -> traiter(p, sid, "aucune action", null, ici, retour)));
            if (plot != null && plot.valide()) {
                boutons.add(gui.button(t("admin.unvalidate", "<gold>Dévalider le plot"),
                        t("admin.unvalidate-tip", "<gray>Il repasse en travaux (votes gardés) ; le signalement est classé."),
                        p -> gui.confirm(p, t("admin.unvalidate-title", "<gold><bold>Dévalider le plot n°<plot>", "plot", s.plot()),
                                t("admin.unvalidate-body", "<gray>Le plot repasse en travaux : son créateur pourra le modifier et le revalider."),
                                q -> traiter(q, sid, "plot dévalidé", () -> plots.devalider(q, s.plot()), ici, retour), ici::accept)));
            }
            if (plot != null) {
                boutons.add(gui.button(t("admin.reset", "<red>Remettre le plot à zéro"),
                        t("admin.reset-tip", "<gray>Efface tout ce qui y est construit ; le signalement est classé."),
                        p -> gui.confirm(p, t("admin.reset-title", "<red><bold>Remettre à zéro le plot n°<plot>", "plot", s.plot()),
                                t("admin.reset-body", "<gray>Tout ce qui est construit sur ce plot sera effacé. Impossible d'annuler."),
                                q -> traiter(q, sid, "plot remis à zéro", () -> plots.remettreAZero(q, s.plot()), ici, retour), ici::accept)));
            }
        }
        boutons.add(retour(retour));
        gui.open(joueur, t("admin.detail-title", "<light_purple><bold>Signalement n°<id>", "id", sid), corps, List.of(), boutons, null, 1);
    }

    private interface ActionStaff {
        void faire() throws Refus;
    }

    /** Fait l'action (si besoin), classe le signalement, puis revient à la liste. */
    private void traiter(Player staff, int sid, String nomAction, ActionStaff action, Consumer<Player> ici, Consumer<Player> liste) {
        try {
            if (action != null) action.faire();
            plots.classer(staff, sid, nomAction);
            staff.sendMessage(t("admin.done", "<green>Signalement n°<id> classé (<action>).", "id", sid, "action", nomAction));
            liste.accept(staff);
        } catch (Refus r) {
            refus(staff, r, ici);
        }
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
        if (plots.peutSignaler(joueur.getUniqueId(), id)) {
            boutons.add(gui.button(t("plot.report", "<red>Signaler ce plot"),
                    t("plot.report-tip", "<gray>Prévenir le staff d'un problème."), p -> signaler(p, id, ici)));
        }
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
        if (plot.createur().equals(joueur.getUniqueId()) && !plot.enPreparation() && plot.concours() == 0) {
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
            if (plot.concours() == 0 || staff) {
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

    /** Poudre de blaze du mode vote (KV_Plots) : ouvre le formulaire de signalement du plot noté. */
    private final class PoudreSignalement implements Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onInteract(PlayerInteractEvent e) {
            if (e.getHand() != EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL
                    || !plots.estObjetSignalement(e.getItem())) return;
            int id = plots.plotEnVote(e.getPlayer());
            if (id != 0) signaler(e.getPlayer(), id, null);
        }
    }
}
