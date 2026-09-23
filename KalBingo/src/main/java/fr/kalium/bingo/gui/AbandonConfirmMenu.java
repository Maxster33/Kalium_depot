package fr.kalium.bingo.gui;

import fr.kalium.bingo.game.AbandonService;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static fr.kalium.bingo.gui.DialogGui.text;

/**
 * Confirmation avant abandon DEFINITIF d'une partie (voir AbandonService / GameMenu, objet
 * "Abandonner la partie") - demande explicite de l'utilisateur, 24/09/2026 : "il faut ouvrir une
 * fenêtre type menu et demander : êtes-vous sûr de vouloir abandonner ? (attention vous ne pourrez
 * pas revenir). puis le joueur peut répondre oui et quitter la partie. ou non pour revenir en jeu."
 */
public final class AbandonConfirmMenu {

    private final DialogGui gui;
    private final AbandonService abandonService;

    public AbandonConfirmMenu(JavaPlugin plugin, AbandonService abandonService) {
        this.gui = new DialogGui(plugin);
        this.abandonService = abandonService;
    }

    public void open(Player player) {
        List<Component> body = List.of(
                text("<red>Êtes-vous sûr de vouloir abandonner ?"),
                text("<dark_red><bold>Attention, vous ne pourrez pas revenir.</bold>"));
        List<ActionButton> buttons = List.of(
                gui.button(text("<red><bold>Oui, abandonner"), null, abandonService::abandon),
                gui.button(text("<green>Non, revenir en jeu"), null, p -> p.sendMessage("§aVous restez en jeu.")));
        gui.open(player, text("<gold><bold>Abandonner la partie ?"), body, buttons, 2);
    }
}
