package fr.kalium.core.menu;

import fr.kalium.core.KaliumCore;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Briques communes pour construire les ecrans Dialog (menu principal, statistiques, parametres,
 * et les ecrans des modules comme Claims). Partagee par toutes les classes d'ecran pour eviter de
 * dupliquer la construction des boutons/dialogs a chaque nouveau module.
 */
public final class DialogHelper {

    private final KaliumCore plugin;

    public DialogHelper(KaliumCore plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ boutons

    /** Bouton simple : label/tooltip pris dans les messages (config.yml), action sans besoin des champs de saisie. */
    public ActionButton button(String labelMsg, String tooltipMsg, Consumer<Player> action) {
        return rawButton(plugin.mm().deserialize(plugin.msg(labelMsg)),
                tooltipMsg == null ? null : plugin.mm().deserialize(plugin.msg(tooltipMsg)),
                (view, player) -> action.accept(player));
    }

    /** Bouton avec Component deja construits (ex. label dynamique avec placeholder). */
    public ActionButton rawButton(Component label, Component tooltip, Consumer<Player> action) {
        return rawButton(label, tooltip, (view, player) -> action.accept(player));
    }

    /** Bouton qui a besoin de lire les champs de saisie du dialog (DialogResponseView) au clic. */
    public ActionButton inputButton(String labelMsg, String tooltipMsg, BiConsumer<DialogResponseView, Player> action) {
        return rawButton(plugin.mm().deserialize(plugin.msg(labelMsg)),
                tooltipMsg == null ? null : plugin.mm().deserialize(plugin.msg(tooltipMsg)),
                action);
    }

    public ActionButton rawButton(Component label, Component tooltip, BiConsumer<DialogResponseView, Player> action) {
        return ActionButton.create(
                label,
                tooltip,
                plugin.buttonWidth(),
                DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player clicker) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> action.accept(view, clicker));
                            }
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(Duration.ofMinutes(5))
                                .build()));
    }

    // ------------------------------------------------------------------ affichage

    public void show(Player player, String titleMsg, List<Component> body, List<ActionButton> buttons) {
        show(player, titleMsg, body, buttons, List.of());
    }

    public void show(Player player, String titleMsg, List<Component> body, List<ActionButton> buttons,
                      List<DialogInput> inputs) {
        ActionButton close = ActionButton.create(plugin.mm().deserialize(plugin.msg("close-button")), null,
                plugin.buttonWidth(), null);

        List<DialogBody> dialogBody = new ArrayList<>();
        for (Component line : body) {
            dialogBody.add(DialogBody.plainMessage(line));
        }

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(plugin.mm().deserialize(plugin.msg(titleMsg)))
                        .body(dialogBody)
                        .inputs(inputs)
                        .build())
                .type(DialogType.multiAction(buttons, close, 1)));

        player.showDialog(dialog);
    }
}
