package fr.kalium.portal;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import fr.kalium.menu.KlmMenu;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * KLM_Portal (demande de LeKiwi06, 26/09/2026) : remplace ConditionalEvents + PyxelRegions (+ VelocityCommandForward)
 * pour les portails du lobby. Un portail = une region WorldGuard + une destination de KLM_Menu. Entrer dans la region
 * envoie le joueur vers la destination, comme un clic sur son bouton ; si le bouton est desactive dans KLM_Menu
 * (menu > Parametres), le portail l'est aussi : message + recul. Aucun etat en double : KLM_Menu est seul maitre de
 * l'activation. Prevu pour tous les serveurs Paper (demande de LeKiwi06).
 */
public final class KlmPortal extends JavaPlugin implements Listener, TabCompleter {

    /** Un portail : region WorldGuard (dans un monde) reliee a une destination de KLM_Menu. */
    private record Portal(String region, String world, String destination) {
    }

    /** Portails par monde, puis par nom de region (en minuscules). */
    private final Map<String, Map<String, Portal>> portals = new HashMap<>();
    /** Portail dans lequel se trouve chaque joueur : on ne declenche qu'a l'entree, pas a chaque pas dedans. */
    private final Map<UUID, String> inside = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, Long> lastWarning = new HashMap<>();

    private KlmMenu menu;
    private Lang lang;
    private Gui gui;
    private Landings landings;
    /** 1.2.0 : effets de zone (voir RegionEffects). */
    private final RegionEffects regionEffects = new RegionEffects(this);
    private double pushStrength;
    private long cooldownMs;

    // ------------------------------------------------------------------ cycle de vie

    @Override
    public void onEnable() {
        saveDefaultConfig();
        menu = (KlmMenu) getServer().getPluginManager().getPlugin("KLM_Menu");
        lang = new Lang(this);
        gui = new Gui(this, lang);
        loadPortals();
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("klmportal");
        if (command != null) {
            command.setTabCompleter(this);
        }
        // Interface "Ajouter un portail" dans le catalogue "Interfaces" de la boussole (rubrique admin).
        getServer().getServicesManager().register(MenuSection.class, MenuSection.of(this, "add-portal",
                MenuSection.Audience.ADMINS,
                lang.c("catalog.title", "<#09add3>Ajouter un portail"),
                lang.c("catalog.description", "<gray>Relier une région WorldGuard à une destination du menu."),
                this::openAddMenu), this, ServicePriority.Normal);
        // 1.1.0 : points de chute (voir Landings).
        landings = new Landings(this, lang, gui);
        getServer().getPluginManager().registerEvents(landings, this);
        menu.setBeforeConnect(landings::beforeConnect);
        getServer().getServicesManager().register(MenuSection.class, MenuSection.of(this, "landings",
                MenuSection.Audience.ADMINS,
                lang.c("catalog.landings-title", "<#09add3>Points de chute"),
                lang.c("catalog.landings-description", "<gray>Où arrivent les joueurs envoyés sur un autre serveur, "
                        + "et ceux qui arrivent ici."),
                landings::openMenu), this, ServicePriority.Normal);
        // 1.2.0 : effets de zone, verifies toutes les secondes.
        getServer().getScheduler().runTaskTimer(this, regionEffects::tick, 20L, 20L);
        // Verification des regions une fois tous les mondes et WorldGuard prets.
        getServer().getScheduler().runTask(this, this::checkRegions);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        if (menu != null && menu.isEnabled()) {
            menu.setBeforeConnect(null);
        }
    }

    /** Serveurs vers lesquels KLM_Menu envoie depuis ce serveur (pour les points de chute). */
    List<String> destinationServers() {
        return menu.serverIds();
    }

