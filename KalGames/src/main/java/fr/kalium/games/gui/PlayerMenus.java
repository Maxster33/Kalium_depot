package fr.kalium.games.gui;

import fr.kalium.games.KalGames;
import fr.kalium.games.data.Kit;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.game.InstanceManager;
import fr.kalium.games.game.PvpInstance;
import fr.kalium.games.game.RaceInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Menus des joueurs : choix du mini-jeu, creation / acces aux parties, vote du kit, menu de la partie. */
public final class PlayerMenus {

    private static final long COOLDOWN_MS = 400L;

    private final KalGames plugin;
    private final Gui gui;
    private final Map<UUID, Long> lastOpen = new HashMap<>();

    public PlayerMenus(KalGames plugin, Gui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    private Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

    private Component name(Minigame minigame) {
        return plugin.lang().parse(minigame.display());
    }

    /**
     * Objet du hub, objet "menu de la partie", /kalgames menu : KG_Menu ouvre le menu de la partie en cours (voir
     * openCurrent) ou l'accueil de kal-games (1.16.0 : le cadre des menus du hub est dans KG_Menu).
     */
    public void openMenuFor(Player player) {
        plugin.kgMenu().openFor(player);
    }

    /** Menu de la partie ou spectateur si le joueur est dans un mini-jeu de KalGames (true), sinon rien (false). */
    public boolean openCurrent(Player player) {
        if (plugin.instances().of(player) != null) {
            openGameMenu(player);
            return true;
        }
        if (plugin.instances().spectatorOf(player) != null) {
            openSpectatorMenu(player);
            return true;
        }
        return false;
    }

    public void forget(UUID uuid) {
        lastOpen.remove(uuid);
    }

    // ------------------------------------------------------------------ hub : liste des mini-jeux

    /** Accueil de kal-games (dans KG_Menu depuis la 1.16.0). */
    public void openGames(Player player) {
        plugin.kgMenu().openHome(player, null);
    }

    /** Boutons des mini-jeux de KalGames pour l'accueil de KG_Menu (1.16.0 : meme contenu que l'ancien menu). */
    public List<fr.kalium.kgmenu.api.MenuProvider.Entry> gameEntries(Player player) {
        List<fr.kalium.kgmenu.api.MenuProvider.Entry> entries = new ArrayList<>();
        boolean admin = plugin.isAdmin(player);
        InstanceManager manager = plugin.instances();
        for (Minigame minigame : plugin.repository().minigames()) {
            if (!minigame.playable()) {
                continue;
            }
            boolean usable = !manager.usableArenas(minigame).isEmpty();
            if (!usable && !admin) {
                continue;
            }
            List<Component> tip = new ArrayList<>();
            if (!minigame.description().isBlank()) {
                tip.add(plugin.lang().parse("<gray>" + minigame.description()));
            }
            if (usable) {
                tip.add(t("menu.games-count", "<dark_gray>Parties en cours : <white><games></white> - Joueurs : <white><players></white>",
                        "games", manager.gamesOf(minigame.id()), "players", manager.playersIn(minigame.id())));
            } else {
                tip.add(t("menu.games-unusable", "<red>Aucune arène complète (visible des modérateurs uniquement)."));
            }
            entries.add(new fr.kalium.kgmenu.api.MenuProvider.Entry(minigame.id(), name(minigame),
                    Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), tip),
                    (p, back) -> openMinigame(p, minigame)));
        }
        return entries;
    }

    // ------------------------------------------------------------------ mini-jeu choisi

    public void openMinigame(Player player, Minigame minigame) {
        if (minigame.type() == MinigameType.RUSH) {
            // 1.11.0 : Rush -> le joueur choisit d'abord l'arene (demande explicite de l'utilisateur).
            openRushArenas(player, minigame);
            return;
        }
        InstanceManager manager = plugin.instances();
        List<ActionButton> buttons = new ArrayList<>();
        if (minigame.privateEnabled()) {
            buttons.add(gui.button(t("menu.private-create", "<green>Créer une partie privée"),
                    t("menu.private-create-tip", "<gray>Vous êtes l'hôte et invitez vos amis avec un code."),
                    p -> openCreatePrivate(p, minigame)));
            buttons.add(gui.button(t("menu.private-join", "<aqua>Rejoindre une partie privée"),
                    t("menu.private-join-tip", "<gray>Avec un code ou depuis la liste des parties ouvertes."),
                    p -> openJoinPrivate(p, minigame)));
        }
        if (minigame.publicEnabled()) {
            GameInstance hall = manager.hall(minigame.id());
            int connected = hall == null ? 0 : hall.members().size();
            Component label = t("menu.public-join", "<gold>Jouer en partie publique <dark_gray>(<n> joueur(s))", "n", connected);
            Component tip = hall == null
                    ? t("menu.public-tip-empty", "<gray>Rejoignez la file d'attente : vous attendrez votre tour dans les gradins.")
                    : t("menu.public-tip", "<gray>Dans les gradins : <white><n></white> joueur(s), <white><q></white> en file.",
                            "n", hall.members().size(), "q", hall.queue().size());
            buttons.add(gui.button(label, tip, p -> {
                Component error = manager.joinPublic(p, minigame);
                if (error != null) {
                    p.sendMessage(plugin.prefix().append(error));
                }
            }));
        }
        if (minigame.getBool("allow-spectate", true)) {
            buttons.add(gui.button(t("menu.spectate", "<blue>Regarder une partie"),
                    t("menu.spectate-tip", "<gray>Mode spectateur, sans y participer."), p -> openSpectate(p, minigame)));
        }
        buttons.add(gui.button(t("menu.rankings", "<light_purple>Classements"),
                t("menu.rankings-tip", "<gray>Top 10 général et du mois."), p -> plugin.ranking().openPlayer(p, minigame.id(), false, q -> plugin.menus().openMinigame(q, minigame))));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, this::openGames));
        List<Component> body = new ArrayList<>();
        body.add(minigame.description().isBlank() ? Component.empty() : plugin.lang().parse("<gray>" + minigame.description()));
        gui.open(player, name(minigame), body, List.of(), buttons, null, 1);
    }

    // ------------------------------------------------------------------ Rush (1.11.0)

    /** Rush : choix de l'arene (2 ou 4 equipes), puis partie publique / privee sur cette arene. */
    public void openRushArenas(Player player, Minigame minigame) {
        InstanceManager manager = plugin.instances();
        List<ActionButton> buttons = new ArrayList<>();
        for (Arena arena : manager.usableArenas(minigame)) {
            int teams = fr.kalium.games.model.RushLayout.teamCount(arena);
            GameInstance hall = manager.arenaHall(minigame, arena);
            int waiting = hall == null || hall.matchInProgress() ? 0 : hall.members().size();
            Component label = plugin.lang().parse(arena.display())
                    .append(t("rush.arena-teams", " <gray>(<n> équipes)", "n", teams));
            Component tip = t("rush.arena-tip", "<gray>Partie publique : <white><w></white> joueur(s) en attente.", "w", waiting);
            buttons.add(gui.button(label, tip, p -> openRushArena(p, minigame, arena)));
        }
        if (minigame.privateEnabled()) {
            buttons.add(gui.button(t("menu.private-join", "<aqua>Rejoindre une partie privée"),
                    t("menu.private-join-tip", "<gray>Avec un code ou depuis la liste des parties ouvertes."),
                    p -> openJoinPrivate(p, minigame)));
        }
        if (minigame.getBool("allow-spectate", true)) {
            buttons.add(gui.button(t("menu.spectate", "<blue>Regarder une partie"),
                    t("menu.spectate-tip", "<gray>Mode spectateur, sans y participer."), p -> openSpectate(p, minigame)));
        }
        buttons.add(gui.button(t("menu.rankings", "<light_purple>Classements"),
                t("menu.rankings-tip", "<gray>Top 10 général et du mois."), p -> plugin.ranking().openPlayer(p, minigame.id(), false, q -> plugin.menus().openMinigame(q, minigame))));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, this::openGames));
        List<Component> body = new ArrayList<>();
        if (!minigame.description().isBlank()) {
            body.add(plugin.lang().parse("<gray>" + minigame.description()));
        }
        body.add(t("rush.choose-arena", "<white>Choisissez une arène :"));
        gui.open(player, name(minigame), body, List.of(), buttons, null, 1);
    }

    public void openRushArena(Player player, Minigame minigame, Arena arena) {
        InstanceManager manager = plugin.instances();
        List<ActionButton> buttons = new ArrayList<>();
        if (minigame.publicEnabled()) {
            GameInstance hall = manager.arenaHall(minigame, arena);
            int waiting = hall == null || hall.matchInProgress() ? 0 : hall.members().size();
            buttons.add(gui.button(t("rush.public-join", "<gold>Jouer en partie publique <dark_gray>(<n> en attente)", "n", waiting),
                    t("rush.public-tip", "<gray>La partie démarre dès qu'il y a assez de joueurs."), p -> {
                        Component error = manager.joinPublicArena(p, minigame, arena);
                        if (error != null) {
                            p.sendMessage(plugin.prefix().append(error));
                        }
                    }));
        }
        if (minigame.privateEnabled()) {
            buttons.add(gui.button(t("menu.private-create", "<green>Créer une partie privée"),
                    t("menu.private-create-tip", "<gray>Vous êtes l'hôte et invitez vos amis avec un code."),
                    p -> openRushCreatePrivate(p, minigame, arena)));
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, p -> openRushArenas(p, minigame)));
        List<Component> body = List.of(t("rush.arena-body", "<gray><n> équipes de <white><size></white> joueur(s) maximum.",
                "n", fr.kalium.games.model.RushLayout.teamCount(arena), "size", Math.max(1, Math.min(4, minigame.getInt("team-size", 4)))));
        gui.open(player, plugin.lang().parse(arena.display()), body, List.of(), buttons, null, 1);
    }

    private void openRushCreatePrivate(Player player, Minigame minigame, Arena arena) {
        List<DialogInput> inputs = List.of(
                gui.toggle("listed", t("menu.create-listed", "Afficher la partie dans la liste des parties ouvertes"), true));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("menu.create-confirm", "<green>Créer la partie"), null, (p, view) -> {
            Map<String, Object> options = new HashMap<>();
            Boolean listed = view.getBoolean("listed");
            options.put("listed", listed != null && listed);
            InstanceManager.Result result = plugin.instances().createPrivate(p, minigame, arena.id(), options);
            if (!result.ok()) {
                p.sendMessage(plugin.prefix().append(result.error()));
                return;
            }
            p.sendMessage(plugin.prefix().append(t("menu.create-done",
                    "<green>Partie créée. Code : <white><bold><code></bold></white> <gray>(à partager avec vos amis).",
                    "code", result.instance().code())));
        }));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, p -> openRushArena(p, minigame, arena)));
        gui.open(player, t("menu.create-title", "<green>Nouvelle partie privée"),
                List.of(plugin.lang().parse(arena.display())), inputs, buttons, gui.close(), 1);
    }

    /** Choix de l'equipe dans la salle d'attente (une equipe pleine ne peut plus etre rejointe). */
    public void openRushTeams(Player player, fr.kalium.games.game.RushInstance game) {
        if (!game.teamsOpen()) {
            plugin.tell(player, "rush.team-locked", "<red>Les équipes ne se choisissent que dans la salle d'attente.");
            return;
        }
        UUID uuid = player.getUniqueId();
        List<Component> body = new ArrayList<>();
        List<ActionButton> buttons = new ArrayList<>();
        for (fr.kalium.games.model.RushLayout.Team team : game.teams()) {
            List<String> names = new ArrayList<>();
            for (UUID member : game.membersOf(team)) {
                names.add(nameOf(member));
            }
            Component teamName = fr.kalium.games.game.RushItems.teamName(team);
            body.add(t("rush.teams-line", "<team> <dark_gray>(<n>/<size>) <gray><names>", "team", teamName,
                    "n", names.size(), "size", game.teamSize(), "names", String.join(", ", names)));
            boolean mine = game.teamOf(uuid) == team;
            boolean full = names.size() >= game.teamSize();
            Component label = mine
                    ? t("vote.selected", "<green>✔ ").append(t("rush.teams-join", "Équipe <team>", "team", teamName))
                    : full
                    ? t("rush.teams-full", "<dark_gray>Équipe <team> <dark_gray>(complète)", "team", teamName)
                    : t("rush.teams-join", "Équipe <team>", "team", teamName);
            buttons.add(gui.button(label, null, p -> {
                Component error = game.chooseTeam(p, team);
                if (error != null) {
                    p.sendMessage(plugin.prefix().append(error));
                }
                if (game.teamsOpen() && game.isMember(p.getUniqueId())) {
                    openRushTeams(p, game);
                }
            }));
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, this::openGameMenu));
        gui.open(player, t("rush.teams-title", "<aqua><bold>Choisissez votre équipe"), body, List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ partie privee : creation

    public void openCreatePrivate(Player player, Minigame minigame) {
        List<Arena> arenas = plugin.instances().usableArenas(minigame);
        if (arenas.isEmpty()) {
            plugin.tell(player, "game.no-arena", "<red>Aucune arène complète n'est disponible pour ce mini-jeu.");
            return;
        }
        List<DialogInput> inputs = new ArrayList<>();
        if (arenas.size() > 1) {
            List<String> ids = new ArrayList<>();
            List<Component> labels = new ArrayList<>();
            for (Arena arena : arenas) {
                ids.add(arena.id());
                labels.add(plugin.lang().parse(arena.display()));
            }
            inputs.add(gui.choice("arena", t("menu.create-arena", "Arène"), ids, labels, ids.get(0)));
        }
        MinigameType type = minigame.type();
        if (type == MinigameType.PVP_KIT) {
            int maxTeams = 2;
            for (Arena arena : arenas) {
                int count = 0;
                for (String letter : List.of("a", "b", "c", "d")) {
                    if (arena.point("spawn-" + letter) == null) {
                        break;
                    }
                    count++;
                }
                maxTeams = Math.max(maxTeams, count);
            }
            if (maxTeams > 2) {
                inputs.add(gui.number("teams", t("menu.create-teams", "Nombre d'équipes"), 2, maxTeams, 2, 1));
            }
            inputs.add(gui.number("teamSize", t("menu.create-team-size", "Joueurs par équipe"), 1, 4, 1, 1));
            if (minigame.getBool("bedrock-option", true)) {
                inputs.add(gui.toggle("haste", t("menu.create-haste", "PvP Bedrock (Haste, clic spam)"), false));
            }
            inputs.add(gui.choice("rounds", t("menu.create-rounds", "Nombre de manches"),
                    List.of("1", "3", "5"),
                    List.of(t("menu.rounds-1", "1 manche"), t("menu.rounds-3", "3 manches (première équipe à 2 victoires)"),
                            t("menu.rounds-5", "5 manches (première équipe à 3 victoires)")), "1"));
            inputs.add(gui.choice("kitMode", t("menu.create-kitmode", "Choix du kit"),
                    List.of("vote", "random"),
                    List.of(t("menu.kitmode-vote", "Vote avant chaque manche"),
                            t("menu.kitmode-random", "Kit aléatoire à chaque manche (le même pour toutes les équipes)")), "vote"));
        } else {
            int max = minigame.getInt("max-players", type == MinigameType.BOAT_RACE ? 12 : 8);
            if (max > 1) {
                inputs.add(gui.number("maxPlayers", t("menu.create-max", "Joueurs maximum"), 1, max, Math.min(max, 4), 1));
            }
            if (type == MinigameType.BOAT_RACE) {
                inputs.add(gui.number("laps", t("menu.create-laps", "Nombre de tours (1 à 40)"), 1, 40, Math.max(1, Math.min(40, minigame.getInt("laps", 3))), 1));
            }
            if (type == MinigameType.PARKOUR) {
                inputs.add(gui.toggle("training", t("menu.create-training",
                        "Mode entraînement (sans limite de temps, sans points ni classement)"), false));
            }
        }
        inputs.add(gui.toggle("listed", t("menu.create-listed", "Afficher la partie dans la liste des parties ouvertes"), true));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("menu.create-confirm", "<green>Créer la partie"), null, (p, view) -> {
            Map<String, Object> options = new HashMap<>();
            String arenaId = arenas.size() > 1 ? view.getText("arena") : arenas.get(0).id();
            putNumber(options, "teams", view);
            putNumber(options, "teamSize", view);
            putNumber(options, "maxPlayers", view);
            putNumber(options, "laps", view);
            Boolean haste = view.getBoolean("haste");
            if (haste != null) {
                options.put("haste", haste);
            }
            String roundsText = view.getText("rounds");
            if (roundsText != null) {
                try {
                    options.put("rounds", Integer.parseInt(roundsText.trim()));
                } catch (NumberFormatException ignored) {
                    options.put("rounds", 1);
                }
            }
            String kitMode = view.getText("kitMode");
            if (kitMode != null) {
                options.put("kitMode", kitMode);
            }
            Boolean training = view.getBoolean("training");
            if (training != null) {
                options.put("training", training);
            }
            Boolean listed = view.getBoolean("listed");
            options.put("listed", listed != null && listed);
            InstanceManager.Result result = plugin.instances().createPrivate(p, minigame, arenaId, options);
            if (!result.ok()) {
                p.sendMessage(plugin.prefix().append(result.error()));
                return;
            }
            p.sendMessage(plugin.prefix().append(t("menu.create-done",
                    "<green>Partie créée. Code : <white><bold><code></bold></white> <gray>(à partager avec vos amis).",
                    "code", result.instance().code())));
        }));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, p -> openMinigame(p, minigame)));
        gui.open(player, t("menu.create-title", "<green>Nouvelle partie privée"),
                List.of(t("menu.create-body", "<gray>Réglez la partie puis créez-la.")), inputs, buttons, gui.close(), 1);
    }

    private void putNumber(Map<String, Object> options, String key, DialogResponseView view) {
        Float value = view.getFloat(key);
        if (value != null) {
            options.put(key, Math.round(value));
        }
    }

    // ------------------------------------------------------------------ partie privee : acces

    public void openJoinPrivate(Player player, Minigame minigame) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("menu.join-code", "<aqua>Rejoindre avec ce code"), null, (p, view) -> {
            String code = view.getText("code");
            Component error = plugin.instances().joinPrivate(p, code == null ? "" : code);
            if (error != null) {
                p.sendMessage(plugin.prefix().append(error));
            }
        }));
        for (GameInstance game : plugin.instances().listedPrivate(minigame.id())) {
            String hostName = game.host() == null ? "?" : nameOf(game.host());
            Component label = t("menu.join-listed", "<white><code></white> <gray>- <white><host></white> <dark_gray>(<n> joueur(s))",
                    "code", game.code(), "host", hostName, "n", game.members().size());
            buttons.add(gui.button(label, null, p -> {
                Component error = plugin.instances().joinInstance(p, game);
                if (error != null) {
                    p.sendMessage(plugin.prefix().append(error));
                }
            }));
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, p -> openMinigame(p, minigame)));
        List<DialogInput> inputs = List.of(gui.text("code", t("menu.join-input", "Code de la partie"), "", 8));
        gui.open(player, t("menu.join-title", "<aqua>Rejoindre une partie privée"),
                List.of(t("menu.join-body", "<gray>Saisissez le code donné par l'hôte, ou choisissez une partie ouverte.")),
                inputs, buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ mode spectateur

    /** Liste les parties de ce mini-jeu qu'on peut regarder (publique en cours + parties privees visibles ou par code). */
    public void openSpectate(Player player, Minigame minigame) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("menu.spectate-code", "<aqua>Regarder avec un code"), null, (p, view) -> {
            String code = view.getText("code");
            Component error = plugin.instances().joinSpectatorByCode(p, code == null ? "" : code);
            if (error != null) {
                p.sendMessage(plugin.prefix().append(error));
            }
        }));
        for (GameInstance game : plugin.instances().spectatable(minigame.id())) {
            Component label = game.isPublic()
                    ? t("menu.spectate-public", "<gold>Partie publique <dark_gray>(<n> joueur(s))", "n", game.members().size())
                    : t("menu.spectate-listed", "<white><code></white> <gray>- <white><host></white> <dark_gray>(<n> joueur(s))",
                            "code", game.code(), "host", game.host() == null ? "?" : nameOf(game.host()), "n", game.members().size());
            buttons.add(gui.button(label, null, p -> {
                Component error = plugin.instances().joinSpectator(p, game);
                if (error != null) {
                    p.sendMessage(plugin.prefix().append(error));
                }
            }));
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, p -> openMinigame(p, minigame)));
        List<DialogInput> inputs = List.of(gui.text("code", t("menu.spectate-input", "Code de la partie"), "", 8));
        gui.open(player, t("menu.spectate-title", "<aqua><bold>Regarder une partie"),
                List.of(t("menu.spectate-body", "<gray>Vous serez en mode spectateur (vol libre). Tapez /hub pour arrêter de regarder.")),
                inputs, buttons, gui.close(), 1);
    }

    /** Menu d'un spectateur : arreter de regarder / retour au lobby. */
    public void openSpectatorMenu(Player player) {
        GameInstance game = plugin.instances().spectatorOf(player);
        if (game == null) {
            openGames(player);
            return;
        }
        List<Component> body = new ArrayList<>();
        body.add(t("spectate.body-title", "<gray>Vous regardez : <white><name>", "name", name(game.minigame())));
        body.add(t("spectate.body-info", "<gray>Mode spectateur : vol libre. Tapez /hub à tout moment pour arrêter de regarder."));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.button(t("spectate.stop", "<red>Arrêter de regarder"),
                t("spectate.stop-tip", "<gray>Retour au hub de Kal-Games."), p -> plugin.instances().leaveSpectator(p, false)));
        buttons.add(gui.button(t("game.lobby", "<#09add3>Retour au lobby"), null, plugin::connectLobby));
        gui.open(player, t("spectate.menu-title", "<gold><bold>Mode spectateur"), body, List.of(), buttons, null, 1);
    }

    private String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "?" : name;
    }

    // ------------------------------------------------------------------ vote du kit

    public void openVote(Player player, PvpInstance game) {
        if (!game.voteOpen(player.getUniqueId())) {
            return;
        }
        List<Kit> kits = game.voteKits();
        List<ActionButton> buttons = new ArrayList<>();
        String mine = game.voteOf(player.getUniqueId());
        for (Kit kit : kits) {
            boolean selected = kit.id().equals(mine);
            Component label = Component.empty();
            if (selected) {
                label = label.append(t("vote.selected", "<green>✔ "));
            }
            label = label.append(plugin.lang().parse(kit.display()))
                    .append(t("vote.count", " <dark_gray>(<n>)", "n", game.votesFor(kit.id())));
            List<Component> tip = new ArrayList<>(plugin.kits().summary(kit, 14));
            buttons.add(gui.button(label, Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), tip),
                    p -> vote(p, game, kit.id())));
        }
        Component random = Component.empty();
        if (PvpInstance.RANDOM_KIT.equals(mine)) {
            random = random.append(t("vote.selected", "<green>✔ "));
        }
        random = random.append(t("vote.random", "<light_purple>Kit aléatoire"))
                .append(t("vote.count", " <dark_gray>(<n>)", "n", game.votesFor(PvpInstance.RANDOM_KIT)));
        buttons.add(gui.button(random, t("vote.random-tip", "<gray>Un kit tiré au sort parmi tous les kits."),
                p -> vote(p, game, PvpInstance.RANDOM_KIT)));

        List<Component> body = new ArrayList<>();
        if (game.phase() == GameInstance.Phase.VOTE) {
            body.add(t("vote.body-running", "<gray>Le kit le plus voté sera utilisé. Temps restant : <white><s></white> s.",
                    "s", game.secondsLeft()));
        } else {
            body.add(t("vote.body-lobby", "<gray>Votez dès maintenant : le vote compte quand l'hôte lance la partie."));
        }
        gui.open(player, t("vote.title", "<gold><bold>Vote du kit"), body, List.of(), buttons, gui.close(), 2);
    }

    private void vote(Player player, PvpInstance game, String kitId) {
        Component error = game.castVote(player, kitId);
        if (error != null) {
            player.sendMessage(plugin.prefix().append(error));
            return;
        }
        Kit kit = plugin.kits().get(kitId);
        Component label = kit == null ? t("vote.random", "<light_purple>Kit aléatoire") : plugin.lang().parse(kit.display());
        player.sendMessage(plugin.prefix().append(t("vote.done", "<gray>Vote enregistré : <white><kit></white>", "kit", label)));
        openVote(player, game);
    }

    // ------------------------------------------------------------------ menu de la partie

    public void openGameMenu(Player player) {
        GameInstance game = plugin.instances().of(player);
        if (game == null) {
            openGames(player);
            return;
        }
        UUID uuid = player.getUniqueId();
        List<ActionButton> buttons = new ArrayList<>();
        List<Component> body = new ArrayList<>();
        body.add(t("game.body-title", "<gray>Mini-jeu : <white><name>", "name", name(game.minigame())));
        if (!game.isPublic()) {
            body.add(t("game.body-code", "<gray>Code de la partie : <white><bold><code></bold></white>", "code", game.code()));
        }
        if (!game.ready()) {
            body.add(t("game.body-preparing", "<yellow>Préparation de l'arène…"));
        }
        if (game instanceof PvpInstance info && !info.isPublic()) {
            body.add(t("game.body-pvp", "<gray>Manches : <white><rounds></white> - Kit : <white><kit></white>",
                    "rounds", info.rounds(), "kit", info.randomKits() ? "aléatoire" : "vote"));
        }
        if (game instanceof RaceInstance race && race.training()) {
            body.add(t("game.body-training", "<green>Mode entraînement : sans limite de temps, ni points, ni classement."));
            buttons.add(gui.button(t("game.training-restart", "<green>Recommencer depuis le départ"), null, p -> {
                GameInstance current = plugin.instances().of(p);
                if (current instanceof RaceInstance r) {
                    r.restartTraining(p);
                }
            }));
        }

        if (!game.isPublic() && game.isHost(uuid) && game.phase() == GameInstance.Phase.WAITING) {
            buttons.add(gui.button(t("game.start", "<green><bold>Lancer la partie"), null, p -> {
                GameInstance current = plugin.instances().of(p);
                if (current != null) {
                    Component error = current.requestStart(p);
                    if (error != null) {
                        p.sendMessage(plugin.prefix().append(error));
                    }
                }
            }));
        }
        if (game instanceof PvpInstance pvp) {
            if (pvp.voteOpen(uuid)) {
                buttons.add(gui.button(t("game.vote", "<gold>Voter pour le kit"), null, p -> openVote(p, pvp)));
            }
            if (!pvp.isPublic() && pvp.phase() == GameInstance.Phase.WAITING) {
                buttons.add(gui.button(t("game.teams", "<aqua>Équipes"), null, p -> openTeams(p, pvp)));
            }
        }
        if (game instanceof fr.kalium.games.game.RushInstance rush && rush.teamsOpen()) {
            fr.kalium.games.model.RushLayout.Team team = rush.teamOf(uuid);
            body.add(team == null
                    ? t("rush.menu-no-team", "<red>Vous n'avez pas encore choisi d'équipe.")
                    : t("rush.menu-team", "<gray>Votre équipe : <team>", "team", fr.kalium.games.game.RushItems.teamName(team)));
            buttons.add(gui.button(t("rush.menu-teams", "<aqua>Choisir mon équipe"), null, p -> openRushTeams(p, rush)));
        }
        if (!game.isPublic() && game.isHost(uuid)) {
            Component label = game.listed()
                    ? t("game.listed-on", "<gray>Partie visible dans la liste : <green>oui")
                    : t("game.listed-off", "<gray>Partie visible dans la liste : <red>non");
            buttons.add(gui.button(label, null, p -> {
                GameInstance current = plugin.instances().of(p);
                if (current != null && current.isHost(p.getUniqueId())) {
                    current.listed(!current.listed());
                }
                openGameMenu(p);
            }));
        }
        if (game.isPublic() && game.members().contains(uuid) && !(game instanceof fr.kalium.games.game.RushInstance)) {
            boolean inQueue = game.queued(uuid);
            if (game.isParticipant(uuid) && game.matchInProgress()) {
                body.add(t("game.body-playing", "<gray>Vous participez au match en cours."));
            } else {
                Component label = inQueue
                        ? t("game.queue-leave", "<yellow>Quitter la file d'attente")
                        : t("game.queue-join", "<green>Rejoindre la file d'attente");
                body.add(inQueue
                        ? t("game.body-queue", "<gray>Position dans la file : <white><pos>/<total>", "pos", game.queuePosition(uuid), "total", game.queue().size())
                        : t("game.body-spectator", "<gray>Vous regardez la partie depuis les gradins."));
                buttons.add(gui.button(label, null, p -> {
                    GameInstance current = plugin.instances().of(p);
                    if (current != null) {
                        current.setQueued(p.getUniqueId(), !current.queued(p.getUniqueId()));
                    }
                    openGameMenu(p);
                }));
            }
        }
        buttons.add(gui.button(t("game.leave", "<red>Quitter la partie"), t("game.leave-tip", "<gray>Retour au hub de Kal-Games."),
                p -> plugin.hub().sendToHub(p)));
        buttons.add(gui.button(t("game.lobby", "<#09add3>Retour au lobby"), null, plugin::connectLobby));
        if (plugin.isAdmin(player)) {
            buttons.add(gui.button(t("game.admin-close", "<dark_red>Fermer la partie (modérateur)"), null, p -> {
                GameInstance current = plugin.instances().of(p);
                if (current != null) {
                    plugin.instances().close(current, t("game.closed-by-admin", "<yellow>La partie a été fermée par un modérateur."));
                }
            }));
        }
        gui.open(player, t("game.menu-title", "<gold><bold>Menu de la partie"), body, List.of(), buttons, null, 1);
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
                    names.add(nameOf(member));
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
                int current = game.teamOf(member);
                int next = (current + 1) % game.privateTeams();
                Component label = t("teams.move", "Déplacer <name> <dark_gray>→ <team>", "name", nameOf(member), "team", game.teamName(next));
                buttons.add(gui.button(label, null, p -> {
                    Component error = game.moveToTeam(member, next);
                    if (error != null) {
                        p.sendMessage(plugin.prefix().append(error));
                    }
                    openTeams(p, game);
                }));
            }
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, this::openGameMenu));
        gui.open(player, t("teams.title", "<aqua><bold>Équipes"), body, List.of(), buttons, gui.close(), 1);
    }
}
