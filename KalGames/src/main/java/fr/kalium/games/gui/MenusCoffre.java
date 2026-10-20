package fr.kalium.games.gui;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.game.InstanceManager;
import fr.kalium.games.game.Reglages;
import fr.kalium.games.game.RushInstance;
import fr.kalium.games.game.RushItems;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import fr.kalium.games.model.RushLayout;
import fr.kalium.menu.api.Contenant;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.26.0 - menus des joueurs en coffre (demande de LeKiwi06, 20/10/2026 : « faire une alternative avec des interfaces
 * coffres pour tout ») : réglages d'une nouvelle partie, menu de la partie, menu du spectateur, choix de l'équipe du
 * Rush. Ouverts à la place des fenêtres de {@link PlayerMenus} quand le joueur a choisi les coffres (réglage
 * « Paramètres › Affichage » de KG_Menu, par défaut). Modèle des menus en coffre de KLM_Menu ({@link Contenant}) :
 * objets en haut, barre d'actions sur vitres grises en bas (action principale en colonne 4, sortie en colonne 5), messages
 * au-dessus de la barre d'objets.
 */
public final class MenusCoffre {

    /** Marques des coffres (pour les remplir à nouveau sans les refermer). */
    private enum Ecran { REGLAGES, PARTIE, SPECTATEUR, EQUIPES }

    /** Partie en cours de réglage par un joueur (avant « Créer la partie »). */
    private static final class Brouillon {
        final Minigame jeu;
        final Consumer<Player> retour;
        String arene;
        final Map<String, Object> options = new HashMap<>();

        Brouillon(Minigame jeu, Consumer<Player> retour) {
            this.jeu = jeu;
            this.retour = retour;
        }
    }

    private final KalGames plugin;
    private final Map<UUID, Brouillon> brouillons = new HashMap<>();
    /** Hôte qui change les réglages de sa partie (« Changer les réglages ») : partie à remplacer. */
    private final Map<UUID, GameInstance> remplacements = new HashMap<>();

    public MenusCoffre(KalGames plugin) {
        this.plugin = plugin;
    }

    private Component t(String key, String def, Object... pairs) {
        return plugin.t(key, def, pairs);
    }

    private Component nom(Minigame jeu) {
        return plugin.lang().parse(jeu.display());
    }

    public void forget(UUID uuid) {
        brouillons.remove(uuid);
        remplacements.remove(uuid);
    }

