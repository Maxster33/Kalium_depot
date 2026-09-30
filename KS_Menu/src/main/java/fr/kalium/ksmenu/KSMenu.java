package fr.kalium.ksmenu;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.InterfaceItem;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * KS_Menu (cahier des charges : catégorie 2 « Économie », LeKiwi06, 29/09/2026) : menu du serveur Event.
 *
 * - Étoile du Nether (case 5 de la barre, réglable : hub-item.slot) : clic droit = menu d'Event. Comme les autres objets
 *   de menu d'Event (items-by-default: false de KLM_Menu), elle n'est donnée qu'après « /menu on » et retirée par
 *   « /menu off » ; verrouillée (ni déplacée, ni jetée).
 * - Le menu liste les boutons que les plugins d'Event déclarent avec ajouterBouton(...) (au départ : « Économie »,
 *   KS_Economy). Aussi ouvert par /menu (KLM_Menu) et /event.
 */
public final class KSMenu extends JavaPlugin implements Listener {

    /** Un bouton du menu d'Event. */
    private record Bouton(Plugin plugin, String id, Component nom, Component description, int ordre,
                          Consumer<Player> ouvrir) {
    }

    private static final List<Bouton> BOUTONS = new ArrayList<>();

    private Lang lang;
    private Gui gui;
    private NamespacedKey cle;

    /**
     * Ajoute un bouton au menu d'Event (à appeler dans onEnable d'un plugin d'Event, avec KS_Menu en depend ou
     * softdepend). Un bouton de même plugin et même id est remplacé.
     */
    public static void ajouterBouton(Plugin plugin, String id, Component nom, Component description, int ordre,
                                     Consumer<Player> ouvrir) {
        BOUTONS.removeIf(b -> b.plugin() == plugin && b.id().equals(id));
        BOUTONS.add(new Bouton(plugin, id, nom, description, ordre, ouvrir));
    }

    @Override
    public void onEnable() {
        lang = new Lang(this);
        gui = new Gui(this, lang);
        cle = new NamespacedKey(this, "objet_menu");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getServicesManager().register(InterfaceItem.class, interfaceItem(), this, ServicePriority.Normal);
        getCommand("event").setExecutor(this);
        getServer().getScheduler().runTaskTimer(this, () -> getServer().getOnlinePlayers().forEach(this::verifier),
                40L, 40L);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        BOUTONS.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    // ------------------------------------------------------------------ menu

    /** Menu d'Event : un bouton par plugin déclaré (plugins désactivés ignorés). */
    public void ouvrir(Player joueur) {
        List<ActionButton> boutons = new ArrayList<>();
        BOUTONS.stream()
                .filter(b -> b.plugin().isEnabled())
                .sorted(Comparator.comparingInt(Bouton::ordre))
                .forEach(b -> boutons.add(gui.button(b.nom(), b.description(), p -> b.ouvrir().accept(p))));
        List<Component> corps = boutons.isEmpty()
                ? List.of(lang.c("menu.vide", "<gray>Aucune interface pour l'instant."))
                : List.of();
        gui.open(joueur, lang.c("menu.titre", "<gold><bold>Event"), corps, List.of(), boutons, null, 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ étoile

    private int emplacement() {
        int slot = getConfig().getInt("hub-item.slot", 4);
        return slot < 0 || slot > 8 ? 4 : slot;
    }

    private boolean masque(Player joueur) {
        return getServer().getPluginManager().getPlugin("KLM_Menu") instanceof fr.kalium.menu.KlmMenu klm
                && klm.itemsHidden(joueur);
    }

    /** Étoile déclarée à KLM_Menu pour /menu (ouvrir, /menu on | off). */
    private InterfaceItem interfaceItem() {
        return new InterfaceItem() {
            public Plugin owner() {
                return KSMenu.this;
            }

            public String id() {
                return "event";
            }

            public Component name() {
                return lang.c("objet.nom", "<gold><bold>Event");
            }

            public int order() {
                return 1;
            }

            public boolean available(Player joueur) {
                return true;
            }

            public void open(Player joueur) {
                ouvrir(joueur);
            }

            public int slot(Player joueur) {
                return emplacement();
            }

            public boolean isItem(ItemStack item) {
                return estObjet(item);
            }

            public void give(Player joueur) {
                getServer().getScheduler().runTask(KSMenu.this, () -> verifier(joueur));
            }
        };
    }

    private ItemStack objet() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.c("objet.nom", "<gold><bold>Event")
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(List.of(lang.c("objet.description", "<gray>Clic droit : menu du serveur")
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        meta.getPersistentDataContainer().set(cle, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean estObjet(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cle, PersistentDataType.BYTE);
    }

    /** Une seule étoile, à sa case, si le joueur n'a pas masqué ses objets de menu ; sinon aucune. */
    void verifier(Player joueur) {
        PlayerInventory inv = joueur.getInventory();
        boolean masque = masque(joueur);
        int slot = emplacement();
        int trouve = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            if (!estObjet(inv.getItem(i))) {
                continue;
            }
            if (masque || trouve >= 0) {
                inv.setItem(i, null);
            } else {
                trouve = i;
            }
        }
        if (estObjet(joueur.getItemOnCursor())) {
            joueur.setItemOnCursor(null);
        }
        if (masque) {
            return;
        }
        if (trouve < 0) {
            ItemStack avant = inv.getItem(slot);
            inv.setItem(slot, objet());
            if (avant != null && !avant.getType().isAir()) {
                for (ItemStack reste : inv.addItem(avant).values()) {
                    joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
                }
            }
        } else if (trouve != slot) {
            ItemStack autre = inv.getItem(slot);
            inv.setItem(slot, inv.getItem(trouve));
            inv.setItem(trouve, autre);
        }
    }

    private void verifierPlusTard(Player joueur) {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (joueur.isOnline()) {
                verifier(joueur);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        verifierPlusTard(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        verifierPlusTard(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !estObjet(event.getItem())) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            ouvrir(event.getPlayer());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (estObjet(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (estObjet(event.getMainHandItem()) || estObjet(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    /** Couvre aussi l'inventaire créatif (InventoryCreativeEvent hérite de InventoryClickEvent). */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        if (estObjet(event.getCurrentItem()) || estObjet(event.getCursor())
                || (event.getClick() == ClickType.NUMBER_KEY
                && estObjet(joueur.getInventory().getItem(event.getHotbarButton())))
                || (event.getClick() == ClickType.SWAP_OFFHAND
                && estObjet(joueur.getInventory().getItemInOffHand()))) {
            event.setCancelled(true);
            verifierPlusTard(joueur);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (estObjet(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }
}
