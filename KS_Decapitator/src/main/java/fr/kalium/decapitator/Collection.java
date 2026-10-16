package fr.kalium.decapitator;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.2.0 (LeKiwi06, 11/10/2026) : têtes déjà obtenues par chaque joueur, montrées dans /tetes. Une tête compte dès
 * qu'elle a été dans l'inventaire du joueur, quelle que soit sa provenance (mob tué, échange, coffre, don) : ramassée
 * au sol, ou vue dans son inventaire (contrôle toutes les 5 secondes, à la connexion, à la déconnexion et à
 * l'ouverture de /tetes). Données : plugins/KS_Decapitator/collection.yml.
 * 1.3.0 : les têtes de joueurs aussi (id « joueur:<uuid> », voir TetesDeJoueurs), quand elles sont actives.
 */
final class Collection implements Listener {

    /** Ticks entre deux contrôles des inventaires. */
    private static final long PERIODE = 100L;

    private final KSDecapitator plugin;
    private final File fichier;
    private final Map<UUID, Set<String>> obtenues = new HashMap<>();
    private boolean aSauver;

    Collection(KSDecapitator plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "collection.yml");
        charger();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::controler, PERIODE, PERIODE);
    }

    /** Ids des têtes que ce joueur a déjà eues. */
    Set<String> de(UUID joueur) {
        return obtenues.getOrDefault(joueur, Set.of());
    }

    /** Note les têtes de l'inventaire du joueur (armure et main secondaire comprises). */
    void regarder(Player joueur) {
        for (ItemStack objet : joueur.getInventory().getContents()) {
            noter(joueur, objet);
        }
    }

    private void noter(Player joueur, ItemStack objet) {
        String id = KSDecapitator.idTete(objet);
        if (id == null && KSDecapitator.tetesDeJoueurs() != null) {
            id = TetesDeJoueurs.idTete(objet);
        }
        if (id != null && obtenues.computeIfAbsent(joueur.getUniqueId(), u -> new LinkedHashSet<>()).add(id)) {
            aSauver = true;
        }
    }

    private void controler() {
        plugin.getServer().getOnlinePlayers().forEach(this::regarder);
        if (aSauver) {
            sauver();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRamasser(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player joueur) {
            noter(joueur, event.getItem().getItemStack());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        regarder(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        regarder(event.getPlayer());
    }

    private void charger() {
        ConfigurationSection joueurs = YamlConfiguration.loadConfiguration(fichier).getConfigurationSection("joueurs");
        if (joueurs == null) {
            return;
        }
        for (String cle : joueurs.getKeys(false)) {
            try {
                obtenues.put(UUID.fromString(cle), new LinkedHashSet<>(joueurs.getStringList(cle)));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("collection.yml : joueur ignoré (" + cle + ").");
            }
        }
    }

    void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        obtenues.forEach((joueur, ids) -> yaml.set("joueurs." + joueur, new ArrayList<>(ids)));
        try {
            yaml.save(fichier);
            aSauver = false;
        } catch (IOException e) {
            plugin.getLogger().warning("collection.yml non enregistré : " + e.getMessage());
        }
    }
}
