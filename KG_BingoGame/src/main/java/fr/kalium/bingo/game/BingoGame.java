package fr.kalium.bingo.game;

import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.score.ScoreEngine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Une partie de Bingo : ensemble d'instances (une par equipe) partageant la
 * meme seed, un etat commun, un debut et une duree (section 1 du cahier des
 * charges), ainsi qu'une grille d'objectifs (section 2, voir GridGenerator -
 * une seule grille, partagee par toutes les equipes).
 *
 * Porte aussi la progression de chaque equipe (teamProgress) et le calcul du
 * score ; la validation elle-meme est faite par ObjectiveValidationTask et la
 * fin de partie par GameEndService.
 */
public class BingoGame {

    private final String gameId;
    private final long seed;
    private final Duration duration;
    private final List<BingoInstance> instances = new ArrayList<>();
    private BingoGrid grid;

    private GameState state = GameState.PREPARING;
    private Instant startedAt;

    /**
     * Progression PAR EQUIPE (numero d'equipe -&gt; cases validees, index = row * taille + col,
     * voir BingoGrid) - la grille elle-meme est UNIQUE et partagee par toutes les equipes
     * (voir BingoGrid), mais chaque equipe valide ses propres cases independamment. Remplace le
     * champ GridCell.validated (inerte, un seul booleen partage par case, incompatible avec un
     * suivi par equipe) plutot que de le detourner. Alimente par la detection automatique
     * d'objectifs (voir ObjectiveValidationTask) : une fois validee, une case reste acquise pour
     * le reste de la partie meme si l'equipe perd/depense l'objet ensuite (demande explicite de
     * l'utilisateur, choix confirme via AskUserQuestion le 23/09/2026 - pas de reverification).
     */
    private final Map<Integer, boolean[]> teamProgress = new HashMap<>();

    /** Reglages de la partie (0.3.0 : mode, bingos requis, composition - voir BingoSettings). */
    private BingoSettings settings = BingoSettings.defaults();

    public BingoSettings getSettings() {
        return settings;
    }

    public void setSettings(BingoSettings settings) {
        this.settings = settings == null ? BingoSettings.defaults() : settings;
    }

    /** Points de la partie (0.3.0, voir ScoreEngine) - recree avec la grille. */
    private ScoreEngine scoreEngine;

    public BingoGame(String gameId, long seed, Duration duration) {
        this.gameId = gameId;
        this.seed = seed;
        this.duration = duration;
    }

    public String getGameId() {
        return gameId;
    }

    public long getSeed() {
        return seed;
    }

