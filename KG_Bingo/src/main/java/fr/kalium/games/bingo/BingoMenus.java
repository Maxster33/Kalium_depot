package fr.kalium.games.bingo;

import fr.kalium.games.KalGames;
import fr.kalium.games.gui.Gui;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Menus Bingo du hub kal-games : bouton "Bingo" du menu Mini-jeux (creer / rejoindre / liste des parties en salle
 * d'attente) et Parametres > Bingo (duree maximale). Deplaces tels quels de KalGames (PlayerMenus / AdminMenus,
 * 1.10.1 a 1.10.6) dans KG_Bingo 1.0.0, sans changement de comportement : memes textes (cles "bingo." et
 * "admin.bingo-" du lang.yml de KalGames), meme apparence (assistant Gui de KalGames). Les boutons sont
 * fournis a KG_Menu par KGBingo.onEnable (MenuProvider, 1.3.0).
 */
final class BingoMenus {

    private final KGBingo plugin;
    private final KalGames kg;
    private final Gui gui;

    BingoMenus(KGBingo plugin, KalGames kg) {
        this.plugin = plugin;
        this.kg = kg;
        this.gui = kg.gui();
    }

    Component t(String key, String def, Object... pairs) {
        return kg.t(key, def, pairs);
    }

    private boolean guard(Player player) {
        if (!kg.isAdmin(player)) {
            kg.tell(player, "admin.denied", "<red>Réservé aux modérateurs.");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ menu Bingo du hub (joueurs)

    /** Nombre maximum de parties encore en salle d'attente affichees dans le menu Bingo - au-dela,
     *  les plus anciennes ne s'affichent pas (choix confirme par l'utilisateur, 24/09/2026). */
    private static final int BINGO_OPEN_PARTIES_SHOWN = 6;

    /**
     * Menu Bingo : creer une partie (hote, code genere), en rejoindre une avec un code, OU cliquer
     * directement sur l'une des parties encore en salle d'attente listees ci-dessous (AJOUTE le
     * 24/09/2026, demande explicite de l'utilisateur : "il faut pouvoir voir les parties qui sont
     * creees et encore en salle d'attente dans le menu kalgames de maniere a pouvoir la rejoindre
     * facilement") - puis transfert vers le serveur Bingo (bingo.server-name). Reutilise exactement
     * la meme logique que /bingo create et /bingo join (BingoPartyManager, voir BingoCommand) - les
     * deux commandes restent disponibles en parallele, ce menu n'est qu'un autre point d'entree vers
     * le meme systeme.
     */
    void openBingoMenu(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.button(t("bingo.create", "<green>Créer une partie Bingo"),
                t("bingo.create-tip", "<gray>Choisissez le nombre d'équipes et de joueurs par équipe, puis devenez l'hôte."),
                this::openBingoCreate));
        buttons.add(gui.form(t("bingo.join", "<aqua>Rejoindre avec un code"), null, (p, view) -> {
            String code = view.getText("code");
            bingoJoin(p, code == null ? "" : code);
        }));

        // Parties encore en salle d'attente, cliquables directement (pas besoin de connaitre le
        // code) - voir BingoPartyManager.openParties, alimentee par PartyStatusNotifier cote
        // KalBingo des qu'une partie demarre/est annulee. Une partie deja pleine reste affichee
        // (visibilite confirmee par l'utilisateur) mais son bouton n'inscrit pas le joueur : il est
        // juste informe qu'elle est complete. "Pleine" = capacite REELLE de CETTE partie
        // (party.maxPlayers(), equipes x taille choisies par l'hote), PAS le plafond global
        // bingo.max-party-size (corrige le 24/09/2026 - affichait par ex. "2/16" pour une partie a
        // 2 equipes d'1 joueur, qui ne peut en realite accueillir que 2 joueurs).
        for (BingoParty party : plugin.parties().openParties(BINGO_OPEN_PARTIES_SHOWN)) {
            buttons.add(bingoPartyButton(party));
        }

        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, kg.menus()::openGames));
        List<DialogInput> inputs = List.of(gui.text("code", t("bingo.join-input", "Code de la partie"), "", 8));
        List<Component> body = new ArrayList<>();
        body.add(t("bingo.body", "<gray>Créez une partie (vous serez l'hôte), rejoignez-en une avec un code, "
                + "ou cliquez directement sur une partie encore en salle d'attente ci-dessous."));
        gui.open(player, t("bingo.title", "<gold><bold>Bingo"), body, inputs, buttons, gui.close(), 1);
    }

    /** Un bouton par partie encore en salle d'attente listee dans openBingoMenu - voir ce menu. */
    private ActionButton bingoPartyButton(BingoParty party) {
        String hostName = Bukkit.getOfflinePlayer(party.host()).getName();
        int current = party.roster().size();
        int maxPartySize = party.maxPlayers();
        boolean full = current >= maxPartySize;
        // 1.4.0 : le pseudo de l'hote est un parametre (<host>) : l'ancienne cle « bingo.party-entry » contenait le pseudo
        // ecrit en dur et avait ete figee dans le lang.yml du serveur au premier affichage (toujours le meme pseudo).
        // Nouvelle cle, et nombre d'equipes / de joueurs par equipe plus clair qu'avant (« 2x4 équipes »).
        Component label = t("bingo.party-entry-2",
                "<yellow><host></yellow> <gray>· <teams> équipe(s) de <size> · <current>/<max> joueurs</gray>",
                "host", hostName == null ? "?" : hostName, "teams", party.teamCount(), "size", party.teamSize(),
                "current", current, "max", maxPartySize);
        if (full) {
            label = label.append(t("bingo.party-entry-full", " <red>(complet)"));
        }
        Component tooltip = t("bingo.party-entry-tip", full
                ? "<red>Cette partie est déjà complète."
                : "<gray>Cliquez pour rejoindre directement cette partie.");
        if (full) {
            return gui.button(label, tooltip, p ->
                    p.sendMessage(kg.prefix().append(t("bingo.party-full", "<red>Cette partie est déjà complète."))));
        }
        return gui.button(label, tooltip, p -> bingoJoin(p, party.code()));
    }

    /**
     * Ecran de creation : nombre d'equipes / joueurs par equipe, choisis par l'hote pour CETTE
     * partie (bornes par bingo.max-team-count/-size de config.yml - 4x4 par defaut, confirme par
     * l'utilisateur). Demande explicite : "je puisse directement configurer le nombre d'équipe et
     * le nombre de personne par équipe" a la creation.
     */
    public void openBingoCreate(Player player) {
        int maxTeamCount = Math.max(1, plugin.getConfig().getInt("bingo.max-team-count", 4));
        int maxTeamSize = Math.max(1, plugin.getConfig().getInt("bingo.max-team-size", 4));
        int defaultCount = Math.max(1, Math.min(maxTeamCount, plugin.getConfig().getInt("bingo.default-team-count", 2)));
        int defaultSize = Math.max(1, Math.min(maxTeamSize, plugin.getConfig().getInt("bingo.default-team-size", 4)));

        // Duree de partie (demande explicite de l'utilisateur, 23/09/2026) : choisie par l'hote en
        // MINUTES (plus lisible qu'en secondes dans un formulaire), bornee par bingo.min/max-duration-
        // minutes - meme principe que teamCount/teamSize ci-dessus. Pre-remplie AVEC LE MAXIMUM admin
        // (bingo.max-duration-minutes, modifiable via Parametres > Bingo, voir AdminMenus.openBingoSettings) :
        // demande explicite de l'utilisateur, "le createur de la partie doit pouvoir réduire le temps
        // mais ne doit pas pouvoir dépasser le temps maximum défini par les opérateurs" - l'hote ne peut
        // donc que reduire cette valeur, jamais la depasser (borne haute du champ ci-dessous).
        int minDurationMinutes = Math.max(1, plugin.getConfig().getInt("bingo.min-duration-minutes", 5));
        int maxDurationMinutes = Math.max(minDurationMinutes, plugin.getConfig().getInt("bingo.max-duration-minutes", 60));
        int defaultDurationMinutes = maxDurationMinutes;

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(gui.number("teamCount", t("bingo.create-teams", "Nombre d'équipes"), 1, maxTeamCount, defaultCount, 1));
        inputs.add(gui.number("teamSize", t("bingo.create-teamsize", "Joueurs par équipe"), 1, maxTeamSize, defaultSize, 1));
        inputs.add(gui.number("duration", t("bingo.create-duration", "Durée de la partie (minutes, sauf blackout)"),
                minDurationMinutes, maxDurationMinutes, defaultDurationMinutes, 5));
        // 1.1.0 - demande explicite de LeKiwi06 (24/09/2026) : mode (bingos a achever, 3 a 12, avec chrono ; ou
        // blackout, grille complete sans chrono) et composition de la grille par difficulte (25 cases ; par defaut
        // 10 faciles, 10 normaux, 5 difficiles, 0 extreme - "trop dur pour des debutants").
        inputs.add(gui.choice("mode", t("bingo.create-mode", "Mode de jeu"), List.of("BINGOS", "BLACKOUT"),
                List.of(t("bingo.mode-bingos", "Bingos (lignes, colonnes, diagonales) avec chrono"),
                        t("bingo.mode-blackout", "Blackout : grille complète, sans chrono")), "BINGOS"));
        inputs.add(gui.number("bingos", t("bingo.create-bingos", "Bingos à achever pour gagner"), 3, 12, 3, 1));
        inputs.add(gui.number("easy", t("bingo.create-easy", "Objectifs faciles"), 0, GRID_CELLS, 10, 1));
        inputs.add(gui.number("medium", t("bingo.create-medium", "Objectifs normaux"), 0, GRID_CELLS, 10, 1));
        inputs.add(gui.number("hard", t("bingo.create-hard", "Objectifs difficiles"), 0, GRID_CELLS, 5, 1));
        inputs.add(gui.number("extreme", t("bingo.create-extreme", "Objectifs extrêmes"), 0, GRID_CELLS, 0, 1));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("bingo.create-confirm", "<green>Créer la partie"), null, (p, view) -> {
            Float teamCountValue = view.getFloat("teamCount");
            Float teamSizeValue = view.getFloat("teamSize");
            Float durationValue = view.getFloat("duration");
            int teamCount = teamCountValue == null ? defaultCount : Math.round(teamCountValue);
            int teamSize = teamSizeValue == null ? defaultSize : Math.round(teamSizeValue);
            int durationMinutes = durationValue == null ? defaultDurationMinutes : Math.round(durationValue);
            String mode = "BLACKOUT".equals(view.getText("mode")) ? "BLACKOUT" : "BINGOS";
            int bingos = intOf(view.getFloat("bingos"), 3);
            int easy = intOf(view.getFloat("easy"), 10);
            int medium = intOf(view.getFloat("medium"), 10);
            int hard = intOf(view.getFloat("hard"), 5);
            int extreme = intOf(view.getFloat("extreme"), 0);
            if (easy + medium + hard + extreme != GRID_CELLS) {
                p.sendMessage(kg.prefix().append(t("bingo.create-bad-grid",
                        "<red>La grille doit contenir exactement <cells> objectifs (vous en avez choisi <total>).",
                        "cells", GRID_CELLS, "total", easy + medium + hard + extreme)));
                openBingoCreate(p);
                return;
            }
            String rules = "mode=" + mode + ";bingos=" + bingos + ";easy=" + easy + ";medium=" + medium
                    + ";hard=" + hard + ";extreme=" + extreme;
            bingoCreate(p, teamCount, teamSize, durationMinutes, rules,
                    ("BLACKOUT".equals(mode) ? "blackout" : bingos + " bingos") + ", " + easy + " F / " + medium + " N / "
                            + hard + " D / " + extreme + " X");
        }));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, this::openBingoMenu));
        List<Component> body = List.of(t("bingo.create-body", "<gray>Réglez la partie puis créez-la. Vous serez l'hôte."));
        gui.open(player, t("bingo.create-title", "<gold><bold>Nouvelle partie Bingo"), body, inputs, buttons, gui.close(), 1);
    }

    /** Nombre de cases d'une grille 5x5 (la composition choisie doit y correspondre). */
    private static final int GRID_CELLS = 25;

    private static int intOf(Float value, int fallback) {
        return value == null ? fallback : Math.round(value);
    }

    private void bingoCreate(Player player, int teamCount, int teamSize, int durationMinutes, String rules, String summary) {
        BingoParty party = plugin.parties().create(player, teamCount, teamSize,
                java.time.Duration.ofMinutes(durationMinutes));
        party.setRules(rules);
        player.sendMessage(kg.prefix().append(t("bingo.created-rules", "<gray>Règles : <white><rules>", "rules", summary)));
        player.sendMessage(kg.prefix().append(t("bingo.created",
                "<green>Partie Bingo créée (<teams> équipe(s) x <size> joueur(s), <minutes> min). "
                        + "Code à partager : <white><bold><code></bold>",
                "teams", party.teamCount(), "size", party.teamSize(),
                "minutes", party.duration().toMinutes(), "code", party.code())));
        plugin.parties().transferToBingo(player);
    }

    private void bingoJoin(Player player, String code) {
        if (code.isBlank()) {
            player.sendMessage(kg.prefix().append(t("bingo.join-empty", "<red>Merci d'indiquer un code.")));
            return;
        }
        BingoParty party = plugin.parties().join(player, code);
        if (party == null) {
            player.sendMessage(kg.prefix().append(t("bingo.join-invalid", "<red>Code invalide, partie pleine ou introuvable.")));
            return;
        }
        player.sendMessage(kg.prefix().append(t("bingo.joined", "<green>Partie rejointe, transfert en cours…")));
        plugin.parties().transferToBingo(player);
    }

    // ------------------------------------------------------------------ Parametres > Bingo (moderateurs)

    /**
     * Reglage admin : duree MAXIMALE (minutes) d'une partie Bingo - demande explicite de
     * l'utilisateur, "le temps par defaut maximum doit être 1h, cette limite doit pouvoir etre
     * changee via les parametres de kalgames. le createur de la partie doit pouvoir réduire le
     * temps mais ne doit pas pouvoir dépasser le temps maximum défini par les opérateurs." Le
     * formulaire de creation (PlayerMenus.openBingoCreate) se pre-remplit avec cette valeur et
     * plafonne le choix de l'hote dessus ; BingoPartyManager.create() applique la meme borne cote
     * serveur (jamais de confiance au client). Persiste directement dans config.yml (bingo.max-
     * duration-minutes), pas via le systeme Minigame/SettingSpec : Bingo n'est pas un Minigame.
     */
    void openBingoSettings(Player player) {
        if (!guard(player)) {
            return;
        }
        int minDurationMinutes = Math.max(1, plugin.getConfig().getInt("bingo.min-duration-minutes", 5));
        int currentMax = Math.max(minDurationMinutes, plugin.getConfig().getInt("bingo.max-duration-minutes", 60));
        List<DialogInput> inputs = List.of(
                gui.number("maxDuration", t("admin.bingo-max-duration", "Durée maximale d'une partie (minutes)"),
                        minDurationMinutes, 24 * 60, currentMax, 5));
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(gui.form(t("admin.save", "<green>Enregistrer"), null, (p, view) -> {
            if (!guard(p)) {
                return;
            }
            Float value = view.getFloat("maxDuration");
            int newMax = Math.max(minDurationMinutes, value == null ? currentMax : Math.round(value));
            plugin.getConfig().set("bingo.max-duration-minutes", newMax);
            plugin.saveConfig();
            kg.tell(p, "admin.saved", "<green>Enregistré.");
            openBingoSettings(p);
        }));
        buttons.add(gui.button(t("menu.back", "<gray>Retour"), null, kg.admin()::openHome));
        List<Component> body = List.of(t("admin.bingo-body",
                "<gray>L'hôte d'une partie peut réduire la durée à la création, jamais la dépasser."));
        gui.open(player, t("admin.bingo-title", "<light_purple><bold>Bingo"), body, inputs, buttons, gui.close(), 1);
    }
}
