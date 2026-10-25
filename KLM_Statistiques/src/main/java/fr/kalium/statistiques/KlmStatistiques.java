package fr.kalium.statistiques;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * KLM_Statistiques - demande de LeKiwi06, 23/10/2026 (avec le parrainage) : « stats de temps de jeu visibles pour les
 * joueurs Bedrock » ; puis « toutes les stats vanilla, dans l'onglet Information, rajouter une catégorie Statistiques
 * [...] mettre aussi cette interface pour les joueurs Java ». Maquette validée le même jour.
 *
 * Les joueurs Bedrock n'ont pas l'écran Statistiques de Java. Le serveur compte pourtant leurs statistiques comme pour
 * tout joueur (world/stats/&lt;uuid&gt;.json, depuis la création du monde) : ce menu les lit telles quelles, rien à
 * reconstituer. Ce sont les statistiques de CE serveur (chaque serveur a les siennes).
 *
 * Menu en coffre (Contenant de KLM_Menu), bouton « Statistiques » du comparateur « Informations », /statistiques :
 * - rangée du haut : les onglets de l'écran de Java (Général ; objets minés, fabriqués, utilisés, cassés, ramassés,
 *   jetés ; créatures tuées, tué par) ;
 * - au milieu : l'onglet choisi, une case par statistique non nulle (36 par page), du plus grand au plus petit (ou par
 *   ordre alphabétique : bouton « Trier ») ; Général commence par le temps de jeu ;
 * - noms des statistiques, objets et créatures : clés de traduction du jeu (le jeu du joueur les affiche dans sa langue ;
 *   Geyser les traduit pour Bedrock).
 * 1.1.0 (LeKiwi06, 23/10/2026 : « j'aimerais que les heures AFK ne comptent pas (considérer un joueur comme AFK s'il
 * n'effectue pas d'actions pendant plus de 5 minutes et enlever l'AFK quand une action est effectuée) », puis « aussi
 * dans Statistiques ») : « Temps de jeu actif » en tête de l'onglet Général, compté par ce plugin depuis son
 * installation (le temps vanilla, AFK compris, reste affiché juste après). Action : tourner la tête, parler, lancer une
 * commande, casser, poser ou utiliser un bloc (même règle que KS_Parrainage). Données : temps-actif.yml.
 */
public final class KlmStatistiques extends JavaPlugin implements org.bukkit.event.Listener {

    /** Statistiques par page (4 rangées entre les onglets et la barre d'actions). */
    private static final int PAR_PAGE = 36;
    private static final String MARQUE = "klm_statistiques";

    /** Un onglet : statistique par objet ou créature, ou null pour « Général ». */
    private record Onglet(String id, Statistic statistique, Material icone, String nom) {
    }

    private static final List<Onglet> ONGLETS = List.of(
            new Onglet("general", null, Material.CLOCK, "<gold>Général"),
            new Onglet("mines", Statistic.MINE_BLOCK, Material.IRON_PICKAXE, "<aqua>Minés"),
            new Onglet("fabriques", Statistic.CRAFT_ITEM, Material.CRAFTING_TABLE, "<aqua>Fabriqués"),
            new Onglet("utilises", Statistic.USE_ITEM, Material.FLINT_AND_STEEL, "<aqua>Utilisés"),
            new Onglet("casses", Statistic.BREAK_ITEM, Material.DAMAGED_ANVIL, "<aqua>Cassés"),
            new Onglet("ramasses", Statistic.PICKUP, Material.HOPPER, "<aqua>Ramassés"),
            new Onglet("jetes", Statistic.DROP, Material.DROPPER, "<aqua>Jetés"),
            new Onglet("tues", Statistic.KILL_ENTITY, Material.IRON_SWORD, "<red>Créatures tuées"),
            new Onglet("tue-par", Statistic.ENTITY_KILLED_BY, Material.SKELETON_SKULL, "<red>Tué par"));

