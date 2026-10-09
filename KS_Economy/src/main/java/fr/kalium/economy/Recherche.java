package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1.2.0 - recherche dans les boutiques (LeKiwi06 : « une barre de recherche pour trouver des items en particulier ou
 * par catégorie avec des filtres ») : nom français ou id de l'objet (facultatif), catégorie, type de prix (points,
 * objet), en stock seulement, tri (plus vendus, moins cher, plus cher, nom). Boutiques des autres joueurs seulement.
 *
 * 1.5.0 (LeKiwi06, 09/10/2026) : un seul coffre : les boutiques trouvées en haut, les filtres dessous (un clic change
 * le filtre, la liste suit) ; seul le nom de l'objet se saisit (fenêtre de Gui).
 */
final class Recherche {

    static final List<String> CATEGORIES = List.of("toutes", "speciaux", "potions", "armures", "outils", "nourriture",
            "ressources", "redstone", "blocs", "autres");
    private static final List<String> NOMS_CATEGORIES = List.of("Toutes", "Objets spéciaux", "Potions et élixirs",
            "Armures", "Outils et armes", "Nourriture", "Minerais et ressources", "Redstone", "Blocs", "Autres");

    /** Critères d'une recherche (gardés pour « Modifier la recherche »). */
    record Criteres(String texte, String categorie, String prix, boolean enStock, String tri) {
        static Criteres parDefaut() {
            return new Criteres("", "toutes", "tous", true, "vendus");
        }
    }

    private final KSEconomy plugin;
    private final Magasins magasins;
    private final Boutiques boutiques;
    private final Lang lang;
    private final Gui gui;
    private final Menus menus;

    Recherche(KSEconomy plugin, Magasins magasins, Boutiques boutiques) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.boutiques = boutiques;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.menus = plugin.menus();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ menu

    /** Résultats par page : 4 rangées ; la 5e porte les filtres. */
    private static final int PAR_PAGE = 36;

    private static final List<Material> ICONES_CATEGORIES = List.of(Material.CHEST, Material.NETHER_STAR,
            Material.BREWING_STAND, Material.IRON_CHESTPLATE, Material.IRON_PICKAXE, Material.BREAD, Material.DIAMOND,
            Material.REDSTONE, Material.BRICKS, Material.FEATHER);
    private static final List<String> PRIX = List.of("tous", "points", "objet");
    private static final List<String> NOMS_PRIX = List.of("Tous", "En points", "En objet");
    private static final List<String> TRIS = List.of("vendus", "moins-cher", "plus-cher", "nom");
    private static final List<String> NOMS_TRIS = List.of("Plus vendus", "Moins cher (points)", "Plus cher (points)",
            "Nom");

    private static String nom(List<String> ids, List<String> noms, String id) {
        int i = ids.indexOf(id);
        return noms.get(Math.max(0, i));
    }

    private static String suivant(List<String> ids, String id) {
        return ids.get((ids.indexOf(id) + 1) % ids.size());
    }

    /** 1.5.0 : la recherche et ses résultats sont un seul coffre (voir {@link #resultats}). */
    void ouvrir(Player joueur, Criteres c) {
        resultats(joueur, c, 0);
    }

