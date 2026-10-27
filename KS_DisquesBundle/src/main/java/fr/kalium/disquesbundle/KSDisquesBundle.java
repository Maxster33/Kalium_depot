package fr.kalium.disquesbundle;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_DisquesBundle (demande de LeKiwi06, 27/10/2026 : « fait en sorte que les disques agissent comme un item stackable
 * par 64 dans les bundle » ; choix : seulement dans le bundle, les disques restent non empilables ailleurs).
 *
 * Lu dans le code du jeu 26.3 (BundleContents.getWeight) : le poids d'un objet dans un bundle vaut 1 / sa taille de
 * pile ; un disque (pile de 1) remplit donc le bundle à lui seul. Aucun autre réglage n'existe.
 *
 * Entrée : juste avant un clic gauche qui met un disque dans un bundle (disque sur le bundle, ou bundle sur le disque),
 * le disque reçoit une taille de pile de 64 ; le jeu fait ensuite l'insertion lui-même (poids 1/64, disques
 * identiques regroupés en une pile). Sortie : le jeu sort une pile entière (clic droit, ou bundle vidé par terre) ;
 * le tick suivant, tout disque à 64 trouvé hors d'un bundle (inventaire du joueur, curseur, contenant ouvert) reprend
 * sa taille de pile normale et est séparé en disques à l'unité : un reste là où il est, les autres vont dans
 * l'inventaire du joueur (au sol s'il est plein). Un objet au sol est séparé dès son apparition.
 */
public final class KSDisquesBundle extends JavaPlugin implements Listener {

    /** Taille de pile donnée aux disques dans un bundle (poids 1/64). */
    private static final int PILE = 64;

    /** Joueurs dont l'inventaire est à vérifier au prochain tick. */
    private final Set<UUID> aVerifier = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClic(InventoryClickEvent event) {
        ItemStack curseur = event.getCursor();
        ItemStack case_ = event.getCurrentItem();
        boolean bundleTouche = estBundle(curseur) || estBundle(case_);
        if (!bundleTouche && !estDisque(curseur) && !estDisque(case_)) return;
        if (event.getClick() == ClickType.LEFT) {
            if (estDisque(curseur) && estBundle(case_)) {
                event.getView().setCursor(aPile(curseur));
            } else if (estBundle(curseur) && estDisque(case_)) {
                event.setCurrentItem(aPile(case_));
            }
        }
        verifierAuProchainTick(event.getWhoClicked());
    }

    /** Disque sorti d'un bundle au sol (bundle vidé à la main, bundle détruit) : séparé en disques à l'unité. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onObjetAuSol(ItemSpawnEvent event) {
        Item entite = event.getEntity();
        ItemStack objet = entite.getItemStack();
        if (!estDisqueEnPile(objet)) return;
        int nombre = objet.getAmount();
        ItemStack unite = normal(objet);
        entite.setItemStack(unite);
        Location lieu = entite.getLocation();
        for (int i = 1; i < nombre; i++) {
            lieu.getWorld().dropItem(lieu, unite.clone(), copie -> copie.setVelocity(entite.getVelocity()));
        }
    }

    /** Filet de sécurité : un arrêt du serveur entre la sortie d'un disque et sa vérification. */
    @EventHandler
    public void onConnexion(PlayerJoinEvent event) {
        verifierAuProchainTick(event.getPlayer());
    }

    private void verifierAuProchainTick(HumanEntity joueur) {
        if (!(joueur instanceof Player p) || !aVerifier.add(p.getUniqueId())) return;
        getServer().getScheduler().runTask(this, () -> {
            aVerifier.remove(p.getUniqueId());
            if (p.isOnline()) verifier(p);
        });
    }

    /** Remet à l'unité les disques à 64 hors d'un bundle : curseur, inventaire du joueur, contenant ouvert. */
    private void verifier(Player joueur) {
        Inventory perso = joueur.getInventory();
        ItemStack curseur = joueur.getItemOnCursor();
        if (estDisqueEnPile(curseur)) {
            joueur.setItemOnCursor(normal(curseur));
            ranger(joueur, curseur, curseur.getAmount() - 1);
        }
        Inventory haut = joueur.getOpenInventory().getTopInventory();
        if (haut != perso) separer(joueur, haut);
        separer(joueur, perso);
    }

    private void separer(Player joueur, Inventory inventaire) {
        for (int i = 0; i < inventaire.getSize(); i++) {
            ItemStack objet = inventaire.getItem(i);
            if (!estDisqueEnPile(objet)) continue;
            inventaire.setItem(i, normal(objet));
            ranger(joueur, objet, objet.getAmount() - 1);
        }
    }

    /** Range dans l'inventaire du joueur `nombre` disques à l'unité, ce qui ne rentre pas tombe à ses pieds. */
    private void ranger(Player joueur, ItemStack modele, int nombre) {
        ItemStack unite = normal(modele);
        for (int i = 0; i < nombre; i++) {
            Map<Integer, ItemStack> reste = joueur.getInventory().addItem(unite.clone());
            for (ItemStack r : reste.values()) joueur.getWorld().dropItem(joueur.getLocation(), r);
        }
    }

    private static boolean estBundle(ItemStack objet) {
        return objet != null && Tag.ITEMS_BUNDLES.isTagged(objet.getType());
    }

    private static boolean estDisque(ItemStack objet) {
        return objet != null && objet.getType().isRecord();
    }

    /** Disque dont la taille de pile a été changée (par ce plugin) : à remettre à l'unité hors d'un bundle. */
    private static boolean estDisqueEnPile(ItemStack objet) {
        return estDisque(objet) && objet.isDataOverridden(DataComponentTypes.MAX_STACK_SIZE);
    }

    private static ItemStack aPile(ItemStack disque) {
        ItemStack copie = disque.clone();
        copie.setData(DataComponentTypes.MAX_STACK_SIZE, PILE);
        return copie;
    }

    /** Une copie à l'unité, taille de pile du jeu (1). */
    private static ItemStack normal(ItemStack disque) {
        ItemStack copie = disque.clone();
        copie.resetData(DataComponentTypes.MAX_STACK_SIZE);
        copie.setAmount(1);
        return copie;
    }
}
