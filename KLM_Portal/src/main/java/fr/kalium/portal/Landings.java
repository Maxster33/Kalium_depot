package fr.kalium.portal;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Points de chute (KLM_Portal 1.1.0 - demande de LeKiwi06, 26/09/2026) : ConditionalEvents remettait tout le monde au
 * centre du lobby a la connexion ; "il faudrait integrer ca au plugin en mettant un point de chute pour chaque
 * teleportation interserveur avec : coordonnees ou derniere position dans le serveur de destination", regle cote
 * DEPART (choix de LeKiwi06).
 *
 * - Serveur de depart : avant chaque changement de serveur de KLM_Menu (boussole, /lobby, portails), la consigne de la
 *   destination ("landings" dans config.yml) est deposee dans KaliumRelay (entree "landing-<uuid>" : jamais melangee
 *   aux affectations du Bingo, qui utilisent l'UUID seul).
 * - Serveur d'arrivee : a la connexion, la consigne est lue (et effacee) ; sans consigne (connexion directe, depart
 *   sans KLM_Portal, destination sans point de chute), on applique "default-arrival" de CE serveur.
 */
final class Landings implements Listener {

    static final String LAST = "derniere-position";
    static final String POINT = "coordonnees";
    static final String NONE = "aucun";

    /** Point de chute ; monde vide = monde principal du serveur d'arrivee. */
    record Point(String world, double x, double y, double z, float yaw, float pitch) {
        static Point of(Location location) {
            return new Point(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch());
        }
    }

    private final KlmPortal plugin;
    private final Lang lang;
    private final Gui gui;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private boolean warnedNoRelay;

    Landings(KlmPortal plugin, Lang lang, Gui gui) {
        this.plugin = plugin;
        this.lang = lang;
        this.gui = gui;
    }

    // ------------------------------------------------------------------ relais

    private String relayUrl() {
        String url = plugin.getConfig().getString("relay-url", "");
        return url == null ? "" : url.trim().replaceAll("/+$", "");
    }

    private String relayToken() {
        return plugin.getConfig().getString("relay-token", "");
    }

    private boolean relayReady() {
        return !relayUrl().isBlank() && !relayToken().isBlank();
    }

    private URI relayUri(Player player) {
        return URI.create(relayUrl() + "/assignment/landing-" + player.getUniqueId());
    }

