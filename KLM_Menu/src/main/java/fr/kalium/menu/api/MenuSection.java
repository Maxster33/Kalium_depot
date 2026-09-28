package fr.kalium.menu.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Une interface d'un plugin, affichee dans le catalogue de KLM_Menu (2.0.0) - demande de LeKiwi06, 24/09/2026 :
 * "que KLM_Menu interroge les plugins de chez nous pour savoir s'ils n'ont pas une interface a rajouter [...] chaque
 * plugin gere individuellement son interface mais en etant affiche dans KLM_Menu".
 *
 * Un plugin declare ses interfaces au demarrage par le registre de services de Paper :
 * <pre>
 * getServer().getServicesManager().register(MenuSection.class, section, this, ServicePriority.Normal);
 * </pre>
 * KLM_Menu les recupere une fois le serveur completement demarre (puis a chaque ajout ou retrait) et les range dans
 * le comparateur « Informations » (2.4.0 : Classements, et Parametres pour les admins). Le contenu reste entierement
 * gere par le plugin : KLM_Menu ne fait qu'afficher le bouton et appeler {@link #open}.
 */
public interface MenuSection {

    /** Pour qui est cette interface (les operateurs voient tout). */
    enum Audience {
        /** Tout joueur autorise par {@link #visibleTo}. */
        PLAYERS,
        /** Moderateurs / administrateurs seulement (rangee a part dans le catalogue). */
        ADMINS
    }

    /** Identifiant stable, unique dans le plugin (ex. "hub", "settings", "rankings"). */
    String id();

    /** Plugin qui fournit cette interface (sert a ranger le catalogue). */
    Plugin owner();

    /** Nom du bouton dans le catalogue. */
    Component title();

    /** Une phrase qui explique a quoi sert l'interface (info-bulle du bouton). */
    Component description();

    /** Icone (reservee a un affichage futur en inventaire ; les menus actuels sont des Dialogs). */
    default Material icon() {
        return Material.BOOK;
    }

    default Audience audience() {
        return Audience.PLAYERS;
    }

    /** Ce joueur peut-il voir ce bouton ? (par defaut : oui pour PLAYERS, operateurs pour ADMINS) */
    default boolean visibleTo(Player player) {
        return audience() == Audience.PLAYERS || player.isOp();
    }

    /**
     * Ouvre l'interface pour ce joueur. {@code back} ramene au menu de KLM_Menu qui l'a ouverte (« Informations ») :
     * a utiliser pour le bouton "Retour" du premier ecran (facultatif).
     */
    void open(Player player, Consumer<Player> back);

    /**
     * 2.4.0 - interface de classements : rangee sous « Classements » dans le comparateur « Informations » (joueurs et
     * admins). Les interfaces ADMINS qui ne sont pas des classements vont dans « Parametres » (admins seulement) ; les
     * autres interfaces PLAYERS ne sont plus affichees par KLM_Menu (elles ont leur propre objet, ex. l'etoile).
     */
    default boolean ranking() {
        return false;
    }

    /** 2.4.0 - ordre d'affichage dans « Informations » (plus petit = plus haut), puis ordre alphabetique du plugin. */
    default int order() {
        return 100;
    }

    /**
     * 2.4.0 - boutons a afficher pour cette interface : elle-meme par defaut. Une interface qui regroupe plusieurs
     * reglages (ex. KG_Menu : un par jeu) peut renvoyer une liste, affichee a plat dans « Parametres » (pas de
     * sous-menu inutile).
     */
    default java.util.List<MenuSection> expand(Player player) {
        return java.util.List.of(this);
    }

    /** Raccourci pour declarer une interface sans ecrire de classe. */
    static MenuSection of(Plugin owner, String id, Audience audience, Component title, Component description,
                          java.util.function.BiConsumer<Player, Consumer<Player>> opener) {
        return of(owner, id, audience, false, 100, title, description, opener);
    }

    /** 2.4.0 - raccourci avec le genre (classement ou non) et l'ordre d'affichage. */
    static MenuSection of(Plugin owner, String id, Audience audience, boolean ranking, int order, Component title,
                          Component description, java.util.function.BiConsumer<Player, Consumer<Player>> opener) {
        return new MenuSection() {
            @Override
            public boolean ranking() {
                return ranking;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public Plugin owner() {
                return owner;
            }

            @Override
            public Component title() {
                return title;
            }

            @Override
            public Component description() {
                return description;
            }

            @Override
            public Audience audience() {
                return audience;
            }

            @Override
            public void open(Player player, Consumer<Player> back) {
                opener.accept(player, back);
            }
        };
    }
}
