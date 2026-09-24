package fr.kalium.bingo.game;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Une partie Bingo EN COURS DE FORMATION dans la salle d'attente de CE serveur (avant la
 * creation des instances de jeu par GameManager/PartyStarter). Alimentee par
 * AssignmentService des que kal-games confirme, pour un joueur, a quelle partie il
 * appartient (gameId, seed, duree, liste des joueurs attendus). Les equipes sont ensuite
 * assignees ICI par l'hote (menu PartyMenu ou /bingoteam - pas de choix d'equipe cote
 * kal-games).
 */
public final class BingoParty {

    private final String gameId;
    private final long seed;
    private final Duration duration;
    private final UUID host;
    private final int teamCount;
    private final int teamSize;
    private final Set<UUID> expectedRoster;
    private final Set<UUID> connected = new LinkedHashSet<>();
    private final Map<UUID, Integer> teamOf = new HashMap<>();
    private final Instant createdAt = Instant.now();

    public BingoParty(String gameId, long seed, Duration duration, UUID host, int teamCount, int teamSize,
                       Set<UUID> expectedRoster) {
        this.gameId = gameId;
        this.seed = seed;
        this.duration = duration;
        this.host = host;
        this.teamCount = Math.max(1, teamCount);
        this.teamSize = Math.max(1, teamSize);
        this.expectedRoster = expectedRoster;
    }

    /**
     * Moment de creation de CETTE partie (salle d'attente KG_BingoGame), utilise par
     * PartyCountdownService pour calculer le delai minimum avant demarrage
     * (10s * nombre d'equipes, demande explicite de l'utilisateur - couvre la
     * pre-generation en cascade des mondes, voir InstanceWorldPreparer).
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Choisi par l'hote a la creation, cote kal-games (voir PlayerMenus.openBingoCreate). */
    public UUID getHost() {
        return host;
    }

    public boolean isHost(UUID playerId) {
        return host != null && host.equals(playerId);
    }

    public int getTeamCount() {
        return teamCount;
    }

    public int getTeamSize() {
        return teamSize;
    }

    /**
     * Tente d'assigner ce joueur a l'equipe donnee (1-based), en respectant getTeamCount()/
     * getTeamSize(). Reutilise par /bingoteam (TeamCommand) et le menu joueur (PartyMenu) - une
     * seule implementation, pas de logique dupliquee.
     */
    public TeamJoinResult trySetTeam(UUID playerId, int teamNumber) {
        if (teamNumber < 1 || teamNumber > teamCount) {
            return TeamJoinResult.INVALID_TEAM;
        }
        Integer current = teamOf(playerId);
        boolean alreadyInThatTeam = current != null && current.intValue() == teamNumber;
        if (!alreadyInThatTeam && teamSize(teamNumber) >= teamSize) {
            return TeamJoinResult.TEAM_FULL;
        }
        setTeam(playerId, teamNumber);
        return TeamJoinResult.OK;
    }

    public enum TeamJoinResult {
        OK, INVALID_TEAM, TEAM_FULL
    }

    public String getGameId() {
        return gameId;
    }

    public long getSeed() {
        return seed;
    }

    /** Reglages choisis par l'hote (0.3.0, voir BingoSettings). */
    private BingoSettings settings = BingoSettings.defaults();

    public BingoSettings getSettings() {
        return settings;
    }

    public void setSettings(BingoSettings settings) {
        this.settings = settings == null ? BingoSettings.defaults() : settings;
    }

    public Duration getDuration() {
        return duration;
    }

    public Set<UUID> getExpectedRoster() {
        return expectedRoster;
    }

    /**
     * Fusionne un roster plus a jour recu de kal-games (ex : un joueur a rejoint la partie APRES
     * que cet objet ait ete cree cote KG_BingoGame - voir PartyManager.getOrCreate, correctif 0.1.9).
     * N'ecrase rien, ajoute seulement.
     */
    public void mergeExpectedRoster(Set<UUID> moreExpected) {
        if (moreExpected != null) {
            expectedRoster.addAll(moreExpected);
        }
    }

    public Set<UUID> getConnected() {
        return connected;
    }

    public void markConnected(UUID playerId) {
        connected.add(playerId);
    }

    public void markDisconnected(UUID playerId) {
        connected.remove(playerId);
    }

    /** Assignation directe, SANS validation (utilisee par trySetTeam une fois les bornes verifiees). */
    public void setTeam(UUID playerId, int teamNumber) {
        teamOf.put(playerId, teamNumber);
    }

    public Integer teamOf(UUID playerId) {
        return teamOf.get(playerId);
    }

    public int teamSize(int teamNumber) {
        int count = 0;
        for (int t : teamOf.values()) {
            if (t == teamNumber) {
                count++;
            }
        }
        return count;
    }

    /** Regroupe les choix en liste d'equipes non vides, triees par numero - format attendu par GameManager.createGame. */
    public List<List<UUID>> teamsForGameCreation() {
        Map<Integer, List<UUID>> byTeam = new TreeMap<>();
        for (Map.Entry<UUID, Integer> entry : teamOf.entrySet()) {
            byTeam.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        return new ArrayList<>(byTeam.values());
    }
}
