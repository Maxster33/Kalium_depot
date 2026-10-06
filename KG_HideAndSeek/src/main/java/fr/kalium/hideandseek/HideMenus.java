package fr.kalium.hideandseek;

import fr.kalium.games.KalGames;
import fr.kalium.games.gui.Gui;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.util.Items;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Menus du Hide and Seek : choix du bloc et soundboard (hiders), blocs de chaque map et liste des sons (moderateurs).
 * Textes dans le lang.yml de KalGames (cles « hns.* »).
 */
public final class HideMenus {

    private static final Pattern SOUND_KEY = Pattern.compile("[a-z0-9_.:-]{3,80}");

    private final KGHideAndSeek hns;
    private final KalGames plugin;
    private final Gui gui;

    public HideMenus(KGHideAndSeek hns, KalGames plugin) {
        this.hns = hns;
        this.plugin = plugin;
        this.gui = plugin.gui();
    }

    private Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

    /** Nom du bloc dans la langue du joueur. */
    private static Component blockName(Material material) {
        return Component.translatable(material.translationKey());
    }

    // ------------------------------------------------------------------ hiders : choix du bloc

    public void openBlocks(Player player, HideInstance game) {
        HideInstance.Hider hider = game.hider(player.getUniqueId());
        if (hider == null) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (Material material : game.pool()) {
            Component label = blockName(material).color(NamedTextColor.WHITE);
            if (material == hider.block()) {
                label = t("hns.selected", "<green>✔ ").append(label);
            }
            buttons.add(gui.button(label, null, p -> {
                Component refusal = game.changeBlock(p, material);
                if (refusal != null) {
                    p.sendMessage(plugin.prefix().append(refusal));
                    return;
                }
                p.sendMessage(plugin.prefix().append(t("hns.block-chosen", "<gray>Vous imitez : <white><block></white>",
                        "block", blockName(material))));
            }));
        }
        List<Component> body = new ArrayList<>();
        body.add(t("hns.blocks-body", "<gray>Choisissez le bloc du décor que vous imitez."));
        int wait = game.blockWait(hider);
        if (wait > 0) {
            body.add(t("hns.blocks-wait", "<red>Prochain changement possible dans <s> s.", "s", wait));
        }
        gui.open(player, t("hns.blocks-title", "<aqua><bold>Votre bloc"), body, List.of(), buttons, gui.close(), 2);
    }

    // ------------------------------------------------------------------ hiders : soundboard

    public void openSounds(Player player, HideInstance game) {
        if (game.hider(player.getUniqueId()) == null) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (SoundBoard.Entry entry : hns.sounds().all()) {
            buttons.add(gui.button(Component.text(entry.label(), NamedTextColor.YELLOW), null, p -> game.playBoard(p, entry.key())));
        }
        buttons.add(gui.button(t("hns.favorites-button", "<gold>Mes sons favoris"), null, p -> openFavorites(p, game)));
        gui.open(player, t("hns.sounds-title", "<gold><bold>Soundboard"),
                List.of(t("hns.sounds-body", "<gray>Le son est joué là où vous êtes : tout le monde l'entend.")),
                List.of(), buttons, gui.close(), 2);
    }

