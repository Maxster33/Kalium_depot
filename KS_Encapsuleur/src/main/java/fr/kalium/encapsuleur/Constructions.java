package fr.kalium.encapsuleur;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.structure.Structure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Les constructions encapsulées : un fichier de structure du jeu par construction
 * (plugins/KS_Encapsuleur/constructions/<id>.nbt, blocs et données de leurs blocs, sans les entités), avec dans ses
 * données : les blocs restés sur place (jamais posés : leur contenu est resté dans le monde), les blocs custom de
 * KS_Blocs, la face avant et le propriétaire. Une construction posée est rangée dans constructions/posees/.
 */
final class Constructions {

    private static final String ESPACE = "ks_encapsuleur";
    static final NamespacedKey RESTANTS = new NamespacedKey(ESPACE, "restants");
    static final NamespacedKey CUSTOM = new NamespacedKey(ESPACE, "custom");
    static final NamespacedKey AVANT = new NamespacedKey(ESPACE, "avant");
    static final NamespacedKey PROPRIO = new NamespacedKey(ESPACE, "proprio");
    static final NamespacedKey BLOCS = new NamespacedKey(ESPACE, "blocs");

    private Constructions() {
    }

    static File fichier(KSEncapsuleur plugin, String id) {
        return new File(new File(plugin.getDataFolder(), "constructions"), id + ".nbt");
    }

    static void sauver(KSEncapsuleur plugin, String id, Structure construction) throws IOException {
        File f = fichier(plugin, id);
        f.getParentFile().mkdirs();
        Bukkit.getStructureManager().saveStructure(f, construction);
        // Les mêmes données dans <id>.yml : la liste des blocs restés sur place ne doit jamais se perdre (leur contenu
        // est dans le fichier de structure, et il est resté dans le monde).
        PersistentDataContainer d = construction.getPersistentDataContainer();
        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
        int[] r = restants(construction);
        List<Integer> liste = new ArrayList<>();
        for (int v : r) {
            liste.add(v);
        }
        y.set("restants", liste);
        y.set("custom", d.getOrDefault(CUSTOM, PersistentDataType.STRING, ""));
        y.set("avant", d.getOrDefault(AVANT, PersistentDataType.STRING, "SOUTH"));
        y.set("proprio", d.getOrDefault(PROPRIO, PersistentDataType.STRING, ""));
        y.set("blocs", d.getOrDefault(BLOCS, PersistentDataType.INTEGER, 0));
        y.save(donnees(f));
    }

    private static File donnees(File nbt) {
        return new File(nbt.getParentFile(), nbt.getName().replace(".nbt", ".yml"));
    }

    /** La construction de cet id, ou null (absente, déjà posée, illisible). */
    static Structure charger(KSEncapsuleur plugin, String id) {
        if (id == null || !id.matches("[0-9a-f-]{36}")) {
            return null;
        }
        File f = fichier(plugin, id);
        if (!f.exists()) {
            return null;
        }
        File yml = donnees(f);
        if (!yml.exists()) {
            plugin.getLogger().severe("Construction " + id + " sans " + yml.getName() + " : refusée.");
            return null;
        }
        try {
            Structure construction = Bukkit.getStructureManager().loadStructure(f);
            org.bukkit.configuration.file.YamlConfiguration y =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(yml);
            noter(construction, y.getIntegerList("restants").stream().mapToInt(Integer::intValue).toArray(),
                    y.getString("custom", ""), y.getString("avant", "SOUTH"), y.getString("proprio", ""),
                    y.getInt("blocs"));
            return construction;
        } catch (IOException | RuntimeException erreur) {
            plugin.getLogger().severe("Construction " + id + " illisible : " + erreur);
            return null;
        }
    }

    /** Construction posée : rangée dans constructions/posees/ (jamais supprimée). */
    static void archiver(KSEncapsuleur plugin, String id) {
        File f = fichier(plugin, id);
        File dest = new File(new File(f.getParentFile(), "posees"), f.getName());
        try {
            dest.getParentFile().mkdirs();
            Files.move(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File yml = donnees(f);
            if (yml.exists()) {
                Files.move(yml.toPath(), donnees(dest).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException erreur) {
            plugin.getLogger().severe("Construction " + id + " posée mais pas rangée : " + erreur);
        }
    }

    static int[] restants(Structure construction) {
        int[] r = construction.getPersistentDataContainer().get(RESTANTS, PersistentDataType.INTEGER_ARRAY);
        return r == null ? new int[0] : r;
    }

    /** Blocs custom : positions relatives (x, y, z) et « type|variante ». */
    static void custom(Structure construction, List<int[]> positions, List<String> ids) {
        String texte = construction.getPersistentDataContainer().get(CUSTOM, PersistentDataType.STRING);
        if (texte == null || texte.isEmpty()) {
            return;
        }
        for (String ligne : texte.split(";")) {
            String[] p = ligne.split(",", 4);
            if (p.length == 4) {
                positions.add(new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])});
                ids.add(p[3]);
            }
        }
    }

    static void noter(Structure construction, int[] restants, String custom, String avant, String proprio,
                      int blocs) {
        PersistentDataContainer d = construction.getPersistentDataContainer();
        d.set(RESTANTS, PersistentDataType.INTEGER_ARRAY, restants);
        d.set(CUSTOM, PersistentDataType.STRING, custom);
        d.set(AVANT, PersistentDataType.STRING, avant);
        d.set(PROPRIO, PersistentDataType.STRING, proprio);
        d.set(BLOCS, PersistentDataType.INTEGER, blocs);
    }

    /**
     * Blocs à poser, de bas en haut, eau et lave à la fin : ni l'air, ni l'eau qui coule, ni les blocs restés sur place
     * à l'encapsulage.
     */
    static List<BlockState> aPoser(Structure construction) {
        int[] r = restants(construction);
        Set<Long> restes = new HashSet<>();
        for (int i = 0; i + 2 < r.length; i += 3) {
            restes.add(Chantiers.cle(r[i], r[i + 1], r[i + 2]));
        }
        List<BlockState> blocs = new ArrayList<>();
        if (construction.getPaletteCount() == 0) {
            return blocs;
        }
        for (BlockState b : construction.getPalettes().get(0).getBlocks()) {
            Material type = b.getType();
            if (type.isAir() || type == Material.STRUCTURE_VOID || restes.contains(Chantiers.cle(b.getX(), b.getY(), b.getZ()))) {
                continue;
            }
            if ((type == Material.WATER || type == Material.LAVA)
                    && !(b.getBlockData() instanceof Levelled l && l.getLevel() == 0)) {
                continue;
            }
            blocs.add(b);
        }
        blocs.sort(Comparator.comparingInt((BlockState b) -> fluide(b) ? 1 : 0).thenComparingInt(BlockState::getY)
                .thenComparingInt(BlockState::getX).thenComparingInt(BlockState::getZ));
        return blocs;
    }

    private static boolean fluide(BlockState b) {
        return b.getType() == Material.WATER || b.getType() == Material.LAVA;
    }
}