    /**
     * 1.1.1 - statistiques générales : nom dans le jeu (clé de traduction « stat.minecraft.<nom> », différent du nom de
     * Bukkit pour une vingtaine d'entre elles, ex. PLAY_ONE_MINUTE = play_time, CHEST_OPENED = open_chest : relevé dans
     * le fichier de langue du client 26.3) et icône. En 1.1.0, le nom de Bukkit était utilisé : noms non traduits et temps
     * de jeu affiché en ticks (signalé par LeKiwi06, 23/10/2026). « sleep_in_straw_bed » n'existe pas dans Bukkit.
     */
    private record Generale(String nom, Material icone) {
    }

    private static final Map<Statistic, Generale> GENERALES = Map.ofEntries(
            Map.entry(Statistic.ANIMALS_BRED, new Generale("animals_bred", Material.WHEAT)),
            Map.entry(Statistic.AVIATE_ONE_CM, new Generale("aviate_one_cm", Material.ELYTRA)),
            Map.entry(Statistic.BELL_RING, new Generale("bell_ring", Material.BELL)),
            Map.entry(Statistic.BOAT_ONE_CM, new Generale("boat_one_cm", Material.OAK_BOAT)),
            Map.entry(Statistic.ARMOR_CLEANED, new Generale("clean_armor", Material.LEATHER_CHESTPLATE)),
            Map.entry(Statistic.BANNER_CLEANED, new Generale("clean_banner", Material.WHITE_BANNER)),
            Map.entry(Statistic.CLEAN_SHULKER_BOX, new Generale("clean_shulker_box", Material.SHULKER_BOX)),
            Map.entry(Statistic.CLIMB_ONE_CM, new Generale("climb_one_cm", Material.LADDER)),
            Map.entry(Statistic.CROUCH_ONE_CM, new Generale("crouch_one_cm", Material.LEATHER_LEGGINGS)),
            Map.entry(Statistic.DAMAGE_ABSORBED, new Generale("damage_absorbed", Material.GOLDEN_APPLE)),
            Map.entry(Statistic.DAMAGE_BLOCKED_BY_SHIELD, new Generale("damage_blocked_by_shield", Material.SHIELD)),
            Map.entry(Statistic.DAMAGE_DEALT, new Generale("damage_dealt", Material.IRON_SWORD)),
            Map.entry(Statistic.DAMAGE_DEALT_ABSORBED, new Generale("damage_dealt_absorbed", Material.GOLDEN_SWORD)),
            Map.entry(Statistic.DAMAGE_DEALT_RESISTED, new Generale("damage_dealt_resisted", Material.NETHERITE_SWORD)),
            Map.entry(Statistic.DAMAGE_RESISTED, new Generale("damage_resisted", Material.IRON_CHESTPLATE)),
            Map.entry(Statistic.DAMAGE_TAKEN, new Generale("damage_taken", Material.REDSTONE)),
            Map.entry(Statistic.DEATHS, new Generale("deaths", Material.SKELETON_SKULL)),
            Map.entry(Statistic.DROP_COUNT, new Generale("drop", Material.DROPPER)),
            Map.entry(Statistic.CAKE_SLICES_EATEN, new Generale("eat_cake_slice", Material.CAKE)),
            Map.entry(Statistic.ITEM_ENCHANTED, new Generale("enchant_item", Material.ENCHANTING_TABLE)),
            Map.entry(Statistic.FALL_ONE_CM, new Generale("fall_one_cm", Material.FEATHER)),
            Map.entry(Statistic.CAULDRON_FILLED, new Generale("fill_cauldron", Material.CAULDRON)),
            Map.entry(Statistic.FISH_CAUGHT, new Generale("fish_caught", Material.FISHING_ROD)),
            Map.entry(Statistic.FLY_ONE_CM, new Generale("fly_one_cm", Material.PHANTOM_MEMBRANE)),
            Map.entry(Statistic.HAPPY_GHAST_ONE_CM, new Generale("happy_ghast_one_cm", Material.GHAST_TEAR)),
            Map.entry(Statistic.HORSE_ONE_CM, new Generale("horse_one_cm", Material.SADDLE)),
            Map.entry(Statistic.DISPENSER_INSPECTED, new Generale("inspect_dispenser", Material.DISPENSER)),
            Map.entry(Statistic.DROPPER_INSPECTED, new Generale("inspect_dropper", Material.DROPPER)),
            Map.entry(Statistic.HOPPER_INSPECTED, new Generale("inspect_hopper", Material.HOPPER)),
            Map.entry(Statistic.INTERACT_WITH_ANVIL, new Generale("interact_with_anvil", Material.ANVIL)),
            Map.entry(Statistic.BEACON_INTERACTION, new Generale("interact_with_beacon", Material.BEACON)),
            Map.entry(Statistic.INTERACT_WITH_BLAST_FURNACE, new Generale("interact_with_blast_furnace", Material.BLAST_FURNACE)),
            Map.entry(Statistic.BREWINGSTAND_INTERACTION, new Generale("interact_with_brewingstand", Material.BREWING_STAND)),
            Map.entry(Statistic.INTERACT_WITH_CAMPFIRE, new Generale("interact_with_campfire", Material.CAMPFIRE)),
            Map.entry(Statistic.INTERACT_WITH_CARTOGRAPHY_TABLE, new Generale("interact_with_cartography_table", Material.CARTOGRAPHY_TABLE)),
            Map.entry(Statistic.CRAFTING_TABLE_INTERACTION, new Generale("interact_with_crafting_table", Material.CRAFTING_TABLE)),
            Map.entry(Statistic.FURNACE_INTERACTION, new Generale("interact_with_furnace", Material.FURNACE)),
            Map.entry(Statistic.INTERACT_WITH_GRINDSTONE, new Generale("interact_with_grindstone", Material.GRINDSTONE)),
            Map.entry(Statistic.INTERACT_WITH_LECTERN, new Generale("interact_with_lectern", Material.LECTERN)),
            Map.entry(Statistic.INTERACT_WITH_LOOM, new Generale("interact_with_loom", Material.LOOM)),
            Map.entry(Statistic.INTERACT_WITH_SMITHING_TABLE, new Generale("interact_with_smithing_table", Material.SMITHING_TABLE)),
            Map.entry(Statistic.INTERACT_WITH_SMOKER, new Generale("interact_with_smoker", Material.SMOKER)),
            Map.entry(Statistic.INTERACT_WITH_STONECUTTER, new Generale("interact_with_stonecutter", Material.STONECUTTER)),
            Map.entry(Statistic.JUMP, new Generale("jump", Material.RABBIT_FOOT)),
            Map.entry(Statistic.LEAVE_GAME, new Generale("leave_game", Material.OAK_DOOR)),
            Map.entry(Statistic.MINECART_ONE_CM, new Generale("minecart_one_cm", Material.MINECART)),
            Map.entry(Statistic.MOB_KILLS, new Generale("mob_kills", Material.ZOMBIE_HEAD)),
            Map.entry(Statistic.NAUTILUS_ONE_CM, new Generale("nautilus_one_cm", Material.NAUTILUS_SHELL)),
            Map.entry(Statistic.OPEN_BARREL, new Generale("open_barrel", Material.BARREL)),
            Map.entry(Statistic.CHEST_OPENED, new Generale("open_chest", Material.CHEST)),
            Map.entry(Statistic.ENDERCHEST_OPENED, new Generale("open_enderchest", Material.ENDER_CHEST)),
            Map.entry(Statistic.SHULKER_BOX_OPENED, new Generale("open_shulker_box", Material.PURPLE_SHULKER_BOX)),
            Map.entry(Statistic.PIG_ONE_CM, new Generale("pig_one_cm", Material.CARROT_ON_A_STICK)),
            Map.entry(Statistic.NOTEBLOCK_PLAYED, new Generale("play_noteblock", Material.NOTE_BLOCK)),
            Map.entry(Statistic.RECORD_PLAYED, new Generale("play_record", Material.JUKEBOX)),
            Map.entry(Statistic.PLAY_ONE_MINUTE, new Generale("play_time", Material.CLOCK)),
            Map.entry(Statistic.PLAYER_KILLS, new Generale("player_kills", Material.DIAMOND_SWORD)),
            Map.entry(Statistic.FLOWER_POTTED, new Generale("pot_flower", Material.FLOWER_POT)),
            Map.entry(Statistic.RAID_TRIGGER, new Generale("raid_trigger", Material.OMINOUS_BOTTLE)),
            Map.entry(Statistic.RAID_WIN, new Generale("raid_win", Material.TOTEM_OF_UNDYING)),
            Map.entry(Statistic.SLEEP_IN_BED, new Generale("sleep_in_bed", Material.RED_BED)),
            Map.entry(Statistic.SNEAK_TIME, new Generale("sneak_time", Material.CHAINMAIL_LEGGINGS)),
            Map.entry(Statistic.SPRINT_ONE_CM, new Generale("sprint_one_cm", Material.GOLDEN_BOOTS)),
            Map.entry(Statistic.STRIDER_ONE_CM, new Generale("strider_one_cm", Material.WARPED_FUNGUS_ON_A_STICK)),
            Map.entry(Statistic.SWIM_ONE_CM, new Generale("swim_one_cm", Material.TROPICAL_FISH)),
            Map.entry(Statistic.TALKED_TO_VILLAGER, new Generale("talked_to_villager", Material.WRITABLE_BOOK)),
            Map.entry(Statistic.TARGET_HIT, new Generale("target_hit", Material.TARGET)),
            Map.entry(Statistic.TIME_SINCE_DEATH, new Generale("time_since_death", Material.WITHER_ROSE)),
            Map.entry(Statistic.TIME_SINCE_REST, new Generale("time_since_rest", Material.WHITE_BED)),
            Map.entry(Statistic.TOTAL_WORLD_TIME, new Generale("total_world_time", Material.SUNFLOWER)),
            Map.entry(Statistic.TRADED_WITH_VILLAGER, new Generale("traded_with_villager", Material.EMERALD)),
            Map.entry(Statistic.TRAPPED_CHEST_TRIGGERED, new Generale("trigger_trapped_chest", Material.TRAPPED_CHEST)),
            Map.entry(Statistic.NOTEBLOCK_TUNED, new Generale("tune_noteblock", Material.AMETHYST_SHARD)),
            Map.entry(Statistic.CAULDRON_USED, new Generale("use_cauldron", Material.WATER_BUCKET)),
            Map.entry(Statistic.WALK_ON_WATER_ONE_CM, new Generale("walk_on_water_one_cm", Material.LILY_PAD)),
            Map.entry(Statistic.WALK_ONE_CM, new Generale("walk_one_cm", Material.LEATHER_BOOTS)),
            Map.entry(Statistic.WALK_UNDER_WATER_ONE_CM, new Generale("walk_under_water_one_cm", Material.HEART_OF_THE_SEA)));

