package fr.kalium.contacts.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 1.2.0 (étape 3 de KLM_Contacts) - un plugin de jeu qui sait faire entrer un joueur dans une de ses parties : sert au
 * groupe de jeu (« le chef entre dans une partie, les membres y entrent avec lui ») et à « Inviter dans ma partie ».
 *
 * Le plugin de jeu :
 * - se déclare au démarrage, s'il trouve KLM_Contacts :
 *   <pre>
 *   getServer().getServicesManager().register(JeuDeGroupe.class, jeu, this, ServicePriority.Normal);
 *   </pre>
 * - annonce chaque joueur qui vient d'entrer dans une partie (créée ou rejointe, privée ou file publique) :
 *   {@code KlmContacts.annoncer(joueur, id(), reference, libellé)}. Si ce joueur est chef d'un groupe, le proxy fait
 *   suivre ses membres ; sinon rien ne se passe.
 *
 * La référence est un texte sans espace, choisi par le jeu, qui lui suffit pour retrouver la partie (ex. « code:AB12 »,
 * « public:boatrace »). Elle lui est rendue telle quelle dans {@link #rejoindre}, sur CE serveur.
 */
public interface JeuDeGroupe {

    /** Plugin qui fournit le jeu. */
    Plugin owner();

    /** Identifiant stable du jeu : lettres minuscules seulement (ex. « kalgames », « bingo », « buildbattle »). */
    String id();

    /**
     * Fait entrer le joueur dans la partie désignée (comme s'il l'avait rejointe par le menu). En cas de refus (partie
     * pleine, terminée...), c'est le jeu qui le dit au joueur.
     */
    void rejoindre(Player joueur, String reference);

    /**
     * Partie privée où se trouve ce joueur et où il peut inviter quelqu'un (« Inviter dans ma partie ») : un tableau
     * {référence, nom affiché du jeu}, ou null s'il n'est dans aucune partie de ce genre.
     */
    default String[] partieDe(Player joueur) {
        return null;
    }
}
