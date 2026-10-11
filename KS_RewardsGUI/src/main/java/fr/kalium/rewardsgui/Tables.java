package fr.kalium.rewardsgui;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1.5.0 (LeKiwi06, 11/10/2026 : fusion de deux coffres de récompense, « Nouveau tirage ») : tables de butin de ce
 * serveur, pour tirer le contenu d'un coffre fusionné. Le contenu des autres coffres est tiré par le serveur qui donne la
 * récompense (KG_Rewards, KV_Rewards) ; ce plugin n'en reçoit que le résultat.
 *
 * Fichier plugins/KS_RewardsGUI/fusion.yml (produit avec les butin.yml de KG_Rewards et KV_Rewards ; absent : pas de
 * fusion) :
 * - « ordre » : les pools, dans l'ordre des fréquences ;
 * - « pools » : même format que butin.yml (poids, contenu : {type: objet, id, nombre} avec au besoin « potion »,
 *   « enchanter: [min, max] », « enchantements: {clé: niveau} » ; {type: custom, id, nombre}) ;
 * - « reglages » : lignes triées par valeur. Une ligne vaut de sa valeur à celle de la ligne suivante : nombre de
 *   tirages, fréquences des pools au début (« debut ») et à la fin (« fin ») de cet intervalle ; entre les deux, les
 *   fréquences sont interpolées. Au-delà de la dernière ligne : ses fréquences, et un nombre de tirages proportionnel à
 *   la valeur.
 */
final class Tables {

    private record Entree(int poids, List<Map<String, Object>> contenu) {
    }

    private record Reglage(double valeur, int tirages, double[] debut, double[] fin) {
    }

    private final KSRewardsGUI plugin;
    private final List<String> ordre = new ArrayList<>();
    private final Map<String, List<Entree>> pools = new LinkedHashMap<>();
    private final List<Reglage> reglages = new ArrayList<>();

    Tables(KSRewardsGUI plugin) {
        this.plugin = plugin;
        charger(new File(plugin.getDataFolder(), "fusion.yml"));
    }

    /** Faux tant que fusion.yml manque ou ne contient rien d'utilisable. */
    boolean disponibles() {
        return !reglages.isEmpty() && pools.values().stream().anyMatch(entrees -> !entrees.isEmpty());
    }

    // ------------------------------------------------------------------ tirage

    /** Nombre de tirages et fréquence de chaque pool (dans l'ordre de « ordre ») pour une valeur moyenne. */
    private Reglage reglage(double valeur) {
        int i = 0;
        while (i + 1 < reglages.size() && reglages.get(i + 1).valeur() <= valeur) {
            i++;
        }
        Reglage r = reglages.get(i);
        if (i + 1 == reglages.size()) {
            int tirages = valeur <= r.valeur() ? r.tirages() : (int) Math.max(r.tirages(), Math.round(r.tirages() * valeur / r.valeur()));
            return new Reglage(valeur, tirages, r.debut(), r.debut());
        }
        double suivant = reglages.get(i + 1).valeur();
        double part = Math.max(0, Math.min(1, (valeur - r.valeur()) / (suivant - r.valeur())));
        double[] frequences = new double[ordre.size()];
        for (int k = 0; k < frequences.length; k++) {
            frequences[k] = r.debut()[k] + part * (r.fin()[k] - r.debut()[k]);
        }
        return new Reglage(valeur, r.tirages(), frequences, frequences);
    }

    /**
     * Contenu d'un coffre de cette valeur moyenne, au format d'un message de récompense ({type: objet, donnees, nombre},
     * {type: custom, id, nombre}) : un pool par tirage selon les fréquences, puis une entrée selon les poids.
     */
    List<Map<String, Object>> tirer(double valeur) {
        List<Map<String, Object>> contenu = new ArrayList<>();
        if (!disponibles()) {
            return contenu;
        }
        Reglage r = reglage(valeur);
        double[] frequences = r.debut().clone();
        ThreadLocalRandom hasard = ThreadLocalRandom.current();
        double total = 0;
        for (int k = 0; k < ordre.size(); k++) {
            frequences[k] = pools.get(ordre.get(k)).isEmpty() ? 0 : Math.max(0, frequences[k]);
            total += frequences[k];
        }
        for (int i = 0; i < r.tirages() && total > 0; i++) {
            double tirage = hasard.nextDouble(total);
            for (int k = 0; k < ordre.size(); k++) {
                tirage -= frequences[k];
                if (tirage < 0) {
                    Entree e = choisir(pools.get(ordre.get(k)), hasard);
                    if (e != null) {
                        e.contenu().forEach(el -> ajouter(contenu, el));
                    }
                    break;
                }
            }
        }
        return contenu;
    }

