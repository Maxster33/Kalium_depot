package fr.kalium.supertool;

import fr.kalium.cultures.KSCultures;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Cultures de KS_Cultures (tomate, maïs, riz, kiwi...) pour la Laboureuse : API de KS_Cultures 1.1.0.
 *
 * Classe chargée seulement si KS_Cultures est activé (elle utilise ses classes).
 */
final class Cultures {

    private Cultures() {
    }

    static boolean mure(Block bloc) {
        return KSCultures.cultureMure(bloc);
    }

    static boolean recolter(Player joueur, Block bloc) {
        return KSCultures.recolter(joueur, bloc);
    }
}
