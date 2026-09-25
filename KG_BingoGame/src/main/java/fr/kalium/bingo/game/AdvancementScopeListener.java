package fr.kalium.bingo.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * 0.7.0 : annonces des succes (advancements) de Minecraft limitees a la partie - demande de LeKiwi06, 25/09/2026, apres
 * un test a 2 parties simultanees : « on voit juste les achievements vanilla des autres parties [...] j'aimerais que ce
 * ne soit pas le cas, mais qu'on voie quand meme les achievements de nos coequipiers et adversaires ».
 *
 * Un joueur EN PARTIE : son annonce n'est envoyee qu'aux joueurs de SA partie (toutes les equipes, coequipiers et
 * adversaires ; pas ceux qui ont abandonne). Un joueur hors partie (salle d'attente, apres une partie) : son annonce
 * n'est envoyee qu'aux joueurs qui ne sont pas en partie. Le succes lui-meme est toujours obtenu normalement.
 */
public final class AdvancementScopeListener implements Listener {

    private final GameManager gameManager;

    public AdvancementScopeListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Component message = event.message();
        if (message == null) {
            return; // succes sans annonce (recettes...)
        }
        event.message(null); // pas d'annonce a tout le serveur : envoyee ci-dessous aux bons joueurs
        Optional<BingoGame> game = gameManager.findGameOf(event.getPlayer().getUniqueId());
        if (game.isPresent()) {
            for (BingoInstance instance : game.get().getInstances()) {
                for (UUID playerId : instance.getTeam().getPlayers()) {
                    if (instance.hasAbandoned(playerId)) {
                        continue;
                    }
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(message);
                    }
                }
            }
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (gameManager.findGameOf(player.getUniqueId()).isEmpty()) {
                player.sendMessage(message);
            }
        }
    }
}
