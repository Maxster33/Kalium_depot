package fr.kalium.economy;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Menu « Économie » (/economie ou bouton de KS_Menu) : solde, tout déposer, déposer des objets (coffre), retirer,
 * masquer / révéler son solde.
 * 1.5.0 : menus en coffres (voir Menus) ; seule la saisie d'un nombre à retirer reste une fenêtre de Gui.
 */
final class MenuEconomie implements Listener {

    /** Objets qu'on peut retirer : id du choix -> (objet, valeur en émeraudes). */
    private static final List<String> RETRAITS = List.of("emeraude", "bloc", "tier1", "tier2", "tier3", "tier4",
            "tier5", "tier6");

    private final KSEconomy plugin;
    private final Lang lang;
    private final Gui gui;
    private final Menus menus;

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
        this.menus = plugin.menus();
    }

    private String pts(long valeur) {
        return KSEconomy.points(valeur);
    }

    private Component ligneSolde(Player joueur) {
        return lang.c("eco.solde", "<white>Solde : <green><solde>", "solde", pts(KSEconomy.solde(joueur.getUniqueId())));
    }

    // ------------------------------------------------------------------ menu principal

    /** 1.5.0 : coffre (solde en haut ; dépôt et retrait à gauche, magasins et rachats au centre, masquage à droite). */
    void ouvrir(Player joueur) {
        boolean masque = plugin.estMasque(joueur.getUniqueId());
        Contenant c = menus.menu(joueur, 5, lang.c("eco.titre", "<dark_gray>Économie"), "economie");
        c.poser(4, Menus.objet(Material.EMERALD_BLOCK, ligneSolde(joueur), masque
                ? lang.c("menu.masque", "<gray>Ton solde est <red>masqué</red> dans la liste des joueurs (Tab) : "
                        + "<pourcent> % prélevé chaque jour à minuit.", "pourcent", pourcent())
                : lang.c("menu.visible", "<gray>Ton solde est <green>visible</green> par les autres joueurs dans la "
                        + "liste des joueurs (Tab).")), null);
        c.poser(19, Menus.objet(Material.HOPPER, lang.c("menu.bouton-tout-deposer", "<green>Tout déposer"),
                lang.c("menu.info-tout-deposer", "<gray>Émeraudes, blocs d'émeraude et blocs compressés de ton "
                        + "inventaire")), this::toutDeposer);
        c.poser(20, Menus.objet(Material.CHEST, lang.c("menu.bouton-deposer", "<green>Déposer des objets"),
                lang.c("menu.info-deposer", "<gray>Pose dans le coffre les émeraudes à déposer")), this::ouvrirDepot);
        c.poser(21, Menus.objet(Material.EMERALD, lang.c("menu.bouton-retirer", "<yellow>Retirer"),
                lang.c("menu.info-retirer", "<gray>Émeraudes, blocs ou blocs compressés")), this::ouvrirRetrait);
        // 1.1.0 : catalogue des magasins.
        c.poser(23, Menus.objet(Material.OAK_HANGING_SIGN, lang.c("menu.bouton-magasins", "<gold>Magasins"),
                lang.c("menu.info-magasins", "<gray>Boutiques des joueurs, achat à distance, ton magasin")),
                p -> plugin.menuMagasin().catalogue(p, 0));
        // 1.3.0 : rachats de la semaine.
        c.poser(24, Menus.objet(Material.GOLD_INGOT, lang.c("menu.bouton-rachats", "<gold>Rachats de la semaine"),
                lang.c("menu.info-rachats", "<gray>10 objets rachetés par le serveur, tirés chaque lundi")),
                p -> plugin.rachats().ouvrir(p));
        if (masque) {
            c.poser(25, Menus.objet(Material.GLASS, lang.c("menu.bouton-reveler", "<aqua>Révéler mon solde"),
                    lang.c("menu.info-reveler", "<gray>Gratuit, immédiat")), p -> {
                plugin.masquer(p, false);
                ouvrir(p);
            });
        } else {
            c.poser(25, Menus.objet(Material.TINTED_GLASS, lang.c("menu.bouton-masquer", "<red>Masquer mon solde"),
                    lang.c("menu.info-masquer", "<gray>Coûte <pourcent> % de ton solde par jour", "pourcent",
                            pourcent())), this::confirmerMasquer);
        }
        menus.sortie(c, null);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private String pourcent() {
        double valeur = plugin.pourcentMasque();
        return valeur == Math.rint(valeur) ? String.valueOf((long) valeur) : String.valueOf(valeur).replace('.', ',');
    }

    private void confirmerMasquer(Player joueur) {
        menus.confirmer(joueur, lang.c("masquer.titre-coffre", "<dark_gray>Masquer mon solde"),
                lang.c("masquer.texte", "<white>Ton solde ne sera plus visible dans la liste des joueurs. Chaque jour à "
                        + "minuit (heure de Paris), <red><pourcent> %</red> de ton solde est prélevé (arrondi à l'unité "
                        + "inférieure). Tu peux le révéler gratuitement à tout moment.", "pourcent", pourcent()),
                p -> {
                    plugin.masquer(p, true);
                    ouvrir(p);
                }, this::ouvrir);
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
            menus.message(joueur, lang.c("depot.rien", "<red>Aucune émeraude dans ton inventaire."), true);
            return;
        }
        joueur.getInventory().setStorageContents(contenu);
        long points = pointsPour(emeraudes);
        KSEconomy.crediter(joueur.getUniqueId(), points);
        ouvrir(joueur);
        menus.message(joueur, lang.c("depot.fait", "<green><points></green> <white>déposés (<emeraudes> émeraudes).",
                "points", pts(points), "emeraudes", KSEconomy.nombre(emeraudes)), false);
    }

    private void ouvrirDepot(Player joueur) {
        Depot depot = new Depot();
        depot.inventaire = Bukkit.createInventory(depot, 27,
                lang.c("depot.titre-coffre", "Déposer des émeraudes"));
        Contenant.apres(() -> joueur.openInventory(depot.inventaire));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Depot && event.getPlayer() instanceof Player joueur) {
            long points = vider(joueur, event.getInventory());
            if (points >= 0) {
                // Retour au menu seulement si le joueur a refermé le coffre lui-même (pas si une autre fenêtre s'ouvre).
                boolean retour = event.getReason() == InventoryCloseEvent.Reason.PLAYER;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (joueur.isOnline()) {
                        if (retour) {
                            ouvrir(joueur);
                        }
                        menus.message(joueur, lang.c("depot.fait-coffre", "<green><points></green> <white>déposés. "
                                + "Les autres objets t'ont été rendus.", "points", pts(points)), false);
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

    /** Coût en points du retrait de ce nombre d'objets (taux de retrait, arrondi à l'unité supérieure). */
    private long cout(ItemStack modele, long nombre) {
        return (long) Math.ceil(KSEconomy.emeraudesParUnite(modele) * nombre * plugin.tauxRetrait());
    }

    /** 1.5.0 : les 8 objets qu'on peut retirer ; un clic ouvre le choix du nombre. */
    private void ouvrirRetrait(Player joueur) {
        Contenant c = menus.menu(joueur, 4, lang.c("retrait.titre-coffre", "<dark_gray>Retirer"), "retrait");
        c.poser(4, Contenant.objet(Material.EMERALD_BLOCK, ligneSolde(joueur), List.of()), null);
        int[] places = {9, 10, 11, 12, 14, 15, 16, 17};
        for (int i = 0; i < RETRAITS.size(); i++) {
            String id = RETRAITS.get(i);
            ItemStack modele = objetRetrait(id);
            c.poser(places[i], Contenant.objet(modele, nomRetrait(id).colorIfAbsent(NamedTextColor.YELLOW), List.of(
                    lang.c("retrait.cout-unite", "<gray>Coût : <white><cout></white> l'unité", "cout",
                            pts(cout(modele, 1))),
                    lang.c("retrait.clic", "<dark_gray>Clic : choisir le nombre"))), p -> nombreRetrait(p, id));
        }
        menus.sortie(c, this::ouvrir);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** 1.5.0 : nombre à retirer : 1, 8, 64, le maximum possible (solde et place), ou un nombre saisi. */
    private void nombreRetrait(Player joueur, String id) {
        if (!RETRAITS.contains(id)) {
            ouvrirRetrait(joueur);
            return;
        }
        ItemStack modele = objetRetrait(id);
        long solde = KSEconomy.solde(joueur.getUniqueId());
        long unite = Math.max(1, cout(modele, 1));
        int max = (int) Math.min(place(joueur, modele), Math.min(solde / unite, 9_999_999));
        while (max > 0 && cout(modele, max) > solde) {
            max--;
        }
        Contenant c = menus.menu(joueur, 4, lang.c("retrait.titre-nombre", "<dark_gray>Retirer : combien ?"),
                "retrait-nombre");
        c.poser(4, Contenant.objet(modele, nomRetrait(id).colorIfAbsent(NamedTextColor.YELLOW), List.of(
                lang.c("retrait.cout-unite", "<gray>Coût : <white><cout></white> l'unité", "cout", pts(unite)),
                ligneSolde(joueur))), null);
        int[] nombres = {1, 8, 64};
        for (int i = 0; i < nombres.length; i++) {
            int n = nombres[i];
            c.poser(10 + i, Contenant.objet(Menus.telQuel(modele, n),
                    lang.c("retrait.bouton-nombre", "<yellow>Retirer <nombre>", "nombre", n),
                    List.of(lang.c("retrait.cout", "<gray>Coût : <white><cout>", "cout", pts(cout(modele, n))))),
                    p -> retirer(p, id, n));
        }
        int tout = max;
        if (tout > 0) {
            c.poser(14, Contenant.objet(Menus.telQuel(modele, tout),
                    lang.c("retrait.bouton-max", "<yellow>Retirer le maximum : <nombre>", "nombre",
                            KSEconomy.nombre(tout)),
                    List.of(lang.c("retrait.cout", "<gray>Coût : <white><cout>", "cout", pts(cout(modele, tout))),
                            lang.c("retrait.max-info", "<dark_gray>Selon ton solde et ta place."))),
                    p -> retirer(p, id, tout));
        }
        c.poser(16, Menus.objet(Material.OAK_SIGN, lang.c("retrait.bouton-autre", "<aqua>Autre nombre"),
                lang.c("retrait.autre-info", "<gray>Écrire le nombre à retirer")), p -> saisirRetrait(p, id));
        menus.sortie(c, this::ouvrirRetrait);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Saisie d'un nombre (petite fenêtre : un coffre ne sait pas faire de saisie). */
    private void saisirRetrait(Player joueur, String id) {
        List<DialogInput> champs = List.of(gui.text("nombre", lang.c("retrait.nombre", "Nombre"), "1", 7));
        List<ActionButton> boutons = List.of(gui.form(lang.c("retrait.valider", "<yellow>Retirer"), null,
                (p, vue) -> {
                    int nombre = Boutiques.entier(vue.getText("nombre"));
                    nombreRetrait(p, id);
                    retirer(p, id, nombre);
                }));
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, lang.c("retrait.titre", "<yellow><bold>Retirer"),
                    List.of(nomRetrait(id), lang.c("menu.solde", "<white>Solde : <green><bold><solde></bold>", "solde",
                            pts(KSEconomy.solde(joueur.getUniqueId())))),
                    champs, boutons, gui.button(lang.c("gui.retour", "<gray>Retour"), null, p -> nombreRetrait(p, id)),
                    1);
            lang.saveIfNeeded();
        });
    }

    private void retirer(Player joueur, String id, int nombre) {
        if (id == null || !RETRAITS.contains(id)) {
            ouvrirRetrait(joueur);
            return;
        }
        if (nombre < 1) {
            menus.message(joueur, lang.c("retrait.nombre-invalide", "<red>Nombre invalide."), true);
            return;
        }
        ItemStack modele = objetRetrait(id);
        long cout = cout(modele, nombre);
        if (KSEconomy.solde(joueur.getUniqueId()) < cout) {
            menus.message(joueur, lang.c("retrait.solde", "<red>Solde insuffisant : il faut <cout>.", "cout",
                    pts(cout)), true);
            return;
        }
        if (place(joueur, modele) < nombre) {
            menus.message(joueur, lang.c("retrait.plein", "<red>Pas assez de place dans ton inventaire."), true);
            return;
        }
        if (!KSEconomy.debiter(joueur.getUniqueId(), cout)) {
            menus.message(joueur, lang.c("retrait.solde", "<red>Solde insuffisant : il faut <cout>.", "cout",
                    pts(cout)), true);
            return;
        }
        int reste = nombre;
        while (reste > 0) {
            ItemStack pile = modele.clone();
            pile.setAmount(Math.min(reste, modele.getMaxStackSize()));
            reste -= pile.getAmount();
            joueur.getInventory().addItem(pile);
        }
        nombreRetrait(joueur, id);
        menus.message(joueur, lang.c("retrait.fait", "<white><nombre> x <objet> retiré(s) pour <yellow><cout></yellow>.",
                "nombre", nombre, "objet", nomRetrait(id), "cout", pts(cout)), false);
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
