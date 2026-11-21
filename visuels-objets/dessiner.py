# Textures 16x16 des objets custom de KaLium, dans le style vanilla.
# Elles partent des textures du jeu Java installe sur le PC (recolorees, completees) : les PNG produits contiennent donc
# des pixels de Mojang et restent hors du depot (png/ est dans .gitignore), comme les images de geyser-bedrock.
# Usage : python visuels-objets/dessiner.py [--tout] [--jar <chemin du .jar du jeu>]
#   sans --tout : cree seulement les PNG manquants (les retouches a la main sont gardees) et refait apercu.html
#   --tout      : redessine tout et ECRASE les retouches
#   --planche   : assemble les PNG en une seule image a la taille reelle (planche.png, 10 par ligne, ordre dans
#                 planche.txt) pour tout retoucher d'un coup dans un editeur avec une grille de 16x16
#   --decouper  : redecoupe planche.png en PNG (seules les cases modifiees sont reecrites)
import colorsys
import math
import os
import random
import struct
import sys
import zipfile
import zlib

DIR = os.path.dirname(os.path.abspath(__file__))
PNG_DIR = os.path.join(DIR, "png")
JAR = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "versions", "26.2", "26.2.jar")
if "--jar" in sys.argv:
    JAR = sys.argv[sys.argv.index("--jar") + 1]


# ---------------------------------------------------------------- PNG

