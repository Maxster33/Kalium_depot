package fr.kalium.rewards;

import fr.kalium.scoreboards.KGScoreBoards;
import fr.kalium.scoreboards.data.StatsService;
import fr.kalium.scoreboards.data.StatsService.Row;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Moteur des récompenses (cahier des charges, catégorie 4, validé le 30/09/2026).
 *
 * - Grilles : chaque mini-jeu de KG_ScoreBoards + « general » (paliers : somme des points de tous les jeux ; tops :
 *   moyenne des 5 meilleurs scores de jeu, 0 pour les jeux manquants).
 * - Paliers (semaine, mois, permanent) : minimale tous les 100 jusqu'à 1 000, petite tous les 500 jusqu'à 10 000,
 *   moyenne tous les 1 000 jusqu'à 50 000, grosse tous les 10 000 (aussi au-delà de 100 000), exceptionnelle tous les
 *   100 000 ; à un seuil commun, seulement la plus grosse ; chaque seuil une fois par période (et par prestige pour le
 *   permanent). Aucun palier rétroactif : au premier démarrage, les scores actuels servent de point de départ.
 * - Prestige (paliers permanents, par grille, dès 100 000) : compteur des paliers permanents remis à 0, +20 % de
 *   quantité par niveau sur les paliers permanents seulement ; classements de KG_ScoreBoards intacts.
 * - Tops : classiques 1, 2, 3, 5, 10, 25, 50, 100 (seul le meilleur) et pourcentages 50, 25, 10, 5, 1 % (cumulés ; rang
 *   <= x % des joueurs qui ont au moins 1 point, arrondi au-dessus). Semaine et mois : classement final à la clôture de
 *   KG_ScoreBoards. Permanent : à l'arrivée (une fois par top) puis chaque semaine passée dans le top depuis l'arrivée
 *   (le moins bon top occupé pendant la semaine compte ; le compte repart à zéro en sortant des tops).
 * - 1.3.0 (LeKiwi06, 09/10/2026) : plus de paliers de la semaine (il reste ceux du mois et les permanents ; les tops
 *   de la semaine restent) ; plus de tops 100, 50, 25 et 10 (il reste 5, 3, 2, 1 et les tops en %).
 */
final class Moteur {

    /** 1.3.0 (LeKiwi06, 09/10/2026 : « on supprime [...] les top 100, 50, 25, et 10 ») : il reste 5, 3, 2 et 1. */
    static final int[] TOPS = {5, 3, 2, 1};
    /** 1.3.0 (LeKiwi06, 09/10/2026 : « on supprime les paliers de semaines ») : paliers du mois et permanents. */
    static final List<String> PERIODES_PALIERS = List.of("mois", "permanent");
    static final int[] TOPS_POURCENT = {50, 25, 10, 5, 1};
    static final String GENERAL = "general";
    private static final long SEMAINE_MS = 7L * 24 * 60 * 60 * 1000;
    static final double SEUIL_PRESTIGE = 100_000;

    /** Grille d'un joueur. */
    static final class Grille {
        int prestige;
        /** Score permanent au moment du dernier prestige (le compteur des paliers permanents part de là). */
        double decalage;
        /** Clé de période (« semaine:2026-10-03 », « mois:2026-10 », « permanent:0 ») -> plus haut seuil récompensé. */
        final Map<String, Double> paliers = new LinkedHashMap<>();
        /** Tops permanents déjà atteints (« top-3 », « top-10p »...). */
        final Set<String> topsAtteints = new LinkedHashSet<>();
        long entree;
        long echeance;
        int pireClassique;
        int pirePourcent;
    }

    static final class Joueur {
        String nom = "?";
        final Map<String, Grille> grilles = new LinkedHashMap<>();
    }

    private final KGRewards plugin;
    private final Butin butin;
    private final Envois envois;
    private final File fichier;
    final Map<UUID, Joueur> joueurs = new LinkedHashMap<>();
    private boolean initialise;

