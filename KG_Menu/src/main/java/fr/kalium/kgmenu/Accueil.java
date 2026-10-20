package fr.kalium.kgmenu;

import fr.kalium.kgmenu.api.MenuProvider;
import fr.kalium.menu.api.Contenant;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.2.0 - accueil de Kal-Games (demande de LeKiwi06, 20/10/2026) : « quand on ouvre l'interface on voit "créer une
 * partie", "rejoindre une partie", "voir les parties en cours", puis on sélectionne le jeu et soit ça nous ouvre
 * l'interface de config d'une partie soit la liste des parties pour rejoindre » ; « tant qu'on a pas assez de parties en
 * cours, montrer toutes les parties avec le titre du jeu et l'hôte ». Les parties publiques ne sont plus proposées.
 *
 * Chaque écran existe en coffre (par défaut) et en fenêtre, selon le réglage « Paramètres › Affichage » du joueur ; les
 * deux ont la même organisation. Les jeux et les parties viennent des plugins de kal-games (MenuProvider).
 */
final class Accueil {

    /** Marques des coffres (pour les remplir à nouveau sans les refermer). */
    private enum Ecran { ACCUEIL, CREER, REJOINDRE, EN_COURS, AFFICHAGE }

    private final KgMenu plugin;

    Accueil(KgMenu plugin) {
        this.plugin = plugin;
    }

    private Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

    // ------------------------------------------------------------------ données des plugins

    private List<MenuProvider.Jeu> jeux(Player joueur) {
        List<MenuProvider.Jeu> jeux = new ArrayList<>();
        for (MenuProvider provider : plugin.providers()) {
            try {
                if (provider.owner().isEnabled()) {
                    jeux.addAll(provider.jeux(joueur));
                }
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Jeux de " + provider.owner().getName() + " indisponibles : " + e);
            }
        }
        return jeux;
    }

    private List<MenuProvider.Partie> parties(Player joueur) {
        List<MenuProvider.Partie> parties = new ArrayList<>();
        for (MenuProvider provider : plugin.providers()) {
            try {
                if (provider.owner().isEnabled()) {
                    parties.addAll(provider.parties(joueur));
                }
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Parties de " + provider.owner().getName() + " indisponibles : " + e);
            }
        }
        // Parties en attente d'abord, puis les plus remplies.
        parties.sort(Comparator.comparing(MenuProvider.Partie::lancee).thenComparing(p -> -p.joueurs()));
        return parties;
    }

    private List<MenuProvider.Partie> enCours(Player joueur) {
        List<MenuProvider.Partie> liste = new ArrayList<>();
        for (MenuProvider.Partie partie : parties(joueur)) {
            if (partie.lancee() && partie.regarder() != null) {
                liste.add(partie);
            }
        }
        return liste;
    }

    private MenuProvider.Derniere derniere(Player joueur) {
        MenuProvider.Derniere plusRecente = null;
        for (MenuProvider provider : plugin.providers()) {
            try {
                MenuProvider.Derniere d = provider.owner().isEnabled() ? provider.derniere(joueur) : null;
                if (d != null && (plusRecente == null || d.quand() > plusRecente.quand())) {
                    plusRecente = d;
                }
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Dernière partie de " + provider.owner().getName() + " indisponible : " + e);
            }
        }
        return plusRecente;
    }

    /** Icône d'un jeu d'un plugin pas encore adapté (config icones.<id>, sinon un papier). */
    private ItemStack icone(MenuProvider.Jeu jeu) {
        if (jeu.icone() != null) {
            return jeu.icone();
        }
        String defaut = switch (jeu.id()) {
            case "kg_bingo" -> "FILLED_MAP";
            case "kg_buildbattle" -> "BRICKS";
            default -> "PAPER";
        };
        Material materiau = Material.matchMaterial(plugin.getConfig().getString("icones." + jeu.id(), defaut));
        return new ItemStack(materiau == null || !materiau.isItem() || materiau.isAir() ? Material.PAPER : materiau);
    }

    private static ItemStack icone(ItemStack icone) {
        return icone == null ? new ItemStack(Material.PAPER) : icone;
    }

