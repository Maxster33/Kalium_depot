package fr.kalium.scoreboards.api;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * 1.7.0 : lecture du journal des parties (voir GameLog) a partir d'un curseur, pour le bot Discord.
 *
 * Le journal est en ajout seul : {@code journal/<aaaa-mm>/<mini-jeu>.jsonl}. Le curseur retient le mois en cours de
 * lecture et, pour chaque fichier de ce mois, la position (en octets) deja lue ; les mois precedents sont lus en
 * entier. Il est rendu au bot sous forme opaque (base64 de {@code aaaa-mm|jeu=octets,jeu=octets}).
 * Seules les lignes completes (terminees par un saut de ligne) sont rendues : une ligne en cours d'ecriture sera lue
 * au prochain appel.
 */
public final class JournalReader {

    public record Line(String id, String json) {
    }

    public record Page(List<Line> lines, String cursor, boolean more) {
    }

    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern FILE = Pattern.compile("[a-z0-9_-]+\\.jsonl");

    private final File root;

    public JournalReader(File root) {
        this.root = root;
    }

    /** Lit au plus limit evenements apres le curseur (null ou vide : depuis le debut du journal). */
    public Page read(String cursorText, int limit) throws IOException {
        Cursor cursor = Cursor.decode(cursorText);
        List<Line> lines = new ArrayList<>();
        String[] months = root.list((dir, name) -> MONTH.matcher(name).matches() && new File(dir, name).isDirectory());
        if (months == null || months.length == 0) {
            return new Page(lines, cursor.encode(), false);
        }
        Arrays.sort(months);
        Cursor position = cursor;
        for (String month : months) {
            if (cursor.month != null && month.compareTo(cursor.month) < 0) {
                continue;
            }
            Map<String, Long> offsets = new TreeMap<>(month.equals(cursor.month) ? cursor.offsets : Map.of());
            String[] files = new File(root, month).list((dir, name) -> FILE.matcher(name).matches());
            if (files != null) {
                Arrays.sort(files);
                for (String file : files) {
                    String game = file.substring(0, file.length() - ".jsonl".length());
                    long offset = offsets.getOrDefault(game, 0L);
                    offsets.put(game, readFile(new File(new File(root, month), file), month, game, offset, lines, limit));
                    if (lines.size() >= limit) {
                        return new Page(lines, new Cursor(month, offsets).encode(), true);
                    }
                }
            }
            position = new Cursor(month, offsets);
        }
        return new Page(lines, position.encode(), false);
    }

    /** Ajoute les lignes completes du fichier a partir de offset ; renvoie la nouvelle position. */
    private static long readFile(File file, String month, String game, long offset, List<Line> out, int limit) throws IOException {
        long length = file.length();
        if (offset >= length) {
            return Math.min(offset, length);
        }
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            in.skipNBytes(offset);
            long position = offset;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);
            int b;
            while (out.size() < limit && (b = in.read()) != -1) {
                if (b != '\n') {
                    buffer.write(b);
                    continue;
                }
                long start = position;
                position += buffer.size() + 1L;
                String json = buffer.toString(StandardCharsets.UTF_8).trim();
                buffer.reset();
                if (!json.isEmpty()) {
                    out.add(new Line(month + "/" + game + "@" + start, json));
                }
            }
            return position;
        }
    }

    /** Curseur : mois en cours de lecture et positions des fichiers de ce mois. */
    private record Cursor(String month, Map<String, Long> offsets) {

        static Cursor decode(String text) {
            if (text == null || text.isBlank()) {
                return new Cursor(null, Map.of());
            }
            try {
                String raw = new String(Base64.getUrlDecoder().decode(text.trim()), StandardCharsets.UTF_8);
                int bar = raw.indexOf('|');
                String month = bar < 0 ? raw : raw.substring(0, bar);
                if (!MONTH.matcher(month).matches()) {
                    return new Cursor(null, Map.of());
                }
                Map<String, Long> offsets = new LinkedHashMap<>();
                if (bar >= 0 && bar + 1 < raw.length()) {
                    for (String pair : raw.substring(bar + 1).split(",")) {
                        int eq = pair.indexOf('=');
                        if (eq > 0) {
                            offsets.put(pair.substring(0, eq), Math.max(0, Long.parseLong(pair.substring(eq + 1))));
                        }
                    }
                }
                return new Cursor(month, offsets);
            } catch (RuntimeException e) {
                return new Cursor(null, Map.of());
            }
        }

        String encode() {
            if (month == null) {
                return "";
            }
            StringBuilder raw = new StringBuilder(month).append('|');
            boolean first = true;
            for (Map.Entry<String, Long> entry : offsets.entrySet()) {
                if (!first) {
                    raw.append(',');
                }
                raw.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
