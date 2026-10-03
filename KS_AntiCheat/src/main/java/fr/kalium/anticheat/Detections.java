package fr.kalium.anticheat;

import fr.kalium.anticheat.Alertes.Gravite;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Détections (cahier, catégorie 6, étape 2) : GrimAC, macros, AFK prolongé. (X-ray : Minage.)
 *
 * - GrimAC : ses signalements (FlagEvent, par son propre bus d'événements, branché par réflexion : aucune
 *   dépendance à sa version) deviennent une alerte légère quand une même vérification signale un joueur
 *   grimac.alerte-signalements fois en 10 minutes ; infraction grave (suspension automatique) si la vérification
 *   est dans grimac.lourdes et que son niveau de violation atteint grimac.seuil-suspension.
 * - Macros : alerte légère si le joueur répète des actions (casser, pêcher, frapper, utiliser un objet) alors qu'il
 *   ne bouge ni lui ni sa caméra depuis macros.minutes ; alerte seulement.
 * - AFK : expulsion au bout de afk.minutes (60) sans vrai mouvement ni mouvement de caméra (poussé par l'eau, dans un
 *   véhicule : ne compte pas). Permission ksanticheat.afk-libre : jamais expulsé.
 */
final class Detections implements Listener {

    private final KSAntiCheat plugin;
    /** Dernier vrai mouvement / dernier mouvement de caméra. */
    private final Map<UUID, Long> dernierMouvement = new HashMap<>();
    private final Map<UUID, Long> derniereCamera = new HashMap<>();
    /** Actions faites sans bouger (macros), dates. */
    private final Map<UUID, Deque<Long>> actionsImmobiles = new HashMap<>();
    /** Signalements GrimAC récents : joueur|vérification -> dates (lu depuis le fil de GrimAC). */
    private final Map<String, Deque<Long>> signalements = new ConcurrentHashMap<>();

