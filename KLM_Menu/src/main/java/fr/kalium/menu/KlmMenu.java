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
 * - Interfaces des plugins : chaque plugin declare les siennes (MenuSection, registre de services de Paper) ;
 *   KLM_Menu les decouvre au demarrage. 2.4.0 : elles sont dans le comparateur « Informations » (case de gauche) :
 *   Classements pour tous, Parametres pour les admins ; la boussole ne sert plus qu'a la navigation.
 * - 2.4.0 : menus lisibles sur Bedrock (couleurs trop claires assombries, voir BedrockColors) ; boussole et
 *   comparateur verrouilles pour tout le monde.
 */
public final class KlmMenu extends JavaPlugin implements Listener, PluginMessageListener, CommandExecutor, TabCompleter {

    private static final String CHANNEL = "BungeeCord";
    private static final long OPEN_COOLDOWN_MS = 800L;

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, Integer> playerCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastOpen = new ConcurrentHashMap<>();
    private final Map<String, ServerEntry> servers = new LinkedHashMap<>();

    private NamespacedKey compassKey;
    /** 2.4.0 : etiquette du comparateur « Informations » (klm_menu:informations). */
    private NamespacedKey informationsKey;
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
    private enum Special { NONE, OP_SWITCH, GAMEMODE_SWITCH }

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
        informationsKey = new NamespacedKey(this, "informations");
        lang = new fr.kalium.menu.api.Lang(this);
        gui = new fr.kalium.menu.api.Gui(this, lang);

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);

        for (String name : List.of("servers", "lobby", "kaliummenu", "informations", "menu")) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(this);
                command.setTabCompleter(this);
            }
        }
        loadHidden();
        registerOwnItems();

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

    // ------------------------------------------------------------------ interfaces declarees par les plugins (2.0.0)

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

    // ------------------------------------------------------------------ comparateur « Informations » (2.4.0)

    /**
     * Demande de LeKiwi06 (28/09/2026) : « pour tout ce qui est classements et parametres, tu vas faire ca dans un
     * comparateur avec texture enchantee en slot 1 de la hotbar qui portera le nom "Informations" ; pour les joueurs il
     * contiendra que le classement pour le moment, pour les admins il doit aussi contenir les parametres retravailles ».
     * Remplace le bouton « Interfaces » (catalogue par plugin) et le bouton « Parametres » de la boussole, qui ne sert
     * plus qu'a la navigation.
     */
    private static final java.util.Comparator<fr.kalium.menu.api.MenuSection> SECTION_ORDER =
            java.util.Comparator.comparingInt((fr.kalium.menu.api.MenuSection s) -> s.order())
                    .thenComparing(s -> s.owner().getName().toLowerCase(Locale.ROOT));

    private boolean isAdmin(Player player) {
        return player.hasPermission("kaliummenu.admin");
    }

    /** Interfaces de classements visibles par ce joueur. */
    private List<fr.kalium.menu.api.MenuSection> rankingSections(Player player) {
        List<fr.kalium.menu.api.MenuSection> list = new ArrayList<>();
        for (var section : visibleSections(player)) {
            if (section.ranking()) {
                list.add(section);
            }
        }
        list.sort(SECTION_ORDER);
        return list;
    }

    /** Reglages (interfaces ADMINS qui ne sont pas des classements), a plat. */
    private List<fr.kalium.menu.api.MenuSection> settingsSections(Player player) {
        List<fr.kalium.menu.api.MenuSection> list = new ArrayList<>();
        for (var section : visibleSections(player)) {
            if (section.ranking() || section.moderation()
                    || section.audience() != fr.kalium.menu.api.MenuSection.Audience.ADMINS) {
                continue;
            }
            try {
                list.addAll(section.expand(player));
            } catch (RuntimeException e) {
                getLogger().warning("Interface " + section.owner().getName() + "/" + section.id() + " : " + e);
            }
        }
        list.sort(SECTION_ORDER);
        return list;
    }

    /** 2.7.0 - outils de moderation visibles par ce joueur (rubrique « Moderation » de /menu). */
    private List<fr.kalium.menu.api.MenuSection> moderationSections(Player player) {
        List<fr.kalium.menu.api.MenuSection> list = new ArrayList<>();
        for (var section : visibleSections(player)) {
            if (section.moderation()) {
                list.add(section);
            }
        }
        list.sort(SECTION_ORDER);
        return list;
    }

    /** 2.7.0 - rubrique « Moderation » : un bouton par outil de moderation des plugins de ce serveur. */
    private void openModeration(Player player) {
        List<fr.kalium.menu.api.MenuSection> list = moderationSections(player);
        if (list.isEmpty()) {
            openInterfaces(player);
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (var section : list) {
            buttons.add(gui.button(section.title(), section.description(), p -> openSection(p, section, this::openModeration)));
        }
        buttons.add(gui.button(lang.c("info.back", "<gray>Retour"), null, this::openInterfaces));
        gui.open(player, lang.c("moderation.title", "<red><bold>Modération"),
                List.of(lang.c("moderation.body", "<gray>Outils de modération de <white>ce serveur<gray>.")), List.of(),
                buttons, null, 1);
    }

    /** Le joueur a-t-il quelque chose a voir dans « Informations » ? (sinon, pas de comparateur) */
    private boolean hasInformations(Player player) {
        return isAdmin(player) || !rankingSections(player).isEmpty() || recompenses() != null;
    }

    /** 2.6.0 : interface Récompenses de ce serveur (KG_Rewards, KS_RewardsGUI, KV_Rewards...), ou null. */
    private fr.kalium.menu.api.Recompenses recompenses() {
        for (var service : getServer().getServicesManager().getRegistrations(fr.kalium.menu.api.Recompenses.class)) {
            if (service.getPlugin().isEnabled()) {
                return service.getProvider();
            }
        }
        return null;
    }

    /** 2.4.0 - API : ouvre « Informations » (comparateur). */
    public void openInformations(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        List<fr.kalium.menu.api.MenuSection> rankings = rankingSections(player);
        if (!rankings.isEmpty()) {
            buttons.add(gui.button(lang.c("info.rankings", "<gold><bold>Classements"),
                    lang.c("info.rankings-tip", "<gray>Meilleurs joueurs de chaque jeu, général et du mois."), p -> {
                        List<fr.kalium.menu.api.MenuSection> list = rankingSections(p);
                        if (list.size() == 1) {
                            openSection(p, list.get(0), this::openInformations);
                        } else {
                            openRankings(p);
                        }
                    }));
        }
        // 2.6.0 (catégorie 4) : progression des récompenses (configuration pour les admins).
        if (recompenses() != null) {
            buttons.add(gui.button(lang.c("info.rewards", "<light_purple><bold>Récompenses"),
                    lang.c("info.rewards-tip", "<gray>Ta progression dans les paliers et les tops de ce serveur."), p -> {
                        fr.kalium.menu.api.Recompenses r = recompenses();
                        if (r != null) {
                            r.ouvrir(p);
                        }
                    }));
        }
        if (isAdmin(player)) {
            buttons.add(gui.button(lang.c("info.settings", "<red><bold>Paramètres"),
                    lang.c("info.settings-tip", "<gray>Réglages de <white>ce serveur<gray> (réservé aux admins)."), this::openParametres));
        }
        List<Component> body = List.of(buttons.isEmpty()
                ? lang.c("info.empty", "<gray>Rien à afficher sur ce serveur pour le moment.")
                : lang.c("info.body", "<gray>Choisis une rubrique."));
        gui.open(player, lang.c("info.title", "<#09add3><bold>Informations"), body, List.of(), buttons, null, 1);
    }

    private void openRankings(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        for (var section : rankingSections(player)) {
            buttons.add(gui.button(section.title(), section.description(), p -> openSection(p, section, this::openRankings)));
        }
        buttons.add(gui.button(lang.c("info.back", "<gray>Retour"), null, this::openInformations));
        gui.open(player, lang.c("info.rankings-title", "<gold><bold>Classements"),
                List.of(lang.c("info.rankings-body", "<gray>Choisis un classement.")), List.of(), buttons, null, 1);
    }

    /** 2.4.0 - API : ouvre « Parametres » (admins) : les reglages de chaque plugin de ce serveur, a plat. */
    public void openParametres(Player player) {
        if (!isAdmin(player)) {
            openInformations(player);
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (var section : settingsSections(player)) {
            buttons.add(gui.button(section.title(), section.description(), p -> openSection(p, section, this::openParametres)));
        }
        buttons.add(gui.button(lang.c("info.teleports", "<aqua>Téléportations"),
                lang.c("info.teleports-tip", "<gray>Activer ou désactiver les destinations de la boussole (et les portails reliés)."),
                this::openSettings));
        buttons.add(gui.button(lang.c("info.back", "<gray>Retour"), null, this::openInformations));
        gui.open(player, lang.c("info.settings-title", "<red><bold>Paramètres"),
                List.of(lang.c("info.settings-body", "<gray>Réglages de <white>ce serveur<gray>.")), List.of(), buttons, null, 1);
    }

    private void openSection(Player player, fr.kalium.menu.api.MenuSection section, java.util.function.Consumer<Player> back) {
        if (!section.owner().isEnabled() || !section.visibleTo(player)) {
            player.sendMessage(mm.deserialize(msg("destination-disabled")));
            return;
        }
        try {
            section.open(player, back);
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
        giveInformations(player); // 2.4.0 : comparateur donne ou retire selon les nouveaux droits

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

        if (name.equals("menu")) {
            if (args.length == 0) {
                openInterfaces(player);
            } else if (args[0].equalsIgnoreCase("on")) {
                showItems(player);
            } else if (args[0].equalsIgnoreCase("off")) {
                hideItems(player);
            } else {
                player.sendMessage(lang.c("menu-cmd.usage", "<gray>/menu : ouvrir le menu. <white>/menu off<gray> : retirer les objets de menu de la barre, <white>/menu on<gray> : les remettre."));
            }
        } else if (name.equals("servers")) {
            openMenu(player);
        } else if (name.equals("informations")) {
            openInformations(player);
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
        if (command.getName().equalsIgnoreCase("menu")) {
            List<String> options = new ArrayList<>();
            if (args.length == 1) {
                for (String option : List.of("on", "off")) {
                    if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                        options.add(option);
                    }
                }
            }
            return options;
        }
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
        // 2.4.0 : « Interfaces » et « Parametres » quittent la boussole (navigation seulement) pour le comparateur
        // « Informations ».
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
        // 2.5.0 : boutons élargis à leur texte, qui ne défile jamais (voir Lisible).
        fr.kalium.menu.api.Lisible.Fenetre fenetre = fr.kalium.menu.api.Lisible.ajuster(List.of(), buttons, close, 1);
        buttons = fenetre.boutons();
        close = fenetre.sortie();
        DialogBase base = DialogBase.builder(mm.deserialize(msg("menu-title")))
                .body(List.of(DialogBody.plainMessage(mm.deserialize(msg("menu-header")))))
                .build();
        // 2.4.0 : couleurs trop claires assombries pour les joueurs Bedrock.
        boolean bedrock = fr.kalium.menu.api.BedrockColors.isBedrock(player);
        DialogBase shownBase = bedrock ? fr.kalium.menu.api.BedrockColors.adapt(base) : base;
        List<ActionButton> shownButtons = bedrock ? fr.kalium.menu.api.BedrockColors.adaptButtons(buttons) : buttons;
        ActionButton shownClose = bedrock ? fr.kalium.menu.api.BedrockColors.adapt(close) : close;

        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(shownBase)
                .type(DialogType.multiAction(shownButtons, shownClose, 1)));

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
        // 2.4.0 : ouvert depuis « Informations > Parametres » : bouton Retour ; Gui adapte les couleurs pour Bedrock.
        buttons.add(gui.button(lang.c("info.back", "<gray>Retour"), null, this::openParametres));
        gui.open(player, mm.deserialize(msg("settings-title")), List.of(mm.deserialize(msg("settings-header"))),
                List.of(), buttons, ActionButton.create(mm.deserialize(msg("close-button")), null, buttonWidth, null), 1);
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
        java.util.function.BiFunction<Player, String, java.util.concurrent.CompletableFuture<?>> hook = beforeConnect;
        if (hook == null) {
            player.sendPluginMessage(this, CHANNEL, data);
            return;
        }
        // 2.3.0 : on attend le plugin (ex. KLM_Portal qui depose le point de chute), 3 s au plus, puis on envoie.
        java.util.concurrent.CompletableFuture<?> ready;
        try {
            ready = hook.apply(player, server);
        } catch (RuntimeException e) {
            getLogger().warning("Avant le changement de serveur : " + e);
            ready = java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        ready.completeOnTimeout(null, 3, java.util.concurrent.TimeUnit.SECONDS)
                .whenComplete((result, error) -> getServer().getScheduler().runTask(this, () -> {
                    if (player.isOnline()) {
                        player.sendPluginMessage(this, CHANNEL, data);
                    }
                }));
    }

    private volatile java.util.function.BiFunction<Player, String, java.util.concurrent.CompletableFuture<?>> beforeConnect;

    /**
     * 2.3.0 - API pour KLM_Portal (demande de LeKiwi06, 26/09/2026 : un point de chute pour chaque teleportation
     * inter-serveur, regle cote depart). Appele avant CHAQUE changement de serveur fait par KLM_Menu (boussole,
     * /lobby, portails) avec le joueur et le nom du serveur vise ; le changement attend la fin du resultat (3 s au
     * plus). null pour retirer.
     */
    public void setBeforeConnect(java.util.function.BiFunction<Player, String, java.util.concurrent.CompletableFuture<?>> hook) {
        this.beforeConnect = hook;
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
            giveInformations(player);
        }
    }

    // ------------------------------------------------------------------ comparateur « Informations » : objet (2.4.0)

    /** Comparateur actif sur ce serveur ? Par defaut comme la boussole (desactivee la ou elle gene : Bingo, survie). */
    private boolean informationsEnabled() {
        return getConfig().getBoolean("informations.enabled", getConfig().getBoolean("compass.enabled", true));
    }

    private ItemStack createInformations() {
        Material material = Material.matchMaterial(getConfig().getString("informations.material", "COMPARATOR"));
        if (material == null || material.isAir() || !material.isItem()) {
            material = Material.COMPARATOR;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(noItalic(lang.c("info.item-name", "<#09add3><bold>Informations")));
        meta.lore(List.of(noItalic(lang.c("info.item-lore", "<gray>Clic droit : classements"))));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(informationsKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        lang.saveIfNeeded();
        return item;
    }

    /** 2.4.0 - API : le comparateur « Informations » (ex. pour le ranger ailleurs, KV_Menu sur son plot). */
    public boolean isInformations(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(informationsKey, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------------ /menu, /menu on, /menu off (2.4.0)

    /**
     * Demande de LeKiwi06 (28/09/2026) : « /menu ouvre l'interface (si le serveur ne possede qu'un item d'interface :
     * ouvrir directement cette interface ; s'il en possede plusieurs : menu de selection) ; /menu on/off sert a
     * donner / retirer les divers items d'interface de la hotbar : nether star, boussole, comparateur... ; si des items
     * sont contenus dans les slots correspondants : refuser la commande (sauf sur Kanvas comme on est en creatif) ;
     * durant un mini-jeu : refuser la commande ».
     * Choix de chaque joueur, par serveur : plugins/KLM_Menu/objets-masques.yml (joueurs = /menu off, affiches =
     * /menu on). 2.4.1 : sur un serveur ou les objets sont retires par defaut (items-by-default: false, Kixster et
     * Event : « par defaut il doit etre off pour pas deranger les joueurs, mais il faut laisser la possibilite pour
     * ceux qui preferent avoir la boussole », LeKiwi06, 28/09/2026), seuls les joueurs qui ont fait /menu on les ont.
     */
    private final java.util.Set<UUID> hidden = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<UUID> shown = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private java.io.File hiddenFile() {
        return new java.io.File(getDataFolder(), "objets-masques.yml");
    }

    /** 2.4.1 : objets de menu donnes par defaut sur ce serveur ? (items-by-default, true si absent) */
    private boolean itemsByDefault() {
        return getConfig().getBoolean("items-by-default", true);
    }

    private static void readUuids(java.util.Set<UUID> into, List<String> values) {
        into.clear();
        for (String value : values) {
            try {
                into.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // ligne abimee : ignoree
            }
        }
    }

    private void loadHidden() {
        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(hiddenFile());
        readUuids(hidden, yaml.getStringList("joueurs"));
        readUuids(shown, yaml.getStringList("affiches"));
    }

    private void saveHidden() {
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        List<String> off = new ArrayList<>();
        for (UUID uuid : hidden) {
            off.add(uuid.toString());
        }
        List<String> on = new ArrayList<>();
        for (UUID uuid : shown) {
            on.add(uuid.toString());
        }
        yaml.set("joueurs", off);
        yaml.set("affiches", on);
        try {
            getDataFolder().mkdirs();
            yaml.save(hiddenFile());
        } catch (IOException e) {
            getLogger().warning("Impossible d'enregistrer objets-masques.yml : " + e.getMessage());
        }
    }

    /**
     * 2.4.0 - API : le joueur est-il sans objets de menu (/menu off, ou 2.4.1 : objets retires par defaut sur ce
     * serveur et pas de /menu on) ? Les plugins ne lui en donnent pas.
     */
    public boolean itemsHidden(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        return hidden.contains(uuid) || (!itemsByDefault() && !shown.contains(uuid));
    }

    /** Boussole et comparateur, declares comme les objets des autres plugins. */
    private void registerOwnItems() {
        getServer().getServicesManager().register(fr.kalium.menu.api.InterfaceItem.class, new fr.kalium.menu.api.InterfaceItem() {
            public org.bukkit.plugin.Plugin owner() { return KlmMenu.this; }
            public String id() { return "navigation"; }
            public Component name() { return mm.deserialize(getConfig().getString("compass.name", "<#09add3><bold>Navigation")); }
            public int order() { return 10; }
            public boolean available(Player player) { return true; }
            public void open(Player player) { openMenu(player); }
            public int slot(Player player) {
                if (!getConfig().getBoolean("compass.enabled", true)) {
                    return -1;
                }
                int slot = getConfig().getInt("compass.slot", 8);
                return slot < 0 || slot > 8 ? 8 : slot;
            }
            public boolean isItem(ItemStack item) { return isCompass(item); }
            public void give(Player player) { giveCompass(player); }
        }, this, org.bukkit.plugin.ServicePriority.Normal);
        getServer().getServicesManager().register(fr.kalium.menu.api.InterfaceItem.class, new fr.kalium.menu.api.InterfaceItem() {
            public org.bukkit.plugin.Plugin owner() { return KlmMenu.this; }
            public String id() { return "informations"; }
            public Component name() { return lang.c("info.item-name", "<#09add3><bold>Informations"); }
            public int order() { return 20; }
            public boolean available(Player player) { return hasInformations(player); }
            public void open(Player player) { openInformations(player); }
            public int slot(Player player) {
                if (!informationsEnabled() || !hasInformations(player)) {
                    return -1;
                }
                int slot = getConfig().getInt("informations.slot", 0);
                return slot < 0 || slot > 8 ? 0 : slot;
            }
            public boolean isItem(ItemStack item) { return isInformations(item); }
            public void give(Player player) { giveInformations(player); }
        }, this, org.bukkit.plugin.ServicePriority.Normal);
    }

    /** Objets d'interface de ce serveur (tous les plugins), dans l'ordre d'affichage. */
    private List<fr.kalium.menu.api.InterfaceItem> interfaceItems() {
        List<fr.kalium.menu.api.InterfaceItem> list = new ArrayList<>();
        for (var registration : getServer().getServicesManager().getRegistrations(fr.kalium.menu.api.InterfaceItem.class)) {
            if (registration.getPlugin().isEnabled()) {
                list.add(registration.getProvider());
            }
        }
        list.sort(java.util.Comparator.comparingInt(fr.kalium.menu.api.InterfaceItem::order)
                .thenComparing(item -> item.owner().getName().toLowerCase(Locale.ROOT)));
        return list;
    }

    private boolean isInterfaceItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        for (var item : interfaceItems()) {
            try {
                if (item.isItem(stack)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // plugin en erreur : on continue
            }
        }
        return false;
    }

    /** /menu : l'interface du serveur, directement s'il n'y en a qu'une, sinon un menu de choix. */
    private void openInterfaces(Player player) {
        List<fr.kalium.menu.api.InterfaceItem> list = new ArrayList<>();
        for (var item : interfaceItems()) {
            try {
                if (item.available(player)) {
                    list.add(item);
                }
            } catch (RuntimeException e) {
                getLogger().warning("Objet d'interface " + item.owner().getName() + "/" + item.id() + " : " + e);
            }
        }
        // 2.7.0 : rubrique « Modération » (staff : outils de modération déclarés par les plugins de ce serveur).
        boolean moderation = !moderationSections(player).isEmpty();
        if (list.isEmpty() && !moderation) {
            player.sendMessage(lang.c("menu-cmd.none", "<red>Aucun menu n'est disponible ici."));
            return;
        }
        if (list.size() == 1 && !moderation) {
            list.get(0).open(player);
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (var item : list) {
            buttons.add(gui.button(item.name(), null, item::open));
        }
        if (moderation) {
            buttons.add(gui.button(lang.c("moderation.button", "<red><bold>Modération"),
                    lang.c("moderation.button-tip", "<gray>Signalements et outils du staff de ce serveur."), this::openModeration));
        }
        gui.open(player, lang.c("menu-cmd.title", "<#09add3><bold>Menus"),
                List.of(lang.c("menu-cmd.body", "<gray>Choisis un menu.")), List.of(), buttons, null, 1);
    }

    /** Le joueur est-il en partie (d'apres les plugins de jeu) ? */
    private boolean inGame(Player player) {
        for (var registration : getServer().getServicesManager().getRegistrations(fr.kalium.menu.api.PlayerActivity.class)) {
            try {
                if (registration.getPlugin().isEnabled() && registration.getProvider().inGame(player)) {
                    return true;
                }
            } catch (RuntimeException e) {
                getLogger().warning("Activité de " + registration.getPlugin().getName() + " : " + e);
            }
        }
        return false;
    }

    /** /menu off : retire tous les objets d'interface et n'en donne plus (jusqu'a /menu on). */
    private void hideItems(Player player) {
        if (inGame(player)) {
            player.sendMessage(lang.c("menu-cmd.in-game", "<red>Impossible pendant une partie."));
            return;
        }
        if (itemsHidden(player)) {
            player.sendMessage(lang.c("menu-cmd.already-off", "<yellow>Tes objets de menu sont déjà retirés. <gray>(/menu on pour les remettre)"));
            return;
        }
        hidden.add(player.getUniqueId());
        shown.remove(player.getUniqueId());
        saveHidden();
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isInterfaceItem(inventory.getItem(i))) {
                inventory.setItem(i, null);
            }
        }
        if (isInterfaceItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
        player.sendMessage(lang.c("menu-cmd.off", "<green>Objets de menu retirés de ta barre. <gray>/menu les ouvre toujours ; /menu on pour les remettre."));
    }

    /**
     * /menu on : redonne les objets d'interface a leurs cases. Refuse si une de ces cases contient autre chose, sauf en
     * creatif (Kanvas) : l'objet du joueur est alors deplace dans l'inventaire (ou remplace s'il n'y a plus de place,
     * sans perte en creatif).
     */
    private void showItems(Player player) {
        if (inGame(player)) {
            player.sendMessage(lang.c("menu-cmd.in-game", "<red>Impossible pendant une partie."));
            return;
        }
        if (!itemsHidden(player)) {
            player.sendMessage(lang.c("menu-cmd.already-on", "<yellow>Tes objets de menu sont déjà dans ta barre. <gray>(/menu off pour les retirer)"));
            return;
        }
        PlayerInventory inventory = player.getInventory();
        List<fr.kalium.menu.api.InterfaceItem> toGive = new ArrayList<>();
        java.util.TreeSet<Integer> busy = new java.util.TreeSet<>();
        for (var item : interfaceItems()) {
            int slot;
            try {
                slot = item.slot(player);
            } catch (RuntimeException e) {
                continue;
            }
            if (slot < 0 || slot > 8) {
                continue;
            }
            toGive.add(item);
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().isAir() && !isInterfaceItem(current)) {
                busy.add(slot);
            }
        }
        boolean creative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;
        if (!busy.isEmpty() && !creative) {
            List<String> numbers = new ArrayList<>();
            for (int slot : busy) {
                numbers.add(String.valueOf(slot + 1));
            }
            player.sendMessage(lang.c("menu-cmd.busy", "<red>Libère d'abord <white>la case <slots></white> de ta barre (cases comptées de 1 à 9 depuis la gauche).",
                    "slots", String.join(", ", numbers)));
            return;
        }
        for (int slot : busy) {
            ItemStack occupant = inventory.getItem(slot);
            inventory.setItem(slot, null);
            // Hors de la barre si possible (cases 9 a 35), sinon remplace (creatif : aucune perte).
            for (int i = 9; i < 36 && occupant != null; i++) {
                ItemStack free = inventory.getItem(i);
                if (free == null || free.getType().isAir()) {
                    inventory.setItem(i, occupant);
                    occupant = null;
                }
            }
        }
        hidden.remove(player.getUniqueId());
        shown.add(player.getUniqueId());
        saveHidden();
        for (var item : toGive) {
            try {
                item.give(player);
            } catch (RuntimeException e) {
                getLogger().warning("Objet d'interface " + item.owner().getName() + "/" + item.id() + " : " + e);
            }
        }
        player.sendMessage(lang.c("menu-cmd.on", "<green>Objets de menu remis dans ta barre."));
    }

    /** Objets de KLM_Menu verrouilles (boussole, comparateur). */
    private boolean isLocked(ItemStack item) {
        return isCompass(item) || isInformations(item);
    }

    /**
     * Donne le comparateur a son emplacement (case de gauche par defaut) a qui a quelque chose a y voir (classement sur
     * ce serveur, ou admin) ; le retire aux autres (ex. un admin repasse joueur sur un serveur sans classement).
     */
    private void giveInformations(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean wanted = informationsEnabled() && hasInformations(player) && !itemsHidden(player); // 2.4.0 : /menu off
        boolean present = false;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isInformations(inventory.getItem(i))) {
                if (!wanted || present) {
                    inventory.setItem(i, null);
                }
                present = true;
            }
        }
        if (!wanted || present) {
            return;
        }
        int slot = getConfig().getInt("informations.slot", 0);
        if (slot < 0 || slot > 8) {
            slot = 0;
        }
        ItemStack existing = inventory.getItem(slot);
        if (existing != null && !existing.getType().isAir()) {
            // Comme la boussole : jamais d'objet du joueur ecrase, autre case libre de la barre sinon.
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
        inventory.setItem(slot, createInformations());
    }

    /**
     * 2.2.0 - API pour KLM_Portal (demande de LeKiwi06, 26/09/2026 : les portails se desactivent quand on desactive
     * le bouton dans KLM_Menu). Une destination est active tant qu'elle n'est pas dans disabled-destinations
     * (menu > Parametres) ; un identifiant absent du menu de ce serveur est donc toujours actif.
     */
    public boolean isDestinationEnabled(String id) {
        return id != null && !id.isBlank() && !isDisabled(id);
    }

    /** 2.2.0 - identifiants des destinations de CE serveur (ceux du menu Parametres), ex. pour l'autocompletion. */
    public List<String> destinationIds() {
        List<String> ids = new ArrayList<>();
        for (ServerEntry entry : allDestinations()) {
            ids.add(entry.id());
        }
        return ids;
    }

    /** 2.3.0 - noms des serveurs vers lesquels ce menu envoie (sans les entrees locales), ex. pour les points de chute. */
    public List<String> serverIds() {
        List<String> ids = new ArrayList<>();
        for (ServerEntry entry : menuEntries()) {
            ids.add(entry.id());
        }
        return ids;
    }

    /**
     * 2.2.0 - envoie le joueur vers une destination, exactement comme un clic sur son bouton : entree locale =
     * sa commande sur ce serveur, sinon changement de serveur par le proxy (l'identifiant est le nom du serveur dans
     * velocity.toml). Renvoie false (sans rien faire) si la destination est desactivee.
     */
    public boolean sendToDestination(Player player, String id) {
        if (player == null || !player.isOnline() || !isDestinationEnabled(id)) {
            return false;
        }
        for (ServerEntry entry : localEntries.values()) {
            if (entry.id().equalsIgnoreCase(id)) {
                player.performCommand(entry.command());
                return true;
            }
        }
        if (lobbyRole && id.equalsIgnoreCase(lobbyServer)) {
            player.sendMessage(mm.deserialize(msg("already-in-lobby")));
            return true;
        }
        // Meme orthographe que le bouton du menu (Velocity distingue les majuscules : "Kanvas").
        String target = id;
        for (String key : servers.keySet()) {
            if (key.equalsIgnoreCase(id)) {
                target = key;
            }
        }
        connect(player, target);
        return true;
    }

    private void giveCompass(Player player) {
        if (!getConfig().getBoolean("compass.enabled", true) || itemsHidden(player)) { // 2.4.0 : /menu off
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

    /**
     * 2.4.0 : verrouille pour TOUT LE MONDE, operateurs compris (demande de LeKiwi06, 28/09/2026 : « la boussole
     * n'est pas lock in slot, on peut la drop et la bouger ») ; la permission kaliummenu.bypass n'a plus d'effet.
     */
    private boolean lockEnabled(Player player) {
        return getConfig().getBoolean("compass.lock-item", true);
    }

    /** Apres un clic annule, renvoie l'inventaire au joueur (en creatif, son jeu croit sinon l'objet deplace). */
    private void resync(Player player) {
        getServer().getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }

    // ------------------------------------------------------------------ evenements

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                giveCompass(player);
                giveInformations(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                giveCompass(player);
                giveInformations(player);
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
        if (isInformations(event.getItem())) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            Long previous = lastOpen.put(event.getPlayer().getUniqueId(), now);
            if (previous == null || now - previous >= OPEN_COOLDOWN_MS) {
                openInformations(event.getPlayer());
            }
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
        if (lockEnabled(event.getPlayer()) && isLocked(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            resync(event.getPlayer());
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!lockEnabled(event.getPlayer())) {
            return;
        }
        if (isLocked(event.getMainHandItem()) || isLocked(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    /** Couvre aussi le mode creatif (InventoryCreativeEvent est un InventoryClickEvent). */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !lockEnabled(player)) {
            return;
        }
        boolean locked = isLocked(event.getCurrentItem()) || isLocked(event.getCursor())
                || (event.getClick() == ClickType.NUMBER_KEY && isLocked(player.getInventory().getItem(event.getHotbarButton())))
                || (event.getClick() == ClickType.SWAP_OFFHAND && isLocked(player.getInventory().getItemInOffHand()));
        // Creatif : le jeu du joueur envoie directement le nouveau contenu d'une case ; on refuse de remplacer une case
        // qui contient un objet verrouille (ex. glisse vers l'inventaire creatif pour le detruire).
        if (!locked && event instanceof org.bukkit.event.inventory.InventoryCreativeEvent
                && event.getClickedInventory() == player.getInventory() && event.getSlot() >= 0
                && isLocked(player.getInventory().getItem(event.getSlot()))) {
            locked = true;
        }
        if (locked) {
            event.setCancelled(true);
            resync(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && lockEnabled(player) && isLocked(event.getOldCursor())) {
            event.setCancelled(true);
            resync(player);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isLocked);
    }

    // ------------------------------------------------------------------ utilitaires

    private String msg(String key) {
        String value = getConfig().getString("messages." + key);
        if (value != null) {
            return value;
        }
        return switch (key) {
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
