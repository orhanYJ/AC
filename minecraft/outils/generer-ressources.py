#!/usr/bin/env python3
"""
Génère les ressources des variantes, des blocs de construction, des objets, des personnages
et des effets de « L'Ombre — Paris 1889 » : modèles, états, textures, noms, recettes, butins.
À lancer depuis le dossier minecraft/ :  python3 outils/generer-ressources.py
Il ne touche pas aux ressources écrites à la main (blocs d'origine).
"""
import json, os, random, math
from PIL import Image, ImageDraw

R = "src/main/resources"
A = f"{R}/assets/ombre"; D = f"{R}/data/ombre"
for d in ["blockstates", "models/block", "models/item", "textures/block", "textures/item", "textures/entity", "textures/mob_effect", "lang"]:
    os.makedirs(f"{A}/{d}", exist_ok=True)
for d in ["loot_table/blocks", "recipe"]:
    os.makedirs(f"{D}/{d}", exist_ok=True)
random.seed(1889)
NOMS = {}

def ecrire(p, o): json.dump(o, open(p, "w"), indent=1, ensure_ascii=False)
def tex(t): return t if ":" in t else "minecraft:block/" + t
def el(fr, to, t, faces=None, tint=False, rot=None, uv=None):
    fs = {}
    for f in (faces or ["north", "south", "east", "west", "up", "down"]):
        fs[f] = {"texture": "#" + t}
        if tint: fs[f]["tintindex"] = 0
        if uv: fs[f]["uv"] = uv
    e = {"from": fr, "to": to, "faces": fs}
    if rot: e["rotation"] = rot
    return e
def modele(nom, textures, elements=None, parent="minecraft:block/block"):
    textures = {k: tex(v) for k, v in textures.items()}
    if elements is not None: textures.setdefault("particle", list(textures.values())[0])
    m = {"parent": parent, "textures": textures}
    if elements is not None: m["elements"] = elements
    ecrire(f"{A}/models/block/{nom}.json", m)
def objet_bloc(nom, modele_nom=None): ecrire(f"{A}/models/item/{nom}.json", {"parent": f"ombre:block/{modele_nom or nom}"})
def oriente(nom, m=None):
    m = m or nom
    ecrire(f"{A}/blockstates/{nom}.json", {"variants": {f"facing={d}": ({"model": f"ombre:block/{m}"} | ({"y": r} if r else {}))
                                                         for d, r in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]}})
def axe(nom, m=None):
    m = m or nom
    ecrire(f"{A}/blockstates/{nom}.json", {"variants": {"axis=z": {"model": f"ombre:block/{m}"}, "axis=x": {"model": f"ombre:block/{m}", "y": 90}}})
def simple(nom, m=None): ecrire(f"{A}/blockstates/{nom}.json", {"variants": {"": {"model": f"ombre:block/{m or nom}"}}})
def butin(nom):
    ecrire(f"{D}/loot_table/blocks/{nom}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"ombre:{nom}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
def I(x):
    if x.startswith("#"): return {"tag": x[1:]}
    return {"item": x if ":" in x else "minecraft:" + x}
def sans_forme(nom, ingr, n=1, cat="building"):
    ecrire(f"{D}/recipe/{nom}.json", {"type": "minecraft:crafting_shapeless", "category": cat, "ingredients": [I(x) for x in ingr],
                                      "result": {"id": f"ombre:{nom}", "count": n}})
def tailleur(nom, ingr, n=1, suffixe=""):
    ecrire(f"{D}/recipe/{nom}{suffixe}_tailleur.json", {"type": "minecraft:stonecutting", "ingredient": I(ingr), "result": {"id": f"ombre:{nom}", "count": n}})

COUL_FR = {"white": ("blanc", "blanche"), "orange": ("orange", "orange"), "magenta": ("magenta", "magenta"), "light_blue": ("bleu clair", "bleu clair"),
           "yellow": ("jaune", "jaune"), "lime": ("vert clair", "vert clair"), "pink": ("rose", "rose"), "gray": ("gris", "grise"),
           "light_gray": ("gris clair", "gris clair"), "cyan": ("cyan", "cyan"), "purple": ("violet", "violette"), "blue": ("bleu", "bleue"),
           "brown": ("marron", "marron"), "green": ("vert", "verte"), "red": ("rouge", "rouge"), "black": ("noir", "noire")}
RGB = {"rouge": (176, 46, 38), "blanc": (233, 236, 236), "vert": (84, 109, 27), "bleu": (53, 57, 157), "jaune": (248, 198, 39), "noir": (29, 29, 33)}

# ───────────── Auvents et cabines de toutes les couleurs ─────────────
for c, (m, f) in COUL_FR.items():
    n = f"auvent_{c}"
    modele(n, {"toile": f"{c}_wool", "bois": "stripped_oak_log"},
           [el([0, 9, 0], [16, 11, 16], "toile", rot={"origin": [8, 10, 0], "axis": "x", "angle": 22.5}), el([0, 10, 0], [16, 12, 1], "bois")])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["ombre:auvent", f"{c}_dye"]); NOMS[f"block.ombre.{n}"] = f"Auvent {m}"
    n = f"cabine_{c}"
    modele(n, {"bois": "spruce_planks", "cadre": "stripped_dark_oak_log", "rideau": f"{c}_wool", "fer": "iron_block"},
           [el([0, 0, 14], [16, 32, 16], "bois"), el([0, 0, 0], [1, 32, 14], "cadre"), el([15, 0, 0], [16, 32, 14], "cadre"),
            el([0, 30, 0], [16, 32, 16], "cadre"), el([1, 0, 0.5], [15, 29, 1.2], "rideau"), el([0, 29, 0], [16, 30, 1], "fer")])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["ombre:cabine", f"{c}_dye"]); NOMS[f"block.ombre.{n}"] = f"Cabine à rideau {m}"

# Auvents rayés : une texture à bandes pour chaque paire
NOM_RAY = {"rouge_blanc": "rouge et blanc", "vert_blanc": "vert et blanc", "bleu_blanc": "bleu et blanc", "jaune_blanc": "jaune et blanc",
           "noir_blanc": "noir et blanc", "rouge_jaune": "rouge et jaune", "bleu_jaune": "bleu et jaune", "vert_jaune": "vert et jaune"}
