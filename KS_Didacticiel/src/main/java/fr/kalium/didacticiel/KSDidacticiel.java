package fr.kalium.didacticiel;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Didacticiel de Kixster (demande de LeKiwi06, 18/10/2026 : « un didacticiel qui sera dans le menu information sur
 * kixster, ce didacticiel permet d'expliquer tous les changements ; il faudra donc le mettre à jour régulièrement »).
 *
 * Bouton « Didacticiel » dans le comparateur « Informations » de KLM_Menu (et /didacticiel). Menu en coffre : un objet
 * par thème, puis un objet par nouveauté, l'explication dans sa description. Tout le contenu est dans
 * plugins/KS_Didacticiel/didacticiel.yml (copie de référence dans le dépôt) : une mise à jour = envoyer le fichier,
 * puis /didacticiel recharger, sans recompiler ni redémarrer.
 *
 * « Nouveau » (choix de LeKiwi06 : « jusqu'à ce que la personne l'ait ouvert pour la première fois ») : une nouveauté
 * dont la valeur « nouveau » n'a pas encore été vue par ce joueur porte la marque (et son thème aussi) ; ouvrir le thème
 * enregistre la valeur vue (vus.yml). Changer la valeur « nouveau » d'une nouveauté la remet en « Nouveau » pour tous.
 *
 * 1.2.0 (LeKiwi06, 18/10/2026) : une nouveauté peut porter des recettes (« recettes » : clés de recettes du serveur,
 * « ks_crafts:red_sand », ou un début suivi de « * ») : un clic montre ces recettes, une par page ; un thème ou une
 * nouveauté peut demander un plugin (« requiert : KS_Trains », ou « KS_Decapitator 1.3.0 » pour une version minimale) :
 * sans lui, il n'est pas montré.
 */
public final class KSDidacticiel extends JavaPlugin {

    /** Plus de 45 objets ne tiennent pas au-dessus de la barre d'un coffre de 6 rangées. */
    private static final int MAX = Contenant.PAR_PAGE;
    private static final Object MARQUE_THEMES = "ks_didacticiel:themes";

    record Nouveaute(String id, String nom, Material icone, String texte, String commande, String nouveau,
                     List<String> recettes, String requiert) {
    }

    record Theme(String id, String nom, Material icone, String resume, List<Nouveaute> nouveautes, String requiert) {
    }

    /** 1.2.0 : cases de la grille d'établi dans le menu des recettes (3 x 3, colonnes 1 à 3, rangées 1 à 3). */
    private static final int[] GRILLE = {10, 11, 12, 19, 20, 21, 28, 29, 30};

    private Lang lang;
    private String titre = "Didacticiel";
    private List<Theme> themes = List.of();
    private File fichierVus;
    private YamlConfiguration vus;
    private boolean enregistrementPrevu;

    @Override
    public void onEnable() {
        lang = new Lang(this);
        charger();
        fichierVus = new File(getDataFolder(), "vus.yml");
        vus = YamlConfiguration.loadConfiguration(fichierVus);
        Component titreSection = lang.c("section.titre", "<gold><bold>Didacticiel");
        Component description = lang.c("section.description", "<gray>Tout ce qui change sur Kixster, thème par thème.");
        getServer().getServicesManager().register(MenuSection.class, new MenuSection() {
            @Override
            public String id() {
                return "didacticiel";
            }

            @Override
            public Plugin owner() {
                return KSDidacticiel.this;
            }

            @Override
            public Component title() {
                return titreSection;
            }

            @Override
            public Component description() {
                return description;
            }

            @Override
            public boolean informations() {
                return true;
            }

            /** Avant « Contacts » (50) : premier bouton de joueur après les classements et les récompenses. */
            @Override
            public int order() {
                return 10;
            }

            @Override
            public void open(Player joueur, Consumer<Player> retour) {
                ouvrirThemes(joueur, retour);
            }
        }, this, ServicePriority.Normal);
        prendreHelp();
        getServer().getScheduler().runTask(this, this::prendreHelp);
        lang.saveIfNeeded();
    }

    /**
     * 1.1.0 (LeKiwi06, 18/10/2026 : « /help et /didacticiel (ouvre le didacticiel) ») : /help existe déjà (aide du
     * serveur), et un alias de plugin.yml perd contre une commande existante. Le nom est donc repris dans la table des
     * commandes, au démarrage puis une fois le serveur prêt ; l'aide d'origine reste sous bukkit:help et minecraft:help.
     */
    private void prendreHelp() {
        try {
            getServer().getCommandMap().getKnownCommands().put("help", getCommand("didacticiel"));
        } catch (RuntimeException e) {
            getLogger().warning("/help non repris (" + e.getMessage() + ") : /didacticiel marche.");
        }
    }

    @Override
    public void onDisable() {
        if (vus != null) {
            enregistrerVus();
        }
    }

    // ------------------------------------------------------------------ contenu (didacticiel.yml)

    /** Lit didacticiel.yml (créé depuis le jar s'il manque) ; renvoie le bilan (nombre de thèmes et de nouveautés). */
    private String charger() {
        File fichier = new File(getDataFolder(), "didacticiel.yml");
        if (!fichier.exists()) {
            saveResource("didacticiel.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        List<String> avertissements = new ArrayList<>();
        List<Theme> lus = new ArrayList<>();
        ConfigurationSection sectionThemes = yml.getConfigurationSection("themes");
        if (sectionThemes != null) {
            for (String id : sectionThemes.getKeys(false)) {
                ConfigurationSection t = sectionThemes.getConfigurationSection(id);
                if (t == null) {
                    continue;
                }
                List<Nouveaute> nouveautes = new ArrayList<>();
                ConfigurationSection sectionNouveautes = t.getConfigurationSection("nouveautes");
                if (sectionNouveautes != null) {
                    for (String nid : sectionNouveautes.getKeys(false)) {
                        ConfigurationSection n = sectionNouveautes.getConfigurationSection(nid);
                        if (n == null) {
                            continue;
                        }
                        nouveautes.add(new Nouveaute(nid, n.getString("nom", nid),
                                icone(n.getString("icone"), id + "." + nid, avertissements), n.getString("texte", ""),
                                n.getString("commande", ""), n.getString("nouveau", ""),
                                List.copyOf(n.getStringList("recettes")), n.getString("requiert", "")));
                    }
                }
                if (nouveautes.size() > MAX) {
                    avertissements.add("thème " + id + " : " + nouveautes.size() + " nouveautés, seules les " + MAX
                            + " premières sont affichées");
                    nouveautes = nouveautes.subList(0, MAX);
                }
                lus.add(new Theme(id, t.getString("nom", id), icone(t.getString("icone"), id, avertissements),
                        t.getString("resume", ""), List.copyOf(nouveautes), t.getString("requiert", "")));
            }
        }
        if (lus.size() > MAX) {
            avertissements.add(lus.size() + " thèmes, seuls les " + MAX + " premiers sont affichés");
            lus = lus.subList(0, MAX);
        }
        titre = yml.getString("titre", "Didacticiel");
        themes = List.copyOf(lus);
        avertissements.forEach(a -> getLogger().warning("didacticiel.yml : " + a));
        int total = themes.stream().mapToInt(t -> t.nouveautes().size()).sum();
        String bilan = themes.size() + " thèmes, " + total + " nouveautés"
                + (avertissements.isEmpty() ? "" : ", " + avertissements.size() + " avertissement(s) dans la console");
        getLogger().info("Didacticiel chargé : " + bilan);
        return bilan;
    }

    /** Icône d'un thème ou d'une nouveauté : nom d'objet du jeu (ex. EMERALD) ; inconnu = livre, avec un avertissement. */
    private static Material icone(String nom, String ou, List<String> avertissements) {
        Material materiau = nom == null ? null : Material.matchMaterial(nom);
        if (materiau == null || !materiau.isItem() || materiau.isAir()) {
            avertissements.add(ou + " : icône « " + nom + " » inconnue, livre à la place");
            return Material.BOOK;
        }
        return materiau;
    }

    // ------------------------------------------------------------------ plugins demandés (1.2.0)

    /** « requiert » : vide = toujours ; « Plugin » = activé ; « Plugin 1.3.0 » = activé, en 1.3.0 ou plus. */
    private boolean requis(String exigence) {
        if (exigence == null || exigence.isBlank()) {
            return true;
        }
        String[] parties = exigence.trim().split("\\s+");
        Plugin plugin = getServer().getPluginManager().getPlugin(parties[0]);
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        return parties.length < 2 || comparer(plugin.getPluginMeta().getVersion(), parties[1]) >= 0;
    }

    private static int comparer(String a, String b) {
        String[] x = a.split("[.-]");
        String[] y = b.split("[.-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int d = Integer.compare(nombre(x, i), nombre(y, i));
            if (d != 0) {
                return d;
            }
        }
        return 0;
    }

    private static int nombre(String[] parties, int i) {
        try {
            return i < parties.length ? Integer.parseInt(parties[i]) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Nouveautés montrées sur ce serveur (celles dont le plugin demandé est là). */
    private List<Nouveaute> visibles(Theme theme) {
        return theme.nouveautes().stream().filter(n -> requis(n.requiert())).toList();
    }

    /** Thèmes montrés : plugin demandé présent et au moins une nouveauté montrée. */
    private List<Theme> themesVisibles() {
        return themes.stream().filter(t -> requis(t.requiert()) && !visibles(t).isEmpty()).toList();
    }

    // ------------------------------------------------------------------ « Nouveau » (vus.yml)

    private boolean nonLue(Player joueur, Theme theme, Nouveaute nouveaute) {
        return !nouveaute.nouveau().isBlank() && !nouveaute.nouveau().equals(
                vus.getString(joueur.getUniqueId() + "." + theme.id() + "." + nouveaute.id()));
    }

    private int nonLues(Player joueur, Theme theme) {
        return (int) visibles(theme).stream().filter(n -> nonLue(joueur, theme, n)).count();
    }

    /** Le joueur a ouvert ce thème : toutes ses nouveautés sont vues, dans leur état actuel. */
    private void marquerLues(Player joueur, Theme theme) {
        boolean change = false;
        for (Nouveaute n : visibles(theme)) {
            if (nonLue(joueur, theme, n)) {
                vus.set(joueur.getUniqueId() + "." + theme.id() + "." + n.id(), n.nouveau());
                change = true;
            }
        }
        if (change && !enregistrementPrevu) {
            enregistrementPrevu = true;
            getServer().getScheduler().runTaskLater(this, this::enregistrerVus, 100L);
        }
    }

    private void enregistrerVus() {
        enregistrementPrevu = false;
        try {
            vus.save(fichierVus);
        } catch (IOException e) {
            getLogger().warning("vus.yml non enregistré : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ menus

    /** Écran 1 : un objet par thème. {@code retour} : vers « Informations » (null si ouvert par /didacticiel). */
    private void ouvrirThemes(Player joueur, Consumer<Player> retour) {
        List<Theme> themes = themesVisibles();
        int rangees = Math.max(1, (themes.size() + 8) / 9) + 1;
        Contenant menu = Contenant.pour(joueur, rangees, Component.text(titre, NamedTextColor.DARK_GRAY), MARQUE_THEMES);
        if (themes.isEmpty()) {
            menu.poser(4, Contenant.objet(Material.PAPER, lang.c("vide", "<gray>Le didacticiel est vide pour l'instant."),
                    List.of()), null);
        }
        for (int i = 0; i < themes.size(); i++) {
            Theme theme = themes.get(i);
            int nouvelles = nonLues(joueur, theme);
            List<Component> lignes = new ArrayList<>(Contenant.lignes(theme.resume(), NamedTextColor.GRAY));
            lignes.add(lang.c("theme.compte", "<gray><n> nouveauté(s)", "n", visibles(theme).size()));
            if (nouvelles > 0) {
                lignes.add(lang.c("theme.nouveau", "<green><bold>Nouveau : <n> à découvrir", "n", nouvelles));
            }
            lignes.add(lang.c("theme.clic", "<yellow>Clic : voir le détail"));
            ItemStack objet = Contenant.objet(theme.icone(), Component.text(theme.nom(), NamedTextColor.GOLD), lignes);
            menu.poser(i, briller(objet, nouvelles > 0), p -> ouvrirTheme(p, theme.id(), retour));
        }
        menu.barre();
        menu.poser(menu.bas(3), Contenant.objet(Material.BOOK, lang.c("aide.titre", "<gold>Comment ça marche"), List.of(
                lang.c("aide.1", "<gray>Clique sur un thème pour voir"),
                lang.c("aide.2", "<gray>ses nouveautés, puis passe la"),
                lang.c("aide.3", "<gray>souris sur chaque objet pour"),
                lang.c("aide.4", "<gray>lire son explication."),
                lang.c("aide.5", "<green>Nouveau<gray> : pas encore lu."))), null);
        if (retour != null) {
            menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, lang.c("retour", "<yellow>Retour"),
                    List.of(lang.c("retour-informations", "<gray>Vers « Informations »"))),
                    p -> Contenant.saisie(p, () -> retour.accept(p)));
        } else {
            menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, lang.c("fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
        }
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Écran 2 : un objet par nouveauté du thème ; ouvrir le thème le marque comme lu pour ce joueur. */
    private void ouvrirTheme(Player joueur, String id, Consumer<Player> retour) {
        Theme theme = themes.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
        if (theme == null) { // contenu rechargé entre-temps
            ouvrirThemes(joueur, retour);
            return;
        }
        List<Nouveaute> nouveautes = visibles(theme);
        int rangees = Math.max(1, (nouveautes.size() + 8) / 9) + 1;
        Contenant menu = Contenant.pour(joueur, rangees, Component.text(theme.nom(), NamedTextColor.DARK_GRAY),
                "ks_didacticiel:theme:" + theme.id());
        Map<String, Recipe> toutes = nouveautes.stream().anyMatch(n -> !n.recettes().isEmpty()) ? recettesDuServeur()
                : Map.of();
        for (int i = 0; i < nouveautes.size(); i++) {
            Nouveaute n = nouveautes.get(i);
            boolean nouvelle = nonLue(joueur, theme, n);
            List<Component> lignes = new ArrayList<>();
            if (nouvelle) {
                lignes.add(lang.c("nouveaute.nouveau", "<green><bold>Nouveau"));
            }
            lignes.addAll(Contenant.lignes(n.texte(), NamedTextColor.GRAY));
            if (!n.commande().isBlank()) {
                lignes.add(lang.c("nouveaute.commande", "<yellow>Commande : <commande>", "commande", n.commande()));
            }
            // 1.2.0 : une nouveauté qui porte des recettes s'ouvre sur elles.
            int nombreRecettes = recettes(n, toutes).size();
            if (nombreRecettes > 0) {
                lignes.add(lang.c("nouveaute.recettes", "<yellow>Clic : voir les recettes (<n>)", "n", nombreRecettes));
            }
            ItemStack objet = Contenant.objet(n.icone(), Component.text(n.nom(), NamedTextColor.GOLD), lignes);
            menu.poser(i, briller(objet, nouvelle), nombreRecettes == 0 ? null
                    : p -> ouvrirRecettes(p, theme.id(), n.id(), 0, retour));
        }
        menu.barre();
        menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, lang.c("retour", "<yellow>Retour"),
                List.of(lang.c("retour-themes", "<gray>Vers la liste des thèmes"))), p -> ouvrirThemes(p, retour));
        menu.ouvrir(joueur);
        marquerLues(joueur, theme);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ recettes (1.2.0)

    /** Recettes du serveur qui ont une clé, par clé (« ks_crafts:red_sand »). */
    private Map<String, Recipe> recettesDuServeur() {
        Map<String, Recipe> toutes = new LinkedHashMap<>();
        Iterator<Recipe> it = getServer().recipeIterator();
        while (it.hasNext()) {
            Recipe recette = it.next();
            if (recette instanceof Keyed cle) {
                toutes.put(cle.getKey().toString(), recette);
            }
        }
        return toutes;
    }

    /** Recettes d'une nouveauté, dans l'ordre de sa liste (« ks_crafts:coral_* » : toutes celles qui commencent ainsi). */
    private static List<Recipe> recettes(Nouveaute n, Map<String, Recipe> toutes) {
        Map<String, Recipe> r = new LinkedHashMap<>();
        for (String motif : n.recettes()) {
            String m = motif.trim().toLowerCase(Locale.ROOT);
            if (m.contains("*")) {
                // 1.2.1 : « * » n'importe où (« ks_crafts:*_armure ») : n'importe quelle suite de caractères.
                StringBuilder motifRegex = new StringBuilder();
                for (String partie : m.split("\\*", -1)) {
                    if (motifRegex.length() > 0) {
                        motifRegex.append(".*");
                    }
                    motifRegex.append(java.util.regex.Pattern.quote(partie));
                }
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(motifRegex.toString());
                toutes.forEach((cle, recette) -> {
                    if (p.matcher(cle).matches()) {
                        r.putIfAbsent(cle, recette);
                    }
                });
            } else if (toutes.containsKey(m)) {
                r.putIfAbsent(m, toutes.get(m));
            }
        }
        return List.copyOf(r.values());
    }

    /** Une recette par page : la grille d'établi (ou l'entrée du four), puis le résultat ; pages et retour en bas. */
    private void ouvrirRecettes(Player joueur, String idTheme, String idNouveaute, int page, Consumer<Player> retour) {
        Theme theme = themes.stream().filter(t -> t.id().equals(idTheme)).findFirst().orElse(null);
        Nouveaute n = theme == null ? null
                : theme.nouveautes().stream().filter(x -> x.id().equals(idNouveaute)).findFirst().orElse(null);
        List<Recipe> liste = n == null ? List.of() : recettes(n, recettesDuServeur());
        if (liste.isEmpty()) {
            ouvrirThemes(joueur, retour);
            return;
        }
        int p = Math.max(0, Math.min(page, liste.size() - 1));
        Recipe recette = liste.get(p);
        Contenant menu = Contenant.pour(joueur, 6, Component.text(n.nom(), NamedTextColor.DARK_GRAY),
                "ks_didacticiel:recettes:" + idTheme + ":" + idNouveaute);
        for (int i = 0; i < 45; i++) {
            menu.poser(i, Contenant.decor(), null);
        }
        for (int place : GRILLE) {
            menu.poser(place, null, null);
        }
        Material poste = Material.CRAFTING_TABLE;
        Component texte = lang.c("recettes.etabli", "<gray>À l'établi");
        if (recette instanceof ShapedRecipe forme) {
            String[] rangs = forme.getShape();
            Map<Character, RecipeChoice> choix = forme.getChoiceMap();
            // 1.2.1 : une lettre répétée autant de fois que son choix a d'objets (wagon contrôleur : 5 cases, 5 pièces
            // différentes, chacune une fois) montre un objet différent dans chaque case.
            Map<Character, Integer> occurrences = new java.util.HashMap<>();
            for (String rang : rangs) {
                for (char lettre : rang.toCharArray()) {
                    occurrences.merge(lettre, 1, Integer::sum);
                }
            }
            Map<Character, Integer> vues = new java.util.HashMap<>();
            for (int l = 0; l < rangs.length && l < 3; l++) {
                for (int c = 0; c < rangs[l].length() && c < 3; c++) {
                    char lettre = rangs[l].charAt(c);
                    RecipeChoice ch = choix.get(lettre);
                    if (ch == null) {
                        continue;
                    }
                    int rang = vues.merge(lettre, 1, Integer::sum) - 1;
                    List<ItemStack> objets = objets(ch);
                    if (occurrences.get(lettre) > 1 && objets.size() == occurrences.get(lettre)) {
                        menu.poser(GRILLE[l * 3 + c], objets.get(rang), null);
                    } else {
                        menu.poser(GRILLE[l * 3 + c], ingredient(ch), null);
                    }
                }
            }
        } else if (recette instanceof ShapelessRecipe sansForme) {
            List<RecipeChoice> choix = sansForme.getChoiceList();
            for (int i = 0; i < choix.size() && i < 9; i++) {
                menu.poser(GRILLE[i], ingredient(choix.get(i)), null);
            }
            texte = lang.c("recettes.sans-forme", "<gray>À l'établi, dans n'importe quel ordre");
        } else if (recette instanceof CookingRecipe<?> cuisson) {
            menu.poser(GRILLE[4], ingredient(cuisson.getInputChoice()), null);
            poste = recette instanceof BlastingRecipe ? Material.BLAST_FURNACE : Material.FURNACE;
            texte = recette instanceof BlastingRecipe ? lang.c("recettes.haut-fourneau", "<gray>Au haut fourneau")
                    : lang.c("recettes.four", "<gray>Au four");
        }
        menu.poser(23, Contenant.objet(poste, texte, List.of()), null);
        menu.poser(25, recette.getResult().clone(), null);
        menu.barre();
        if (p > 0) {
            menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, lang.c("recettes.precedente", "<yellow>Recette "
                    + "précédente"), List.of()), j -> ouvrirRecettes(j, idTheme, idNouveaute, p - 1, retour));
        }
        if (p < liste.size() - 1) {
            menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, lang.c("recettes.suivante", "<yellow>Recette "
                    + "suivante"), List.of()), j -> ouvrirRecettes(j, idTheme, idNouveaute, p + 1, retour));
        }
        menu.poser(menu.bas(4), Contenant.objet(Material.PAPER, lang.c("recettes.page", "<gray>Recette <n> sur <total>",
                "n", p + 1, "total", liste.size()), List.of()), null);
        menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, lang.c("retour", "<yellow>Retour"),
                List.of(lang.c("retour-theme", "<gray>Vers le thème"))), j -> ouvrirTheme(j, idTheme, retour));
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Un ingrédient : l'objet de la recette ; s'il y en a plusieurs au choix, le premier, avec « Au choix ». */
    private ItemStack ingredient(RecipeChoice choix) {
        List<ItemStack> objets = objets(choix);
        if (objets.isEmpty()) {
            return null;
        }
        ItemStack objet = objets.get(0);
        int variantes = objets.size();
        if (variantes > 1) {
            ItemMeta meta = objet.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
                lore.add(lang.c("recettes.variantes", "<gray>Ou une de ses <n> variantes", "n", variantes)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                meta.lore(lore);
                objet.setItemMeta(meta);
            }
        }
        return objet;
    }

    /**
     * 1.2.2 : les objets acceptés par un choix de recette, dans l'ordre. Une recette relue sur un serveur 26.3 ne rend
     * plus ses objets en MaterialChoice mais en ItemTypeChoice (lu dans CraftRecipe.toBukkit) : sans ce cas, seul le
     * premier objet était montré (wagon contrôleur : 5 comparateurs).
     */
    private static List<ItemStack> objets(RecipeChoice choix) {
        List<ItemStack> objets = new ArrayList<>();
        if (choix instanceof RecipeChoice.ItemTypeChoice types) {
            for (io.papermc.paper.registry.TypedKey<org.bukkit.inventory.ItemType> cle : types.itemTypes().values()) {
                Material materiau = Material.matchMaterial(cle.key().asString());
                if (materiau != null && materiau.isItem()) {
                    objets.add(new ItemStack(materiau));
                }
            }
        } else if (choix instanceof RecipeChoice.MaterialChoice materiaux) {
            materiaux.getChoices().forEach(m -> objets.add(new ItemStack(m)));
        } else if (choix instanceof RecipeChoice.ExactChoice exacts) {
            exacts.getChoices().forEach(o -> objets.add(o.clone()));
        }
        if (objets.isEmpty()) {
            try {
                objets.add(choix.getItemStack().clone());
            } catch (RuntimeException e) {
                // choix vide : rien à montrer
            }
        }
        return objets;
    }

    /** Une nouveauté non lue (ou un thème qui en contient) brille dans le menu. */
    private static ItemStack briller(ItemStack objet, boolean oui) {
        if (oui) {
            ItemMeta meta = objet.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                objet.setItemMeta(meta);
            }
        }
        return objet;
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("recharger")) {
            if (!sender.hasPermission("ksdidacticiel.admin")) {
                sender.sendMessage(lang.c("recharger.refus", "<red>Tu n'as pas la permission de recharger le didacticiel."));
                return true;
            }
            String bilan = charger();
            sender.sendMessage(lang.c("recharger.ok", "<green>Didacticiel rechargé : <bilan>.", "bilan", bilan));
            lang.saveIfNeeded();
            return true;
        }
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Le didacticiel s'ouvre en jeu ; depuis la console : /didacticiel recharger.");
            return true;
        }
        ouvrirThemes(joueur, null);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && sender.hasPermission("ksdidacticiel.admin") && "recharger".startsWith(
                args[0].toLowerCase()) ? List.of("recharger") : List.of();
    }
}
