package fr.kalium.marteau;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Table d'enchantement (cahier § 3) : pour un marteau et pour un livre, KS_Marteau tire lui-même les enchantements,
 * avec l'algorithme du jeu (EnchantmentHelper.selectEnchantment : niveau modifié par l'enchantabilité, tirage pondéré,
 * enchantements suivants avec 1 chance sur 50 par niveau, un enchantement retiré d'un livre qui en a plusieurs).
 * Les coûts en niveaux restent ceux du jeu (bibliothèques).
 * - Marteau : Efficacité, Toucher de soie, Solidité, Aura de feu, Fléau des arthropodes (masse du jeu) et Magnétisme ;
 *   ni Brèche, ni Densité, ni Charge de vent, ni Châtiment, ni Fortune.
 * - Livre : les enchantements du jeu (tag in_enchanting_table) et Magnétisme, aussi fréquent que Fortune.
 * Seul ce tirage propose Magnétisme : dans un monde où KS_Marteau est en test, ses écouteurs ne sont pas appelés et la
 * table reste celle du jeu.
 */
final class Table implements Listener {

    /** Enchantements du marteau à la table (ordre sans importance). */
    static List<Enchantment> enchantementsMarteau() {
        List<Enchantment> l = new ArrayList<>(List.of(Enchantment.EFFICIENCY, Enchantment.SILK_TOUCH,
                Enchantment.UNBREAKING, Enchantment.FIRE_ASPECT, Enchantment.BANE_OF_ARTHROPODS));
        if (KSMarteau.magnetisme() != null) {
            l.add(KSMarteau.magnetisme());
        }
        return l;
    }

    /** Enchantements permis sur le marteau (table + enclume) : ceux de la table, plus Raccommodage et la malédiction. */
    static boolean permis(Enchantment e) {
        return enchantementsMarteau().contains(e) || e.equals(Enchantment.MENDING)
                || e.equals(Enchantment.VANISHING_CURSE);
    }

    /** Incompatibles sur le marteau : règles du jeu, plus Toucher de soie et Aura de feu (cahier). */
    static boolean incompatibles(Enchantment a, Enchantment b) {
        if (a.equals(b)) {
            return true;
        }
        if (a.conflictsWith(b) || b.conflictsWith(a)) {
            return true;
        }
        Set<Enchantment> paire = Set.of(a, b);
        return paire.contains(Enchantment.SILK_TOUCH) && paire.contains(Enchantment.FIRE_ASPECT);
    }

    private List<Enchantment> livres;
    /** Derniers tirages proposés à chaque joueur, par bouton (0 à 2). */
    private final Map<UUID, List<Map<Enchantment, Integer>>> proposes = new HashMap<>();

