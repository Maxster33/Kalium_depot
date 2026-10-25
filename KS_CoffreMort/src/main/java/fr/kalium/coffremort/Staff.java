package fr.kalium.coffremort;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.1.0 (LeKiwi06, 25/10/2026 : « une interface pour que les opérateurs voient les coffres de morts actifs et puisse
 * les transférer au rewards des joueurs concernés ») : coffres de mort de tous les joueurs, en menu de type contenant.
 *
 * - Liste : un coffre par case (tête du mort ; monde, coordonnées, temps restant, piles), celui qui disparaît le plus tôt
 *   en premier, 45 par page.
 * - Un clic : « Voir le contenu » (aperçu, rien ne se prend) ou « Transférer dans /rewards » (après confirmation) : le
 *   contenu part dans les récompenses du mort, en ligne ou non, et le coffre disparaît. Gratuit : ni badge ni jeton.
 * - Chaque transfert est noté dans `plugins/KS_CoffreMort/transferts.log` (date, opérateur, mort, position, contenu).
 */
final class Staff {

    static final String PERMISSION = "kscoffremort.staff";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KSCoffreMort plugin;
    private final Lang lang;
    private final File journal;

    Staff(KSCoffreMort plugin, Lang lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.journal = new File(plugin.getDataFolder(), "transferts.log");
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private Component titre() {
        return t("staff.titre", "<dark_red><bold>Coffres de mort actifs");
    }

    /** Lignes de description coupées à environ 35 caractères. */
    private static List<Component> lignes(Component... textes) {
        List<Component> lignes = new ArrayList<>();
        for (Component texte : textes) {
            lignes.addAll(Contenant.lignes(texte));
        }
        return lignes;
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

    /** Sortie de la barre d'actions : « Retour » vers l'écran d'où l'on vient, sinon « Fermer ». */
    private void sortie(Contenant c, Consumer<Player> retour) {
        if (retour == null) {
            c.poser(c.bas(5), Contenant.objet(Material.BARRIER, t("contenant.fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
        } else {
            c.poser(c.bas(5), Contenant.objet(Material.OAK_DOOR, t("contenant.retour", "<yellow>Retour"), List.of()),
                    retour);
        }
    }

    // ------------------------------------------------------------------ liste

    /** Tous les coffres de mort actifs. retour : écran précédent (le menu staff de /rewards), ou null. */
    void ouvrir(Player op, int page, Consumer<Player> retour) {
        if (!op.hasPermission(PERMISSION)) {
            return;
        }
        List<KSCoffreMort.Coffre> liste = new ArrayList<>(plugin.tousLesCoffres());
        liste.sort(Comparator.comparingLong(c -> c.expiration));
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant c = Contenant.pour(op, titre(), "ks_coffremort:staff");
        c.barre();
        long maintenant = System.currentTimeMillis();
        for (int i = 0; i < Contenant.PAR_PAGE && p * Contenant.PAR_PAGE + i < liste.size(); i++) {
            KSCoffreMort.Coffre coffre = liste.get(p * Contenant.PAR_PAGE + i);
            c.poser(i, Contenant.objet(tete(coffre.joueur), t("staff.coffre", "<gold><nom>", "nom", coffre.nom), lignes(
                    t("staff.position", "<gray><monde> <white><x> <y> <z>", "monde", plugin.nomMonde(coffre.pos.monde()),
                            "x", coffre.pos.x(), "y", coffre.pos.y(), "z", coffre.pos.z()),
                    t("staff.temps", "<gray>Disparaît dans <white><temps>", "temps",
                            KSCoffreMort.temps(coffre.expiration - maintenant)),
                    t("staff.piles", "<gray><piles> pile(s) d'objets", "piles", KSCoffreMort.contenu(coffre).size()),
                    t("staff.clic", "<yellow>Clic : voir ou transférer"))), j -> detail(j, coffre.id, p, retour));
        }
        if (liste.isEmpty()) {
            c.poser(22, Contenant.objet(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    t("staff.aucun", "<gray>Aucun coffre de mort actif"), List.of()), null);
        }
        if (p > 0) {
            c.poser(c.bas(0), Contenant.objet(Material.ARROW, t("contenant.precedent", "<yellow>Page précédente"),
                    List.of(t("contenant.page", "<gray>Page <n> sur <total>", "n", p, "total", pages))),
                    j -> ouvrir(j, p - 1, retour));
        }
        if (p < pages - 1) {
            c.poser(c.bas(8), Contenant.objet(Material.ARROW, t("contenant.suivant", "<yellow>Page suivante"),
                    List.of(t("contenant.page", "<gray>Page <n> sur <total>", "n", p + 2, "total", pages))),
                    j -> ouvrir(j, p + 1, retour));
        }
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, t("staff.aide-nom", "<gold>Coffres de mort (opérateurs)"),
                lignes(t("staff.aide", "<gray>Les coffres de mort de tous les joueurs, le plus proche de sa fin en "
                        + "premier. Un transfert envoie le contenu dans les récompenses (/rewards) du mort, même hors "
                        + "ligne, et retire le coffre. Chaque transfert est noté."))), null);
        c.poser(c.bas(4), Contenant.objet(Material.CLOCK, t("staff.actualiser", "<yellow>Actualiser"),
                lignes(t("staff.total", "<gray><n> coffre(s) actif(s)", "n", liste.size()))),
                j -> ouvrir(j, p, retour));
        sortie(c, retour);
        c.ouvrir(op);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ un coffre

    private void detail(Player op, String id, int page, Consumer<Player> retour) {
        KSCoffreMort.Coffre coffre = plugin.coffre(id);
        if (coffre == null) {
            Contenant.message(op, t("staff.disparu", "<red>Ce coffre de mort n'existe plus."), true);
            ouvrir(op, page, retour);
            return;
        }
        Contenant c = Contenant.pour(op, 3, t("staff.titre-coffre", "<dark_red><bold>Coffre de <nom>", "nom", coffre.nom),
                "ks_coffremort:staff-coffre");
        for (int place = 0; place < c.taille(); place++) {
            c.poser(place, Contenant.decor(), null);
        }
        c.poser(11, Contenant.objet(Material.SPYGLASS, t("staff.voir", "<white>Voir le contenu"),
                lignes(t("staff.voir-aide", "<gray>Aperçu : rien ne se prend"))), j -> {
            KSCoffreMort.Coffre actuel = plugin.coffre(id);
            if (actuel == null) {
                detail(j, id, page, retour);
            } else {
                Contenant.apres(() -> plugin.montrer(j, actuel));
            }
        });
        c.poser(13, Contenant.objet(tete(coffre.joueur), t("staff.coffre", "<gold><nom>", "nom", coffre.nom), lignes(
                t("staff.position", "<gray><monde> <white><x> <y> <z>", "monde", plugin.nomMonde(coffre.pos.monde()),
                        "x", coffre.pos.x(), "y", coffre.pos.y(), "z", coffre.pos.z()),
                t("staff.temps", "<gray>Disparaît dans <white><temps>", "temps",
                        KSCoffreMort.temps(coffre.expiration - System.currentTimeMillis())),
                t("staff.piles", "<gray><piles> pile(s) d'objets", "piles", KSCoffreMort.contenu(coffre).size()))), null);
        c.poser(15, Contenant.objet(Material.ENDER_CHEST, t("staff.transferer", "<light_purple>Transférer dans /rewards"),
                lignes(t("staff.transferer-aide", "<gray>Le contenu part dans les récompenses de <nom> et le coffre "
                        + "disparaît", "nom", coffre.nom))), j -> confirmer(j, id, page, retour));
        c.poser(22, Contenant.objet(Material.OAK_DOOR, t("contenant.retour", "<yellow>Retour"), List.of()),
                j -> ouvrir(j, page, retour));
        c.ouvrir(op);
        lang.saveIfNeeded();
    }

    /** Confirmation : « Confirmer » à gauche, le texte au centre, « Annuler » à droite. */
    private void confirmer(Player op, String id, int page, Consumer<Player> retour) {
        KSCoffreMort.Coffre coffre = plugin.coffre(id);
        if (coffre == null) {
            detail(op, id, page, retour);
            return;
        }
        Contenant c = Contenant.pour(op, 3, t("staff.titre-confirmer", "<dark_red><bold>Transférer ce coffre ?"),
                "ks_coffremort:staff-confirmation");
        for (int place = 0; place < c.taille(); place++) {
            c.poser(place, Contenant.decor(), null);
        }
        c.poser(11, Contenant.objet(Material.LIME_CONCRETE, t("contenant.confirmer", "<green>Confirmer"), List.of()),
                j -> transferer(j, id, page, retour));
        c.poser(13, Contenant.objet(Material.PAPER, t("contenant.a-confirmer", "<gold>À confirmer"), lignes(
                t("staff.confirmer", "<white>Les <piles> pile(s) du coffre de <nom> partent dans ses récompenses "
                        + "(/rewards) et le coffre disparaît.", "piles", KSCoffreMort.contenu(coffre).size(),
                        "nom", coffre.nom))), null);
        c.poser(15, Contenant.objet(Material.RED_CONCRETE, t("contenant.annuler", "<red>Annuler"), List.of()),
                j -> detail(j, id, page, retour));
        c.ouvrir(op);
        lang.saveIfNeeded();
    }

    private void transferer(Player op, String id, int page, Consumer<Player> retour) {
        KSCoffreMort.Coffre coffre = plugin.coffre(id);
        if (coffre == null || !op.hasPermission(PERMISSION)) {
            Contenant.message(op, t("staff.disparu", "<red>Ce coffre de mort n'existe plus."), true);
            ouvrir(op, page, retour);
            return;
        }
        List<ItemStack> objets = KSCoffreMort.contenu(coffre);
        String resume = resume(objets);
        int envoyees = plugin.transferer(coffre, "Coffre de mort transféré le "
                + LocalDateTime.now(ZoneId.of("Europe/Paris")).format(DATE));
        if (envoyees < 0) {
            Contenant.message(op, t("staff.sans-rewards", "<red>Les récompenses sont indisponibles : coffre gardé."), true);
            ouvrir(op, page, retour);
            return;
        }
        String ligne = op.getName() + " transfère le coffre de mort de " + coffre.nom + " (" + coffre.joueur + ") en "
                + coffre.pos.monde() + " " + coffre.pos.x() + " " + coffre.pos.y() + " " + coffre.pos.z()
                + " dans /rewards : " + envoyees + "/" + objets.size() + " pile(s) ; contenu : " + resume;
        noter(ligne);
        plugin.getLogger().info(ligne);
        if (envoyees < objets.size()) {
            Contenant.message(op, t("staff.partiel", "<gold>Transfert partiel : <n> pile(s) restent dans le coffre.",
                    "n", objets.size() - envoyees), true);
        } else {
            Contenant.message(op, t("staff.transfere", "<green>Contenu envoyé dans les récompenses de <nom>.",
                    "nom", coffre.nom), false);
            Player mort = Bukkit.getPlayer(coffre.joueur);
            if (mort != null) {
                mort.sendMessage(t("staff.prevenu", "<gold>Un opérateur a envoyé le contenu de ton coffre de mort "
                        + "(<x> <y> <z>) dans <white>/rewards<gold>.", "x", coffre.pos.x(), "y", coffre.pos.y(),
                        "z", coffre.pos.z()));
            }
        }
        ouvrir(op, page, retour);
    }

    /** « 3 DIAMOND, 1 DIAMOND_SWORD (Épée de Lupo), ... » : de quoi vérifier un transfert après coup. */
    private static String resume(List<ItemStack> objets) {
        List<String> parties = new ArrayList<>();
        for (ItemStack objet : objets) {
            String nom = objet.hasItemMeta() && objet.getItemMeta().hasDisplayName()
                    ? " (" + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(objet.getItemMeta().displayName()) + ")" : "";
            parties.add(objet.getAmount() + " " + objet.getType().name() + nom);
        }
        return parties.isEmpty() ? "rien" : String.join(", ", parties);
    }

    private void noter(String ligne) {
        plugin.getDataFolder().mkdirs();
        try (PrintWriter sortie = new PrintWriter(new FileWriter(journal, StandardCharsets.UTF_8, true))) {
            sortie.println(LocalDateTime.now(ZoneId.of("Europe/Paris")).format(DATE_LOG) + " " + ligne);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'écrire transferts.log : " + e.getMessage());
        }
    }
}
