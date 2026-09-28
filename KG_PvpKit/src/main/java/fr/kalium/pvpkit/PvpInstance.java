package fr.kalium.pvpkit;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.Pos;
import fr.kalium.games.world.Template;
import fr.kalium.scoreboards.data.StatsService;
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
 *
 * 1.0.0 : repris de KalGames 1.21.0 (PvpInstance) ; memes regles de partie (vote, equipes, manches, kit aleatoire,
 * Haste, restauration de l'arene), memes textes (lang.yml de KalGames, cles « pvp.* »). Nouveautes (demande de LeKiwi06,
 * 28/09/2026) :
 * <ul>
 *   <li>bareme par manche : points par elimination, victoire, 2e et 3e place (a 3 ou 4 equipes), bonus d'inferiorite
 *       numerique, puis multiplicateurs ADDITIFS (regle commune, comme la course de bateau) : serie de manches gagnees
 *       d'affilee et declassement du kit ; points decimaux ;</li>
 *   <li>declassement du kit (0 a 4) choisi par chaque joueur avant le combat : -1 niveau a chaque enchantement et -20 %
 *       des consommables par niveau, +50 % de points par niveau (voir Downgrade) ;</li>
 *   <li>elimination attribuee au dernier joueur qui a frappe la victime (10 s), meme si elle tombe dans le vide.</li>
 * </ul>
 * Parties privees a plusieurs manches : chaque manche rapporte ses points, sans bonus de fin de match (choix de
 * LeKiwi06).
 */
public final class PvpInstance extends GameInstance {

    public static final String RANDOM_KIT = "*";
    private static final String[] LETTERS = {"a", "b", "c", "d"};
    private static final String[] TEAM_NAMES = {
            "<red>Équipe A</red>", "<blue>Équipe B</blue>", "<green>Équipe C</green>", "<yellow>Équipe D</yellow>"};

    private final KGPvpKit pvp;
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
    /** Niveau de declassement choisi par chaque joueur (0 = kit complet). */
    private final Map<UUID, Integer> downgrade = new HashMap<>();
    /** Niveau applique a chaque participant pour la manche en cours (fige au compte a rebours). */
    private final Map<UUID, Integer> roundDowngrade = new HashMap<>();
    /** Eliminations de la manche en cours, par joueur. */
    private final Map<UUID, Integer> kills = new HashMap<>();
    /** Equipes eliminees pendant la manche, dans l'ordre (la derniere eliminee finit 2e). */
    private final List<Integer> eliminated = new ArrayList<>();
    private long fightStart;

    public PvpInstance(KalGames plugin, String id, Minigame minigame, Arena arena, Template template,
                       boolean publicGame, Map<String, Object> options, int slot) {
        super(plugin, id, minigame, arena, template, publicGame, options, slot);
        this.pvp = KGPvpKit.get();
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
        return teamsAvailable(arena());
    }

