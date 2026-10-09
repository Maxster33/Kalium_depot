package fr.kalium.contacts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import fr.kalium.contacts.Relais.Ami;
import fr.kalium.contacts.Relais.Liste;
import fr.kalium.contacts.Relais.Personne;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;

/**
 * Menus de KLM_Contacts (Dialogs de la boîte à outils de KLM_Menu) : liste des amis avec leur serveur, fiche d'un ami,
 * ajout, demandes reçues et envoyées, joueurs bloqués, réglages. Chaque ouverture relit la liste sur le relais.
 */
final class Menus {

    /** Amis affichés par page (2 colonnes). */
    private static final int PAR_PAGE = 12;

    private final KlmContacts plugin;
    private final Relais relais;
    private final Lang lang;
    private final Gui gui;

    Menus(KlmContacts plugin, Relais relais, Lang lang, Gui gui) {
        this.plugin = plugin;
        this.relais = relais;
        this.lang = lang;
        this.gui = gui;
    }

    private Component t(String cle, String def, Object... paires) {
        return lang.c(cle, def, paires);
    }

    /** Nom affiché d'un serveur (nom du velocity.toml -> texte de lang.yml). */
    String serveur(String nom) {
        String cle = nom.toLowerCase(Locale.ROOT);
        String def = switch (cle) {
            case "lobby" -> "Lobby";
            case "kal-games" -> "Kal-Games";
            case "serveur-jeux" -> "Serveur Jeux";
            case "kixster" -> "Kixster";
            case "event" -> "Event";
            case "kanvas" -> "Kanvas";
            default -> nom;
        };
        return lang.raw("serveur." + cle, def);
    }

    private ActionButton retour(Consumer<Player> vers) {
        return gui.button(t("menu.retour", "<gray>Retour"), null, vers::accept);
    }

    // ------------------------------------------------------------------ liste des amis

    void ouvrir(Player joueur, Consumer<Player> retour) {
        ouvrir(joueur, retour, 0);
    }

    private void ouvrir(Player joueur, Consumer<Player> retour, int page) {
        relais.lister(joueur, liste -> {
            if (liste == null) {
                return;
            }
            int pages = Math.max(1, (liste.amis.size() + PAR_PAGE - 1) / PAR_PAGE);
            int p = Math.max(0, Math.min(pages - 1, page));
            List<ActionButton> boutons = new ArrayList<>();
            for (Ami ami : liste.amis.subList(p * PAR_PAGE, Math.min(liste.amis.size(), (p + 1) * PAR_PAGE))) {
                Component libelle = ami.enLigne()
                        ? t("liste.ami-en-ligne", "<green><nom> <gray>· <white><serveur>", "nom", ami.nom(), "serveur", serveur(ami.serveur()))
                        : t("liste.ami-hors-ligne", "<gray><nom> · hors ligne", "nom", ami.nom());
                boutons.add(gui.button(libelle, t("liste.ami-info", "<gray>Rejoindre, écrire, retirer, bloquer."),
                        j -> fiche(j, ami.id(), retour, p)));
            }
            if (p > 0) {
                boutons.add(gui.button(t("liste.precedente", "<gray>Page précédente"), null, j -> ouvrir(j, retour, p - 1)));
            }
            if (p < pages - 1) {
                boutons.add(gui.button(t("liste.suivante", "<gray>Page suivante"), null, j -> ouvrir(j, retour, p + 1)));
            }
            boutons.add(gui.button(t("liste.ajouter", "<green>Ajouter un ami"),
                    t("liste.ajouter-info", "<gray>Envoie une demande d'ami à un joueur, par son pseudo."), j -> ajouter(j, retour)));
            boutons.add(gui.button(t("liste.demandes", "<yellow>Demandes (<n>)", "n", liste.recues.size()),
                    t("liste.demandes-info", "<gray>Demandes d'ami reçues et envoyées."), j -> demandes(j, retour)));
            boutons.add(gui.button(t("liste.groupe", "<aqua>Groupe de jeu"),
                    t("liste.groupe-info", "<gray>Inviter des joueurs, se déplacer ensemble, tchat de groupe."),
                    j -> groupe(j, k -> ouvrir(k, retour, 0))));
            boutons.add(gui.button(t("liste.bloques", "<red>Joueurs bloqués"),
                    t("liste.bloques-info", "<gray>Un joueur bloqué ne peut plus t'envoyer de demande ni de message."),
                    j -> bloques(j, retour)));
            boutons.add(gui.button(t("liste.reglages", "<aqua>Réglages"),
                    t("liste.reglages-info", "<gray>Mode invisible, notifications, messages privés."), j -> reglages(j, retour)));
            int enLigne = 0;
            for (Ami ami : liste.amis) {
                if (ami.enLigne()) {
                    enLigne++;
                }
            }
            List<Component> corps = new ArrayList<>();
            corps.add(liste.amis.isEmpty()
                    ? t("liste.vide", "<gray>Tu n'as pas encore d'ami. Ajoute un joueur avec son pseudo.")
                    : t("liste.corps", "<gray>Amis en ligne : <white><n></white> sur <total>.", "n", enLigne, "total", liste.amis.size()));
            if (pages > 1) {
                corps.add(t("liste.page", "<gray>Page <p> sur <pages>.", "p", p + 1, "pages", pages));
            }
            if (liste.invisible) {
                corps.add(t("liste.invisible", "<yellow>Mode invisible : tes amis te voient hors ligne."));
            }
            corps.add(t("liste.aide", "<dark_gray>Message privé : /mp pseudo message. Réponse : /r message."));
            gui.open(joueur, t("liste.titre", "<light_purple><bold>Contacts"), corps, List.of(), boutons,
                    retour == null ? null : retour(retour), 2);
            lang.saveIfNeeded();
        });
    }

