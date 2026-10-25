package fr.kalium.rewardsgui;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.9.0 (LeKiwi06, 25/10/2026 : « un bouton spécial dans le /rewards dédié aux opérateurs pour donner des coffres de
 * récompenses aux joueurs [...] nom du joueur, nom du coffre, valeur du coffre [...] il faut aussi y inclure un
 * historique de quelle personne a donné quoi à qui ») : outils des opérateurs, en menus de type contenant (maquette
 * validée le 25/10/2026).
 *
 * - Bouton « Outils des opérateurs » dans la barre de /rewards (permission ksrewardsgui.staff, opérateurs par défaut) :
 *   « Donner un coffre », « Historique des dons », « Coffres de mort actifs » (KS_CoffreMort 1.1.0, /coffres tous).
 * - Donner un coffre : petite fenêtre (joueur, nom du coffre, valeur moyenne en émeraudes), puis confirmation. Le
 *   joueur reçoit dans /rewards un coffre « Non ouvert » d'origine « Staff », tiré avec les tables de fusion.yml
 *   (deposerCoffre, comme le parrainage). Le joueur doit s'être déjà connecté à ce serveur.
 * - Historique : dons.yml (date, opérateur, joueur, nom, valeur), le plus récent en premier ; chaque don est aussi
 *   noté dans recuperations.log (ligne « DON ») et dans la console.
 */
final class Staff {

    static final String PERMISSION = "ksrewardsgui.staff";
    /** Le nombre de tirages croît avec la valeur : au-delà, le tirage d'un coffre ralentirait le serveur. */
    private static final double VALEUR_MAX = 10_000;
    private static final int NOM_MAX = 40;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    /** Un don fait par un opérateur. */
    private record Don(long date, String op, String joueur, UUID uuid, String nom, double valeur) {
    }

    /** Saisie en cours d'un opérateur (gardée si la fenêtre est refusée, pour corriger sans tout retaper). */
    private static final class Brouillon {
        String joueur = "";
        String nom = "";
        String valeur = "";
    }

    private final KSRewardsGUI plugin;
    private final Lang lang;
    private final Gui gui;
    private final File fichier;
    /** Du plus ancien au plus récent. */
    private final List<Don> dons = new ArrayList<>();
    private final Map<UUID, Brouillon> brouillons = new HashMap<>();

