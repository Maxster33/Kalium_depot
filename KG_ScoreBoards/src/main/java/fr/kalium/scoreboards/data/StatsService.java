package fr.kalium.scoreboards.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Statistiques des joueurs par mini-jeu : points cumules et meilleur temps, depuis la creation du serveur
 * ("general") et pour le mois en cours. Au changement de mois, le classement complet du mois ecoule est
 * archive (dossier archives/) puis le classement mensuel repart de zero.
 */
public final class StatsService {

    /** Une ligne de classement. */
    public static final class Row {
        private final UUID uuid;
        private String name;
        /** 1.4.0 : points decimaux (baremes a coefficients : x1,5...). */
        private double points;
        private long bestMs = -1;
        private long bestLapMs = -1;

        public Row(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID uuid() {
            return uuid;
        }

        public String name() {
            return name;
        }

        public double points() {
            return points;
        }

        /** 1.4.0 : points prets a afficher (voir formatPoints). */
        public String pointsText() {
            return formatPoints(points);
        }

        /** Meilleur temps en millisecondes, ou -1 s'il n'y en a pas. */
        public long bestMs() {
            return bestMs;
        }

        /** Meilleur temps sur 1 tour (courses de bateau) en millisecondes, ou -1 s'il n'y en a pas. */
        public long bestLapMs() {
            return bestLapMs;
        }

        /** Departage a points egaux : meilleur temps sur 1 tour (course de bateau), sinon meilleur temps de course. */
        long tieTime() {
            if (bestLapMs >= 0) {
                return bestLapMs;
            }
            return bestMs < 0 ? Long.MAX_VALUE : bestMs;
        }

        boolean inPointsRanking() {
            return points > 0 || bestMs >= 0;
        }
    }

    /** Une archive mensuelle. */
    public record Archive(String id, String month, long archivedAt) {
        /** Libelle lisible ("septembre 2026"). */
        public String label() {
            String text = month;
            try {
                YearMonth ym = YearMonth.parse(month);
                text = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + ym.getYear();
            } catch (RuntimeException ignored) {
                // libelle brut
            }
            int suffix = id.indexOf('_');
            return suffix < 0 ? text : text + " (" + id.substring(suffix + 1) + ")";
        }
    }

    /** Ordre d'un classement : points decroissants, puis meilleur temps, puis nom. */
    private static final Comparator<Row> ORDER = Comparator
            .comparingDouble((Row row) -> -row.points)
            .thenComparingLong(Row::tieTime)
            .thenComparing(row -> row.name.toLowerCase(Locale.ROOT));

    /** Ordre du classement des meilleurs temps sur 1 tour : temps croissant, puis nom. */
    private static final Comparator<Row> LAP_ORDER = Comparator
            .comparingLong((Row row) -> row.bestLapMs)
            .thenComparing(row -> row.name.toLowerCase(Locale.ROOT));

    /** Ce dont le service a besoin du plugin (permet de le tester hors serveur). */
    public interface Host {
        File dataFolder();

        /** Fuseau horaire configure (vide = celui du serveur). */
        String timezone();

        Logger logger();

        /** Ecrit en tache de fond (ou immediatement si le plugin est arrete). */
        void async(Runnable task);

        boolean enabled();
    }

    private final Host plugin;
    private Clock clock = Clock.systemUTC();
    private final File file;
    private final File archiveDir;
    private final Map<String, Map<UUID, Row>> all = new HashMap<>();
    private final Map<String, Map<UUID, Row>> monthly = new HashMap<>();
    private String monthKey;
    /** Parties privees classees du jour : jeu -> joueur -> nombre (remis a zero au changement de jour). */
    private final Map<String, Map<UUID, Integer>> privateToday = new HashMap<>();
    private String privateDay;
    private boolean dirty;
    private Runnable changeListener = () -> { };

    public StatsService(Host plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.dataFolder(), "stats.yml");
        this.archiveDir = new File(plugin.dataFolder(), "archives");
    }

    /** Horloge (tests). */
    public void clock(Clock value) {
        this.clock = value;
    }

    /** Appele apres chaque modification (mise a jour des affichages). */
    public void onChange(Runnable listener) {
        this.changeListener = listener == null ? () -> { } : listener;
    }

    // ------------------------------------------------------------------ temps

