package fr.kalium.bingo.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * 0.8.5 - demande de Maxster33 (03/10/2026) : la creation d'un overworld de partie figeait tout le serveur 5 a 6 s
 * (log « Selecting spawn point ... Prepared spawn area in 5049 ms ») : Minecraft cherche le point d'apparition d'un
 * nouveau monde sur le thread principal, en generant le terrain au passage.
 *
 * Ce generateur garde TOUTE la generation normale (relief, surface, grottes, decorations, structures, creatures : le
 * monde est identique a un monde sans generateur pour la meme seed) mais fournit lui-meme le point d'apparition, ce qui
 * saute cette recherche. Le vrai point de depart (sur la terre ferme) est choisi ensuite, une fois le terrain
 * pre-genere (voir InstanceWorldManager.placeSpawnOnLand).
 */
public final class FixedSpawnGenerator extends ChunkGenerator {

    /** Point provisoire : remplace par placeSpawnOnLand avant l'arrivee des joueurs. */
    static final int PROVISIONAL_Y = 100;

    @Override
    public boolean shouldGenerateNoise() {
        return true;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return true;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return true;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return true;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, PROVISIONAL_Y, 0.5);
    }
}
