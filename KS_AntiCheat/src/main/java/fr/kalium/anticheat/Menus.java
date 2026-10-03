package fr.kalium.anticheat;

import fr.kalium.anticheat.Alertes.Alerte;
import fr.kalium.anticheat.Suspensions.Suspension;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Interface staff (cahier, catégorie 6 ; rubrique « Modération » de /menu et /anticheat) : alertes récentes, joueurs
 * avec alertes, recherche d'un joueur (alertes, invsee, ecsee, suspendre / lever), suspendus, morts d'entités
 * importantes, journal des consultations.
 */
final class Menus {

    private static final int PAR_PAGE = 10;

    private final KSAntiCheat plugin;
    private final Lang lang;
    private final Gui gui;
    /** Retour vers la rubrique « Modération » de KLM_Menu (si le menu a été ouvert de là). */
    private final Map<UUID, Consumer<Player>> retours = new HashMap<>();

    Menus(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private ActionButton retour(Consumer<Player> vers) {
        return gui.button(t("menu.retour", "<gray>Retour"), null, vers::accept);
    }

    // ------------------------------------------------------------------ accueil

    void memoriserRetour(Player staff, Consumer<Player> retourModeration) {
        if (retourModeration != null) {
            retours.put(staff.getUniqueId(), retourModeration);
        }
    }

    void accueil(Player staff, Consumer<Player> retourModeration) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        if (retourModeration != null) {
            retours.put(staff.getUniqueId(), retourModeration);
        }
        long jour = System.currentTimeMillis() - 24L * 3600_000;
        long recentes = plugin.alertes().recentes(5000).stream().filter(a -> a.date >= jour).count();
        List<Component> corps = List.of(t("menu.resume", "<white>Alertes des dernières 24 h : <yellow><n></yellow> ; "
                + "suspendus : <red><s>", "n", recentes, "s", plugin.suspensions().toutes().size()));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("menu.bouton-alertes", "<yellow>Alertes récentes"), null, p -> alertesRecentes(p)));
        boutons.add(gui.button(t("menu.bouton-joueurs", "<white>Joueurs avec alertes"), null, p -> joueurs(p, 0)));
        boutons.add(gui.button(t("menu.bouton-chercher", "<white>Chercher un joueur"), null, p -> chercher(p, false, false)));
        boutons.add(gui.button(t("menu.bouton-suspendus", "<red>Suspendus"), null, this::suspendus));
        boutons.add(gui.button(t("menu.bouton-minage", "<gold>Minage (x-ray)"), null, p -> minage(p, null)));
        boutons.add(gui.button(t("menu.bouton-morts", "<white>Morts d'entités"), null, this::morts));
        boutons.add(gui.button(t("menu.bouton-consultations", "<white>Journal invsee / ecsee"), null, this::consultations));
        Consumer<Player> versModeration = retours.get(staff.getUniqueId());
        if (versModeration != null) {
            boutons.add(retour(versModeration));
        }
        gui.open(staff, t("menu.titre", "<dark_red><bold>Anti-triche"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void accueil(Player staff) {
        accueil(staff, null);
    }

    // ------------------------------------------------------------------ alertes

    private void alertesRecentes(Player staff) {
        List<Alerte> liste = plugin.alertes().recentes(15);
        List<Component> corps = new ArrayList<>();
        Map<UUID, String> joueurs = new java.util.LinkedHashMap<>();
        for (Alerte a : liste) {
            corps.add(Component.text(a.nom + " - " + Alertes.ligne(a)));
            joueurs.putIfAbsent(a.joueur, a.nom);
        }
        if (corps.isEmpty()) {
            corps.add(t("menu.aucune-alerte", "<gray>Aucune alerte."));
        }
        List<ActionButton> boutons = new ArrayList<>();
        joueurs.forEach((uuid, nom) -> boutons.add(gui.button(Component.text(nom), null,
                p -> joueur(p, plugin.getServer().getOfflinePlayer(uuid)))));
        boutons.add(retour(this::accueil));
        gui.open(staff, t("menu.titre-alertes", "<yellow><bold>Alertes récentes"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void joueurs(Player staff, int page) {
        List<Map.Entry<UUID, String>> liste = new ArrayList<>(plugin.alertes().joueurs().entrySet());
        int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = p * PAR_PAGE; i < Math.min(liste.size(), (p + 1) * PAR_PAGE); i++) {
            UUID uuid = liste.get(i).getKey();
            int n = plugin.alertes().duJoueur(uuid).size();
            boutons.add(gui.button(Component.text(liste.get(i).getValue() + " (" + n + ")"), null,
                    j -> joueur(j, plugin.getServer().getOfflinePlayer(uuid))));
        }
        if (p > 0) {
            boutons.add(gui.button(t("menu.precedent", "<yellow>Page précédente"), null, j -> joueurs(j, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("menu.suivant", "<yellow>Page suivante"), null, j -> joueurs(j, p + 1)));
        }
        boutons.add(retour(this::accueil));
        gui.open(staff, t("menu.titre-joueurs", "<white><bold>Joueurs avec alertes"),
                List.of(liste.isEmpty() ? t("menu.aucune-alerte", "<gray>Aucune alerte.")
                        : t("menu.joueurs-aide", "<gray>Le plus récent d'abord ; entre parenthèses : nombre d'alertes.")),
                List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    /** Recherche d'un joueur ; directInv / ender : ouvre directement son inventaire ou son coffre de l'Ender. */
    void chercher(Player staff, boolean directInv, boolean ender) {
        ActionButton valider = gui.form(t("menu.bouton-valider", "<green>Valider"), null, (p, vue) -> {
            OfflinePlayer cible = KSAntiCheat.connu(vue.getText("pseudo"));
            if (cible == null) {
                gui.notice(p, t("menu.titre", "<dark_red><bold>Anti-triche"),
                        t("menu.inconnu", "<red>Joueur inconnu (il doit être déjà venu sur le serveur)."),
                        q -> chercher(q, directInv, ender));
                lang.saveIfNeeded();
                return;
            }
            if (directInv) {
                plugin.inventaires().ouvrir(p, cible, ender);
            } else {
                joueur(p, cible);
            }
        });
        Consumer<Player> versModeration = retours.get(staff.getUniqueId());
        ActionButton retour = directInv && versModeration != null ? retour(versModeration) : retour(this::accueil);
        gui.open(staff, directInv ? (ender ? t("menu.titre-ecsee", "<dark_purple><bold>EcSee")
                        : t("menu.titre-invsee", "<aqua><bold>Invsee")) : t("menu.titre-chercher", "<white><bold>Chercher un joueur"),
                List.of(), List.of(gui.text("pseudo", t("menu.champ-pseudo", "Pseudo du joueur"), "", 16)),
                List.of(valider, retour), gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ joueur

    void joueur(Player staff, OfflinePlayer cible) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        UUID uuid = cible.getUniqueId();
        String nom = cible.getName() == null ? "?" : cible.getName();
        List<Alerte> alertes = plugin.alertes().duJoueur(uuid);
        Suspension s = plugin.suspensions().suspension(uuid);
        List<Component> corps = new ArrayList<>();
        corps.add(t("joueur.etat", "<white><nom> <gray>- <etat> - <n> alerte(s)", "nom", nom,
                "etat", cible.isOnline() ? "en ligne" : "hors ligne", "n", alertes.size()));
        if (s != null) {
            corps.add(t("joueur.suspendu", "<red>Suspendu le <date> (<par>) : <raison>", "date",
                    Alertes.DATE.format(Instant.ofEpochMilli(s.date)), "par", s.par, "raison", s.raison));
        }
        for (int i = 0; i < Math.min(8, alertes.size()); i++) {
            corps.add(Component.text(Alertes.ligne(alertes.get(i))));
        }
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("joueur.bouton-invsee", "<aqua>Inventaire"), null, p -> plugin.inventaires().ouvrir(p, cible, false)));
        boutons.add(gui.button(t("joueur.bouton-ecsee", "<dark_purple>Coffre de l'Ender"), null,
                p -> plugin.inventaires().ouvrir(p, cible, true)));
        if (s == null) {
            boutons.add(gui.button(t("joueur.bouton-suspendre", "<red>Suspendre"), null, p -> suspendre(p, cible)));
        } else {
            boutons.add(gui.button(t("joueur.bouton-lever", "<green>Lever la suspension"), null, p -> gui.confirm(p,
                    t("joueur.titre-lever", "<green><bold>Lever la suspension"),
                    t("joueur.lever-texte", "<white><nom> pourra de nouveau se connecter à Event.", "nom", nom),
                    q -> {
                        plugin.suspensions().lever(uuid, q.getName());
                        joueur(q, cible);
                    }, q -> joueur(q, cible))));
        }
        if (alertes.size() > 8) {
            boutons.add(gui.button(t("joueur.bouton-toutes", "<yellow>Toutes les alertes"), null, p -> toutes(p, cible, 0)));
        }
        boutons.add(retour(this::accueil));
        gui.open(staff, t("joueur.titre", "<dark_red><bold><nom>", "nom", nom), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void toutes(Player staff, OfflinePlayer cible, int page) {
        List<Alerte> alertes = plugin.alertes().duJoueur(cible.getUniqueId());
        int pages = Math.max(1, (alertes.size() + 15 - 1) / 15);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<Component> corps = new ArrayList<>();
        for (int i = p * 15; i < Math.min(alertes.size(), (p + 1) * 15); i++) {
            corps.add(Component.text(Alertes.ligne(alertes.get(i))));
        }
        List<ActionButton> boutons = new ArrayList<>();
        if (p > 0) {
            boutons.add(gui.button(t("menu.precedent", "<yellow>Page précédente"), null, j -> toutes(j, cible, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("menu.suivant", "<yellow>Page suivante"), null, j -> toutes(j, cible, p + 1)));
        }
        boutons.add(retour(j -> joueur(j, cible)));
        gui.open(staff, t("joueur.titre-alertes", "<yellow><bold>Alertes de <nom>", "nom",
                cible.getName() == null ? "?" : cible.getName()), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void suspendre(Player staff, OfflinePlayer cible) {
        String nom = cible.getName() == null ? "?" : cible.getName();
        ActionButton valider = gui.form(t("joueur.bouton-suspendre-ok", "<red>Suspendre"), null, (p, vue) -> {
            String raison = vue.getText("raison") == null ? "" : vue.getText("raison").trim();
            plugin.suspensions().suspendre(cible.getUniqueId(), nom, raison.isEmpty() ? "sans raison précisée" : raison,
                    p.getName());
            joueur(p, cible);
        });
        gui.open(staff, t("joueur.titre-suspendre", "<red><bold>Suspendre <nom>", "nom", nom),
                List.of(t("joueur.suspendre-aide", "<gray>Il ne pourra plus se connecter à Event (message : « Une erreur "
                        + "inhabituelle est survenue, contacte le staff. »), jusqu'à ce que le staff lève la suspension.")),
                List.of(gui.text("raison", t("joueur.champ-raison", "Raison (pour le staff)"), "", 100)),
                List.of(valider, retour(p -> joueur(p, cible))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ listes

    private void suspendus(Player staff) {
        Map<UUID, Suspension> liste = plugin.suspensions().toutes();
        List<ActionButton> boutons = new ArrayList<>();
        liste.forEach((uuid, s) -> boutons.add(gui.button(Component.text(s.nom), t("suspendus.info", "<gray><raison>",
                "raison", s.raison), p -> joueur(p, plugin.getServer().getOfflinePlayer(uuid)))));
        boutons.add(retour(this::accueil));
        gui.open(staff, t("suspendus.titre", "<red><bold>Suspendus"), List.of(liste.isEmpty()
                        ? t("suspendus.aucun", "<gray>Aucun joueur suspendu.")
                        : t("suspendus.aide", "<gray>Choisis un joueur : alertes, inventaire, lever la suspension.")),
                List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void morts(Player staff) {
        List<Component> corps = new ArrayList<>();
        for (String l : plugin.morts().dernieres(15)) {
            corps.add(Component.text(l));
        }
        if (corps.isEmpty()) {
            corps.add(t("morts.aucune", "<gray>Aucune mort d'entité importante depuis le démarrage (journal complet : "
                    + "plugins/KS_AntiCheat/morts.log)."));
        }
        gui.open(staff, t("morts.titre", "<white><bold>Morts d'entités importantes"), corps, List.of(),
                List.of(retour(this::accueil)), gui.close(), 1);
        lang.saveIfNeeded();
    }

    /** Onglet « Minage » (rubrique Modération) : filons rares cachés par joueur, 24 dernières heures. */
    void minage(Player staff, Consumer<Player> retourModeration) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        memoriserRetour(staff, retourModeration);
        List<Component> corps = new ArrayList<>();
        corps.add(t("minage.aide", "<gray>Seuls comptent les diamants, émeraudes et débris antiques cachés (pas à l'air "
                + "libre) ; un filon compte une fois ; taux pour 1 000 blocs de roche minés. Du plus suspect au moins suspect."));
        List<String> lignes = plugin.minage().resume24h();
        for (int i = 0; i < Math.min(15, lignes.size()); i++) {
            corps.add(Component.text(lignes.get(i)));
        }
        if (lignes.isEmpty()) {
            corps.add(t("minage.aucun", "<gray>Aucun filon rare caché trouvé ces 24 dernières heures."));
        }
        List<ActionButton> boutons = new ArrayList<>();
        Consumer<Player> versModeration = retours.get(staff.getUniqueId());
        boutons.add(versModeration != null ? retour(versModeration) : retour(this::accueil));
        gui.open(staff, t("minage.titre", "<gold><bold>Minage (x-ray)"), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void consultations(Player staff) {
        List<Component> corps = new ArrayList<>();
        for (String l : plugin.inventaires().dernieresConsultations(15)) {
            corps.add(Component.text(l));
        }
        if (corps.isEmpty()) {
            corps.add(t("consultations.aucune", "<gray>Aucune consultation pour l'instant."));
        }
        gui.open(staff, t("consultations.titre", "<white><bold>Journal invsee / ecsee"), corps, List.of(),
                List.of(retour(this::accueil)), gui.close(), 1);
        lang.saveIfNeeded();
    }
}
