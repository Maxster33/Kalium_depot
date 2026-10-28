package fr.kalium.alcools;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Alcool neutre comme combustible (cahier, partie 3.2.1) : four, fumoir, haut fourneau.
 *
 * L'alcool neutre porte le composant de combustible du jeu (voir {@link Alcools}) : clic, Maj+clic, glisser et entonnoirs
 * le mettent dans la case du combustible comme du charbon. Ici : 50 cuissons garanties (10 000 ticks au four, 5 000 au
 * fumoir et au haut fourneau), puis la fiole vide reste dans la case, comme le seau vide de la lave. Un entonnoir sous
 * le four l'aspire (le jeu ne laisse sortir par le bas que les seaux : le plugin la déplace lui-même, et réessaie
 * tant que l'entonnoir est plein ou bloqué par la redstone).
 */
final class Carburant implements Listener {

    private static final int FOUR = 10000;
    private static final int RAPIDE = 5000;

    private final KSAlcools plugin;

    /** Fours dont la case contient une fiole vide en attente d'un entonnoir (oubliés au redémarrage). */
    private final Set<Block> fioles = new HashSet<>();

    Carburant(KSAlcools plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::entonnoirs, 8L, 8L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBruler(FurnaceBurnEvent event) {
        if (!Alcools.estNeutre(event.getFuel())) {
            return;
        }
        event.setBurnTime(event.getBlock().getType() == Material.FURNACE ? FOUR : RAPIDE);
        event.setBurning(true);
        // La fiole remplace l'alcool ci-dessous (le jeu n'a pas de fiole à rendre pour une potion).
        event.setConsumeFuel(false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrule(FurnaceBurnEvent event) {
        if (!Alcools.estNeutre(event.getFuel()) || !event.isBurning() || event.getBurnTime() <= 0) {
            return;
        }
        BlockState etat = event.getBlock().getState(false);
        if (!(etat instanceof Furnace four)) {
            return;
        }
        four.getInventory().setFuel(new ItemStack(Material.GLASS_BOTTLE));
        Block bloc = event.getBlock();
        // Au tick suivant : le four a fini son tour.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!versEntonnoir(bloc)) {
                fioles.add(bloc);
            }
        });
    }

    /** Tous les 8 ticks (rythme d'un entonnoir) : fioles en attente. */
    private void entonnoirs() {
        fioles.removeIf(bloc -> !bloc.getWorld().isChunkLoaded(bloc.getX() >> 4, bloc.getZ() >> 4)
                || fiole(bloc) == null || versEntonnoir(bloc));
    }

    /** Inventaire du four si sa case du combustible contient une fiole vide, sinon null. */
    private static FurnaceInventory fiole(Block bloc) {
        if (!(bloc.getState(false) instanceof Furnace four)) {
            return null;
        }
        ItemStack combustible = four.getInventory().getFuel();
        return combustible != null && combustible.getType() == Material.GLASS_BOTTLE && !combustible.hasItemMeta()
                ? four.getInventory() : null;
    }

    /** La fiole vide passe dans l'entonnoir sous le four (activé, avec de la place). Vrai si elle n'est plus là. */
    private static boolean versEntonnoir(Block bloc) {
        FurnaceInventory four = fiole(bloc);
        if (four == null) {
            return true;
        }
        Block dessous = bloc.getRelative(BlockFace.DOWN);
        if (dessous.getType() != Material.HOPPER
                || !(dessous.getBlockData() instanceof org.bukkit.block.data.type.Hopper reglage) || !reglage.isEnabled()
                || !(dessous.getState(false) instanceof Hopper entonnoir)) {
            return false;
        }
        ItemStack fiole = four.getFuel();
        if (!entonnoir.getInventory().addItem(fiole.clone()).isEmpty()) {
            return false;
        }
        four.setFuel(null);
        return true;
    }
}
