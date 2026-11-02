package fr.kalium.encapsuleur;

import fr.kalium.encapsuleur.Analyse.Bilan;
import fr.kalium.menu.api.Contenant;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Menus en coffre de l'encapsuleur : avertissement, liste des blocs (restants ou gênants), aperçu de la pose. */
final class Menus {

    private Menus() {
    }

    /** « Attention, certains blocs ne sont pas encapsulés » : Voir la liste, Valider, Annuler (décidé par LeKiwi06). */
    static void avertissement(KSEncapsuleur p, Player joueur, Bilan bilan) {
        Contenant c = Contenant.pour(joueur, 3, p.lang.c("avertissement.titre", "Attention"), "encapsuleur-attention");
        for (int i = 0; i < c.taille(); i++) {
            c.poser(i, Contenant.decor(), null);
        }
        c.poser(4, Contenant.objet(Material.PAPER, p.lang.c("avertissement.nom",
                "<yellow>Attention, certains blocs ne sont pas encapsulés"), List.of(
                p.lang.c("avertissement.nombre", "<gray><n> bloc(s) resteront sur place.", "n", bilan.nombreRestants()),
                p.lang.c("avertissement.raison", "<gray>Contenants pleins et blocs incassables.")
        )), null);
        c.poser(11, Contenant.objet(Material.BOOK, p.lang.c("avertissement.liste", "<aqua>Voir la liste"), List.of()),
                j -> liste(p, j, p.lang.c("liste.titre", "Blocs qui restent"), bilan.restent,
                        k -> avertissement(p, k, bilan)));
        c.poser(13, Contenant.objet(Material.LIME_CONCRETE, p.lang.c("avertissement.valider", "<green>Valider"),
                Contenant.lignes(p.lang.c("avertissement.valider-info", "<gray>Encapsule sans ces blocs."))),
                j -> p.encapsuler(j, true));
        c.poser(15, Contenant.objet(Material.RED_CONCRETE, p.lang.c("avertissement.annuler", "<red>Annuler"),
                Contenant.lignes(p.lang.c("avertissement.annuler-info", "<gray>Rien ne se passe."))),
                Contenant::fermer);
        c.ouvrir(joueur);
        p.lang.saveIfNeeded();
    }

    /** Les blocs par type, avec leur nombre et la position de chacun (8 positions par type au plus). */
    static void liste(KSEncapsuleur p, Player joueur, Component titre, Map<Material, List<Block>> blocs,
                      Consumer<Player> retour) {
        Contenant c = Contenant.pour(joueur, 6, titre, "encapsuleur-liste");
        int place = 0;
        for (Map.Entry<Material, List<Block>> e : blocs.entrySet()) {
            if (place >= 45) {
                break;
            }
            List<Component> lignes = new ArrayList<>();
            List<Block> liste = e.getValue();
            for (int i = 0; i < liste.size() && i < 8; i++) {
                Block b = liste.get(i);
                lignes.add(p.lang.c("liste.position", "<gray><x> <y> <z>", "x", b.getX(), "y", b.getY(), "z", b.getZ()));
            }
            if (liste.size() > 8) {
                lignes.add(p.lang.c("liste.autres", "<gray>et <n> autre(s)", "n", liste.size() - 8));
            }
            Material icone = e.getKey().isItem() ? e.getKey() : Material.BARRIER;
            c.poser(place++, Contenant.objet(icone, Component.translatable(e.getKey().translationKey())
                    .append(Component.text(" x" + liste.size())), lignes), null);
        }
        c.barre();
        c.poser(c.bas(4), Contenant.objet(Material.ARROW, p.lang.c("liste.retour", "<yellow>Retour"), List.of()),
                retour);
        c.ouvrir(joueur);
        p.lang.saveIfNeeded();
    }

    /** Menu de l'aperçu : tourner, miroir, décaler, valider, annuler. */
    static void pose(KSEncapsuleur p, Player joueur, Apercu.Etat etat) {
        Contenant c = Contenant.pour(joueur, 4, p.lang.c("pose.titre", "Poser la construction"), "encapsuleur-pose");
        for (int i = 0; i < c.taille(); i++) {
            c.poser(i, Contenant.decor(), null);
        }
        Apercu a = p.apercu;
        bouton(c, 10, Material.SPECTRAL_ARROW, p.lang.c("pose.gauche-tourner", "<yellow>Tourner à gauche"),
                j -> a.changer(j, e -> e.tours--));
        bouton(c, 11, Material.SPECTRAL_ARROW, p.lang.c("pose.droite-tourner", "<yellow>Tourner à droite"),
                j -> a.changer(j, e -> e.tours++));
        bouton(c, 13, Material.GLASS_PANE, p.lang.c("pose.miroir", "<yellow>Miroir"),
                j -> a.changer(j, e -> e.miroir = !e.miroir));
        bouton(c, 15, Material.LIME_CONCRETE, p.lang.c("pose.valider", "<green>Valider"), a::valider);
        bouton(c, 16, Material.RED_CONCRETE, p.lang.c("pose.annuler", "<red>Annuler"), j -> {
            a.annuler(j);
            Contenant.fermer(j);
        });
        bouton(c, 19, Material.OAK_BUTTON, p.lang.c("pose.avancer", "<aqua>Éloigner"), j -> a.changer(j, e -> e.avance++));
        bouton(c, 20, Material.OAK_BUTTON, p.lang.c("pose.reculer", "<aqua>Rapprocher"), j -> a.changer(j, e -> e.avance--));
        bouton(c, 21, Material.OAK_BUTTON, p.lang.c("pose.a-gauche", "<aqua>Vers la gauche"), j -> a.changer(j, e -> e.droite--));
        bouton(c, 22, Material.OAK_BUTTON, p.lang.c("pose.a-droite", "<aqua>Vers la droite"), j -> a.changer(j, e -> e.droite++));
        bouton(c, 23, Material.OAK_BUTTON, p.lang.c("pose.monter", "<aqua>Monter"), j -> a.changer(j, e -> e.haut++));
        bouton(c, 24, Material.OAK_BUTTON, p.lang.c("pose.descendre", "<aqua>Descendre"), j -> a.changer(j, e -> e.haut--));
        c.poser(4, Contenant.objet(Material.STRUCTURE_BLOCK, p.lang.c("pose.info", "<gold>Aperçu"), List.of(
                p.lang.c("pose.info-1", "<gray>Contour bleu, face avant en vert,"),
                p.lang.c("pose.info-2", "<gray>coins en lanternes de mer."),
                p.lang.c("pose.info-3", "<gray>Clic droit avec la construction"),
                p.lang.c("pose.info-4", "<gray>pour rouvrir ce menu."))), null);
        c.ouvrir(joueur);
        p.lang.saveIfNeeded();
    }

    private static void bouton(Contenant c, int place, Material icone, Component nom, Consumer<Player> action) {
        c.poser(place, Contenant.objet(icone, nom, List.of()), action);
    }
}
