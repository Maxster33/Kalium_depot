package fr.kalium.pvpkit;

import fr.kalium.games.KalGames;
import fr.kalium.games.game.GameInstance;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.model.MinigameType;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fr.kalium.games.model.PointSpec.single;
import static fr.kalium.games.model.SettingSpec.bool;
import static fr.kalium.games.model.SettingSpec.integer;

/**
 * KG_PvpKit : le PvP Kit de Kal-Games, sorti de KalGames (REGLES.md, regle 2.2), comme KG_BoatRace et KG_Parkour.
 * S'appuie sur le moteur de parties de KalGames (arenes, files d'attente, parties publiques et privees) : ce plugin
 * enregistre le type « PVP_KIT » (reglages, points d'arene, moteur, formulaire de partie privee, kits).
 *
 * Meme nom de type et memes points d'arene qu'avant : le mini-jeu, ses arenes, ses classements et sa liste de kits
 * (minigames.yml de KalGames) sont repris tels quels. Les kits sont recopies de KalGames au premier demarrage (voir
 * KitLibrary). Nouveau bareme (points decimaux, voir PvpInstance) : nouvelles cles de reglage, les anciennes
 * (points-win, points-bonus) ne sont plus utilisees.
 */
public final class KGPvpKit extends JavaPlugin implements Listener {

    /** Delai pendant lequel le dernier joueur qui a frappe la victime obtient l'elimination (chute dans le vide...). */
    private static final long LAST_HIT_MILLIS = 10_000;

    private static KGPvpKit instance;

    private KalGames games;
    private KitLibrary kits;
    private PvpMenus menus;
    private record Hit(UUID attacker, long at) {
    }
    private final Map<UUID, Hit> lastHits = new HashMap<>();
    /** Manches gagnees d'affilee par joueur (multiplicateur de serie) ; remis a zero au redemarrage du serveur. */
    private final Map<UUID, Integer> streaks = new HashMap<>();

    public static KGPvpKit get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        games = (KalGames) getServer().getPluginManager().getPlugin("KalGames");
        kits = new KitLibrary(this);
        kits.load();
        menus = new PvpMenus(this, games);
        getServer().getPluginManager().registerEvents(this, this);

