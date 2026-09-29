package fr.kalium.biomechanger;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Historique des changements de biome (fichier historique.yml du plugin) : date, joueur, monde, position du joueur,
 * biome choisi, forme (sphère ou cube). Chaque changement est inscrit et enregistré tout de suite.
 */
final class Historique {

    /** Un changement de biome. */
    record Changement(long date, UUID joueur, String nom, String monde, int x, int y, int z, String biome, String forme) {
    }

    private final JavaPlugin plugin;
    private final File fichier;
    private final List<Changement> changements = new ArrayList<>();

    Historique(JavaPlugin plugin, File fichier) {
        this.plugin = plugin;
        this.fichier = fichier;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        for (Map<?, ?> ligne : yaml.getMapList("changements")) {
            try {
                changements.add(new Changement(((Number) ligne.get("date")).longValue(),
                        UUID.fromString(String.valueOf(ligne.get("joueur"))), String.valueOf(ligne.get("nom")),
                        String.valueOf(ligne.get("monde")), ((Number) ligne.get("x")).intValue(),
                        ((Number) ligne.get("y")).intValue(), ((Number) ligne.get("z")).intValue(),
                        String.valueOf(ligne.get("biome")),
                        ligne.get("forme") != null ? String.valueOf(ligne.get("forme")) : "sphère"));
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Ligne illisible dans historique.yml, ignorée : " + ligne);
            }
        }
    }

    void ajouter(Player player, Location lieu, Biome biome, KSBiomeChanger.Forme forme) {
        changements.add(new Changement(System.currentTimeMillis(), player.getUniqueId(), player.getName(),
                lieu.getWorld().getName(), lieu.getBlockX(), lieu.getBlockY(), lieu.getBlockZ(), biome.getKey().toString(),
                forme.nom));
        enregistrer();
    }

    private void enregistrer() {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Changement c : changements) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("date", c.date());
            ligne.put("joueur", c.joueur().toString());
            ligne.put("nom", c.nom());
            ligne.put("monde", c.monde());
            ligne.put("x", c.x());
            ligne.put("y", c.y());
            ligne.put("z", c.z());
            ligne.put("biome", c.biome());
            ligne.put("forme", c.forme());
            lignes.add(ligne);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("changements", lignes);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer historique.yml : " + e.getMessage());
        }
    }

    /** Tous les changements, du plus récent au plus ancien. */
    List<Changement> tous() {
        List<Changement> liste = new ArrayList<>(changements);
        Collections.reverse(liste);
        return liste;
    }

    /** Changements d'un joueur, du plus récent au plus ancien. */
    List<Changement> de(UUID joueur) {
        List<Changement> liste = new ArrayList<>();
        for (Changement c : tous()) {
            if (c.joueur().equals(joueur)) {
                liste.add(c);
            }
        }
        return liste;
    }
}
