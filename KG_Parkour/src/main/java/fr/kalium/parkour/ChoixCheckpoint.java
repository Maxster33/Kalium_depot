package fr.kalium.parkour;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.menu.api.Contenant;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.0 (demande de LeKiwi06, 26/10/2026 : « ajoute un item en parkour pour se téléporter vers le checkpoint de son
 * choix en mode entrainement ») : objet « Choisir un point de contrôle » (plume), donné aux joueurs d'un parcours
 * d'entraînement. Clic droit : la liste du départ et de tous les points de contrôle du parcours, atteints ou non ; un
 * clic y téléporte le joueur et relance son chrono depuis ce point (voir ParkourInstance.teleportTraining).
 *
 * Coffre ou fenêtre selon le réglage d'affichage du joueur (KalGames.enCoffre), avec la même organisation. L'objet est
 * reconnu par une marque de ce plugin (pas celle de KalGames : KalGames n'a pas a etre modifie) ; KalGames annule deja
 * l'utilisation des objets en course (inventaire verrouille), cet ecouteur passe apres lui.
 */
final class ChoixCheckpoint implements Listener {

    /** Case de la barre d'objets : 0 = dernier point de controle (KalGames), 4 = menu de la partie, 8 = boussole. */
    static final int SLOT = 1;

    private final KalGames games;
    private final NamespacedKey key;

    ChoixCheckpoint(KGParkour plugin, KalGames games) {
        this.games = games;
        this.key = new NamespacedKey(plugin, "choix-checkpoint");
    }

    private Component t(String key, String def, Object... pairs) {
        return games.t(key, def, pairs);
    }

    void give(Player player) {
        ItemStack item = Contenant.objet(Material.FEATHER,
                t("race.training-choice-item-name", "<aqua><bold>Choisir un point de contrôle"),
                List.of(t("race.training-choice-item-lore0", "<gray>Clic droit : aller au point"),
                        t("race.training-choice-item-lore1", "<gray>de contrôle de votre choix.")));
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        player.getInventory().setItem(SLOT, item);
    }

    private boolean isOurs(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isOurs(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        GameInstance game = games.instances().of(player);
        if (game instanceof ParkourInstance parkour && parkour.canChooseCheckpoint(player)) {
            open(player, parkour, 0);
        } else {
            // Objet reste dans l'inventaire hors d'un entrainement : il ne sert plus.
            player.getInventory().setItem(event.getHand(), null);
        }
    }

    private void open(Player player, ParkourInstance game, int page) {
        if (games.enCoffre(player)) {
            openChest(player, game, page);
        } else {
            openWindow(player, game);
        }
    }

    // ------------------------------------------------------------------ coffre

    private void openChest(Player player, ParkourInstance game, int page) {
        int total = game.checkpointCount();
        int entries = total + 1; // le depart, puis chaque point de controle
        int pages = Math.max(1, (entries + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int current = Math.max(0, Math.min(page, pages - 1));
        int shown = Math.min(Contenant.PAR_PAGE, entries - current * Contenant.PAR_PAGE);
        int rows = Math.min(6, (shown + 8) / 9 + 1);
        Contenant menu = Contenant.pour(player, rows, t("race.training-choice-title", "Choisir un point de contrôle"),
                "kg-parkour-choix-checkpoint");
        int reached = game.reachedCheckpoints(player);
        for (int i = 0; i < shown; i++) {
            int entry = current * Contenant.PAR_PAGE + i;
            int checkpoint = entry - 1; // -1 = depart
            menu.poser(i, entryItem(checkpoint, total, reached), p -> {
                Contenant.fermer(p);
                go(p, checkpoint);
            });
        }
        menu.barre();
        if (current > 0) {
            menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, t("race.training-choice-previous", "<white>Page précédente"),
                    List.of()), p -> openChest(p, game, current - 1));
        }
        if (current < pages - 1) {
            menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, t("race.training-choice-next", "<white>Page suivante"),
                    List.of()), p -> openChest(p, game, current + 1));
        }
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("race.training-choice-close", "<red>Fermer"),
                List.of()), Contenant::fermer);
        menu.ouvrir(player);
    }

    /**
     * Objet d'une case : le depart (checkpoint = -1) ou un point de controle (nombre de l'objet = son numero, jusqu'a 64),
     * en surbrillance pour le dernier atteint.
     */
    private ItemStack entryItem(int checkpoint, int total, int reached) {
        List<Component> lore = new ArrayList<>();
        if (checkpoint < 0) {
            lore.add(t("race.training-choice-start-lore", "<gray>Clic : repartir du début."));
            if (reached == 0) {
                lore.add(t("race.training-choice-here", "<yellow>Votre dernier point atteint"));
            }
            ItemStack item = Contenant.objet(Material.LIME_CONCRETE, t("race.training-choice-start", "<green><bold>Départ"), lore);
            if (reached == 0) {
                glint(item);
            }
            return item;
        }
        ItemStack item;
        int number = checkpoint + 1;
        lore.add(t("race.training-choice-cp-lore", "<gray>Clic : y aller (chrono remis à zéro)."));
        if (number == reached) {
            lore.add(t("race.training-choice-here", "<yellow>Votre dernier point atteint"));
        } else if (number > reached) {
            lore.add(t("race.training-choice-not-reached", "<dark_gray>Pas encore atteint sur ce tour."));
        }
        item = new ItemStack(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Math.max(1, Math.min(64, number)));
        item = Contenant.objet(item, t("race.training-choice-cp", "<aqua>Point de contrôle <white><n>/<total>",
                "n", number, "total", total), lore);
        if (number == reached) {
            glint(item);
        }
        return item;
    }

    private static void glint(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
    }

    // ------------------------------------------------------------------ fenetre

    private void openWindow(Player player, ParkourInstance game) {
        int total = game.checkpointCount();
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(games.gui().button(t("race.training-choice-start", "<green><bold>Départ"),
                t("race.training-choice-start-lore", "<gray>Clic : repartir du début."), p -> go(p, -1)));
        for (int i = 0; i < total; i++) {
            int checkpoint = i;
            buttons.add(games.gui().button(t("race.training-choice-cp", "<aqua>Point de contrôle <white><n>/<total>",
                    "n", i + 1, "total", total), null, p -> go(p, checkpoint)));
        }
        games.gui().open(player, t("race.training-choice-title", "Choisir un point de contrôle"),
                List.of(t("race.training-choice-body", "<gray>Votre chrono repart de zéro depuis le point choisi.")),
                List.of(), buttons, null, total >= 8 ? 3 : 2);
    }

    // ------------------------------------------------------------------ teleportation

    /** Le joueur est-il toujours dans un parcours d'entrainement ? Sinon rien (menu reste ouvert apres une sortie...). */
    private void go(Player player, int checkpoint) {
        if (games.instances().of(player) instanceof ParkourInstance parkour && parkour.canChooseCheckpoint(player)) {
            parkour.teleportTraining(player, checkpoint);
        }
    }
}