for paire, nomfr in NOM_RAY.items():
    c1, c2 = paire.split("_")
    t = Image.new("RGBA", (16, 16))
    for x in range(16):
        col = RGB[c1] if (x // 2) % 2 == 0 else RGB[c2]
        for y in range(16):
            k = 0.92 + 0.08 * ((x * 7 + y * 3) % 5) / 4
            t.putpixel((x, y), tuple(int(v * k) for v in col) + (255,))
    t.save(f"{A}/textures/block/raye_{paire}.png")
    n = f"auvent_raye_{paire}"
    modele(n, {"toile": f"ombre:block/raye_{paire}", "bois": "stripped_oak_log"},
           [el([0, 9, 0], [16, 11, 16], "toile", rot={"origin": [8, 10, 0], "axis": "x", "angle": 22.5}), el([0, 10, 0], [16, 12, 1], "bois")])
    oriente(n); objet_bloc(n); butin(n)
    sans_forme(n, ["ombre:auvent", {"rouge": "red_dye", "vert": "green_dye", "bleu": "blue_dye", "jaune": "yellow_dye", "noir": "black_dye"}[c1],
                   "white_dye" if c2 == "blanc" else "yellow_dye"])
    NOMS[f"block.ombre.{n}"] = f"Auvent rayé {nomfr}"

# ───────────── Enseignes de boutique (un dessin par métier) ─────────────
def panneau(fond, dessin):
    t = Image.new("RGBA", (16, 16), fond + (255,)); d = ImageDraw.Draw(t)
    d.rectangle((0, 0, 15, 15), outline=(212, 175, 55, 255))
    dessin(d); return t
BOUT = {
    "boulangerie": ("Boulangerie", (120, 30, 30), lambda d: (d.line((3, 11, 12, 4), fill=(222, 170, 90), width=3), d.line((5, 10, 6, 9), fill=(160, 100, 40)), d.line((8, 8, 9, 7), fill=(160, 100, 40)))),
    "cafe": ("Café", (30, 60, 40), lambda d: (d.rectangle((4, 7, 10, 12), fill=(240, 240, 235)), d.arc((9, 8, 13, 11), 270, 90, fill=(240, 240, 235)), d.line((6, 3, 6, 5), fill=(200, 200, 200)), d.line((8, 2, 8, 5), fill=(200, 200, 200)))),
    "pharmacie": ("Pharmacie", (235, 235, 230), lambda d: (d.rectangle((6, 2, 9, 13), fill=(40, 160, 70)), d.rectangle((2, 6, 13, 9), fill=(40, 160, 70)))),
    "horloger": ("Horloger", (30, 30, 50), lambda d: (d.ellipse((2, 2, 13, 13), fill=(240, 230, 200), outline=(212, 175, 55)), d.line((7, 7, 7, 4), fill=(20, 20, 20)), d.line((7, 7, 10, 8), fill=(20, 20, 20)))),
    "librairie": ("Librairie", (70, 40, 90), lambda d: (d.rectangle((3, 4, 7, 12), fill=(200, 60, 50)), d.rectangle((8, 4, 12, 12), fill=(220, 200, 150)), d.line((7, 4, 7, 12), fill=(60, 30, 20)))),
    "boucherie": ("Boucherie", (150, 20, 20), lambda d: (d.ellipse((3, 4, 12, 11), fill=(230, 140, 140)), d.ellipse((5, 6, 8, 9), fill=(250, 235, 230)), d.line((11, 7, 14, 4), fill=(240, 240, 240), width=2))),
    "chapelier": ("Chapelier", (200, 180, 140), lambda d: (d.rectangle((5, 3, 10, 10), fill=(25, 25, 25)), d.rectangle((2, 10, 13, 11), fill=(25, 25, 25)), d.rectangle((5, 8, 10, 9), fill=(150, 30, 30)))),
    "barbier": ("Barbier", (240, 240, 240), lambda d: [d.line((4 + i, 2, 4 + i - 3, 13), fill=(200, 30, 30) if i % 4 < 2 else (40, 60, 160), width=1) for i in range(0, 12)]),
    "cordonnier": ("Cordonnier", (90, 60, 30), lambda d: (d.polygon([(3, 4), (7, 4), (7, 9), (13, 10), (13, 12), (3, 12)], fill=(30, 20, 15)), d.line((3, 12, 13, 12), fill=(150, 120, 80)))),
    "fleuriste": ("Fleuriste", (60, 110, 60), lambda d: (d.line((7, 7, 7, 13), fill=(40, 120, 40)), d.ellipse((4, 2, 10, 8), fill=(230, 80, 120)), d.ellipse((6, 4, 8, 6), fill=(250, 220, 80)))),
    "tailleur": ("Tailleur", (40, 40, 70), lambda d: (d.line((3, 3, 12, 12), fill=(210, 210, 220), width=2), d.line((12, 3, 3, 12), fill=(210, 210, 220), width=2), d.ellipse((2, 10, 5, 13), outline=(212, 175, 55)), d.ellipse((10, 10, 13, 13), outline=(212, 175, 55)))),
    "marchand_vin": ("Marchand de vin", (110, 20, 40), lambda d: (d.rectangle((6, 6, 9, 13), fill=(40, 80, 40)), d.rectangle((7, 2, 8, 6), fill=(40, 80, 40)), d.rectangle((6, 9, 9, 11), fill=(230, 220, 190)))),
}
for b, (nomfr, fond, dessin) in BOUT.items():
    panneau(fond, dessin).save(f"{A}/textures/block/enseigne_{b}.png")
    n = f"enseigne_{b}"
    modele(n, {"fer": "polished_blackstone", "bois": "spruce_planks", "peint": f"ombre:block/enseigne_{b}", "chaine": "iron_block"},
           [el([7, 12, 0], [9, 14, 16], "fer"), el([7, 10, 0], [9, 16, 2], "fer"), el([7.75, 10, 4], [8.25, 12, 5], "chaine"),
            el([7.75, 10, 12], [8.25, 12, 13], "chaine"), el([7.5, 1.5, 3], [8.5, 10, 14], "bois"),
            el([7.4, 2, 3.5], [8.6, 9.5, 13.5], "peint", ["east", "west"], uv=[0, 0, 16, 16])])
    oriente(n); objet_bloc(n); butin(n)
    sans_forme(n, ["ombre:enseigne", {"boulangerie": "bread", "cafe": "cocoa_beans", "pharmacie": "glistering_melon_slice", "horloger": "clock",
                   "librairie": "book", "boucherie": "porkchop", "chapelier": "black_wool", "barbier": "shears", "cordonnier": "leather_boots",
                   "fleuriste": "poppy", "tailleur": "string", "marchand_vin": "glass_bottle"}[b]])
    NOMS[f"block.ombre.{n}"] = f"Enseigne : {nomfr}"

# ───────────── Bois, pierres, fers ─────────────
BOIS_FR = {"spruce": "sapin", "birch": "bouleau", "dark_oak": "chêne noir", "acacia": "acacia", "cherry": "cerisier", "mangrove": "palétuvier"}
for w, fr in BOIS_FR.items():
    n = f"poutre_{w}"
    modele(n, {"bois": f"stripped_{w}_log"}, [el([5, 6, 0], [11, 12, 16], "bois")]); axe(n); objet_bloc(n); butin(n)
    ecrire(f"{D}/recipe/{n}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["LLL"],
                                    "key": {"L": I(f"stripped_{w}_log")}, "result": {"id": f"ombre:{n}", "count": 6}})
    NOMS[f"block.ombre.{n}"] = f"Poutre de {fr}"
    n = f"banc_{w}"
    modele(n, {"bois": f"{w}_planks", "fer": "polished_blackstone"},
           [el([0, 6, 2], [16, 8, 13], "bois"), el([0, 9, 2], [16, 11, 3.5], "bois"), el([0, 12.5, 2], [16, 14.5, 3.5], "bois"),
            el([1, 0, 3], [2.5, 6, 12], "fer"), el([13.5, 0, 3], [15, 6, 12], "fer"), el([1, 6, 1.5], [2.5, 15, 3], "fer"), el([13.5, 6, 1.5], [15, 15, 3], "fer")])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, [f"{w}_planks", f"{w}_planks", "iron_nugget"], 2)
    NOMS[f"block.ombre.{n}"] = f"Banc public en {fr}"
