package fr.kalium.atouts;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les atouts et leur pose sur un équipement (cahier, partie 3.5).
 *
 * Pendant l'atout, l'objet change vraiment de type (bottes en cuir, tête de gardien, bottes en cuivre) en gardant ses
 * caractéristiques : le type d'origine, ses modificateurs d'attributs (armure, robustesse, résistance au recul),
 * sa durabilité, sa réparation, sa résistance au feu (netherite), son enchantabilité, sa pile de 1 et son nom de
 * base sont recopiés comme valeurs explicites ; son apparence portée vient du nouveau type. Les enchantements, le nom
 * donné, la garniture et l'usure suivent d'eux-mêmes. Fin de l'atout : il reprend son type, et les valeurs recopiées
 * qui venaient de son type d'origine sont retirées (l'usure reste).
 *
 * Marques sur l'objet : atout, origine (type d'origine), restant (secondes), ajouts (composants recopiés à retirer),
 * lignes (nombre de lignes de description d'origine).
 */
final class Atouts {

    /** Un atout : nom, ressource, emplacement, apparence. */
    record Atout(String id, String nom, Material ressource, EquipmentSlot emplacement, Material apparence) {
    }

    static final String SKI = "ski";
    static final String POISCAILLE = "poiscaille";
    static final String COUPE_FEU = "coupe_feu";

    static final Map<String, Atout> ATOUTS = new LinkedHashMap<>();

    static {
        ATOUTS.put(SKI, new Atout(SKI, "Bottes de ski", Material.RABBIT_FOOT, EquipmentSlot.FEET, Material.LEATHER_BOOTS));
        ATOUTS.put(POISCAILLE, new Atout(POISCAILLE, "Casque poiscaille", Material.PUFFERFISH, EquipmentSlot.HEAD,
                Material.PLAYER_HEAD));
        ATOUTS.put(COUPE_FEU, new Atout(COUPE_FEU, "Bottes coupe-feu", Material.MAGMA_CREAM, EquipmentSlot.FEET,
                Material.COPPER_BOOTS));
    }

    /** Une heure par craft (choix de LeKiwi06), cumulable sans plafond. */
    static final int HEURE = 3600;

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
        return a.emplacement() == EquipmentSlot.FEET ? org.bukkit.Tag.ITEMS_FOOT_ARMOR.isTagged(m)
                : org.bukkit.Tag.ITEMS_HEAD_ARMOR.isTagged(m);
    }

    // ------------------------------------------------------------------ pose

    /**
     * L'équipement avec cet atout : même atout déjà posé, une heure de plus ; autre atout, il est perdu (et son temps).
     */
    static ItemStack poser(ItemStack equipement, Atout a) {
        int restant = HEURE;
        ItemStack base = equipement.clone();
        Atout actuel = atout(base);
        if (actuel != null) {
            if (actuel == a) {
                restant += restant(base);
            }
            base = retirer(base);
        }
        ItemStack r = base.withType(a.apparence());
        Equippable original = base.getData(DataComponentTypes.EQUIPPABLE);
        // Composants recopiés qui venaient du type d'origine : retirés à la fin de l'atout.
        List<String> ajouts = new ArrayList<>();
        for (DataComponentType.Valued<?> type : COPIES) {
            if ((base.hasData(type) || r.hasData(type)) && !base.isDataOverridden(type)) {
                ajouts.add(type.key().asString());
            }
        }
        if (original != null && !base.isDataOverridden(DataComponentTypes.EQUIPPABLE)) {
            ajouts.add(DataComponentTypes.EQUIPPABLE.key().asString());
        }
        // D'abord la description, les marques et la tête (objet relu), puis les valeurs recopiées.
        ItemMeta meta = r.getItemMeta();
        if (meta instanceof SkullMeta crane) {
            PlayerProfile profil = Bukkit.createProfile(UUID.nameUUIDFromBytes("ks_atouts:gardien".getBytes(
                    StandardCharsets.UTF_8)), null);
            String textures = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + GARDIEN + "\"}}}";
            profil.setProperty(new ProfileProperty("textures",
                    Base64.getEncoder().encodeToString(textures.getBytes(StandardCharsets.UTF_8))));
            crane.setPlayerProfile(profil);
            ajouts.add(DataComponentTypes.PROFILE.key().asString());
        }
        List<Component> lignes = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(cleAtout, PersistentDataType.STRING, a.id());
        pdc.set(cleOrigine, PersistentDataType.STRING, base.getType().getKey().toString());
        pdc.set(cleRestant, PersistentDataType.INTEGER, restant);
        pdc.set(cleAjouts, PersistentDataType.LIST.strings(), ajouts);
        pdc.set(cleLignes, PersistentDataType.INTEGER, lignes.size());
        lignes.add(ligne(a, restant));
        meta.lore(lignes);
        r.setItemMeta(meta);
        for (DataComponentType.Valued<?> type : COPIES) {
            copier(base, r, type);
        }
        // Usure : gardée telle quelle (et à la fin de l'atout).
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

    /** L'objet sans atout : type d'origine, valeurs recopiées retirées, description d'origine. */
    static ItemStack retirer(ItemStack item) {
        if (atout(item) == null) {
            return item;
        }
        ItemMeta ancienne = item.getItemMeta();
        PersistentDataContainer pdc = ancienne.getPersistentDataContainer();
        List<String> ajouts = pdc.getOrDefault(cleAjouts, PersistentDataType.LIST.strings(), List.of());
        int nombre = pdc.getOrDefault(cleLignes, PersistentDataType.INTEGER, 0);
        ItemStack r = item.withType(origine(item));
        for (String cle : ajouts) {
            NamespacedKey k = NamespacedKey.fromString(cle);
            DataComponentType type = k == null ? null : Registry.DATA_COMPONENT_TYPE.get(k);
            if (type != null) {
                r.resetData(type);
            }
        }
        ItemMeta meta = r.getItemMeta();
        List<Component> lignes = meta.hasLore() ? meta.lore() : List.of();
        meta.lore(nombre == 0 || lignes == null ? null : new ArrayList<>(lignes.subList(0, Math.min(nombre, lignes.size()))));
        PersistentDataContainer marques = meta.getPersistentDataContainer();
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
        return Component.text("Atout : " + a.nom() + " (" + duree(secondes) + ")", NamedTextColor.AQUA)
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
