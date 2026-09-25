package fr.kalium.scoreboards;

import fr.kalium.scoreboards.board.BoardService;
import fr.kalium.menu.api.Lang;
import fr.kalium.scoreboards.data.StatsService;
import fr.kalium.menu.api.Gui;
import fr.kalium.scoreboards.gui.RankingMenus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * KG_ScoreBoards (1.0.0) : les classements, sortis de KalGames 1.13.0 (demande de LeKiwi06, 24/09/2026 : un plugin =
 * un role). Donnees (stats.yml, archives/), panneaux flottants du hub (boards.yml) et menus de classement, a
 * l'identique de KalGames 1.13.0 pour les joueurs.
 *
 * Autonome (option 1 choisie par LeKiwi06) : il ne depend d'aucun autre plugin. Les plugins qui l'utilisent
 * (KalGames, puis KG_Bingo) :
 * - fournissent leurs classements (addCategories : identifiant -&gt; nom et type) ;
 * - enregistrent les points et les temps (stats()) ;
 * - ouvrent les menus de classement en indiquant ou revient le bouton "Retour" ;
 * - peuvent fournir la regle "moderateur" et les mondes interdits aux panneaux (setAdminCheck, setForbiddenWorld).
 */
public final class KGScoreBoards extends JavaPlugin {

    private Lang lang;
    private StatsService stats;
    /** 1.3.0 : journal des parties (voir GameLog). */
    private fr.kalium.scoreboards.data.GameLog gameLog;
    private BoardService boards;
    private Gui gui;
    private RankingMenus rankings;
    private final List<Function<String, Category>> providers = new ArrayList<>();
    /** 1.1.0 : identifiants des classements de chaque source (pour les lister dans le catalogue de KLM_Menu). */
    private final Map<Function<String, Category>, java.util.function.Supplier<java.util.Collection<String>>> lists = new java.util.LinkedHashMap<>();
    private Predicate<Player> adminCheck = p -> p.hasPermission("kalgames.admin");
    private Predicate<World> forbiddenWorld = w -> false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        stats = new StatsService(new StatsService.Host() {
            @Override
            public java.io.File dataFolder() {
                return getDataFolder();
            }

            @Override
            public String timezone() {
                return getConfig().getString("stats.timezone", "");
            }

            @Override
            public java.util.logging.Logger logger() {
                return getLogger();
            }

            @Override
            public void async(Runnable task) {
                Bukkit.getScheduler().runTaskAsynchronously(KGScoreBoards.this, task);
            }

            @Override
            public boolean enabled() {
                return isEnabled();
            }
        });
        stats.load();
        gameLog = new fr.kalium.scoreboards.data.GameLog(getDataFolder(), stats::zone, getLogger());
        boards = new BoardService(this);
        stats.onChange(boards::refreshSoon);
        gui = new Gui(this, lang); // 1.1.0 : boite a outils de KLM_Menu
        rankings = new RankingMenus(this, gui);

