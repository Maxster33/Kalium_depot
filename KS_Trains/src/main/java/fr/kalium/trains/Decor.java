package fr.kalium.trains;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1.3.0 (LeKiwi06 : « j'aimerais que les wagonnets soient un peu plus gros, le train a l'air minuscule » ; le jeu ne
 * grossit pas un wagonnet : réponse « grandis-le un peu pour les Java », et « le four central de la locomotive émet des
 * particules de feu de camp pour rendre ça plus impressionnant »).
 *
 * Décor des wagons d'un train : le chargement (four, coffre, entonnoir, jukebox) est affiché plus gros par un modèle 3D
 * de bloc posé dans le wagonnet (passager). Seuls les joueurs Java le voient ; les joueurs Bedrock voient les wagonnets
 * du jeu (choix de LeKiwi06). Le modèle n'est pas enregistré avec le monde : il est remis chaque seconde tant que le
 * wagon est dans un train, retiré sinon. Le four du milieu de la locomotive fume (feu de camp) quand il est alimenté.
 */
final class Decor {

    private final KSTrains plugin;
    private final NamespacedKey cleDecor;

    Decor(KSTrains plugin) {
        this.plugin = plugin;
        this.cleDecor = new NamespacedKey(plugin, "decor");
    }

    boolean estDecor(Entity entite) {
        return entite instanceof BlockDisplay && entite.getPersistentDataContainer().has(cleDecor, PersistentDataType.BYTE);
    }

    BlockDisplay decor(Minecart wagon) {
        for (Entity passager : wagon.getPassengers()) {
            if (estDecor(passager)) {
                return (BlockDisplay) passager;
            }
        }
        return null;
    }

    /** Bloc du chargement d'un wagon de train, ou null (wagonnet simple, contrôleur : on y monte). */
    private BlockData chargement(Minecart wagon, boolean alimente, Vector avant) {
        if (wagon instanceof PoweredMinecart) {
            BlockData four = Material.FURNACE.createBlockData();
            if (four instanceof Lightable allume) {
                allume.setLit(alimente);
            }
            if (four instanceof Directional tourne && avant != null) {
                tourne.setFacing(face(avant));
            }
            return four;
        }
        if (wagon instanceof StorageMinecart) {
            return Material.CHEST.createBlockData();
        }
        if (wagon instanceof HopperMinecart) {
            return Material.HOPPER.createBlockData();
        }
        if (plugin.estJukebox(wagon)) {
            return Material.JUKEBOX.createBlockData();
        }
        return null;
    }

    /**
     * 1.3.8 (LeKiwi06 : « les fours ne regardent pas dans le bon sens ») : la face avant d'un four regarde vers l'avant
     * du train (côté tête), arrondi au point cardinal le plus proche ; l'orientation du wagonnet du jeu, décalée, n'est
     * plus utilisée pour les fours.
     */
    private static BlockFace face(Vector avant) {
        if (Math.abs(avant.getX()) >= Math.abs(avant.getZ())) {
            return avant.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return avant.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    /**
     * Chaque seconde, wagon d'un train : le modèle est là, à jour (four allumé, tourné vers l'avant du train).
     *
     * @param avant direction de l'avant du train à ce wagon (vers le wagon de devant), ou null
     */
    void entretenir(Minecart wagon, boolean alimente, Vector avant) {
        BlockData bloc = chargement(wagon, alimente, avant);
        BlockDisplay modele = decor(wagon);
        if (bloc == null) {
            retirer(wagon);
            return;
        }
        if (modele == null) {
            modele = wagon.getWorld().spawn(wagon.getLocation(), BlockDisplay.class, d -> {
                d.getPersistentDataContainer().set(cleDecor, PersistentDataType.BYTE, (byte) 1);
                d.setPersistent(false);
                d.setTeleportDuration(1);
            });
            if (!wagon.addPassenger(modele)) {
                modele.remove();
                return;
            }
        }
        if (!bloc.equals(modele.getBlock())) {
            modele.setBlock(bloc);
        }
        orienter(wagon, modele);
    }

    /** 1.3.7 : chaque tick, wagon d'un train qui roule : le modèle suit la pente (1.3.8 : et le four, l'avant). */
    void orienter(Minecart wagon, Vector avant) {
        BlockDisplay modele = decor(wagon);
        if (modele == null) {
            return;
        }
        if (wagon instanceof PoweredMinecart && avant != null && modele.getBlock() instanceof Directional four
                && four.getFacing() != face(avant)) {
            BlockData tourne = modele.getBlock().clone();
            ((Directional) tourne).setFacing(face(avant));
            modele.setBlock(tourne);
        }
        orienter(wagon, modele);
    }

    /**
     * 1.3.7 (LeKiwi06, capture : « un minecart en pente ne bouge pas son bloc correctement ») : sur un rail en pente, le
     * modèle penche de 45° vers le haut de la pente (le wagonnet du jeu penche ainsi), tourné autour de son centre ;
     * ailleurs il est droit, tourné comme le wagon. Le modèle ne suit pas de lui-même l'inclinaison de son wagon (avant :
     * remis une fois par seconde, toujours droit). Changement adouci sur 2 ticks.
     */
    private void orienter(Minecart wagon, BlockDisplay modele) {
        float taille = (float) plugin.getConfig().getDouble("decor.taille", 0.875);
        float hauteur = (float) plugin.getConfig().getDouble("decor.hauteur", -0.1);
        Quaternionf pente = new Quaternionf();
        Block rail = Voie.rail(wagon);
        boolean penche = false;
        if (rail != null && rail.getBlockData() instanceof Rail donnees) {
            float quart = (float) (Math.PI / 4);
            switch (donnees.getShape()) {
                case ASCENDING_EAST -> { pente.rotateZ(quart); penche = true; }
                case ASCENDING_WEST -> { pente.rotateZ(-quart); penche = true; }
                case ASCENDING_SOUTH -> { pente.rotateX(-quart); penche = true; }
                case ASCENDING_NORTH -> { pente.rotateX(quart); penche = true; }
                default -> { }
            }
        }
        // Rotation autour du centre du bloc : le centre reste à (0, hauteur + taille / 2, 0).
        Vector3f demi = pente.transform(new Vector3f(taille / 2, taille / 2, taille / 2));
        Transformation voulue = new Transformation(new Vector3f(-demi.x, hauteur + taille / 2 - demi.y, -demi.z),
                pente, new Vector3f(taille, taille, taille), new Quaternionf());
        if (!voulue.equals(modele.getTransformation())) {
            modele.setInterpolationDelay(0);
            modele.setInterpolationDuration(2);
            modele.setTransformation(voulue);
        }
        // En pente, les axes du monde (la pente est calculée dessus) ; four : axes du monde aussi (sa face est donnée
        // par son bloc) ; sinon tourné comme le wagon.
        float cap = penche || wagon instanceof PoweredMinecart ? 0 : wagon.getLocation().getYaw();
        if (Math.abs(modele.getLocation().getYaw() - cap) > 1f) {
            modele.setRotation(cap, 0);
        }
    }

    /** Wagon sorti d'un train : plus de modèle. */
    void retirer(Minecart wagon) {
        BlockDisplay modele = decor(wagon);
        if (modele != null) {
            modele.remove();
        }
    }

    /** Fumée de feu de camp au-dessus d'un four (le four du milieu de la locomotive). */
    void fumer(Minecart four) {
        Location dessus = four.getLocation().add(0, 1.3, 0);
        four.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, dessus, 0, 0, 0.07, 0, 1);
    }
}
