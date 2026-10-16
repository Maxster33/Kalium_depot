package fr.kalium.trains;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un train chargé en entier : ses wagons dans l'ordre (0 = avant, côté locomotive) et le trajet de la tête, sur lequel
 * chaque autre wagon est placé à chaque tick.
 */
final class Train {

    final UUID id;
    final List<Minecart> wagons;

    /** Trajet de la tête : premier point = sa position au dernier tick, puis les plus anciennes. */
    final LinkedList<Vector> chemin = new LinkedList<>();

    boolean marche;
    /** Vitesse voulue de la tête (bloc par tick). */
    double vitesse;
    /** Position de la tête à l'arrêt (elle n'en bouge pas). */
    Location arret;
    /** Dernière position de la tête sur un rail (si elle déraille, elle y revient). */
    Location surRail;
    /** Tick depuis lequel le train est arrêté (chunks gardés chargés encore une minute). */
    long arreteDepuis;
    /** Ticks passés sans avancer alors qu'il devrait rouler (bloqué contre un obstacle). */
    int bloque;
    final Set<Long> chunks = new HashSet<>();

    Train(UUID id, List<Minecart> wagons) {
        this.id = id;
        this.wagons = new ArrayList<>(wagons);
        for (Minecart wagon : wagons) {
            chemin.add(wagon.getLocation().toVector());
        }
    }

    World monde() {
        return wagons.get(0).getWorld();
    }

    Minecart tete() {
        return wagons.get(0);
    }

    boolean complet() {
        for (Minecart wagon : wagons) {
            if (!wagon.isValid()) {
                return false;
            }
        }
        return true;
    }

    int rang(Minecart wagon) {
        for (int i = 0; i < wagons.size(); i++) {
            if (wagons.get(i).getUniqueId().equals(wagon.getUniqueId())) {
                return i;
            }
        }
        return -1;
    }

    /** Direction de la marche : celle de la tête si elle roule, sinon du 2e wagon vers la tête (trajet). */
    Vector direction() {
        Vector vitesseTete = tete().getVelocity();
        if (vitesseTete.lengthSquared() > 0.0025) {
            return vitesseTete.clone().normalize();
        }
        Vector devant = chemin.getFirst();
        for (Vector point : chemin) {
            Vector ecart = devant.clone().subtract(point);
            if (ecart.lengthSquared() > 0.09) {
                return ecart.normalize();
            }
        }
        return new Vector(0, 0, 0);
    }

    /**
     * Point du trajet à {@code distance} blocs derrière {@code devant} (position prévue de la tête), ou null si le
     * trajet est trop court.
     */
    Vector point(Vector devant, double distance) {
        Vector precedent = devant;
        double reste = distance;
        for (Vector point : chemin) {
            double longueur = point.distance(precedent);
            if (longueur >= reste && longueur > 1e-6) {
                return precedent.clone().add(point.clone().subtract(precedent).multiply(reste / longueur));
            }
            reste -= longueur;
            precedent = point;
        }
        return null;
    }

    /** Ajoute la position de la tête au trajet et oublie ce qui est au-delà du dernier wagon. */
    void avancer(Vector tete, double longueurUtile) {
        if (chemin.isEmpty() || chemin.getFirst().distanceSquared(tete) > 1e-4) {
            chemin.addFirst(tete.clone());
        }
        double total = 0;
        Vector precedent = null;
        Iterator<Vector> iterateur = chemin.iterator();
        while (iterateur.hasNext()) {
            Vector point = iterateur.next();
            if (precedent != null) {
                total += point.distance(precedent);
                if (total > longueurUtile) {
                    // Le premier point au-delà est gardé, les suivants oubliés.
                    while (iterateur.hasNext()) {
                        iterateur.next();
                        iterateur.remove();
                    }
                    break;
                }
            }
            precedent = point;
        }
    }
}
