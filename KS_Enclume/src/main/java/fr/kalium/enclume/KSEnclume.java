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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
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
 */
public final class KSEnclume extends JavaPlugin implements Listener {

    /** Au-dela, le jeu du joueur affiche « Trop cher ! ». */
    private static final int MAX_AFFICHE = 39;
    /** 1.1.2 : coût envoyé au joueur au-delà de 39 niveaux ; à 0, son jeu n'écrit aucune ligne de coût. */
    private static final int COUT_ENVOYE = 0;
    private static final int RESULT_SLOT = 2;

    /** Vrai coût (> 39) du résultat affiché dans l'enclume ouverte par chaque joueur, et le résultat sans la ligne ajoutée. */
    private record Etat(int cout, ItemStack resultat) {
    }

    private final Map<UUID, Etat> etats = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getView() instanceof AnvilView anvil) {
            anvil.setMaximumRepairCost(Integer.MAX_VALUE);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepare(PrepareAnvilEvent event) {
        event.getView().setMaximumRepairCost(Integer.MAX_VALUE);
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
            return;
        }
        etats.put(player.getUniqueId(), new Etat(cout, result.clone()));
        event.setResult(avecCout(result, cout, player.getLevel() >= cout));
        view.setRepairCost(coutEnvoye(player, cout));
        // 1.1.1 : le jeu du joueur recalcule lui-même le résultat quand les cases changent et le vide dès 40 niveaux
        // (croix rouge). On lui renvoie tout le contenu de l'enclume au tick suivant : il recalcule les cases
        // d'entrée, puis reçoit le résultat et le coût du serveur, qui restent affichés.
        getServer().getScheduler().runTask(this, player::updateInventory);
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
        return player.getLevel() >= cout || player.getGameMode() == GameMode.CREATIVE ? MAX_AFFICHE : cout;
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

    /** Copie du résultat avec « Coût réel : N niveaux » en dernière ligne (vert si le joueur a assez de niveaux). */
    private static ItemStack avecCout(ItemStack result, int cout, boolean assez) {
        ItemStack affiche = result.clone();
        ItemMeta meta = affiche.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.text("Coût réel : " + cout + " niveaux", assez ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        affiche.setItemMeta(meta);
        return affiche;
    }

    /**
     * Prise du résultat : le résultat sans la ligne ajoutée est remis dans la case et le vrai coût est rendu à l'enclume
     * juste avant la prise : l'enclume vanilla vérifie et retire elle-même les niveaux (1.1.2). Si la prise n'a pas eu
     * lieu (ex. curseur occupé), l'affichage est remis au tick suivant. Refusée si le joueur n'a pas le vrai coût. Les prises gérées par un autre plugin (clic annulé, ex. KS_FioleExp, qui donne sa
     * propre fiole) sont ignorées.
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
        int avant = player.getLevel();
        boolean creatif = player.getGameMode() == GameMode.CREATIVE;
        if (!creatif && avant < etat.cout()) {
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
                view.getTopInventory().setItem(RESULT_SLOT, avecCout(reste, etat.cout(), player.getLevel() >= etat.cout()));
                view.setRepairCost(coutEnvoye(player, etat.cout()));
                player.updateInventory();
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView() instanceof AnvilView) {
            etats.remove(event.getPlayer().getUniqueId());
        }
    }
}
