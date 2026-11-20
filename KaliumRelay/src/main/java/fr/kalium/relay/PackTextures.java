package fr.kalium.relay;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 1.12.0 - pack de ressources de KaLium envoyé par le proxy (demande de LeKiwi06 du 20/11/2026 : « que le pack
 * s'active sur tout le serveur kalium, c'est embêtant d'avoir à le reload chaque fois qu'on rejoint kixster »).
 *
 * Avant, KS_Textures le proposait à chaque arrivée sur Kixster ; Velocity retire les packs d'un serveur quand le joueur
 * en change (origine DOWNSTREAM_SERVER), d'où un nouveau téléchargement à chaque retour. Envoyé par le proxy (origine
 * PLUGIN_ON_PROXY), il reste chargé d'un serveur à l'autre : proposé une seule fois, à l'arrivée sur KaLium (premier
 * serveur rejoint). Facultatif ; jamais aux joueurs Bedrock (identifiant Floodgate, qui commence par des zéros).
 *
 * Même identifiant que celui de KS_Textures : son pack.url doit rester VIDE sur Kixster, sinon le pack du serveur
 * remplacerait celui du proxy et serait retiré au changement de serveur. relay.properties : pack-url, pack-sha1,
 * pack-message (vide : rien n'est envoyé).
 */
final class PackTextures {

    private static final UUID ID_PACK = UUID.nameUUIDFromBytes("KaLium-textures".getBytes(StandardCharsets.UTF_8));

    private final ProxyServer server;
    private final RelayConfig config;
    private final Logger logger;

    PackTextures(ProxyServer server, RelayConfig config, Logger logger) {
        this.server = server;
        this.config = config;
        this.logger = logger;
        logger.info("[KaliumRelay] Pack de ressources : " + (actif() ? "propose a l'arrivee sur KaLium (Java)"
                : "non propose (pack-url ou pack-sha1 vide dans relay.properties)"));
    }

    private boolean actif() {
        return !config.packUrl().isEmpty() && config.packSha1().matches("[0-9a-f]{40}");
    }

    @Subscribe
    public void onConnecte(ServerPostConnectEvent event) {
        Player joueur = event.getPlayer();
        if (event.getPreviousServer() != null || !actif() || joueur.getUniqueId().getMostSignificantBits() == 0) {
            return;
        }
        try {
            ResourcePackInfo pack = server.createResourcePackBuilder(config.packUrl())
                    .setId(ID_PACK)
                    .setHash(HexFormat.of().parseHex(config.packSha1()))
                    .setShouldForce(false)
                    .setPrompt(Component.text(config.packMessage(), NamedTextColor.GOLD))
                    .build();
            joueur.sendResourcePackOffer(pack);
        } catch (RuntimeException e) {
            logger.warn("[KaliumRelay] Pack de ressources non envoye a " + joueur.getUsername() + " : " + e);
        }
    }
}
