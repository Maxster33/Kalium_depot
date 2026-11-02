package fr.kalium.blocs;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;

import java.util.List;

/**
 * 0.2.0 (cultures de KS_Cultures) : blocs custom dont la base est une plante, la canne à sucre (cultures sur terre) ou
 * le varech (riz). Le serveur garde ces vrais blocs, mais c'est le plugin du type qui décide de leur vie :
 *
 * <ul>
 *   <li>règle de survie de la base (canne à sucre sans eau à côté, varech sans appui...) : le jeu voudrait casser le
 *       bloc, KS_Blocs l'en empêche et prévient l'écouteur ({@link EcouteurBloc#verifie}) au tick suivant ;</li>
 *   <li>pas de pousse vanilla : ni nouvelle canne à sucre au-dessus, ni varech qui monte, ni poudre d'os du jeu
 *       (distributeur) ;</li>
 *   <li>pas de vraie canne à sucre ni de vrai varech posés sur un bloc custom de la même base ;</li>
 *   <li>pistons : la canne à sucre et le varech sont cassés par un piston (butin vanilla) ; un piston qui en pousserait
 *       un ne bouge pas ;</li>
 *   <li>éponge : elle ne retire pas un varech custom.</li>
 * </ul>
 *
 * L'eau ne casse ni la canne à sucre ni le varech (vérifié dans le code du jeu : FlowingFluid).
 */
final class Plantes implements Listener {

    private static final List<Material> BASES = List.of(Material.SUGAR_CANE, Material.KELP);

    private final KSBlocs plugin;
    private final Registre registre;
    private final Affichage affichage;

    Plantes(KSBlocs plugin, Registre registre, Affichage affichage) {
        this.plugin = plugin;
        this.registre = registre;
        this.affichage = affichage;
    }

    /** Le type du bloc custom à cette place si sa base est une plante et que le serveur l'a bien, sinon null. */
    private TypeBloc plante(Block b) {
        Registre.Entree e = registre.get(b);
        TypeBloc t = e == null ? null : affichage.types.get(e.type());
        return t != null && BASES.contains(t.base()) && b.getType() == t.base() ? t : null;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDestruction(BlockDestroyEvent event) {
        Block b = event.getBlock();
        if (plante(b) == null) {
            return;
        }
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Registre.Entree e = registre.get(b);
            TypeBloc t = plante(b);
            if (t != null) {
                t.ecouteur().verifie(b, e.variante());
            }
        });
    }

    /** Canne à sucre : la pousse crée le bloc du dessus. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPousse(BlockGrowEvent event) {
        if (plante(event.getBlock()) != null || plante(event.getBlock().getRelative(BlockFace.DOWN)) != null) {
            event.setCancelled(true);
        }
    }

    /** Varech : la pousse passe par un événement de propagation, de la tête vers le bloc du dessus. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPropagation(BlockSpreadEvent event) {
        if (plante(event.getSource()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPoudreDOs(BlockFertilizeEvent event) {
        if (plante(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPose(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        TypeBloc dessous = plante(b.getRelative(BlockFace.DOWN));
        // (une place déjà custom : essai de pose d'un plugin, qui demande aux protections si le joueur peut construire)
        if (dessous != null && b.getType() == dessous.base() && KSBlocs.type(event.getItemInHand()) == null
                && registre.get(b) == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> plante(b) != null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> plante(b) != null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEponge(SpongeAbsorbEvent event) {
        event.getBlocks().removeIf((BlockState s) -> plante(s.getBlock()) != null);
    }
}
