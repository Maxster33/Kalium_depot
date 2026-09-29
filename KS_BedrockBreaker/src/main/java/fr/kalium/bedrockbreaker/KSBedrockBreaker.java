package fr.kalium.bedrockbreaker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * KS_BedrockBreaker (demande de Maxster33, 29/09/2026) : le Bedrock Breaker, sorti de KS_Crafts (qui garde la recette).
 *
 * Objet : livre de connaissances (ni outil, ni bloc, ni ingrédient de craft) avec l'image du bâton de blaze, nommé
 * « Bedrock Breaker », description « Utilisation unique », empilable par 64. Son clic droit vanilla est annulé.
 * Clic droit sur un bloc de bedrock : le bloc disparaît (aucun drop) et un Bedrock Breaker est consommé. Refusé :
 * - sur la couche du fond de la dimension (hauteur minimale du monde), pour ne pas ouvrir le vide ;
 * - là où les protections l'interdisent : un cassage de bloc est simulé (BlockBreakEvent), que WorldGuard (ou tout
 *   autre plugin de protection) annule dans une région protégée ; en mode aventure aussi.
 *
 * 1.0.1 : un bloc par seconde au plus et par joueur (un clic répété cassait aussi le bloc juste derrière).
 *
 * Les anciens Bedrock Breaker (houe en bois marquée par KS_Crafts 1.0.0 à 1.3.0) marchent encore, avec ces règles.
 * Autres plugins : creerBreaker() (KS_Crafts, KS_KaliumGive).
 */
public final class KSBedrockBreaker extends JavaPlugin implements Listener {

    /** Marqueur des anciens Bedrock Breaker (houe en bois de KS_Crafts). */
    private static final NamespacedKey ANCIEN_MARQUEUR = new NamespacedKey("ks_crafts", "bedrock_breaker");

    /** 1.0.1 : délai minimal entre deux blocs cassés par un même joueur. */
    private static final long DELAI_MS = 1000;

    private static NamespacedKey marqueur;

    /** Heure du dernier bloc cassé par chaque joueur. */
    private final Map<UUID, Long> dernierCassage = new HashMap<>();

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "bedrock_breaker");
        getServer().getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------ objet

    /** Un Bedrock Breaker. Nécessite que le plugin soit activé. */
    public static ItemStack creerBreaker() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Bedrock Breaker", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Utilisation unique", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setItemModel(NamespacedKey.minecraft("blaze_rod"));
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean estBreaker(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer donnees = item.getItemMeta().getPersistentDataContainer();
        return donnees.has(marqueur, PersistentDataType.BYTE)
                || (item.getType() == Material.WOODEN_HOE && donnees.has(ANCIEN_MARQUEUR, PersistentDataType.BYTE));
    }

    // ------------------------------------------------------------------ utilisation

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        EquipmentSlot main = event.getHand();
        if ((action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) || main == null
                || !estBreaker(event.getItem())) {
            return;
        }
        // Annule l'effet vanilla (livre de connaissances ; ancienne houe : labourer).
        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (action != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.BEDROCK) {
            return;
        }
        Player player = event.getPlayer();
        if (block.getY() <= block.getWorld().getMinHeight()) {
            player.sendActionBar(Component.text("La couche du fond ne peut pas être cassée.", NamedTextColor.RED));
            return;
        }
        if (player.getGameMode() == GameMode.ADVENTURE) {
            return;
        }
        // 1.0.1 : clic droit maintenu (répété par le jeu) ou clic Bedrock en double : sans ce délai, le bloc de bedrock
        // juste derrière était cassé aussi, avec un 2e Bedrock Breaker.
        long maintenant = System.currentTimeMillis();
        Long dernier = dernierCassage.get(player.getUniqueId());
        if (dernier != null && maintenant - dernier < DELAI_MS) {
            return;
        }
        ItemStack enMain = player.getInventory().getItem(main);
        if (!estBreaker(enMain)) {
            return;
        }
        // Protections : WorldGuard annule ce cassage simulé dans une région protégée (et affiche son message).
        BlockBreakEvent cassage = new BlockBreakEvent(block, player);
        cassage.setDropItems(false);
        cassage.setExpToDrop(0);
        if (!cassage.callEvent()) {
            return;
        }
        dernierCassage.put(player.getUniqueId(), maintenant);
        block.setType(Material.AIR);
        enMain.setAmount(enMain.getAmount() - 1);
        player.getInventory().setItem(main, enMain.getAmount() > 0 ? enMain : null);
    }
}
