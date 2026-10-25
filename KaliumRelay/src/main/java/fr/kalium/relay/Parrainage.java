package fr.kalium.relay;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 1.10.0 - parrainage et liste blanche de Kixster (demande de LeKiwi06, 23/10/2026, message d'annonce : « pour chaque
 * joueur qui vient grâce à vous, vous recevez tous les 2 des récompenses [...] votre ami n'a plus qu'à accepter le
 * parrainage, et il sera automatiquement whitelisté sur Kixster » ; « une interface apparaît quand vous essayez de
 * rejoindre Kixster sans être whitelisté, vous devez préciser votre pseudo Discord, un admin traitera la demande »).
 *
 * Le proxy garde :
 * - les propositions de parrainage (parrain -> filleul) et les parrainages acceptés (un seul parrain par filleul, gardé
 *   pour toujours : historique des admins, sanctions) ;
 * - les demandes de liste blanche (pseudo Discord), traitées par un admin dans le menu de KLM_Contacts ;
 * - les joueurs « autorisés » : acceptés (parrainage ou admin) mais pas encore ajoutés à la liste blanche de Kixster ;
 *   KS_Parrainage les y ajoute à leur arrivée, puis le confirme (« wapplique ») ;
 * - une copie de la liste blanche de Kixster (et de ses opérateurs), envoyée par KS_Parrainage (« wsync »).
 * Le temps de jeu des filleuls et les récompenses sont comptés sur Kixster (KS_Parrainage) ; le proxy n'en garde
 * qu'une copie pour l'affichage (« pjeu »).
 *
 * Entrée sur Kixster ({@link #peutEntrer}) : un joueur absent de la copie (et non autorisé, non admin) est refusé par le
 * proxy, et son serveur actuel reçoit le message « whitelist » (canal kalium:contacts) : KLM_Contacts lui ouvre la
 * fenêtre de demande, ou l'acceptation d'un parrainage s'il en a reçu un. Tant que Kixster n'a jamais envoyé sa liste,
 * le proxy laisse passer (la liste blanche vanilla de Kixster s'applique seule).
 *
 * 1.10.1 (LeKiwi06, 23/10/2026) : un joueur déjà whitelisté peut aussi prendre un parrain (rattrapage des récompenses,
 * temps passé estimé par KS_Parrainage) ; pas de parrainage croisé ; « whitelist » dans les textes (au lieu de « liste
 * blanche ») ; date d'acceptation envoyée à KS_Parrainage (« pkixster »).
 *
 * 1.10.2 (LeKiwi06, 26/10/2026 : « j'aimerai que tu me créer un historique pour les demandes de whitelist traités ») :
 * chaque demande traitée (par un admin, ou par un parrainage accepté) est gardée dans l'historique, avec la date de
 * traitement ; elle y reste quand le joueur refait une demande (avant : la nouvelle demande remplaçait l'ancienne).
 * Les demandes déjà traitées avant la 1.10.2 y sont reprises sans date de traitement.
 *
 * Données : plugins/kaliumrelay/parrainage.txt (lignes à onglets) et whitelist-kixster.txt. Toutes les méthodes sont
 * appelées sous le verrou de {@link Contacts} (comme ContactsStore).
 */
final class Parrainage {

    /** Propositions de parrainage en attente par parrain. */
    private static final int MAX_PROPOSITIONS = 10;
    private static final int MAX_DISCORD = 40;

    record Accepte(UUID filleul, UUID parrain, long date) {
    }

    /**
     * Demande de liste blanche : etat « attente », « acceptee » ou « refusee » ; admin = pseudo de qui l'a traitee
     * (« parrainage » : acceptee par un parrainage) ; traite = date du traitement (0 : inconnue, avant la 1.10.2).
     */
    static final class Demande {
        final UUID joueur;
        String discord;
        long date;
        String etat = "attente";
        String admin = "";
        long traite;

        Demande(UUID joueur) {
            this.joueur = joueur;
        }
    }

    private final Contacts contacts;
    private final ProxyServer server;
    private final Logger logger;
    private final RelayConfig config;
    private final Path fichier;
    private final Path fichierListe;
    private final MinecraftChannelIdentifier canal = MinecraftChannelIdentifier.create("kalium", "contacts");

    /** Filleul -> parrainage accepte. */
    private final Map<UUID, Accepte> acceptes = new LinkedHashMap<>();
    /** Filleul -> parrains qui le lui proposent. */
    private final Map<UUID, Set<UUID>> propositions = new LinkedHashMap<>();
    /** Joueur -> source (« parrain:&lt;uuid&gt; » ou « admin:&lt;pseudo&gt; ») : a ajouter a la liste blanche de Kixster. */
    private final Map<UUID, String> autorises = new LinkedHashMap<>();
    private final Map<UUID, Demande> demandes = new LinkedHashMap<>();
    /** 1.10.2 : demandes traitees, dans l'ordre du traitement (gardees pour toujours). */
    private final List<Demande> traitees = new ArrayList<>();
    /** Filleul -> {secondes de jeu actives, palier atteint} (copie de KS_Parrainage, pour l'affichage). */
    private final Map<UUID, long[]> jeu = new LinkedHashMap<>();
    /** Copie de la liste blanche de Kixster (joueurs et operateurs) ; null tant qu'elle n'a jamais ete envoyee. */
    private Set<UUID> liste;
    private boolean listeActive = true;
    /** Paliers de KS_Parrainage (« heures:valeur,... »), pour l'affichage du prochain palier. */
    private String paliers = "";

    Parrainage(Contacts contacts, ProxyServer server, Logger logger, RelayConfig config, Path dataDirectory) {
        this.contacts = contacts;
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.fichier = dataDirectory.resolve("parrainage.txt");
        this.fichierListe = dataDirectory.resolve("whitelist-kixster.txt");
        charger();
    }

    // ------------------------------------------------------------------ donnees

    private void charger() {
        try {
            if (Files.exists(fichier)) {
                for (String ligne : Files.readAllLines(fichier, StandardCharsets.UTF_8)) {
                    String[] c = ligne.split("\t", -1);
                    try {
                        switch (c[0]) {
                            case "A" -> acceptes.put(UUID.fromString(c[1]),
                                    new Accepte(UUID.fromString(c[1]), UUID.fromString(c[2]), Long.parseLong(c[3])));
                            case "P" -> propositions.computeIfAbsent(UUID.fromString(c[1]), u -> new LinkedHashSet<>())
                                    .add(UUID.fromString(c[2]));
                            case "W" -> autorises.put(UUID.fromString(c[1]), c[2]);
                            case "D" -> {
                                Demande d = new Demande(UUID.fromString(c[1]));
                                d.discord = c[2];
                                d.date = Long.parseLong(c[3]);
                                d.etat = c[4];
                                d.admin = c.length > 5 ? c[5] : "";
                                demandes.put(d.joueur, d);
                            }
                            case "H" -> {
                                Demande d = new Demande(UUID.fromString(c[1]));
                                d.discord = c[2];
                                d.date = Long.parseLong(c[3]);
                                d.etat = c[4];
                                d.admin = c[5];
                                d.traite = Long.parseLong(c[6]);
                                traitees.add(d);
                            }
                            case "T" -> paliers = c[1];
                            case "J" -> jeu.put(UUID.fromString(c[1]), new long[]{Long.parseLong(c[2]), Long.parseLong(c[3])});
                            default -> {
                            }
                        }
                    } catch (RuntimeException e) {
                        // ligne abimee : ignoree
                    }
                }
            }
            if (traitees.isEmpty()) {
                // Premier demarrage en 1.10.2 : les demandes deja traitees passent dans l'historique (sans date).
                for (Demande d : demandes.values()) {
                    if (!d.etat.equals("attente")) {
                        traitees.add(copie(d));
                    }
                }
            }
            if (Files.exists(fichierListe)) {
                List<String> lignes = Files.readAllLines(fichierListe, StandardCharsets.UTF_8);
                liste = new HashSet<>();
                for (String ligne : lignes) {
                    if (ligne.startsWith("active=")) {
                        listeActive = Boolean.parseBoolean(ligne.substring("active=".length()).trim());
                    } else if (!ligne.isBlank()) {
                        try {
                            liste.add(UUID.fromString(ligne.trim()));
                        } catch (IllegalArgumentException ignored) {
                            // ligne abimee : ignoree
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("[KaliumRelay] Parrainage : donnees illisibles : " + e.getMessage());
        }
    }

    private void sauver() {
        List<String> lignes = new ArrayList<>();
        acceptes.values().forEach(a -> lignes.add("A\t" + a.filleul() + "\t" + a.parrain() + "\t" + a.date()));
        propositions.forEach((filleul, parrains) -> parrains.forEach(p -> lignes.add("P\t" + filleul + "\t" + p)));
        autorises.forEach((id, source) -> lignes.add("W\t" + id + "\t" + source));
        demandes.values().forEach(d -> lignes.add("D\t" + d.joueur + "\t" + d.discord + "\t" + d.date + "\t" + d.etat
                + "\t" + d.admin));
        traitees.forEach(d -> lignes.add("H\t" + d.joueur + "\t" + d.discord + "\t" + d.date + "\t" + d.etat + "\t"
                + d.admin + "\t" + d.traite));
        jeu.forEach((id, v) -> lignes.add("J\t" + id + "\t" + v[0] + "\t" + v[1]));
        lignes.add("T\t" + paliers);
        ecrire(fichier, lignes);
    }

    private void sauverListe() {
        List<String> lignes = new ArrayList<>();
        lignes.add("active=" + listeActive);
        liste.forEach(id -> lignes.add(id.toString()));
        ecrire(fichierListe, lignes);
    }

    private void ecrire(Path cible, List<String> lignes) {
        try {
            Path temporaire = cible.resolveSibling(cible.getFileName() + ".tmp");
            Files.write(temporaire, lignes, StandardCharsets.UTF_8);
            Files.move(temporaire, cible, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.warn("[KaliumRelay] Parrainage : ecriture de " + cible.getFileName() + " impossible : " + e.getMessage());
        }
    }

    private static Demande copie(Demande d) {
        Demande c = new Demande(d.joueur);
        c.discord = d.discord;
        c.date = d.date;
        c.etat = d.etat;
        c.admin = d.admin;
        c.traite = d.traite;
        return c;
    }

    /** 1.10.2 : la demande en attente est traitee ; une copie part dans l'historique. */
    private void clore(Demande d, boolean acceptee, String par) {
        d.etat = acceptee ? "acceptee" : "refusee";
        d.admin = par;
        d.traite = System.currentTimeMillis();
        traitees.add(copie(d));
    }

    // ------------------------------------------------------------------ etat d'un joueur

    private String nom(UUID id) {
        String nom = contacts.store.get(id).name;
        return nom.isEmpty() ? id.toString().substring(0, 8) : nom;
    }

    /** Sur la liste blanche de Kixster (copie), ou accepte et en attente d'y etre ajoute. */
    private boolean surKixster(UUID id) {
        return autorises.containsKey(id) || (liste != null && liste.contains(id));
    }

    /**
     * Peut encore etre parraine : n'a jamais eu de parrain. 1.10.1 (LeKiwi06, 23/10/2026 : « j'aimerais que les joueurs
     * deja whitelist puissent prendre un parrain, pour qu'on puisse rattraper les recompenses qu'on aurait du avoir ») :
     * un joueur deja sur Kixster peut aussi etre parraine (avant : seulement un joueur jamais venu).
     */
    private boolean parrainable(UUID id) {
        return !acceptes.containsKey(id);
    }

    /** 1.10.1 : pas de parrainage croise (un parrain, ou le parrain de son parrain..., ne devient pas son filleul). */
    private boolean cycle(UUID parrain, UUID filleul) {
        UUID p = parrain;
        for (int i = 0; i < 100 && p != null; i++) {
            if (p.equals(filleul)) {
                return true;
            }
            Accepte a = acceptes.get(p);
            p = a == null ? null : a.parrain();
        }
        return false;
    }

    private static boolean bedrock(UUID id) {
        return id.getMostSignificantBits() == 0L;
    }

    /**
     * Entree sur le serveur de la liste blanche (parrainage-serveur de relay.properties) : vrai si le joueur peut y aller.
     * Sinon il est prevenu et son serveur actuel lui ouvre la demande (ou l'acceptation d'un parrainage).
     */
    boolean peutEntrer(Player joueur, String cible) {
        if (!cible.equalsIgnoreCase(config.parrainageServeur())) {
            return true;
        }
        UUID id = joueur.getUniqueId();
        synchronized (contacts) {
            if (liste == null || !listeActive || surKixster(id)
                    || config.isAdmin(joueur.getUsername(), id)) {
                return true;
            }
        }
        joueur.sendMessage(Contacts.info("Tu n'es pas encore whitelisté sur Kixster : fais-toi parrainer par "
                + "un ami, ou envoie une demande aux admins."));
        Optional<ServerConnection> ici = joueur.getCurrentServer();
        if (ici.isEmpty() || !ici.get().sendPluginMessage(canal, "whitelist".getBytes(StandardCharsets.UTF_8))) {
            joueur.sendMessage(Contacts.info("Menu Contacts > Parrainage pour faire ta demande."));
        }
        logger.info("[KaliumRelay] " + joueur.getUsername() + " : entree sur '" + cible + "' refusee (liste blanche).");
        return false;
    }

    /** Arrivee sur KaLium : propositions de parrainage recues. */
    void arrivee(Player joueur) {
        int recues;
        synchronized (contacts) {
            Set<UUID> p = propositions.get(joueur.getUniqueId());
            recues = p == null ? 0 : p.size();
        }
        if (recues > 0) {
            joueur.sendMessage(Contacts.info("Tu as " + recues + " proposition" + (recues > 1 ? "s" : "")
                    + " de parrainage pour Kixster : ").append(lien("/parrainage")));
        }
    }

    /**
     * 1.10.2 (LeKiwi06, 26/10/2026 : « met moi un message quand je rejoins le lobby comme pour les rewards pour me dire
     * combien de demande de whitelist sont en attente ») : appele 3 s apres chaque arrivee sur le lobby. Avant, le
     * nombre n'etait envoye qu'a la premiere connexion, au moment meme de l'arrivee (message sans doute perdu).
     */
    void rappelWhitelist(Player joueur) {
        if (!joueur.isActive() || !config.isAdmin(joueur.getUsername(), joueur.getUniqueId())) {
            return;
        }
        int enAttente = 0;
        synchronized (contacts) {
            for (Demande d : demandes.values()) {
                if (d.etat.equals("attente")) {
                    enAttente++;
                }
            }
        }
        if (enAttente > 0) {
            joueur.sendMessage(Component.text("Tu as " + enAttente + " demande" + (enAttente > 1 ? "s" : "")
                    + " de whitelist en attente : ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("menu > Modération > Demandes de whitelist", NamedTextColor.GRAY)));
        }
    }

    private static Component lien(String commande) {
        return Component.text(commande, NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand(commande));
    }

    // ------------------------------------------------------------------ requetes

    private static final Set<String> ACTIONS = Set.of("plist", "pparrainer", "pannuler", "paccepter", "prefuser",
            "pkixster", "pjeu", "wdemande", "wliste", "whisto", "whdemandes", "waccepter", "wrefuser", "wcheck", "wapplique", "wsync");

    static boolean gere(String action) {
        return ACTIONS.contains(action);
    }

    /** Requetes POST /contacts/&lt;action&gt; du parrainage (appele sous le verrou de Contacts). */
    String handle(String action, Map<String, String> params) {
        // Requetes de KS_Parrainage (serveur de la liste blanche) : sans joueur.
        switch (action) {
            case "wsync" -> {
                return synchroniser(params.getOrDefault("active", "true"), params.getOrDefault("list", ""));
            }
            case "wcheck" -> {
                UUID id = uuid(params.get("target"));
                return id == null ? "err:bad" : "ok:" + autorises.containsKey(id);
            }
            case "wapplique" -> {
                UUID id = uuid(params.get("target"));
                if (id != null && autorises.remove(id) != null) {
                    if (liste != null) {
                        liste.add(id);
                        sauverListe();
                    }
                    sauver();
                }
                return "ok";
            }
            case "pkixster" -> {
                StringBuilder out = new StringBuilder("ok\n");
                for (Accepte a : acceptes.values()) {
                    out.append("A\t").append(a.filleul()).append('\t').append(nom(a.filleul())).append('\t')
                            .append(a.parrain()).append('\t').append(nom(a.parrain())).append('\t').append(a.date())
                            .append('\n');
                }
                return out.toString();
            }
            case "pjeu" -> {
                String table = params.getOrDefault("paliers", "");
                if (table.matches("[0-9.:,]*")) {
                    paliers = table;
                }
                for (String partie : params.getOrDefault("data", "").split(",")) {
                    String[] c = partie.split(":");
                    UUID id = c.length == 3 ? uuid(c[0]) : null;
                    if (id != null && acceptes.containsKey(id)) {
                        try {
                            jeu.put(id, new long[]{Long.parseLong(c[1]), Long.parseLong(c[2])});
                        } catch (NumberFormatException ignored) {
                            // valeur abimee : ignoree
                        }
                    }
                }
                sauver();
                return "ok";
            }
            default -> {
            }
        }
        UUID moi = uuid(params.get("player"));
        if (moi == null) {
            return "err:player";
        }
        return switch (action) {
            case "plist" -> lister(moi);
            case "pparrainer" -> parrainer(moi, params.get("target"));
            case "pannuler" -> {
                UUID filleul = cible(params.get("target"));
                Set<UUID> p = filleul == null ? null : propositions.get(filleul);
                if (p == null || !p.remove(moi)) {
                    yield "err:none";
                }
                if (p.isEmpty()) {
                    propositions.remove(filleul);
                }
                sauver();
                yield "ok:" + nom(filleul);
            }
            case "paccepter" -> accepter(moi, cible(params.get("target")));
            case "prefuser" -> {
                UUID parrain = cible(params.get("target"));
                Set<UUID> p = parrain == null ? null : propositions.get(moi);
                if (p == null || !p.remove(parrain)) {
                    yield "err:none";
                }
                if (p.isEmpty()) {
                    propositions.remove(moi);
                }
                sauver();
                contacts.tell(parrain, Contacts.info(nom(moi) + " a refusé ton parrainage."));
                yield "ok:" + nom(parrain);
            }
            case "wdemande" -> demander(moi, params.getOrDefault("discord", ""));
            case "wliste" -> listeDemandes();
            case "whisto" -> historique();
            case "whdemandes" -> historiqueDemandes();
            case "waccepter", "wrefuser" -> traiter(moi, cible(params.get("target")), action.equals("waccepter"));
            default -> "err:action";
        };
    }

    private static UUID uuid(String texte) {
        try {
            return texte == null ? null : UUID.fromString(texte.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Joueur designe par son UUID ou son pseudo (deja venu sur KaLium), ou null. */
    private UUID cible(String texte) {
        UUID id = uuid(texte);
        if (id != null) {
            return contacts.store.get(id).name.isEmpty() ? null : id;
        }
        return contacts.store.byName(texte);
    }

    private String synchroniser(String active, String texte) {
        Set<UUID> nouvelle = new HashSet<>();
        for (String partie : texte.split(",")) {
            UUID id = uuid(partie);
            if (id != null) {
                nouvelle.add(id);
            }
        }
        liste = nouvelle;
        listeActive = Boolean.parseBoolean(active.trim());
        // Deja ajoutes a la liste blanche de Kixster (par un admin, a la main) : plus rien a faire pour eux.
        if (autorises.keySet().removeIf(nouvelle::contains)) {
            sauver();
        }
        sauverListe();
        return "ok";
    }

    /** Tout ce que les menus de parrainage affichent pour ce joueur (lignes a onglets apres « ok »). */
    private String lister(UUID moi) {
        StringBuilder out = new StringBuilder("ok\n");
        out.append("K\t").append(surKixster(moi)).append('\t').append(liste != null).append('\n');
        out.append("T\t").append(paliers).append('\n');
        Accepte monParrain = acceptes.get(moi);
        if (monParrain != null) {
            out.append("P\t").append(monParrain.parrain()).append('\t').append(nom(monParrain.parrain())).append('\n');
        }
        for (UUID parrain : propositions.getOrDefault(moi, Set.of())) {
            out.append("I\t").append(parrain).append('\t').append(nom(parrain)).append('\n');
        }
        for (Accepte a : acceptes.values()) {
            if (a.parrain().equals(moi)) {
                long[] v = jeu.getOrDefault(a.filleul(), new long[]{0, 0});
                out.append("F\t").append(a.filleul()).append('\t').append(nom(a.filleul())).append("\tA\t")
                        .append(v[0]).append('\t').append(v[1]).append('\n');
            }
        }
        propositions.forEach((filleul, parrains) -> {
            if (parrains.contains(moi)) {
                out.append("F\t").append(filleul).append('\t').append(nom(filleul)).append("\tP\t0\t0\n");
            }
        });
        for (UUID ami : contacts.store.get(moi).friends) {
            if (parrainable(ami) && !cycle(moi, ami) && !propositions.getOrDefault(ami, Set.of()).contains(moi)) {
                String ou = contacts.store.get(ami).invisible ? null : contacts.serverOf(ami);
                out.append("E\t").append(ami).append('\t').append(nom(ami)).append('\t').append(ou == null ? "" : ou)
                        .append('\n');
            }
        }
        Demande d = demandes.get(moi);
        if (d != null) {
            out.append("D\t").append(d.etat).append('\n');
        }
        return out.toString();
    }

    private String parrainer(UUID moi, String texte) {
        UUID filleul = cible(texte);
        if (filleul == null) {
            return "err:unknown";
        }
        if (filleul.equals(moi)) {
            return "err:self";
        }
        if (liste == null) {
            return "err:indisponible";
        }
        if (!surKixster(moi)) {
            return "err:not-allowed";
        }
        if (!parrainable(filleul)) {
            return "err:already:" + nom(filleul);
        }
        if (cycle(moi, filleul)) {
            return "err:cycle:" + nom(filleul);
        }
        if (contacts.store.get(filleul).blocked.contains(moi)) {
            // Bloque par ce joueur : la proposition n'arrive pas, et rien ne le lui apprend.
            return "ok:" + nom(filleul);
        }
        Set<UUID> p = propositions.computeIfAbsent(filleul, u -> new LinkedHashSet<>());
        if (p.contains(moi)) {
            return "err:pending:" + nom(filleul);
        }
        long miennes = propositions.values().stream().filter(s -> s.contains(moi)).count();
        if (miennes >= MAX_PROPOSITIONS) {
            if (p.isEmpty()) {
                propositions.remove(filleul);
            }
            return "err:too-many";
        }
        p.add(moi);
        sauver();
        contacts.tell(filleul, Contacts.prefix().append(Component.text(nom(moi), NamedTextColor.WHITE))
                .append(Component.text(" te propose de te parrainer sur Kixster : ", NamedTextColor.GRAY))
                .append(lien("/parrainage")));
        logger.info("[KaliumRelay] Parrainage propose : " + nom(moi) + " -> " + nom(filleul));
        return "ok:" + nom(filleul);
    }

    private String accepter(UUID moi, UUID parrain) {
        Set<UUID> p = propositions.get(moi);
        if (parrain == null || p == null || !p.contains(parrain)) {
            return "err:none";
        }
        if (!parrainable(moi)) {
            propositions.remove(moi);
            sauver();
            return "err:already:" + nom(moi);
        }
        if (cycle(parrain, moi)) {
            p.remove(parrain);
            if (p.isEmpty()) {
                propositions.remove(moi);
            }
            sauver();
            return "err:cycle:" + nom(parrain);
        }
        boolean dejaSurKixster = surKixster(moi);
        // Un seul parrain : les autres propositions tombent.
        propositions.remove(moi);
        acceptes.put(moi, new Accepte(moi, parrain, System.currentTimeMillis()));
        if (!dejaSurKixster) {
            autorises.put(moi, "parrain:" + parrain);
        }
        Demande d = demandes.get(moi);
        if (d != null && d.etat.equals("attente")) {
            clore(d, true, "parrainage");
        }
        sauver();
        contacts.tell(parrain, Contacts.prefix().append(Component.text(nom(moi), NamedTextColor.WHITE))
                .append(Component.text(dejaSurKixster ? " a accepté ton parrainage." : " a accepté ton parrainage : il peut "
                        + "rejoindre Kixster.", NamedTextColor.GREEN)));
        logger.info("[KaliumRelay] Parrainage accepte : " + nom(parrain) + " -> " + nom(moi));
        return "ok:" + nom(parrain);
    }

    private String demander(UUID moi, String discord) {
        String propre = Contacts.clean(discord);
        if (propre.length() < 2 || propre.length() > MAX_DISCORD) {
            return "err:bad";
        }
        if (surKixster(moi)) {
            return "err:already";
        }
        Demande d = demandes.get(moi);
        if (d != null && d.etat.equals("attente")) {
            return "err:pending";
        }
        d = new Demande(moi);
        d.discord = propre.replace('\t', ' ');
        d.date = System.currentTimeMillis();
        demandes.put(moi, d);
        sauver();
        String texte = nom(moi) + (bedrock(moi) ? " (Bedrock)" : "") + " demande la whitelist de Kixster (Discord : "
                + d.discord + ").";
        for (Player admin : server.getAllPlayers()) {
            if (config.isAdmin(admin.getUsername(), admin.getUniqueId())) {
                admin.sendMessage(Contacts.info(texte));
            }
        }
        logger.info("[KaliumRelay] Demande de liste blanche : " + texte);
        return "ok";
    }

    private String listeDemandes() {
        StringBuilder out = new StringBuilder("ok\n");
        for (Demande d : demandes.values()) {
            if (d.etat.equals("attente")) {
                out.append("D\t").append(d.joueur).append('\t').append(nom(d.joueur)).append('\t').append(d.discord)
                        .append('\t').append(d.date).append('\t').append(bedrock(d.joueur)).append('\n');
            }
        }
        return out.toString();
    }

    /** Parrainages acceptes (le plus recent d'abord) : pour voir qui a fait venir qui (sanctions). */
    private String historique() {
        List<Accepte> tous = new ArrayList<>(acceptes.values());
        tous.sort((a, b) -> Long.compare(b.date(), a.date()));
        StringBuilder out = new StringBuilder("ok\n");
        for (Accepte a : tous) {
            long[] v = jeu.getOrDefault(a.filleul(), new long[]{0, 0});
            out.append("H\t").append(a.filleul()).append('\t').append(nom(a.filleul())).append('\t').append(a.parrain())
                    .append('\t').append(nom(a.parrain())).append('\t').append(a.date()).append('\t').append(v[0])
                    .append('\n');
        }
        return out.toString();
    }

    /**
     * 1.10.2 : demandes de whitelist traitees, la plus recente d'abord (lignes « H » : joueur, pseudo, Discord, date de
     * la demande, etat, traitee par, date du traitement, Bedrock).
     */
    private String historiqueDemandes() {
        StringBuilder out = new StringBuilder("ok\n");
        for (int i = traitees.size() - 1; i >= 0; i--) {
            Demande d = traitees.get(i);
            out.append("H\t").append(d.joueur).append('\t').append(nom(d.joueur)).append('\t').append(d.discord)
                    .append('\t').append(d.date).append('\t').append(d.etat).append('\t').append(d.admin).append('\t')
                    .append(d.traite).append('\t').append(bedrock(d.joueur)).append('\n');
        }
        return out.toString();
    }

    private String traiter(UUID admin, UUID joueur, boolean accepter) {
        Demande d = joueur == null ? null : demandes.get(joueur);
        if (d == null || !d.etat.equals("attente")) {
            return "err:none";
        }
        clore(d, accepter, nom(admin));
        if (accepter) {
            autorises.put(joueur, "admin:" + nom(admin));
        }
        sauver();
        contacts.tell(joueur, accepter
                ? Contacts.prefix().append(Component.text("Ta demande de whitelist est acceptée : tu peux rejoindre "
                + "Kixster.", NamedTextColor.GREEN))
                : Contacts.info("Ta demande de whitelist pour Kixster a été refusée."));
        logger.info("[KaliumRelay] Demande de liste blanche de " + nom(joueur) + (accepter ? " acceptee" : " refusee")
                + " par " + nom(admin));
        return "ok:" + nom(joueur);
    }
}
