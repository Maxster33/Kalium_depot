package fr.kalium.bingo.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.Color;

/**
 * Nom et couleur des equipes en partie (0.4.0) - demande explicite de LeKiwi06, 24/09/2026 : "equipe A : rouge,
 * equipe B : bleu, equipe C : jaune, equipe D : verte". Le numero interne (1 a 4) ne change pas : 1 = A, 2 = B...
 */
public final class TeamStyle {

    private static final String[] LETTERS = {"A", "B", "C", "D"};
    private static final NamedTextColor[] CHAT = {NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.YELLOW, NamedTextColor.GREEN};
    /** Couleurs de fond des cases sur la carte (voir GridMapRenderer). */
    private static final Color[] MAP = {new Color(210, 40, 40), new Color(50, 90, 220), new Color(240, 210, 40), new Color(60, 170, 60)};

    private TeamStyle() {
    }

    private static int index(int team) {
        return Math.floorMod(team - 1, LETTERS.length);
    }

    public static String letter(int team) {
        return LETTERS[index(team)];
    }

    public static NamedTextColor color(int team) {
        return CHAT[index(team)];
    }

    public static Color mapColor(int team) {
        return MAP[index(team)];
    }

    /** "Équipe A", en couleur. */
    public static Component name(int team) {
        return Component.text("Équipe " + letter(team), color(team));
    }

    /** "l'équipe A", en couleur. */
    public static Component nameLower(int team) {
        return Component.text("l'équipe " + letter(team), color(team));
    }
}
