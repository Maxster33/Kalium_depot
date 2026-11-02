package fr.kalium.contrats;

import fr.kalium.contrats.Contrat.Ligne;
import fr.kalium.contrats.Demandes.Brouillon;
import fr.kalium.contrats.Demandes.Demande;
import fr.kalium.contrats.Demandes.Etat;
import fr.kalium.contrats.api.DemandesPubliques;
import fr.kalium.economy.KSEconomy;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.1.0 : menus des demandes publiques (bouton « Demandes publiques » de /contrat, choix de LeKiwi06 du 02/11/2026) :
 * liste des demandes ouvertes, « Mes demandes » (annuler), création (objets demandés, récompense, précision,
 * expiration facultative). La livraison ne se fait que depuis une boîte aux lettres (KS_Courrier, service
 * {@link DemandesPubliques}).
 */
final class MenusDemandes implements Listener, DemandesPubliques {

    /** Expirations proposées, en jours (0 : aucune). */
    private static final int[] JOURS = {0, 1, 3, 7, 14, 30};

    private final KSContrats plugin;
    private final Lang lang;
    private final Gui gui;
    private final Demandes demandes;
    private final Map<UUID, Brouillon> brouillons = new HashMap<>();
    /** Où revient « Retour » depuis l'écriture d'une demande (la liste d'où le joueur vient). */
    private final Map<UUID, Consumer<Player>> origines = new HashMap<>();
    /** Écran ouvert qui réagit à un clic sur un objet de l'inventaire du joueur. */
    private final Map<UUID, Map.Entry<Object, Consumer<ItemStack>>> clicsInventaire = new HashMap<>();

    MenusDemandes(KSContrats plugin, Demandes demandes) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.demandes = demandes;
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private static List<Component> gris(String texte) {
        return Contenant.lignes(texte, NamedTextColor.GRAY);
    }

    private Contenant menu(Player p, int rangees, Component titre, Object marque) {
        Contenant c = Contenant.pour(p, rangees, titre, marque);
        c.barre();
        clicsInventaire.remove(p.getUniqueId());
        return c;
    }

    private void sortie(Contenant c, Consumer<Player> retour) {
        c.poser(c.bas(5), Contenant.objet(Material.OAK_DOOR, t("contenant.retour", "<yellow>Retour"), List.of()),
                retour == null ? Contenant::fermer : retour);
    }

    private void pages(Contenant c, int page, int pages, Consumer<Integer> aller) {
        if (page > 0) {
            c.poser(c.bas(0), Contenant.objet(Material.ARROW, t("contenant.precedent", "<yellow>Page précédente"),
                    List.of(t("contenant.page", "<gray>Page <n> sur <total>", "n", page, "total", pages))),
                    j -> aller.accept(page - 1));
        }
        if (page < pages - 1) {
            c.poser(c.bas(8), Contenant.objet(Material.ARROW, t("contenant.suivant", "<yellow>Page suivante"),
                    List.of(t("contenant.page", "<gray>Page <n> sur <total>", "n", page + 2, "total", pages))),
                    j -> aller.accept(page + 1));
        }
    }

