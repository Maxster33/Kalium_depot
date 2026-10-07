import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Construit un datapack "KaLium Terralith" : seulement quelques biomes de Terralith, sans aucune structure.
 * args : <dossier Terralith dezippe> <overworld.json vanilla (rapport biome_parameters)> <dossier de sortie>
 *        <biome1,biome2,...>
 */
public class BuildPack {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    static final String[] PARAMS = {"temperature", "humidity", "continentalness", "erosion", "weirdness", "depth"};
    // Registres jamais copies : structures et presets de monde.
    static final Set<String> EXCLUDED = Set.of("structure", "structure_set", "template_pool", "processor_list",
            "world_preset", "multi_noise_biome_source_parameter_list", "noise_settings");
    static final Pattern REF = Pattern.compile("^#?terralith:[a-z0-9_/.\\-]+$");

    static Path ter, out;
    static Set<String> keep;
    static Set<String> copied = new TreeSet<>();
    static Deque<String> todo = new ArrayDeque<>();
    static Set<String> seen = new HashSet<>();
    static List<String> missing = new ArrayList<>();

    public static void main(String[] a) throws Exception {
        ter = Path.of(a[0]);
        Path vanillaFile = Path.of(a[1]);
        out = Path.of(a[2]);
        keep = new TreeSet<>();
        for (String b : a[3].split(",")) keep.add("terralith:" + b.trim());

        for (String b : keep)
            if (!Files.exists(ter.resolve("data/terralith/worldgen/biome/" + b.substring(10) + ".json")))
                throw new IllegalArgumentException("Biome introuvable : " + b);

        // 1. overworld.json : biomes non retenus -> biome vanilla au meme point climatique
        JsonArray vanilla = read(vanillaFile).getAsJsonObject().getAsJsonArray("biomes");
        JsonObject dim = read(ter.resolve("data/minecraft/dimension/overworld.json")).getAsJsonObject();
        dim.remove("fabric:load_conditions");
        dim.remove("neoforge:conditions");
        JsonArray entries = dim.getAsJsonObject("generator").getAsJsonObject("biome_source").getAsJsonArray("biomes");
        Map<String, Map<String, Integer>> remap = new TreeMap<>();
        Map<String, Integer> keptCount = new TreeMap<>();
        for (Iterator<JsonElement> it = entries.iterator(); it.hasNext(); ) {
            JsonObject o = it.next().getAsJsonObject();
            String biome = o.get("biome").getAsString();
            if (!biome.startsWith("terralith:")) continue;
            if (keep.contains(biome)) { keptCount.merge(biome, 1, Integer::sum); continue; }
            // Grottes de Terralith : entree retiree, le sous-sol reprend le comportement vanilla
            if (biome.startsWith("terralith:cave/")) {
                it.remove();
                remap.computeIfAbsent(biome, k -> new TreeMap<>()).merge("(entree retiree)", 1, Integer::sum);
                continue;
            }
            String repl = nearestVanilla(o.getAsJsonObject("parameters"), vanilla);
            o.addProperty("biome", repl);
            remap.computeIfAbsent(biome, k -> new TreeMap<>()).merge(repl, 1, Integer::sum);
        }
        write("data/minecraft/dimension/overworld.json", dim);

        // 2. Regles de surface de Terralith, listes de biomes nettoyees
        Path surfaceDir = ter.resolve("data/minecraft/worldgen/material_rule");
        try (var s = Files.walk(surfaceDir)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) {
                String rel = ter.relativize(p).toString().replace('\\', '/');
                JsonElement surface = pruneBiomes(read(p));
                write(rel, surface);
                copied.add(rel);
                scan(surface);
            }
        }

        // 3. Biomes retenus + tout ce qu'ils referencent
        for (String b : keep) todo.add(b);
        while (!todo.isEmpty()) resolve(todo.poll());

