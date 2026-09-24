package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Effet de regeneration dans la salle d'attente - AJOUTE en 0.1.19, demande explicite de
 * l'utilisateur : "il faut ajouter un effet de regen dans la salle d'attente".
 *
 * Verifie chaque seconde (voir BingoPlugin) : tout joueur present dans le monde des salles
 * d'attente (lobby.world-name, avant ET apres une partie) recoit Regeneration I sans limite de
 * duree, sans particules ; il la perd des qu'il en sort (lancement de la partie, retour vers
 * kal-games...). Une verification periodique plutot que des evenements d'entree/sortie : couvre
 * sans cas particulier toutes les facons d'arriver dans la salle ou d'en partir.
 */
public final class LobbyRegenTask implements Runnable {

    private static final int AMPLIFIER = 0; // Regeneration I

    private final LobbySlots lobbySlots;

    public LobbyRegenTask(LobbySlots lobbySlots) {
        this.lobbySlots = lobbySlots;
    }

    @Override
    public void run() {
        World lobbyWorld = lobbySlots.world();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean inLobby = lobbyWorld != null && player.getWorld().equals(lobbyWorld);
            PotionEffect current = player.getPotionEffect(PotionEffectType.REGENERATION);
            if (inLobby) {
                if (current == null) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                            PotionEffect.INFINITE_DURATION, AMPLIFIER, true, false, true));
                }
            } else if (current != null && current.isInfinite()) {
                // Seule cette tache donne une regeneration infinie : c'est forcement la notre.
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }
}
