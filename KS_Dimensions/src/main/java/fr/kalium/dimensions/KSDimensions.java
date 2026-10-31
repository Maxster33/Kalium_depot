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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Dimensions (serveur Event) - demande de Maxster33, 25/09/2026 : "/dimensions (utilisable que par les operateurs),
 * un menu permettant d'activer / desactiver les portails du nether et portail de l'end".
 *
 * Un portail desactive empeche d'ALLER dans sa dimension depuis le monde normal (joueurs et entites) ; le retour
 * reste possible, pour ne jamais bloquer un joueur deja dans le Nether ou l'End. L'etat est garde dans config.yml.
 *
 * 1.1.0 (LeKiwi06, 31/10/2026, Kixster) : entrer dans le Nether par un portail coute 20 points (nether-entree-prix),
 * comme une teleportation : un menu de confirmation s'ouvre dans le portail ; paye, le joueur garde son passage
 * (passes.yml) jusqu'a ce qu'il entre. Le retour est gratuit. Plus de passage gratuit : les perles de l'Ender ne menent
 * plus du monde normal au Nether (ni par un portail, ni par une perle restee dans le Nether), et un vehicule ou une
 * monture qui porte un joueur ne passe plus le portail vers le Nether (il faut descendre). Mobs et objets seuls passent.
 */
public final class KSDimensions extends JavaPlugin implements Listener {

    private static final String NETHER = "nether-portals-enabled";
    private static final String END = "end-portals-enabled";
    private static final String PRIX_NETHER = "nether-entree-prix";

