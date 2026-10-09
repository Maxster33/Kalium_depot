package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.economy.Magasins.Magasin;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.1.3 - signalements (demande de LeKiwi06, 03/10/2026 : « un bouton pour signaler une boutique ou un magasin au
 * complet, pour arnaque, contenu inapproprié, etc ») : raisons à cocher + « Autre » ; message au staff connecté
 * (kseconomy.staff) et dans la console ; enregistrés dans plugins/KS_Economy/signalements.yml ; /magasin signalements
 * (staff) : voir, se téléporter, supprimer la boutique signalée, classer.
 *
 * 1.5.0 (LeKiwi06, 09/10/2026) : menus en coffres (voir Menus) : raisons à cocher (teintures), liste et fiche des
 * signalements du staff. Restent en fenêtre de Gui les saisies de texte : « Autre », note de classement.
 */
final class Signalements {

    static final String PERMISSION_STAFF = "kseconomy.staff";
    private static final int AUTRE_MAX = 200;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Paris"));

    static final class Signalement {
        int id;
        /** « boutique » ou « magasin ». */
        String type;
        /** Id de la boutique, ou UUID du propriétaire du magasin. */
        String cible;
        UUID proprio;
        String nomCible;
        Location lieu;
        UUID auteur;
        final List<String> raisons = new ArrayList<>();
        String autre = "";
        long date;
        boolean classe;
        String traitePar = "";
        String action = "";
    }

    private final KSEconomy plugin;
    private final Magasins magasins;
    private final Lang lang;
    private final Gui gui;
    private final Menus menus;
    private final File fichier;
    private final Map<Integer, Signalement> liste = new LinkedHashMap<>();
    private int prochainId = 1;

    Signalements(KSEconomy plugin, Magasins magasins) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        this.menus = plugin.menus();
        this.fichier = new File(plugin.getDataFolder(), "signalements.yml");
        charger();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    static boolean staff(Player joueur) {
        return joueur.hasPermission(PERMISSION_STAFF);
    }

    /** Raisons proposées (cases à cocher), puis « Autre » en texte libre. */
    private List<Component> raisons() {
        return List.of(
                t("signalement.raison-arnaque", "Arnaque (objet ou prix trompeur)"),
                t("signalement.raison-contenu", "Contenu inapproprié (nom, texte)"),
                t("signalement.raison-theme", "Thème du magasin non respecté"));
    }

