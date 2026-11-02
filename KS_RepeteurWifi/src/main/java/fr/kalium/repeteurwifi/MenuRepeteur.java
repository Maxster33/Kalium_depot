package fr.kalium.repeteurwifi;

import fr.kalium.menu.api.Contenant;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Menu en coffre d'un répéteur (boîte à outils de KLM_Menu) : émission, code, réception ; en bas, aide et « Fermer ».
 *
 * Le bouton « Code » ouvre une enclume sans bloc (MenuType.ANVIL) : un papier au nom du code actuel dans la première
 * case, le joueur le renomme. Le résultat montre le code tapé (vert s'il est valable). Validation : clic sur le résultat
 * (Java), ou fermeture de l'enclume avec un code valable (Bedrock, dont le jeu refuse de prendre un résultat à 0 niveau ;
 * Geyser transmet le texte au serveur pendant la frappe). Rien ne se prend ni ne se dépose dans cette enclume, et le
 * papier disparaît à la fermeture. Codes : 6 lettres ou chiffres, mis en majuscules.
 */
final class MenuRepeteur implements Listener {

    private static final String MARQUE = "ks_repeteurwifi";
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9]{6}");

    /** Une enclume de saisie ouverte : sa fenêtre et le répéteur réglé. */
    private record Saisie(InventoryView vue, Block bloc) {
    }

    private final KSRepeteurWifi plugin;
    private final Map<UUID, Saisie> saisies = new HashMap<>();

    MenuRepeteur(KSRepeteurWifi plugin) {
        this.plugin = plugin;
    }

    /** Le code tapé, en majuscules, ou null s'il n'a pas 6 lettres ou chiffres. */
    static String code(String texte) {
        if (texte == null) {
            return null;
        }
        String t = texte.trim();
        return CODE.matcher(t).matches() ? t.toUpperCase(Locale.ROOT) : null;
    }

    void ouvrir(Player joueur, Block b) {
        Repeteurs.Repeteur r = plugin.repeteurs.get(b);
        if (r == null) {
            return;
        }
        Contenant menu = Contenant.pour(joueur, 3, plugin.t("menu.titre", "Répéteur wifi"), MARQUE);
        menu.poser(10, mode(Material.REPEATER, plugin.t("menu.emission", "<gold>Émission"),
                plugin.t("menu.emission-aide", "<gray>Le courant reçu allume les récepteurs du même code."), r.emetteur),
                p -> choisir(p, b, true));
        menu.poser(16, mode(Material.COMPARATOR, plugin.t("menu.reception", "<aqua>Réception"),
                plugin.t("menu.reception-aide", "<gray>S'allume quand un émetteur du même code reçoit du courant."),
                !r.emetteur), p -> choisir(p, b, false));
        List<Component> lignes = new ArrayList<>();
        lignes.add(r.code == null ? plugin.t("menu.code-aucun", "<red>Aucun code")
                : plugin.t("menu.code-actuel", "<gray>Code : <white><code>", "code", r.code));
        lignes.addAll(Contenant.lignes(plugin.t("menu.code-clic", "<gray>Clic : taper 6 lettres ou chiffres")));
        menu.poser(13, Contenant.objet(Material.NAME_TAG, plugin.t("menu.code", "<yellow>Code"), lignes),
                p -> saisir(p, b));
        menu.barre();
        menu.poser(menu.bas(3), Contenant.objet(Material.BOOK, plugin.t("menu.aide", "<yellow>Aide"),
                Contenant.lignes(plugin.t("menu.aide-texte", "<gray>Un récepteur s'allume quand un émetteur du même "
                        + "code, à <portee> blocs ou moins, reçoit du courant.", "portee", plugin.repeteurs.portee()))),
                null);
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, plugin.t("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        menu.ouvrir(joueur);
    }

    private ItemStack mode(Material icone, Component nom, Component aide, boolean choisi) {
        List<Component> lignes = new ArrayList<>(Contenant.lignes(aide));
        lignes.add(choisi ? plugin.t("menu.mode-choisi", "<green>Mode choisi")
                : plugin.t("menu.mode-clic", "<gray>Clic : choisir ce mode"));
        ItemStack objet = Contenant.objet(icone, nom, lignes);
        if (choisi) {
            ItemMeta meta = objet.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            objet.setItemMeta(meta);
        }
        return objet;
    }

    /** Le répéteur est encore là (un clic dans un menu resté ouvert après sa casse ne fait rien). */
    private boolean present(Player joueur, Block b) {
        if (plugin.repeteurs.get(b) != null && KSRepeteurWifi.estRepeteur(b)) {
            return true;
        }
        Contenant.fermer(joueur);
        return false;
    }

    private void choisir(Player joueur, Block b, boolean emetteur) {
        if (!present(joueur, b)) {
            return;
        }
        plugin.repeteurs.changerMode(b, emetteur);
        ouvrir(joueur, b);
    }

    // ------------------------------------------------------------------ saisie du code (enclume)

    private void saisir(Player joueur, Block b) {
        if (!present(joueur, b)) {
            return;
        }
        String actuel = plugin.repeteurs.get(b).code;
        Contenant.saisie(joueur, () -> {
            AnvilView vue = MenuType.ANVIL.builder().title(plugin.t("enclume.titre", "Code du répéteur"))
                    .checkReachable(false).build(joueur);
            // sans code : un nom invalide (le jeu Java renvoie le nom du papier comme texte tapé)
            ItemStack papier = new ItemStack(Material.PAPER);
            ItemMeta meta = papier.getItemMeta();
            meta.displayName(actuel == null ? plugin.t("enclume.papier", "Code") : Component.text(actuel));
            papier.setItemMeta(meta);
            saisies.put(joueur.getUniqueId(), new Saisie(vue, b));
            vue.getTopInventory().setFirstItem(papier);
            joueur.openInventory(vue);
            if (!vue.getTopInventory().equals(joueur.getOpenInventory().getTopInventory())) {
                saisies.remove(joueur.getUniqueId()); // ouverture refusée
                return;
            }
            joueur.sendMessage(plugin.t("message.enclume",
                    "<gray>Renomme le papier : 6 lettres ou chiffres, puis clique sur le résultat ou ferme l'enclume."));
        });
    }

    private Saisie saisie(InventoryView vue) {
        if (!(vue.getPlayer() instanceof Player joueur)) {
            return null;
        }
        Saisie s = saisies.get(joueur.getUniqueId());
        return s != null && s.vue().getTopInventory().equals(vue.getTopInventory()) ? s : null;
    }

    /** Après tous les autres plugins : le résultat montre le code tapé, sans coût. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareAnvilEvent event) {
        if (saisie(event.getView()) == null) {
            return;
        }
        String code = code(event.getView().getRenameText());
        ItemStack resultat = code == null
                ? Contenant.objet(Material.BARRIER, plugin.t("enclume.invalide", "<red>Code invalide"),
                Contenant.lignes(plugin.t("enclume.invalide-aide", "<gray>6 lettres ou chiffres, sans espace")))
                : Contenant.objet(Material.PAPER, plugin.t("enclume.valider", "<green>Valider : <code>", "code", code),
                Contenant.lignes(plugin.t("enclume.valider-aide", "<gray>Clic ici ou ferme l'enclume")));
        event.setResult(resultat);
        event.getView().setRepairCost(0);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClic(InventoryClickEvent event) {
        Saisie s = saisie(event.getView());
        if (s == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory() && event.getRawSlot() == 2
                && event.getWhoClicked() instanceof Player joueur) {
            String code = code(((AnvilView) event.getView()).getRenameText());
            if (code == null) {
                return;
            }
            appliquer(joueur, s, code);
            Contenant.apres(() -> {
                joueur.closeInventory();
                if (KSRepeteurWifi.estRepeteur(s.bloc())) {
                    ouvrir(joueur, s.bloc());
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGlisser(InventoryDragEvent event) {
        if (saisie(event.getView()) != null) {
            event.setCancelled(true);
        }
    }

    /** Fermeture : code valable appliqué (s'il n'a pas déjà été validé), papier retiré avant que le jeu le rende. */
    @EventHandler
    public void onFermeture(InventoryCloseEvent event) {
        Saisie s = saisie(event.getView());
        if (s == null) {
            return;
        }
        saisies.remove(event.getPlayer().getUniqueId());
        String code = code(((AnvilView) event.getView()).getRenameText());
        event.getView().getTopInventory().clear();
        if (code != null && event.getPlayer() instanceof Player joueur
                && event.getReason() != InventoryCloseEvent.Reason.DISCONNECT) {
            appliquer(joueur, s, code);
        }
    }

    /** Règle le code (sans effet s'il ne change pas : la fermeture qui suit un clic sur le résultat ne refait rien). */
    private void appliquer(Player joueur, Saisie s, String code) {
        Repeteurs.Repeteur r = plugin.repeteurs.get(s.bloc());
        if (r == null || !KSRepeteurWifi.estRepeteur(s.bloc())) {
            Contenant.message(joueur, plugin.t("message.disparu", "<red>Ce répéteur n'existe plus."), true);
            return;
        }
        if (code.equals(r.code)) {
            return;
        }
        plugin.repeteurs.changerCode(s.bloc(), code);
        Contenant.message(joueur, plugin.t("message.code", "<green>Code réglé : <white><code>", "code", code), false);
    }

    /** Arrêt du plugin : les enclumes de saisie sont fermées (papier retiré). */
    void fermerTout() {
        for (Saisie s : new ArrayList<>(saisies.values())) {
            s.vue().getTopInventory().clear();
            s.vue().close();
        }
        saisies.clear();
    }
}
