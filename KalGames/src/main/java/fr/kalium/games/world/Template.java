package fr.kalium.games.world;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Modele d'arene : blocs non vides d'une zone, en coordonnees relatives a son coin minimum.
 * Un bloc est empaquete dans un long : dx(13) dz(13) dy(10) palette(28) (format KGT2, jusqu'a 8191 blocs
 * par cote). Les anciens fichiers KGT1 (dx(10) dy(10) dz(10) palette(30)) sont convertis au chargement.
 */
public final class Template {

    /** Taille maximale (blocs) par cote horizontal permise par le format de fichier. */
    public static final int MAX_SIZE = 8191;
    /** Hauteur maximale permise par le format de fichier. */
    public static final int MAX_HEIGHT = 1023;
    private static final int MAGIC_V1 = 0x4B475431; // "KGT1"
    private static final int MAGIC = 0x4B475432; // "KGT2"

    private final int originX;
    private final int originY;
    private final int originZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final String[] palette;
    /** Blocs regroupes par chunk relatif (dx >> 4, dz >> 4) : c'est la seule copie gardee en memoire (8 octets/bloc). */
    private final Map<Long, long[]> byChunk;
    private final int count;

    public Template(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ, String[] palette, Map<Long, long[]> byChunk) {
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.byChunk = byChunk;
        long total = 0;
        for (long[] group : byChunk.values()) {
            total += group.length;
        }
        this.count = (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Construit un modele a partir d'une liste plate de blocs (regroupee par chunk). */
    public Template(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ, String[] palette, long[] blocks) {
        this(originX, originY, originZ, sizeX, sizeY, sizeZ, palette, group(blocks, blocks.length, (sizeX + 15) >> 4, (sizeZ + 15) >> 4));
    }

    public int originX() {
        return originX;
    }

    public int originY() {
        return originY;
    }

    public int originZ() {
        return originZ;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public int blockCount() {
        return count;
    }

    public String[] palette() {
        return palette;
    }

    public static long pack(int dx, int dy, int dz, int paletteIndex) {
        return ((long) (dx & 0x1FFF) << 51) | ((long) (dz & 0x1FFF) << 38) | ((long) (dy & 0x3FF) << 28) | (paletteIndex & 0xFFFFFFFL);
    }

    public static int dx(long packed) {
        return (int) ((packed >>> 51) & 0x1FFF);
    }

    public static int dz(long packed) {
        return (int) ((packed >>> 38) & 0x1FFF);
    }

    public static int dy(long packed) {
        return (int) ((packed >>> 28) & 0x3FF);
    }

    public static int paletteIndex(long packed) {
        return (int) (packed & 0xFFFFFFFL);
    }

    /** Ancien format KGT1 : dx(10) dy(10) dz(10) palette(30). */
    private static long convertV1(long old) {
        int dx = (int) ((old >>> 50) & 0x3FF);
        int dy = (int) ((old >>> 40) & 0x3FF);
        int dz = (int) ((old >>> 30) & 0x3FF);
        int palette = (int) (old & 0x3FFFFFFFL);
        return pack(dx, dy, dz, palette);
    }

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public static int chunkX(long key) {
        return (int) (key >> 32);
    }

    public static int chunkZ(long key) {
        return (int) key;
    }

    /** Index du chunk relatif d'un bloc, ou -1 s'il est en dehors de la zone declaree (fichier corrompu). */
    private static int indexOf(long packed, int cx, int cz) {
        int x = dx(packed) >> 4;
        int z = dz(packed) >> 4;
        return x < cx && z < cz ? z * cx + x : -1;
    }

    /** Regroupe une liste plate par chunk relatif (les blocs hors zone sont ignores). */
    private static Map<Long, long[]> group(long[] blocks, int length, int cx, int cz) {
        int[] counts = new int[cx * cz];
        for (int i = 0; i < length; i++) {
            int index = indexOf(blocks[i], cx, cz);
            if (index >= 0) {
                counts[index]++;
            }
        }
        long[][] groups = new long[counts.length][];
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                groups[i] = new long[counts[i]];
            }
        }
        int[] cursor = new int[counts.length];
        for (int i = 0; i < length; i++) {
            int index = indexOf(blocks[i], cx, cz);
            if (index >= 0) {
                groups[index][cursor[index]++] = blocks[i];
            }
        }
        Map<Long, long[]> result = new HashMap<>();
        for (int i = 0; i < groups.length; i++) {
            if (groups[i] != null) {
                result.put(chunkKey(i % cx, i / cx), groups[i]);
            }
        }
        return result;
    }

    /** Blocs regroupes par chunk relatif (dx >> 4, dz >> 4). Le decalage d'instance est un multiple de 16. */
    public Map<Long, long[]> byChunk() {
        return byChunk;
    }

    /** Nombre de chunks couverts par la zone (relatifs), meme sans bloc. */
    public int chunksX() {
        return (sizeX + 15) >> 4;
    }

    public int chunksZ() {
        return (sizeZ + 15) >> 4;
    }

    // ------------------------------------------------------------------ fichier

    public void save(File file) throws IOException {
        file.getParentFile().mkdirs();
        // Compression rapide (niveau 1) et ecriture par gros blocs : une grosse arene s'ecrit en quelques secondes.
        GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(file.toPath()), 1 << 16) {
            {
                def.setLevel(Deflater.BEST_SPEED);
            }
        };
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(gzip, 1 << 16))) {
            out.writeInt(MAGIC);
            out.writeInt(originX);
            out.writeInt(originY);
            out.writeInt(originZ);
            out.writeInt(sizeX);
            out.writeInt(sizeY);
            out.writeInt(sizeZ);
            out.writeInt(palette.length);
            for (String entry : palette) {
                out.writeUTF(entry);
            }
            out.writeInt(count);
            ByteBuffer buffer = ByteBuffer.allocate(8 * 8192);
            for (long[] group : byChunk.values()) {
                for (long block : group) {
                    if (!buffer.hasRemaining()) {
                        out.write(buffer.array(), 0, buffer.position());
                        buffer.clear();
                    }
                    buffer.putLong(block);
                }
            }
            out.write(buffer.array(), 0, buffer.position());
        }
    }

    private static void flushRun(Map<Long, long[]> chunks, long[] run, int length, int chunk, int cx) {
        if (chunk < 0 || length == 0) {
            return;
        }
        long key = chunkKey(chunk % cx, chunk / cx);
        long[] existing = chunks.get(key);
        if (existing == null) {
            chunks.put(key, Arrays.copyOf(run, length));
        } else {
            long[] merged = Arrays.copyOf(existing, existing.length + length);
            System.arraycopy(run, 0, merged, existing.length, length);
            chunks.put(key, merged);
        }
    }

    public static Template load(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file.toPath()))))) {
            int magic = in.readInt();
            if (magic != MAGIC && magic != MAGIC_V1) {
                throw new IOException("Fichier de modèle invalide : " + file.getName());
            }
            int ox = in.readInt();
            int oy = in.readInt();
            int oz = in.readInt();
            int sx = in.readInt();
            int sy = in.readInt();
            int sz = in.readInt();
            String[] palette = new String[in.readInt()];
            for (int i = 0; i < palette.length; i++) {
                palette[i] = in.readUTF();
            }
            int total = in.readInt();
            int cx = (sx + 15) >> 4;
            int cz = (sz + 15) >> 4;
            // Les blocs sont ecrits chunk par chunk : on lit par series contigues (un seul tableau de travail
            // reutilise) pour ne jamais garder deux copies du modele en memoire.
            Map<Long, long[]> chunks = new HashMap<>();
            long[] run = new long[4096];
            int runLength = 0;
            int runChunk = -2;
            for (int i = 0; i < total; i++) {
                long value = in.readLong();
                if (magic == MAGIC_V1) {
                    value = convertV1(value);
                }
                int chunk = indexOf(value, cx, cz);
                if (chunk != runChunk) {
                    flushRun(chunks, run, runLength, runChunk, cx);
                    runLength = 0;
                    runChunk = chunk;
                }
                if (chunk < 0) {
                    continue;
                }
                if (runLength == run.length) {
                    run = Arrays.copyOf(run, run.length * 2);
                }
                run[runLength++] = value;
            }
            flushRun(chunks, run, runLength, runChunk, cx);
            return new Template(ox, oy, oz, sx, sy, sz, palette, chunks);
        }
    }
}
