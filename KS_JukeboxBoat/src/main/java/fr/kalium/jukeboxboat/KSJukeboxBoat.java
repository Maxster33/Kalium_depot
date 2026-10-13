package fr.kalium.jukeboxboat;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
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
import org.bukkit.inventory.InventoryHolder;
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
 * Autres plugins : creerBateau() (KS_Crafts), estBateau().
 */
public final class KSJukeboxBoat extends JavaPlugin implements Listener {

    /** Recul du 2e passager d'un bateau par rapport à son centre (valeur du jeu). */
    private static final float PLACE_ARRIERE = -0.6f;

    /** Distance au-delà de laquelle le menu ne commande plus le bateau. */
    private static final double PORTEE = 8;

    private static KSJukeboxBoat instance;

    private NamespacedKey cleBateau;
    private NamespacedKey cleDisque;
    private NamespacedKey cleJukebox;
    private Lang lang;

    /** Bateaux à jukebox présents dans les mondes chargés. */
    private final Map<UUID, Boat> bateaux = new HashMap<>();

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
        lang = new Lang(this);
        getServer().getPluginManager().registerEvents(this, this);
        for (World monde : Bukkit.getWorlds()) {
            monde.getEntitiesByClass(Boat.class).forEach(this::accueillir);
        }
        getServer().getScheduler().runTaskTimer(this, this::orienter, 1L, 1L);
        creerBateau(Material.OAK_BOAT);
        lang.saveIfNeeded();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ objet

    /** Le bateau à jukebox de ce bois (objet de bateau ou de radeau sans coffre). Nécessite que le plugin soit activé. */
    public static ItemStack creerBateau(Material bateau) {
        KSJukeboxBoat p = instance;
        ItemStack objet = new ItemStack(bateau);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName((bateau.name().endsWith("_RAFT")
                ? p.t("objet.nom-radeau", "<white>Radeau à jukebox") : p.t("objet.nom", "<white>Bateau à jukebox"))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = new ArrayList<>();
        for (Component ligne : List.of(p.t("objet.ligne-1", "<gray>Joue les disques de ton inventaire"),
                p.t("objet.ligne-2", "<gray>À pied : accroupi + clic droit"),
                p.t("objet.ligne-3", "<gray>À bord : touche de saut"))) {
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

    private boolean estBateau(Entity entite) {
        return entite instanceof Boat && !(entite instanceof InventoryHolder)
                && entite.getPersistentDataContainer().has(cleBateau, PersistentDataType.BYTE);
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
                || bateau instanceof InventoryHolder || !estBateau(joueur.getInventory().getItem(main))) {
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
            // Jukebox resté sans bateau (arrêt du serveur pendant qu'un joueur montait à bord).
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
            bateau.addPassenger(jukebox);
        }
        regler(bateau);
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
     * arrière : le décalage du modèle compense, pour qu'il reste au même endroit.
     */
    private void regler(Boat bateau) {
        BlockDisplay jukebox = jukebox(bateau);
        if (jukebox == null) {
            return;
        }
        float taille = (float) getConfig().getDouble("jukebox.taille", 0.55);
        float hauteur = (float) getConfig().getDouble("jukebox.hauteur", 0.0);
        float position = (float) getConfig().getDouble("jukebox.position", -0.4);
        float place = bateau.getPassengers().size() > 1 ? PLACE_ARRIERE : 0f;
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

    /** Seul un joueur monte à bord : un animal y prendrait la place du jukebox ou du conducteur. */
    @EventHandler(ignoreCancelled = true)
    public void onMonter(VehicleEnterEvent event) {
        if (estBateau(event.getVehicle()) && !(event.getEntered() instanceof Player)
                && !estJukebox(event.getEntered())) {
            event.setCancelled(true);
        }
    }

    /** Le conducteur doit être le premier passager : le jukebox lui laisse la place, puis remonte derrière lui. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConduire(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player) || !estBateau(event.getVehicle())) {
            return;
        }
        Boat bateau = (Boat) event.getVehicle();
        BlockDisplay jukebox = jukebox(bateau);
        if (jukebox != null) {
            bateau.removePassenger(jukebox);
        }
        getServer().getScheduler().runTask(this, () -> {
            if (jukebox != null && jukebox.isValid()
                    && !(bateau.isValid() && jukebox(bateau) == null && bateau.addPassenger(jukebox))) {
                jukebox.remove();
            }
            accueillir(bateau);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDescendre(VehicleExitEvent event) {
        if (event.getExited() instanceof Player && estBateau(event.getVehicle())) {
            Boat bateau = (Boat) event.getVehicle();
            getServer().getScheduler().runTask(this, () -> accueillir(bateau));
        }
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
        if (Tag.ITEMS_BOATS.isTagged(objet.getType()) && !estBateau(objet)) {
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

    // ------------------------------------------------------------------ menu

    /** À pied : accroupi + clic droit sur le bateau (refusé là où une protection interdit de toucher aux bateaux). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCliquer(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && event.getPlayer().isSneaking()
                && estBateau(event.getRightClicked())) {
            event.setCancelled(true);
            ouvrir(event.getPlayer(), (Boat) event.getRightClicked());
        }
    }

    /** À bord : touche de saut (sans effet dans un bateau). */
    @EventHandler
    public void onSauter(PlayerInputEvent event) {
        Player joueur = event.getPlayer();
        if (event.getInput().isJump() && estBateau(joueur.getVehicle())
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