    private List<Enchantment> enchantementsLivres() {
        if (livres == null) {
            List<Enchantment> l = new ArrayList<>(RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                    .getTagValues(EnchantmentTagKeys.IN_ENCHANTING_TABLE));
            if (KSMarteau.magnetisme() != null && !l.contains(KSMarteau.magnetisme())) {
                l.add(KSMarteau.magnetisme());
            }
            livres = l;
        }
        return livres;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareItemEnchantEvent event) {
        ItemStack objet = event.getItem();
        boolean livre = objet.getType() == Material.BOOK;
        if (!livre && KSMarteau.niveau(objet) == null) {
            return;
        }
        if (livre && KSMarteau.magnetisme() == null) {
            return; // rien à ajouter au tirage du jeu
        }
        List<Enchantment> liste = livre ? enchantementsLivres() : enchantementsMarteau();
        int enchantabilite = livre ? 1 : enchantabilite(objet);
        int graine = event.getEnchanter().getEnchantmentSeed();
        int[] couts = event.getExpLevelCostsOffered();
        EnchantmentOffer[] offres = event.getOffers();
        List<Map<Enchantment, Integer>> tirages = new ArrayList<>();
        for (int i = 0; i < offres.length; i++) {
            Map<Enchantment, Integer> tirage = new LinkedHashMap<>();
            if (couts[i] > 0) {
                Random hasard = new Random(graine + i);
                List<Choix> choix = choisir(hasard, liste, couts[i], enchantabilite);
                if (livre && choix.size() > 1) {
                    choix.remove(hasard.nextInt(choix.size()));
                }
                if (!choix.isEmpty()) {
                    Choix indice = choix.get(hasard.nextInt(choix.size()));
                    offres[i] = new EnchantmentOffer(indice.e(), indice.niveau(), couts[i]);
                    choix.forEach(c -> tirage.put(c.e(), c.niveau()));
                } else {
                    offres[i] = null;
                }
            } else {
                offres[i] = null;
            }
            tirages.add(tirage);
        }
        proposes.put(event.getEnchanter().getUniqueId(), tirages);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        ItemStack objet = event.getItem();
        boolean livre = objet.getType() == Material.BOOK;
        if (!livre && KSMarteau.niveau(objet) == null) {
            return;
        }
        if (livre && KSMarteau.magnetisme() == null) {
            return;
        }
        List<Map<Enchantment, Integer>> tirages = proposes.get(event.getEnchanter().getUniqueId());
        int bouton = event.whichButton();
        if (tirages == null || bouton < 0 || bouton >= tirages.size() || tirages.get(bouton).isEmpty()) {
            event.setCancelled(true);
            return;
        }
        event.getEnchantsToAdd().clear();
        event.getEnchantsToAdd().putAll(tirages.get(bouton));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        proposes.remove(event.getPlayer().getUniqueId());
    }

    private static int enchantabilite(ItemStack objet) {
        Enchantable e = objet.getData(DataComponentTypes.ENCHANTABLE);
        return e != null ? e.value() : 15;
    }

    // ------------------------------------------------------------------ algorithme du jeu

    record Choix(Enchantment e, int niveau) {
    }

    /** EnchantmentHelper.selectEnchantment (Minecraft 26.x), avec la liste et les incompatibilités du marteau. */
    static List<Choix> choisir(Random hasard, List<Enchantment> liste, int cout, int enchantabilite) {
        List<Choix> resultat = new ArrayList<>();
        if (enchantabilite <= 0) {
            return resultat;
        }
        int niveau = cout + 1 + hasard.nextInt(enchantabilite / 4 + 1) + hasard.nextInt(enchantabilite / 4 + 1);
        float ecart = (hasard.nextFloat() + hasard.nextFloat() - 1.0f) * 0.15f;
        niveau = Math.max(1, Math.round(niveau + niveau * ecart));
        List<Choix> dispo = disponibles(niveau, liste);
        if (dispo.isEmpty()) {
            return resultat;
        }
        resultat.add(tirer(hasard, dispo));
        while (hasard.nextInt(50) <= niveau) {
            Choix dernier = resultat.get(resultat.size() - 1);
            dispo.removeIf(c -> incompatibles(c.e(), dernier.e()));
            if (dispo.isEmpty()) {
                break;
            }
            resultat.add(tirer(hasard, dispo));
            niveau /= 2;
        }
        return resultat;
    }

    /** Pour chaque enchantement, le plus haut niveau dont la fourchette de coût contient ce niveau. */
    private static List<Choix> disponibles(int niveau, List<Enchantment> liste) {
        List<Choix> dispo = new ArrayList<>();
        for (Enchantment e : liste) {
            for (int n = e.getMaxLevel(); n >= e.getStartLevel(); n--) {
                if (niveau >= e.getMinModifiedCost(n) && niveau <= e.getMaxModifiedCost(n)) {
                    dispo.add(new Choix(e, n));
                    break;
                }
            }
        }
        return dispo;
    }

    private static Choix tirer(Random hasard, List<Choix> dispo) {
        int total = 0;
        for (Choix c : dispo) {
            total += c.e().getWeight();
        }
        int t = hasard.nextInt(Math.max(1, total));
        for (Choix c : dispo) {
            t -= c.e().getWeight();
            if (t < 0) {
                return c;
            }
        }
        return dispo.get(dispo.size() - 1);
    }
}