    // ------------------------------------------------------------------ fiche d'un ami

    private void fiche(Player joueur, java.util.UUID id, Consumer<Player> retour, int page) {
        relais.lister(joueur, liste -> {
            if (liste == null) {
                return;
            }
            Ami ami = liste.ami(id);
            if (ami == null) {
                ouvrir(joueur, retour, page);
                return;
            }
            Consumer<Player> versListe = j -> ouvrir(j, retour, page);
            List<ActionButton> boutons = new ArrayList<>();
            if (ami.enLigne() && !ami.serveur().equalsIgnoreCase(liste.monServeur)) {
                boutons.add(gui.button(t("fiche.rejoindre", "<green>Rejoindre son serveur"),
                        t("fiche.rejoindre-info", "<gray>Tu es envoyé sur <white><serveur></white>.", "serveur", serveur(ami.serveur())),
                        j -> plugin.rejoindre(j, ami.nom())));
            }
            boutons.add(gui.button(t("fiche.message", "<aqua>Envoyer un message"),
                    t("fiche.message-info", "<gray>Message privé, d'un serveur à l'autre."), j -> message(j, ami, retour, page)));
            if (ami.enLigne()) {
                boutons.add(gui.button(t("fiche.inviter", "<aqua>Inviter dans mon groupe"),
                        t("fiche.inviter-info", "<gray>S'il accepte, vous jouez en groupe (tu es le chef si tu n'as pas encore de groupe)."),
                        j -> relais.appeler(j, "ginvite", Map.of("target", ami.nom()), r -> { })));
            }
            if (ami.enLigne() && plugin.partieDe(joueur) != null) {
                // 1.2.0 : je suis dans une partie privée où l'on peut inviter.
                boutons.add(gui.button(t("fiche.inviter-partie", "<gold>Inviter dans ma partie"),
                        t("fiche.inviter-partie-info", "<gray>S'il accepte (60 s), il est amené sur ce serveur et entre dans ta partie."),
                        j -> plugin.inviterEnPartie(j, ami.nom())));
            }
            boutons.add(gui.button(t("fiche.retirer", "<yellow>Retirer des amis"), null,
                    j -> gui.confirm(j, t("fiche.retirer-titre", "<yellow>Retirer des amis"),
                            t("fiche.retirer-corps", "<gray>Retirer <white><nom></white> de tes amis ?", "nom", ami.nom()),
                            k -> plugin.agir(k, "remove", ami.nom(), versListe), k -> fiche(k, id, retour, page))));
            boutons.add(gui.button(t("fiche.bloquer", "<red>Bloquer"), null,
                    j -> gui.confirm(j, t("fiche.bloquer-titre", "<red>Bloquer"),
                            t("fiche.bloquer-corps", "<gray>Bloquer <white><nom></white> ? Il est retiré de tes amis et ne peut plus "
                                    + "t'envoyer de demande ni de message. Il n'est pas prévenu.", "nom", ami.nom()),
                            k -> plugin.agir(k, "block", ami.nom(), versListe), k -> fiche(k, id, retour, page))));
            Component etat = ami.enLigne()
                    ? t("fiche.en-ligne", "<green>En ligne <gray>sur <white><serveur></white>.", "serveur", serveur(ami.serveur()))
                    : t("fiche.hors-ligne", "<gray>Hors ligne.");
            gui.open(joueur, t("fiche.titre", "<light_purple><bold><nom>", "nom", ami.nom()), List.of(etat), List.of(), boutons,
                    retour(versListe), 1);
            lang.saveIfNeeded();
        });
    }