PIERRE = {"sandstone": ("sandstone", "grès"), "quartz": ("quartz_block_side", "quartz"), "calcite": ("calcite", "calcite"),
          "bricks": ("bricks", "brique"), "deepslate": ("polished_deepslate", "ardoise sombre"), "andesite": ("polished_andesite", "andésite")}
for p, (t, fr) in PIERRE.items():
    n = f"corniche_{p}"
    modele(n, {"pierre": t, "appui": t}, [el([0, 11, 0], [16, 16, 7], "pierre"), el([2, 7, 0], [4, 11, 4], "appui"), el([12, 7, 0], [14, 11, 4], "appui")])
    oriente(n); objet_bloc(n); butin(n); tailleur(n, {"sandstone": "sandstone", "quartz": "quartz_block", "calcite": "calcite", "bricks": "bricks",
                                                    "deepslate": "polished_deepslate", "andesite": "polished_andesite"}[p], 2)
    NOMS[f"block.ombre.{n}"] = f"Corniche de {fr}"
FER = {"vert_de_gris": ("oxidized_copper", "vert-de-gris", "copper_ingot"), "dore": ("gold_block", "doré", "gold_ingot"), "blanc": ("white_concrete", "blanc", "white_dye")}
for f, (t, fr, ing) in FER.items():
    n = f"balcon_{f}"
    modele(n, {"sol": "smooth_stone", "grille": "iron_bars", "fer": t},
           [el([0, 0, 0], [16, 2, 16], "sol"), el([0, 2, 15], [16, 13, 15.6], "grille", ["north", "south"]), el([0, 13, 14.6], [16, 14, 16], "fer"),
            el([0, 2, 15], [1, 13, 16], "fer"), el([15, 2, 15], [16, 13, 16], "fer"), el([7.5, 2, 15], [8.5, 13, 16], "fer"), el([1, -2, 1], [15, 0, 3], "sol")])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["ombre:balcon", ing]); NOMS[f"block.ombre.{n}"] = f"Balcon {fr}"
for r, (t, fr, ing) in {"vert": ("oxidized_copper", "vert", "copper_ingot"), "dore": ("gold_block", "doré", "gold_ingot")}.items():
    n = f"reverbere_{r}"
    modele(n, {"fer": t, "verre": "glass", "feu": "glowstone"},
           [el([7, 13, 0], [9, 15, 12], "fer"), el([6, 12, 0], [10, 16, 1], "fer"), el([7.5, 11, 11.5], [8.5, 13, 12.5], "fer"),
            el([4.5, 11, 8.5], [11.5, 12, 15.5], "fer"), el([5, 4, 9], [11, 11, 15], "verre"), el([6, 5, 10], [10, 10, 14], "feu"), el([6.5, 3, 10.5], [9.5, 4, 13.5], "fer")])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["ombre:reverbere", ing]); NOMS[f"block.ombre.{n}"] = f"Réverbère {fr}"
VOLETS = {"vert": ("green_terracotta", "verts", "green_dye"), "bleu": ("blue_terracotta", "bleus", "blue_dye"), "rouge": ("red_terracotta", "rouges", "red_dye"),
          "gris": ("light_gray_terracotta", "gris", "gray_dye"), "blanc": ("white_terracotta", "blancs", "white_dye"), "brun": ("brown_terracotta", "bruns", "brown_dye"),
          "jaune": ("yellow_terracotta", "jaunes", "yellow_dye"), "noir": ("black_terracotta", "noirs", "black_dye")}
for v, (t, fr, dye) in VOLETS.items():
    n = f"volets_{v}"
    modele(n, {"bois": "dark_oak_planks", "peint": t},
           [el([0, 0, 0], [4, 16, 1], "peint"), el([12, 0, 0], [16, 16, 1], "peint"), el([0, 4, 0], [4, 5, 1.3], "bois"), el([0, 11, 0], [4, 12, 1.3], "bois"),
            el([12, 4, 0], [16, 5, 1.3], "bois"), el([12, 11, 0], [16, 12, 1.3], "bois")])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["#minecraft:planks", dye], 2); NOMS[f"block.ombre.{n}"] = f"Volets {fr}"
ETAL = {"fruits": ("yellow_wool", "pumpkin_side", "de fruits", "apple"), "poisson": ("blue_wool", "prismarine", "de poissons", "cod"),
        "fleurs": ("pink_wool", "flowering_azalea_leaves", "de fleurs", "poppy"), "pain": ("brown_wool", "hay_block_top", "de pain", "bread"),
        "etoffes": ("purple_wool", "magenta_wool", "d'étoffes", "string"), "legumes": ("green_wool", "moss_block", "de légumes", "carrot")}
for e, (toile, marchandise, fr, ing) in ETAL.items():
    n = f"etal_{e}"
    modele(n, {"bois": "spruce_planks", "fruits": marchandise, "toile": toile, "blanc": "white_wool"},
           [el([0, 0, 0], [16, 9, 5], "bois"), el([1, 9, 1], [15, 11, 4], "fruits"), el([0, 0, 15], [1, 15, 16], "bois"), el([15, 0, 15], [16, 15, 16], "bois"),
            el([0, 9, 0], [1, 15, 1], "bois"), el([15, 9, 0], [16, 15, 1], "bois"), el([0, 15, 0], [16, 16, 16], "toile"),
            el([3, 16, 0], [5, 16.2, 16], "blanc", ["up"]), el([7, 16, 0], [9, 16.2, 16], "blanc", ["up"]), el([11, 16, 0], [13, 16.2, 16], "blanc", ["up"])])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["ombre:etal", ing]); NOMS[f"block.ombre.{n}"] = f"Étal {fr}"
for c, (t, fr, ing) in {"pierre": ("stone_bricks", "en pierre", "stone_bricks"), "terre_cuite": ("orange_terracotta", "en terre cuite", "terracotta"),
                        "ardoise": ("deepslate_tiles", "en ardoise", "deepslate_tiles")}.items():
    n = f"cheminee_{c}"
    modele(n, {"brique": t, "pierre": "smooth_stone", "pot": "terracotta"},
           [el([3, 0, 3], [13, 14, 13], "brique"), el([2, 14, 2], [14, 16, 14], "pierre"), el([4, 16, 5], [7, 21, 8], "pot"), el([9, 16, 8], [12, 20, 11], "pot")])
    simple(n); objet_bloc(n); butin(n); sans_forme(n, ["ombre:cheminee", ing]); NOMS[f"block.ombre.{n}"] = f"Cheminée {fr}"

# ───────────── Affiches de 1889 ─────────────
def affiche(fond, titre, dessin):
    t = Image.new("RGBA", (16, 16), fond + (255,)); d = ImageDraw.Draw(t)
    d.rectangle((0, 0, 15, 15), outline=tuple(max(0, v - 60) for v in fond))
    d.rectangle((2, 1, 13, 2), fill=titre)
    dessin(d)
    for y in (13, 14): d.line((3, y, 12, y), fill=(50, 40, 35))
    return t
