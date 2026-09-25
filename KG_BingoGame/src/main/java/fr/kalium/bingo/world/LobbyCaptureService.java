package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Logique de capture de la salle d'attente (position du joueur = coin/point d'apparition,
 * "comme pour les arenes sur kal-games" - demande explicite de l'utilisateur), extraite de
 * BingoAdminCommand pour etre partagee entre les commandes texte (/bingoadmin lobby ...,
 * gardees pour compatibilite) et le menu graphique (/menu, voir gui.LobbyMenu - demande de
 * l'utilisateur car "je ne comprends pas l'utilisation de la commande").
 * <p>
 * 0.7.5 : les coins et le point d'apparition ne sont plus gardes en memoire par joueur mais partages (les derniers
 * definis) et enregistres dans lobby_capture.yml (demande de LeKiwi06, 25/09/2026 : « note bien les coordonnées de la
 * dernière recapture pour éviter de les perdre au restart »). /bingoadmin lobby capture suffit donc apres un
 * redemarrage, y compris depuis la console. Les coordonnees sont aussi ecrites dans la console a chaque changement.
 */
public final class LobbyCaptureService {

    private final LobbyTemplateService lobbyTemplateService;
    private final LobbySlots lobbySlots;

    private final File file;
    private final Logger logger;

    private Location pos1;
    private Location pos2;
    private Location spawnMark;

    public LobbyCaptureService(LobbyTemplateService lobbyTemplateService, LobbySlots lobbySlots, File dataFolder,
                               Logger logger) {
        this.lobbyTemplateService = lobbyTemplateService;
        this.lobbySlots = lobbySlots;
        this.file = new File(dataFolder, "lobby_capture.yml");
        this.logger = logger;
        load();
    }

    private void load() {
        World world = lobbySlots.world();
        if (!file.exists() || world == null) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        pos1 = read(yaml, "pos1", world);
        pos2 = read(yaml, "pos2", world);
        spawnMark = read(yaml, "spawn", world);
        logger.info("Salle d'attente : positions de capture rechargées - coin 1 " + text(pos1) + ", coin 2 " + text(pos2)
                + ", apparition " + text(spawnMark));
    }

    private static Location read(YamlConfiguration yaml, String key, World world) {
        if (!yaml.isConfigurationSection(key)) {
            return null;
        }
        return new Location(world, yaml.getDouble(key + ".x"), yaml.getDouble(key + ".y"), yaml.getDouble(key + ".z"),
                (float) yaml.getDouble(key + ".yaw"), (float) yaml.getDouble(key + ".pitch"));
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "Positions de la derniere capture de la salle d'attente (monde " + lobbyWorldName() + ").",
                "Ecrit par KG_BingoGame a chaque /bingoadmin lobby pos1|pos2|spawn (ou via /menu)."));
        write(yaml, "pos1", pos1);
        write(yaml, "pos2", pos2);
        write(yaml, "spawn", spawnMark);
        try {
            yaml.save(file);
        } catch (IOException e) {
            logger.warning("Impossible d'enregistrer " + file.getName() + " : " + e.getMessage());
        }
    }

    private static void write(YamlConfiguration yaml, String key, Location location) {
        if (location == null) {
            return;
        }
        yaml.set(key + ".x", location.getX());
        yaml.set(key + ".y", location.getY());
        yaml.set(key + ".z", location.getZ());
        yaml.set(key + ".yaw", (double) location.getYaw());
        yaml.set(key + ".pitch", (double) location.getPitch());
    }

    private static String text(Location location) {
        if (location == null) {
            return "(non défini)";
        }
        return String.format(Locale.ROOT, "%.2f %.2f %.2f (%.1f/%.1f)", location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public String lobbyWorldName() {
        return lobbySlots.world() != null ? lobbySlots.world().getName() : null;
    }

    public boolean isPos1Set(Player player) {
        return pos1 != null;
    }

    public boolean isPos2Set(Player player) {
        return pos2 != null;
    }

    public boolean isSpawnSet(Player player) {
        return spawnMark != null;
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
        pos1 = player.getLocation();
        save();
        player.sendMessage("§aCoin 1 défini à votre position. §7(" + text(pos1) + ")");
        logger.info("Salle d'attente : coin 1 défini par " + player.getName() + " en " + text(pos1));
        return true;
    }

    public boolean setCorner2(Player player) {
        if (!checkWorld(player)) {
            return false;
        }
        pos2 = player.getLocation();
        save();
        player.sendMessage("§aCoin 2 défini à votre position. §7(" + text(pos2) + ")");
        logger.info("Salle d'attente : coin 2 défini par " + player.getName() + " en " + text(pos2));
        return true;
    }

    public boolean setSpawn(Player player) {
        if (!checkWorld(player)) {
            return false;
        }
        spawnMark = player.getLocation();
        save();
        player.sendMessage("§aPoint d'apparition des joueurs défini à votre position/orientation actuelle. §7(" + text(spawnMark) + ")");
        logger.info("Salle d'attente : point d'apparition défini par " + player.getName() + " en " + text(spawnMark));
        return true;
    }

    /**
     * Lance la capture (0.1.21 : sur plusieurs ticks, voir LobbyTemplateService). Les messages de fin
     * sont envoyes au joueur s'il est toujours connecte.
     * @return false en cas de refus immediat (message deja envoye au joueur).
     */
    public boolean capture(CommandSender player) {
        Location c1 = pos1;
        Location c2 = pos2;
        Location spawn = spawnMark;
        if (c1 == null || c2 == null || spawn == null) {
            player.sendMessage("§cDéfinissez d'abord le coin 1, le coin 2 et le point d'apparition.");
            return false;
        }
        if (lobbyTemplateService.busy()) {
            player.sendMessage("§cUne capture ou une mise à jour des salles d'attente est déjà en cours, patientez.");
            return false;
        }
        CommandSender id = player;
        logger.info("Salle d'attente : capture lancée par " + player.getName() + " - coin 1 " + text(c1) + ", coin 2 "
                + text(c2) + ", apparition " + text(spawn));
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

    private static void tell(CommandSender sender, String message) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message); // console
            return;
        }
        Player online = Bukkit.getPlayer(player.getUniqueId());
        if (online != null) {
            online.sendMessage(message);
        }
    }

    public void sendInfo(CommandSender player) {
        boolean exists = lobbyTemplateService.exists();
        player.sendMessage("§7Salle d'attente : " + (exists ? "§adéfinie" : "§cnon définie")
                + "§7 - emplacements configurés : " + lobbySlots.slotCount());
        player.sendMessage("§7Coin 1 : §f" + text(pos1) + "§7 - coin 2 : §f" + text(pos2) + "§7 - apparition : §f"
                + text(spawnMark));
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
