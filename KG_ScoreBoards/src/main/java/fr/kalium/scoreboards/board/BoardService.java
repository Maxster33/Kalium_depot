package fr.kalium.scoreboards.board;

import fr.kalium.scoreboards.Category;
import fr.kalium.scoreboards.KGScoreBoards;
import fr.kalium.scoreboards.data.StatsService;
import fr.kalium.scoreboards.data.StatsService.Row;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Classements affiches dans le hub : un panneau de texte flottant (entite d'affichage) par mini-jeu et par
 * type (general / du mois), place par un moderateur depuis le menu Parametres. Le texte se met a jour tout seul.
 * KG_ScoreBoards 1.0.0 : deplace de KalGames 1.13.0 ; le nom et le type du mini-jeu viennent du classement (Category)
 * fourni par le plugin qui l'utilise.
 */
public final class BoardService implements Listener {

    public static final int TOP = 10;
    private static final String TAG = "kalgames_board:";

    private final KGScoreBoards plugin;
    private final File file;
    private final Map<String, Location> boards = new LinkedHashMap<>();
    /** Entrees dont le monde est introuvable au chargement : conservees telles quelles (et leurs panneaux laisses en place). */
    private final Map<String, String> unresolved = new LinkedHashMap<>();
    private BukkitTask pending;
    private BukkitTask timer;

    public BoardService(KGScoreBoards plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "boards.yml");
    }

    public static String key(String minigameId, boolean monthly) {
        return key(minigameId, monthly, false);
    }

    /** Cle d'un panneau : mini-jeu + (general | mois), et lap = classement des meilleurs temps sur 1 tour. */
    public static String key(String minigameId, boolean monthly, boolean lap) {
        return minigameId + ":" + (lap ? "lap-" : "") + (monthly ? "month" : "all");
    }

    // ------------------------------------------------------------------ chargement

    public void load() {
        boards.clear();
        unresolved.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("boards");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String[] parts = section.getString(key, "").split(";");
            if (parts.length != 4) {
                continue;
            }
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                plugin.getLogger().warning("Classement " + key + " : monde introuvable (" + parts[0] + ").");
                unresolved.put(key, section.getString(key, ""));
                continue;
            }
            try {
                boards.put(key, new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
            } catch (NumberFormatException ignored) {
                // position invalide : ignoree
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, String> entry : unresolved.entrySet()) {
            yaml.set("boards." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Location> entry : boards.entrySet()) {
            Location l = entry.getValue();
            yaml.set("boards." + entry.getKey(), String.format(Locale.ROOT, "%s;%.2f;%.2f;%.2f", l.getWorld().getName(), l.getX(), l.getY(), l.getZ()));
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer boards.yml : " + e.getMessage());
        }
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        stop();
        timer = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 40L, 20L * 60);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (pending != null) {
            pending.cancel();
            pending = null;
        }
    }

    // ------------------------------------------------------------------ gestion

    public Location location(String minigameId, boolean monthly) {
        return location(minigameId, monthly, false);
    }

    public Location location(String minigameId, boolean monthly, boolean lap) {
        Location l = boards.get(key(minigameId, monthly, lap));
        return l == null ? null : l.clone();
    }

    /** Place (ou deplace) un classement a cette position. */
    public void place(String minigameId, boolean monthly, Location location) {
        place(minigameId, monthly, false, location);
    }

    public void place(String minigameId, boolean monthly, boolean lap, Location location) {
        String key = key(minigameId, monthly, lap);
        unresolved.remove(key);
        Location old = boards.put(key, location.clone());
        if (old != null) {
            removeEntities(key, old);
        }
        save();
        refresh(key);
    }

    public void remove(String minigameId, boolean monthly) {
        remove(minigameId, monthly, false);
    }

    public void remove(String minigameId, boolean monthly, boolean lap) {
        String key = key(minigameId, monthly, lap);
        Location old = boards.remove(key);
        if (old != null) {
            removeEntities(key, old);
            save();
        }
    }

    public void removeAllOf(String minigameId) {
        remove(minigameId, false, false);
        remove(minigameId, true, false);
        remove(minigameId, false, true);
        remove(minigameId, true, true);
    }

    // ------------------------------------------------------------------ affichage

    /** Temps affiche a cote des points : meilleur temps sur 1 tour (course de bateau) ou meilleur temps de course (parcours). -1 = aucun. */
    public long shownTime(Category category, Row row) {
        if (category == null) {
            return -1;
        }
        return switch (category.kind()) {
            case LAP -> row.bestLapMs();
            case TIME -> row.bestMs();
            default -> -1;
        };
    }

    /** Lignes de classement (rang, joueur, points, meilleur temps eventuel). */
    public Component rows(List<Row> rows, int firstRank, Category category) {
        List<Component> lines = new ArrayList<>();
        int rank = firstRank;
        for (Row row : rows) {
            long time = shownTime(category, row);
            boolean showTime = time >= 0;
            lines.add(plugin.t(showTime ? "board.line-time" : "board.line",
                    showTime ? "<yellow><rank>.</yellow> <white><name></white> <dark_gray>-</dark_gray> <green><points> pts</green> <dark_gray>-</dark_gray> <aqua><time></aqua>"
                            : "<yellow><rank>.</yellow> <white><name></white> <dark_gray>-</dark_gray> <green><points> pts</green>",
                    "rank", rank, "name", row.name(), "points", row.pointsText(),
                    "time", showTime ? StatsService.formatTime(time) : ""));
            rank++;
        }
        if (lines.isEmpty()) {
            lines.add(plugin.t("board.empty", "<gray>Aucun joueur classé pour le moment."));
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    /** Lignes du classement des meilleurs temps sur 1 tour (rang, joueur, temps). */
    public Component lapRows(List<Row> rows, int firstRank) {
        List<Component> lines = new ArrayList<>();
        int rank = firstRank;
        for (Row row : rows) {
            lines.add(plugin.t("board.line-lap",
                    "<yellow><rank>.</yellow> <white><name></white> <dark_gray>-</dark_gray> <aqua><time></aqua>",
                    "rank", rank, "name", row.name(), "time", StatsService.formatTime(row.bestLapMs())));
            rank++;
        }
        if (lines.isEmpty()) {
            lines.add(plugin.t("board.empty-lap", "<gray>Aucun temps enregistré pour le moment."));
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    /** Ce mini-jeu a un classement des meilleurs temps sur 1 tour ? (courses de bateau) */
    public boolean hasLapTimes(Category category) {
        return category != null && category.kind() == Category.Kind.LAP;
    }

    public boolean hasTimes(Category category) {
        return category != null && (category.kind() == Category.Kind.TIME || category.kind() == Category.Kind.LAP);
    }

    /** Texte complet d'un panneau. */
    public Component render(String minigameId, boolean monthly) {
        return render(minigameId, monthly, false);
    }

    public Component render(String minigameId, boolean monthly, boolean lap) {
        Category minigame = plugin.category(minigameId);
        Component name = minigame == null ? Component.text(minigameId) : minigame.name();
        StatsService stats = plugin.stats();
        if (lap) {
            Component lapTitle = monthly
                    ? plugin.t("board.title-lap-month", "<gold><bold><minigame></bold></gold><newline><yellow>Top <n> meilleurs temps sur 1 tour du mois</yellow> <gray>(<month>)</gray>",
                            "minigame", name, "n", TOP, "month", StatsService.monthLabel(stats.monthKey()))
                    : plugin.t("board.title-lap-all", "<gold><bold><minigame></bold></gold><newline><yellow>Top <n> meilleurs temps sur 1 tour</yellow> <gray>(général)</gray>",
                            "minigame", name, "n", TOP);
            return Component.join(JoinConfiguration.newlines(), lapTitle, Component.empty(), lapRows(stats.lapTop(minigameId, monthly, TOP), 1));
        }
        Component title = monthly
                ? plugin.t("board.title-month", "<gold><bold><minigame></bold></gold><newline><yellow>Top <n> du mois</yellow> <gray>(<month>)</gray>",
                        "minigame", name, "n", TOP, "month", StatsService.monthLabel(stats.monthKey()))
                : plugin.t("board.title-all", "<gold><bold><minigame></bold></gold><newline><yellow>Top <n> général</yellow> <gray>(depuis le début)</gray>",
                        "minigame", name, "n", TOP);
        Component body = rows(stats.top(minigameId, monthly, TOP), 1, minigame);
        return Component.join(JoinConfiguration.newlines(), title, Component.empty(), body);
    }

    // ------------------------------------------------------------------ entites

    private TextDisplay find(String key, Location location) {
        World world = location.getWorld();
        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;
        if (world == null || !world.isChunkLoaded(cx, cz)) {
            return null;
        }
        Chunk chunk = world.getChunkAt(cx, cz);
        TextDisplay found = null;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof TextDisplay display && display.getScoreboardTags().contains(TAG + key)) {
                if (found == null) {
                    found = display;
                } else {
                    display.remove();
                }
            }
        }
        return found;
    }

    private void removeEntities(String key, Location location) {
        World world = location.getWorld();
        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;
        if (world == null || !world.isChunkLoaded(cx, cz)) {
            return;
        }
        for (Entity entity : world.getChunkAt(cx, cz).getEntities()) {
            if (entity instanceof TextDisplay display && display.getScoreboardTags().contains(TAG + key)) {
                display.remove();
            }
        }
    }

    private void style(TextDisplay display) {
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setLineWidth(400);
        display.setBackgroundColor(Color.fromARGB(110, 0, 0, 0));
        display.setSeeThrough(false);
        display.setShadowed(true);
        display.setViewRange(1.0f);
        float scale = (float) Math.max(0.3, Math.min(6.0, plugin.getConfig().getDouble("boards.scale", 1.3)));
        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
    }

    /** Texte du panneau d'une cle (mini-jeu:all | month | lap-all | lap-month). */
    private Component renderKey(String key) {
        int split = key.lastIndexOf(':');
        String kind = key.substring(split + 1);
        return render(key.substring(0, split), kind.endsWith("month"), kind.startsWith("lap-"));
    }

    /** Cree ou met a jour le panneau (uniquement si son chunk est charge). */
    public void refresh(String key) {
        Location location = boards.get(key);
        if (location == null || location.getWorld() == null) {
            return;
        }
        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;
        if (!location.getWorld().isChunkLoaded(cx, cz)) {
            return;
        }
        Chunk chunk = location.getWorld().getChunkAt(cx, cz);
        if (!chunk.isEntitiesLoaded()) {
            return;
        }
        Component text = renderKey(key);
        TextDisplay display = find(key, location);
        if (display == null) {
            display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
                entity.addScoreboardTag(TAG + key);
                entity.setPersistent(true);
                style(entity);
                entity.text(text);
            });
            return;
        }
        if (display.getLocation().distanceSquared(location) > 0.01) {
            display.teleport(location);
        }
        style(display);
        display.text(text);
    }

    public void refreshAll() {
        try {
            plugin.stats().checkRollover();
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Vérification du changement de mois impossible : " + e);
        }
        for (String key : new ArrayList<>(boards.keySet())) {
            try {
                refresh(key);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Mise à jour du classement " + key + " impossible : " + e);
            }
        }
    }

    /** Mise a jour groupee (evite de tout recalculer a chaque point). */
    public void refreshSoon() {
        if (pending != null || !plugin.isEnabled()) {
            return;
        }
        pending = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending = null;
            refreshAll();
        }, 100L);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : new ArrayList<>(event.getEntities())) {
            if (!(entity instanceof TextDisplay display)) {
                continue;
            }
            String tag = null;
            for (String candidate : display.getScoreboardTags()) {
                if (candidate.startsWith(TAG)) {
                    tag = candidate.substring(TAG.length());
                    break;
                }
            }
            if (tag == null) {
                continue;
            }
            Location configured = boards.get(tag);
            if (configured == null && unresolved.containsKey(tag)) {
                continue;
            }
            if (configured == null || configured.getWorld() != display.getWorld()
                    || configured.distanceSquared(display.getLocation()) > 4.0) {
                display.remove();
                continue;
            }
            display.text(renderKey(tag));
        }
    }
}