    /**
     * 1.5.0 : coffre ; en haut les boutiques trouvées (l'objet vendu, 36 par page), dessous les filtres : un clic sur
     * un filtre le change et la liste suit. Seul le nom de l'objet se saisit (fenêtre de Gui).
     */
    private void resultats(Player joueur, Criteres c, int page) {
        List<Boutique> trouves = chercher(joueur, c);
        int pages = Math.max(1, (trouves.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant menu = menus.menu(joueur, 6, t("recherche.titre-coffre", "<dark_gray>Rechercher un objet"),
                "recherche");
        for (int i = p * PAR_PAGE; i < Math.min(trouves.size(), (p + 1) * PAR_PAGE); i++) {
            Boutique b = trouves.get(i);
            menu.poser(i - p * PAR_PAGE, boutiques.icone(b,
                    t("recherche.magasin", "<gray>Magasin : <white><nom>", "nom", nomMagasin(b)),
                    t("catalogue.clic-acheter", "<dark_gray>Clic : acheter")),
                    q -> boutiques.ouvrirAchat(q, b, r -> resultats(r, c, p)));
        }
        if (trouves.isEmpty()) {
            menu.poser(13, Menus.objet(Material.PAPER, t("recherche.aucune", "<gray>Aucune boutique trouvée"),
                    t("recherche.aucune-info", "<dark_gray>Change les filtres de la rangée du dessous.")), null);
        }
        // Filtres
        for (int place = PAR_PAGE; place < PAR_PAGE + 9; place++) {
            menu.poser(place, Contenant.decor(), null);
        }
        Component clic = t("recherche.clic-changer", "<dark_gray>Clic : changer");
        menu.poser(37, Contenant.objet(Material.NAME_TAG, c.texte().isEmpty()
                        ? t("recherche.filtre-objet-tous", "<white>Objet : <yellow>tous")
                        : t("recherche.filtre-objet", "<white>Objet : <yellow><texte>", "texte", c.texte()),
                List.of(t("recherche.filtre-objet-info", "<gray>Nom français ou id de l'objet"),
                        t("recherche.clic-ecrire", "<dark_gray>Clic : écrire"))), q -> saisir(q, c));
        int categorie = Math.max(0, CATEGORIES.indexOf(c.categorie()));
        menu.poser(39, Contenant.objet(ICONES_CATEGORIES.get(categorie),
                t("recherche.filtre-categorie", "<white>Catégorie : <yellow><nom>", "nom",
                        NOMS_CATEGORIES.get(categorie)),
                List.of(t("recherche.clic-choisir", "<dark_gray>Clic : choisir"))), q -> categories(q, c));
        menu.poser(40, Contenant.objet("objet".equals(c.prix()) ? Material.CHEST : Material.EMERALD,
                t("recherche.filtre-prix", "<white>Prix : <yellow><nom>", "nom", nom(PRIX, NOMS_PRIX, c.prix())),
                List.of(clic)), q -> resultats(q, new Criteres(c.texte(), c.categorie(), suivant(PRIX, c.prix()),
                c.enStock(), c.tri()), 0));
        menu.poser(41, Contenant.objet(c.enStock() ? Material.LIME_DYE : Material.GRAY_DYE, c.enStock()
                        ? t("recherche.filtre-stock-oui", "<white>En stock seulement : <green>oui")
                        : t("recherche.filtre-stock-non", "<white>En stock seulement : <red>non"),
                List.of(clic)), q -> resultats(q, new Criteres(c.texte(), c.categorie(), c.prix(), !c.enStock(),
                c.tri()), 0));
        menu.poser(43, Contenant.objet(Material.HOPPER,
                t("recherche.filtre-tri", "<white>Tri : <yellow><nom>", "nom", nom(TRIS, NOMS_TRIS, c.tri())),
                List.of(clic)), q -> resultats(q, new Criteres(c.texte(), c.categorie(), c.prix(), c.enStock(),
                suivant(TRIS, c.tri())), 0));
        // Barre d'actions
        menus.pages(menu, p, pages, (q, n) -> resultats(q, c, n));
        menu.poser(menu.bas(4), Contenant.objet(Material.PAPER,
                t("recherche.trouvees", "<white><n> boutique(s) trouvée(s)", "n", trouves.size()),
                List.of(t("contenant.page", "<gray>Page <n> sur <total>", "n", p + 1, "total", pages))), null);
        menus.sortie(menu, q -> plugin.menuMagasin().catalogue(q, 0));
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Choix de la catégorie : un objet par catégorie. */
    private void categories(Player joueur, Criteres c) {
        Contenant menu = menus.menu(joueur, 4, t("recherche.titre-categories", "<dark_gray>Catégorie"),
                "recherche-categories");
        int[] places = {2, 3, 4, 5, 6, 11, 12, 13, 14, 15};
        for (int i = 0; i < CATEGORIES.size() && i < places.length; i++) {
            String id = CATEGORIES.get(i);
            boolean choisie = id.equals(c.categorie());
            menu.poser(places[i], Contenant.objet(ICONES_CATEGORIES.get(i),
                    Component.text(NOMS_CATEGORIES.get(i), choisie ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    choisie ? List.of(t("recherche.categorie-choisie", "<green>Catégorie choisie")) : List.of()),
                    q -> resultats(q, new Criteres(c.texte(), id, c.prix(), c.enStock(), c.tri()), 0));
        }
        menus.sortie(menu, q -> resultats(q, c, 0));
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Nom de l'objet cherché : saisie de texte (fenêtre de Gui) ; vide : tous les objets. */
    private void saisir(Player joueur, Criteres c) {
        ActionButton chercher = gui.form(t("recherche.bouton", "<green>Rechercher"), null, (p, vue) -> {
            String texte = vue.getText("texte") == null ? "" : vue.getText("texte").trim();
            resultats(p, new Criteres(texte, c.categorie(), c.prix(), c.enStock(), c.tri()), 0);
        });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("recherche.titre", "<gold><bold>Rechercher un objet"),
                    List.of(t("recherche.aide-nom", "<gray>Nom français ou id de l'objet. Vide : tous les objets.")),
                    List.of(gui.text("texte", t("recherche.champ-objet", "Objet (nom ou id, facultatif)"), c.texte(),
                            64)),
                    List.of(chercher, gui.button(t("magasin.retour", "<gray>Retour"), null, p -> resultats(p, c, 0))),
                    gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    private String nomMagasin(Boutique b) {
        Magasins.Magasin m = magasins.magasins.get(b.proprio);
        return m == null ? Boutiques.nomJoueur(b.proprio) : m.nom;
    }

    private List<Boutique> chercher(Player joueur, Criteres c) {
        String texte = simplifier(c.texte());
        List<Boutique> r = new ArrayList<>();
        for (Boutique b : magasins.boutiques.values()) {
            if (b.proprio.equals(joueur.getUniqueId())) {
                continue;
            }
            if (!texte.isEmpty() && !simplifier(Boutiques.texteObjet(b.objet)).contains(texte)
                    && !simplifier(b.objet.getType().getKey().getKey()).contains(texte)
                    && !simplifier(Boutiques.nomBoutique(b)).contains(texte)) {
                continue;
            }
            if (!"toutes".equals(c.categorie()) && !c.categorie().equals(categorie(b.objet))) {
                continue;
            }
            if ("points".equals(c.prix()) && !b.enPoints() || "objet".equals(c.prix()) && b.enPoints()) {
                continue;
            }
            if (c.enStock() && boutiques.etat(b) != Boutiques.Etat.OUVERTE) {
                continue;
            }
            r.add(b);
        }
        Map<String, Integer> vendus = plugin.ventes().lotsDeLaSemaineParBoutique();
        Comparator<Boutique> parNom = Comparator.comparing(b -> simplifier(Boutiques.nomBoutique(b)));
        // Prix unitaire en points (les prix en objet ne se comparent pas : après les autres).
        Comparator<Boutique> parPrix = Comparator.comparingDouble(b -> b.enPoints()
                ? (double) b.prixPoints / Math.max(1, b.quantite) : Double.MAX_VALUE);
        switch (c.tri()) {
            case "moins-cher" -> r.sort(parPrix.thenComparing(parNom));
            case "plus-cher" -> r.sort(Comparator.comparingDouble((Boutique b) -> b.enPoints()
                    ? -(double) b.prixPoints / Math.max(1, b.quantite) : Double.MAX_VALUE).thenComparing(parNom));
            case "nom" -> r.sort(parNom);
            default -> r.sort(Comparator.comparingInt((Boutique b) -> -vendus.getOrDefault(b.id, 0)).thenComparing(parNom));
        }
        return r;
    }

    private static String simplifier(String texte) {
        String s = Normalizer.normalize(texte == null ? "" : texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    // ------------------------------------------------------------------ catégories

    /** Catégorie d'un objet (dans l'ordre : spéciaux, potions, armures, outils, nourriture, ressources, redstone...). */
    static String categorie(ItemStack objet) {
        Material m = objet.getType();
        String n = m.name();
        if (objet.hasItemMeta()) {
            for (NamespacedKey cle : objet.getItemMeta().getPersistentDataContainer().getKeys()) {
                if (cle.getNamespace().startsWith("ks_")) {
                    return "speciaux";
                }
            }
        }
        if (n.endsWith("POTION") || m == Material.TIPPED_ARROW || m == Material.DRAGON_BREATH) {
            return "potions";
        }
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || m == Material.ELYTRA || m == Material.SHIELD || n.endsWith("_HORSE_ARMOR") || m == Material.WOLF_ARMOR) {
            return "armures";
        }
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL")
                || n.endsWith("_HOE") || m == Material.BOW || m == Material.CROSSBOW || m == Material.TRIDENT
                || m == Material.MACE || m == Material.SHEARS || m == Material.FISHING_ROD || m == Material.FLINT_AND_STEEL
                || m == Material.BRUSH || n.endsWith("ARROW") || n.endsWith("_SPEAR")) {
            return "outils";
        }
        if (m.isEdible()) {
            return "nourriture";
        }
        if (n.endsWith("_ORE") || n.endsWith("_INGOT") || n.endsWith("_NUGGET") || n.startsWith("RAW_")
                || m == Material.DIAMOND || m == Material.EMERALD || m == Material.LAPIS_LAZULI || m == Material.COAL
                || m == Material.CHARCOAL || m == Material.QUARTZ || m == Material.AMETHYST_SHARD
                || m == Material.NETHERITE_SCRAP || m == Material.ANCIENT_DEBRIS || n.endsWith("_BLOCK")
                && (n.startsWith("DIAMOND") || n.startsWith("EMERALD") || n.startsWith("GOLD") || n.startsWith("IRON")
                || n.startsWith("NETHERITE") || n.startsWith("LAPIS") || n.startsWith("COAL") || n.startsWith("COPPER")
                || n.startsWith("RAW_"))) {
            return "ressources";
        }
        if (n.startsWith("REDSTONE") || m == Material.REPEATER || m == Material.COMPARATOR || n.endsWith("PISTON")
                || m == Material.OBSERVER || m == Material.HOPPER || m == Material.DISPENSER || m == Material.DROPPER
                || m == Material.LEVER || n.endsWith("_BUTTON") || n.endsWith("_PRESSURE_PLATE") || m == Material.TARGET
                || m == Material.DAYLIGHT_DETECTOR || m == Material.TRIPWIRE_HOOK || n.endsWith("RAIL")
                || m == Material.CRAFTER || m == Material.SCULK_SENSOR || m == Material.CALIBRATED_SCULK_SENSOR) {
            return "redstone";
        }
        if (m.isBlock()) {
            return "blocs";
        }
        return "autres";
    }
}
