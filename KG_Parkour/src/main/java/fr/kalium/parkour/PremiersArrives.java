package fr.kalium.parkour;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 1.4.0 (demande de LeKiwi06, 26/10/2026 : « fais une récompense de 1000 émeraudes pour la 1ère personne qui arrive à
 * finir le parkour ») : le premier joueur qui finit chaque map du Parcours gagne des émeraudes (réglage du jeu
 * « first-map-emeralds », 1 000 par défaut), une seule fois par map. Ses réponses : une fois par map, parties
 * publiques et privées (jamais l'entraînement), opérateurs compris.
 *
 * Le gagnant de chaque map est gardé dans plugins/KG_Parkour/premiers.yml (identifiant de l'arène). La récompense part
 * par KG_Rewards (coffre « Non ouvert » de KS_RewardsGUI sur la survie) ; si KG_Rewards est absent ou trop ancien, elle
 * reste marquée « envoye: false » et repart au démarrage suivant.
 */
final class PremiersArrives {

    private final KGParkour plugin;
    private final File fichier;
    private final YamlConfiguration yaml;

    PremiersArrives(KGParkour plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "premiers.yml");
        this.yaml = YamlConfiguration.loadConfiguration(fichier);
    }

    /**
     * Arrivée d'un coureur (course publique ou privée, pas l'entraînement) : s'il est le premier à finir cette map, il
     * est retenu et sa récompense envoyée. Renvoie le nombre d'émeraudes gagnées, 0 sinon.
     */
    long arrivee(String arena, String nomMap, Player joueur, long millis, long emeraudes) {
        if (emeraudes <= 0 || yaml.isConfigurationSection("maps." + arena)) {
            return 0;
        }
        String chemin = "maps." + arena;
        yaml.set(chemin + ".map", nomMap);
        yaml.set(chemin + ".joueur", joueur.getUniqueId().toString());
        yaml.set(chemin + ".nom", joueur.getName());
        yaml.set(chemin + ".temps", millis);
        yaml.set(chemin + ".date", System.currentTimeMillis());
        yaml.set(chemin + ".emeraudes", emeraudes);
        yaml.set(chemin + ".envoye", false);
        sauver();
        plugin.getLogger().info("Premier à finir la map " + arena + " : " + joueur.getName() + " (" + joueur.getUniqueId()
                + "), " + emeraudes + " émeraudes.");
        envoyer(arena);
        return emeraudes;
    }

    /** Récompenses pas encore parties (KG_Rewards absent au moment de l'arrivée) : nouvel essai. */
    void envoyerEnAttente() {
        ConfigurationSection maps = yaml.getConfigurationSection("maps");
        if (maps == null) {
            return;
        }
        for (String arena : maps.getKeys(false)) {
            if (!maps.getBoolean(arena + ".envoye", true)) {
                envoyer(arena);
            }
        }
    }

    private void envoyer(String arena) {
        String chemin = "maps." + arena;
        Plugin rewards = Bukkit.getPluginManager().getPlugin("KG_Rewards");
        if (rewards == null || !rewards.isEnabled()) {
            plugin.getLogger().warning("KG_Rewards absent : récompense du premier sur la map " + arena
                    + " gardée dans premiers.yml (renvoyée au prochain démarrage).");
            return;
        }
        try {
            ((fr.kalium.rewards.KGRewards) rewards).envoyerEmeraudes(UUID.fromString(yaml.getString(chemin + ".joueur")),
                    yaml.getString(chemin + ".nom"), "Premier à finir le parcours « " + yaml.getString(chemin + ".map") + " »",
                    yaml.getLong(chemin + ".emeraudes"));
        } catch (LinkageError | ClassCastException e) {
            plugin.getLogger().warning("KG_Rewards trop ancien (1.4.0 minimum) : récompense du premier sur la map " + arena
                    + " gardée dans premiers.yml (renvoyée au prochain démarrage).");
            return;
        }
        yaml.set(chemin + ".envoye", true);
        sauver();
    }

    private void sauver() {
        try {
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("premiers.yml non enregistré : " + e.getMessage());
        }
    }
}
