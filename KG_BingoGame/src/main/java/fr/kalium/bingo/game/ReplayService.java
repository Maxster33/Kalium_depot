package fr.kalium.bingo.game;

import fr.kalium.bingo.gui.LobbyItems;
import fr.kalium.bingo.network.RelayClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.11.0 - bouton « Rejouer » (demande de LeKiwi06, 09/10/2026 : « pour tous les mini-jeux : faire un bouton à la fin
 * de la partie pour relancer une partie avec les mêmes paramètres », propose pendant 30 secondes).
 *
 * A la fin d'une partie, chaque joueur place dans la salle d'attente d'apres-partie recoit l'objet « Rejouer » pour
 * game.replay-seconds (30 s). Au clic, les reglages de la partie terminee (equipes, taille, duree, mode, grille) sont
 * deposes sur le relais (cle « bingo-rejouer-&lt;uuid&gt; ») puis le joueur est renvoye sur kal-games, ou KG_Bingo
 * (1.9.0) recree la partie avec ces reglages et une nouvelle seed - ou lui fait rejoindre celle qu'un autre joueur a
 * deja relancee - et le renvoie aussitot ici, dans la nouvelle salle d'attente. Les parties ne se creent que sur
 * kal-games (limites, liste des parties, codes) : d'ou cet aller-retour.
 *
 * Le premier qui clique devient l'hote ; l'objet des autres devient « Rejoindre la partie de X ».
 */
public final class ReplayService {

    private record Offer(String endedGameId, String settings) {
    }

    private final JavaPlugin plugin;
    private final LobbyItems lobbyItems;
    private final RelayClient relayClient;
    private GameEndService gameEndService;
    private final Map<UUID, Offer> offers = new HashMap<>();
    /** Partie terminee -> pseudo du premier joueur qui l'a relancee. */
    private final Map<String, String> relaunchedBy = new HashMap<>();
    /** Joueurs dont le depot sur le relais est en cours (double clic). */
    private final Set<UUID> sending = new HashSet<>();

    public ReplayService(JavaPlugin plugin, LobbyItems lobbyItems, RelayClient relayClient) {
        this.plugin = plugin;
        this.lobbyItems = lobbyItems;
        this.relayClient = relayClient;
    }

    public void setGameEndService(GameEndService gameEndService) {
        this.gameEndService = gameEndService;
    }

    private int seconds() {
        return Math.max(5, plugin.getConfig().getInt("game.replay-seconds", 30));
    }

    /** Fin de partie : les joueurs places dans la salle d'attente d'apres-partie recoivent l'objet « Rejouer ». */
    public void offer(BingoGame game, Collection<Player> players) {
        if (players.isEmpty()) {
            return;
        }
        String ended = game.getGameId();
        String settings = "ended=" + ended + "\n"
                + "teamCount=" + game.replayTeamCount() + "\n"
                + "teamSize=" + game.replayTeamSize() + "\n"
                + "duration=" + game.replayDurationSeconds() + "\n"
                + "rules=" + game.getSettings().encode();
        Offer offer = new Offer(ended, settings);
        int seconds = seconds();
        for (Player player : players) {
            offers.put(player.getUniqueId(), offer);
            lobbyItems.giveReplay(player, null);
            player.sendMessage("§aEnvie de rejouer ? §7Utilisez l'objet §fRejouer§7 de votre barre (" + seconds + " s).");
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> expire(offer), seconds * 20L);
    }

    private void expire(Offer offer) {
        var iterator = offers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() != offer) {
                continue;
            }
            iterator.remove();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                lobbyItems.removeReplay(player);
            }
        }
        relaunchedBy.remove(offer.endedGameId());
    }

    /** Deconnexion : la proposition est perdue. */
    public void forget(UUID playerId) {
        offers.remove(playerId);
        sending.remove(playerId);
    }

    /** Clic droit avec l'objet « Rejouer ». */
    public void use(Player player) {
        UUID playerId = player.getUniqueId();
        Offer offer = offers.get(playerId);
        if (offer == null || gameEndService == null || !gameEndService.isLingering(playerId)) {
            offers.remove(playerId);
            lobbyItems.removeReplay(player);
            player.sendMessage("§cIl est trop tard pour rejouer cette partie.");
            return;
        }
        if (!sending.add(playerId)) {
            return;
        }
        player.sendMessage("§7Relance de la partie…");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean posted = relayClient.postReplay(playerId, offer.settings());
            Bukkit.getScheduler().runTask(plugin, () -> sent(playerId, offer, posted));
        });
    }

    private void sent(UUID playerId, Offer offer, boolean posted) {
        if (!sending.remove(playerId)) {
            return; // deconnecte entre-temps
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        if (!posted) {
            player.sendMessage("§cImpossible de relancer la partie pour l'instant (relais injoignable).");
            return;
        }
        offers.remove(playerId);
        if (relaunchedBy.putIfAbsent(offer.endedGameId(), player.getName()) == null) {
            // Premier a relancer : l'objet des autres joueurs de cette partie devient « Rejoindre la partie de X ».
            for (Map.Entry<UUID, Offer> entry : offers.entrySet()) {
                Player other = entry.getValue() == offer ? Bukkit.getPlayer(entry.getKey()) : null;
                if (other != null && lobbyItems.hasReplay(other)) {
                    lobbyItems.giveReplay(other, player.getName());
                    other.sendMessage("§f" + player.getName() + "§e a relancé la partie : utilisez l'objet §fRejoindre§e pour le suivre.");
                }
            }
        }
        // Retour sur kal-games (objets retires, emplacement de salle d'attente libere), ou KG_Bingo recree ou fait
        // rejoindre la partie, puis renvoie le joueur ici.
        gameEndService.leaveVoluntarily(player);
    }
}
