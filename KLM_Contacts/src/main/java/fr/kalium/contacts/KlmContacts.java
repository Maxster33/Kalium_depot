package fr.kalium.contacts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import net.kyori.adventure.text.Component;

/**
 * KLM_Contacts - étape 1 « amis » (demande de LeKiwi06, 09/10/2026 : « un KLM_Contacts pour ajouter nos amis, créer des
 * groupes avec eux, voir dans quels serveurs de KaLium ils sont »). Présent sur chaque serveur Paper : menus et commande
 * /amis. Les données, la présence, les déplacements et les messages privés (/mp, /r) sont sur le proxy (KaliumRelay
 * 1.6.0), voir {@link Relais}.
 *
 * - /amis : ouvre le menu ; /amis ajouter | accepter | refuser | retirer | rejoindre &lt;pseudo&gt;.
 * - /bloquer &lt;pseudo&gt;, /debloquer &lt;pseudo&gt;.
 * - Bouton « Contacts » du comparateur « Informations » (KLM_Menu 2.10.0 ; avec une version plus ancienne, le plugin
 *   fonctionne par ses commandes, sans le bouton).
 * 1.1.0 - étape 2 « groupe de jeu » : les groupes vivent sur le proxy (KaliumRelay 1.7.0), qui fournit aussi les commandes
 * /groupe et /gc ; ici : le menu « Groupe de jeu », le réglage « Suivre le chef » et la réponse au proxy quand il demande
 * si un joueur est en partie avant de le déplacer avec son chef.
 * 1.2.0 - étape 3 « parties » : les plugins de jeu se déclarent ({@link fr.kalium.contacts.api.JeuDeGroupe}) et annoncent
 * les entrées en partie ({@link #annoncer}) ; quand le proxy le demande (message « game »), le joueur entre dans la partie
 * de son chef de groupe ou dans celle où il a été invité. Bouton « Inviter dans ma partie » sur la fiche d'un ami.
 */
public final class KlmContacts extends JavaPlugin implements org.bukkit.event.Listener {

    /** 1.1.0 : canal des messages du proxy (KaliumRelay) vers ce plugin. */
    private static final String CANAL = "kalium:contacts";

    private static final List<String> SOUS_COMMANDES = List.of("ajouter", "accepter", "refuser", "retirer", "rejoindre");

