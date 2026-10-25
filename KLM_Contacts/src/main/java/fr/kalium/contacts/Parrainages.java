package fr.kalium.contacts;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;

/**
 * 1.5.0 - parrainage et liste blanche de Kixster (demande de LeKiwi06, 23/10/2026 ; maquette validée le même jour).
 * Données sur le proxy (KaliumRelay 1.10.0, classe Parrainage) ; temps de jeu et récompenses comptés sur Kixster
 * (KS_Parrainage). Menus en coffre (outil Contenant de KLM_Menu) ; seule la saisie d'un pseudo ou du pseudo Discord est
 * une petite fenêtre.
 *
 * - « Parrainer un joueur » (menu Contacts, /parrainage) : têtes des amis sans parrain (1.5.1 : aussi ceux déjà
 *   whitelistés), « Autre pseudo »,
 *   « Mes filleuls » (temps de jeu, palier, prochain palier), « Propositions reçues ».
 * - Le filleul accepte ou refuse (petit coffre) ; il est alors ajouté à la liste blanche de Kixster à son arrivée.
 * - Refusé à l'entrée de Kixster (message « whitelist » du proxy) : acceptation d'un parrainage reçu, sinon fenêtre de
 *   demande aux admins avec le pseudo Discord.
 * - 1.5.1 : « whitelist » dans tous les textes (nouvelles clés de lang.yml) ; refus d'un parrainage croisé.
 * - Modération › Demandes de whitelist (permission klmcontacts.whitelist) : accepter / refuser chaque demande, et
 *   l'historique des parrainages (qui a fait venir qui).
 * - 1.5.2 (LeKiwi06, 26/10/2026 : « j'aimerai que tu me créer un historique pour les demandes de whitelist traités ») :
 *   « Demandes traitées » : chaque demande acceptée ou refusée (la plus récente d'abord), par qui, quand.
 */
final class Parrainages {

    /** Parrain ou filleul : état « A » (accepté) ou « P » (proposé), secondes de jeu actives, palier atteint. */
    record Filleul(UUID id, String nom, boolean accepte, long secondes, int palier) {
    }

    record Personne(UUID id, String nom, String serveur) {
    }

    /** Ce que les menus affichent pour un joueur (réponse « plist » du proxy). */
    static final class Etat {
        boolean surKixster;
        /** Faux tant que Kixster n'a jamais envoyé sa liste blanche au proxy. */
        boolean disponible;
        Personne parrain;
        final List<Personne> propositions = new ArrayList<>();
        final List<Filleul> filleuls = new ArrayList<>();
        final List<Personne> eligibles = new ArrayList<>();
        /** État de ma demande aux admins (« attente », « acceptee », « refusee »), ou vide. */
        String demande = "";
        /** Paliers : {heures, valeur du coffre}. */
        final List<double[]> paliers = new ArrayList<>();
    }

    record Demande(UUID id, String nom, String discord, long date, boolean bedrock) {
    }

    /** 1.5.2 : demande traitée ; par = pseudo de l'admin, ou « parrainage » ; traite = 0 si la date est inconnue. */
    record Traitee(UUID id, String nom, String discord, long date, boolean acceptee, String par, long traite,
                   boolean bedrock) {
    }

