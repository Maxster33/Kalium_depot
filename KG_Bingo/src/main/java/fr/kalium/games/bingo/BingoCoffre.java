package fr.kalium.games.bingo;

import fr.kalium.games.KalGames;
import fr.kalium.menu.api.Contenant;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.11.0 - réglages d'une partie Bingo en coffre (demande de LeKiwi06, 20/10/2026 : menus de Kal-Games en coffre ;
 * « ouvrir l'interface de jauge quand on clique sur l'item quand c'est nécessaire, par exemple [...] la répartition
 * des items dans le bingo » ; Échap ramène à la page précédente et enregistre les jauges).
 *
 * Un seul coffre au lieu des deux fenêtres : équipes, joueurs par équipe, mode, durée, bingos à achever, composition
 * de la grille. Un nombre s'ouvre dans une petite fenêtre avec une jauge ; les 4 jauges de la composition (liées : leur
 * total fait la grille) sont dans une seule fenêtre. Les derniers réglages de chaque joueur sont retenus
 * (plugins/KG_Bingo/reglages.yml) pour la création suivante et « Recréer ma dernière partie ».
 */
final class BingoCoffre {

    private enum Ecran { REGLAGES }

    /** Réglages en cours (ou retenus) d'une partie Bingo. */
    static final class Reglages {
        int equipes;
        int taille;
        String mode;
        int duree;
        int bingos;
        int faciles;
        int normaux;
        int difficiles;
        int extremes;
        Consumer<Player> retour;

        int total() {
            return faciles + normaux + difficiles + extremes;
        }
    }

    static final int GRID_CELLS = 25;

    private final KGBingo plugin;
    private final KalGames kg;
    private final Map<UUID, Reglages> brouillons = new HashMap<>();
    private final File fichier;
    private final YamlConfiguration retenus;

    BingoCoffre(KGBingo plugin, KalGames kg) {
        this.plugin = plugin;
        this.kg = kg;
        this.fichier = new File(plugin.getDataFolder(), "reglages.yml");
        this.retenus = YamlConfiguration.loadConfiguration(fichier);
    }

    private Component t(String key, String def, Object... pairs) {
        return kg.t(key, def, pairs);
    }

    void forget(UUID uuid) {
        brouillons.remove(uuid);
    }

    // ------------------------------------------------------------------ bornes (config.yml)

    private int maxEquipes() {
        return Math.max(1, plugin.getConfig().getInt("bingo.max-team-count", 4));
    }

    private int maxTaille() {
        return Math.max(1, plugin.getConfig().getInt("bingo.max-team-size", 4));
    }

    private int minDuree() {
        return Math.max(1, plugin.getConfig().getInt("bingo.min-duration-minutes", 5));
    }

    private int maxDuree() {
        return Math.max(minDuree(), plugin.getConfig().getInt("bingo.max-duration-minutes", 60));
    }

    private int maxBingos(Reglages r) {
        return r.equipes == 1 ? 10 : 12;
    }

    private static int borne(int valeur, int min, int max) {
        return Math.max(min, Math.min(max, valeur));
    }

    /** Remet chaque réglage dans ses bornes et le mode en accord avec le nombre d'équipes. */
    private void corriger(Reglages r) {
        r.equipes = borne(r.equipes, 1, maxEquipes());
        r.taille = borne(r.taille, 1, maxTaille());
        r.duree = borne(r.duree, minDuree(), maxDuree());
        if (r.equipes > 1 && "CHRONO".equals(r.mode)) {
            r.mode = "BINGOS"; // le contre la montre n'existe qu'à une équipe
        }
        if (!"BINGOS".equals(r.mode) && !"BLACKOUT".equals(r.mode) && !"CHRONO".equals(r.mode)) {
            r.mode = r.equipes == 1 ? "CHRONO" : "BINGOS";
        }
        r.bingos = borne(r.bingos, 3, maxBingos(r));
        r.faciles = borne(r.faciles, 0, GRID_CELLS);
        r.normaux = borne(r.normaux, 0, GRID_CELLS);
        r.difficiles = borne(r.difficiles, 0, GRID_CELLS);
        r.extremes = borne(r.extremes, 0, GRID_CELLS);
    }

