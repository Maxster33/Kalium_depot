package fr.kalium.coffremort;

import com.sk89q.worldguard.bukkit.ProtectionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Régions WorldGuard à l'emplacement d'un coffre de mort.
 *
 * Classe chargée seulement si WorldGuard est activé (elle utilise ses classes).
 */
final class ProtectionWorldGuard {

    private ProtectionWorldGuard() {
    }

    /** Vrai si le joueur ne peut ni poser ni casser ce bloc (régions et drapeaux de WorldGuard). */
    static boolean protege(Block bloc, Player joueur) {
        ProtectionQuery question = WorldGuardPlugin.inst().createProtectionQuery();
        return !question.testBlockPlace(joueur, bloc.getLocation(), Material.CHEST)
                && !question.testBlockBreak(joueur, bloc);
    }
}
