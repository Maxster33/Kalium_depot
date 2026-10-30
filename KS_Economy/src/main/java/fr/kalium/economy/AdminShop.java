package fr.kalium.economy;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 1.13.0 (LeKiwi06, 30/10/2026, événement d'Halloween de Kixster ; cahier local validé le 30/10) : admin shop, où le
 * joueur achète au serveur, ouvert seulement pendant un événement, et rachats de la semaine étendus.
 *
 * - Ouverture par le staff : /adminshop ouvrir <jours>, /adminshop fermer ; fermeture automatique à la fin. Message à
 *   tous à l'ouverture, la veille de la fin et à la fermeture. Pendant l'événement, les rachats de la semaine passent
 *   de 10 à 100 objets (evenement.rachats-objets) ; à la fermeture, on garde les 10 premiers.
 * - Objets et prix dans adminshop.yml (copié du jar au premier démarrage) : objets custom sans craft (créés par
 *   KS_KaliumGive), livre Raccommodage, coffres de récompense (déposés dans /rewards par KS_RewardsGUI). Prix =
 *   1,5 × la valeur validée. Aucune limite d'achat. Un objet dont le plugin est absent n'est pas proposé (amulette tant
 *   que KS_MaleficesNether n'est pas là).
 * - Paiement en points ; refus si le solde ou la place manque. Monde de test : le compte de test (CompteTest).
 */
final class AdminShop implements TabExecutor {

    /** Un objet de l'admin shop. type : custom (id de KS_KaliumGive), livre (enchantement), coffre (valeur). */
    record Article(String cle, String type, String id, double valeur, long prix) {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM 'à' HH'h'mm", Locale.FRENCH)
            .withZone(KSEconomy.PARIS);
    /** Cases des objets : trois rangées de 7, centrées. */
    private static final int[] PLACES = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32,
            33, 34};

    private final KSEconomy plugin;
    private final Lang lang;
    private final Menus menus;
    private final File fichierEtat;
    private final List<Article> articles = new ArrayList<>();
    /** Fin de l'événement (millisecondes), 0 s'il est fermé. */
    private long fin;
    private boolean veilleAnnoncee;

    AdminShop(KSEconomy plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.menus = plugin.menus();
        this.fichierEtat = new File(plugin.getDataFolder(), "evenement.yml");
        chargerArticles();
        YamlConfiguration etat = YamlConfiguration.loadConfiguration(fichierEtat);
        fin = etat.getLong("fin", 0);
        veilleAnnoncee = etat.getBoolean("veille-annoncee", false);
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private void chargerArticles() {
        File fichier = new File(plugin.getDataFolder(), "adminshop.yml");
        if (!fichier.exists()) {
            plugin.saveResource("adminshop.yml", false);
        }
        ConfigurationSection objets = YamlConfiguration.loadConfiguration(fichier).getConfigurationSection("objets");
        if (objets == null) {
            plugin.getLogger().warning("adminshop.yml : aucune section « objets ».");
            return;
        }
        for (String cle : objets.getKeys(false)) {
            ConfigurationSection o = objets.getConfigurationSection(cle);
            String type = o == null ? "" : o.getString("type", "custom");
            long prix = o == null ? 0 : o.getLong("prix", 0);
            if (prix <= 0 || !List.of("custom", "livre", "coffre").contains(type)) {
                plugin.getLogger().warning("adminshop.yml : objet « " + cle + " » ignoré (type ou prix invalide).");
                continue;
            }
            articles.add(new Article(cle, type, o.getString("id", cle), o.getDouble("valeur", 0), prix));
        }
        plugin.getLogger().info("Admin shop : " + articles.size() + " objets dans adminshop.yml.");
    }

    private void sauverEtat() {
        YamlConfiguration etat = new YamlConfiguration();
        etat.set("fin", fin);
        etat.set("veille-annoncee", veilleAnnoncee);
        try {
            plugin.getDataFolder().mkdirs();
            etat.save(fichierEtat);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer evenement.yml : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ événement

    /** L'événement est-il en cours (admin shop ouvert, rachats étendus) ? */
    boolean ouvert() {
        return fin > System.currentTimeMillis();
    }

    private String dateFin() {
        return DATE.format(Instant.ofEpochMilli(fin));
    }

    /** Toutes les minutes : annonce de la veille, fermeture automatique. */
    void verifier() {
        if (fin == 0) {
            return;
        }
        long maintenant = System.currentTimeMillis();
        if (maintenant >= fin) {
            fermer(null);
            return;
        }
        long veille = plugin.getConfig().getLong("evenement.annonce-veille-heures", 24) * 3_600_000L;
        if (!veilleAnnoncee && fin - maintenant <= veille) {
            veilleAnnoncee = true;
            sauverEtat();
            Bukkit.broadcast(t("adminshop.annonce-veille", "<gold>L'admin shop ferme le <white><date></white> : "
                    + "dernières heures pour en profiter (/adminshop).", "date", dateFin()));
            lang.saveIfNeeded();
        }
    }

    private void ouvrirEvenement(CommandSender staff, int jours) {
        fin = System.currentTimeMillis() + jours * 86_400_000L;
        veilleAnnoncee = false;
        sauverEtat();
        plugin.getLogger().info(staff.getName() + " ouvre l'admin shop pour " + jours + " jour(s), jusqu'au "
                + dateFin() + ".");
        plugin.rachats().verifier();
        Bukkit.broadcast(t("adminshop.annonce-ouverture", "<gold><bold>L'admin shop est ouvert</bold></gold> <white>"
                + "jusqu'au <date> : /adminshop ou le menu Économie. Les rachats de la semaine passent à <n> objets.",
                "date", dateFin(), "n", plugin.rachats().nombre()));
        lang.saveIfNeeded();
    }

    /** Ferme l'événement (staff : la commande ; null : la fin est passée). */
    private void fermer(CommandSender staff) {
        fin = 0;
        veilleAnnoncee = false;
        sauverEtat();
        plugin.getLogger().info("Admin shop fermé" + (staff == null ? " (fin de l'événement)" : " par "
                + staff.getName()) + ".");
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            if (Contenant.ouvert(joueur, "adminshop") != null || Contenant.ouvert(joueur, "adminshop-achat") != null) {
                joueur.closeInventory();
            }
        }
        plugin.rachats().verifier();
        Bukkit.broadcast(t("adminshop.annonce-fermeture", "<gold>L'admin shop est fermé. <white>Les rachats de la "
                + "semaine reviennent à <n> objets.", "n", plugin.rachats().nombre()));
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player joueur) {
                ouvrir(joueur);
            } else {
                sender.sendMessage("Commande réservée aux joueurs (staff : /adminshop ouvrir <jours> | fermer).");
            }
            return true;
        }
        if (!sender.hasPermission(Signalements.PERMISSION_STAFF)) {
            sender.sendMessage(t("adminshop.staff-seulement", "<red>Réservé au staff."));
            lang.saveIfNeeded();
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "ouvrir" -> {
                int jours = args.length == 2 ? Boutiques.entier(args[1]) : 0;
                if (jours < 1 || jours > 365) {
                    sender.sendMessage(t("adminshop.usage-ouvrir", "<red>/adminshop ouvrir <jours> (1 à 365)."));
                } else {
                    ouvrirEvenement(sender, jours);
                    sender.sendMessage(t("adminshop.ouvert-staff", "<green>Admin shop ouvert jusqu'au <date>.",
                            "date", dateFin()));
                }
            }
            case "fermer" -> {
                if (fin == 0) {
                    sender.sendMessage(t("adminshop.deja-ferme", "<red>L'admin shop est déjà fermé."));
                } else {
                    fermer(sender);
                }
            }
            case "etat" -> sender.sendMessage(ouvert()
                    ? t("adminshop.etat-ouvert", "<green>Admin shop ouvert jusqu'au <date> ; rachats : <n> objets.",
                    "date", dateFin(), "n", plugin.rachats().nombre())
                    : t("adminshop.etat-ferme", "<gray>Admin shop fermé ; rachats : <n> objets.", "n",
                    plugin.rachats().nombre()));
            default -> sender.sendMessage(t("adminshop.usage", "<red>/adminshop [ouvrir <jours> | fermer | etat]"));
        }
        lang.saveIfNeeded();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(Signalements.PERMISSION_STAFF)) {
            List<String> r = new ArrayList<>();
            for (String s : List.of("ouvrir", "fermer", "etat")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    r.add(s);
                }
            }
            return r;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ouvrir")) {
            return List.of("15");
        }
        return List.of();
    }

    // ------------------------------------------------------------------ objets

    /** Un objet neuf de cet article (null : coffre, ou plugin de l'objet absent). */
    private ItemStack creer(Article a) {
        if (a.type().equals("livre")) {
            Enchantment ench = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft(a.id()));
            ItemStack livre = new ItemStack(Material.ENCHANTED_BOOK);
            if (ench != null && livre.getItemMeta() instanceof EnchantmentStorageMeta meta) {
                meta.addStoredEnchant(ench, ench.getMaxLevel(), true);
                livre.setItemMeta(meta);
                return livre;
            }
            return null;
        }
        if (!a.type().equals("custom")) {
            return null;
        }
        Plugin give = plugin.getServer().getPluginManager().getPlugin("KS_KaliumGive");
        if (give == null || !give.isEnabled()) {
            return null;
        }
        try {
            // Par réflexion, comme Rachats : KS_KaliumGive dépend de plugins qui dépendent de KS_Economy.
            return (ItemStack) give.getClass().getMethod("creer", String.class).invoke(null, a.id());
        } catch (ReflectiveOperationException | RuntimeException erreur) {
            plugin.getLogger().warning("Admin shop : objet " + a.id() + " non créé : " + erreur);
            return null;
        }
    }

    private static Plugin rewards() {
        Plugin p = Bukkit.getPluginManager().getPlugin("KS_RewardsGUI");
        return p != null && p.isEnabled() ? p : null;
    }

    /** L'article peut-il être vendu ici (son plugin est là) ? */
    private boolean disponible(Article a) {
        return a.type().equals("coffre") ? rewards() != null && a.valeur() > 0 : creer(a) != null;
    }

    /** Image de l'article dans le menu (sans les marques de son plugin : ce n'est qu'une image). */
    private ItemStack icone(Article a) {
        if (a.type().equals("coffre")) {
            return new ItemStack(Material.CHEST);
        }
        ItemStack objet = creer(a);
        if (objet == null) {
            return new ItemStack(Material.BARRIER);
        }
        ItemMeta meta = objet.getItemMeta();
        if (meta != null) {
            PersistentDataContainer marques = meta.getPersistentDataContainer();
            for (NamespacedKey cle : new ArrayList<>(marques.getKeys())) {
                marques.remove(cle);
            }
            objet.setItemMeta(meta);
        }
        return objet;
    }

    private Component nom(Article a) {
        if (a.type().equals("coffre")) {
            return t("adminshop.nom-coffre", "<light_purple>Coffre de récompense (valeur <valeur>)", "valeur",
                    KSEconomy.nombre(Math.round(a.valeur())));
        }
        ItemStack objet = creer(a);
        return objet == null ? Component.text(a.cle()) : KSEconomy.nomObjet(objet);
    }

    // ------------------------------------------------------------------ menus

    /** Menu de l'admin shop : un objet par case ; un clic ouvre l'achat. */
    void ouvrir(Player joueur) {
        if (!ouvert()) {
            menus.message(joueur, t("adminshop.ferme", "<red>L'admin shop est fermé : il n'ouvre que pendant les "
                    + "événements."), true);
            return;
        }
        Contenant c = menus.menu(joueur, 6, t("adminshop.titre", "<dark_gray>Admin shop"), "adminshop");
        c.poser(4, Menus.objet(Material.JACK_O_LANTERN, t("adminshop.info", "<gold>Admin shop"),
                t("adminshop.intro", "<white>Le serveur vend ces objets jusqu'au <date>.", "date", dateFin()),
                t("eco.solde", "<white>Solde : <green><solde>", "solde",
                        KSEconomy.points(KSEconomy.solde(joueur.getUniqueId())))), null);
        int i = 0;
        for (Article a : articles) {
            if (i >= PLACES.length || !disponible(a)) {
                continue;
            }
            List<Component> lignes = new ArrayList<>();
            lignes.add(t("adminshop.prix", "<gray>Prix : <green><prix></green> l'unité", "prix", KSEconomy.points(a.prix())));
            if (a.type().equals("coffre")) {
                lignes.add(t("adminshop.coffre-info", "<gray>Arrive dans /rewards, contenu tiré à l'ouverture."));
            }
            lignes.add(t("adminshop.clic", "<dark_gray>Clic : acheter"));
            c.poser(PLACES[i++], Contenant.objet(icone(a), nom(a), lignes), p -> detail(p, a));
        }
        if (i == 0) {
            c.poser(22, Contenant.objet(Material.BARRIER, t("adminshop.vide", "<red>Aucun objet en vente."),
                    List.of()), null);
        }
        menus.sortie(c, plugin.menuEconomie()::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Achat : 1, 8 ou 64 (un coffre : 1, 5 ou 10), puis confirmation. */
    private void detail(Player joueur, Article a) {
        if (!ouvert()) {
            ouvrir(joueur);
            return;
        }
        Contenant c = menus.menu(joueur, 4, t("adminshop.titre-achat", "<dark_gray>Acheter au serveur"),
                "adminshop-achat");
        ItemStack image = icone(a);
        c.poser(4, Contenant.objet(image, nom(a), List.of(
                t("adminshop.prix", "<gray>Prix : <green><prix></green> l'unité", "prix", KSEconomy.points(a.prix())),
                t("eco.solde", "<white>Solde : <green><solde>", "solde",
                        KSEconomy.points(KSEconomy.solde(joueur.getUniqueId()))))), null);
        int[] nombres = a.type().equals("coffre") ? new int[]{1, 5, 10}
                : image.getMaxStackSize() >= 64 ? new int[]{1, 8, 64} : new int[]{1, 2, 4};
        for (int k = 0; k < nombres.length; k++) {
            int n = nombres[k];
            c.poser(11 + 2 * k, Contenant.objet(Menus.telQuel(image, n),
                    t("adminshop.bouton-nombre", "<yellow>Acheter <nombre>", "nombre", n),
                    List.of(t("adminshop.cout", "<gray>Coût : <white><cout>", "cout", KSEconomy.points(a.prix() * n)))),
                    p -> confirmer(p, a, n));
        }
        menus.sortie(c, this::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private void confirmer(Player joueur, Article a, int n) {
        menus.confirmer(joueur, t("adminshop.titre-confirmer", "<dark_gray>Confirmer l'achat"),
                t("adminshop.a-confirmer", "<white>Acheter <yellow><nombre> x <objet></yellow> pour <green><cout>"
                        + "</green> ?", "nombre", n, "objet", nom(a), "cout", KSEconomy.points(a.prix() * n)),
                p -> acheter(p, a, n), p -> detail(p, a));
    }

    private void acheter(Player joueur, Article a, int n) {
        if (!ouvert()) {
            joueur.closeInventory();
            menus.message(joueur, t("adminshop.ferme", "<red>L'admin shop est fermé : il n'ouvre que pendant les "
                    + "événements."), true);
            return;
        }
        UUID id = joueur.getUniqueId();
        long cout = a.prix() * n;
        boolean coffre = a.type().equals("coffre");
        ItemStack modele = coffre ? null : creer(a);
        if (!coffre && modele == null || coffre && rewards() == null) {
            detail(joueur, a);
            menus.message(joueur, t("adminshop.indisponible", "<red>Cet objet n'est pas disponible en ce moment."), true);
            return;
        }
        if (!coffre && MenuEconomie.place(joueur, modele) < n) {
            detail(joueur, a);
            menus.message(joueur, t("adminshop.place", "<red>Pas assez de place dans ton inventaire."), true);
            return;
        }
        if (!KSEconomy.debiter(id, cout)) {
            detail(joueur, a);
            menus.message(joueur, t("adminshop.solde", "<red>Solde insuffisant : il faut <cout>.", "cout",
                    KSEconomy.points(cout)), true);
            return;
        }
        int livres = coffre ? deposerCoffres(joueur, a, n) : donner(joueur, a, modele, n);
        if (livres < n) {
            KSEconomy.crediter(id, a.prix() * (n - livres));
            plugin.getLogger().warning("Admin shop : " + (n - livres) + " x " + a.cle() + " non livrés à "
                    + joueur.getName() + ", remboursés.");
        }
        if (livres == 0) {
            detail(joueur, a);
            menus.message(joueur, t("adminshop.echec", "<red>Achat impossible pour le moment : rien n'a été prélevé."),
                    true);
            return;
        }
        plugin.getLogger().info(joueur.getName() + (CompteTest.enTest(joueur) ? " (monde de test)" : "") + " achète "
                + livres + " x " + a.cle() + " pour " + a.prix() * livres + " points (admin shop).");
        detail(joueur, a);
        menus.message(joueur, coffre
                ? t("adminshop.achete-coffre", "<white>Acheté : <yellow><nombre> x <objet></yellow> pour <green><cout>"
                        + "</green>. /rewards pour les récupérer.", "nombre", livres, "objet", nom(a), "cout",
                KSEconomy.points(a.prix() * livres))
                : t("adminshop.achete", "<white>Acheté : <yellow><nombre> x <objet></yellow> pour <green><cout>"
                        + "</green>.", "nombre", livres, "objet", nom(a), "cout", KSEconomy.points(a.prix() * livres)),
                false);
    }

    /** Donne n objets ; un objet non empilable est créé à l'unité (il peut porter un identifiant). */
    private int donner(Player joueur, Article a, ItemStack modele, int n) {
        int donnes = 0;
        while (donnes < n) {
            ItemStack pile = modele.getMaxStackSize() > 1 ? modele.clone() : creer(a);
            if (pile == null) {
                break;
            }
            int nombre = Math.min(n - donnes, pile.getMaxStackSize());
            pile.setAmount(nombre);
            int reste = 0;
            for (ItemStack r : joueur.getInventory().addItem(pile).values()) {
                reste += r.getAmount();
            }
            donnes += nombre - reste;
            if (reste > 0) {
                break;
            }
        }
        return donnes;
    }

    /** Dépose n coffres « Non ouvert » dans /rewards (compte de test dans le monde de test). */
    private int deposerCoffres(Player joueur, Article a, int n) {
        Plugin r = rewards();
        if (r == null) {
            return 0;
        }
        String raison = lang.raw("adminshop.raison-coffre", "Admin shop");
        int deposes = 0;
        try {
            java.lang.reflect.Method deposer = r.getClass().getMethod("deposerCoffre", UUID.class, String.class,
                    String.class, String.class, double.class);
            for (int k = 0; k < n; k++) {
                if (!(boolean) deposer.invoke(null, CompteTest.de(joueur.getUniqueId()), joueur.getName(), "Admin shop",
                        raison, a.valeur())) {
                    break;
                }
                deposes++;
            }
        } catch (ReflectiveOperationException | RuntimeException erreur) {
            plugin.getLogger().warning("Admin shop : coffre non déposé : " + erreur);
        }
        return deposes;
    }
}