    /** Appele par KLM_Menu avant d'envoyer le joueur sur "server" : depose la consigne de cette destination. */
    CompletableFuture<?> beforeConnect(Player player, String server) {
        String payload = payloadFor(server);
        if (payload == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (!relayReady()) {
            if (!warnedNoRelay) {
                warnedNoRelay = true;
                plugin.getLogger().warning("Point de chute de " + server + " non transmis : relay-url ou relay-token "
                        + "vide dans config.yml.");
            }
            return CompletableFuture.completedFuture(null);
        }
        HttpRequest request = HttpRequest.newBuilder(relayUri(player))
                .header("X-Kalium-Relay-Token", relayToken())
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(error -> {
                    plugin.getLogger().warning("Relais injoignable (point de chute de " + server + ") : " + error);
                    return null;
                });
    }

    /**
     * Consigne a transmettre pour ce serveur, ou null (aucun point de chute : le serveur d'arrivee decide).
     * Format : "<serveur>|derniere-position" ou "<serveur>|coordonnees|<monde>|x|y|z|yaw|pitch".
     */
    private String payloadFor(String server) {
        ConfigurationSection landing = landingSection(server);
        if (landing == null) {
            return null;
        }
        String mode = landing.getString("mode", NONE);
        if (LAST.equals(mode)) {
            return server + "|" + LAST;
        }
        if (POINT.equals(mode)) {
            return server + "|" + POINT + "|" + landing.getString("world", "") + "|" + landing.getDouble("x")
                    + "|" + landing.getDouble("y") + "|" + landing.getDouble("z") + "|" + landing.getDouble("yaw")
                    + "|" + landing.getDouble("pitch");
        }
        return null;
    }

    /** Section "landings.<serveur>" (nom insensible a la casse), ou null. */
    private ConfigurationSection landingSection(String server) {
        ConfigurationSection all = plugin.getConfig().getConfigurationSection("landings");
        if (all == null) {
            return null;
        }
        for (String key : all.getKeys(false)) {
            if (key.equalsIgnoreCase(server)) {
                return all.getConfigurationSection(key);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ arrivee

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!relayReady()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> arrive(player, null));
            return;
        }
        HttpRequest request = HttpRequest.newBuilder(relayUri(player))
                .header("X-Kalium-Relay-Token", relayToken())
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, error) -> response != null && response.statusCode() == 200 ? response.body() : null)
                .thenAccept(payload -> plugin.getServer().getScheduler().runTask(plugin, () -> arrive(player, payload)));
    }

    private void arrive(Player player, String payload) {
        if (!player.isOnline()) {
            return;
        }
        String[] parts = payload == null ? new String[0] : payload.split("\\|", -1);
        String self = plugin.getConfig().getString("server-name", "");
        // Consigne pour un autre serveur (ex. envoi rate puis connexion ici dans les 2 minutes) : ignoree.
        boolean forHere = parts.length >= 2 && (self == null || self.isBlank() || parts[0].equalsIgnoreCase(self));
        if (forHere && LAST.equals(parts[1])) {
            return;
        }
        if (forHere && POINT.equals(parts[1]) && parts.length >= 8) {
            try {
                teleport(player, parts[2], Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                        Double.parseDouble(parts[5]), Float.parseFloat(parts[6]), Float.parseFloat(parts[7]));
                return;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Point de chute illisible pour " + player.getName() + " : " + payload);
            }
        }
        ConfigurationSection def = plugin.getConfig().getConfigurationSection("default-arrival");
        if (def != null && POINT.equals(def.getString("mode", LAST))) {
            teleport(player, def.getString("world", ""), def.getDouble("x"), def.getDouble("y"), def.getDouble("z"),
                    (float) def.getDouble("yaw"), (float) def.getDouble("pitch"));
        }
    }

    private void teleport(Player player, String worldName, double x, double y, double z, float yaw, float pitch) {
        World world = worldName == null || worldName.isBlank() ? null : plugin.getServer().getWorld(worldName);
        if (world == null) {
            if (worldName != null && !worldName.isBlank()) {
                plugin.getLogger().warning("Point de chute : monde " + worldName + " introuvable, monde principal utilisé.");
            }
            world = plugin.getServer().getWorlds().get(0);
        }
        player.teleport(new Location(world, x, y, z, yaw, pitch), PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    // ------------------------------------------------------------------ commandes

    /** /klmportal chute <destination> aucun | derniere | <x> <y> <z> [yaw pitch] [monde] */
    void commandLanding(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(lang.c("usage-landing", "<gray>Usage : /<label> chute <destination> aucun | derniere | "
                    + "<x> <y> <z> [yaw pitch] [monde]", "label", label));
            return;
        }
        String server = args[1];
        String how = args[2].toLowerCase(Locale.ROOT);
        if (how.equals("aucun")) {
            setLanding(sender, server, NONE, null);
        } else if (how.startsWith("dern")) {
            setLanding(sender, server, LAST, null);
        } else {
            Point point = parsePoint(sender, args, 2);
            if (point != null) {
                setLanding(sender, server, POINT, point);
            }
        }
    }

    /** /klmportal arrivee ici | derniere | info */
    void commandArrival(CommandSender sender, String label, String[] args) {
        String how = args.length < 2 ? "info" : args[1].toLowerCase(Locale.ROOT);
        if (how.equals("ici")) {
            if (sender instanceof Player player) {
                setArrival(sender, Point.of(player.getLocation()));
            } else {
                sender.sendMessage(lang.c("players-only", "<red>À faire en jeu, dans le monde de la région."));
            }
        } else if (how.startsWith("dern")) {
            setArrival(sender, null);
        } else {
            sender.sendMessage(lang.c("arrival-info", "<gray>Arrivée sur ce serveur (sans consigne) : <state>",
                    "state", describe(plugin.getConfig().getConfigurationSection("default-arrival"))));
        }
    }

    /** Lit "<x> <y> <z> [yaw pitch] [monde]" a partir de args[from] ; message et null si illisible. */
    private Point parsePoint(CommandSender sender, String[] args, int from) {
        try {
            double x = Double.parseDouble(args[from].replace(',', '.'));
            double y = Double.parseDouble(args[from + 1].replace(',', '.'));
            double z = Double.parseDouble(args[from + 2].replace(',', '.'));
            float yaw = 0;
            float pitch = 0;
            int next = from + 3;
            if (args.length >= from + 5) {
                yaw = Float.parseFloat(args[from + 3].replace(',', '.'));
                pitch = Float.parseFloat(args[from + 4].replace(',', '.'));
                next = from + 5;
            }
            String world = args.length > next ? args[next] : "";
            return new Point(world == null ? "" : world.trim(), x, y, z, yaw, pitch);
        } catch (RuntimeException e) {
            sender.sendMessage(lang.c("bad-coordinates", "<red>Coordonnées illisibles : x y z, puis yaw pitch et le "
                    + "monde si besoin (vide = monde principal du serveur d'arrivée)."));
            return null;
        }
    }

    // ------------------------------------------------------------------ enregistrement

    private void setLanding(CommandSender sender, String server, String mode, Point point) {
        String key = server;
        ConfigurationSection existing = landingSection(server);
        if (existing != null) {
            key = existing.getName();
        }
        String path = "landings." + key;
        if (NONE.equals(mode)) {
            plugin.getConfig().set(path, null);
        } else {
            plugin.getConfig().set(path, null);
            plugin.getConfig().set(path + ".mode", mode);
            if (point != null) {
                writePoint(path, point);
            }
        }
        plugin.saveConfig();
        plugin.getLogger().info("Point de chute vers " + key + " : " + mode + " (par " + sender.getName() + ").");
        sender.sendMessage(lang.c("landing-set", "<green>Point de chute vers <white><server></white> : <state>",
                "server", key, "state", describe(plugin.getConfig().getConfigurationSection(path))));
        if (!relayReady() && !NONE.equals(mode)) {
            sender.sendMessage(lang.c("landing-no-relay", "<yellow>Attention : relay-url / relay-token vides dans "
                    + "config.yml, le point de chute ne sera pas transmis."));
        }
    }

    private void setArrival(CommandSender sender, Point here) {
        plugin.getConfig().set("default-arrival", null);
        plugin.getConfig().set("default-arrival.mode", here == null ? LAST : POINT);
        if (here != null) {
            writePoint("default-arrival", here);
        }
        plugin.saveConfig();
        plugin.getLogger().info("Arrivée par défaut : " + (here == null ? LAST : POINT) + " (par " + sender.getName() + ").");
        sender.sendMessage(lang.c("arrival-set", "<green>Arrivée sur ce serveur (sans consigne) : <state>",
                "state", describe(plugin.getConfig().getConfigurationSection("default-arrival"))));
    }

    private void writePoint(String path, Point point) {
        plugin.getConfig().set(path + ".world", point.world());
        plugin.getConfig().set(path + ".x", round(point.x()));
        plugin.getConfig().set(path + ".y", round(point.y()));
        plugin.getConfig().set(path + ".z", round(point.z()));
        plugin.getConfig().set(path + ".yaw", round(point.yaw()));
        plugin.getConfig().set(path + ".pitch", round(point.pitch()));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Component describe(ConfigurationSection section) {
        String mode = section == null ? NONE : section.getString("mode", NONE);
        if (LAST.equals(mode)) {
            return lang.c("state-last", "<white>dernière position");
        }
        if (POINT.equals(mode)) {
            String world = section.getString("world", "");
            return lang.c("state-point", "<white><x> <y> <z></white> <gray>(<world>)",
                    "x", section.getDouble("x"), "y", section.getDouble("y"), "z", section.getDouble("z"),
                    "world", world.isBlank() ? "monde principal" : world);
        }
        return lang.c("state-none", "<gray>non défini (le serveur d'arrivée décide)");
    }

    // ------------------------------------------------------------------ interface "Points de chute"

    void openMenu(Player player, Consumer<Player> back) {
        if (!player.hasPermission("klmportal.admin")) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.button(lang.c("landings.arrival-button", "<gold>Arrivée sur ce serveur : <state>",
                        "state", describe(plugin.getConfig().getConfigurationSection("default-arrival"))),
                lang.c("landings.arrival-tooltip", "<gray>Pour les joueurs qui arrivent sans consigne (connexion "
                        + "directe, serveur sans point de chute)."),
                p -> openArrival(p, back)));
        for (String server : plugin.destinationServers()) {
            buttons.add(gui.button(lang.c("landings.server-button", "<#09add3><server></#09add3> : <state>",
                            "server", server, "state", describe(landingSection(server))),
                    null, p -> openLanding(p, server, back)));
        }
        buttons.add(gui.button(lang.c("add.back", "<gray>Retour"), null, back::accept));
        gui.open(player, lang.c("landings.title", "<#09add3><bold>Points de chute"),
                List.of(lang.c("landings.body", "<gray>Où arrivent les joueurs envoyés d'ici vers chaque serveur "
                        + "(boussole, /lobby, portails). Coordonnées : relève-les avec F3 sur le serveur d'arrivée.")),
                List.of(), buttons, null, 1);
        lang.saveIfNeeded();
    }

    private void openLanding(Player player, String server, Consumer<Player> back) {
        ConfigurationSection current = landingSection(server);
        String mode = current == null ? NONE : current.getString("mode", NONE);
        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(gui.choice("mode", lang.c("landings.mode", "Point de chute"), List.of(NONE, LAST, POINT),
                List.of(lang.c("state-none", "<gray>non défini (le serveur d'arrivée décide)"),
                        lang.c("state-last", "<white>dernière position"),
                        lang.c("landings.mode-point", "<white>coordonnées ci-dessous")), mode));
        inputs.add(gui.text("x", Component.text("X"), text(current, "x"), 16));
        inputs.add(gui.text("y", Component.text("Y"), text(current, "y"), 16));
        inputs.add(gui.text("z", Component.text("Z"), text(current, "z"), 16));
        inputs.add(gui.text("yaw", lang.c("landings.yaw", "Orientation (yaw, facultatif)"), text(current, "yaw"), 16));
        inputs.add(gui.text("pitch", lang.c("landings.pitch", "Inclinaison (pitch, facultatif)"), text(current, "pitch"), 16));
        inputs.add(gui.text("world", lang.c("landings.world", "Monde (vide = monde principal)"),
                current == null ? "" : current.getString("world", ""), 64));
        ActionButton save = gui.form(lang.c("landings.save", "<green>Enregistrer"), null, (clicker, view) -> {
            if (!clicker.hasPermission("klmportal.admin")) {
                return;
            }
            String chosen = view.getText("mode");
            if (POINT.equals(chosen)) {
                String[] args = {"", "", view.getText("x"), view.getText("y"), view.getText("z"),
                        blankToZero(view.getText("yaw")), blankToZero(view.getText("pitch")), view.getText("world")};
                Point point = parsePoint(clicker, args, 2);
                if (point == null) {
                    return;
                }
                setLanding(clicker, server, POINT, point);
            } else {
                setLanding(clicker, server, LAST.equals(chosen) ? LAST : NONE, null);
            }
            openMenu(clicker, back);
        });
        gui.open(player, lang.c("landings.server-title", "<#09add3><bold>Point de chute : <server>", "server", server),
                List.of(lang.c("landings.server-body", "<gray>Coordonnées sur le serveur <white><server></white>.",
                        "server", server)),
                inputs, List.of(save, gui.button(lang.c("add.back", "<gray>Retour"), null, p -> openMenu(p, back))),
                null, 1);
        lang.saveIfNeeded();
    }

    private void openArrival(Player player, Consumer<Player> back) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.button(lang.c("landings.here", "<green>Définir ici (ta position et ton orientation)"), null, p -> {
            if (p.hasPermission("klmportal.admin")) {
                setArrival(p, Point.of(p.getLocation()));
                openMenu(p, back);
            }
        }));
        buttons.add(gui.button(lang.c("landings.last", "<white>Dernière position"), null, p -> {
            if (p.hasPermission("klmportal.admin")) {
                setArrival(p, null);
                openMenu(p, back);
            }
        }));
        buttons.add(gui.button(lang.c("add.back", "<gray>Retour"), null, p -> openMenu(p, back)));
        gui.open(player, lang.c("landings.arrival-title", "<gold><bold>Arrivée sur ce serveur"),
                List.of(lang.c("landings.arrival-body", "<gray>Actuellement : <state><gray>. S'applique aux joueurs qui "
                        + "arrivent sans consigne du serveur de départ.",
                        "state", describe(plugin.getConfig().getConfigurationSection("default-arrival")))),
                List.of(), buttons, null, 1);
        lang.saveIfNeeded();
    }

    private static String text(ConfigurationSection section, String key) {
        return section == null || !section.contains(key) ? "" : String.valueOf(section.getDouble(key));
    }

    private static String blankToZero(String value) {
        return value == null || value.isBlank() ? "0" : value;
    }
}
