package fr.kalium.crafts;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.23.0 (LeKiwi06, 28/10/2026 : « j'aimerais qu'on puisse décrafter les bateaux et minecarts spéciaux (jukebox et
 * renforcé) » ; choix : les deux rendus, wagon jukebox compris). Un véhicule spécial seul dans la grille donne son bloc
 * spécial, et le véhicule simple reste dans la grille (comme le seau vide d'un gâteau) :
 * - bateau à jukebox (KS_JukeboxBoat) : jukebox + le bateau ; bateau à coffre et jukebox : jukebox + le bateau à coffre ;
 * - bateau, radeau ou minecart avec coffre renforcé (KS_CoffreRenforce) : coffre renforcé + bateau, radeau ou minecart ;
 * - wagon jukebox (KS_Trains) : jukebox + minecart.
 * Dans un crafter, le véhicule simple reste aussi dans sa case (comme les restes d'un craft vanilla).
 *
 * Une seule recette (n'importe quel bateau ou minecart seul), cachée du livre ; le résultat est choisi ici, et refusé
 * pour un véhicule ordinaire. KS_CoffreRenforce et KS_JukeboxBoat refusent leurs véhicules dans les autres recettes.
 */
final class Decrafts implements Listener {

    static final String ID = "decraft_vehicule";

    private final KSCrafts plugin;
    private final NamespacedKey cle;

    /** Bloc spécial rendu (résultat) et véhicule simple qui reste dans la grille. */
    private record Decraft(ItemStack bloc, ItemStack vehicule) {
    }

    Decrafts(KSCrafts plugin) {
        this.plugin = plugin;
        this.cle = new NamespacedKey(plugin, ID);
    }

    /** La recette : un bateau, un radeau ou un minecart (avec ou sans coffre), seul. */
    ShapelessRecipe recette() {
        List<Material> vehicules = new ArrayList<>(Tag.ITEMS_BOATS.getValues());
        for (Material bateau : Tag.ITEMS_CHEST_BOATS.getValues()) {
            if (!vehicules.contains(bateau)) {
                vehicules.add(bateau);
            }
        }
        vehicules.add(Material.MINECART);
        vehicules.add(Material.CHEST_MINECART);
        ShapelessRecipe recette = new ShapelessRecipe(cle, new ItemStack(Material.JUKEBOX));
        recette.addIngredient(new RecipeChoice.MaterialChoice(vehicules));
        return recette;
    }

    private boolean estDecraft(Recipe recette) {
        return recette instanceof Keyed k && k.getKey().equals(cle);
    }

    /** Ce que rend un véhicule spécial, ou null pour un autre objet. */
    private Decraft decraft(ItemStack objet) {
        if (objet == null || objet.isEmpty()) {
            return null;
        }
        Material type = objet.getType();
        if (plugin.actif("KS_JukeboxBoat") && fr.kalium.jukeboxboat.KSJukeboxBoat.estBateau(objet)) {
            return new Decraft(new ItemStack(Material.JUKEBOX), new ItemStack(type));
        }
        if (plugin.actif("KS_Trains") && fr.kalium.trains.KSTrains.estJukebox(objet)) {
            return new Decraft(new ItemStack(Material.JUKEBOX), new ItemStack(Material.MINECART));
        }
        if (plugin.actif("KS_CoffreRenforce") && type != Material.CHEST
                && fr.kalium.coffrerenforce.KSCoffreRenforce.estObjetRenforce(objet)) {
            Material simple = type == Material.CHEST_MINECART ? Material.MINECART
                    : Material.matchMaterial(type.name().replace("_CHEST_BOAT", "_BOAT").replace("_CHEST_RAFT", "_RAFT"));
            return simple == null ? null
                    : new Decraft(fr.kalium.coffrerenforce.KSCoffreRenforce.creerCoffre(), new ItemStack(simple));
        }
        return null;
    }

    /** Case du seul objet de la grille, ou -1 (grille vide ou plusieurs objets). */
    private static int seul(ItemStack[] grille) {
        int place = -1;
        for (int i = 0; i < grille.length; i++) {
            if (grille[i] != null && !grille[i].isEmpty()) {
                if (place >= 0) {
                    return -1;
                }
                place = i;
            }
        }
        return place;
    }

    /** Après KS_JukeboxBoat et KS_CoffreRenforce, qui refusent leurs véhicules dans les recettes. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreparer(PrepareItemCraftEvent event) {
        if (!estDecraft(event.getRecipe())) {
            return;
        }
        ItemStack[] grille = event.getInventory().getMatrix();
        int place = seul(grille);
        Decraft d = place < 0 ? null : decraft(grille[place]);
        event.getInventory().setResult(d == null ? null : d.bloc());
    }

    /** Prise du résultat : refusée pour un véhicule ordinaire ; sinon le véhicule simple revient dans la grille. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CraftItemEvent event) {
        if (!estDecraft(event.getRecipe())) {
            return;
        }
        ItemStack[] grille = event.getInventory().getMatrix();
        int place = seul(grille);
        Decraft d = place < 0 ? null : decraft(grille[place]);
        if (d == null || !(event.getWhoClicked() instanceof Player joueur)) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
            return;
        }
        InventoryView vue = event.getView();
        ItemStack special = grille[place].clone();
        // Le jeu retire l'objet de la grille après l'événement : le véhicule simple y est posé au tick suivant.
        Bukkit.getScheduler().runTask(plugin, () -> rendre(joueur, vue, place, special, d.vehicule()));
    }

    private static void rendre(Player joueur, InventoryView vue, int place, ItemStack special, ItemStack vehicule) {
        if (joueur.isOnline() && joueur.getOpenInventory() == vue
                && vue.getTopInventory() instanceof CraftingInventory grille) {
            ItemStack[] matrice = grille.getMatrix();
            if (place < matrice.length && special.isSimilar(matrice[place])) {
                // Craft pas fait (Maj + clic, inventaire plein) : rien à rendre.
                return;
            }
            if (place < matrice.length && (matrice[place] == null || matrice[place].isEmpty())) {
                matrice[place] = vehicule;
                grille.setMatrix(matrice);
                return;
            }
        }
        if (joueur.isOnline()) {
            for (ItemStack reste : joueur.getInventory().addItem(vehicule).values()) {
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
            }
        } else {
            joueur.getWorld().dropItemNaturally(joueur.getLocation(), vehicule);
        }
    }

    /** Crafter : même résultat ; le véhicule simple reste dans sa case (sinon il tombe au-dessus du crafter). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterBloc(CrafterCraftEvent event) {
        if (!estDecraft(event.getRecipe())
                || !(event.getBlock().getState(false) instanceof org.bukkit.block.Crafter crafter)) {
            return;
        }
        Inventory cases = crafter.getInventory();
        int place = seul(cases.getContents());
        Decraft d = place < 0 ? null : decraft(cases.getItem(place));
        if (d == null) {
            event.setCancelled(true);
            return;
        }
        event.setResult(d.bloc());
        org.bukkit.block.Block bloc = event.getBlock();
        ItemStack special = cases.getItem(place).clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (bloc.getState(false) instanceof org.bukkit.block.Crafter apres) {
                ItemStack dedans = apres.getInventory().getItem(place);
                if (special.isSimilar(dedans)) {
                    return;
                }
                if (dedans == null || dedans.isEmpty()) {
                    apres.getInventory().setItem(place, d.vehicule());
                    return;
                }
            }
            bloc.getWorld().dropItemNaturally(bloc.getLocation().add(0.5, 1.1, 0.5), d.vehicule());
        });
    }
}
