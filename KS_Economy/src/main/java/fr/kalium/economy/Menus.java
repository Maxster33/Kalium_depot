package fr.kalium.economy;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 1.5.0 (LeKiwi06, 09/10/2026 : « rendre les interfaces plus jolies [...] davantage d'interfaces de type contenant
 * quand c'est possible au lieu des boutons ») : pièces communes des menus de KS_Economy, tous passés en coffres
 * (classe Contenant de KLM_Menu 2.11.0). Modèle : objets cliquables en haut, barre d'actions en bas (pages aux deux
 * bouts, aide, action principale au centre, « Retour » ou « Fermer » à sa droite) ; les messages courts s'affichent
 * au-dessus de la barre d'objets du joueur ; une saisie (texte, nombre) reste une petite fenêtre de Gui.
 */
final class Menus {

    private final Lang lang;

    Menus(Lang lang) {
        this.lang = lang;
    }

    /** Menu de cette marque (le même coffre est rempli à nouveau s'il est déjà ouvert), barre d'actions posée. */
    Contenant menu(Player joueur, int rangees, Component titre, Object marque) {
        Contenant c = Contenant.pour(joueur, rangees, titre, marque);
        c.barre();
        return c;
    }

    /** Sortie de la barre d'actions : « Retour » vers l'écran précédent, ou « Fermer » s'il n'y en a pas. */
    void sortie(Contenant c, Consumer<Player> retour) {
        if (retour == null) {
            c.poser(c.bas(5), Contenant.objet(Material.BARRIER, lang.c("contenant.fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
        } else {
            c.poser(c.bas(5), Contenant.objet(Material.OAK_DOOR, lang.c("contenant.retour", "<yellow>Retour"),
                    List.of()), retour);
        }
    }

    /** Flèches de page aux deux bouts de la barre d'actions. */
    void pages(Contenant c, int page, int pages, BiConsumer<Player, Integer> aller) {
        if (page > 0) {
            c.poser(c.bas(0), Contenant.objet(Material.ARROW, lang.c("contenant.precedent", "<yellow>Page précédente"),
                    List.of(lang.c("contenant.page", "<gray>Page <n> sur <total>", "n", page, "total", pages))),
                    j -> aller.accept(j, page - 1));
        }
        if (page < pages - 1) {
            c.poser(c.bas(8), Contenant.objet(Material.ARROW, lang.c("contenant.suivant", "<yellow>Page suivante"),
                    List.of(lang.c("contenant.page", "<gray>Page <n> sur <total>", "n", page + 2, "total", pages))),
                    j -> aller.accept(j, page + 1));
        }
    }

    /** Livre d'aide de la barre d'actions (sans action) : les textes sont coupés en lignes courtes. */
    void aide(Contenant c, Component nom, Component... textes) {
        c.poser(c.bas(3), Contenant.objet(Material.BOOK, nom, lignes(textes)), null);
    }

    /** Lignes de description : chaque texte coupé à environ 35 caractères (une description ne passe pas à la ligne). */
    static List<Component> lignes(Component... textes) {
        List<Component> lignes = new ArrayList<>();
        for (Component texte : textes) {
            if (texte != null) {
                lignes.addAll(Contenant.lignes(texte));
            }
        }
        return lignes;
    }

    /** Un objet de menu dont la description est coupée en lignes courtes. */
    static ItemStack objet(Material materiau, Component nom, Component... textes) {
        return Contenant.objet(materiau, nom, lignes(textes));
    }

    static ItemStack objet(ItemStack modele, Component nom, Component... textes) {
        return Contenant.objet(modele, nom, lignes(textes));
    }

    /** Un objet montré tel quel (nom, enchantements, description d'origine), en pile de 1 à sa pile maximale. */
    static ItemStack telQuel(ItemStack modele, int nombre) {
        ItemStack objet = modele.clone();
        objet.setAmount(Math.max(1, Math.min(nombre, objet.getMaxStackSize())));
        return objet;
    }

    /** Confirmation : « Confirmer » à gauche, le texte au centre, « Annuler » à droite. */
    void confirmer(Player joueur, Component titre, Component texte, Consumer<Player> oui, Consumer<Player> non) {
        Contenant c = Contenant.pour(joueur, 3, titre, "confirmation");
        for (int place = 0; place < c.taille(); place++) {
            c.poser(place, Contenant.decor(), null);
        }
        c.poser(11, Contenant.objet(Material.LIME_CONCRETE, lang.c("contenant.confirmer", "<green>Confirmer"),
                List.of()), oui);
        c.poser(13, objet(Material.PAPER, lang.c("contenant.a-confirmer", "<gold>À confirmer"), texte), null);
        c.poser(15, Contenant.objet(Material.RED_CONCRETE, lang.c("contenant.annuler", "<red>Annuler"), List.of()),
                non == null ? Contenant::fermer : non);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /**
     * Message court au-dessus de la barre d'objets (refus : son grave). Sans menu ouvert (commande tapée, clic sur un
     * panneau), il est aussi écrit dans le tchat : au-dessus de la barre d'objets, il ne reste que 3 secondes.
     */
    void message(Player joueur, Component texte, boolean refus) {
        Contenant.message(joueur, texte, refus);
        if (!(joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant)) {
            joueur.sendMessage(texte);
        }
        lang.saveIfNeeded();
    }

    /** Tête d'un joueur (l'objet d'un magasin dans les listes). */
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
}
