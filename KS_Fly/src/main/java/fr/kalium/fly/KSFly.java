package fr.kalium.fly;

import fr.kalium.jetons.KSJetons;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Fly (cahier des charges : catégorie 7, ajouts « jetons et badges » de LeKiwi06, 08/10/2026) : serveur Event.
 *
 * Voler dans ses claims (propriétaire ou membre), en survie, avec un jeton ou un badge de fly (KS_Jetons) :
 * - Jeton de fly : 10 minutes de vol, d'affilée à partir de l'activation. Il s'active par une fenêtre qui s'ouvre
 *   quand le joueur essaie de voler (double saut) sans jeton ni badge actif ; « Ne plus m'afficher cette fenêtre » :
 *   un jeton est alors activé d'office à chaque essai (réglage rétabli par /fly).
 * - Badge de fly : 1 à 15 minutes de vol par heure selon son niveau, décomptées seulement en vol ; la réserve se
 *   remplit d'un coup 1 heure après le début de son utilisation. Délai tenu par joueur (changer de badge ne recharge
 *   rien). Le badge sert avant un jeton.
 * - Compteur en barre de boss. Fin du temps ou sortie de ses claims en vol : le vol s'arrête, les dégâts de chute
 *   s'appliquent.
 *
 * Le serveur ne voit le double saut que si le vol est autorisé : il l'est donc, dans ses claims, pour le joueur qui a
 * de quoi voler (temps de jeton, réserve de badge ou jeton en stock), et retiré dès que ce n'est plus le cas. Les
 * dégâts de chute restent actifs pendant ce temps (sans cela, le vol autorisé les supprime).
 */
public final class KSFly extends JavaPlugin implements Listener {

    /** Ce que le plugin retient d'un joueur. */
    private static final class Etat {
        /** Fin du temps de vol du jeton activé (millisecondes), 0 : aucun. */
        long jetonFin;
        /** Début de l'utilisation de la réserve du badge (millisecondes), 0 : réserve pleine. */
        long badgeDebut;
        /** Secondes de vol prises sur la réserve du badge depuis badgeDebut. */
        double badgeUtilise;
        /** « Ne plus m'afficher cette fenêtre » : un jeton s'active d'office. */
        boolean auto;
    }

    private static final long PERIODE = 5L;

