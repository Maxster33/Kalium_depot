package fr.kalium.espaces;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Etat d'un joueur dans chaque espace (inventaire, coffre de l'Ender, experience, vie, faim, effets, mode de jeu, vol,
 * position) : un fichier par joueur et par espace, etats/<espace>/<uuid>.yml. Lu hors du fil principal a la
 * pre-connexion, rendu a l'arrivee, ecrit au depart. Aussi : dernier espace de chaque joueur (derniers.yml).
 */
final class Etats {

    private final KlmEspaces plugin;
    /** Etat lu a la pre-connexion, en attente de l'arrivee du joueur. */
    private final Map<UUID, YamlConfiguration> charges = new ConcurrentHashMap<>();
    private final Map<UUID, String> derniers = new ConcurrentHashMap<>();
    private final File fichierDerniers;

    Etats(KlmEspaces plugin) {
        this.plugin = plugin;
        this.fichierDerniers = new File(plugin.getDataFolder(), "derniers.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichierDerniers);
        for (String cle : yaml.getKeys(false)) {
            try {
                derniers.put(UUID.fromString(cle), yaml.getString(cle, ""));
            } catch (IllegalArgumentException ignored) {
                // ligne illisible : ignoree
            }
        }
    }

    private File fichier(String espace, UUID joueur) {
        return new File(new File(plugin.dossierEtats(), espace), joueur + ".yml");
    }

    String dernier(UUID joueur) {
        return derniers.get(joueur);
    }

    void precharger(UUID joueur, String espace) {
        File f = fichier(espace, joueur);
        if (f.isFile()) {
            charges.put(joueur, YamlConfiguration.loadConfiguration(f));
        } else {
            charges.remove(joueur);
        }
    }

    void oublier(UUID joueur) {
        charges.remove(joueur);
    }

    /** Position sauvee dans l'espace en attente (null si aucune, ou monde non charge). */
    Location position(UUID joueur) {
        YamlConfiguration yaml = charges.get(joueur);
        if (yaml == null || !yaml.isString("position.monde")) {
            return null;
        }
        World monde = Bukkit.getWorld(yaml.getString("position.monde", ""));
        if (monde == null) {
            return null;
        }
        return new Location(monde, yaml.getDouble("position.x"), yaml.getDouble("position.y"), yaml.getDouble("position.z"),
                (float) yaml.getDouble("position.yaw"), (float) yaml.getDouble("position.pitch"));
    }

    /** Rend l'etat en attente ; faux s'il n'y en a pas (jamais venu dans cet espace). */
    boolean rendre(Player joueur) {
        YamlConfiguration yaml = charges.remove(joueur.getUniqueId());
        if (yaml == null) {
            return false;
        }
        try {
            joueur.getInventory().setContents(lire(yaml.getString("inventaire", ""), joueur.getInventory().getSize()));
            joueur.getEnderChest().setContents(lire(yaml.getString("ender", ""), joueur.getEnderChest().getSize()));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Inventaire de " + joueur.getName() + " illisible (" + e + ") : mis de cote, "
                    + "fichier garde.");
            vider(joueur);
            return true;
        }
        joueur.setLevel(yaml.getInt("niveau", 0));
        joueur.setExp((float) Math.max(0, Math.min(1, yaml.getDouble("xp", 0))));
        AttributeInstance max = joueur.getAttribute(Attribute.MAX_HEALTH);
        double vieMax = max == null ? 20 : max.getValue();
        joueur.setHealth(Math.max(1, Math.min(vieMax, yaml.getDouble("vie", vieMax))));
        joueur.setFoodLevel(yaml.getInt("faim", 20));
        joueur.setSaturation((float) yaml.getDouble("saturation", 5));
        for (PotionEffect effet : new ArrayList<>(joueur.getActivePotionEffects())) {
            joueur.removePotionEffect(effet.getType());
        }
        List<?> effets = yaml.getList("effets", List.of());
        for (Object o : effets) {
            if (o instanceof PotionEffect effet) {
                joueur.addPotionEffect(effet);
            }
        }
        try {
            joueur.setGameMode(GameMode.valueOf(yaml.getString("mode", "SURVIVAL")));
        } catch (IllegalArgumentException ignored) {
            // mode inconnu : inchange
        }
        joueur.setAllowFlight(yaml.getBoolean("vol-autorise", false));
        if (joueur.getAllowFlight()) {
            joueur.setFlying(yaml.getBoolean("en-vol", false));
        }
        return true;
    }

