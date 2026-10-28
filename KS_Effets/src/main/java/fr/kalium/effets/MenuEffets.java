package fr.kalium.effets;

import fr.kalium.menu.api.Contenant;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * /effects : menu en coffre des effets en cours (du jeu et custom) avec leur temps restant, rafraîchi chaque seconde.
 * Clic sur un effet : petit menu pour le suivre en barre de boss ou en barre d'action (un seul effet par barre : en
 * choisir un autre remplace le précédent), ou ne plus le suivre.
 */
final class MenuEffets {

    private static final String MARQUE = "ks_effets:effets";

    /** Marque du petit menu de suivi d'un effet. */
    private record Choix(String cle) {
    }

    private final KSEffets plugin;

    MenuEffets(KSEffets plugin) {
        this.plugin = plugin;
    }

    void ouvrir(Player joueur) {
        Contenant c = Contenant.pour(joueur, plugin.t("menu.titre", "Tes effets"), MARQUE);
        remplir(joueur, c);
        c.ouvrir(joueur);
    }

    /** Chaque seconde : met à jour les temps du menu s'il est ouvert. */
    void rafraichir(Player joueur) {
        Contenant c = Contenant.ouvert(joueur, MARQUE);
        if (c != null) {
            c.vider();
            remplir(joueur, c);
        }
    }

    private void remplir(Player joueur, Contenant c) {
        Registre.Compte compte = plugin.registre().compte(plugin.compteDe(joueur), false);
        List<KSEffets.Entree> entrees = plugin.entrees(joueur);
        int place = 0;
        for (KSEffets.Entree e : entrees) {
            if (place >= Contenant.PAR_PAGE) {
                break;
            }
            boolean boss = compte != null && e.cle().equals(compte.suiviBoss);
            boolean action = compte != null && e.cle().equals(compte.suiviAction);
            List<Component> lignes = new ArrayList<>();
            lignes.add(plugin.t("menu.temps", "<gray>Temps restant : <white><temps>", "temps", plugin.temps(e.restant())));
            if (boss) {
                lignes.add(plugin.t("menu.suivi-boss", "<gold>Suivi en barre de boss"));
            }
            if (action) {
                lignes.add(plugin.t("menu.suivi-action", "<gold>Suivi en barre d'action"));
            }
            lignes.add(plugin.t("menu.clic", "<yellow>Clic : suivre cet effet"));
            String cle = e.cle();
            c.poser(place++, brillant(Contenant.objet(e.icone(), e.nom(), lignes), boss || action), j -> choix(j, cle));
        }
        if (entrees.isEmpty()) {
            c.poser(22, Contenant.objet(Material.GLASS_BOTTLE, plugin.t("menu.vide", "<gray>Aucun effet en cours"),
                    List.of()), null);
        }
        c.barre();
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, plugin.t("menu.aide", "<yellow>Aide"),
                Contenant.lignes(plugin.t("menu.aide-texte", "<gray>Clique sur un effet pour suivre son temps "
                        + "restant en barre de boss ou en barre d'action. Un seul effet par barre."))), null);
        c.poser(c.bas(5), Contenant.objet(Material.BARRIER, plugin.t("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
    }

    /** Petit menu d'un effet : barre de boss, barre d'action, ne plus suivre, retour. */
    private void choix(Player joueur, String cle) {
        KSEffets.Entree e = plugin.entree(joueur, cle);
        if (e == null) {
            Contenant.message(joueur, plugin.t("message.termine", "<red>Cet effet est terminé."), true);
            ouvrir(joueur);
            return;
        }
        Registre.Compte compte = plugin.registre().compte(plugin.compteDe(joueur), false);
        boolean boss = compte != null && cle.equals(compte.suiviBoss);
        boolean action = compte != null && cle.equals(compte.suiviAction);
        Contenant c = Contenant.pour(joueur, 1, plugin.t("choix.titre", "Suivre un effet"), new Choix(cle));
        c.poser(0, Contenant.objet(e.icone(), e.nom(), List.of(plugin.t("menu.temps",
                "<gray>Temps restant : <white><temps>", "temps", plugin.temps(e.restant())))), null);
        c.poser(2, brillant(Contenant.objet(Material.DRAGON_HEAD, plugin.t("choix.boss", "<aqua>Barre de boss"),
                Contenant.lignes(plugin.t("choix.boss-texte", "<gray>Le temps restant en haut de l'écran."))), boss),
                j -> suivre(j, cle, true));
        c.poser(4, brillant(Contenant.objet(Material.OAK_SIGN, plugin.t("choix.action", "<aqua>Barre d'action"),
                Contenant.lignes(plugin.t("choix.action-texte",
                        "<gray>Le temps restant au-dessus de ta barre d'objets."))), action),
                j -> suivre(j, cle, false));
        if (boss || action) {
            c.poser(6, Contenant.objet(Material.GRAY_DYE, plugin.t("choix.arreter", "<red>Ne plus suivre"),
                    List.of()), j -> arreter(j, cle));
        }
        c.poser(8, Contenant.objet(Material.OAK_DOOR, plugin.t("menu.retour", "<yellow>Retour"), List.of()),
                this::ouvrir);
        c.ouvrir(joueur);
    }

    private void suivre(Player joueur, String cle, boolean boss) {
        KSEffets.Entree e = plugin.entree(joueur, cle);
        if (e == null) {
            Contenant.message(joueur, plugin.t("message.termine", "<red>Cet effet est terminé."), true);
            ouvrir(joueur);
            return;
        }
        Registre.Compte compte = plugin.registre().compte(plugin.compteDe(joueur), true);
        if (boss) {
            compte.suiviBoss = cle;
        } else {
            compte.suiviAction = cle;
        }
        plugin.registre().modifie();
        plugin.suivi().maj(joueur, compte);
        Contenant.message(joueur, boss
                ? plugin.t("message.boss", "<green>Suivi en barre de boss : <nom>", "nom", e.nom())
                : plugin.t("message.action", "<green>Suivi en barre d'action : <nom>", "nom", e.nom()), false);
        ouvrir(joueur);
    }

    private void arreter(Player joueur, String cle) {
        Registre.Compte compte = plugin.registre().compte(plugin.compteDe(joueur), false);
        if (compte != null) {
            if (cle.equals(compte.suiviBoss)) {
                compte.suiviBoss = null;
            }
            if (cle.equals(compte.suiviAction)) {
                compte.suiviAction = null;
            }
            plugin.registre().modifie();
            plugin.suivi().maj(joueur, compte);
        }
        Contenant.message(joueur, plugin.t("message.arret", "<gray>Cet effet n'est plus suivi."), false);
        ouvrir(joueur);
    }

    /** Objet de menu qui brille (effet suivi, choix en cours). */
    private static ItemStack brillant(ItemStack objet, boolean oui) {
        if (oui) {
            ItemMeta meta = objet.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            objet.setItemMeta(meta);
        }
        return objet;
    }
}
