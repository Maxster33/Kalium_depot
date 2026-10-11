package fr.kalium.claim;

import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * 1.3.0 (LeKiwi06, 11/10/2026) : « rend les bateaux, minecart, etc posable et cassable dans les zones protégés et claims ».
 *
 * Un joueur peut poser un bateau, un radeau ou un wagonnet partout, même là où une protection (région WorldGuard, claim
 * de SimpleClaimSystem) le lui refuse, et casser partout ceux qui n'ont pas d'inventaire. Ces écoutes passent après
 * celles des protections (priorité HIGHEST, et ce plugin est activé après elles) et lèvent leur refus.
 *
 * Gardés sous la protection des lieux (choix de Claude, signalés) : les véhicules à inventaire (bateau à coffre,
 * wagonnet à coffre ou à entonnoir) ne se cassent pas chez les autres, sinon on y volerait leur contenu ; le wagonnet à
 * TNT ne se pose pas dans une zone protégée.
 */
final class Vehicules implements Listener {

    /** Objet qui pose un bateau, un radeau ou un wagonnet (ni TNT, ni bloc de commande). */
    private static boolean objet(Material m) {
        String nom = m.name();
        return nom.endsWith("_BOAT") || nom.endsWith("_RAFT") || m == Material.MINECART || m == Material.CHEST_MINECART
                || m == Material.HOPPER_MINECART || m == Material.FURNACE_MINECART;
    }

    private static boolean vehicule(Entity e) {
        return e instanceof Boat || (e instanceof Minecart && !(e instanceof ExplosiveMinecart)
                && !(e instanceof CommandMinecart) && !(e instanceof SpawnerMinecart));
    }

    private static boolean sansInventaire(Entity e) {
        return vehicule(e) && !(e instanceof InventoryHolder);
    }

    /** L'objet en main est utilisé même si une protection l'a refusé ; le bloc visé, lui, reste protégé. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUtiliser(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && event.getItem() != null && objet(event.getItem().getType())
                && event.useItemInHand() == Event.Result.DENY) {
            event.setUseItemInHand(Event.Result.ALLOW);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPoser(EntityPlaceEvent event) {
        if (event.isCancelled() && event.getPlayer() != null && vehicule(event.getEntity())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFrapper(VehicleDamageEvent event) {
        if (event.isCancelled() && event.getAttacker() instanceof Player && sansInventaire(event.getVehicle())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCasser(VehicleDestroyEvent event) {
        if (event.isCancelled() && event.getAttacker() instanceof Player && sansInventaire(event.getVehicle())) {
            event.setCancelled(false);
        }
    }
}
