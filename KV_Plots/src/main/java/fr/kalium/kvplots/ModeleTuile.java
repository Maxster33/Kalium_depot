package fr.kalium.kvplots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Tuile de référence de la grille, relevée une fois dans le monde puis gardée dans modele-tuile.yml : un carré de
 * « pas » x « pas » colonnes à partir du coin nord-ouest de l'intérieur du plot (0, 0) = l'intérieur du plot vierge,
 * la route à l'est, la route au sud et le croisement au sud-est (bordures en bedrock comprises).
 *
 * Recopiée partout (position modulo le pas), elle génère la grille à l'identique ; sa colonne centrale sert de sol
 * pour remplacer les routes intérieures d'un grand plot.
 */
final class ModeleTuile {

    private final int pas, yMin;
    /** palette[0] = air. */
    private final BlockData[] palette;
    /** Pour chaque colonne (tx * pas + tz) : indices de palette de yMin jusqu'au plus haut bloc non vide. */
    private final short[][] colonnes;

    private ModeleTuile(int pas, int yMin, BlockData[] palette, short[][] colonnes) {
        this.pas = pas;
        this.yMin = yMin;
        this.palette = palette;
        this.colonnes = colonnes;
    }

    static ModeleTuile relever(World monde, int origineX, int origineZ, int pas) {
        int yMin = monde.getMinHeight();
        List<BlockData> palette = new ArrayList<>();
        Map<String, Short> index = new HashMap<>();
        palette.add(null);
        short[][] colonnes = new short[pas * pas][];
        for (int tx = 0; tx < pas; tx++) {
            for (int tz = 0; tz < pas; tz++) {
                int x = origineX + tx, z = origineZ + tz;
                int haut = monde.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
                short[] col = new short[Math.max(0, haut - yMin + 1)];
                for (int i = 0; i < col.length; i++) {
                    Block b = monde.getBlockAt(x, yMin + i, z);
                    if (b.getType().isAir()) continue;
                    BlockData d = b.getBlockData();
                    Short n = index.get(d.getAsString());
                    if (n == null) {
                        n = (short) palette.size();
                        index.put(d.getAsString(), n);
                        palette.add(d);
                    }
                    col[i] = n;
                }
                colonnes[tx * pas + tz] = col;
            }
        }
        return new ModeleTuile(pas, yMin, palette.toArray(new BlockData[0]), colonnes);
    }

    /**
     * Pose en (x, z) la colonne (tx, tz) de la tuile ; au-dessus, de l'air jusqu'au plus haut bloc existant. Ne touche
     * pas aux blocs déjà identiques. Renvoie le nombre de blocs examinés.
     */
    int poser(World monde, int x, int z, int tx, int tz) {
        short[] col = colonnes[tx * pas + tz];
        int haut = Math.max(yMin + col.length - 1, monde.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE));
        for (int y = yMin; y <= haut; y++) {
            int i = y - yMin;
            BlockData voulu = i < col.length ? palette[col[i]] : null;
            Block b = monde.getBlockAt(x, y, z);
            if (voulu == null) {
                if (!b.getType().isAir()) b.setType(Material.AIR, false);
            } else if (b.getType() != voulu.getMaterial() || !b.getBlockData().equals(voulu)) {
                b.setBlockData(voulu, false);
            }
        }
        return haut - yMin + 1;
    }

    /** Pose en (x, z) la colonne de la tuile qui correspond à sa place dans la grille. */
    int poserGrille(World monde, int x, int z, int origineX, int origineZ) {
        return poser(monde, x, z, Math.floorMod(x - origineX, pas), Math.floorMod(z - origineZ, pas));
    }

    /** Pose en (x, z) le sol d'un plot vierge (colonne au centre de l'intérieur du plot). */
    int poserSol(World monde, int x, int z, int taille) {
        return poser(monde, x, z, taille / 2, taille / 2);
    }

    // --- Fichier : palette + une ligne par colonne, en « indice*répétitions » séparés par des espaces ---

    void sauver(File fichier) throws IOException {
        List<String> pal = new ArrayList<>();
        for (int i = 1; i < palette.length; i++) pal.add(palette[i].getAsString());
        List<String> cols = new ArrayList<>();
        for (short[] col : colonnes) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < col.length; ) {
                int j = i;
                while (j < col.length && col[j] == col[i]) j++;
                if (sb.length() > 0) sb.append(' ');
                sb.append(col[i]).append('*').append(j - i);
                i = j;
            }
            cols.add(sb.toString());
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("pas", pas);
        yml.set("y-min", yMin);
        yml.set("palette", pal);
        yml.set("colonnes", cols);
        yml.save(fichier);
    }

    /** Null si le fichier est absent ou ne correspond pas au pas de la grille. */
    static ModeleTuile charger(File fichier, int pasAttendu) {
        if (!fichier.isFile()) return null;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        int pas = yml.getInt("pas");
        if (pas != pasAttendu) return null;
        List<String> pal = yml.getStringList("palette");
        BlockData[] palette = new BlockData[pal.size() + 1];
        for (int i = 0; i < pal.size(); i++) palette[i + 1] = Bukkit.createBlockData(pal.get(i));
        List<String> cols = yml.getStringList("colonnes");
        if (cols.size() != pas * pas) return null;
        short[][] colonnes = new short[pas * pas][];
        for (int c = 0; c < colonnes.length; c++) {
            List<Short> valeurs = new ArrayList<>();
            String ligne = cols.get(c).trim();
            if (!ligne.isEmpty()) {
                for (String morceau : ligne.split(" ")) {
                    int etoile = morceau.indexOf('*');
                    short v = Short.parseShort(morceau.substring(0, etoile));
                    int n = Integer.parseInt(morceau.substring(etoile + 1));
                    for (int k = 0; k < n; k++) valeurs.add(v);
                }
            }
            short[] col = new short[valeurs.size()];
            for (int i = 0; i < col.length; i++) col[i] = valeurs.get(i);
            colonnes[c] = col;
        }
        return new ModeleTuile(pas, yml.getInt("y-min"), palette, colonnes);
    }
}
