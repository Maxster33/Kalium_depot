# Textures des blocs custom de KS_Blocs, a partir du registre KS_Blocs/src/main/resources/apparences.json :
#   - sortie/KaLium-blocs-<VERSION>.zip : pack Java (hors du depot : pixels de Mojang). Definitions completes des
#     blocs de base utilises (bloc musical...) : chaque etat reserve a son modele, tous les autres le modele vanilla.
#     A fusionner plus tard dans le pack de KS_Textures (visuels-objets/pack.py) ; pour un essai, a mettre dans le
#     dossier resourcepacks du jeu ;
#   - geyser-bedrock/custom_mappings/kalium_blocs.json et geyser-bedrock/pack/textures/terrain_texture.json :
#     correspondance Bedrock (Geyser, sur le proxy) et noms des textures du pack Bedrock (geyser-bedrock/build.sh) ;
#   - visuels-objets/png/blocs/<nom>.png : textures provisoires, creees seulement si elles manquent (les images de
#     LeKiwi06 a leur place ne sont jamais ecrasees).
# Usage : python KS_Blocs/pack/blocs.py [--jar <jar du jeu 26.3>] [--geyser <dossier>]
#   --geyser : ecrire la correspondance Bedrock ailleurs que dans geyser-bedrock (essai, sans toucher au dossier commun ;
#   le dossier doit contenir custom_mappings/ et pack/textures/).
import json
import os
import sys
import zipfile

VERSION = "0.2.0"
DIR = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.abspath(os.path.join(DIR, "..", ".."))
APPARENCES = os.path.join(RACINE, "KS_Blocs", "src", "main", "resources", "apparences.json")
PNG_DIR = os.path.join(RACINE, "visuels-objets", "png", "blocs")
SORTIE = os.path.join(RACINE, "sortie")
GEYSER = os.path.join(RACINE, "geyser-bedrock")
JAR = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "versions", "26.3", "26.3.jar")
if "--jar" in sys.argv:
    JAR = sys.argv[sys.argv.index("--jar") + 1]
if "--geyser" in sys.argv:
    GEYSER = os.path.abspath(sys.argv[sys.argv.index("--geyser") + 1])
FORMAT = 97  # ressources du jeu 26.3

sys.path.insert(0, os.path.join(RACINE, "visuels-objets"))
import dessiner  # noqa: E402  (lecture et ecriture des PNG, teintes)

dessiner.JAR = JAR

# Proprietes de chaque bloc de base, dans l'ordre du jeu (toutes les combinaisons = tous les etats)
INSTRUMENTS = ["harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone",
               "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling", "trumpet", "trumpet_exposed",
               "trumpet_oxidized", "trumpet_weathered", "zombie", "skeleton", "creeper", "dragon", "wither_skeleton",
               "piglin", "custom_head"]
PROPRIETES = {
    "note_block": [("instrument", INSTRUMENTS), ("note", [str(n) for n in range(25)]), ("powered", ["false", "true"])],
    "sugar_cane": [("age", [str(n) for n in range(16)])],
    "kelp": [("age", [str(n) for n in range(26)])],
}
# Durete (secondes de minage pour Geyser) et rendu Bedrock de chaque bloc de base
# Plantes (0.2.0, KS_Cultures) : forme en croix, sans collision (comme en Java), cassees d'un coup, laissent passer la
# lumiere ; cadre de selection d'une plante (12 x 16 pixels).
PLANTE = {"geometry": "minecraft:geometry.cross", "destructible_by_mining": 0, "light_dampening": 0,
          "collision_box": {"origin": [0, 0, 0], "size": [0, 0, 0]},
          "selection_box": {"origin": [-6, 0, -6], "size": [12, 16, 12]}}
BEDROCK = {"note_block": {"destructible_by_mining": 1}, "sugar_cane": PLANTE, "kelp": PLANTE}
FACES = ["north", "east", "south", "west"]

