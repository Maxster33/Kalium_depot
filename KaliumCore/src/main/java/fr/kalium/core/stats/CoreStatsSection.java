package fr.kalium.core.stats;

import fr.kalium.core.KaliumCore;
import fr.kalium.core.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;

/**
 * Statistiques de base demandees pour le menu : temps de jeu, niveaux depenses, blocs casses,
 * blocs poses, monstres tues. Non rattachees a un module optionnel : toujours affichees.
 */
public final class CoreStatsSection implements StatSection {

    private final KaliumCore plugin;

    public CoreStatsSection(KaliumCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String moduleId() {
        // Rattachee au module "stats" : l'enregistrement continue toujours en arriere-plan
        // (StatsListener n'est pas concerne par ce gate), seul l'affichage est masque si le module
        // "Statistiques" est desactive depuis les Parametres.
        return "stats";
    }

    @Override
    public List<Component> render(PlayerStats stats) {
        return List.of(
                line("stats-playtime", Format.duration(stats.playtimeSeconds())),
                line("stats-levels-spent", Format.number(stats.levelsSpent())),
                line("stats-blocks-broken", Format.number(stats.blocksBroken())),
                line("stats-blocks-placed", Format.number(stats.blocksPlaced())),
                line("stats-monsters-killed", Format.number(stats.monstersKilled())));
    }

    private Component line(String key, String value) {
        return plugin.mm().deserialize(plugin.msg(key), Placeholder.unparsed("value", value));
    }
}
