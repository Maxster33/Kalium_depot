package fr.kalium.menu.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 2.4.0 - un plugin de jeu dit a KLM_Menu si un joueur est en partie : « /menu on » et « /menu off » sont alors refuses
 * (demande de LeKiwi06, 28/09/2026 : « durant un mini-jeu : refuser la commande »), pour ne pas toucher a
 * l'inventaire de la partie. Declaration par le registre de services de Paper, comme {@link InterfaceItem}.
 */
public interface PlayerActivity {

    Plugin owner();

    /** Le joueur est-il dans une partie (salle d'attente, jeu, spectateur...) de ce plugin ? */
    boolean inGame(Player player);
}
