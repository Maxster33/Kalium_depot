package fr.kalium.textures;

import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.1.0 (LeKiwi06, 29/10/2026 : « j'aimerai que nos items ait une couleur dans leurs noms pour signifier la rareté
 * comme les items de base du jeu ») : le nom d'un objet custom prend la couleur de sa rareté, seulement dans ce qui est
 * envoyé aux joueurs (comme les textures).
 *
 * Choix de LeKiwi06 : les 4 raretés du jeu et une 5e, Légendaire en doré ; la rareté vient du prix de l'objet
 * (seuils ci-dessous) ; jamais moins rare que l'objet vanilla (plancher : rareté que le jeu donne à l'objet, ex.
 * fiole d'expérience et têtes en jaune) ; la couleur propre des alcools, élixirs et aliments est remplacée.
 * Tableau validé : sortie/rarete/raretes-proposition.xlsx (hors du dépôt).
 *
 * Prix : le barème de KS_Economy (rachats.csv, copié dans le jar à la compilation : recompiler après un changement de
 * prix), sinon les valeurs ci-dessous (récompenses, ou recette estimée). Les atouts (équipement du joueur, nom donné
 * par le joueur) ne sont pas recolorés.
 */
final class Raretes {

    /** Couleur de chaque rareté ; les 4 premières dans l'ordre de la rareté du jeu (net.minecraft.world.item.Rarity). */
    private static final TextColor[] COULEURS = {TextColor.WHITE, TextColor.YELLOW, TextColor.AQUA,
            TextColor.LIGHT_PURPLE, TextColor.GOLD};
    /** Prix (émeraudes) à partir duquel commencent Peu commun, Rare, Épique, Légendaire (LeKiwi06, 29/10/2026). */
    private static final double[] SEUILS = {10, 100, 1_000, 10_000};

    private static final String PDC = "PublicBukkitValues";
    /** Nom d'origine, rangé dans l'objet envoyé pour le lui rendre s'il revient d'un joueur en créatif. */
    private static final String NOM_ORIGINE = "ks_textures:nom";

    private record Cible(String valeur, double prix) {
    }

    /** Objets custom du barème, par clé de leur marque (valeur null : la marque suffit). */
    private static final Map<String, List<Cible>> BAREME = new HashMap<>();
    /** Prix du barème par identifiant (jetons, blocs d'émeraude compressés, fiole de 15 niveaux). */
    private static final Map<String, Double> PAR_ID = new HashMap<>();
    /** Objets absents du barème : clé de la marque, ou clé=valeur. */
    private static final Map<String, Double> AUTRES = new HashMap<>();
    /** Badge de niveau 1, valeur des récompenses ; chaque niveau vaut 2 badges du niveau précédent. */
    private static final Map<String, Double> BADGES = Map.of("fly", 450.0, "tp", 600.0, "localisation", 750.0,
            "mort", 3_600.0);

    static {
        AUTRES.put("ks_jetons:jeton=fly", 30.0); // valeur des récompenses
        AUTRES.put("ks_jetons:jeton=localisation", 1.58); // 8 blocs de cuivre + boussole
        AUTRES.put("ks_meule:cristal", 600.0); // valeur des récompenses
        AUTRES.put("ks_coffrerenforce:coffre", 516.0); // coffre + 4 blocs de fer + 4 fragments de spawner (+ véhicule)
        // Recettes estimées, toutes communes
        AUTRES.put("ks_alcools:alcool", 0.22); // 0,07 à 0,22
        AUTRES.put("ks_alcools:molotov", 0.09);
        AUTRES.put("ks_alcools:bandage", 0.20);
        AUTRES.put("ks_alcools:torche_augmentee", 0.03);
        AUTRES.put("ks_cuisine:aliment", 0.44); // 0,05 à 0,44, pomme d'amour comprise
        AUTRES.put("ks_cassecroute:casse_croute", 4.41);
        AUTRES.put("ks_trains:locomotive", 1.74);
        AUTRES.put("ks_trains:controleur-objet", 1.77);
        AUTRES.put("ks_trains:jukebox-objet", 4.54);
        AUTRES.put("ks_trains:boussole-train", 0.50);
        AUTRES.put("ks_verrouvehicule:verrou", 0.45);
        AUTRES.put("ks_jukeboxboat:bateau", 4.32);
        AUTRES.put("ks_crafts:bedrock_breaker", 7.008); // ancien marqueur, même prix que ks_bedrockbreaker
    }

    private Raretes() {
    }

