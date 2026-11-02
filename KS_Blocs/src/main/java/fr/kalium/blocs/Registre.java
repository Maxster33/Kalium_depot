package fr.kalium.blocs;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Position de chaque bloc custom, par monde et par chunk, gardée dans les données du chunk (clé ks_blocs:blocs :
 * « x,y,z,type,variante » séparés par « ; », x et z dans le chunk). En mémoire seulement pour les chunks chargés.
 *
 * Modifié sur le fil principal, lu aussi par les connexions des joueurs (réécriture des paquets) : tables concurrentes.
 */
final class Registre {

    record Entree(String type, String variante) {
    }

    private final Map<UUID, Map<Long, Map<Integer, Entree>>> mondes = new ConcurrentHashMap<>();
    private final NamespacedKey cle;

    Registre(NamespacedKey cle) {
        this.cle = cle;
    }

    static long cleChunk(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Position dans le chunk : x et z de 0 à 15, y du monde (-2048 à 2047). */
    static int clePos(int x, int y, int z) {
        return ((y + 2048) << 8) | ((z & 15) << 4) | (x & 15);
    }

    static int x(int clePos) {
        return clePos & 15;
    }

    static int z(int clePos) {
        return (clePos >> 4) & 15;
    }

    static int y(int clePos) {
        return (clePos >>> 8) - 2048;
    }

    /** Les blocs custom d'un chunk chargé (lecture seule), ou null s'il n'y en a pas. */
    Map<Integer, Entree> chunk(UUID monde, int cx, int cz) {
        Map<Long, Map<Integer, Entree>> m = mondes.get(monde);
        return m == null ? null : m.get(cleChunk(cx, cz));
    }

    Entree get(UUID monde, int x, int y, int z) {
        Map<Integer, Entree> c = chunk(monde, x >> 4, z >> 4);
        return c == null ? null : c.get(clePos(x, y, z));
    }

    Entree get(Block b) {
        return get(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
    }

    /** Lit les données d'un chunk qui vient d'être chargé ; renvoie ses blocs custom (null s'il n'y en a pas). */
    Map<Integer, Entree> charger(Chunk chunk) {
        String texte = chunk.getPersistentDataContainer().get(cle, PersistentDataType.STRING);
        Map<Long, Map<Integer, Entree>> m = mondes.computeIfAbsent(chunk.getWorld().getUID(), k -> new ConcurrentHashMap<>());
        long k = cleChunk(chunk.getX(), chunk.getZ());
        if (texte == null || texte.isEmpty()) {
            m.remove(k);
            return null;
        }
        Map<Integer, Entree> c = new ConcurrentHashMap<>();
        for (String ligne : texte.split(";")) {
            String[] p = ligne.split(",", 5);
            if (p.length == 5) {
                try {
                    c.put(clePos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])),
                            new Entree(p[3], p[4]));
                } catch (NumberFormatException ignore) {
                    // ligne abîmée : ignorée
                }
            }
        }
        m.put(k, c);
        return c;
    }

    void decharger(Chunk chunk) {
        Map<Long, Map<Integer, Entree>> m = mondes.get(chunk.getWorld().getUID());
        if (m != null) {
            m.remove(cleChunk(chunk.getX(), chunk.getZ()));
        }
    }

    void oublierMonde(UUID monde) {
        mondes.remove(monde);
    }

    void mettre(Block b, Entree e) {
        Map<Long, Map<Integer, Entree>> m = mondes.computeIfAbsent(b.getWorld().getUID(), k -> new ConcurrentHashMap<>());
        Map<Integer, Entree> c = m.computeIfAbsent(cleChunk(b.getX() >> 4, b.getZ() >> 4), k -> new ConcurrentHashMap<>());
        c.put(clePos(b.getX(), b.getY(), b.getZ()), e);
        enregistrer(b.getChunk(), c);
    }

    Entree enlever(Block b) {
        Map<Integer, Entree> c = chunk(b.getWorld().getUID(), b.getX() >> 4, b.getZ() >> 4);
        if (c == null) {
            return null;
        }
        Entree e = c.remove(clePos(b.getX(), b.getY(), b.getZ()));
        if (e != null) {
            enregistrer(b.getChunk(), c);
        }
        return e;
    }

    private void enregistrer(Chunk chunk, Map<Integer, Entree> c) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (c.isEmpty()) {
            pdc.remove(cle);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Entree> e : c.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            int p = e.getKey();
            sb.append(x(p)).append(',').append(y(p)).append(',').append(z(p)).append(',')
                    .append(e.getValue().type()).append(',').append(e.getValue().variante());
        }
        pdc.set(cle, PersistentDataType.STRING, sb.toString());
    }
}
