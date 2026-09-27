package fr.kalium.kvbuildbattle;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Règles du monde du Build Battle, reprises de KV_Plots (ReglesMonde) : créatif + vol pour tous, pas de TNT ni
 * d'explosion, rien ne brûle (la lave et l'eau coulent, mais pas hors de leur zone constructible), redstone désactivée.
 */
final class Regles implements Listener {

    private final KVBuildBattle plugin;

    Regles(KVBuildBattle plugin) {
        this.plugin = plugin;
    }

    private boolean ici(Block b) {
        return b.getWorld().equals(plugin.monde());
    }

    // --- Créatif + vol ---

    /** Un tick plus tard, pour passer après les plugins qui changent le mode de jeu à l'arrivée. */
    private void creatifPlusTard(Player joueur) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> creatif(joueur), 1L);
    }

    private void creatif(Player joueur) {
        if (!joueur.isOnline() || !joueur.getWorld().equals(plugin.monde())) return;
        if (plugin.jeu() != null && plugin.jeu().aventure(joueur)) return; // salle d'attente : mode aventure (0.3.2)
        if (joueur.getGameMode() != GameMode.CREATIVE) joueur.setGameMode(GameMode.CREATIVE);
        joueur.setAllowFlight(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        creatifPlusTard(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        creatifPlusTard(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        creatifPlusTard(e.getPlayer());
    }

    /**
     * Hors opérateurs, on reste en créatif dans le monde du Build Battle (KLM_Menu remet en survie quand un
     * opérateur « repasse joueur »).
     */
    @EventHandler(ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent e) {
        Player joueur = e.getPlayer();
        if (!joueur.getWorld().equals(plugin.monde()) || joueur.isOp() || e.getNewGameMode() == GameMode.CREATIVE) return;
        if (e.getNewGameMode() == GameMode.ADVENTURE && plugin.jeu() != null && plugin.jeu().aventure(joueur)) return;
        e.setCancelled(true);
        creatifPlusTard(joueur); // le vol peut avoir été retiré entre-temps
    }

    // --- Pas de TNT ni d'explosion ---

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (ici(e.getBlock()) && e.getBlock().getType() == Material.TNT) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cLa TNT est interdite dans le Build Battle.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceEntity(EntityPlaceEvent e) {
        if (e.getEntityType() == EntityType.TNT_MINECART && e.getEntity().getWorld().equals(plugin.monde())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent e) {
        if (e.getEntity().getWorld().equals(plugin.monde())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (e.getEntity().getWorld().equals(plugin.monde())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (ici(e.getBlock())) e.setCancelled(true);
    }

    // --- Rien ne brûle ---

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (ici(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (!ici(e.getBlock())) return;
        switch (e.getCause()) {
            case SPREAD, LAVA, LIGHTNING, FIREBALL, EXPLOSION -> e.setCancelled(true);
            default -> { }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (ici(e.getBlock()) && e.getSource().getType() == Material.FIRE) e.setCancelled(true);
    }

    // --- Les liquides ne sortent pas de leur zone constructible ---

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        Block de = e.getBlock(), vers = e.getToBlock();
        if (!ici(de)) return;
        Arene a = plugin.arene();
        if (a.zoneEn(de.getX(), de.getY(), de.getZ()) != a.zoneEn(vers.getX(), vers.getY(), vers.getZ())) e.setCancelled(true);
    }

    // --- Redstone désactivée ---

    @EventHandler
    public void onRedstone(BlockRedstoneEvent e) {
        if (ici(e.getBlock())) e.setNewCurrent(0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonOut(BlockPistonExtendEvent e) {
        if (ici(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonIn(BlockPistonRetractEvent e) {
        if (ici(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        if (ici(e.getBlock())) e.setCancelled(true);
    }
}