        // Panneaux : une fois le serveur completement demarre (mondes charges, plugins utilisateurs actifs).
        Bukkit.getScheduler().runTask(this, () -> {
            boards.load();
            boards.start();
            lang.saveIfNeeded();
        });
        BukkitTask save = Bukkit.getScheduler().runTaskTimer(this, () -> {
            lang.saveIfNeeded();
            stats.saveIfNeeded(false);
        }, 20L * 30, 20L * 30);
        // 1.2.0 : boutons fournis a KG_Menu (menu du serveur kal-games) au lieu de KLM_Menu directement : "Classements"
        // dans l'accueil, "Classements (modération)" dans les Parametres.
        getServer().getServicesManager().register(fr.kalium.kgmenu.api.MenuProvider.class, new fr.kalium.kgmenu.api.MenuProvider() {
            @Override
            public org.bukkit.plugin.Plugin owner() {
                return KGScoreBoards.this;
            }

            @Override
            public int order() {
                return 90;
            }

            @Override
            public List<Entry> games(Player player) {
                return List.of();
            }

            @Override
            public List<Entry> extras(Player player) {
                return List.of(new Entry("rankings", t("klm.rankings", "<light_purple>Classements"),
                        t("klm.rankings-tip", "<gray>Top 10 général et du mois de chaque jeu."), (p, back) -> openList(p, false, back)));
            }

            @Override
            public List<Entry> settings(Player player) {
                if (!isAdmin(player)) {
                    return List.of();
                }
                return List.of(new Entry("rankings-admin", t("klm.rankings-admin", "<light_purple>Classements (modération)"),
                        t("klm.rankings-admin-tip", "<gray>Classements complets, archives, panneaux du hub, clôture du mois."),
                        (p, back) -> openList(p, true, back)));
            }
        }, this, org.bukkit.plugin.ServicePriority.Normal);
        // 1.5.0 : /classements verifier | crediter (voir GameAudit).
        org.bukkit.command.PluginCommand audit = getCommand("classements");
        if (audit != null) {
            audit.setExecutor(this::onAudit);
        }
        getLogger().info("KG_ScoreBoards actif.");
    }

    // ------------------------------------------------------------------ 1.5.0 : verification des parties

    private fr.kalium.scoreboards.data.GameAudit audit() {
        return new fr.kalium.scoreboards.data.GameAudit(getDataFolder(), stats.zone(),
                uuid -> Bukkit.getOfflinePlayer(uuid).isOp() && getConfig().getBoolean("stats.exclude-operators", true));
    }

    private boolean onAudit(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0 || !(args[0].equalsIgnoreCase("verifier") || args[0].equalsIgnoreCase("crediter"))) {
            sender.sendMessage("/" + label + " verifier [jours]  - liste ce qui n'a pas ete compte (7 jours par defaut)");
            sender.sendMessage("/" + label + " crediter <id|tout> [jours]  - credite un element (ou tous) de la liste");
            return true;
        }
        boolean credit = args[0].equalsIgnoreCase("crediter");
        if (credit && args.length < 2) {
            sender.sendMessage("Precisez l'identifiant (voir /" + label + " verifier) ou « tout ».");
            return true;
        }
        int days = 7;
        String daysText = credit ? (args.length > 2 ? args[2] : null) : (args.length > 1 ? args[1] : null);
        if (daysText != null) {
            try {
                days = Math.max(1, Math.min(62, Integer.parseInt(daysText)));
            } catch (NumberFormatException e) {
                sender.sendMessage("Nombre de jours invalide : " + daysText);
                return true;
            }
        }
        int span = days;
        String target = credit ? args[1] : null;
        fr.kalium.scoreboards.data.GameAudit audit = audit();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            List<fr.kalium.scoreboards.data.GameAudit.Missing> missing;
            try {
                missing = audit.scan(span);
            } catch (java.io.IOException e) {
                Bukkit.getScheduler().runTask(this, () -> sender.sendMessage("Lecture du journal impossible : " + e.getMessage()));
                return;
            }
            Bukkit.getScheduler().runTask(this, () -> {
                if (!credit) {
                    showAudit(sender, missing, span, label);
                } else {
                    creditAudit(sender, missing, target);
                }
            });
        });
        return true;
    }

    private void showAudit(CommandSender sender, List<fr.kalium.scoreboards.data.GameAudit.Missing> missing, int days, String label) {
        if (missing.isEmpty()) {
            sender.sendMessage("Rien de non compte sur les " + days + " derniers jours.");
            return;
        }
        sender.sendMessage("Non compte sur les " + days + " derniers jours : " + missing.size() + " element(s).");
        java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");
        for (fr.kalium.scoreboards.data.GameAudit.Missing m : missing) {
            String value = m.kind().equals("points") ? fr.kalium.scoreboards.data.StatsService.formatPoints(m.value()) + " pts"
                    : fr.kalium.scoreboards.data.StatsService.formatTime((long) m.value());
            sender.sendMessage(" [" + m.id() + "] " + m.at().format(format) + " " + m.game() + " " + m.name() + " : " + m.kind()
                    + " " + value + " (" + m.reason() + ")");
        }
        sender.sendMessage("Pour crediter : /" + label + " crediter <id> ou /" + label + " crediter tout");
    }

    private void creditAudit(CommandSender sender, List<fr.kalium.scoreboards.data.GameAudit.Missing> missing, String target) {
        int count = 0;
        for (fr.kalium.scoreboards.data.GameAudit.Missing m : missing) {
            if (!target.equalsIgnoreCase("tout") && !target.equalsIgnoreCase(m.id())) {
                continue;
            }
            String name = m.name() == null ? "?" : m.name();
            switch (m.kind()) {
                case "points" -> stats.addPoints(m.game(), m.player(), name, m.value());
                case "temps" -> stats.recordTime(m.game(), m.player(), name, (long) m.value());
                case "tour" -> stats.recordLap(m.game(), m.player(), name, (long) m.value());
                default -> {
                    continue;
                }
            }
            log(m.game(), "credit", fr.kalium.scoreboards.data.GameAudit.creditFields(m, sender.getName()));
            count++;
        }
        boards.refreshSoon();
        sender.sendMessage(count == 0 ? "Aucun element correspondant (voir /classements verifier)."
                : count + " element(s) credite(s) dans les classements (general et du mois).");
    }

    @Override
    public void onDisable() {
        if (boards != null) {
            boards.stop();
        }
        if (stats != null) {
            stats.saveIfNeeded(true);
        }
        if (lang != null) {
            lang.saveIfNeeded();
        }
        if (gameLog != null) {
            gameLog.close();
        }
    }

    // ------------------------------------------------------------------ API pour les autres plugins

    /** Ajoute une source de classements (identifiant -&gt; classement, ou null si inconnu de cette source). */
    public void addCategories(Function<String, Category> provider) {
        providers.add(provider);
    }

    /** Idem, avec la liste de ses identifiants (1.1.0 : les classements apparaissent dans le catalogue de KLM_Menu). */
    public void addCategories(Function<String, Category> provider, java.util.function.Supplier<java.util.Collection<String>> ids) {
        providers.add(provider);
        lists.put(provider, ids);
    }

    public void removeCategories(Function<String, Category> provider) {
        providers.remove(provider);
        lists.remove(provider);
    }

    /** Tous les classements connus (dans l'ordre des sources). */
    public List<Category> categories() {
        List<Category> all = new ArrayList<>();
        for (Map.Entry<Function<String, Category>, java.util.function.Supplier<java.util.Collection<String>>> entry : lists.entrySet()) {
            for (String id : entry.getValue().get()) {
                Category category = entry.getKey().apply(id);
                if (category != null) {
                    all.add(category);
                }
            }
        }
        return all;
    }

    /** Classement de cet identifiant, ou null s'il n'est fourni par aucun plugin. */
    public Category category(String id) {
        for (Function<String, Category> provider : providers) {
            Category category = provider.apply(id);
            if (category != null) {
                return category;
            }
        }
        return null;
    }

    /** Regle "moderateur" pour les menus d'administration (par defaut : permission kalgames.admin, comme avant). */
    public void setAdminCheck(Predicate<Player> adminCheck) {
        this.adminCheck = adminCheck == null ? p -> p.hasPermission("kalgames.admin") : adminCheck;
    }

    /** Mondes ou l'on ne peut pas placer de panneau (ex. le monde des parties de KalGames). */
    public void setForbiddenWorld(Predicate<World> forbiddenWorld) {
        this.forbiddenWorld = forbiddenWorld == null ? w -> false : forbiddenWorld;
    }

    /**
     * 1.3.0 : ajoute un evenement au journal des parties du mini-jeu (voir GameLog : un fichier par mois et par
     * mini-jeu, une ligne JSON par evenement). Appele par les jeux (KG_BoatRace : « lap », « race »...).
     */
    public void log(String game, String type, Map<String, Object> fields) {
        if (gameLog != null) {
            gameLog.record(game, type, fields);
        }
    }

    public StatsService stats() {
        return stats;
    }

    public BoardService boards() {
        return boards;
    }

    /** Top 10 d'un classement pour un joueur ; back = bouton "Retour". */
    public void openPlayer(Player player, String categoryId, boolean monthly, Consumer<Player> back) {
        Category category = category(categoryId);
        if (category != null) {
            rankings.openPlayer(player, category, monthly, false, back);
        }
    }

    /** Menu moderateur d'un classement ; back = bouton "Retour". */
    public void openAdmin(Player player, String categoryId, Consumer<Player> back) {
        Category category = category(categoryId);
        if (category != null) {
            rankings.openAdmin(player, category, back);
        }
    }

    /** Liste des classements (catalogue de KLM_Menu) : joueur (Top 10) ou moderation. */
    private void openList(Player player, boolean admin, Consumer<Player> back) {
        List<io.papermc.paper.registry.data.dialog.ActionButton> buttons = new ArrayList<>();
        for (Category category : categories()) {
            Consumer<Player> here = p -> openList(p, admin, back);
            buttons.add(gui.button(category.name(), null, p -> {
                if (admin) {
                    rankings.openAdmin(p, category, here);
                } else {
                    rankings.openPlayer(p, category, false, false, here);
                }
            }));
        }
        if (back != null) {
            buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, back::accept));
        }
        gui.open(player, admin ? t("klm.rankings-admin", "<light_purple>Classements (modération)") : t("klm.rankings", "<light_purple>Classements"),
                List.of(buttons.size() > (back != null ? 1 : 0) ? t("klm.rankings-body", "<gray>Choisissez un jeu.")
                        : t("klm.rankings-none", "<gray>Aucun classement pour le moment.")),
                List.of(), buttons, null, 1);
    }

    /** Retire tous les panneaux d'un classement (ex. mini-jeu supprime). */
    public void removeBoardsOf(String categoryId) {
        boards.removeAllOf(categoryId);
    }

    // ------------------------------------------------------------------ outils internes (textes, droits)

    public Lang lang() {
        return lang;
    }

    public Component prefix() {
        return lang.c("prefix", "<gold><bold>KalGames</bold> <dark_gray>» ");
    }

    public Component t(String key, String def, Object... pairs) {
        return lang.c(key, def, pairs);
    }

    public void tell(CommandSender to, String key, String def, Object... pairs) {
        to.sendMessage(prefix().append(t(key, def, pairs)));
    }

    public boolean isAdmin(Player player) {
        return adminCheck.test(player);
    }

    public boolean isForbiddenWorld(World world) {
        return world != null && forbiddenWorld.test(world);
    }

    public void sync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(this, runnable);
        }
    }
}
