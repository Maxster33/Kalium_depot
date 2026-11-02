package fr.kalium.boussole;

import fr.kalium.biomechanger.KSBiomeChanger;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BiomeSearchResult;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Boussole (LeKiwi06, 01-02/11/2026, cahier local Blocs_Cultures_Objets.md, section 2) : la boussole du voyageur.
 *
 * - Craft sans forme : une boussole et un Changeur de Biome neuf (KS_BiomeChanger, 10 utilisations sur 10 : choix de
 *   LeKiwi06 du 02/11) ; la recette est celle d'un Changeur tel que creerChangeur() le fabrique.
 * - Clic droit avec la boussole en main : menu en coffre (MenuBoussole) avec les biomes que la génération du monde du
 *   joueur peut produire (Terralith compris ; overworld, Nether et End, choix du 02/11), noms de biomes.yml.
 * - Choix d'un biome : recherche de Paper (World#locateNearestBiome, la même que /locate biome : bruit de génération
 *   seulement, aucun chunk chargé ni généré) dans RAYON blocs autour du joueur, en asynchrone ; une recherche à la fois
 *   par joueur. Trouvé : la boussole devient magnétisée vers ce point (comme une boussole sur une magnétite, sans
 *   magnétite à garder) ; Geyser la montre en boussole magnétisée aux joueurs Bedrock.
 * - Recherches illimitées ; temps de recharge de RECHARGE minutes par joueur entre deux recherches réussies (choix
 *   du 02/11), gardé dans les données du joueur (survit au redémarrage). Une recherche sans résultat ne compte pas.
 */
public final class KSBoussole extends JavaPlugin implements Listener {

    private static NamespacedKey marqueur;
    /** Biome visé par une boussole réglée (« minecraft:plains »). */
    private static NamespacedKey cleBiome;
    /** Données du joueur : date de sa dernière recherche réussie (millisecondes). */
    private NamespacedKey cleRecharge;
    private NamespacedKey cleRecette;
    private static Lang lang;

