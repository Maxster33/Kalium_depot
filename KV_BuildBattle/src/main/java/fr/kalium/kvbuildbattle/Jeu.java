package fr.kalium.kvbuildbattle;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import fr.kalium.menu.api.Gui;

/**
 * Parties du Build Battle sur Kanvas. Arrivée des joueurs envoyés par KG_BuildBattle : leur affectation est lue sur le
 * relais HTTP (clé « buildbattle-<uuid> ») et ils rejoignent la salle d'attente de leur partie, une par colonne de
 * l'arène (partie privée = même identifiant ; file publique = partie publique de même taille d'équipe qui a encore de
 * la place, sinon une nouvelle). Fait avancer les parties chaque seconde (voir {@link Partie}).
 */
final class Jeu implements Listener {

    /** Thèmes par défaut, si « themes » est absent de config.yml (ajout à la main sur le serveur). */
    static final List<String> THEMES = List.of("Château", "Pirate", "Espace", "Ferme", "Volcan", "Dragon", "Forêt magique",
            "Ville futuriste", "Sous l'eau", "Fête foraine", "Désert", "Robot", "Maison hantée", "Jungle", "Montagne",
            "Hiver", "Cuisine", "Musique", "Sport", "Dinosaures", "Moyen Âge", "Village japonais", "Train",
            "Phare", "Cabane dans les arbres", "Nourriture géante", "Insectes", "Cirque", "Laboratoire", "Bateau",
            "Temple", "Île tropicale", "Pâtisserie", "Ruines", "Nether", "L'End", "Monstre", "Jardin", "Arc-en-ciel",
            "Western");

    private final KVBuildBattle plugin;
    private final Gui gui;
    private final Objets objets;
    private final Inventaires inventaires;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final Map<Integer, Partie> parColonne = new LinkedHashMap<>();
    private final Map<UUID, Partie> parJoueur = new HashMap<>();

    Jeu(KVBuildBattle plugin, Gui gui) {
        this.plugin = plugin;
        this.gui = gui;
        this.objets = new Objets(plugin);
        this.inventaires = new Inventaires(plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::seconde, 20L, 20L);
    }

    Gui gui() {
        return gui;
    }

    Objets objets() {
        return objets;
    }

    List<Partie> parties() {
        return new ArrayList<>(parColonne.values());
    }

    private void seconde() {
        for (Partie p : List.copyOf(parColonne.values())) {
            try {
                p.seconde();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Erreur dans la partie de la colonne " + (p.colonne + 1), e);
            }
        }
    }

    List<String> themesAuHasard(int n) {
        List<String> liste = new ArrayList<>(plugin.getConfig().getStringList("themes"));
        if (liste.isEmpty()) liste.addAll(THEMES);
        Collections.shuffle(liste);
        return new ArrayList<>(liste.subList(0, Math.min(n, liste.size())));
    }

