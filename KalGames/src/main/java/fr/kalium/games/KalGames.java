package fr.kalium.games;

import fr.kalium.games.command.KalGamesCommand;
import fr.kalium.games.data.KitLibrary;
import fr.kalium.games.data.Lang;
import fr.kalium.games.data.Repository;
import fr.kalium.games.game.HubService;
import fr.kalium.games.game.InstanceManager;
import fr.kalium.games.game.ItemService;
import fr.kalium.games.game.ScoreBridge;
import fr.kalium.games.gui.AdminMenus;
import fr.kalium.games.gui.Gui;
import fr.kalium.games.gui.PlayerMenus;
import fr.kalium.games.listener.ConnectionListener;
import fr.kalium.games.listener.GameListener;
import fr.kalium.games.listener.HubListener;
import fr.kalium.games.world.InstanceWorld;
import fr.kalium.games.world.TemplateService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** KalGames : hub, menus et parties instanciees pour le serveur Kal-Games. */
public final class KalGames extends JavaPlugin {

    private Lang lang;
    private KitLibrary kits;
    private Repository repository;
    private TemplateService templates;
    private InstanceWorld worlds;
    private InstanceManager instances;
    private HubService hub;
    private ItemService items;
    private ScoreBridge scores;
    private Gui gui;
    private PlayerMenus menus;
    private AdminMenus admin;
    /** 1.14.0 : les classements (donnees, panneaux, menus) sont dans le plugin KG_ScoreBoards. */
    private fr.kalium.scoreboards.KGScoreBoards ranking;
    private java.util.function.Function<String, fr.kalium.scoreboards.Category> categories;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        kits = new KitLibrary(this);
        kits.load();
        repository = new Repository(this);
        repository.load(kits);
        templates = new TemplateService(this);
        worlds = new InstanceWorld(this);
        items = new ItemService(this);
        // 1.14.0 : classements dans KG_ScoreBoards (depend: dans plugin.yml). KalGames lui fournit ses mini-jeux comme
        // classements (nom affiche, type : temps de parcours ou temps sur 1 tour), sa regle "moderateur" et le monde
        // des parties (pas de panneau de classement dedans).
        ranking = (fr.kalium.scoreboards.KGScoreBoards) getServer().getPluginManager().getPlugin("KG_ScoreBoards");
        categories = id -> {
            fr.kalium.games.model.Minigame minigame = repository.minigame(id);
            if (minigame == null) {
                return null;
            }
            fr.kalium.scoreboards.Category.Kind kind = switch (minigame.type()) {
                case BOAT_RACE -> fr.kalium.scoreboards.Category.Kind.LAP;
                case PARKOUR -> fr.kalium.scoreboards.Category.Kind.TIME;
                default -> fr.kalium.scoreboards.Category.Kind.POINTS;
            };
            return new fr.kalium.scoreboards.Category(id, lang.parse(minigame.display()), kind);
        };
        ranking.addCategories(categories, () -> repository.minigames().stream().map(fr.kalium.games.model.Minigame::id).toList());
        ranking.setAdminCheck(this::isAdmin);
        scores = new ScoreBridge(this);
        hub = new HubService(this);
        gui = new Gui(this);
        menus = new PlayerMenus(this, gui);
        admin = new AdminMenus(this, gui);
        instances = new InstanceManager(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new HubListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        // 1.11.0 : regles propres au Rush (PNJ marchands, lits, morts sans ecran de mort).
        getServer().getPluginManager().registerEvents(new fr.kalium.games.listener.RushListener(this), this);

        KalGamesCommand command = new KalGamesCommand(this);
        for (String name : new String[]{"kalgames", "hub"}) {
            PluginCommand pluginCommand = getCommand(name);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(command);
                pluginCommand.setTabCompleter(command);
            }
        }
        // 1.13.0 : le Bingo (menu, /bingo, lien avec le serveur Bingo) est dans le plugin separe KG_Bingo, qui
        // ajoute ses boutons aux menus via PlayerMenus.addGameEntry / AdminMenus.addSettingsEntry.

