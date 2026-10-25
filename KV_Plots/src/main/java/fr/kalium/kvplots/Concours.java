package fr.kalium.kvplots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.kalium.kvplots.api.KanvasPlots.PhaseConcours;
import fr.kalium.kvplots.api.Taille;

/**
 * Concours de build (demande de LeKiwi06, 26/09/2026), gardés dans concours.yml. 1.7.0 (LeKiwi06, 11/10/2026) : un
 * seul concours en construction à la fois, mais il peut démarrer pendant les votes du précédent. Phases :
 * EN_COURS (participation et construction jusqu'à « fin »), puis VOTES (plots figés et notables jusqu'à « finVotes »),
 * puis TERMINE (votes clos, plots gardés à part) ; ANNULE si le staff l'annule. 1.8.0 (LeKiwi06, 25/10/2026) : les plots
 * d'un concours terminé restent notables comme des plots normaux ; son classement est figé à la clôture des votes.
 */
final class Concours {

    /** 1.8.0 : place au classement figé d'un concours terminé : plot, points et nombre de votes à la clôture. */
    record Place(int plot, int points, int votes) {}

    static final class Un {
        final int id;
        String theme;
        final Taille taille;
        PhaseConcours phase = PhaseConcours.EN_COURS;
        final long debut;
        long fin, dureeVotes, finVotes;
        /** 1.8.0 : classement à la clôture des votes (concours terminé), du premier au dernier. */
        final List<Place> classement = new ArrayList<>();

        Un(int id, String theme, Taille taille, long debut, long fin, long dureeVotes) {
            this.id = id;
            this.theme = theme;
            this.taille = taille;
            this.debut = debut;
            this.fin = fin;
            this.dureeVotes = dureeVotes;
        }

        boolean actif() {
            return phase == PhaseConcours.EN_COURS || phase == PhaseConcours.VOTES;
        }
    }

    private final KVPlots plugin;
    private final File fichier;
    private final Map<Integer, Un> parId = new LinkedHashMap<>();
    private int prochainId = 1;

    Concours(KVPlots plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "concours.yml");
    }

    Un parId(int id) {
        return parId.get(id);
    }

    /** Concours en construction (un seul à la fois), ou null. */
    Un enConstruction() {
        for (Un c : parId.values()) {
            if (c.phase == PhaseConcours.EN_COURS) return c;
        }
        return null;
    }

    /** 1.7.0 : concours actifs : celui en construction d'abord, puis ceux en votes, du plus ancien au plus récent. */
    List<Un> actifs() {
        List<Un> l = new ArrayList<>();
        Un construction = enConstruction();
        if (construction != null) l.add(construction);
        for (Un c : parId.values()) {
            if (c.phase == PhaseConcours.VOTES) l.add(c);
        }
        return l;
    }

    /** Premier concours actif (celui en construction, sinon le plus ancien en votes), ou null. */
    Un actuel() {
        List<Un> l = actifs();
        return l.isEmpty() ? null : l.get(0);
    }

    /** Concours terminés, du plus récent au plus ancien. */
    List<Un> termines() {
        List<Un> l = new ArrayList<>();
        for (Un c : parId.values()) {
            if (c.phase == PhaseConcours.TERMINE) l.add(c);
        }
        l.sort(Comparator.comparingLong((Un c) -> c.debut).reversed());
        return l;
    }

    Un creer(String theme, Taille taille, long fin, long dureeVotes) {
        Un c = new Un(prochainId++, theme, taille, System.currentTimeMillis(), fin, dureeVotes);
        parId.put(c.id, c);
        sauver();
        return c;
    }

    /** « 2 j 3 h 10 min » (au moins « 0 min »). */
    static String duree(long ms) {
        long minutes = Math.max(0, ms) / 60_000L;
        long j = minutes / 1440, h = minutes / 60 % 24, m = minutes % 60;
        StringBuilder sb = new StringBuilder();
        if (j > 0) sb.append(j).append(" j ");
        if (j > 0 || h > 0) sb.append(h).append(" h ");
        return sb.append(m).append(" min").toString();
    }

    /** 1.7.0 : durée courte pour le tableau sur le côté : « 2 j 3 h », « 3 h 10 min », « 10 min ». */
    static String dureeCourte(long ms) {
        long minutes = Math.max(0, ms) / 60_000L;
        long j = minutes / 1440, h = minutes / 60 % 24, m = minutes % 60;
        return j > 0 ? j + " j " + h + " h" : h > 0 ? h + " h " + m + " min" : m + " min";
    }

    void charger() {
        parId.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        prochainId = yml.getInt("prochain-id", 1);
        ConfigurationSection liste = yml.getConfigurationSection("concours");
        if (liste == null) return;
        for (String cle : liste.getKeys(false)) {
            ConfigurationSection s = liste.getConfigurationSection(cle);
            try {
                Un c = new Un(Integer.parseInt(cle), s.getString("theme", ""), Taille.valueOf(s.getString("taille")),
                        s.getLong("debut"), s.getLong("fin"), s.getLong("duree-votes"));
                c.phase = PhaseConcours.valueOf(s.getString("phase", "EN_COURS"));
                c.finVotes = s.getLong("fin-votes");
                for (String place : s.getStringList("classement")) { // « plot:points:votes »
                    String[] v = place.split(":");
                    c.classement.add(new Place(Integer.parseInt(v[0]), Integer.parseInt(v[1]), Integer.parseInt(v[2])));
                }
                parId.put(c.id, c);
                prochainId = Math.max(prochainId, c.id + 1);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Concours " + cle + " illisible : ignoré", e);
            }
        }
    }

    void sauver() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("prochain-id", prochainId);
        for (Un c : parId.values()) {
            String b = "concours." + c.id + ".";
            yml.set(b + "theme", c.theme);
            yml.set(b + "taille", c.taille.name());
            yml.set(b + "phase", c.phase.name());
            yml.set(b + "debut", c.debut);
            yml.set(b + "fin", c.fin);
            yml.set(b + "duree-votes", c.dureeVotes);
            yml.set(b + "fin-votes", c.finVotes);
            if (!c.classement.isEmpty()) {
                yml.set(b + "classement", c.classement.stream().map(pl -> pl.plot() + ":" + pl.points() + ":" + pl.votes()).toList());
            }
        }
        try {
            yml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer concours.yml", e);
        }
    }
}