    /** Derniers réglages du joueur, sinon les valeurs par défaut de la config. */
    Reglages depart(Player joueur) {
        Reglages r = new Reglages();
        String base = joueur.getUniqueId() + ".";
        r.equipes = retenus.getInt(base + "equipes", plugin.getConfig().getInt("bingo.default-team-count", 2));
        r.taille = retenus.getInt(base + "taille", plugin.getConfig().getInt("bingo.default-team-size", 4));
        r.mode = retenus.getString(base + "mode", r.equipes == 1 ? "CHRONO" : "BINGOS");
        r.duree = retenus.getInt(base + "duree", maxDuree());
        r.bingos = retenus.getInt(base + "bingos", 3);
        r.faciles = retenus.getInt(base + "faciles", 10);
        r.normaux = retenus.getInt(base + "normaux", 10);
        r.difficiles = retenus.getInt(base + "difficiles", 5);
        r.extremes = retenus.getInt(base + "extremes", 0);
        corriger(r);
        return r;
    }

    private void retenir(Player joueur, Reglages r) {
        String base = joueur.getUniqueId() + ".";
        retenus.set(base + "equipes", r.equipes);
        retenus.set(base + "taille", r.taille);
        retenus.set(base + "mode", r.mode);
        retenus.set(base + "duree", r.duree);
        retenus.set(base + "bingos", r.bingos);
        retenus.set(base + "faciles", r.faciles);
        retenus.set(base + "normaux", r.normaux);
        retenus.set(base + "difficiles", r.difficiles);
        retenus.set(base + "extremes", r.extremes);
        retenus.set(base + "quand", System.currentTimeMillis());
        try {
            plugin.getDataFolder().mkdirs();
            retenus.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("reglages.yml non enregistré : " + e.getMessage());
        }
    }

    /** Date de la dernière partie créée par ce joueur (millisecondes), 0 s'il n'en a jamais créé. */
    long derniere(Player joueur) {
        return retenus.getLong(joueur.getUniqueId() + ".quand", 0L);
    }

    // ------------------------------------------------------------------ création

    /** « Créer une partie » &gt; Bingo : réglages en coffre (ou les fenêtres, si le joueur a choisi les fenêtres). */
    void creer(Player joueur, Consumer<Player> retour) {
        long attente = plugin.parties().creationWaitSeconds(joueur); // 1.7.0 : 2 parties par heure
        if (attente > 0) {
            dire(joueur, BingoPartyManager.creationWaitText(attente));
            return;
        }
        Reglages r = depart(joueur);
        r.retour = retour;
        brouillons.put(joueur.getUniqueId(), r);
        Contenant.apres(() -> reglages(joueur));
    }

    /** Recrée la dernière partie du joueur (mêmes réglages) : null si c'est fait, sinon le refus. */
    Component recreer(Player joueur) {
        Reglages r = depart(joueur);
        return lancer(joueur, r);
    }

