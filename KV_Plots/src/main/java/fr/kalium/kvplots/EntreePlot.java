package fr.kalium.kvplots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Dans un plot : un tableau sur le côté de l'écran (sidebar) avec ses informations - titre, numéro et taille,
 * créateur, éditeurs, état, note globale, points, note donnée par le joueur - mis à jour toutes les 2 secondes.
 * En entrant dans un plot, sa description est écrite dans le chat. Sur les routes, le tableau disparaît (le tableau
 * d'avant est remis). Demande de LeKiwi06, 26/09/2026 : le tableau remplace le titre au centre de l'écran.
 */
final class EntreePlot implements Listener {

    private static final int EDITEURS_MAX = 3;

    private final KVPlots plugin;
    /** Dernier plot où se trouvait chaque joueur (0 = aucun). */
    private final Map<UUID, Integer> dernier = new HashMap<>();
    /** Tableau affiché par KV_Plots, et celui que le joueur avait avant (remis en sortant du plot). */
    private final Map<UUID, Scoreboard> tableaux = new HashMap<>(), avant = new HashMap<>();

    EntreePlot(KVPlots plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player joueur : plugin.getServer().getOnlinePlayers()) {
                if (tableaux.containsKey(joueur.getUniqueId())) verifier(joueur);
            }
        }, 40L, 40L);
    }

    /** Texte d'un joueur avec les codes couleur « & ». */
    static Component texte(String brut) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(brut == null ? "" : brut);
    }

    private static Component ligne(String libelle, Component valeur) {
        return Component.text(libelle, NamedTextColor.GRAY).append(valeur);
    }

    private static Component blanc(String valeur) {
        return Component.text(valeur, NamedTextColor.WHITE);
    }

    private List<Component> lignes(Player joueur, Plot p) {
        UUID u = joueur.getUniqueId();
        List<Component> l = new ArrayList<>();
        Concours.Un c = plugin.concours().actuel();
        if (c != null) {
            l.add(Component.text("Concours de build : ", NamedTextColor.LIGHT_PURPLE).append(texte(c.theme)));
            long reste = (c.phase == fr.kalium.kvplots.api.KanvasPlots.PhaseConcours.EN_COURS ? c.fin : c.finVotes)
                    - System.currentTimeMillis();
            l.add(Component.text(c.phase == fr.kalium.kvplots.api.KanvasPlots.PhaseConcours.EN_COURS ? "Fin dans : " : "Votes : fin dans ",
                    NamedTextColor.GRAY).append(blanc(Concours.duree(reste))));
        }
        l.add(Component.empty());
        l.add(Component.text("Plot n°" + p.id + " · " + p.taille.nom, NamedTextColor.GOLD));
        if (p.concours != 0) {
            Concours.Un du = plugin.concours().parId(p.concours);
            l.add(Component.text(du != null && du.actif() ? "Plot du concours" : "Ancien concours"
                    + (du == null ? "" : " : " + du.theme), NamedTextColor.LIGHT_PURPLE));
        }
        l.add(ligne("Créateur : ", blanc(KVPlots.nom(p.createur))));
        if (p.editeurs.isEmpty()) {
            l.add(ligne("Éditeurs : ", blanc("aucun")));
        } else {
            l.add(Component.text("Éditeurs :", NamedTextColor.GRAY));
            int n = 0;
            for (UUID e : p.editeurs) {
                if (n++ == EDITEURS_MAX) {
                    l.add(Component.text(" + " + (p.editeurs.size() - EDITEURS_MAX) + " autre(s)", NamedTextColor.WHITE));
                    break;
                }
                l.add(Component.text(" " + KVPlots.nom(e), NamedTextColor.WHITE));
            }
        }
        l.add(ligne("État : ", p.chantier != Plot.Chantier.AUCUN ? Component.text("travaux en cours", NamedTextColor.YELLOW)
                : p.etat == Plot.Etat.VALIDE ? Component.text("validé", NamedTextColor.GREEN) : blanc("en travaux")));
        l.add(Component.empty());
        l.add(ligne("Note globale : ", p.votes.isEmpty() ? blanc("aucune")
                : blanc(String.format(Locale.FRANCE, "%.1f/5", p.moyenne()))));
        l.add(ligne("Votes : ", blanc(String.valueOf(p.votes.size()))));
        l.add(ligne("Points : ", blanc(String.valueOf(p.points()))));
        Plot.Vote v = p.votes.get(u);
        l.add(ligne("Ta note : ", !p.estExterieur(u) ? blanc("ton plot")
                : v != null ? Component.text(v.note() + "/5", NamedTextColor.GOLD) : blanc("pas encore")));
        return l;
    }

    private void afficher(Player joueur, Plot p) {
        UUID u = joueur.getUniqueId();
        Scoreboard tableau = tableaux.get(u);
        if (tableau == null) {
            avant.put(u, joueur.getScoreboard());
            tableau = plugin.getServer().getScoreboardManager().getNewScoreboard();
            tableaux.put(u, tableau);
            joueur.setScoreboard(tableau);
        }
        // Recréé à chaque mise à jour : le nombre de lignes change (éditeurs).
        Objective ancien = tableau.getObjective("kvplot");
        if (ancien != null) ancien.unregister();
        Component titre = p.titre.isEmpty() ? Component.text("Plot n°" + p.id, NamedTextColor.GOLD) : texte(p.titre);
        Objective o = tableau.registerNewObjective("kvplot", Criteria.DUMMY, titre);
        o.numberFormat(NumberFormat.blank());
        List<Component> lignes = lignes(joueur, p);
        for (int i = 0; i < lignes.size(); i++) {
            var score = o.getScore("l" + i);
            score.setScore(lignes.size() - i);
            score.customName(lignes.get(i));
        }
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void cacher(Player joueur) {
        UUID u = joueur.getUniqueId();
        if (tableaux.remove(u) == null) return;
        Scoreboard precedent = avant.remove(u);
        joueur.setScoreboard(precedent != null ? precedent : plugin.getServer().getScoreboardManager().getMainScoreboard());
    }

    private void verifier(Player joueur) {
        if (!joueur.isOnline()) return;
        Plot p = joueur.getWorld().equals(plugin.monde())
                ? plugin.plotEn(joueur.getLocation().getBlockX(), joueur.getLocation().getBlockZ()) : null;
        int id = p == null ? 0 : p.id;
        Integer precedent = dernier.put(joueur.getUniqueId(), id);
        if (p == null) {
            cacher(joueur);
            return;
        }
        afficher(joueur, p);
        if ((precedent == null || precedent != id) && !p.description.isEmpty()) {
            joueur.sendMessage(Component.text("« ", NamedTextColor.GRAY).append(texte(p.description))
                    .append(Component.text(" »", NamedTextColor.GRAY)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            Player joueur = e.getPlayer();
            Plot p = plugin.plotEn(e.getTo().getBlockX(), e.getTo().getBlockZ());
            Integer precedent = dernier.get(joueur.getUniqueId());
            // Sur le même plot, la mise à jour régulière suffit.
            if (precedent == null || precedent != (p == null || !joueur.getWorld().equals(plugin.monde()) ? 0 : p.id)) {
                verifier(joueur);
            }
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
        UUID u = e.getPlayer().getUniqueId();
        dernier.remove(u);
        tableaux.remove(u);
        avant.remove(u);
    }
}
