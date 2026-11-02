package fr.kalium.repeteurwifi;

import fr.kalium.blocs.KSBlocs;
import fr.kalium.blocs.TypeBloc;
import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.event.block.TargetHitEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * KS_RepeteurWifi (cahier local Blocs_Cultures_Objets.md, partie 7, LeKiwi06, 01-02/11/2026) : répéteur wifi de
 * Kixster, bloc custom du socle KS_Blocs.
 *
 * - Craft (sans forme) : 1 capteur sculk calibré, 3 répéteurs, 3 comparateurs, 2 lingots d'or.
 * - Clic droit : menu en coffre (émission / réception, bouton « Code » qui ouvre une enclume pour taper 6 lettres ou
 *   chiffres) ; accroupi, le clic pose le bloc tenu contre le répéteur. Dans un claim : propriétaire et membres
 *   seulement.
 * - Un récepteur s'allume (puissance 15, comme un bloc de redstone) quand un émetteur du même code, à 64 blocs ou moins
 *   dans le même monde, reçoit du courant. Un récepteur change d'état au plus une fois toutes les 10 ticks (config).
 *
 * Côté serveur, chaque répéteur est un vrai bloc cible (target) : le jeu lui donne une puissance de 0 à 15 (propriété
 * « power », TargetBlock.ownSignal) émise vers les 6 faces, qui ne bouge plus tant qu'aucun projectile ne le touche
 * (son tick de remise à 0 n'est programmé que par un projectile ; TargetHitEvent annulé ici). Le plugin règle donc la
 * puissance lui-même, et KS_Blocs montre aux joueurs l'état du bloc musical réservé à l'apparence du répéteur. Trois
 * types de KS_Blocs (une apparence chacun) : émetteur, récepteur éteint, récepteur allumé.
 */
public final class KSRepeteurWifi extends JavaPlugin implements Listener {

    static final String EMETTEUR = "ks_repeteurwifi:emetteur";
    static final String RECEPTEUR = "ks_repeteurwifi:recepteur";
    static final String RECEPTEUR_ALLUME = "ks_repeteurwifi:recepteur_allume";
    /** Seule variante de chaque apparence. */
    static final String VARIANTE = "defaut";

    Lang lang;
    Repeteurs repeteurs;
    MenuRepeteur menu;
    private NamespacedKey cleRecette;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        repeteurs = new Repeteurs(this, getConfig().getInt("portee", 64),
                Math.max(1, getConfig().getInt("intervalle-minimum-ticks", 10)));
        menu = new MenuRepeteur(this);
        try {
            ItemStack objet = objetModele();
            KSBlocs.declarer(new TypeBloc(EMETTEUR, "repeteur_wifi_emetteur", Material.TARGET).objet(objet)
                    .butin(c -> butin()).ecouteur(repeteurs));
            KSBlocs.declarer(new TypeBloc(RECEPTEUR, "repeteur_wifi_recepteur", Material.TARGET)
                    .butin(c -> butin()).ecouteur(repeteurs));
            KSBlocs.declarer(new TypeBloc(RECEPTEUR_ALLUME, "repeteur_wifi_recepteur_allume", Material.TARGET)
                    .butin(c -> butin()).ecouteur(repeteurs));
        } catch (RuntimeException e) {
            // apparences absentes de apparences.json (KS_Blocs) : rien ne peut s'afficher
            getLogger().severe("Types de blocs refuses par KS_Blocs : " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cleRecette = new NamespacedKey(this, "repeteur_wifi");
        ShapelessRecipe recette = new ShapelessRecipe(cleRecette, KSBlocs.objet(EMETTEUR, 1));
        recette.addIngredient(1, Material.CALIBRATED_SCULK_SENSOR);
        recette.addIngredient(3, Material.REPEATER);
        recette.addIngredient(3, Material.COMPARATOR);
        recette.addIngredient(2, Material.GOLD_INGOT);
        Bukkit.addRecipe(recette);
        Bukkit.getOnlinePlayers().forEach(p -> p.discoverRecipe(cleRecette));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getScheduler().runTaskTimer(this, repeteurs::chaqueTick, 1L, 1L);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        if (cleRecette != null) {
            Bukkit.removeRecipe(cleRecette);
        }
        if (menu != null) {
            menu.fermerTout();
        }
    }

    Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    /** L'objet du répéteur (bloc cible, image provisoire du capteur sculk calibré en attendant sa règle KS_Textures). */
    private ItemStack objetModele() {
        ItemStack objet = new ItemStack(Material.TARGET);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(t("objet.nom", "<aqua>Répéteur wifi").decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (Component l : Contenant.lignes(t("objet.description",
                "<gray>Transmet la redstone sans fil, jusqu'à <portee> blocs. Clic droit : régler.",
                "portee", repeteurs.portee()))) {
            lore.add(l.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        objet.setItemMeta(meta);
        objet.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "calibrated_sculk_sensor"));
        return objet;
    }

    /** Ce que donne un répéteur cassé, quel que soit son mode : un répéteur neuf (sans code). */
    private static List<ItemStack> butin() {
        ItemStack o = KSBlocs.objet(EMETTEUR, 1);
        return o == null ? List.of() : List.of(o);
    }

    static boolean estRepeteur(Block b) {
        TypeBloc t = KSBlocs.type(b);
        return t != null && t.id().startsWith("ks_repeteurwifi:") && b.getType() == Material.TARGET;
    }

    // ------------------------------------------------------------------ événements

    /** Clic droit : le menu (accroupi, le bloc tenu se pose contre le répéteur, comme sur un coffre). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClic(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || b == null || !estRepeteur(b)) {
            return;
        }
        Player joueur = event.getPlayer();
        if (joueur.isSneaking() && event.hasItem()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!Claims.peutRegler(joueur, b)) {
            Contenant.message(joueur, t("message.claim",
                    "<red>Ce répéteur est dans le claim d'un autre joueur."), true);
            return;
        }
        menu.ouvrir(joueur, b);
    }

    /** Un projectile ne change pas la puissance d'un répéteur (le bloc cible du jeu le ferait). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCible(TargetHitEvent event) {
        if (event.getHitBlock() != null && estRepeteur(event.getHitBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        repeteurs.pousses(event.getBlocks(), sens(event.getBlock(), event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        repeteurs.pousses(event.getBlocks(), sens(event.getBlock(), event.getDirection().getOppositeFace())
                .getOppositeFace());
    }

    /** Sens de la poussée : la face du piston (comme KS_Blocs). */
    private static BlockFace sens(Block piston, BlockFace parDefaut) {
        return piston.getBlockData() instanceof org.bukkit.block.data.Directional d ? d.getFacing() : parDefaut;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDechargement(ChunkUnloadEvent event) {
        repeteurs.decharger(event.getChunk());
    }

    @EventHandler
    public void onArrivee(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(cleRecette);
    }
}
