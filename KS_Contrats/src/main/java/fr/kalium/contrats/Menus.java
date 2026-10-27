package fr.kalium.contrats;

import fr.kalium.contrats.Contrat.Bilan;
import fr.kalium.contrats.Contrat.Demande;
import fr.kalium.contrats.Contrat.Engagement;
import fr.kalium.contrats.Contrat.Etat;
import fr.kalium.contrats.Contrat.Ligne;
import fr.kalium.contrats.Contrat.Periode;
import fr.kalium.contrats.Contrat.Termes;
import fr.kalium.contrats.Contrat.TypeDemande;
import fr.kalium.economy.KSEconomy;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Menus en coffre du /contrat (maquette validée par LeKiwi06 le 27/10/2026) : « Mes contrats », création (une case par
 * étape), choix des joueurs, engagements (bien ou service), durée, propositions, fiche d'un contrat, période à valider,
 * admin (litiges, répartition des garanties). Saisies (texte, nombres) : petites fenêtres ouvertes depuis un coffre.
 */
final class Menus implements Listener {

    private final KSContrats plugin;
    private final Lang lang;
    private final Gui gui;
    private final Registre registre;
    /** Brouillons des nouveaux contrats, et des modifications (par compte). */
    private final Map<UUID, Brouillon> nouveaux = new HashMap<>();
    private final Map<UUID, Brouillon> modifs = new HashMap<>();
    /** Répartition en cours d'un admin (joueur -> participant -> points). */
    private final Map<UUID, Map<UUID, Long>> repartitions = new HashMap<>();
    /** Écran ouvert qui réagit à un clic sur un objet de l'inventaire du joueur (logo, objet d'un bien). */
    private final Map<UUID, Map.Entry<Object, Consumer<ItemStack>>> clicsInventaire = new HashMap<>();

    Menus(KSContrats plugin, Registre registre) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.registre = registre;
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ outils

    private Contenant menu(Player p, int rangees, Component titre, Object marque) {
        Contenant c = Contenant.pour(p, rangees, titre, marque);
        c.barre();
        clicsInventaire.remove(p.getUniqueId());
        return c;
    }

    private void sortie(Contenant c, Consumer<Player> retour) {
        if (retour == null) {
            c.poser(c.bas(5), Contenant.objet(Material.BARRIER, t("contenant.fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
        } else {
            c.poser(c.bas(5), Contenant.objet(Material.OAK_DOOR, t("contenant.retour", "<yellow>Retour"), List.of()),
                    retour);
        }
    }

    private void pages(Contenant c, int page, int pages, BiConsumer<Player, Integer> aller) {
        if (page > 0) {
            c.poser(c.bas(0), Contenant.objet(Material.ARROW, t("contenant.precedent", "<yellow>Page précédente"),
                    List.of(t("contenant.page", "<gray>Page <n> sur <total>", "n", page, "total", pages))),
                    j -> aller.accept(j, page - 1));
        }
        if (page < pages - 1) {
            c.poser(c.bas(8), Contenant.objet(Material.ARROW, t("contenant.suivant", "<yellow>Page suivante"),
                    List.of(t("contenant.page", "<gray>Page <n> sur <total>", "n", page + 2, "total", pages))),
                    j -> aller.accept(j, page + 1));
        }
    }

    private void aide(Contenant c, Component... textes) {
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, t("contenant.aide", "<yellow>Aide"), lignes(textes)), null);
    }

    static List<Component> lignes(Component... textes) {
        List<Component> lignes = new ArrayList<>();
        for (Component texte : textes) {
            if (texte != null) {
                lignes.addAll(Contenant.lignes(texte));
            }
        }
        return lignes;
    }

    private static List<Component> gris(String texte) {
        return Contenant.lignes(texte, NamedTextColor.GRAY);
    }

    private static ItemStack brillant(ItemStack objet) {
        ItemMeta meta = objet.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(true);
            objet.setItemMeta(meta);
        }
        return objet;
    }

    private void message(Player p, Component texte, boolean refus) {
        if (p.getOpenInventory().getTopInventory().getHolder() instanceof Contenant) {
            Contenant.message(p, texte, refus);
        } else {
            p.sendMessage(texte);
        }
    }

