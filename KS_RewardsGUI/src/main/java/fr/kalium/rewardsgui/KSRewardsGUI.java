package fr.kalium.rewardsgui;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_RewardsGUI (cahier des charges : catégorie 4 « Récompenses », LeKiwi06, 30/09/2026) : réception sur Event des
 * récompenses envoyées par les autres serveurs (KG_Rewards, KV_Rewards...), /rewards pour les récupérer, journal des
 * récupérations (anti-triche, catégorie 6).
 *
 * - Transport : boîte aux lettres « event » de KaliumRelay 1.3.0 (durable : un message reste tant qu'il n'est pas
 *   confirmé). Toutes les 30 secondes, les messages sont lus, enregistrés (recompenses.yml), puis confirmés.
 * - Format d'un message (YAML) : voir Recompense.
 * - /rewards (et bouton « Récompenses » de KS_Menu) : récompenses en attente (origine, raison, contenu) ; Récupérer /
 *   Tout récupérer ; refus si l'inventaire manque de place ; pas d'expiration.
 * - Journal : plugins/KS_RewardsGUI/recuperations.log (date, joueur, origine, raison, contenu).
 * - 1.1.0 (catégorie 7) : deposer(...) : un plugin d'Event dépose directement une récompense en objets (KS_CoffreMort :
 *   coffre de mort envoyé dans /rewards).
 * - 1.2.0 : une récompense venue d'un autre serveur est remise dans un coffre « Non ouvert » au contenu caché (Coffres) ;
 *   un dépôt local (coffre de mort) est toujours rendu en objets, comme avant.
 */
