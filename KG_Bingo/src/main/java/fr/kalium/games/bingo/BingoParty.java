package fr.kalium.games.bingo;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Une partie Bingo cote kal-games : juste assez d'informations pour transferer les
 * joueurs vers le serveur Bingo et repondre a "quelle partie pour ce joueur ?" (voir
 * BingoNetworkListener). La partie elle-meme (equipes, grille, validation...) est geree
 * entierement par KalBingo une fois les joueurs transferes - kal-games ne fait que
 * creer/rejoindre la partie et transferer (demande explicite de l'utilisateur : pas de
 * salle d'attente sur kal-games).
 */
public final class BingoParty {

    private final String gameId;
    private final String code;
    private final UUID host;
    private final long seed;
    private final Duration duration;
    private final int teamCount;
    private final int teamSize;
    private final Instant createdAt = Instant.now();
    private final Set<UUID> roster = new LinkedHashSet<>();

    public BingoParty(String gameId, String code, UUID host, long seed, Duration duration, int teamCount, int teamSize) {
        this.gameId = gameId;
        this.code = code;
        this.host = host;
        this.seed = seed;
        this.duration = duration;
        this.teamCount = teamCount;
        this.teamSize = teamSize;
        roster.add(host);
    }

    public String gameId() {
        return gameId;
    }

    public String code() {
        return code;
    }

    public UUID host() {
        return host;
    }

    public long seed() {
        return seed;
    }

    public Duration duration() {
        return duration;
    }

    /** Reglages de jeu choisis a la creation (1.1.0 : mode, bingos requis, composition de la grille), transmis
     *  tels quels a KG_BingoGame ("mode=BINGOS;bingos=3;easy=10;medium=10;hard=5;extreme=0"). Vide = reglages par
     *  defaut de KG_BingoGame. */
    private String rules = "";

    public String rules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules == null ? "" : rules;
    }

    /** Nombre d'equipes et taille max par equipe CHOISIS PAR L'HOTE a la creation (voir PlayerMenus.openBingoMenu). */
    public int teamCount() {
        return teamCount;
    }

    public int teamSize() {
        return teamSize;
    }

    /** Capacité RÉELLE de cette partie (teamCount x teamSize choisis par l'hôte à la création) -
     *  AJOUTÉ le 24/09/2026, correctif : la capacité affichée/appliquée utilisait auparavant le
     *  plafond GLOBAL bingo.max-party-size (16 par défaut) au lieu de la config de CETTE partie
     *  (ex. "2/16" affiché pour une partie 2 équipes x 1, qui ne peut en réalité accueillir que 2
     *  joueurs) - voir BingoPartyManager.join() et PlayerMenus.bingoPartyButton. */
    public int maxPlayers() {
        return teamCount * teamSize;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Set<UUID> roster() {
        return roster;
    }
}
