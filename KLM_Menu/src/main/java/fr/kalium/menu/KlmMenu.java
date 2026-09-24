package fr.kalium.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KLM_Menu (anciennement KaliumMenu, renomme en 2.0.0) : la couche profonde des interfaces du reseau KaLium, presente
 * sur TOUS les serveurs Paper (demande de LeKiwi06, 24/09/2026).
 * - Navigation entre serveurs (lobby, hubs...), base sur les Dialogs natifs de Minecraft (aucun coffre). Le transfert
 *   passe par le canal BungeeCord, supporte par Velocity (bungee-plugin-message-channel = true).
 * - Boite a outils des menus pour les autres plugins (fr.kalium.menu.api : Gui, Lang).
 * - Catalogue des interfaces : chaque plugin declare les siennes (MenuSection, registre de services de Paper) ;
 *   KLM_Menu les decouvre au demarrage et les affiche, rangees par plugin, dans le bouton "Interfaces".
 */
public final class KlmMenu extends JavaPlugin implements Listener, PluginMessageListener, CommandExecutor, TabCompleter {

    private static final String CHANNEL = "BungeeCord";
    private static final long OPEN_COOLDOWN_MS = 800L;

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, Integer> playerCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastOpen = new ConcurrentHashMap<>();
    private final Map<String, ServerEntry> servers = new LinkedHashMap<>();

    private NamespacedKey compassKey;
    /** 2.0.0 : etiquette des boussoles donnees par KaliumMenu (avant le renommage), toujours reconnue. */
    private final NamespacedKey legacyCompassKey = new NamespacedKey("kaliummenu", "menu_compass");
    /** 2.0.0 : interfaces declarees par les plugins (voir MenuSection), mises a jour par refreshSections(). */
    private final List<fr.kalium.menu.api.MenuSection> sections = new java.util.concurrent.CopyOnWriteArrayList<>();
    private fr.kalium.menu.api.Lang lang;
    private fr.kalium.menu.api.Gui gui;
    private boolean lobbyRole;
    private String lobbyServer;
    private boolean showCounts;
    private int buttonWidth;

    /** Bouton special (en plus de "changer de serveur" / "executer une commande"). */
    private enum Special { NONE, OP_SWITCH, GAMEMODE_SWITCH, SETTINGS, CATALOG }

    /** Entree du menu : un serveur, ou (local = true) une commande executee sur ce serveur. */
    private record ServerEntry(String id, String display, String description, boolean local, String command,
                               List<Pattern> worlds, List<Pattern> excludeWorlds, Special special) {

        ServerEntry(String id, String display, String description) {
            this(id, display, description, false, "", List.of(), List.of(), Special.NONE);
        }

        ServerEntry(String id, String display, String description, boolean local, String command,
                    List<Pattern> worlds, List<Pattern> excludeWorlds) {
            this(id, display, description, local, command, worlds, excludeWorlds, Special.NONE);
        }
    }

    /** Operateurs autorises a utiliser les boutons de bascule operateur/joueur et de changement de mode de jeu
     *  (defaut si la cle "operators" est absente du config.yml). */
    private static final List<String> DEFAULT_OPERATORS = List.of("Maaxster", "LeKiwi06");

    private static final org.bukkit.GameMode[] GAMEMODE_CYCLE = {
            org.bukkit.GameMode.SURVIVAL, org.bukkit.GameMode.CREATIVE,
            org.bukkit.GameMode.ADVENTURE, org.bukkit.GameMode.SPECTATOR
    };

    private final java.util.Set<String> operators = new java.util.HashSet<>();

    private final Map<String, ServerEntry> localEntries = new LinkedHashMap<>();

    /** Destinations desactivees sur CE serveur (1.5.0) : identifiants des entrees du menu (serveurs, retour au
     *  lobby, entrees locales). Cachees pour tout le monde (choix de l'utilisateur) ; /lobby suit le retour au lobby. */
    private final java.util.Set<String> disabled = new java.util.LinkedHashSet<>();

