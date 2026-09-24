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
        // 1.1.0 : interfaces declarees a KLM_Menu (catalogue "Interfaces" de la boussole).
        getServer().getServicesManager().register(fr.kalium.menu.api.MenuSection.class,
                fr.kalium.menu.api.MenuSection.of(this, "rankings", fr.kalium.menu.api.MenuSection.Audience.PLAYERS,
                        t("klm.rankings", "<light_purple>Classements"),
                        t("klm.rankings-tip", "<gray>Top 10 général et du mois de chaque jeu."),
                        (p, back) -> openList(p, false, back)),
                this, org.bukkit.plugin.ServicePriority.Normal);
        getServer().getServicesManager().register(fr.kalium.menu.api.MenuSection.class,
                new fr.kalium.menu.api.MenuSection() {
                    @Override
                    public String id() {
                        return "rankings-admin";
                    }

                    @Override
                    public org.bukkit.plugin.Plugin owner() {
                        return KGScoreBoards.this;
                    }

                    @Override
                    public Component title() {
                        return t("klm.rankings-admin", "<light_purple>Classements (modération)");
                    }

                    @Override
                    public Component description() {
                        return t("klm.rankings-admin-tip", "<gray>Classements complets, archives, panneaux du hub, clôture du mois.");
                    }

                    @Override
                    public Audience audience() {
                        return Audience.ADMINS;
                    }

                    @Override
                    public boolean visibleTo(Player player) {
                        return isAdmin(player);
                    }

                    @Override
                    public void open(Player player, Consumer<Player> back) {
                        openList(player, true, back);
                    }
                }, this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("KG_ScoreBoards actif.");
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
