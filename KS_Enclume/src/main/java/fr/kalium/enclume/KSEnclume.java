package fr.kalium.enclume;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * coût envoyé au joueur est donc plafonné à 39 et le vrai coût est écrit dans la barre d'action. À la prise, l'enclume
 * retire les 39 niveaux affichés et le plugin retire le reste (le joueur doit avoir le vrai coût en niveaux).
 *
 * 1.1.1 : le résultat au-delà de 39 niveaux est renvoyé au joueur (sinon son jeu le masquait : croix rouge).
 */
public final class KSEnclume extends JavaPlugin implements Listener {

    /** Au-dela, le jeu du joueur affiche « Trop cher ! ». */
    private static final int MAX_AFFICHE = 39;
    private static final int RESULT_SLOT = 2;

    /** Vrai coût (> 39) du résultat affiché dans l'enclume ouverte par chaque joueur. */
    private final Map<UUID, Integer> vraiCout = new HashMap<>();

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

    /** Apres tous les autres plugins (KS_FioleExp compris) : plafonne le coût affiché à 39. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAffichage(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        if (!(view.getPlayer() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getResult();
        int cout = view.getRepairCost();
        if (result == null || result.isEmpty() || cout <= MAX_AFFICHE) {
            vraiCout.remove(player.getUniqueId());
            return;
        }
        vraiCout.put(player.getUniqueId(), cout);
        view.setRepairCost(MAX_AFFICHE);
        player.sendActionBar(Component.text("Coût réel : " + cout + " niveaux",
                player.getLevel() >= cout ? NamedTextColor.GREEN : NamedTextColor.RED));
        // 1.1.1 : le jeu du joueur recalcule lui-même le résultat quand les cases changent et le vide dès 40 niveaux
        // (croix rouge). On lui renvoie tout le contenu de l'enclume au tick suivant : il recalcule les cases
        // d'entrée, puis reçoit le résultat et le coût (39) du serveur, qui restent affichés.
        getServer().getScheduler().runTask(this, player::updateInventory);
    }

    /**
     * Prise du résultat : l'enclume vanilla retire les 39 niveaux affichés, le plugin retire le reste au tick suivant
     * si la prise a bien eu lieu. Les prises gérées par un autre plugin (clic annulé, ex. KS_FioleExp) sont ignorées.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTake(InventoryClickEvent event) {
        if (!(event.getView() instanceof AnvilView) || event.getRawSlot() != RESULT_SLOT
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Integer cout = vraiCout.get(player.getUniqueId());
        ItemStack result = event.getCurrentItem();
        if (cout == null || result == null || result.isEmpty() || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        int avant = player.getLevel();
        if (avant < cout) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Pas assez de niveaux : il en faut " + cout + ".", NamedTextColor.RED));
            return;
        }
        int reste = cout - MAX_AFFICHE;
        getServer().getScheduler().runTask(this, () -> {
            if (player.isOnline() && player.getLevel() <= avant - MAX_AFFICHE) {
                player.giveExpLevels(-reste);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView() instanceof AnvilView) {
            vraiCout.remove(event.getPlayer().getUniqueId());
        }
    }
}
