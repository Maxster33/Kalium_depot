package fr.kalium.menu.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 2.9.0 (LeKiwi06, 03/10/2026) : QR codes en main secondaire (KLM_Hub sur le lobby). Le menu « Recherche de joueurs »
 * de la boussole propose un bouton par QR code tant qu'un plugin du serveur fournit ce service ; sans lui, seuls les
 * liens sont affichés.
 *
 * Déclaration, au démarrage du plugin :
 * <pre>
 * getServer().getServicesManager().register(QrCodes.class, qrCodes, this, ServicePriority.Normal);
 * </pre>
 */
public interface QrCodes {

    /** Plugin qui fournit les QR codes. */
    Plugin owner();

    /**
     * Met dans la main secondaire du joueur la carte du QR code de ce lien, retirée toute seule après quelques
     * secondes. Renvoie false (le joueur est prévenu) si ce n'est pas possible, ex. main secondaire déjà prise.
     */
    boolean donner(Player player, String lien, Component nom);
}
