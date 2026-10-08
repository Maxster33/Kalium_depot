package fr.kalium.jetons;

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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_Jetons (cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort », LeKiwi06, 03/10/2026 ; ajouts
 * « jetons et badges » du 08/10/2026) : serveur Event.
 *
 * - Jetons : de fly, de la mort, de téléportation, de localisation (apparence : lingot de fer, de netherite, d'or, de
 *   cuivre) et de claim ; livres de connaissances inutilisables, empilables par 64.
 * - Badges (2.0.0) : « versions améliorées des jetons », de fly, de la mort, de téléportation, de localisation
 *   (apparence : bloc du lingot du jeton) ; un niveau ; « ils s'améliorent en fusionnant 2 badges du même niveau dans
 *   une enclume » (coût en niveaux : 30, 50, puis + 20 par niveau, réglable).
 * - **Inventaire spécial** (2.0.0 : « l'interface des hopper », un contenant pour les jetons, un pour les badges,
 *   « cases excédentes » bloquées par une barrière) : bouton « Inventaire spécial » du menu et /jetons. Un badge est
 *   porté quand il est rangé dans le contenant des badges ; jamais 2 badges du même type.
 * - Les autres plugins (KS_Teleport, KS_CoffreMort, KS_Fly, KS_Claim) lisent l'inventaire spécial : nombre(),
 *   consommer(), ajouter(), prix(), niveauBadge(), valeurBadge(). Les délais des badges sont tenus par ces plugins, par
 *   joueur (et non par badge).
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
        /** Emplacements de localisation. PROVISOIRE : valeurs à fixer par LeKiwi06. */
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
                case LOCALISATION -> v + (v > 1 ? " emplacements" : " emplacement") + " de localisation";
            };
        }
    }

    /** Contenant de l'inventaire spécial (interface d'entonnoir) : jetons ou badges d'un joueur. */
    private record Sac(UUID joueur, boolean badges) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
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

    private static KSJetons instance;
    private static NamespacedKey marqueur;
    private static NamespacedKey cleBadge;
    private static NamespacedKey cleNiveau;
    private static NamespacedKey cleVerrou;

    private Lang lang;
    private Gui gui;
    private File fichier;
    private final Map<UUID, Inventory> sacsJetons = new HashMap<>();
    private final Map<UUID, Inventory> sacsBadges = new HashMap<>();
    /** Jetons qui n'ont pas trouvé de place dans le contenant des jetons : ils comptent et y descendent dès que possible. */
    private final Map<UUID, EnumMap<Type, Long>> attente = new HashMap<>();
    private boolean aSauver;
    /** Dernier message de refus envoyé à chaque joueur (millisecondes). */
    private final Map<UUID, Long> refus = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        marqueur = new NamespacedKey(this, "jeton");
        cleBadge = new NamespacedKey(this, "badge");
        cleNiveau = new NamespacedKey(this, "niveau");
        cleVerrou = new NamespacedKey(this, "verrou");
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
            if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Sac) {
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
        meta.lore(List.of(texte(type.effet, NamedTextColor.GRAY),
                texte("À ranger dans tes jetons : /jetons", NamedTextColor.DARK_GRAY)));
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

    private static ItemStack verrou() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte("Case bloquée", NamedTextColor.RED));
        meta.getPersistentDataContainer().set(cleVerrou, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean estVerrou(ItemStack item) {
        return item != null && !item.isEmpty() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleVerrou, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------------ API (autres plugins)

    /** Jetons de ce type dans l'inventaire spécial du joueur. */
    public static long nombre(UUID joueur, Type type) {
        EnumMap<Type, Long> m = instance.attente.get(joueur);
        long n = m == null ? 0 : m.getOrDefault(type, 0L);
        for (ItemStack item : instance.sac(joueur, false).getContents()) {
            if (type(item) == type) {
                n += item.getAmount();
            }
        }
        return n;
    }

    /** Retire n jetons de l'inventaire spécial ; faux (rien n'est retiré) s'il n'en a pas assez. */
    public static boolean consommer(UUID joueur, Type type, long n) {
        if (n < 0 || nombre(joueur, type) < n) {
            return false;
        }
        long reste = n;
        EnumMap<Type, Long> m = instance.attente.get(joueur);
        if (m != null) {
            long pris = Math.min(m.getOrDefault(type, 0L), reste);
            m.merge(type, -pris, Long::sum);
            reste -= pris;
        }
        Inventory sac = instance.sac(joueur, false);
        for (int i = 0; i < sac.getSize() && reste > 0; i++) {
            ItemStack item = sac.getItem(i);
            if (type(item) != type) {
                continue;
            }
            int pris = (int) Math.min(item.getAmount(), reste);
            if (pris == item.getAmount()) {
                sac.setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - pris);
                sac.setItem(i, item);
            }
            reste -= pris;
        }
        instance.remplir(joueur);
        instance.sauverBientot();
        return true;
    }

    /** Ajoute n jetons à l'inventaire spécial (contenant plein : ils attendent une place, et comptent déjà). */
    public static void ajouter(UUID joueur, Type type, long n) {
        if (n <= 0) {
            return;
        }
        instance.attente.computeIfAbsent(joueur, u -> new EnumMap<>(Type.class)).merge(type, n, Long::sum);
        instance.remplir(joueur);
        instance.sauverBientot();
    }

    /** Prix d'un jeton en points (config.yml) ; 0 : pas en vente (toujours 0 pour un jeton qui ne s'achète pas). */
    public static long prix(Type type) {
        return type.achetable ? Math.max(0, instance.getConfig().getLong("prix." + type.id, 0)) : 0;
    }

    /** Niveau du badge de ce type porté par le joueur (rangé dans son inventaire spécial) ; 0 : aucun. */
    public static int niveauBadge(UUID joueur, Badge badge) {
        for (ItemStack item : instance.sac(joueur, true).getContents()) {
            if (badge(item) == badge) {
                return Math.min(niveau(item), niveauMax(badge));
            }
        }
        return 0;
    }

    /** Valeur du badge porté par le joueur ; 0 s'il n'en porte pas. */
    public static int valeurBadge(UUID joueur, Badge badge) {
        return valeurBadge(badge, niveauBadge(joueur, badge));
    }

    // ------------------------------------------------------------------ contenants

    /** Cases utilisables d'un contenant ; les suivantes sont bloquées. */
    private static int cases(boolean badges) {
        return badges ? Math.min(5, Badge.values().length) : 5;
    }

    private Inventory sac(UUID joueur, boolean badges) {
        return (badges ? sacsBadges : sacsJetons).computeIfAbsent(joueur, u -> {
            Inventory inv = Bukkit.createInventory(new Sac(u, badges), InventoryType.HOPPER, badges
                    ? lang.c("special.titre-badges", "Tes badges") : lang.c("special.titre-jetons", "Tes jetons"));
            for (int i = cases(badges); i < inv.getSize(); i++) {
                inv.setItem(i, verrou());
            }
            return inv;
        });
    }

    /** Les jetons en attente descendent dans le contenant tant qu'il y a de la place. */
    private void remplir(UUID joueur) {
        EnumMap<Type, Long> m = attente.get(joueur);
        if (m == null) {
            return;
        }
        Inventory sac = sac(joueur, false);
        for (Type type : Type.values()) {
            long n = m.getOrDefault(type, 0L);
            while (n > 0) {
                ItemStack pile = creer(type);
                int q = (int) Math.min(pile.getMaxStackSize(), n);
                pile.setAmount(q);
                int refuses = 0;
                for (ItemStack reste : sac.addItem(pile).values()) {
                    refuses += reste.getAmount();
                }
                n -= q - refuses;
                if (refuses > 0) {
                    break;
                }
            }
            if (n > 0) {
                m.put(type, n);
            } else {
                m.remove(type);
            }
        }
        if (m.isEmpty()) {
            attente.remove(joueur);
        }
    }

    /**
     * L'objet peut-il entrer dans le contenant ? Jetons : tout jeton. Badges : un badge dont le type n'est pas déjà
     * dans une autre case (caseVisee : case où il est posé, -1 si inconnue).
     */
    private boolean accepte(Sac sac, Inventory inv, ItemStack item, int caseVisee) {
        if (!sac.badges()) {
            return type(item) != null;
        }
        Badge badge = badge(item);
        if (badge == null) {
            return false;
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != caseVisee && badge(inv.getItem(i)) == badge) {
                return false;
            }
        }
        return true;
    }

    /** Message dans le tchat (la barre d'action est cachée par le contenant), au plus un toutes les 2 secondes. */
    private void refuser(Player joueur, Sac sac, ItemStack item) {
        long maintenant = System.currentTimeMillis();
        Long dernier = refus.put(joueur.getUniqueId(), maintenant);
        if (dernier != null && maintenant - dernier < 2000) {
            return;
        }
        joueur.sendMessage(!sac.badges() ? t("special.refus-jeton", "<red>Seuls les jetons vont ici.")
                : badge(item) == null ? t("special.refus-badge", "<red>Seuls les badges vont ici.")
                : t("special.refus-double", "<red>Tu portes déjà un badge de ce type."));
        lang.saveIfNeeded();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClicSac(InventoryClickEvent event) {
        Inventory haut = event.getView().getTopInventory();
        if (!(haut.getHolder() instanceof Sac sac) || !(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        int brut = event.getRawSlot();
        boolean dansSac = brut >= 0 && brut < haut.getSize();
        if (dansSac && estVerrou(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }
        // Objet qui entrerait dans le contenant : curseur, touche de la barre, main secondaire, ou Maj + clic en bas.
        ItemStack entrant = null;
        if (dansSac) {
            if (event.getClick() == ClickType.NUMBER_KEY) {
                entrant = joueur.getInventory().getItem(event.getHotbarButton());
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                entrant = joueur.getInventory().getItemInOffHand();
            } else {
                entrant = event.getCursor();
            }
        } else if (event.isShiftClick()) {
            entrant = event.getCurrentItem();
        }
        if (entrant != null && !entrant.isEmpty() && !accepte(sac, haut, entrant, dansSac ? brut : -1)) {
            event.setCancelled(true);
            refuser(joueur, sac, entrant);
            return;
        }
        sauverBientot();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlisserSac(InventoryDragEvent event) {
        Inventory haut = event.getView().getTopInventory();
        if (!(haut.getHolder() instanceof Sac sac) || !(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        for (int brut : event.getRawSlots()) {
            if (brut < haut.getSize() && !accepte(sac, haut, event.getOldCursor(), -1)) {
                event.setCancelled(true);
                refuser(joueur, sac, event.getOldCursor());
                return;
            }
        }
        sauverBientot();
    }

    /** Fermeture : ce qui n'a rien à faire dans le contenant est rendu au joueur (filet de sécurité), puis on enregistre. */
    @EventHandler
    public void onFermerSac(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof Sac sac) || !(event.getPlayer() instanceof Player joueur)) {
            return;
        }
        EnumSet<Badge> portes = EnumSet.noneOf(Badge.class);
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (i >= cases(sac.badges())) {
                if (!estVerrou(item)) {
                    rendre(joueur, item);
                    inv.setItem(i, verrou());
                }
                continue;
            }
            if (item == null || item.isEmpty()) {
                continue;
            }
            Badge badge = badge(item);
            if (sac.badges() ? badge == null || !portes.add(badge) : type(item) == null) {
                rendre(joueur, item);
                inv.setItem(i, null);
            } else if (sac.badges()) {
                // Description remise à jour (valeurs du config.yml).
                inv.setItem(i, creerBadge(badge, niveau(item)));
            }
        }
        if (!sac.badges()) {
            remplir(sac.joueur());
        }
        sauverBientot();
    }

    private static void rendre(Player joueur, ItemStack item) {
        if (item == null || item.isEmpty() || estVerrou(item)) {
            return;
        }
        for (ItemStack tombe : joueur.getInventory().addItem(item).values()) {
            joueur.getWorld().dropItemNaturally(joueur.getLocation(), tombe);
        }
    }

    // ------------------------------------------------------------------ menu

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private Component titre() {
        return t("special.titre", "<aqua><bold>Inventaire spécial");
    }

    /** Inventaire spécial : ce que le joueur a, et l'accès aux deux contenants. */
    public void ouvrir(Player joueur) {
        UUID id = joueur.getUniqueId();
        remplir(id);
        List<Component> corps = new ArrayList<>();
        corps.add(t("special.aide", "<gray>Les jetons et les badges rangés ici servent directement. Un badge est porté "
                + "tant qu'il est rangé dans tes badges, un seul par type."));
        for (Type type : Type.values()) {
            EnumMap<Type, Long> m = attente.get(id);
            long enAttente = m == null ? 0 : m.getOrDefault(type, 0L);
            corps.add(t("special.ligne-jeton", "<aqua><nom></aqua> <gray>: <white><n></white><attente>", "nom", type.nom,
                    "n", nombre(id, type), "attente", enAttente > 0 ? " (dont " + enAttente + " en attente de place)" : ""));
        }
        for (Badge badge : Badge.values()) {
            int niveau = niveauBadge(id, badge);
            corps.add(niveau == 0
                    ? t("special.ligne-badge-aucun", "<gold><nom></gold> <gray>: aucun", "nom", badge.nom)
                    : t("special.ligne-badge", "<gold><nom></gold> <gray>: <white>niveau <niveau></white> (<effet>)",
                    "nom", badge.nom, "niveau", niveau, "effet", badge.effet(valeurBadge(badge, niveau))));
        }
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("special.bouton-jetons", "<aqua>Mes jetons"),
                t("special.info-jetons", "<gray>Contenant : seuls les jetons y entrent"),
                p -> p.openInventory(sac(p.getUniqueId(), false))));
        boutons.add(gui.button(t("special.bouton-badges", "<gold>Mes badges"),
                t("special.info-badges", "<gray>Contenant : un badge de chaque type"),
                p -> p.openInventory(sac(p.getUniqueId(), true))));
        for (Type type : Type.values()) {
            long prix = prix(type);
            if (prix > 0 && Bukkit.getPluginManager().isPluginEnabled("KS_Economy")) {
                boutons.add(gui.button(t("special.bouton-acheter", "<green>Acheter : <nom>", "nom", type.nom),
                        t("special.info-acheter", "<gray><prix> par jeton", "prix", points(prix)),
                        p -> quantite(p, type)));
            }
        }
        gui.open(joueur, titre(), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
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
        if (prix <= 0 || !Bukkit.getPluginManager().isPluginEnabled("KS_Economy")) {
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

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection anciens = yaml.getConfigurationSection("stocks");
        if (anciens != null) {
            // 1.0.0 : nombres de jetons. Ils passent en attente et descendent dans le contenant du joueur. Le jeton
            // d'emplacement n'existe plus. Le fichier de la 1.0.0 est gardé à côté.
            try {
                File copie = new File(getDataFolder(), "jetons-1.0.0.yml");
                if (!copie.exists()) {
                    Files.copy(fichier.toPath(), copie.toPath());
                }
            } catch (IOException e) {
                getLogger().warning("Copie de jetons.yml (1.0.0) impossible : " + e.getMessage());
            }
            lireNombres(anciens, true);
        }
        ConfigurationSection enAttente = yaml.getConfigurationSection("attente");
        if (enAttente != null) {
            lireNombres(enAttente, false);
        }
        ConfigurationSection sacs = yaml.getConfigurationSection("sacs");
        if (sacs != null) {
            for (String cle : sacs.getKeys(false)) {
                try {
                    UUID joueur = UUID.fromString(cle);
                    lireSac(sacs.getConfigurationSection(cle + ".jetons"), sac(joueur, false), false);
                    lireSac(sacs.getConfigurationSection(cle + ".badges"), sac(joueur, true), true);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Inventaire spécial ignoré : " + cle + " (" + e.getMessage() + ")");
                }
            }
        }
        for (UUID joueur : new ArrayList<>(attente.keySet())) {
            remplir(joueur);
        }
        if (anciens != null) {
            sauver();
        }
    }

    private void lireNombres(ConfigurationSection section, boolean ancien) {
        for (String cle : section.getKeys(false)) {
            ConfigurationSection js = section.getConfigurationSection(cle);
            if (js == null) {
                continue;
            }
            try {
                UUID joueur = UUID.fromString(cle);
                for (String id : js.getKeys(false)) {
                    Type type = Type.parId(id);
                    long n = js.getLong(id);
                    if (type != null && n > 0) {
                        attente.computeIfAbsent(joueur, u -> new EnumMap<>(Type.class)).merge(type, n, Long::sum);
                    } else if (type == null && ancien) {
                        getLogger().warning("Jetons de la 1.0.0 non repris (type supprimé) : " + n + " x " + id
                                + " de " + cle + " (gardés dans jetons-1.0.0.yml).");
                    }
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Jetons ignorés : " + cle);
            }
        }
    }

    private void lireSac(ConfigurationSection section, Inventory inv, boolean badges) {
        if (section == null) {
            return;
        }
        for (String cle : section.getKeys(false)) {
            try {
                int i = Integer.parseInt(cle);
                if (i < 0 || i >= cases(badges)) {
                    continue;
                }
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(section.getString(cle, "")));
                if (badges ? badge(item) != null : type(item) != null) {
                    inv.setItem(i, item);
                }
            } catch (RuntimeException e) {
                getLogger().warning("Objet illisible dans jetons.yml : " + section.getCurrentPath() + "." + cle);
            }
        }
    }

    private void ecrireSac(YamlConfiguration yaml, String chemin, Inventory inv, boolean badges) {
        for (int i = 0; i < cases(badges); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.isEmpty() && !estVerrou(item)) {
                yaml.set(chemin + "." + i, Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
        }
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
        sacsJetons.forEach((u, inv) -> ecrireSac(yaml, "sacs." + u + ".jetons", inv, false));
        sacsBadges.forEach((u, inv) -> ecrireSac(yaml, "sacs." + u + ".badges", inv, true));
        attente.forEach((u, m) -> m.forEach((t, n) -> {
            if (n > 0) {
                yaml.set("attente." + u + "." + t.id, n);
            }
        }));
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer jetons.yml : " + e.getMessage());
        }
    }
}