    private void message(Player joueur, Ami ami, Consumer<Player> retour, int page) {
        List<DialogInput> champs = List.of(gui.text("texte", t("message.champ", "Message"), "", 200));
        List<ActionButton> boutons = List.of(gui.form(t("message.envoyer", "<green>Envoyer"), null, (j, vue) -> {
            String texte = vue.getText("texte");
            if (texte == null || texte.isBlank()) {
                return;
            }
            relais.appeler(j, "msg", Map.of("target", ami.nom(), "text", texte), reponse -> {
                if (reponse != null && !reponse.ok()) {
                    plugin.dire(j, "message.refuse", "<red>Message non envoyé.");
                }
            });
        }));
        gui.open(joueur, t("message.titre", "<aqua>Message à <nom>", "nom", ami.nom()),
                List.of(t("message.corps", "<gray>Il le reçoit tout de suite s'il est en ligne.")), champs, boutons,
                retour(j -> fiche(j, ami.id(), retour, page)), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ ajout

    private void ajouter(Player joueur, Consumer<Player> retour) {
        List<DialogInput> champs = List.of(gui.text("pseudo", t("ajouter.champ", "Pseudo du joueur"), "", 32));
        List<ActionButton> boutons = List.of(gui.form(t("ajouter.valider", "<green>Envoyer la demande"), null, (j, vue) -> {
            String pseudo = vue.getText("pseudo");
            if (pseudo == null || pseudo.isBlank()) {
                ouvrir(j, retour, 0);
                return;
            }
            plugin.agir(j, "add", pseudo.trim(), k -> ouvrir(k, retour, 0));
        }));
        gui.open(joueur, t("ajouter.titre", "<green>Ajouter un ami"),
                List.of(t("ajouter.corps", "<gray>Écris son pseudo exact. Il doit être déjà venu sur KaLium ; il verra ta "
                        + "demande tout de suite, ou à sa prochaine connexion.")), champs, boutons,
                retour(j -> ouvrir(j, retour, 0)), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ demandes

    private void demandes(Player joueur, Consumer<Player> retour) {
        relais.lister(joueur, liste -> {
            if (liste == null) {
                return;
            }
            Consumer<Player> ici = j -> demandes(j, retour);
            List<ActionButton> boutons = new ArrayList<>();
            int limite = Math.min(liste.recues.size(), 10);
            for (Personne p : liste.recues.subList(0, limite)) {
                boutons.add(gui.button(t("demandes.accepter", "<green>Accepter <nom>", "nom", p.nom()), null,
                        j -> plugin.agir(j, "accept", p.id().toString(), ici)));
                boutons.add(gui.button(t("demandes.refuser", "<red>Refuser <nom>", "nom", p.nom()), null,
                        j -> plugin.agir(j, "deny", p.id().toString(), ici)));
            }
            if (!liste.envoyees.isEmpty()) {
                boutons.add(gui.button(t("demandes.envoyees", "<yellow>Demandes envoyées (<n>)", "n", liste.envoyees.size()),
                        t("demandes.envoyees-info", "<gray>Demandes en attente de réponse : tu peux les annuler."),
                        j -> envoyees(j, retour)));
            }
            List<Component> corps = new ArrayList<>();
            corps.add(liste.recues.isEmpty()
                    ? t("demandes.vide", "<gray>Aucune demande d'ami reçue.")
                    : t("demandes.corps", "<gray>Demandes d'ami reçues : <white><n></white>.", "n", liste.recues.size()));
            if (liste.recues.size() > limite) {
                corps.add(t("demandes.suite", "<gray>Les suivantes s'affichent quand tu as répondu à celles-ci."));
            }
            gui.open(joueur, t("demandes.titre", "<yellow><bold>Demandes d'ami"), corps, List.of(), boutons,
                    retour(j -> ouvrir(j, retour, 0)), 2);
            lang.saveIfNeeded();
        });
    }

    private void envoyees(Player joueur, Consumer<Player> retour) {
        relais.lister(joueur, liste -> {
            if (liste == null) {
                return;
            }
            List<ActionButton> boutons = new ArrayList<>();
            for (Personne p : liste.envoyees) {
                boutons.add(gui.button(t("envoyees.annuler", "<red>Annuler : <nom>", "nom", p.nom()), null,
                        j -> plugin.agir(j, "cancel", p.id().toString(), k -> envoyees(k, retour))));
            }
            gui.open(joueur, t("envoyees.titre", "<yellow><bold>Demandes envoyées"),
                    List.of(liste.envoyees.isEmpty()
                            ? t("envoyees.vide", "<gray>Aucune demande en attente.")
                            : t("envoyees.corps", "<gray>Clique sur une demande pour l'annuler.")), List.of(), boutons,
                    retour(j -> demandes(j, retour)), 2);
            lang.saveIfNeeded();
        });
    }

    // ------------------------------------------------------------------ joueurs bloqués

    private void bloques(Player joueur, Consumer<Player> retour) {
        relais.lister(joueur, liste -> {
            if (liste == null) {
                return;
            }
            Consumer<Player> ici = j -> bloques(j, retour);
            List<ActionButton> boutons = new ArrayList<>();
            for (Personne p : liste.bloques) {
                boutons.add(gui.button(t("bloques.debloquer", "<green>Débloquer <nom>", "nom", p.nom()), null,
                        j -> plugin.agir(j, "unblock", p.id().toString(), ici)));
            }
            boutons.add(gui.button(t("bloques.ajouter", "<red>Bloquer un joueur"), null, j -> bloquer(j, retour)));
            gui.open(joueur, t("bloques.titre", "<red><bold>Joueurs bloqués"),
                    List.of(liste.bloques.isEmpty()
                            ? t("bloques.vide", "<gray>Tu n'as bloqué personne.")
                            : t("bloques.corps", "<gray>Clique sur un joueur pour le débloquer.")), List.of(), boutons,
                    retour(j -> ouvrir(j, retour, 0)), 2);
            lang.saveIfNeeded();
        });
    }

    private void bloquer(Player joueur, Consumer<Player> retour) {
        List<DialogInput> champs = List.of(gui.text("pseudo", t("bloquer.champ", "Pseudo du joueur"), "", 32));
        List<ActionButton> boutons = List.of(gui.form(t("bloquer.valider", "<red>Bloquer"), null, (j, vue) -> {
            String pseudo = vue.getText("pseudo");
            if (pseudo == null || pseudo.isBlank()) {
                bloques(j, retour);
                return;
            }
            plugin.agir(j, "block", pseudo.trim(), k -> bloques(k, retour));
        }));
        gui.open(joueur, t("bloquer.titre", "<red>Bloquer un joueur"),
                List.of(t("bloquer.corps", "<gray>Il ne pourra plus t'envoyer de demande d'ami ni de message privé. "
                        + "Il n'est pas prévenu.")), champs, boutons, retour(j -> bloques(j, retour)), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ réglages

    private void reglages(Player joueur, Consumer<Player> retour) {
        relais.lister(joueur, liste -> {
            if (liste == null) {
                return;
            }
            List<DialogInput> champs = List.of(
                    gui.toggle("invisible", t("reglages.invisible", "Mode invisible"), liste.invisible),
                    gui.toggle("notifications", t("reglages.notifications", "Connexions de mes amis"), liste.notifications),
                    gui.choice("mp", t("reglages.mp", "Messages privés"), List.of("all", "friends", "none"),
                            List.of(t("reglages.mp-tous", "Tout le monde"), t("reglages.mp-amis", "Amis seulement"),
                                    t("reglages.mp-personne", "Personne")), liste.mp),
                    gui.choice("suivre", t("reglages.suivre", "Suivre le chef"), List.of("auto", "ask"),
                            List.of(t("reglages.suivre-auto", "D'office"), t("reglages.suivre-demander", "Me demander avant")),
                            liste.suivre),
                    gui.choice("invitations", t("reglages.invitations", "Invitations de groupe"), List.of("all", "friends"),
                            List.of(t("reglages.invitations-tous", "Tout le monde"), t("reglages.invitations-amis", "Amis seulement")),
                            liste.invitations));
            List<ActionButton> boutons = List.of(gui.form(t("reglages.enregistrer", "<green>Enregistrer"), null, (j, vue) -> {
                boolean invisible = Boolean.TRUE.equals(vue.getBoolean("invisible"));
                boolean notifications = !Boolean.FALSE.equals(vue.getBoolean("notifications"));
                String mp = vue.getText("mp") == null ? liste.mp : vue.getText("mp");
                String suivre = vue.getText("suivre") == null ? liste.suivre : vue.getText("suivre");
                String invitations = vue.getText("invitations") == null ? liste.invitations : vue.getText("invitations");
                // Réglages enregistrés l'un après l'autre, puis retour à la liste.
                enregistrer(j, List.of(new String[]{"invisible", String.valueOf(invisible)},
                        new String[]{"notify", String.valueOf(notifications)}, new String[]{"mp", mp},
                        new String[]{"follow", suivre}, new String[]{"invites", invitations}), 0, k -> {
                    plugin.dire(k, "reglages.fait", "<green>Réglages enregistrés.");
                    ouvrir(k, retour, 0);
                });
            }));
            gui.open(joueur, t("reglages.titre", "<aqua><bold>Réglages"),
                    List.of(t("reglages.corps-invisible", "<gray>Mode invisible : tes amis te voient hors ligne et ne savent pas sur "
                                    + "quel serveur tu es. Tu vois toujours les tiens."),
                            t("reglages.corps-notifications", "<gray>Connexions de mes amis : un message quand un ami se connecte "
                                    + "ou se déconnecte."),
                            t("reglages.corps-suivre", "<gray>Suivre le chef : quand le chef de ton groupe change de serveur, tu es "
                                    + "déplacé d'office (sauf en pleine partie), ou seulement si tu l'acceptes.")),
                    champs, boutons, retour(j -> ouvrir(j, retour, 0)), 1);
            lang.saveIfNeeded();
        });
    }

    /** Enregistre les réglages (clé, valeur) un par un ; s'arrête si le relais ne répond plus. */
    private void enregistrer(Player joueur, List<String[]> reglages, int index, Consumer<Player> fin) {
        if (index >= reglages.size()) {
            fin.accept(joueur);
            return;
        }
        relais.appeler(joueur, "set", Map.of("key", reglages.get(index)[0], "value", reglages.get(index)[1]), reponse -> {
            if (reponse != null) {
                enregistrer(joueur, reglages, index + 1, fin);
            }
        });
    }

    // ------------------------------------------------------------------ groupe de jeu (1.1.0)

    /** Action de groupe : le proxy dit lui-même le résultat dans le tchat ; le menu est rouvert ensuite. */
    private void agirGroupe(Player joueur, String action, String cible, Consumer<Player> ensuite) {
        relais.appeler(joueur, action, cible == null ? Map.of() : Map.of("target", cible), reponse -> {
            if (reponse != null && ensuite != null) {
                ensuite.accept(joueur);
            }
        });
    }

    void groupe(Player joueur, Consumer<Player> retour) {
        relais.groupe(joueur, groupe -> {
            if (groupe == null) {
                return;
            }
            Consumer<Player> ici = j -> groupe(j, retour);
            List<ActionButton> boutons = new ArrayList<>();
            List<Component> corps = new ArrayList<>();
            if (groupe.partiePar != null) {
                // 1.2.0 : partie du chef du groupe, ou invitation d'un joueur dans sa partie.
                corps.add(groupe.partieJeu.isEmpty()
                        ? t("groupe.partie", "<white><nom></white> <gray>te propose de rejoindre sa partie.", "nom", groupe.partiePar)
                        : t("groupe.partie-jeu", "<white><nom></white> <gray>te propose de rejoindre sa partie (<jeu>).",
                        "nom", groupe.partiePar, "jeu", groupe.partieJeu));
                boutons.add(gui.button(t("groupe.partie-accepter", "<green>Rejoindre la partie"), null,
                        j -> agirGroupe(j, "ggameaccept", null, null)));
                boutons.add(gui.button(t("groupe.partie-refuser", "<red>Refuser la partie"), null,
                        j -> agirGroupe(j, "ggamedeny", null, ici)));
            }
            if (!groupe.existe()) {
                corps.add(t("groupe.aucun", "<gray>Tu n'es dans aucun groupe. Invite un joueur : dès qu'il accepte, le groupe "
                        + "est créé et tu en es le chef."));
                if (groupe.invitePar != null) {
                    corps.add(t("groupe.invite", "<white><nom></white> <gray>t'invite dans son groupe.", "nom", groupe.invitePar));
                    boutons.add(gui.button(t("groupe.accepter", "<green>Rejoindre le groupe"), null, j -> agirGroupe(j, "gaccept", null, ici)));
                    boutons.add(gui.button(t("groupe.refuser", "<red>Refuser"), null, j -> agirGroupe(j, "gdeny", null, ici)));
                }
                boutons.add(gui.button(t("groupe.inviter", "<aqua>Inviter un joueur"), null, j -> inviterGroupe(j, retour)));
            } else {
                Relais.Membre chef = groupe.chef();
                corps.add(t("groupe.corps", "<gray>Groupe de <white><chef></white> : <n> sur <max> joueurs.",
                        "chef", chef == null ? "?" : chef.nom(), "n", groupe.membres.size(), "max", groupe.maximum));
                for (Relais.Membre membre : groupe.membres) {
                    corps.add(membre.serveur().isEmpty()
                            ? t("groupe.membre-hors-ligne", "<gray>- <nom> · hors ligne", "nom", membre.nom())
                            : t("groupe.membre", "<gray>- <white><nom></white> · <serveur>", "nom", membre.nom(),
                            "serveur", serveur(membre.serveur())));
                }
                corps.add(t("groupe.aide", "<dark_gray>Tchat de groupe : /gc message."));
                if (groupe.jeSuisChef) {
                    if (groupe.membres.size() < groupe.maximum) {
                        boutons.add(gui.button(t("groupe.inviter", "<aqua>Inviter un joueur"), null, j -> inviterGroupe(j, retour)));
                    }
                    boutons.add(gui.button(t("groupe.gerer", "<yellow>Gérer les membres"),
                            t("groupe.gerer-info", "<gray>Exclure un membre, ou nommer un autre chef."), j -> membres(j, retour)));
                } else {
                    boutons.add(gui.button(t("groupe.suivre", "<green>Rejoindre le chef"),
                            t("groupe.suivre-info", "<gray>Tu es envoyé sur le serveur du chef du groupe."),
                            j -> agirGroupe(j, "ggo", null, null)));
                }
                boutons.add(gui.button(t("groupe.quitter", "<red>Quitter le groupe"), null,
                        j -> gui.confirm(j, t("groupe.quitter-titre", "<red>Quitter le groupe"),
                                t("groupe.quitter-corps", "<gray>Quitter le groupe ?"),
                                k -> agirGroupe(k, "gleave", null, ici), ici::accept)));
                if (groupe.jeSuisChef) {
                    boutons.add(gui.button(t("groupe.dissoudre", "<red>Dissoudre le groupe"), null,
                            j -> gui.confirm(j, t("groupe.dissoudre-titre", "<red>Dissoudre le groupe"),
                                    t("groupe.dissoudre-corps", "<gray>Dissoudre le groupe ? Tous les membres en sortent."),
                                    k -> agirGroupe(k, "gdisband", null, ici), ici::accept)));
                }
            }
            gui.open(joueur, t("groupe.titre", "<aqua><bold>Groupe de jeu"), corps, List.of(), boutons,
                    retour == null ? null : retour(retour), 1);
            lang.saveIfNeeded();
        });
    }

    private void inviterGroupe(Player joueur, Consumer<Player> retour) {
        List<DialogInput> champs = List.of(gui.text("pseudo", t("inviter.champ", "Pseudo du joueur"), "", 32));
        List<ActionButton> boutons = List.of(gui.form(t("inviter.valider", "<green>Inviter"), null, (j, vue) -> {
            String pseudo = vue.getText("pseudo");
            if (pseudo == null || pseudo.isBlank()) {
                groupe(j, retour);
                return;
            }
            agirGroupe(j, "ginvite", pseudo.trim(), k -> groupe(k, retour));
        }));
        gui.open(joueur, t("inviter.titre", "<aqua>Inviter dans le groupe"),
                List.of(t("inviter.corps", "<gray>Le joueur doit être en ligne sur KaLium. Il a 60 secondes pour accepter.")),
                champs, boutons, retour(j -> groupe(j, retour)), 1);
        lang.saveIfNeeded();
    }

    /** Chef du groupe : exclure un membre ou lui passer le rôle de chef. */
    private void membres(Player joueur, Consumer<Player> retour) {
        relais.groupe(joueur, groupe -> {
            if (groupe == null) {
                return;
            }
            if (!groupe.existe() || !groupe.jeSuisChef) {
                groupe(joueur, retour);
                return;
            }
            Consumer<Player> ici = j -> membres(j, retour);
            List<ActionButton> boutons = new ArrayList<>();
            for (Relais.Membre membre : groupe.membres) {
                if (membre.chef()) {
                    continue;
                }
                boutons.add(gui.button(t("membres.exclure", "<red>Exclure <nom>", "nom", membre.nom()), null,
                        j -> agirGroupe(j, "gkick", membre.nom(), ici)));
                boutons.add(gui.button(t("membres.chef", "<yellow>Chef : <nom>", "nom", membre.nom()),
                        t("membres.chef-info", "<gray>Il devient le chef du groupe à ta place."),
                        j -> agirGroupe(j, "gleader", membre.nom(), k -> groupe(k, retour))));
            }
            gui.open(joueur, t("membres.titre", "<yellow><bold>Membres du groupe"),
                    List.of(t("membres.corps", "<gray>Exclure un membre, ou nommer un autre chef.")), List.of(), boutons,
                    retour(j -> groupe(j, retour)), 2);
            lang.saveIfNeeded();
        });
    }
}
