package fr.kalium.core.claims;

import fr.kalium.core.KaliumCore;
import fr.kalium.core.menu.DialogHelper;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Ecrans d'administration du systeme de claim, ouverts depuis Parametres (reserve aux operateurs) :
 * emplacements par defaut pour les nouveaux joueurs, consultation/ajustement des emplacements d'un
 * joueur donne (en ligne ou non), et le sous-ecran "Options" (interrupteurs fins : claim, unclaim,
 * protection casse/depot/utilisation/pvp, chunk principal, conversion en jeton - 8 au total). Seul
 * l'interrupteur maitre du module ("Systeme de claim") vit dans l'ecran commun "Options actives"
 * (voir MenuService.openActiveOptions, et toggleInputs()/applyToggles() ci-dessous, le point d'entree
 * que ce module expose a cet ecran commun) ; tous les autres reglages fins sont regroupes ici, dans
 * "Options" (voir openOptions), pour ne pas noyer l'ecran commun avec les reglages propres a un seul
 * module.
 */
public final class ClaimsAdminMenu {

    // Les cles de DialogInput sont validees par Paper avec les memes regles que les variables de
    // fonction Minecraft (StringTemplate.isValidVariableName) : lettres/chiffres/underscore
    // UNIQUEMENT - ni point ni tiret, sous peine d'IllegalArgumentException("key must be a valid
    // input name") au moment d'ouvrir le Dialog (c'etait la cause du bouton "Claims" qui ne faisait
    // rien : la cle "default-slots" contenait un tiret).
    private static final String KEY_MASTER = "claims_enabled";
    private static final String KEY_CLAIM = "claims_claim_action";
    private static final String KEY_UNCLAIM = "claims_unclaim_action";
    private static final String KEY_PROTECTION_BREAK = "claims_protection_break";
    private static final String KEY_PROTECTION_PLACE = "claims_protection_place";
    private static final String KEY_PROTECTION_INTERACT = "claims_protection_interact";
    private static final String KEY_PROTECTION_PVP = "claims_protection_pvp";
    private static final String KEY_HOME = "claims_home_chunk";
    private static final String KEY_ITEM = "claims_item_conversion";

    private final KaliumCore plugin;
    private final DialogHelper dialogs;

    public ClaimsAdminMenu(KaliumCore plugin, DialogHelper dialogs) {
        this.plugin = plugin;
        this.dialogs = dialogs;
    }

    // ------------------------------------------------------------------ parametres du module (non-interrupteurs)

    public void openSettings(Player player) {
        if (!plugin.isAdmin(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }
        ClaimsService claims = plugin.claims();

        List<Component> body = new ArrayList<>();
        body.add(plugin.mm().deserialize(plugin.msg("settings-claims-header")));
        body.add(plugin.mm().deserialize(plugin.msg("settings-claims-default-slots-current"),
                Placeholder.unparsed("value", String.valueOf(claims.defaultSlots()))));

        List<ActionButton> buttons = new ArrayList<>();

        buttons.add(dialogs.inputButton("settings-claims-default-slots-save", null, (view, p) -> {
            Float value = view.getFloat("default_slots");
            if (value != null) {
                claims.defaultSlots(Math.round(value));
                p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-claims-default-slots-saved"),
                        Placeholder.unparsed("value", String.valueOf(claims.defaultSlots()))));
            }
            openSettings(p);
        }));

        buttons.add(dialogs.button("settings-claims-options-button", null, this::openOptions));
        buttons.add(dialogs.button("settings-claims-manage-button", null, this::openPlayerAdmin));
        buttons.add(dialogs.button("back-menu-button", null, p -> plugin.menu().openSettings(p)));

        List<DialogInput> inputs = List.of(
                DialogInput.numberRange("default_slots", plugin.mm().deserialize(plugin.msg("settings-claims-default-slots-label")),
                                0f, 200f)
                        .initial((float) claims.defaultSlots())
                        .step(1f)
                        .build());

