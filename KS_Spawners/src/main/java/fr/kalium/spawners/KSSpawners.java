package fr.kalium.spawners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KS_Spawners (demande de Maxster33, 29/09/2026) : spawners avec leur créature, fabriqués avec KS_Crafts.
 *
 * - Objet : spawner « Spawner à zombi »... marqué (id de la créature), empilable par 64.
 * - Pose : le jeu n'applique la créature d'un objet spawner que pour un opérateur ; le plugin la règle lui-même et
 *   marque le bloc comme posé par un joueur. Ensuite, le spawner fonctionne comme un spawner vanilla.
 * - Cassage (n'importe quel outil, choix de Maxster33) : un spawner posé par un joueur tombe tel quel, sans XP (sinon
 *   on pourrait produire de l'XP à l'infini) ; en créatif, rien ne tombe (comme en vanilla). Les spawners naturels
 *   gardent le comportement vanilla (KS_LootBlocs y ajoute les Fragments de Spawner).
 * - Explosion (TNT, creeper...) : un spawner posé par un joueur n'est pas perdu, il tombe au sol.
 *
 * Marqueur du bloc posé : « ks_spawners:pose » (lu aussi par KS_LootBlocs). Autres plugins : creerSpawner(id)
 * (KS_Crafts, KS_KaliumGive).
 */
public final class KSSpawners extends JavaPlugin implements Listener {

    /** Id -> créature. */
    public static final Map<String, EntityType> CREATURES = new LinkedHashMap<>();
    /** Id -> nom affiché. */
    private static final Map<String, String> NOMS = new LinkedHashMap<>();

    static {
        ajouter("zombi", EntityType.ZOMBIE, "Spawner à zombi");
        ajouter("squelette", EntityType.SKELETON, "Spawner à squelette");
        ajouter("araignee", EntityType.SPIDER, "Spawner à araignée");
        ajouter("creeper", EntityType.CREEPER, "Spawner à creeper");
        ajouter("blaze", EntityType.BLAZE, "Spawner à blaze");
        ajouter("mouton", EntityType.SHEEP, "Spawner à mouton");
        ajouter("vache", EntityType.COW, "Spawner à vache");
        ajouter("poule", EntityType.CHICKEN, "Spawner à poule");
    }

    private static void ajouter(String id, EntityType type, String nom) {
        CREATURES.put(id, type);
        NOMS.put(id, nom);
    }

    /** Marqueur de l'objet (id de la créature). */
    private static NamespacedKey marqueurObjet;
    /** Marqueur du bloc posé par un joueur (id de la créature). */
    private static NamespacedKey marqueurPose;

    @Override
    public void onEnable() {
        marqueurObjet = new NamespacedKey(this, "creature");
        marqueurPose = new NamespacedKey(this, "pose");
        getServer().getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------ objets

    /** Un spawner (id : clé de CREATURES), ou null si l'id est inconnu. Nécessite que le plugin soit activé. */
    public static ItemStack creerSpawner(String id) {
        String nom = NOMS.get(id);
        if (nom == null) {
            return null;
        }
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(nom, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(marqueurObjet, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private static String idObjet(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(marqueurObjet, PersistentDataType.STRING);
    }

    // ------------------------------------------------------------------ pose et cassage

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String id = idObjet(event.getItemInHand());
        if (id == null || !CREATURES.containsKey(id)
                || !(event.getBlockPlaced().getState() instanceof CreatureSpawner spawner)) {
            return;
        }
        spawner.setSpawnedType(CREATURES.get(id));
        spawner.getPersistentDataContainer().set(marqueurPose, PersistentDataType.STRING, id);
        spawner.update();
    }

    /** MONITOR : l'objet ne tombe que si aucun plugin (protections...) n'a annulé le cassage. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER || !(block.getState() instanceof CreatureSpawner spawner)) {
            return;
        }
        String id = idPose(spawner);
        if (id == null) {
            return; // spawner naturel : comportement vanilla
        }
        event.setExpToDrop(0);
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            lacher(block, id);
        }
    }

    /** Demande de Maxster33 : un spawner posé détruit par une explosion n'est pas perdu, il tombe au sol. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        explosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        explosion(event.blockList());
    }

    private static void explosion(List<Block> blocs) {
        for (Block block : blocs) {
            if (block.getType() == Material.SPAWNER && block.getState() instanceof CreatureSpawner spawner) {
                String id = idPose(spawner);
                if (id != null) {
                    lacher(block, id);
                }
            }
        }
    }

    /**
     * Id d'un spawner posé par un joueur (null pour un spawner naturel) : créature actuelle si c'est l'une des nôtres
     * (un opérateur a pu la changer avec un oeuf), sinon celle posée.
     */
    private static String idPose(CreatureSpawner spawner) {
        String id = spawner.getPersistentDataContainer().get(marqueurPose, PersistentDataType.STRING);
        if (id == null) {
            return null;
        }
        for (Map.Entry<String, EntityType> entry : CREATURES.entrySet()) {
            if (entry.getValue() == spawner.getSpawnedType()) {
                return entry.getKey();
            }
        }
        return id;
    }

    private static void lacher(Block block, String id) {
        ItemStack objet = creerSpawner(id);
        if (objet != null) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), objet);
        }
    }
}
