package fr.kalium.scoreboards.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 1.3.0 : journal des parties (donnees brutes pour les graphiques en jeu et le futur bot Discord, qui ne parlera qu'a
 * KG_ScoreBoards : toutes les donnees des jeux centralisees ici, dans un format clair et stable).
 *
 * Un fichier par mois et par mini-jeu : {@code plugins/KG_ScoreBoards/journal/<aaaa-mm>/<mini-jeu>.jsonl}, une ligne
 * JSON par evenement (format « JSON Lines »). Chaque ligne commence par les memes champs :
 * <ul>
 *   <li>{@code v} : version du format de ligne (1) ;</li>
 *   <li>{@code type} : genre d'evenement, choisi par le jeu (ex. « lap », « race ») ;</li>
 *   <li>{@code at} : date et heure (ISO 8601, fuseau de stats.timezone) ;</li>
 *   <li>{@code game} : identifiant du mini-jeu ;</li>
 * </ul>
 * puis les champs propres a l'evenement, fournis par le jeu (voir KG_BoatRace pour « lap » et « race »). Ecriture en
 * arriere-plan, une seule a la fois et dans l'ordre d'arrivee ; jamais d'ecriture sur le thread principal.
 */
public final class GameLog {

    /** Version du format de ligne : a augmenter si un champ commun change de sens. */
    public static final int FORMAT = 1;

    private static final Pattern SAFE = Pattern.compile("[^a-z0-9_-]");

    private final File root;
    private final Supplier<ZoneId> zone;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "KG_ScoreBoards-journal");
        thread.setDaemon(true);
        return thread;
    });

    public GameLog(File dataFolder, Supplier<ZoneId> zone, Logger logger) {
        this.root = new File(dataFolder, "journal");
        this.zone = zone;
        this.logger = logger;
    }

    /**
     * Ajoute un evenement au journal du mini-jeu. fields : champs propres a l'evenement (valeurs simples, listes ou
     * dictionnaires), dans l'ordre voulu. Appelable depuis n'importe quel thread.
     */
    public void record(String game, String type, Map<String, Object> fields) {
        ZoneId zoneId = zone.get();
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("v", FORMAT);
        line.put("type", type);
        line.put("at", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        line.put("game", game);
        line.putAll(fields);
        String json = gson.toJson(line);
        String month = YearMonth.from(now).toString();
        String file = SAFE.matcher(game.toLowerCase(java.util.Locale.ROOT)).replaceAll("_") + ".jsonl";
        writer.execute(() -> append(new File(new File(root, month), file), json));
    }

    private void append(File file, String json) {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (BufferedWriter out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                out.write(json);
                out.newLine();
            }
        } catch (IOException e) {
            logger.warning("Journal des parties : écriture impossible dans " + file + " : " + e.getMessage());
        }
    }

    /** Arret du serveur : termine les ecritures en attente (5 s maximum). */
    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Journal des parties : écritures en attente abandonnées à l'arrêt.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
