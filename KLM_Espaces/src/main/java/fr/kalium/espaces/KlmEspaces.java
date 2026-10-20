package fr.kalium.espaces;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KLM_Espaces 1.0.0 (demande de LeKiwi06, 21/10/2026) : Kal-Games et Serveur Jeux expirent ; Kal-Games est fusionne
 * dans Kanvas, « tout sera sur le meme serveur, mais de facon hermetique, on fera meme des tchats separes ; les gens ne
 * pourront pas passer de Kanvas a Kal-Games sans le lobby ». Meme principe que les mondes simultanes de Serveur Jeux :
 * un seul serveur Paper, plusieurs groupes de mondes independants.
 *
 * Un espace = un ou plusieurs noms Velocity + des mondes. A la connexion, KaliumRelay (1.9.0) depose sur le relais le
 * nom Velocity demande (cle « arrivee-<uuid> ») : le joueur arrive dans l'espace correspondant. Chaque espace a :
 * - ses inventaires (inventaire, coffre de l'Ender, experience, vie, effets, mode de jeu, position), sauves au depart
 *   du joueur dans plugins/KLM_Espaces/etats/<espace>/<uuid>.yml et rendus a son retour dans l'espace ;
 * - ses joueurs visibles (les autres sont caches, liste Tab comprise) ;
 * - son tchat, ses messages d'arrivee, de depart et de mort.
 * Les autres plugins lisent l'espace d'un joueur dans ses metadonnees « klm_espace » (identifiant) et « klm_serveur »
 * (nom Velocity officiel de l'espace), sans dependre de ce plugin.
 */
public final class KlmEspaces extends JavaPlugin implements Listener {

    /** Un espace : identifiant, noms Velocity (le premier est l'officiel), mondes (nom ou cle, « * » accepte). */
    record Espace(String id, List<String> serveurs, List<String> mondes, String arrivee) {

        boolean contient(World monde) {
            if (monde == null) {
                return false;
            }
            String nom = monde.getName().toLowerCase(Locale.ROOT);
            String cle = monde.getKey().toString().toLowerCase(Locale.ROOT);
            for (String motif : mondes) {
                String m = motif.toLowerCase(Locale.ROOT);
                if (correspond(m, nom) || correspond(m, cle)) {
                    return true;
                }
            }
            return false;
        }

        String serveur() {
            return serveurs.isEmpty() ? id : serveurs.get(0);
        }

        private static boolean correspond(String motif, String texte) {
            return texte.matches(("\\Q" + motif + "\\E").replace("*", "\\E.*\\Q"));
        }
    }

    private final Map<String, Espace> espaces = new LinkedHashMap<>();
    /** Espace de chaque joueur connecte (ou en cours de connexion). */
    private final Map<UUID, String> espaceDe = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final MiniMessage mm = MiniMessage.miniMessage();
    private Etats etats;
    private NamespacedKey cleEspace;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cleEspace = new NamespacedKey(this, "espace");
        etats = new Etats(this);
        charger();
        getServer().getPluginManager().registerEvents(this, this);
        // Rechargement a chaud (/reload, deconseille) : les joueurs deja la gardent l'espace de leur monde.
        for (Player joueur : getServer().getOnlinePlayers()) {
            Espace espace = espaceDuMonde(joueur.getWorld());
            entrer(joueur, espace != null ? espace : parDefaut());
        }
    }

    @Override
    public void onDisable() {
        for (Player joueur : getServer().getOnlinePlayers()) {
            Espace espace = espace(joueur.getUniqueId());
            if (espace != null) {
                etats.sauver(joueur, espace.id());
            }
        }
        etats.ecrireDerniers();
    }

    private void charger() {
        reloadConfig();
        espaces.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("espaces");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                espaces.put(id, new Espace(id, s.getStringList("serveurs"), s.getStringList("mondes"),
                        s.getString("arrivee", "")));
            }
        }
        if (espaces.isEmpty()) {
            getLogger().warning("Aucun espace dans config.yml : tous les joueurs sont dans le meme espace.");
            espaces.put("serveur", new Espace("serveur", List.of(), List.of("*"), ""));
        }
    }

    // ------------------------------------------------------------------ espaces

    private Espace parDefaut() {
        Espace espace = espaces.get(getConfig().getString("espace-par-defaut", ""));
        return espace != null ? espace : espaces.values().iterator().next();
    }

    private Espace parServeur(String serveur) {
        for (Espace espace : espaces.values()) {
            for (String nom : espace.serveurs()) {
                if (nom.equalsIgnoreCase(serveur)) {
                    return espace;
                }
            }
        }
        return null;
    }

    private Espace espaceDuMonde(World monde) {
        for (Espace espace : espaces.values()) {
            if (espace.contient(monde)) {
                return espace;
            }
        }
        return null;
    }

    Espace espace(UUID joueur) {
        String id = espaceDe.get(joueur);
        return id == null ? null : espaces.get(id);
    }

    private boolean memeEspace(Player a, Player b) {
        String ea = espaceDe.get(a.getUniqueId());
        String eb = espaceDe.get(b.getUniqueId());
        return ea == null || eb == null || ea.equals(eb);
    }

    private World mondeArrivee(Espace espace) {
        String nom = espace.arrivee();
        if (nom == null || nom.isBlank()) {
            return null;
        }
        World monde = Bukkit.getWorld(nom);
        if (monde == null) {
            NamespacedKey cle = NamespacedKey.fromString(nom.toLowerCase(Locale.ROOT));
            monde = cle == null ? null : Bukkit.getWorld(cle);
        }
        return monde;
    }

    /** Nom Velocity demande par le joueur, lu sur le relais (null si inconnu ou relais injoignable). */
    private String serveurDemande(UUID joueur) {
        String url = getConfig().getString("relay-url", "");
        String jeton = getConfig().getString("relay-token", "");
        if (url == null || url.isBlank() || jeton == null || jeton.isBlank()) {
            return null;
        }
        try {
            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/assignment/arrivee-" + joueur))
                    .header("X-Kalium-Relay-Token", jeton)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> reponse = http.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return reponse.statusCode() == 200 ? reponse.body().trim() : null;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            getLogger().warning("Relais injoignable (espace de " + joueur + ") : " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ arrivee

    /** Hors du fil principal, avant l'arrivee : espace du joueur (nom demande, sinon dernier espace, sinon defaut). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        UUID id = event.getUniqueId();
        String demande = serveurDemande(id);
        Espace espace = demande == null ? null : parServeur(demande);
        if (espace == null) {
            if (demande != null) {
                getLogger().warning(event.getName() + " demande '" + demande + "', qui n'est l'espace d'aucun : "
                        + "dernier espace ou espace par defaut.");
            }
            espace = espaces.get(etats.dernier(id));
        }
        if (espace == null) {
            espace = parDefaut();
        }
        espaceDe.put(id, espace.id());
        etats.precharger(id, espace.id());
    }

    /**
     * Position d'arrivee : la derniere position du joueur si elle est dans son espace (donnees du serveur), sinon sa
     * position sauvee dans cet espace, sinon le point d'apparition du monde d'arrivee. Les plugins de l'espace
     * (KalGames : hub) peuvent ensuite la changer.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        UUID id = event.getConnection().getProfile().getId();
        Espace espace = id == null ? null : espace(id);
        if (espace == null || espace.contient(event.getSpawnLocation().getWorld())) {
            return;
        }
        Location position = etats.position(id);
        if (position == null || !espace.contient(position.getWorld())) {
            World monde = mondeArrivee(espace);
            position = monde == null ? null : monde.getSpawnLocation().add(0.5, 0, 0.5);
        }
        if (position != null) {
            event.setSpawnLocation(position);
        }
    }

    /** Avant les autres plugins : etat de l'espace rendu (ou mis de cote), puis visibilite. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        Espace espace = espace(joueur.getUniqueId());
        if (espace == null) { // arrive sans passer par la pre-connexion (rare) : espace de son monde
            espace = espaceDuMonde(joueur.getWorld());
            if (espace == null) {
                espace = parDefaut();
            }
            etats.precharger(joueur.getUniqueId(), espace.id());
        }
        entrer(joueur, espace);
    }

    private void entrer(Player joueur, Espace espace) {
        espaceDe.put(joueur.getUniqueId(), espace.id());
        joueur.setMetadata("klm_espace", new FixedMetadataValue(this, espace.id()));
        joueur.setMetadata("klm_serveur", new FixedMetadataValue(this, espace.serveur()));
        // Les donnees du serveur (inventaire...) appartiennent a l'espace marque dans le joueur ; sans marque, a
        // « espace-des-donnees-existantes » (joueurs venus avant KLM_Espaces).
        String marque = joueur.getPersistentDataContainer().get(cleEspace, PersistentDataType.STRING);
        if (marque == null) {
            marque = getConfig().getString("espace-des-donnees-existantes", "");
        }
        if (!espace.id().equals(marque)) {
            // Les donnees du serveur sont l'etat le plus recent de l'autre espace : sauvees pour lui d'abord.
            if (espaces.containsKey(marque)) {
                etats.sauver(joueur, marque);
            }
            if (!etats.rendre(joueur)) {
                Etats.vider(joueur);
            }
        } else {
            etats.oublier(joueur.getUniqueId()); // donnees du serveur deja a jour (plus recentes que la sauvegarde)
        }
        joueur.getPersistentDataContainer().set(cleEspace, PersistentDataType.STRING, espace.id());
        for (Player autre : getServer().getOnlinePlayers()) {
            if (autre == joueur) {
                continue;
            }
            if (memeEspace(joueur, autre)) {
                joueur.showPlayer(this, autre);
                autre.showPlayer(this, joueur);
            } else {
                joueur.hidePlayer(this, autre);
                autre.hidePlayer(this, joueur);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        Espace espace = espace(joueur.getUniqueId());
        if (espace != null) {
            etats.sauver(joueur, espace.id());
        }
        espaceDe.remove(joueur.getUniqueId());
        etats.oublier(joueur.getUniqueId());
        joueur.removeMetadata("klm_espace", this);
        joueur.removeMetadata("klm_serveur", this);
    }

    // ------------------------------------------------------------------ messages

    /** Tchat du serveur : seulement les joueurs du meme espace (la console voit tout). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player auteur = event.getPlayer();
        event.viewers().removeIf(lecteur -> lecteur instanceof Player p && !memeEspace(p, auteur));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinMessage(PlayerJoinEvent event) {
        Component message = event.joinMessage();
        event.joinMessage(null);
        diffuser(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuitMessage(PlayerQuitEvent event) {
        Component message = event.quitMessage();
        event.quitMessage(null);
        diffuser(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeathMessage(PlayerDeathEvent event) {
        Component message = event.deathMessage();
        event.deathMessage(null);
        diffuser(event.getPlayer(), message);
    }

    private void diffuser(Player auteur, Component message) {
        if (message == null) {
            return;
        }
        getServer().getConsoleSender().sendMessage(message);
        for (Player lecteur : getServer().getOnlinePlayers()) {
            if (memeEspace(lecteur, auteur)) {
                lecteur.sendMessage(message);
            }
        }
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("recharger")) {
            charger();
            sender.sendMessage(mm.deserialize(getConfig().getString("messages.recharge", "<green>Rechargé.")));
            return true;
        }
        sender.sendMessage(mm.deserialize(getConfig().getString("messages.liste-titre", "<gold>Espaces :")));
        for (Espace espace : espaces.values()) {
            List<String> noms = new ArrayList<>();
            for (Player joueur : getServer().getOnlinePlayers()) {
                if (espace.id().equals(espaceDe.get(joueur.getUniqueId()))) {
                    noms.add(joueur.getName());
                }
            }
            sender.sendMessage(mm.deserialize(getConfig().getString("messages.liste-ligne",
                            "<yellow><espace></yellow> <gray>(<count>) :</gray> <white><players>"),
                    Placeholder.unparsed("espace", espace.id() + " / " + espace.serveur()),
                    Placeholder.unparsed("count", String.valueOf(noms.size())),
                    Placeholder.unparsed("players", noms.isEmpty() ? "-" : String.join(", ", noms))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("liste", "recharger") : List.of();
    }

    /** La position n'est sauvee que si elle est dans l'espace (sinon : point d'arrivee au retour). */
    boolean dansEspace(String espace, World monde) {
        Espace e = espaces.get(espace);
        return e != null && e.contient(monde);
    }

    File dossierEtats() {
        return new File(getDataFolder(), "etats");
    }
}
