package fr.kalium.relay;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 1.7.0 - groupes de jeu de KLM_Contacts, etape 2 (demande de LeKiwi06, 09/10/2026 : « creer des groupes avec eux » ;
 * reponses : groupe temporaire avec un chef, tchat de groupe, les membres suivent le chef d'office, reglable par
 * membre). Les groupes vivent en memoire sur le proxy : ils disparaissent quand il ne reste qu'une personne, et au
 * redemarrage du proxy.
 *
 * - Creer un groupe = inviter quelqu'un : a l'acceptation (60 s), celui qui a invite devient le chef.
 * - Le chef invite, exclut, nomme un autre chef, dissout ; chacun peut quitter. Chef parti ou deconnecte : le plus
 *   ancien membre connecte devient chef. Membre deconnecte depuis plus de 5 minutes : il sort du groupe.
 * - Commandes du proxy : /groupe [inviter|accepter|refuser|quitter|exclure|chef|dissoudre|suivre|info] et /gc &lt;message&gt;
 *   (tchat de groupe, ecrit dans le journal du proxy ; a ajouter aux commandes bloquees de LibertyBans).
 * - Suivre le chef : quand il change de serveur, chaque membre est deplace d'office, sauf s'il a choisi « me demander
 *   avant » ou s'il est en pleine partie (il recoit alors une proposition « /groupe suivre »). « En partie » n'est connu
 *   que du serveur du membre : le proxy le lui demande par un message de plugin (canal kalium:contacts, « follow ») et
 *   KLM_Contacts 1.1.0 repond par POST /contacts/gfollow. Sans reponse sous 3 s : proposition. Jamais vers un serveur
 *   de contacts-no-join (Serveur Jeux : on n'y entre que par une partie).
 *
 * Toutes les methodes qui touchent aux groupes sont appelees sous le verrou de {@link Contacts}.
 */
final class ContactsGroups {

    private static final long INVITE_MILLIS = 60_000L;
    private static final long OFFLINE_MILLIS = 5 * 60_000L;
    private static final long FOLLOW_WAIT_SECONDS = 3L;

    private static final List<String> SUBCOMMANDS = List.of("inviter", "accepter", "refuser", "quitter", "exclure", "chef",
            "dissoudre", "suivre", "info");

    static final class Group {
        UUID leader;
        /** Membres dans l'ordre d'arrivee (le chef compris). */
        final Set<UUID> members = new LinkedHashSet<>();
    }

    private record Invite(UUID from, long expires) {
    }

    private record PendingFollow(String server) {
    }

    /**
     * 1.8.0 (etape 3) - partie proposee a un joueur : celle ou son chef de groupe vient d'entrer, ou celle ou un joueur
     * l'invite. server = serveur ou le jeu sait faire entrer le joueur (kal-games) ; game et ref designent la partie
     * pour le plugin du jeu (voir fr.kalium.contacts.api.JeuDeGroupe).
     */
    private static final class Offer {
        final UUID from;
        final String server;
        final String game;
        final String ref;
        final String label;
        final long expires;
        /** Invitation d'un joueur (toujours a accepter), sinon partie du chef du groupe. */
        final boolean invite;
        /** Le joueur a accepte (/partie accepter, /groupe suivre) : il entre meme s'il etait en partie. */
        boolean confirmed;
        /** La proposition lui a deja ete envoyee. */
        boolean proposed;

        Offer(UUID from, String server, String game, String ref, String label, long expires, boolean invite) {
            this.from = from;
            this.server = server;
            this.game = game;
            this.ref = ref;
            this.label = label;
            this.expires = expires;
            this.invite = invite;
        }
    }

    private final Contacts contacts;
    private final Object plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final RelayConfig config;
    private final ActiveGameRegistry activeGames;
    private final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.create("kalium", "contacts");
    private final Map<UUID, Group> groupOf = new HashMap<>();
    /** Invitation en attente de chaque joueur invite (une seule a la fois). */
    private final Map<UUID, Invite> invites = new HashMap<>();
    /** Membres deconnectes : instant de la deconnexion. */
    private final Map<UUID, Long> offlineSince = new HashMap<>();
    /** Membres dont on attend la reponse du serveur (« en partie ? ») avant de les deplacer. */
    private final Map<UUID, PendingFollow> pendingFollow = new HashMap<>();
    /** 1.8.0 : partie proposee a chaque joueur (une seule a la fois). */
    private final Map<UUID, Offer> offers = new HashMap<>();
    /** 1.8.0 : instant ou un joueur vient d'entrer dans une partie proposee (il va peut-etre changer de serveur). */
    private final Map<UUID, Long> joinedAt = new HashMap<>();

    ContactsGroups(Contacts contacts, Object plugin, ProxyServer server, Logger logger, RelayConfig config,
                   ActiveGameRegistry activeGames) {
        this.contacts = contacts;
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.activeGames = activeGames;
    }

    void register() {
        server.getChannelRegistrar().register(channel);
        server.getEventManager().register(plugin, this);
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("groupe").aliases("group").plugin(plugin).build(), new SimpleCommand() {
                    @Override
                    public void execute(Invocation invocation) {
                        if (invocation.source() instanceof Player player) {
                            command(player, invocation.arguments());
                        }
                    }

                    @Override
                    public List<String> suggest(Invocation invocation) {
                        if (!(invocation.source() instanceof Player player)) {
                            return List.of();
                        }
                        return suggestions(player, invocation.arguments());
                    }
                });
        server.getCommandManager().register(server.getCommandManager().metaBuilder("gc").plugin(plugin).build(),
                (SimpleCommand) invocation -> {
                    if (!(invocation.source() instanceof Player player)) {
                        return;
                    }
                    if (invocation.arguments().length == 0) {
                        player.sendMessage(error("Utilisation : /gc <message>"));
                        return;
                    }
                    chat(player, String.join(" ", invocation.arguments()));
                });
        // 1.8.0 : /partie accepter | refuser (partie du chef du groupe, ou invitation d'un joueur dans sa partie).
        server.getCommandManager().register(server.getCommandManager().metaBuilder("partie").plugin(plugin).build(),
                new SimpleCommand() {
                    @Override
                    public void execute(Invocation invocation) {
                        if (!(invocation.source() instanceof Player player)) {
                            return;
                        }
                        String sub = invocation.arguments().length == 0 ? "" : invocation.arguments()[0].toLowerCase(Locale.ROOT);
                        synchronized (contacts) {
                            switch (sub) {
                                case "accepter" -> acceptOffer(player.getUniqueId());
                                case "refuser" -> denyOffer(player.getUniqueId());
                                default -> player.sendMessage(info("Utilisation : /partie accepter | refuser"));
                            }
                        }
                    }

                    @Override
                    public List<String> suggest(Invocation invocation) {
                        return invocation.arguments().length <= 1 ? List.of("accepter", "refuser") : List.of();
                    }
                });
        server.getScheduler().buildTask(plugin, this::sweep).repeat(20L, TimeUnit.SECONDS).schedule();
    }

    // ------------------------------------------------------------------ textes

    private static Component prefix() {
        return Component.text("Groupe » ", NamedTextColor.AQUA);
    }

    private static Component info(String text) {
        return prefix().append(Component.text(text, NamedTextColor.GRAY));
    }

    private static Component error(String text) {
        return prefix().append(Component.text(text, NamedTextColor.RED));
    }

    /** Nom affiche d'un serveur (nom du velocity.toml). */
    static String pretty(String serverName) {
        return switch (serverName.toLowerCase(Locale.ROOT)) {
            case "lobby" -> "Lobby";
            case "kal-games" -> "Kal-Games";
            case "serveur-jeux" -> "Serveur Jeux";
            case "kixster" -> "Kixster";
            case "event" -> "Event";
            case "kanvas" -> "Kanvas";
            default -> serverName;
        };
    }

    private String name(UUID id) {
        Optional<Player> player = server.getPlayer(id);
        if (player.isPresent()) {
            return player.get().getUsername();
        }
        String stored = contacts.store.get(id).name;
        return stored.isEmpty() ? id.toString().substring(0, 8) : stored;
    }

    private void broadcast(Group group, Component message) {
        for (UUID member : group.members) {
            contacts.tell(member, message);
        }
    }

    // ------------------------------------------------------------------ commandes

    private void command(Player player, String[] args) {
        UUID id = player.getUniqueId();
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        String target = args.length > 1 ? args[1] : "";
        synchronized (contacts) {
            switch (sub) {
                case "" -> openMenu(player);
                case "info" -> status(player);
                case "inviter" -> {
                    if (target.isEmpty()) {
                        player.sendMessage(error("Utilisation : /groupe inviter <pseudo>"));
                    } else {
                        invite(id, target);
                    }
                }
                case "accepter" -> accept(id);
                case "refuser" -> deny(id);
                case "quitter" -> leave(id);
                case "exclure" -> {
                    if (target.isEmpty()) {
                        player.sendMessage(error("Utilisation : /groupe exclure <pseudo>"));
                    } else {
                        kick(id, target);
                    }
                }
                case "chef" -> {
                    if (target.isEmpty()) {
                        player.sendMessage(error("Utilisation : /groupe chef <pseudo>"));
                    } else {
                        promote(id, target);
                    }
                }
                case "dissoudre" -> disband(id);
                case "suivre" -> go(id);
                default -> player.sendMessage(info("Utilisation : /groupe (menu), /groupe inviter | exclure | chef <pseudo>, "
                        + "/groupe accepter | refuser | quitter | dissoudre | suivre | info, /gc <message>"));
            }
        }
    }

    private List<String> suggestions(Player player, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length <= 1) {
            String start = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(start)) {
                    out.add(sub);
                }
            }
            return out;
        }
        if (args.length != 2) {
            return out;
        }
        String start = args[1].toLowerCase(Locale.ROOT);
        synchronized (contacts) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "inviter" -> {
                    for (Player other : server.getAllPlayers()) {
                        if (out.size() < 30 && !other.equals(player) && !contacts.store.get(other.getUniqueId()).invisible
                                && other.getUsername().toLowerCase(Locale.ROOT).startsWith(start)) {
                            out.add(other.getUsername());
                        }
                    }
                }
                case "exclure", "chef" -> {
                    Group group = groupOf.get(player.getUniqueId());
                    if (group != null) {
                        for (UUID member : group.members) {
                            String memberName = name(member);
                            if (!member.equals(player.getUniqueId()) && memberName.toLowerCase(Locale.ROOT).startsWith(start)) {
                                out.add(memberName);
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }
        return out;
    }

    /** « /groupe » sans rien : le serveur du joueur ouvre le menu du groupe (KLM_Contacts). */
    private void openMenu(Player player) {
        Optional<ServerConnection> connection = player.getCurrentServer();
        if (connection.isEmpty() || !connection.get().sendPluginMessage(channel, "groupe".getBytes(StandardCharsets.UTF_8))) {
            status(player);
        }
    }

    private void status(Player player) {
        Group group = groupOf.get(player.getUniqueId());
        if (group == null) {
            player.sendMessage(info("Tu n'es dans aucun groupe. Pour en créer un : /groupe inviter <pseudo>"));
            return;
        }
        StringBuilder names = new StringBuilder();
        for (UUID member : group.members) {
            if (names.length() > 0) {
                names.append(", ");
            }
            String where = contacts.serverOf(member);
            names.append(name(member)).append(member.equals(group.leader) ? " (chef)" : "")
                    .append(where == null ? " [hors ligne]" : " [" + pretty(where) + "]");
        }
        player.sendMessage(info("Groupe (" + group.members.size() + "/" + config.maxGroup() + ") : " + names));
    }

    // ------------------------------------------------------------------ requetes de KLM_Contacts

    boolean handles(String action) {
        return switch (action) {
            case "group", "ginvite", "gaccept", "gdeny", "gleave", "gkick", "gleader", "gdisband", "gfollow", "ggo",
                 "ggame", "ginvitegame", "ggameget", "ggameaccept", "ggamedeny", "whereami" -> true;
            default -> false;
        };
    }

    /**
     * Requetes POST /contacts/&lt;action&gt; du menu « Groupe » (appele sous verrou par Contacts.handle) : group (etat du
     * groupe), ginvite, gaccept, gdeny, gleave, gkick, gleader, gdisband, ggo (rejoindre le chef), gfollow (reponse du
     * serveur a « follow » : busy=true|false). Le resultat est aussi dit au joueur dans le tchat, par le proxy.
     */
    String handle(String action, UUID id, Map<String, String> params) {
        String target = params.getOrDefault("target", "");
        return switch (action) {
            case "group" -> describe(id);
            case "ginvite" -> invite(id, target);
            case "gaccept" -> accept(id);
            case "gdeny" -> deny(id);
            case "gleave" -> leave(id);
            case "gkick" -> kick(id, target);
            case "gleader" -> promote(id, target);
            case "gdisband" -> disband(id);
            case "ggo" -> go(id);
            case "gfollow" -> followAnswer(id, Boolean.parseBoolean(params.getOrDefault("busy", "false")));
            // 1.8.0 (etape 3) : parties.
            case "ggame" -> announce(id, params);
            case "ginvitegame" -> inviteGame(id, target, params);
            case "ggameget" -> gameGet(id, Boolean.parseBoolean(params.getOrDefault("busy", "false")));
            case "ggameaccept" -> acceptOffer(id);
            case "ggamedeny" -> denyOffer(id);
            case "whereami" -> {
                String where = contacts.serverOf(id);
                yield where == null ? "err:offline" : "ok:" + where;
            }
            default -> "err:action";
        };
    }

    /** Etat du groupe pour le menu : « G » (chef, suis-je le chef, taille maximale), « P » par membre, « I » invitation. */
    private String describe(UUID id) {
        StringBuilder out = new StringBuilder("ok\n");
        Group group = groupOf.get(id);
        if (group != null) {
            out.append("G\t").append(group.leader).append('\t').append(group.leader.equals(id)).append('\t')
                    .append(config.maxGroup()).append('\n');
            for (UUID member : group.members) {
                String where = contacts.serverOf(member);
                out.append("P\t").append(member).append('\t').append(name(member)).append('\t')
                        .append(where == null ? "" : where).append('\t').append(member.equals(group.leader)).append('\n');
            }
        }
        Invite invite = invites.get(id);
        if (invite != null && invite.expires() > System.currentTimeMillis()) {
            out.append("I\t").append(invite.from()).append('\t').append(name(invite.from())).append('\n');
        }
        Offer offer = liveOffer(id);
        if (offer != null) {
            // 1.8.0 : partie proposee (« O », pseudo de celui qui la propose, nom du jeu).
            out.append("O\t").append(name(offer.from)).append('\t').append(offer.label).append('\n');
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ invitations

    private String fail(UUID id, String code, String message) {
        contacts.tell(id, error(message));
        return "err:" + code;
    }

    private String invite(UUID id, String targetName) {
        Group group = groupOf.get(id);
        if (group != null && !group.leader.equals(id)) {
            return fail(id, "not-leader", "Seul le chef du groupe peut inviter.");
        }
        Optional<Player> found = server.getPlayer(targetName);
        if (found.isEmpty() || contacts.store.get(found.get().getUniqueId()).invisible) {
            return fail(id, "offline", targetName + " n'est pas en ligne.");
        }
        Player target = found.get();
        UUID to = target.getUniqueId();
        if (to.equals(id)) {
            return fail(id, "self", "Tu ne peux pas t'inviter toi-même.");
        }
        ContactsStore.Profile me = contacts.store.get(id);
        ContactsStore.Profile other = contacts.store.get(to);
        if (me.blocked.contains(to)) {
            return fail(id, "blocked", "Tu as bloqué ce joueur : /debloquer " + target.getUsername());
        }
        Group theirs = groupOf.get(to);
        if (theirs != null) {
            return fail(id, "in-group", target.getUsername() + (theirs == group ? " est déjà dans ton groupe."
                    : " est déjà dans un groupe."));
        }
        if (group != null && group.members.size() >= config.maxGroup()) {
            return fail(id, "full", "Le groupe est complet (" + config.maxGroup() + " joueurs).");
        }
        if (ContactsStore.MP_FRIENDS.equals(other.invites) && !other.friends.contains(id)) {
            return fail(id, "friends-only", target.getUsername() + " n'accepte que les invitations de ses amis.");
        }
        contacts.tell(id, info("Invitation envoyée à " + target.getUsername() + " (60 s pour répondre)."));
        if (other.blocked.contains(id)) {
            // Bloque par ce joueur : l'invitation n'arrive pas, et rien ne le lui apprend.
            return "ok";
        }
        invites.put(to, new Invite(id, System.currentTimeMillis() + INVITE_MILLIS));
        target.sendMessage(prefix().append(Component.text(name(id), NamedTextColor.WHITE))
                .append(Component.text(" t'invite dans son groupe : ", NamedTextColor.GRAY))
                .append(Component.text("/groupe accepter", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/groupe accepter")))
                .append(Component.text(" ou ", NamedTextColor.GRAY))
                .append(Component.text("/groupe refuser", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/groupe refuser")))
                .append(Component.text(" (60 s)", NamedTextColor.GRAY)));
        return "ok";
    }

    private String accept(UUID id) {
        Invite invite = invites.remove(id);
        if (invite == null || invite.expires() <= System.currentTimeMillis()) {
            return fail(id, "none", "Aucune invitation en attente.");
        }
        if (groupOf.containsKey(id)) {
            return fail(id, "in-group", "Tu es déjà dans un groupe : quitte-le d'abord (/groupe quitter).");
        }
        UUID from = invite.from();
        Group group = groupOf.get(from);
        if (server.getPlayer(from).isEmpty() || (group != null && !group.leader.equals(from))) {
            return fail(id, "invalid", "Cette invitation n'est plus valable.");
        }
        if (group != null && group.members.size() >= config.maxGroup()) {
            return fail(id, "full", "Le groupe est complet (" + config.maxGroup() + " joueurs).");
        }
        if (group == null) {
            // Premier invite qui accepte : le groupe est cree, celui qui a invite en est le chef.
            group = new Group();
            group.leader = from;
            group.members.add(from);
            groupOf.put(from, group);
        }
        group.members.add(id);
        groupOf.put(id, group);
        broadcast(group, prefix().append(Component.text(name(id), NamedTextColor.WHITE))
                .append(Component.text(" a rejoint le groupe (" + group.members.size() + "/" + config.maxGroup() + "). Tchat : /gc",
                        NamedTextColor.GRAY)));
        follow(group, id, contacts.serverOf(group.leader));
        return "ok";
    }

    private String deny(UUID id) {
        Invite invite = invites.remove(id);
        if (invite == null || invite.expires() <= System.currentTimeMillis()) {
            return fail(id, "none", "Aucune invitation en attente.");
        }
        contacts.tell(id, info("Invitation refusée."));
        contacts.tell(invite.from(), info(name(id) + " a refusé ton invitation."));
        return "ok";
    }

    // ------------------------------------------------------------------ vie du groupe

    /** Membre du groupe du chef designe par son pseudo (majuscules ignorees), ou null. */
    private UUID member(Group group, String targetName) {
        for (UUID member : group.members) {
            if (name(member).equalsIgnoreCase(targetName) || member.toString().equalsIgnoreCase(targetName)) {
                return member;
            }
        }
        return null;
    }

    private void remove(Group group, UUID id) {
        group.members.remove(id);
        groupOf.remove(id, group);
        offlineSince.remove(id);
        pendingFollow.remove(id);
    }

    /** Le chef n'est plus la : le plus ancien membre connecte (a defaut le plus ancien) devient chef. */
    private void newLeader(Group group) {
        UUID chosen = null;
        for (UUID member : group.members) {
            if (member.equals(group.leader)) {
                continue; // l'ancien chef (encore membre s'il vient seulement de se deconnecter)
            }
            if (chosen == null) {
                chosen = member;
            }
            if (server.getPlayer(member).isPresent()) {
                chosen = member;
                break;
            }
        }
        if (chosen == null) {
            return;
        }
        group.leader = chosen;
        broadcast(group, prefix().append(Component.text(name(chosen), NamedTextColor.WHITE))
                .append(Component.text(" est le nouveau chef du groupe.", NamedTextColor.GRAY)));
    }

    /** Un groupe d'une seule personne n'existe plus. */
    private void dissolveIfAlone(Group group) {
        if (group.members.size() > 1) {
            return;
        }
        for (UUID last : new ArrayList<>(group.members)) {
            contacts.tell(last, info("Le groupe est dissous : il ne reste que toi."));
            remove(group, last);
        }
    }

    private String leave(UUID id) {
        Group group = groupOf.get(id);
        if (group == null) {
            return fail(id, "no-group", "Tu n'es dans aucun groupe.");
        }
        boolean wasLeader = group.leader.equals(id);
        remove(group, id);
        contacts.tell(id, info("Tu as quitté le groupe."));
        broadcast(group, info(name(id) + " a quitté le groupe."));
        if (wasLeader) {
            newLeader(group);
        }
        dissolveIfAlone(group);
        return "ok";
    }

    private String kick(UUID id, String targetName) {
        Group group = groupOf.get(id);
        if (group == null) {
            return fail(id, "no-group", "Tu n'es dans aucun groupe.");
        }
        if (!group.leader.equals(id)) {
            return fail(id, "not-leader", "Seul le chef du groupe peut exclure.");
        }
        UUID target = member(group, targetName);
        if (target == null || target.equals(id)) {
            return fail(id, "unknown", targetName + " n'est pas dans ton groupe.");
        }
        String targetShown = name(target);
        remove(group, target);
        contacts.tell(target, info("Tu as été exclu du groupe."));
        broadcast(group, info(targetShown + " a été exclu du groupe."));
        dissolveIfAlone(group);
        return "ok";
    }

    private String promote(UUID id, String targetName) {
        Group group = groupOf.get(id);
        if (group == null) {
            return fail(id, "no-group", "Tu n'es dans aucun groupe.");
        }
        if (!group.leader.equals(id)) {
            return fail(id, "not-leader", "Seul le chef du groupe peut nommer un autre chef.");
        }
        UUID target = member(group, targetName);
        if (target == null || target.equals(id)) {
            return fail(id, "unknown", targetName + " n'est pas dans ton groupe.");
        }
        if (server.getPlayer(target).isEmpty()) {
            return fail(id, "offline", name(target) + " n'est pas en ligne.");
        }
        group.leader = target;
        broadcast(group, prefix().append(Component.text(name(target), NamedTextColor.WHITE))
                .append(Component.text(" est le nouveau chef du groupe.", NamedTextColor.GRAY)));
        return "ok";
    }

    private String disband(UUID id) {
        Group group = groupOf.get(id);
        if (group == null) {
            return fail(id, "no-group", "Tu n'es dans aucun groupe.");
        }
        if (!group.leader.equals(id)) {
            return fail(id, "not-leader", "Seul le chef peut dissoudre le groupe (/groupe quitter pour partir).");
        }
        broadcast(group, info("Le groupe est dissous."));
        for (UUID member : new ArrayList<>(group.members)) {
            remove(group, member);
        }
        return "ok";
    }

    // ------------------------------------------------------------------ tchat de groupe

    void chat(Player sender, String rawText) {
        String text = Contacts.clean(rawText);
        if (text.isEmpty()) {
            return;
        }
        synchronized (contacts) {
            Group group = groupOf.get(sender.getUniqueId());
            if (group == null) {
                sender.sendMessage(error("Tu n'es dans aucun groupe."));
                return;
            }
            broadcast(group, Component.text("[Groupe] " + sender.getUsername() + " : ", NamedTextColor.AQUA)
                    .append(Component.text(text, NamedTextColor.WHITE)));
            logger.info("[Contacts] GC (groupe de " + name(group.leader) + ") " + sender.getUsername() + " : " + text);
        }
    }

    // ------------------------------------------------------------------ suivre le chef

    /** Un joueur vient d'arriver sur un serveur (premiere connexion comprise). */
    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        synchronized (contacts) {
            String here = contacts.serverOf(id);
            // 1.8.0 : une partie l'attend sur ce serveur (celle de son chef, ou une invitation acceptee).
            Offer offer = liveOffer(id);
            if (offer != null && offer.server.equals(here)) {
                sendGame(id);
            }
            Group group = groupOf.get(id);
            if (group == null) {
                return;
            }
            pendingFollow.remove(id);
            if (offlineSince.remove(id) != null) {
                broadcast(group, info(player.getUsername() + " est de retour dans le groupe."));
            }
            if (group.leader.equals(id)) {
                for (UUID member : new ArrayList<>(group.members)) {
                    if (!member.equals(id)) {
                        follow(group, member, here);
                    }
                }
            } else if (event.getPreviousServer() == null) {
                // Membre qui se reconnecte a KaLium : il rejoint le chef.
                follow(group, id, contacts.serverOf(group.leader));
            }
        }
    }

    /** Messages du canal kalium:contacts : jamais relayes (un joueur ne peut pas en fabriquer pour son serveur). */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier().equals(channel)) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }

    private void propose(UUID member, Group group, String leaderServer) {
        Offer offer = liveOffer(member);
        if (offer != null && offer.from.equals(group.leader)) {
            proposeGame(member, offer); // 1.8.0 : le chef est entre dans une partie
            return;
        }
        contacts.tell(member, prefix().append(Component.text(name(group.leader), NamedTextColor.WHITE))
                .append(Component.text(" (chef) est sur " + pretty(leaderServer) + " : ", NamedTextColor.GRAY))
                .append(Component.text("/groupe suivre", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/groupe suivre"))));
    }

    /** Le chef est sur leaderServer : ce membre le suit d'office, ou recoit une proposition. Appele sous verrou. */
    private void follow(Group group, UUID member, String leaderServer) {
        if (leaderServer == null || config.noJoin(leaderServer) || member.equals(group.leader)) {
            return;
        }
        // 1.8.0 : le chef vient d'entrer dans une partie qui se joue ailleurs (Build Battle sur Kanvas...) : c'est la
        // partie proposee qui emmene le membre, pas le changement de serveur du chef.
        Offer offer = liveOffer(member);
        Long joined = joinedAt.get(member);
        if ((offer != null && offer.from.equals(group.leader) && !offer.server.equals(leaderServer))
                || (joined != null && System.currentTimeMillis() - joined < 10_000L)) {
            return;
        }
        Optional<Player> player = server.getPlayer(member);
        if (player.isEmpty() || leaderServer.equals(contacts.serverOf(member))) {
            return;
        }
        Optional<ServerConnection> connection = player.get().getCurrentServer();
        // « Me demander avant », partie de Bingo en cours (connue du relais), membre sur un serveur ou l'on n'est que
        // pour une partie (Serveur Jeux : salle d'attente, partie, salle d'apres-partie) ou serveur injoignable :
        // proposition.
        String memberServer = contacts.serverOf(member);
        if (ContactsStore.FOLLOW_ASK.equals(contacts.store.get(member).follow) || activeGames.get(member).isPresent()
                || (memberServer != null && config.noJoin(memberServer))
                || connection.isEmpty()
                || !connection.get().sendPluginMessage(channel, "follow".getBytes(StandardCharsets.UTF_8))) {
            propose(member, group, leaderServer);
            return;
        }
        // Le serveur du membre dit s'il est en partie (POST /contacts/gfollow) ; sans reponse, proposition.
        PendingFollow pending = new PendingFollow(leaderServer);
        pendingFollow.put(member, pending);
        server.getScheduler().buildTask(plugin, () -> {
            synchronized (contacts) {
                if (pendingFollow.remove(member, pending)) {
                    Group current = groupOf.get(member);
                    String where = current == null ? null : target(member, current);
                    if (where != null && !config.noJoin(where) && !where.equals(contacts.serverOf(member))) {
                        propose(member, current, where);
                    }
                }
            }
        }).delay(FOLLOW_WAIT_SECONDS, TimeUnit.SECONDS).schedule();
    }

    /** Reponse du serveur du membre : en partie (proposition) ou libre (deplacement). */
    private String followAnswer(UUID member, boolean busy) {
        if (pendingFollow.remove(member) == null) {
            return "err:none";
        }
        Group group = groupOf.get(member);
        String where = group == null ? null : target(member, group);
        if (where == null || config.noJoin(where) || where.equals(contacts.serverOf(member))) {
            return "ok";
        }
        if (busy) {
            propose(member, group, where);
            return "ok:busy";
        }
        return move(member, group, where) ? "ok:moved" : "err:closed";
    }

    private boolean move(UUID member, Group group, String where) {
        Optional<Player> player = server.getPlayer(member);
        Optional<RegisteredServer> destination = server.getServer(where);
        if (player.isEmpty() || destination.isEmpty()) {
            return false;
        }
        player.get().sendMessage(info("Tu suis " + name(group.leader) + " sur " + pretty(where) + "."));
        player.get().createConnectionRequest(destination.get()).fireAndForget();
        return true;
    }

    /** « /groupe suivre » : rejoindre le chef maintenant (meme en partie : c'est le joueur qui decide). */
    private String go(UUID id) {
        Group group = groupOf.get(id);
        if (group == null) {
            return fail(id, "no-group", "Tu n'es dans aucun groupe.");
        }
        if (group.leader.equals(id)) {
            return fail(id, "leader", "Tu es le chef : ce sont les autres qui te suivent.");
        }
        Offer offer = liveOffer(id);
        if (offer != null && offer.from.equals(group.leader)) {
            return acceptOffer(id); // 1.8.0 : le chef est en partie, « suivre » = entrer dans sa partie
        }
        String where = contacts.serverOf(group.leader);
        if (where == null) {
            return fail(id, "offline", "Le chef du groupe n'est pas en ligne.");
        }
        if (where.equals(contacts.serverOf(id))) {
            return fail(id, "same", "Tu es déjà sur le serveur du chef.");
        }
        if (config.noJoin(where)) {
            return fail(id, "closed", "On n'entre pas librement sur " + pretty(where) + " : il faut y être invité par une partie.");
        }
        pendingFollow.remove(id);
        return move(id, group, where) ? "ok" : "err:closed";
    }

    // ------------------------------------------------------------------ parties (1.8.0, etape 3)

    private static final long GROUP_OFFER_MILLIS = 120_000L;
    private static final long INVITE_OFFER_MILLIS = 60_000L;

    private Offer liveOffer(UUID id) {
        Offer offer = offers.get(id);
        if (offer != null && offer.expires <= System.currentTimeMillis()) {
            offers.remove(id);
            return null;
        }
        return offer;
    }

    /** Serveur ou ce membre doit aller : celui de la partie proposee par son chef, sinon celui du chef. */
    private String target(UUID member, Group group) {
        Offer offer = liveOffer(member);
        return offer != null && offer.from.equals(group.leader) ? offer.server : contacts.serverOf(group.leader);
    }

    /** Demande au serveur du joueur de le faire entrer dans la partie proposee (il repondra par ggameget). */
    private boolean sendGame(UUID id) {
        Optional<ServerConnection> connection = server.getPlayer(id).flatMap(Player::getCurrentServer);
        return connection.isPresent() && connection.get().sendPluginMessage(channel, "game".getBytes(StandardCharsets.UTF_8));
    }

    private void proposeGame(UUID id, Offer offer) {
        offer.proposed = true;
        Component line = prefix().append(Component.text(name(offer.from), NamedTextColor.WHITE))
                .append(Component.text((offer.invite ? " t'invite dans sa partie" : " (chef) entre dans une partie")
                        + (offer.label.isEmpty() ? "" : " (" + offer.label + ")") + " : ", NamedTextColor.GRAY))
                .append(Component.text("/partie accepter", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/partie accepter")));
        if (offer.invite) {
            line = line.append(Component.text(" ou ", NamedTextColor.GRAY))
                    .append(Component.text("/partie refuser", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/partie refuser")))
                    .append(Component.text(" (60 s)", NamedTextColor.GRAY));
        }
        contacts.tell(id, line);
    }

    /** Partie decrite par un serveur Paper : jeu, reference, nom affiche, serveur ou l'on y entre. */
    private Offer offer(UUID from, Map<String, String> params, boolean invite) {
        String game = params.getOrDefault("game", "");
        String ref = params.getOrDefault("ref", "");
        String where = params.getOrDefault("server", "");
        if (where.isBlank()) {
            where = contacts.serverOf(from);
        }
        if (where == null || !game.matches("[a-z]{1,16}") || !ref.matches("[^\\s]{1,64}")) {
            return null;
        }
        String label = Contacts.clean(params.getOrDefault("label", ""));
        return new Offer(from, where, game, ref, label.length() > 40 ? label.substring(0, 40) : label,
                System.currentTimeMillis() + (invite ? INVITE_OFFER_MILLIS : GROUP_OFFER_MILLIS), invite);
    }

    /**
     * Un joueur vient d'entrer dans une partie (annonce par le plugin du jeu). S'il est chef d'un groupe, chaque membre
     * connecte y entre avec lui : d'office, ou sur proposition (« me demander avant », membre en partie ou sur Serveur
     * Jeux). Sans groupe, ou simple membre : rien.
     */
    private String announce(UUID id, Map<String, String> params) {
        Group group = groupOf.get(id);
        if (group == null || !group.leader.equals(id)) {
            return "ok:ignored";
        }
        for (UUID member : new ArrayList<>(group.members)) {
            if (member.equals(id) || server.getPlayer(member).isEmpty()) {
                continue;
            }
            Offer offer = offer(id, params, false);
            if (offer == null) {
                return "err:bad";
            }
            offers.put(member, offer);
            String memberServer = contacts.serverOf(member);
            if (ContactsStore.FOLLOW_ASK.equals(contacts.store.get(member).follow) || activeGames.get(member).isPresent()
                    || (memberServer != null && config.noJoin(memberServer))) {
                proposeGame(member, offer);
            } else if (offer.server.equals(memberServer)) {
                if (!sendGame(member)) {
                    proposeGame(member, offer);
                }
            } else {
                follow(group, member, offer.server);
            }
        }
        return "ok";
    }

    /** « Inviter dans ma partie » : l'invite a 60 s pour accepter (/partie accepter). */
    private String inviteGame(UUID id, String targetName, Map<String, String> params) {
        Optional<Player> found = server.getPlayer(targetName);
        if (found.isEmpty() || contacts.store.get(found.get().getUniqueId()).invisible) {
            return fail(id, "offline", targetName + " n'est pas en ligne.");
        }
        Player target = found.get();
        UUID to = target.getUniqueId();
        if (to.equals(id)) {
            return fail(id, "self", "Tu ne peux pas t'inviter toi-même.");
        }
        ContactsStore.Profile me = contacts.store.get(id);
        ContactsStore.Profile other = contacts.store.get(to);
        if (me.blocked.contains(to)) {
            return fail(id, "blocked", "Tu as bloqué ce joueur : /debloquer " + target.getUsername());
        }
        if (ContactsStore.MP_FRIENDS.equals(other.invites) && !other.friends.contains(id)) {
            return fail(id, "friends-only", target.getUsername() + " n'accepte que les invitations de ses amis.");
        }
        Offer offer = offer(id, params, true);
        if (offer == null) {
            return fail(id, "bad", "Tu n'es pas dans une partie où l'on peut inviter.");
        }
        contacts.tell(id, info("Invitation envoyée à " + target.getUsername() + " (60 s pour répondre)."));
        if (other.blocked.contains(id)) {
            return "ok"; // bloque par ce joueur : rien n'arrive, et rien ne le lui apprend
        }
        offers.put(to, offer);
        proposeGame(to, offer);
        return "ok";
    }

    /** « /partie accepter » : le joueur va sur le serveur de la partie s'il n'y est pas, puis y entre. */
    private String acceptOffer(UUID id) {
        Offer offer = liveOffer(id);
        if (offer == null) {
            return fail(id, "none", "Aucune partie ne t'est proposée pour le moment.");
        }
        offer.confirmed = true;
        if (offer.server.equals(contacts.serverOf(id))) {
            return sendGame(id) ? "ok" : fail(id, "closed", "Impossible de rejoindre cette partie pour le moment.");
        }
        Optional<Player> player = server.getPlayer(id);
        Optional<RegisteredServer> destination = server.getServer(offer.server);
        if (player.isEmpty() || destination.isEmpty()) {
            return fail(id, "closed", "Impossible de rejoindre cette partie pour le moment.");
        }
        pendingFollow.remove(id);
        player.get().sendMessage(info("Tu rejoins " + name(offer.from) + " sur " + pretty(offer.server) + "."));
        // A l'arrivee (onServerPostConnect), le serveur le fait entrer dans la partie.
        player.get().createConnectionRequest(destination.get()).fireAndForget();
        return "ok";
    }

    private String denyOffer(UUID id) {
        Offer offer = liveOffer(id);
        if (offer == null) {
            return fail(id, "none", "Aucune partie ne t'est proposée pour le moment.");
        }
        offers.remove(id);
        contacts.tell(id, info("Proposition refusée."));
        if (offer.invite) {
            contacts.tell(offer.from, info(name(id) + " a refusé ton invitation."));
        }
        return "ok";
    }

    /**
     * Le serveur du joueur demande la partie ou le faire entrer (reponse au message « game »). En partie ailleurs, en
     * « me demander avant » ou simple invitation : tant qu'il n'a pas accepte, il recoit la proposition et rien ne bouge.
     * Reponse : « ok », puis une ligne « jeu TAB reference ».
     */
    private String gameGet(UUID id, boolean busy) {
        Offer offer = liveOffer(id);
        if (offer == null) {
            return "err:none";
        }
        if (!offer.server.equals(contacts.serverOf(id))) {
            return "err:elsewhere";
        }
        if (!offer.confirmed && (busy || offer.invite || ContactsStore.FOLLOW_ASK.equals(contacts.store.get(id).follow))) {
            if (!offer.proposed) {
                proposeGame(id, offer);
            }
            return "err:busy";
        }
        offers.remove(id);
        joinedAt.put(id, System.currentTimeMillis());
        return "ok\n" + offer.game + "\t" + offer.ref + "\n";
    }

    // ------------------------------------------------------------------ deconnexions

    /** Deconnexion d'un joueur (appele sous verrou par Contacts). */
    void disconnected(UUID id, String username) {
        invites.remove(id);
        pendingFollow.remove(id);
        offers.remove(id);
        joinedAt.remove(id);
        Group group = groupOf.get(id);
        if (group == null) {
            return;
        }
        offlineSince.put(id, System.currentTimeMillis());
        broadcast(group, info(username + " s'est déconnecté (sa place dans le groupe est gardée 5 min)."));
        if (group.leader.equals(id)) {
            newLeader(group);
        }
    }

    /** Toutes les 20 s : membres deconnectes depuis plus de 5 minutes retires, invitations perimees oubliees. */
    private void sweep() {
        synchronized (contacts) {
            long now = System.currentTimeMillis();
            invites.values().removeIf(invite -> invite.expires() <= now);
            offers.values().removeIf(offer -> offer.expires <= now);
            joinedAt.values().removeIf(at -> now - at > 60_000L);
            for (Map.Entry<UUID, Long> entry : new ArrayList<>(offlineSince.entrySet())) {
                if (now - entry.getValue() < OFFLINE_MILLIS) {
                    continue;
                }
                UUID id = entry.getKey();
                offlineSince.remove(id);
                Group group = groupOf.get(id);
                if (group == null || server.getPlayer(id).isPresent()) {
                    continue;
                }
                boolean wasLeader = group.leader.equals(id);
                String gone = name(id);
                remove(group, id);
                broadcast(group, info(gone + " a quitté le groupe (déconnecté depuis 5 min)."));
                if (wasLeader) {
                    newLeader(group);
                }
                dissolveIfAlone(group);
            }
        }
    }
}
