package fr.kalium.malefices;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Les effets des maléfices et leur montée avec les jours (cahier, partie 3.2), lus dans config.yml.
 *
 * Jours 1 à n (n effets) : un effet de plus par jour, dans l'ordre. Ensuite, une montée par jour, dans le même ordre :
 * un niveau de plus (« niveau »), ou des secondes de plus par activation (« duree » : obscurité, lévitation, nausée) ;
 * un effet qui ne monte pas (« non » : infestation) ou déjà à son maximum est sauté. Quand plus rien ne monte, les jours
 * suivants restent comme le dernier.
 */
final class Malefices {

    /** Un activation dure au plus une minute (une minute du jour). */
    static final int DUREE_MAX = 60;

    enum Montee { NIVEAU, DUREE, NON }

    /** Un effet de la liste. */
    record Effet(PotionEffectType type, String nom, int duree, Montee montee, int niveauMax, int pas) {
    }

    /** Un effet au jour donné : niveau (1 = I) et durée d'une activation en secondes. */
    record Etat(int index, Effet effet, int niveau, int duree) {
    }

    private final List<Effet> effets = new ArrayList<>();

    Malefices(FileConfiguration config, Logger log) {
        for (Map<?, ?> m : config.getMapList("effets")) {
            String id = String.valueOf(m.get("effet"));
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(id.toLowerCase()));
            if (type == null) {
                log.warning("Effet inconnu dans config.yml, ignoré : " + id);
                continue;
            }
            Object nom = m.get("nom");
            Montee montee = switch (String.valueOf(m.get("monte")).toLowerCase()) {
                case "niveau" -> Montee.NIVEAU;
                case "duree", "durée" -> Montee.DUREE;
                default -> Montee.NON;
            };
            effets.add(new Effet(type, nom == null ? id : String.valueOf(nom), borne(entier(m.get("duree"), 60), 1, DUREE_MAX),
                    montee, Math.max(1, entier(m.get("niveau-max"), 10)), Math.max(1, entier(m.get("duree-pas"), 10))));
        }
    }

    private static int entier(Object o, int defaut) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return o == null ? defaut : Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    private static int borne(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    int taille() {
        return effets.size();
    }

    /** Les effets actifs au jour donné (0 : aucun), dans l'ordre de la liste. */
    List<Etat> au(int jour) {
        Map<Integer, int[]> e = new LinkedHashMap<>();
        for (int i = 0; i < effets.size() && i < jour; i++) {
            e.put(i, new int[]{1, effets.get(i).duree()});
        }
        int j = effets.size();
        while (j < jour) {
            boolean monte = false;
            for (int i = 0; i < effets.size() && j < jour; i++) {
                Effet f = effets.get(i);
                int[] v = e.get(i);
                if (f.montee() == Montee.NIVEAU && v[0] < f.niveauMax()) {
                    v[0]++;
                } else if (f.montee() == Montee.DUREE && v[1] < DUREE_MAX) {
                    v[1] = Math.min(DUREE_MAX, v[1] + f.pas());
                } else {
                    continue;
                }
                j++;
                monte = true;
            }
            if (!monte) {
                break;
            }
        }
        List<Etat> r = new ArrayList<>();
        e.forEach((i, v) -> r.add(new Etat(i, effets.get(i), v[0], v[1])));
        return r;
    }

    /** Nom affiché avec le niveau : « Faim II » ; sans niveau pour un effet qui monte en durée. */
    static String nom(Etat e) {
        if (e.niveau() <= 1 || e.effet().montee() == Montee.DUREE) {
            return e.effet().nom();
        }
        String[] romains = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return e.effet().nom() + " " + (e.niveau() < romains.length ? romains[e.niveau()] : String.valueOf(e.niveau()));
    }

    /** Niveau payé à l'amulette : le niveau, 1 pour un effet qui monte en durée. */
    static int niveauPaye(Etat e) {
        return e.effet().montee() == Montee.DUREE ? 1 : e.niveau();
    }
}
