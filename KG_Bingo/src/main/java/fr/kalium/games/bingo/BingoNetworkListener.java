package fr.kalium.games.bingo;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Repond aux demandes d'affectation envoyees par KalBingo des qu'un joueur arrive sur le
 * serveur Bingo. Design "pull" (Bingo demande, kal-games repond) retenu avec l'utilisateur
 * pour eviter un probleme de fiabilite reseau : un message "Forward" envoye vers un
 * serveur ne comptant encore AUCUN joueur connecte n'est pas garanti d'etre livre (le canal
 * BungeeCord/Velocity route via la connexion d'un joueur). En inversant le sens - Bingo
 * interroge des qu'un joueur (donc une connexion) existe chez lui, kal-games (qui a
 * quasiment toujours des joueurs en ligne) repond - les deux trajets ont toujours un
 * chemin garanti.
 */
public final class BingoNetworkListener implements PluginMessageListener {

    private static final String REQUEST_SUBCHANNEL = "KalBingoAssignRequest";
    private static final String RESPONSE_SUBCHANNEL = "KalBingoAssignResponse";
    /** Voir fr.kalium.bingo.network.PartyStatusNotifier côté KalBingo - AJOUTÉ le 24/09/2026,
     *  signale qu'une partie encore en salle d'attente vient de démarrer/être annulée, pour la
     *  retirer de la liste des parties rejoignables (BingoPartyManager.openParties). */
    private static final String PARTY_CLOSED_SUBCHANNEL = "KalBingoPartyClosed";

    private final KGBingo plugin;
    private final BingoPartyManager parties;

    public BingoNetworkListener(KGBingo plugin, BingoPartyManager parties) {
        this.plugin = plugin;
        this.parties = parties;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subChannel = in.readUTF();
            if (subChannel.equals(PARTY_CLOSED_SUBCHANNEL)) {
                handlePartyClosed(in);
                return;
            }
            if (!subChannel.equals(REQUEST_SUBCHANNEL)) {
                return; // pas pour nous (ex: reponses "GetServers" d'un autre usage du canal)
            }
            short len = in.readShort();
            byte[] payload = new byte[len];
            in.readFully(payload);
            DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));
            UUID requested = UUID.fromString(payloadIn.readUTF());

            // "carrier" est un joueur QUELCONQUE actuellement en ligne sur kal-games (choisi par le
            // proxy pour acheminer le message forwarde) - PAS forcement le joueur concerne par la
            // demande. Ne jamais s'y fier pour l'identite du joueur affecte, seulement pour l'envoi.
            BingoParty party = parties.partyOf(requested);
            respond(carrier, requested, party);
        } catch (IOException e) {
            plugin.getLogger().warning("[Bingo] Message d'affectation illisible : " + e.getMessage());
        }
    }

    /** Une partie encore en salle d'attente vient de démarrer/être annulée côté KalBingo - voir
     *  PARTY_CLOSED_SUBCHANNEL ci-dessus. On l'oublie ici aussi : elle ne doit plus apparaître
     *  comme rejoignable dans le menu (BingoPartyManager.openParties). */
    private void handlePartyClosed(DataInputStream in) throws IOException {
        short len = in.readShort();
        byte[] payload = new byte[len];
        in.readFully(payload);
        DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));
        String gameId = payloadIn.readUTF();
        parties.remove(gameId);
    }

    private void respond(Player carrier, UUID requested, BingoParty party) {
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            DataOutputStream payloadOut = new DataOutputStream(payloadBytes);
            payloadOut.writeUTF(requested.toString());
            payloadOut.writeBoolean(party != null);
            if (party != null) {
                payloadOut.writeUTF(party.gameId());
                payloadOut.writeLong(party.seed());
                payloadOut.writeLong(party.duration().getSeconds());
                payloadOut.writeUTF(party.host().toString());
                payloadOut.writeInt(party.teamCount());
                payloadOut.writeInt(party.teamSize());
                payloadOut.writeInt(party.roster().size());
                for (UUID member : party.roster()) {
                    payloadOut.writeUTF(member.toString());
                }
                payloadOut.writeUTF(party.rules()); // 1.1.0 : lu par KG_BingoGame 0.3.0+ (ignore par les plus anciens)
            }
            byte[] payload = payloadBytes.toByteArray();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Forward");
            out.writeUTF(plugin.getConfig().getString("bingo.server-name", "kixster"));
            out.writeUTF(RESPONSE_SUBCHANNEL);
            out.writeShort(payload.length);
            out.write(payload);

            carrier.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("[Bingo] Impossible d'envoyer la reponse d'affectation : " + e.getMessage());
        }
    }
}