    private Lang lang;
    private Relais relais;
    private Menus menus;
    /** 1.2.0 : nom de CE serveur dans le velocity.toml, demandé une fois au proxy (null tant qu'il n'est pas connu). */
    private String monServeur;
    /** 1.2.0 : instant d'arrivée de chaque joueur sur ce serveur. */
    private final Map<java.util.UUID, Long> arrivees = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        relais = new Relais(this);
        menus = new Menus(this, relais, lang, new Gui(this, lang));
        Component titre = lang.c("section.titre", "<light_purple><bold>Contacts");
        Component description = lang.c("section.description", "<gray>Tes amis, le serveur où ils sont, les demandes d'ami.");
        getServer().getServicesManager().register(MenuSection.class, new MenuSection() {
            @Override
            public String id() {
                return "contacts";
            }

            @Override
            public Plugin owner() {
                return KlmContacts.this;
            }

            @Override
            public Component title() {
                return titre;
            }

            @Override
            public Component description() {
                return description;
            }

            @Override
            public boolean informations() {
                return true;
            }

            @Override
            public int order() {
                return 50;
            }

            @Override
            public void open(Player joueur, Consumer<Player> retour) {
                menus.ouvrir(joueur, retour);
            }
        }, this, ServicePriority.Normal);
        // 1.1.0 (groupe de jeu) : messages du proxy (KaliumRelay 1.7.0) sur le canal kalium:contacts.
        // « follow » : le chef du groupe a changé de serveur ; on répond si le joueur est en partie (il n'est alors pas
        // déplacé d'office). « groupe » : commande /groupe sans rien, ouvrir le menu du groupe.
        getServer().getMessenger().registerIncomingPluginChannel(this, CANAL, (canal, joueur, message) -> {
            String texte = new String(message, java.nio.charset.StandardCharsets.UTF_8);
            if (texte.equals("follow")) {
                relais.appelerEnSilence(joueur, "gfollow", Map.of("busy", String.valueOf(enPartie(joueur))));
            } else if (texte.equals("groupe")) {
                menus.groupe(joueur, null);
            } else if (texte.equals("game")) {
                entrerEnPartie(joueur); // 1.2.0
            }
        });
        getServer().getPluginManager().registerEvents(this, this);
        lang.saveIfNeeded();
        if (getConfig().getString("relay-token", "").isBlank()) {
            getLogger().warning("relay-token est vide dans config.yml : les contacts sont indisponibles tant qu'il n'est pas "
                    + "rempli (même valeur que sur les autres plugins reliés au relais).");
        }
    }

    // ------------------------------------------------------------------ parties (1.2.0, étape 3)

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        arrivees.put(joueur.getUniqueId(), System.currentTimeMillis());
        if (monServeur == null && !getConfig().getString("relay-token", "").isBlank()) {
            // Nom de ce serveur pour le proxy : demandé une fois, quand un joueur est là (2 s après son arrivée).
            getServer().getScheduler().runTaskLater(this, () -> {
                if (monServeur == null && joueur.isOnline()) {
                    relais.appelerEnSilence(joueur, "whereami", Map.of(), reponse -> {
                        if (reponse != null && reponse.ok() && !reponse.detail().isBlank()) {
                            monServeur = reponse.detail();
                        }
                    });
                }
            }, 40L);
        }
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        arrivees.remove(event.getPlayer().getUniqueId());
    }

    private fr.kalium.contacts.api.JeuDeGroupe jeu(String id) {
        for (var service : getServer().getServicesManager().getRegistrations(fr.kalium.contacts.api.JeuDeGroupe.class)) {
            if (service.getPlugin().isEnabled() && service.getProvider().id().equals(id)) {
                return service.getProvider();
            }
        }
        return null;
    }

    /**
     * API pour les plugins de jeu : ce joueur vient d'entrer dans une partie (voir {@link fr.kalium.contacts.api.JeuDeGroupe}).
     * S'il est chef d'un groupe, le proxy y fait entrer ses membres ; sinon rien. Sans effet si le relais n'est pas réglé.
     */
    public void annoncer(Player joueur, String jeu, String reference, String libelle) {
        if (joueur == null || !joueur.isOnline() || getConfig().getString("relay-token", "").isBlank()) {
            return;
        }
        Map<String, String> parametres = new java.util.HashMap<>();
        parametres.put("game", jeu);
        parametres.put("ref", reference);
        parametres.put("label", libelle == null ? "" : libelle);
        if (monServeur != null) {
            parametres.put("server", monServeur);
        }
        relais.appelerEnSilence(joueur, "ggame", parametres);
    }

    /** Partie privée où se trouve ce joueur et où il peut inviter : {jeu, référence, nom affiché}, ou null. */
    String[] partieDe(Player joueur) {
        for (var service : getServer().getServicesManager().getRegistrations(fr.kalium.contacts.api.JeuDeGroupe.class)) {
            try {
                String[] partie = service.getPlugin().isEnabled() ? service.getProvider().partieDe(joueur) : null;
                if (partie != null && partie.length >= 2) {
                    return new String[]{service.getProvider().id(), partie[0], partie[1]};
                }
            } catch (RuntimeException e) {
                getLogger().warning("Partie de " + service.getPlugin().getName() + " illisible : " + e);
            }
        }
        return null;
    }

    /** « Inviter dans ma partie » : le proxy prévient l'invité (60 s pour accepter) et dit le résultat dans le tchat. */
    void inviterEnPartie(Player joueur, String cible) {
        String[] partie = partieDe(joueur);
        if (partie == null) {
            dire(joueur, "partie.aucune", "<red>Tu n'es pas dans une partie privée où l'on peut inviter.");
            return;
        }
        Map<String, String> parametres = new java.util.HashMap<>();
        parametres.put("target", cible);
        parametres.put("game", partie[0]);
        parametres.put("ref", partie[1]);
        parametres.put("label", partie[2]);
        if (monServeur != null) {
            parametres.put("server", monServeur);
        }
        relais.appeler(joueur, "ginvitegame", parametres, reponse -> { });
    }

    /**
     * Message « game » du proxy : une partie attend ce joueur ici (celle de son chef de groupe, ou une invitation
     * acceptée). On demande laquelle au proxy, en disant si le joueur est déjà en partie (il n'est alors pas déplacé
     * tant qu'il n'a pas accepté), puis le plugin du jeu le fait entrer. Juste après son arrivée sur le serveur, on
     * attend la remise à zéro du joueur par le hub.
     */
    private void entrerEnPartie(Player joueur) {
        java.util.UUID id = joueur.getUniqueId();
        long depuis = System.currentTimeMillis() - arrivees.getOrDefault(id, 0L);
        getServer().getScheduler().runTaskLater(this, () -> {
            Player present = getServer().getPlayer(id);
            if (present == null) {
                return;
            }
            relais.appelerEnSilence(present, "ggameget", Map.of("busy", String.valueOf(enPartie(present))), reponse -> {
                if (reponse == null || !reponse.ok() || reponse.lignes().isEmpty()) {
                    return;
                }
                String[] partie = reponse.lignes().get(0).split("\t", -1);
                fr.kalium.contacts.api.JeuDeGroupe jeu = partie.length < 2 ? null : jeu(partie[0].trim());
                if (jeu == null) {
                    dire(present, "partie.introuvable", "<red>Cette partie n'est plus disponible.");
                    return;
                }
                try {
                    jeu.rejoindre(present, partie[1].trim());
                } catch (RuntimeException e) {
                    getLogger().warning("Entrée en partie (" + jeu.owner().getName() + ") impossible : " + e);
                    dire(present, "partie.introuvable", "<red>Cette partie n'est plus disponible.");
                }
            });
        }, depuis < 2000L ? 30L : 1L);
    }

    /** Le joueur est-il dans une partie (salle d'attente, jeu, spectateur) d'un plugin de ce serveur ? */
    private boolean enPartie(Player joueur) {
        for (var service : getServer().getServicesManager().getRegistrations(fr.kalium.menu.api.PlayerActivity.class)) {
            try {
                if (service.getPlugin().isEnabled() && service.getProvider().inGame(joueur)) {
                    return true;
                }
            } catch (RuntimeException e) {
                getLogger().warning("Activité de " + service.getPlugin().getName() + " illisible : " + e);
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        if (lang != null) {
            lang.saveIfNeeded();
        }
    }

    Component prefixe() {
        return lang.c("prefixe", "<light_purple>Contacts » ");
    }

    void dire(Player joueur, String cle, String def, Object... paires) {
        joueur.sendMessage(prefixe().append(lang.c(cle, def, paires)));
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ commandes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "bloquer" -> {
                if (args.length != 1) {
                    dire(joueur, "usage.bloquer", "<gray>Utilisation : <white>/bloquer pseudo");
                } else {
                    agir(joueur, "block", args[0], null);
                }
            }
            case "debloquer" -> {
                if (args.length != 1) {
                    dire(joueur, "usage.debloquer", "<gray>Utilisation : <white>/debloquer pseudo");
                } else {
                    agir(joueur, "unblock", args[0], null);
                }
            }
            default -> {
                if (args.length == 0) {
                    menus.ouvrir(joueur, null);
                } else if (args.length != 2 || !SOUS_COMMANDES.contains(args[0].toLowerCase(Locale.ROOT))) {
                    dire(joueur, "usage.amis",
                            "<gray>Utilisation : <white>/amis</white> (menu), <white>/amis ajouter | accepter | refuser | retirer | rejoindre pseudo");
                } else {
                    switch (args[0].toLowerCase(Locale.ROOT)) {
                        case "ajouter" -> agir(joueur, "add", args[1], null);
                        case "accepter" -> agir(joueur, "accept", args[1], null);
                        case "refuser" -> agir(joueur, "deny", args[1], null);
                        case "retirer" -> agir(joueur, "remove", args[1], null);
                        default -> rejoindre(joueur, args[1]);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> propositions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("amis") && args.length == 1) {
            for (String sous : SOUS_COMMANDES) {
                if (sous.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    propositions.add(sous);
                }
            }
            return propositions;
        }
        return null; // pseudo : joueurs de ce serveur
    }

    // ------------------------------------------------------------------ actions

    /**
     * Action sur un joueur (add, accept, deny, cancel, remove, block, unblock), désigné par son pseudo ou son UUID : le
     * résultat est dit dans le tchat, puis {@code ensuite} est appelé (retour au menu), s'il est fourni.
     */
    void agir(Player joueur, String action, String cible, Consumer<Player> ensuite) {
        relais.appeler(joueur, action, Map.of("target", cible), reponse -> {
            if (reponse == null) {
                return;
            }
            if (reponse.ok()) {
                // « ok:sent:<pseudo> », « ok:friends:<pseudo> » (add, accept) ou « ok:<pseudo> ».
                String detail = reponse.partie(0);
                switch (action) {
                    case "add", "accept" -> {
                        if (detail.equals("friends")) {
                            dire(joueur, "fait.amis", "<green><white><nom></white> et toi êtes maintenant amis.", "nom", reponse.partie(1));
                        } else {
                            dire(joueur, "fait.demande", "<green>Demande d'ami envoyée à <white><nom></white>.", "nom", reponse.partie(1));
                        }
                    }
                    case "deny" -> dire(joueur, "fait.refuse", "<gray>Demande de <white><nom></white> refusée.", "nom", detail);
                    case "cancel" -> dire(joueur, "fait.annule", "<gray>Demande à <white><nom></white> annulée.", "nom", detail);
                    case "remove" -> dire(joueur, "fait.retire", "<gray><white><nom></white> n'est plus dans tes amis.", "nom", detail);
                    case "block" -> dire(joueur, "fait.bloque", "<gray><white><nom></white> est bloqué.", "nom", detail);
                    case "unblock" -> dire(joueur, "fait.debloque", "<gray><white><nom></white> est débloqué.", "nom", detail);
                    default -> {
                    }
                }
            } else {
                refus(joueur, action, reponse.partie(0));
            }
            if (ensuite != null) {
                ensuite.accept(joueur);
            }
        });
    }

    private void refus(Player joueur, String action, String code) {
        switch (code) {
            case "unknown" -> dire(joueur, "refus.inconnu", "<red>Joueur inconnu : il doit être venu au moins une fois sur KaLium.");
            case "self" -> dire(joueur, "refus.soi", "<red>C'est toi.");
            case "already" -> dire(joueur, "refus.deja-ami", "<yellow>Ce joueur est déjà ton ami.");
            case "pending" -> dire(joueur, "refus.deja-demande", "<yellow>Tu lui as déjà envoyé une demande.");
            case "blocked" -> dire(joueur, "refus.bloque", "<red>Tu as bloqué ce joueur : débloque-le d'abord (/debloquer).");
            case "full" -> dire(joueur, "refus.plein", "<red>Ta liste d'amis est pleine.");
            case "target-full" -> dire(joueur, "refus.plein-autre", "<red>Sa liste d'amis est pleine.");
            case "too-many" -> dire(joueur, "refus.trop-demandes", "<red>Trop de demandes en attente : annules-en d'abord.");
            case "none" -> {
                switch (action) {
                    case "remove" -> dire(joueur, "refus.pas-ami", "<red>Ce joueur n'est pas dans tes amis.");
                    case "unblock" -> dire(joueur, "refus.pas-bloque", "<red>Ce joueur n'est pas bloqué.");
                    default -> dire(joueur, "refus.pas-demande", "<red>Aucune demande d'ami avec ce joueur.");
                }
            }
            default -> dire(joueur, "refus.autre", "<red>Action impossible.");
        }
    }

    /** « Rejoindre son serveur » : le proxy déplace le joueur vers le serveur de son ami. */
    void rejoindre(Player joueur, String cible) {
        relais.appeler(joueur, "join", Map.of("target", cible), reponse -> {
            if (reponse == null) {
                return;
            }
            if (reponse.ok()) {
                dire(joueur, "rejoindre.fait", "<gray>Connexion à <white><serveur></white>…", "serveur", menus.serveur(reponse.partie(0)));
                return;
            }
            switch (reponse.partie(0)) {
                case "unknown" -> dire(joueur, "refus.inconnu", "<red>Joueur inconnu : il doit être venu au moins une fois sur KaLium.");
                case "not-friend" -> dire(joueur, "refus.pas-ami", "<red>Ce joueur n'est pas dans tes amis.");
                case "offline" -> dire(joueur, "rejoindre.hors-ligne", "<red>Cet ami n'est pas en ligne.");
                case "same" -> dire(joueur, "rejoindre.meme", "<yellow>Vous êtes déjà sur le même serveur.");
                case "closed" -> dire(joueur, "rejoindre.ferme", "<red>On n'entre pas librement sur <white><serveur></white> : il faut y être invité par une partie.",
                        "serveur", menus.serveur(reponse.partie(1)));
                default -> dire(joueur, "refus.autre", "<red>Action impossible.");
            }
        });
    }
}
