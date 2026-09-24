package fr.kalium.relay;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.google.inject.Inject;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Plugin Velocity (proxy) : relais HTTP entre KalGames et KalBingo pour l'affectation des joueurs
 * a une partie Bingo (voir RelayHttpServer pour le detail du "pourquoi"). Ne fait QUE ca (plus,
 * depuis la 1.1.0, le routage initial des reconnexions en cours de partie - voir
 * onChooseInitialServer) - n'intercepte, ne modifie et ne relaie aucun autre trafic de jeu normal.
 *
 * NOTE (1.0.1) : @Inject vient de com.google.inject (Guice), PAS javax.inject. Le premier essai
 * (javax.inject.Inject, pour eviter la dependance a Guice) a echoue au demarrage sur le proxy :
 * "No injectable constructor for type KaliumRelay" - le jar livre ne contenait QUE nos classes
 * compilees (le .jar de javax.inject sert uniquement a compiler, jamais embarque dans le plugin,
 * meme convention que velocity-api/paper-api), et rien ne garantit que javax.inject.Inject soit
 * present/resolu identiquement sur le classpath runtime d'un plugin Velocity. Le systeme de
 * plugins de Velocity repose entierement sur Guice : son propre Injector garantit que
 * com.google.inject.Inject est toujours present et correctement resolu, donc plus sur.
 */
@Plugin(id = "kaliumrelay", name = "KaliumRelay", version = "1.1.1",
        description = "Relais HTTP entre KalGames et KalBingo, independant de la presence d'un joueur.",
        authors = {"KaLium"})
public final class KaliumRelay {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private RelayHttpServer http;
    /** Registre "joueur en partie" (reconnexion en cours de partie, voir onChooseInitialServer). */
    private final ActiveGameRegistry activeGameRegistry = new ActiveGameRegistry();

    @Inject
    public KaliumRelay(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        RelayConfig config = RelayConfig.loadOrCreate(dataDirectory, logger);
        http = new RelayHttpServer(config, logger, activeGameRegistry);
        try {
            http.start();
            logger.info("[KaliumRelay] Serveur HTTP relais demarre sur le port " + config.port() + ".");
        } catch (Exception e) {
            logger.error("[KaliumRelay] Impossible de demarrer le serveur HTTP relais.", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (http != null) {
            http.stop();
        }
    }

    /**
     * Reconnexion en cours de partie (1.1.0, demande explicite de l'utilisateur : "si un joueur
     * est deconnecte durant une partie et qu'il se reconnecte avant la fin de la partie il faut
     * que le proxy le renvoi directement sur la partie"). KalBingo enregistre chaque joueur comme
     * "en partie sur kixster" au lancement de sa partie (voir ActiveGameRegistry /
     * RelayHttpServer.handleActiveGame) ; ici, a CHAQUE nouvelle connexion au proxy (y compris une
     * reconnexion apres deconnexion), on verifie ce registre AVANT de laisser Velocity choisir le
     * serveur initial habituel (try list de velocity.toml, normalement kal-games/lobby) - si le
     * joueur y figure, on le redirige directement vers ce serveur plutot que de le laisser arriver
     * sur kal-games puis devoir en repartir. Ne modifie rien si le joueur n'y figure pas (comportement
     * de routage initial totalement inchange), ou si le serveur enregistre n'est pas/plus declare
     * sur ce proxy (garde-fou : pas de config incoherente, on se contente de logguer).
     */
    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Optional<String> serverName = activeGameRegistry.get(event.getPlayer().getUniqueId());
        if (serverName.isEmpty()) {
            return;
        }
        Optional<RegisteredServer> target = server.getServer(serverName.get());
        if (target.isEmpty()) {
            logger.warn("[KaliumRelay] " + event.getPlayer().getUsername() + " est enregistre en partie sur '"
                    + serverName.get() + "', mais ce serveur n'est pas declare sur ce proxy (velocity.toml) - "
                    + "routage initial inchange.");
            return;
        }
        event.setInitialServer(target.get());
        logger.info("[KaliumRelay] " + event.getPlayer().getUsername() + " reconnecte directement sur '"
                + serverName.get() + "' (partie Bingo en cours).");
    }
}