# Textures provisoires : (texture du jeu, teinte des pixels clairs ou None)
PROVISOIRES = {
    "pumpkin_side": ("block/pumpkin_side", None),
    "pumpkin_top": ("block/pumpkin_top", None),
    "jack_o_lantern_bleue": ("block/jack_o_lantern", 0.55),  # bleu de la flamme des ames
    "jack_o_lantern_cuivre": ("block/jack_o_lantern", 0.36),  # vert de la flamme de la torche de cuivre
    # 0.2.0 : stades des cultures de KS_Cultures (images du jeu en attendant celles de LeKiwi06)
    "culture_pousse": ("block/wheat_stage1", None),
    "culture_jeune": ("block/carrots_stage1", None),
    "culture_fleur": ("block/torchflower_crop_stage1", None),
    "culture_tomate": ("block/beetroots_stage3", None),
    "culture_poivron": ("block/carrots_stage3", None),
    "culture_aubergine": ("block/potatoes_stage3", None),
    "culture_oignon": ("block/beetroots_stage2", None),
    "culture_salade": ("block/potatoes_stage2", None),
    "culture_mais_bas": ("block/wheat_stage6", None),
    "culture_mais_haut": ("block/wheat_stage7", None),
    "culture_kiwi": ("block/sweet_berry_bush_stage2", None),
    "culture_kiwi_fruits": ("block/sweet_berry_bush_stage3", 0.08),  # baies teintees en brun
    "riz_pousse": ("block/wheat_stage2", None),
    "riz_jeune": ("block/wheat_stage4", None),
    "riz_mur": ("block/wheat_stage7", None),
}


def etats(bloc):
    combinaisons = [[]]
    for nom, valeurs in PROPRIETES[bloc]:
        combinaisons = [c + [f"{nom}={v}"] for c in combinaisons for v in valeurs]
    return [",".join(c) for c in combinaisons]


def normaliser(bloc, texte):
    """Proprietes dans l'ordre du jeu (cle commune au registre, au pack Java et a Geyser)."""
    valeurs = dict(p.split("=") for p in texte.split(","))
    return ",".join(f"{nom}={valeurs[nom]}" for nom, _ in PROPRIETES[bloc])


def texture_provisoire(nom):
    chemin = os.path.join(PNG_DIR, nom + ".png")
    if os.path.exists(chemin):
        return
    if nom not in PROVISOIRES:
        sys.exit(f"Texture manquante, sans texture provisoire prevue : {chemin}")
    source, teinte = PROVISOIRES[nom]
    px = dessiner.vanilla(source)
    if teinte is not None:
        if nom.startswith("culture_"):  # les baies rouges
            rouges = dessiner.masque(px, lambda c, x, y: dessiner.hsv(c)[1] > 0.4
                                     and (dessiner.hsv(c)[0] < 0.05 or dessiner.hsv(c)[0] > 0.9))
            dessiner.teinter(px, teinte, rouges, sat=0.8, val=0.8)
        else:
            clairs = dessiner.masque(px, lambda c, x, y: dessiner.hsv(c)[2] >= 0.99)  # la lueur du visage
            dessiner.teinter(px, teinte, clairs, sat=1.1, val=1.05)
    os.makedirs(PNG_DIR, exist_ok=True)
    dessiner.ecrire_png(px, chemin)
    print(f"texture provisoire : {chemin}")