    private static String texte(Component c) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
    }

    // ------------------------------------------------------------------ joueur

    void signalerBoutique(Player joueur, Boutique b, Consumer<Player> retour) {
        formulaire(joueur, "boutique", b.id, b.proprio, Boutiques.nomBoutique(b), b.panneau, retour);
    }

    void signalerMagasin(Player joueur, Magasin m, Consumer<Player> retour) {
        formulaire(joueur, "magasin", m.proprio.toString(), m.proprio, m.nom, m.position, retour);
    }

    private void formulaire(Player joueur, String type, String cible, UUID proprio, String nom, Location lieu,
                            Consumer<Player> retour) {
        if (proprio.equals(joueur.getUniqueId())) {
            retour.accept(joueur);
            return;
        }
        if (dejaSignale(joueur.getUniqueId(), type, cible)) {
            menus.message(joueur, t("signalement.deja", "<yellow>Tu as déjà signalé ceci ; le staff ne l'a pas encore "
                    + "traité."), true);
            return;
        }
        Signalement s = new Signalement();
        s.type = type;
        s.cible = cible;
        s.proprio = proprio;
        s.nomCible = nom;
        s.lieu = lieu;
        s.auteur = joueur.getUniqueId();
        cocher(joueur, s, retour);
    }

    /**
     * 1.5.0 : coffre du signalement : une teinture par raison (verte : cochée), un livre pour « Autre » (saisie de
     * texte, fenêtre de Gui), « Envoyer » dans la barre d'actions. Le signalement en préparation garde les choix.
     */
    private void cocher(Player joueur, Signalement s, Consumer<Player> retour) {
        List<Component> raisons = raisons();
        Contenant c = menus.menu(joueur, 4, "boutique".equals(s.type)
                ? t("signalement.titre-boutique-coffre", "<dark_gray>Signaler la boutique")
                : t("signalement.titre-magasin-coffre", "<dark_gray>Signaler le magasin"), "signalement-" + s.type);
        c.poser(4, Menus.objet(Material.PAPER, t("signalement.cible", "<white><nom>", "nom", s.nomCible),
                t("catalogue.info", "<gray>de <proprio>", "proprio", Boutiques.nomJoueur(s.proprio)),
                t("signalement.aide-coffre", "<gray>Coche les raisons, ou précise dans « Autre », puis « Envoyer ».")),
                null);
        int[] places = {10, 12, 14};
        for (int i = 0; i < raisons.size() && i < places.length; i++) {
            String raison = texte(raisons.get(i));
            boolean cochee = s.raisons.contains(raison);
            c.poser(places[i], Contenant.objet(cochee ? Material.LIME_DYE : Material.GRAY_DYE,
                    Component.text(raison, cochee ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    List.of(cochee ? t("signalement.cochee", "<green>Cochée <dark_gray>(clic : décocher)")
                            : t("signalement.a-cocher", "<dark_gray>Clic : cocher"))), p -> {
                if (!s.raisons.remove(raison)) {
                    s.raisons.add(raison);
                }
                cocher(p, s, retour);
            });
        }
        c.poser(16, Menus.objet(Material.WRITABLE_BOOK, t("signalement.autre", "<white>Autre (précise)"),
                s.autre.isEmpty() ? null : Component.text("« " + s.autre + " »", NamedTextColor.YELLOW),
                t("recherche.clic-ecrire", "<dark_gray>Clic : écrire")), p -> preciser(p, s, retour));
        c.poser(c.bas(4), Contenant.objet(Material.BELL, t("signalement.envoyer", "<red><bold>Envoyer le signalement"),
                List.of()), p -> envoyer(p, s, retour));
        menus.sortie(c, retour);
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private void preciser(Player joueur, Signalement s, Consumer<Player> retour) {
        ActionButton valider = gui.form(t("boutique.valider", "<green>Valider"), null, (p, vue) -> {
            String autre = vue.getText("autre") == null ? "" : vue.getText("autre").trim();
            s.autre = autre.length() > AUTRE_MAX ? autre.substring(0, AUTRE_MAX) : autre;
            cocher(p, s, retour);
        });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, "boutique".equals(s.type) ? t("signalement.titre-boutique", "<red><bold>Signaler la boutique")
                            : t("signalement.titre-magasin", "<red><bold>Signaler le magasin"), List.of(),
                    List.of(gui.text("autre", t("signalement.champ-autre", "Autre (précise)"), s.autre, AUTRE_MAX)),
                    List.of(valider, gui.button(t("signalement.retour", "<gray>Retour"), null,
                            p -> cocher(p, s, retour))), null, 1);
            lang.saveIfNeeded();
        });
    }

    private void envoyer(Player joueur, Signalement s, Consumer<Player> retour) {
        if (s.raisons.isEmpty() && s.autre.isEmpty()) {
            menus.message(joueur, t("signalement.vide", "<red>Coche au moins une raison, ou précise dans « Autre »."),
                    true);
            return;
        }
        if (dejaSignale(joueur.getUniqueId(), s.type, s.cible)) {
            retour.accept(joueur);
            return;
        }
        List<String> ordre = raisons().stream().map(Signalements::texte).toList();
        s.raisons.sort(java.util.Comparator.comparingInt(ordre::indexOf));
        s.id = prochainId++;
        s.date = System.currentTimeMillis();
        liste.put(s.id, s);
        sauver();
        prevenirStaff(s);
        retour.accept(joueur);
        menus.message(joueur, t("signalement.merci", "<gray>Ton signalement a été envoyé au staff."), false);
    }

    private boolean dejaSignale(UUID auteur, String type, String cible) {
        for (Signalement s : liste.values()) {
            if (!s.classe && s.auteur.equals(auteur) && s.type.equals(type) && s.cible.equals(cible)) {
                return true;
            }
        }
        return false;
    }

    private static String typeAffiche(Signalement s) {
        return "boutique".equals(s.type) ? "Boutique" : "Magasin";
    }

    private String resume(Signalement s) {
        List<String> tout = new ArrayList<>(s.raisons);
        if (!s.autre.isEmpty()) {
            tout.add("« " + s.autre + " »");
        }
        return String.join(", ", tout);
    }

    private void prevenirStaff(Signalement s) {
        String quoi = ("boutique".equals(s.type) ? "la boutique « " : "le magasin « ") + s.nomCible + " » de "
                + Boutiques.nomJoueur(s.proprio);
        plugin.getLogger().info("Signalement n°" + s.id + " par " + Boutiques.nomJoueur(s.auteur) + " : " + quoi + " - "
                + resume(s));
        Component message = t("signalement.staff", "<red>[Signalement n°<id>] <white><auteur> <gray>signale <white><quoi>"
                        + "<gray> : <raisons>. <yellow>/magasin signalements", "id", s.id,
                "auteur", Boutiques.nomJoueur(s.auteur), "quoi", quoi, "raisons", resume(s));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (staff(p)) {
                p.sendMessage(message);
            }
        }
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ staff

    /** Lignes communes d'un signalement : sa cible, son auteur et sa date, ses raisons. */
    private List<Component> fiche(Signalement s) {
        return Menus.lignes(
                t("staff.cible", "<white><type> « <nom> » de <proprio>", "type", typeAffiche(s), "nom", s.nomCible,
                        "proprio", Boutiques.nomJoueur(s.proprio)),
                t("staff.auteur", "<gray>Par <white><auteur></white>, le <date>", "auteur",
                        Boutiques.nomJoueur(s.auteur), "date", DATE.format(Instant.ofEpochMilli(s.date))),
                t("staff.raisons", "<gray>Raisons : <white><raisons>", "raisons", resume(s)));
    }

    /**
     * /magasin signalements (ou rubrique « Modération » de /menu) : non traités (ou classés), par pages. 1.5.0 : coffre,
     * un signalement par case (coffre : boutique ; pancarte : magasin).
     *
     * @param retour écran précédent (fenêtre de /menu), ou null
     */
    void ouvrirStaff(Player joueur, boolean classes, int page, Consumer<Player> retour) {
        if (!staff(joueur)) {
            return;
        }
        List<Signalement> choisis = new ArrayList<>();
        for (Signalement s : liste.values()) {
            if (s.classe == classes) {
                choisis.add(s);
            }
        }
        java.util.Collections.reverse(choisis);
        int pages = Math.max(1, (choisis.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        Contenant c = menus.menu(joueur, 6, classes ? t("staff.titre-classes-coffre", "<dark_gray>Signalements classés")
                : t("staff.titre-coffre", "<dark_gray>Signalements"), "signalements-" + classes);
        for (int i = p * Contenant.PAR_PAGE; i < Math.min(choisis.size(), (p + 1) * Contenant.PAR_PAGE); i++) {
            Signalement s = choisis.get(i);
            List<Component> lignes = new ArrayList<>(fiche(s));
            lignes.add(t("staff.clic", "<dark_gray>Clic : ouvrir"));
            c.poser(i - p * Contenant.PAR_PAGE, Contenant.objet(
                    "boutique".equals(s.type) ? Material.CHEST : Material.OAK_HANGING_SIGN,
                    t("staff.bouton-signalement", "<white>n°<id> : <nom>", "id", s.id, "nom",
                            Boutiques.couper(s.nomCible, 24)), lignes), j -> detail(j, s, classes, retour));
        }
        if (choisis.isEmpty()) {
            c.poser(22, Contenant.objet(Material.PAPER, t("staff.aucun", "<gray>Aucun signalement."), List.of()), null);
        }
        menus.pages(c, p, pages, (j, n) -> ouvrirStaff(j, classes, n, retour));
        c.poser(c.bas(4), classes
                ? Contenant.objet(Material.BELL, t("staff.bouton-non-traites", "<yellow>Non traités"), List.of())
                : Contenant.objet(Material.BOOKSHELF, t("staff.bouton-classes", "<gray>Classés"), List.of()),
                j -> ouvrirStaff(j, !classes, 0, retour));
        // L'écran précédent est une fenêtre de /menu : le coffre est refermé avant de l'ouvrir.
        menus.sortie(c, retour == null ? null : j -> Contenant.saisie(j, () -> retour.accept(j)));
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** 1.5.0 : coffre d'un signalement : sa fiche, la boutique signalée, et les actions du staff. */
    private void detail(Player joueur, Signalement s, boolean classes, Consumer<Player> retour) {
        if (!staff(joueur)) {
            return;
        }
        Contenant c = menus.menu(joueur, 5, t("staff.titre-detail-coffre", "<dark_gray>Signalement n°<id>", "id", s.id),
                "signalement-staff");
        List<Component> lignes = new ArrayList<>(fiche(s));
        Boutique b = "boutique".equals(s.type) ? magasins.boutiques.get(s.cible) : null;
        if (b == null && "boutique".equals(s.type)) {
            lignes.add(t("staff.disparue", "<gray>Cette boutique n'existe plus."));
        }
        if (s.classe) {
            lignes.addAll(Menus.lignes(t("staff.classe", "<green>Classé par <qui> : <action>", "qui", s.traitePar,
                    "action", s.action)));
        }
        c.poser(b == null ? 4 : 3, Contenant.objet(Material.PAPER, t("staff.detail-nom", "<gold>Signalement n°<id>", "id",
                s.id), lignes), null);
        if (b != null) {
            c.poser(5, plugin.boutiques().icone(b), null);
        }
        Location lieu = b != null ? b.panneau : s.lieu;
        if (lieu != null && lieu.getWorld() != null) {
            c.poser(19, Contenant.objet(Material.ENDER_PEARL, t("staff.bouton-tp", "<white>Se téléporter"), List.of()),
                    p -> Contenant.apres(() -> {
                        p.closeInventory();
                        p.teleport(lieu.clone().add(0.5, 0, 0.5));
                    }));
        }
        if (b != null && !s.classe) {
            c.poser(21, Menus.objet(Material.LAVA_BUCKET, t("staff.bouton-supprimer", "<red>Supprimer la boutique"),
                    t("staff.supprimer-info", "<gray>Une confirmation est demandée ; le signalement sera classé")),
                    p -> menus.confirmer(p, t("staff.titre-supprimer-coffre", "<dark_gray>Supprimer la boutique"),
                            t("staff.supprimer-texte", "<white>La boutique est supprimée (panneau ordinaire, contenu du "
                                    + "contenant et points en attente laissés au propriétaire) ; le signalement est "
                                    + "classé."),
                            q -> {
                                if (magasins.boutiques.containsKey(b.id)) {
                                    plugin.boutiques().supprimer(b, null, false, false);
                                }
                                classer(q, s, "boutique supprimée");
                                ouvrirStaff(q, false, 0, retour);
                            }, q -> detail(q, s, classes, retour)));
        }
        if (!s.classe) {
            c.poser(23, Menus.objet(Material.LIME_DYE, t("staff.bouton-classer", "<green>Classer"),
                    t("staff.classer-info", "<gray>Sans note : « rien à faire »")), p -> {
                classer(p, s, "rien à faire");
                ouvrirStaff(p, false, 0, retour);
            });
            c.poser(25, Menus.objet(Material.WRITABLE_BOOK, t("staff.bouton-classer-note", "<green>Classer avec une note"),
                    t("staff.classer-note-info", "<gray>Écrire l'action faite")), p -> noter(p, s, classes, retour));
        }
        menus.sortie(c, p -> ouvrirStaff(p, classes, 0, retour));
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Classer avec une note : saisie de texte (fenêtre de Gui). */
    private void noter(Player joueur, Signalement s, boolean classes, Consumer<Player> retour) {
        ActionButton classer = gui.form(t("staff.bouton-classer", "<green>Classer"), null, (p, vue) -> {
            String action = vue.getText("action") == null ? "" : vue.getText("action").trim();
            if (!s.classe) {
                classer(p, s, action.isEmpty() ? "rien à faire" : action);
            }
            ouvrirStaff(p, false, 0, retour);
        });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("staff.titre-detail", "<red><bold>Signalement n°<id>", "id", s.id), List.of(),
                    List.of(gui.text("action", t("staff.champ-action", "Action faite (pour classer)"), "", 100)),
                    List.of(classer, gui.button(t("signalement.retour", "<gray>Retour"), null,
                            p -> detail(p, s, classes, retour))), gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    private void classer(Player staff, Signalement s, String action) {
        s.classe = true;
        s.traitePar = staff.getName();
        s.action = action.length() > 100 ? action.substring(0, 100) : action;
        sauver();
        plugin.getLogger().info("Signalement n°" + s.id + " classé par " + staff.getName() + " : " + s.action);
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        prochainId = Math.max(1, yaml.getInt("prochain-id", 1));
        ConfigurationSection ss = yaml.getConfigurationSection("signalements");
        if (ss == null) {
            return;
        }
        for (String cle : ss.getKeys(false)) {
            ConfigurationSection c = ss.getConfigurationSection(cle);
            try {
                Signalement s = new Signalement();
                s.id = Integer.parseInt(cle);
                s.type = c.getString("type", "boutique");
                s.cible = c.getString("cible", "");
                s.proprio = UUID.fromString(c.getString("proprio", ""));
                s.nomCible = c.getString("nom", "?");
                s.lieu = c.getLocation("lieu");
                s.auteur = UUID.fromString(c.getString("auteur", ""));
                s.raisons.addAll(c.getStringList("raisons"));
                s.autre = c.getString("autre", "");
                s.date = c.getLong("date");
                s.classe = c.getBoolean("classe");
                s.traitePar = c.getString("traite-par", "");
                s.action = c.getString("action", "");
                liste.put(s.id, s);
                prochainId = Math.max(prochainId, s.id + 1);
            } catch (IllegalArgumentException | NullPointerException e) {
                plugin.getLogger().warning("Signalement ignoré : " + cle);
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("prochain-id", prochainId);
        liste.forEach((id, s) -> {
            String b = "signalements." + id;
            yaml.set(b + ".type", s.type);
            yaml.set(b + ".cible", s.cible);
            yaml.set(b + ".proprio", s.proprio.toString());
            yaml.set(b + ".nom", s.nomCible);
            yaml.set(b + ".lieu", s.lieu);
            yaml.set(b + ".auteur", s.auteur.toString());
            yaml.set(b + ".raisons", s.raisons);
            yaml.set(b + ".autre", s.autre);
            yaml.set(b + ".date", s.date);
            yaml.set(b + ".classe", s.classe);
            yaml.set(b + ".traite-par", s.traitePar);
            yaml.set(b + ".action", s.action);
        });
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer signalements.yml : " + e.getMessage());
        }
    }
}
