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

    /**
     * Jeton par defaut - IDENTIQUE a celui deja renseigne dans network.relay-token cote KalGames
     * ET KalBingo (config.yml), pour que les trois marchent ensemble sans etape de copier-coller
     * manuelle. Un vrai secret aleatoire serait genere si la securite de ce canal devenait
     * sensible (il ne transite que des UUID de partie Bingo et des noms de serveur) - a changer
     * ICI et dans les deux config.yml a la fois si besoin.
     */
    private static final String DEFAULT_TOKEN = "5e844e1f-0aac-4bd8-bb46-71c83431c9a8";

    private final int port;
    private final String token;

    private RelayConfig(int port, String token) {
        this.port = port;
        this.token = token;
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
            if (!props.containsKey("token")) {
                props.setProperty("token", DEFAULT_TOKEN);
                changed = true;
            }
            if (changed) {
                try (OutputStream out = Files.newOutputStream(file)) {
                    props.store(out, "KaliumRelay - port d'ecoute et jeton partage "
                            + "(a copier tel quel dans network.relay-url / network.relay-token "
                            + "cote KalGames ET KalBingo, config.yml)");
                }
            }
            int port = Integer.parseInt(props.getProperty("port").trim());
            String token = props.getProperty("token").trim();
            logger.info("[KaliumRelay] Jeton partage (a copier dans KalGames/KalBingo si besoin) : " + token);
            return new RelayConfig(port, token);
        } catch (IOException e) {
            logger.error("[KaliumRelay] Impossible de charger/creer relay.properties, valeurs par defaut utilisees.", e);
            return new RelayConfig(DEFAULT_PORT, UUID.randomUUID().toString());
        }
    }
}
