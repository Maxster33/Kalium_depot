package fr.kalium.bingo.gui;

import fr.kalium.bingo.game.GameEndService;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static fr.kalium.bingo.gui.DialogGui.text;

/**
 * Menu ouvert par le clic sur la nether star de la salle d'attente POST-partie (apres victoire ou
 * temps ecoule, voir GameEndService.isLingering) - demande explicite de l'utilisateur, 23/09/2026 :
 * "on leur redonne une netherstar pour qu'ils puissent retourner au hub kalgames (via un menu)".
 * Remplace le depart immediat au clic utilise jusqu'ici (voir LobbyProtectionListener), qui
 * renvoyait directement sans confirmation.
 *
 * Volontairement minimal (un seul bouton) - meme principe que GameMenu/PartyMenu : pas de contenu
 * non demande (classement final, statistiques...), le cahier des charges ne demande qu'un retour
 * au hub via menu.
 */
public final class PostGameMenu {

    private final DialogGui gui;
    private final GameEndService gameEndService;

    public PostGameMenu(JavaPlugin plugin, GameEndService gameEndService) {
        this.gui = new DialogGui(plugin);
        this.gameEndService = gameEndService;
    }

    public void open(Player player) {
        List<Component> body = List.of(text("<gray>La partie est terminée. Vous pouvez repartir quand vous le souhaitez."));
        List<ActionButton> buttons = List.of(
                gui.button(text("<green><bold>Retour à kal-games"), null, gameEndService::leaveVoluntarily));
        gui.open(player, text("<gold><bold>Partie terminée"), body, buttons, 1);
    }
}
