package fr.kalium.cultures;

import java.util.List;
import java.util.Locale;

/**
 * Les nouveaux végétaux (cahier local Blocs_Cultures_Objets.md, parties 12 et 14, prix et récoltes validés le 02/11).
 *
 * Stades (variantes de l'apparence ks_cultures_terre ou ks_cultures_eau de KS_Blocs) : « pousse », « jeune », puis la
 * variante mûre. Maïs : deux blocs de haut une fois mûr (mais_bas, mais_haut). Cerises et kiwi : arbuste sans fruits,
 * puis avec fruits ; la récolte (clic droit) le ramène sans fruits.
 */
enum Vegetal {

    TOMATE(Lieu.TERRE, "tomate", 1, 3, 1, 2, "Tomate", "Graines de tomate", "beetroot", "wheat_seeds"),
    POIVRON(Lieu.TERRE, "poivron", 1, 3, 1, 2, "Poivron", "Graines de poivron", "carrot", "wheat_seeds"),
    AUBERGINE(Lieu.TERRE, "aubergine", 1, 3, 1, 2, "Aubergine", "Graines d'aubergine", "chorus_fruit", "wheat_seeds"),
    OIGNON(Lieu.TERRE, "oignon", 1, 3, 1, 2, "Oignon", "Graines d'oignon", "potato", "wheat_seeds"),
    SALADE(Lieu.TERRE, "salade", 1, 3, 1, 2, "Salade", "Graines de salade", "dried_kelp", "wheat_seeds"),
    MAIS(Lieu.TERRE, "mais_bas", 1, 2, 1, 2, "Épi de maïs", "Graines de maïs", "wheat", "melon_seeds"),
    /** Le grain se replante (dans l'eau) : pas d'objet graine séparé. */
    RIZ(Lieu.EAU, "riz", 1, 4, 0, 0, "Riz", null, "pumpkin_seeds", null),
    /** Le fruit se replante, comme les baies sucrées. */
    CERISES(Lieu.ARBUSTE, "cerisier_fruits", 1, 2, 0, 0, "Cerises", null, "sweet_berries", null),
    KIWI(Lieu.ARBUSTE, "kiwi_fruits", 1, 2, 0, 0, "Kiwi", null, "glow_berries", null);

    /** Où il se plante : terre labourée, sous l'eau (riz), terre ou terre labourée (arbustes). */
    enum Lieu { TERRE, EAU, ARBUSTE }

    final Lieu lieu;
    /** Variante mûre (pour un arbuste : avec fruits). */
    final String mur;
    final int fruitsMin, fruitsMax, grainesMin, grainesMax;
    final String nomFruit, nomGraine;
    /** Image provisoire (item_model du jeu) en attendant les textures de LeKiwi06 (règles KS_Textures). */
    final String modeleFruit, modeleGraine;

    Vegetal(Lieu lieu, String mur, int fruitsMin, int fruitsMax, int grainesMin, int grainesMax, String nomFruit,
            String nomGraine, String modeleFruit, String modeleGraine) {
        this.lieu = lieu;
        this.mur = mur;
        this.fruitsMin = fruitsMin;
        this.fruitsMax = fruitsMax;
        this.grainesMin = grainesMin;
        this.grainesMax = grainesMax;
        this.nomFruit = nomFruit;
        this.nomGraine = nomGraine;
        this.modeleFruit = modeleFruit;
        this.modeleGraine = modeleGraine;
    }

    /** Nom court (config.yml, objets, liste des découvertes) : tomate, mais, cerises... */
    String cle() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Id du type de bloc custom (KS_Blocs). */
    String type() {
        return "ks_cultures:" + cle();
    }

    /** Id de l'objet fruit (ks_cultures:objet). */
    String fruit() {
        return cle();
    }

    /** Id de l'objet graine, ou null si le fruit se replante lui-même. */
    String graine() {
        return nomGraine == null ? null : "graine_" + cle();
    }

    /** L'objet qui se plante : la graine, ou le fruit pour le riz et les arbustes. */
    String semence() {
        return graine() != null ? graine() : fruit();
    }

    /** Variante sans fruits d'un arbuste. */
    String arbuste() {
        return lieu == Lieu.ARBUSTE ? mur.replace("_fruits", "") : null;
    }

    /** Vrai si le composteur peut en tirer des graines une fois le végétal découvert (1 fruit -> 1 graine). */
    boolean aDesGraines() {
        return graine() != null;
    }

    /** Végétaux qu'un mélange de graines peut faire pousser (sur la terre labourée : tous sauf le riz). */
    static List<Vegetal> duMelange() {
        return List.of(TOMATE, POIVRON, AUBERGINE, OIGNON, SALADE, MAIS, CERISES, KIWI);
    }

    static Vegetal parCle(String cle) {
        for (Vegetal v : values()) {
            if (v.cle().equals(cle)) {
                return v;
            }
        }
        return null;
    }

    static Vegetal parType(String type) {
        return type != null && type.startsWith("ks_cultures:") ? parCle(type.substring("ks_cultures:".length())) : null;
    }

    static Vegetal parFruit(String id) {
        return parCle(id);
    }

    static Vegetal parGraine(String id) {
        return id != null && id.startsWith("graine_") ? parCle(id.substring("graine_".length())) : null;
    }

    /** Le végétal que plante cet objet (graine, grain de riz, cerises, kiwi), ou null. */
    static Vegetal parSemence(String id) {
        for (Vegetal v : values()) {
            if (v.semence().equals(id)) {
                return v;
            }
        }
        return null;
    }
}