    private int rayon;
    private long rechargeMs;
    private YamlConfiguration noms;
    private MenuBoussole menu;
    private final Set<UUID> enCours = new HashSet<>();
    private final Map<UUID, Long> derniereOuverture = new HashMap<>();

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "boussole_voyageur");
        cleBiome = new NamespacedKey(this, "biome");
        cleRecharge = new NamespacedKey(this, "derniere_recherche");
        cleRecette = new NamespacedKey(this, "boussole_voyageur");
        saveDefaultConfig();
        // Valeurs par défaut dans le code : une clé absente du config.yml du serveur garde le réglage voulu.
        rayon = getConfig().getInt("rayon", 2000);
        rechargeMs = Math.max(0, getConfig().getLong("recharge-minutes", 15)) * 60_000L;
        noms = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("biomes.yml"), StandardCharsets.UTF_8));
        lang = new Lang(this);
        menu = new MenuBoussole(this, lang);
        getServer().getPluginManager().registerEvents(this, this);

        ShapelessRecipe recette = new ShapelessRecipe(cleRecette, creerBoussole());
        recette.addIngredient(new RecipeChoice.ExactChoice(new ItemStack(Material.COMPASS)));
        recette.addIngredient(new RecipeChoice.ExactChoice(KSBiomeChanger.creerChangeur()));
        Bukkit.addRecipe(recette);
        getLogger().info("Boussole du voyageur : rayon " + rayon + " blocs, recharge " + rechargeMs / 60_000L + " min.");
    }

    @Override
    public void onDisable() {
        Bukkit.removeRecipe(cleRecette);
        if (lang != null) {
            lang.saveIfNeeded();
        }
    }

    int rayon() {
        return rayon;
    }

    // ------------------------------------------------------------------ objet

    /** Une boussole du voyageur, pas encore réglée. */
    public static ItemStack creerBoussole() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.c("objet-nom", "<white>Boussole du voyageur").decoration(TextDecoration.ITALIC, false));
        // Une seule par case : régler une boussole d'une pile les réglerait toutes.
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.BYTE, (byte) 1);
        meta.lore(description(null));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean estBoussole(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(marqueur, PersistentDataType.BYTE);
    }

    /** Clé du biome visé par cette boussole, ou null si elle n'est pas réglée. */
    static String cible(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(cleBiome, PersistentDataType.STRING);
    }

    private static List<Component> description(Component biome) {
        List<Component> lignes = new ArrayList<>();
        if (biome != null) {
            lignes.add(lang.c("objet-cible", "<gray>Pointe vers : <white><biome>", "biome", biome));
        }
        lignes.add(lang.c("objet-usage", "<gray>Clic droit : chercher un biome"));
        lignes.replaceAll(l -> l.decoration(TextDecoration.ITALIC, false));
        return lignes;
    }

    // ------------------------------------------------------------------ biomes

    /** Biomes que la génération de ce monde peut produire, triés par nom français. */
    List<Biome> biomes(World monde) {
        BiomeProvider fournisseur = monde.getBiomeProvider();
        if (fournisseur == null) {
            fournisseur = monde.vanillaBiomeProvider();
        }
        List<Biome> liste = new ArrayList<>(fournisseur.getBiomes(monde));
        liste.sort(Comparator.comparing(this::nom));
        return liste;
    }

    /** Nom français du biome (biomes.yml, vanilla et Terralith) ; absent : tiré de son identifiant. */
    String nom(Biome biome) {
        Key cle = biome.key();
        String nom = noms.getString(cle.namespace() + "." + cle.value());
        if (nom != null) {
            return nom;
        }
        String brut = cle.value().replace('_', ' ');
        return Character.toUpperCase(brut.charAt(0)) + brut.substring(1);
    }

    /** Nom français d'un biome donné par sa clé (« minecraft:plains »). */
    String nom(String cle) {
        NamespacedKey key = NamespacedKey.fromString(cle);
        Biome biome = key == null ? null : Registry.BIOME.get(key);
        return biome == null ? cle : nom(biome);
    }

    // ------------------------------------------------------------------ recharge

    /** Temps de recharge restant du joueur, en millisecondes (0 : prêt). */
    long recharge(Player joueur) {
        Long derniere = joueur.getPersistentDataContainer().get(cleRecharge, PersistentDataType.LONG);
        return derniere == null ? 0 : Math.max(0, derniere + rechargeMs - System.currentTimeMillis());
    }

    /** « 12 min 05 s » ou « 40 s ». */
    static String duree(long ms) {
        long s = (ms + 999) / 1000;
        return s >= 60 ? (s / 60) + " min " + String.format("%02d", s % 60) + " s" : s + " s";
    }

    /** 2000 -> « 2 000 ». */
    static String milliers(int n) {
        String s = Integer.toString(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    boolean cherche(Player joueur) {
        return enCours.contains(joueur.getUniqueId());
    }

    // ------------------------------------------------------------------ utilisation

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || event.getHand() == null || !estBoussole(event.getItem())) {
            return;
        }
        // Un coffre, une porte... s'ouvrent comme avec une boussole ordinaire ; une magnétite ne règle pas la boussole.
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getType() != Material.LODESTONE
                && event.getClickedBlock().getType().isInteractable() && !event.getPlayer().isSneaking()) {
            return;
        }
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        // Un clic peut arriver en double (Bedrock, ou boussole dans les deux mains) : un seul menu par seconde.
        long maintenant = System.currentTimeMillis();
        Long dernier = derniereOuverture.put(joueur.getUniqueId(), maintenant);
        if (dernier != null && maintenant - dernier < 1000) {
            return;
        }
        menu.ouvrir(joueur, 0);
    }

    /** Choix d'un biome dans le menu : tout est revérifié ici. */
    void chercher(Player joueur, Biome biome) {
        if (cherche(joueur)) {
            Contenant.message(joueur, lang.c("deja-en-cours", "<red>Une recherche est déjà en cours."), true);
            return;
        }
        long reste = recharge(joueur);
        if (reste > 0) {
            Contenant.message(joueur, lang.c("recharge", "<red>Ta boussole se recharge encore : <temps>.",
                    "temps", duree(reste)), true);
            return;
        }
        if (place(joueur) < 0) {
            Contenant.message(joueur, lang.c("sans-boussole", "<red>Tu n'as plus de boussole du voyageur sur toi."), true);
            return;
        }
        Contenant.fermer(joueur);
        World monde = joueur.getWorld();
        Location origine = joueur.getLocation().clone();
        UUID id = joueur.getUniqueId();
        enCours.add(id);
        Contenant.message(joueur, lang.c("recherche", "<gray>La boussole cherche : <white><biome></white>...",
                "biome", nom(biome)), false);
        // Recherche dans le bruit de génération (aucun chunk touché) : hors du fil principal pour ne pas le ralentir.
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            BiomeSearchResult resultat = null;
            boolean erreur = false;
            try {
                resultat = monde.locateNearestBiome(origine, rayon, 32, 64, biome);
            } catch (RuntimeException e) {
                erreur = true;
                getLogger().warning("Recherche de " + biome.key() + " impossible : " + e);
            }
            BiomeSearchResult trouve = resultat;
            boolean echec = erreur;
            Bukkit.getScheduler().runTask(this, () -> terminer(id, monde, biome, trouve, echec));
        });
    }

    private void terminer(UUID id, World monde, Biome biome, BiomeSearchResult resultat, boolean erreur) {
        enCours.remove(id);
        Player joueur = Bukkit.getPlayer(id);
        if (joueur == null || erreur) {
            if (joueur != null) {
                Contenant.message(joueur, lang.c("erreur", "<red>La recherche a échoué, réessaie plus tard."), true);
            }
            return;
        }
        if (resultat == null) {
            Contenant.message(joueur, lang.c("introuvable", "<red>Aucun biome <biome> à moins de <rayon> blocs.",
                    "biome", nom(biome), "rayon", milliers(rayon)), true);
            return;
        }
        if (joueur.getWorld() != monde) {
            Contenant.message(joueur, lang.c("autre-monde", "<red>Tu as changé de monde pendant la recherche."), true);
            return;
        }
        int place = place(joueur);
        if (place < 0) {
            Contenant.message(joueur, lang.c("sans-boussole", "<red>Tu n'as plus de boussole du voyageur sur toi."), true);
            return;
        }
        PlayerInventory inventaire = joueur.getInventory();
        ItemStack boussole = inventaire.getItem(place);
        CompassMeta meta = (CompassMeta) boussole.getItemMeta();
        meta.setLodestone(resultat.getLocation());
        meta.setLodestoneTracked(false);
        meta.getPersistentDataContainer().set(cleBiome, PersistentDataType.STRING, biome.key().asString());
        meta.lore(description(Component.text(nom(biome))));
        boussole.setItemMeta(meta);
        inventaire.setItem(place, boussole);
        joueur.getPersistentDataContainer().set(cleRecharge, PersistentDataType.LONG, System.currentTimeMillis());
        Contenant.message(joueur, lang.c("trouve", "<green>Ta boussole pointe vers : <white><biome></white>.",
                "biome", nom(biome)), false);
    }

    /** Case de la boussole à régler : main, autre main, puis le reste de l'inventaire ; -1 si aucune. */
    private static int place(Player joueur) {
        PlayerInventory inventaire = joueur.getInventory();
        int main = inventaire.getHeldItemSlot();
        if (estBoussole(inventaire.getItem(main))) {
            return main;
        }
        if (estBoussole(inventaire.getItemInOffHand())) {
            return 40;
        }
        for (int i = 0; i < 36; i++) {
            if (estBoussole(inventaire.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Biome visé par la boussole tenue (pour le menu), ou null. */
    String cibleTenue(Player joueur) {
        int place = place(joueur);
        if (place < 0) {
            return null;
        }
        return cible(joueur.getInventory().getItem(place));
    }
}