    public ZoneId zone() {
        String name = plugin.timezone();
        if (name != null && !name.isBlank()) {
            try {
                return ZoneId.of(name.trim());
            } catch (DateTimeException e) {
                plugin.logger().warning("stats.timezone invalide (" + name + ") : fuseau du serveur utilise.");
            }
        }
        return ZoneId.systemDefault();
    }

    private String currentMonth() {
        return YearMonth.now(clock.withZone(zone())).toString();
    }

    /** Mois du classement mensuel en cours (aaaa-mm). */
    public String monthKey() {
        return monthKey;
    }

    public static String monthLabel(String key) {
        return new Archive(key, key, 0).label();
    }

    /**
     * 1.4.0 : affichage des points (charte : 6 chiffres au plus, decimales jusqu'a un million, puis M / Md avec 3
     * decimales au plus). Ex. 3 ; 12,5 ; 1234,56 ; 123456 ; 1,235 M ; 2,5 Md.
     */
    public static String formatPoints(double points) {
        double abs = Math.abs(points);
        if (abs >= 1_000_000_000d) {
            return trim(String.format(Locale.FRANCE, "%.3f", points / 1_000_000_000d)) + " Md";
        }
        if (abs >= 999_999.5d) { // au-dela, l'arrondi donnerait 7 chiffres
            return trim(String.format(Locale.FRANCE, "%.3f", points / 1_000_000d)) + " M";
        }
        int integerDigits = abs < 1 ? 1 : (int) Math.floor(Math.log10(abs)) + 1;
        int decimals = Math.max(0, Math.min(2, 6 - integerDigits));
        return trim(String.format(Locale.FRANCE, "%." + decimals + "f", points));
    }

    private static String trim(String text) {
        if (!text.contains(",")) {
            return text;
        }
        text = text.replaceAll("0+$", "");
        return text.endsWith(",") ? text.substring(0, text.length() - 1) : text;
    }

