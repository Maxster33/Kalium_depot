package fr.kalium.bingo.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Une instance = un monde Overworld independant, assigne a UNE SEULE equipe
 * (1 a 4 joueurs) pour toute la duree de la partie (section 0 et section 7
 * du cahier des charges - le principe "un joueur = une instance" s'applique
 * ici au niveau de l'equipe : tous les membres d'une meme equipe partagent
 * la meme instance, exactement comme des coequipiers dans un meme monde).
 *
 * Isolation attendue entre instances (section 7) :
 *  - pas de teleportation entre instances par les joueurs
 *  - pas d'interaction entre joueurs d'instances differentes
 *  - pas de partage d'objets entre instances
 *  - pas de validation croisee entre instances
 * Ces garanties sont a assurer par GameManager / des listeners d'evenements
 * (non encore implementes ici), pas par cette classe elle-meme.
 */
public class BingoInstance {

    private final String instanceId;      // ex: "bingo_<gameId>_1"
    private final BingoTeam team;
    private World world;
    private Location spawnLocation;

    /** Suivi de connexion PAR JOUEUR (une equipe peut avoir des membres connectes/deconnectes independamment). */
    private final Map<UUID, Boolean> playerConnected = new HashMap<>();

    public BingoInstance(String instanceId, BingoTeam team) {
        this.instanceId = instanceId;
        this.team = team;
        for (UUID playerId : team.getPlayers()) {
            playerConnected.put(playerId, false);
        }
    }

    public String getInstanceId() {
        return instanceId;
    }

    public BingoTeam getTeam() {
        return team;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public boolean isPlayerConnected(UUID playerId) {
        return playerConnected.getOrDefault(playerId, false);
    }

    public void setPlayerConnected(UUID playerId, boolean connected) {
        if (!team.contains(playerId)) {
            return;
        }
        playerConnected.put(playerId, connected);
    }

    /**
     * Abandon VOLONTAIRE et PERMANENT (voir AbandonService, menu Objectifs -&gt; "Abandonner la
     * partie", confirmation demandee) - AJOUTE le 24/09/2026, demande explicite de l'utilisateur.
     * Distinct d'une simple deconnexion (playerConnected, qui autorise une reconnexion normale a
     * cette meme instance) : un joueur abandonne reste dans team.getPlayers() (roster informatif,
     * conserve pour ne pas fausser la composition d'equipe affichee) mais n'est plus JAMAIS compte
     * comme actif (voir hasAnyActivePlayer ci-dessous, et GameManager.findGameOf qui l'exclut
     * desormais de toute reconnexion a cette partie, meme s'il se reconnecte au serveur ensuite).
     */
    private final Set<UUID> abandoned = new HashSet<>();

    public void markAbandoned(UUID playerId) {
        if (!team.contains(playerId)) {
            return;
        }
        abandoned.add(playerId);
        playerConnected.put(playerId, false);
    }

    public boolean hasAbandoned(UUID playerId) {
        return abandoned.contains(playerId);
    }

    /** true si TOUS les membres de cette equipe ont abandonne (voir GameEndService.checkTeamAbandonment, 0.1.18). */
    public boolean isFullyAbandoned() {
        return !team.getPlayers().isEmpty() && abandoned.containsAll(team.getPlayers());
    }

    /** true si au moins un membre de l'equipe est actuellement connecte ET n'a PAS abandonne -
     *  remplace l'ancien hasAnyPlayerConnected (qui ignorait l'abandon) pour toute decision de fin
     *  de partie/reconnexion, voir BingoGame.hasAnyConnectedPlayer. */
    public boolean hasAnyActivePlayer() {
        for (UUID playerId : team.getPlayers()) {
            if (isPlayerConnected(playerId) && !abandoned.contains(playerId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAssignedTo(Player player) {
        return player != null && team.contains(player.getUniqueId());
    }

    public boolean isAssignedTo(UUID playerId) {
        return team.contains(playerId);
    }
}