    Moteur(KGRewards plugin, Butin butin, Envois envois) {
        this.plugin = plugin;
        this.butin = butin;
        this.envois = envois;
        this.fichier = new File(plugin.getDataFolder(), "joueurs.yml");
        charger();
    }

    private static StatsService stats() {
        return ((KGScoreBoards) Bukkit.getPluginManager().getPlugin("KG_ScoreBoards")).stats();
    }

    Joueur joueur(UUID uuid) {
        return joueurs.computeIfAbsent(uuid, u -> new Joueur());
    }

    Grille grille(UUID uuid, String grille) {
        return joueur(uuid).grilles.computeIfAbsent(grille, g -> new Grille());
    }

    int prestige(String grille, UUID uuid) {
        Joueur j = joueurs.get(uuid);
        Grille g = j == null ? null : j.grilles.get(grille);
        return g == null ? 0 : g.prestige;
    }

    static String nomGrille(String grille) {
        if (GENERAL.equals(grille)) {
            return "général";
        }
        KGScoreBoards sb = (KGScoreBoards) Bukkit.getPluginManager().getPlugin("KG_ScoreBoards");
        var categorie = sb == null ? null : sb.category(grille);
        return categorie == null ? grille : PlainTextComponentSerializer.plainText().serialize(categorie.name());
    }

    static String nomPeriode(String type) {
        return switch (type) {
            case "semaine" -> "semaine";
            case "mois" -> "mois";
            default -> "permanent";
        };
    }

    // ------------------------------------------------------------------ scores

    /** Score d'une grille pour une période de KG_ScoreBoards (« general », « mois », « semaine »). */
    static double score(String periode, String grille, UUID uuid) {
        StatsService stats = stats();
        if (!GENERAL.equals(grille)) {
            Row row = stats.periodRow(periode, grille, uuid);
            return row == null ? 0 : row.points();
        }
        double total = 0;
        for (String jeu : stats.minigamesWithPlayers()) {
            Row row = stats.periodRow(periode, jeu, uuid);
            total += row == null ? 0 : row.points();
        }
        return total;
    }

    /** Compteur des paliers : semaine / mois tels quels ; permanent depuis le dernier prestige. */
    double compteur(String type, String grille, UUID uuid) {
        return switch (type) {
            case "semaine" -> score("semaine", grille, uuid);
            case "mois" -> score("mois", grille, uuid);
            default -> Math.max(0, score("general", grille, uuid) - grille(uuid, grille).decalage);
        };
    }

    private String clePeriode(String type, Grille g) {
        return switch (type) {
            case "semaine" -> "semaine:" + stats().weekKey();
            case "mois" -> "mois:" + stats().monthKey();
            default -> "permanent:" + g.prestige;
        };
    }

    // ------------------------------------------------------------------ paliers

    /** Catégorie d'un seuil, ou null si ce n'est pas un seuil. La plus grosse à un seuil commun. */
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

    /** Seuils strictement au-dessus de « depuis » et jusqu'à « jusqua » compris. */
    static List<Long> seuils(double depuis, double jusqua) {
        List<Long> liste = new ArrayList<>();
        long t = ((long) Math.floor(depuis) / 100 + 1) * 100;
        for (; t <= jusqua; t += 100) {
            if (categorie(t) != null) {
                liste.add(t);
            }
        }
        return liste;
    }

    /** Prochain seuil au-dessus d'un compteur (affichage). */
    static long prochainSeuil(double compteur) {
        long t = ((long) Math.floor(compteur) / 100 + 1) * 100;
        while (categorie(t) == null) {
            t += 100;
        }
        return t;
    }