    Staff(KSRewardsGUI plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.fichier = new File(plugin.getDataFolder(), "dons.yml");
        charger();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private static List<Component> lignes(Component... textes) {
        List<Component> lignes = new ArrayList<>();
        for (Component texte : textes) {
            lignes.addAll(Contenant.lignes(texte));
        }
        return lignes;
    }

    /** Petit menu de 3 rangées sur fond de vitres. */
    private Contenant petit(Player op, Component titre, String marque) {
        Contenant c = Contenant.pour(op, 3, titre, marque);
        for (int place = 0; place < c.taille(); place++) {
            c.poser(place, Contenant.decor(), null);
        }
        return c;
    }

    // ------------------------------------------------------------------ menu des opérateurs

    /** L'objet du bouton de la barre de /rewards. */
    ItemStack bouton() {
        return Contenant.objet(Material.COMMAND_BLOCK, t("staff.bouton", "<red>Outils des opérateurs"),
                lignes(t("staff.bouton-aide", "<gray>Donner un coffre, historique des dons, coffres de mort")));
    }

    void ouvrir(Player op) {
        if (!op.hasPermission(PERMISSION)) {
            return;
        }
        Contenant c = petit(op, t("staff.titre", "<dark_red><bold>Outils des opérateurs"), "ks_rewardsgui:staff");
        c.poser(11, Contenant.objet(Material.CHEST, t("staff.donner", "<gold>Donner un coffre"),
                lignes(t("staff.donner-aide", "<gray>Un coffre de récompense pour un joueur : bug trouvé, événement..."))),
                this::formulaire);
        c.poser(13, Contenant.objet(Material.WRITABLE_BOOK, t("staff.historique", "<yellow>Historique des dons"),
                lignes(t("staff.historique-aide", "<gray>Qui a donné quoi, à qui, et quand : <n> don(s)",
                        "n", dons.size()))), p -> historique(p, 0));
        if (coffreMort() != null) {
            c.poser(15, Contenant.objet(Material.SKELETON_SKULL, t("staff.morts", "<dark_red>Coffres de mort actifs"),
                    lignes(t("staff.morts-aide", "<gray>Les voir, et les transférer dans /rewards du mort"))),
                    this::coffresDeMort);
        }
        // /rewards est un menu de KS_RewardsGUI (sa propre classe Contenant) : ouvert au tick suivant, hors du clic.
        c.poser(22, Contenant.objet(Material.OAK_DOOR, t("staff.retour", "<yellow>Retour"), List.of()),
                p -> Bukkit.getScheduler().runTask(plugin, () -> plugin.ouvrirMenu(p)));
        c.ouvrir(op);
        lang.saveIfNeeded();
    }

    private static Plugin coffreMort() {
        Plugin p = Bukkit.getPluginManager().getPlugin("KS_CoffreMort");
        return p != null && p.isEnabled() ? p : null;
    }

    /**
     * Menu de KS_CoffreMort 1.1.0 (KSCoffreMort.ouvrirStaff), avec « Retour » vers ce menu. KS_CoffreMort dépend déjà de
     * ce plugin : il est appelé sans dépendance à la compilation (sinon les deux plugins se demanderaient l'un l'autre).
     */
    private void coffresDeMort(Player op) {
        if (CompteTest.enTest(op)) { // 1.10.0 : coffres de mort de la survie, pas depuis le monde de test
            refuser(op, t("staff.refus-monde-test", "<red>Pas depuis le monde de test."));
            return;
        }
        Plugin p = coffreMort();
        try {
            p.getClass().getMethod("ouvrirStaff", Player.class, Consumer.class)
                    .invoke(null, op, (Consumer<Player>) this::ouvrir);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // KS_CoffreMort plus ancien que 1.1.0 : sa commande, sans « Retour ».
            Contenant.apres(() -> op.performCommand("coffres tous"));
        }
    }

    // ------------------------------------------------------------------ donner un coffre

    private void formulaire(Player op) {
        Brouillon b = brouillons.computeIfAbsent(op.getUniqueId(), u -> new Brouillon());
        ActionButton valider = gui.form(t("staff.valider", "<green>Valider"), null, (p, vue) -> {
            b.joueur = texte(vue.getText("joueur"));
            b.nom = texte(vue.getText("nom"));
            b.valeur = texte(vue.getText("valeur"));
            verifier(p, b);
        });
        Contenant.saisie(op, () -> {
            gui.open(op, t("staff.titre-don", "<gold><bold>Donner un coffre"),
                    List.of(t("staff.don-aide", "<gray>Le joueur reçoit dans /rewards un coffre « Non ouvert » de cette "
                            + "valeur moyenne, nommé « Coffre : <nom du coffre> ».")),
                    List.of(gui.text("joueur", t("staff.champ-joueur", "Joueur"), b.joueur, 16),
                            gui.text("nom", t("staff.champ-nom", "Nom du coffre"), b.nom, NOM_MAX),
                            gui.text("valeur", t("staff.champ-valeur", "Valeur (émeraudes)"), b.valeur, 8)),
                    List.of(valider, gui.button(t("staff.retour", "<yellow>Retour"), null, this::ouvrir)),
                    gui.close(), 2);
            lang.saveIfNeeded();
        });
    }

    private static String texte(String s) {
        return s == null ? "" : s.trim();
    }

    /** Refus : petit message, puis la fenêtre revient avec ce qui a été tapé. */
    private void refuser(Player op, Component message) {
        gui.notice(op, t("staff.titre-don", "<gold><bold>Donner un coffre"), message, this::formulaire);
        lang.saveIfNeeded();
    }

    private void verifier(Player op, Brouillon b) {
        if (!op.hasPermission(PERMISSION)) {
            return;
        }
        OfflinePlayer cible = b.joueur.isEmpty() ? null : Bukkit.getOfflinePlayerIfCached(b.joueur);
        if (cible == null || (!cible.isOnline() && !cible.hasPlayedBefore())) {
            refuser(op, t("staff.refus-joueur", "<red>Joueur inconnu : il doit s'être déjà connecté à ce serveur."));
            return;
        }
        if (b.nom.isEmpty()) {
            refuser(op, t("staff.refus-nom", "<red>Donne un nom au coffre."));
            return;
        }
        double valeur;
        try {
            valeur = Double.parseDouble(b.valeur.replace(',', '.'));
        } catch (NumberFormatException e) {
            valeur = -1;
        }
        if (!(valeur > 0 && valeur <= VALEUR_MAX)) {
            refuser(op, t("staff.refus-valeur", "<red>Valeur invalide : un nombre d'émeraudes entre 0 et <max>.",
                    "max", Coffres.nombre(VALEUR_MAX)));
            return;
        }
        UUID compte = cible.getUniqueId();
        if (CompteTest.enTest(op)) {
            // 1.10.0 : depuis le monde de test, seulement au compte de test d'un joueur qui s'y trouve.
            Player enLigne = cible.getPlayer();
            if (enLigne == null || !CompteTest.enTest(enLigne)) {
                refuser(op, t("staff.refus-monde-test-joueur",
                        "<red>Depuis le monde de test, on ne donne qu'aux joueurs qui s'y trouvent."));
                return;
            }
            compte = CompteTest.de(compte);
        }
        confirmer(op, compte, cible.getName() == null ? b.joueur : cible.getName(), b.nom, valeur);
    }

    private void confirmer(Player op, UUID uuid, String joueur, String nom, double valeur) {
        Contenant c = petit(op, t("staff.titre-confirmer", "<dark_red><bold>Donner ce coffre ?"),
                "ks_rewardsgui:staff-confirmation");
        c.poser(11, Contenant.objet(Material.LIME_CONCRETE, t("staff.confirmer", "<green>Confirmer"), List.of()),
                p -> donner(p, uuid, joueur, nom, valeur));
        c.poser(13, Contenant.objet(Material.CHEST, t("staff.apercu", "<gold>Coffre : <nom>", "nom", nom), lignes(
                t("staff.apercu-joueur", "<gray>Pour <white><joueur>", "joueur", joueur),
                t("staff.apercu-valeur", "<gray>Valeur moyenne : <yellow><valeur></yellow> émeraude(s)",
                        "valeur", Coffres.nombre(valeur)))), null);
        c.poser(15, Contenant.objet(Material.RED_CONCRETE, t("staff.annuler", "<red>Annuler"),
                lignes(t("staff.annuler-aide", "<gray>Retour à la fenêtre, pour corriger"))), this::formulaire);
        c.ouvrir(op);
        lang.saveIfNeeded();
    }

    private void donner(Player op, UUID uuid, String joueur, String nom, double valeur) {
        if (!op.hasPermission(PERMISSION)) {
            return;
        }
        if (!KSRewardsGUI.deposerCoffre(uuid, joueur, "Staff", nom, valeur)) {
            Contenant.message(op, t("staff.refus-tables", "<red>Don impossible : fusion.yml absent ou vide sur ce "
                    + "serveur."), true);
            ouvrir(op);
            return;
        }
        Don don = new Don(System.currentTimeMillis(), op.getName(), joueur, uuid, nom, valeur);
        dons.add(don);
        sauver();
        brouillons.remove(op.getUniqueId());
        String ligne = "DON | " + op.getName() + " (" + op.getUniqueId() + ") donne « Coffre : " + nom + " » (valeur "
                + Coffres.nombre(valeur) + ") à " + joueur + " (" + uuid + ")";
        plugin.journal(ligne);
        plugin.getLogger().info(ligne);
        Contenant.message(op, t("staff.donne", "<green>Coffre envoyé dans /rewards de <joueur>.", "joueur", joueur),
                false);
        ouvrir(op);
    }

    // ------------------------------------------------------------------ historique

    private void historique(Player op, int page) {
        if (!op.hasPermission(PERMISSION)) {
            return;
        }
        List<Don> liste = new ArrayList<>(dons);
        java.util.Collections.reverse(liste);
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant c = Contenant.pour(op, t("staff.titre-historique", "<dark_gray>Historique des dons"),
                "ks_rewardsgui:staff-historique");
        c.barre();
        for (int i = 0; i < Contenant.PAR_PAGE && p * Contenant.PAR_PAGE + i < liste.size(); i++) {
            Don don = liste.get(p * Contenant.PAR_PAGE + i);
            c.poser(i, Contenant.objet(Material.CHEST, t("staff.don-nom", "<gold><nom>", "nom", don.nom()), lignes(
                    t("staff.don-qui", "<gray>De <white><op></white> à <white><joueur>", "op", don.op(),
                            "joueur", don.joueur()),
                    t("staff.don-valeur", "<gray>Valeur : <yellow><valeur></yellow> émeraude(s)",
                            "valeur", Coffres.nombre(don.valeur())),
                    t("staff.don-date", "<gray>Le <date>", "date",
                            DATE.format(Instant.ofEpochMilli(don.date()).atZone(ZoneId.of("Europe/Paris")))))), null);
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("staff.aucun-don", "<gray>Aucun don pour le moment"),
                    List.of()), null);
        }
        if (p > 0) {
            c.poser(c.bas(0), Contenant.objet(Material.ARROW, t("staff.precedent", "<yellow>Page précédente"),
                    List.of(t("staff.page", "<gray>Page <n> sur <total>", "n", p, "total", pages))),
                    j -> historique(j, p - 1));
        }
        if (p < pages - 1) {
            c.poser(c.bas(8), Contenant.objet(Material.ARROW, t("staff.suivant", "<yellow>Page suivante"),
                    List.of(t("staff.page", "<gray>Page <n> sur <total>", "n", p + 2, "total", pages))),
                    j -> historique(j, p + 1));
        }
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, t("staff.historique", "<yellow>Historique des dons"),
                lignes(t("staff.historique-livre", "<gray>Les coffres donnés par les opérateurs, le plus récent en "
                        + "premier. Aussi noté dans recuperations.log (lignes DON)."))), null);
        c.poser(c.bas(5), Contenant.objet(Material.OAK_DOOR, t("staff.retour", "<yellow>Retour"), List.of()),
                this::ouvrir);
        c.ouvrir(op);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        for (Map<?, ?> m : yaml.getMapList("dons")) {
            try {
                dons.add(new Don(((Number) m.get("date")).longValue(), String.valueOf(m.get("op")),
                        String.valueOf(m.get("joueur")), UUID.fromString(String.valueOf(m.get("uuid"))),
                        String.valueOf(m.get("nom")), ((Number) m.get("valeur")).doubleValue()));
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Don illisible ignoré dans dons.yml : " + m);
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> liste = new ArrayList<>();
        for (Don don : dons) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", don.date());
            m.put("op", don.op());
            m.put("joueur", don.joueur());
            m.put("uuid", don.uuid().toString());
            m.put("nom", don.nom());
            m.put("valeur", don.valeur());
            liste.add(m);
        }
        yaml.set("dons", liste);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer dons.yml : " + e.getMessage());
        }
    }
}
