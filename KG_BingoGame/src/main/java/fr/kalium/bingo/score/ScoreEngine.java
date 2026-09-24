package fr.kalium.bingo.score;

import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.grid.Difficulty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bareme de points du Bingo (0.3.0) - demande explicite de LeKiwi06, 24/09/2026, calcule EN TEMPS REEL :
 *
 * - valeur d'un objectif : Facile 1, Normal 3, Difficile 5, Extreme 10 (voir Difficulty) ;
 * - 1re equipe a valider un objectif : +0 / +1 / +2 / +3 sur cet objectif ;
 * - bingo = ligne, colonne ou diagonale entierement validee par une equipe. 1re equipe a achever CE bingo :
 *   +0,5 au coefficient de chacune de ses cases ; bingo compose uniquement d'objectifs difficiles / extremes :
 *   encore +0,5, pour toute equipe qui l'acheve. Cumulatif et ADDITIF (choix de LeKiwi06 : une case dans deux
 *   bingos "1er" a un coefficient de 2, pas 2,25) ;
 * - victoire : +1 / +2 / +3 / +5 sur chaque objectif valide par l'equipe gagnante (fin de partie seulement) ;
 * - les bonus s'ajoutent AVANT les coefficients : points d'une case = (valeur + bonus) x coefficient ;
 * - un bonus debloque par un joueur profite a toute son equipe.
 *
 * Tout est deduit du journal ordonne des validations (equipe, case, joueur) : le rejouer dans le meme ordre
 * redonne exactement le meme etat - c'est ce journal qui est sauvegarde (voir GamePersistence).
 *
 * Points SOLO (proposition en attente de validation par LeKiwi06) : objectifs valides par le joueur lui-meme
 * (valeur + bonus de 1re equipe) + gain complet de chaque bingo auquel il a participe (au moins une case
 * validee par lui) + bonus de victoire sur ses propres objectifs.
 */
public final class ScoreEngine {

    /** Une validation : case {@code cell} validee pour l'equipe {@code team} par le joueur {@code player}
     *  (null pour une ancienne sauvegarde sans joueur). */
    public record Validation(int team, int cell, UUID player) {
    }

    /** Un bingo tout juste acheve : gain en points pour l'equipe, et joueurs qui y ont valide une case. */
    public record BingoEvent(int line, String name, boolean first, boolean hardOnly, double gain, Set<UUID> participants) {
    }

    /** Resultat d'une validation : points gagnes sur l'objectif lui-meme, et bingos acheves avec elle. */
    public record Result(int cell, boolean first, double itemPoints, List<BingoEvent> bingos) {
    }

    private final BingoGrid grid;
    private final int size;
    private final List<int[]> lines = new ArrayList<>();
    private final List<String> lineNames = new ArrayList<>();
    private final List<Validation> log = new ArrayList<>();

    private final int[] firstTeam;
    private final int[] firstBingoTeam;
    private final Map<Integer, UUID[]> owners = new HashMap<>();
    private final Map<Integer, Set<Integer>> completedLines = new HashMap<>();
    /** Gain de chaque bingo au moment ou il a ete acheve (equipe -> ligne -> gain), pour les points solo. */
    private final Map<Integer, Map<Integer, Double>> bingoGains = new HashMap<>();

    public ScoreEngine(BingoGrid grid) {
        this.grid = grid;
        this.size = grid.getSize();
        for (int r = 0; r < size; r++) {
            int[] line = new int[size];
            for (int c = 0; c < size; c++) {
                line[c] = r * size + c;
            }
            lines.add(line);
            lineNames.add("Ligne " + (r + 1));
        }
        for (int c = 0; c < size; c++) {
            int[] line = new int[size];
            for (int r = 0; r < size; r++) {
                line[r] = r * size + c;
            }
            lines.add(line);
            lineNames.add("Colonne " + (c + 1));
        }
        int[] diag = new int[size];
        int[] anti = new int[size];
        for (int i = 0; i < size; i++) {
            diag[i] = i * size + i;
            anti[i] = i * size + (size - 1 - i);
        }
        lines.add(diag);
        lineNames.add("Diagonale ↘");
        lines.add(anti);
        lineNames.add("Diagonale ↙");

        this.firstTeam = new int[size * size];
        Arrays.fill(firstTeam, -1);
        this.firstBingoTeam = new int[lines.size()];
        Arrays.fill(firstBingoTeam, -1);
    }

    /** Nombre total de bingos possibles sur la grille (12 pour une grille 5x5). */
    public int lineCount() {
        return lines.size();
    }

    public String lineName(int line) {
        return lineNames.get(line);
    }

    public int[] lineCells(int line) {
        return lines.get(line).clone();
    }

    public List<Validation> log() {
        return List.copyOf(log);
    }

    private Difficulty difficulty(int cell) {
        return grid.getCells().get(cell).getObjective().difficulty();
    }

    private final Map<Integer, boolean[]> validated = new HashMap<>();

    private boolean isValidated(int team, int cell) {
        boolean[] v = validated.get(team);
        return v != null && v[cell];
    }

