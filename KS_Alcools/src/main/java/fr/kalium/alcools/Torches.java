package fr.kalium.alcools;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Torche augmentée (cahier, partie 3.3) : se pose comme une torche ; le plugin pose autour d'elle des blocs de lumière
 * de niveau 15 (invisibles) sur une grille de 3 (environ 500 blocs, lumière minimale 11) dans un rayon de 15 blocs,
 * seulement dans l'air et les sources d'eau, jamais dans le claim d'un autre joueur que celui qui l'a posée.
 *
 * Les torches sont notées dans leur chunk (position et poseur). Torche disparue (cassée, emportée par l'eau, support
 * cassé, explosion, piston, commande) : sa grille est recalculée depuis sa position et ses blocs de lumière de niveau
 * 15 retirés (pas de liste), puis les grilles des torches augmentées voisines sont reposées. Vérifié au tick suivant
 * d'un objet « torche » apparu sur place, et toutes les 5 secondes pour les torches des chunks chargés. Objet rendu :
 * une torche augmentée (la torche du jeu qui tombe est remplacée).
 *
 * Limite : un bloc de lumière de niveau 15 posé à la main sur un point de la grille est retiré avec elle.
 */
final class Torches implements Listener {

    private static final int RAYON = 15;
    private static final int ECART = 3;
    private static final int NIVEAU = 15;

    /** Une torche augmentée posée. */
    private record Torche(World monde, int x, int y, int z, UUID poseur) {
        String code() {
            return x + "," + y + "," + z + "," + poseur;
        }

        Block bloc() {
            return monde.getBlockAt(x, y, z);
        }

        static Torche lire(World monde, String code) {
            String[] p = code.split(",");
            if (p.length != 4) {
                return null;
            }
            try {
                return new Torche(monde, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                        UUID.fromString(p[3]));
            } catch (IllegalArgumentException erreur) {
                return null;
            }
        }
    }

