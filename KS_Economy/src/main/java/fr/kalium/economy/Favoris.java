package fr.kalium.economy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.2.0 - favoris du catalogue (LeKiwi06 : « il faudrait aussi un onglet favoris ») : magasins (« m:<propriétaire> »)
 * et boutiques (« b:<id> ») de chaque joueur. plugins/KS_Economy/favoris.yml.
 */
final class Favoris {

    private final KSEconomy plugin;
    private final File fichier;
    private final Map<UUID, Set<String>> favoris = new HashMap<>();

    Favoris(KSEconomy plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "favoris.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection s = yaml.getConfigurationSection("favoris");
        if (s != null) {
            for (String cle : s.getKeys(false)) {
                try {
                    favoris.put(UUID.fromString(cle), new LinkedHashSet<>(s.getStringList(cle)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Favoris ignorés : " + cle);
                }
            }
        }
    }

    static String magasin(UUID proprio) {
        return "m:" + proprio;
    }

    static String boutique(String id) {
        return "b:" + id;
    }

    boolean contient(UUID joueur, String cle) {
        return favoris.getOrDefault(joueur, Set.of()).contains(cle);
    }

    /** Ajoute ou retire ; vrai si c'est maintenant un favori. */
    boolean basculer(UUID joueur, String cle) {
        Set<String> s = favoris.computeIfAbsent(joueur, u -> new LinkedHashSet<>());
        boolean ajoute = s.add(cle);
        if (!ajoute) {
            s.remove(cle);
        }
        sauver();
        return ajoute;
    }

    List<String> de(UUID joueur) {
        return new ArrayList<>(favoris.getOrDefault(joueur, Set.of()));
    }

    void retirer(UUID joueur, String cle) {
        Set<String> s = favoris.get(joueur);
        if (s != null && s.remove(cle)) {
            sauver();
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        favoris.forEach((u, s) -> {
            if (!s.isEmpty()) {
                yaml.set("favoris." + u, new ArrayList<>(s));
            }
        });
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer favoris.yml : " + e.getMessage());
        }
    }
}
