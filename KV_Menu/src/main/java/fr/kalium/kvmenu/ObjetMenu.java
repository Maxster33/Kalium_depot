package fr.kalium.kvmenu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.kvplots.api.KanvasPlots.PlotInfo;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Objet du menu de Kanvas (étoile du Nether, emplacement 4 par défaut) : clic droit = menu de Kanvas. Donné à l'arrivée
 * dans le monde des plots, verrouillé (pas de déplacement, pas de jet), remis à sa place toutes les 2 secondes : en
 * créatif, le client peut modifier l'inventaire sans passer par les clics habituels.
 * Pendant un vote (KV_Plots), il laisse sa place aux terracottas : l'inventaire est mis de côté puis rendu par KV_Plots.
 */
final class ObjetMenu implements Listener {

    private final KvMenu plugin;
    private final Lang lang;
    private final NamespacedKey cle;
    private final Consumer<Player> ouvrir;

    ObjetMenu(KvMenu plugin, Lang lang, Consumer<Player> ouvrir) {
        this.plugin = plugin;
        this.lang = lang;
        this.ouvrir = ouvrir;
        this.cle = new NamespacedKey(plugin, "objet_menu");
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) verifier(p);
        }, 40L, 40L);
    }

    private int emplacement() {
        int slot = plugin.getConfig().getInt("hub-item.slot", 4);
        return slot < 0 || slot > 8 ? 4 : slot;
    }

    private boolean ici(Player p) {
        return p.getWorld().equals(plugin.plots().monde());
    }

    private ItemStack objet() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("hub-item.material", "NETHER_STAR"));
        if (m == null || m.isAir() || !m.isItem()) m = Material.NETHER_STAR;
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.c("item.name", "<gold><bold>Kanvas").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(List.of(lang.c("item.lore", "<gray>Clic droit : plots, visites...")
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        meta.getPersistentDataContainer().set(cle, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean estObjet(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cle, PersistentDataType.BYTE);
    }

    /**
     * Sur un plot en travaux dont le joueur est créateur ou éditeur, l'étoile et la boussole de KLM_Menu sont rangées
     * dans la dernière rangée de l'inventaire (hors barre d'objets), pour construire plus facilement ; elles reviennent
     * à leur place en sortant du plot (demande de LeKiwi06, 26/09/2026). Rangées, pas retirées : KLM_Menu 2.0.0 ne
     * redonne la boussole qu'à l'arrivée sur le serveur.
     */
    private static final int RANGEMENT_ETOILE = 34, RANGEMENT_BOUSSOLE = 35;
    private static final NamespacedKey BOUSSOLE = new NamespacedKey("klm_menu", "menu_compass");
    private static final NamespacedKey BOUSSOLE_ANCIENNE = new NamespacedKey("kaliummenu", "menu_compass");

    /** Dernier état connu (sur son plot en travaux ou non), pour ne revérifier qu'au changement. */
    private final Map<UUID, Boolean> surChantier = new HashMap<>();

    private boolean surSonChantier(Player p) {
        PlotInfo plot = plugin.plots().plotEn(p.getLocation());
        UUID u = p.getUniqueId();
        return plot != null && !plot.valide() && (plot.createur().equals(u) || plot.editeurs().contains(u));
    }

    private static boolean estBoussole(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(BOUSSOLE, PersistentDataType.BYTE) || pdc.has(BOUSSOLE_ANCIENNE, PersistentDataType.BYTE);
    }

    /** Emplacement de la boussole dans la configuration de KLM_Menu (8 par défaut). */
    private int emplacementBoussole() {
        int slot = plugin.getServer().getPluginManager().getPlugin("KLM_Menu") instanceof JavaPlugin klm
                ? klm.getConfig().getInt("compass.slot", 8) : 8;
        return slot < 0 || slot > 8 ? 8 : slot;
    }

    private static void echanger(PlayerInventory inv, int a, int b) {
        ItemStack x = inv.getItem(a);
        inv.setItem(a, inv.getItem(b));
        inv.setItem(b, x);
    }

    /**
     * Dans le monde des plots : une seule étoile, à son emplacement (ou rangée sur son plot en travaux). Ailleurs :
     * l'étoile est retirée.
     */
    void verifier(Player p) {
        if (plugin.plots().enVote(p)) return; // inventaire remplacé par les terracottas (KV_Plots)
        PlayerInventory inv = p.getInventory();
        boolean ici = ici(p);
        boolean chantier = ici && surSonChantier(p);
        surChantier.put(p.getUniqueId(), chantier);
        int slot = chantier ? RANGEMENT_ETOILE : emplacement();
        int trouve = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            if (!estObjet(inv.getItem(i))) continue;
            if (!ici || trouve >= 0) {
                inv.setItem(i, null);
            } else {
                trouve = i;
            }
        }
        if (estObjet(p.getItemOnCursor())) p.setItemOnCursor(null);
        if (!ici) return;
        if (trouve < 0) {
            ItemStack avant = inv.getItem(slot);
            inv.setItem(slot, objet());
            if (avant != null && !avant.getType().isAir()) {
                for (ItemStack reste : inv.addItem(avant).values()) p.getWorld().dropItemNaturally(p.getLocation(), reste);
            }
        } else if (trouve != slot) {
            echanger(inv, trouve, slot);
        }
        for (int i = 0; i < 36; i++) {
            if (!estBoussole(inv.getItem(i))) continue;
            if (chantier && i < 9) echanger(inv, i, RANGEMENT_BOUSSOLE);
            else if (!chantier && i >= 9) echanger(inv, i, emplacementBoussole());
            break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        Player p = e.getPlayer();
        if (!ici(p)) return;
        boolean chantier = surSonChantier(p);
        if (!Boolean.valueOf(chantier).equals(surChantier.get(p.getUniqueId()))) verifier(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        surChantier.remove(e.getPlayer().getUniqueId());
    }

    private void verifierPlusTard(Player p) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) verifier(p);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        verifierPlusTard(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        verifierPlusTard(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        verifierPlusTard(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !estObjet(e.getItem())) return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            ouvrir.accept(e.getPlayer());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (estObjet(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (estObjet(e.getMainHandItem()) || estObjet(e.getOffHandItem())) e.setCancelled(true);
    }

    /** Couvre aussi l'inventaire créatif (InventoryCreativeEvent hérite de InventoryClickEvent). */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (estObjet(e.getCurrentItem()) || estObjet(e.getCursor())
                || (e.getClick() == ClickType.NUMBER_KEY && estObjet(p.getInventory().getItem(e.getHotbarButton())))
                || (e.getClick() == ClickType.SWAP_OFFHAND && estObjet(p.getInventory().getItemInOffHand()))) {
            e.setCancelled(true);
            verifierPlusTard(p);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (estObjet(e.getOldCursor())) e.setCancelled(true);
    }
}
