package fr.kalium.anticheat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 1.6.0 - anti-vol maison (cahier « Atouts, alcools, aliments », section 3.6 ; LeKiwi06, 20/11/2026 : « on fait
 * l'anti-vol en mode signaler seulement »). Préalable au Shéjiǔ, qui demandera allow-flight=true sur Kixster.
 *
 * Même règle que le jeu (ServerGamePacketListenerImpl, lu dans Paper 26.3) : un joueur « flotte » quand il ne descend
 * pas (moins de 0,03125 bloc par tick) et qu'aucun bloc (autre que l'air) ne touche sa boîte élargie de 0,0625 et
 * prolongée de 0,55 vers le bas (noBlocksAround). L'eau, la lave, les échelles, les lianes, la poudreuse, les
 * échafaudages, le miel, le slime... sont donc des appuis, comme pour le jeu. N'ont jamais d'alerte : créatif et
 * spectateur, vol autorisé (getAllowFlight : jetons et badges de KS_Fly), élytre, lévitation, chute lente, trident,
 * véhicule et fusée Hyperboost (Detections.enPoussee), sommeil, mort, monde de test ; debout sur une entité
 * (bateau, wagonnet, shulker, happy ghast...), qui sert d'appui comme un bloc.
 *
 * Mode « signaler seulement » : rien n'est fait au joueur. Alerte légère après vol.ticks ticks en l'air (60 = 3 s :
 * avant l'expulsion du jeu à 4 s tant que allow-flight=false), alerte grave après vol.ticks-grave (possible seulement
 * avec allow-flight=true). Une alerte de chaque par épisode ; tout appui, toute descente ou téléportation le termine.
 * Compté chaque tick sur la position connue du serveur : un joueur qui lague en l'air peut être signalé (le ping est
 * dans le détail pour en juger).
 */
final class Vol implements Listener {

    private static final double DESCENTE = -0.03125;

    private final KSAntiCheat plugin;
    private final Map<UUID, Etat> etats = new HashMap<>();

    private static final class Etat {
        UUID monde;
        double y;
        int ticks;
        boolean alerte;
        boolean grave;
    }

    Vol(KSAntiCheat plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        if (!plugin.getConfig().getBoolean("vol.actif", true)) {
            plugin.getLogger().info("Anti-vol désactivé (vol.actif: false).");
            return;
        }
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
        plugin.getLogger().info("Anti-vol : signaler seulement, alerte après " + seuil() + " ticks en l'air.");
    }

    private int seuil() {
        return Math.max(20, plugin.getConfig().getInt("vol.ticks", 60));
    }

    private int seuilGrave() {
        return Math.max(seuil() + 20, plugin.getConfig().getInt("vol.ticks-grave", 200));
    }

    private void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location l = p.getLocation();
            Etat e = etats.computeIfAbsent(p.getUniqueId(), k -> new Etat());
            boolean memeMonde = l.getWorld().getUID().equals(e.monde);
            double dy = memeMonde ? l.getY() - e.y : 0;
            e.monde = l.getWorld().getUID();
            e.y = l.getY();
            if (!memeMonde || exempte(p) || dy < DESCENTE || appui(p)) {
                remettre(e);
                continue;
            }
            e.ticks++;
            if (!e.alerte && e.ticks >= seuil()) {
                e.alerte = true;
                alerter(p, e, Alertes.Gravite.LEGERE);
            } else if (e.alerte && !e.grave && e.ticks >= seuilGrave()) {
                e.grave = true;
                alerter(p, e, Alertes.Gravite.GRAVE);
            }
        }
    }

    private static void remettre(Etat e) {
        e.ticks = 0;
        e.alerte = false;
        e.grave = false;
    }

    private void alerter(Player p, Etat e, Alertes.Gravite gravite) {
        Location l = p.getLocation();
        String detail = String.format(java.util.Locale.ROOT, "en l'air sans appui depuis %.1f s (%s %d %d %d), ping %d ms",
                e.ticks / 20.0, l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), p.getPing());
        plugin.alertes().ajouter(p.getUniqueId(), p.getName(), "Vol ?", gravite, detail);
    }

    /** Ceux que le jeu ou nos plugins laissent voler ou flotter. */
    private boolean exempte(Player p) {
        GameMode mode = p.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR || p.getAllowFlight() || p.isFlying()
                || p.isGliding() || p.isRiptiding() || p.isInsideVehicle() || p.isSleeping() || p.isDead()
                || p.hasPotionEffect(PotionEffectType.LEVITATION) || p.hasPotionEffect(PotionEffectType.SLOW_FALLING)
                || Detections.enPoussee(p.getUniqueId()) || plugin.enTest(p);
    }

    /** Un bloc (autre que l'air) dans la boîte du joueur élargie de 0,0625 et prolongée de 0,55 vers le bas. */
    private static boolean appui(Player p) {
        BoundingBox b = p.getBoundingBox();
        double m = 0.0625;
        int x0 = floor(b.getMinX() - m), x1 = floor(b.getMaxX() + m);
        int y0 = floor(b.getMinY() - m - 0.55), y1 = floor(b.getMaxY() + m);
        int z0 = floor(b.getMinZ() - m), z1 = floor(b.getMaxZ() + m);
        World w = p.getWorld();
        // Debout sur une entité (bateau, wagonnet, shulker, happy ghast...) : un appui pour le jeu, pas un bloc.
        BoundingBox zone = new BoundingBox(b.getMinX() - m, b.getMinY() - m - 0.55, b.getMinZ() - m,
                b.getMaxX() + m, b.getMaxY() + m, b.getMaxZ() + m);
        for (Entity autre : w.getNearbyEntities(zone)) {
            if (autre != p && !(autre instanceof Item) && !(autre instanceof Projectile)
                    && !(autre instanceof ExperienceOrb)) {
                return true;
            }
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!w.isChunkLoaded(x >> 4, z >> 4)) {
                    return true; // comme le jeu : rien n'est compté hors des chunks chargés
                }
                for (int y = y0; y <= y1; y++) {
                    if (!w.getBlockAt(x, y, z).getType().isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Etat e = etats.get(event.getPlayer().getUniqueId());
        if (e != null) {
            remettre(e);
            e.monde = null;
        }
    }

    @EventHandler
    public void onMonde(PlayerChangedWorldEvent event) {
        etats.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        etats.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        etats.remove(event.getPlayer().getUniqueId());
    }
}
