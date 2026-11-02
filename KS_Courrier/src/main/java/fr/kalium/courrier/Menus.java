package fr.kalium.courrier;

import fr.kalium.contrats.api.DemandesPubliques;
import fr.kalium.courrier.Donnees.Envoi;
import fr.kalium.courrier.Donnees.Type;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Menus en coffre de la boîte aux lettres : courrier reçu, envoyer une lettre ou un colis (choix du destinataire, frais),
 * demandes publiques (KS_Contrats), historique des envois et des réceptions (choix de LeKiwi06 du 02/11/2026). Le colis
 * se remplit dans un coffre à 27 cases libres ({@link Emballage}).
 */
final class Menus implements Listener {

    private final KSCourrier plugin;
    private final Lang lang;
    private final Gui gui;
    private final Donnees donnees;
    private final Poste poste;
    private final Objets objets;
    /** Écran ouvert qui réagit à un clic sur un objet de l'inventaire du joueur (livre de la lettre). */
    private final Map<UUID, Map.Entry<Object, Consumer<ItemStack>>> clicsInventaire = new HashMap<>();

    Menus(KSCourrier plugin, Donnees donnees, Poste poste, Objets objets) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.donnees = donnees;
        this.poste = poste;
        this.objets = objets;
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
        if (retour == null) {
            c.poser(c.bas(5), Contenant.objet(Material.BARRIER, t("contenant.fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
        } else {
            c.poser(c.bas(5), Contenant.objet(Material.OAK_DOOR, t("contenant.retour", "<yellow>Retour"), List.of()),
                    retour);
        }
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
        List<Component> lignes = new ArrayList<>();
        for (Component texte : textes) {
            lignes.addAll(Contenant.lignes(texte));
        }
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, t("contenant.aide", "<yellow>Aide"), lignes), null);
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

    private static ItemStack tete(UUID joueur) {
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

    // ------------------------------------------------------------------ boîte

    void boite(Player p) {
        UUID compte = Poste.compte(p);
        Contenant c = menu(p, 4, t("boite.titre", "<dark_gray>Boîte aux lettres"), "courrier:boite");
        int n = donnees.recus(compte).size();
        ItemStack recus = Contenant.objet(new ItemStack(Material.CHEST, Math.max(1, Math.min(64, n))),
                t("boite.recus", "<gold>Courrier reçu (<n>)", "n", n), gris("Lettres et colis qui t'attendent. "
                        + "Clic sur un envoi : il va dans ton inventaire, clic droit dessus pour l'ouvrir."));
        c.poser(10, n > 0 ? brillant(recus) : recus, j -> recus(j, 0));
        c.poser(12, Contenant.objet(objets.enveloppe(), t("boite.lettre", "<yellow>Envoyer une lettre"),
                gris("Un livre écrit (signé) et une enveloppe de ton inventaire.")),
                j -> destinataire(j, Type.LETTRE, 0));
        c.poser(14, Contenant.objet(objets.colis(), t("boite.colis", "<yellow>Envoyer un colis"),
                gris("Jusqu'à 27 piles d'objets, avec un colis vide de ton inventaire.")),
                j -> destinataire(j, Type.COLIS, 0));
        DemandesPubliques demandes = Bukkit.getPluginManager().isPluginEnabled("KS_Contrats")
                ? Bukkit.getServicesManager().load(DemandesPubliques.class) : null;
        if (demandes != null) {
            c.poser(16, Contenant.objet(Material.OAK_HANGING_SIGN, t("boite.demandes", "<gold>Demandes publiques"),
                    gris("Livre une demande d'un autre joueur et reçois sa récompense.")),
                    j -> demandes.ouvrirDepuisBoite(j, this::boite));
        }
        c.poser(c.bas(2), Contenant.objet(Material.WRITABLE_BOOK, t("boite.historique", "<aqua>Historique"),
                gris("Tes envois et tes réceptions.")), j -> historique(j, compte, p.getName(), 0, this::boite));
        aide(c, t("aide.boite1", "<gray>Le courrier arrive dans ta boîte même si tu n'es pas là. Pour envoyer, le "
                + "destinataire doit avoir sa boîte aux lettres."), t("aide.boite2", "<gray>Frais : 1 point par "
                + "tranche de 1 000 blocs entre les deux boîtes (gratuit sous 1 000 blocs)."));
        sortie(c, null);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    void recus(Player p, int page) {
        UUID compte = Poste.compte(p);
        List<Envoi> liste = donnees.recus(compte);
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, t("recus.titre", "<dark_gray>Courrier reçu"), "courrier:recus");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Envoi e = liste.get(i);
            ItemStack icone = objets.scelle(e);
            ItemMeta meta = icone.getItemMeta();
            List<Component> lore = new ArrayList<>(meta.lore());
            lore.set(lore.size() - 1, t("recus.prendre", "<yellow>Clic : le prendre"));
            meta.lore(lore);
            icone.setItemMeta(meta);
            c.poser(i - pg * Contenant.PAR_PAGE, icone, j -> {
                Component refus = poste.retirer(j, e);
                if (refus != null) {
                    message(j, refus, true);
                    return;
                }
                Contenant.apres(() -> recus(j, pg));
            });
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("recus.vide", "<gray>Aucun courrier"), List.of()), null);
        }
        pages(c, pg, pages, n -> recus(p, n));
        sortie(c, this::boite);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ destinataire

