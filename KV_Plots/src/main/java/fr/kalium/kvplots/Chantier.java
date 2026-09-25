package fr.kalium.kvplots;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.logging.Level;

import org.bukkit.scheduler.BukkitTask;

/**
 * Travaux sur le terrain, étalés sur plusieurs ticks pour ne pas bloquer le serveur : au plus « blocs-par-tick »
 * blocs examinés par tick. Les tâches passent l'une après l'autre.
 */
final class Chantier {

    /** Une tâche : des colonnes (x, z) à poser avec le modèle de sol, puis une action de fin. */
    record Tache(Iterator<int[]> colonnes, Runnable fin) {}

    private final KVPlots plugin;
    private final Deque<Tache> taches = new ArrayDeque<>();
    private BukkitTask boucle;

    Chantier(KVPlots plugin) {
        this.plugin = plugin;
    }

    void ajouter(Tache tache) {
        taches.add(tache);
        if (boucle == null) boucle = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        ModeleSol modele = plugin.modeleSol();
        int budget = plugin.getConfig().getInt("blocs-par-tick", 20000);
        while (budget > 0 && !taches.isEmpty()) {
            Tache t = taches.peek();
            if (!t.colonnes().hasNext()) {
                taches.poll();
                try {
                    t.fin().run();
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.SEVERE, "Erreur en fin de travaux", e);
                }
                continue;
            }
            int[] xz = t.colonnes().next();
            modele.poser(plugin.monde(), xz[0], xz[1]);
            budget -= modele.hauteur();
        }
        if (taches.isEmpty()) {
            boucle.cancel();
            boucle = null;
        }
    }
}
