package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 1.26.0 - réglages retenus de chaque joueur (demande de LeKiwi06, 20/10/2026, ajouts acceptés : « réglages retenus
 * par jeu » et « recréer ma dernière partie »). Fichier plugins/KalGames/reglages.yml :
 * <pre>
 * &lt;uuid&gt;:
 *   jeux:
 *     &lt;mini-jeu&gt;: { arene: ..., options: { maxPlayers: 4, laps: 3, listed: true } }
 *   derniere: { jeu: &lt;mini-jeu&gt;, quand: &lt;millisecondes&gt; }
 * </pre>
 */
public final class Reglages {

    /** Réglages d'une partie : arène (peut être null) et options. */
    public record Choix(String arene, Map<String, Object> options) {
    }

    /** Dernière partie créée : mini-jeu, date, et ses réglages. */
    public record Derniere(String jeu, long quand, Choix choix) {
    }

    private final KalGames plugin;
    private final File fichier;
    private final YamlConfiguration donnees;
    private boolean modifie;

    public Reglages(KalGames plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "reglages.yml");
        this.donnees = YamlConfiguration.loadConfiguration(fichier);
    }

    /** Derniers réglages de ce joueur pour ce mini-jeu, ou null. */
    public Choix choix(UUID joueur, String jeu) {
        ConfigurationSection section = donnees.getConfigurationSection(joueur + ".jeux." + jeu);
        if (section == null) {
            return null;
        }
        Map<String, Object> options = new HashMap<>();
        ConfigurationSection opts = section.getConfigurationSection("options");
        if (opts != null) {
            for (String cle : opts.getKeys(false)) {
                options.put(cle, opts.get(cle));
            }
        }
        return new Choix(section.getString("arene"), options);
    }

    public Derniere derniere(UUID joueur) {
        String jeu = donnees.getString(joueur + ".derniere.jeu");
        if (jeu == null) {
            return null;
        }
        Choix choix = choix(joueur, jeu);
        return choix == null ? null : new Derniere(jeu, donnees.getLong(joueur + ".derniere.quand"), choix);
    }

    /** Le joueur vient de créer une partie de ce mini-jeu avec ces réglages. */
    public void retenir(UUID joueur, String jeu, String arene, Map<String, Object> options) {
        String base = joueur + ".jeux." + jeu;
        donnees.set(base, null);
        donnees.set(base + ".arene", arene);
        for (Map.Entry<String, Object> option : options.entrySet()) {
            Object valeur = option.getValue();
            // Seulement des valeurs simples (nombres, cases, textes) : le reste n'a pas de sens d'une partie à l'autre.
            if (valeur instanceof Number || valeur instanceof Boolean || valeur instanceof String) {
                donnees.set(base + ".options." + option.getKey(), valeur);
            }
        }
        donnees.set(joueur + ".derniere.jeu", jeu);
        donnees.set(joueur + ".derniere.quand", System.currentTimeMillis());
        modifie = true;
        enregistrer();
    }

    public void enregistrer() {
        if (!modifie) {
            return;
        }
        try {
            plugin.getDataFolder().mkdirs();
            donnees.save(fichier);
            modifie = false;
        } catch (IOException e) {
            plugin.getLogger().warning("reglages.yml non enregistré : " + e.getMessage());
        }
    }
}
