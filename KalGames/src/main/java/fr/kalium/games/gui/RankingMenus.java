package fr.kalium.games.gui;

import fr.kalium.games.KalGames;
import fr.kalium.games.data.StatsService;
import fr.kalium.games.data.StatsService.Archive;
import fr.kalium.games.data.StatsService.Row;
import fr.kalium.games.game.BoardService;
import fr.kalium.games.model.Minigame;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Menus des classements : consultation par les joueurs, et outils des moderateurs (archives, panneaux du hub). */
public final class RankingMenus {

    private static final int PAGE = 15;

    private final KalGames plugin;
    private final Gui gui;

    public RankingMenus(KalGames plugin, Gui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    private Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

    private Component name(Minigame minigame) {
        return plugin.lang().parse(minigame.display());
    }

    private boolean guard(Player player) {
        if (!plugin.isAdmin(player)) {
            plugin.tell(player, "admin.denied", "<red>Réservé aux modérateurs.");
            return false;
        }
        return true;
    }

    private ActionButton adminButton(Component label, Component tip, Gui.Click click) {
        return gui.button(label, tip, p -> {
            if (guard(p)) {
                click.run(p);
            }
        });
    }

    // ------------------------------------------------------------------ joueurs

    /** Top 10 general ou du mois d'un mini-jeu. */
    public void openPlayer(Player player, Minigame minigame, boolean monthly) {
        openPlayer(player, minigame, monthly, false);
    }

    /** Top 10 general ou du mois ; lap = meilleurs temps sur 1 tour (courses de bateau). */
    public void openPlayer(Player player, Minigame minigame, boolean monthly, boolean lap) {
        StatsService stats = plugin.stats();
        BoardService boards = plugin.boards();
        boolean lapAvailable = boards.hasLapTimes(minigame);
        boolean showLap = lap && lapAvailable;
        List<Component> body = new ArrayList<>();
        UUID uuid = player.getUniqueId();
        if (showLap) {
            body.add(monthly
                    ? t("rank.title-lap-month", "<yellow>Meilleurs temps sur 1 tour du mois <gray>(<month>)", "month", StatsService.monthLabel(stats.monthKey()))
                    : t("rank.title-lap-all", "<yellow>Meilleurs temps sur 1 tour <gray>(depuis le début)"));
            body.add(boards.lapRows(stats.lapTop(minigame.id(), monthly, BoardService.TOP), 1));
            List<Row> laps = stats.lapRanking(minigame.id(), monthly);
            for (int i = 0; i < laps.size(); i++) {
                if (laps.get(i).uuid().equals(uuid)) {
                    body.add(t("rank.mine-lap", "<gray>Votre position : <white><rank></white> <dark_gray>(<time>)", "rank", i + 1,
                            "time", StatsService.formatTime(laps.get(i).bestLapMs())));
                    break;
                }
            }
        } else {
            body.add(monthly
                    ? t("rank.title-month", "<yellow>Top <n> du mois <gray>(<month>)", "n", BoardService.TOP, "month", StatsService.monthLabel(stats.monthKey()))
                    : t("rank.title-all", "<yellow>Top <n> général <gray>(depuis le début)", "n", BoardService.TOP));
            body.add(boards.rows(stats.top(minigame.id(), monthly, BoardService.TOP), 1, minigame));
            List<Row> all = stats.ranking(minigame.id(), monthly);
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).uuid().equals(uuid)) {
                    Row mine = all.get(i);
                    body.add(t("rank.mine", "<gray>Votre position : <white><rank></white> <dark_gray>(<points> pts)", "rank", i + 1, "points", mine.points()));
                    break;
                }
            }
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.button(monthly ? t("rank.show-all", "<gold>Voir le classement général") : t("rank.show-month", "<gold>Voir le classement du mois"),
                null, p -> openPlayer(p, minigame, !monthly, showLap)));
        if (lapAvailable) {
            buttons.add(gui.button(showLap ? t("rank.show-points", "<aqua>Voir le classement des points")
                            : t("rank.show-lap", "<aqua>Voir les meilleurs temps sur 1 tour"),
                    null, p -> openPlayer(p, minigame, monthly, !showLap)));
        }
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, p -> plugin.menus().openMinigame(p, minigame)));
        gui.open(player, t("rank.title", "<light_purple><bold>Classements <name>", "name", name(minigame)), body, List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ moderateurs

    public void openAdmin(Player player, Minigame minigame) {
        if (!guard(player)) {
            return;
        }
        StatsService stats = plugin.stats();
        List<Component> body = new ArrayList<>();
        body.add(t("rank.admin-body", "<gray>Mois en cours : <white><month></white> - <white><players></white> joueur(s) classé(s) ce mois, <white><all></white> au total.",
                "month", StatsService.monthLabel(stats.monthKey()),
                "players", stats.ranking(minigame.id(), true).size(), "all", stats.ranking(minigame.id(), false).size()));
        body.add(t("rank.admin-info", "<gray>À la fin du mois, le classement complet est archivé automatiquement puis remis à zéro."));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(adminButton(t("rank.admin-all", "<gold>Classement général (complet)"), null, p -> openFull(p, minigame, "all", 0)));
        buttons.add(adminButton(t("rank.admin-month", "<gold>Classement du mois (complet)"), null, p -> openFull(p, minigame, "month", 0)));
        if (plugin.boards().hasLapTimes(minigame)) {
            buttons.add(adminButton(t("rank.admin-lap-all", "<gold>Meilleurs temps sur 1 tour : général (complet)"), null, p -> openFull(p, minigame, "lap-all", 0)));
            buttons.add(adminButton(t("rank.admin-lap-month", "<gold>Meilleurs temps sur 1 tour : du mois (complet)"), null, p -> openFull(p, minigame, "lap-month", 0)));
        }
        buttons.add(adminButton(t("rank.admin-archives", "<aqua>Archives des mois passés"), null, p -> openArchives(p, minigame)));
        buttons.add(adminButton(t("rank.admin-boards", "<green>Panneaux dans le hub"),
                t("rank.admin-boards-tip", "<gray>Choisir où afficher le Top 10 dans le hub."), p -> openBoards(p, minigame)));
        buttons.add(adminButton(t("rank.admin-close-month", "<dark_red>Clôturer le mois maintenant"),
                t("rank.admin-close-month-tip", "<gray>Archive le mois en cours et remet le classement du mois à zéro."),
                p -> gui.confirm(p, t("rank.close-title", "<dark_red>Clôturer le mois ?"),
                        t("rank.close-body", "<gray>Tous les classements mensuels (tous les mini-jeux) seront archivés puis remis à zéro."),
                        q -> {
                            if (!guard(q)) {
                                return;
                            }
                            Archive archive = plugin.stats().closeMonthNow();
                            plugin.boards().refreshAll();
                            plugin.tell(q, "rank.closed", archive == null
                                    ? "<yellow>Aucun joueur classé : rien à archiver, le classement du mois est remis à zéro."
                                    : "<green>Mois archivé (<label>) et remis à zéro.", "label", archive == null ? "" : archive.label());
                            openAdmin(q, minigame);
                        }, q -> openAdmin(q, minigame))));
        buttons.add(adminButton(t("menu.back", "<gray>Retour"), null, p -> plugin.admin().openMinigame(p, minigame)));
        gui.open(player, t("rank.admin-title", "<light_purple><bold>Classements <name>", "name", name(minigame)), body, List.of(), buttons, gui.close(), 1);
    }

    /** Classement complet pagine. source : all, month, a:&lt;archive&gt; ; lap-all, lap-month, la:&lt;archive&gt; = temps sur 1 tour. */
    private void openFull(Player player, Minigame minigame, String source, int page) {
        if (!guard(player)) {
            return;
        }
        StatsService stats = plugin.stats();
        BoardService boards = plugin.boards();
        boolean lap = source.startsWith("lap-") || source.startsWith("la:");
        List<Row> rows;
        Component title;
        if (source.equals("all")) {
            rows = stats.ranking(minigame.id(), false);
            title = t("rank.full-all", "<yellow>Classement général");
        } else if (source.equals("month")) {
            rows = stats.ranking(minigame.id(), true);
            title = t("rank.full-month", "<yellow>Classement de <month> (en cours)", "month", StatsService.monthLabel(stats.monthKey()));
        } else if (source.equals("lap-all")) {
            rows = stats.lapRanking(minigame.id(), false);
            title = t("rank.full-lap-all", "<yellow>Meilleurs temps sur 1 tour : général");
        } else if (source.equals("lap-month")) {
            rows = stats.lapRanking(minigame.id(), true);
            title = t("rank.full-lap-month", "<yellow>Meilleurs temps sur 1 tour de <month> (en cours)", "month", StatsService.monthLabel(stats.monthKey()));
        } else {
            String id = source.substring(source.indexOf(':') + 1);
            rows = lap ? stats.archivedLaps(id, minigame.id()) : stats.archived(id, minigame.id());
            String label = id;
            for (Archive archive : stats.archives()) {
                if (archive.id().equals(id)) {
                    label = archive.label();
                }
            }
            title = lap ? t("rank.full-lap-archive", "<yellow>Archive (temps sur 1 tour) : <month>", "month", label)
                    : t("rank.full-archive", "<yellow>Archive : <month>", "month", label);
        }
        int pages = Math.max(1, (rows.size() + PAGE - 1) / PAGE);
        int current = Math.max(0, Math.min(pages - 1, page));
        int from = current * PAGE;
        List<Row> slice = rows.subList(Math.min(from, rows.size()), Math.min(rows.size(), from + PAGE));
        List<Component> body = new ArrayList<>();
        body.add(title);
        body.add(t("rank.full-count", "<gray><n> joueur(s) - page <p>/<pages>", "n", rows.size(), "p", current + 1, "pages", pages));
        body.add(lap ? boards.lapRows(slice, from + 1) : boards.rows(slice, from + 1, minigame));
        List<ActionButton> buttons = new ArrayList<>();
        if (current > 0) {
            buttons.add(adminButton(t("rank.prev", "<gray>← Page précédente"), null, p -> openFull(p, minigame, source, current - 1)));
        }
        if (current < pages - 1) {
            buttons.add(adminButton(t("rank.next", "<gray>Page suivante →"), null, p -> openFull(p, minigame, source, current + 1)));
        }
        if (source.equals("all") && !rows.isEmpty()) {
            buttons.add(adminButton(t("rank.remove-open", "<red>Supprimer un joueur du classement"),
                    t("rank.remove-open-tip", "<gray>Efface ses points et son temps pour ce jeu."), p -> openRemove(p, minigame, 0, false)));
        }
        if (source.equals("lap-all") && !rows.isEmpty()) {
            buttons.add(adminButton(t("rank.remove-lap-open", "<red>Supprimer un temps"),
                    t("rank.remove-lap-open-tip", "<gray>Efface le meilleur temps sur 1 tour d'un joueur (ses points ne changent pas)."),
                    p -> openRemove(p, minigame, 0, true)));
        }
        if (source.startsWith("a:") && boards.hasLapTimes(minigame)) {
            buttons.add(adminButton(t("rank.archive-show-lap", "<aqua>Voir les temps sur 1 tour"), null, p -> openFull(p, minigame, "la:" + source.substring(2), 0)));
        }
        if (source.startsWith("la:")) {
            buttons.add(adminButton(t("rank.archive-show-points", "<aqua>Voir les points"), null, p -> openFull(p, minigame, "a:" + source.substring(3), 0)));
        }
        buttons.add(adminButton(t("menu.back", "<gray>Retour"), null,
                p -> {
                    if (source.startsWith("a:") || source.startsWith("la:")) {
                        openArchives(p, minigame);
                    } else {
                        openAdmin(p, minigame);
                    }
                }));
        gui.open(player, t("rank.full-title", "<light_purple><bold><name>", "name", name(minigame)), body, List.of(), buttons, gui.close(), 1);
    }

    private static final int REMOVE_PAGE = 10;

    /** Choix du joueur dont on efface les points et le temps (classement general), ou son temps sur 1 tour (lap). */
    private void openRemove(Player player, Minigame minigame, int page, boolean lap) {
        if (!guard(player)) {
            return;
        }
        StatsService stats = plugin.stats();
        BoardService boards = plugin.boards();
        List<Row> rows = lap ? stats.lapRanking(minigame.id(), false) : stats.ranking(minigame.id(), false);
        int pages = Math.max(1, (rows.size() + REMOVE_PAGE - 1) / REMOVE_PAGE);
        int current = Math.max(0, Math.min(pages - 1, page));
        int from = current * REMOVE_PAGE;
        String back = lap ? "lap-all" : "all";
        List<ActionButton> buttons = new ArrayList<>();
        for (int i = from; i < Math.min(rows.size(), from + REMOVE_PAGE); i++) {
            Row row = rows.get(i);
            long shown = boards.shownTime(minigame, row);
            boolean showTime = !lap && shown >= 0;
            Component label = lap
                    ? t("rank.remove-line-lap", "<yellow><rank>.</yellow> <white><name></white> <gray>- <time>",
                            "rank", i + 1, "name", row.name(), "time", StatsService.formatTime(row.bestLapMs()))
                    : t(showTime ? "rank.remove-line-time" : "rank.remove-line",
                            showTime ? "<yellow><rank>.</yellow> <white><name></white> <gray>- <points> pts - <time>" : "<yellow><rank>.</yellow> <white><name></white> <gray>- <points> pts",
                            "rank", i + 1, "name", row.name(), "points", row.points(),
                            "time", showTime ? StatsService.formatTime(shown) : "");
            UUID target = row.uuid();
            String targetName = row.name();
            buttons.add(adminButton(label, null, p -> gui.confirm(p,
                    lap ? t("rank.remove-lap-title", "<dark_red>Supprimer le temps de <name> ?", "name", targetName)
                            : t("rank.remove-title", "<dark_red>Supprimer <name> du classement ?", "name", targetName),
                    lap ? t("rank.remove-lap-body", "<gray>Son meilleur temps sur 1 tour pour <name> sera effacé du classement général <white>et</white> du classement du mois. Ses points ne changent pas. Les archives ne changent pas.",
                            "name", name(minigame))
                            : t("rank.remove-body", "<gray>Ses points et son meilleur temps pour <name> seront effacés du classement général <white>et</white> du classement du mois pour ce jeu. Les archives des mois passés ne changent pas.",
                                    "name", name(minigame)),
                    q -> {
                        if (!guard(q)) {
                            return;
                        }
                        boolean removed = lap ? plugin.stats().removeLap(minigame.id(), target) : plugin.stats().removePlayer(minigame.id(), target);
                        plugin.boards().refreshAll();
                        plugin.tell(q, "rank.removed", removed ? "<green><name> a été retiré du classement." : "<yellow><name> n'était plus classé.",
                                "name", targetName);
                        openRemove(q, minigame, current, lap);
                    }, q -> openRemove(q, minigame, current, lap))));
        }
        if (current > 0) {
            buttons.add(adminButton(t("rank.prev", "<gray>← Page précédente"), null, p -> openRemove(p, minigame, current - 1, lap)));
        }
        if (current < pages - 1) {
            buttons.add(adminButton(t("rank.next", "<gray>Page suivante →"), null, p -> openRemove(p, minigame, current + 1, lap)));
        }
        buttons.add(adminButton(t("menu.back", "<gray>Retour"), null, p -> openFull(p, minigame, back, 0)));
        gui.open(player, t("rank.remove-menu-title", "<dark_red><bold>Supprimer un joueur"),
                List.of(t("rank.remove-menu-body", "<gray>Choisissez le joueur à retirer du classement de <name> <dark_gray>(page <p>/<pages>)",
                        "name", name(minigame), "p", current + 1, "pages", pages)),
                List.of(), buttons, gui.close(), 1);
    }

    private void openArchives(Player player, Minigame minigame) {
        if (!guard(player)) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        List<Archive> archives = plugin.stats().archives();
        for (Archive archive : archives) {
            boolean has = plugin.stats().archivedMinigames(archive.id()).contains(minigame.id());
            Component label = has
                    ? t("rank.archive-line", "<white><label>", "label", archive.label())
                    : t("rank.archive-line-empty", "<dark_gray><label> <gray>(aucun joueur)", "label", archive.label());
            buttons.add(adminButton(label, null, p -> openFull(p, minigame, "a:" + archive.id(), 0)));
        }
        buttons.add(adminButton(t("menu.back", "<gray>Retour"), null, p -> openAdmin(p, minigame)));
        gui.open(player, t("rank.archives-title", "<aqua><bold>Archives <name>", "name", name(minigame)),
                List.of(archives.isEmpty()
                        ? t("rank.archives-none", "<gray>Aucune archive pour le moment : la première sera créée à la fin du mois.")
                        : t("rank.archives-body", "<gray>Choisissez un mois pour consulter son classement complet.")),
                List.of(), buttons, gui.close(), 1);
    }

    // ------------------------------------------------------------------ panneaux du hub

    private String describe(Location location) {
        if (location == null) {
            return "non placé";
        }
        return String.format(Locale.ROOT, "%s %.1f, %.1f, %.1f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    private void openBoards(Player player, Minigame minigame) {
        if (!guard(player)) {
            return;
        }
        BoardService boards = plugin.boards();
        boolean laps = boards.hasLapTimes(minigame);
        List<Component> body = new ArrayList<>();
        body.add(t("rank.boards-info", "<gray>Placez-vous à l'endroit voulu dans le hub (le texte apparaît à hauteur de vos yeux), puis choisissez le bouton."));
        body.add(t("rank.boards-all", "<gray>Général : <white><where>", "where", describe(boards.location(minigame.id(), false))));
        body.add(t("rank.boards-month", "<gray>Du mois : <white><where>", "where", describe(boards.location(minigame.id(), true))));
        if (laps) {
            body.add(t("rank.boards-lap-all", "<gray>Temps sur 1 tour, général : <white><where>", "where", describe(boards.location(minigame.id(), false, true))));
            body.add(t("rank.boards-lap-month", "<gray>Temps sur 1 tour, du mois : <white><where>", "where", describe(boards.location(minigame.id(), true, true))));
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(adminButton(t("rank.boards-place-all", "<green>Placer le classement général ici"), null, p -> place(p, minigame, false, false)));
        buttons.add(adminButton(t("rank.boards-place-month", "<green>Placer le classement du mois ici"), null, p -> place(p, minigame, true, false)));
        if (laps) {
            buttons.add(adminButton(t("rank.boards-place-lap-all", "<green>Placer les temps sur 1 tour (général) ici"), null, p -> place(p, minigame, false, true)));
            buttons.add(adminButton(t("rank.boards-place-lap-month", "<green>Placer les temps sur 1 tour (du mois) ici"), null, p -> place(p, minigame, true, true)));
        }
        buttons.add(adminButton(t("rank.boards-remove-all", "<red>Retirer le classement général"), null, p -> {
            boards.remove(minigame.id(), false);
            openBoards(p, minigame);
        }));
        buttons.add(adminButton(t("rank.boards-remove-month", "<red>Retirer le classement du mois"), null, p -> {
            boards.remove(minigame.id(), true);
            openBoards(p, minigame);
        }));
        if (laps) {
            buttons.add(adminButton(t("rank.boards-remove-lap-all", "<red>Retirer les temps sur 1 tour (général)"), null, p -> {
                boards.remove(minigame.id(), false, true);
                openBoards(p, minigame);
            }));
            buttons.add(adminButton(t("rank.boards-remove-lap-month", "<red>Retirer les temps sur 1 tour (du mois)"), null, p -> {
                boards.remove(minigame.id(), true, true);
                openBoards(p, minigame);
            }));
        }
        buttons.add(adminButton(t("menu.back", "<gray>Retour"), null, p -> openAdmin(p, minigame)));
        gui.open(player, t("rank.boards-title", "<green><bold>Panneaux de classement"), body, List.of(), buttons, gui.close(), 1);
    }

    private void place(Player player, Minigame minigame, boolean monthly, boolean lap) {
        if (player.getWorld() == plugin.worlds().world()) {
            plugin.tell(player, "rank.boards-wrong-world", "<red>Placez le panneau dans le hub, pas dans le monde des parties.");
            return;
        }
        Location eye = player.getEyeLocation();
        Location where = new Location(eye.getWorld(), Math.round(eye.getX() * 10) / 10.0, Math.round(eye.getY() * 10) / 10.0,
                Math.round(eye.getZ() * 10) / 10.0);
        plugin.boards().place(minigame.id(), monthly, lap, where);
        plugin.tell(player, "rank.boards-placed", "<green>Panneau placé.");
        openBoards(player, minigame);
    }
}
