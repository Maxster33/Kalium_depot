package fr.kalium.decapitator;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1.3.0 (LeKiwi06, 16/10/2026) : têtes de joueurs. Chaque joueur reçoit en secret un type de mob tiré au hasard. Un mob
 * de ce type, renommé avec le pseudo du joueur puis tué par un joueur, lâche la tête de ce joueur (sans limite).
 *
 * - Actif seulement si config.yml dit « tetes-de-joueurs: true » (Kixster SMP) ; ailleurs, rien n'est tiré.
 * - Pseudo : celui d'un joueur déjà venu sur ce serveur, sans tenir compte des majuscules.
 * - Tirage : parmi les créatures de tetes.txt, au hasard (pas de formule : le code est public, le tirage ne doit pas
 *   pouvoir être recalculé). Fait à la connexion, ou au premier mob renommé pour un joueur qui ne s'est pas reconnecté.
 * - Secret : aucune commande, aucun message, rien dans le journal du serveur. Données dans
 *   plugins/KS_Decapitator/tetes_de_joueurs.yml (lisible par qui a accès aux fichiers du serveur).
 * - Texture : le skin du joueur est noté à chaque connexion, pour que sa tête l'ait aussi quand il est hors ligne.
 * - /tetes : dès sa première tête de joueur obtenue, un joueur a des pages de plus avec tous les joueurs du serveur
 *   (liste à jour à chaque ouverture, nouveaux joueurs compris). Une tête de joueur compte dès qu'elle passe dans son
 *   inventaire (Collection, id « joueur:<uuid> »), reconnue au profil de la tête, quelle que soit sa provenance.
 */
final class TetesDeJoueurs implements Listener {

    /** Début de l'id d'une tête de joueur dans la collection (joueur:<uuid>). */
    static final String PREFIXE = "joueur:";

    private final KSDecapitator plugin;
    private final File fichier;
    /** Joueur -> type de mob tiré. */
    private final Map<UUID, EntityType> mobs = new HashMap<>();
    /** Joueur -> propriété « textures » de son profil (valeur, signature). */
    private final Map<UUID, ProfileProperty> skins = new HashMap<>();
    private final List<EntityType> tirables = new ArrayList<>();

    TetesDeJoueurs(KSDecapitator plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "tetes_de_joueurs.yml");
        for (String creature : KSDecapitator.creatures()) {
            EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(creature));
            if (type != null) {
                tirables.add(type);
            }
        }
        charger();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        boolean change = mobs.get(joueur.getUniqueId()) == null && mob(joueur.getUniqueId()) != null;
        for (ProfileProperty propriete : joueur.getPlayerProfile().getProperties()) {
            if ("textures".equals(propriete.getName()) && !propriete.equals(skins.get(joueur.getUniqueId()))) {
                skins.put(joueur.getUniqueId(), propriete);
                change = true;
            }
        }
        if (change) {
            sauver();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        Component nom = mob.customName();
        if (mob instanceof Player || mob.getKiller() == null || nom == null) {
            return;
        }
        String pseudo = PlainTextComponentSerializer.plainText().serialize(nom).strip();
        OfflinePlayer cible = plugin.getServer().getOfflinePlayerIfCached(pseudo);
        if (cible == null || !(cible.isOnline() || cible.hasPlayedBefore())) {
            return;
        }
        boolean nouveau = !mobs.containsKey(cible.getUniqueId());
        EntityType type = mob(cible.getUniqueId());
        if (nouveau) {
            sauver();
        }
        if (type == mob.getType()) {
            event.getDrops().add(tete(cible));
        }
    }

    /** Type de mob du joueur, tiré au hasard la première fois. */
    private EntityType mob(UUID joueur) {
        if (tirables.isEmpty()) {
            return null;
        }
        return mobs.computeIfAbsent(joueur, u -> tirables.get(ThreadLocalRandom.current().nextInt(tirables.size())));
    }

    /** Joueurs déjà venus sur le serveur, par ordre alphabétique (lu à chaque appel : les nouveaux y sont). */
    List<OfflinePlayer> joueurs() {
        List<OfflinePlayer> joueurs = new ArrayList<>();
        for (OfflinePlayer joueur : plugin.getServer().getOfflinePlayers()) {
            if (joueur.getName() != null) {
                joueurs.add(joueur);
            }
        }
        joueurs.sort(Comparator.comparing(joueur -> joueur.getName().toLowerCase(Locale.ROOT)));
        return joueurs;
    }

    /** « joueur:<uuid> » si l'objet est la tête d'un joueur déjà venu sur le serveur, sinon null. */
    static String idTete(ItemStack objet) {
        if (objet == null || objet.getType() != Material.PLAYER_HEAD
                || !(objet.getItemMeta() instanceof SkullMeta meta) || meta.getPlayerProfile() == null) {
            return null;
        }
        UUID id = meta.getPlayerProfile().getId();
        if (id == null) {
            return null;
        }
        OfflinePlayer joueur = Bukkit.getOfflinePlayer(id);
        return joueur.isOnline() || joueur.hasPlayedBefore() ? PREFIXE + id : null;
    }

    /** Tête de joueur avec son skin (profil du joueur en ligne, sinon skin noté à sa dernière connexion). */
    ItemStack tete(OfflinePlayer joueur) {
        PlayerProfile profil;
        if (joueur instanceof Player enLigne) {
            profil = enLigne.getPlayerProfile();
        } else {
            profil = plugin.getServer().createProfile(joueur.getUniqueId(), joueur.getName());
            ProfileProperty skin = skins.get(joueur.getUniqueId());
            if (skin != null) {
                profil.setProperty(skin);
            }
        }
        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) tete.getItemMeta();
        meta.setPlayerProfile(profil);
        tete.setItemMeta(meta);
        return tete;
    }

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection joueurs = yaml.getConfigurationSection("joueurs");
        if (joueurs == null) {
            return;
        }
        for (String cle : joueurs.getKeys(false)) {
            try {
                UUID joueur = UUID.fromString(cle);
                EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(joueurs.getString(cle + ".mob", "")));
                if (type != null) {
                    mobs.put(joueur, type);
                }
                String valeur = joueurs.getString(cle + ".skin");
                if (valeur != null) {
                    skins.put(joueur, new ProfileProperty("textures", valeur, joueurs.getString(cle + ".signature")));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("tetes_de_joueurs.yml : joueur ignoré.");
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        mobs.forEach((joueur, type) -> yaml.set("joueurs." + joueur + ".mob", type.getKey().getKey()));
        skins.forEach((joueur, skin) -> {
            yaml.set("joueurs." + joueur + ".skin", skin.getValue());
            yaml.set("joueurs." + joueur + ".signature", skin.getSignature());
        });
        try {
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("tetes_de_joueurs.yml non enregistré : " + e.getMessage());
        }
    }
}
