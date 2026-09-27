package fr.kalium.kgbuildbattle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;

/**
 * Menus du Build Battle sur kal-games (ouverts depuis KG_Menu) : file publique solo / duo / trio / squad (tempo
 * normal), créer une partie privée (tempo, taille des équipes, thèmes écrits), rejoindre une partie listée ou par code.
 */
final class Menus {

    private static final String[] TAILLES = {"Solo", "Duo", "Trio", "Squad"};

    private final Parties parties;
    private final Lang lang;
    private final Gui gui;

    Menus(Parties parties, Lang lang, Gui gui) {
        this.parties = parties;
        this.lang = lang;
        this.gui = gui;
    }

    Component t(String cle, String def, Object... paires) {
        return lang.c(cle, def, paires);
    }

    void ouvrir(Player joueur, Consumer<Player> retour) {
        List<ActionButton> boutons = new ArrayList<>();
        for (int taille = 1; taille <= 4; taille++) {
            int n = taille;
            boutons.add(gui.button(t("menu.public-" + n, "<green>File publique : " + TAILLES[n - 1]),
                    t("menu.public-info", "<gray>Tempo normal (5 min). Tu rejoins la salle d'attente sur Kanvas ; la partie "
                            + "démarre dès qu'il y a au moins 2 équipes."),
                    p -> parties.envoyerPublic(p, n)));
        }
        boutons.add(gui.button(t("menu.creer", "<gold>Créer une partie privée"),
                t("menu.creer-info", "<gray>Tu choisis le tempo, la taille des équipes et le mode des thèmes."),
                p -> creer(p, retour)));
        boutons.add(gui.button(t("menu.code", "<aqua>Rejoindre avec un code"), null, p -> rejoindreCode(p, retour)));
        for (Partie partie : parties.listees(10)) {
            boutons.add(gui.button(t("menu.partie", "<yellow><hote> <gray>· <taille> · <tempo> · <nb>/<max>",
                            "hote", partie.nomHote, "taille", TAILLES[partie.tailleEquipes - 1], "tempo",
                            partie.tempo.libelle(), "nb", partie.joueurs.size(), "max", partie.places()),
                    t("menu.partie-info", "<gray>Code <white><code></white><gray>. Clique pour rejoindre.", "code", partie.code),
                    p -> rejoindre(p, partie)));
        }
        List<Component> corps = List.of(t("menu.corps", "<gray>Construis sur un thème avec ton équipe, puis vote pour "
                + "les constructions des autres. La partie se joue sur <white>Kanvas</white>."));
        gui.open(joueur, t("menu.titre", "<gold><bold>Build Battle"), corps, List.of(), boutons,
                retour == null ? null : gui.button(t("menu.retour", "<gray>Retour"), null, retour::accept), 2);
        lang.saveIfNeeded();
    }

    private void creer(Player joueur, Consumer<Player> retour) {
        List<String> tempos = new ArrayList<>();
        List<Component> libellesTempos = new ArrayList<>();
        for (Tempo tp : Tempo.values()) {
            tempos.add(tp.name());
            libellesTempos.add(Component.text(tp.libelle()));
        }
        List<String> tailles = List.of("1", "2", "3", "4");
        List<Component> libellesTailles = new ArrayList<>();
        for (String s : TAILLES) libellesTailles.add(Component.text(s));
        gui.open(joueur, t("menu.creer", "<gold>Créer une partie privée"),
                List.of(t("menu.creer-corps", "<gray>Jusqu'à <max> équipes. Tu lanceras la partie depuis la salle "
                        + "d'attente sur Kanvas.", "max", parties.equipesMax())),
                List.of(gui.choice("tempo", t("menu.tempo", "Tempo"), tempos, libellesTempos, Tempo.NORMAL.name()),
                        gui.choice("taille", t("menu.taille", "Taille des équipes"), tailles, libellesTailles, "1"),
                        gui.toggle("ecrits", t("menu.ecrits", "Thèmes écrits par les joueurs"), false)),
                List.of(gui.form(t("menu.valider", "<green>Créer et rejoindre"), null, (p, vue) -> {
                    int taille;
                    try {
                        taille = Integer.parseInt(vue.getText("taille"));
                    } catch (RuntimeException e) {
                        taille = 1;
                    }
                    Partie partie = parties.creer(p, Tempo.lire(vue.getText("tempo")), taille,
                            Boolean.TRUE.equals(vue.getBoolean("ecrits")));
                    p.sendMessage("§aPartie privée créée : code §f" + partie.code + "§a.");
                    parties.envoyerPrive(p, partie);
                })),
                gui.button(t("menu.retour", "<gray>Retour"), null, p -> ouvrir(p, retour)), 1);
        lang.saveIfNeeded();
    }

    private void rejoindreCode(Player joueur, Consumer<Player> retour) {
        gui.open(joueur, t("menu.code", "<aqua>Rejoindre avec un code"), List.of(),
                List.of(gui.text("code", t("menu.code-champ", "Code de la partie"), "", 4)),
                List.of(gui.form(t("menu.rejoindre", "<green>Rejoindre"), null, (p, vue) -> {
                    Partie partie = parties.parCode(vue.getText("code"));
                    if (partie == null) {
                        p.sendMessage("§cAucune partie avec ce code.");
                        return;
                    }
                    rejoindre(p, partie);
                })),
                gui.button(t("menu.retour", "<gray>Retour"), null, p -> ouvrir(p, retour)), 1);
    }

    private void rejoindre(Player joueur, Partie partie) {
        if (!parties.rejoindre(joueur, partie)) {
            joueur.sendMessage("§cCette partie est complète.");
            return;
        }
        parties.envoyerPrive(joueur, partie);
    }
}
