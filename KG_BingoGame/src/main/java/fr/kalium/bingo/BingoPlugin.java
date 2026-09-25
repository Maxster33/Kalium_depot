package fr.kalium.bingo;

import fr.kalium.bingo.command.BingoAdminCommand;
import fr.kalium.bingo.command.MenuCommand;
import fr.kalium.bingo.command.TeamCommand;
import fr.kalium.bingo.game.AbandonService;
import fr.kalium.bingo.game.GameEndService;
import fr.kalium.bingo.game.GameHudService;
import fr.kalium.bingo.game.GameManager;
import fr.kalium.bingo.game.ObjectiveValidationTask;
import fr.kalium.bingo.game.PartyCountdownService;
import fr.kalium.bingo.game.PartyManager;
import fr.kalium.bingo.game.PartyStarter;
import fr.kalium.bingo.game.GameRespawnListener;
import fr.kalium.bingo.game.PlayerResetService;
import fr.kalium.bingo.grid.GridGenerator;
import fr.kalium.bingo.grid.ObjectiveLibrary;
import fr.kalium.bingo.game.PartyCanceller;
import fr.kalium.bingo.gui.AbandonConfirmMenu;
import fr.kalium.bingo.gui.GameItemListener;
import fr.kalium.bingo.gui.GameItems;
import fr.kalium.bingo.gui.GameMenu;
import fr.kalium.bingo.gui.GameMenuListener;
import fr.kalium.bingo.gui.LobbyItems;
import fr.kalium.bingo.gui.LobbyMenu;
import fr.kalium.bingo.gui.PartyMenu;
import fr.kalium.bingo.gui.PostGameMenu;
import fr.kalium.bingo.network.AssignmentNetworkListener;
import fr.kalium.bingo.network.AssignmentService;
import fr.kalium.bingo.network.PartyStatusNotifier;
import fr.kalium.bingo.network.PlayerConnectListener;
import fr.kalium.bingo.network.RelayClient;
import fr.kalium.bingo.persistence.GamePersistence;
import fr.kalium.bingo.world.InstanceWorldManager;
import fr.kalium.bingo.world.InstanceWorldPreparer;
import fr.kalium.bingo.world.LobbyCaptureService;
import fr.kalium.bingo.world.LobbyProtectionListener;
import fr.kalium.bingo.world.LobbyRegenTask;
import fr.kalium.bingo.world.LobbySlots;
import fr.kalium.bingo.world.LobbyTemplateService;
import fr.kalium.bingo.world.VoidGenerator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

/**
 * Point d'entree du plugin KG_BingoGame (serveur dedie, separe de kal-games -
 * decision confirmee par l'utilisateur).
 *
 * Etat actuel (detail par version dans JOURNAL.md) : salle d'attente et equipes assignees par
 * l'hote, un overworld + Nether + End par equipe (meme seed), grille, validation automatique des
 * objectifs, score, chronometre, fin de partie (victoire, temps ecoule, abandon), reconnexion,
 * persistance des parties en cours. La partie est lancee par l'hote (menu) ou un operateur
 * (/bingoadmin start), apres un delai minimum (PartyCountdownService). Pas encore faits (cahier des
 * charges to_do_list_Kal_Games_Bingo.txt) : carte custom, detection de ligne a l'ecran, points par
 * difficulte, conditions particulieres des objectifs, integration recompenses.
 */
public class BingoPlugin extends JavaPlugin {

    private GameManager gameManager;
    private LobbyTemplateService lobbyTemplateService;
    private LobbySlots lobbySlots;
    private PartyManager partyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        InstanceWorldManager worldManager = new InstanceWorldManager(this, getLogger());

