package fr.kalium.kgmenu.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Ce qu'un plugin de kal-games apporte au menu du serveur (KG_Menu 1.0.0) - demande de LeKiwi06, 24/09/2026 : "quand
 * on cree un jeu et qu'il a donc son interface de prevue, il la transmet / soit detectee au restart par KG_Menu".
 *
 * Declaration, au demarrage du plugin :
 * <pre>
 * getServer().getServicesManager().register(MenuProvider.class, provider, this, ServicePriority.Normal);
 * </pre>
 * KG_Menu decouvre tous les fournisseurs une fois le serveur demarre (puis a chaque ajout ou retrait). Les listes sont
 * demandees a chaque ouverture du menu : un mini-jeu cree en jeu apparait aussitot. Chaque plugin garde entierement
 * la main sur ses interfaces : KG_Menu n'affiche que les boutons.
 */
public interface MenuProvider {

    /** Un bouton : libelle, info-bulle (peut etre null) et ouverture (joueur, action du bouton "Retour"). */
    record Entry(String id, Component label, Component tooltip, BiConsumer<Player, Consumer<Player>> open) {
    }

    /** Plugin qui fournit ces boutons. */
    Plugin owner();

    /** Ordre d'affichage entre les plugins (plus petit = plus haut). */
    default int order() {
        return 100;
    }

    /** Jeux proposes a ce joueur dans l'accueil (liste des jeux). */
    List<Entry> games(Player player);

    /** Autres boutons de l'accueil, apres les jeux (ex. Classements). */
    default List<Entry> extras(Player player) {
        return List.of();
    }

    /** Reglages proposes a ce joueur dans l'accueil "Parametres" (ne renvoyer que ce qu'il a le droit de voir). */
    default List<Entry> settings(Player player) {
        return List.of();
    }

    /**
     * Le joueur est-il en ce moment dans un jeu de ce plugin (partie, spectateur...) ? Si oui, ouvrir le menu de sa
     * partie et renvoyer true : l'objet du hub ouvre alors ce menu plutot que la liste des jeux.
     */
    default boolean openCurrent(Player player) {
        return false;
    }
}
