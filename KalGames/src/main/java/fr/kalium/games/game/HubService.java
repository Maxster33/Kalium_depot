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

    /**
     * 1.27.0 - Kal-Games fusionne dans Kanvas (LeKiwi06, 21/10/2026) : le hub et les protections de KalGames ne
     * concernent plus que ses mondes (hub et monde des parties), plus tout le serveur.
     */
    public boolean isKalGamesWorld(World world) {
        return world != null && (isHubWorld(world) || world == plugin.worlds().world());
    }

    /**
     * 1.27.0 - mondes d'ou un joueur qui se connecte est ramene au hub, en plus du hub et du monde des parties
     * (« hub.teleport-from-worlds », defaut : le monde du Build Battle, qui fait partie de l'espace Kal-Games).
     */
    public boolean bringsToHubOnJoin(World world) {
        if (world == null) {
            return false;
        }
        if (isKalGamesWorld(world)) {
            return true;
        }
        java.util.List<String> extra = plugin.getConfig().isList("hub.teleport-from-worlds")
                ? plugin.getConfig().getStringList("hub.teleport-from-worlds") : java.util.List.of("buildbattle");
        for (String name : extra) {
            if (name.equalsIgnoreCase(world.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1.27.0 - joueur d'un autre espace du serveur (KLM_Espaces, metadonnee « klm_espace ») : /hub, /kalgames et les
     * menus de KalGames lui sont refuses (espaces etanches, on passe par le lobby). Sans KLM_Espaces : jamais.
     */
    public boolean otherSpace(Player player) {
        if (!player.hasMetadata("klm_espace")) {
            return false;
        }
        String space = player.getMetadata("klm_espace").get(0).asString();
        return !space.equalsIgnoreCase(plugin.getConfig().getString("hub.space", "kal-games"));
    }

    /**
     * 1.27.0 - le monde du hub n'est plus forcement le monde principal du serveur (Kal-Games dans Kanvas) : charge
     * au demarrage (comme KV_Plots ; sous Paper 26.3 les mondes sont des dimensions du monde principal, pas des
     * dossiers a la racine du serveur, d'ou l'absence de test sur le disque).
     */
    public void loadHubWorld() {
        String name = plugin.getConfig().getString("hub.world", "Kal-Games");
        if (Bukkit.getWorld(name) != null) {
            return;
        }
        World world = new org.bukkit.WorldCreator(name).createWorld();
        plugin.getLogger().info("Monde du hub '" + name + "' charge" + (world == null ? " : ECHEC." : "."));
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

    /** Objets du hub : objet de KG_Menu (menu des mini-jeux) + boussole de KLM_Menu (1.16.0 : geres par ces plugins). */
    public void giveHubItems(Player player) {
        plugin.kgMenu().giveHubItem(player);
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

    /**
     * La boussole est geree par KLM_Menu (1.16.0 - demande de LeKiwi06 : "la boussole fait partie de l'interface [...]
     * geree sur tous les serveurs par le meme plugin") : KalGames lui demande seulement de la remettre apres avoir vide
     * l'inventaire.
     */
    private void giveCompass(Player player) {
        plugin.klm().giveNavigation(player);
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
