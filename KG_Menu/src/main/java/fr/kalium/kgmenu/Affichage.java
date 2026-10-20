package fr.kalium.kgmenu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * 1.2.0 - réglages d'affichage de chaque joueur (demande de LeKiwi06, 20/10/2026 : « faire une option pour switch du
 * /dialog au /coffre dans l'interface, paramètre, affichage »). Rangés dans plugins/KG_Menu/affichage.yml, gardés d'une
 * connexion à l'autre ; un joueur absent du fichier a les valeurs par défaut (menus en coffres).
 */
final class Affichage {

    private final KgMenu plugin;
    private final File fichier;
    private final YamlConfiguration donnees;
    private boolean modifie;

    Affichage(KgMenu plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "affichage.yml");
        this.donnees = YamlConfiguration.loadConfiguration(fichier);
    }

    /** Menus en coffres (true, par défaut) ou en fenêtres (false). */
    boolean coffres(Player joueur) {
        return donnees.getBoolean(joueur.getUniqueId() + ".coffres", plugin.getConfig().getBoolean("affichage.coffres-par-defaut", true));
    }

    void coffres(Player joueur, boolean valeur) {
        donnees.set(joueur.getUniqueId() + ".coffres", valeur);
        modifie = true;
        enregistrer();
    }

    /** Enregistre le fichier s'il a changé (appelé après chaque changement et à l'arrêt). */
    void enregistrer() {
        if (!modifie) {
            return;
        }
        try {
            plugin.getDataFolder().mkdirs();
            donnees.save(fichier);
            modifie = false;
        } catch (IOException e) {
            plugin.getLogger().warning("affichage.yml non enregistré : " + e.getMessage());
        }
    }
}
