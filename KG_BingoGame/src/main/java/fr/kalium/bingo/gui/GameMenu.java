package fr.kalium.bingo.gui;

import fr.kalium.bingo.game.BingoGame;
import fr.kalium.bingo.game.BingoInstance;
import fr.kalium.bingo.game.GameManager;
import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.grid.Difficulty;
import fr.kalium.bingo.grid.GridCell;
import fr.kalium.bingo.grid.Objective;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Menu EN PARTIE (ouvert via l'objet papier, voir GameItems / GameItemListener) : la grille de
 * bingo affichee comme un INVENTAIRE - demande explicite de l'utilisateur, 24/09/2026 : "il serait
 * préférable de visualiser la liste dans une page type inventaire" (remplace l'ancien Dialog texte
 * utilise jusqu'a la 0.1.13).
 *
 * Grille 5x5 centree dans les 5 premieres rangees (colonnes 3 a 7, voir GRID_COL_OFFSET), chaque
 * case = un objet representant l'objectif (materiau, quantite en taille de pile). Le SURVOL
 * (tooltip natif Minecraft, aucun code supplementaire necessaire) affiche le statut de CHAQUE
 * equipe pour cette case - demande explicite de l'utilisateur : "équipe A : complété (vert) / non
 * complété (rouge), équipe B : ...". Une case deja validee par l'equipe DU JOUEUR QUI REGARDE
 * brille (enchantement factice masque, meme technique que les livres enchantes - voir
 * glow()) : "il doit être brillant ... comme pour les livres enchantés".
 *
 * Menu en LECTURE SEULE (tout clic est annule, voir GameMenuListener) hormis les deux objets
 * verrouilles de la derniere rangee : "Fermer" (CLOSE_SLOT, bas a droite) et "Abandonner la
 * partie" (ABANDON_SLOT, juste au-dessus - ouvre AbandonConfirmMenu).
 */
public final class GameMenu {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    /** Decale la grille 5x5 pour la centrer dans les 9 colonnes de l'inventaire. */
    private static final int GRID_COL_OFFSET = 2;
    public static final int CLOSE_SLOT = SIZE - 1;        // derniere rangee, derniere colonne
    public static final int ABANDON_SLOT = SIZE - 1 - 9;  // juste au-dessus de CLOSE_SLOT

    private final GameManager gameManager;

    public GameMenu(JavaPlugin plugin, GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        Optional<BingoGame> gameOpt = gameManager.findGameOf(player.getUniqueId());
        if (gameOpt.isEmpty()) {
            player.sendMessage("§cAucune partie Bingo en cours pour vous.");
            return;
        }
        BingoGame game = gameOpt.get();
        BingoGrid grid = game.getGrid();
        Optional<BingoInstance> instanceOpt = game.findInstanceOf(player.getUniqueId());
        if (grid == null || instanceOpt.isEmpty()) {
            player.sendMessage("§cLa grille n'est pas encore prête.");
            return;
        }
        int myTeam = instanceOpt.get().getTeam().getTeamNumber();
        int size = grid.getSize();

        Inventory inv = Bukkit.createInventory(new GameMenuHolder(), SIZE, Component.text("Objectifs Bingo"));

        List<GridCell> cells = grid.getCells();
        for (int row = 0; row < size && row < ROWS - 1; row++) {
            for (int col = 0; col < size && col < 9; col++) {
                int index = row * size + col;
                int slot = row * 9 + (GRID_COL_OFFSET + col);
                if (slot < 0 || slot >= SIZE) {
                    continue;
                }
                inv.setItem(slot, buildCellItem(game, cells.get(index), index, myTeam));
            }
        }

        inv.setItem(CLOSE_SLOT, closeItem());
        inv.setItem(ABANDON_SLOT, abandonItem());

        player.openInventory(inv);
    }

    private ItemStack buildCellItem(BingoGame game, GridCell cell, int index, int myTeam) {
        Objective objective = cell.getObjective();
        ItemStack item = new ItemStack(objective.material(), Math.max(1, Math.min(64, objective.quantity())));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorFor(objective.difficulty()) + objective.quantity() + "x " + objective.material().name());

        List<String> lore = new ArrayList<>();
        lore.add("§8Difficulté : " + colorFor(objective.difficulty()) + objective.difficulty().name());
        if (objective.hasCondition()) {
            lore.add("§7" + objective.condition());
        }
        lore.add("");
        for (BingoInstance instance : game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            boolean done = game.isValidated(team, index);
            lore.add("§7Équipe " + team + " : " + (done ? "§aComplété" : "§cNon complété"));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        if (game.isValidated(myTeam, index)) {
            glow(item);
        }
        return item;
    }

    /** Fait briller l'objet comme un livre enchanté, sans afficher d'enchantement dans le tooltip
     *  (ItemFlag.HIDE_ENCHANTS deja pose ci-dessus) - demande explicite de l'utilisateur : "il doit
     *  être brillant ... comme pour les livres enchantés par exemple". */
    private void glow(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.LUCK_OF_THE_SEA, 1);
    }

    private String colorFor(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "§9";
            case MEDIUM -> "§e";
            case HARD -> "§6";
            case EXTREME -> "§c";
        };
    }

    private ItemStack closeItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lFermer");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack abandonItem() {
        ItemStack item = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§4§lAbandonner la partie");
        meta.setLore(List.of("§7Quitter définitivement cette partie.", "§7(action irréversible)"));
        item.setItemMeta(meta);
        return item;
    }

    /** Marqueur pour reconnaitre cet inventaire dans GameMenuListener - jamais interroge par ce
     *  plugin lui-meme, uniquement via instanceof (meme principe que d'autres marqueurs Bukkit). */
    public static final class GameMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
