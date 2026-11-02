package fr.kalium.encapsuleur;

import fr.kalium.encapsuleur.Analyse.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.structure.Structure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Les chantiers en cours, petit à petit (comme la lave de KG_Parkour) et repris après un redémarrage (chantiers.yml) :
 * - retrait : les blocs emportés sont enlevés de haut en bas, sans mise à jour des voisins (rien ne tombe ni ne se
 *   lâche) ; pendant le retrait, la zone est protégée (rien ne s'y casse, ne s'y pose, ne s'y ouvre, ni n'en sort par
 *   entonnoir), pour qu'aucun bloc ou objet ne soit à la fois dans la construction et dans le monde ;
 * - pose : les blocs sont posés de bas en haut, eau et lave à la fin (elles recoulent), puis les blocs custom de
 *   KS_Blocs sont retenus à leur nouvelle place.
 */
final class Chantiers implements Listener {

    static final int RETRAIT_PAR_TICK = 4000;
    static final int POSE_PAR_TICK = 1500;

    private final KSEncapsuleur plugin;
    private final File fichier;
    private final Map<String, Chantier> enCours = new ConcurrentHashMap<>();

    private abstract static class Chantier {
        final String id;
        int index;

        Chantier(String id, int index) {
            this.id = id;
            this.index = index;
        }

        /** Avance d'un pas ; vrai quand c'est fini. */
        abstract boolean avancer();

        abstract void noter(ConfigurationSection s);
    }

