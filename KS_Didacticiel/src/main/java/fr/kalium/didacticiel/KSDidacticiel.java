package fr.kalium.didacticiel;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
 */
public final class KSDidacticiel extends JavaPlugin {

    /** Plus de 45 objets ne tiennent pas au-dessus de la barre d'un coffre de 6 rangées. */
    private static final int MAX = Contenant.PAR_PAGE;
    private static final Object MARQUE_THEMES = "ks_didacticiel:themes";

    record Nouveaute(String id, String nom, Material icone, String texte, String commande, String nouveau) {
    }

    record Theme(String id, String nom, Material icone, String resume, List<Nouveaute> nouveautes) {
    }

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
        lang.saveIfNeeded();
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
                                n.getString("commande", ""), n.getString("nouveau", "")));
                    }
                }
                if (nouveautes.size() > MAX) {
                    avertissements.add("thème " + id + " : " + nouveautes.size() + " nouveautés, seules les " + MAX
                            + " premières sont affichées");
                    nouveautes = nouveautes.subList(0, MAX);
                }
                lus.add(new Theme(id, t.getString("nom", id), icone(t.getString("icone"), id, avertissements),
                        t.getString("resume", ""), List.copyOf(nouveautes)));
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

    // ------------------------------------------------------------------ « Nouveau » (vus.yml)

    private boolean nonLue(Player joueur, Theme theme, Nouveaute nouveaute) {
        return !nouveaute.nouveau().isBlank() && !nouveaute.nouveau().equals(
                vus.getString(joueur.getUniqueId() + "." + theme.id() + "." + nouveaute.id()));
    }

    private int nonLues(Player joueur, Theme theme) {
        return (int) theme.nouveautes().stream().filter(n -> nonLue(joueur, theme, n)).count();
    }

    /** Le joueur a ouvert ce thème : toutes ses nouveautés sont vues, dans leur état actuel. */
    private void marquerLues(Player joueur, Theme theme) {
        boolean change = false;
        for (Nouveaute n : theme.nouveautes()) {
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
            lignes.add(lang.c("theme.compte", "<gray><n> nouveauté(s)", "n", theme.nouveautes().size()));
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
        int rangees = Math.max(1, (theme.nouveautes().size() + 8) / 9) + 1;
        Contenant menu = Contenant.pour(joueur, rangees, Component.text(theme.nom(), NamedTextColor.DARK_GRAY),
                "ks_didacticiel:theme:" + theme.id());
        for (int i = 0; i < theme.nouveautes().size(); i++) {
            Nouveaute n = theme.nouveautes().get(i);
            boolean nouvelle = nonLue(joueur, theme, n);
            List<Component> lignes = new ArrayList<>();
            if (nouvelle) {
                lignes.add(lang.c("nouveaute.nouveau", "<green><bold>Nouveau"));
            }
            lignes.addAll(Contenant.lignes(n.texte(), NamedTextColor.GRAY));
            if (!n.commande().isBlank()) {
                lignes.add(lang.c("nouveaute.commande", "<yellow>Commande : <commande>", "commande", n.commande()));
            }
            ItemStack objet = Contenant.objet(n.icone(), Component.text(n.nom(), NamedTextColor.GOLD), lignes);
            menu.poser(i, briller(objet, nouvelle), null);
        }
        menu.barre();
        menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, lang.c("retour", "<yellow>Retour"),
                List.of(lang.c("retour-themes", "<gray>Vers la liste des thèmes"))), p -> ouvrirThemes(p, retour));
        menu.ouvrir(joueur);
        marquerLues(joueur, theme);
        lang.saveIfNeeded();
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
