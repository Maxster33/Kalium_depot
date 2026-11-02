package fr.kalium.cultures;

import fr.kalium.blocs.KSBlocs;
import fr.kalium.blocs.TypeBloc;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * KS_Cultures (LeKiwi06, liste du 01/11/2026 validée le 02/11, cahier local Blocs_Cultures_Objets.md, parties 10 à 14) :
 * cultures de Kixster.
 *
 * <ul>
 *   <li>Fleurs : au composteur, 1 fleur -> 1 à 3 graines ; la graine posée sur la terre labourée pousse, puis la vraie
 *       fleur est posée. Les grandes fleurs ne se dupliquent plus à la poudre d'os ({@link Fleurs}).</li>
 *   <li>Nouveaux végétaux ({@link Vegetal}) : maïs (2 blocs de haut), tomate, poivron, salade, oignon, aubergine, riz
 *       (dans l'eau), cerises et kiwi (arbustes récoltés d'un clic droit). Récolte à la main seulement : ni l'eau ni
 *       les villageois fermiers ne les récoltent (canne à sucre et varech, voir KS_Blocs).</li>
 *   <li>Graines par biome : une herbe cassée dans le bon biome (Terralith compris, config.yml) donne la graine du
 *       végétal une fois sur 8, en plus de la graine de blé vanilla. Une fois le végétal récolté mûr, le composteur en
 *       donne aussi les graines (1 fruit -> 1 graine) : liste des végétaux découverts par joueur.</li>
 *   <li>Mélange de graines : le composteur stocke des graines ; avec 9 sortes différentes, il donne un mélange (9 graines
 *       -> 1 mélange), qui fait pousser un végétal au hasard ({@link Composteur}).</li>
 * </ul>
 *
 * Les stades de pousse sont des blocs custom de KS_Blocs (apparences ks_cultures_terre, sur la canne à sucre, et
 * ks_cultures_eau, sur le varech) ; textures provisoires en attendant celles de LeKiwi06. Objets : livre de
 * connaissances (aucun usage vanilla) à l'image d'un objet du jeu, marqué ks_cultures:objet (règles de KS_Textures à
 * ajouter).
 */
public final class KSCultures extends JavaPlugin {

    static final String MELANGE = "melange";

    private static KSCultures instance;

    private Lang lang;
    private NamespacedKey cleObjet;
    private NamespacedKey cleDecouverts;
    private Plantes plantes;