    /** Une case : son icône, son nom, sa valeur affichée, sa valeur brute (tri). */
    private record Ligne(ItemStack icone, Component nom, String valeur, long brut, String tri) {
    }

    private Lang lang;

    /** 1.1.0 : AFK après 5 minutes sans action (LeKiwi06). */
    private static final long AFK_MS = 5 * 60_000L;
    /** Dernière action de chaque joueur connecté (millisecondes). */
    private final Map<java.util.UUID, Long> activite = new java.util.HashMap<>();
    /** Temps de jeu actif de chaque joueur, en secondes. */
    private final Map<java.util.UUID, Long> actif = new java.util.HashMap<>();
    private java.io.File fichierActif;
    /** Date du début du compte (affichée dans le menu). */
    private String depuis = "";
    private boolean aSauver;

    @Override
    public void onEnable() {
        lang = new Lang(this);
        chargerActif();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getOnlinePlayers().forEach(p -> activite.put(p.getUniqueId(), System.currentTimeMillis()));
        // Chaque minute : une minute de plus aux joueurs actifs ; sauvegarde toutes les 5 minutes.
        getServer().getScheduler().runTaskTimer(this, this::compter, 1200L, 1200L);
        getServer().getScheduler().runTaskTimer(this, this::sauverActif, 6000L, 6000L);
        Component titre = lang.c("section.titre", "<aqua><bold>Statistiques");
        Component description = lang.c("section.description", "<gray>Tes statistiques sur ce serveur : temps de jeu, "
                + "distances, blocs minés, créatures...");
        getServer().getServicesManager().register(MenuSection.class, new MenuSection() {
            @Override
            public String id() {
                return "statistiques";
            }

            @Override
            public Plugin owner() {
                return KlmStatistiques.this;
            }

            @Override
            public Component title() {
                return titre;
            }

            @Override
            public Component description() {
                return description;
            }

            @Override
            public boolean informations() {
                return true;
            }

            @Override
            public int order() {
                return 60;
            }

            @Override
            public void open(Player joueur, Consumer<Player> retour) {
                ouvrir(joueur, retour, 0, 0, false);
            }
        }, this, ServicePriority.Normal);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        aSauver = true;
        sauverActif();
    }