    /** Clé d'un chunk dans les tables. */
    private record CleChunk(UUID monde, int x, int z) {
        static CleChunk de(Chunk chunk) {
            return new CleChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    private final KSAlcools plugin;
    private final NamespacedKey cleChunk;

    /** Torches des chunks chargés, par chunk. */
    private final Map<CleChunk, List<Torche>> chargees = new HashMap<>();
    /** Objets « torche » apparus sur une torche augmentée, à remplacer si elle a disparu au tick suivant. */
    private final Map<Item, Torche> tombees = new HashMap<>();

    Torches(KSAlcools plugin) {
        this.plugin = plugin;
        this.cleChunk = new NamespacedKey(plugin, "torches");
    }

    void demarrer() {
        for (World monde : Bukkit.getWorlds()) {
            for (Chunk chunk : monde.getLoadedChunks()) {
                charger(chunk);
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::verifierTout, 100L, 100L);
    }

    // ------------------------------------------------------------------ registre (chunks)

    private void charger(Chunk chunk) {
        List<String> codes = chunk.getPersistentDataContainer().get(cleChunk, PersistentDataType.LIST.strings());
        if (codes == null || codes.isEmpty()) {
            return;
        }
        List<Torche> torches = new ArrayList<>();
        for (String code : codes) {
            Torche t = Torche.lire(chunk.getWorld(), code);
            if (t != null) {
                torches.add(t);
            }
        }
        chargees.put(CleChunk.de(chunk), torches);
    }

    private void enregistrer(Chunk chunk) {
        List<Torche> torches = chargees.get(CleChunk.de(chunk));
        if (torches == null || torches.isEmpty()) {
            chunk.getPersistentDataContainer().remove(cleChunk);
            chargees.remove(CleChunk.de(chunk));
        } else {
            chunk.getPersistentDataContainer().set(cleChunk, PersistentDataType.LIST.strings(),
                    torches.stream().map(Torche::code).toList());
        }
    }

    private Torche torcheEn(Block bloc) {
        List<Torche> torches = chargees.get(CleChunk.de(bloc.getChunk()));
        if (torches != null) {
            for (Torche t : torches) {
                if (t.x() == bloc.getX() && t.y() == bloc.getY() && t.z() == bloc.getZ()) {
                    return t;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onChargerChunk(ChunkLoadEvent event) {
        charger(event.getChunk());
    }

    @EventHandler
    public void onDechargerChunk(ChunkUnloadEvent event) {
        chargees.remove(CleChunk.de(event.getChunk()));
    }

    // ------------------------------------------------------------------ poser

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPoser(BlockPlaceEvent event) {
        if (!plugin.estTorche(event.getItemInHand())) {
            return;
        }
        Block bloc = event.getBlockPlaced();
        Torche t = new Torche(bloc.getWorld(), bloc.getX(), bloc.getY(), bloc.getZ(), event.getPlayer().getUniqueId());
        chargees.computeIfAbsent(CleChunk.de(bloc.getChunk()), c -> new ArrayList<>()).add(t);
        enregistrer(bloc.getChunk());
        eclairer(t);
    }

    /** Pose les blocs de lumière de la grille d'une torche (air et sources d'eau, hors des claims des autres). */
    private void eclairer(Torche t) {
        Map<CleChunk, Boolean> refus = new HashMap<>();
        BlockData air = lumiere(false);
        BlockData eau = lumiere(true);
        grille(t, bloc -> {
            Chunk chunk = bloc.getChunk();
            if (refus.computeIfAbsent(CleChunk.de(chunk), c -> plugin.claimRefuse(chunk, t.poseur()))) {
                return;
            }
            Material type = bloc.getType();
            if (type == Material.AIR || type == Material.CAVE_AIR) {
                bloc.setBlockData(air, false);
            } else if (type == Material.WATER && bloc.getBlockData() instanceof Levelled niveau && niveau.getLevel() == 0) {
                bloc.setBlockData(eau, false);
            }
        });
    }

    private static BlockData lumiere(boolean eau) {
        Light light = (Light) Material.LIGHT.createBlockData();
        light.setLevel(NIVEAU);
        light.setWaterlogged(eau);
        return light;
    }

    /** Points de la grille d'une torche (sauf la torche), dans les chunks chargés. */
    private static void grille(Torche t, java.util.function.Consumer<Block> action) {
        int pas = RAYON / ECART;
        for (int i = -pas; i <= pas; i++) {
            for (int j = -pas; j <= pas; j++) {
                for (int k = -pas; k <= pas; k++) {
                    int dx = i * ECART;
                    int dy = j * ECART;
                    int dz = k * ECART;
                    if ((dx == 0 && dy == 0 && dz == 0) || dx * dx + dy * dy + dz * dz > RAYON * RAYON) {
                        continue;
                    }
                    int y = t.y() + dy;
                    if (y < t.monde().getMinHeight() || y >= t.monde().getMaxHeight()
                            || !t.monde().isChunkLoaded((t.x() + dx) >> 4, (t.z() + dz) >> 4)) {
                        continue;
                    }
                    action.accept(t.monde().getBlockAt(t.x() + dx, y, t.z() + dz));
                }
            }
        }
    }

    // ------------------------------------------------------------------ disparition

    /** La torche n'est plus là : sa lumière est retirée et les torches voisines sont rallumées. */
    private void verifier(Torche t) {
        Material type = t.bloc().getType();
        if (type == Material.TORCH || type == Material.WALL_TORCH) {
            return;
        }
        Chunk chunk = t.bloc().getChunk();
        List<Torche> torches = chargees.get(CleChunk.de(chunk));
        if (torches == null || !torches.remove(t)) {
            return;
        }
        enregistrer(chunk);
        grille(t, bloc -> {
            if (bloc.getType() == Material.LIGHT && bloc.getBlockData() instanceof Light light
                    && light.getLevel() == NIVEAU) {
                bloc.setType(light.isWaterlogged() ? Material.WATER : Material.AIR, false);
            }
        });
        // Les grilles voisines qui croisaient celle-ci ont pu perdre des blocs.
        for (List<Torche> liste : chargees.values()) {
            for (Torche voisine : liste) {
                if (voisine.monde().equals(t.monde()) && Math.abs(voisine.x() - t.x()) <= 2 * RAYON
                        && Math.abs(voisine.y() - t.y()) <= 2 * RAYON && Math.abs(voisine.z() - t.z()) <= 2 * RAYON) {
                    eclairer(voisine);
                }
            }
        }
    }

    /** Toutes les 5 secondes : torches des chunks chargés. */
    private void verifierTout() {
        List<Torche> toutes = new ArrayList<>();
        chargees.values().forEach(toutes::addAll);
        toutes.forEach(this::verifier);
    }

    /** Cassée par un joueur : la torche du jeu qui tombe devient une torche augmentée. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCasser(BlockDropItemEvent event) {
        Torche t = torcheEn(event.getBlock());
        if (t == null) {
            return;
        }
        for (Item objet : event.getItems()) {
            remplacer(objet);
        }
        Bukkit.getScheduler().runTask(plugin, () -> verifier(t));
    }

    /** Autres cas (eau, support, explosion, piston) : objet « torche » apparu sur une torche augmentée. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onApparition(ItemSpawnEvent event) {
        Item objet = event.getEntity();
        if (objet.getItemStack().getType() != Material.TORCH || objet.getThrower() != null
                || plugin.estTorche(objet.getItemStack())) {
            return;
        }
        Torche t = torcheEn(objet.getLocation().getBlock());
        if (t == null) {
            return;
        }
        boolean premier = tombees.isEmpty();
        tombees.put(objet, t);
        if (premier) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Set<Torche> vues = new HashSet<>();
                tombees.forEach((item, torche) -> {
                    Material type = torche.bloc().getType();
                    if (item.isValid() && type != Material.TORCH && type != Material.WALL_TORCH) {
                        remplacer(item);
                    }
                    vues.add(torche);
                });
                tombees.clear();
                vues.forEach(this::verifier);
            });
        }
    }

    private void remplacer(Item objet) {
        ItemStack pile = objet.getItemStack();
        if (pile.getType() == Material.TORCH && !plugin.estTorche(pile)) {
            ItemStack torche = KSAlcools.creerTorche(1);
            torche.setAmount(pile.getAmount());
            objet.setItemStack(torche);
        }
    }
}