    private void aide(Contenant c, Component... textes) {
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, t("contenant.aide", "<yellow>Aide"), Menus.lignes(textes)),
                null);
    }

    private void message(Player p, Component texte, boolean refus) {
        if (p.getOpenInventory().getTopInventory().getHolder() instanceof Contenant) {
            Contenant.message(p, texte, refus);
        }
        p.sendMessage(texte);
    }

    private static ItemStack brillant(ItemStack objet) {
        ItemMeta meta = objet.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(true);
            objet.setItemMeta(meta);
        }
        return objet;
    }

    // ------------------------------------------------------------------ saisies (comme Menus)

    private void saisirTexte(Player p, Component titre, String initial, int max, boolean plusieursLignes,
                             Consumer<String> ok, Consumer<Player> retour) {
        Contenant.saisie(p, () -> {
            TextDialogInput.Builder b = DialogInput.text("texte", t("saisie.champ", "Texte"))
                    .initial(initial == null ? "" : initial).maxLength(max).width(300);
            if (plusieursLignes) {
                b.multiline(TextDialogInput.MultilineOptions.create(8, 100));
            }
            List<ActionButton> boutons = List.of(gui.form(t("saisie.valider", "<green>Valider"), null, (j, vue) -> {
                String texte = vue.getText("texte");
                ok.accept(texte == null ? "" : texte.trim());
            }));
            gui.open(p, titre, List.of(), List.of(b.build()), boutons,
                    gui.button(t("saisie.retour", "<gray>Retour"), null, retour::accept), 1);
            lang.saveIfNeeded();
        });
    }

    private void saisirNombre(Player p, Component titre, long initial, long max, Consumer<Long> ok,
                              Consumer<Player> retour) {
        saisirTexte(p, titre, String.valueOf(initial), 12, false, texte -> {
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

    private void confirmer(Player p, Component titre, List<Component> description, Consumer<Player> oui,
                           Consumer<Player> non) {
        Contenant c = menu(p, 3, titre, "contrats:demande-confirmer");
        c.poser(11, Contenant.objet(Material.LIME_CONCRETE, t("confirmer.oui", "<green><bold>Confirmer"), description),
                oui);
        c.poser(15, Contenant.objet(Material.RED_CONCRETE, t("confirmer.non", "<red>Annuler"), List.of()), non);
        c.ouvrir(p);
    }

    // ------------------------------------------------------------------ textes d'une demande

    private Component ligne(Ligne l) {
        Component c = Component.text("• " + Textes.nombre(l.quantite) + " ", NamedTextColor.WHITE)
                .append(Textes.nomObjet(l.modele).color(NamedTextColor.WHITE));
        return l.exact ? c.append(Component.text(" (cet objet précis)", NamedTextColor.GRAY)) : c;
    }

    private List<Component> description(Demande d) {
        List<Component> l = new ArrayList<>();
        l.add(t("publique.demande", "<yellow>Demande :"));
        d.lignes.forEach(x -> l.add(ligne(x)));
        if (!d.texte.isBlank()) {
            l.addAll(gris(d.texte));
        }
        l.add(t("publique.recompense", "<gold>Récompense :"));
        if (d.points > 0) {
            l.add(Component.text("• " + Textes.points(d.points), NamedTextColor.WHITE));
        }
        for (ItemStack o : d.objets) {
            l.add(Component.text("• " + Textes.nombre(o.getAmount()) + " ", NamedTextColor.WHITE)
                    .append(Textes.nomObjet(o).color(NamedTextColor.WHITE)));
        }
        l.add(t("publique.par", "<gray>Par <joueur>, le <date>", "joueur", d.nomAuteur, "date", Textes.date(d.creation)));
        if (d.etat == Etat.OUVERTE && d.expiration > 0) {
            l.add(t("publique.expire", "<gray>Expire dans <duree>", "duree",
                    Textes.duree(d.expiration - System.currentTimeMillis())));
        }
        return l;
    }

    private ItemStack icone(Demande d, Component nom, List<Component> lignes) {
        ItemStack modele = d.lignes.isEmpty() ? new ItemStack(Material.PAPER) : d.lignes.get(0).modele.clone();
        ItemStack o = Contenant.objet(modele, nom, lignes);
        o.setAmount(Math.max(1, Math.min(64, d.lignes.isEmpty() ? 1 : d.lignes.get(0).quantite)));
        return o;
    }

    // ------------------------------------------------------------------ liste

    @Override
    public void ouvrirDepuisBoite(Player joueur, Consumer<Player> retour) {
        liste(joueur, 0, retour, true);
    }

    /** Demandes ouvertes ; depuisBoite : bouton « Livrer » (le joueur est à sa boîte aux lettres). */
    void liste(Player p, int page, Consumer<Player> retour, boolean depuisBoite) {
        UUID compte = Registre.compte(p);
        origines.put(p.getUniqueId(), j -> liste(j, 0, retour, depuisBoite));
        List<Demande> liste = demandes.ouvertes(compte);
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, t("demandes.titre", "<dark_gray>Demandes publiques"), "contrats:demandes");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Demande d = liste.get(i);
            boolean mienne = d.auteur.equals(compte);
            List<Component> lignes = description(d);
            lignes.add(mienne ? t("demandes.mienne", "<aqua>Ta demande : annule-la depuis « Mes demandes ».")
                    : depuisBoite ? t("demandes.clic-livrer", "<green>Clic : livrer (tout en une fois).")
                    : t("demandes.livrer-boite", "<aqua>Livre-la depuis ta boîte aux lettres."));
            Consumer<Player> action = !mienne && depuisBoite ? j -> confirmerLivraison(j, d, retour) : null;
            c.poser(i - pg * Contenant.PAR_PAGE, icone(d, t("demandes.nom", "<gold>Demande n°<n> de <joueur>", "n",
                    d.id.substring(1), "joueur", d.nomAuteur), lignes), action);
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("demandes.vide", "<gray>Aucune demande ouverte"),
                    gris("Crée la tienne avec le livre et plume en bas.")), null);
        }
        pages(c, pg, pages, n -> liste(p, n, retour, depuisBoite));
        c.poser(c.bas(1), Contenant.objet(Material.WRITABLE_BOOK, t("demandes.creer", "<green>Créer une demande"),
                gris("Ton brouillon est gardé si tu fermes le menu.")), this::editeur);
        int miennes = demandes.ouvertesDe(compte);
        c.poser(c.bas(2), Contenant.objet(Material.CHEST, t("demandes.miennes", "<aqua>Mes demandes (<n>/<max>)", "n",
                miennes, "max", Demandes.OUVERTES_MAX), gris("Tes demandes ouvertes (annuler) et terminées.")),
                j -> mes(j, 0));
        aide(c, t("aide.demandes1", "<gray>Une demande publique peut être remplie par n'importe qui, sans "
                + "réservation : le premier qui livre tous les objets est payé."), t("aide.demandes2", "<gray>La "
                + "récompense est retenue dès la création et rendue si tu annules. On livre depuis sa boîte aux "
                + "lettres ; les objets arrivent dans la boîte du demandeur."));
        sortie(c, retour);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void confirmerLivraison(Player p, Demande d, Consumer<Player> retour) {
        List<Component> lignes = description(d);
        lignes.add(t("demandes.livrer-texte", "<white>Les objets sont pris dans ton inventaire et déposés dans la boîte "
                + "du demandeur ; la récompense est pour toi."));
        confirmer(p, t("demandes.livrer-titre", "<green><bold>Livrer la demande n°<n>", "n", d.id.substring(1)),
                lignes, j -> {
                    Component refus = demandes.livrer(j, d);
                    if (refus != null) {
                        message(j, refus, true);
                        return;
                    }
                    message(j, t("demandes.livree", "<green>Demande livrée : <recompense>.", "recompense",
                            recompense(d)), false);
                    Contenant.apres(() -> liste(j, 0, retour, true));
                }, j -> liste(j, 0, retour, true));
    }

    private String recompense(Demande d) {
        List<String> parts = new ArrayList<>();
        if (d.points > 0) {
            parts.add(Textes.points(d.points) + " reçus");
        }
        if (!d.objets.isEmpty()) {
            parts.add("objets dans ta boîte aux lettres");
        }
        return String.join(", ", parts);
    }

    // ------------------------------------------------------------------ mes demandes

    void mes(Player p, int page) {
        UUID compte = Registre.compte(p);
        List<Demande> liste = demandes.de(compte);
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, t("mes.titre", "<dark_gray>Mes demandes publiques"), "contrats:mes-demandes");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Demande d = liste.get(i);
            List<Component> lignes = description(d);
            lignes.add(switch (d.etat) {
                case OUVERTE -> t("mes.ouverte", "<green>Ouverte. Clic : annuler (récompense rendue).");
                case LIVREE -> t("mes.livree", "<aqua>Livrée par <joueur> le <date>.", "joueur", d.nomLivreur, "date",
                        Textes.date(d.fin));
                case ANNULEE -> t("mes.annulee", "<gray>Annulée le <date>.", "date", Textes.date(d.fin));
                case EXPIREE -> t("mes.expiree", "<gray>Expirée le <date>.", "date", Textes.date(d.fin));
            });
            ItemStack o = icone(d, t("demandes.nom", "<gold>Demande n°<n> de <joueur>", "n", d.id.substring(1),
                    "joueur", d.nomAuteur), lignes);
            c.poser(i - pg * Contenant.PAR_PAGE, d.etat == Etat.OUVERTE ? brillant(o) : o,
                    d.etat == Etat.OUVERTE ? j -> confirmerAnnulation(j, d, pg) : null);
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("mes.vide", "<gray>Aucune demande"), List.of()), null);
        }
        pages(c, pg, pages, n -> mes(p, n));
        c.poser(c.bas(1), Contenant.objet(Material.WRITABLE_BOOK, t("demandes.creer", "<green>Créer une demande"),
                gris("Ton brouillon est gardé si tu fermes le menu.")), this::editeur);
        sortie(c, retourListe(p));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private Consumer<Player> retourListe(Player p) {
        return origines.getOrDefault(p.getUniqueId(), j -> liste(j, 0, j2 -> plugin.menus().accueil(j2, 0), false));
    }

    private void confirmerAnnulation(Player p, Demande d, int page) {
        confirmer(p, t("mes.annuler-titre", "<red><bold>Annuler la demande n°<n>", "n", d.id.substring(1)),
                gris("La récompense t'est rendue (points sur ton solde, objets dans ta boîte aux lettres)."), j -> {
                    Component refus = demandes.annuler(j, d);
                    if (refus != null) {
                        message(j, refus, true);
                        return;
                    }
                    Contenant.apres(() -> mes(j, page));
                }, j -> mes(j, page));
    }

    // ------------------------------------------------------------------ écriture d'une demande

    private Brouillon brouillon(Player p) {
        return brouillons.computeIfAbsent(Registre.compte(p), u -> new Brouillon());
    }

    void editeur(Player p) {
        Brouillon b = brouillon(p);
        Contenant c = menu(p, 4, t("editeur.titre", "<dark_gray>Nouvelle demande publique"), "contrats:demande-editeur");
        List<Component> demande = new ArrayList<>();
        b.lignes.forEach(l -> demande.add(ligne(l)));
        if (demande.isEmpty()) {
            demande.addAll(gris("Aucun objet. Clic : choisir les objets demandés (5 au plus)."));
        }
        c.poser(10, Contenant.objet(b.lignes.isEmpty() ? new ItemStack(Material.HOPPER) : b.lignes.get(0).modele.clone(),
                t("editeur.objets", "<yellow>1. Objets demandés"), demande), this::objets);
        List<Component> recompense = new ArrayList<>();
        if (b.points > 0) {
            recompense.add(Component.text("• " + Textes.points(b.points), NamedTextColor.WHITE));
        }
        for (ItemStack o : b.objets) {
            recompense.add(Component.text("• " + Textes.nombre(o.getAmount()) + " ", NamedTextColor.WHITE)
                    .append(Textes.nomObjet(o).color(NamedTextColor.WHITE)));
        }
        if (recompense.isEmpty()) {
            recompense.addAll(gris("Aucune. Clic : des points et / ou des objets, retenus dès la publication."));
        }
        c.poser(12, Contenant.objet(Material.GOLD_INGOT, t("editeur.recompense", "<yellow>2. Récompense"), recompense),
                this::recompense);
        c.poser(14, Contenant.objet(Material.PAPER, t("editeur.precision", "<yellow>3. Précision <gray>(facultatif)"),
                gris(b.texte.isBlank() ? "Aucune." : b.texte)),
                j -> saisirTexte(j, t("editeur.precision-titre", "<yellow><bold>Précision"), b.texte, 200, true,
                        texte -> {
                            b.texte = texte;
                            editeur(j);
                        }, this::editeur));
        c.poser(16, Contenant.objet(Material.CLOCK, t("editeur.expiration", "<yellow>4. Expiration : <valeur>",
                "valeur", b.jours == 0 ? "aucune" : b.jours + " j"), gris("Clic : changer (aucune, 1, 3, 7, 14 ou 30 "
                + "jours). À l'expiration, la récompense est rendue.")), j -> {
                    int i = 0;
                    while (i < JOURS.length && JOURS[i] != b.jours) {
                        i++;
                    }
                    b.jours = JOURS[(i + 1) % JOURS.length];
                    editeur(j);
                });
        c.poser(c.bas(4), Contenant.objet(Material.LIME_CONCRETE, t("editeur.publier", "<green><bold>Publier"),
                gris("La récompense est retenue tout de suite.")), this::publier);
        aide(c, t("aide.editeur", "<gray><n> demandes ouvertes au plus par joueur. N'importe qui peut la livrer, en une "
                + "fois, depuis sa boîte aux lettres.", "n", Demandes.OUVERTES_MAX));
        sortie(c, retourListe(p));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void publier(Player p) {
        Brouillon b = brouillon(p);
        List<Component> lignes = new ArrayList<>();
        lignes.add(t("publique.demande", "<yellow>Demande :"));
        b.lignes.forEach(l -> lignes.add(ligne(l)));
        lignes.addAll(gris("La récompense (points retirés de ton solde, objets pris dans ton inventaire) est retenue "
                + "jusqu'à la livraison, et rendue si tu annules."));
        confirmer(p, t("editeur.publier-titre", "<green><bold>Publier la demande"), lignes, j -> {
            Component refus = demandes.publier(j, b);
            if (refus != null) {
                message(j, refus, true);
                return;
            }
            brouillons.remove(Registre.compte(j));
            message(j, t("editeur.publiee", "<green>Demande publiée."), false);
            Contenant.apres(() -> mes(j, 0));
        }, this::editeur);
    }

    /** Objets demandés : clic sur un objet de l'inventaire (cet objet précis) ou nom écrit (n'importe lequel). */
    private void objets(Player p) {
        Brouillon b = brouillon(p);
        Object marque = "contrats:demande-objets";
        Contenant c = menu(p, 4, t("editeur.objets-titre", "<dark_gray>Objets demandés"), marque);
        for (int i = 0; i < Demandes.LIGNES_MAX; i++) {
            if (i < b.lignes.size()) {
                Ligne l = b.lignes.get(i);
                int index = i;
                List<Component> d = new ArrayList<>(gris(l.exact ? "Choisi dans l'inventaire : il faudra livrer cet "
                        + "objet précis (usure non comptée)." : "Nom écrit : n'importe quel objet de ce type compte."));
                d.addAll(gris("Clic : changer la quantité (0 : retirer la ligne)."));
                ItemStack o = Contenant.objet(l.modele, Component.text(Textes.nombre(l.quantite) + " ",
                        NamedTextColor.YELLOW).append(Textes.nomObjet(l.modele)), d);
                o.setAmount(Math.max(1, Math.min(64, l.quantite)));
                c.poser(11 + i, o, j -> quantite(j, index));
            } else {
                c.poser(11 + i, Contenant.objet(Material.LIGHT_GRAY_STAINED_GLASS_PANE, t("bien.libre",
                        "<gray>Case libre"), gris("Clique sur un objet de ton inventaire, ou écris son nom.")), null);
            }
        }
        c.poser(c.bas(2), Contenant.objet(Material.NAME_TAG, t("bien.nom", "<yellow>Écrire le nom d'un objet"),
                gris("En français (« bloc de fer ») ; n'importe quel objet de ce type comptera.")),
                j -> saisirTexte(j, t("bien.nom-titre", "<yellow><bold>Nom de l'objet"), "", 40, false,
                        texte -> resultats(j, texte), this::objets));
        aide(c, t("aide.demande-objets", "<gray>Clique sur un objet de ton inventaire : il faudra livrer exactement "
                + "cet objet (mêmes enchantements, usure non comptée)."));
        c.poser(c.bas(4), Contenant.objet(Material.LIME_CONCRETE, t("bien.fini", "<green>Terminé"), List.of()),
                this::editeur);
        sortie(c, this::editeur);
        clicsInventaire.put(p.getUniqueId(), Map.entry(marque, objet -> {
            if (b.lignes.size() >= Demandes.LIGNES_MAX) {
                message(p, t("refus.cinq", "<red>5 objets différents au plus."), true);
                return;
            }
            for (Ligne l : b.lignes) {
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
            b.lignes.add(l);
            Contenant.apres(() -> objets(p));
        }));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void quantite(Player p, int index) {
        Brouillon b = brouillon(p);
        if (index >= b.lignes.size()) {
            objets(p);
            return;
        }
        Ligne l = b.lignes.get(index);
        saisirNombre(p, t("bien.quantite-titre", "<yellow><bold>Quantité"), l.quantite, 1_000_000L, n -> {
            if (n == 0) {
                b.lignes.remove(l);
            } else {
                l.quantite = (int) (long) n;
            }
            objets(p);
        }, this::objets);
    }

    private void resultats(Player p, String texte) {
        List<Material> trouves = texte.isBlank() ? List.of() : KSEconomy.chercherObjets(texte);
        if (trouves.isEmpty()) {
            p.sendMessage(t("refus.aucun-objet", "<red>Aucun objet ne correspond à « <texte> ».", "texte", texte));
            objets(p);
            return;
        }
        Contenant c = menu(p, 6, t("resultats.titre", "<dark_gray>Objets trouvés"), "contrats:demande-resultats");
        for (int i = 0; i < trouves.size() && i < Contenant.PAR_PAGE; i++) {
            Material m = trouves.get(i);
            ItemStack modele = new ItemStack(m);
            c.poser(i, Contenant.objet(modele, Textes.nomObjet(modele).color(NamedTextColor.YELLOW),
                    gris("Clic : choisir, puis la quantité.")), j -> {
                Brouillon b = brouillon(j);
                for (Ligne l : b.lignes) {
                    if (!l.exact && l.modele.getType() == m) {
                        message(j, t("refus.deja-ligne", "<red>Cet objet est déjà dans la liste : clique dessus pour "
                                + "changer sa quantité."), true);
                        return;
                    }
                }
                if (b.lignes.size() >= Demandes.LIGNES_MAX) {
                    message(j, t("refus.cinq", "<red>5 objets différents au plus."), true);
                    return;
                }
                Ligne l = new Ligne();
                l.modele = modele;
                l.exact = false;
                l.quantite = 1;
                b.lignes.add(l);
                quantite(j, b.lignes.size() - 1);
            });
        }
        sortie(c, this::objets);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    /** Récompense : points (saisie) et objets (clic sur une pile de l'inventaire, retirée à la publication). */
    private void recompense(Player p) {
        Brouillon b = brouillon(p);
        Object marque = "contrats:demande-recompense";
        Contenant c = menu(p, 4, t("editeur.recompense-titre", "<dark_gray>Récompense"), marque);
        c.poser(10, Contenant.objet(Material.GOLD_INGOT, b.points > 0 ? t("bien.points", "<yellow><points>", "points",
                Textes.points(b.points)) : t("bien.points-aucun", "<yellow>Ajouter des points"),
                gris("Clic : changer le montant (0 : aucun).")), j -> saisirNombre(j, t("editeur.points-titre",
                "<yellow><bold>Points de récompense"), b.points, 1_000_000_000L, n -> {
                    b.points = n;
                    recompense(j);
                }, this::recompense));
        for (int i = 0; i < Demandes.OBJETS_MAX; i++) {
            if (i < b.objets.size()) {
                ItemStack o = b.objets.get(i);
                int index = i;
                ItemStack icone = Contenant.objet(o.clone(), Component.text(Textes.nombre(o.getAmount()) + " ",
                        NamedTextColor.YELLOW).append(Textes.nomObjet(o)), gris("Clic : retirer de la récompense."));
                c.poser(12 + i, icone, j -> {
                    b.objets.remove(index);
                    recompense(j);
                });
            } else {
                c.poser(12 + i, Contenant.objet(Material.LIGHT_GRAY_STAINED_GLASS_PANE, t("bien.libre",
                        "<gray>Case libre"), gris("Clique sur une pile de ton inventaire.")), null);
            }
        }
        aide(c, t("aide.recompense", "<gray>Clique sur une pile de ton inventaire pour l'ajouter. Elle reste dans ton "
                + "inventaire jusqu'à la publication, puis elle est gardée par le serveur."));
        c.poser(c.bas(4), Contenant.objet(Material.LIME_CONCRETE, t("bien.fini", "<green>Terminé"), List.of()),
                this::editeur);
        sortie(c, this::editeur);
        clicsInventaire.put(p.getUniqueId(), Map.entry(marque, objet -> {
            for (ItemStack o : b.objets) {
                if (o.isSimilar(objet)) {
                    // même objet : une seule ligne, quantités ajoutées (vérifiées ensemble à la publication)
                    o.setAmount(o.getAmount() + objet.getAmount());
                    Contenant.apres(() -> recompense(p));
                    return;
                }
            }
            if (b.objets.size() >= Demandes.OBJETS_MAX) {
                message(p, t("refus.cinq-objets", "<red>5 objets différents au plus."), true);
                return;
            }
            b.objets.add(objet.clone());
            Contenant.apres(() -> recompense(p));
        }));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ écoute

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)
                || !(event.getView().getTopInventory().getHolder() instanceof Contenant)
                || event.getClickedInventory() == null
                || event.getClickedInventory() == event.getView().getTopInventory()) {
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
        lang.saveIfNeeded();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clicsInventaire.remove(event.getPlayer().getUniqueId());
        origines.remove(event.getPlayer().getUniqueId());
    }
}
