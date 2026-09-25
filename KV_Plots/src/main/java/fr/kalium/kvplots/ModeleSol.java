package fr.kalium.kvplots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Colonne de sol d'un plot vierge (du bas au haut du monde), relevée une fois au centre du plot (0, 0) puis gardée
 * dans modele-sol.yml. Sert à remplacer les routes intérieures d'un grand plot par du sol identique à celui des plots.
 */
final class ModeleSol {

    private final int yMin;
    /** Bloc de chaque hauteur, à partir de yMin ; null = air. */
    private final BlockData[] blocs;

    private ModeleSol(int yMin, BlockData[] blocs) {
        this.yMin = yMin;
        this.blocs = blocs;
    }

    int hauteur() {
        return blocs.length;
    }

    static ModeleSol relever(World monde, int x, int z) {
        int yMin = monde.getMinHeight();
        BlockData[] blocs = new BlockData[monde.getMaxHeight() - yMin];
        for (int i = 0; i < blocs.length; i++) {
            Block b = monde.getBlockAt(x, yMin + i, z);
            blocs[i] = b.getType().isAir() ? null : b.getBlockData();
        }
        return new ModeleSol(yMin, blocs);
    }

    /** Pose la colonne en (x, z) ; ne touche pas aux blocs déjà identiques. */
    void poser(World monde, int x, int z) {
        for (int i = 0; i < blocs.length; i++) {
            Block b = monde.getBlockAt(x, yMin + i, z);
            BlockData voulu = blocs[i];
            if (voulu == null) {
                if (!b.getType().isAir()) b.setType(Material.AIR, false);
            } else if (b.getType() != voulu.getMaterial() || !b.getBlockData().equals(voulu)) {
                b.setBlockData(voulu, false);
            }
        }
    }

    /** Format : liste de « y:bloc » pour les blocs qui ne sont pas de l'air. */
    void sauver(File fichier) throws IOException {
        List<String> lignes = new ArrayList<>();
        for (int i = 0; i < blocs.length; i++) {
            if (blocs[i] != null) lignes.add((yMin + i) + ":" + blocs[i].getAsString());
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("y-min", yMin);
        yml.set("hauteur", blocs.length);
        yml.set("colonne", lignes);
        yml.save(fichier);
    }

    static ModeleSol charger(File fichier) {
        if (!fichier.isFile()) return null;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        int yMin = yml.getInt("y-min");
        BlockData[] blocs = new BlockData[yml.getInt("hauteur")];
        for (String ligne : yml.getStringList("colonne")) {
            int sep = ligne.indexOf(':');
            int y = Integer.parseInt(ligne.substring(0, sep));
            blocs[y - yMin] = Bukkit.createBlockData(ligne.substring(sep + 1));
        }
        return new ModeleSol(yMin, blocs);
    }
}
