package fr.kalium.parrainage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.menu.api.Lang;
import fr.kalium.rewardsgui.KSRewardsGUI;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;

/**
 * KS_Parrainage (Kixster) - demande de LeKiwi06, 23/10/2026 (message d'annonce du parrainage, barème validé le même jour) :
 * « pour chaque joueur qui vient grâce à vous, vous recevez tous les 2 des récompenses sur Kixster SMP [...] plus vos
 * invités jouent, plus vous recevrez de grosses récompenses » ; filleul « automatiquement whitelisté sur Kixster ».
 *
 * Les parrainages et les demandes de liste blanche sont gardés par le proxy (KaliumRelay 1.10.0) et se font dans les
 * menus de KLM_Contacts (sur tous les serveurs). Ce plugin, sur Kixster :
 * - liste blanche : à la connexion d'un joueur qui n'y est pas, demande au proxy s'il a été accepté (parrainage, ou
 *   demande acceptée par un admin) ; si oui, il entre et il est ajouté à la liste blanche, puis le proxy est prévenu
 *   (« wapplique »). La liste blanche de ce serveur (et ses opérateurs) est envoyée au proxy au démarrage, toutes les
 *   5 minutes et après chaque ajout (« wsync ») : c'est elle qui lui fait refuser l'entrée et ouvrir la demande.
 * - temps de jeu ACTIF de chaque filleul (pas d'AFK : proposition de Claude validée par LeKiwi06 le 23/10/2026), compté minute par minute ;
 *   à chaque palier de config.yml, un coffre « Non ouvert » pour le filleul et un pour le parrain (KS_RewardsGUI 1.6.0,
 *   /rewards). Un coffre qui n'a pas pu être déposé est réessayé chaque minute.
 * - copie du temps de jeu et des paliers envoyée au proxy chaque minute, pour le menu « Mes filleuls ».
 *
 * Données : plugins/KS_Parrainage/filleuls.yml.
 */
public final class KSParrainage extends JavaPlugin implements Listener {

    /** Un filleul (parrainage accepté) : temps actif compté ici, nombre de paliers atteints. */
    private static final class Filleul {
        String nom;
        UUID parrain;
        String nomParrain;
        long secondes;
        int palier;
    }

    /** Un coffre à déposer (réessayé tant que KS_RewardsGUI le refuse). */
    private record Du(UUID joueur, String nom, String raison, double valeur) {
    }

