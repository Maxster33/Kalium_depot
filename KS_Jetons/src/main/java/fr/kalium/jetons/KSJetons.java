package fr.kalium.jetons;

import fr.kalium.menu.api.BedrockColors;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * KS_Jetons (cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort », LeKiwi06, 03/10/2026 ; ajouts
 * « jetons et badges » du 08/10/2026) : serveurs de survie.
 *
 * - Jetons : de fly, de la mort, de téléportation, de localisation (apparence : lingot de fer, de netherite, d'or, de
 *   cuivre) et de claim ; livres de connaissances inutilisables, empilables par 64.
 * - Badges (2.0.0) : « versions améliorées des jetons », de fly, de la mort, de téléportation, de localisation
 *   (apparence : bloc du lingot du jeton) ; un niveau ; « ils s'améliorent en fusionnant 2 badges du même niveau dans
 *   une enclume » (coût en niveaux : 30, 50, puis + 20 par niveau, réglable).
 * - **Inventaire spécial** (2.1.0, LeKiwi06, 18/10/2026 : « une seule interface de coffre, avec des slots dédiés pour
 *   les jetons de localisation si la personne a un badge de localisation etc ») : /jetons, /badges, /jeton, /badge et
 *   le bouton du menu. Un coffre de 4 rangées : une case par badge (porté tant qu'il y est), une case par jeton (sans
 *   limite de nombre), les cases de localisation (autant que le niveau du badge de localisation : un jeton posé dans une
 *   case = une destination de /home ; retiré, la case attend 24 h avant d'en reprendre un), une barre d'actions.
 * - Les autres plugins (KS_Teleport, KS_CoffreMort, KS_Fly, KS_Claim) lisent l'inventaire spécial : nombre(),
 *   consommer(), ajouter(), prix(), niveauBadge(), valeurBadge(), localisations(). Les délais des badges sont tenus par
 *   ces plugins, par joueur (et non par badge).
 * - 2.3.0 (maléfices du Nether, LeKiwi06, 30/10/2026 : « une 3e catégorie : équipements spéciaux ») : rangée « Tes
 *   équipements spéciaux » (coffre de 5 rangées). Les plugins y déclarent leurs équipements (declarerEquipement) ;
 *   un équipement est porté tant qu'il y est rangé (porteEquipement, equipement).
 */
public final class KSJetons extends JavaPlugin implements Listener {

    /** Les jetons : id, nom, apparence (modèle vanilla), effet, achetable avec le score. */
    public enum Type {
        FLY("fly", "Jeton de fly", "iron_ingot", "10 minutes de vol dans tes claims", false),
        MORT("mort", "Jeton de la mort", "netherite_ingot", "Récupère un de tes coffres de mort", false),
        TP("tp", "Jeton de téléportation", "gold_ingot", "Une téléportation sans attendre le délai", true),
        LOCALISATION("localisation", "Jeton de localisation", "copper_ingot",
                "Enregistre une destination de téléportation", false),
        CLAIM("claim", "Jeton de claim", "golden_shovel", "Un claim gratuit de plus", true);

        final String id;
        final String nom;
        final String modele;
        final String effet;
        final boolean achetable;

        Type(String id, String nom, String modele, String effet, boolean achetable) {
            this.id = id;
            this.nom = nom;
            this.modele = modele;
            this.effet = effet;
            this.achetable = achetable;
        }

        public String id() {
            return id;
        }

        public String nom() {
            return nom;
        }

        public static Type parId(String id) {
            for (Type t : values()) {
                if (t.id.equalsIgnoreCase(id)) {
                    return t;
                }
            }
            return null;
        }
    }

    /** Les badges : id, nom, apparence (bloc du lingot du jeton), valeur de chaque niveau (config.yml : badges.<id>). */
    public enum Badge {
        /** Minutes de vol par heure. */
        FLY("fly", "Badge de fly", "iron_block", 1, 3, 5, 10, 15),
        /** Jours réels entre deux récupérations gratuites. */
        MORT("mort", "Badge de la mort", "netherite_block", 7, 6, 5, 4, 3, 2, 1),
        /** Téléportations gratuites supplémentaires stockées. */
        TP("tp", "Badge de téléportation", "gold_block", 1, 2, 3, 4, 5),
        /** Emplacements de localisation, en plus de /home bed et /spawn (niveau 1 : 1, niveau 2 : 2, etc.). */
        LOCALISATION("localisation", "Badge de localisation", "copper_block", 1, 2, 3, 4, 5);

        final String id;
        final String nom;
        final String modele;
        final List<Integer> defaut;

        Badge(String id, String nom, String modele, Integer... defaut) {
            this.id = id;
            this.nom = nom;
            this.modele = modele;
            this.defaut = List.of(defaut);
        }

        public String id() {
            return id;
        }

        public String nom() {
            return nom;
        }

        public static Badge parId(String id) {
            for (Badge b : values()) {
                if (b.id.equalsIgnoreCase(id)) {
                    return b;
                }
            }
            return null;
        }

        /** Texte de l'effet pour la valeur v du niveau. */
        String effet(int v) {
            return switch (this) {
                case FLY -> v + (v > 1 ? " minutes" : " minute") + " de vol par heure dans tes claims";
                case MORT -> "Un coffre de mort récupéré gratuitement " + (v > 1 ? "tous les " + v + " jours" : "chaque jour");
                case TP -> v + (v > 1 ? " téléportations gratuites" : " téléportation gratuite") + " de plus en réserve";
                case LOCALISATION -> v + (v > 1 ? " emplacements" : " emplacement") + " de localisation en plus";
            };
        }
    }

    /** 2.1.0 : l'inventaire spécial ouvert (un coffre, dessiné à partir de la fiche du joueur à chaque changement). */
    private record Special(UUID joueur) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** 2.1.0 : ce que l'inventaire spécial d'un joueur contient. */
    private static final class Fiche {
        /** Badges portés : type -> niveau. */
        final EnumMap<Badge, Integer> badges = new EnumMap<>(Badge.class);
        /** Jetons (sauf localisation) : type -> nombre, sans limite. */
        final EnumMap<Type, Long> jetons = new EnumMap<>(Type.class);
        /** Cases de localisation (0, 1...) -> jeton posé (un seul ; il porte sa magnétite, voir KS_Teleport). */
        final TreeMap<Integer, ItemStack> localisations = new TreeMap<>();
        /** Case de localisation -> date à laquelle son jeton a été retiré. */
        final Map<Integer, Long> liberees = new HashMap<>();
        /** Objets à rendre au joueur (reprise de la 2.0) : à sa connexion ou à l'ouverture de l'inventaire spécial. */
        final List<ItemStack> aRendre = new ArrayList<>();
        /** 2.3.0 : équipements spéciaux rangés : id de l'équipement -> objet (un seul ; porté tant qu'il y est). */
        final Map<String, ItemStack> equipements = new LinkedHashMap<>();

        boolean vide() {
            return badges.isEmpty() && jetons.isEmpty() && localisations.isEmpty() && liberees.isEmpty()
                    && aRendre.isEmpty() && equipements.isEmpty();
        }
    }

    /** 2.3.0 : un équipement spécial déclaré par un autre plugin (KS_MaleficesNether : amulette mangeuse de mal). */
    private record Equipement(String id, String nom, Predicate<ItemStack> reconnait) {
    }

    /** Fusion possible de deux badges : badge et niveau obtenus, coût en niveaux. */
    private record Fusion(Badge badge, int niveau, int cout) {
    }

    /** Au-delà, le jeu du joueur affiche « Trop cher ! » (voir KS_Enclume). */
    private static final int COUT_AFFICHE_MAX = 39;
    private static final int CASE_RESULTAT = 2;
    /** Coût de fusion par défaut pour atteindre le niveau 2, 3... ; ensuite + PAS_FUSION par niveau. */
    private static final List<Integer> FUSION_DEFAUT = List.of(30, 50, 70, 90, 110, 130);
    private static final int PAS_FUSION = 20;

    /**
     * 2.1.0 : le coffre de l'inventaire spécial : 3 rangées (badges, jetons, localisations) et la barre d'actions.
     * 2.3.0 : une 4e rangée, les équipements spéciaux, avant la barre d'actions.
     */
    private static final int TAILLE = 45;
    private static final int LIGNE_BADGES = 0;
    private static final int LIGNE_JETONS = 9;
    private static final int LIGNE_LOCALISATIONS = 18;
    private static final int LIGNE_EQUIPEMENTS = 27;
    private static final int CASE_AIDE = 39;
    private static final int CASE_ACHAT = 40;
    private static final int CASE_FERMER = 41;
    /** Cases de localisation au plus (une rangée, après son étiquette). */
    private static final int LOCALISATIONS_MAX = 8;
    /** 2.3.0 : équipements spéciaux au plus (une rangée, après son étiquette). */
    private static final int EQUIPEMENTS_MAX = 8;
    /** Les jetons qui ont leur case, dans l'ordre (le jeton de localisation a sa propre rangée). */
    private static final List<Type> JETONS_RANGES = Arrays.stream(Type.values())
            .filter(t -> t != Type.LOCALISATION).toList();

    private static KSJetons instance;
    private static NamespacedKey marqueur;
    private static NamespacedKey cleBadge;
    private static NamespacedKey cleNiveau;

    private Lang lang;
    private Gui gui;
    private File fichier;
    private final Map<UUID, Fiche> fiches = new HashMap<>();
    private boolean aSauver;
    /** Dernier message de refus envoyé à chaque joueur (millisecondes). */
    private final Map<UUID, Long> refus = new HashMap<>();
    /** 2.1.0 : KS_Teleport vérifie un jeton de localisation posé (magnétite) ; null = accepté. */
    private BiFunction<Player, ItemStack, Component> validateur;
    /** 2.1.0 : prévenus quand les jetons de localisation d'un joueur changent (KS_Teleport : magnétites protégées). */
    private final List<Consumer<UUID>> ecouteurs = new ArrayList<>();
    /** 2.3.0 : équipements spéciaux déclarés, dans l'ordre de leurs cases. */
    private final Map<String, Equipement> equipementsDeclares = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        marqueur = new NamespacedKey(this, "jeton");
        cleBadge = new NamespacedKey(this, "badge");
        cleNiveau = new NamespacedKey(this, "niveau");
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "jetons.yml");
        charger();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("jetons").setExecutor(this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "jetons",
                    lang.c("special.bouton-nom", "<aqua><bold>Inventaire spécial"),
                    lang.c("special.bouton-description", "<gray>Tes jetons et tes badges"), 40, this::ouvrir);
        }
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Special) {
                joueur.closeInventory();
            }
        }
        sauver();
    }

    // ------------------------------------------------------------------ objets

    private static Component texte(String texte, NamedTextColor couleur) {
        return Component.text(texte, couleur).decoration(TextDecoration.ITALIC, false);
    }

    /** Un jeton (objet). */
    public static ItemStack creer(Type type) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte(type.nom, NamedTextColor.AQUA));
        // 2.1.0 : le jeton de localisation se pose dans une case de localisation de l'inventaire spécial.
        meta.lore(List.of(texte(type.effet, NamedTextColor.GRAY), texte(type == Type.LOCALISATION
                ? "À poser dans tes localisations : /jetons" : "À ranger dans tes jetons : /jetons", NamedTextColor.DARK_GRAY)));
        meta.setItemModel(NamespacedKey.minecraft(type.modele));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.STRING, type.id);
        item.setItemMeta(meta);
        return item;
    }

    /** Un badge (objet) du niveau donné (ramené entre 1 et le niveau maximal). */
    public static ItemStack creerBadge(Badge badge, int niveau) {
        int max = niveauMax(badge);
        niveau = Math.max(1, Math.min(max, niveau));
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte(badge.nom + " (niveau " + niveau + ")", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(texte(badge.effet(valeurBadge(badge, niveau)), NamedTextColor.GRAY));
        lore.add(texte(niveau < max ? "Fusionne 2 badges de ce niveau dans une enclume" : "Niveau maximal",
                NamedTextColor.GRAY));
        lore.add(texte("À ranger dans tes badges : /jetons", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        meta.setItemModel(NamespacedKey.minecraft(badge.modele));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(cleBadge, PersistentDataType.STRING, badge.id);
        meta.getPersistentDataContainer().set(cleNiveau, PersistentDataType.INTEGER, niveau);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * id_custom des objets qu'on peut donner (KS_KaliumGive) : jeton_<id> et badge_<id>_<niveau>. Le jeton de
     * localisation n'y est pas : il ne vient que du craft, lié à une magnétite.
     */
    public static List<String> idsCustom() {
        List<String> ids = new ArrayList<>();
        for (Type type : Type.values()) {
            if (type != Type.LOCALISATION) {
                ids.add("jeton_" + type.id);
            }
        }
        for (Badge badge : Badge.values()) {
            for (int niveau = 1; niveau <= niveauMax(badge); niveau++) {
                ids.add("badge_" + badge.id + "_" + niveau);
            }
        }
        return ids;
    }

    /** L'objet d'un id_custom (voir idsCustom), ou null. */
    public static ItemStack creerCustom(String id) {
        if (id == null || instance == null || !instance.isEnabled() || !idsCustom().contains(id)) {
            return null;
        }
        if (id.startsWith("jeton_")) {
            return creer(Type.parId(id.substring(6)));
        }
        int coupe = id.lastIndexOf('_');
        return creerBadge(Badge.parId(id.substring(6, coupe)), Integer.parseInt(id.substring(coupe + 1)));
    }

    private static String lire(ItemStack item, NamespacedKey cle) {
        if (item == null || cle == null || item.isEmpty() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(cle, PersistentDataType.STRING);
    }

    /** Type du jeton, ou null si ce n'est pas un jeton. */
    public static Type type(ItemStack item) {
        String id = lire(item, marqueur);
        return id == null ? null : Type.parId(id);
    }

    /** Type du badge, ou null si ce n'est pas un badge. */
    public static Badge badge(ItemStack item) {
        String id = lire(item, cleBadge);
        return id == null ? null : Badge.parId(id);
    }

    /** Niveau du badge (objet), 0 si ce n'est pas un badge. */
    public static int niveau(ItemStack item) {
        if (badge(item) == null) {
            return 0;
        }
        Integer n = item.getItemMeta().getPersistentDataContainer().get(cleNiveau, PersistentDataType.INTEGER);
        return n == null ? 1 : Math.max(1, n);
    }

    private static List<Integer> valeurs(Badge badge) {
        List<Integer> liste = instance.getConfig().getIntegerList("badges." + badge.id);
        return liste.isEmpty() ? badge.defaut : liste;
    }

    /** Niveau maximal d'un badge. */
    public static int niveauMax(Badge badge) {
        return valeurs(badge).size();
    }

    /** Valeur d'un badge à ce niveau (minutes de vol, jours, téléportations, emplacements) ; 0 pour le niveau 0. */
    public static int valeurBadge(Badge badge, int niveau) {
        List<Integer> liste = valeurs(badge);
        return niveau < 1 ? 0 : liste.get(Math.min(niveau, liste.size()) - 1);
    }

    // ------------------------------------------------------------------ API (autres plugins)

    /** 2.2.0 : dans le monde de test (KS_MondeTest), la fiche du compte de test du joueur. */
    private static Fiche fiche(UUID joueur) {
        return instance.fiches.computeIfAbsent(CompteTest.de(joueur), u -> new Fiche());
    }

    /** Jetons de ce type dans l'inventaire spécial du joueur (localisation : jetons posés dans ses cases). */
    public static long nombre(UUID joueur, Type type) {
        Fiche f = instance.fiches.get(CompteTest.de(joueur));
        if (f == null) {
            return 0;
        }
        return type == Type.LOCALISATION ? f.localisations.size() : f.jetons.getOrDefault(type, 0L);
    }

    /** Retire n jetons de l'inventaire spécial ; faux (rien n'est retiré) s'il n'en a pas assez. Pas la localisation. */
    public static boolean consommer(UUID joueur, Type type, long n) {
        if (type == Type.LOCALISATION || n < 0 || nombre(joueur, type) < n) {
            return false;
        }
        Fiche f = fiche(joueur);
        long reste = f.jetons.getOrDefault(type, 0L) - n;
        if (reste > 0) {
            f.jetons.put(type, reste);
        } else {
            f.jetons.remove(type);
        }
        instance.changement(joueur, false);
        return true;
    }

    /** Ajoute n jetons à l'inventaire spécial. Pas la localisation (chaque jeton porte sa magnétite : voir rendre). */
    public static void ajouter(UUID joueur, Type type, long n) {
        if (n <= 0 || type == Type.LOCALISATION) {
            return;
        }
        fiche(joueur).jetons.merge(type, n, Long::sum);
        instance.changement(joueur, false);
    }

    /** Prix d'un jeton en points (config.yml) ; 0 : pas en vente (toujours 0 pour un jeton qui ne s'achète pas). */
    public static long prix(Type type) {
        return type.achetable ? Math.max(0, instance.getConfig().getLong("prix." + type.id, 0)) : 0;
    }

    /** Niveau du badge de ce type porté par le joueur (rangé dans son inventaire spécial) ; 0 : aucun. */
    public static int niveauBadge(UUID joueur, Badge badge) {
        Fiche f = instance.fiches.get(CompteTest.de(joueur));
        Integer n = f == null ? null : f.badges.get(badge);
        return n == null ? 0 : Math.min(n, niveauMax(badge));
    }

    /** Valeur du badge porté par le joueur ; 0 s'il n'en porte pas. */
    public static int valeurBadge(UUID joueur, Badge badge) {
        return valeurBadge(badge, niveauBadge(joueur, badge));
    }

    /** 2.1.0 : jetons de localisation posés dans les cases du joueur : case (0, 1...) -> jeton (copie). */
    public static Map<Integer, ItemStack> localisations(UUID joueur) {
        Map<Integer, ItemStack> r = new TreeMap<>();
        Fiche f = instance.fiches.get(CompteTest.de(joueur));
        if (f != null) {
            f.localisations.forEach((k, jeton) -> r.put(k, jeton.clone()));
        }
        return r;
    }

    /** 2.1.0 : les jetons de localisation posés de tous les joueurs (en ligne ou non). */
    public static Map<UUID, Map<Integer, ItemStack>> toutesLocalisations() {
        Map<UUID, Map<Integer, ItemStack>> r = new HashMap<>();
        instance.fiches.forEach((u, f) -> {
            if (!f.localisations.isEmpty()) {
                Map<Integer, ItemStack> copie = new TreeMap<>(); // 2.2.0 : par fiche (compte de test compris)
                f.localisations.forEach((k, jeton) -> copie.put(k, jeton.clone()));
                r.put(u, copie);
            }
        });
        return r;
    }

    /** 2.1.0 : temps (millisecondes) avant qu'une case de localisation vidée reprenne un jeton ; 0 : tout de suite. */
    public static long attenteLocalisation(UUID joueur, int caseLocalisation) {
        Fiche f = instance.fiches.get(CompteTest.de(joueur));
        Long date = f == null ? null : f.liberees.get(caseLocalisation);
        return date == null ? 0 : Math.max(0, date + instance.delaiLocalisation() - System.currentTimeMillis());
    }

    /** 2.1.0 : délai d'une case de localisation après le retrait de son jeton (config.yml, 24 h par défaut). */
    public static long delaiLocalisationMs() {
        return instance.delaiLocalisation();
    }

    /** 2.1.0 (KS_Teleport) : vérifie un jeton de localisation que le joueur pose ; renvoie le refus, ou null. */
    public static void validateurLocalisation(BiFunction<Player, ItemStack, Component> verification) {
        instance.validateur = verification;
    }

    /** 2.1.0 (KS_Teleport) : appelé avec le joueur dont les jetons de localisation viennent de changer. */
    public static void ecouterLocalisations(Consumer<UUID> ecouteur) {
        instance.ecouteurs.add(ecouteur);
    }

    /**
     * 2.1.0 : pose un jeton de localisation dans une case, sans vérification (reprise des emplacements de KS_Teleport
     * 1.3.0 et avant). Faux si la case est prise ou hors des cases.
     */
    public static boolean poserLocalisation(UUID joueur, int caseLocalisation, ItemStack jeton) {
        Fiche f = fiche(joueur);
        if (caseLocalisation < 0 || caseLocalisation >= LOCALISATIONS_MAX || f.localisations.containsKey(caseLocalisation)
                || type(jeton) != Type.LOCALISATION) {
            return false;
        }
        ItemStack un = jeton.clone();
        un.setAmount(1);
        f.localisations.put(caseLocalisation, un);
        instance.changement(joueur, true);
        return true;
    }

    /** 2.1.0 : un objet à rendre au joueur, tout de suite s'il est connecté, sinon à sa prochaine connexion. */
    public static void rendre(UUID joueur, ItemStack objet) {
        if (objet == null || objet.isEmpty()) {
            return;
        }
        fiche(joueur).aRendre.add(objet.clone());
        Player p = Bukkit.getPlayer(joueur);
        if (p != null) {
            instance.donnerARendre(p);
        }
        instance.sauverBientot();
    }

    /** 2.1.0 : ouvre l'inventaire spécial (KS_Teleport : clic droit avec un jeton de localisation, /home). */
    public static void ouvrirSpecial(Player joueur) {
        instance.ouvrir(joueur);
    }

    /**
     * 2.3.0 : déclare un équipement spécial (à l'activation du plugin qui le possède) : il a sa case dans la rangée
     * « Tes équipements spéciaux » (8 au plus, dans l'ordre des déclarations) et il est porté tant qu'il y est rangé.
     *
     * @param id        identifiant, ex. « amulette »
     * @param nom       nom affiché sur la case vide
     * @param reconnait vrai pour un objet de cet équipement
     */
    public static void declarerEquipement(String id, String nom, Predicate<ItemStack> reconnait) {
        if (instance == null || id == null || reconnait == null) {
            return;
        }
        if (!instance.equipementsDeclares.containsKey(id) && instance.equipementsDeclares.size() >= EQUIPEMENTS_MAX) {
            instance.getLogger().warning("Équipement spécial refusé (" + EQUIPEMENTS_MAX + " au plus) : " + id);
            return;
        }
        instance.equipementsDeclares.put(id, new Equipement(id, nom, reconnait));
    }

    /** 2.3.0 : l'équipement spécial rangé par le joueur (copie), ou null. */
    public static ItemStack equipement(UUID joueur, String id) {
        Fiche f = instance.fiches.get(CompteTest.de(joueur));
        ItemStack objet = f == null ? null : f.equipements.get(id);
        return objet == null ? null : objet.clone();
    }

    /** 2.3.0 : le joueur porte-t-il cet équipement spécial (rangé dans son inventaire spécial) ? */
    public static boolean porteEquipement(UUID joueur, String id) {
        Fiche f = instance.fiches.get(CompteTest.de(joueur));
        return f != null && f.equipements.containsKey(id);
    }

    /** 2.3.0 : l'équipement spécial déclaré que cet objet représente, ou null. */
    private Equipement equipementDe(ItemStack objet) {
        if (objet == null || objet.isEmpty()) {
            return null;
        }
        for (Equipement e : equipementsDeclares.values()) {
            if (e.reconnait().test(objet)) {
                return e;
            }
        }
        return null;
    }

    private Equipement equipementDeCase(int place) {
        int i = place - LIGNE_EQUIPEMENTS - 1;
        List<Equipement> liste = new ArrayList<>(equipementsDeclares.values());
        return place > LIGNE_EQUIPEMENTS && place < LIGNE_EQUIPEMENTS + 9 && i < liste.size() ? liste.get(i) : null;
    }

    private long delaiLocalisation() {
        return Math.max(0, getConfig().getLong("localisation-delai-heures", 24)) * 3_600_000L;
    }

    /** Après un changement : enregistrer, redessiner le coffre ouvert du joueur, prévenir KS_Teleport. */
    private void changement(UUID joueur, boolean localisations) {
        sauverBientot();
        Player p = Bukkit.getPlayer(joueur);
        if (p != null && p.getOpenInventory().getTopInventory().getHolder() instanceof Special s
                && s.joueur().equals(joueur)) {
            dessiner(p.getOpenInventory().getTopInventory(), joueur);
        }
        if (localisations) {
            ecouteurs.forEach(e -> e.accept(joueur));
        }
    }

    // ------------------------------------------------------------------ le coffre

    /** Cases de localisation du coffre : la valeur du badge de localisation au niveau maximal (8 au plus). */
    private static int casesLocalisation() {
        return Math.min(LOCALISATIONS_MAX, valeurBadge(Badge.LOCALISATION, niveauMax(Badge.LOCALISATION)));
    }

    /** Plus petit niveau du badge de localisation qui ouvre la case k (0, 1...). */
    private static int niveauPour(int k) {
        for (int n = 1; n <= niveauMax(Badge.LOCALISATION); n++) {
            if (valeurBadge(Badge.LOCALISATION, n) > k) {
                return n;
            }
        }
        return niveauMax(Badge.LOCALISATION);
    }

    private static Badge badgeDeCase(int place) {
        int i = place - LIGNE_BADGES - 1;
        return place > LIGNE_BADGES && place < LIGNE_BADGES + 9 && i < Badge.values().length ? Badge.values()[i] : null;
    }

    private static Type jetonDeCase(int place) {
        int i = place - LIGNE_JETONS - 1;
        return place > LIGNE_JETONS && place < LIGNE_JETONS + 9 && i < JETONS_RANGES.size() ? JETONS_RANGES.get(i) : null;
    }

    private static int localisationDeCase(int place) {
        int k = place - LIGNE_LOCALISATIONS - 1;
        return place > LIGNE_LOCALISATIONS && place < LIGNE_LOCALISATIONS + 9 && k < casesLocalisation() ? k : -1;
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private Component titre() {
        return t("special.titre", "<aqua><bold>Inventaire spécial");
    }

    private ItemStack fixe(Material materiau, Component nom, Component... lignes) {
        return Contenant.objet(materiau, nom, List.of(lignes));
    }

    private static String chrono(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return s >= 3600 ? s / 3600 + " h " + (s % 3600) / 60 + " min" : s / 60 + " min " + s % 60 + " s";
    }

    private static boolean economie() {
        return Bukkit.getPluginManager().isPluginEnabled("KS_Economy");
    }

    /** Remplit le coffre d'après la fiche : rien de ce qu'il montre n'est un vrai objet (les clics sont gérés ici). */
    private void dessiner(Inventory inv, UUID joueur) {
        Fiche f = fiche(joueur);
        for (int i = 0; i < TAILLE; i++) {
            inv.setItem(i, Contenant.decor());
        }
        inv.setItem(LIGNE_BADGES, fixe(Material.ORANGE_STAINED_GLASS_PANE, t("coffre.ligne-badges", "<gold>Tes badges"),
                t("coffre.ligne-badges-1", "<gray>Un badge par type, porté tant"),
                t("coffre.ligne-badges-2", "<gray>qu'il est rangé ici.")));
        Badge[] badges = Badge.values();
        for (int b = 0; b < badges.length && b < 8; b++) {
            Integer n = f.badges.get(badges[b]);
            inv.setItem(LIGNE_BADGES + 1 + b, n != null ? creerBadge(badges[b], n)
                    : fixe(Material.LIGHT_GRAY_STAINED_GLASS_PANE, t("coffre.badge-vide", "<gray><nom> : aucun", "nom",
                    badges[b].nom), t("coffre.badge-poser", "<dark_gray>Pose un badge de ce type ici")));
        }
        inv.setItem(LIGNE_JETONS, fixe(Material.LIGHT_BLUE_STAINED_GLASS_PANE, t("coffre.ligne-jetons", "<aqua>Tes jetons"),
                t("coffre.ligne-jetons-1", "<gray>Une case par type de jeton,"),
                t("coffre.ligne-jetons-2", "<gray>autant de jetons que tu veux.")));
        for (int j = 0; j < JETONS_RANGES.size() && j < 8; j++) {
            Type type = JETONS_RANGES.get(j);
            long n = f.jetons.getOrDefault(type, 0L);
            if (n > 0) {
                ItemStack pile = creer(type);
                pile.setAmount((int) Math.min(64, n));
                ItemMeta meta = pile.getItemMeta();
                List<Component> lore = new ArrayList<>(meta.lore());
                lore.add(t("coffre.jeton-nombre", "<white>Tu en as <n>", "n", n).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                pile.setItemMeta(meta);
                inv.setItem(LIGNE_JETONS + 1 + j, pile);
            } else {
                inv.setItem(LIGNE_JETONS + 1 + j, fixe(Material.LIGHT_GRAY_STAINED_GLASS_PANE, t("coffre.jeton-vide",
                        "<gray><nom> : aucun", "nom", type.nom), t("coffre.jeton-poser", "<dark_gray>Pose des jetons ici")));
            }
        }
        inv.setItem(LIGNE_LOCALISATIONS, fixe(Material.LIME_STAINED_GLASS_PANE, t("coffre.ligne-localisations",
                        "<green>Tes localisations"),
                t("coffre.ligne-localisations-1", "<gray>Un jeton de localisation par"),
                t("coffre.ligne-localisations-2", "<gray>case : sa magnétite devient une"),
                t("coffre.ligne-localisations-3", "<gray>destination de /home."),
                t("coffre.ligne-localisations-4", "<gray>Retiré, la case attend <heures> h.", "heures",
                        delaiLocalisation() / 3_600_000L)));
        int actives = valeurBadge(joueur, Badge.LOCALISATION);
        for (int k = 0; k < casesLocalisation(); k++) {
            ItemStack jeton = f.localisations.get(k);
            ItemStack vue;
            if (jeton != null) {
                vue = jeton.clone();
                ItemMeta meta = vue.getItemMeta();
                List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
                lore.add((k < actives
                        ? t("coffre.loc-active", "<green>Case <n> : destination de /home", "n", k + 1)
                        : t("coffre.loc-inactive", "<red>Case <n> : badge trop bas", "n", k + 1))
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                vue.setItemMeta(meta);
            } else if (k >= actives) {
                vue = fixe(Material.BARRIER, t("coffre.loc-bloquee", "<red>Case <n> : bloquée", "n", k + 1),
                        t("coffre.loc-niveau", "<gray>Badge de localisation niveau <niveau>", "niveau", niveauPour(k)));
            } else {
                long attente = attenteLocalisation(joueur, k);
                vue = attente > 0
                        ? fixe(Material.CLOCK, t("coffre.loc-attente", "<yellow>Case <n> : dans <temps>", "n", k + 1,
                        "temps", chrono(attente)), t("coffre.loc-attente-info", "<gray>Elle reprendra un jeton ensuite."))
                        : fixe(Material.LIGHT_GRAY_STAINED_GLASS_PANE, t("coffre.loc-libre", "<gray>Case <n> : libre",
                        "n", k + 1), t("coffre.loc-poser", "<dark_gray>Pose un jeton de localisation ici"));
            }
            inv.setItem(LIGNE_LOCALISATIONS + 1 + k, vue);
        }
        inv.setItem(LIGNE_EQUIPEMENTS, fixe(Material.PURPLE_STAINED_GLASS_PANE, t("coffre.ligne-equipements",
                        "<light_purple>Tes équipements spéciaux"),
                t("coffre.ligne-equipements-1", "<gray>Un équipement par type, porté"),
                t("coffre.ligne-equipements-2", "<gray>tant qu'il est rangé ici.")));
        int e = 0;
        for (Equipement eq : equipementsDeclares.values()) {
            ItemStack objet = f.equipements.get(eq.id());
            inv.setItem(LIGNE_EQUIPEMENTS + 1 + e, objet != null ? objet.clone()
                    : fixe(Material.LIGHT_GRAY_STAINED_GLASS_PANE, t("coffre.equipement-vide", "<gray><nom> : aucun",
                    "nom", eq.nom()), t("coffre.equipement-poser", "<dark_gray>Pose cet équipement ici")));
            e++;
        }
        inv.setItem(CASE_AIDE, fixe(Material.BOOK, t("coffre.aide", "<gold>Comment ça marche"),
                t("coffre.aide-1", "<gray>Clic sur un objet de ton inventaire"),
                t("coffre.aide-2", "<gray>avec Maj : il va dans sa case."),
                t("coffre.aide-3", "<gray>Clic sur une case : tu reprends"),
                t("coffre.aide-4", "<gray>ce qu'elle contient."),
                t("coffre.aide-5", "<gray>Clic droit : la moitié des jetons.")));
        long prix = prix(Type.TP);
        if (prix > 0 && economie()) {
            inv.setItem(CASE_ACHAT, fixe(Material.EMERALD, t("coffre.acheter", "<green>Acheter : <nom>", "nom", Type.TP.nom),
                    t("coffre.acheter-prix", "<gray><prix> par jeton", "prix", points(prix))));
        }
        inv.setItem(CASE_FERMER, fixe(Material.BARRIER, t("coffre.fermer", "<red>Fermer")));
    }

    /** Inventaire spécial : un coffre de 5 rangées (badges, jetons, localisations, équipements, barre d'actions). */
    public void ouvrir(Player joueur) {
        donnerARendre(joueur);
        Inventory inv = Bukkit.createInventory(new Special(joueur.getUniqueId()), TAILLE, titre());
        dessiner(inv, joueur.getUniqueId());
        joueur.openInventory(inv);
        lang.saveIfNeeded();
    }

    /**
     * Tous les clics qui touchent le coffre sont annulés puis faits ici, au tick suivant (le jeu ne déplacerait pas les
     * objets dans la bonne case) : clic (droit) sur une case, Maj + clic sur un objet de son inventaire. Les clics dans
     * son propre inventaire restent libres.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClicSpecial(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Special special)
                || !(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        int brut = event.getRawSlot();
        boolean haut = brut >= 0 && brut < TAILLE;
        ClickType clic = event.getClick();
        boolean bas = event.getClickedInventory() instanceof PlayerInventory;
        if (!haut && !(bas && event.isShiftClick()) && clic != ClickType.DOUBLE_CLICK) {
            return;
        }
        event.setCancelled(true);
        if (!joueur.getUniqueId().equals(special.joueur()) || (clic != ClickType.LEFT && clic != ClickType.RIGHT
                && clic != ClickType.SHIFT_LEFT && clic != ClickType.SHIFT_RIGHT)) {
            return;
        }
        int caseBas = event.getSlot();
        getServer().getScheduler().runTask(this, () -> {
            if (!joueur.isOnline() || !(joueur.getOpenInventory().getTopInventory().getHolder() instanceof Special)) {
                return;
            }
            boolean localisations = haut ? clicCase(joueur, brut, clic) : ranger(joueur, caseBas);
            changement(joueur.getUniqueId(), localisations);
            joueur.updateInventory();
            lang.saveIfNeeded();
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGlisserSpecial(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Special
                && event.getRawSlots().stream().anyMatch(brut -> brut < TAILLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onConnexion(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (joueur.isOnline()) {
                donnerARendre(joueur);
            }
        }, 20L);
    }

    /** Clic sur une case du coffre ; vrai si les jetons de localisation ont changé. */
    private boolean clicCase(Player joueur, int place, ClickType clic) {
        UUID id = joueur.getUniqueId();
        Fiche f = fiche(id);
        if (place == CASE_FERMER) {
            joueur.closeInventory();
            return false;
        }
        if (place == CASE_ACHAT && prix(Type.TP) > 0 && economie()) {
            joueur.closeInventory();
            // Un joueur Bedrock n'affiche pas une fenêtre de saisie pendant qu'un coffre se referme.
            getServer().getScheduler().runTaskLater(this, () -> {
                if (joueur.isOnline()) {
                    quantite(joueur, Type.TP);
                }
            }, BedrockColors.isBedrock(joueur) ? 5L : 1L);
            return false;
        }
        ItemStack curseur = joueur.getItemOnCursor();
        boolean vide = curseur == null || curseur.isEmpty();
        boolean maj = clic.isShiftClick();
        Badge badge = badgeDeCase(place);
        Type type = jetonDeCase(place);
        int k = localisationDeCase(place);
        Equipement eq = equipementDeCase(place);
        if (eq != null) {
            ItemStack range = f.equipements.get(eq.id());
            if (vide || maj) {
                if (range != null) {
                    f.equipements.remove(eq.id());
                    reprendre(joueur, range, maj || !vide);
                }
                return false;
            }
            if (!eq.reconnait().test(curseur)) {
                refuser(joueur, t("coffre.refus-equipement", "<red>Seul l'équipement « <nom> » va dans cette case.",
                        "nom", eq.nom()));
                return false;
            }
            ItemStack un = curseur.clone();
            un.setAmount(1);
            f.equipements.put(eq.id(), un);
            curseur.setAmount(curseur.getAmount() - 1);
            if (range == null) {
                joueur.setItemOnCursor(curseur.getAmount() > 0 ? curseur : null);
            } else if (curseur.getAmount() > 0) {
                joueur.setItemOnCursor(curseur);
                donner(joueur, range);
            } else {
                // Échange : l'équipement déjà rangé passe sur le curseur.
                joueur.setItemOnCursor(range);
            }
            return false;
        }
        if (badge != null) {
            Integer niveau = f.badges.get(badge);
            if (vide || maj) {
                if (niveau != null) {
                    f.badges.remove(badge);
                    reprendre(joueur, creerBadge(badge, niveau), maj || !vide);
                }
                return false;
            }
            if (badge(curseur) != badge) {
                refuser(joueur, badge(curseur) == null ? t("coffre.refus-badge", "<red>Seul un <nom> va dans cette case.",
                        "nom", badge.nom) : t("coffre.refus-autre-badge", "<red>Ce badge va dans sa propre case."));
                return false;
            }
            f.badges.put(badge, niveau(curseur));
            // Échange : le badge déjà porté passe sur le curseur.
            joueur.setItemOnCursor(niveau == null ? null : creerBadge(badge, niveau));
            return false;
        }
        if (type != null) {
            long n = f.jetons.getOrDefault(type, 0L);
            if (vide || maj) {
                if (n > 0) {
                    int pris = (int) Math.min(64, n);
                    if (clic == ClickType.RIGHT) {
                        pris = (pris + 1) / 2;
                    }
                    ItemStack pile = creer(type);
                    pile.setAmount(pris);
                    if (n - pris > 0) {
                        f.jetons.put(type, n - pris);
                    } else {
                        f.jetons.remove(type);
                    }
                    reprendre(joueur, pile, maj || !vide);
                }
                return false;
            }
            if (type(curseur) != type) {
                refuser(joueur, t("coffre.refus-jeton", "<red>Seuls les <nom>s vont dans cette case.", "nom", type.nom));
                return false;
            }
            int pose = clic == ClickType.RIGHT ? 1 : curseur.getAmount();
            f.jetons.merge(type, (long) pose, Long::sum);
            curseur.setAmount(curseur.getAmount() - pose);
            joueur.setItemOnCursor(curseur.getAmount() > 0 ? curseur : null);
            return false;
        }
        if (k < 0) {
            return false;
        }
        ItemStack jeton = f.localisations.get(k);
        if (jeton != null) {
            if (!vide && !maj) {
                refuser(joueur, t("coffre.refus-occupee", "<red>Cette case a déjà un jeton : reprends-le d'abord."));
                return false;
            }
            f.localisations.remove(k);
            f.liberees.put(k, System.currentTimeMillis());
            reprendre(joueur, jeton, maj || !vide);
            getLogger().info(joueur.getName() + " retire son jeton de localisation de la case " + (k + 1) + ".");
            joueur.sendMessage(t("coffre.loc-retire", "<yellow>Case <n> libérée : elle reprendra un jeton dans <heures> h.",
                    "n", k + 1, "heures", delaiLocalisation() / 3_600_000L));
            return true;
        }
        if (vide || maj) {
            return false;
        }
        Component refusPose = refusLocalisation(joueur, curseur, k);
        if (refusPose != null) {
            refuser(joueur, refusPose);
            return false;
        }
        poserDepuis(joueur, curseur, k);
        curseur.setAmount(curseur.getAmount() - 1);
        joueur.setItemOnCursor(curseur.getAmount() > 0 ? curseur : null);
        return true;
    }

    /** Maj + clic sur un objet de son inventaire : il va dans sa case ; vrai si les jetons de localisation ont changé. */
    private boolean ranger(Player joueur, int caseBas) {
        PlayerInventory inventaire = joueur.getInventory();
        ItemStack objet = inventaire.getItem(caseBas);
        if (objet == null || objet.isEmpty()) {
            return false;
        }
        Fiche f = fiche(joueur.getUniqueId());
        Badge badge = badge(objet);
        Type type = type(objet);
        if (badge != null) {
            if (f.badges.containsKey(badge)) {
                refuser(joueur, t("coffre.refus-double", "<red>Tu portes déjà un <nom> : reprends-le d'abord.", "nom",
                        badge.nom));
                return false;
            }
            f.badges.put(badge, niveau(objet));
            objet.setAmount(objet.getAmount() - 1);
            inventaire.setItem(caseBas, objet.getAmount() > 0 ? objet : null);
            return false;
        }
        if (type == Type.LOCALISATION) {
            Component premierRefus = null;
            for (int k = 0; k < casesLocalisation(); k++) {
                if (f.localisations.containsKey(k)) {
                    continue;
                }
                Component refusPose = refusLocalisation(joueur, objet, k);
                if (refusPose == null) {
                    poserDepuis(joueur, objet, k);
                    objet.setAmount(objet.getAmount() - 1);
                    inventaire.setItem(caseBas, objet.getAmount() > 0 ? objet : null);
                    return true;
                }
                if (premierRefus == null) {
                    premierRefus = refusPose;
                }
            }
            refuser(joueur, premierRefus != null ? premierRefus
                    : t("coffre.refus-plein", "<red>Toutes tes cases de localisation sont prises."));
            return false;
        }
        if (type != null) {
            f.jetons.merge(type, (long) objet.getAmount(), Long::sum);
            inventaire.setItem(caseBas, null);
            return false;
        }
        Equipement eq = equipementDe(objet);
        if (eq != null) {
            if (f.equipements.containsKey(eq.id())) {
                refuser(joueur, t("coffre.refus-equipement-double", "<red>Tu portes déjà : <nom>. Reprends-le d'abord.",
                        "nom", eq.nom()));
                return false;
            }
            ItemStack un = objet.clone();
            un.setAmount(1);
            f.equipements.put(eq.id(), un);
            objet.setAmount(objet.getAmount() - 1);
            inventaire.setItem(caseBas, objet.getAmount() > 0 ? objet : null);
            return false;
        }
        // 2.3.0 : nouvelle clé (texte changé : les équipements spéciaux aussi).
        refuser(joueur, t("coffre.refus-objet-2",
                "<red>Seuls les jetons, les badges et les équipements spéciaux vont dans l'inventaire spécial."));
        return false;
    }

    /** Pourquoi ce jeton ne peut pas aller dans la case k, ou null s'il le peut. */
    private Component refusLocalisation(Player joueur, ItemStack objet, int k) {
        UUID id = joueur.getUniqueId();
        if (type(objet) != Type.LOCALISATION) {
            return t("coffre.refus-localisation", "<red>Seuls les jetons de localisation vont dans cette case.");
        }
        if (k >= valeurBadge(id, Badge.LOCALISATION)) {
            return t("coffre.refus-niveau", "<red>Case bloquée : il faut un badge de localisation de niveau <niveau>.",
                    "niveau", niveauPour(k));
        }
        long attente = attenteLocalisation(id, k);
        if (attente > 0) {
            return t("coffre.refus-attente", "<red>Cette case reprendra un jeton dans <temps>.", "temps", chrono(attente));
        }
        return validateur == null ? null : validateur.apply(joueur, objet);
    }

    private void poserDepuis(Player joueur, ItemStack objet, int k) {
        Fiche f = fiche(joueur.getUniqueId());
        ItemStack un = objet.clone();
        un.setAmount(1);
        f.localisations.put(k, un);
        f.liberees.remove(k);
        getLogger().info(joueur.getName() + " pose un jeton de localisation dans la case " + (k + 1) + ".");
        joueur.sendMessage(t("coffre.loc-pose", "<green>Jeton posé dans la case <n> : sa magnétite est une destination "
                + "de /home.", "n", k + 1));
    }

    /** Ce qu'on reprend d'une case : sur le curseur (vide), ou dans l'inventaire (Maj, ou curseur occupé). */
    private static void reprendre(Player joueur, ItemStack objet, boolean versInventaire) {
        if (versInventaire) {
            donner(joueur, objet);
        } else {
            joueur.setItemOnCursor(objet);
        }
    }

    /** Message dans le tchat (la barre d'action est cachée par le coffre), au plus un toutes les 2 secondes. */
    private void refuser(Player joueur, Component message) {
        long maintenant = System.currentTimeMillis();
        Long dernier = refus.put(joueur.getUniqueId(), maintenant);
        if (dernier != null && maintenant - dernier < 2000) {
            return;
        }
        joueur.sendMessage(message);
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
    }

    private static void donner(Player joueur, ItemStack item) {
        if (item == null || item.isEmpty()) {
            return;
        }
        for (ItemStack tombe : joueur.getInventory().addItem(item).values()) {
            joueur.getWorld().dropItemNaturally(joueur.getLocation(), tombe);
        }
    }

    /** Objets en attente pour ce joueur (reprise de la 2.0) : dans son inventaire, au sol s'il est plein. */
    private void donnerARendre(Player joueur) {
        Fiche f = fiches.get(CompteTest.de(joueur.getUniqueId()));
        if (f == null || f.aRendre.isEmpty()) {
            return;
        }
        int n = f.aRendre.size();
        f.aRendre.forEach(objet -> donner(joueur, objet));
        f.aRendre.clear();
        joueur.sendMessage(t("special.rendus", "<yellow><n> objet(s) de ton inventaire spécial t'ont été rendus (ils "
                + "n'avaient plus de case).", "n", n));
        sauverBientot();
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ commande et achat

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    private void quantite(Player joueur, Type type) {
        ActionButton valider = gui.form(t("special.valider", "<green>Valider"), null, (p, vue) -> {
            long n;
            try {
                n = Long.parseLong(vue.getText("nombre") == null ? "" : vue.getText("nombre").trim());
            } catch (NumberFormatException e) {
                n = 0;
            }
            if (n < 1 || n > 64) {
                gui.notice(p, titre(), t("special.nombre-invalide", "<red>Nombre invalide (1 à 64)."),
                        q -> quantite(q, type));
                lang.saveIfNeeded();
                return;
            }
            acheter(p, type, n);
        });
        gui.open(joueur, t("special.titre-acheter", "<green><bold>Acheter : <nom>", "nom", type.nom),
                List.of(t("special.prix", "<gray>Prix : <white><prix></white> par jeton.", "prix", points(prix(type)))),
                List.of(gui.text("nombre", t("special.champ-nombre", "Nombre de jetons"), "1", 2)),
                List.of(valider, gui.button(t("special.retour", "<gray>Retour"), null, this::ouvrir)), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void acheter(Player joueur, Type type, long n) {
        long prix = prix(type);
        if (prix <= 0 || !economie()) {
            ouvrir(joueur);
            return;
        }
        long total = Math.multiplyExact(prix, n);
        if (!fr.kalium.economy.KSEconomy.debiter(joueur.getUniqueId(), total)) {
            gui.notice(joueur, titre(), t("special.solde", "<red>Il te faut <prix>.", "prix", points(total)), this::ouvrir);
            lang.saveIfNeeded();
            return;
        }
        ajouter(joueur.getUniqueId(), type, n);
        getLogger().info(joueur.getName() + " achète " + n + " " + type.nom + " pour " + total + " points.");
        gui.notice(joueur, titre(), t("special.achete", "<green><n> <nom> acheté(s).", "n", n, "nom", type.nom),
                this::ouvrir);
        lang.saveIfNeeded();
    }

    /** Un jeton ou un badge ne s'utilise pas comme un livre de connaissances. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && (type(event.getItem()) != null || badge(event.getItem()) != null)) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    static String points(long n) {
        String brut = Long.toString(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < brut.length(); i++) {
            if (i > 0 && (brut.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(brut.charAt(i));
        }
        return sb + (n > 1 ? " points" : " point");
    }

    // ------------------------------------------------------------------ fusion des badges à l'enclume

    /** Coût en niveaux pour obtenir un badge de ce niveau (config.yml : fusion-niveaux ; ensuite + 20 par niveau). */
    private int coutFusion(int niveauObtenu) {
        List<Integer> liste = getConfig().getIntegerList("fusion-niveaux");
        if (liste.isEmpty()) {
            liste = FUSION_DEFAUT;
        }
        int i = niveauObtenu - 2;
        int cout = i < liste.size() ? liste.get(i) : liste.get(liste.size() - 1) + PAS_FUSION * (i - liste.size() + 1);
        return Math.max(1, cout);
    }

    /** Deux badges du même type et du même niveau, sous le niveau maximal ; null sinon. */
    private Fusion fusion(ItemStack premier, ItemStack second) {
        Badge badge = badge(premier);
        if (badge == null || badge != badge(second) || premier.getAmount() != 1 || second.getAmount() != 1) {
            return null;
        }
        int niveau = niveau(premier);
        if (niveau != niveau(second) || niveau >= niveauMax(badge)) {
            return null;
        }
        return new Fusion(badge, niveau + 1, coutFusion(niveau + 1));
    }

    private static boolean estNotre(ItemStack item) {
        return type(item) != null || badge(item) != null;
    }

    private static boolean assez(Player joueur, int cout) {
        return joueur.getLevel() >= cout || joueur.getGameMode() == GameMode.CREATIVE;
    }

    /**
     * Enclume avec un jeton ou un badge : seul résultat possible, la fusion de deux badges (ni renommage ni autre
     * mélange). Priorité HIGH, comme KS_FioleExp : après la réduction de KS_Enclume (NORMAL). Le coût laissé à
     * l'enclume ne dépasse pas 39 : KS_Enclume (HIGHEST) ne touche donc pas à ce résultat, le vrai coût est écrit en
     * dernière ligne de l'aperçu. L'enclume vanilla annule le coût après le calcul (deux livres ne se combinent pas) :
     * il est renvoyé au tick suivant.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnclume(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        if (!estNotre(inv.getFirstItem()) && !estNotre(inv.getSecondItem())) {
            return;
        }
        AnvilView view = event.getView();
        Fusion fusion = fusion(inv.getFirstItem(), inv.getSecondItem());
        if (fusion == null || !(view.getPlayer() instanceof Player joueur)) {
            event.setResult(null);
            return;
        }
        boolean assez = assez(joueur, fusion.cout());
        ItemStack apercu = creerBadge(fusion.badge(), fusion.niveau());
        ItemMeta meta = apercu.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.lore());
        lore.add(texte("Coût de la fusion : " + fusion.cout() + " niveaux", assez ? NamedTextColor.GREEN
                : NamedTextColor.RED));
        meta.lore(lore);
        apercu.setItemMeta(meta);
        event.setResult(apercu);
        view.setRepairCost(Math.min(fusion.cout(), COUT_AFFICHE_MAX));
        // Assez de niveaux : 39 au plus à l'écran (au-delà le jeu écrit « Trop cher ! ») ; sinon le vrai coût.
        int affiche = assez ? Math.min(fusion.cout(), COUT_AFFICHE_MAX) : fusion.cout();
        getServer().getScheduler().runTask(this, () -> {
            if (joueur.getOpenInventory().getTopInventory().equals(inv)
                    && fusion.equals(fusion(inv.getFirstItem(), inv.getSecondItem()))) {
                view.setRepairCost(affiche);
                joueur.updateInventory();
            }
        });
    }

    /**
     * Prise du résultat d'une fusion, faite par le plugin (l'enclume vanilla refuserait : pour elle, deux livres ne se combinent pas) : niveaux
     * retirés, les deux badges consommés, badge du niveau suivant donné.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPriseFusion(InventoryClickEvent event) {
        if (!(event.getView() instanceof AnvilView view) || event.getRawSlot() != CASE_RESULTAT
                || !(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        AnvilInventory inv = (AnvilInventory) view.getTopInventory();
        if (!estNotre(inv.getFirstItem()) && !estNotre(inv.getSecondItem())) {
            return;
        }
        event.setCancelled(true);
        Fusion fusion = fusion(inv.getFirstItem(), inv.getSecondItem());
        if (fusion == null) {
            return;
        }
        // Pas assez de niveaux : la ligne du coût est déjà en rouge dans l'aperçu.
        if (!assez(joueur, fusion.cout())) {
            return;
        }
        ItemStack donne = creerBadge(fusion.badge(), fusion.niveau());
        ClickType clic = event.getClick();
        if (clic.isShiftClick()) {
            if (joueur.getInventory().firstEmpty() < 0) {
                return;
            }
            joueur.getInventory().addItem(donne);
        } else if ((clic == ClickType.LEFT || clic == ClickType.RIGHT)
                && (view.getCursor() == null || view.getCursor().isEmpty())) {
            view.setCursor(donne);
        } else {
            return;
        }
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            joueur.giveExpLevels(-fusion.cout());
        }
        inv.setFirstItem(null);
        inv.setSecondItem(null);
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        getLogger().info(joueur.getName() + " fusionne deux badges : " + fusion.badge().nom + " niveau "
                + fusion.niveau() + " pour " + fusion.cout() + " niveaux.");
        getServer().getScheduler().runTask(this, joueur::updateInventory);
    }

    // ------------------------------------------------------------------ enregistrement

    private static String b64(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private ItemStack objetDe(String texte, String ou) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(texte == null ? "" : texte));
        } catch (RuntimeException e) {
            getLogger().warning("Objet illisible dans jetons.yml : " + ou);
            return null;
        }
    }

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        if (yaml.getInt("version", 1) < 2 && (yaml.contains("stocks") || yaml.contains("sacs") || yaml.contains("attente"))) {
            reprendreAnciens(yaml);
            return;
        }
        ConfigurationSection joueurs = yaml.getConfigurationSection("joueurs");
        if (joueurs == null) {
            return;
        }
        for (String cle : joueurs.getKeys(false)) {
            ConfigurationSection s = joueurs.getConfigurationSection(cle);
            UUID id;
            try {
                id = UUID.fromString(cle);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Inventaire spécial ignoré : " + cle);
                continue;
            }
            if (s == null) {
                continue;
            }
            Fiche f = new Fiche();
            ConfigurationSection badges = s.getConfigurationSection("badges");
            if (badges != null) {
                for (String b : badges.getKeys(false)) {
                    Badge badge = Badge.parId(b);
                    if (badge != null && badges.getInt(b) > 0) {
                        f.badges.put(badge, badges.getInt(b));
                    }
                }
            }
            ConfigurationSection jetons = s.getConfigurationSection("jetons");
            if (jetons != null) {
                for (String j : jetons.getKeys(false)) {
                    Type type = Type.parId(j);
                    if (type != null && type != Type.LOCALISATION && jetons.getLong(j) > 0) {
                        f.jetons.put(type, jetons.getLong(j));
                    }
                }
            }
            ConfigurationSection localisations = s.getConfigurationSection("localisations");
            if (localisations != null) {
                for (String k : localisations.getKeys(false)) {
                    ItemStack jeton = objetDe(localisations.getString(k), cle + ".localisations." + k);
                    try {
                        if (jeton != null && type(jeton) == Type.LOCALISATION) {
                            f.localisations.put(Integer.parseInt(k), jeton);
                        }
                    } catch (NumberFormatException e) {
                        if (jeton != null) {
                            f.aRendre.add(jeton);
                        }
                    }
                }
            }
            ConfigurationSection liberees = s.getConfigurationSection("liberees");
            if (liberees != null) {
                for (String k : liberees.getKeys(false)) {
                    try {
                        f.liberees.put(Integer.parseInt(k), liberees.getLong(k));
                    } catch (NumberFormatException ignore) {
                        // case illisible : oubliée
                    }
                }
            }
            for (String texte : s.getStringList("a-rendre")) {
                ItemStack objet = objetDe(texte, cle + ".a-rendre");
                if (objet != null) {
                    f.aRendre.add(objet);
                }
            }
            // 2.3.0 : gardés même si leur plugin n'est pas là (ils n'ont alors pas de case).
            ConfigurationSection equipements = s.getConfigurationSection("equipements");
            if (equipements != null) {
                for (String e : equipements.getKeys(false)) {
                    ItemStack objet = objetDe(equipements.getString(e), cle + ".equipements." + e);
                    if (objet != null) {
                        f.equipements.put(e, objet);
                    }
                }
            }
            fiches.put(id, f);
        }
    }

    /**
     * 2.1.0 : reprise du jetons.yml de la 2.0 (deux contenants d'entonnoir par joueur, jetons en attente) et de la 1.0.0
     * (nombres) : les badges vont dans leur case (un deuxième du même type est rendu au joueur), les jetons dans leur
     * case, les jetons de localisation non utilisés sont rendus au joueur. L'ancien fichier est gardé (jetons-2.0.yml).
     */
    private void reprendreAnciens(YamlConfiguration yaml) {
        try {
            File copie = new File(getDataFolder(), yaml.contains("stocks") ? "jetons-1.0.0.yml" : "jetons-2.0.yml");
            if (!copie.exists()) {
                Files.copy(fichier.toPath(), copie.toPath());
            }
        } catch (IOException e) {
            getLogger().warning("Copie de l'ancien jetons.yml impossible : " + e.getMessage());
        }
        for (String section : List.of("stocks", "attente")) {
            ConfigurationSection nombres = yaml.getConfigurationSection(section);
            if (nombres == null) {
                continue;
            }
            for (String cle : nombres.getKeys(false)) {
                ConfigurationSection js = nombres.getConfigurationSection(cle);
                try {
                    UUID id = UUID.fromString(cle);
                    if (js == null) {
                        continue;
                    }
                    for (String t : js.getKeys(false)) {
                        Type type = Type.parId(t);
                        if (type != null && type != Type.LOCALISATION && js.getLong(t) > 0) {
                            fiche(id).jetons.merge(type, js.getLong(t), Long::sum);
                        } else if (js.getLong(t) > 0) {
                            getLogger().warning("Jetons non repris (type supprimé) : " + js.getLong(t) + " x " + t
                                    + " de " + cle + " (gardés dans l'ancien fichier).");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Jetons ignorés : " + cle);
                }
            }
        }
        int rendus = 0;
        ConfigurationSection sacs = yaml.getConfigurationSection("sacs");
        if (sacs != null) {
            for (String cle : sacs.getKeys(false)) {
                UUID id;
                try {
                    id = UUID.fromString(cle);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Inventaire spécial ignoré : " + cle);
                    continue;
                }
                Fiche f = fiche(id);
                for (String partie : List.of("jetons", "badges")) {
                    ConfigurationSection sac = sacs.getConfigurationSection(cle + "." + partie);
                    if (sac == null) {
                        continue;
                    }
                    for (String place : sac.getKeys(false)) {
                        ItemStack objet = objetDe(sac.getString(place), cle + "." + partie + "." + place);
                        if (objet == null) {
                            continue;
                        }
                        Badge badge = badge(objet);
                        Type type = type(objet);
                        if (badge != null && !f.badges.containsKey(badge)) {
                            f.badges.put(badge, niveau(objet));
                        } else if (type != null && type != Type.LOCALISATION) {
                            f.jetons.merge(type, (long) objet.getAmount(), Long::sum);
                        } else {
                            f.aRendre.add(objet);
                            rendus++;
                        }
                    }
                }
            }
        }
        getLogger().info("jetons.yml repris en 2.1.0 : " + fiches.size() + " joueur(s), " + rendus
                + " objet(s) à rendre à leur joueur (jetons de localisation, badges en double).");
        sauver();
    }

    /** Enregistre au tick suivant (une seule fois, même après plusieurs changements). */
    private void sauverBientot() {
        if (aSauver) {
            return;
        }
        if (!isEnabled()) {
            sauver();
            return;
        }
        aSauver = true;
        getServer().getScheduler().runTask(this, () -> {
            aSauver = false;
            sauver();
        });
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("version", 2);
        long limite = System.currentTimeMillis() - delaiLocalisation();
        fiches.forEach((u, f) -> {
            if (f.vide()) {
                return;
            }
            String chemin = "joueurs." + u + ".";
            f.badges.forEach((b, n) -> yaml.set(chemin + "badges." + b.id, n));
            f.jetons.forEach((t, n) -> {
                if (n > 0) {
                    yaml.set(chemin + "jetons." + t.id, n);
                }
            });
            f.localisations.forEach((k, jeton) -> yaml.set(chemin + "localisations." + k, b64(jeton)));
            f.liberees.forEach((k, date) -> {
                if (date > limite) {
                    yaml.set(chemin + "liberees." + k, date);
                }
            });
            if (!f.aRendre.isEmpty()) {
                yaml.set(chemin + "a-rendre", f.aRendre.stream().map(KSJetons::b64).toList());
            }
            f.equipements.forEach((e, objet) -> yaml.set(chemin + "equipements." + e, b64(objet)));
        });
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer jetons.yml : " + e.getMessage());
        }
    }
}
