package fr.kalium.core.stats;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Une section de l'ecran Statistiques.
 * <p>
 * {@code moduleId() == null} : section de base, toujours affichee (temps de jeu, niveaux depenses,
 * blocs casses/poses, monstres tues). {@code moduleId() != null} : section rattachee a un module
 * optionnel (voir ModuleRegistry) ; elle disparait automatiquement des que ce module est desactive
 * depuis l'ecran Parametres.
 */
public interface StatSection {

    String moduleId();

    List<Component> render(PlayerStats stats);
}
