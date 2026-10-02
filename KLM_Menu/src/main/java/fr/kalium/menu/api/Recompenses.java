package fr.kalium.menu.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 2.6.0 (catégorie 4 « Récompenses », LeKiwi06) : interface Récompenses du serveur (KG_Rewards sur kal-games,
 * KS_RewardsGUI sur Event, KV_Rewards sur Kanvas...). Le comparateur « Informations » affiche un bouton « Récompenses »
 * qui l'ouvre : progression des joueurs, configuration pour les admins. Seul le plugin du serveur où l'on est compte.
 *
 * Déclaration, au démarrage du plugin :
 * <pre>
 * getServer().getServicesManager().register(Recompenses.class, recompenses, this, ServicePriority.Normal);
 * </pre>
 */
public interface Recompenses {

    /** Plugin qui fournit l'interface. */
    Plugin owner();

    /** Ouvre l'interface Récompenses pour ce joueur. */
    void ouvrir(Player player);
}
