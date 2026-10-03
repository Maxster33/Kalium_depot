package fr.kalium.hub;

import java.nio.charset.StandardCharsets;

/**
 * Generateur de QR code (norme ISO/IEC 18004), ecrit pour KLM_Hub afin de n'embarquer aucune bibliotheque : mode
 * octets, versions 1 a 10 (jusqu'a 271 caracteres), correction d'erreurs L ou M (M quand le texte tient dans la meme
 * taille). Suffisant pour un lien ; un texte trop long est refuse.
 */
final class QrCode {

    private static final int VERSION_MAX = 10;
    /** Octets de correction par bloc et nombre de blocs, par version (indice 0 inutilise) : niveau L puis M. */
    private static final int[][] CORRECTION = {
            {0, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18},
            {0, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26}};
    private static final int[][] BLOCS = {
            {0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4},
            {0, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5}};
    /** Bits du niveau de correction dans les informations de format : L = 1, M = 0. */
    private static final int[] BITS_NIVEAU = {1, 0};

    private final int taille;
    private final boolean[][] modules;
    private final boolean[][] reserve;

    private QrCode(int version, int niveau, byte[] donnees) {
        taille = version * 4 + 17;
        modules = new boolean[taille][taille];
        reserve = new boolean[taille][taille];
        dessinerMotifs(version);
        placer(entrelacer(version, niveau, donnees));
        int meilleur = 0;
        int minimum = Integer.MAX_VALUE;
        for (int masque = 0; masque < 8; masque++) {
            masquer(masque);
            dessinerFormat(niveau, masque);
            int penalite = penalite();
            if (penalite < minimum) {
                minimum = penalite;
                meilleur = masque;
            }
            masquer(masque); // le meme masque applique deux fois s'annule
        }
        masquer(meilleur);
        dessinerFormat(niveau, meilleur);
    }

    /** QR code de ce texte (UTF-8). IllegalArgumentException s'il est trop long. */
    static QrCode de(String texte) {
        byte[] octets = texte.getBytes(StandardCharsets.UTF_8);
        for (int version = 1; version <= VERSION_MAX; version++) {
            int besoin = 4 + (version < 10 ? 8 : 16) + octets.length * 8;
            if (besoin > capacite(version, 0) * 8) {
                continue;
            }
            int niveau = besoin <= capacite(version, 1) * 8 ? 1 : 0;
            return new QrCode(version, niveau, coder(octets, version, capacite(version, niveau)));
        }
        throw new IllegalArgumentException("Texte trop long pour un QR code : " + octets.length + " octets");
    }

    /** Nombre de modules par cote. */
    int taille() {
        return taille;
    }

    /** Module sombre ? (x = colonne, y = ligne) */
    boolean sombre(int x, int y) {
        return modules[y][x];
    }

    // ------------------------------------------------------------------ donnees

    private static int motsBruts(int version) {
        int bits = (16 * version + 128) * version + 64;
        if (version >= 2) {
            int alignements = version / 7 + 2;
            bits -= (25 * alignements - 10) * alignements - 55;
            if (version >= 7) {
                bits -= 36;
            }
        }
        return bits / 8;
    }

    /** Octets de donnees disponibles pour cette version et ce niveau. */
    private static int capacite(int version, int niveau) {
        return motsBruts(version) - CORRECTION[niveau][version] * BLOCS[niveau][version];
    }

    /** Mode octets : indicateur, longueur, texte, fin, puis remplissage jusqu'a la capacite. */
    private static byte[] coder(byte[] octets, int version, int capacite) {
        boolean[] bits = new boolean[capacite * 8];
        int n = 0;
        n = ajouter(bits, n, 0b0100, 4);
        n = ajouter(bits, n, octets.length, version < 10 ? 8 : 16);
        for (byte octet : octets) {
            n = ajouter(bits, n, octet & 0xFF, 8);
        }
        n = Math.min(bits.length, n + 4);
        n = (n + 7) / 8 * 8;
        for (int remplissage = 0xEC; n < bits.length; remplissage ^= 0xEC ^ 0x11) {
            n = ajouter(bits, n, remplissage, 8);
        }
        byte[] resultat = new byte[capacite];
        for (int i = 0; i < bits.length; i++) {
            if (bits[i]) {
                resultat[i >>> 3] |= (byte) (1 << (7 - (i & 7)));
            }
        }
        return resultat;
    }

    private static int ajouter(boolean[] bits, int position, int valeur, int longueur) {
        for (int i = longueur - 1; i >= 0; i--) {
            bits[position++] = ((valeur >>> i) & 1) != 0;
        }
        return position;
    }

