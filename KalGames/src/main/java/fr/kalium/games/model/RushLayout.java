package fr.kalium.games.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure d'une arene Rush (AJOUTE en 1.11.0, etape 1 du mini-jeu Rush - demande explicite de
 * l'utilisateur) : equipes, PNJ marchands et cles des points d'arene a definir dans les parametres.
 *
 * Une arene Rush est pour 2 equipes (bleue + rouge) ou 4 equipes (+ jaune + verte) - demande
 * explicite : "il faut pouvoir configurer des arenes pour 2 equipes ou des arenes pour 4 equipes".
 * Le nombre d'equipes se deduit des points definis : bases jaune ET verte completes = 4 equipes,
 * aucune des deux = 2 equipes, entre les deux = arene incomplete (voir missing()).
 *
 * Chaque base : point d'apparition, lit, zone des monnaies (2 coins - choix de l'utilisateur via
 * AskUserQuestion : "une zone par base"), et un point par PNJ ("chacun des PNJ doit apparaitre
 * dans chacune des bases, je definirai leur position grace aux parametres").
 */
public final class RushLayout {

    private RushLayout() {
    }

    public enum Team {
        BLUE("blue", "bleue", "Bleu"),
        RED("red", "rouge", "Rouge"),
        YELLOW("yellow", "jaune", "Jaune"),
        GREEN("green", "verte", "Vert");

        private final String key;
        private final String baseAdjective;
        private final String display;

        Team(String key, String baseAdjective, String display) {
            this.key = key;
            this.baseAdjective = baseAdjective;
            this.display = display;
        }

        public String key() {
            return key;
        }

        /** "Base bleue", "Base rouge"... */
        public String baseName() {
            return "Base " + baseAdjective;
        }

        public String display() {
            return display;
        }

        /** "bleue", "rouge"... (pour "l'equipe bleue"). */
        public String adjective() {
            return baseAdjective;
        }

        /** Bases jaune et verte : uniquement pour les arenes a 4 equipes. */
        public boolean optional() {
            return this == YELLOW || this == GREEN;
        }
    }

    /** PNJ marchands presents dans chaque base (memes echanges pour toutes les equipes). */
    public enum Shop {
        TRADER("trader", "Trader"),
        TRADER2("trader2", "Trader #2"),
        RUSHER("rusher", "Rusher"),
        ARCHER("archer", "Archer"),
        MACON("macon", "Maçon"),
        ARMURIER("armurier", "Armurier"),
        SECRET("secret", "Secret Trader");

        private final String key;
        private final String display;

        Shop(String key, String display) {
            this.key = key;
            this.display = display;
        }

        public String key() {
            return key;
        }

        public String display() {
            return display;
        }
    }

    public static final String WAITING_KEY = "stands";

    public static String spawnKey(Team team) {
        return "spawn-" + team.key();
    }

    public static String bedKey(Team team) {
        return "bed-" + team.key();
    }

    /** corner = 1 ou 2. */
    public static String generatorKey(Team team, int corner) {
        return "gen-" + team.key() + "-" + corner;
    }

    public static String npcKey(Team team, Shop shop) {
        return "npc-" + team.key() + "-" + shop.key();
    }

    /** Tous les points d'une arene Rush, groupes par base dans le menu Parametres. */
    public static List<PointSpec> points() {
        List<PointSpec> points = new ArrayList<>();
        points.add(PointSpec.single(WAITING_KEY, "Salle d'attente", true,
                "Où les joueurs attendent et choisissent leur équipe avant le début de la partie."));
        for (Team team : Team.values()) {
            boolean required = !team.optional();
            String base = team.baseName();
            String optionalNote = team.optional() ? " (arène à 4 équipes uniquement)" : "";
            points.add(PointSpec.single(spawnKey(team), base + " : apparition", required,
                    "Où les joueurs de l'équipe apparaissent au début de la partie. Regardez dans la direction voulue." + optionalNote)
                    .inGroup(base));
            points.add(PointSpec.single(bedKey(team), base + " : lit", required,
                    "Placez-vous à l'endroit du PIED du lit, en regardant vers la tête du lit. Le lit est posé automatiquement au début de chaque partie." + optionalNote)
                    .inGroup(base));
            points.add(PointSpec.single(generatorKey(team, 1), base + " : zone des monnaies (coin 1)", required,
                    "Bronze, Silver et Gold apparaissent au hasard entre les deux coins de cette zone." + optionalNote)
                    .inGroup(base));
            points.add(PointSpec.single(generatorKey(team, 2), base + " : zone des monnaies (coin 2)", required,
                    "Bronze, Silver et Gold apparaissent au hasard entre les deux coins de cette zone." + optionalNote)
                    .inGroup(base));
            for (Shop shop : Shop.values()) {
                points.add(PointSpec.single(npcKey(team, shop), base + " : PNJ " + shop.display(), required,
                        "Position et orientation du PNJ (il regarde dans la même direction que vous)." + optionalNote)
                        .inGroup(base));
            }
        }
        return points;
    }

    /** Cles de tous les points d'une base. */
    public static List<String> keysOf(Team team) {
        List<String> keys = new ArrayList<>();
        keys.add(spawnKey(team));
        keys.add(bedKey(team));
        keys.add(generatorKey(team, 1));
        keys.add(generatorKey(team, 2));
        for (Shop shop : Shop.values()) {
            keys.add(npcKey(team, shop));
        }
        return keys;
    }

    private static int countSet(Arena arena, Team team) {
        int n = 0;
        for (String key : keysOf(team)) {
            if (arena.point(key) != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * Verification propre a Rush, en plus des points obligatoires (bases bleue et rouge) : les bases
     * jaune et verte sont soit TOUTES LES DEUX completes (arene a 4 equipes), soit toutes les deux
     * vides (arene a 2 equipes). Une arene a 3 equipes n'est pas prevue.
     */
    public static List<String> missing(Arena arena) {
        List<String> missing = new ArrayList<>();
        int total = keysOf(Team.YELLOW).size();
        int yellow = countSet(arena, Team.YELLOW);
        int green = countSet(arena, Team.GREEN);
        if (yellow == 0 && green == 0) {
            return missing; // arene a 2 equipes
        }
        if (yellow < total || green < total) {
            missing.add("bases jaune et verte complètes (arène à 4 équipes) ou toutes deux vides (arène à 2 équipes)");
        }
        return missing;
    }

    /** 4 si les bases jaune et verte sont completes, sinon 2. */
    public static int teamCount(Arena arena) {
        int total = keysOf(Team.YELLOW).size();
        return countSet(arena, Team.YELLOW) == total && countSet(arena, Team.GREEN) == total ? 4 : 2;
    }
}
