package fr.kalium.bingo.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * Icones des objets pour la carte de la grille (0.4.0). Au premier demarrage (ou a un changement de version du
 * jeu), telecharge le jeu officiel chez Mojang (piston-meta.mojang.com, ~40 Mo), en extrait une icone 16x16 par
 * objet (IconResolver) dans plugins/KG_BingoGame/icons/&lt;version&gt;/, puis supprime le telechargement. Les
 * demarrages suivants ne font que relire ces images. Tout se fait hors du fil principal du serveur ; tant que les
 * icones ne sont pas pretes, la carte affiche un "?" a leur place.
 */
public final class IconLibrary {

    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<Material, BufferedImage> icons = new ConcurrentHashMap<>();
    private volatile boolean ready;
    /** Incremente quand les icones deviennent disponibles (la carte se redessine). */
    private volatile int generation;

    public IconLibrary(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public BufferedImage icon(Material material) {
        return icons.get(material);
    }

    public boolean isReady() {
        return ready;
    }

    public int generation() {
        return generation;
    }

    /** Lance le chargement (ou la preparation) des icones en arriere-plan. */
    public void loadAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                load();
            } catch (Exception e) {
                logger.warning("[KG_BingoGame] Icones de la carte indisponibles : " + e.getMessage()
                        + " (la carte affichera \"?\" à la place des objets).");
            }
        });
    }

    private void load() throws IOException, InterruptedException {
        String version = Bukkit.getMinecraftVersion();
        // 0.8.1 : « -3d » = icones refaites avec le rendu 3D des blocs (les anciennes, dans icons/<version>/, ne sont
        // plus lues).
        Path dir = plugin.getDataFolder().toPath().resolve("icons").resolve(version + "-3d");
        Path done = dir.resolve(".complet");
        if (!Files.exists(done)) {
            prepare(version, dir);
            Files.writeString(done, "ok");
        }
        int count = 0;
        try (var files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".png")) {
                    continue;
                }
                Material material = Material.matchMaterial(name.substring(0, name.length() - 4));
                BufferedImage image = ImageIO.read(file.toFile());
                if (material != null && image != null) {
                    icons.put(material, image);
                    count++;
                }
            }
        }
        ready = true;
        generation++;
        logger.info("[KG_BingoGame] " + count + " icône(s) d'objets chargée(s) pour la carte.");
    }

    /** Telecharge le jeu de cette version et en extrait une icone par objet. */
    private void prepare(String version, Path dir) throws IOException, InterruptedException {
        logger.info("[KG_BingoGame] Préparation des icônes de la carte (téléchargement du jeu " + version
                + " chez Mojang, une seule fois)...");
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        JsonObject manifest = getJson(http, MANIFEST);
        String versionUrl = null;
        for (var element : manifest.getAsJsonArray("versions")) {
            JsonObject entry = element.getAsJsonObject();
            if (version.equals(entry.get("id").getAsString())) {
                versionUrl = entry.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("version " + version + " introuvable chez Mojang");
        }
        String clientUrl = getJson(http, versionUrl).getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
        Files.createDirectories(dir);
        Path jar = dir.resolve("client.jar.tmp");
        HttpResponse<Path> response = http.send(HttpRequest.newBuilder(URI.create(clientUrl)).timeout(Duration.ofMinutes(10)).build(),
                HttpResponse.BodyHandlers.ofFile(jar));
        if (response.statusCode() != 200) {
            throw new IOException("téléchargement du jeu refusé (code " + response.statusCode() + ")");
        }
        int ok = 0;
        int missing = 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            IconResolver resolver = new IconResolver(zip);
            for (Material material : Material.values()) {
                if (material.isLegacy() || !material.isItem() || material.isAir()) {
                    continue;
                }
                String id = material.getKey().getKey();
                BufferedImage image;
                try {
                    image = resolver.resolve(id);
                } catch (RuntimeException e) {
                    image = null;
                }
                if (image == null) {
                    missing++;
                    continue;
                }
                ImageIO.write(image, "png", new File(dir.toFile(), id + ".png"));
                ok++;
            }
        } finally {
            Files.deleteIfExists(jar);
        }
        logger.info("[KG_BingoGame] Icônes préparées : " + ok + " (sans icône : " + missing + ").");
    }

    private static JsonObject getJson(HttpClient http, String url) throws IOException, InterruptedException {
        HttpResponse<InputStream> response = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("réponse " + response.statusCode() + " pour " + url);
        }
        try (InputStreamReader reader = new InputStreamReader(response.body(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