AFF = {
    "theatre": ("Théâtre", (180, 30, 40), (240, 210, 120), lambda d: (d.ellipse((3, 4, 7, 9), fill=(250, 240, 220)), d.ellipse((8, 5, 12, 10), fill=(30, 30, 30)))),
    "cirque": ("Cirque", (240, 200, 60), (180, 30, 40), lambda d: (d.polygon([(8, 3), (13, 11), (3, 11)], fill=(200, 40, 40)), d.line((8, 3, 8, 11), fill=(250, 250, 250)))),
    "exposition": ("Exposition universelle", (40, 60, 110), (240, 200, 80), lambda d: (d.polygon([(8, 3), (11, 12), (5, 12)], fill=(140, 70, 50)), d.line((6, 9, 10, 9), fill=(60, 30, 20)))),
    "bal": ("Grand bal", (90, 30, 90), (240, 220, 160), lambda d: (d.ellipse((4, 4, 7, 7), fill=(250, 220, 200)), d.polygon([(5, 7), (3, 12), (8, 12)], fill=(230, 80, 120)), d.ellipse((9, 4, 12, 7), fill=(250, 220, 200)), d.rectangle((9, 7, 12, 12), fill=(30, 30, 40)))),
    "chemin_de_fer": ("Chemins de fer", (230, 220, 190), (40, 40, 40), lambda d: (d.rectangle((3, 7, 11, 10), fill=(30, 30, 30)), d.rectangle((9, 4, 11, 7), fill=(30, 30, 30)), d.ellipse((4, 9, 6, 11), fill=(150, 30, 30)), d.ellipse((8, 9, 10, 11), fill=(150, 30, 30)), d.line((12, 3, 14, 2), fill=(200, 200, 200)))),
    "ballon": ("Ascension en ballon", (120, 180, 220), (180, 40, 40), lambda d: (d.ellipse((4, 3, 11, 10), fill=(220, 60, 50)), d.line((5, 9, 7, 12), fill=(80, 60, 40)), d.line((10, 9, 8, 12), fill=(80, 60, 40)), d.rectangle((7, 11, 8, 12), fill=(120, 80, 40)))),
    "recherche": ("Recherché", (225, 210, 170), (120, 20, 20), lambda d: (d.ellipse((5, 4, 10, 10), fill=(60, 50, 45)), d.rectangle((6, 8, 9, 11), fill=(40, 40, 40)), d.line((5, 6, 10, 6), fill=(20, 20, 20)))),
    "chocolat": ("Chocolat", (120, 70, 40), (240, 220, 160), lambda d: (d.rectangle((4, 5, 11, 11), fill=(70, 40, 20)), d.line((4, 8, 11, 8), fill=(110, 70, 40)), d.line((7, 5, 7, 11), fill=(110, 70, 40)))),
    "opera": ("Opéra", (20, 20, 40), (212, 175, 55), lambda d: (d.rectangle((3, 7, 12, 11), fill=(212, 175, 55)), d.polygon([(3, 7), (8, 4), (12, 7)], fill=(180, 150, 60)), [d.line((x, 8, x, 11), fill=(60, 40, 20)) for x in (5, 8, 10)])),
    "savon": ("Savon", (150, 200, 220), (40, 80, 140), lambda d: (d.rectangle((4, 6, 11, 10), fill=(250, 240, 240)), d.ellipse((10, 3, 13, 6), outline=(250, 250, 250)), d.ellipse((2, 4, 4, 6), outline=(250, 250, 250)))),
    "velocipede": ("Vélocipèdes", (240, 160, 60), (30, 30, 30), lambda d: (d.ellipse((2, 7, 7, 12), outline=(30, 30, 30)), d.ellipse((9, 8, 13, 12), outline=(30, 30, 30)), d.line((5, 9, 11, 10), fill=(30, 30, 30)), d.line((8, 6, 8, 10), fill=(30, 30, 30)))),
    "proclamation": ("Avis à la population", (240, 238, 228), (30, 50, 110), lambda d: [d.line((3, y, 12, y), fill=(60, 60, 60)) for y in (5, 7, 9, 11)]),
}
TEINTURES = ["red_dye", "yellow_dye", "blue_dye", "purple_dye", "white_dye", "light_blue_dye", "black_dye", "brown_dye", "orange_dye", "cyan_dye", "lime_dye", "gray_dye"]
for k, (a, (nomfr, fond, titre, dessin)) in enumerate(AFF.items()):
    affiche(fond, titre, dessin).save(f"{A}/textures/block/affiche_{a}.png")
    n = f"affiche_{a}"
    modele(n, {"papier": f"ombre:block/affiche_{a}"}, [el([1, 1, 0], [15, 15, 0.5], "papier", ["north", "south"])])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["paper", TEINTURES[k]], 2); NOMS[f"block.ombre.{n}"] = f"Affiche : {nomfr}"

FANION = {"rouge": "red", "bleu": "blue", "vert": "green", "jaune": "yellow", "blanc": "white", "noir": "black", "violet": "purple", "orange": "orange"}
for f, c in FANION.items():
    n = f"fanion_{f}"
    elts = [el([7.5, 0, 7.5], [8.5, 31, 8.5], "hampe"), el([7, 31, 7], [9, 32, 9], "or")]
    for i in range(6):   # un triangle en escalier
        elts.append(el([8.5 + i * 1.2, 22 + i * 0.8, 7.8], [8.5 + (i + 1) * 1.2, 30 - i * 0.8, 8.2], "toile"))
    modele(n, {"hampe": "stripped_oak_log", "or": "gold_block", "toile": f"{c}_wool"}, elts)
    simple(n); objet_bloc(n); butin(n); sans_forme(n, ["stick", f"{c}_wool"]); NOMS[f"block.ombre.{n}"] = f"Fanion {COUL_FR[c][0]}"

# ───────────── Blocs de construction parisiens (textures dessinées) ─────────────
def bruit(t, base, ampl, graine):
    r = random.Random(graine)
    for x in range(16):
        for y in range(16):
            k = 1 + (r.random() - 0.5) * ampl
            t.putpixel((x, y), tuple(max(0, min(255, int(v * k))) for v in base) + (255,))
def texture(nom, base, ampl=0.08, graine=1, dessin=None):
    t = Image.new("RGBA", (16, 16)); bruit(t, base, ampl, graine)
    if dessin: dessin(ImageDraw.Draw(t), t)
    t.save(f"{A}/textures/block/{nom}.png")
CREME = (222, 207, 172)
def appareil(d, t, joint=(190, 175, 140)):
    for y in (0, 4, 8, 12): d.line((0, y, 15, y), fill=joint)
    for r, y in enumerate((0, 4, 8, 12)):
        for x in ((0, 8) if r % 2 == 0 else (4, 12)): d.line((x, y, x, y + 3), fill=joint)
