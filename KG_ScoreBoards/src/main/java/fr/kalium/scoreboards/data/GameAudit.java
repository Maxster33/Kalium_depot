package fr.kalium.scoreboards.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 1.5.0 : verification des parties des derniers jours (demande de LeKiwi06, 25/09/2026, apres des points de course non
 * comptes : « que la façon dont tu as récupéré les games des dernières 24 h puisse vérifier les games des 7 derniers
 * jours »). Relit le journal des parties (voir GameLog) et liste ce qui n'a PAS ete compte dans les classements :
 * <ul>
 *   <li>« points » (KalGames : PvP Kit, Parcours, Rush) avec {@code counted: false} ;</li>
 *   <li>« time » (temps de parcours) avec {@code counted: false} ;</li>
 *   <li>course de bateau (KG_BoatRace) : tours sous le seuil des meilleurs temps et points de la course (cumul) d'un
 *       joueur dont les tours etaient {@code ranked: false}.</li>
 * </ul>
 * Les operateurs (au moment de la verification) sont ecartes : leurs points ne comptent jamais. Un element credite
 * (voir credit) est note dans le journal (evenement « credit ») et n'est plus propose ensuite.
 */
public final class GameAudit {

    /** Un element non compte : identifiant court (pour la commande), jeu, joueur, genre et valeur. */
    public record Missing(String id, OffsetDateTime at, String game, UUID player, String name, String kind, double value,
                          String reason) {
    }

    private final File journal;
    private final ZoneId zone;
    private final Predicate<UUID> operator;

    public GameAudit(File dataFolder, ZoneId zone, Predicate<UUID> operator) {
        this.journal = new File(dataFolder, "journal");
        this.zone = zone;
        this.operator = operator;
    }

    /** Elements non comptes des days derniers jours, du plus ancien au plus recent. */
    public List<Missing> scan(int days) throws IOException {
        OffsetDateTime since = OffsetDateTime.now(zone).minusDays(Math.max(1, days));
        List<JsonObject> lines = read(since);
        Set<String> credited = new HashSet<>();
        Map<String, Boolean> ranked = new HashMap<>(); // (partie|joueur) -> tours de course classes ?
        for (JsonObject line : lines) {
            String type = str(line, "type");
            if ("credit".equals(type)) {
                credited.add(str(line, "ref"));
            } else if ("lap".equals(type) && line.has("ranked")) {
                ranked.merge(str(line, "match") + "|" + str(line, "player"), line.get("ranked").getAsBoolean(), Boolean::logicalAnd);
            }
        }
        // Tours de course non classes : seul le MEILLEUR tour de chaque (partie, joueur) est propose (c'est le seul qui
        // peut compter pour un record).
        Map<String, JsonObject> bestLaps = new LinkedHashMap<>();
        for (JsonObject line : lines) {
            if ("lap".equals(str(line, "type")) && line.has("ranked") && !line.get("ranked").getAsBoolean()
                    && line.has("recorded") && line.get("recorded").getAsBoolean()) {
                bestLaps.merge(str(line, "match") + "|" + str(line, "player"), line,
                        (a, b) -> num(b, "lapMillis") < num(a, "lapMillis") ? b : a);
            }
        }
        List<Missing> result = new ArrayList<>();
        for (JsonObject line : lines) {
            String type = str(line, "type");
            OffsetDateTime at = OffsetDateTime.parse(str(line, "at"));
            String game = str(line, "game");
            switch (type == null ? "" : type) {
                case "points", "time" -> {
                    if (!line.has("counted") || line.get("counted").getAsBoolean() || "operateur".equals(str(line, "reason"))) {
                        continue;
                    }
                    double value = "points".equals(type) ? num(line, "points") : num(line, "millis");
                    add(result, credited, at, game, line, "points".equals(type) ? "points" : "temps", value, str(line, "reason"));
                }
                case "lap" -> {
                    if (bestLaps.get(str(line, "match") + "|" + str(line, "player")) != line) {
                        continue;
                    }
                    add(result, credited, at, game, line, "tour", num(line, "lapMillis"), "partie-non-classee");
                }
                case "race" -> {
                    if (!line.has("results")) {
                        continue;
                    }
                    for (JsonElement element : line.getAsJsonArray("results")) {
                        JsonObject one = element.getAsJsonObject();
                        Boolean wasRanked = ranked.get(str(line, "match") + "|" + str(one, "player"));
                        if (wasRanked == null || wasRanked || !one.has("cumulative")) {
                            continue;
                        }
                        JsonObject fake = new JsonObject();
                        fake.addProperty("match", str(line, "match"));
                        fake.addProperty("player", str(one, "player"));
                        fake.addProperty("name", str(one, "name"));
                        add(result, credited, at, game, fake, "points", num(one, "cumulative"), "partie-non-classee");
                    }
                }
                default -> {
                }
            }
        }
        return result;
    }

    private void add(List<Missing> result, Set<String> credited, OffsetDateTime at, String game, JsonObject line, String kind,
                     double value, String reason) {
        String playerText = str(line, "player");
        if (playerText == null || value <= 0) {
            return;
        }
        UUID player = UUID.fromString(playerText);
        if (operator.test(player)) {
            return; // les operateurs ne comptent jamais
        }
        String id = shortId(kind + "|" + game + "|" + str(line, "match") + "|" + playerText + "|" + at + "|" + value);
        if (credited.contains(id)) {
            return;
        }
        result.add(new Missing(id, at, game, player, str(line, "name"), kind, value, reason));
    }

    /** Lignes du journal depuis since (fichiers des mois concernes). */
    private List<JsonObject> read(OffsetDateTime since) throws IOException {
        List<JsonObject> lines = new ArrayList<>();
        YearMonth month = YearMonth.from(since);
        YearMonth last = YearMonth.now(zone);
        while (!month.isAfter(last)) {
            File[] files = new File(journal, month.toString()).listFiles((dir, name) -> name.endsWith(".jsonl"));
            if (files != null) {
                for (File file : files) {
                    for (String text : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                        if (text.isBlank()) {
                            continue;
                        }
                        try {
                            JsonObject line = JsonParser.parseString(text).getAsJsonObject();
                            if (line.has("at") && !OffsetDateTime.parse(line.get("at").getAsString()).isBefore(since)) {
                                lines.add(line);
                            }
                        } catch (RuntimeException ignored) {
                            // ligne illisible : ignoree
                        }
                    }
                }
            }
            month = month.plusMonths(1);
        }
        lines.sort((a, b) -> OffsetDateTime.parse(str(a, "at")).compareTo(OffsetDateTime.parse(str(b, "at"))));
        return lines;
    }

    /** Champs de l'evenement « credit » ecrit apres avoir credite un element. */
    public static Map<String, Object> creditFields(Missing missing, String by) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("ref", missing.id());
        fields.put("kind", missing.kind());
        fields.put("player", missing.player().toString());
        fields.put("name", missing.name());
        fields.put("value", missing.value());
        fields.put("originalAt", missing.at().toString());
        fields.put("by", by);
        return fields;
    }

    private static String str(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static double num(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? 0 : element.getAsDouble();
    }

    private static String shortId(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                out.append(String.format("%02x", hash[i]));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
