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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Dans un plot : un tableau sur le côté de l'écran (sidebar) avec ses informations - titre, numéro et taille,
 * créateur, éditeurs, état, note, points, note donnée par le joueur - mis à jour toutes les 2 secondes.
 * En entrant dans un plot, sa description est écrite dans le chat. Sur les routes, le tableau disparaît (le tableau
 * d'avant est remis). Demande de LeKiwi06, 26/09/2026 : le tableau remplace le titre au centre de l'écran.
 */
final class EntreePlot implements Listener {

    private static final int EDITEURS_MAX = 3;

    // 1.7.0 (LeKiwi06 : « pour les lignes trop longues comme un thème de build, rend le déroulant de la meme façon que
    // la boss bar dans le lobby ») : un texte écrit par un joueur (thème d'un concours, titre d'un plot) plus long que
    // sa place défile derrière une fenêtre, un caractère tous les 3 ticks, avec un arrêt de 3 secondes à son début
    // (mêmes réglages que le texte défilant de KLM_Hub).
    /** Caractères d'une ligne du tableau ; au-delà, le texte du joueur défile dans la place qui reste. */
    private static final int LARGEUR = 26;
    /** Espaces entre la fin du texte et son retour. */
    private static final int ESPACE = 4;
    private static final int TICKS_PAR_CARACTERE = 3;
    /** Pas (de 3 ticks) pendant lesquels le début du texte reste affiché : 3 secondes. */
    private static final int PAUSE = 20;

    /** Ligne dont le texte défile : numéro de la ligne (-1 : le titre du tableau), libellé fixe, texte, fenêtre. */
    private record Defilant(int ligne, Component libelle, String brut, int largeur) {}

    private final KVPlots plugin;
    /** Dernier plot où se trouvait chaque joueur (0 = aucun). */
    private final Map<UUID, Integer> dernier = new HashMap<>();
    /** Tableau affiché par KV_Plots, et celui que le joueur avait avant (remis en sortant du plot). */
    private final Map<UUID, Scoreboard> tableaux = new HashMap<>(), avant = new HashMap<>();
    /** Lignes qui défilent dans le tableau de chaque joueur, et nombre de lignes de ce tableau. */
    private final Map<UUID, List<Defilant>> defilants = new HashMap<>();
    private final Map<UUID, Integer> nombres = new HashMap<>();
    /** Avance d'un toutes les 3 ticks : position commune de tous les textes qui défilent. */
    private long pas;

    EntreePlot(KVPlots plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player joueur : plugin.getServer().getOnlinePlayers()) {
                if (tableaux.containsKey(joueur.getUniqueId())) verifier(joueur);
            }
        }, 40L, 40L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::defiler, TICKS_PAR_CARACTERE, TICKS_PAR_CARACTERE);
    }

    // ------------------------------------------------------------------ texte qui défile

    private static int longueur(Component texte) {
        return PlainTextComponentSerializer.plainText().serialize(texte).length();
    }

    /**
     * Ce qu'on voit d'un texte à codes couleur « & » : le texte entier s'il tient dans la fenêtre ; sinon les « largeur »
     * caractères à partir de la position du moment, le texte tournant en boucle (avec ses couleurs).
     */
    private Component fenetre(String brut, int largeur) {
        Component entier = texte(brut);
        if (longueur(entier) <= largeur) return entier;
        // Caractères visibles, chacun avec les codes actifs à cet endroit.
        List<Character> lettres = new ArrayList<>();
        List<String> styles = new ArrayList<>();
        String style = "";
        for (int i = 0; i < brut.length(); i++) {
            char c = brut.charAt(i);
            if (c == '&' && i + 1 < brut.length() && "0123456789abcdefklmnor".indexOf(Character.toLowerCase(brut.charAt(i + 1))) >= 0) {
                char code = Character.toLowerCase(brut.charAt(++i));
                style = code == 'r' ? "" : code >= 'k' && code <= 'o' ? style + "&" + code : "&" + code;
                continue;
            }
            lettres.add(c);
            styles.add(style);
        }
        int ruban = lettres.size() + ESPACE;
        // Arrêt au début du texte, puis un tour complet.
        int phase = (int) (pas % (PAUSE + ruban));
        int decalage = phase < PAUSE ? 0 : phase - PAUSE;
        StringBuilder visible = new StringBuilder();
        String courant = null;
        for (int i = 0; i < largeur; i++) {
            int k = (decalage + i) % ruban;
            String s = k < lettres.size() ? styles.get(k) : "";
            if (!s.equals(courant)) {
                visible.append("&r").append(s);
                courant = s;
            }
            visible.append(k < lettres.size() ? lettres.get(k) : ' ');
        }
        return texte(visible.toString());
    }

    /** Ajoute « libellé + texte du joueur » ; si la ligne dépasse LARGEUR, le texte défile dans la place qui reste. */
    private void ajouter(List<Component> l, List<Defilant> liste, Component libelle, String brut) {
        int place = Math.max(8, LARGEUR - longueur(libelle));
        if (longueur(texte(brut)) > place) liste.add(new Defilant(l.size(), libelle, brut, place));
        l.add(libelle.append(fenetre(brut, place)));
    }

    /** Toutes les 3 ticks : les textes qui défilent avancent d'un caractère (seules ces lignes sont réécrites). */
    private void defiler() {
        pas++;
        for (Map.Entry<UUID, List<Defilant>> e : defilants.entrySet()) {
            Scoreboard tableau = tableaux.get(e.getKey());
            Objective o = tableau == null ? null : tableau.getObjective("kvplot");
            if (o == null) continue;
            for (Defilant d : e.getValue()) {
                Component c = d.libelle().append(fenetre(d.brut(), d.largeur()));
                if (d.ligne() < 0) o.displayName(c);
                else o.getScore("l" + d.ligne()).customName(c);
            }
        }
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

    private List<Component> lignes(Player joueur, Plot p, List<Defilant> liste) {
        UUID u = joueur.getUniqueId();
        List<Component> l = new ArrayList<>();
        // 1.7.0 (LeKiwi06 : « reduire tout les sidebar trop long ») : lignes courtes ; un concours en construction et
        // un autre en votes peuvent être affichés ensemble.
        long maintenant = System.currentTimeMillis();
        List<Concours.Un> actifs = plugin.concours().actifs();
        for (Concours.Un c : actifs) {
            if (c.phase == fr.kalium.kvplots.api.KanvasPlots.PhaseConcours.EN_COURS) {
                ajouter(l, liste, Component.text("Concours : ", NamedTextColor.LIGHT_PURPLE), c.theme);
                l.add(ligne("Fin : ", blanc(Concours.dureeCourte(c.fin - maintenant))));
            } else {
                l.add(Component.text("Fin des votes : ", NamedTextColor.LIGHT_PURPLE)
                        .append(blanc(Concours.dureeCourte(c.finVotes - maintenant))));
            }
        }
        if (!actifs.isEmpty()) l.add(Component.empty());
        l.add(Component.text("Plot n°" + p.id + " · " + p.taille.nom, NamedTextColor.GOLD));
        if (p.concours != 0) {
            Concours.Un du = plugin.concours().parId(p.concours);
            l.add(Component.text(du != null && du.actif() ? "Plot du concours" : "Ancien concours", NamedTextColor.LIGHT_PURPLE));
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
        l.add(ligne("État : ", p.chantier != Plot.Chantier.AUCUN ? Component.text("travaux", NamedTextColor.YELLOW)
                : p.etat == Plot.Etat.VALIDE ? Component.text("validé", NamedTextColor.GREEN) : blanc("en travaux")));
        l.add(Component.empty());
        l.add(ligne("Note : ", p.votes.isEmpty() ? blanc("aucune")
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
        List<Defilant> liste = new ArrayList<>();
        List<Component> lignes = lignes(joueur, p, liste);
        Component titre = p.titre.isEmpty() ? Component.text("Plot n°" + p.id, NamedTextColor.GOLD) : fenetre(p.titre, LARGEUR);
        if (!p.titre.isEmpty() && longueur(texte(p.titre)) > LARGEUR) liste.add(new Defilant(-1, Component.empty(), p.titre, LARGEUR));
        // Recréé seulement quand le nombre de lignes change (éditeurs, concours) ; sinon les lignes sont réécrites en
        // place (1.7.0 : un tableau recréé toutes les 2 secondes ferait sauter un texte qui défile).
        Objective o = tableau.getObjective("kvplot");
        Integer nombre = nombres.put(u, lignes.size());
        if (o == null || nombre == null || nombre != lignes.size()) {
            if (o != null) o.unregister();
            o = tableau.registerNewObjective("kvplot", Criteria.DUMMY, titre);
            o.numberFormat(NumberFormat.blank());
            for (int i = 0; i < lignes.size(); i++) o.getScore("l" + i).setScore(lignes.size() - i);
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            o.displayName(titre);
        }
        for (int i = 0; i < lignes.size(); i++) o.getScore("l" + i).customName(lignes.get(i));
        if (liste.isEmpty()) defilants.remove(u);
        else defilants.put(u, liste);
    }

    private void cacher(Player joueur) {
        UUID u = joueur.getUniqueId();
        if (tableaux.remove(u) == null) return;
        defilants.remove(u);
        nombres.remove(u);
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
        defilants.remove(u);
        nombres.remove(u);
    }
}
