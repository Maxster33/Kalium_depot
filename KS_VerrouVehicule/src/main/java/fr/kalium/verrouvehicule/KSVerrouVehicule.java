package fr.kalium.verrouvehicule;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.entity.Leashable;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_VerrouVehicule (demande de LeKiwi06, 14/10/2026 : « un item pour verrouiller le contenant d'un véhicule, histoire
 * qu'on puisse le garer même hors claims et qui ne soit pas cassable, déplaçable, vidable par hopper, etc. tant que le
 * contenant n'est pas vide »).
 *
 * Objet : un crochet vanilla brillant, « Verrou de véhicule » (recette dans KS_Crafts : crochet + 4 lingots de fer).
 * Clic droit sur un bateau ou radeau à coffre, un wagonnet à coffre ou à entonnoir, un âne, une mule ou un lama à
 * coffre (le sien) : le verrou y reste posé, au nom du joueur (le propriétaire). Il ne protège que tant que le
 * contenant n'est pas vide :
 * - seul le propriétaire l'ouvre (permission ksverrouvehicule.contourner : les opérateurs aussi) ;
 * - personne ne le casse, propriétaire compris (coups, explosions, lave...) ; un animal ne prend aucun dégât ;
 * - aucun entonnoir ni wagonnet à entonnoir ne le vide (le remplir reste possible) ;
 * - les autres joueurs peuvent monter, mais ne conduisent pas : ils s'assoient sur un siège invisible (porte-armure
 *   marqueur) posé dans le véhicule, qui n'a alors plus de conducteur (lu dans le code du jeu 26.3 : seul un joueur
 *   premier passager conduit un bateau ; un joueur qui monte passe toujours en premier, d'où le siège). Le propriétaire
 *   qui monte prend la place du conducteur : le passager descend (un bateau à coffre n'a qu'une place) ;
 * - personne d'autre ne le déplace (poussée, laisse, conduite) ; un bateau ou un wagonnet est ancré : poussé par un
 *   piston, un courant d'eau, des rails, une explosion, il revient à sa place. Exception : dans un claim de
 *   SimpleClaimSystem, son propriétaire et ses membres peuvent le déplacer (sans l'ouvrir ni le casser), dans ce claim
 *   seulement.
 * Le propriétaire retire son verrou par le petit menu qui s'ouvre avant le coffre (« Ouvrir le coffre », « Retirer le
 * verrou », « Fermer »). Véhicule vide : véhicule normal ; cassé (ou animal mort), il lâche son verrou.
 *
 * 1.1.0 (KS_Trains 1.2.0, étape 3) : un wagon attelé dans un train de KS_Trains n'est pas ancré (le train le place) ;
 * KS_Trains ne laisse atteler un wagon verrouillé et régler son coffre que par son propriétaire.
 *
 * Autres plugins : creerVerrou() (KS_Crafts), estVerrou().
 */
public final class KSVerrouVehicule extends JavaPlugin implements Listener {

    private static final String CONTOURNER = "ksverrouvehicule.contourner";

    /** Écart horizontal (en blocs) toléré avant de ramener un véhicule ancré à sa place. */
    private static final double ECART = 0.3;

    /** Après une poussée permise, le véhicule reste libre pendant ce temps (ticks), et tant qu'il roule ou glisse. */
    private static final long LIBRE = 40;
    private static final double ARRET = 0.02;

    /** Distance au-delà de laquelle le menu ne commande plus le véhicule. */
    private static final double PORTEE = 6;

    private static KSVerrouVehicule instance;

    private NamespacedKey cleVerrou;
    private NamespacedKey cleProprietaire;
    private NamespacedKey cleNom;
    private NamespacedKey cleSiege;
    /** 1.1.0 : wagon attelé dans un train de KS_Trains (identifiant du train). */
    private static final NamespacedKey CLE_TRAIN = NamespacedKey.fromString("ks_trains:train");
    private Lang lang;
    private boolean claims;
    private long tick;

    /** Véhicules et animaux verrouillés présents dans les mondes chargés. */
    private final Map<UUID, Suivi> suivis = new HashMap<>();

    private static final class Suivi {
        final Entity entite;
        Location ancre;
        long libreJusqua = -1;

        Suivi(Entity entite) {
            this.entite = entite;
            this.ancre = entite.getLocation();
        }
    }

    /** Ce qu'affiche un menu : le véhicule qu'il commande. */
    private record Marque(UUID vehicule) {
    }

