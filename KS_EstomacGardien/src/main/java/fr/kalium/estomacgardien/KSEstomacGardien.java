package fr.kalium.estomacgardien;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KS_EstomacGardien (demande de Maxster33, 28/09/2026) : objet « Estomac du gardien », avec l'image du sac noir.
 *
 * 2.0.0 (LeKiwi06, 26/10/2026 : « il marchera comme un coffre de récompense avec une interface de coffre malgré que ce
 * soit un bundle , avec une mention non ouvert ou ouvert ( mais on ne peux pas le fusionner dans l'enclume ) ») :
 * clic droit = fenêtre de coffre. « Non ouvert » : le contenu est tiré à la première ouverture ; ensuite « Ouvert (reste
 * n piles) », le reste est gardé dans l'objet et il se rouvre ; vidé, il disparaît. Un clic sur un objet de la fenêtre
 * le prend ; on ne peut rien y déposer. Un estomac ouvert perd la marque « estomac » (les rachats de KS_Economy ne
 * reconnaissent que les estomacs non ouverts) et porte « estomac_ouvert ». Refusé à l'enclume (ni renommé, ni fusionné).
 * Avant : clic droit = l'objet était consommé et donnait son contenu directement.
 *
 * L'objet de base est un livre de connaissances (ni sac, ni bloc, ni ingrédient de craft, pile de 1) dont le clic droit
 * vanilla est annulé ; seule l'image (item_model) est celle du sac noir.
 * Autres plugins : creerEstomac() (KS_LootEntites, KS_KaliumGive).
 */
public final class KSEstomacGardien extends JavaPlugin implements Listener {

    private static final List<Material> CORAUX = List.of(
            Material.TUBE_CORAL, Material.BRAIN_CORAL, Material.BUBBLE_CORAL, Material.FIRE_CORAL, Material.HORN_CORAL,
            Material.TUBE_CORAL_FAN, Material.BRAIN_CORAL_FAN, Material.BUBBLE_CORAL_FAN, Material.FIRE_CORAL_FAN,
            Material.HORN_CORAL_FAN,
            Material.TUBE_CORAL_BLOCK, Material.BRAIN_CORAL_BLOCK, Material.BUBBLE_CORAL_BLOCK,
            Material.FIRE_CORAL_BLOCK, Material.HORN_CORAL_BLOCK);

    private static final List<Material> ALGUES = List.of(Material.KELP, Material.SEAGRASS);

    /** Tous les bateaux et radeaux, avec ou sans coffre. */
    private static final List<Material> BATEAUX = Arrays.stream(Material.values())
            .filter(m -> !m.isLegacy() && m.isItem())
            .filter(m -> m.name().endsWith("_BOAT") || m.name().endsWith("_RAFT"))
            .toList();

    /** 2.0.0 : cases de la fenêtre (le contenu tient toujours dans une rangée de coffre de 27 cases). */
    private static final int TAILLE = 27;

    private static NamespacedKey cle;
    /** 2.0.0 : estomac ouvert (contenu restant, objets sérialisés) ; il n'a plus la marque « estomac ». */
    private static NamespacedKey cleOuvert;
    /** 2.0.0 : identifiant de l'estomac pendant que sa fenêtre est ouverte (retrouver le bon objet à la fermeture). */
    private static NamespacedKey cleVue;

    @Override
    public void onEnable() {
        cle = new NamespacedKey(this, "estomac");
        cleOuvert = new NamespacedKey(this, "estomac_ouvert");
        cleVue = new NamespacedKey(this, "vue");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Vue) {
                player.closeInventory();
            }
        }
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    /** Entier au hasard entre min et max compris. */
    private static int entre(int min, int max) {
        return random().nextInt(min, max + 1);
    }

    // ------------------------------------------------------------------ objet

    /** Un Estomac du gardien (non ouvert). Nécessite que le plugin soit activé. */
    public static ItemStack creerEstomac() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Estomac du gardien", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft("black_bundle"));
        meta.lore(List.of(ligne("Non ouvert", NamedTextColor.GREEN)));
        meta.getPersistentDataContainer().set(cle, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private static Component ligne(String texte, NamedTextColor couleur) {
        return Component.text(texte, couleur).decoration(TextDecoration.ITALIC, false);
    }

    /** Estomac du gardien, ouvert ou non. */
    public static boolean estEstomac(ItemStack item) {
        return estNonOuvert(item) || estOuvert(item);
    }

    /** 2.0.0 : estomac jamais ouvert (le seul que les rachats reconnaissent). */
    public static boolean estNonOuvert(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cle, PersistentDataType.BYTE);
    }

    private static boolean estOuvert(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(cleOuvert, PersistentDataType.BYTE_ARRAY);
    }

    /** 2.0.0 : estomac donné avant la 2.0.0 (sans ligne « Non ouvert ») : la ligne est ajoutée. */
    @EventHandler
    public void onSlot(PlayerInventorySlotChangeEvent event) {
        ItemStack item = event.getNewItemStack();
        if (estNonOuvert(item) && !item.getItemMeta().hasLore()) {
            ItemMeta meta = item.getItemMeta();
            meta.lore(List.of(ligne("Non ouvert", NamedTextColor.GREEN)));
            item.setItemMeta(meta);
            event.getPlayer().getInventory().setItem(event.getSlot(), item);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null || !estEstomac(event.getItem())) {
            return;
        }
        // Annule l'effet vanilla du livre de connaissances.
        event.setCancelled(true);
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> ouvrir(player, hand));
    }

    // ------------------------------------------------------------------ 2.0.0 : fenêtre de coffre

    /** Fenêtre ouverte : main qui tient l'estomac et identifiant de l'estomac. */
    private static final class Vue implements InventoryHolder {
        private final EquipmentSlot main;
        private final String id;
        private Inventory inventaire;

        private Vue(EquipmentSlot main, String id) {
            this.main = main;
            this.id = id;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    private void ouvrir(Player player, EquipmentSlot main) {
        ItemStack estomac = player.getInventory().getItem(main);
        if (!player.isOnline() || !estEstomac(estomac) || estomac.getAmount() != 1
                || player.getOpenInventory().getTopInventory().getHolder() instanceof Vue) {
            return;
        }
        List<ItemStack> objets = estOuvert(estomac) ? lire(estomac) : contenu();
        String id = UUID.randomUUID().toString();
        // Ouvert dès maintenant (contenu tiré écrit dans l'objet) : il ne peut plus être revendu comme non ouvert.
        ecrire(estomac, objets, id);
        player.getInventory().setItem(main, estomac);
        Vue vue = new Vue(main, id);
        Inventory inventaire = Bukkit.createInventory(vue, TAILLE, Component.text("Estomac du gardien"));
        vue.inventaire = inventaire;
        for (int i = 0; i < objets.size() && i < TAILLE; i++) {
            inventaire.setItem(i, objets.get(i));
        }
        player.openInventory(inventaire);
    }

    /** Contenu restant d'un estomac ouvert. */
    private static List<ItemStack> lire(ItemStack estomac) {
        byte[] octets = estomac.getItemMeta().getPersistentDataContainer().get(cleOuvert, PersistentDataType.BYTE_ARRAY);
        List<ItemStack> objets = new ArrayList<>();
        if (octets != null) {
            for (ItemStack objet : ItemStack.deserializeItemsFromBytes(octets)) {
                if (objet != null && !objet.isEmpty()) {
                    objets.add(objet);
                }
            }
        }
        return objets;
    }

    /** L'estomac devient ouvert avec ce contenu (id : fenêtre ouverte ; null : fenêtre fermée). */
    private static void ecrire(ItemStack estomac, List<ItemStack> objets, String id) {
        ItemMeta meta = estomac.getItemMeta();
        PersistentDataContainer donnees = meta.getPersistentDataContainer();
        donnees.remove(cle);
        donnees.set(cleOuvert, PersistentDataType.BYTE_ARRAY,
                ItemStack.serializeItemsAsBytes(objets.toArray(new ItemStack[0])));
        if (id == null) {
            donnees.remove(cleVue);
        } else {
            donnees.set(cleVue, PersistentDataType.STRING, id);
        }
        meta.lore(List.of(ligne("Ouvert (reste " + objets.size() + (objets.size() > 1 ? " piles)" : " pile)"),
                NamedTextColor.GRAY)));
        estomac.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClic(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Vue vue)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Rien ne bouge par le jeu (ni dépôt dans la fenêtre, ni déplacement de l'estomac) : seul un clic sur un objet
        // de la fenêtre le prend.
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= TAILLE || event.getClick().isKeyboardClick()) {
            return;
        }
        ItemStack objet = vue.inventaire.getItem(slot);
        if (objet == null || objet.isEmpty()) {
            return;
        }
        // L'estomac (pile de 1) ne reçoit jamais d'objet : addItem ne touche pas sa case.
        Map<Integer, ItemStack> reste = player.getInventory().addItem(objet.clone());
        vue.inventaire.setItem(slot, reste.isEmpty() ? null : reste.get(0));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGlisser(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Vue) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFermer(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Vue vue) || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        List<ItemStack> reste = new ArrayList<>();
        for (ItemStack objet : vue.inventaire.getContents()) {
            if (objet != null && !objet.isEmpty()) {
                reste.add(objet);
            }
        }
        ItemStack estomac = player.getInventory().getItem(vue.main);
        if (!estOuvert(estomac)
                || !vue.id.equals(estomac.getItemMeta().getPersistentDataContainer().get(cleVue, PersistentDataType.STRING))) {
            // Estomac introuvable (rien ne peut le déplacer pendant que la fenêtre est ouverte) : reste rendu au joueur.
            for (ItemStack objet : player.getInventory().addItem(reste.toArray(new ItemStack[0])).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), objet);
            }
            return;
        }
        if (reste.isEmpty()) {
            player.getInventory().setItem(vue.main, null);
        } else {
            ecrire(estomac, reste, null);
            player.getInventory().setItem(vue.main, estomac);
        }
    }

    /** 2.0.0 : un estomac ne passe pas à l'enclume (ni renommage, ni fusion), ouvert ou non. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnclume(PrepareAnvilEvent event) {
        if (estEstomac(event.getInventory().getFirstItem()) || estEstomac(event.getInventory().getSecondItem())) {
            event.setResult(null);
        }
    }

    // ------------------------------------------------------------------ contenu

    /**
     * Tirage 1 (1 tirage) : 20 % casque en diamant enchanté niveau 30 à 50.
     * Tirage 2 (4 tirages, poids) : oeuf de tortue 3, bateau 3, corail x2-3 10, algue x2-3 10, trident enchanté niveau
     * 10 à 29 3, armure de nautile en diamant 1, coeur de la mer 1.
     * Tirage 3 (3 tirages, poids) : voir tirage3.
     * 2.0.0 (LeKiwi06 : « il faut qu'il contiennent + d'items », proposition validée) : tirage 2 fait 4 fois (avant : 2),
     * nouveau tirage 3 de butin marin.
     */
    static List<ItemStack> contenu() {
        List<ItemStack> items = new ArrayList<>();
        if (random().nextDouble() < 0.20) {
            items.add(enchanter(new ItemStack(Material.DIAMOND_HELMET), entre(30, 50)));
        }
        for (int i = 0; i < 4; i++) {
            items.add(tirage2());
        }
        for (int i = 0; i < 3; i++) {
            items.add(tirage3());
        }
        return items;
    }

    private static final List<Material> POISSONS = List.of(Material.COD, Material.SALMON, Material.TROPICAL_FISH,
            Material.PUFFERFISH);

    /**
     * 2.0.0 : butin marin (poids sur 54) : éclats de prismarine x4-8 10, cristaux de prismarine x2-5 10, poisson cru au
     * hasard (morue, saumon, poisson tropical, poisson-globe) x2-5 10, concombres de mer x2-4 6, sacs d'encre luisante
     * x2-4 6, lanternes aquatiques x2-4 5, carapace de nautile 3, écaille de tortue x1-2 3, éponge 1.
     */
    private static ItemStack tirage3() {
        int roll = random().nextInt(54);
        if ((roll -= 10) < 0) {
            return new ItemStack(Material.PRISMARINE_SHARD, entre(4, 8));
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(Material.PRISMARINE_CRYSTALS, entre(2, 5));
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(POISSONS.get(random().nextInt(POISSONS.size())), entre(2, 5));
        }
        if ((roll -= 6) < 0) {
            return new ItemStack(Material.SEA_PICKLE, entre(2, 4));
        }
        if ((roll -= 6) < 0) {
            return new ItemStack(Material.GLOW_INK_SAC, entre(2, 4));
        }
        if ((roll -= 5) < 0) {
            return new ItemStack(Material.SEA_LANTERN, entre(2, 4));
        }
        if ((roll -= 3) < 0) {
            return new ItemStack(Material.NAUTILUS_SHELL);
        }
        if ((roll -= 3) < 0) {
            return new ItemStack(Material.TURTLE_SCUTE, entre(1, 2));
        }
        return new ItemStack(Material.SPONGE);
    }

    private static ItemStack tirage2() {
        int roll = random().nextInt(31);
        if ((roll -= 3) < 0) {
            return new ItemStack(Material.TURTLE_EGG);
        }
        if ((roll -= 3) < 0) {
            return new ItemStack(BATEAUX.get(random().nextInt(BATEAUX.size())));
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(CORAUX.get(random().nextInt(CORAUX.size())), entre(2, 3));
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(ALGUES.get(random().nextInt(ALGUES.size())), entre(2, 3));
        }
        if ((roll -= 3) < 0) {
            return enchanter(new ItemStack(Material.TRIDENT), entre(10, 29));
        }
        if ((roll -= 1) < 0) {
            return new ItemStack(Material.DIAMOND_NAUTILUS_ARMOR);
        }
        return new ItemStack(Material.HEART_OF_THE_SEA);
    }

    /** Comme la fonction vanilla enchant_with_levels (enchantements « #on_random_loot »). */
    private static ItemStack enchanter(ItemStack item, int niveaux) {
        return Bukkit.getItemFactory().enchantWithLevels(item, niveaux,
                RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                        .getTag(EnchantmentTagKeys.ON_RANDOM_LOOT),
                random());
    }
}