    static int teamsAvailable(Arena arena) {
        int count = 0;
        for (String letter : LETTERS) {
            if (arena.point("spawn-" + letter) == null) {
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

    // ------------------------------------------------------------------ declassement

    public int maxDowngrade() {
        return Math.max(0, Math.min(4, minigame().getInt("downgrade-max-level", 4)));
    }

    public int downgradeOf(UUID uuid) {
        return Math.min(maxDowngrade(), downgrade.getOrDefault(uuid, 0));
    }

    /** Le joueur peut-il encore changer de declassement (pas pendant son propre combat) ? */
    public boolean downgradeOpen(UUID uuid) {
        if (!members.contains(uuid) || maxDowngrade() == 0) {
            return false;
        }
        boolean fighting = participants.contains(uuid) && (phase == Phase.COUNTDOWN || phase == Phase.RUNNING);
        return !fighting;
    }

    public Component setDowngrade(Player player, int level) {
        if (!downgradeOpen(player.getUniqueId())) {
            return t("pvp.downgrade-locked", "<red>Le déclassement ne se change pas pendant votre combat.");
        }
        downgrade.put(player.getUniqueId(), Math.max(0, Math.min(maxDowngrade(), level)));
        return null;
    }

    /** Multiplicateur apporte par un niveau de declassement (ex. niveau 2 : +1,0). */
    public double downgradeBonus(int level) {
        return level * minigame().getInt("downgrade-bonus-pct", 50) / 100.0;
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
                    pvp.menus().openVote(player, this);
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
        kills.clear();
        eliminated.clear();
        roundDowngrade.clear();
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
            pvp.menus().openVote(player, this);
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
            Kit kit = pvp.kits().get(id);
            if (kit != null) {
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
        if (!RANDOM_KIT.equals(kitId) && (pvp.kits().get(kitId) == null)) {
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
        Kit kit = winner == null || RANDOM_KIT.equals(winner) ? null : pvp.kits().get(winner);
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
        int percent = Math.max(0, Math.min(25, minigame().getInt("downgrade-consumables-pct", 20)));
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            int level = downgradeOf(uuid);
            roundDowngrade.put(uuid, level);
            plugin.hub().resetPlayer(player, GameMode.SURVIVAL);
            pvp.kits().apply(player, Downgrade.apply(chosenKit, level, percent));
            if (level > 0) {
                player.sendMessage(plugin.prefix().append(t("pvp.downgrade-applied",
                        "<gray>Kit déclassé : niveau <white><level></white> <dark_gray>(points x<mult>)",
                        "level", level, "mult", fmt(1 + downgradeBonus(level)))));
            }
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
        fightStart = System.currentTimeMillis();
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
        eliminate(victim, killer != null ? killer : pvp.lastAttacker(victim));
    }

    private void eliminate(Player victim, Player killer) {
        alive.remove(victim.getUniqueId());
        boolean credited = killer != null && !killer.equals(victim) && participants.contains(killer.getUniqueId())
                && teamOf.getOrDefault(killer.getUniqueId(), -1) != teamOf.getOrDefault(victim.getUniqueId(), -2);
        if (credited) {
            kills.merge(killer.getUniqueId(), 1, Integer::sum);
            broadcast(t("pvp.kill", "<gray><victim> <red>a été éliminé par <white><killer></white>.",
                    "victim", victim.getName(), "killer", killer.getName()));
        } else {
            broadcast(t("pvp.death", "<gray><victim> <red>est éliminé.", "victim", victim.getName()));
        }
        noteEliminatedTeam(teamOf.getOrDefault(victim.getUniqueId(), -1));
        checkWin();
    }

    /** L'equipe n'a plus aucun joueur en vie : on note son rang d'elimination (places du bareme). */
    private void noteEliminatedTeam(int team) {
        if (team < 0 || eliminated.contains(team)) {
            return;
        }
        for (UUID uuid : alive) {
            if (teamOf.getOrDefault(uuid, -1) == team) {
                return;
            }
        }
        eliminated.add(team);
    }

    @Override
    public void handleVoid(Player player) {
        UUID uuid = player.getUniqueId();
        if (isMatchSpectator(uuid)) {
            // Deja en mode spectateur (vol libre) : on ne le recadre plus, il peut voler ou il veut.
            return;
        }
        if (phase == Phase.RUNNING && alive.contains(uuid)) {
            eliminate(player, pvp.lastAttacker(player));
        }
        // Le match continue pour les autres : mode spectateur (vol libre) plutot que gradins libres.
        if (matchInProgress()) {
            becomeMatchSpectator(player);
        } else {
            arriveInStands(player);
        }
    }

    /** Apres la reapparition d'un joueur elimine : mode spectateur si le match continue, sinon gradins d'attente. */
    @Override
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
            broadcast(t("pvp.draw-2", "<yellow>Manche nulle : plus personne en vie. Seules les éliminations rapportent des points."));
            title(participants, t("pvp.draw-title", "<yellow>Manche nulle"), Component.empty(), 5, 50, 10);
        } else {
            wins.merge(winnerTeam, 1, Integer::sum);
            List<String> names = new ArrayList<>();
            for (UUID uuid : participants) {
                if (teamOf.getOrDefault(uuid, -1) == winnerTeam && members.contains(uuid)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        names.add(player.getName());
                    }
                }
            }
            Collections.sort(names);
            broadcast(t(rounds > 1 ? "pvp.round-win-2" : "pvp.win-2",
                    rounds > 1 ? "<green>Manche remportée par <aqua><names></aqua> !" : "<green>Victoire de <aqua><names></aqua> !",
                    "names", String.join(", ", names)));
            title(participants, t(rounds > 1 ? "pvp.round-win-title" : "pvp.win-title",
                            rounds > 1 ? "<green><bold>Manche gagnée !" : "<green><bold>Victoire !"),
                    t("pvp.win-sub", "<white><team>", "team", teamName(winnerTeam)), 5, 60, 15);
        }
        scoreRound();
        matchOver = decided();
        if (rounds > 1) {
            broadcast(t("pvp.score", "<gray>Score : <scoreline>", "scoreline", plugin.lang().parse(scoreLine())));
            if (matchOver) {
                announceMatchResult();
            }
        }
    }

    // ------------------------------------------------------------------ bareme

    /** Points d'un joueur pour la manche et leur detail (affiche au joueur, ecrit dans le journal). */
    private record RoundScore(double base, double multiplier, double points, int kills, int place, int level, int streak,
                              String detail) {
    }

    /**
     * Bareme d'une manche (demande de LeKiwi06, 28/09/2026). D'abord les points : eliminations, victoire (ou 2e / 3e
     * place a 3 ou 4 equipes), bonus d'inferiorite numerique pour l'equipe gagnante. Puis les multiplicateurs, ADDITIFS
     * (x1,25 et x1,5 = x1,75) : serie de manches gagnees d'affilee (gagnants seulement) et declassement du kit.
     */
    private void scoreRound() {
        int teamCount = teamSizes.size();
        int winnerSize = teamSizes.getOrDefault(winnerTeam, 1);
        int maxOpponent = 0;
        for (Map.Entry<Integer, Integer> entry : teamSizes.entrySet()) {
            if (entry.getKey() != winnerTeam) {
                maxOpponent = Math.max(maxOpponent, entry.getValue());
            }
        }
        int gap = winnerTeam < 0 ? 0 : Math.max(0, maxOpponent - winnerSize);
        List<Map<String, Object>> results = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !members.contains(uuid)) {
                continue;
            }
            int team = teamOf.getOrDefault(uuid, -1);
            boolean won = winnerTeam >= 0 && team == winnerTeam;
            int place = won ? 1 : placeOf(team, teamCount);
            // Serie : +1 a chaque manche gagnee, remise a zero a chaque manche perdue (une manche nulle ne change rien).
            int streak = pvp.streak(uuid);
            if (won) {
                streak = pvp.streak(uuid, streak + 1);
            } else if (winnerTeam >= 0) {
                streak = pvp.streak(uuid, 0);
            }
            RoundScore score = score(uuid, won, place, teamCount, gap, streak);
            boolean counted = credit(player, score);
            results.add(result(uuid, player.getName(), team, score, counted));
            if (score.points() > 0) {
                player.sendMessage(plugin.prefix().append(t(counted ? "pvp.points" : "pvp.points-uncounted",
                        counted ? "<gold>+<points> pt(s) <gray>(<detail>)"
                                : "<gold>+<points> pt(s) <gray>(<detail>) <dark_gray>- non comptés au classement",
                        "points", StatsService.formatPoints(score.points()), "detail", score.detail())));
            }
        }
        logRound(results);
    }