public final class KSRewardsGUI extends JavaPlugin implements Listener {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Joueur -> récompenses en attente (id du message -> récompense), dans l'ordre d'arrivée. */
    private final Map<UUID, LinkedHashMap<String, Recompense>> enAttente = new LinkedHashMap<>();
    /** Ids des messages déjà reçus (évite un doublon si la confirmation a échoué). */
    private final Set<String> recus = new LinkedHashSet<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static KSRewardsGUI instance;
    private File fichier;
    private Lang lang;
    private Gui gui;
    private Menu menu;
    private Coffres coffres;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "recompenses.yml");
        charger();
        coffres = new Coffres(this);
        menu = new Menu(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(coffres, this);
        getServer().getPluginManager().registerEvents(new Contenant.Ecoute(), this);
        getCommand("rewards").setExecutor(this);
        // Bouton « Récompenses » du comparateur « Informations » de KLM_Menu 2.6.0 (s'il est présent).
        if (getServer().getPluginManager().getPlugin("KLM_Menu") != null) {
            getServer().getServicesManager().register(fr.kalium.menu.api.Recompenses.class, new fr.kalium.menu.api.Recompenses() {
                public org.bukkit.plugin.Plugin owner() {
                    return KSRewardsGUI.this;
                }

                public void ouvrir(Player joueur) {
                    menu.ouvrir(joueur);
                }
            }, this, org.bukkit.plugin.ServicePriority.Normal);
        }
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "rewards", lang.c("bouton.nom", "<light_purple><bold>Récompenses"),
                    lang.c("bouton.description", "<gray>Récompenses gagnées sur les autres serveurs"), 30, menu::ouvrir);
        }
        if (relayUrl().isBlank() || relayToken().isBlank()) {
            getLogger().warning("relay-url ou relay-token vide dans config.yml : aucune récompense ne sera reçue.");
        }
        long intervalle = Math.max(10, getConfig().getLong("intervalle-secondes", 30)) * 20;
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::relever, 100L, intervalle);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        if (coffres != null) {
            coffres.fermerTout();
            coffres.sauver();
        }
        sauver();
    }

    Lang lang() {
        return lang;
    }

    Gui gui() {
        return gui;
    }

    Coffres coffres() {
        return coffres;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            menu.ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    // ------------------------------------------------------------------ relais

    private String relayUrl() {
        String url = getConfig().getString("relay-url", "");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String relayToken() {
        return getConfig().getString("relay-token", "");
    }

    private String boite() {
        return getConfig().getString("boite", "event");
    }

    /** Lit la boîte aux lettres (hors du fil principal), enregistre les récompenses, puis les confirme. */
    private void relever() {
        if (relayUrl().isBlank() || relayToken().isBlank()) {
            return;
        }
        String liste;
        try {
            HttpResponse<String> reponse = http.send(requete("").GET().build(), HttpResponse.BodyHandlers.ofString());
            if (reponse.statusCode() != 200) {
                getLogger().warning("Relais : réponse " + reponse.statusCode() + " à la lecture de la boîte.");
                return;
            }
            liste = reponse.body();
        } catch (IOException | InterruptedException e) {
            return;
        }
        List<String> aConfirmer = new ArrayList<>();
        for (String ligne : liste.split("\n")) {
            int tab = ligne.indexOf('\t');
            if (tab <= 0) {
                continue;
            }
            String id = ligne.substring(0, tab);
            String corps = new String(Base64.getDecoder().decode(ligne.substring(tab + 1).trim()), StandardCharsets.UTF_8);
            synchronized (this) {
                if (!recus.contains(id)) {
                    Recompense r = Recompense.lire(corps);
                    if (r == null) {
                        getLogger().warning("Message illisible ignoré : " + id);
                    } else {
                        enAttente.computeIfAbsent(r.joueur, u -> new LinkedHashMap<>()).put(id, r);
                        Player enLigne = Bukkit.getPlayer(r.joueur);
                        if (enLigne != null) {
                            Bukkit.getScheduler().runTask(this, () -> enLigne.sendMessage(lang.c("arrivee",
                                    "<light_purple>Nouvelle récompense : <raison>. <gray>/rewards pour la récupérer.",
                                    "raison", r.raison)));
                        }
                    }
                    recus.add(id);
                }
            }
            aConfirmer.add(id);
        }
        if (aConfirmer.isEmpty()) {
            return;
        }
        sauver();
        for (String id : aConfirmer) {
            try {
                http.send(requete("/" + id).DELETE().build(), HttpResponse.BodyHandlers.discarding());
            } catch (IOException | InterruptedException e) {
                return; // reconfirmé au prochain passage (doublon évité par « recus »)
            }
        }
    }

    private HttpRequest.Builder requete(String suite) {
        return HttpRequest.newBuilder(URI.create(relayUrl() + "/mail/" + boite() + suite))
                .timeout(Duration.ofSeconds(10)).header("X-Kalium-Relay-Token", relayToken());
    }

    // ------------------------------------------------------------------ récompenses en attente

    /**
     * 1.0.1 (anti-triche) : pour chaque joueur qui a des récompenses en attente, la date d'envoi de la plus ancienne
     * (millisecondes).
     */
    public synchronized Map<UUID, Long> plusAnciennesEnAttente() {
        Map<UUID, Long> r = new LinkedHashMap<>();
        enAttente.forEach((joueur, liste) -> liste.values().forEach(rec -> {
            // 1.1.0 : un dépôt local (coffre de mort) qui attend n'a rien de suspect.
            if (!rec.locale) {
                r.merge(joueur, rec.date, Math::min);
            }
        }));
        return r;
    }

    /**
     * 1.4.1 (modération) : une récompense en attente : date d'envoi, origine, raison, nombre d'éléments de son contenu,
     * et si c'est un dépôt local (objets du joueur lui-même, ex. coffre de mort).
     */
    public record EnAttente(long date, String origine, String raison, int elements, boolean locale) {
    }

    /**
     * 1.4.1 (modération : fiche d'un joueur de KS_AntiCheat 1.3.0) : récompenses en attente de ce joueur, en ligne ou
     * hors ligne, de la plus ancienne à la plus récente. Lecture seule : le contenu n'est ni montré ni touché.
     */
    public synchronized List<EnAttente> recompensesEnAttente(UUID joueur) {
        List<EnAttente> r = new ArrayList<>();
        for (Recompense rec : enAttente.getOrDefault(joueur, new LinkedHashMap<>()).values()) {
            r.add(new EnAttente(rec.date, rec.origine, rec.raison, rec.contenu.size(), rec.locale));
        }
        return r;
    }

    /**
     * 1.1.0 : dépose une récompense en objets pour un joueur, sans passer par le relais (fil principal). Elle apparaît
     * dans /rewards comme les autres ; 36 piles au plus (elle doit tenir dans un inventaire vide). Faux si rien n'a été
     * déposé (plugin désactivé, aucun objet).
     */
    public static boolean deposer(UUID joueur, String nom, String origine, String raison, List<org.bukkit.inventory.ItemStack> objets) {
        if (instance == null || !instance.isEnabled() || objets.isEmpty() || objets.size() > 36) {
            return false;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("joueur", joueur.toString());
        yaml.set("nom", nom);
        yaml.set("origine", origine);
        yaml.set("raison", raison);
        yaml.set("date", System.currentTimeMillis());
        yaml.set("locale", true);
        List<Map<String, Object>> contenu = new ArrayList<>();
        for (org.bukkit.inventory.ItemStack objet : objets) {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("type", "objet");
            element.put("donnees", Base64.getEncoder().encodeToString(objet.serializeAsBytes()));
            element.put("nombre", objet.getAmount());
            contenu.add(element);
        }
        yaml.set("contenu", contenu);
        Recompense r = Recompense.lire(yaml.saveToString());
        if (r == null) {
            return false;
        }
        synchronized (instance) {
            instance.enAttente.computeIfAbsent(joueur, u -> new LinkedHashMap<>()).put("local-" + UUID.randomUUID(), r);
        }
        instance.sauver();
        return true;
    }

    synchronized List<Map.Entry<String, Recompense>> enAttente(UUID joueur) {
        return new ArrayList<>(enAttente.getOrDefault(joueur, new LinkedHashMap<>()).entrySet());
    }

    /** Retire une récompense récupérée et l'écrit dans le journal. */
    synchronized void recuperee(Player joueur, String id, Recompense r) {
        LinkedHashMap<String, Recompense> liste = enAttente.get(joueur.getUniqueId());
        if (liste == null || liste.remove(id) == null) {
            return;
        }
        if (liste.isEmpty()) {
            enAttente.remove(joueur.getUniqueId());
        }
        sauver();
        journal(joueur.getName() + " (" + joueur.getUniqueId() + ") | " + r.origine + " | " + r.raison + " | "
                + r.resume());
    }

    /** Une ligne datée dans recuperations.log (récupérations, et ouvertures de coffres depuis la 1.2.0). */
    void journal(String ligne) {
        try (PrintWriter journal = new PrintWriter(new FileWriter(new File(getDataFolder(), "recuperations.log"),
                StandardCharsets.UTF_8, true))) {
            journal.println(LocalDateTime.now(ZoneId.of("Europe/Paris")).format(DATE) + " | " + ligne);
        } catch (IOException e) {
            getLogger().warning("Journal des récupérations : " + e.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        int n = enAttente(event.getPlayer().getUniqueId()).size();
        if (n > 0) {
            Bukkit.getScheduler().runTaskLater(this, () -> event.getPlayer().sendMessage(lang.c("en-attente",
                    "<light_purple>Tu as <nombre> récompense(s) à récupérer : <gray>/rewards", "nombre", n)), 60L);
        }
    }

    // ------------------------------------------------------------------ enregistrement

    private synchronized void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        recus.addAll(yaml.getStringList("recus"));
        ConfigurationSection joueurs = yaml.getConfigurationSection("en-attente");
        if (joueurs == null) {
            return;
        }
        for (String cle : joueurs.getKeys(false)) {
            ConfigurationSection messages = joueurs.getConfigurationSection(cle);
            if (messages == null) {
                continue;
            }
            for (String id : messages.getKeys(false)) {
                Recompense r = Recompense.lire(messages.getString(id, ""));
                if (r != null) {
                    enAttente.computeIfAbsent(r.joueur, u -> new LinkedHashMap<>()).put(id, r);
                }
            }
        }
    }

    synchronized void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> derniers = new ArrayList<>(recus);
        yaml.set("recus", derniers.subList(Math.max(0, derniers.size() - 5000), derniers.size()));
        enAttente.forEach((joueur, liste) -> liste.forEach((id, r) -> yaml.set("en-attente." + joueur + "." + id,
                r.texte)));
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer recompenses.yml : " + e.getMessage());
        }
    }

    /** Lecture tolérante d'un YAML (null si illisible). */
    static YamlConfiguration yaml(String texte) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(texte);
            return yaml;
        } catch (InvalidConfigurationException e) {
            return null;
        }
    }
}
