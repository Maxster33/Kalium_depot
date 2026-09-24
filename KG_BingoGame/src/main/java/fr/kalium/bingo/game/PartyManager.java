package fr.kalium.bingo.game;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Parties Bingo EN FORMATION dans la salle d'attente (avant creation des instances de
 * jeu, voir PartyStarter/GameManager). Alimente par AssignmentService des que kal-games
 * confirme l'affectation d'un joueur.
 */
public final class PartyManager {

    private final Map<String, BingoParty> parties = new HashMap<>();

    /**
     * NE PAS remplacer par un simple computeIfAbsent : un joueur qui rejoint la partie APRES le
     * premier appel (ex. l'hote se connecte avant que kal-games ne sache qu'un second joueur a
     * rejoint) doit quand meme etre ajoute au roster de la partie DEJA CREEE cote KG_BingoGame - sinon
     * son UUID n'apparait jamais dans getExpectedRoster() (bug corrige en 0.1.9, repere lors du
     * test "un joueur qui rejoint arrive sur le modele puis se fait expulser").
     */
    /**
     * `onCreated` est invoque UNIQUEMENT si une NOUVELLE BingoParty vient d'etre creee (pas en cas
     * de fusion dans une partie deja existante, voir mergeExpectedRoster) - AJOUTE le 23/09/2026
     * pour declencher la pre-generation en cascade des mondes d'instance des la creation de la
     * salle d'attente (voir InstanceWorldPreparer / AssignmentService), une seule fois par partie.
     * Peut etre null si l'appelant n'a rien a faire dans ce cas.
     */
    public BingoParty getOrCreate(String gameId, long seed, Duration duration, UUID host, int teamCount, int teamSize,
                                   Set<UUID> expectedRoster, Runnable onCreated) {
        BingoParty existing = parties.get(gameId);
        if (existing != null) {
            existing.mergeExpectedRoster(expectedRoster);
            return existing;
        }
        BingoParty created = new BingoParty(gameId, seed, duration, host, teamCount, teamSize, expectedRoster);
        parties.put(gameId, created);
        if (onCreated != null) {
            onCreated.run();
        }
        return created;
    }

    public BingoParty get(String gameId) {
        return parties.get(gameId);
    }

    public BingoParty partyOf(UUID playerId) {
        for (BingoParty party : parties.values()) {
            if (party.getExpectedRoster().contains(playerId)) {
                return party;
            }
        }
        return null;
    }

    public void remove(String gameId) {
        parties.remove(gameId);
    }
}
