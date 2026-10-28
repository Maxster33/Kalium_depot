package fr.kalium.trains;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 1.2.0 (étape 3 du cahier, règles 8 et 9) : wagons à entonnoir et réglages des wagons à coffre d'un train.
 *
 * Un wagon à entonnoir prend dans les 3 wagons derrière lui (le plus proche d'abord : coffres dont la sortie est
 * autorisée, autres entonnoirs) et donne aux 3 wagons devant lui (fours : carburant seulement, dans leur case ; coffres
 * dont l'entrée est autorisée ; autres entonnoirs). Devant = côté locomotive (rang plus petit), même en marche arrière.
 * Rythme d'un entonnoir du jeu : 1 objet tous les 8 ticks dans chaque sens. Seau de lave donné à un four dont la
 * réserve est vide : la réserve prend 1 000 blocs et le seau vide reste dans l'entonnoir (1.6.0 : alcool neutre de
 * KS_Alcools, 500 blocs, la fiole vide reste dans l'entonnoir). Dans un train, l'entonnoir
 * n'aspire plus les objets au sol ni les contenants au-dessus des rails.
 *
 * Réglages d'un wagon à coffre (entrée, sortie ; autorisées par défaut) : respectés par les entonnoirs du train
 * seulement (un entonnoir posé au sol fait comme d'habitude).
 */
final class Entonnoirs implements Listener {

    /** Rythme des transferts (ticks), comme un entonnoir du jeu. */
    static final int RYTHME = 8;
    private static final int PORTEE = 3;

    private final KSTrains plugin;
    private final Foyers foyers;
    private final NamespacedKey cleEntree;
    private final NamespacedKey cleSortie;

    Entonnoirs(KSTrains plugin, Foyers foyers) {
        this.plugin = plugin;
        this.foyers = foyers;
        this.cleEntree = new NamespacedKey(plugin, "entree");
        this.cleSortie = new NamespacedKey(plugin, "sortie");
    }

    // ------------------------------------------------------------------ réglages des coffres

    boolean entree(StorageMinecart coffre) {
        return autorise(coffre, cleEntree);
    }

    boolean sortie(StorageMinecart coffre) {
        return autorise(coffre, cleSortie);
    }

    private static boolean autorise(Entity coffre, NamespacedKey cle) {
        Byte valeur = coffre.getPersistentDataContainer().get(cle, PersistentDataType.BYTE);
        return valeur == null || valeur != 0;
    }

    void basculerEntree(StorageMinecart coffre) {
        basculer(coffre, cleEntree);
    }

    void basculerSortie(StorageMinecart coffre) {
        basculer(coffre, cleSortie);
    }

    private static void basculer(Entity coffre, NamespacedKey cle) {
        PersistentDataContainer donnees = coffre.getPersistentDataContainer();
        donnees.set(cle, PersistentDataType.BYTE, (byte) (autorise(coffre, cle) ? 0 : 1));
    }

    // ------------------------------------------------------------------ transferts

    /** Tous les {@link #RYTHME} ticks, pour un train complet : chaque wagon à entonnoir prend puis donne un objet. */
    void transferer(Train train) {
        for (int i = 0; i < train.wagons.size(); i++) {
            if (train.wagons.get(i) instanceof HopperMinecart entonnoir) {
                prendre(train, i, entonnoir.getInventory());
                donner(train, i, entonnoir.getInventory());
            }
        }
    }

    private void prendre(Train train, int rang, Inventory entonnoir) {
        for (int j = rang + 1; j <= Math.min(train.wagons.size() - 1, rang + PORTEE); j++) {
            Minecart wagon = train.wagons.get(j);
            // 1.5.0 : minecart avec coffre renforcé : son stock passe par KS_CoffreRenforce.
            if (wagon instanceof StorageMinecart coffre && CoffresRenforces.est(coffre)) {
                if (sortie(coffre) && CoffresRenforces.prendreUn(coffre, entonnoir)) {
                    return;
                }
                continue;
            }
            Inventory source = wagon instanceof StorageMinecart coffre && sortie(coffre) ? coffre.getInventory()
                    : wagon instanceof HopperMinecart autre ? autre.getInventory() : null;
            if (source != null && deplacerUn(source, entonnoir)) {
                return;
            }
        }
    }

    /**
     * 1.3.0 (LeKiwi06 : « le minecart hopper ne transmet pas bien le charbon dans les 3 minecarts de la locomotive ») :
     * le carburant va d'abord aux fours, le moins rempli d'abord (avant : le plus proche, jusqu'à ce que sa case soit
     * pleine) ; la locomotive compte comme un ensemble : si l'entonnoir atteint un de ses fours, il les alimente tous.
     * Puis un objet va au coffre ou à l'entonnoir le plus proche devant lui.
     */
    private void donner(Train train, int rang, Inventory entonnoir) {
        List<PoweredMinecart> fours = new ArrayList<>();
        for (int j = rang - 1; j >= Math.max(0, rang - PORTEE); j--) {
            if (train.wagons.get(j) instanceof PoweredMinecart four && !fours.contains(four)) {
                fours.add(four);
                if (plugin.estLoco(four)) {
                    // Toute la locomotive : les fours de locomotive attelés de part et d'autre.
                    for (int k = j - 1; k >= 0 && plugin.estLoco(train.wagons.get(k)); k--) {
                        if (!fours.contains(train.wagons.get(k))) {
                            fours.add((PoweredMinecart) train.wagons.get(k));
                        }
                    }
                    for (int k = j + 1; k < rang && plugin.estLoco(train.wagons.get(k)); k++) {
                        if (!fours.contains(train.wagons.get(k))) {
                            fours.add((PoweredMinecart) train.wagons.get(k));
                        }
                    }
                }
            }
        }
        fours.sort(Comparator.comparingDouble(foyers::charge));
        for (PoweredMinecart four : fours) {
            if (alimenter(four, entonnoir)) {
                return;
            }
        }
        for (int j = rang - 1; j >= Math.max(0, rang - PORTEE); j--) {
            Minecart wagon = train.wagons.get(j);
            if (wagon instanceof StorageMinecart coffre && CoffresRenforces.est(coffre)) {
                if (entree(coffre) && CoffresRenforces.donnerUn(entonnoir, coffre)) {
                    return;
                }
                continue;
            }
            Inventory cible = wagon instanceof StorageMinecart coffre && entree(coffre) ? coffre.getInventory()
                    : wagon instanceof HopperMinecart autre ? autre.getInventory() : null;
            if (cible != null && deplacerUn(entonnoir, cible)) {
                return;
            }
        }
    }

    /** Déplace un objet (le premier qui trouve sa place) d'un inventaire à l'autre. */
    private static boolean deplacerUn(Inventory source, Inventory cible) {
        for (int place = 0; place < source.getSize(); place++) {
            ItemStack objet = source.getItem(place);
            if (objet == null || objet.isEmpty()) {
                continue;
            }
            ItemStack un = objet.clone();
            un.setAmount(1);
            if (cible.addItem(un).isEmpty()) {
                objet.setAmount(objet.getAmount() - 1);
                source.setItem(place, objet.isEmpty() ? null : objet);
                return true;
            }
        }
        return false;
    }

    /** Donne à un four le premier carburant de l'entonnoir qu'il accepte (seau de lave, alcool neutre : le récipient vide reste). */
    private boolean alimenter(PoweredMinecart four, Inventory entonnoir) {
        for (int place = 0; place < entonnoir.getSize(); place++) {
            ItemStack objet = entonnoir.getItem(place);
            if (!Foyers.carburant(objet)) {
                continue;
            }
            if (Foyers.liquide(objet)) {
                // Un seau de lave ou une potion ne s'empile pas : le récipient vide prend sa place.
                if (foyers.verser(four, objet)) {
                    entonnoir.setItem(place, Foyers.recipient(objet));
                    return true;
                }
            } else if (foyers.ajouter(four, objet)) {
                objet.setAmount(objet.getAmount() - 1);
                entonnoir.setItem(place, objet.isEmpty() ? null : objet);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ entonnoir d'un train : pas d'aspiration

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRamasser(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder(false) instanceof HopperMinecart entonnoir
                && plugin.trainDe(entonnoir) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAspirer(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder(false) instanceof HopperMinecart entonnoir
                && event.getInitiator().equals(event.getDestination()) && plugin.trainDe(entonnoir) != null) {
            event.setCancelled(true);
        }
    }
}
