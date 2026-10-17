package fr.kalium.trains;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
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

    /**
     * Chaque seconde, wagon d'un train : le modèle est là, à jour (four allumé), orienté le long du train.
     *
     * @param axe direction du train à ce wagon, vers l'avant (du wagon de derrière au wagon de devant), ou null
     */
    void entretenir(Minecart wagon, boolean alimente, Vector axe) {
        // 1.4.0 : `decor.actif` à false : pas de modèle agrandi (serveur allégé).
        if (!plugin.getConfig().getBoolean("decor.actif", true)) {
            retirer(wagon);
            return;
        }
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
        orienter(modele, axe);
    }

    /** Chaque tick, wagon d'un train qui roule : le modèle suit le train (pentes, virages). */
    void orienter(Minecart wagon, Vector axe) {
        BlockDisplay modele = decor(wagon);
        if (modele != null) {
            orienter(modele, axe);
        }
    }

    /**
     * 1.4.2 (LeKiwi06, vidéo : « le bloc bouge dans tous les sens en pente ») : l'orientation venait du bloc de rail
     * sous le wagon (1.3.7), qui change d'un tick à l'autre sur une pente (le wagonnet est à mi-hauteur entre deux
     * blocs) : le modèle basculait sans cesse entre penché et droit, et tournait (direction du wagonnet du jeu, ou point
     * cardinal du four). Maintenant le modèle est orienté sur l'axe du train à ce wagon (du wagon de derrière à celui de
     * devant) : sa face avant (celle du four) pointe vers l'avant du train, et il penche exactement comme la voie, avec
     * une transition douce en haut et en bas des pentes. Rotation autour de son centre, changement adouci sur 2 ticks,
     * renvoyé seulement s'il a tourné de plus d'un demi-degré.
     */
    private void orienter(BlockDisplay modele, Vector axe) {
        float taille = (float) plugin.getConfig().getDouble("decor.taille", 0.875);
        float hauteur = (float) plugin.getConfig().getDouble("decor.hauteur", -0.1);
        Quaternionf rotation = new Quaternionf();
        if (axe != null) {
            double horizontal = Math.sqrt(axe.getX() * axe.getX() + axe.getZ() * axe.getZ());
            if (horizontal > 1e-4) {
                // Face avant du bloc (nord, -Z) vers l'axe : cap autour de Y, puis inclinaison autour de X.
                float cap = (float) Math.atan2(-axe.getX(), -axe.getZ());
                float pente = (float) Math.atan2(axe.getY(), horizontal);
                rotation.rotateY(cap).rotateX(pente);
            }
        }
        // Rotation autour du centre du bloc : le centre reste à (0, hauteur + taille / 2, 0).
        Vector3f demi = rotation.transform(new Vector3f(taille / 2, taille / 2, taille / 2), new Vector3f());
        Vector3f decalage = new Vector3f(-demi.x, hauteur + taille / 2 - demi.y, -demi.z);
        Transformation actuelle = modele.getTransformation();
        // 1.4.4 : produit scalaire calculé ici : Quaternionf.dot(Quaternionf) de JOML 1.10.8 (compilation) n'existe plus
        // dans JOML 1.10.9 (serveur Paper 26.3) : erreur à chaque tick dans la console, modèles jamais orientés.
        Quaternionf avant = actuelle.getLeftRotation();
        float dot = avant.x * rotation.x + avant.y * rotation.y + avant.z * rotation.z + avant.w * rotation.w;
        if (Math.abs(dot) > Math.cos(Math.toRadians(0.25))
                && Math.abs(actuelle.getScale().x - taille) < 1e-4
                && actuelle.getTranslation().distance(decalage) < 1e-3) {
            return;
        }
        Transformation voulue = new Transformation(decalage, rotation, new Vector3f(taille, taille, taille),
                new Quaternionf());
        modele.setInterpolationDelay(0);
        modele.setInterpolationDuration(2);
        modele.setTransformation(voulue);
        if (Math.abs(modele.getLocation().getYaw()) > 0.5f) {
            modele.setRotation(0, 0);
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