    /** Decoupe en blocs, ajoute la correction de chaque bloc et entrelace le tout. */
    private static byte[] entrelacer(int version, int niveau, byte[] donnees) {
        int blocs = BLOCS[niveau][version];
        int correction = CORRECTION[niveau][version];
        int bruts = motsBruts(version);
        int courts = blocs - bruts % blocs;
        int longueurCourte = bruts / blocs;
        byte[][] tous = new byte[blocs][];
        byte[] diviseur = diviseur(correction);
        for (int i = 0, k = 0; i < blocs; i++) {
            int longueur = longueurCourte - correction + (i < courts ? 0 : 1);
            byte[] bloc = new byte[longueurCourte + 1];
            System.arraycopy(donnees, k, bloc, 0, longueur);
            byte[] reste = reste(java.util.Arrays.copyOfRange(donnees, k, k + longueur), diviseur);
            System.arraycopy(reste, 0, bloc, bloc.length - correction, correction);
            k += longueur;
            tous[i] = bloc;
        }
        byte[] resultat = new byte[bruts];
        for (int i = 0, k = 0; i <= longueurCourte; i++) {
            for (int j = 0; j < blocs; j++) {
                // Les blocs courts ont une case vide a la fin de leurs donnees : elle est sautee.
                if (i != longueurCourte - correction || j >= courts) {
                    resultat[k++] = tous[j][i];
                }
            }
        }
        return resultat;
    }

    // ------------------------------------------------------------------ Reed-Solomon (corps de Galois a 256 elements)

