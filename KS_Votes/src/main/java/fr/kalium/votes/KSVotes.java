package fr.kalium.votes;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.menu.api.Lang;
import fr.kalium.rewardsgui.KSRewardsGUI;
import net.kyori.adventure.text.event.ClickEvent;

/**
 * KS_Votes (Kixster) - demande de LeKiwi06 du 31/10/2026 (cahier local KS_Votes.md), montants et paliers décidés le
 * 02/11/2026 (cahier local Blocs_Cultures_Objets.md, section 1 « /vote ») :
 * - un coffre de butin « Non ouvert » (KS_RewardsGUI.deposerCoffre, tables de fusion.yml) par vote, par site et par
 *   jour (minuit, heure de Paris), même si le site permet de voter plus souvent ;
 * - valeur selon la série (jours de suite avec au moins un vote) : 5 (1-2 jours), 10 (dès 3), 15 (dès 7), 20 (dès 14,
 *   plafond) ; un seul jour manqué remet la série à zéro ;
 * - top du mois (votes récompensés) : coffres de 250 / 150 / 75 aux trois premiers, donnés au début du mois suivant
 *   (réponse de LeKiwi06 du 02/11/2026) ;
 * - pseudo reçu : d'abord un joueur Java de ce nom, sinon le joueur Bedrock « .pseudo » (oubli du point plus probable
 *   qu'un même pseudo sur les deux versions, LeKiwi06 02/11/2026) ; pseudo jamais venu : vote gardé jusqu'à sa
 *   première venue (attente-jours) ;
 * - réception : événement de NuVotifier (installé ici, alimenté par le NuVotifier du proxy), lu par réflexion pour
 *   ne pas dépendre de son jar à la compilation ; sites sans Votifier : API interrogée pour les joueurs en ligne.
 *
 * Données : plugins/KS_Votes/votes.yml.
 */
public final class KSVotes extends JavaPlugin implements Listener {

    /** Un site de vote (config.yml). */
    record Site(String id, String nom, String lien, List<String> votifier, String apiUrl, String apiCle, Pattern apiOk) {
        boolean api() {
            return !apiUrl.isBlank() && apiOk != null;
        }
    }

    /** Un joueur connu : série et dernier jour récompensé par site. */
    static final class Votant {
        String nom;
        int serie;
        LocalDate dernierJour;
        final Map<String, LocalDate> sites = new HashMap<>();
    }

    /** Un vote d'un pseudo jamais venu, gardé jusqu'à sa première venue. */
    record Attente(String pseudo, String site, long date) {
    }

    /** Un coffre à déposer (réessayé chaque minute tant que KS_RewardsGUI le refuse). */
    record Du(UUID joueur, String nom, String raison, double valeur) {
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final Map<String, Site> sites = new LinkedHashMap<>();
    final Map<UUID, Votant> votants = new HashMap<>();
    /** Votes récompensés par mois (« 2026-11 ») puis par joueur : nombre, et heure du dernier (départage du top). */
    final Map<String, Map<UUID, long[]>> mois = new TreeMap<>();
    private final List<Attente> attentes = new ArrayList<>();
    private final List<Du> dus = new ArrayList<>();
    private final TreeMap<Integer, Double> paliers = new TreeMap<>();
    final List<Double> topCoffres = new ArrayList<>();
    /** Dernier mois dont le top a été récompensé. */
    private String topDonne;
    /** Dernière vérification des API demandée par chaque joueur (ouverture de /vote). */
    private final Map<UUID, Long> derniereVerif = new HashMap<>();
    private File fichier;
    ZoneId fuseau;
    String prefixe;
    Lang lang;
    Menu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        menu = new Menu(this);
        fichier = new File(getDataFolder(), "votes.yml");
        lireConfig();
        charger();
        getServer().getPluginManager().registerEvents(this, this);
        ecouterVotifier();
        // Coffres refusés, attentes trop vieilles, top du mois : chaque minute.
        getServer().getScheduler().runTaskTimer(this, this::chaqueMinute, 200L, 1200L);
        planifierApi();
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        sauver();
    }

    // ------------------------------------------------------------------ configuration et données