    /** Points ajoutés (KG_ScoreBoards) : paliers de la grille du jeu et de la grille générale (mois et permanent). */
    void pointsAjoutes(String jeu, UUID uuid, String nom) {
        joueur(uuid).nom = nom;
        for (String grille : List.of(jeu, GENERAL)) {
            for (String type : PERIODES_PALIERS) {
                Grille g = grille(uuid, grille);
                String cle = clePeriode(type, g);
                double compteur = compteur(type, grille, uuid);
                double dernier = g.paliers.getOrDefault(cle, 0.0);
                if (compteur <= dernier) {
                    continue;
                }
                List<Long> franchis = seuils(dernier, compteur);
                if (franchis.isEmpty()) {
                    continue;
                }
                double multiplicateur = "permanent".equals(type) ? 1 + 0.2 * g.prestige : 1;
                for (long t : franchis) {
                    String cat = categorie(t);
                    envois.envoyer(uuid, nom, "Palier " + t + " points - " + nomGrille(grille) + " ("
                                    + nomPeriode(type) + (g.prestige > 0 && "permanent".equals(type) ? ", prestige "
                                    + g.prestige : "") + ")", butin.tirer(type, cat, multiplicateur));
                }
                g.paliers.put(cle, (double) franchis.get(franchis.size() - 1));
            }
        }
        sauver();
    }

    // ------------------------------------------------------------------ prestige

    boolean prestigePossible(UUID uuid, String grille) {
        return compteur("permanent", grille, uuid) >= SEUIL_PRESTIGE;
    }

    void prendrePrestige(UUID uuid, String grille) {
        Grille g = grille(uuid, grille);
        g.prestige++;
        g.decalage = score("general", grille, uuid);
        sauver();
        plugin.getLogger().info("Prestige : " + joueur(uuid).nom + " passe au prestige " + g.prestige + " ("
                + nomGrille(grille) + ").");
    }

    // ------------------------------------------------------------------ tops

    /** Une ligne de classement (joueur, score). */
    record Ligne(UUID uuid, String nom, double score) {
    }

    private static List<Ligne> lignes(List<Row> rows) {
        List<Ligne> liste = new ArrayList<>();
        for (Row r : rows) {
            if (r.points() >= 1) {
                liste.add(new Ligne(r.uuid(), r.name(), r.points()));
            }
        }
        return liste;
    }

    /** Classement global : moyenne des 5 meilleurs scores de jeu (0 pour les jeux manquants). */
    static List<Ligne> global(Map<String, List<Row>> parJeu) {
        Map<UUID, List<Double>> scores = new HashMap<>();
        Map<UUID, String> noms = new HashMap<>();
        parJeu.values().forEach(rows -> rows.forEach(r -> {
            scores.computeIfAbsent(r.uuid(), u -> new ArrayList<>()).add(r.points());
            noms.put(r.uuid(), r.name());
        }));
        List<Ligne> liste = new ArrayList<>();
        scores.forEach((uuid, s) -> {
            s.sort(Comparator.reverseOrder());
            double total = 0;
            for (int i = 0; i < 5 && i < s.size(); i++) {
                total += s.get(i);
            }
            double moyenne = total / 5;
            if (moyenne >= 1) {
                liste.add(new Ligne(uuid, noms.get(uuid), moyenne));
            }
        });
        liste.sort(Comparator.comparingDouble(Ligne::score).reversed());
        return liste;
    }

    /** Meilleur top classique d'un rang (0 : aucun). */
    static int topClassique(int rang) {
        int meilleur = 0;
        for (int x : TOPS) {
            if (rang <= x) {
                meilleur = x;
            }
        }
        return meilleur;
    }

    /** Meilleur top % d'un rang parmi n joueurs (0 : aucun). */
    static int topPourcent(int rang, int n) {
        int meilleur = 0;
        for (int x : TOPS_POURCENT) {
            if (rang <= (int) Math.ceil(x * n / 100.0)) {
                meilleur = x;
            }
        }
        return meilleur;
    }

