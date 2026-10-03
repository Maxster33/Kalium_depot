package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
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
 */
final class Recherche {

    private static final int PAR_PAGE = 10;

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

    Recherche(KSEconomy plugin, Magasins magasins, Boutiques boutiques) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.boutiques = boutiques;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ formulaire

    void ouvrir(Player joueur, Criteres c) {
        List<Component> noms = new ArrayList<>();
        for (String n : NOMS_CATEGORIES) {
            noms.add(Component.text(n));
        }
        ActionButton chercher = gui.form(t("recherche.bouton", "<green>Rechercher"), null, (p, vue) -> {
            String texte = vue.getText("texte") == null ? "" : vue.getText("texte").trim();
            Criteres nouveaux = new Criteres(texte, valeur(vue.getText("categorie"), "toutes"),
                    valeur(vue.getText("prix"), "tous"), Boolean.TRUE.equals(vue.getBoolean("stock")),
                    valeur(vue.getText("tri"), "vendus"));
            resultats(p, nouveaux, 0);
        });
        gui.open(joueur, t("recherche.titre", "<gold><bold>Rechercher un objet"),
                List.of(t("recherche.aide", "<gray>Nom français ou id de l'objet (facultatif), puis les filtres.")),
                List.of(gui.text("texte", t("recherche.champ-objet", "Objet (nom ou id, facultatif)"), c.texte(), 64),
                        gui.choice("categorie", t("recherche.champ-categorie", "Catégorie"), CATEGORIES, noms, c.categorie()),
                        gui.choice("prix", t("recherche.champ-prix", "Prix"), List.of("tous", "points", "objet"),
                                List.of(Component.text("Tous"), Component.text("En points"), Component.text("En objet")),
                                c.prix()),
                        gui.toggle("stock", t("recherche.champ-stock", "En stock seulement"), c.enStock()),
                        gui.choice("tri", t("recherche.champ-tri", "Tri"), List.of("vendus", "moins-cher", "plus-cher", "nom"),
                                List.of(Component.text("Plus vendus"), Component.text("Moins cher (points)"),
                                        Component.text("Plus cher (points)"), Component.text("Nom")), c.tri())),
                List.of(chercher, gui.button(t("magasin.retour", "<gray>Retour"), null,
                        p -> plugin.menuMagasin().catalogue(p, 0))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private static String valeur(String v, String defaut) {
        return v == null || v.isBlank() ? defaut : v;
    }

    // ------------------------------------------------------------------ résultats

    private void resultats(Player joueur, Criteres c, int page) {
        List<Boutique> trouves = chercher(joueur, c);
        int pages = Math.max(1, (trouves.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<Component> corps = new ArrayList<>();
        corps.add(t("recherche.nombre", "<white><n> boutique(s) trouvée(s) <gray>(page <page> / <pages>)", "n",
                trouves.size(), "page", p + 1, "pages", pages));
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = p * PAR_PAGE; i < Math.min(trouves.size(), (p + 1) * PAR_PAGE); i++) {
            Boutique b = trouves.get(i);
            corps.add(Component.text(Boutiques.nomBoutique(b) + " : ").append(Boutiques.lot(b.quantite, b.objet))
                    .append(Component.text(" contre ")).append(boutiques.prix(b))
                    .append(Component.text(" - " + nomMagasin(b))));
            boutons.add(gui.button(boutiques.boutonBoutique(b), boutiques.etatTexte(b), q -> boutiques.ouvrirAchat(q, b)));
        }
        if (p > 0) {
            boutons.add(gui.button(t("magasin.precedent", "<yellow>Page précédente"), null, q -> resultats(q, c, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("magasin.suivant", "<yellow>Page suivante"), null, q -> resultats(q, c, p + 1)));
        }
        boutons.add(gui.button(t("recherche.modifier", "<white>Modifier la recherche"), null, q -> ouvrir(q, c)));
        gui.open(joueur, t("recherche.titre-resultats", "<gold><bold>Résultats"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
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
