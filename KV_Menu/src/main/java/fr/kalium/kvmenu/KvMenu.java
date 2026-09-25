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

/**
 * KV_Menu (1.0.0) : menus du serveur Kanvas (demande de LeKiwi06, 26/09/2026 : « une interface au lieu de juste avoir
 * les commandes »). Hiérarchie des interfaces : KLM_Menu (catalogue) -> KV_Menu -> actions de KV_Plots.
 *
 * - Accueil : réserver un plot moyen / grand, mes plots.
 * - Mes plots : un bouton par plot (créateur ou éditeur) -> fiche du plot : téléportation, éditeurs, remise à zéro,
 *   suppression (créateur).
 * - Ouverture : étoile du Nether (emplacement 4), /kanvas (/kv, /plots) et le catalogue de KLM_Menu (entrée « Kanvas »).
 */
public final class KvMenu extends JavaPlugin {

    private Lang lang;
    private Gui gui;
    private KanvasPlots plots;

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
                    t("home.my-plots-tip", "<gray>Téléportation, éditeurs."), p -> mesPlots(p, ici)));
        }
        if (back != null) boutons.add(retour(back));
        List<Component> corps = new ArrayList<>();
        corps.add(t("home.body", "<gray>Construis en créatif sur tes plots, seul ou avec des éditeurs."));
        PlotInfo sous = plots.plotEn(joueur.getLocation());
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

    // ------------------------------------------------------------------ mes plots

    private void mesPlots(Player joueur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> mesPlots(p, retour);
        List<ActionButton> boutons = new ArrayList<>();
        for (PlotInfo plot : plots.plotsDe(joueur.getUniqueId())) {
            boolean createur = plot.createur().equals(joueur.getUniqueId());
            Component libelle = createur
                    ? t("my.plot", "<yellow>Plot n°<id> <gray>(<size>)", "id", plot.id(), "size", nomTaille(plot.taille()))
                    : t("my.plot-editor", "<yellow>Plot n°<id> <gray>(<size>, plot de <owner>)",
                            "id", plot.id(), "size", nomTaille(plot.taille()), "owner", nom(plot.createur()));
            boutons.add(gui.button(libelle, null, p -> fiche(p, plot.id(), ici)));
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
        corps.add(t("plot.owner", "<gray>Créateur : <white><owner>", "owner", nom(plot.createur())));
        corps.add(plot.editeurs().isEmpty() ? t("plot.no-editors", "<gray>Éditeurs : <white>aucun")
                : t("plot.editors", "<gray>Éditeurs : <white><names>", "names",
                        String.join(", ", plot.editeurs().stream().map(KvMenu::nom).toList())));
        corps.add(plot.enPreparation() ? t("plot.state-preparing", "<gray>État : <yellow>en préparation")
                : plot.valide() ? t("plot.state-validated", "<gray>État : <green>validé")
                : t("plot.state-building", "<gray>État : <white>en travaux"));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("plot.tp", "<aqua>Se téléporter"), null, p -> {
            try {
                plots.teleporter(p, id);
            } catch (Refus r) {
                refus(p, r, ici);
            }
        }));
        if (plot.createur().equals(joueur.getUniqueId())) {
            boutons.add(gui.button(t("plot.editors-button", "<yellow>Éditeurs"),
                    t("plot.editors-tip", "<gray>Ajouter ou retirer des joueurs qui construisent avec toi."),
                    p -> editeurs(p, id, ici)));
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