    private static int multiplier(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    private static byte[] diviseur(int degre) {
        byte[] resultat = new byte[degre];
        resultat[degre - 1] = 1;
        int racine = 1;
        for (int i = 0; i < degre; i++) {
            for (int j = 0; j < degre; j++) {
                resultat[j] = (byte) multiplier(resultat[j] & 0xFF, racine);
                if (j + 1 < degre) {
                    resultat[j] ^= resultat[j + 1];
                }
            }
            racine = multiplier(racine, 0x02);
        }
        return resultat;
    }

    private static byte[] reste(byte[] donnees, byte[] diviseur) {
        byte[] resultat = new byte[diviseur.length];
        for (byte octet : donnees) {
            int facteur = (octet ^ resultat[0]) & 0xFF;
            System.arraycopy(resultat, 1, resultat, 0, resultat.length - 1);
            resultat[resultat.length - 1] = 0;
            for (int i = 0; i < resultat.length; i++) {
                resultat[i] ^= (byte) multiplier(diviseur[i] & 0xFF, facteur);
            }
        }
        return resultat;
    }

    // ------------------------------------------------------------------ motifs fixes

    private void fixer(int x, int y, boolean sombre) {
        modules[y][x] = sombre;
        reserve[y][x] = true;
    }

    private void dessinerMotifs(int version) {
        for (int i = 0; i < taille; i++) {
            fixer(6, i, i % 2 == 0);
            fixer(i, 6, i % 2 == 0);
        }
        dessinerRepere(3, 3);
        dessinerRepere(taille - 4, 3);
        dessinerRepere(3, taille - 4);
        int[] positions = alignements(version);
        for (int i = 0; i < positions.length; i++) {
            for (int j = 0; j < positions.length; j++) {
                boolean coin = (i == 0 && j == 0) || (i == 0 && j == positions.length - 1)
                        || (i == positions.length - 1 && j == 0);
                if (!coin) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            fixer(positions[i] + dx, positions[j] + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
                        }
                    }
                }
            }
        }
        dessinerFormat(0, 0); // reserve les cases ; les vraies valeurs sont ecrites apres le choix du masque
        if (version >= 7) {
            int reste = version;
            for (int i = 0; i < 12; i++) {
                reste = (reste << 1) ^ ((reste >>> 11) * 0x1F25);
            }
            int bits = version << 12 | reste;
            for (int i = 0; i < 18; i++) {
                boolean bit = ((bits >>> i) & 1) != 0;
                int a = taille - 11 + i % 3;
                int b = i / 3;
                fixer(a, b, bit);
                fixer(b, a, bit);
            }
        }
    }

    /** Repere de coin (7 x 7) et sa bordure claire. */
    private void dessinerRepere(int x, int y) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int distance = Math.max(Math.abs(dx), Math.abs(dy));
                int xx = x + dx;
                int yy = y + dy;
                if (xx >= 0 && xx < taille && yy >= 0 && yy < taille) {
                    fixer(xx, yy, distance != 2 && distance != 4);
                }
            }
        }
    }

    private int[] alignements(int version) {
        if (version == 1) {
            return new int[0];
        }
        int nombre = version / 7 + 2;
        int pas = (version * 4 + nombre * 2 + 1) / (nombre * 2 - 2) * 2;
        int[] resultat = new int[nombre];
        resultat[0] = 6;
        for (int i = nombre - 1, position = taille - 7; i >= 1; i--, position -= pas) {
            resultat[i] = position;
        }
        return resultat;
    }

    /** Informations de format (niveau de correction et masque), ecrites deux fois autour des reperes. */
    private void dessinerFormat(int niveau, int masque) {
        int donnees = BITS_NIVEAU[niveau] << 3 | masque;
        int reste = donnees;
        for (int i = 0; i < 10; i++) {
            reste = (reste << 1) ^ ((reste >>> 9) * 0x537);
        }
        int bits = (donnees << 10 | reste) ^ 0x5412;
        for (int i = 0; i <= 5; i++) {
            fixer(8, i, bit(bits, i));
        }
        fixer(8, 7, bit(bits, 6));
        fixer(8, 8, bit(bits, 7));
        fixer(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) {
            fixer(14 - i, 8, bit(bits, i));
        }
        for (int i = 0; i < 8; i++) {
            fixer(taille - 1 - i, 8, bit(bits, i));
        }
        for (int i = 8; i < 15; i++) {
            fixer(8, taille - 15 + i, bit(bits, i));
        }
        fixer(8, taille - 8, true);
    }

    private static boolean bit(int valeur, int i) {
        return ((valeur >>> i) & 1) != 0;
    }

    // ------------------------------------------------------------------ placement, masques

    /** Ecrit les octets en zigzag, de bas en haut puis de haut en bas, par colonnes de deux en partant de la droite. */
    private void placer(byte[] octets) {
        int i = 0;
        for (int droite = taille - 1; droite >= 1; droite -= 2) {
            if (droite == 6) {
                droite = 5;
            }
            for (int vertical = 0; vertical < taille; vertical++) {
                for (int j = 0; j < 2; j++) {
                    int x = droite - j;
                    boolean montant = ((droite + 1) & 2) == 0;
                    int y = montant ? taille - 1 - vertical : vertical;
                    if (!reserve[y][x] && i < octets.length * 8) {
                        modules[y][x] = bit(octets[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    private void masquer(int masque) {
        for (int y = 0; y < taille; y++) {
            for (int x = 0; x < taille; x++) {
                boolean inverser = switch (masque) {
                    case 0 -> (x + y) % 2 == 0;
                    case 1 -> y % 2 == 0;
                    case 2 -> x % 3 == 0;
                    case 3 -> (x + y) % 3 == 0;
                    case 4 -> (x / 3 + y / 2) % 2 == 0;
                    case 5 -> x * y % 2 + x * y % 3 == 0;
                    case 6 -> (x * y % 2 + x * y % 3) % 2 == 0;
                    default -> ((x + y) % 2 + x * y % 3) % 2 == 0;
                };
                modules[y][x] ^= inverser & !reserve[y][x];
            }
        }
    }

    /** Note d'un masque (plus elle est basse, plus le QR code est facile a lire) : les 4 regles de la norme. */
    private int penalite() {
        int resultat = 0;
        for (int sens = 0; sens < 2; sens++) {
            for (int a = 0; a < taille; a++) {
                int suite = 0;
                boolean couleur = false;
                int historique = 0; // 11 derniers modules de la ligne, pour le motif 1:1:3:1:1 borde de clair
                for (int b = 0; b < taille; b++) {
                    boolean module = sens == 0 ? modules[a][b] : modules[b][a];
                    if (b > 0 && module == couleur) {
                        suite++;
                        if (suite == 5) {
                            resultat += 3;
                        } else if (suite > 5) {
                            resultat++;
                        }
                    } else {
                        couleur = module;
                        suite = 1;
                    }
                    historique = ((historique << 1) | (module ? 1 : 0)) & 0x7FF;
                    if (b >= 10 && (historique == 0b10111010000 || historique == 0b00001011101)) {
                        resultat += 40;
                    }
                }
            }
        }
        int sombres = 0;
        for (int y = 0; y < taille; y++) {
            for (int x = 0; x < taille; x++) {
                if (modules[y][x]) {
                    sombres++;
                }
                if (x + 1 < taille && y + 1 < taille && modules[y][x] == modules[y][x + 1]
                        && modules[y][x] == modules[y + 1][x] && modules[y][x] == modules[y + 1][x + 1]) {
                    resultat += 3;
                }
            }
        }
        int total = taille * taille;
        resultat += ((Math.abs(sombres * 20 - total * 10) + total - 1) / total - 1) * 10;
        return resultat;
    }
}
