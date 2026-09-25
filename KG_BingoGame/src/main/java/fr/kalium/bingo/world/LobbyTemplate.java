package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Modele de la salle d'attente Bingo : un cuboide de BlockData + un point d'apparition
 * relatif, capture une fois par un admin (voir BingoAdminCommand) puis colle a plusieurs
 * emplacements (LobbySlots). 0.7.3 : les entites sont aussi copiees (voir EntityCopy) ; pas de contenus
 * de coffres ni de panneaux geres specifiquement.
 *
 * 0.1.21 (taille maximale portee a 256 blocs par cote, demande explicite de l'utilisateur) : les blocs
 * sont stockes sous forme de PALETTE (liste des etats de blocs differents) + un indice 16 bits par
 * position (2 octets par bloc au lieu d'une chaine de caracteres), et le fichier est binaire compresse.
 * L'ancien format texte (0.1.x) reste lisible.
 */
public final class LobbyTemplate {

    /** Premier entier d'un fichier au nouveau format (apres decompression). */
    private static final int MAGIC = 0x4B4C5432; // "KLT2"
    /** 0.7.3 : meme format + entites a la fin. */
    private static final int MAGIC_ENTITIES = 0x4B4C5433; // "KLT3"
    /** Nombre maximal d'etats de blocs differents (indice 16 bits). */
    public static final int MAX_PALETTE = 65_535;
    public static final String AIR = "minecraft:air";

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final String[] palette; // palette[0] = air
    private final char[] blocks;    // index = x + y*sizeX + z*sizeX*sizeY -> indice dans la palette
    private final BlockData[] resolved;

    /** Point d'apparition, relatif au coin le plus bas (coordonnees fractionnaires conservees). */
    private final double spawnOffsetX;
    private final double spawnOffsetY;
    private final double spawnOffsetZ;
    private final float spawnYaw;
    private final float spawnPitch;

    /**
     * 0.7.3 : entites de la salle (animaux, porte-armures, cadres... ; demande de LeKiwi06 : « l'entité que j'avais
     * placée dans le hub n'a pas été dupliquée dans les autres hubs (la vache) ») : position relative au coin le plus
     * bas, orientation et copie complete de l'entite (SNBT de Paper : type, nom, equipement, IA...).
     */
    public record EntityCopy(double dx, double dy, double dz, float yaw, float pitch, String snapshot) {
    }

    private List<EntityCopy> entities = List.of();

    public List<EntityCopy> entities() {
        return entities;
    }

    LobbyTemplate withEntities(List<EntityCopy> list) {
        this.entities = List.copyOf(list);
        return this;
    }

    LobbyTemplate(int sizeX, int sizeY, int sizeZ, String[] palette, char[] blocks,
                  double spawnOffsetX, double spawnOffsetY, double spawnOffsetZ,
                  float spawnYaw, float spawnPitch) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.blocks = blocks;
        this.resolved = new BlockData[palette.length];
        this.spawnOffsetX = spawnOffsetX;
        this.spawnOffsetY = spawnOffsetY;
        this.spawnOffsetZ = spawnOffsetZ;
        this.spawnYaw = spawnYaw;
        this.spawnPitch = spawnPitch;
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

    /** Indice de palette a cette position relative. */
    int paletteIndexAt(int x, int y, int z) {
        return blocks[x + y * sizeX + z * sizeX * sizeY];
    }

    /** Etat de bloc (mis en cache) d'un indice de palette. */
    BlockData data(int paletteIndex) {
        BlockData data = resolved[paletteIndex];
        if (data == null) {
            try {
                data = Bukkit.createBlockData(palette[paletteIndex]);
            } catch (IllegalArgumentException e) {
                data = Bukkit.createBlockData(Material.AIR);
            }
            resolved[paletteIndex] = data;
        }
        return data;
    }

    public BlockData blockAt(int x, int y, int z) {
        return data(paletteIndexAt(x, y, z));
    }

    /** Point d'apparition reel une fois le modele colle a `origin` (coin le plus bas). */
    public Location spawnLocation(Location origin) {
        return new Location(origin.getWorld(),
                origin.getBlockX() + spawnOffsetX,
                origin.getBlockY() + spawnOffsetY,
                origin.getBlockZ() + spawnOffsetZ,
                spawnYaw, spawnPitch);
    }

    public void save(File file) throws IOException {
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(tmp), 1 << 16), 1 << 16))) {
            out.writeInt(MAGIC_ENTITIES);
            out.writeInt(sizeX);
            out.writeInt(sizeY);
            out.writeInt(sizeZ);
            out.writeDouble(spawnOffsetX);
            out.writeDouble(spawnOffsetY);
            out.writeDouble(spawnOffsetZ);
            out.writeFloat(spawnYaw);
            out.writeFloat(spawnPitch);
            out.writeInt(palette.length);
            for (String entry : palette) {
                out.writeUTF(entry);
            }
            for (char index : blocks) {
                out.writeChar(index);
            }
            out.writeInt(entities.size());
            for (EntityCopy copy : entities) {
                out.writeDouble(copy.dx());
                out.writeDouble(copy.dy());
                out.writeDouble(copy.dz());
                out.writeFloat(copy.yaw());
                out.writeFloat(copy.pitch());
                byte[] bytes = copy.snapshot().getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
            }
        }
        Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public static LobbyTemplate load(File file) throws IOException {
        boolean gzip;
        try (InputStream probe = new FileInputStream(file)) {
            int b1 = probe.read();
            int b2 = probe.read();
            gzip = b1 == 0x1f && b2 == 0x8b;
        }
        return gzip ? loadBinary(file) : loadLegacyText(file);
    }

    private static LobbyTemplate loadBinary(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file), 1 << 16), 1 << 16))) {
            int magic = in.readInt();
            if (magic != MAGIC && magic != MAGIC_ENTITIES) {
                throw new IOException("Format de modele inconnu.");
            }
            int sizeX = in.readInt();
            int sizeY = in.readInt();
            int sizeZ = in.readInt();
            double sx = in.readDouble();
            double sy = in.readDouble();
            double sz = in.readDouble();
            float yaw = in.readFloat();
            float pitch = in.readFloat();
            int paletteSize = in.readInt();
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || paletteSize <= 0 || paletteSize > MAX_PALETTE + 1) {
                throw new IOException("Modele corrompu.");
            }
            String[] palette = new String[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                palette[i] = in.readUTF();
            }
            char[] blocks = new char[sizeX * sizeY * sizeZ];
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = in.readChar();
                if (blocks[i] >= paletteSize) {
                    throw new IOException("Modele corrompu (indice de palette).");
                }
            }
            LobbyTemplate template = new LobbyTemplate(sizeX, sizeY, sizeZ, palette, blocks, sx, sy, sz, yaw, pitch);
            if (magic == MAGIC_ENTITIES) {
                int count = in.readInt();
                List<EntityCopy> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    double dx = in.readDouble();
                    double dy = in.readDouble();
                    double dz = in.readDouble();
                    float eyaw = in.readFloat();
                    float epitch = in.readFloat();
                    byte[] bytes = new byte[in.readInt()];
                    in.readFully(bytes);
                    list.add(new EntityCopy(dx, dy, dz, eyaw, epitch, new String(bytes, StandardCharsets.UTF_8)));
                }
                template.withEntities(list);
            }
            return template;
        }
    }

    /** Ancien format texte (jusqu'a 0.1.20) : une ligne d'en-tete puis une ligne par bloc. */
    private static LobbyTemplate loadLegacyText(File file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                throw new IOException("Fichier de modele vide.");
            }
            String[] parts = header.split(" ");
            int sizeX = Integer.parseInt(parts[0]);
            int sizeY = Integer.parseInt(parts[1]);
            int sizeZ = Integer.parseInt(parts[2]);
            double spawnOffsetX = Double.parseDouble(parts[3]);
            double spawnOffsetY = Double.parseDouble(parts[4]);
            double spawnOffsetZ = Double.parseDouble(parts[5]);
            float spawnYaw = Float.parseFloat(parts[6]);
            float spawnPitch = Float.parseFloat(parts[7]);

            int total = sizeX * sizeY * sizeZ;
            List<String> palette = new ArrayList<>();
            Map<String, Integer> indexes = new HashMap<>();
            palette.add(AIR);
            indexes.put(AIR, 0);
            char[] blocks = new char[total];
            for (int i = 0; i < total; i++) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("Fichier de modele tronque.");
                }
                Integer index = indexes.get(line);
                if (index == null) {
                    index = palette.size();
                    if (index > MAX_PALETTE) {
                        throw new IOException("Trop de blocs differents dans le modele.");
                    }
                    palette.add(line);
                    indexes.put(line, index);
                }
                blocks[i] = (char) index.intValue();
            }
            return new LobbyTemplate(sizeX, sizeY, sizeZ, palette.toArray(new String[0]), blocks,
                    spawnOffsetX, spawnOffsetY, spawnOffsetZ, spawnYaw, spawnPitch);
        }
    }
}
