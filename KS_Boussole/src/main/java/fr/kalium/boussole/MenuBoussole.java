package fr.kalium.boussole;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu en coffre de la boussole du voyageur (boîte à outils de KLM_Menu) : un objet par biome du monde du joueur,
 * 45 par page, dans l'ordre des noms français ; le biome visé brille. Barre du bas : pages à gauche et à droite,
 * état de la boussole au centre (biome visé, temps de recharge), « Fermer » à côté.
 */
final class MenuBoussole {

    private static final String MARQUE = "ks_boussole";

    private final KSBoussole plugin;
    private final Lang lang;

    MenuBoussole(KSBoussole plugin, Lang lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    void ouvrir(Player joueur, int page) {
        List<Biome> biomes = plugin.biomes(joueur.getWorld());
        int pages = Math.max(1, (biomes.size() + Contenant.PAR_PAGE - 1) / Contenant.PAR_PAGE);
        int numero = Math.max(0, Math.min(page, pages - 1));
        String cible = plugin.cibleTenue(joueur);
        Contenant menu = Contenant.pour(joueur, lang.c("menu-titre", "Boussole du voyageur"), MARQUE);
        int debut = numero * Contenant.PAR_PAGE;
        for (int i = 0; i < Contenant.PAR_PAGE && debut + i < biomes.size(); i++) {
            Biome biome = biomes.get(debut + i);
            boolean vise = biome.key().asString().equals(cible);
            List<Component> lignes = new ArrayList<>();
            if (vise) {
                lignes.add(lang.c("menu-biome-vise", "<green>Ta boussole pointe vers ce biome"));
            }
            lignes.addAll(Contenant.lignes(lang.c("menu-biome-clic",
                    "<gray>Clic : chercher ce biome à moins de <rayon> blocs", "rayon", KSBoussole.milliers(plugin.rayon()))));
            ItemStack objet = Contenant.objet(icone(biome), Component.text(plugin.nom(biome), NamedTextColor.WHITE), lignes);
            if (vise) {
                ItemMeta meta = objet.getItemMeta();
                meta.setEnchantmentGlintOverride(true);
                objet.setItemMeta(meta);
            }
            menu.poser(i, objet, p -> plugin.chercher(p, biome));
        }
        menu.barre();
        if (numero > 0) {
            menu.poser(menu.bas(0), Contenant.objet(Material.ARROW, lang.c("menu-precedente", "<yellow>Page précédente"),
                    List.of()), p -> ouvrir(p, numero - 1));
        }
        if (numero < pages - 1) {
            menu.poser(menu.bas(8), Contenant.objet(Material.ARROW, lang.c("menu-suivante", "<yellow>Page suivante"),
                    List.of()), p -> ouvrir(p, numero + 1));
        }
        menu.poser(menu.bas(4), etat(joueur, cible), null);
        menu.poser(menu.bas(5), Contenant.objet(Material.BARRIER, lang.c("menu-fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
        menu.ouvrir(joueur);
    }

    /** Case du centre : biome visé et temps de recharge. */
    private ItemStack etat(Player joueur, String cible) {
        List<Component> lignes = new ArrayList<>();
        lignes.add(cible == null
                ? lang.c("menu-etat-sans-cible", "<gray>Pas encore réglée")
                : lang.c("menu-etat-cible", "<gray>Pointe vers : <white><biome>", "biome", plugin.nom(cible)));
        long reste = plugin.recharge(joueur);
        lignes.add(reste > 0
                ? lang.c("menu-etat-recharge", "<red>Recharge : <temps>", "temps", KSBoussole.duree(reste))
                : lang.c("menu-etat-prete", "<green>Prête à chercher"));
        return Contenant.objet(Material.COMPASS, lang.c("menu-etat-nom", "<gold>Boussole du voyageur"), lignes);
    }

    /** Objet qui représente un biome, d'après son identifiant (vanilla et Terralith). */
    static Material icone(Biome biome) {
        String id = biome.key().value();
        String[][] regles = {
                {"deep_dark", "SCULK"}, {"lush_caves", "MOSS_BLOCK"}, {"dripstone", "POINTED_DRIPSTONE"},
                {"sulfur", "YELLOW_CONCRETE_POWDER"}, {"cave", "STONE"},
                {"mushroom", "RED_MUSHROOM_BLOCK"}, {"cherry", "CHERRY_LEAVES"}, {"pale", "PALE_OAK_LOG"},
                {"mangrove", "MANGROVE_ROOTS"}, {"bamboo", "BAMBOO"}, {"jungle", "JUNGLE_LOG"},
                {"frozen", "PACKED_ICE"}, {"ice", "PACKED_ICE"}, {"glacial", "BLUE_ICE"},
                {"warm_ocean", "BRAIN_CORAL_BLOCK"}, {"ocean", "PRISMARINE"}, {"river", "WATER_BUCKET"},
                {"beach", "SAND"}, {"shore", "STONE"}, {"swamp", "LILY_PAD"},
                {"badlands", "TERRACOTTA"}, {"desert", "SAND"}, {"savanna", "ACACIA_LOG"},
                {"volcanic", "MAGMA_BLOCK"}, {"snowy", "SNOW_BLOCK"}, {"peaks", "SNOW_BLOCK"}, {"wintry", "SNOW_BLOCK"},
                {"slopes", "SNOW_BLOCK"}, {"cliffs", "STONE"}, {"mountains", "STONE"}, {"windswept", "GRAVEL"},
                {"grove", "SPRUCE_LEAVES"}, {"taiga", "SPRUCE_LOG"}, {"birch", "BIRCH_LOG"}, {"dark_forest", "DARK_OAK_LOG"},
                {"flower", "POPPY"}, {"sunflower", "SUNFLOWER"}, {"meadow", "CORNFLOWER"}, {"forest", "OAK_LOG"},
                {"plains", "GRASS_BLOCK"},
                {"crimson", "CRIMSON_NYLIUM"}, {"warped", "WARPED_NYLIUM"}, {"soul_sand", "SOUL_SAND"},
                {"basalt", "BASALT"}, {"nether", "NETHERRACK"},
                {"end", "END_STONE"}, {"void", "BLACK_CONCRETE"},
        };
        for (String[] regle : regles) {
            if (id.contains(regle[0])) {
                return Material.valueOf(regle[1]);
            }
        }
        return Material.GRASS_BLOCK;
    }
}
