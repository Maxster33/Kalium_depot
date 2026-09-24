package fr.kalium.bingo.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Point de spawn d'un joueur pendant une partie + remise a zero en fin de partie / abandon -
 * AJOUTE en 0.1.18, demande explicite de l'utilisateur : "l'endroit ou les joueurs apparaissent sur
 * la map doit devenir leur point de spawn (actuellement si on meurt on est renvoyé sur le modèle de
 * la salle d'attente). à la fin d'une partie ou si un joueur abandonne il faut clear son inventaire
 * et supprimer son point de spawn".
 *
 * Point de spawn = setRespawnLocation(..., force=true) (meme mecanisme qu'un lit, sans lit) : le
 * serveur le sauvegarde avec les donnees du joueur, il survit donc a une deconnexion ou a un
 * redemarrage pendant la partie.
 *
 * Joueur HORS LIGNE au moment de la fin de partie : impossible de toucher a son inventaire tout de
 * suite. Il est note dans pending-resets.yml et remis a zero a sa prochaine connexion a ce serveur
 * (voir PlayerConnectListener) - sinon il retrouverait son ancien inventaire (et un point de spawn
 * pointant vers un monde supprime) a sa partie suivante. Fichier plutot que memoire : doit survivre
 * a un redemarrage du serveur entre-temps.
 */
public final class PlayerResetService {

    private final Logger logger;
    private final File pendingFile;
    private final Set<UUID> pending = new HashSet<>();

    public PlayerResetService(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        this.pendingFile = new File(plugin.getDataFolder(), "pending-resets.yml");
        load();
    }

    /** Fait du point d'apparition sur la map d'instance le point de spawn du joueur. */
    public void setGameSpawn(Player player, Location spawn) {
        if (spawn != null) {
            player.setRespawnLocation(spawn, true);
        }
    }

    /** Vide ENTIEREMENT l'inventaire (contenu, armure, main secondaire) et supprime le point de spawn. */
    public void reset(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[]{null, null, null, null});
        inventory.setItemInOffHand(null);
        player.setItemOnCursor(null);
        player.setRespawnLocation(null);
    }

    /** Remise a zero immediate si le joueur est en ligne, sinon differee a sa prochaine connexion. */
    public void resetOrDefer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            reset(player);
            return;
        }
        if (pending.add(playerId)) {
            save();
        }
    }

    /** A appeler a la connexion (voir PlayerConnectListener), AVANT de redonner le moindre objet. */
    public void applyPending(Player player) {
        if (pending.remove(player.getUniqueId())) {
            reset(player);
            save();
        }
    }

    private void load() {
        if (!pendingFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(pendingFile);
        for (String raw : yaml.getStringList("players")) {
            try {
                pending.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // ligne illisible : ignoree
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> raw = new ArrayList<>();
        for (UUID id : pending) {
            raw.add(id.toString());
        }
        yaml.set("players", raw);
        try {
            File dir = pendingFile.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            yaml.save(pendingFile);
        } catch (IOException e) {
            logger.warning("[KG_BingoGame] Impossible d'enregistrer pending-resets.yml : " + e.getMessage());
        }
    }
}
