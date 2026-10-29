package fr.kalium.textures;

import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * KS_Textures (LeKiwi06, 29/10/2026 : « tu peux faire le texture pack ») : les textures des objets custom de Kixster
 * (dossier visuels-objets du dépôt), sur Java et sur Bedrock.
 *
 * Choix de LeKiwi06 : pack hébergé sur minepack.fr ; pack facultatif, un joueur Java qui le refuse voit l'image
 * d'aujourd'hui ; mêmes textures sur Bedrock (geyser-bedrock) ; un seul plugin d'affichage plutôt qu'une marque dans
 * chaque plugin.
 *
 * Fonctionnement : les objets ne changent pas sur le serveur. Dans ce qui est envoyé aux joueurs, un objet custom
 * reconnu ({@link Regles}) reçoit sa texture en custom_model_data ({@link Reecriture}). Le pack remplace la définition
 * vanilla de l'objet de base (potion, bateau...) par un choix sur cette marque, avec la définition vanilla en dernier
 * recours : sans le pack, ou pour un objet non marqué, rien ne change. Sur le proxy, Geyser lit la même marque.
 *
 * Le pack est proposé (facultatif) à chaque joueur Java qui arrive sur le serveur, si config.yml donne son adresse et
 * son SHA-1 ; pas aux joueurs Bedrock (identifiant Floodgate, qui commence par des zéros).
 *
 * Code du serveur Paper 26.3 (paquets, composants) : à revérifier à chaque mise à jour de Minecraft. Ne pas recharger
 * le plugin à chaud (/reload) : les connexions déjà ouvertes garderaient l'ancienne réécriture.
 */
public final class KSTextures extends JavaPlugin implements Listener {

    private static final Key ECOUTEUR = Key.key("ks_textures", "reecriture");
    private static final String NOM = "ks_textures";
    /** Toujours le même identifiant : une nouvelle version du pack remplace l'ancienne chez le joueur. */
    private static final UUID ID_PACK = UUID.nameUUIDFromBytes("KaLium-textures".getBytes(StandardCharsets.UTF_8));

    private ResourcePackRequest demande;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        demande = demande();
        ChannelInitializeListenerHolder.addListener(ECOUTEUR, canal -> {
            if (canal.pipeline().get("packet_handler") != null) {
                canal.pipeline().addBefore("packet_handler", NOM, new Reecriture(getLogger()));
            } else {
                canal.pipeline().addLast(NOM, new Reecriture(getLogger()));
            }
        });
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(Regles.TEXTURES.size() + " textures d'objets ; pack "
                + (demande == null ? "non propose (adresse ou SHA-1 vide dans config.yml)" : "propose aux joueurs Java"));
    }

    @Override
    public void onDisable() {
        ChannelInitializeListenerHolder.removeListener(ECOUTEUR);
    }

    private ResourcePackRequest demande() {
        String adresse = getConfig().getString("pack.url", "").trim();
        String sha1 = getConfig().getString("pack.sha1", "").trim().toLowerCase();
        if (adresse.isEmpty() || !sha1.matches("[0-9a-f]{40}")) {
            return null;
        }
        String texte = getConfig().getString("pack.message", "Textures des objets de KaLium (facultatif)");
        return ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(ID_PACK, URI.create(adresse), sha1))
                .required(false)
                .replace(false)
                .prompt(Component.text(texte, NamedTextColor.GOLD))
                .build();
    }

    @EventHandler
    public void onArrivee(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        if (demande != null && joueur.getUniqueId().getMostSignificantBits() != 0) {
            joueur.sendResourcePacks(demande);
        }
    }
}
