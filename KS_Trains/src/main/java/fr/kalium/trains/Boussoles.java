package fr.kalium.trains;

import fr.kalium.menu.api.Contenant;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.3.0 (LeKiwi06 : « faire en sorte que le minecart contrôleur marche comme une lodestone si on clique avec une
 * boussole dessus, pour pouvoir retrouver le train facilement » ; réponse : elle suit le train).
 *
 * Clic droit sur un wagon contrôleur avec une boussole : elle devient « Boussole du train », liée à ce contrôleur
 * (boussole de magnétite qui ne dépend d'aucune magnétite). Toutes les 2 secondes, chaque boussole liée de
 * l'inventaire d'un joueur connecté pointe vers la position actuelle du contrôleur (mise à jour quand il a bougé de
 * plus de 8 blocs, pour ne pas faire sursauter l'objet en main sans cesse) ; contrôleur dans un chunk non chargé : sa
 * dernière position connue, gardée dans `controleurs.yml`.
 */
final class Boussoles {

    private final KSTrains plugin;
    private final NamespacedKey cleLien;
    private final File fichier;
    private final Map<UUID, Location> positions = new HashMap<>();
    private boolean modifie;

    Boussoles(KSTrains plugin) {
        this.plugin = plugin;
        this.cleLien = new NamespacedKey(plugin, "boussole-train");
        this.fichier = new File(plugin.getDataFolder(), "controleurs.yml");
        YamlConfiguration donnees = YamlConfiguration.loadConfiguration(fichier);
        for (String cle : donnees.getKeys(false)) {
            Location lieu = donnees.getLocation(cle);
            if (lieu != null && lieu.getWorld() != null) {
                try {
                    positions.put(UUID.fromString(cle), lieu);
                } catch (IllegalArgumentException ignore) {
                    // clé illisible : oubliée
                }
            }
        }
    }

    /** Clic droit avec une boussole sur un wagon contrôleur : une boussole de la pile devient la boussole du train. */
    void lier(Player joueur, Entity controleur, ItemStack pile) {
        ItemStack boussole = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) boussole.getItemMeta();
        meta.setLodestoneTracked(false);
        meta.setLodestone(controleur.getLocation());
        meta.displayName(plugin.t("boussole.nom", "<white>Boussole du train").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(plugin.t("boussole.ligne", "<gray>Pointe vers le wagon contrôleur")
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(cleLien, PersistentDataType.STRING, controleur.getUniqueId().toString());
        boussole.setItemMeta(meta);
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            pile.setAmount(pile.getAmount() - 1);
        }
        for (ItemStack reste : joueur.getInventory().addItem(boussole).values()) {
            joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
        }
        noter(controleur);
        joueur.playSound(joueur.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1f, 1f);
        Contenant.message(joueur, plugin.t("boussole.ok", "<green>Boussole liée au train"), false);
    }

    /** Retient la position d'un contrôleur chargé. */
    void noter(Entity controleur) {
        Location avant = positions.get(controleur.getUniqueId());
        Location ici = controleur.getLocation();
        if (avant == null || !avant.getWorld().equals(ici.getWorld()) || avant.distanceSquared(ici) > 1) {
            positions.put(controleur.getUniqueId(), ici);
            modifie = true;
        }
    }

    /** Contrôleur cassé : sa position est oubliée (ses boussoles restent sur la dernière). */
    void oublier(UUID controleur) {
        if (positions.remove(controleur) != null) {
            modifie = true;
        }
    }

    /** Toutes les 2 secondes : boussoles des joueurs connectés mises à jour. */
    void mettreAJour() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            PlayerInventory inventaire = joueur.getInventory();
            for (int place = 0; place < inventaire.getSize(); place++) {
                ItemStack objet = inventaire.getItem(place);
                if (objet == null || objet.getType() != Material.COMPASS || !objet.hasItemMeta()) {
                    continue;
                }
                CompassMeta meta = (CompassMeta) objet.getItemMeta();
                String lien = meta.getPersistentDataContainer().get(cleLien, PersistentDataType.STRING);
                if (lien == null) {
                    continue;
                }
                Location cible;
                try {
                    cible = positions.get(UUID.fromString(lien));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                Location actuelle = meta.getLodestone();
                if (cible == null || cible.getWorld() == null || (actuelle != null && actuelle.getWorld() != null
                        && actuelle.getWorld().equals(cible.getWorld()) && actuelle.distanceSquared(cible) < 64)) {
                    continue;
                }
                meta.setLodestone(cible);
                objet.setItemMeta(meta);
                inventaire.setItem(place, objet);
            }
        }
    }

    /** Enregistre les positions connues (à l'arrêt du plugin, et chaque minute si elles ont changé). */
    void enregistrer(boolean force) {
        if (!modifie && !force) {
            return;
        }
        YamlConfiguration donnees = new YamlConfiguration();
        for (Map.Entry<UUID, Location> entree : positions.entrySet()) {
            World monde = entree.getValue().getWorld();
            if (monde != null) {
                donnees.set(entree.getKey().toString(), entree.getValue());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            donnees.save(fichier);
            modifie = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer controleurs.yml : " + e.getMessage());
        }
    }
}
