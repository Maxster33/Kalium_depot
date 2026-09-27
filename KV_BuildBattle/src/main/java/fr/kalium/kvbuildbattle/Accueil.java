package fr.kalium.kvbuildbattle;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Arrivée des joueurs envoyés par KG_BuildBattle (kal-games). À la connexion, l'affectation du joueur est lue sur le
 * relais HTTP (clé « buildbattle-<uuid> ») : file publique (taille d'équipe) ou partie privée (identifiant et
 * réglages de l'hôte). Le joueur rejoint la salle d'attente de sa partie, une par colonne de l'arène : une partie
 * publique de même taille d'équipe qui a encore de la place, sinon une nouvelle partie sur une colonne libre.
 * Le lancement de la partie (compte à rebours, thème, construction...) viendra à l'étape 2.
 */
final class Accueil implements Listener {

    enum Type { PUBLIC, PRIVE }

    /** Une partie en salle d'attente, sur une colonne de l'arène. */
    static final class Salon {
        final int colonne;
        final Type type;
        final String partie; // identifiant de la partie privée (null en public)
        final String code;
        final UUID hote;
        final int tailleEquipes, equipesMax;
        final String tempo;
        final boolean themesEcrits;
        final Set<UUID> joueurs = new LinkedHashSet<>();

        Salon(int colonne, Type type, String partie, String code, UUID hote, int tailleEquipes, int equipesMax,
              String tempo, boolean themesEcrits) {
            this.colonne = colonne;
            this.type = type;
            this.partie = partie;
            this.code = code;
            this.hote = hote;
            this.tailleEquipes = tailleEquipes;
            this.equipesMax = equipesMax;
            this.tempo = tempo;
            this.themesEcrits = themesEcrits;
        }

        int places() {
            return tailleEquipes * equipesMax;
        }

        String mode() {
            String[] noms = {"Solo", "Duo", "Trio", "Squad"};
            return noms[Math.max(1, Math.min(4, tailleEquipes)) - 1];
        }
    }

    private final KVBuildBattle plugin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final Map<Integer, Salon> parColonne = new HashMap<>();
    private final Map<UUID, Salon> parJoueur = new HashMap<>();

    Accueil(KVBuildBattle plugin) {
        this.plugin = plugin;
    }

    List<Salon> salons() {
        return new ArrayList<>(parColonne.values());
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

    private String lire(UUID id) {
        // Valeur par défaut dans le code : la clé est absente du config.yml déjà déployé en 0.1.0 (REGLES.md, 3.3).
        String url = plugin.getConfig().getString("relay-url", "http://7018.mystrator.com:46199");
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

    void accueillir(Player joueur, Map<String, String> a) {
        quitter(joueur.getUniqueId());
        Type type = "prive".equals(a.get("type")) ? Type.PRIVE : Type.PUBLIC;
        int taille = Math.max(1, Math.min(4, entier(a, "tailleEquipes", 1)));
        int equipesMax = Math.max(2, Math.min(plugin.arene().boitesParColonne(), entier(a, "equipesMax", 8)));
        Salon salon = null;
        for (Salon s : parColonne.values()) {
            boolean memePartie = type == Type.PRIVE ? s.type == Type.PRIVE && s.partie != null && s.partie.equals(a.get("partie"))
                    : s.type == Type.PUBLIC && s.tailleEquipes == taille;
            if (memePartie && s.joueurs.size() < s.places()) {
                salon = s;
                break;
            }
        }
        if (salon == null) {
            int colonne = colonneLibre();
            if (colonne < 0) {
                joueur.sendMessage("§cToutes les arènes du Build Battle sont occupées, réessaie dans quelques minutes.");
                renvoyer(joueur);
                return;
            }
            UUID hote = null;
            try {
                if (a.get("hote") != null) hote = UUID.fromString(a.get("hote"));
            } catch (IllegalArgumentException ignore) {
                // hôte illisible : partie sans hôte
            }
            salon = new Salon(colonne, type, a.get("partie"), a.get("code"), hote, taille, equipesMax,
                    a.getOrDefault("tempo", "NORMAL"), Boolean.parseBoolean(a.get("themesEcrits")));
            parColonne.put(colonne, salon);
        }
        salon.joueurs.add(joueur.getUniqueId());
        parJoueur.put(joueur.getUniqueId(), salon);
        Location l = plugin.arene().apparitionSalle(salon.colonne);
        if (l == null) {
            l = plugin.monde().getSpawnLocation().add(0.5, 0, 0.5);
            joueur.sendMessage("§e(La salle d'attente n'est pas encore capturée.)");
        }
        joueur.teleport(l);
        joueur.setAllowFlight(true);
        String quoi = salon.type == Type.PRIVE ? "partie privée §f" + salon.code : "file publique §f" + salon.mode();
        for (UUID u : salon.joueurs) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            if (p.equals(joueur)) {
                p.sendMessage("§6Build Battle §7- " + quoi + "§7 : salle d'attente (" + salon.joueurs.size() + "/"
                        + salon.places() + " joueurs).");
            } else {
                p.sendMessage("§7" + joueur.getName() + " a rejoint la salle d'attente (" + salon.joueurs.size() + "/"
                        + salon.places() + ").");
            }
        }
    }

    private int colonneLibre() {
        for (int c = 0; c < plugin.arene().colonnes(); c++) {
            if (!parColonne.containsKey(c)) return c;
        }
        return -1;
    }

    /** Retire le joueur de sa salle d'attente ; une salle vide libère sa colonne. */
    void quitter(UUID id) {
        Salon s = parJoueur.remove(id);
        if (s == null) return;
        s.joueurs.remove(id);
        if (s.joueurs.isEmpty()) parColonne.remove(s.colonne);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        quitter(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        if (!e.getPlayer().getWorld().equals(plugin.monde())) quitter(e.getPlayer().getUniqueId());
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
}
