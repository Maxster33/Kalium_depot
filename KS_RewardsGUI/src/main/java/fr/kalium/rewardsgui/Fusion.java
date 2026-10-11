package fr.kalium.rewardsgui;

import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1.5.0 (LeKiwi06, 11/10/2026) : « j'aimerai qu'on puisse fusionner 2 coffres de récompenses dans l'enclume ce qui
 * additionnerait leurs valeurs, et couterait un nombre de point d'xp égale au nouveau montant en emeraude du coffres [...]
 * le coffre s'appellera "coffre de récompense fusionné", ça permettrait de stacker pleins de petits coffres pour essayer
 * d'avoir une grosse récompense sur la longueur ». Ses réponses : « le clique droit sur l'enclume ouvre une interface de
 * coffre avec un bouton valider sur la case du centre bas et le cout en xp affiché entre parenthèse, ne pas accepter les
 * coffres déjà ouvert » ; contenu : « Nouveau tirage ».
 *
 * - Clic droit sur une enclume, un coffre de récompense en main : ce menu s'ouvre (pas le coffre, pas l'enclume).
 * - Un clic sur un coffre de son inventaire le choisit (2 au plus), un clic sur un coffre choisi le retire. Les coffres
 *   ne quittent pas l'inventaire du joueur : le menu n'en montre que l'image, et rien ne s'y prend ni ne s'y dépose.
 * - Refusés : coffre déjà ouvert, coffre sans valeur moyenne connue, coffre inconnu de ce serveur.
 * - « Valider (N XP) », case du centre de la rangée du bas : N = valeur du coffre fusionné, arrondie à l'unité
 *   supérieure, en points d'expérience (pas en niveaux). Les deux coffres disparaissent avec leur contenu ; le coffre
 *   fusionné reçoit un nouveau contenu, tiré comme une seule récompense de la valeur totale (Tables). Il reste choisi
 *   dans le menu, pour lui ajouter aussitôt un autre coffre.
 */
final class Fusion implements Listener {

    private static final int PREMIER = 11;
    private static final int SECOND = 15;
    private static final int AIDE = 13;
    private static final int VALIDER = 22;

    /** Menu d'un joueur : les deux coffres choisis (identifiants, null : case libre). */
    private static final class Fenetre implements InventoryHolder {
        final String[] choisis = new String[2];
        Inventory inventaire;

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    private final KSRewardsGUI plugin;
    private final Lang lang;
    private final Tables tables;

    Fusion(KSRewardsGUI plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.tables = new Tables(plugin);
    }

    /** Ouvre le menu ; le coffre tenu en main est choisi d'office s'il est accepté. */
    void ouvrir(Player joueur, String enMain) {
        if (!tables.disponibles()) {
            message(joueur, lang.c("fusion.indisponible", "<red>La fusion des coffres n'est pas disponible ici."));
            lang.saveIfNeeded();
            return;
        }
        Fenetre fenetre = new Fenetre();
        fenetre.inventaire = Bukkit.createInventory(fenetre, 27, lang.c("fusion.titre", "<dark_gray>Fusion de coffres"));
        if (enMain != null && plugin.coffres().refusFusion(enMain) == null) {
            fenetre.choisis[0] = enMain;
        }
        remplir(joueur, fenetre);
        joueur.openInventory(fenetre.inventaire);
        lang.saveIfNeeded();
    }

    private static int cout(double valeur) {
        return (int) Math.max(1, Math.ceil(valeur - 1e-9));
    }

    private double valeur(Fenetre fenetre) {
        return plugin.coffres().valeur(fenetre.choisis[0]) + plugin.coffres().valeur(fenetre.choisis[1]);
    }

    private static boolean assez(Player joueur, int cout) {
        return joueur.getGameMode() == GameMode.CREATIVE || joueur.calculateTotalExperiencePoints() >= cout;
    }

