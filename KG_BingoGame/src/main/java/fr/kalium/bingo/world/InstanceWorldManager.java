package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.util.TriState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * Creation/suppression des mondes d'instance Bingo (section 8).
 *
 * IMPORTANT - decision technique NON figee :
 * Cette implementation genere chaque monde a la volee via WorldCreator +
 * seed identique (approche confirmee par l'utilisateur : "Terrain genere a
 * la volee par seed" plutot qu'un monde modele/copie). Le cahier des
 * charges (section 8) demandait de proposer une alternative "monde
 * modele/copie" si elle s'averait plus fiable techniquement - point deja
 * tranche avec l'utilisateur, generation live retenue.
 *
 * Limite connue, non encore traitee : la generation live de plusieurs
 * instances en meme temps peut etre couteuse en CPU/disque au lancement
 * d'une partie (instances.max-simultaneous-games sert de garde-fou
 * prudent en attendant une mesure reelle des performances).
 *
 * 0.6.0 (allegement, demande de LeKiwi06 du 24/09/2026) : la zone de spawn des mondes de partie n'est plus
 * gardee chargee en permanence (keepSpawnLoaded = false : creation plus courte, moins de memoire par monde),
 * et les dossiers des maps terminees sont effaces du disque en arriere-plan.
 */
public class InstanceWorldManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    public InstanceWorldManager(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /**
     * Cree (ou recree) un monde d'instance avec la seed donnee.
     *
     * @param worldName nom unique du monde (ex: "bingo_<gameId>_1")
     * @param seed      seed partagee par toutes les instances d'une meme partie
     */
    public World createInstanceWorld(String worldName, long seed) {
        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalStateException(
                    "Un monde nomme '" + worldName + "' existe deja - reinitialiser avant de recreer.");
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.seed(seed);
        creator.type(WorldType.NORMAL);
        creator.environment(World.Environment.NORMAL);
        creator.keepSpawnLoaded(TriState.FALSE); // 0.6.0, voir en-tete

        logger.info("[KG_BingoGame] Generation du monde d'instance '" + worldName
                + "' (seed=" + seed + ")...");

        World world = creator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Echec de creation du monde '" + worldName + "'.");
        }

        // Le point de depart doit etre le spawn naturel du monde, identique
        // pour toutes les instances d'une meme partie (section 0 et 9).
        world.setSpawnFlags(true, true);
        applyGameRules(world);

        return world;
    }

    // ------------------------------------------------------------------ Nether et End (0.1.22)

    /** Suffixes des mondes Nether / End d'une instance (demande explicite de l'utilisateur, 0.1.22 : "le nether
     *  et l'end doivent etre generes comme l'overworld (chacun le sien)"). */
    public static final String NETHER_SUFFIX = "_nether";
    public static final String END_SUFFIX = "_the_end";

    public static String dimensionName(String baseWorldName, World.Environment environment) {
        return switch (environment) {
            case NETHER -> baseWorldName + NETHER_SUFFIX;
            case THE_END -> baseWorldName + END_SUFFIX;
            default -> baseWorldName;
        };
    }

    /** Nom de l'overworld d'instance correspondant a un monde (lui-meme, ou son Nether / son End). */
    public static String baseNameOf(String worldName) {
        if (worldName.endsWith(NETHER_SUFFIX)) {
            return worldName.substring(0, worldName.length() - NETHER_SUFFIX.length());
        }
        if (worldName.endsWith(END_SUFFIX)) {
            return worldName.substring(0, worldName.length() - END_SUFFIX.length());
        }
        return worldName;
    }

    /**
     * Nether ou End PROPRE a une instance (meme seed que son overworld) : charge s'il existe deja sur le disque,
     * genere sinon. Renvoie le monde deja charge s'il l'est.
     */
    public World createDimension(String baseWorldName, World.Environment environment, long seed) {
        String name = dimensionName(baseWorldName, environment);
        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            return existing;
        }
        WorldCreator creator = new WorldCreator(name);
        creator.seed(seed);
        creator.type(WorldType.NORMAL);
        creator.environment(environment);
        creator.keepSpawnLoaded(TriState.FALSE); // 0.6.0, voir en-tete
        logger.info("[KG_BingoGame] Generation du monde d'instance '" + name + "' (seed=" + seed + ")...");
        World world = creator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Echec de creation du monde '" + name + "'.");
        }
        world.setSpawnFlags(true, true);
        applyGameRules(world);
        return world;
    }

    /** Recharge (sans generer) le Nether et l'End d'une instance s'ils existent deja sur le disque (redemarrage). */
    public void loadDimensionsIfPresent(String baseWorldName, long seed) {
        for (World.Environment environment : new World.Environment[] {World.Environment.NETHER, World.Environment.THE_END}) {
            String name = dimensionName(baseWorldName, environment);
            if (Bukkit.getWorld(name) == null && existsOnDisk(name)) {
                try {
                    createDimension(baseWorldName, environment, seed);
                } catch (RuntimeException e) {
                    logger.warning("[KG_BingoGame] Rechargement du monde '" + name + "' echoue : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Regles de jeu des mondes de partie (0.1.22) : keepInventory active - demande explicite de l'utilisateur :
     * "il faut activer le keep inventory pour les joueurs du Bingo lorsqu'ils sont dans une partie et dans les 3
     * dimensions". Les mondes de partie n'accueillent que des joueurs en partie (la salle d'attente est un autre
     * monde, non concerne).
     */
    public void applyGameRules(World world) {
        world.setGameRule(org.bukkit.GameRules.KEEP_INVENTORY, true); // 0.3.0 : GameRule.KEEP_INVENTORY est "a supprimer" depuis 1.21.11
    }

    public Location getNaturalSpawn(World world) {
        return world.getSpawnLocation();
    }

    /**
     * Decharge et supprime completement le dossier d'un monde d'instance
     * (section 1, etape 12 : "suppression des map utilisees").
     */
    public void deleteInstanceWorld(String worldName) {
        // 0.1.22 : l'overworld de l'instance ET son Nether / son End.
        deleteWorld(dimensionName(worldName, World.Environment.NETHER));
        deleteWorld(dimensionName(worldName, World.Environment.THE_END));
        deleteWorld(worldName);
    }

    /**
     * Seuls les mondes de partie ("..._<uuid de partie>_<n>", eventuellement suivi de _nether / _the_end) peuvent
     * etre supprimes : garde-fou contre toute suppression d'un autre monde (salle d'attente, monde principal).
     */
    private static final java.util.regex.Pattern INSTANCE_NAME = java.util.regex.Pattern.compile(
            ".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}_\\d+(" + NETHER_SUFFIX + "|" + END_SUFFIX + ")?");

    /**
     * Dossiers possibles d'un monde. 0.1.22 : sur ce serveur (Paper 26.x) les mondes ajoutes ne sont PAS a la
     * racine du serveur mais dans "<monde principal>/dimensions/minecraft/<nom>" - l'ancien code ne cherchait
     * qu'a la racine, si bien que les maps des parties terminees n'etaient jamais effacees du disque.
     */
    private java.util.Set<Path> candidateFolders(String worldName) {
        java.util.Set<Path> candidates = new java.util.LinkedHashSet<>();
        candidates.add(new File(Bukkit.getWorldContainer(), worldName).toPath().toAbsolutePath().normalize());
        for (World loaded : Bukkit.getWorlds()) {
            try {
                Path parent = loaded.getWorldPath().toAbsolutePath().normalize().getParent();
                if (parent != null) {
                    candidates.add(parent.resolve(worldName));
                }
            } catch (RuntimeException ignored) {
                // monde sans dossier connu
            }
        }
        return candidates;
    }

    /** true si le dossier de ce monde existe deja sur le disque (a la racine ou dans dimensions/minecraft). */
    public boolean existsOnDisk(String worldName) {
        for (Path candidate : candidateFolders(worldName)) {
            if (Files.isDirectory(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void deleteWorld(String worldName) {
        if (!INSTANCE_NAME.matcher(worldName).matches()) {
            logger.severe("[KG_BingoGame] Suppression refusee : '" + worldName + "' n'est pas un monde de partie.");
            return;
        }
        java.util.Set<Path> folders = new java.util.LinkedHashSet<>();
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            folders.add(world.getWorldPath().toAbsolutePath().normalize());
            for (var player : world.getPlayers()) {
                // Les joueurs doivent avoir ete deplaces AVANT cet appel ;
                // ceci est une securite, pas le mecanisme principal de sortie.
                logger.warning("[KG_BingoGame] Joueur " + player.getName()
                        + " encore present dans '" + worldName + "' lors de la suppression.");
            }
            boolean unloaded = Bukkit.unloadWorld(world, false);
            if (!unloaded) {
                logger.severe("[KG_BingoGame] Impossible de decharger le monde '" + worldName + "'.");
                return;
            }
        }

        folders.addAll(candidateFolders(worldName));
        boolean wasLoaded = world != null;
        Runnable delete = () -> {
            boolean deleted = false;
            for (Path folder : folders) {
                Path name = folder.getFileName();
                if (name != null && name.toString().equals(worldName) && Files.isDirectory(folder)) {
                    deleteDirectoryRecursively(folder);
                    deleted = true;
                    logger.info("[KG_BingoGame] Monde '" + worldName + "' supprime du disque (" + folder + ").");
                }
            }
            if (!deleted && wasLoaded) {
                logger.warning("[KG_BingoGame] Dossier du monde '" + worldName + "' introuvable : rien n'a ete supprime du disque.");
            }
        };
        // 0.6.0 : effacement des fichiers en arriere-plan (5 s apres le dechargement, le temps que le serveur
        // referme les fichiers du monde) plutot que sur le thread principal.
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, delete, 100L);
        } else {
            delete.run();
        }
    }

    private void deleteDirectoryRecursively(Path path) {
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.warning("[KG_BingoGame] Impossible de supprimer " + p + " : " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.severe("[KG_BingoGame] Erreur lors de la suppression du dossier " + path + " : " + e.getMessage());
        }
    }
}
