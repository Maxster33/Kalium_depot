package fr.kalium.supertool;

import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockType;

/**
 * Une machine (Foreuse, Abatteuse, Excavatrice, Laboureuse) à un niveau (diamant, netherite). Son identifiant est la
 * marque de l'objet (`ks_supertool:outil`), le nom de sa texture et son id de KS_KaliumGive : « foreuse »,
 * « foreuse_netherite », « abatteuse »...
 */
record Outil(Machine machine, Niveau niveau) {

    /** Les quatre machines (cahiers locaux Marteau.md et Outils_3x3.md, noms « machine industrielle » du 20/11). */
    enum Machine {
        FOREUSE("foreuse", "Foreuse", Material.MACE, Material.MACE, Tag.MINEABLE_PICKAXE, true,
                "Mine en 3 × 3 ce qu'une pioche mine", null),
        ABATTEUSE("abatteuse", "Abatteuse", Material.DIAMOND_AXE, Material.NETHERITE_AXE, Tag.MINEABLE_AXE, false,
                "Coupe en 3 × 3 ce qu'une hache coupe", "Clic droit : écorce, gratte, retire la cire en 3 × 3"),
        EXCAVATRICE("excavatrice", "Excavatrice", Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                Tag.MINEABLE_SHOVEL, false, "Creuse en 3 × 3 ce qu'une pelle creuse", "Clic droit : chemins en 3 × 3"),
        LABOUREUSE("laboureuse", "Laboureuse", Material.DIAMOND_HOE, Material.NETHERITE_HOE, Tag.MINEABLE_HOE, false,
                "Casse en 3 × 3 ce qu'une houe casse", "Laboure, récolte et replante en 3 × 3");

        final String id;
        final String nom;
        final Material diamant;
        final Material netherite;
        /** Blocs que l'outil du jeu mine (cassés en 3 × 3). */
        final Tag<Material> mine;
        /** Foreuse seulement : un voisin plus dur que le bloc frappé reste en place (choix du 20/11). */
        final boolean regleDurete;
        final String usage;
        final String clicDroit;

        Machine(String id, String nom, Material diamant, Material netherite, Tag<Material> mine, boolean regleDurete,
                String usage, String clicDroit) {
            this.id = id;
            this.nom = nom;
            this.diamant = diamant;
            this.netherite = netherite;
            this.mine = mine;
            this.regleDurete = regleDurete;
            this.usage = usage;
            this.clicDroit = clicDroit;
        }

        Material base(Niveau niveau) {
            return niveau == Niveau.DIAMANT ? diamant : netherite;
        }
    }

    /** Les deux niveaux : solidité 6 fois l'outil du jeu ; la Foreuse (une masse) reçoit aussi vitesse et dégâts. */
    enum Niveau {
        DIAMANT(1561 * 6, 5.0, 8.0f, BlockTypeTagKeys.INCORRECT_FOR_DIAMOND_TOOL, Tag.INCORRECT_FOR_DIAMOND_TOOL),
        NETHERITE(2031 * 6, 6.0, 9.0f, BlockTypeTagKeys.INCORRECT_FOR_NETHERITE_TOOL,
                Tag.INCORRECT_FOR_NETHERITE_TOOL);

        final int solidite;
        /** Foreuse : bonus de dégâts de la main (1) : 6 au total en diamant, 7 en netherite. */
        final double degatsForeuse;
        /** Foreuse : vitesse de la pioche du même niveau. */
        final float vitesseForeuse;
        final TagKey<BlockType> mauvaisOutil;
        final Tag<Material> mauvaisOutilMateriaux;

        Niveau(int solidite, double degatsForeuse, float vitesseForeuse, TagKey<BlockType> mauvaisOutil,
               Tag<Material> mauvaisOutilMateriaux) {
            this.solidite = solidite;
            this.degatsForeuse = degatsForeuse;
            this.vitesseForeuse = vitesseForeuse;
            this.mauvaisOutil = mauvaisOutil;
            this.mauvaisOutilMateriaux = mauvaisOutilMateriaux;
        }
    }

    String id() {
        return machine.id + (niveau == Niveau.NETHERITE ? "_netherite" : "");
    }

    String nom() {
        return machine.nom + (niveau == Niveau.NETHERITE ? " en netherite" : "");
    }

    Material base() {
        return machine.base(niveau);
    }

    static Outil deId(String id) {
        if (id == null) {
            return null;
        }
        for (Machine m : Machine.values()) {
            for (Niveau n : Niveau.values()) {
                Outil o = new Outil(m, n);
                if (o.id().equals(id)) {
                    return o;
                }
            }
        }
        return null;
    }
}
