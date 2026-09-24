package fr.kalium.bingo.network;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Reçoit la réponse d'affectation envoyée par kal-games (voir AssignmentService). */
public final class AssignmentNetworkListener implements PluginMessageListener {

    private final JavaPlugin plugin;
    private final AssignmentService assignmentService;

    public AssignmentNetworkListener(JavaPlugin plugin, AssignmentService assignmentService) {
        this.plugin = plugin;
        this.assignmentService = assignmentService;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subChannel = in.readUTF();
            if (!subChannel.equals(AssignmentService.RESPONSE_SUBCHANNEL)) {
                return; // pas pour nous
            }
            short len = in.readShort();
            byte[] payload = new byte[len];
            in.readFully(payload);
            DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));

            UUID playerId = UUID.fromString(payloadIn.readUTF());
            boolean found = payloadIn.readBoolean();
            if (!found) {
                assignmentService.handleResponse(playerId, false, null, 0L, 0L, null, 0, 0, null, null);
                return;
            }
            String gameId = payloadIn.readUTF();
            long seed = payloadIn.readLong();
            long durationSeconds = payloadIn.readLong();
            UUID host = UUID.fromString(payloadIn.readUTF());
            int teamCount = payloadIn.readInt();
            int teamSize = payloadIn.readInt();
            int rosterSize = payloadIn.readInt();
            Set<UUID> roster = new HashSet<>();
            for (int i = 0; i < rosterSize; i++) {
                roster.add(UUID.fromString(payloadIn.readUTF()));
            }
            // 0.3.0 : reglages de partie (voir BingoSettings), absents si KG_Bingo est plus ancien.
            String rules = payloadIn.available() > 0 ? payloadIn.readUTF() : null;
            assignmentService.handleResponse(playerId, true, gameId, seed, durationSeconds, host, teamCount, teamSize, roster, rules);
        } catch (IOException e) {
            plugin.getLogger().warning("[KG_BingoGame] Réponse d'affectation illisible : " + e.getMessage());
        }
    }
}
