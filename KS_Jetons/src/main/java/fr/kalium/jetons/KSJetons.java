package fr.kalium.jetons;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * KS_Jetons (cahier des charges : catégorie 7 « Déplacements, jetons, coffres de mort », LeKiwi06, 03/10/2026) :
 * serveur Event.
 *
 * - Jetons : de téléportation, d'emplacement, de claim, de la mort (objets : livre de connaissances inutilisable, avec
 *   l'apparence d'un objet vanilla ; empilables par 64). Gagnés via /rewards (id_custom de KS_KaliumGive) ou achetés
 *   avec le score.
 * - **Inventaire de jetons** (« faire un onglet inventaire spécial pour les jetons, histoire de les y stocker et les en
 *   retirer quand on veut ») : bouton « Jetons » du menu d'Event et /jetons : nombre de chaque jeton, déposer (coffre :
 *   les jetons y entrent, le reste est rendu), retirer, acheter (prix du config.yml ; 0 : pas encore en vente).
 * - Les autres plugins (KS_Teleport, KS_CoffreMort, KS_Claim) consomment les jetons de l'inventaire de jetons :
 *   nombre(), consommer(), ajouter(), prix().
 */
public final class KSJetons extends JavaPlugin implements Listener {

    /** Les jetons : id, nom, apparence (modèle vanilla). */
    public enum Type {
        TP("tp", "Jeton de téléportation", "ender_eye"),
        EMPLACEMENT("emplacement", "Jeton d'emplacement", "filled_map"),
        CLAIM("claim", "Jeton de claim", "golden_shovel"),
        MORT("mort", "Jeton de la mort", "skeleton_skull");

        final String id;
        final String nom;
        final String modele;

