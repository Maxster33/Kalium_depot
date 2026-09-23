package fr.kalium.games.world;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Capture, stockage et collage des modeles d'arene (sans dependance externe). */
public final class TemplateService {


    public record CaptureResult(boolean ok, String error, int blocks) {
    }

    private final JavaPlugin plugin;
    private final File directory;
    private final Map<String, Template> cache = new HashMap<>();

    public TemplateService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "templates");
    }

    public File file(String arenaId) {
        return new File(directory, arenaId + ".kgt");
    }

    public boolean exists(String arenaId) {
        return file(arenaId).exists();
    }

    /** Modele charge (cache ou disque), null s'il n'existe pas ou est illisible. */
    public Template get(String arenaId) {
        Template cached = cache.get(arenaId);
        if (cached != null) {
            return cached;
        }
        File file = file(arenaId);
        if (!file.exists()) {
            return null;
        }
        try {
            Template template = Template.load(file);
            cache.put(arenaId, template);
            return template;
        } catch (IOException e) {
            plugin.getLogger().warning("Modèle illisible (" + arenaId + ") : " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ limites (config.yml, section arenas)

    /** Taille maximale d'une arene (blocs) sur chaque cote horizontal. */
    public int maxSize() {
        return maxSize(plugin);
    }

    public static int maxSize(JavaPlugin plugin) {
        return Math.max(64, Math.min(Template.MAX_SIZE, plugin.getConfig().getInt("arenas.max-size", 1500)));
    }

    private long maxVolume() {
        return Math.max(1_000_000L, plugin.getConfig().getLong("arenas.max-volume", 500_000_000L));
    }

    private long maxBlocks() {
        return Math.max(100_000L, Math.min(2_000_000_000L, plugin.getConfig().getLong("arenas.max-blocks", 60_000_000L)));
    }

    /** Chunks charges en arriere-plan en meme temps (capture, collage, effacement). */
    private static final int CAPTURE_CHUNKS_IN_FLIGHT = 8;
    private static final int LOAD_CHUNKS_IN_FLIGHT = 16;

    /** Blocs lus par tick pendant la capture (1.12.3, remplace capture-chunks-per-tick). */
    private int captureBlocksPerTick() {
        return Math.max(2_000, Math.min(500_000, plugin.getConfig().getInt("arenas.capture-blocks-per-tick", 30_000)));
    }

    /** Budget par tick de l'effacement d'une ancienne copie (1.12.3) : la moitie de celui du collage. */
    private int wipeBlocksPerTick() {
        return Math.max(5_000, plugin.getConfig().getInt("instances.wipe-blocks-per-tick", pasteBlocksPerTick() / 2));
    }

    private int pasteBlocksPerTick() {
        return Math.max(5_000, plugin.getConfig().getInt("instances.paste-blocks-per-tick", 40_000));
    }

    public void delete(String arenaId) {
        cache.remove(arenaId);
        file(arenaId).delete();
    }

    // ------------------------------------------------------------------ capture

    /** Copie la zone (deux coins inclus) du monde source dans le modele de l'arene, sur plusieurs ticks. */
    public void capture(World source, int x1, int y1, int z1, int x2, int y2, int z2, String arenaId, Consumer<CaptureResult> callback) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.max(Math.min(y1, y2), source.getMinHeight());
        int maxY = Math.min(Math.max(y1, y2), source.getMaxHeight() - 1);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        if (sizeY <= 0) {
            callback.accept(new CaptureResult(false, "Hauteur invalide.", 0));
            return;
        }
        int maxSize = maxSize();
        if (sizeX > maxSize || sizeZ > maxSize) {
            callback.accept(new CaptureResult(false, "Zone trop grande (maximum " + maxSize + " blocs par côté : " + sizeX + " x " + sizeZ
                    + "). La limite se règle avec arenas.max-size dans config.yml.", 0));
            return;
        }
        if (sizeY > Template.MAX_HEIGHT) {
            callback.accept(new CaptureResult(false, "Zone trop haute (maximum " + Template.MAX_HEIGHT + " blocs).", 0));
            return;
        }
        long maxVolume = maxVolume();
        if ((long) sizeX * sizeY * sizeZ > maxVolume) {
            callback.accept(new CaptureResult(false, "Zone trop volumineuse (maximum " + maxVolume + " blocs). "
                    + "La limite se règle avec arenas.max-volume dans config.yml.", 0));
            return;
        }
        final long maxBlocks = maxBlocks();
        final int blocksPerTick = captureBlocksPerTick();
        final int worldMinHeight = source.getMinHeight();

        final int fMinY = minY;
        final int fMaxY = maxY;
        plugin.getLogger().info("Capture de l'arène " + arenaId + " : " + sizeX + " x " + sizeY + " x " + sizeZ
                + " blocs, " + blocksPerTick + " blocs analysés par tick.");
        // 1.12.3 : capture ralentie et sans chargement synchrone (le serveur pouvait etre arrete par le watchdog).
        // Les chunks sont charges en arriere-plan (getChunkAtAsync, jamais generes), copies dans une photo
        // (ChunkSnapshot) puis analyses a raison d'un nombre maximal de blocs par tick, en reprenant au milieu d'un
        // chunk si besoin. Les sections vides sont sautees.
        new BukkitRunnable() {
            private final int firstCx = minX >> 4;
            private final int firstCz = minZ >> 4;
            private final int spanCx = (maxX >> 4) - firstCx + 1;
            private final int total = spanCx * ((maxZ >> 4) - firstCz + 1);
            private int nextRequest;
            private int inFlight;
            private int scanned;
            private final java.util.ArrayDeque<ChunkSnapshot> ready = new java.util.ArrayDeque<>();
            private ChunkSnapshot current;
            private boolean[] emptySections;
            private int column;
            private Throwable loadError;
            private boolean over;
            private long lastLog = System.currentTimeMillis();
            private final Map<BlockData, Integer> paletteIndex = new HashMap<>();
            private final List<String> palette = new ArrayList<>();
            // Blocs regroupes des la capture par chunk relatif : une seule copie en memoire (8 octets/bloc).
            private final int relX = (sizeX + 15) >> 4;
            private final int relZ = (sizeZ + 15) >> 4;
            private final long[][] groups = new long[relX * relZ][];
            private final int[] sizes = new int[relX * relZ];
            private long count;

            @Override
            public void run() {
                try {
                    requestChunks();
                    if (loadError != null) {
                        throw loadError;
                    }
                    int budget = blocksPerTick;
                    while (budget > 0) {
                        if (current == null) {
                            current = ready.poll();
                            if (current == null) {
                                break;
                            }
                            column = 0;
                            emptySections = new boolean[(source.getMaxHeight() - worldMinHeight + 15) >> 4];
                            for (int s = 0; s < emptySections.length; s++) {
                                try {
                                    emptySections[s] = current.isSectionEmpty(s);
                                } catch (RuntimeException ignored) {
                                    emptySections[s] = false;
                                }
                            }
                        }
                        budget -= scanColumns(budget);
                        if (count > maxBlocks) {
                            stop();
                            callback.accept(new CaptureResult(false, "Zone trop dense : plus de " + maxBlocks
                                    + " blocs non vides (arenas.max-blocks dans config.yml).", 0));
                            return;
                        }
                    }
                    if (nextRequest >= total && inFlight == 0 && ready.isEmpty() && current == null) {
                        finish();
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastLog >= 10_000L) {
                        lastLog = now;
                        plugin.getLogger().info("Capture de l'arène " + arenaId + " : " + scanned + " / " + total + " chunks.");
                    }
                } catch (Throwable t) {
                    stop();
                    plugin.getLogger().warning("Capture échouée : " + t);
                    callback.accept(new CaptureResult(false, "Erreur pendant la capture : " + t.getMessage(), 0));
                }
            }

            private void stop() {
                over = true;
                cancel();
                ready.clear();
                current = null;
            }

            /** Quelques chunks charges en arriere-plan a l'avance (jamais generes : un chunk inexistant est vide). */
            private void requestChunks() {
                while (inFlight < CAPTURE_CHUNKS_IN_FLIGHT && nextRequest < total) {
                    int index = nextRequest++;
                    int chunkX = firstCx + index % spanCx;
                    int chunkZ = firstCz + index / spanCx;
                    inFlight++;
                    source.getChunkAtAsync(chunkX, chunkZ, false).whenComplete((chunk, error) -> {
                        inFlight--;
                        if (over) {
                            return;
                        }
                        if (error != null) {
                            loadError = error;
                        } else if (chunk != null) {
                            ready.add(chunk.getChunkSnapshot(false, false, false));
                        } else {
                            scanned++;
                        }
                    });
                }
            }

            /** Analyse les colonnes (x, z) du chunk courant a partir de "column". Renvoie le cout (blocs lus). */
            private int scanColumns(int budget) {
                int cost = 0;
                int baseX = current.getX() << 4;
                int baseZ = current.getZ() << 4;
                while (column < 256 && cost < budget) {
                    int lx = column & 15;
                    int lz = column >> 4;
                    column++;
                    cost++;
                    int wx = baseX + lx;
                    int wz = baseZ + lz;
                    if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) {
                        continue;
                    }
                    int rel = ((wz - minZ) >> 4) * relX + ((wx - minX) >> 4);
                    for (int y = fMinY; y <= fMaxY; y++) {
                        int section = (y - worldMinHeight) >> 4;
                        if (emptySections[section]) {
                            y = worldMinHeight + (section << 4) + 15; // saute la fin de la section vide
                            continue;
                        }
                        cost++;
                        if (current.getBlockType(lx, y, lz).isAir()) {
                            continue;
                        }
                        BlockData data = current.getBlockData(lx, y, lz);
                        Integer index = paletteIndex.get(data);
                        if (index == null) {
                            index = palette.size();
                            palette.add(data.getAsString());
                            paletteIndex.put(data, index);
                        }
                        long[] group = groups[rel];
                        int size = sizes[rel];
                        if (group == null) {
                            group = new long[64];
                            groups[rel] = group;
                        } else if (size == group.length) {
                            group = Arrays.copyOf(group, size + (size >> 1) + 16);
                            groups[rel] = group;
                        }
                        group[size] = Template.pack(wx - minX, y - fMinY, wz - minZ, index);
                        sizes[rel] = size + 1;
                        count++;
                    }
                }
                if (column >= 256) {
                    current = null;
                    scanned++;
                }
                return cost;
            }

            private void finish() {
                over = true;
                cancel();
                Map<Long, long[]> chunks = new java.util.HashMap<>();
                for (int i = 0; i < groups.length; i++) {
                    if (groups[i] != null) {
                        chunks.put(Template.chunkKey(i % relX, i / relX), Arrays.copyOf(groups[i], sizes[i]));
                        groups[i] = null;
                    }
                }
                Template template = new Template(minX, fMinY, minZ, sizeX, sizeY, sizeZ,
                        palette.toArray(new String[0]), chunks);
                plugin.getLogger().info("Capture de l'arène " + arenaId + " terminée : " + template.blockCount() + " blocs.");
                // L'ecriture d'une tres grosse arene est longue : hors du thread principal (sinon le watchdog
                // du serveur pourrait l'arreter).
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        template.save(file(arenaId));
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            cache.put(arenaId, template);
                            callback.accept(new CaptureResult(true, null, template.blockCount()));
                        });
                    } catch (IOException e) {
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> callback.accept(new CaptureResult(false, "Écriture impossible : " + e.getMessage(), 0)));
                    }
                });
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ------------------------------------------------------------------ collage / liberation

    /**
     * Colle le modele au niveau de l'emplacement (slotX, slotZ, multiples de 16) et garde les chunks
     * charges. done est appele quand tout est en place. La tache renvoyee peut etre annulee.
     */
    public BukkitTask paste(Template template, World world, int slotX, int slotZ, Runnable done) {
        int baseCx = slotX >> 4;
        int baseCz = slotZ >> 4;
        // Un chunk deja charge est "sale" (reste d'une partie precedente) : il est vide avant le collage.
        // La zone d'effacement est tout l'emplacement (pas seulement l'arene qui va etre collee) et toute la
        // hauteur du monde : une arene precedente plus grande ou plus haute ne laisse ainsi aucun bloc.
        int cell = cellChunks();
        java.util.Set<Long> stale = new java.util.LinkedHashSet<>();
        for (int i = 0; i < cell; i++) {
            for (int j = 0; j < cell; j++) {
                if (world.isChunkLoaded(baseCx + i, baseCz + j)) {
                    stale.add(Template.chunkKey(i, j));
                }
            }
        }
        // 1.12.3 : les chunks de l'arene ne sont plus charges d'un coup (chargement synchrone de centaines de chunks =
        // serveur bloque) : ils sont charges en arriere-plan, quelques-uns a la fois, et gardes charges par un ticket
        // des leur arrivee. Chaque chunk de l'arene est aussi vide avant le collage (cout negligeable s'il est deja
        // vide) : un chunk reste sur le disque d'une ancienne copie ne laisse ainsi aucun bloc.
        final int chunksX = template.chunksX();
        final int chunksZ = template.chunksZ();
        for (int n = 0; n < chunksX * chunksZ; n++) {
            stale.add(Template.chunkKey(n % chunksX, n / chunksX));
        }
        final long[] toLoad = stale.stream().mapToLong(Long::longValue).toArray();
        final int chunksTotal = toLoad.length;
        List<Map.Entry<Long, long[]>> work = new ArrayList<>(template.byChunk().entrySet());
        BlockData[] resolved = new BlockData[template.palette().length];
        BlockData air = Bukkit.createBlockData(Material.AIR);
        final int pasteBlocksPerTick = pasteBlocksPerTick();
        return new BukkitRunnable() {
            private int index;
            private int cursor;
            private java.util.Iterator<Long> staleIterator;
            private int nextLoad;
            private int loading;
            private int loaded;

            @Override
            public void run() {
                try {
                    // 0) chargement en arriere-plan des chunks de l'arene
                    if (loaded < chunksTotal) {
                        while (loading < LOAD_CHUNKS_IN_FLIGHT && nextLoad < chunksTotal) {
                            long key = toLoad[nextLoad++];
                            int cx = baseCx + Template.chunkX(key);
                            int cz = baseCz + Template.chunkZ(key);
                            loading++;
                            world.getChunkAtAsync(cx, cz, true).whenComplete((chunk, error) -> {
                                loading--;
                                loaded++;
                                if (!isCancelled() && chunk != null) {
                                    world.addPluginChunkTicket(cx, cz, plugin);
                                }
                            });
                        }
                        if (loaded < chunksTotal) {
                            return;
                        }
                    }
                    int budget = pasteBlocksPerTick;
                    if (staleIterator == null) {
                        staleIterator = stale.iterator();
                    }
                    // 1) nettoyage des chunks sales (et des chunks de l'arene)
                    while (staleIterator.hasNext() && budget > 0) {
                        long key = staleIterator.next();
                        int cx = baseCx + Template.chunkX(key);
                        int cz = baseCz + Template.chunkZ(key);
                        if (!world.isChunkLoaded(cx, cz)) {
                            continue; // ne devrait pas arriver (ticket pose au chargement)
                        }
                        budget -= clearChunk(world, world.getChunkAt(cx, cz), air);
                        if (Template.chunkX(key) >= chunksX || Template.chunkZ(key) >= chunksZ) {
                            // chunk sale hors de l'arene : plus besoin de le garder charge
                            world.removePluginChunkTicket(cx, cz, plugin);
                        }
                    }
                    if (staleIterator.hasNext()) {
                        return;
                    }
                    // 2) collage
                    while (index < work.size() && budget > 0) {
                        Map.Entry<Long, long[]> entry = work.get(index);
                        int cx = baseCx + Template.chunkX(entry.getKey());
                        int cz = baseCz + Template.chunkZ(entry.getKey());
                        if (cursor == 0) {
                            world.getChunkAt(cx, cz);
                        }
                        long[] blocks = entry.getValue();
                        while (cursor < blocks.length && budget > 0) {
                            long packed = blocks[cursor++];
                            budget--;
                            int paletteIndex = Template.paletteIndex(packed);
                            BlockData data = resolved[paletteIndex];
                            if (data == null) {
                                data = parse(template.palette()[paletteIndex]);
                                resolved[paletteIndex] = data;
                            }
                            world.getBlockAt(slotX + Template.dx(packed), template.originY() + Template.dy(packed),
                                    slotZ + Template.dz(packed)).setBlockData(data, false);
                        }
                        if (cursor >= blocks.length) {
                            index++;
                            cursor = 0;
                        }
                    }
                    if (index >= work.size()) {
                        cancel();
                        done.run();
                    }
                } catch (Throwable t) {
                    cancel();
                    plugin.getLogger().severe("Collage de l'arène échoué : " + t);
                    done.run();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Remise a l'identique COMPLETE d'une copie deja collee (1.12.1 - demande explicite de l'utilisateur pour le
     * Rush : "il faut remplacer tous les blocs qui ne sont plus comme le modele de la map, y compris les blocs
     * d'air"). Chaque position de la zone de l'arene est comparee au modele (une position absente du modele doit
     * etre de l'air) et corrigee si elle differe - contrairement a la restauration habituelle, qui ne remet en
     * etat que les blocs SUIVIS pendant la partie. Etalee sur plusieurs ticks (meme budget que le collage).
     * done recoit le nombre de blocs corriges.
     */
    public BukkitTask reconcile(Template template, World world, int slotX, int slotZ, Consumer<Integer> done) {
        final int chunksX = template.chunksX();
        final int chunksZ = template.chunksZ();
        final int sizeX = template.sizeX();
        final int sizeY = template.sizeY();
        final int sizeZ = template.sizeZ();
        final int originY = template.originY();
        final int budgetPerTick = pasteBlocksPerTick();
        final BlockData air = Bukkit.createBlockData(Material.AIR);
        final BlockData[] resolved = new BlockData[template.palette().length];
        return new BukkitRunnable() {
            private int chunk;
            private int fixed;

            @Override
            public void run() {
                try {
                    int budget = budgetPerTick;
                    while (chunk < chunksX * chunksZ && budget > 0) {
                        int i = chunk % chunksX;
                        int j = chunk / chunksX;
                        chunk++;
                        budget -= reconcileChunk(i, j);
                    }
                    if (chunk >= chunksX * chunksZ) {
                        cancel();
                        done.accept(fixed);
                    }
                } catch (Throwable t) {
                    cancel();
                    plugin.getLogger().severe("Remise à l'identique de l'arène échouée : " + t);
                    done.accept(fixed);
                }
            }

            /** Compare une colonne de chunk (16 x hauteur de l'arene x 16) au modele. Renvoie un cout pour le budget. */
            private int reconcileChunk(int i, int j) {
                int x0 = i << 4;
                int z0 = j << 4;
                int width = Math.min(16, sizeX - x0);
                int depth = Math.min(16, sizeZ - z0);
                if (width <= 0 || depth <= 0) {
                    return 1;
                }
                // Blocs attendus dans cette colonne : -1 = air.
                int[] expected = new int[16 * 16 * sizeY];
                Arrays.fill(expected, -1);
                long[] blocks = template.byChunk().get(Template.chunkKey(i, j));
                if (blocks != null) {
                    for (long packed : blocks) {
                        int lx = Template.dx(packed) - x0;
                        int lz = Template.dz(packed) - z0;
                        int dy = Template.dy(packed);
                        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && dy >= 0 && dy < sizeY) {
                            expected[(dy * 16 + lz) * 16 + lx] = Template.paletteIndex(packed);
                        }
                    }
                }
                int worldX0 = slotX + x0;
                int worldZ0 = slotZ + z0;
                Chunk chunk = world.getChunkAt(worldX0 >> 4, worldZ0 >> 4);
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
                int baseX = worldX0 & 15;
                int baseZ = worldZ0 & 15;
                int cost = 16 * 16 * sizeY / 8;
                for (int lx = 0; lx < width; lx++) {
                    for (int lz = 0; lz < depth; lz++) {
                        for (int dy = 0; dy < sizeY; dy++) {
                            int y = originY + dy;
                            if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                                continue;
                            }
                            int index = expected[(dy * 16 + lz) * 16 + lx];
                            BlockData current = snapshot.getBlockData(baseX + lx, y, baseZ + lz);
                            if (index < 0) {
                                if (!current.getMaterial().isAir()) {
                                    chunk.getBlock(baseX + lx, y, baseZ + lz).setBlockData(air, false);
                                    fixed++;
                                    cost += 4;
                                }
                                continue;
                            }
                            BlockData wanted = resolved[index];
                            if (wanted == null) {
                                wanted = parse(template.palette()[index]);
                                resolved[index] = wanted;
                            }
                            if (!current.equals(wanted)) {
                                chunk.getBlock(baseX + lx, y, baseZ + lz).setBlockData(wanted, false);
                                fixed++;
                                cost += 4;
                            }
                        }
                    }
                }
                return cost;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private BlockData parse(String text) {
        try {
            return Bukkit.createBlockData(text);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Bloc inconnu dans un modèle : " + text);
            return Bukkit.createBlockData(Material.AIR);
        }
    }

    /** Largeur (en chunks) d'un emplacement de la grille : c'est la zone qui est toujours effacee en entier. */
    private int cellChunks() {
        return plugin instanceof fr.kalium.games.KalGames kg && kg.worlds() != null ? Math.max(1, kg.worlds().spacing() >> 4) : 96;
    }

    /**
     * Vide un chunk sur toute la hauteur du monde (les sections deja vides sont ignorees).
     * Renvoie un cout approximatif pour le budget du tick.
     */
    private int clearChunk(World world, Chunk chunk, BlockData air) {
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();
        int cost = 300;
        for (int y0 = minHeight; y0 < maxHeight; y0 += 16) {
            boolean empty = false;
            try {
                empty = snapshot.isSectionEmpty((y0 - minHeight) >> 4);
            } catch (RuntimeException ignored) {
                // section inconnue : on la parcourt
            }
            if (empty) {
                continue;
            }
            int changed = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = y0; y < y0 + 16; y++) {
                        // Lecture dans la photo du chunk (rapide), ecriture seulement si le bloc n'est pas de l'air.
                        if (!snapshot.getBlockType(x, y, z).isAir()) {
                            chunk.getBlock(x, y, z).setBlockData(air, false);
                            changed++;
                        }
                    }
                }
            }
            cost += 1024 + changed * 4;
        }
        return cost;
    }

    /**
     * Libere l'emplacement : retire les tickets et decharge SANS sauvegarde tous les chunks charges de
     * l'emplacement entier (pas seulement ceux de l'arene), ce qui efface tous les blocs. Les joueurs
     * doivent deja etre partis.
     */
    public void release(Template template, World world, int slotX, int slotZ) {
        int baseCx = slotX >> 4;
        int baseCz = slotZ >> 4;
        // Tout l'emplacement (1.12.3) : un collage interrompu peut avoir pose des tickets hors de l'arene.
        int cell = Math.max(cellChunks(), Math.max(template.chunksX(), template.chunksZ()));
        for (int i = 0; i < cell; i++) {
            for (int j = 0; j < cell; j++) {
                world.removePluginChunkTicket(baseCx + i, baseCz + j, plugin);
            }
        }
        unloadCell(world, baseCx, baseCz);
    }

    /** Deuxieme passe (quelques ticks plus tard) pour les chunks qui n'auraient pas pu se decharger. */
    public void unloadAgain(Template template, World world, int slotX, int slotZ) {
        unloadCell(world, slotX >> 4, slotZ >> 4);
    }

    /**
     * Efface reellement les blocs de l'emplacement (mis a l'air, sur plusieurs ticks) avant de le liberer.
     * A la difference de {@link #release}, qui se contente de decharger les chunks sans sauvegarder (ce qui suffisait
     * quand le monde des instances n'etait jamais sauvegarde), cette methode est necessaire pour un abandon
     * DEFINITIF d'une copie (arene supprimee ou recapturee) : depuis que le monde peut etre conserve entre deux
     * redemarrages propres (voir InstanceWorld), une copie juste dechargee sans etre effacee laisserait ses blocs
     * sur le disque indefiniment si elle avait deja ete sauvegardee une fois.
     */
    public void clearArea(World world, int slotX, int slotZ, Runnable done) {
        wipeQueue.add(new WipeJob(world, slotX >> 4, slotZ >> 4, done));
        startNextWipe();
    }

    /**
     * 1.12.3 : les effacements passent UN PAR UN (file d'attente). Avant, chaque effacement posait d'un coup un ticket
     * sur les 96 x 96 chunks de l'emplacement, ce qui les chargeait (et les generait) de facon synchrone : apres une
     * recapture, le serveur restait bloque et le watchdog l'arretait.
     */
    private final java.util.ArrayDeque<WipeJob> wipeQueue = new java.util.ArrayDeque<>();
    private WipeJob activeWipe;

    private void startNextWipe() {
        if (activeWipe != null || wipeQueue.isEmpty() || !plugin.isEnabled()) {
            return;
        }
        activeWipe = wipeQueue.poll();
        activeWipe.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Effacement d'un emplacement : chaque chunk est demande en arriere-plan SANS generation (un chunk jamais genere
     * n'a rien a effacer), quelques-uns a la fois ; un chunk charge est vide, puis relache (il est alors sauvegarde
     * vide et decharge par le serveur). Budget de blocs par tick : instances.wipe-blocks-per-tick.
     */
    private final class WipeJob extends BukkitRunnable {
        private final World world;
        private final int baseCx;
        private final int baseCz;
        private final Runnable done;
        private final int cell = cellChunks();
        private final int total = cell * cell;
        private final BlockData air = Bukkit.createBlockData(Material.AIR);
        private final java.util.ArrayDeque<Long> ready = new java.util.ArrayDeque<>();
        private int next;
        private int inFlight;
        private int cleared;
        private boolean over;
        private final long started = System.currentTimeMillis();

        WipeJob(World world, int baseCx, int baseCz, Runnable done) {
            this.world = world;
            this.baseCx = baseCx;
            this.baseCz = baseCz;
            this.done = done;
        }

        @Override
        public void run() {
            try {
                // Demandes en arriere-plan (limitees par tick pour ne pas saturer le systeme de chunks).
                int issued = 0;
                while (inFlight < LOAD_CHUNKS_IN_FLIGHT && next < total && issued < 64) {
                    int n = next++;
                    issued++;
                    int cx = baseCx + n / cell;
                    int cz = baseCz + n % cell;
                    if (world.isChunkLoaded(cx, cz)) {
                        world.addPluginChunkTicket(cx, cz, plugin);
                        ready.add(Template.chunkKey(cx, cz));
                        continue;
                    }
                    inFlight++;
                    world.getChunkAtAsync(cx, cz, false).whenComplete((chunk, error) -> {
                        inFlight--;
                        if (!over && chunk != null) {
                            world.addPluginChunkTicket(cx, cz, plugin);
                            ready.add(Template.chunkKey(cx, cz));
                        }
                    });
                }
                int budget = wipeBlocksPerTick();
                while (budget > 0 && !ready.isEmpty()) {
                    long key = ready.poll();
                    int cx = Template.chunkX(key);
                    int cz = Template.chunkZ(key);
                    if (world.isChunkLoaded(cx, cz)) {
                        budget -= clearChunk(world, world.getChunkAt(cx, cz), air);
                        cleared++;
                    }
                    world.removePluginChunkTicket(cx, cz, plugin);
                }
                if (next >= total && inFlight == 0 && ready.isEmpty()) {
                    end();
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("Effacement de l'emplacement échoué : " + t);
                for (Long key : ready) {
                    world.removePluginChunkTicket(Template.chunkX(key), Template.chunkZ(key), plugin);
                }
                ready.clear();
                end();
            }
        }

        private void end() {
            over = true;
            cancel();
            plugin.getLogger().info("Ancienne copie d'arène effacée (" + cleared + " chunks, "
                    + (System.currentTimeMillis() - started) / 1000 + " s).");
            activeWipe = null;
            try {
                done.run();
            } finally {
                startNextWipe();
            }
        }
    }

    private void unloadCell(World world, int baseCx, int baseCz) {
        int cell = cellChunks();
        for (int i = 0; i < cell; i++) {
            for (int j = 0; j < cell; j++) {
                if (world.isChunkLoaded(baseCx + i, baseCz + j)) {
                    world.unloadChunk(baseCx + i, baseCz + j, false);
                }
            }
        }
    }
}
