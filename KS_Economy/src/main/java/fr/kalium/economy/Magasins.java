package fr.kalium.economy;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.1.0 - magasins et boutiques (cahier des charges, catégorie 2, partie « Magasins » révisée par LeKiwi06 le
 * 02/10/2026) : données, enregistrement (plugins/KS_Economy/magasins.yml), zone des magasins, claims.
 *
 * - Magasin : 1 par joueur, créé dans un de ses claims de la région WorldGuard « zone_shop » ; des claims (les chunks
 *   où il peut poser des boutiques), 10 boutiques + 1 par agrandissement, une fiche (nom, description, position).
 * - Boutique : un panneau sur un contenant (coffre, coffre piégé, tonneau, shulker, coffre en cuivre), dans un claim du
 *   magasin ; un objet vendu (quantité) contre un objet (quantité) ou des points ; les points payés attendent dans la
 *   boutique. 1.1.2 : nom, fermeture temporaire ; une boutique supprimée garde sa place 3 h (délai réglable).
 */
final class Magasins {

    /** Contenants acceptés pour une boutique (1.1.2 : coffres en cuivre, tous états et cirés, compris). */
    static boolean contenantAccepte(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL
                || type.name().endsWith("SHULKER_BOX") || type.name().endsWith("COPPER_CHEST");
    }

    /** 1.1.2 : longueur maximale du nom d'une boutique. */
    static final int NOM_MAX = 20;

    static final class Magasin {
        UUID proprio;
        String nom;
        String description = "";
        Location position;
        /** Chunks du magasin : « monde:x:z ». */
        final List<String> claims = new ArrayList<>();
        int agrandissements;
    }

    static final class Boutique {
        String id;
        UUID proprio;
        Location panneau;
        ItemStack objet;
        int quantite;
        /** Prix en objet (null : prix en points). */
        ItemStack prixObjet;
        int prixQuantite;
        long prixPoints;
        long pointsEnAttente;
        /** 1.1.2 : nom choisi par le propriétaire (null : nom de l'objet vendu). */
        String nom;
        /** 1.1.2 : fermée temporairement par son propriétaire (aucun achat). */
        boolean fermee;

        boolean enPoints() {
            return prixObjet == null;
        }
    }

    private final KSEconomy plugin;
    private final File fichier;
    final Map<UUID, Magasin> magasins = new LinkedHashMap<>();
    final Map<String, Boutique> boutiques = new LinkedHashMap<>();
    /** Contenants verrouillés (gestion à distance en cours) : clé du bloc. */
    final Set<String> verrous = new HashSet<>();
    /** 1.1.2 : propriétaire -> dates des boutiques supprimées (leur place reste prise pendant le délai). */
    final Map<UUID, List<Long>> suppressions = new LinkedHashMap<>();

