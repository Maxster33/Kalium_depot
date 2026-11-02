package fr.kalium.blocs;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * L'état qu'un joueur voit à une position : celui du bloc custom s'il y en a un, l'état unique des vrais blocs pour
 * un bloc de base (bloc musical...), sinon l'état du serveur. Appelé par les connexions des joueurs (hors du fil
 * principal) : rien ici ne touche au monde.
 */
final class Affichage {

    record Pos(UUID monde, int x, int y, int z) {
    }

    /** Un état retenu quelques secondes (particules de casse après le retrait, note jouée). */
    private record Retenu<T>(T valeur, long jusqua) {
    }

    private static final long DUREE_MS = 3000;

    private final Registre registre;
    private final Apparences apparences;
    /** id du type -> type, et bloc du serveur de sa base. */
    final Map<String, TypeBloc> types = new ConcurrentHashMap<>();
    private final Map<String, Block> basesServeur = new ConcurrentHashMap<>();
    /** Blocs custom dont le bloc du serveur a changé sans passer par KS_Blocs : vérifiés sur le fil principal. */
    final Queue<Pos> aVerifier = new ConcurrentLinkedQueue<>();
    private final Map<Pos, Boolean> enAttente = new ConcurrentHashMap<>();
    private final Map<Pos, Retenu<BlockState>> retires = new ConcurrentHashMap<>();
    /** Note (0-24) d'un vrai bloc musical qui vient de jouer, -1 pour un instrument sans note (tête...). */
    private final Map<Pos, Retenu<Integer>> notes = new ConcurrentHashMap<>();

    Affichage(Registre registre, Apparences apparences) {
        this.registre = registre;
        this.apparences = apparences;
    }

    void declarer(TypeBloc type, Block baseServeur) {
        types.put(type.id(), type);
        basesServeur.put(type.id(), baseServeur);
    }

    Apparences apparences() {
        return apparences;
    }

    Map<Integer, Registre.Entree> registreChunk(UUID monde, int cx, int cz) {
        return registre.chunk(monde, cx, cz);
    }

    /** L'état montré d'un bloc custom, ou null si le type n'est pas déclaré ou la variante inconnue. */
    BlockState etatCustom(String type, String variante) {
        TypeBloc t = types.get(type);
        return t == null ? null : apparences.etat(t.apparence(), variante);
    }

    /** Ce que voit un joueur à cette position, quand le serveur y a cet état. */
    BlockState affiche(UUID monde, int x, int y, int z, BlockState serveur) {
        Registre.Entree e = registre.get(monde, x, y, z);
        if (e != null) {
            BlockState custom = custom(monde, x, y, z, e, serveur);
            if (custom != null) {
                return custom;
            }
        }
        return vrai(serveur);
    }

    /** L'état d'un bloc custom si le serveur a bien sa base à cette place, sinon null. */
    BlockState custom(UUID monde, int x, int y, int z, Registre.Entree e, BlockState serveur) {
        Block base = basesServeur.get(e.type());
        if (base == null) {
            return null; // plugin du type absent : on garde la position sans rien montrer
        }
        if (serveur.getBlock() != base) {
            if (!serveur.is(Blocks.MOVING_PISTON)) {
                Pos p = new Pos(monde, x, y, z);
                if (enAttente.putIfAbsent(p, Boolean.TRUE) == null) {
                    aVerifier.add(p);
                }
            }
            return null;
        }
        return etatCustom(e.type(), e.variante());
    }

    void verifie(Pos p) {
        enAttente.remove(p);
    }

    /** L'état montré à un vrai bloc : l'état unique de son type si c'est une base utilisée, sinon lui-même. */
    BlockState vrai(BlockState serveur) {
        if (apparences.bases().contains(serveur.getBlock())) {
            return Apparences.ETAT_DES_VRAIS.get(serveur.getBlock());
        }
        return serveur;
    }

    // ------------------------------------------------------------------ particules de casse, notes

    void retire(Pos p, BlockState montre) {
        retires.put(p, new Retenu<>(montre, System.currentTimeMillis() + DUREE_MS));
    }

    /** L'état à utiliser pour les particules de casse d'un bloc (paquet envoyé juste après le retrait). */
    BlockState particules(UUID monde, int x, int y, int z, BlockState serveur) {
        Retenu<BlockState> r = retires.get(new Pos(monde, x, y, z));
        if (r != null && r.jusqua() > System.currentTimeMillis()) {
            return r.valeur();
        }
        return affiche(monde, x, y, z, serveur);
    }

    void note(Pos p, int note) {
        notes.put(p, new Retenu<>(note, System.currentTimeMillis() + DUREE_MS));
    }

    /** La note jouée par un vrai bloc musical à cette place, ou null si inconnue. */
    Integer note(UUID monde, int x, int y, int z) {
        Retenu<Integer> r = notes.get(new Pos(monde, x, y, z));
        return r == null || r.jusqua() < System.currentTimeMillis() ? null : r.valeur();
    }

    /** Oublie les états retenus trop vieux (appelé chaque seconde). */
    void nettoyer() {
        long maintenant = System.currentTimeMillis();
        retires.values().removeIf(r -> r.jusqua() < maintenant);
        notes.values().removeIf(r -> r.jusqua() < maintenant);
    }
}
