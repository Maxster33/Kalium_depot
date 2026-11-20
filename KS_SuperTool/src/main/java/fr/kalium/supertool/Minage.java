package fr.kalium.supertool;

import fr.kalium.supertool.Outil.Machine;
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
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Casse en 3 × 3 (cahiers § 3 et 6) :
 * - un bloc que l'outil du jeu mine casse aussi les 8 blocs autour, dans le plan de la face frappée ; parmi eux, seuls
 *   ceux que l'outil mine sont cassés ; Foreuse seulement : pas un voisin plus dur que le bloc frappé ;
 * - jamais : contenants, spawners, spawners des épreuves, coffres-forts, blocs incassables ; accroupi : un seul bloc ;
 * - chaque voisin est cassé par le joueur lui-même (Player#breakBlock) : claims, protections, butin, XP, Toucher de
 *   soie et 1 point de solidité par bloc passent par le jeu et les autres plugins ;
 * - Laboureuse, culture mûre (blé, carottes, pommes de terre, betteraves, verrues) : les cultures mûres en 3 × 3 à la
 *   même hauteur sont récoltées puis replantées, une graine (ou carotte, pomme de terre, verrue) reprise dans la
 *   récolte de chacune ; accroupi : seulement celle-ci ;
 * - Foreuse avec Aura de feu : les minerais cassés donnent le résultat du four, sans l'XP de la cuisson.
 */
final class Minage implements Listener {

    private static final Map<Material, Material> CUISSON = new EnumMap<>(Material.class);
    /** Cultures récoltées par la Laboureuse, et ce qu'on replante. */
    static final Map<Material, Material> SEMENCES = new EnumMap<>(Material.class);

    static {
        CUISSON.put(Material.RAW_IRON, Material.IRON_INGOT);
        CUISSON.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        CUISSON.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        CUISSON.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SEMENCES.put(Material.WHEAT, Material.WHEAT_SEEDS);
        SEMENCES.put(Material.CARROTS, Material.CARROT);
        SEMENCES.put(Material.POTATOES, Material.POTATO);
        SEMENCES.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        SEMENCES.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    private final Plugin plugin;
    /** Dernière face frappée par chaque joueur (clic gauche sur un bloc, Java et Bedrock). */
    private final Map<UUID, Frappe> frappes = new HashMap<>();
    /** Cultures que la Laboureuse est en train de récolter (replantées à la chute de leur butin). */
    private final Set<Location> recoltes = new HashSet<>();
    /** Vrai pendant que l'outil casse les blocs voisins (les casses qu'il provoque ne relancent pas un 3 × 3). */
    private boolean enCours;

    private record Frappe(Location bloc, BlockFace face) {
    }

    Minage(Plugin plugin) {
        this.plugin = plugin;
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
        Outil outil = KSSuperTool.outil(joueur.getInventory().getItemInMainHand());
        Block centre = event.getBlock();
        if (outil == null) {
            return;
        }
        if (outil.machine() == Machine.LABOUREUSE && mure(centre)) {
            recolter(joueur, outil, centre);
            return;
        }
        if (joueur.isSneaking() || !compatible(centre, outil)) {
            return;
        }
        // Foreuse : le temps de casse est celui du bloc frappé (calculé par le client) : un voisin plus dur que lui
        // reste en place (réponse de LeKiwi06 du 20/11 : pas d'obsidienne cassée en visant de la pierre).
        float durete = outil.machine().regleDurete ? centre.getType().getHardness() : Float.MAX_VALUE;
        casser(joueur, outil, voisins(centre, face(joueur, centre)), b -> compatible(b, outil)
                && b.getType().getHardness() <= durete);
    }

    private void recolter(Player joueur, Outil outil, Block centre) {
        List<Location> marquees = new ArrayList<>(List.of(centre.getLocation()));
        recoltes.add(centre.getLocation());
        if (!joueur.isSneaking()) {
            casser(joueur, outil, voisins(centre, BlockFace.UP), b -> {
                if (!mure(b)) {
                    return false;
                }
                recoltes.add(b.getLocation());
                marquees.add(b.getLocation());
                return true;
            });
        }
        // Le butin tombe pendant la casse : au tick suivant, une culture sans butin (créatif) n'est plus attendue.
        plugin.getServer().getScheduler().runTask(plugin, () -> marquees.forEach(recoltes::remove));
    }

    private interface Filtre {
        boolean garde(Block b);
    }

    private void casser(Player joueur, Outil outil, List<Block> blocs, Filtre filtre) {
        enCours = true;
        try {
            for (Block bloc : blocs) {
                // L'outil peut casser en route (ou changer de main) : on s'arrête là.
                if (!outil.equals(KSSuperTool.outil(joueur.getInventory().getItemInMainHand()))) {
                    break;
                }
                if (filtre.garde(bloc) && !joueur.breakBlock(bloc)) {
                    recoltes.remove(bloc.getLocation());
                }
            }
        } finally {
            enCours = false;
        }
    }

    /** Une culture que la Laboureuse récolte, à maturité. */
    static boolean mure(Block bloc) {
        return SEMENCES.containsKey(bloc.getType()) && bloc.getBlockData() instanceof Ageable a
                && a.getAge() >= a.getMaximumAge();
    }

    /** Un bloc que l'outil du jeu de cette machine mine, et qui n'est pas exclu. */
    static boolean compatible(Block bloc, Outil outil) {
        Material type = bloc.getType();
        if (type.isAir() || !outil.machine().mine.isTagged(type)
                || outil.niveau().mauvaisOutilMateriaux.isTagged(type) || type.getHardness() < 0) {
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
        return faceVisee(joueur, centre);
    }

    static BlockFace faceVisee(Player joueur, Block centre) {
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
    static List<Block> voisins(Block centre, BlockFace face) {
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

    // ------------------------------------------------------------------ butin : replantation, Aura de feu

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onButin(BlockDropItemEvent event) {
        Location lieu = event.getBlock().getLocation();
        if (recoltes.remove(lieu)) {
            replanter(event, event.getBlockState().getBlockData());
        }
        ItemStack outil = event.getPlayer().getInventory().getItemInMainHand();
        Outil o = KSSuperTool.outil(outil);
        if (o == null || o.machine() != Machine.FOREUSE || !outil.containsEnchantment(Enchantment.FIRE_ASPECT)) {
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

    /** Reprend une semence dans la récolte et replante la culture (âge 0) au tick suivant. */
    private void replanter(BlockDropItemEvent event, BlockData avant) {
        Material culture = avant.getMaterial();
        Material semence = SEMENCES.get(culture);
        if (semence == null) {
            return;
        }
        for (Item objet : new ArrayList<>(event.getItems())) {
            ItemStack pile = objet.getItemStack();
            if (pile.getType() != semence) {
                continue;
            }
            if (pile.getAmount() <= 1) {
                event.getItems().remove(objet);
            } else {
                pile.setAmount(pile.getAmount() - 1);
                objet.setItemStack(pile);
            }
            Block bloc = event.getBlock();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (bloc.getType().isAir()) {
                    bloc.setType(culture);
                }
            });
            return;
        }
    }
}
