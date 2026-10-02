package fr.kalium.rewardsgui;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /rewards : récompenses en attente, 8 par page ; « Récupérer » (une) ou « Tout récupérer » (dans l'ordre d'arrivée,
 * tant que l'inventaire a de la place). Textes de boutons courts (jamais de défilement), détail en texte au-dessus.
 */
final class Menu {

    private static final int PAR_PAGE = 8;

    private final KSRewardsGUI plugin;
    private final Lang lang;
    private final Gui gui;

    Menu(KSRewardsGUI plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    void ouvrir(Player joueur) {
        page(joueur, 0);
    }

    private void page(Player joueur, int page) {
        List<Map.Entry<String, Recompense>> liste = plugin.enAttente(joueur.getUniqueId());
        int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        if (liste.isEmpty()) {
            corps.add(lang.c("menu.vide", "<gray>Aucune récompense en attente. Les récompenses gagnées sur les autres "
                    + "serveurs arrivent ici."));
        }
        for (int i = p * PAR_PAGE; i < Math.min(liste.size(), (p + 1) * PAR_PAGE); i++) {
            Recompense r = liste.get(i).getValue();
            String id = liste.get(i).getKey();
            int numero = i + 1;
            corps.add(lang.c("menu.ligne", "<white><n>. <gold><origine></gold> - <raison> : ", "n", numero, "origine",
                    r.origine, "raison", r.raison).append(r.description()));
            boutons.add(gui.button(lang.c("menu.recuperer", "<green>Récupérer <n>", "n", numero), null,
                    j -> recuperer(j, id, true)));
        }
        if (liste.size() > 1) {
            boutons.add(gui.button(lang.c("menu.tout", "<green><bold>Tout récupérer"), null, this::toutRecuperer));
        }
        if (p > 0) {
            boutons.add(gui.button(lang.c("menu.precedent", "<yellow>Page précédente"), null, j -> page(j, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(lang.c("menu.suivant", "<yellow>Page suivante"), null, j -> page(j, p + 1)));
        }
        gui.open(joueur, lang.c("menu.titre", "<light_purple><bold>Récompenses <gray>(<nombre>)", "nombre", liste.size()),
                corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    /** Une récompense : refusée (message) si un objet ne peut pas être créé ou si l'inventaire manque de place. */
    private boolean recuperer(Player joueur, String id, boolean rouvrir) {
        Recompense r = null;
        for (Map.Entry<String, Recompense> e : plugin.enAttente(joueur.getUniqueId())) {
            if (e.getKey().equals(id)) {
                r = e.getValue();
            }
        }
        if (r == null) {
            if (rouvrir) {
                ouvrir(joueur);
            }
            return false;
        }
        List<ItemStack> objets = r.objets();
        if (objets == null) {
            notice(joueur, lang.c("menu.inconnu", "<red>Un objet de cette récompense n'existe pas (encore) sur ce "
                    + "serveur : préviens un administrateur. Elle reste en attente."));
            return false;
        }
        long argent = r.argent();
        if (argent > 0 && !Bukkit.getPluginManager().isPluginEnabled("KS_Economy")) {
            notice(joueur, lang.c("menu.sans-economie", "<red>L'économie est indisponible : récompense gardée."));
            return false;
        }
        if (!rentre(joueur, objets)) {
            if (rouvrir) {
                notice(joueur, lang.c("menu.plein", "<red>Pas assez de place dans ton inventaire."));
            }
            return false;
        }
        plugin.recuperee(joueur, id, r);
        objets.forEach(o -> joueur.getInventory().addItem(o));
        if (argent > 0) {
            fr.kalium.economy.KSEconomy.crediter(joueur.getUniqueId(), argent);
        }
        if (rouvrir) {
            ouvrir(joueur);
        }
        return true;
    }

    private void toutRecuperer(Player joueur) {
        int recuperees = 0;
        int restantes = 0;
        for (Map.Entry<String, Recompense> e : plugin.enAttente(joueur.getUniqueId())) {
            if (recuperer(joueur, e.getKey(), false)) {
                recuperees++;
            } else {
                restantes++;
            }
        }
        notice(joueur, restantes == 0
                ? lang.c("menu.tout-fait", "<green><nombre> récompense(s) récupérée(s).", "nombre", recuperees)
                : lang.c("menu.tout-partiel", "<yellow><nombre> récupérée(s) ; <reste> en attente (place ou objet "
                        + "manquant).", "nombre", recuperees, "reste", restantes));
    }

    private void notice(Player joueur, Component texte) {
        gui.notice(joueur, lang.c("menu.titre-court", "<light_purple><bold>Récompenses"), texte, this::ouvrir);
        lang.saveIfNeeded();
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
