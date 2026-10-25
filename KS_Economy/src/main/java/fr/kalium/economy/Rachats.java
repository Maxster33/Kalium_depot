package fr.kalium.economy;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
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
import org.bukkit.plugin.Plugin;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1.3.0 (LeKiwi06, 04/10/2026) : barème des prix d'Event (rachats.csv, dans le jar) et « Rachats de la semaine » : le
 * serveur rachète aux joueurs 10 objets tirés chaque semaine (lundi, heure de Paris).
 *
 * - Tirage (1.4.0, LeKiwi06, 09/10/2026 : plus de quota par gamme de prix) : 10 familles au hasard, puis un objet de
 *   chaque famille (les variantes d'un objet comptent pour un seul : têtes, cuivre, sapin...).
 *   Jamais d'objet issu d'une dimension dont KS_Dimensions ferme les portails (Nether, End) : ceux d'une dimension
 *   fermée après le tirage sont remplacés.
 * - Prix de la semaine : prix du barème à plus ou moins 25 %, arrondi à l'unité dès 1 émeraude.
 * - Lot : la quantité (arrondie à l'unité inférieure) qui approche 1 / 10 / 64 / 320 émeraudes selon la gamme du prix
 *   de la semaine, payée au nombre entier le plus proche ; à l'unité à partir de 100 et pour un objet non empilable.
 * - Quota par objet, par joueur et par jour (1.8.2, LeKiwi06) : au-dessus de 2 500 points gagnés en vendant au serveur
 *   (compteur du compte, qui ne baisse jamais : retirer ses émeraudes n'y change rien), un objet ne rapporte pas plus
 *   de 0,5 % de ces gains (0,75 % si le solde est masqué) ; la vente qui fait dépasser passe, les suivantes sont
 *   refusées. Avant : 25 000 de cagnotte (le solde), 5 % et 7,5 %.
 */
final class Rachats implements CommandExecutor {

    /** Une ligne du barème. cible : m, potion:TYPE, pdc:espace:clé:type[:valeur], ou - (jamais tirée). */
    record Entree(String id, String famille, double prix, String cible, String nom, int pile, Material materiel,
                  String dimensions) {
    }

    /**
     * Un objet racheté cette semaine : quantité par lot et points payés par lot. bareme (1.8.0) : prix du barème au
     * moment du tirage ; NaN pour une offre tirée avant la 1.8.0 ou gardée par un marchand ambulant.
     */
    record Offre(Entree entree, double unite, int quantite, long points, double bareme) {
    }

    /** Bornes des gammes de prix : moins de 0,1 ; 0,1 à 1 ; 1 à 10 ; 10 à 100 ; 100 et plus. */
    private static final double[] BORNES = {0.1, 1, 10, 100};
    private static final long[] LOTS = {1, 10, 64, 320};
    /** Utilisations restantes d'un Changeur de Biome (KS_BiomeChanger 1.2.0) et celles d'un neuf. */
    private static final NamespacedKey UTILISATIONS_CHANGEUR = new NamespacedKey("ks_biomechanger", "utilisations");
    private static final int UTILISATIONS_CHANGEUR_NEUF = 10;

    private final KSEconomy plugin;
    private final Lang lang;
    private final Menus menus;
    private final File fichier;
    private final Map<String, Entree> base = new LinkedHashMap<>();
    private final List<Offre> offres = new ArrayList<>();
    /** 1.8.0 : objets au prix changé depuis le barème des versions 1.3.0 à 1.7.0 (rachats-modifies.txt du jar). */
    private final Set<String> modifies = new HashSet<>();
    /** Points gagnés aujourd'hui : joueur -> id de l'objet -> points. */
    private final Map<UUID, Map<String, Long>> gains = new HashMap<>();
    private String semaine;
    private LocalDate jourGains;

    Rachats(KSEconomy plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.menus = plugin.menus();
        this.fichier = new File(plugin.getDataFolder(), "rachats.yml");
        chargerBase();
        chargerModifies();
        charger();
        verifier();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ barème

    private void chargerModifies() {
        try (InputStream in = plugin.getResource("rachats-modifies.txt")) {
            if (in == null) {
                return;
            }
            BufferedReader lecteur = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                if (!ligne.startsWith("#") && !ligne.isBlank()) {
                    modifies.add(ligne.trim());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("rachats-modifies.txt illisible : " + e.getMessage());
        }
    }

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
                base.put(c[0], new Entree(c[0], c[1], Double.parseDouble(c[2]), cible, c[4], pile, materiel,
                        c.length > 6 ? c[6] : ""));
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

    static int gamme(double prix) {
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

    Component nom(Entree e) {
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
            // 1.10.1 : un Changeur de Biome entamé n'est pas racheté (sans compteur : neuf, comme dans KS_BiomeChanger).
            Integer utilisations = marques.get(UTILISATIONS_CHANGEUR, PersistentDataType.INTEGER);
            if (utilisations != null && utilisations < UTILISATIONS_CHANGEUR_NEUF) {
                return false;
            }
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

    static int compter(Player joueur, Entree e) {
        int total = 0;
        for (ItemStack objet : joueur.getInventory().getStorageContents()) {
            if (correspond(objet, e)) {
                total += objet.getAmount();
            }
        }
        return total;
    }

    static void retirer(Player joueur, Entree e, int nombre) {
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
        if (remplacerModifies()) {
            change = true;
        }
        if (remplacerFermes()) {
            change = true;
        }
        if (change) {
            sauver();
        }
    }

    /** Portails de cette dimension ouverts dans KS_Dimensions (clé de son config.yml) ; vrai sans ce plugin. */
    private boolean ouverte(String cle) {
        Plugin dimensions = plugin.getServer().getPluginManager().getPlugin("KS_Dimensions");
        return dimensions == null || !dimensions.isEnabled() || dimensions.getConfig().getBoolean(cle, true);
    }

    /** Faux pour un objet issu d'une dimension fermée (KS_Dimensions) : il serait impossible à obtenir. */
    private static boolean permis(Entree e, boolean nether, boolean end) {
        return (nether || !e.dimensions().contains("nether")) && (end || !e.dimensions().contains("end"));
    }

    /**
     * 1.4.0 : complète les offres jusqu'au nombre voulu (10), sans gamme de prix : une famille au hasard, puis un de
     * ses objets. Une famille déjà présente ne revient que s'il n'en reste pas d'autre.
     */
    private void completer(boolean nether, boolean end) {
        completer(offres, Math.max(1, plugin.getConfig().getInt("rachats.objets", 10)), nether, end);
    }

    /** Le même tirage, dans la liste donnée (rachats de la semaine, ou d'un marchand ambulant depuis la 1.7.0). */
    private void completer(List<Offre> offres, int nombre, boolean nether, boolean end) {
        completer(offres, nombre, nether, end, Set.of());
    }

    /** 1.8.0 : le même tirage, sans les objets de « exclus » (ceux qu'on vient de retirer des offres). */
    private void completer(List<Offre> offres, int nombre, boolean nether, boolean end, Set<String> exclus) {
        ThreadLocalRandom hasard = ThreadLocalRandom.current();
        Set<String> prises = new HashSet<>();
        Set<String> offerts = new HashSet<>(exclus);
        for (Offre o : offres) {
            prises.add(o.entree().famille());
            offerts.add(o.entree().id());
        }
        Map<String, List<Entree>> familles = new HashMap<>();
        for (Entree e : base.values()) {
            if (tirable(e) && permis(e, nether, end) && !offerts.contains(e.id())) {
                familles.computeIfAbsent(e.famille(), f -> new ArrayList<>()).add(e);
            }
        }
        List<String> noms = new ArrayList<>(familles.keySet());
        Collections.sort(noms);
        Collections.shuffle(noms, hasard);
        noms.sort((a, b) -> Boolean.compare(prises.contains(a), prises.contains(b)));
        for (int k = 0; offres.size() < nombre && k < noms.size(); k++) {
            List<Entree> choix = familles.get(noms.get(k));
            offres.add(offre(choix.get(hasard.nextInt(choix.size())), hasard));
        }
    }

    /**
     * 1.7.0 (marchand ambulant) : un tirage à part, selon les règles des rachats de la semaine (familles, objets tirables,
     * dimensions ouvertes, prix à plus ou moins 25 %, lot de la gamme du prix).
     */
    List<Offre> tirage(int nombre) {
        List<Offre> liste = new ArrayList<>();
        completer(liste, nombre, ouverte("nether-portals-enabled"), ouverte("end-portals-enabled"));
        return liste;
    }

    /** 1.7.0 : un rachat déjà tiré, retrouvé par l'id de son objet (null si l'objet n'est plus dans le barème). */
    Offre offre(String id, double unite, int quantite, long points) {
        Entree e = base.get(id);
        return e == null || e.cible().equals("-") || quantite <= 0 || points <= 0 ? null
                : new Offre(e, unite, quantite, points, Double.NaN);
    }

    private void journal(String debut, boolean nether, boolean end) {
        StringBuilder journal = new StringBuilder(debut + (nether ? "" : " (Nether fermé)")
                + (end ? "" : " (End fermé)") + " :");
        for (Offre o : offres) {
            journal.append(' ').append(o.quantite()).append(" x ").append(o.entree().id()).append(" = ")
                    .append(o.points()).append(" ;");
        }
        plugin.getLogger().info(journal.toString());
    }

    private void tirer() {
        offres.clear();
        boolean nether = ouverte("nether-portals-enabled");
        boolean end = ouverte("end-portals-enabled");
        completer(nether, end);
        journal("Rachats de la semaine " + semaine, nether, end);
    }

    /**
     * 1.4.0 : une dimension fermée après le tirage (ou un tirage fait quand KS_Dimensions n'était pas lisible) : ses
     * objets sont remplacés, pour qu'aucun objet d'une dimension fermée ne reste proposé. Vrai si une offre a changé.
     */
    private boolean remplacerFermes() {
        boolean nether = ouverte("nether-portals-enabled");
        boolean end = ouverte("end-portals-enabled");
        if (!offres.removeIf(o -> !permis(o.entree(), nether, end))) {
            return false;
        }
        completer(nether, end);
        journal("Rachats de la semaine " + semaine + " : objets d'une dimension fermée remplacés", nether, end);
        return true;
    }

    /**
     * 1.8.0 (demande de LeKiwi06 : « refaire un tirage pour les items qui ont été modifiés, on tire un nouvel objet à la
     * place ») : une offre dont le prix du barème a changé depuis son tirage (nouveau rachats.csv) est retirée et
     * remplacée par un nouveau tirage, jamais par le même objet. Vrai si une offre a changé.
     */
    private boolean remplacerModifies() {
        Set<String> retires = new LinkedHashSet<>();
        offres.removeIf(o -> modifiee(o) && retires.add(o.entree().id()));
        // les offres gardées qui datent d'avant la 1.8.0 reçoivent le prix actuel : un prochain changement sera vu
        boolean marquees = false;
        for (int i = 0; i < offres.size(); i++) {
            Offre o = offres.get(i);
            if (Double.isNaN(o.bareme())) {
                offres.set(i, new Offre(o.entree(), o.unite(), o.quantite(), o.points(), o.entree().prix()));
                marquees = true;
            }
        }
        if (retires.isEmpty()) {
            return marquees;
        }
        boolean nether = ouverte("nether-portals-enabled");
        boolean end = ouverte("end-portals-enabled");
        completer(offres, Math.max(1, plugin.getConfig().getInt("rachats.objets", 10)), nether, end, retires);
        journal("Rachats de la semaine " + semaine + " : objets au prix modifié remplacés (" + String.join(", ", retires)
                + ")", nether, end);
        return true;
    }

    /**
     * Vrai si le prix du barème de l'objet n'est plus celui du tirage. Offre tirée avant la 1.8.0 (prix du barème non
     * enregistré) : vrai si l'objet est dans rachats-modifies.txt, ou si son prix de la semaine sort de l'écart permis
     * autour du prix actuel (un prix de 1 émeraude ou plus est arrondi à l'unité, d'où la marge de 0,5).
     */
    private boolean modifiee(Offre o) {
        double prix = o.entree().prix();
        if (!Double.isNaN(o.bareme())) {
            return Math.abs(o.bareme() - prix) > 1e-9;
        }
        if (modifies.contains(o.entree().id())) {
            return true;
        }
        double ecart = ecart();
        double marge = (o.unite() >= 1 ? 0.5 : 0) + 1e-9;
        return o.unite() < prix * (1 - ecart) - marge || o.unite() > prix * (1 + ecart) + marge;
    }

    private double ecart() {
        return Math.max(0, Math.min(90, plugin.getConfig().getDouble("rachats.variation-pourcent", 25))) / 100;
    }

    /** Prix de la semaine (barème à plus ou moins 25 %) puis lot de la gamme de ce prix. */
    private Offre offre(Entree e, ThreadLocalRandom hasard) {
        double ecart = ecart();
        double brut = e.prix() * (1 + (hasard.nextDouble() * 2 - 1) * ecart);
        double unite = brut >= 1 ? Math.rint(brut) : brut;
        int g = gamme(unite);
        if (g >= LOTS.length || e.pile() <= 1) {
            return new Offre(e, unite, 1, Math.max(1, Math.round(unite)), e.prix());
        }
        int quantite = (int) Math.max(1, Math.floor(LOTS[g] / unite + 1e-9));
        return new Offre(e, unite, quantite, Math.max(1, Math.round(quantite * unite)), e.prix());
    }

    // ------------------------------------------------------------------ quota

    private long gagne(UUID joueur, String id) {
        Map<String, Long> duJoueur = gains.get(CompteTest.de(joueur));
        return duJoueur == null ? 0 : duJoueur.getOrDefault(id, 0L);
    }

    /** Quota du jour par objet pour ce joueur, ou -1 tant que ses points gagnés ne dépassent pas le seuil. */
    private long quota(UUID joueur) {
        return quota(joueur, KSEconomy.gagne(joueur));
    }

    private long quota(UUID joueur, long gagnes) {
        if (gagnes <= plugin.getConfig().getLong("rachats.quota.seuil", 2500)) {
            return -1;
        }
        double pourcent = plugin.estMasque(joueur)
                ? plugin.getConfig().getDouble("rachats.quota.pourcent-masque", 0.75)
                : plugin.getConfig().getDouble("rachats.quota.pourcent", 0.5);
        return (long) Math.floor(gagnes * pourcent / 100);
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
        return t("rachats.titre-coffre", "<dark_gray>Rachats de la semaine");
    }

    private String pourcent(UUID joueur) {
        double valeur = plugin.estMasque(joueur) ? plugin.getConfig().getDouble("rachats.quota.pourcent-masque", 0.75)
                : plugin.getConfig().getDouble("rachats.quota.pourcent", 0.5);
        return valeur == Math.rint(valeur) ? String.valueOf((long) valeur) : String.valueOf(valeur).replace('.', ',');
    }

    private Component ligneQuota(Player joueur) {
        long quota = quota(joueur.getUniqueId());
        if (quota < 0) {
            return t("rachats.sans-quota-gains", "<gray>Aucun quota avant <seuil> gagnés en rachats (toi : <gagne>).",
                    "seuil", KSEconomy.points(plugin.getConfig().getLong("rachats.quota.seuil", 2500)), "gagne",
                    KSEconomy.nombre(KSEconomy.gagne(joueur.getUniqueId())));
        }
        return t("rachats.quota-gains", "<gray>Quota du jour : <white><quota></white> par objet (<pourcent> % de tes "
                + "<gagne> gagnés en rachats).", "quota", KSEconomy.points(quota), "pourcent",
                pourcent(joueur.getUniqueId()), "gagne", KSEconomy.points(KSEconomy.gagne(joueur.getUniqueId())));
    }

    /**
     * 1.5.1 : l'apparence d'un objet custom du barème, créé par KS_KaliumGive (mêmes id_custom que /kaliumgive), sans
     * la marque de son plugin : l'objet du menu n'est qu'une image. Une étoile si l'objet ne peut pas être créé
     * (KS_KaliumGive ou le plugin de l'objet absent). Appel par réflexion : KS_KaliumGive dépend de plugins qui
     * dépendent de KS_Economy (KS_Jetons, KS_Elixir), une dépendance dans l'autre sens ferait une boucle.
     */
    private ItemStack custom(Entree e) {
        ItemStack objet = null;
        Plugin give = plugin.getServer().getPluginManager().getPlugin("KS_KaliumGive");
        if (give != null && give.isEnabled()) {
            try {
                objet = (ItemStack) give.getClass().getMethod("creer", String.class).invoke(null, e.id());
            } catch (ReflectiveOperationException | RuntimeException erreur) {
                plugin.getLogger().warning("Rachats : objet custom " + e.id() + " non créé : " + erreur);
            }
        }
        if (objet == null) {
            // Le bloc de charbon de bois (KS_Crafts, inconnu de KS_KaliumGive) est un bloc de charbon marqué.
            return new ItemStack(e.id().equals("bloc_charbon_de_bois") ? Material.COAL_BLOCK : Material.NETHER_STAR);
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

    /**
     * 1.5.0 : l'objet d'un rachat dans le menu (l'objet du jeu, la potion ; 1.5.1 : l'objet custom lui-même, avant
     * une étoile du Nether pour tous).
     */
    ItemStack icone(Offre o) {
        Entree e = o.entree();
        ItemStack objet;
        if (e.materiel() != null) {
            objet = new ItemStack(e.materiel());
        } else if (e.cible().startsWith("potion:")) {
            objet = new ItemStack(Material.POTION);
            if (objet.getItemMeta() instanceof PotionMeta potion) {
                try {
                    potion.setBasePotionType(org.bukkit.potion.PotionType.valueOf(
                            e.cible().substring(7).toUpperCase(Locale.ROOT)));
                    objet.setItemMeta(potion);
                } catch (IllegalArgumentException inconnue) {
                    // potion inconnue de cette version : une fiole sans effet
                }
            }
        } else {
            objet = custom(e);
        }
        objet.setAmount(Math.max(1, Math.min(o.quantite(), objet.getMaxStackSize())));
        return objet;
    }

    /** Cases des 10 rachats : deux rangées de 5, centrées. */
    private static final int[] PLACES = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    /** 1.5.0 : coffre ; un objet par rachat (la pile montre le lot), un clic ouvre la vente. */
    void ouvrir(Player joueur) {
        verifier();
        Contenant c = menus.menu(joueur, 6, titre(), "rachats");
        c.poser(4, Menus.objet(Material.CLOCK, t("rachats.info", "<gold>Rachats de la semaine"),
                t("rachats.intro", "<white>Le serveur rachète ces objets jusqu'à dimanche soir (minuit, heure de "
                        + "Paris). Nouveau tirage chaque lundi."),
                t("eco.solde", "<white>Solde : <green><solde>", "solde",
                        KSEconomy.points(KSEconomy.solde(joueur.getUniqueId()))),
                ligneQuota(joueur)), null);
        for (int i = 0; i < offres.size() && i < 36; i++) {
            Offre o = offres.get(i);
            int possede = compter(joueur, o.entree());
            int place = offres.size() <= PLACES.length ? PLACES[i] : 9 + i;
            c.poser(place, Contenant.objet(icone(o),
                    t("rachats.objet", "<yellow><quantite> x <objet>", "quantite", o.quantite(), "objet",
                            nom(o.entree())),
                    List.of(t("rachats.objet-prix", "<gray>Racheté <green><points></green> le lot", "points",
                                    KSEconomy.points(o.points())),
                            t("rachats.bouton-info", "<gray>Tu en as <white><n></white> (<lots> lot(s))", "n", possede,
                                    "lots", possede / o.quantite()),
                            t("rachats.objet-clic", "<dark_gray>Clic : vendre"))), p -> detail(p, o));
        }
        if (offres.isEmpty()) {
            c.poser(22, Contenant.objet(Material.BARRIER, t("rachats.vide", "<red>Aucun rachat cette semaine."),
                    List.of()), null);
        }
        menus.sortie(c, plugin.menuEconomie()::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** 1.5.0 : vente d'un objet : un lot, ou tout ce que l'inventaire et le quota permettent. */
    private void detail(Player joueur, Offre o) {
        if (!offres.contains(o)) {
            ouvrir(joueur);
            return;
        }
        UUID id = joueur.getUniqueId();
        int possede = compter(joueur, o.entree());
        int lots = possede / o.quantite();
        long quota = quota(id);
        Contenant c = menus.menu(joueur, 4, t("rachats.titre-vente", "<dark_gray>Vendre au serveur"), "rachat");
        c.poser(4, Menus.objet(icone(o),
                t("rachats.objet", "<yellow><quantite> x <objet>", "quantite", o.quantite(), "objet", nom(o.entree())),
                t("rachats.objet-prix", "<gray>Racheté <green><points></green> le lot", "points",
                        KSEconomy.points(o.points())),
                t("rachats.detail-inventaire", "<gray>Dans ton inventaire : <white><n></white>, soit <white><lots>"
                        + "</white> lot(s).", "n", possede, "lots", lots),
                quota >= 0 ? t("rachats.detail-quota", "<gray>Gagné aujourd'hui avec cet objet : <white><gagne></white> "
                        + "sur <white><quota></white>.", "gagne", KSEconomy.nombre(gagne(id, o.entree().id())), "quota",
                        KSEconomy.points(quota)) : ligneQuota(joueur)), null);
        c.poser(lots > 1 ? 11 : 13, Contenant.objet(Material.EMERALD, t("rachats.vendre-un", "<green>Vendre 1 lot"),
                List.of(t("rachats.vendre-un-info", "<gray>+ <points>", "points", KSEconomy.points(o.points())))),
                p -> vendre(p, o, false));
        if (lots > 1) {
            c.poser(15, Menus.objet(Material.EMERALD_BLOCK, t("rachats.vendre-tout", "<green>Tout vendre"),
                    t("rachats.vendre-tout-info", "<gray><lots> lots, dans la limite du quota", "lots", lots)),
                    p -> vendre(p, o, true));
        }
        menus.sortie(c, this::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private void vendre(Player joueur, Offre o, boolean tout) {
        verifier();
        if (!offres.contains(o)) {
            ouvrir(joueur);
            menus.message(joueur, t("rachats.termine", "<red>Ce rachat est terminé : nouveau tirage."), true);
            return;
        }
        UUID id = joueur.getUniqueId();
        String objet = o.entree().id();
        int lots = compter(joueur, o.entree()) / o.quantite();
        if (lots < 1) {
            menus.message(joueur, t("rachats.manque", "<red>Il te faut <quantite> x <objet> dans ton inventaire.",
                    "quantite", o.quantite(), "objet", nom(o.entree())), true);
            return;
        }
        // Lots vendus : la vente qui fait dépasser le quota passe, on ne bloque qu'ensuite. Le quota suit les points
        // gagnés (1.8.2 ; avant : la cagnotte), qui grossissent à chaque lot.
        long gagnes = KSEconomy.gagne(id);
        long deja = gagne(id, objet);
        int vendus = 0;
        while (vendus < lots) {
            long gain = (long) vendus * o.points();
            long quota = quota(id, gagnes + gain);
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
            KSEconomy.crediterVente(id, total);
            gains.computeIfAbsent(CompteTest.de(id), u -> new HashMap<>()).merge(objet, total, Long::sum);
        }
        if (vendus == 0) {
            menus.message(joueur, t("rachats.quota-atteint", "<red>Quota du jour atteint pour cet objet "
                    + "(<quota>). Reviens demain.", "quota", KSEconomy.points(quota(id))), true);
            return;
        }
        sauver();
        plugin.getLogger().info(joueur.getName() + " vend " + (vendus * o.quantite()) + " x " + objet + " pour "
                + total + " points (rachats de la semaine).");
        detail(joueur, o);
        menus.message(joueur, t("rachats.vendu", "<white>Vendu : <yellow><nombre> x <objet></yellow> pour "
                + "<green><points></green>.", "nombre", vendus * o.quantite(), "objet", nom(o.entree()), "points",
                KSEconomy.points(total)), false);
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
                    offres.add(new Offre(e, liste.getDouble(cle + ".unite"), quantite, points,
                            liste.getDouble(cle + ".bareme", Double.NaN)));
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
            if (!Double.isNaN(o.bareme())) {
                yaml.set(cle + ".bareme", o.bareme());
            }
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
