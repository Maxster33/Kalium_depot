package fr.kalium.bingo.map;

import fr.kalium.bingo.game.BingoGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Une carte (MapView) par partie en cours, la meme pour tous ses joueurs (0.4.0, voir GridMapRenderer). Creee a la
 * demande, oubliee au nettoyage de la partie. Apres un redemarrage du serveur, une nouvelle carte est creee et
 * redonnee aux joueurs (voir GameItems.give).
 */
public final class GameMapService {

    private final IconLibrary icons;
    private final Map<String, MapView> views = new HashMap<>();

    public GameMapService(IconLibrary icons) {
        this.icons = icons;
    }

    public MapView viewFor(BingoGame game) {
        return views.computeIfAbsent(game.getGameId(), id -> {
            MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
            for (MapRenderer renderer : view.getRenderers()) {
                view.removeRenderer(renderer);
            }
            view.setTrackingPosition(false);
            view.setUnlimitedTracking(false);
            view.setLocked(true);
            view.addRenderer(new GridMapRenderer(game, icons));
            return view;
        });
    }

    /** Objet carte de cette partie (sans l'etiquette "objet du plugin", ajoutee par GameItems). */
    public ItemStack mapItem(BingoGame game) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapView(viewFor(game));
        meta.displayName(Component.text("Grille Bingo", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("À tenir en main secondaire.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /** Numero de la carte de cette partie, ou -1 si elle n'existe pas encore. */
    public int mapIdOf(BingoGame game) {
        MapView view = views.get(game.getGameId());
        return view == null ? -1 : view.getId();
    }

    public void forget(String gameId) {
        MapView view = views.remove(gameId);
        if (view != null) {
            for (MapRenderer renderer : view.getRenderers()) {
                view.removeRenderer(renderer);
            }
        }
    }
}
