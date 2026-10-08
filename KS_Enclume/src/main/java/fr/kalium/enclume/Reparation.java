package fr.kalium.enclume;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Repairable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 1.3.0 : coût d'une réparation complète à l'enclume, en dernière ligne de la description de chaque objet réparable de
 * l'inventaire des joueurs (armures, outils, armes, élytres, boucliers, arcs, tridents...).
 *
 * Calcul (règles vanilla de l'enclume, avec le matériau de l'objet en 2e case) :
 * 1. pénalité de l'objet (elle augmente à chaque passage à l'enclume : 0, 1, 3, 7, 15...) ;
 * 2. + 1 niveau par unité de matériau : chaque unité répare un quart de la solidité maximale, jusqu'à la réparation
 *    complète ;
 * 3. réduction de KS_Enclume 1.2.0 au-delà de 50 niveaux.
 * Ex. : épée en diamant (1 561 de solidité, une unité répare 390), pénalité 7, abîmée de 800 : 3 diamants
 * (800 - 390 - 390 = 20, puis 0) ; 7 + 3 = 10 niveaux.
 *
 * Objet sans matériau de réparation (arc, trident, canne à pêche...) : seule la fusion avec un autre objet le répare ;
 * la ligne donne la pénalité, minimum de toute fusion. Objet intact : « objet intact ». Objet incassable : rien.
 *
 * Mise à jour chaque seconde (seulement si la ligne change), à la connexion et à la fermeture d'un inventaire. Rien
 * n'est changé pendant qu'un inventaire (enclume, coffre...) est ouvert : pas de conflit avec les clics. La ligne est
 * reconnue au texte gardé dans le marqueur ks_enclume:ligne_reparation (les autres lignes ne sont pas touchées).
 */
final class Reparation implements Listener {

    /** Unités de matériau au plus (une pile). */
    private static final int MAX_UNITES = 64;

    private final JavaPlugin plugin;
    private final NamespacedKey cle;

    Reparation(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cle = new NamespacedKey(plugin, "ligne_reparation");
    }

    /** Texte de la ligne pour cet objet, ou null s'il n'est pas réparable. */
    static String texte(ItemStack item) {
        if (item == null || item.isEmpty() || item.hasData(DataComponentTypes.UNBREAKABLE)) {
            return null;
        }
        Integer max = item.getData(DataComponentTypes.MAX_DAMAGE);
        if (max == null || max <= 0) {
            return null;
        }
        int usure = Objects.requireNonNullElse(item.getData(DataComponentTypes.DAMAGE), 0);
        int penalite = Objects.requireNonNullElse(item.getData(DataComponentTypes.REPAIR_COST), 0);
        Repairable repairable = item.getData(DataComponentTypes.REPAIRABLE);
        boolean materiau = repairable != null && !repairable.types().isEmpty() && max / 4 > 0;
        if (usure <= 0) {
            return "Réparation : objet intact";
        }
        if (!materiau) {
            return "Réparation : au moins " + niveaux(KSEnclume.reduire(penalite)) + " (par fusion)";
        }
        // Boucle de l'enclume vanilla : chaque unité répare min(usure restante, solidité max / 4), 1 niveau par unité.
        int unites = 0;
        int reste = usure;
        int part = Math.min(reste, max / 4);
        while (part > 0 && unites < MAX_UNITES) {
            reste -= part;
            unites++;
            part = Math.min(reste, max / 4);
        }
        return "Réparation : " + niveaux(KSEnclume.reduire(penalite + unites));
    }

    private static String niveaux(int n) {
        return n + (n > 1 ? " niveaux" : " niveau");
    }

    /** Copie de l'objet avec la ligne à jour, ou null si rien ne change. */
    ItemStack mettreAJour(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        String voulu = texte(item);
        String actuel = item.getPersistentDataContainer().get(cle, PersistentDataType.STRING);
        if (Objects.equals(voulu, actuel)) {
            return null;
        }
        ItemStack copie = item.clone();
        ItemMeta meta = copie.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        if (actuel != null) {
            for (int i = lore.size() - 1; i >= 0; i--) {
                if (actuel.equals(PlainTextComponentSerializer.plainText().serialize(lore.get(i)))) {
                    lore.remove(i);
                    break;
                }
            }
        }
        if (voulu != null) {
            lore.add(Component.text(voulu, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(cle, PersistentDataType.STRING, voulu);
        } else {
            meta.getPersistentDataContainer().remove(cle);
        }
        meta.lore(lore.isEmpty() ? null : lore);
        copie.setItemMeta(meta);
        return copie;
    }

    /** Inventaire du joueur (barre, armure, seconde main), sauf si un autre inventaire est ouvert. */
    void mettreAJour(Player joueur) {
        InventoryType ouvert = joueur.getOpenInventory().getTopInventory().getType();
        if (ouvert != InventoryType.CRAFTING && ouvert != InventoryType.CREATIVE) {
            return;
        }
        PlayerInventory inventaire = joueur.getInventory();
        for (int i = 0; i < inventaire.getSize(); i++) {
            ItemStack nouveau = mettreAJour(inventaire.getItem(i));
            if (nouveau != null) {
                inventaire.setItem(i, nouveau);
            }
        }
    }

    void toutMettreAJour() {
        plugin.getServer().getOnlinePlayers().forEach(this::mettreAJour);
    }

    private void plusTard(Player joueur) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (joueur.isOnline()) {
                mettreAJour(joueur);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plusTard(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player joueur) {
            plusTard(joueur);
        }
    }
}
