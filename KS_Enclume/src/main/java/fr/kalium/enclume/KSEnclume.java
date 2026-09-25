package fr.kalium.enclume;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KS_Enclume (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md) - demande de Maxster33, 25/09/2026 : « possibilité
 * de réparer ou fusion sans contrainte de prix trop couteux ( prix calculé selon les règles vanilla , juste sans
 * plafond maximum ) ».
 *
 * Le jeu calcule le coût comme d'habitude (pénalité des réparations successives comprise) ; seul le plafond de 40
 * niveaux ("Trop cher !") est levé. Il est levé à l'ouverture de l'enclume (avant tout calcul) et à chaque calcul.
 */
public final class KSEnclume extends JavaPlugin implements Listener {

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
}
