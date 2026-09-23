package fr.kalium.games.world;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.BitSet;

/**
 * Monde vide unique dans lequel toutes les parties sont collees a des emplacements distincts.
 * Chaque arene a sa propre ZONE (une rangee de la grille, propre a l'arene) : les parties d'une meme arene
 * se suivent le long de cette rangee et deux arenes differentes ne partagent jamais le meme espace.
 * Le monde n'est normalement PAS sauvegarde sur le disque (autosave desactive) : il est explicitement sauvegarde
 * et marque "propre" a un arret normal du plugin (voir {@link #markClean()}), pour que les arenes deja collees
 * (voir ArenaPool) survivent a un redemarrage au lieu d'etre re-generees a chaque fois. En cas d'arret brutal
 * (crash, kill), aucun marqueur "propre" n'est present au demarrage suivant : le monde est alors efface par
 * securite (etat potentiellement corrompu), comme avant.
 */
public final class InstanceWorld {

    private final JavaPlugin plugin;
    private final BitSet used = new BitSet();
    private World world;
    private int spacing;
    /** Nombre maximal de parties simultanees d'une meme arene (longueur d'une zone). */
    private int columns;
    private int base;
    /** Nombre maximal de parties simultanees, toutes arenes confondues. */
    private int max;
    /** Zone (numero de rangee) de chaque arene, attribuee a la premiere partie (puis restauree au redemarrage). */
    private final java.util.Map<String, Integer> zones = new java.util.LinkedHashMap<>();
    private int nextZone;
    /** Vrai si un etat sauvegarde valide (arret propre, meme disposition de grille) a ete restaure a ce demarrage. */
    private boolean cleanRestart;

    public InstanceWorld(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Cree ou charge le monde des instances. A appeler une fois le serveur demarre. */
    public boolean init() {
        // Chaque emplacement est assez grand pour la plus grande arene permise (arenas.max-size) PLUS la distance
        // minimale (instances.min-distance, 1000 blocs par defaut) : deux parties sont toujours a au moins
        // cette distance l'une de l'autre, quelles que soient les arenes utilisees.
        int minDistance = Math.max(0, plugin.getConfig().getInt("instances.min-distance", 1000));
        int needed = TemplateService.maxSize(plugin) + minDistance;
        spacing = (Math.max(Math.max(256, plugin.getConfig().getInt("instances.spacing", 1536)), needed) + 15) & ~15;
        columns = Math.max(1, plugin.getConfig().getInt("instances.per-arena", plugin.getConfig().getInt("instances.columns", 10)));
        base = plugin.getConfig().getInt("instances.base", 262144) & ~15;
        max = Math.max(1, plugin.getConfig().getInt("instances.max", 40));

        NamespacedKey key = new NamespacedKey(plugin, "instances");
        World existing = Bukkit.getWorld(key);
        if (existing != null) {
            world = existing;
        } else {
            WorldCreator creator = WorldCreator.ofKey(key)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false);
            world = Bukkit.createWorld(creator);
        }
        if (world == null) {
            return false;
        }
        // "Propre" seulement si le dernier arret a ecrit le marqueur ET que la disposition de la grille (qui
        // determine les coordonnees de chaque emplacement) n'a pas change depuis : sinon les blocs restes sur
        // disque ne correspondraient plus forcement a la bonne arene.
        String signature = layoutSignature();
        cleanRestart = signature.equals(readMarker(world));
        deleteMarker(world);
        if (!cleanRestart && purgeStoredData(world)) {
            // Arret brutal (ou disposition changee) : les blocs restes sur disque sont effaces, monde vierge recree.
            Bukkit.unloadWorld(world, false);
            World fresh = Bukkit.getWorld(key);
            if (fresh == null) {
                fresh = Bukkit.createWorld(WorldCreator.ofKey(key)
                        .generator(new VoidGenerator())
                        .environment(World.Environment.NORMAL)
                        .generateStructures(false));
            }
            if (fresh == null) {
                return false;
            }
            world = fresh;
        }
        configure(world);
        if (cleanRestart) {
            loadZones();
            plugin.getLogger().info("Monde des instances restauré (arrêt précédent propre) : arènes déjà collées conservées.");
        }
        return true;
    }

    /** Vrai si un etat sauvegarde valide (memes emplacements) a ete restaure : l'ArenaPool peut recharger ses copies. */
    public boolean cleanRestart() {
        return cleanRestart;
    }

