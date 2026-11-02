package fr.kalium.blocs;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Jack o'lanterns bleue et cuivre (cahier, partie 8 ; choix de LeKiwi06 du 02/11/2026) : premier bloc custom, pour
 * valider le socle.
 *
 * - Recettes comme la vraie : citrouille sculptée + torche des âmes (bleue), citrouille sculptée + torche de cuivre
 *   (cuivre), sans forme.
 * - Lumière de leur torche : 10 (bleue), 14 (cuivre). Le jeu calcule la lumière à partir du bloc qu'il voit (un bloc
 *   musical n'éclaire pas) : un bloc de lumière est posé devant la face, seulement dans l'air ou une source d'eau (comme
 *   les cookies à la baie lumineuse de KS_Cuisine), et retiré avec la jack o'lantern. Face bouchée : pas de lumière,
 *   remise dès que la place se libère.
 * - Orientation : face vers le joueur qui la pose, comme une citrouille. Objet : un bloc musical à l'image de la jack
 *   o'lantern vanilla tant que KS_Textures n'a pas leur texture.
 */
final class JackOLanterns implements Listener, EcouteurBloc {

    static final String BLEUE = "ks_blocs:jack_o_lantern_bleue";
    static final String CUIVRE = "ks_blocs:jack_o_lantern_cuivre";

    private final KSBlocs plugin;
    private final List<NamespacedKey> recettes = new ArrayList<>();

    JackOLanterns(KSBlocs plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        declarer(BLEUE, "jack_o_lantern_bleue", plugin.t("objet.jack_o_lantern_bleue",
                "<#4FC3F7>Jack o'lantern bleue"), Material.SOUL_TORCH);
        declarer(CUIVRE, "jack_o_lantern_cuivre", plugin.t("objet.jack_o_lantern_cuivre",
                "<#E3A15A>Jack o'lantern cuivre"), Material.COPPER_TORCH);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getOnlinePlayers().forEach(p -> p.discoverRecipes(recettes));
    }

    void arreter() {
        recettes.forEach(Bukkit::removeRecipe);
    }

    private void declarer(String id, String apparence, net.kyori.adventure.text.Component nom, Material torche) {
        ItemStack objet = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        objet.setItemMeta(meta);
        objet.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "jack_o_lantern"));
        KSBlocs.declarer(new TypeBloc(id, apparence, Material.NOTE_BLOCK).objet(objet).ecouteur(this));

        NamespacedKey cle = new NamespacedKey(plugin, apparence);
        ShapelessRecipe r = new ShapelessRecipe(cle, KSBlocs.objet(id, 1));
        r.addIngredient(new RecipeChoice.MaterialChoice(Material.CARVED_PUMPKIN));
        r.addIngredient(new RecipeChoice.MaterialChoice(torche));
        Bukkit.addRecipe(r);
        recettes.add(cle);
    }

    @EventHandler
    public void onArrivee(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        p.discoverRecipes(recettes);
    }

    // ------------------------------------------------------------------ lumière

    private static int niveau(String id) {
        return BLEUE.equals(id) ? 10 : 14;
    }

    private static BlockFace face(String variante) {
        return BlockFace.valueOf(variante.toUpperCase(Locale.ROOT));
    }

    @Override
    public void pose(Block bloc, String variante) {
        allumer(bloc, variante);
    }

    @Override
    public void verifie(Block bloc, String variante) {
        allumer(bloc, variante);
    }

    @Override
    public void retire(Block bloc, String variante) {
        Block devant = bloc.getRelative(face(variante));
        if (devant.getBlockData() instanceof Light l) {
            devant.setType(l.isWaterlogged() ? Material.WATER : Material.AIR);
            // une autre jack o'lantern éclairait peut-être la même place
            plugin.getServer().getScheduler().runTask(plugin, () -> KSBlocs.verifierAutour(devant));
        }
    }

    private void allumer(Block bloc, String variante) {
        TypeBloc t = KSBlocs.type(bloc);
        if (t == null) {
            return;
        }
        int niveau = niveau(t.id());
        Block devant = bloc.getRelative(face(variante));
        boolean eau = devant.getType() == Material.WATER && devant.getBlockData() instanceof Levelled l
                && l.getLevel() == 0;
        if (devant.getType().isAir() || eau) {
            Light light = (Light) Material.LIGHT.createBlockData();
            light.setLevel(niveau);
            light.setWaterlogged(eau);
            devant.setBlockData(light, false);
        } else if (devant.getBlockData() instanceof Light l && l.getLevel() < niveau) {
            l.setLevel(niveau);
            devant.setBlockData(l, false);
        }
    }
}