        Type(String id, String nom, String modele) {
            this.id = id;
            this.nom = nom;
            this.modele = modele;
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

    /** Coffre de dépôt des jetons. */
    private record Depot(UUID joueur) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static KSJetons instance;
    private static NamespacedKey marqueur;

    private Lang lang;
    private Gui gui;
    private File fichier;
    private final Map<UUID, EnumMap<Type, Long>> stocks = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        marqueur = new NamespacedKey(this, "jeton");
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "jetons.yml");
        charger();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("jetons").setExecutor(this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "jetons", lang.c("bouton.nom", "<aqua><bold>Jetons"),
                    lang.c("bouton.description", "<gray>Tes jetons : déposer, retirer, acheter"), 40, this::ouvrir);
        }
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        sauver();
    }

    // ------------------------------------------------------------------ API (autres plugins)

    /** Un jeton (objet). */
    public static ItemStack creer(Type type) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.nom, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("À déposer dans tes jetons : /jetons", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setItemModel(NamespacedKey.minecraft(type.modele));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.STRING, type.id);
        item.setItemMeta(meta);
        return item;
    }

    /** Un jeton par son id (tp, emplacement, claim, mort), ou null (pour KS_KaliumGive). */
    public static ItemStack creer(String id) {
        Type t = Type.parId(id);
        return t == null || instance == null || !instance.isEnabled() ? null : creer(t);
    }

    /** Type du jeton, ou null si ce n'est pas un jeton. */
    public static Type type(ItemStack item) {
        if (item == null || marqueur == null || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(marqueur, PersistentDataType.STRING);
        return id == null ? null : Type.parId(id);
    }

    /** Jetons de ce type dans l'inventaire de jetons du joueur. */
    public static long nombre(UUID joueur, Type type) {
        return instance.stocks.getOrDefault(joueur, new EnumMap<>(Type.class)).getOrDefault(type, 0L);
    }

    /** Retire n jetons de l'inventaire de jetons ; faux (rien n'est retiré) s'il n'en a pas assez. */
    public static boolean consommer(UUID joueur, Type type, long n) {
        if (n < 0 || nombre(joueur, type) < n) {
            return false;
        }
        instance.stocks.computeIfAbsent(joueur, u -> new EnumMap<>(Type.class)).merge(type, -n, Long::sum);
        instance.sauver();
        return true;
    }

    /** Ajoute n jetons à l'inventaire de jetons. */
    public static void ajouter(UUID joueur, Type type, long n) {
        if (n <= 0) {
            return;
        }
        instance.stocks.computeIfAbsent(joueur, u -> new EnumMap<>(Type.class)).merge(type, n, Long::sum);
        instance.sauver();
    }

    /** Prix d'un jeton en points (config.yml) ; 0 : pas en vente. */
    public static long prix(Type type) {
        return Math.max(0, instance.getConfig().getLong("prix." + type.id, 0));
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

    /** Inventaire de jetons. */
    public void ouvrir(Player joueur) {
        List<Component> corps = new ArrayList<>();
        corps.add(t("menu.aide", "<gray>Tes jetons sont rangés ici : ils servent directement (téléportations, claims, "
                + "coffres de mort). Dépose ceux que tu as en objets, ou retire-en pour les donner."));
        List<ActionButton> boutons = new ArrayList<>();
        for (Type type : Type.values()) {
            long n = nombre(joueur.getUniqueId(), type);
            long prix = prix(type);
            corps.add(t("menu.ligne", "<aqua><nom></aqua> <gray>: <white><n></white><prix>", "nom", type.nom, "n", n,
                    "prix", prix > 0 ? " (achat : " + points(prix) + ")" : ""));
            if (n > 0) {
                boutons.add(gui.button(t("menu.bouton-retirer", "<white>Retirer : <nom>", "nom", type.nom), null,
                        p -> quantite(p, type, false)));
            }
            if (prix > 0 && Bukkit.getPluginManager().isPluginEnabled("KS_Economy")) {
                boutons.add(gui.button(t("menu.bouton-acheter", "<green>Acheter : <nom>", "nom", type.nom), null,
                        p -> quantite(p, type, true)));
            }
        }
        boutons.add(0, gui.button(t("menu.bouton-deposer", "<aqua>Déposer des jetons"),
                t("menu.deposer-info", "<gray>Coffre : les jetons y entrent, le reste t'est rendu"), this::deposer));
        gui.open(joueur, t("menu.titre", "<aqua><bold>Jetons"), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void quantite(Player joueur, Type type, boolean achat) {
        ActionButton valider = gui.form(t("menu.valider", "<green>Valider"), null, (p, vue) -> {
            long n;
            try {
                n = Long.parseLong(vue.getText("nombre") == null ? "" : vue.getText("nombre").trim());
            } catch (NumberFormatException e) {
                n = 0;
            }
            if (n < 1 || n > 640) {
                gui.notice(p, t("menu.titre", "<aqua><bold>Jetons"), t("menu.nombre-invalide",
                        "<red>Nombre invalide (1 à 640)."), q -> quantite(q, type, achat));
                lang.saveIfNeeded();
                return;
            }
            if (achat) {
                acheter(p, type, n);
            } else {
                retirer(p, type, n);
            }
        });
        gui.open(joueur, achat ? t("menu.titre-acheter", "<green><bold>Acheter : <nom>", "nom", type.nom)
                        : t("menu.titre-retirer", "<white><bold>Retirer : <nom>", "nom", type.nom),
                achat ? List.of(t("menu.prix", "<gray>Prix : <white><prix></white> par jeton.", "prix", points(prix(type))))
                        : List.of(t("menu.stock", "<gray>Tu en as <white><n></white>.", "n", nombre(joueur.getUniqueId(), type))),
                List.of(gui.text("nombre", t("menu.champ-nombre", "Nombre de jetons"), "1", 3)),
                List.of(valider, gui.button(t("menu.retour", "<gray>Retour"), null, this::ouvrir)), gui.close(), 1);
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
            gui.notice(joueur, t("menu.titre", "<aqua><bold>Jetons"), t("menu.solde", "<red>Il te faut <prix>.", "prix",
                    points(total)), this::ouvrir);
            lang.saveIfNeeded();
            return;
        }
        ajouter(joueur.getUniqueId(), type, n);
        getLogger().info(joueur.getName() + " achète " + n + " " + type.nom + " pour " + total + " points.");
        gui.notice(joueur, t("menu.titre", "<aqua><bold>Jetons"), t("menu.achete", "<green><n> <nom> acheté(s).", "n", n,
                "nom", type.nom), this::ouvrir);
        lang.saveIfNeeded();
    }

    private void retirer(Player joueur, Type type, long n) {
        if (!consommer(joueur.getUniqueId(), type, n)) {
            ouvrir(joueur);
            return;
        }
        long reste = n;
        while (reste > 0) {
            ItemStack pile = creer(type);
            int q = (int) Math.min(64, reste);
            pile.setAmount(q);
            for (ItemStack tombe : joueur.getInventory().addItem(pile).values()) {
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), tombe);
            }
            reste -= q;
        }
        ouvrir(joueur);
    }

    private void deposer(Player joueur) {
        Inventory coffre = Bukkit.createInventory(new Depot(joueur.getUniqueId()), 27,
                t("menu.titre-depot", "Dépose tes jetons"));
        joueur.openInventory(coffre);
    }

    @EventHandler
    public void onCloseDepot(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Depot) || !(event.getPlayer() instanceof Player joueur)) {
            return;
        }
        int deposes = 0;
        for (ItemStack item : event.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Type type = type(item);
            if (type != null) {
                ajouter(joueur.getUniqueId(), type, item.getAmount());
                deposes += item.getAmount();
            } else {
                for (ItemStack tombe : joueur.getInventory().addItem(item).values()) {
                    joueur.getWorld().dropItemNaturally(joueur.getLocation(), tombe);
                }
            }
        }
        event.getInventory().clear();
        if (deposes > 0) {
            joueur.sendMessage(t("menu.deposes", "<aqua><n> jeton(s) déposé(s). <gray>/jetons", "n", deposes));
            lang.saveIfNeeded();
        }
    }

    /** Un jeton ne s'utilise pas comme un livre de connaissances. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && type(event.getItem()) != null) {
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

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection s = yaml.getConfigurationSection("stocks");
        if (s == null) {
            return;
        }
        for (String cle : s.getKeys(false)) {
            try {
                UUID u = UUID.fromString(cle);
                EnumMap<Type, Long> m = new EnumMap<>(Type.class);
                ConfigurationSection js = s.getConfigurationSection(cle);
                for (String t : js.getKeys(false)) {
                    Type type = Type.parId(t);
                    if (type != null) {
                        m.put(type, js.getLong(t));
                    }
                }
                stocks.put(u, m);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Jetons ignorés : " + cle);
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        stocks.forEach((u, m) -> m.forEach((t, n) -> {
            if (n > 0) {
                yaml.set("stocks." + u + "." + t.id.toLowerCase(Locale.ROOT), n);
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