    // ------------------------------------------------------------------ cycle de vie

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        compassKey = new NamespacedKey(this, "menu_compass");
        lang = new fr.kalium.menu.api.Lang(this);
        gui = new fr.kalium.menu.api.Gui(this, lang);

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);

        for (String name : List.of("servers", "lobby", "kaliummenu")) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(this);
                command.setTabCompleter(this);
            }
        }

        // Garde le cache du nombre de joueurs a jour tant qu'un joueur est en ligne.
        getServer().getScheduler().runTaskTimer(this, this::refreshCounts, 40L, 200L);

        // 2.0.0 : decouverte des interfaces une fois TOUS les plugins actives (et suivi des ajouts / retraits).
        getServer().getScheduler().runTask(this, () -> refreshSections(true));

        getLogger().info("KLM_Menu actif (role : " + (lobbyRole ? "lobby" : "backend") + ").");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        if (lang != null) {
            lang.saveIfNeeded();
        }
    }

    // ------------------------------------------------------------------ catalogue des interfaces (2.0.0)

    /** Interroge le registre de services : toutes les interfaces declarees par les plugins actifs. */
    private void refreshSections(boolean log) {
        List<fr.kalium.menu.api.MenuSection> found = new ArrayList<>();
        for (var registration : getServer().getServicesManager().getRegistrations(fr.kalium.menu.api.MenuSection.class)) {
            if (registration.getPlugin().isEnabled()) {
                found.add(registration.getProvider());
            }
        }
        found.sort(java.util.Comparator.comparing((fr.kalium.menu.api.MenuSection s) -> s.owner().getName().toLowerCase(Locale.ROOT))
                .thenComparing(s -> s.audience().ordinal()));
        sections.clear();
        sections.addAll(found);
        lang.saveIfNeeded();
        if (log) {
            java.util.Set<String> owners = new java.util.TreeSet<>();
            for (var section : found) {
                owners.add(section.owner().getName());
            }
            getLogger().info(found.size() + " interface(s) trouvée(s)" + (owners.isEmpty() ? "." : " : " + String.join(", ", owners) + "."));
        }
    }

    @EventHandler
    public void onServiceRegister(org.bukkit.event.server.ServiceRegisterEvent event) {
        if (event.getProvider().getService() == fr.kalium.menu.api.MenuSection.class) {
            getServer().getScheduler().runTask(this, () -> refreshSections(false));
        }
    }

    @EventHandler
    public void onServiceUnregister(org.bukkit.event.server.ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == fr.kalium.menu.api.MenuSection.class) {
            getServer().getScheduler().runTask(this, () -> refreshSections(false));
        }
    }

    @EventHandler
    public void onPluginDisable(org.bukkit.event.server.PluginDisableEvent event) {
        if (event.getPlugin() != this) {
            sections.removeIf(section -> section.owner() == event.getPlugin());
        }
    }

    private List<fr.kalium.menu.api.MenuSection> visibleSections(Player player) {
        List<fr.kalium.menu.api.MenuSection> list = new ArrayList<>();
        for (var section : sections) {
            try {
                if (section.owner().isEnabled() && section.visibleTo(player)) {
                    list.add(section);
                }
            } catch (RuntimeException e) {
                getLogger().warning("Interface " + section.owner().getName() + "/" + section.id() + " : " + e);
            }
        }
        return list;
    }

    /**
     * Catalogue (demande de LeKiwi06 : "une interface claire pour trouver les interfaces de chaque chose") : un bouton
     * par plugin, puis ses interfaces. Un plugin qui n'en a qu'une l'ouvre directement.
     */
    private void openCatalog(Player player) {
        Map<String, List<fr.kalium.menu.api.MenuSection>> byPlugin = new LinkedHashMap<>();
        for (var section : visibleSections(player)) {
            byPlugin.computeIfAbsent(section.owner().getName(), k -> new ArrayList<>()).add(section);
        }
        List<Component> body = new ArrayList<>();
        body.add(lang.c("catalog.body", "<gray>Toutes les interfaces de <white>ce serveur<gray>, rangées par plugin."));
        if (byPlugin.isEmpty()) {
            body.add(lang.c("catalog.empty", "<gray>Aucune interface disponible ici."));
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (Map.Entry<String, List<fr.kalium.menu.api.MenuSection>> entry : byPlugin.entrySet()) {
            List<fr.kalium.menu.api.MenuSection> list = entry.getValue();
            Component label = lang.c("catalog.plugin", "<gold><plugin></gold> <dark_gray>(<n>)",
                    "plugin", entry.getKey(), "n", list.size());
            Component tip = list.size() == 1 ? list.get(0).description()
                    : lang.c("catalog.plugin-tip", "<gray><n> interfaces", "n", list.size());
            buttons.add(gui.button(label, tip, p -> {
                if (list.size() == 1) {
                    openSection(p, list.get(0));
                } else {
                    openPluginSections(p, entry.getKey(), list);
                }
            }));
        }
        buttons.add(gui.button(lang.c("catalog.back", "<gray>Retour"), null, this::openMenu));
        gui.open(player, lang.c("catalog.title", "<#09add3><bold>Interfaces"), body, List.of(), buttons, null, 1);
    }

    private void openPluginSections(Player player, String pluginName, List<fr.kalium.menu.api.MenuSection> list) {
        List<ActionButton> buttons = new ArrayList<>();
        for (var section : list) {
            Component label = section.audience() == fr.kalium.menu.api.MenuSection.Audience.ADMINS
                    ? section.title().append(lang.c("catalog.admin-mark", " <dark_gray>(admin)"))
                    : section.title();
            buttons.add(gui.button(label, section.description(), p -> openSection(p, section)));
        }
        buttons.add(gui.button(lang.c("catalog.back", "<gray>Retour"), null, this::openCatalog));
        gui.open(player, lang.c("catalog.plugin-title", "<gold><bold><plugin>", "plugin", pluginName),
                List.of(lang.c("catalog.plugin-body", "<gray>Interfaces de <white><plugin><gray>.", "plugin", pluginName)),
                List.of(), buttons, null, 1);
    }

    private void openSection(Player player, fr.kalium.menu.api.MenuSection section) {
        if (!section.owner().isEnabled() || !section.visibleTo(player)) {
            player.sendMessage(mm.deserialize(msg("destination-disabled")));
            return;
        }
        try {
            section.open(player, this::openCatalog);
        } catch (RuntimeException e) {
            getLogger().warning("Ouverture de " + section.owner().getName() + "/" + section.id() + " impossible : " + e);
        }
    }

    private void loadSettings() {
        reloadConfig();
        lobbyRole = "lobby".equalsIgnoreCase(getConfig().getString("role", "backend"));
        lobbyServer = getConfig().getString("lobby-server", "lobby");
        showCounts = getConfig().getBoolean("show-player-count", true);
        buttonWidth = Math.max(1, Math.min(1024, getConfig().getInt("button-width", 220)));

        operators.clear();
        List<String> configured = getConfig().contains("operators") ? getConfig().getStringList("operators") : DEFAULT_OPERATORS;
        for (String value : configured) {
            if (value != null && !value.isBlank()) {
                operators.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }

        disabled.clear();
        for (String id : getConfig().getStringList("disabled-destinations")) {
            if (id != null && !id.isBlank()) {
                disabled.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }

        servers.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("servers");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                servers.put(id, new ServerEntry(
                        id,
                        section.getString(id + ".display", id),
                        section.getString(id + ".description", "")));
            }
        }

        // Entrees locales : boutons qui executent une commande sur ce serveur (ex. /hub), selon le monde du joueur.
        localEntries.clear();
        ConfigurationSection local = getConfig().getConfigurationSection("local-entries");
        if (local != null) {
            for (String id : local.getKeys(false)) {
                String action = local.getString(id + ".action", "command");
                String command = local.getString(id + ".command", id);
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                localEntries.put(id, new ServerEntry(
                        id,
                        local.getString(id + ".display", id),
                        local.getString(id + ".description", ""),
                        !action.equalsIgnoreCase("server"),
                        command,
                        globs(local.getStringList(id + ".worlds")),
                        globs(local.getStringList(id + ".exclude-worlds"))));
            }
        }
    }

    private List<Pattern> globs(List<String> values) {
        List<Pattern> patterns = new ArrayList<>();
        for (String value : values) {
            StringBuilder regex = new StringBuilder();
            for (String part : value.split("\\*", -1)) {
                if (regex.length() > 0 || value.startsWith("*")) {
                    regex.append(".*");
                }
                regex.append(Pattern.quote(part));
            }
            patterns.add(Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE));
        }
        return patterns;
    }

    private boolean matchesWorld(Player player, List<Pattern> patterns) {
        String name = player.getWorld().getName();
        String key = player.getWorld().getKey().toString();
        for (Pattern pattern : patterns) {
            if (pattern.matcher(name).matches() || pattern.matcher(key).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean visibleFor(Player player, ServerEntry entry) {
        if (!entry.excludeWorlds().isEmpty() && matchesWorld(player, entry.excludeWorlds())) {
            return false;
        }
        return entry.worlds().isEmpty() || matchesWorld(player, entry.worlds());
    }

    /** Le joueur figure-t-il dans la liste "operators" (pseudo, insensible a la casse, ou UUID) ? */
    private boolean isListedOperator(Player player) {
        return operators.contains(player.getName().toLowerCase(Locale.ROOT))
                || operators.contains(player.getUniqueId().toString().toLowerCase(Locale.ROOT));
    }

    /** Bascule le joueur entre operateur et joueur (sur ce serveur uniquement, comme /op et /deop). */
    private void toggleOperator(Player player) {
        if (!player.isOnline()) {
            return;
        }
        // Verifie a nouveau au moment du clic : la liste a pu etre rechargee entre-temps.
        if (!isListedOperator(player)) {
            return;
        }
        boolean nowOp = !player.isOp();
        player.setOp(nowOp);
        player.updateCommands();
        if (!nowOp) {
            // Repasser joueur remet automatiquement en survie (evite de rester en creatif/spectateur sans les droits).
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
        player.sendMessage(mm.deserialize(msg(nowOp ? "op-now-operator" : "op-now-player")));
        getLogger().info(player.getName() + (nowOp ? " est passé opérateur." : " est repassé joueur (mode survie)."));
    }

    private String gamemodeKey(org.bukkit.GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> "gamemode-survival";
            case CREATIVE -> "gamemode-creative";
            case ADVENTURE -> "gamemode-adventure";
            case SPECTATOR -> "gamemode-spectator";
        };
    }

    private org.bukkit.GameMode nextGamemode(org.bukkit.GameMode current) {
        for (int i = 0; i < GAMEMODE_CYCLE.length; i++) {
            if (GAMEMODE_CYCLE[i] == current) {
                return GAMEMODE_CYCLE[(i + 1) % GAMEMODE_CYCLE.length];
            }
        }
        return GAMEMODE_CYCLE[0];
    }

    /** Fait passer le joueur au mode de jeu suivant du cycle (survie -> creatif -> aventure -> spectateur -> survie). */
    private void cycleGamemode(Player player) {
        if (!player.isOnline() || !isListedOperator(player)) {
            return;
        }
        // En joueur (pas operateur), seule la survie est autorisee : le bouton n'est pas propose, mais on verifie
        // a nouveau au clic (dialog en cache, ou droits retires entre-temps).
        if (!player.isOp()) {
            if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.sendMessage(mm.deserialize(msg("gamemode-locked")));
            }
            return;
        }
        org.bukkit.GameMode next = nextGamemode(player.getGameMode());
        player.setGameMode(next);
        player.sendMessage(mm.deserialize(msg("gamemode-now"),
                Placeholder.component("mode", mm.deserialize(msg(gamemodeKey(next))))));
        getLogger().info(player.getName() + " est passé en mode " + next + ".");
    }

    // ------------------------------------------------------------------ commandes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("kaliummenu")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                sender.sendMessage(mm.deserialize(msg("reloaded")));
            } else if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
                Player target = args.length >= 2 ? getServer().getPlayerExact(args[1])
                        : (sender instanceof Player self ? self : null);
                if (target == null) {
                    sender.sendMessage(mm.deserialize(msg("player-not-found")));
                } else {
                    giveCompass(target);
                    if (sender instanceof Player) {
                        sender.sendMessage(mm.deserialize(msg("give-done"), Placeholder.unparsed("player", target.getName())));
                    }
                }
            } else {
                sender.sendMessage(mm.deserialize("<gray>Usage : /" + label + " reload | give [joueur]"));
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (name.equals("servers")) {
            openMenu(player);
        } else if (name.equals("lobby")) {
            if (lobbyRole) {
                player.sendMessage(mm.deserialize(msg("already-in-lobby")));
            } else if (isDisabled(lobbyServer)) {
                player.sendMessage(mm.deserialize(msg("destination-disabled")));
            } else {
                connect(player, lobbyServer);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("kaliummenu")) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("reload", "give")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    result.add(option);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player online : getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(online.getName());
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ menu (Dialog)

    /** Entrees "serveur" (celles dont on affiche le nombre de joueurs). */
    private Collection<ServerEntry> menuEntries() {
        if (lobbyRole) {
            return servers.values();
        }
        return List.of(new ServerEntry(lobbyServer, msg("back-button"), msg("back-description")));
    }

    /** Entrees affichees a ce joueur : entrees locales (selon son monde) puis serveurs. */
    private List<ServerEntry> menuEntriesFor(Player player) {
        List<ServerEntry> entries = new ArrayList<>();
        List<ServerEntry> visibleLocals = new ArrayList<>();
        for (ServerEntry entry : localEntries.values()) {
            if (visibleFor(player, entry)) {
                visibleLocals.add(entry);
            }
        }
        if (lobbyRole) {
            entries.addAll(servers.values());
            entries.addAll(visibleLocals);
        } else {
            entries.addAll(visibleLocals);
            entries.addAll(menuEntries());
        }
        entries.removeIf(entry -> isDisabled(entry.id()));
        if (!visibleSections(player).isEmpty()) {
            entries.add(new ServerEntry("catalog", msg("catalog-button"), msg("catalog-description"),
                    true, "", List.of(), List.of(), Special.CATALOG));
        }
        if (player.hasPermission("kaliummenu.admin")) {
            entries.add(new ServerEntry("settings", msg("settings-button"), msg("settings-description"),
                    true, "", List.of(), List.of(), Special.SETTINGS));
        }
        if (isListedOperator(player)) {
            boolean op = player.isOp();
            entries.add(new ServerEntry(
                    "op-switch",
                    msg(op ? "op-button-to-player" : "op-button-to-operator"),
                    msg(op ? "op-description-to-player" : "op-description-to-operator"),
                    true, "", List.of(), List.of(), Special.OP_SWITCH));

            // Le changement de mode de jeu n'est propose qu'aux operateurs actifs : en joueur, on reste en survie.
            if (op) {
                org.bukkit.GameMode next = nextGamemode(player.getGameMode());
                entries.add(new ServerEntry(
                        "gamemode-switch",
                        msg("gamemode-button").replace("<mode>", msg(gamemodeKey(player.getGameMode()))),
                        msg("gamemode-description").replace("<mode>", msg(gamemodeKey(next))),
                        true, "", List.of(), List.of(), Special.GAMEMODE_SWITCH));
            }
        }
        return entries;
    }

    private void openMenu(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastOpen.put(player.getUniqueId(), now);
        if (previous != null && now - previous < OPEN_COOLDOWN_MS) {
            return;
        }

        List<ServerEntry> entries = menuEntriesFor(player);
        if (entries.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("no-servers")));
            return;
        }

        if (!showCounts) {
            showDialog(player, entries);
            return;
        }

        // Demande des chiffres a jour, puis affichage apres un court delai (~200 ms).
        for (ServerEntry entry : entries) {
            if (!entry.local()) {
                requestCount(player, entry.id());
            }
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                showDialog(player, entries);
            }
        }, 4L);
    }

    private void showDialog(Player player, List<ServerEntry> entries) {
        List<ActionButton> buttons = new ArrayList<>();

        for (ServerEntry entry : entries) {
            Component label = mm.deserialize(entry.display());
            Integer count = showCounts && !entry.local() ? playerCounts.get(entry.id().toLowerCase(Locale.ROOT)) : null;
            if (count != null) {
                label = label.append(mm.deserialize(
                        msg("count-format"),
                        Placeholder.unparsed("count", String.valueOf(count))));
            }
            Component tooltip = entry.description().isBlank() ? null : mm.deserialize(entry.description());
            String target = entry.id();

            buttons.add(ActionButton.create(
                    label,
                    tooltip,
                    buttonWidth,
                    DialogAction.customClick(
                            (view, audience) -> {
                                if (audience instanceof Player clicker) {
                                    getServer().getScheduler().runTask(this, () -> {
                                        if (entry.special() == Special.OP_SWITCH) {
                                            toggleOperator(clicker);
                                        } else if (entry.special() == Special.GAMEMODE_SWITCH) {
                                            cycleGamemode(clicker);
                                        } else if (entry.special() == Special.SETTINGS) {
                                            openSettings(clicker);
                                        } else if (entry.special() == Special.CATALOG) {
                                            openCatalog(clicker);
                                        } else if (isDisabled(target)) {
                                            clicker.sendMessage(mm.deserialize(msg("destination-disabled")));
                                        } else if (entry.local()) {
                                            clicker.performCommand(entry.command());
                                        } else {
                                            connect(clicker, target);
                                        }
                                    });
                                }
                            },
                            ClickCallback.Options.builder()
                                    .uses(1)
                                    .lifetime(Duration.ofMinutes(2))
                                    .build())));
        }

        // Action nulle = ferme simplement le Dialog.
        ActionButton close = ActionButton.create(mm.deserialize(msg("close-button")), null, buttonWidth, null);

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(mm.deserialize(msg("menu-title")))
                        .body(List.of(DialogBody.plainMessage(mm.deserialize(msg("menu-header")))))
                        .build())
                .type(DialogType.multiAction(buttons, close, 1)));

        player.showDialog(dialog);
    }

    // ------------------------------------------------------------------ parametres (1.5.0)

    private boolean isDisabled(String id) {
        return id != null && disabled.contains(id.toLowerCase(Locale.ROOT));
    }

    /** Toutes les destinations de ce serveur (quel que soit le monde du joueur), pour le menu Parametres. */
    private List<ServerEntry> allDestinations() {
        List<ServerEntry> list = new ArrayList<>(menuEntries());
        list.addAll(localEntries.values());
        return list;
    }

    /**
     * Parametres (1.5.0 - demande explicite de l'utilisateur : "ajouter a kalium menu un bouton parametre pour les
     * op, pour pouvoir activer ou desactiver des teleportations") : un interrupteur par destination du menu de CE
     * serveur. Une destination desactivee est cachee pour tout le monde. Enregistre dans config.yml
     * (disabled-destinations).
     */
    private void openSettings(Player player) {
        if (!player.hasPermission("kaliummenu.admin")) {
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (ServerEntry entry : allDestinations()) {
            boolean off = isDisabled(entry.id());
            Component label = mm.deserialize(entry.display())
                    .append(mm.deserialize(msg(off ? "settings-state-off" : "settings-state-on")));
            String id = entry.id().toLowerCase(Locale.ROOT);
            buttons.add(ActionButton.create(label, mm.deserialize(msg("settings-toggle-description")), buttonWidth,
                    DialogAction.customClick((view, audience) -> {
                        if (audience instanceof Player clicker) {
                            getServer().getScheduler().runTask(this, () -> {
                                if (!clicker.hasPermission("kaliummenu.admin")) {
                                    return;
                                }
                                if (!disabled.remove(id)) {
                                    disabled.add(id);
                                }
                                getConfig().set("disabled-destinations", new ArrayList<>(disabled));
                                saveConfig();
                                openSettings(clicker);
                            });
                        }
                    }, ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build())));
        }
        ActionButton back = ActionButton.create(mm.deserialize(msg("close-button")), null, buttonWidth, null);
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(mm.deserialize(msg("settings-title")))
                        .body(List.of(DialogBody.plainMessage(mm.deserialize(msg("settings-header")))))
                        .build())
                .type(DialogType.multiAction(buttons, back, 1)));
        player.showDialog(dialog);
    }

    // ------------------------------------------------------------------ canal BungeeCord / Velocity

    private void connect(Player player, String server) {
        if (!player.isOnline()) {
            return;
        }
        byte[] data = pluginMessage("Connect", server);
        if (data == null) {
            return;
        }
        player.sendMessage(mm.deserialize(msg("connecting"), Placeholder.unparsed("server", server)));
        player.sendPluginMessage(this, CHANNEL, data);
    }

    private void requestCount(Player carrier, String server) {
        byte[] data = pluginMessage("PlayerCount", server);
        if (data != null) {
            carrier.sendPluginMessage(this, CHANNEL, data);
        }
    }

    private void refreshCounts() {
        if (!showCounts) {
            return;
        }
        Player carrier = getServer().getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            return;
        }
        for (ServerEntry entry : menuEntries()) {
            requestCount(carrier, entry.id());
        }
    }

    private byte[] pluginMessage(String subChannel, String argument) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(subChannel);
            out.writeUTF(argument);
        } catch (IOException e) {
            getLogger().warning("Impossible de construire le message " + subChannel + " : " + e.getMessage());
            return null;
        }
        return bytes.toByteArray();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subChannel = in.readUTF();
            if (subChannel.equals("PlayerCount")) {
                String server = in.readUTF();
                int count = in.readInt();
                playerCounts.put(server.toLowerCase(Locale.ROOT), count);
            }
        } catch (IOException ignored) {
            // Message incomplet ou inconnu : on l'ignore.
        }
    }

    // ------------------------------------------------------------------ boussole

    private ItemStack createCompass() {
        Material material = Material.matchMaterial(getConfig().getString("compass.material", "COMPASS"));
        if (material == null || material.isAir() || !material.isItem()) {
            material = Material.COMPASS;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(noItalic(mm.deserialize(getConfig().getString("compass.name", "<#09add3><bold>Navigation"))));
        List<Component> lore = new ArrayList<>();
        for (String line : getConfig().getStringList("compass.lore")) {
            lore.add(noItalic(mm.deserialize(line)));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private boolean isCompass(ItemStack item) {
        return item != null
                && !item.getType().isAir()
                && item.hasItemMeta()
                && (item.getItemMeta().getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE)
                        || item.getItemMeta().getPersistentDataContainer().has(legacyCompassKey, PersistentDataType.BYTE));
    }

    /**
     * 2.1.0 - API pour les autres plugins : remet les objets de navigation de KLM_Menu (la boussole) apres qu'un
     * plugin a vide l'inventaire du joueur (ex. KalGames au hub). Demande de LeKiwi06 : "la boussole fait partie de
     * l'interface [...] geree sur tous les serveurs par le meme plugin" - les autres plugins ne la fabriquent plus
     * (KalGames passait par la commande /kaliummenu give). Sans effet si la boussole est desactivee sur ce serveur.
     */
    public void giveNavigation(Player player) {
        if (player != null && player.isOnline()) {
            giveCompass(player);
        }
    }

    private void giveCompass(Player player) {
        if (!getConfig().getBoolean("compass.enabled", true)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            if (isCompass(stack)) {
                return;
            }
        }

        int slot = getConfig().getInt("compass.slot", 8);
        if (slot < 0 || slot > 8) {
            slot = 8;
        }
        ItemStack existing = inventory.getItem(slot);
        if (existing != null && !existing.getType().isAir()) {
            // Ne jamais ecraser l'objet d'un joueur : on cherche un autre emplacement libre de la hotbar.
            slot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack candidate = inventory.getItem(i);
                if (candidate == null || candidate.getType().isAir()) {
                    slot = i;
                    break;
                }
            }
            if (slot == -1) {
                return;
            }
        }
        inventory.setItem(slot, createCompass());
    }

    private boolean lockEnabled(Player player) {
        return getConfig().getBoolean("compass.lock-item", true) && !player.hasPermission("kaliummenu.bypass");
    }

    // ------------------------------------------------------------------ evenements

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                giveCompass(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                giveCompass(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastOpen.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isCompass(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (lockEnabled(event.getPlayer()) && isCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!lockEnabled(event.getPlayer())) {
            return;
        }
        if (isCompass(event.getMainHandItem()) || isCompass(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !lockEnabled(player)) {
            return;
        }
        if (isCompass(event.getCurrentItem()) || isCompass(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY && isCompass(player.getInventory().getItem(event.getHotbarButton()))) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND && isCompass(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && lockEnabled(player) && isCompass(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isCompass);
    }

    // ------------------------------------------------------------------ utilitaires

    private String msg(String key) {
        String value = getConfig().getString("messages." + key);
        if (value != null) {
            return value;
        }
        return switch (key) {
            case "catalog-button" -> "<#09add3><bold>Interfaces";
            case "catalog-description" -> "<gray>Toutes les interfaces de <white>ce serveur<gray> (jeux, classements, réglages...).";
            case "settings-button" -> "<yellow><bold>Paramètres";
            case "settings-description" -> "<gray>Activer ou désactiver les téléportations de <white>ce serveur<gray>.";
            case "settings-title" -> "<yellow><bold>Paramètres des téléportations";
            case "settings-header" -> "<gray>Clique sur une destination pour l'activer ou la désactiver sur <white>ce serveur<gray>. Une destination désactivée est cachée pour tout le monde.";
            case "settings-state-on" -> " <dark_gray>| <green>activée";
            case "settings-state-off" -> " <dark_gray>| <red>désactivée";
            case "settings-toggle-description" -> "<gray>Clique pour changer.";
            case "destination-disabled" -> "<red>Cette téléportation est désactivée.";
            case "give-done" -> "<green>Boussole donnée à <player>.";
            case "player-not-found" -> "<red>Joueur introuvable.";
            case "op-button-to-player" -> "<red><bold>Passer en joueur";
            case "op-button-to-operator" -> "<green><bold>Passer opérateur";
            case "op-description-to-player" -> "<gray>Retire tes droits d'opérateur sur <white>ce serveur<gray>.";
            case "op-description-to-operator" -> "<gray>Reprends tes droits d'opérateur sur <white>ce serveur<gray>.";
            case "op-now-player" -> "<yellow>Tu es maintenant <white>joueur<yellow> sur ce serveur.";
            case "op-now-operator" -> "<yellow>Tu es maintenant <green>opérateur<yellow> sur ce serveur.";
            case "gamemode-button" -> "<#09add3><bold>Mode : <mode>";
            case "gamemode-description" -> "<gray>Clique pour passer en <mode><gray>.";
            case "gamemode-now" -> "<yellow>Tu es maintenant en mode <mode><yellow>.";
            case "gamemode-survival" -> "<white>Survie";
            case "gamemode-creative" -> "<aqua>Créatif";
            case "gamemode-adventure" -> "<gold>Aventure";
            case "gamemode-spectator" -> "<gray>Spectateur";
            case "gamemode-locked" -> "<red>En joueur, seul le mode survie est autorisé.";
            default -> "<red>[" + key + "]";
        };
    }
}