    /** Joueurs en ligne qui ont une boîte (même monde : survie ou test), ou un pseudo écrit. */
    void destinataire(Player p, Type type, int page) {
        UUID compte = Poste.compte(p);
        if (donnees.boite(compte) == null) {
            message(p, t("refus.sans-boite", "<red>Pose d'abord ta boîte aux lettres dans un de tes claims."), true);
            return;
        }
        List<Player> liste = new ArrayList<>();
        for (Player q : Bukkit.getOnlinePlayers()) {
            UUID cq = Poste.compte(q);
            if (!cq.equals(compte) && CompteTest.enTest(q) == CompteTest.enTest(p) && donnees.boite(cq) != null) {
                liste.add(q);
            }
        }
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        Contenant c = menu(p, 6, type == Type.LETTRE ? t("dest.titre-lettre", "<dark_gray>Lettre pour...")
                : t("dest.titre-colis", "<dark_gray>Colis pour..."), "courrier:destinataire");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Player q = liste.get(i);
            UUID cq = Poste.compte(q);
            String nom = q.getName();
            c.poser(i - pg * Contenant.PAR_PAGE, Contenant.objet(tete(q.getUniqueId()), Component.text(nom,
                    NamedTextColor.YELLOW), List.of(t("dest.frais", "<gray>Frais : <frais>", "frais",
                    Poste.points(poste.fraisVers(p, cq))))), j -> suite(j, type, cq, nom));
        }
        pages(c, pg, pages, n -> destinataire(p, type, n));
        c.poser(c.bas(1), Contenant.objet(Material.NAME_TAG, t("dest.pseudo", "<yellow>Écrire un pseudo"),
                gris("Un joueur hors ligne qui a sa boîte aux lettres.")), j -> saisirPseudo(j, type));
        aide(c, t("aide.dest", "<gray>Seuls les joueurs qui ont leur boîte aux lettres peuvent recevoir du courrier."));
        sortie(c, this::boite);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void saisirPseudo(Player p, Type type) {
        Contenant.saisie(p, () -> {
            DialogInput champ = DialogInput.text("pseudo", t("dest.champ", "Pseudo")).initial("").maxLength(32)
                    .width(300).build();
            List<ActionButton> boutons = List.of(gui.form(t("saisie.valider", "<green>Valider"), null, (j, vue) -> {
                String nom = vue.getText("pseudo");
                OfflinePlayer o = nom == null || nom.isBlank() ? null : Bukkit.getOfflinePlayerIfCached(nom.trim());
                if (o == null) {
                    j.sendMessage(t("refus.pseudo-inconnu", "<red>Joueur inconnu : « <pseudo> ».", "pseudo",
                            nom == null ? "" : nom.trim()));
                    destinataire(j, type, 0);
                    return;
                }
                UUID cible = CompteTest.enTest(j) ? CompteTest.compteDeTest(o.getUniqueId()) : o.getUniqueId();
                suite(j, type, cible, o.getName() == null ? nom.trim() : o.getName());
            }));
            gui.open(p, t("dest.pseudo-titre", "<yellow><bold>Destinataire"), List.of(), List.of(champ), boutons,
                    gui.button(t("saisie.retour", "<gray>Retour"), null, j -> destinataire(j, type, 0)), 1);
            lang.saveIfNeeded();
        });
    }

