package fr.kalium.anticheat;

import fr.kalium.anticheat.Alertes.Gravite;
import org.bukkit.GameMode;
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
import org.bukkit.event.block.BlockPlaceEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Minage / x-ray (cahier, catégorie 6 ; 1.1.0 : retours des tests de Maxster33, LeKiwi06 03/10/2026 : « inclure tous
 * les minerais avec une catégorie par minerai », « un score par minerai pas par filon », « compter ceux cassés avec la
 * fortune et la délicatesse, et même sans enchantement »).
 *
 * - Chaque minerai cassé compte (quel que soit l'outil), rangé par catégorie (charbon, cuivre, fer, or, or du Nether,
 *   redstone, lapis, diamant, émeraude, quartz, débris antiques), comparé à la roche minée (pierre, deepslate, tuf,
 *   netherrack...).
 * - « Caché » : aucun voisin d'air, d'eau ou de lave d'origine (les blocs que le joueur a lui-même cassés dans les
 *   15 dernières minutes ne comptent pas : tunnel et filon minés bloc par bloc comptent chaque minerai ; un minerai
 *   vu dans une grotte reste « visible »). Avant (1.0.x) : un filon par 30 s et au plus 1 face d'air : compte faux.
 * - Minerais posés par un joueur : ignorés.
 * - Alertes : sur les minerais rares cachés (diamant, émeraude, débris antiques), par heure : alerte légère, heure
 *   extrême, infraction grave si plusieurs heures extrêmes (seuils xray.*). Onglet « Minage » : 24 dernières heures.
 */
final class Minage implements Listener {

    /** Catégories de minerais, dans l'ordre d'affichage. */
    enum Categorie {
        DIAMANT("diamant", true), EMERAUDE("émeraude", true), DEBRIS("débris antiques", true), OR("or", false),
        OR_NETHER("or du Nether", false), FER("fer", false), CUIVRE("cuivre", false), REDSTONE("redstone", false),
        LAPIS("lapis", false), CHARBON("charbon", false), QUARTZ("quartz", false);

        final String nom;
        final boolean rare;

        Categorie(String nom, boolean rare) {
            this.nom = nom;
            this.rare = rare;
        }
    }

    private static final Map<Material, Categorie> MINERAIS = new EnumMap<>(Material.class);

    static {
        MINERAIS.put(Material.DIAMOND_ORE, Categorie.DIAMANT);
        MINERAIS.put(Material.DEEPSLATE_DIAMOND_ORE, Categorie.DIAMANT);
        MINERAIS.put(Material.EMERALD_ORE, Categorie.EMERAUDE);
        MINERAIS.put(Material.DEEPSLATE_EMERALD_ORE, Categorie.EMERAUDE);
        MINERAIS.put(Material.ANCIENT_DEBRIS, Categorie.DEBRIS);
        MINERAIS.put(Material.GOLD_ORE, Categorie.OR);
        MINERAIS.put(Material.DEEPSLATE_GOLD_ORE, Categorie.OR);
        MINERAIS.put(Material.NETHER_GOLD_ORE, Categorie.OR_NETHER);
        MINERAIS.put(Material.IRON_ORE, Categorie.FER);
        MINERAIS.put(Material.DEEPSLATE_IRON_ORE, Categorie.FER);
        MINERAIS.put(Material.COPPER_ORE, Categorie.CUIVRE);
        MINERAIS.put(Material.DEEPSLATE_COPPER_ORE, Categorie.CUIVRE);
        MINERAIS.put(Material.REDSTONE_ORE, Categorie.REDSTONE);
        MINERAIS.put(Material.DEEPSLATE_REDSTONE_ORE, Categorie.REDSTONE);
        MINERAIS.put(Material.LAPIS_ORE, Categorie.LAPIS);
        MINERAIS.put(Material.DEEPSLATE_LAPIS_ORE, Categorie.LAPIS);
        MINERAIS.put(Material.COAL_ORE, Categorie.CHARBON);
        MINERAIS.put(Material.DEEPSLATE_COAL_ORE, Categorie.CHARBON);
        MINERAIS.put(Material.NETHER_QUARTZ_ORE, Categorie.QUARTZ);
    }

