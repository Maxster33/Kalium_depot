package fr.kalium.anticheat;

import fr.kalium.anticheat.Alertes.Gravite;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * X-ray (cahier, catégorie 6, étape 2) : ne comptent que les minerais rares (diamant, émeraude, débris antiques)
 * **cachés** au moment où le joueur les atteint : au plus un bloc d'air autour (le tunnel par lequel il arrive), ni
 * eau ni lave ; un mineur de grotte (minerai à l'air libre) n'est pas compté. Un filon = une fois (minerais voisins
 * cassés à la suite). Comparé à la roche minée (pierre, deepslate, netherrack, basalte...).
 *
 * Par heure (heures pleines, 24 dernières gardées dans minage.yml) :
 * - alerte légère si filons cachés >= xray.alerte-filons ET taux >= xray.alerte-taux (pour 1 000 blocs de roche) ;
 * - heure « extrême » si filons >= xray.extreme-filons ET taux >= xray.extreme-taux ;
 * - infraction grave (suspension) si xray.extreme-heures heures extrêmes dans les xray.fenetre-heures dernières heures
 *   (« sur plusieurs heures » : on ne suspend pas un joueur chanceux une fois).
 * Onglet « Minage » de la rubrique Modération : joueurs des dernières 24 h, du plus suspect au moins suspect.
 */
final class Minage implements Listener {

    static final Set<Material> RARES = EnumSet.of(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.ANCIENT_DEBRIS);
    private static final Set<Material> ROCHE = EnumSet.of(Material.STONE, Material.DEEPSLATE, Material.TUFF,
            Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.NETHERRACK, Material.BASALT,
            Material.BLACKSTONE, Material.CALCITE, Material.SMOOTH_BASALT);
    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST};

    /** Une heure d'un joueur. */
    static final class Heure {
        int roche;
        int filons;
        boolean alerte;
        boolean extreme;
    }

    private final KSAntiCheat plugin;
    private final File fichier;
    /** Joueur -> heure (numéro d'heure depuis 1970) -> compteurs. */
    private final Map<UUID, Map<Long, Heure>> heures = new HashMap<>();
    private final Map<UUID, String> noms = new HashMap<>();
    /** Dernier minerai rare caché compté (filon en cours) : lieu et date. */
    private final Map<UUID, Location> dernierFilon = new HashMap<>();
    private final Map<UUID, Long> dateDernierFilon = new HashMap<>();
    private boolean modifie;

    Minage(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "minage.yml");
        charger();
    }

    private static long heureActuelle() {
        return System.currentTimeMillis() / 3_600_000L;
    }

    private Heure heure(UUID joueur) {
        return heures.computeIfAbsent(joueur, u -> new LinkedHashMap<>()).computeIfAbsent(heureActuelle(), h -> new Heure());
    }

    /** Minerai caché : au plus un bloc d'air autour, ni eau ni lave. */
    private static boolean cache(Block bloc) {
        int air = 0;
        for (BlockFace f : FACES) {
            Material t = bloc.getRelative(f).getType();
            if (t == Material.WATER || t == Material.LAVA) {
                return false;
            }
            if (t.isAir()) {
                air++;
            }
        }
        return air <= 1;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        Block bloc = event.getBlock();
        Material type = bloc.getType();
        UUID u = p.getUniqueId();
        if (ROCHE.contains(type)) {
            heure(u).roche++;
            modifie = true;
            return;
        }
        if (!RARES.contains(type)) {
            return;
        }
        // Même filon : minerai rare voisin (3 blocs) cassé moins de 30 s après le précédent.
        Location ici = bloc.getLocation();
        Location avant = dernierFilon.get(u);
        long maintenant = System.currentTimeMillis();
        boolean memeFilon = avant != null && avant.getWorld() == ici.getWorld() && avant.distanceSquared(ici) <= 9
                && maintenant - dateDernierFilon.getOrDefault(u, 0L) < 30_000;
        dernierFilon.put(u, ici);
        dateDernierFilon.put(u, maintenant);
        if (memeFilon || !cache(bloc)) {
            return;
        }
        noms.put(u, p.getName());
        Heure h = heure(u);
        h.filons++;
        modifie = true;
        verifier(u, p.getName(), h);
    }

    private double taux(Heure h) {
        return h.filons * 1000.0 / Math.max(h.roche, 1);
    }

    private void verifier(UUID u, String nom, Heure h) {
        var c = plugin.getConfig();
        double t = taux(h);
        if (!h.alerte && h.filons >= c.getInt("xray.alerte-filons", 5) && t >= c.getDouble("xray.alerte-taux", 3)) {
            h.alerte = true;
            plugin.alertes().ajouter(u, nom, "X-ray ?", Gravite.LEGERE, h.filons + " filons rares cachés cette heure pour "
                    + h.roche + " blocs de roche (" + String.format("%.1f", t) + " pour 1 000)");
        }
        if (!h.extreme && h.filons >= c.getInt("xray.extreme-filons", 10) && t >= c.getDouble("xray.extreme-taux", 8)) {
            h.extreme = true;
            int fenetre = Math.max(1, c.getInt("xray.fenetre-heures", 6));
            int requis = Math.max(1, c.getInt("xray.extreme-heures", 3));
            long maintenant = heureActuelle();
            int extremes = 0;
            for (Map.Entry<Long, Heure> e : heures.get(u).entrySet()) {
                if (maintenant - e.getKey() < fenetre && e.getValue().extreme) {
                    extremes++;
                }
            }
            if (extremes >= requis && !plugin.suspensions().suspendu(u)) {
                plugin.infractionGrave(u, nom, "X-ray", extremes + " heures extrêmes sur les " + fenetre
                        + " dernières (cette heure : " + h.filons + " filons cachés, " + String.format("%.1f", t)
                        + " pour 1 000 blocs de roche)");
            } else {
                plugin.alertes().ajouter(u, nom, "X-ray (heure extrême)", Gravite.LEGERE, extremes + " / " + requis
                        + " heures extrêmes sur " + fenetre + " h (" + h.filons + " filons cachés, "
                        + String.format("%.1f", t) + " pour 1 000)");
            }
        }
    }

    /** Résumé des dernières 24 h, du plus suspect au moins suspect (onglet Minage). */
    List<String> resume24h() {
        long maintenant = heureActuelle();
        List<Object[]> lignes = new ArrayList<>();
        heures.forEach((u, parHeure) -> {
            int roche = 0;
            int filons = 0;
            int extremes = 0;
            for (Map.Entry<Long, Heure> e : parHeure.entrySet()) {
                if (maintenant - e.getKey() < 24) {
                    roche += e.getValue().roche;
                    filons += e.getValue().filons;
                    extremes += e.getValue().extreme ? 1 : 0;
                }
            }
            if (filons > 0) {
                double t = filons * 1000.0 / Math.max(roche, 1);
                lignes.add(new Object[]{t, noms.getOrDefault(u, "?") + " : " + filons + " filons cachés, " + roche
                        + " roche, " + String.format("%.1f", t) + " pour 1 000" + (extremes > 0 ? ", " + extremes
                        + " h extrême(s)" : "")});
            }
        });
        lignes.sort((a, b) -> Double.compare((double) b[0], (double) a[0]));
        List<String> r = new ArrayList<>();
        for (Object[] l : lignes) {
            r.add((String) l[1]);
        }
        return r;
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection js = yaml.getConfigurationSection("joueurs");
        if (js == null) {
            return;
        }
        for (String cle : js.getKeys(false)) {
            try {
                UUID u = UUID.fromString(cle);
                noms.put(u, js.getString(cle + ".nom", "?"));
                ConfigurationSection hs = js.getConfigurationSection(cle + ".heures");
                if (hs == null) {
                    continue;
                }
                Map<Long, Heure> m = heures.computeIfAbsent(u, x -> new LinkedHashMap<>());
                for (String hc : hs.getKeys(false)) {
                    Heure h = new Heure();
                    h.roche = hs.getInt(hc + ".roche");
                    h.filons = hs.getInt(hc + ".filons");
                    h.alerte = hs.getBoolean(hc + ".alerte");
                    h.extreme = hs.getBoolean(hc + ".extreme");
                    m.put(Long.parseLong(hc), h);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Minage ignoré : " + cle);
            }
        }
    }

    /** Toutes les 30 s et à l'arrêt ; heures de plus de 24 h oubliées. */
    void sauver() {
        if (!modifie) {
            return;
        }
        modifie = false;
        long maintenant = heureActuelle();
        YamlConfiguration yaml = new YamlConfiguration();
        heures.forEach((u, m) -> {
            m.keySet().removeIf(h -> maintenant - h >= 24);
            m.forEach((h, v) -> {
                String b = "joueurs." + u + ".heures." + h;
                yaml.set(b + ".roche", v.roche);
                yaml.set(b + ".filons", v.filons);
                yaml.set(b + ".alerte", v.alerte);
                yaml.set(b + ".extreme", v.extreme);
            });
            if (!m.isEmpty()) {
                yaml.set("joueurs." + u + ".nom", noms.getOrDefault(u, "?"));
            }
        });
        heures.values().removeIf(Map::isEmpty);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer minage.yml : " + e.getMessage());
        }
    }
}
