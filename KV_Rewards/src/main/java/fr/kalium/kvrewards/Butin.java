package fr.kalium.kvrewards;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KV_Rewards : copie de la classe de KG_Rewards (mêmes règles), périodes de Kanvas.
 * Tables de butin (décisions de LeKiwi06, 03/10/2026) : des POOLS par rareté, communs à tout (chaque pool : des entrées,
 * chacune un contenu et un poids) ; pour chaque NIVEAU de récompense (paliers minimale... exceptionnelle, tops %, tops 1
 * à 100) et chaque PÉRIODE (semaine, mois, permanent) : un nombre de tirages, la fréquence (%) de chaque pool et des
 * récompenses fixes données en plus. Enregistré dans plugins/KV_Rewards/butin.yml (modifié par l'interface admin).
 *
 * Un élément de contenu suit le format de KS_RewardsGUI : {type: objet, donnees: base64, nombre: n},
 * {type: custom, id: id_custom, nombre: n}, {type: argent, montant: n}.
 */
final class Butin {

    /** Kanvas : mois et permanent (cahier), plus les places des concours de build. */
    static final List<String> PERIODES = List.of("mois", "permanent", "concours");

    /** Niveaux : id -> nom affiché, dans l'ordre. */
    static final Map<String, String> NIVEAUX = new LinkedHashMap<>();

    /** Niveaux des concours de build (récompense par place). */
    static final Map<String, String> NIVEAUX_CONCOURS = new LinkedHashMap<>();

    static {
        NIVEAUX.put("minimale", "Palier : minimale");
        NIVEAUX.put("petite", "Palier : petite");
        NIVEAUX.put("moyenne", "Palier : moyenne");
        NIVEAUX.put("grosse", "Palier : grosse");
        NIVEAUX.put("exceptionnelle", "Palier : exceptionnelle");
        for (int x : Moteur.TOPS_POURCENT) {
            NIVEAUX.put("top-" + x + "p", "Top " + x + " %");
        }
        for (int x : Moteur.TOPS) {
            NIVEAUX.put("top-" + x, "Top " + x);
        }
        for (int place = 1; place <= Moteur.PLACES_CONCOURS; place++) {
            NIVEAUX_CONCOURS.put("concours-" + place, place == 1 ? "1re place" : place + "e place");
        }
    }

    /** Niveaux d'une période. */
    static Map<String, String> niveauxDe(String periode) {
        return "concours".equals(periode) ? NIVEAUX_CONCOURS : NIVEAUX;
    }

    static final class Entree {
        final List<Map<String, Object>> contenu = new ArrayList<>();
        int poids = 1;
    }

    static final class Niveau {
        int tirages;
        final Map<String, Integer> frequences = new LinkedHashMap<>();
        final List<Map<String, Object>> fixes = new ArrayList<>();
    }

    private final File fichier;
    /** Pool -> entrées. */
    final Map<String, List<Entree>> pools = new LinkedHashMap<>();
    /** Période -> niveau -> réglages. */
    final Map<String, Map<String, Niveau>> niveaux = new LinkedHashMap<>();

    Butin(File dossier) {
        this.fichier = new File(dossier, "butin.yml");
        charger();
    }

    Niveau niveau(String periode, String niveau) {
        return niveaux.computeIfAbsent(periode, p -> new LinkedHashMap<>()).computeIfAbsent(niveau, n -> new Niveau());
    }

    // ------------------------------------------------------------------ tirage

    /**
     * Contenu d'une récompense : tirages (pool choisi selon les fréquences, puis entrée selon les poids) + fixes ;
     * quantités multipliées (prestige : objets arrondis au hasard, argent à l'unité inférieure).
     */
    List<Map<String, Object>> tirer(String periode, String niveau, double multiplicateur) {
        Niveau n = niveau(periode, niveau);
        List<Map<String, Object>> contenu = new ArrayList<>();
        ThreadLocalRandom hasard = ThreadLocalRandom.current();
        int totalFrequences = 0;
        for (Map.Entry<String, Integer> f : n.frequences.entrySet()) {
            if (f.getValue() > 0 && pools.containsKey(f.getKey()) && !pools.get(f.getKey()).isEmpty()) {
                totalFrequences += f.getValue();
            }
        }
        for (int i = 0; i < n.tirages && totalFrequences > 0; i++) {
            int tirage = hasard.nextInt(totalFrequences);
            for (Map.Entry<String, Integer> f : n.frequences.entrySet()) {
                List<Entree> entrees = pools.get(f.getKey());
                if (f.getValue() <= 0 || entrees == null || entrees.isEmpty()) {
                    continue;
                }
                tirage -= f.getValue();
                if (tirage < 0) {
                    Entree e = choisir(entrees, hasard);
                    if (e != null) {
                        e.contenu.forEach(el -> contenu.add(new LinkedHashMap<>(el)));
                    }
                    break;
                }
            }
        }
        n.fixes.forEach(el -> contenu.add(new LinkedHashMap<>(el)));
        if (multiplicateur != 1) {
            for (Map<String, Object> el : contenu) {
                if ("argent".equals(el.get("type"))) {
                    el.put("montant", (long) Math.floor(nombre(el.get("montant")) * multiplicateur));
                } else {
                    double valeur = nombre(el.get("nombre")) * multiplicateur;
                    long entier = (long) Math.floor(valeur);
                    if (hasard.nextDouble() < valeur - entier) {
                        entier++;
                    }
                    el.put("nombre", Math.max(1, entier));
                }
            }
        }
        return contenu;
    }

    private static Entree choisir(List<Entree> entrees, ThreadLocalRandom hasard) {
        int total = 0;
        for (Entree e : entrees) {
            total += Math.max(0, e.poids);
        }
        if (total <= 0) {
            return null;
        }
        int tirage = hasard.nextInt(total);
        for (Entree e : entrees) {
            tirage -= Math.max(0, e.poids);
            if (tirage < 0) {
                return e;
            }
        }
        return null;
    }

    static long nombre(Object valeur) {
        return valeur instanceof Number n ? n.longValue() : 0;
    }

    // ------------------------------------------------------------------ enregistrement

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> contenu(List<Map<?, ?>> brut) {
        List<Map<String, Object>> liste = new ArrayList<>();
        for (Map<?, ?> m : brut) {
            Map<String, Object> el = new LinkedHashMap<>();
            m.forEach((k, v) -> el.put(String.valueOf(k), v));
            liste.add(el);
        }
        return liste;
    }

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection ps = yaml.getConfigurationSection("pools");
        if (ps != null) {
            for (String pool : ps.getKeys(false)) {
                List<Entree> entrees = new ArrayList<>();
                for (Map<?, ?> m : ps.getMapList(pool)) {
                    Entree e = new Entree();
                    e.poids = (int) nombre(m.get("poids"));
                    Object c = m.get("contenu");
                    if (c instanceof List<?> l) {
                        List<Map<?, ?>> maps = new ArrayList<>();
                        l.forEach(o -> {
                            if (o instanceof Map<?, ?> mm) {
                                maps.add(mm);
                            }
                        });
                        e.contenu.addAll(contenu(maps));
                    }
                    entrees.add(e);
                }
                pools.put(pool, entrees);
            }
        } else {
            for (String pool : List.of("Commun", "Rare", "Épique", "Légendaire")) {
                pools.put(pool, new ArrayList<>());
            }
        }
        ConfigurationSection ns = yaml.getConfigurationSection("niveaux");
        for (String periode : PERIODES) {
            for (String id : niveauxDe(periode).keySet()) {
                Niveau n = niveau(periode, id);
                ConfigurationSection s = ns == null ? null : ns.getConfigurationSection(periode + "." + id);
                if (s == null) {
                    continue;
                }
                n.tirages = s.getInt("tirages");
                ConfigurationSection f = s.getConfigurationSection("frequences");
                if (f != null) {
                    f.getKeys(false).forEach(pool -> n.frequences.put(pool, f.getInt(pool)));
                }
                n.fixes.addAll(contenu(s.getMapList("fixes")));
            }
        }
    }

    void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        pools.forEach((pool, entrees) -> {
            List<Map<String, Object>> liste = new ArrayList<>();
            for (Entree e : entrees) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("poids", e.poids);
                m.put("contenu", e.contenu);
                liste.add(m);
            }
            yaml.set("pools." + pool, liste);
        });
        niveaux.forEach((periode, parNiveau) -> parNiveau.forEach((id, n) -> {
            String cle = "niveaux." + periode + "." + id;
            yaml.set(cle + ".tirages", n.tirages);
            n.frequences.forEach((pool, f) -> yaml.set(cle + ".frequences." + pool, f));
            yaml.set(cle + ".fixes", n.fixes);
        }));
        try {
            fichier.getParentFile().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            throw new IllegalStateException("butin.yml : " + e.getMessage(), e);
        }
    }
}
