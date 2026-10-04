package fr.kalium.economy;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu « Économie » (/economie ou bouton de KS_Menu) : solde, tout déposer, déposer des objets (coffre), retirer,
 * masquer / révéler son solde. Menus en Dialog (boîte à outils de KLM_Menu), sauf le dépôt (coffre).
 */
final class MenuEconomie implements Listener {

    /** Objets qu'on peut retirer : id du choix -> (objet, valeur en émeraudes). */
    private static final List<String> RETRAITS = List.of("emeraude", "bloc", "tier1", "tier2", "tier3", "tier4",
            "tier5", "tier6");

    private final KSEconomy plugin;
    private final Lang lang;
    private final Gui gui;

    /** Coffre de dépôt : objets posés dedans convertis à la fermeture. */
    private static final class Depot implements InventoryHolder {
        Inventory inventaire;

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    MenuEconomie(KSEconomy plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private String pts(long valeur) {
        return KSEconomy.points(valeur);
    }

    // ------------------------------------------------------------------ menu principal

    void ouvrir(Player joueur) {
        long solde = KSEconomy.solde(joueur.getUniqueId());
        boolean masque = plugin.estMasque(joueur.getUniqueId());
        List<Component> corps = new ArrayList<>();
        corps.add(lang.c("menu.solde", "<white>Solde : <green><bold><solde></bold>", "solde", pts(solde)));
        corps.add(masque
                ? lang.c("menu.masque", "<gray>Ton solde est <red>masqué</red> dans la liste des joueurs (Tab) : "
                        + "<pourcent> % prélevé chaque jour à minuit.", "pourcent", pourcent())
                : lang.c("menu.visible", "<gray>Ton solde est <green>visible</green> par les autres joueurs dans la "
                        + "liste des joueurs (Tab)."));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(lang.c("menu.bouton-tout-deposer", "<green>Tout déposer"),
                lang.c("menu.info-tout-deposer", "<gray>Émeraudes, blocs d'émeraude et blocs compressés de ton "
                        + "inventaire"), this::toutDeposer));
        boutons.add(gui.button(lang.c("menu.bouton-deposer", "<green>Déposer des objets"),
                lang.c("menu.info-deposer", "<gray>Pose dans le coffre les émeraudes à déposer"), this::ouvrirDepot));
        boutons.add(gui.button(lang.c("menu.bouton-retirer", "<yellow>Retirer"),
                lang.c("menu.info-retirer", "<gray>Émeraudes, blocs ou blocs compressés"), this::ouvrirRetrait));
        // 1.1.0 : catalogue des magasins.
        boutons.add(gui.button(lang.c("menu.bouton-magasins", "<gold>Magasins"),
                lang.c("menu.info-magasins", "<gray>Boutiques des joueurs, achat à distance, ton magasin"),
                p -> plugin.menuMagasin().catalogue(p, 0)));
        // 1.3.0 : rachats de la semaine.
        boutons.add(gui.button(lang.c("menu.bouton-rachats", "<gold>Rachats de la semaine"),
                lang.c("menu.info-rachats", "<gray>10 objets rachetés par le serveur, tirés chaque lundi"),
                p -> plugin.rachats().ouvrir(p)));
        boutons.add(masque
                ? gui.button(lang.c("menu.bouton-reveler", "<aqua>Révéler mon solde"),
                lang.c("menu.info-reveler", "<gray>Gratuit, immédiat"), p -> {
                    plugin.masquer(p, false);
                    ouvrir(p);
                })
                : gui.button(lang.c("menu.bouton-masquer", "<red>Masquer mon solde"),
                lang.c("menu.info-masquer", "<gray>Coûte <pourcent> % de ton solde par jour", "pourcent", pourcent()),
                this::confirmerMasquer));
        gui.open(joueur, lang.c("menu.titre", "<green><bold>Économie"), corps, List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private String pourcent() {
        double valeur = plugin.pourcentMasque();
        return valeur == Math.rint(valeur) ? String.valueOf((long) valeur) : String.valueOf(valeur).replace('.', ',');
    }

    private void confirmerMasquer(Player joueur) {
        gui.confirm(joueur, lang.c("masquer.titre", "<red>Masquer mon solde"),
                lang.c("masquer.texte", "<white>Ton solde ne sera plus visible dans la liste des joueurs. Chaque jour à "
                        + "minuit (heure de Paris), <red><pourcent> %</red> de ton solde est prélevé (arrondi à l'unité "
                        + "inférieure). Tu peux le révéler gratuitement à tout moment.", "pourcent", pourcent()),
                p -> {
                    plugin.masquer(p, true);
                    ouvrir(p);
                }, this::ouvrir);
    }

    private void message(Player joueur, Component texte) {
        gui.notice(joueur, lang.c("menu.titre", "<green><bold>Économie"), texte, this::ouvrir);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ dépôt

    /** Points pour ce nombre d'émeraudes (taux de dépôt, arrondi à l'unité inférieure). */
    private long pointsPour(long emeraudes) {
        return (long) Math.floor(emeraudes * plugin.tauxDepot());
    }

    private void toutDeposer(Player joueur) {
        long emeraudes = 0;
        ItemStack[] contenu = joueur.getInventory().getStorageContents();
        for (int i = 0; i < contenu.length; i++) {
            long valeur = KSEconomy.emeraudesParUnite(contenu[i]);
            if (valeur > 0) {
                emeraudes += valeur * contenu[i].getAmount();
                contenu[i] = null;
            }
        }
        if (emeraudes == 0) {
            message(joueur, lang.c("depot.rien", "<red>Aucune émeraude dans ton inventaire."));
            return;
        }
        joueur.getInventory().setStorageContents(contenu);
        long points = pointsPour(emeraudes);
        KSEconomy.crediter(joueur.getUniqueId(), points);
        message(joueur, lang.c("depot.fait", "<green><points></green> <white>déposés (<emeraudes> émeraudes).",
                "points", pts(points), "emeraudes", KSEconomy.nombre(emeraudes)));
    }

    private void ouvrirDepot(Player joueur) {
        Depot depot = new Depot();
        depot.inventaire = Bukkit.createInventory(depot, 27,
                lang.c("depot.titre-coffre", "Déposer des émeraudes"));
        joueur.openInventory(depot.inventaire);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Depot && event.getPlayer() instanceof Player joueur) {
            long points = vider(joueur, event.getInventory());
            if (points >= 0) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (joueur.isOnline()) {
                        message(joueur, lang.c("depot.fait-coffre", "<green><points></green> <white>déposés. Les "
                                + "autres objets t'ont été rendus.", "points", pts(points)));
                    }
                });
            }
        }
    }

