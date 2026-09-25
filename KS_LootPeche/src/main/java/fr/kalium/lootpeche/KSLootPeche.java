package fr.kalium.lootpeche;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_LootPeche (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md) - demande de Maxster33, 25/09/2026 : « les
 * livres enchantés sont retirés des loot table » ; réponse : « tire au sort » un autre trésor.
 *
 * Un livre enchanté pêché est remplacé par un des autres trésors de la table vanilla, à chances égales : arc
 * enchanté, canne à pêche enchantée (enchantement de niveau 30 et usure aléatoire jusqu'à 25 %, comme en vanilla),
 * étiquette, carapace de nautile, selle.
 */
public final class KSLootPeche extends JavaPlugin implements Listener {

    private static final List<Material> TREASURES = List.of(
            Material.BOW, Material.FISHING_ROD, Material.NAME_TAG, Material.NAUTILUS_SHELL, Material.SADDLE);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)
                || caught.getItemStack().getType() != Material.ENCHANTED_BOOK) {
            return;
        }
        caught.setItemStack(treasure());
    }

    private ItemStack treasure() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Material type = TREASURES.get(random.nextInt(TREASURES.size()));
        ItemStack item = new ItemStack(type);
        if (type == Material.BOW || type == Material.FISHING_ROD) {
            item = Bukkit.getItemFactory().enchantWithLevels(item, 30, true, random);
            if (item.getItemMeta() instanceof Damageable meta) {
                meta.setDamage((int) (type.getMaxDurability() * random.nextDouble(0.0, 0.25)));
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
