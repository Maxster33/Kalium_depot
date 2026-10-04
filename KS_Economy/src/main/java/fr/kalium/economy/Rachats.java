package fr.kalium.economy;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1.3.0 (LeKiwi06, 04/10/2026) : barème des prix d'Event (rachats.csv, dans le jar) et « Rachats de la semaine » : le
 * serveur rachète aux joueurs 10 objets tirés chaque semaine (lundi, heure de Paris), 2 par gamme de prix.
 *
 * - Tirage : dans chaque gamme (moins de 0,1 ; 0,1 à 1 ; 1 à 10 ; 10 à 100 ; 100 et plus), 2 familles au hasard, puis
 *   un objet de la famille dans cette gamme (les variantes d'un objet comptent pour un seul : têtes, cuivre, sapin...).
 * - Prix de la semaine : prix du barème à plus ou moins 25 %, arrondi à l'unité dès 1 émeraude.
 * - Lot : la quantité (arrondie à l'unité inférieure) qui approche 1 / 10 / 64 / 320 émeraudes selon la gamme du prix
 *   de la semaine, payée au nombre entier le plus proche ; à l'unité à partir de 100 et pour un objet non empilable.
 * - Quota par objet, par joueur et par jour : au-dessus de 25 000 de cagnotte, un objet ne rapporte pas plus de 5 % de
 *   la cagnotte (7,5 % si elle est masquée) ; la vente qui fait dépasser passe, les suivantes sont refusées.
 */
final class Rachats implements CommandExecutor {

    /** Une ligne du barème. cible : m, potion:TYPE, pdc:espace:clé:type[:valeur], ou - (jamais tirée). */
    record Entree(String id, String famille, double prix, String cible, String nom, int pile, Material materiel) {
    }

    /** Un objet racheté cette semaine : quantité par lot et points payés par lot. */
    record Offre(Entree entree, double unite, int quantite, long points) {
    }

    /** Bornes des gammes de prix : moins de 0,1 ; 0,1 à 1 ; 1 à 10 ; 10 à 100 ; 100 et plus. */
    private static final double[] BORNES = {0.1, 1, 10, 100};
    private static final long[] LOTS = {1, 10, 64, 320};

    private final KSEconomy plugin;
    private final Lang lang;
    private final Gui gui;
    private final File fichier;
    private final Map<String, Entree> base = new LinkedHashMap<>();
    private final List<Offre> offres = new ArrayList<>();
    /** Points gagnés aujourd'hui : joueur -> id de l'objet -> points. */
    private final Map<UUID, Map<String, Long>> gains = new HashMap<>();
    private String semaine;
    private LocalDate jourGains;

    Rachats(KSEconomy plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.fichier = new File(plugin.getDataFolder(), "rachats.yml");
        chargerBase();
        charger();
        verifier();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ barème

    private void chargerBase() {
        int absents = 0;
        try (InputStream in = plugin.getResource("rachats.csv")) {
            if (in == null) {
                plugin.getLogger().warning("rachats.csv absent du jar : aucun rachat.");
                return;
            }
            BufferedReader lecteur = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                String[] c = ligne.split(";", -1);
                if (ligne.startsWith("#") || c.length < 6) {
                    continue;
                }
                String cible = c[3];
                Material materiel = null;
                int pile = c[5].isEmpty() ? 1 : Integer.parseInt(c[5]);
                if (cible.equals("m")) {
                    materiel = Material.matchMaterial(c[0]);
                    if (materiel == null || !materiel.isItem()) {
                        // objet d'une version plus récente que le serveur : gardé dans la base, jamais tiré
                        cible = "-";
                        materiel = null;
                        absents++;
                    } else {
                        pile = materiel.getMaxStackSize();
                    }
                }
                base.put(c[0], new Entree(c[0], c[1], Double.parseDouble(c[2]), cible, c[4], pile, materiel));
            }
        } catch (IOException | NumberFormatException e) {
            plugin.getLogger().warning("rachats.csv illisible : " + e.getMessage());
        }
        plugin.getLogger().info("Barème : " + base.size() + " objets (" + absents + " inconnus de cette version).");
    }

    /** Prix du barème (émeraudes à l'unité), 0 si l'id est inconnu. */
    double prix(String id) {
        Entree e = base.get(id);
        return e == null ? 0 : e.prix();
    }

    private static int gamme(double prix) {
        int g = 0;
        while (g < BORNES.length && prix >= BORNES[g]) {
            g++;
        }
        return g;
    }

    /** Un objet non empilable s'achète à l'unité : il lui faut un prix d'au moins 1 émeraude. */
    private static boolean tirable(Entree e) {
        return !e.cible().equals("-") && (e.pile() > 1 || e.prix() >= 1);
    }

    private Component nom(Entree e) {
        if (!e.nom().isEmpty()) {
            return Component.text(e.nom());
        }
        return Boutiques.nomObjet(new ItemStack(e.materiel()));
    }

    // ------------------------------------------------------------------ reconnaître un objet

    /** Objet vanilla « nu » : ni marque de plugin, ni enchantement, ni usure, ni contenu. */
    private static boolean nu(ItemStack objet) {
        if (!objet.hasItemMeta()) {
            return true;
        }
        ItemMeta meta = objet.getItemMeta();
        if (!meta.getPersistentDataContainer().getKeys().isEmpty() || meta.hasEnchants()) {
            return false;
        }
        if (meta instanceof Damageable usure && usure.hasDamage()) {
            return false;
        }
        if (meta instanceof BundleMeta sac && sac.hasItems()) {
            return false;
        }
        return !(meta instanceof BlockStateMeta bloc && bloc.hasBlockState()
                && bloc.getBlockState() instanceof Container contenant && !contenant.getSnapshotInventory().isEmpty());
    }

    private static boolean correspond(ItemStack objet, Entree e) {
        if (objet == null || objet.getType().isAir()) {
            return false;
        }
        String cible = e.cible();
        if (cible.equals("m")) {
            return objet.getType() == e.materiel() && nu(objet);
        }
        if (!objet.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = objet.getItemMeta();
        PersistentDataContainer marques = meta.getPersistentDataContainer();
        if (cible.startsWith("potion:")) {
            if (objet.getType() != Material.POTION || !(meta instanceof PotionMeta potion) || !marques.getKeys().isEmpty()
                    || potion.hasCustomEffects() || potion.getBasePotionType() == null) {
                return false;
            }
            return potion.getBasePotionType().getKey().getKey().equalsIgnoreCase(cible.substring(7));
        }
        if (cible.startsWith("pdc:")) {
            String[] c = cible.split(":", 5);
            NamespacedKey cle = new NamespacedKey(c[1], c[2]);
            return switch (c[3]) {
                case "s" -> marques.has(cle, PersistentDataType.STRING)
                        && c[4].equals(marques.get(cle, PersistentDataType.STRING));
                case "i" -> marques.has(cle, PersistentDataType.INTEGER)
                        && Integer.parseInt(c[4]) == marques.get(cle, PersistentDataType.INTEGER);
                default -> marques.has(cle, PersistentDataType.BYTE);
            };
        }
        return false;
    }

    private static int compter(Player joueur, Entree e) {
        int total = 0;
        for (ItemStack objet : joueur.getInventory().getStorageContents()) {
            if (correspond(objet, e)) {
                total += objet.getAmount();
            }
        }
        return total;
    }

    private static void retirer(Player joueur, Entree e, int nombre) {
        ItemStack[] contenu = joueur.getInventory().getStorageContents();
        int reste = nombre;
        for (int i = 0; i < contenu.length && reste > 0; i++) {
            if (!correspond(contenu[i], e)) {
                continue;
            }
            int pris = Math.min(reste, contenu[i].getAmount());
            reste -= pris;
            if (pris == contenu[i].getAmount()) {
                contenu[i] = null;
            } else {
                contenu[i].setAmount(contenu[i].getAmount() - pris);
            }
        }
        joueur.getInventory().setStorageContents(contenu);
    }

    // ------------------------------------------------------------------ tirage de la semaine

    private static String semaineDu(LocalDate jour) {
        return jour.get(IsoFields.WEEK_BASED_YEAR) + "-S"
                + String.format(Locale.ROOT, "%02d", jour.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    /** Toutes les minutes : nouveau tirage le lundi (heure de Paris), gains du jour remis à zéro à minuit. */
    void verifier() {
        LocalDate jour = LocalDate.now(KSEconomy.PARIS);
        boolean change = false;
        if (!jour.equals(jourGains)) {
            jourGains = jour;
            gains.clear();
            change = true;
        }
        if (!semaineDu(jour).equals(semaine) || offres.isEmpty()) {
            if (base.isEmpty()) {
                return;
            }
            semaine = semaineDu(jour);
            tirer();
            change = true;
        }
        if (change) {
            sauver();
        }
    }

    private void tirer() {
        offres.clear();
        ThreadLocalRandom hasard = ThreadLocalRandom.current();
        int parGamme = Math.max(1, plugin.getConfig().getInt("rachats.objets-par-gamme", 2));
        Set<String> prises = new HashSet<>();
        for (int g = 0; g <= BORNES.length; g++) {
            Map<String, List<Entree>> familles = new HashMap<>();
            for (Entree e : base.values()) {
                if (tirable(e) && gamme(e.prix()) == g) {
                    familles.computeIfAbsent(e.famille(), f -> new ArrayList<>()).add(e);
                }
            }
            List<String> noms = new ArrayList<>(familles.keySet());
            Collections.sort(noms);
            Collections.shuffle(noms, hasard);
            // une famille déjà tirée dans une autre gamme passe après les autres
            noms.sort((a, b) -> Boolean.compare(prises.contains(a), prises.contains(b)));
            for (int k = 0; k < parGamme && k < noms.size(); k++) {
                List<Entree> choix = familles.get(noms.get(k));
                offres.add(offre(choix.get(hasard.nextInt(choix.size())), hasard));
                prises.add(noms.get(k));
            }
        }
        StringBuilder journal = new StringBuilder("Rachats de la semaine " + semaine + " :");
        for (Offre o : offres) {
            journal.append(' ').append(o.quantite()).append(" x ").append(o.entree().id()).append(" = ")
                    .append(o.points()).append(" ;");
        }
        plugin.getLogger().info(journal.toString());
    }

    /** Prix de la semaine (barème à plus ou moins 25 %) puis lot de la gamme de ce prix. */
    private Offre offre(Entree e, ThreadLocalRandom hasard) {
        double ecart = Math.max(0, Math.min(90, plugin.getConfig().getDouble("rachats.variation-pourcent", 25))) / 100;
        double brut = e.prix() * (1 + (hasard.nextDouble() * 2 - 1) * ecart);
        double unite = brut >= 1 ? Math.rint(brut) : brut;
        int g = gamme(unite);
        if (g >= LOTS.length || e.pile() <= 1) {
            return new Offre(e, unite, 1, Math.max(1, Math.round(unite)));
        }
        int quantite = (int) Math.max(1, Math.floor(LOTS[g] / unite + 1e-9));
        return new Offre(e, unite, quantite, Math.max(1, Math.round(quantite * unite)));
    }

    // ------------------------------------------------------------------ quota

    private long gagne(UUID joueur, String id) {
        Map<String, Long> duJoueur = gains.get(joueur);
        return duJoueur == null ? 0 : duJoueur.getOrDefault(id, 0L);
    }

    /** Quota du jour par objet pour ce joueur, ou -1 tant que sa cagnotte ne dépasse pas le seuil. */
    private long quota(UUID joueur) {
        return quota(joueur, KSEconomy.solde(joueur));
    }

    private long quota(UUID joueur, long solde) {
        if (solde <= plugin.getConfig().getLong("rachats.quota.seuil", 25000)) {
            return -1;
        }
        double pourcent = plugin.estMasque(joueur)
                ? plugin.getConfig().getDouble("rachats.quota.pourcent-masque", 7.5)
                : plugin.getConfig().getDouble("rachats.quota.pourcent", 5.0);
        return (long) Math.floor(solde * pourcent / 100);
    }

    // ------------------------------------------------------------------ menus

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    private Component titre() {
        return t("rachats.titre", "<gold><bold>Rachats de la semaine");
    }

    private String pourcent(UUID joueur) {
        double valeur = plugin.estMasque(joueur) ? plugin.getConfig().getDouble("rachats.quota.pourcent-masque", 7.5)
                : plugin.getConfig().getDouble("rachats.quota.pourcent", 5.0);
        return valeur == Math.rint(valeur) ? String.valueOf((long) valeur) : String.valueOf(valeur).replace('.', ',');
    }

    private Component ligneQuota(Player joueur) {
        long quota = quota(joueur.getUniqueId());
        if (quota < 0) {
            return t("rachats.sans-quota", "<gray>Aucun quota tant que ta cagnotte ne dépasse pas <seuil>.", "seuil",
                    KSEconomy.points(plugin.getConfig().getLong("rachats.quota.seuil", 25000)));
        }
        return t("rachats.quota", "<gray>Quota du jour : <white><quota></white> par objet (<pourcent> % de ta "
                + "cagnotte).", "quota", KSEconomy.points(quota), "pourcent", pourcent(joueur.getUniqueId()));
    }

    void ouvrir(Player joueur) {
        verifier();
        List<Component> corps = new ArrayList<>();
        corps.add(t("rachats.intro", "<white>Le serveur rachète ces objets jusqu'à dimanche soir (minuit, heure de "
                + "Paris). Nouveau tirage chaque lundi."));
        corps.add(t("menu.solde", "<white>Solde : <green><bold><solde></bold>", "solde",
                KSEconomy.points(KSEconomy.solde(joueur.getUniqueId()))));
        corps.add(ligneQuota(joueur));
        List<ActionButton> boutons = new ArrayList<>();
        for (Offre o : offres) {
            int possede = compter(joueur, o.entree());
            boutons.add(gui.button(
                    t("rachats.bouton", "<quantite> x <objet> : <green><points>", "quantite", o.quantite(), "objet",
                            nom(o.entree()), "points", KSEconomy.nombre(o.points())),
                    t("rachats.bouton-info", "<gray>Tu en as <white><n></white> (<lots> lot(s))", "n", possede, "lots",
                            possede / o.quantite()),
                    p -> detail(p, o)));
        }
        if (offres.isEmpty()) {
            corps.add(t("rachats.vide", "<red>Aucun rachat cette semaine."));
        }
        gui.open(joueur, titre(), corps, List.of(), boutons,
                gui.button(t("gui.retour", "<gray>Retour"), null, plugin.menuEconomie()::ouvrir), 2);
        lang.saveIfNeeded();
    }

    private void detail(Player joueur, Offre o) {
        if (!offres.contains(o)) {
            ouvrir(joueur);
            return;
        }
        UUID id = joueur.getUniqueId();
        int possede = compter(joueur, o.entree());
        int lots = possede / o.quantite();
        List<Component> corps = new ArrayList<>();
        corps.add(t("rachats.detail-lot", "<white>Le serveur achète <yellow><quantite> x <objet></yellow> pour "
                + "<green><points></green>.", "quantite", o.quantite(), "objet", nom(o.entree()), "points",
                KSEconomy.points(o.points())));
        corps.add(t("rachats.detail-inventaire", "<gray>Dans ton inventaire : <white><n></white>, soit <white><lots>"
                + "</white> lot(s).", "n", possede, "lots", lots));
        long quota = quota(id);
        if (quota >= 0) {
            corps.add(t("rachats.detail-quota", "<gray>Gagné aujourd'hui avec cet objet : <white><gagne></white> sur "
                    + "<white><quota></white>.", "gagne", KSEconomy.nombre(gagne(id, o.entree().id())), "quota",
                    KSEconomy.points(quota)));
        } else {
            corps.add(ligneQuota(joueur));
        }
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("rachats.vendre-un", "<green>Vendre 1 lot"),
                t("rachats.vendre-un-info", "<gray>+ <points>", "points", KSEconomy.points(o.points())),
                p -> vendre(p, o, false)));
        if (lots > 1) {
            boutons.add(gui.button(t("rachats.vendre-tout", "<green>Tout vendre"),
                    t("rachats.vendre-tout-info", "<gray><lots> lots, dans la limite du quota", "lots", lots),
                    p -> vendre(p, o, true)));
        }
        gui.open(joueur, titre(), corps, List.of(), boutons,
                gui.button(t("gui.retour", "<gray>Retour"), null, this::ouvrir), 1);
        lang.saveIfNeeded();
    }

    private void vendre(Player joueur, Offre o, boolean tout) {
        verifier();
        if (!offres.contains(o)) {
            gui.notice(joueur, titre(), t("rachats.termine", "<red>Ce rachat est terminé : nouveau tirage."),
                    this::ouvrir);
            return;
        }
        UUID id = joueur.getUniqueId();
        String objet = o.entree().id();
        int lots = compter(joueur, o.entree()) / o.quantite();
        if (lots < 1) {
            gui.notice(joueur, titre(), t("rachats.manque", "<red>Il te faut <quantite> x <objet> dans ton inventaire.",
                    "quantite", o.quantite(), "objet", nom(o.entree())), p -> detail(p, o));
            lang.saveIfNeeded();
            return;
        }
        // Lots vendus : la vente qui fait dépasser le quota passe, on ne bloque qu'ensuite. Le quota suit la
        // cagnotte, qui grossit à chaque lot.
        long solde = KSEconomy.solde(id);
        long deja = gagne(id, objet);
        int vendus = 0;
        while (vendus < lots) {
            long gain = (long) vendus * o.points();
            long quota = quota(id, solde + gain);
            if (quota >= 0 && deja + gain >= quota) {
                break;
            }
            vendus++;
            if (!tout) {
                break;
            }
        }
        long total = (long) vendus * o.points();
        if (vendus > 0) {
            retirer(joueur, o.entree(), vendus * o.quantite());
            KSEconomy.crediter(id, total);
            gains.computeIfAbsent(id, u -> new HashMap<>()).merge(objet, total, Long::sum);
        }
        if (vendus == 0) {
            gui.notice(joueur, titre(), t("rachats.quota-atteint", "<red>Quota du jour atteint pour cet objet "
                    + "(<quota>). Reviens demain.", "quota", KSEconomy.points(quota(id))), p -> detail(p, o));
            lang.saveIfNeeded();
            return;
        }
        sauver();
        plugin.getLogger().info(joueur.getName() + " vend " + (vendus * o.quantite()) + " x " + objet + " pour "
                + total + " points (rachats de la semaine).");
        gui.notice(joueur, titre(), t("rachats.vendu", "<white>Vendu : <yellow><nombre> x <objet></yellow> pour "
                + "<green><points></green>.", "nombre", vendus * o.quantite(), "objet", nom(o.entree()), "points",
                KSEconomy.points(total)), p -> detail(p, o));
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ sauvegarde

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        semaine = yaml.getString("semaine");
        ConfigurationSection liste = yaml.getConfigurationSection("offres");
        if (liste != null) {
            for (String cle : liste.getKeys(false)) {
                Entree e = base.get(liste.getString(cle + ".id", ""));
                int quantite = liste.getInt(cle + ".quantite");
                long points = liste.getLong(cle + ".points");
                if (e != null && !e.cible().equals("-") && quantite > 0 && points > 0) {
                    offres.add(new Offre(e, liste.getDouble(cle + ".unite"), quantite, points));
                }
            }
        }
        String jour = yaml.getString("gains-du");
        jourGains = jour == null ? null : LocalDate.parse(jour);
        ConfigurationSection joueurs = yaml.getConfigurationSection("gains");
        if (joueurs != null) {
            for (String uuid : joueurs.getKeys(false)) {
                ConfigurationSection objets = joueurs.getConfigurationSection(uuid);
                try {
                    Map<String, Long> duJoueur = new HashMap<>();
                    for (String objet : objets.getKeys(false)) {
                        duJoueur.put(objet, objets.getLong(objet));
                    }
                    gains.put(UUID.fromString(uuid), duJoueur);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Gains ignorés (uuid invalide) : " + uuid);
                }
            }
        }
    }

    void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("semaine", semaine);
        for (int i = 0; i < offres.size(); i++) {
            Offre o = offres.get(i);
            String cle = "offres." + i;
            yaml.set(cle + ".id", o.entree().id());
            yaml.set(cle + ".unite", o.unite());
            yaml.set(cle + ".quantite", o.quantite());
            yaml.set(cle + ".points", o.points());
        }
        yaml.set("gains-du", jourGains == null ? null : jourGains.toString());
        gains.forEach((uuid, duJoueur) -> {
            ConfigurationSection section = yaml.createSection("gains." + uuid);
            duJoueur.forEach(section::set);
        });
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer rachats.yml : " + e.getMessage());
        }
    }
}
