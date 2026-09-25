package fr.kalium.kvplots;

/**
 * Géométrie de la grille de plots. Une case = l'intérieur d'un plot moyen (taille x taille), suivie d'une route
 * (bordures en bedrock comprises) : la grille se répète tous les « pas » = taille + route blocs.
 * Un grand plot = 2 x 2 cases dont on a retiré les routes intérieures : taille * 2 + route de côté.
 */
final class Grille {

    /** Case de la grille : colonne (vers +x) et ligne (vers +z). */
    record Case(int colonne, int ligne) {
        double distance2(double c, double l) {
            double dc = colonne - c, dl = ligne - l;
            return dc * dc + dl * dl;
        }
    }

    final int origineX, origineZ, taille, route, pas;
    final int colonneMin, colonneMax, ligneMin, ligneMax;

    Grille(int origineX, int origineZ, int taille, int route,
           int colonneMin, int colonneMax, int ligneMin, int ligneMax) {
        this.origineX = origineX;
        this.origineZ = origineZ;
        this.taille = taille;
        this.route = route;
        this.pas = taille + route;
        this.colonneMin = colonneMin;
        this.colonneMax = colonneMax;
        this.ligneMin = ligneMin;
        this.ligneMax = ligneMax;
    }

    /** Case dont l'intérieur contient (x, z), ou null si (x, z) est sur une route ou une bordure. */
    Case caseEn(int x, int z) {
        int dx = x - origineX, dz = z - origineZ;
        if (Math.floorMod(dx, pas) >= taille || Math.floorMod(dz, pas) >= taille) return null;
        return new Case(Math.floorDiv(dx, pas), Math.floorDiv(dz, pas));
    }

    /** Case dont la zone (intérieur + route qui suit à l'est ou au sud) contient (x, z). */
    Case caseProche(int x, int z) {
        return new Case(Math.floorDiv(x - origineX, pas), Math.floorDiv(z - origineZ, pas));
    }

    boolean dansLeMonde(Case c) {
        return c.colonne() >= colonneMin && c.colonne() <= colonneMax
                && c.ligne() >= ligneMin && c.ligne() <= ligneMax;
    }

    int minX(int colonne) {
        return origineX + colonne * pas;
    }

    int minZ(int ligne) {
        return origineZ + ligne * pas;
    }

    /** Côté de l'intérieur d'un plot de cette taille. */
    int cote(Taille t) {
        return t == Taille.GRAND ? taille * 2 + route : taille;
    }
}