CONSTR = {
    "pierre_de_paris": ("Pierre de Paris (appareillée)", lambda: texture("pierre_de_paris", CREME, 0.07, 1, appareil), "smooth_sandstone"),
    "pierre_de_paris_lisse": ("Pierre de Paris (lisse)", lambda: texture("pierre_de_paris_lisse", (226, 212, 180), 0.05, 2), "smooth_sandstone"),
    "pierre_de_paris_sculptee": ("Pierre de Paris (sculptée)", lambda: texture("pierre_de_paris_sculptee", CREME, 0.06, 3,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(185, 170, 135)), d.ellipse((5, 5, 10, 10), outline=(185, 170, 135)),
                      d.ellipse((7, 7, 8, 8), fill=(185, 170, 135)), d.rectangle((2, 2, 13, 13), outline=(240, 228, 200)))), "smooth_sandstone"),
    "zinc": ("Zinc (toit)", lambda: texture("zinc", (150, 160, 168), 0.04, 4,
        lambda d, t: [(d.line((x, 0, x, 15), fill=(120, 128, 136)), d.line((x + 1, 0, x + 1, 15), fill=(178, 186, 192))) for x in (3, 11)]), "iron_ingot"),
    "ardoise": ("Ardoise (toit)", lambda: texture("ardoise", (62, 68, 82), 0.1, 5,
        lambda d, t: [d.line((0, y, 15, y), fill=(40, 44, 55)) for y in (3, 7, 11, 15)] + [d.line((x, y - 3, x, y), fill=(40, 44, 55)) for y in (3, 7, 11, 15) for x in ((2, 10) if y % 8 == 3 else (6, 14))]), "deepslate"),
    "pave_parisien": ("Pavés parisiens", lambda: texture("pave_parisien", (118, 116, 112), 0.14, 6,
        lambda d, t: [d.rectangle((x, y, x + 3, y + 3), outline=(80, 78, 75)) for y in (0, 4, 8, 12) for x in ((0, 4, 8, 12) if y % 8 == 0 else (2, 6, 10, 14))]), "cobblestone"),
    "trottoir": ("Trottoir", lambda: texture("trottoir", (160, 158, 150), 0.05, 7, lambda d, t: (d.line((0, 0, 15, 0), fill=(130, 128, 120)), d.line((0, 0, 0, 15), fill=(130, 128, 120)))), "stone"),
    "parquet": ("Parquet en point de Hongrie", lambda: texture("parquet", (150, 100, 55), 0.12, 8,
        lambda d, t: [d.line((x, y, x + 3, y + 3), fill=(110, 70, 35)) for x in range(-4, 16, 4) for y in range(0, 16, 8)] + [d.line((x + 4, y + 4, x + 7, y + 1), fill=(110, 70, 35)) for x in range(-4, 16, 4) for y in range(0, 16, 8)]), "oak_planks"),
    "carrelage": ("Carrelage en damier", lambda: texture("carrelage", (235, 232, 222), 0.03, 9,
        lambda d, t: [d.rectangle((x, y, x + 3, y + 3), fill=(30, 30, 34)) for x in range(0, 16, 4) for y in range(0, 16, 4) if (x // 4 + y // 4) % 2 == 0]), "smooth_stone"),
    "marbre_blanc": ("Marbre blanc", lambda: texture("marbre_blanc", (236, 234, 230), 0.03, 10,
        lambda d, t: (d.line((0, 3, 6, 6, ), fill=(190, 190, 195)), d.line((6, 6, 15, 12), fill=(200, 200, 205)), d.line((3, 15, 9, 10), fill=(205, 205, 210)))), "calcite"),
    "marbre_noir": ("Marbre noir", lambda: texture("marbre_noir", (34, 32, 36), 0.08, 11,
        lambda d, t: (d.line((0, 4, 7, 8), fill=(120, 110, 100)), d.line((7, 8, 15, 5), fill=(90, 85, 80)), d.line((2, 15, 12, 11), fill=(100, 95, 90)))), "polished_blackstone"),
    "brique_parisienne": ("Brique parisienne", lambda: texture("brique_parisienne", (150, 78, 58), 0.1, 12,
        lambda d, t: [d.line((0, y, 15, y), fill=(200, 190, 175)) for y in (2, 5, 8, 11, 14)] + [d.line((x, y + 1, x, y + 2), fill=(200, 190, 175)) for i, y in enumerate((2, 5, 8, 11)) for x in ((3, 11) if i % 2 == 0 else (7, 15))]), "bricks"),
    "crepi": ("Crépi", lambda: texture("crepi", (214, 202, 178), 0.12, 13), "sandstone"),
    "devanture": ("Devanture de boutique (verte)", lambda: texture("devanture", (40, 80, 55), 0.05, 14,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(30, 60, 40)), d.line((0, 3, 15, 3), fill=(212, 175, 55)), d.rectangle((3, 6, 12, 12), outline=(60, 110, 75)))), "oak_planks"),
    "devanture_rouge": ("Devanture de boutique (rouge)", lambda: texture("devanture_rouge", (120, 30, 35), 0.05, 15,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(90, 20, 25)), d.line((0, 3, 15, 3), fill=(212, 175, 55)), d.rectangle((3, 6, 12, 12), outline=(150, 50, 55)))), "oak_planks"),
    "boiserie": ("Boiserie", lambda: texture("boiserie", (120, 82, 50), 0.06, 16,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(90, 60, 35)), d.rectangle((3, 3, 12, 12), outline=(150, 110, 70)))), "oak_planks"),
}
PAPIERS = {"rouge": ((140, 35, 40), (170, 60, 60)), "vert": ((50, 90, 60), (80, 120, 85)), "bleu": ((40, 55, 100), (70, 90, 140)), "jaune": ((200, 170, 90), (225, 200, 120))}
for c, (fond, motif) in PAPIERS.items():
    CONSTR[f"papier_peint_{c}"] = (f"Papier peint {c}", (lambda c=c, fond=fond, motif=motif: texture(f"papier_peint_{c}", fond, 0.03, 17,
        lambda d, t: [d.polygon([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], outline=motif) for x in (4, 12) for y in (4, 12)] + [d.point((8, 8), fill=motif), d.point((0, 0), fill=motif)])), "paper")
CONSTR["papier_peint_raye"] = ("Papier peint rayé", lambda: texture("papier_peint_raye", (220, 210, 185), 0.02, 18,
    lambda d, t: [d.line((x, 0, x, 15), fill=(150, 60, 70)) for x in (2, 3, 10, 11)]), "paper")
CONSTR["papier_peint_fleuri"] = ("Papier peint fleuri", lambda: texture("papier_peint_fleuri", (225, 215, 190), 0.02, 19,
    lambda d, t: [(d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(200, 90, 110)), d.point((x + 2, y + 1), fill=(90, 130, 80))) for x, y in ((3, 3), (11, 6), (6, 11), (13, 13))]), "paper")
for nom, (fr, dessiner, ing) in CONSTR.items():
    dessiner()
    modele(nom, {"all": f"ombre:block/{nom}"}, parent="minecraft:block/cube_all")
    simple(nom); objet_bloc(nom); butin(nom); tailleur(nom, ing, 2 if ing == "iron_ingot" else 1)
    NOMS[f"block.ombre.{nom}"] = fr
