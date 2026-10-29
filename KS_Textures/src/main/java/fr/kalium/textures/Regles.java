package fr.kalium.textures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Quel objet custom porte quelle texture : chaque règle reconnaît un objet à la marque que son plugin écrit dans
 * l'objet (clé du PersistentDataContainer, et sa valeur quand le plugin y range un identifiant). L'identifiant de la
 * texture est celui du pack (modèle {@code kalium:item/<id>}) et de {@code visuels-objets/png/<id>.png}.
 *
 * Ajouter un objet : une ligne ici, son PNG dans visuels-objets, puis refaire le pack (visuels-objets/pack.py).
 */
final class Regles {

    /** Compartiment du PersistentDataContainer de Bukkit dans le composant custom_data. */
    private static final String PDC = "PublicBukkitValues";

    private record Regle(String texture, String cle, String valeur, Predicate<ItemStack> filtre) {
    }

    private static final List<Regle> REGLES = new ArrayList<>();
    static final Set<String> TEXTURES = new HashSet<>();

    private static void r(String texture, String cle) {
        r(texture, cle, null, null);
    }

    private static void r(String texture, String cle, String valeur) {
        r(texture, cle, valeur, null);
    }

    private static void r(String texture, String cle, String valeur, Predicate<ItemStack> filtre) {
        REGLES.add(new Regle(texture, cle, valeur, filtre));
        TEXTURES.add(texture);
    }

    static {
        r("estomac_gardien", "ks_estomacgardien:estomac");
        r("cle_de_l_end", "ks_ec_extension:cle_de_l_end");
        r("bedrock_breaker", "ks_bedrockbreaker:bedrock_breaker");
        r("bedrock_breaker", "ks_crafts:bedrock_breaker"); // ancien marqueur, encore lu par KS_BedrockBreaker
        r("fragment_spawner", "ks_itemsimple:objet", "fragment_spawner");
        r("coeur_spawner", "ks_itemsimple:objet", "coeur_spawner");
        r("changeur_biome", "ks_biomechanger:changeur_biome");
        for (String t : List.of("fly", "mort", "tp", "localisation", "claim")) {
            r("jeton_" + t, "ks_jetons:jeton", t);
        }
        for (String t : List.of("fly", "mort", "tp", "localisation")) {
            r("badge_" + t, "ks_jetons:badge", t);
        }
        r("cristal_purification", "ks_meule:cristal");
        r("casse_croute_dore", "ks_cassecroute:casse_croute");

        // KS_Alcools : la version jetable (potion jetable) garde l'image de l'alcool
        for (String a : List.of("neutre", "tequila", "vodka", "hydromel", "manzana", "biere", "tabascoolise")) {
            r("alcool_" + a, "ks_alcools:alcool", a);
        }
        r("molotov", "ks_alcools:molotov");
        r("bandage", "ks_alcools:bandage");
        r("torche_augmentee", "ks_alcools:torche_augmentee");

        for (String e : List.of("super_gateau", "chauve_souris", "anguille", "ignifugation", "plume", "phenix",
                "fantome", "titan", "vent", "rebond", "fortune")) {
            r("elixir_" + e, "ks_elixir:elixir", e);
        }

        r("locomotive", "ks_trains:locomotive");
        r("wagon_controleur", "ks_trains:controleur-objet");
        r("wagon_jukebox", "ks_trains:jukebox-objet");
        r("boussole_train", "ks_trains:boussole-train");
        r("verrou_vehicule", "ks_verrouvehicule:verrou");

        // KS_Cuisine (pomme d'amour : pomme vanilla à l'aspect enchanté, pas de texture)
        for (String a : List.of("cookie_baie_sucree", "cookie_baie_lumineuse", "sandwich", "brunch", "ble_souffle",
                "bol_cereales")) {
            r(a, "ks_cuisine:aliment", a);
        }

        for (int t = 1; t <= 6; t++) {
            r("emeraude_compressee_" + t, "ks_economy:tier", String.valueOf(t));
        }
        r("bloc_charbon_de_bois", "ks_crafts:bloc_charbon_de_bois");
        r("fiole_experience", "ks_fioleexp:points");
        r("fiole_experience_mort", "ks_coffremort:points");

        // KS_JukeboxBoat : le bateau à coffre et jukebox prend l'image du bateau à jukebox (il a déjà l'aspect enchanté)
        r("radeau_jukebox", "ks_jukeboxboat:bateau", null,
                s -> s.is(Items.BAMBOO_RAFT) || s.is(Items.BAMBOO_CHEST_RAFT));
        r("bateau_jukebox", "ks_jukeboxboat:bateau");
    }