    /**
     * Sauvegarde le monde sur disque et marque l'arret comme "propre" : au prochain demarrage, les arenes deja
     * collees seront conservees au lieu d'etre effacees. A appeler une seule fois, a l'extinction du plugin.
     */
    public void markClean() {
        if (world == null) {
            return;
        }
        try {
            saveZones();
            world.save();
            File marker = markerFile(world);
            marker.getParentFile().mkdirs();
            Files.writeString(marker.toPath(), layoutSignature(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("Sauvegarde du monde des instances impossible : " + e.getMessage());
        }
    }

    /** Identifie la disposition de la grille : si elle change (config modifiee), les anciens emplacements ne sont plus fiables. */
    private String layoutSignature() {
        return spacing + ";" + columns + ";" + base;
    }

    private File markerFile(World w) {
        return new File(w.getWorldFolder(), "kalgames-clean-shutdown.txt");
    }

    private String readMarker(World w) {
        try {
            File marker = markerFile(w);
            return marker.exists() ? Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim() : "";
        } catch (IOException e) {
            return "";
        }
    }

    private void deleteMarker(World w) {
        markerFile(w).delete();
    }

    private File zonesFile() {
        return new File(plugin.getDataFolder(), "instances-zones.yml");
    }

    /** Recharge l'attribution zone <-> arene (numeros stables entre redemarrages, tant que la disposition ne change pas). */
    private void loadZones() {
        File file = zonesFile();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            int zone = cfg.getInt(key, -1);
            if (zone < 0) {
                continue;
            }
            zones.put(key, zone);
            nextZone = Math.max(nextZone, zone + 1);
        }
    }

    private void saveZones() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (java.util.Map.Entry<String, Integer> entry : zones.entrySet()) {
            cfg.set(entry.getKey(), entry.getValue());
        }
        try {
            cfg.save(zonesFile());
        } catch (IOException e) {
            plugin.getLogger().warning("Sauvegarde des zones d'instances impossible : " + e.getMessage());
        }
    }

    /** Supprime region / entities / poi du monde des instances. Renvoie true si quelque chose a ete supprime. */
    private boolean purgeStoredData(World w) {
        try {
            File folder = w.getWorldFolder();
            if (!folder.getCanonicalPath().contains("instances")) {
                return false;
            }
            boolean removed = false;
            for (File dir : findDataDirs(folder, 0)) {
                deleteRecursively(dir);
                removed = true;
            }
            return removed;
        } catch (Exception e) {
            plugin.getLogger().warning("Nettoyage du monde des instances impossible : " + e.getMessage());
            return false;
        }
    }

    private java.util.List<File> findDataDirs(File dir, int depth) {
        java.util.List<File> result = new java.util.ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null || depth > 3) {
            return result;
        }
        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }
            String name = child.getName();
            if (name.equals("region") || name.equals("entities") || name.equals("poi")) {
                File[] files = child.listFiles();
                if (files != null && files.length > 0) {
                    result.add(child);
                }
            } else {
                result.addAll(findDataDirs(child, depth + 1));
            }
        }
        return result;
    }

    private void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    /** Emplacement (zone * columns + colonne) qui contient ce point, ou -1. */
    public int slotAt(int x, int z) {
        if (x < base || z < base) {
            return -1;
        }
        int col = (x - base) / spacing;
        int row = (z - base) / spacing;
        if (col >= columns || row >= nextZone) {
            return -1;
        }
        return row * columns + col;
    }

    /** Numero de zone (rangee) de l'arene : attribue a la premiere utilisation, puis stable jusqu'au redemarrage. */
    public synchronized int zoneOf(String arenaId) {
        Integer zone = zones.get(arenaId);
        if (zone == null) {
            zone = nextZone++;
            zones.put(arenaId, zone);
            plugin.getLogger().info("Zone d'instances de l'arène " + arenaId + " : z=" + slotZ(zone * columns)
                    + " (x de " + base + " a " + (base + columns * spacing) + ").");
        }
        return zone;
    }

    @SuppressWarnings("deprecation")
    private void configure(World w) {
        w.setAutoSave(false);
        w.setDifficulty(Difficulty.NORMAL);
        w.setPVP(true);
        w.setTime(6000L);
        w.setStorm(false);
        w.setSpawnLocation(0, 80, 0);
        w.setGameRule(GameRules.ADVANCE_TIME, false);
        w.setGameRule(GameRules.ADVANCE_WEATHER, false);
        w.setGameRule(GameRules.SPAWN_MOBS, false);
        w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        w.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        w.setGameRule(GameRules.MOB_GRIEFING, false);
        w.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
    }

    public World world() {
        return world;
    }

    public boolean ready() {
        return world != null;
    }

    public int spacing() {
        return spacing;
    }

    /**
     * Reserve un emplacement libre dans la zone de l'arene (-1 si la zone de l'arene est pleine). Le nombre maximal de
     * parties simultanees (instances.max) est verifie par le gestionnaire de parties.
     */
    public synchronized int allocate(String arenaId) {
        int zone = zoneOf(arenaId);
        for (int col = 0; col < columns; col++) {
            int slot = zone * columns + col;
            if (!used.get(slot)) {
                used.set(slot);
                return slot;
            }
        }
        return -1;
    }

    public synchronized void release(int slot) {
        if (slot >= 0) {
            used.clear(slot);
        }
    }

    /** Marque directement un emplacement comme occupe (restauration d'une copie d'arene deja collee). */
    public synchronized void markUsed(int slot) {
        if (slot >= 0) {
            used.set(slot);
        }
    }

    public synchronized int usedSlots() {
        return used.cardinality();
    }

    public int maxSlots() {
        return max;
    }

    public int slotX(int slot) {
        return base + (slot % columns) * spacing;
    }

    public int slotZ(int slot) {
        return base + (slot / columns) * spacing;
    }
}