    @Override
    public void onEnable() {
        instance = this;
        cleVerrou = new NamespacedKey(this, "verrou");
        cleProprietaire = new NamespacedKey(this, "proprietaire");
        cleNom = new NamespacedKey(this, "nom");
        cleSiege = new NamespacedKey(this, "siege");
        lang = new Lang(this);
        claims = getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem");
        if (!claims) {
            getLogger().warning("SimpleClaimSystem absent : seul le propriétaire déplace un véhicule verrouillé.");
        }
        getServer().getPluginManager().registerEvents(this, this);
        for (World monde : Bukkit.getWorlds()) {
            for (Entity entite : monde.getEntities()) {
                if (estSiege(entite)) {
                    entite.eject();
                    entite.remove();
                } else if (verrouille(entite)) {
                    suivre(entite);
                }
            }
        }
        getServer().getScheduler().runTaskTimer(this, this::ancrer, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, this::surveiller, 20L, 20L);
        creerVerrou();
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        // Les sièges ne sont pas enregistrés avec le monde : on fait descendre leurs passagers tout de suite.
        for (World monde : Bukkit.getWorlds()) {
            for (ArmorStand siege : monde.getEntitiesByClass(ArmorStand.class)) {
                if (estSiege(siege)) {
                    siege.eject();
                    siege.remove();
                }
            }
        }
        lang.saveIfNeeded();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ objet

    /** Un verrou de véhicule. Nécessite que le plugin soit activé. */
    public static ItemStack creerVerrou() {
        KSVerrouVehicule p = instance;
        ItemStack objet = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(p.t("objet.nom", "<white>Verrou de véhicule").decoration(TextDecoration.ITALIC, false));
        List<Component> lignes = new ArrayList<>();
        for (Component ligne : List.of(
                p.t("objet.ligne-1", "<gray>Clic droit sur un véhicule à coffre"),
                p.t("objet.ligne-2", "<gray>(bateau, wagonnet, âne, mule, lama)"),
                p.t("objet.ligne-3", "<gray>Protégé tant que son coffre"),
                p.t("objet.ligne-4", "<gray>n'est pas vide"),
                p.t("objet.ligne-5", "<gray>Retrait : en ouvrant le coffre"))) {
            lignes.add(ligne.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lignes);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(p.cleVerrou, PersistentDataType.BYTE, (byte) 1);
        objet.setItemMeta(meta);
        return objet;
    }

    public static boolean estVerrou(ItemStack objet) {
        return instance != null && objet != null && objet.getType() == Material.TRIPWIRE_HOOK && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(instance.cleVerrou, PersistentDataType.BYTE);
    }

    /** Le verrou ne se pose pas comme un crochet (il perdrait son nom). */
    @EventHandler(ignoreCancelled = true)
    public void onPoserBloc(BlockPlaceEvent event) {
        if (estVerrou(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    /** Le verrou ne sert pas de crochet dans une recette (coffre piégé, arbalète...). */
    @EventHandler
    public void onPreparer(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (estVerrou(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ véhicule verrouillé

    /** Véhicule ou animal dont le contenant peut être verrouillé. */
    private static boolean verrouillable(Entity entite) {
        return entite instanceof ChestBoat || entite instanceof StorageMinecart || entite instanceof HopperMinecart
                || (entite instanceof ChestedHorse cheval && cheval.isCarryingChest());
    }

    private UUID proprietaire(Entity entite) {
        if (entite == null) {
            return null;
        }
        String texte = entite.getPersistentDataContainer().get(cleProprietaire, PersistentDataType.STRING);
        if (texte == null) {
            return null;
        }
        try {
            return UUID.fromString(texte);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean verrouille(Entity entite) {
        return proprietaire(entite) != null;
    }

    private String nom(Entity entite) {
        String nom = entite.getPersistentDataContainer().get(cleNom, PersistentDataType.STRING);
        return nom == null ? "?" : nom;
    }

    private boolean estProprietaire(Player joueur, Entity entite) {
        return joueur.getUniqueId().equals(proprietaire(entite));
    }

    /**
     * Vrai si le contenant n'est pas vide. Âne, mule, lama : seules les cases du coffre comptent (les dernières de son
     * inventaire, 3 par colonne : 5 colonnes, ou la force du lama, lu dans le code du jeu 26.3), pas la selle ni le
     * tapis.
     */
    private static boolean plein(Entity entite) {
        Inventory inventaire;
        int debut = 0;
        if (entite instanceof ChestedHorse cheval) {
            if (!cheval.isCarryingChest()) {
                return false;
            }
            inventaire = cheval.getInventory();
            int cases = 3 * (cheval instanceof Llama lama ? lama.getStrength() : 5);
            debut = Math.max(0, inventaire.getSize() - cases);
        } else if (entite instanceof InventoryHolder detenteur) {
            inventaire = detenteur.getInventory();
        } else {
            return false;
        }
        for (int place = debut; place < inventaire.getSize(); place++) {
            ItemStack objet = inventaire.getItem(place);
            if (objet != null && !objet.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Verrouillé et contenant non vide : la protection s'applique. */
    private boolean protege(Entity entite) {
        return entite != null && verrouillable(entite) && verrouille(entite) && plein(entite);
    }

    /** Propriétaire, ou propriétaire / membre du claim où se trouve le véhicule, ou opérateur. */
    private boolean peutDeplacer(Player joueur, Entity entite) {
        return estProprietaire(joueur, entite) || joueur.hasPermission(CONTOURNER)
                || (claims && Claims.membre(joueur, entite.getLocation()));
    }

    private void refuser(Player joueur, Entity entite) {
        Contenant.message(joueur, t("refus", "<red>Verrouillé par <nom>", "nom", nom(entite)), true);
    }

    private void suivre(Entity entite) {
        suivis.putIfAbsent(entite.getUniqueId(), new Suivi(entite));
    }

    @EventHandler
    public void onArriver(EntityAddToWorldEvent event) {
        Entity entite = event.getEntity();
        if (estSiege(entite)) {
            // Siège resté sans véhicule ou sans passager.
            getServer().getScheduler().runTaskLater(this, () -> {
                if (entite.isValid() && (entite.getVehicle() == null || entite.getPassengers().isEmpty())) {
                    entite.eject();
                    entite.remove();
                }
            }, 2L);
        } else if (verrouille(entite)) {
            suivre(entite);
        }
    }

    @EventHandler
    public void onPartir(EntityRemoveFromWorldEvent event) {
        suivis.remove(event.getEntity().getUniqueId());
    }

    // ------------------------------------------------------------------ poser, ouvrir, menu

    /**
     * Verrou en main : il se pose. Clic qui ouvrirait le contenant (wagonnet : clic droit ; bateau, animal : accroupi +
     * clic droit) : menu du verrou pour le propriétaire, refus pour les autres (protégé). Avant les protections et
     * KS_JukeboxBoat (lecture / pause du bateau à coffre et jukebox), qui ignorent un clic déjà annulé.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCliquer(PlayerInteractEntityEvent event) {
        Entity cible = event.getRightClicked();
        Player joueur = event.getPlayer();
        boolean main = event.getHand() == EquipmentSlot.HAND;
        if (estVerrou(joueur.getInventory().getItemInMainHand())
                && (verrouillable(cible) || cible instanceof Vehicle)) {
            event.setCancelled(true);
            if (main) {
                poser(joueur, cible);
            }
            return;
        }
        if (!verrouillable(cible) || !verrouille(cible) || laisse(joueur)) {
            return;
        }
        boolean ouvrirait = cible instanceof Minecart || joueur.isSneaking();
        if (ouvrirait) {
            if (estProprietaire(joueur, cible)) {
                event.setCancelled(true);
                if (main) {
                    menu(joueur, cible);
                }
            } else if (protege(cible) && !joueur.hasPermission(CONTOURNER)) {
                event.setCancelled(true);
                if (main) {
                    refuser(joueur, cible);
                }
            }
            return;
        }
        // Monter : un passager est assis sur le siège ; le propriétaire (ou un membre du claim) prend sa place.
        Entity siege = siege(cible);
        if (siege != null && peutDeplacer(joueur, cible)) {
            event.setCancelled(true);
            if (main) {
                reprendre(joueur, cible, siege);
            }
        }
    }

    /**
     * Laisse ou cisailles en main, ou entités tenues en laisse (accroupi + clic droit les attache à l'entité visée) :
     * le jeu attache, coupe ou transfère la laisse (règles de la laisse plus bas).
     */
    private static boolean laisse(Player joueur) {
        Material main = joueur.getInventory().getItemInMainHand().getType();
        Material autre = joueur.getInventory().getItemInOffHand().getType();
        if (main == Material.LEAD || main == Material.SHEARS || autre == Material.LEAD || autre == Material.SHEARS) {
            return true;
        }
        if (joueur.isSneaking()) {
            for (Entity proche : joueur.getNearbyEntities(16, 16, 16)) {
                if (proche instanceof Leashable tenu && tenu.isLeashed() && joueur.equals(tenu.getLeashHolder())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void poser(Player joueur, Entity cible) {
        if (!verrouillable(cible)) {
            Contenant.message(joueur, t("poser.sans-coffre", "<red>Ce véhicule n'a pas de coffre"), true);
            return;
        }
        if (verrouille(cible)) {
            Contenant.message(joueur, t("poser.deja", "<red>Déjà verrouillé par <nom>", "nom", nom(cible)), true);
            return;
        }
        if (cible instanceof ChestedHorse cheval) {
            AnimalTamer maitre = cheval.getOwner();
            if (maitre == null || !joueur.getUniqueId().equals(maitre.getUniqueId())) {
                Contenant.message(joueur, t("poser.pas-maitre", "<red>Ce n'est pas ton animal"), true);
                return;
            }
        }
        if (claims && Claims.chezUnAutre(joueur, cible.getLocation())) {
            Contenant.message(joueur, t("poser.claim", "<red>Tu es dans le claim d'un autre joueur"), true);
            return;
        }
        PersistentDataContainer donnees = cible.getPersistentDataContainer();
        donnees.set(cleProprietaire, PersistentDataType.STRING, joueur.getUniqueId().toString());
        donnees.set(cleNom, PersistentDataType.STRING, joueur.getName());
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            ItemStack objet = joueur.getInventory().getItemInMainHand();
            objet.setAmount(objet.getAmount() - 1);
        }
        suivre(cible);
        cible.getWorld().playSound(cible.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, 1.2f);
        Contenant.message(joueur, t("poser.ok", "<green>Verrouillé : protégé tant que le coffre n'est pas vide"), false);
    }

    /** Petit menu du propriétaire, avant le coffre. */
    private void menu(Player joueur, Entity vehicule) {
        Contenant menu = Contenant.pour(joueur, 1, t("menu.titre", "Verrou de véhicule"),
                new Marque(vehicule.getUniqueId()));
        menu.barre();
        List<Component> lignes = new ArrayList<>();
        lignes.add(plein(vehicule) ? t("menu.protege", "<green>Protégé : le coffre n'est pas vide")
                : t("menu.libre", "<gray>Pas protégé : le coffre est vide"));
        lignes.addAll(Contenant.lignes(t("menu.retirer-clic", "<gray>Clic : le verrou revient dans ton inventaire")));
        menu.poser(menu.bas(3), Contenant.objet(Material.TRIPWIRE_HOOK, t("menu.retirer", "<yellow>Retirer le verrou"),
                lignes), j -> retirer(j, vehicule));
        menu.poser(menu.bas(4), Contenant.objet(Material.CHEST, t("menu.ouvrir", "<white>Ouvrir le coffre"),
                List.of()), j -> ouvrirCoffre(j, vehicule));
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        menu.ouvrir(joueur);
    }

    /** Le véhicule est encore là, à portée, et verrouillé par ce joueur. */
    private boolean aPortee(Player joueur, Entity vehicule) {
        boolean ok = vehicule.isValid() && estProprietaire(joueur, vehicule)
                && vehicule.getWorld().equals(joueur.getWorld())
                && vehicule.getLocation().distanceSquared(joueur.getLocation()) <= PORTEE * PORTEE;
        if (!ok) {
            Contenant.message(joueur, t("menu.loin", "<red>Le véhicule n'est plus à portée"), true);
            Contenant.fermer(joueur);
        }
        return ok;
    }

    private void ouvrirCoffre(Player joueur, Entity vehicule) {
        if (!aPortee(joueur, vehicule)) {
            return;
        }
        Contenant.apres(() -> {
            if (!joueur.isOnline() || !vehicule.isValid()) {
                return;
            }
            if (vehicule instanceof ChestedHorse cheval) {
                ouvrirAnimal(joueur, cheval);
            } else if (vehicule instanceof InventoryHolder detenteur) {
                joueur.openInventory(detenteur.getInventory());
            }
        });
    }

    /**
     * L'inventaire d'un âne, d'une mule ou d'un lama ne s'ouvre pas par l'API de Paper : on appelle la méthode du jeu
     * (AbstractHorse.openCustomInventoryScreen, noms du jeu au lancement de Paper), comme accroupi + clic droit. Le jeu
     * refuse si quelqu'un d'autre est dessus.
     */
    private void ouvrirAnimal(Player joueur, ChestedHorse cheval) {
        if (!cheval.getPassengers().isEmpty() && !cheval.getPassengers().contains(joueur)) {
            Contenant.message(joueur, t("menu.occupe", "<red>Quelqu'un est monté dessus"), true);
            return;
        }
        try {
            Object animal = cheval.getClass().getMethod("getHandle").invoke(cheval);
            Object humain = joueur.getClass().getMethod("getHandle").invoke(joueur);
            Class<?> classeJoueur = Class.forName("net.minecraft.world.entity.player.Player");
            animal.getClass().getMethod("openCustomInventoryScreen", classeJoueur).invoke(animal, humain);
        } catch (ReflectiveOperationException | RuntimeException e) {
            getLogger().warning("Ouverture de l'inventaire d'un animal impossible : " + e);
            Contenant.message(joueur, t("menu.erreur", "<red>Ouverture impossible, préviens un opérateur"), true);
        }
    }

    private void retirer(Player joueur, Entity vehicule) {
        if (!aPortee(joueur, vehicule)) {
            return;
        }
        PersistentDataContainer donnees = vehicule.getPersistentDataContainer();
        donnees.remove(cleProprietaire);
        donnees.remove(cleNom);
        suivis.remove(vehicule.getUniqueId());
        if (vehicule instanceof ChestedHorse cheval) {
            cheval.setCollidable(true);
        }
        Entity siege = siege(vehicule);
        if (siege != null) {
            siege.eject();
            siege.remove();
        }
        for (ItemStack reste : joueur.getInventory().addItem(creerVerrou()).values()) {
            joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
        }
        vehicule.getWorld().playSound(vehicule.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1f, 1.2f);
        Contenant.message(joueur, t("menu.retire", "<green>Verrou retiré"), false);
        Contenant.fermer(joueur);
    }

    /** Coffre ouvert par un autre moyen (touche d'inventaire à bord, etc.) : même règle. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onOuvrir(InventoryOpenEvent event) {
        if (event.getInventory().getHolder(false) instanceof Entity vehicule && event.getPlayer() instanceof Player joueur
                && protege(vehicule) && !estProprietaire(joueur, vehicule) && !joueur.hasPermission(CONTOURNER)) {
            event.setCancelled(true);
            refuser(joueur, vehicule);
        }
    }

    /** Entonnoir ou wagonnet à entonnoir : ne vide pas un contenant protégé (le remplir reste possible). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVider(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder(false) instanceof Entity vehicule && protege(vehicule)) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ casser

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFrapper(VehicleDamageEvent event) {
        if (protege(event.getVehicle())) {
            event.setCancelled(true);
            if (event.getAttacker() instanceof Player joueur) {
                refuser(joueur, event.getVehicle());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCasser(VehicleDestroyEvent event) {
        if (protege(event.getVehicle())) {
            event.setCancelled(true);
        }
    }

    /** Âne, mule, lama protégés : aucun dégât (sauf le vide et /kill). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlesser(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (event.getEntity() instanceof ChestedHorse cheval && cause != EntityDamageEvent.DamageCause.VOID
                && cause != EntityDamageEvent.DamageCause.KILL && protege(cheval)) {
            event.setCancelled(true);
            if (event instanceof EntityDamageByEntityEvent coup && coup.getDamager() instanceof Player joueur) {
                refuser(joueur, cheval);
            }
        }
    }

    /** Véhicule cassé (vide) : il lâche son verrou. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDetruit(VehicleDestroyEvent event) {
        Entity vehicule = event.getVehicle();
        if (verrouillable(vehicule) && verrouille(vehicule)) {
            vehicule.getPersistentDataContainer().remove(cleProprietaire);
            vehicule.getWorld().dropItemNaturally(vehicule.getLocation(), creerVerrou());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMourir(EntityDeathEvent event) {
        if (event.getEntity() instanceof ChestedHorse cheval && verrouille(cheval)) {
            event.getDrops().add(creerVerrou());
        }
    }

    /** Cisailles : un autre ne retire pas la selle ni le tapis d'un animal protégé. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTondre(PlayerShearEntityEvent event) {
        Entity cible = event.getEntity();
        if (protege(cible) && !estProprietaire(event.getPlayer(), cible)
                && !event.getPlayer().hasPermission(CONTOURNER)) {
            event.setCancelled(true);
            refuser(event.getPlayer(), cible);
        }
    }

    // ------------------------------------------------------------------ monter

    private boolean estSiege(Entity entite) {
        return entite instanceof ArmorStand
                && entite.getPersistentDataContainer().has(cleSiege, PersistentDataType.BYTE);
    }

    private Entity siege(Entity vehicule) {
        for (Entity passager : vehicule.getPassengers()) {
            if (estSiege(passager)) {
                return passager;
            }
        }
        return null;
    }

    /**
     * Après KS_JukeboxBoat (personne ne monte dans son bateau à coffre et jukebox). Les animaux et monstres ne montent
     * pas ; le propriétaire et ceux qui peuvent le déplacer montent normalement (ils conduisent) ; les autres joueurs
     * s'assoient sur le siège, si la place est libre.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonter(VehicleEnterEvent event) {
        Entity vehicule = event.getVehicle();
        Entity entrant = event.getEntered();
        if (estSiege(entrant) || !protege(vehicule)) {
            return;
        }
        if (!(entrant instanceof Player joueur)) {
            event.setCancelled(true);
            return;
        }
        if (peutDeplacer(joueur, vehicule)) {
            return;
        }
        event.setCancelled(true);
        getServer().getScheduler().runTask(this, () -> asseoir(joueur, vehicule));
    }

    private void asseoir(Player joueur, Entity vehicule) {
        if (!joueur.isOnline() || !vehicule.isValid() || joueur.isInsideVehicle()
                || !vehicule.getPassengers().isEmpty() || !joueur.getWorld().equals(vehicule.getWorld())
                || joueur.getLocation().distanceSquared(vehicule.getLocation()) > 25) {
            return;
        }
        ArmorStand siege = vehicule.getWorld().spawn(vehicule.getLocation(), ArmorStand.class, s -> {
            s.setInvisible(true);
            s.setMarker(true);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setSilent(true);
            s.setBasePlate(false);
            s.setPersistent(false);
            s.getPersistentDataContainer().set(cleSiege, PersistentDataType.BYTE, (byte) 1);
        });
        if (!vehicule.addPassenger(siege) || !siege.addPassenger(joueur)) {
            siege.eject();
            siege.remove();
            return;
        }
        Contenant.message(joueur, t("passager", "<yellow>Passager : seul <nom> conduit ce véhicule",
                "nom", nom(vehicule)), false);
    }

    /** Le propriétaire monte : le passager du siège descend, le propriétaire conduit. */
    private void reprendre(Player joueur, Entity vehicule, Entity siege) {
        for (Entity passager : siege.getPassengers()) {
            if (passager instanceof Player autre) {
                Contenant.message(autre, t("place-reprise", "<yellow><nom> reprend sa place", "nom",
                        joueur.getName()), false);
            }
        }
        siege.eject();
        siege.remove();
        getServer().getScheduler().runTask(this, () -> {
            if (joueur.isOnline() && vehicule.isValid() && !joueur.isInsideVehicle()
                    && vehicule.getPassengers().isEmpty()) {
                vehicule.addPassenger(joueur);
            }
        });
    }

    /** Siège quitté (le joueur descend, se déconnecte) ou éjecté du véhicule (anti-triche) : il disparaît. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDescendre(EntityDismountEvent event) {
        Entity siege = estSiege(event.getDismounted()) ? event.getDismounted()
                : estSiege(event.getEntity()) ? event.getEntity() : null;
        if (siege != null) {
            getServer().getScheduler().runTask(this, () -> {
                if (siege.isValid()) {
                    siege.eject();
                    siege.remove();
                }
            });
        }
    }

    // ------------------------------------------------------------------ déplacer

    /** Personne d'autre ne pousse un véhicule protégé ni n'y est pris à bord ; une poussée permise le libère. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPousser(VehicleEntityCollisionEvent event) {
        Entity vehicule = event.getVehicle();
        Entity autre = event.getEntity();
        if (!protege(vehicule) || estSiege(autre) || vehicule.getPassengers().contains(autre)) {
            return;
        }
        if (autre instanceof Player joueur && peutDeplacer(joueur, vehicule)) {
            Suivi suivi = suivis.get(vehicule.getUniqueId());
            if (suivi != null) {
                suivi.libreJusqua = tick + LIBRE;
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAttacher(PlayerLeashEntityEvent event) {
        Entity cible = event.getEntity();
        if (protege(cible) && !peutDeplacer(event.getPlayer(), cible)) {
            event.setCancelled(true);
            refuser(event.getPlayer(), cible);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDetacher(PlayerUnleashEntityEvent event) {
        Entity cible = event.getEntity();
        Player joueur = event.getPlayer();
        if (protege(cible) && !peutDeplacer(joueur, cible)
                && !(cible instanceof Leashable tenu && tenu.isLeashed() && joueur.equals(tenu.getLeashHolder()))) {
            event.setCancelled(true);
            refuser(joueur, cible);
        }
    }

    /**
     * Chaque tick : un bateau ou un wagonnet protégé qui n'est ni conduit par quelqu'un qui peut le déplacer, ni tenu
     * en laisse (seuls ceux qui peuvent le déplacer l'attachent), ni poussé par eux depuis peu (ou encore en train de
     * rouler après cette poussée), revient à sa place s'il s'en écarte : piston, courant d'eau, rails, explosion. Une
     * descente (chute, eau) est acceptée. Un conducteur qui ne peut plus le déplacer (membre d'un claim qui en sort, ou
     * monté quand le coffre était vide) descend.
     */
    private void ancrer() {
        tick++;
        Iterator<Suivi> iterateur = suivis.values().iterator();
        while (iterateur.hasNext()) {
            Suivi suivi = iterateur.next();
            Entity vehicule = suivi.entite;
            if (!vehicule.isValid()) {
                iterateur.remove();
                continue;
            }
            Location ici = vehicule.getLocation();
            // 1.1.0 : un wagon attelé dans un train de KS_Trains est placé par ce plugin : il n'est pas ancré.
            if (!protege(vehicule) || !ici.getWorld().equals(suivi.ancre.getWorld())
                    || (CLE_TRAIN != null && vehicule.getPersistentDataContainer().has(CLE_TRAIN, PersistentDataType.STRING))) {
                suivi.ancre = ici;
                continue;
            }
            List<Entity> passagers = vehicule.getPassengers();
            if (!passagers.isEmpty() && passagers.get(0) instanceof Player conducteur) {
                if (peutDeplacer(conducteur, vehicule)) {
                    suivi.ancre = ici;
                    continue;
                }
                vehicule.removePassenger(conducteur);
                refuser(conducteur, vehicule);
            }
            // Un animal marche de lui-même : il n'est pas ancré.
            if (vehicule instanceof ChestedHorse) {
                continue;
            }
            boolean pousse = tick <= suivi.libreJusqua;
            if (pousse && vehicule.getVelocity().lengthSquared() > ARRET * ARRET) {
                suivi.libreJusqua = Math.max(suivi.libreJusqua, tick + 5);
            }
            if (pousse || (vehicule instanceof Leashable tenu && tenu.isLeashed())) {
                suivi.ancre = ici;
                continue;
            }
            double dx = ici.getX() - suivi.ancre.getX();
            double dz = ici.getZ() - suivi.ancre.getZ();
            if (dx * dx + dz * dz <= ECART * ECART) {
                suivi.ancre.setY(ici.getY());
                continue;
            }
            vehicule.setVelocity(new Vector());
            vehicule.teleport(suivi.ancre.clone(), TeleportFlag.EntityState.RETAIN_PASSENGERS);
        }
    }

    /**
     * Chaque seconde, animaux protégés : ils ne sont plus poussés par les autres entités, et un lama ne suit une
     * caravane que si elle est menée en laisse par quelqu'un qui peut le déplacer.
     */
    private void surveiller() {
        for (Suivi suivi : suivis.values()) {
            if (!(suivi.entite instanceof ChestedHorse cheval) || !cheval.isValid()) {
                continue;
            }
            boolean protege = protege(cheval);
            if (cheval.isCollidable() == protege) {
                cheval.setCollidable(!protege);
            }
            if (protege && cheval instanceof Llama lama && lama.inCaravan()) {
                Llama tete = lama;
                for (int i = 0; i < 64 && tete.getCaravanHead() != null; i++) {
                    tete = tete.getCaravanHead();
                }
                if (!(tete.isLeashed() && tete.getLeashHolder() instanceof Player meneur
                        && peutDeplacer(meneur, lama))) {
                    lama.leaveCaravan();
                }
            }
        }
    }
}