    /**
     * Lit le barème (id ; famille ; prix ; cible ; ...) : une cible {@code pdc:<espace>:<clé>:<s|b|i>[:<valeur>]}
     * est un objet custom.
     */
    static int charger(InputStream flux) throws IOException {
        int n = 0;
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux, StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                if (ligne.startsWith("#") || ligne.isBlank()) {
                    continue;
                }
                String[] c = ligne.split(";", -1);
                double prix;
                try {
                    prix = Double.parseDouble(c[2]);
                } catch (RuntimeException e) {
                    continue;
                }
                PAR_ID.put(c[0], prix);
                String[] cible = c[3].split(":", 5);
                if (cible.length >= 4 && cible[0].equals("pdc")) {
                    BAREME.computeIfAbsent(cible[1] + ":" + cible[2], k -> new ArrayList<>())
                            .add(new Cible(cible.length == 5 ? cible[4] : null, prix));
                    n++;
                }
            }
        }
        return n;
    }

    /** Le prix de l'objet custom d'après ses marques, ou null si ce n'est pas un objet custom connu. */
    private static Double prix(CompoundTag marques) {
        for (String cle : List.of("ks_fioleexp:points", "ks_coffremort:points")) {
            Integer points = entier(marques.get(cle));
            Double fiole = PAR_ID.get("fiole_exp(15)");
            if (points != null && fiole != null) {
                return points * fiole / 315; // 315 points dans une fiole de 15 niveaux
            }
        }
        String badge = texte(marques.get("ks_jetons:badge"));
        if (badge != null && BADGES.containsKey(badge)) {
            Integer niveau = entier(marques.get("ks_jetons:niveau"));
            return BADGES.get(badge) * Math.pow(2, Math.max(1, niveau == null ? 1 : niveau) - 1);
        }
        String jeton = texte(marques.get("ks_jetons:jeton"));
        if (jeton != null && PAR_ID.containsKey("jeton_" + jeton)) {
            return PAR_ID.get("jeton_" + jeton);
        }
        String tier = texte(marques.get("ks_economy:tier"));
        if (tier != null && PAR_ID.containsKey("bloc_emeraude_compresse_tier_" + tier)) {
            return PAR_ID.get("bloc_emeraude_compresse_tier_" + tier);
        }
        for (String cle : marques.keySet()) {
            List<Cible> cibles = BAREME.get(cle);
            if (cibles == null) {
                continue;
            }
            String valeur = texte(marques.get(cle));
            for (Cible c : cibles) {
                if (c.valeur == null || c.valeur.equals(valeur)) {
                    return c.prix;
                }
            }
        }
        for (String cle : marques.keySet()) {
            Double prix = AUTRES.get(cle + "=" + texte(marques.get(cle)));
            if (prix == null) {
                prix = AUTRES.get(cle);
            }
            if (prix != null) {
                return prix;
            }
        }
        return null;
    }

    /** Rang de la rareté (0 Commun à 4 Légendaire), ou -1 si l'objet n'est pas un objet custom connu. */
    static int rang(ItemStack objet) {
        if (objet == null || objet.isEmpty()) {
            return -1;
        }
        CustomData donnees = objet.get(DataComponents.CUSTOM_DATA);
        if (donnees == null || !(donnees.getUnsafe().get(PDC) instanceof CompoundTag marques)
                || marques.contains("ks_atouts:atout")) {
            return -1;
        }
        Double prix = prix(marques);
        if (prix == null) {
            return -1;
        }
        int rang = 0;
        while (rang < SEUILS.length && prix >= SEUILS[rang]) {
            rang++;
        }
        // Plancher : la rareté que le jeu donne à l'objet (objet de base, enchantements compris)
        return Math.max(rang, objet.getRarity().ordinal());
    }

    /**
     * L'objet avec son nom à la couleur de sa rareté (une copie si {@code copie} est faux, sinon l'objet reçu, déjà une
     * copie), ou l'objet reçu s'il n'y a rien à changer. Le nom d'origine est rangé dans l'objet envoyé.
     */
    static ItemStack colorer(ItemStack objet, boolean copie) {
        Component nom = objet == null ? null : objet.get(DataComponents.CUSTOM_NAME);
        if (nom == null) {
            return objet;
        }
        int rang = rang(objet);
        if (rang < 0) {
            return objet;
        }
        Component colore = recolorer(nom, COULEURS[rang]);
        if (colore.equals(nom)) {
            return objet;
        }
        DataResult<Tag> origine = ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, nom);
        if (origine.result().isEmpty()) {
            return objet;
        }
        ItemStack resultat = copie ? objet : objet.copy();
        resultat.set(DataComponents.CUSTOM_NAME, colore);
        CustomData.update(DataComponents.CUSTOM_DATA, resultat, t -> t.put(NOM_ORIGINE, origine.result().get()));
        return resultat;
    }

    /** L'inverse de {@link #colorer}, pour un objet renvoyé par un joueur en créatif. */
    static ItemStack decolorer(ItemStack objet, boolean copie) {
        CustomData donnees = objet == null || objet.isEmpty() ? null : objet.get(DataComponents.CUSTOM_DATA);
        Tag origine = donnees == null ? null : donnees.getUnsafe().get(NOM_ORIGINE);
        if (origine == null) {
            return objet;
        }
        ItemStack resultat = copie ? objet : objet.copy();
        ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, origine).result()
                .ifPresent(nom -> resultat.set(DataComponents.CUSTOM_NAME, nom));
        CustomData.update(DataComponents.CUSTOM_DATA, resultat, t -> t.remove(NOM_ORIGINE));
        return resultat;
    }

    /** Le texte à la couleur donnée : la couleur des parties du texte est retirée, elles prennent celle du début. */
    private static Component recolorer(Component texte, TextColor couleur) {
        MutableComponent resultat = texte.plainCopy().setStyle(texte.getStyle().withColor(couleur));
        for (Component partie : texte.getSiblings()) {
            resultat.append(recolorer(partie, null));
        }
        return resultat;
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

    private static Integer entier(Tag valeur) {
        return valeur instanceof NumericTag n ? n.intValue() : null;
    }
}
