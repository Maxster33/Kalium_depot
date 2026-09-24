package fr.kalium.bingo.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Une equipe au sein d'une partie de Bingo : 1 a 4 joueurs (confirme par
 * l'utilisateur - "en solo ou en coop il faut pouvoir aller jusque 4
 * equipes, chaque equipe doivent pouvoir contenir 1 a 4 joueurs"). Le solo
 * est simplement une equipe a 1 joueur : meme modele pour les deux cas.
 *
 * La formation des equipes (assignees par l'hote dans la salle d'attente de ce
 * serveur, voir BingoParty/PartyMenu) n'est pas geree ici : cette classe ne fait
 * que representer le resultat final transmis a la partie Bingo.
 */
public class BingoTeam {

    private final int teamNumber;
    private final List<UUID> players;

    public BingoTeam(int teamNumber, List<UUID> players) {
        this.teamNumber = teamNumber;
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
    }

    public int getTeamNumber() {
        return teamNumber;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public boolean contains(UUID playerId) {
        return players.contains(playerId);
    }

    public int size() {
        return players.size();
    }
}