# Escaliers et dalles
def escalier(nom, base):
    t = {"bottom": f"ombre:block/{base}", "top": f"ombre:block/{base}", "side": f"ombre:block/{base}"}
    modele(nom, t, parent="minecraft:block/stairs"); modele(nom + "_inner", t, parent="minecraft:block/inner_stairs"); modele(nom + "_outer", t, parent="minecraft:block/outer_stairs")
    var = {}
    rot = {"east": 0, "south": 90, "west": 180, "north": 270}
    for f in ["east", "north", "south", "west"]:
        for h in ["bottom", "top"]:
            for s in ["inner_left", "inner_right", "outer_left", "outer_right", "straight"]:
                m = f"ombre:block/{nom}" + ("_inner" if s.startswith("inner") else "_outer" if s.startswith("outer") else "")
                y = rot[f]
                if s in ("inner_left", "outer_left"): y = (y - 90) % 360 if h == "bottom" else y
                if h == "top" and s in ("inner_right", "outer_right"): y = (y + 90) % 360
                v = {"model": m}
                if h == "top": v["x"] = 180
                if y: v["y"] = y
                if h == "top" or y: v["uvlock"] = True
                var[f"facing={f},half={h},shape={s}"] = v
    ecrire(f"{A}/blockstates/{nom}.json", {"variants": var}); objet_bloc(nom); butin(nom)
def dalle(nom, base):
    t = {"bottom": f"ombre:block/{base}", "top": f"ombre:block/{base}", "side": f"ombre:block/{base}"}
    modele(nom, t, parent="minecraft:block/slab"); modele(nom + "_top", t, parent="minecraft:block/slab_top")
    ecrire(f"{A}/blockstates/{nom}.json", {"variants": {"type=bottom": {"model": f"ombre:block/{nom}"}, "type=top": {"model": f"ombre:block/{nom}_top"},
                                                         "type=double": {"model": f"ombre:block/{base}"}}})
    objet_bloc(nom)
    ecrire(f"{D}/loot_table/blocks/{nom}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"ombre:{nom}",
        "functions": [{"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [{"condition": "minecraft:block_state_property", "block": f"ombre:{nom}",
                       "properties": {"type": "double"}}]}, {"function": "minecraft:explosion_decay"}]}]}]})
for base in ["zinc", "ardoise", "pierre_de_paris", "pave_parisien"]:
    escalier(base + "_escalier", base); dalle(base + "_dalle", base)
    tailleur(base + "_escalier", f"ombre:{base}"); tailleur(base + "_dalle", f"ombre:{base}", 2)
    NOMS[f"block.ombre.{base}_escalier"] = NOMS[f"block.ombre.{base}"].split(" (")[0] + " (escalier)"
    NOMS[f"block.ombre.{base}_dalle"] = NOMS[f"block.ombre.{base}"].split(" (")[0] + " (dalle)"

# ───────────── Objets ─────────────
def icone(nom, dessin, modele_parent="minecraft:item/generated"):
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(t); dessin(d, t)
    t.save(f"{A}/textures/item/{nom}.png")
    ecrire(f"{A}/models/item/{nom}.json", {"parent": modele_parent, "textures": {"layer0": f"ombre:item/{nom}"}})
icone("baguette", lambda d, t: (d.line((2, 13, 13, 2), fill=(210, 150, 70), width=3), d.line((3, 13, 13, 3), fill=(180, 120, 50)),
                                [d.point((x, 15 - x - 2), fill=(250, 220, 160)) for x in (5, 8, 11)]))
icone("croissant", lambda d, t: (d.arc((2, 4, 14, 14), 180, 360, fill=(215, 150, 60), width=4), d.line((5, 6, 6, 9), fill=(160, 100, 40)), d.line((10, 6, 9, 9), fill=(160, 100, 40))))
icone("fromage", lambda d, t: (d.polygon([(2, 12), (14, 12), (14, 6), (2, 10)], fill=(240, 200, 80)), d.polygon([(2, 10), (14, 6), (11, 4)], fill=(250, 220, 120)),
                               d.ellipse((6, 9, 8, 11), fill=(200, 160, 50)), d.ellipse((11, 8, 12, 9), fill=(200, 160, 50))))
icone("vin", lambda d, t: (d.rectangle((6, 6, 10, 14), fill=(40, 70, 40)), d.rectangle((7, 2, 9, 6), fill=(40, 70, 40)), d.rectangle((6, 9, 10, 11), fill=(235, 225, 195)),
                           d.point((8, 10), fill=(140, 20, 40)), d.rectangle((7, 1, 9, 2), fill=(140, 30, 40))))
icone("cafe", lambda d, t: (d.rectangle((4, 7, 10, 12), fill=(245, 245, 240)), d.arc((9, 8, 13, 11), 270, 90, fill=(245, 245, 240)), d.rectangle((5, 7, 9, 8), fill=(80, 50, 30)),
                            d.ellipse((2, 12, 12, 14), fill=(230, 230, 225)), d.line((6, 3, 6, 5), fill=(200, 200, 200)), d.line((8, 2, 8, 5), fill=(200, 200, 200))))
icone("ecus", lambda d, t: [(d.ellipse((x, y, x + 6, y + 6), fill=(230, 190, 60), outline=(170, 130, 30)), d.point((x + 3, y + 3), fill=(250, 230, 140))) for x, y in ((2, 8), (8, 7), (5, 3))])
icone("petard", lambda d, t: (d.rectangle((5, 5, 10, 14), fill=(190, 40, 40)), d.line((5, 7, 10, 7), fill=(240, 220, 120)), d.line((5, 12, 10, 12), fill=(240, 220, 120)),
                              d.line((8, 5, 10, 1), fill=(120, 100, 80)), d.point((11, 0), fill=(255, 200, 60))))
def flechette(couleur):
    return lambda d, t: (d.line((2, 13, 11, 4), fill=(200, 200, 205), width=1), d.line((11, 4, 14, 1), fill=(240, 240, 240)),
                         d.polygon([(2, 13), (1, 10), (4, 12)], fill=couleur), d.polygon([(2, 13), (5, 14), (3, 11)], fill=couleur))
icone("flechette_soporifique", flechette((90, 110, 200)))
icone("flechette_empoisonnee", flechette((70, 160, 50)))
icone("tenue_gardien", lambda d, t: (d.polygon([(4, 3), (11, 3), (14, 6), (12, 8), (11, 7), (11, 14), (4, 14), (4, 7), (3, 8), (1, 6)], fill=(35, 45, 80)),
                                     [d.point((8, y), fill=(200, 200, 210)) for y in (5, 8, 11)], d.rectangle((6, 1, 9, 3), fill=(30, 30, 50))))
icone("lance_grappin", lambda d, t: (d.rectangle((3, 8, 10, 11), fill=(70, 50, 35)), d.rectangle((9, 7, 13, 9), fill=(110, 110, 120)),
                                     d.line((13, 8, 15, 5), fill=(170, 170, 180)), d.line((13, 8, 15, 11), fill=(170, 170, 180)), d.line((14, 8, 15, 8), fill=(170, 170, 180)),
                                     d.rectangle((4, 11, 6, 14), fill=(60, 40, 28)), d.line((10, 9, 3, 3), fill=(150, 112, 70))), "minecraft:item/handheld")
