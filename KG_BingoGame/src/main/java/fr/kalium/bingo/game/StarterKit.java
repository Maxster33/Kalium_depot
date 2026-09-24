package fr.kalium.bingo.game;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

/**
 * Etat du joueur au DEBUT d'une partie (voir PartyStarter) - AJOUTE en 0.1.19, demandes explicites
 * de l'utilisateur : "au début de la partie la vie et la nourriture des joueurs doit être remontée
 * à fond" et "il faut ajouter un kit de départ aux joueurs quand la partie commence : 8 steak
 * cuits, pioche en pierre, hache en pierre, houe en pierre, pelle en pierre, épée en pierre".
 *
 * L'inventaire est vide juste avant de donner le kit, pour que chacun parte avec exactement la
 * meme chose (sinon un joueur pourrait garder des objets d'une ancienne partie). Emplacements
 * fixes dans la barre rapide ; l'emplacement 8 reste libre pour le papier "Objectifs" (GameItems).
 */
public final class StarterKit {

    private StarterKit() {
    }

    public static void prepare(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[]{null, null, null, null});
        inventory.setItemInOffHand(null);
        player.setItemOnCursor(null);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        player.setFireTicks(0);
        // Regeneration de la salle d'attente (voir LobbyRegenTask) : retiree des maintenant plutot
        // qu'a la prochaine verification de la tache.
        player.removePotionEffect(PotionEffectType.REGENERATION);

        inventory.setItem(0, new ItemStack(Material.STONE_SWORD));
        inventory.setItem(1, new ItemStack(Material.STONE_PICKAXE));
        inventory.setItem(2, new ItemStack(Material.STONE_AXE));
        inventory.setItem(3, new ItemStack(Material.STONE_SHOVEL));
        inventory.setItem(4, new ItemStack(Material.STONE_HOE));
        inventory.setItem(5, new ItemStack(Material.COOKED_BEEF, 8));
    }
}
