package fr.kalium.jukeboxboat;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.entity.Leashable;
import io.papermc.paper.datacomponent.item.JukeboxPlayable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.JukeboxSong;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_JukeboxBoat (demande de LeKiwi06, 13/10/2026 : « un bateau très similaire au bateau avec coffres, mais là il peut
 * lire les disques de notre inventaire »).
 *
 * Objet : un bateau ou un radeau vanilla marqué, nommé « Bateau à jukebox » (recette dans KS_Crafts : bateau + jukebox).
 * Posé, c'est un bateau simple marqué, avec un jukebox affiché à l'arrière (modèle 3D de bloc, passager du bateau : il
 * prend la 2e place ; les joueurs Bedrock ne le voient pas, choix de LeKiwi06). Cassé, il redonne le bateau à jukebox.
 *
 * Menu des disques (coffre de KLM_Menu) : à pied, accroupi + clic droit sur le bateau ; à bord, touche de saut. Un clic
 * sur un disque de son inventaire le range dans le bateau et lance la musique, qui sort du bateau et le suit, pour tous
 * les joueurs proches ; un clic sur le disque du bateau arrête la musique et le rend. Le disque reste dans le bateau
 * sans joueur à bord, et tombe si le bateau est cassé.
 *
 * 1.1.0 (LeKiwi06, 13/10/2026 : « un bateau avec coffre et jukebox ? on ne peut pas monter dedans, mais on peut le
 * tracter avec une laisse [...] le jukebox lirait en automatique les disques présents dans le coffre en boucle ») :
 * bateau à coffre et jukebox, en plus du premier. C'est un bateau à coffre vanilla marqué (27 cases, tout objet), le
 * jukebox affiché à la place du siège : personne n'y monte. Accroupi + clic droit ouvre le coffre (comportement du
 * jeu) ; un clic droit normal alterne lecture et pause. En lecture, les disques du coffre sont joués l'un après
 * l'autre, dans l'ordre des cases, en boucle, tant qu'un joueur est à portée de la musique ; ils restent dans le coffre.
 *
 * 1.1.0, correction (LeKiwi06 : bateau tenu en laisse par un Happy Ghast, « il m'en fait descendre direct et laisse
 * derrière un jukebox fantôme immobile et invulnérable ») : c'est l'anti-triche (GrimAC, « Simulation ») qui éjecte
 * tous les passagers du bateau, jukebox compris. Un jukebox éjecté remonte maintenant dans son bateau au tick suivant,
 * ou disparaît s'il n'a plus de bateau (onEjecter). Le jukebox ne quitte plus le bateau quand un joueur monte : le
 * jeu place de lui-même le joueur en premier passager (lu dans le code du jeu 26.3, Entity.addPassenger).
 *
 * 1.1.1 (LeKiwi06 : « il faudrait que le jukebox soit à peu près 30% plus gros dans le bateau, je le trouve trop
 * petit par rapport au coffre », sur les deux bateaux) : jukebox de 0,72 bloc au lieu de 0,55, replacé pour ne pas
 * entrer dans le coffre ni dépasser de la poupe (valeurs par défaut de regler() et du config.yml).
 *
 * Autres plugins : creerBateau() (KS_Crafts), estBateau().
 */
public final class KSJukeboxBoat extends JavaPlugin implements Listener {

    /** Recul du 2e passager d'un bateau par rapport à son centre (valeur du jeu). */
    private static final float PLACE_ARRIERE = -0.6f;

    /** Avance du passager d'un bateau à coffre par rapport à son centre (valeur du jeu). */
    private static final float PLACE_COFFRE = 0.15f;

    /** 1.1.0 : rythme de la lecture en boucle, et silence entre deux disques, en ticks. */
    private static final long PAS_BOUCLE = 10;
    private static final long SILENCE = 20;

    /** Distance au-delà de laquelle le menu ne commande plus le bateau. */
    private static final double PORTEE = 8;

