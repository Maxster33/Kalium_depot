package fr.kalium.contrats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrat en cours d'écriture (« Créer un contrat », « Modifier », « Proposer une modification »). Gardé quand le
 * joueur ferme le menu, jusqu'à l'envoi ou au redémarrage du serveur.
 */
final class Brouillon {

    /** Modification d'un contrat actif ou contre-proposition : son identifiant ; null : nouveau contrat. */
    String contrat;
    /** Contre-proposition d'une demande reçue (sinon : modification d'un contrat actif). */
    boolean contre;
    /** Partie de l'auteur : A pour un nouveau contrat. */
    boolean moiA = true;
    List<UUID> equipe = new ArrayList<>();
    List<UUID> autres = new ArrayList<>();
    Map<UUID, String> noms = new LinkedHashMap<>();
    Contrat.Termes termes = new Contrat.Termes();
    /** Le type (bien ou service) de chaque engagement a-t-il été choisi ? */
    boolean typeA;
    boolean typeB;

    Contrat.Engagement mien() {
        return termes.de(moiA);
    }

    Contrat.Engagement leur() {
        return termes.de(!moiA);
    }

    /** Brouillon pré-rempli depuis un contrat (participants fixés). */
    static Brouillon depuis(Contrat c, Contrat.Termes termes, UUID compte, boolean contre) {
        Brouillon b = new Brouillon();
        b.contrat = c.id;
        b.contre = contre;
        b.moiA = c.dansA(compte);
        b.termes = termes.copie();
        if (c.etat == Contrat.Etat.ACTIF && c.echeance > 0) {
            // L'expiration se compte à l'acceptation : le brouillon reprend le temps qui reste.
            b.termes.duree = Math.max(Contrat.MINUTE, c.echeance - System.currentTimeMillis());
        }
        b.typeA = true;
        b.typeB = true;
        b.noms.putAll(c.noms);
        return b;
    }
}
