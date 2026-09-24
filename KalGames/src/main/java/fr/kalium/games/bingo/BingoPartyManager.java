package fr.kalium.games.bingo;

import fr.kalium.games.KalGames;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creation/jointure des parties Bingo cote kal-games (demande explicite de l'utilisateur :
 * le host cree une partie, ceux qui la rejoignent par code sont transferes IMMEDIATEMENT
 * vers le serveur Bingo - pas de salle d'attente ici, elle vit sur le serveur Bingo
 * lui-meme, voir KalBingo).
 *
 * La duree de partie est choisie par l'hote a la creation, plafonnee par
 * bingo.max-duration-minutes (reglable par un operateur dans Parametres > Bingo) : c'est bien
 * ICI, cote kal-games, que ce reglage vit, pas dans KalBingo. La seed est tiree aleatoirement a
 * chaque partie.
 */
public final class BingoPartyManager {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final KalGames plugin;
    private final Map<String, BingoParty> byCode = new HashMap<>();
    private final Map<String, BingoParty> byGameId = new HashMap<>();
    private final Map<UUID, BingoParty> byPlayer = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public BingoPartyManager(KalGames plugin) {
        this.plugin = plugin;
    }

    /** Utilise les valeurs d'equipes/duree par defaut de config.yml (bingo.default-team-count/-size,
     *  bingo.duration-seconds) - voir /bingo create. */
    public BingoParty create(Player host) {
        int defaultCount = Math.max(1, plugin.getConfig().getInt("bingo.default-team-count", 2));
        int defaultSize = Math.max(1, plugin.getConfig().getInt("bingo.default-team-size", 4));
        return create(host, defaultCount, defaultSize);
    }

    /**
     * @param teamCount nombre d'equipes voulu pour cette partie (choisi par l'hote, voir
     *                   PlayerMenus.openBingoMenu), borne a [1, bingo.max-team-count]
     * @param teamSize   taille max par equipe, bornee a [1, bingo.max-team-size]
     */
    public BingoParty create(Player host, int teamCount, int teamSize) {
        Duration defaultDuration = Duration.ofSeconds(Math.max(60, plugin.getConfig().getLong("bingo.duration-seconds", 3600)));
        return create(host, teamCount, teamSize, defaultDuration);
    }

    /**
     * @param teamCount nombre d'equipes voulu pour cette partie (choisi par l'hote, voir
     *                   PlayerMenus.openBingoMenu), borne a [1, bingo.max-team-count]
     * @param teamSize   taille max par equipe, bornee a [1, bingo.max-team-size]
     * @param duration   duree de la partie CHOISIE PAR L'HOTE a la creation (demande explicite de
     *                    l'utilisateur, 23/09/2026 : "il faut ajouter la possibilité de choisir la
     *                    durée de la partie lorsqu'on la crée sur Kal") - bornee a
     *                    [bingo.min-duration-minutes, bingo.max-duration-minutes] (5-60 min par
     *                    defaut ; le plafond max-duration-minutes est modifiable par un operateur
     *                    via Parametres > Bingo, voir AdminMenus.openBingoSettings - l'hote ne peut
     *                    que le REDUIRE, jamais le depasser, meme principe que teamCount/teamSize
     *                    ci-dessus). Le formulaire de creation (voir PlayerMenus.openBingoCreate) est
     *                    pre-rempli avec ce plafond. bingo.duration-seconds reste le repli pour les
     *                    appelants qui ne proposent pas ce choix (/bingo create, voir BingoCommand).
     */
    public BingoParty create(Player host, int teamCount, int teamSize, Duration duration) {
        String gameId = UUID.randomUUID().toString();
        String code;
        do {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (byCode.containsKey(code));

        long seed = random.nextLong();
        int maxTeamCount = Math.max(1, plugin.getConfig().getInt("bingo.max-team-count", 4));
        int maxTeamSize = Math.max(1, plugin.getConfig().getInt("bingo.max-team-size", 4));
        int clampedCount = Math.max(1, Math.min(maxTeamCount, teamCount));
        int clampedSize = Math.max(1, Math.min(maxTeamSize, teamSize));

        long minDurationSeconds = Math.max(60, plugin.getConfig().getInt("bingo.min-duration-minutes", 5) * 60L);
        long maxDurationSeconds = Math.max(minDurationSeconds, plugin.getConfig().getInt("bingo.max-duration-minutes", 60) * 60L);
        long requestedSeconds = duration == null ? minDurationSeconds : duration.getSeconds();
        Duration clampedDuration = Duration.ofSeconds(Math.max(minDurationSeconds, Math.min(maxDurationSeconds, requestedSeconds)));

        BingoParty party = new BingoParty(gameId, code, host.getUniqueId(), seed, clampedDuration, clampedCount, clampedSize);
        byCode.put(code, party);
        byGameId.put(gameId, party);
        byPlayer.put(host.getUniqueId(), party);
        return party;
    }

    public BingoParty partyOf(UUID playerId) {
        return byPlayer.get(playerId);
    }

    public BingoParty byGameId(String gameId) {
        return byGameId.get(gameId);
    }

    /**
     * Parties ENCORE EN SALLE D'ATTENTE (pas démarrées, pas annulées - voir PartyStatusNotifier
     * côté KalBingo qui retire une partie de cette liste dès qu'elle démarre/est annulée, via
     * remove(gameId) ci-dessous) - AJOUTÉ le 24/09/2026, demande explicite de l'utilisateur : "il
     * faut pouvoir voir les parties qui sont créées et encore en salle d'attente dans le menu
     * kalgames de manière à pouvoir la rejoindre facilement" - voir PlayerMenus.openBingoMenu.
     * Les plus récemment créées en premier (les plus susceptibles d'être encore en train de se
     * remplir), limitées à `limit` pour tenir dans un menu sans pagination.
     */
    public List<BingoParty> openParties(int limit) {
        return byGameId.values().stream()
                .sorted(Comparator.comparing(BingoParty::createdAt).reversed())
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());
    }

