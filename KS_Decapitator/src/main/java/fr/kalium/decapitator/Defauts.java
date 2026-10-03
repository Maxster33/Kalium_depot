package fr.kalium.decapitator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.1.0 (LeKiwi06, 03/10/2026) : la tête de wither squelette tombe **sale, endommagée et désactivée** (description) ;
 * chaque défaut se répare par un craft de KS_Crafts, dans n'importe quel ordre ; réparée de ses 3 défauts, elle devient
 * un vrai crâne de wither squelette (qui invoque le Wither). Abîmée, elle se pose comme décoration ; ses défauts restants
 * sont gardés quand on la pose puis la casse (aussi par une explosion ou un piston).
 */
final class Defauts implements Listener {

    /** Clé de la tête concernée (tetes.txt). */
    static final String CLE_WITHER_SQUELETTE = "wither_skeleton";
    /** Les défauts, dans l'ordre d'affichage : id -> texte. */
    static final Map<String, String> NOMS = new java.util.LinkedHashMap<>();

    static {
        NOMS.put("sale", "Sale");
        NOMS.put("endommagee", "Endommagée");
        NOMS.put("desactivee", "Désactivée");
    }

    private static NamespacedKey cle;
    /** Têtes cassées il y a moins de 2 s : bloc -> défauts (pour la tête qui va apparaître au sol). */
    private static final Map<String, Etat> casses = new HashMap<>();

    private record Etat(Set<String> defauts, long date) {
    }

    Defauts(KSDecapitator plugin) {
        cle = new NamespacedKey(plugin, "defauts");
    }

    static boolean concernee(String cleTete) {
        return CLE_WITHER_SQUELETTE.equals(cleTete);
    }

    /** Écrit les défauts (et leur description) sur une tête. */
    static void appliquer(ItemStack item, Set<String> defauts) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(cle, PersistentDataType.STRING, String.join(",", defauts));
        List<Component> lore = new ArrayList<>();
        for (Map.Entry<String, String> e : NOMS.entrySet()) {
            if (defauts.contains(e.getKey())) {
                lore.add(Component.text(e.getValue(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            }
        }
        if (!defauts.isEmpty()) {
            lore.add(Component.text("À réparer à l'établi pour invoquer le Wither", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    /** Défauts d'une tête (vide si aucun ou si ce n'est pas une tête concernée). */
    static Set<String> lire(ItemStack item) {
        if (item == null || !item.hasItemMeta() || cle == null) {
            return new LinkedHashSet<>();
        }
        String s = item.getItemMeta().getPersistentDataContainer().get(cle, PersistentDataType.STRING);
        Set<String> r = new LinkedHashSet<>();
        if (s != null) {
            for (String d : s.split(",")) {
                if (NOMS.containsKey(d)) {
                    r.add(d);
                }
            }
        }
        return r;
    }

    // ------------------------------------------------------------------ tête posée puis cassée

    private static String cleBloc(Block b) {
        return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private static String cleBloc(Location l) {
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    /** Tête abîmée posée : ses défauts sont gardés dans le bloc. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Set<String> defauts = lire(event.getItemInHand());
        if (!defauts.isEmpty() && event.getBlockPlaced().getState() instanceof Skull crane) {
            crane.getPersistentDataContainer().set(cle, PersistentDataType.STRING, String.join(",", defauts));
            crane.update();
        }
    }

    private static void noter(Block b) {
        if (b.getType() != Material.PLAYER_HEAD && b.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }
        if (!(b.getState() instanceof Skull crane)) {
            return;
        }
        String s = crane.getPersistentDataContainer().get(cle, PersistentDataType.STRING);
        if (s == null) {
            return;
        }
        Set<String> d = new LinkedHashSet<>(List.of(s.split(",")));
        d.retainAll(NOMS.keySet());
        long maintenant = System.currentTimeMillis();
        casses.values().removeIf(e -> maintenant - e.date() > 2000);
        casses.put(cleBloc(b), new Etat(d, maintenant));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        noter(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(Defauts::noter);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(Defauts::noter);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(Defauts::noter);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        event.getBlocks().forEach(Defauts::noter);
    }

    /**
     * Défauts d'une tête qui apparaît au sol : ceux du bloc cassé à cet endroit (s'il vient d'être cassé), sinon tous
     * (nouvelle tête, ou tête cassée sans qu'on le sache : elle redevient abîmée, jamais réparée par erreur).
     */
    static Set<String> pourTeteAuSol(Location lieu) {
        Etat e = casses.remove(cleBloc(lieu));
        if (e != null && System.currentTimeMillis() - e.date() <= 2000) {
            return e.defauts();
        }
        return new LinkedHashSet<>(NOMS.keySet());
    }
}
