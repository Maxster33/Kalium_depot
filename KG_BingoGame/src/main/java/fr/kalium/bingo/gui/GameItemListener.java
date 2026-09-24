package fr.kalium.bingo.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Clic droit sur l'objet "Objectifs" (GameItems) -&gt; ouvre GameMenu ; empeche UNIQUEMENT de le
 * jeter (PlayerDropItemEvent) - demande explicite de l'utilisateur : contrairement a
 * LobbyItems/LobbyProtectionListener (objet de la salle d'attente totalement verrouille), celui-ci
 * reste deplacable/rangeable librement dans l'inventaire ("de manière à pouvoir libérer la case si
 * on le souhaite mais qu'on ne puisse pas le jeter").
 */
public final class GameItemListener implements Listener {

    private final GameItems gameItems;
    private final GameMenu gameMenu;

    public GameItemListener(GameItems gameItems, GameMenu gameMenu) {
        this.gameItems = gameItems;
        this.gameMenu = gameMenu;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !gameItems.isOurs(event.getItem())) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            gameMenu.open(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (gameItems.isOurs(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
