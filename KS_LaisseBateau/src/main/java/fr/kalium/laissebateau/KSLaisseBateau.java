package fr.kalium.laissebateau;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_LaisseBateau (demande de LeKiwi06, 14/10/2026 : « tu peux réduire la longueur de la laisse si deux bateaux sont
 * accrochés l'un à l'autre ? »).
 *
 * Lu dans le code du jeu 26.3 (Leashable) : une laisse ne tire qu'au-delà de 6 blocs (élastique) et casse à 12 ; rien
 * de propre aux bateaux, et ces valeurs ne se règlent pas. Ce plugin tire donc lui-même : tout bateau ou radeau tenu
 * en laisse par un autre bateau, et que personne ne conduit (il est alors calculé par le serveur), est ramené à
 * `longueur` blocs (3 par défaut, de centre à centre) de celui qui le tracte dès qu'il s'en éloigne davantage. Il
 * prend la vitesse du bateau qui tracte, plus un rappel proportionnel à l'écart. En deçà, le jeu fait comme d'habitude.
 * La corde s'affiche entre les deux bateaux : elle paraît donc plus courte. Laisse tenue par un joueur ou une barrière :
 * inchangée.
 *
 * 1.0.1 (LeKiwi06 : « le bateau de derrière ne change jamais d'orientation ; d'habitude ça ressemble davantage à une
 * remorque qui nous suit et pas à un boulet qu'on traîne ») : le bateau remorqué est tourné à chaque tick vers celui
 * qui le tracte, comme une remorque vers son attelage (le jeu ne tourne un bateau que s'il est conduit).
 *
 * 1.0.2 (LeKiwi06 : « le bateau tracté ne suit pas correctement le bateau tracteur », sans pouvoir décrire le défaut) :
 * le bateau remorqué prenait la vitesse du tracteur, plus un rappel élastique au-delà de la longueur : dans un virage il
 * glissait de côté, parallèlement au tracteur, au lieu de suivre sa trace ; au ralentissement il continuait sur son
 * élan (« boulet »). Maintenant, une remorque à timon rigide : il est placé à chaque tick sur la droite qui le relie à
 * l'arrière du tracteur (l'attelage), à `longueur` blocs de centre à centre quand ils sont alignés ; il coupe donc les
 * virages par l'intérieur comme une remorque, et recule si le tracteur recule. `raideur` ne sert plus.
 */
public final class KSLaisseBateau extends JavaPlugin {

    /** Vitesse horizontale maximale donnée au bateau remorqué (blocs par tick), contre les sauts de position. */
    private static final double VITESSE_MAX = 1.5;
    /** 1.0.2 : distance du centre du tracteur à son attelage (son arrière ; un bateau mesure 1,375 bloc). */
    private static final double ATTELAGE = 0.7;
    /** 1.0.2 : frein du jeu sur un bateau dans l'eau, appliqué avant son déplacement (AbstractBoat.floatBoat). */
    private static final double FREIN_EAU = 0.9;

    /** Bateaux tenus en laisse par un bateau, relevés chaque seconde. */
    private final List<Boat> remorques = new ArrayList<>();

    /** Position au tick d'avant de chaque bateau qui tracte (sa vitesse : il est souvent conduit par un joueur). */
    private Map<UUID, Location> precedentes = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getScheduler().runTaskTimer(this, this::chercher, 1L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::tirer, 1L, 1L);
    }

    private void chercher() {
        remorques.clear();
        for (World monde : Bukkit.getWorlds()) {
            for (Boat bateau : monde.getEntitiesByClass(Boat.class)) {
                if (bateau.isLeashed() && bateau.getLeashHolder() instanceof Boat) {
                    remorques.add(bateau);
                }
            }
        }
    }

    private void tirer() {
        double longueur = getConfig().getDouble("longueur", 3.0);
        Map<UUID, Location> positions = new HashMap<>();
        for (Boat bateau : remorques) {
            if (!bateau.isValid() || !bateau.isLeashed() || !(bateau.getLeashHolder() instanceof Boat tracteur)
                    || !tracteur.isValid() || !tracteur.getWorld().equals(bateau.getWorld())) {
                continue;
            }
            Location ici = tracteur.getLocation();
            Location avant = precedentes.get(tracteur.getUniqueId());
            positions.put(tracteur.getUniqueId(), ici);
            // Un bateau conduit est calculé par le jeu de son conducteur : le serveur ne le tire pas.
            List<Entity> passagers = bateau.getPassengers();
            if (!passagers.isEmpty() && passagers.get(0) instanceof Player) {
                continue;
            }
            Location la = bateau.getLocation();
            // Attelage : l'arrière du tracteur (un bateau avance vers (-sin cap, cos cap)).
            double cap = Math.toRadians(ici.getYaw());
            double hx = ici.getX() + Math.sin(cap) * ATTELAGE;
            double hz = ici.getZ() - Math.cos(cap) * ATTELAGE;
            if (avant != null && avant.getWorld().equals(ici.getWorld())) {
                // Où sera l'attelage à la fin de ce tick : le tracteur continue son mouvement du tick d'avant.
                double mx = ici.getX() - avant.getX();
                double mz = ici.getZ() - avant.getZ();
                double m = Math.sqrt(mx * mx + mz * mz);
                if (m <= VITESSE_MAX) {
                    hx += mx;
                    hz += mz;
                }
            }
            double dx = hx - la.getX();
            double dz = hz - la.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 0.3) {
                continue;
            }
            orienter(bateau, la, dx, dz);
            // Place voulue : sur la droite attelage-bateau, à la longueur du timon de l'attelage.
            double timon = Math.max(0.5, longueur - ATTELAGE);
            double ecart = distance - timon;
            double vx = dx / distance * ecart;
            double vz = dz / distance * ecart;
            // Le jeu freine le bateau (x0,9 sur l'eau) avant de le déplacer : compensé.
            vx /= FREIN_EAU;
            vz /= FREIN_EAU;
            double vitesse = Math.sqrt(vx * vx + vz * vz);
            if (vitesse > VITESSE_MAX) {
                vx *= VITESSE_MAX / vitesse;
                vz *= VITESSE_MAX / vitesse;
            }
            bateau.setVelocity(new Vector(vx, bateau.getVelocity().getY(), vz));
        }
        precedentes = positions;
    }

    /**
     * 1.0.1 : avant du bateau remorqué vers le bateau qui le tracte (un bateau avance vers (-sin cap, cos cap)) ; 1.0.2 :
     * vers son attelage. Pas de changement pour moins d'un demi-degré (pas de renvoi inutile).
     */
    private static void orienter(Boat bateau, Location la, double dx, double dz) {
        float cap = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float ecart = Math.abs(((cap - la.getYaw()) % 360 + 540) % 360 - 180);
        if (ecart > 0.5f) {
            bateau.setRotation(cap, la.getPitch());
        }
    }
}
