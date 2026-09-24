package fr.kalium.bingo.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Trouve l'icone 16x16 d'un objet dans le jeu officiel (client.jar de Mojang) - 0.4.0, carte de la grille (demande de
 * LeKiwi06 : "prends les textures vanilla du jeu"). Aucune image n'est mise dans le depot (public) : elles sont lues
 * sur le serveur depuis le jeu telecharge chez Mojang (voir IconLibrary).
 *
 * Methode : modele de l'objet (assets/minecraft/items/&lt;id&gt;.json -&gt; models/...), en remontant les "parent" ;
 * objet plat : couches layer0 (+ layer1 superposee) ; bloc : une face (all, side, front...). Images animees : 1re image.
 * Quelques textures grises sont teintees comme en jeu (feuilles, nenuphar, cuir). Objets rendus en 3D sans texture
 * plate (coffre, bouclier, tete...) : texture "particle" du modele, a defaut rien (IconLibrary dessine alors "?").
 */
public final class IconResolver {

    private static final String[] BLOCK_KEYS = {"all", "front", "north", "side", "texture", "end", "top", "cross", "plant", "pattern", "particle"};

    private final ZipFile jar;

    public IconResolver(ZipFile jar) {
        this.jar = jar;
    }

    /** @param id identifiant de l'objet sans espace de noms (ex. "oak_log") ; null si introuvable. */
    public BufferedImage resolve(String id) throws IOException {
        BufferedImage special = special(id);
        if (special != null) {
            return special;
        }
        String model = modelOf(id);
        if (model == null) {
            return null;
        }
        Map<String, String> textures = new HashMap<>();
        collectTextures(model, textures, 0);
        if (textures.containsKey("layer0")) {
            BufferedImage base = texture(ref(textures, "layer0"));
            if (base == null) {
                return null;
            }
            base = tint(id, base, true);
            if (textures.containsKey("layer1")) {
                BufferedImage over = texture(ref(textures, "layer1"));
                if (over != null) {
                    Graphics2D g = base.createGraphics();
                    g.drawImage(over, 0, 0, null);
                    g.dispose();
                }
            }
            return base;
        }
        if (id.endsWith("_bed") && textures.containsKey("up")) {
            return texture(ref(textures, "up")); // dessus du lit (oreiller) plutot que les planches
        }
        for (String key : BLOCK_KEYS) {
            if (textures.containsKey(key)) {
                BufferedImage image = texture(ref(textures, key));
                if (image != null) {
                    return tint(id, image, false);
                }
            }
        }
        return null;
    }

    /**
     * Objets dessines en 3D par le jeu, sans texture plate : icone decoupee dans la texture du modele (face avant du
     * coffre, du bouclier, de la tete).
     */
    private BufferedImage special(String id) throws IOException {
        return switch (id) {
            case "chest", "trapped_chest" -> chestFront("entity/chest/normal");
            case "ender_chest" -> chestFront("entity/chest/ender");
            case "shield" -> crop("entity/shield/shield_base_nopattern", 1, 1, 12, 22);
            case "wither_skeleton_skull" -> crop("entity/skeleton/wither_skeleton", 8, 8, 8, 8);
            case "skeleton_skull" -> crop("entity/skeleton/skeleton", 8, 8, 8, 8);
            default -> null;
        };
    }

    /** Face avant d'un coffre : couvercle, corps et serrure. */
    private BufferedImage chestFront(String path) throws IOException {
        BufferedImage raw = rawTexture(path);
        if (raw == null) {
            return null;
        }
        int s = raw.getWidth() / 64;
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(raw.getSubimage(14 * s, 14 * s, 14 * s, 5 * s), 1, 1, 14, 5, null);   // couvercle
        g.drawImage(raw.getSubimage(14 * s, 33 * s, 14 * s, 10 * s), 1, 6, 14, 10, null); // corps
        g.drawImage(raw.getSubimage(1 * s, 1 * s, 2 * s, 4 * s), 7, 4, 2, 4, null);       // serrure
        g.dispose();
        return out;
    }

    /** Zone (x, y, largeur, hauteur, en pixels d'une texture 64 de large) agrandie et centree dans 16x16. */
    private BufferedImage crop(String path, int x, int y, int w, int h) throws IOException {
        BufferedImage raw = rawTexture(path);
        if (raw == null) {
            return null;
        }
        int s = raw.getWidth() / 64;
        BufferedImage part = raw.getSubimage(x * s, y * s, w * s, h * s);
        double scale = Math.min(16.0 / w, 16.0 / h);
        int dw = (int) Math.round(w * scale);
        int dh = (int) Math.round(h * scale);
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(part, (16 - dw) / 2, (16 - dh) / 2, dw, dh, null);
        g.dispose();
        return out;
    }

