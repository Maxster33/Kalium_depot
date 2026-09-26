package fr.kalium.kvplots;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

/**
 * En entrant dans un plot : son titre (ou « Plot n°X ») et son créateur à l'écran, sa description dans le chat.
 * Rien sur les routes.
 */
final class EntreePlot implements Listener {

    private final KVPlots plugin;
    /** Dernier plot où se trouvait chaque joueur (0 = aucun). */
    private final Map<UUID, Integer> dernier = new HashMap<>();

    EntreePlot(KVPlots plugin) {
        this.plugin = plugin;
    }

    /** Texte d'un joueur avec les codes couleur « & ». */
    static Component texte(String brut) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(brut == null ? "" : brut);
    }

    private void verifier(Player joueur) {
        if (!joueur.isOnline()) return;
        Plot p = joueur.getWorld().equals(plugin.monde())
                ? plugin.plotEn(joueur.getLocation().getBlockX(), joueur.getLocation().getBlockZ()) : null;
        int id = p == null ? 0 : p.id;
        Integer avant = dernier.put(joueur.getUniqueId(), id);
        if (p == null || (avant != null && avant == id)) return;
        Component titre = p.titre.isEmpty() ? Component.text("Plot n°" + p.id, NamedTextColor.GOLD) : texte(p.titre);
        Component sous = Component.text("par " + KVPlots.nom(p.createur)
                + (p.etat == Plot.Etat.VALIDE ? "" : " (en travaux)"), NamedTextColor.GRAY);
        joueur.showTitle(Title.title(titre, sous,
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2000), Duration.ofMillis(500))));
        if (!p.description.isEmpty()) {
            joueur.sendMessage(Component.text("« ", NamedTextColor.GRAY).append(texte(p.description))
                    .append(Component.text(" »", NamedTextColor.GRAY)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            verifier(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> verifier(e.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> verifier(e.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dernier.remove(e.getPlayer().getUniqueId());
    }
}