    private static final Set<Material> ROCHE = EnumSet.of(Material.STONE, Material.DEEPSLATE, Material.TUFF,
            Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.NETHERRACK, Material.BASALT,
            Material.BLACKSTONE, Material.CALCITE, Material.SMOOTH_BASALT);
    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST};
    private static final long QUINZE_MINUTES = 15 * 60_000L;

    /** Une heure d'un joueur : roche, minerais (total et cachés) par catégorie. */
    static final class Heure {
        int roche;
        final EnumMap<Categorie, int[]> minerais = new EnumMap<>(Categorie.class);
        boolean alerte;
        boolean extreme;

        int rares() {
            int n = 0;
            for (Map.Entry<Categorie, int[]> e : minerais.entrySet()) {
                if (e.getKey().rare) {
                    n += e.getValue()[1];
                }
            }
            return n;
        }
    }

    private record Casse(String cle, long date) {
    }

    private final KSAntiCheat plugin;
    private final File fichier;
    private final Map<UUID, Map<Long, Heure>> heures = new HashMap<>();
    private final Map<UUID, String> noms = new HashMap<>();
    /** Blocs cassés récemment par chaque joueur (15 min) : ouvertures faites par lui, pas d'origine. */
    private final Map<UUID, Deque<Casse>> casses = new HashMap<>();
    private final Map<UUID, Set<String>> cassesIndex = new HashMap<>();
    /** Minerais posés par un joueur (ignorés au cassage). */
    private final Set<String> poses = new LinkedHashSet<>();
    private boolean modifie;

    Minage(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "minage.yml");
        charger();
    }

    private static long heureActuelle() {
        return System.currentTimeMillis() / 3_600_000L;
    }

    private static String cle(Block b) {
        return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private Heure heure(UUID joueur) {
        return heures.computeIfAbsent(joueur, u -> new LinkedHashMap<>()).computeIfAbsent(heureActuelle(), h -> new Heure());
    }

    /** Minerai posé par un joueur : ne comptera pas. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (MINERAIS.containsKey(event.getBlockPlaced().getType())) {
            poses.add(cle(event.getBlockPlaced()));
            while (poses.size() > 20_000) {
                poses.remove(poses.iterator().next());
            }
            modifie = true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block bloc = event.getBlock();
        String ici = cle(bloc);
        if (p.getGameMode() == GameMode.CREATIVE) {
            poses.remove(ici);
            return;
        }
        UUID u = p.getUniqueId();
        Material type = bloc.getType();
        Categorie cat = MINERAIS.get(type);
        boolean roche = ROCHE.contains(type);
        if (cat != null && poses.remove(ici)) {
            noterCasse(u, ici);
            return;
        }
        if (roche) {
            heure(u).roche++;
            modifie = true;
        } else if (cat != null) {
            noms.put(u, p.getName());
            Heure h = heure(u);
            int[] n = h.minerais.computeIfAbsent(cat, c -> new int[2]);
            n[0]++;
            if (cache(u, bloc)) {
                n[1]++;
            }
            modifie = true;
            if (cat.rare) {
                verifier(u, p.getName(), h);
            }
        }
        noterCasse(u, ici);
    }

    private void noterCasse(UUID u, String cle) {
        long maintenant = System.currentTimeMillis();
        Deque<Casse> d = casses.computeIfAbsent(u, x -> new ArrayDeque<>());
        Set<String> index = cassesIndex.computeIfAbsent(u, x -> new java.util.HashSet<>());
        d.addLast(new Casse(cle, maintenant));
        index.add(cle);
        while (!d.isEmpty() && maintenant - d.peekFirst().date() > QUINZE_MINUTES) {
            index.remove(d.removeFirst().cle());
        }
    }

    /** Aucun voisin d'air, d'eau ou de lave d'origine (les ouvertures faites par le joueur ne comptent pas). */
    private boolean cache(UUID joueur, Block bloc) {
        Set<String> parLui = cassesIndex.getOrDefault(joueur, Set.of());
        for (BlockFace f : FACES) {
            Block voisin = bloc.getRelative(f);
            Material t = voisin.getType();
            if ((t.isAir() || t == Material.WATER || t == Material.LAVA) && !parLui.contains(cle(voisin))) {
                return false;
            }
        }
        return true;
    }

    private static double taux(int n, int roche) {
        return n * 1000.0 / Math.max(roche, 1);
    }

    private void verifier(UUID u, String nom, Heure h) {
        var c = plugin.getConfig();
        int rares = h.rares();
        double t = taux(rares, h.roche);
        if (!h.alerte && rares >= c.getInt("xray.alerte-minerais", 15) && t >= c.getDouble("xray.alerte-taux-minerais", 12)) {
            h.alerte = true;
            plugin.alertes().ajouter(u, nom, "X-ray ?", Gravite.LEGERE, rares + " minerais rares cachés cette heure pour "
                    + h.roche + " blocs de roche (" + String.format("%.1f", t) + " pour 1 000) - " + detail(h));
        }
        if (!h.extreme && rares >= c.getInt("xray.extreme-minerais", 30) && t >= c.getDouble("xray.extreme-taux-minerais", 30)) {
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
                        + " dernières (cette heure : " + rares + " minerais rares cachés, " + String.format("%.1f", t)
                        + " pour 1 000 blocs de roche)");
            } else {
                plugin.alertes().ajouter(u, nom, "X-ray (heure extrême)", Gravite.LEGERE, extremes + " / " + requis
                        + " heures extrêmes sur " + fenetre + " h (" + rares + " minerais rares cachés, "
                        + String.format("%.1f", t) + " pour 1 000) - " + detail(h));
            }
        }
    }

    /** « diamant 12 (9 cachés, 1,4 %) ; or 6 (6, 0,7 %)... » (pourcentage : minerais / roche). */
    private static String detail(Heure h) {
        return detail(h.minerais, h.roche);
    }

    private static String detail(EnumMap<Categorie, int[]> minerais, int roche) {
        List<String> parties = new ArrayList<>();
        for (Categorie cat : Categorie.values()) {
            int[] n = minerais.get(cat);
            if (n != null && n[0] > 0) {
                parties.add(cat.nom + " " + n[0] + " (" + n[1] + " cachés, "
                        + String.format("%.1f", n[0] * 100.0 / Math.max(roche, 1)) + " %)");
            }
        }
        return parties.isEmpty() ? "aucun minerai" : String.join(" ; ", parties);
    }

    /** Résumé des dernières 24 h, du plus suspect au moins suspect (onglet Minage). */
    List<String> resume24h() {
        long maintenant = heureActuelle();
        List<Object[]> lignes = new ArrayList<>();
        heures.forEach((u, parHeure) -> {
            int roche = 0;
            int extremes = 0;
            EnumMap<Categorie, int[]> total = new EnumMap<>(Categorie.class);
            for (Map.Entry<Long, Heure> e : parHeure.entrySet()) {
                if (maintenant - e.getKey() >= 24) {
                    continue;
                }
                roche += e.getValue().roche;
                extremes += e.getValue().extreme ? 1 : 0;
                e.getValue().minerais.forEach((c, n) -> {
                    int[] t = total.computeIfAbsent(c, x -> new int[2]);
                    t[0] += n[0];
                    t[1] += n[1];
                });
            }
            if (roche == 0 && total.isEmpty()) {
                return;
            }
            int rares = 0;
            for (Map.Entry<Categorie, int[]> e : total.entrySet()) {
                if (e.getKey().rare) {
                    rares += e.getValue()[1];
                }
            }
            double t = taux(rares, roche);
            lignes.add(new Object[]{t, noms.getOrDefault(u, "?") + " : " + roche + " roche, rares cachés "
                    + String.format("%.1f", t) + " pour 1 000" + (extremes > 0 ? ", " + extremes + " h extrême(s)" : "")
                    + " - " + detail(total, roche)});
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
        if (yaml.getInt("format", 1) < 2) {
            // Ancien format (filons, 1.0.x) : remis à zéro.
            return;
        }
        poses.addAll(yaml.getStringList("poses"));
        ConfigurationSection js = yaml.getConfigurationSection("joueurs");
        if (js == null) {
            return;
        }
        for (String cleJ : js.getKeys(false)) {
            try {
                UUID u = UUID.fromString(cleJ);
                noms.put(u, js.getString(cleJ + ".nom", "?"));
                ConfigurationSection hs = js.getConfigurationSection(cleJ + ".heures");
                if (hs == null) {
                    continue;
                }
                Map<Long, Heure> m = heures.computeIfAbsent(u, x -> new LinkedHashMap<>());
                for (String hc : hs.getKeys(false)) {
                    Heure h = new Heure();
                    h.roche = hs.getInt(hc + ".roche");
                    h.alerte = hs.getBoolean(hc + ".alerte");
                    h.extreme = hs.getBoolean(hc + ".extreme");
                    ConfigurationSection ms = hs.getConfigurationSection(hc + ".minerais");
                    if (ms != null) {
                        for (String c : ms.getKeys(false)) {
                            try {
                                h.minerais.put(Categorie.valueOf(c), new int[]{ms.getInt(c + ".total"), ms.getInt(c + ".caches")});
                            } catch (IllegalArgumentException e) {
                                // catégorie inconnue : ignorée
                            }
                        }
                    }
                    m.put(Long.parseLong(hc), h);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Minage ignoré : " + cleJ);
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
        yaml.set("format", 2);
        yaml.set("poses", new ArrayList<>(poses));
        heures.forEach((u, m) -> {
            m.keySet().removeIf(h -> maintenant - h >= 24);
            m.forEach((h, v) -> {
                String b = "joueurs." + u + ".heures." + h;
                yaml.set(b + ".roche", v.roche);
                yaml.set(b + ".alerte", v.alerte);
                yaml.set(b + ".extreme", v.extreme);
                v.minerais.forEach((c, n) -> {
                    yaml.set(b + ".minerais." + c.name() + ".total", n[0]);
                    yaml.set(b + ".minerais." + c.name() + ".caches", n[1]);
                });
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
