package fr.kalium.villageois;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * KS_Villageois (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md) - demande de Maxster33, 25/09/2026 : « les
 * villageois n'ont plus de trade par défaut , aucun moyen de les lvl up etc , seuls les pnj donnés a l'avenir en
 * auront ».
 *
 * Villageois et marchands ambulants ne reçoivent plus aucun échange (ils gardent leur métier) : sans échange, pas
 * d'expérience, donc pas de niveau. Un échange déjà présent est retiré quand un joueur interagit avec l'entité.
 * Les futurs PNJ à échanges portent l'étiquette {@value #PNJ_TAG} (commande /tag) : ils ne sont jamais touchés.
 */
public final class KSVillageois extends JavaPlugin implements Listener {

    public static final String PNJ_TAG = "ks_pnj";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private static boolean managed(AbstractVillager villager) {
        return !villager.getScoreboardTags().contains(PNJ_TAG);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAcquireTrade(VillagerAcquireTradeEvent event) {
        if (managed(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof AbstractVillager villager && managed(villager)
                && villager.getRecipeCount() > 0) {
            villager.setRecipes(List.of());
        }
    }
}