    private void lireConfig() {
        reloadConfig();
        fuseau = ZoneId.of(getConfig().getString("fuseau", "Europe/Paris"));
        prefixe = getConfig().getString("prefixe-bedrock", ".");
        paliers.clear();
        ConfigurationSection p = getConfig().getConfigurationSection("paliers");
        if (p != null) {
            for (String k : p.getKeys(false)) {
                try {
                    paliers.put(Integer.parseInt(k), p.getDouble(k));
                } catch (NumberFormatException e) {
                    getLogger().warning("Palier ignoré (jours invalides) : " + k);
                }
            }
        }
        if (paliers.isEmpty()) {
            paliers.put(1, 5.0);
            paliers.put(3, 10.0);
            paliers.put(7, 15.0);
            paliers.put(14, 20.0);
        }
        topCoffres.clear();
        List<Double> top = getConfig().getDoubleList("top-coffres");
        topCoffres.addAll(top.isEmpty() && !getConfig().isSet("top-coffres") ? List.of(250.0, 150.0, 75.0) : top);
        sites.clear();
        ConfigurationSection s = getConfig().getConfigurationSection("sites");
        if (s != null) {
            for (String id : s.getKeys(false)) {
                ConfigurationSection c = s.getConfigurationSection(id);
                if (c == null) {
                    continue;
                }
                Pattern ok = null;
                String regex = c.getString("api.ok", "");
                if (!regex.isBlank()) {
                    try {
                        ok = Pattern.compile(regex);
                    } catch (RuntimeException e) {
                        getLogger().warning("Site " + id + " : api.ok n'est pas une expression régulière valide.");
                    }
                }
                List<String> votifier = new ArrayList<>();
                for (String v : c.getStringList("votifier")) {
                    if (!v.isBlank()) {
                        votifier.add(v.toLowerCase(Locale.ROOT));
                    }
                }
                sites.put(id, new Site(id, c.getString("nom", id), c.getString("lien", ""), votifier,
                        c.getString("api.url", ""), c.getString("api.cle", ""), ok));
            }
        }
    }

