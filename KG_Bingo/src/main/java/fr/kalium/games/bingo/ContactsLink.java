package fr.kalium.games.bingo;

import fr.kalium.contacts.KlmContacts;
import fr.kalium.contacts.api.JeuDeGroupe;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.function.BiConsumer;

/**
 * 1.10.0 - lien avec KLM_Contacts (etape 3 des contacts, demande de LeKiwi06 du 09/10/2026 : le groupe de jeu entre
 * ensemble dans une partie, y compris quand son chef clique sur « Rejouer »). Chaque joueur envoye vers le serveur Bingo
 * (partie creee, rejointe ou relancee : BingoPartyManager.transferToBingo) est annonce a KLM_Contacts avec le code de sa
 * partie ; s'il est chef d'un groupe, le proxy ramene ses membres sur kal-games et {@link #rejoindre} les fait entrer
 * dans la partie, comme avec le code.
 *
 * Cette classe n'est chargee que si KLM_Contacts est installe (voir KGBingo.onEnable).
 */
final class ContactsLink implements JeuDeGroupe {

    private static final String CODE = "code:";

    private final KGBingo plugin;
    private final KlmContacts contacts;

    private ContactsLink(KGBingo plugin, KlmContacts contacts) {
        this.plugin = plugin;
        this.contacts = contacts;
    }

    /** Declare le Bingo a KLM_Contacts ; renvoie ce qu'il faut appeler pour chaque joueur envoye en partie. */
    static BiConsumer<Player, BingoParty> create(KGBingo plugin, Plugin contacts) {
        ContactsLink link = new ContactsLink(plugin, (KlmContacts) contacts);
        plugin.getServer().getServicesManager().register(JeuDeGroupe.class, link, plugin, ServicePriority.Normal);
        return (player, party) -> link.contacts.annoncer(player, link.id(), CODE + party.code(), "Bingo");
    }

    @Override
    public Plugin owner() {
        return plugin;
    }

    @Override
    public String id() {
        return "bingo";
    }

    @Override
    public void rejoindre(Player player, String reference) {
        plugin.groupJoin(player, reference.startsWith(CODE) ? reference.substring(CODE.length()) : reference);
    }
}
