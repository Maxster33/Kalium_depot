package fr.kalium.core.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Petites fonctions de mise en forme pour l'affichage des statistiques et du commerce. */
public final class Format {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .withZone(ZoneId.systemDefault());

    private Format() {
    }

    /** "22/09 14:03" - utilise pour l'historique des ventes/achats et les ventes flash programmees. */
    public static String dateTime(long epochMillis) {
        return DATE_TIME.format(java.time.Instant.ofEpochMilli(epochMillis));
    }

    /** "0 min", "45 min", "3 h 24 min"... */
    public static String duration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (hours <= 0) {
            return minutes + " min";
        }
        return hours + " h " + minutes + " min";
    }

    /** Regroupe les milliers par un espace (12345 -> "12 345"), sans dependre d'une locale. */
    public static String number(long value) {
        String digits = Long.toString(Math.abs(value));
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            out.append(digits.charAt(i));
            count++;
            if (count % 3 == 0 && i != 0) {
                out.append(' ');
            }
        }
        String result = out.reverse().toString();
        return value < 0 ? "-" + result : result;
    }
}
