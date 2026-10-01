package fr.kalium.menu.api;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 2.5.0 - aucun texte ne doit défiler dans un bouton ou un champ d'un menu (demande de LeKiwi06, 01/10/2026 : « c'est
 * illisible »). Le jeu fait défiler un texte plus large que son bouton : avant d'afficher un menu, chaque bouton et
 * chaque champ (liste déroulante, curseur) est élargi à la largeur de son texte, mesurée avec la police de Minecraft.
 *
 * - Boutons d'un même menu : même largeur (celle du plus long texte, au moins la largeur prévue), 400 pixels au plus ;
 *   si les colonnes ne tiennent plus à l'écran, moins de colonnes.
 * - Un texte qui dépasse encore 400 pixels est signalé une fois dans la console (texte à raccourcir).
 *
 * Utilisé par Gui (tous les plugins qui s'en servent) et par les menus construits à la main (KlmMenu, KalGames,
 * KG_BingoGame, KS_Economy) : {@link #ajuster}.
 */
public final class Lisible {

    /** Largeur maximale d'un bouton ou d'un champ (pixels de l'interface). */
    public static final int LARGEUR_MAX = 400;
    /** Place des colonnes : au-delà, une colonne de moins (écran de 480 pixels en échelle d'interface 4). */
    private static final int LARGEUR_ECRAN = 500;
    /** Marge autour du texte dans un bouton (bords compris). */
    private static final int MARGE = 12;

    /** Textes trop longs déjà signalés (une fois chacun). */
    private static final Set<String> SIGNALES = new HashSet<>();

    private Lisible() {
    }

    /** Menu prêt à afficher : champs, boutons, bouton de sortie et nombre de colonnes ajustés. */
    public record Fenetre(List<DialogInput> champs, List<ActionButton> boutons, ActionButton sortie, int colonnes) {
    }

    /** Ajuste un menu (sortie : peut être null ; colonnes : celles prévues). */
    public static Fenetre ajuster(List<DialogInput> champs, List<ActionButton> boutons, ActionButton sortie,
                                  int colonnes) {
        int largeur = 0;
        for (ActionButton bouton : boutons) {
            largeur = Math.max(largeur, Math.max(bouton.width(), besoin(bouton.label())));
        }
        if (sortie != null) {
            largeur = Math.max(largeur, Math.max(sortie.width(), besoin(sortie.label())));
        }
        largeur = Math.min(largeur, LARGEUR_MAX);
        List<ActionButton> ajustes = new ArrayList<>();
        for (ActionButton bouton : boutons) {
            ajustes.add(largeur(bouton, largeur));
        }
        ActionButton sortieAjustee = sortie == null ? null : largeur(sortie, largeur);
        int c = Math.max(1, colonnes);
        while (c > 1 && c * largeur + (c - 1) * 4 > LARGEUR_ECRAN) {
            c--;
        }
        List<DialogInput> champsAjustes = new ArrayList<>();
        for (DialogInput champ : champs) {
            champsAjustes.add(champ(champ));
        }
        return new Fenetre(champsAjustes, ajustes, sortieAjustee, c);
    }

    /** Bouton à cette largeur (inchangé s'il l'a déjà). */
    private static ActionButton largeur(ActionButton bouton, int largeur) {
        if (besoin(bouton.label()) > LARGEUR_MAX) {
            signaler(bouton.label());
        }
        return bouton.width() == largeur ? bouton
                : ActionButton.create(bouton.label(), bouton.tooltip(), largeur, bouton.action());
    }

    /** Liste déroulante (« libellé: option ») et curseur (« libellé: valeur ») élargis à leur plus long texte. */
    private static DialogInput champ(DialogInput champ) {
        if (champ instanceof SingleOptionDialogInput choix) {
            int texte = 0;
            for (SingleOptionDialogInput.OptionEntry option : choix.entries()) {
                Component affiche = option.display() != null ? option.display() : Component.text(option.id());
                texte = Math.max(texte, largeurTexte(affiche));
            }
            if (choix.labelVisible()) {
                texte += largeurTexte(choix.label()) + largeurTexte(Component.text(": "));
            }
            int voulu = Math.min(LARGEUR_MAX, texte + MARGE);
            if (texte + MARGE > LARGEUR_MAX) {
                signaler(choix.label());
            }
            return voulu <= choix.width() ? champ
                    : DialogInput.singleOption(choix.key(), voulu, choix.entries(), choix.label(), choix.labelVisible());
        }
        if (champ instanceof NumberRangeDialogInput curseur) {
            String valeur = String.valueOf((long) Math.max(Math.abs(curseur.start()), Math.abs(curseur.end())));
            int texte = largeurTexte(curseur.label()) + largeurTexte(Component.text(": -" + valeur + ".00"));
            int voulu = Math.min(LARGEUR_MAX, texte + MARGE);
            if (texte + MARGE > LARGEUR_MAX) {
                signaler(curseur.label());
            }
            return voulu <= curseur.width() ? champ
                    : DialogInput.numberRange(curseur.key(), voulu, curseur.label(), curseur.labelFormat(),
                    curseur.start(), curseur.end(), curseur.initial(), curseur.step());
        }
        return champ;
    }

    private static void signaler(Component texte) {
        String brut = PlainTextComponentSerializer.plainText().serialize(texte);
        synchronized (SIGNALES) {
            if (SIGNALES.add(brut)) {
                Bukkit.getLogger().warning("[KLM_Menu] Texte trop long pour un bouton ou un champ (" + largeurTexte(texte)
                        + " pixels, " + LARGEUR_MAX + " au plus), à raccourcir : " + brut);
            }
        }
    }

    /** Largeur nécessaire au bouton pour ce texte. */
    public static int besoin(Component texte) {
        return largeurTexte(texte) + MARGE;
    }

    // ------------------------------------------------------------------ police de Minecraft

    /** Largeur d'un texte en pixels de l'interface (police par défaut ; gras : +1 par caractère). */
    public static int largeurTexte(Component texte) {
        return texte == null ? 0 : largeur(texte, false);
    }

    private static int largeur(Component composant, boolean grasParent) {
        TextDecoration.State etat = composant.style().decoration(TextDecoration.BOLD);
        boolean gras = etat == TextDecoration.State.TRUE || (etat == TextDecoration.State.NOT_SET && grasParent);
        int total = 0;
        if (composant instanceof TextComponent texte) {
            total += largeur(texte.content(), gras);
        } else {
            // Texte traduit, sélecteur...: estimation avec son texte brut, sans les enfants (comptés ci-dessous).
            total += largeur(PlainTextComponentSerializer.plainText().serialize(composant.children(List.of())), gras);
        }
        for (Component enfant : composant.children()) {
            total += largeur(enfant, gras);
        }
        return total;
    }

    private static int largeur(String texte, boolean gras) {
        int total = 0;
        for (int i = 0; i < texte.length(); i++) {
            total += avance(texte.charAt(i)) + (gras ? 1 : 0);
        }
        return total;
    }

    /** Avance d'un caractère (largeur du dessin + 1 pixel d'espace) ; accents : comme la lettre de base. */
    private static int avance(char c) {
        if (c > 127) {
            String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
            c = base.isEmpty() ? c : base.charAt(0);
        }
        return switch (c) {
            case '!', '\'', ',', '.', ':', ';', 'i', '|' -> 2;
            case 'l', '`' -> 3;
            case ' ', 'I', 't', '(', ')', '[', ']', '{', '}', '"', '*' -> 4;
            case 'f', 'k', '<', '>' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }
}
