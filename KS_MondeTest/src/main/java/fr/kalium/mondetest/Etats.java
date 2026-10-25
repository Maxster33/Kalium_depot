package fr.kalium.mondetest;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Etat d'un joueur de chaque cote (survie « kixster » ou monde de test « test ») : inventaire, coffre de l'Ender et son
 * extension (KS_EC_Extension), experience, vie, faim, effets, mode de jeu, vol, statut d'operateur (cote survie) et
 * position. Un fichier par joueur et par cote, etats/<cote>/<uuid>.yml, qui garde l'etat du cote ou le joueur N'EST PAS
 * (l'etat du cote ou il est se trouve dans ses donnees de joueur, sauvees par le serveur). Repris de KLM_Espaces.
 */
final class Etats {

    /**
     * KS_EC_Extension garde les 3 lignes du bas du coffre de l'Ender et ses cases debloquees dans les donnees du joueur,
     * pas dans le coffre vanilla : sans elles, un objet du monde de test passerait en survie par le coffre de l'Ender.
     */
    private static final NamespacedKey EC_EXTENSION = new NamespacedKey("ks_ec_extension", "extension");
    private static final NamespacedKey EC_CASES = new NamespacedKey("ks_ec_extension", "cases_debloquees");

    private final KsMondeTest plugin;

    Etats(KsMondeTest plugin) {
        this.plugin = plugin;
    }

    private File fichier(String cote, UUID joueur) {
        return new File(new File(new File(plugin.getDataFolder(), "etats"), cote), joueur + ".yml");
    }

    private YamlConfiguration lire(String cote, UUID joueur) {
        File f = fichier(cote, joueur);
        return f.isFile() ? YamlConfiguration.loadConfiguration(f) : null;
    }

    /** Position sauvee de ce cote (null si aucune, ou monde non charge). Lisible hors du fil principal. */
    Location position(String cote, UUID joueur) {
        YamlConfiguration yaml = lire(cote, joueur);
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

    /** Statut d'operateur sauve du cote survie (faux si aucun etat). */
    boolean op(String cote, UUID joueur) {
        YamlConfiguration yaml = lire(cote, joueur);
        return yaml != null && yaml.getBoolean("op", false);
    }

    /**
     * Sauve l'etat actuel du joueur pour ce cote. Position : « depart » si elle est de ce cote, sinon sa position
     * actuelle si elle est de ce cote, sinon celle deja sauvee. Faux si le fichier n'a pas pu etre ecrit.
     */
    boolean sauver(Player joueur, String cote, Location depart) {
        Location ancienne = position(cote, joueur.getUniqueId());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pseudo", joueur.getName());
        yaml.set("inventaire", ecrire(joueur.getInventory().getContents()));
        yaml.set("ender", ecrire(joueur.getEnderChest().getContents()));
        PersistentDataContainer donnees = joueur.getPersistentDataContainer();
        byte[] extension = donnees.get(EC_EXTENSION, PersistentDataType.BYTE_ARRAY);
        if (extension != null) {
            yaml.set("ec-extension", Base64.getEncoder().encodeToString(extension));
        }
        yaml.set("ec-cases-debloquees", donnees.getOrDefault(EC_CASES, PersistentDataType.INTEGER, 0));
        yaml.set("niveau", joueur.getLevel());
        yaml.set("xp", (double) joueur.getExp());
        yaml.set("vie", joueur.getHealth());
        yaml.set("faim", joueur.getFoodLevel());
        yaml.set("saturation", (double) joueur.getSaturation());
        yaml.set("effets", new ArrayList<>(joueur.getActivePotionEffects()));
        yaml.set("mode", joueur.getGameMode().name());
        yaml.set("vol-autorise", joueur.getAllowFlight());
        yaml.set("en-vol", joueur.isFlying());
        yaml.set("op", joueur.isOp());
        Location l = depart != null && plugin.cote(depart.getWorld()).equals(cote) ? depart
                : plugin.cote(joueur.getWorld()).equals(cote) ? joueur.getLocation() : ancienne;
        if (l != null && l.getWorld() != null) {
            yaml.set("position.monde", l.getWorld().getName());
            yaml.set("position.x", l.getX());
            yaml.set("position.y", l.getY());
            yaml.set("position.z", l.getZ());
            yaml.set("position.yaw", (double) l.getYaw());
            yaml.set("position.pitch", (double) l.getPitch());
        }
        File f = fichier(cote, joueur.getUniqueId());
        f.getParentFile().mkdirs();
        try {
            yaml.save(f);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Etat de " + joueur.getName() + " (cote " + cote + ") non sauve : " + e.getMessage());
            return false;
        }
    }

    /** Rend l'etat sauve de ce cote ; faux s'il n'y en a pas (jamais venu de ce cote). Le statut d'op n'est pas touche. */
    boolean rendre(Player joueur, String cote) {
        YamlConfiguration yaml = lire(cote, joueur.getUniqueId());
        if (yaml == null) {
            return false;
        }
        try {
            joueur.getInventory().setContents(lire(yaml.getString("inventaire", ""), joueur.getInventory().getSize()));
            joueur.getEnderChest().setContents(lire(yaml.getString("ender", ""), joueur.getEnderChest().getSize()));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Inventaire de " + joueur.getName() + " (cote " + cote + ") illisible (" + e
                    + ") : inventaire vide, fichier garde.");
            vider(joueur, GameMode.SURVIVAL);
            return true;
        }
        PersistentDataContainer donnees = joueur.getPersistentDataContainer();
        String extension = yaml.getString("ec-extension", "");
        if (extension.isBlank()) {
            donnees.remove(EC_EXTENSION);
        } else {
            donnees.set(EC_EXTENSION, PersistentDataType.BYTE_ARRAY, Base64.getDecoder().decode(extension));
        }
        donnees.set(EC_CASES, PersistentDataType.INTEGER, yaml.getInt("ec-cases-debloquees", 0));
        joueur.setLevel(yaml.getInt("niveau", 0));
        joueur.setExp((float) Math.max(0, Math.min(1, yaml.getDouble("xp", 0))));
        AttributeInstance max = joueur.getAttribute(Attribute.MAX_HEALTH);
        double vieMax = max == null ? 20 : max.getValue();
        joueur.setHealth(Math.max(1, Math.min(vieMax, yaml.getDouble("vie", vieMax))));
        joueur.setFoodLevel(yaml.getInt("faim", 20));
        joueur.setSaturation((float) yaml.getDouble("saturation", 5));
        joueur.setFireTicks(0);
        joueur.setFallDistance(0);
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

    /** Joueur jamais venu de ce cote : il repart de rien (son etat est deja sauve pour l'autre cote). */
    static void vider(Player joueur, GameMode mode) {
        joueur.getInventory().clear();
        joueur.getEnderChest().clear();
        joueur.getPersistentDataContainer().remove(EC_EXTENSION);
        joueur.getPersistentDataContainer().remove(EC_CASES);
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
        joueur.setFallDistance(0);
        joueur.setGameMode(mode);
        joueur.setAllowFlight(mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR);
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
