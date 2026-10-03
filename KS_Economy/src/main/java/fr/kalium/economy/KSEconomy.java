package fr.kalium.economy;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_Economy (cahier des charges : catégorie 2 « Économie », LeKiwi06, 29/09/2026), 1re partie : score (émeraudes
 * dématérialisées), blocs d'émeraude compressés, menu Économie, /echange, économie Vault. Les magasins (Tradeshop)
 * viendront après KS_Claim.
 *
 * - 1 émeraude = 1 point ; taux de conversion réglables (conversion.depot, conversion.retrait : 1 par défaut).
 * - Solde visible des autres dans la liste des joueurs (Tab) ; masqué, il coûte 1 % par jour (minuit, heure de Paris).
 * - Blocs compressés tier 1 à 6 (10^n blocs d'émeraude = 9 x 10^n points) : ni posables, ni décraftables.
 *
 * Autres plugins : solde(uuid), crediter(uuid, points), debiter(uuid, points), creerBloc(tier), tierBloc(objet)
 * (KS_Elixir : bloc tier 3 de l'Élixir de Fortune).
 */
public final class KSEconomy extends JavaPlugin implements Listener {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    /** Valeur en émeraudes d'un bloc compressé : tier n = 10^n blocs d'émeraude = 9 x 10^n émeraudes. */
    static final int TIER_MAX = 6;

    /** Un compte : solde en points, solde masqué ou non, dernier pseudo connu. */
    static final class Compte {
        long solde;
        boolean masque;
        String nom;
    }

    private static KSEconomy instance;
    private static NamespacedKey cleTier;

    private final Map<UUID, Compte> comptes = new HashMap<>();
    private File fichier;
    private LocalDate dernierPrelevement;
    private boolean aSauver;

    private Lang lang;
    private Gui gui;
    private MenuEconomie menu;
    private Echanges echanges;
    private Magasins magasins;
    private Boutiques boutiques;
    private MenuMagasin menuMagasin;
    private Signalements signalements;

    @Override
    public void onEnable() {
        instance = this;
        cleTier = new NamespacedKey(this, "tier");
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "comptes.yml");
        charger();

        menu = new MenuEconomie(this);
        echanges = new Echanges(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(echanges, this);
        getCommand("economie").setExecutor(this);
        getCommand("echange").setExecutor(echanges);
        getCommand("echange").setTabCompleter(echanges);
        // 1.1.0 : magasins et boutiques (cahier, « Magasins » révisés le 02/10/2026).
        magasins = new Magasins(this);
        boutiques = new Boutiques(this, magasins);
        menuMagasin = new MenuMagasin(this, magasins, boutiques);
        signalements = new Signalements(this, magasins);
        getServer().getPluginManager().registerEvents(boutiques, this);
        getServer().getPluginManager().registerEvents(menuMagasin, this);
        getCommand("magasin").setExecutor(menuMagasin);
        getCommand("magasin").setTabCompleter(menuMagasin);
        if (!getServer().getPluginManager().isPluginEnabled("WorldGuard")
                || !getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem")) {
            getLogger().warning("WorldGuard ou SimpleClaimSystem absent : aucun magasin ne peut être créé.");
        }

        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "economie", lang.c("bouton.nom", "<green><bold>Économie"),
                    lang.c("bouton.description", "<gray>Solde, dépôt et retrait d'émeraudes"), 10, menu::ouvrir);
        }
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            VaultEconomie.enregistrer(this);
            getLogger().info("Déclaré comme économie Vault.");
        } else {
            getLogger().warning("Vault absent : KS_Economy n'est pas déclaré comme économie Vault.");
        }

        // Toutes les minutes : prélèvement du jour (minuit à Paris) et sauvegarde si besoin.
        getServer().getScheduler().runTaskTimer(this, () -> {
            prelevementDuJour();
            sauverSiBesoin();
        }, 20L * 60, 20L * 60);
        Bukkit.getOnlinePlayers().forEach(this::majListe);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        if (echanges != null) {
            echanges.toutAnnuler();
        }
        if (menuMagasin != null) {
            menuMagasin.toutFermer();
        }
        if (menu != null) {
            menu.fermerDepots();
        }
        aSauver = true;
        sauverSiBesoin();
    }

    Lang lang() {
        return lang;
    }

    Gui gui() {
        return gui;
    }

    Boutiques boutiques() {
        return boutiques;
    }

    Signalements signalements() {
        return signalements;
    }

    MenuMagasin menuMagasin() {
        return menuMagasin;
    }

    /** 1.1.0 (KS_Claim) : pourquoi refuser de supprimer ou vendre ce claim (il porte des boutiques), ou null. */
    public static String refusClaim(UUID proprio, org.bukkit.Chunk chunk) {
        return instance.magasins == null ? null : instance.magasins.refusClaim(proprio, chunk);
    }

    /** 1.1.0 (KS_Claim) : claim supprimé ou vendu ; il quitte le magasin de son ancien propriétaire. */
    public static void claimRetire(UUID proprio, org.bukkit.Chunk chunk) {
        if (instance.magasins != null) {
            instance.magasins.claimRetire(proprio, chunk);
        }
    }

    // ------------------------------------------------------------------ réglages

    /** Points reçus par émeraude déposée (taxe de conversion : 1 = 1:1). */
    double tauxDepot() {
        return Math.max(0, getConfig().getDouble("conversion.depot", 1.0));
    }

    /** Points payés par émeraude retirée (1 = 1:1). */
    double tauxRetrait() {
        return Math.max(0, getConfig().getDouble("conversion.retrait", 1.0));
    }

    /** Part du solde prélevée chaque jour quand il est masqué, en % (1 par défaut). */
    double pourcentMasque() {
        return Math.max(0, Math.min(100, getConfig().getDouble("masquage.pourcent-par-jour", 1.0)));
    }

    // ------------------------------------------------------------------ comptes (autres plugins)

    /** Solde d'un joueur, en points (0 s'il n'a pas de compte). */
    public static long solde(UUID joueur) {
        synchronized (instance.comptes) {
            Compte compte = instance.comptes.get(joueur);
            return compte == null ? 0 : compte.solde;
        }
    }

    /** Ajoute des points (points &gt; 0). */
    public static void crediter(UUID joueur, long points) {
        if (points <= 0) {
            return;
        }
        synchronized (instance.comptes) {
            Compte compte = instance.compte(joueur);
            compte.solde = Math.addExact(compte.solde, points);
            instance.aSauver = true;
        }
        instance.majListePlusTard(joueur);
    }

    /** Retire des points si le solde suffit ; false sinon (rien n'est retiré). */
    public static boolean debiter(UUID joueur, long points) {
        if (points < 0) {
            return false;
        }
        synchronized (instance.comptes) {
            Compte compte = instance.compte(joueur);
            if (compte.solde < points) {
                return false;
            }
            compte.solde -= points;
            instance.aSauver = true;
        }
        instance.majListePlusTard(joueur);
        return true;
    }

    /** Le compte existe-t-il ? */
    static boolean aUnCompte(UUID joueur) {
        synchronized (instance.comptes) {
            return instance.comptes.containsKey(joueur);
        }
    }

    static void creerCompte(UUID joueur) {
        synchronized (instance.comptes) {
            instance.compte(joueur);
            instance.aSauver = true;
        }
    }

    private Compte compte(UUID joueur) {
        return comptes.computeIfAbsent(joueur, u -> {
            Compte compte = new Compte();
            Player enLigne = Bukkit.getPlayer(u);
            compte.nom = enLigne != null ? enLigne.getName() : null;
            return compte;
        });
    }

    boolean estMasque(UUID joueur) {
        synchronized (comptes) {
            Compte compte = comptes.get(joueur);
            return compte != null && compte.masque;
        }
    }

    void masquer(Player joueur, boolean masque) {
        synchronized (comptes) {
            compte(joueur.getUniqueId()).masque = masque;
            aSauver = true;
        }
        majListe(joueur);
    }

    // ------------------------------------------------------------------ liste des joueurs (Tab)

    /** « Pseudo  1 234 » dans la liste des joueurs, ou le pseudo seul si le solde est masqué. */
    void majListe(Player joueur) {
        Component nom = Component.text(joueur.getName());
        if (!estMasque(joueur.getUniqueId())) {
            nom = nom.append(lang.c("tab.solde", " <green><solde>", "solde", nombre(solde(joueur.getUniqueId()))));
        }
        joueur.playerListName(nom);
    }

    private void majListePlusTard(UUID joueur) {
        Runnable maj = () -> {
            Player enLigne = Bukkit.getPlayer(joueur);
            if (enLigne != null) {
                majListe(enLigne);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            maj.run();
        } else {
            Bukkit.getScheduler().runTask(this, maj);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        synchronized (comptes) {
            compte(joueur.getUniqueId()).nom = joueur.getName();
            aSauver = true;
        }
        majListe(joueur);
    }

    // ------------------------------------------------------------------ prélèvement du solde masqué

    /**
     * Une fois par jour, au premier passage après minuit (heure de Paris) : chaque solde masqué perd 1 % (arrondi à
     * l'unité inférieure ; rien si le solde est nul). Serveur éteint à minuit : prélèvement au démarrage suivant, une
     * seule fois quel que soit le nombre de jours passés.
     */
    private void prelevementDuJour() {
        LocalDate aujourdhui = LocalDate.now(PARIS);
        if (aujourdhui.equals(dernierPrelevement)) {
            return;
        }
        double part = pourcentMasque() / 100;
        synchronized (comptes) {
            if (dernierPrelevement != null) {
                for (Compte compte : comptes.values()) {
                    if (compte.masque && compte.solde > 0) {
                        compte.solde -= (long) Math.floor(compte.solde * part);
                    }
                }
            }
            dernierPrelevement = aujourdhui;
            aSauver = true;
        }
        Bukkit.getOnlinePlayers().forEach(this::majListe);
    }

    // ------------------------------------------------------------------ sauvegarde

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        var section = yaml.getConfigurationSection("comptes");
        if (section != null) {
            for (String cle : section.getKeys(false)) {
                try {
                    Compte compte = new Compte();
                    compte.solde = Math.max(0, section.getLong(cle + ".solde"));
                    compte.masque = section.getBoolean(cle + ".masque");
                    compte.nom = section.getString(cle + ".nom");
                    comptes.put(UUID.fromString(cle), compte);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Compte ignoré (uuid invalide) : " + cle);
                }
            }
        }
        String date = yaml.getString("dernier-prelevement");
        dernierPrelevement = date == null ? LocalDate.now(PARIS) : LocalDate.parse(date);
        prelevementDuJour();
    }

    private void sauverSiBesoin() {
        YamlConfiguration yaml = new YamlConfiguration();
        synchronized (comptes) {
            if (!aSauver) {
                return;
            }
            yaml.set("dernier-prelevement", dernierPrelevement.toString());
            comptes.forEach((uuid, compte) -> {
                String cle = "comptes." + uuid;
                yaml.set(cle + ".nom", compte.nom);
                yaml.set(cle + ".solde", compte.solde);
                yaml.set(cle + ".masque", compte.masque);
            });
            aSauver = false;
        }
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer comptes.yml : " + e.getMessage());
            aSauver = true;
        }
    }

    /** Pseudo d'un compte (dernier connu), ou null. */
    static String nomDuCompte(UUID joueur) {
        synchronized (instance.comptes) {
            Compte compte = instance.comptes.get(joueur);
            return compte == null ? null : compte.nom;
        }
    }

    // ------------------------------------------------------------------ blocs d'émeraude compressés

    /** Valeur en émeraudes d'un bloc compressé : 9 x 10^tier. */
    static long emeraudesDuTier(int tier) {
        long valeur = 9;
        for (int i = 0; i < tier; i++) {
            valeur *= 10;
        }
        return valeur;
    }

    /** Un bloc d'émeraude compressé (tier 1 à 6), ou null. Nécessite que le plugin soit activé. */
    public static ItemStack creerBloc(int tier) {
        if (tier < 1 || tier > TIER_MAX) {
            return null;
        }
        ItemStack bloc = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = bloc.getItemMeta();
        meta.displayName(Component.text("Bloc d'émeraude compressé (tier " + tier + ")", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Valeur : " + nombre(emeraudesDuTier(tier)) + " points", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(cleTier, PersistentDataType.INTEGER, tier);
        bloc.setItemMeta(meta);
        return bloc;
    }

    /** Tier d'un bloc compressé, 0 si ce n'en est pas un. */
    public static int tierBloc(ItemStack objet) {
        if (objet == null || objet.getType() != Material.EMERALD_BLOCK || !objet.hasItemMeta()) {
            return 0;
        }
        Integer tier = objet.getItemMeta().getPersistentDataContainer().get(cleTier, PersistentDataType.INTEGER);
        return tier == null || tier < 1 || tier > TIER_MAX ? 0 : tier;
    }

    /** Valeur en émeraudes d'un objet (par unité) : émeraude 1, bloc 9, bloc compressé 9 x 10^tier ; 0 sinon. */
    static long emeraudesParUnite(ItemStack objet) {
        if (objet == null) {
            return 0;
        }
        int tier = tierBloc(objet);
        if (tier > 0) {
            return emeraudesDuTier(tier);
        }
        if (objet.hasItemMeta() && objet.getItemMeta().getPersistentDataContainer().has(cleTier)) {
            return 0;
        }
        return switch (objet.getType()) {
            case EMERALD -> 1;
            case EMERALD_BLOCK -> 9;
            default -> 0;
        };
    }

    /** Impossibles à poser. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (tierBloc(event.getItemInHand()) > 0) {
            event.setCancelled(true);
        }
    }

    /** Impossibles à décrafter : aucune recette vanilla ne les accepte (les recettes des plugins, oui). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (recetteVanilla(event.getRecipe()) && contientBloc(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    /** Autocrafteur. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (recetteVanilla(event.getRecipe()) && event.getBlock().getState() instanceof Crafter crafter
                && contientBloc(crafter.getInventory().getContents())) {
            event.setCancelled(true);
        }
    }

    private static boolean recetteVanilla(Recipe recette) {
        return recette instanceof Keyed keyed && NamespacedKey.MINECRAFT.equals(keyed.getKey().getNamespace());
    }

    private static boolean contientBloc(ItemStack[] objets) {
        for (ItemStack objet : objets) {
            if (tierBloc(objet) > 0) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ commande et textes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            menu.ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    /** 1234567 -> « 1 234 567 ». */
    static String nombre(long valeur) {
        String chiffres = String.valueOf(Math.abs(valeur));
        StringBuilder sortie = new StringBuilder(valeur < 0 ? "-" : "");
        for (int i = 0; i < chiffres.length(); i++) {
            if (i > 0 && (chiffres.length() - i) % 3 == 0) {
                sortie.append(' ');
            }
            sortie.append(chiffres.charAt(i));
        }
        return sortie.toString();
    }

    /** « 1 point », « 1 234 points ». */
    static String points(long valeur) {
        return nombre(valeur) + (Math.abs(valeur) > 1 ? " points" : " point");
    }
}
