package fr.kalium.atouts;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Les atouts et leur pose sur un équipement (cahier, partie 3.5).
 *
 * Pendant l'effet de l'atout (1.3.0 : seulement pendant l'effet, voir KSAtouts), l'objet change vraiment de type
 * (bottes en cuir, tête de gardien, bottes en cuivre, bloc de miel, plastron en or) en gardant ses caractéristiques : le type d'origine, ses modificateurs d'attributs (armure, robustesse, résistance au
 * recul), sa durabilité, sa réparation, sa résistance au feu (netherite), son enchantabilité, sa pile de 1 et son nom
 * de base sont recopiés comme valeurs explicites ; son apparence portée vient du nouveau type. Les enchantements, le nom
 * donné, la garniture et l'usure suivent d'eux-mêmes. Fin de l'effet : il reprend son type, et les valeurs recopiées
 * qui venaient de son type d'origine sont retirées (l'usure reste).
 *
 * Marques sur l'objet : atout, origine (type d'origine), restant (secondes), ajouts (composants recopiés à retirer,
 * seulement pendant l'effet), lignes (nombre de lignes de description d'origine).
 */
final class Atouts {

    /**
     * Un atout : nom, ressource, emplacement, apparence, secondes ajoutées par craft, décompte seulement dans le Nether
     * (1.2.0).
     */
    record Atout(String id, String nom, Material ressource, EquipmentSlot emplacement, Material apparence, int parCraft,
                 boolean nether) {
    }

    static final String SKI = "ski";
    static final String POISCAILLE = "poiscaille";
    static final String COUPE_FEU = "coupe_feu";
    static final String VENTOUSE = "ventouse";
    static final String ANTI_RADIATION = "anti_radiation";

    /** Une heure par craft (choix de LeKiwi06), cumulable sans plafond. */
    static final int HEURE = 3600;
    /** 1.2.0 : plastron anti-radiation, un jour du Nether (20 minutes) par craft (LeKiwi06, 30/10). */
    static final int JOUR_NETHER = 1200;

    static final Map<String, Atout> ATOUTS = new LinkedHashMap<>();

    static {
        ATOUTS.put(SKI, new Atout(SKI, "Bottes de ski", Material.RABBIT_FOOT, EquipmentSlot.FEET, Material.LEATHER_BOOTS,
                HEURE, false));
        ATOUTS.put(POISCAILLE, new Atout(POISCAILLE, "Casque poiscaille", Material.PUFFERFISH, EquipmentSlot.HEAD,
                Material.PLAYER_HEAD, HEURE, false));
        ATOUTS.put(COUPE_FEU, new Atout(COUPE_FEU, "Bottes coupe-feu", Material.MAGMA_CREAM, EquipmentSlot.FEET,
                Material.COPPER_BOOTS, HEURE, false));
        // Un bloc de miel porté s'affiche sur la tête (pas d'apparence d'armure : le jeu dessine l'objet).
        ATOUTS.put(VENTOUSE, new Atout(VENTOUSE, "Casque ventouse", Material.HONEY_BLOCK, EquipmentSlot.HEAD,
                Material.HONEY_BLOCK, HEURE, false));
        // 1.2.0 : un plastron en or (les piglins ne l'attaquent donc pas : voulu par LeKiwi06, 30/10).
        ATOUTS.put(ANTI_RADIATION, new Atout(ANTI_RADIATION, "Plastron anti-radiation", Material.GOLD_BLOCK,
                EquipmentSlot.CHEST, Material.GOLDEN_CHESTPLATE, JOUR_NETHER, true));
    }

    /** 1.3.0 : types que prend un objet pendant l'effet de son atout (test rapide avant de lire ses marques). */
    private static final Set<Material> APPARENCES = EnumSet.noneOf(Material.class);

    static {
        ATOUTS.values().forEach(a -> APPARENCES.add(a.apparence()));
    }

    /** Texture de la tête de gardien (table de KS_Decapitator). */
    private static final String GARDIEN = "e2ba34416670454b1a20496f80b9398529f49003fc613eb930248ea9b5d1a391";

    /** Valeurs recopiées du type d'origine. */
    private static final List<DataComponentType.Valued<?>> COPIES = List.of(DataComponentTypes.ATTRIBUTE_MODIFIERS,
            DataComponentTypes.MAX_DAMAGE, DataComponentTypes.REPAIRABLE, DataComponentTypes.DAMAGE_RESISTANT,
            DataComponentTypes.ENCHANTABLE, DataComponentTypes.MAX_STACK_SIZE, DataComponentTypes.ITEM_NAME,
            DataComponentTypes.RARITY);

    private static NamespacedKey cleAtout;
    private static NamespacedKey cleOrigine;
    private static NamespacedKey cleRestant;
    private static NamespacedKey cleAjouts;
    private static NamespacedKey cleLignes;

    private Atouts() {
    }

