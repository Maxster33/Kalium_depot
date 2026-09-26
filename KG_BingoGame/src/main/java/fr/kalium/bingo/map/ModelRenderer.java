package fr.kalium.bingo.map;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 0.8.1 - rendu 3D (vue isometrique, comme l'inventaire du jeu) des objets en forme de bloc, a partir des « elements »
 * de leur modele (boites avec leurs faces texturees). Demande de LeKiwi06 (26/09/2026) : sur la carte de la grille, le
 * piston collant ne se distinguait pas du piston, le juke-box du bloc musical, les lits n'etaient que l'oreiller,
 * l'enclume et d'autres objets en 3D n'etaient pas reconnaissables (une seule face etait montree).
 *
 * Vue : dessus, face nord a gauche, face est a droite (comme les blocs dans l'inventaire). Faces ombrees (dessus 100 %,
 * cotes 80 % et 60 %). Rendu a 3 fois la taille puis reduit a 16 x 16 (moyenne), avec un tampon de profondeur. La
 * piece entiere (plusieurs blocs pour un lit) est centree et mise a l'echelle dans l'icone.
 */
final class ModelRenderer {

    /** Une face a dessiner : 4 coins (x, y, z en seiziemes de bloc), image et zone de texture, ombre. */
    record Face(double[][] corners, BufferedImage texture, double u0, double v0, double u1, double v1, double shade) {
    }

    private static final int SS = 3; // sur-echantillonnage
    private static final int OUT = 16;

    private final List<Face> faces = new ArrayList<>();

    void add(Face face) {
        faces.add(face);
    }

    boolean isEmpty() {
        return faces.isEmpty();
    }

    /** Projection : x vers l'est, y vers le haut, z vers le sud ; le coin le plus proche est (16, y, 0). */
    private static double sx(double[] p) {
        return (p[0] - 16) * 0.5 + p[2] * 0.5;
    }

    /**
     * Vue de l'inventaire du jeu (inclinaison de 30 degres) : une arete verticale mesure cos 30 / cos 45 = 1,22 fois la
     * largeur d'une face (0.8.1 : 1 fois au depart, les blocs paraissaient ecrases - LeKiwi06).
     */
    private static double sy(double[] p) {
        return -p[1] * 0.6124 - ((16 - p[0]) + p[2]) * 0.25;
    }

    private static double depth(double[] p) {
        return p[0] - p[2] + p[1];
    }

    BufferedImage render() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Face f : faces) {
            for (double[] c : f.corners()) {
                minX = Math.min(minX, sx(c));
                maxX = Math.max(maxX, sx(c));
                minY = Math.min(minY, sy(c));
                maxY = Math.max(maxY, sy(c));
            }
        }
        int size = OUT * SS;
        double span = Math.max(maxX - minX, maxY - minY);
        if (span <= 0) {
            return null;
        }
        double scale = size / span;
        double offX = (size - (maxX - minX) * scale) / 2 - minX * scale;
        double offY = (size - (maxY - minY) * scale) / 2 - minY * scale;

        int[] color = new int[size * size];
        double[] zbuf = new double[size * size];
        java.util.Arrays.fill(zbuf, -Double.MAX_VALUE);
        for (Face f : faces) {
            double[][] c = f.corners();
            double ax = sx(c[0]) * scale + offX, ay = sy(c[0]) * scale + offY;
            double ux = sx(c[1]) * scale + offX - ax, uy = sy(c[1]) * scale + offY - ay; // direction u (coin 0 -> 1)
            double vx = sx(c[3]) * scale + offX - ax, vy = sy(c[3]) * scale + offY - ay; // direction v (coin 0 -> 3)
            double det = ux * vy - uy * vx;
            if (Math.abs(det) < 1e-9) {
                continue; // face vue par la tranche
            }
            double d0 = depth(c[0]), du = depth(c[1]) - d0, dv = depth(c[3]) - d0;
            int bx0 = (int) Math.floor(Math.min(Math.min(ax, ax + ux), Math.min(ax + vx, ax + ux + vx)));
            int bx1 = (int) Math.ceil(Math.max(Math.max(ax, ax + ux), Math.max(ax + vx, ax + ux + vx)));
            int by0 = (int) Math.floor(Math.min(Math.min(ay, ay + uy), Math.min(ay + vy, ay + uy + vy)));
            int by1 = (int) Math.ceil(Math.max(Math.max(ay, ay + uy), Math.max(ay + vy, ay + uy + vy)));
            BufferedImage tex = f.texture();
            int tw = tex.getWidth(), th = tex.getHeight();
            for (int py = Math.max(0, by0); py < Math.min(size, by1); py++) {
                for (int px = Math.max(0, bx0); px < Math.min(size, bx1); px++) {
                    double qx = px + 0.5 - ax, qy = py + 0.5 - ay;
                    double s = (qx * vy - qy * vx) / det;
                    double t = (ux * qy - uy * qx) / det;
                    if (s < 0 || s > 1 || t < 0 || t > 1) {
                        continue;
                    }
                    double d = d0 + s * du + t * dv;
                    int idx = py * size + px;
                    if (d < zbuf[idx]) {
                        continue;
                    }
                    double u = f.u0() + s * (f.u1() - f.u0());
                    double v = f.v0() + t * (f.v1() - f.v0());
                    int tx = Math.min(tw - 1, Math.max(0, (int) Math.floor(u / 16.0 * tw)));
                    int ty = Math.min(th - 1, Math.max(0, (int) Math.floor(v / 16.0 * th)));
                    int argb = tex.getRGB(tx, ty);
                    if ((argb >>> 24) < 128) {
                        continue; // transparent : on voit derriere
                    }
                    int r = (int) (((argb >> 16) & 0xFF) * f.shade());
                    int g = (int) (((argb >> 8) & 0xFF) * f.shade());
                    int b = (int) ((argb & 0xFF) * f.shade());
                    color[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
                    zbuf[idx] = d;
                }
            }
        }
        // Reduction a 16 x 16 : moyenne des pixels opaques, opaque si au moins un tiers l'est.
        BufferedImage out = new BufferedImage(OUT, OUT, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < OUT; y++) {
            for (int x = 0; x < OUT; x++) {
                int n = 0, r = 0, g = 0, b = 0;
                for (int dy = 0; dy < SS; dy++) {
                    for (int dx = 0; dx < SS; dx++) {
                        int c = color[(y * SS + dy) * size + x * SS + dx];
                        if ((c >>> 24) != 0) {
                            n++;
                            r += (c >> 16) & 0xFF;
                            g += (c >> 8) & 0xFF;
                            b += c & 0xFF;
                        }
                    }
                }
                if (n * 3 >= SS * SS) {
                    out.setRGB(x, y, 0xFF000000 | ((r / n) << 16) | ((g / n) << 8) | (b / n));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ construction des faces d'une boite

    /** Coins d'une face (dans l'ordre haut-gauche, haut-droite, bas-droite, bas-gauche de sa texture). */
    static double[][] corners(String dir, double[] a, double[] b) {
        double x1 = a[0], y1 = a[1], z1 = a[2], x2 = b[0], y2 = b[1], z2 = b[2];
        return switch (dir) {
            case "up" -> new double[][] {{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
            case "down" -> new double[][] {{x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}, {x1, y1, z1}};
            case "north" -> new double[][] {{x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}, {x2, y1, z1}};
            case "south" -> new double[][] {{x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}, {x1, y1, z2}};
            case "west" -> new double[][] {{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}, {x1, y1, z1}};
            case "east" -> new double[][] {{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}, {x2, y1, z2}};
            default -> null;
        };
    }

    /** Zone de texture par defaut d'une face (quand le modele n'en donne pas). */
    static double[] defaultUv(String dir, double[] a, double[] b) {
        return switch (dir) {
            case "up" -> new double[] {a[0], a[2], b[0], b[2]};
            case "down" -> new double[] {a[0], 16 - b[2], b[0], 16 - a[2]};
            case "north" -> new double[] {16 - b[0], 16 - b[1], 16 - a[0], 16 - a[1]};
            case "south" -> new double[] {a[0], 16 - b[1], b[0], 16 - a[1]};
            case "west" -> new double[] {a[2], 16 - b[1], b[2], 16 - a[1]};
            case "east" -> new double[] {16 - b[2], 16 - b[1], 16 - a[2], 16 - a[1]};
            default -> null;
        };
    }

    static double shade(String dir) {
        return switch (dir) {
            case "up" -> 1.0;
            case "north", "south" -> 0.8;
            case "east", "west" -> 0.62;
            default -> 0.5;
        };
    }

    /** Rotation d'un point autour d'un axe (rotation d'un element du modele). */
    static double[] rotate(double[] p, double[] origin, String axis, double angleDeg) {
        double r = Math.toRadians(angleDeg), cos = Math.cos(r), sin = Math.sin(r);
        double x = p[0] - origin[0], y = p[1] - origin[1], z = p[2] - origin[2];
        double nx = x, ny = y, nz = z;
        switch (axis) {
            case "x" -> {
                ny = y * cos - z * sin;
                nz = y * sin + z * cos;
            }
            case "y" -> {
                nx = x * cos + z * sin;
                nz = -x * sin + z * cos;
            }
            case "z" -> {
                nx = x * cos - y * sin;
                ny = x * sin + y * cos;
            }
            default -> {
            }
        }
        return new double[] {nx + origin[0], ny + origin[1], nz + origin[2]};
    }
}