    /** Fin de semaine ou de mois : tops du classement final de chaque jeu et du global. */
    void periodeCloturee(String type, Map<String, List<Row>> classements) {
        String libelle = "semaine".equals(type) ? "de la semaine" : "du mois";
        classements.forEach((jeu, rows) -> attribuerTopsFinaux(type, jeu, lignes(rows), libelle));
        attribuerTopsFinaux(type, GENERAL, global(classements), libelle);
        sauver();
    }

    private void attribuerTopsFinaux(String type, String grille, List<Ligne> lignes, String libelle) {
        int n = lignes.size();
        for (int i = 0; i < n; i++) {
            Ligne l = lignes.get(i);
            int rang = i + 1;
            int classique = topClassique(rang);
            if (classique > 0) {
                envois.envoyer(l.uuid(), l.nom(), "Top " + classique + " " + libelle + " - " + nomGrille(grille),
                        butin.tirer(type, "top-" + classique, 1));
            }
            int pourcent = topPourcent(rang, n);
            for (int x : TOPS_POURCENT) {
                if (pourcent > 0 && x >= pourcent) {
                    envois.envoyer(l.uuid(), l.nom(), "Top " + x + " % " + libelle + " - " + nomGrille(grille),
                            butin.tirer(type, "top-" + x + "p", 1));
                }
            }
        }
    }

    /** Toutes les 5 minutes : tops permanents (arrivée, semaines passées dans le top). */
    void verifierTopsPermanents() {
        StatsService stats = stats();
        Map<String, List<Row>> parJeu = new LinkedHashMap<>();
        for (String jeu : stats.minigamesWithPlayers()) {
            parJeu.put(jeu, stats.periodRanking("general", jeu));
        }
        long maintenant = System.currentTimeMillis();
        parJeu.forEach((jeu, rows) -> suivreTops(jeu, lignes(rows), maintenant));
        suivreTops(GENERAL, global(parJeu), maintenant);
        sauver();
    }

