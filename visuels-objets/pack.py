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

VERSION = "1.7.0"  # 1.7.0 : objets de KS_Cultures et stades de pousse (canne a sucre, varech), repeteur wifi ; 1.6.0 : rhum et Shejiu (KS_Alcools), Abatteuse, Excavatrice, Laboureuse (KS_SuperTool) ; 1.5.0 : Foreuse (KS_SuperTool) ; 1.2.0 : blocs custom de KS_Blocs ; 1.2.1 : jack o'lanterns lumineuses ; 1.3.0 : low fire, low shield ; 1.4.0 : liane du Shejiu invisible
# Blocs de base que KS_Blocs reecrit sur le serveur (version deployee) : voir plus bas
BASES_BLOCS = {"note_block", "sugar_cane", "kelp"}  # 1.7.0 : KS_Blocs 0.3.0 (protection) sur Lobby, Kanvas, Kixster, Event
DIR = os.path.dirname(os.path.abspath(__file__))
PNG_DIR = os.path.join(DIR, "png")
SORTIE = os.path.join(DIR, "..", "sortie")
JAR = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "versions", "26.3", "26.3.jar")
if "--jar" in sys.argv:
    JAR = sys.argv[sys.argv.index("--jar") + 1]
FORMAT = 97  # ressources du jeu 26.3 (version.json du jar : resource_major)

sys.path.insert(0, DIR)
import dessiner  # noqa: E402  (liste des objets, la meme que la planche)

sys.path.insert(0, os.path.join(DIR, "..", "KS_Blocs", "pack"))
import blocs as ks_blocs  # noqa: E402  (blocs custom de KS_Blocs)

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
    "amulette_mangeuse_de_mal": ["heart_of_the_sea"],
    "foreuse": ["mace"], "foreuse_netherite": ["mace"],
    "abatteuse": ["diamond_axe"], "abatteuse_netherite": ["netherite_axe"],
    "excavatrice": ["diamond_shovel"], "excavatrice_netherite": ["netherite_shovel"],
    "laboureuse": ["diamond_hoe"], "laboureuse_netherite": ["netherite_hoe"],
}
# 1.7.0 : objets de KS_Cultures : l'image du jeu de leur item_model, et la tarte ou le livre en dessous (en trop, sans gene)
for _i, _n, _b in dessiner.FRUITS_CULTURES:
    BASES[_i] = [_b, "pumpkin_pie"]
for _i, _n, _b in dessiner.GRAINES_CULTURES:
    BASES[_i] = [_b, "knowledge_book"]
for _f, _n, _t in dessiner.FLEURS_CULTURES:
    BASES["graine_" + _f] = ["beetroot_seeds", "knowledge_book"]
BASES["melange"] = ["pitcher_pod", "knowledge_book"]
BASES["repeteur_wifi"] = ["calibrated_sculk_sensor", "target"]  # KS_RepeteurWifi : bloc cible, item_model du capteur
# Textures de face de bloc : affichees en cube, comme le bloc vanilla
BLOCS = {"bloc_charbon_de_bois"} | {f"emeraude_compressee_{t}" for t in range(1, 7)}
# Tenues comme un outil
OUTILS = {"bedrock_breaker"} | {"abatteuse", "abatteuse_netherite", "excavatrice", "excavatrice_netherite", "laboureuse", "laboureuse_netherite"}  # + machines de KS_SuperTool
# Tenues avec un modele du jeu precis (1.5.0 : la Foreuse, une masse, tenue comme la masse)
PARENTS = {"foreuse": "minecraft:item/handheld_mace", "foreuse_netherite": "minecraft:item/handheld_mace"}


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


FEU_DECALAGE = 6  # pixels (sur 16) dont la flamme du feu a l'ecran descend
BOUCLIER_DECALAGE = 6  # unites de modele (1/16 de bloc) dont le bouclier descend en vue a la 1re personne


