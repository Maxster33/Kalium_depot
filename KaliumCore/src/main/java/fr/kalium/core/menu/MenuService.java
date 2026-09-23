package fr.kalium.core.menu;

import fr.kalium.core.KaliumCore;
import fr.kalium.core.module.ModuleRegistry;
import fr.kalium.core.stats.PlayerStats;
import fr.kalium.core.stats.StatSection;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menu principal, ecran Statistiques et ecran Parametres, bases sur les Dialogs natifs de
 * Minecraft (meme approche que KaliumMenu : aucun coffre, aucun objet en barre d'action necessaire
 * - le menu s'ouvre par commande, ce qui laisse la hotbar libre pour la survie). Les ecrans propres
 * a chaque module optionnel (ex. Claims) vivent dans leurs propres classes et sont ouverts via
 * ModuleRegistry.ModuleInfo.menuAction()/settingsAction().
 */
public final class MenuService {

    private static final long OPEN_COOLDOWN_MS = 800L;

    private final KaliumCore plugin;
    private final DialogHelper dialogs;
    private final Map<UUID, Long> lastOpen = new ConcurrentHashMap<>();

    public MenuService(KaliumCore plugin, DialogHelper dialogs) {
        this.plugin = plugin;
        this.dialogs = dialogs;
    }

    /** true si le joueur vient d'ouvrir le menu il y a moins de OPEN_COOLDOWN_MS (anti-spam de /kalium). */
    public boolean onCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastOpen.put(player.getUniqueId(), now);
        return previous != null && now - previous < OPEN_COOLDOWN_MS;
    }

    // ------------------------------------------------------------------ menu principal

    public void openMain(Player player) {
        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(dialogs.button("back-button", "back-description", p -> plugin.connectToLobby(p)));

        if (plugin.modules().isEnabled("claims") && plugin.claims().homeEnabled()) {
            buttons.add(dialogs.button("home-button", "home-description", this::teleportHome));
        }

        // Boutons des modules actifs (Statistiques, Claims, futurs modules type Commerce...).
        for (ModuleRegistry.ModuleInfo info : plugin.modules().all()) {
            if (info.menuLabel() == null || !plugin.modules().isEnabled(info.id())) {
                continue;
            }
            buttons.add(dialogs.rawButton(info.menuLabel(), info.menuDescription(), info.menuAction()));
        }

        if (plugin.isAdmin(player)) {
            buttons.add(dialogs.button("settings-button", "settings-description", this::openSettings));
        }

        dialogs.show(player, "menu-title",
                List.of(plugin.mm().deserialize(plugin.msg("menu-header"))), buttons);
    }

    private void teleportHome(Player player) {
        Location home = plugin.claims().homeLocation(player.getUniqueId());
        if (home == null) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("home-none")));
            return;
        }
        player.teleport(home);
        player.sendMessage(plugin.mm().deserialize(plugin.msg("home-teleported")));
    }

    // ------------------------------------------------------------------ statistiques

    public void openStats(Player player) {
        PlayerStats stats = plugin.stats().liveStats(player);
        List<Component> lines = new ArrayList<>();
        lines.add(plugin.mm().deserialize(plugin.msg("stats-header")));
        for (StatSection section : plugin.stats().sections()) {
            if (section.moduleId() != null && !plugin.modules().isEnabled(section.moduleId())) {
                continue;
            }
            lines.addAll(section.render(stats));
        }

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.button("back-menu-button", null, this::openMain));

        dialogs.show(player, "stats-title", lines, buttons);
    }

    // ------------------------------------------------------------------ parametres (operateurs)

    public void openSettings(Player player) {
        if (!plugin.isAdmin(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }

        List<Component> body = new ArrayList<>();
        body.add(plugin.mm().deserialize(plugin.msg("settings-header")));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.button("settings-options-button", null, this::openActiveOptions));
        for (ModuleRegistry.ModuleInfo info : plugin.modules().all()) {
            if (info.settingsAction() != null) {
                // Module avec ses propres options avancees non-interrupteurs (ex. Claims : emplacements
                // par defaut, gestion des joueurs) : bouton de navigation vers son propre ecran. Ses
                // interrupteurs, eux, sont regroupes avec tous les autres dans "Options actives".
                buttons.add(dialogs.rawButton(
                        plugin.mm().deserialize(plugin.msg("settings-open-button"),
                                Placeholder.component("module", info.displayName())),
                        null, info.settingsAction()));
            }
        }

        buttons.add(dialogs.button("settings-reload-button", null, p -> {
            plugin.loadSettings();
            p.sendMessage(plugin.mm().deserialize(plugin.msg("reloaded")));
            openSettings(p);
        }));
        buttons.add(dialogs.button("back-menu-button", null, this::openMain));

        dialogs.show(player, "settings-title", body, buttons);
    }

    // ------------------------------------------------------------------ options actives (interrupteurs regroupes)

    /**
     * Ecran unique regroupant TOUTES les options activables/desactivables du plugin (modules simples
     * comme Statistiques, et les interrupteurs fins des modules avances comme Claims) sous forme de
     * cases a cocher, avec un seul bouton pour tout enregistrer d'un coup.
     */
    public void openActiveOptions(Player player) {
        if (!plugin.isAdmin(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }

        List<Component> body = List.of(plugin.mm().deserialize(plugin.msg("settings-options-header")));

        List<DialogInput> inputs = new ArrayList<>();
        for (ModuleRegistry.ModuleInfo info : plugin.modules().all()) {
            if (info.settingsAction() == null) {
                // Module simple (ex. Statistiques) : une seule case, son etat actif/inactif.
                boolean enabled = plugin.modules().isEnabled(info.id());
                // Cle de DialogInput validee par Paper comme un nom de variable Minecraft : lettres/
                // chiffres/underscore uniquement (ni point ni tiret) - d'ou "module_" et non "module.".
                inputs.add(DialogInput.bool("module_" + info.id(), info.displayName())
                        .initial(enabled)
                        .onTrue(plugin.msg("toggle-on"))
                        .onFalse(plugin.msg("toggle-off"))
                        .build());
            }
        }
        inputs.addAll(plugin.claimsAdminMenu().toggleInputs());
        inputs.addAll(plugin.marketAdminMenu().toggleInputs());

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.inputButton("settings-options-save-button", null, (view, p) -> {
            for (ModuleRegistry.ModuleInfo info : plugin.modules().all()) {
                if (info.settingsAction() == null) {
                    Boolean value = view.getBoolean("module_" + info.id());
                    if (value != null) {
                        plugin.modules().setEnabled(info.id(), value);
                    }
                }
            }
            plugin.claimsAdminMenu().applyToggles(view);
            plugin.marketAdminMenu().applyToggles(view);
            plugin.saveConfig();
            p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-options-saved")));
            openActiveOptions(p);
        }));
        buttons.add(dialogs.button("back-menu-button", null, this::openSettings));

        dialogs.show(player, "settings-options-title", body, buttons, inputs);
    }
}
