# Pack de ressources Java des objets custom (KS_Textures), a partir des PNG de visuels-objets/png.
# Usage : python visuels-objets/pack.py [--jar <jar du jeu 26.3>]
# Sortie : sortie/KaLium-textures-<VERSION>.zip (hors du depot : pixels de Mojang) et son SHA-1, a deposer sur
# minepack.fr puis a recopier dans plugins/KS_Textures/config.yml (pack.url, pack.sha1).
#
# Fonctionnement : pour chaque objet de base (potion, bateau, lingot de fer...), la definition vanilla
# assets/minecraft/items/<base>.json est remplacee par un choix sur la 1re chaine de custom_model_data, que
# KS_Textures ajoute aux objets custom envoyes aux joueurs ; la definition vanilla reste en dernier recours, donc un
# objet ordinaire ne change pas. Chaque base recoit tous les cas : une base en trop ne gene pas, une base oubliee
# laisse l'objet a son image d'aujourd'hui.
import hashlib
import json
import os
import re
import sys
import zipfile

VERSION = "1.0.0"
DIR = os.path.dirname(os.path.abspath(__file__))
PNG_DIR = os.path.join(DIR, "png")
SORTIE = os.path.join(DIR, "..", "sortie")
JAR = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "versions", "26.3", "26.3.jar")
if "--jar" in sys.argv:
    JAR = sys.argv[sys.argv.index("--jar") + 1]
FORMAT = 97  # ressources du jeu 26.3 (version.json du jar : resource_major)

sys.path.insert(0, DIR)
import dessiner  # noqa: E402  (liste des objets, la meme que la planche)

# Objets de base de chaque texture : la definition vanilla utilisee par le jeu (item_model de l'objet, sinon son type)
BASES = {
    "estomac_gardien": ["black_bundle"], "cle_de_l_end": ["ominous_trial_key"], "bedrock_breaker": ["blaze_rod"],
    "fragment_spawner": ["disc_fragment_5"], "coeur_spawner": ["respawn_anchor"], "changeur_biome": ["end_crystal"],
    "jeton_fly": ["iron_ingot"], "jeton_mort": ["netherite_ingot"], "jeton_tp": ["gold_ingot"],
    "jeton_localisation": ["copper_ingot"], "jeton_claim": ["golden_shovel"], "badge_fly": ["iron_block"],
    "badge_mort": ["netherite_block"], "badge_tp": ["gold_block"], "badge_localisation": ["copper_block"],
    "cristal_purification": ["amethyst_shard"], "casse_croute_dore": ["suspicious_stew"], "bandage": ["paper"],
    "molotov": ["splash_potion"], "torche_augmentee": ["torch"], "locomotive": ["furnace_minecart"],
    "wagon_controleur": ["minecart"], "wagon_jukebox": ["minecart"], "boussole_train": ["compass"],
    "verrou_vehicule": ["tripwire_hook"], "cookie_baie_sucree": ["cookie"], "cookie_baie_lumineuse": ["cookie"],
    "sandwich": ["bread"], "brunch": ["cooked_porkchop"], "ble_souffle": ["wheat"], "bol_cereales": ["mushroom_stew"],
    "bloc_charbon_de_bois": ["coal_block"], "fiole_experience": ["experience_bottle"],
    "fiole_experience_mort": ["experience_bottle"], "radeau_jukebox": ["bamboo_raft", "bamboo_chest_raft"],
}
# Textures de face de bloc : affichees en cube, comme le bloc vanilla
BLOCS = {"bloc_charbon_de_bois"} | {f"emeraude_compressee_{t}" for t in range(1, 7)}
# Tenues comme un outil
OUTILS = {"bedrock_breaker"}


def bases(id_, boats):
    if id_.startswith("alcool_"):
        return ["potion", "splash_potion"]  # version jetable : potion jetable
    if id_.startswith("elixir_"):
        return ["potion"]
    if id_.startswith("emeraude_compressee_"):
        return ["emerald_block"]
    if id_ == "bateau_jukebox":
        return boats  # tous les bois, avec ou sans coffre (le bateau a coffre et jukebox prend la meme image)
    return BASES[id_]


def main():
    jar = zipfile.ZipFile(JAR)
    noms = set(jar.namelist())
    ids = [i for i, *_ in dessiner.objets()]
    manquants = [i for i in ids if not os.path.exists(os.path.join(PNG_DIR, i + ".png"))]
    if manquants:
        sys.exit("PNG manquants dans visuels-objets/png : " + ", ".join(manquants))
    boats = sorted(re.match(r"assets/minecraft/items/(.*)\.json", n).group(1) for n in noms
                   if re.fullmatch(r"assets/minecraft/items/[a-z_]+_(chest_)?boat\.json", n))

    fichiers = {}
    fichiers["pack.mcmeta"] = json.dumps({"pack": {
        "description": "Textures des objets de KaLium " + VERSION,
        "min_format": FORMAT, "max_format": FORMAT + 9}}, indent=2)
    with open(os.path.join(PNG_DIR, "jeton_tp.png"), "rb") as f:
        fichiers["pack.png"] = f.read()

    par_base = {}
    for id_ in ids:
        with open(os.path.join(PNG_DIR, id_ + ".png"), "rb") as f:
            fichiers[f"assets/kalium/textures/item/{id_}.png"] = f.read()
        if id_ in BLOCS:
            modele = {"parent": "minecraft:block/cube_all", "textures": {"all": f"kalium:item/{id_}"}}
        else:
            parent = "minecraft:item/handheld" if id_ in OUTILS else "minecraft:item/generated"
            modele = {"parent": parent, "textures": {"layer0": f"kalium:item/{id_}"}}
        fichiers[f"assets/kalium/models/item/{id_}.json"] = json.dumps(modele, indent=2)
        for b in bases(id_, boats):
            par_base.setdefault(b, None)

    cas = [{"when": i, "model": {"type": "minecraft:model", "model": f"kalium:item/{i}"}} for i in ids]
    for b in sorted(par_base):
        chemin = f"assets/minecraft/items/{b}.json"
        if chemin not in noms:
            sys.exit(f"Definition vanilla absente du jeu : {chemin}")
        vanilla = json.loads(jar.read(chemin))
        definition = dict(vanilla)
        definition["model"] = {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
                               "cases": cas, "fallback": vanilla["model"]}
        fichiers[chemin] = json.dumps(definition, indent=2)

    os.makedirs(SORTIE, exist_ok=True)
    zip_chemin = os.path.abspath(os.path.join(SORTIE, f"KaLium-textures-{VERSION}.zip"))
    with zipfile.ZipFile(zip_chemin, "w", zipfile.ZIP_DEFLATED) as z:
        for nom in sorted(fichiers):
            info = zipfile.ZipInfo(nom, date_time=(2026, 1, 1, 0, 0, 0))  # date fixe : meme contenu, meme SHA-1
            info.compress_type = zipfile.ZIP_DEFLATED
            donnees = fichiers[nom]
            z.writestr(info, donnees.encode("utf-8") if isinstance(donnees, str) else donnees)
    with open(zip_chemin, "rb") as f:
        sha1 = hashlib.sha1(f.read()).hexdigest()
    print(f"{len(ids)} textures, {len(par_base)} objets de base -> {zip_chemin}")
    print(f"SHA-1 : {sha1}")


if __name__ == "__main__":
    main()