    Detections(KSAntiCheat plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::verifierAfk, 20L * 60, 20L * 60);
        for (Player p : Bukkit.getOnlinePlayers()) {
            actif(p.getUniqueId(), true, true);
        }
    }

    private void actif(UUID joueur, boolean mouvement, boolean camera) {
        long maintenant = System.currentTimeMillis();
        if (mouvement) {
            dernierMouvement.put(joueur, maintenant);
        }
        if (camera) {
            derniereCamera.put(joueur, maintenant);
        }
    }

    // ------------------------------------------------------------------ mouvements

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        actif(event.getPlayer().getUniqueId(), true, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID u = event.getPlayer().getUniqueId();
        dernierMouvement.remove(u);
        derniereCamera.remove(u);
        actionsImmobiles.remove(u);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location de = event.getFrom();
        Location vers = event.getTo();
        Player p = event.getPlayer();
        boolean camera = de.getYaw() != vers.getYaw() || de.getPitch() != vers.getPitch();
        // Poussé par l'eau ou transporté : pas un vrai mouvement.
        boolean mouvement = !p.isInsideVehicle() && !p.isInWater() && de.distanceSquared(vers) > 0.0025;
        if (camera || mouvement) {
            actif(p.getUniqueId(), mouvement, camera);
        }
    }

    // ------------------------------------------------------------------ AFK

    private void verifierAfk() {
        long limite = Math.max(1, plugin.getConfig().getLong("afk.minutes", 60)) * 60_000L;
        long maintenant = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("ksanticheat.afk-libre")) {
                continue;
            }
            long dernier = Math.max(dernierMouvement.getOrDefault(p.getUniqueId(), maintenant),
                    derniereCamera.getOrDefault(p.getUniqueId(), maintenant));
            if (maintenant - dernier >= limite) {
                plugin.getLogger().info(p.getName() + " expulsé : inactif depuis " + (limite / 60_000) + " minutes.");
                p.kick(plugin.lang().c("afk.message", "<yellow>Expulsé après <minutes> minutes d'inactivité.",
                        "minutes", limite / 60_000));
                plugin.lang().saveIfNeeded();
            }
        }
    }

    // ------------------------------------------------------------------ macros

    private void action(Player p, String quoi) {
        long seuil = Math.max(1, plugin.getConfig().getLong("macros.minutes", 5)) * 60_000L;
        long maintenant = System.currentTimeMillis();
        long mouvement = dernierMouvement.getOrDefault(p.getUniqueId(), maintenant);
        long camera = derniereCamera.getOrDefault(p.getUniqueId(), maintenant);
        if (maintenant - mouvement < seuil || maintenant - camera < seuil) {
            actionsImmobiles.remove(p.getUniqueId());
            return;
        }
        Deque<Long> dates = actionsImmobiles.computeIfAbsent(p.getUniqueId(), u -> new ArrayDeque<>());
        dates.addLast(maintenant);
        while (!dates.isEmpty() && maintenant - dates.peekFirst() > 10 * 60_000L) {
            dates.removeFirst();
        }
        int n = Math.max(1, plugin.getConfig().getInt("macros.actions", 20));
        if (dates.size() >= n) {
            dates.clear();
            plugin.alertes().ajouter(p.getUniqueId(), p.getName(), "Macro ?", Gravite.LEGERE,
                    n + " actions (" + quoi + ") sans bouger ni tourner la caméra depuis " + (seuil / 60_000) + " min");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        action(event.getPlayer(), "casser");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            action(event.getPlayer(), "pêcher");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Player p = event.getDamager() instanceof Player j ? j
                : event.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player j ? j : null;
        if (p != null) {
            action(p, "frapper");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUse(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && event.getItem() != null && event.useItemInHand() != org.bukkit.event.Event.Result.DENY) {
            action(event.getPlayer(), "utiliser un objet");
        }
    }

    // ------------------------------------------------------------------ GrimAC

    /** Branche les signalements de GrimAC (s'il est là). */
    void brancherGrim() {
        Plugin grim = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grim == null || !grim.isEnabled()) {
            plugin.getLogger().info("GrimAC absent : ses alertes ne sont pas reprises.");
            return;
        }
        try {
            ClassLoader cl = grim.getClass().getClassLoader();
            Class<?> fournisseur = Class.forName("ac.grim.grimac.api.GrimAPIProvider", true, cl);
            Class<?> apiItf = Class.forName("ac.grim.grimac.api.GrimAbstractAPI", true, cl);
            Class<?> busItf = Class.forName("ac.grim.grimac.api.event.EventBus", true, cl);
            Class<?> ecouteur = Class.forName("ac.grim.grimac.api.event.GrimEventListener", true, cl);
            Class<?> flag = Class.forName("ac.grim.grimac.api.event.events.FlagEvent", true, cl);
            Class<?> checkEvent = Class.forName("ac.grim.grimac.api.event.events.GrimCheckEvent", true, cl);
            Class<?> verbose = Class.forName("ac.grim.grimac.api.event.events.GrimVerboseCheckEvent", true, cl);
            Class<?> check = Class.forName("ac.grim.grimac.api.AbstractCheck", true, cl);
            Class<?> user = Class.forName("ac.grim.grimac.api.GrimUser", true, cl);
            Method getUser = checkEvent.getMethod("getUser");
            Method getCheck = checkEvent.getMethod("getCheck");
            Method getViolations = checkEvent.getMethod("getViolations");
            Method getVerbose = verbose.getMethod("getVerbose");
            Method getCheckName = check.getMethod("getCheckName");
            Method getName = user.getMethod("getName");
            Method getUuid = user.getMethod("getUniqueId");
            Object api = fournisseur.getMethod("get").invoke(null);
            Object bus = apiItf.getMethod("getEventBus").invoke(api);
            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{ecouteur}, (p, m, args) -> {
                switch (m.getName()) {
                    case "handle" -> {
                        Object ev = args[0];
                        Object u = getUser.invoke(ev);
                        String nomCheck = String.valueOf(getCheckName.invoke(getCheck.invoke(ev)));
                        double vl = ((Number) getViolations.invoke(ev)).doubleValue();
                        String detail = String.valueOf(getVerbose.invoke(ev));
                        signalement((UUID) getUuid.invoke(u), String.valueOf(getName.invoke(u)), nomCheck, vl, detail);
                        return null;
                    }
                    case "hashCode" -> {
                        return System.identityHashCode(p);
                    }
                    case "equals" -> {
                        return p == args[0];
                    }
                    case "toString" -> {
                        return "KS_AntiCheat-FlagEvent";
                    }
                    default -> {
                        return null;
                    }
                }
            });
            busItf.getMethod("subscribe", Object.class, Class.class, ecouteur).invoke(bus, plugin, flag, proxy);
            plugin.getLogger().info("Signalements de GrimAC repris.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            plugin.getLogger().warning("Impossible de reprendre les signalements de GrimAC : " + e);
        }
    }

    /** Signalement GrimAC (fil de GrimAC) : compté ; alerte / suspension sur le fil principal. */
    private void signalement(UUID joueur, String nom, String verification, double vl, String detail) {
        String cle = joueur + "|" + verification;
        long maintenant = System.currentTimeMillis();
        Deque<Long> dates = signalements.computeIfAbsent(cle, k -> new ArrayDeque<>());
        int n;
        synchronized (dates) {
            dates.addLast(maintenant);
            while (!dates.isEmpty() && maintenant - dates.peekFirst() > 10 * 60_000L) {
                dates.removeFirst();
            }
            n = dates.size();
        }
        int seuilAlerte = Math.max(1, plugin.getConfig().getInt("grimac.alerte-signalements", 10));
        double seuilGrave = plugin.getConfig().getDouble("grimac.seuil-suspension", 100);
        List<String> lourdes = plugin.getConfig().getStringList("grimac.lourdes");
        if (lourdes.isEmpty()) {
            lourdes = List.of("Reach", "Hitboxes", "FastBreak", "FarBreak", "MultiBreak", "Timer");
        }
        List<String> verifs = lourdes;
        boolean lourde = verifs.stream().anyMatch(l -> verification.toLowerCase().startsWith(l.toLowerCase()));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (lourde && vl >= seuilGrave && !plugin.suspensions().suspendu(joueur)) {
                plugin.infractionGrave(joueur, nom, "GrimAC " + verification,
                        "niveau " + Math.round(vl) + " (seuil " + Math.round(seuilGrave) + ")" + court(detail));
            } else if (n == seuilAlerte) {
                plugin.alertes().ajouter(joueur, nom, "GrimAC " + verification, Gravite.LEGERE,
                        n + " signalements en 10 min, niveau " + Math.round(vl) + court(detail));
            }
        });
    }

    private static String court(String detail) {
        if (detail == null || detail.isBlank() || "null".equals(detail)) {
            return "";
        }
        return " (" + (detail.length() > 60 ? detail.substring(0, 60) + "…" : detail) + ")";
    }
}
