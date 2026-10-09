package fr.kalium.relay;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 1.6.0 - partie proxy de KLM_Contacts, etape 1 « amis » (demande de LeKiwi06, 09/10/2026 : « un KLM_Contacts pour
 * ajouter nos amis [...] voir dans quels serveurs de KaLium ils sont »). Le proxy garde les donnees ({@link
 * ContactsStore}), sait ou se trouve chaque joueur, deplace les joueurs et livre lui-meme les messages et les
 * notifications (sans delai, sans dependre d'un joueur connecte sur le serveur d'arrivee).
 *
 * - Requetes de KLM_Contacts (menus et commande /amis des serveurs Paper) : POST /contacts/&lt;action&gt;, voir
 *   {@link #handle}.
 * - Commandes du proxy : /mp &lt;pseudo&gt; &lt;message&gt; et /r &lt;message&gt;. Elles passent par le proxy pour que
 *   LibertyBans puisse les refuser a un joueur rendu muet (les ajouter a ses commandes bloquees) ; le champ « Envoyer
 *   un message » du menu rejoue la commande /mp au nom du joueur, donc suit la meme regle.
 * - Messages prives ecrits dans le journal du proxy (moderation, accord de LeKiwi06 du 09/10/2026).
 */
final class Contacts {

    private static final int MAX_MESSAGE = 256;
    private static final int MAX_PENDING = 30;

    private final Object plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final RelayConfig config;
    private final ContactsStore store;
    /** Dernier correspondant de chaque joueur (/r), en memoire. */
    private final Map<UUID, UUID> lastPartner = new HashMap<>();
    /** Joueurs dont la connexion a ete annoncee a leurs amis (pour n'annoncer que leur deconnexion a eux). */
    private final Set<UUID> announced = new HashSet<>();

    Contacts(Object plugin, ProxyServer server, Logger logger, RelayConfig config, Path dataDirectory) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.store = new ContactsStore(dataDirectory, logger);
    }

    void register() {
        server.getEventManager().register(plugin, this);
        server.getCommandManager().register(server.getCommandManager().metaBuilder("mp").plugin(plugin).build(),
                new SimpleCommand() {
                    @Override
                    public void execute(Invocation invocation) {
                        String[] args = invocation.arguments();
                        if (!(invocation.source() instanceof Player player)) {
                            return;
                        }
                        if (args.length < 2) {
                            player.sendMessage(error("Utilisation : /mp <pseudo> <message>"));
                            return;
                        }
                        message(player, args[0], String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
                    }

                    @Override
                    public List<String> suggest(Invocation invocation) {
                        String[] args = invocation.arguments();
                        if (args.length > 1 || !(invocation.source() instanceof Player player)) {
                            return List.of();
                        }
                        return onlineFriendNames(player.getUniqueId(), args.length == 0 ? "" : args[0]);
                    }
                });
        server.getCommandManager().register(server.getCommandManager().metaBuilder("r").plugin(plugin).build(),
                (SimpleCommand) invocation -> {
                    if (!(invocation.source() instanceof Player player)) {
                        return;
                    }
                    if (invocation.arguments().length == 0) {
                        player.sendMessage(error("Utilisation : /r <message>"));
                        return;
                    }
                    UUID partner;
                    synchronized (this) {
                        partner = lastPartner.get(player.getUniqueId());
                    }
                    Optional<Player> target = partner == null ? Optional.empty() : server.getPlayer(partner);
                    if (target.isEmpty()) {
                        player.sendMessage(error("Personne à qui répondre : ton correspondant n'est plus en ligne."));
                        return;
                    }
                    message(player, target.get().getUsername(), String.join(" ", invocation.arguments()));
                });
    }

    // ------------------------------------------------------------------ textes

    private static Component prefix() {
        return Component.text("Contacts » ", NamedTextColor.LIGHT_PURPLE);
    }

    private static Component info(String text) {
        return prefix().append(Component.text(text, NamedTextColor.GRAY));
    }

    private static Component error(String text) {
        return prefix().append(Component.text(text, NamedTextColor.RED));
    }

    private void tell(UUID id, Component message) {
        server.getPlayer(id).ifPresent(player -> player.sendMessage(message));
    }

    // ------------------------------------------------------------------ presence

    /** Serveur ou se trouve le joueur, ou null s'il est hors ligne. */
    private String serverOf(UUID id) {
        return server.getPlayer(id).flatMap(Player::getCurrentServer)
                .map(connection -> connection.getServerInfo().getName()).orElse(null);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        synchronized (this) {
            store.seen(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
        }
    }

    /** Arrivee sur KaLium (premier serveur) : annonce aux amis, rappel des demandes en attente. */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (event.getPreviousServer().isPresent()) {
            return;
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        int pending;
        synchronized (this) {
            ContactsStore.Profile profile = store.get(id);
            pending = profile.incoming.size();
            if (!profile.invisible && announced.add(id)) {
                announce(profile, Component.text("+ ", NamedTextColor.GREEN)
                        .append(Component.text(player.getUsername(), NamedTextColor.WHITE))
                        .append(Component.text(" s'est connecté.", NamedTextColor.GRAY)));
            }
        }
        if (pending > 0) {
            player.sendMessage(info("Tu as " + pending + " demande" + (pending > 1 ? "s" : "") + " d'ami en attente : ")
                    .append(Component.text("/amis", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/amis"))));
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        synchronized (this) {
            lastPartner.remove(id);
            if (announced.remove(id)) {
                announce(store.get(id), Component.text("- ", NamedTextColor.RED)
                        .append(Component.text(player.getUsername(), NamedTextColor.WHITE))
                        .append(Component.text(" s'est déconnecté.", NamedTextColor.GRAY)));
            }
        }
    }

    /** Message envoye aux amis en ligne qui gardent les notifications. Appele sous verrou. */
    private void announce(ContactsStore.Profile profile, Component message) {
        Component line = prefix().append(message);
        for (UUID friend : profile.friends) {
            if (server.getPlayer(friend).isPresent() && store.get(friend).notify) {
                tell(friend, line);
            }
        }
    }

    // ------------------------------------------------------------------ requetes de KLM_Contacts

    /** Joueur designe par son UUID ou son pseudo (deja venu sur KaLium), ou null. Appele sous verrou. */
    private UUID resolve(String target) {
        if (target == null || target.isBlank()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(target.trim());
            return store.get(id).name.isEmpty() ? null : id;
        } catch (IllegalArgumentException e) {
            return store.byName(target);
        }
    }

    /**
     * Traite une requete POST /contacts/&lt;action&gt; (parametres : lignes « cle=valeur » du corps). Reponse : « ok » ou
     * « ok:&lt;detail&gt; », sinon « err:&lt;code&gt; » ; pour « list », les lignes de donnees suivent.
     * Actions : list, add, accept, deny, cancel, remove, block, unblock, set, join, msg.
     */
    synchronized String handle(String action, Map<String, String> params) {
        UUID id;
        try {
            id = UUID.fromString(params.getOrDefault("player", ""));
        } catch (IllegalArgumentException e) {
            return "err:player";
        }
        ContactsStore.Profile me = store.get(id);
        if ("list".equals(action)) {
            return list(me);
        }
        if ("set".equals(action)) {
            return set(me, params.getOrDefault("key", ""), params.getOrDefault("value", ""));
        }
        if ("msg".equals(action)) {
            return menuMessage(id, params.getOrDefault("target", ""), params.getOrDefault("text", ""));
        }
        UUID targetId = resolve(params.get("target"));
        if (targetId == null) {
            return "err:unknown";
        }
        if (targetId.equals(id)) {
            return "err:self";
        }
        ContactsStore.Profile target = store.get(targetId);
        return switch (action) {
            case "add" -> add(me, target);
            case "accept" -> accept(me, target);
            case "deny" -> {
                if (!me.incoming.remove(target.id)) {
                    yield "err:none";
                }
                target.outgoing.remove(me.id);
                store.save(me);
                store.save(target);
                yield "ok:" + target.name;
            }
            case "cancel" -> {
                if (!me.outgoing.remove(target.id)) {
                    yield "err:none";
                }
                target.incoming.remove(me.id);
                store.save(me);
                store.save(target);
                yield "ok:" + target.name;
            }
            case "remove" -> {
                if (!me.friends.remove(target.id)) {
                    yield "err:none";
                }
                target.friends.remove(me.id);
                store.save(me);
                store.save(target);
                yield "ok:" + target.name;
            }
            case "block" -> {
                // Plus de demandes ni de messages ; l'amitie et les demandes en cours sont retirees. Il n'est pas prevenu.
                me.blocked.add(target.id);
                me.friends.remove(target.id);
                me.incoming.remove(target.id);
                me.outgoing.remove(target.id);
                target.friends.remove(me.id);
                target.incoming.remove(me.id);
                target.outgoing.remove(me.id);
                store.save(me);
                store.save(target);
                yield "ok:" + target.name;
            }
            case "unblock" -> {
                if (!me.blocked.remove(target.id)) {
                    yield "err:none";
                }
                store.save(me);
                yield "ok:" + target.name;
            }
            case "join" -> join(me, target);
            default -> "err:action";
        };
    }

    private String list(ContactsStore.Profile me) {
        StringBuilder out = new StringBuilder("ok\n");
        String mine = serverOf(me.id);
        out.append("M\t").append(mine == null ? "" : mine).append('\n');
        out.append("S\tinvisible\t").append(me.invisible).append('\n');
        out.append("S\tnotify\t").append(me.notify).append('\n');
        out.append("S\tmp\t").append(me.mp).append('\n');
        for (UUID friend : me.friends) {
            ContactsStore.Profile profile = store.get(friend);
            // Ami invisible : affiche hors ligne.
            String where = profile.invisible ? null : serverOf(friend);
            out.append("F\t").append(friend).append('\t').append(profile.name).append('\t')
                    .append(where == null ? "" : where).append('\n');
        }
        for (UUID other : me.incoming) {
            out.append("R\t").append(other).append('\t').append(store.get(other).name).append('\n');
        }
        for (UUID other : me.outgoing) {
            out.append("O\t").append(other).append('\t').append(store.get(other).name).append('\n');
        }
        for (UUID other : me.blocked) {
            out.append("B\t").append(other).append('\t').append(store.get(other).name).append('\n');
        }
        return out.toString();
    }

    private String set(ContactsStore.Profile me, String key, String value) {
        switch (key) {
            case "invisible" -> me.invisible = Boolean.parseBoolean(value);
            case "notify" -> me.notify = Boolean.parseBoolean(value);
            case "mp" -> {
                if (!ContactsStore.MP_ALL.equals(value) && !ContactsStore.MP_FRIENDS.equals(value)
                        && !ContactsStore.MP_NONE.equals(value)) {
                    return "err:bad";
                }
                me.mp = value;
            }
            default -> {
                return "err:bad";
            }
        }
        store.save(me);
        return "ok";
    }

    private String add(ContactsStore.Profile me, ContactsStore.Profile target) {
        if (me.friends.contains(target.id)) {
            return "err:already";
        }
        if (me.blocked.contains(target.id)) {
            return "err:blocked";
        }
        if (me.incoming.contains(target.id)) {
            // Il m'avait deja demande : les deux demandes se rejoignent.
            return accept(me, target);
        }
        if (me.outgoing.contains(target.id)) {
            return "err:pending";
        }
        if (me.friends.size() >= config.maxFriends()) {
            return "err:full";
        }
        if (me.outgoing.size() >= MAX_PENDING) {
            return "err:too-many";
        }
        if (target.blocked.contains(me.id)) {
            // Bloque par ce joueur : la demande n'arrive pas, et rien ne le lui apprend.
            return "ok:sent:" + target.name;
        }
        me.outgoing.add(target.id);
        target.incoming.add(me.id);
        store.save(me);
        store.save(target);
        tell(target.id, prefix().append(Component.text(me.name, NamedTextColor.WHITE))
                .append(Component.text(" te demande en ami : ", NamedTextColor.GRAY))
                .append(Component.text("/amis accepter " + me.name, NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/amis accepter " + me.name)))
                .append(Component.text(" (ou /amis)", NamedTextColor.GRAY)));
        return "ok:sent:" + target.name;
    }

    private String accept(ContactsStore.Profile me, ContactsStore.Profile target) {
        if (!me.incoming.contains(target.id)) {
            return "err:none";
        }
        if (me.friends.size() >= config.maxFriends()) {
            return "err:full";
        }
        if (target.friends.size() >= config.maxFriends()) {
            return "err:target-full";
        }
        me.incoming.remove(target.id);
        me.outgoing.remove(target.id);
        target.outgoing.remove(me.id);
        target.incoming.remove(me.id);
        me.friends.add(target.id);
        target.friends.add(me.id);
        store.save(me);
        store.save(target);
        tell(target.id, prefix().append(Component.text(me.name, NamedTextColor.WHITE))
                .append(Component.text(" a accepté ta demande d'ami.", NamedTextColor.GREEN)));
        return "ok:friends:" + target.name;
    }

    /** « Rejoindre son serveur » : le proxy deplace le joueur. */
    private String join(ContactsStore.Profile me, ContactsStore.Profile target) {
        if (!me.friends.contains(target.id)) {
            return "err:not-friend";
        }
        Optional<Player> player = server.getPlayer(me.id);
        String where = target.invisible ? null : serverOf(target.id);
        if (player.isEmpty() || where == null) {
            return "err:offline";
        }
        if (where.equals(serverOf(me.id))) {
            return "err:same";
        }
        Optional<RegisteredServer> destination = server.getServer(where);
        if (destination.isEmpty() || config.noJoin(where)) {
            return "err:closed:" + where;
        }
        player.get().createConnectionRequest(destination.get()).fireAndForget();
        return "ok:" + where;
    }

    /** Champ « Envoyer un message » du menu : la commande /mp est rejouee au nom du joueur (memes regles, memes refus). */
    private String menuMessage(UUID id, String target, String text) {
        Optional<Player> player = server.getPlayer(id);
        String clean = clean(text);
        if (player.isEmpty() || clean.isEmpty() || !target.matches("[^\\s]{1,32}")) {
            return "err:bad";
        }
        server.getCommandManager().executeAsync(player.get(), "mp " + target + " " + clean);
        return "ok";
    }

    // ------------------------------------------------------------------ messages prives

    private static String clean(String text) {
        String clean = text == null ? "" : text.replaceAll("\\p{Cntrl}", " ").trim();
        return clean.length() > MAX_MESSAGE ? clean.substring(0, MAX_MESSAGE) : clean;
    }

    private void message(Player sender, String targetName, String rawText) {
        String text = clean(rawText);
        if (text.isEmpty()) {
            return;
        }
        Optional<Player> found = server.getPlayer(targetName);
        Component notOnline = error(targetName + " n'est pas en ligne.");
        if (found.isEmpty()) {
            sender.sendMessage(notOnline);
            return;
        }
        Player target = found.get();
        UUID from = sender.getUniqueId();
        UUID to = target.getUniqueId();
        if (from.equals(to)) {
            sender.sendMessage(error("Tu ne peux pas t'écrire à toi-même."));
            return;
        }
        boolean deliver;
        synchronized (this) {
            ContactsStore.Profile me = store.get(from);
            ContactsStore.Profile other = store.get(to);
            if (me.blocked.contains(to)) {
                sender.sendMessage(error("Tu as bloqué ce joueur : /debloquer " + target.getUsername()));
                return;
            }
            boolean friends = me.friends.contains(to);
            // Joueur invisible : hors ligne pour tous, sauf pour celui a qui il vient d'ecrire (reponse).
            if (other.invisible && !to.equals(lastPartner.get(from))) {
                sender.sendMessage(notOnline);
                return;
            }
            if (ContactsStore.MP_NONE.equals(other.mp) || (ContactsStore.MP_FRIENDS.equals(other.mp) && !friends)) {
                sender.sendMessage(error(target.getUsername() + " n'accepte pas les messages privés"
                        + (ContactsStore.MP_FRIENDS.equals(other.mp) ? " (amis seulement)." : ".")));
                return;
            }
            // Bloque par le destinataire : rien n'est livre, et rien ne le lui apprend.
            deliver = !other.blocked.contains(from);
            lastPartner.put(from, to);
            if (deliver) {
                lastPartner.put(to, from);
            }
        }
        sender.sendMessage(Component.text("[MP] À " + target.getUsername() + " : ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(text, NamedTextColor.WHITE)));
        if (deliver) {
            target.sendMessage(Component.text("[MP] De " + sender.getUsername() + " : ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(text, NamedTextColor.WHITE))
                    .append(Component.text("  (/r pour répondre)", NamedTextColor.DARK_GRAY)));
        }
        logger.info("[Contacts] MP " + sender.getUsername() + " -> " + target.getUsername()
                + (deliver ? "" : " (non livre : bloque)") + " : " + text);
    }

    /** Pseudos des amis en ligne et visibles, pour la touche Tab de /mp. */
    private synchronized List<String> onlineFriendNames(UUID id, String start) {
        List<String> names = new ArrayList<>();
        String prefix = start.toLowerCase(Locale.ROOT);
        for (UUID friend : store.get(id).friends) {
            Optional<Player> player = server.getPlayer(friend);
            if (player.isPresent() && !store.get(friend).invisible
                    && player.get().getUsername().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(player.get().getUsername());
            }
        }
        return names;
    }
}