    Magasins(KSEconomy plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "magasins.yml");
        charger();
    }

    // ------------------------------------------------------------------ réglages

    String region() {
        return plugin.getConfig().getString("magasins.region", "zone_shop");
    }

    long prixCreation() {
        return Math.max(0, plugin.getConfig().getLong("magasins.prix-creation", 45_000));
    }

    int blocsCreation() {
        return Math.max(0, plugin.getConfig().getInt("magasins.blocs-tier3-creation", 5));
    }

    long prixAgrandissement() {
        return Math.max(0, plugin.getConfig().getLong("magasins.prix-agrandissement", 10_000));
    }

    int boutiquesDeBase() {
        return Math.max(1, plugin.getConfig().getInt("magasins.boutiques", 10));
    }

    int boutiquesMax(Magasin magasin) {
        return boutiquesDeBase() + magasin.agrandissements;
    }

    /** 1.1.2 : pendant ce délai après une suppression, la place de la boutique reste prise (anti « switch »). */
    long delaiSuppressionMs() {
        return (long) (Math.max(0, plugin.getConfig().getDouble("magasins.delai-suppression-heures", 3)) * 3_600_000L);
    }

    /** 1.1.2 : places encore prises par des boutiques supprimées il y a moins que le délai. */
    int placesEnAttente(UUID proprio) {
        List<Long> dates = suppressions.get(proprio);
        if (dates == null) {
            return 0;
        }
        long limite = System.currentTimeMillis() - delaiSuppressionMs();
        dates.removeIf(d -> d <= limite);
        if (dates.isEmpty()) {
            suppressions.remove(proprio);
            return 0;
        }
        return dates.size();
    }

    /** 1.1.2 : millisecondes avant que la prochaine place se libère (0 : aucune en attente). */
    long avantLiberation(UUID proprio) {
        if (placesEnAttente(proprio) == 0) {
            return 0;
        }
        long premiere = suppressions.get(proprio).stream().mapToLong(Long::longValue).min().orElse(0);
        return Math.max(0, premiere + delaiSuppressionMs() - System.currentTimeMillis());
    }

    void noterSuppression(UUID proprio) {
        if (delaiSuppressionMs() > 0) {
            suppressions.computeIfAbsent(proprio, u -> new ArrayList<>()).add(System.currentTimeMillis());
        }
    }

    // ------------------------------------------------------------------ clés

    static String cleChunk(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    static String cleBloc(Block bloc) {
        return bloc.getWorld().getName() + ":" + bloc.getX() + ":" + bloc.getY() + ":" + bloc.getZ();
    }

    // ------------------------------------------------------------------ zone et claims

    /** Le centre de ce chunk est-il dans la région des magasins ? */
    boolean dansLaZone(Chunk chunk) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return false;
        }
        return ZoneWorldGuard.contient(chunk.getWorld(), region(), chunk.getX() * 16 + 8, chunk.getZ() * 16 + 8);
    }

    /** Propriétaire du claim de ce chunk (SimpleClaimSystem), ou null. */
    static UUID proprioDuClaim(Chunk chunk) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SimpleClaimSystem")) {
            return null;
        }
        return ClaimsSCS.proprio(chunk);
    }

    Magasin magasinDuChunk(Chunk chunk) {
        String cle = cleChunk(chunk);
        for (Magasin m : magasins.values()) {
            if (m.claims.contains(cle)) {
                return m;
            }
        }
        return null;
    }

    int nombreDeBoutiques(UUID proprio) {
        int n = 0;
        for (Boutique b : boutiques.values()) {
            if (b.proprio.equals(proprio)) {
                n++;
            }
        }
        return n;
    }

    List<Boutique> boutiquesDe(UUID proprio) {
        List<Boutique> liste = new ArrayList<>();
        for (Boutique b : boutiques.values()) {
            if (b.proprio.equals(proprio)) {
                liste.add(b);
            }
        }
        return liste;
    }

    /** Pourquoi KS_Claim doit refuser de supprimer ou vendre ce claim (texte), ou null. */
    String refusClaim(UUID proprio, Chunk chunk) {
        Magasin m = magasins.get(proprio);
        if (m == null || !m.claims.contains(cleChunk(chunk))) {
            return null;
        }
        for (Boutique b : boutiquesDe(proprio)) {
            if (b.panneau.getWorld() != null && b.panneau.getChunk().equals(chunk)) {
                return "Ce claim porte des boutiques de ton magasin : retire-les d'abord.";
            }
        }
        return null;
    }

    /** Claim supprimé ou vendu : il quitte le magasin. */
    void claimRetire(UUID proprio, Chunk chunk) {
        Magasin m = magasins.get(proprio);
        if (m != null && m.claims.remove(cleChunk(chunk))) {
            sauver();
        }
    }

    // ------------------------------------------------------------------ panneaux et contenants

    /** Contenant auquel ce panneau est attaché (contre lui, ou dessous pour un panneau posé), ou null. */
    static Block contenantDu(Block panneau) {
        Block support = panneau.getBlockData() instanceof WallSign mural
                ? panneau.getRelative(mural.getFacing().getOppositeFace())
                : panneau.getRelative(0, -1, 0);
        return contenantAccepte(support.getType()) ? support : null;
    }

    /** Inventaire du contenant (coffre double compris), ou null. */
    static Inventory inventaire(Block contenant) {
        if (contenant == null) {
            return null;
        }
        BlockState etat = contenant.getState();
        return etat instanceof Container c ? c.getInventory() : null;
    }

    Boutique boutiqueDuPanneau(Block panneau) {
        String cle = cleBloc(panneau);
        for (Boutique b : boutiques.values()) {
            if (cleBloc(b.panneau.getBlock()).equals(cle)) {
                return b;
            }
        }
        return null;
    }

    /** Boutiques dont le contenant est ce bloc (ou l'autre moitié de son coffre double). */
    List<Boutique> boutiquesDuContenant(Block contenant) {
        List<Boutique> liste = new ArrayList<>();
        Inventory inv = inventaire(contenant);
        Location ici = inv == null ? null : inv.getLocation();
        for (Boutique b : boutiques.values()) {
            // 1.1.2 : seulement les panneaux tout près (contre le contenant ou l'autre moitié d'un coffre double),
            // sans charger le chunk des autres boutiques.
            if (b.panneau.getWorld() == null || !b.panneau.getWorld().equals(contenant.getWorld())
                    || Math.abs(b.panneau.getBlockX() - contenant.getX()) > 2
                    || Math.abs(b.panneau.getBlockY() - contenant.getY()) > 2
                    || Math.abs(b.panneau.getBlockZ() - contenant.getZ()) > 2) {
                continue;
            }
            Inventory autre = inventaire(contenantDu(b.panneau.getBlock()));
            if (autre != null && ici != null && ici.equals(autre.getLocation())) {
                liste.add(b);
            }
        }
        return liste;
    }

    /** Clé du verrou d'une boutique : son contenant (les deux moitiés d'un coffre double ont la même). */
    static String cleVerrou(Boutique b) {
        Inventory inv = inventaire(contenantDu(b.panneau.getBlock()));
        Location l = inv == null ? b.panneau : inv.getLocation();
        return l == null ? cleBloc(b.panneau.getBlock()) : cleBloc(l.getBlock());
    }

    boolean verrouillee(Boutique b) {
        return verrous.contains(cleVerrou(b));
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection ms = yaml.getConfigurationSection("magasins");
        if (ms != null) {
            for (String cle : ms.getKeys(false)) {
                try {
                    Magasin m = new Magasin();
                    m.proprio = UUID.fromString(cle);
                    m.nom = ms.getString(cle + ".nom", "Magasin");
                    m.description = ms.getString(cle + ".description", "");
                    m.position = ms.getLocation(cle + ".position");
                    m.claims.addAll(ms.getStringList(cle + ".claims"));
                    m.agrandissements = ms.getInt(cle + ".agrandissements");
                    magasins.put(m.proprio, m);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Magasin ignoré : " + cle);
                }
            }
        }
        ConfigurationSection ss = yaml.getConfigurationSection("suppressions");
        if (ss != null) {
            for (String cle : ss.getKeys(false)) {
                try {
                    List<Long> dates = new ArrayList<>(ss.getLongList(cle));
                    if (!dates.isEmpty()) {
                        suppressions.put(UUID.fromString(cle), dates);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Suppressions ignorées : " + cle);
                }
            }
        }
        ConfigurationSection bs = yaml.getConfigurationSection("boutiques");
        if (bs != null) {
            for (String id : bs.getKeys(false)) {
                try {
                    Boutique b = new Boutique();
                    b.id = id;
                    b.proprio = UUID.fromString(bs.getString(id + ".proprio", ""));
                    b.panneau = bs.getLocation(id + ".panneau");
                    b.objet = bs.getItemStack(id + ".objet");
                    b.quantite = bs.getInt(id + ".quantite", 1);
                    b.prixObjet = bs.getItemStack(id + ".prix-objet");
                    b.prixQuantite = bs.getInt(id + ".prix-quantite", 1);
                    b.prixPoints = bs.getLong(id + ".prix-points");
                    b.pointsEnAttente = bs.getLong(id + ".points-en-attente");
                    b.nom = bs.getString(id + ".nom", null);
                    b.fermee = bs.getBoolean(id + ".fermee", false);
                    if (b.panneau == null || b.objet == null) {
                        throw new IllegalArgumentException("incomplète");
                    }
                    boutiques.put(id, b);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Boutique ignorée : " + id + " (" + e.getMessage() + ")");
                }
            }
        }
    }

    void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        magasins.forEach((uuid, m) -> {
            String cle = "magasins." + uuid;
            yaml.set(cle + ".nom", m.nom);
            yaml.set(cle + ".description", m.description);
            yaml.set(cle + ".position", m.position);
            yaml.set(cle + ".claims", m.claims);
            yaml.set(cle + ".agrandissements", m.agrandissements);
        });
        boutiques.forEach((id, b) -> {
            String cle = "boutiques." + id;
            yaml.set(cle + ".proprio", b.proprio.toString());
            yaml.set(cle + ".panneau", b.panneau);
            yaml.set(cle + ".objet", b.objet);
            yaml.set(cle + ".quantite", b.quantite);
            yaml.set(cle + ".prix-objet", b.prixObjet);
            yaml.set(cle + ".prix-quantite", b.prixQuantite);
            yaml.set(cle + ".prix-points", b.prixPoints);
            yaml.set(cle + ".points-en-attente", b.pointsEnAttente);
            yaml.set(cle + ".nom", b.nom);
            yaml.set(cle + ".fermee", b.fermee);
        });
        suppressions.forEach((uuid, dates) -> yaml.set("suppressions." + uuid, dates));
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer magasins.yml : " + e.getMessage());
        }
    }

    String nouvelId() {
        int n = boutiques.size() + 1;
        while (boutiques.containsKey("b" + n)) {
            n++;
        }
        return "b" + n;
    }

    static World monde(String cleChunk) {
        return Bukkit.getWorld(cleChunk.substring(0, cleChunk.indexOf(':')));
    }
}
