package fr.kalium.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 1.0.1 (LeKiwi06 : « pourquoi je dois attendre que les joueurs se soient connectés pour consulter leur
 * inventaire ? ») : lecture du fichier de sauvegarde d'un joueur hors ligne (world/players/data/<uuid>.dat, format NBT
 * compressé), pour invsee / ecsee même s'il n'est pas revenu depuis l'installation de l'anti-triche. Lecture seule :
 * les changements du staff restent appliqués à sa prochaine connexion (écrire dans le fichier d'un joueur risquerait
 * d'abîmer sa sauvegarde).
 *
 * Chaque objet est reconstruit par Paper (ItemStack.deserializeBytes : l'objet tel qu'enregistré + la version des
 * données du fichier, mises à jour par Paper si besoin).
 */
final class Sauvegardes {

    /** Une balise NBT : type et valeur (compound : Map, liste : Liste). */
    private record Tag(byte type, Object valeur) {
    }

    private record Liste(byte type, List<Tag> elements) {
    }

    /** Contenu lu : inventaire au format de PlayerInventory#getContents (41 cases) et coffre de l'Ender (27). */
    record Contenu(ItemStack[] inventaire, ItemStack[] ender) {
    }

    private Sauvegardes() {
    }

    /** Fichier de sauvegarde du joueur, ou null s'il n'existe pas. */
    static File fichier(UUID joueur) {
        List<File> candidats = new ArrayList<>();
        World monde = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (monde != null) {
            candidats.add(new File(monde.getWorldFolder(), "players/data"));
            candidats.add(new File(monde.getWorldFolder(), "playerdata"));
            candidats.add(new File(Bukkit.getWorldContainer(), monde.getName() + "/players/data"));
            candidats.add(new File(Bukkit.getWorldContainer(), monde.getName() + "/playerdata"));
        }
        for (File dossier : candidats) {
            File f = new File(dossier, joueur + ".dat");
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    /** Lit l'inventaire et le coffre de l'Ender d'un joueur hors ligne ; null si pas de fichier ou illisible. */
    @SuppressWarnings("unchecked")
    static Contenu lire(UUID joueur) throws IOException {
        File f = fichier(joueur);
        if (f == null) {
            return null;
        }
        Map<String, Tag> racine;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(f)))) {
            byte type = in.readByte();
            in.readUTF();
            if (type != 10) {
                return null;
            }
            racine = (Map<String, Tag>) lireValeur(in, type).valeur();
        }
        Tag version = racine.get("DataVersion");
        int dataVersion = version == null ? 0 : ((Number) version.valeur()).intValue();
        ItemStack[] inventaire = new ItemStack[41];
        ItemStack[] ender = new ItemStack[27];
        remplir(racine.get("Inventory"), inventaire, 36, dataVersion);
        remplir(racine.get("EnderItems"), ender, 27, dataVersion);
        Tag equipement = racine.get("equipment");
        if (equipement != null && equipement.valeur() instanceof Map<?, ?> eq) {
            String[] cles = {"feet", "legs", "chest", "head", "offhand"};
            for (int i = 0; i < cles.length; i++) {
                Object t = eq.get(cles[i]);
                if (t instanceof Tag tag && tag.valeur() instanceof Map<?, ?> objet) {
                    inventaire[36 + i] = objet((Map<String, Tag>) objet, dataVersion);
                }
            }
        }
        return new Contenu(inventaire, ender);
    }

    @SuppressWarnings("unchecked")
    private static void remplir(Tag liste, ItemStack[] cible, int cases, int dataVersion) {
        if (liste == null || !(liste.valeur() instanceof Liste l)) {
            return;
        }
        for (Tag t : l.elements()) {
            if (!(t.valeur() instanceof Map<?, ?> m)) {
                continue;
            }
            Map<String, Tag> objet = new LinkedHashMap<>((Map<String, Tag>) m);
            Tag slot = objet.remove("Slot");
            int s = slot == null ? -1 : ((Number) slot.valeur()).intValue();
            if (s >= 0 && s < cases) {
                cible[s] = objet(objet, dataVersion);
            }
        }
    }

    /** Un objet NBT -> ItemStack (Paper), ou null s'il est illisible. */
    private static ItemStack objet(Map<String, Tag> objet, int dataVersion) {
        Map<String, Tag> copie = new LinkedHashMap<>(objet);
        copie.remove("Slot");
        copie.put("DataVersion", new Tag((byte) 3, dataVersion));
        RuntimeException erreur = null;
        for (boolean compresse : new boolean[]{true, false}) {
            try {
                ItemStack item = ItemStack.deserializeBytes(octets(copie, compresse));
                return item.getType().isAir() ? null : item;
            } catch (IOException e) {
                erreur = new RuntimeException(e);
            } catch (RuntimeException e) {
                // Paper attend normalement des données compressées (comme serializeAsBytes) ; sinon, sans compression.
                erreur = e;
            }
        }
        Bukkit.getLogger().warning("[KS_AntiCheat] Objet illisible dans une sauvegarde : " + erreur);
        return null;
    }

    private static byte[] octets(Map<String, Tag> objet, boolean compresse) throws IOException {
        ByteArrayOutputStream octets = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(compresse ? new GZIPOutputStream(octets) : octets)) {
            out.writeByte(10);
            out.writeUTF("");
            ecrireValeur(out, new Tag((byte) 10, objet));
        }
        return octets.toByteArray();
    }

    // ------------------------------------------------------------------ NBT (lecture / écriture)

    private static Tag lireValeur(DataInputStream in, byte type) throws IOException {
        return switch (type) {
            case 1 -> new Tag(type, in.readByte());
            case 2 -> new Tag(type, in.readShort());
            case 3 -> new Tag(type, in.readInt());
            case 4 -> new Tag(type, in.readLong());
            case 5 -> new Tag(type, in.readFloat());
            case 6 -> new Tag(type, in.readDouble());
            case 7 -> {
                byte[] b = new byte[in.readInt()];
                in.readFully(b);
                yield new Tag(type, b);
            }
            case 8 -> new Tag(type, lireTexte(in));
            case 9 -> {
                byte et = in.readByte();
                int n = in.readInt();
                List<Tag> elements = new ArrayList<>(Math.max(0, n));
                for (int i = 0; i < n; i++) {
                    elements.add(lireValeur(in, et));
                }
                yield new Tag(type, new Liste(et, elements));
            }
            case 10 -> {
                Map<String, Tag> m = new LinkedHashMap<>();
                while (true) {
                    byte t = in.readByte();
                    if (t == 0) {
                        break;
                    }
                    String nom = lireTexte(in);
                    m.put(nom, lireValeur(in, t));
                }
                yield new Tag(type, m);
            }
            case 11 -> {
                int[] v = new int[in.readInt()];
                for (int i = 0; i < v.length; i++) {
                    v[i] = in.readInt();
                }
                yield new Tag(type, v);
            }
            case 12 -> {
                long[] v = new long[in.readInt()];
                for (int i = 0; i < v.length; i++) {
                    v[i] = in.readLong();
                }
                yield new Tag(type, v);
            }
            default -> throw new IOException("Type NBT inconnu : " + type);
        };
    }

    /** Texte NBT : longueur sur 2 octets + UTF-8 modifié (comme DataInput#readUTF). */
    private static String lireTexte(DataInputStream in) throws IOException {
        return in.readUTF();
    }

    @SuppressWarnings("unchecked")
    private static void ecrireValeur(DataOutputStream out, Tag tag) throws IOException {
        switch (tag.type()) {
            case 1 -> out.writeByte((Byte) tag.valeur());
            case 2 -> out.writeShort((Short) tag.valeur());
            case 3 -> out.writeInt(((Number) tag.valeur()).intValue());
            case 4 -> out.writeLong((Long) tag.valeur());
            case 5 -> out.writeFloat((Float) tag.valeur());
            case 6 -> out.writeDouble((Double) tag.valeur());
            case 7 -> {
                byte[] b = (byte[]) tag.valeur();
                out.writeInt(b.length);
                out.write(b);
            }
            case 8 -> out.writeUTF((String) tag.valeur());
            case 9 -> {
                Liste l = (Liste) tag.valeur();
                out.writeByte(l.elements().isEmpty() ? 0 : l.type());
                out.writeInt(l.elements().size());
                for (Tag e : l.elements()) {
                    ecrireValeur(out, e);
                }
            }
            case 10 -> {
                for (Map.Entry<String, Tag> e : ((Map<String, Tag>) tag.valeur()).entrySet()) {
                    out.writeByte(e.getValue().type());
                    out.writeUTF(e.getKey());
                    ecrireValeur(out, e.getValue());
                }
                out.writeByte(0);
            }
            case 11 -> {
                int[] v = (int[]) tag.valeur();
                out.writeInt(v.length);
                for (int x : v) {
                    out.writeInt(x);
                }
            }
            case 12 -> {
                long[] v = (long[]) tag.valeur();
                out.writeInt(v.length);
                for (long x : v) {
                    out.writeLong(x);
                }
            }
            default -> throw new IOException("Type NBT inconnu : " + tag.type());
        }
    }
}