    /** Joueur jamais venu dans cet espace : on repart de rien (son etat est deja sauve dans l'autre espace). */
    static void vider(Player joueur) {
        joueur.getInventory().clear();
        joueur.getEnderChest().clear();
        joueur.setLevel(0);
        joueur.setExp(0);
        for (PotionEffect effet : new ArrayList<>(joueur.getActivePotionEffects())) {
            joueur.removePotionEffect(effet.getType());
        }
        AttributeInstance max = joueur.getAttribute(Attribute.MAX_HEALTH);
        joueur.setHealth(max == null ? 20 : max.getValue());
        joueur.setFoodLevel(20);
        joueur.setSaturation(5);
        joueur.setFireTicks(0);
    }

    void sauver(Player joueur, String espace) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pseudo", joueur.getName());
        yaml.set("inventaire", ecrire(joueur.getInventory().getContents()));
        yaml.set("ender", ecrire(joueur.getEnderChest().getContents()));
        yaml.set("niveau", joueur.getLevel());
        yaml.set("xp", (double) joueur.getExp());
        yaml.set("vie", joueur.getHealth());
        yaml.set("faim", joueur.getFoodLevel());
        yaml.set("saturation", (double) joueur.getSaturation());
        yaml.set("effets", new ArrayList<>(joueur.getActivePotionEffects()));
        yaml.set("mode", joueur.getGameMode().name());
        yaml.set("vol-autorise", joueur.getAllowFlight());
        yaml.set("en-vol", joueur.isFlying());
        Location l = joueur.getLocation();
        if (plugin.dansEspace(espace, l.getWorld())) {
            yaml.set("position.monde", l.getWorld().getName());
            yaml.set("position.x", l.getX());
            yaml.set("position.y", l.getY());
            yaml.set("position.z", l.getZ());
            yaml.set("position.yaw", (double) l.getYaw());
            yaml.set("position.pitch", (double) l.getPitch());
        }
        File f = fichier(espace, joueur.getUniqueId());
        f.getParentFile().mkdirs();
        try {
            yaml.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Etat de " + joueur.getName() + " (espace " + espace + ") non sauve : " + e.getMessage());
        }
        derniers.put(joueur.getUniqueId(), espace);
        ecrireDerniers();
    }

    void ecrireDerniers() {
        YamlConfiguration yaml = new YamlConfiguration();
        derniers.forEach((id, espace) -> yaml.set(id.toString(), espace));
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichierDerniers);
        } catch (IOException e) {
            plugin.getLogger().warning("derniers.yml non ecrit : " + e.getMessage());
        }
    }

    private static String ecrire(ItemStack[] objets) {
        ItemStack[] copie = new ItemStack[objets.length];
        for (int i = 0; i < objets.length; i++) {
            copie[i] = objets[i] == null ? ItemStack.empty() : objets[i];
        }
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(copie));
    }

    private static ItemStack[] lire(String texte, int taille) {
        ItemStack[] resultat = new ItemStack[taille];
        if (texte == null || texte.isBlank()) {
            return resultat;
        }
        ItemStack[] lus = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(texte));
        for (int i = 0; i < Math.min(taille, lus.length); i++) {
            resultat[i] = lus[i] == null || lus[i].isEmpty() ? null : lus[i];
        }
        return resultat;
    }
}
