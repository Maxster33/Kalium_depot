package fr.kalium.menu.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Assistant pour les menus (Dialog natif de Minecraft, aucun coffre) - boite a outils commune de KLM_Menu (2.0.0),
 * reprise telle quelle de KalGames. Chaque plugin cree le sien avec son propre Lang (ses textes) : largeur des boutons
 * lue dans sa config (gui.button-width, 240 par defaut).
 */
public final class Gui {

    /** Action d'un bouton. */
    public interface Click {
        void run(Player player);
    }

    /** Action d'un bouton avec acces aux champs du formulaire. */
    public interface FormClick {
        void run(Player player, DialogResponseView view);
    }

    /**
     * Minecraft n'accepte comme nom de champ que des lettres, chiffres et "_" (pas de "-"), et Paper reserve
     * la cle "id" pour identifier le rappel : on prefixe donc toutes les cles et on les traduit a la lecture.
     */
    static String wire(String key) {
        StringBuilder out = new StringBuilder("f_");
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            out.append(c < 128 && (Character.isLetterOrDigit(c) || c == '_') ? c : '_');
        }
        return out.toString();
    }

    /** Vue des reponses qui accepte les cles d'origine. */
    private record KeyedView(DialogResponseView delegate) implements DialogResponseView {
        @Override
        public net.kyori.adventure.nbt.api.BinaryTagHolder payload() {
            return delegate.payload();
        }

        @Override
        public String getText(String key) {
            return delegate.getText(wire(key));
        }

        @Override
        public Boolean getBoolean(String key) {
            return delegate.getBoolean(wire(key));
        }

        @Override
        public Float getFloat(String key) {
            return delegate.getFloat(wire(key));
        }
    }

    private final JavaPlugin plugin;
    private final Lang lang;

    public Gui(JavaPlugin plugin, Lang lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    private Component t(String key, String def) {
        return lang.c(key, def);
    }

    private void sync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private int width() {
        return Math.max(100, Math.min(400, plugin.getConfig().getInt("gui.button-width", 240)));
    }

    private ClickCallback.Options options() {
        return ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(30)).build();
    }

    // ------------------------------------------------------------------ boutons

    public ActionButton button(Component label, Component tooltip, Click click) {
        return form(label, tooltip, (player, view) -> click.run(player));
    }

    public ActionButton form(Component label, Component tooltip, FormClick click) {
        return ActionButton.create(label, tooltip, width(),
                DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player clicker) {
                        sync(() -> {
                            try {
                                click.run(clicker, new KeyedView(view));
                            } catch (Throwable t) {
                                plugin.getLogger().warning("Erreur de menu : " + t);
                                t.printStackTrace();
                            }
                        });
                    }
                }, options()));
    }

    /** Bouton qui ne fait que fermer le menu. */
    public ActionButton close(Component label) {
        return ActionButton.create(label, null, width(), null);
    }

    public ActionButton close() {
        return close(t("gui.close", "<red>Fermer"));
    }

    // ------------------------------------------------------------------ champs

    public DialogInput text(String key, Component label, String initial, int maxLength) {
        return DialogInput.text(wire(key), label).initial(initial == null ? "" : initial).maxLength(maxLength).width(260).build();
    }

    public DialogInput toggle(String key, Component label, boolean initial) {
        return DialogInput.bool(wire(key), label).initial(initial).build();
    }

    public DialogInput number(String key, Component label, float min, float max, float initial, float step) {
        return DialogInput.numberRange(wire(key), label, min, max)
                .initial(Math.max(min, Math.min(max, initial))).step(step).width(260).build();
    }

    /** Liste deroulante : ids et libelles, l'option initiale est celle dont l'id vaut selected. */
    public DialogInput choice(String key, Component label, List<String> ids, List<Component> labels, String selected) {
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        boolean any = false;
        for (int i = 0; i < ids.size(); i++) {
            boolean initial = ids.get(i).equals(selected);
            any |= initial;
            entries.add(SingleOptionDialogInput.OptionEntry.create(ids.get(i), labels.get(i), initial));
        }
        if (!any && !entries.isEmpty()) {
            SingleOptionDialogInput.OptionEntry first = entries.get(0);
            entries.set(0, SingleOptionDialogInput.OptionEntry.create(first.id(), first.display(), true));
        }
        return DialogInput.singleOption(wire(key), label, entries).width(260).build();
    }

    // ------------------------------------------------------------------ dialogues

    /**
     * Affiche un menu. Les boutons sont disposes en colonnes ; exit ferme le menu
     * (null : bouton "Fermer" par defaut).
     */
    public void open(Player player, Component title, List<Component> body, List<DialogInput> inputs,
                     List<ActionButton> buttons, ActionButton exit, int columns) {
        List<DialogBody> bodies = new ArrayList<>();
        for (Component line : body) {
            bodies.add(DialogBody.plainMessage(line, 320));
        }
        DialogBase base = DialogBase.builder(title)
                .body(bodies)
                .inputs(inputs)
                .build();
        ActionButton exitButton = exit == null ? close() : exit;
        Dialog dialog = buttons.isEmpty()
                ? Dialog.create(factory -> factory.empty().base(base).type(DialogType.notice(exitButton)))
                : Dialog.create(factory -> factory.empty()
                        .base(base)
                        .type(DialogType.multiAction(buttons, exitButton, Math.max(1, columns))));
        player.showDialog(dialog);
    }

    public void open(Player player, Component title, List<Component> body, List<ActionButton> buttons) {
        open(player, title, body, List.of(), buttons, null, 1);
    }

    /** Petit message avec un bouton pour continuer. */
    public void notice(Player player, Component title, Component message, Click after) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(button(t("gui.ok", "<green>OK"), null, after == null ? p -> { } : after));
        open(player, title, List.of(message), List.of(), buttons, close(), 1);
    }

    /** Confirmation oui / non. */
    public void confirm(Player player, Component title, Component message, Click yes, Click no) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(button(t("gui.yes", "<green>Confirmer"), null, yes));
        buttons.add(button(t("gui.no", "<red>Annuler"), null, no == null ? p -> { } : no));
        open(player, title, List.of(message), List.of(), buttons, close(t("gui.close", "<red>Fermer")), 2);
    }
}