    static void init(KSAtouts plugin) {
        cleAtout = new NamespacedKey(plugin, "atout");
        cleOrigine = new NamespacedKey(plugin, "origine");
        cleRestant = new NamespacedKey(plugin, "restant");
        cleAjouts = new NamespacedKey(plugin, "ajouts");
        cleLignes = new NamespacedKey(plugin, "lignes");
    }

    // ------------------------------------------------------------------ lecture

    /** Atout de l'objet, ou null. */
    static Atout atout(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(cleAtout, PersistentDataType.STRING);
        return id == null ? null : ATOUTS.get(id);
    }

    /** Secondes restantes de l'atout de l'objet (0 sans atout). */
    static int restant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer s = item.getItemMeta().getPersistentDataContainer().get(cleRestant, PersistentDataType.INTEGER);
        return s == null ? 0 : s;
    }

    /** Type d'origine d'un objet sous atout (son type actuel sinon). */
    static Material origine(ItemStack item) {
        if (atout(item) == null) {
            return item.getType();
        }
        String nom = item.getItemMeta().getPersistentDataContainer().get(cleOrigine, PersistentDataType.STRING);
        Material m = nom == null ? null : Material.matchMaterial(nom);
        return m == null ? item.getType() : m;
    }

    /** L'équipement accepte-t-il cet atout (son type d'origine est-il une pièce de cet emplacement) ? */
    static boolean accepte(ItemStack item, Atout a) {
        if (item == null || item.isEmpty() || item.getAmount() != 1) {
            return false;
        }
        Material m = origine(item);
        return switch (a.emplacement()) {
            case FEET -> org.bukkit.Tag.ITEMS_FOOT_ARMOR.isTagged(m);
            case CHEST -> org.bukkit.Tag.ITEMS_CHEST_ARMOR.isTagged(m);
            default -> org.bukkit.Tag.ITEMS_HEAD_ARMOR.isTagged(m);
        };
    }

    // ------------------------------------------------------------------ pose

    /**
     * L'équipement avec cet atout : même atout déjà posé, une heure de plus (plastron : 20 minutes) ; autre atout, il est
     * perdu (et son temps). 1.3.0 : l'objet garde son type ; il ne prend l'apparence de l'atout que pendant l'effet
     * ({@link #activer}).
     */
    static ItemStack poser(ItemStack equipement, Atout a) {
        int restant = a.parCraft();
        ItemStack r = equipement.clone();
        Atout actuel = atout(r);
        if (actuel != null) {
            if (actuel == a) {
                restant += restant(r);
            }
            r = retirer(r);
        }
        ItemMeta meta = r.getItemMeta();
        List<Component> lignes = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(cleAtout, PersistentDataType.STRING, a.id());
        pdc.set(cleOrigine, PersistentDataType.STRING, r.getType().getKey().toString());
        pdc.set(cleRestant, PersistentDataType.INTEGER, restant);
        pdc.set(cleLignes, PersistentDataType.INTEGER, lignes.size());
        lignes.add(ligne(a, restant));
        meta.lore(lignes);
        r.setItemMeta(meta);
        return r;
    }

    /** 1.3.0 : l'objet a-t-il en ce moment l'apparence de son atout (type changé) ? */
    static boolean actif(ItemStack item) {
        if (item == null || !APPARENCES.contains(item.getType())) {
            return false;
        }
        return atout(item) != null && item.getType() != origine(item);
    }

    /**
     * 1.3.0 : l'objet sous atout avec l'apparence de son atout (vrai type), en gardant ses caractéristiques. Déjà de ce
     * type (bottes en cuir de ski...), il est rendu tel quel.
     */
    static ItemStack activer(ItemStack base) {
        Atout a = atout(base);
        if (a == null || base.getType() == a.apparence()) {
            return base;
        }
        ItemStack r = base.withType(a.apparence());
        Equippable original = base.getData(DataComponentTypes.EQUIPPABLE);
        // Composants recopiés qui venaient du type d'origine : retirés quand l'effet s'arrête.
        List<String> ajouts = new ArrayList<>();
        for (DataComponentType.Valued<?> type : COPIES) {
            if ((base.hasData(type) || r.hasData(type)) && !base.isDataOverridden(type)) {
                ajouts.add(type.key().asString());
            }
        }
        if (original != null && !base.isDataOverridden(DataComponentTypes.EQUIPPABLE)) {
            ajouts.add(DataComponentTypes.EQUIPPABLE.key().asString());
        }
        boolean tete = r.getType() == Material.PLAYER_HEAD;
        if (tete) {
            ajouts.add(DataComponentTypes.PROFILE.key().asString());
        }
        // D'abord la marque (objet relu), puis la tête et les valeurs recopiées.
        ItemMeta meta = r.getItemMeta();
        meta.getPersistentDataContainer().set(cleAjouts, PersistentDataType.LIST.strings(), ajouts);
        r.setItemMeta(meta);
        if (tete) {
            // 1.3.1 : profil SANS nom. Avec un nom (même vide, ce que donnait Bukkit.createProfile), le jeu affiche
            // « <nom>'s Head » à la place du nom de l'équipement.
            String textures = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + GARDIEN + "\"}}}";
            r.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile()
                    .uuid(UUID.nameUUIDFromBytes("ks_atouts:gardien".getBytes(StandardCharsets.UTF_8)))
                    .addProperty(new ProfileProperty("textures",
                            Base64.getEncoder().encodeToString(textures.getBytes(StandardCharsets.UTF_8))))
                    .build());
        }
        for (DataComponentType.Valued<?> type : COPIES) {
            copier(base, r, type);
        }
        // Usure : gardée telle quelle (et quand l'effet s'arrête).
        Integer usure = base.getData(DataComponentTypes.DAMAGE);
        if (usure != null) {
            r.setData(DataComponentTypes.DAMAGE, usure);
        }
        // Porté : réglages de l'équipement d'origine (son, usure quand on est touché), apparence du nouveau type.
        Equippable visuel = new ItemStack(a.apparence()).getData(DataComponentTypes.EQUIPPABLE);
        if (original != null) {
            r.setData(DataComponentTypes.EQUIPPABLE, original.toBuilder()
                    .assetId(visuel == null ? null : visuel.assetId()).build());
        }
        return r;
    }

    /** Recopie une valeur (effective : explicite ou du type) ; absente de l'origine, elle est retirée. */
    private static <T> void copier(ItemStack de, ItemStack vers, DataComponentType.Valued<T> type) {
        T valeur = de.getData(type);
        if (valeur != null) {
            vers.setData(type, valeur);
        } else if (vers.hasData(type)) {
            vers.unsetData(type);
        }
    }

    /**
     * 1.3.0 : l'objet sous atout avec son aspect d'origine (type d'origine, valeurs recopiées retirées) ; l'atout, son
     * temps et sa ligne de description restent.
     */
    static ItemStack desactiver(ItemStack item) {
        if (!actif(item)) {
            return item;
        }
        List<String> ajouts = item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(cleAjouts, PersistentDataType.LIST.strings(), List.of());
        ItemStack r = item.withType(origine(item));
        for (String cle : ajouts) {
            NamespacedKey k = NamespacedKey.fromString(cle);
            DataComponentType type = k == null ? null : Registry.DATA_COMPONENT_TYPE.get(k);
            if (type != null) {
                r.resetData(type);
            }
        }
        ItemMeta meta = r.getItemMeta();
        meta.getPersistentDataContainer().remove(cleAjouts);
        r.setItemMeta(meta);
        return r;
    }

    /** L'objet sans atout : aspect d'origine, description d'origine, marques retirées. */
    static ItemStack retirer(ItemStack item) {
        if (atout(item) == null) {
            return item;
        }
        ItemStack r = desactiver(item);
        ItemMeta meta = r.getItemMeta();
        PersistentDataContainer marques = meta.getPersistentDataContainer();
        int nombre = marques.getOrDefault(cleLignes, PersistentDataType.INTEGER, 0);
        List<Component> lignes = meta.hasLore() ? meta.lore() : List.of();
        meta.lore(nombre == 0 || lignes == null ? null : new ArrayList<>(lignes.subList(0, Math.min(nombre, lignes.size()))));
        for (NamespacedKey k : List.of(cleAtout, cleOrigine, cleRestant, cleAjouts, cleLignes)) {
            marques.remove(k);
        }
        r.setItemMeta(meta);
        return r;
    }

    /** Retire des secondes à l'atout de l'objet (modifié sur place) ; vrai s'il reste du temps. */
    static boolean decompter(ItemStack item, int secondes) {
        Atout a = atout(item);
        if (a == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int reste = pdc.getOrDefault(cleRestant, PersistentDataType.INTEGER, 0) - secondes;
        if (reste <= 0) {
            return false;
        }
        pdc.set(cleRestant, PersistentDataType.INTEGER, reste);
        int nombre = pdc.getOrDefault(cleLignes, PersistentDataType.INTEGER, 0);
        List<Component> lignes = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        while (lignes.size() > nombre) {
            lignes.remove(lignes.size() - 1);
        }
        lignes.add(ligne(a, reste));
        meta.lore(lignes);
        item.setItemMeta(meta);
        return true;
    }

    private static Component ligne(Atout a, int secondes) {
        return Component.text("Atout : " + a.nom() + " (" + duree(secondes) + (a.nether() ? " dans le Nether" : "") + ")",
                        NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false);
    }

    /** « 1 h 05 min », « 12 min », « moins d'une minute ». */
    static String duree(int secondes) {
        int minutes = secondes / 60;
        if (minutes == 0) {
            return "moins d'une minute";
        }
        return minutes >= 60 ? minutes / 60 + " h " + String.format("%02d", minutes % 60) + " min" : minutes + " min";
    }
}
