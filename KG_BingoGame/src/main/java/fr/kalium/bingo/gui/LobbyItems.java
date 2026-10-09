package fr.kalium.bingo.gui;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Objet verrouille remis a chaque joueur en arrivant dans la salle d'attente : une Nether Star
 * (meme materiau/convention que le "menu de la partie" de KalGames, ItemService.gameMenuItem)
 * dont le clic droit ouvre PartyMenu (constitution des equipes, lancer/annuler la partie -
 * demande explicite de l'utilisateur : "il faudrait me remettre la nether star bloquee pour
 * accéder au menu de configuration de la partie").
 *
 * Le MEME objet (materiau/mecanique de verrouillage) est aussi utilise en salle d'attente POST-
 * partie (voir GameEndService/PostGameMenu), avec un nom/lore DIFFERENT (voir givePostGame) : la
 * demande explicite de l'utilisateur pour la fin de partie est "une netherstar pour qu'ils
 * puissent retourner au hub kalgames (via un menu)", un usage distinct du menu de configuration
 * avant-partie ci-dessus.
 */
public final class LobbyItems {

    private static final int SLOT = 4;

    private final JavaPlugin plugin;
    private final NamespacedKey key;

    public LobbyItems(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lobby-item");
    }

    private ItemStack menuItem(String displayName, java.util.List<String> lore) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "party-menu");
        item.setItemMeta(meta);
        return item;
    }

    /** Vide la hotbar concernee puis donne l'objet (evite les doublons si deja present) - salle
     *  d'attente AVANT le lancement de la partie (choix des equipes, lancer/annuler). */
    public void give(Player player) {
        give(player, "§6§lMenu de la partie",
                java.util.List.of("§7Clic droit : équipes, lancer", "§7ou annuler la partie."));
    }

    /** Meme objet, salle d'attente APRES la fin de la partie (victoire ou temps ecoule, voir
     *  GameEndService) - lore distinct : ouvre desormais fr.kalium.bingo.gui.PostGameMenu (retour a
     *  kal-games), pas PartyMenu (qui echouerait, la BingoParty est deja consommee). */
    public void givePostGame(Player player) {
        give(player, "§6§lPartie terminée", java.util.List.of("§7Clic droit : retourner à kal-games."));
    }

    private void give(Player player, String displayName, java.util.List<String> lore) {
        if (isOurs(player.getInventory().getItem(SLOT))) {
            return;
        }
        player.getInventory().setItem(SLOT, menuItem(displayName, lore));
    }

    /**
     * Retire l'objet de menu de la salle d'attente s'il est present (n'importe quelle case, pas
     * seulement SLOT : au cas ou le joueur l'aurait deplace avant que le drop n'en soit empeche).
     * A appeler quand le joueur quitte la salle d'attente pour de bon (partie lancee) - demande
     * explicite de l'utilisateur : l'objet restait dans la hotbar sur la carte de jeu, inutilisable
     * ("il n'y a pas de partie en attente"), ce qui pretait a confusion.
     */
    public void remove(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isLocked(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
    }

    // ------------------------------------------------------------------ 0.11.0 : objet « Rejouer »

    private static final int REPLAY_SLOT = 6;

    /**
     * 0.11.0 : objet « Rejouer » de la salle d'attente d'apres-partie (voir fr.kalium.bingo.game.ReplayService).
     * hostName = pseudo de celui qui a deja relance la partie (l'objet devient « Rejoindre la partie de X »), ou null.
     * Remplace l'objet « Rejouer » deja present ; sinon pose en case 6, ou dans la premiere case libre de la barre.
     */
    public void giveReplay(Player player, String hostName) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        if (hostName == null) {
            meta.setDisplayName("§a§lRejouer");
            meta.setLore(java.util.List.of("§7Clic droit : relancer une partie", "§7avec les mêmes réglages."));
        } else {
            meta.setDisplayName("§a§lRejoindre la partie de " + hostName);
            meta.setLore(java.util.List.of("§7Clic droit : entrer dans la partie", "§7qui vient d'être relancée."));
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "replay");
        item.setItemMeta(meta);
        var inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (isReplay(inventory.getItem(slot))) {
                inventory.setItem(slot, item);
                return;
            }
        }
        int target = -1;
        for (int slot = REPLAY_SLOT; slot < REPLAY_SLOT + 9 && target < 0; slot++) {
            ItemStack current = inventory.getItem(slot % 9);
            if (current == null || current.getType().isAir()) {
                target = slot % 9;
            }
        }
        if (target >= 0) {
            inventory.setItem(target, item);
        }
    }

    public boolean hasReplay(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            if (isReplay(player.getInventory().getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    public void removeReplay(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isReplay(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
    }

    public boolean isReplay(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return "replay".equals(item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    /** Objet verrouille de la salle d'attente (menu de la partie ou « Rejouer ») : ni deplace, ni lache. */
    public boolean isLocked(ItemStack item) {
        return isOurs(item) || isReplay(item);
    }

    public boolean isOurs(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return "party-menu".equals(item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }
}