    // ------------------------------------------------------------------ temps de jeu actif (1.1.0)

    private void chargerActif() {
        fichierActif = new java.io.File(getDataFolder(), "temps-actif.yml");
        org.bukkit.configuration.file.YamlConfiguration yaml =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(fichierActif);
        depuis = yaml.getString("depuis", "");
        if (depuis.isEmpty()) {
            depuis = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            aSauver = true;
        }
        org.bukkit.configuration.ConfigurationSection section = yaml.getConfigurationSection("joueurs");
        if (section != null) {
            for (String cle : section.getKeys(false)) {
                try {
                    actif.put(java.util.UUID.fromString(cle), section.getLong(cle));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Joueur illisible dans temps-actif.yml : " + cle);
                }
            }
        }
    }

    private void sauverActif() {
        if (!aSauver || fichierActif == null) {
            return;
        }
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("depuis", depuis);
        actif.forEach((id, secondes) -> yaml.set("joueurs." + id, secondes));
        try {
            yaml.save(fichierActif);
            aSauver = false;
        } catch (java.io.IOException e) {
            getLogger().warning("Écriture de temps-actif.yml impossible : " + e.getMessage());
        }
    }

    private void compter() {
        long maintenant = System.currentTimeMillis();
        for (Player joueur : getServer().getOnlinePlayers()) {
            // 1.2.0 : le temps passé dans le monde de test de Kixster (KS_MondeTest, métadonnée « ks_compte ») ne compte pas.
            if (!joueur.hasMetadata("ks_compte") && maintenant - activite.getOrDefault(joueur.getUniqueId(), 0L) <= AFK_MS) {
                actif.merge(joueur.getUniqueId(), 60L, Long::sum);
                aSauver = true;
            }
        }
    }

