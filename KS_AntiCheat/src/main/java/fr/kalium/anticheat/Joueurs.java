package fr.kalium.anticheat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.2.0 (LeKiwi06, 09/10/2026 : « une interface de coffre avec les têtes de tous les joueurs même hors ligne, pour ne
 * pas avoir à taper leurs pseudo ») : tous les joueurs connus du serveur (connectés, ou qui y ont une sauvegarde),
 * 45 têtes par page, les connectés d'abord puis par ordre alphabétique ; clic sur une tête = fiche du joueur (Menus :
 * inventaire, coffre de l'Ender, claims, alertes, suspension). Rien ne peut être pris ni déposé dans le menu.
 *
 * Tête d'un joueur hors ligne : l'objet ne porte que son identifiant, c'est le jeu du staff qui va chercher l'apparence
 * (un joueur Bedrock garde donc la tête par défaut).
 */
final class Joueurs implements Listener {

    private static final int PAR_PAGE = 45;
    private static final int PRECEDENTE = 45;
    private static final int RETOUR = 49;
    private static final int SUIVANTE = 53;
    private static final DateTimeFormatter VU = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Paris"));

    /** Joueur hors ligne : pseudo et dernière connexion, lus une fois dans sa sauvegarde puis gardés en mémoire. */
    private record Connu(String nom, long vu) {
    }

    /** Page affichée, gardée par l'inventaire du menu : joueur de chaque case. */
    private record Page(int numero, UUID[] cases) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final KSAntiCheat plugin;
    private final Map<UUID, Connu> connus = new HashMap<>();
    /** D'où le staff a ouvert la liste (rubrique « Modération » de /menu, ou accueil de l'anti-triche). */
    private final Map<UUID, Consumer<Player>> retours = new HashMap<>();

    Joueurs(KSAntiCheat plugin) {
        this.plugin = plugin;
    }

    private Component t(String cle, String defaut, Object... paires) {
        return plugin.lang().c(cle, defaut, paires).decoration(TextDecoration.ITALIC, false);
    }

    /** Ouvre la liste à sa première page ; retour (facultatif) : menu d'où elle a été ouverte. */
    void ouvrir(Player staff, Consumer<Player> retour) {
        if (retour == null) {
            retours.remove(staff.getUniqueId());
        } else {
            retours.put(staff.getUniqueId(), retour);
        }
        ouvrir(staff, 0);
    }

    private void ouvrir(Player staff, int numero) {
        if (!KSAntiCheat.staff(staff)) {
            return;
        }
        List<Map.Entry<UUID, Connu>> liste = liste();
        int pages = Math.max(1, (liste.size() + PAR_PAGE - 1) / PAR_PAGE);
        int page = Math.max(0, Math.min(numero, pages - 1));
        UUID[] cases = new UUID[PAR_PAGE];
        Inventory menu = Bukkit.createInventory(new Page(page, cases), 54,
                Component.text("Joueurs (" + liste.size() + ") - page " + (page + 1) + "/" + pages));
        for (int i = 0; i < PAR_PAGE && page * PAR_PAGE + i < liste.size(); i++) {
            Map.Entry<UUID, Connu> joueur = liste.get(page * PAR_PAGE + i);
            cases[i] = joueur.getKey();
            menu.setItem(i, tete(joueur.getKey(), joueur.getValue()));
        }
        if (page > 0) {
            menu.setItem(PRECEDENTE, bouton(Material.ARROW, t("tetes.precedente", "<yellow>Page précédente")));
        }
        if (retours.containsKey(staff.getUniqueId())) {
            menu.setItem(RETOUR, bouton(Material.OAK_DOOR, t("tetes.retour", "<gray>Retour")));
        }
        if (page < pages - 1) {
            menu.setItem(SUIVANTE, bouton(Material.ARROW, t("tetes.suivante", "<yellow>Page suivante")));
        }
        staff.openInventory(menu);
        plugin.lang().saveIfNeeded();
    }

