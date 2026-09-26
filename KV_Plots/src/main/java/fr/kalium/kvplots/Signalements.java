package fr.kalium.kvplots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Signalements de plots, gardés dans signalements.yml (consultés et traités par le staff dans KV_Menu). */
final class Signalements {

    /** Un signalement ; classé = traité par le staff (avec l'action faite). */
    static final class Signalement {
        final int id, plot;
        final UUID auteur;
        final List<String> raisons;
        final String autre;
        final long date;
        boolean classe;
        UUID traitePar;
        String action = "";

        Signalement(int id, int plot, UUID auteur, List<String> raisons, String autre, long date) {
            this.id = id;
            this.plot = plot;
            this.auteur = auteur;
            this.raisons = raisons;
            this.autre = autre;
            this.date = date;
        }
    }

    private final KVPlots plugin;
    private final File fichier;
    private final Map<Integer, Signalement> parId = new LinkedHashMap<>();
    private int prochainId = 1;

    Signalements(KVPlots plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "signalements.yml");
    }

    Signalement parId(int id) {
        return parId.get(id);
    }

    /** Du plus récent au plus ancien. */
    List<Signalement> liste(boolean classes) {
        List<Signalement> l = new ArrayList<>();
        for (Signalement s : parId.values()) {
            if (s.classe == classes) l.add(s);
        }
        l.sort(Comparator.comparingLong((Signalement s) -> s.date).reversed());
        return l;
    }

    /** Signalement non classé de ce joueur sur ce plot, ou null. */
    Signalement ouvert(UUID auteur, int plot) {
        for (Signalement s : parId.values()) {
            if (!s.classe && s.plot == plot && s.auteur.equals(auteur)) return s;
        }
        return null;
    }

    Signalement ajouter(int plot, UUID auteur, List<String> raisons, String autre) {
        Signalement s = new Signalement(prochainId++, plot, auteur, raisons, autre, System.currentTimeMillis());
        parId.put(s.id, s);
        sauver();
        return s;
    }

    void charger() {
        parId.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        prochainId = yml.getInt("prochain-id", 1);
        ConfigurationSection liste = yml.getConfigurationSection("signalements");
        if (liste == null) return;
        for (String cle : liste.getKeys(false)) {
            ConfigurationSection c = liste.getConfigurationSection(cle);
            try {
                Signalement s = new Signalement(Integer.parseInt(cle), c.getInt("plot"), UUID.fromString(c.getString("auteur")),
                        c.getStringList("raisons"), c.getString("autre", ""), c.getLong("date"));
                s.classe = c.getBoolean("classe");
                String t = c.getString("traite-par");
                s.traitePar = t == null ? null : UUID.fromString(t);
                s.action = c.getString("action", "");
                parId.put(s.id, s);
                prochainId = Math.max(prochainId, s.id + 1);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Signalement " + cle + " illisible : ignoré", e);
            }
        }
    }

    void sauver() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("prochain-id", prochainId);
        for (Signalement s : parId.values()) {
            String b = "signalements." + s.id + ".";
            yml.set(b + "plot", s.plot);
            yml.set(b + "auteur", s.auteur.toString());
            yml.set(b + "raisons", s.raisons);
            yml.set(b + "autre", s.autre);
            yml.set(b + "date", s.date);
            yml.set(b + "classe", s.classe);
            yml.set(b + "traite-par", s.traitePar == null ? null : s.traitePar.toString());
            yml.set(b + "action", s.action);
        }
        try {
            yml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer signalements.yml", e);
        }
    }
}
