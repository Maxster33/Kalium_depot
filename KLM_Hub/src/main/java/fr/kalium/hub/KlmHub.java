package fr.kalium.hub;

import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.QrCodes;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * KLM_Hub 1.0.0 (demande de LeKiwi06, 03/10/2026) : ce qui accueille les joueurs au lobby.
 * - Barre de boss violette, titre vert pale « KaLium SMC | » suivi d'un texte defilant qui montre tour a tour le lien
 *   du Discord et celui du Twitch, avec un arret quand le lien est affiche en entier.
 * - QR codes (service QrCodes de KLM_Menu, bouton « Recherche de joueurs » de la boussole) : une carte en main
 *   secondaire pendant 30 secondes ; la barre de boss du joueur se vide pendant ce temps (« le temps restant via les
 *   hp »). La carte est verrouillee et retiree a la fin, a la deconnexion et a la mort.
 */
public final class KlmHub extends JavaPlugin implements Listener, QrCodes {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private Lang lang;
    private NamespacedKey cleQr;

    // Barre de boss : une par joueur (sa progression sert de minuteur au QR code de ce joueur).
    private boolean barrePermanente;
    private String format;
    private BossBar.Color couleur;
    private Component titre = Component.empty();
    private final Map<UUID, BossBar> barres = new HashMap<>();

    // Texte defilant : les textes sont mis bout a bout sur un ruban qui tourne en boucle derriere une fenetre.
    private String ruban = "";
    private int largeur;
    private int periode;
    private int decalage;
    private int attente;
    private int ticksParCaractere;
    private int pauseTicks;
    private boolean defile;
    private long tick;

