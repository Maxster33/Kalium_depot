package fr.kalium.core.market;

import fr.kalium.core.KaliumCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Cablage des clics pour les ecrans du commerce : les pages type "coffre" (MarketGuiHolder -
 * Marche/Vente flash/Mes ventes/Recherche, purement navigables, aucun objet n'y transite jamais) et
 * le coffre de recompenses personnel (MarketRewardHolder, ou l'on peut uniquement retirer, jamais
 * deposer). La mise en vente, la recherche (saisie du mot-cle) et les historiques passent
 * integralement par des Dialogs natifs (voir MarketGui) : il n'y a plus d'enclume ni d'autre
 * inventaire virtuel a gerer ici.
 */
public final class MarketGuiListener implements Listener {

    private final KaliumCore plugin;

    public MarketGuiListener(KaliumCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (topHolder instanceof MarketGuiHolder holder) {
            handleGuiClick(event, holder);
        } else if (topHolder instanceof MarketRewardHolder holder) {
            handleRewardClick(event, holder);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        int topSize = event.getView().getTopInventory().getSize();
        if (topHolder instanceof MarketGuiHolder || topHolder instanceof MarketRewardHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    // Sur les pages navigables, rien ne transite jamais ; sur le coffre de
                    // recompenses, un glisser-deposer reviendrait toujours a y deposer un objet.
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MarketRewardHolder holder) {
            List<ItemStack> survivors = new ArrayList<>();
            for (int i = 0; i < MarketService.PAGE_SIZE; i++) {
                ItemStack item = event.getInventory().getItem(i);
                if (item != null && !item.getType().isAir()) {
                    survivors.add(item);
                }
            }
            plugin.market().reconcileRewardPage(holder.owner(), holder.page(), survivors);
        }
    }

    // ------------------------------------------------------------------ pages type "coffre" du commerce

    private void handleGuiClick(InventoryClickEvent event, MarketGuiHolder holder) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < topSize;
        if (clickedTop || event.isShiftClick()) {
            // Ecran purement decoratif/navigable : ni depot ni retrait, jamais (voir MarketGuiHolder).
            event.setCancelled(true);
        }
        if (!clickedTop) {
            return;
        }

        int slot = event.getRawSlot();
        MarketGui gui = plugin.marketGui();

        if (MarketGui.isNavSlot(slot)) {
            if (slot == MarketGui.navPrevSlot()) {
                gui.navigate(player, holder, holder.page() - 1);
            } else if (slot == MarketGui.navNextSlot()) {
                gui.navigate(player, holder, holder.page() + 1);
            } else if (slot == MarketGui.navBackSlot()) {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openHub(player));
            }
            return;
        }
        if (!MarketGui.isContentSlot(slot)) {
            return;
        }

        switch (holder.screen()) {
            case MARCHE, RECHERCHE -> {
                Long id = gui.listingIdAtSlot(holder, slot);
                if (id != null) {
                    gui.buyListing(player, holder, id);
                }
            }
            case VENTE_FLASH -> {
                Long id = gui.flashIdAtSlot(holder, slot);
                if (id != null) {
                    gui.buyFlash(player, holder, id);
                }
            }
            case MES_VENTES -> {
                Long id = gui.ownListingIdAtSlot(player, holder, slot);
                if (id != null) {
                    gui.cancelListing(player, holder, id);
                }
            }
        }
    }

    // ------------------------------------------------------------------ coffre de recompenses

    private void handleRewardClick(InventoryClickEvent event, MarketRewardHolder holder) {
        int topSize = event.getView().getTopInventory().getSize();
        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < topSize;
        if (!clickedTop) {
            if (event.isShiftClick()) {
                // Shift-clic depuis l'inventaire du joueur reviendrait a "ranger" un objet dans le coffre.
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getRawSlot();
        if (MarketGui.isNavSlot(slot)) {
            // Les icones de navigation ne sont pas des objets a recuperer.
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            MarketGui gui = plugin.marketGui();
            if (slot == MarketGui.navPrevSlot()) {
                gui.openRewards(player, holder.page() - 1);
            } else if (slot == MarketGui.navNextSlot()) {
                gui.openRewards(player, holder.page() + 1);
            } else if (slot == MarketGui.navBackSlot()) {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> gui.openHub(player));
            }
            return;
        }

        InventoryAction action = event.getAction();
        if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR
                || action == InventoryAction.HOTBAR_SWAP) {
            // Jamais deposer : ce coffre ne sert qu'a recuperer ses achats/gains, pas de stockage libre.
            event.setCancelled(true);
        }
    }
}
