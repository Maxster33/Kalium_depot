package fr.kalium.anticheat;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Morts d'entités importantes (cahier, catégorie 6 : en plus des journaux de CoreProtect) : villageois, animaux
 * apprivoisés, mobs nommés, golems, boss (liste réglable : morts.types, morts.nommes, morts.apprivoises).
 * plugins/KS_AntiCheat/morts.log (date, entité, nom, propriétaire, lieu, tué par) ; 200 dernières dans l'interface.
 */
final class Morts implements Listener {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Europe/Paris"));
    static final List<String> TYPES_PAR_DEFAUT = List.of("VILLAGER", "IRON_GOLEM", "SNOW_GOLEM", "COPPER_GOLEM",
            "ALLAY", "WITHER", "ENDER_DRAGON", "ELDER_GUARDIAN", "WARDEN");

    private final KSAntiCheat plugin;
    private final Deque<String> dernieres = new ArrayDeque<>();

    Morts(KSAntiCheat plugin) {
        this.plugin = plugin;
    }

    private boolean importante(LivingEntity e) {
        List<String> types = plugin.getConfig().getStringList("morts.types");
        if (types.isEmpty()) {
            types = TYPES_PAR_DEFAUT;
        }
        if (types.contains(e.getType().name())) {
            return true;
        }
        if (plugin.getConfig().getBoolean("morts.nommes", true) && e.customName() != null && !(e instanceof Player)) {
            return true;
        }
        return plugin.getConfig().getBoolean("morts.apprivoises", true) && e instanceof Tameable t && t.isTamed();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e instanceof Player || !importante(e)) {
            return;
        }
        String nom = e.customName() == null ? "" : " « " + PlainTextComponentSerializer.plainText().serialize(e.customName()) + " »";
        String proprio = "";
        if (e instanceof Tameable t && t.isTamed()) {
            AnimalTamer tamer = t.getOwner();
            proprio = " (à " + (tamer == null || tamer.getName() == null ? "?" : tamer.getName()) + ")";
        }
        Location l = e.getLocation();
        String lieu = l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
        String ligne = DATE.format(Instant.now()) + " | " + e.getType().name().toLowerCase(Locale.ROOT) + nom + proprio
                + " | " + lieu + " | " + cause(e);
        dernieres.addFirst(ligne);
        while (dernieres.size() > 200) {
            dernieres.removeLast();
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(new File(plugin.getDataFolder(), "morts.log"),
                StandardCharsets.UTF_8, true))) {
            out.println(ligne);
        } catch (IOException ex) {
            plugin.getLogger().warning("Journal des morts : " + ex.getMessage());
        }
    }

    private static String cause(LivingEntity e) {
        Player tueur = e.getKiller();
        if (tueur != null) {
            return "tué par " + tueur.getName();
        }
        EntityDamageEvent dernier = e.getLastDamageCause();
        if (dernier instanceof EntityDamageByEntityEvent parEntite) {
            Entity source = parEntite.getDamager();
            if (source instanceof Projectile p && p.getShooter() instanceof Entity tireur) {
                source = tireur;
            }
            return "tué par " + (source instanceof Player p ? p.getName() : source.getType().name().toLowerCase(Locale.ROOT));
        }
        return dernier == null ? "cause inconnue" : dernier.getCause().name().toLowerCase(Locale.ROOT);
    }

    List<String> dernieres(int nombre) {
        List<String> r = new ArrayList<>();
        for (String l : dernieres) {
            if (r.size() >= nombre) {
                break;
            }
            r.add(l);
        }
        return r;
    }
}