    /**
     * Enregistre une validation et renvoie ce qu'elle rapporte (a appeler une seule fois par case et par
     * equipe, dans l'ordre reel des validations).
     */
    public Result validate(int team, int cell, UUID player) {
        log.add(new Validation(team, cell, player));
        UUID[] o = owners.computeIfAbsent(team, t -> new UUID[size * size]);
        o[cell] = player;
        validated.computeIfAbsent(team, t -> new boolean[size * size])[cell] = true;
        boolean first = firstTeam[cell] == -1;
        if (first) {
            firstTeam[cell] = team;
        }
        double itemPoints = basePlusFirst(team, cell);

        List<BingoEvent> events = new ArrayList<>();
        for (int line = 0; line < lines.size(); line++) {
            int[] cells = lines.get(line);
            if (!contains(cells, cell) || !isComplete(team, cells)) {
                continue;
            }
            completedLines.computeIfAbsent(team, t -> new LinkedHashSet<>()).add(line);
            boolean firstBingo = firstBingoTeam[line] == -1;
            if (firstBingo) {
                firstBingoTeam[line] = team;
            }
            boolean hardOnly = isHardOnly(cells);
            double factor = 0.5 * ((firstBingo ? 1 : 0) + (hardOnly ? 1 : 0));
            double sum = 0;
            Set<UUID> participants = new LinkedHashSet<>();
            for (int c : cells) {
                sum += basePlusFirst(team, c);
                if (o[c] != null) {
                    participants.add(o[c]);
                }
            }
            double gain = factor * sum;
            bingoGains.computeIfAbsent(team, t -> new HashMap<>()).put(line, gain);
            events.add(new BingoEvent(line, lineNames.get(line), firstBingo, hardOnly, gain, participants));
        }
        return new Result(cell, first, itemPoints, events);
    }

    private static boolean contains(int[] cells, int cell) {
        for (int c : cells) {
            if (c == cell) {
                return true;
            }
        }
        return false;
    }

    private boolean isComplete(int team, int[] cells) {
        for (int c : cells) {
            if (!isValidated(team, c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isHardOnly(int[] cells) {
        for (int c : cells) {
            if (!difficulty(c).isHardOrAbove()) {
                return false;
            }
        }
        return true;
    }

    /** Valeur de l'objectif + bonus de 1re equipe si c'est cette equipe qui l'a valide en premier. */
    private double basePlusFirst(int team, int cell) {
        Difficulty d = difficulty(cell);
        return d.points() + (firstTeam[cell] == team ? d.firstBonus() : 0);
    }

    /** Coefficient actuel de cette case pour cette equipe (1 + 0,5 par bonus de bingo, additif). */
    public double coefficient(int team, int cell) {
        double coef = 1.0;
        for (int line : completedLines.getOrDefault(team, Set.of())) {
            int[] cells = lines.get(line);
            if (!contains(cells, cell)) {
                continue;
            }
            if (firstBingoTeam[line] == team) {
                coef += 0.5;
            }
            if (isHardOnly(cells)) {
                coef += 0.5;
            }
        }
        return coef;
    }

    /** Score d'equipe en temps reel (sans bonus de victoire). */
    public double teamScore(int team) {
        return teamScore(team, false);
    }

    /** Score d'equipe ; {@code winner} ajoute le bonus de victoire sur chaque objectif valide. */
    public double teamScore(int team, boolean winner) {
        double total = 0;
        for (int cell = 0; cell < size * size; cell++) {
            if (!isValidated(team, cell)) {
                continue;
            }
            double points = basePlusFirst(team, cell) + (winner ? difficulty(cell).winBonus() : 0);
            total += points * coefficient(team, cell);
        }
        return total;
    }

    /** Points solo d'un joueur (voir en-tete : proposition en attente de validation). */
    public double soloScore(int team, UUID player, boolean winner) {
        UUID[] o = owners.get(team);
        if (o == null || player == null) {
            return 0;
        }
        double total = 0;
        for (int cell = 0; cell < o.length; cell++) {
            if (player.equals(o[cell])) {
                total += basePlusFirst(team, cell) + (winner ? difficulty(cell).winBonus() : 0);
            }
        }
        for (Map.Entry<Integer, Double> bingo : bingoGains.getOrDefault(team, Map.of()).entrySet()) {
            for (int c : lines.get(bingo.getKey())) {
                if (player.equals(o[c])) {
                    total += bingo.getValue();
                    break;
                }
            }
        }
        return total;
    }

    /** Nombre de bingos acheves par cette equipe. */
    public int bingoCount(int team) {
        return completedLines.getOrDefault(team, Set.of()).size();
    }

    /** Equipe qui a valide cette case en premier (-1 si personne). */
    public int firstTeamOf(int cell) {
        return firstTeam[cell];
    }

    /** Joueur qui a valide cette case pour cette equipe (null si inconnu ou non validee). */
    public UUID ownerOf(int team, int cell) {
        UUID[] o = owners.get(team);
        return o == null ? null : o[cell];
    }

    /** Equipe qui a acheve ce bingo en premier (-1 si personne). */
    public int firstTeamOfLine(int line) {
        return firstBingoTeam[line];
    }

    /** true si cette equipe a acheve ce bingo. */
    public boolean hasCompleted(int team, int line) {
        return completedLines.getOrDefault(team, Set.of()).contains(line);
    }

    /** Indices des bingos (lignes, colonnes, diagonales) qui contiennent cette case. */
    public List<Integer> linesOf(int cell) {
        List<Integer> result = new ArrayList<>();
        for (int line = 0; line < lines.size(); line++) {
            if (contains(lines.get(line), cell)) {
                result.add(line);
            }
        }
        return result;
    }

    /** Bonus de 1re equipe de cette case pour cette equipe (0 si ce n'est pas elle). */
    public int firstBonusOf(int team, int cell) {
        return firstTeam[cell] == team ? difficulty(cell).firstBonus() : 0;
    }

    /** Affichage des points : entier si possible, sinon une decimale avec une virgule ("7,5"). */
    public static String format(double points) {
        if (Math.abs(points - Math.rint(points)) < 1e-9) {
            return String.valueOf((long) Math.rint(points));
        }
        return String.format(java.util.Locale.FRANCE, "%.1f", points);
    }
}
