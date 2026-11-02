package fr.kalium.blocs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registre des états réservés aux blocs custom (apparences.json, dans le jar). Une apparence donne un état du bloc de
 * base par variante. Les blocs de base qu'aucune apparence n'utilise ne sont jamais réécrits.
 */
final class Apparences {

    /** Bloc de base -> état montré à tous les vrais blocs de ce type (un seul modèle pour tous leurs états). */
    static final Map<Block, BlockState> ETAT_DES_VRAIS = Map.of(
            Blocks.NOTE_BLOCK, Blocks.NOTE_BLOCK.defaultBlockState(),
            Blocks.SUGAR_CANE, Blocks.SUGAR_CANE.defaultBlockState(),
            Blocks.KELP, Blocks.KELP.defaultBlockState());

    /** Apparence -> variante -> état. */
    private final Map<String, Map<String, BlockState>> etats = new LinkedHashMap<>();
    /** Blocs de base utilisés par au moins une apparence : les seuls que la réécriture touche. */
    private final Set<Block> bases;

    Apparences(InputStream flux) {
        JsonObject racine = JsonParser.parseReader(new InputStreamReader(flux, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<BlockState, String> pris = new HashMap<>();
        java.util.HashSet<Block> b = new java.util.HashSet<>();
        for (Map.Entry<String, JsonElement> e : racine.entrySet()) {
            if (e.getKey().startsWith("_")) {
                continue;
            }
            JsonObject a = e.getValue().getAsJsonObject();
            String bloc = a.get("bloc").getAsString();
            Map<String, BlockState> variantes = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> v : a.getAsJsonObject("variantes").entrySet()) {
                String texte = "minecraft:" + bloc + "[" + v.getValue().getAsString() + "]";
                BlockState etat = ((CraftBlockData) Bukkit.createBlockData(texte)).getState();
                if (!ETAT_DES_VRAIS.containsKey(etat.getBlock())) {
                    throw new IllegalArgumentException(texte + " : bloc de base non pris en charge");
                }
                if (ETAT_DES_VRAIS.get(etat.getBlock()) == etat) {
                    throw new IllegalArgumentException(texte + " : etat des vrais blocs, interdit");
                }
                String deja = pris.put(etat, e.getKey() + "/" + v.getKey());
                if (deja != null) {
                    throw new IllegalArgumentException(texte + " : deja pris par " + deja);
                }
                variantes.put(v.getKey(), etat);
                b.add(etat.getBlock());
            }
            etats.put(e.getKey(), variantes);
        }
        bases = Set.copyOf(b);
    }

    /** L'état d'une variante, ou null si l'apparence ou la variante n'existe pas. */
    BlockState etat(String apparence, String variante) {
        Map<String, BlockState> v = etats.get(apparence);
        return v == null ? null : v.get(variante);
    }

    Set<String> variantes(String apparence) {
        Map<String, BlockState> v = etats.get(apparence);
        return v == null ? Set.of() : v.keySet();
    }

    boolean existe(String apparence) {
        return etats.containsKey(apparence);
    }

    Set<Block> bases() {
        return bases;
    }

    int nombre() {
        return etats.size();
    }
}
