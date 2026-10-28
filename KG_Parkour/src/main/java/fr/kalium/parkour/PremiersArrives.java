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
 * finir le parkour ») : le premier joueur qui finit chaque map du Parcours est récompensé, une seule fois par map. Ses
 * réponses : une fois par map, parties publiques et privées (jamais l'entraînement), opérateurs compris.
 *
 * 1.6.0 (LeKiwi06, 28/10/2026 : « c'est pas 1000 emeraudes qu'il doit recevoir, mais un coffre de récompense d'une
 * valeur de 1000 emeraude, c'est les joueurs qui reussissent ensuite qui reçoivent une simple somme en emeraudes ») :
 * le premier reçoit un coffre tiré du niveau « mois / exceptionnelle » des tables de KG_Rewards (valeur 1 000) ; chaque
 * joueur qui finit ensuite la map reçoit des émeraudes (réglage « map-finish-emeralds », 500), une fois par map (le
 * premier n'en reçoit pas en plus).
 *
 * Gardé dans plugins/KG_Parkour/premiers.yml : maps.<arène> (le premier ; « recompense: coffre », ou « emeraudes: n »
 * pour ceux de la 1.4.0) et arrivees.<arène>.<uuid> (les suivants). Les récompenses partent par KG_Rewards (coffre
 * « Non ouvert » de KS_RewardsGUI sur la survie) ; si KG_Rewards est absent ou trop ancien, elles restent marquées
 * « envoye: false » et repartent au démarrage suivant.
 */
final class PremiersArrives {

    /** 1.6.0 : niveau des tables de butin de KG_Rewards tiré pour le premier (choix de LeKiwi06, 28/10/2026). */
    static final String PERIODE_COFFRE = "mois";
    static final String NIVEAU_COFFRE = "exceptionnelle";

    /** Ce que gagne un joueur à l'arrivée : le coffre du premier, des émeraudes, ou rien. */
    record Gain(boolean coffre, long emeraudes) {
        static final Gain AUCUN = new Gain(false, 0);

        boolean rien() {
            return !coffre && emeraudes <= 0;
        }
    }

    private final KGParkour plugin;
    private final File fichier;
    private final YamlConfiguration yaml;

    PremiersArrives(KGParkour plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "premiers.yml");
        this.yaml = YamlConfiguration.loadConfiguration(fichier);
    }

    /**
     * Arrivée d'un coureur (course publique ou privée, pas l'entraînement) : le premier à finir cette map gagne le
     * coffre (si « coffre » est vrai), les suivants des émeraudes à leur première arrivée sur la map (0 = aucune).
     */
    Gain arrivee(String arena, String nomMap, Player joueur, long millis, boolean coffre, long emeraudes) {
        String uuid = joueur.getUniqueId().toString();
        // Coffre désactivé : tout le monde est traité comme un suivant (émeraudes).
        if (coffre && !yaml.isConfigurationSection("maps." + arena)) {
            String chemin = "maps." + arena;
            ecrire(chemin, nomMap, joueur, millis);
            yaml.set(chemin + ".recompense", "coffre");
            yaml.set(chemin + ".envoye", false);
            sauver();
            plugin.getLogger().info("Premier à finir la map " + arena + " : " + joueur.getName() + " (" + uuid
                    + "), coffre " + PERIODE_COFFRE + " / " + NIVEAU_COFFRE + ".");
            envoyer(chemin);
            return new Gain(true, 0);
        }
        String chemin = "arrivees." + arena + "." + uuid;
        if (emeraudes <= 0 || uuid.equals(yaml.getString("maps." + arena + ".joueur")) || yaml.isConfigurationSection(chemin)) {
            return Gain.AUCUN;
        }
        ecrire(chemin, nomMap, joueur, millis);
        yaml.set(chemin + ".emeraudes", emeraudes);
        yaml.set(chemin + ".envoye", false);
        sauver();
        plugin.getLogger().info("Première arrivée sur la map " + arena + " : " + joueur.getName() + " (" + uuid + "), "
                + emeraudes + " émeraudes.");
        envoyer(chemin);
        return new Gain(false, emeraudes);
    }

    private void ecrire(String chemin, String nomMap, Player joueur, long millis) {
        yaml.set(chemin + ".map", nomMap);
        yaml.set(chemin + ".joueur", joueur.getUniqueId().toString());
        yaml.set(chemin + ".nom", joueur.getName());
        yaml.set(chemin + ".temps", millis);
        yaml.set(chemin + ".date", System.currentTimeMillis());
    }

    /** Récompenses pas encore parties (KG_Rewards absent au moment de l'arrivée) : nouvel essai. */
    void envoyerEnAttente() {
        ConfigurationSection maps = yaml.getConfigurationSection("maps");
        if (maps != null) {
            for (String arena : maps.getKeys(false)) {
                if (!maps.getBoolean(arena + ".envoye", true)) {
                    envoyer("maps." + arena);
                }
            }
        }
        ConfigurationSection arrivees = yaml.getConfigurationSection("arrivees");
        if (arrivees != null) {
            for (String arena : arrivees.getKeys(false)) {
                ConfigurationSection joueurs = arrivees.getConfigurationSection(arena);
                for (String uuid : joueurs == null ? java.util.Set.<String>of() : joueurs.getKeys(false)) {
                    if (!joueurs.getBoolean(uuid + ".envoye", true)) {
                        envoyer("arrivees." + arena + "." + uuid);
                    }
                }
            }
        }
    }

    /** Envoie la récompense de cette section (coffre ou émeraudes) et la marque envoyée si KG_Rewards l'a prise. */
    private void envoyer(String chemin) {
        Plugin rewards = Bukkit.getPluginManager().getPlugin("KG_Rewards");
        if (rewards == null || !rewards.isEnabled()) {
            plugin.getLogger().warning("KG_Rewards absent : récompense " + chemin
                    + " gardée dans premiers.yml (renvoyée au prochain démarrage).");
            return;
        }
        UUID joueur = UUID.fromString(yaml.getString(chemin + ".joueur"));
        String nom = yaml.getString(chemin + ".nom");
        String map = yaml.getString(chemin + ".map");
        boolean coffre = "coffre".equals(yaml.getString(chemin + ".recompense"));
        try {
            fr.kalium.rewards.KGRewards kg = (fr.kalium.rewards.KGRewards) rewards;
            if (coffre) {
                if (!kg.envoyerCoffre(joueur, nom, "Premier à finir le parcours « " + map + " »", PERIODE_COFFRE, NIVEAU_COFFRE)) {
                    plugin.getLogger().warning("Coffre non tiré par KG_Rewards : récompense " + chemin
                            + " gardée dans premiers.yml (renvoyée au prochain démarrage).");
                    return;
                }
            } else {
                String raison = chemin.startsWith("maps.")
                        ? "Premier à finir le parcours « " + map + " »"
                        : "Parcours « " + map + " » terminé";
                kg.envoyerEmeraudes(joueur, nom, raison, yaml.getLong(chemin + ".emeraudes"));
            }
        } catch (LinkageError | ClassCastException e) {
            plugin.getLogger().warning("KG_Rewards trop ancien (" + (coffre ? "1.5.0" : "1.4.0") + " minimum) : récompense "
                    + chemin + " gardée dans premiers.yml (renvoyée au prochain démarrage).");
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