    /** Les 6 sons de la barre d'objets, gardes d'une partie a l'autre. */
    public void openFavorites(Player player, HideInstance game) {
        List<SoundBoard.Entry> all = hns.sounds().all();
        if (all.isEmpty()) {
            return;
        }
        List<String> ids = new ArrayList<>();
        List<Component> labels = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            ids.add(String.valueOf(i));
            labels.add(Component.text(all.get(i).label()));
        }
        List<SoundBoard.Entry> current = hns.sounds().favoritesOf(player.getUniqueId());
        List<DialogInput> inputs = new ArrayList<>();
        int slots = Math.min(SoundBoard.FAVORITES, all.size());
        for (int slot = 0; slot < slots; slot++) {
            int selected = slot < current.size() ? all.indexOf(current.get(slot)) : slot;
            inputs.add(gui.choice("fav" + slot, t("hns.favorites-slot", "Case <n>", "n", slot + 1), ids, labels,
                    String.valueOf(Math.max(0, selected))));
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("hns.save", "<green>Enregistrer"), null, (p, view) -> {
            List<String> keys = new ArrayList<>();
            for (int slot = 0; slot < slots; slot++) {
                String text = view.getText("fav" + slot);
                try {
                    int index = Integer.parseInt(text == null ? "" : text.trim());
                    if (index >= 0 && index < all.size() && !keys.contains(all.get(index).key())) {
                        keys.add(all.get(index).key());
                    }
                } catch (NumberFormatException ignored) {
                    // case laissee telle quelle
                }
            }
            hns.sounds().setFavorites(p.getUniqueId(), keys);
            HideInstance.Hider hider = game.hider(p.getUniqueId());
            if (hider != null) {
                game.giveHotbar(p, hider);
            }
            p.sendMessage(plugin.prefix().append(t("hns.favorites-saved", "<green>Sons favoris enregistrés.")));
        }));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, p -> openSounds(p, game)));
        gui.open(player, t("hns.favorites-title", "<gold><bold>Sons favoris"),
                List.of(t("hns.favorites-body", "<gray>Les sons de votre barre d'objets, gardés d'une partie à l'autre.")),
                inputs, buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ moderateurs

    private boolean guard(Player player) {
        if (!plugin.isAdmin(player)) {
            plugin.tell(player, "admin.denied", "<red>Réservé aux modérateurs.");
            return false;
        }
        return true;
    }

    private ActionButton btn(Component label, Component tip, Gui.Click click) {
        return gui.button(label, tip, p -> {
            if (guard(p)) {
                click.run(p);
            }
        });
    }

    /** « Blocs des maps » : bouton de la page du jeu dans Informations &gt; Parametres. */
    public void openMaps(Player player, Minigame minigame) {
        if (!guard(player)) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (Arena arena : plugin.repository().arenasOf(minigame.id())) {
            Component label = plugin.lang().parse(arena.display())
                    .append(t("hns.admin-map-count", " <dark_gray>(<n>)", "n", hns.blocks().of(arena.id()).size()));
            buttons.add(btn(label, null, p -> openMapBlocks(p, minigame, arena)));
        }
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, p -> plugin.admin().openMinigame(p, minigame)));
        gui.open(player, t("hns.admin-maps-title", "<aqua><bold>Blocs des maps"),
                List.of(t("hns.admin-maps-body", "<gray>Choisissez une map pour régler les blocs que les hiders peuvent imiter. Une map sans bloc n'est pas jouable.")),
                List.of(), buttons, gui.close(), 1);
    }

    private void openMapBlocks(Player player, Minigame minigame, Arena arena) {
        if (!guard(player)) {
            return;
        }
        List<Material> list = hns.blocks().of(arena.id());
        List<Component> body = new ArrayList<>();
        body.add(t("hns.admin-blocks-body",
                "<gray>Blocs imités par les hiders ; un seeker qui frappe un vrai bloc de cette liste perd un demi-cœur. Acceptés : blocs d'une case sur lesquels on bute (enclume, composteur, pot décoratif...). Refusés : dalles, escaliers, barrières, murets, vitres, portes, lits, blocs traversables."));
        if (list.isEmpty()) {
            body.add(t("hns.admin-blocks-none", "<red>Aucun bloc : la map n'est pas jouable."));
        } else {
            List<Component> names = new ArrayList<>();
            for (Material material : list) {
                names.add(blockName(material).color(NamedTextColor.WHITE));
            }
            body.add(Component.join(JoinConfiguration.commas(true), names));
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("hns.admin-add-looked", "<green>+ Ajouter le bloc regardé"), null, p -> {
            Block target = p.getTargetBlockExact(6);
            addBlock(p, arena, target == null ? null : target.getType());
            openMapBlocks(p, minigame, arena);
        }));
        buttons.add(btn(t("hns.admin-add-held", "<green>+ Ajouter le bloc en main"), null, p -> {
            addBlock(p, arena, p.getInventory().getItemInMainHand().getType());
            openMapBlocks(p, minigame, arena);
        }));
        for (Material material : new ArrayList<>(list)) {
            buttons.add(btn(t("hns.admin-remove", "<red>✖ <name>", "name", blockName(material)), null, p -> {
                hns.blocks().remove(arena.id(), material);
                openMapBlocks(p, minigame, arena);
            }));
        }
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, p -> openMaps(p, minigame)));
        gui.open(player, plugin.lang().parse(arena.display()), body, List.of(), buttons, gui.close(), 2);
    }

    private void addBlock(Player player, Arena arena, Material material) {
        MapBlocks.Refusal refusal = MapBlocks.refusal(material, player.getLocation());
        if (refusal != null) {
            switch (refusal) {
                case NOT_BLOCK -> plugin.tell(player, "hns.admin-refused-none", "<red>Aucun bloc : regardez un bloc de près, ou tenez-en un en main.");
                case NO_ITEM -> plugin.tell(player, "hns.admin-refused-item", "<red>Ce bloc n'existe pas en objet : un hider ne peut pas le porter sur la tête.");
                case FAMILY -> plugin.tell(player, "hns.admin-refused-family", "<red>Bloc refusé : dalles, escaliers, barrières, murets, vitres, portes et lits ne sont pas acceptés.");
                default -> plugin.tell(player, "hns.admin-refused-passable", "<red>Bloc refusé : on passe au travers (fleur, herbe, torche...).");
            }
            return;
        }
        if (!hns.blocks().add(arena.id(), material)) {
            plugin.tell(player, "hns.admin-block-known", "<yellow>Ce bloc est déjà dans la liste.");
        }
    }

    /** « Sons du soundboard » : liste commune a toutes les maps. */
    public void openSoundsAdmin(Player player, Minigame minigame) {
        if (!guard(player)) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("hns.admin-sound-add", "<green>+ Ajouter un son"), null, p -> openNewSound(p, minigame)));
        for (SoundBoard.Entry entry : new ArrayList<>(hns.sounds().all())) {
            buttons.add(btn(t("hns.admin-remove", "<red>✖ <name>", "name", entry.label()),
                    Component.text(entry.key(), NamedTextColor.GRAY), p -> {
                        hns.sounds().remove(entry.key());
                        openSoundsAdmin(p, minigame);
                    }));
        }
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, p -> plugin.admin().openMinigame(p, minigame)));
        gui.open(player, t("hns.admin-sounds-title", "<aqua><bold>Sons du soundboard"),
                List.of(t("hns.admin-sounds-body", "<gray><n> son(s), communs à toutes les maps. Cliquez sur un son pour le retirer.",
                        "n", hns.sounds().all().size())),
                List.of(), buttons, gui.close(), 2);
    }

    private void openNewSound(Player player, Minigame minigame) {
        List<DialogInput> inputs = List.of(
                gui.text("key", t("hns.admin-sound-key", "Son Minecraft"), "entity.wolf.ambient", 80),
                gui.text("label", t("hns.admin-sound-label", "Nom affiché"), "Loup", 24),
                gui.text("icon", t("hns.admin-sound-icon", "Objet de l'icône"), "BONE", 40));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("hns.admin-sound-create", "<green>Ajouter le son"), null, (p, view) -> {
            if (!guard(p)) {
                return;
            }
            String key = view.getText("key") == null ? "" : view.getText("key").trim().toLowerCase(Locale.ROOT);
            String label = view.getText("label") == null ? "" : view.getText("label").trim();
            if (!SOUND_KEY.matcher(key).matches() || label.isEmpty()) {
                plugin.tell(p, "hns.admin-sound-bad", "<red>Son invalide : il faut un identifiant Minecraft (ex. entity.wolf.ambient) et un nom.");
                return;
            }
            if (hns.sounds().get(key) != null) {
                plugin.tell(p, "hns.admin-sound-known", "<yellow>Ce son est déjà dans la liste.");
                return;
            }
            hns.sounds().add(new SoundBoard.Entry(key, label, Items.material(view.getText("icon"), Material.NOTE_BLOCK)));
            // Le moderateur entend le son tout de suite : s'il n'entend rien, l'identifiant est faux.
            p.playSound(p.getLocation(), key, SoundCategory.MASTER, 1f, 1f);
            plugin.tell(p, "hns.admin-sound-added", "<green>Son ajouté. <gray>Si vous n'avez rien entendu, son identifiant est faux : retirez-le.");
            openSoundsAdmin(p, minigame);
        }));
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, p -> openSoundsAdmin(p, minigame)));
        gui.open(player, t("hns.admin-sound-new", "<green><bold>Nouveau son"),
                List.of(t("hns.admin-sound-help", "<gray>Son Minecraft : son identifiant en anglais (ex. entity.wolf.ambient). Objet de l'icône : nom anglais d'un objet (ex. BONE).")),
                inputs, buttons, gui.close(), 1);
    }
}