    private void charger() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(fichier);
        votants.clear();
        ConfigurationSection j = y.getConfigurationSection("joueurs");
        if (j != null) {
            for (String k : j.getKeys(false)) {
                ConfigurationSection c = j.getConfigurationSection(k);
                try {
                    Votant v = new Votant();
                    v.nom = c.getString("nom", "?");
                    v.serie = c.getInt("serie");
                    String d = c.getString("dernier-jour");
                    v.dernierJour = d == null ? null : LocalDate.parse(d);
                    ConfigurationSection ss = c.getConfigurationSection("sites");
                    if (ss != null) {
                        for (String site : ss.getKeys(false)) {
                            v.sites.put(site, LocalDate.parse(ss.getString(site)));
                        }
                    }
                    votants.put(UUID.fromString(k), v);
                } catch (RuntimeException e) {
                    getLogger().warning("Joueur ignoré dans votes.yml : " + k);
                }
            }
        }
        mois.clear();
        ConfigurationSection m = y.getConfigurationSection("mois");
        if (m != null) {
            for (String ym : m.getKeys(false)) {
                Map<UUID, long[]> compte = new HashMap<>();
                ConfigurationSection c = m.getConfigurationSection(ym);
                for (String k : c.getKeys(false)) {
                    try {
                        compte.put(UUID.fromString(k), new long[] { c.getLong(k + ".votes"), c.getLong(k + ".dernier") });
                    } catch (IllegalArgumentException e) {
                        // ligne illisible : ignorée
                    }
                }
                mois.put(ym, compte);
            }
        }
        attentes.clear();
        for (Map<?, ?> a : y.getMapList("attente")) {
            attentes.add(new Attente(String.valueOf(a.get("pseudo")), String.valueOf(a.get("site")),
                    ((Number) a.get("date")).longValue()));
        }
        dus.clear();
        for (Map<?, ?> a : y.getMapList("dus")) {
            dus.add(new Du(UUID.fromString(String.valueOf(a.get("joueur"))), String.valueOf(a.get("nom")),
                    String.valueOf(a.get("raison")), ((Number) a.get("valeur")).doubleValue()));
        }
        topDonne = y.getString("top-donne");
    }

    void sauver() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Votant> e : votants.entrySet()) {
            String k = "joueurs." + e.getKey() + ".";
            Votant v = e.getValue();
            y.set(k + "nom", v.nom);
            y.set(k + "serie", v.serie);
            y.set(k + "dernier-jour", v.dernierJour == null ? null : v.dernierJour.toString());
            for (Map.Entry<String, LocalDate> s : v.sites.entrySet()) {
                y.set(k + "sites." + s.getKey(), s.getValue().toString());
            }
        }
        for (Map.Entry<String, Map<UUID, long[]>> e : mois.entrySet()) {
            for (Map.Entry<UUID, long[]> c : e.getValue().entrySet()) {
                y.set("mois." + e.getKey() + "." + c.getKey() + ".votes", c.getValue()[0]);
                y.set("mois." + e.getKey() + "." + c.getKey() + ".dernier", c.getValue()[1]);
            }
        }
        List<Map<String, Object>> a = new ArrayList<>();
        for (Attente at : attentes) {
            a.add(Map.of("pseudo", at.pseudo(), "site", at.site(), "date", at.date()));
        }
        y.set("attente", a);
        List<Map<String, Object>> d = new ArrayList<>();
        for (Du du : dus) {
            d.add(Map.of("joueur", du.joueur().toString(), "nom", du.nom(), "raison", du.raison(), "valeur", du.valeur()));
        }
        y.set("dus", d);
        y.set("top-donne", topDonne);
        try {
            getDataFolder().mkdirs();
            y.save(fichier);
        } catch (IOException e) {
            getLogger().warning("Impossible d'enregistrer votes.yml : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ jours, séries, paliers

    LocalDate aujourdhui() {
        return LocalDate.now(fuseau);
    }

    /** Temps restant avant minuit (heure de Paris). */
    Duration avantMinuit() {
        ZonedDateTime maintenant = ZonedDateTime.now(fuseau);
        return Duration.between(maintenant, maintenant.toLocalDate().plusDays(1).atStartOfDay(fuseau));
    }

    double valeur(int serie) {
        Map.Entry<Integer, Double> e = paliers.floorEntry(Math.max(1, serie));
        return e == null ? paliers.firstEntry().getValue() : e.getValue();
    }

    /** Prochain palier au-dessus de cette série, ou null au plafond. */
    Map.Entry<Integer, Double> prochainPalier(int serie) {
        return paliers.higherEntry(serie);
    }

    /** Série en cours : 0 si le dernier jour avec un vote est avant-hier ou plus ancien. */
    int serieActuelle(Votant v) {
        if (v == null || v.dernierJour == null || v.dernierJour.isBefore(aujourdhui().minusDays(1))) {
            return 0;
        }
        return v.serie;
    }

    /** Série qu'aura le joueur à son prochain vote d'aujourd'hui. */
    int serieAuProchainVote(Votant v) {
        int s = serieActuelle(v);
        return v != null && aujourdhui().equals(v.dernierJour) ? s : s + 1;
    }

    String moisCle(YearMonth ym) {
        return ym.toString();
    }

    /** Les premiers du mois : [uuid, votes], par votes décroissants puis par l'heure du dernier vote. */
    List<Map.Entry<UUID, long[]>> classement(String ym, int n) {
        List<Map.Entry<UUID, long[]>> l = new ArrayList<>(mois.getOrDefault(ym, Map.of()).entrySet());
        l.sort(Comparator.<Map.Entry<UUID, long[]>>comparingLong(e -> -e.getValue()[0])
                .thenComparingLong(e -> e.getValue()[1]));
        return l.subList(0, Math.min(n, l.size()));
    }

    String nom(UUID u) {
        Votant v = votants.get(u);
        if (v != null) {
            return v.nom;
        }
        String n = Bukkit.getOfflinePlayer(u).getName();
        return n == null ? "?" : n;
    }

    // ------------------------------------------------------------------ réception des votes

    /** Événement de NuVotifier (plugin « Votifier »), écouté par réflexion. */
    private void ecouterVotifier() {
        Plugin votifier = getServer().getPluginManager().getPlugin("Votifier");
        if (votifier == null) {
            getLogger().warning("NuVotifier (Votifier) absent : seuls les sites à API sont reçus.");
            return;
        }
        try {
            Class<? extends Event> classe = Class.forName("com.vexsoftware.votifier.model.VotifierEvent", true,
                    votifier.getClass().getClassLoader()).asSubclass(Event.class);
            getServer().getPluginManager().registerEvent(classe, this, EventPriority.NORMAL, (l, e) -> {
                if (!classe.isInstance(e)) {
                    return;
                }
                try {
                    Object vote = e.getClass().getMethod("getVote").invoke(e);
                    String service = String.valueOf(vote.getClass().getMethod("getServiceName").invoke(vote));
                    String pseudo = String.valueOf(vote.getClass().getMethod("getUsername").invoke(vote));
                    if (Bukkit.isPrimaryThread()) {
                        recevoirVotifier(service, pseudo);
                    } else {
                        Bukkit.getScheduler().runTask(this, () -> recevoirVotifier(service, pseudo));
                    }
                } catch (ReflectiveOperationException ex) {
                    getLogger().warning("Vote illisible : " + ex);
                }
            }, this);
            getLogger().info("Votes de NuVotifier écoutés.");
        } catch (ClassNotFoundException | ClassCastException ex) {
            getLogger().warning("NuVotifier présent mais VotifierEvent introuvable : " + ex);
        }
    }

    private void recevoirVotifier(String service, String pseudo) {
        String s = service == null ? "" : service.toLowerCase(Locale.ROOT);
        for (Site site : sites.values()) {
            for (String v : site.votifier()) {
                if (s.contains(v)) {
                    recevoir(pseudo, site.id());
                    return;
                }
            }
        }
        getLogger().warning("Vote de " + pseudo + " d'un service inconnu : « " + service
                + " » (à ajouter dans « votifier » du bon site, config.yml).");
    }

    /** Un vote reçu pour un pseudo (Votifier ou /votes test) : rattaché à un joueur, ou gardé. Fil principal. */
    void recevoir(String pseudo, String site) {
        pseudo = pseudo == null ? "" : pseudo.trim();
        if (pseudo.isEmpty() || !sites.containsKey(site)) {
            return;
        }
        UUID u = trouver(pseudo);
        if (u == null) {
            attentes.add(new Attente(pseudo, site, System.currentTimeMillis()));
            sauver();
            getLogger().info("Vote de " + pseudo + " sur " + site + " gardé (pseudo jamais venu sur ce serveur).");
            return;
        }
        recompenser(u, nom(u), site, System.currentTimeMillis());
    }

    /** Un joueur Java de ce nom, sinon le joueur Bedrock « préfixe + nom » ; null si aucun n'est venu. */
    UUID trouver(String pseudo) {
        if (pseudo.startsWith(prefixe)) {
            return connu(pseudo);
        }
        UUID java = connu(pseudo);
        return java != null ? java : connu(prefixe + pseudo);
    }

    private UUID connu(String nom) {
        Player p = Bukkit.getPlayerExact(nom);
        if (p != null) {
            return p.getUniqueId();
        }
        for (Map.Entry<UUID, Votant> e : votants.entrySet()) {
            if (e.getValue().nom.equalsIgnoreCase(nom)) {
                return e.getKey();
            }
        }
        OfflinePlayer o = Bukkit.getOfflinePlayerIfCached(nom);
        return o != null && o.hasPlayedBefore() && nom.equalsIgnoreCase(o.getName()) ? o.getUniqueId() : null;
    }

    /**
     * Récompense un vote (un par site et par jour) : série, coffre, compte du mois, annonce. Faux si ce joueur a déjà
     * été récompensé sur ce site ce jour-là (ou plus tard, pour un vote gardé).
     */
    boolean recompenser(UUID u, String nom, String siteId, long date) {
        Site site = sites.get(siteId);
        if (site == null) {
            return false;
        }
        LocalDate jour = Instant.ofEpochMilli(date).atZone(fuseau).toLocalDate();
        Votant v = votants.computeIfAbsent(u, k -> new Votant());
        if (v.nom == null) {
            v.nom = nom;
        }
        LocalDate deja = v.sites.get(siteId);
        if (deja != null && !jour.isAfter(deja)) {
            return false;
        }
        v.sites.put(siteId, jour);
        if (v.dernierJour == null || jour.isAfter(v.dernierJour.plusDays(1))) {
            v.serie = 1;
            v.dernierJour = jour;
        } else if (jour.equals(v.dernierJour.plusDays(1))) {
            v.serie++;
            v.dernierJour = jour;
        }
        // Même jour, ou vote gardé plus ancien que le dernier jour : série inchangée.
        int serie = Math.max(1, v.serie);
        double valeur = valeur(serie);
        String raison = lang.raw("raison", "Vote sur <site>").replace("<site>", site.nom());
        if (!KSRewardsGUI.deposerCoffre(u, v.nom, "Votes", raison, valeur)) {
            dus.add(new Du(u, v.nom, raison, valeur));
        }
        String ym = moisCle(YearMonth.from(jour));
        long[] c = mois.computeIfAbsent(ym, k -> new HashMap<>()).computeIfAbsent(u, k -> new long[2]);
        c[0]++;
        c[1] = date;
        sauver();
        getLogger().info("Vote de " + v.nom + " sur " + siteId + " : coffre de " + valeur + " (série " + serie + ").");
        Player p = Bukkit.getPlayer(u);
        if (p != null) {
            p.sendMessage(lang.c("merci", "<green>Merci pour ton vote sur <site> ! <gray>Coffre de valeur <valeur> "
                    + "dans /rewards (série : <serie> j).", "site", site.nom(), "valeur", format(valeur), "serie", serie));
            menu.rafraichir(p);
        }
        if (getConfig().getBoolean("annonce", true)) {
            Bukkit.broadcast(lang.c("annonce", "<green><joueur> <gray>a voté pour Kixster sur <green><site><gray> : "
                    + "<yellow>/vote", "joueur", v.nom, "site", site.nom())
                    .clickEvent(ClickEvent.runCommand("/vote")));
        }
        return true;
    }

    static String format(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Chaque joueur venu est retenu avec son pseudo : un vote à son nom le retrouve, même hors ligne.
        votants.computeIfAbsent(p.getUniqueId(), k -> new Votant()).nom = p.getName();
        // Votes gardés pour ce pseudo (Bedrock : aussi sans le point, si aucun joueur Java ne porte ce nom).
        String sansPrefixe = p.getName().startsWith(prefixe) ? p.getName().substring(prefixe.length()) : null;
        List<Attente> pour = new ArrayList<>();
        for (Attente a : attentes) {
            if (a.pseudo().equalsIgnoreCase(p.getName())
                    || (sansPrefixe != null && a.pseudo().equalsIgnoreCase(sansPrefixe) && connu(sansPrefixe) == null)) {
                pour.add(a);
            }
        }
        if (!pour.isEmpty()) {
            attentes.removeAll(pour);
            pour.sort(Comparator.comparingLong(Attente::date));
            for (Attente a : pour) {
                recompenser(p.getUniqueId(), p.getName(), a.site(), a.date());
            }
        }
        sauver();
    }

    // ------------------------------------------------------------------ sites à API

    private void planifierApi() {
        long t = Math.max(60, getConfig().getLong("api-intervalle-secondes", 300)) * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                verifierApi(p);
            }
        }, t, t);
    }

    /** À l'ouverture de /vote : vérification des API pour ce joueur, au plus une fois par 30 secondes. */
    void verifierApiMenu(Player p) {
        long maintenant = System.currentTimeMillis();
        Long avant = derniereVerif.get(p.getUniqueId());
        if (avant != null && maintenant - avant < 30_000) {
            return;
        }
        derniereVerif.put(p.getUniqueId(), maintenant);
        verifierApi(p);
    }

    /** Interroge (hors du fil principal) chaque site à API où ce joueur n'a pas été récompensé aujourd'hui. */
    private void verifierApi(Player p) {
        Votant v = votants.get(p.getUniqueId());
        LocalDate jour = aujourdhui();
        String ip = p.getAddress() == null ? "" : p.getAddress().getAddress().getHostAddress();
        String pseudo = p.getName().startsWith(prefixe) ? p.getName().substring(prefixe.length()) : p.getName();
        UUID u = p.getUniqueId();
        String nom = p.getName();
        for (Site site : sites.values()) {
            if (!site.api() || (v != null && jour.equals(v.sites.get(site.id())))) {
                continue;
            }
            String url = site.apiUrl().replace("{ip}", enc(ip)).replace("{pseudo}", enc(pseudo))
                    .replace("{cle}", enc(site.apiCle()));
            HttpRequest req;
            try {
                req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build();
            } catch (IllegalArgumentException e) {
                getLogger().warning("Site " + site.id() + " : api.url invalide.");
                continue;
            }
            http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
                if (r.statusCode() / 100 == 2 && site.apiOk().matcher(r.body()).find()) {
                    Bukkit.getScheduler().runTask(this, () -> recompenser(u, nom, site.id(), System.currentTimeMillis()));
                }
            }).exceptionally(ex -> null);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ chaque minute

    private void chaqueMinute() {
        boolean change = false;
        if (!dus.isEmpty()) {
            List<Du> encore = new ArrayList<>();
            for (Du d : dus) {
                if (!KSRewardsGUI.deposerCoffre(d.joueur(), d.nom(), "Votes", d.raison(), d.valeur())) {
                    encore.add(d);
                }
            }
            change = encore.size() != dus.size();
            dus.clear();
            dus.addAll(encore);
        }
        long limite = System.currentTimeMillis() - getConfig().getLong("attente-jours", 30) * 86_400_000L;
        change |= attentes.removeIf(a -> a.date() < limite);
        change |= donnerTop();
        if (change) {
            sauver();
        }
    }

    /** Coffres du top du mois précédent, une seule fois. Vrai si quelque chose a changé. */
    private boolean donnerTop() {
        String precedent = moisCle(YearMonth.now(fuseau).minusMonths(1));
        if (topDonne != null && topDonne.compareTo(precedent) >= 0) {
            return false;
        }
        topDonne = precedent;
        List<Map.Entry<UUID, long[]>> top = classement(precedent, topCoffres.size());
        for (int i = 0; i < top.size(); i++) {
            UUID u = top.get(i).getKey();
            double valeur = topCoffres.get(i);
            if (valeur <= 0) {
                continue;
            }
            String raison = lang.raw("raison-top", "Top des votes de <mois> : <place>e").replace("<mois>", precedent)
                    .replace("<place>", String.valueOf(i + 1));
            if (!KSRewardsGUI.deposerCoffre(u, nom(u), "Votes", raison, valeur)) {
                dus.add(new Du(u, nom(u), raison, valeur));
            }
            getLogger().info("Top des votes de " + precedent + " : " + nom(u) + " " + (i + 1) + "e, coffre de " + valeur);
        }
        lang.saveIfNeeded();
        return true;
    }

    // ------------------------------------------------------------------ commandes

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("vote")) {
            if (s instanceof Player p) {
                menu.ouvrir(p);
            } else {
                s.sendMessage("Commande réservée aux joueurs.");
            }
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "recharger" -> {
                lireConfig();
                lang.load();
                s.sendMessage("KS_Votes : config.yml et lang.yml relus (" + sites.size() + " sites).");
            }
            case "test" -> {
                if (args.length < 3 || !sites.containsKey(args[2])) {
                    s.sendMessage("Usage : /votes test <joueur> <site>   sites : " + String.join(", ", sites.keySet()));
                    return true;
                }
                UUID u = trouver(args[1]);
                if (u == null) {
                    recevoir(args[1], args[2]);
                    s.sendMessage("Pseudo jamais venu : vote gardé jusqu'à sa première venue.");
                } else if (recompenser(u, nom(u), args[2], System.currentTimeMillis())) {
                    s.sendMessage("Vote simulé pour " + nom(u) + " sur " + args[2] + ".");
                } else {
                    s.sendMessage(nom(u) + " a déjà été récompensé sur " + args[2] + " aujourd'hui.");
                }
            }
            case "serie" -> {
                if (args.length < 2) {
                    return false;
                }
                UUID u = trouver(args[1]);
                if (u == null) {
                    s.sendMessage("Joueur inconnu sur ce serveur : " + args[1]);
                    return true;
                }
                Votant v = votants.computeIfAbsent(u, k -> new Votant());
                if (v.nom == null) {
                    v.nom = nom(u);
                }
                if (args.length >= 3) {
                    int jours;
                    try {
                        jours = Math.max(0, Integer.parseInt(args[2]));
                    } catch (NumberFormatException e) {
                        s.sendMessage("Nombre de jours invalide : " + args[2]);
                        return true;
                    }
                    // La série corrigée compte aujourd'hui : un vote aujourd'hui ne l'augmente pas.
                    v.serie = jours;
                    v.dernierJour = jours == 0 ? null : aujourdhui();
                    sauver();
                }
                s.sendMessage(v.nom + " : série de " + serieActuelle(v) + " jour(s), dernier jour avec un vote : "
                        + (v.dernierJour == null ? "aucun" : v.dernierJour) + ", prochain coffre : "
                        + format(valeur(serieAuProchainVote(v))) + ".");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("votes")) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("test", "serie", "recharger");
        }
        if (args.length == 2) {
            return null;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
            return new ArrayList<>(sites.keySet());
        }
        return List.of();
    }
}
