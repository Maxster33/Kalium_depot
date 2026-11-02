package fr.kalium.encapsuleur;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.Campfire;
import org.bukkit.block.Chest;
import org.bukkit.block.Jukebox;
import org.bukkit.block.data.Levelled;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ce que l'encapsuleur fera de chaque bloc d'une zone (cahier « Blocs, cultures et objets », section 3, décidé par
 * LeKiwi06) :
 * - emporté : tout bloc cassable, avec ses données (panneaux, bannières, têtes, ruches et leurs abeilles, spawners de
 *   KS_Spawners, coffres renforcés et shulkers améliorées vides...), blocs custom de KS_Blocs compris ;
 * - reste sur place : un contenant qui contient quelque chose (coffres, tonneaux, fours, entonnoirs, alambics,
 *   crafters...), un objet posé dans un bloc (feu de camp, jukebox, pupitre, pot décoré, bibliothèque sculptée,
 *   étagère, bloc suspect) et un bloc incassable (bedrock, portail...) ;
 * - eau et lave : les sources sont emportées, l'eau qui coule est seulement enlevée.
 */
final class Analyse {

    enum Sort { AIR, EMPORTE, FLUIDE_QUI_COULE, RESTE_CONTENANT, RESTE_INCASSABLE }

    /** Une zone (coins inclus). */
    record Zone(World monde, int x1, int y1, int z1, int x2, int y2, int z2) {

        static Zone de(Block a, Block b) {
            return new Zone(a.getWorld(), Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ()));
        }

        int sx() {
            return x2 - x1 + 1;
        }

        int sy() {
            return y2 - y1 + 1;
        }

        int sz() {
            return z2 - z1 + 1;
        }

        long volume() {
            return (long) sx() * sy() * sz();
        }

        boolean contient(Block b) {
            return b.getWorld().equals(monde) && b.getX() >= x1 && b.getX() <= x2 && b.getY() >= y1 && b.getY() <= y2
                    && b.getZ() >= z1 && b.getZ() <= z2;
        }
    }

    /** Résultat : blocs emportés, et ceux qui restent (par type, avec leurs positions). */
    static final class Bilan {
        int emportes;
        final Map<Material, List<Block>> restent = new LinkedHashMap<>();
        /** Positions relatives des blocs qui restent (x, y, z à la suite). */
        final List<Integer> restantsRelatifs = new ArrayList<>();

        int nombreRestants() {
            return restantsRelatifs.size() / 3;
        }
    }

    private Analyse() {
    }

    static Bilan analyser(Zone zone) {
        Bilan bilan = new Bilan();
        for (int y = zone.y1(); y <= zone.y2(); y++) {
            for (int x = zone.x1(); x <= zone.x2(); x++) {
                for (int z = zone.z1(); z <= zone.z2(); z++) {
                    Block b = zone.monde().getBlockAt(x, y, z);
                    Sort sort = sort(b);
                    if (sort == Sort.EMPORTE) {
                        bilan.emportes++;
                    } else if (sort == Sort.RESTE_CONTENANT || sort == Sort.RESTE_INCASSABLE) {
                        bilan.restent.computeIfAbsent(b.getType(), t -> new ArrayList<>()).add(b);
                        bilan.restantsRelatifs.add(x - zone.x1());
                        bilan.restantsRelatifs.add(y - zone.y1());
                        bilan.restantsRelatifs.add(z - zone.z1());
                    }
                }
            }
        }
        return bilan;
    }

    static Sort sort(Block b) {
        Material type = b.getType();
        if (type.isAir()) {
            return Sort.AIR;
        }
        if (type == Material.WATER || type == Material.LAVA) {
            return b.getBlockData() instanceof Levelled l && l.getLevel() == 0 ? Sort.EMPORTE : Sort.FLUIDE_QUI_COULE;
        }
        if (type.getHardness() < 0) {
            return Sort.RESTE_INCASSABLE;
        }
        return plein(b.getState(false)) ? Sort.RESTE_CONTENANT : Sort.EMPORTE;
    }

    /** Un contenant, ou un bloc où un objet est posé, qui contient quelque chose. */
    private static boolean plein(BlockState etat) {
        if (etat instanceof Chest coffre) {
            return !coffre.getBlockInventory().isEmpty();
        }
        if (etat instanceof io.papermc.paper.block.TileStateInventoryHolder contenant) {
            return !contenant.getInventory().isEmpty();
        }
        if (etat instanceof Jukebox jukebox) {
            return jukebox.hasRecord();
        }
        if (etat instanceof Campfire feu) {
            for (int i = 0; i < feu.getSize(); i++) {
                if (feu.getItem(i) != null && !feu.getItem(i).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        if (etat instanceof BrushableBlock suspect) {
            return suspect.getItem() != null && !suspect.getItem().isEmpty();
        }
        return false;
    }

    /** Remplaçables à la pose : air, eau, herbe, neige (décidé par LeKiwi06). */
    static boolean remplacable(Material type) {
        return type.isAir() || switch (type) {
            case WATER, SHORT_GRASS, TALL_GRASS, FERN, LARGE_FERN, SNOW -> true;
            default -> false;
        };
    }
}
