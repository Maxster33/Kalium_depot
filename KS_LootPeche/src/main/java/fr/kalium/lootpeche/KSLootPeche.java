package fr.kalium.lootpeche;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_LootPeche (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md) - demande de Maxster33, 25/09/2026 : « les
 * livres enchantés sont retirés des loot table » ; réponse : « tire au sort » un autre trésor.
 *
 * 1.1.0 (LeKiwi06, 26/10/2026 : « remet les livre enchantés dans la pêche , mais retire le fait que le mending puisse
 * spawn ») : les livres enchantés sont de nouveau pêchés (table vanilla) ; un livre, un arc ou une canne à pêche pêché
 * avec Raccommodage est réenchanté de la même façon que le jeu (niveau 30, enchantements de trésor compris) jusqu'à ne
 * plus l'avoir. Avant : chaque livre était remplacé par un autre trésor (arc, canne, étiquette, carapace, selle).
 */
public final class KSLootPeche extends JavaPlugin implements Listener {

    /** Tirages au plus avant de retirer simplement Raccommodage (il sort rarement : jamais atteint en pratique). */
    private static final int ESSAIS = 50;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        ItemStack objet = caught.getItemStack();
        if (raccommodage(objet)) {
            caught.setItemStack(sansRaccommodage(objet));
        }
    }

    private static boolean raccommodage(ItemStack item) {
        if (item.getItemMeta() instanceof EnchantmentStorageMeta livre) {
            return livre.hasStoredEnchant(Enchantment.MENDING);
        }
        return item.containsEnchantment(Enchantment.MENDING);
    }

    /** Le même objet, réenchanté comme le fait la pêche vanilla jusqu'à ne plus avoir Raccommodage. */
    private static ItemStack sansRaccommodage(ItemStack item) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ItemStack vierge = new ItemStack(item.getType() == Material.ENCHANTED_BOOK ? Material.BOOK : item.getType());
        for (int i = 0; i < ESSAIS; i++) {
            ItemStack nouveau = Bukkit.getItemFactory().enchantWithLevels(vierge.clone(), 30, true, random);
            if (!raccommodage(nouveau)) {
                garderUsure(item, nouveau);
                return nouveau;
            }
        }
        ItemStack reste = item.clone();
        if (reste.getItemMeta() instanceof EnchantmentStorageMeta livre) {
            livre.removeStoredEnchant(Enchantment.MENDING);
            reste.setItemMeta(livre);
        } else {
            reste.removeEnchantment(Enchantment.MENDING);
        }
        return reste;
    }

    /** Usure de l'arc ou de la canne pêchés (tirée par le jeu) gardée sur l'objet réenchanté. */
    private static void garderUsure(ItemStack ancien, ItemStack nouveau) {
        if (ancien.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable a
                && nouveau.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable n && a.hasDamage()) {
            n.setDamage(a.getDamage());
            nouveau.setItemMeta(n);
        }
    }
}
