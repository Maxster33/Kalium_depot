package fr.kalium.mondetest;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.hanging.HangingEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * KS_MondeTest 1.2.0 (demande de LeKiwi06, 03/11/2026) : « le 1er deploiement ne les rend effectif qu'en monde test,
 * et une fois valide, on les rend accessible sur le monde survie, sinon des versions work in progress pourraient etre
 * utilisees ou annoncees trop tot alors qu'elles sont encore bugguees ».
 *
 * phases.yml : « en-test » et « valides », chacun nom -> date. Un nom est un plugin (« KS_Courrier ») ou une cle de
 * nouveaute (« KS_Contrats:demandes ») pour une mise a jour d'un plugin deja ouvert (Paper ne charge qu'une version
 * par serveur : on ne peut pas couper tout le plugin sans couper aussi l'ancien).
 * - Plugin en test (quarantaine, sans toucher a son code) : hors du monde de test, ses commandes sont refusees, ses
 *   ecouteurs ne recoivent pas les evenements situes dans un autre monde (joueur, bloc, entite, inventaire,
 *   vehicule, monde), ses recettes ne se fabriquent pas et sortent du livre de recettes, ses coffres ne s'ouvrent pas.
 *   Les evenements sans monde (demarrage, serveur) passent, et la deconnexion aussi (nettoyage). Pas couvert : ses
 *   taches repetees (a faire verifier par le plugin lui-meme, comme une cle).
 * - Cle : le plugin demande lui-meme, par sa classe PhaseTest (copiee, sans dependance), si elle est ouverte dans un
 *   monde (methode publique phaseOuverte de KsMondeTest). Une cle inconnue est mise en test a la premiere question.
 * - Un plugin dont le nom commence par un des « prefixes-auto » et absent des deux listes est mis en test tout seul :
 *   un oubli ne peut pas ouvrir un plugin en survie. Premier demarrage (pas de phases.yml) : tous les plugins presents
 *   sont valides, sauf « premiers-en-test ».
 * Validation : /mondetest valider <nom> (immediat, sans redemarrage), /mondetest retester <nom>, /mondetest phases.
 */
final class Phases implements Listener {

    private static final String EN_TEST = "en-test";
    private static final String VALIDES = "valides";

    private final KsMondeTest plugin;
    private final File fichier;
    /** Noms en minuscules -> [nom tel qu'ecrit, date]. */
    private final Map<String, String[]> enTest = new TreeMap<>();
    private final Map<String, String[]> valides = new TreeMap<>();