        // 4. Tags de biomes (minecraft et terralith) filtres sur les biomes retenus
        for (String ns : List.of("minecraft", "terralith")) {
            Path tagDir = ter.resolve("data/" + ns + "/tags/worldgen/biome");
            if (!Files.isDirectory(tagDir)) continue;
            try (var s = Files.walk(tagDir)) {
                for (Path p : s.filter(Files::isRegularFile).toList()) {
                    String rel = ter.relativize(p).toString().replace('\\', '/');
                    if (ns.equals("terralith") && rel.contains("/has_structure/")) continue;
                    JsonObject tag = read(p).getAsJsonObject();
                    JsonArray kept = new JsonArray();
                    for (JsonElement v : tag.getAsJsonArray("values")) {
                        String id = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
                        if (keep.contains(id)) kept.add(v);
                        else if (id.startsWith("#terralith:") && !id.contains("has_structure")) { kept.add(v); todo.add(id); }
                    }
                    if (kept.isEmpty()) continue;
                    JsonObject t = new JsonObject();
                    t.add("values", kept);
                    write(rel, t);
                }
            }
        }

        while (!todo.isEmpty()) resolve(todo.poll());

        JsonObject meta = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("min_format", 121);
        pack.addProperty("max_format", 121);
        pack.addProperty("description", "KaLium : biomes choisis de Terralith 2.6.5, sans structures");
        meta.add("pack", pack);
        write("pack.mcmeta", meta);

