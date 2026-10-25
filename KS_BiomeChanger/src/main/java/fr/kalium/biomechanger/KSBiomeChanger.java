package fr.kalium.biomechanger;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.BiomeTagKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_BiomeChanger (demande de Maxster33, 29/09/2026) : le Changeur de Biome.
 *
 * - Objet : livre de connaissances (ni bloc, ni ingrédient vanilla) avec l'image du cristal de l'End, nommé « Changeur de
 *   Biome », empilable par 64 ; son clic droit vanilla est annulé. 1.2.0 : UTILISATIONS utilisations par Changeur,
 *   restantes dans sa description (un Changeur sans compteur, d'avant 1.2.0, est neuf) ; seuls les Changeurs au même
 *   nombre d'utilisations s'empilent.
 * - Clic droit avec l'objet en main (overworld seulement) : menu (MenuBiome) avec les biomes de l'overworld, un bouton
 *   pour choisir la forme (sphère de 32 blocs de rayon ou cube de même volume, retenue pour chaque joueur) et un bouton
 *   « Historique ». Le choix d'un biome revérifie qu'il reste un Changeur de Biome dans l'inventaire, refuse si la zone
 *   touche une région WorldGuard (ProtectionWorldGuard), consomme une utilisation (le Changeur se casse à la
 *   dernière), change le biome autour du joueur (aucun bloc
 *   ne bouge : seul le biome change, par cellules de 4 x 4 x 4 blocs comme dans le jeu), écrit « Vous avez changé le
 *   biome pour : <biome> » et inscrit le changement dans l'historique (Historique, fichier historique.yml).
 *
 * Autres plugins : creerChangeur() (KS_Crafts, KS_KaliumGive).
 */
public final class KSBiomeChanger extends JavaPlugin implements Listener {

    /** Rayon de la sphère modifiée (blocs). */
    static final int RAYON = 32;
    /** Demi-côté du cube de même volume que la sphère : côté = rayon x racine cubique de 4 pi / 3 (51,6 blocs). */
    static final double DEMI_COTE = RAYON * Math.cbrt(4 * Math.PI / 3) / 2;
    /** Utilisations d'un Changeur de Biome neuf (1.2.0, demande de LeKiwi06). */
    static final int UTILISATIONS = 10;

    /** Forme de la zone modifiée, choisie par le joueur dans le menu. */
    enum Forme {
        SPHERE("sphère"), CUBE("cube");

        final String nom;

        Forme(String nom) {
            this.nom = nom;
        }
    }

    private static NamespacedKey marqueur;
    /** Utilisations restantes (absent : Changeur neuf). */
    private static NamespacedKey cleUtilisations;

    private final Map<UUID, Long> derniereOuverture = new HashMap<>();
    /** Forme choisie par chaque joueur (sphère par défaut, oubliée au redémarrage). */
    private final Map<UUID, Forme> formes = new HashMap<>();
    private List<Biome> biomes;
    private Historique historique;
    private MenuBiome menu;

    @Override
    public void onEnable() {
        marqueur = new NamespacedKey(this, "changeur_biome");
        cleUtilisations = new NamespacedKey(this, "utilisations");
        biomes = new ArrayList<>(RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
                .getTagValues(BiomeTagKeys.IS_OVERWORLD));
        biomes.sort(Comparator.comparing(biome -> biome.getKey().toString()));
        historique = new Historique(this, new File(getDataFolder(), "historique.yml"));
        menu = new MenuBiome(this, historique);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(biomes.size() + " biomes de l'overworld proposés.");
    }

    @Override
    public void onDisable() {
        if (menu != null) {
            menu.enregistrerTextes();
        }
    }

    List<Biome> biomes() {
        return biomes;
    }

    Forme forme(Player player) {
        return formes.getOrDefault(player.getUniqueId(), Forme.SPHERE);
    }

    void changerForme(Player player) {
        formes.put(player.getUniqueId(), forme(player) == Forme.SPHERE ? Forme.CUBE : Forme.SPHERE);
    }

    // ------------------------------------------------------------------ objet

    /** Un Changeur de Biome. Nécessite que le plugin soit activé. */
    public static ItemStack creerChangeur() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Changeur de Biome", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft("end_crystal"));
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(marqueur, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return avecUtilisations(item, UTILISATIONS);
    }

    /** Utilisations restantes d'un Changeur de Biome (UTILISATIONS s'il n'a pas de compteur). */
    static int utilisations(ItemStack item) {
        Integer restantes = item.getItemMeta().getPersistentDataContainer().get(cleUtilisations, PersistentDataType.INTEGER);
        return restantes == null ? UTILISATIONS : restantes;
    }

    /** Le Changeur avec ce nombre d'utilisations restantes, écrit dans sa description. */
    private static ItemStack avecUtilisations(ItemStack item, int restantes) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(cleUtilisations, PersistentDataType.INTEGER, restantes);
        meta.lore(List.of(Component.text("Utilisations restantes : " + restantes + "/" + UTILISATIONS, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean estChangeur(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(marqueur, PersistentDataType.BYTE);
    }

    static boolean dansOverworld(Player player) {
        return player.getWorld().getEnvironment() == World.Environment.NORMAL;
    }

    // ------------------------------------------------------------------ utilisation

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || event.getHand() == null || !estChangeur(event.getItem())) {
            return;
        }
        // Annule l'effet vanilla du livre de connaissances (et l'interaction avec le bloc visé).
        event.setCancelled(true);
        Player player = event.getPlayer();
        // Un clic peut arriver en double (Bedrock, ou objet dans les deux mains) : un seul menu par seconde.
        long maintenant = System.currentTimeMillis();
        Long dernier = derniereOuverture.put(player.getUniqueId(), maintenant);
        if (dernier != null && maintenant - dernier < 1000) {
            return;
        }
        if (!dansOverworld(player)) {
            player.sendMessage(Component.text("Le Changeur de Biome ne s'utilise que dans l'overworld.", NamedTextColor.RED));
            return;
        }
        menu.ouvrir(player);
    }

    /** Choix d'un biome dans le menu : tout est revérifié au moment de la validation. */
    void changer(Player player, Biome biome) {
        if (!player.isOnline()) {
            return;
        }
        if (!dansOverworld(player)) {
            player.sendMessage(Component.text("Le Changeur de Biome ne s'utilise que dans l'overworld.", NamedTextColor.RED));
            return;
        }
        int slot = slotChangeur(player);
        if (slot < 0) {
            player.sendMessage(Component.text("Tu n'as plus de Changeur de Biome dans ton inventaire.", NamedTextColor.RED));
            return;
        }
        Location centre = player.getLocation();
        Forme forme = forme(player);
        List<int[]> cellules = cellules(centre, forme);
        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")
                && ProtectionWorldGuard.touche(centre.getWorld(), cellules)) {
            player.sendMessage(Component.text("Impossible : la zone touche une zone protégée. Ton Changeur de Biome n'a pas"
                    + " été utilisé.", NamedTextColor.RED));
            return;
        }
        // 1.1.0 (LeKiwi06, catégorie 3) : refusé si la zone touche le claim d'un autre (sauf pour ses membres).
        if (getServer().getPluginManager().isPluginEnabled("SimpleClaimSystem")
                && ProtectionClaims.touche(centre.getWorld(), cellules, player)) {
            player.sendMessage(Component.text("Impossible : la zone touche le claim d'un autre joueur. Ton Changeur de Biome"
                    + " n'a pas été utilisé.", NamedTextColor.RED));
            return;
        }
        int restantes = utiliser(player, slot);
        appliquer(centre.getWorld(), cellules, biome);
        player.sendMessage(Component.text("Vous avez changé le biome pour : ", NamedTextColor.GREEN)
                .append(Component.translatable(biome, NamedTextColor.GOLD)));
        if (restantes > 0) {
            player.sendMessage(Component.text("Utilisations restantes de ce Changeur de Biome : " + restantes + "/"
                    + UTILISATIONS, NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Ton Changeur de Biome s'est cassé.", NamedTextColor.GRAY));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
        }
        historique.ajouter(player, centre, biome, forme);
    }

    /**
     * Retire une utilisation au Changeur de la case ; renvoie celles qui lui restent (0 : cassé, retiré). Dans une pile,
     * le Changeur utilisé reste dans la case (il resservira en premier) et le reste de la pile va ailleurs dans
     * l'inventaire (au sol s'il est plein).
     */
    private static int utiliser(Player player, int slot) {
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(slot);
        int restantes = utilisations(item) - 1;
        ItemStack reste = null;
        if (item.getAmount() > 1) {
            reste = item.clone();
            reste.setAmount(item.getAmount() - 1);
        }
        inventory.setItem(slot, restantes > 0 ? avecUtilisations(item.asOne(), restantes) : null);
        if (reste != null) {
            for (ItemStack deborde : inventory.addItem(reste).values()) {
                player.getWorld().dropItem(player.getLocation(), deborde);
            }
        }
        return restantes;
    }

    /**
     * Case d'un Changeur de Biome : celui de la main principale en priorité, sinon celui de l'inventaire qui a le moins
     * d'utilisations restantes (pour finir un Changeur entamé avant d'en entamer un autre) ; -1.
     */
    private static int slotChangeur(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (estChangeur(inventory.getItemInMainHand())) {
            return inventory.getHeldItemSlot();
        }
        ItemStack[] contenu = inventory.getContents();
        int slot = -1;
        for (int i = 0; i < contenu.length; i++) {
            if (estChangeur(contenu[i]) && (slot < 0 || utilisations(contenu[i]) < utilisations(contenu[slot]))) {
                slot = i;
            }
        }
        return slot;
    }

    /**
     * Cellules de 4 x 4 x 4 blocs (résolution des biomes du jeu), en coordonnées de cellule (qx, qy, qz), dont le centre
     * est dans la forme : sphère de RAYON blocs, ou cube de DEMI_COTE blocs de part et d'autre du joueur. Limitées à la
     * hauteur du monde.
     */
    static List<int[]> cellules(Location centre, Forme forme) {
        World world = centre.getWorld();
        int minY = Math.max(world.getMinHeight(), centre.getBlockY() - RAYON);
        int maxY = Math.min(world.getMaxHeight() - 1, centre.getBlockY() + RAYON);
        List<int[]> liste = new ArrayList<>();
        for (int qx = Math.floorDiv(centre.getBlockX() - RAYON, 4); qx <= Math.floorDiv(centre.getBlockX() + RAYON, 4); qx++) {
            for (int qz = Math.floorDiv(centre.getBlockZ() - RAYON, 4); qz <= Math.floorDiv(centre.getBlockZ() + RAYON, 4); qz++) {
                for (int qy = Math.floorDiv(minY, 4); qy <= Math.floorDiv(maxY, 4); qy++) {
                    double dx = qx * 4 + 2 - centre.getX();
                    double dy = qy * 4 + 2 - centre.getY();
                    double dz = qz * 4 + 2 - centre.getZ();
                    boolean dedans = forme == Forme.SPHERE
                            ? dx * dx + dy * dy + dz * dz <= (double) RAYON * RAYON
                            : Math.abs(dx) <= DEMI_COTE && Math.abs(dy) <= DEMI_COTE && Math.abs(dz) <= DEMI_COTE;
                    if (dedans) {
                        liste.add(new int[] {qx, qy, qz});
                    }
                }
            }
        }
        return liste;
    }

    /** Change le biome des cellules, sans toucher aux blocs, puis renvoie les chunks concernés aux joueurs (couleurs de
     * l'herbe, du feuillage, de l'eau...). */
    private static void appliquer(World world, List<int[]> cellules, Biome biome) {
        Set<Long> chunks = new HashSet<>();
        for (int[] c : cellules) {
            world.setBiome(c[0] * 4, c[1] * 4, c[2] * 4, biome);
            chunks.add(((long) (c[0] >> 2) << 32) | ((c[2] >> 2) & 0xFFFFFFFFL));
        }
        for (long chunk : chunks) {
            world.refreshChunk((int) (chunk >> 32), (int) chunk);
        }
    }
}
