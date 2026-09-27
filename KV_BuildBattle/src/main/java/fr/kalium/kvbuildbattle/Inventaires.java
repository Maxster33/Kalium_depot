package fr.kalium.kvbuildbattle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Inventaire Kanvas du joueur mis de côté pendant le Build Battle (étoile de KV_Menu, boussole, constructions en
 * cours...) et rendu quand il quitte la partie. Écrit sur le disque (inventaires/<uuid>.yml) pour être rendu à la
 * connexion suivante si le serveur s'arrête entre-temps. Même principe que le mode vote de KV_Plots.
 */
final class Inventaires {

    private final KVBuildBattle plugin;
    private final File dossier;

    Inventaires(KVBuildBattle plugin) {
        this.plugin = plugin;
        this.dossier = new File(plugin.getDataFolder(), "inventaires");
    }

    private File fichier(UUID joueur) {
        return new File(dossier, joueur + ".yml");
    }

    boolean deCote(UUID joueur) {
        return fichier(joueur).exists();
    }

    /** Met l'inventaire de côté puis le vide ; si un inventaire est déjà de côté, il est gardé (jamais écrasé). */
    void mettreDeCote(Player joueur) {
        File f = fichier(joueur.getUniqueId());
        if (!f.exists()) {
            List<String> contenu = new ArrayList<>();
            for (ItemStack item : joueur.getInventory().getContents()) {
                contenu.add(item == null || item.getType().isAir() ? ""
                        : Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("contenu", contenu);
            try {
                dossier.mkdirs();
                yml.save(f);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Inventaire de " + joueur.getName() + " non enregistré : gardé tel quel", e);
                return;
            }
        }
        joueur.getInventory().clear();
        joueur.setItemOnCursor(null);
    }

    /** Rend l'inventaire mis de côté (s'il y en a un) et efface le fichier. */
    void rendre(Player joueur) {
        File f = fichier(joueur.getUniqueId());
        if (!f.exists()) return;
        PlayerInventory inv = joueur.getInventory();
        List<String> contenu = YamlConfiguration.loadConfiguration(f).getStringList("contenu");
        ItemStack[] items = new ItemStack[inv.getContents().length];
        for (int i = 0; i < items.length && i < contenu.size(); i++) {
            String s = contenu.get(i);
            items[i] = s.isEmpty() ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
        }
        inv.setContents(items);
        joueur.setItemOnCursor(null);
        if (!f.delete()) plugin.getLogger().warning("Impossible d'effacer " + f.getName());
    }
}