    // --- Arrivée ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinTot(PlayerJoinEvent e) {
        // Inventaire resté de côté (serveur arrêté pendant une partie) : rendu à la connexion.
        inventaires.rendre(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        // Lecture sur le relais hors du fil principal ; quelques essais (le dépôt précède l'envoi, mais par sécurité).
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String affectation = null;
            for (int essai = 0; essai < 3 && affectation == null; essai++) {
                if (essai > 0) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                affectation = lire(id);
            }
            if (affectation == null) return; // joueur venu pour les plots : rien à faire
            String a = affectation;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(id);
                if (p != null) accueillir(p, lireCles(a));
            });
        });
    }

    private String url() {
        // Valeur par défaut dans le code : la clé est absente du config.yml déjà déployé en 0.1.0 (REGLES.md, 3.3).
        return plugin.getConfig().getString("relay-url", "http://7018.mystrator.com:46199");
    }

    private String lire(UUID id) {
        String url = url();
        if (url == null || url.isBlank()) return null;
        try {
            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/assignment/buildbattle-" + id))
                    .header("X-Kalium-Relay-Token", plugin.getConfig().getString("relay-token", ""))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> r = http.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return r.statusCode() == 200 ? r.body() : null;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Partie privée lancée : KG_BuildBattle la retire de sa liste (clé « buildbattle-fermee-<id> »). */
    void publierFermee(String id) {
        String url = url();
        if (url == null || url.isBlank() || id == null) return;
        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create(url + "/assignment/buildbattle-fermee-" + id))
                .header("X-Kalium-Relay-Token", plugin.getConfig().getString("relay-token", ""))
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString("fermee", StandardCharsets.UTF_8))
                .build();
        http.sendAsync(requete, HttpResponse.BodyHandlers.discarding()).exceptionally(e -> {
            plugin.getLogger().warning("Relais injoignable : la partie " + id + " restera listée sur kal-games jusqu'à expiration.");
            return null;
        });
    }

    private static Map<String, String> lireCles(String texte) {
        Map<String, String> m = new HashMap<>();
        for (String ligne : texte.split("\n")) {
            int i = ligne.indexOf('=');
            if (i > 0) m.put(ligne.substring(0, i).trim(), ligne.substring(i + 1).trim());
        }
        return m;
    }

    private static int entier(Map<String, String> m, String cle, int def) {
        try {
            return Integer.parseInt(m.get(cle));
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static int minutes(String tempo) {
        return switch (tempo == null ? "" : tempo.toUpperCase()) {
            case "FAST" -> 3;
            case "LONGUE" -> 10;
            case "EXTRA" -> 30;
            default -> 5;
        };
    }

    void accueillir(Player joueur, Map<String, String> a) {
        sortir(joueur);
        Partie.Type type = "prive".equals(a.get("type")) ? Partie.Type.PRIVE : Partie.Type.PUBLIC;
        int taille = Math.max(1, Math.min(4, entier(a, "tailleEquipes", 1)));
        int equipesMax = Math.max(2, Math.min(plugin.arene().boitesParColonne(), entier(a, "equipesMax", 8)));
        Partie partie = null;
        for (Partie p : parColonne.values()) {
            boolean meme = type == Partie.Type.PRIVE
                    ? p.type == Partie.Type.PRIVE && p.id != null && p.id.equals(a.get("partie"))
                    : p.type == Partie.Type.PUBLIC && p.tailleEquipes == taille;
            if (meme && p.phase == Partie.Phase.ATTENTE && p.joueurs.size() < p.places()) {
                partie = p;
                break;
            }
        }
        if (partie == null && type == Partie.Type.PRIVE && a.get("partie") != null) {
            for (Partie p : parColonne.values()) {
                if (a.get("partie").equals(p.id)) {
                    joueur.sendMessage("§cCette partie a déjà commencé ou est complète.");
                    renvoyer(joueur);
                    return;
                }
            }
        }
        if (partie == null) {
            int colonne = colonneLibre();
            if (colonne < 0) {
                joueur.sendMessage("§cToutes les arènes du Build Battle sont occupées, réessaie dans quelques minutes.");
                renvoyer(joueur);
                return;
            }
            UUID hote = joueur.getUniqueId();
            try {
                if (a.get("hote") != null) hote = UUID.fromString(a.get("hote"));
            } catch (IllegalArgumentException ignore) {
                // hôte illisible : le premier arrivé
            }
            partie = new Partie(plugin, this, colonne, type, a.get("partie"), a.get("code"), hote, taille, equipesMax,
                    minutes(a.get("tempo")), Boolean.parseBoolean(a.get("themesEcrits")));
            parColonne.put(colonne, partie);
        }
        Location l = plugin.arene().apparitionSalle(partie.colonne);
        if (l == null) {
            l = plugin.monde().getSpawnLocation().add(0.5, 0, 0.5);
            joueur.sendMessage("§e(La salle d'attente n'est pas encore capturée.)");
        }
        joueur.teleport(l);
        joueur.setAllowFlight(true);
        inventaires.mettreDeCote(joueur);
        parJoueur.put(joueur.getUniqueId(), partie);
        partie.accueillir(joueur);
    }

    private int colonneLibre() {
        for (int c = 0; c < plugin.arene().colonnes(); c++) {
            if (!parColonne.containsKey(c)) return c;
        }
        return -1;
    }

    /** Le joueur quitte sa partie (s'il en a une) et récupère son inventaire de Kanvas. */
    void sortir(Player joueur) {
        Partie p = parJoueur.remove(joueur.getUniqueId());
        if (p != null) p.retirer(joueur);
        inventaires.rendre(joueur);
    }

    /** Colonne libérée (partie finie, zones vidées). */
    void liberer(Partie p) {
        if (parColonne.get(p.colonne) == p) parColonne.remove(p.colonne);
    }

    /** Renvoie le joueur sur kal-games (canal BungeeCord « Connect »). */
    void renvoyer(Player joueur) {
        ByteArrayOutputStream octets = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(octets)) {
            out.writeUTF("Connect");
            out.writeUTF(plugin.getConfig().getString("serveur-retour", "kal-games"));
        } catch (IOException e) {
            return;
        }
        joueur.sendPluginMessage(plugin, "BungeeCord", octets.toByteArray());
    }

    /** Arrêt du plugin : chacun récupère son inventaire. */
    void toutRendre() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (parJoueur.containsKey(p.getUniqueId())) sortir(p);
        }
    }

    // --- Signalement d'un thème écrit ---

    void signalerTheme(Player signaleur, Partie partie, String theme, UUID auteur) {
        String nomAuteur = auteur == null ? "?" : String.valueOf(Bukkit.getOfflinePlayer(auteur).getName());
        String message = "§c[Build Battle] §f" + signaleur.getName() + " §csignale le thème « §f" + theme + "§c » proposé par §f"
                + nomAuteur + " §7(colonne " + (partie.colonne + 1) + ", " + partie.description() + ")";
        plugin.getLogger().warning(message.replaceAll("§.", ""));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("kvbuildbattle.admin")) p.sendMessage(message);
        }
        File f = new File(plugin.getDataFolder(), "signalements-themes.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        int n = yml.getInt("prochain-id", 1);
        String b = "signalements." + n + ".";
        yml.set(b + "date", LocalDateTime.now().withNano(0).toString());
        yml.set(b + "theme", theme);
        yml.set(b + "auteur", auteur == null ? "" : auteur.toString());
        yml.set(b + "auteur-pseudo", nomAuteur);
        yml.set(b + "signale-par", signaleur.getUniqueId().toString());
        yml.set(b + "signale-par-pseudo", signaleur.getName());
        yml.set(b + "partie", partie.description());
        yml.set("prochain-id", n + 1);
        try {
            yml.save(f);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer signalements-themes.yml", e);
        }
    }

    // --- Événements ---

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        sortir(e.getPlayer());
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        if (!e.getPlayer().getWorld().equals(plugin.monde())) sortir(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Partie p = parJoueur.get(e.getPlayer().getUniqueId());
        if (p == null) return;
        String action = objets.action(e.getItem());
        if (action == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL) return;
        Player j = e.getPlayer();
        switch (action) {
            case Objets.LANCER -> p.lancerParHote(j);
            case Objets.THEME -> p.ouvrirVoteTheme(j);
            case Objets.SIGNALER -> p.ouvrirSignalement(j);
            default -> {
                if (action.startsWith(Objets.NOTE)) {
                    try {
                        p.noter(j, Integer.parseInt(action.substring(Objets.NOTE.length())));
                    } catch (NumberFormatException ignore) {
                        // objet abîmé
                    }
                }
            }
        }
    }

    /** Hors construction, l'inventaire est figé (objets de la salle, du thème et du vote). */
    private boolean fige(Player joueur) {
        Partie p = parJoueur.get(joueur.getUniqueId());
        return p != null && p.phase != Partie.Phase.CONSTRUCTION;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player j && fige(j)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player j && fige(j)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (fige(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (fige(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player j && fige(j)) e.setCancelled(true);
    }

    /** Pendant la construction on reste dans sa boîte ; pendant les votes et les résultats, dans la boîte montrée. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        Partie p = parJoueur.get(e.getPlayer().getUniqueId());
        if (p == null) return;
        int boite = p.boiteImposee(e.getPlayer().getUniqueId());
        if (boite < 0 || plugin.arene().dansBoite(p.colonne, boite, e.getTo())) return;
        Location retour = plugin.arene().apparitionBoite(p.colonne, boite);
        if (retour != null) e.setTo(retour);
    }
}
