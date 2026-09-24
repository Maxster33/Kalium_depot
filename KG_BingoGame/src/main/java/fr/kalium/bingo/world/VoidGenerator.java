package fr.kalium.bingo.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/** Generateur de monde vide : la salle d'attente y est collee a plusieurs emplacements. */
public final class VoidGenerator extends ChunkGenerator {

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, 80, 0.5);
    }
}
