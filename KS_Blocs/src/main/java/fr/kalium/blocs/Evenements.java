package fr.kalium.blocs;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vie des blocs custom côté serveur : chargement des chunks, pose, casse et butin, explosions, pistons, feu, entités ;
 * le bloc musical d'un bloc custom ne joue pas et ne se règle pas. Après une pose ou une casse, les blocs custom voisins
 * sont renvoyés au joueur : son jeu a prédit leur nouvel instrument (bloc musical) et changé leur état.
 */
final class Evenements implements Listener {

    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST};

    private final KSBlocs plugin;
    private final Registre registre;
    private final Affichage affichage;

    Evenements(KSBlocs plugin, Registre registre, Affichage affichage) {
        this.plugin = plugin;
        this.registre = registre;
        this.affichage = affichage;
    }

    private TypeBloc type(Registre.Entree e) {
        return e == null ? null : affichage.types.get(e.type());
    }

    // ------------------------------------------------------------------ chunks

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChargement(ChunkLoadEvent event) {
        Map<Integer, Registre.Entree> m = registre.charger(event.getChunk());
        if (m == null || m.isEmpty()) {
            return;
        }
        // un tick plus tard : écouteurs (lumière...) et renvoi, au cas où le chunk serait parti avant la lecture
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!event.getChunk().isLoaded()) {
                return;
            }
            for (Map.Entry<Integer, Registre.Entree> e : m.entrySet()) {
                int p = e.getKey();
                Block b = event.getChunk().getBlock(Registre.x(p), Registre.y(p), Registre.z(p));
                TypeBloc t = type(e.getValue());
                if (t != null && b.getType() == t.base()) {
                    t.ecouteur().verifie(b, e.getValue().variante());
                    KSBlocs.renvoyer(b);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDechargement(ChunkUnloadEvent event) {
        registre.decharger(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMondeDecharge(WorldUnloadEvent event) {
        if (!event.isCancelled()) {
            registre.oublierMonde(event.getWorld().getUID());
        }
    }

    // ------------------------------------------------------------------ pose et casse

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPose(BlockPlaceEvent event) {
        renvoyerVoisins(event.getPlayer(), event.getBlockPlaced());
        if (event.isCancelled() || !event.canBuild()) {
            return;
        }
        Block b = event.getBlockPlaced();
        TypeBloc t = KSBlocs.type(event.getItemInHand());
        if (t != null && b.getType() == t.base()) {
            KSBlocs.enregistrer(b, t, plugin.variantePosee(t, event.getPlayer(), b));
        }
        apresChangement(b);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCasse(BlockBreakEvent event) {
        renvoyerVoisins(event.getPlayer(), event.getBlock());
        if (event.isCancelled()) {
            return;
        }
        Block b = event.getBlock();
        Registre.Entree e = registre.get(b);
        TypeBloc t = type(e);
        if (t != null && b.getType() == t.base()) {
            Player joueur = event.getPlayer();
            if (event.isDropItems() && joueur.getGameMode() != GameMode.CREATIVE) {
                ItemStack outil = joueur.getInventory().getItemInMainHand();
                for (ItemStack o : t.butin(new TypeBloc.Casse(joueur, outil, b, e.variante()))) {
                    b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), o);
                }
            }
            event.setDropItems(false);
            plugin.supprimer(b, e, t);
        }
        apresChangement(b);
    }

    /** Le jeu du joueur a recalculé l'instrument des blocs musicaux voisins : on lui renvoie les blocs custom. */
    private void renvoyerVoisins(Player joueur, Block b) {
        List<Block> customs = new ArrayList<>();
        for (BlockFace f : FACES) {
            Block v = b.getRelative(f);
            if (registre.get(v) != null) {
                customs.add(v);
            }
        }
        if (!customs.isEmpty()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (Block v : customs) {
                    if (joueur.isOnline() && joueur.getWorld().equals(v.getWorld())) {
                        joueur.sendBlockChange(v.getLocation(), v.getBlockData());
                    }
                }
            });
        }
    }

    private void apresChangement(Block b) {
        plugin.getServer().getScheduler().runTask(plugin, () -> KSBlocs.verifierAutour(b));
    }

    // ------------------------------------------------------------------ explosions, feu, entités

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        exploser(event.blockList(), event.getYield());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent event) {
        exploser(event.blockList(), event.getYield());
    }

    /** Les blocs custom sont retirés de la liste et cassés ici : leur butin à la place de celui du bloc de base. */
    private void exploser(List<Block> blocs, float rendement) {
        for (Iterator<Block> it = blocs.iterator(); it.hasNext(); ) {
            Block b = it.next();
            Registre.Entree e = registre.get(b);
            TypeBloc t = type(e);
            if (t == null || b.getType() != t.base()) {
                continue;
            }
            it.remove();
            if (ThreadLocalRandom.current().nextFloat() < rendement) {
                for (ItemStack o : t.butin(new TypeBloc.Casse(null, null, b, e.variante()))) {
                    b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), o);
                }
            }
            plugin.supprimer(b, e, t);
            b.setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFeu(BlockBurnEvent event) {
        Registre.Entree e = registre.get(event.getBlock());
        if (e != null) {
            plugin.supprimer(event.getBlock(), e, type(e));
        }
    }

    /** Wither, ravageur... : le bloc est cassé ici, avec le butin du bloc custom. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntite(EntityChangeBlockEvent event) {
        Block b = event.getBlock();
        Registre.Entree e = registre.get(b);
        TypeBloc t = type(e);
        if (t == null || b.getType() != t.base() || event.getTo() == t.base()) {
            return;
        }
        event.setCancelled(true);
        for (ItemStack o : t.butin(new TypeBloc.Casse(null, null, b, e.variante()))) {
            b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), o);
        }
        plugin.supprimer(b, e, t);
        b.setType(Material.AIR);
    }

    // ------------------------------------------------------------------ pistons

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        pousser(event.getBlocks(), facePiston(event.getBlock(), event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        pousser(event.getBlocks(), facePiston(event.getBlock(), event.getDirection().getOppositeFace()).getOppositeFace());
    }

    /** Sens de la poussée : la face du piston (plus sûr que getDirection, qui a changé de sens selon les versions). */
    private static BlockFace facePiston(Block piston, BlockFace parDefaut) {
        return piston.getBlockData() instanceof Directional d ? d.getFacing() : parDefaut;
    }

    private void pousser(List<Block> blocs, BlockFace sens) {
        List<Block> de = new ArrayList<>();
        List<Registre.Entree> entrees = new ArrayList<>();
        for (Block b : blocs) {
            Registre.Entree e = registre.get(b);
            if (e != null) {
                de.add(b);
                entrees.add(e);
            }
        }
        if (de.isEmpty()) {
            return;
        }
        // tout enlever avant de tout remettre : une file de blocs custom se suit
        for (int i = 0; i < de.size(); i++) {
            registre.enlever(de.get(i));
            TypeBloc t = type(entrees.get(i));
            if (t != null) {
                t.ecouteur().retire(de.get(i), entrees.get(i).variante());
            }
        }
        List<Block> vers = new ArrayList<>();
        for (int i = 0; i < de.size(); i++) {
            Block v = de.get(i).getRelative(sens);
            registre.mettre(v, entrees.get(i));
            vers.add(v);
        }
        // fin du mouvement du piston (2 ticks) : écouteurs à la nouvelle place
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < vers.size(); i++) {
                Block v = vers.get(i);
                Registre.Entree e = registre.get(v);
                TypeBloc t = type(e);
                if (t != null && e.equals(entrees.get(i)) && v.getType() == t.base()) {
                    t.ecouteur().pose(v, e.variante());
                }
            }
        }, 3L);
    }

    // ------------------------------------------------------------------ bloc musical

    @EventHandler(priority = EventPriority.LOWEST)
    public void onNote(NotePlayEvent event) {
        if (registre.get(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    /** Note d'un vrai bloc musical : pour la couleur de sa particule (Reecriture). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoteJouee(NotePlayEvent event) {
        Block b = event.getBlock();
        int note = estTete(event) ? -1 : event.getNote().getId();
        affichage.note(new Affichage.Pos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ()), note);
    }

    /** Vrai pour un instrument sans note : têtes de mobs et tête de joueur posées sur le bloc. */
    private static boolean estTete(NotePlayEvent event) {
        return switch (event.getInstrument()) {
            case ZOMBIE, SKELETON, CREEPER, DRAGON, WITHER_SKELETON, PIGLIN, CUSTOM_HEAD -> true;
            default -> false;
        };
    }

    /** Clic droit sur un bloc custom : pas de réglage de la note, mais l'objet en main se pose normalement. */
    @EventHandler(priority = EventPriority.LOW)
    public void onClic(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && registre.get(event.getClickedBlock()) != null) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    /** Un objet de bloc custom ne brûle pas dans un four (le bloc musical est un combustible). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombustible(FurnaceBurnEvent event) {
        if (KSBlocs.id(event.getFuel()) != null) {
            event.setCancelled(true);
        }
    }
}
