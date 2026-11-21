package fr.kalium.repeteurwifi;

import fr.kalium.blocs.EcouteurBloc;
import fr.kalium.blocs.KSBlocs;
import fr.kalium.blocs.TypeBloc;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les répéteurs des chunks chargés et la transmission.
 *
 * - Le mode vient du type KS_Blocs (émetteur, récepteur, récepteur allumé) ; le code est écrit dans les données du
 *   chunk (« ks_repeteurwifi:codes », « x,y,z:CODE » séparés par « ; », coordonnées dans le chunk).
 * - À chaque tick : les émetteurs alimentés (courant reçu d'un voisin, comme une lampe) sont relevés ; un récepteur
 *   veut être allumé si un émetteur alimenté du même code est à la portée ou moins, dans le même monde. Il change
 *   d'état au plus une fois tous les « intervalle » ticks ; un allumage demandé trop tôt (impulsion courte, horloge
 *   rapide) est gardé et fait dès que possible.
 * - Pistons : KS_Blocs prévient le répéteur à l'ancienne place (retiré) puis à la nouvelle (posé, 3 ticks plus
 *   tard) ; le code suit grâce aux déplacements relevés ici au même moment.
 */
final class Repeteurs implements EcouteurBloc {

    record Pos(UUID monde, int x, int y, int z) {
        static Pos de(Block b) {
            return new Pos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
        }

        Block bloc() {
            World w = Bukkit.getWorld(monde);
            return w == null ? null : w.getBlockAt(x, y, z);
        }
    }

    static final class Repeteur {
        final Pos pos;
        boolean emetteur;
        String code;
        boolean allume;
        /** Tick du dernier changement d'état (récepteur). */
        long dernier = Long.MIN_VALUE / 2;
        /** Allumage demandé pendant l'intervalle : fait dès qu'il est passé. */
        boolean enAttente;

        Repeteur(Pos pos) {
            this.pos = pos;
        }
    }

    /** Code retiré avec un répéteur (piston), gardé quelques ticks. */
    private record Orphelin(String code, long tick) {
    }

    private final KSRepeteurWifi plugin;
    private final NamespacedKey cleCodes;
    private final int portee;
    private final long porteeCarre;
    private final int intervalle;
    private final Map<Pos, Repeteur> index = new LinkedHashMap<>();
    /** Arrivée d'un répéteur poussé -> sa place de départ. */
    private final Map<Pos, Pos> arrivees = new HashMap<>();
    private final Map<Pos, Orphelin> orphelins = new HashMap<>();
    /** Vrai pendant un changement de type (mode, allumage) : KS_Blocs prévient retire puis pose, ce n'en est pas un. */
    private boolean enChangement;
    private long tick;

    Repeteurs(KSRepeteurWifi plugin, int portee, int intervalle) {
        this.plugin = plugin;
        this.cleCodes = new NamespacedKey(plugin, "codes");
        this.portee = portee;
        this.porteeCarre = (long) portee * portee;
        this.intervalle = intervalle;
    }

    int portee() {
        return portee;
    }

    Repeteur get(Block b) {
        return index.get(Pos.de(b));
    }

    // ------------------------------------------------------------------ écouteur KS_Blocs

    @Override
    public void pose(Block bloc, String variante) {
        if (enChangement) {
            return;
        }
        Pos p = Pos.de(bloc);
        Pos depart = arrivees.remove(p);
        Orphelin o = depart == null ? null : orphelins.remove(depart);
        if (o != null) {
            ecrireCode(bloc, o.code());
        }
        index.remove(p);
        suivre(bloc);
    }

    @Override
    public void retire(Block bloc, String variante) {
        if (enChangement) {
            return;
        }
        Pos p = Pos.de(bloc);
        Repeteur r = index.remove(p);
        String code = r != null ? r.code : lireCode(bloc);
        if (code != null) {
            orphelins.put(p, new Orphelin(code, tick));
        }
        ecrireCode(bloc, null);
    }

    @Override
    public void verifie(Block bloc, String variante) {
        if (!index.containsKey(Pos.de(bloc))) {
            suivre(bloc);
        }
    }

    /** Ajoute un répéteur à l'index, d'après son type et le code du chunk ; remet sa puissance d'accord avec son type. */
    private void suivre(Block bloc) {
        TypeBloc t = KSBlocs.type(bloc);
        if (t == null || bloc.getType() != Material.TARGET) {
            return;
        }
        Repeteur r = new Repeteur(Pos.de(bloc));
        r.emetteur = t.id().equals(KSRepeteurWifi.EMETTEUR);
        r.allume = t.id().equals(KSRepeteurWifi.RECEPTEUR_ALLUME);
        r.code = lireCode(bloc);
        index.put(r.pos, r);
        puissance(bloc, r.allume ? 15 : 0); // poussé par un piston, le bloc cible revient à 0
    }

    void pousses(List<Block> blocs, BlockFace sens) {
        for (Block b : blocs) {
            if (KSRepeteurWifi.estRepeteur(b)) {
                arrivees.put(Pos.de(b.getRelative(sens)), Pos.de(b));
            }
        }
    }

    void decharger(Chunk c) {
        UUID monde = c.getWorld().getUID();
        index.keySet().removeIf(p -> p.monde().equals(monde) && p.x() >> 4 == c.getX() && p.z() >> 4 == c.getZ());
    }

    // ------------------------------------------------------------------ réglages (menu)