    public static String formatTime(long millis) {
        long minutes = millis / 60000;
        long seconds = millis / 1000 % 60;
        long hundredths = millis / 10 % 100;
        return String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, hundredths);
    }

    // ------------------------------------------------------------------ chargement / enregistrement

    public void load() {
        all.clear();
        monthly.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        monthKey = yaml.getString("month", currentMonth());
        readSection(yaml.getConfigurationSection("all"), all);
        readSection(yaml.getConfigurationSection("monthly"), monthly);
        privateToday.clear();
        privateDay = yaml.getString("private-games.day", null);
        ConfigurationSection counts = yaml.getConfigurationSection("private-games.counts");
        if (counts != null) {
            for (String minigame : counts.getKeys(false)) {
                ConfigurationSection players = counts.getConfigurationSection(minigame);
                if (players == null) {
                    continue;
                }
                Map<UUID, Integer> map = new LinkedHashMap<>();
                for (String key : players.getKeys(false)) {
                    try {
                        map.put(UUID.fromString(key), players.getInt(key, 0));
                    } catch (IllegalArgumentException ignored) {
                        // identifiant invalide : ligne ignoree
                    }
                }
                privateToday.put(minigame, map);
            }
        }
        dirty = false;
        checkRollover();
    }

    private void readSection(ConfigurationSection section, Map<String, Map<UUID, Row>> target) {
        if (section == null) {
            return;
        }
        for (String minigame : section.getKeys(false)) {
            ConfigurationSection players = section.getConfigurationSection(minigame);
            if (players == null) {
                continue;
            }
            Map<UUID, Row> rows = new LinkedHashMap<>();
            for (String key : players.getKeys(false)) {
                ConfigurationSection s = players.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(key);
                    Row row = new Row(uuid, s.getString("name", "?"));
                    row.points = s.getDouble("points", 0);
                    row.bestMs = s.getLong("best-ms", -1L);
                    row.bestLapMs = s.getLong("best-lap-ms", -1L);
                    rows.put(uuid, row);
                } catch (IllegalArgumentException ignored) {
                    // identifiant invalide : ligne ignoree
                }
            }
            target.put(minigame, rows);
        }
    }

    private void writeSection(YamlConfiguration yaml, String path, Map<String, Map<UUID, Row>> source) {
        for (Map.Entry<String, Map<UUID, Row>> entry : source.entrySet()) {
            for (Row row : entry.getValue().values()) {
                String base = path + "." + entry.getKey() + "." + row.uuid;
                yaml.set(base + ".name", row.name);
                yaml.set(base + ".points", row.points);
                if (row.bestMs >= 0) {
                    yaml.set(base + ".best-ms", row.bestMs);
                }
                if (row.bestLapMs >= 0) {
                    yaml.set(base + ".best-lap-ms", row.bestLapMs);
                }
            }
        }
    }

    private String snapshot() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("month", monthKey);
        writeSection(yaml, "all", all);
        writeSection(yaml, "monthly", monthly);
        if (privateDay != null) {
            yaml.set("private-games.day", privateDay);
            for (Map.Entry<String, Map<UUID, Integer>> entry : privateToday.entrySet()) {
                for (Map.Entry<UUID, Integer> count : entry.getValue().entrySet()) {
                    yaml.set("private-games.counts." + entry.getKey() + "." + count.getKey(), count.getValue());
                }
            }
        }
        return yaml.saveToString();
    }

    /** Enregistre si des changements sont en attente (ecriture asynchrone sauf si sync). */
    public void saveIfNeeded(boolean sync) {
        if (!dirty) {
            return;
        }
        dirty = false;
        String data = snapshot();
        Runnable write = () -> writeFile(file, data);
        if (sync || !plugin.enabled()) {
            write.run();
        } else {
            plugin.async(write);
        }
    }

    private void writeFile(File target, String data) {
        try {
            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            File temp = new File(target.getPath() + ".tmp");
            Files.writeString(temp.toPath(), data, StandardCharsets.UTF_8);
            Files.move(temp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.logger().warning("Impossible d'enregistrer " + target.getName() + " : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ ecriture des stats

    private Row row(Map<String, Map<UUID, Row>> source, String minigame, UUID uuid, String name) {
        Row row = source.computeIfAbsent(minigame, k -> new LinkedHashMap<>()).computeIfAbsent(uuid, k -> new Row(uuid, name));
        if (name != null && !name.isBlank()) {
            row.name = name;
        }
        return row;
    }

    /** Ajoute des points au joueur (classement general et du mois). */
    public void addPoints(String minigame, UUID uuid, String name, int points) {
        addPoints(minigame, uuid, name, (double) points);
    }

    /** 1.4.0 : points decimaux (arrondis au centieme). */
    public void addPoints(String minigame, UUID uuid, String name, double points) {
        points = Math.round(points * 100) / 100.0;
        if (points <= 0) {
            return;
        }
        checkRollover();
        row(all, minigame, uuid, name).points += points;
        row(monthly, minigame, uuid, name).points += points;
        changed();
    }

    /** Enregistre un temps : garde le meilleur (general et du mois). Renvoie true si c'est un record personnel. */
    public boolean recordTime(String minigame, UUID uuid, String name, long millis) {
        if (millis <= 0) {
            return false;
        }
        checkRollover();
        Row general = row(all, minigame, uuid, name);
        boolean record = general.bestMs < 0 || millis < general.bestMs;
        if (record) {
            general.bestMs = millis;
        }
        Row month = row(monthly, minigame, uuid, name);
        if (month.bestMs < 0 || millis < month.bestMs) {
            month.bestMs = millis;
        }
        changed();
        return record;
    }

    /** Enregistre un temps sur 1 tour : garde le meilleur (general et du mois). Renvoie true si c'est un record personnel. */
    public boolean recordLap(String minigame, UUID uuid, String name, long millis) {
        if (millis <= 0) {
            return false;
        }
        checkRollover();
        Row general = row(all, minigame, uuid, name);
        boolean record = general.bestLapMs < 0 || millis < general.bestLapMs;
        if (record) {
            general.bestLapMs = millis;
        }
        Row month = row(monthly, minigame, uuid, name);
        if (month.bestLapMs < 0 || millis < month.bestLapMs) {
            month.bestLapMs = millis;
        }
        changed();
        return record;
    }

    /** Efface le meilleur temps sur 1 tour d'un joueur (general et du mois) ; ses points ne changent pas. */
    public boolean removeLap(String minigame, UUID uuid) {
        boolean removed = false;
        for (Map<String, Map<UUID, Row>> source : List.of(all, monthly)) {
            Map<UUID, Row> rows = source.get(minigame);
            Row row = rows == null ? null : rows.get(uuid);
            if (row != null && row.bestLapMs >= 0) {
                row.bestLapMs = -1;
                removed = true;
            }
        }
        if (removed) {
            changed();
        }
        return removed;
    }

    /** Supprime les points et temps d'un joueur pour ce mini-jeu (classement general et du mois). Les archives restent. */
    public boolean removePlayer(String minigame, UUID uuid) {
        boolean removed = false;
        Map<UUID, Row> general = all.get(minigame);
        if (general != null && general.remove(uuid) != null) {
            removed = true;
        }
        Map<UUID, Row> month = monthly.get(minigame);
        if (month != null && month.remove(uuid) != null) {
            removed = true;
        }
        if (removed) {
            changed();
        }
        return removed;
    }

    // ------------------------------------------------------------------ parties privees du jour

    private void checkDay() {
        String today = LocalDate.now(clock.withZone(zone())).toString();
        if (!today.equals(privateDay)) {
            privateToday.clear();
            privateDay = today;
            dirty = true;
        }
    }

    /** Nombre de parties privees classees deja jouees aujourd'hui par ce joueur pour ce mini-jeu. */
    public int privateGamesToday(String minigame, UUID uuid) {
        checkDay();
        Map<UUID, Integer> map = privateToday.get(minigame);
        return map == null ? 0 : map.getOrDefault(uuid, 0);
    }

    /**
     * Compte une partie privee classee pour ce joueur. limit &lt;= 0 : pas de limite. Renvoie le nombre de parties
     * du jour apres ajout, ou -1 si la limite du jour est deja atteinte (la partie ne compte alors pas).
     */
    public int claimPrivateGame(String minigame, UUID uuid, int limit) {
        int used = privateGamesToday(minigame, uuid);
        if (limit > 0 && used >= limit) {
            return -1;
        }
        privateToday.computeIfAbsent(minigame, k -> new LinkedHashMap<>()).put(uuid, used + 1);
        dirty = true;
        return used + 1;
    }

    private void changed() {
        dirty = true;
        changeListener.run();
    }

    // ------------------------------------------------------------------ lecture

    private static List<Row> sorted(Map<UUID, Row> rows) {
        List<Row> list = new ArrayList<>();
        if (rows != null) {
            for (Row row : rows.values()) {
                if (row.inPointsRanking()) {
                    list.add(row);
                }
            }
        }
        list.sort(ORDER);
        return list;
    }

    private static List<Row> lapSorted(Map<UUID, Row> rows) {
        List<Row> list = new ArrayList<>();
        if (rows != null) {
            for (Row row : rows.values()) {
                if (row.bestLapMs >= 0) {
                    list.add(row);
                }
            }
        }
        list.sort(LAP_ORDER);
        return list;
    }

    private static boolean hasData(Map<UUID, Row> rows) {
        if (rows != null) {
            for (Row row : rows.values()) {
                if (row.inPointsRanking() || row.bestLapMs >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Classement complet des meilleurs temps sur 1 tour (courses de bateau). */
    public List<Row> lapRanking(String minigame, boolean month) {
        return lapSorted((month ? monthly : all).get(minigame));
    }

    public List<Row> lapTop(String minigame, boolean month, int limit) {
        List<Row> list = lapRanking(minigame, month);
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    /** Classement complet (tous les joueurs classes). */
    public List<Row> ranking(String minigame, boolean month) {
        return sorted((month ? monthly : all).get(minigame));
    }

    public List<Row> top(String minigame, boolean month, int limit) {
        List<Row> list = ranking(minigame, month);
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    /** Points et meilleur temps generaux d'un joueur. */
    public Row playerRow(String minigame, UUID uuid) {
        Map<UUID, Row> rows = all.get(minigame);
        return rows == null ? null : rows.get(uuid);
    }

    // ------------------------------------------------------------------ archives mensuelles

    /** Verifie le changement de mois (appelee regulierement) ; archive et remet a zero le classement du mois. */
    public void checkRollover() {
        String now = currentMonth();
        if (monthKey == null) {
            monthKey = now;
            dirty = true;
            return;
        }
        if (!monthKey.equals(now)) {
            archiveAndReset(now);
        }
    }

    /** Cloture manuelle : archive le mois en cours (avec son contenu actuel) et repart de zero. */
    public Archive closeMonthNow() {
        return archiveAndReset(monthKey == null ? currentMonth() : monthKey);
    }

    private Archive archiveAndReset(String newMonthKey) {
        String closing = monthKey == null ? newMonthKey : monthKey;
        Archive result = null;
        boolean any = false;
        for (Map<UUID, Row> rows : monthly.values()) {
            if (hasData(rows)) {
                any = true;
                break;
            }
        }
        if (any) {
            result = writeArchive(closing);
        }
        monthly.clear();
        monthKey = newMonthKey;
        dirty = true;
        saveIfNeeded(true);
        plugin.logger().info("Classements mensuels de " + closing + " archivés"
                + (result == null ? " (aucun joueur classé)." : " (" + result.id() + ").") + " Nouveau mois : " + newMonthKey + ".");
        changeListener.run();
        return result;
    }

    private Archive writeArchive(String month) {
        String id = month;
        int n = 2;
        while (new File(archiveDir, id + ".yml").exists()) {
            id = month + "_" + n++;
        }
        long now = Instant.now().toEpochMilli();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("month", month);
        yaml.set("archived-at", now);
        for (Map.Entry<String, Map<UUID, Row>> entry : monthly.entrySet()) {
            List<Row> ranked = new ArrayList<>();
            for (Row row : entry.getValue().values()) {
                if (row.inPointsRanking() || row.bestLapMs >= 0) {
                    ranked.add(row);
                }
            }
            if (ranked.isEmpty()) {
                continue;
            }
            ranked.sort(ORDER);
            List<Map<String, Object>> lines = new ArrayList<>();
            for (Row row : ranked) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("uuid", row.uuid.toString());
                line.put("name", row.name);
                line.put("points", row.points);
                if (row.bestMs >= 0) {
                    line.put("best-ms", row.bestMs);
                }
                if (row.bestLapMs >= 0) {
                    line.put("best-lap-ms", row.bestLapMs);
                }
                lines.add(line);
            }
            yaml.set("minigames." + entry.getKey(), lines);
        }
        writeFile(new File(archiveDir, id + ".yml"), yaml.saveToString());
        return new Archive(id, month, now);
    }

    /** Archives disponibles, la plus recente d'abord. */
    public List<Archive> archives() {
        List<Archive> list = new ArrayList<>();
        File[] files = archiveDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return list;
        }
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - 4);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
            list.add(new Archive(id, yaml.getString("month", id), yaml.getLong("archived-at", f.lastModified())));
        }
        list.sort(Comparator.comparingLong(Archive::archivedAt).reversed());
        return list;
    }

    /** Mini-jeux presents dans une archive. */
    public java.util.Set<String> archivedMinigames(String archiveId) {
        java.util.Set<String> ids = new TreeSet<>();
        ConfigurationSection section = YamlConfiguration.loadConfiguration(archiveFile(archiveId)).getConfigurationSection("minigames");
        if (section != null) {
            ids.addAll(section.getKeys(false));
        }
        return ids;
    }

    private File archiveFile(String archiveId) {
        // L'identifiant vient d'un listage de dossier : on interdit tout separateur de chemin.
        String safe = archiveId.replaceAll("[^0-9A-Za-z_-]", "");
        return new File(archiveDir, safe + ".yml");
    }

    private List<Row> archivedRows(String archiveId, String minigame) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archiveFile(archiveId));
        List<Row> rows = new ArrayList<>();
        for (Map<?, ?> line : yaml.getMapList("minigames." + minigame)) {
            try {
                UUID uuid = UUID.fromString(String.valueOf(line.get("uuid")));
                Row row = new Row(uuid, String.valueOf(line.get("name")));
                Object points = line.get("points");
                row.points = points instanceof Number number ? number.doubleValue() : 0;
                Object best = line.get("best-ms");
                row.bestMs = best instanceof Number number ? number.longValue() : -1L;
                Object lap = line.get("best-lap-ms");
                row.bestLapMs = lap instanceof Number number ? number.longValue() : -1L;
                rows.add(row);
            } catch (IllegalArgumentException ignored) {
                // ligne invalide
            }
        }
        return rows;
    }

    /** Classement complet archive d'un mini-jeu (points). */
    public List<Row> archived(String archiveId, String minigame) {
        List<Row> rows = new ArrayList<>();
        for (Row row : archivedRows(archiveId, minigame)) {
            if (row.inPointsRanking()) {
                rows.add(row);
            }
        }
        rows.sort(ORDER);
        return rows;
    }

    /** Classement archive des meilleurs temps sur 1 tour d'un mini-jeu. */
    public List<Row> archivedLaps(String archiveId, String minigame) {
        return lapSorted(toMap(archivedRows(archiveId, minigame)));
    }

    private static Map<UUID, Row> toMap(List<Row> rows) {
        Map<UUID, Row> map = new LinkedHashMap<>();
        for (Row row : rows) {
            map.put(row.uuid, row);
        }
        return map;
    }
}
