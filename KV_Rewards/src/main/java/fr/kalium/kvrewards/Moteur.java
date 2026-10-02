package fr.kalium.kvrewards;

import fr.kalium.kvplots.api.KanvasPlots;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Moteur des récompenses de Kanvas (cahier des charges, catégorie 4 ; mêmes règles que KG_Rewards, sur les NOTES
 * reçues par ses plots, créateur et éditeurs compris (choix de LeKiwi06, 03/10/2026)).
 *
 * - Une seule grille : les notes reçues. Périodes : mois aligné (1er vendredi 21 h -> 1er vendredi 21 h, comme
 *   KG_ScoreBoards) et permanent. Paliers, prestige (permanent, +20 % sur les paliers permanents), tops (classiques et
 *   %) : comme KG_Rewards. Tops du mois : classement final au changement de mois (vérifié chaque minute).
 * - Concours de build : récompense des 3 premières places (total des notes, puis moyenne ; tous les bâtisseurs du plot).
 * - Aucun palier rétroactif : au premier démarrage, les notes déjà reçues servent de point de départ.
 */
final class Moteur {

    static final int[] TOPS = {100, 50, 25, 10, 5, 3, 2, 1};
    static final int[] TOPS_POURCENT = {50, 25, 10, 5, 1};
    static final int PLACES_CONCOURS = 3;
    static final double SEUIL_PRESTIGE = 100_000;
    private static final long SEMAINE_MS = 7L * 24 * 60 * 60 * 1000;
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    static final class Grille {
        int prestige;
        double decalage;
        final Map<String, Double> paliers = new LinkedHashMap<>();
        final Set<String> topsAtteints = new LinkedHashSet<>();
        long entree;
        long echeance;
        int pireClassique;
        int pirePourcent;
    }

    private final KVRewards plugin;
    private final Butin butin;
    private final Envois envois;
    private final File fichier;
    /** Joueur -> grille des notes. */
    final Map<UUID, Grille> grilles = new LinkedHashMap<>();
    private boolean initialise;
    private String moisEnCours;

    Moteur(KVRewards plugin, Butin butin, Envois envois) {
        this.plugin = plugin;
        this.butin = butin;
        this.envois = envois;
        this.fichier = new File(plugin.getDataFolder(), "joueurs.yml");
        charger();
    }

    static KanvasPlots plots() {
        return Bukkit.getServicesManager().load(KanvasPlots.class);
    }

    Grille grille(UUID uuid) {
        return grilles.computeIfAbsent(uuid, u -> new Grille());
    }

    int prestige(UUID uuid) {
        Grille g = grilles.get(uuid);
        return g == null ? 0 : g.prestige;
    }

    static String nom(UUID uuid) {
        String nom = Bukkit.getOfflinePlayer(uuid).getName();
        return nom == null ? "?" : nom;
    }

    static String nomPeriode(String type) {
        return switch (type) {
            case "mois" -> "mois";
            case "concours" -> "concours de build";
            default -> "permanent";
        };
    }

    // ------------------------------------------------------------------ mois aligné