    private void remplir(Player joueur, Fenetre fenetre) {
        Inventory menu = fenetre.inventaire;
        for (int place = 0; place < menu.getSize(); place++) {
            menu.setItem(place, Contenant.decor());
        }
        menu.setItem(PREMIER, fenetre.choisis[0] == null ? null : plugin.coffres().image(fenetre.choisis[0]));
        menu.setItem(SECOND, fenetre.choisis[1] == null ? null : plugin.coffres().image(fenetre.choisis[1]));
        menu.setItem(AIDE, Contenant.objet(Material.ANVIL, lang.c("fusion.aide", "<gold>Fusion de coffres"), List.of(
                lang.c("fusion.aide-1", "<gray>Clique sur 2 coffres de récompense"),
                lang.c("fusion.aide-2", "<gray>non ouverts de ton inventaire."),
                lang.c("fusion.aide-3", "<gray>Leurs valeurs s'additionnent et"),
                lang.c("fusion.aide-4", "<gray>un nouveau contenu est tiré."))));
        if (fenetre.choisis[0] == null || fenetre.choisis[1] == null) {
            menu.setItem(VALIDER, Contenant.objet(Material.GRAY_DYE, lang.c("fusion.attente", "<gray>Valider"),
                    List.of(lang.c("fusion.attente-1", "<dark_gray>Choisis d'abord 2 coffres."))));
            return;
        }
        double valeur = valeur(fenetre);
        int cout = cout(valeur);
        boolean assez = assez(joueur, cout);
        List<Component> lignes = new ArrayList<>();
        lignes.add(lang.c("fusion.resultat", "<gold>Coffre de récompense fusionné"));
        lignes.add(lang.c("coffre.valeur", "<gray>Valeur moyenne : <yellow><valeur></yellow> émeraude(s)", "valeur",
                Coffres.nombre(valeur)));
        lignes.add(lang.c("fusion.cout", "<gray>Coût : <cout> points d'expérience", "cout", cout));
        lignes.add(assez ? lang.c("fusion.xp", "<gray>Tu en as <n>.", "n", joueur.calculateTotalExperiencePoints())
                : lang.c("fusion.xp-manque", "<red>Tu n'en as que <n>.", "n", joueur.calculateTotalExperiencePoints()));
        menu.setItem(VALIDER, Contenant.objet(assez ? Material.LIME_DYE : Material.RED_DYE, assez
                ? lang.c("fusion.valider", "<green>Valider (<cout> XP)", "cout", cout)
                : lang.c("fusion.valider-manque", "<red>Valider (<cout> XP)", "cout", cout), lignes));
    }

    private static void message(Player joueur, Component texte) {
        joueur.sendActionBar(texte);
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Fenetre fenetre)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player joueur) || event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory() == fenetre.inventaire) {
            switch (event.getSlot()) {
                case PREMIER -> fenetre.choisis[0] = null;
                case SECOND -> fenetre.choisis[1] = null;
                case VALIDER -> valider(joueur, fenetre);
                default -> {
                    return;
                }
            }
        } else {
            choisir(joueur, fenetre, plugin.coffres().id(event.getCurrentItem()));
        }
        remplir(joueur, fenetre);
        lang.saveIfNeeded();
    }

    /** Clic sur un objet de son inventaire : un coffre de récompense accepté prend la première case libre. */
    private void choisir(Player joueur, Fenetre fenetre, String id) {
        if (id == null || id.equals(fenetre.choisis[0]) || id.equals(fenetre.choisis[1])) {
            return;
        }
        Component refus = plugin.coffres().refusFusion(id);
        if (refus != null) {
            message(joueur, refus);
        } else if (fenetre.choisis[0] == null) {
            fenetre.choisis[0] = id;
        } else if (fenetre.choisis[1] == null) {
            fenetre.choisis[1] = id;
        } else {
            message(joueur, lang.c("fusion.complet", "<red>2 coffres sont déjà choisis : clique sur l'un pour le retirer."));
        }
    }

    private void valider(Player joueur, Fenetre fenetre) {
        String a = fenetre.choisis[0];
        String b = fenetre.choisis[1];
        if (a == null || b == null) {
            return;
        }
        // Les coffres ont pu être ouverts, ou ne plus exister, depuis qu'ils ont été choisis.
        for (int i = 0; i < 2; i++) {
            Component refus = plugin.coffres().refusFusion(fenetre.choisis[i]);
            if (refus != null || plugin.coffres().caseDe(joueur, fenetre.choisis[i]) < 0) {
                fenetre.choisis[i] = null;
                message(joueur, refus != null ? refus : lang.c("fusion.absent", "<red>Ce coffre n'est plus dans ton inventaire."));
                return;
            }
        }
        double valeur = valeur(fenetre);
        int cout = cout(valeur);
        if (!assez(joueur, cout)) {
            message(joueur, lang.c("fusion.manque", "<red>Il te faut <cout> points d'expérience.", "cout", cout));
            return;
        }
        List<Map<String, Object>> contenu = tables.tirer(valeur);
        if (contenu.isEmpty()) {
            message(joueur, lang.c("fusion.indisponible", "<red>La fusion des coffres n'est pas disponible ici."));
            return;
        }
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            joueur.setExperienceLevelAndProgress(joueur.calculateTotalExperiencePoints() - cout);
        }
        fenetre.choisis[0] = plugin.coffres().fusionner(joueur, a, b, valeur, cout, contenu);
        fenetre.choisis[1] = null;
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1f);
        joueur.sendActionBar(lang.c("fusion.fait", "<green>Coffres fusionnés : <valeur> émeraude(s) de valeur moyenne.",
                "valeur", Coffres.nombre(valeur)));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Fenetre) {
            event.setCancelled(true);
        }
    }
}