for o in ["passant", "gardien", "gendarme", "officier"]:
    ecrire(f"{A}/models/item/oeuf_{o}.json", {"parent": "minecraft:item/template_spawn_egg"})
NOMS.update({
    "item.ombre.baguette": "Baguette", "item.ombre.croissant": "Croissant", "item.ombre.fromage": "Fromage", "item.ombre.vin": "Bouteille de vin",
    "item.ombre.cafe": "Tasse de café", "item.ombre.ecus": "Écus (à jeter : la foule accourt)", "item.ombre.petard": "Pétard (bruit et fumée)",
    "item.ombre.flechette_soporifique": "Fléchette soporifique", "item.ombre.flechette_empoisonnee": "Fléchette empoisonnée",
    "item.ombre.tenue_gardien": "Tenue de gardien (déguisement)", "item.ombre.lance_grappin": "Lance-grappin",
    "item.ombre.oeuf_passant": "Œuf de passant", "item.ombre.oeuf_gardien": "Œuf de gardien de la paix", "item.ombre.oeuf_gendarme": "Œuf de gendarme",
    "item.ombre.oeuf_officier": "Œuf d'officier",
    "entity.ombre.passant": "Passant", "entity.ombre.gardien": "Gardien de la paix", "entity.ombre.gendarme": "Gendarme",
    "entity.ombre.officier": "Officier", "entity.ombre.projectile": "Projectile",
    "effect.ombre.endormi": "Endormi", "effect.ombre.agonie": "Agonie", "effect.ombre.deguise": "Déguisé",
    "itemGroup.ombre": "L'Ombre — Assassin et parkour", "itemGroup.ombre.paris": "L'Ombre — Paris 1889", "itemGroup.ombre.objets": "L'Ombre — Objets et personnages",
})
for n, ingr, k in [("baguette", ["wheat", "wheat", "wheat"], 2), ("croissant", ["wheat", "milk_bucket", "egg"], 4), ("fromage", ["milk_bucket", "brown_mushroom"], 3),
                   ("vin", ["glass_bottle", "sweet_berries", "sweet_berries"], 1), ("cafe", ["cocoa_beans", "water_bucket", "sugar"], 2),
                   ("ecus", ["gold_nugget", "gold_nugget"], 8), ("petard", ["gunpowder", "paper", "red_dye"], 3),
                   ("flechette_soporifique", ["arrow", "poppy"], 2), ("flechette_empoisonnee", ["arrow", "spider_eye"], 2),
                   ("tenue_gardien", ["blue_wool", "blue_wool", "iron_nugget", "black_wool"], 1),
                   ("lance_grappin", ["crossbow", "tripwire_hook", "lead"], 1)]:
    sans_forme(n, ingr, k, "misc")

# ───────────── Icônes d'effets (18 × 18) ─────────────
def icone_effet(nom, dessin):
    t = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(t); dessin(d); t.save(f"{A}/textures/mob_effect/{nom}.png")
icone_effet("endormi", lambda d: (d.text((3, 2), "Z", fill=(180, 190, 255)), d.text((9, 7), "z", fill=(140, 150, 230))))
icone_effet("agonie", lambda d: (d.ellipse((4, 3, 13, 11), fill=(90, 170, 60)), d.rectangle((6, 11, 11, 15), fill=(90, 170, 60)), d.point((7, 7), fill=(0, 0, 0)), d.point((10, 7), fill=(0, 0, 0))))
icone_effet("deguise", lambda d: (d.rectangle((3, 4, 14, 8), fill=(35, 45, 80)), d.rectangle((2, 8, 15, 10), fill=(25, 30, 50)), d.rectangle((6, 10, 11, 15), fill=(230, 190, 150))))

# ───────────── Les personnages (textures de 64 × 64, au format des joueurs) ─────────────
PEAU = [(232, 190, 150), (205, 160, 120), (170, 120, 85), (120, 80, 55)]
def boite(img, u, v, w, h, dd, couleur):
    d = ImageDraw.Draw(img)
    faces = {"haut": (u + dd, v, w, dd), "bas": (u + dd + w, v, w, dd), "droite": (u, v + dd, dd, h), "devant": (u + dd, v + dd, w, h),
             "gauche": (u + dd + w, v + dd, dd, h), "dos": (u + dd + w + dd, v + dd, w, h)}
    for nom, (x, y, ww, hh) in faces.items():
        c = couleur(nom) if callable(couleur) else couleur
        d.rectangle((x, y, x + ww - 1, y + hh - 1), fill=c + (255,))
    return faces
def rangees(img, faces, rangs, couleur, sauf=("haut", "bas")):
    """Colore les rangées (depuis le haut) des 4 faces latérales."""
    for nom, (x, y, w, h) in faces.items():
        if nom in sauf: continue
        for r in rangs:
            if 0 <= r < h:
                for i in range(w): img.putpixel((x + i, y + r), couleur + (255,))
def personnage(nom, peau, cheveux, haut, bas, chaussures, details=None, chapeau=None):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # Tête
    t = boite(img, 0, 0, 8, 8, 8, peau)
    for f in ("haut", "dos"):
        x, y, w, h = t[f]; ImageDraw.Draw(img).rectangle((x, y, x + w - 1, y + h - 1), fill=cheveux + (255,))
    rangees(img, t, [0, 1], cheveux)
    x, y, w, h = t["devant"]; d = ImageDraw.Draw(img)
    d.point((x + 2, y + 4), fill=(255, 255, 255)); d.point((x + 3, y + 4), fill=(40, 30, 30))
    d.point((x + 5, y + 4), fill=(40, 30, 30)); d.point((x + 6, y + 4), fill=(255, 255, 255))
    d.line((x + 3, y + 6, x + 4, y + 6), fill=tuple(int(v * 0.75) for v in peau))
    # Corps, bras, jambes
    corps = boite(img, 16, 16, 8, 12, 4, haut)
    for (u, v) in ((40, 16), (32, 48)):
        b = boite(img, u, v, 4, 12, 4, haut); rangees(img, b, [9, 10, 11], peau)
        x, y, w, h = b["bas"]; d.rectangle((x, y, x + w - 1, y + h - 1), fill=peau + (255,))
    for (u, v) in ((0, 16), (16, 48)):
        j = boite(img, u, v, 4, 12, 4, bas); rangees(img, j, [10, 11], chaussures)
        x, y, w, h = j["bas"]; d.rectangle((x, y, x + w - 1, y + h - 1), fill=chaussures + (255,))
    if details: details(img, d, corps, t)
    if chapeau: chapeau(img, d)
    img.save(f"{A}/textures/entity/{nom}.png")
def boutons(couleur, x0=0):
    def f(img, d, corps, t):
        x, y, w, h = corps["devant"]
        for k in (2, 5, 8): d.point((x + 3 + x0, y + k), fill=couleur); d.point((x + 4 + x0, y + k), fill=couleur)
    return f
def col(c):
    def f(img, d, corps, t):
        x, y, w, h = corps["devant"]; d.rectangle((x + 3, y, x + 4, y + 2), fill=c)
    return f