    /** Message d'un menu : au-dessus de la barre d'objets si un coffre est ouvert, sinon dans le tchat. */
    public void dire(Player joueur, Component texte, boolean refus) {
        if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant) {
            Contenant.message(joueur, texte, refus);
        } else {
            joueur.sendMessage(plugin.prefix().append(texte));
        }
    }

    // ------------------------------------------------------------------ icônes

    /** Icône d'un mini-jeu : menus.icones.&lt;TYPE&gt; de la config, sinon une icône par type. */
    public ItemStack icone(Minigame jeu) {
        String type = jeu.type().name();
        String defaut = switch (type) {
            case "BOAT_RACE" -> "OAK_BOAT";
            case "PARKOUR" -> "LEATHER_BOOTS";
            case "PVP_KIT" -> "IRON_SWORD";
            case "RUSH" -> "RED_BED";
            case "HIDE_AND_SEEK" -> "CARVED_PUMPKIN";
            case "PILIERS_FORTUNE" -> "GOLD_BLOCK";
            case "PONG" -> "SLIME_BALL";
            case "MANHUNT" -> "COMPASS";
            case "HUNGER_GAMES" -> "CHEST";
            default -> "PAPER";
        };
        Material materiau = Material.matchMaterial(plugin.getConfig().getString("menus.icones." + type, defaut));
        return new ItemStack(materiau == null || materiau.isAir() || !materiau.isItem() ? Material.PAPER : materiau);
    }

    private ItemStack tete(UUID uuid) {
        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        if (tete.getItemMeta() instanceof SkullMeta meta) {
            OfflinePlayer joueur = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(joueur);
            tete.setItemMeta(meta);
        }
        return tete;
    }

    private static ItemStack bascule(boolean actif) {
        return new ItemStack(actif ? Material.LIME_DYE : Material.GRAY_DYE);
    }

    /** Cases d'une rangée pour n objets, centrés et espacés d'une case quand il y a la place. */
    private static int[] cases(int rangee, int n) {
        int[] places = new int[Math.min(n, 9)];
        int debut = rangee * 9;
        if (n <= 5) {
            int premier = 4 - (n - 1);
            for (int i = 0; i < places.length; i++) {
                places[i] = debut + premier + 2 * i;
            }
        } else {
            int premier = (9 - places.length) / 2;
            for (int i = 0; i < places.length; i++) {
                places[i] = debut + premier + i;
            }
        }
        return places;
    }

    // ------------------------------------------------------------------ création d'une partie

    /**
     * Crée la partie privée, retient ses réglages (prochaine fois, « Recréer ma dernière partie ») et donne le code
     * dans le tchat (à partager). null si c'est fait, sinon le motif du refus.
     */
    public Component creerPartie(Player joueur, Minigame jeu, String arene, Map<String, Object> options) {
        // 1.26.0 : « Changer les réglages » : la nouvelle partie remplace l'ancienne, tout le monde y passe.
        GameInstance ancienne = remplacements.remove(joueur.getUniqueId());
        if (ancienne != null && (ancienne.closing() || plugin.instances().of(joueur) != ancienne
                || !ancienne.isHost(joueur.getUniqueId()) || !ancienne.relanceEnAttente())) {
            ancienne = null;
        }
        List<UUID> suivants = new ArrayList<>();
        if (ancienne != null) {
            suivants.addAll(ancienne.members());
            suivants.remove(joueur.getUniqueId());
        }
        InstanceManager.Result resultat = plugin.instances().createPrivate(joueur, jeu, arene, options);
        if (!resultat.ok()) {
            if (ancienne != null) {
                ancienne.reprendreRelance();
            }
            return resultat.error();
        }
        plugin.reglages().retenir(joueur.getUniqueId(), jeu.id(), resultat.instance().arena().id(), options);
        if (ancienne != null) {
            GameInstance nouvelle = resultat.instance();
            for (UUID uuid : suivants) {
                Player suivant = Bukkit.getPlayer(uuid);
                if (suivant != null) {
                    Component refus = plugin.instances().joinInstance(suivant, nouvelle);
                    suivant.sendMessage(plugin.prefix().append(refus != null ? refus
                            : t("relance.nouveaux-reglages", "<yellow>L'hôte a changé les réglages : nouvelle partie préparée.")));
                }
            }
            nouvelle.lancerCompteARebours();
        }
        joueur.sendMessage(plugin.prefix().append(t("menu.create-done",
                "<green>Partie créée. Code : <white><bold><code></bold></white> <gray>(à partager avec vos amis).",
                "code", resultat.instance().code())));
        return null;
    }

    /**
     * 1.26.0 : l'hôte change les réglages entre deux manches : la relance attend ; « Créer la partie » prépare une
     * nouvelle partie avec ces réglages (arène comprise) et y fait passer tout le monde.
     */
    public void modifier(Player hote, GameInstance partie) {
        if (partie.isPublic() || !partie.isHost(hote.getUniqueId()) || partie.phase() != GameInstance.Phase.WAITING) {
            dire(hote, t("host.only", "<red>Seul l'hôte de la partie peut faire cela."), true);
            return;
        }
        partie.suspendreRelance();
        remplacements.put(hote.getUniqueId(), partie);
        Consumer<Player> retour = p -> {
            remplacements.remove(p.getUniqueId());
            partie.reprendreRelance();
            plugin.menus().openCurrent(p);
        };
        Minigame jeu = partie.minigame();
        if (jeu.type().createForm() != null || !plugin.enCoffre(hote)) {
            Contenant.saisie(hote, () -> plugin.menus().openCreatePrivate(hote, jeu, retour));
            return;
        }
        Brouillon brouillon = new Brouillon(jeu, retour);
        brouillon.arene = partie.arena().id();
        brouillon.options.putAll(partie.optionsCopie());
        brouillons.put(hote.getUniqueId(), brouillon);
        reglages(hote);
    }

    /** « Créer une partie » &gt; un jeu : ses réglages (en coffre, ou la fenêtre du jeu s'il fournit son formulaire). */
    public void creer(Player joueur, Minigame jeu, Consumer<Player> retour) {
        List<Arena> arenes = plugin.instances().usableArenas(jeu);
        if (arenes.isEmpty()) {
            dire(joueur, t("game.no-arena", "<red>Aucune arène complète n'est disponible pour ce mini-jeu."), true);
            return;
        }
        if (jeu.type().createForm() != null) {
            // Formulaire fourni par le jeu (PvP Kit, Cache-cache, Piliers, Pong) : encore une fenêtre (étape 5).
            Contenant.saisie(joueur, () -> plugin.menus().openCreatePrivate(joueur, jeu, retour));
            return;
        }
        Brouillon brouillon = new Brouillon(jeu, retour);
        Reglages.Choix retenu = plugin.reglages().choix(joueur.getUniqueId(), jeu.id());
        brouillon.arene = arenes.get(0).id();
        if (retenu != null) {
            for (Arena arene : arenes) {
                if (arene.id().equals(retenu.arene())) {
                    brouillon.arene = arene.id();
                }
            }
            brouillon.options.putAll(retenu.options());
        }
        brouillons.put(joueur.getUniqueId(), brouillon);
        reglages(joueur);
    }

    private int nombre(Brouillon b, String cle, int min, int max, int defaut) {
        int valeur = b.options.get(cle) instanceof Number n ? n.intValue() : defaut;
        return Math.max(min, Math.min(max, valeur));
    }

    private boolean coche(Brouillon b, String cle, boolean defaut) {
        return b.options.get(cle) instanceof Boolean v ? v : defaut;
    }

    /**
     * 1.26.0 (LeKiwi06, 20/10/2026 : « ouvrir l'interface de jauge quand on clique sur l'item quand c'est nécessaire,
     * par exemple le nombre de tours au bateau ») : un réglage chiffré s'ouvre dans une petite fenêtre avec une jauge ;
     * « Valider » ou « Retour » ramène au coffre des réglages.
     */
    private void jauge(Player joueur, String cle, String libelle, int min, int max, int valeur) {
        Contenant.saisie(joueur, () -> {
            String champ = "valeur";
            plugin.gui().open(joueur, plugin.lang().parse("<white><bold>" + libelle),
                    List.of(t("coffre.jauge-body-echap", "<gray>Faites glisser la jauge. Valider ou Échap : enregistré.")),
                    List.of(plugin.gui().number(champ, plugin.lang().parse(libelle), min, max, valeur, 1)),
                    // « Valider » est le bouton de sortie : Échap enregistre aussi la jauge (LeKiwi06, 20/10/2026).
                    List.of(),
                    plugin.gui().form(t("coffre.jauge-ok", "<green>Valider"), null, (p, vue) -> {
                        Brouillon b = brouillons.get(p.getUniqueId());
                        Float choisi = vue.getFloat(champ);
                        if (b != null && choisi != null) {
                            b.options.put(cle, Math.max(min, Math.min(max, Math.round(choisi))));
                        }
                        reglages(p);
                    }), 1);
        });
    }

    private void reglages(Player joueur) {
        Brouillon b = brouillons.get(joueur.getUniqueId());
        if (b == null) {
            return;
        }
        Minigame jeu = b.jeu;
        MinigameType type = jeu.type();
        List<Arena> arenes = plugin.instances().usableArenas(jeu);
        Contenant menu = Contenant.pour(joueur, 4, t("coffre.reglages-titre", "<dark_gray>Nouvelle partie"), Ecran.REGLAGES);

        // Présentation du jeu en haut.
        List<Component> infos = new ArrayList<>();
        if (!jeu.description().isBlank()) {
            infos.addAll(Contenant.lignes(plugin.lang().parse("<gray>" + jeu.description())));
        }
        int[] range = type.playerRange(jeu, arenes);
        if (range != null) {
            infos.add(range[0] == range[1]
                    ? t("menu.player-range-one", "<gray>Joueurs : <white><n>", "n", range[0])
                    : t("menu.player-range", "<gray>Joueurs : <white><min> à <max>", "min", range[0], "max", range[1]));
        }
        menu.poser(4, Contenant.objet(icone(jeu), nom(jeu), infos), null);

        // Une case par réglage, la valeur dans le nom.
        List<ItemStack> objets = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        if (arenes.size() > 1) {
            Arena actuelle = arenes.get(0);
            int index = 0;
            for (int i = 0; i < arenes.size(); i++) {
                if (arenes.get(i).id().equals(b.arene)) {
                    actuelle = arenes.get(i);
                    index = i;
                }
            }
            int position = index;
            Component nomArene = plugin.lang().parse(actuelle.display());
            if (type == MinigameType.RUSH) {
                nomArene = nomArene.append(t("rush.arena-teams", " <gray>(<n> équipes)", "n", RushLayout.teamCount(actuelle)));
            }
            objets.add(Contenant.objet(Material.FILLED_MAP, t("coffre.arene", "<white>Arène : <arene>", "arene", nomArene),
                    List.of(t("coffre.arene-clic", "<yellow>Clic gauche : suivante"), t("coffre.arene-clic-droit", "<yellow>Clic droit : précédente"))));
            actions.add(p -> {
                int pas = plugin.kgMenu().clic(p).isRightClick() ? -1 : 1;
                b.arene = arenes.get(Math.floorMod(position + pas, arenes.size())).id();
                reglages(p);
            });
        }
        List<Component> clicNombre = List.of(t("coffre.jauge-clic", "<yellow>Clic : régler avec la jauge"));
        int max = jeu.getInt("max-players", 8);
        if (max > 1 && type != MinigameType.RUSH) { // Rush : la taille vient des équipes de l'arène
            int valeur = nombre(b, "maxPlayers", 1, max, Math.min(max, 4));
            b.options.put("maxPlayers", valeur);
            objets.add(Contenant.objet(new ItemStack(Material.PLAYER_HEAD, Math.max(1, Math.min(64, valeur))),
                    t("coffre.joueurs-max", "<white>Joueurs maximum : <aqua><n>", "n", valeur), clicNombre));
            actions.add(p -> jauge(p, "maxPlayers", plugin.lang().raw("menu.create-max", "Joueurs maximum"), 1, max,
                    nombre(b, "maxPlayers", 1, max, Math.min(max, 4))));
        }
        for (MinigameType.CreateOption option : type.createOptions()) {
            int defaut = Math.max(option.min(), Math.min(option.max(), jeu.getInt(option.defaultSetting(), option.fallback())));
            int valeur = nombre(b, option.key(), option.min(), option.max(), defaut);
            b.options.put(option.key(), valeur);
            objets.add(Contenant.objet(new ItemStack(Material.CLOCK, Math.max(1, Math.min(64, valeur))),
                    plugin.lang().parse("<white>" + option.label() + " : <aqua>" + valeur), clicNombre));
            actions.add(p -> jauge(p, option.key(), option.label(), option.min(), option.max(),
                    nombre(b, option.key(), option.min(), option.max(), defaut)));

        }
        for (MinigameType.CreateToggle bascule : type.createToggles()) {
            boolean valeur = coche(b, bascule.key(), false);
            b.options.put(bascule.key(), valeur);
            objets.add(Contenant.objet(bascule(valeur), plugin.lang().parse("<white>" + bascule.label() + " : "
                    + (valeur ? "<green>oui" : "<red>non")), List.of(t("coffre.bascule-clic", "<yellow>Clic : changer"))));
            actions.add(p -> {
                b.options.put(bascule.key(), !coche(b, bascule.key(), false));
                reglages(p);
            });
        }
        boolean listee = coche(b, "listed", true);
        b.options.put("listed", listee);
        List<Component> lignesListe = new ArrayList<>(Contenant.lignes(
                t("coffre.liste-tip", "<gray>Visible dans « Rejoindre une partie ». Sinon, on la rejoint avec son code.")));
        lignesListe.add(t("coffre.bascule-clic", "<yellow>Clic : changer"));
        objets.add(Contenant.objet(bascule(listee), listee ? t("coffre.liste-oui", "<white>Visible dans la liste : <green>oui")
                : t("coffre.liste-non", "<white>Visible dans la liste : <red>non"), lignesListe));
        actions.add(p -> {
            b.options.put("listed", !coche(b, "listed", true));
            reglages(p);
        });
        int[] places = cases(2, objets.size());
        for (int i = 0; i < places.length; i++) {
            menu.poser(places[i], objets.get(i), actions.get(i));
        }

        menu.barre();
        menu.poser(menu.bas(4), Contenant.objet(Material.LIME_CONCRETE, t("coffre.creer", "<green><bold>Créer la partie"),
                Contenant.lignes(t("coffre.creer-tip", "<gray>Vous en êtes l'hôte ; invitez vos amis avec le code."))), p -> {
            Brouillon actuel = brouillons.get(p.getUniqueId());
            if (actuel == null) {
                return;
            }
            // Après le clic : la création téléporte l'hôte dans la salle d'attente.
            Contenant.apres(() -> {
                Component refus = creerPartie(p, actuel.jeu, actuel.arene, new HashMap<>(actuel.options));
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
            b.retour.accept(p);
        };
        menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, t("coffre.retour", "<white>Retour"), List.of()), retour);
        menu.ouvrir(joueur);
        plugin.kgMenu().echap(joueur, menu, retour); // Échap = « Retour »

    }

    // ------------------------------------------------------------------ menu de la partie

    private String pseudo(UUID uuid) {
        Player enLigne = Bukkit.getPlayer(uuid);
        if (enLigne != null) {
            return enLigne.getName();
        }
        String nom = Bukkit.getOfflinePlayer(uuid).getName();
        return nom == null ? "?" : nom;
    }

    private Component etat(GameInstance partie) {
        if (!partie.ready()) {
            return t("game.body-preparing", "<yellow>Préparation de l'arène…");
        }
        return partie.matchInProgress() ? t("coffre.etat-jeu", "<gold>Manche en cours")
                : t("coffre.etat-attente", "<green>Salle d'attente");
    }

    /** Menu de la partie (objet de la partie, objet du hub, /kalgames menu) pour un joueur d'une partie. */
    public void partie(Player joueur) {
        GameInstance partie = plugin.instances().of(joueur);
        if (partie == null) {
            plugin.kgMenu().openHome(joueur, null);
            return;
        }
        UUID uuid = joueur.getUniqueId();
        Contenant menu = Contenant.pour(joueur, 4, t("coffre.partie-titre", "<dark_gray><jeu>", "jeu",
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(nom(partie.minigame()))), Ecran.PARTIE);

        List<Component> infos = new ArrayList<>();
        if (!partie.isPublic()) {
            infos.add(t("game.body-code", "<gray>Code de la partie : <white><bold><code></bold></white>", "code", partie.code()));
            infos.add(t("partie.hote", "<gray>Hôte : <white><hote>", "hote", partie.host() == null ? "?" : pseudo(partie.host())));
        }
        infos.add(t("coffre.joueurs", "<gray>Joueurs : <white><n>/<max>", "n", partie.members().size(), "max", partie.capacite()));
        infos.add(etat(partie));
        if (partie.relance() > 0) {
            infos.add(t("coffre.relance", "<green>Nouvelle partie dans <white><s></white> s", "s", partie.relance()));
        } else if (partie.relanceEnAttente()) {
            infos.add(t("coffre.relance-attente", "<yellow>Relance en attente (joueurs ou réglages)"));
        }
        for (Component ligne : partie.menuInfo(joueur)) {
            infos.addAll(Contenant.lignes(ligne));
        }
        menu.poser(4, Contenant.objet(icone(partie.minigame()), nom(partie.minigame()), infos), null);
        // Résumé de la dernière manche (points gagnés par chacun).
        if (!partie.resume().isEmpty() && !partie.matchInProgress()) {
            List<Component> lignes = new ArrayList<>();
            int rang = 1;
            for (GameInstance.Resultat r : partie.resume()) {
                if (rang > 10) {
                    break;
                }
                lignes.add(t("coffre.resume-ligne", "<gray><rang>. <white><joueur></white> <dark_gray>: <gold>+<points> pts",
                        "rang", rang++, "joueur", r.nom(), "points", fr.kalium.scoreboards.data.StatsService.formatPoints(r.points())));
            }
            menu.poser(0, Contenant.objet(Material.WRITTEN_BOOK, t("coffre.resume", "<gold>Résumé de la manche"), lignes), null);
        }

        // Actions de la partie (rangée du milieu).
        List<ItemStack> objets = new ArrayList<>();
        List<Consumer<Player>> actions = new ArrayList<>();
        if (partie instanceof RushInstance rush && rush.teamsOpen()) {
            RushLayout.Team equipe = rush.teamOf(uuid);
            objets.add(Contenant.objet(equipe == null ? Material.WHITE_WOOL : laine(equipe),
                    t("rush.menu-teams", "<aqua>Choisir mon équipe"),
                    List.of(equipe == null ? t("rush.menu-no-team", "<red>Vous n'avez pas encore choisi d'équipe.")
                            : t("rush.menu-team", "<gray>Votre équipe : <team>", "team", RushItems.teamName(equipe)))));
            actions.add(p -> equipes(p, rush));
        }
        for (GameInstance.MenuAction action : partie.menuActions(joueur)) {
            objets.add(Contenant.objet(Material.PAPER, action.label(), List.of()));
            actions.add(p -> {
                if (plugin.instances().of(p) == partie) {
                    Contenant.apres(() -> action.action().accept(p));
                    Contenant.apres(() -> {
                        if (plugin.instances().of(p) == partie && Contenant.ouvert(p, Ecran.PARTIE) != null) {
                            partie(p);
                        }
                    });
                }
            });
        }
        boolean entreManches = !partie.isPublic() && partie.phase() == GameInstance.Phase.WAITING;
        if (entreManches && partie.minigame().getBool("allow-spectate", true)) {
            objets.add(Contenant.objet(Material.ENDER_EYE, t("coffre.passer-spectateur", "<aqua>Passer spectateur"),
                    Contenant.lignes(t("coffre.passer-spectateur-tip", "<gray>Regarder la prochaine manche sans la jouer."))));
            actions.add(p -> {
                Contenant.fermer(p);
                Contenant.apres(() -> {
                    Component refus = plugin.instances().playerToSpectator(p);
                    if (refus != null) {
                        p.sendMessage(plugin.prefix().append(refus));
                    }
                });
            });
        }
        if (entreManches && partie.isHost(uuid)) {
            objets.add(Contenant.objet(Material.COMPARATOR, t("coffre.changer-reglages", "<white>Changer les réglages"),
                    Contenant.lignes(t("coffre.changer-reglages-tip", "<gray>Le compte à rebours s'arrête pendant que vous réglez la partie."))));
            actions.add(p -> {
                GameInstance actuelle = plugin.instances().of(p);
                if (actuelle != null) {
                    modifier(p, actuelle);
                }
            });
            if (partie.relanceEnAttente()) {
                objets.add(Contenant.objet(Material.CLOCK, t("coffre.reprendre", "<green>Reprendre le compte à rebours"), List.of()));
                actions.add(p -> {
                    GameInstance actuelle = plugin.instances().of(p);
                    if (actuelle != null && actuelle.isHost(p.getUniqueId())) {
                        actuelle.lancerCompteARebours();
                    }
                    partie(p);
                });
            }
        }
        if (!partie.isPublic() && partie.isHost(uuid)) {
            boolean listee = partie.listed();
            objets.add(Contenant.objet(bascule(listee), listee ? t("coffre.liste-oui", "<white>Visible dans la liste : <green>oui")
                    : t("coffre.liste-non", "<white>Visible dans la liste : <red>non"), List.of(t("coffre.bascule-clic", "<yellow>Clic : changer"))));
            actions.add(p -> {
                GameInstance actuelle = plugin.instances().of(p);
                if (actuelle != null && actuelle.isHost(p.getUniqueId())) {
                    actuelle.listed(!actuelle.listed());
                }
                partie(p);
            });
        }
        if (partie.isPublic() && partie.members().contains(uuid) && !(partie instanceof RushInstance)
                && !(partie.isParticipant(uuid) && partie.matchInProgress())) {
            boolean enFile = partie.queued(uuid);
            objets.add(Contenant.objet(enFile ? Material.CLOCK : Material.LIME_DYE,
                    enFile ? t("game.queue-leave", "<yellow>Quitter la file d'attente") : t("game.queue-join", "<green>Rejoindre la file d'attente"),
                    List.of(enFile ? t("game.body-queue", "<gray>Position dans la file : <white><pos>/<total>", "pos", partie.queuePosition(uuid), "total", partie.queue().size())
                            : t("game.body-spectator", "<gray>Vous regardez la partie depuis les gradins."))));
            actions.add(p -> {
                GameInstance actuelle = plugin.instances().of(p);
                if (actuelle != null) {
                    actuelle.setQueued(p.getUniqueId(), !actuelle.queued(p.getUniqueId()));
                }
                partie(p);
            });
        }
        int[] places = cases(1, objets.size());
        for (int i = 0; i < places.length; i++) {
            menu.poser(places[i], objets.get(i), actions.get(i));
        }

        // Joueurs de la partie (rangée du bas), l'hôte en premier.
        List<UUID> membres = new ArrayList<>(partie.members());
        if (partie.host() != null && membres.remove(partie.host())) {
            membres.add(0, partie.host());
        }
        for (int i = 0; i < membres.size() && i < 9; i++) {
            UUID membre = membres.get(i);
            Component nomJoueur = partie.isHost(membre) && !partie.isPublic()
                    ? t("coffre.joueur-hote", "<gold><joueur> <gray>(hôte)", "joueur", pseudo(membre))
                    : t("coffre.joueur", "<white><joueur>", "joueur", pseudo(membre));
            // 1.26.0 : l'hôte peut nommer un autre hôte ou exclure un joueur.
            if (!partie.isPublic() && partie.isHost(uuid) && !membre.equals(uuid)) {
                menu.poser(18 + i, Contenant.objet(tete(membre), nomJoueur, List.of(
                        t("coffre.joueur-nommer", "<yellow>Clic gauche : nommer hôte"),
                        t("coffre.joueur-exclure", "<red>Maj + clic droit : exclure de la partie"))), p -> {
                    ClickType clic = plugin.kgMenu().clic(p);
                    Component refus;
                    if (clic == ClickType.SHIFT_RIGHT) {
                        refus = plugin.instances().kick(p, membre);
                    } else if (!clic.isRightClick() && !clic.isShiftClick()) {
                        refus = plugin.instances().giveHost(p, membre);
                    } else {
                        return;
                    }
                    if (refus != null) {
                        dire(p, refus, true);
                    } else {
                        partie(p);
                    }
                });
            } else {
                menu.poser(18 + i, Contenant.objet(tete(membre), nomJoueur, List.of()), null);
            }
        }

        menu.barre();
        menu.poser(menu.bas(0), Contenant.objet(Material.COMPASS, t("game.lobby", "<#09add3>Retour au lobby"), List.of()),
                p -> Contenant.apres(() -> plugin.connectLobby(p)));
        if (plugin.isAdmin(joueur)) {
            menu.poser(menu.bas(2), Contenant.objet(Material.TNT, t("game.admin-close", "<dark_red>Fermer la partie (modérateur)"), List.of()), p -> {
                GameInstance actuelle = plugin.instances().of(p);
                if (actuelle != null) {
                    Contenant.fermer(p);
                    Contenant.apres(() -> plugin.instances().close(actuelle,
                            t("game.closed-by-admin", "<yellow>La partie a été fermée par un modérateur.")));
                }
            });
        }
        if (!partie.isPublic() && partie.isHost(uuid) && partie.phase() == GameInstance.Phase.WAITING) {
            menu.poser(menu.bas(4), Contenant.objet(Material.LIME_CONCRETE, partie.relance() > 0
                    ? t("coffre.lancer-maintenant", "<green><bold>Lancer maintenant") : t("game.start", "<green><bold>Lancer la partie"), List.of()), p -> {
                Contenant.apres(() -> {
                    GameInstance actuelle = plugin.instances().of(p);
                    if (actuelle == null) {
                        return;
                    }
                    Component refus = actuelle.requestStart(p);
                    if (refus != null) {
                        dire(p, refus, true);
                    } else {
                        p.closeInventory();
                    }
                });
            });
        }
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("coffre.fermer", "<red>Fermer"), List.of()), Contenant::fermer);
        menu.poser(menu.bas(8), Contenant.objet(Material.RED_BED, t("game.leave", "<red>Quitter la partie"),
                List.of(t("game.leave-tip", "<gray>Retour au hub de Kal-Games."))), p -> {
            Contenant.fermer(p);
            Contenant.apres(() -> plugin.hub().sendToHub(p));
        });
        menu.ouvrir(joueur);
        plugin.kgMenu().echap(joueur, menu, null);
    }

    private static Material laine(RushLayout.Team equipe) {
        Material laine = Material.matchMaterial(RushItems.dye(equipe).name() + "_WOOL");
        return laine == null ? Material.WHITE_WOOL : laine;
    }

    /** Choix de l'équipe du Rush dans la salle d'attente (une équipe pleine ne peut plus être rejointe). */
    public void equipes(Player joueur, RushInstance partie) {
        if (!partie.teamsOpen()) {
            dire(joueur, t("rush.team-locked", "<red>Les équipes ne se choisissent que dans la salle d'attente."), true);
            return;
        }
        UUID uuid = joueur.getUniqueId();
        Contenant menu = Contenant.pour(joueur, 3, t("coffre.equipes", "<dark_gray>Choisissez votre équipe"), Ecran.EQUIPES);
        List<RushLayout.Team> equipes = partie.teams();
        int[] places = cases(1, equipes.size());
        for (int i = 0; i < places.length; i++) {
            RushLayout.Team equipe = equipes.get(i);
            List<UUID> membres = partie.membersOf(equipe);
            boolean mienne = partie.teamOf(uuid) == equipe;
            boolean pleine = membres.size() >= partie.teamSize();
            List<Component> lignes = new ArrayList<>();
            lignes.add(t("coffre.equipe-places", "<gray>Joueurs : <white><n>/<size>", "n", membres.size(), "size", partie.teamSize()));
            for (UUID membre : membres) {
                lignes.add(t("coffre.equipe-membre", "<white>- <joueur>", "joueur", pseudo(membre)));
            }
            lignes.add(mienne ? t("coffre.equipe-mienne", "<green>Votre équipe")
                    : pleine ? t("coffre.equipe-pleine", "<red>Équipe complète") : t("coffre.equipe-clic", "<yellow>Clic : rejoindre"));
            ItemStack laine = new ItemStack(laine(equipe), Math.max(1, membres.size()));
            Component nomEquipe = mienne ? t("vote.selected", "<green>✔ ").append(t("rush.teams-join", "Équipe <team>", "team", RushItems.teamName(equipe)))
                    : t("rush.teams-join", "Équipe <team>", "team", RushItems.teamName(equipe));
            menu.poser(places[i], Contenant.objet(laine, nomEquipe, lignes), p -> {
                Component refus = partie.chooseTeam(p, equipe);
                if (refus != null) {
                    dire(p, refus, true);
                }
                if (partie.teamsOpen() && partie.isMember(p.getUniqueId())) {
                    equipes(p, partie);
                }
            });
        }
        menu.barre();
        menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, t("coffre.retour", "<white>Retour"), List.of()), this::partie);
        menu.ouvrir(joueur);
        plugin.kgMenu().echap(joueur, menu, this::partie); // Échap = « Retour »
    }

    // ------------------------------------------------------------------ spectateur

    public void spectateur(Player joueur) {
        GameInstance partie = plugin.instances().spectatorOf(joueur);
        if (partie == null) {
            plugin.kgMenu().openHome(joueur, null);
            return;
        }
        Contenant menu = Contenant.pour(joueur, 3, t("coffre.spectateur-titre", "<dark_gray>Mode spectateur"), Ecran.SPECTATEUR);
        List<Component> infos = new ArrayList<>();
        infos.add(t("spectate.body-title", "<gray>Vous regardez : <white><name>", "name", nom(partie.minigame())));
        if (!partie.isPublic() && partie.host() != null) {
            infos.add(t("partie.hote", "<gray>Hôte : <white><hote>", "hote", pseudo(partie.host())));
        }
        infos.add(etat(partie));
        infos.addAll(Contenant.lignes(t("coffre.spectateur-info", "<gray>Vol libre. Tapez /hub à tout moment pour arrêter de regarder.")));
        menu.poser(4, Contenant.objet(icone(partie.minigame()), nom(partie.minigame()), infos), null);
        if (!partie.isPublic()) {
            // 1.26.0 : un spectateur peut entrer dans la partie (salle d'attente, prochaine manche).
            menu.poser(13, Contenant.objet(Material.LIME_DYE, t("coffre.spectateur-jouer", "<green>Rejoindre la partie"),
                    Contenant.lignes(t("coffre.spectateur-jouer-tip", "<gray>Vous jouerez la prochaine manche."))), p -> {
                Contenant.fermer(p);
                Contenant.apres(() -> {
                    GameInstance regardee = plugin.instances().spectatorOf(p);
                    Component refus = regardee == null ? null : plugin.instances().spectatorToPlayer(p, regardee);
                    if (refus != null) {
                        p.sendMessage(plugin.prefix().append(refus));
                    }
                });
            });
        }

        menu.poser(11, Contenant.objet(Material.ENDER_EYE, t("spectate.stop", "<red>Arrêter de regarder"),
                List.of(t("spectate.stop-tip", "<gray>Retour au hub de Kal-Games."))), p -> {
            Contenant.fermer(p);
            Contenant.apres(() -> plugin.instances().leaveSpectator(p, false));
        });
        menu.poser(15, Contenant.objet(Material.COMPASS, t("game.lobby", "<#09add3>Retour au lobby"), List.of()),
                p -> Contenant.apres(() -> plugin.connectLobby(p)));
        menu.barre();
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, t("coffre.fermer", "<red>Fermer"), List.of()), Contenant::fermer);
        menu.ouvrir(joueur);
        plugin.kgMenu().echap(joueur, menu, null);
    }
}