    /** Rejoint une partie par code. Ne transfere PAS le joueur elle-meme (voir transferToBingo, appele par BingoCommand). */
    public BingoParty join(Player player, String code) {
        BingoParty party = byCode.get(code.toUpperCase());
        if (party == null) {
            return null;
        }
        // Capacite REELLE de CETTE partie (equipes x taille choisies par l'hote a la creation) -
        // corrige le 24/09/2026 : utilisait auparavant le plafond global bingo.max-party-size (16),
        // qui n'a jamais correspondu a la config choisie par l'hote (ex. une partie 2 equipes x 1
        // pouvait accepter jusqu'a 16 joueurs au lieu de 2) - voir BingoParty.maxPlayers().
        if (party.roster().size() >= party.maxPlayers()) {
            return null;
        }
        party.roster().add(player.getUniqueId());
        byPlayer.put(player.getUniqueId(), party);
        return party;
    }

    /**
     * Transfere le joueur vers le serveur Bingo configure (bingo.server-name), meme mecanisme que
     * connectLobby(). Depose AUSSI son affectation sur le relais HTTP (KaliumRelay, cote proxy)
     * avant le transfert : contrairement au canal BungeeCord "Connect" ci-dessous (qui ne concerne
     * QUE ce joueur, deja connecte ici - toujours fiable), le relais evite a KalBingo de devoir
     * redemander cette information a kal-games apres coup via un message qui, lui, ne serait pas
     * garanti d'etre livre si kal-games se retrouvait sans aucun joueur en ligne entre-temps.
     */
    public void transferToBingo(Player player) {
        BingoParty party = byPlayer.get(player.getUniqueId());
        if (party != null) {
            pushAssignment(player.getUniqueId(), party);
        }
        String server = plugin.getConfig().getString("bingo.server-name", "kixster");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            return;
        }
        player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
    }

    /**
     * Depot direct (independant de tout joueur connecte sur Kixster) sur le relais HTTP - voir
     * KaliumRelay (projet Velocity separe) et AssignmentService cote KalBingo, qui va le
     * recuperer des que ce joueur se connecte chez lui. Silencieux si relay-url n'est pas
     * configure (fonctionnalite optionnelle) ou si l'appel echoue (log seulement) : ce depot est
     * un COMPLEMENT au transfert lui-meme, jamais un prerequis bloquant pour deplacer le joueur.
     */
    private void pushAssignment(UUID playerId, BingoParty party) {
        String url = plugin.getConfig().getString("bingo.relay-url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        String token = plugin.getConfig().getString("bingo.relay-token", "");
        String body = "gameId=" + party.gameId() + "\n"
                + "seed=" + party.seed() + "\n"
                + "duration=" + party.duration().getSeconds() + "\n"
                + "host=" + party.host() + "\n"
                + "teamCount=" + party.teamCount() + "\n"
                + "teamSize=" + party.teamSize() + "\n"
                + "roster=" + party.roster().stream().map(UUID::toString).collect(Collectors.joining(","));
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/assignment/" + playerId))
                    .header("X-Kalium-Relay-Token", token)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            http.sendAsync(request, HttpResponse.BodyHandlers.discarding()).exceptionally(e -> {
                plugin.getLogger().warning("[Bingo] Relais HTTP injoignable (" + e.getMessage() + ") - "
                        + "KalBingo redemandera via le canal reseau habituel.");
                return null;
            });
        } catch (Exception e) {
            plugin.getLogger().warning("[Bingo] Erreur en preparant le depot sur le relais : " + e.getMessage());
        }
    }

    /**
     * AJOUTE en 1.10.7 (bug rapporte : "les parties qui sont terminées ne doivent plus apparaître
     * dans la liste des parties sur le menu de KalGames"). Le signal "partie fermee" de KalBingo
     * passait uniquement par le canal Forward, qui n'est livre que si au moins un joueur est
     * connecte ICI - or au demarrage d'une partie tout le monde est en general sur Kixster, donc le
     * signal se perdait. KalBingo le publie desormais aussi sur le relais HTTP (cle
     * "party-closed-<gameId>", voir PartyStatusNotifier/RelayClient.postPartyClosed cote KalBingo) :
     * interroge ici toutes les 5 s (voir KalGames.onEnable) pour chaque partie encore listee, sans
     * bloquer le thread principal. Silencieux si relay-url n'est pas configure.
     */
    public void pollClosedParties() {
        String url = plugin.getConfig().getString("bingo.relay-url", "");
        if (url == null || url.isBlank() || byGameId.isEmpty()) {
            return;
        }
        String token = plugin.getConfig().getString("bingo.relay-token", "");
        for (String gameId : List.copyOf(byGameId.keySet())) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/assignment/party-closed-" + gameId))
                        .header("X-Kalium-Relay-Token", token)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                http.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> remove(gameId));
                    }
                }).exceptionally(e -> null); // relais momentanement injoignable : on reessaiera au prochain tour
            } catch (Exception e) {
                plugin.getLogger().warning("[Bingo] Erreur en interrogeant le relais : " + e.getMessage());
                return;
            }
        }
    }

    /** Nettoyage apres transfert de toute la partie, ou code expire/abandonne. */
    public void remove(String gameId) {
        BingoParty party = byGameId.remove(gameId);
        if (party == null) {
            return;
        }
        byCode.remove(party.code());
        for (UUID uuid : party.roster()) {
            byPlayer.remove(uuid);
        }
    }
}
