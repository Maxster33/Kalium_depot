package fr.kalium.decapitator;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Strider;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.ZombieNautilus;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_Decapitator (cahier des charges : catégorie 1 « Contenu survie », LeKiwi06, 29/09/2026) : les mobs lâchent leur
 * tête.
 *
 * - Chute : 1 % quand un joueur tue le mob (Butin sans effet) ; rien pour un mob né d'un spawner classique (vanilla ou
 *   KS_Spawners) ; les mobs des trial spawners en donnent. Ni joueur, ni Ender Dragon (absents de la table).
 * - Une tête par variante visible, par état (en colère, pollinisée, frigorifié, hurleuse, en charge, chargé) et pour
 *   chaque bébé ; villageois et zombie-villageois : une tête par métier (le bébé n'a pas de métier).
 * - Têtes de joueur à texture (table tetes.txt dans le jar), nom « Tête de ... » blanc non italique, marquées (id).
 *   Posées puis cassées (outil, explosion, piston...), elles redonnent exactement le même objet : toute tête du plugin
 *   qui apparaît au sol sans marqueur est reconstruite (reconnue au profil de la tête, propre à chaque id).
 * - /tetes (opérateurs) : menu de toutes les têtes, pour les vérifier en jeu.
 *
 * Autres plugins : creerTete(id), idTete(objet), ids(), tetesDe(créature) (KS_Crafts, KS_KaliumGive).
 */
public final class KSDecapitator extends JavaPlugin implements Listener {

    /** Chance de chute (réponse de LeKiwi06 : tous les mobs à 1 %). */
    private static final double CHANCE = 0.01;

    /** Une tête de la table. */
    record Tete(String cle, String id, String nom, String empreinte, UUID uuid) {
    }

    /** Clé technique (calculée sur le mob) -> tête ; id -> tête ; uuid du profil -> tête. Ordre de tetes.txt. */
    private static final Map<String, Tete> PAR_CLE = new LinkedHashMap<>();
    private static final Map<String, Tete> PAR_ID = new LinkedHashMap<>();
    private static final Map<UUID, Tete> PAR_UUID = new LinkedHashMap<>();

    private static NamespacedKey marqueur;

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "tete");
        try {
            chargerTable();
        } catch (IOException | RuntimeException e) {
            getLogger().severe("tetes.txt illisible, plugin désactivé : " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new MenuTetes(), this);
        // 1.1.0 : tête de wither squelette sale, endommagée, désactivée (réparée par les crafts de KS_Crafts).
        getServer().getPluginManager().registerEvents(new Defauts(this), this);
        getCommand("tetes").setExecutor(this);
        getLogger().info(PAR_ID.size() + " têtes chargées.");
    }

    // ------------------------------------------------------------------ table des têtes

    /** tetes.txt : « clé;nom;empreinte » par ligne, # pour les commentaires. */
    private void chargerTable() throws IOException {
        PAR_CLE.clear();
        PAR_ID.clear();
        PAR_UUID.clear();
        try (InputStream in = getResource("tetes.txt")) {
            if (in == null) {
                throw new IOException("absent du jar");
            }
            BufferedReader lecteur = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                ligne = ligne.strip();
                if (ligne.isEmpty() || ligne.startsWith("#")) {
                    continue;
                }
                String[] champs = ligne.split(";");
                if (champs.length != 3) {
                    throw new IOException("ligne invalide : " + ligne);
                }
                String id = idDepuisNom(champs[1]);
                UUID uuid = UUID.nameUUIDFromBytes(("ks_decapitator:" + id).getBytes(StandardCharsets.UTF_8));
                Tete tete = new Tete(champs[0], id, champs[1], champs[2], uuid);
                if (PAR_CLE.putIfAbsent(tete.cle(), tete) != null || PAR_ID.putIfAbsent(id, tete) != null) {
                    throw new IOException("clé ou nom en double : " + ligne);
                }
                PAR_UUID.put(uuid, tete);
            }
        }
    }

    /** « Tête de bébé mouton rouge » -> « bebe_mouton_rouge » (sans accents, id de /kaliumgive). */
    static String idDepuisNom(String nom) {
        String sansTete = nom.replaceFirst("^Tête d(e |')", "");
        String sansAccents = Normalizer.normalize(sansTete, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    // ------------------------------------------------------------------ objets (autres plugins)

    /** Une tête (id : voir ids()), ou null si l'id est inconnu. Nécessite que le plugin soit activé. */
    public static ItemStack creerTete(String id) {
        Tete tete = PAR_ID.get(id);
        return tete == null ? null : creer(tete);
    }

    private static ItemStack creer(Tete tete) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profil = Bukkit.createProfile(tete.uuid(), null);
        String textures = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/"
                + tete.empreinte() + "\"}}}";
        profil.setProperty(new ProfileProperty("textures",
                Base64.getEncoder().encodeToString(textures.getBytes(StandardCharsets.UTF_8))));
        meta.setPlayerProfile(profil);
        meta.displayName(Component.text(tete.nom(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.STRING, tete.id());
        item.setItemMeta(meta);
        if (Defauts.concernee(tete.cle())) {
            Defauts.appliquer(item, new java.util.LinkedHashSet<>(Defauts.NOMS.keySet()));
        }
        return item;
    }

    // ------------------------------------------------------------------ 1.1.0 : tête de wither squelette à réparer

    /** 1.1.0 (KS_Crafts) : la tête de wither squelette du plugin (abîmée) ? */
    public static boolean estTeteWitherSquelette(ItemStack item) {
        Tete tete = tete(item);
        return tete != null && Defauts.concernee(tete.cle());
    }

    /** 1.1.0 (KS_Crafts) : défauts restants (« sale », « endommagee », « desactivee ») de la tête de wither squelette. */
    public static java.util.Set<String> defautsTete(ItemStack item) {
        return estTeteWitherSquelette(item) ? Defauts.lire(item) : java.util.Set.of();
    }

    /**
     * 1.1.0 (KS_Crafts) : la tête réparée de ce défaut (une seule) ; sans défaut restant : un vrai crâne de wither
     * squelette (qui invoque le Wither). Null si ce n'est pas la tête, ou si elle n'a pas ce défaut.
     */
    public static ItemStack reparer(ItemStack item, String defaut) {
        java.util.Set<String> defauts = defautsTete(item);
        if (!defauts.remove(defaut)) {
            return null;
        }
        if (defauts.isEmpty()) {
            return new ItemStack(Material.WITHER_SKELETON_SKULL);
        }
        ItemStack r = item.asOne();
        Defauts.appliquer(r, defauts);
        return r;
    }

    /** Id de la tête, ou null si ce n'est pas une tête du plugin (marqueur, ou profil pour une tête reposée). */
    public static String idTete(ItemStack item) {
        Tete tete = tete(item);
        return tete == null ? null : tete.id();
    }

    private static Tete tete(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
            return null;
        }
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(marqueur, PersistentDataType.STRING);
        if (id != null) {
            return PAR_ID.get(id);
        }
        PlayerProfile profil = meta.getPlayerProfile();
        return profil == null || profil.getId() == null ? null : PAR_UUID.get(profil.getId());
    }

    /** Ids de toutes les têtes, dans l'ordre de la table. */
    public static List<String> ids() {
        return List.copyOf(PAR_ID.keySet());
    }

    /** Toutes les têtes d'une créature (variantes, états et bébés compris), ex. pour une recette. */
    public static List<ItemStack> tetesDe(EntityType type) {
        String base = type.getKey().getKey();
        List<ItemStack> tetes = new ArrayList<>();
        for (Tete tete : PAR_CLE.values()) {
            if (tete.cle().equals(base) || tete.cle().startsWith(base + ".")) {
                tetes.add(creer(tete));
            }
        }
        return tetes;
    }

    // ------------------------------------------------------------------ clé du mob tué

    /**
     * Clé technique du mob : &lt;créature&gt;[.&lt;variante&gt;][.&lt;état&gt;...][.baby], avec les noms du jeu
     * (ex. sheep.red.baby, wolf.ashen.angry). Une clé absente de tetes.txt ne donne pas de tête (ex. shulker coloré,
     * bébé noyé, lapin tueur : textures pas encore trouvées ou mob absent de la survie).
     */
    static String cle(LivingEntity mob) {
        StringBuilder cle = new StringBuilder(mob.getType().getKey().getKey());
        boolean bebe = mob instanceof Ageable ageable && !ageable.isAdult();
        switch (mob) {
            case Axolotl a -> ajouter(cle, a.getVariant());
            case Cat c -> ajouter(cle, c.getCatType());
            case Chicken c -> ajouter(cle, c.getVariant());
            case MushroomCow m -> ajouter(cle, m.getVariant());
            case Cow c -> ajouter(cle, c.getVariant());
            case Pig p -> ajouter(cle, p.getVariant());
            case Frog f -> ajouter(cle, f.getVariant());
            case CopperGolem g -> ajouter(cle, g.getWeatheringState());
            case Fox f -> ajouter(cle, f.getFoxType());
            case Horse h -> ajouter(cle, h.getColor());
            case Llama l -> ajouter(cle, l.getColor());
            case Panda p -> ajouter(cle, p.getCombinedGene());
            case Parrot p -> ajouter(cle, p.getVariant());
            case Rabbit r -> ajouter(cle, "Toast".equals(nomDonne(r)) ? "toast" : r.getRabbitType());
            case Sheep s -> ajouter(cle, "jeb_".equals(nomDonne(s)) ? "jeb" : s.getColor());
            case Shulker s -> {
                if (s.getColor() != null) {
                    ajouter(cle, s.getColor());
                }
            }
            case Wolf w -> {
                ajouter(cle, w.getVariant());
                if (w.isAngry()) {
                    cle.append(".angry");
                }
            }
            case ZombieNautilus n -> ajouter(cle, n.getVariant());
            case Bee b -> {
                if (b.hasNectar()) {
                    cle.append(".pollinated");
                }
                if (b.getAnger() > 0) {
                    cle.append(".angry");
                }
            }
            case Strider s -> {
                if (s.isShivering()) {
                    cle.append(".shivering");
                }
            }
            case Goat g -> {
                if (g.isScreaming()) {
                    cle.append(".screaming");
                }
            }
            case Vex v -> {
                if (v.isCharging()) {
                    cle.append(".charging");
                }
            }
            case Creeper c -> {
                if (c.isPowered()) {
                    cle.append(".charged");
                }
            }
            // Le bébé villageois n'a pas de métier : une seule tête de bébé.
            case ZombieVillager z -> {
                if (!bebe) {
                    ajouter(cle, z.getVillagerProfession());
                }
            }
            case Villager v -> {
                if (!bebe) {
                    ajouter(cle, v.getProfession());
                }
            }
            default -> {
            }
        }
        if (bebe) {
            cle.append(".baby");
        }
        return cle.toString();
    }

    /** Ajoute « .variante » : clé du registre du jeu, ou nom de l'énumération en minuscules, ou texte. */
    private static void ajouter(StringBuilder cle, Object variante) {
        if (variante == null) {
            return;
        }
        String nom = variante instanceof Keyed keyed ? keyed.getKey().getKey()
                : variante instanceof Enum<?> e ? e.name().toLowerCase(Locale.ROOT) : variante.toString();
        cle.append('.').append(nom);
    }

    /** Nom donné au mob (étiquette), ou null. */
    private static String nomDonne(LivingEntity mob) {
        Component nom = mob.customName();
        return nom == null ? null : PlainTextComponentSerializer.plainText().serialize(nom);
    }

    // ------------------------------------------------------------------ chute de la tête

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob instanceof Player || mob.getKiller() == null
                || mob.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        Tete tete = PAR_CLE.get(cle(mob));
        if (tete != null && ThreadLocalRandom.current().nextDouble() < CHANCE) {
            event.getDrops().add(creer(tete));
        }
    }

    /**
     * Tête reposée puis cassée : le jeu la redonne avec son nom et sa texture mais sans le marqueur du plugin. Toute
     * tête du plugin qui apparaît au sol est reconstruite à l'identique (quelle que soit la façon de la casser).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        Tete tete = tete(item);
        if (tete != null && !item.getItemMeta().getPersistentDataContainer().has(marqueur)) {
            ItemStack neuve = creer(tete);
            neuve.setAmount(item.getAmount());
            // 1.1.0 : tête de wither squelette reposée : défauts du bloc cassé (sinon tous).
            if (Defauts.concernee(tete.cle())) {
                Defauts.appliquer(neuve, Defauts.pourTeteAuSol(event.getLocation()));
            }
            event.getEntity().setItemStack(neuve);
        }
    }

    // ------------------------------------------------------------------ /tetes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage(Component.text("Commande réservée aux joueurs.", NamedTextColor.RED));
            return true;
        }
        MenuTetes.ouvrir(joueur, 0);
        return true;
    }

    /** Têtes de la table, dans l'ordre (menu). */
    static List<ItemStack> toutes() {
        List<ItemStack> tetes = new ArrayList<>();
        PAR_ID.values().forEach(tete -> tetes.add(creer(tete)));
        return tetes;
    }
}
