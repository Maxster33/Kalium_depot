package fr.kalium.kvbuildbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;

/**
 * Interface admin du Build Battle (catalogue « Interfaces » de KLM_Menu, ou /bbadmin) : poser les coins et points
 * d'apparition à sa position, capturer la boîte et la salle d'attente, générer l'arène, aller voir le résultat. Les
 * commandes texte de /bbadmin font la même chose.
 */
final class MenuAdmin {

    private final KVBuildBattle plugin;
    private final Lang lang;
    private final Gui gui;

    MenuAdmin(KVBuildBattle plugin, Lang lang, Gui gui) {
        this.plugin = plugin;
        this.lang = lang;
        this.gui = gui;
    }

    private Component t(String cle, String def, Object... paires) {
        return lang.c(cle, def, paires);
    }

    void ouvrir(Player joueur, Consumer<Player> retour) {
        Arene a = plugin.arene();
        List<Component> corps = new ArrayList<>();
        corps.add(t("admin.boite", "<gray>Boîte : <etat>", "etat", a.boiteCapturee()
                ? t("admin.capturee", "<green>capturée") : t("admin.non-capturee", "<red>non capturée")));
        corps.add(t("admin.arene", "<gray>Arène : <etat>", "etat", a.boitesCollees()
                ? t("admin.generee", "<green>générée (<colonnes> x <boites>)", "colonnes", a.colonnes(),
                        "boites", a.boitesParColonne())
                : t("admin.non-generee", "<red>non générée")));
        corps.add(t("admin.salle", "<gray>Salle d'attente : <etat>", "etat", a.salleCapturee()
                ? t("admin.capturee", "<green>capturée") : t("admin.non-capturee", "<red>non capturée")));
        corps.add(t("admin.aide", "<gray>Chaque coin ou point d'apparition est pris à <white>ta position</white> "
                + "(et ton orientation pour les points d'apparition)."));

        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("admin.aller-monde", "<aqua>Aller dans le monde du Build Battle"),
                t("admin.aller-monde-info", "<gray>Monde vide : pour construire la salle d'attente."), plugin::allerAuMonde));
        boutons.add(gui.button(t("admin.voir-arene", "<aqua>Voir l'arène générée"),
                t("admin.voir-arene-info", "<gray>Première boîte de la première colonne."), p -> {
                    Location l = a.apparitionBoite(0, 0);
                    if (l == null) p.sendMessage("§cL'arène n'est pas encore générée.");
                    else p.teleport(l);
                }));
        boutons.add(poser("boite.pos1", "<yellow>Boîte : coin 1", "<gray>Un coin de la boîte entière (murs compris)."));
        boutons.add(poser("boite.pos2", "<yellow>Boîte : coin 2", "<gray>Le coin opposé de la boîte entière."));
        boutons.add(poser("boite.zone1", "<yellow>Zone constructible : coin 1", "<gray>Un coin de la zone où l'équipe construit."));
        boutons.add(poser("boite.zone2", "<yellow>Zone constructible : coin 2", "<gray>Le coin opposé de la zone constructible."));
        boutons.add(poser("boite.apparition", "<yellow>Apparition de l'équipe",
                "<gray>Où l'équipe apparaît dans sa boîte (position et orientation)."));
        boutons.add(gui.button(t("admin.capturer-boite", "<green>Capturer la boîte"),
                t("admin.capturer-boite-info", "<gray>Enregistre la boîte, sa zone constructible et le point d'apparition."),
                p -> a.capturerBoite(p)));
        boutons.add(gui.button(t("admin.generer", "<gold>Générer l'arène"),
                t("admin.generer-info", "<gray>Recopie la boîte capturée en <colonnes> colonnes de <boites> dans le monde du "
                        + "Build Battle.", "colonnes", a.colonnes(), "boites", a.boitesParColonne()),
                p -> gui.confirm(p, t("admin.generer", "<gold>Générer l'arène"),
                        t("admin.generer-confirmer", "<gray>Recopier la boîte <total> fois ? Les boîtes déjà générées sont "
                                + "remplacées (constructions comprises).", "total", a.colonnes() * a.boitesParColonne()),
                        a::generer, q -> ouvrir(q, retour))));
        boutons.add(gui.button(t("admin.voir-salle", "<aqua>Voir une salle d'attente"),
                t("admin.voir-salle-info", "<gray>Salle de la première colonne."), p -> {
                    Location l = a.apparitionSalle(0);
                    if (l == null) p.sendMessage("§cLa salle d'attente n'est pas encore capturée.");
                    else p.teleport(l);
                }));
        boutons.add(poser("salle.pos1", "<yellow>Salle : coin 1", "<gray>Un coin de la salle d'attente."));
        boutons.add(poser("salle.pos2", "<yellow>Salle : coin 2", "<gray>Le coin opposé de la salle d'attente."));
        boutons.add(poser("salle.apparition", "<yellow>Salle : apparition",
                "<gray>Où les joueurs apparaissent dans la salle (position et orientation)."));
        boutons.add(gui.button(t("admin.capturer-salle", "<green>Capturer la salle d'attente"),
                t("admin.capturer-salle-info", "<gray>Enregistre la salle et la recopie pour chaque partie (anciennes copies "
                        + "remplacées)."), p -> a.capturerSalle(p)));
        gui.open(joueur, t("admin.titre", "<gold>Build Battle : arène"), corps, List.of(), boutons,
                retour == null ? null : gui.button(t("admin.retour", "<gray>Retour"), null, retour::accept), 2);
        lang.saveIfNeeded();
    }

    /** Bouton qui pose une position à l'endroit du joueur, puis rouvre le menu. */
    private ActionButton poser(String cle, String nom, String info) {
        return gui.button(t("admin." + cle, nom), t("admin." + cle + "-info", info), p -> {
            plugin.arene().poser(p, cle);
            p.sendMessage("§a" + cle + " posé : §7" + Arene.texte(p.getLocation()));
            ouvrir(p, null);
        });
    }
}
