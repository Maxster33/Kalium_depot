package fr.kalium.bingo.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Reglages d'une partie choisis par l'hote dans le menu de creation (KG_Bingo, cote kal-games) - AJOUTE en
 * 0.3.0, demande explicite de LeKiwi06 (24/09/2026) :
 *
 * - mode BINGOS : il faut achever {@code bingosRequired} bingos (lignes, colonnes, diagonales au choix des
 *   joueurs, de 3 a 12) ; la 1re equipe qui y parvient gagne ; chrono actif (si le temps s'ecoule avant, le
 *   meilleur score gagne, ou "Egalite" si les meilleurs scores sont egaux) ;
 * - mode BLACKOUT : sans chrono, la 1re equipe qui valide toute la grille gagne (sauf abandon ou nulle) ;
 * - composition de la grille : nombre d'objectifs de chaque difficulte (total = nombre de cases). Defaut
 *   10 faciles, 10 normaux, 5 difficiles, 0 extreme ("pas d'extreme par defaut, trop dur pour des debutants").
 *
 * Transmis par kal-games sous forme de texte "mode=BINGOS;bingos=3;easy=10;medium=10;hard=5;extreme=0" (voir
 * encode / parse) : une valeur absente ou illisible reprend sa valeur par defaut, si bien qu'un KG_Bingo plus
 * ancien (qui n'envoie rien) donne simplement une partie par defaut.
 */
public record BingoSettings(Mode mode, int bingosRequired, int easy, int medium, int hard, int extreme) {

    public enum Mode {
        BINGOS, BLACKOUT
    }

    public static final int MIN_BINGOS = 3;
    public static final int MAX_BINGOS = 12;

    public static BingoSettings defaults() {
        return new BingoSettings(Mode.BINGOS, MIN_BINGOS, 10, 10, 5, 0);
    }

    public BingoSettings {
        if (mode == null) {
            mode = Mode.BINGOS;
        }
        bingosRequired = Math.max(MIN_BINGOS, Math.min(MAX_BINGOS, bingosRequired));
        easy = Math.max(0, easy);
        medium = Math.max(0, medium);
        hard = Math.max(0, hard);
        extreme = Math.max(0, extreme);
    }

    public boolean isBlackout() {
        return mode == Mode.BLACKOUT;
    }

    public int total() {
        return easy + medium + hard + extreme;
    }

    public String encode() {
        return "mode=" + mode + ";bingos=" + bingosRequired + ";easy=" + easy + ";medium=" + medium
                + ";hard=" + hard + ";extreme=" + extreme;
    }

    public static BingoSettings parse(String text) {
        BingoSettings d = defaults();
        if (text == null || text.isBlank()) {
            return d;
        }
        Map<String, String> fields = new HashMap<>();
        for (String part : text.split(";")) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                fields.put(part.substring(0, idx).trim(), part.substring(idx + 1).trim());
            }
        }
        Mode mode;
        try {
            mode = Mode.valueOf(fields.getOrDefault("mode", d.mode().name()).toUpperCase());
        } catch (IllegalArgumentException e) {
            mode = d.mode();
        }
        return new BingoSettings(mode,
                intOr(fields.get("bingos"), d.bingosRequired()),
                intOr(fields.get("easy"), d.easy()),
                intOr(fields.get("medium"), d.medium()),
                intOr(fields.get("hard"), d.hard()),
                intOr(fields.get("extreme"), d.extreme()));
    }

    private static int intOr(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Description courte pour les messages en jeu. */
    public String describe() {
        String grid = easy + " facile" + (easy > 1 ? "s" : "") + ", " + medium + " norma" + (medium > 1 ? "ux" : "l")
                + ", " + hard + " difficile" + (hard > 1 ? "s" : "") + ", " + extreme + " extrême" + (extreme > 1 ? "s" : "");
        return (isBlackout() ? "Blackout (grille complète, sans chrono)" : bingosRequired + " bingos") + " — " + grid;
    }
}
