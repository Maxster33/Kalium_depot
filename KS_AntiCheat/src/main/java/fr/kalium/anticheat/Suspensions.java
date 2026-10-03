package fr.kalium.anticheat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suspensions (cahier, catégorie 6) : un joueur suspendu (automatiquement pour une infraction grave, ou par le staff)
 * ne peut plus se connecter à Event ; message « Une erreur inhabituelle est survenue, contacte le staff. » (il ne sait
 * pas qu'il est suspecté : il n'efface pas de preuves et vient voir le staff). Le staff lève la suspension (joueur
 * clean) ou le bannit de tout KaLium (étape 5). plugins/KS_AntiCheat/suspensions.yml.
 */
final class Suspensions implements Listener {

    static final class Suspension {
        String nom;
        long date;
        String raison;
        String par;
    }

    private final KSAntiCheat plugin;
    private final File fichier;
    /** Lu pendant la connexion (hors du fil principal). */
    private final Map<UUID, Suspension> liste = new ConcurrentHashMap<>();

    Suspensions(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "suspensions.yml");
        charger();
    }

    private Component message() {
        return plugin.lang().c("suspension.message", "<red>Une erreur inhabituelle est survenue, contacte le staff.");
    }

    boolean suspendu(UUID joueur) {
        return liste.containsKey(joueur);
    }

    Suspension suspension(UUID joueur) {
        return liste.get(joueur);
    }

    Map<UUID, Suspension> toutes() {
        return new LinkedHashMap<>(liste);
    }

    /** Suspend (et expulse s'il est connecté). par : pseudo du staff, ou « automatique ». */
    void suspendre(UUID joueur, String nom, String raison, String par) {
        Suspension s = new Suspension();
        s.nom = nom;
        s.date = System.currentTimeMillis();
        s.raison = raison;
        s.par = par;
        liste.put(joueur, s);
        sauver();
        plugin.getLogger().warning("Suspension de " + nom + " (" + par + ") : " + raison);
        Player p = Bukkit.getPlayer(joueur);
        if (p != null) {
            p.kick(message());
        }
        Component annonce = plugin.lang().c("suspension.annonce", "<dark_red>[Anti-triche] <red><nom> est suspendu "
                + "<gray>(<par>) : <raison>", "nom", nom, "par", par, "raison", raison);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (KSAntiCheat.staff(staff)) {
                staff.sendMessage(annonce);
            }
        }
        plugin.lang().saveIfNeeded();
    }

    void lever(UUID joueur, String par) {
        Suspension s = liste.remove(joueur);
        if (s != null) {
            sauver();
            plugin.getLogger().info("Suspension de " + s.nom + " levée par " + par + ".");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (liste.containsKey(event.getUniqueId())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message());
        }
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection ss = yaml.getConfigurationSection("suspensions");
        if (ss == null) {
            return;
        }
        for (String cle : ss.getKeys(false)) {
            try {
                Suspension s = new Suspension();
                s.nom = ss.getString(cle + ".nom", "?");
                s.date = ss.getLong(cle + ".date");
                s.raison = ss.getString(cle + ".raison", "");
                s.par = ss.getString(cle + ".par", "");
                liste.put(UUID.fromString(cle), s);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Suspension ignorée : " + cle);
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        liste.forEach((uuid, s) -> {
            String b = "suspensions." + uuid;
            yaml.set(b + ".nom", s.nom);
            yaml.set(b + ".date", s.date);
            yaml.set(b + ".raison", s.raison);
            yaml.set(b + ".par", s.par);
        });
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer suspensions.yml : " + e.getMessage());
        }
    }
}
