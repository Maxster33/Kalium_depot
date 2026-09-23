package fr.kalium.core.market;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Table de noms d'objets anglais/francais chargee depuis item_names.tsv (ressource du plugin,
 * extraite des fichiers de langue officiels de Minecraft - voir JOURNAL.md pour la methode de
 * generation). Utilisee uniquement pour que la recherche du commerce fonctionne sur les deux
 * langues, en plus de l'identifiant technique (nom du Material) et du nom personnalise de l'objet
 * le cas echeant (voir MarketService#searchText).
 */
public final class ItemNames {

    private final Map<Material, String> english = new EnumMap<>(Material.class);
    private final Map<Material, String> french = new EnumMap<>(Material.class);

    public ItemNames(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("item_names.tsv")) {
            if (in == null) {
                plugin.getLogger().warning("item_names.tsv introuvable : la recherche du commerce ne fonctionnera qu'en anglais technique.");
                return;
            }
            int loaded = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] parts = line.split("\t", -1);
                    if (parts.length < 3) {
                        continue;
                    }
                    try {
                        Material material = Material.valueOf(parts[0]);
                        if (!parts[1].isBlank()) {
                            english.put(material, parts[1]);
                        }
                        if (!parts[2].isBlank()) {
                            french.put(material, parts[2]);
                        }
                        loaded++;
                    } catch (IllegalArgumentException ignored) {
                        // Material inconnu de cette version du serveur (fichier genere pour une autre version) : ignore.
                    }
                }
            }
            plugin.getLogger().info("Commerce : " + loaded + " noms d'objets (EN/FR) charges pour la recherche.");
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de charger item_names.tsv : " + e.getMessage());
        }
    }

    public String english(Material material) {
        return english.get(material);
    }

    public String french(Material material) {
        return french.get(material);
    }

    /** Minuscules, sans accents (ex. "epee" et "épée" deviennent tous deux "epee") : comparaison insensible aux accents. */
    public static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
