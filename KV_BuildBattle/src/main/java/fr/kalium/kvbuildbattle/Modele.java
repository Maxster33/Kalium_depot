package fr.kalium.kvbuildbattle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/**
 * Une construction capturée (boîte de l'arène ou salle d'attente) : un cuboïde d'états de blocs, un point
 * d'apparition relatif et les entités (porte-armures, cadres...). Même format que la salle d'attente du Bingo
 * (KG_BingoGame, LobbyTemplate) : palette des états de blocs + un indice 16 bits par bloc, fichier compressé.
 */
final class Modele {

    private static final int MAGIC = 0x4B424231; // "KBB1"
    static final int MAX_PALETTE = 65_535;
    static final String AIR = "minecraft:air";

    /** Entité : position relative au coin le plus bas, orientation, copie complète (SNBT de Paper). */
    record Entite(double dx, double dy, double dz, float yaw, float pitch, String copie) {}

    final int sx, sy, sz;
    private final String[] palette; // palette[0] = air
    private final char[] blocs;     // indice = x + y*sx + z*sx*sy
    private final BlockData[] etats;
    /** Point d'apparition, relatif au coin le plus bas. */
    final double ax, ay, az;
    final float yaw, pitch;
    final List<Entite> entites;

    Modele(int sx, int sy, int sz, String[] palette, char[] blocs, double ax, double ay, double az, float yaw,
           float pitch, List<Entite> entites) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.palette = palette;
        this.blocs = blocs;
        this.etats = new BlockData[palette.length];
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.yaw = yaw;
        this.pitch = pitch;
        this.entites = List.copyOf(entites);
    }

    int indice(int x, int y, int z) {
        return blocs[x + y * sx + z * sx * sy];
    }

    BlockData etat(int indice) {
        BlockData d = etats[indice];
        if (d == null) {
            try {
                d = Bukkit.createBlockData(palette[indice]);
            } catch (IllegalArgumentException e) {
                d = Bukkit.createBlockData(Material.AIR);
            }
            etats[indice] = d;
        }
        return d;
    }

    /** Point d'apparition une fois le modèle collé avec son coin le plus bas en (x, y, z). */
    Location apparition(World monde, int x, int y, int z) {
        return new Location(monde, x + ax, y + ay, z + az, yaw, pitch);
    }

    void sauver(File fichier) throws IOException {
        fichier.getParentFile().mkdirs();
        File tmp = new File(fichier.getParentFile(), fichier.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(tmp), 1 << 16), 1 << 16))) {
            out.writeInt(MAGIC);
            out.writeInt(sx);
            out.writeInt(sy);
            out.writeInt(sz);
            out.writeDouble(ax);
            out.writeDouble(ay);
            out.writeDouble(az);
            out.writeFloat(yaw);
            out.writeFloat(pitch);
            out.writeInt(palette.length);
            for (String p : palette) out.writeUTF(p);
            for (char c : blocs) out.writeChar(c);
            out.writeInt(entites.size());
            for (Entite e : entites) {
                out.writeDouble(e.dx());
                out.writeDouble(e.dy());
                out.writeDouble(e.dz());
                out.writeFloat(e.yaw());
                out.writeFloat(e.pitch());
                byte[] b = e.copie().getBytes(StandardCharsets.UTF_8);
                out.writeInt(b.length);
                out.write(b);
            }
        }
        Files.move(tmp.toPath(), fichier.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    static Modele charger(File fichier) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(fichier), 1 << 16), 1 << 16))) {
            if (in.readInt() != MAGIC) throw new IOException("format inconnu");
            int sx = in.readInt(), sy = in.readInt(), sz = in.readInt();
            double ax = in.readDouble(), ay = in.readDouble(), az = in.readDouble();
            float yaw = in.readFloat(), pitch = in.readFloat();
            int n = in.readInt();
            if (sx <= 0 || sy <= 0 || sz <= 0 || n <= 0 || n > MAX_PALETTE + 1) throw new IOException("fichier corrompu");
            String[] palette = new String[n];
            for (int i = 0; i < n; i++) palette[i] = in.readUTF();
            char[] blocs = new char[sx * sy * sz];
            for (int i = 0; i < blocs.length; i++) {
                blocs[i] = in.readChar();
                if (blocs[i] >= n) throw new IOException("fichier corrompu (palette)");
            }
            int ne = in.readInt();
            List<Entite> entites = new ArrayList<>();
            for (int i = 0; i < ne; i++) {
                double dx = in.readDouble(), dy = in.readDouble(), dz = in.readDouble();
                float ey = in.readFloat(), ep = in.readFloat();
                byte[] b = new byte[in.readInt()];
                in.readFully(b);
                entites.add(new Entite(dx, dy, dz, ey, ep, new String(b, StandardCharsets.UTF_8)));
            }
            return new Modele(sx, sy, sz, palette, blocs, ax, ay, az, yaw, pitch, entites);
        }
    }
}