    private void suite(Player p, Type type, UUID destinataire, String nom) {
        Component refus = poste.refusEnvoi(p, destinataire);
        if (refus != null) {
            message(p, refus, true);
            Contenant.apres(() -> destinataire(p, type, 0));
            return;
        }
        Contenant.apres(() -> {
            if (type == Type.LETTRE) {
                lettre(p, destinataire, nom);
            } else {
                Emballage.ouvrir(this, p, destinataire, nom);
            }
        });
    }

    // ------------------------------------------------------------------ lettre

    private void lettre(Player p, UUID destinataire, String nom) {
        Object marque = "courrier:lettre";
        Contenant c = menu(p, 3, t("lettre.titre", "<dark_gray>Lettre pour <joueur>", "joueur", nom), marque);
        c.poser(4, Contenant.objet(Material.WRITTEN_BOOK, t("lettre.choisir", "<yellow>Clique sur un livre écrit de ton "
                + "inventaire"), gris("Écris ta lettre dans un livre et plume, signe-le, puis clique dessus. Il faut "
                + "aussi une enveloppe (2 papiers à l'établi). Frais : " + Poste.points(poste.fraisVers(p,
                destinataire)) + ".")), null);
        sortie(c, j -> destinataire(j, Type.LETTRE, 0));
        clicsInventaire.put(p.getUniqueId(), Map.entry(marque, livre -> {
            if (livre.getType() != Material.WRITTEN_BOOK) {
                message(p, t("refus.pas-livre", "<red>Clique sur un livre écrit (signé)."), true);
                return;
            }
            Contenant.apres(() -> confirmerLettre(p, destinataire, nom, livre));
        }));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    private void confirmerLettre(Player p, UUID destinataire, String nom, ItemStack livre) {
        Contenant c = menu(p, 3, t("lettre.confirmer", "<dark_gray>Envoyer à <joueur> ?", "joueur", nom),
                "courrier:lettre-confirmer");
        c.poser(11, Contenant.objet(Material.LIME_CONCRETE, t("confirmer.envoyer", "<green><bold>Envoyer"),
                List.of(t("confirmer.frais", "<gray>Frais : <frais>", "frais",
                        Poste.points(poste.fraisVers(p, destinataire))))), j -> {
            Component refus = poste.envoyerLettre(j, destinataire, nom, livre);
            if (refus != null) {
                message(j, refus, true);
                return;
            }
            message(j, t("lettre.envoyee", "<green>Lettre envoyée à <joueur>.", "joueur", nom), false);
            Contenant.apres(() -> boite(j));
        });
        c.poser(13, Contenant.objet(livre.clone(), t("lettre.livre", "<yellow>Ta lettre"), List.of()), null);
        c.poser(15, Contenant.objet(Material.RED_CONCRETE, t("confirmer.non", "<red>Annuler"), List.of()),
                j -> lettre(j, destinataire, nom));
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ colis (appelé par Emballage)

    Component titreColis(String nom) {
        return t("colis.titre", "<dark_gray>Colis pour <joueur>", "joueur", nom);
    }

    ItemStack boutonEnvoyer(Player p, UUID destinataire) {
        return Contenant.objet(Material.LIME_CONCRETE, t("colis.envoyer", "<green><bold>Envoyer"), List.of(
                t("confirmer.frais", "<gray>Frais : <frais>", "frais", Poste.points(poste.fraisVers(p, destinataire))),
                t("colis.envoyer-texte", "<gray>Un colis vide est pris dans ton inventaire.")));
    }

    ItemStack boutonAnnuler() {
        return Contenant.objet(Material.RED_CONCRETE, t("colis.annuler", "<red>Annuler"),
                gris("Les objets reviennent dans ton inventaire."));
    }

    ItemStack aideColis() {
        return Contenant.objet(Material.BOOK, t("contenant.aide", "<yellow>Aide"), gris("Pose dans les cases du "
                + "haut les objets à envoyer (27 piles au plus). Fermer ou annuler te les rend."));
    }

    void envoyerColis(Player p, Emballage e) {
        List<ItemStack> contenu = e.contenu();
        Component refus = poste.envoyerColis(p, e.destinataire, e.nom, contenu);
        if (refus != null) {
            message(p, refus, true);
            return;
        }
        e.vider();
        message(p, t("colis.envoye", "<green>Colis envoyé à <joueur>.", "joueur", e.nom), false);
        Bukkit.getScheduler().runTask(plugin, () -> boite(p));
    }

    // ------------------------------------------------------------------ historique

    /** Envois et réceptions d'un compte ; un admin (kscourrier.admin) peut lire les lettres (modération). */
    void historique(Player p, UUID compte, String nom, int page, Consumer<Player> retour) {
        List<Envoi> liste = donnees.historique(compte);
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int pg = Math.min(page, pages - 1);
        boolean admin = p.hasPermission("kscourrier.admin");
        Contenant c = menu(p, 6, t("historique.titre", "<dark_gray>Courrier de <joueur>", "joueur", nom),
                "courrier:historique");
        for (int i = pg * Contenant.PAR_PAGE; i < liste.size() && i < (pg + 1) * Contenant.PAR_PAGE; i++) {
            Envoi e = liste.get(i);
            boolean envoye = compte.equals(e.expediteur);
            List<Component> lore = new ArrayList<>();
            lore.add(t("historique.date", "<gray>Le <date>", "date", Objets.date(e.date)));
            if (!e.titre.isEmpty()) {
                lore.add(Component.text(e.titre, NamedTextColor.GRAY));
            }
            if (envoye) {
                lore.add(t("historique.frais", "<gray>Frais : <frais> (<distance> blocs)", "frais",
                        Poste.points(e.frais), "distance", e.distance));
            }
            for (int k = 0; k < e.resume.size() && k < 10; k++) {
                lore.add(MiniMessage.miniMessage().deserialize("<white>• " + e.resume.get(k)));
            }
            if (e.resume.size() > 10) {
                lore.add(t("historique.plus", "<gray>... et <n> autres piles", "n", e.resume.size() - 10));
            }
            lore.add(switch (e.etat) {
                case BOITE -> t("historique.boite", "<yellow>Dans la boîte de <joueur>", "joueur", e.nomDestinataire);
                case SORTI -> t("historique.sorti", "<yellow>Pris dans la boîte le <date>", "date", Objets.date(e.retrait));
                case OUVERT -> t("historique.ouvert", "<green>Ouvert le <date> par <joueur>", "date",
                        Objets.date(e.ouverture), "joueur", e.ouvreur);
            });
            boolean lisible = admin && e.type == Type.LETTRE && !e.contenu.isEmpty();
            if (lisible) {
                lore.add(t("historique.lire", "<aqua>Clic : lire (modération)"));
            }
            Component titre = envoye
                    ? t("historique.envoye", "<gold><type> envoyé<e> à <joueur>", "type", e.type == Type.LETTRE
                    ? "Lettre" : "Colis", "e", e.type == Type.LETTRE ? "e" : "", "joueur", e.nomDestinataire)
                    : t("historique.recu", "<aqua><type> reçu<e> de <joueur>", "type", e.type == Type.LETTRE
                    ? "Lettre" : "Colis", "e", e.type == Type.LETTRE ? "e" : "", "joueur", e.nomExpediteur);
            ItemStack icone = Contenant.objet(e.type == Type.LETTRE ? Material.PAPER : Material.BUNDLE, titre, lore);
            c.poser(i - pg * Contenant.PAR_PAGE, icone, lisible ? j -> {
                Contenant.fermer(j);
                Contenant.apres(() -> j.openBook(e.contenu.get(0).clone()));
            } : null);
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("historique.vide", "<gray>Aucun envoi ni réception"),
                    List.of()), null);
        }
        pages(c, pg, pages, n -> historique(p, compte, nom, n, retour));
        sortie(c, retour);
        c.ouvrir(p);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ écoute

    /** Clic sur un objet de son inventaire pendant que l'écran de la lettre est ouvert. */
    @EventHandler
    public void onClic(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof Emballage e) {
            e.clic(event);
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof Contenant)
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
    public void onGlisser(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Emballage e) {
            e.glisser(event);
        }
    }

    @EventHandler
    public void onFermer(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Emballage e && event.getPlayer() instanceof Player p) {
            e.rendre(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clicsInventaire.remove(event.getPlayer().getUniqueId());
    }

    /** Arrêt du plugin : les colis en cours de remplissage rendent leurs objets. */
    void fermerTout() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory haut = p.getOpenInventory().getTopInventory();
            if (haut.getHolder() instanceof Emballage e) {
                e.rendre(p);
                p.closeInventory();
            }
        }
    }
}