        // Rapport
        System.out.println("== Entrees conservees par biome (overworld.json)");
        keptCount.forEach((k, v) -> System.out.println("  " + k + " : " + v));
        for (String b : keep) if (!keptCount.containsKey(b)) System.out.println("  !! " + b + " absent de overworld.json");
        System.out.println("== Remplacements (biome Terralith -> biome vanilla : nombre d'entrees)");
        remap.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        System.out.println("== Fichiers copies : " + copied.size());
        copied.forEach(c -> System.out.println("  " + c));
        if (!missing.isEmpty()) { System.out.println("== REFERENCES INTROUVABLES"); missing.forEach(m -> System.out.println("  " + m)); }
    }

    static String nearestVanilla(JsonObject p, JsonArray vanilla) {
        double[] pt = new double[PARAMS.length];
        for (int i = 0; i < PARAMS.length; i++) pt[i] = center(p.get(PARAMS[i]));
        String best = null;
        double bestFit = Double.MAX_VALUE;
        for (JsonElement e : vanilla) {
            JsonObject v = e.getAsJsonObject();
            JsonObject vp = v.getAsJsonObject("parameters");
            double fit = 0;
            for (int i = 0; i < PARAMS.length; i++) {
                double[] r = range(vp.get(PARAMS[i]));
                double d = pt[i] < r[0] ? r[0] - pt[i] : (pt[i] > r[1] ? pt[i] - r[1] : 0);
                fit += d * d;
            }
            double off = vp.has("offset") ? vp.get("offset").getAsDouble() : 0;
            fit += off * off;
            if (fit < bestFit) { bestFit = fit; best = v.get("biome").getAsString(); }
        }
        return best;
    }

    static double center(JsonElement e) { double[] r = range(e); return (r[0] + r[1]) / 2; }

    static double[] range(JsonElement e) {
        if (e == null) return new double[]{0, 0};
        if (e.isJsonPrimitive()) { double d = e.getAsDouble(); return new double[]{d, d}; }
        if (e.isJsonArray()) { JsonArray a = e.getAsJsonArray(); return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble()}; }
        JsonObject o = e.getAsJsonObject();
        return new double[]{o.get("min").getAsDouble(), o.get("max").getAsDouble()};
    }

    /** Retire les biomes Terralith non retenus des listes de biomes ("biome_is"). */
    static JsonElement pruneBiomes(JsonElement e) {
        if (e.isJsonArray()) {
            JsonArray r = new JsonArray();
            for (JsonElement x : e.getAsJsonArray()) r.add(pruneBiomes(x));
            return r;
        }
        if (!e.isJsonObject()) return e;
        JsonObject o = e.getAsJsonObject();
        JsonObject r = new JsonObject();
        for (var en : o.entrySet()) {
            if (en.getKey().equals("biome_is")) {
                JsonElement list = en.getValue();
                JsonArray kept = new JsonArray();
                Iterable<JsonElement> items = list.isJsonArray() ? list.getAsJsonArray() : List.of(list);
                for (JsonElement b : items) {
                    String id = b.getAsString();
                    if (!id.startsWith("terralith:") || keep.contains(id)) kept.add(b);
                }
                r.add("biome_is", kept);
            } else r.add(en.getKey(), pruneBiomes(en.getValue()));
        }
        return r;
    }

    static void copyMinecraftMaterialRules(JsonElement e) throws IOException {
        List<String> refs = new ArrayList<>();
        collect(e, refs, Pattern.compile("^minecraft:[a-z0-9_/.\\-]+$"));
        for (String id : refs) {
            Path p = ter.resolve("data/minecraft/worldgen/material_rule/" + id.substring(10) + ".json");
            if (Files.exists(p) && copied.add(ter.relativize(p).toString().replace('\\', '/'))) {
                JsonElement j = pruneBiomes(read(p));
                write(ter.relativize(p).toString().replace('\\', '/'), j);
                scan(j);
                copyMinecraftMaterialRules(j);
            }
        }
    }

    static void resolve(String ref) throws IOException {
        if (!seen.add(ref)) return;
        boolean tag = ref.startsWith("#");
        String id = tag ? ref.substring(1) : ref;
        String path = id.substring(id.indexOf(':') + 1);
        boolean found = false;
        Path base = ter.resolve(tag ? "data/terralith/tags" : "data/terralith/worldgen");
        if (Files.isDirectory(base)) {
            try (var s = Files.walk(base)) {
                for (Path p : s.filter(Files::isRegularFile).toList()) {
                    String rel = base.relativize(p).toString().replace('\\', '/');
                    // rel = <registre>/<chemin>.json ou worldgen/<registre>/<chemin>.json pour les tags
                    if (!rel.endsWith("/" + path + ".json")) continue;
                    String registry = rel.substring(0, rel.length() - path.length() - 6);
                    if (registry.contains("/") && !tag) continue;
                    String regName = registry.substring(registry.lastIndexOf('/') + 1);
                    if (EXCLUDED.contains(regName)) continue;
                    if (!tag && regName.equals("biome") && !keep.contains(id)) continue;
                    String outRel = ter.relativize(p).toString().replace('\\', '/');
                    JsonElement j = pruneBiomes(read(p));
                    if (tag && regName.equals("biome")) {
                        JsonArray kept = new JsonArray();
                        for (JsonElement v : j.getAsJsonObject().getAsJsonArray("values")) {
                            String vid = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
                            if (!vid.startsWith("terralith:") || keep.contains(vid) || vid.startsWith("#")) kept.add(v);
                        }
                        j.getAsJsonObject().add("values", kept);
                    }
                    found = true;
                    if (copied.add(outRel)) { write(outRel, j); scan(j); }
                }
            }
        }
        if (!found) missing.add(ref);
    }

    static void scan(JsonElement e) {
        List<String> refs = new ArrayList<>();
        collect(e, refs, REF);
        for (String r : refs) if (!seen.contains(r)) todo.add(r);
    }

    static void collect(JsonElement e, List<String> refs, Pattern pat) {
        if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) collect(x, refs, pat);
        else if (e.isJsonObject()) for (var en : e.getAsJsonObject().entrySet()) collect(en.getValue(), refs, pat);
        else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            String s = e.getAsString();
            if (pat.matcher(s).matches()) refs.add(s);
        }
    }

    static JsonElement read(Path p) throws IOException {
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) { return JsonParser.parseReader(r); }
    }

    static void write(String rel, JsonElement j) throws IOException {
        Path p = out.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, GSON.toJson(j), StandardCharsets.UTF_8);
    }
}