    public Duration getDuration() {
        return duration;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public List<BingoInstance> getInstances() {
        return instances;
    }

    public void addInstance(BingoInstance instance) {
        instances.add(instance);
    }

    public BingoGrid getGrid() {
        return grid;
    }

    /** Attache la grille et (re)initialise la progression de chaque equipe a "aucune case validee". */
    public void setGrid(BingoGrid grid) {
        this.grid = grid;
        this.scoreEngine = new ScoreEngine(grid);
        teamProgress.clear();
        int cellCount = grid.getSize() * grid.getSize();
        for (BingoInstance instance : instances) {
            teamProgress.put(instance.getTeam().getTeamNumber(), new boolean[cellCount]);
        }
    }

    public boolean isValidated(int teamNumber, int cellIndex) {
        boolean[] progress = teamProgress.get(teamNumber);
        return progress != null && cellIndex >= 0 && cellIndex < progress.length && progress[cellIndex];
    }

    /**
     * Valide la case pour cette equipe et calcule ce qu'elle rapporte (0.3.0 : {@code player} = joueur qui
     * possedait l'objet, null s'il est inconnu).
     *
     * @return les points gagnes, ou null si la case etait deja validee (ou hors grille).
     */
    public ScoreEngine.Result markValidated(int teamNumber, int cellIndex, UUID player) {
        boolean[] progress = teamProgress.get(teamNumber);
        if (progress == null || cellIndex < 0 || cellIndex >= progress.length || progress[cellIndex]) {
            return null;
        }
        progress[cellIndex] = true;
        return scoreEngine.validate(teamNumber, cellIndex, player);
    }

    public ScoreEngine getScoreEngine() {
        return scoreEngine;
    }

    /** Nombre de cases validees par cette equipe (sur grid.getSize() * grid.getSize()). */
    public int countValidated(int teamNumber) {
        boolean[] progress = teamProgress.get(teamNumber);
        if (progress == null) {
            return 0;
        }
        int count = 0;
        for (boolean b : progress) {
            if (b) {
                count++;
            }
        }
        return count;
    }

    public Optional<BingoInstance> findInstanceOf(UUID playerId) {
        return instances.stream()
                .filter(i -> i.isAssignedTo(playerId))
                .findFirst();
    }

    public Optional<BingoTeam> findTeamOf(UUID playerId) {
        return findInstanceOf(playerId).map(BingoInstance::getTeam);
    }

    public void start() {
        this.startedAt = Instant.now();
        this.state = GameState.IN_PROGRESS;
    }

    /**
     * Restaure une partie EN COURS depuis la persistance (voir GamePersistence), apres un
     * redemarrage/crash du serveur - demande explicite de l'utilisateur : "la partie doit
     * continuer meme si le serveur est redémarré ou si il crash". Reprend directement a l'etat
     * IN_PROGRESS avec le temps restant EXACT tel qu'il etait a la derniere sauvegarde : le
     * chronometre est mis en PAUSE pendant que le serveur est eteint, quelle que soit la duree de
     * l'arret, plutot que de compter ce temps d'arret comme du temps de jeu ecoule (choix confirme
     * via AskUserQuestion le 23/09/2026). Recalcule startedAt a partir de maintenant plutot que de
     * restaurer l'instant absolu d'origine, pour obtenir exactement ce temps restant des cet appel.
     */
    public void restoreInProgress(Duration remainingAtSave) {
        Duration remaining = (remainingAtSave == null || remainingAtSave.isNegative()) ? Duration.ZERO : remainingAtSave;
        this.startedAt = Instant.now().minus(duration).plus(remaining);
        this.state = GameState.IN_PROGRESS;
    }

    /** true si au moins un joueur est actuellement connecte (et n'a pas abandonne, voir
     *  BingoInstance.hasAnyActivePlayer) a l'une des instances de cette partie - voir
     *  GameEndService (fin de partie si personne n'est actif pendant trop longtemps, ou
     *  immediatement si un abandon volontaire vide totalement la partie). */
    public boolean hasAnyConnectedPlayer() {
        return instances.stream().anyMatch(BingoInstance::hasAnyActivePlayer);
    }

    /**
     * Score d'equipe EN TEMPS REEL (0.3.0, bareme de LeKiwi06 - voir ScoreEngine) : remplace l'ancien score
     * (1 point par objectif + 1 par rangee + 5 pour la grille complete). Sans bonus de victoire, ajoute en
     * fin de partie.
     */
    public double score(int teamNumber) {
        return scoreEngine == null ? 0 : scoreEngine.teamScore(teamNumber);
    }

    /** true si le temps imparti est ecoule (partie en cours uniquement). */
    public boolean isTimeUp() {
        if (startedAt == null || settings.isBlackout()) { // 0.3.0 : pas de chrono en blackout
            return false;
        }
        return Instant.now().isAfter(startedAt.plus(duration));
    }

    /** Temps restant avant la fin de la partie (jamais negatif - voir isTimeUp pour le depassement). */
    public Duration getRemaining() {
        if (startedAt == null) {
            return duration;
        }
        Duration elapsed = Duration.between(startedAt, Instant.now());
        Duration remaining = duration.minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /** Temps de jeu ecoule depuis le debut (0.3.0 : affiche en blackout, nulle proposee toutes les heures). */
    public Duration getElapsed() {
        if (startedAt == null) {
            return Duration.ZERO;
        }
        Duration elapsed = Duration.between(startedAt, Instant.now());
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }

    /** Partie arretee en attendant le vote de la derniere equipe (0.3.0) : plus de validation ni de chrono. */
    private boolean frozen;

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /** Restauration d'une partie BLACKOUT (sans chrono) : reprend le temps de jeu deja ecoule (0.3.0). */
    public void restoreElapsed(Duration elapsed) {
        this.startedAt = Instant.now().minus(elapsed == null || elapsed.isNegative() ? Duration.ZERO : elapsed);
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