def chapeau_haut_de_forme(noir=(25, 25, 28), ruban=(80, 20, 20)):
    def f(img, d):
        # Couche « chapeau » de la tête (32,0) : un bord et une calotte (sur la tête, rangées du haut)
        t = boite(img, 32, 0, 8, 8, 8, lambda n: (0, 0, 0))
        for n, (x, y, w, h) in t.items():
            for i in range(w):
                for j in range(h): img.putpixel((x + i, y + j), (0, 0, 0, 0))
        for n in ("devant", "gauche", "droite", "dos"):
            x, y, w, h = t[n]
            for i in range(w):
                img.putpixel((x + i, y), noir + (255,)); img.putpixel((x + i, y + 1), ruban + (255,)); img.putpixel((x + i, y + 2), noir + (255,))
        x, y, w, h = t["haut"]; d.rectangle((x, y, x + w - 1, y + h - 1), fill=noir + (255,))
    return f
def kepi(fond, bandeau):
    def f(img, d):
        t = boite(img, 32, 0, 8, 8, 8, lambda n: (0, 0, 0))
        for n, (x, y, w, h) in t.items():
            for i in range(w):
                for j in range(h): img.putpixel((x + i, y + j), (0, 0, 0, 0))
        for n in ("devant", "gauche", "droite", "dos"):
            x, y, w, h = t[n]
            for i in range(w):
                img.putpixel((x + i, y), fond + (255,)); img.putpixel((x + i, y + 1), fond + (255,)); img.putpixel((x + i, y + 2), bandeau + (255,))
        x, y, w, h = t["haut"]; d.rectangle((x, y, x + w - 1, y + h - 1), fill=fond + (255,))
        x, y, w, h = t["devant"]; d.line((x, y + 3, x + w - 1, y + 3), fill=(20, 20, 20, 255))   # visière
    return f
NOIR = (28, 28, 32)
personnage("passant_0", PEAU[0], (60, 45, 35), (30, 30, 36), (50, 50, 58), NOIR, col((235, 235, 235)), chapeau_haut_de_forme())      # bourgeois
personnage("passant_1", PEAU[1], (40, 30, 25), (60, 90, 160), (70, 60, 50), (60, 40, 25), boutons((30, 50, 100)), kepi((50, 50, 60), (50, 50, 60)))  # ouvrier, blouse bleue, casquette
personnage("passant_2", PEAU[0], (110, 70, 40), (110, 40, 90), (110, 40, 90), NOIR, col((240, 230, 230)), chapeau_haut_de_forme((90, 30, 70), (230, 200, 220)))  # dame
personnage("passant_3", PEAU[2], (30, 20, 15), (40, 100, 70), (40, 100, 70), NOIR, col((240, 220, 180)), chapeau_haut_de_forme((200, 180, 140), (40, 100, 70)))  # élégante
personnage("passant_4", PEAU[1], (90, 60, 35), (120, 80, 50), (120, 80, 50), (60, 40, 25),
           lambda img, d, corps, t: d.rectangle((corps["devant"][0] + 1, corps["devant"][1] + 5, corps["devant"][0] + 6, corps["devant"][1] + 11), fill=(235, 230, 220)))  # marchande, tablier
personnage("passant_5", PEAU[3], (20, 15, 10), (110, 85, 55), (80, 70, 60), (60, 40, 25), boutons((60, 45, 30)), kepi((90, 75, 55), (90, 75, 55)))  # gamin
personnage("passant_6", PEAU[0], (150, 100, 50), (60, 60, 70), (60, 60, 70), NOIR,
           lambda img, d, corps, t: d.rectangle((corps["devant"][0], corps["devant"][1], corps["devant"][0] + 7, corps["devant"][1] + 1), fill=(190, 30, 40)),
           kepi((40, 40, 45), (40, 40, 45)))  # artiste, écharpe rouge, béret
personnage("passant_7", PEAU[2], (50, 35, 25), (100, 100, 105), (60, 60, 65), NOIR, col((240, 240, 240)), chapeau_haut_de_forme((60, 50, 40), (60, 50, 40)))  # étudiant, melon
BLEU_NUIT = (32, 40, 72)
personnage("gardien", PEAU[1], (40, 30, 25), BLEU_NUIT, BLEU_NUIT, NOIR, boutons((205, 210, 220)), kepi(BLEU_NUIT, (190, 40, 40)))
personnage("gendarme", PEAU[0], (50, 40, 30), BLEU_NUIT, (220, 220, 225), NOIR,
           lambda img, d, corps, t: (d.line((corps["devant"][0], corps["devant"][1], corps["devant"][0] + 7, corps["devant"][1] + 11), fill=(240, 240, 240)),
                                     d.line((corps["devant"][0] + 7, corps["devant"][1], corps["devant"][0], corps["devant"][1] + 11), fill=(240, 240, 240))),
           chapeau_haut_de_forme(NOIR, (212, 175, 55)))
personnage("officier", PEAU[0], (90, 80, 70), BLEU_NUIT, (170, 30, 35), NOIR,
           lambda img, d, corps, t: (boutons((230, 190, 70))(img, d, corps, t), d.rectangle((corps["haut"][0], corps["haut"][1], corps["haut"][0] + 1, corps["haut"][1] + 3), fill=(230, 190, 70)),
                                     d.rectangle((corps["haut"][0] + 6, corps["haut"][1], corps["haut"][0] + 7, corps["haut"][1] + 3), fill=(230, 190, 70))),
           kepi(BLEU_NUIT, (230, 190, 70)))

# ───────────── Noms ─────────────
for l in ("fr_fr", "en_us"):
    p = f"{A}/lang/{l}.json"; dd = json.load(open(p, encoding="utf-8"))
    dd.update(NOMS)
    json.dump(dd, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(f"{len(NOMS)} noms, ressources générées.")

# ───────────── Placage des pièces qui dépassent du bloc ─────────────
# Une pièce qui sort du cube 0–16 (le dos d'une cabine de 2 blocs, une hampe, une tête de gargouille…)
# irait chercher sa texture hors de sa case, dans l'image voisine. On lui donne des coordonnées
# de texture explicites (« uv »), ramenées dans la case.
def plaquer(dossier):
    n = 0
    for f in os.listdir(dossier):
        p = os.path.join(dossier, f)
        m = json.load(open(p))
        if "elements" not in m: continue
        change = False
        for e in m["elements"]:
            a, b = e["from"], e["to"]
            dehors = any(v < 0 or v > 16 for v in a + b)
            if not dehors: continue
            for face, d in e["faces"].items():
                if "uv" in d: continue
                if face in ("north", "south"): w, h = b[0] - a[0], b[1] - a[1]
                elif face in ("east", "west"): w, h = b[2] - a[2], b[1] - a[1]
                else: w, h = b[0] - a[0], b[2] - a[2]
                d["uv"] = [0, 0, round(min(16, max(0.5, w)), 2), round(min(16, max(0.5, h)), 2)]
                change = True
        if change:
            ecrire(p, m); n += 1
    return n
print("modèles corrigés :", plaquer(f"{A}/models/block"))