    /** Résultat d'une action du registre : refus affiché, sinon la suite. */
    private void apres(Player p, Component refus, Component fait, Runnable suite) {
        registre.sauverSiBesoin();
        lang.saveIfNeeded();
        if (refus != null) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Contenant) {
                Contenant.message(p, refus, true);
            }
            p.sendMessage(refus);
            return;
        }
        if (fait != null) {
            message(p, fait, false);
        }
        suite.run();
    }

    static ItemStack tete(UUID joueur) {
        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        try {
            if (tete.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(joueur));
                tete.setItemMeta(meta);
            }
        } catch (RuntimeException inconnu) {
            // joueur sans profil connu du serveur : une tête ordinaire
        }
        return tete;
    }

    /** Tête d'un participant (compte de test : tête du vrai joueur, retrouvé par son pseudo). */
    private static ItemStack teteDe(UUID compte, String nom) {
        if (!CompteTest.estTest(compte)) {
            return tete(compte);
        }
        OfflinePlayer o = nom == null ? null : Bukkit.getOfflinePlayerIfCached(nom);
        return o == null ? new ItemStack(Material.PLAYER_HEAD) : tete(o.getUniqueId());
    }

    // ------------------------------------------------------------------ saisies

    private DialogInput champ(String cle, Component libelle, String initial, int max, boolean plusieursLignes) {
        TextDialogInput.Builder b = DialogInput.text("f_" + cle, libelle).initial(initial == null ? "" : initial)
                .maxLength(max).width(300);
        if (plusieursLignes) {
            b.multiline(TextDialogInput.MultilineOptions.create(8, 100));
        }
        return b.build();
    }

    /** Fenêtre de saisie d'un texte, ouverte depuis un coffre ; « Retour » rouvre l'écran d'avant. */
    private void saisirTexte(Player p, Component titre, Component corps, String initial, int max,
                             boolean plusieursLignes, Consumer<String> ok, Consumer<Player> retour) {
        Contenant.saisie(p, () -> {
            List<ActionButton> boutons = List.of(gui.form(t("saisie.valider", "<green>Valider"), null,
                    (j, vue) -> {
                        String texte = vue.getText("texte");
                        ok.accept(texte == null ? "" : texte.trim());
                    }));
            gui.open(p, titre, corps == null ? List.of() : List.of(corps),
                    List.of(champ("texte", t("saisie.champ", "Texte"), initial, max, plusieursLignes)), boutons,
                    gui.button(t("saisie.retour", "<gray>Retour"), null, retour::accept), 1);
            lang.saveIfNeeded();
        });
    }

    /** Saisie d'un nombre entier positif ou nul. */
    private void saisirNombre(Player p, Component titre, Component corps, long initial, long max, Consumer<Long> ok,
                              Consumer<Player> retour) {
        saisirTexte(p, titre, corps, String.valueOf(initial), 12, false, texte -> {
            long n;
            try {
                n = Long.parseLong(texte.replaceAll("\\s", ""));
            } catch (NumberFormatException e) {
                n = -1;
            }
            if (n < 0 || n > max) {
                p.sendMessage(t("saisie.nombre-invalide", "<red>Nombre invalide (de 0 à <max>).", "max",
                        Textes.nombre(max)));
                retour.accept(p);
                return;
            }
            ok.accept(n);
        }, retour);
    }

    /** Petite confirmation en coffre : vert = confirmer, rouge = revenir. */
    private void confirmer(Player p, Component titre, List<Component> description, Consumer<Player> oui,
                           Consumer<Player> non) {
        Contenant c = menu(p, 3, titre, "contrats:confirmer");
        c.poser(11, Contenant.objet(Material.LIME_CONCRETE, t("confirmer.oui", "<green><bold>Confirmer"), description), oui);
        c.poser(15, Contenant.objet(Material.RED_CONCRETE, t("confirmer.non", "<red>Annuler"), List.of()), non);
        c.ouvrir(p);
    }

    // ------------------------------------------------------------------ textes d'un contrat

    private Component ligneBien(Ligne l, Integer livre, Periode periode) {
        Component c = Component.text("• " + Textes.nombre(l.quantite) + " ", NamedTextColor.WHITE)
                .append(Textes.nomObjet(l.modele).color(NamedTextColor.WHITE));
        if (l.exact) {
            c = c.append(Component.text(" (cet objet précis)", NamedTextColor.GRAY));
        }
        if (livre != null) {
            c = c.append(Component.text(" — " + Textes.nombre(livre) + "/" + Textes.nombre(l.quantite)
                    + (periode == null ? "" : " " + Textes.periodeEnCours(periode)),
                    livre >= l.quantite ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        }
        return c;
    }

    /** Lignes d'un engagement ; livres non null : avec ce qui est livré (période en cours). */
    private List<Component> engagement(Engagement e, List<Integer> livres, long pointsLivres, Periode periode,
                                       boolean livraisons) {
        List<Component> l = new ArrayList<>();
        if (e.service) {
            l.add(t("texte.service", "<light_purple>Service :"));
            l.add(Component.text(e.texte, NamedTextColor.WHITE));
            return l;
        }
        for (int i = 0; i < e.lignes.size(); i++) {
            l.add(ligneBien(e.lignes.get(i), livraisons && livres != null && i < livres.size() ? livres.get(i) : null,
                    periode));
        }
        if (e.points > 0) {
            Component c = Component.text("• " + Textes.points(e.points), NamedTextColor.WHITE);
            if (livraisons) {
                c = c.append(Component.text(" — " + Textes.nombre(pointsLivres) + "/" + Textes.nombre(e.points)
                        + (periode == null ? "" : " " + Textes.periodeEnCours(periode)),
                        pointsLivres >= e.points ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            }
            l.add(c);
        }
        if (!e.texte.isBlank()) {
            l.add(Component.text("Précision : " + e.texte, NamedTextColor.GRAY));
        }
        if (l.isEmpty()) {
            l.add(t("texte.vide", "<gray>(rien pour l'instant)"));
        }
        return l;
    }

    private static String sEngage(List<UUID> partie) {
        return partie.size() > 1 ? " s'engagent à :" : " s'engage à :";
    }

    /** Durée d'un contrat (actif : temps restant ; proposition : comptée à l'acceptation). */
    private Component duree(Contrat c, Termes t, boolean enVigueur) {
        long maintenant = System.currentTimeMillis();
        if (t.periode == null) {
            if (enVigueur && c.echeance > 0) {
                return t("texte.expire", "<gray>Expiration : dans <reste> (le <date>)", "reste",
                        Textes.duree(c.echeance - maintenant), "date", Textes.date(c.echeance));
            }
            return t.duree > 0 ? t("texte.expire-apres", "<gray>Expiration : <duree> après l'acceptation", "duree",
                    Textes.duree(t.duree)) : t("texte.sans-expiration", "<gray>Sans date d'expiration");
        }
        Component r = t("texte.recurrent", "<gray>Récurrent : <periode>", "periode", Textes.periode(t.periode));
        if (enVigueur) {
            r = r.append(t("texte.periode-jusqu", "<gray> (période en cours jusqu'au <date>)", "date",
                    Textes.date(t.periode.suivante(c.debutPeriode))));
        }
        Component fin = enVigueur && c.echeance > 0
                ? t("texte.fin", "<gray>Fin : dans <reste> (le <date>)", "reste", Textes.duree(c.echeance - maintenant),
                "date", Textes.date(c.echeance))
                : t.duree > 0 ? t("texte.fin-apres", "<gray>Fin : <duree> après l'acceptation", "duree",
                Textes.duree(t.duree)) : t("texte.sans-fin", "<gray>Sans fin (jusqu'à une annulation ou une validation)");
        return r.append(Component.newline()).append(fin);
    }

    /** Texte complet d'une version d'un contrat (fenêtre de lecture). */
    private List<Component> texte(Contrat c, Termes t, boolean enVigueur) {
        List<Component> l = new ArrayList<>();
        l.add(t("texte.partie-a", "<gold>Partie A : <white><noms>", "noms", c.nomsDe(c.partieA)));
        l.add(t("texte.partie-b", "<gold>Partie B : <white><noms>", "noms", c.nomsDe(c.partieB)));
        l.add(Component.text(c.nomsDe(c.partieA) + sEngage(c.partieA), NamedTextColor.YELLOW));
        l.addAll(engagement(t.a, c.livresA, c.pointsLivresA, t.periode, enVigueur));
        l.add(Component.text(c.nomsDe(c.partieB) + sEngage(c.partieB), NamedTextColor.YELLOW));
        l.addAll(engagement(t.b, c.livresB, c.pointsLivresB, t.periode, enVigueur));
        l.add(duree(c, t, enVigueur));
        l.add(t.garantie > 0
                ? t("texte.garantie", "<gray>Garantie : <garantie> par partie, divisés entre ses joueurs", "garantie",
                Textes.points(t.garantie))
                : t("texte.sans-garantie", "<gray>Sans garantie"));
        if (!c.sequestre.isEmpty()) {
            l.add(t("texte.sequestre", "<gray>En séquestre : <total>", "total", Textes.points(c.totalSequestre())));
        }
        return l;
    }

    private Component etat(Contrat c) {
        return switch (c.etat) {
            case PROPOSITION -> t("etat.proposition", "<yellow>Proposition (en attente d'acceptation)");
            case ACTIF -> t("etat.actif", "<green>Actif");
            case ECHU -> t("etat.echu", "<yellow>Arrivé à son terme : à valider ou contester");
            case LITIGE -> t("etat.litige", "<red>EN LITIGE : garanties bloquées jusqu'à la décision d'un admin");
            case TERMINE -> switch (c.fin) {
                case REFUSE -> t("etat.refuse", "<gray>Refusé le <date>", "date", Textes.date(c.dateFin));
                case RETIRE -> t("etat.retire", "<gray>Proposition retirée le <date>", "date", Textes.date(c.dateFin));
                case VALIDE -> t("etat.valide", "<gray>Terminé et validé le <date>", "date", Textes.date(c.dateFin));
                case ANNULE -> t("etat.annule", "<gray>Annulé le <date>", "date", Textes.date(c.dateFin));
                case TRANCHE -> t("etat.tranche", "<gray>Tranché par un admin le <date>", "date", Textes.date(c.dateFin));
            };
        };
    }

    /** Objet d'un contrat vu par ce joueur : son logo, son nom, un résumé (aussi dans /echange). */
    ItemStack icone(Player pour, Contrat c) {
        UUID compte = Registre.compte(pour);
        Registre.Perso perso = registre.persoSiExiste(compte, c);
        ItemStack modele = perso != null && perso.logo != null ? perso.logo.clone()
                : new ItemStack(c.etat == Etat.PROPOSITION ? Material.PAPER : Material.BOOK);
        modele.setAmount(1);
        List<Component> l = new ArrayList<>();
        l.add(etat(c));
        l.add(t("resume.parties", "<gray>A : <white><a></white> · B : <white><b>", "a", c.nomsDe(c.partieA), "b",
                c.nomsDe(c.partieB)));
        Termes t = c.termesVus();
        if (c.etat == Etat.ACTIF || c.etat == Etat.ECHU || c.etat == Etat.PROPOSITION) {
            l.add(duree(c, t, c.etat != Etat.PROPOSITION));
            if (t.garantie > 0) {
                l.add(t("resume.garantie", "<gray>Garantie : <garantie> par partie", "garantie",
                        Textes.points(t.garantie)));
            }
        }
        if (c.etat == Etat.ACTIF && c.participe(compte)) {
            boolean a = c.dansA(compte);
            Engagement mien = t.de(a);
            if (!mien.service) {
                l.add(t("resume.ta-partie", "<yellow>Ta partie doit livrer :"));
                l.addAll(engagement(mien, c.livres(a), a ? c.pointsLivresA : c.pointsLivresB, t.periode, true));
            }
        }
        if (registre.attend(c, compte)) {
            l.add(t("resume.attend", "<gold><bold>Demande ta réponse"));
        } else if (c.demande != null && c.ouvert()) {
            l.add(t("resume.demande", "<gray>Demande en cours, en attente des autres"));
        }
        List<Component> coupees = new ArrayList<>();
        l.forEach(x -> coupees.addAll(Contenant.lignes(x)));
        ItemStack objet = Contenant.objet(modele, Component.text(registre.nomPour(compte, c), NamedTextColor.YELLOW)
                .append(Component.text(" (" + c.id + ")", NamedTextColor.DARK_GRAY)), coupees);
        return registre.favori(compte, c) ? brillant(objet) : objet;
    }

    /** Fenêtre de lecture : le texte complet (une description d'objet serait trop courte). */
    private void lire(Player p, Contrat c, List<Component> corps, Consumer<Player> retour) {
        Contenant.saisie(p, () -> {
            gui.open(p, Component.text(registre.nomPour(Registre.compte(p), c), NamedTextColor.GOLD), corps, List.of(),
                    List.of(), gui.button(t("saisie.retour", "<gray>Retour"), null, retour::accept), 1);
            lang.saveIfNeeded();
        });
    }

    // ------------------------------------------------------------------ « Mes contrats »

    void accueil(Player p, int page) {
        UUID compte = Registre.compte(p);
        List<Contrat> liste = registre.de(compte, Etat.ACTIF, Etat.ECHU, Etat.LITIGE);
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, t("accueil.titre", "<dark_gray>Mes contrats"), "contrats:accueil");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Contrat k = liste.get(i);
            c.poser(i - pg * Contenant.PAR_PAGE, icone(p, k), j -> fiche(j, k.id, j2 -> accueil(j2, pg)));
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("accueil.vide", "<gray>Aucun contrat actif"),
                    gris("Crée ton premier contrat avec le livre et plume en bas.")), null);
        }
        pages(c, pg, pages, this::accueil);
        c.poser(c.bas(1), Contenant.objet(Material.WRITABLE_BOOK, t("accueil.creer", "<green>Créer un contrat"),
                gris("Ton brouillon est gardé si tu fermes le menu.")), j -> {
            nouveaux.computeIfAbsent(compte, u -> new Brouillon());
            brouillon(j);
        });
        int n = registre.attention(compte);
        ItemStack props = Contenant.objet(new ItemStack(Material.PAPER, Math.max(1, Math.min(64, n))),
                t("accueil.propositions", "<aqua>Propositions (<n>)", "n", n),
                gris("Contrats, modifications, validations anticipées, annulations et périodes qui attendent ta "
                        + "réponse, et tes demandes en cours."));
        c.poser(c.bas(2), n > 0 ? brillant(props) : props, j -> propositions(j, 0));
        aide(c, t("aide.accueil1", "<gray>Un contrat garde une trace de vos engagements : un admin peut trancher en "
                + "cas de non-respect."), t("aide.accueil2", "<gray>Toute demande doit être acceptée par tous les autres "
                + "joueurs du contrat. Les biens se livrent par /echange en choisissant le contrat."));
        sortie(c, null);
        c.poser(c.bas(6), Contenant.objet(Material.CHEST, t("accueil.termines", "<gray>Terminés"),
                gris("Contrats finis, annulés, refusés ou tranchés, avec leur historique.")), j -> termines(j, 0));
        if (p.hasPermission("kscontrats.admin")) {
            c.poser(c.bas(7), Contenant.objet(Material.ANVIL, t("accueil.admin", "<light_purple>Litiges (<n>)", "n",
                    registre.litiges()), gris("Admins : contrats en litige, recherche par joueur.")), j -> admin(j, 0, null));
        }
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    void termines(Player p, int page) {
        UUID compte = Registre.compte(p);
        List<Contrat> liste = registre.de(compte, Etat.TERMINE);
        liste.sort((x, y) -> Long.compare(y.dateFin, x.dateFin));
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, t("termines.titre", "<dark_gray>Contrats terminés"), "contrats:termines");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Contrat k = liste.get(i);
            c.poser(i - pg * Contenant.PAR_PAGE, icone(p, k), j -> fiche(j, k.id, j2 -> termines(j2, pg)));
        }
        pages(c, pg, pages, this::termines);
        sortie(c, j -> accueil(j, 0));
        c.ouvrir(p);
    }

    // ------------------------------------------------------------------ propositions

    void propositions(Player p, int page) {
        UUID compte = Registre.compte(p);
        List<ItemStack> objets = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        Consumer<Player> ici = j -> propositions(j, page);
        // Ce qui attend ma réponse d'abord, puis mes demandes en attente des autres.
        for (boolean moi : new boolean[]{true, false}) {
            for (Contrat k : registre.de(compte, Etat.PROPOSITION, Etat.ACTIF, Etat.ECHU)) {
                if (k.demande != null && (k.etat != Etat.ECHU) && moi == !k.demande.acceptes.contains(compte)) {
                    ItemStack o = icone(p, k).withType(moi ? Material.PAPER : Material.MAP);
                    objets.add(moi ? brillant(o) : o);
                    actions.add(j -> ficheDemande(j, k.id, ici));
                }
                if (moi) {
                    for (Bilan b : k.bilans) {
                        if (!b.valides.contains(compte)) {
                            objets.add(brillant(Contenant.objet(Material.CLOCK, Component.text(registre.nomPour(compte, k),
                                    NamedTextColor.YELLOW), lignes(t(b.dernier ? "bilan.fin" : "bilan.periode",
                                    b.dernier ? "<gold>Contrat arrivé à son terme : valider ou contester"
                                            : "<gold>Période terminée : valider ou contester"),
                                    t("bilan.date", "<gray>Du <debut> au <fin>", "debut", Textes.date(b.debut), "fin",
                                            Textes.date(b.fin)), t("bilan.delai", "<gray>Sans réponse le <date> : validé.",
                                            "date", Textes.date(b.fin + Contrat.DELAI_BILAN))))));
                            actions.add(j -> ficheBilan(j, k.id, k.bilans.indexOf(b), ici));
                        }
                    }
                }
            }
        }
        int pages = Math.max(1, (objets.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, t("propositions.titre", "<dark_gray>Propositions"), "contrats:propositions");
        for (int i = pg * Contenant.PAR_PAGE; i < objets.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            c.poser(i - pg * Contenant.PAR_PAGE, objets.get(i), actions.get(i));
        }
        if (objets.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("propositions.vide", "<gray>Rien en attente"), List.of()),
                    null);
        }
        pages(c, pg, pages, this::propositions);
        aide(c, t("aide.propositions", "<gray>Papier brillant : attend ta réponse. Carte : ta demande, en attente des "
                + "autres. Horloge : période à valider ou contester."));
        sortie(c, j -> accueil(j, 0));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    void ficheDemande(Player p, String id, Consumer<Player> retour) {
        Contrat k = registre.contrats.get(id);
        UUID compte = Registre.compte(p);
        if (k == null || k.demande == null || !k.participe(compte)) {
            message(p, t("refus.plus-de-demande", "<red>Cette demande n'attend plus de réponse."), true);
            retour.accept(p);
            return;
        }
        Demande d = k.demande;
        Consumer<Player> ici = j -> ficheDemande(j, id, retour);
        Contenant c = menu(p, 6, t("demande.titre", "<dark_gray>Demande sur le contrat"), "contrats:demande:" + id);
        List<String> attente = new ArrayList<>();
        k.participants().forEach(u -> {
            if (!d.acceptes.contains(u)) {
                attente.add(k.nom(u));
            }
        });
        List<String> ont = new ArrayList<>();
        d.acceptes.forEach(u -> ont.add(k.nom(u)));
        Component type = switch (d.type) {
            case NOUVEAU -> t("demande.nouveau", "<gold><auteur> propose ce contrat", "auteur", k.nom(d.auteur));
            case MODIFICATION -> t("demande.modif", "<gold><auteur> propose une modification", "auteur", k.nom(d.auteur));
            case VALIDATION -> t("demande.validation", "<gold><auteur> demande la validation anticipée", "auteur",
                    k.nom(d.auteur));
            case ANNULATION -> t("demande.annulation", "<gold><auteur> demande l'annulation", "auteur", k.nom(d.auteur));
        };
        c.poser(13, Contenant.objet(Material.WRITTEN_BOOK, t("demande.lire", "<yellow>Lire la demande"),
                lignes(type, t("demande.ont", "<gray>Ont accepté : <noms>", "noms", String.join(", ", ont)),
                        t("demande.attente", "<gray>En attente : <noms>", "noms", String.join(", ", attente)),
                        t("demande.date", "<gray>Demandé le <date>", "date", Textes.date(d.date)),
                        t("demande.clic", "<gray>Clic : texte complet"))), j -> {
            List<Component> corps = new ArrayList<>();
            corps.add(type);
            if (d.termes != null) {
                if (d.type == TypeDemande.MODIFICATION) {
                    corps.add(t("demande.nouvelle-version", "<aqua><bold>Nouvelle version"));
                }
                corps.addAll(texte(k, d.termes, false));
            }
            if (k.termes != null) {
                corps.add(t("demande.en-vigueur", "<aqua><bold>Version en vigueur"));
                corps.addAll(texte(k, k.termes, true));
            }
            lire(j, k, corps, ici);
        });
        boolean auteur = d.auteur.equals(compte);
        if (!auteur && !d.acceptes.contains(compte)) {
            long besoin = d.type == TypeDemande.NOUVEAU ? k.part(compte, d.termes.garantie)
                    : d.type == TypeDemande.MODIFICATION
                    ? Math.max(0, k.part(compte, d.termes.garantie) - k.sequestre.getOrDefault(compte, 0L)) : 0;
            c.poser(29, Contenant.objet(Material.LIME_CONCRETE, t("demande.accepter", "<green><bold>Accepter"),
                    lignes(besoin > 0 ? t("demande.retenu", "<gray>Retenu sur ton score : <points> (ton score : "
                            + "<solde>).", "points", Textes.points(besoin), "solde",
                            Textes.points(KSEconomy.soldeCompte(compte))) : null,
                            t("demande.effectif", "<gray>Effectif quand tous auront accepté."))),
                    j -> apres(j, registre.accepter(k, compte), t("fait.accepte", "<green>Accepté."),
                            () -> retourOuFiche(j, k, retour)));
        } else if (!auteur) {
            c.poser(29, Contenant.objet(Material.LIME_STAINED_GLASS, t("demande.deja", "<green>Tu as accepté"),
                    gris("En attente des autres joueurs.")), null);
        }
        if (!auteur && (d.type == TypeDemande.NOUVEAU || d.type == TypeDemande.MODIFICATION)) {
            c.poser(31, Contenant.objet(Material.FEATHER, t("demande.modifier", "<yellow>Proposer une modification"),
                    gris("Ouvre le brouillon pré-rempli ; ta version repart chez tous les autres joueurs.")), j -> {
                modifs.put(compte, Brouillon.depuis(k, d.termes, compte, true));
                brouillon(j);
            });
        }
        if (auteur) {
            c.poser(31, Contenant.objet(Material.BARRIER, t("demande.retirer", "<red>Retirer ma demande"),
                    gris(d.type == TypeDemande.NOUVEAU ? "La proposition est retirée ; chacun récupère sa garantie."
                            : "Le contrat reste tel qu'il était.")),
                    j -> apres(j, registre.refuser(k, compte), t("fait.retire", "<gray>Demande retirée."),
                            () -> retour.accept(j)));
        } else {
            c.poser(33, Contenant.objet(Material.RED_CONCRETE, t("demande.refuser", "<red><bold>Refuser"),
                    gris(d.type == TypeDemande.NOUVEAU ? "La proposition tombe pour tout le monde ; chacun récupère sa "
                            + "garantie." : "La demande tombe pour tout le monde ; le contrat reste tel qu'il était.")),
                    j -> confirmer(j, t("confirmer.refuser", "<dark_gray>Refuser la demande ?"), List.of(),
                            j2 -> apres(j2, registre.refuser(k, compte), t("fait.refuse", "<gray>Demande refusée."),
                                    () -> retour.accept(j2)), ici));
        }
        aide(c, t("aide.demande", "<gray>Accepter, refuser, ou proposer ta propre version. Un seul refus fait tomber "
                + "la demande pour tout le monde."));
        sortie(c, retour);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void retourOuFiche(Player p, Contrat k, Consumer<Player> retour) {
        if (k.etat == Etat.ACTIF && k.demande == null) {
            fiche(p, k.id, j -> accueil(j, 0));
        } else {
            retour.accept(p);
        }
    }

    void ficheBilan(Player p, String id, int index, Consumer<Player> retour) {
        Contrat k = registre.contrats.get(id);
        UUID compte = Registre.compte(p);
        if (k == null || index < 0 || index >= k.bilans.size() || !k.participe(compte)) {
            message(p, t("refus.plus-de-bilan", "<red>Cette période n'attend plus de réponse."), true);
            retour.accept(p);
            return;
        }
        Bilan b = k.bilans.get(index);
        Consumer<Player> ici = j -> ficheBilan(j, id, index, retour);
        Contenant c = menu(p, 6, t(b.dernier ? "bilan.titre-fin" : "bilan.titre", b.dernier
                ? "<dark_gray>Fin du contrat" : "<dark_gray>Période terminée"), "contrats:bilan:" + id + ":" + index);
        List<Component> l = new ArrayList<>();
        l.add(t("bilan.date", "<gray>Du <debut> au <fin>", "debut", Textes.date(b.debut), "fin", Textes.date(b.fin)));
        l.add(Component.text(k.nomsDe(k.partieA) + " :", NamedTextColor.YELLOW));
        l.addAll(engagement(k.termes.a, b.livresA, b.pointsA, null, true));
        l.add(Component.text(k.nomsDe(k.partieB) + " :", NamedTextColor.YELLOW));
        l.addAll(engagement(k.termes.b, b.livresB, b.pointsB, null, true));
        l.add(t("bilan.delai", "<gray>Sans réponse le <date> : validé.", "date", Textes.date(b.fin + Contrat.DELAI_BILAN)));
        List<Component> coupees = new ArrayList<>();
        l.forEach(x -> coupees.addAll(Contenant.lignes(x)));
        c.poser(4, Contenant.objet(Material.CLOCK, Component.text(registre.nomPour(compte, k), NamedTextColor.YELLOW),
                coupees), j -> lire(j, k, l, ici));
        c.poser(13, Contenant.objet(Material.WRITTEN_BOOK, t("bilan.lire", "<yellow>Lire le contrat"), List.of()),
                j -> lire(j, k, texte(k, k.termes, false), ici));
        if (b.valides.contains(compte)) {
            List<String> attente = new ArrayList<>();
            k.participants().forEach(u -> {
                if (!b.valides.contains(u)) {
                    attente.add(k.nom(u));
                }
            });
            c.poser(29, Contenant.objet(Material.LIME_STAINED_GLASS, t("bilan.deja", "<green>Tu as validé"),
                    lignes(t("bilan.attente", "<gray>En attente : <noms>", "noms", String.join(", ", attente)))), null);
        } else {
            c.poser(29, Contenant.objet(Material.LIME_CONCRETE, t("bilan.valider", "<green><bold>Valider"),
                    gris(b.dernier ? "Garanties rendues quand tous auront validé." : "La période est rangée dans "
                            + "l'historique ; le contrat continue.")),
                    j -> apres(j, registre.validerBilan(k, b, compte), t("fait.valide", "<green>Validé."),
                            () -> retour.accept(j)));
        }
        c.poser(33, Contenant.objet(Material.RED_BANNER, t("bilan.contester", "<red><bold>Contester"),
                gris("Explique ce qui n'a pas été respecté : le contrat passe en litige, garanties bloquées, admins "
                        + "prévenus.")), j -> signaler(j, k, ici, retour));
        sortie(c, retour);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void signaler(Player p, Contrat k, Consumer<Player> retour, Consumer<Player> apresSignalement) {
        UUID compte = Registre.compte(p);
        saisirTexte(p, t("signaler.titre", "<red><bold>Signaler un non-respect"), t("signaler.texte",
                "<white>Explique ce qui n'a pas été respecté (lu par l'admin qui tranchera)."), "", 300, true, texte -> {
            if (texte.isBlank()) {
                p.sendMessage(t("signaler.vide", "<red>Écris une explication."));
                retour.accept(p);
                return;
            }
            apres(p, registre.signaler(k, compte, texte), t("fait.signale", "<yellow>Signalé : le contrat est en "
                    + "litige."), () -> fiche(p, k.id, j -> accueil(j, 0)));
        }, retour);
    }

    // ------------------------------------------------------------------ fiche d'un contrat

    void fiche(Player p, String id, Consumer<Player> retour) {
        Contrat k = registre.contrats.get(id);
        UUID compte = Registre.compte(p);
        if (k == null || !k.participe(compte)) {
            retour.accept(p);
            return;
        }
        Object marque = "contrats:fiche:" + id;
        Consumer<Player> ici = j -> fiche(j, id, retour);
        Contenant c = menu(p, 6, t("fiche.titre", "<dark_gray>Contrat"), marque);
        c.poser(4, icone(p, k), j -> lire(j, k, k.termes != null ? texte(k, k.termes, k.etat == Etat.ACTIF)
                : texte(k, k.termesVus(), false), ici));
        Registre.Perso perso = registre.persoSiExiste(compte, k);
        c.poser(19, Contenant.objet(Material.NAME_TAG, t("fiche.renommer", "<yellow>Renommer"),
                gris("Nom personnel : seul toi le vois.")), j -> saisirTexte(j, t("fiche.renommer-titre",
                "<yellow><bold>Nom du contrat"), t("fiche.renommer-texte", "<white>Vide : nom par défaut."),
                perso != null && perso.nom != null ? perso.nom : "", 32, false, texte -> {
                    registre.perso(compte, k).nom = texte.isBlank() ? null : texte;
                    registre.modifie();
                    registre.sauverSiBesoin();
                    fiche(j, id, retour);
                }, ici));
        c.poser(20, Contenant.objet(Material.ITEM_FRAME, t("fiche.logo", "<yellow>Changer le logo"),
                gris("Clique sur un objet de ton inventaire pendant que cette fiche est ouverte. Clic ici : logo par "
                        + "défaut. Logo personnel.")), j -> {
            registre.perso(compte, k).logo = null;
            registre.modifie();
            fiche(j, id, retour);
        });
        boolean fav = registre.favori(compte, k);
        ItemStack etoile = Contenant.objet(Material.NETHER_STAR, fav ? t("fiche.favori-oui", "<gold>Favori : oui")
                : t("fiche.favori-non", "<gray>Favori : non"), gris("Les favoris sont en tête de « Mes contrats »."));
        c.poser(21, fav ? brillant(etoile) : etoile, j -> {
            registre.perso(compte, k).favori = !fav;
            registre.modifie();
            fiche(j, id, retour);
        });
        if (k.etat == Etat.ACTIF && k.demande == null) {
            c.poser(23, Contenant.objet(Material.WRITABLE_BOOK, t("fiche.modifier", "<yellow>Modifier"),
                    gris("Brouillon pré-rempli, à faire accepter par tous. L'ancienne version reste en vigueur d'ici "
                            + "là.")), j -> {
                modifs.put(compte, Brouillon.depuis(k, k.termes, compte, false));
                brouillon(j);
            });
            c.poser(24, Contenant.objet(Material.LIME_DYE, t("fiche.valider", "<green>Valider en avance"),
                    gris("Contrat rempli : à faire accepter par tous ; garanties rendues.")),
                    j -> confirmer(j, t("confirmer.valider", "<dark_gray>Demander la validation ?"), List.of(),
                            j2 -> apres(j2, registre.demander(k, compte, TypeDemande.VALIDATION, null),
                                    t("fait.demande", "<green>Demande envoyée aux autres joueurs."), () -> fiche(j2, id,
                                            retour)), ici));
            c.poser(25, Contenant.objet(Material.RED_DYE, t("fiche.annuler", "<red>Annuler"),
                    gris("À faire accepter par tous ; garanties rendues.")),
                    j -> confirmer(j, t("confirmer.annuler", "<dark_gray>Demander l'annulation ?"), List.of(),
                            j2 -> apres(j2, registre.demander(k, compte, TypeDemande.ANNULATION, null),
                                    t("fait.demande", "<green>Demande envoyée aux autres joueurs."), () -> fiche(j2, id,
                                            retour)), ici));
        } else if (k.demande != null && k.ouvert()) {
            c.poser(24, Contenant.objet(Material.CLOCK, t("fiche.demande", "<gold>Demande en cours"),
                    gris("Clic : voir la demande.")), j -> ficheDemande(j, id, ici));
        } else {
            c.poser(24, Contenant.objet(k.etat == Etat.LITIGE ? Material.RED_BANNER : Material.GRAY_DYE,
                    etat(k), List.of()), null);
        }
        if (k.etat == Etat.ACTIF || k.etat == Etat.ECHU) {
            c.poser(39, Contenant.objet(Material.RED_BANNER, t("fiche.signaler", "<red>Signaler un non-respect"),
                    gris("Texte d'explication ; contrat en litige, garanties bloquées, admins prévenus.")),
                    j -> signaler(j, k, ici, retour));
            for (int i = 0; i < k.bilans.size(); i++) {
                if (!k.bilans.get(i).valides.contains(compte)) {
                    int index = i;
                    c.poser(40, brillant(Contenant.objet(Material.CLOCK, t("fiche.bilan", "<gold>Période à valider"),
                            gris("Valider ou contester."))), j -> ficheBilan(j, id, index, ici));
                    break;
                }
            }
        }
        c.poser(41, Contenant.objet(Material.BOOKSHELF, t("fiche.historique", "<yellow>Versions et historique"),
                gris("Chaque version acceptée, avec la date et ses signataires, et tout ce qui s'est passé.")),
                j -> lire(j, k, historique(k), ici));
        aide(c, t("aide.fiche", "<gray>À gauche : tes réglages personnels. À droite : les demandes communes, à faire "
                + "accepter par tous."));
        sortie(c, retour);
        clicsInventaire.put(p.getUniqueId(), Map.entry(marque, objet -> {
            ItemStack logo = objet.clone();
            logo.setAmount(1);
            registre.perso(compte, k).logo = logo;
            registre.modifie();
            registre.sauverSiBesoin();
            Contenant.apres(() -> fiche(p, id, retour));
        }));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private List<Component> historique(Contrat k) {
        List<Component> l = new ArrayList<>();
        for (Contrat.Version v : k.versions) {
            l.add(t("historique.version", "<aqua><bold>Version <n></bold> <gray>du <date>, signée par <noms>", "n",
                    v.numero, "date", Textes.date(v.date), "noms", String.join(", ", v.signataires)));
            l.addAll(texte(k, v.termes, false));
        }
        l.add(t("historique.titre", "<aqua><bold>Historique"));
        int debut = Math.max(0, k.historique.size() - 80);
        for (int i = debut; i < k.historique.size(); i++) {
            Map.Entry<Long, String> e = k.historique.get(i);
            l.add(Component.text(Textes.date(e.getKey()) + " : ", NamedTextColor.GRAY)
                    .append(Component.text(e.getValue(), NamedTextColor.WHITE)));
        }
        return l;
    }

    // ------------------------------------------------------------------ brouillon

    private Brouillon brouillonDe(UUID compte) {
        Brouillon b = modifs.get(compte);
        return b != null ? b : nouveaux.get(compte);
    }

    private void retourBrouillon(Player p) {
        UUID compte = Registre.compte(p);
        Brouillon b = brouillonDe(compte);
        if (b != null && b.contrat != null) {
            modifs.remove(compte);
            fiche(p, b.contrat, j -> accueil(j, 0));
        } else {
            accueil(p, 0);
        }
    }

    void brouillon(Player p) {
        UUID compte = Registre.compte(p);
        Brouillon b = brouillonDe(compte);
        if (b == null) {
            accueil(p, 0);
            return;
        }
        Contrat k = b.contrat == null ? null : registre.contrats.get(b.contrat);
        if (b.contrat != null && (k == null || !k.ouvert())) {
            modifs.remove(compte);
            accueil(p, 0);
            return;
        }
        boolean fixe = k != null;
        Contenant c = menu(p, 6, b.contrat == null ? t("brouillon.titre", "<dark_gray>Créer un contrat")
                : t("brouillon.titre-modif", "<dark_gray>Modifier le contrat"), "contrats:brouillon");
        List<String> equipe = new ArrayList<>();
        equipe.add(p.getName());
        b.equipe.forEach(u -> equipe.add(b.noms.getOrDefault(u, "?")));
        List<String> autres = new ArrayList<>();
        b.autres.forEach(u -> autres.add(b.noms.getOrDefault(u, "?")));
        if (fixe) {
            c.poser(10, Contenant.objet(Material.PLAYER_HEAD, t("brouillon.equipe-fixe", "<gray>1. Ma partie"),
                    gris(k.nomsDe(b.moiA ? k.partieA : k.partieB) + " (fixé : les joueurs ne changent pas dans une "
                            + "modification)")), null);
            c.poser(11, Contenant.objet(Material.PLAYER_HEAD, t("brouillon.autres-fixe", "<gray>2. L'autre partie"),
                    gris(k.nomsDe(b.moiA ? k.partieB : k.partieA))), null);
        } else {
            c.poser(10, Contenant.objet(Material.PLAYER_HEAD, t("brouillon.equipe", "<aqua>1. Mon équipe <gray>"
                    + "(facultatif)"), gris(String.join(", ", equipe) + ". Clic : choisir les joueurs de ton côté.")),
                    j -> choixJoueurs(j, true, 0));
            c.poser(11, Contenant.objet(Material.PLAYER_HEAD, t("brouillon.autres", "<aqua>2. L'autre partie"),
                    gris((autres.isEmpty() ? "Personne pour l'instant" : String.join(", ", autres))
                            + ". Clic : choisir avec qui passer le contrat.")), j -> choixJoueurs(j, false, 0));
        }
        c.poser(12, etape(t("brouillon.mien", "<yellow>3. Mon engagement"), b.mien(), b.moiA ? b.typeA : b.typeB),
                j -> choixType(j, true));
        c.poser(13, etape(t("brouillon.leur", "<yellow>4. Leur engagement"), b.leur(), b.moiA ? b.typeB : b.typeA),
                j -> choixType(j, false));
        List<Component> d = new ArrayList<>(Contenant.lignes(dureeBrouillon(b.termes)));
        d.addAll(gris("Clic : une fois ou récurrent, expiration ou fin."));
        c.poser(14, Contenant.objet(b.termes.periode == null ? Material.CLOCK : Material.REPEATER,
                t("brouillon.duree", "<yellow>5. Durée <gray>(facultatif)"), d), this::duree);
        List<Component> g = new ArrayList<>();
        if (b.termes.garantie > 0) {
            g.addAll(gris(Textes.points(b.termes.garantie) + " par partie, divisés entre ses joueurs, retenus jusqu'à "
                    + "la fin du contrat."));
        } else {
            g.addAll(gris("Aucune."));
        }
        g.addAll(gris("Clic : changer le montant (0 : aucune)."));
        c.poser(15, Contenant.objet(Material.GOLD_INGOT, t("brouillon.garantie", "<yellow>6. Garantie <gray>"
                + "(facultatif)"), g), j -> saisirNombre(j, t("brouillon.garantie-titre", "<yellow><bold>Garantie"),
                t("brouillon.garantie-texte", "<white>Points par partie, divisés entre ses joueurs (0 : aucune). Chacun "
                        + "paie sa part quand il accepte, rendue à la fin du contrat."), b.termes.garantie,
                1_000_000_000L, n -> {
                    b.termes.garantie = n;
                    brouillon(j);
                }, this::brouillon));
        c.poser(16, brillant(Contenant.objet(Material.LIME_CONCRETE, b.contrat == null
                ? t("brouillon.envoyer", "<green><bold>Envoyer la proposition")
                : t("brouillon.envoyer-modif", "<green><bold>Envoyer la modification"),
                gris("Active quand tous les autres joueurs auront accepté."))), this::envoyer);
        if (b.contrat == null) {
            c.poser(22, Contenant.objet(Material.LAVA_BUCKET, t("brouillon.effacer", "<red>Effacer le brouillon"),
                    List.of()), j -> confirmer(j, t("confirmer.effacer", "<dark_gray>Effacer le brouillon ?"), List.of(),
                    j2 -> {
                        nouveaux.remove(compte);
                        accueil(j2, 0);
                    }, this::brouillon));
        }
        aide(c, t("aide.brouillon", "<gray>Remplis les cases dans l'ordre que tu veux. Le brouillon est gardé si tu "
                + "fermes le menu (jusqu'au redémarrage du serveur)."));
        sortie(c, this::retourBrouillon);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private ItemStack etape(Component nom, Engagement e, boolean typeChoisi) {
        List<Component> l = new ArrayList<>();
        if (!typeChoisi) {
            l.addAll(gris("À remplir. Clic : choisir bien ou service."));
        } else {
            l.add(e.service ? t("type.service", "<light_purple>Service") : t("type.bien", "<green>Bien"));
            for (Component x : engagement(e, null, 0, null, false)) {
                l.addAll(Contenant.lignes(x));
            }
            l.addAll(gris("Clic : changer."));
        }
        return Contenant.objet(e.service ? Material.IRON_PICKAXE : Material.CHEST, nom, l);
    }

    private Component dureeBrouillon(Termes t) {
        if (t.periode == null) {
            return t.duree > 0 ? t("texte.expire-apres", "<gray>Expiration : <duree> après l'acceptation", "duree",
                    Textes.duree(t.duree)) : t("brouillon.une-fois", "<gray>Une fois, sans date d'expiration");
        }
        return t("brouillon.recurrent", "<gray>Récurrent : <periode> ; <fin>", "periode", Textes.periode(t.periode),
                "fin", t.duree > 0 ? "fin " + Textes.duree(t.duree) + " après l'acceptation" : "sans fin");
    }

    private void envoyer(Player p) {
        UUID compte = Registre.compte(p);
        Brouillon b = brouillonDe(compte);
        if (b == null) {
            accueil(p, 0);
            return;
        }
        if (!b.typeA || !b.typeB) {
            message(p, t("refus.type", "<red>Remplis les deux engagements (étapes 3 et 4)."), true);
            return;
        }
        Termes t = b.termes;
        for (Engagement e : List.of(t.a, t.b)) {
            if (e.service) {
                e.lignes.clear();
                e.points = 0;
            }
        }
        if (b.contrat == null) {
            apres(p, registre.proposer(compte, p.getName(), b), t("fait.envoye", "<green>Proposition envoyée."), () -> {
                nouveaux.remove(compte);
                propositions(p, 0);
            });
            return;
        }
        Contrat k = registre.contrats.get(b.contrat);
        if (k == null) {
            modifs.remove(compte);
            accueil(p, 0);
            return;
        }
        Component refus = b.contre ? registre.contreProposer(k, compte, t)
                : registre.demander(k, compte, TypeDemande.MODIFICATION, t);
        apres(p, refus, t("fait.envoye-modif", "<green>Modification envoyée aux autres joueurs."), () -> {
            modifs.remove(compte);
            propositions(p, 0);
        });
    }

    // ------------------------------------------------------------------ étapes 1 et 2 : joueurs

    void choixJoueurs(Player p, boolean equipe, int page) {
        UUID compte = Registre.compte(p);
        Brouillon b = brouillonDe(compte);
        if (b == null || b.contrat != null) {
            brouillon(p);
            return;
        }
        List<UUID> choisis = equipe ? b.equipe : b.autres;
        List<UUID> autreCote = equipe ? b.autres : b.equipe;
        boolean test = CompteTest.enTest(p);
        // Choisis d'abord (même hors ligne), puis les joueurs en ligne du même monde (survie ou monde de test).
        List<UUID> liste = new ArrayList<>(choisis);
        Map<UUID, UUID> vrais = new LinkedHashMap<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            UUID co = Registre.compte(o);
            if (o != p && CompteTest.enTest(o) == test) {
                vrais.put(co, o.getUniqueId());
                b.noms.putIfAbsent(co, o.getName());
                if (!liste.contains(co)) {
                    liste.add(co);
                }
            }
        }
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, equipe ? t("joueurs.titre-equipe", "<dark_gray>Mon équipe")
                : t("joueurs.titre-autres", "<dark_gray>L'autre partie"), "contrats:joueurs:" + equipe);
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            UUID co = liste.get(i);
            String nom = b.noms.getOrDefault(co, "?");
            boolean choisi = choisis.contains(co);
            boolean enFace = autreCote.contains(co);
            ItemStack tete = vrais.containsKey(co) ? tete(vrais.get(co)) : teteDe(co, nom);
            List<Component> l = new ArrayList<>(gris(vrais.containsKey(co) ? "En ligne" : "Hors ligne"));
            l.addAll(gris(enFace ? "Déjà dans l'autre partie : impossible d'être des deux côtés."
                    : choisi ? "Choisi. Clic : retirer." : "Clic : ajouter."));
            ItemStack o = Contenant.objet(tete, Component.text(nom, choisi ? NamedTextColor.GREEN : NamedTextColor.YELLOW), l);
            c.poser(i - pg * Contenant.PAR_PAGE, choisi ? brillant(o) : o, enFace ? null : j -> {
                if (choisis.contains(co)) {
                    choisis.remove(co);
                } else if (choisis.size() >= (equipe ? Registre.PAR_PARTIE - 1 : Registre.PAR_PARTIE)) {
                    message(j, t("refus.trop", "<red>10 joueurs au plus par partie."), true);
                    return;
                } else {
                    choisis.add(co);
                }
                choixJoueurs(j, equipe, pg);
            });
        }
        pages(c, pg, pages, (j, n) -> choixJoueurs(j, equipe, n));
        aide(c, t("aide.joueurs", "<gray>Joueurs en ligne sur Kixster (dans le monde de test : ceux du monde de test). "
                + "Pour un joueur hors ligne : « Autre pseudo »."));
        c.poser(c.bas(4), Contenant.objet(Material.NAME_TAG, t("joueurs.autre", "<yellow>Autre pseudo"),
                gris("Joueur déjà venu sur Kixster, même hors ligne.")), j -> saisirTexte(j, t("joueurs.autre-titre",
                "<yellow><bold>Pseudo du joueur"), null, "", 16, false, nom -> {
                    ajouterPseudo(j, b, choisis, autreCote, nom);
                    choixJoueurs(j, equipe, pg);
                }, j2 -> choixJoueurs(j2, equipe, pg)));
        sortie(c, this::brouillon);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void ajouterPseudo(Player p, Brouillon b, List<UUID> choisis, List<UUID> autreCote, String nom) {
        OfflinePlayer o = nom.isBlank() ? null : Bukkit.getOfflinePlayerIfCached(nom);
        if (o == null || (!o.hasPlayedBefore() && !o.isOnline())) {
            p.sendMessage(t("refus.inconnu", "<red>Joueur inconnu sur Kixster : <nom>.", "nom", nom));
            return;
        }
        UUID co = CompteTest.enTest(p) ? CompteTest.compteDeTest(o.getUniqueId()) : o.getUniqueId();
        if (co.equals(Registre.compte(p))) {
            p.sendMessage(t("refus.soi", "<red>Tu fais déjà partie du contrat."));
            return;
        }
        if (autreCote.contains(co) || choisis.contains(co)) {
            p.sendMessage(t("refus.deja-choisi", "<red>Ce joueur est déjà choisi."));
            return;
        }
        if (choisis.size() >= Registre.PAR_PARTIE) {
            p.sendMessage(t("refus.trop", "<red>10 joueurs au plus par partie."));
            return;
        }
        b.noms.put(co, o.getName() == null ? nom : o.getName());
        choisis.add(co);
    }

    // ------------------------------------------------------------------ étapes 3 et 4 : engagements

    void choixType(Player p, boolean mien) {
        Brouillon b = brouillonDe(Registre.compte(p));
        if (b == null) {
            accueil(p, 0);
            return;
        }
        Engagement e = mien ? b.mien() : b.leur();
        boolean choisi = (b.moiA == mien) ? b.typeA : b.typeB;
        Contenant c = menu(p, 3, mien ? t("type.titre-mien", "<dark_gray>Mon engagement")
                : t("type.titre-leur", "<dark_gray>Leur engagement"), "contrats:type:" + mien);
        ItemStack bien = Contenant.objet(Material.CHEST, t("type.bien", "<green>Bien"),
                gris("Objets ou points à livrer, avec leur quantité (5 objets différents au plus). Livrés par /echange "
                        + ": suivis dans le contrat (10/64)."));
        ItemStack service = Contenant.objet(Material.IRON_PICKAXE, t("type.service", "<light_purple>Service"),
                gris("Texte libre : creuser une zone, faire un build, ramener un mob... Validé ou contesté par les "
                        + "joueurs."));
        c.poser(11, choisi && !e.service ? brillant(bien) : bien, j -> {
            if (!choisi || e.service) {
                e.service = false;
                e.texte = "";
                marquerType(b, mien);
            }
            bien(j, mien);
        });
        c.poser(15, choisi && e.service ? brillant(service) : service, j -> saisirTexte(j, mien
                ? t("type.service-mien", "<yellow><bold>Ce à quoi ta partie s'engage")
                : t("type.service-leur", "<yellow><bold>Ce à quoi l'autre partie s'engage"), null,
                e.service ? e.texte : "", 400, true, texte -> {
                    if (texte.isBlank()) {
                        j.sendMessage(t("refus.texte-vide", "<red>Écris l'engagement."));
                        choixType(j, mien);
                        return;
                    }
                    e.service = true;
                    e.texte = texte;
                    e.lignes.clear();
                    e.points = 0;
                    marquerType(b, mien);
                    brouillon(j);
                }, j2 -> choixType(j2, mien)));
        sortie(c, this::brouillon);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private static void marquerType(Brouillon b, boolean mien) {
        if (b.moiA == mien) {
            b.typeA = true;
        } else {
            b.typeB = true;
        }
    }

    void bien(Player p, boolean mien) {
        Brouillon b = brouillonDe(Registre.compte(p));
        if (b == null) {
            accueil(p, 0);
            return;
        }
        Engagement e = mien ? b.mien() : b.leur();
        Object marque = "contrats:bien:" + mien;
        Contenant c = menu(p, 6, mien ? t("bien.titre-mien", "<dark_gray>Ce que ma partie livre")
                : t("bien.titre-leur", "<dark_gray>Ce que l'autre partie livre"), marque);
        for (int i = 0; i < 5; i++) {
            if (i < e.lignes.size()) {
                Ligne l = e.lignes.get(i);
                int index = i;
                List<Component> d = new ArrayList<>(gris(l.exact ? "Choisi dans l'inventaire : il faudra livrer cet "
                        + "objet précis (usure non comptée)." : "Nom écrit : n'importe quel objet de ce type compte."));
                d.addAll(gris("Clic : changer la quantité (0 : retirer la ligne)."));
                ItemStack o = Contenant.objet(l.modele, Component.text(Textes.nombre(l.quantite) + " ",
                        NamedTextColor.YELLOW).append(Textes.nomObjet(l.modele)), d);
                o.setAmount(Math.max(1, Math.min(64, l.quantite)));
                c.poser(10 + i, o, j -> quantite(j, mien, index));
            } else {
                c.poser(10 + i, Contenant.objet(Material.LIGHT_GRAY_STAINED_GLASS_PANE, t("bien.libre",
                        "<gray>Case libre"), gris("Clique sur un objet de ton inventaire, ou écris son nom.")), null);
            }
        }
        c.poser(16, Contenant.objet(Material.PAPER, t("bien.precision", "<yellow>Précision <gray>(facultatif)"),
                gris(e.texte.isBlank() ? "Aucune. Exemple : « livré au coffre de ma base »." : e.texte)),
                j -> saisirTexte(j, t("bien.precision-titre", "<yellow><bold>Précision"), null, e.texte, 200, true,
                        texte -> {
                            e.texte = texte;
                            bien(j, mien);
                        }, j2 -> bien(j2, mien)));
        c.poser(29, Contenant.objet(Material.NAME_TAG, t("bien.nom", "<yellow>Écrire le nom d'un objet"),
                gris("En français (« bloc de fer ») ; n'importe quel objet de ce type comptera.")),
                j -> saisirTexte(j, t("bien.nom-titre", "<yellow><bold>Nom de l'objet"), null, "", 40, false,
                        texte -> resultats(j, mien, texte), j2 -> bien(j2, mien)));
        c.poser(31, Contenant.objet(Material.GOLD_INGOT, e.points > 0 ? t("bien.points", "<yellow><points>", "points",
                Textes.points(e.points)) : t("bien.points-aucun", "<yellow>Ajouter des points"),
                gris("Clic : changer le montant (0 : aucun).")), j -> saisirNombre(j, t("bien.points-titre",
                "<yellow><bold>Points à livrer"), null, e.points, 1_000_000_000L, n -> {
                    e.points = n;
                    bien(j, mien);
                }, j2 -> bien(j2, mien)));
        c.poser(33, Contenant.objet(Material.HOPPER, t("bien.inventaire", "<yellow>Ajouter un objet de l'inventaire"),
                gris("Clique sur un objet de ton inventaire : il faudra livrer exactement cet objet (mêmes "
                        + "enchantements, usure non comptée). Quantité : celle du stack, à changer ensuite.")), null);
        aide(c, t("aide.bien", "<gray>5 objets différents au plus, et des points. Livrés par /echange en choisissant "
                + "le contrat : la progression s'affiche (10/64)."));
        c.poser(c.bas(4), Contenant.objet(Material.LIME_CONCRETE, t("bien.fini", "<green>Terminé"), List.of()),
                this::brouillon);
        sortie(c, this::brouillon);
        clicsInventaire.put(p.getUniqueId(), Map.entry(marque, objet -> {
            if (e.lignes.size() >= 5) {
                message(p, t("refus.cinq", "<red>5 objets différents au plus."), true);
                return;
            }
            for (Ligne l : e.lignes) {
                if (l.exact && l.correspond(objet)) {
                    message(p, t("refus.deja-ligne", "<red>Cet objet est déjà dans la liste : clique dessus pour "
                            + "changer sa quantité."), true);
                    return;
                }
            }
            Ligne l = new Ligne();
            l.modele = objet.clone();
            l.modele.setAmount(1);
            l.exact = true;
            l.quantite = objet.getAmount();
            e.lignes.add(l);
            Contenant.apres(() -> bien(p, mien));
        }));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void quantite(Player p, boolean mien, int index) {
        Brouillon b = brouillonDe(Registre.compte(p));
        if (b == null) {
            accueil(p, 0);
            return;
        }
        Engagement e = mien ? b.mien() : b.leur();
        if (index >= e.lignes.size()) {
            bien(p, mien);
            return;
        }
        Ligne l = e.lignes.get(index);
        saisirNombre(p, t("bien.quantite-titre", "<yellow><bold>Quantité"), t("bien.quantite-texte",
                "<white>0 : retirer la ligne."), l.quantite, 1_000_000L, n -> {
            if (n == 0) {
                e.lignes.remove(l);
            } else {
                l.quantite = (int) (long) n;
            }
            bien(p, mien);
        }, j -> bien(j, mien));
    }

    private void resultats(Player p, boolean mien, String texte) {
        List<Material> trouves = texte.isBlank() ? List.of() : KSEconomy.chercherObjets(texte);
        if (trouves.isEmpty()) {
            p.sendMessage(t("refus.aucun-objet", "<red>Aucun objet ne correspond à « <texte> ».", "texte", texte));
            bien(p, mien);
            return;
        }
        Contenant c = menu(p, 6, t("resultats.titre", "<dark_gray>Objets trouvés"), "contrats:resultats");
        for (int i = 0; i < trouves.size() && i < Contenant.PAR_PAGE; i++) {
            Material m = trouves.get(i);
            ItemStack modele = new ItemStack(m);
            c.poser(i, Contenant.objet(modele, Textes.nomObjet(modele).color(NamedTextColor.YELLOW),
                    gris("Clic : choisir, puis la quantité.")), j -> {
                Brouillon b = brouillonDe(Registre.compte(j));
                if (b == null) {
                    accueil(j, 0);
                    return;
                }
                Engagement e = mien ? b.mien() : b.leur();
                for (Ligne l : e.lignes) {
                    if (!l.exact && l.modele.getType() == m) {
                        message(j, t("refus.deja-ligne", "<red>Cet objet est déjà dans la liste : clique dessus pour "
                                + "changer sa quantité."), true);
                        return;
                    }
                }
                if (e.lignes.size() >= 5) {
                    message(j, t("refus.cinq", "<red>5 objets différents au plus."), true);
                    return;
                }
                Ligne l = new Ligne();
                l.modele = modele;
                l.exact = false;
                l.quantite = 1;
                e.lignes.add(l);
                quantite(j, mien, e.lignes.size() - 1);
            });
        }
        sortie(c, j -> bien(j, mien));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ étape 5 : durée

    void duree(Player p) {
        Brouillon b = brouillonDe(Registre.compte(p));
        if (b == null) {
            accueil(p, 0);
            return;
        }
        Termes t = b.termes;
        Contenant c = menu(p, 3, t("duree.titre", "<dark_gray>Durée"), "contrats:duree");
        ItemStack fois = Contenant.objet(Material.CLOCK, t("duree.une-fois", "<yellow>Une fois"),
                gris("Contrat « one shot » ; date d'expiration facultative."));
        c.poser(10, t.periode == null ? brillant(fois) : fois, j -> {
            t.periode = null;
            duree(j);
        });
        Periode[] periodes = Periode.values();
        Material[] icones = {Material.SUNFLOWER, Material.REPEATER, Material.COMPARATOR, Material.FILLED_MAP};
        for (int i = 0; i < periodes.length; i++) {
            Periode pr = periodes[i];
            String nom = Textes.periode(pr);
            ItemStack o = Contenant.objet(icones[i], Component.text(Character.toUpperCase(nom.charAt(0))
                    + nom.substring(1), NamedTextColor.YELLOW), gris("Récurrent, à compter de l'acceptation : les "
                    + "quantités sont dues à chaque période."));
            c.poser(12 + i, t.periode == pr ? brillant(o) : o, j -> {
                t.periode = pr;
                duree(j);
            });
        }
        List<Component> l = new ArrayList<>(Contenant.lignes(dureeBrouillon(t)));
        l.addAll(gris("Clic : jours, heures, minutes après l'acceptation, ou « Aucune »."));
        c.poser(16, Contenant.objet(Material.WHITE_BANNER, t.periode == null
                ? t("duree.expiration", "<yellow>Expiration <gray>(facultatif)")
                : t("duree.fin", "<yellow>Fin du récurrent <gray>(facultatif)"), l), this::saisirExpiration);
        sortie(c, this::brouillon);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void saisirExpiration(Player p) {
        Brouillon b = brouillonDe(Registre.compte(p));
        if (b == null) {
            accueil(p, 0);
            return;
        }
        Termes t = b.termes;
        long minutes = t.duree / Contrat.MINUTE;
        Contenant.saisie(p, () -> {
            List<ActionButton> boutons = List.of(
                    gui.form(t("saisie.valider", "<green>Valider"), null, (j, vue) -> {
                        long total = 0;
                        try {
                            total += lire(vue.getText("jours")) * Contrat.JOUR;
                            total += lire(vue.getText("heures")) * Contrat.HEURE;
                            total += lire(vue.getText("minutes")) * Contrat.MINUTE;
                        } catch (NumberFormatException e) {
                            total = -1;
                        }
                        if (total < 0 || total > 3650L * Contrat.JOUR) {
                            j.sendMessage(t("saisie.duree-invalide", "<red>Durée invalide."));
                        } else {
                            t.duree = total;
                        }
                        duree(j);
                    }),
                    gui.button(t("duree.aucune", "<yellow>Aucune"), null, j -> {
                        t.duree = 0;
                        duree(j);
                    }));
            gui.open(p, t("duree.saisie-titre", "<yellow><bold>Dans combien de temps ?"),
                    List.of(t("duree.saisie-texte", "<white>Compté à partir de l'acceptation du contrat.")),
                    List.of(champ("jours", t("duree.jours", "Jours"), String.valueOf(minutes / (24 * 60)), 4, false),
                            champ("heures", t("duree.heures", "Heures"), String.valueOf(minutes / 60 % 24), 2, false),
                            champ("minutes", t("duree.minutes", "Minutes"), String.valueOf(minutes % 60), 2, false)),
                    boutons, gui.button(t("saisie.retour", "<gray>Retour"), null, this::duree), 2);
            lang.saveIfNeeded();
        });
    }

    private static long lire(String texte) {
        String s = texte == null ? "" : texte.replaceAll("\\s", "");
        long n = s.isEmpty() ? 0 : Long.parseLong(s);
        if (n < 0) {
            throw new NumberFormatException();
        }
        return n;
    }

    // ------------------------------------------------------------------ admin

    void admin(Player p, int page, String filtre) {
        if (!p.hasPermission("kscontrats.admin")) {
            accueil(p, 0);
            return;
        }
        List<Contrat> liste = new ArrayList<>();
        UUID cherche = null;
        if (filtre != null) {
            OfflinePlayer o = Bukkit.getOfflinePlayerIfCached(filtre);
            cherche = o == null ? null : o.getUniqueId();
        }
        for (Contrat k : registre.contrats.values()) {
            if (cherche == null ? k.etat == Etat.LITIGE
                    : k.participe(cherche) || k.participe(CompteTest.compteDeTest(cherche))) {
                liste.add(k);
            }
        }
        liste.sort((x, y) -> Long.compare(y.cree, x.cree));
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, filtre == null ? t("admin.titre", "<dark_gray>Contrats en litige")
                : t("admin.titre-joueur", "<dark_gray>Contrats de <joueur>", "joueur", filtre), "contrats:admin");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Contrat k = liste.get(i);
            ItemStack o = Contenant.objet(k.etat == Etat.LITIGE ? Material.RED_BANNER : Material.BOOK,
                    Component.text(k.nomsDe(k.partieA) + " / " + k.nomsDe(k.partieB), NamedTextColor.YELLOW)
                            .append(Component.text(" (" + k.id + ")", NamedTextColor.DARK_GRAY)),
                    lignes(etat(k), t("admin.sequestre", "<gray>En séquestre : <points>", "points",
                            Textes.points(k.totalSequestre())), t("admin.cree", "<gray>Créé le <date>", "date",
                            Textes.date(k.cree))));
            c.poser(i - pg * Contenant.PAR_PAGE, o, j -> adminFiche(j, k.id, j2 -> admin(j2, pg, filtre)));
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, filtre == null ? t("admin.vide", "<gray>Aucun litige")
                    : t("admin.vide-joueur", "<gray>Aucun contrat"), List.of()), null);
        }
        pages(c, pg, pages, (j, n) -> admin(j, n, filtre));
        c.poser(c.bas(4), Contenant.objet(Material.SPYGLASS, t("admin.chercher", "<yellow>Contrats d'un joueur"),
                gris("Tous ses contrats, terminés compris (et ceux de son moi de test).")),
                j -> saisirTexte(j, t("admin.chercher-titre", "<yellow><bold>Pseudo du joueur"), null, "", 16, false,
                        nom -> admin(j, 0, nom.isBlank() ? null : nom), j2 -> admin(j2, pg, filtre)));
        if (filtre != null) {
            c.poser(c.bas(6), Contenant.objet(Material.RED_BANNER, t("admin.litiges", "<yellow>Litiges"), List.of()),
                    j -> admin(j, 0, null));
        }
        sortie(c, j -> accueil(j, 0));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    void adminFiche(Player p, String id, Consumer<Player> retour) {
        Contrat k = registre.contrats.get(id);
        if (k == null || !p.hasPermission("kscontrats.admin")) {
            retour.accept(p);
            return;
        }
        Consumer<Player> ici = j -> adminFiche(j, id, retour);
        Contenant c = menu(p, 6, t("admin.fiche", "<dark_gray>Contrat <id>", "id", id), "contrats:admin:" + id);
        c.poser(4, Contenant.objet(Material.WRITTEN_BOOK, Component.text(k.nomsDe(k.partieA) + " / "
                + k.nomsDe(k.partieB), NamedTextColor.YELLOW), lignes(etat(k), t("admin.lire", "<gray>Clic : le contrat "
                + "en vigueur (ou proposé)"))), j -> lire(j, k, texte(k, k.termesVus(), k.etat == Etat.ACTIF), ici));
        c.poser(20, Contenant.objet(Material.BOOKSHELF, t("fiche.historique", "<yellow>Versions et historique"),
                gris("Versions, livraisons, signalements, décisions.")), j -> lire(j, k, historique(k), ici));
        if (k.etat == Etat.LITIGE) {
            c.poser(24, brillant(Contenant.objet(Material.ANVIL, t("admin.trancher", "<light_purple>Trancher"),
                    gris("Répartir les garanties en séquestre (" + Textes.points(k.totalSequestre()) + ") entre les "
                            + "joueurs, puis clore."))), j -> {
                repartitions.put(j.getUniqueId(), new LinkedHashMap<>());
                tousRendre(k, repartitions.get(j.getUniqueId()));
                repartition(j, id, ici);
            });
        }
        sortie(c, retour);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private static void tousRendre(Contrat k, Map<UUID, Long> r) {
        r.clear();
        for (UUID u : k.participants()) {
            r.put(u, k.sequestre.getOrDefault(u, 0L));
        }
    }

    private static void toutA(Contrat k, Map<UUID, Long> r, boolean partieA) {
        r.clear();
        k.participants().forEach(u -> r.put(u, 0L));
        List<UUID> partie = partieA ? k.partieA : k.partieB;
        long total = k.totalSequestre();
        for (int i = 0; i < partie.size(); i++) {
            r.put(partie.get(i), total / partie.size() + (i < total % partie.size() ? 1 : 0));
        }
    }

    void repartition(Player p, String id, Consumer<Player> retour) {
        Contrat k = registre.contrats.get(id);
        Map<UUID, Long> r = repartitions.get(p.getUniqueId());
        if (k == null || r == null || k.etat != Etat.LITIGE || !p.hasPermission("kscontrats.admin")) {
            retour.accept(p);
            return;
        }
        Consumer<Player> ici = j -> repartition(j, id, retour);
        Contenant c = menu(p, 6, t("repartition.titre", "<dark_gray>Répartir les garanties"), "contrats:repartition");
        List<UUID> tous = k.participants();
        for (int i = 0; i < tous.size() && i < 27; i++) {
            UUID u = tous.get(i);
            long recoit = r.getOrDefault(u, 0L);
            c.poser(i, Contenant.objet(teteDe(u, k.nom(u)), Component.text(k.nom(u) + " : " + Textes.points(recoit),
                    NamedTextColor.YELLOW), lignes(t("repartition.partie", "<gray>Partie <partie>", "partie",
                    k.dansA(u) ? "A" : "B"), t("repartition.verse", "<gray>Garantie versée : <points>", "points",
                    Textes.points(k.sequestre.getOrDefault(u, 0L))), t("repartition.clic", "<gray>Clic : montant libre"))),
                    j -> saisirNombre(j, Component.text(k.nom(u), NamedTextColor.YELLOW), t("repartition.saisie",
                            "<white>Points que ce joueur reçoit."), recoit, k.totalSequestre(), n -> {
                        r.put(u, n);
                        repartition(j, id, retour);
                    }, ici));
        }
        long total = r.values().stream().mapToLong(Long::longValue).sum();
        c.poser(37, Contenant.objet(Material.HOPPER, t("repartition.rendre", "<yellow>Tout rendre"),
                gris("Chacun récupère sa garantie.")), j -> {
            tousRendre(k, r);
            repartition(j, id, retour);
        });
        c.poser(38, Contenant.objet(Material.ORANGE_BANNER, t("repartition.a", "<yellow>Tout à la partie A"),
                gris(k.nomsDe(k.partieA))), j -> {
            toutA(k, r, true);
            repartition(j, id, retour);
        });
        c.poser(39, Contenant.objet(Material.BLUE_BANNER, t("repartition.b", "<yellow>Tout à la partie B"),
                gris(k.nomsDe(k.partieB))), j -> {
            toutA(k, r, false);
            repartition(j, id, retour);
        });
        boolean juste = total == k.totalSequestre();
        c.poser(41, Contenant.objet(juste ? Material.EMERALD : Material.REDSTONE, t("repartition.total",
                "<white>Total : <total> / <sequestre>", "total", Textes.points(total), "sequestre",
                Textes.points(k.totalSequestre())), gris(juste ? "Le total tombe juste." : "Le total doit tomber juste "
                + "pour clore.")), null);
        c.poser(43, brillant(Contenant.objet(Material.ANVIL, t("repartition.clore", "<light_purple><bold>Trancher et "
                + "clore"), gris("Points versés, décision gardée dans l'historique, joueurs prévenus."))),
                j -> confirmer(j, t("confirmer.trancher", "<dark_gray>Trancher ce litige ?"), List.of(), j2 ->
                        apres(j2, registre.trancher(k, new LinkedHashMap<>(r), j2.getName()), t("fait.tranche",
                                "<green>Litige tranché."), () -> {
                            repartitions.remove(j2.getUniqueId());
                            admin(j2, 0, null);
                        }), ici));
        sortie(c, retour);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ écoute

    /** Clic sur un objet de son inventaire pendant qu'une fiche (logo) ou un bien (objet) est ouvert. */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)
                || !(event.getView().getTopInventory().getHolder() instanceof Contenant)
                || event.getClickedInventory() == null || event.getClickedInventory() == event.getView().getTopInventory()) {
            return;
        }
        Map.Entry<Object, Consumer<ItemStack>> attente = clicsInventaire.get(p.getUniqueId());
        ItemStack objet = event.getCurrentItem();
        if (attente == null || objet == null || objet.getType().isAir()
                || Contenant.ouvert(p, attente.getKey()) == null) {
            return;
        }
        event.setCancelled(true);
        attente.getValue().accept(objet.clone());
        registre.sauverSiBesoin();
        lang.saveIfNeeded();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clicsInventaire.remove(event.getPlayer().getUniqueId());
        repartitions.remove(event.getPlayer().getUniqueId());
    }
}
