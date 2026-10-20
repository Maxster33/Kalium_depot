package fr.kalium.kgmenu.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

    /**
     * Un bouton : libelle, info-bulle (peut etre null) et ouverture (joueur, action du bouton "Retour").
     * 1.1.0 : {@code players} = nombre de joueurs dans ce jeu, affiche sur le bouton par KG_Menu (demande de LeKiwi06,
     * 28/09/2026 : « pour chaque bouton visant a rejoindre un jeu, le plugin doit afficher combien il y a de joueurs
     * dedans ») ; -1 = inconnu (rien d'affiche).
     */
    record Entry(String id, Component label, Component tooltip, BiConsumer<Player, Consumer<Player>> open, int players) {

        /** Sans nombre de joueurs (reglages, boutons divers, plugins compiles avant la 1.1.0). */
        public Entry(String id, Component label, Component tooltip, BiConsumer<Player, Consumer<Player>> open) {
            this(id, label, tooltip, open, -1);
        }
    }

    /**
     * 1.2.0 (demande de LeKiwi06, 20/10/2026 : accueil « Créer une partie / Rejoindre une partie / Parties en cours »)
     * : un jeu qu'on peut créer. {@code creer} ouvre les réglages de la partie (joueur, action « Retour » vers le choix
     * du jeu) ; {@code joueurs} = joueurs dans ce jeu (-1 = inconnu).
     */
    record Jeu(String id, Component nom, ItemStack icone, List<Component> lignes, int joueurs,
               BiConsumer<Player, Consumer<Player>> creer) {
    }

    /**
     * 1.2.0 : une partie listée (tous jeux confondus) : nom du jeu, hôte, joueurs / places (places -1 = sans limite
     * connue), lancée ou en attente. {@code rejoindre} entre dans la partie (lancée : pour la manche suivante) ;
     * {@code regarder} (null = interdit) la regarde en spectateur. Chaque action renvoie null si c'est fait, sinon le
     * motif du refus.
     */
    record Partie(String id, Component jeu, ItemStack icone, String hote, int joueurs, int places, boolean lancee,
                  java.util.function.Function<Player, Component> rejoindre,
                  java.util.function.Function<Player, Component> regarder) {
    }

    /** 1.2.0 : dernière partie créée par ce joueur (« Recréer ma dernière partie ») ; quand = date en millisecondes. */
    record Derniere(Component nom, ItemStack icone, long quand, java.util.function.Function<Player, Component> recreer) {
    }

    /**
     * 1.2.0 : jeux de l'accueil « Créer une partie ». Par défaut, les boutons de {@link #games} (pour un plugin pas
     * encore adapté : son ancien menu s'ouvre).
     */
    default List<Jeu> jeux(Player player) {
        List<Jeu> jeux = new java.util.ArrayList<>();
        for (Entry entry : games(player)) {
            jeux.add(new Jeu(entry.id(), entry.label(), null,
                    entry.tooltip() == null ? List.of() : List.of(entry.tooltip()), entry.players(), entry.open()));
        }
        return jeux;
    }

    /** 1.2.0 : parties de ce plugin à montrer dans « Rejoindre une partie » et « Parties en cours ». */
    default List<Partie> parties(Player player) {
        return List.of();
    }

    /** 1.2.0 : dernière partie créée par ce joueur dans ce plugin, ou null. */
    default Derniere derniere(Player player) {
        return null;
    }

    /**
     * 1.2.0 : « Entrer un code » : null si ce plugin ne connaît pas ce code, sinon le résultat (Component.empty() =
     * le joueur est entré dans la partie, autre texte = motif du refus).
     */
    default Component code(Player player, String code) {
        return null;
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

    /**
     * Reglages proposes a ce joueur (ne renvoyer que ce qu'il a le droit de voir). 1.1.0 : chaque reglage devient un
     * bouton de « Informations &gt; Parametres » (comparateur de KLM_Menu) ; un bouton par jeu de preference.
     */
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
