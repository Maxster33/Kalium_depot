package fr.kalium.malefices;

import fr.kalium.effets.KSEffets;
import fr.kalium.jetons.KSJetons;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * KS_MaleficesNether (cahier des charges : Malefices_Nether.md, validé par LeKiwi06 le 30/10/2026) : serveur Kixster.
 *
 * - Compteur par joueur : temps réellement passé dans le Nether, connecté, en survie ou en aventure (pause sinon et à
 *   la déconnexion). Un jour = 20 minutes (jamais les jours du jeu). Hors du Nether, il descend (un jour effacé en 10
 *   minutes) ; la mort le remet à zéro (pour retourner chercher son coffre de mort sans maléfice).
 * - Maléfices : au début de chaque jour N, chaque effet du jour ({@link Malefices}) tire au hasard N minutes
 *   différentes parmi les 20 du jour et s'active à chacune (1 minute, ou 10 s et plus pour l'obscurité, la lévitation
 *   et la nausée) ; au jour 20, continu. Effet sur le joueur seul, sans particules (icône gardée : KS_Atouts reconnaît
 *   ainsi sa propre lévitation), remis toutes les 5 s (le lait ne soulage que quelques secondes).
 * - Barre de boss rouge à chaque effet actif : « Maléfice (jour N) : Faim + Lenteur », temps du premier effet actif.
 *   Effet « Maléfice du Nether » dans /effects (KS_Effets).
 * - Plastron anti-radiation (atout de KS_Atouts) porté : durée de chaque activation divisée par 2.
 * - Amulette mangeuse de mal : craft en jambières (7 chaînes en cuivre, une balise en bas au milieu) ; rangée dans les
 *   équipements spéciaux de /jetons (KS_Jetons), elle retire chaque effet en prenant 5 points d'XP par seconde et par
 *   niveau (du plus dangereux au moins dangereux) ; sans assez d'XP, l'effet s'applique.
 */
public final class KSMaleficesNether extends JavaPlugin implements Listener {

    /** Effet custom de /effects (KS_Effets). */
    static final String EFFET = "ks_maleficesnether:malefice";
    /** Équipement spécial de KS_Jetons. */
    static final String EQUIPEMENT = "amulette";
    /** Atout de KS_Atouts. */
    static final String PLASTRON = "anti_radiation";
    static final String NOM_AMULETTE = "Amulette mangeuse de mal";

    /** Une activation d'un effet dans le jour en cours. */
    private static final class Activation {
        final Malefices.Etat etat;
        /** Seconde du jour où elle commence. */
        final int debut;
        /** Seconde du jour de la dernière pose de l'effet (avant le début : pas encore posé). */
        int posee = Integer.MIN_VALUE / 2;
        /** Durée de l'activation en cours (plastron compris). */
        int duree;

        Activation(Malefices.Etat etat, int debut) {
            this.etat = etat;
            this.debut = debut;
        }
    }

    /** Le compteur d'un compte (le compte de test dans le monde de test : KS_MondeTest). */
    private static final class Compteur {
        long secondes;
        /** Jour dont les activations ont été tirées (-1 : aucun). */
        int jourTire = -1;
        final List<Activation> activations = new ArrayList<>();
    }

    private static KSMaleficesNether instance;
    private static NamespacedKey cleAmulette;

