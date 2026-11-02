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
 * - Lumière : 10 (bleue, comme la torche des âmes), 13 (cuivre ; 14 comme sa torche, ramenée à 13 le 02/11 par
 *   LeKiwi06 : les cookies à la baie lumineuse de KS_Cuisine posent des lumières de niveau 14 et effacent au chargement
 *   d'un chunk toutes celles qu'ils ne portent pas). Le jeu calcule la lumière à partir du bloc qu'il voit (un bloc
 *   musical n'éclaire pas) : un bloc de lumière est posé devant la face, seulement dans l'air ou une source d'eau, et
 *   retiré avec la jack o'lantern (seulement s'il est de son niveau : pas celui d'un joueur au cookie). Face bouchée ou
 *   lumière d'un autre niveau devant : pas de lumière, remise dès que la place se libère (vérifiée toutes les 5 s).
 * - Orientation : face vers le joueur qui la pose, comme une citrouille. Objet : un bloc musical à l'image de la jack
 *   o'lantern vanilla tant que KS_Textures n'a pas leur texture.
 */
final class JackOLanterns implements Listener {

    static final String BLEUE = "ks_blocs:jack_o_lantern_bleue";
    static final String CUIVRE = "ks_blocs:jack_o_lantern_cuivre";

    private final KSBlocs plugin;
    private final List<NamespacedKey> recettes = new ArrayList<>();
    private final List<Lumiere> lumieres = new ArrayList<>();

    JackOLanterns(KSBlocs plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        declarer(BLEUE, "jack_o_lantern_bleue", plugin.t("objet.jack_o_lantern_bleue",
                "<#4FC3F7>Jack o'lantern bleue"), Material.SOUL_TORCH, 10);
        declarer(CUIVRE, "jack_o_lantern_cuivre", plugin.t("objet.jack_o_lantern_cuivre",
                "<#E3A15A>Jack o'lantern cuivre"), Material.COPPER_TORCH, 13);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getOnlinePlayers().forEach(p -> p.discoverRecipes(recettes));
        // lumière prise ou laissée libre entre-temps (cookie, bloc retiré par un piston...)
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Lumiere l : lumieres) {
                KSBlocs.pourChaque(l.id, l::allumer);
            }
        }, 100L, 100L);
    }

    void arreter() {
        recettes.forEach(Bukkit::removeRecipe);
    }

    private void declarer(String id, String apparence, net.kyori.adventure.text.Component nom, Material torche,
                          int niveau) {
        ItemStack objet = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        objet.setItemMeta(meta);
        objet.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "jack_o_lantern"));
        Lumiere lumiere = new Lumiere(id, niveau);
        lumieres.add(lumiere);
        KSBlocs.declarer(new TypeBloc(id, apparence, Material.NOTE_BLOCK).objet(objet).ecouteur(lumiere));

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

    private static BlockFace face(String variante) {
        return BlockFace.valueOf(variante.toUpperCase(Locale.ROOT));
    }

    /** Le bloc de lumière devant une jack o'lantern d'un type (un niveau par type). */
    private final class Lumiere implements EcouteurBloc {

        private final String id;
        private final int niveau;

        Lumiere(String id, int niveau) {
            this.id = id;
            this.niveau = niveau;
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
            if (devant.getBlockData() instanceof Light l && l.getLevel() == niveau) {
                devant.setType(l.isWaterlogged() ? Material.WATER : Material.AIR);
                // une autre jack o'lantern du même type éclairait peut-être la même place
                plugin.getServer().getScheduler().runTask(plugin, () -> KSBlocs.verifierAutour(devant));
            }
        }

        void allumer(Block bloc, String variante) {
            if (bloc.getType() != Material.NOTE_BLOCK) {
                return;
            }
            Block devant = bloc.getRelative(face(variante));
            boolean eau = devant.getType() == Material.WATER && devant.getBlockData() instanceof Levelled l
                    && l.getLevel() == 0;
            if (devant.getType().isAir() || eau) {
                Light light = (Light) Material.LIGHT.createBlockData();
                light.setLevel(niveau);
                light.setWaterlogged(eau);
                devant.setBlockData(light, false);
            }
        }
    }
}
