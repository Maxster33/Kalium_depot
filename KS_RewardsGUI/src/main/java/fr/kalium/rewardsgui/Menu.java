package fr.kalium.rewardsgui;

import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /rewards : récompenses en attente. 1.4.0 (LeKiwi06, 09/10/2026 : « davantage d'interfaces de type contenant [...] au
 * lieu des boutons ») : un coffre de 6 rangées ; en haut, un objet par récompense (45 par page), dont le clic la
 * récupère ; en bas, une barre d'actions sur fond de vitres grises (pages, tout récupérer, aide, fermer). Les messages
 * s'affichent au-dessus de la barre d'objets du joueur, plus dans une fenêtre ni dans le tchat.
 *
 * 1.2.0 : une récompense arrive dans un coffre fermé (une case d'inventaire, contenu caché jusqu'à l'ouverture) ; un
 * dépôt local (coffre de mort) est rendu en objets.
 */
final class Menu {

    private static final int PAR_PAGE = 45;
    private static final String MARQUE = "rewards";

    private final KSRewardsGUI plugin;
    private final Lang lang;

    Menu(KSRewardsGUI plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
    }

    void ouvrir(Player joueur) {
        page(joueur, 0);
    }

    private void page(Player joueur, int page) {
        List<Map.Entry<String, Recompense>> liste = plugin.enAttente(joueur.getUniqueId());
        int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        // Le menu déjà ouvert est rempli à nouveau : il ne se referme pas entre deux clics.
        Contenant ouvert = Contenant.ouvert(joueur, MARQUE);
        Contenant menu = ouvert != null ? ouvert : new Contenant(6, lang.c("liste.titre", "<dark_gray>Récompenses"), MARQUE);
        menu.vider();
        for (int i = p * PAR_PAGE; i < Math.min(liste.size(), (p + 1) * PAR_PAGE); i++) {
            Recompense r = liste.get(i).getValue();
            String id = liste.get(i).getKey();
            menu.poser(i - p * PAR_PAGE, icone(r), j -> {
                Component refus = recuperer(j, id);
                if (refus == null) {
                    j.playSound(j.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1f);
                } else {
                    message(j, refus);
                }
                page(j, p);
            });
        }
        if (liste.isEmpty()) {
            menu.poser(22, Contenant.objet(Material.PAPER, lang.c("liste.vide", "<gray>Aucune récompense en attente"),
                    List.of(lang.c("liste.vide-1", "<dark_gray>Les récompenses gagnées sur les"),
                            lang.c("liste.vide-2", "<dark_gray>autres serveurs arrivent ici."))), null);
        }
        // Barre d'actions
        for (int place = 45; place < 54; place++) {
            menu.poser(place, Contenant.decor(), null);
        }
        if (p > 0) {
            menu.poser(45, Contenant.objet(Material.ARROW, lang.c("liste.precedent", "<yellow>Page précédente"),
                    List.of(lang.c("liste.page", "<gray>Page <n> sur <total>", "n", p, "total", pages))), j -> page(j, p - 1));
        }
        if (p < pages - 1) {
            menu.poser(53, Contenant.objet(Material.ARROW, lang.c("liste.suivant", "<yellow>Page suivante"),
                    List.of(lang.c("liste.page", "<gray>Page <n> sur <total>", "n", p + 2, "total", pages))),
                    j -> page(j, p + 1));
        }
        menu.poser(48, Contenant.objet(Material.BOOK, lang.c("liste.aide", "<aqua>Comment ça marche"),
                List.of(lang.c("liste.aide-1", "<gray>Chaque récompense arrive dans"),
                        lang.c("liste.aide-2", "<gray>un coffre fermé."),
                        lang.c("liste.aide-3", "<gray>Clic droit, coffre en main,"),
                        lang.c("liste.aide-4", "<gray>pour l'ouvrir."))), null);
        if (liste.size() > 1) {
            menu.poser(49, Contenant.objet(Material.HOPPER, lang.c("liste.tout", "<green>Tout récupérer"),
                    List.of(lang.c("liste.tout-1", "<gray><nombre> récompenses en attente", "nombre", liste.size()),
                            lang.c("liste.tout-2", "<dark_gray>Tant qu'il reste de la place."))), j -> {
                toutRecuperer(j);
                page(j, p);
            });
        }
        menu.poser(50, Contenant.objet(Material.BARRIER, lang.c("liste.fermer", "<red>Fermer"), List.of()),
                Player::closeInventory);
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** L'objet d'une récompense dans la liste : un coffre (contenu caché), ou un coffre de l'Ender pour un dépôt local. */
    private ItemStack icone(Recompense r) {
        List<Component> lignes = new ArrayList<>();
        lignes.add(lang.c("liste.origine", "<gray>Origine : <white><origine>", "origine", r.origine));
        if (r.locale) {
            lignes.add(lang.c("liste.piles", "<gray><nombre> pile(s) d'objets", "nombre", r.contenu.size()));
            lignes.add(lang.c("liste.clic-objets", "<green>Clic : récupérer tes objets"));
            return Contenant.objet(Material.ENDER_CHEST, lang.c("liste.nom", "<gold><raison>", "raison", r.raison), lignes);
        }
        if (r.valeur > 0) {
            lignes.add(lang.c("liste.valeur", "<gray>Valeur moyenne : <yellow><valeur></yellow> émeraude(s)", "valeur",
                    Coffres.nombre(r.valeur)));
        }
        lignes.add(lang.c("liste.clic-coffre", "<green>Clic : récupérer ce coffre"));
        return Contenant.objet(Material.CHEST, lang.c("liste.nom", "<gold><raison>", "raison", r.raison), lignes);
    }

    /** Message court au-dessus de la barre d'objets du joueur, avec un son de refus. */
    private static void message(Player joueur, Component texte) {
        joueur.sendActionBar(texte);
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
    }

    /** Récupère une récompense ; renvoie null, ou le message du refus (elle reste alors en attente). */
    private Component recuperer(Player joueur, String id) {
        Recompense r = null;
        for (Map.Entry<String, Recompense> e : plugin.enAttente(joueur.getUniqueId())) {
            if (e.getKey().equals(id)) {
                r = e.getValue();
            }
        }
        if (r == null) {
            return null; // déjà récupérée
        }
        if (!r.locale) {
            return recupererCoffre(joueur, id, r);
        }
        List<ItemStack> objets = r.objets();
        if (objets == null) {
            return lang.c("liste.inconnu", "<red>Un objet n'existe pas encore sur ce serveur : préviens un admin.");
        }
        long argent = r.argent();
        if (argent > 0 && !Bukkit.getPluginManager().isPluginEnabled("KS_Economy")) {
            return lang.c("liste.sans-economie", "<red>L'économie est indisponible : récompense gardée.");
        }
        if (!rentre(joueur, objets)) {
            return lang.c("liste.plein", "<red>Pas assez de place dans ton inventaire.");
        }
        plugin.recuperee(joueur, id, r);
        objets.forEach(o -> joueur.getInventory().addItem(o));
        if (argent > 0) {
            fr.kalium.economy.KSEconomy.crediter(joueur.getUniqueId(), argent);
        }
        return null;
    }

    /**
     * 1.2.0 : la récompense est remise dans un coffre fermé (une case libre suffit). Les objets et les points ne sont
     * créés qu'à l'ouverture du coffre : rien n'est vérifié ici.
     */
    private Component recupererCoffre(Player joueur, String id, Recompense r) {
        if (joueur.getInventory().firstEmpty() < 0) {
            return lang.c("liste.plein", "<red>Pas assez de place dans ton inventaire.");
        }
        ItemStack coffre = plugin.coffres().creer(r);
        plugin.recuperee(joueur, id, r);
        joueur.getInventory().addItem(coffre);
        // Signal pour l'anti-triche (revente suspecte) : l'objet reçu est le coffre.
        Bukkit.getPluginManager().callEvent(new fr.kalium.rewardsgui.api.RecompenseRecupereeEvent(joueur, r.origine,
                r.raison, List.of(coffre), 0, r.date));
        return null;
    }

    private void toutRecuperer(Player joueur) {
        int recuperees = 0;
        int restantes = 0;
        for (Map.Entry<String, Recompense> e : plugin.enAttente(joueur.getUniqueId())) {
            if (recuperer(joueur, e.getKey()) == null) {
                recuperees++;
            } else {
                restantes++;
            }
        }
        if (restantes == 0) {
            joueur.sendActionBar(lang.c("liste.tout-fait", "<green><nombre> récompense(s) récupérée(s).", "nombre",
                    recuperees));
            joueur.playSound(joueur.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1f);
        } else {
            message(joueur, lang.c("liste.tout-partiel", "<yellow><nombre> récupérée(s), <reste> en attente : fais de la "
                    + "place.", "nombre", recuperees, "reste", restantes));
        }
    }

    /** L'inventaire (hors armure et main secondaire) peut-il recevoir tous ces objets ? */
    private static boolean rentre(Player joueur, List<ItemStack> objets) {
        Inventory essai = Bukkit.createInventory(null, 36);
        ItemStack[] contenu = joueur.getInventory().getStorageContents();
        for (int i = 0; i < contenu.length && i < 36; i++) {
            essai.setItem(i, contenu[i] == null ? null : contenu[i].clone());
        }
        for (ItemStack objet : objets) {
            if (!essai.addItem(objet.clone()).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