    private Lang lang;
    private Malefices malefices;
    private NamespacedKey cleRecette;
    private int jourSecondes;
    private int descente;
    private int rafraichissement;
    private int amulettePoints;
    private int plastronDiviseur;
    private boolean atouts;
    private File fichier;
    private boolean modifie;
    private final Map<UUID, Compteur> compteurs = new HashMap<>();
    /** Barre de boss affichée, par joueur. */
    private final Map<UUID, BossBar> barres = new HashMap<>();
    private final Random hasard = new Random();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        cleAmulette = new NamespacedKey(this, "amulette");
        cleRecette = new NamespacedKey(this, "amulette");
        lang = new Lang(this);
        jourSecondes = Math.max(60, getConfig().getInt("jour-secondes", 1200));
        descente = Math.max(0, getConfig().getInt("descente-par-seconde", 2));
        rafraichissement = Math.max(1, getConfig().getInt("rafraichissement-secondes", 5));
        amulettePoints = Math.max(0, getConfig().getInt("amulette-points", 5));
        plastronDiviseur = Math.max(1, getConfig().getInt("plastron-diviseur", 2));
        malefices = new Malefices(getConfig(), getLogger());
        atouts = getServer().getPluginManager().isPluginEnabled("KS_Atouts");
        fichier = new File(getDataFolder(), "donnees.yml");
        charger();
        recette();
        KSJetons.declarerEquipement(EQUIPEMENT, NOM_AMULETTE, KSMaleficesNether::estAmulette);
        KSEffets.declarer(EFFET, Component.text("Maléfice du Nether", NamedTextColor.RED),
                new ItemStack(Material.CRYING_OBSIDIAN));
        KSEffets.fournisseur(EFFET, this::restantEffet);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::seconde, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (modifie) {
                sauver();
            }
        }, 1200L, 1200L);
        Bukkit.getOnlinePlayers().forEach(this::livreDeRecettes);
        getLogger().info(malefices.taille() + " effets de maléfice chargés ; un jour = " + jourSecondes + " s.");
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        for (UUID id : new ArrayList<>(barres.keySet())) {
            Player joueur = Bukkit.getPlayer(id);
            if (joueur != null) {
                cacher(joueur);
            }
        }
        Bukkit.removeRecipe(cleRecette);
        sauver();
        instance = null;
    }

    private Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    // ------------------------------------------------------------------ amulette

    /** L'amulette mangeuse de mal (objet). */
    public static ItemStack creerAmulette() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(texte(NOM_AMULETTE, NamedTextColor.LIGHT_PURPLE));
        int points = instance == null ? 5 : instance.amulettePoints;
        meta.lore(List.of(texte("Rangée dans tes équipements spéciaux,", NamedTextColor.GRAY),
                texte("elle absorbe les maléfices du Nether", NamedTextColor.GRAY),
                texte("en prenant " + points + " points d'XP par seconde", NamedTextColor.GRAY),
                texte("et par niveau de chaque effet retiré.", NamedTextColor.GRAY),
                texte("À ranger dans tes équipements : /jetons", NamedTextColor.DARK_GRAY)));
        // Apparence provisoire, en attendant la texture de LeKiwi06 (KS_Textures, geyser-bedrock).
        meta.setItemModel(NamespacedKey.minecraft("heart_of_the_sea"));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(cleAmulette, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** Cet objet est-il une amulette mangeuse de mal ? */
    public static boolean estAmulette(ItemStack item) {
        return item != null && cleAmulette != null && !item.isEmpty() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleAmulette, PersistentDataType.BYTE);
    }

    private static Component texte(String texte, NamedTextColor couleur) {
        return Component.text(texte, couleur).decoration(TextDecoration.ITALIC, false);
    }

    /** Craft en forme de jambières : 7 chaînes en cuivre (toutes, oxydées ou cirées), une balise en bas au milieu. */
    private void recette() {
        ShapedRecipe r = new ShapedRecipe(cleRecette, creerAmulette());
        r.shape("CCC", "C C", "CBC");
        r.setIngredient('C', new RecipeChoice.MaterialChoice(Material.COPPER_CHAIN, Material.EXPOSED_COPPER_CHAIN,
                Material.WEATHERED_COPPER_CHAIN, Material.OXIDIZED_COPPER_CHAIN, Material.WAXED_COPPER_CHAIN,
                Material.WAXED_EXPOSED_COPPER_CHAIN, Material.WAXED_WEATHERED_COPPER_CHAIN,
                Material.WAXED_OXIDIZED_COPPER_CHAIN));
        r.setIngredient('B', Material.BEACON);
        Bukkit.addRecipe(r);
    }

    /** La recette apparaît dans le livre de recettes quand le joueur a une balise. */
    private void debloquer(Player joueur, ItemStack obtenu) {
        if (obtenu != null && obtenu.getType() == Material.BEACON && !joueur.hasDiscoveredRecipe(cleRecette)) {
            joueur.discoverRecipe(cleRecette);
        }
    }

    private void livreDeRecettes(Player joueur) {
        for (ItemStack item : joueur.getInventory().getContents()) {
            debloquer(joueur, item);
        }
    }

    @EventHandler
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        debloquer(event.getPlayer(), event.getNewItemStack());
    }

    /** L'amulette ne s'utilise pas comme un livre de connaissances (il serait consommé). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUtiliser(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && estAmulette(event.getItem())) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    // ------------------------------------------------------------------ compteur

    private Compteur compteur(Player joueur) {
        return compteurs.computeIfAbsent(CompteTest.de(joueur.getUniqueId()), u -> new Compteur());
    }

    private int jour(Compteur c) {
        return (int) (c.secondes / jourSecondes);
    }

    private static boolean dansLeNether(Player joueur) {
        return joueur.getWorld().getEnvironment() == World.Environment.NETHER;
    }

    private static boolean compte(Player joueur) {
        return joueur.getGameMode() == GameMode.SURVIVAL || joueur.getGameMode() == GameMode.ADVENTURE;
    }

    /** /effects : sans limite dans le Nether ; dehors, temps avant de repasser sous le jour 1 ; 0 sans maléfice. */
    private int restantEffet(Player joueur) {
        Compteur c = compteurs.get(CompteTest.de(joueur.getUniqueId()));
        if (c == null || jour(c) < 1) {
            return 0;
        }
        if (dansLeNether(joueur) || descente == 0) {
            return -1;
        }
        return (int) Math.max(1, (c.secondes - jourSecondes + descente) / descente);
    }

    /** Chaque seconde : compteur, maléfices, barre de boss. */
    private void seconde() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            Compteur c = compteur(joueur);
            if (joueur.isDead()) {
                cacher(joueur);
                continue;
            }
            if (!dansLeNether(joueur)) {
                if (c.secondes > 0) {
                    c.secondes = Math.max(0, c.secondes - descente);
                    modifie = true;
                }
                if (jour(c) != c.jourTire) {
                    c.jourTire = -1;
                    c.activations.clear();
                }
                cacher(joueur);
                continue;
            }
            if (!compte(joueur)) {
                cacher(joueur);
                continue;
            }
            int avant = jour(c);
            c.secondes++;
            modifie = true;
            int jour = jour(c);
            if (jour != c.jourTire) {
                tirer(c, jour);
                if (jour > avant) {
                    annoncer(joueur, jour);
                }
            }
            appliquer(joueur, c, (int) (c.secondes % jourSecondes));
        }
    }

    /** Tire les activations du jour : pour chaque effet, N minutes différentes (N = le jour, toutes au-delà). */
    private void tirer(Compteur c, int jour) {
        c.jourTire = jour;
        c.activations.clear();
        int minutes = Math.max(1, jourSecondes / 60);
        List<Integer> places = new ArrayList<>();
        for (int m = 0; m < minutes; m++) {
            places.add(m);
        }
        for (Malefices.Etat e : malefices.au(jour)) {
            Collections.shuffle(places, hasard);
            for (int k = 0; k < Math.min(jour, minutes); k++) {
                int marge = Malefices.DUREE_MAX - e.duree();
                c.activations.add(new Activation(e, places.get(k) * 60 + (marge > 0 ? hasard.nextInt(marge + 1) : 0)));
            }
        }
    }

    /** Message au début d'un nouveau jour : ce qui arrive ou monte ce jour-là. */
    private void annoncer(Player joueur, int jour) {
        List<Malefices.Etat> maintenant = malefices.au(jour);
        List<Malefices.Etat> hier = malefices.au(jour - 1);
        Component change = null;
        for (Malefices.Etat e : maintenant) {
            Malefices.Etat avant = hier.stream().filter(h -> h.index() == e.index()).findFirst().orElse(null);
            if (avant == null) {
                change = t("jour.nouveau", "<gray>Nouveau maléfice : <red><effet></red>.", "effet", Malefices.nom(e));
            } else if (e.niveau() > avant.niveau()) {
                change = t("jour.niveau", "<gray>Maléfice plus fort : <red><effet></red>.", "effet", Malefices.nom(e));
            } else if (e.duree() > avant.duree()) {
                change = t("jour.duree", "<gray><effet> dure maintenant <red><secondes> s</red>.", "effet",
                        Malefices.nom(e), "secondes", e.duree());
            }
        }
        joueur.sendMessage(t("jour.titre", "<dark_red><bold>Nether : jour <jour>.", "jour", jour));
        if (change != null) {
            joueur.sendMessage(change);
        }
    }

    /** Effets en cours à cette seconde du jour : amulette, pose (toutes les 5 s), barre de boss. */
    private void appliquer(Player joueur, Compteur c, int t) {
        boolean plastron = atouts && fr.kalium.atouts.KSAtouts.porteAtout(joueur, PLASTRON);
        List<Activation> actives = new ArrayList<>();
        for (Activation a : c.activations) {
            int duree = plastron ? Math.max(1, (a.etat.duree() + plastronDiviseur - 1) / plastronDiviseur) : a.etat.duree();
            if (t >= a.debut && t < a.debut + duree) {
                a.duree = duree;
                actives.add(a);
            }
        }
        Set<Activation> bloquees = new HashSet<>();
        if (!actives.isEmpty() && amulettePoints > 0 && KSJetons.porteEquipement(joueur.getUniqueId(), EQUIPEMENT)) {
            // Du plus dangereux (fin de la liste) au moins dangereux, tant que le joueur a assez d'XP.
            List<Activation> ordre = new ArrayList<>(actives);
            ordre.sort(Comparator.comparingInt((Activation a) -> a.etat.index()).reversed());
            int xp = joueur.calculateTotalExperiencePoints();
            int paye = 0;
            for (Activation a : ordre) {
                int cout = amulettePoints * Malefices.niveauPaye(a.etat);
                if (xp - paye >= cout) {
                    paye += cout;
                    bloquees.add(a);
                }
            }
            if (paye > 0) {
                joueur.setExperienceLevelAndProgress(xp - paye);
            }
        }
        List<Activation> visibles = new ArrayList<>();
        for (Activation a : actives) {
            PotionEffectType type = a.etat.effet().type();
            if (bloquees.contains(a)) {
                retirerNotre(joueur, type);
                a.posee = Integer.MIN_VALUE / 2;
                continue;
            }
            visibles.add(a);
            if (a.posee < a.debut || t - a.posee >= rafraichissement) {
                int reste = a.debut + a.duree - t;
                int ticks = Math.min(rafraichissement + 1, reste) * 20;
                joueur.addPotionEffect(new PotionEffect(type, ticks, a.etat.niveau() - 1, false, false, true));
                a.posee = t;
            }
        }
        barre(joueur, jour(c), visibles, t);
    }

    /** Retire l'effet s'il vient d'ici (sans particules, avec icône, court) : un autre effet du même type reste. */
    private void retirerNotre(Player joueur, PotionEffectType type) {
        PotionEffect actuel = joueur.getPotionEffect(type);
        if (actuel != null && !actuel.hasParticles() && actuel.hasIcon() && !actuel.isAmbient()
                && actuel.getDuration() <= (rafraichissement + 1) * 20) {
            joueur.removePotionEffect(type);
        }
    }

    // ------------------------------------------------------------------ barre de boss

    private void barre(Player joueur, int jour, List<Activation> visibles, int t) {
        if (visibles.isEmpty()) {
            cacher(joueur);
            return;
        }
        visibles.sort(Comparator.comparingInt(a -> a.debut));
        Activation premier = visibles.get(0);
        float progression = Math.max(0f, Math.min(1f, (premier.debut + premier.duree - t) / (float) premier.duree));
        String effets = visibles.stream().map(a -> Malefices.nom(a.etat)).distinct().collect(Collectors.joining(" + "));
        Component titre = t("barre.titre", "<red>Maléfice (jour <jour>) : <effets>", "jour", jour, "effets", effets);
        BossBar b = barres.get(joueur.getUniqueId());
        if (b == null) {
            b = BossBar.bossBar(titre, progression, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            barres.put(joueur.getUniqueId(), b);
            joueur.showBossBar(b);
        } else {
            b.name(titre);
            b.progress(progression);
        }
    }

    private void cacher(Player joueur) {
        BossBar b = barres.remove(joueur.getUniqueId());
        if (b != null) {
            joueur.hideBossBar(b);
        }
    }

    // ------------------------------------------------------------------ événements

    /** La mort remet le compteur à zéro. */
    @EventHandler
    public void onMort(PlayerDeathEvent event) {
        Compteur c = compteur(event.getPlayer());
        if (c.secondes > 0) {
            c.secondes = 0;
            modifie = true;
        }
        c.jourTire = -1;
        c.activations.clear();
        cacher(event.getPlayer());
    }

    /** En entrant dans le Nether avec un compteur déjà au jour 1 ou plus : le rappel du jour. */
    @EventHandler
    public void onMonde(PlayerChangedWorldEvent event) {
        Player joueur = event.getPlayer();
        Compteur c = compteurs.get(CompteTest.de(joueur.getUniqueId()));
        if (dansLeNether(joueur) && c != null && jour(c) >= 1) {
            joueur.sendMessage(t("jour.reprise", "<dark_red>Nether : tu reprends au jour <jour>.", "jour", jour(c)));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        livreDeRecettes(event.getPlayer());
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        cacher(event.getPlayer());
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection joueurs = yaml.getConfigurationSection("joueurs");
        if (joueurs == null) {
            return;
        }
        for (String cle : joueurs.getKeys(false)) {
            try {
                Compteur c = new Compteur();
                c.secondes = Math.max(0, joueurs.getLong(cle + ".secondes"));
                compteurs.put(UUID.fromString(cle), c);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Compteur ignoré dans donnees.yml : " + cle);
            }
        }
    }

    private void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        compteurs.forEach((u, c) -> {
            if (c.secondes > 0) {
                yaml.set("joueurs." + u + ".secondes", c.secondes);
            }
        });
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
            modifie = false;
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer donnees.yml : " + e.getMessage());
        }
    }
}
