package fr.kalium.didacticiel;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.3.0 (LeKiwi06, 18/10/2026 : « ajoute une touche "poser une question" dans le didacticiel, précise que le staff
 * répondra aux questions posées et modifiera le didacticiel si besoin ») : ses choix : outil staff dans la rubrique
 * « Modération » de /menu et réponse en jeu ; pas de limite de questions.
 *
 * - Bouton « Poser une question » dans la barre de l'écran des thèmes : fenêtre de saisie ; la question est enregistrée
 *   (questions.yml), notée dans la console et annoncée au staff connecté.
 * - Outil « Questions du didacticiel » (permission ksdidacticiel.staff, opérateurs) : questions en attente (les plus
 *   anciennes d'abord) ou répondues ; clic sur une question en attente : fenêtre de réponse.
 * - Réponse : envoyée au joueur dans le tchat, ou à sa prochaine connexion s'il n'est pas là.
 */
final class Questions implements Listener {

    static final String PERMISSION_STAFF = "ksdidacticiel.staff";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .withZone(ZoneId.of("Europe/Paris"));

    /** Une question et, s'il y en a une, sa réponse. */
    private static final class Question {
        int id;
        UUID joueur;
        String pseudo;
        long date;
        String texte;
        String reponse;
        String repondant;
        long dateReponse;
        /** La réponse a été montrée au joueur. */
        boolean livree;
    }

    private final KSDidacticiel plugin;
    private final Lang lang;
    private final Gui gui;
    private final File fichier;
    private final Map<Integer, Question> questions = new TreeMap<>();
    private int prochain = 1;

    Questions(KSDidacticiel plugin, Lang lang, Gui gui) {
        this.plugin = plugin;
        this.lang = lang;
        this.gui = gui;
        this.fichier = new File(plugin.getDataFolder(), "questions.yml");
        charger();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ joueur : poser une question

    /** Le bouton de la barre de l'écran des thèmes. */
    ItemStack bouton() {
        return Contenant.objet(Material.WRITABLE_BOOK, t("question.bouton", "<green>Poser une question"), List.of(
                t("question.bouton-1", "<gray>Une question sur Kixster ?"),
                t("question.bouton-2", "<gray>Le staff y répondra en jeu et"),
                t("question.bouton-3", "<gray>modifiera le didacticiel si"),
                t("question.bouton-4", "<gray>besoin."),
                t("question.bouton-clic", "<yellow>Clic : écrire ta question")));
    }

    /** Fenêtre de saisie de la question ; {@code retour} rouvre le didacticiel. */
    void poser(Player joueur, Consumer<Player> retour) {
        ActionButton envoyer = gui.form(t("question.envoyer", "<green>Envoyer"), null, (p, vue) -> {
            String texte = vue.getText("question") == null ? "" : vue.getText("question").trim();
            if (texte.isEmpty()) {
                gui.notice(p, titre(), t("question.vide", "<red>Écris ta question avant de l'envoyer."),
                        q -> poser(q, retour));
                lang.saveIfNeeded();
                return;
            }
            enregistrer(p, texte);
            gui.notice(p, titre(), t("question.envoyee", "<green>Question envoyée. <gray>Le staff te répondra en jeu (dans "
                    + "le tchat, ou à ta prochaine connexion) et modifiera le didacticiel si besoin."), retour::accept);
            lang.saveIfNeeded();
        });
        gui.open(joueur, titre(), List.of(t("question.texte", "<gray>Pose ta question sur Kixster : le staff y répondra "
                        + "en jeu, dans le tchat (ou à ta prochaine connexion), et modifiera le didacticiel si besoin.")),
                List.of(gui.text("question", t("question.champ", "Ta question"), "", 256)),
                List.of(envoyer, gui.button(t("question.retour", "<gray>Retour"), null, retour::accept)), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private Component titre() {
        return t("question.titre", "<gold><bold>Poser une question");
    }

    private void enregistrer(Player joueur, String texte) {
        Question q = new Question();
        q.id = prochain++;
        q.joueur = joueur.getUniqueId();
        q.pseudo = joueur.getName();
        q.date = System.currentTimeMillis();
        q.texte = texte;
        questions.put(q.id, q);
        sauver();
        plugin.getLogger().info("Question n°" + q.id + " de " + q.pseudo + " : " + texte);
        Component annonce = t("question.annonce", "<gold>[Didacticiel] <white><pseudo></white> pose une question : <gray>"
                + "<texte> <dark_gray>(/menu, Modération, Questions du didacticiel)", "pseudo", q.pseudo, "texte", texte);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(PERMISSION_STAFF)) {
                staff.sendMessage(annonce);
            }
        }
    }

    // ------------------------------------------------------------------ staff : répondre

    /** Outil de la rubrique « Modération » : questions en attente, ou répondues ; 45 par page. */
    void ouvrirStaff(Player staff, boolean repondues, int page, Consumer<Player> retour) {
        List<Question> liste = new ArrayList<>(questions.values().stream()
                .filter(q -> (q.reponse != null) == repondues).toList());
        // En attente : les plus anciennes d'abord ; répondues : les plus récentes d'abord.
        liste.sort(repondues ? Comparator.comparingLong((Question q) -> -q.dateReponse)
                : Comparator.comparingLong((Question q) -> q.date));
        int pages = Math.max(1, (liste.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant menu = Contenant.pour(staff, repondues ? t("staff.titre-repondues", "<dark_gray>Questions répondues")
                : t("staff.titre", "<dark_gray>Questions du didacticiel"), "ks_didacticiel:questions:" + repondues);
        for (int i = p * Contenant.PAR_PAGE; i < Math.min(liste.size(), (p + 1) * Contenant.PAR_PAGE); i++) {
            Question q = liste.get(i);
            List<Component> lignes = new ArrayList<>();
            lignes.add(t("staff.date", "<gray>Posée le <date>", "date", DATE.format(Instant.ofEpochMilli(q.date))));
            lignes.addAll(Contenant.lignes(q.texte, NamedTextColor.WHITE));
            if (q.reponse != null) {
                lignes.add(t("staff.repondu", "<green>Réponse de <repondant> (<date>) :", "repondant", q.repondant,
                        "date", DATE.format(Instant.ofEpochMilli(q.dateReponse))));
                lignes.addAll(Contenant.lignes(q.reponse, NamedTextColor.GRAY));
                lignes.add(q.livree ? t("staff.livree", "<dark_gray>Lue par le joueur")
                        : t("staff.non-livree", "<dark_gray>Montrée à sa prochaine connexion"));
            } else {
                lignes.add(t("staff.clic", "<yellow>Clic : répondre"));
            }
            ItemStack objet = Contenant.objet(q.reponse != null ? Material.BOOK : Material.PAPER,
                    t("staff.nom", "<gold>n°<id> · <pseudo>", "id", q.id, "pseudo", q.pseudo), lignes);
            int id = q.id;
            menu.poser(i - p * Contenant.PAR_PAGE, objet, q.reponse != null ? null
                    : s -> Contenant.saisie(s, () -> repondre(s, id, retour)));
        }
        if (liste.isEmpty()) {
            menu.poser(22, Contenant.objet(Material.PAPER, repondues
                    ? t("staff.aucune-repondue", "<gray>Aucune question répondue.")
                    : t("staff.aucune", "<gray>Aucune question en attente."), List.of()), null);
        }
        menu.barre();
        if (p > 0) {
            menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, t("staff.precedente", "<yellow>Page précédente"),
                    List.of()), s -> ouvrirStaff(s, repondues, p - 1, retour));
        }
        if (p < pages - 1) {
            menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, t("staff.suivante", "<yellow>Page suivante"),
                    List.of()), s -> ouvrirStaff(s, repondues, p + 1, retour));
        }
        menu.poser(menu.bas(3), Contenant.objet(Material.CLOCK, repondues
                        ? t("staff.voir-attente", "<yellow>Voir les questions en attente")
                        : t("staff.voir-repondues", "<yellow>Voir les questions répondues"), List.of()),
                s -> ouvrirStaff(s, !repondues, 0, retour));
        if (retour != null) {
            menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, t("staff.retour", "<yellow>Retour"), List.of()),
                    s -> Contenant.saisie(s, () -> retour.accept(s)));
        } else {
            menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("staff.fermer", "<red>Fermer"), List.of()),
                    Contenant::fermer);
        }
        menu.ouvrir(staff);
        lang.saveIfNeeded();
    }

    private void repondre(Player staff, int id, Consumer<Player> retour) {
        Question q = questions.get(id);
        if (q == null || q.reponse != null) {
            ouvrirStaff(staff, false, 0, retour);
            return;
        }
        ActionButton envoyer = gui.form(t("staff.envoyer", "<green>Envoyer la réponse"), null, (s, vue) -> {
            String texte = vue.getText("reponse") == null ? "" : vue.getText("reponse").trim();
            Question actuelle = questions.get(id);
            if (actuelle == null || actuelle.reponse != null) {
                gui.notice(s, titreStaff(), t("staff.deja", "<red>Quelqu'un a déjà répondu à cette question."),
                        r -> ouvrirStaff(r, false, 0, retour));
            } else if (texte.isEmpty()) {
                gui.notice(s, titreStaff(), t("staff.vide", "<red>Écris ta réponse avant de l'envoyer."),
                        r -> repondre(r, id, retour));
            } else {
                actuelle.reponse = texte;
                actuelle.repondant = s.getName();
                actuelle.dateReponse = System.currentTimeMillis();
                Player joueur = Bukkit.getPlayer(actuelle.joueur);
                if (joueur != null) {
                    livrer(joueur, actuelle);
                }
                sauver();
                plugin.getLogger().info(s.getName() + " répond à la question n°" + id + " de " + actuelle.pseudo + " : "
                        + texte);
                gui.notice(s, titreStaff(), joueur != null
                        ? t("staff.envoyee", "<green>Réponse envoyée à <pseudo>.", "pseudo", actuelle.pseudo)
                        : t("staff.envoyee-absent", "<green>Réponse enregistrée : <pseudo> la verra à sa prochaine "
                        + "connexion.", "pseudo", actuelle.pseudo), r -> ouvrirStaff(r, false, 0, retour));
            }
            lang.saveIfNeeded();
        });
        List<Component> corps = new ArrayList<>();
        corps.add(t("staff.question-de", "<gray>Question de <white><pseudo></white>, le <date> :", "pseudo", q.pseudo,
                "date", DATE.format(Instant.ofEpochMilli(q.date))));
        corps.add(Component.text(q.texte, NamedTextColor.WHITE));
        corps.add(t("staff.rappel", "<gray>Si la réponse manque au didacticiel, pense à le mettre à jour."));
        gui.open(staff, titreStaff(), corps, List.of(gui.text("reponse", t("staff.champ", "Ta réponse"), "", 256)),
                List.of(envoyer, gui.button(t("staff.retour-liste", "<gray>Retour"), null,
                        s -> ouvrirStaff(s, false, 0, retour))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private Component titreStaff() {
        return t("staff.titre-repondre", "<gold><bold>Répondre à une question");
    }

    // ------------------------------------------------------------------ livraison des réponses

    private void livrer(Player joueur, Question q) {
        joueur.sendMessage(t("reponse.titre", "<gold>[Didacticiel] <white><repondant></white> a répondu à ta question :",
                "repondant", q.repondant));
        joueur.sendMessage(t("reponse.question", "<gray>« <texte> »", "texte", q.texte));
        joueur.sendMessage(t("reponse.texte", "<white><texte>", "texte", q.reponse));
        q.livree = true;
    }

    /** Réponses arrivées pendant l'absence du joueur : montrées 2 secondes après sa connexion. */
    @EventHandler
    public void onConnexion(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!joueur.isOnline()) {
                return;
            }
            boolean change = false;
            for (Question q : questions.values()) {
                if (q.reponse != null && !q.livree && q.joueur.equals(joueur.getUniqueId())) {
                    livrer(joueur, q);
                    change = true;
                }
            }
            if (change) {
                sauver();
                lang.saveIfNeeded();
            }
        }, 40L);
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(fichier);
        prochain = Math.max(1, yml.getInt("prochain", 1));
        ConfigurationSection section = yml.getConfigurationSection("questions");
        if (section == null) {
            return;
        }
        for (String cle : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(cle);
            try {
                Question q = new Question();
                q.id = Integer.parseInt(cle);
                q.joueur = UUID.fromString(s.getString("joueur", ""));
                q.pseudo = s.getString("pseudo", "?");
                q.date = s.getLong("date");
                q.texte = s.getString("texte", "");
                q.reponse = s.getString("reponse");
                q.repondant = s.getString("repondant");
                q.dateReponse = s.getLong("date-reponse");
                q.livree = s.getBoolean("livree");
                questions.put(q.id, q);
                prochain = Math.max(prochain, q.id + 1);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Question illisible dans questions.yml : " + cle);
            }
        }
    }

    private void sauver() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("prochain", prochain);
        for (Question q : questions.values()) {
            String c = "questions." + q.id + ".";
            yml.set(c + "joueur", q.joueur.toString());
            yml.set(c + "pseudo", q.pseudo);
            yml.set(c + "date", q.date);
            yml.set(c + "texte", q.texte);
            if (q.reponse != null) {
                yml.set(c + "reponse", q.reponse);
                yml.set(c + "repondant", q.repondant);
                yml.set(c + "date-reponse", q.dateReponse);
                yml.set(c + "livree", q.livree);
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("questions.yml non enregistré : " + e.getMessage());
        }
    }
}
