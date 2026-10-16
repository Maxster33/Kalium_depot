package fr.kalium.trains;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
    private BlockData chargement(Minecart wagon, boolean alimente) {
        if (wagon instanceof PoweredMinecart) {
            BlockData four = Material.FURNACE.createBlockData();
            if (four instanceof Lightable allume) {
                allume.setLit(alimente);
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

    /** Chaque seconde, wagon d'un train : le modèle est là, à jour (four allumé), tourné comme le wagon. */
    void entretenir(Minecart wagon, boolean alimente) {
        BlockData bloc = chargement(wagon, alimente);
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

    /** 1.3.7 : chaque tick, wagon d'un train qui roule : le modèle suit la pente. */
    void orienter(Minecart wagon) {
        BlockDisplay modele = decor(wagon);
        if (modele != null) {
            orienter(wagon, modele);
        }
    }

    /**
     * 1.3.7 (LeKiwi06, capture : « un minecart en pente ne bouge pas son bloc correctement ») : sur un rail en pente, le
     * modèle penche de 45° vers le haut de la pente (le wagonnet du jeu penche ainsi), tourné autour de son centre ;
     * ailleurs il est droit, tourné comme le wagon. Le modèle ne suit pas de lui-même l'inclinaison de son wagon (avant :
     * remis une fois par seconde, toujours droit). Changement adouci sur 2 ticks.
     */
    private void orienter(Minecart wagon, BlockDisplay modele) {
        float taille = (float) plugin.getConfig().getDouble("decor.taille", 1.0);
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
        // En pente, les axes du monde (la pente est calculée dessus) ; sinon tourné comme le wagon.
        float cap = penche ? 0 : wagon.getLocation().getYaw();
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
