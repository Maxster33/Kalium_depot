package fr.kalium.bingo.map;

import fr.kalium.bingo.game.BingoGame;
import fr.kalium.bingo.game.BingoInstance;
import fr.kalium.bingo.game.TeamStyle;
import fr.kalium.bingo.grid.BingoGrid;
import fr.kalium.bingo.grid.Objective;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Dessin de la carte de la grille (0.4.0) - demande explicite de LeKiwi06, 24/09/2026 : une seule carte pour tous
 * les joueurs, a tenir en main secondaire. Fond blanc, separations noires ; une case validee prend la couleur de
 * l'equipe (TeamStyle), partagee en 2, 3 ou 4 bandes verticales de meme largeur si plusieurs equipes l'ont validee
 * (toute la case est coloree, sans coin blanc) ; icone de l'objet centree dans la case, sans jamais toucher les
 * lignes ; quantite en bas a droite de l'icone (comme dans un inventaire), en blanc entoure de noir pour rester
 * lisible sur toutes les icones.
 *
 * Grille 5x5 sur 128 pixels : lignes de 1 pixel, cases de 24 pixels, icones 16x16 (marge de 4 pixels).
 * Redessinee seulement quand une case est validee ou quand les icones deviennent disponibles.
 */
public final class GridMapRenderer extends MapRenderer {

    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;
    private static final Color UNKNOWN = new Color(120, 120, 120);

    private final BingoGame game;
    private final java.util.function.Function<org.bukkit.Material, BufferedImage> icons;
    private final java.util.function.IntSupplier iconsGeneration;
    private long drawnSignature = -1;

    public GridMapRenderer(BingoGame game, IconLibrary icons) {
        this(game, icons::icon, icons::generation);
    }

    /** Constructeur direct (aussi utilise pour tester le dessin hors serveur). */
    public GridMapRenderer(BingoGame game, java.util.function.Function<org.bukkit.Material, BufferedImage> icons,
                           java.util.function.IntSupplier iconsGeneration) {
        super(false); // une seule image pour tous les joueurs
        this.game = game;
        this.icons = icons;
        this.iconsGeneration = iconsGeneration;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        BingoGrid grid = game.getGrid();
        if (grid == null || game.getScoreEngine() == null) {
            return;
        }
        long signature = game.getScoreEngine().validationCount() * 1000L + iconsGeneration.getAsInt();
        if (signature == drawnSignature) {
            return;
        }
        drawnSignature = signature;

        int n = grid.getSize();
        int cell = (128 - (n + 1)) / n;
        int total = n * cell + n + 1;
        int origin = (128 - total) / 2;

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                canvas.setPixelColor(x, y, WHITE);
            }
        }
        for (int i = 0; i <= n; i++) {
            int line = origin + i * (cell + 1);
            for (int k = origin; k < origin + total; k++) {
                canvas.setPixelColor(line, k, BLACK);
                canvas.setPixelColor(k, line, BLACK);
            }
        }
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int index = row * n + col;
                int x0 = origin + 1 + col * (cell + 1);
                int y0 = origin + 1 + row * (cell + 1);
                drawBackground(canvas, index, x0, y0, cell);
                Objective objective = grid.getCells().get(index).getObjective();
                drawIcon(canvas, objective, x0 + (cell - 16) / 2, y0 + (cell - 16) / 2);
                if (objective.quantity() > 1) {
                    drawQuantity(canvas, String.valueOf(objective.quantity()), x0 + cell - 2, y0 + cell - 2);
                }
            }
        }
    }

    /** Bandes verticales de meme largeur, une par equipe ayant valide la case (dans l'ordre A, B, C, D). */
    private void drawBackground(MapCanvas canvas, int index, int x0, int y0, int cell) {
        List<Integer> teams = new ArrayList<>();
        for (BingoInstance instance : game.getInstances()) {
            int team = instance.getTeam().getTeamNumber();
            if (game.isValidated(team, index)) {
                teams.add(team);
            }
        }
        teams.sort(Integer::compare);
        if (teams.isEmpty()) {
            return;
        }
        for (int i = 0; i < teams.size(); i++) {
            int from = Math.round(i * cell / (float) teams.size());
            int to = Math.round((i + 1) * cell / (float) teams.size());
            Color color = TeamStyle.mapColor(teams.get(i));
            for (int x = x0 + from; x < x0 + to; x++) {
                for (int y = y0; y < y0 + cell; y++) {
                    canvas.setPixelColor(x, y, color);
                }
            }
        }
    }

    private void drawIcon(MapCanvas canvas, Objective objective, int x0, int y0) {
        BufferedImage image = icons.apply(objective.material());
        if (image == null) {
            drawOutlined(canvas, "?", x0 + 5, y0 + 4, UNKNOWN);
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) >= 128) {
                    canvas.setPixelColor(x0 + x, y0 + y, new Color(argb, false));
                }
            }
        }
    }

    /** Quantite alignee a droite sur {@code right} et en bas sur {@code bottom} (pixels inclus). */
    private void drawQuantity(MapCanvas canvas, String text, int right, int bottom) {
        MapFont font = MinecraftFont.Font;
        int width = font.getWidth(text);
        int height = font.getHeight();
        drawOutlined(canvas, text, right - width + 1, bottom - height + 2, WHITE);
    }

    /** Texte avec un contour noir d'un pixel. */
    private void drawOutlined(MapCanvas canvas, String text, int x, int y, Color color) {
        MapFont font = MinecraftFont.Font;
        for (int pass = 0; pass < 2; pass++) {
            int cx = x;
            for (char c : text.toCharArray()) {
                MapFont.CharacterSprite sprite = font.getChar(c);
                if (sprite == null) {
                    continue;
                }
                for (int r = 0; r < sprite.getHeight(); r++) {
                    for (int col = 0; col < sprite.getWidth(); col++) {
                        if (!sprite.get(r, col)) {
                            continue;
                        }
                        if (pass == 0) {
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dy = -1; dy <= 1; dy++) {
                                    set(canvas, cx + col + dx, y + r + dy, BLACK);
                                }
                            }
                        } else {
                            set(canvas, cx + col, y + r, color);
                        }
                    }
                }
                cx += sprite.getWidth() + 1;
            }
        }
    }

    private static void set(MapCanvas canvas, int x, int y, Color color) {
        if (x >= 0 && x < 128 && y >= 0 && y < 128) {
            canvas.setPixelColor(x, y, color);
        }
    }
}
