package fr.kalium.bingo.game;

import fr.kalium.bingo.score.ScoreEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Votes de match nul (0.3.0) - demande explicite de LeKiwi06, 24/09/2026 :
 *
 * - un joueur propose la nulle (menu Objectifs) -&gt; vote de SON equipe ; si elle accepte, vote de TOUTES les
 *   autres equipes ; chaque equipe decide a la majorite de ses joueurs ; la nulle n'est acceptee que si toutes
 *   les equipes l'acceptent. Egalite de votes dans une equipe = OUI pour cette equipe (precision de LeKiwi06) ;
 * - 3 minutes pour voter (a chaque etape) ; delai GLOBAL de 30 minutes entre deux propositions dans une partie ;
 * - propositions automatiques, votees directement par toutes les equipes : en blackout toutes les heures de jeu,
 *   et quand une equipe abandonne alors qu'il en reste plusieurs ("question de fair-play") ;
 * - s'il ne reste qu'une equipe (toutes les autres ont abandonne), elle seule vote (voir proposeFinal) ;
 * - la proposition affiche entre parentheses les points que chaque equipe garderait en cas d'acceptation
 *   (nulle = chaque equipe garde ses propres points d'equipe).
 *
 * Votes par boutons cliquables dans le tchat ([Oui] / [Non], commande /bingonulle oui|non).
 */
public final class DrawVoteService {

    public static final Duration VOTE_DURATION = Duration.ofMinutes(3);
    public static final Duration COOLDOWN = Duration.ofMinutes(30);

    private enum Phase { PROPOSER_TEAM, ALL_TEAMS, FINAL }

    private final class Vote {
        final BingoGame game;
        Phase phase;
        final int proposerTeam;
        final Consumer<Boolean> onDecision;
        Instant deadline;
        /** equipe -&gt; joueurs appeles a voter (connectes et n'ayant pas abandonne au debut de l'etape). */
        final Map<Integer, Set<UUID>> voters = new LinkedHashMap<>();
        final Map<Integer, Map<UUID, Boolean>> ballots = new HashMap<>();
        final Map<Integer, Boolean> decisions = new HashMap<>();

        Vote(BingoGame game, Phase phase, int proposerTeam, Consumer<Boolean> onDecision) {
            this.game = game;
            this.phase = phase;
            this.proposerTeam = proposerTeam;
            this.onDecision = onDecision;
        }
    }

    private final GameManager gameManager;
    private final Map<String, Vote> votes = new HashMap<>();
    private final Map<String, Instant> lastProposal = new HashMap<>();
    /** Consequence d'une nulle acceptee par toutes les equipes (branche par GameEndService). */
    private Consumer<BingoGame> onDrawAccepted = game -> { };

    public DrawVoteService(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void setOnDrawAccepted(Consumer<BingoGame> onDrawAccepted) {
        this.onDrawAccepted = onDrawAccepted;
    }

    public boolean isVoting(BingoGame game) {
        return votes.containsKey(game.getGameId());
    }

    // ------------------------------------------------------------------ propositions

    /** Proposition d'un joueur (menu Objectifs). */
    public void propose(Player player) {
        BingoGame game = gameManager.findGameOf(player.getUniqueId())
                .filter(g -> g.getState() == GameState.IN_PROGRESS).orElse(null);
        if (game == null) {
            player.sendMessage(Component.text("Aucune partie Bingo en cours pour vous.", NamedTextColor.RED));
            return;
        }
        if (isVoting(game)) {
            player.sendMessage(Component.text("Un vote est déjà en cours.", NamedTextColor.RED));
            return;
        }
        Instant last = lastProposal.get(game.getGameId());
        if (last != null && Instant.now().isBefore(last.plus(COOLDOWN))) {
            long minutes = Math.max(1, Duration.between(Instant.now(), last.plus(COOLDOWN)).toMinutes() + 1);
            player.sendMessage(Component.text("Une nulle a déjà été proposée récemment : nouvelle proposition possible dans "
                    + minutes + " min.", NamedTextColor.RED));
            return;
        }
        if (game.getInstances().size() < 2) {
            player.sendMessage(Component.text("Une nulle n'a de sens qu'à plusieurs équipes.", NamedTextColor.RED));
            return;
        }
        int team = game.findTeamOf(player.getUniqueId()).map(BingoTeam::getTeamNumber).orElse(-1);
        lastProposal.put(game.getGameId(), Instant.now());
        Vote vote = new Vote(game, Phase.PROPOSER_TEAM, team, accepted -> {
            if (accepted) {
                onDrawAccepted.accept(game);
            }
        });
        votes.put(game.getGameId(), vote);
        startStage(vote, List.of(team), Component.text(player.getName() + " propose une nulle à son équipe.", NamedTextColor.GOLD));
        castBallot(player, true); // le proposant vote oui
    }

    /** Proposition automatique a toutes les equipes encore en jeu (blackout toutes les heures, abandon d'une equipe). */
    public void proposeAutomatic(BingoGame game, String reason) {
        if (isVoting(game) || game.getState() != GameState.IN_PROGRESS) {
            return;
        }
        List<Integer> teams = teamsStillPlaying(game);
        if (teams.size() < 2) {
            return;
        }
        lastProposal.put(game.getGameId(), Instant.now());
        Vote vote = new Vote(game, Phase.ALL_TEAMS, -1, accepted -> {
            if (accepted) {
                onDrawAccepted.accept(game);
            }
        });
        votes.put(game.getGameId(), vote);
        startStage(vote, teams, Component.text(reason + " : une nulle est proposée à toutes les équipes.", NamedTextColor.GOLD));
    }

    /** Derniere equipe en jeu (toutes les autres ont abandonne) : elle seule vote. {@code onDecision} recoit true si
     *  elle accepte la nulle (egalite de votes comprise), false si elle refuse ou si personne n'a vote. */
    public void proposeFinal(BingoGame game, int team, Consumer<Boolean> onDecision) {
        Vote previous = votes.remove(game.getGameId());
        if (previous != null) {
            broadcast(previous.game, Component.text("Le vote en cours est annulé.", NamedTextColor.GRAY));
        }
        Vote vote = new Vote(game, Phase.FINAL, team, onDecision);
        votes.put(game.getGameId(), vote);
        startStage(vote, List.of(team), Component.text("Toutes les autres équipes ont abandonné : la partie est arrêtée. "
                + "Votre équipe peut accepter une nulle ; si elle refuse, le score s'applique.", NamedTextColor.GOLD));
    }

    /** Annule le vote d'une partie qui se termine autrement (victoire, temps ecoule...). */
    public void cancel(BingoGame game) {
        votes.remove(game.getGameId());
    }

    public void forget(BingoGame game) {
        votes.remove(game.getGameId());
        lastProposal.remove(game.getGameId());
    }

    private List<Integer> teamsStillPlaying(BingoGame game) {
        List<Integer> teams = new ArrayList<>();
        for (BingoInstance instance : game.getInstances()) {
            if (!instance.isFullyAbandoned()) {
                teams.add(instance.getTeam().getTeamNumber());
            }
        }
        return teams;
    }

    private void startStage(Vote vote, List<Integer> teams, Component intro) {
        vote.deadline = Instant.now().plus(VOTE_DURATION);
        vote.voters.clear();
        vote.ballots.clear();
        vote.decisions.clear();
        for (BingoInstance instance : vote.game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            if (!teams.contains(team)) {
                continue;
            }
            Set<UUID> eligible = new LinkedHashSet<>();
            for (UUID id : instance.getTeam().getPlayers()) {
                Player p = Bukkit.getPlayer(id);
                if (!instance.hasAbandoned(id) && p != null && p.isOnline()) {
                    eligible.add(id);
                }
            }
            vote.voters.put(team, eligible);
            vote.ballots.put(team, new HashMap<>());
        }
        Component preview = Component.text("(En cas de nulle : " + pointsPreview(vote.game) + ")", NamedTextColor.GRAY);
        Component buttons = Component.text("Vote (3 min) : ", NamedTextColor.YELLOW)
                .append(Component.text("[Oui]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/bingonulle oui"))
                        .hoverEvent(HoverEvent.showText(Component.text("Accepter la nulle"))))
                .append(Component.text("  "))
                .append(Component.text("[Non]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/bingonulle non"))
                        .hoverEvent(HoverEvent.showText(Component.text("Refuser la nulle"))));
        for (Set<UUID> ids : vote.voters.values()) {
            for (UUID id : ids) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.sendMessage(intro);
                    p.sendMessage(preview);
                    p.sendMessage(buttons);
                }
            }
        }
        // Equipes sans aucun votant connecte : refus d'office (personne pour accepter).
        for (Map.Entry<Integer, Set<UUID>> entry : vote.voters.entrySet()) {
            if (entry.getValue().isEmpty()) {
                vote.decisions.put(entry.getKey(), false);
            }
        }
        evaluate(vote, false);
    }

    /** Points que chaque equipe garderait si la nulle etait acceptee (ses propres points d'equipe). */
    public String pointsPreview(BingoGame game) {
        List<String> parts = new ArrayList<>();
        for (BingoInstance instance : game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            parts.add("équipe " + TeamStyle.letter(team) + " " + ScoreEngine.format(game.score(team)) + " pts");
        }
        return String.join(", ", parts);
    }

    // ------------------------------------------------------------------ votes

    /** /bingonulle oui|non */
    public void castBallot(Player player, boolean yes) {
        BingoGame game = gameManager.findGameOf(player.getUniqueId()).orElse(null);
        Vote vote = game == null ? null : votes.get(game.getGameId());
        if (vote == null) {
            player.sendMessage(Component.text("Aucun vote de nulle en cours.", NamedTextColor.RED));
            return;
        }
        int team = game.findTeamOf(player.getUniqueId()).map(BingoTeam::getTeamNumber).orElse(-1);
        Set<UUID> eligible = vote.voters.get(team);
        if (eligible == null || !eligible.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Vous ne participez pas à ce vote.", NamedTextColor.RED));
            return;
        }
        if (vote.decisions.containsKey(team)) {
            player.sendMessage(Component.text("Votre équipe a déjà décidé.", NamedTextColor.GRAY));
            return;
        }
        vote.ballots.get(team).put(player.getUniqueId(), yes);
        teamMessage(game, team, Component.text(player.getName() + " vote " + (yes ? "oui" : "non") + " ("
                + count(vote, team, true) + " oui, " + count(vote, team, false) + " non sur " + eligible.size() + ").",
                yes ? NamedTextColor.GREEN : NamedTextColor.RED));
        evaluate(vote, false);
    }

    private int count(Vote vote, int team, boolean value) {
        int n = 0;
        for (boolean b : vote.ballots.get(team).values()) {
            if (b == value) {
                n++;
            }
        }
        return n;
    }

    /** Decide les equipes dont le resultat est deja acquis (ou toutes, a l'expiration), puis conclut l'etape. */
    private void evaluate(Vote vote, boolean expired) {
        for (Map.Entry<Integer, Set<UUID>> entry : vote.voters.entrySet()) {
            int team = entry.getKey();
            if (vote.decisions.containsKey(team)) {
                continue;
            }
            int eligible = entry.getValue().size();
            int yes = count(vote, team, true);
            int no = count(vote, team, false);
            // Egalite = OUI pour l'equipe (precision de LeKiwi06 : "histoire qu'un ou 2 joueurs ne bloquent pas
            // toutes les autres equipes").
            if (yes * 2 >= eligible) {
                vote.decisions.put(team, true);
            } else if (no * 2 > eligible) {
                vote.decisions.put(team, false);
            } else if (expired) {
                vote.decisions.put(team, yes > 0 && yes >= no); // personne n'a vote : refus
            }
        }
        boolean anyRefused = vote.decisions.containsValue(false);
        boolean allDecided = vote.decisions.size() == vote.voters.size();
        if (!anyRefused && !allDecided) {
            return;
        }
        boolean accepted = !anyRefused;
        switch (vote.phase) {
            case PROPOSER_TEAM -> {
                if (!accepted) {
                    votes.remove(vote.game.getGameId());
                    teamMessage(vote.game, vote.proposerTeam, Component.text("Votre équipe a refusé la nulle.", NamedTextColor.RED));
                    return;
                }
                List<Integer> others = new ArrayList<>(teamsStillPlaying(vote.game));
                others.remove(Integer.valueOf(vote.proposerTeam));
                if (others.isEmpty()) {
                    finish(vote, true);
                    return;
                }
                vote.phase = Phase.ALL_TEAMS;
                teamMessage(vote.game, vote.proposerTeam, Component.text("Votre équipe accepte : la nulle est proposée aux autres équipes.",
                        NamedTextColor.GOLD));
                startStage(vote, others, Component.text("", NamedTextColor.GOLD).append(TeamStyle.name(vote.proposerTeam)).append(Component.text(" propose une nulle.", NamedTextColor.GOLD)));
            }
            case ALL_TEAMS, FINAL -> finish(vote, accepted);
        }
    }

    private void finish(Vote vote, boolean accepted) {
        votes.remove(vote.game.getGameId());
        if (vote.phase != Phase.FINAL) {
            broadcast(vote.game, accepted
                    ? Component.text("Nulle acceptée par toutes les équipes.", NamedTextColor.GOLD)
                    : Component.text("Nulle refusée : la partie continue.", NamedTextColor.YELLOW));
        }
        vote.onDecision.accept(accepted);
    }

    /** A appeler chaque seconde : expiration des votes. */
    public void tick() {
        for (Vote vote : new ArrayList<>(votes.values())) {
            if (vote.game.getState() != GameState.IN_PROGRESS) {
                votes.remove(vote.game.getGameId());
                continue;
            }
            if (Instant.now().isAfter(vote.deadline)) {
                evaluate(vote, true);
            }
        }
    }

    private void teamMessage(BingoGame game, int team, Component message) {
        for (BingoInstance instance : game.getInstances()) {
            if (instance.getTeam().getTeamNumber() != team) {
                continue;
            }
            for (UUID id : instance.getTeam().getPlayers()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline() && !instance.hasAbandoned(id)) {
                    p.sendMessage(message);
                }
            }
        }
    }

    private void broadcast(BingoGame game, Component message) {
        for (BingoInstance instance : game.getInstances()) {
            teamMessage(game, instance.getTeam().getTeamNumber(), message);
        }
    }
}