    Chantiers(KSEncapsuleur plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "chantiers.yml");
    }

    // ------------------------------------------------------------------ retrait

    private final class Retrait extends Chantier {
        final Zone zone;
        final Set<Long> restants = new HashSet<>();
        final List<int[]> custom;

        Retrait(String id, Zone zone, int[] restantsRelatifs, List<int[]> custom, int index) {
            super(id, index);
            this.zone = zone;
            for (int i = 0; i + 2 < restantsRelatifs.length; i += 3) {
                restants.add(cle(restantsRelatifs[i], restantsRelatifs[i + 1], restantsRelatifs[i + 2]));
            }
            this.custom = custom;
        }

        @Override
        boolean avancer() {
            if (index == 0) {
                // Blocs custom oubliés par KS_BLocs d'abord (leurs blocs du serveur sont enlevés ensuite).
                for (int[] c : custom) {
                    Block b = zone.monde().getBlockAt(zone.x1() + c[0], zone.y1() + c[1], zone.z1() + c[2]);
                    Integration.retirerCustom(b);
                }
            }
            long volume = zone.volume();
            int faits = 0;
            int etage = zone.sx() * zone.sz();
            while (index < volume && faits < RETRAIT_PAR_TICK) {
                // De haut en bas : index 0 = étage du haut.
                int y = zone.sy() - 1 - index / etage;
                int reste = index % etage;
                int x = reste / zone.sz();
                int z = reste % zone.sz();
                index++;
                if (restants.contains(cle(x, y, z))) {
                    continue;
                }
                Block b = zone.monde().getBlockAt(zone.x1() + x, zone.y1() + y, zone.z1() + z);
                if (!b.getType().isAir()) {
                    b.setType(Material.AIR, false);
                    faits++;
                }
            }
            return index >= volume;
        }

        @Override
        void noter(ConfigurationSection s) {
            s.set("type", "retrait");
            s.set("monde", zone.monde().getName());
            s.set("zone", List.of(zone.x1(), zone.y1(), zone.z1(), zone.x2(), zone.y2(), zone.z2()));
        }
    }

    static long cle(int x, int y, int z) {
        return ((long) x << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    /** Commence le retrait d'une zone encapsulée. */
    void retirer(String id, Zone zone, int[] restants, List<int[]> custom) {
        lancer(new Retrait(id, zone, restants, custom, 0));
    }

    /** Un chantier de cette construction est en cours. */
    boolean enCours(String id) {
        return enCours.values().stream().anyMatch(c -> c.id.equals(id));
    }

    /** Une zone en cours de retrait contient ce bloc. */
    boolean protege(Block b) {
        for (Chantier c : enCours.values()) {
            if (c instanceof Retrait r && r.zone.contient(b)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ pose

    private final class Pose extends Chantier {
        final World monde;
        final int ox, oy, oz;
        final StructureRotation rotation;
        final Mirror miroir;
        final List<BlockState> blocs;
        final Forme forme;
        final List<int[]> custom;
        final List<String> customIds;

        Pose(String id, World monde, int ox, int oy, int oz, StructureRotation rotation, Mirror miroir, int index,
             Structure construction) {
            super(id, index);
            this.monde = monde;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.rotation = rotation;
            this.miroir = miroir;
            this.forme = new Forme(construction.getSize().getBlockX(), construction.getSize().getBlockY(),
                    construction.getSize().getBlockZ(), rotation, miroir);
            this.blocs = Constructions.aPoser(construction);
            this.custom = new ArrayList<>();
            this.customIds = new ArrayList<>();
            Constructions.custom(construction, custom, customIds);
        }

        @Override
        boolean avancer() {
            int faits = 0;
            while (index < blocs.size() && faits < POSE_PAR_TICK) {
                BlockState modele = blocs.get(index++);
                int[] p = forme.position(modele.getX(), modele.getY(), modele.getZ());
                Location lieu = new Location(monde, ox + p[0], oy + p[1], oz + p[2]);
                BlockState etat = modele.copy(lieu);
                BlockData donnees = etat.getBlockData().clone();
                donnees.mirror(miroir);
                donnees.rotate(rotation);
                etat.setBlockData(donnees);
                boolean fluide = donnees.getMaterial() == Material.WATER || donnees.getMaterial() == Material.LAVA;
                etat.update(true, fluide);
                faits++;
            }
            if (index < blocs.size()) {
                return false;
            }
            for (int i = 0; i < custom.size(); i++) {
                int[] c = custom.get(i);
                int[] p = forme.position(c[0], c[1], c[2]);
                Integration.poserCustom(monde.getBlockAt(ox + p[0], oy + p[1], oz + p[2]), customIds.get(i));
            }
            return true;
        }

        @Override
        void noter(ConfigurationSection s) {
            s.set("type", "pose");
            s.set("monde", monde.getName());
            s.set("origine", List.of(ox, oy, oz));
            s.set("rotation", rotation.name());
            s.set("miroir", miroir.name());
        }
    }

    void poser(String id, World monde, int ox, int oy, int oz, StructureRotation rotation, Mirror miroir,
               Structure construction) {
        lancer(new Pose(id, monde, ox, oy, oz, rotation, miroir, 0, construction));
    }

    // ------------------------------------------------------------------ déroulement

    private void lancer(Chantier c) {
        enCours.put(c.id + ":" + c.getClass().getSimpleName(), c);
        enregistrer();
        if (c.avancer()) {
            finir(c);
        }
    }

    void tick() {
        for (Map.Entry<String, Chantier> e : List.copyOf(enCours.entrySet())) {
            try {
                if (e.getValue().avancer()) {
                    finir(e.getValue());
                }
            } catch (RuntimeException erreur) {
                plugin.getLogger().severe("Chantier " + e.getKey() + " en erreur : " + erreur);
                enCours.remove(e.getKey());
                enregistrer();
            }
        }
    }

    private int compteur;

    /** Avancement noté toutes les 5 s (reprise après un redémarrage). */
    void noterAvancement() {
        if (!enCours.isEmpty() && ++compteur % 5 == 0) {
            enregistrer();
        }
    }

    private void finir(Chantier c) {
        enCours.remove(c.id + ":" + c.getClass().getSimpleName());
        enregistrer();
        if (c instanceof Pose) {
            Constructions.archiver(plugin, c.id);
        }
    }

    void enregistrer() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Chantier> e : enCours.entrySet()) {
            ConfigurationSection s = y.createSection(e.getKey().replace(':', '_'));
            s.set("id", e.getValue().id);
            s.set("index", e.getValue().index);
            e.getValue().noter(s);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(fichier);
        } catch (IOException erreur) {
            plugin.getLogger().severe("chantiers.yml non enregistré : " + erreur);
        }
    }

    /** Reprise des chantiers interrompus (arrêt du serveur). */
    void reprendre() {
        if (!fichier.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(fichier);
        for (String k : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(k);
            String id = s.getString("id");
            World monde = Bukkit.getWorld(s.getString("monde", ""));
            Structure construction = Constructions.charger(plugin, id);
            if (monde == null || construction == null) {
                plugin.getLogger().severe("Chantier " + k + " impossible à reprendre (monde ou construction absents).");
                continue;
            }
            Chantier c;
            if ("retrait".equals(s.getString("type"))) {
                List<Integer> z = s.getIntegerList("zone");
                List<int[]> custom = new ArrayList<>();
                Constructions.custom(construction, custom, new ArrayList<>());
                c = new Retrait(id, new Zone(monde, z.get(0), z.get(1), z.get(2), z.get(3), z.get(4), z.get(5)),
                        Constructions.restants(construction), custom, 0);
            } else {
                List<Integer> o = s.getIntegerList("origine");
                c = new Pose(id, monde, o.get(0), o.get(1), o.get(2),
                        StructureRotation.valueOf(s.getString("rotation", "NONE")),
                        Mirror.valueOf(s.getString("miroir", "NONE")), Math.max(0, s.getInt("index") - POSE_PAR_TICK),
                        construction);
            }
            enCours.put(c.id + ":" + c.getClass().getSimpleName(), c);
            plugin.getLogger().info("Chantier repris : " + k);
        }
    }

    /** Arrêt du serveur : on va au bout des retraits en cours (sinon la zone ne serait plus protégée). */
    void arreter() {
        for (Chantier c : List.copyOf(enCours.values())) {
            if (c instanceof Retrait) {
                while (!c.avancer()) {
                    // jusqu'au bout
                }
                finir(c);
            }
        }
        enregistrer();
    }

    // ------------------------------------------------------------------ protection pendant le retrait

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCasser(BlockBreakEvent event) {
        if (protege(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPoser(BlockPlaceEvent event) {
        if (protege(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteragir(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && protege(event.getClickedBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::protege);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplosionBloc(BlockExplodeEvent event) {
        event.blockList().removeIf(this::protege);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonPousse(BlockPistonExtendEvent event) {
        if (protege(event.getBlock()) || event.getBlocks().stream().anyMatch(this::protege)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonTire(BlockPistonRetractEvent event) {
        if (protege(event.getBlock()) || event.getBlocks().stream().anyMatch(this::protege)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntiteBloc(EntityChangeBlockEvent event) {
        if (protege(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCouler(BlockFromToEvent event) {
        if (protege(event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntonnoir(InventoryMoveItemEvent event) {
        if (dansZone(event.getSource()) || dansZone(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean dansZone(Inventory inventaire) {
        if (enCours.isEmpty() || inventaire == null) {
            return false;
        }
        Location lieu = inventaire.getLocation();
        return lieu != null && lieu.getWorld() != null && protege(lieu.getBlock());
    }

    static Comparator<BlockState> basEnHaut() {
        return Comparator.comparingInt(BlockState::getY);
    }
}
