package fr.kalium.kgmenu;

import fr.kalium.kgmenu.api.MenuProvider;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * KG_Menu (1.0.0) : le menu du serveur kal-games (demande de LeKiwi06, 24/09/2026). Hierarchie des interfaces :
 * KLM_Menu (tous les serveurs : navigation, boussole, catalogue) -&gt; menu de chaque serveur (ici KG_Menu) -&gt;
 * interfaces des jeux et fonctions, gerees par leurs propres plugins.
 *
 * - Decouvre au demarrage les fournisseurs (MenuProvider) declares par les plugins de kal-games (KalGames, KG_Bingo,
 *   KG_ScoreBoards...) et affiche leurs boutons : accueil (jeux, puis autres boutons) et accueil "Parametres".
 * - Objet du hub (etoile "Mini-jeux", repris de KalGames) : ouvre le menu de la partie en cours si un jeu s'en charge,
 *   sinon l'accueil. Donne par KalGames quand il prepare le hub (giveHubItem), verrouille ici.
 * - Apparait dans le catalogue de KLM_Menu sous une seule entree "Kal-Games" (+ "Kal-Games : paramètres" pour les
 *   admins).
 */
public final class KgMenu extends JavaPlugin implements Listener {

    private static final long COOLDOWN_MS = 400L;

    private Lang lang;
    private Gui gui;
    private NamespacedKey itemKey;
    private final List<MenuProvider> providers = new CopyOnWriteArrayList<>();
    private final Map<UUID, Long> lastOpen = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        itemKey = new NamespacedKey(this, "hub_item");
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getServicesManager().register(MenuSection.class,
                MenuSection.of(this, "kalgames", MenuSection.Audience.PLAYERS,
                        t("klm.home", "<gold><bold>Kal-Games"), t("klm.home-tip", "<gray>Mini-jeux, Bingo, classements."),
                        (p, back) -> openHome(p, back)),
                this, ServicePriority.Normal);
        getServer().getServicesManager().register(MenuSection.class, new MenuSection() {
            @Override
            public String id() {
                return "settings";
            }

            @Override
            public org.bukkit.plugin.Plugin owner() {
                return KgMenu.this;
            }

            @Override
            public Component title() {
                return t("klm.settings", "<light_purple>Kal-Games : paramètres");
            }

            @Override
            public Component description() {
                return t("klm.settings-tip", "<gray>Réglages des jeux et fonctions de Kal-Games.");
            }

            @Override
            public Audience audience() {
                return Audience.ADMINS;
            }

            @Override
            public boolean visibleTo(Player player) {
                return !settingsFor(player).isEmpty();
            }

            @Override
            public void open(Player player, Consumer<Player> back) {
                openSettings(player, back);
            }
        }, this, ServicePriority.Normal);

