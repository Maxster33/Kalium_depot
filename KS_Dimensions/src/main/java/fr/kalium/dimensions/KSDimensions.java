package fr.kalium.dimensions;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_Dimensions (serveur Event) - demande de Maxster33, 25/09/2026 : "/dimensions (utilisable que par les operateurs),
 * un menu permettant d'activer / desactiver les portails du nether et portail de l'end".
 *
 * Un portail desactive empeche d'ALLER dans sa dimension depuis le monde normal (joueurs et entites) ; le retour
 * reste possible, pour ne jamais bloquer un joueur deja dans le Nether ou l'End. L'etat est garde dans config.yml.
 */
public final class KSDimensions extends JavaPlugin implements Listener {

    private static final String NETHER = "nether-portals-enabled";
    private static final String END = "end-portals-enabled";

    private Lang lang;
    private Gui gui;
    /** Dernier message "portail desactive" par joueur, pour ne pas le repeter a chaque tick passe dans le portail. */
    private final Map<UUID, Long> lastWarning = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getServicesManager().register(MenuSection.class, MenuSection.of(this, "dimensions",
                MenuSection.Audience.ADMINS,
                lang.c("catalog.title", "<light_purple>Dimensions"),
                lang.c("catalog.description", "<gray>Activer ou désactiver les portails du Nether et de l'End."),
                (player, back) -> openMenu(player)), this, ServicePriority.Normal);
        lang.saveIfNeeded();
        getLogger().info("Portails du Nether : " + (enabled(NETHER) ? "activés" : "désactivés")
                + " ; portails de l'End : " + (enabled(END) ? "activés" : "désactivés") + ".");
    }

    private boolean enabled(String key) {
        return getConfig().getBoolean(key, true);
    }

    private void setEnabled(String key, boolean value, Player by) {
        getConfig().set(key, value);
        saveConfig();
        getLogger().info((key.equals(NETHER) ? "Portails du Nether " : "Portails de l'End ")
                + (value ? "activés" : "désactivés") + " par " + by.getName() + ".");
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Portails du Nether : " + (enabled(NETHER) ? "activés" : "désactivés")
                    + " ; portails de l'End : " + (enabled(END) ? "activés" : "désactivés")
                    + ". Le menu s'ouvre en jeu (/dimensions).");
            return true;
        }
        if (!player.hasPermission("ksdimensions.admin")) {
            player.sendMessage(lang.c("no-permission", "<red>Cette commande est réservée aux opérateurs."));
            return true;
        }
        openMenu(player);
        return true;
    }

    // ------------------------------------------------------------------ menu

    private Component state(boolean on) {
        return on ? lang.c("menu.on", "<green>activé") : lang.c("menu.off", "<red>désactivé");
    }

    private void openMenu(Player player) {
        if (!player.hasPermission("ksdimensions.admin")) {
            return;
        }
        boolean nether = enabled(NETHER);
        boolean end = enabled(END);
        List<Component> body = new ArrayList<>();
        body.add(lang.c("menu.header", "<gray>Un portail désactivé empêche d'aller dans sa dimension ; "
                + "le retour vers le monde normal reste possible."));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.button(
                lang.c("menu.nether", "<gold>Portail du Nether : <state>", "state", state(nether)),
                lang.c(nether ? "menu.click-off" : "menu.click-on",
                        nether ? "<gray>Clique pour désactiver." : "<gray>Clique pour activer."),
                p -> toggle(p, NETHER)));
        buttons.add(gui.button(
                lang.c("menu.end", "<light_purple>Portail de l'End : <state>", "state", state(end)),
                lang.c(end ? "menu.click-off" : "menu.click-on",
                        end ? "<gray>Clique pour désactiver." : "<gray>Clique pour activer."),
                p -> toggle(p, END)));
        gui.open(player, lang.c("menu.title", "<light_purple><bold>Dimensions"), body, List.of(), buttons, null, 1);
        lang.saveIfNeeded();
    }

    private void toggle(Player player, String key) {
        if (!player.hasPermission("ksdimensions.admin")) {
            return;
        }
        boolean value = !enabled(key);
        setEnabled(key, value, player);
        player.sendMessage(lang.c(key.equals(NETHER) ? "toggled.nether" : "toggled.end",
                key.equals(NETHER) ? "<gray>Portail du Nether : <state>" : "<gray>Portail de l'End : <state>",
                "state", state(value)));
        openMenu(player);
    }

    // ------------------------------------------------------------------ portails

    /** Le portail mene-t-il depuis le monde normal vers une dimension desactivee ? */
    private boolean blocked(World from, boolean nether) {
        return from != null && from.getEnvironment() == World.Environment.NORMAL && !enabled(nether ? NETHER : END);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        boolean nether;
        if (event.getCause() == TeleportCause.NETHER_PORTAL) {
            nether = true;
        } else if (event.getCause() == TeleportCause.END_PORTAL) {
            nether = false;
        } else {
            return;
        }
        if (!blocked(event.getFrom().getWorld(), nether)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = lastWarning.get(player.getUniqueId());
        if (last == null || now - last > 3000) {
            lastWarning.put(player.getUniqueId(), now);
            player.sendActionBar(lang.c(nether ? "blocked.nether" : "blocked.end",
                    nether ? "<red>Le portail du Nether est désactivé." : "<red>Le portail de l'End est désactivé."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        PortalType type = event.getPortalType();
        if (type != PortalType.NETHER && type != PortalType.ENDER) {
            return;
        }
        if (blocked(event.getFrom().getWorld(), type == PortalType.NETHER)) {
            event.setCancelled(true);
        }
    }
}