    private void loadPortals() {
        reloadConfig();
        // Valeurs par defaut dans le code : un config.yml deja present ne recoit jamais les nouvelles cles.
        pushStrength = Math.max(0.1, Math.min(3.0, getConfig().getDouble("push-strength", 0.8)));
        cooldownMs = Math.max(0, getConfig().getLong("cooldown-seconds", 3)) * 1000L;
        portals.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("portals");
        if (section != null) {
            for (String region : section.getKeys(false)) {
                String world = section.getString(region + ".world", "");
                String destination = section.getString(region + ".destination", "");
                if (world.isBlank() || destination.isBlank()) {
                    getLogger().warning("Portail " + region + " ignoré : \"world\" ou \"destination\" manquant.");
                    continue;
                }
                portals.computeIfAbsent(world, w -> new LinkedHashMap<>())
                        .put(region.toLowerCase(Locale.ROOT), new Portal(region, world, destination));
            }
        }
        int count = portals.values().stream().mapToInt(Map::size).sum();
        getLogger().info(count + " portail(s) chargé(s).");
        regionEffects.load();
    }

    private void checkRegions() {
        for (Map<String, Portal> byRegion : portals.values()) {
            for (Portal portal : byRegion.values()) {
                if (region(portal) == null) {
                    getLogger().warning("Portail " + portal.region() + " : région WorldGuard introuvable dans le monde "
                            + portal.world() + " (portail inactif).");
                }
            }
        }
    }

