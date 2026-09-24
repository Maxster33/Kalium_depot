package fr.kalium.bingo.gui;

import fr.kalium.bingo.game.TeamStyle;
import fr.kalium.bingo.score.ScoreEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resume de fin de partie (0.4.0) - demande explicite de LeKiwi06, 24/09/2026 : "on pourra voir les tetes dans une
 * interface de resume de fin de partie". Ouvert depuis le menu de la nether star de la salle d'attente post-partie
 * (PostGameMenu). Une rangee par equipe, dans l'ordre du classement : laine de la couleur de l'equipe (points), puis
 * la tete de chacun de ses joueurs (points solo, objectifs, bingos). Lecture seule.
 */
public final class SummaryMenu implements Listener {

    /** Resultat d'un joueur, fige en fin de partie. */
    public record PlayerLine(UUID id, String name, double solo, int objectives, int firsts, int bingos, List<Component> items) {
    }

    /** Resultat d'une equipe, fige en fin de partie (ordre du classement). */
    public record TeamLine(int team, int rank, double points, double own, boolean abandoned, List<PlayerLine> players) {
    }

    /** Resume complet d'une partie. */
    public record Summary(String title, String reason, List<TeamLine> teams) {
    }

    private static final int ROWS = 6;

    public void open(Player player, Summary summary) {
        Inventory inv = Bukkit.createInventory(new Holder(), ROWS * 9, Component.text("Résumé : " + summary.title()));
        int row = 0;
        for (TeamLine team : summary.teams()) {
            if (row >= ROWS - 1) {
                break;
            }
            inv.setItem(row * 9, teamItem(team));
            int col = 1;
            for (PlayerLine line : team.players()) {
                if (col > 8) {
                    break;
                }
                inv.setItem(row * 9 + col, headItem(team.team(), line));
                col++;
            }
            row++;
        }
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(plain(Component.text(summary.title(), NamedTextColor.GOLD)));
        meta.lore(List.of(plain(Component.text(summary.reason(), NamedTextColor.GRAY))));
        info.setItemMeta(meta);
        inv.setItem(ROWS * 9 - 5, info);
        player.openInventory(inv);
    }

    private ItemStack teamItem(TeamLine team) {
        Material wool = switch (TeamStyle.letter(team.team())) {
            case "A" -> Material.RED_WOOL;
            case "B" -> Material.BLUE_WOOL;
            case "C" -> Material.YELLOW_WOOL;
            default -> Material.LIME_WOOL;
        };
        ItemStack item = new ItemStack(wool);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(Component.text((team.rank() > 0 ? team.rank() + ". " : "") + "Équipe " + TeamStyle.letter(team.team()),
                TeamStyle.color(team.team()))));
        List<Component> lore = new ArrayList<>();
        double behind = team.points() - team.own();
        lore.add(plain(Component.text("Points : ", NamedTextColor.GRAY).append(Component.text(ScoreEngine.format(team.points())
                + (behind > 1e-9 ? " (" + ScoreEngine.format(team.own()) + " + " + ScoreEngine.format(behind) + ")" : ""), NamedTextColor.WHITE))));
        if (team.abandoned()) {
            lore.add(plain(Component.text("A abandonné", NamedTextColor.RED)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack headItem(int team, PlayerLine line) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(line.id()));
        meta.displayName(plain(Component.text(line.name(), TeamStyle.color(team))));
        List<Component> lore = new ArrayList<>();
        lore.add(plain(Component.text("Points solo : ", NamedTextColor.GRAY).append(Component.text(ScoreEngine.format(line.solo()), NamedTextColor.WHITE))));
        lore.add(plain(Component.text("Objectifs validés : ", NamedTextColor.GRAY).append(Component.text(line.objectives()
                + (line.firsts() > 0 ? " (dont " + line.firsts() + " en 1er)" : ""), NamedTextColor.WHITE))));
        lore.add(plain(Component.text("Bingos : ", NamedTextColor.GRAY).append(Component.text(String.valueOf(line.bingos()), NamedTextColor.WHITE))));
        for (Component entry : line.items()) {
            lore.add(plain(Component.text(" • ", NamedTextColor.DARK_GRAY).append(entry)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    /** Marqueur de cet inventaire. */
    public static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