    private void action(Player joueur) {
        activite.put(joueur.getUniqueId(), System.currentTimeMillis());
    }

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        action(event.getPlayer());
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        activite.remove(event.getPlayer().getUniqueId());
    }

    /** Tourner la tête (un bassin AFK déplace le joueur sans la faire tourner). */
    @org.bukkit.event.EventHandler(ignoreCancelled = true)
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getFrom().getYaw() != event.getTo().getYaw() || event.getFrom().getPitch() != event.getTo().getPitch()) {
            action(event.getPlayer());
        }
    }

    @org.bukkit.event.EventHandler(ignoreCancelled = true)
    public void onBreak(org.bukkit.event.block.BlockBreakEvent event) {
        action(event.getPlayer());
    }

    @org.bukkit.event.EventHandler(ignoreCancelled = true)
    public void onPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        action(event.getPlayer());
    }

    @org.bukkit.event.EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        action(event.getPlayer());
    }

    @org.bukkit.event.EventHandler
    public void onCommandeJoueur(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        action(event.getPlayer());
    }

    @SuppressWarnings("deprecation")
    @org.bukkit.event.EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        java.util.UUID id = event.getPlayer().getUniqueId();
        getServer().getScheduler().runTask(this, () -> activite.put(id, System.currentTimeMillis()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player joueur) {
            ouvrir(joueur, null, 0, 0, false);
        } else {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
        }
        return true;
    }

    private Component t(String cle, String def, Object... paires) {
        return lang.c(cle, def, paires);
    }

    // ------------------------------------------------------------------ menu

    private void ouvrir(Player joueur, Consumer<Player> retour, int onglet, int page, boolean alphabetique) {
        Onglet o = ONGLETS.get(onglet);
        Contenant menu = Contenant.pour(joueur, t("menu.titre", "<dark_gray>Statistiques · <onglet>", "onglet",
                lang.raw("onglet-court." + o.id(), o.nom().replaceAll("<[^>]+>", ""))), MARQUE);
        for (int i = 0; i < ONGLETS.size(); i++) {
            Onglet autre = ONGLETS.get(i);
            int index = i;
            ItemStack icone = Contenant.objet(autre.icone(), t("onglet." + autre.id(), autre.nom()),
                    i == onglet ? List.of(t("onglet.choisi", "<green>Onglet affiché"))
                            : List.of(t("onglet.clic", "<yellow>Clic : afficher")));
            if (i == onglet) {
                ItemMeta meta = icone.getItemMeta();
                meta.setEnchantmentGlintOverride(true);
                icone.setItemMeta(meta);
            }
            menu.poser(i, icone, i == onglet ? null : j -> ouvrir(j, retour, index, 0, alphabetique));
        }
        List<Ligne> lignes = o.statistique() == null ? general(joueur) : detail(joueur, o.statistique());
        if (alphabetique) {
            lignes.sort((a, b) -> a.tri().compareTo(b.tri()));
        } else if (o.statistique() != null) {
            lignes.sort((a, b) -> Long.compare(b.brut(), a.brut()));
        }
        int pages = Math.max(1, (lignes.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(pages - 1, page));
        List<Ligne> visibles = lignes.subList(p * PAR_PAGE, Math.min(lignes.size(), (p + 1) * PAR_PAGE));
        for (int i = 0; i < visibles.size(); i++) {
            Ligne l = visibles.get(i);
            menu.poser(9 + i, Contenant.objet(l.icone(), l.nom().colorIfAbsent(NamedTextColor.WHITE),
                    List.of(Component.text(l.valeur(), NamedTextColor.YELLOW))), null);
        }
        if (visibles.isEmpty()) {
            menu.poser(31, Contenant.objet(Material.GRAY_DYE, t("menu.vide", "<gray>Rien pour le moment"), List.of()), null);
        }
        menu.barre();
        if (p > 0) {
            menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, t("menu.precedente", "<gray>Page précédente"), List.of()),
                    j -> ouvrir(j, retour, onglet, p - 1, alphabetique));
        }
        if (p < pages - 1) {
            menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, t("menu.suivante", "<gray>Page suivante"), List.of()),
                    j -> ouvrir(j, retour, onglet, p + 1, alphabetique));
        }
        menu.poser(menu.bas(2), Contenant.objet(Material.BOOK, t("menu.aide", "<yellow>Statistiques"),
                Contenant.lignes(t("menu.aide-info", "<gray>Tes statistiques sur ce serveur depuis ta première venue, "
                        + "comme l'écran Statistiques de Java. Page <p> sur <pages>.", "p", p + 1, "pages", pages))), null);
        menu.poser(menu.bas(3), Contenant.objet(Material.HOPPER, alphabetique
                        ? t("menu.tri-alpha", "<aqua>Trier : ordre alphabétique")
                        : t("menu.tri-nombre", "<aqua>Trier : du plus grand au plus petit"),
                List.of(t("menu.tri-clic", "<yellow>Clic : changer le tri"))),
                j -> ouvrir(j, retour, onglet, 0, !alphabetique));
        menu.poser(menu.bas(4), retour == null
                ? Contenant.objet(Material.BARRIER, t("menu.fermer", "<red>Fermer"), List.of())
                : Contenant.objet(Material.OAK_DOOR, t("menu.retour", "<gray>Retour"), List.of()),
                j -> {
                    if (retour == null) {
                        Contenant.fermer(j);
                    } else {
                        Contenant.saisie(j, () -> retour.accept(j));
                    }
                });
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ lecture des statistiques

    /** Onglet Général : statistiques sans objet ni créature, non nulles ; le temps de jeu d'abord. */
    private List<Ligne> general(Player joueur) {
        List<Ligne> lignes = new ArrayList<>();
        // 1.1.0 : temps de jeu actif (sans AFK), compté par ce plugin.
        long secondesActives = actif.getOrDefault(joueur.getUniqueId(), 0L);
        lignes.add(new Ligne(new ItemStack(Material.RECOVERY_COMPASS),
                t("general.actif", "<green>Temps de jeu actif (sans AFK), depuis le <date>", "date", depuis),
                valeur("play_time", (int) Math.min(Integer.MAX_VALUE, secondesActives * 20L)), Long.MAX_VALUE, "!actif"));
        List<Statistic> ordre = new ArrayList<>();
        ordre.add(Statistic.PLAY_ONE_MINUTE);
        for (Statistic s : Statistic.values()) {
            if (s.getType() == Statistic.Type.UNTYPED && s != Statistic.PLAY_ONE_MINUTE) {
                ordre.add(s);
            }
        }
        for (Statistic s : ordre) {
            int v;
            try {
                v = joueur.getStatistic(s);
            } catch (RuntimeException e) {
                continue;
            }
            if (v == 0) {
                continue;
            }
            Generale g = GENERALES.get(s);
            String cle = g == null ? s.getKey().getKey() : g.nom();
            lignes.add(new Ligne(new ItemStack(g == null ? Material.PAPER : g.icone()),
                    Component.translatable("stat.minecraft." + cle), valeur(cle, v), v, cle));
        }
        return lignes;
    }

    /** Onglet par objet (blocs pour « Minés ») ou par créature : valeurs non nulles. */
    private List<Ligne> detail(Player joueur, Statistic s) {
        List<Ligne> lignes = new ArrayList<>();
        if (s.getType() == Statistic.Type.ENTITY) {
            for (EntityType type : EntityType.values()) {
                if (type == EntityType.UNKNOWN || !type.isAlive()) {
                    continue;
                }
                int v;
                try {
                    v = joueur.getStatistic(s, type);
                } catch (RuntimeException e) {
                    continue;
                }
                if (v > 0) {
                    lignes.add(new Ligne(new ItemStack(icone(type)), Component.translatable(type.translationKey()),
                            nombre(v), v, type.name()));
                }
            }
            return lignes;
        }
        for (Material m : Material.values()) {
            if (m.isLegacy() || m.isAir() || (s.getType() == Statistic.Type.BLOCK ? !m.isBlock() : !m.isItem())) {
                continue;
            }
            int v;
            try {
                v = joueur.getStatistic(s, m);
            } catch (RuntimeException e) {
                continue;
            }
            if (v > 0) {
                lignes.add(new Ligne(new ItemStack(m.isItem() ? m : Material.PAPER), Component.translatable(m.translationKey()),
                        nombre(v), v, m.name()));
            }
        }
        return lignes;
    }

    private static Material icone(EntityType type) {
        return switch (type) {
            case PLAYER -> Material.PLAYER_HEAD;
            case ENDER_DRAGON -> Material.DRAGON_HEAD;
            case WITHER -> Material.WITHER_SKELETON_SKULL;
            case GIANT -> Material.ZOMBIE_HEAD;
            default -> {
                Material oeuf = Material.matchMaterial(type.name() + "_SPAWN_EGG");
                yield oeuf != null && oeuf.isItem() ? oeuf : Material.NAME_TAG;
            }
        };
    }

    /** Valeur affichée d'une statistique générale : durée (ticks), distance (cm), dégâts (dixièmes), ou nombre. */
    private static String valeur(String cle, int v) {
        if (cle.endsWith("_one_cm")) {
            return v >= 100_000 ? String.format(Locale.FRANCE, "%.2f km", v / 100_000.0)
                    : String.format(Locale.FRANCE, "%.1f m", v / 100.0);
        }
        if (cle.contains("time") || cle.startsWith("time_since")) {
            long secondes = v / 20L;
            long minutes = secondes / 60;
            if (minutes < 60) {
                return minutes + " min " + String.format("%02d", secondes % 60) + " s";
            }
            long heures = minutes / 60;
            return heures >= 24 ? (heures / 24) + " j " + (heures % 24) + " h " + String.format("%02d", minutes % 60) + " min"
                    : heures + " h " + String.format("%02d", minutes % 60) + " min";
        }
        if (cle.startsWith("damage_")) {
            return String.format(Locale.FRANCE, "%.1f", v / 10.0);
        }
        return nombre(v);
    }

    private static String nombre(long v) {
        return String.format(Locale.FRANCE, "%,d", v).replace(' ', ' ').replace(' ', ' ');
    }
}