    Phases(KsMondeTest plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "phases.yml");
    }

    // ------------------------------------------------------------------ fichier

    synchronized void charger() {
        enTest.clear();
        valides.clear();
        if (!fichier.exists()) {
            List<String> premiers = plugin.getConfig().getStringList("premiers-en-test");
            for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
                boolean test = premiers.stream().anyMatch(n -> n.equalsIgnoreCase(p.getName()));
                (test ? enTest : valides).put(cle(p.getName()), new String[] {p.getName(), aujourdhui()});
            }
            for (String n : premiers) {
                enTest.putIfAbsent(cle(n), new String[] {n, aujourdhui()});
            }
            plugin.getLogger().info("phases.yml cree : " + valides.size() + " plugin(s) valide(s), en test : "
                    + noms(enTest) + ".");
            sauver();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        lire(yaml.getConfigurationSection(EN_TEST), enTest);
        lire(yaml.getConfigurationSection(VALIDES), valides);
        valides.keySet().removeAll(enTest.keySet());
    }

    private static void lire(ConfigurationSection section, Map<String, String[]> dans) {
        if (section != null) {
            for (String nom : section.getKeys(false)) {
                dans.put(cle(nom), new String[] {nom, section.getString(nom, "")});
            }
        }
    }

    private synchronized void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "KS_MondeTest : phases de deploiement (nom -> date). Un plugin ou une cle (Plugin:nouveaute) « en-test »",
                "ne marche que dans le monde de test. Ne pas modifier a la main serveur allume : /mondetest valider <nom>,",
                "/mondetest retester <nom>, /mondetest phases."));
        enTest.values().forEach(v -> yaml.set(EN_TEST + "." + v[0], v[1]));
        valides.values().forEach(v -> yaml.set(VALIDES + "." + v[0], v[1]));
        if (enTest.isEmpty()) {
            yaml.createSection(EN_TEST);
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("phases.yml non ecrit : " + e.getMessage());
        }
    }

    private static String cle(String nom) {
        return nom.trim().toLowerCase(Locale.ROOT);
    }

    private static String aujourdhui() {
        return LocalDate.now(ZoneId.of("Europe/Paris")).toString();
    }

    private static String noms(Map<String, String[]> m) {
        List<String> l = new ArrayList<>();
        m.values().forEach(v -> l.add(v[0]));
        return l.isEmpty() ? "-" : String.join(", ", l);
    }

    // ------------------------------------------------------------------ questions

    /** Plugin en quarantaine (en test en entier) ? */
    synchronized boolean pluginEnTest(String nom) {
        return nom != null && enTest.containsKey(cle(nom));
    }

    /**
     * Ouvert dans ce monde ? Toujours dans le monde de test ; ailleurs, non si le plugin ou la cle est en test. Une cle
     * (« Plugin:nouveaute ») jamais vue est mise en test.
     */
    synchronized boolean ouvert(String nom, World monde) {
        if (nom == null || nom.isBlank()) {
            return true;
        }
        if (KsMondeTest.TEST.equals(plugin.cote(monde))) {
            return true;
        }
        String c = cle(nom);
        int deuxPoints = c.indexOf(':');
        if (deuxPoints > 0 && enTest.containsKey(c.substring(0, deuxPoints))) {
            return false;
        }
        if (enTest.containsKey(c)) {
            return false;
        }
        if (deuxPoints > 0 && !valides.containsKey(c)) {
            enTest.put(c, new String[] {nom.trim(), aujourdhui()});
            sauver();
            plugin.getLogger().warning("Nouvelle cle « " + nom.trim() + " » : en test (monde de test seulement) "
                    + "jusqu'a /mondetest valider.");
            plugin.journalTexte("phases | nouvelle cle en test : " + nom.trim());
            return false;
        }
        return true;
    }

    /** Plugins nouveaux (prefixes-auto, absents des deux listes) : en test. */
    private synchronized void nouveauxPlugins() {
        List<String> prefixes = plugin.getConfig().getStringList("prefixes-auto");
        boolean change = false;
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            String c = cle(p.getName());
            if (enTest.containsKey(c) || valides.containsKey(c)
                    || prefixes.stream().noneMatch(pre -> p.getName().startsWith(pre))) {
                continue;
            }
            enTest.put(c, new String[] {p.getName(), aujourdhui()});
            change = true;
            plugin.getLogger().warning("Nouveau plugin " + p.getName() + " : en test (monde de test seulement) jusqu'a "
                    + "/mondetest valider " + p.getName() + ".");
            plugin.journalTexte("phases | nouveau plugin en test : " + p.getName());
        }
        if (change) {
            sauver();
        }
    }

    // ------------------------------------------------------------------ commandes

    /** /mondetest valider|retester <nom> ; renvoie le nom tel qu'enregistre, ou null si rien n'a change. */
    synchronized String valider(String nom, String par) {
        String c = cle(nom);
        String[] avant = enTest.remove(c);
        if (avant == null && valides.containsKey(c)) {
            return null;
        }
        String ecrit = avant != null ? avant[0] : nomDuPlugin(nom);
        valides.put(c, new String[] {ecrit, aujourdhui()});
        sauver();
        plugin.journalTexte(par + " | phases | valide : " + ecrit);
        envelopper();
        return ecrit;
    }

    synchronized String retester(String nom, String par) {
        String c = cle(nom);
        if (enTest.containsKey(c)) {
            return null;
        }
        String[] avant = valides.remove(c);
        String ecrit = avant != null ? avant[0] : nomDuPlugin(nom);
        enTest.put(c, new String[] {ecrit, aujourdhui()});
        sauver();
        plugin.journalTexte(par + " | phases | remis en test : " + ecrit);
        envelopper();
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            retirerRecettes(joueur);
        }
        return ecrit;
    }

    /** Orthographe exacte d'un plugin installe (sinon le nom tape). */
    private static String nomDuPlugin(String nom) {
        Plugin p = Bukkit.getPluginManager().getPlugin(nom.trim());
        return p != null ? p.getName() : nom.trim();
    }

    synchronized List<String> lignesEnTest() {
        List<String> l = new ArrayList<>();
        enTest.values().forEach(v -> l.add(v[0] + " (" + v[1] + ")"));
        return l;
    }

    synchronized List<String> nomsEnTest() {
        List<String> l = new ArrayList<>();
        enTest.values().forEach(v -> l.add(v[0]));
        return l;
    }

    synchronized List<String> nomsValides() {
        List<String> l = new ArrayList<>();
        valides.values().forEach(v -> l.add(v[0]));
        return l;
    }

    // ------------------------------------------------------------------ quarantaine des ecouteurs

    /** Ecouteur d'un plugin en test : l'evenement ne lui est passe que s'il a lieu dans le monde de test. */
    private final class Enveloppe extends RegisteredListener {
        final RegisteredListener original;

        Enveloppe(RegisteredListener original) {
            super(original.getListener(), original.getExecutor(), original.getPriority(), original.getPlugin(),
                    original.isIgnoringCancelled());
            this.original = original;
        }

        @Override
        public void callEvent(Event event) throws EventException {
            if (permis(event)) {
                original.callEvent(event);
            }
        }
    }

    /** Pose ou retire les enveloppes selon les phases (fil principal). */
    void envelopper() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::envelopper);
            return;
        }
        for (HandlerList liste : HandlerList.getHandlerLists()) {
            for (RegisteredListener rl : liste.getRegisteredListeners()) {
                if (rl instanceof Enveloppe e) {
                    if (!pluginEnTest(e.getPlugin().getName())) {
                        liste.unregister(e);
                        liste.register(e.original);
                    }
                } else if (rl.getPlugin() != plugin && pluginEnTest(rl.getPlugin().getName())) {
                    liste.unregister(rl);
                    liste.register(new Enveloppe(rl));
                }
            }
        }
    }

    private boolean permis(Event event) {
        if (event instanceof PlayerQuitEvent || event instanceof PlayerKickEvent) {
            return true;
        }
        if (event instanceof PlayerChangedWorldEvent e && KsMondeTest.TEST.equals(plugin.cote(e.getFrom()))) {
            return true; // le plugin apprend que le joueur quitte le monde de test
        }
        World monde = mondeDe(event);
        return monde == null || KsMondeTest.TEST.equals(plugin.cote(monde));
    }

    private static World mondeDe(Event event) {
        try {
            if (event instanceof PlayerEvent e) {
                return e.getPlayer().getWorld();
            }
            if (event instanceof BlockEvent e) {
                return e.getBlock().getWorld();
            }
            if (event instanceof EntityEvent e) {
                return e.getEntity().getWorld();
            }
            if (event instanceof InventoryEvent e) {
                return e.getView().getPlayer().getWorld();
            }
            if (event instanceof InventoryMoveItemEvent e) {
                return e.getSource().getLocation() != null ? e.getSource().getLocation().getWorld() : null;
            }
            if (event instanceof InventoryPickupItemEvent e) {
                return e.getItem().getWorld();
            }
            if (event instanceof VehicleEvent e) {
                return e.getVehicle().getWorld();
            }
            if (event instanceof HangingEvent e) {
                return e.getEntity().getWorld();
            }
            if (event instanceof WorldEvent e) {
                return e.getWorld();
            }
            if (event instanceof WeatherEvent e) {
                return e.getWorld();
            }
        } catch (RuntimeException ignored) {
            // evenement incomplet : traite comme sans monde
        }
        return null;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        nouveauxPlugins();
        envelopper();
        List<String> l = nomsEnTest();
        plugin.getLogger().info("En test (monde de test seulement) : " + (l.isEmpty() ? "-" : String.join(", ", l)));
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            nouveauxPlugins();
            envelopper();
        });
    }

    /** Ecouteurs enregistres apres le demarrage : nouvelle passe de temps en temps. */
    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::envelopper, 200L, 200L);
    }

    // ------------------------------------------------------------------ recettes

    private boolean recetteFermee(Recipe recette, World monde) {
        if (!(recette instanceof Keyed k) || KsMondeTest.TEST.equals(plugin.cote(monde))) {
            return false;
        }
        Plugin p = pluginDuNamespace(k.getKey().getNamespace());
        return p != null && pluginEnTest(p.getName());
    }

    private static Plugin pluginDuNamespace(String namespace) {
        if (NamespacedKey.MINECRAFT.equals(namespace)) {
            return null;
        }
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p.getName().toLowerCase(Locale.ROOT).equals(namespace)) {
                return p;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null && recetteFermee(event.getRecipe(), event.getView().getPlayer().getWorld())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (recetteFermee(event.getRecipe(), event.getWhoClicked().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        Recipe r = event.getInventory().getRecipe();
        if (r != null && recetteFermee(r, event.getView().getPlayer().getWorld())) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCook(BlockCookEvent event) {
        if (event.getRecipe() != null && recetteFermee(event.getRecipe(), event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    /** Livre de recettes : celles des plugins en test sont retirees en survie (rien ne s'annonce avant l'heure). */
    void retirerRecettes(Player joueur) {
        if (KsMondeTest.TEST.equals(plugin.cote(joueur.getWorld()))) {
            return;
        }
        List<NamespacedKey> cles = new ArrayList<>();
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            if (it.next() instanceof Keyed k && joueur.hasDiscoveredRecipe(k.getKey())) {
                Plugin p = pluginDuNamespace(k.getKey().getNamespace());
                if (p != null && pluginEnTest(p.getName())) {
                    cles.add(k.getKey());
                }
            }
        }
        if (!cles.isEmpty()) {
            joueur.undiscoverRecipes(cles);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (joueur.isOnline()) {
                retirerRecettes(joueur);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        retirerRecettes(event.getPlayer());
    }
}
