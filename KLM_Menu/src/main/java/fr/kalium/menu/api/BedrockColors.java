package fr.kalium.menu.api;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 2.4.0 - couleurs des menus lisibles sur Bedrock (demande de LeKiwi06, 28/09/2026 : "certaines couleurs se voient tres
 * mal sur Bedrock, notamment le gris clair, vert et vert clair, jaune pale : toutes les couleurs trop claires").
 *
 * Geyser affiche les menus (Dialog) dans des fenetres Bedrock a fond clair : une couleur claire y devient illisible,
 * alors qu'elle se lit bien sur le fond sombre de Java. Pour un joueur Bedrock seulement, chaque couleur trop claire
 * est donc assombrie (meme teinte, luminosite reduite) ; les joueurs Java voient les menus sans changement.
 * Le texte sans couleur (blanc par defaut sur Java) est laisse au client Bedrock, qui l'affiche deja en sombre.
 */
public final class BedrockColors {

    /**
     * Luminance relative (0 = noir, 1 = blanc) au-dela de laquelle une couleur est assombrie. 0,15 = contraste d'au
     * moins 3 contre 1 sur le gris clair des boutons Bedrock (luminance ~0,56).
     */
    private static final double MAX_LUMINANCE = 0.15;

    private BedrockColors() {
    }

    /** Joueur Bedrock (Geyser / Floodgate : identifiant qui commence par des zeros), comme dans les autres plugins. */
    public static boolean isBedrock(Player player) {
        return player != null && isBedrock(player.getUniqueId());
    }

    public static boolean isBedrock(UUID uuid) {
        return uuid != null && uuid.getMostSignificantBits() == 0;
    }

    // ------------------------------------------------------------------ couleurs

    private static double channel(int value) {
        double c = value / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double luminance(TextColor color) {
        return 0.2126 * channel(color.red()) + 0.7152 * channel(color.green()) + 0.0722 * channel(color.blue());
    }

    /** Couleur assombrie jusqu'a etre lisible sur fond clair (inchangee si elle l'est deja). */
    public static TextColor darken(TextColor color) {
        if (color == null || luminance(color) <= MAX_LUMINANCE) {
            return color;
        }
        double factor = 1.0;
        TextColor result = color;
        while (factor > 0.05 && luminance(result) > MAX_LUMINANCE) {
            factor -= 0.05;
            result = TextColor.color((int) Math.round(color.red() * factor), (int) Math.round(color.green() * factor),
                    (int) Math.round(color.blue() * factor));
        }
        // Couleur nommee la plus proche quand elle existe (rendu plus net sur Bedrock), sinon la couleur calculee.
        NamedTextColor named = NamedTextColor.namedColor(result.value());
        return named != null ? named : result;
    }

    /** Texte avec toutes ses couleurs trop claires assombries (enfants, info-bulles comprises). */
    public static Component adapt(Component component) {
        if (component == null) {
            return null;
        }
        Style style = component.style();
        TextColor color = style.color();
        Component result = component;
        if (color != null) {
            TextColor dark = darken(color);
            if (!dark.equals(color)) {
                result = result.color(dark);
            }
        }
        if (style.hoverEvent() != null && style.hoverEvent().value() instanceof Component hover) {
            result = result.hoverEvent(adapt(hover));
        }
        List<Component> children = component.children();
        if (!children.isEmpty()) {
            List<Component> adapted = new ArrayList<>(children.size());
            for (Component child : children) {
                adapted.add(adapt(child));
            }
            result = result.children(adapted);
        }
        return result;
    }

    // ------------------------------------------------------------------ elements d'un menu

    public static ActionButton adapt(ActionButton button) {
        if (button == null) {
            return null;
        }
        return ActionButton.create(adapt(button.label()), adapt(button.tooltip()), button.width(), button.action());
    }

    public static List<ActionButton> adaptButtons(List<ActionButton> buttons) {
        List<ActionButton> list = new ArrayList<>(buttons.size());
        for (ActionButton button : buttons) {
            list.add(adapt(button));
        }
        return list;
    }

    public static DialogBody adapt(DialogBody body) {
        if (body instanceof PlainMessageDialogBody plain) {
            return DialogBody.plainMessage(adapt(plain.contents()), plain.width());
        }
        return body;
    }

    public static DialogInput adapt(DialogInput input) {
        if (input instanceof TextDialogInput text) {
            return DialogInput.text(text.key(), text.width(), adapt(text.label()), text.labelVisible(), text.initial(),
                    text.maxLength(), text.multiline());
        }
        if (input instanceof BooleanDialogInput bool) {
            return DialogInput.bool(bool.key(), adapt(bool.label()), bool.initial(), bool.onTrue(), bool.onFalse());
        }
        if (input instanceof NumberRangeDialogInput number) {
            return DialogInput.numberRange(number.key(), number.width(), adapt(number.label()), number.labelFormat(),
                    number.start(), number.end(), number.initial(), number.step());
        }
        if (input instanceof SingleOptionDialogInput choice) {
            List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
            for (SingleOptionDialogInput.OptionEntry entry : choice.entries()) {
                entries.add(SingleOptionDialogInput.OptionEntry.create(entry.id(), adapt(entry.display()), entry.initial()));
            }
            return DialogInput.singleOption(choice.key(), choice.width(), entries, adapt(choice.label()), choice.labelVisible());
        }
        return input;
    }

    /** Titre, textes et champs d'un menu. */
    public static DialogBase adapt(DialogBase base) {
        List<DialogBody> bodies = new ArrayList<>();
        for (DialogBody body : base.body()) {
            bodies.add(adapt(body));
        }
        List<DialogInput> inputs = new ArrayList<>();
        for (DialogInput input : base.inputs()) {
            inputs.add(adapt(input));
        }
        return DialogBase.create(adapt(base.title()), adapt(base.externalTitle()), base.canCloseWithEscape(), base.pause(),
                base.afterAction(), bodies, inputs);
    }
}
