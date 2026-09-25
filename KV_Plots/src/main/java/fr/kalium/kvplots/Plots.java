package fr.kalium.kvplots;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Plots réservés : index par case, lecture et écriture de plots.yml. */
final class Plots {

    private final KVPlots plugin;
    private final File fichier;
    private final Map<Integer, Plot> parId = new HashMap<>();
    private final Map<Grille.Case, Plot> parCase = new HashMap<>();
    private int prochainId = 1;

    Plots(KVPlots plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "plots.yml");
    }

    Collection<Plot> tous() {
        return parId.values();
    }

    Plot parId(int id) {
        return parId.get(id);
    }

    Plot en(Grille.Case c) {
        return c == null ? null : parCase.get(c);
    }

    boolean libre(Grille.Case c) {
        return !parCase.containsKey(c);
    }

    /** Plots dont le joueur est créateur, triés par numéro. */
    List<Plot> duCreateur(UUID joueur) {
        List<Plot> liste = new ArrayList<>();
        for (Plot p : parId.values()) {
            if (p.createur.equals(joueur)) liste.add(p);
        }
        liste.sort(Comparator.comparingInt(p -> p.id));
        return liste;
    }

    /** Plots dont le joueur est créateur ou éditeur, triés par numéro. */
    List<Plot> duJoueur(UUID joueur) {
        List<Plot> liste = new ArrayList<>();
        for (Plot p : parId.values()) {
            if (p.peutConstruire(joueur)) liste.add(p);
        }
        liste.sort(Comparator.comparingInt(p -> p.id));
        return liste;
    }

    int compter(UUID createur, Taille taille) {
        int n = 0;
        for (Plot p : parId.values()) {
            if (p.createur.equals(createur) && p.taille == taille) n++;
        }
        return n;
    }

    Plot creer(Taille taille, Grille.Case c, UUID createur) {
        Plot p = new Plot(prochainId++, taille, c.colonne(), c.ligne(), createur, System.currentTimeMillis());
        ajouter(p);
        return p;
    }

    private void ajouter(Plot p) {
        parId.put(p.id, p);
        for (Grille.Case c : p.cases()) parCase.put(c, p);
    }

    void charger() {
        parId.clear();
        parCase.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        prochainId = yml.getInt("prochain-id", 1);
        ConfigurationSection plots = yml.getConfigurationSection("plots");
        if (plots == null) return;
        for (String cle : plots.getKeys(false)) {
            ConfigurationSection s = plots.getConfigurationSection(cle);
            try {
                Plot p = new Plot(Integer.parseInt(cle), Taille.valueOf(s.getString("taille")),
                        s.getInt("colonne"), s.getInt("ligne"), UUID.fromString(s.getString("createur")),
                        s.getLong("creation"));
                for (String u : s.getStringList("editeurs")) p.editeurs.add(UUID.fromString(u));
                for (String u : s.getStringList("historique-editeurs")) p.historiqueEditeurs.add(UUID.fromString(u));
                p.etat = Plot.Etat.valueOf(s.getString("etat", "TRAVAUX"));
                p.fusionEnCours = s.getBoolean("fusion-en-cours");
                ajouter(p);
                prochainId = Math.max(prochainId, p.id + 1);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Plot " + cle + " illisible dans plots.yml : ignoré", e);
            }
        }
    }

    void sauver() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("prochain-id", prochainId);
        for (Plot p : parId.values()) {
            String b = "plots." + p.id + ".";
            yml.set(b + "taille", p.taille.name());
            yml.set(b + "colonne", p.colonne);
            yml.set(b + "ligne", p.ligne);
            yml.set(b + "createur", p.createur.toString());
            yml.set(b + "creation", p.creation);
            yml.set(b + "editeurs", p.editeurs.stream().map(UUID::toString).toList());
            yml.set(b + "historique-editeurs", p.historiqueEditeurs.stream().map(UUID::toString).toList());
            yml.set(b + "etat", p.etat.name());
            yml.set(b + "fusion-en-cours", p.fusionEnCours);
        }
        try {
            yml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer plots.yml", e);
        }
    }
}