    private Lang lang;
    private Gui gui;
    /** Dernier message "portail desactive" par joueur, pour ne pas le repeter a chaque tick passe dans le portail. */
    private final Map<UUID, Long> lastWarning = new HashMap<>();
    /** Joueurs qui ont paye l'entree du Nether sans l'avoir encore utilisee (gardes dans passes.yml). */
    private final Set<UUID> passes = new HashSet<>();
    /** Derniere ouverture du menu de paiement par joueur, pour ne pas le rouvrir a chaque tick passe dans le portail. */
    private final Map<UUID, Long> dernierMenu = new HashMap<>();
    private File fichierPasses;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichierPasses = new File(getDataFolder(), "passes.yml");
        for (String id : YamlConfiguration.loadConfiguration(fichierPasses).getStringList("passes")) {
            try {
                passes.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // ligne abimee : ignoree
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getServicesManager().register(MenuSection.class, MenuSection.of(this, "dimensions",
                MenuSection.Audience.ADMINS,
                lang.c("catalog.title", "<light_purple>Dimensions"),
                lang.c("catalog.description", "<gray>Activer ou désactiver les portails du Nether et de l'End."),
                (player, back) -> openMenu(player)), this, ServicePriority.Normal);
        lang.saveIfNeeded();
        getLogger().info("Portails du Nether : " + (enabled(NETHER) ? "activés" : "désactivés")
                + " ; portails de l'End : " + (enabled(END) ? "activés" : "désactivés") + ".");
        getLogger().info("Entrée dans le Nether : " + (prixNether() > 0 ? prixNether() + " points" : "gratuite")
                + " ; passages payés pas encore utilisés : " + passes.size() + ".");
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
        Player player = event.getPlayer();
        if (blocked(event.getFrom().getWorld(), nether)) {
            event.setCancelled(true);
            warn(player, lang.c(nether ? "blocked.nether" : "blocked.end",
                    nether ? "<red>Le portail du Nether est désactivé." : "<red>Le portail de l'End est désactivé."));
            return;
        }
        if (nether && fromOverworld(event.getFrom().getWorld()) && prixNether() > 0
                && !passes.contains(player.getUniqueId())) {
            event.setCancelled(true);
            proposerPaiement(player);
        }
    }

    /** Le joueur est vraiment parti vers le Nether : son passage paye est utilise. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortalDone(PlayerPortalEvent event) {
        if (event.getCause() == TeleportCause.NETHER_PORTAL && fromOverworld(event.getFrom().getWorld())
                && passes.remove(event.getPlayer().getUniqueId())) {
            sauverPasses();
            getLogger().info(event.getPlayer().getName() + " entre dans le Nether (passage payé).");
        }
    }

    /** Une perle de l'Ender ne mene jamais du monde normal au Nether (perle passee par un portail ou laissee la-bas). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != TeleportCause.ENDER_PEARL || event.getTo() == null) {
            return;
        }
        World to = event.getTo().getWorld();
        if (fromOverworld(event.getFrom().getWorld()) && to != null && to.getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            warn(event.getPlayer(), pearlMessage());
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
            return;
        }
        if (type != PortalType.NETHER || !fromOverworld(event.getFrom().getWorld())) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof EnderPearl pearl) {
            event.setCancelled(true);
            if (pearl.getShooter() instanceof Player shooter) {
                warn(shooter, pearlMessage());
            }
            return;
        }
        List<Player> riders = new ArrayList<>();
        playersOn(entity, riders);
        if (!riders.isEmpty()) {
            event.setCancelled(true);
            for (Player rider : riders) {
                warn(rider, lang.c("toll.vehicle", "<red>Descends pour passer le portail du Nether."));
            }
        }
    }

    // ------------------------------------------------------------------ peage du Nether

    private long prixNether() {
        return Math.max(0, getConfig().getLong(PRIX_NETHER, 20));
    }

    private static boolean fromOverworld(World from) {
        return from != null && from.getEnvironment() == World.Environment.NORMAL;
    }

    /** Joueurs portes par l'entite (passagers, et passagers des passagers). */
    private static void playersOn(Entity entity, List<Player> into) {
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Player player) {
                into.add(player);
            }
            playersOn(passenger, into);
        }
    }

    private Component pearlMessage() {
        return lang.c("toll.pearl", "<red>Les perles de l'Ender ne mènent pas au Nether : passe le portail à pied.");
    }

    /** Message dans la barre d'action, au plus toutes les 3 s par joueur. */
    private void warn(Player player, Component message) {
        long now = System.currentTimeMillis();
        Long last = lastWarning.get(player.getUniqueId());
        if (last == null || now - last > 3000) {
            lastWarning.put(player.getUniqueId(), now);
            player.sendActionBar(message);
        }
    }

    /** Menu "payer l'entree ?" ouvert dans le portail ; pas rouvert avant 10 s (le portail redemande a chaque tick). */
    private void proposerPaiement(Player player) {
        long now = System.currentTimeMillis();
        Long last = dernierMenu.get(player.getUniqueId());
        if (last != null && now - last < 10_000) {
            return;
        }
        dernierMenu.put(player.getUniqueId(), now);
        if (!getServer().getPluginManager().isPluginEnabled("KS_Economy")) {
            warn(player, lang.c("toll.no-economy", "<red>L'entrée du Nether est payante, mais la banque est indisponible."));
            return;
        }
        gui.confirm(player, lang.c("toll.title", "<dark_red><bold>Nether"),
                lang.c("toll.question", "<white>Entrer dans le Nether coûte <gold><prix> points</gold>, comme une "
                        + "téléportation. <gray>Le retour est gratuit.", "prix", prixNether()),
                this::payer, null);
        lang.saveIfNeeded();
    }

    private void payer(Player player) {
        UUID id = player.getUniqueId();
        long prix = prixNether();
        if (passes.contains(id) || prix == 0) {
            player.sendMessage(lang.c("toll.already", "<green>Ton entrée est déjà payée : passe le portail."));
            return;
        }
        if (!getServer().getPluginManager().isPluginEnabled("KS_Economy")
                || !fr.kalium.economy.KSEconomy.debiter(id, prix)) {
            player.sendMessage(lang.c("toll.funds", "<red>Il te faut <prix> points pour entrer dans le Nether.",
                    "prix", prix));
            return;
        }
        passes.add(id);
        sauverPasses();
        dernierMenu.remove(id);
        getLogger().info(player.getName() + " paie " + prix + " points pour entrer dans le Nether.");
        player.sendMessage(lang.c("toll.paid", "<green><prix> points payés. <gray>Reste dans le portail (ou reviens-y) "
                + "pour entrer dans le Nether.", "prix", prix));
        lang.saveIfNeeded();
    }

    private void sauverPasses() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("passes", passes.stream().map(UUID::toString).toList());
        try {
            yaml.save(fichierPasses);
        } catch (IOException e) {
            getLogger().warning("passes.yml non enregistré : " + e.getMessage());
        }
    }
}
