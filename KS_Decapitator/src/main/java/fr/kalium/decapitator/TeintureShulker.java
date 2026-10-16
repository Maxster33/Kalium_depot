package fr.kalium.decapitator;

import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 1.3.0 (LeKiwi06, 16/10/2026) : un clic droit avec un colorant sur un shulker le teint, comme sur Bedrock. Sur Java,
 * un shulker n'a de couleur que par commande (ou en se dupliquant depuis un shulker déjà coloré) : sans cela, les 16
 * têtes de shulker colorées ne pourraient pas être obtenues en survie. Une fois teint, il se duplique dans sa couleur
 * (règle du jeu).
 *
 * Un colorant consommé (pas en créatif) ; rien si le shulker a déjà cette couleur. Un clic annulé par un autre plugin
 * (protection de claim...) ne teint pas.
 */
final class TeintureShulker implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Shulker shulker)) {
            return;
        }
        Player joueur = event.getPlayer();
        // Le jeu envoie un clic par main : la main secondaire ne compte que si la main principale ne tient pas de colorant.
        if (event.getHand() == EquipmentSlot.OFF_HAND && couleur(joueur.getInventory().getItemInMainHand()) != null) {
            return;
        }
        ItemStack colorant = joueur.getInventory().getItem(event.getHand());
        DyeColor couleur = couleur(colorant);
        if (couleur == null) {
            return;
        }
        event.setCancelled(true);
        if (couleur == shulker.getColor()) {
            return;
        }
        shulker.setColor(couleur);
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            colorant.subtract();
        }
        shulker.getWorld().playSound(shulker, Sound.ITEM_DYE_USE, SoundCategory.PLAYERS, 1f, 1f);
        joueur.swingHand(event.getHand());
    }

    /** Couleur d'un colorant (RED_DYE -> RED), null si l'objet n'est pas un colorant. */
    private static DyeColor couleur(ItemStack objet) {
        if (objet == null || !objet.getType().name().endsWith("_DYE")) {
            return null;
        }
        Material type = objet.getType();
        try {
            return DyeColor.valueOf(type.name().substring(0, type.name().length() - "_DYE".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
