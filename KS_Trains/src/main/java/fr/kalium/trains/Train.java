package fr.kalium.trains;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
 * Un train chargé en entier : ses wagons dans l'ordre (0 = avant, côté locomotive) et le trajet du train, sur lequel
 * chaque wagon est placé à chaque tick derrière le wagon qui mène (la tête en marche avant ; 1.1.0 : le dernier wagon
 * en marche arrière).
 */
final class Train {

    final UUID id;
    final List<Minecart> wagons;

    /** Trajet : premier point côté avant du train, dernier point côté arrière. */
    final LinkedList<Vector> chemin = new LinkedList<>();

    /**
     * 1.1.0 : niveau de vitesse voulu : 0 arrêt, 1 à 4 en avant (5 blocs/s par niveau, plafonné par la vitesse
     * maximale du train), -1 marche arrière (1 bloc/s). Changé par le wagon contrôleur ou le menu ; gardé quand le
     * conducteur descend.
     */
    int niveau;
    /** Vitesse du train (bloc par tick, toujours positive ; le sens est dans {@link #sens}). */
    double vitesse;
    /** 1.1.0 : sens de la marche (1 avant, -1 arrière), gardé à l'arrêt : il dit quel wagon mène. */
    int sens = 1;
    /** Position du wagon qui mène à l'arrêt (il n'en bouge pas). */
    Location arret;
    /** Dernière position du wagon qui mène sur un rail (s'il déraille, il y revient). */
    Location surRail;
    /** Tick depuis lequel le train est arrêté (chunks gardés chargés encore une minute). */
    long arreteDepuis;
    /** Ticks passés sans avancer alors qu'il devrait rouler (bloqué contre un obstacle). */
    int bloque;
    final Set<Long> chunks = new HashSet<>();
    /** 1.3.4 : tick du dernier souffle de vapeur de la locomotive. */
    long dernierSouffle;
    /** 1.3.5 : dernier bloc de rail du wagon qui mène (pour compter les rails propulseurs éteints franchis). */
    Block dernierRail;

    Train(UUID id, List<Minecart> wagons) {
        this.id = id;
        this.wagons = new ArrayList<>(wagons);
        reconstruire();
    }

    /** Trajet refait sur la position des wagons (formation du train, changement de sens). */
    void reconstruire() {
        chemin.clear();
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

    /** Wagon qui mène : la tête en marche avant, le dernier wagon en marche arrière. */
    Minecart meneur() {
        return sens > 0 ? wagons.get(0) : wagons.get(wagons.size() - 1);
    }

    /** Le {@code k}-ième wagon derrière celui qui mène (0 = celui qui mène). */
    Minecart suiveur(int k) {
        return sens > 0 ? wagons.get(k) : wagons.get(wagons.size() - 1 - k);
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

    private Iterator<Vector> depuisMeneur() {
        return sens > 0 ? chemin.iterator() : chemin.descendingIterator();
    }

    Vector bout() {
        return sens > 0 ? chemin.getFirst() : chemin.getLast();
    }

    /** Direction de la marche : celle du wagon qui mène s'il roule, sinon s'éloignant du wagon qui le suit (trajet). */
    Vector direction() {
        Vector vitesseMeneur = meneur().getVelocity();
        if (vitesseMeneur.lengthSquared() > 0.0025) {
            return vitesseMeneur.clone().normalize();
        }
        Vector devant = bout();
        Iterator<Vector> points = depuisMeneur();
        while (points.hasNext()) {
            Vector ecart = devant.clone().subtract(points.next());
            if (ecart.lengthSquared() > 0.09) {
                return ecart.normalize();
            }
        }
        return new Vector(0, 0, 0);
    }

    /**
     * Point du trajet à {@code distance} blocs derrière {@code devant} (position prévue du wagon qui mène), ou null si
     * le trajet est trop court.
     */
    Vector point(Vector devant, double distance) {
        Vector precedent = devant;
        double reste = distance;
        Iterator<Vector> points = depuisMeneur();
        while (points.hasNext()) {
            Vector point = points.next();
            double longueur = point.distance(precedent);
            if (longueur >= reste && longueur > 1e-6) {
                return precedent.clone().add(point.clone().subtract(precedent).multiply(reste / longueur));
            }
            reste -= longueur;
            precedent = point;
        }
        return null;
    }

    /** Ajoute la position du wagon qui mène au trajet et oublie ce qui est au-delà du dernier wagon qui le suit. */
    void avancer(Vector meneur, double longueurUtile) {
        if (chemin.isEmpty() || bout().distanceSquared(meneur) > 1e-4) {
            if (sens > 0) {
                chemin.addFirst(meneur.clone());
            } else {
                chemin.addLast(meneur.clone());
            }
        }
        double total = 0;
        Vector precedent = null;
        Iterator<Vector> points = depuisMeneur();
        while (points.hasNext()) {
            Vector point = points.next();
            if (precedent != null) {
                total += point.distance(precedent);
                if (total > longueurUtile) {
                    // Le premier point au-delà est gardé, les suivants oubliés.
                    while (points.hasNext()) {
                        points.next();
                        points.remove();
                    }
                    break;
                }
            }
            precedent = point;
        }
    }
}
