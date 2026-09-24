package fr.kalium.bingo.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Assistant pour les menus en Dialog natif Minecraft (aucun inventaire/coffre) - meme principe
 * que Gui.java cote KalGames. Introduit en 0.1.9 pour remplacer l'ancien PartyMenu base sur un
 * inventaire Bukkit (demande explicite de l'utilisateur : "je préfère que la nether star ouvre
 * un menu plutôt qu'une page type inventaire").
 *
 * Volontairement plus simple que la version KalGames : pas de systeme de traduction multi-langue
 * (KalBingo n'en a pas, les messages restent en dur comme partout ailleurs dans ce plugin), pas
 * de champs de saisie (PartyMenu n'en a pas besoin) - a etoffer si un besoin precis apparait
 * (regle du projet : ne pas construire au-dela de ce qui est demande).
 */
public final class DialogGui {

    public interface Click {
        void run(Player player);
    }

    private final JavaPlugin plugin;

    public DialogGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Raccourci MiniMessage (memes tags que cote KalGames : <gold>, <gray>, etc.). */
    public static Component text(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

    private ClickCallback.Options options() {
        return ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build();
    }

    public ActionButton button(Component label, Component tooltip, Click click) {
        return ActionButton.create(label, tooltip, 240,
                DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player clicker) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            try {
                                click.run(clicker);
                            } catch (Throwable t) {
                                plugin.getLogger().warning("[KalBingo] Erreur de menu : " + t);
                            }
                        });
                    }
                }, options()));
    }

    public ActionButton close(Component label) {
        return ActionButton.create(label, null, 240, null);
    }

    public ActionButton close() {
        return close(text("<red>Fermer"));
    }

    /** Affiche un menu. Les boutons sont disposes en colonnes ; fermeture via le bouton "Fermer". */
    public void open(Player player, Component title, List<Component> body, List<ActionButton> buttons, int columns) {
        List<DialogBody> bodies = new ArrayList<>();
        for (Component line : body) {
            bodies.add(DialogBody.plainMessage(line, 320));
        }
        DialogBase base = DialogBase.builder(title).body(bodies).build();
        ActionButton exit = close();
        Dialog dialog = buttons.isEmpty()
                ? Dialog.create(factory -> factory.empty().base(base).type(DialogType.notice(exit)))
                : Dialog.create(factory -> factory.empty().base(base).type(DialogType.multiAction(buttons, exit, Math.max(1, columns))));
        player.showDialog(dialog);
    }
}