    /** Passe en émission ou en réception (éteint). */
    void changerMode(Block b, boolean emetteur) {
        Repeteur r = get(b);
        if (r == null || r.emetteur == emetteur) {
            return;
        }
        r.emetteur = emetteur;
        r.allume = false;
        r.enAttente = false;
        r.dernier = Long.MIN_VALUE / 2;
        changerType(b, emetteur ? KSRepeteurWifi.EMETTEUR : KSRepeteurWifi.RECEPTEUR);
        puissance(b, 0);
    }

    void changerCode(Block b, String code) {
        Repeteur r = get(b);
        if (r == null) {
            return;
        }
        r.code = code;
        ecrireCode(b, code);
    }

    private void changerType(Block b, String id) {
        TypeBloc t = KSBlocs.type(id);
        if (t == null) {
            return;
        }
        enChangement = true;
        try {
            KSBlocs.retirer(b, false);
            KSBlocs.enregistrer(b, t, KSRepeteurWifi.VARIANTE);
        } finally {
            enChangement = false;
        }
    }

    private static void puissance(Block b, int niveau) {
        if (b.getBlockData() instanceof AnaloguePowerable a && a.getPower() != niveau) {
            a.setPower(niveau);
            b.setBlockData(a, true);
        }
    }

    // ------------------------------------------------------------------ transmission

    void chaqueTick() {
        tick++;
        if (!orphelins.isEmpty()) {
            orphelins.values().removeIf(o -> tick - o.tick() > 20);
            if (orphelins.isEmpty()) {
                arrivees.clear();
            }
        }
        if (index.isEmpty()) {
            return;
        }
        // 1.0.1 : déploiement en deux temps (KS_MondeTest) : rien ne transmet dans un monde où le plugin n'est pas ouvert
        Map<UUID, Boolean> ouverts = new HashMap<>();
        // émetteurs alimentés, par code
        Map<String, List<Pos>> actifs = new HashMap<>();
        List<Repeteur> recepteurs = new ArrayList<>();
        for (Iterator<Repeteur> it = index.values().iterator(); it.hasNext(); ) {
            Repeteur r = it.next();
            if (!r.emetteur) {
                recepteurs.add(r);
                continue;
            }
            if (r.code == null) {
                continue;
            }
            Block b = r.pos.bloc();
            if (b == null) {
                it.remove();
                continue;
            }
            if (b.isBlockIndirectlyPowered() && ouvert(ouverts, b.getWorld())) {
                actifs.computeIfAbsent(r.code, k -> new ArrayList<>()).add(r.pos);
            }
        }
        for (Repeteur r : recepteurs) {
            boolean voulu = r.code != null && aPortee(r.pos, actifs.get(r.code))
                    && Boolean.TRUE.equals(ouverts.get(r.pos.monde()));
            boolean libre = tick - r.dernier >= intervalle;
            if (r.allume) {
                r.enAttente = false;
                if (!voulu && libre) {
                    allumer(r, false);
                }
            } else if (voulu || r.enAttente) {
                if (libre) {
                    allumer(r, true);
                } else {
                    r.enAttente = true;
                }
            }
        }
    }

    /** Le plugin est-il ouvert dans ce monde (KS_MondeTest) ? Une fois par monde et par tick. */
    private static boolean ouvert(Map<UUID, Boolean> ouverts, World monde) {
        return ouverts.computeIfAbsent(monde.getUID(), u -> PhaseTest.ouvert("KS_RepeteurWifi", monde));
    }

    private boolean aPortee(Pos p, List<Pos> emetteurs) {
        if (emetteurs == null) {
            return false;
        }
        for (Pos e : emetteurs) {
            if (!e.monde().equals(p.monde())) {
                continue;
            }
            long dx = e.x() - p.x(), dy = e.y() - p.y(), dz = e.z() - p.z();
            if (dx * dx + dy * dy + dz * dz <= porteeCarre) {
                return true;
            }
        }
        return false;
    }

    private void allumer(Repeteur r, boolean allume) {
        Block b = r.pos.bloc();
        r.allume = allume;
        r.enAttente = false;
        r.dernier = tick;
        if (b == null || !KSRepeteurWifi.estRepeteur(b)) {
            return;
        }
        changerType(b, allume ? KSRepeteurWifi.RECEPTEUR_ALLUME : KSRepeteurWifi.RECEPTEUR);
        puissance(b, allume ? 15 : 0);
    }

    // ------------------------------------------------------------------ codes dans les données du chunk

    private static String cle(Block b) {
        return (b.getX() & 15) + "," + b.getY() + "," + (b.getZ() & 15);
    }

    private Map<String, String> codes(Chunk c) {
        Map<String, String> m = new LinkedHashMap<>();
        String brut = c.getPersistentDataContainer().get(cleCodes, PersistentDataType.STRING);
        if (brut != null && !brut.isEmpty()) {
            for (String e : brut.split(";")) {
                int i = e.indexOf(':');
                if (i > 0) {
                    m.put(e.substring(0, i), e.substring(i + 1));
                }
            }
        }
        return m;
    }

    String lireCode(Block b) {
        return codes(b.getChunk()).get(cle(b));
    }

    /** Écrit (ou efface, code null) le code de ce répéteur dans les données de son chunk. */
    private void ecrireCode(Block b, String code) {
        Chunk c = b.getChunk();
        Map<String, String> m = codes(c);
        boolean change = code == null ? m.remove(cle(b)) != null : !code.equals(m.put(cle(b), code));
        if (!change) {
            return;
        }
        PersistentDataContainer pdc = c.getPersistentDataContainer();
        if (m.isEmpty()) {
            pdc.remove(cleCodes);
            return;
        }
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (s.length() > 0) {
                s.append(';');
            }
            s.append(e.getKey()).append(':').append(e.getValue());
        }
        pdc.set(cleCodes, PersistentDataType.STRING, s.toString());
    }
}