    private RegionManager regions(World world) {
        return world == null ? null
                : WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    private ProtectedRegion region(Portal portal) {
        RegionManager manager = regions(getServer().getWorld(portal.world()));
        return manager == null ? null : manager.getRegion(portal.region());
    }

    /** Portail dont la region contient ce bloc (null si aucun). */
    private Portal portalAt(Location location) {
        Map<String, Portal> byRegion = portals.get(location.getWorld().getName());
        if (byRegion == null || byRegion.isEmpty()) {
            return null;
        }
        RegionManager manager = regions(location.getWorld());
        if (manager == null) {
            return null;
        }
        BlockVector3 block = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        for (Portal portal : byRegion.values()) {
            ProtectedRegion region = manager.getRegion(portal.region());
            if (region != null && region.contains(block)) {
                return portal;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ portails

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ() && from.getWorld() == to.getWorld()) {
            return;
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Portal portal = portalAt(to);
        if (portal == null) {
            inside.remove(id);
            return;
        }
        if (portal.region().equalsIgnoreCase(inside.get(id))) {
            return;
        }

        if (!menu.isDestinationEnabled(portal.destination())) {
            // Portail desactive : on empeche l'entree et on repousse le joueur.
            event.setCancelled(true);
            pushBack(player, from, portal);
            long now = System.currentTimeMillis();
            Long last = lastWarning.get(id);
            if (last == null || now - last > 3000) {
                lastWarning.put(id, now);
                player.sendActionBar(lang.c("disabled", "<red>Ce portail est désactivé."));
                lang.saveIfNeeded();
            }
            return;
        }

        inside.put(id, portal.region());
        long now = System.currentTimeMillis();
        Long last = lastUse.get(id);
        if (last != null && now - last < cooldownMs) {
            return;
        }
        lastUse.put(id, now);
        menu.sendToDestination(player, portal.destination());
    }

    /** Recul horizontal, du centre de la region vers le joueur (a defaut, a l'oppose de son regard). */
    private void pushBack(Player player, Location from, Portal portal) {
        Vector direction = null;
        ProtectedRegion region = region(portal);
        if (region != null) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            double centerX = (min.x() + max.x() + 1) / 2.0;
            double centerZ = (min.z() + max.z() + 1) / 2.0;
            direction = new Vector(from.getX() - centerX, 0, from.getZ() - centerZ);
        }
        if (direction == null || direction.lengthSquared() < 1.0E-4) {
            direction = player.getLocation().getDirection().setY(0).multiply(-1);
        }
        if (direction.lengthSquared() < 1.0E-4) {
            return;
        }
        Vector velocity = direction.normalize().multiply(pushStrength).setY(0.3);
        // Au tick suivant : le retour a "from" (mouvement annule) remettrait sinon la vitesse a zero.
        getServer().getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                player.setVelocity(velocity);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        inside.remove(id);
        lastUse.remove(id);
        lastWarning.remove(id);
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("klmportal.admin")) {
            sender.sendMessage(lang.c("no-permission", "<red>Cette commande est réservée aux opérateurs."));
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set" -> set(sender, label, args);
            case "remove" -> remove(sender, label, args);
            case "list" -> list(sender);
            case "chute" -> landings.commandLanding(sender, label, args);
            case "arrivee" -> landings.commandArrival(sender, label, args);
            case "reload" -> {
                loadPortals();
                checkRegions();
                inside.clear();
                sender.sendMessage(lang.c("reloaded", "<green>Portails rechargés."));
            }
            default -> sender.sendMessage(lang.c("usage",
                    "<gray>Usage : /<label> set <région> <destination> | remove <région> | list | reload | "
                            + "chute <destination> aucun|derniere|<x> <y> <z> [yaw pitch] [monde] | arrivee ici|derniere|info",
                    "label", label));
        }
        lang.saveIfNeeded();
        return true;
    }

    private void set(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.c("players-only", "<red>À faire en jeu, dans le monde de la région."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(lang.c("usage-set", "<gray>Usage : /<label> set <région> <destination>", "label", label));
            return;
        }
        createPortal(player, args[1], args[2]);
    }

    /** Cree (ou modifie) le portail de cette region, dans le monde du joueur ; vrai si reussi. */
    private boolean createPortal(Player player, String regionName, String destination) {
        World world = player.getWorld();
        RegionManager manager = regions(world);
        ProtectedRegion region = manager == null ? null : manager.getRegion(regionName);
        if (region == null) {
            player.sendMessage(lang.c("region-not-found",
                    "<red>Aucune région WorldGuard <white><region></white> dans ce monde.", "region", regionName));
            return false;
        }
        // On garde le nom de la region tel que WorldGuard l enregistre (minuscules).
        String name = region.getId();
        getConfig().set("portals." + name + ".world", world.getName());
        getConfig().set("portals." + name + ".destination", destination);
        saveConfig();
        loadPortals();
        getLogger().info("Portail " + name + " (" + world.getName() + ") relié à " + destination + " par "
                + player.getName() + ".");
        player.sendMessage(lang.c("set", "<green>Portail <white><region></white> relié à <white><destination></white>.",
                "region", name, "destination", destination));
        if (menu.destinationIds().stream().noneMatch(d -> d.equalsIgnoreCase(destination))) {
            player.sendMessage(lang.c("set-unknown",
                    "<yellow>Attention : <white><destination></white> n'est pas un bouton de KLM_Menu sur ce serveur "
                            + "(le portail enverra sur le serveur de ce nom, sans pouvoir être désactivé).",
                    "destination", destination));
        }
        return true;
    }

    // ------------------------------------------------------------------ interface "Ajouter un portail"

    /**
     * Demande de LeKiwi06 (26/09/2026) : "ajoute une interface 'ajouter un portail' en tant qu'admin a la boussole".
     * Deux listes : les regions WorldGuard du monde du joueur qui ne sont pas encore des portails, et les destinations
     * de KLM_Menu sur ce serveur. La region se dessine avant, avec WorldEdit et /rg define.
     */
    private void openAddMenu(Player player, Consumer<Player> back) {
        if (!player.hasPermission("klmportal.admin")) {
            return;
        }
        Component title = lang.c("add.title", "<#09add3><bold>Ajouter un portail");
        ActionButton backButton = gui.button(lang.c("add.back", "<gray>Retour"), null, back::accept);

        List<String> freeRegions = new ArrayList<>();
        RegionManager manager = regions(player.getWorld());
        if (manager != null) {
            Map<String, Portal> existing = portals.getOrDefault(player.getWorld().getName(), Map.of());
            for (String id : manager.getRegions().keySet()) {
                if (!id.equals("__global__") && !existing.containsKey(id.toLowerCase(Locale.ROOT))) {
                    freeRegions.add(id);
                }
            }
            freeRegions.sort(String::compareTo);
        }
        List<String> destinations = menu.destinationIds();

        if (freeRegions.isEmpty() || destinations.isEmpty()) {
            Component reason = freeRegions.isEmpty()
                    ? lang.c("add.no-region", "<gray>Aucune région WorldGuard libre dans ce monde. Dessine la zone "
                            + "avec la baguette de WorldEdit, puis <white>/rg define <nom></white>, et reviens ici.")
                    : lang.c("add.no-destination", "<gray>Aucune destination dans KLM_Menu sur ce serveur.");
            gui.open(player, title, List.of(reason), List.of(), List.of(backButton), null, 1);
            lang.saveIfNeeded();
            return;
        }

        List<Component> regionLabels = new ArrayList<>();
        freeRegions.forEach(id -> regionLabels.add(Component.text(id)));
        List<Component> destinationLabels = new ArrayList<>();
        destinations.forEach(id -> destinationLabels.add(Component.text(id)));

        Component body = lang.c("add.body", "<gray>Monde : <white><world></white>. Un joueur qui entre dans la région "
                + "est envoyé vers la destination ; si son bouton est désactivé dans le menu, le portail l'est aussi.",
                "world", player.getWorld().getName());
        ActionButton create = gui.form(lang.c("add.create", "<green>Créer le portail"), null, (clicker, view) -> {
            if (!clicker.hasPermission("klmportal.admin")) {
                return;
            }
            String region = view.getText("region");
            String destination = view.getText("destination");
            if (region != null && destination != null && createPortal(clicker, region, destination)) {
                openAddMenu(clicker, back);
            }
        });
        gui.open(player, title, List.of(body),
                List.of(gui.choice("region", lang.c("add.region", "Région WorldGuard"), freeRegions, regionLabels, null),
                        gui.choice("destination", lang.c("add.destination", "Destination"), destinations,
                                destinationLabels, null)),
                List.of(create, backButton), null, 1);
        lang.saveIfNeeded();
    }

    private void remove(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(lang.c("usage-remove", "<gray>Usage : /<label> remove <région>", "label", label));
            return;
        }
        String key = null;
        ConfigurationSection section = getConfig().getConfigurationSection("portals");
        if (section != null) {
            for (String region : section.getKeys(false)) {
                if (region.equalsIgnoreCase(args[1])) {
                    key = region;
                }
            }
        }
        if (key == null) {
            sender.sendMessage(lang.c("not-a-portal", "<red><white><region></white> n'est pas un portail.", "region", args[1]));
            return;
        }
        getConfig().set("portals." + key, null);
        saveConfig();
        loadPortals();
        sender.sendMessage(lang.c("removed",
                "<green>Portail <white><region></white> retiré (la région WorldGuard est conservée).", "region", key));
    }