    private Lang lang;
    private Gui gui;
    private File fichier;
    private NamespacedKey cleGere;
    private final Map<UUID, Etat> etats = new HashMap<>();
    /** Joueurs dont le vol est autorisé par ce plugin (marqueur aussi sur le joueur, pour le retirer après un arrêt). */
    private final Set<UUID> geres = new HashSet<>();
    private final Map<UUID, BossBar> barres = new HashMap<>();
    private long dernierPassage;
    private boolean modifie;
    private int passages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cleGere = new NamespacedKey(this, "vol");
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "fly.yml");
        charger();
        // SCS 1.13.1 ne crée pas son API lui-même (sans effet si c'est déjà fait).
        SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                getServer().getPluginManager().getPlugin("SimpleClaimSystem"));
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("fly").setExecutor(this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "fly", lang.c("bouton.nom", "<aqua><bold>Fly"),
                    lang.c("bouton.description", "<gray>Voler dans tes claims : jetons, badge, réglage"), 42, this::ouvrir);
        }
        dernierPassage = System.currentTimeMillis();
        getServer().getScheduler().runTaskTimer(this, this::passage, PERIODE, PERIODE);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            lacher(joueur);
            cacher(joueur);
        }
        sauver();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private long dureeJeton() {
        return Math.max(1, getConfig().getLong("jeton-minutes", 10)) * 60_000L;
    }

    private long recharge() {
        return Math.max(1, getConfig().getLong("badge-recharge-minutes", 60)) * 60_000L;
    }

    private Etat etat(UUID joueur) {
        return etats.computeIfAbsent(joueur, u -> new Etat());
    }

    // ------------------------------------------------------------------ ce que le joueur a

    /** Dans un claim dont il est propriétaire ou membre (le propriétaire est membre de son claim dans SCS). */
    private static boolean chezLui(Player joueur) {
        Claim claim = SimpleClaimSystemAPI_Provider.getAPI().getClaimAtChunk(joueur.getLocation().getChunk());
        return claim != null && claim.isMember(joueur.getUniqueId());
    }

    private static boolean survie(Player joueur) {
        return joueur.getGameMode() == GameMode.SURVIVAL || joueur.getGameMode() == GameMode.ADVENTURE;
    }

    private boolean jetonActif(Etat etat, long maintenant) {
        return etat.jetonFin > maintenant;
    }

    /** Secondes de vol de la réserve pleine du badge porté (0 sans badge). */
    private static double badgeMax(UUID joueur) {
        return KSJetons.valeurBadge(joueur, KSJetons.Badge.FLY) * 60.0;
    }

    /** Secondes de vol restantes sur la réserve du badge ; la réserve se remplit d'un coup après le délai. */
    private double badgeReste(UUID joueur, Etat etat, long maintenant) {
        if (etat.badgeDebut > 0 && maintenant >= etat.badgeDebut + recharge()) {
            etat.badgeDebut = 0;
            etat.badgeUtilise = 0;
            modifie = true;
        }
        return Math.max(0, badgeMax(joueur) - etat.badgeUtilise);
    }

    private static String chrono(double secondes) {
        long s = Math.max(0, (long) Math.ceil(secondes));
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    // ------------------------------------------------------------------ passage régulier

    /** Quatre fois par seconde : autorise ou retire le vol, décompte la réserve du badge, met la barre à jour. */
    private void passage() {
        long maintenant = System.currentTimeMillis();
        double ecoule = Math.min(2000, maintenant - dernierPassage) / 1000.0;
        dernierPassage = maintenant;
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            UUID id = joueur.getUniqueId();
            if (!survie(joueur)) {
                // Créatif, spectateur : le vol du mode de jeu n'est pas touché.
                if (geres.remove(id)) {
                    joueur.getPersistentDataContainer().remove(cleGere);
                    joueur.setFlyingFallDamage(TriState.NOT_SET);
                }
                cacher(joueur);
                continue;
            }
            Etat etat = etat(id);
            boolean jeton = jetonActif(etat, maintenant);
            double reste = badgeReste(id, etat, maintenant);
            boolean chez = chezLui(joueur);
            if (chez && (jeton || reste > 0 || KSJetons.nombre(id, KSJetons.Type.FLY) > 0)) {
                prendre(joueur);
                if (joueur.isFlying() && !jeton) {
                    if (reste <= 0) {
                        // Le temps du jeton vient de finir, sans réserve de badge pour prendre la suite.
                        joueur.setFlying(false);
                        joueur.sendMessage(t("fin.temps", "<red>Ton temps de vol est écoulé : fin du vol."));
                    } else {
                        if (etat.badgeDebut == 0) {
                            etat.badgeDebut = maintenant;
                        }
                        etat.badgeUtilise += ecoule;
                        modifie = true;
                        reste = Math.max(0, reste - ecoule);
                        if (reste <= 0) {
                            joueur.setFlying(false);
                            joueur.sendMessage(t("fin.badge", "<red>La réserve de ton badge de fly est vide : fin du vol."));
                        }
                    }
                }
            } else if (geres.contains(id)) {
                boolean volait = joueur.isFlying();
                lacher(joueur);
                if (volait) {
                    joueur.sendMessage(chez ? t("fin.temps", "<red>Ton temps de vol est écoulé : fin du vol.")
                            : t("fin.claim", "<red>Tu sors de tes claims : fin du vol."));
                }
            }
            barre(joueur, jeton ? etat.jetonFin - maintenant : -1, joueur.isFlying() && !jeton ? reste : -1, badgeMax(id));
        }
        // Le temps du badge change en continu : enregistré toutes les 30 secondes.
        if (++passages % 120 == 0 && modifie) {
            sauver();
        }
        lang.saveIfNeeded();
    }

    /** Autorise le vol (le double saut devient visible), en gardant les dégâts de chute. */
    private void prendre(Player joueur) {
        if (geres.add(joueur.getUniqueId())) {
            joueur.getPersistentDataContainer().set(cleGere, PersistentDataType.BYTE, (byte) 1);
            joueur.setFlyingFallDamage(TriState.TRUE);
        }
        if (!joueur.getAllowFlight()) {
            joueur.setAllowFlight(true);
        }
    }

    /** Retire le vol autorisé par ce plugin (le joueur tombe s'il volait). */
    private void lacher(Player joueur) {
        if (!geres.remove(joueur.getUniqueId())) {
            return;
        }
        joueur.getPersistentDataContainer().remove(cleGere);
        if (survie(joueur)) {
            joueur.setFlying(false);
            joueur.setAllowFlight(false);
        }
        joueur.setFlyingFallDamage(TriState.NOT_SET);
    }

    // ------------------------------------------------------------------ barre de boss

    /** Temps du jeton (millisecondes) ou réserve du badge (secondes) à afficher ; -1 : rien. */
    private void barre(Player joueur, long jetonMs, double badgeS, double badgeMax) {
        if (jetonMs < 0 && badgeS < 0) {
            cacher(joueur);
            return;
        }
        Component nom;
        float progression;
        BossBar.Color couleur;
        if (jetonMs >= 0) {
            nom = t("barre.jeton", "<aqua>Jeton de fly</aqua> <white>: <temps>", "temps", chrono(jetonMs / 1000.0));
            progression = (float) jetonMs / dureeJeton();
            couleur = BossBar.Color.BLUE;
        } else {
            nom = t("barre.badge", "<gold>Badge de fly</gold> <white>: <temps>", "temps", chrono(badgeS));
            progression = badgeMax <= 0 ? 0 : (float) (badgeS / badgeMax);
            couleur = BossBar.Color.YELLOW;
        }
        progression = Math.max(0f, Math.min(1f, progression));
        BossBar barre = barres.get(joueur.getUniqueId());
        if (barre == null) {
            barre = BossBar.bossBar(nom, progression, couleur, BossBar.Overlay.PROGRESS);
            barres.put(joueur.getUniqueId(), barre);
            joueur.showBossBar(barre);
        } else {
            barre.name(nom);
            barre.progress(progression);
            barre.color(couleur);
        }
    }

    private void cacher(Player joueur) {
        BossBar barre = barres.remove(joueur.getUniqueId());
        if (barre != null) {
            joueur.hideBossBar(barre);
        }
    }

    // ------------------------------------------------------------------ essai de vol

    /** Double saut : permis s'il reste du temps de jeton ou de la réserve de badge ; sinon un jeton est proposé. */
    @EventHandler(ignoreCancelled = true)
    public void onVol(PlayerToggleFlightEvent event) {
        Player joueur = event.getPlayer();
        UUID id = joueur.getUniqueId();
        if (!event.isFlying() || !geres.contains(id)) {
            return;
        }
        long maintenant = System.currentTimeMillis();
        Etat etat = etat(id);
        if (!chezLui(joueur)) {
            event.setCancelled(true);
            return;
        }
        // Le badge sert avant un jeton ; un jeton déjà activé continue de servir (son temps court de toute façon).
        if (jetonActif(etat, maintenant) || badgeReste(id, etat, maintenant) > 0) {
            return;
        }
        if (KSJetons.nombre(id, KSJetons.Type.FLY) < 1) {
            event.setCancelled(true);
            return;
        }
        if (etat.auto) {
            if (!activer(joueur, false)) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        fenetre(joueur);
    }

    /** Fenêtre du jeton de fly, avec « Ne plus m'afficher cette fenêtre ». */
    private void fenetre(Player joueur) {
        long n = KSJetons.nombre(joueur.getUniqueId(), KSJetons.Type.FLY);
        ActionButton activer = gui.form(t("fenetre.activer", "<green>Activer un jeton"), null, (p, vue) -> {
            if (Boolean.TRUE.equals(vue.getBoolean("auto"))) {
                etat(p.getUniqueId()).auto = true;
                modifie = true;
            }
            activer(p, true);
        });
        gui.open(joueur, t("fenetre.titre", "<aqua><bold>Jeton de fly"),
                List.of(t("fenetre.texte", "<white>Activer un jeton de fly ? <gray>Il donne <minutes> minutes de vol dans "
                                + "tes claims, d'affilée à partir de maintenant. Tu en as <white><n></white>.",
                        "minutes", dureeJeton() / 60_000L, "n", n)),
                List.of(gui.toggle("auto", t("fenetre.auto", "Ne plus m'afficher cette fenêtre"), false)),
                List.of(activer), gui.close(t("fenetre.annuler", "<red>Annuler")), 1);
        lang.saveIfNeeded();
    }

    /**
     * Consomme un jeton de fly et lance son temps de vol. decoller : fait aussi décoller le joueur (depuis la fenêtre ;
     * pendant le double saut, c'est le jeu qui le fait). Faux si rien n'a été activé.
     */
    private boolean activer(Player joueur, boolean decoller) {
        UUID id = joueur.getUniqueId();
        Etat etat = etat(id);
        long maintenant = System.currentTimeMillis();
        if (jetonActif(etat, maintenant)) {
            return true;
        }
        if (!survie(joueur) || !chezLui(joueur)) {
            joueur.sendMessage(t("activer.hors-claim", "<red>Un jeton de fly ne s'active que dans tes claims."));
            return false;
        }
        if (!KSJetons.consommer(id, KSJetons.Type.FLY, 1)) {
            joueur.sendMessage(t("activer.aucun", "<red>Tu n'as plus de jeton de fly."));
            return false;
        }
        etat.jetonFin = maintenant + dureeJeton();
        sauver();
        prendre(joueur);
        if (decoller) {
            joueur.setFlying(true);
        }
        joueur.sendMessage(t("activer.fait", "<aqua>Jeton de fly activé : <white><minutes> minutes</white> de vol dans "
                + "tes claims.", "minutes", dureeJeton() / 60_000L));
        getLogger().info(joueur.getName() + " active un jeton de fly.");
        return true;
    }

    // ------------------------------------------------------------------ connexion

    /** Vol resté autorisé après un arrêt brutal du serveur : retiré. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        if (joueur.getPersistentDataContainer().has(cleGere, PersistentDataType.BYTE)) {
            geres.add(joueur.getUniqueId());
            lacher(joueur);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lacher(event.getPlayer());
        cacher(event.getPlayer());
        if (modifie) {
            sauver();
        }
    }

    // ------------------------------------------------------------------ /fly

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            ouvrir(joueur);
        } else {
            sender.sendMessage("Commande réservée aux joueurs.");
        }
        return true;
    }

    /** Où en est le joueur : jetons, temps du jeton activé, réserve du badge, réglage de la fenêtre. */
    public void ouvrir(Player joueur) {
        UUID id = joueur.getUniqueId();
        Etat etat = etat(id);
        long maintenant = System.currentTimeMillis();
        List<Component> corps = new ArrayList<>();
        corps.add(t("menu.aide", "<gray>Dans tes claims (propriétaire ou membre), fais un double saut pour voler. Ton "
                + "badge de fly sert d'abord ; sinon un jeton de fly est proposé."));
        corps.add(t("menu.jetons", "<aqua>Jetons de fly</aqua> <gray>: <white><n>", "n",
                KSJetons.nombre(id, KSJetons.Type.FLY)));
        if (jetonActif(etat, maintenant)) {
            corps.add(t("menu.jeton-actif", "<aqua>Jeton activé</aqua> <gray>: encore <white><temps>", "temps",
                    chrono((etat.jetonFin - maintenant) / 1000.0)));
        }
        double max = badgeMax(id);
        if (max <= 0) {
            corps.add(t("menu.badge-aucun", "<gold>Badge de fly</gold> <gray>: aucun"));
        } else {
            double reste = badgeReste(id, etat, maintenant);
            corps.add(etat.badgeDebut == 0
                    ? t("menu.badge-plein", "<gold>Badge de fly</gold> <gray>: <white><temps></white> de vol, réserve pleine",
                    "temps", chrono(reste))
                    : t("menu.badge", "<gold>Badge de fly</gold> <gray>: <white><temps></white> de vol ; réserve pleine "
                    + "dans <white><attente>", "temps", chrono(reste), "attente",
                    chrono((etat.badgeDebut + recharge() - maintenant) / 1000.0)));
        }
        corps.add(etat.auto ? t("menu.auto-oui", "<gray>Jeton de fly : <white>activé d'office</white> à chaque essai de vol.")
                : t("menu.auto-non", "<gray>Jeton de fly : <white>une fenêtre demande</white> avant d'en activer un."));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(etat.auto ? t("menu.bouton-fenetre", "<white>Remettre la fenêtre")
                : t("menu.bouton-auto", "<white>Activer d'office"), null, p -> {
            Etat e = etat(p.getUniqueId());
            e.auto = !e.auto;
            sauver();
            ouvrir(p);
        }));
        gui.open(joueur, t("menu.titre", "<aqua><bold>Fly"), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection section = yaml.getConfigurationSection("joueurs");
        if (section == null) {
            return;
        }
        for (String cle : section.getKeys(false)) {
            try {
                Etat etat = new Etat();
                etat.jetonFin = section.getLong(cle + ".jeton-fin");
                etat.badgeDebut = section.getLong(cle + ".badge-debut");
                etat.badgeUtilise = section.getDouble(cle + ".badge-utilise");
                etat.auto = section.getBoolean(cle + ".auto");
                etats.put(UUID.fromString(cle), etat);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Joueur ignoré dans fly.yml : " + cle);
            }
        }
    }

    private void sauver() {
        modifie = false;
        long maintenant = System.currentTimeMillis();
        YamlConfiguration yaml = new YamlConfiguration();
        etats.forEach((joueur, etat) -> {
            String chemin = "joueurs." + joueur + ".";
            if (etat.jetonFin > maintenant) {
                yaml.set(chemin + "jeton-fin", etat.jetonFin);
            }
            if (etat.badgeDebut > 0) {
                yaml.set(chemin + "badge-debut", etat.badgeDebut);
                yaml.set(chemin + "badge-utilise", etat.badgeUtilise);
            }
            if (etat.auto) {
                yaml.set(chemin + "auto", true);
            }
        });
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer fly.yml : " + e.getMessage());
        }
    }
}
