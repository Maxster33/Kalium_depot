package fr.kalium.bingo.network;

import fr.kalium.bingo.game.BingoParty;
import fr.kalium.bingo.game.GameManager;
import fr.kalium.bingo.game.PartyManager;
import fr.kalium.bingo.gui.LobbyItems;
import fr.kalium.bingo.world.InstanceWorldPreparer;
import fr.kalium.bingo.world.LobbySlots;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Demande a kal-games "quelle partie pour ce joueur ?" des qu'un joueur arrive sur ce
 * serveur sans affectation connue. Design "pull" (Bingo demande, kal-games repond) retenu
 * avec l'utilisateur pour eviter un probleme de fiabilite reseau : un message envoye vers
 * un serveur ne comptant encore AUCUN joueur connecte n'est pas garanti d'etre livre (le
 * canal BungeeCord/Velocity route via la connexion d'un joueur) - alors qu'ici, Bingo
 * n'interroge qu'une fois qu'un joueur (donc une connexion) existe chez lui.
 *
 * Place le joueur dans la salle d'attente correspondante des que la reponse arrive ; le
 * renvoie vers kal-games si rien n'arrive dans le delai configure
 * (reconnect-during-game.assignment-wait-seconds).
 *
 * La requete est REEMISE PERIODIQUEMENT pendant toute la fenetre d'attente (pas un envoi
 * unique) : le probleme de fiabilite documente ci-dessus pour le sens "kal-games -> Bingo"
 * (un Forward vers un serveur sans aucun joueur connecte n'est pas garanti d'etre livre)
 * peut en realite aussi toucher CE sens-la en test solo - au moment ou l'hote cree la partie
 * et est transfere, il peut etre le SEUL joueur present sur kal-games, qui se retrouve donc a
 * 0 joueur juste apres son depart, exactement le cas que le design "pull" essayait d'eviter,
 * mais dans l'autre sens. Reemettre la requete plusieurs fois laisse une chance a un nouveau
 * joueur/carrier de se (re)connecter a kal-games entre-temps.
 *
 * EN PARALLELE de ce canal (garde comme filet de securite), un second chemin totalement
 * independant de tout joueur connecte est interroge : le relais HTTP KaliumRelay (plugin
 * Velocity separe, sur le proxy), ou kal-games depose l'affectation directement au moment du
 * transfert (voir BingoPartyManager.transferToBingo). Comme il ne depend d'aucun joueur, ni
 * ici ni sur kal-games, il resout en general le probleme bien plus vite et plus fiablement -
 * voir RelayClient. La premiere des deux voies a resoudre gagne (pending.remove() garantit
 * qu'une resolution tardive de l'autre voie est ignoree, voir handleResponse).
 */
public final class AssignmentService {

    public static final String REQUEST_SUBCHANNEL = "KG_BingoGameAssignRequest";
    public static final String RESPONSE_SUBCHANNEL = "KG_BingoGameAssignResponse";

    /** Intervalle entre deux reemissions de la requete, tant qu'aucune reponse n'est arrivee. */
    private static final long RETRY_INTERVAL_TICKS = 40L; // 2s

    /** Le relais HTTP est rapide et fiable (independant de tout joueur) : pas besoin d'attendre
     *  aussi longtemps qu'entre deux reemissions du canal BungeeCord. */
    private static final long RELAY_POLL_INTERVAL_TICKS = 10L; // 0.5s

    private final JavaPlugin plugin;
    private final PartyManager partyManager;
    private final GameManager gameManager;
    private final LobbySlots lobbySlots;
    private final LobbyItems lobbyItems;
    private final RelayClient relayClient;
    private final InstanceWorldPreparer instanceWorldPreparer;
    private final String kalGamesServerName;
    private final long waitTicks;

    /** Joueurs actuellement en attente d'une reponse (pour ignorer une reponse tardive/dupliquee,
     *  quelle que soit la voie - canal reseau ou relais HTTP - qui resout en premier). */
    private final Set<UUID> pending = new HashSet<>();