    private void list(CommandSender sender) {
        List<Portal> all = new ArrayList<>();
        portals.values().forEach(byRegion -> all.addAll(byRegion.values()));
        if (all.isEmpty()) {
            sender.sendMessage(lang.c("list-empty", "<gray>Aucun portail sur ce serveur."));
            return;
        }
        sender.sendMessage(lang.c("list-header", "<#09add3><bold>Portails de ce serveur :"));
        for (Portal portal : all) {
            boolean on = menu.isDestinationEnabled(portal.destination());
            sender.sendMessage(lang.c("list-line",
                    "<gray>- <white><region></white> (<world>) → <white><destination></white> : <state>",
                    "region", portal.region(), "world", portal.world(), "destination", portal.destination(),
                    "state", on ? lang.c("state-on", "<green>activé") : lang.c("state-off", "<red>désactivé")));
            if (region(portal) == null) {
                sender.sendMessage(lang.c("list-missing", "<red>  région WorldGuard introuvable."));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("set", "remove", "list", "reload", "chute", "arrivee"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chute")) {
            options.addAll(destinationServers());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("chute")) {
            options.addAll(List.of("aucun", "derniere"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("arrivee")) {
            options.addAll(List.of("ici", "derniere", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender instanceof Player player) {
            RegionManager manager = regions(player.getWorld());
            if (manager != null) {
                options.addAll(manager.getRegions().keySet());
                options.remove("__global__");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            portals.values().forEach(byRegion -> byRegion.values().forEach(p -> options.add(p.region())));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            options.addAll(menu.destinationIds());
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }
}
