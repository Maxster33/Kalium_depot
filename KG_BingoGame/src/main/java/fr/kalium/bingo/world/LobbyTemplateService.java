package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Capture/collage de la salle d'attente Bingo (demandee par l'utilisateur, salle vivant
 * sur le serveur Bingo lui-meme). Un seul modele (pas un systeme multi-arenes comme
 * KalGames) : une seule salle, dupliquee a plusieurs emplacements par LobbySlots.
 *
 * Selection "comme pour les arenes sur kal-games" (demande explicite de l'utilisateur) :
 * un coin = la position actuelle du joueur (pas de coordonnees a taper a la main), voir
 * BingoAdminCommand. Recapturable a tout moment : chaque nouvelle capture ecrase le
 * modele precedent et republie automatiquement sur tous les emplacements.
 *
 * 0.1.21 - taille maximale portee de 64 a 256 blocs par cote (demande explicite de l'utilisateur). Une
 * salle de 256 x 256 x 256 represente 16,7 millions de blocs (x 4 emplacements) : capture, effacement et
 * collage ne se font donc plus d'un seul coup (le serveur aurait ete bloque puis arrete par le watchdog)
 * mais par petits morceaux, un nombre limite de blocs par tick (lobby.blocks-per-tick), chunk par chunk,
 * les chunks etant charges en arriere-plan. Les operations passent une par une dans une file d'attente.
 */
public final class LobbyTemplateService {

    private final JavaPlugin plugin;
    private final File file;
    private LobbyTemplate cached;

    private final ArrayDeque<RegionJob> queue = new ArrayDeque<>();
    private RegionJob active;

    public LobbyTemplateService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lobby_template.kgt");
    }

    public boolean exists() {
        return file.exists();
    }

    public LobbyTemplate get() {
        if (cached != null) {
            return cached;
        }
        if (!file.exists()) {
            return null;
        }
        try {
            cached = LobbyTemplate.load(file);
            return cached;
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().warning("[KG_BingoGame] Modele de salle d'attente illisible : " + e.getMessage());
            return null;
        }
    }

    public int maxSize() {
        return Math.max(4, Math.min(256, plugin.getConfig().getInt("lobby.max-size", 256)));
    }

    /** Cout maximal par tick (1 par bloc lu, 4 par bloc modifie). 0.6.0 : 10 000 par defaut au lieu de 30 000, et
     *  jusqu'a 500 minimum (demande de LeKiwi06 : "les recaptures de map sont trop gourmandes [...] on va alléger
     *  ça en allongeant dans le temps"). */
    private int blocksPerTick() {
        return Math.max(500, Math.min(500_000, plugin.getConfig().getInt("lobby.blocks-per-tick", 10_000)));
    }

    /** true tant qu'une capture, un effacement ou un collage est en cours ou en attente. */
    public boolean busy() {
        return active != null || !queue.isEmpty();
    }

    /**
     * Capture le cuboide defini par deux coins (coordonnees incluses, coin le plus bas =
     * origine du modele) ainsi qu'un point d'apparition (position + orientation), sur plusieurs ticks.
     * La taille est verifiee tout de suite (IllegalArgumentException) ; onDone recoit le modele
     * (deja en cache, sauvegarde en arriere-plan), onError un message si la capture echoue en cours de route.
     */
    public void capture(Location corner1, Location corner2, Location spawnMarker,
                        Consumer<LobbyTemplate> onDone, Consumer<String> onError) {
        World world = corner1.getWorld();
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.max(world.getMinHeight(), Math.min(corner1.getBlockY(), corner2.getBlockY()));
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int maxY = Math.min(world.getMaxHeight() - 1, Math.max(corner1.getBlockY(), corner2.getBlockY()));
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        int limit = maxSize();
        if (sizeX > limit || sizeY > limit || sizeZ > limit) {
            throw new IllegalArgumentException(
                    "Selection trop grande (max " + limit + " blocs par cote, voir lobby.max-size).");
        }
        if (sizeY <= 0) {
            throw new IllegalArgumentException("Hauteur de selection invalide.");
        }

        final double spawnOffsetX = spawnMarker.getX() - minX;
        final double spawnOffsetY = spawnMarker.getY() - minY;
        final double spawnOffsetZ = spawnMarker.getZ() - minZ;
        final float yaw = spawnMarker.getYaw();
        final float pitch = spawnMarker.getPitch();

        // 0.7.3 : entites de la salle (hors joueurs, objets au sol, projectiles), relevees tout de suite (l'admin est
        // sur place : les chunks de la salle sont charges).
        final List<LobbyTemplate.EntityCopy> entities = new ArrayList<>();
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(new org.bukkit.util.BoundingBox(minX, minY, minZ,
                maxX + 1, maxY + 1, maxZ + 1))) {
            if (!copiable(entity)) {
                continue;
            }
            org.bukkit.entity.EntitySnapshot snapshot = entity.createSnapshot();
            if (snapshot == null) {
                continue;
            }
            Location at = entity.getLocation();
            entities.add(new LobbyTemplate.EntityCopy(at.getX() - minX, at.getY() - minY, at.getZ() - minZ,
                    at.getYaw(), at.getPitch(), snapshot.getAsString()));
        }

        final char[] blocks = new char[sizeX * sizeY * sizeZ];
        final List<String> palette = new ArrayList<>();
        final Map<BlockData, Integer> indexes = new HashMap<>();
        palette.add(LobbyTemplate.AIR);

        enqueue(new RegionJob("capture", world, minX, minY, minZ, sizeX, sizeY, sizeZ) {
            @Override
            int apply(Block block, int rx, int ry, int rz) {
                if (block.getType().isAir()) {
                    return 1; // indice 0 = air (tableau deja a zero)
                }
                BlockData data = block.getBlockData();
                Integer index = indexes.get(data);
                if (index == null) {
                    index = palette.size();
                    if (index > LobbyTemplate.MAX_PALETTE) {
                        throw new IllegalStateException("trop de blocs differents dans la salle");
                    }
                    palette.add(data.getAsString());
                    indexes.put(data, index);
                }
                blocks[rx + ry * sizeX + rz * sizeX * sizeY] = (char) index.intValue();
                return 2;
            }

            @Override
            void finished(Throwable error) {
                if (error != null) {
                    onError.accept("Capture échouée : " + error.getMessage());
                    return;
                }
                LobbyTemplate template = new LobbyTemplate(sizeX, sizeY, sizeZ, palette.toArray(new String[0]), blocks,
                        spawnOffsetX, spawnOffsetY, spawnOffsetZ, yaw, pitch).withEntities(entities);
                cached = template;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        template.save(file);
                    } catch (IOException e) {
                        plugin.getLogger().severe("[KG_BingoGame] Impossible de sauvegarder le modele de salle d'attente : " + e.getMessage());
                    }
                });
                onDone.accept(template);
            }
        });
    }

    /**
     * Efface (remplace par de l'air) la zone qu'occupait ce modele une fois colle a `origin` - utilise
     * pour supprimer les anciennes salles d'attente avant d'en coller de nouvelles apres une
     * recapture (0.1.20, voir LobbySlots.clearAllSlots). Sur plusieurs ticks (0.1.21).
     */
    public void clear(LobbyTemplate template, Location origin, Runnable done) {
        final BlockData air = Bukkit.createBlockData(Material.AIR);
        enqueue(new RegionJob("effacement", origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(),
                template.sizeX(), template.sizeY(), template.sizeZ()) {
            @Override
            int apply(Block block, int rx, int ry, int rz) {
                if (block.getType().isAir()) {
                    return 1;
                }
                block.setBlockData(air, false);
                return 4;
            }

            @Override
            void finished(Throwable error) {
                removeEntities(template, origin);
                if (done != null) {
                    done.run();
                }
            }
        });
    }

    /** Colle le modele dans le monde donne, coin le plus bas a `origin`. Sur plusieurs ticks (0.1.21). */
    public void paste(LobbyTemplate template, Location origin, Runnable done) {
        enqueue(new RegionJob("collage", origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(),
                template.sizeX(), template.sizeY(), template.sizeZ()) {
            @Override
            int apply(Block block, int rx, int ry, int rz) {
                int index = template.paletteIndexAt(rx, ry, rz);
                // 0.7.6 : les barrieres ne sont pas recopiees dans les salles collees (demande de LeKiwi06,
                // 25/09/2026 : « enlever les blocs invisibles maintenant que l'on est invulnérable » - voir
                // LobbyProtectionListener). Le modele et la salle d'origine les gardent.
                if (index != 0 && template.data(index).getMaterial() == org.bukkit.Material.BARRIER) {
                    index = 0;
                }
                if (index == 0) {
                    if (block.getType().isAir()) {
                        return 1;
                    }
                    block.setBlockData(template.data(0), false);
                    return 4;
                }
                BlockData wanted = template.data(index);
                if (block.getBlockData().equals(wanted)) {
                    return 2;
                }
                block.setBlockData(wanted, false);
                return 4;
            }

            @Override
            void finished(Throwable error) {
                placeEntities(template, origin);
                if (done != null) {
                    done.run();
                }
            }
        });
    }

    private static final java.util.regex.Pattern CUSTOM_NAME = java.util.regex.Pattern.compile("CustomName:\"((?:[^\"\\\\]|\\\\.)*)\"");

    /**
     * 0.7.4 : le nom personnalise est reapplique explicitement a la copie (signale par LeKiwi06 : les copies de la vache
     * « Giselle » n'avaient pas le bon nom), relu dans la copie enregistree ; affichage permanent recopie aussi.
     */
    private static void applyName(org.bukkit.entity.Entity created, String snapshot) {
        java.util.regex.Matcher matcher = CUSTOM_NAME.matcher(snapshot);
        if (matcher.find()) {
            String name = matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
            created.customName(net.kyori.adventure.text.Component.text(name));
            created.setCustomNameVisible(snapshot.contains("CustomNameVisible:1b") || snapshot.contains("CustomNameVisible:true"));
        }
    }

    /** 0.7.3 : entites copiees avec la salle (voir LobbyTemplate.EntityCopy). */
    private static boolean copiable(org.bukkit.entity.Entity entity) {
        return !(entity instanceof org.bukkit.entity.Player) && !(entity instanceof org.bukkit.entity.Item)
                && !(entity instanceof org.bukkit.entity.ExperienceOrb) && !(entity instanceof org.bukkit.entity.Projectile);
    }

    /** Retire les entites copiables presentes dans la zone d'une salle collee a origin (avant recollage / effacement). */
    private void removeEntities(LobbyTemplate template, Location origin) {
        org.bukkit.util.BoundingBox box = new org.bukkit.util.BoundingBox(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(),
                origin.getBlockX() + template.sizeX(), origin.getBlockY() + template.sizeY(), origin.getBlockZ() + template.sizeZ());
        for (org.bukkit.entity.Entity entity : origin.getWorld().getNearbyEntities(box)) {
            if (copiable(entity)) {
                entity.remove();
            }
        }
    }

    /**
     * Recree les entites du modele dans une salle collee a origin. Les anciennes copies (collage precedent, redemarrage)
     * sont d'abord retirees : pas de doublons. Les chunks de la zone sont charges pour l'occasion.
     */
    private void placeEntities(LobbyTemplate template, Location origin) {
        if (template.entities().isEmpty()) {
            return;
        }
        World world = origin.getWorld();
        for (int cx = origin.getBlockX() >> 4; cx <= (origin.getBlockX() + template.sizeX()) >> 4; cx++) {
            for (int cz = origin.getBlockZ() >> 4; cz <= (origin.getBlockZ() + template.sizeZ()) >> 4; cz++) {
                world.getChunkAt(cx, cz).getEntities(); // charge le chunk et ses entites
            }
        }
        removeEntities(template, origin);
        int placed = 0;
        for (LobbyTemplate.EntityCopy copy : template.entities()) {
            try {
                Location at = new Location(world, origin.getBlockX() + copy.dx(), origin.getBlockY() + copy.dy(),
                        origin.getBlockZ() + copy.dz(), copy.yaw(), copy.pitch());
                org.bukkit.entity.Entity created = Bukkit.getEntityFactory().createEntitySnapshot(copy.snapshot()).createEntity(at);
                created.setInvulnerable(true); // decor de la salle : ne peut pas etre tue (demande de LeKiwi06)
                applyName(created, copy.snapshot());
                placed++;
            } catch (RuntimeException e) {
                plugin.getLogger().warning("[KG_BingoGame] Entité de la salle d'attente non recréée : " + e.getMessage());
            }
        }
        plugin.getLogger().info("[KG_BingoGame] Salle d'attente : " + placed + " entité(s) recréée(s).");
    }

    // ------------------------------------------------------------------ file d'attente

    private void enqueue(RegionJob job) {
        queue.add(job);
        startNext();
    }

    private void startNext() {
        if (active != null || queue.isEmpty() || !plugin.isEnabled()) {
            return;
        }
        active = queue.poll();
        active.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Parcourt une zone chunk par chunk (chaque chunk charge en arriere-plan puis garde charge par un
     * ticket le temps de son traitement), bloc par bloc, dans la limite de blocksPerTick() par tick, en
     * reprenant la ou elle s'etait arretee au tick suivant.
     */
    private abstract class RegionJob extends BukkitRunnable {
        private final String label;
        private final World world;
        private final int x0;
        private final int y0;
        private final int z0;
        private final int sx;
        private final int sy;
        private final int sz;
        private final int firstCx;
        private final int firstCz;
        private final int spanCx;
        private final int totalChunks;
        private int chunkIndex;
        private boolean requested;
        private boolean loaded;
        private int column;
        private int dy;
        private boolean over;
        private final long started = System.currentTimeMillis();

        RegionJob(String label, World world, int x0, int y0, int z0, int sx, int sy, int sz) {
            this.label = label;
            this.world = world;
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.firstCx = x0 >> 4;
            this.firstCz = z0 >> 4;
            this.spanCx = ((x0 + sx - 1) >> 4) - firstCx + 1;
            this.totalChunks = spanCx * (((z0 + sz - 1) >> 4) - firstCz + 1);
        }

        /** Traite un bloc (coordonnees relatives a la zone). Renvoie son cout. */
        abstract int apply(Block block, int rx, int ry, int rz);

        /** Fin du travail (error = null si tout s'est bien passe). */
        abstract void finished(Throwable error);

        @Override
        public void run() {
            try {
                int budget = blocksPerTick();
                while (budget > 0 && chunkIndex < totalChunks) {
                    int cx = firstCx + chunkIndex % spanCx;
                    int cz = firstCz + chunkIndex / spanCx;
                    if (!loaded) {
                        if (world.isChunkLoaded(cx, cz)) {
                            world.addPluginChunkTicket(cx, cz, plugin);
                            loaded = true;
                            column = 0;
                            dy = 0;
                        } else {
                            if (!requested) {
                                requested = true;
                                world.getChunkAtAsync(cx, cz, true).whenComplete((chunk, error) -> {
                                    if (over) {
                                        return;
                                    }
                                    if (chunk != null) {
                                        world.addPluginChunkTicket(cx, cz, plugin);
                                        loaded = true;
                                        column = 0;
                                        dy = 0;
                                    } else {
                                        requested = false; // nouvel essai au tick suivant
                                    }
                                });
                            }
                            return; // chunk en cours de chargement en arriere-plan
                        }
                    }
                    int xa = Math.max(x0, cx << 4);
                    int xb = Math.min(x0 + sx - 1, (cx << 4) + 15);
                    int za = Math.max(z0, cz << 4);
                    int zb = Math.min(z0 + sz - 1, (cz << 4) + 15);
                    int width = xb - xa + 1;
                    int columns = width * (zb - za + 1);
                    while (budget > 0 && column < columns) {
                        int x = xa + column % width;
                        int z = za + column / width;
                        while (budget > 0 && dy < sy) {
                            budget -= apply(world.getBlockAt(x, y0 + dy, z), x - x0, dy, z - z0);
                            dy++;
                        }
                        if (dy >= sy) {
                            dy = 0;
                            column++;
                        }
                    }
                    if (column >= columns) {
                        world.removePluginChunkTicket(cx, cz, plugin);
                        chunkIndex++;
                        loaded = false;
                        requested = false;
                    }
                }
                if (chunkIndex >= totalChunks) {
                    end(null);
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("[KG_BingoGame] Salle d'attente - " + label + " échoué : " + t);
                end(t);
            }
        }

        private void end(Throwable error) {
            if (over) {
                return;
            }
            over = true;
            cancel();
            if (loaded) {
                int cx = firstCx + chunkIndex % spanCx;
                int cz = firstCz + chunkIndex / spanCx;
                world.removePluginChunkTicket(cx, cz, plugin);
            }
            if (error == null) {
                plugin.getLogger().info("[KG_BingoGame] Salle d'attente - " + label + " terminé (" + sx + "x" + sy + "x" + sz
                        + ", " + (System.currentTimeMillis() - started) / 1000 + " s).");
            }
            active = null;
            try {
                finished(error);
            } finally {
                startNext();
            }
        }
    }
}