    static ZonedDateTime debutMois(YearMonth mois) {
        return mois.atDay(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.FRIDAY)).atTime(21, 0).atZone(PARIS);
    }

    static YearMonth moisActuel() {
        ZonedDateTime maintenant = ZonedDateTime.now(PARIS);
        YearMonth mois = YearMonth.from(maintenant);
        return maintenant.isBefore(debutMois(mois)) ? mois.minusMonths(1) : mois;
    }

    static long debutMoisMs(YearMonth mois) {
        return debutMois(mois).toInstant().toEpochMilli();
    }

    // ------------------------------------------------------------------ compteurs

    double compteur(String type, UUID uuid) {
        KanvasPlots p = plots();
        if ("mois".equals(type)) {
            return p.notesRecues(uuid, debutMoisMs(moisActuel()));
        }
        return Math.max(0, p.notesRecues(uuid, 0) - grille(uuid).decalage);
    }

    private String clePeriode(String type, Grille g) {
        return "mois".equals(type) ? "mois:" + moisActuel() : "permanent:" + g.prestige;
    }

    // ------------------------------------------------------------------ paliers (mêmes seuils que KG_Rewards)

    static String categorie(long t) {
        if (t <= 0) {
            return null;
        }
        if (t % 100_000 == 0) {
            return "exceptionnelle";
        }
        if (t % 10_000 == 0) {
            return "grosse";
        }
        if (t <= 50_000 && t % 1_000 == 0) {
            return "moyenne";
        }
        if (t <= 10_000 && t % 500 == 0) {
            return "petite";
        }
        if (t <= 1_000 && t % 100 == 0) {
            return "minimale";
        }
        return null;
    }

    static List<Long> seuils(double depuis, double jusqua) {
        List<Long> liste = new ArrayList<>();
        for (long t = ((long) Math.floor(depuis) / 100 + 1) * 100; t <= jusqua; t += 100) {
            if (categorie(t) != null) {
                liste.add(t);
            }
        }
        return liste;
    }

    static long prochainSeuil(double compteur) {
        long t = ((long) Math.floor(compteur) / 100 + 1) * 100;
        while (categorie(t) == null) {
            t += 100;
        }
        return t;
    }

    /** Note reçue : paliers de chaque bâtisseur du plot (mois et permanent). */
    void noteRecue(List<UUID> batisseurs) {
        for (UUID uuid : batisseurs) {
            for (String type : List.of("mois", "permanent")) {
                Grille g = grille(uuid);
                String cle = clePeriode(type, g);
                double compteur = compteur(type, uuid);
                double dernier = g.paliers.getOrDefault(cle, 0.0);
                List<Long> franchis = compteur > dernier ? seuils(dernier, compteur) : List.of();
                if (franchis.isEmpty()) {
                    continue;
                }
                double multiplicateur = "permanent".equals(type) ? 1 + 0.2 * g.prestige : 1;
                for (long t : franchis) {
                    envois.envoyer(uuid, nom(uuid), "Palier " + t + " points de notes (" + nomPeriode(type)
                                    + (g.prestige > 0 && "permanent".equals(type) ? ", prestige " + g.prestige : "") + ")",
                            butin.tirer(type, categorie(t), multiplicateur));
                }
                g.paliers.put(cle, (double) franchis.get(franchis.size() - 1));
            }
        }
        sauver();
    }

    boolean prestigePossible(UUID uuid) {
        return compteur("permanent", uuid) >= SEUIL_PRESTIGE;
    }

    void prendrePrestige(UUID uuid) {
        Grille g = grille(uuid);
        g.prestige++;
        g.decalage = plots().notesRecues(uuid, 0);
        sauver();
        plugin.getLogger().info("Prestige : " + nom(uuid) + " passe au prestige " + g.prestige + ".");
    }

    // ------------------------------------------------------------------ tops

    record Ligne(UUID uuid, double score) {
    }

    private static List<Ligne> classement(Map<UUID, Long> notes) {
        List<Ligne> liste = new ArrayList<>();
        notes.forEach((uuid, n) -> {
            if (n >= 1) {
                liste.add(new Ligne(uuid, n));
            }
        });
        liste.sort((a, b) -> Double.compare(b.score(), a.score()));
        return liste;
    }

    static int topClassique(int rang) {
        int meilleur = 0;
        for (int x : TOPS) {
            if (rang <= x) {
                meilleur = x;
            }
        }
        return meilleur;
    }

    static int topPourcent(int rang, int n) {
        int meilleur = 0;
        for (int x : TOPS_POURCENT) {
            if (rang <= (int) Math.ceil(x * n / 100.0)) {
                meilleur = x;
            }
        }
        return meilleur;
    }

    /** Chaque minute : changement de mois -> tops du mois qui se termine (classement final). */
    void verifierMois() {
        YearMonth actuel = moisActuel();
        if (moisEnCours == null) {
            moisEnCours = actuel.toString();
            sauver();
            return;
        }
        if (moisEnCours.equals(actuel.toString())) {
            return;
        }
        YearMonth fini = YearMonth.parse(moisEnCours);
        Map<UUID, Long> depuisDebut = plots().notesParBatisseur(debutMoisMs(fini));
        Map<UUID, Long> depuisFin = plots().notesParBatisseur(debutMoisMs(actuel));
        Map<UUID, Long> duMois = new LinkedHashMap<>();
        depuisDebut.forEach((uuid, n) -> duMois.put(uuid, n - depuisFin.getOrDefault(uuid, 0L)));
        List<Ligne> lignes = classement(duMois);
        int n = lignes.size();
        for (int i = 0; i < n; i++) {
            UUID uuid = lignes.get(i).uuid();
            int classique = topClassique(i + 1);
            if (classique > 0) {
                envois.envoyer(uuid, nom(uuid), "Top " + classique + " du mois - notes de Kanvas",
                        butin.tirer("mois", "top-" + classique, 1));
            }
            int pourcent = topPourcent(i + 1, n);
            for (int x : TOPS_POURCENT) {
                if (pourcent > 0 && x >= pourcent) {
                    envois.envoyer(uuid, nom(uuid), "Top " + x + " % du mois - notes de Kanvas",
                            butin.tirer("mois", "top-" + x + "p", 1));
                }
            }
        }
        plugin.getLogger().info("Fin du mois " + fini + " : tops récompensés (" + n + " joueur(s) classé(s)).");
        moisEnCours = actuel.toString();
        sauver();
    }

    /** Toutes les 5 minutes : tops permanents (arrivée, semaines passées dans le top). */
    void verifierTopsPermanents() {
        List<Ligne> lignes = classement(plots().notesParBatisseur(0));
        long maintenant = System.currentTimeMillis();
        int n = lignes.size();
        Set<UUID> presents = new HashSet<>();
        for (int i = 0; i < n; i++) {
            UUID uuid = lignes.get(i).uuid();
            int classique = topClassique(i + 1);
            int pourcent = topPourcent(i + 1, n);
            if (classique == 0 && pourcent == 0) {
                continue;
            }
            presents.add(uuid);
            Grille g = grille(uuid);
            if (classique > 0 && g.topsAtteints.add("top-" + classique)) {
                envois.envoyer(uuid, nom(uuid), "Arrivée dans le top " + classique + " - notes de Kanvas",
                        butin.tirer("permanent", "top-" + classique, 1));
            }
            for (int x : TOPS_POURCENT) {
                if (pourcent > 0 && x >= pourcent && g.topsAtteints.add("top-" + x + "p")) {
                    envois.envoyer(uuid, nom(uuid), "Arrivée dans le top " + x + " % - notes de Kanvas",
                            butin.tirer("permanent", "top-" + x + "p", 1));
                }
            }
            if (g.entree == 0) {
                g.entree = maintenant;
                g.echeance = maintenant + SEMAINE_MS;
                g.pireClassique = classique;
                g.pirePourcent = pourcent;
                continue;
            }
            g.pireClassique = g.pireClassique == 0 || classique == 0 ? 0 : Math.max(g.pireClassique, classique);
            g.pirePourcent = g.pirePourcent == 0 || pourcent == 0 ? 0 : Math.max(g.pirePourcent, pourcent);
            while (maintenant >= g.echeance) {
                if (g.pireClassique > 0) {
                    envois.envoyer(uuid, nom(uuid), "Semaine dans le top " + g.pireClassique + " - notes de Kanvas",
                            butin.tirer("permanent", "top-" + g.pireClassique, 1));
                }
                for (int x : TOPS_POURCENT) {
                    if (g.pirePourcent > 0 && x >= g.pirePourcent) {
                        envois.envoyer(uuid, nom(uuid), "Semaine dans le top " + x + " % - notes de Kanvas",
                                butin.tirer("permanent", "top-" + x + "p", 1));
                    }
                }
                g.pireClassique = classique;
                g.pirePourcent = pourcent;
                g.echeance += SEMAINE_MS;
            }
        }
        grilles.forEach((uuid, g) -> {
            if (g.entree != 0 && !presents.contains(uuid)) {
                g.entree = 0;
                g.echeance = 0;
            }
        });
        sauver();
    }

    // ------------------------------------------------------------------ concours

    /** Votes d'un concours clos : les 3 premières places (au moins 1 point) ; tous les bâtisseurs du plot. */
    void concoursTermine(String theme, List<KanvasPlots.PlotInfo> classement) {
        for (int i = 0; i < Math.min(PLACES_CONCOURS, classement.size()); i++) {
            KanvasPlots.PlotInfo plot = classement.get(i);
            if (plot.points() < 1) {
                break;
            }
            Set<UUID> batisseurs = new LinkedHashSet<>();
            batisseurs.add(plot.createur());
            batisseurs.addAll(plot.editeurs());
            String place = i == 0 ? "1re place" : (i + 1) + "e place";
            for (UUID uuid : batisseurs) {
                envois.envoyer(uuid, nom(uuid), place + " du concours « " + theme + " »",
                        butin.tirer("concours", "concours-" + (i + 1), 1));
            }
        }
    }

    // ------------------------------------------------------------------ démarrage

    void initialiserSiBesoin() {
        if (initialise) {
            return;
        }
        Map<UUID, Long> toutes = plots().notesParBatisseur(0);
        for (UUID uuid : toutes.keySet()) {
            Grille g = grille(uuid);
            for (String type : List.of("mois", "permanent")) {
                g.paliers.put(clePeriode(type, g), compteur(type, uuid));
            }
        }
        List<Ligne> lignes = classement(toutes);
        int n = lignes.size();
        for (int i = 0; i < n; i++) {
            Grille g = grille(lignes.get(i).uuid());
            int classique = topClassique(i + 1);
            int pourcent = topPourcent(i + 1, n);
            if (classique > 0) {
                g.topsAtteints.add("top-" + classique);
            }
            for (int x : TOPS_POURCENT) {
                if (pourcent > 0 && x >= pourcent) {
                    g.topsAtteints.add("top-" + x + "p");
                }
            }
        }
        initialise = true;
        moisEnCours = moisActuel().toString();
        sauver();
        plugin.getLogger().info("Premier démarrage : point de départ enregistré pour " + toutes.size() + " joueur(s).");
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        initialise = yaml.getBoolean("initialise", false);
        moisEnCours = yaml.getString("mois-en-cours", null);
        ConfigurationSection js = yaml.getConfigurationSection("joueurs");
        if (js == null) {
            return;
        }
        for (String cle : js.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(cle);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection s = js.getConfigurationSection(cle);
            Grille g = grille(uuid);
            g.prestige = s.getInt("prestige");
            g.decalage = s.getDouble("decalage");
            ConfigurationSection ps = s.getConfigurationSection("paliers");
            if (ps != null) {
                ps.getKeys(false).forEach(k -> g.paliers.put(k.replace('|', ':'), ps.getDouble(k)));
            }
            g.topsAtteints.addAll(s.getStringList("tops-atteints"));
            g.entree = s.getLong("suivi.entree");
            g.echeance = s.getLong("suivi.echeance");
            g.pireClassique = s.getInt("suivi.pire-classique");
            g.pirePourcent = s.getInt("suivi.pire-pourcent");
        }
    }

    void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("initialise", initialise);
        yaml.set("mois-en-cours", moisEnCours);
        grilles.forEach((uuid, g) -> {
            String b = "joueurs." + uuid;
            yaml.set(b + ".prestige", g.prestige);
            yaml.set(b + ".decalage", g.decalage);
            g.paliers.forEach((k, v) -> yaml.set(b + ".paliers." + k.replace(':', '|'), v));
            yaml.set(b + ".tops-atteints", new ArrayList<>(g.topsAtteints));
            yaml.set(b + ".suivi.entree", g.entree);
            yaml.set(b + ".suivi.echeance", g.echeance);
            yaml.set(b + ".suivi.pire-classique", g.pireClassique);
            yaml.set(b + ".suivi.pire-pourcent", g.pirePourcent);
        });
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer joueurs.yml : " + e.getMessage());
        }
    }
}
