package fr.kalium.cadresbedrock;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_CadresBedrock 0.1.0, prototype (demande de LeKiwi06, 29/10/2026) : « peut-on fix les cadres invisible pour les
 * joueurs bedrock ? », puis « fais un prototype du plugin (support invisible cote Bedrock) dans Kixster Test ».
 *
 * Cause (jar de Geyser 2.12.0 lu le 29/10/2026, ItemFrameEntity) : pour un joueur Bedrock, Geyser remplace chaque cadre
 * par un bloc « cadre » et ne regarde jamais s'il est invisible ; Bedrock n'a pas de cadre invisible. Un cadre invisible
 * s'affiche donc comme un cadre normal.
 *
 * Contournement : pour chaque cadre invisible qui contient un objet, dans les mondes de « mondes » seulement, un
 * porte-armure invisible (non sauvegarde, invisible par defaut) tient une copie de l'objet. Il n'est montre qu'aux
 * joueurs Bedrock (Floodgate : UUID dont la moitie haute vaut 0), a qui le cadre est cache. Les joueurs Java ne voient
 * aucun changement. Un cadre invisible vide reste visible des joueurs Bedrock (cadre normal), pour qu'ils puissent y
 * mettre un objet.
 *
 * Le joueur Bedrock agit sur le cadre en visant le porte-armure : clic droit = tourner l'objet (si aucun plugin, claim
 * compris, ne refuse le clic sur le cadre), coup = le coup est donne au cadre (Player#attack : regles du jeu et claims).
 *
 * Geyser ne rend les poses des bras qu'avec le pack optionnel de Geyser (absent) : le porte-armure garde sa pose de base,
 * l'objet est tenu dans la main droite. Les decalages du config.yml placent la main sur le cadre ; ils se reglent en jeu
 * avec /cadresbedrock recharger.
 */
public final class KsCadresBedrock extends JavaPlugin implements Listener {

    private NamespacedKey cle;
    private final Set<String> mondes = new HashSet<>();
    private boolean petit = true;
    private double cote = 0.3125;
    private double hauteur = 0.65;
    private double ecartMur = 0.1;
    private long intervalle = 10;

    /** Porte-armure de chaque cadre (UUID du cadre). */
    private final Map<UUID, ArmorStand> supports = new HashMap<>();
    /** Cadre de chaque porte-armure (UUID du porte-armure). */
    private final Map<UUID, ItemFrame> cadreDuSupport = new HashMap<>();
    /** Par joueur Bedrock : cadres caches et porte-armures montres par ce plugin. */
    private final Map<UUID, Set<UUID>> cadresCaches = new HashMap<>();
    private final Map<UUID, Set<UUID>> supportsMontres = new HashMap<>();

    @Override
    public void onEnable() {
        cle = new NamespacedKey(this, "cadre");
        saveDefaultConfig();
        lireConfig();
        // Porte-armures restes d'un arret brutal (ils ne sont pas sauvegardes, par prudence).
        for (World w : getServer().getWorlds()) {
            for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                if (estSupport(a)) a.remove();
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::synchroniser, 20L, intervalle);
        getLogger().info("Cadres invisibles pour les joueurs Bedrock dans : " + mondes);
    }

    @Override
    public void onDisable() {
        toutRetirer();
    }

    private void lireConfig() {
        reloadConfig();
        mondes.clear();
        List<String> liste = getConfig().contains("mondes") ? getConfig().getStringList("mondes") : List.of("Kixster Test");
        mondes.addAll(liste);
        petit = getConfig().getBoolean("petit", true);
        cote = getConfig().getDouble("main.cote", 0.3125);
        hauteur = getConfig().getDouble("main.hauteur", 0.65);
        ecartMur = getConfig().getDouble("ecart-mur", 0.1);
        intervalle = Math.max(1, getConfig().getLong("intervalle", 10));
    }

    static boolean bedrock(Player player) {
        return player.getUniqueId().getMostSignificantBits() == 0;
    }

    private boolean estSupport(Entity e) {
        return e instanceof ArmorStand && e.getPersistentDataContainer().has(cle, PersistentDataType.STRING);
    }

    private static boolean aRemplacer(ItemFrame cadre) {
        return !cadre.isVisible() && cadre.getItem() != null && !cadre.getItem().getType().isAir();
    }

    // ------------------------------------------------------------------ synchronisation

    private void synchroniser() {
        Set<UUID> vus = new HashSet<>();
        for (String nom : mondes) {
            World w = getServer().getWorld(nom);
            if (w == null) continue;
            for (ItemFrame cadre : w.getEntitiesByClass(ItemFrame.class)) {
                if (!aRemplacer(cadre)) continue;
                vus.add(cadre.getUniqueId());
                ArmorStand support = supports.get(cadre.getUniqueId());
                if (support == null || !support.isValid()) {
                    if (support != null) cadreDuSupport.remove(support.getUniqueId());
                    support = creerSupport(cadre);
                    supports.put(cadre.getUniqueId(), support);
                    cadreDuSupport.put(support.getUniqueId(), cadre);
                }
                ItemStack objet = copieUnique(cadre.getItem());
                if (!objet.equals(support.getEquipment().getItemInMainHand())) {
                    support.getEquipment().setItemInMainHand(objet);
                }
            }
        }
        // Cadres vides, devenus visibles, casses ou dechargés : leur porte-armure part.
        Iterator<Map.Entry<UUID, ArmorStand>> it = supports.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ArmorStand> e = it.next();
            if (vus.contains(e.getKey())) continue;
            cadreDuSupport.remove(e.getValue().getUniqueId());
            e.getValue().remove();
            it.remove();
        }
        for (Player p : getServer().getOnlinePlayers()) {
            if (bedrock(p)) mettreAJourVue(p);
        }
    }

    private void mettreAJourVue(Player p) {
        Set<UUID> caches = cadresCaches.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        Set<UUID> montres = supportsMontres.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        Set<UUID> cadresVoulus = new HashSet<>();
        Set<UUID> supportsVoulus = new HashSet<>();
        for (Map.Entry<UUID, ArmorStand> e : supports.entrySet()) {
            ItemFrame cadre = cadreDuSupport.get(e.getValue().getUniqueId());
            if (cadre == null) continue;
            cadresVoulus.add(e.getKey());
            supportsVoulus.add(e.getValue().getUniqueId());
            if (caches.add(e.getKey())) p.hideEntity(this, cadre);
            if (montres.add(e.getValue().getUniqueId())) p.showEntity(this, e.getValue());
        }
        // Cadre redevenu normal (vide ou visible) : de nouveau montre au joueur.
        for (Iterator<UUID> it = caches.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            if (cadresVoulus.contains(id)) continue;
            Entity cadre = getServer().getEntity(id);
            if (cadre != null) p.showEntity(this, cadre);
            it.remove();
        }
        montres.retainAll(supportsVoulus);
    }

    private ArmorStand creerSupport(ItemFrame cadre) {
        Location lieu = placeDuSupport(cadre);
        String idCadre = cadre.getUniqueId().toString();
        return cadre.getWorld().spawn(lieu, ArmorStand.class, a -> {
            // Regle avant l'apparition : jamais envoye aux joueurs Java, jamais sauvegarde avec le chunk.
            a.setVisibleByDefault(false);
            a.setPersistent(false);
            a.setInvisible(true);
            a.setGravity(false);
            a.setCanTick(false);
            a.setSmall(petit);
            a.setArms(true);
            a.setBasePlate(false);
            a.setSilent(true);
            a.setCollidable(false);
            a.setDisabledSlots(EquipmentSlot.values());
            a.getPersistentDataContainer().set(cle, PersistentDataType.STRING, idCadre);
        });
    }

    /**
     * Le porte-armure regarde dans le sens du cadre (dos au mur), sa main droite sur le centre du cadre. Cadre au sol ou
     * au plafond : main sur le centre du cadre, regard vers le sud.
     */
    private Location placeDuSupport(ItemFrame cadre) {
        double echelle = petit ? 0.5 : 1.0;
        BlockFace face = cadre.getFacing();
        Vector sens = new Vector(face.getModX(), 0, face.getModZ());
        boolean mur = sens.lengthSquared() > 0;
        if (!mur) sens = new Vector(0, 0, 1);
        // Main droite d'une entite qui regarde vers « sens » : a sa droite, soit (-z, 0, x).
        Vector droite = new Vector(-sens.getZ(), 0, sens.getX());
        Location lieu = cadre.getLocation().clone();
        lieu.add(droite.multiply(-cote * echelle));
        if (mur) lieu.add(sens.clone().multiply(ecartMur));
        lieu.add(0, -hauteur * echelle, 0);
        lieu.setYaw((float) Math.toDegrees(Math.atan2(-sens.getX(), sens.getZ())));
        lieu.setPitch(0);
        return lieu;
    }

    private static ItemStack copieUnique(ItemStack objet) {
        ItemStack copie = objet.clone();
        copie.setAmount(1);
        return copie;
    }

    private void toutRetirer() {
        for (Player p : getServer().getOnlinePlayers()) {
            Set<UUID> caches = cadresCaches.get(p.getUniqueId());
            if (caches == null) continue;
            for (UUID id : caches) {
                Entity cadre = getServer().getEntity(id);
                if (cadre != null) p.showEntity(this, cadre);
            }
        }
        cadresCaches.clear();
        supportsMontres.clear();
        for (ArmorStand a : supports.values()) a.remove();
        supports.clear();
        cadreDuSupport.clear();
    }

    // ------------------------------------------------------------------ actions du joueur Bedrock

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteraction(PlayerInteractEntityEvent event) {
        // Tous les clics sur le porte-armure (dont PlayerArmorStandManipulateEvent) : jamais d'echange d'objet.
        if (estSupport(event.getRightClicked())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractionAt(PlayerInteractAtEntityEvent event) {
        if (!estSupport(event.getRightClicked())) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemFrame cadre = cadreDuSupport.get(event.getRightClicked().getUniqueId());
        if (cadre == null || !cadre.isValid() || cadre.isFixed()) return;
        // Meme clic sur le cadre, pour que les claims et les autres plugins puissent le refuser.
        PlayerInteractEntityEvent clic = new PlayerInteractEntityEvent(event.getPlayer(), cadre, EquipmentSlot.HAND);
        if (!clic.callEvent()) return;
        cadre.setRotation(cadre.getRotation().rotateClockwise());
        cadre.getWorld().playSound(cadre.getLocation(), Sound.ENTITY_ITEM_FRAME_ROTATE_ITEM, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCoup(EntityDamageEvent event) {
        if (!estSupport(event.getEntity())) return;
        event.setCancelled(true);
        if (!(event instanceof EntityDamageByEntityEvent parEntite)) return;
        if (!(parEntite.getDamager() instanceof Player joueur) || !bedrock(joueur)) return;
        ItemFrame cadre = cadreDuSupport.get(event.getEntity().getUniqueId());
        if (cadre != null && cadre.isValid()) joueur.attack(cadre);
    }

    @EventHandler
    public void onMort(EntityDeathEvent event) {
        // Copie de l'objet du cadre : ne doit jamais tomber (/kill par exemple).
        if (estSupport(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Paper oublie les entites cachees a la deconnexion.
        cadresCaches.remove(event.getPlayer().getUniqueId());
        supportsMontres.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("recharger")) {
            toutRetirer();
            lireConfig();
            sender.sendMessage("§aKS_CadresBedrock rechargé (petit : " + petit + ", côté : " + cote + ", hauteur : "
                    + hauteur + ", écart du mur : " + ecartMur + ") ; porte-armures replacés dans " + intervalle
                    + " ticks au plus.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            List<String> noms = new ArrayList<>();
            for (Player p : getServer().getOnlinePlayers()) if (bedrock(p)) noms.add(p.getName());
            sender.sendMessage("§e" + supports.size() + " cadre(s) invisible(s) remplacé(s) dans " + mondes
                    + " ; joueurs Bedrock en ligne : " + noms);
            return true;
        }
        sender.sendMessage("§e/cadresbedrock recharger §7(relit config.yml et replace les porte-armures)§e, "
                + "/cadresbedrock info");
        return true;
    }
}