        MinigameType type = new MinigameType("PVP_KIT", "PvP Kit",
                "Combat d'équipes (jusqu'à 4) avec vote du kit. Dernière équipe en vie.",
                List.of(
                        integer("vote-seconds", "Durée du vote de kit (s)", 60, 10, 180, "Le vote se termine plus tôt si tout le monde a voté."),
                        integer("countdown-seconds", "Compte à rebours avant combat (s)", 3, 0, 10, "0 = combat immediat."),
                        integer("end-delay-seconds", "Délai après la victoire (s)", 5, 1, 30, "Avant le retour en tribune."),
                        integer("points-kill", "Points par élimination", 5, 0, 100, "Pour chaque adversaire éliminé, même si son équipe perd la manche."),
                        integer("points-round-win", "Points de victoire (par manche)", 10, 0, 100, "Pour chaque joueur de l'équipe gagnante."),
                        integer("points-second", "Points de la 2e place", 4, 0, 100, "À 3 ou 4 équipes : dernière équipe éliminée."),
                        integer("points-third", "Points de la 3e place", 2, 0, 100, "À 4 équipes."),
                        integer("points-inferiority", "Bonus par joueur d'écart", 5, 0, 100, "Ajouté aux gagnants quand leur équipe était en infériorité numérique."),
                        integer("streak-1-wins", "Série : 1er palier (manches gagnées d'affilée)", 3, 2, 50, "À partir de ce nombre de manches gagnées d'affilée."),
                        integer("streak-1-bonus-pct", "Série : bonus du 1er palier (%)", 25, 0, 200, "Multiplicateur ajouté (25 = x1,25)."),
                        integer("streak-2-wins", "Série : 2e palier (manches gagnées d'affilée)", 5, 2, 50, "À partir de ce nombre de manches gagnées d'affilée."),
                        integer("streak-2-bonus-pct", "Série : bonus du 2e palier (%)", 50, 0, 200, "Multiplicateur ajouté (50 = x1,5)."),
                        integer("downgrade-max-level", "Déclassement : niveau maximum", 4, 0, 4, "0 = pas de déclassement."),
                        integer("downgrade-bonus-pct", "Déclassement : bonus par niveau (%)", 50, 0, 200, "Multiplicateur ajouté par niveau (50 : niveau 4 = x3)."),
                        integer("downgrade-consumables-pct", "Déclassement : consommables retirés par niveau (%)", 20, 0, 25, "Aliments, potions, perles, flèches, totems..."),
                        integer("public-team-size", "Partie publique : joueurs par équipe", 1, 1, 4, "1 = chacun pour soi."),
                        integer("public-min-teams", "Partie publique : équipes minimum", 2, 2, 4, "Nombre d'équipes pour lancer un match."),
                        integer("public-max-teams", "Partie publique : équipes maximum", 4, 2, 4, "Un match démarre dès que ce nombre est atteint."),
                        integer("public-gather-seconds", "Partie publique : attente avant lancement (s)", 30, 5, 180, "Délai dès que le minimum est atteint."),
                        integer("prewarm-arenas", "Copies de chaque arène préchargées au démarrage", 0, 0, 10, "Collées dès le démarrage du serveur et gardées de côté : aucune arène à charger au lancement d'une partie (0 = aucune)."),
                        integer("max-private-games", "Parties privées simultanées maximum", 0, 0, 40, "0 = pas de limite."),
                        bool("break-map", "Casser les blocs de la carte", false, "Non : seuls les blocs posés pendant le match sont cassables. Tout est restauré à la fin."),
                        bool("bedrock-option", "Option PvP Bedrock (Haste) proposée", true, "Propose la case Haste dans les parties privées."),
                        bool("allow-spectate", "Autoriser le mode spectateur", true, "Les joueurs peuvent regarder la partie (publique ou privée) sans y participer (vol libre).")),
                List.of(
                        single("stands", "Gradins (tribune)", true, "Où attendent les spectateurs et les éliminés."),
                        single("spawn-a", "Départ équipe A", true, "Regarde vers le centre."),
                        single("spawn-b", "Départ équipe B", true, "Regarde vers le centre."),
                        single("spawn-c", "Départ équipe C", false, "Optionnel : active la 3e équipe."),
                        single("spawn-d", "Départ équipe D", false, "Optionnel : active la 4e équipe.")))
                .engine(PvpInstance::new)
                .prewarmAllowed(true)
                .createForm(new CreateForm())
                .adminInfo(minigame -> List.of(games.t("admin.mg-kits", "<gray>Kits proposés au vote : <white><n></white>",
                        "n", minigame.kits().size())))
                .adminAction(new MinigameType.AdminAction(games.t("admin.mg-kit-pick", "<aqua>Kits du mini-jeu"),
                        menus::openMinigameKits));
        MinigameType.register(type);
        getLogger().info("PvP Kit enregistré auprès de KalGames (" + kits.all().size() + " kit(s)).");
    }

    /**
     * A l'arret, Paper desactive ce plugin AVANT KalGames : les parties de PvP Kit en cours sont fermees ici, tant que le
     * code de ce plugin est encore disponible (comme KG_Parkour).
     */
    @Override
    public void onDisable() {
        if (games != null && games.instances() != null) {
            for (GameInstance game : new ArrayList<>(games.instances().all())) {
                if (game instanceof PvpInstance) {
                    games.instances().close(game, null);
                }
            }
        }
        instance = null;
    }

    public KitLibrary kits() {
        return kits;
    }

    public PvpMenus menus() {
        return menus;
    }

    // ------------------------------------------------------------------ series

    public int streak(UUID uuid) {
        return streaks.getOrDefault(uuid, 0);
    }

    public int streak(UUID uuid, int value) {
        streaks.put(uuid, value);
        return value;
    }

    // ------------------------------------------------------------------ dernier coup recu

    /** Joueur qui a frappe la victime en dernier (10 s maximum), ou null. */
    public Player lastAttacker(Player victim) {
        Hit hit = lastHits.get(victim.getUniqueId());
        if (hit == null || System.currentTimeMillis() - hit.at() > LAST_HIT_MILLIS) {
            return null;
        }
        return Bukkit.getPlayer(hit.attacker());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damager = shooter;
        }
        if (damager instanceof Player attacker && !attacker.equals(victim)
                && games.instances().of(victim) instanceof PvpInstance) {
            lastHits.put(victim.getUniqueId(), new Hit(attacker.getUniqueId(), System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastHits.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ creation d'une partie privee

    /** Formulaire de creation d'une partie privee (repris de KalGames 1.21.0 : equipes, Haste, manches, choix du kit). */
    private final class CreateForm implements MinigameType.CreateForm {

        @Override
        public List<DialogInput> inputs(Minigame minigame, List<Arena> arenas, fr.kalium.games.gui.Gui gui) {
            List<DialogInput> inputs = new ArrayList<>();
            int maxTeams = 2;
            for (Arena arena : arenas) {
                maxTeams = Math.max(maxTeams, PvpInstance.teamsAvailable(arena));
            }
            if (maxTeams > 2) {
                inputs.add(gui.number("teams", games.t("menu.create-teams", "Nombre d'équipes"), 2, maxTeams, 2, 1));
            }
            inputs.add(gui.number("teamSize", games.t("menu.create-team-size", "Joueurs par équipe"), 1, 4, 1, 1));
            if (minigame.getBool("bedrock-option", true)) {
                inputs.add(gui.toggle("haste", games.t("menu.create-haste", "PvP Bedrock (Haste, clic spam)"), false));
            }
            inputs.add(gui.choice("rounds", games.t("menu.create-rounds", "Nombre de manches"),
                    List.of("1", "3", "5"),
                    List.of(games.t("menu.rounds-1", "1 manche"), games.t("menu.rounds-3", "3 manches (première équipe à 2 victoires)"),
                            games.t("menu.rounds-5", "5 manches (première équipe à 3 victoires)")), "1"));
            inputs.add(gui.choice("kitMode", games.t("menu.create-kitmode", "Choix du kit"),
                    List.of("vote", "random"),
                    List.of(games.t("menu.kitmode-vote", "Vote avant chaque manche"),
                            games.t("menu.kitmode-random", "Kit aléatoire à chaque manche (le même pour toutes les équipes)")), "vote"));
            return inputs;
        }

        @Override
        public void read(io.papermc.paper.dialog.DialogResponseView view, Map<String, Object> options) {
            for (String key : List.of("teams", "teamSize")) {
                Float value = view.getFloat(key);
                if (value != null) {
                    options.put(key, Math.round(value));
                }
            }
            Boolean haste = view.getBoolean("haste");
            if (haste != null) {
                options.put("haste", haste);
            }
            String roundsText = view.getText("rounds");
            if (roundsText != null) {
                try {
                    options.put("rounds", Integer.parseInt(roundsText.trim()));
                } catch (NumberFormatException ignored) {
                    options.put("rounds", 1);
                }
            }
            String kitMode = view.getText("kitMode");
            if (kitMode != null) {
                options.put("kitMode", kitMode);
            }
        }
    }
}