        // Le monde des instances est cree une fois le serveur completement demarre.
        Bukkit.getScheduler().runTask(this, () -> {
            if (!worlds.init()) {
                getLogger().severe("Impossible de créer le monde des parties : les mini-jeux sont indisponibles.");
            } else if (worlds.cleanRestart()) {
                // Arret precedent propre, meme disposition de grille : les arenes deja collees sont recuperees
                // (pas de rechargement), au lieu d'etre re-generees a chaque redemarrage.
                instances.restoreCells();
            }
            instances.start();
            // Pre-generation des arenes (course de bateau...) : apres le demarrage complet, un collage a la fois.
            // Ne complete que ce qui manque encore : les copies restaurees ci-dessus comptent deja.
            Bukkit.getScheduler().runTaskLater(this, instances::prewarm, 100L);
            // Monde des parties connu seulement maintenant : pas de panneau de classement dedans.
            ranking.setForbiddenWorld(w -> w == worlds.world());
            lang.saveIfNeeded();
        });
        BukkitTask save = Bukkit.getScheduler().runTaskTimer(this, () -> {
            lang.saveIfNeeded();
        }, 20L * 30, 20L * 30);
        // 1.15.0 : interfaces declarees a KLM_Menu (catalogue "Interfaces" de la boussole).
        getServer().getServicesManager().register(fr.kalium.menu.api.MenuSection.class,
                fr.kalium.menu.api.MenuSection.of(this, "hub", fr.kalium.menu.api.MenuSection.Audience.PLAYERS,
                        t("klm.hub", "<gold>Hub Kal-Games"), t("klm.hub-tip", "<gray>Mini-jeux : parties publiques et privées, classements."),
                        (p, back) -> menus.openMenuFor(p)),
                this, org.bukkit.plugin.ServicePriority.Normal);
        getServer().getServicesManager().register(fr.kalium.menu.api.MenuSection.class,
                new fr.kalium.menu.api.MenuSection() {
                    @Override
                    public String id() {
                        return "settings";
                    }

                    @Override
                    public org.bukkit.plugin.Plugin owner() {
                        return KalGames.this;
                    }

                    @Override
                    public net.kyori.adventure.text.Component title() {
                        return t("klm.settings", "<yellow>Paramètres Kal-Games");
                    }

                    @Override
                    public net.kyori.adventure.text.Component description() {
                        return t("klm.settings-tip", "<gray>Mini-jeux, arènes, kits, hub.");
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
                    public void open(Player player, java.util.function.Consumer<Player> back) {
                        admin.openHome(player);
                    }
                }, this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("KalGames actif : " + repository.minigames().size() + " mini-jeu(x), " + repository.arenas().size()
                + " arène(s), " + kits.all().size() + " kit(s).");
        if (save.isCancelled()) {
            getLogger().fine("Sauvegarde automatique des textes indisponible.");
        }
    }

    @Override
    public void onDisable() {
        if (instances != null) {
            instances.stop();
        }
        if (worlds != null) {
            // Sauvegarde le monde des instances et marque l'arret comme propre : les arenes deja collees
            // (voir instances.stop() ci-dessus, qui les a sauvegardees) seront restaurees au prochain demarrage.
            worlds.markClean();
        }
        if (ranking != null && categories != null) {
            ranking.removeCategories(categories);
        }
        if (lang != null) {
            lang.saveIfNeeded();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    /** Recharge config.yml, lang.yml, kits et mini-jeux (les parties en cours sont fermees). */
    public void reloadAll() {
        instances.closeAll(t("admin.reload-closed", "<yellow>Configuration rechargée : la partie est fermée."));
        reloadConfig();
        lang.load();
        kits.load();
        repository.load(kits);
        ranking.boards().load();
        ranking.boards().refreshAll();
        instances.prewarm();
    }

    // ------------------------------------------------------------------ acces

    public Lang lang() {
        return lang;
    }

    public KitLibrary kits() {
        return kits;
    }

    public Repository repository() {
        return repository;
    }

    public TemplateService templates() {
        return templates;
    }

    public InstanceWorld worlds() {
        return worlds;
    }

    public InstanceManager instances() {
        return instances;
    }

    public HubService hub() {
        return hub;
    }

    public ItemService items() {
        return items;
    }

    public ScoreBridge scores() {
        return scores;
    }

    /** Donnees des classements (KG_ScoreBoards). */
    public fr.kalium.scoreboards.data.StatsService stats() {
        return ranking.stats();
    }

    /** Classements : menus, panneaux (KG_ScoreBoards). */
    public fr.kalium.scoreboards.KGScoreBoards ranking() {
        return ranking;
    }

    public Gui gui() {
        return gui;
    }

    public PlayerMenus menus() {
        return menus;
    }

    public AdminMenus admin() {
        return admin;
    }

    // ------------------------------------------------------------------ utilitaires

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
        return player.hasPermission("kalgames.admin");
    }

    public void sync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(this, runnable);
        }
    }

    public BukkitTask later(long ticks, Runnable runnable) {
        return Bukkit.getScheduler().runTaskLater(this, runnable, ticks);
    }

    /** Envoie le joueur sur le lobby (canal BungeeCord, gere par Velocity). */
    public void connectLobby(Player player) {
        String server = getConfig().getString("lobby-server", "lobby");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            return;
        }
        player.sendMessage(prefix().append(t("lobby.connecting", "<gray>Connexion à <white><server><gray>…", "server", server)));
        player.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
    }
}
