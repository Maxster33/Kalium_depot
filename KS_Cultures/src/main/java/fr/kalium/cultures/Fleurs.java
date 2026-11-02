package fr.kalium.cultures;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Culture des fleurs (cahier local, parties 10 et 14) : au composteur, 1 fleur -> 1 à 3 graines ; une graine posée sur
 * la terre labourée reste « en croissance » (variante « fleur » de ks_cultures_terre), puis la vraie fleur est posée.
 *
 * Torchiflore et plante carnivore : leurs graines vanilla (elles poussent déjà en vanilla). Oeilchidées ouverte et
 * fermée : une seule graine, qui donne l'oeilchidée fermée (elle s'ouvre la nuit). Pas de graine pour la rose de
 * wither ni le pissenlit doré (2,53 et 0,38 au barème, absents du tableau validé : question posée à LeKiwi06).
 */
final class Fleurs {

    /** Graine custom -> fleur posée à maturité. */
    static final Map<String, Material> GRAINES = new LinkedHashMap<>();
    /** Nom français de chaque graine custom. */
    static final Map<String, String> NOMS = new LinkedHashMap<>();

    static {
        fleur(Material.DANDELION, "Graines de pissenlit");
        fleur(Material.POPPY, "Graines de coquelicot");
        fleur(Material.BLUE_ORCHID, "Graines d'orchidée bleue");
        fleur(Material.ALLIUM, "Graines d'allium");
        fleur(Material.AZURE_BLUET, "Graines de houstonie bleue");
        fleur(Material.RED_TULIP, "Graines de tulipe rouge");
        fleur(Material.ORANGE_TULIP, "Graines de tulipe orange");
        fleur(Material.WHITE_TULIP, "Graines de tulipe blanche");
        fleur(Material.PINK_TULIP, "Graines de tulipe rose");
        fleur(Material.OXEYE_DAISY, "Graines de marguerite");
        fleur(Material.CORNFLOWER, "Graines de bleuet");
        fleur(Material.LILY_OF_THE_VALLEY, "Graines de muguet");
        GRAINES.put("graine_eyeblossom", Material.CLOSED_EYEBLOSSOM);
        NOMS.put("graine_eyeblossom", "Graines d'oeilchidée");
        fleur(Material.SUNFLOWER, "Graines de tournesol");
        fleur(Material.LILAC, "Graines de lilas");
        fleur(Material.ROSE_BUSH, "Graines de rosier");
        fleur(Material.PEONY, "Graines de pivoine");
    }

    private static void fleur(Material m, String nom) {
        String id = "graine_" + m.name().toLowerCase(Locale.ROOT);
        GRAINES.put(id, m);
        NOMS.put(id, nom);
    }

    /** Grandes fleurs : plus de duplication à la poudre d'os. */
    static boolean grande(Material m) {
        return m == Material.SUNFLOWER || m == Material.LILAC || m == Material.ROSE_BUSH || m == Material.PEONY;
    }

    /**
     * La graine que donne cette fleur au composteur : id d'une graine custom, « minecraft:torchflower_seeds » /
     * « minecraft:pitcher_pod », ou null si la fleur n'en donne pas.
     */
    static String graineDe(Material fleur) {
        if (fleur == Material.TORCHFLOWER) {
            return "minecraft:torchflower_seeds";
        }
        if (fleur == Material.PITCHER_PLANT) {
            return "minecraft:pitcher_pod";
        }
        if (fleur == Material.OPEN_EYEBLOSSOM || fleur == Material.CLOSED_EYEBLOSSOM) {
            return "graine_eyeblossom";
        }
        String id = "graine_" + fleur.name().toLowerCase(Locale.ROOT);
        return GRAINES.containsKey(id) ? id : null;
    }

    /** Id du type de bloc custom d'une graine de fleur en croissance. */
    static String type(String graine) {
        return "ks_cultures:fleur_" + graine.substring("graine_".length());
    }

    /** La graine d'un type de fleur en croissance, ou null. */
    static String graineDuType(String type) {
        if (type == null || !type.startsWith("ks_cultures:fleur_")) {
            return null;
        }
        String g = "graine_" + type.substring("ks_cultures:fleur_".length());
        return GRAINES.containsKey(g) ? g : null;
    }

    private Fleurs() {
    }
}