    private BufferedImage rawTexture(String path) throws IOException {
        ZipEntry entry = jar.getEntry("assets/minecraft/textures/" + path + ".png");
        if (entry == null) {
            return null;
        }
        try (InputStream in = jar.getInputStream(entry)) {
            return ImageIO.read(in);
        }
    }

    /** Nom du modele de l'objet ("minecraft:item/diamond"...), d'apres items/&lt;id&gt;.json puis models/item/&lt;id&gt;.json. */
    private String modelOf(String id) throws IOException {
        JsonObject def = json("assets/minecraft/items/" + id + ".json");
        if (def != null) {
            String found = firstModel(def);
            if (found != null) {
                return found;
            }
        }
        if (jar.getEntry("assets/minecraft/models/item/" + id + ".json") != null) {
            return "minecraft:item/" + id;
        }
        if (jar.getEntry("assets/minecraft/models/block/" + id + ".json") != null) {
            return "minecraft:block/" + id;
        }
        return null;
    }

    /** Premier champ "model" (texte) rencontre dans la definition de l'objet (cas "model", "condition", "select"...). */
    private static String firstModel(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement model = object.get("model");
            if (model != null && model.isJsonPrimitive() && "minecraft:model".equals(str(object.get("type")))) {
                return model.getAsString();
            }
            if (model != null && model.isJsonPrimitive() && object.get("type") == null) {
                return model.getAsString();
            }
            for (String key : List.of("model", "fallback", "on_false", "on_true", "cases", "entries", "base")) {
                String found = firstModel(object.get(key));
                if (found != null) {
                    return found;
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) {
                    String found = firstModel(entry.getValue());
                    if (found != null) {
                        return found;
                    }
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String found = firstModel(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String str(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    /** Textures du modele et de ses parents (celles de l'enfant priment). */
    private void collectTextures(String model, Map<String, String> into, int depth) throws IOException {
        if (depth > 12 || model == null || model.startsWith("builtin/")) {
            return;
        }
        JsonObject json = json("assets/minecraft/models/" + stripNamespace(model) + ".json");
        if (json == null) {
            return;
        }
        JsonObject textures = json.getAsJsonObject("textures");
        if (textures != null) {
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    into.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                } else if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("sprite")) {
                    into.putIfAbsent(entry.getKey(), entry.getValue().getAsJsonObject().get("sprite").getAsString()); // ex. verre
                }
            }
        }
        collectTextures(str(json.get("parent")), into, depth + 1);
    }

    /** Suit les references "#cle" jusqu'a un vrai chemin de texture. */
    private static String ref(Map<String, String> textures, String key) {
        String value = textures.get(key);
        for (int i = 0; i < 8 && value != null && value.startsWith("#"); i++) {
            value = textures.get(value.substring(1));
        }
        return value;
    }

    private static String stripNamespace(String name) {
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    /** Image 16x16 (1re image d'une texture animee, redimensionnee si besoin), ou null. */
    private BufferedImage texture(String path) throws IOException {
        if (path == null) {
            return null;
        }
        ZipEntry entry = jar.getEntry("assets/minecraft/textures/" + stripNamespace(path) + ".png");
        if (entry == null) {
            return null;
        }
        BufferedImage raw;
        try (InputStream in = jar.getInputStream(entry)) {
            raw = ImageIO.read(in);
        }
        if (raw == null) {
            return null;
        }
        int size = raw.getWidth();
        BufferedImage frame = raw.getSubimage(0, 0, size, Math.min(size, raw.getHeight()));
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(frame, 0, 0, 16, 16, null);
        g.dispose();
        return out;
    }

    private JsonObject json(String path) throws IOException {
        ZipEntry entry = jar.getEntry(path);
        if (entry == null) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        }
    }

    /** Teintes appliquees par le jeu a certaines textures grises. */
    private static BufferedImage tint(String id, BufferedImage image, boolean layer0) {
        int color;
        if (id.endsWith("_leaves") && !id.startsWith("cherry") && !id.startsWith("azalea") && !id.startsWith("flowering")
                && !id.startsWith("pale")) {
            color = id.startsWith("spruce") ? 0x619961 : id.startsWith("birch") ? 0x80A755 : 0x48B518;
        } else if (id.equals("lily_pad")) {
            color = 0x208030;
        } else if (layer0 && id.startsWith("leather_")) {
            color = 0xA06540;
        } else if (id.equals("vine") || id.equals("fern") || id.equals("short_grass") || id.equals("tall_grass")) {
            color = 0x48B518;
        } else {
            return image;
        }
        int tr = (color >> 16) & 0xFF;
        int tg = (color >> 8) & 0xFF;
        int tb = color & 0xFF;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int a = argb >>> 24;
                int r = ((argb >> 16) & 0xFF) * tr / 255;
                int g = ((argb >> 8) & 0xFF) * tg / 255;
                int b = (argb & 0xFF) * tb / 255;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }
}