    /** Tous les joueurs : les connectés (par ordre alphabétique), puis les autres (par ordre alphabétique). */
    private List<Map.Entry<UUID, Connu>> liste() {
        Map<UUID, Connu> tous = new LinkedHashMap<>();
        for (OfflinePlayer joueur : Bukkit.getOfflinePlayers()) {
            UUID uuid = joueur.getUniqueId();
            if (!joueur.isOnline()) {
                tous.put(uuid, connus.computeIfAbsent(uuid, u -> new Connu(joueur.getName(), joueur.getLastSeen())));
            }
        }
        // Un joueur connecté pour la première fois n'a pas encore de sauvegarde.
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            tous.put(joueur.getUniqueId(), new Connu(joueur.getName(), 0));
        }
        List<Map.Entry<UUID, Connu>> liste = new ArrayList<>(tous.entrySet());
        liste.sort(Comparator.comparing((Map.Entry<UUID, Connu> e) -> Bukkit.getPlayer(e.getKey()) == null)
                .thenComparing(e -> nom(e.getKey(), e.getValue()), String.CASE_INSENSITIVE_ORDER));
        return liste;
    }

    private static String nom(UUID uuid, Connu connu) {
        return connu.nom() == null ? uuid.toString().substring(0, 8) : connu.nom();
    }

    private ItemStack tete(UUID uuid, Connu connu) {
        Player enLigne = Bukkit.getPlayer(uuid);
        ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) tete.getItemMeta();
        meta.setOwningPlayer(enLigne != null ? enLigne : Bukkit.getOfflinePlayer(uuid));
        String nom = nom(uuid, connu);
        boolean suspendu = plugin.suspensions().suspendu(uuid);
        meta.displayName(suspendu ? t("tetes.nom-suspendu", "<red><nom>", "nom", nom)
                : enLigne != null ? t("tetes.nom-en-ligne", "<green><nom>", "nom", nom)
                : t("tetes.nom-hors-ligne", "<white><nom>", "nom", nom));
        List<Component> lignes = new ArrayList<>();
        if (enLigne != null) {
            lignes.add(t("tetes.en-ligne", "<green>En ligne"));
        } else if (connu.vu() > 0) {
            lignes.add(t("tetes.hors-ligne-vu", "<gray>Hors ligne, vu le <date>", "date",
                    VU.format(Instant.ofEpochMilli(connu.vu()))));
        } else {
            lignes.add(t("tetes.hors-ligne", "<gray>Hors ligne"));
        }
        long alertes = plugin.alertes().duJoueur(uuid).stream().filter(a -> a.gravite != Alertes.Gravite.ACTION).count();
        if (alertes > 0) {
            lignes.add(t("tetes.alertes", "<yellow><n> alerte(s)", "n", alertes));
        }
        if (suspendu) {
            lignes.add(t("tetes.suspendu", "<red>Suspendu"));
        }
        lignes.add(t("tetes.clic", "<dark_gray>Clic : fiche du joueur"));
        meta.lore(lignes);
        tete.setItemMeta(meta);
        return tete;
    }

    private static ItemStack bouton(Material objet, Component texte) {
        ItemStack bouton = new ItemStack(objet);
        ItemMeta meta = bouton.getItemMeta();
        meta.displayName(texte);
        bouton.setItemMeta(meta);
        return bouton;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Page page)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player staff) || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int case_ = event.getRawSlot();
        Consumer<Player> retour = retours.get(staff.getUniqueId());
        // Au tick suivant : on ne ferme ni ne remplace un inventaire pendant le clic.
        if (case_ == PRECEDENTE && page.numero() > 0) {
            plugin.getServer().getScheduler().runTask(plugin, () -> ouvrir(staff, page.numero() - 1));
        } else if (case_ == SUIVANTE && event.getCurrentItem() != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> ouvrir(staff, page.numero() + 1));
        } else if (case_ == RETOUR && retour != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                staff.closeInventory();
                retour.accept(staff);
            });
        } else if (case_ >= 0 && case_ < PAR_PAGE && page.cases()[case_] != null) {
            UUID cible = page.cases()[case_];
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                staff.closeInventory();
                plugin.menus().joueur(staff, Bukkit.getOfflinePlayer(cible), p -> ouvrir(p, page.numero()));
            });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Page) {
            event.setCancelled(true);
        }
    }

    /** Déconnexion : pseudo et heure gardés pour la liste (sans relire la sauvegarde du joueur). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        connus.put(joueur.getUniqueId(), new Connu(joueur.getName(), System.currentTimeMillis()));
        retours.remove(joueur.getUniqueId());
    }
}