def lire_png(d, taille=16):
    i, idat, pal, trns = 8, b"", [], b""
    while i < len(d):
        n, t = struct.unpack(">I4s", d[i:i + 8])
        c = d[i + 8:i + 8 + n]
        i += 12 + n
        if t == b"IHDR":
            w, h, bd, ct = struct.unpack(">IIBB", c[:10])
        elif t == b"PLTE":
            pal = [tuple(c[k:k + 3]) for k in range(0, len(c), 3)]
        elif t == b"tRNS":
            trns = c
        elif t == b"IDAT":
            idat += c
    bpp = {6: 4, 2: 3, 3: 1, 4: 2, 0: 1}[ct]
    st = w * bpp if bd == 8 else (w * bd + 7) // 8
    raw, prev, k, lignes = zlib.decompress(idat), bytearray(st), 0, []
    for _ in range(h):
        f, ligne = raw[k], bytearray(raw[k + 1:k + 1 + st])
        k += 1 + st
        for x in range(st):
            a = ligne[x - bpp] if x >= bpp else 0
            b = prev[x]
            cc = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                ligne[x] = (ligne[x] + a) & 255
            elif f == 2:
                ligne[x] = (ligne[x] + b) & 255
            elif f == 3:
                ligne[x] = (ligne[x] + (a + b) // 2) & 255
            elif f == 4:
                pa, pb, pc = abs(b - cc), abs(a - cc), abs(a + b - 2 * cc)
                ligne[x] = (ligne[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else cc)) & 255
        prev = ligne
        v = ligne if bd == 8 else [(ligne[(x * bd) // 8] >> (8 - bd - (x * bd) % 8)) & ((1 << bd) - 1) for x in range(w)]
        r = []
        for x in range(w):
            if ct == 6:
                r.append(tuple(v[x * 4:x * 4 + 4]))
            elif ct == 2:
                p = tuple(v[x * 3:x * 3 + 3])
                cache = len(trns) == 6 and p == (trns[1], trns[3], trns[5])
                r.append(p + ((0,) if cache else (255,)))
            elif ct == 3:
                r.append(pal[v[x]] + ((trns[v[x]] if v[x] < len(trns) else 255),))
            elif ct == 4:
                r.append((v[x * 2],) * 3 + (v[x * 2 + 1],))
            else:
                cache = len(trns) == 2 and v[x] == (trns[0] << 8 | trns[1])
                r.append((v[x] * 255 // ((1 << bd) - 1),) * 3 + ((0,) if cache else (255,)))
        lignes.append(r)
    lignes = [[(c if c[3] > 0 else None) for c in r] for r in lignes]
    return [r[:taille] for r in lignes[:taille]] if taille else lignes


def ecrire_png(px, chemin):
    brut = b"".join(b"\x00" + b"".join(bytes(c if c else (0, 0, 0, 0)) for c in row) for row in px)
    bloc = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    with open(chemin, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + bloc(b"IHDR", struct.pack(">IIBBBBB", len(px[0]), len(px), 8, 6, 0, 0, 0))
                + bloc(b"IDAT", zlib.compress(brut, 9)) + bloc(b"IEND", b""))


_jar = None


def vanilla(chemin):
    """Texture du jeu, ex. 'item/potion' ; une copie modifiable (16x16, None = transparent)."""
    global _jar
    if _jar is None:
        _jar = zipfile.ZipFile(JAR)
    return lire_png(_jar.read(f"assets/minecraft/textures/{chemin}.png"))


def copie(px):
    return [row[:] for row in px]


def vide():
    return [[None] * 16 for _ in range(16)]


# ---------------------------------------------------------------- couleurs

def rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)


def depuis_hsv(h, s, v, a=255):
    r, g, b = colorsys.hsv_to_rgb(h % 1, max(0, min(1, s)), max(0, min(1, v)))
    return (round(r * 255), round(g * 255), round(b * 255), a)


def multiplier(c, t):
    return (c[0] * t[0] // 255, c[1] * t[1] // 255, c[2] * t[2] // 255, c[3])


def degrade(hexa, n=6):
    """Rampe de n couleurs (de l'ombre a la lumiere) autour d'une couleur, avec le decalage de teinte du vanilla."""
    h, s, v = hsv(rgb(hexa))
    out = []
    for i in range(n):
        t = i / (n - 1)
        out.append(depuis_hsv(h + (t - 0.6) * 0.04, s * (1.25 - 0.45 * t), v * (0.32 + 0.85 * t)))
    return out


def rampe(chemin, n=7):
    """Les couleurs d'une texture vanilla, de la plus sombre a la plus claire, reduites a n paliers."""
    cs = sorted({c for row in vanilla(chemin) for c in row if c}, key=lum)
    if len(cs) <= n:
        return cs
    return [cs[round(i * (len(cs) - 1) / (n - 1))] for i in range(n)]


def appliquer(px, r, masque=None, decal=0.0):
    """Remplace chaque pixel par la couleur de la rampe r a la meme hauteur de luminosite (degrade de couleurs)."""
    pts = [(x, y) for y in range(16) for x in range(16) if px[y][x] and (masque is None or masque[y][x])]
    if not pts:
        return px
    ls = [lum(px[y][x]) for x, y in pts]
    lo, hi = min(ls), max(ls)
    for (x, y), l in zip(pts, ls):
        t = 0.5 if hi == lo else (l - lo) / (hi - lo)
        t = max(0.0, min(1.0, t + decal))
        c = r[round(t * (len(r) - 1))]
        px[y][x] = c[:3] + (px[y][x][3],)
    return px


def teinter(px, teinte, masque=None, sat=1.0, val=1.0):
    for y in range(16):
        for x in range(16):
            c = px[y][x]
            if c and (masque is None or masque[y][x]):
                h, s, v = hsv(c)
                px[y][x] = depuis_hsv(teinte, s * sat, v * val, c[3])
    return px


def masque(px, test):
    return [[bool(px[y][x]) and test(px[y][x], x, y) for x in range(16)] for y in range(16)]


def bouchon(c, x, y):
    """Bouchon de liege des fioles vanilla (brun-rouge, en haut)."""
    h, s, v = hsv(c)
    return y <= 5 and s > 0.35 and (h < 0.11 or h > 0.95)


def superposer(dessous, dessus):
    out = copie(dessous)
    for y in range(16):
        for x in range(16):
            if dessus[y][x]:
                out[y][x] = dessus[y][x]
    return out


def poser(px, points, couleur):
    for x, y in points:
        px[y][x] = couleur
    return px


# ---------------------------------------------------------------- dessins a la main (paliers de lumiere)

def peindre(matieres, niveaux, rampes):
    """matieres : une lettre par pixel (rampe) ; niveaux : 0 (contour) a 9 (reflet). Comme les textures vanilla, le
    contour est la couleur la plus sombre de la rampe de la matiere."""
    px = vide()
    for y in range(16):
        for x in range(16):
            m, n = matieres[y][x], niveaux[y][x]
            if m != "." and n != ".":
                r = rampes[m]
                px[y][x] = r[round(int(n) / 9 * (len(r) - 1))]
    return px


def disque(cx, cy, r_contour, r_face, emblème=None, ex=0, ey=0):
    """Piece vue de face : contour, rebord eclaire en haut a gauche, face creuse, motif grave."""
    mats, nivs = [], []
    grave = set()
    if emblème:
        for j, l in enumerate(emblème):
            for i, ch in enumerate(l):
                if ch == "#":
                    grave.add((ex + i, ey + j))
    for y in range(16):
        lm, ln = "", ""
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            lumiere = -(dx + dy) / (math.sqrt(2) * max(d, 0.01))
            dedans = lambda X, Y: math.hypot(X - cx, Y - cy) <= r_contour
            if not dedans(x, y):
                lm, ln = lm + ".", ln + "."
            elif not all(dedans(X, Y) for X, Y in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))):
                lm, ln = lm + "M", ln + "0"
            elif d > r_face:
                lm, ln = lm + "M", ln + str(max(2, min(9, round(6 + 3 * lumiere))))
            elif (x, y) in grave:
                lm, ln = lm + "M", ln + "2"
            elif (x - 1, y - 1) in grave:
                lm, ln = lm + "M", ln + "7"
            else:
                lm, ln = lm + "M", ln + str(max(3, min(6, round(5 - 1.2 * lumiere * d / r_face))))
        mats.append(lm)
        nivs.append(ln)
    return mats, nivs


MOTIFS = {
    "plume": ["....#", "...#.", "..##.", ".#...", "#...."],
    "crane": [".###.", "#.#.#", "#####", ".#.#.", "....."],
    "portail": [".###.", "#...#", "#.#.#", "#..#.", ".##.."],
    "repere": [".###.", "#...#", ".#.#.", "..#..", "....."],
    "drapeau": ["###..", "####.", "#....", "#....", "#...."],
}

ARGENT = None  # rampes lues au premier besoin


def rampes_metaux():
    return {
        "fer": rampe("item/iron_ingot"), "netherite": rampe("item/netherite_ingot"), "or": rampe("item/gold_ingot"),
        "cuivre": rampe("item/copper_ingot"), "emeraude": rampe("item/emerald"),
    }


def jeton(metal, motif):
    m, n = disque(7.5, 7.5, 7.0, 4.6, MOTIFS[motif], 5, 5)
    return peindre(m, n, {"M": rampes_metaux()[metal]})


def badge(metal, ruban, motif):
    m, n = disque(7.5, 9.5, 5.9, 3.6, MOTIFS[motif], 5, 7)
    rub_m = ["..RRRR....RRRR..", "...RRRR..RRRR...", "....RRRRRRRR....", ".....RRRRRR.....", "......RRRR......"]
    rub_n = ["..0550....0550..", "...0660..0660...", "....07700770....", ".....077770.....", "......0000......"]
    m = rub_m + m[5:]
    n = [rub_n[y] if y < 5 else n[y] for y in range(16)]
    for y in range(4, 6):  # le haut de la medaille passe devant le ruban
        pass
    return peindre(m, n, {"M": rampes_metaux()[metal], "R": degrade(ruban)})


def verrou():
    m = ["................",
         "......SSSS......",
         ".....SSSSSS.....",
         "....SSS..SSS....",
         "....SS....SS....",
         "....SS....SS....",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "..LLLLLLLLLLLL..",
         "................"]
    n = ["................",
         "......0000......",
         ".....079970.....",
         "....076..670....",
         "....07....50....",
         "....07....50....",
         "..000000000000..",
         "..099999999970..",
         "..097777777750..",
         "..077771177750..",
         "..077771177750..",
         "..077777177750..",
         "..075555555540..",
         "..054444444430..",
         "..000000000000..",
         "................"]
    return peindre(m, n, {"S": rampes_metaux()["fer"], "L": rampes_metaux()["or"]})


def amulette():
    """Amulette mangeuse de mal (KS_MaleficesNether) : collier de chaîne en cuivre, pendentif cerclé de cuivre autour
    d'un cœur de balise."""
    m = [".C............C.",
         "..C..........C..",
         "..C..........C..",
         "...C........C...",
         "....C......C....",
         "....C......C....",
         ".....C....C.....",
         "......CCCC......",
         ".....FFFFFF.....",
         "....FffffffF....",
         "...FfGGGGGGfF...",
         "...FfGGGGGGfF...",
         "...FfGGGGGGfF...",
         "...FfGGGGGGfF...",
         "....FffffffF....",
         ".....FFFFFF....."]
    n = [".7............7.",
         "..4..........4..",
         "..7..........7..",
         "...4........4...",
         "....7......7....",
         "....4......4....",
         ".....7....7.....",
         "......5775......",
         ".....000000.....",
         "....09988770....",
         "...0898876540...",
         "...0787665440...",
         "...0776654330...",
         "...0665543230...",
         "....05444330....",
         ".....000000....."]
    cuivre = rampes_metaux()["cuivre"]
    return peindre(m, n, {"C": rampe("item/copper_chain"), "F": cuivre, "f": cuivre, "G": rampe("block/beacon")})


# ---------------------------------------------------------------- recettes a partir du vanilla

def potion(couleur, base="item/potion", couleur_bouchon=None):
    """Fiole vanilla : liquide (potion_overlay) teinte, puis le verre par-dessus."""
    liquide = vanilla("item/potion_overlay")
    t = rgb(couleur)
    for y in range(16):
        for x in range(16):
            if liquide[y][x]:
                liquide[y][x] = multiplier(liquide[y][x], t)
    px = superposer(liquide, vanilla(base))
    if couleur_bouchon:
        appliquer(px, couleur_bouchon if isinstance(couleur_bouchon, list) else degrade(couleur_bouchon),
                  masque(px, bouchon))
    return px


def zone_liquide():
    o = vanilla("item/potion_overlay")
    return [[o[y][x] is not None for x in range(16)] for y in range(16)]


def haut_de(px, m=None):
    for y in range(16):
        for x in range(16):
            if px[y][x] and (m is None or m[y][x]):
                return y
    return 0


def bulles(px, points, couleur="#FFFFFF"):
    return poser(px, points, rgb(couleur))


def chiffon_et_flamme(px):
    """Molotov : le bouchon devient un chiffon, une flamme au-dessus."""
    mb = masque(px, bouchon)
    appliquer(px, degrade("#D8C9A3"), mb)
    pts = [(x, y) for y in range(16) for x in range(16) if mb[y][x]]
    if pts:
        y0 = min(y for _, y in pts)
        xs = sorted(x for x, y in pts if y == y0)
        cx = xs[len(xs) // 2]
        flamme = [(cx, y0 - 1, "#FFB12B"), (cx - 1, y0 - 1, "#FF7A1A"), (cx + 1, y0 - 1, "#E8551A"),
                  (cx, y0 - 2, "#FFE066"), (cx - 1, y0 - 2, "#FFB12B"), (cx, y0 - 3, "#FFB12B"),
                  (cx + 1, y0 - 3, "#FF7A1A")]
        for x, y, c in flamme:
            if 0 <= x < 16 and 0 <= y < 16:
                px[y][x] = rgb(c)
    return px


def torche_augmentee():
    px = vanilla("block/torch")
    flamme = [(x, y) for y in range(16) for x in range(16) if px[y][x] and hsv(px[y][x])[1] < 0.75
              and lum(px[y][x]) > 0.6]
    if flamme:
        xs = [x for x, _ in flamme]
        ys = [y for _, y in flamme]
        x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
        for x in range(x0 - 1, x1 + 2):  # flamme elargie d'un pixel de chaque cote, plus haute de deux
            for y in range(y0 - 2, y1 + 1):
                if 0 <= x < 16 and 0 <= y < 16 and px[y][x] is None:
                    bord = x in (x0 - 1, x1 + 1) or y == y0 - 2
                    px[y][x] = rgb("#E8551A" if bord else "#FFB12B")
        if y0 - 3 >= 0:
            px[y0 - 3][(x0 + x1) // 2] = rgb("#FF7A1A")
        for x in range(x0, x1 + 1):  # bande de tissu sous la flamme
            if y1 + 1 < 16 and px[y1 + 1][x]:
                px[y1 + 1][x] = rgb("#C9B48A")
    return px


def wagon_controleur():
    px = vanilla("item/minecart")
    socle = rampe("block/cobblestone")
    bois = rampe("block/oak_planks")
    # socle de pierre au fond du wagonnet, manche de levier penche, pommeau rouge
    for y, ligne in ((5, (0, 6, 6, 5, 0)), (6, (0, 4, 4, 3, 0)), (7, (0, 0, 0, 0, 0))):
        for i, n in enumerate(ligne):
            px[y][4 + i] = socle[round(n / 6 * (len(socle) - 1))]
    for x, y in ((6, 4), (7, 3), (8, 2)):
        px[y][x] = bois[-2]
        px[y][x + 1] = bois[2]
    px[1][8], px[1][9], px[2][10] = rgb("#E53935"), rgb("#B02525"), rgb("#7A1A1A")
    return px


def locomotive():
    px = [[None] * 16] + vanilla("item/furnace_minecart")[:15]
    noir = rampe("item/netherite_ingot")
    for y in range(0, 4):  # cheminee de 2 pixels, contour sombre
        px[y][5], px[y][6], px[y][7], px[y][8] = noir[0], noir[2], noir[3], noir[0]
    px[0][5], px[0][6], px[0][7], px[0][8] = noir[0], noir[4], noir[4], noir[0]
    return px


def boussole_train():
    px = vanilla("item/compass_00")
    aiguille = masque(px, lambda c, x, y: hsv(c)[1] > 0.5 and (hsv(c)[0] < 0.05 or hsv(c)[0] > 0.95))
    appliquer(px, rampes_metaux()["or"], aiguille)
    return px


def globe_biomes():
    px = vanilla("item/heart_of_the_sea")
    carte = [
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwggggwwwwww",
        "wwwwgggggggwwwww",
        "wwwgggggggsswwww",
        "wwggggggggssswww",
        "wwgggwwggsssswww",
        "wnngwwwwwssswwww",
        "wnnnwwwwwwsswwww",
        "wwnnwwwwgggwwwww",
        "wwwwwwwggggwwwww",
        "wwwwwwwwgggwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
    ]
    couleurs = {"w": rgb("#3F76E4"), "g": rgb("#5BAA3C"), "s": rgb("#DBCB86"), "n": rgb("#EEF4F7")}
    ls = [lum(c) for row in px for c in row if c]
    moyenne = sum(ls) / len(ls)
    for y in range(16):
        for x in range(16):
            c = px[y][x]
            if c:
                f = lum(c) / moyenne
                b = couleurs[carte[y][x]]
                px[y][x] = tuple(min(255, round(v * f)) for v in b[:3]) + (c[3],)
    return px


def bloc_emeraude(tier):
    px = vanilla("block/emerald_block")
    f = 0.72 + 0.11 * (tier - 1)
    for y in range(16):
        for x in range(16):
            h, s, v = hsv(px[y][x])
            px[y][x] = depuis_hsv(h, s * (1.1 - 0.06 * tier), v * f)
    if tier == 6:
        cadre = [[x in (0, 15) or y in (0, 15) for x in range(16)] for y in range(16)]
        appliquer(px, rampes_metaux()["or"], cadre)
    clair = depuis_hsv(0.38, 0.35, 1.0)
    for i in range(tier):
        px[13][8 - tier + 2 * i] = clair
    return px


def cookie(baie):
    """Cookie vanilla : les pepites (pixels sombres) deviennent des baies."""
    px = vanilla("item/cookie")
    # pepites vanilla : brun sombre un peu plus rouge que le contour (teinte < 0,07)
    pepites = masque(px, lambda c, x, y: hsv(c)[2] < 0.4 and hsv(c)[0] < 0.07)
    appliquer(px, baie[-3:], pepites)
    return px


def sandwich():
    """Pain vanilla ouvert sur sa longueur : une bande de jambon rose et une de beurre."""
    px = vanilla("item/bread")
    pts = [(x, y) for y in range(16) for x in range(16) if px[y][x]]
    mx = sum(x for x, _ in pts) / len(pts)
    my = sum(y for _, y in pts) / len(pts)
    jambon, beurre = degrade("#E8949A", 5), rgb("#FFF0A8")
    for x, y in pts:  # le pain est en diagonale (bas gauche -> haut droite) : distance a cet axe
        d = ((x - mx) + (y - my)) / math.sqrt(2)
        voisins = all(0 <= X < 16 and 0 <= Y < 16 and px[Y][X] for X, Y in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
        if not voisins:
            continue
        if -0.75 <= d < 0.0:
            px[y][x] = jambon[3]
        elif 0.0 <= d < 0.75:
            px[y][x] = jambon[2]
        elif 0.75 <= d < 1.5:
            px[y][x] = beurre
    return px


def brunch():
    """Cotelette cuite vanilla avec un oeuf au plat dessus."""
    px = vanilla("item/cooked_porkchop")
    blanc = [(6, 5), (7, 5), (8, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7),
             (6, 8), (7, 8), (8, 8)]
    for x, y in blanc:
        px[y][x] = rgb("#FAF7EE")
    for x, y in ((5, 5), (4, 6), (4, 7), (5, 8), (6, 9), (7, 9), (8, 9), (9, 8), (10, 7), (10, 6), (9, 5), (8, 4),
                 (7, 4), (6, 4)):
        if px[y][x]:
            px[y][x] = rgb("#D9D2C0")
    poser(px, [(7, 6)], rgb("#FFD23A"))
    poser(px, [(6, 6)], rgb("#FFF0A0"))
    poser(px, [(6, 7), (7, 7)], rgb("#F2A81E"))
    return px


def ble_souffle():
    """Ble vanilla, grains gonfles et dores."""
    return appliquer(vanilla("item/wheat"), [rgb(h) for h in ("#6E4A1A", "#A8742A", "#D9A64A", "#F0CC72", "#FFE6A8",
                                                               "#FFF6D6")], decal=0.1)


def bol_cereales():
    """Bol de ragout vanilla : le ragout devient du lait avec des grains de ble souffle."""
    px = vanilla("item/mushroom_stew")
    bol = rampe("item/bowl")
    contenu = masque(px, lambda c, x, y: c[:3] not in [b[:3] for b in bol])
    appliquer(px, [rgb(h) for h in ("#CFC6B4", "#E9E2D3", "#F7F3EA", "#FFFFFF")], contenu)
    rnd = random.Random("bol_cereales")
    grains = [rgb(h) for h in ("#D9A64A", "#F0CC72", "#B8822E")]
    for y in range(16):
        for x in range(16):
            if contenu[y][x] and rnd.random() < 0.4:
                px[y][x] = rnd.choice(grains)
    return px


def fiole_experience(teinte):
    px = vanilla("item/experience_bottle")
    contenu = masque(px, lambda c, x, y: not bouchon(c, x, y) and hsv(c)[1] > 0.3)
    return teinter(px, teinte, contenu)


# ---------------------------------------------------------------- KS_Cultures (20/11/2026, premier jet a retoucher)

def recolorer(chemin, couleur, test=None):
    """Texture du jeu dont les pixels (tous, ou ceux du test) prennent le degrade d'une couleur."""
    px = vanilla(chemin)
    appliquer(px, degrade(couleur, 7), masque(px, test) if test else None)
    return px


def kiwi_coupe():
    """Kiwi coupe : peau brune, chair verte plus claire au centre, coeur creme, pepins noirs."""
    px = vide()
    peau, chair = degrade("#7A5530", 7), degrade("#7FC23A", 7)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            ombre = x + y > 16
            if r <= 6.6:
                if r > 5.6:
                    px[y][x] = peau[1 if ombre else 3]
                elif r <= 1.3:
                    px[y][x] = rgb("#F3EDC8")
                else:
                    px[y][x] = chair[max(2, min(6, round(6 - (r - 1.3) / 4.3 * 3 - (1 if ombre else 0))))]
    for k in range(8):
        a = k * math.pi / 4 + 0.3
        px[round(7.5 + 2.7 * math.sin(a))][round(7.5 + 2.7 * math.cos(a))] = rgb("#1E1A14")
    return px


# Silhouettes : x corps (h = reflet du corps), g / G feuilles (sombre / claire), s tige, r racines ; . vide
FRUITS_DESSINES = {
"tomate": ("""
................
.......s........
.....g.s.g......
....xgggggx.....
..xxxxgggxxxx...
.xxhxxxxxxxxxx..
.xhhxxxxxxxxxx..
.xhxxxxxxxxxxx..
.xxxxxxxxxxxxx..
.xxxxxxxxxxxxx..
.xxxxxxxxxxxxx..
..xxxxxxxxxxx...
...xxxxxxxxx....
.....xxxxx......
................
................""", {"x": "#E8341C", "g": "#3F9A2C", "s": "#2E6B1E"}),
"poivron": ("""
................
........s.......
.......ss.......
....xgggggx.....
...xxxgggxxx....
..xhxxxxxxxxx...
..xhhxxxxxxxx...
..xhxxxxxxxxx...
..xxxxxxxxxxx...
..xxxxxxxxxxx...
..xxxxxxxxxxx...
..xxxxxxxxxxx...
..xxxxxxxxxxx...
...xx.xxx.xx....
................
................""", {"x": "#4CB83A", "g": "#2E6B1E", "s": "#5A7A2A"}),
"aubergine": ("""
................
...........ss...
..........sgg...
........ggggg...
.......xgggg....
......xxxgg.....
.....xhxxxx.....
....xhxxxxx.....
...xhxxxxxx.....
..xhxxxxxxx.....
.xxxxxxxxx......
.xxxxxxxxx......
.xxxxxxxx.......
..xxxxxx........
...xxxx.........
................""", {"x": "#6A2C82", "g": "#3F9A2C", "s": "#2E6B1E"}),
"oignon": ("""
................
.......s........
.......s........
.......x........
......xxx.......
.....xhxxx......
....xhxxxxx.....
...xhxxxxxxx....
..xhxxxxxxxxx...
..xxxxxxxxxxx...
..xxxxxxxxxxx...
..xxxxxxxxxxx...
...xxxxxxxxx....
....xxxxxxx.....
.....r.r.r......
................""", {"x": "#D29A4C", "s": "#8A6A40", "r": "#E8DDB0"}),
"salade": ("""
................
................
.....xx..xx.....
....xxxxxxxxx...
...xxhxxxxxxxx..
..xxhxxGxxxxxx..
..xxxxGGGxxxxx..
.xxxxxGGGGxxxxx.
.xxxxGGGGGGxxxx.
.xxxxGGGGGGxxxx.
.xxxxxGGGGxxxxx.
..xxxxxGGxxxxx..
..xxxxxxxxxxxx..
...xxxxxxxxxx...
.....xxxxxx.....
................""", {"x": "#4FA83A", "G": "#B6E070"}),
"mais": ("""
................
............s...
..........xxx...
.........xxxxx..
........xhxxxx..
.......xhxxxxx..
......xhxxxxx...
.....xhxxxxx....
...g.xxxxxxg....
..gg.xxxxxgg....
..ggxxxxxggg....
..gggxxxggg.....
..ggggxgggg.....
...ggggggg......
...s............
................""", {"x": "#F2C12E", "g": "#7DBA45", "s": "#C8B070"}),
"riz": ("""
................
................
......xx........
.....hxx...xx...
.....xx...hxx...
..........xx....
...xx...........
..hxx....xx.....
..xx....hxx.....
........xx..xx..
....xx.....hxx..
...hxx.....xx...
...xx...xx......
.......hxx......
.......xx.......
................""", {"x": "#F4F0E0"}),
"cerises": ("""
................
..........ggg...
........s.gggg..
.......s.s.gg...
......s...s.....
.....s....s.....
....s......s....
...xxx.....s....
..xhxxx...xxx...
..xhxxx..xhxxx..
..xxxxx..xhxxx..
...xxx...xxxxx..
..........xxx...
................
................
................""", {"x": "#B01030", "g": "#3F9A2C", "s": "#5A3A1E"}),
}
_CORPS = {"x", "h"}
_CONTOURES = {"x", "h", "g", "G"}


def fruit_dessine(nom):
    """Fruit dessine pixel par pixel (21/11/2026, style inspire de Croptopia sans en reprendre d'image) : contour
    sombre, ombre en bas a droite, reflet en haut a gauche."""
    carte, couleurs = FRUITS_DESSINES[nom]
    g = [l for l in carte.strip("\n").split("\n")]
    assert len(g) == 16 and all(len(l) == 16 for l in g), nom
    mat = lambda x, y: g[y][x] if 0 <= x < 16 and 0 <= y < 16 and g[y][x] != "." else None
    groupe = lambda m: "x" if m in _CORPS else m
    rampes = {m: degrade(c, 7) for m, c in couleurs.items()}
    px = vide()
    for y in range(16):
        for x in range(16):
            m = mat(x, y)
            if not m:
                continue
            k = groupe(m)
            r = rampes[k if k in rampes else "x"]
            if m == "h":
                n = 6
            elif m in ("s", "r"):
                n = 3
            else:
                meme = lambda a, b: mat(a, b) and groupe(mat(a, b)) == k
                n = 4
                if not meme(x + 1, y) or not meme(x, y + 1):
                    n = 2
                    if not meme(x + 1, y) and not meme(x, y + 1):
                        n = 1
                elif not meme(x - 1, y) or not meme(x, y - 1):
                    n = 5
                if nom == "mais" and k == "x" and (x + y) % 2 == 0 and n > 2:
                    n -= 1
                if nom == "oignon" and k == "x" and x % 3 == 1 and 2 < n < 6:
                    n -= 1
                if nom == "salade" and k == "x" and (x * 3 + y) % 5 == 0 and n > 2:
                    n -= 1
            px[y][x] = r[n]
    # contour : la couleur la plus sombre du voisin (corps en priorite)
    out = [row[:] for row in px]
    for y in range(16):
        for x in range(16):
            if mat(x, y):
                continue
            voisins = [mat(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            voisins = [groupe(v) for v in voisins if v in _CONTOURES]
            if voisins:
                k = "x" if "x" in voisins else voisins[0]
                out[y][x] = degrade(couleurs[k], 7)[0]
    return out



def fruits_cultures():
    f = {n: fruit_dessine(n) for n in FRUITS_DESSINES}
    f["kiwi"] = kiwi_coupe()
    return f


def _cadre(px):
    pts = [(x, y) for y in range(len(px)) for x in range(len(px[0])) if px[y][x]]
    return (min(p[0] for p in pts), min(p[1] for p in pts), max(p[0] for p in pts), max(p[1] for p in pts))


def miniature(px, taille=7):
    """La partie dessinee de px reduite a taille x taille au plus (dans chaque carre, le pixel le plus colore)."""
    x0, y0, x1, y1 = _cadre(px)
    w, h = x1 - x0 + 1, y1 - y0 + 1
    k = max(w, h) / taille if max(w, h) > taille else 1
    nw, nh = max(1, round(w / k)), max(1, round(h / k))
    out = [[None] * nw for _ in range(nh)]
    for y in range(nh):
        for x in range(nw):
            best = None
            for sy in range(int(y * k), max(int(y * k) + 1, int((y + 1) * k))):
                for sx in range(int(x * k), max(int(x * k) + 1, int((x + 1) * k))):
                    c = px[y0 + min(sy, h - 1)][x0 + min(sx, w - 1)]
                    if c and (best is None or hsv(c)[1] * hsv(c)[2] > hsv(best)[1] * hsv(best)[2]):
                        best = c
            out[y][x] = best
    return out


def corolle(chemin):
    """Le haut d'une petite fleur du jeu (sans la tige), 7 x 7 au plus."""
    px = vanilla(chemin)
    vert = lambda c: 0.17 < hsv(c)[0] < 0.45 and hsv(c)[1] > 0.3
    pts = [(x, y) for y in range(16) for x in range(16) if px[y][x] and not vert(px[y][x])]
    if not pts:
        return miniature(px)
    y0 = min(y for _, y in pts)
    a = max(0, min(16 - 7, round(sum(x for x, _ in pts) / len(pts)) - 3))
    haut = vide()
    for y in range(y0, min(16, y0 + 7)):
        haut[y - y0][:7] = px[y][a:a + 7]
    return miniature(haut)


def coller(fond, petit, x0, y0):
    """petit pose sur fond en (x0, y0), avec un liseré sombre pour le detacher."""
    out = copie(fond)
    h, w = len(petit), len(petit[0])
    for y in range(-1, h + 1):
        for x in range(-1, w + 1):
            X, Y = x0 + x, y0 + y
            dedans = 0 <= y < h and 0 <= x < w and petit[y][x]
            voisin = any(0 <= y + dy < h and 0 <= x + dx < w and petit[y + dy][x + dx]
                         for dy in (-1, 0, 1) for dx in (-1, 0, 1))
            if 0 <= X < 16 and 0 <= Y < 16 and voisin and not dedans:
                out[Y][X] = (40, 30, 20, 255)
    for y in range(h):
        for x in range(w):
            if petit[y][x] and 0 <= x0 + x < 16 and 0 <= y0 + y < 16:
                out[y0 + y][x0 + x] = petit[y][x]
    return out


def contourer(px, couleur):
    """Contour sombre (le bas du degrade de la couleur) autour de chaque pixel dessine (style des fruits)."""
    sombre = degrade(couleur, 7)[0]
    out = copie(px)
    for y in range(16):
        for x in range(16):
            if not px[y][x] and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[y + dy][x + dx]
                                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y][x] = sombre
    return out


def graines(base, couleur, petit):
    """Graines du jeu recolorees et contourees, avec en haut a droite la miniature de ce qu'elles donnent."""
    return coller(contourer(recolorer(base, couleur), couleur), petit, 16 - len(petit[0]), 0)


def melange_graines():
    """Graines de ble, chacune d'une autre couleur."""
    px = vanilla("item/wheat_seeds")
    pal = ["#E8341C", "#F2C12E", "#3E9B2A", "#6A2C82", "#D29A4C", "#3060D0", "#F4EFDC", "#E070B0"]
    vus, n = set(), 0
    for y in range(16):
        for x in range(16):
            if px[y][x] and (x, y) not in vus:
                pile, comp = [(x, y)], set()
                vus.add((x, y))
                while pile:
                    a, b = pile.pop()
                    comp.add((a, b))
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        A, B = a + dx, b + dy
                        if 0 <= A < 16 and 0 <= B < 16 and px[B][A] and (A, B) not in vus:
                            vus.add((A, B))
                            pile.append((A, B))
                appliquer(px, degrade(pal[n % len(pal)], 7), [[(a, b) in comp for a in range(16)] for b in range(16)])
                n += 1
    return contourer(px, "#6B4A2A")


# (id, nom, image de l'objet du jeu sous le livre de connaissances ou la tarte) : KS_Cultures (Vegetal.java, Fleurs.java)
FRUITS_CULTURES = [("tomate", "Tomate", "beetroot"), ("poivron", "Poivron", "carrot"),
                   ("aubergine", "Aubergine", "chorus_fruit"), ("oignon", "Oignon", "potato"),
                   ("salade", "Salade", "dried_kelp"), ("mais", "Épi de maïs", "wheat"), ("riz", "Riz", "pumpkin_seeds"),
                   ("cerises", "Cerises", "sweet_berries"), ("kiwi", "Kiwi", "glow_berries")]
GRAINES_CULTURES = [("graine_tomate", "Graines de tomate", "wheat_seeds"),
                    ("graine_poivron", "Graines de poivron", "wheat_seeds"),
                    ("graine_aubergine", "Graines d'aubergine", "wheat_seeds"),
                    ("graine_oignon", "Graines d'oignon", "wheat_seeds"),
                    ("graine_salade", "Graines de salade", "wheat_seeds"),
                    ("graine_mais", "Graines de maïs", "melon_seeds")]
COULEUR_GRAINES = {"tomate": "#C9A46A", "poivron": "#E9DFA8", "aubergine": "#C9B27A", "oignon": "#3A3330",
                   "salade": "#B88A4A", "mais": "#F0C030"}
# (fleur, nom de la graine, texture du jeu) ; grandes fleurs : la texture entiere reduite
FLEURS_CULTURES = [("dandelion", "pissenlit", "dandelion"), ("poppy", "coquelicot", "poppy"),
                   ("blue_orchid", "orchidée bleue", "blue_orchid"), ("allium", "allium", "allium"),
                   ("azure_bluet", "houstonie bleue", "azure_bluet"), ("red_tulip", "tulipe rouge", "red_tulip"),
                   ("orange_tulip", "tulipe orange", "orange_tulip"), ("white_tulip", "tulipe blanche", "white_tulip"),
                   ("pink_tulip", "tulipe rose", "pink_tulip"), ("oxeye_daisy", "marguerite", "oxeye_daisy"),
                   ("cornflower", "bleuet", "cornflower"), ("lily_of_the_valley", "muguet", "lily_of_the_valley"),
                   ("eyeblossom", "oeilchidée", "open_eyeblossom"), ("sunflower", "tournesol", "sunflower_front"),
                   ("lilac", "lilas", "lilac_top"), ("rose_bush", "rosier", "rose_bush_top"),
                   ("peony", "pivoine", "peony_top")]
GRANDES_FLEURS = {"sunflower", "lilac", "rose_bush", "peony"}


def _feuillage():
    """Plants de carottes du jeu sans les carottes (les pixels orange deviennent des feuilles sombres)."""
    px = vanilla("block/carrots_stage3")
    appliquer(px, degrade("#2F6B22", 7), masque(px, lambda c, x, y: 0.02 < hsv(c)[0] < 0.14 and hsv(c)[1] > 0.4))
    return px


def _fruits_sur(px, couleur, points, forme):
    r = degrade(couleur, 7)
    for x0, y0 in points:
        for dx, dy, n in forme:
            px[y0 + dy][x0 + dx] = r[n]
    return px


_ROND = [(0, 0, 4), (1, 0, 3), (0, 1, 3), (1, 1, 1)]  # 2 x 2, reflet en haut a gauche
_LONG = [(0, 0, 5), (1, 0, 3), (0, 1, 4), (1, 1, 2), (0, 2, 3), (1, 2, 1)]  # 2 x 3
_POS = [(3, 6), (10, 5), (6, 9), (12, 10), (1, 11)]


def stade_mur(nom):
    """Stade mur d'un vegetal de KS_Cultures (bloc en croix, visuels-objets/png/blocs), premier jet du 20/11/2026 :
    le feuillage d'une culture du jeu et les fruits de l'objet. Appele par KS_Blocs/pack/blocs.py (textures
    provisoires)."""
    if nom == "culture_tomate":
        return _fruits_sur(_feuillage(), "#EE3F1A", _POS, _ROND)
    if nom == "culture_poivron":
        return _fruits_sur(_feuillage(), "#A8F050", _POS[:4], _LONG)
    if nom == "culture_aubergine":
        return _fruits_sur(_feuillage(), "#6A2C82", _POS[:4], _LONG)
    if nom == "culture_oignon":  # betteraves du jeu, bulbes dores
        return recolorer("block/beetroots_stage3", "#D29A4C",
                         lambda c, x, y: hsv(c)[1] > 0.3 and (hsv(c)[0] > 0.8 or hsv(c)[0] < 0.06))
    if nom == "culture_salade":  # la salade de l'objet, posee au sol
        sal = fruits_cultures()["salade"]
        bas = _cadre(sal)[3]
        px = vide()
        for y in range(16):
            if 0 <= y - bas + 15 < 16:
                px[y - bas + 15] = sal[y][:]
        return px
    if nom == "culture_mais_bas":  # ble du jeu en vert
        return recolorer("block/wheat_stage6", "#4E9A2E")
    if nom == "culture_mais_haut":  # ble du jeu en vert, epis jaunes sur les tiges, panicules claires en haut
        px = recolorer("block/wheat_stage7", "#4E9A2E")
        epi, panicule = degrade("#F2C12E", 7), degrade("#E8D27A", 7)
        tiges = [x for x in range(16) if sum(1 for y in range(16) if px[y][x]) >= 8]
        for x in tiges[::3][:4]:
            for dy, n in ((0, 5), (1, 4), (2, 3)):
                px[8 + x % 3 + dy][x] = epi[n]
        for x in range(16):
            for y in range(16):
                if px[y][x]:
                    if y < 4:
                        px[y][x] = panicule[4]
                    break
        return px
    raise KeyError(nom)


def objets_cultures(add):
    f = fruits_cultures()
    for id_, nom, base in FRUITS_CULTURES:
        add(id_, nom, "KS_Cultures", f"item_model {base} (base tarte)", f[id_])
    for id_, nom, base in GRAINES_CULTURES:
        v = id_[len("graine_"):]
        add(id_, nom, "KS_Cultures", f"item_model {base} (base livre)",
            graines("item/wheat_seeds", COULEUR_GRAINES[v], miniature(f[v])))
    for fl, nom, tex in FLEURS_CULTURES:
        nom = ("Graines d'" if nom[0] in "aeiouéo" else "Graines de ") + nom
        petit = miniature(vanilla("block/" + tex)) if fl in GRANDES_FLEURS else corolle("block/" + tex)
        add("graine_" + fl, nom, "KS_Cultures", "item_model beetroot_seeds (base livre)",
            graines("item/beetroot_seeds", "#8A6A40", petit))
    add("melange", "Mélange de graines", "KS_Cultures", "item_model pitcher_pod (base livre)", melange_graines())


# ---------------------------------------------------------------- objets

def objets():
    """(id, nom, plugin, apparence actuelle, image)"""
    o = []
    add = lambda *a: o.append(a)
    metaux = rampes_metaux()

    # Objets qui ont deja leur correspondance Bedrock (geyser-bedrock).
    add("estomac_gardien", "Estomac du gardien", "KS_EstomacGardien", "item_model black_bundle",
        appliquer(vanilla("item/black_bundle"), degrade("#4FA394", 7)))
    cle = vanilla("item/ominous_trial_key")
    yeux = masque(cle, lambda c, x, y: hsv(c)[1] > 0.4 and (hsv(c)[0] < 0.06 or hsv(c)[0] > 0.9))
    teinter(cle, 0.78, [[not v for v in row] for row in yeux], sat=1.1, val=1.35)
    teinter(cle, 0.42, yeux, sat=1.0, val=1.1)
    add("cle_de_l_end", "Clé de l'End", "KS_EC_Extension", "item_model ominous_trial_key", cle)
    pioche = vanilla("item/iron_pickaxe")
    manche = masque(pioche, lambda c, x, y: hsv(c)[1] > 0.25)
    appliquer(pioche, rampe("item/blaze_rod"), manche)
    appliquer(pioche, rampe("block/bedrock"), [[not v for v in row] for row in manche])
    add("bedrock_breaker", "Bedrock Breaker", "KS_BedrockBreaker", "item_model blaze_rod", pioche)
    add("fragment_spawner", "Fragment de Spawner", "KS_ItemSimple", "item_model disc_fragment_5",
        appliquer(vanilla("item/disc_fragment_5"), rampe("block/spawner") + [rgb("#3BA0E8")]))
    add("coeur_spawner", "Cœur de Spawner", "KS_ItemSimple", "item_model respawn_anchor",
        appliquer(vanilla("item/heart_of_the_sea"), [rgb(h) for h in ("#15181D", "#262C33", "#3A434C", "#59646E",
                                                         "#C8501A", "#FF8A1A", "#FFC233", "#FFF0A0")]))
    add("changeur_biome", "Changeur de Biome", "KS_BiomeChanger", "item_model end_crystal", globe_biomes())

    for id_, nom, metal, motif, actuel in (("fly", "Jeton de fly", "fer", "plume", "iron_ingot"),
                                           ("mort", "Jeton de la mort", "netherite", "crane", "netherite_ingot"),
                                           ("tp", "Jeton de téléportation", "or", "portail", "gold_ingot"),
                                           ("localisation", "Jeton de localisation", "cuivre", "repere", "copper_ingot"),
                                           ("claim", "Jeton de claim", "emeraude", "drapeau", "golden_shovel")):
        add("jeton_" + id_, nom, "KS_Jetons", "item_model " + actuel, jeton(metal, motif))
    for id_, nom, metal, ruban, motif, actuel in (
            ("fly", "Badge de fly", "fer", "#5AA9E6", "plume", "iron_block"),
            ("mort", "Badge de la mort", "netherite", "#B0202A", "crane", "netherite_block"),
            ("tp", "Badge de téléportation", "or", "#8E4EC6", "portail", "gold_block"),
            ("localisation", "Badge de localisation", "cuivre", "#3C9E5A", "repere", "copper_block")):
        add("badge_" + id_, nom, "KS_Jetons", "item_model " + actuel, badge(metal, ruban, motif))

    cristal = vanilla("item/amethyst_shard")
    add("cristal_purification", "Cristal de purification", "KS_Meule", "item_model amethyst_shard",
        teinter(cristal, 0.52, sat=0.55, val=1.12))
    add("casse_croute_dore", "Casse-croûte doré", "KS_CasseCroute", "item_model suspicious_stew (base tarte)",
        appliquer(vanilla("item/bread"), metaux["or"], decal=0.05))
    papier = vanilla("item/paper")
    pts = [(x, y) for y in range(16) for x in range(16) if papier[y][x]]
    cx = round(sum(x for x, _ in pts) / len(pts))
    cy = round(sum(y for _, y in pts) / len(pts))
    rouge = degrade("#D03030")
    poser(papier, [(cx, cy - 1), (cx - 1, cy), (cx, cy)], rouge[4])
    poser(papier, [(cx + 1, cy), (cx, cy + 1)], rouge[2])
    add("bandage", "Bandage", "KS_Alcools", "item_model paper (base kelp séché)", papier)

    # KS_Alcools (fioles vanilla ; la version jetable garde la meme image)
    liege = None
    add("alcool_neutre", "Alcool neutre", "KS_Alcools", "potion", potion("#E3EEF2", couleur_bouchon=liege))
    add("alcool_tequila", "Tequila", "KS_Alcools", "potion", potion("#E8C547", couleur_bouchon="#3E9E3E"))
    add("alcool_vodka", "Vodka", "KS_Alcools", "potion", potion("#C9D8E8", couleur_bouchon=metaux["fer"]))
    miel = vanilla("item/honey_bottle")
    teinter(miel, 0.075, masque(miel, lambda c, x, y: hsv(c)[1] > 0.45 and not bouchon(c, x, y)), sat=1.05, val=0.95)
    add("alcool_hydromel", "Hydromel", "KS_Alcools", "potion", miel)
    add("alcool_manzana", "Manzana", "KS_Alcools", "potion", potion("#9BC53D", couleur_bouchon="#C0392B"))
    biere = potion("#D9902B")
    zl = zone_liquide()
    y_mousse = haut_de(biere, zl)
    for x in range(16):
        if zl[y_mousse][x]:
            biere[y_mousse][x] = rgb("#FBF3DC")
        if y_mousse + 1 < 16 and zl[y_mousse + 1][x] and x % 2 == 0:
            biere[y_mousse + 1][x] = rgb("#F1E2B8")
    add("alcool_biere", "Bière", "KS_Alcools", "potion", biere)
    add("alcool_tabascoolise", "Tabascoolisé", "KS_Alcools", "potion", potion("#C8261E", couleur_bouchon="#2E7D32"))
    # KS_Alcools 1.1.0 / 1.3.0 : rhum ambré à la cire noire ; Shéjiǔ (« vin de serpent ») : un serpent doré dans le
    # liquide vert olive, bouchon rouge
    add("alcool_rhum", "Rhum", "KS_Alcools", "potion", potion("#9A5420", couleur_bouchon="#2B2B2B"))
    shejiu = potion("#6B8E23", couleur_bouchon="#7A1414")
    zl = zone_liquide()
    serpent = [p for p in [(8, 9), (7, 10), (8, 11), (9, 12), (8, 13), (7, 13)] if zl[p[1]][p[0]]]
    poser(shejiu, serpent, rgb("#D8B43A"))
    poser(shejiu, [(9, 8)], rgb("#8A6A12"))  # tête
    add("alcool_shejiu", "Shéjiǔ", "KS_Alcools", "potion", shejiu)
    add("molotov", "Cocktail Molotov", "KS_Alcools", "splash_potion",
        chiffon_et_flamme(potion("#FF7A1A", base="item/splash_potion")))
    add("torche_augmentee", "Torche augmentée", "KS_Alcools", "torch", torche_augmentee())

    # KS_Elixir : fiole vanilla, bouchon en or
    for id_, nom, liquide in (("super_gateau", "Super Gâteau", "#FF69B4"),
                              ("chauve_souris", "Élixir de chauve-souris", "#2B2F8F"),
                              ("anguille", "Élixir d'anguille", "#87CEEB"),
                              ("ignifugation", "Élixir d'ignifugation", "#F0A020"),
                              ("plume", "Élixir de Plume", "#E08AF0"),
                              ("phenix", "Élixir de Phénix", "#D8322A"),
                              ("fantome", "Élixir du Fantôme", "#A8A8B8"),
                              ("titan", "Élixir de Titan", "#B8862A"),
                              ("vent", "Élixir du Vent", "#5FE0E0"),
                              ("rebond", "Élixir de Rebond", "#7CDB4A"),
                              ("fortune", "Élixir de Fortune", "#1F7A36")):
        add("elixir_" + id_, nom, "KS_Elixir", "potion", potion(liquide, couleur_bouchon=metaux["or"]))

    # KS_Trains et KS_VerrouVehicule
    add("locomotive", "Locomotive", "KS_Trains", "furnace_minecart", locomotive())
    add("wagon_controleur", "Wagon contrôleur", "KS_Trains", "minecart", wagon_controleur())
    add("boussole_train", "Boussole du train", "KS_Trains", "compass", boussole_train())
    add("verrou_vehicule", "Verrou de véhicule", "KS_VerrouVehicule", "tripwire_hook", verrou())

    # Pas de texture (choix de LeKiwi06, 29/10/2026) : coffre renforce, vehicules avec coffre renforce et pomme d'amour
    # gardent l'image vanilla avec l'aspect enchante ; le bateau (radeau) a coffre et jukebox prend l'image du bateau
    # (radeau) a jukebox, avec l'aspect enchante.

    # KS_Cuisine
    add("cookie_baie_sucree", "Cookie à la baie sucrée", "KS_Cuisine", "cookie", cookie(rampe("item/sweet_berries")))
    add("cookie_baie_lumineuse", "Cookie à la baie lumineuse", "KS_Cuisine", "cookie",
        cookie([rgb(h) for h in ("#8A3A0E", "#C8601A", "#FF9A1A", "#FFC233", "#FFF0A0")]))
    add("sandwich", "Sandwich jambon-beurre", "KS_Cuisine", "bread", sandwich())
    add("brunch", "Brunch", "KS_Cuisine", "cooked_porkchop", brunch())
    add("ble_souffle", "Blé soufflé", "KS_Cuisine", "item_model wheat (base kelp séché)", ble_souffle())
    add("bol_cereales", "Bol de céréales", "KS_Cuisine", "item_model mushroom_stew (base tarte)", bol_cereales())

    # KS_Economy, KS_Crafts
    for t in range(1, 7):
        add(f"emeraude_compressee_{t}", f"Bloc d'émeraude compressé (tier {t})", "KS_Economy", "emerald_block",
            bloc_emeraude(t))
    add("bloc_charbon_de_bois", "Bloc de charbon de bois", "KS_Crafts", "coal_block",
        appliquer(vanilla("block/coal_block"), rampe("item/charcoal")))

    # Fioles d'experience
    add("fiole_experience", "Fiole d'expérience (niveaux)", "KS_FioleExp", "experience_bottle", fiole_experience(0.5))
    add("fiole_experience_mort", "Fiole d'expérience (points, coffre de mort)", "KS_CoffreMort", "experience_bottle",
        fiole_experience(0.78))

    # Vehicules a jukebox : dessines par LeKiwi06 (le PNG fait foi) ; sans PNG, l'image vanilla d'aujourd'hui sert de
    # base. Les bateaux (radeaux) a coffre et jukebox reprennent ces images, avec l'aspect enchante.
    for id_, nom, plugin, base in (
            ("bateau_jukebox", "Bateau à jukebox (et à coffre et jukebox, enchanté)", "KS_JukeboxBoat", "oak_boat"),
            ("radeau_jukebox", "Radeau à jukebox (et à coffre et jukebox, enchanté)", "KS_JukeboxBoat", "bamboo_raft"),
            ("wagon_jukebox", "Wagon jukebox", "KS_Trains", "minecart")):
        add(id_, nom, plugin, base, vanilla("item/" + base))

    # KS_MaleficesNether (30/10/2026) : premier jet, à retoucher par LeKiwi06.
    add("amulette_mangeuse_de_mal", "Amulette mangeuse de mal", "KS_MaleficesNether", "item_model heart_of_the_sea",
        amulette())

    # KS_SuperTool (20/11/2026) : Foreuse dessinée par LeKiwi06, couleurs et ombres du jeu par Claude (le PNG fait
    # foi) ; sans PNG, l'image de la masse sert de base. Le Cœur de machine garde l'image du noyau lourd.
    add("foreuse", "Foreuse", "KS_SuperTool", "mace", vanilla("item/mace"))
    add("foreuse_netherite", "Foreuse en netherite", "KS_SuperTool", "mace", vanilla("item/mace"))
    # Abatteuse (hache double à croissants, d'après un croquis de LeKiwi06), Excavatrice, Laboureuse : dessinées par
    # Claude, validées par LeKiwi06 le 20/11/2026 (le PNG fait foi).
    add("abatteuse", "Abatteuse", "KS_SuperTool", "diamond_axe", vanilla("item/diamond_axe"))
    add("abatteuse_netherite", "Abatteuse en netherite", "KS_SuperTool", "netherite_axe", vanilla("item/netherite_axe"))
    add("excavatrice", "Excavatrice", "KS_SuperTool", "diamond_shovel", vanilla("item/diamond_shovel"))
    add("excavatrice_netherite", "Excavatrice en netherite", "KS_SuperTool", "netherite_shovel", vanilla("item/netherite_shovel"))
    add("laboureuse", "Laboureuse", "KS_SuperTool", "diamond_hoe", vanilla("item/diamond_hoe"))
    add("laboureuse_netherite", "Laboureuse en netherite", "KS_SuperTool", "netherite_hoe", vanilla("item/netherite_hoe"))

    # KS_Cultures (20/11/2026) : premier jet de Claude a partir des images du jeu, a retoucher par LeKiwi06
    objets_cultures(add)
    return o


PLANCHE, ORDRE, COLONNES = os.path.join(DIR, "planche.png"), os.path.join(DIR, "planche.txt"), 10


def lire_fichier(chemin, taille=16):
    with open(chemin, "rb") as f:
        return lire_png(f.read(), taille)


def planche(liste):
    ids = [i for i, *_ in liste if os.path.exists(os.path.join(PNG_DIR, i + ".png"))]
    lignes = (len(ids) + COLONNES - 1) // COLONNES
    px = [[None] * (16 * COLONNES) for _ in range(16 * lignes)]
    for n, id_ in enumerate(ids):
        case = lire_fichier(os.path.join(PNG_DIR, id_ + ".png"))
        for y in range(16):
            px[16 * (n // COLONNES) + y][16 * (n % COLONNES):16 * (n % COLONNES) + 16] = case[y]
    ecrire_png(px, PLANCHE)
    with open(ORDRE, "w", encoding="utf-8") as f:
        f.write("".join(id_ + "\n" for id_ in ids))
    print(f"{len(ids)} objets dans {PLANCHE} ({16 * COLONNES}x{16 * lignes}), ordre dans {ORDRE}")


def decouper():
    with open(ORDRE, encoding="utf-8") as f:
        ids = [l.strip() for l in f if l.strip()]
    px = lire_fichier(PLANCHE, None)
    changes = []
    for n, id_ in enumerate(ids):
        x0, y0 = 16 * (n % COLONNES), 16 * (n // COLONNES)
        case = [row[x0:x0 + 16] for row in px[y0:y0 + 16]]
        chemin = os.path.join(PNG_DIR, id_ + ".png")
        if not os.path.exists(chemin) or lire_fichier(chemin) != case:
            ecrire_png(case, chemin)
            changes.append(id_)
    print(f"{len(changes)} PNG reecrits depuis {PLANCHE}" + (" : " + ", ".join(changes) if changes else ""))


def main():
    tout = "--tout" in sys.argv
    if "--decouper" in sys.argv:
        decouper()
    os.makedirs(PNG_DIR, exist_ok=True)
    liste = objets()
    ecrits = 0
    for id_, _, _, _, px in liste:
        chemin = os.path.join(PNG_DIR, id_ + ".png")
        if tout or not os.path.exists(chemin):
            ecrire_png(px, chemin)
            ecrits += 1
    cartes = "\n".join(
        f'<figure><div class="slot"><img src="png/{i}.png" alt=""></div><figcaption><b>{n}</b>'
        f'<span>{i}.png</span><span>{p} · aujourd\'hui : {a}</span></figcaption></figure>'
        for i, n, p, a, _ in liste)
    with open(os.path.join(DIR, "apercu.html"), "w", encoding="utf-8") as f:
        f.write(APERCU.replace("{{N}}", str(len(liste))).replace("{{CARTES}}", cartes))
    print(f"{len(liste)} objets, {ecrits} PNG ecrits dans {PNG_DIR}")
    if "--planche" in sys.argv:
        planche(liste)


APERCU = """<!doctype html>
<html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Textures des objets</title>
<style>
:root { --fond:#f4f1ea; --carte:#fff; --texte:#222; --gris:#6b6b6b; --slot:#8b8b8b; }
@media (prefers-color-scheme: dark) { :root { --fond:#1c1c1c; --carte:#2a2a2a; --texte:#eee; --gris:#aaa; } }
body { background:var(--fond); color:var(--texte); font:14px/1.4 system-ui, sans-serif; margin:0; padding:24px 16px; }
h1 { font-size:20px; margin:0 0 4px; } p { color:var(--gris); margin:0 0 20px; }
.grille { display:grid; grid-template-columns:repeat(auto-fill, minmax(190px, 1fr)); gap:12px; }
figure { margin:0; background:var(--carte); border-radius:8px; padding:12px; display:flex; gap:12px; align-items:center; }
.slot { background:var(--slot); border:3px solid; border-color:#373737 #fff #fff #373737; padding:4px; flex:none; }
.slot img { width:64px; height:64px; image-rendering:pixelated; display:block; }
figcaption { display:flex; flex-direction:column; min-width:0; }
figcaption span { color:var(--gris); font-size:12px; overflow-wrap:anywhere; }
label { display:inline-flex; gap:6px; align-items:center; margin-bottom:16px; color:var(--gris); }
body.grand .slot img { width:128px; height:128px; }
body.grand .grille { grid-template-columns:repeat(auto-fill, minmax(260px, 1fr)); }
</style></head><body>
<h1>Textures des objets custom ({{N}})</h1>
<p>Premier jet, à retoucher. Chaque image est un PNG 16×16 du dossier png/ : ouvre-le dans un éditeur de pixel art, puis recharge cette page.</p>
<label><input type="checkbox" onchange="document.body.classList.toggle('grand', this.checked)"> Grandes images</label>
<div class="grille">
{{CARTES}}
</div></body></html>
"""

if __name__ == "__main__":
    main()
