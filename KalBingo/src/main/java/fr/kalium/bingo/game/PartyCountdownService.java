package fr.kalium.bingo.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Delai minimum avant qu'une partie en salle d'attente puisse reellement demarrer : 10 secondes
 * * nombre d'equipes (demande explicite de l'utilisateur, 24/09/2026 - "il faut ajouter un délai
 * de: [10 secondes*le nombre d'équipe] entre l'arrivée dans la salle d'attente et le début de la
 * partie"), le temps que la pre-generation en cascade des mondes ait une chance de finir (voir
 * InstanceWorldPreparer, instances.pregeneration-stagger-seconds - meme ordre de grandeur, formule
 * volontairement gardee independante et simple, telle que demandee par l'utilisateur).
 *
 * Si l'hote (ou un operateur via /bingoadmin start - MEME regle que l'hote, demande explicite de
 * l'utilisateur : "respecte le délai comme l'hôte") tente de lancer la partie avant la fin de ce
 * delai, un chronometre s'affiche (action bar, au-dessus de la barre de vie - demande explicite de
 * l'utilisateur) au lieu d'un echec, et la partie demarre automatiquement des que le delai est
 * ecoule (voir PartyStarter.start(), rappele par le Runnable onReady passe a scheduleStart()).
 */
public final class PartyCountdownService {

    private final JavaPlugin plugin;
    private final Map<String, Countdown> active = new HashMap<>();

    public PartyCountdownService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Delai minimum total pour cette partie (10s * nombre d'equipes, voir en-tete de classe). */
    public Duration minDelay(BingoParty party) {
        return Duration.ofSeconds(10L * party.getTeamCount());
    }

    public boolean isReady(BingoParty party) {
        return !Instant.now().isBefore(party.getCreatedAt().plus(minDelay(party)));
    }

    /** Temps restant avant que la partie puisse demarrer (jamais negatif). */
    public Duration remaining(BingoParty party) {
        Duration left = Duration.between(Instant.now(), party.getCreatedAt().plus(minDelay(party)));
        return left.isNegative() ? Duration.ZERO : left;
    }

    /**
     * Programme le demarrage automatique de cette partie des que le delai minimum est ecoule, avec
     * un chronometre affiche entre-temps (action bar, tous les joueurs actuellement connectes de la
     * partie - voir BingoParty.getConnected()). Ne fait rien si un compte a rebours est deja en
     * cours pour ce gameId (evite de le relancer/desynchroniser a chaque nouveau clic sur "Lancer
     * la partie" ou nouvel appel a /bingoadmin start).
     *
     * @return true si un NOUVEAU compte a rebours vient d'etre programme, false si un compte a
     *         rebours etait deja en cours pour cette partie.
     */
    public boolean scheduleStart(BingoParty party, Runnable onReady) {
        String gameId = party.getGameId();
        if (active.containsKey(gameId)) {
            return false;
        }
        Instant readyAt = party.getCreatedAt().plus(minDelay(party));

        BukkitTask broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> broadcast(party, readyAt), 0L, 20L);

        long remainingTicks = Math.max(1L, Duration.between(Instant.now(), readyAt).toMillis() / 50L);
        BukkitTask startTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Countdown countdown = active.remove(gameId);
            if (countdown != null) {
                countdown.broadcastTask().cancel();
            }
            onReady.run();
        }, remainingTicks);

        active.put(gameId, new Countdown(broadcastTask, startTask));
        return true;
    }

    /**
     * Annule un compte a rebours en cours pour ce gameId (partie annulee par l'hote - voir
     * PartyCanceller - ou demarree/nettoyee par un autre chemin), sans effet si aucun compte a
     * rebours n'est en cours.
     */
    public void cancel(String gameId) {
        Countdown countdown = active.remove(gameId);
        if (countdown != null) {
            countdown.broadcastTask().cancel();
            countdown.startTask().cancel();
        }
    }

    private void broadcast(BingoParty party, Instant readyAt) {
        Duration left = Duration.between(Instant.now(), readyAt);
        long seconds = left.isNegative() ? 0L : left.getSeconds() + (left.getNano() > 0 ? 1 : 0);
        Component message = Component.text("Début de la partie dans : ", NamedTextColor.GOLD)
                .append(Component.text(seconds + "s", NamedTextColor.AQUA));
        for (UUID playerId : party.getConnected()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendActionBar(message);
            }
        }
    }

    private record Countdown(BukkitTask broadcastTask, BukkitTask startTask) {}
}