    /** Place d'une equipe perdante : la derniere eliminee est 2e ; 0 si inconnue (manche nulle...). */
    private int placeOf(int team, int teamCount) {
        int index = eliminated.indexOf(team);
        if (index < 0 || winnerTeam < 0) {
            return 0;
        }
        return teamCount - index;
    }

    private RoundScore score(UUID uuid, boolean won, int place, int teamCount, int gap, int streak) {
        int killCount = kills.getOrDefault(uuid, 0);
        List<String> parts = new ArrayList<>();
        double base = 0;
        if (killCount > 0) {
            int value = killCount * minigame().getInt("points-kill", 5);
            base += value;
            parts.add(killCount + " élim. " + value);
        }
        if (won) {
            int value = minigame().getInt("points-round-win", 10);
            base += value;
            parts.add("victoire " + value);
            if (gap > 0) {
                int bonus = gap * minigame().getInt("points-inferiority", 5);
                base += bonus;
                parts.add("infériorité " + bonus);
            }
        } else if (place == 2 && teamCount >= 3) {
            int value = minigame().getInt("points-second", 4);
            base += value;
            parts.add("2e " + value);
        } else if (place == 3 && teamCount >= 4) {
            int value = minigame().getInt("points-third", 2);
            base += value;
            parts.add("3e " + value);
        }
        double streakBonus = 0;
        if (won) {
            if (streak >= minigame().getInt("streak-2-wins", 5)) {
                streakBonus = minigame().getInt("streak-2-bonus-pct", 50) / 100.0;
            } else if (streak >= minigame().getInt("streak-1-wins", 3)) {
                streakBonus = minigame().getInt("streak-1-bonus-pct", 25) / 100.0;
            }
        }
        int level = roundDowngrade.getOrDefault(uuid, 0);
        double multiplier = 1 + streakBonus + downgradeBonus(level);
        double points = Math.round(base * multiplier * 100) / 100.0;
        StringBuilder detail = new StringBuilder(String.join(" + ", parts));
        if (multiplier > 1 && base > 0) {
            detail.append(" x").append(fmt(multiplier));
            List<String> why = new ArrayList<>();
            if (streakBonus > 0) {
                why.add("série de " + streak);
            }
            if (level > 0) {
                why.add("déclassement " + level);
            }
            detail.append(" : ").append(String.join(", ", why));
        }
        return new RoundScore(base, multiplier, points, killCount, place, level, streak, detail.toString());
    }