    /** Résultat d'une action (appelé après le clic) : null = fait (le coffre se ferme), sinon le refus en message. */
    private void resultat(Player joueur, Component refus) {
        if (refus == null || refus.equals(Component.empty())) {
            if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant) {
                joueur.closeInventory();
            }
            return;
        }
        plugin.message(joueur, refus, true);
    }

    private static String texte(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    // ------------------------------------------------------------------ accueil

    void accueil(Player joueur, Consumer<Player> retour) {
        if (plugin.coffres(joueur)) {
            accueilCoffre(joueur, retour);
        } else {
            accueilFenetre(joueur, retour);
        }
    }

    private void accueilCoffre(Player joueur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> accueil(p, retour);
        Contenant menu = Contenant.pour(joueur, 4, t("coffre.accueil", "<dark_gray>Kal-Games"), Ecran.ACCUEIL);
        List<MenuProvider.Partie> parties = parties(joueur);
        int enJeu = 0;
        for (MenuProvider.Partie partie : parties) {
            if (partie.lancee() && partie.regarder() != null) {
                enJeu++;
            }
        }
        menu.poser(11, Contenant.objet(Material.CRAFTING_TABLE, t("accueil.creer", "<green><bold>Créer une partie"),
                Contenant.lignes(t("accueil.creer-tip", "<gray>Choisissez un jeu et réglez votre partie : vous en êtes l'hôte."))),
                p -> creer(p, ici));
        List<Component> lignes = new ArrayList<>(Contenant.lignes(
                t("accueil.rejoindre-tip", "<gray>Toutes les parties ouvertes, tous jeux confondus.")));
        lignes.add(t("accueil.ouvertes", "<white><n></white> <gray>partie(s) ouverte(s)", "n", parties.size()));
        menu.poser(13, Contenant.objet(Material.ENDER_PEARL, t("accueil.rejoindre", "<aqua><bold>Rejoindre une partie"), lignes),
                p -> rejoindre(p, ici, 0));
        lignes = new ArrayList<>(Contenant.lignes(t("accueil.en-cours-tip", "<gray>Regarder une partie lancée, en spectateur.")));
        lignes.add(t("accueil.en-jeu", "<white><n></white> <gray>partie(s) en jeu", "n", enJeu));
        menu.poser(15, Contenant.objet(Material.SPYGLASS, t("accueil.en-cours", "<yellow><bold>Parties en cours"), lignes),
                p -> enCours(p, ici, 0));
        MenuProvider.Derniere derniere = derniere(joueur);
        if (derniere != null) {
            List<Component> l = new ArrayList<>();
            l.add(derniere.nom());
            l.addAll(Contenant.lignes(t("accueil.derniere-tip", "<gray>Même jeu, mêmes réglages : vous en êtes l'hôte.")));
            menu.poser(22, Contenant.objet(icone(derniere.icone()), t("accueil.derniere", "<gold>Recréer ma dernière partie"), l),
                    p -> Contenant.apres(() -> resultat(p, derniere.recreer().apply(p))));
        }
        // Autres boutons des plugins (rare) : rangée du bas, de part et d'autre de la dernière partie.
        int[] places = {19, 20, 21, 23, 24, 25};
        int i = 0;
        for (MenuProvider.Entry entry : plugin.extrasFor(joueur)) {
            if (i >= places.length) {
                break;
            }
            menu.poser(places[i++], Contenant.objet(Material.PAPER, entry.label(),
                    entry.tooltip() == null ? List.of() : Contenant.lignes(entry.tooltip())),
                    p -> Contenant.saisie(p, () -> entry.open().accept(p, ici)));
        }
        menu.barre();
        sortie(menu, retour);
        menu.poser(menu.bas(7), Contenant.objet(Material.COMPARATOR, t("accueil.parametres", "<white>Paramètres"),
                Contenant.lignes(t("accueil.parametres-tip", "<gray>Affichage : menus en coffres ou en fenêtres."))),
                p -> affichage(p, ici));
        menu.ouvrir(joueur);
        plugin.echap(joueur, menu, retour);
    }

    /** « Retour » (porte) si l'écran a été ouvert depuis un autre menu, sinon « Fermer » (barrière), colonne 5. */
    private void sortie(Contenant menu, Consumer<Player> retour) {
        if (retour != null) {
            menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, t("coffre.retour", "<white>Retour"), List.of()), retour);
        } else {
            menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("coffre.fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
        }
    }

    private void accueilFenetre(Player joueur, Consumer<Player> retour) {
        Consumer<Player> ici = p -> accueil(p, retour);
        List<ActionButton> boutons = new ArrayList<>();
        List<MenuProvider.Partie> parties = parties(joueur);
        boutons.add(plugin.gui().button(t("accueil.creer", "<green><bold>Créer une partie"),
                t("accueil.creer-tip", "<gray>Choisissez un jeu et réglez votre partie : vous en êtes l'hôte."), p -> creer(p, ici)));
        boutons.add(plugin.gui().button(t("accueil.rejoindre-f", "<aqua><bold>Rejoindre une partie</bold> <dark_gray>(<n>)", "n", parties.size()),
                t("accueil.rejoindre-tip", "<gray>Toutes les parties ouvertes, tous jeux confondus."), p -> rejoindre(p, ici, 0)));
        boutons.add(plugin.gui().button(t("accueil.en-cours", "<yellow><bold>Parties en cours"),
                t("accueil.en-cours-tip", "<gray>Regarder une partie lancée, en spectateur."), p -> enCours(p, ici, 0)));
        MenuProvider.Derniere derniere = derniere(joueur);
        if (derniere != null) {
            boutons.add(plugin.gui().button(t("accueil.derniere", "<gold>Recréer ma dernière partie"),
                    Component.join(JoinConfiguration.newlines(), derniere.nom(),
                            t("accueil.derniere-tip", "<gray>Même jeu, mêmes réglages : vous en êtes l'hôte.")),
                    p -> Contenant.apres(() -> resultat(p, derniere.recreer().apply(p)))));
        }
        for (MenuProvider.Entry entry : plugin.extrasFor(joueur)) {
            boutons.add(plugin.gui().button(entry.label(), entry.tooltip(), p -> entry.open().accept(p, ici)));
        }
        boutons.add(plugin.gui().button(t("accueil.parametres", "<white>Paramètres"),
                t("accueil.parametres-tip", "<gray>Affichage : menus en coffres ou en fenêtres."), p -> affichage(p, ici)));
        // Échap = « Retour » (bouton de sortie de la fenêtre) quand l'accueil a été ouvert depuis un autre menu.
        plugin.gui().open(joueur, t("menu.games-title", "<gold><bold>Mini-jeux Kal-Games"),
                List.of(t("accueil.intro", "<gray>Créez une partie et invitez vos amis, ou rejoignez une partie ouverte.")),
                List.of(), boutons, retour == null ? null : plugin.gui().button(t("menu.back", "<gray>Retour"), null, retour::accept), 1);
    }

    // ------------------------------------------------------------------ créer : choix du jeu

    void creer(Player joueur, Consumer<Player> retour) {
        creer(joueur, retour, 0);
    }

    private void creer(Player joueur, Consumer<Player> retour, int page) {
        List<MenuProvider.Jeu> jeux = jeux(joueur);
        if (!plugin.coffres(joueur)) {
            List<ActionButton> boutons = new ArrayList<>();
            Consumer<Player> ici = p -> creer(p, retour, 0);
            for (MenuProvider.Jeu jeu : jeux) {
                Component tip = jeu.lignes().isEmpty() ? null : Component.join(JoinConfiguration.newlines(), jeu.lignes());
                Component nom = jeu.joueurs() < 0 ? jeu.nom()
                        : jeu.nom().append(t("menu.players", " <dark_gray>| <gray><n> en jeu", "n", jeu.joueurs()));
                boutons.add(plugin.gui().button(nom, tip, p -> jeu.creer().accept(p, ici)));
            }
            plugin.gui().open(joueur, t("creer.titre", "<green><bold>Créer une partie"),
                    List.of(jeux.isEmpty() ? t("menu.games-empty", "<gray>Aucun mini-jeu n'est disponible pour le moment.")
                            : t("creer.intro", "<gray>Choisissez le jeu de votre partie.")), List.of(), boutons,
                    plugin.gui().button(t("menu.back", "<gray>Retour"), null, retour::accept), 1);
            return;
        }
        Contenant menu = Contenant.pour(joueur, t("creer.titre-coffre", "<dark_gray>Créer : choisir le jeu"), Ecran.CREER);
        Consumer<Player> ici = p -> creer(p, retour, page);
        int debut = page * Contenant.PAR_PAGE;
        for (int i = debut; i < jeux.size() && i < debut + Contenant.PAR_PAGE; i++) {
            MenuProvider.Jeu jeu = jeux.get(i);
            List<Component> lignes = new ArrayList<>();
            for (Component ligne : jeu.lignes()) {
                lignes.addAll(Contenant.lignes(ligne));
            }
            if (jeu.joueurs() >= 0) {
                lignes.add(t("creer.en-jeu", "<gray>Joueurs en jeu : <white><n>", "n", jeu.joueurs()));
            }
            lignes.add(t("creer.clic", "<yellow>Clic : régler la partie"));
            boolean ancien = jeu.icone() == null; // plugin pas encore adapté : son menu est une fenêtre
            menu.poser(i - debut, Contenant.objet(icone(jeu), jeu.nom(), lignes), p -> {
                if (ancien) {
                    Contenant.saisie(p, () -> jeu.creer().accept(p, ici));
                } else {
                    Contenant.apres(() -> jeu.creer().accept(p, ici));
                }
            });
        }
        if (jeux.isEmpty()) {
            menu.poser(22, Contenant.objet(Material.PAPER, t("menu.games-empty", "<gray>Aucun mini-jeu n'est disponible pour le moment."), List.of()), null);
        }
        menu.barre();
        pages(menu, page, jeux.size(), p -> creer(p, retour, page - 1), p -> creer(p, retour, page + 1));
        sortie(menu, retour);
        menu.ouvrir(joueur);
        plugin.echap(joueur, menu, retour);
    }

    private void pages(Contenant menu, int page, int total, Consumer<Player> avant, Consumer<Player> apres) {
        if (page > 0) {
            menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, t("coffre.page-avant", "<white>Page précédente"), List.of()), avant);
        }
        if ((page + 1) * Contenant.PAR_PAGE < total) {
            menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, t("coffre.page-apres", "<white>Page suivante"), List.of()), apres);
        }
    }

    // ------------------------------------------------------------------ rejoindre / parties en cours

    private List<Component> lignesPartie(MenuProvider.Partie partie, boolean regarder) {
        List<Component> lignes = new ArrayList<>();
        lignes.add(t("partie.hote", "<gray>Hôte : <white><hote>", "hote", partie.hote() == null ? "?" : partie.hote()));
        lignes.add(partie.places() > 0
                ? t("partie.joueurs", "<gray>Joueurs : <white><n>/<max>", "n", partie.joueurs(), "max", partie.places())
                : t("partie.joueurs-seul", "<gray>Joueurs : <white><n>", "n", partie.joueurs()));
        if (regarder) {
            lignes.add(t("partie.clic-regarder", "<yellow>Clic : regarder la partie"));
        } else {
            lignes.add(partie.lancee() ? t("partie.lancee", "<gold>En jeu : vous jouerez la manche suivante.")
                    : t("partie.attente", "<green>En attente de joueurs."));
            lignes.add(t("partie.clic-rejoindre", "<yellow>Clic : rejoindre la partie"));
        }
        return lignes;
    }

    /** Libellé d'un bouton de fenêtre « jeu · hôte » : le nom du jeu est raccourci s'il ferait défiler le bouton. */
    private Component libelle(MenuProvider.Partie partie) {
        String hote = partie.hote() == null ? "?" : partie.hote();
        String jeu = texte(partie.jeu());
        int place = plugin.largeurBoutons() - 20 - KgMenu.largeur(" · " + hote);
        if (KgMenu.largeur(jeu) > place) {
            while (jeu.length() > 3 && KgMenu.largeur(jeu + "…") > place) {
                jeu = jeu.substring(0, jeu.length() - 1);
            }
            jeu = jeu.trim() + "…";
        }
        return t("partie.libelle", "<white><jeu> <dark_gray>· <gray><hote>", "jeu", jeu, "hote", hote);
    }

    void rejoindre(Player joueur, Consumer<Player> retour, int page) {
        List<MenuProvider.Partie> parties = parties(joueur);
        if (!plugin.coffres(joueur)) {
            List<ActionButton> boutons = new ArrayList<>();
            boutons.add(plugin.gui().button(t("rejoindre.code", "<aqua>Entrer un code"), null, p -> code(p, q -> rejoindre(q, retour, 0))));
            for (MenuProvider.Partie partie : parties) {
                boutons.add(plugin.gui().button(libelle(partie),
                        Component.join(JoinConfiguration.newlines(), lignesPartie(partie, false)),
                        p -> Contenant.apres(() -> resultat(p, partie.rejoindre().apply(p)))));
            }
            plugin.gui().open(joueur, t("rejoindre.titre", "<aqua><bold>Rejoindre une partie"),
                    List.of(parties.isEmpty() ? t("rejoindre.vide", "<gray>Aucune partie ouverte pour l'instant : créez la vôtre !")
                            : t("rejoindre.intro", "<gray>Choisissez une partie, ou entrez le code donné par l'hôte.")),
                    List.of(), boutons, plugin.gui().button(t("menu.back", "<gray>Retour"), null, retour::accept), 1);
            return;
        }
        Contenant menu = Contenant.pour(joueur, t("rejoindre.titre-coffre", "<dark_gray>Rejoindre une partie"), Ecran.REJOINDRE);
        int debut = page * Contenant.PAR_PAGE;
        for (int i = debut; i < parties.size() && i < debut + Contenant.PAR_PAGE; i++) {
            MenuProvider.Partie partie = parties.get(i);
            menu.poser(i - debut, Contenant.objet(icone(partie.icone()), partie.jeu(), lignesPartie(partie, false)),
                    p -> Contenant.apres(() -> resultat(p, partie.rejoindre().apply(p))));
        }
        Consumer<Player> ici = p -> rejoindre(p, retour, page);
        menu.barre();
        if (parties.isEmpty()) {
            menu.poser(22, Contenant.objet(Material.PAPER, t("rejoindre.vide", "<gray>Aucune partie ouverte pour l'instant : créez la vôtre !"), List.of()), null);
            menu.poser(menu.bas(4), Contenant.objet(Material.CRAFTING_TABLE, t("accueil.creer", "<green><bold>Créer une partie"), List.of()),
                    p -> creer(p, ici));
        }
        pages(menu, page, parties.size(), p -> rejoindre(p, retour, page - 1), p -> rejoindre(p, retour, page + 1));
        menu.poser(menu.bas(3), Contenant.objet(Material.NAME_TAG, t("rejoindre.code", "<aqua>Entrer un code"),
                Contenant.lignes(t("rejoindre.code-tip", "<gray>Pour une partie qui n'est pas dans la liste."))),
                p -> code(p, ici));
        sortie(menu, retour);
        menu.ouvrir(joueur);
        plugin.echap(joueur, menu, retour);
    }

    void enCours(Player joueur, Consumer<Player> retour, int page) {
        List<MenuProvider.Partie> parties = enCours(joueur);
        if (!plugin.coffres(joueur)) {
            List<ActionButton> boutons = new ArrayList<>();
            for (MenuProvider.Partie partie : parties) {
                boutons.add(plugin.gui().button(libelle(partie),
                        Component.join(JoinConfiguration.newlines(), lignesPartie(partie, true)),
                        p -> Contenant.apres(() -> resultat(p, partie.regarder().apply(p)))));
            }
            plugin.gui().open(joueur, t("en-cours.titre", "<yellow><bold>Parties en cours"),
                    List.of(parties.isEmpty() ? t("en-cours.vide", "<gray>Aucune partie n'est lancée pour l'instant.")
                            : t("en-cours.intro", "<gray>Vous regarderez la partie en spectateur (vol libre).")),
                    List.of(), boutons, plugin.gui().button(t("menu.back", "<gray>Retour"), null, retour::accept), 1);
            return;
        }
        Contenant menu = Contenant.pour(joueur, t("en-cours.titre-coffre", "<dark_gray>Parties en cours"), Ecran.EN_COURS);
        int debut = page * Contenant.PAR_PAGE;
        for (int i = debut; i < parties.size() && i < debut + Contenant.PAR_PAGE; i++) {
            MenuProvider.Partie partie = parties.get(i);
            menu.poser(i - debut, Contenant.objet(icone(partie.icone()), partie.jeu(), lignesPartie(partie, true)),
                    p -> Contenant.apres(() -> resultat(p, partie.regarder().apply(p))));
        }
        if (parties.isEmpty()) {
            menu.poser(22, Contenant.objet(Material.PAPER, t("en-cours.vide", "<gray>Aucune partie n'est lancée pour l'instant."), List.of()), null);
        }
        menu.barre();
        pages(menu, page, parties.size(), p -> enCours(p, retour, page - 1), p -> enCours(p, retour, page + 1));
        sortie(menu, retour);
        menu.ouvrir(joueur);
        plugin.echap(joueur, menu, retour);
    }

    /** « Entrer un code » : petite fenêtre de saisie ; chaque plugin essaie le code. */
    private void code(Player joueur, Consumer<Player> retour) {
        Contenant.saisie(joueur, () -> plugin.gui().open(joueur, t("rejoindre.code-titre", "<aqua><bold>Entrer un code"),
                List.of(t("rejoindre.code-body", "<gray>Saisissez le code donné par l'hôte de la partie.")),
                List.of(plugin.gui().text("code", t("rejoindre.code-champ", "Code de la partie"), "", 8)),
                List.of(plugin.gui().form(t("rejoindre.code-ok", "<green>Rejoindre"), null, (p, vue) -> {
                            String code = vue.getText("code");
                            Component resultat = essayerCode(p, code == null ? "" : code.trim());
                            if (resultat != null) {
                                p.sendMessage(resultat);
                            }
                        })),
                plugin.gui().button(t("menu.back", "<gray>Retour"), null, retour::accept), 1));
    }

    /** null si le joueur est entré dans une partie, sinon le motif du refus. */
    private Component essayerCode(Player joueur, String code) {
        if (!code.isEmpty()) {
            for (MenuProvider provider : plugin.providers()) {
                try {
                    Component resultat = provider.owner().isEnabled() ? provider.code(joueur, code) : null;
                    if (resultat != null) {
                        return resultat.equals(Component.empty()) ? null : resultat;
                    }
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Code refusé par " + provider.owner().getName() + " : " + e);
                }
            }
        }
        return t("rejoindre.code-inconnu", "<red>Aucune partie ne correspond à ce code.");
    }

    // ------------------------------------------------------------------ paramètres › affichage

    void affichage(Player joueur, Consumer<Player> retour) {
        boolean coffres = plugin.coffres(joueur);
        Component titreMode = coffres ? t("affichage.coffres", "<white>Menus : <green>coffres")
                : t("affichage.fenetres", "<white>Menus : <aqua>fenêtres");
        Component tipMode = coffres ? t("affichage.coffres-tip", "<gray>Clic : passer aux menus en fenêtres (boutons).")
                : t("affichage.fenetres-tip", "<gray>Clic : passer aux menus en coffres (objets).");
        Consumer<Player> basculer = p -> {
            boolean nouveau = !plugin.coffres(p);
            plugin.coffres(p, nouveau);
            Component fait = nouveau ? t("affichage.passe-coffres", "<green>Menus de Kal-Games en coffres.")
                    : t("affichage.passe-fenetres", "<green>Menus de Kal-Games en fenêtres.");
            if (nouveau) {
                affichage(p, retour);
                plugin.message(p, fait, false);
            } else {
                Contenant.saisie(p, () -> affichage(p, retour));
                p.sendMessage(fait);
            }
        };
        if (!coffres) {
            plugin.gui().open(joueur, t("affichage.titre", "<white><bold>Paramètres › Affichage"),
                    List.of(t("affichage.body", "<gray>Réglages qui ne concernent que vous.")), List.of(),
                    List.of(plugin.gui().button(titreMode, tipMode, basculer::accept)),
                    plugin.gui().button(t("menu.back", "<gray>Retour"), null, retour::accept), 1);
            return;
        }
        Contenant menu = Contenant.pour(joueur, 3, t("affichage.titre-coffre", "<dark_gray>Paramètres › Affichage"), Ecran.AFFICHAGE);
        menu.poser(4, Contenant.objet(Material.CHEST, titreMode, Contenant.lignes(tipMode)), basculer);
        menu.barre();
        sortie(menu, retour);
        menu.ouvrir(joueur);
        plugin.echap(joueur, menu, retour);
    }
}
