package fr.kalium.observateur;

import fr.kalium.bingo.BingoPlugin;
import fr.kalium.bingo.game.BingoGame;
import fr.kalium.bingo.game.BingoParty;
import fr.kalium.bingo.game.GameState;
import fr.kalium.bingo.game.TeamStyle;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Liste des joueurs du serveur, groupes par partie, puis choix de la vue (libre ou dans ses yeux). */
final class ObservationMenu {

    private final BingoPlugin bingo;
    private final ObservationService service;
    private final Gui gui;
    private final Lang lang;

    ObservationMenu(BingoPlugin bingo, ObservationService service, Gui gui, Lang lang) {
        this.bingo = bingo;
        this.service = service;
        this.gui = gui;
        this.lang = lang;
    }

    /** Ou se trouve un joueur : rang de tri (parties, salles d'attente, ailleurs) et texte affiche. */
    private record Place(int order, String label) {
    }

    private Place placeOf(Player player, List<BingoGame> games) {
        UUID id = player.getUniqueId();
        for (int i = 0; i < games.size(); i++) {
            BingoGame game = games.get(i);
            var instance = game.findInstanceOf(id);
            if (instance.isPresent()) {
                return new Place(i, "Partie " + (i + 1) + " · équipe " + TeamStyle.letter(instance.get().getTeam().getTeamNumber()));
            }
        }
        BingoParty party = bingo.getPartyManager() == null ? null : bingo.getPartyManager().partyOf(id);
        if (party != null) {
            String host = Bukkit.getOfflinePlayer(party.getHost()).getName();
            return new Place(1000, "Salle d'attente de " + (host != null ? host : "?"));
        }
        if (player.getWorld().getName().startsWith("bingo_")) {
            return new Place(1001, "Salle d'attente");
        }
        return new Place(1002, "Hors Bingo");
    }

    void openList(Player op) {
        List<BingoGame> games = new ArrayList<>();
        if (bingo.getGameManager() != null) {
            for (BingoGame game : bingo.getGameManager().getActiveGames()) {
                if (game.getState() == GameState.IN_PROGRESS) {
                    games.add(game);
                }
            }
        }
        games.sort(Comparator.comparing(BingoGame::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        record Entry(Player player, Place place) {
        }
        List<Entry> entries = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(op)) {
                entries.add(new Entry(online, placeOf(online, games)));
            }
        }
        entries.sort(Comparator.comparingInt((Entry e) -> e.place().order())
                .thenComparing(e -> e.place().label())
                .thenComparing(e -> e.player().getName(), String.CASE_INSENSITIVE_ORDER));

        List<Component> body = new ArrayList<>();
        body.add(lang.c("menu.aide", "<gray>Choisissez un joueur, puis la vue : libre ou dans ses yeux."));
        if (service.isObserving(op)) {
            body.add(lang.c("menu.aide-quitter", "<gray>/observer quitter : revenir à votre position."));
        }
        List<ActionButton> buttons = new ArrayList<>();
        for (Entry entry : entries) {
            Player target = entry.player();
            buttons.add(gui.button(
                    lang.c("menu.joueur", "<white><name></white> <gray>· <place>", "name", target.getName(), "place", entry.place().label()),
                    null, p -> openChoice(p, target.getUniqueId())));
        }
        if (buttons.isEmpty()) {
            body.add(lang.c("menu.vide", "<yellow>Aucun autre joueur connecté."));
        }
        gui.open(op, lang.c("menu.titre", "<gold><bold>Observer un joueur"), body, List.of(), buttons, null, 1);
    }

    private void openChoice(Player op, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            op.sendMessage(lang.c("msg.absent", "<red>Ce joueur n'est plus connecté."));
            openList(op);
            return;
        }
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.button(lang.c("menu.libre", "<green>Vue libre"),
                lang.c("menu.libre-aide", "<gray>Téléporté à côté de lui, déplacement libre"),
                p -> observe(p, targetId, false)));
        buttons.add(gui.button(lang.c("menu.yeux", "<aqua>Dans ses yeux"),
                lang.c("menu.yeux-aide", "<gray>Vous voyez ce qu'il voit (Maj pour vous détacher)"),
                p -> observe(p, targetId, true)));
        buttons.add(gui.button(lang.c("menu.retour", "<gray>Retour"), null, this::openList));
        gui.open(op, lang.c("menu.titre-joueur", "<gold><bold>Observer <name>", "name", target.getName()),
                List.of(), List.of(), buttons, null, 1);
    }

    private void observe(Player op, UUID targetId, boolean throughEyes) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            op.sendMessage(lang.c("msg.absent", "<red>Ce joueur n'est plus connecté."));
            return;
        }
        service.observe(op, target, throughEyes);
    }
}
