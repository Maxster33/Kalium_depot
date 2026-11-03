package fr.kalium.didacticiel;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Deploiement en deux temps (KS_MondeTest 1.2.0, demande de LeKiwi06 du 03/11/2026) : une nouveaute ne marche d'abord
 * que dans le monde de test, et en survie une fois validee (/mondetest valider <cle>). Le nom est une cle
 * « Plugin:nouveaute » (une cle jamais vue est mise en test par KS_MondeTest) ou un nom de plugin.
 * Sans KS_MondeTest : ferme (rien de non valide ne doit s'ouvrir par accident).
 * Meme classe dans chaque plugin concerne (aucune dependance entre plugins : appel par reflexion).
 */
final class PhaseTest {

    private static volatile Plugin source;
    private static volatile Method methode;

    private PhaseTest() {
    }

    /** La nouveaute est-elle ouverte dans ce monde ? */
    static boolean ouvert(String cle, World monde) {
        Plugin p = Bukkit.getPluginManager().getPlugin("KS_MondeTest");
        if (p == null || !p.isEnabled()) {
            return false;
        }
        try {
            Method m = methode;
            if (m == null || source != p) {
                m = p.getClass().getMethod("phaseOuverte", String.class, World.class);
                methode = m;
                source = p;
            }
            return (Boolean) m.invoke(p, cle, monde);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** La nouveaute est-elle ouverte la ou se trouve ce joueur ? */
    static boolean ouvert(String cle, Player joueur) {
        return joueur != null && ouvert(cle, joueur.getWorld());
    }
}