        String worldNamePrefix = config.getString("instances.world-name-prefix", "bingo_");
        // Pre-generation en cascade des mondes d'instance des la creation de la salle d'attente
        // (AJOUTE le 23/09/2026, demande explicite de l'utilisateur : "générer les maps directement
        // après la création de la partie et en cascade pour éviter les lags") - voir
        // InstanceWorldPreparer, declenche depuis AssignmentService, consomme par
        // GameManager.prepareInstances.
        Duration pregenerationStagger = Duration.ofSeconds(config.getLong("instances.pregeneration-stagger-seconds", 10));
        // 0.1.18 : rayon (blocs) de terrain pre-genere autour du spawn de chaque map - demande
        // explicite de l'utilisateur ("les maps ne sont toujours pas générées à l'avance"), rayon
        // choisi via AskUserQuestion (~200 blocs). 0 = desactive.
        int pregenerationRadius = config.getInt("instances.pregeneration-radius-blocks", 200);
        // 0.6.0 : debit total de la pre-generation du terrain, toutes parties confondues (demande de LeKiwi06 :
        // "ralentir la vitesse de génération [...] on veux que ce soit fluide"). Cles absentes du config.yml
        // deja deploye : valeurs par defaut ci-dessous.
        int pregenerationChunksPerSecond = config.getInt("instances.pregeneration-chunks-per-second", 20);
        int pregenerationChunksInFlight = config.getInt("instances.pregeneration-max-chunks-in-flight", 2);
        InstanceWorldPreparer instanceWorldPreparer = new InstanceWorldPreparer(this, getLogger(), worldManager,
                worldNamePrefix, pregenerationStagger, pregenerationRadius,
                pregenerationChunksPerSecond, pregenerationChunksInFlight);

        // Liste d'objectifs (objectives.yml, dossier de donnees - PAS le .jar) et generation de
        // grille (section 2 du cahier des charges, etape 4 de l'ordre de priorite). load() copie
        // le fichier d'exemples par defaut si absent, puis charge la liste depuis le disque.
        ObjectiveLibrary objectiveLibrary = new ObjectiveLibrary(this);
        objectiveLibrary.load();
        GridGenerator gridGenerator = new GridGenerator();

        int maxTeams = config.getInt("teams.max-teams");
        int maxTeamSize = config.getInt("teams.max-team-size");
        Duration defaultDuration = Duration.ofSeconds(config.getLong("game.default-duration-seconds"));
        // 0.3.0 : deconnexion consecutive au-dela de ce delai = abandon definitif (demande explicite de LeKiwi06 :
        // "le compte a rebours est celui d'un joueur deconnecte, donc 10 minutes"). Remplace l'ancienne cle
        // reconnect-during-game.abandon-after-seconds, qui ne declenchait rien.
        Duration abandonAfter = Duration.ofSeconds(config.getLong("game.disconnect-abandon-seconds", 600));
        int maxSimultaneousGames = config.getInt("instances.max-simultaneous-games");
        int gridSize = config.getInt("grid.size", 5);

        this.gameManager = new GameManager(getLogger(), worldManager, instanceWorldPreparer, objectiveLibrary, gridGenerator,
                maxTeams, maxTeamSize, defaultDuration, worldNamePrefix, abandonAfter, maxSimultaneousGames, gridSize);

        // Persistance des parties EN COURS (voir GamePersistence) - demande explicite de
        // l'utilisateur, 23/09/2026 : "la partie doit continuer meme si le serveur est redémarré
        // ou si il crash". Le rechargement effectif (loadAll) est differe a ServerLoadEvent, meme
        // contrainte que lobbySlots.init() ci-dessous (creation de monde interdite en phase STARTUP).
        GamePersistence gamePersistence = new GamePersistence(this, gameManager);

