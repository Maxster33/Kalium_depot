package fr.kalium.alcools;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 1.2.0 (LeKiwi06, 30/10/2026) : l'alambic ne brasse qu'une fiole à la fois, dans la case du milieu (potions du jeu
 * et alcools : trois par trois, c'était trop fort).
 *
 * - Cases de gauche et de droite : une barrière « Case fermée » y est affichée quand elles sont vides
 *   ({@link CasesFermees}, affichage seul) ; on ne peut rien y poser (clic, glisser, touches 1 à 9, main secondaire) ; on peut
 *   encore en retirer ce qui s'y trouvait.
 * - Clic avec Maj depuis l'inventaire : le jeu remplit d'abord la case de gauche ; au tick suivant, la fiole passe au
 *   milieu s'il est libre, sinon elle revient au joueur.
 * - Entonnoir (ou distributeur) sur le côté : une fiole n'entre que si le milieu est libre, et toujours au milieu.
 * - Filet de sécurité : au début d'un brassage, une fiole restée sur le côté passe au milieu s'il est libre, sinon
 *   elle tombe au-dessus de l'alambic ; au brassage, les côtés ne changent pas.
 *
 * Le carburant reste celui du jeu (vérifié dans le code de Paper 26.3) : aucun brassage, potion ou alcool, sans
 * poudre de blaze (une poudre = 20 brassages, donc 20 fioles).
 */
final class Alambic implements Listener {

    private static final int MILIEU = 1;
    private static final int[] COTES = {0, 2};

    private final KSAlcools plugin;

    Alambic(KSAlcools plugin) {
        this.plugin = plugin;
    }

    private static boolean cote(int caseAlambic) {
        return caseAlambic == 0 || caseAlambic == 2;
    }

    private static boolean vide(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    /** Fioles qu'un entonnoir sur le côté pose dans les cases du bas (ni ingrédient ni carburant). */
    private static boolean fiole(ItemStack item) {
        return item != null && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION
                || item.getType() == Material.LINGERING_POTION);
    }

    private void refuser(HumanEntity joueur) {
        joueur.sendActionBar(plugin.t("alambic.une_fiole",
                "<red>Une seule fiole à la fois, dans la case du milieu."));
    }

    // ------------------------------------------------------------------ joueurs

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClic(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory() instanceof BrewerInventory alambic)
                || event.getClickedInventory() == null) {
            return;
        }
        HumanEntity joueur = event.getWhoClicked();
        if (event.getClickedInventory() == alambic) {
            if (!cote(event.getSlot())) {
                return;
            }
            boolean pose;
            if (event.getClick() == ClickType.NUMBER_KEY) {
                pose = !vide(joueur.getInventory().getItem(event.getHotbarButton()));
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                pose = !vide(joueur.getInventory().getItemInOffHand());
            } else {
                pose = switch (event.getAction()) {
                    case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> true;
                    default -> false;
                };
            }
            if (pose) {
                event.setCancelled(true);
                refuser(joueur);
            } else if (vide(event.getCurrentItem())) {
                // Clic sur la barrière affichée (CasesFermees) : refusé, le serveur renvoie l'écran et elle revient.
                event.setCancelled(true);
            }
        } else if (event.isShiftClick()) {
            // Le jeu choisit lui-même les fioles acceptées : on laisse faire, puis on range au tick suivant.
            Bukkit.getScheduler().runTask(plugin, () -> ranger(alambic, joueur));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlisser(InventoryDragEvent event) {
        if (event.getView().getTopInventory() instanceof BrewerInventory
                && event.getRawSlots().stream().anyMatch(Alambic::cote)) {
            event.setCancelled(true);
            refuser(event.getWhoClicked());
        }
    }

    // ------------------------------------------------------------------ entonnoirs

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntonnoir(InventoryMoveItemEvent event) {
        if (!(event.getDestination() instanceof BrewerInventory alambic) || !fiole(event.getItem())) {
            return;
        }
        // Le jeu prendrait la première case libre du bas : on la choisit à sa place.
        event.setCancelled(true);
        if (!vide(alambic.getItem(MILIEU))) {
            return;
        }
        Inventory source = event.getSource();
        ItemStack fiole = event.getItem().clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (vide(alambic.getItem(MILIEU)) && source.containsAtLeast(fiole, 1)
                    && source.removeItem(fiole.clone()).isEmpty()) {
                alambic.setItem(MILIEU, fiole);
            }
        });
    }

    // ------------------------------------------------------------------ filet de sécurité

    @EventHandler(priority = EventPriority.HIGH)
    public void onDebut(BrewingStartEvent event) {
        if (event.getBlock().getState(false) instanceof BrewingStand stand) {
            ranger(stand.getInventory(), null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrassage(BrewEvent event) {
        List<ItemStack> resultats = event.getResults();
        for (int c : COTES) {
            ItemStack avant = event.getContents().getItem(c);
            if (!vide(avant) && c < resultats.size()) {
                resultats.set(c, avant.clone());
            }
        }
    }

    /**
     * Fioles des côtés : au milieu s'il est libre, sinon rendues au joueur (inventaire plein : à ses pieds) ou, sans
     * joueur, posées au-dessus de l'alambic.
     */
    private static void ranger(BrewerInventory alambic, HumanEntity joueur) {
        for (int c : COTES) {
            ItemStack item = alambic.getItem(c);
            if (vide(item)) {
                continue;
            }
            alambic.setItem(c, null);
            if (vide(alambic.getItem(MILIEU))) {
                alambic.setItem(MILIEU, item);
            } else if (joueur != null) {
                joueur.getInventory().addItem(item).values()
                        .forEach(reste -> joueur.getWorld().dropItem(joueur.getLocation(), reste));
            } else {
                Block bloc = alambic.getHolder() != null ? alambic.getHolder().getBlock() : null;
                if (bloc != null) {
                    Location lieu = bloc.getLocation().add(0.5, 1, 0.5);
                    bloc.getWorld().dropItem(lieu, item);
                }
            }
        }
    }
}