    private static Entree choisir(List<Entree> entrees, ThreadLocalRandom hasard) {
        int total = 0;
        for (Entree e : entrees) {
            total += Math.max(0, e.poids());
        }
        if (total <= 0) {
            return null;
        }
        int tirage = hasard.nextInt(total);
        for (Entree e : entrees) {
            tirage -= Math.max(0, e.poids());
            if (tirage < 0) {
                return e;
            }
        }
        return null;
    }

    /** Ajoute un élément tiré ; un objet « enchanter » est créé à chaque unité (chacune son tirage d'enchantements). */
    private void ajouter(List<Map<String, Object>> contenu, Map<String, Object> el) {
        if (!"objet".equals(el.get("type")) || el.containsKey("donnees")) {
            contenu.add(new LinkedHashMap<>(el));
            return;
        }
        long unites = Math.max(1, nombre(el.get("nombre")));
        for (long i = 0; i < unites; i++) {
            ItemStack objet = creer(el);
            if (objet != null) {
                contenu.add(fige(objet, 1));
            }
        }
    }

    private static Map<String, Object> fige(ItemStack objet, long nombre) {
        Map<String, Object> el = new LinkedHashMap<>();
        el.put("type", "objet");
        el.put("donnees", Base64.getEncoder().encodeToString(objet.serializeAsBytes()));
        el.put("nombre", nombre);
        return el;
    }

    private static long nombre(Object valeur) {
        return valeur instanceof Number n ? n.longValue() : 0;
    }

    // ------------------------------------------------------------------ objets

