package fr.kalium.trains;

import java.util.UUID;

/**
 * 1.4.0 : amis de KLM_Contacts (1.3.0 et plus). Classe chargée seulement si KLM_Contacts est activé (elle utilise ses
 * classes).
 */
final class AmisContacts {

    private AmisContacts() {
    }

    static boolean amis(UUID joueur, UUID autre) {
        return fr.kalium.contacts.api.Amis.sontAmis(joueur, autre);
    }
}
