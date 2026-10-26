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
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_LootPeche (cahier des charges : KS_Event/CAHIER_DES_CHARGES.md) - demande de Maxster33, 25/09/2026 : « les
 * livres enchantés sont retirés des loot table » ; réponse : « tire au sort » un autre trésor.
 *
 * 1.1.0 (LeKiwi06, 26/10/2026 : « remet les livre enchantés dans la pêche , mais retire le fait que le mending puisse
 * spawn ») : les livres enchantés sont de nouveau pêchés (table vanilla) ; un livre, un arc ou une canne à pêche pêché
 * avec Raccommodage est réenchanté de la même façon que le jeu (niveau 30, enchantements de trésor compris) jusqu'à ne
 * plus l'avoir. Avant : chaque livre était remplacé par un autre trésor (arc, canne, étiquette, carapace, selle).
 *
 * 1.2.0 (LeKiwi06, 26/10/2026) : prises rares (config.yml, prises-rares) : têtes des mobs marins de KS_Decapitator
 * (bébés et axolotls compris, plus la tête est rare, moins elle sort), tête de wither squelette (ses 3 défauts), fiole
 * sinistre, pomme de Notch. Chaque prise a 1 chance sur N par ligne d'être remplacée par cet objet (un seul tirage par
 * prise). Chances validées par LeKiwi06 (« rend tout 2 fois plus courant » que la 1re proposition).
 */
public final class KSLootPeche extends JavaPlugin implements Listener {

    /** Tirages au plus avant de retirer simplement Raccommodage (il sort rarement : jamais atteint en pratique). */
    private static final int ESSAIS = 50;

    /** 1.2.0 : une prise rare (clé de config.yml) et sa chance par prise. */
    private record PriseRare(String cle, double chance) {
    }

    private final List<PriseRare> prisesRares = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        var section = getConfig().getConfigurationSection("prises-rares");
        if (section != null) {
            for (String cle : section.getKeys(false)) {
                double n = section.getDouble(cle);
                if (n < 1) {
                    getLogger().warning("Prise rare ignorée (1 chance sur N, N doit valoir au moins 1) : " + cle);
                } else if (creer(cle) == null) {
                    getLogger().warning("Prise rare ignorée (objet inconnu ou KS_Decapitator absent) : " + cle);
                } else {
                    prisesRares.add(new PriseRare(cle, 1 / n));
                }
            }
        }
        getLogger().info(prisesRares.size() + " prises rares.");
        getServer().getPluginManager().registerEvents(this, this);
    }

    /** 1.2.0 : objet d'une prise rare, ou null si la clé est inconnue. */
    private ItemStack creer(String cle) {
        if (cle.startsWith("tete:")) {
            return getServer().getPluginManager().isPluginEnabled("KS_Decapitator")
                    ? fr.kalium.decapitator.KSDecapitator.creerTete(cle.substring(5)) : null;
        }
        return switch (cle) {
            case "fiole_sinistre" -> {
                ItemStack fiole = new ItemStack(Material.OMINOUS_BOTTLE);
                if (fiole.getItemMeta() instanceof OminousBottleMeta meta) {
                    meta.setAmplifier(ThreadLocalRandom.current().nextInt(5));
                    fiole.setItemMeta(meta);
                }
                yield fiole;
            }
            case "pomme_de_notch" -> new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
            default -> null;
        };
    }

    /** 1.2.0 : la prise rare tirée pour cette prise, ou null (un seul tirage pour toute la table). */
    private ItemStack priseRare() {
        double tirage = ThreadLocalRandom.current().nextDouble();
        for (PriseRare prise : prisesRares) {
            tirage -= prise.chance();
            if (tirage < 0) {
                return creer(prise.cle());
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        ItemStack rare = priseRare();
        if (rare != null) {
            caught.setItemStack(rare);
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
