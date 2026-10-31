package fr.kalium.parkour;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 1.7.0 (demande de LeKiwi06, 31/10/2026, map « Ascension vers l'enfer ») : la masse prêtée le temps d'une section,
 * les coups de masse sur les cibles de la section, et l'arrivée par bouton.
 *
 * KalGames annule tout coup porté dans une arène (GameListener, priorité HIGH) ; Wind Burst ne se déclenche que si le
 * coup touche vraiment (Player.attack : effets d'après-coup seulement si hurtOrSimulate réussit). Le coup de masse d'un
 * coureur sur une cible de sa section est donc rétabli ici (HIGHEST) ; la cible a 1 024 points de vie, remis au maximum
 * à chaque passage de la boucle (ParkourInstance.keepTargets). Entre joueurs, rien ne change : canFight reste faux.
 */
final class Masse implements Listener {

    /** Case de la barre d'objets (0 : retour au point de contrôle, 1 : plume de l'entraînement, 4 : menu). */
    static final int SLOT = 2;

    private final KalGames games;
    private final NamespacedKey key;

    Masse(KGParkour plugin, KalGames games) {
        this.games = games;
        this.key = new NamespacedKey(plugin, "masse");
    }

    /** Donne (et met en main) ou retire la masse de la section. */
    void set(Player player, boolean wanted) {
        PlayerInventory inventory = player.getInventory();
        if (!wanted) {
            for (int i = 0; i < inventory.getSize(); i++) {
                if (isOurs(inventory.getItem(i))) {
                    inventory.setItem(i, null);
                }
            }
            return;
        }
        if (!isOurs(inventory.getItem(SLOT))) {
            inventory.setItem(SLOT, item());
        }
        inventory.setHeldItemSlot(SLOT);
    }

    private ItemStack item() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(games.t("race.mace-item-name", "<gold><bold>Masse du parcours")
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(List.of(games.t("race.mace-item-lore", "<gray>Prêtée jusqu'au prochain point de contrôle.")
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
        meta.addEnchant(Enchantment.WIND_BURST, 1, true);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean isOurs(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Coup de masse d'un coureur sur une cible de sa section : rétabli (sinon Wind Burst ne part pas). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || event.getEntity() instanceof Player
                || !isOurs(player.getInventory().getItemInMainHand())) {
            return;
        }
        GameInstance game = games.instances().of(player);
        if (game instanceof ParkourInstance parkour && parkour.maceHit(player, event.getEntity())) {
            event.setCancelled(false);
        }
    }

    /** Souffle de Wind Burst : seul celui qui vient de frapper est projeté, pas les coureurs autour. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        if (event.getCause() != EntityKnockbackEvent.KnockbackCause.EXPLOSION || !(event.getEntity() instanceof Player player)) {
            return;
        }
        GameInstance game = games.instances().of(player);
        if (game instanceof ParkourInstance parkour && parkour.shieldedFromBlast(player)) {
            event.setCancelled(true);
        }
    }

    /** Arrivée par bouton : clic droit sur le bouton posé au point « Arrivée ». */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onButton(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null
                || !Tag.BUTTONS.isTagged(block.getType())) {
            return;
        }
        GameInstance game = games.instances().of(event.getPlayer());
        if (game instanceof ParkourInstance parkour) {
            parkour.pressButton(event.getPlayer(), block);
        }
    }
}
