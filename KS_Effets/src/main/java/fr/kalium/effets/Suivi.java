package fr.kalium.effets;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Suivi du temps restant d'un effet, choisi dans /effects : un effet en barre de boss, un effet en barre d'action
 * (mis à jour chaque seconde). Quand l'effet suivi se termine, son suivi s'arrête. La barre d'action est partagée avec
 * les messages courts des autres plugins : un tel message est remplacé par le suivi à la seconde suivante.
 */
final class Suivi {

    private final KSEffets plugin;
    private final Map<UUID, BossBar> barres = new HashMap<>();
    /** Effet affiché dans la barre de boss du joueur et plus grand temps vu (durée totale des effets du jeu inconnue). */
    private final Map<UUID, String> cles = new HashMap<>();
    private final Map<UUID, Integer> maxVus = new HashMap<>();

    Suivi(KSEffets plugin) {
        this.plugin = plugin;
    }

    /** Met à jour les barres du joueur (c : son compte, peut être null). */
    void maj(Player joueur, Registre.Compte c) {
        if (c == null) {
            cacher(joueur);
            return;
        }
        if (c.suiviBoss != null) {
            KSEffets.Entree e = plugin.entree(joueur, c.suiviBoss);
            if (e == null) {
                c.suiviBoss = null;
                plugin.registre().modifie();
            } else {
                afficherBoss(joueur, e);
            }
        }
        if (c.suiviBoss == null) {
            cacher(joueur);
        }
        if (c.suiviAction != null) {
            KSEffets.Entree e = plugin.entree(joueur, c.suiviAction);
            if (e == null) {
                c.suiviAction = null;
                plugin.registre().modifie();
            } else {
                joueur.sendActionBar(texte(e));
            }
        }
    }

    private void afficherBoss(Player joueur, KSEffets.Entree e) {
        UUID id = joueur.getUniqueId();
        if (!e.cle().equals(cles.put(id, e.cle()))) {
            maxVus.remove(id);
        }
        int max = Math.max(Math.max(plugin.total(joueur, e.cle()), e.restant()), maxVus.getOrDefault(id, 0));
        maxVus.put(id, max);
        float progression = e.restant() < 0 || max <= 0 ? 1f : Math.min(1f, Math.max(0f, e.restant() / (float) max));
        BossBar barre = barres.get(id);
        if (barre == null) {
            barre = BossBar.bossBar(texte(e), progression, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            barres.put(id, barre);
            joueur.showBossBar(barre);
        } else {
            barre.name(texte(e));
            barre.progress(progression);
        }
    }

    private Component texte(KSEffets.Entree e) {
        return plugin.t("barre.texte", "<nom><gray> : <white><temps>", "nom", e.nom(), "temps", plugin.temps(e.restant()));
    }

    /** Cache la barre de boss du joueur. */
    void cacher(Player joueur) {
        UUID id = joueur.getUniqueId();
        BossBar barre = barres.remove(id);
        if (barre != null) {
            joueur.hideBossBar(barre);
        }
        cles.remove(id);
        maxVus.remove(id);
    }

    /** À l'arrêt du plugin : cache toutes les barres de boss. */
    void toutCacher() {
        for (UUID id : barres.keySet().toArray(new UUID[0])) {
            Player joueur = Bukkit.getPlayer(id);
            if (joueur != null) {
                cacher(joueur);
            }
        }
        barres.clear();
    }
}