    public AssignmentService(JavaPlugin plugin, PartyManager partyManager, GameManager gameManager, LobbySlots lobbySlots,
                              LobbyItems lobbyItems, RelayClient relayClient, InstanceWorldPreparer instanceWorldPreparer,
                              String kalGamesServerName, Duration waitDelay) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.gameManager = gameManager;
        this.lobbySlots = lobbySlots;
        this.lobbyItems = lobbyItems;
        this.relayClient = relayClient;
        this.instanceWorldPreparer = instanceWorldPreparer;
        this.kalGamesServerName = kalGamesServerName;
        this.waitTicks = Math.max(20L, waitDelay.getSeconds() * 20L);
    }

    /** A appeler quand un joueur arrive (PlayerJoinEvent), connu ou non - la demande sert aussi de re-confirmation. */
    public void requestAssignment(Player player) {
        UUID playerId = player.getUniqueId();
        pending.add(playerId);
        sendRequest(player);
        scheduleRetries(playerId, RETRY_INTERVAL_TICKS);
        pollRelay(playerId, 0L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!pending.remove(playerId)) {
                return; // deja resolu entre-temps
            }
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                handleNoGameFound(p);
            }
        }, waitTicks);
    }

    /**
     * Interroge le relais HTTP (voir RelayClient) toutes les RELAY_POLL_INTERVAL_TICKS, en
     * parallele du canal BungeeCord ci-dessus. Appel reseau bloquant donc TOUJOURS effectue de
     * maniere asynchrone (jamais sur le thread principal) ; le traitement du resultat repasse sur
     * le thread principal avant de toucher l'API Bukkit (teleportation, etc.).
     */
    private void pollRelay(UUID playerId, long elapsed) {
        if (elapsed >= waitTicks || !pending.contains(playerId)) {
            return; // le timeout global s'en charge, ou deja resolu
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String body = relayClient.fetch(playerId);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!pending.contains(playerId)) {
                    return; // deja resolu entre-temps (canal reseau, ou timeout)
                }
                if (body != null) {
                    applyRelayAssignment(playerId, body);
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> pollRelay(playerId, elapsed + RELAY_POLL_INTERVAL_TICKS), RELAY_POLL_INTERVAL_TICKS);
                }
            });
        });
    }

    private void applyRelayAssignment(UUID playerId, String body) {
        try {
            Map<String, String> fields = new HashMap<>();
            for (String line : body.split("\n")) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    fields.put(line.substring(0, idx), line.substring(idx + 1));
                }
            }
            String gameId = fields.get("gameId");
            long seed = Long.parseLong(fields.get("seed"));
            long durationSeconds = Long.parseLong(fields.get("duration"));
            UUID host = UUID.fromString(fields.get("host"));
            int teamCount = Integer.parseInt(fields.get("teamCount"));
            int teamSize = Integer.parseInt(fields.get("teamSize"));
            Set<UUID> roster = new HashSet<>();
            String rosterField = fields.getOrDefault("roster", "");
            if (!rosterField.isBlank()) {
                for (String uuid : rosterField.split(",")) {
                    roster.add(UUID.fromString(uuid));
                }
            }
            handleResponse(playerId, true, gameId, seed, durationSeconds, host, teamCount, teamSize, roster, fields.get("rules"));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("[KG_BingoGame] Reponse du relais HTTP illisible pour " + playerId + " : " + e.getMessage());
        }
    }

    /** Reemet la requete toutes les RETRY_INTERVAL_TICKS tant que le joueur est encore en attente. */
    private void scheduleRetries(UUID playerId, long delay) {
        if (delay >= waitTicks) {
            return; // plus de reemission au-dela de la fenetre d'attente, le timeout s'en charge
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!pending.contains(playerId)) {
                return; // deja resolu (succes ou "not found" explicite)
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return;
            }
            sendRequest(player);
            scheduleRetries(playerId, delay + RETRY_INTERVAL_TICKS);
        }, delay);
    }

    private void sendRequest(Player player) {
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payloadOut = new DataOutputStream(payloadBytes);
            payloadOut.writeUTF(player.getUniqueId().toString());
            byte[] payload = payloadBytes.toByteArray();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Forward");
            out.writeUTF(kalGamesServerName);
            out.writeUTF(REQUEST_SUBCHANNEL);
            out.writeShort(payload.length);
            out.write(payload);

            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("[KG_BingoGame] Impossible d'envoyer la demande d'affectation : " + e.getMessage());
        }
    }

    /** Appele par AssignmentNetworkListener a la reception d'une reponse de kal-games. */
    public void handleResponse(UUID playerId, boolean found, String gameId, long seed, long durationSeconds,
                                UUID host, int teamCount, int teamSize, Set<UUID> roster, String rules) {
        if (!pending.remove(playerId)) {
            return; // deja resolu par l'autre voie (canal BungeeCord ou relais HTTP) ou par le timeout : on ignore
        }

        if (!found) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                handleNoGameFound(player);
            }
            return;
        }

        // Garde-fou AJOUTE le 24/09/2026 (corrige un bug rapporte par l'utilisateur : un joueur qui
        // abandonne une partie EN COURS puis essaie de la rejoindre depuis kal-games se retrouvait
        // renvoye dans une salle d'attente VIDE et fantome). Si ce gameId correspond DEJA a une
        // partie active ici (PartyStarter.start() l'a deja convertie, elle n'est donc plus dans
        // PartyManager), c'est que kal-games pensait encore cette partie "en salle d'attente" -
        // signal de fermeture non recu ou en retard (voir PartyStatusNotifier) - mais elle a bien
        // demarre. PartyManager.getOrCreate ne sait pas faire la difference entre "partie jamais vue"
        // et "partie deja demarree" et creerait sinon une PARTIE FANTOME vide (jamais reclamee,
        // jamais lancee) au lieu de refuser. On refuse explicitement plutot que de laisser
        // getOrCreate() faire cette confusion.
        if (gameManager.getGame(gameId) != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage("§cCette partie a déjà démarré, vous ne pouvez plus la rejoindre depuis kal-games.");
                sendBackToKalGames(player);
            }
            return;
        }

        BingoParty party = partyManager.getOrCreate(gameId, seed, Duration.ofSeconds(durationSeconds), host, teamCount, teamSize,
                roster, () -> instanceWorldPreparer.startPreGeneration(gameId, seed, teamCount));
        if (rules != null) {
            party.setSettings(fr.kalium.bingo.game.BingoSettings.parse(rules)); // 0.3.0 : mode, bingos, composition
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return; // reconnectera plus tard, sa partie est deja enregistree
        }

        Location spawn = lobbySlots.reserve(party.getGameId());
        if (spawn == null) {
            player.sendMessage("§cSalle d'attente indisponible (aucune salle définie, ou toutes occupées).");
            sendBackToKalGames(player);
            return;
        }
        party.markConnected(playerId);
        player.teleport(spawn);
        // 0.3.1 : mode aventure dans la salle d'attente (demande de LeKiwi06, 24/09/2026), survie sur la map.
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        lobbyItems.give(player);
        player.sendMessage("§aBienvenue dans la salle d'attente Bingo ! Choisissez votre équipe : /bingoteam <1-"
                + party.getTeamCount() + "|random> (ou l'objet de menu reçu).");
    }

    /**
     * Aucune partie trouvee pour ce joueur (ni reponse "found=false", ni reponse du tout avant le
     * delai). Un joueur normal est renvoye vers kal-games (comportement d'origine). Un operateur
     * (permission "bingo.admin", meme celle qui protege /bingoadmin - "les operateurs" demandes par
     * l'utilisateur) reste sur Bingo a la place : il doit pouvoir construire/modifier la salle
     * d'attente (/bingoadmin lobby tp ...) sans etre renvoye au bout de
     * reconnect-during-game.assignment-wait-seconds simplement parce qu'aucune partie n'est en cours.
     */
    private void handleNoGameFound(Player player) {
        if (player.hasPermission("bingo.admin")) {
            player.sendMessage("§7Aucune partie Bingo en cours pour vous - vous restez sur le serveur (opérateur). "
                    + "Utilisez /bingoadmin lobby tp pour accéder à la salle d'attente.");
            return;
        }
        player.sendMessage("§cAucune partie Bingo trouvée pour vous, retour à kal-games.");
        sendBackToKalGames(player);
    }

    /** Expose pour PartyCanceller (annulation de partie depuis le menu joueur, voir PartyMenu). */
    public void sendBackToKalGames(Player player) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(kalGamesServerName);
            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException ignored) {
            // rien de plus a faire : le joueur restera sur Bingo, il pourra reessayer via une reconnexion
        }
    }
}
