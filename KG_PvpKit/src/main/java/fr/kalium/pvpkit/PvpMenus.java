package fr.kalium.pvpkit;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.gui.Gui;
import fr.kalium.games.model.Minigame;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Menus du PvP Kit (repris de KalGames 1.21.0 : vote du kit, equipes, gestion des kits) ; 1.0.0 : declassement du kit
 * (dans le vote et depuis le menu de la partie). Textes dans le lang.yml de KalGames (memes cles qu'avant).
 */
public final class PvpMenus {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{2,24}");

    private final KGPvpKit pvp;
    private final KalGames plugin;
    private final Gui gui;
    /** Page du mini-jeu d'ou la gestion des kits a ete ouverte, pour le bouton Retour. */
    private final Map<UUID, Minigame> kitsFrom = new HashMap<>();

    public PvpMenus(KGPvpKit pvp, KalGames plugin) {
        this.pvp = pvp;
        this.plugin = plugin;
        this.gui = plugin.gui();
    }

    private Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

    private Component lines(List<Component> list) {
        return Component.join(JoinConfiguration.newlines(), list);
    }

    // ------------------------------------------------------------------ vote du kit

    public void openVote(Player player, PvpInstance game) {
        UUID uuid = player.getUniqueId();
        if (!game.voteOpen(uuid)) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        String mine = game.voteOf(uuid);
        for (Kit kit : game.voteKits()) {
            Component label = Component.empty();
            if (kit.id().equals(mine)) {
                label = label.append(t("vote.selected", "<green>✔ "));
            }
            label = label.append(plugin.lang().parse(kit.display()))
                    .append(t("vote.count", " <dark_gray>(<n>)", "n", game.votesFor(kit.id())));
            buttons.add(gui.button(label, lines(pvp.kits().summary(kit, 14)), p -> vote(p, game, kit.id())));
        }
        Component random = Component.empty();
        if (PvpInstance.RANDOM_KIT.equals(mine)) {
            random = random.append(t("vote.selected", "<green>✔ "));
        }
        random = random.append(t("vote.random", "<light_purple>Kit aléatoire"))
                .append(t("vote.count", " <dark_gray>(<n>)", "n", game.votesFor(PvpInstance.RANDOM_KIT)));
        buttons.add(gui.button(random, t("vote.random-tip", "<gray>Un kit tiré au sort parmi tous les kits."),
                p -> vote(p, game, PvpInstance.RANDOM_KIT)));
        // 1.0.0 : declassement du kit, un bouton par niveau (le choix reste d'une manche a l'autre).
        if (game.downgradeOpen(uuid)) {
            addDowngradeButtons(buttons, player, game, () -> openVote(player, game));
        }

        List<Component> body = new ArrayList<>();
        if (game.phase() == GameInstance.Phase.VOTE) {
            body.add(t("vote.body-running", "<gray>Le kit le plus voté sera utilisé. Temps restant : <white><s></white> s.",
                    "s", game.secondsLeft()));
        } else {
            body.add(t("vote.body-lobby", "<gray>Votez dès maintenant : le vote compte quand l'hôte lance la partie."));
        }
        if (game.downgradeOpen(uuid)) {
            body.add(downgradeHelp(game));
        }
        gui.open(player, t("vote.title", "<gold><bold>Vote du kit"), body, List.of(), buttons, gui.close(), 2);
    }

    private void vote(Player player, PvpInstance game, String kitId) {
        Component error = game.castVote(player, kitId);
        if (error != null) {
            player.sendMessage(plugin.prefix().append(error));
            return;
        }
        Kit kit = pvp.kits().get(kitId);
        Component label = kit == null ? t("vote.random", "<light_purple>Kit aléatoire") : plugin.lang().parse(kit.display());
        player.sendMessage(plugin.prefix().append(t("vote.done", "<gray>Vote enregistré : <white><kit></white>", "kit", label)));
        openVote(player, game);
    }

    // ------------------------------------------------------------------ declassement

    private Component downgradeHelp(PvpInstance game) {
        return t("pvp.downgrade-help", "<gray>Déclassement : chaque niveau retire un niveau à chaque enchantement et <white><pct> %</white> des consommables, et donne <white>+<bonus> %</white> de points.",
                "pct", Math.max(0, Math.min(25, game.minigame().getInt("downgrade-consumables-pct", 20))),
                "bonus", game.minigame().getInt("downgrade-bonus-pct", 50));
    }

    private void addDowngradeButtons(List<ActionButton> buttons, Player player, PvpInstance game, Runnable reopen) {
        int current = game.downgradeOf(player.getUniqueId());
        for (int level = 0; level <= game.maxDowngrade(); level++) {
            final int target = level;
            Component label = Component.empty();
            if (level == current) {
                label = label.append(t("vote.selected", "<green>✔ "));
            }
            label = label.append(level == 0
                    ? t("pvp.downgrade-none", "<gray>Kit complet <dark_gray>(x1)")
                    : t("pvp.downgrade-level", "<light_purple>Déclassement <level> <dark_gray>(x<mult>)",
                    "level", level, "mult", PvpInstance.fmt(1 + game.downgradeBonus(level))));
            buttons.add(gui.button(label, null, p -> {
                Component error = game.setDowngrade(p, target);
                if (error != null) {
                    p.sendMessage(plugin.prefix().append(error));
                    return;
                }
                reopen.run();
            }));
        }
    }

    /** Declassement seul (kit aleatoire ou unique, parties publiques en attente...) : depuis le menu de la partie. */
    public void openDowngrade(Player player, PvpInstance game) {
        if (!game.downgradeOpen(player.getUniqueId())) {
            plugin.tell(player, "pvp.downgrade-locked", "<red>Le déclassement ne se change pas pendant votre combat.");
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        addDowngradeButtons(buttons, player, game, () -> openDowngrade(player, game));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, plugin.menus()::openGameMenu));
        gui.open(player, t("pvp.downgrade-title", "<light_purple><bold>Déclassement du kit"), List.of(downgradeHelp(game)),
                List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ equipes

    public void openTeams(Player player, PvpInstance game) {
        if (game.isPublic() || game.phase() != GameInstance.Phase.WAITING) {
            plugin.tell(player, "team.locked", "<red>Les équipes ne sont modifiables que dans le salon d'une partie privée.");
            return;
        }
        UUID uuid = player.getUniqueId();
        List<Component> body = new ArrayList<>();
        for (int team = 0; team < game.privateTeams(); team++) {
            List<String> names = new ArrayList<>();
            for (UUID member : game.members()) {
                if (game.teamOf(member) == team) {
                    names.add(game.nameOf(member));
                }
            }
            body.add(t("teams.line", "<team> <dark_gray>(<n>/<size>) <gray><names>", "team", game.teamName(team),
                    "n", names.size(), "size", game.privateTeamSize(), "names", String.join(", ", names)));
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (int team = 0; team < game.privateTeams(); team++) {
            final int target = team;
            Component label = t("teams.join", "Rejoindre <team>", "team", game.teamName(team));
            if (game.teamOf(uuid) == team) {
                label = t("vote.selected", "<green>✔ ").append(label);
            }
            buttons.add(gui.button(label, null, p -> {
                Component error = game.changeTeam(p, target);
                if (error != null) {
                    p.sendMessage(plugin.prefix().append(error));
                }
                openTeams(p, game);
            }));
        }
        if (game.isHost(uuid)) {
            for (UUID member : new ArrayList<>(game.members())) {
                if (member.equals(uuid)) {
                    continue;
                }
                int next = (game.teamOf(member) + 1) % game.privateTeams();
                Component label = t("teams.move", "Déplacer <name> <dark_gray>→ <team>", "name", game.nameOf(member), "team", game.teamName(next));
                buttons.add(gui.button(label, null, p -> {
                    Component error = game.moveToTeam(member, next);
                    if (error != null) {
                        p.sendMessage(plugin.prefix().append(error));
                    }
                    openTeams(p, game);
                }));
            }
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, plugin.menus()::openGameMenu));
        gui.open(player, t("teams.title", "<aqua><bold>Équipes"), body, List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ kits (moderateurs)

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

    private ActionButton frm(Component label, Gui.FormClick click) {
        return gui.form(label, null, (p, view) -> {
            if (guard(p)) {
                click.run(p, view);
            }
        });
    }

    /** Objets jamais copies dans un kit : objets verrouilles du hub, boussole et comparateur de KLM_Menu. */
    private boolean lockedItem(ItemStack item) {
        if (plugin.items().isOurs(item)) {
            return true;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        var data = item.getItemMeta().getPersistentDataContainer();
        return data.has(new NamespacedKey("klm_menu", "menu_compass"), PersistentDataType.BYTE)
                || data.has(new NamespacedKey("kaliummenu", "menu_compass"), PersistentDataType.BYTE)
                || data.has(new NamespacedKey("klm_menu", "informations"), PersistentDataType.BYTE);
    }

    /** « Kits du mini-jeu » : bouton de la page du PvP Kit dans Informations &gt; Parametres (KalGames 1.22.0). */
    public void openMinigameKits(Player player, Minigame minigame) {
        if (!guard(player)) {
            return;
        }
        List<DialogInput> inputs = new ArrayList<>();
        for (Kit kit : pvp.kits().all()) {
            inputs.add(gui.toggle("k_" + kit.id(), plugin.lang().parse(kit.display()), minigame.kits().contains(kit.id())));
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.save", "<green>Enregistrer"), (p, view) -> {
            minigame.kits().clear();
            for (Kit kit : pvp.kits().all()) {
                Boolean value = view.getBoolean("k_" + kit.id());
                if (value != null && value) {
                    minigame.kits().add(kit.id());
                }
            }
            plugin.repository().save();
            plugin.tell(p, "admin.saved", "<green>Enregistré.");
            plugin.admin().openMinigame(p, minigame);
        }));
        buttons.add(btn(t("admin.kits-manage", "<aqua>Gérer les kits"), null, p -> {
            kitsFrom.put(p.getUniqueId(), minigame);
            openKits(p);
        }));
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, p -> plugin.admin().openMinigame(p, minigame)));
        gui.open(player, t("admin.mgkits-title", "<aqua><bold>Kits du mini-jeu"),
                List.of(t("admin.mgkits-body", "<gray>Cochez les kits proposés au vote.")), inputs, buttons, gui.close(), 1);
    }

    private void openKits(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        for (Kit kit : pvp.kits().all()) {
            buttons.add(btn(plugin.lang().parse(kit.display()), lines(pvp.kits().summary(kit, 12)), p -> openKit(p, kit)));
        }
        buttons.add(btn(t("admin.kits-create", "<green>+ Créer depuis mon inventaire"), null, this::openNewKit));
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, p -> {
            Minigame from = kitsFrom.get(p.getUniqueId());
            if (from != null && plugin.repository().minigame(from.id()) != null) {
                openMinigameKits(p, from);
            } else {
                plugin.admin().openHome(p);
            }
        }));
        gui.open(player, t("admin.kits-title", "<aqua><bold>Kits"),
                List.of(t("admin.kits-body", "<gray><n> kit(s). Survolez un kit pour voir son contenu.", "n", pvp.kits().all().size())),
                List.of(), buttons, gui.close(), 1);
    }

    private void openKit(Player player, Kit kit) {
        List<Component> body = new ArrayList<>(pvp.kits().summary(kit, 20));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(btn(t("admin.kit-update", "<yellow>Remplacer par mon inventaire actuel"), null, p -> {
            pvp.kits().createFromInventory(kit.id(), kit.display(), p, this::lockedItem);
            plugin.tell(p, "admin.kit-updated", "<green>Kit mis à jour.");
            openKits(p);
        }));
        buttons.add(btn(t("admin.kit-delete", "<dark_red>Supprimer ce kit"), null, p -> {
            pvp.kits().delete(kit.id());
            for (Minigame minigame : plugin.repository().minigames()) {
                minigame.kits().remove(kit.id());
            }
            plugin.repository().save();
            plugin.tell(p, "admin.kit-deleted", "<green>Kit supprimé.");
            openKits(p);
        }));
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, this::openKits));
        gui.open(player, plugin.lang().parse(kit.display()), body, List.of(), buttons, gui.close(), 1);
    }

    private void openNewKit(Player player) {
        List<DialogInput> inputs = List.of(
                gui.text("id", t("admin.kit-id", "Identifiant du kit"), "", 24),
                gui.text("display", t("admin.kit-display", "Nom affiché (MiniMessage)"), "<yellow>Nouveau kit", 60));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(frm(t("admin.kit-create-confirm", "<green>Créer le kit"), (p, view) -> {
            String id = view.getText("id") == null ? "" : view.getText("id").trim().toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                plugin.tell(p, "admin.bad-id", "<red>Identifiant invalide : 2 à 24 caractères parmi a-z, 0-9, - et _.");
                return;
            }
            if (pvp.kits().exists(id)) {
                plugin.tell(p, "admin.id-taken", "<red>Cet identifiant existe déjà.");
                return;
            }
            String display = view.getText("display") == null || view.getText("display").isBlank() ? id : view.getText("display");
            Kit kit = pvp.kits().createFromInventory(id, display, p, this::lockedItem);
            if (kit.empty()) {
                pvp.kits().delete(id);
                plugin.tell(p, "admin.kit-empty", "<red>Votre inventaire est vide : rien à enregistrer.");
                return;
            }
            plugin.tell(p, "admin.kit-created", "<green>Kit créé. Ajoutez-le aux mini-jeux voulus depuis \"Kits du mini-jeu\".");
            openKits(p);
        }));
        buttons.add(btn(t("menu.back", "<gray>Retour"), null, this::openKits));
        gui.open(player, t("admin.kit-new-title", "<green><bold>Nouveau kit"),
                List.of(t("admin.kit-new-body", "<gray>Préparez votre inventaire (armure, main secondaire, objets) puis validez : il est copié tel quel.")),
                inputs, buttons, gui.close(), 1);
    }
}