        // Decouverte une fois tous les plugins actives, puis a chaque ajout / retrait.
        getServer().getScheduler().runTask(this, () -> refreshProviders(true));
        getLogger().info("KG_Menu actif.");
    }

    @Override
    public void onDisable() {
        if (lang != null) {
            lang.saveIfNeeded();
        }
    }

    Component t(String key, String def, Object... pairs) {
        return lang.c(key, def, pairs);
    }

    // ------------------------------------------------------------------ decouverte des fournisseurs

    private void refreshProviders(boolean log) {
        List<MenuProvider> found = new ArrayList<>();
        for (var registration : getServer().getServicesManager().getRegistrations(MenuProvider.class)) {
            if (registration.getPlugin().isEnabled()) {
                found.add(registration.getProvider());
            }
        }
        found.sort(Comparator.comparingInt(MenuProvider::order).thenComparing(p -> p.owner().getName()));
        providers.clear();
        providers.addAll(found);
        lang.saveIfNeeded();
        if (log) {
            List<String> names = new ArrayList<>();
            for (MenuProvider provider : found) {
                names.add(provider.owner().getName());
            }
            getLogger().info(found.size() + " fournisseur(s) d'interface trouvé(s)" + (names.isEmpty() ? "." : " : " + String.join(", ", names) + "."));
        }
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == MenuProvider.class) {
            getServer().getScheduler().runTask(this, () -> refreshProviders(false));
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == MenuProvider.class) {
            getServer().getScheduler().runTask(this, () -> refreshProviders(false));
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != this) {
            providers.removeIf(provider -> provider.owner() == event.getPlugin());
        }
    }

    private interface Lister {
        List<MenuProvider.Entry> list(MenuProvider provider, Player player);
    }

    private List<MenuProvider.Entry> collect(Player player, Lister lister) {
        List<MenuProvider.Entry> all = new ArrayList<>();
        for (MenuProvider provider : providers) {
            try {
                if (provider.owner().isEnabled()) {
                    all.addAll(lister.list(provider, player));
                }
            } catch (RuntimeException e) {
                getLogger().warning("Menu de " + provider.owner().getName() + " indisponible : " + e);
            }
        }
        return all;
    }

    private List<MenuProvider.Entry> settingsFor(Player player) {
        return collect(player, MenuProvider::settings);
    }

    private ActionButton button(MenuProvider.Entry entry, Consumer<Player> back) {
        return gui.button(entry.label(), entry.tooltip(), p -> {
            try {
                entry.open().accept(p, back);
            } catch (RuntimeException e) {
                getLogger().warning("Ouverture de " + entry.id() + " impossible : " + e);
            }
        });
    }

    // ------------------------------------------------------------------ menus

    /** Objet du hub, /kalgames menu : menu de la partie en cours si un jeu s'en charge, sinon l'accueil. */
    public void openFor(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastOpen.put(player.getUniqueId(), now);
        if (previous != null && now - previous < COOLDOWN_MS) {
            return;
        }
        for (MenuProvider provider : providers) {
            try {
                if (provider.owner().isEnabled() && provider.openCurrent(player)) {
                    return;
                }
            } catch (RuntimeException e) {
                getLogger().warning("Menu de partie de " + provider.owner().getName() + " : " + e);
            }
        }
        openHome(player, null);
    }

    /** Accueil : les jeux, puis les autres boutons, puis "Parametres" (si le joueur a des reglages). */
    public void openHome(Player player, Consumer<Player> back) {
        Consumer<Player> here = p -> openHome(p, back);
        List<ActionButton> buttons = new ArrayList<>();
        List<MenuProvider.Entry> games = collect(player, MenuProvider::games);
        for (MenuProvider.Entry entry : games) {
            buttons.add(button(entry, here));
        }
        for (MenuProvider.Entry entry : collect(player, MenuProvider::extras)) {
            buttons.add(button(entry, here));
        }
        if (!settingsFor(player).isEmpty()) {
            buttons.add(gui.button(t("menu.settings", "<light_purple><bold>Paramètres"),
                    t("menu.settings-tip", "<gray>Réservé aux modérateurs : jeux, arènes, kits, hub..."), p -> openSettings(p, here)));
        }
        if (back != null) {
            buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, back::accept));
        }
        List<Component> body = List.of(games.isEmpty()
                ? t("menu.games-empty", "<gray>Aucun mini-jeu n'est disponible pour le moment.")
                : t("menu.games-intro", "<gray>Choisissez un mini-jeu."));
        gui.open(player, t("menu.games-title", "<gold><bold>Mini-jeux Kal-Games"), body, List.of(), buttons, null, 1);
    }

    /** Accueil "Parametres" : les reglages de chaque plugin que ce joueur a le droit de voir. */
    public void openSettings(Player player, Consumer<Player> back) {
        Consumer<Player> here = p -> openSettings(p, back);
        List<ActionButton> buttons = new ArrayList<>();
        for (MenuProvider.Entry entry : settingsFor(player)) {
            buttons.add(button(entry, here));
        }
        Consumer<Player> exit = back != null ? back : p -> openHome(p, null);
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, exit::accept));
        gui.open(player, t("admin.home-title", "<light_purple><bold>Paramètres Kal-Games"),
                List.of(buttons.size() > 1 ? t("settings.body", "<gray>Choisissez ce que vous voulez régler.")
                        : t("settings.none", "<gray>Aucun réglage disponible.")),
                List.of(), buttons, null, 1);
    }

    // ------------------------------------------------------------------ objet du hub

    /** Objet du hub (repris de KalGames : etoile "Mini-jeux", emplacement 4 par defaut). */
    public ItemStack hubItem() {
        Material material = Material.matchMaterial(getConfig().getString("hub-item.material", "NETHER_STAR"));
        if (material == null || material.isAir() || !material.isItem()) {
            material = Material.NETHER_STAR;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(t("item.games.name", "<gold><bold>Mini-jeux").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(List.of(
                t("item.games.lore0", "<gray>Clic droit pour choisir").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                t("item.games.lore1", "<gray>un mini-jeu.").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        lang.saveIfNeeded();
        return item;
    }

    /** Donne l'objet du hub a son emplacement (appele par KalGames quand il prepare le hub). */
    public void giveHubItem(Player player) {
        int slot = getConfig().getInt("hub-item.slot", 4);
        if (slot < 0 || slot > 8) {
            slot = 4;
        }
        player.getInventory().setItem(slot, hubItem());
    }

    public boolean isHubItem(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    private boolean locked(Player player) {
        return !player.hasPermission("kgmenu.bypass");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isHubItem(event.getItem())) {
            return;
        }
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            openFor(event.getPlayer());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (locked(event.getPlayer()) && isHubItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (locked(event.getPlayer()) && (isHubItem(event.getMainHandItem()) || isHubItem(event.getOffHandItem()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !locked(player)) {
            return;
        }
        if (isHubItem(event.getCurrentItem()) || isHubItem(event.getCursor())
                || (event.getClick() == ClickType.NUMBER_KEY && isHubItem(player.getInventory().getItem(event.getHotbarButton())))
                || (event.getClick() == ClickType.SWAP_OFFHAND && isHubItem(player.getInventory().getItemInOffHand()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && locked(player) && isHubItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isHubItem);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastOpen.remove(event.getPlayer().getUniqueId());
    }
}