    /**
     * Credite les points au classement (general et du mois, KG_ScoreBoards, points decimaux) et note l'attribution dans
     * le journal des parties (evenement « points », comme KalGames : /classements verifier retrouve les points non comptes).
     */
    private boolean credit(Player player, RoundScore score) {
        if (score.points() <= 0) {
            return false;
        }
        String reason = plugin.scores().excluded(player) ? "operateur" : !ranked(player) ? "partie-non-classee" : null;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("player", player.getUniqueId().toString());
        fields.put("name", player.getName());
        fields.put("platform", player.getUniqueId().getMostSignificantBits() == 0 ? "bedrock" : "java");
        fields.put("points", score.points());
        fields.put("counted", reason == null);
        fields.put("reason", reason);
        plugin.ranking().log(minigame().id(), "points", fields);
        if (reason != null) {
            return false;
        }
        plugin.ranking().stats().addPoints(minigame().id(), player.getUniqueId(), player.getName(), score.points());
        plugin.ranking().boards().refreshSoon();
        return true;
    }

    private Map<String, Object> result(UUID uuid, String name, int team, RoundScore score, boolean counted) {
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("player", uuid.toString());
        one.put("name", name);
        one.put("team", team);
        one.put("kills", score.kills());
        one.put("place", score.place());
        one.put("downgrade", score.level());
        one.put("streak", score.streak());
        one.put("base", score.base());
        one.put("multiplier", score.multiplier());
        one.put("points", score.points());
        one.put("counted", counted);
        return one;
    }

    /** Detail de la manche dans le journal des parties (evenement « round ») : sert a caler le bareme. */
    private void logRound(List<Map<String, Object>> results) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("match", matchId());
        fields.put("arena", arena().id());
        fields.put("public", isPublic());
        fields.put("round", round);
        fields.put("rounds", rounds);
        fields.put("kit", chosenKit == null ? null : chosenKit.id());
        fields.put("teams", teamSizes.size());
        fields.put("winnerTeam", winnerTeam);
        fields.put("fightMillis", fightStart == 0 ? 0 : System.currentTimeMillis() - fightStart);
        fields.put("results", results);
        plugin.ranking().log(minigame().id(), "round", fields);
    }

    static String fmt(double value) {
        double rounded = Math.round(value * 100) / 100.0;
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.valueOf(rounded).replace('.', ',');
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

    String nameOf(UUID uuid) {
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
        downgrade.remove(uuid);
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
            noteEliminatedTeam(teamOf.getOrDefault(uuid, -1));
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
        kills.clear();
        eliminated.clear();
        roundDowngrade.clear();
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

    // ------------------------------------------------------------------ menus (crochets de KalGames 1.22.0)

    @Override
    public boolean openVote(Player player) {
        pvp.menus().openVote(player, this);
        return true;
    }

    @Override
    public List<Component> menuInfo(Player player) {
        List<Component> lines = new ArrayList<>();
        if (!isPublic()) {
            lines.add(t("game.body-pvp", "<gray>Manches : <white><rounds></white> - Kit : <white><kit></white>",
                    "rounds", rounds, "kit", randomKits ? "aléatoire" : "vote"));
        }
        if (maxDowngrade() > 0) {
            int level = downgradeOf(player.getUniqueId());
            lines.add(t("pvp.downgrade-line", "<gray>Déclassement du kit : <white><level></white> <dark_gray>(points x<mult>)",
                    "level", level, "mult", fmt(1 + downgradeBonus(level))));
        }
        return lines;
    }

    @Override
    public List<MenuAction> menuActions(Player player) {
        UUID uuid = player.getUniqueId();
        List<MenuAction> actions = new ArrayList<>();
        if (voteOpen(uuid)) {
            actions.add(new MenuAction(t("game.vote", "<gold>Voter pour le kit"), p -> pvp.menus().openVote(p, this)));
        }
        if (downgradeOpen(uuid)) {
            actions.add(new MenuAction(t("pvp.downgrade-button", "<light_purple>Déclassement du kit"), p -> pvp.menus().openDowngrade(p, this)));
        }
        if (!isPublic() && phase == Phase.WAITING) {
            actions.add(new MenuAction(t("game.teams", "<aqua>Équipes"), p -> pvp.menus().openTeams(p, this)));
        }
        return actions;
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
