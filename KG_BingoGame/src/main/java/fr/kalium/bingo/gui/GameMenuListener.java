package fr.kalium.bingo.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Verrouille le menu de grille (GameMenu, inventaire) : tout clic/glisser-deposer y est annule
 * (menu en LECTURE SEULE, aucun objet ne doit pouvoir en sortir ni y entrer), a l'exception des
 * deux objets de controle de la derniere rangee - voir GameMenu.CLOSE_SLOT/ABANDON_SLOT.
 */
public final class GameMenuListener implements Listener {

    private final AbandonConfirmMenu abandonConfirmMenu;
    private final fr.kalium.bingo.game.DrawVoteService drawVotes;

    public GameMenuListener(AbandonConfirmMenu abandonConfirmMenu, fr.kalium.bingo.game.DrawVoteService drawVotes) {
        this.abandonConfirmMenu = abandonConfirmMenu;
        this.drawVotes = drawVotes;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GameMenu.GameMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return; // clic dans l'inventaire du joueur (bas de l'ecran) : deja annule ci-dessus, rien d'autre a faire
        }
        int slot = event.getSlot();
        if (slot == GameMenu.CLOSE_SLOT) {
            event.getWhoClicked().closeInventory();
        } else if (slot == GameMenu.ABANDON_SLOT && event.getWhoClicked() instanceof Player player) {
            player.closeInventory();
            abandonConfirmMenu.open(player);
        } else if (slot == GameMenu.DRAW_SLOT && event.getWhoClicked() instanceof Player player) {
            player.closeInventory();
            drawVotes.propose(player); // 0.3.0
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GameMenu.GameMenuHolder) {
            event.setCancelled(true);
        }
    }
}
