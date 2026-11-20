package fr.kalium.marteau;

import fr.kalium.marteau.KSMarteau.Niveau;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TrialSpawner;
import org.bukkit.block.Vault;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minage du marteau (cahier § 3) :
 * - un bloc qu'une pioche du même niveau mine (tag mineable/pickaxe, sans les blocs trop durs pour ce niveau) casse
 *   aussi les 8 blocs autour, dans le plan de la face frappée ; parmi eux, seuls ceux qu'une pioche mine sont cassés
 *   (terre, bois, gravier... restent en place) ;
 * - jamais : contenants, spawners (KS_Spawners compris), spawners des épreuves, coffres-forts, blocs incassables ;
 * - accroupi : un seul bloc ; sur un bloc qu'une pioche ne mine pas, un seul bloc, lentement, comme une pioche ;
 * - chaque bloc voisin est cassé par le joueur lui-même (Player#breakBlock) : claims, protections, butin, XP,
 *   Toucher de soie et 1 point de solidité par bloc (Solidité compris) passent par le jeu et les autres plugins ;
 * - Aura de feu : les minerais cassés donnent le résultat du four (fer, or, cuivre bruts, débris antiques), sans l'XP
 *   de la cuisson.
 */
final class Minage implements Listener {

    /** Résultat du four pour le butin des minerais (Aura de feu). */
    private static final Map<Material, Material> CUISSON = new EnumMap<>(Material.class);

    static {
        CUISSON.put(Material.RAW_IRON, Material.IRON_INGOT);
        CUISSON.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        CUISSON.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        CUISSON.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
    }

    /** Dernière face frappée par chaque joueur (clic gauche sur un bloc, Java et Bedrock). */
    private final Map<UUID, Frappe> frappes = new HashMap<>();
    /** Vrai pendant que le marteau casse les blocs voisins (les casses qu'il provoque ne relancent pas un 3 × 3). */
    private boolean enCours;

    private record Frappe(Location bloc, BlockFace face) {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFrappe(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            frappes.put(event.getPlayer().getUniqueId(),
                    new Frappe(event.getClickedBlock().getLocation(), event.getBlockFace()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        frappes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCasse(BlockBreakEvent event) {
        if (enCours) {
            return;
        }
        Player joueur = event.getPlayer();
        Niveau niveau = KSMarteau.niveau(joueur.getInventory().getItemInMainHand());
        Block centre = event.getBlock();
        if (niveau == null || joueur.isSneaking() || !compatible(centre, niveau)) {
            return;
        }
        List<Block> voisins = voisins(centre, face(joueur, centre));
        enCours = true;
        try {
            for (Block bloc : voisins) {
                // Le marteau peut casser en route (ou changer de main) : on s'arrête là.
                if (KSMarteau.niveau(joueur.getInventory().getItemInMainHand()) != niveau) {
                    break;
                }
                if (compatible(bloc, niveau)) {
                    joueur.breakBlock(bloc);
                }
            }
        } finally {
            enCours = false;
        }
    }

    /** Un bloc qu'une pioche de ce niveau mine, et qui n'est pas exclu. */
    static boolean compatible(Block bloc, Niveau niveau) {
        Material type = bloc.getType();
        if (type.isAir() || !Tag.MINEABLE_PICKAXE.isTagged(type) || niveau.mauvaisOutilMateriaux.isTagged(type)
                || type.getHardness() < 0) {
            return false;
        }
        BlockState etat = bloc.getState(false);
        return !(etat instanceof Container || etat instanceof InventoryHolder || etat instanceof CreatureSpawner
                || etat instanceof TrialSpawner || etat instanceof Vault);
    }

    /** Face frappée : celle du dernier clic sur ce bloc, sinon celle visée, sinon selon le regard. */
    private BlockFace face(Player joueur, Block centre) {
        Frappe f = frappes.get(joueur.getUniqueId());
        if (f != null && f.face() != null && f.bloc().equals(centre.getLocation())) {
            return f.face();
        }
        AttributeInstance portee = joueur.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        RayTraceResult vise = joueur.rayTraceBlocks(portee != null ? portee.getValue() + 1 : 6, FluidCollisionMode.NEVER);
        if (vise != null && centre.equals(vise.getHitBlock()) && vise.getHitBlockFace() != null) {
            return vise.getHitBlockFace();
        }
        float pitch = joueur.getLocation().getPitch();
        if (pitch > 45) {
            return BlockFace.UP;
        }
        if (pitch < -45) {
            return BlockFace.DOWN;
        }
        return joueur.getFacing().getOppositeFace();
    }

    /** Les 8 blocs autour du centre, dans le plan perpendiculaire à la face. */
    private static List<Block> voisins(Block centre, BlockFace face) {
        List<Block> liste = new ArrayList<>(8);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                liste.add(switch (face) {
                    case UP, DOWN -> centre.getRelative(a, 0, b);
                    case NORTH, SOUTH -> centre.getRelative(a, b, 0);
                    default -> centre.getRelative(0, b, a);
                });
            }
        }
        return liste;
    }

    // ------------------------------------------------------------------ Aura de feu

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onButin(BlockDropItemEvent event) {
        ItemStack outil = event.getPlayer().getInventory().getItemInMainHand();
        if (KSMarteau.niveau(outil) == null || !outil.containsEnchantment(Enchantment.FIRE_ASPECT)) {
            return;
        }
        Material type = event.getBlockState().getType();
        if (!Tag.IRON_ORES.isTagged(type) && !Tag.GOLD_ORES.isTagged(type) && !Tag.COPPER_ORES.isTagged(type)
                && type != Material.ANCIENT_DEBRIS) {
            return;
        }
        for (Item objet : event.getItems()) {
            ItemStack pile = objet.getItemStack();
            Material cuit = CUISSON.get(pile.getType());
            if (cuit != null) {
                objet.setItemStack(new ItemStack(cuit, pile.getAmount()));
            }
        }
    }
}
