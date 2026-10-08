package fr.kalium.enclume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KS_Enclume (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md) - demande de Maxster33, 25/09/2026 : « possibilité
 * de réparer ou fusion sans contrainte de prix trop couteux ( prix calculé selon les règles vanilla , juste sans
 * plafond maximum ) ».
 *
 * Le jeu calcule le coût comme d'habitude (pénalité des réparations successives comprise) ; seul le plafond de 40
 * niveaux ("Trop cher !") est levé. Il est levé à l'ouverture de l'enclume (avant tout calcul) et à chaque calcul.
 *
 * 1.1.0 (Maxster33, 29/09/2026) : le jeu du joueur affiche lui-même « Trop cher ! » dès 40 niveaux. Au-delà de 39, le
 * coût envoyé au joueur est donc plafonné à 39 et le vrai coût est indiqué (1.1.1 : dans la description du résultat). À la prise, l'enclume
 * retire les 39 niveaux affichés et le plugin retire le reste (le joueur doit avoir le vrai coût en niveaux).
 *
 * 1.1.1 : le résultat au-delà de 39 niveaux est renvoyé au joueur (sinon son jeu le masquait : croix rouge) ; vrai coût
 * en dernière ligne de sa description (la barre d'action était cachée par l'enclume), retirée à la prise.
 *
 * 1.1.2 : au-delà de 39 niveaux, l'enclume n'affiche aucun coût (0 envoyé au joueur) ; le vrai coût est rendu à
 * l'enclume au moment de la prise, qui retire elle-même les niveaux.
 *
 * 1.1.3 : joueurs Bedrock (leur jeu refuse la prise à coût 0) : 39 s'ils ont assez de niveaux, sinon « Trop cher ! ».
 *
 * 1.1.4 : joueurs Bedrock : leur jeu calcule lui-même l'aperçu du résultat (la ligne ajoutée au résultat n'y apparaît
 * pas), mais montre la description des objets d'entrée envoyée par le serveur. La ligne « Coût réel » est donc ajoutée
 * à l'objet de la 1re case (marqueur invisible), ce qui la fait aussi apparaître dans leur aperçu. Elle est retirée dès
 * que l'objet quitte l'enclume : prise du résultat, clic ou glisser (inventaire et curseur nettoyés au tick suivant),
 * fermeture (cases d'entrée nettoyées avant que le jeu ne rende les objets), et par sécurité à la connexion et à
 * l'ouverture d'une enclume.
 *
 * 1.2.0 (Maxster33, 01/10/2026) : « l'augmentation du prix en expérience pour la réparation et l'amélioration d'objet
 * [...] réduite de moitié à partir du niveau 50 » ; choix : coût vanilla jusqu'à 50, partie au-dessus comptée pour
 * moitié (70 -> 60, 100 -> 75). Les fioles de KS_FioleExp gardent leur propre coût.
 *
 * 1.3.0 (Maxster33, 08/10/2026) : « ajouter le coût en niveaux nécessaire pour la réparation directement dans les
 * descriptions des équipements » ; choix : réparation complète avec le matériau de l'objet, sur tout objet réparable.
 * Dernière ligne de la description des objets de l'inventaire (voir Reparation).
 */
public final class KSEnclume extends JavaPlugin implements Listener {

    /** Au-dela, le jeu du joueur affiche « Trop cher ! ». */
    private static final int MAX_AFFICHE = 39;
    /** 1.1.2 : coût envoyé au joueur au-delà de 39 niveaux ; à 0, son jeu n'écrit aucune ligne de coût. */
    private static final int COUT_ENVOYE = 0;
    private static final int RESULT_SLOT = 2;
    /** 1.2.0 : au-delà de ce coût, la partie en plus compte pour moitié. */
    private static final int SEUIL_REDUCTION = 50;

    /** Vrai coût (> 39) du résultat affiché dans l'enclume ouverte par chaque joueur, et le résultat sans la ligne ajoutée. */
    private record Etat(int cout, ItemStack resultat) {
    }

    private final Map<UUID, Etat> etats = new HashMap<>();

    /** 1.1.4 : marque la ligne « Coût réel » ajoutée à l'objet de la 1re case (joueurs Bedrock). */
    private NamespacedKey marque;

    @Override
    public void onEnable() {
        marque = new NamespacedKey(this, "ligne_cout");
        getServer().getPluginManager().registerEvents(this, this);
        Reparation reparation = new Reparation(this);
        getServer().getPluginManager().registerEvents(reparation, this);
        getServer().getScheduler().runTaskTimer(this, reparation::toutMettreAJour, 20L, 20L);
    }

    /** 1.2.0 : coût réduit au-delà de 50 niveaux (partie au-dessus comptée pour moitié) ; aussi pour 1.3.0. */
    static int reduire(int cout) {
        return cout > SEUIL_REDUCTION ? SEUIL_REDUCTION + (cout - SEUIL_REDUCTION) / 2 : cout;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getView() instanceof AnvilView anvil) {
            anvil.setMaximumRepairCost(Integer.MAX_VALUE);
            if (event.getPlayer() instanceof Player player) {
                nettoyerJoueur(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepare(PrepareAnvilEvent event) {
        event.getView().setMaximumRepairCost(Integer.MAX_VALUE);
    }

    /**
     * 1.2.0 : au-delà de 50 niveaux, la partie du coût vanilla au-dessus de 50 compte pour moitié (arrondi à l'unité
     * inférieure). Priorité NORMAL : avant KS_FioleExp (HIGH), qui impose ensuite son propre coût pour une fiole, et avant
     * l'affichage (HIGHEST), qui voit donc déjà le coût réduit.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareReduction(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        int cout = view.getRepairCost();
        if (cout > SEUIL_REDUCTION) {
            view.setRepairCost(reduire(cout));
        }
    }

    /**
     * Apres tous les autres plugins (KS_FioleExp compris) : plafonne le coût affiché à 39 et ajoute le vrai coût en
     * dernière ligne de la description du résultat (1.1.1 : la barre d'action était cachée par l'enclume).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAffichage(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        if (!(view.getPlayer() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getResult();
        int cout = view.getRepairCost();
        if (result == null || result.isEmpty() || cout <= MAX_AFFICHE) {
            etats.remove(player.getUniqueId());
            planifierMarque(player, view);
            return;
        }
        // Le résultat vanilla copie l'objet de la 1re case : sans la ligne ajoutée pour Bedrock.
        ItemStack propre = nettoyer(result.clone());
        etats.put(player.getUniqueId(), new Etat(cout, propre));
        event.setResult(avecCout(propre, cout, assez(player, cout)));
        view.setRepairCost(coutEnvoye(player, cout));
        // 1.1.1 : le jeu du joueur recalcule lui-même le résultat quand les cases changent et le vide dès 40 niveaux
        // (croix rouge). On lui renvoie tout le contenu de l'enclume au tick suivant : il recalcule les cases
        // d'entrée, puis reçoit le résultat et le coût du serveur, qui restent affichés.
        getServer().getScheduler().runTask(this, player::updateInventory);
        planifierMarque(player, view);
    }

    private static boolean assez(Player player, int cout) {
        return player.getLevel() >= cout || player.getGameMode() == GameMode.CREATIVE;
    }

    /**
     * Coût envoyé au joueur au-delà de 39 niveaux. Java : 0 (aucune ligne de coût). Bedrock (1.1.3) : son jeu refuse la
     * prise avec un coût de 0 ; on envoie 39 s'il a assez de niveaux, sinon le vrai coût (son jeu affiche alors
     * « Trop cher ! »). « 40+ » est impossible : le jeu écrit lui-même un nombre.
     */
    private static int coutEnvoye(Player player, int cout) {
        if (!estBedrock(player)) {
            return COUT_ENVOYE;
        }
        return assez(player, cout) ? MAX_AFFICHE : cout;
    }

    /** Joueur Bedrock (Floodgate, lu sans dépendance de compilation ; sinon UUID Floodgate : 64 premiers bits à 0). */
    private static boolean estBedrock(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return (Boolean) api.getMethod("isFloodgatePlayer", UUID.class).invoke(instance, uuid);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return uuid.getMostSignificantBits() == 0;
        }
    }

    private static Component ligneCout(int cout, boolean assez) {
        return Component.text("Coût réel : " + cout + " niveaux", assez ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);
    }

    /** Copie du résultat avec « Coût réel : N niveaux » en dernière ligne (vert si le joueur a assez de niveaux). */
    private static ItemStack avecCout(ItemStack result, int cout, boolean assez) {
        ItemStack affiche = result.clone();
        ItemMeta meta = affiche.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(ligneCout(cout, assez));
        meta.lore(lore);
        affiche.setItemMeta(meta);
        return affiche;
    }

    // ------------------------------------------------------------------ 1.1.4 : ligne sur l'objet de la 1re case (Bedrock)

    /** Copie de l'objet avec la ligne « Coût réel » en dernier et le marqueur. */
    private ItemStack marquer(ItemStack item, int cout, boolean assez) {
        ItemStack marquee = avecCout(item, cout, assez);
        ItemMeta meta = marquee.getItemMeta();
        meta.getPersistentDataContainer().set(marque, PersistentDataType.BYTE, (byte) 1);
        marquee.setItemMeta(meta);
        return marquee;
    }

    private boolean estMarque(ItemStack item) {
        return item != null && !item.isEmpty() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(marque, PersistentDataType.BYTE);
    }

    /** Retire la ligne ajoutée (la dernière) et le marqueur ; renvoie l'objet tel quel s'il n'est pas marqué. */
    private ItemStack nettoyer(ItemStack item) {
        if (!estMarque(item)) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(marque);
        List<Component> lore = meta.lore();
        if (lore != null && !lore.isEmpty()) {
            List<Component> reste = new ArrayList<>(lore.subList(0, lore.size() - 1));
            meta.lore(reste.isEmpty() ? null : reste);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Au tick suivant (on ne change pas une case pendant le calcul de l'enclume) : met la 1re case dans l'état voulu,
     * avec la ligne si le coût dépasse 39 pour un joueur Bedrock, sans sinon. Le changement relance le calcul, qui
     * retombe sur le même état : pas de boucle.
     */
    private void planifierMarque(Player player, AnvilView view) {
        if (!estBedrock(player)) {
            return;
        }
        getServer().getScheduler().runTask(this, () -> {
            if (!player.getOpenInventory().getTopInventory().equals(view.getTopInventory())) {
                return;
            }
            AnvilInventory inventory = (AnvilInventory) view.getTopInventory();
            ItemStack premier = inventory.getFirstItem();
            if (premier == null || premier.isEmpty()) {
                return;
            }
            Etat etat = etats.get(player.getUniqueId());
            ItemStack voulu = nettoyer(premier.clone());
            if (etat != null) {
                voulu = marquer(voulu, etat.cout(), assez(player, etat.cout()));
            }
            if (!voulu.equals(premier)) {
                inventory.setFirstItem(voulu);
            }
        });
    }

    /** Retire la ligne de tous les objets marqués de l'inventaire du joueur et de son curseur. */
    private void nettoyerJoueur(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (estMarque(item)) {
                inventory.setItem(i, nettoyer(item.clone()));
            }
        }
        ItemStack curseur = player.getItemOnCursor();
        if (estMarque(curseur)) {
            player.setItemOnCursor(nettoyer(curseur.clone()));
        }
    }

    /** Tout clic ou glisser dans une enclume : un objet marqué a pu passer dans l'inventaire ou sous le curseur. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClicEnclume(InventoryClickEvent event) {
        if (event.getView() instanceof AnvilView && event.getWhoClicked() instanceof Player player) {
            getServer().getScheduler().runTask(this, () -> nettoyerJoueur(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGlisserEnclume(InventoryDragEvent event) {
        if (event.getView() instanceof AnvilView && event.getWhoClicked() instanceof Player player) {
            getServer().getScheduler().runTask(this, () -> nettoyerJoueur(player));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        nettoyerJoueur(event.getPlayer());
    }

    // ------------------------------------------------------------------ prise et fermeture

    /**
     * Prise du résultat : le résultat sans la ligne ajoutée est remis dans la case et le vrai coût est rendu à l'enclume
     * juste avant la prise : l'enclume vanilla vérifie et retire elle-même les niveaux (1.1.2). Si la prise n'a pas eu
     * lieu (ex. curseur occupé), l'affichage est remis au tick suivant. Refusée si le joueur n'a pas le vrai coût. Les
     * prises gérées par un autre plugin (clic annulé, ex. KS_FioleExp, qui donne sa propre fiole) sont ignorées.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTake(InventoryClickEvent event) {
        if (!(event.getView() instanceof AnvilView view) || event.getRawSlot() != RESULT_SLOT
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Etat etat = etats.get(player.getUniqueId());
        ItemStack result = event.getCurrentItem();
        if (etat == null || result == null || result.isEmpty()) {
            return;
        }
        if (!assez(player, etat.cout())) {
            event.setCancelled(true);
            return;
        }
        event.setCurrentItem(etat.resultat().clone());
        view.setRepairCost(etat.cout());
        getServer().getScheduler().runTask(this, () -> {
            ItemStack reste = view.getTopInventory().getItem(RESULT_SLOT);
            // Pas pris : la case contient encore le résultat propre remis ci-dessus.
            if (player.getOpenInventory().getTopInventory().equals(view.getTopInventory())
                    && etat.resultat().equals(reste)) {
                view.getTopInventory().setItem(RESULT_SLOT, avecCout(reste, etat.cout(), assez(player, etat.cout())));
                view.setRepairCost(coutEnvoye(player, etat.cout()));
                player.updateInventory();
            }
        });
    }

    /** Fermeture : les cases d'entrée sont nettoyées avant que le jeu ne rende les objets (ou les fasse tomber). */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getView() instanceof AnvilView view)) {
            return;
        }
        AnvilInventory inventory = (AnvilInventory) view.getTopInventory();
        if (estMarque(inventory.getFirstItem())) {
            inventory.setFirstItem(nettoyer(inventory.getFirstItem().clone()));
        }
        if (estMarque(inventory.getSecondItem())) {
            inventory.setSecondItem(nettoyer(inventory.getSecondItem().clone()));
        }
        etats.remove(event.getPlayer().getUniqueId());
        if (event.getPlayer() instanceof Player player) {
            getServer().getScheduler().runTask(this, () -> {
                if (player.isOnline()) {
                    nettoyerJoueur(player);
                }
            });
        }
    }
}