    // QR codes : fin (horloge du serveur, en millisecondes) du QR code de chaque joueur, une carte par lien.
    private int dureeQr;
    private final Map<UUID, Long> finQr = new HashMap<>();
    private final Map<String, MapView> cartes = new HashMap<>();
    private final Map<String, Integer> numeros = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        cleQr = new NamespacedKey(this, "qr_code");
        charger();
        chargerNumeros();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getServicesManager().register(QrCodes.class, this, this, ServicePriority.Normal);
        getServer().getScheduler().runTaskTimer(this, this::chaqueTick, 1L, 1L);
        for (Player joueur : getServer().getOnlinePlayers()) {
            accueillir(joueur);
        }
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        for (Player joueur : getServer().getOnlinePlayers()) {
            purger(joueur);
            BossBar barre = barres.get(joueur.getUniqueId());
            if (barre != null) {
                joueur.hideBossBar(barre);
            }
        }
        barres.clear();
        finQr.clear();
        if (lang != null) {
            lang.saveIfNeeded();
        }
    }

    /** Toute nouvelle cle a sa valeur par defaut ici : un config.yml deja en place n'est jamais complete. */
    private void charger() {
        barrePermanente = getConfig().getBoolean("bossbar.enabled", true);
        format = getConfig().getString("bossbar.format", "<#98fb98>KaLium SMC | <texte>");
        try {
            couleur = BossBar.Color.valueOf(getConfig().getString("bossbar.color", "PURPLE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            getLogger().warning("bossbar.color inconnue : barre violette (PURPLE).");
            couleur = BossBar.Color.PURPLE;
        }
        List<String> textes = getConfig().contains("bossbar.texts") ? getConfig().getStringList("bossbar.texts")
                : List.of("discord.gg/3PsEbZPpdW", "twitch.tv/lekiwi06");
        textes = new ArrayList<>(textes);
        textes.removeIf(texte -> texte == null || texte.isBlank());
        // La fenetre fait au moins la longueur du plus long texte : chacun doit pouvoir s'afficher en entier.
        largeur = Math.max(1, getConfig().getInt("bossbar.scroll.width", 20));
        for (String texte : textes) {
            largeur = Math.max(largeur, texte.length());
        }
        int espace = Math.max(1, getConfig().getInt("bossbar.scroll.gap", 4));
        periode = largeur + espace;
        ticksParCaractere = Math.max(1, getConfig().getInt("bossbar.scroll.ticks-per-character", 3));
        pauseTicks = Math.max(0, (int) Math.round(getConfig().getDouble("bossbar.scroll.pause-seconds", 3) * 20));
        StringBuilder bande = new StringBuilder();
        for (String texte : textes) {
            bande.append(texte).append(" ".repeat(periode - texte.length()));
        }
        ruban = bande.toString();
        defile = textes.size() > 1;
        decalage = 0;
        attente = pauseTicks;
        titre = titre();
        dureeQr = Math.max(1, getConfig().getInt("qr.seconds", 30));
    }

    // ------------------------------------------------------------------ barre de boss

    /** Titre actuel : le format, avec la partie du ruban visible dans la fenetre. */
    private Component titre() {
        StringBuilder fenetre = new StringBuilder();
        for (int i = 0; i < largeur && !ruban.isEmpty(); i++) {
            fenetre.append(ruban.charAt((decalage + i) % ruban.length()));
        }
        // Sans les espaces de fin, le titre (centre par le jeu) reste bien au milieu pendant un arret.
        return mm.deserialize(format, Placeholder.unparsed("texte", fenetre.toString().stripTrailing()));
    }

    private BossBar montrerBarre(Player joueur) {
        BossBar barre = barres.get(joueur.getUniqueId());
        if (barre == null) {
            barre = BossBar.bossBar(titre, 1f, couleur, BossBar.Overlay.PROGRESS);
            barres.put(joueur.getUniqueId(), barre);
            joueur.showBossBar(barre);
        }
        return barre;
    }

    private void cacherBarre(Player joueur) {
        BossBar barre = barres.remove(joueur.getUniqueId());
        if (barre != null) {
            joueur.hideBossBar(barre);
        }
    }

    private void chaqueTick() {
        tick++;
        if (defile) {
            if (attente > 0) {
                attente--;
            } else if (tick % ticksParCaractere == 0) {
                decalage = (decalage + 1) % ruban.length();
                if (decalage % periode == 0) {
                    attente = pauseTicks; // un texte est affiche en entier : arret
                }
                titre = titre();
                for (BossBar barre : barres.values()) {
                    barre.name(titre);
                }
            }
        }
        if (finQr.isEmpty()) {
            return;
        }
        long maintenant = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Long>> it = finQr.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> entree = it.next();
            Player joueur = getServer().getPlayer(entree.getKey());
            long reste = entree.getValue() - maintenant;
            if (joueur == null) {
                it.remove();
            } else if (reste <= 0) {
                it.remove();
                finirQr(joueur);
            } else {
                BossBar barre = barres.get(joueur.getUniqueId());
                if (barre != null) {
                    barre.progress(Math.max(0f, Math.min(1f, reste / (dureeQr * 1000f))));
                }
            }
        }
    }

    // ------------------------------------------------------------------ QR codes

    @Override
    public Plugin owner() {
        return this;
    }

    @Override
    public boolean donner(Player joueur, String lien, Component nom) {
        PlayerInventory inventaire = joueur.getInventory();
        ItemStack actuel = inventaire.getItemInOffHand();
        if (!actuel.isEmpty() && !estQr(actuel)) {
            joueur.sendMessage(lang.c("qr.main-prise", "<red>Libère d'abord ta main gauche (main secondaire) pour afficher le QR code."));
            lang.saveIfNeeded();
            return false;
        }
        MapView vue;
        try {
            vue = carte(lien);
        } catch (IllegalArgumentException e) {
            getLogger().warning("QR code impossible pour « " + lien + " » : " + e.getMessage());
            joueur.sendMessage(lang.c("qr.erreur", "<red>Ce QR code n'est pas disponible."));
            lang.saveIfNeeded();
            return false;
        }
        ItemStack carte = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) carte.getItemMeta();
        meta.setMapView(vue);
        meta.displayName(nom.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(List.of(lang.c("qr.description", "<gray>Scanne-le avec ton téléphone.")
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        meta.getPersistentDataContainer().set(cleQr, PersistentDataType.BYTE, (byte) 1);
        carte.setItemMeta(meta);
        inventaire.setItemInOffHand(carte);
        finQr.put(joueur.getUniqueId(), System.currentTimeMillis() + dureeQr * 1000L);
        montrerBarre(joueur).progress(1f);
        joueur.sendMessage(lang.c("qr.donne", "<green>QR code dans ta main gauche pendant <white><secondes> s</white>. <gray>Lien : <white><lien>",
                "secondes", dureeQr,
                "lien", Component.text(lien).decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl(lien))));
        lang.saveIfNeeded();
        return true;
    }

    /** Fin des 30 secondes : la carte est retiree, la barre se remplit (ou disparait si elle n'est pas permanente). */
    private void finirQr(Player joueur) {
        purger(joueur);
        if (barrePermanente) {
            BossBar barre = barres.get(joueur.getUniqueId());
            if (barre != null) {
                barre.progress(1f);
            }
        } else {
            cacherBarre(joueur);
        }
    }

    private boolean estQr(ItemStack objet) {
        return objet != null && !objet.isEmpty() && objet.hasItemMeta()
                && objet.getItemMeta().getPersistentDataContainer().has(cleQr, PersistentDataType.BYTE);
    }

    /** Retire toute carte de QR code de l'inventaire du joueur (main secondaire, curseur, reste d'un plantage). */
    private void purger(Player joueur) {
        PlayerInventory inventaire = joueur.getInventory();
        for (int i = 0; i < inventaire.getSize(); i++) {
            if (estQr(inventaire.getItem(i))) {
                inventaire.setItem(i, null);
            }
        }
        if (estQr(joueur.getItemOnCursor())) {
            joueur.setItemOnCursor(null);
        }
    }

    /**
     * Carte du QR code de ce lien. Son numero est garde dans cartes.yml : la meme carte du monde ressert apres un
     * redemarrage, au lieu d'en creer une nouvelle a chaque fois.
     */
    private MapView carte(String lien) {
        MapView vue = cartes.get(lien);
        if (vue != null) {
            return vue;
        }
        QrCode qr = QrCode.de(lien);
        Integer numero = numeros.get(lien);
        vue = numero == null ? null : Bukkit.getMap(numero);
        if (vue == null) {
            vue = Bukkit.createMap(getServer().getWorlds().get(0));
            numeros.put(lien, vue.getId());
            sauverNumeros();
        }
        for (MapRenderer rendu : vue.getRenderers()) {
            vue.removeRenderer(rendu);
        }
        vue.setTrackingPosition(false);
        vue.setUnlimitedTracking(false);
        vue.setLocked(true);
        vue.addRenderer(new Rendu(qr));
        cartes.put(lien, vue);
        return vue;
    }

    /** Dessine le QR code en noir sur blanc, centre, avec les plus gros modules qui tiennent sur 128 pixels. */
    private static final class Rendu extends MapRenderer {

        private final QrCode qr;
        private boolean dessine;

        Rendu(QrCode qr) {
            this.qr = qr;
        }

        @Override
        public void render(MapView vue, MapCanvas toile, Player joueur) {
            if (dessine) {
                return;
            }
            dessine = true;
            int modules = qr.taille();
            // 2 modules de marge blanche au moins de chaque cote (le bord clair de la carte fait le reste).
            int pas = Math.max(1, 128 / (modules + 4));
            int origine = (128 - modules * pas) / 2;
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    int mx = Math.floorDiv(x - origine, pas);
                    int my = Math.floorDiv(y - origine, pas);
                    boolean sombre = mx >= 0 && mx < modules && my >= 0 && my < modules && qr.sombre(mx, my);
                    toile.setPixelColor(x, y, sombre ? Color.BLACK : Color.WHITE);
                }
            }
        }
    }

    private File fichierNumeros() {
        return new File(getDataFolder(), "cartes.yml");
    }

    private void chargerNumeros() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichierNumeros());
        for (Map<?, ?> ligne : yaml.getMapList("cartes")) {
            if (ligne.get("lien") instanceof String lien && ligne.get("numero") instanceof Number numero) {
                numeros.put(lien, numero.intValue());
            }
        }
    }

    private void sauverNumeros() {
        List<Map<String, Object>> lignes = new ArrayList<>();
        numeros.forEach((lien, numero) -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("lien", lien);
            ligne.put("numero", numero);
            lignes.add(ligne);
        });
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("cartes", lignes);
        try {
            getDataFolder().mkdirs();
            yaml.save(fichierNumeros());
        } catch (IOException e) {
            getLogger().warning("Impossible d'enregistrer cartes.yml : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ evenements

    private void accueillir(Player joueur) {
        purger(joueur);
        if (barrePermanente) {
            montrerBarre(joueur);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        accueillir(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        finQr.remove(joueur.getUniqueId());
        purger(joueur);
        cacherBarre(joueur);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::estQr);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (estQr(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            resynchroniser(event.getPlayer());
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (estQr(event.getMainHandItem()) || estQr(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    /** Une carte en main secondaire se pose sinon dans un cadre d'un simple clic droit. */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND && estQr(event.getPlayer().getInventory().getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    /** Couvre aussi le mode creatif (InventoryCreativeEvent est un InventoryClickEvent), comme la boussole de KLM_Menu. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        boolean verrouille = estQr(event.getCurrentItem()) || estQr(event.getCursor())
                || (event.getClick() == ClickType.NUMBER_KEY && estQr(joueur.getInventory().getItem(event.getHotbarButton())))
                || (event.getClick() == ClickType.SWAP_OFFHAND && estQr(joueur.getInventory().getItemInOffHand()));
        if (!verrouille && event instanceof org.bukkit.event.inventory.InventoryCreativeEvent
                && event.getClickedInventory() == joueur.getInventory() && event.getSlot() >= 0
                && estQr(joueur.getInventory().getItem(event.getSlot()))) {
            verrouille = true;
        }
        if (verrouille) {
            event.setCancelled(true);
            resynchroniser(joueur);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player joueur && estQr(event.getOldCursor())) {
            event.setCancelled(true);
            resynchroniser(joueur);
        }
    }

    /** Apres un clic annule, renvoie l'inventaire au joueur (en creatif, son jeu croit sinon l'objet deplace). */
    private void resynchroniser(Player joueur) {
        getServer().getScheduler().runTask(this, () -> {
            if (joueur.isOnline()) {
                joueur.updateInventory();
            }
        });
    }
}
