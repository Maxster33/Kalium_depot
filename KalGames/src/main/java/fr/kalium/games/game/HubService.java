package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.Locale;

/** Hub de Kal-Games : point d'arrivee, etat des joueurs et objets verrouilles. */
public final class HubService {

    private final KalGames plugin;

    public HubService(KalGames plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ position

    public World hubWorld() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("hub.world", "Kal-Games"));
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        return world;
    }

    public Location hubLocation() {
        World world = hubWorld();
        if (plugin.getConfig().getBoolean("hub.use-world-spawn", true)) {
            Location spawn = world.getSpawnLocation();
            return new Location(world, spawn.getBlockX() + 0.5, spawn.getBlockY(), spawn.getBlockZ() + 0.5, spawn.getYaw(), spawn.getPitch());
        }
        return new Location(world,
                plugin.getConfig().getDouble("hub.x"), plugin.getConfig().getDouble("hub.y"), plugin.getConfig().getDouble("hub.z"),
                (float) plugin.getConfig().getDouble("hub.yaw"), (float) plugin.getConfig().getDouble("hub.pitch"));
    }

    public void setHub(Location location) {
        plugin.getConfig().set("hub.world", location.getWorld().getName());
        plugin.getConfig().set("hub.use-world-spawn", false);
        plugin.getConfig().set("hub.x", round(location.getX()));
        plugin.getConfig().set("hub.y", round(location.getY()));
        plugin.getConfig().set("hub.z", round(location.getZ()));
        plugin.getConfig().set("hub.yaw", round(location.getYaw()));
        plugin.getConfig().set("hub.pitch", round(location.getPitch()));
        plugin.saveConfig();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public boolean isHubWorld(World world) {
        return world != null && world.equals(hubWorld());
    }

    private GameMode hubMode() {
        try {
            return GameMode.valueOf(plugin.getConfig().getString("hub.gamemode", "ADVENTURE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GameMode.ADVENTURE;
        }
    }

    // ------------------------------------------------------------------ etat du joueur

    /** Remet le joueur a neuf (inventaire, effets, vie, mode de jeu). */
    public void resetPlayer(Player player, GameMode mode) {
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGameMode(mode);
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFallDistance(0f);
        player.setExp(0f);
        player.setLevel(0);
        player.setInvulnerable(false);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setVelocity(new Vector());
    }

    /** Objets du hub : menu des mini-jeux + boussole de KaliumMenu. */
    public void giveHubItems(Player player) {
        player.getInventory().setItem(plugin.items().slot("items.games.slot", 4), plugin.items().gamesItem());
        giveCompass(player);
    }

    /** Objets d'un joueur dans les gradins / en course : menu de la partie + boussole. */
    public void giveInGameItems(Player player, boolean checkpointItem) {
        player.getInventory().setItem(plugin.items().slot("items.game-menu.slot", 4), plugin.items().gameMenuItem());
        if (checkpointItem) {
            player.getInventory().setItem(plugin.items().slot("items.checkpoint.slot", 0), plugin.items().checkpointItem());
        }
        giveCompass(player);
    }

    /** La boussole est celle de KaliumMenu (commande /kmenu give, qui verrouille l'objet). */
    private void giveCompass(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("KaliumMenu")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kaliummenu give " + player.getName());
        }
    }

    /** Etat des joueurs dans les gradins d'une partie. */
    public void applyStandsState(Player player) {
        resetPlayer(player, GameMode.ADVENTURE);
        giveInGameItems(player, false);
    }

    /**
     * Renvoie le joueur au hub de Kal-Games (quitte sa partie au passage, ou arrete de regarder s'il etait en mode
     * spectateur : la barre d'objets n'etant pas utilisable en mode spectateur, c'est la commande /hub (ou
     * /kalgames quitter) qui sert de sortie).
     */
    public void sendToHub(Player player) {
        if (plugin.instances().spectatorOf(player) != null) {
            plugin.instances().leaveSpectator(player, false);
            return;
        }
        plugin.instances().leave(player, false);
        resetPlayer(player, hubMode());
        Location hub = hubLocation();
        player.teleport(hub);
        giveHubItems(player);
    }

    /** Etat d'un spectateur qui regarde une partie sans y participer : mode spectateur, vol actif, objets de partie. */
    public void applySpectatorState(Player player) {
        resetPlayer(player, GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        giveInGameItems(player, false);
        // La barre d'objets n'est pas accessible en mode spectateur (HUD vanilla) : on rappelle la commande de sortie.
        plugin.tell(player, "spectate.how-to-leave",
                "<gray>Mode spectateur : tapez <white>/hub</white> à tout moment pour arrêter de regarder.");
    }

    /** Renvoie un spectateur au hub. Le spectateur n'est pas membre de la partie : pas besoin d'instances().leave. */
    public void sendSpectatorToHub(Player player) {
        resetPlayer(player, hubMode());
        player.teleport(hubLocation());
        giveHubItems(player);
    }
}
