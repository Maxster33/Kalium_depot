package fr.kalium.bingo.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prévient kal-games qu'une partie EN ATTENTE (salle d'attente, voir PartyManager) vient de
 * disparaître - démarrée (PartyStarter) ou annulée (PartyCanceller) - AJOUTÉ le 24/09/2026,
 * demande explicite de l'utilisateur : "il faut pouvoir voir les parties qui sont créées et
 * encore en salle d'attente dans le menu kalgames de manière à pouvoir la rejoindre
 * facilement". Voir BingoPartyManager.openParties()/remove(gameId) côté kal-games.
 *
 * CORRIGÉ en 0.1.19 (bug rapporté : "les parties qui sont terminées ne doivent plus apparaître
 * dans la liste des parties sur le menu de KalGames"). La version d'origine n'envoyait ce signal
 * QUE par le canal "Forward" en supposant qu'il était toujours livrable parce qu'il part d'un
 * joueur connecté ICI. C'était faux : un Forward n'est livré que si au moins un joueur est
 * connecté sur le serveur CIBLE (kal-games). Au démarrage d'une partie, tous les joueurs sont en
 * général sur Kixster et kal-games est vide : le signal était perdu et la partie restait listée.
 *
 * Désormais, en plus du Forward (instantané quand kal-games a du monde) : publication sur le
 * relais HTTP (KaliumRelay, indépendant de tout joueur - voir RelayClient.postPartyClosed), que
 * kal-games interroge toutes les 5 s pour les parties qu'il liste encore. Une entrée du relais
 * expire au bout de 2 min : si kal-games ne l'a pas lue entre-temps (serveur vide mis en pause par
 * le jeu, redémarrage...), elle est republiée toutes les minutes pendant REPUBLISH_FOR_MILLIS.
 */
public final class PartyStatusNotifier {

    public static final String SUBCHANNEL = "KalBingoPartyClosed";

    /** Durée pendant laquelle une fermeture est republiée sur le relais (voir la classe). */
    private static final long REPUBLISH_FOR_MILLIS = 6L * 60 * 60 * 1000;
    private static final long REPUBLISH_INTERVAL_TICKS = 20L * 60;

    private final JavaPlugin plugin;
    private final String kalGamesServerName;
    private final RelayClient relayClient;

    /** gameId fermé -&gt; instant de fermeture (voir republish()). Manipulé sur le thread principal. */
    private final Map<String, Long> closedAt = new HashMap<>();

    public PartyStatusNotifier(JavaPlugin plugin, String kalGamesServerName, RelayClient relayClient) {
        this.plugin = plugin;
        this.kalGamesServerName = kalGamesServerName;
        this.relayClient = relayClient;
        Bukkit.getScheduler().runTaskTimer(plugin, this::republish, REPUBLISH_INTERVAL_TICKS, REPUBLISH_INTERVAL_TICKS);
    }

    /** @param carrier un joueur actuellement en ligne sur ce serveur, pour le Forward (peut être null :
     *                  le relais suffit). */
    public void notifyClosed(String gameId, Player carrier) {
        closedAt.put(gameId, System.currentTimeMillis());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> relayClient.postPartyClosed(gameId));
        if (carrier != null && carrier.isOnline()) {
            sendForward(gameId, carrier);
        }
    }

    private void republish() {
        long now = System.currentTimeMillis();
        closedAt.values().removeIf(at -> now - at > REPUBLISH_FOR_MILLIS);
        if (closedAt.isEmpty()) {
            return;
        }
        List<String> gameIds = new ArrayList<>(closedAt.keySet());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> gameIds.forEach(relayClient::postPartyClosed));
    }

    private void sendForward(String gameId, Player carrier) {
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payloadOut = new DataOutputStream(payloadBytes);
            payloadOut.writeUTF(gameId);
            byte[] payload = payloadBytes.toByteArray();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Forward");
            out.writeUTF(kalGamesServerName);
            out.writeUTF(SUBCHANNEL);
            out.writeShort(payload.length);
            out.write(payload);

            carrier.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("[KalBingo] Impossible de prévenir kal-games de la fermeture de la partie '"
                    + gameId + "' : " + e.getMessage());
        }
    }
}
