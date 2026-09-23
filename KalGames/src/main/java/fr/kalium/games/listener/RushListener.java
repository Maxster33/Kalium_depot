package fr.kalium.games.listener;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.game.RushInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;

/**
 * Regles propres au Rush (1.11.0) : PNJ marchands, lits (point de reapparition, pose, casse, TNT),
 * morts interceptees (pas d'ecran de mort - voir RushInstance). Les regles communes a toutes les
 * parties (blocs suivis/restaures, degats, equipes...) restent dans GameListener / HubListener ; ce
 * listener passe APRES eux (priorite HIGHEST) et ne concerne que les parties Rush.
 */
public final class RushListener implements Listener {

    private final KalGames plugin;
    private final NamespacedKey shopKey;

    public RushListener(KalGames plugin) {
        this.plugin = plugin;
        this.shopKey = RushInstance.shopKey(plugin);
    }

    private RushInstance rushOf(Player player) {
        GameInstance game = plugin.instances().of(player);
        return game instanceof RushInstance rush ? rush : null;
    }

    // ------------------------------------------------------------------ PNJ marchands

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        String shop = entity.getPersistentDataContainer().get(shopKey, PersistentDataType.STRING);
        if (shop == null) {
            return;
        }
        event.setCancelled(true);
        RushInstance rush = rushOf(event.getPlayer());
        if (rush != null) {
            rush.openShop(event.getPlayer(), shop);
        }
    }

    /** Les PNJ ne peuvent pas etre tues (1.12.1, demande explicite) : aucun degat, quelle qu'en soit la source. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(shopKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ lits

    /** Clic droit sur un lit : point de reapparition sur ce lit (au lieu de dormir). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || !Tag.BEDS.isTagged(block.getType())) {
            return;
        }
        RushInstance rush = rushOf(event.getPlayer());
        if (rush == null) {
            return;
        }
        if (event.getPlayer().isSneaking() && event.getItem() != null && event.getItem().getType().isBlock()) {
            return; // accroupi avec un bloc en main : pose d'un bloc contre le lit, comme en vanilla
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        rush.setSpawnOnBed(event.getPlayer(), block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBedPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!Tag.BEDS.isTagged(block.getType())) {
            return;
        }
        RushInstance rush = rushOf(event.getPlayer());
        if (rush == null) {
            return;
        }
        Component refusal = rush.bedPlacementRefusal();
        if (refusal != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.prefix().append(refusal));
            return;
        }
        // Les deux moities du lit sont en place au tick suivant.
        plugin.later(1L, () -> rush.bedPlaced(block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!Tag.BEDS.isTagged(block.getType())) {
            return;
        }
        RushInstance rush = rushOf(event.getPlayer());
        if (rush != null) {
            rush.bedRemoved(block, event.getPlayer());
        }
    }

    /** TNT : casse tous les blocs de l'arene (remise a l'identique en fin de partie) ; les lits touches sont retires. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplode(EntityExplodeEvent event) {
        GameInstance game = plugin.instances().at(event.getLocation());
        if (!(game instanceof RushInstance rush)) {
            return;
        }
        Player source = event.getEntity() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p ? p : null;
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!rush.explosionMayBreak(block)) {
                iterator.remove();
            } else if (Tag.BEDS.isTagged(block.getType())) {
                rush.bedRemoved(block, source);
            }
        }
    }

    // ------------------------------------------------------------------ morts

    /** Degat mortel : mort geree par le Rush (inventaire au sol, spectateur, reapparition) sans ecran de mort. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        RushInstance rush = rushOf(victim);
        if (rush == null || !rush.isAlive(victim.getUniqueId())) {
            return;
        }
        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Player player) {
                attacker = player;
            } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
                attacker = shooter;
            } else if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player igniter) {
                attacker = igniter;
            }
            rush.recordAttack(victim, attacker);
        }
        if (event.getFinalDamage() >= victim.getHealth()) {
            event.setCancelled(true);
            rush.kill(victim, attacker, false);
        }
    }
}
