package fr.kalium.bingo.gui;

import fr.kalium.bingo.game.BingoParty;
import fr.kalium.bingo.game.PartyCanceller;
import fr.kalium.bingo.game.PartyManager;
import fr.kalium.bingo.game.PartyStarter;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static fr.kalium.bingo.gui.DialogGui.text;

/**
 * Menu du joueur dans la salle d'attente (ouvert via l'objet verrouille LobbyItems, clic droit -
 * voir LobbyProtectionListener). Reconstruit en Dialog natif en 0.1.9 (demande explicite de
 * l'utilisateur : "je préfère que la nether star ouvre un menu plutôt qu'une page type
 * inventaire") - remplace l'ancien inventaire Bukkit (chest 9 slots).
 *
 * Assignation des equipes : DESORMAIS reservee a l'hote (0.1.9, demande explicite de
 * l'utilisateur : "le créateur de la partie doit assigner les équipes aux joueurs lui même").
 * Remplace la decision precedente ("choix manuel OU aleatoire, par chaque joueur pour
 * lui-meme") - un joueur normal voit desormais la composition des equipes en LECTURE SEULE, seul
 * l'hote peut cliquer pour faire passer un joueur a l'equipe suivante. /bingoteam a ete adapte en
 * consequence (voir TeamCommand). Validation partagee entre les deux (BingoParty.trySetTeam),
 * aucune logique dupliquee.
 */
public final class PartyMenu {

    private final DialogGui gui;
    private final PartyManager partyManager;
    private final PartyStarter partyStarter;
    private final PartyCanceller partyCanceller;

    public PartyMenu(JavaPlugin plugin, PartyManager partyManager, PartyStarter partyStarter, PartyCanceller partyCanceller) {
        this.gui = new DialogGui(plugin);
        this.partyManager = partyManager;
        this.partyStarter = partyStarter;
        this.partyCanceller = partyCanceller;
    }

    public void open(Player player) {
        BingoParty party = partyManager.partyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cVous n'êtes dans aucune partie Bingo en attente.");
            return;
        }
        boolean host = party.isHost(player.getUniqueId());

        List<Component> body = new ArrayList<>();
        body.add(text("<gray>Hôte : <white>" + nameOf(party.getHost())));
        for (int team = 1; team <= party.getTeamCount(); team++) {
            List<String> names = new ArrayList<>();
            for (UUID member : party.getExpectedRoster()) {
                Integer memberTeam = party.teamOf(member);
                if (memberTeam != null && memberTeam == team) {
                    names.add(nameOf(member));
                }
            }
            body.add(text("<gold>Équipe " + team + " <dark_gray>(" + names.size() + "/" + party.getTeamSize() + ")</dark_gray> "
                    + "<gray>" + (names.isEmpty() ? "-" : String.join(", ", names))));
        }
        Integer myTeam = party.teamOf(player.getUniqueId());
        body.add(text("<gray>Votre équipe : <white>" + (myTeam == null ? "aucune" : String.valueOf(myTeam))));
        if (!host) {
            body.add(text("<dark_gray><i>Seul l'hôte peut assigner les équipes.</i>"));
        }

        List<ActionButton> buttons = new ArrayList<>();
        if (host) {
            for (UUID member : party.getConnected()) {
                Integer current = party.teamOf(member);
                Component label = text("<aqua>" + nameOf(member) + "</aqua> <dark_gray>→</dark_gray> <gray>équipe suivante "
                        + "(actuel : " + (current == null ? "aucune" : current) + ")");
                buttons.add(gui.button(label, null, p -> assignNext(p, party, member)));
            }
            buttons.add(gui.button(text("<green><bold>Lancer la partie"), null, p -> launch(p, party)));
            buttons.add(gui.button(text("<red><bold>Annuler la partie"), null, p -> partyCanceller.cancel(party.getGameId())));
        }

        gui.open(player, text("<gold><bold>Menu de la partie Bingo"), body, buttons, 1);
    }

    /** Fait passer `target` a la prochaine equipe non complete (host uniquement, voir open()). */
    private void assignNext(Player host, BingoParty party, UUID target) {
        int current = party.teamOf(target) == null ? 0 : party.teamOf(target);
        int teamCount = party.getTeamCount();
        for (int i = 1; i <= teamCount; i++) {
            int candidate = (current + i - 1) % teamCount + 1;
            BingoParty.TeamJoinResult result = party.trySetTeam(target, candidate);
            if (result == BingoParty.TeamJoinResult.OK) {
                host.sendMessage("§a" + nameOf(target) + " → équipe " + candidate + ".");
                open(host);
                return;
            }
        }
        host.sendMessage("§cToutes les équipes sont complètes.");
        open(host);
    }

    private void launch(Player host, BingoParty party) {
        PartyStarter.Result result = partyStarter.start(party.getGameId());
        if (result.scheduled()) {
            // Delai minimum pas encore ecoule (voir PartyCountdownService) : pas un echec, la
            // partie demarrera automatiquement - le chronometre s'affiche en action bar entre-temps.
            host.sendMessage(result.message());
        } else if (result.ok()) {
            host.sendMessage("§aPartie démarrée (" + result.game().getInstances().size() + " équipe(s)).");
        } else {
            host.sendMessage("§c" + (result.message() != null ? result.message() : result.failure().name()));
        }
    }

    private String nameOf(UUID uuid) {
        if (uuid == null) {
            return "?";
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name == null ? "?" : name;
    }
}
