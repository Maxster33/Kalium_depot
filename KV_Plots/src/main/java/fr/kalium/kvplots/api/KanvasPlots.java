package fr.kalium.kvplots.api;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Accès aux plots de Kanvas pour les autres plugins (KV_Menu...). Publié par KV_Plots dans le registre de services de
 * Paper :
 * <pre>
 * KanvasPlots plots = getServer().getServicesManager().load(KanvasPlots.class);
 * </pre>
 * Les actions vérifient elles-mêmes les droits du joueur et lèvent {@link Refus} avec le message à lui afficher.
 */
public interface KanvasPlots {

    /** Vue d'un plot (copie : ne change pas si le plot change ensuite). */
    record PlotInfo(int id, Taille taille, UUID createur, List<UUID> editeurs, boolean valide, boolean enPreparation) {}

    /** Action refusée ; le message est prêt à être montré au joueur. */
    final class Refus extends Exception {
        public Refus(String message) {
            super(message, null, false, false);
        }
    }

    /** Plots dont le joueur est créateur ou éditeur, triés par numéro. */
    List<PlotInfo> plotsDe(UUID joueur);

    /** Plot à cet endroit (routes intérieures d'un grand plot comprises), ou null. */
    PlotInfo plotEn(Location lieu);

    PlotInfo plot(int id);

    /** Plots de cette taille que le joueur a créés. */
    int occupes(UUID joueur, Taille taille);

    /** Plots de cette taille qu'un joueur peut avoir en même temps. */
    int places(UUID joueur, Taille taille);

    /**
     * Réserve un plot : le plot libre où se tient le joueur, sinon le plus proche du centre ; téléporte le joueur
     * (après la préparation pour un grand plot).
     */
    PlotInfo reserver(Player joueur, Taille taille) throws Refus;

    /** Téléporte le joueur au bord de l'un de ses plots (créateur ou éditeur). */
    void teleporter(Player joueur, int id) throws Refus;

    /** Ajoute un éditeur (seul le créateur du plot peut le faire). */
    void ajouterEditeur(Player createur, int id, UUID editeur) throws Refus;

    /** Retire un éditeur (seul le créateur du plot peut le faire). */
    void retirerEditeur(Player createur, int id, UUID editeur) throws Refus;
}
