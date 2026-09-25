package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.data.Kit;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.world.Template;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PvP Kit : jusqu'a 4 equipes, vote du kit dans les gradins, combat, derniere equipe en vie.
 * Reprend les regles du datapack pvpkit (points de victoire + bonus d'infériorite).
 */
public final class PvpInstance extends GameInstance {

    public static final String RANDOM_KIT = "*";
    private static final String[] LETTERS = {"a", "b", "c", "d"};
    private static final String[] TEAM_NAMES = {
            "<red>Équipe A</red>", "<blue>Équipe B</blue>", "<green>Équipe C</green>", "<yellow>Équipe D</yellow>"};

    private final int privateTeams;
    private final int privateTeamSize;
    private final boolean haste;
    /** Parties privees : nombre de manches (1, 3 ou 5), meilleur des N. */
    private final int rounds;
    /** Parties privees : kit tire au sort a chaque manche (le meme pour toutes les equipes), sans vote. */
    private final boolean randomKits;

    private int round;
    private int roundsPlayed;
    private boolean matchOver;
    /** Manches gagnees par equipe (equipes du match). */
    private final Map<Integer, Integer> wins = new TreeMap<>();
    private final Map<UUID, Integer> teamOf = new HashMap<>();
    private final Map<UUID, String> votes = new HashMap<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> greeted = new HashSet<>();
    private final Map<Integer, Integer> teamSizes = new TreeMap<>();
    private Kit chosenKit;
    private int winnerTeam = -1;

    public PvpInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                       boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        int available = teamsAvailable();
        this.privateTeams = Math.max(2, Math.min(available, optionInt("teams", 2)));
        this.privateTeamSize = Math.max(1, Math.min(4, optionInt("teamSize", 1)));
        this.haste = optionBool("haste", false) && minigame.getBool("bedrock-option", true);
        int wanted = publicGame ? 1 : optionInt("rounds", 1);
        this.rounds = wanted >= 5 ? 5 : wanted >= 3 ? 3 : 1;
        this.randomKits = !publicGame && "random".equals(option("kitMode"));
    }

    public int rounds() {
        return rounds;
    }

    public boolean randomKits() {
        return randomKits;
    }

    // ------------------------------------------------------------------ equipes

    /** Nombre d'equipes possibles (points de depart A, B, C, D definis a la suite). */
    public int teamsAvailable() {
        int count = 0;
        for (String letter : LETTERS) {
            if (arena().point("spawn-" + letter) == null) {
                break;
            }
            count++;
        }
        return count;
    }

    public int privateTeams() {
        return privateTeams;
    }

    public int privateTeamSize() {
        return privateTeamSize;
    }

    public boolean haste() {
        return haste;
    }

    public Component teamName(int team) {
        return plugin.lang().parse(TEAM_NAMES[Math.max(0, Math.min(3, team))]);
    }

    public int teamOf(UUID uuid) {
        return teamOf.getOrDefault(uuid, -1);
    }

    public int teamCount(int team) {
        int count = 0;
        for (UUID uuid : members) {
            if (teamOf.getOrDefault(uuid, -1) == team) {
                count++;
            }
        }
        return count;
    }

    /** Parties privees : le joueur change d'equipe (salon uniquement). */
    public Component changeTeam(Player player, int team) {
        if (isPublic() || phase != Phase.WAITING || !members.contains(player.getUniqueId())) {
            return t("team.locked", "<red>Les équipes ne sont modifiables que dans le salon d'une partie privée.");
        }
        if (team < 0 || team >= privateTeams) {
            return t("team.invalid", "<red>Équipe invalide.");
        }
        if (teamOf.getOrDefault(player.getUniqueId(), -1) == team) {
            return null;
        }
        if (teamCount(team) >= privateTeamSize) {
            return t("team.full", "<red>Cette équipe est complète.");
        }
        teamOf.put(player.getUniqueId(), team);
        return null;
    }

    /** Parties privees : l'hote deplace un joueur. */
    public Component moveToTeam(UUID uuid, int team) {
        if (!members.contains(uuid) || isPublic() || phase != Phase.WAITING) {
            return t("team.locked", "<red>Les équipes ne sont modifiables que dans le salon d'une partie privée.");
        }
        if (team < 0 || team >= privateTeams) {
            return t("team.invalid", "<red>Équipe invalide.");
        }
        if (teamOf.getOrDefault(uuid, -1) != team && teamCount(team) >= privateTeamSize) {
            return t("team.full", "<red>Cette équipe est complète.");
        }
        teamOf.put(uuid, team);
        return null;
    }

    private int smallestTeam() {
        int best = -1;
        int bestCount = Integer.MAX_VALUE;
        for (int team = 0; team < privateTeams; team++) {
            int count = teamCount(team);
            if (count < privateTeamSize && count < bestCount) {
                best = team;
                bestCount = count;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ admission

    @Override
    protected Component canAdmit(Player player) {
        if (teamsAvailable() < 2) {
            return t("pvp.no-spawns", "<red>Cette arène n'a pas assez de points de départ.");
        }
        if (!isPublic() && members.size() >= privateTeams * privateTeamSize) {
            return t("join.full", "<red>La partie est complète.");
        }
        return null;
    }

    @Override
    protected void onMemberJoined(Player player) {
        if (!isPublic()) {
            int team = smallestTeam();
            if (team >= 0) {
                teamOf.put(player.getUniqueId(), team);
            }
        }
    }

    @Override
    protected void onArrived(Player player) {
        UUID uuid = player.getUniqueId();
        if (isPublic()) {
            if (greeted.add(uuid)) {
                player.sendMessage(plugin.prefix().append(t("pvp.public-welcome",
                        "<gray>Vous êtes dans les gradins. Vous jouerez à votre tour : le vote du kit s'ouvrira au début de votre match.")));
            }
            return;
        }
        if (phase == Phase.WAITING && greeted.add(uuid)) {
            if (randomKits) {
                player.sendMessage(plugin.prefix().append(t("pvp.private-welcome-random",
                        "<gray>Partie privée <white><code></white> <dark_gray>(<rounds>)</dark_gray>. Kit aléatoire à chaque manche, le même pour toutes les équipes. L'hôte lance la partie.",
                        "code", code, "rounds", roundsText())));
                return;
            }
            player.sendMessage(plugin.prefix().append(t("pvp.private-welcome",
                    "<gray>Partie privée <white><code></white> <dark_gray>(<rounds>)</dark_gray>. Votez pour le kit ; l'hôte lance la partie.",
                    "code", code, "rounds", roundsText())));
            plugin.later(10L, () -> {
                if (player.isOnline() && members.contains(uuid) && phase == Phase.WAITING) {
                    plugin.menus().openVote(player, this);
                }
            });
        }
    }

    // ------------------------------------------------------------------ demarrage

    @Override
    protected int minParticipants() {
        int teamSize = Math.max(1, minigame().getInt("public-team-size", 1));
        return Math.max(2, minigame().getInt("public-min-teams", 2)) * teamSize;
    }

    @Override
    protected int maxParticipants() {
        int teamSize = Math.max(1, minigame().getInt("public-team-size", 1));
        int teams = Math.min(Math.min(4, minigame().getInt("public-max-teams", 4)), teamsAvailable());
        return Math.max(2, teams) * teamSize;
    }

    @Override
    protected int gatherSeconds() {
        return minigame().getInt("public-gather-seconds", 30);
    }

    @Override
    protected List<UUID> pickParticipants(List<UUID> queue) {
        int teamSize = Math.max(1, minigame().getInt("public-team-size", 1));
        int maxTeams = Math.min(Math.min(4, minigame().getInt("public-max-teams", 4)), teamsAvailable());
        int teams = Math.min(maxTeams, queue.size() / teamSize);
        if (teams < Math.max(2, minigame().getInt("public-min-teams", 2))) {
            return List.of();
        }
        return new ArrayList<>(queue.subList(0, teams * teamSize));
    }

    @Override
    protected Component validatePrivateStart() {
        if (voteKits().isEmpty()) {
            return t("pvp.no-kits", "<red>Aucun kit n'est configuré pour ce mini-jeu.");
        }
        Set<Integer> used = new HashSet<>();
        for (UUID uuid : members) {
            int team = teamOf.getOrDefault(uuid, -1);
            if (team >= 0) {
                used.add(team);
            }
        }
        if (used.size() < 2) {
            return t("pvp.need-teams", "<red>Il faut au moins deux équipes non vides.");
        }
        return null;
    }

    @Override
    protected void beginMatch() {
        alive.clear();
        teamSizes.clear();
        wins.clear();
        winnerTeam = -1;
        chosenKit = null;
        round = 0;
        roundsPlayed = 0;
        matchOver = false;

        if (isPublic()) {
            teamOf.clear();
            int teamSize = Math.max(1, minigame().getInt("public-team-size", 1));
            int index = 0;
            for (UUID uuid : participants) {
                teamOf.put(uuid, index / teamSize);
                index++;
            }
        }
        for (UUID uuid : participants) {
            int team = teamOf.getOrDefault(uuid, 0);
            teamSizes.merge(team, 1, Integer::sum);
            wins.putIfAbsent(team, 0);
        }
        votes.keySet().removeIf(uuid -> !participants.contains(uuid));

        if (voteKits().isEmpty()) {
            broadcast(t("pvp.no-kits", "<red>Aucun kit n'est configuré pour ce mini-jeu."));
            endMatch();
            return;
        }

        StringBuilder teams = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : teamSizes.entrySet()) {
            teams.append(TEAM_NAMES[entry.getKey()]).append(" (").append(entry.getValue()).append(")  ");
        }
        broadcast(t("pvp.match-start", "<gold>Match : <teams>", "teams", plugin.lang().parse(teams.toString().trim())));
        if (rounds > 1) {
            broadcast(t("pvp.rounds-info", "<gray>Manches : <white><rounds></white> - la première équipe à <white><need></white> victoire(s) gagne.",
                    "rounds", rounds, "need", rounds / 2 + 1));
        }
        beginRound();
    }

    /** Manches : debut d'une manche (vote ou tirage du kit, puis compte a rebours). */
    private void beginRound() {
        round++;
        alive.clear();
        winnerTeam = -1;
        chosenKit = null;
        teamSizes.clear();
        for (UUID uuid : participants) {
            teamSizes.merge(teamOf.getOrDefault(uuid, 0), 1, Integer::sum);
            alive.add(uuid);
        }
        if (teamSizes.size() < 2) {
            broadcast(t("pvp.aborted", "<yellow>Match annulé : il ne reste pas assez d'équipes."));
            endMatch();
            return;
        }
        if (rounds > 1) {
            broadcastTo(participants, t("pvp.round-start", "<gold><bold>Manche <n>/<total></bold></gold> <gray>- <scoreline>",
                    "n", round, "total", rounds, "scoreline", plugin.lang().parse(scoreLine())));
        }
        List<Kit> kits = voteKits();
        if (kits.isEmpty()) {
            broadcast(t("pvp.no-kits", "<red>Aucun kit n'est configuré pour ce mini-jeu."));
            endMatch();
            return;
        }
        if (randomKits || kits.size() == 1) {
            chosenKit = kits.get(randomKits ? ThreadLocalRandom.current().nextInt(kits.size()) : 0);
            if (randomKits) {
                broadcastTo(participants, t("pvp.kit-random", "<gray>Kit tiré au sort : <white><kit></white>",
                        "kit", plugin.lang().parse(chosenKit.display())));
            }
            startCountdown();
            return;
        }
        if (round > 1) {
            votes.clear();
        }
        phase = Phase.VOTE;
        secondsLeft = Math.max(5, minigame().getInt("vote-seconds", 60));
        title(participants, t("pvp.vote-title", "<gold><bold>Vote du kit"), t("pvp.vote-sub", "<gray>Choisissez le kit du match"), 5, 40, 10);
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (player.getWorld() != world || !contains(player.getLocation())) {
                arriveInStands(player);
            }
            plugin.menus().openVote(player, this);
        }
    }

    /** Manche suivante : arene restauree, tout le monde dans les gradins. */
    private void nextRound() {
        restoreBlocks();
        clearEntities();
        votes.clear();
        for (UUID uuid : new ArrayList<>(participants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                arriveInStands(player);
            }
        }
        beginRound();
    }

    private String scoreLine() {
        StringBuilder line = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : wins.entrySet()) {
            if (line.length() > 0) {
                line.append(" <dark_gray>-</dark_gray> ");
            }
            line.append(TEAM_NAMES[Math.max(0, Math.min(3, entry.getKey()))]).append(" <white>").append(entry.getValue()).append("</white>");
        }
        return line.toString();
    }

    private String roundsText() {
        return rounds == 1 ? "1 manche" : rounds + " manches";
    }

    private boolean decided() {
        int best = 0;
        for (int value : wins.values()) {
            best = Math.max(best, value);
        }
        return roundsPlayed >= rounds || best > rounds / 2;
    }

    // ------------------------------------------------------------------ vote

    public List<Kit> voteKits() {
        List<Kit> kits = new ArrayList<>();
        for (String id : minigame().kits()) {
            Kit kit = plugin.kits().get(id);
            if (kit != null && !kit.broken()) {
                kits.add(kit);
            }
        }
        return kits;
    }

    public boolean voteOpen(UUID uuid) {
        if (randomKits) {
            return false;
        }
        if (phase == Phase.VOTE) {
            return participants.contains(uuid);
        }
        return !isPublic() && phase == Phase.WAITING && members.contains(uuid);
    }

    public Component castVote(Player player, String kitId) {
        if (!voteOpen(player.getUniqueId())) {
            return t("pvp.vote-closed", "<red>Le vote n'est pas ouvert pour vous.");
        }
        if (!RANDOM_KIT.equals(kitId) && (plugin.kits().get(kitId) == null)) {
            return t("pvp.vote-unknown", "<red>Kit inconnu.");
        }
        votes.put(player.getUniqueId(), kitId);
        return null;
    }

    public String voteOf(UUID uuid) {
        return votes.get(uuid);
    }

    public int votesFor(String kitId) {
        int count = 0;
        for (Map.Entry<UUID, String> entry : votes.entrySet()) {
            if (entry.getValue().equals(kitId) && (phase != Phase.VOTE || participants.contains(entry.getKey()))) {
                count++;
            }
        }
        return count;
    }

    public int secondsLeft() {
        return secondsLeft;
    }

    private boolean allVoted() {
        for (UUID uuid : participants) {
            if (Bukkit.getPlayer(uuid) != null && !votes.containsKey(uuid)) {
                return false;
            }
        }
        return true;
    }

    private void endVote() {
        List<Kit> kits = voteKits();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (UUID uuid : participants) {
            String vote = votes.get(uuid);
            if (vote != null) {
                counts.merge(vote, 1, Integer::sum);
            }
        }
        String winner = null;
        int best = 0;
        List<String> tied = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                tied.clear();
                tied.add(entry.getKey());
            } else if (entry.getValue() == best) {
                tied.add(entry.getKey());
            }
        }
        if (!tied.isEmpty()) {
            winner = tied.get(ThreadLocalRandom.current().nextInt(tied.size()));
        }
        Kit kit = winner == null || RANDOM_KIT.equals(winner) ? null : plugin.kits().get(winner);
        if (kit != null && kit.broken()) {
            kit = null;
        }
        boolean random = kit == null;
        if (kit == null) {
            kit = kits.get(ThreadLocalRandom.current().nextInt(kits.size()));
        }
        chosenKit = kit;
        broadcastTo(participants, t(random ? "pvp.kit-random" : "pvp.kit-chosen",
                random ? "<gray>Kit tiré au sort : <white><kit></white>" : "<gray>Kit choisi : <white><kit></white> <dark_gray>(<votes> vote(s))",
                "kit", plugin.lang().parse(kit.display()), "votes", best));
        startCountdown();
    }

    // ------------------------------------------------------------------ combat

    private void startCountdown() {
        phase = Phase.COUNTDOWN;
        secondsLeft = Math.max(0, minigame().getInt("countdown-seconds", 3));
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            plugin.hub().resetPlayer(player, GameMode.SURVIVAL);
            plugin.kits().apply(player, chosenKit);
            if (haste) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, 50, false, false, true));
            }
            player.teleport(spawnOf(teamOf.getOrDefault(uuid, 0)));
        }
        if (secondsLeft == 0) {
            fight();
        } else {
            showCountdown();
        }
    }

    private void showCountdown() {
        title(participants, Component.text(secondsLeft, net.kyori.adventure.text.format.NamedTextColor.GOLD),
                t("pvp.countdown-sub", "<gray>Préparez-vous"), 0, 22, 0);
    }

    private Location spawnOf(int team) {
        Pos pos = arena().point("spawn-" + LETTERS[Math.max(0, Math.min(3, team))]);
        return pos == null ? stands() : loc(pos);
    }

    private void fight() {
        Set<Integer> teamsReady = new HashSet<>();
        for (UUID uuid : alive) {
            teamsReady.add(teamOf.getOrDefault(uuid, -1));
        }
        if (teamsReady.size() < 2) {
            // Un joueur est parti juste avant le combat : il ne reste pas assez d'equipes.
            broadcast(t("pvp.aborted", "<yellow>Match annulé : il ne reste pas assez d'équipes."));
            endMatch();
            return;
        }
        phase = Phase.RUNNING;
        title(participants, t("pvp.fight-title", "<red><bold>Combat !"), Component.empty(), 0, 25, 10);
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
            }
        }
    }

    @Override
    protected void matchSecond() {
        switch (phase) {
            case VOTE -> {
                secondsLeft--;
                if (allVoted() || secondsLeft <= 0) {
                    endVote();
                } else if (secondsLeft == 30 || secondsLeft == 10 || secondsLeft <= 3) {
                    broadcastTo(participants, t("pvp.vote-remaining", "<gray>Fin du vote dans <white><s></white> s.", "s", secondsLeft));
                }
            }
            case COUNTDOWN -> {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    fight();
                } else {
                    showCountdown();
                }
            }
            case ENDING -> {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    if (matchOver) {
                        endMatch();
                    } else {
                        nextRound();
                    }
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void handleDeath(Player victim, Player killer) {
        if (phase != Phase.RUNNING || !alive.contains(victim.getUniqueId())) {
            return;
        }
        eliminate(victim, killer);
    }

    private void eliminate(Player victim, Player killer) {
        alive.remove(victim.getUniqueId());
        if (killer != null && !killer.equals(victim)) {
            broadcast(t("pvp.kill", "<gray><victim> <red>a été éliminé par <white><killer></white>.",
                    "victim", victim.getName(), "killer", killer.getName()));
        } else {
            broadcast(t("pvp.death", "<gray><victim> <red>est éliminé.", "victim", victim.getName()));
        }
        checkWin();
    }

    @Override
    public void handleVoid(Player player) {
        UUID uuid = player.getUniqueId();
        if (isMatchSpectator(uuid)) {
            // Deja en mode spectateur (vol libre) : on ne le recadre plus, il peut voler ou il veut.
            return;
        }
        if (phase == Phase.RUNNING && alive.contains(uuid)) {
            eliminate(player, null);
        }
        // Le match continue pour les autres : mode spectateur (vol libre) plutot que gradins libres.
        if (matchInProgress()) {
            becomeMatchSpectator(player);
        } else {
            arriveInStands(player);
        }
    }

    /** Apres la reapparition d'un joueur elimine : mode spectateur si le match continue, sinon gradins d'attente. */
    public void onRespawned(Player player) {
        if (matchInProgress()) {
            becomeMatchSpectator(player);
        } else {
            arriveInStands(player);
        }
    }

    private void checkWin() {
        if (phase != Phase.RUNNING) {
            return;
        }
        Set<Integer> teamsAlive = new HashSet<>();
        for (UUID uuid : alive) {
            teamsAlive.add(teamOf.getOrDefault(uuid, -1));
        }
        if (teamsAlive.size() > 1) {
            return;
        }
        winnerTeam = teamsAlive.isEmpty() ? -1 : teamsAlive.iterator().next();
        finish();
    }

    private void finish() {
        phase = Phase.ENDING;
        secondsLeft = Math.max(1, minigame().getInt("end-delay-seconds", 5));
        roundsPlayed++;
        if (winnerTeam < 0) {
            broadcast(t("pvp.draw", "<yellow>Manche nulle : plus personne en vie. Aucun point attribué."));
            title(participants, t("pvp.draw-title", "<yellow>Manche nulle"), Component.empty(), 5, 50, 10);
        } else {
            wins.merge(winnerTeam, 1, Integer::sum);
            int winnerSize = teamSizes.getOrDefault(winnerTeam, 1);
            int maxOpponent = 0;
            for (Map.Entry<Integer, Integer> entry : teamSizes.entrySet()) {
                if (entry.getKey() != winnerTeam) {
                    maxOpponent = Math.max(maxOpponent, entry.getValue());
                }
            }
            int points = minigame().getInt("points-win", 1)
                    + minigame().getInt("points-bonus", 2) * Math.max(0, maxOpponent - winnerSize);

            List<String> names = new ArrayList<>();
            for (UUID uuid : participants) {
                if (teamOf.getOrDefault(uuid, -1) != winnerTeam || !members.contains(uuid)) {
                    continue;
                }
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                names.add(player.getName());
                plugin.scores().award(this, player, points);
            }
            Collections.sort(names);
            broadcast(t(rounds > 1 ? "pvp.round-win" : "pvp.win",
                    rounds > 1 ? "<green>Manche remportée par <aqua><names></aqua> ! <gold>+<points> point(s)</gold> chacun."
                            : "<green>Victoire de <aqua><names></aqua> ! <gold>+<points> point(s)</gold> chacun.",
                    "names", String.join(", ", names), "points", points));
            title(participants, t(rounds > 1 ? "pvp.round-win-title" : "pvp.win-title",
                            rounds > 1 ? "<green><bold>Manche gagnée !" : "<green><bold>Victoire !"),
                    t("pvp.win-sub", "<white><team>", "team", teamName(winnerTeam)), 5, 60, 15);
        }
        matchOver = decided();
        if (rounds > 1) {
            broadcast(t("pvp.score", "<gray>Score : <scoreline>", "scoreline", plugin.lang().parse(scoreLine())));
            if (matchOver) {
                announceMatchResult();
            }
        }
    }

    private void announceMatchResult() {
        int best = 0;
        for (int value : wins.values()) {
            best = Math.max(best, value);
        }
        List<Integer> leaders = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : wins.entrySet()) {
            if (entry.getValue() == best) {
                leaders.add(entry.getKey());
            }
        }
        if (best == 0 || leaders.size() != 1) {
            broadcast(t("pvp.match-tie", "<yellow><bold>Match terminé : égalité."));
            return;
        }
        int team = leaders.get(0);
        List<String> names = new ArrayList<>();
        for (UUID uuid : participants) {
            if (teamOf.getOrDefault(uuid, -1) == team) {
                names.add(nameOf(uuid));
            }
        }
        Collections.sort(names);
        broadcast(t("pvp.match-win", "<gold><bold>Match remporté par <team></bold></gold> <gray>(<names>)",
                "team", teamName(team), "names", String.join(", ", names)));
        title(participants, t("pvp.match-win-title", "<gold><bold>Match gagné !"),
                t("pvp.win-sub", "<white><team>", "team", teamName(team)), 5, 70, 20);
    }

    private String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "?" : name;
    }

    @Override
    protected void onMemberLeft(UUID uuid, boolean wasParticipant, boolean disconnected) {
        votes.remove(uuid);
        greeted.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (!wasParticipant || !matchInProgress()) {
            teamOf.remove(uuid);
            return;
        }
        boolean wasAlive = alive.remove(uuid);
        if (phase == Phase.RUNNING && wasAlive) {
            String name = player != null ? player.getName() : "Un joueur";
            broadcast(t("pvp.left", "<gray><name> <red>a quitté la partie en plein combat.", "name", name));
            if (player != null) {
                plugin.scores().combatLeave(player.getName(), minigame());
            }
            checkWin();
        } else if (phase == Phase.VOTE || phase == Phase.COUNTDOWN) {
            Set<Integer> teamsLeft = new HashSet<>();
            for (UUID other : alive) {
                teamsLeft.add(teamOf.getOrDefault(other, -1));
            }
            if (teamsLeft.size() < 2) {
                broadcast(t("pvp.aborted", "<yellow>Match annulé : il ne reste pas assez d'équipes."));
                // Le joueur a deja ete retire des membres : endMatch renvoie les autres dans les gradins.
                plugin.later(1L, this::endMatchIfActive);
            }
        }
        teamOf.remove(uuid);
    }

    private void endMatchIfActive() {
        if (phase == Phase.VOTE || phase == Phase.COUNTDOWN) {
            endMatch();
        }
    }

    @Override
    protected void onMatchReset() {
        votes.clear();
        alive.clear();
        teamSizes.clear();
        chosenKit = null;
        winnerTeam = -1;
        wins.clear();
        round = 0;
        roundsPlayed = 0;
        matchOver = false;
        if (isPublic()) {
            teamOf.clear();
        }
    }

    // ------------------------------------------------------------------ regles

    @Override
    public boolean canBuild(Player player) {
        return phase == Phase.RUNNING && alive.contains(player.getUniqueId());
    }

    @Override
    public boolean canInteract(Player player) {
        return canBuild(player);
    }

    @Override
    public boolean canBreak(Player player, Block block) {
        if (!canBuild(player) || !inBounds(block.getX(), block.getY(), block.getZ())) {
            return false;
        }
        return isPlaced(block) || minigame().getBool("break-map", false);
    }

    @Override
    public boolean takesDamage(Player player) {
        return phase == Phase.RUNNING && alive.contains(player.getUniqueId());
    }

    @Override
    public boolean canFight(Player attacker, Player victim) {
        if (phase != Phase.RUNNING) {
            return false;
        }
        UUID a = attacker.getUniqueId();
        UUID v = victim.getUniqueId();
        return alive.contains(a) && alive.contains(v) && teamOf.getOrDefault(a, -1) != teamOf.getOrDefault(v, -2);
    }

    @Override
    public boolean frozen(Player player) {
        return phase == Phase.COUNTDOWN && participants.contains(player.getUniqueId());
    }

    @Override
    public boolean inventoryLocked(Player player) {
        boolean fighting = alive.contains(player.getUniqueId())
                && (phase == Phase.COUNTDOWN || phase == Phase.RUNNING || phase == Phase.ENDING);
        return !fighting;
    }

    public boolean isAlive(UUID uuid) {
        return alive.contains(uuid);
    }

    public Kit chosenKit() {
        return chosenKit;
    }

    // ------------------------------------------------------------------ affichage

    @Override
    protected Component statusFor(Player player) {
        UUID uuid = player.getUniqueId();
        if (phase == Phase.VOTE && participants.contains(uuid)) {
            return t("pvp.status-vote", "<gold>Vote du kit : <white><s></white> s", "s", secondsLeft);
        }
        if (isPublic()) {
            return queueStatus(player);
        }
        if (phase == Phase.WAITING) {
            return t("pvp.status-lobby", "<gold>Partie privée <white><code></white> <dark_gray>(<rounds>) <dark_gray>- <gray>en attente de l'hôte",
                    "code", code, "rounds", roundsText());
        }
        if (rounds > 1 && participants.contains(uuid) && round > 0) {
            return t("pvp.status-round", "<gold>Manche <white><n>/<total></white> <dark_gray>| <gray><scoreline>",
                    "n", round, "total", rounds, "scoreline", plugin.lang().parse(scoreLine()));
        }
        return null;
    }

    public List<Integer> activeTeams() {
        return new ArrayList<>(teamSizes.keySet());
    }
}
