package fr.kalium.boatrace;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.boat.ChestRaft;
import net.minecraft.world.entity.vehicle.boat.Raft;
import net.minecraft.world.item.Item;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * 1.6.2 : bateaux de course « non solides » cote serveur (anti-collision, demande de LeKiwi06 du 17/10/2026).
 *
 * Cause des collisions restantes (journal de Kal-Games, 10 au 17/10/2026) : en bateau, le serveur rejoue chaque
 * deplacement envoye par le jeu du joueur, avec les collisions entre entites. Un bateau est toujours solide pour les
 * autres bateaux (AbstractBoat.canBeCollidedWith renvoie toujours vrai, aucun reglage ni API ne le change) : quand un
 * coureur entre dans le bateau cache d'un adversaire, le serveur le ramene en arriere (« moved wrongly » dans le
 * journal ; 101 fois pendant la course a 4 du 17/10 vers minuit, jamais en 277 minutes de courses solo). Cacher les
 * bateaux aux joueurs (CollisionShield) ne suffit donc pas.
 *
 * Ici, chaque bateau de course est une sous-classe du bateau du jeu qui n'est ni solide ni poussable : le serveur
 * l'ignore dans les collisions entre entites (les blocs comptent toujours). Les joueurs le voient comme un bateau
 * normal (meme type d'entite). Les pilotes sont rendus non solides par CollisionShield (Player#setCollidable).
 *
 * Ce code utilise les classes internes du serveur (Paper 26.3, noms du jeu en clair) : a verifier a chaque mise a
 * jour de Paper. S'il ne fonctionne plus, spawn renvoie null et la course utilise un bateau normal (message dans
 * le journal du serveur, une seule fois).
 */
final class GhostBoats {

    private static boolean warned;

    private GhostBoats() {
    }

    /** Bateau non solide de ce type a cet endroit, ou null si le serveur ne le permet pas (bateau normal a prevoir). */
    static Entity spawn(World world, Location at, EntityType type, Logger logger) {
        net.minecraft.world.entity.Entity boat = null;
        try {
            ServerLevel level = ((CraftWorld) world).getHandle();
            Identifier id = Identifier.parse(type.getKey().toString());
            net.minecraft.world.entity.EntityType<?> nmsType = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            Supplier<Item> drop = () -> item;
            String path = id.getPath();
            if (path.endsWith("_chest_raft")) {
                boat = new GhostChestRaft(cast(nmsType), level, drop);
            } else if (path.endsWith("_chest_boat")) {
                boat = new GhostChestBoat(cast(nmsType), level, drop);
            } else if (path.endsWith("_raft")) {
                boat = new GhostRaft(cast(nmsType), level, drop);
            } else if (path.endsWith("_boat")) {
                boat = new GhostBoat(cast(nmsType), level, drop);
            } else {
                return null;
            }
            boat.snapTo(at.getX(), at.getY(), at.getZ(), at.getYaw(), 0f);
            if (!level.addFreshEntity(boat, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
                return null;
            }
            return boat.getBukkitEntity();
        } catch (RuntimeException | LinkageError e) {
            if (boat != null && boat.isAlive()) {
                boat.discard(); // pas de bateau vide oublie sur la piste
            }
            if (!warned) {
                warned = true;
                logger.warning("Bateaux non solides indisponibles (version de Paper ?), bateaux normaux utilises : " + e);
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.entity.Entity> net.minecraft.world.entity.EntityType<T> cast(
            net.minecraft.world.entity.EntityType<?> type) {
        return (net.minecraft.world.entity.EntityType<T>) type;
    }

    private static final class GhostBoat extends Boat {
        GhostBoat(net.minecraft.world.entity.EntityType<? extends Boat> type, ServerLevel level, Supplier<Item> drop) {
            super(type, level, drop);
        }

        @Override
        public boolean canBeCollidedWith(net.minecraft.world.entity.Entity other) {
            return false;
        }

        @Override
        public boolean isPushable() {
            return false;
        }
    }

    private static final class GhostRaft extends Raft {
        GhostRaft(net.minecraft.world.entity.EntityType<? extends Raft> type, ServerLevel level, Supplier<Item> drop) {
            super(type, level, drop);
        }

        @Override
        public boolean canBeCollidedWith(net.minecraft.world.entity.Entity other) {
            return false;
        }

        @Override
        public boolean isPushable() {
            return false;
        }
    }

    private static final class GhostChestBoat extends ChestBoat {
        GhostChestBoat(net.minecraft.world.entity.EntityType<? extends ChestBoat> type, ServerLevel level, Supplier<Item> drop) {
            super(type, level, drop);
        }

        @Override
        public boolean canBeCollidedWith(net.minecraft.world.entity.Entity other) {
            return false;
        }

        @Override
        public boolean isPushable() {
            return false;
        }
    }

    private static final class GhostChestRaft extends ChestRaft {
        GhostChestRaft(net.minecraft.world.entity.EntityType<? extends ChestRaft> type, ServerLevel level, Supplier<Item> drop) {
            super(type, level, drop);
        }

        @Override
        public boolean canBeCollidedWith(net.minecraft.world.entity.Entity other) {
            return false;
        }

        @Override
        public boolean isPushable() {
            return false;
        }
    }
}
