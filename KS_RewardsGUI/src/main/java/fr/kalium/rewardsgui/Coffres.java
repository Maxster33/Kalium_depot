package fr.kalium.rewardsgui;

import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.2.0 (LeKiwi06, 09/10/2026) : « il faut qu'on récupère les récompenses dans des coffres renommés, avec un tag "non
 * ouvert" pour s'assurer qu'ils n'ont pas été lootés (on pourra vendre nos loot box comme ça) ». Ses réponses : ouverture
 * au clic droit, coffre en main (fenêtre de coffre, le tag saute dès l'ouverture) ; contenu caché ; coffres uniques, en
 * vente directe seulement.
 *
 * - Une récompense récupérée dans /rewards donne UN objet : un coffre au nom de la récompense, marqué « Non ouvert ».
 * - Le contenu n'est pas dans l'objet : il est gardé ici (plugins/KS_RewardsGUI/coffres.yml), retrouvé par l'identifiant
 *   du coffre. Personne ne peut donc le lire avant l'ouverture, et un coffre copié ne s'ouvre qu'une fois.
 * - Clic droit, coffre en main : à la première ouverture, le coffre devient « Ouvert » et une fenêtre montre les objets
 *   (1.3.0 : les points du score d'une récompense y sont mis en émeraudes). Un clic sur un objet le prend ; on ne peut rien y
 *   déposer. Un coffre vidé disparaît ; sinon il garde ce qui reste et se rouvre.
 * - Le coffre ne se pose pas, ne se renomme pas à l'enclume, ne sert ni d'ingrédient ni de combustible.
 */
final class Coffres implements Listener {

    /** Un coffre : sa récompense, et ce qu'il reste dedans une fois ouvert. */
    private static final class Coffre {
        Recompense recompense;
        boolean ouvert;
        final List<ItemStack> reste = new ArrayList<>();
    }

    /** Fenêtre d'un coffre ouvert : les « montres » premiers objets du reste y sont affichés. */
    private static final class Fenetre implements InventoryHolder {
        final String id;
        int montres;
        Inventory inventaire;

        Fenetre(String id) {
            this.id = id;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    private final KSRewardsGUI plugin;
    private final Lang lang;
    private final NamespacedKey cle;
    private final File fichier;
    private final Map<String, Coffre> coffres = new LinkedHashMap<>();
    /** Coffres dont la fenêtre est ouverte en ce moment. */
    private final Set<String> ouverts = new HashSet<>();

    Coffres(KSRewardsGUI plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.cle = new NamespacedKey(plugin, "coffre");
        this.fichier = new File(plugin.getDataFolder(), "coffres.yml");
        charger();
    }

    // ------------------------------------------------------------------ objet

    /** Crée le coffre d'une récompense (enregistré) et renvoie son objet. */
    ItemStack creer(Recompense r) {
        String id = UUID.randomUUID().toString();
        Coffre c = new Coffre();
        c.recompense = r;
        coffres.put(id, c);
        sauver();
        return objet(id, c);
    }

    private ItemStack objet(String id, Coffre c) {
        ItemStack objet = new ItemStack(Material.CHEST);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(sansItalique(lang.c("coffre.nom", "<gold>Coffre : <white><raison>", "raison", c.recompense.raison)));
        List<Component> lore = new ArrayList<>();
        lore.add(sansItalique(lang.c("coffre.origine", "<gray>Origine : <origine>", "origine", c.recompense.origine)));
        // 1.3.0 (LeKiwi06 : « il faut afficher sur le coffre de loot sa valeur moyenne ») : celle de son niveau.
        if (c.recompense.valeur > 0) {
            double v = c.recompense.valeur;
            String texte = v >= 10 || v == Math.rint(v) ? String.valueOf(Math.round(v))
                    : String.valueOf(Math.round(v * 10) / 10.0).replace('.', ',');
            lore.add(sansItalique(lang.c("coffre.valeur", "<gray>Valeur moyenne : <yellow><valeur></yellow> émeraude(s)",
                    "valeur", texte)));
        }
        lore.add(sansItalique(c.ouvert
                ? lang.c("coffre.ouvert", "<red>Ouvert <gray>(reste <nombre> pile(s))", "nombre", c.reste.size())
                : lang.c("coffre.non-ouvert", "<green>Non ouvert")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(cle, PersistentDataType.STRING, id);
        objet.setItemMeta(meta);
        return objet;
    }

    private static Component sansItalique(Component texte) {
        return texte.decoration(TextDecoration.ITALIC, false);
    }

    /** Identifiant du coffre porté par cet objet, ou null. */
    private String id(ItemStack objet) {
        if (objet == null || objet.getType() != Material.CHEST || !objet.hasItemMeta()) {
            return null;
        }
        return objet.getItemMeta().getPersistentDataContainer().get(cle, PersistentDataType.STRING);
    }

    /** Case de l'inventaire du joueur qui contient ce coffre (main secondaire comprise), ou -1. */
    private int caseDe(Player joueur, String id) {
        ItemStack[] contenu = joueur.getInventory().getContents();
        for (int i = 0; i < contenu.length; i++) {
            if (id.equals(id(contenu[i]))) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ ouverture

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        String id = event.getHand() == null ? null : id(event.getItem());
        if (id == null) {
            return;
        }
        // Le coffre ne se pose jamais : le clic droit l'ouvre.
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> ouvrir(joueur, id));
    }

    private void ouvrir(Player joueur, String id) {
        int place = caseDe(joueur, id);
        if (!joueur.isOnline() || place < 0 || ouverts.contains(id)) {
            return;
        }
        Coffre c = coffres.get(id);
        if (c == null) {
            joueur.getInventory().setItem(place, null);
            joueur.sendMessage(lang.c("coffre.vide", "<gray>Ce coffre était vide."));
            lang.saveIfNeeded();
            return;
        }
        if (!c.ouvert) {
            List<ItemStack> objets = c.recompense.objets();
            if (objets == null) {
                joueur.sendMessage(lang.c("coffre.inconnu", "<red>Ce coffre contient un objet qui n'existe pas (encore) "
                        + "sur ce serveur : préviens un administrateur. Il reste fermé."));
                lang.saveIfNeeded();
                return;
            }
            c.ouvert = true;
            c.reste.addAll(objets);
            // 1.3.0 (LeKiwi06 : « il faut que les émeraudes soient données dans le coffre, pas via un message dans le
            // tchat ») : les points d'une récompense deviennent des émeraudes dans le coffre (blocs au-delà d'une pile).
            c.reste.addAll(emeraudes(c.recompense.argent()));
            plugin.journal("OUVERTURE | " + joueur.getName() + " (" + joueur.getUniqueId() + ") | coffre " + id + " | "
                    + c.recompense.origine + " | " + c.recompense.raison + " | gagné par " + c.recompense.nom + " | "
                    + c.recompense.resume());
        }
        if (c.reste.isEmpty()) {
            coffres.remove(id);
            joueur.getInventory().setItem(place, null);
            sauver();
            lang.saveIfNeeded();
            return;
        }
        Fenetre fenetre = new Fenetre(id);
        fenetre.montres = Math.min(54, c.reste.size());
        fenetre.inventaire = Bukkit.createInventory(fenetre, (fenetre.montres + 8) / 9 * 9,
                lang.c("coffre.titre", "<dark_gray>Coffre : <raison>", "raison", c.recompense.raison));
        for (int i = 0; i < fenetre.montres; i++) {
            fenetre.inventaire.setItem(i, c.reste.get(i).clone());
        }
        joueur.getInventory().setItem(place, objet(id, c));
        sauver();
        ouverts.add(id);
        joueur.openInventory(fenetre.inventaire);
        lang.saveIfNeeded();
    }

    /** Des points en émeraudes : jusqu'à 64, des émeraudes ; au-delà, des blocs d'émeraude (9) et le reste en émeraudes. */
    private static List<ItemStack> emeraudes(long points) {
        List<ItemStack> piles = new ArrayList<>();
        long blocs = points > 64 ? points / 9 : 0;
        long unites = points > 64 ? points % 9 : Math.max(0, points);
        for (; blocs > 0; blocs -= 64) {
            piles.add(new ItemStack(Material.EMERALD_BLOCK, (int) Math.min(64, blocs)));
        }
        if (unites > 0) {
            piles.add(new ItemStack(Material.EMERALD, (int) unites));
        }
        return piles;
    }

    /** Un clic sur un objet du coffre le prend ; tout le reste est refusé (rien ne se dépose dans un coffre). */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory haut = event.getView().getTopInventory();
        if (!(haut.getHolder() instanceof Fenetre)) {
            return;
        }
        event.setCancelled(true);
        ItemStack pile = event.getCurrentItem();
        if (event.getClickedInventory() != haut || pile == null || pile.getType().isAir()
                || !(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        Map<Integer, ItemStack> refuses = joueur.getInventory().addItem(pile.clone());
        if (refuses.isEmpty()) {
            haut.setItem(event.getSlot(), null);
            return;
        }
        ItemStack refuse = refuses.values().iterator().next();
        haut.setItem(event.getSlot(), refuse);
        if (refuse.getAmount() == pile.getAmount()) {
            joueur.sendMessage(lang.c("coffre.plein", "<red>Pas assez de place dans ton inventaire."));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Fenetre) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Fenetre fenetre && event.getPlayer() instanceof Player joueur) {
            fermer(joueur, fenetre);
        }
    }

    /** Fenêtre fermée : le coffre garde ce qui n'a pas été pris ; vide, il disparaît. */
    private void fermer(Player joueur, Fenetre fenetre) {
        if (!ouverts.remove(fenetre.id)) {
            return;
        }
        Coffre c = coffres.get(fenetre.id);
        if (c == null) {
            return;
        }
        List<ItemStack> reste = new ArrayList<>();
        for (ItemStack pile : fenetre.inventaire.getContents()) {
            if (pile != null && !pile.getType().isAir()) {
                reste.add(pile);
            }
        }
        reste.addAll(c.reste.subList(Math.min(fenetre.montres, c.reste.size()), c.reste.size()));
        c.reste.clear();
        c.reste.addAll(reste);
        int place = caseDe(joueur, fenetre.id);
        if (c.reste.isEmpty()) {
            coffres.remove(fenetre.id);
        }
        if (place >= 0) {
            joueur.getInventory().setItem(place, c.reste.isEmpty() ? null : objet(fenetre.id, c));
        }
        sauver();
    }

    /** Arrêt du plugin : les fenêtres ouvertes sont fermées (leur reste est enregistré). */
    void fermerTout() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Fenetre fenetre) {
                fermer(joueur, fenetre);
                joueur.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------ protections

    @EventHandler(ignoreCancelled = true)
    public void onPose(BlockPlaceEvent event) {
        if (id(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack objet : event.getInventory().getMatrix()) {
            if (id(objet) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (event.getBlock().getState() instanceof Container conteneur) {
            for (ItemStack objet : conteneur.getInventory().getContents()) {
                if (id(objet) != null) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombustible(FurnaceBurnEvent event) {
        if (id(event.getFuel()) != null) {
            event.setCancelled(true);
        }
    }

    /** Pas de renommage à l'enclume : le nom du coffre dit ce qu'il contient. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnclume(PrepareAnvilEvent event) {
        if (id(event.getInventory().getFirstItem()) != null || id(event.getInventory().getSecondItem()) != null) {
            event.setResult(null);
        }
    }

    // ------------------------------------------------------------------ enregistrement

    private void charger() {
        ConfigurationSection section = YamlConfiguration.loadConfiguration(fichier).getConfigurationSection("coffres");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            Recompense r = Recompense.lire(section.getString(id + ".recompense", ""));
            if (r == null) {
                plugin.getLogger().warning("coffres.yml : coffre illisible ignoré : " + id);
                continue;
            }
            Coffre c = new Coffre();
            c.recompense = r;
            c.ouvert = section.getBoolean(id + ".ouvert");
            for (String donnees : section.getStringList(id + ".reste")) {
                try {
                    c.reste.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(donnees)));
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("coffres.yml : objet illisible ignoré dans le coffre " + id);
                }
            }
            coffres.put(id, c);
        }
    }

    void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        coffres.forEach((id, c) -> {
            yaml.set("coffres." + id + ".recompense", c.recompense.texte);
            yaml.set("coffres." + id + ".ouvert", c.ouvert);
            List<String> reste = new ArrayList<>();
            c.reste.forEach(objet -> reste.add(Base64.getEncoder().encodeToString(objet.serializeAsBytes())));
            yaml.set("coffres." + id + ".reste", reste);
        });
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'enregistrer coffres.yml : " + e.getMessage());
        }
    }
}
