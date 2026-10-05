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

    /**
     * 0.8.5 - CHRONO (contre la montre, demande de Maxster33, 03/10/2026) : une seule equipe, 10 min au depart, + temps
     * par objectif valide ; victoire seulement si toute la grille est remplie avant la fin du chrono (bonus de
     * victoire habituel + 1 point par tranche de 20 s restantes) ; sinon defaite, les points des objectifs restent.
     * 0.10.0 (demande de Maxster33, 05/10/2026) : victoire avec le nombre de bingos choisi (bingosRequired, 3 a 12) au
     * lieu de toute la grille ; bonus de victoire + bonus de temps, inchanges ; les bingos rapportent comme dans toute
     * partie a une equipe (bareme des parties a plusieurs equipes, sans les bonus « en 1er »).
     */
    public enum Mode {
        BINGOS, BLACKOUT, CHRONO
    }

    /** Contre la montre : chrono de depart, temps gagne par objectif, secondes restantes par point de bonus. */
    public static final java.time.Duration CHRONO_START = java.time.Duration.ofMinutes(10);
    /**
     * 0.8.7 - temps gagne par objectif valide, selon sa difficulte (demande de Maxster33, 03/10/2026 ; avant : 5 min
     * pour tous) : facile 2 min 30, normal 4 min, difficile 6 min 30, extreme 10 min.
     */
    public static java.time.Duration chronoBonus(fr.kalium.bingo.grid.Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> java.time.Duration.ofSeconds(150);
            case MEDIUM -> java.time.Duration.ofMinutes(4);
            case HARD -> java.time.Duration.ofSeconds(390);
            case EXTREME -> java.time.Duration.ofMinutes(10);
        };
    }

    /** Duree lisible pour les messages : « 2 min 30 », « 4 min ». */
    public static String formatChrono(java.time.Duration duration) {
        long seconds = duration.getSeconds();
        return seconds % 60 == 0 ? (seconds / 60) + " min" : (seconds / 60) + " min " + String.format("%02d", seconds % 60);
    }
    public static final int CHRONO_SECONDS_PER_POINT = 20;

    public static final int MIN_BINGOS = 3;
    public static final int MAX_BINGOS = 12;
    /** 0.10.0 - partie a une seule equipe : 10 bingos au plus (demande de Maxster33, 05/10/2026). */
    public static final int MAX_BINGOS_SOLO = 10;

    /** 0.10.0 : memes reglages avec au plus {@code max} bingos a achever. */
    public BingoSettings withMaxBingos(int max) {
        return bingosRequired <= max ? this : new BingoSettings(mode, max, easy, medium, hard, extreme);
    }

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

    /** 0.8.5 : contre la montre. */
    public boolean isChrono() {
        return mode == Mode.CHRONO;
    }

    /** 0.8.5 : la victoire demande toute la grille (blackout ; 0.10.0 : plus le contre la montre). */
    public boolean needsFullGrid() {
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
        String rules = isBlackout() ? "Blackout (grille complète, sans chrono)"
                : isChrono() ? "Contre la montre (" + bingosRequired + " bingos ; 10 min, puis +2 min 30 à +10 min par objectif selon sa difficulté)"
                : bingosRequired + " bingos";
        return rules + " — " + grid;
    }
}
