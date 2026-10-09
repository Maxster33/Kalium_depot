package fr.kalium.relay;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/**
 * Config minimale du relais : port d'ecoute et jeton partage. Fichier .properties simple (pas de
 * dependance YAML supplementaire) dans le dossier de donnees du plugin Velocity - genere avec des
 * valeurs par defaut au premier demarrage si absent.
 */
final class RelayConfig {

    /** Port ouvert par l'utilisateur sur l'hebergement Minestrator du proxy (46199). */
    private static final int DEFAULT_PORT = 46199;

    // 1.1.1 : plus aucun jeton ecrit dans le code (l'ancien jeton fixe, publie dans le depot public, a du etre
    // change le 24/09/2026 - voir REGLES.md, section 2). Si relay.properties n'a pas de jeton, un jeton
    // aleatoire y est genere ; il est ensuite recopie A LA MAIN dans les config.yml de KalGames
    // (bingo.relay-token) et KalBingo (network.relay-token) sur les serveurs. Il n'est jamais affiche dans la
    // console.

    /** 1.2.0 : joueurs autorises a utiliser /server (pseudos ou UUID, en minuscules). */
    private static final String DEFAULT_ADMINS = "LeKiwi06,Maaxster";

    private final int port;
    private final String token;
    private final java.util.Set<String> admins;

    /** 1.6.0 (KLM_Contacts) : nombre maximal d'amis par joueur. */
    private static final int DEFAULT_MAX_FRIENDS = 100;
    /** 1.6.0 (KLM_Contacts) : serveurs qu'on ne rejoint pas par « Rejoindre son serveur » (on n'y entre que par une partie). */
    private static final String DEFAULT_NO_JOIN = "serveur-jeux";

    private int maxFriends = DEFAULT_MAX_FRIENDS;
    private java.util.Set<String> noJoin = parseAdmins(DEFAULT_NO_JOIN);

    private RelayConfig(int port, String token, java.util.Set<String> admins) {
        this.port = port;
        this.token = token;
        this.admins = admins;
    }

    /** 1.7.0 (KLM_Contacts) : nombre maximal de joueurs dans un groupe de jeu. */
    private int maxGroup = 8;

    int maxGroup() {
        return maxGroup;
    }

    int maxFriends() {
        return maxFriends;
    }

    /** Ce serveur est-il ferme a « Rejoindre son serveur » (nom Velocity, majuscules ignorees) ? */
    boolean noJoin(String serverName) {
        return noJoin.contains(serverName.toLowerCase(java.util.Locale.ROOT));
    }

    /** Le joueur (pseudo ou UUID) est-il admin (autorise a utiliser /server) ? */
    boolean isAdmin(String name, UUID uuid) {
        return admins.contains(name.toLowerCase(java.util.Locale.ROOT)) || admins.contains(uuid.toString().toLowerCase(java.util.Locale.ROOT));
    }

    private static java.util.Set<String> parseAdmins(String text) {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                out.add(part.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return out;
    }

    int port() {
        return port;
    }

    String token() {
        return token;
    }

    static RelayConfig loadOrCreate(Path dataDirectory, Logger logger) {
        try {
            Files.createDirectories(dataDirectory);
            Path file = dataDirectory.resolve("relay.properties");
            Properties props = new Properties();
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            }
            boolean changed = false;
            if (!props.containsKey("port")) {
                props.setProperty("port", String.valueOf(DEFAULT_PORT));
                changed = true;
            }
            if (!props.containsKey("admins")) {
                props.setProperty("admins", DEFAULT_ADMINS); // 1.2.0 : seuls eux peuvent utiliser /server
                changed = true;
            }
            // 1.6.0 (KLM_Contacts) : plafond d'amis et serveurs fermes a « Rejoindre son serveur ».
            if (!props.containsKey("contacts-max-friends")) {
                props.setProperty("contacts-max-friends", String.valueOf(DEFAULT_MAX_FRIENDS));
                changed = true;
            }
            if (!props.containsKey("contacts-max-group")) {
                props.setProperty("contacts-max-group", "8");
                changed = true;
            }
            if (!props.containsKey("contacts-no-join")) {
                props.setProperty("contacts-no-join", DEFAULT_NO_JOIN);
                changed = true;
            }
            boolean generated = false;
            if (props.getProperty("token", "").isBlank()) {
                props.setProperty("token", UUID.randomUUID().toString());
                changed = true;
                generated = true;
            }
            if (changed) {
                try (OutputStream out = Files.newOutputStream(file)) {
                    props.store(out, "KaliumRelay - port d'ecoute et jeton partage "
                            + "(jeton a recopier dans bingo.relay-token de KalGames et network.relay-token "
                            + "de KalBingo, config.yml des serveurs - jamais dans le depot git)");
                }
            }
            if (generated) {
                logger.warn("[KaliumRelay] Nouveau jeton genere dans relay.properties : le recopier dans les "
                        + "config.yml de KalGames et KalBingo sur les serveurs.");
            }
            int port = Integer.parseInt(props.getProperty("port").trim());
            String token = props.getProperty("token").trim();
            RelayConfig config = new RelayConfig(port, token, parseAdmins(props.getProperty("admins", DEFAULT_ADMINS)));
            try {
                config.maxFriends = Math.max(1, Integer.parseInt(props.getProperty("contacts-max-friends").trim()));
            } catch (NumberFormatException e) {
                config.maxFriends = DEFAULT_MAX_FRIENDS;
            }
            config.noJoin = parseAdmins(props.getProperty("contacts-no-join", DEFAULT_NO_JOIN));
            try {
                config.maxGroup = Math.max(2, Integer.parseInt(props.getProperty("contacts-max-group").trim()));
            } catch (NumberFormatException e) {
                config.maxGroup = 8;
            }
            return config;
        } catch (IOException e) {
            logger.error("[KaliumRelay] Impossible de charger/creer relay.properties, valeurs par defaut utilisees.", e);
            return new RelayConfig(DEFAULT_PORT, UUID.randomUUID().toString(), parseAdmins(DEFAULT_ADMINS));
        }
    }
}
