package fr.kalium.marteau;

import fr.kalium.marteau.KSMarteau.Niveau;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;

/**
 * Enclume et table de forge (cahier § 3).
 *
 * Enclume, marteau à gauche : KS_Marteau calcule le résultat lui-même, à la manière du jeu (AnvilMenu.createResult),
 * parce que le jeu traite le marteau comme une masse (il accepterait Densité, Brèche, Charge de vent, Châtiment et
 * refuserait Efficacité, Toucher de soie et Magnétisme d'un livre) :
 * - à droite : un livre enchanté ou un marteau du même niveau, rien d'autre (pas de réparation par matériau) ;
 * - seuls les enchantements permis passent ; un enchantement incompatible coûte 1 et n'est pas ajouté ; deux niveaux
 *   égaux donnent le niveau suivant (au plus le maximum) ;
 * - coût : celui du jeu (coût d'enclume × niveau, divisé par 2 pour un livre ; 2 pour une réparation par un autre
 *   marteau ; 1 pour un renommage ; plus les pénalités de travaux précédents), pénalité du résultat doublée + 1.
 * Masse ordinaire à gauche : ni Magnétisme, ni marteau à droite (un marteau ne sert pas à réparer une masse).
 *
 * Forge : seul un marteau en diamant (usé et enchanté compris) devient un marteau en netherite.
 */
final class Enclume implements Listener {

    private final NamespacedKey recetteNetherite;

    Enclume(NamespacedKey recetteNetherite) {
        this.recetteNetherite = recetteNetherite;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnclume(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack gauche = inv.getFirstItem();
        ItemStack droite = inv.getSecondItem();
        Niveau niveau = KSMarteau.niveau(gauche);
        if (niveau == null) {
            proteger(event, gauche, droite);
            return;
        }
        if (droite == null || droite.getType().isAir()) {
            return; // renommage seul : le résultat du jeu garde tous les composants du marteau
        }
        boolean livre = droite.getType() == Material.ENCHANTED_BOOK;
        Niveau autre = KSMarteau.niveau(droite);
        if (!livre && autre != niveau) {
            event.setResult(null);
            return;
        }
        ItemStack resultat = gauche.clone();
        resultat.setAmount(1);
        int cout = 0;
        boolean repare = false;

        if (autre != null) {
            Damageable mg = (Damageable) gauche.getItemMeta();
            Damageable md = (Damageable) droite.getItemMeta();
            int max = niveau.solidite;
            int reste = (max - mg.getDamage()) + (max - md.getDamage()) + max * 12 / 100;
            int usure = Math.max(0, max - reste);
            if (usure < mg.getDamage()) {
                Damageable mr = (Damageable) resultat.getItemMeta();
                mr.setDamage(usure);
                resultat.setItemMeta(mr);
                cout += 2;
                repare = true;
            }
        }

        Map<Enchantment, Integer> source = livre
                ? ((EnchantmentStorageMeta) droite.getItemMeta()).getStoredEnchants() : droite.getEnchantments();
        Map<Enchantment, Integer> actuels = new HashMap<>(gauche.getEnchantments());
        boolean ajoute = false;
        for (Map.Entry<Enchantment, Integer> e : source.entrySet()) {
            Enchantment ench = e.getKey();
            if (!Table.permis(ench)) {
                continue;
            }
            boolean compatible = true;
            for (Enchantment deja : actuels.keySet()) {
                if (!deja.equals(ench) && Table.incompatibles(deja, ench)) {
                    compatible = false;
                    cout += 1;
                }
            }
            if (!compatible) {
                continue;
            }
            int avant = actuels.getOrDefault(ench, 0);
            int apres = avant == e.getValue() ? avant + 1 : Math.max(avant, e.getValue());
            apres = Math.min(apres, ench.getMaxLevel());
            actuels.put(ench, apres);
            ajoute = true;
            int c = ench.getAnvilCost();
            if (livre) {
                c = Math.max(1, c / 2);
            }
            cout += c * apres;
        }
        if (!ajoute && !repare) {
            event.setResult(null);
            return;
        }
        for (Enchantment e : resultat.getEnchantments().keySet()) {
            resultat.removeEnchantment(e);
        }
        resultat.addUnsafeEnchantments(actuels);

        AnvilView vue = event.getView();
        String nom = vue.getRenameText();
        if (nom != null && !nom.isBlank()) {
            ItemMeta meta = resultat.getItemMeta();
            Component actuel = meta.displayName();
            String texteActuel = actuel == null ? "" : PlainTextComponentSerializer.plainText().serialize(actuel);
            if (!nom.equals(texteActuel)) {
                meta.displayName(Component.text(nom).decoration(TextDecoration.ITALIC, true));
                resultat.setItemMeta(meta);
                cout += 1;
            }
        }
        int penaliteG = penalite(gauche);
        int penaliteD = penalite(droite);
        resultat.setData(DataComponentTypes.REPAIR_COST, Math.max(penaliteG, penaliteD) * 2 + 1);
        vue.setRepairCost(penaliteG + penaliteD + cout);
        event.setResult(resultat);
    }

    /** Masse ordinaire (ou autre objet) à gauche : pas de Magnétisme, pas de marteau sacrifié pour une masse. */
    private static void proteger(PrepareAnvilEvent event, ItemStack gauche, ItemStack droite) {
        if (gauche == null || gauche.getType() == Material.BOOK || gauche.getType() == Material.ENCHANTED_BOOK) {
            return; // livre + livre : Magnétisme se combine comme les autres enchantements
        }
        if (KSMarteau.niveau(droite) != null) {
            event.setResult(null);
            return;
        }
        ItemStack resultat = event.getResult();
        Enchantment m = KSMarteau.magnetisme();
        if (resultat != null && m != null && resultat.containsEnchantment(m)) {
            event.setResult(null);
        }
    }

    private static int penalite(ItemStack objet) {
        Integer p = objet.getData(DataComponentTypes.REPAIR_COST);
        return p == null ? 0 : p;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onForge(PrepareSmithingEvent event) {
        Recipe recette = event.getInventory().getRecipe();
        if (!(recette instanceof Keyed k) || !k.getKey().equals(recetteNetherite)) {
            return;
        }
        ItemStack base = event.getInventory().getInputEquipment();
        if (KSMarteau.niveau(base) != Niveau.DIAMANT) {
            event.setResult(null);
            return;
        }
        event.setResult(KSMarteau.ameliorer(base));
    }
}