    private static KSJukeboxBoat instance;

    private NamespacedKey cleBateau;
    private NamespacedKey cleDisque;
    private NamespacedKey cleJukebox;
    /** 1.1.0, bateau à coffre : 1 = lecture (aussi sans la clé), 0 = pause. */
    private NamespacedKey cleLecture;
    /** 1.1.0, bateau à coffre : case du coffre où chercher le prochain disque. */
    private NamespacedKey clePiste;
    private Lang lang;

    /** Bateaux à jukebox présents dans les mondes chargés. */
    private final Map<UUID, Boat> bateaux = new HashMap<>();

    /** 1.1.0 : disque en cours de lecture dans chaque bateau à coffre (case, disque, tick de fin). */
    private final Map<UUID, Piste> pistes = new HashMap<>();

    private record Piste(int place, ItemStack disque, long fin) {
    }

    /** Ce qu'affiche un menu : le bateau qu'il commande. */
    private record Marque(UUID bateau) {
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        cleBateau = new NamespacedKey(this, "bateau");
        cleDisque = new NamespacedKey(this, "disque");
        cleJukebox = new NamespacedKey(this, "jukebox");
        cleLecture = new NamespacedKey(this, "lecture");
        clePiste = new NamespacedKey(this, "piste");
        lang = new Lang(this);
        getServer().getPluginManager().registerEvents(this, this);
        for (World monde : Bukkit.getWorlds()) {
            monde.getEntitiesByClass(Boat.class).forEach(this::accueillir);
            // Jukebox restés sans bateau (fantômes de la 1.0.0).
            for (BlockDisplay jukebox : monde.getEntitiesByClass(BlockDisplay.class)) {
                if (estJukebox(jukebox) && !estBateau(jukebox.getVehicle())) {
                    jukebox.remove();
                }
            }
        }
        getServer().getScheduler().runTaskTimer(this, this::orienter, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, this::boucle, PAS_BOUCLE, PAS_BOUCLE);
        creerBateau(Material.OAK_BOAT);
        creerBateau(Material.OAK_CHEST_BOAT);
        lang.saveIfNeeded();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ objet

