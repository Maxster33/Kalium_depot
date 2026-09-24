package fr.kalium.bingo.game;

import fr.kalium.bingo.network.AssignmentService;
import fr.kalium.bingo.network.PartyStatusNotifier;
import fr.kalium.bingo.world.InstanceWorldPreparer;
import fr.kalium.bingo.world.LobbySlots;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Annule une partie EN ATTENTE (pas encore demarree - voir PartyStarter pour le demarrage) :
 * renvoie tous les joueurs actuellement connectes vers kal-games, libere l'emplacement de
 * salle d'attente et oublie la partie. Declenchee depuis le menu joueur (PartyMenu, bouton
 * "Annuler la partie", reserve a l'hote) - demande explicite de l'utilisateur ("annuler la
 * partie"), aucune commande texte equivalente n'existait avant.
 */
public final class PartyCanceller {

    private final PartyManager partyManager;
    private final LobbySlots lobbySlots;
    private final AssignmentService assignmentService;
    private final InstanceWorldPreparer instanceWorldPreparer;
    private final PartyStatusNotifier partyStatusNotifier;
    private final PartyCountdownService countdownService;

    public PartyCanceller(PartyManager partyManager, LobbySlots lobbySlots, AssignmentService assignmentService,
                           InstanceWorldPreparer instanceWorldPreparer, PartyStatusNotifier partyStatusNotifier,
                           PartyCountdownService countdownService) {
        this.partyManager = partyManager;
        this.lobbySlots = lobbySlots;
        this.assignmentService = assignmentService;
        this.instanceWorldPreparer = instanceWorldPreparer;
        this.partyStatusNotifier = partyStatusNotifier;
        this.countdownService = countdownService;
    }

    /** @return false si aucune partie en attente sous ce gameId. */
    public boolean cancel(String gameId) {
        BingoParty party = partyManager.get(gameId);
        if (party == null) {
            return false;
        }
        Set<UUID> connected = new HashSet<>(party.getConnected());
        // Previent kal-games AVANT de renvoyer les joueurs (voir PartyStatusNotifier) : cette
        // partie ne doit plus apparaitre comme "encore en salle d'attente, a rejoindre" dans son
        // menu - AJOUTE le 24/09/2026. N'importe quel joueur encore connecte ici suffit a
        // acheminer le message.
        connected.stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .findFirst()
                .ifPresentOrElse(carrier -> partyStatusNotifier.notifyClosed(gameId, carrier),
                        () -> partyStatusNotifier.notifyClosed(gameId, null)); // 0.1.19 : le relais suffit

        for (UUID uuid : connected) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage("§7La partie a été annulée par l'hôte, retour à kal-games.");
                assignmentService.sendBackToKalGames(player);
            }
        }
        lobbySlots.release(gameId);
        partyManager.remove(gameId);
        // Annule/supprime les mondes eventuellement deja pre-generes en cascade pour cette partie
        // (voir InstanceWorldPreparer) - ils ne seront jamais reclames, la partie n'aura pas lieu.
        instanceWorldPreparer.cancel(gameId);
        // Arrete un compte a rebours de demarrage automatique eventuellement en cours pour cette
        // partie (voir PartyCountdownService) - sinon il declencherait start() sur une partie qui
        // vient d'etre annulee/nettoyee.
        countdownService.cancel(gameId);
        return true;
    }
}
