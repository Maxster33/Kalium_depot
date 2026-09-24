package fr.kalium.games.gui;

import fr.kalium.games.KalGames;
import fr.kalium.games.data.Kit;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import fr.kalium.games.model.PointSpec;
import fr.kalium.games.model.Pos;
import fr.kalium.games.model.SettingSpec;
import fr.kalium.games.world.Template;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Menu "Parametres" (moderateurs) : mini-jeux, arenes, points, capture, kits, hub, parties en cours. */
public final class AdminMenus {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{2,24}");
    private static final int SLIDER_MAX_SPAN = 200;

    private record Corner(String world, int x, int y, int z) {
    }

    private final KalGames plugin;
    private final Gui gui;
    private final Map<UUID, Corner> corner1 = new HashMap<>();
    private final Map<UUID, Corner> corner2 = new HashMap<>();
    private final Set<String> capturing = new HashSet<>();

    public AdminMenus(KalGames plugin, Gui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    // ------------------------------------------------------------------ utilitaires

    private Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

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

    private ActionButton frm(Component label, Component tip, Gui.FormClick click) {
        return gui.form(label, tip, (p, view) -> {
            if (guard(p)) {
                click.run(p, view);
            }
        });
    }

    private ActionButton back(Gui.Click target) {
        return btn(t("menu.back", "<gray>Retour"), null, target);
    }

    private void say(Player player, String key, String def, Object... pairs) {
        plugin.tell(player, key, def, pairs);
    }

    private Component lines(List<Component> list) {
        return Component.join(JoinConfiguration.newlines(), list);
    }

    private static int parseInt(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Component onOff(boolean value) {
        return value ? t("admin.on", "<green>activé") : t("admin.off", "<red>désactivé");
    }

    private World creationWorld(Arena arena, Player player) {
        String[] parts = arena.suggestedArea().split(";");
        if (parts.length == 7) {
            World world = Bukkit.getWorld(parts[0]);
            if (world != null) {
                return world;
            }
        }
        return player.getWorld();
    }

    // ------------------------------------------------------------------ boutons ajoutes par d'autres plugins (1.13.0)

    /** Boutons de l'accueil des Parametres declares par d'autres plugins (voir MenuEntry), dans l'ordre d'ajout. */
    private final List<MenuEntry> settingsEntries = new ArrayList<>();

    /**
     * Ajoute (ou remplace, meme id) un bouton dans l'accueil des Parametres (moderateurs), apres "Hub de Kal-Games".
     * Le clic n'est execute que pour un moderateur (kalgames.admin).
     */
    public void addSettingsEntry(MenuEntry entry) {
        removeSettingsEntry(entry.id());
        settingsEntries.add(entry);
    }

    public void removeSettingsEntry(String id) {
        settingsEntries.removeIf(e -> e.id().equals(id));
    }

    // ------------------------------------------------------------------ accueil

    public void openHome(Player player) {
        if (!guard(player)) {
            return;
        }
        int ready = 0;
        for (Arena arena : plugin.repository().arenas()) {
            Minigame minigame = plugin.repository().minigame(arena.minigameId());
            if (minigame != null && arena.ready(minigame.type())) {
                ready++;
            }
        }
        List<Component> body = new ArrayList<>();
        body.add(t("admin.home-stats", "<gray>Mini-jeux : <white><mg></white> - Arènes prêtes : <white><ready>/<all></white> - Parties en cours : <white><games></white> (<slots>/<max> emplacements)",
                "mg", plugin.repository().minigames().size(), "ready", ready, "all", plugin.repository().arenas().size(),
                "games", plugin.instances().all().size(), "slots", plugin.worlds().usedSlots(), "max", plugin.worlds().maxSlots()));
        if (!plugin.worlds().ready()) {
            body.add(t("admin.home-noworld", "<red>Le monde des parties n'a pas pu être créé (voir la console)."));
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.home-minigames", "<gold>Mini-jeux"), t("admin.home-minigames-tip", "<gray>Ajouter, modifier, régler les mini-jeux."), this::openMinigames));
        buttons.add(btn(t("admin.home-arenas", "<green>Arènes"), t("admin.home-arenas-tip", "<gray>Points, capture de la zone, test."), p -> openArenas(p, null)));
        buttons.add(btn(t("admin.home-kits", "<aqua>Kits"), t("admin.home-kits-tip", "<gray>Créer depuis votre inventaire ou importer de PlayerKits2."), this::openKits));
        buttons.add(btn(t("admin.home-hub", "<yellow>Hub de Kal-Games"), t("admin.home-hub-tip", "<gray>Définir le point d'arrivée."), this::openHub));
        // 1.13.0 : boutons ajoutes par d'autres plugins (ex. "Bingo" par KG_Bingo), voir MenuEntry.
        for (MenuEntry entry : new ArrayList<>(settingsEntries)) {
            buttons.add(btn(entry.label().get(), entry.tooltip() == null ? null : entry.tooltip().get(), entry.click()));
        }
        buttons.add(btn(t("admin.home-games", "<light_purple>Parties en cours"), null, this::openInstances));
        buttons.add(btn(t("admin.home-reload", "<gray>Recharger la configuration"), null, p -> {
            plugin.reloadAll();
            say(p, "admin.reloaded", "<green>Configuration rechargée.");
            openHome(p);
        }));
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, plugin.menus()::openGames));
        gui.open(player, t("admin.home-title", "<light_purple><bold>Paramètres Kal-Games"), body, List.of(), buttons, null, 1);
    }

    // ------------------------------------------------------------------ hub

    private void openHub(Player player) {
        Location hub = plugin.hub().hubLocation();
        List<Component> body = List.of(t("admin.hub-body", "<gray>Point d'arrivée actuel : <white><w> <pos></white><newline><gray>Les joueurs y arrivent et y reviennent après une partie.",
                "w", hub.getWorld().getName(), "pos", String.format(Locale.ROOT, "%.1f, %.1f, %.1f", hub.getX(), hub.getY(), hub.getZ())));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.hub-set", "<green>Définir le hub ici"), null, p -> {
            plugin.hub().setHub(p.getLocation());
            say(p, "admin.hub-set-done", "<green>Le hub est maintenant votre position actuelle.");
            openHub(p);
        }));
        buttons.add(btn(t("admin.hub-tp", "<aqua>Aller au hub"), null, p -> p.teleport(plugin.hub().hubLocation())));
        buttons.add(back(this::openHome));
        gui.open(player, t("admin.hub-title", "<yellow><bold>Hub de Kal-Games"), body, List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ mini-jeux

    private void openMinigames(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        for (Minigame minigame : plugin.repository().minigames()) {
            Component label = plugin.lang().parse(minigame.display());
            Component tip = t("admin.mg-tip", "<gray><type> - <state>", "type", minigame.type().display(), "state", onOff(minigame.enabled()));
            buttons.add(btn(label, tip, p -> openMinigame(p, minigame)));
        }
        buttons.add(btn(t("admin.mg-new", "<green>+ Nouveau mini-jeu"), null, this::openNewMinigame));
        buttons.add(back(this::openHome));
        gui.open(player, t("admin.mg-title", "<gold><bold>Mini-jeux"),
                List.of(t("admin.mg-body", "<gray>Choisissez un mini-jeu à modifier ou ajoutez-en un.")), List.of(), buttons, gui.close(), 1);
    }

    private void openNewMinigame(Player player) {
        List<String> ids = new ArrayList<>();
        List<Component> labels = new ArrayList<>();
        for (MinigameType type : MinigameType.values()) {
            ids.add(type.name());
            labels.add(type.playable()
                    ? Component.text(type.display())
                    : t("admin.type-config-only", "<type> <gray>(configuration seule)", "type", type.display()));
        }
        List<DialogInput> inputs = List.of(
                gui.text("id", t("admin.new-id", "Identifiant (minuscules, sans espace)"), "", 24),
                gui.text("display", t("admin.new-display", "Nom affiché (MiniMessage accepté)"), "<yellow><bold>Nouveau jeu", 60),
                gui.choice("type", t("admin.new-type", "Type de mini-jeu"), ids, labels, MinigameType.PARKOUR.name()));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.new-create", "<green>Créer"), null, (p, view) -> {
            String id = view.getText("id") == null ? "" : view.getText("id").trim().toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                say(p, "admin.bad-id", "<red>Identifiant invalide : 2 à 24 caractères parmi a-z, 0-9, - et _.");
                return;
            }
            if (plugin.repository().minigame(id) != null) {
                say(p, "admin.id-taken", "<red>Cet identifiant existe déjà.");
                return;
            }
            MinigameType type;
            try {
                type = MinigameType.valueOf(view.getText("type"));
            } catch (Exception e) {
                say(p, "admin.bad-type", "<red>Type invalide.");
                return;
            }
            String display = view.getText("display") == null || view.getText("display").isBlank() ? id : view.getText("display");
            Minigame minigame = plugin.repository().createMinigame(id, display, type);
            minigame.description(type.description());
            if (type == MinigameType.PVP_KIT) {
                for (Kit kit : plugin.kits().all()) {
                    minigame.kits().add(kit.id());
                }
            }
            plugin.repository().save();
            say(p, "admin.mg-created", "<green>Mini-jeu créé. Ajoutez maintenant une arène.");
            openMinigame(p, minigame);
        }));
        buttons.add(back(this::openMinigames));
        gui.open(player, t("admin.new-title", "<green><bold>Nouveau mini-jeu"),
                List.of(t("admin.new-body", "<gray>Course de bateau, parcours, PvP Kit sont jouables. Hunger Games, Manhunt et Build Battle : la configuration est prête, le moteur arrive.")),
                inputs, buttons, gui.close(), 1);
    }

    public void openMinigame(Player player, Minigame minigame) {
        if (!guard(player)) {
            return;
        }
        MinigameType type = minigame.type();
        List<Arena> arenas = plugin.repository().arenasOf(minigame.id());
        int ready = 0;
        for (Arena arena : arenas) {
            if (arena.ready(type)) {
                ready++;
            }
        }
        List<Component> body = new ArrayList<>();
        body.add(t("admin.mg-type", "<gray>Type : <white><type></white>", "type", type.display()));
        body.add(t("admin.mg-status", "<gray>Statut : <state> <dark_gray>| <gray>Publique : <pub> <dark_gray>| <gray>Privée : <priv>",
                "state", onOff(minigame.enabled()), "pub", onOff(minigame.publicEnabled()), "priv", onOff(minigame.privateEnabled())));
        body.add(t("admin.mg-arenas", "<gray>Arènes prêtes : <white><ready>/<all></white>", "ready", ready, "all", arenas.size()));
        if (!type.playable()) {
            body.add(t("admin.mg-noengine", "<yellow>Moteur de jeu à venir : vous pouvez déjà tout configurer, mais le mini-jeu n'est pas encore jouable."));
        }
        if (type == MinigameType.PVP_KIT) {
            body.add(t("admin.mg-kits", "<gray>Kits proposés au vote : <white><n></white>", "n", minigame.kits().size()));
        }

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.mg-rename", "<white>Nom et description"), null, p -> openMinigameInfo(p, minigame)));
        buttons.add(btn(t("admin.mg-toggle-enabled", "Mini-jeu : <state>", "state", onOff(minigame.enabled())), null, p -> {
            minigame.enabled(!minigame.enabled());
            plugin.repository().save();
            openMinigame(p, minigame);
        }));
        buttons.add(btn(t("admin.mg-toggle-public", "Parties publiques : <state>", "state", onOff(minigame.publicEnabled())), null, p -> {
            minigame.publicEnabled(!minigame.publicEnabled());
            plugin.repository().save();
            openMinigame(p, minigame);
        }));
        buttons.add(btn(t("admin.mg-toggle-private", "Parties privées : <state>", "state", onOff(minigame.privateEnabled())), null, p -> {
            minigame.privateEnabled(!minigame.privateEnabled());
            plugin.repository().save();
            openMinigame(p, minigame);
        }));
        buttons.add(btn(t("admin.mg-settings", "<gold>Réglages"), null, p -> openSettings(p, minigame)));
        if (type == MinigameType.PVP_KIT) {
            buttons.add(btn(t("admin.mg-kit-pick", "<aqua>Kits du mini-jeu"), null, p -> openMinigameKits(p, minigame)));
        }
        buttons.add(btn(t("admin.mg-arena-list", "<green>Arènes (<n>)", "n", arenas.size()), null, p -> openArenas(p, minigame)));
        buttons.add(btn(t("admin.mg-rankings", "<light_purple>Classements (général, mois, archives, panneaux du hub)"), null,
                p -> plugin.ranking().openAdmin(p, minigame.id(), q -> plugin.admin().openMinigame(q, minigame))));
        buttons.add(btn(t("admin.mg-delete", "<dark_red>Supprimer le mini-jeu"), null, p -> gui.confirm(p,
                t("admin.mg-delete-title", "<dark_red>Supprimer ?"),
                t("admin.mg-delete-body", "<gray>Le mini-jeu <white><name></white> et toutes ses arènes seront supprimés.", "name", plugin.lang().parse(minigame.display())),
                q -> {
                    if (!guard(q)) {
                        return;
                    }
                    plugin.instances().closeAllOf(minigame.id(), t("admin.mg-removed", "<yellow>Ce mini-jeu a été supprimé."));
                    for (Arena arena : plugin.repository().arenasOf(minigame.id())) {
                        plugin.templates().delete(arena.id());
                    }
                    plugin.ranking().removeBoardsOf(minigame.id());
                    plugin.repository().deleteMinigame(minigame.id());
                    say(q, "admin.mg-deleted", "<green>Mini-jeu supprimé.");
                    openMinigames(q);
                }, this::openHome)));
        buttons.add(back(this::openMinigames));
        gui.open(player, plugin.lang().parse(minigame.display()), body, List.of(), buttons, gui.close(), 1);
    }

    private void openMinigameInfo(Player player, Minigame minigame) {
        List<DialogInput> inputs = List.of(
                gui.text("display", t("admin.info-display", "Nom affiché (MiniMessage)"), minigame.display(), 80),
                gui.text("description", t("admin.info-description", "Description"), minigame.description(), 200));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.save", "<green>Enregistrer"), null, (p, view) -> {
            String display = view.getText("display");
            if (display != null && !display.isBlank()) {
                minigame.display(display);
            }
            minigame.description(view.getText("description"));
            plugin.repository().save();
            say(p, "admin.saved", "<green>Enregistré.");
            openMinigame(p, minigame);
        }));
        buttons.add(back(p -> openMinigame(p, minigame)));
        gui.open(player, t("admin.info-title", "<white><bold>Nom et description"),
                List.of(t("admin.info-body", "<gray>Exemples de format : <yellow>&lt;red&gt;&lt;bold&gt;PvP Kit</yellow>")), inputs, buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ reglages

    private void openSettings(Player player, Minigame minigame) {
        List<DialogInput> inputs = new ArrayList<>();
        List<Component> body = new ArrayList<>();
        body.add(t("admin.settings-body", "<gray>Chaque réglage a une valeur par défaut. Modifiez puis enregistrez."));
        if (!minigame.type().playable()) {
            body.add(t("admin.mg-noengine", "<yellow>Moteur de jeu à venir : vous pouvez déjà tout configurer, mais le mini-jeu n'est pas encore jouable."));
        }
        List<SettingSpec> specs = minigame.type().settings();
        for (SettingSpec spec : specs) {
            Component label = spec.help().isBlank()
                    ? Component.text(spec.label())
                    : Component.text(spec.label() + " - " + spec.help());
            String key = "s_" + spec.key();
            switch (spec.kind()) {
                case BOOL -> inputs.add(gui.toggle(key, label, minigame.getBool(spec.key(), (Boolean) spec.def())));
                case TEXT -> inputs.add(gui.text(key, label, minigame.getText(spec.key(), (String) spec.def()), 200));
                case INT -> {
                    int current = minigame.getInt(spec.key(), ((Number) spec.def()).intValue());
                    if (spec.max() - spec.min() <= SLIDER_MAX_SPAN) {
                        inputs.add(gui.number(key, label, (float) spec.min(), (float) spec.max(), current, 1));
                    } else {
                        inputs.add(gui.text(key, Component.text(spec.label() + " (" + (int) spec.min() + " à " + (int) spec.max() + ")"),
                                String.valueOf(current), 6));
                    }
                }
            }
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.save", "<green>Enregistrer"), null, (p, view) -> {
            for (SettingSpec spec : specs) {
                String key = "s_" + spec.key();
                switch (spec.kind()) {
                    case BOOL -> {
                        Boolean value = view.getBoolean(key);
                        if (value != null) {
                            minigame.settings().put(spec.key(), value);
                        }
                    }
                    case TEXT -> {
                        String value = view.getText(key);
                        if (value == null) {
                            break;
                        }
                        if (spec.key().equals("boat-type")) {
                            try {
                                EntityType.valueOf(value.trim().toUpperCase(Locale.ROOT));
                            } catch (IllegalArgumentException e) {
                                say(p, "admin.bad-boat", "<red>Type de bateau inconnu : <value>", "value", value);
                                break;
                            }
                            value = value.trim().toUpperCase(Locale.ROOT);
                        }
                        minigame.settings().put(spec.key(), value);
                    }
                    case INT -> {
                        Float slider = spec.max() - spec.min() <= SLIDER_MAX_SPAN ? view.getFloat(key) : null;
                        int value = slider != null ? Math.round(slider)
                                : parseInt(view.getText(key), minigame.getInt(spec.key(), ((Number) spec.def()).intValue()));
                        value = (int) Math.max(spec.min(), Math.min(spec.max(), value));
                        minigame.settings().put(spec.key(), value);
                    }
                }
            }
            plugin.repository().save();
            say(p, "admin.saved", "<green>Enregistré.");
            openMinigame(p, minigame);
        }));
        buttons.add(back(p -> openMinigame(p, minigame)));
        gui.open(player, t("admin.settings-title", "<gold><bold>Réglages"), body, inputs, buttons, gui.close(), 1);
    }

    private void openMinigameKits(Player player, Minigame minigame) {
        List<DialogInput> inputs = new ArrayList<>();
        for (Kit kit : plugin.kits().all()) {
            inputs.add(gui.toggle("k_" + kit.id(), plugin.lang().parse(kit.display()), minigame.kits().contains(kit.id())));
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.save", "<green>Enregistrer"), null, (p, view) -> {
            minigame.kits().clear();
            for (Kit kit : plugin.kits().all()) {
                Boolean value = view.getBoolean("k_" + kit.id());
                if (value != null && value) {
                    minigame.kits().add(kit.id());
                }
            }
            plugin.repository().save();
            say(p, "admin.saved", "<green>Enregistré.");
            openMinigame(p, minigame);
        }));
        buttons.add(btn(t("admin.kits-manage", "<aqua>Gérer les kits"), null, this::openKits));
        buttons.add(back(p -> openMinigame(p, minigame)));
        gui.open(player, t("admin.mgkits-title", "<aqua><bold>Kits du mini-jeu"),
                List.of(t("admin.mgkits-body", "<gray>Cochez les kits proposés au vote.")), inputs, buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ kits

    private boolean lockedItem(ItemStack item) {
        if (plugin.items().isOurs(item)) {
            return true;
        }
        // Boussole de KLM_Menu (etiquette "klm_menu:") ou de l'ancien KaliumMenu ("kaliummenu:") - 1.15.1.
        if (!item.hasItemMeta()) {
            return false;
        }
        var data = item.getItemMeta().getPersistentDataContainer();
        return data.has(new NamespacedKey("klm_menu", "menu_compass"), PersistentDataType.BYTE)
                || data.has(new NamespacedKey("kaliummenu", "menu_compass"), PersistentDataType.BYTE);
    }

    private void openKits(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        for (Kit kit : plugin.kits().all()) {
            Component tip = lines(plugin.kits().summary(kit, 12));
            buttons.add(btn(plugin.lang().parse(kit.display()), tip, p -> openKit(p, kit)));
        }
        buttons.add(btn(t("admin.kits-create", "<green>+ Créer depuis mon inventaire"), null, this::openNewKit));
        buttons.add(btn(t("admin.kits-import", "<aqua>+ Importer depuis PlayerKits2"), null, this::openImportKits));
        buttons.add(back(this::openHome));
        gui.open(player, t("admin.kits-title", "<aqua><bold>Kits"),
                List.of(t("admin.kits-body", "<gray><n> kit(s). Survolez un kit pour voir son contenu.", "n", plugin.kits().all().size())),
                List.of(), buttons, gui.close(), 1);
    }

    private void openKit(Player player, Kit kit) {
        List<Component> body = new ArrayList<>();
        body.add(t("admin.kit-source", "<gray>Source : <white><source>", "source", kit.source()));
        body.addAll(plugin.kits().summary(kit, 20));
        List<ActionButton> buttons = new ArrayList<>();
        if (kit.source().equals(Kit.SOURCE_INVENTORY)) {
            buttons.add(btn(t("admin.kit-update", "<yellow>Remplacer par mon inventaire actuel"), null, p -> {
                String id = kit.id();
                String display = kit.display();
                plugin.kits().createFromInventory(id, display, p, this::lockedItem);
                say(p, "admin.kit-updated", "<green>Kit mis à jour.");
                openKits(p);
            }));
        }
        buttons.add(btn(t("admin.kit-delete", "<dark_red>Supprimer ce kit"), null, p -> {
            plugin.kits().delete(kit.id());
            for (Minigame minigame : plugin.repository().minigames()) {
                minigame.kits().remove(kit.id());
            }
            plugin.repository().save();
            say(p, "admin.kit-deleted", "<green>Kit supprimé.");
            openKits(p);
        }));
        buttons.add(back(this::openKits));
        gui.open(player, plugin.lang().parse(kit.display()), body, List.of(), buttons, gui.close(), 1);
    }

    private void openNewKit(Player player) {
        List<DialogInput> inputs = List.of(
                gui.text("id", t("admin.kit-id", "Identifiant du kit"), "", 24),
                gui.text("display", t("admin.kit-display", "Nom affiché (MiniMessage)"), "<yellow>Nouveau kit", 60));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.kit-create-confirm", "<green>Créer le kit"), null, (p, view) -> {
            String id = view.getText("id") == null ? "" : view.getText("id").trim().toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                say(p, "admin.bad-id", "<red>Identifiant invalide : 2 à 24 caractères parmi a-z, 0-9, - et _.");
                return;
            }
            if (plugin.kits().exists(id)) {
                say(p, "admin.id-taken", "<red>Cet identifiant existe déjà.");
                return;
            }
            Kit kit = plugin.kits().createFromInventory(id, view.getText("display") == null || view.getText("display").isBlank() ? id : view.getText("display"),
                    p, this::lockedItem);
            if (kit.slots().isEmpty() && kit.armor()[0] == null && kit.armor()[1] == null && kit.armor()[2] == null && kit.armor()[3] == null) {
                plugin.kits().delete(id);
                say(p, "admin.kit-empty", "<red>Votre inventaire est vide : rien à enregistrer.");
                return;
            }
            say(p, "admin.kit-created", "<green>Kit créé. Ajoutez-le aux mini-jeux voulus depuis \"Kits du mini-jeu\".");
            openKits(p);
        }));
        buttons.add(back(this::openKits));
        gui.open(player, t("admin.kit-new-title", "<green><bold>Nouveau kit"),
                List.of(t("admin.kit-new-body", "<gray>Préparez votre inventaire (armure, main secondaire, objets) puis validez : il est copié tel quel.")),
                inputs, buttons, gui.close(), 1);
    }

    private void openImportKits(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        for (String name : plugin.kits().pk2Available()) {
            boolean known = plugin.kits().exists(name);
            Component label = known
                    ? t("admin.pk2-known", "<white><name> <dark_gray>(déjà importé - recharger)", "name", name)
                    : Component.text(name);
            buttons.add(btn(label, null, p -> {
                plugin.kits().importPk2(name);
                say(p, "admin.pk2-done", "<green>Kit <name> importé.", "name", name);
                openKits(p);
            }));
        }
        buttons.add(back(this::openKits));
        Component body = buttons.size() == 1
                ? t("admin.pk2-none", "<red>Aucun kit trouvé dans <path>.", "path", plugin.kits().pk2Folder().getPath())
                : t("admin.pk2-body", "<gray>Choisissez un kit PlayerKits2 à copier.");
        gui.open(player, t("admin.pk2-title", "<aqua><bold>Importer de PlayerKits2"), List.of(body), List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ arenes

    private void openArenas(Player player, Minigame filter) {
        List<ActionButton> buttons = new ArrayList<>();
        int total = 0;
        for (Arena arena : plugin.repository().arenas()) {
            if (filter != null && !arena.minigameId().equals(filter.id())) {
                continue;
            }
            total++;
            Minigame owner = plugin.repository().minigame(arena.minigameId());
            boolean ready = owner != null && arena.ready(owner.type());
            Component label = Component.empty()
                    .append(ready ? t("admin.ready", "<green>✔ ") : t("admin.notready", "<yellow>⚠ "))
                    .append(plugin.lang().parse(arena.display()));
            if (filter == null && owner != null) {
                label = label.append(t("admin.arena-owner", " <dark_gray>(<name>)", "name", plugin.lang().parse(owner.display())));
            }
            Component tip = ready
                    ? t("admin.arena-ready-tip", "<green>Arène complète.")
                    : t("admin.arena-missing-tip", "<yellow>Manque : <list>", "list", owner == null ? "?" : String.join(", ", arena.missing(owner.type())));
            buttons.add(btn(label, tip, p -> openArena(p, arena)));
        }
        if (filter != null) {
            buttons.add(btn(t("admin.arena-new", "<green>+ Nouvelle arène"), null, p -> openNewArena(p, filter)));
            buttons.add(back(p -> openMinigame(p, filter)));
        } else {
            buttons.add(back(this::openHome));
        }
        Component body = total == 0
                ? t("admin.arenas-empty", "<gray>Aucune arène pour le moment.")
                : t("admin.arenas-body", "<gray>✔ = prête, ⚠ = éléments manquants (survolez pour le détail).");
        gui.open(player, t("admin.arenas-title", "<green><bold>Arènes"), List.of(body), List.of(), buttons, gui.close(), 1);
    }

    private void openNewArena(Player player, Minigame minigame) {
        List<DialogInput> inputs = List.of(
                gui.text("id", t("admin.arena-id", "Identifiant de l'arène"), "", 24),
                gui.text("display", t("admin.arena-display", "Nom affiché"), "Nouvelle arène", 60));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.arena-create", "<green>Créer"), null, (p, view) -> {
            String id = view.getText("id") == null ? "" : view.getText("id").trim().toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                say(p, "admin.bad-id", "<red>Identifiant invalide : 2 à 24 caractères parmi a-z, 0-9, - et _.");
                return;
            }
            if (plugin.repository().arena(id) != null) {
                say(p, "admin.id-taken", "<red>Cet identifiant existe déjà.");
                return;
            }
            String display = view.getText("display") == null || view.getText("display").isBlank() ? id : view.getText("display");
            Arena arena = plugin.repository().createArena(id, minigame.id(), display);
            say(p, "admin.arena-created", "<green>Arène créée. Définissez ses points puis capturez la zone.");
            openArena(p, arena);
        }));
        buttons.add(back(p -> openArenas(p, minigame)));
        gui.open(player, t("admin.arena-new-title", "<green><bold>Nouvelle arène"),
                List.of(t("admin.arena-new-body", "<gray>Construisez l'arène dans un monde (par exemple ici, dans Kal-Games), puis définissez ses points et capturez la zone.")),
                inputs, buttons, gui.close(), 1);
    }

    public void openArena(Player player, Arena arena) {
        if (!guard(player)) {
            return;
        }
        Minigame minigame = plugin.repository().minigame(arena.minigameId());
        if (minigame == null) {
            openArenas(player, null);
            return;
        }
        MinigameType type = minigame.type();
        List<Component> body = new ArrayList<>();
        body.add(t("admin.arena-mg", "<gray>Mini-jeu : <white><name>", "name", plugin.lang().parse(minigame.display())));
        Template template = arena.hasTemplate() ? plugin.templates().get(arena.id()) : null;
        if (template == null) {
            body.add(t("admin.arena-no-template", "<yellow>Modèle : aucun (capturez la zone)."));
        } else {
            body.add(t("admin.arena-template", "<gray>Modèle : <white><x>x<y>x<z></white> blocs (<n> blocs non vides)",
                    "x", template.sizeX(), "y", template.sizeY(), "z", template.sizeZ(), "n", template.blockCount()));
        }
        List<String> missing = arena.missing(type);
        if (missing.isEmpty()) {
            body.add(t("admin.arena-complete", "<green>Arène complète : elle est proposée aux joueurs."));
        } else {
            body.add(t("admin.arena-missing", "<yellow>Manque : <list>", "list", String.join(", ", missing)));
        }

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.arena-capture", "<gold>Zone de l'arène (capture)"),
                t("admin.arena-capture-tip", "<gray>Copie la construction : chaque partie en reçoit une copie."), p -> openCapture(p, arena)));
        List<String> groups = new ArrayList<>();
        for (PointSpec spec : type.points()) {
            if (spec.group() != null) {
                // 1.11.0 : points groupes (ex. une base Rush) -> un seul bouton ouvrant un sous-menu
                if (!groups.contains(spec.group())) {
                    groups.add(spec.group());
                    buttons.add(groupButton(arena, type, spec.group()));
                }
                continue;
            }
            buttons.add(pointButton(arena, spec));
        }
        for (MinigameType.ItemListSpec spec : type.itemLists()) {
            List<ItemStack> items = arena.itemLists().getOrDefault(spec.key(), List.of());
            buttons.add(btn(t("admin.arena-items", "<aqua><label> (<n> objets)", "label", spec.label(), "n", items.size()), null,
                    p -> openItemList(p, arena, spec)));
        }
        if (arena.ready(type) && type.playable()) {
            buttons.add(btn(t("admin.arena-test", "<green>Tester l'arène (partie privée)"), null, p -> {
                Map<String, Object> options = new HashMap<>();
                options.put("teams", 2);
                options.put("teamSize", 1);
                options.put("listed", false);
                startTest(p, minigame, arena, options);
            }));
        }
        buttons.add(btn(t("admin.arena-rename", "<white>Renommer"), null, p -> openRenameArena(p, arena)));
        buttons.add(btn(t("admin.arena-delete", "<dark_red>Supprimer l'arène"), null, p -> gui.confirm(p,
                t("admin.arena-delete-title", "<dark_red>Supprimer ?"),
                t("admin.arena-delete-body", "<gray>L'arène <white><name></white> et son modèle seront supprimés.", "name", plugin.lang().parse(arena.display())),
                q -> {
                    if (!guard(q)) {
                        return;
                    }
                    plugin.instances().closeAllOfArena(arena.id(), t("admin.arena-removed", "<yellow>Cette arène a été supprimée."));
                    plugin.templates().delete(arena.id());
                    plugin.repository().deleteArena(arena.id());
                    say(q, "admin.arena-deleted", "<green>Arène supprimée.");
                    openArenas(q, minigame);
                }, p2 -> openArena(p2, arena))));
        buttons.add(back(p -> openArenas(p, minigame)));
        gui.open(player, plugin.lang().parse(arena.display()), body, List.of(), buttons, gui.close(), 1);
    }

    private ActionButton pointButton(Arena arena, PointSpec spec) {
        boolean set = spec.list() ? !arena.list(spec.key()).isEmpty() : arena.point(spec.key()) != null;
        Component label = Component.empty()
                .append(set ? t("admin.ready", "<green>✔ ") : (spec.required() ? t("admin.missing", "<red>✘ ") : t("admin.optional", "<gray>○ ")))
                .append(Component.text(spec.label()));
        if (spec.list()) {
            label = label.append(Component.text(" (" + arena.list(spec.key()).size() + ")"));
        }
        return btn(label, spec.help().isBlank() ? null : Component.text(spec.help()), p -> openPoint(p, arena, spec));
    }

    /** Bouton d'un groupe de points (1.11.0) : ✔ tout est defini, ✘ un point obligatoire manque, ○ sinon. */
    private ActionButton groupButton(Arena arena, MinigameType type, String group) {
        int total = 0;
        int set = 0;
        boolean requiredMissing = false;
        for (PointSpec spec : type.points()) {
            if (!group.equals(spec.group())) {
                continue;
            }
            total++;
            boolean present = spec.list() ? !arena.list(spec.key()).isEmpty() : arena.point(spec.key()) != null;
            if (present) {
                set++;
            } else if (spec.required()) {
                requiredMissing = true;
            }
        }
        Component label = Component.empty()
                .append(set == total ? t("admin.ready", "<green>✔ ") : (requiredMissing ? t("admin.missing", "<red>✘ ") : t("admin.optional", "<gray>○ ")))
                .append(Component.text(group + " (" + set + "/" + total + ")"));
        return btn(label, null, p -> openPointGroup(p, arena, group));
    }

    private void openPointGroup(Player player, Arena arena, String group) {
        if (!guard(player)) {
            return;
        }
        Minigame minigame = plugin.repository().minigame(arena.minigameId());
        if (minigame == null) {
            openArenas(player, null);
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (PointSpec spec : minigame.type().points()) {
            if (group.equals(spec.group())) {
                buttons.add(pointButton(arena, spec));
            }
        }
        buttons.add(back(p -> openArena(p, arena)));
        gui.open(player, Component.text(group), List.of(), List.of(), buttons, gui.close(), 1);
    }

    private void startTest(Player p, Minigame minigame, Arena arena, Map<String, Object> options) {
        var result = plugin.instances().createPrivate(p, minigame, arena.id(), options);
        if (!result.ok()) {
            p.sendMessage(plugin.prefix().append(result.error()));
        } else {
            say(p, "admin.arena-test-started", "<green>Partie de test créée (code <code>).", "code", result.instance().code());
        }
    }

    private void openRenameArena(Player player, Arena arena) {
        List<DialogInput> inputs = List.of(gui.text("display", t("admin.arena-display", "Nom affiché"), arena.display(), 60));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.save", "<green>Enregistrer"), null, (p, view) -> {
            String display = view.getText("display");
            if (display != null && !display.isBlank()) {
                arena.display(display);
                plugin.repository().save();
            }
            openArena(p, arena);
        }));
        buttons.add(back(p -> openArena(p, arena)));
        gui.open(player, t("admin.arena-rename-title", "<white><bold>Renommer l'arène"), List.of(), inputs, buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ points d'arene

    private void openPoint(Player player, Arena arena, PointSpec spec) {
        List<Component> body = new ArrayList<>();
        if (!spec.help().isBlank()) {
            body.add(Component.text(spec.help()));
        }
        World world = creationWorld(arena, player);
        List<ActionButton> buttons = new ArrayList<>();
        if (!spec.list()) {
            Pos current = arena.point(spec.key());
            body.add(current == null
                    ? t("admin.point-unset", "<gray>Valeur actuelle : <red>non définie")
                    : t("admin.point-set", "<gray>Valeur actuelle : <white><pos>", "pos", current.pretty()));
            buttons.add(btn(t("admin.point-here", "<green>Définir ici (ma position)"), null, p -> {
                setPoint(p, arena, spec, p.getLocation());
                openPoint(p, arena, spec);
            }));
            if (current != null) {
                buttons.add(btn(t("admin.point-goto", "<aqua>Y aller"), null, p -> p.teleport(current.at(world, 0, 0))));
                if (!spec.required()) {
                    buttons.add(btn(t("admin.point-clear", "<yellow>Effacer"), null, p -> {
                        arena.points().remove(spec.key());
                        plugin.repository().save();
                        openPoint(p, arena, spec);
                    }));
                }
            }
        } else {
            List<Pos> list = arena.list(spec.key());
            body.add(t("admin.point-count", "<gray>Positions enregistrées : <white><n>", "n", list.size()));
            buttons.add(btn(t("admin.point-add", "<green>Ajouter ici (ma position)"), null, p -> {
                arena.lists().computeIfAbsent(spec.key(), k -> new ArrayList<>()).add(Pos.of(p.getLocation()));
                plugin.repository().save();
                warnOutside(p, arena, p.getLocation());
                openPoint(p, arena, spec);
            }));
            if (!list.isEmpty()) {
                buttons.add(btn(t("admin.point-remove-last", "<yellow>Retirer la dernière"), null, p -> {
                    List<Pos> current = arena.lists().get(spec.key());
                    if (current != null && !current.isEmpty()) {
                        current.remove(current.size() - 1);
                        plugin.repository().save();
                    }
                    openPoint(p, arena, spec);
                }));
                buttons.add(btn(t("admin.point-clear-all", "<red>Tout effacer"), null, p -> {
                    arena.lists().remove(spec.key());
                    plugin.repository().save();
                    openPoint(p, arena, spec);
                }));
                for (int i = 0; i < list.size() && i < 24; i++) {
                    Pos pos = list.get(i);
                    buttons.add(btn(t("admin.point-goto-n", "<aqua>Aller au n°<n> <dark_gray>(<pos>)", "n", i + 1, "pos", pos.pretty()), null,
                            p -> p.teleport(pos.at(world, 0, 0))));
                }
            }
        }
        buttons.add(back(p -> {
            if (spec.group() != null) {
                openPointGroup(p, arena, spec.group());
            } else {
                openArena(p, arena);
            }
        }));
        gui.open(player, Component.text(spec.label()), body, List.of(), buttons, gui.close(), 1);
    }

    private void setPoint(Player player, Arena arena, PointSpec spec, Location location) {
        arena.points().put(spec.key(), Pos.of(location));
        plugin.repository().save();
        say(player, "admin.point-saved", "<green>Point enregistré : <pos>", "pos", Pos.of(location).pretty());
        warnOutside(player, arena, location);
    }

    private void warnOutside(Player player, Arena arena, Location location) {
        if (!arena.hasTemplate()) {
            return;
        }
        Template template = plugin.templates().get(arena.id());
        if (template == null) {
            return;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        boolean inside = x >= template.originX() && x < template.originX() + template.sizeX()
                && y >= template.originY() && y < template.originY() + template.sizeY()
                && z >= template.originZ() && z < template.originZ() + template.sizeZ();
        if (!inside) {
            say(player, "admin.point-outside", "<yellow>Attention : ce point est en dehors de la zone capturée de l'arène.");
        }
    }

    private void openItemList(Player player, Arena arena, MinigameType.ItemListSpec spec) {
        List<ItemStack> items = arena.itemLists().getOrDefault(spec.key(), List.of());
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.items-save", "<green>Enregistrer mon inventaire"), null, p -> {
            List<ItemStack> copy = new ArrayList<>();
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && !item.getType().isAir() && !lockedItem(item)) {
                    copy.add(item.clone());
                }
            }
            arena.itemLists().put(spec.key(), copy);
            plugin.repository().save();
            say(p, "admin.items-saved", "<green><n> objet(s) enregistré(s).", "n", copy.size());
            openItemList(p, arena, spec);
        }));
        buttons.add(btn(t("admin.items-clear", "<red>Vider la liste"), null, p -> {
            arena.itemLists().remove(spec.key());
            plugin.repository().save();
            openItemList(p, arena, spec);
        }));
        buttons.add(back(p -> openArena(p, arena)));
        gui.open(player, Component.text(spec.label()),
                List.of(t("admin.items-body", "<gray>Objets enregistrés : <white><n></white>. Remplissez votre inventaire avec le butin voulu puis enregistrez.", "n", items.size())),
                List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ capture de la zone

    private Corner cornerOf(Player player) {
        Location l = player.getLocation();
        return new Corner(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private String cornerText(Corner corner) {
        return corner == null ? "non défini" : corner.world() + " " + corner.x() + ", " + corner.y() + ", " + corner.z();
    }

    private void openCapture(Player player, Arena arena) {
        UUID uuid = player.getUniqueId();
        Corner c1 = corner1.get(uuid);
        Corner c2 = corner2.get(uuid);
        List<Component> body = new ArrayList<>();
        body.add(t("admin.capture-help", "<gray>Placez-vous à un coin de la construction, définissez le coin 1 ; allez au coin opposé (autre hauteur comprise), définissez le coin 2 ; puis capturez."));
        body.add(t("admin.capture-c1", "<gray>Coin 1 : <white><c>", "c", cornerText(c1)));
        body.add(t("admin.capture-c2", "<gray>Coin 2 : <white><c>", "c", cornerText(c2)));
        if (!arena.suggestedArea().isBlank()) {
            body.add(t("admin.capture-last", "<gray>Dernière zone : <white><a>", "a", arena.suggestedArea().replace(';', ' ')));
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.capture-set1", "<green>Coin 1 = ma position"), null, p -> {
            corner1.put(p.getUniqueId(), cornerOf(p));
            openCapture(p, arena);
        }));
        buttons.add(btn(t("admin.capture-set2", "<green>Coin 2 = ma position"), null, p -> {
            corner2.put(p.getUniqueId(), cornerOf(p));
            openCapture(p, arena);
        }));
        String[] parts = arena.suggestedArea().split(";");
        if (parts.length == 7) {
            buttons.add(btn(t("admin.capture-use-last", "<aqua>Reprendre la dernière zone / la zone suggérée"), null, p -> {
                corner1.put(p.getUniqueId(), new Corner(parts[0], parseInt(parts[1], 0), parseInt(parts[2], 0), parseInt(parts[3], 0)));
                corner2.put(p.getUniqueId(), new Corner(parts[0], parseInt(parts[4], 0), parseInt(parts[5], 0), parseInt(parts[6], 0)));
                openCapture(p, arena);
            }));
        }
        if (c1 != null && c2 != null) {
            buttons.add(btn(t("admin.capture-go", "<gold><bold>Capturer la zone"),
                    t("admin.capture-go-tip", "<gray>Remplace le modèle actuel. Les copies déjà générées sont supprimées ; une partie en cours continue et sa copie est supprimée à la fin."),
                    p -> capture(p, arena)));
        }
        if (arena.hasTemplate()) {
            buttons.add(btn(t("admin.capture-delete", "<red>Supprimer le modèle"), null, p -> {
                plugin.instances().closeAllOfArena(arena.id(), t("admin.arena-changed", "<yellow>L'arène a été modifiée."));
                plugin.templates().delete(arena.id());
                arena.hasTemplate(false);
                plugin.repository().save();
                openCapture(p, arena);
            }));
        }
        buttons.add(back(p -> openArena(p, arena)));
        gui.open(player, t("admin.capture-title", "<gold><bold>Capture de la zone"), body, List.of(), buttons, gui.close(), 1);
    }

    private void capture(Player player, Arena arena) {
        Corner c1 = corner1.get(player.getUniqueId());
        Corner c2 = corner2.get(player.getUniqueId());
        if (c1 == null || c2 == null) {
            return;
        }
        if (!c1.world().equals(c2.world())) {
            say(player, "admin.capture-worlds", "<red>Les deux coins doivent être dans le même monde.");
            return;
        }
        World world = Bukkit.getWorld(c1.world());
        if (world == null) {
            say(player, "admin.capture-noworld", "<red>Monde introuvable.");
            return;
        }
        if (!capturing.add(arena.id())) {
            say(player, "admin.capture-busy", "<red>Une capture est déjà en cours pour cette arène.");
            return;
        }
        // 1.12.2 : les parties en cours ne sont plus fermees (demande explicite de l'utilisateur) - voir plus bas.
        say(player, "admin.capture-start", "<gray>Capture en cours…");
        final UUID uuid = player.getUniqueId();
        plugin.templates().capture(world, c1.x(), c1.y(), c1.z(), c2.x(), c2.y(), c2.z(), arena.id(), result -> plugin.sync(() -> {
            capturing.remove(arena.id());
            Player online = Bukkit.getPlayer(uuid);
            if (result.ok()) {
                arena.hasTemplate(true);
                arena.suggestedArea(c1.world() + ";" + c1.x() + ";" + c1.y() + ";" + c1.z() + ";" + c2.x() + ";" + c2.y() + ";" + c2.z());
                plugin.repository().save();
                // 1.12.2 - demande explicite : les copies deja generees de l'ancien modele sont supprimees ; une copie
                // utilisee par une partie en cours n'est supprimee qu'a la fin de cette partie (voir ArenaPool.release).
                plugin.instances().retireArenaCopies(arena.id());
                if (online != null) {
                    say(online, "admin.capture-done", "<green>Zone capturée (<n> blocs non vides).", "n", result.blocks());
                    openArena(online, arena);
                }
            } else if (online != null) {
                say(online, "admin.capture-failed", "<red>Capture impossible : <error>", "error", result.error());
            }
        }));
    }

    // ------------------------------------------------------------------ parties en cours

    private void openInstances(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        for (GameInstance game : new ArrayList<>(plugin.instances().all())) {
            Component label = t("admin.game-line", "<white><id> <gray>- <mg> <dark_gray>(<kind>, <n> joueur(s), <phase>)",
                    "id", game.id(), "mg", plugin.lang().parse(game.minigame().display()),
                    "kind", game.isPublic() ? "publique" : "privée " + game.code(), "n", game.members().size(), "phase", game.phase().name());
            buttons.add(btn(label, null, p -> openInstance(p, game)));
        }
        buttons.add(back(this::openHome));
        gui.open(player, t("admin.games-title", "<light_purple><bold>Parties en cours"),
                List.of(t("admin.games-body", "<gray><n> partie(s).", "n", plugin.instances().all().size())), List.of(), buttons, gui.close(), 1);
    }

    private void openInstance(Player player, GameInstance game) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.game-join", "<aqua>Regarder (téléportation dans les gradins)"), null, p -> {
            if (game.closing() || !game.ready()) {
                say(p, "admin.game-unavailable", "<red>Cette partie n'est plus disponible.");
                return;
            }
            Component error = plugin.instances().joinInstance(p, game);
            if (error != null) {
                p.sendMessage(plugin.prefix().append(error));
            }
        }));
        buttons.add(btn(t("admin.game-close", "<dark_red>Fermer la partie"), null, p -> {
            plugin.instances().close(game, t("game.closed-by-admin", "<yellow>La partie a été fermée par un modérateur."));
            openInstances(p);
        }));
        buttons.add(back(this::openInstances));
        gui.open(player, Component.text("Partie " + game.id()), List.of(), List.of(), buttons, gui.close(), 1);
    }
}
