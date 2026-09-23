# Découpe une grille de textures (4 × 4, cases séparées par un fin trait blanc, comme celle
# que Gemini a produite) en textures de 256 × 256 qui se répètent sans couture, dans textures/.
#
# Usage : python3 outils/decouper-grille.py textures/grille-gemini.webp
# Il faut Pillow et numpy (pip install pillow numpy). Ensuite : node outils/integrer-textures.js
#
# Pour chaque case :
#  1. on retire le trait blanc ;
#  2. pour un motif régulier (planches, bambous, rangs de chaume…), on cherche le recadrage dont
#     les deux bords se ressemblent le plus, pour que la répétition tombe sur une jointure ;
#  3. on raccorde les bords : fondu avec l'image décalée d'une demi-case pour ce qui est irrégulier
#     (sable, herbe, roche), fondu des deux bords vers leur moyenne pour ce qui est régulier ;
#  4. on ramène la teinte moyenne sur celle de l'ancienne texture dessinée, pour que la lumière
#     des niveaux, réglée sur elle, reste juste.
import sys, os
import numpy as np
from PIL import Image

N = 256
ICI = os.path.dirname(os.path.abspath(__file__))
SORTIE = os.path.join(ICI, '..', 'textures')

# nom du fichier, ligne, colonne, raccord en largeur, raccord en hauteur, teinte visée (moyenne, écart),
# et en option la saturation gardée (l'enduit perd ses taches de rouille, qui se verraient en se répétant)
# « bruit » : irrégulier ; « motif » : régulier, on recadre sur une jointure.
CASES = [
    ('sable',     0, 0, 'bruit', 'bruit', (134, 117, 89), (16, 14, 13)),
    ('herbe',     0, 1, 'bruit', 'bruit', (64, 74, 37),   (12, 14, 7)),
    ('enduit',    0, 2, 'bruit', 'bruit', (191, 185, 170), (20, 20, 21), 0.4),
    ('planches',  0, 3, 'bruit', 'motif', (167, 168, 156), (24, 25, 24)),
    ('chaume',    1, 0, 'bruit', 'motif', (118, 92, 48),  (34, 26, 12)),
    ('basalte',   1, 1, 'bruit', 'bruit', (64, 62, 54),   (15, 14, 12)),
    ('bambou',    1, 2, 'motif', 'bruit', (126, 110, 63), (34, 33, 22)),
    ('rondins',   1, 3, 'motif', 'bruit', (74, 57, 41),   (22, 19, 16)),
    ('ecorce',    2, 0, 'bruit', 'bruit', (80, 64, 45),   (14, 12, 9)),
    ('tapa',      2, 1, 'motif', 'motif', (169, 147, 115), (59, 64, 56)),
    ('feuillage', 2, 3, 'bruit', 'bruit', (43, 81, 38),   (13, 20, 10)),
    ('terre',     2, 2, 'bruit', 'bruit', (112, 84, 58),  (15, 13, 11)),
]

def cases(im):
    """Repère les traits blancs et rend les bornes des 4 × 4 cases."""
    a = np.asarray(im).astype(float).min(axis=2) > 235
    def bornes(prof):
        traits = [i for i, v in enumerate(prof) if v > 0.8]
        groupes = []
        for t in traits:
            if groupes and t - groupes[-1][-1] <= 2: groupes[-1].append(t)
            else: groupes.append([t])
        lim = [0] + [x for g in groupes for x in (g[0], g[-1] + 1)] + [len(prof)]
        return [(lim[i], lim[i + 1]) for i in range(0, len(lim), 2)]
    return bornes(a.mean(axis=0)), bornes(a.mean(axis=1))

def recadrage(img, axe):
    """Pour un motif régulier : le recadrage (au moins 80 % de la case) aux bords les plus semblables."""
    a = img if axe == 1 else img.transpose(1, 0, 2)
    L = a.shape[1]; best = (1e9, 0, L)
    for x0 in range(0, int(L * 0.12)):
        for x1 in range(int(L * 0.88), L):
            if x1 - x0 < L * 0.8: continue
            d = np.abs(a[:, x0:x0 + 2] - a[:, x1 - 2:x1]).mean()
            if d < best[0]: best = (d, x0, x1)
    a = a[:, best[1]:best[2]]
    return a if axe == 1 else a.transpose(1, 0, 2)

def fondu_bruit(a, axe, bande):
    """Mêle l'image à elle-même décalée d'une demi-case : les bords viennent du milieu, sans couture."""
    L = a.shape[axe]; d = np.minimum(np.arange(L), L - 1 - np.arange(L))
    w = np.clip(d / bande, 0, 1); w = w * w * (3 - 2 * w)
    w = w[None, :, None] if axe == 1 else w[:, None, None]
    return a * w + np.roll(a, L // 2, axis=axe) * (1 - w)

def fondu_motif(a, axe, bande):
    """Amène les deux bords vers leur moyenne : pas de double image sur un motif régulier."""
    if axe == 0: return fondu_motif(a.transpose(1, 0, 2), 1, bande).transpose(1, 0, 2)
    a = a.copy(); L = a.shape[1]; bord = (a[:, :1] + a[:, -1:]) / 2
    for i in range(bande):
        t = i / bande; t = t * t * (3 - 2 * t)
        a[:, i] = a[:, i] * t + bord[:, 0] * (1 - t)
        a[:, L - 1 - i] = a[:, L - 1 - i] * t + bord[:, 0] * (1 - t)
    return a

def teinte(a, moy, ecart):
    """Même clarté moyenne que l'ancienne texture, couleur et contraste à mi-chemin des deux."""
    m = a.reshape(-1, 3).mean(0); s = a.reshape(-1, 3).std(0).mean() + 1e-3
    moy = np.array(moy, float)
    vise = (m / m.mean() + moy / moy.mean()) / 2 * moy.mean()
    k = np.sqrt(np.mean(ecart) / s)
    return np.clip((a - m) * k + vise, 0, 255)

def main(chemin):
    im = Image.open(chemin).convert('RGB')
    X, Y = cases(im)
    os.makedirs(SORTIE, exist_ok=True)
    for nom, li, co, rx, ry, moy, ecart, *sat in CASES:
        x0, x1 = X[co]; y0, y1 = Y[li]
        a = np.asarray(im.crop((x0 + 3, y0 + 3, x1 - 3, y1 - 3))).astype(float)
        if rx == 'motif': a = recadrage(a, 1)
        if ry == 'motif': a = recadrage(a, 0)
        a = np.asarray(Image.fromarray(a.astype(np.uint8)).resize((N, N), Image.LANCZOS)).astype(float)
        a = fondu_bruit(a, 1, 56) if rx == 'bruit' else fondu_motif(a, 1, 10)
        a = fondu_bruit(a, 0, 56) if ry == 'bruit' else fondu_motif(a, 0, 10)
        a = teinte(a, moy, ecart)
        if sat: g = a.mean(axis=2, keepdims=True); a = g + (a - g) * sat[0]
        f = os.path.join(SORTIE, nom + '.webp')
        Image.fromarray(a.astype(np.uint8)).save(f, 'WEBP', quality=82, method=6)
        print(nom.ljust(10), os.path.getsize(f) // 1024, 'Ko')

if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(SORTIE, 'grille-gemini.webp'))