    private record Palier(double heures, double parrain, double filleul) {
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final Map<UUID, Filleul> filleuls = new LinkedHashMap<>();
    private final List<Du> dus = new ArrayList<>();
    private final List<Palier> paliers = new ArrayList<>();
    /** Dernière action de chaque joueur connecté (millisecondes). */
    private final Map<UUID, Long> activite = new HashMap<>();
    /** Joueurs acceptés par le proxy, en cours de connexion : à laisser entrer et à ajouter à la liste blanche. */
    private final Set<UUID> aAjouter = ConcurrentHashMap.newKeySet();
    private File fichier;
    private Lang lang;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        fichier = new File(getDataFolder(), "filleuls.yml");
        lirePaliers();
        charger();
        getServer().getPluginManager().registerEvents(this, this);
        if (relayUrl().isBlank() || relayToken().isBlank()) {
            getLogger().warning("relay-url ou relay-token vide dans config.yml : parrainage et liste blanche automatique "
                    + "inactifs.");
        }
        // Liste blanche envoyée au proxy 5 s après le démarrage, puis toutes les 5 minutes.
        getServer().getScheduler().runTaskTimer(this, this::envoyerListe, 100L, 6000L);
        // Filleuls (relus sur le proxy), temps actif, paliers, copie pour le proxy : chaque minute.
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::relireFilleuls, 200L, 1200L);
        getServer().getScheduler().runTaskTimer(this, this::compter, 1200L, 1200L);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        sauver();
    }

    // ------------------------------------------------------------------ configuration et données

    private String relayUrl() {
        return getConfig().getString("relay-url", "");
    }

    private String relayToken() {
        return getConfig().getString("relay-token", "");
    }

    private void lirePaliers() {
        paliers.clear();
        for (Map<?, ?> m : getConfig().getMapList("paliers")) {
            try {
                paliers.add(new Palier(((Number) m.get("heures")).doubleValue(), ((Number) m.get("parrain")).doubleValue(),
                        ((Number) m.get("filleul")).doubleValue()));
            } catch (RuntimeException e) {
                getLogger().warning("Palier illisible dans config.yml : " + m);
            }
        }
        paliers.sort((a, b) -> Double.compare(a.heures(), b.heures()));
    }

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection section = yaml.getConfigurationSection("filleuls");
        if (section != null) {
            for (String cle : section.getKeys(false)) {
                try {
                    Filleul f = new Filleul();
                    f.nom = section.getString(cle + ".nom", "?");
                    f.parrain = UUID.fromString(section.getString(cle + ".parrain", ""));
                    f.nomParrain = section.getString(cle + ".nom-parrain", "?");
                    f.secondes = section.getLong(cle + ".secondes");
                    f.palier = section.getInt(cle + ".palier");
                    filleuls.put(UUID.fromString(cle), f);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Filleul illisible dans filleuls.yml : " + cle);
                }
            }
        }
        for (Map<?, ?> m : yaml.getMapList("dus")) {
            try {
                dus.add(new Du(UUID.fromString(String.valueOf(m.get("joueur"))), String.valueOf(m.get("nom")),
                        String.valueOf(m.get("raison")), ((Number) m.get("valeur")).doubleValue()));
            } catch (RuntimeException e) {
                getLogger().warning("Coffre à déposer illisible dans filleuls.yml : " + m);
            }
        }
    }

    private void sauver() {
        if (fichier == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        filleuls.forEach((id, f) -> {
            String cle = "filleuls." + id;
            yaml.set(cle + ".nom", f.nom);
            yaml.set(cle + ".parrain", f.parrain.toString());
            yaml.set(cle + ".nom-parrain", f.nomParrain);
            yaml.set(cle + ".secondes", f.secondes);
            yaml.set(cle + ".palier", f.palier);
        });
        List<Map<String, Object>> liste = new ArrayList<>();
        for (Du d : dus) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("joueur", d.joueur().toString());
            m.put("nom", d.nom());
            m.put("raison", d.raison());
            m.put("valeur", d.valeur());
            liste.add(m);
        }
        yaml.set("dus", liste);
        try {
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().warning("Écriture de filleuls.yml impossible : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ relais

    /** POST /contacts/&lt;action&gt; (hors du fil principal) : réponse, ou null si le relais est injoignable. */
    private String appeler(String action, Map<String, String> parametres) {
        if (relayUrl().isBlank() || relayToken().isBlank()) {
            return null;
        }
        StringBuilder corps = new StringBuilder();
        parametres.forEach((cle, valeur) -> corps.append(cle).append('=')
                .append(valeur.replace('\n', ' ').replace('\r', ' ')).append('\n'));
        HttpRequest requete = HttpRequest.newBuilder(URI.create(relayUrl() + "/contacts/" + action))
                .header("X-Kalium-Relay-Token", relayToken())
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.ofString(corps.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> reponse = http.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (reponse.statusCode() != 200) {
                getLogger().warning("Relais : « " + action + " » refusé (code " + reponse.statusCode() + ").");
                return null;
            }
            return reponse.body();
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void appelerPlusTard(String action, Map<String, String> parametres) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> appeler(action, parametres));
    }

    /** Liste blanche et opérateurs de ce serveur, envoyés au proxy (lue sur le fil principal, envoyée en arrière-plan). */
    private void envoyerListe() {
        StringBuilder liste = new StringBuilder();
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) {
            ids.add(p.getUniqueId());
        }
        for (OfflinePlayer p : Bukkit.getOperators()) {
            ids.add(p.getUniqueId());
        }
        for (UUID id : ids) {
            if (liste.length() > 0) {
                liste.append(',');
            }
            liste.append(id);
        }
        appelerPlusTard("wsync", Map.of("active", String.valueOf(Bukkit.hasWhitelist()), "list", liste.toString()));
    }

    // ------------------------------------------------------------------ liste blanche

    /** Joueur absent de la liste blanche : le proxy l'a-t-il accepté (parrainage, demande) ? Fil de connexion. */
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID id = event.getUniqueId();
        if (!Bukkit.hasWhitelist()) {
            return;
        }
        OfflinePlayer joueur = Bukkit.getOfflinePlayer(id);
        if (joueur.isWhitelisted() || joueur.isOp()) {
            return;
        }
        String reponse = appeler("wcheck", Map.of("target", id.toString()));
        if (reponse != null && reponse.trim().equals("ok:true")) {
            aAjouter.add(id);
        }
    }

    /** Refusé par la liste blanche mais accepté par le proxy : il entre (sauf s'il est banni de ce serveur). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onValidate(PlayerConnectionValidateLoginEvent event) {
        if (event.isAllowed()) {
            return;
        }
        com.destroystokyo.paper.profile.PlayerProfile profil = event.getConnection() instanceof PlayerLoginConnection l
                ? l.getAuthenticatedProfile()
                : event.getConnection() instanceof PlayerConfigurationConnection c ? c.getProfile() : null;
        UUID id = profil == null ? null : profil.getId();
        if (id != null && aAjouter.contains(id) && !Bukkit.getOfflinePlayer(id).isBanned()) {
            event.allow();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        UUID id = joueur.getUniqueId();
        activite.put(id, System.currentTimeMillis());
        if (aAjouter.remove(id)) {
            joueur.setWhitelisted(true);
            getLogger().info(joueur.getName() + " ajouté à la liste blanche (accepté sur le proxy : parrainage ou demande).");
            appelerPlusTard("wapplique", Map.of("target", id.toString()));
            envoyerListe();
            Filleul f = filleuls.get(id);
            getServer().getScheduler().runTaskLater(this, () -> {
                if (!joueur.isOnline()) {
                    return;
                }
                joueur.sendMessage(f == null
                        ? lang.c("bienvenue", "<green>Bienvenue sur Kixster ! <gray>Tu es maintenant sur la liste blanche.")
                        : lang.c("bienvenue-filleul", "<green>Bienvenue sur Kixster ! <gray>Ton parrain : <white><parrain></white>. "
                        + "Joue pour gagner vos coffres de récompense (/rewards).", "parrain", f.nomParrain));
                lang.saveIfNeeded();
            }, 60L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activite.remove(event.getPlayer().getUniqueId());
        aAjouter.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ activité (temps actif)

    private void actif(Player joueur) {
        activite.put(joueur.getUniqueId(), System.currentTimeMillis());
    }

    /** Tourner la tête (un bassin AFK déplace le joueur sans la faire tourner). */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getYaw() != event.getTo().getYaw() || event.getFrom().getPitch() != event.getTo().getPitch()) {
            actif(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        actif(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        actif(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        actif(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        actif(event.getPlayer());
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        getServer().getScheduler().runTask(this, () -> activite.put(id, System.currentTimeMillis()));
    }

    // ------------------------------------------------------------------ filleuls et paliers

    /** Relit les parrainages acceptés sur le proxy (hors du fil principal), puis les applique sur le fil principal. */
    private void relireFilleuls() {
        String reponse = appeler("pkixster", Map.of());
        if (reponse == null || !reponse.startsWith("ok")) {
            return;
        }
        List<String[]> lignes = new ArrayList<>();
        for (String ligne : reponse.split("\n")) {
            String[] c = ligne.split("\t", -1);
            if (c.length >= 5 && c[0].equals("A")) {
                lignes.add(c);
            }
        }
        getServer().getScheduler().runTask(this, () -> {
            boolean change = false;
            for (String[] c : lignes) {
                try {
                    UUID id = UUID.fromString(c[1]);
                    Filleul f = filleuls.get(id);
                    if (f == null) {
                        f = new Filleul();
                        filleuls.put(id, f);
                        change = true;
                    }
                    f.nom = c[2];
                    f.parrain = UUID.fromString(c[3]);
                    f.nomParrain = c[4].trim();
                } catch (IllegalArgumentException e) {
                    // ligne abimée : ignorée
                }
            }
            if (change) {
                sauver();
            }
        });
    }

    /** Chaque minute : temps actif des filleuls connectés, paliers atteints, coffres à déposer, copie pour le proxy. */
    private void compter() {
        long maintenant = System.currentTimeMillis();
        long delai = Math.max(1, getConfig().getLong("inactivite-minutes", 5)) * 60_000L;
        boolean change = false;
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            Filleul f = filleuls.get(joueur.getUniqueId());
            if (f == null || f.parrain == null || maintenant - activite.getOrDefault(joueur.getUniqueId(), 0L) > delai) {
                continue;
            }
            f.secondes += 60;
            f.nom = joueur.getName();
            change = true;
            while (f.palier < paliers.size() && f.secondes >= paliers.get(f.palier).heures() * 3600) {
                Palier p = paliers.get(f.palier);
                String heures = nombre(p.heures());
                dus.add(new Du(joueur.getUniqueId(), f.nom, lang.raw("raison-filleul", "Parrainage : <heures> h de jeu sur Kixster")
                        .replace("<heures>", heures), p.filleul()));
                dus.add(new Du(f.parrain, f.nomParrain, lang.raw("raison-parrain", "Parrainage : <filleul> a joué <heures> h")
                        .replace("<filleul>", f.nom).replace("<heures>", heures), p.parrain()));
                f.palier++;
                getLogger().info("Parrainage : " + f.nom + " (filleul de " + f.nomParrain + ") atteint le palier de "
                        + heures + " h.");
            }
        }
        // Coffres à déposer (réessayés tant que KS_RewardsGUI les refuse, ex. fusion.yml absent).
        int avant = dus.size();
        dus.removeIf(d -> d.valeur() <= 0
                || KSRewardsGUI.deposerCoffre(d.joueur(), d.nom(), "Parrainage", d.raison(), d.valeur()));
        if (change || dus.size() != avant) {
            sauver();
        }
        if (avant > 0 && !dus.isEmpty() && dus.size() == avant) {
            getLogger().warning(dus.size() + " coffre(s) de parrainage en attente : KS_RewardsGUI les refuse (fusion.yml "
                    + "présent ?).");
        }
        // Copie pour le menu « Mes filleuls » (KLM_Contacts).
        StringBuilder data = new StringBuilder();
        filleuls.forEach((id, f) -> {
            if (data.length() > 0) {
                data.append(',');
            }
            data.append(id).append(':').append(f.secondes).append(':').append(f.palier);
        });
        StringBuilder table = new StringBuilder();
        for (Palier p : paliers) {
            if (table.length() > 0) {
                table.append(',');
            }
            table.append(nombre(p.heures())).append(':').append(nombre(Math.max(p.parrain(), p.filleul())));
        }
        appelerPlusTard("pjeu", Map.of("data", data.toString(), "paliers", table.toString()));
        lang.saveIfNeeded();
    }

    private static String nombre(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("recharger")) {
            reloadConfig();
            lirePaliers();
            sender.sendMessage("KS_Parrainage : config.yml relu (" + paliers.size() + " paliers).");
            return true;
        }
        sender.sendMessage("KS_Parrainage : " + filleuls.size() + " filleul(s), " + paliers.size() + " palier(s), "
                + dus.size() + " coffre(s) en attente de dépôt ; liste blanche " + (Bukkit.hasWhitelist() ? "active" : "inactive")
                + ". /ksparrainage recharger : relire config.yml.");
        for (Map.Entry<UUID, Filleul> e : filleuls.entrySet()) {
            Filleul f = e.getValue();
            sender.sendMessage("- " + f.nom + " (parrain " + f.nomParrain + ") : " + (f.secondes / 3600) + " h "
                    + String.format("%02d", f.secondes % 3600 / 60) + " actives, " + f.palier + " palier(s)");
        }
        return true;
    }
}