    @Override
    public void onEnable() {
        instance = this;
        cleObjet = new NamespacedKey(this, "objet");
        cleDecouverts = new NamespacedKey(this, "decouverts");
        saveDefaultConfig();
        lang = new Lang(this);
        plantes = new Plantes(this);
        Composteur composteur = new Composteur(this);
        for (Vegetal v : Vegetal.values()) {
            KSBlocs.declarer(new TypeBloc(v.type(), v.lieu == Vegetal.Lieu.EAU ? "ks_cultures_eau" : "ks_cultures_terre",
                    v.lieu == Vegetal.Lieu.EAU ? Material.KELP : Material.SUGAR_CANE)
                    .butin(plantes::butin).ecouteur(plantes));
        }
        for (String graine : Fleurs.GRAINES.keySet()) {
            KSBlocs.declarer(new TypeBloc(Fleurs.type(graine), "ks_cultures_terre", Material.SUGAR_CANE)
                    .butin(plantes::butin).ecouteur(plantes));
        }
        getServer().getPluginManager().registerEvents(plantes, this);
        getServer().getPluginManager().registerEvents(composteur, this);
        getServer().getScheduler().runTaskTimer(this, plantes::pousser, 1200L, 1200L);
        lang.saveIfNeeded();
        getLogger().info(Vegetal.values().length + " vegetaux, " + Fleurs.GRAINES.size() + " graines de fleurs");
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    static KSCultures instance() {
        return instance;
    }

    Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    // ------------------------------------------------------------------ réglages (valeurs par défaut dans le code)

    /** Durée moyenne d'un stade de pousse, en minutes (chunk chargé, assez de lumière). */
    double minutesParStade(String plante) {
        double defaut = "mais".equals(plante) ? 20 : 15;
        return Math.max(1, getConfig().getDouble("pousse.minutes-par-stade." + plante,
                getConfig().getDouble("pousse.minutes-par-stade.defaut", defaut)));
    }

    /** Chance qu'une herbe cassée dans le bon biome donne une graine (vanilla, graines de blé : 1 sur 8). */
    double chanceHerbe() {
        return getConfig().getDouble("herbes.chance", 0.125);
    }

    /** Végétaux dont la graine peut sortir d'une herbe de ce biome (clé « minecraft:plains », « terralith:... »). */
    List<Vegetal> vegetauxDuBiome(String biome) {
        String nom = biome.substring(biome.indexOf(':') + 1);
        List<Vegetal> res = new ArrayList<>();
        for (Vegetal v : Vegetal.values()) {
            List<String> motifs = getConfig().isList("herbes.biomes." + v.cle())
                    ? getConfig().getStringList("herbes.biomes." + v.cle()) : BIOMES_PAR_DEFAUT.get(v);
            boolean ok = false;
            for (String m : motifs) {
                if (m.startsWith("!")) {
                    if (nom.contains(m.substring(1))) {
                        ok = false;
                        break;
                    }
                } else if (nom.contains(m)) {
                    ok = true;
                }
            }
            if (ok) {
                res.add(v);
            }
        }
        return res;
    }

    /**
     * Proposition (à valider par LeKiwi06) : un biome convient si son nom contient l'un des mots, sauf ceux précédés de
     * « ! ». Les biomes de Terralith ont les mêmes mots (savanna, taiga, jungle...).
     */
    static final Map<Vegetal, List<String>> BIOMES_PAR_DEFAUT = Map.of(
            Vegetal.TOMATE, List.of("savanna"),
            Vegetal.POIVRON, List.of("badlands", "shrubland", "desert"),
            Vegetal.AUBERGINE, List.of("jungle", "!bamboo"),
            Vegetal.OIGNON, List.of("taiga", "!snowy"),
            Vegetal.SALADE, List.of("forest", "!dark", "!snowy", "!wintry"),
            Vegetal.MAIS, List.of("plains", "meadow", "!snowy"),
            Vegetal.RIZ, List.of("swamp", "river", "!frozen"),
            Vegetal.CERISES, List.of("cherry"),
            Vegetal.KIWI, List.of("bamboo"));

    // ------------------------------------------------------------------ objets

    /** L'id d'un objet de KS_Cultures (tomate, graine_tomate, graine_dandelion, melange...), ou null. */
    static String id(ItemStack objet) {
        if (instance == null || objet == null || objet.getType() != Material.KNOWLEDGE_BOOK || !objet.hasItemMeta()) {
            return null;
        }
        return objet.getItemMeta().getPersistentDataContainer().get(instance.cleObjet, PersistentDataType.STRING);
    }

    /** n objets de KS_Cultures de cet id, ou null si l'id est inconnu. */
    static ItemStack objet(String id, int n) {
        String nom;
        String modele;
        Vegetal v;
        if (MELANGE.equals(id)) {
            nom = "Mélange de graines";
            modele = "pitcher_pod";
        } else if (Fleurs.NOMS.containsKey(id)) {
            nom = Fleurs.NOMS.get(id);
            modele = "beetroot_seeds";
        } else if ((v = Vegetal.parGraine(id)) != null && v.aDesGraines()) {
            nom = v.nomGraine;
            modele = v.modeleGraine;
        } else if ((v = Vegetal.parFruit(id)) != null) {
            nom = v.nomFruit;
            modele = v.modeleFruit;
        } else {
            return null;
        }
        ItemStack o = new ItemStack(Material.KNOWLEDGE_BOOK, n);
        ItemMeta meta = o.getItemMeta();
        meta.displayName(instance.t("objet." + id, "<white>" + nom).decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft(modele));
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(instance.cleObjet, PersistentDataType.STRING, id);
        o.setItemMeta(meta);
        return o;
    }

    /**
     * n objets d'une clé de stockage (« minecraft:wheat_seeds » pour un objet du jeu, sinon id de KS_Cultures), ou
     * null si inconnue.
     */
    static ItemStack objetCle(String cle, int n) {
        if (cle.startsWith("minecraft:")) {
            Material m = Material.matchMaterial(cle);
            return m == null ? null : new ItemStack(m, n);
        }
        return objet(cle, n);
    }

    /** Clé d'un objet pour le composteur : id de KS_Cultures, ou « minecraft:<objet> » pour un objet du jeu sans marque. */
    static String cle(ItemStack objet) {
        String id = id(objet);
        if (id != null) {
            return id;
        }
        return objet.getType() == Material.KNOWLEDGE_BOOK ? null : objet.getType().getKey().asString();
    }

    // ------------------------------------------------------------------ végétaux découverts

    Set<String> decouverts(Player joueur) {
        String s = joueur.getPersistentDataContainer().get(cleDecouverts, PersistentDataType.STRING);
        Set<String> res = new LinkedHashSet<>();
        if (s != null && !s.isEmpty()) {
            res.addAll(Arrays.asList(s.split(",")));
        }
        return res;
    }

    boolean decouvert(Player joueur, Vegetal v) {
        return decouverts(joueur).contains(v.cle());
    }

    /** Retient qu'un joueur a récolté ce végétal mûr ; message la première fois s'il a des graines au composteur. */
    void decouvrir(Player joueur, Vegetal v) {
        Set<String> d = decouverts(joueur);
        if (!d.add(v.cle())) {
            return;
        }
        joueur.getPersistentDataContainer().set(cleDecouverts, PersistentDataType.STRING, String.join(",", d));
        if (v.aDesGraines()) {
            joueur.sendMessage(t("decouverte", "<green>Nouveau végétal découvert : <white><nom></white> ! Le composteur "
                    + "peut maintenant en tirer des graines.", "nom", v.nomFruit.toLowerCase(Locale.ROOT)));
        }
    }
}