    /** Convertit les émeraudes du coffre, rend le reste. Points crédités, ou -1 si le coffre était vide. */
    private long vider(Player joueur, Inventory coffre) {
        long emeraudes = 0;
        boolean vide = true;
        for (ItemStack objet : coffre.getContents()) {
            if (objet == null || objet.getType().isAir()) {
                continue;
            }
            vide = false;
            long valeur = KSEconomy.emeraudesParUnite(objet);
            if (valeur > 0) {
                emeraudes += valeur * objet.getAmount();
            } else {
                for (ItemStack reste : joueur.getInventory().addItem(objet).values()) {
                    joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
                }
            }
        }
        coffre.clear();
        if (vide) {
            return -1;
        }
        long points = pointsPour(emeraudes);
        KSEconomy.crediter(joueur.getUniqueId(), points);
        return points;
    }

    /** Arrêt du serveur : coffres de dépôt ouverts vidés (émeraudes déposées, reste rendu). */
    void fermerDepots() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            Inventory haut = joueur.getOpenInventory().getTopInventory();
            if (haut.getHolder() instanceof Depot) {
                vider(joueur, haut);
                joueur.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------ retrait

    private static ItemStack objetRetrait(String id) {
        return switch (id) {
            case "emeraude" -> new ItemStack(Material.EMERALD);
            case "bloc" -> new ItemStack(Material.EMERALD_BLOCK);
            default -> KSEconomy.creerBloc(Integer.parseInt(id.substring(4)));
        };
    }

    /**
     * 1.0.2 : noms courts (le texte d'un bouton ou d'un champ ne doit jamais défiler, demande de LeKiwi06) ; les
     * valeurs sont dans le texte du menu. Nouvelles clés : les anciennes, plus longues, restent dans lang.yml.
     */
    private Component nomRetrait(String id) {
        return switch (id) {
            case "emeraude" -> lang.c("retrait.nom-emeraude", "Émeraude");
            case "bloc" -> lang.c("retrait.nom-bloc", "Bloc d'émeraude");
            default -> lang.c("retrait.nom-tier", "Compressé tier <tier>", "tier", id.substring(4));
        };
    }

    private void ouvrirRetrait(Player joueur) {
        List<Component> noms = new ArrayList<>();
        RETRAITS.forEach(id -> noms.add(nomRetrait(id)));
        List<DialogInput> champs = List.of(
                gui.choice("objet", lang.c("retrait.champ-objet", "Objet"), RETRAITS, noms, "emeraude"),
                gui.text("nombre", lang.c("retrait.nombre", "Nombre"), "1", 7));
        List<ActionButton> boutons = List.of(gui.form(lang.c("retrait.valider", "<yellow>Retirer"), null,
                (p, vue) -> retirer(p, vue.getText("objet"), vue.getText("nombre"))));
        gui.open(joueur, lang.c("retrait.titre", "<yellow><bold>Retirer"),
                List.of(lang.c("menu.solde", "<white>Solde : <green><bold><solde></bold>", "solde",
                        pts(KSEconomy.solde(joueur.getUniqueId()))),
                        lang.c("retrait.valeurs", "<gray>Valeur en points : émeraude 1, bloc d'émeraude 9, compressé "
                                + "tier 1 : 90, tier 2 : 900, tier 3 : 9 000, tier 4 : 90 000, tier 5 : 900 000, "
                                + "tier 6 : 9 000 000.")),
                champs, boutons, gui.button(lang.c("gui.retour", "<gray>Retour"), null, this::ouvrir), 1);
        lang.saveIfNeeded();
    }

    private void retirer(Player joueur, String id, String texteNombre) {
        if (id == null || !RETRAITS.contains(id)) {
            ouvrirRetrait(joueur);
            return;
        }
        int nombre;
        try {
            nombre = Integer.parseInt(texteNombre == null ? "" : texteNombre.replaceAll("\\s", ""));
        } catch (NumberFormatException e) {
            nombre = 0;
        }
        if (nombre < 1) {
            message(joueur, lang.c("retrait.nombre-invalide", "<red>Nombre invalide."));
            return;
        }
        ItemStack modele = objetRetrait(id);
        long emeraudes = KSEconomy.emeraudesParUnite(modele) * nombre;
        long cout = (long) Math.ceil(emeraudes * plugin.tauxRetrait());
        if (KSEconomy.solde(joueur.getUniqueId()) < cout) {
            message(joueur, lang.c("retrait.solde", "<red>Solde insuffisant : il faut <cout>.", "cout", pts(cout)));
            return;
        }
        if (place(joueur, modele) < nombre) {
            message(joueur, lang.c("retrait.plein", "<red>Pas assez de place dans ton inventaire."));
            return;
        }
        if (!KSEconomy.debiter(joueur.getUniqueId(), cout)) {
            message(joueur, lang.c("retrait.solde", "<red>Solde insuffisant : il faut <cout>.", "cout", pts(cout)));
            return;
        }
        int reste = nombre;
        while (reste > 0) {
            ItemStack pile = modele.clone();
            pile.setAmount(Math.min(reste, modele.getMaxStackSize()));
            reste -= pile.getAmount();
            joueur.getInventory().addItem(pile);
        }
        message(joueur, lang.c("retrait.fait", "<white><nombre> x <objet> retiré(s) pour <yellow><cout></yellow>.",
                "nombre", nombre, "objet", nomRetrait(id), "cout", pts(cout)));
    }

    /** Nombre de ces objets que l'inventaire (hors armure et main secondaire) peut encore recevoir. */
    static int place(Player joueur, ItemStack modele) {
        int place = 0;
        for (ItemStack objet : joueur.getInventory().getStorageContents()) {
            if (objet == null || objet.getType().isAir()) {
                place += modele.getMaxStackSize();
            } else if (objet.isSimilar(modele)) {
                place += Math.max(0, objet.getMaxStackSize() - objet.getAmount());
            }
        }
        return place;
    }
}
