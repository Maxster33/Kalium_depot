package fr.kalium.contacts.api;

import fr.kalium.contacts.KlmContacts;

import java.util.UUID;

/**
 * 1.3.0 (demande de LeKiwi06 du 17/10/2026, pour KS_Trains : un train se partage avec les amis de son propriétaire) :
 * liste d'amis lisible par les autres plugins.
 *
 * La liste d'un joueur n'est connue que s'il est connecté sur CE serveur : elle est demandée au proxy à son arrivée,
 * puis chaque minute (un ami ajouté ou retiré compte au plus tard une minute après). Un plugin qui s'en sert déclare
 * {@code softdepend: [KLM_Contacts]} et n'appelle cette classe que si KLM_Contacts est activé.
 */
public final class Amis {

    private Amis() {
    }

    /**
     * Vrai si {@code autre} est dans la liste d'amis de {@code joueur}. {@code joueur} doit être connecté sur ce serveur
     * (sinon : faux). L'amitié étant réciproque, on passe le joueur connecté qui agit.
     */
    public static boolean sontAmis(UUID joueur, UUID autre) {
        KlmContacts plugin = KlmContacts.instance();
        return plugin != null && plugin.ami(joueur, autre);
    }
}