        dialogs.show(player, "settings-claims-title", body, buttons, inputs);
    }

    // ------------------------------------------------------------------ interrupteur maitre (pour "Options actives")

    /** Case a cocher unique (l'interrupteur maitre du module) pour l'ecran commun "Options actives". */
    public List<DialogInput> toggleInputs() {
        return List.of(boolInput(KEY_MASTER, "settings-claims-toggle-master", plugin.claims().masterEnabled()));
    }

    private DialogInput boolInput(String key, String labelMsgKey, boolean current) {
        return DialogInput.bool(key, plugin.mm().deserialize(plugin.msg(labelMsgKey)))
                .initial(current)
                .onTrue(plugin.msg("toggle-on"))
                .onFalse(plugin.msg("toggle-off"))
                .build();
    }

    /** Applique la case a cocher lue depuis l'ecran "Options actives". Ne sauvegarde pas (l'appelant
     *  sauvegarde une seule fois apres avoir applique tous les modules). */
    public void applyToggles(DialogResponseView view) {
        Boolean master = view.getBoolean(KEY_MASTER);
        if (master != null) {
            plugin.modules().setEnabled("claims", master);
        }
    }

    // ------------------------------------------------------------------ options fines du module claims

    /**
     * Sous-ecran "Options" (bouton dans Parametres > Claims) : reglages fins du module - reclamer un
     * chunk, liberer un chunk, les 4 volets de la protection (casse, depot, utilisation, pvp), le
     * chunk principal (base) et la conversion en jeton.
     */
    public void openOptions(Player player) {
        if (!plugin.isAdmin(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }
        ClaimsService claims = plugin.claims();

        List<Component> body = List.of(plugin.mm().deserialize(plugin.msg("settings-claims-options-header")));

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(boolInput(KEY_CLAIM, "settings-claims-toggle-claim", claims.claimActionEnabled()));
        inputs.add(boolInput(KEY_UNCLAIM, "settings-claims-toggle-unclaim", claims.unclaimActionEnabled()));
        inputs.add(boolInput(KEY_PROTECTION_BREAK, "settings-claims-toggle-protection-break", claims.breakProtectionEnabled()));
        inputs.add(boolInput(KEY_PROTECTION_PLACE, "settings-claims-toggle-protection-place", claims.placeProtectionEnabled()));
        inputs.add(boolInput(KEY_PROTECTION_INTERACT, "settings-claims-toggle-protection-interact", claims.interactProtectionEnabled()));
        inputs.add(boolInput(KEY_PROTECTION_PVP, "settings-claims-toggle-protection-pvp", claims.pvpProtectionEnabled()));
        inputs.add(boolInput(KEY_HOME, "settings-claims-toggle-home", claims.homeEnabled()));
        inputs.add(boolInput(KEY_ITEM, "settings-claims-toggle-item", claims.itemConversionEnabled()));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.inputButton("settings-options-save-button", null, (view, p) -> {
            Boolean claim = view.getBoolean(KEY_CLAIM);
            if (claim != null) {
                claims.claimActionEnabled(claim);
            }
            Boolean unclaim = view.getBoolean(KEY_UNCLAIM);
            if (unclaim != null) {
                claims.unclaimActionEnabled(unclaim);
            }
            Boolean brk = view.getBoolean(KEY_PROTECTION_BREAK);
            if (brk != null) {
                claims.breakProtectionEnabled(brk);
            }
            Boolean place = view.getBoolean(KEY_PROTECTION_PLACE);
            if (place != null) {
                claims.placeProtectionEnabled(place);
            }
            Boolean interact = view.getBoolean(KEY_PROTECTION_INTERACT);
            if (interact != null) {
                claims.interactProtectionEnabled(interact);
            }
            Boolean pvp = view.getBoolean(KEY_PROTECTION_PVP);
            if (pvp != null) {
                claims.pvpProtectionEnabled(pvp);
            }
            Boolean home = view.getBoolean(KEY_HOME);
            if (home != null) {
                claims.homeEnabled(home);
            }
            Boolean item = view.getBoolean(KEY_ITEM);
            if (item != null) {
                claims.itemConversionEnabled(item);
            }
            plugin.saveConfig();
            p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-options-saved")));
            openOptions(p);
        }));
        buttons.add(dialogs.button("back-menu-button", null, this::openSettings));

        dialogs.show(player, "settings-claims-options-title", body, buttons, inputs);
    }

    // ------------------------------------------------------------------ gestion des emplacements d'un joueur

    public void openPlayerAdmin(Player admin) {
        openPlayerAdmin(admin, "", null);
    }

    /**
     * @param presetName pseudo a pre-remplir (garde la saisie de l'admin d'un clic a l'autre : sans
     *                   ca, "Consulter" puis "Appliquer" obligerait a retaper le pseudo a chaque fois).
     * @param infoLine   resultat de la derniere action (consultation ou ajustement), affiche dans le
     *                   corps du Dialog plutot que dans le chat ; null si aucune action n'a encore ete
     *                   effectuee sur cet ecran.
     */
    private void openPlayerAdmin(Player admin, String presetName, Component infoLine) {
        if (!plugin.isAdmin(admin)) {
            admin.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }

        List<Component> body = new ArrayList<>();
        body.add(plugin.mm().deserialize(plugin.msg("settings-claims-manage-header")));
        if (infoLine != null) {
            body.add(infoLine);
        }

        List<DialogInput> inputs = List.of(
                DialogInput.text("player", plugin.mm().deserialize(plugin.msg("settings-claims-manage-player-label")))
                        .initial(presetName == null ? "" : presetName)
                        .maxLength(16)
                        .build(),
                DialogInput.numberRange("delta", plugin.mm().deserialize(plugin.msg("settings-claims-manage-delta-label")),
                                -500f, 500f)
                        .initial(0f)
                        .step(1f)
                        .build());

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.inputButton("settings-claims-manage-consult-button", null, (view, p) -> {
            String name = view.getText("player");
            openPlayerAdmin(p, name, consultInfo(name));
        }));
        buttons.add(dialogs.inputButton("settings-claims-manage-apply-button", null, (view, p) -> {
            String name = view.getText("player");
            openPlayerAdmin(p, name, applyInfo(name, view.getFloat("delta")));
        }));
        buttons.add(dialogs.button("back-menu-button", null, this::openSettings));

        dialogs.show(admin, "settings-claims-manage-title", body, buttons, inputs);
    }

    /** Construit la ligne d'info affichee dans le Dialog (plutot qu'un message de chat) pour "Consulter". */
    private Component consultInfo(String name) {
        OfflinePlayer target = resolve(name);
        if (target == null) {
            return plugin.mm().deserialize(plugin.msg("settings-claims-manage-unknown-player"));
        }
        ClaimsService claims = plugin.claims();
        int used = claims.used(target.getUniqueId());
        int total = claims.total(target.getUniqueId());
        return plugin.mm().deserialize(plugin.msg("settings-claims-manage-info"),
                Placeholder.unparsed("player", String.valueOf(target.getName())),
                Placeholder.unparsed("used", String.valueOf(used)),
                Placeholder.unparsed("total", String.valueOf(total)),
                Placeholder.unparsed("available", String.valueOf(Math.max(0, total - used))));
    }

    /** Construit la ligne d'info affichee dans le Dialog (plutot qu'un message de chat) pour "Appliquer". */
    private Component applyInfo(String name, Float delta) {
        OfflinePlayer target = resolve(name);
        if (target == null) {
            return plugin.mm().deserialize(plugin.msg("settings-claims-manage-unknown-player"));
        }
        int change = delta == null ? 0 : Math.round(delta);
        plugin.claims().adjustSlots(target.getUniqueId(), change);
        return plugin.mm().deserialize(plugin.msg("settings-claims-manage-applied"),
                Placeholder.unparsed("player", String.valueOf(target.getName())),
                Placeholder.unparsed("total", String.valueOf(plugin.claims().total(target.getUniqueId()))));
    }

    private OfflinePlayer resolve(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name.trim());
        if (target == null) {
            target = Bukkit.getOfflinePlayer(name.trim());
        }
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            return null;
        }
        return target;
    }
}
