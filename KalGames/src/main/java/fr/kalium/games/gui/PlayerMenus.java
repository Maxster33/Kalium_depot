package fr.kalium.games.gui;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.game.InstanceManager;
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
        // 1.26.0 : en coffre si le joueur l'a choisi (KG_Menu, « Paramètres › Affichage »).
        boolean coffre = plugin.enCoffre(player);
        if (plugin.instances().of(player) != null) {
            if (coffre) {
                plugin.coffres().partie(player);
            } else {
                openGameMenu(player);
            }
            return true;
        }
        if (plugin.instances().spectatorOf(player) != null) {
            if (coffre) {
                plugin.coffres().spectateur(player);
            } else {
                openSpectatorMenu(player);
            }
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
            // 1.23.0 (demande de Maxster33) : nombre de joueurs possibles, ex. « Joueurs : 4 à 8 ».
            int[] range = minigame.type().playerRange(minigame, manager.usableArenas(minigame));
            if (range != null) {
                tip.add(range[0] == range[1]
                        ? t("menu.player-range-one", "<gray>Joueurs : <white><n>", "n", range[0])
                        : t("menu.player-range", "<gray>Joueurs : <white><min> à <max>", "min", range[0], "max", range[1]));
            }
            if (usable) {
                tip.add(t("menu.games-count", "<dark_gray>Parties en cours : <white><games></white> - Joueurs : <white><players></white>",
                        "games", manager.gamesOf(minigame.id()), "players", manager.playersIn(minigame.id())));
            } else {
                tip.add(t("menu.games-unusable", "<red>Aucune arène complète (visible des modérateurs uniquement)."));
            }
            // 1.21.0 : nombre de joueurs affiche sur le bouton par KG_Menu 1.1.0.
            entries.add(new fr.kalium.kgmenu.api.MenuProvider.Entry(minigame.id(), name(minigame),
                    Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), tip),
                    (p, back) -> openMinigame(p, minigame), manager.playersIn(minigame.id())));
        }
        return entries;
    }

    // ------------------------------------------------------------------ 1.26.0 : accueil de KG_Menu 1.2.0

    /**
     * Parties publiques proposées dans les menus ? Non par défaut (LeKiwi06, 20/10/2026 : « masquer pour le moment les
     * parties publiques des mini-jeux, on a pas assez de joueurs pour ça »).
     */
    public boolean publicShown() {
        return plugin.getConfig().getBoolean("menus.parties-publiques", false);
    }

    /** « Créer une partie » : les mini-jeux jouables (les modérateurs voient aussi ceux sans arène complète). */
    public List<fr.kalium.kgmenu.api.MenuProvider.Jeu> jeux(Player player) {
        List<fr.kalium.kgmenu.api.MenuProvider.Jeu> jeux = new ArrayList<>();
        boolean admin = plugin.isAdmin(player);
        InstanceManager manager = plugin.instances();
        for (Minigame minigame : plugin.repository().minigames()) {
            if (!minigame.playable() || !minigame.privateEnabled()) {
                continue;
            }
            List<Arena> arenas = manager.usableArenas(minigame);
            if (arenas.isEmpty() && !admin) {
                continue;
            }
            List<Component> lignes = new ArrayList<>();
            if (!minigame.description().isBlank()) {
                lignes.add(plugin.lang().parse("<gray>" + minigame.description()));
            }
            int[] range = minigame.type().playerRange(minigame, arenas);
            if (range != null) {
                lignes.add(range[0] == range[1]
                        ? t("menu.player-range-one", "<gray>Joueurs : <white><n>", "n", range[0])
                        : t("menu.player-range", "<gray>Joueurs : <white><min> à <max>", "min", range[0], "max", range[1]));
            }
            if (arenas.isEmpty()) {
                lignes.add(t("menu.games-unusable", "<red>Aucune arène complète (visible des modérateurs uniquement)."));
            }
            jeux.add(new fr.kalium.kgmenu.api.MenuProvider.Jeu(minigame.id(), name(minigame), plugin.coffres().icone(minigame),
                    lignes, manager.playersIn(minigame.id()), (p, back) -> create(p, minigame, back)));
        }
        return jeux;
    }

    /** Réglages d'une nouvelle partie de ce mini-jeu, en coffre ou en fenêtre. */
    public void create(Player player, Minigame minigame, java.util.function.Consumer<Player> back) {
        if (plugin.enCoffre(player)) {
            plugin.coffres().creer(player, minigame, back);
        } else {
            openCreatePrivate(player, minigame, back);
        }
    }

    /** « Rejoindre une partie » et « Parties en cours » : les parties privées listées (et publiques si affichées). */
    public List<fr.kalium.kgmenu.api.MenuProvider.Partie> parties(Player player) {
        List<fr.kalium.kgmenu.api.MenuProvider.Partie> parties = new ArrayList<>();
        boolean publiques = publicShown();
        for (GameInstance game : plugin.instances().all()) {
            if (game.closing() || (game.isPublic() ? !publiques || game.members().isEmpty() : !game.listed())) {
                continue;
            }
            Minigame minigame = game.minigame();
            boolean spectate = minigame.getBool("allow-spectate", true);
            String host = game.isPublic() ? "Partie publique" : game.host() == null ? "?" : nameOf(game.host());
            parties.add(new fr.kalium.kgmenu.api.MenuProvider.Partie(game.id(), name(minigame), plugin.coffres().icone(minigame),
                    host, game.members().size(), game.capacite(), game.matchInProgress(),
                    p -> game.isPublic() ? plugin.instances().joinPublic(p, minigame) : plugin.instances().joinPrivate(p, game.code()),
                    spectate ? p -> plugin.instances().joinSpectator(p, game) : null));
        }
        return parties;
    }

    /** « Recréer ma dernière partie » : même jeu, même arène, mêmes réglages. */
    public fr.kalium.kgmenu.api.MenuProvider.Derniere derniere(Player player) {
        fr.kalium.games.game.Reglages.Derniere derniere = plugin.reglages().derniere(player.getUniqueId());
        if (derniere == null) {
            return null;
        }
        Minigame minigame = plugin.repository().minigame(derniere.jeu());
        if (minigame == null || !minigame.playable() || !minigame.privateEnabled()
                || plugin.instances().usableArenas(minigame).isEmpty()) {
            return null;
        }
        return new fr.kalium.kgmenu.api.MenuProvider.Derniere(name(minigame), plugin.coffres().icone(minigame), derniere.quand(),
                p -> plugin.coffres().creerPartie(p, minigame, derniere.choix().arene(), new HashMap<>(derniere.choix().options())));
    }

    /** « Entrer un code » : null si ce code n'est pas une partie de KalGames. */
    public Component code(Player player, String code) {
        GameInstance game = plugin.instances().byCode(code.toUpperCase(java.util.Locale.ROOT));
        if (game == null) {
            return null;
        }
        Component error = plugin.instances().joinPrivate(player, game.code());
        return error == null ? Component.empty() : error;
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
        openCreatePrivate(player, minigame, p -> plugin.kgMenu().openCreate(p));
    }

    /** 1.26.0 : back = bouton « Retour » ; les champs reprennent les derniers réglages du joueur pour ce jeu. */
    public void openCreatePrivate(Player player, Minigame minigame, java.util.function.Consumer<Player> back) {
        List<Arena> arenas = plugin.instances().usableArenas(minigame);
        fr.kalium.games.game.Reglages.Choix remembered = plugin.reglages().choix(player.getUniqueId(), minigame.id());
        Map<String, Object> last = remembered == null ? Map.of() : remembered.options();
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
            String selected = remembered != null && ids.contains(remembered.arene()) ? remembered.arene() : ids.get(0);
            inputs.add(gui.choice("arena", t("menu.create-arena", "Arène"), ids, labels, selected));
        }
        MinigameType type = minigame.type();
        if (type.createForm() != null) {
            // 1.22.0 : formulaire fourni par le jeu (equipes, manches, kit du PvP Kit : KG_PvpKit).
            inputs.addAll(type.createForm().inputs(minigame, arenas, gui));
        } else {
            int max = minigame.getInt("max-players", 8);
            if (max > 1 && type != MinigameType.RUSH) { // Rush : la taille vient des équipes de l'arène
                int initial = last.get("maxPlayers") instanceof Number n ? n.intValue() : Math.min(max, 4);
                inputs.add(gui.number("maxPlayers", t("menu.create-max", "Joueurs maximum"), 1, max, Math.max(1, Math.min(max, initial)), 1));
            }
            // 1.17.0 : options declarees par le type (ex. nombre de tours de KG_BoatRace).
            for (MinigameType.CreateOption option : type.createOptions()) {
                int def = last.get(option.key()) instanceof Number n ? n.intValue() : minigame.getInt(option.defaultSetting(), option.fallback());
                def = Math.max(option.min(), Math.min(option.max(), def));
                inputs.add(gui.number(option.key(), plugin.lang().parse(option.label()), option.min(), option.max(), def, 1));
            }
            // 1.20.0 : cases a cocher declarees par le type (ex. mode entrainement de KG_Parkour).
            for (MinigameType.CreateToggle toggle : type.createToggles()) {
                inputs.add(gui.toggle(toggle.key(), plugin.lang().parse(toggle.label()), last.get(toggle.key()) instanceof Boolean b && b));
            }
        }
        inputs.add(gui.toggle("listed", t("menu.create-listed", "Afficher la partie dans la liste des parties ouvertes"),
                !(last.get("listed") instanceof Boolean b) || b));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("menu.create-confirm", "<green>Créer la partie"), null, (p, view) -> {
            Map<String, Object> options = new HashMap<>();
            String arenaId = arenas.size() > 1 ? view.getText("arena") : arenas.get(0).id();
            if (minigame.type().createForm() != null) {
                minigame.type().createForm().read(view, options);
            }
            putNumber(options, "maxPlayers", view);
            for (MinigameType.CreateOption option : minigame.type().createOptions()) {
                putNumber(options, option.key(), view);
            }
            for (MinigameType.CreateToggle toggle : minigame.type().createToggles()) {
                Boolean value = view.getBoolean(toggle.key());
                if (value != null) {
                    options.put(toggle.key(), value);
                }
            }
            Boolean listed = view.getBoolean("listed");
            options.put("listed", listed != null && listed);
            // 1.26.0 : création commune aux coffres (réglages retenus, code dans le tchat).
            Component error = plugin.coffres().creerPartie(p, minigame, arenaId, options);
            if (error != null) {
                p.sendMessage(plugin.prefix().append(error));
            }
        }));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, back::accept));

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

    /** 1.26.0 : l'hôte nomme un autre hôte ou exclut un joueur (fenêtres). */
    private void openHostPlayers(Player player) {
        GameInstance game = plugin.instances().of(player);
        if (game == null || !game.isHost(player.getUniqueId())) {
            openGameMenu(player);
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (UUID member : game.members()) {
            if (member.equals(player.getUniqueId())) {
                continue;
            }
            String name = nameOf(member);
            buttons.add(gui.button(t("host.give-button", "<yellow>Nommer hôte : <white><player>", "player", name), null, p -> {
                Component error = plugin.instances().giveHost(p, member);
                if (error != null) {
                    p.sendMessage(plugin.prefix().append(error));
                }
                openGameMenu(p);
            }));
            buttons.add(gui.button(t("host.kick-button", "<red>Exclure : <white><player>", "player", name), null, p ->
                    gui.confirm(p, t("host.kick-title", "<red><bold>Exclure un joueur"),
                            t("host.kick-confirm", "<gray>Renvoyer <white><player></white> au hub ?", "player", name), q -> {
                                Component error = plugin.instances().kick(q, member);
                                if (error != null) {
                                    q.sendMessage(plugin.prefix().append(error));
                                }
                                openGameMenu(q);
                            }, this::openHostPlayers)));
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, this::openGameMenu));
        gui.open(player, t("host.players-title", "<white><bold>Joueurs de la partie"), List.of(), List.of(), buttons, gui.close(), 1);
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
        if (!game.isPublic()) {
            // 1.26.0 : entrer dans la partie (salle d'attente, prochaine manche).
            buttons.add(gui.button(t("coffre.spectateur-jouer", "<green>Rejoindre la partie"),
                    t("coffre.spectateur-jouer-tip", "<gray>Vous jouerez la prochaine manche."), p -> {
                        GameInstance watched = plugin.instances().spectatorOf(p);
                        Component error = watched == null ? null : plugin.instances().spectatorToPlayer(p, watched);
                        if (error != null) {
                            p.sendMessage(plugin.prefix().append(error));
                        }
                    }));
        }
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
        // 1.26.0 : relance en salle d'attente et résumé de la manche.
        if (game.relance() > 0) {
            body.add(t("coffre.relance", "<green>Nouvelle partie dans <white><s></white> s", "s", game.relance()));
        } else if (game.relanceEnAttente()) {
            body.add(t("coffre.relance-attente", "<yellow>Relance en attente (joueurs ou réglages)"));
        }
        if (!game.resume().isEmpty() && !game.matchInProgress()) {
            body.add(t("coffre.resume", "<gold>Résumé de la manche"));
            int rang = 1;
            for (GameInstance.Resultat r : game.resume()) {
                if (rang > 10) {
                    break;
                }
                body.add(t("coffre.resume-ligne", "<gray><rang>. <white><joueur></white> <dark_gray>: <gold>+<points> pts",
                        "rang", rang++, "joueur", r.nom(), "points", fr.kalium.scoreboards.data.StatsService.formatPoints(r.points())));
            }
        }
        // 1.20.0 : informations et boutons fournis par le jeu lui-meme (ex. entrainement du Parcours, KG_Parkour).
        body.addAll(game.menuInfo(player));
        for (GameInstance.MenuAction action : game.menuActions(player)) {
            buttons.add(gui.button(action.label(), null, p -> {
                if (plugin.instances().of(p) == game) {
                    action.action().accept(p);
                }
            }));
        }

        boolean betweenRounds = !game.isPublic() && game.phase() == GameInstance.Phase.WAITING;
        if (betweenRounds && game.minigame().getBool("allow-spectate", true)) {
            buttons.add(gui.button(t("coffre.passer-spectateur", "<aqua>Passer spectateur"),
                    t("coffre.passer-spectateur-tip", "<gray>Regarder la prochaine manche sans la jouer."), p -> {
                        Component error = plugin.instances().playerToSpectator(p);
                        if (error != null) {
                            p.sendMessage(plugin.prefix().append(error));
                        }
                    }));
        }
        if (betweenRounds && game.isHost(uuid)) {
            buttons.add(gui.button(t("coffre.changer-reglages", "<white>Changer les réglages"),
                    t("coffre.changer-reglages-tip", "<gray>Le compte à rebours s'arrête pendant que vous réglez la partie."), p -> {
                        GameInstance current = plugin.instances().of(p);
                        if (current != null) {
                            plugin.coffres().modifier(p, current);
                        }
                    }));
            if (game.relanceEnAttente()) {
                buttons.add(gui.button(t("coffre.reprendre", "<green>Reprendre le compte à rebours"), null, p -> {
                    GameInstance current = plugin.instances().of(p);
                    if (current != null && current.isHost(p.getUniqueId())) {
                        current.lancerCompteARebours();
                    }
                }));
            }
            if (game.members().size() > 1) {
                buttons.add(gui.button(t("host.players", "<white>Joueurs de la partie"),
                        t("host.players-tip", "<gray>Nommer un autre hôte, ou exclure un joueur."), this::openHostPlayers));
            }
        }
        if (!game.isPublic() && game.isHost(uuid) && game.phase() == GameInstance.Phase.WAITING) {
            buttons.add(gui.button(game.relance() > 0 ? t("coffre.lancer-maintenant", "<green><bold>Lancer maintenant")
                    : t("game.start", "<green><bold>Lancer la partie"), null, p -> {
                GameInstance current = plugin.instances().of(p);
                if (current != null) {
                    Component error = current.requestStart(p);
                    if (error != null) {
                        p.sendMessage(plugin.prefix().append(error));
                    }
                }
            }));
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
}
