package fr.kalium.games.game;

import fr.kalium.contacts.KlmContacts;
import fr.kalium.contacts.api.JeuDeGroupe;
import fr.kalium.games.KalGames;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.function.BiConsumer;

/**
 * 1.25.0 - lien avec KLM_Contacts (étape 3 des contacts, demande de LeKiwi06 du 09/10/2026 : le groupe de jeu entre
 * ensemble dans une partie, « Inviter dans ma partie », le groupe suit son chef quand il clique sur « Rejouer »).
 *
 * - Chaque entree d'un joueur dans une partie (creee ou rejointe, privee ou file publique : InstanceManager.enter) est
 *   annoncee a KLM_Contacts : si ce joueur est chef d'un groupe, le proxy y fait entrer ses membres.
 * - KLM_Contacts rend la reference de la partie (« code:AB12 », « public:&lt;mini-jeu&gt; », « public:&lt;mini-jeu&gt;@&lt;arene&gt; »
 *   pour le Rush) et {@link #rejoindre} y fait entrer le joueur, comme par le menu.
 *
 * Cette classe n'est chargee que si KLM_Contacts est installe (voir KalGames.onEnable) : sans lui, rien ne change.
 */
public final class ContactsLink implements JeuDeGroupe {

    private static final String CODE = "code:";
    private static final String PUBLIC = "public:";

    private final KalGames plugin;
    private final KlmContacts contacts;

    private ContactsLink(KalGames plugin, KlmContacts contacts) {
        this.plugin = plugin;
        this.contacts = contacts;
    }

    /** Declare les jeux du moteur a KLM_Contacts ; renvoie ce qu'il faut appeler a chaque entree en partie. */
    public static BiConsumer<Player, GameInstance> create(KalGames plugin, Plugin contacts) {
        ContactsLink link = new ContactsLink(plugin, (KlmContacts) contacts);
        plugin.getServer().getServicesManager().register(JeuDeGroupe.class, link, plugin, ServicePriority.Normal);
        return link::entered;
    }

    @Override
    public Plugin owner() {
        return plugin;
    }

    @Override
    public String id() {
        return "kalgames";
    }

    private String label(Minigame minigame) {
        return PlainTextComponentSerializer.plainText().serialize(plugin.lang().parse(minigame.display()));
    }

    private String reference(GameInstance instance) {
        if (!instance.isPublic()) {
            return CODE + instance.code();
        }
        // Rush : une partie publique par arene.
        return PUBLIC + instance.minigame().id()
                + (instance.minigame().type() == fr.kalium.games.model.MinigameType.RUSH ? "@" + instance.arena().id() : "");
    }

    private void entered(Player player, GameInstance instance) {
        if (!instance.isPublic() && instance.code().isEmpty()) {
            return;
        }
        contacts.annoncer(player, id(), reference(instance), label(instance.minigame()));
    }

    @Override
    public void rejoindre(Player player, String reference) {
        InstanceManager manager = plugin.instances();
        Component error;
        if (reference.startsWith(CODE)) {
            error = manager.joinPrivate(player, reference.substring(CODE.length()));
        } else if (reference.startsWith(PUBLIC)) {
            String rest = reference.substring(PUBLIC.length());
            int at = rest.indexOf('@');
            Minigame minigame = plugin.repository().minigame(at < 0 ? rest : rest.substring(0, at));
            if (minigame == null) {
                error = plugin.t("game.unavailable", "<red>Ce mini-jeu n'est pas disponible.");
            } else {
                Arena arena = null;
                if (at >= 0) {
                    for (Arena candidate : manager.usableArenas(minigame)) {
                        if (candidate.id().equals(rest.substring(at + 1))) {
                            arena = candidate;
                        }
                    }
                }
                error = arena != null ? manager.joinPublicArena(player, minigame, arena) : manager.joinPublic(player, minigame);
            }
        } else {
            error = plugin.t("join.unknown-code", "<red>Aucune partie ne correspond à ce code.");
        }
        if (error != null) {
            player.sendMessage(plugin.prefix().append(error));
        }
    }

    @Override
    public String[] partieDe(Player player) {
        GameInstance instance = plugin.instances().of(player);
        if (instance == null || instance.isPublic() || instance.closing() || instance.code().isEmpty()) {
            return null;
        }
        return new String[]{CODE + instance.code(), label(instance.minigame())};
    }
}