    record Historique(UUID filleul, String nomFilleul, UUID parrain, String nomParrain, long date, long secondes) {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));
    private static final String MARQUE = "klm_contacts_parrainage";

    private final KlmContacts plugin;
    private final Relais relais;
    private final Lang lang;
    private final Gui gui;

    Parrainages(KlmContacts plugin, Relais relais, Lang lang, Gui gui) {
        this.plugin = plugin;
        this.relais = relais;
        this.lang = lang;
        this.gui = gui;
    }

    private Component t(String cle, String def, Object... paires) {
        return lang.c(cle, def, paires);
    }

    private List<Component> l(String cle, String def, Object... paires) {
        return Contenant.lignes(t(cle, def, paires));
    }

    // ------------------------------------------------------------------ relais

    private void etat(Player joueur, Consumer<Etat> suite) {
        relais.appeler(joueur, "plist", Map.of(), reponse -> {
            if (reponse == null || !reponse.ok()) {
                suite.accept(null);
                return;
            }
            Etat e = new Etat();
            for (String ligne : reponse.lignes()) {
                String[] c = ligne.split("\t", -1);
                try {
                    switch (c[0]) {
                        case "K" -> {
                            e.surKixster = Boolean.parseBoolean(c[1]);
                            e.disponible = Boolean.parseBoolean(c[2].trim());
                        }
                        case "T" -> {
                            for (String p : c[1].trim().split(",")) {
                                String[] hv = p.split(":");
                                if (hv.length == 2) {
                                    e.paliers.add(new double[]{Double.parseDouble(hv[0]), Double.parseDouble(hv[1])});
                                }
                            }
                        }
                        case "P" -> e.parrain = new Personne(UUID.fromString(c[1]), c[2].trim(), "");
                        case "I" -> e.propositions.add(new Personne(UUID.fromString(c[1]), c[2].trim(), ""));
                        case "F" -> e.filleuls.add(new Filleul(UUID.fromString(c[1]), c[2], c[3].equals("A"),
                                Long.parseLong(c[4]), Integer.parseInt(c[5].trim())));
                        case "E" -> e.eligibles.add(new Personne(UUID.fromString(c[1]), c[2], c.length > 3 ? c[3].trim() : ""));
                        case "D" -> e.demande = c[1].trim();
                        default -> {
                        }
                    }
                } catch (RuntimeException ex) {
                    // ligne incomplète : ignorée
                }
            }
            e.eligibles.sort((a, b) -> a.serveur().isEmpty() != b.serveur().isEmpty() ? (a.serveur().isEmpty() ? 1 : -1)
                    : a.nom().compareToIgnoreCase(b.nom()));
            suite.accept(e);
        });
    }

    // ------------------------------------------------------------------ outils

    private static ItemStack tete(UUID id, String nom) {
        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        if (tete.getItemMeta() instanceof SkullMeta meta) {
            try {
                meta.setPlayerProfile(Bukkit.createProfile(id, nom.length() <= 16 ? nom : null));
            } catch (IllegalArgumentException e) {
                // pseudo refusé (ex. Bedrock « .Pseudo ») : tête sans nom
                meta.setPlayerProfile(Bukkit.createProfile(id));
            }
            tete.setItemMeta(meta);
        }
        return tete;
    }

    /** Durée lisible : « 14 h 05 », « 35 min ». */
    static String duree(long secondes) {
        long minutes = secondes / 60;
        return minutes < 60 ? minutes + " min" : (minutes / 60) + " h " + String.format("%02d", minutes % 60);
    }

    private static String nombre(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private void message(Player joueur, Component texte, boolean refus) {
        Contenant.message(joueur, texte, refus);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ « Parrainer un joueur »

    /** Menu principal du parrainage (bouton du menu Contacts, /parrainage). */
    void ouvrir(Player joueur, Consumer<Player> retour) {
        ouvrir(joueur, retour, 0);
    }

    private void ouvrir(Player joueur, Consumer<Player> retour, int page) {
        etat(joueur, e -> {
            if (e == null) {
                return;
            }
            Contenant menu = Contenant.pour(joueur, t("parrainage.titre", "<dark_gray>Parrainer un joueur"), MARQUE);
            int pages = Math.max(1, (e.eligibles.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
            int p = Math.max(0, Math.min(pages - 1, page));
            List<Personne> visibles = e.eligibles.subList(p * Contenant.PAR_PAGE,
                    Math.min(e.eligibles.size(), (p + 1) * Contenant.PAR_PAGE));
            for (int i = 0; i < visibles.size(); i++) {
                Personne ami = visibles.get(i);
                List<Component> lignes = new ArrayList<>();
                lignes.add(ami.serveur().isEmpty() ? t("parrainage.ami-hors-ligne", "<gray>Hors ligne")
                        : t("parrainage.ami-en-ligne", "<green>En ligne <gray>sur <white><serveur>", "serveur",
                        plugin.menus().serveur(ami.serveur())));
                lignes.addAll(l("parrainage.ami-info", "<yellow>Clic : lui proposer de le parrainer sur Kixster."));
                menu.poser(i, Contenant.objet(tete(ami.id(), ami.nom()), t("parrainage.ami", "<white><nom>", "nom", ami.nom()),
                        lignes), j -> proposer(j, ami.nom(), k -> ouvrir(k, retour, p)));
            }
            menu.barre();
            if (p > 0) {
                menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, t("parrainage.precedente", "<gray>Page précédente"),
                        List.of()), j -> ouvrir(j, retour, p - 1));
            }
            if (p < pages - 1) {
                menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, t("parrainage.suivante", "<gray>Page suivante"),
                        List.of()), j -> ouvrir(j, retour, p + 1));
            }
            int acceptes = 0;
            for (Filleul f : e.filleuls) {
                if (f.accepte()) {
                    acceptes++;
                }
            }
            menu.poser(menu.bas(2), Contenant.objet(Material.TOTEM_OF_UNDYING,
                    t("parrainage.filleuls", "<gold>Mes filleuls (<n>)", "n", acceptes),
                    l("parrainage.filleuls-info", "<gray>Leur temps de jeu sur Kixster et vos prochains coffres.")),
                    j -> filleuls(j, k -> ouvrir(k, retour, p)));
            List<Component> aide = new ArrayList<>();
            aide.addAll(l("parrainage.aide-1b", "<gray>Fais venir un ami sur Kixster, ou parraine un joueur déjà "
                    + "whitelisté qui n'a pas de parrain : dès qu'il accepte, il est whitelisté."));
            aide.addAll(l("parrainage.aide-2", "<gray>Plus il joue, plus vos coffres de récompense (à tous les deux) sont "
                    + "gros : /rewards sur Kixster."));
            aide.addAll(l("parrainage.aide-3", "<red>Faire venir des joueurs qui ne respectent pas les règles peut te "
                    + "valoir une sanction."));
            if (e.parrain != null) {
                aide.add(t("parrainage.mon-parrain", "<gray>Ton parrain : <white><nom>", "nom", e.parrain.nom()));
            }
            menu.poser(menu.bas(3), Contenant.objet(Material.BOOK, t("parrainage.aide", "<yellow>Le parrainage"), aide), null);
            if (!e.disponible) {
                menu.poser(menu.bas(4), Contenant.objet(Material.BARRIER, t("parrainage.indisponible", "<red>Indisponible"),
                        l("parrainage.indisponible-info-2", "<gray>Kixster n'a pas encore envoyé sa whitelist : "
                                + "réessaie plus tard.")), null);
            } else if (!e.surKixster) {
                menu.poser(menu.bas(4), Contenant.objet(Material.BARRIER, t("parrainage.pas-kixster", "<red>Réservé aux joueurs de Kixster"),
                        l("parrainage.pas-kixster-info-2", "<gray>Il faut être whitelisté sur Kixster pour parrainer.")), null);
            } else {
                menu.poser(menu.bas(4), Contenant.objet(Material.NAME_TAG, t("parrainage.autre", "<green>Autre pseudo"),
                        l("parrainage.autre-info", "<gray>Parrainer un joueur qui n'est pas dans tes amis (déjà venu sur "
                                + "KaLium).")), j -> Contenant.saisie(j, () -> saisirPseudo(j, retour)));
            }
            menu.poser(menu.bas(5), retour == null
                    ? Contenant.objet(Material.BARRIER, t("parrainage.fermer", "<red>Fermer"), List.of())
                    : Contenant.objet(Material.OAK_DOOR, t("parrainage.retour", "<gray>Retour"), List.of()),
                    j -> {
                        if (retour == null) {
                            Contenant.fermer(j);
                        } else {
                            Contenant.saisie(j, () -> retour.accept(j));
                        }
                    });
            if (!e.propositions.isEmpty()) {
                menu.poser(menu.bas(6), Contenant.objet(Material.WRITABLE_BOOK,
                        t("parrainage.propositions", "<green>Propositions reçues (<n>)", "n", e.propositions.size()),
                        l("parrainage.propositions-info", "<gray>Des joueurs te proposent de te parrainer sur Kixster.")),
                        j -> propositions(j, k -> ouvrir(k, retour, p)));
            } else if (!e.surKixster && e.disponible) {
                menu.poser(menu.bas(6), Contenant.objet(Material.PAPER, t("parrainage.demander", "<aqua>Demander aux admins"),
                        l("parrainage.demander-info-2", "<gray>Pas de parrain ? Demande la whitelist de Kixster avec ton "
                                + "pseudo Discord.")), j -> demander(j, e.demande));
            }
            if (visibles.isEmpty()) {
                menu.poser(22, Contenant.objet(Material.GRAY_DYE, t("parrainage.vide", "<gray>Aucun ami à parrainer"),
                        l("parrainage.vide-info-2", "<gray>Tes amis ont tous déjà un parrain, ou tu n'as pas encore d'ami : "
                                + "utilise « Autre pseudo ».")), null);
            }
            menu.ouvrir(joueur);
            lang.saveIfNeeded();
        });
    }

    private void proposer(Player joueur, String cible, Consumer<Player> ensuite) {
        relais.appeler(joueur, "pparrainer", Map.of("target", cible), reponse -> {
            if (reponse == null) {
                return;
            }
            if (reponse.ok()) {
                message(joueur, t("parrainage.envoye", "<green>Proposition envoyée à <white><nom></white>.", "nom",
                        reponse.partie(0)), false);
            } else {
                message(joueur, switch (reponse.partie(0)) {
                    case "unknown" -> t("refus.inconnu", "<red>Joueur inconnu : il doit être venu au moins une fois sur KaLium.");
                    case "self" -> t("refus.soi", "<red>C'est toi.");
                    case "indisponible" -> t("parrainage.indisponible-info-2", "<gray>Kixster n'a pas encore envoyé sa "
                            + "whitelist : réessaie plus tard.");
                    case "not-allowed" -> t("parrainage.pas-kixster-info-2", "<gray>Il faut être whitelisté sur Kixster pour parrainer.");
                    case "already" -> t("parrainage.refus-deja-2", "<red><nom> a déjà un parrain.", "nom", reponse.partie(1));
                    case "cycle" -> t("parrainage.refus-cycle", "<red><nom> est ton parrain (ou celui de ton parrain) : il ne "
                            + "peut pas être ton filleul.", "nom", reponse.partie(1));
                    case "pending" -> t("parrainage.refus-attente", "<yellow>Tu as déjà proposé à <nom>.", "nom", reponse.partie(1));
                    case "too-many" -> t("parrainage.refus-trop", "<red>Trop de propositions en attente : annules-en d'abord.");
                    default -> t("refus.autre", "<red>Action impossible.");
                }, true);
            }
            if (ensuite != null) {
                ensuite.accept(joueur);
            }
        });
    }

    private void saisirPseudo(Player joueur, Consumer<Player> retour) {
        List<DialogInput> champs = List.of(gui.text("pseudo", t("parrainage.champ", "Pseudo du joueur"), "", 32));
        List<ActionButton> boutons = List.of(gui.form(t("parrainage.valider", "<green>Proposer le parrainage"), null, (j, vue) -> {
            String pseudo = vue.getText("pseudo");
            if (pseudo == null || pseudo.isBlank()) {
                ouvrir(j, retour);
                return;
            }
            proposer(j, pseudo.trim(), k -> ouvrir(k, retour));
        }));
        gui.open(joueur, t("parrainage.saisie-titre", "<gold>Parrainer un joueur"),
                List.of(t("parrainage.saisie-corps-2", "<gray>Écris son pseudo exact. Il doit être déjà venu sur KaLium et ne "
                        + "pas avoir de parrain.")), champs, boutons,
                gui.button(t("menu.retour", "<gray>Retour"), null, j -> ouvrir(j, retour)), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ « Mes filleuls »

    private void filleuls(Player joueur, Consumer<Player> retour) {
        etat(joueur, e -> {
            if (e == null) {
                return;
            }
            Contenant menu = Contenant.pour(joueur, t("filleuls.titre", "<dark_gray>Mes filleuls"), MARQUE);
            int n = Math.min(e.filleuls.size(), Contenant.PAR_PAGE);
            for (int i = 0; i < n; i++) {
                Filleul f = e.filleuls.get(i);
                List<Component> lignes = new ArrayList<>();
                if (f.accepte()) {
                    lignes.add(t("filleuls.temps", "<gray>Temps de jeu actif sur Kixster : <white><temps>", "temps", duree(f.secondes())));
                    lignes.add(t("filleuls.palier", "<gray>Paliers atteints : <white><n></white> sur <total>", "n", f.palier(),
                            "total", e.paliers.size()));
                    if (f.palier() < e.paliers.size()) {
                        double[] suivant = e.paliers.get(f.palier());
                        lignes.add(t("filleuls.suivant", "<yellow>Prochain : <heures> h, coffre de <valeur> chacun",
                                "heures", nombre(suivant[0]), "valeur", nombre(suivant[1])));
                    } else if (!e.paliers.isEmpty()) {
                        lignes.add(t("filleuls.fini", "<green>Tous les paliers sont atteints."));
                    }
                    menu.poser(i, Contenant.objet(tete(f.id(), f.nom()), t("filleuls.nom", "<gold><nom>", "nom", f.nom()), lignes), null);
                } else {
                    lignes.add(t("filleuls.attente", "<gray>Proposition en attente de réponse."));
                    lignes.add(t("filleuls.annuler", "<red>Clic : annuler la proposition."));
                    menu.poser(i, Contenant.objet(Material.CLOCK, t("filleuls.nom-attente", "<gray><nom>", "nom", f.nom()), lignes),
                            j -> confirmer(j, t("filleuls.annuler-titre", "<dark_gray>Annuler la proposition ?"), f.id(), f.nom(),
                                    k -> relais.appeler(k, "pannuler", Map.of("target", f.id().toString()), r -> {
                                        if (r != null && r.ok()) {
                                            message(k, t("filleuls.annule", "<gray>Proposition à <white><nom></white> annulée.",
                                                    "nom", f.nom()), false);
                                        }
                                        filleuls(k, retour);
                                    }), k -> filleuls(k, retour)));
                }
            }
            if (e.filleuls.isEmpty()) {
                menu.poser(22, Contenant.objet(Material.GRAY_DYE, t("filleuls.vide", "<gray>Aucun filleul pour le moment"),
                        List.of()), null);
            }
            menu.barre();
            menu.poser(menu.bas(4), Contenant.objet(Material.OAK_DOOR, t("parrainage.retour", "<gray>Retour"), List.of()),
                    retour::accept);
            menu.ouvrir(joueur);
            lang.saveIfNeeded();
        });
    }

    /** Petit coffre de confirmation : vert à gauche, la tête au centre, rouge à droite. */
    private void confirmer(Player joueur, Component titre, UUID id, String nom, Consumer<Player> oui, Consumer<Player> non) {
        Contenant menu = Contenant.pour(joueur, 1, titre, MARQUE + "_confirmer");
        for (int i = 0; i < 9; i++) {
            menu.poser(i, Contenant.decor(), null);
        }
        menu.poser(1, Contenant.objet(Material.LIME_CONCRETE, t("confirmer.oui", "<green>Confirmer"), List.of()), oui);
        menu.poser(4, Contenant.objet(tete(id, nom), t("confirmer.nom", "<white><nom>", "nom", nom), List.of()), null);
        menu.poser(7, Contenant.objet(Material.RED_CONCRETE, t("confirmer.non", "<red>Annuler"), List.of()), non);
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ propositions reçues (filleul)

    private void propositions(Player joueur, Consumer<Player> retour) {
        etat(joueur, e -> {
            if (e == null) {
                return;
            }
            if (e.propositions.size() == 1) {
                repondre(joueur, e.propositions.get(0), retour);
                return;
            }
            Contenant menu = Contenant.pour(joueur, t("propositions.titre", "<dark_gray>Propositions de parrainage"), MARQUE);
            int n = Math.min(e.propositions.size(), Contenant.PAR_PAGE);
            for (int i = 0; i < n; i++) {
                Personne parrain = e.propositions.get(i);
                menu.poser(i, Contenant.objet(tete(parrain.id(), parrain.nom()), t("propositions.nom", "<white><nom>", "nom", parrain.nom()),
                        l("propositions.info", "<yellow>Clic : accepter ou refuser son parrainage.")),
                        j -> repondre(j, parrain, k -> propositions(k, retour)));
            }
            if (e.propositions.isEmpty()) {
                menu.poser(22, Contenant.objet(Material.GRAY_DYE, t("propositions.vide", "<gray>Aucune proposition"), List.of()), null);
            }
            menu.barre();
            menu.poser(menu.bas(3), Contenant.objet(Material.BOOK, t("propositions.aide", "<yellow>Un seul parrain"),
                    l("propositions.aide-info", "<gray>Tu ne peux accepter qu'un parrain : les autres propositions tombent.")), null);
            menu.poser(menu.bas(4), retour == null
                    ? Contenant.objet(Material.BARRIER, t("parrainage.fermer", "<red>Fermer"), List.of())
                    : Contenant.objet(Material.OAK_DOOR, t("parrainage.retour", "<gray>Retour"), List.of()),
                    j -> {
                        if (retour == null) {
                            Contenant.fermer(j);
                        } else {
                            retour.accept(j);
                        }
                    });
            menu.ouvrir(joueur);
            lang.saveIfNeeded();
        });
    }

    private void repondre(Player joueur, Personne parrain, Consumer<Player> retour) {
        Contenant menu = Contenant.pour(joueur, 1, t("repondre.titre", "<dark_gray>Parrainage de <nom>", "nom", parrain.nom()),
                MARQUE + "_confirmer");
        for (int i = 0; i < 9; i++) {
            menu.poser(i, Contenant.decor(), null);
        }
        menu.poser(1, Contenant.objet(Material.LIME_CONCRETE, t("repondre.accepter", "<green>Accepter"),
                l("repondre.accepter-info-2", "<gray>Tu es whitelisté sur Kixster (si tu ne l'es pas déjà) ; vous recevez "
                        + "tous les deux des coffres selon ton temps de jeu sur Kixster.")), j -> relais.appeler(j, "paccepter",
                Map.of("target", parrain.id().toString()), r -> {
                    if (r == null) {
                        return;
                    }
                    if (r.ok()) {
                        plugin.dire(j, "repondre.accepte-2", "<green>Parrainage de <white><nom></white> accepté.", "nom",
                                parrain.nom());
                        Contenant.fermer(j);
                    } else {
                        message(j, switch (r.partie(0)) {
                            case "already" -> t("repondre.deja-2", "<yellow>Tu as déjà un parrain.");
                            case "cycle" -> t("repondre.cycle", "<red><nom> est ton filleul (ou celui d'un de tes filleuls) : il ne "
                                    + "peut pas être ton parrain.", "nom", parrain.nom());
                            default -> t("repondre.plus-la", "<red>Cette proposition n'existe plus.");
                        }, true);
                        Contenant.fermer(j);
                    }
                }));
        menu.poser(4, Contenant.objet(tete(parrain.id(), parrain.nom()), t("repondre.nom", "<white><nom>", "nom", parrain.nom()),
                l("repondre.nom-info", "<gray>te propose de te parrainer sur Kixster.")), null);
        menu.poser(7, Contenant.objet(Material.RED_CONCRETE, t("repondre.refuser", "<red>Refuser"), List.of()),
                j -> relais.appeler(j, "prefuser", Map.of("target", parrain.id().toString()), r -> {
                    if (r != null && r.ok()) {
                        message(j, t("repondre.refuse", "<gray>Parrainage de <white><nom></white> refusé.", "nom", parrain.nom()), false);
                    }
                    if (retour == null) {
                        Contenant.fermer(j);
                    } else {
                        retour.accept(j);
                    }
                }));
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ refusé à l'entrée de Kixster

    /** Message « whitelist » du proxy : le joueur vient d'être refusé à l'entrée de Kixster. */
    void refuseKixster(Player joueur) {
        etat(joueur, e -> {
            if (e == null || e.surKixster) {
                return;
            }
            if (!e.propositions.isEmpty()) {
                propositions(joueur, null);
            } else {
                demander(joueur, e.demande);
            }
        });
    }

    /** Fenêtre de demande aux admins (pseudo Discord), ou le rappel qu'une demande attend déjà. */
    private void demander(Player joueur, String etatDemande) {
        if (etatDemande.equals("attente")) {
            Contenant.fermer(joueur);
            plugin.dire(joueur, "demande.deja-2", "<yellow>Ta demande de whitelist pour Kixster est en attente : un admin "
                    + "la traitera dès que possible.");
            return;
        }
        Contenant.saisie(joueur, () -> {
            List<DialogInput> champs = List.of(gui.text("discord", t("demande.champ", "Pseudo Discord"), "", 40));
            List<ActionButton> boutons = List.of(gui.form(t("demande.envoyer", "<green>Envoyer la demande"), null, (j, vue) -> {
                String discord = vue.getText("discord");
                if (discord == null || discord.isBlank()) {
                    plugin.dire(j, "demande.vide", "<red>Écris ton pseudo Discord pour envoyer la demande.");
                    return;
                }
                relais.appeler(j, "wdemande", Map.of("discord", discord.trim()), r -> {
                    if (r == null) {
                        return;
                    }
                    if (r.ok()) {
                        plugin.dire(j, "demande.envoyee", "<green>Demande envoyée : un admin la traitera dès que possible "
                                + "(tu seras prévenu).");
                    } else {
                        switch (r.partie(0)) {
                            case "pending" -> plugin.dire(j, "demande.deja-2", "<yellow>Ta demande de whitelist pour "
                                    + "Kixster est en attente : un admin la traitera dès que possible.");
                            case "already" -> plugin.dire(j, "demande.inutile-2", "<green>Tu es déjà whitelisté sur Kixster.");
                            default -> plugin.dire(j, "demande.mauvais", "<red>Pseudo Discord invalide (2 à 40 caractères).");
                        }
                    }
                });
            }));
            List<Component> corps = new ArrayList<>();
            corps.add(t("demande.corps-2", "<gray>Tu n'es pas encore whitelisté sur Kixster. Demande à un ami de te "
                    + "parrainer (menu Contacts), ou envoie une demande aux admins."));
            if (etatDemande.equals("refusee")) {
                corps.add(t("demande.refusee", "<red>Ta dernière demande a été refusée."));
            }
            gui.open(joueur, t("demande.titre", "<aqua><bold>Rejoindre Kixster"), corps, champs, boutons,
                    gui.close(t("demande.annuler", "<gray>Annuler")), 1);
            lang.saveIfNeeded();
        });
    }

    // ------------------------------------------------------------------ modération : demandes de whitelist

    void demandes(Player joueur, Consumer<Player> retour) {
        relais.appeler(joueur, "wliste", Map.of(), reponse -> {
            if (reponse == null || !reponse.ok()) {
                return;
            }
            List<Demande> liste = new ArrayList<>();
            for (String ligne : reponse.lignes()) {
                String[] c = ligne.split("\t", -1);
                try {
                    if (c[0].equals("D")) {
                        liste.add(new Demande(UUID.fromString(c[1]), c[2], c[3], Long.parseLong(c[4]),
                                Boolean.parseBoolean(c[5].trim())));
                    }
                } catch (RuntimeException e) {
                    // ligne incomplète : ignorée
                }
            }
            Contenant menu = Contenant.pour(joueur, t("demandes-wl.titre", "<dark_gray>Demandes de whitelist"), MARQUE + "_admin");
            int n = Math.min(liste.size(), Contenant.PAR_PAGE);
            for (int i = 0; i < n; i++) {
                Demande d = liste.get(i);
                List<Component> lignes = List.of(
                        t("demandes-wl.discord", "<gray>Discord : <white><discord>", "discord", d.discord()),
                        t("demandes-wl.date", "<gray>Le <white><date>", "date", DATE.format(Instant.ofEpochMilli(d.date()))),
                        d.bedrock() ? t("demandes-wl.bedrock", "<gray>Joueur Bedrock") : t("demandes-wl.java", "<gray>Joueur Java"),
                        t("demandes-wl.clic", "<yellow>Clic : accepter ou refuser."));
                menu.poser(i, Contenant.objet(Material.PAPER, t("demandes-wl.nom", "<white><nom>", "nom", d.nom()), lignes),
                        j -> traiter(j, d, k -> demandes(k, retour)));
            }
            if (liste.isEmpty()) {
                menu.poser(22, Contenant.objet(Material.GRAY_DYE, t("demandes-wl.vide", "<gray>Aucune demande en attente"),
                        List.of()), null);
            }
            menu.barre();
            menu.poser(menu.bas(5), retour == null
                    ? Contenant.objet(Material.BARRIER, t("parrainage.fermer", "<red>Fermer"), List.of())
                    : Contenant.objet(Material.OAK_DOOR, t("parrainage.retour", "<gray>Retour"), List.of()),
                    j -> {
                        if (retour == null) {
                            Contenant.fermer(j);
                        } else {
                            Contenant.saisie(j, () -> retour.accept(j));
                        }
                    });
            menu.poser(menu.bas(6), Contenant.objet(Material.WRITTEN_BOOK, t("demandes-wl.historique", "<aqua>Parrainages"),
                    l("demandes-wl.historique-info", "<gray>Qui a parrainé qui (le plus récent d'abord).")),
                    j -> historique(j, k -> demandes(k, retour)));
            menu.poser(menu.bas(7), Contenant.objet(Material.BOOKSHELF, t("demandes-wl.traitees", "<aqua>Demandes traitées"),
                    l("demandes-wl.traitees-info", "<gray>Demandes acceptées ou refusées (la plus récente d'abord).")),
                    j -> traitees(j, k -> demandes(k, retour), 0));
            menu.ouvrir(joueur);
            lang.saveIfNeeded();
        });
    }

    private void traiter(Player joueur, Demande d, Consumer<Player> retour) {
        Contenant menu = Contenant.pour(joueur, 1, t("traiter.titre", "<dark_gray>Demande de <nom>", "nom", d.nom()),
                MARQUE + "_confirmer");
        for (int i = 0; i < 9; i++) {
            menu.poser(i, Contenant.decor(), null);
        }
        Consumer<Boolean> agir = accepter -> relais.appeler(joueur, accepter ? "waccepter" : "wrefuser",
                Map.of("target", d.id().toString()), r -> {
                    if (r != null) {
                        message(joueur, r.ok()
                                ? (accepter ? t("traiter.accepte", "<green>Demande de <white><nom></white> acceptée.", "nom", d.nom())
                                : t("traiter.refuse", "<gray>Demande de <white><nom></white> refusée.", "nom", d.nom()))
                                : t("traiter.deja", "<yellow>Cette demande a déjà été traitée."), !r.ok());
                    }
                    retour.accept(joueur);
                });
        menu.poser(1, Contenant.objet(Material.LIME_CONCRETE, t("traiter.accepter", "<green>Accepter"),
                l("traiter.accepter-info-2", "<gray>Il est whitelisté sur Kixster à sa prochaine arrivée.")),
                j -> agir.accept(true));
        menu.poser(4, Contenant.objet(Material.PAPER, t("demandes-wl.nom", "<white><nom>", "nom", d.nom()),
                List.of(t("demandes-wl.discord", "<gray>Discord : <white><discord>", "discord", d.discord()))), null);
        menu.poser(7, Contenant.objet(Material.RED_CONCRETE, t("traiter.refuser", "<red>Refuser"),
                l("traiter.refuser-info", "<gray>Il est prévenu ; il pourra refaire une demande.")), j -> agir.accept(false));
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private void historique(Player joueur, Consumer<Player> retour) {
        relais.appeler(joueur, "whisto", Map.of(), reponse -> {
            if (reponse == null || !reponse.ok()) {
                return;
            }
            List<Historique> liste = new ArrayList<>();
            for (String ligne : reponse.lignes()) {
                String[] c = ligne.split("\t", -1);
                try {
                    if (c[0].equals("H")) {
                        liste.add(new Historique(UUID.fromString(c[1]), c[2], UUID.fromString(c[3]), c[4],
                                Long.parseLong(c[5]), Long.parseLong(c[6].trim())));
                    }
                } catch (RuntimeException e) {
                    // ligne incomplète : ignorée
                }
            }
            Contenant menu = Contenant.pour(joueur, t("historique.titre", "<dark_gray>Parrainages"), MARQUE + "_admin");
            int n = Math.min(liste.size(), Contenant.PAR_PAGE);
            for (int i = 0; i < n; i++) {
                Historique h = liste.get(i);
                menu.poser(i, Contenant.objet(tete(h.filleul(), h.nomFilleul()), t("historique.nom", "<white><nom>", "nom", h.nomFilleul()),
                        List.of(t("historique.parrain", "<gray>Parrain : <gold><nom>", "nom", h.nomParrain()),
                                t("historique.date", "<gray>Le <white><date>", "date", DATE.format(Instant.ofEpochMilli(h.date()))),
                                t("historique.temps", "<gray>Temps de jeu actif : <white><temps>", "temps", duree(h.secondes())))), null);
            }
            menu.barre();
            if (liste.isEmpty()) {
                menu.poser(22, Contenant.objet(Material.GRAY_DYE, t("historique.vide", "<gray>Aucun parrainage"), List.of()), null);
            } else if (liste.size() > n) {
                menu.poser(menu.bas(3), Contenant.objet(Material.BOOK, t("historique.suite", "<gray><n> plus anciens non affichés",
                        "n", liste.size() - n), List.of()), null);
            }
            menu.poser(menu.bas(4), Contenant.objet(Material.OAK_DOOR, t("parrainage.retour", "<gray>Retour"), List.of()),
                    retour::accept);
            menu.ouvrir(joueur);
            lang.saveIfNeeded();
        });
    }

    // ------------------------------------------------------------------ 1.5.2 : demandes traitées

    private void traitees(Player joueur, Consumer<Player> retour, int page) {
        relais.appeler(joueur, "whdemandes", Map.of(), reponse -> {
            if (reponse == null || !reponse.ok()) {
                return;
            }
            List<Traitee> liste = new ArrayList<>();
            for (String ligne : reponse.lignes()) {
                String[] c = ligne.split("\t", -1);
                try {
                    if (c[0].equals("H")) {
                        liste.add(new Traitee(UUID.fromString(c[1]), c[2], c[3], Long.parseLong(c[4]),
                                c[5].equals("acceptee"), c[6], Long.parseLong(c[7]), Boolean.parseBoolean(c[8].trim())));
                    }
                } catch (RuntimeException e) {
                    // ligne incomplète : ignorée
                }
            }
            Contenant menu = Contenant.pour(joueur, t("traitees.titre", "<dark_gray>Demandes traitées"), MARQUE + "_admin");
            int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
            int p = Math.max(0, Math.min(pages - 1, page));
            List<Traitee> visibles = liste.subList(p * Contenant.PAR_PAGE,
                    Math.min(liste.size(), (p + 1) * Contenant.PAR_PAGE));
            for (int i = 0; i < visibles.size(); i++) {
                Traitee d = visibles.get(i);
                List<Component> lignes = new ArrayList<>();
                if (d.par().equals("parrainage")) {
                    lignes.add(t("traitees.parrainage", "<green>Acceptée <gray>(parrainage accepté)"));
                } else if (d.acceptee()) {
                    lignes.add(t("traitees.acceptee", "<green>Acceptée <gray>par <white><admin>", "admin", d.par()));
                } else {
                    lignes.add(t("traitees.refusee", "<red>Refusée <gray>par <white><admin>", "admin", d.par()));
                }
                lignes.add(d.traite() > 0
                        ? t("traitees.date", "<gray>Traitée le <white><date>", "date", DATE.format(Instant.ofEpochMilli(d.traite())))
                        : t("traitees.date-inconnue", "<gray>Traitée avant le 26/10/2026"));
                lignes.add(t("traitees.demandee", "<gray>Demandée le <white><date>", "date", DATE.format(Instant.ofEpochMilli(d.date()))));
                lignes.add(t("demandes-wl.discord", "<gray>Discord : <white><discord>", "discord", d.discord()));
                lignes.add(d.bedrock() ? t("demandes-wl.bedrock", "<gray>Joueur Bedrock") : t("demandes-wl.java", "<gray>Joueur Java"));
                menu.poser(i, Contenant.objet(d.acceptee() ? Material.LIME_DYE : Material.RED_DYE,
                        t("traitees.nom", "<white><nom>", "nom", d.nom()), lignes), null);
            }
            menu.barre();
            if (liste.isEmpty()) {
                menu.poser(22, Contenant.objet(Material.GRAY_DYE, t("traitees.vide", "<gray>Aucune demande traitée"), List.of()), null);
            } else {
                menu.poser(menu.bas(3), Contenant.objet(Material.BOOK, t("traitees.total", "<yellow><n> demandes traitées",
                        "n", liste.size()), l("traitees.page", "<gray>Page <white><page></white> sur <white><pages></white>.",
                        "page", p + 1, "pages", pages)), null);
            }
            if (p > 0) {
                menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, t("parrainage.precedente", "<gray>Page précédente"),
                        List.of()), j -> traitees(j, retour, p - 1));
            }
            if (p < pages - 1) {
                menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, t("parrainage.suivante", "<gray>Page suivante"),
                        List.of()), j -> traitees(j, retour, p + 1));
            }
            menu.poser(menu.bas(4), Contenant.objet(Material.OAK_DOOR, t("parrainage.retour", "<gray>Retour"), List.of()),
                    retour::accept);
            menu.ouvrir(joueur);
            lang.saveIfNeeded();
        });
    }
}