    private void dire(Player joueur, String texte) {
        Component c = Component.text(texte);
        if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant) {
            Contenant.message(joueur, c, true);
        } else {
            joueur.sendMessage(kg.prefix().append(c));
        }
    }

    private void dire(Player joueur, Component texte, boolean refus) {
        if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant) {
            Contenant.message(joueur, texte, refus);
        } else {
            joueur.sendMessage(kg.prefix().append(texte));
        }
    }

    private Component nomMode(String mode) {
        return switch (mode) {
            case "BLACKOUT" -> t("bingo.mode-blackout-court", "Blackout, sans chrono");
            case "CHRONO" -> t("bingo.mode-chrono-court", "Contre la montre, 1 équipe");
            default -> t("bingo.mode-bingos-court", "Bingos, avec chrono");
        };
    }

    private void reglages(Player joueur) {
        Reglages r = brouillons.get(joueur.getUniqueId());
        if (r == null) {
            return;
        }
        corriger(r);
        Contenant menu = Contenant.pour(joueur, 4, t("bingo.coffre-titre", "<dark_gray>Nouvelle partie Bingo"), Ecran.REGLAGES);

        List<Component> aide = new ArrayList<>();
        aide.addAll(Contenant.lignes(t("bingo.coffre-aide-bingos", "<gray>Bingos : lignes, colonnes ou diagonales à achever, avec chrono.")));
        aide.addAll(Contenant.lignes(t("bingo.coffre-aide-blackout", "<gray>Blackout : grille complète, sans chrono.")));
        if (r.equipes == 1) {
            aide.addAll(Contenant.lignes(t("bingo.coffre-aide-chrono", "<gray>Contre la montre (1 équipe) : 10 min au départ, "
                    + "du temps en plus à chaque objectif, bonus selon le temps restant.")));
        }
        menu.poser(4, Contenant.objet(Material.FILLED_MAP, t("bingo.hub-entry", "<gold><bold>Bingo"), aide), null);

        List<Component> jaugeClic = List.of(t("coffre.jauge-clic", "<yellow>Clic : régler avec la jauge"));
        // Rangée 2 : équipes, joueurs par équipe, mode, durée, bingos.
        menu.poser(18, Contenant.objet(new ItemStack(Material.WHITE_BANNER, r.equipes),
                t("bingo.coffre-equipes", "<white>Équipes : <aqua><n>", "n", r.equipes), jaugeClic),
                p -> jauge(p, "equipes", "Nombre d'équipes", 1, maxEquipes(), r.equipes, 1));
        menu.poser(20, Contenant.objet(new ItemStack(Material.PLAYER_HEAD, r.taille),
                t("bingo.coffre-taille", "<white>Joueurs par équipe : <aqua><n>", "n", r.taille), jaugeClic),
                p -> jauge(p, "taille", "Joueurs par équipe", 1, maxTaille(), r.taille, 1));
        List<String> modes = new ArrayList<>(List.of("BINGOS", "BLACKOUT"));
        if (r.equipes == 1) {
            modes.add("CHRONO");
        }
        menu.poser(22, Contenant.objet(Material.COMPASS, t("bingo.coffre-mode", "<white>Mode : <aqua><mode>", "mode", nomMode(r.mode)),
                List.of(t("coffre.mode-clic", "<yellow>Clic : mode suivant"))), p -> {
            int i = modes.indexOf(r.mode);
            r.mode = modes.get((i + 1) % modes.size());
            reglages(p);
        });
        boolean dureeUtile = "BINGOS".equals(r.mode);
        List<Component> lignesDuree = new ArrayList<>();
        if (!dureeUtile) {
            lignesDuree.add(t("bingo.coffre-duree-inutile", "<gray>Ne compte pas dans ce mode."));
        }
        lignesDuree.addAll(jaugeClic);
        menu.poser(24, Contenant.objet(new ItemStack(Material.CLOCK, Math.max(1, Math.min(64, r.duree))),
                t("bingo.coffre-duree", "<white>Durée : <aqua><n> min", "n", r.duree), lignesDuree),
                p -> jauge(p, "duree", "Durée (minutes)", minDuree(), maxDuree(), r.duree, 5));
        List<Component> lignesBingos = new ArrayList<>();
        if ("BLACKOUT".equals(r.mode)) {
            lignesBingos.add(t("bingo.coffre-bingos-inutile", "<gray>Ne compte pas en blackout."));
        }
        lignesBingos.addAll(jaugeClic);
        menu.poser(26, Contenant.objet(new ItemStack(Material.TARGET, r.bingos),
                t("bingo.coffre-bingos", "<white>Bingos à achever : <aqua><n>", "n", r.bingos), lignesBingos),
                p -> jauge(p, "bingos", "Bingos à achever", 3, maxBingos(r), r.bingos, 1));
        // Composition de la grille : une seule fenêtre avec les 4 jauges.
        boolean juste = r.total() == GRID_CELLS;
        List<Component> lignesGrille = new ArrayList<>();
        lignesGrille.add(t("bingo.coffre-grille-faciles", "<green>Faciles : <white><n>", "n", r.faciles));
        lignesGrille.add(t("bingo.coffre-grille-normaux", "<yellow>Normaux : <white><n>", "n", r.normaux));
        lignesGrille.add(t("bingo.coffre-grille-difficiles", "<gold>Difficiles : <white><n>", "n", r.difficiles));
        lignesGrille.add(t("bingo.coffre-grille-extremes", "<red>Extrêmes : <white><n>", "n", r.extremes));
        lignesGrille.add(juste ? t("bingo.coffre-grille-total", "<gray>Total : <green><n>/<cells>", "n", r.total(), "cells", GRID_CELLS)
                : t("bingo.coffre-grille-total-faux", "<gray>Total : <red><n>/<cells> <gray>(il en faut <cells>)", "n", r.total(), "cells", GRID_CELLS));
        lignesGrille.add(t("bingo.coffre-grille-clic", "<yellow>Clic : régler la répartition"));
        menu.poser(13, Contenant.objet(new ItemStack(Material.MAP), t("bingo.coffre-grille", "<white>Composition de la grille"),
                lignesGrille), this::grille);

        menu.barre();
        menu.poser(menu.bas(4), Contenant.objet(Material.LIME_CONCRETE, t("coffre.creer", "<green><bold>Créer la partie"),
                Contenant.lignes(t("bingo.coffre-creer-tip", "<gray>Vous en êtes l'hôte ; vous partez sur le serveur du Bingo."))), p -> {
            Reglages actuel = brouillons.get(p.getUniqueId());
            if (actuel == null) {
                return;
            }
            if (actuel.total() != GRID_CELLS) {
                dire(p, t("bingo.create-bad-grid",
                        "<red>La grille doit contenir exactement <cells> objectifs (vous en avez choisi <total>).",
                        "cells", GRID_CELLS, "total", actuel.total()), true);
                return;
            }
            Contenant.apres(() -> {
                Component refus = lancer(p, actuel);
                if (refus != null) {
                    dire(p, refus, true);
                    return;
                }
                brouillons.remove(p.getUniqueId());
                p.closeInventory();
            });
        });
        Consumer<Player> retour = p -> {
            brouillons.remove(p.getUniqueId());
            if (r.retour != null) {
                r.retour.accept(p);
            } else {
                Contenant.fermer(p);
            }
        };
        menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, t("coffre.retour", "<white>Retour"), List.of()), retour);
        menu.ouvrir(joueur);
        kg.kgMenu().echap(joueur, menu, retour); // Échap = « Retour »
    }

    /** Un réglage chiffré : petite fenêtre avec une jauge ; « Valider » ou Échap enregistre et ramène au coffre. */
    private void jauge(Player joueur, String cle, String libelle, int min, int max, int valeur, int pas) {
        Contenant.saisie(joueur, () -> kg.gui().open(joueur, kg.lang().parse("<white><bold>" + libelle),
                List.of(t("coffre.jauge-body-echap", "<gray>Faites glisser la jauge. Valider ou Échap : enregistré.")),
                List.of(kg.gui().number("valeur", kg.lang().parse(libelle), min, max, valeur, pas)),
                List.of(),
                kg.gui().form(t("coffre.jauge-ok", "<green>Valider"), null, (p, vue) -> {
                    Reglages r = brouillons.get(p.getUniqueId());
                    Float choisi = vue.getFloat("valeur");
                    if (r != null && choisi != null) {
                        int v = borne(Math.round(choisi), min, max);
                        switch (cle) {
                            case "equipes" -> {
                                if (r.equipes > 1 && v == 1 && "BINGOS".equals(r.mode)) {
                                    r.mode = "CHRONO"; // 1.7.2 : en passant à une équipe, le contre la montre par défaut
                                }
                                r.equipes = v;
                            }

                            case "taille" -> r.taille = v;
                            case "duree" -> r.duree = v;
                            case "bingos" -> r.bingos = v;
                            default -> {
                            }
                        }
                    }
                    reglages(p);
                }), 1));
    }

    /** Composition de la grille : les 4 jauges dans une seule fenêtre (leur total doit faire 25). */
    private void grille(Player joueur) {
        Reglages r = brouillons.get(joueur.getUniqueId());
        if (r == null) {
            return;
        }
        List<DialogInput> champs = List.of(
                kg.gui().number("easy", t("bingo.create-easy", "Objectifs faciles"), 0, GRID_CELLS, r.faciles, 1),
                kg.gui().number("medium", t("bingo.create-medium", "Objectifs normaux"), 0, GRID_CELLS, r.normaux, 1),
                kg.gui().number("hard", t("bingo.create-hard", "Objectifs difficiles"), 0, GRID_CELLS, r.difficiles, 1),
                kg.gui().number("extreme", t("bingo.create-extreme", "Objectifs extrêmes"), 0, GRID_CELLS, r.extremes, 1));
        Contenant.saisie(joueur, () -> kg.gui().open(joueur, t("bingo.grille-titre", "<white><bold>Composition de la grille"),
                List.of(t("bingo.grille-body", "<gray>Le total doit faire <white><cells></white> objectifs. Valider ou Échap : enregistré.",
                        "cells", GRID_CELLS)),
                champs, List.of(),
                kg.gui().form(t("coffre.jauge-ok", "<green>Valider"), null, (p, vue) -> {
                    Reglages actuel = brouillons.get(p.getUniqueId());
                    if (actuel != null) {
                        actuel.faciles = valeur(vue.getFloat("easy"), actuel.faciles);
                        actuel.normaux = valeur(vue.getFloat("medium"), actuel.normaux);
                        actuel.difficiles = valeur(vue.getFloat("hard"), actuel.difficiles);
                        actuel.extremes = valeur(vue.getFloat("extreme"), actuel.extremes);
                    }
                    reglages(p);
                    if (actuel != null && actuel.total() != GRID_CELLS) {
                        dire(p, t("bingo.grille-faux", "<red>Total : <n>/<cells> objectifs : ajustez la répartition.",
                                "n", actuel.total(), "cells", GRID_CELLS), true);
                    }
                }), 1));
    }

    private static int valeur(Float choisi, int defaut) {
        return choisi == null ? defaut : borne(Math.round(choisi), 0, GRID_CELLS);
    }

    /** Crée la partie avec ces réglages et transfère l'hôte : null si c'est fait, sinon le refus. */
    Component lancer(Player joueur, Reglages r) {
        corriger(r);
        long attente = plugin.parties().creationWaitSeconds(joueur);
        if (attente > 0) {
            return Component.text(BingoPartyManager.creationWaitText(attente));
        }
        if (r.total() != GRID_CELLS) {
            return t("bingo.create-bad-grid", "<red>La grille doit contenir exactement <cells> objectifs (vous en avez choisi <total>).",
                    "cells", GRID_CELLS, "total", r.total());
        }
        retenir(joueur, r);
        String rules = "mode=" + r.mode + ";bingos=" + r.bingos + ";easy=" + r.faciles + ";medium=" + r.normaux
                + ";hard=" + r.difficiles + ";extreme=" + r.extremes;
        String resume = ("BLACKOUT".equals(r.mode) ? "blackout" : "CHRONO".equals(r.mode)
                ? "contre la montre, " + r.bingos + " bingos" : r.bingos + " bingos")
                + ", " + r.faciles + " F / " + r.normaux + " N / " + r.difficiles + " D / " + r.extremes + " X";
        plugin.menus().bingoCreate(joueur, r.equipes, r.taille, r.duree, rules, resume);
        return null;
    }
}