    private Regles() {
    }

    /** La texture de l'objet, ou null si ce n'est pas un objet custom connu. */
    static String texture(ItemStack objet) {
        if (objet == null || objet.isEmpty()) {
            return null;
        }
        CustomData donnees = objet.get(DataComponents.CUSTOM_DATA);
        if (donnees == null) {
            return null;
        }
        Tag pdc = donnees.getUnsafe().get(PDC);
        if (!(pdc instanceof CompoundTag marques)) {
            return null;
        }
        for (Regle regle : REGLES) {
            Tag valeur = marques.get(regle.cle);
            if (valeur == null || regle.valeur != null && !regle.valeur.equals(texte(valeur))) {
                continue;
            }
            if (regle.filtre == null || regle.filtre.test(objet)) {
                return regle.texture;
            }
        }
        return null;
    }

    private static String texte(Tag valeur) {
        if (valeur instanceof StringTag s) {
            return s.value();
        }
        if (valeur instanceof NumericTag n) {
            return String.valueOf(n.intValue());
        }
        return null;
    }

    /**
     * Une copie de l'objet avec sa texture en première chaîne de custom_model_data (le pack choisit le modèle d'après
     * elle), ou l'objet lui-même s'il n'a pas de texture ou l'a déjà. L'objet reçu n'est jamais modifié : il peut être
     * celui du serveur.
     */
    static ItemStack marquer(ItemStack objet) {
        String texture = texture(objet);
        if (texture == null) {
            return objet;
        }
        CustomModelData actuel = objet.get(DataComponents.CUSTOM_MODEL_DATA);
        if (actuel != null && !actuel.strings().isEmpty() && texture.equals(actuel.strings().get(0))) {
            return objet;
        }
        List<String> chaines = new ArrayList<>();
        chaines.add(texture);
        ItemStack copie = objet.copy();
        if (actuel == null) {
            copie.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), chaines, List.of()));
        } else {
            chaines.addAll(actuel.strings());
            copie.set(DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(actuel.floats(), actuel.flags(), chaines, actuel.colors()));
        }
        return copie;
    }

    /**
     * L'inverse de {@link #marquer}, pour un objet renvoyé par un joueur en créatif : sans cela, la marque d'affichage
     * serait enregistrée dans l'objet du serveur (et il ne s'empilerait plus avec les autres).
     */
    static ItemStack demarquer(ItemStack objet) {
        if (objet == null || objet.isEmpty()) {
            return objet;
        }
        CustomModelData actuel = objet.get(DataComponents.CUSTOM_MODEL_DATA);
        if (actuel == null || actuel.strings().isEmpty() || !TEXTURES.contains(actuel.strings().get(0))
                || !actuel.strings().get(0).equals(texture(objet))) {
            return objet;
        }
        ItemStack copie = objet.copy();
        List<String> reste = actuel.strings().subList(1, actuel.strings().size());
        if (reste.isEmpty() && actuel.floats().isEmpty() && actuel.flags().isEmpty() && actuel.colors().isEmpty()) {
            copie.remove(DataComponents.CUSTOM_MODEL_DATA);
        } else {
            copie.set(DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(actuel.floats(), actuel.flags(), List.copyOf(reste), actuel.colors()));
        }
        return copie;
    }
}