def confort(jar):
    """Low fire et low shield.
    - Feu : le jeu dessine la flamme a l'ecran avec la texture block/fire_1 (ScreenEffectRenderer, FIRE_1 en 26.3) ;
      chaque image de son animation est descendue de FEU_DECALAGE pixels (le haut devient transparent). Le feu pose
      dans le monde utilise aussi fire_1 pour la moitie de ses faces : elles sont plus basses (comme avec tous les
      packs « low fire »).
    - Bouclier : modeles item/shield et item/shield_blocking du jeu, vue a la 1re personne descendue de
      BOUCLIER_DECALAGE (au repos et en parade) ; le reste (3e personne, inventaire...) est inchange."""
    out = {}
    image = dessiner.lire_png(jar.read("assets/minecraft/textures/block/fire_1.png"), taille=0)
    larg = len(image[0])
    for debut in range(0, len(image), larg):  # images carrees l'une sous l'autre
        cadre = image[debut:debut + larg]
        image[debut:debut + larg] = [[None] * larg for _ in range(FEU_DECALAGE)] + cadre[:larg - FEU_DECALAGE]
    temp = os.path.join(SORTIE, "_fire_1.png")
    os.makedirs(SORTIE, exist_ok=True)
    dessiner.ecrire_png(image, temp)
    with open(temp, "rb") as f:
        out["assets/minecraft/textures/block/fire_1.png"] = f.read()
    os.remove(temp)
    out["assets/minecraft/textures/block/fire_1.png.mcmeta"] = jar.read(
        "assets/minecraft/textures/block/fire_1.png.mcmeta")
    for modele in ("shield", "shield_blocking"):
        chemin = f"assets/minecraft/models/item/{modele}.json"
        m = json.loads(jar.read(chemin))
        for main in ("firstperson_righthand", "firstperson_lefthand"):
            m["display"][main]["translation"][1] -= BOUCLIER_DECALAGE
        out[chemin] = json.dumps(m, indent=2)
    # 1.4.0 : Shéjiǔ (KS_Alcools 1.3.0) : la liane sans aucune face, envoyee au seul buveur pour qu'il grimpe, n'a plus
    # de modele (le jeu lui donne sinon celui de la liane cote nord). Cet etat n'existe jamais dans le monde.
    vigne = json.loads(jar.read("assets/minecraft/blockstates/vine.json"))
    sans_face = {"east": "false", "north": "false", "south": "false", "up": "false", "west": "false"}
    # le jeu dessine la liane sans face sur ses 5 faces (5 cas) : remplaces par un seul cas au modele vide
    reste = [c for c in vigne["multipart"] if c.get("when") != sans_face]
    if len(vigne["multipart"]) - len(reste) != 5:
        sys.exit("vine.json : cas de la liane sans face inattendus")
    vigne["multipart"] = reste + [{"apply": {"model": "kalium:block/vide"}, "when": sans_face}]
    out["assets/minecraft/blockstates/vine.json"] = json.dumps(vigne, indent=2)
    out["assets/kalium/models/block/vide.json"] = json.dumps(
        {"textures": {"particle": "minecraft:block/vine"}, "elements": []}, indent=2)
    return out


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
            parent = PARENTS.get(id_) or ("minecraft:item/handheld" if id_ in OUTILS else "minecraft:item/generated")
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

    # 1.2.0 : blocs custom de KS_Blocs (KS_Blocs/pack/blocs.py), seulement ceux des blocs de base reecrits sur TOUS les
    # serveurs Paper (le proxy envoie le pack a tout le reseau). 1.7.0 : canne a sucre et varech, depuis KS_Blocs 0.3.0
    # sur Lobby, Kanvas, Kixster et Event (20/11) : leurs vrais blocs sont montres en age=0, jamais en culture. Un
    # serveur Paper sans KS_Blocs 0.3.0 montrerait des cultures a la place de la canne a sucre et du varech.
    blocs, reserve = ks_blocs.fichiers_java(BASES_BLOCS)
    for nom, donnees in blocs.items():
        if nom in fichiers:
            sys.exit(f"Fichier en double entre les objets et les blocs : {nom}")
        fichiers[nom] = donnees

    # 1.3.0 : confort (demande de LeKiwi06 du 20/11, le pack est envoye par le proxy sur tout KaLium)
    for nom, donnees in confort(jar).items():
        if nom in fichiers:
            sys.exit(f"Fichier en double (confort) : {nom}")
        fichiers[nom] = donnees

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
    print(f"{len(ids)} textures, {len(par_base)} objets de base, "
          f"{sum(len(p) for p in reserve.values())} etats de blocs custom ({', '.join(sorted(reserve))}) -> {zip_chemin}")
    print(f"SHA-1 : {sha1}")


if __name__ == "__main__":
    main()