    /**
     * L'objet décrit par un élément, comme dans KG_Rewards : id, « potion: <type> », « enchanter: [min, max] » (enchanté
     * comme à une table d'enchantement d'un niveau tiré entre min et max : jamais d'enchantement de trésor ; un livre
     * devient un livre enchanté), puis « enchantements: {clé: niveau} » posés en plus. Null (et une ligne dans la
     * console) si l'id, le type de potion ou un enchantement est inconnu.
     */
    private ItemStack creer(Map<String, Object> el) {
        String id = String.valueOf(el.get("id"));
        Material materiau = el.get("id") == null ? null : Material.matchMaterial(id);
        if (materiau == null || !materiau.isItem() || materiau.isAir()) {
            plugin.getLogger().severe("fusion.yml : objet inconnu « " + id + " », élément ignoré.");
            return null;
        }
        ItemStack objet = new ItemStack(materiau);
        if (el.get("potion") != null) {
            PotionType type;
            try {
                type = PotionType.valueOf(String.valueOf(el.get("potion")).toUpperCase());
            } catch (IllegalArgumentException e) {
                type = null;
            }
            if (type == null || !(objet.getItemMeta() instanceof PotionMeta potion)) {
                plugin.getLogger().severe("fusion.yml : potion inconnue « " + el.get("potion") + " » sur " + id
                        + ", élément ignoré.");
                return null;
            }
            potion.setBasePotionType(type);
            objet.setItemMeta(potion);
        }
        if (el.get("enchanter") instanceof List<?> niveaux && niveaux.size() == 2) {
            int min = (int) Math.max(1, nombre(niveaux.get(0)));
            int max = (int) Math.max(min, nombre(niveaux.get(1)));
            ThreadLocalRandom hasard = ThreadLocalRandom.current();
            objet = Bukkit.getItemFactory().enchantWithLevels(objet, hasard.nextInt(min, max + 1),
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                            .getTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE), hasard);
        } else if (el.containsKey("enchanter")) {
            plugin.getLogger().severe("fusion.yml : « enchanter » doit être [min, max] sur " + id + ", élément ignoré.");
            return null;
        }
        if (el.get("enchantements") instanceof Map<?, ?> enchantements) {
            ItemMeta meta = objet.getItemMeta();
            for (Map.Entry<?, ?> e : enchantements.entrySet()) {
                String cle = String.valueOf(e.getKey()).toLowerCase();
                Enchantment enchantement = NamespacedKey.fromString(cle) == null ? null : RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.fromString(cle));
                if (enchantement == null) {
                    plugin.getLogger().severe("fusion.yml : enchantement inconnu « " + cle + " » sur " + id
                            + ", élément ignoré.");
                    return null;
                }
                int niveau = (int) Math.max(1, nombre(e.getValue()));
                if (meta instanceof EnchantmentStorageMeta livre) {
                    livre.addStoredEnchant(enchantement, niveau, true);
                } else {
                    meta.addEnchant(enchantement, niveau, true);
                }
            }
            objet.setItemMeta(meta);
        }
        return objet;
    }

    // ------------------------------------------------------------------ lecture

    private void charger(File fichier) {
        if (!fichier.isFile()) {
            plugin.getLogger().info("fusion.yml absent : la fusion des coffres est désactivée.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ordre.addAll(yaml.getStringList("ordre"));
        ConfigurationSection ps = yaml.getConfigurationSection("pools");
        int entrees = 0;
        for (String pool : ordre) {
            List<Entree> liste = new ArrayList<>();
            for (Map<?, ?> m : ps == null ? List.<Map<?, ?>>of() : ps.getMapList(pool)) {
                List<Map<String, Object>> contenu = new ArrayList<>();
                if (m.get("contenu") instanceof List<?> elements) {
                    for (Object o : elements) {
                        Map<String, Object> el = element(o);
                        if (el != null) {
                            contenu.add(el);
                        }
                    }
                }
                if (!contenu.isEmpty()) {
                    liste.add(new Entree((int) nombre(m.get("poids")), contenu));
                }
            }
            pools.put(pool, liste);
            entrees += liste.size();
        }
        for (Map<?, ?> m : yaml.getMapList("reglages")) {
            double[] debut = frequences(m.get("debut"));
            double[] fin = frequences(m.get("fin"));
            if (m.get("valeur") instanceof Number valeur && debut != null && fin != null && nombre(m.get("tirages")) > 0) {
                reglages.add(new Reglage(valeur.doubleValue(), (int) nombre(m.get("tirages")), debut, fin));
            }
        }
        reglages.sort((a, b) -> Double.compare(a.valeur(), b.valeur()));
        plugin.getLogger().info("fusion.yml : " + entrees + " entrées dans " + ordre.size() + " pools, " + reglages.size()
                + " réglages" + (disponibles() ? "." : " : rien d'utilisable, la fusion des coffres est désactivée."));
    }

    /**
     * Un élément d'un pool, prêt à être tiré : un objet sans « enchanter » est créé une fois et figé (« donnees ») ; avec
     * « enchanter », il est gardé tel quel et créé à chaque tirage. Null s'il ne peut pas être créé.
     */
    private Map<String, Object> element(Object brut) {
        if (!(brut instanceof Map<?, ?> m)) {
            return null;
        }
        Map<String, Object> el = new LinkedHashMap<>();
        m.forEach((k, v) -> el.put(String.valueOf(k), v));
        long nombre = Math.max(1, nombre(el.get("nombre")));
        el.put("nombre", nombre);
        if (!"objet".equals(el.get("type"))) {
            return "custom".equals(el.get("type")) && el.get("id") != null ? el : null;
        }
        if (el.containsKey("donnees")) {
            return el;
        }
        ItemStack objet = creer(el);
        if (objet == null) {
            return null;
        }
        return el.containsKey("enchanter") ? el : fige(objet, nombre);
    }

    private double[] frequences(Object brut) {
        if (!(brut instanceof List<?> liste) || liste.size() != ordre.size()) {
            return null;
        }
        double[] f = new double[liste.size()];
        for (int i = 0; i < f.length; i++) {
            f[i] = liste.get(i) instanceof Number n ? n.doubleValue() : 0;
        }
        return f;
    }
}
