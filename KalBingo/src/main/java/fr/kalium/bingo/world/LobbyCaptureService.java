package fr.kalium.bingo.world;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logique de capture de la salle d'attente (position du joueur = coin/point d'apparition,
 * "comme pour les arenes sur kal-games" - demande explicite de l'utilisateur), extraite de
 * BingoAdminCommand pour etre partagee entre les commandes texte (/bingoadmin lobby ...,
 * gardees pour compatibilite) et le menu graphique (/menu, voir gui.LobbyMenu - demande de
 * l'utilisateur car "je ne comprends pas l'utilisation de la commande").
 */
public final class LobbyCaptureService {

    private final LobbyTemplateService lobbyTemplateService;
    private final LobbySlots lobbySlots;

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<UUID, Location> spawnMark = new HashMap<>();

    public LobbyCaptureService(LobbyTemplateService lobbyTemplateService, LobbySlots lobbySlots) {
        this.lobbyTemplateService = lobbyTemplateService;
        this.lobbySlots = lobbySlots;
    }

    public String lobbyWorldName() {
        return lobbySlots.world() != null ? lobbySlots.world().getName() : null;
    }

    public boolean isPos1Set(Player player) {
        return pos1.containsKey(player.getUniqueId());
    }

    public boolean isPos2Set(Player player) {
        return pos2.containsKey(player.getUniqueId());
    }

    public boolean isSpawnSet(Player player) {
        return spawnMark.containsKey(player.getUniqueId());
    }

    public boolean isTemplateDefined() {
        return lobbyTemplateService.exists();
    }

    public int slotCount() {
        return lobbySlots.slotCount();
    }

    /** Teleporte le joueur dans le monde de la salle d'attente (necessaire pour tout le reste). */
    public boolean teleportToLobby(Player player) {
        if (lobbySlots.world() == null) {
            player.sendMessage("§cLe monde de la salle d'attente n'est pas disponible (le plugin a-t-il bien démarré ?).");
            return false;
        }
        player.teleport(new Location(lobbySlots.world(), 0.5, 80, 0.5));
        player.sendMessage("§aTéléporté dans '" + lobbyWorldName() + "'. Construisez votre salle ici, puis définissez les coins et le point d'apparition.");
        return true;
    }

    public boolean setCorner1(Player player) {
        if (!checkWorld(player)) {
            return false;
        }
        pos1.put(player.getUniqueId(), player.getLocation());
        player.sendMessage("§aCoin 1 défini à votre position.");
        return true;
    }

    public boolean setCorner2(Player player) {
        if (!checkWorld(player)) {
            return false;
        }
        pos2.put(player.getUniqueId(), player.getLocation());
        player.sendMessage("§aCoin 2 défini à votre position.");
        return true;
    }

    public boolean setSpawn(Player player) {
        if (!checkWorld(player)) {
            return false;
        }
        spawnMark.put(player.getUniqueId(), player.getLocation());
        player.sendMessage("§aPoint d'apparition des joueurs défini à votre position/orientation actuelle.");
        return true;
    }

    /**
     * Lance la capture (0.1.21 : sur plusieurs ticks, voir LobbyTemplateService). Les messages de fin
     * sont envoyes au joueur s'il est toujours connecte.
     * @return false en cas de refus immediat (message deja envoye au joueur).
     */
    public boolean capture(Player player) {
        Location c1 = pos1.get(player.getUniqueId());
        Location c2 = pos2.get(player.getUniqueId());
        Location spawn = spawnMark.get(player.getUniqueId());
        if (c1 == null || c2 == null || spawn == null) {
            player.sendMessage("§cDéfinissez d'abord le coin 1, le coin 2 et le point d'apparition.");
            return false;
        }
        if (lobbyTemplateService.busy()) {
            player.sendMessage("§cUne capture ou une mise à jour des salles d'attente est déjà en cours, patientez.");
            return false;
        }
        UUID id = player.getUniqueId();
        try {
            LobbyTemplate previous = lobbyTemplateService.get();
            lobbyTemplateService.capture(c1, c2, spawn, template -> {
                tell(id, "§aSalle d'attente capturée (" + template.sizeX() + "x" + template.sizeY() + "x" + template.sizeZ()
                        + "). " + (previous != null ? "Suppression des anciennes salles puis c" : "C")
                        + "ollage sur les " + lobbySlots.slotCount() + " emplacements en cours...");
                // 0.1.20 : les salles deja collees avec l'ancien modele sont supprimees AVANT de coller le
                // nouveau (sinon les restes de l'ancien restaient en place - voir LobbySlots.clearAllSlots).
                lobbySlots.clearAllSlots(previous);
                lobbySlots.pasteAllSlots(() -> tell(id, "§aSalles d'attente republiées sur tous les emplacements ("
                        + lobbySlots.slotCount() + ")."));
            }, error -> tell(id, "§c" + error));
            player.sendMessage("§eCapture en cours (" + lobbyTemplateService.maxSize()
                    + " blocs max par côté) : elle est faite petit à petit pour ne pas bloquer le serveur, un message confirmera la fin.");
            return true;
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c" + e.getMessage());
            return false;
        }
    }

    private static void tell(UUID id, String message) {
        Player player = org.bukkit.Bukkit.getPlayer(id);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    public void sendInfo(Player player) {
        boolean exists = lobbyTemplateService.exists();
        player.sendMessage("§7Salle d'attente : " + (exists ? "§adéfinie" : "§cnon définie")
                + "§7 - emplacements configurés : " + lobbySlots.slotCount());
    }

    private boolean checkWorld(Player player) {
        String worldName = lobbyWorldName();
        if (worldName == null) {
            player.sendMessage("§cLe monde de la salle d'attente n'est pas disponible.");
            return false;
        }
        if (!player.getWorld().getName().equals(worldName)) {
            player.sendMessage("§cVous devez être dans le monde '" + worldName + "' (voir /menu > Aller dans la salle d'attente).");
            return false;
        }
        return true;
    }
}
