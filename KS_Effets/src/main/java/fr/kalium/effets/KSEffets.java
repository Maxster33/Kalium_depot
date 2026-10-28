package fr.kalium.effets;

import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * KS_Effets (cahier des charges : Atouts_Alcools_Aliments.md, LeKiwi06, 28/10/2026, partie 3.1) : serveur Kixster.
 *
 * - Moteur des effets custom des autres plugins (KS_Alcools, KS_Cuisine, KS_Atouts) : chaque plugin déclare ses
 *   effets ({@link #declarer}) puis les donne ({@link #donner}) ; KS_Effets tient les minuteurs (en secondes, ne
 *   s'écoulent qu'en jeu, gardés à la déconnexion et au redémarrage dans effets.yml) et prévient le plugin à la fin
 *   ({@link EffetFinEvent}). Ce que fait l'effet (musique, lumière...) reste dans le plugin qui l'a déclaré : il
 *   demande {@link #actif} quand il en a besoin. Un effet dont le temps est gardé ailleurs (atout sur un objet) passe
 *   par {@link #fournisseur} pour apparaître dans /effects.
 * - Comme les effets du jeu, un effet custom s'efface à la mort et au lait (config.yml) ; donné à nouveau, il garde
 *   le plus long des deux temps.
 * - /effects : menu en coffre des effets en cours, du jeu et custom, avec leur temps restant ; suivre un effet en
 *   barre de boss ou en barre d'action, un seul effet par barre ({@link MenuEffets}, {@link Suivi}).
 * - Monde de test (KS_MondeTest) : tout est rangé sous le compte de test du joueur ({@link CompteTest}).
 */
public final class KSEffets extends JavaPlugin implements Listener {

    /** Identifiant d'un effet custom : « plugin:nom », sans point (le point sépare les clés de effets.yml). */
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+:[a-z0-9_-]+");

    /** Ce qui s'affiche dans /effects pour un effet custom déclaré. */
    record Type(Component nom, ItemStack icone) {
    }

    /** Un effet en cours, du jeu ou custom, tel que /effects et les barres l'affichent. restant : secondes, -1 sans limite. */
    record Entree(String cle, Component nom, ItemStack icone, int restant) {
    }

    private static KSEffets instance;

    private Lang lang;
    private Registre registre;
    private Suivi suivi;
    private MenuEffets menu;
    private final Map<String, Type> types = new LinkedHashMap<>();
    private final Map<String, ToIntFunction<Player>> fournisseurs = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        lang = new Lang(this);
        registre = new Registre(this);
        registre.charger();
        suivi = new Suivi(this);
        menu = new MenuEffets(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::seconde, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, () -> registre.enregistrer(false), 1200L, 1200L);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        if (suivi != null) {
            suivi.toutCacher();
        }
        if (registre != null) {
            registre.enregistrer(true);
        }
        instance = null;
    }

    // ------------------------------------------------------------------ API des autres plugins

    /**
     * Déclare un effet custom (à l'activation du plugin qui le possède) : son nom et son icône dans /effects et les
     * barres de suivi.
     *
     * @param id « plugin:nom » en minuscules, ex. « ks_alcools:rhum »
     */
    public static void declarer(String id, Component nom, ItemStack icone) {
        verifier(id);
        if (instance != null) {
            instance.types.put(id, new Type(nom, icone == null ? new ItemStack(Material.GLASS_BOTTLE) : icone.clone()));
        }
    }

    /**
     * Effet dont le temps est tenu par un autre plugin (ex. atout sur l'objet porté) : affiché dans /effects tant que
     * la fonction rend autre chose que 0. Valeur : secondes restantes, -1 = actif sans limite de temps, 0 = inactif.
     * L'effet doit aussi être déclaré ({@link #declarer}).
     */
    public static void fournisseur(String id, ToIntFunction<Player> restant) {
        verifier(id);
        if (instance != null) {
            instance.fournisseurs.put(id, restant);
        }
    }

    /** Donne un effet custom pour ce nombre de secondes ; s'il est déjà actif, le plus long des deux temps est gardé. */
    public static void donner(Player joueur, String id, int secondes) {
        verifier(id);
        if (instance == null || secondes <= 0) {
            return;
        }
        Registre.Compte c = instance.registre.compte(compte(joueur), true);
        Registre.Minuteur m = c.effets.get(id);
        if (m == null) {
            c.effets.put(id, new Registre.Minuteur(secondes, secondes));
        } else if (secondes > m.restant) {
            m.restant = secondes;
            m.total = Math.max(m.total, secondes);
        }
        instance.registre.modifie();
    }

    /** Retire un effet custom (événement de fin, cause RETIRE). Faux s'il n'était pas actif. */
    public static boolean retirer(Player joueur, String id) {
        return instance != null && instance.finir(joueur, id, EffetFinEvent.Cause.RETIRE);
    }

    /** L'effet custom est-il actif pour ce joueur (minuteur en cours ou fournisseur actif) ? */
    public static boolean actif(Player joueur, String id) {
        return restant(joueur, id) != 0;
    }

    /** Secondes restantes d'un effet custom : 0 s'il est inactif, -1 s'il est actif sans limite (fournisseur). */
    public static int restant(Player joueur, String id) {
        if (instance == null) {
            return 0;
        }
        Registre.Compte c = instance.registre.compte(compte(joueur), false);
        Registre.Minuteur m = c == null ? null : c.effets.get(id);
        if (m != null) {
            return m.restant;
        }
        ToIntFunction<Player> f = instance.fournisseurs.get(id);
        return f == null ? 0 : f.applyAsInt(joueur);
    }

    /** Les effets custom actifs de ce joueur (minuteurs et fournisseurs). */
    public static Set<String> actifs(Player joueur) {
        Set<String> ids = new LinkedHashSet<>();
        if (instance == null) {
            return ids;
        }
        Registre.Compte c = instance.registre.compte(compte(joueur), false);
        if (c != null) {
            ids.addAll(c.effets.keySet());
        }
        instance.fournisseurs.forEach((id, f) -> {
            if (f.applyAsInt(joueur) != 0) {
                ids.add(id);
            }
        });
        return ids;
    }

    private static void verifier(String id) {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Identifiant d'effet invalide (attendu « plugin:nom ») : " + id);
        }
    }

    private static UUID compte(Player joueur) {
        return CompteTest.de(joueur.getUniqueId());
    }

    // ------------------------------------------------------------------ minuteurs

    /** Chaque seconde : minuteurs des joueurs en jeu, barres de suivi, menus ouverts. */
    private void seconde() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            Registre.Compte c = registre.compte(compte(joueur), false);
            if (c != null && !c.effets.isEmpty()) {
                List<String> finis = new ArrayList<>();
                for (Map.Entry<String, Registre.Minuteur> e : c.effets.entrySet()) {
                    if (--e.getValue().restant <= 0) {
                        finis.add(e.getKey());
                    }
                }
                finis.forEach(id -> finir(joueur, id, EffetFinEvent.Cause.EXPIRE));
                registre.modifie();
            }
            suivi.maj(joueur, c);
            menu.rafraichir(joueur);
        }
    }

    private boolean finir(Player joueur, String id, EffetFinEvent.Cause cause) {
        Registre.Compte c = registre.compte(compte(joueur), false);
        if (c == null || c.effets.remove(id) == null) {
            return false;
        }
        registre.modifie();
        getServer().getPluginManager().callEvent(new EffetFinEvent(joueur, id, cause));
        return true;
    }

    private void toutFinir(Player joueur, EffetFinEvent.Cause cause) {
        Registre.Compte c = registre.compte(compte(joueur), false);
        if (c != null) {
            for (String id : new ArrayList<>(c.effets.keySet())) {
                finir(joueur, id, cause);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (getConfig().getBoolean("effacer-a-la-mort", true) && !event.isCancelled()) {
            toutFinir(event.getEntity(), EffetFinEvent.Cause.MORT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.MILK_BUCKET && getConfig().getBoolean("effacer-au-lait", true)) {
            toutFinir(event.getPlayer(), EffetFinEvent.Cause.LAIT);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        suivi.cacher(event.getPlayer());
        registre.enregistrer(false);
    }

    // ------------------------------------------------------------------ effets en cours (menu, barres)

    /** Effets en cours du joueur : ceux du jeu, puis les custom à minuteur, puis ceux des fournisseurs. */
    List<Entree> entrees(Player joueur) {
        List<Entree> liste = new ArrayList<>();
        for (PotionEffect effet : joueur.getActivePotionEffects()) {
            liste.add(entreeJeu(effet));
        }
        Registre.Compte c = registre.compte(compte(joueur), false);
        if (c != null) {
            c.effets.forEach((id, m) -> liste.add(entreeCustom(id, m.restant)));
        }
        fournisseurs.forEach((id, f) -> {
            int restant = f.applyAsInt(joueur);
            if (restant != 0 && (c == null || !c.effets.containsKey(id))) {
                liste.add(entreeCustom(id, restant));
            }
        });
        return liste;
    }

    /** Un effet en cours par sa clé (« minecraft:speed » ou id custom), ou null s'il n'est pas actif. */
    Entree entree(Player joueur, String cle) {
        NamespacedKey key = NamespacedKey.fromString(cle);
        PotionEffectType type = key == null ? null : Registry.MOB_EFFECT.get(key);
        if (type != null) {
            PotionEffect effet = joueur.getPotionEffect(type);
            return effet == null ? null : entreeJeu(effet);
        }
        int restant = restant(joueur, cle);
        return restant == 0 ? null : entreeCustom(cle, restant);
    }

    /** Durée totale connue d'un effet custom à minuteur (barre de boss), 0 sinon. */
    int total(Player joueur, String cle) {
        Registre.Compte c = registre.compte(compte(joueur), false);
        Registre.Minuteur m = c == null ? null : c.effets.get(cle);
        return m == null ? 0 : m.total;
    }

    private Entree entreeJeu(PotionEffect effet) {
        PotionEffectType type = effet.getType();
        NamedTextColor couleur = type.getCategory() == PotionEffectTypeCategory.BENEFICIAL ? NamedTextColor.BLUE
                : type.getCategory() == PotionEffectTypeCategory.HARMFUL ? NamedTextColor.RED : NamedTextColor.GRAY;
        Component nom = Component.translatable(type).color(couleur);
        if (effet.getAmplifier() > 0) {
            nom = nom.append(Component.text(" " + romain(effet.getAmplifier() + 1)));
        }
        ItemStack icone = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) icone.getItemMeta();
        meta.setColor(type.getColor());
        icone.setItemMeta(meta);
        int restant = effet.isInfinite() ? -1 : Math.max(1, (effet.getDuration() + 19) / 20);
        return new Entree(type.getKey().toString(), nom, icone, restant);
    }

    private Entree entreeCustom(String id, int restant) {
        Type type = types.get(id);
        Component nom = type == null ? Component.text(id, NamedTextColor.GRAY) : type.nom();
        ItemStack icone = type == null ? new ItemStack(Material.GLASS_BOTTLE) : type.icone().clone();
        return new Entree(id, nom, icone, restant);
    }

    private static String romain(int n) {
        String[] chiffres = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= 10 ? chiffres[n] : String.valueOf(n);
    }

    /** « 45 s », « 2 min 05 s », « 1 h 05 min ». */
    static String duree(int secondes) {
        int h = secondes / 3600;
        int m = secondes / 60 % 60;
        int s = secondes % 60;
        if (h > 0) {
            return h + " h " + String.format("%02d", m) + " min";
        }
        if (m > 0) {
            return m + " min " + String.format("%02d", s) + " s";
        }
        return s + " s";
    }

    /** Temps restant affiché : durée, ou « sans limite ». */
    Component temps(int restant) {
        return restant < 0 ? t("temps.illimite", "sans limite") : Component.text(duree(restant));
    }

    // ------------------------------------------------------------------ commande, textes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage(t("commande.joueur", "<red>Commande réservée aux joueurs."));
            return true;
        }
        menu.ouvrir(joueur);
        return true;
    }

    Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    Registre registre() {
        return registre;
    }

    Suivi suivi() {
        return suivi;
    }

    UUID compteDe(Player joueur) {
        return compte(joueur);
    }
}
