#!/usr/bin/env python3
"""
Génère les ressources des variantes, des blocs de construction, des objets, des personnages
et des effets de « L'Ombre — Prague 1600 » : modèles, états, textures, noms, recettes, butins.
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

# Cabine à rideau (2 blocs de haut). Aucune pièce ne doit en chevaucher une autre : deux faces de textures
# différentes au même endroit « clignotent » (le toit recouvrait le haut des montants et du fond).
def cabine(n, rideau):
    modele(n, {"bois": "spruce_planks", "cadre": "stripped_dark_oak_log", "rideau": rideau, "fer": "iron_block"},
           [el([0, 0, 14], [16, 30, 16], "bois", uv=[0, 0, 16, 16]),
            el([0, 0, 0], [1, 30, 14], "cadre", uv=[0, 0, 1, 16]), el([15, 0, 0], [16, 30, 14], "cadre", uv=[0, 0, 1, 16]),
            el([0, 30, 0], [16, 32, 16], "cadre", uv=[0, 0, 16, 2]),
            el([1, 0, 0.5], [15, 29, 1.2], "rideau", uv=[1, 0, 15, 16]), el([1, 29, 0], [15, 30, 0.4], "fer", uv=[0, 0, 14, 1])])
    # Le dessus et le dessous du toit gardent toute leur texture
    m = json.load(open(f"{A}/models/block/{n}.json"))
    for f in ("up", "down"): m["elements"][3]["faces"][f]["uv"] = [0, 0, 16, 16]
    ecrire(f"{A}/models/block/{n}.json", m)
cabine("cabine", "red_wool")

# ───────────── Auvents et cabines de toutes les couleurs ─────────────
for c, (m, f) in COUL_FR.items():
    n = f"auvent_{c}"
    modele(n, {"toile": f"{c}_wool", "bois": "stripped_oak_log"},
           [el([0, 9, 0], [16, 11, 16], "toile", rot={"origin": [8, 10, 0], "axis": "x", "angle": 22.5}), el([0, 10, 0], [16, 12, 1], "bois")])
    oriente(n); objet_bloc(n); butin(n); sans_forme(n, ["ombre:auvent", f"{c}_dye"]); NOMS[f"block.ombre.{n}"] = f"Auvent {m}"
    n = f"cabine_{c}"
    cabine(n, f"{c}_wool")
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
    "boulangerie": ("Boulangerie", (120, 30, 30), lambda d: (d.ellipse((3, 5, 12, 11), fill=(200, 140, 70)), d.line((5, 7, 6, 9), fill=(150, 90, 40)), d.line((8, 6, 9, 9), fill=(150, 90, 40)), d.line((10, 7, 11, 9), fill=(150, 90, 40)))),
    "brasseur": ("Brasseur", (40, 70, 40), lambda d: (d.rectangle((4, 5, 10, 13), fill=(230, 170, 40)), d.rectangle((4, 3, 10, 5), fill=(250, 250, 240)), d.arc((9, 6, 13, 11), 270, 90, fill=(140, 100, 60)), d.line((4, 8, 10, 8), fill=(140, 100, 60)))),
    "apothicaire": ("Apothicaire", (30, 60, 60), lambda d: (d.rectangle((5, 6, 10, 13), fill=(220, 220, 210)), d.rectangle((6, 3, 9, 6), fill=(180, 140, 80)), d.rectangle((6, 8, 9, 11), fill=(60, 150, 80)))),
    "horloger": ("Horloger", (30, 30, 50), lambda d: (d.ellipse((2, 2, 13, 13), fill=(240, 230, 200), outline=(212, 175, 55)), d.line((7, 7, 7, 4), fill=(20, 20, 20)), d.line((7, 7, 10, 8), fill=(20, 20, 20)))),
    "imprimeur": ("Imprimeur", (70, 40, 30), lambda d: (d.rectangle((3, 4, 12, 12), fill=(235, 225, 195)), [d.line((5, y, 10, y), fill=(30, 30, 30)) for y in (6, 8, 10)], d.rectangle((3, 3, 12, 4), fill=(30, 30, 30)))),
    "boucherie": ("Boucherie", (150, 20, 20), lambda d: (d.ellipse((3, 4, 12, 11), fill=(230, 140, 140)), d.ellipse((5, 6, 8, 9), fill=(250, 235, 230)), d.line((11, 7, 14, 4), fill=(240, 240, 240), width=2))),
    "chapelier": ("Chapelier", (200, 180, 140), lambda d: (d.rectangle((5, 4, 10, 9), fill=(25, 25, 25)), d.rectangle((2, 9, 13, 11), fill=(25, 25, 25)), d.line((10, 4, 13, 1), fill=(220, 50, 50), width=2))),
    "barbier": ("Barbier", (240, 240, 240), lambda d: [d.line((4 + i, 2, 4 + i - 3, 13), fill=(200, 30, 30) if i % 4 < 2 else (40, 60, 160), width=1) for i in range(0, 12)]),
    "cordonnier": ("Cordonnier", (90, 60, 30), lambda d: (d.polygon([(3, 4), (7, 4), (7, 9), (13, 10), (13, 12), (3, 12)], fill=(30, 20, 15)), d.line((3, 12, 13, 12), fill=(150, 120, 80)))),
    "alchimiste": ("Alchimiste", (40, 20, 60), lambda d: (d.ellipse((4, 6, 11, 13), fill=(80, 200, 120)), d.rectangle((6, 2, 9, 6), fill=(200, 200, 210)), d.point((6, 9), fill=(220, 255, 220)), d.point((9, 11), fill=(220, 255, 220)), d.polygon([(12, 2), (13, 4), (15, 4), (13, 5), (14, 7), (12, 6), (10, 7), (11, 5), (9, 4), (11, 4)], fill=(240, 200, 60)))),
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
    sans_forme(n, ["ombre:enseigne", {"boulangerie": "bread", "brasseur": "wheat", "apothicaire": "glistering_melon_slice", "horloger": "clock",
                   "imprimeur": "book", "boucherie": "porkchop", "chapelier": "black_wool", "barbier": "shears", "cordonnier": "leather_boots",
                   "alchimiste": "glowstone_dust", "tailleur": "string", "marchand_vin": "glass_bottle"}[b]])
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
    "proclamation": ("Proclamation impériale", (240, 230, 205), (30, 30, 30), lambda d: (d.ellipse((5, 4, 10, 9), fill=(212, 175, 55)), d.point((7, 6), fill=(30, 30, 30)), [d.line((3, y, 12, y), fill=(60, 60, 60)) for y in (10, 11)])),
    "recherche": ("Recherché", (225, 210, 170), (120, 20, 20), lambda d: (d.ellipse((5, 4, 10, 10), fill=(60, 50, 45)), d.rectangle((6, 8, 9, 11), fill=(40, 40, 40)), d.line((5, 6, 10, 6), fill=(20, 20, 20)))),
    "comediens": ("Comédiens ambulants", (150, 40, 40), (240, 210, 120), lambda d: (d.ellipse((3, 4, 7, 9), fill=(250, 240, 220)), d.ellipse((8, 5, 12, 10), fill=(30, 30, 30)), d.arc((4, 6, 6, 8), 0, 180, fill=(30, 30, 30)))),
    "foire": ("Grande foire", (240, 200, 60), (150, 30, 30), lambda d: (d.polygon([(8, 3), (13, 11), (3, 11)], fill=(200, 40, 40)), d.line((8, 3, 8, 11), fill=(250, 250, 250)), d.line((5, 11, 11, 11), fill=(90, 60, 30)))),
    "almanach": ("Almanach", (220, 200, 160), (40, 40, 90), lambda d: (d.ellipse((4, 4, 11, 11), fill=(250, 210, 60)), [d.point((x, y), fill=(250, 210, 60)) for x, y in ((2, 4), (13, 5), (3, 11), (12, 11))], d.arc((6, 6, 9, 9), 0, 180, fill=(120, 80, 20)))),
    "comete": ("La comète", (20, 20, 50), (220, 200, 120), lambda d: (d.ellipse((9, 4, 12, 7), fill=(255, 250, 200)), d.line((9, 6, 2, 11), fill=(200, 200, 230)), d.line((10, 7, 4, 12), fill=(150, 150, 200)), [d.point(p, fill=(250, 250, 250)) for p in ((3, 4), (6, 9), (12, 11))])),
    "remede": ("Remède miracle", (120, 160, 120), (60, 30, 20), lambda d: (d.rectangle((6, 6, 9, 12), fill=(200, 80, 160)), d.rectangle((7, 3, 8, 6), fill=(220, 220, 220)), d.line((4, 7, 5, 6), fill=(250, 250, 250)), d.line((10, 6, 11, 7), fill=(250, 250, 250)))),
    "carte_du_ciel": ("Carte du ciel", (30, 40, 80), (212, 175, 55), lambda d: (d.ellipse((2, 3, 13, 12), outline=(212, 175, 55)), d.ellipse((6, 6, 9, 9), fill=(250, 200, 60)), d.point((3, 7), fill=(250, 250, 250)), d.point((11, 5), fill=(250, 250, 250)))),
    "taverne": ("À la taverne", (110, 70, 40), (240, 220, 160), lambda d: (d.rectangle((4, 5, 9, 12), fill=(230, 170, 40)), d.rectangle((4, 4, 9, 5), fill=(250, 250, 240)), d.arc((8, 6, 12, 10), 270, 90, fill=(80, 50, 30)))),
    "joute": ("Joute", (40, 80, 140), (240, 220, 160), lambda d: (d.line((2, 12, 13, 4), fill=(220, 200, 150), width=1), d.polygon([(5, 9), (8, 7), (9, 11), (6, 12)], fill=(180, 30, 30)), d.polygon([(10, 4), (13, 3), (12, 6)], fill=(240, 240, 240)))),
    "bal_masque": ("Bal masqué", (80, 30, 90), (240, 220, 160), lambda d: (d.ellipse((3, 5, 7, 9), fill=(250, 240, 230)), d.ellipse((8, 5, 12, 9), fill=(250, 240, 230)), d.rectangle((6, 6, 8, 7), fill=(250, 240, 230)), d.point((5, 7), fill=(20, 20, 20)), d.point((10, 7), fill=(20, 20, 20)))),
    "gazette": ("Gazette", (238, 232, 215), (30, 30, 30), lambda d: [d.line((3, y, 7, y), fill=(60, 60, 60)) for y in (5, 7, 9, 11)] + [d.line((9, y, 12, y), fill=(60, 60, 60)) for y in (5, 7, 9, 11)]),
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
def tuiles(d, t):
    # Des tuiles rouges en écailles, rang après rang
    for y in range(0, 16, 4):
        for x in range(-2 if (y // 4) % 2 else 0, 16, 4):
            d.arc((x, y - 2, x + 4, y + 3), 0, 180, fill=(110, 42, 28))
        d.line((0, y + 3, 15, y + 3), fill=(120, 48, 32))
CONSTR = {
    "pierre_de_taille": ("Pierre de taille", lambda: texture("pierre_de_taille", CREME, 0.07, 1, appareil), "smooth_sandstone"),
    "pierre_lisse": ("Pierre lisse", lambda: texture("pierre_lisse", (226, 212, 180), 0.05, 2), "smooth_sandstone"),
    "pierre_sculptee": ("Pierre sculptée", lambda: texture("pierre_sculptee", CREME, 0.06, 3,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(185, 170, 135)), d.ellipse((5, 5, 10, 10), outline=(185, 170, 135)),
                      d.ellipse((7, 7, 8, 8), fill=(185, 170, 135)), d.rectangle((2, 2, 13, 13), outline=(240, 228, 200)))), "smooth_sandstone"),
    "pierre_noircie": ("Pierre gothique noircie", lambda: texture("pierre_noircie", (88, 84, 82), 0.12, 20,
        lambda d, t: appareil(d, t, (60, 57, 56))), "stone_bricks"),
    "tuiles": ("Tuiles rouges", lambda: texture("tuiles", (168, 66, 44), 0.1, 4, tuiles), "brick"),
    "ardoise": ("Ardoise (toit)", lambda: texture("ardoise", (62, 68, 82), 0.1, 5,
        lambda d, t: [d.line((0, y, 15, y), fill=(40, 44, 55)) for y in (3, 7, 11, 15)] + [d.line((x, y - 3, x, y), fill=(40, 44, 55)) for y in (3, 7, 11, 15) for x in ((2, 10) if y % 8 == 3 else (6, 14))]), "deepslate"),
    "pave": ("Pavés", lambda: texture("pave", (118, 116, 112), 0.14, 6,
        lambda d, t: [d.rectangle((x, y, x + 3, y + 3), outline=(80, 78, 75)) for y in (0, 4, 8, 12) for x in ((0, 4, 8, 12) if y % 8 == 0 else (2, 6, 10, 14))]), "cobblestone"),
    "dalles_de_pierre": ("Dalles de pierre", lambda: texture("dalles_de_pierre", (160, 158, 150), 0.05, 7, lambda d, t: (d.line((0, 0, 15, 0), fill=(130, 128, 120)), d.line((0, 0, 0, 15), fill=(130, 128, 120)), d.line((0, 8, 15, 8), fill=(130, 128, 120)))), "stone"),
    "parquet": ("Parquet en point de Hongrie", lambda: texture("parquet", (150, 100, 55), 0.12, 8,
        lambda d, t: [d.line((x, y, x + 3, y + 3), fill=(110, 70, 35)) for x in range(-4, 16, 4) for y in range(0, 16, 8)] + [d.line((x + 4, y + 4, x + 7, y + 1), fill=(110, 70, 35)) for x in range(-4, 16, 4) for y in range(0, 16, 8)]), "oak_planks"),
    "carrelage": ("Carrelage en damier", lambda: texture("carrelage", (235, 232, 222), 0.03, 9,
        lambda d, t: [d.rectangle((x, y, x + 3, y + 3), fill=(30, 30, 34)) for x in range(0, 16, 4) for y in range(0, 16, 4) if (x // 4 + y // 4) % 2 == 0]), "smooth_stone"),
    "marbre_blanc": ("Marbre blanc", lambda: texture("marbre_blanc", (236, 234, 230), 0.03, 10,
        lambda d, t: (d.line((0, 3, 6, 6, ), fill=(190, 190, 195)), d.line((6, 6, 15, 12), fill=(200, 200, 205)), d.line((3, 15, 9, 10), fill=(205, 205, 210)))), "calcite"),
    "marbre_noir": ("Marbre noir", lambda: texture("marbre_noir", (34, 32, 36), 0.08, 11,
        lambda d, t: (d.line((0, 4, 7, 8), fill=(120, 110, 100)), d.line((7, 8, 15, 5), fill=(90, 85, 80)), d.line((2, 15, 12, 11), fill=(100, 95, 90)))), "polished_blackstone"),
    "brique_ancienne": ("Brique ancienne", lambda: texture("brique_ancienne", (150, 78, 58), 0.1, 12,
        lambda d, t: [d.line((0, y, 15, y), fill=(200, 190, 175)) for y in (2, 5, 8, 11, 14)] + [d.line((x, y + 1, x, y + 2), fill=(200, 190, 175)) for i, y in enumerate((2, 5, 8, 11)) for x in ((3, 11) if i % 2 == 0 else (7, 15))]), "bricks"),
    "crepi": ("Crépi blanc", lambda: texture("crepi", (226, 218, 198), 0.1, 13), "sandstone"),
    "sgraffite": ("Sgraffite (façade Renaissance)", lambda: texture("sgraffite", (232, 226, 210), 0.03, 21,
        lambda d, t: [d.polygon([(x, y - 3), (x + 3, y), (x, y + 3), (x - 3, y)], fill=(40, 38, 36)) for x in (4, 12) for y in (4, 12)]
                     + [d.polygon([(x, y - 1), (x + 1, y), (x, y + 1), (x - 1, y)], fill=(232, 226, 210)) for x in (4, 12) for y in (4, 12)]
                     + [d.line((0, 0, 15, 0), fill=(40, 38, 36)), d.line((0, 8, 15, 8), fill=(40, 38, 36))]), "quartz_block"),
    "devanture": ("Devanture de boutique (verte)", lambda: texture("devanture", (40, 80, 55), 0.05, 14,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(30, 60, 40)), d.line((0, 3, 15, 3), fill=(212, 175, 55)), d.rectangle((3, 6, 12, 12), outline=(60, 110, 75)))), "oak_planks"),
    "devanture_rouge": ("Devanture de boutique (rouge)", lambda: texture("devanture_rouge", (120, 30, 35), 0.05, 15,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(90, 20, 25)), d.line((0, 3, 15, 3), fill=(212, 175, 55)), d.rectangle((3, 6, 12, 12), outline=(150, 50, 55)))), "oak_planks"),
    "boiserie": ("Boiserie", lambda: texture("boiserie", (120, 82, 50), 0.06, 16,
        lambda d, t: (d.rectangle((1, 1, 14, 14), outline=(90, 60, 35)), d.rectangle((3, 3, 12, 12), outline=(150, 110, 70)))), "oak_planks"),
}
for c, (fr, rgb) in {"jaune": ("jaune", (232, 206, 128)), "rose": ("rose", (226, 170, 160)), "bleu": ("bleu", (160, 186, 206)),
                     "vert": ("vert", (170, 196, 150)), "ocre": ("ocre", (206, 150, 86))}.items():
    CONSTR[f"crepi_{c}"] = (f"Crépi {fr}", (lambda c=c, rgb=rgb: texture(f"crepi_{c}", rgb, 0.08, 30 + len(c))), "sandstone")
TAPIS = {"rouge": ("rouge", (140, 35, 40), (170, 60, 60)), "verte": ("verte", (50, 90, 60), (80, 120, 85)),
         "bleue": ("bleue", (40, 55, 100), (70, 90, 140)), "jaune": ("jaune", (200, 170, 90), (225, 200, 120))}
for c, (fr, fond, motif) in TAPIS.items():
    CONSTR[f"tapisserie_{c}"] = (f"Tapisserie {fr}", (lambda c=c, fond=fond, motif=motif: texture(f"tapisserie_{c}", fond, 0.03, 17,
        lambda d, t: [d.polygon([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], outline=motif) for x in (4, 12) for y in (4, 12)] + [d.point((8, 8), fill=motif), d.point((0, 0), fill=motif)])), "paper")
CONSTR["tapisserie_rayee"] = ("Tapisserie rayée", lambda: texture("tapisserie_rayee", (220, 210, 185), 0.02, 18,
    lambda d, t: [d.line((x, 0, x, 15), fill=(150, 60, 70)) for x in (2, 3, 10, 11)]), "paper")
CONSTR["tapisserie_fleurie"] = ("Tapisserie fleurie", lambda: texture("tapisserie_fleurie", (225, 215, 190), 0.02, 19,
    lambda d, t: [(d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(200, 90, 110)), d.point((x + 2, y + 1), fill=(90, 130, 80))) for x, y in ((3, 3), (11, 6), (6, 11), (13, 13))]), "paper")
for nom, (fr, dessiner, ing) in CONSTR.items():
    dessiner()
    modele(nom, {"all": f"ombre:block/{nom}"}, parent="minecraft:block/cube_all")
    simple(nom); objet_bloc(nom); butin(nom); tailleur(nom, ing, 1)
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
for base in ["tuiles", "ardoise", "pierre_de_taille", "pave"]:
    escalier(base + "_escalier", base); dalle(base + "_dalle", base)
    tailleur(base + "_escalier", f"ombre:{base}"); tailleur(base + "_dalle", f"ombre:{base}", 2)
    NOMS[f"block.ombre.{base}_escalier"] = NOMS[f"block.ombre.{base}"].split(" (")[0] + " (escalier)"
    NOMS[f"block.ombre.{base}_dalle"] = NOMS[f"block.ombre.{base}"].split(" (")[0] + " (dalle)"

# ───────────── Objets ─────────────
def icone(nom, dessin, modele_parent="minecraft:item/generated"):
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(t); dessin(d, t)
    t.save(f"{A}/textures/item/{nom}.png")
    ecrire(f"{A}/models/item/{nom}.json", {"parent": modele_parent, "textures": {"layer0": f"ombre:item/{nom}"}})
icone("pain_seigle", lambda d, t: (d.ellipse((2, 4, 14, 13), fill=(120, 80, 50)), d.ellipse((3, 5, 13, 11), fill=(140, 95, 60)),
                                   [d.line((x, 6, x + 2, 9), fill=(95, 60, 35)) for x in (5, 8, 11)]))
icone("pain_epices", lambda d, t: (d.rectangle((3, 4, 12, 12), fill=(170, 100, 50)), d.rectangle((4, 5, 11, 11), outline=(240, 230, 210)),
                                   d.ellipse((6, 6, 9, 9), outline=(200, 40, 40)), d.point((7, 7), fill=(240, 230, 210))))
icone("fromage", lambda d, t: (d.polygon([(2, 12), (14, 12), (14, 6), (2, 10)], fill=(240, 200, 80)), d.polygon([(2, 10), (14, 6), (11, 4)], fill=(250, 220, 120)),
                               d.ellipse((6, 9, 8, 11), fill=(200, 160, 50)), d.ellipse((11, 8, 12, 9), fill=(200, 160, 50))))
icone("vin", lambda d, t: (d.rectangle((6, 6, 10, 14), fill=(40, 70, 40)), d.rectangle((7, 2, 9, 6), fill=(40, 70, 40)), d.rectangle((6, 9, 10, 11), fill=(235, 225, 195)),
                           d.point((8, 10), fill=(140, 20, 40)), d.rectangle((7, 1, 9, 2), fill=(140, 30, 40))))
icone("biere", lambda d, t: (d.rectangle((3, 5, 10, 14), fill=(220, 160, 40)), d.rectangle((3, 3, 10, 5), fill=(250, 250, 240)), d.arc((9, 6, 14, 12), 270, 90, fill=(130, 100, 70)),
                             d.line((3, 9, 10, 9), fill=(130, 100, 70)), d.line((3, 14, 10, 14), fill=(130, 100, 70))))
icone("thalers", lambda d, t: [(d.ellipse((x, y, x + 6, y + 6), fill=(200, 205, 212), outline=(130, 135, 145)), d.point((x + 3, y + 3), fill=(240, 240, 245))) for x, y in ((2, 8), (8, 7), (5, 3))])
icone("petard", lambda d, t: (d.rectangle((5, 5, 10, 14), fill=(190, 40, 40)), d.line((5, 7, 10, 7), fill=(240, 220, 120)), d.line((5, 12, 10, 12), fill=(240, 220, 120)),
                              d.line((8, 5, 10, 1), fill=(120, 100, 80)), d.point((11, 0), fill=(255, 200, 60))))
def flechette(couleur):
    return lambda d, t: (d.line((2, 13, 11, 4), fill=(200, 200, 205), width=1), d.line((11, 4, 14, 1), fill=(240, 240, 240)),
                         d.polygon([(2, 13), (1, 10), (4, 12)], fill=couleur), d.polygon([(2, 13), (5, 14), (3, 11)], fill=couleur))
icone("flechette_soporifique", flechette((90, 110, 200)))
icone("flechette_empoisonnee", flechette((70, 160, 50)))
icone("flechette_folie", flechette((190, 40, 80)))
icone("tenue_hallebardier", lambda d, t: (d.polygon([(4, 4), (11, 4), (14, 7), (12, 9), (11, 8), (11, 14), (4, 14), (4, 8), (3, 9), (1, 7)], fill=(180, 40, 40)),
                                     [d.line((x, 5, x, 14), fill=(240, 200, 40)) for x in (6, 9)], d.polygon([(5, 1), (10, 1), (12, 3), (3, 3)], fill=(190, 195, 205))))
icone("lance_grappin", lambda d, t: (d.rectangle((3, 8, 10, 11), fill=(70, 50, 35)), d.rectangle((9, 7, 13, 9), fill=(110, 110, 120)),
                                     d.line((13, 8, 15, 5), fill=(170, 170, 180)), d.line((13, 8, 15, 11), fill=(170, 170, 180)), d.line((14, 8, 15, 8), fill=(170, 170, 180)),
                                     d.rectangle((4, 11, 6, 14), fill=(60, 40, 28)), d.line((10, 9, 3, 3), fill=(150, 112, 70))), "minecraft:item/handheld")
for o in ["passant", "personnage", "hallebardier", "arbaletrier", "capitaine"]:
    ecrire(f"{A}/models/item/oeuf_{o}.json", {"parent": "minecraft:item/template_spawn_egg"})
NOMS.update({
    "item.ombre.pain_seigle": "Pain de seigle", "item.ombre.pain_epices": "Pain d'épices", "item.ombre.fromage": "Fromage", "item.ombre.vin": "Bouteille de vin",
    "item.ombre.biere": "Chope de bière de Bohême", "item.ombre.thalers": "Thalers (à jeter : la foule accourt)", "item.ombre.petard": "Pétard (bruit et fumée)",
    "item.ombre.flechette_soporifique": "Fléchette soporifique", "item.ombre.flechette_empoisonnee": "Fléchette empoisonnée", "item.ombre.flechette_folie": "Fléchette de folie",
    "item.ombre.tenue_hallebardier": "Tenue de hallebardier (déguisement)", "item.ombre.lance_grappin": "Arbalète à grappin",
    "item.ombre.oeuf_passant": "Œuf de passant", "item.ombre.oeuf_personnage": "Œuf de personnage historique",
    "item.ombre.oeuf_hallebardier": "Œuf de hallebardier", "item.ombre.oeuf_arbaletrier": "Œuf d'arbalétrier", "item.ombre.oeuf_capitaine": "Œuf de capitaine",
    "entity.ombre.passant": "Passant", "entity.ombre.personnage": "Personnage historique", "entity.ombre.hallebardier": "Hallebardier",
    "entity.ombre.arbaletrier": "Arbalétrier", "entity.ombre.capitaine": "Capitaine", "entity.ombre.projectile": "Projectile",
    "effect.ombre.endormi": "Endormi", "effect.ombre.agonie": "Agonie", "effect.ombre.deguise": "Déguisé", "effect.ombre.folie": "Folie", "key.ombre.aigle": "Vue de l'aigle",
    "itemGroup.ombre": "L'Ombre — Assassin et parkour", "itemGroup.ombre.prague": "L'Ombre — Prague 1600", "itemGroup.ombre.objets": "L'Ombre — Objets et personnages",
})
for n, ingr, k in [("pain_seigle", ["wheat", "wheat", "wheat"], 2), ("pain_epices", ["wheat", "honey_bottle", "sugar"], 4), ("fromage", ["milk_bucket", "brown_mushroom"], 3),
                   ("vin", ["glass_bottle", "sweet_berries", "sweet_berries"], 1), ("biere", ["glass_bottle", "wheat", "wheat"], 2),
                   ("thalers", ["iron_nugget", "iron_nugget"], 8), ("petard", ["gunpowder", "paper", "red_dye"], 3),
                   ("flechette_soporifique", ["arrow", "poppy"], 2), ("flechette_empoisonnee", ["arrow", "spider_eye"], 2), ("flechette_folie", ["arrow", "fermented_spider_eye"], 2),
                   ("tenue_hallebardier", ["red_wool", "yellow_wool", "iron_ingot", "iron_nugget"], 1),
                   ("lance_grappin", ["crossbow", "tripwire_hook", "lead"], 1)]:
    sans_forme(n, ingr, k, "misc")

# ───────────── Icônes d'effets (18 × 18) ─────────────
def icone_effet(nom, dessin):
    t = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(t); dessin(d); t.save(f"{A}/textures/mob_effect/{nom}.png")
icone_effet("endormi", lambda d: (d.text((3, 2), "Z", fill=(180, 190, 255)), d.text((9, 7), "z", fill=(140, 150, 230))))
icone_effet("agonie", lambda d: (d.ellipse((4, 3, 13, 11), fill=(90, 170, 60)), d.rectangle((6, 11, 11, 15), fill=(90, 170, 60)), d.point((7, 7), fill=(0, 0, 0)), d.point((10, 7), fill=(0, 0, 0))))
icone_effet("folie", lambda d: (d.ellipse((3, 3, 13, 13), outline=(200, 40, 80)), d.line((5, 5, 11, 11), fill=(200, 40, 80)), d.line((11, 5, 5, 11), fill=(200, 40, 80))))
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
BLANC = (238, 236, 228)
def fraise(img, d, corps, t):
    """La fraise : le grand col plissé blanc de 1600, en haut du torse et en bas de la tête."""
    for n in ("devant", "dos", "gauche", "droite"):
        x, y, w, h = corps[n]; d.line((x, y, x + w - 1, y), fill=BLANC)
        x, y, w, h = t[n]; d.line((x, y + h - 1, x + w - 1, y + h - 1), fill=BLANC)
def barbe(couleur, longue=False):
    def f(img, d, corps, t):
        x, y, w, h = t["devant"]
        d.rectangle((x + 1, y + 6, x + 6, y + 7), fill=couleur); d.line((x + 2, y + 5, x + 5, y + 5), fill=couleur)
        if longue:
            for n in ("devant",):
                cx, cy, cw, ch = corps[n]; d.rectangle((cx + 1, cy, cx + 6, cy + 5), fill=couleur)
    return f
def chapeau_plat(c, plume=None):
    def f(img, d):
        t = boite(img, 32, 0, 8, 8, 8, lambda n: (0, 0, 0))
        for n, (x, y, w, h) in t.items():
            for i in range(w):
                for j in range(h): img.putpixel((x + i, y + j), (0, 0, 0, 0))
        for n in ("devant", "gauche", "droite", "dos"):
            x, y, w, h = t[n]
            for i in range(w): img.putpixel((x + i, y), c + (255,)); img.putpixel((x + i, y + 1), c + (255,))
        x, y, w, h = t["haut"]; d.rectangle((x, y, x + w - 1, y + h - 1), fill=c + (255,))
        if plume:
            x, y, w, h = t["droite"]; d.line((x + 1, y, x + 3, y + 1), fill=plume + (255,))
    return f
def morion(img, d):
    t = boite(img, 32, 0, 8, 8, 8, lambda n: (0, 0, 0))
    for n, (x, y, w, h) in t.items():
        for i in range(w):
            for j in range(h): img.putpixel((x + i, y + j), (0, 0, 0, 0))
    acier, clair = (170, 175, 185), (215, 220, 228)
    for n in ("devant", "gauche", "droite", "dos"):
        x, y, w, h = t[n]
        for i in range(w):
            img.putpixel((x + i, y), clair + (255,)); img.putpixel((x + i, y + 1), acier + (255,)); img.putpixel((x + i, y + 2), acier + (255,))
    x, y, w, h = t["haut"]; d.rectangle((x, y, x + w - 1, y + h - 1), fill=acier + (255,)); d.line((x, y + 3, x + w - 1, y + 3), fill=clair + (255,))
def plusieurs(*fs):
    def f(img, d, corps, t):
        for g in fs: g(img, d, corps, t)
    return f
def tablier(c):
    def f(img, d, corps, t):
        x, y, w, h = corps["devant"]; d.rectangle((x + 1, y + 5, x + 6, y + 11), fill=c)
    return f
def chaine_or(img, d, corps, t):
    x, y, w, h = corps["devant"]; d.line((x, y + 1, x + 4, y + 4), fill=(225, 185, 60)); d.line((x + 4, y + 4, x + 7, y + 1), fill=(225, 185, 60))
def livree(c1, c2):
    def f(img, d, corps, t):
        x, y, w, h = corps["devant"]; d.rectangle((x, y, x + 3, y + h - 1), fill=c1); d.rectangle((x + 4, y, x + 7, y + h - 1), fill=c2)
    return f
# Les passants de 1600
personnage("passant_0", PEAU[0], (60, 45, 35), (30, 30, 36), (40, 40, 46), NOIR, fraise, chapeau_plat((30, 30, 34)))            # bourgeois
personnage("passant_1", PEAU[1], (90, 60, 35), (120, 85, 50), (80, 65, 45), (60, 40, 25), tablier((200, 190, 170)), chapeau_plat((110, 90, 60)))  # artisan
personnage("passant_2", PEAU[0], (150, 100, 50), (160, 40, 40), (160, 40, 40), (60, 40, 25), tablier((235, 230, 220)), chapeau_plat(BLANC))  # paysanne, fichu blanc
personnage("passant_3", PEAU[0], (60, 40, 25), (30, 70, 50), (30, 70, 50), NOIR, plusieurs(fraise, chaine_or), chapeau_plat((30, 70, 50), (240, 240, 240)))  # dame
personnage("passant_4", PEAU[2], (40, 30, 20), (50, 70, 130), (50, 70, 130), NOIR, tablier((235, 230, 220)))                    # marchande
personnage("passant_5", PEAU[1], (110, 70, 30), (110, 85, 55), (90, 75, 60), (60, 40, 25), None, chapeau_plat((90, 75, 55)))     # gamin
personnage("passant_6", PEAU[2], (80, 60, 40), (95, 70, 45), (95, 70, 45), (60, 40, 25),
           lambda img, d, corps, t: d.line((corps["devant"][0], corps["devant"][1] + 6, corps["devant"][0] + 7, corps["devant"][1] + 6), fill=(230, 220, 190)))  # moine, cordelière
personnage("passant_7", PEAU[1], (50, 35, 25), (25, 25, 30), (25, 25, 30), NOIR, fraise, chapeau_plat((25, 25, 30)))            # étudiant
# La garde de la ville
personnage("hallebardier", PEAU[1], (60, 40, 25), (180, 40, 40), (240, 200, 40), NOIR, livree((180, 40, 40), (240, 200, 40)), morion)
personnage("arbaletrier", PEAU[0], (90, 60, 35), (110, 80, 50), (70, 60, 50), (60, 40, 25),
           lambda img, d, corps, t: d.line((corps["devant"][0], corps["devant"][1], corps["devant"][0] + 7, corps["devant"][1] + 11), fill=(60, 40, 25)), morion)
personnage("capitaine", PEAU[0], (40, 30, 20), (25, 25, 28), (25, 25, 28), NOIR, plusieurs(fraise, chaine_or, barbe((40, 30, 20))), chapeau_plat((25, 25, 28), (240, 240, 240)))
# Les personnages réels
def nez_or(img, d, corps, t):
    x, y, w, h = t["devant"]; d.point((x + 3, y + 5), fill=(225, 185, 60)); d.point((x + 4, y + 5), fill=(225, 185, 60))
personnage("tycho_brahe", PEAU[0], (170, 90, 40), (40, 30, 30), (40, 30, 30), NOIR,
           plusieurs(fraise, chaine_or, barbe((170, 90, 40)), nez_or), chapeau_plat((40, 30, 30)))
personnage("kepler", PEAU[0], (50, 35, 25), (30, 30, 34), (30, 30, 34), NOIR, plusieurs(fraise, barbe((50, 35, 25))))
personnage("rodolphe_ii", PEAU[0], (90, 60, 35), (20, 20, 22), (20, 20, 22), NOIR,
           plusieurs(fraise, chaine_or, barbe((90, 60, 35)), lambda img, d, corps, t: d.line((corps["devant"][0], corps["devant"][1] + 7, corps["devant"][0] + 7, corps["devant"][1] + 7), fill=(160, 30, 30))),
           chapeau_plat((20, 20, 22), (230, 190, 70)))
personnage("rabbi_loew", PEAU[1], (220, 220, 220), (20, 20, 25), (20, 20, 25), NOIR, barbe((230, 230, 230), True), chapeau_plat((20, 20, 25)))

# ───────────── Le mobilier de 1600 ─────────────
# Horloge astronomique : un cadran bleu, le zodiaque doré, le soleil et la lune
t = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(t)
d.ellipse((0, 0, 15, 15), fill=(212, 175, 55)); d.ellipse((1, 1, 14, 14), fill=(30, 60, 140)); d.ellipse((4, 4, 11, 11), outline=(212, 175, 55))
d.pieslice((1, 1, 14, 14), 200, 340, fill=(20, 30, 60)); d.ellipse((6, 6, 9, 9), fill=(240, 200, 60))
for k in range(12):
    a = k * math.pi / 6; d.point((int(7.5 + math.cos(a) * 6), int(7.5 + math.sin(a) * 6)), fill=(240, 220, 120))
d.line((7, 7, 13, 4), fill=(240, 220, 120)); t.save(f"{A}/textures/block/horloge_astronomique.png")
modele("horloge", {"or": "gold_block", "cadran": "ombre:block/horloge_astronomique"},
       [el([0, 0, 0], [16, 16, 1], "or"), el([0, 0, 1], [16, 16, 1.4], "cadran", ["north", "south"], uv=[0, 0, 16, 16])])
# Bannière impériale : l'aigle noir sur fond d'or
t = Image.new("RGBA", (16, 16), (232, 190, 50, 255)); d = ImageDraw.Draw(t)
d.polygon([(8, 3), (10, 6), (14, 4), (12, 9), (9, 9), (10, 13), (8, 11), (6, 13), (7, 9), (4, 9), (2, 4), (6, 6)], fill=(25, 25, 25))
d.point((8, 4), fill=(200, 40, 40)); t.save(f"{A}/textures/block/banniere.png")
modele("drapeau", {"hampe": "stripped_dark_oak_log", "or": "gold_block", "toile": "ombre:block/banniere"},
       [el([7.5, 0, 7.5], [8.5, 31, 8.5], "hampe"), el([7, 31, 7], [9, 32, 9], "or"), el([8.5, 18, 7.8], [16, 30, 8.2], "toile", ["north", "south"], uv=[0, 0, 16, 16])])
# Charpente : un treillis de poutres, ajouré
t = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(t)
bois, clair = (92, 62, 38, 255), (120, 84, 52, 255)
d.rectangle((0, 0, 15, 1), fill=bois); d.rectangle((0, 14, 15, 15), fill=bois); d.rectangle((0, 0, 1, 15), fill=bois); d.rectangle((14, 0, 15, 15), fill=bois)
d.line((1, 1, 14, 14), fill=clair, width=2); d.line((14, 1, 1, 14), fill=bois, width=2)
t.save(f"{A}/textures/block/charpente.png")
modele("charpente", {"bois": "ombre:block/charpente"}, [el([0, 0, 0], [16, 16, 16], "bois")]); simple("charpente"); objet_bloc("charpente"); butin("charpente")
sans_forme("charpente", ["#minecraft:logs", "stick", "stick"], 6)
# Colonne d'annonces en bois, au chapiteau de cuivre
modele("colonne", {"affiches": "ombre:block/affiches_colonne", "bois": "dark_oak_planks", "cuivre": "oxidized_copper"},
       [el([1, 0, 1], [15, 2, 15], "bois"), el([2, 2, 2], [14, 26, 14], "affiches"), el([1, 26, 1], [15, 28, 15], "bois"),
        el([3, 28, 3], [13, 30, 13], "cuivre"), el([5, 30, 5], [11, 31.5, 11], "cuivre"), el([7, 31.5, 7], [9, 32, 9], "cuivre")])
# Poteau de lanterne et lanterne (bois et fer forgé)
modele("lampadaire", {"bois": "stripped_dark_oak_log"}, [el([6, 0, 6], [10, 16, 10], "bois"), el([5, 0, 5], [11, 1, 11], "bois")])
modele("tete_lampadaire", {"fer": "polished_blackstone", "verre": "glass", "feu": "glowstone", "bois": "stripped_dark_oak_log"},
       [el([6, 0, 6], [10, 3, 10], "bois"), el([4, 3, 4], [12, 4, 12], "fer"), el([4.5, 4, 4.5], [11.5, 11, 11.5], "verre"), el([6, 5, 6], [10, 10, 10], "feu"),
        el([3.5, 11, 3.5], [12.5, 12.5, 12.5], "fer"), el([5, 12.5, 5], [11, 14, 11], "fer"), el([7, 14, 7], [9, 16, 9], "fer")])
# Carrosse
modele("carrosse", {"noir": "black_concrete", "verre": "glass", "roue": "stripped_dark_oak_log", "or": "gold_block", "rouge": "red_terracotta"},
       [el([1, 4, 1], [15, 14, 15], "noir"), el([0, 14, 0], [16, 15, 16], "noir"), el([2, 8, 0.9], [14, 12, 1], "verre", ["north"]), el([2, 8, 15], [14, 12, 15.1], "verre", ["south"]),
        el([-0.5, 0, 1], [0.5, 8, 7], "roue"), el([15.5, 0, 1], [16.5, 8, 7], "roue"), el([-0.5, 0, 9], [0.5, 8, 15], "roue"), el([15.5, 0, 9], [16.5, 8, 15], "roue"),
        el([1, 4.5, -4], [15, 5.5, 1], "rouge"), el([7, 15, 7], [9, 16, 9], "or"), el([1, 10, 15.1], [15, 11, 15.2], "or", ["south"])])
oriente("carrosse"); objet_bloc("carrosse"); butin("carrosse"); sans_forme("carrosse", ["minecart", "black_wool", "gold_nugget"])
# Statue de pont (2 blocs de haut)
modele("statue", {"pierre": "polished_andesite", "socle": "stone_bricks", "or": "gold_block"},
       [el([2, 0, 2], [14, 6, 14], "socle"), el([5, 6, 6], [11, 18, 10], "pierre"), el([6, 18, 6.5], [10, 22, 9.5], "pierre"),
        el([3, 13, 7], [5, 18, 9], "pierre"), el([11, 13, 7], [13, 20, 9], "pierre"), el([5.5, 22, 7], [10.5, 22.5, 9], "or")])
simple("statue"); objet_bloc("statue"); butin("statue"); sans_forme("statue", ["stone_bricks", "stone_bricks", "polished_andesite"])
# Sphère armillaire
modele("sphere_armillaire", {"laiton": "gold_block", "bois": "dark_oak_planks", "globe": "lapis_block"},
       [el([5, 0, 5], [11, 2, 11], "bois"), el([7.5, 2, 7.5], [8.5, 5, 8.5], "laiton"),
        el([2, 9.5, 2], [14, 10.5, 14], "laiton", ["up", "down"]), el([7.5, 4, 2], [8.5, 16, 14], "laiton", ["east", "west"]),
        el([2, 4, 7.5], [14, 16, 8.5], "laiton", ["north", "south"]), el([6.5, 8.5, 6.5], [9.5, 11.5, 9.5], "globe")])
simple("sphere_armillaire"); objet_bloc("sphere_armillaire"); butin("sphere_armillaire"); sans_forme("sphere_armillaire", ["gold_ingot", "compass", "#minecraft:planks"])
# Alambic d'alchimiste
modele("alambic", {"cuivre": "copper_block", "verre": "glass", "fer": "polished_blackstone", "feu": "magma_block"},
       [el([3, 0, 3], [13, 3, 13], "fer"), el([5, 1, 5], [11, 2.5, 11], "feu", ["up"]), el([4, 3, 4], [12, 10, 12], "cuivre"),
        el([6, 10, 6], [10, 13, 10], "cuivre"), el([7, 12, 7], [14, 13, 8], "cuivre"), el([12, 2, 11], [15, 8, 14], "verre")])
simple("alambic"); objet_bloc("alambic"); butin("alambic"); sans_forme("alambic", ["copper_ingot", "copper_ingot", "glass_bottle"])
# Étagère de fioles
modele("etagere_fioles", {"bois": "dark_oak_planks", "rouge": "red_stained_glass", "vert": "lime_stained_glass", "bleu": "light_blue_stained_glass", "livre": "bookshelf"},
       [el([0, 0, 0], [16, 16, 2], "bois"), el([0, 0, 2], [16, 1, 6], "bois"), el([0, 7.5, 2], [16, 8.5, 6], "bois"), el([0, 15, 2], [16, 16, 6], "bois"),
        el([0, 0, 2], [1, 16, 6], "bois"), el([15, 0, 2], [16, 16, 6], "bois"),
        el([2, 1, 3], [4, 5, 5], "rouge"), el([5, 1, 3], [7, 4, 5], "vert"), el([9, 1, 3], [11, 6, 5], "bleu"), el([12, 1, 3], [14, 4, 5], "rouge"),
        el([2, 8.5, 2.5], [8, 14, 5.5], "livre"), el([10, 8.5, 3], [12, 12, 5], "vert"), el([13, 8.5, 3], [14.5, 13, 5], "bleu")])
oriente("etagere_fioles"); objet_bloc("etagere_fioles"); butin("etagere_fioles"); sans_forme("etagere_fioles", ["#minecraft:planks", "glass_bottle", "glass_bottle"])
# Tonneau de bière couché
modele("tonneau_biere", {"douve": "barrel_side", "fond": "barrel_bottom", "fer": "iron_block", "bois": "spruce_planks"},
       [el([0, 0, 1], [16, 3, 3], "bois"), el([0, 0, 13], [16, 3, 15], "bois"), el([1, 2, 2], [15, 14, 14], "douve"),
        el([0.5, 2, 2], [1, 14, 14], "fond", ["west"]), el([15, 2, 2], [15.5, 14, 14], "fond", ["east"]),
        el([3, 2, 1.9], [4, 14, 14.1], "fer"), el([12, 2, 1.9], [13, 14, 14.1], "fer"), el([7, 6, 0], [9, 8, 2], "bois")])
oriente("tonneau_biere"); objet_bloc("tonneau_biere"); butin("tonneau_biere"); sans_forme("tonneau_biere", ["barrel", "wheat"])
# Pupitre d'astronome, avec un grand livre
modele("pupitre", {"bois": "dark_oak_planks", "page": "white_wool", "cuir": "red_terracotta"},
       [el([5, 0, 5], [11, 2, 11], "bois"), el([7, 2, 7], [9, 11, 9], "bois"),
        el([2, 11, 3], [14, 13, 13], "bois", rot={"origin": [8, 12, 8], "axis": "x", "angle": -22.5}),
        el([2.5, 13, 3.5], [13.5, 13.5, 12.5], "page", ["up"], rot={"origin": [8, 12, 8], "axis": "x", "angle": -22.5}),
        el([7.75, 13, 3.5], [8.25, 13.6, 12.5], "cuir", ["up"], rot={"origin": [8, 12, 8], "axis": "x", "angle": -22.5})])
oriente("pupitre"); objet_bloc("pupitre"); butin("pupitre"); sans_forme("pupitre", ["lectern", "book"])

# Étendard de la garde : il marque une zone interdite (14 blocs autour)
t = Image.new("RGBA", (16, 16), (150, 22, 26, 255)); d = ImageDraw.Draw(t)
d.rectangle((0, 0, 15, 15), outline=(214, 170, 52, 255))
for x in range(1, 15, 2): d.point((x, 14), fill=(214, 170, 52, 255))
aigle = ["......#.#.......", ".....#####......", "..#..#####..#...", ".###.#####.###..", "#####.###.#####.", ".####.###.####..",
         "..###.###.###...", "...##.###.##....", "......###.......", ".....#.#.#......", "....#.....#....."]
for j, ligne in enumerate(aigle):
    for i, c in enumerate(ligne):
        if c == "#": d.point((i + 1, j + 2), fill=(24, 20, 20, 255))
d.point((7, 7), fill=(214, 170, 52, 255)); d.point((8, 7), fill=(214, 170, 52, 255))
t.save(f"{A}/textures/block/etendard.png")
modele("etendard", {"hampe": "stripped_dark_oak_log", "or": "gold_block", "toile": "ombre:block/etendard", "socle": "polished_blackstone"},
       [el([5, 0, 5], [11, 2, 11], "socle"), el([7.25, 2, 7.25], [8.75, 31, 8.75], "hampe"), el([7, 31, 7], [9, 32, 9], "or"),
        el([1, 28, 7.5], [15, 29, 8.5], "hampe"), el([1.5, 12, 7.9], [14.5, 28, 8.1], "toile", ["north", "south"], uv=[0, 0, 16, 16]),
        el([0.5, 27.5, 7.25], [1.5, 29.5, 8.75], "or"), el([14.5, 27.5, 7.25], [15.5, 29.5, 8.75], "or")])
oriente("etendard"); objet_bloc("etendard"); butin("etendard"); sans_forme("etendard", ["red_banner", "gold_ingot", "stick"])
NOMS.update({"block.ombre.etendard": "Étendard de la garde (zone interdite)", "block.ombre.affiche_recherche": "Avis de recherche (clic droit : l'arracher)",
             "key.ombre.roue": "Roue des outils (maintenir)", "key.ombre.voyage": "Carte et voyage rapide"})

# Les noms des blocs écrits à la main, selon le thème de 1600
NOMS.update({
    "block.ombre.reverbere": "Lanterne à potence", "block.ombre.reverbere_vert": "Lanterne à potence verte", "block.ombre.reverbere_dore": "Lanterne à potence dorée",
    "block.ombre.lampadaire": "Poteau de lanterne", "block.ombre.tete_lampadaire": "Lanterne",
    "block.ombre.horloge": "Horloge astronomique", "block.ombre.drapeau": "Bannière impériale", "block.ombre.colonne": "Colonne d'annonces",
    "block.ombre.charpente": "Charpente", "block.ombre.carrosse": "Carrosse", "block.ombre.statue": "Statue de pierre",
    "block.ombre.sphere_armillaire": "Sphère armillaire", "block.ombre.alambic": "Alambic d'alchimiste", "block.ombre.etagere_fioles": "Étagère de fioles",
    "block.ombre.tonneau_biere": "Tonneau de bière", "block.ombre.pupitre": "Pupitre d'astronome", "block.ombre.gargouille": "Gargouille (point de vue)",
    "block.ombre.banc": "Banc (s'y asseoir pour se cacher)",
})

# ───────────── Noms ─────────────
ANCIENS = ("pierre_de_paris", "pave_parisien", "brique_parisienne", "papier_peint", ".zinc", "trottoir", "fiacre", "treillis",
           "gardien", "gendarme", "officier", "ecus", "cafe", "croissant", "baguette", "itemGroup.ombre.paris",
           "enseigne_pharmacie", "enseigne_librairie", "enseigne_fleuriste", "affiche_theatre", "affiche_cirque", "affiche_exposition",
           "affiche_bal\"", "affiche_chemin", "affiche_ballon", "affiche_chocolat", "affiche_opera", "affiche_savon", "affiche_velocipede")
for l in ("fr_fr", "en_us"):
    p = f"{A}/lang/{l}.json"; dd = json.load(open(p, encoding="utf-8"))
    for k in list(dd):
        if any(a.replace('\"', '') in k for a in ANCIENS) and k not in NOMS: del dd[k]
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
# ───────────── Faces superposées ─────────────
# Deux pièces dont les faces tombent exactement au même endroit « clignotent » (le jeu ne sait pas laquelle
# dessiner devant). On avance la plus petite d'un cinquantième de pixel : elle passe devant, sans que ça se voie.
FACES_AXE = {"west": (0, "from", -1), "east": (0, "to", 1), "down": (1, "from", -1), "up": (1, "to", 1),
             "north": (2, "from", -1), "south": (2, "to", 1)}
def decoller(dossier):
    n = 0
    for f in os.listdir(dossier):
        p = os.path.join(dossier, f)
        m = json.load(open(p))
        els = [e for e in m.get("elements", []) if "rotation" not in e]
        change = False
        for _ in range(6):
            bouge = False
            for i, a in enumerate(els):
                for b in els[i + 1:]:
                    for face, (k, c, sens) in FACES_AXE.items():
                        if face not in a["faces"] or face not in b["faces"] or abs(a[c][k] - b[c][k]) > 1e-6: continue
                        o = [q for q in range(3) if q != k]
                        if not all(min(a["to"][q], b["to"][q]) - max(a["from"][q], b["from"][q]) > 0.01 for q in o): continue
                        aire = lambda e: (e["to"][o[0]] - e["from"][o[0]]) * (e["to"][o[1]] - e["from"][o[1]])
                        petit = a if aire(a) <= aire(b) else b
                        petit[c][k] = round(petit[c][k] + 0.02 * sens, 3)
                        bouge = change = True
            if not bouge: break
        if change:
            ecrire(p, m); n += 1
    return n
print("faces superposées corrigées :", decoller(f"{A}/models/block"))
print("modèles corrigés :", plaquer(f"{A}/models/block"))