        this.lobbyTemplateService = new LobbyTemplateService(this);
        this.lobbySlots = new LobbySlots(this, lobbyTemplateService);
        // lobbySlots.init() cree/charge le monde "bingo_lobby" via Bukkit.createWorld() : impossible
        // de l'appeler directement ici. Depuis la 0.1.3 ce plugin est en "load: STARTUP" (necessaire
        // pour fournir le generateur du monde par defaut de Kixster, voir getDefaultWorldGenerator),
        // et Bukkit interdit de creer un monde SUPPLEMENTAIRE pendant la phase STARTUP
        // ("Cannot create additional worlds on STARTUP" - plante onEnable() et desactive le plugin
        // si on l'ignore, vu en jeu sur Kixster). On reporte donc l'initialisation de la salle
        // d'attente a ServerLoadEvent, qui se declenche une seule fois une fois le serveur
        // completement demarre (tous les mondes crees, tous les plugins actives).
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onServerLoad(ServerLoadEvent event) {
                if (!lobbySlots.init()) {
                    getLogger().severe("[KG_BingoGame] Le monde de la salle d'attente n'a pas pu être créé - le plugin continue sans salle d'attente.");
                }
                // Restaure les parties EN COURS sauvegardees avant le dernier arret/crash (voir
                // GamePersistence) - meme contrainte que lobbySlots.init() ci-dessus : la creation
                // de monde (rechargement des mondes d'instance existants) est interdite pendant la
                // phase STARTUP, d'ou ce report a ServerLoadEvent.
                gamePersistence.loadAll();
            }
        }, this);

        this.partyManager = new PartyManager();
        LobbyItems lobbyItems = new LobbyItems(this);
        String kalGamesServerName = config.getString("network.kal-games-server-name", "kal-games");
        Duration assignmentWait = Duration.ofSeconds(config.getLong("reconnect-during-game.assignment-wait-seconds", 30));
        RelayClient relayClient = new RelayClient(this);
        AssignmentService assignmentService = new AssignmentService(this, partyManager, gameManager, lobbySlots, lobbyItems,
                relayClient, instanceWorldPreparer, kalGamesServerName, assignmentWait);

        // Objet "Objectifs" (papier) remis sur la carte de jeu, a la place de la Nether Star de la
        // salle d'attente - voir GameItems/GameMenu/GameItemListener, demande explicite de
        // l'utilisateur (liste des objectifs, temps restant, progression des equipes).
        GameItems gameItems = new GameItems(this);
        // 0.4.0 : carte de la grille en main secondaire (icones du jeu officiel, voir fr.kalium.bingo.map).
        fr.kalium.bingo.map.IconLibrary iconLibrary = new fr.kalium.bingo.map.IconLibrary(this);
        iconLibrary.loadAsync();
        fr.kalium.bingo.map.GameMapService gameMaps = new fr.kalium.bingo.map.GameMapService(iconLibrary);
        gameManager.setOnCleanup(gameMaps::forget);
        gameItems.setMapSupplier(
                player -> gameManager.findGameOf(player.getUniqueId()).filter(g -> g.getGrid() != null)
                        .map(gameMaps::mapItem).orElse(null),
                player -> gameManager.findGameOf(player.getUniqueId()).map(gameMaps::mapIdOf).orElse(-1));
        gameItems.setInGameCheck(player -> gameManager.findGameOf(player.getUniqueId())
                .filter(g -> g.getState() == fr.kalium.bingo.game.GameState.IN_PROGRESS).isPresent());
        GameMenu gameMenu = new GameMenu(this, gameManager);
        getServer().getPluginManager().registerEvents(new GameItemListener(gameItems, gameMenu), this);

        // Fin de partie (section 1 etapes 11/12, section 5) - timeout (temps reglementaire ecoule),
        // victoire (grille remplie par une equipe) ET abandon (plus aucun joueur connecte depuis
        // game.no-players-abandon-after-seconds, AJOUTE le 23/09/2026) - voir GameEndService pour
        // le detail des trois declencheurs, demande explicite de l'utilisateur.
        Duration endCleanupDelay = Duration.ofSeconds(config.getLong("game.end-cleanup-delay-seconds", 5));
        Duration postGameLobbyTimeout = Duration.ofSeconds(config.getLong("game.post-game-lobby-timeout-seconds", 600));
        Duration noPlayersAbandonAfter = Duration.ofSeconds(config.getLong("game.no-players-abandon-after-seconds", 600));
        // Point de spawn en partie + remise a zero (inventaire, point de spawn) en fin de partie /
        // abandon - AJOUTE en 0.1.18, demande explicite de l'utilisateur, voir PlayerResetService.
        PlayerResetService playerReset = new PlayerResetService(this);
        getServer().getPluginManager().registerEvents(new GameRespawnListener(gameManager), this);
        // 0.7.0 : annonces des succes (advancements) limitees aux joueurs de la meme partie.
        getServer().getPluginManager().registerEvents(new fr.kalium.bingo.game.AdvancementScopeListener(gameManager), this);
        // 0.1.22 : Nether et End propres a chaque equipe (portails rediriges, voir DimensionPortalListener).
        getServer().getPluginManager().registerEvents(new fr.kalium.bingo.game.DimensionPortalListener(getLogger(), gameManager), this);

        GameEndService gameEndService = new GameEndService(getLogger(), this, gameManager, lobbySlots, lobbyItems,
                relayClient, assignmentService, gamePersistence, playerReset, endCleanupDelay, postGameLobbyTimeout,
                noPlayersAbandonAfter);

        // Abandon volontaire et definitif d'une partie (menu Objectifs -> objet "Abandonner la
        // partie", confirmation demandee) - AJOUTE le 24/09/2026, demande explicite de
        // l'utilisateur. Voir AbandonService/AbandonConfirmMenu/GameMenuListener.
        AbandonService abandonService = new AbandonService(this, gameManager, playerReset, relayClient, assignmentService, gameEndService);
        AbandonConfirmMenu abandonConfirmMenu = new AbandonConfirmMenu(this, abandonService);
        // 0.3.0 : votes de nulle (voir DrawVoteService), proposes depuis le menu Objectifs ou /bingonulle.
        fr.kalium.bingo.game.DrawVoteService drawVotes = new fr.kalium.bingo.game.DrawVoteService(gameManager);
        gameEndService.setDrawVotes(drawVotes);
        getCommand("bingonulle").setExecutor(new fr.kalium.bingo.command.DrawCommand(drawVotes));
        getServer().getPluginManager().registerEvents(new GameMenuListener(abandonConfirmMenu, drawVotes), this);

        // Chronometre + scores en permanence au-dessus de la barre de vie (action bar) - AJOUTE le
        // 24/09/2026, demande explicite de l'utilisateur : "le chrono de la partie doit être
        // affiché sur l'écran au dessus de la barre de vie, avec les scores actuels de chaque
        // équipe." Rafraichi chaque seconde (l'action bar s'efface sinon d'elle-meme).
        // 0.1.19 : regeneration dans la salle d'attente (demande explicite, voir LobbyRegenTask).
        getServer().getScheduler().runTaskTimer(this, new LobbyRegenTask(lobbySlots), 20L, 20L);

        GameHudService gameHudService = new GameHudService(gameManager);
        getServer().getScheduler().runTaskTimer(this, gameHudService::tick, 20L, 20L);

        // Detection automatique de validation des objectifs (section 3, jamais implementee avant -
        // voir ObjectiveValidationTask pour le detail du choix de mecanique, confirme par
        // l'utilisateur). Toutes les secondes : suffisant pour une detection par inventaire, pas
        // besoin de reagir au tick pres. Verifie aussi la victoire (GameEndService.checkWin) des
        // qu'une case vient d'etre validee, et sauvegarde la progression (GamePersistence).
        ObjectiveValidationTask objectiveValidationTask = new ObjectiveValidationTask(gameManager, gameEndService, gamePersistence, gameItems);
        getServer().getScheduler().runTaskTimer(this, objectiveValidationTask::tick, 20L, 20L);

        // Fin par temps ecoule/abandon (GameEndService.tick, independant de la validation des
        // objectifs) et expulsion automatique de la salle d'attente post-partie (10 min par defaut) -
        // meme cadence que la validation des objectifs, pas besoin de plus de precision.
        getServer().getScheduler().runTaskTimer(this, gameEndService::tick, 20L, 20L);

        // 0.3.0 : expulsion pour inactivite (5 min sans action par defaut), voir InactivityService.
        fr.kalium.bingo.game.InactivityService inactivity = new fr.kalium.bingo.game.InactivityService(gameManager,
                Duration.ofSeconds(config.getLong("game.inactivity-kick-seconds", 300)));
        getServer().getPluginManager().registerEvents(inactivity, this);
        getServer().getScheduler().runTaskTimer(this, inactivity::tick, 20L, 20L);

        // Filet de securite (voir GamePersistence) : sauvegarde periodique de toutes les parties EN
        // COURS, en plus de la sauvegarde a chaque validation d'objectif et au lancement - couvre le
        // cas (rare) ou une sauvegarde ponctuelle n'aurait pas eu le temps de s'executer avant un
        // crash. Toutes les 60s : le chronometre etant sauvegarde comme "temps restant" (pas comme
        // instant absolu), une legere marge ici ne fait que rendre le rechargement un peu plus
        // genereux, jamais moins (voir BingoGame.restoreInProgress).
        getServer().getScheduler().runTaskTimer(this, () -> gamePersistence.saveAll(gameManager.getActiveGames()), 1200L, 1200L);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord",
                new AssignmentNetworkListener(this, assignmentService));
        getServer().getPluginManager().registerEvents(
                new PlayerConnectListener(partyManager, gameManager, assignmentService, gameItems, playerReset), this);

        // Previent kal-games quand une partie en salle d'attente demarre/est annulee - AJOUTE le
        // 24/09/2026, demande explicite de l'utilisateur (liste des parties Bingo encore en
        // salle d'attente, rejoignable depuis le menu KalGames) - voir PartyStatusNotifier.
        PartyStatusNotifier partyStatusNotifier = new PartyStatusNotifier(this, kalGamesServerName, relayClient);

        // Delai minimum avant qu'une partie en salle d'attente puisse reellement demarrer (10s *
        // nombre d'equipes) + chronometre si l'hote/un operateur lance trop tot - AJOUTE le
        // 24/09/2026, demande explicite de l'utilisateur, voir PartyCountdownService.
        PartyCountdownService partyCountdownService = new PartyCountdownService(this);

        PartyStarter partyStarter = new PartyStarter(getLogger(), this, gameManager, partyManager, lobbySlots,
                lobbyItems, gameItems, relayClient, gamePersistence, partyStatusNotifier, partyCountdownService, playerReset,
                instanceWorldPreparer);
        PartyCanceller partyCanceller = new PartyCanceller(partyManager, lobbySlots, assignmentService,
                instanceWorldPreparer, partyStatusNotifier, partyCountdownService);

        LobbyCaptureService lobbyCaptureService = new LobbyCaptureService(lobbyTemplateService, lobbySlots);

        var adminCommand = new BingoAdminCommand(lobbyCaptureService, partyStarter, gameManager, objectiveLibrary);
        getCommand("bingoadmin").setExecutor(adminCommand);

        var teamCommand = new TeamCommand(partyManager);
        getCommand("bingoteam").setExecutor(teamCommand);

        // Menu graphique (/menu, reserve aux operateurs via la permission bingo.admin - voir
        // plugin.yml) pour capturer/modifier la salle d'attente : demande explicite de
        // l'utilisateur, "je ne comprends pas l'utilisation de la commande".
        LobbyMenu lobbyMenu = new LobbyMenu(lobbyCaptureService);
        getServer().getPluginManager().registerEvents(lobbyMenu, this);
        getCommand("menu").setExecutor(new MenuCommand(lobbyMenu));
        // 0.5.0 : interface declaree a KLM_Menu (catalogue "Interfaces" de la boussole), operateurs seulement.
        getServer().getServicesManager().register(fr.kalium.menu.api.MenuSection.class,
                new fr.kalium.menu.api.MenuSection() {
                    @Override
                    public String id() {
                        return "lobby";
                    }

                    @Override
                    public org.bukkit.plugin.Plugin owner() {
                        return BingoPlugin.this;
                    }

                    @Override
                    public net.kyori.adventure.text.Component title() {
                        return net.kyori.adventure.text.Component.text("Salle d'attente Bingo", net.kyori.adventure.text.format.NamedTextColor.GOLD);
                    }

                    @Override
                    public net.kyori.adventure.text.Component description() {
                        return net.kyori.adventure.text.Component.text("Capturer ou modifier le modèle de la salle d'attente (comme /menu).",
                                net.kyori.adventure.text.format.NamedTextColor.GRAY);
                    }

                    @Override
                    public Audience audience() {
                        return Audience.ADMINS;
                    }

                    @Override
                    public boolean visibleTo(org.bukkit.entity.Player player) {
                        return player.hasPermission("bingo.admin");
                    }

                    @Override
                    public void open(org.bukkit.entity.Player player, java.util.function.Consumer<org.bukkit.entity.Player> back) {
                        lobbyMenu.open(player);
                    }
                }, this, org.bukkit.plugin.ServicePriority.Normal);

        // Menu JOUEUR de la salle d'attente (objet Nether Star verrouille, voir LobbyItems) : vue
        // des equipes + lancer/annuler la partie (hote uniquement) - demande explicite de
        // l'utilisateur, meme principe que le "menu de la partie" de KalGames. En Dialog natif
        // depuis la 0.1.9 (pas un inventaire) - PartyMenu n'a donc plus besoin d'etre enregistre
        // comme Listener (plus de clic d'inventaire a intercepter, uniquement des boutons de
        // Dialog). Assignation des equipes reservee a l'hote depuis la 0.1.9 (voir PartyMenu).
        // Protection de la salle d'attente (casse/pose/interaction de blocs, spawn de monstres,
        // pvp) regroupee dans le meme listener que le clic sur l'objet.
        PartyMenu partyMenu = new PartyMenu(this, partyManager, partyStarter, partyCanceller);
        // Menu de retour post-partie (nether star de la salle d'attente APRES la fin d'une partie,
        // voir GameEndService/PostGameMenu) - AJOUTE le 23/09/2026, demande explicite de
        // l'utilisateur : "une netherstar pour qu'ils puissent retourner au hub kalgames (via un menu)".
        fr.kalium.bingo.gui.SummaryMenu summaryMenu = new fr.kalium.bingo.gui.SummaryMenu();
        getServer().getPluginManager().registerEvents(summaryMenu, this);
        PostGameMenu postGameMenu = new PostGameMenu(this, gameEndService, summaryMenu);
        getServer().getPluginManager().registerEvents(
                new LobbyProtectionListener(lobbySlots, lobbyItems, partyMenu, gameEndService, postGameMenu), this);

        getLogger().info("[KG_BingoGame] Plugin active (architecture parties/instances par equipe, mondes a seed partagee, "
                + "salle d'attente + choix d'equipe, reception d'affectation depuis kal-games).");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getLogger().info("[KG_BingoGame] Plugin desactive.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public LobbyTemplateService getLobbyTemplateService() {
        return lobbyTemplateService;
    }

    public LobbySlots getLobbySlots() {
        return lobbySlots;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    /**
     * Permet a ce plugin de fournir le generateur d'un monde arbitraire (notamment le monde par
     * defaut du serveur), via le mapping "worlds: &lt;nom&gt;: generator: KG_BingoGame" de bukkit.yml.
     * Reutilise VoidGenerator (deja utilise pour bingo_lobby) : le monde est alors entierement vide.
     * Sert a remplacer la map par defaut de Kixster par un monde vide (demande explicite de
     * l'utilisateur), sans dependre d'un plugin tiers type Multiverse.
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidGenerator();
    }
}