    private void suivreTops(String grille, List<Ligne> lignes, long maintenant) {
        int n = lignes.size();
        Set<UUID> presents = new HashSet<>();
        for (int i = 0; i < n; i++) {
            Ligne l = lignes.get(i);
            int classique = topClassique(i + 1);
            int pourcent = topPourcent(i + 1, n);
            if (classique == 0 && pourcent == 0) {
                continue;
            }
            presents.add(l.uuid());
            joueur(l.uuid()).nom = l.nom();
            Grille g = grille(l.uuid(), grille);
            // Arrivée : une fois par top (classique : seul le meilleur ; % : tous ceux atteints).
            if (classique > 0 && g.topsAtteints.add("top-" + classique)) {
                envois.envoyer(l.uuid(), l.nom(), "Arrivée dans le top " + classique + " - " + nomGrille(grille),
                        butin.tirer("permanent", "top-" + classique, 1));
            }
            for (int x : TOPS_POURCENT) {
                if (pourcent > 0 && x >= pourcent && g.topsAtteints.add("top-" + x + "p")) {
                    envois.envoyer(l.uuid(), l.nom(), "Arrivée dans le top " + x + " % - " + nomGrille(grille),
                            butin.tirer("permanent", "top-" + x + "p", 1));
                }
            }
            // Semaines passées dans le top.
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
                    envois.envoyer(l.uuid(), l.nom(), "Semaine dans le top " + g.pireClassique + " - " + nomGrille(grille),
                            butin.tirer("permanent", "top-" + g.pireClassique, 1));
                }
                for (int x : TOPS_POURCENT) {
                    if (g.pirePourcent > 0 && x >= g.pirePourcent) {
                        envois.envoyer(l.uuid(), l.nom(), "Semaine dans le top " + x + " % - " + nomGrille(grille),
                                butin.tirer("permanent", "top-" + x + "p", 1));
                    }
                }
                g.pireClassique = classique;
                g.pirePourcent = pourcent;
                g.echeance += SEMAINE_MS;
            }
        }
        // Sortis de tous les tops : le compte des semaines repart à zéro.
        for (Map.Entry<UUID, Joueur> e : joueurs.entrySet()) {
            Grille g = e.getValue().grilles.get(grille);
            if (g != null && g.entree != 0 && !presents.contains(e.getKey())) {
                g.entree = 0;
                g.echeance = 0;
            }
        }
    }

    // ------------------------------------------------------------------ démarrage

    /**
     * Premier démarrage : point de départ des paliers = scores actuels (semaine, mois, permanent), pour ne pas donner
     * d'un coup tous les paliers déjà dépassés ; les tops permanents déjà occupés comptent comme atteints.
     */
    void initialiserSiBesoin() {
        if (initialise) {
            return;
        }
        StatsService stats = stats();
        Set<UUID> tous = new LinkedHashSet<>();
        Map<UUID, String> noms = new HashMap<>();
        for (String jeu : stats.minigamesWithPlayers()) {
            for (Row r : stats.periodRanking("general", jeu)) {
                tous.add(r.uuid());
                noms.put(r.uuid(), r.name());
            }
        }
        for (UUID uuid : tous) {
            joueur(uuid).nom = noms.get(uuid);
            for (String jeu : new ArrayList<>(stats.minigamesWithPlayers())) {
                depart(uuid, jeu);
            }
            depart(uuid, GENERAL);
        }
        Map<String, List<Row>> parJeu = new LinkedHashMap<>();
        for (String jeu : stats.minigamesWithPlayers()) {
            parJeu.put(jeu, stats.periodRanking("general", jeu));
        }
        parJeu.forEach((jeu, rows) -> marquerTopsAtteints(jeu, lignes(rows)));
        marquerTopsAtteints(GENERAL, global(parJeu));
        initialise = true;
        sauver();
        plugin.getLogger().info("Premier démarrage : point de départ des paliers et des tops enregistré pour "
                + tous.size() + " joueur(s).");
    }

    private void depart(UUID uuid, String grille) {
        Grille g = grille(uuid, grille);
        for (String type : Butin.PERIODES) {
            g.paliers.put(clePeriode(type, g), compteur(type, grille, uuid));
        }
    }

    private void marquerTopsAtteints(String grille, List<Ligne> lignes) {
        int n = lignes.size();
        for (int i = 0; i < n; i++) {
            Grille g = grille(lignes.get(i).uuid(), grille);
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
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        initialise = yaml.getBoolean("initialise", false);
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
            Joueur j = joueur(uuid);
            j.nom = js.getString(cle + ".nom", "?");
            ConfigurationSection gs = js.getConfigurationSection(cle + ".grilles");
            if (gs == null) {
                continue;
            }
            for (String id : gs.getKeys(false)) {
                ConfigurationSection s = gs.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Grille g = grille(uuid, id);
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
    }

    void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("initialise", initialise);
        joueurs.forEach((uuid, j) -> {
            String cle = "joueurs." + uuid;
            yaml.set(cle + ".nom", j.nom);
            j.grilles.forEach((id, g) -> {
                String b = cle + ".grilles." + id;
                yaml.set(b + ".prestige", g.prestige);
                yaml.set(b + ".decalage", g.decalage);
                // « : » interdit dans une clé YAML de Bukkit : remplacé par « | ».
                g.paliers.forEach((k, v) -> yaml.set(b + ".paliers." + k.replace(':', '|'), v));
                yaml.set(b + ".tops-atteints", new ArrayList<>(g.topsAtteints));
                yaml.set(b + ".suivi.entree", g.entree);
                yaml.set(b + ".suivi.echeance", g.echeance);
                yaml.set(b + ".suivi.pire-classique", g.pireClassique);
                yaml.set(b + ".suivi.pire-pourcent", g.pirePourcent);
            });
        });
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer joueurs.yml : " + e.getMessage());
        }
    }
}