    /**
     * Le bateau à jukebox de ce bois : objet de bateau ou de radeau sans coffre, ou (1.1.0) avec coffre pour le bateau
     * à coffre et jukebox. Nécessite que le plugin soit activé.
     */
    public static ItemStack creerBateau(Material bateau) {
        KSJukeboxBoat p = instance;
        ItemStack objet = new ItemStack(bateau);
        ItemMeta meta = objet.getItemMeta();
        boolean radeau = bateau.name().endsWith("_RAFT");
        Component nom;
        List<Component> description;
        if (Tag.ITEMS_CHEST_BOATS.isTagged(bateau)) {
            nom = radeau ? p.t("objet-coffre.nom-radeau", "<white>Radeau à coffre et jukebox")
                    : p.t("objet-coffre.nom", "<white>Bateau à coffre et jukebox");
            description = List.of(p.t("objet-coffre.ligne-1", "<gray>Lit en boucle les disques du coffre"),
                    p.t("objet-coffre.ligne-2", "<gray>Clic droit : lecture / pause"),
                    p.t("objet-coffre.ligne-3", "<gray>Accroupi + clic droit : coffre"),
                    p.t("objet-coffre.ligne-4", "<gray>On n'y monte pas : laisse"));
        } else {
            nom = radeau ? p.t("objet.nom-radeau", "<white>Radeau à jukebox") : p.t("objet.nom", "<white>Bateau à jukebox");
            description = List.of(p.t("objet.ligne-1", "<gray>Joue les disques de ton inventaire"),
                    p.t("objet.ligne-2", "<gray>À pied : accroupi + clic droit"),
                    p.t("objet.ligne-3", "<gray>À bord : touche de saut"));
        }
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = new ArrayList<>();
        for (Component ligne : description) {
            lignes.add(ligne.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lignes);
        meta.getPersistentDataContainer().set(p.cleBateau, PersistentDataType.BYTE, (byte) 1);
        objet.setItemMeta(meta);
        return objet;
    }

    public static boolean estBateau(ItemStack objet) {
        return instance != null && objet != null && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(instance.cleBateau, PersistentDataType.BYTE);
    }

    /** Bateau à jukebox posé, avec ou sans coffre. */
    private boolean estBateau(Entity entite) {
        return entite instanceof Boat && entite.getPersistentDataContainer().has(cleBateau, PersistentDataType.BYTE);
    }

    /** 1.1.0 : bateau à coffre et jukebox posé. */
    private boolean aCoffre(Entity entite) {
        return entite instanceof ChestBoat && estBateau(entite);
    }

    private boolean estJukebox(Entity entite) {
        return entite instanceof BlockDisplay
                && entite.getPersistentDataContainer().has(cleJukebox, PersistentDataType.BYTE);
    }

    /** Un bateau à jukebox ne sert pas de bateau simple dans une autre recette (le jukebox serait perdu). */
    @EventHandler
    public void onPreparer(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (estBateau(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    /** Un distributeur poserait un bateau simple : le bateau à jukebox y reste. */
    @EventHandler(ignoreCancelled = true)
    public void onDistribuer(BlockDispenseEvent event) {
        if (estBateau(event.getItem())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ bateau posé

    /** Après les protections (KS_Claim rend sa pose possible partout, en priorité HIGHEST). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPoser(EntityPlaceEvent event) {
        Player joueur = event.getPlayer();
        EquipmentSlot main = event.getHand();
        if (joueur == null || main == null || !(event.getEntity() instanceof Boat bateau)
                || !estBateau(joueur.getInventory().getItem(main))) {
            return;
        }
        bateau.getPersistentDataContainer().set(cleBateau, PersistentDataType.BYTE, (byte) 1);
        // Le bateau n'est pas encore dans le monde : son jukebox est posé au tick suivant (onArriver).
    }

    /** Bateau posé, chunk chargé, joueur revenu à bord de son bateau, changement de monde. */
    @EventHandler
    public void onArriver(EntityAddToWorldEvent event) {
        Entity entite = event.getEntity();
        if (estBateau(entite)) {
            getServer().getScheduler().runTask(this, () -> accueillir((Boat) entite));
        } else if (estJukebox(entite)) {
            // Jukebox resté sans bateau (fantôme de la 1.0.0).
            getServer().getScheduler().runTaskLater(this, () -> {
                if (entite.isValid() && !estBateau(entite.getVehicle())) {
                    entite.remove();
                }
            }, 2L);
        }
    }

    private void accueillir(Boat bateau) {
        if (!bateau.isValid() || !estBateau(bateau)) {
            return;
        }
        bateaux.put(bateau.getUniqueId(), bateau);
        if (jukebox(bateau) == null) {
            BlockDisplay jukebox = bateau.getWorld().spawn(bateau.getLocation(), BlockDisplay.class, j -> {
                j.setBlock(Material.JUKEBOX.createBlockData());
                j.getPersistentDataContainer().set(cleJukebox, PersistentDataType.BYTE, (byte) 1);
                j.setRotation(bateau.getLocation().getYaw(), 0);
                j.setTeleportDuration(1);
            });
            if (!bateau.addPassenger(jukebox)) {
                jukebox.remove();
            }
        }
        regler(bateau, bateau.getPassengers().size());
    }

    private BlockDisplay jukebox(Boat bateau) {
        for (Entity passager : bateau.getPassengers()) {
            if (estJukebox(passager)) {
                return (BlockDisplay) passager;
            }
        }
        return null;
    }

    /**
     * Place le jukebox à l'arrière du bateau. Seul à bord, il est assis au centre ; derrière un conducteur, à la place
     * arrière : le décalage du modèle compense, pour qu'il reste au même endroit. 1.1.0, bateau à coffre : le jukebox
     * est devant le coffre, là où s'assoit le passager.
     *
     * @param passagers nombre de passagers du bateau, jukebox compris (celui d'après, quand un joueur monte ou descend)
     */
    private void regler(Boat bateau, int passagers) {
        BlockDisplay jukebox = jukebox(bateau);
        if (jukebox == null) {
            return;
        }
        float taille = (float) getConfig().getDouble("jukebox.taille", 0.72);
        float hauteur = (float) getConfig().getDouble("jukebox.hauteur", 0.0);
        boolean coffre = bateau instanceof ChestBoat;
        float position = (float) (coffre ? getConfig().getDouble("jukebox.position-coffre", 0.49)
                : getConfig().getDouble("jukebox.position", -0.36));
        float place = coffre ? PLACE_COFFRE : passagers > 1 ? PLACE_ARRIERE : 0f;
        jukebox.setTransformation(new Transformation(
                new Vector3f(-taille / 2, hauteur, position - place - taille / 2), new Quaternionf(),
                new Vector3f(taille, taille, taille), new Quaternionf()));
    }

    /** Le jukebox tourne avec le bateau (le serveur ne le fait pas de lui-même quand un joueur conduit). */
    private void orienter() {
        bateaux.values().removeIf(bateau -> !bateau.isValid());
        for (Boat bateau : bateaux.values()) {
            BlockDisplay jukebox = jukebox(bateau);
            if (jukebox != null) {
                float cap = bateau.getLocation().getYaw();
                float ecart = Math.abs(jukebox.getLocation().getYaw() - cap) % 360f;
                if (Math.min(ecart, 360f - ecart) > 0.5f) {
                    jukebox.setRotation(cap, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ passagers

    /**
     * Seul un joueur monte à bord : un animal y prendrait la place du jukebox ou du conducteur. 1.1.0 : personne ne
     * monte dans le bateau à coffre.
     */
    @EventHandler(ignoreCancelled = true)
    public void onMonter(VehicleEnterEvent event) {
        if (estBateau(event.getVehicle()) && !estJukebox(event.getEntered())
                && (!(event.getEntered() instanceof Player) || aCoffre(event.getVehicle()))) {
            event.setCancelled(true);
        }
    }

    /**
     * Le joueur qui monte devient de lui-même le premier passager (celui qui conduit), et le jukebox passe à la place
     * arrière : son décalage est refait tout de suite, puis vérifié au tick suivant (montée refusée entre-temps).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConduire(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && estBateau(event.getVehicle())) {
            Boat bateau = (Boat) event.getVehicle();
            regler(bateau, bateau.getPassengers().size() + 1);
            getServer().getScheduler().runTask(this, () -> accueillir(bateau));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDescendre(VehicleExitEvent event) {
        if (event.getExited() instanceof Player && estBateau(event.getVehicle())) {
            Boat bateau = (Boat) event.getVehicle();
            regler(bateau, bateau.getPassengers().size() - 1);
            getServer().getScheduler().runTask(this, () -> accueillir(bateau));
        }
    }

    /**
     * 1.1.0 : jukebox descendu de son bateau sans que le bateau disparaisse (anti-triche qui éjecte les passagers,
     * colonne de bulles, bateau resté sous l'eau) : il y remonte au tick suivant. Sans bateau, ou si le bateau en a
     * déjà un autre, il disparaît : plus de jukebox fantôme.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEjecter(EntityDismountEvent event) {
        if (!estJukebox(event.getEntity())) {
            return;
        }
        Entity jukebox = event.getEntity();
        Entity bateau = event.getDismounted();
        getServer().getScheduler().runTask(this, () -> {
            if (!jukebox.isValid() || jukebox.getVehicle() != null) {
                return;
            }
            if (bateau.isValid() && estBateau(bateau) && jukebox((Boat) bateau) == null && bateau.addPassenger(jukebox)) {
                regler((Boat) bateau, bateau.getPassengers().size());
            } else {
                jukebox.remove();
            }
        });
    }

    // ------------------------------------------------------------------ bateau cassé

    /** Le bateau cassé redonne le bateau à jukebox (avec son nom, s'il a été renommé), pas un bateau simple. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLacher(EntityDropItemEvent event) {
        if (!estBateau(event.getEntity())) {
            return;
        }
        Item lache = event.getItemDrop();
        ItemStack objet = lache.getItemStack();
        // Le contenu d'un coffre ne passe pas par ici : seul le bateau lui-même est lâché ainsi.
        if (objet.getType() == ((Boat) event.getEntity()).getBoatMaterial() && !estBateau(objet)) {
            ItemStack bateau = creerBateau(objet.getType());
            Component nom = event.getEntity().customName();
            if (nom != null) {
                ItemMeta meta = bateau.getItemMeta();
                meta.displayName(nom);
                bateau.setItemMeta(meta);
            }
            lache.setItemStack(bateau);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisparaitre(EntityRemoveEvent event) {
        if (!estBateau(event.getEntity())) {
            return;
        }
        Boat bateau = (Boat) event.getEntity();
        bateaux.remove(bateau.getUniqueId());
        couper(bateau);
        // Chunk déchargé, ou joueur déconnecté à bord (le bateau est gardé avec lui) : le bateau existe toujours.
        if (event.getCause() != EntityRemoveEvent.Cause.UNLOAD
                && event.getCause() != EntityRemoveEvent.Cause.PLAYER_QUIT) {
            detruire(bateau);
        }
    }

    /** Au cas où la disparition d'un bateau cassé ne serait pas annoncée (onDisparaitre). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCasser(VehicleDestroyEvent event) {
        if (estBateau(event.getVehicle())) {
            Boat bateau = (Boat) event.getVehicle();
            getServer().getScheduler().runTask(this, () -> {
                if (!bateau.isValid()) {
                    detruire(bateau);
                }
            });
        }
    }

    /** Bateau disparu pour de bon : son disque tombe, la musique s'arrête, le jukebox disparaît. Sans effet la 2e fois. */
    private void detruire(Boat bateau) {
        bateaux.remove(bateau.getUniqueId());
        couper(bateau);
        ItemStack disque = disque(bateau);
        if (disque != null) {
            bateau.getPersistentDataContainer().remove(cleDisque);
            arreter(bateau, disque);
            bateau.getWorld().dropItemNaturally(bateau.getLocation(), disque);
        }
        BlockDisplay jukebox = jukebox(bateau);
        if (jukebox != null && isEnabled()) {
            // Pas pendant la disparition du bateau lui-même.
            getServer().getScheduler().runTask(this, jukebox::remove);
        }
    }

    // ------------------------------------------------------------------ disque et musique

    private static JukeboxSong chanson(ItemStack objet) {
        if (objet == null || objet.getType().isAir()) {
            return null;
        }
        JukeboxPlayable lisible = objet.getData(DataComponentTypes.JUKEBOX_PLAYABLE);
        return lisible == null ? null : lisible.jukeboxSong();
    }

    /** Le disque rangé dans le bateau, ou null. */
    private ItemStack disque(Boat bateau) {
        byte[] donnees = bateau.getPersistentDataContainer().get(cleDisque, PersistentDataType.BYTE_ARRAY);
        if (donnees == null) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(donnees);
        } catch (RuntimeException e) {
            getLogger().warning("Disque illisible dans un bateau à jukebox : " + e.getMessage());
            return null;
        }
    }

    private float volume() {
        return (float) getConfig().getDouble("volume", 4.0);
    }

    /** La musique sort du bateau et le suit (catégorie « Jukebox / Blocs musicaux » des réglages du son). */
    private void jouer(Boat bateau, ItemStack disque) {
        JukeboxSong chanson = chanson(disque);
        if (chanson != null) {
            bateau.getWorld().playSound(bateau, chanson.getSound(), SoundCategory.RECORDS, volume(), 1f);
        }
    }

    private void arreter(Boat bateau, ItemStack disque) {
        JukeboxSong chanson = chanson(disque);
        if (chanson == null) {
            return;
        }
        double portee = 16 * Math.max(1f, volume()) + 32;
        for (Player joueur : bateau.getWorld().getPlayers()) {
            if (joueur.getLocation().distanceSquared(bateau.getLocation()) <= portee * portee) {
                joueur.stopSound(chanson.getSound(), SoundCategory.RECORDS);
            }
        }
    }

    // ------------------------------------------------------------------ lecture en boucle (1.1.0, bateau à coffre)

    private boolean enLecture(ChestBoat bateau) {
        Byte lecture = bateau.getPersistentDataContainer().get(cleLecture, PersistentDataType.BYTE);
        return lecture == null || lecture != 0;
    }

    /** Clic droit normal : lecture / pause. La lecture reprend au début du disque interrompu. */
    private void alterner(Player joueur, ChestBoat bateau) {
        boolean lecture = !enLecture(bateau);
        bateau.getPersistentDataContainer().set(cleLecture, PersistentDataType.BYTE, (byte) (lecture ? 1 : 0));
        if (!lecture) {
            couper(bateau);
            Contenant.message(joueur, t("boucle.pause", "<yellow>Pause"), false);
        } else {
            avancer(bateau);
            Piste piste = pistes.get(bateau.getUniqueId());
            Contenant.message(joueur, piste == null ? t("boucle.vide", "<yellow>Aucun disque dans le coffre")
                    : t("boucle.lecture", "<green>Lecture : <disque>", "disque",
                            chanson(piste.disque()).getDescription()), piste == null);
        }
        lang.saveIfNeeded();
    }

    private void boucle() {
        for (Boat bateau : List.copyOf(bateaux.values())) {
            if (bateau instanceof ChestBoat coffre && bateau.isValid()) {
                avancer(coffre);
            }
        }
    }

    /**
     * Fait suivre les disques du coffre, dans l'ordre des cases, en boucle. Rien ne joue sans joueur à portée de la
     * musique (il ne l'entendrait pas en arrivant en cours de morceau) : le disque interrompu reprend à son début.
     */
    private void avancer(ChestBoat bateau) {
        if (!enLecture(bateau)) {
            return;
        }
        Inventory coffre = bateau.getInventory();
        long tick = Bukkit.getCurrentTick();
        boolean ecoute = ecoute(bateau);
        Piste piste = pistes.get(bateau.getUniqueId());
        if (piste != null) {
            ItemStack enPlace = coffre.getItem(piste.place());
            boolean retire = enPlace == null || !enPlace.isSimilar(piste.disque());
            if (!retire && ecoute && tick < piste.fin()) {
                return;
            }
            couper(bateau);
            if (!retire && ecoute) {
                // Morceau terminé : au suivant.
                suivant(bateau, piste.place() + 1);
            }
        }
        if (!ecoute) {
            return;
        }
        Integer depart = bateau.getPersistentDataContainer().get(clePiste, PersistentDataType.INTEGER);
        for (int i = 0; i < coffre.getSize(); i++) {
            int place = Math.floorMod((depart == null ? 0 : depart) + i, coffre.getSize());
            ItemStack disque = coffre.getItem(place);
            JukeboxSong chanson = chanson(disque);
            if (chanson != null) {
                suivant(bateau, place);
                jouer(bateau, disque);
                pistes.put(bateau.getUniqueId(), new Piste(place, disque.asOne(),
                        tick + (long) Math.ceil(chanson.getLengthInSeconds() * 20) + SILENCE));
                return;
            }
        }
    }

    /** Retient la case où chercher le prochain disque (gardée avec le bateau). */
    private void suivant(ChestBoat bateau, int place) {
        Integer avant = bateau.getPersistentDataContainer().get(clePiste, PersistentDataType.INTEGER);
        if (avant == null || avant != place) {
            bateau.getPersistentDataContainer().set(clePiste, PersistentDataType.INTEGER, place);
        }
    }

    /** Arrête le disque en cours d'un bateau à coffre ; la lecture reprendra à ce disque. Sans effet sinon. */
    private void couper(Boat bateau) {
        Piste piste = pistes.remove(bateau.getUniqueId());
        if (piste != null) {
            arreter(bateau, piste.disque());
        }
    }

    /** Vrai si un joueur est à portée de la musique du bateau. */
    private boolean ecoute(Boat bateau) {
        double portee = 16 * Math.max(1f, volume());
        for (Player joueur : bateau.getWorld().getPlayers()) {
            if (joueur.getLocation().distanceSquared(bateau.getLocation()) <= portee * portee) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ menu

    /** À pied : accroupi + clic droit sur le bateau (refusé là où une protection interdit de toucher aux bateaux). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCliquer(PlayerInteractEntityEvent event) {
        if (!estBateau(event.getRightClicked())) {
            return;
        }
        Player joueur = event.getPlayer();
        if (event.getRightClicked() instanceof ChestBoat bateau) {
            // 1.1.0 : accroupi, le jeu ouvre le coffre ; laisse ou cisailles en main, ou laisse du bateau tenue par le
            // joueur, le jeu attache, coupe ou lâche la laisse. Sinon : lecture / pause (le jeu ouvrirait le coffre).
            if (!joueur.isSneaking() && !laisse(joueur, bateau)) {
                event.setCancelled(true);
                if (event.getHand() == EquipmentSlot.HAND) {
                    alterner(joueur, bateau);
                }
            }
        } else if (event.getHand() == EquipmentSlot.HAND && joueur.isSneaking() && !tientUneLaisse(joueur)) {
            event.setCancelled(true);
            ouvrir(joueur, (Boat) event.getRightClicked());
        }
    }

    /**
     * 1.1.0 : accroupi + clic droit sur une entité, le jeu y attache les entités que le joueur tient en laisse (c'est
     * ainsi qu'on accroche le bateau à coffre et jukebox à son propre bateau) : le menu des disques ne s'ouvre pas alors.
     */
    private static boolean tientUneLaisse(Player joueur) {
        for (Entity proche : joueur.getNearbyEntities(16, 16, 16)) {
            if (proche instanceof Leashable tenu && tenu.isLeashed() && joueur.equals(tenu.getLeashHolder())) {
                return true;
            }
        }
        return false;
    }

    private static boolean laisse(Player joueur, ChestBoat bateau) {
        Material main = joueur.getInventory().getItemInMainHand().getType();
        Material autre = joueur.getInventory().getItemInOffHand().getType();
        return main == Material.LEAD || main == Material.SHEARS || autre == Material.LEAD || autre == Material.SHEARS
                || (bateau.isLeashed() && joueur.equals(bateau.getLeashHolder()));
    }

    /** À bord : touche de saut (sans effet dans un bateau). */
    @EventHandler
    public void onSauter(PlayerInputEvent event) {
        Player joueur = event.getPlayer();
        if (event.getInput().isJump() && estBateau(joueur.getVehicle()) && !aCoffre(joueur.getVehicle())
                && !(joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant)) {
            ouvrir(joueur, (Boat) joueur.getVehicle());
        }
    }

    /** Cases de l'inventaire du joueur (rangement, barre d'objets, main secondaire) qui portent un disque. */
    private static List<Integer> disques(PlayerInventory inventaire) {
        List<Integer> cases = new ArrayList<>();
        for (int i = 0; i <= 40; i++) {
            if ((i < 36 || i == 40) && chanson(inventaire.getItem(i)) != null) {
                cases.add(i);
            }
        }
        return cases;
    }

    private void ouvrir(Player joueur, Boat bateau) {
        PlayerInventory inventaire = joueur.getInventory();
        List<Integer> cases = disques(inventaire);
        Contenant menu = Contenant.pour(joueur, (cases.size() + 8) / 9 + 1, t("menu.titre", "Bateau à jukebox"),
                new Marque(bateau.getUniqueId()));
        for (int i = 0; i < cases.size(); i++) {
            int place = cases.get(i);
            ItemStack disque = inventaire.getItem(place);
            menu.poser(i, Contenant.objet(disque, chanson(disque).getDescription().color(NamedTextColor.WHITE),
                    List.of(t("menu.mettre", "<gray>Clic : mettre dans le bateau"))), j -> mettre(j, bateau, place));
        }
        menu.barre();
        ItemStack dedans = disque(bateau);
        if (dedans != null && chanson(dedans) != null) {
            menu.poser(menu.bas(4), Contenant.objet(dedans, chanson(dedans).getDescription().color(NamedTextColor.GREEN),
                    List.of(t("menu.dedans", "<green>Dans le bateau"),
                            t("menu.reprendre", "<gray>Clic : arrêter et reprendre"))), j -> reprendre(j, bateau));
        } else {
            menu.poser(menu.bas(4), Contenant.objet(Material.JUKEBOX, t("menu.aucun", "<white>Aucun disque"),
                    List.of(cases.isEmpty() ? t("menu.aucun-porte", "<gray>Tu ne portes aucun disque")
                            : t("menu.choisir", "<gray>Clique sur un de tes disques"))), null);
        }
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Faux (et menu fermé) si le bateau a disparu ou si le joueur s'en est éloigné. */
    private boolean aPortee(Player joueur, Boat bateau) {
        if (bateau.isValid() && bateau.getWorld().equals(joueur.getWorld())
                && bateau.getLocation().distanceSquared(joueur.getLocation()) <= PORTEE * PORTEE) {
            return true;
        }
        Contenant.message(joueur, t("message.loin", "<red>Le bateau est trop loin"), true);
        Contenant.fermer(joueur);
        lang.saveIfNeeded();
        return false;
    }

    private void mettre(Player joueur, Boat bateau, int place) {
        if (!aPortee(joueur, bateau)) {
            return;
        }
        PlayerInventory inventaire = joueur.getInventory();
        ItemStack pile = inventaire.getItem(place);
        JukeboxSong chanson = chanson(pile);
        if (chanson == null) {
            // L'inventaire a changé depuis l'ouverture du menu.
            ouvrir(joueur, bateau);
            return;
        }
        ItemStack disque = pile.asOne();
        ItemStack ancien = disque(bateau);
        pile.setAmount(pile.getAmount() - 1);
        inventaire.setItem(place, pile.getAmount() > 0 ? pile : null);
        if (ancien != null) {
            arreter(bateau, ancien);
            rendre(joueur, ancien);
        }
        bateau.getPersistentDataContainer().set(cleDisque, PersistentDataType.BYTE_ARRAY, disque.serializeAsBytes());
        jouer(bateau, disque);
        Contenant.message(joueur, t("message.lecture", "<green>Lecture : <disque>", "disque", chanson.getDescription()),
                false);
        ouvrir(joueur, bateau);
    }

    private void reprendre(Player joueur, Boat bateau) {
        if (!aPortee(joueur, bateau)) {
            return;
        }
        ItemStack disque = disque(bateau);
        if (disque != null) {
            bateau.getPersistentDataContainer().remove(cleDisque);
            arreter(bateau, disque);
            rendre(joueur, disque);
            Contenant.message(joueur, t("message.repris", "<yellow>Disque repris"), false);
        }
        ouvrir(joueur, bateau);
    }

    /** Dans l'inventaire du joueur, ou à ses pieds s'il est plein. */
    private static void rendre(Player joueur, ItemStack disque) {
        joueur.getInventory().addItem(disque).values()
                .forEach(reste -> joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste));
    }
}
