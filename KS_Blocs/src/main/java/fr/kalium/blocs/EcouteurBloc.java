package fr.kalium.blocs;

import org.bukkit.block.Block;

/**
 * Ce qu'un plugin veut savoir de ses blocs custom (tout est appelé sur le fil principal du serveur). Toutes les
 * méthodes sont facultatives.
 */
public interface EcouteurBloc {

    /** Le bloc vient d'être posé (par un joueur ou par {@link KSBlocs#poser}), ou déplacé par un piston (à sa nouvelle place). */
    default void pose(Block bloc, String variante) {
    }

    /**
     * Le bloc custom va disparaître de cette place : cassé, explosé, brûlé, poussé par un piston (appelé à l'ancienne
     * place, puis {@link #pose} à la nouvelle), retiré par {@link KSBlocs#retirer}, ou remplacé sans passer par
     * KS_Blocs (WorldEdit, /setblock...). Le bloc du serveur peut déjà avoir changé.
     */
    default void retire(Block bloc, String variante) {
    }

    /** Un bloc voisin a été posé ou cassé, ou le chunk vient d'être chargé (remettre une lumière, par exemple). */
    default void verifie(Block bloc, String variante) {
    }
}