def main():
    with open(APPARENCES, encoding="utf-8") as f:
        registre = {k: v for k, v in json.load(f).items() if not k.startswith("_")}

    def textures_de(a, variante):
        return a.get("textures_variantes", {}).get(variante, a["textures"])

    textures = sorted({t for a in registre.values() for v in [None, *a["variantes"]]
                       for t in textures_de(a, v).values() if ":" not in t})
    for t in textures:
        texture_provisoire(t)

    def ref_java(t):
        return t if ":" in t else f"kalium:block/{t}"

    # ---------------------------------------------------------------- Java
    fichiers = {"pack.mcmeta": json.dumps({"pack": {
        "description": "Blocs custom de KaLium " + VERSION, "min_format": FORMAT, "max_format": FORMAT + 9}}, indent=2)}
    for t in textures:
        with open(os.path.join(PNG_DIR, t + ".png"), "rb") as f:
            fichiers[f"assets/kalium/textures/block/{t}.png"] = f.read()
    reserve = {}  # bloc -> etat -> modele (et rotation)
    for nom, a in registre.items():
        bloc = a["bloc"]
        for variante, etat in a["variantes"].items():
            # un modele par apparence, ou par variante si elle a ses propres textures (0.2.0)
            nom_modele = f"{nom}_{variante}" if variante in a.get("textures_variantes", {}) else nom
            tx = {k: ref_java(v) for k, v in textures_de(a, variante).items()}
            if a["modele"] == "orientable":
                modele = {"parent": "minecraft:block/orientable", "textures": tx}
            elif a["modele"] == "cube":
                modele = {"parent": "minecraft:block/cube_all", "textures": {"all": tx["all"]}}
            elif a["modele"] == "croix":
                modele = {"parent": "minecraft:block/cross", "textures": {"cross": tx["cross"]}}
            else:
                sys.exit(f"Modele inconnu pour {nom} : {a['modele']}")
            fichiers[f"assets/kalium/models/block/{nom_modele}.json"] = json.dumps(modele, indent=2)
            v = {"model": f"kalium:block/{nom_modele}"}
            if a["modele"] == "orientable" and variante in FACES and FACES.index(variante):
                v["y"] = 90 * FACES.index(variante)
            reserve.setdefault(bloc, {})[normaliser(bloc, etat)] = v
    jar = zipfile.ZipFile(JAR)
    for bloc, pris in reserve.items():
        vanilla = json.loads(jar.read(f"assets/minecraft/blockstates/{bloc}.json"))
        if list(vanilla.get("variants", {}).keys()) != [""]:
            sys.exit(f"{bloc} : definition vanilla a plusieurs modeles, remplacement visuel impossible")
        defaut = vanilla["variants"][""]
        variantes = {e: pris.get(e, defaut) for e in etats(bloc)}
        fichiers[f"assets/minecraft/blockstates/{bloc}.json"] = json.dumps({"variants": variantes}, indent=1)

    os.makedirs(SORTIE, exist_ok=True)
    zip_chemin = os.path.join(SORTIE, f"KaLium-blocs-{VERSION}.zip")
    with zipfile.ZipFile(zip_chemin, "w", zipfile.ZIP_DEFLATED) as z:
        for nom in sorted(fichiers):
            info = zipfile.ZipInfo(nom, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            d = fichiers[nom]
            z.writestr(info, d.encode("utf-8") if isinstance(d, str) else d)
    print(f"{len(registre)} apparences, {sum(len(p) for p in reserve.values())} etats reserves -> {zip_chemin}")

    # ---------------------------------------------------------------- Bedrock (Geyser)
    def ref_bedrock(t):
        if ":" in t:
            sys.exit(f"Texture du jeu {t} : Bedrock a besoin d'une image dans visuels-objets/png/blocs")
        return f"kalium_{t}"

    blocs = {}
    for nom, a in registre.items():
        bloc = a["bloc"]
        entree = blocs.setdefault(f"minecraft:{bloc}", {
            "name": f"kalium_{bloc}", "only_override_states": True, "place_air": True,
            "geometry": "minecraft:geometry.full_block", **BEDROCK.get(bloc, {}), "state_overrides": {}})
        for variante, etat in a["variantes"].items():
            tx = textures_de(a, variante)
            if a["modele"] == "orientable":
                mi = {"*": {"texture": ref_bedrock(tx["side"]), "render_method": "opaque"},
                      "up": {"texture": ref_bedrock(tx["top"]), "render_method": "opaque"},
                      "down": {"texture": ref_bedrock(tx["top"]), "render_method": "opaque"},
                      variante: {"texture": ref_bedrock(tx["front"]), "render_method": "opaque"}}
            elif a["modele"] == "cube":
                mi = {"*": {"texture": ref_bedrock(tx["all"]), "render_method": "opaque"}}
            else:
                mi = {"*": {"texture": ref_bedrock(tx["cross"]), "render_method": "alpha_test"}}
            entree["state_overrides"][normaliser(bloc, etat)] = {"material_instances": mi}
    with open(os.path.join(GEYSER, "custom_mappings", "kalium_blocs.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"format_version": 1, "blocks": blocs}, f, indent=2)
        f.write("\n")
    terrain = {"resource_pack_name": "kalium", "texture_name": "atlas.terrain", "padding": 8, "num_mip_levels": 4,
               "texture_data": {f"kalium_{t}": {"textures": f"textures/blocks/kalium_{t}"} for t in textures}}
    with open(os.path.join(GEYSER, "pack", "textures", "terrain_texture.json"), "w", encoding="utf-8",
              newline="\n") as f:
        json.dump(terrain, f, indent=2)
        f.write("\n")
    print(f"Correspondance Bedrock : {GEYSER}/custom_mappings/kalium_blocs.json, pack/textures/terrain_texture.json")


if __name__ == "__main__":
    main()
