package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.economy.Magasins.Magasin;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
 */
final class Signalements {

    static final String PERMISSION_STAFF = "kseconomy.staff";
    private static final int PAR_PAGE = 10;
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
    private final File fichier;
    private final Map<Integer, Signalement> liste = new LinkedHashMap<>();
    private int prochainId = 1;

    Signalements(KSEconomy plugin, Magasins magasins) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
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
            gui.notice(joueur, t("signalement.titre", "<red><bold>Signaler"),
                    t("signalement.deja", "<yellow>Tu as déjà signalé ceci ; le staff ne l'a pas encore traité."),
                    retour::accept);
            lang.saveIfNeeded();
            return;
        }
        List<Component> raisons = raisons();
        List<DialogInput> champs = new ArrayList<>();
        for (int i = 0; i < raisons.size(); i++) {
            champs.add(gui.toggle("r" + i, raisons.get(i), false));
        }
        champs.add(gui.text("autre", t("signalement.champ-autre", "Autre (précise)"), "", AUTRE_MAX));
        ActionButton envoyer = gui.form(t("signalement.envoyer", "<red><bold>Envoyer le signalement"), null, (p, vue) -> {
            Signalement s = new Signalement();
            for (int i = 0; i < raisons.size(); i++) {
                if (Boolean.TRUE.equals(vue.getBoolean("r" + i))) {
                    s.raisons.add(texte(raisons.get(i)));
                }
            }
            String autre = vue.getText("autre") == null ? "" : vue.getText("autre").trim();
            s.autre = autre.length() > AUTRE_MAX ? autre.substring(0, AUTRE_MAX) : autre;
            if (s.raisons.isEmpty() && s.autre.isEmpty()) {
                gui.notice(p, t("signalement.titre", "<red><bold>Signaler"),
                        t("signalement.vide", "<red>Coche au moins une raison, ou précise dans « Autre »."),
                        q -> formulaire(q, type, cible, proprio, nom, lieu, retour));
                lang.saveIfNeeded();
                return;
            }
            if (dejaSignale(p.getUniqueId(), type, cible)) {
                retour.accept(p);
                return;
            }
            s.id = prochainId++;
            s.type = type;
            s.cible = cible;
            s.proprio = proprio;
            s.nomCible = nom;
            s.lieu = lieu;
            s.auteur = p.getUniqueId();
            s.date = System.currentTimeMillis();
            liste.put(s.id, s);
            sauver();
            prevenirStaff(s);
            gui.notice(p, t("signalement.merci-titre", "<green><bold>Merci !"),
                    t("signalement.merci", "<gray>Ton signalement a été envoyé au staff."), retour::accept);
            lang.saveIfNeeded();
        });
        gui.open(joueur, "boutique".equals(type) ? t("signalement.titre-boutique", "<red><bold>Signaler la boutique")
                        : t("signalement.titre-magasin", "<red><bold>Signaler le magasin"),
                List.of(t("signalement.aide", "<white><nom> <gray>de <proprio>. Coche les raisons, ou précise dans "
                        + "« Autre ».", "nom", nom, "proprio", Boutiques.nomJoueur(proprio))),
                champs, List.of(envoyer, gui.button(t("signalement.retour", "<gray>Retour"), null, retour::accept)),
                null, 1);
        lang.saveIfNeeded();
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

    /** /magasin signalements (ou rubrique « Modération » de /menu) : non traités (ou classés), par pages. */
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
        int pages = Math.max(1, (choisis.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = p * PAR_PAGE; i < Math.min(choisis.size(), (p + 1) * PAR_PAGE); i++) {
            Signalement s = choisis.get(i);
            boutons.add(gui.button(t("staff.bouton-signalement", "<white>n°<id> : <nom>", "id", s.id,
                            "nom", Boutiques.couper(s.nomCible, 24)),
                    t("staff.info-signalement", "<gray><type> de <proprio>", "type", typeAffiche(s),
                            "proprio", Boutiques.nomJoueur(s.proprio)), j -> detail(j, s, classes, retour)));
        }
        if (p > 0) {
            boutons.add(gui.button(t("magasin.precedent", "<yellow>Page précédente"), null, j -> ouvrirStaff(j, classes, p - 1, retour)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("magasin.suivant", "<yellow>Page suivante"), null, j -> ouvrirStaff(j, classes, p + 1, retour)));
        }
        boutons.add(classes
                ? gui.button(t("staff.bouton-non-traites", "<yellow>Non traités"), null, j -> ouvrirStaff(j, false, 0, retour))
                : gui.button(t("staff.bouton-classes", "<gray>Classés"), null, j -> ouvrirStaff(j, true, 0, retour)));
        if (retour != null) {
            boutons.add(gui.button(t("signalement.retour", "<gray>Retour"), null, retour::accept));
        }
        List<Component> corps = List.of(choisis.isEmpty() ? t("staff.aucun", "<gray>Aucun signalement.")
                : t("staff.nombre", "<white><nombre> signalement(s).", "nombre", choisis.size()));
        gui.open(joueur, classes ? t("staff.titre-classes", "<red><bold>Signalements classés")
                : t("staff.titre", "<red><bold>Signalements"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void detail(Player joueur, Signalement s, boolean classes, Consumer<Player> retour) {
        if (!staff(joueur)) {
            return;
        }
        List<Component> corps = new ArrayList<>();
        corps.add(t("staff.cible", "<white><type> « <nom> » de <proprio>", "type", typeAffiche(s), "nom", s.nomCible,
                "proprio", Boutiques.nomJoueur(s.proprio)));
        corps.add(t("staff.auteur", "<gray>Par <white><auteur></white>, le <date>", "auteur", Boutiques.nomJoueur(s.auteur),
                "date", DATE.format(Instant.ofEpochMilli(s.date))));
        corps.add(t("staff.raisons", "<gray>Raisons : <white><raisons>", "raisons", resume(s)));
        Boutique b = "boutique".equals(s.type) ? magasins.boutiques.get(s.cible) : null;
        if (b != null) {
            corps.add(t("staff.offre", "<gray>Offre : ").append(Boutiques.lot(b.quantite, b.objet))
                    .append(Component.text(" contre ")).append(plugin.boutiques().prix(b)));
        } else if ("boutique".equals(s.type)) {
            corps.add(t("staff.disparue", "<gray>Cette boutique n'existe plus."));
        }
        if (s.classe) {
            corps.add(t("staff.classe", "<green>Classé par <qui> : <action>", "qui", s.traitePar, "action", s.action));
        }
        List<ActionButton> boutons = new ArrayList<>();
        Location lieu = b != null ? b.panneau : s.lieu;
        if (lieu != null && lieu.getWorld() != null) {
            boutons.add(gui.button(t("staff.bouton-tp", "<white>Se téléporter"), null,
                    p -> p.teleport(lieu.clone().add(0.5, 0, 0.5))));
        }
        if (b != null && !s.classe) {
            boutons.add(gui.button(t("staff.bouton-supprimer", "<red>Supprimer la boutique"), null,
                    p -> gui.confirm(p, t("staff.titre-supprimer", "<red><bold>Supprimer la boutique"),
                            t("staff.supprimer-texte", "<white>La boutique est supprimée (panneau ordinaire, contenu du "
                                    + "contenant et points en attente laissés au propriétaire) ; le signalement est "
                                    + "classé."),
                            q -> {
                                if (magasins.boutiques.containsKey(b.id)) {
                                    plugin.boutiques().supprimer(b, null, false, false);
                                }
                                classer(q, s, "boutique supprimée");
                                ouvrirStaff(q, false, 0, retour);
                            }, q -> detail(q, s, classes, retour))));
        }
        if (!s.classe) {
            boutons.add(gui.form(t("staff.bouton-classer", "<green>Classer"), null, (p, vue) -> {
                String action = vue.getText("action") == null ? "" : vue.getText("action").trim();
                classer(p, s, action.isEmpty() ? "rien à faire" : action);
                ouvrirStaff(p, false, 0, retour);
            }));
        }
        boutons.add(gui.button(t("signalement.retour", "<gray>Retour"), null, p -> ouvrirStaff(p, classes, 0, retour)));
        List<DialogInput> champs = s.classe ? List.of()
                : List.of(gui.text("action", t("staff.champ-action", "Action faite (pour classer)"), "", 100));
        gui.open(joueur, t("staff.titre-detail", "<red><bold>Signalement n°<id>", "id", s.id), corps, champs, boutons,
                gui.close(), 1);
        lang.saveIfNeeded();
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
