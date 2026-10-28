package fr.kalium.effets;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minuteurs des effets custom et choix de suivi, par compte (compte de test à part, voir {@link CompteTest}), gardés
 * dans effets.yml. Un minuteur ne s'écoule que quand le joueur est en jeu sur ce compte, comme un effet du jeu.
 */
final class Registre {

    /** Temps restant et durée totale (pour la barre de boss) d'un effet, en secondes. */
    static final class Minuteur {
        int restant;
        int total;

        Minuteur(int restant, int total) {
            this.restant = restant;
            this.total = total;
        }
    }

    /** Ce qui est gardé pour un compte. */
    static final class Compte {
        final Map<String, Minuteur> effets = new LinkedHashMap<>();
        /** Effet suivi en barre de boss / en barre d'action (clé d'un effet du jeu ou id d'un effet custom), ou null. */
        String suiviBoss;
        String suiviAction;

        boolean vide() {
            return effets.isEmpty() && suiviBoss == null && suiviAction == null;
        }
    }

    private final JavaPlugin plugin;
    private final File fichier;
    private final Map<UUID, Compte> comptes = new HashMap<>();
    private boolean modifie;

    Registre(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "effets.yml");
    }

    /** Le compte, créé s'il n'existe pas et que creer est vrai ; sinon null. */
    Compte compte(UUID compte, boolean creer) {
        return creer ? comptes.computeIfAbsent(compte, c -> new Compte()) : comptes.get(compte);
    }

    void modifie() {
        modifie = true;
    }

    void charger() {
        comptes.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection racine = yml.getConfigurationSection("comptes");
        if (racine == null) {
            return;
        }
        for (String cle : racine.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(cle);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection s = racine.getConfigurationSection(cle);
            Compte c = new Compte();
            ConfigurationSection effets = s.getConfigurationSection("effets");
            if (effets != null) {
                for (String id : effets.getKeys(false)) {
                    int restant = effets.getInt(id + ".restant");
                    if (restant > 0) {
                        c.effets.put(id, new Minuteur(restant, Math.max(restant, effets.getInt(id + ".total"))));
                    }
                }
            }
            c.suiviBoss = s.getString("suivi-boss");
            c.suiviAction = s.getString("suivi-action");
            if (!c.vide()) {
                comptes.put(uuid, c);
            }
        }
    }

    /** Enregistre effets.yml s'il a changé (forcer : à l'arrêt, toujours). */
    void enregistrer(boolean forcer) {
        if (!modifie && !forcer) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Compte> e : comptes.entrySet()) {
            Compte c = e.getValue();
            if (c.vide()) {
                continue;
            }
            String base = "comptes." + e.getKey();
            for (Map.Entry<String, Minuteur> m : c.effets.entrySet()) {
                yml.set(base + ".effets." + m.getKey() + ".restant", m.getValue().restant);
                yml.set(base + ".effets." + m.getKey() + ".total", m.getValue().total);
            }
            yml.set(base + ".suivi-boss", c.suiviBoss);
            yml.set(base + ".suivi-action", c.suiviAction);
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(fichier);
            modifie = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Impossible d'enregistrer effets.yml : " + ex.getMessage());
        }
    }
}
