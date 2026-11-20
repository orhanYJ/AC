# L'Ombre — Prague 1600 (mod Minecraft)

Un mod **Fabric** pour **Minecraft Java 1.21.1** : un jeu d'assassin dans la Prague de l'an 1600, à la cour de l'empereur Rodolphe II. Parkour en hauteur sur les toits de tuiles rouges, la foule où se fondre, les gardes de la ville, des personnages réels (Tycho Brahe, Kepler, Rodolphe II, le rabbin Loew et son Golem), et une confrérie secrète à démanteler.

**Fichier à installer** : [`telecharger/ombre-infiltration-0.1.0.jar`](telecharger/ombre-infiltration-0.1.0.jar)
Lien direct : <https://github.com/orhanYJ/AC/raw/claude/elegant-feynman-qh6m7g/minecraft/telecharger/ombre-infiltration-0.1.0.jar>

## Installation (une seule fois)

1. **Fabric** : <https://fabricmc.net/use/installer/>, onglet « Client », version **1.21.1**, « Installer ».
2. **Fabric API** : <https://modrinth.com/mod/fabric-api/versions>, version pour **1.21.1**.
3. `Windows + R`, `%appdata%\.minecraft`, dossier `mods` (le créer s'il n'existe pas) : y mettre **Fabric API** et **ombre-infiltration-0.1.0.jar**.
4. Lanceur Minecraft : profil **fabric-loader-1.21.1**, « Jouer ».

Pour une grosse carte graphique : ajouter **Sodium** et **Iris** (version 1.21.1) dans `mods`, et un pack de shaders (*Complementary Reimagined*, *BSL*) dans `.minecraft\shaderpacks`. Les toits de Prague au coucher du soleil…

## Jouer

1. Crée un monde **Plat**, avec **« Autoriser les commandes : OUI »**, difficulté **Normale**.
2. Tape `/ombre prague` : la Vieille-Ville se construit au nord-est, tu es placé à son coin sud-ouest, et tu reçois l'équipement.
3. Va sur la grande place, au pied de l'**horloge astronomique** : fais un **clic droit** sur **Tycho Brahe** et **Johannes Kepler**. Ils t'expliquent la mission.
4. Les deux cibles (en or avec l'Instinct, touche **V**) :
   - **Ottokar von Rabenstein**, grand maître de la Confrérie du Corbeau, au sommet de la tour du pont Charles ;
   - **Melchior Kragl**, son alchimiste, dans son laboratoire en haut de la tour nord de l'église de Týn.

**Touche H en jeu : l'écran qui récapitule toutes les commandes.**

### La carte `/ombre prague`

- **La Vieille-Ville** : douze îlots de maisons bourgeoises, façades pastel ou en sgraffite, pignons sur rue, toits de tuiles rouges, lanternes à potence, enseignes, auvents, balcons, gouttières pour grimper, poutres d'un toit à l'autre, cours avec puits et linge.
- **La place de la Vieille-Ville** : le marché, la fontaine, la foule, et la **tour de l'horloge astronomique** (cadran, galerie, flèche, gargouilles = points de vue).
- **L'église de Týn** : sa nef et ses **deux tours à flèches** (près de 50 blocs), des gargouilles, des perchoirs aux pointes, et le laboratoire de l'alchimiste.
- **La Vltava** et le **pont Charles** avec ses statues, entre la **tour du pont** et la petite tour de l'autre rive, où attend **l'empereur Rodolphe II**.
- Au nord, dans la rue du quartier juif, **le rabbin Loew** et **le Golem**, qui s'en prend aux gardes.
- Des **tyroliennes** : de la galerie de l'horloge vers Týn, du sommet de la tour du pont vers l'autre rive.

### Les personnages réels

Ils sont intouchables : on les croise et on leur parle (**clic droit**, plusieurs fois pour la suite). Leurs répliques sont fidèles à l'histoire, et ils donnent des indices.

| Personnage | Qui c'était |
|---|---|
| **Tycho Brahe** (1546–1601) | Astronome de l'empereur, au nez de métal (perdu en duel quand il était étudiant). Ses mesures du ciel, sans lunette, étaient les plus précises de son temps |
| **Johannes Kepler** (1571–1630) | Arrivé à Prague en 1600 pour travailler avec Tycho. Il découvrira plus tard que les planètes tournent en ellipse |
| **Rodolphe II** (1552–1612) | Empereur, installé à Prague, passionné d'arts, de sciences et d'alchimie ; son cabinet de curiosités était célèbre |
| **Rabbi Judah Loew** (vers 1520–1609) | Le « Maharal », grand savant de Prague. Une légende lui attribue la création du Golem |

Les **cibles** et la **Confrérie du Corbeau** sont inventées.

## Commandes

### Escalade (on reste là où on est)

| Geste | Touches |
|---|---|
| S'accrocher | **Espace** en poussant vers le mur, ou en sautant contre lui |
| Monter / descendre | **Z / S** (Ctrl : plus vite ; le long d'une **gouttière**, deux fois plus vite) |
| Se déplacer sur le côté | **Q / D** |
| Bondir vers le haut / sur le côté | **Espace** / **Q ou D + Espace** |
| Saut arrière vers le mur d'en face | **S + Espace** (on s'y raccroche tout seul) |
| Lâcher | **Maj** |

### Rebords, course, chutes, prises au clic droit

| Geste | Touches |
|---|---|
| **Rebord** | Automatique en tombant le long d'un mur. Z ou Espace se hisser · Q/D glisser · Q/D + Espace sauter au rebord voisin · S redescendre · Maj lâcher |
| **Course sur mur** | En sprint, sauter le long d'un mur, Z enfoncé ; Espace rebondit vers le mur d'en face |
| **Perche, enseigne, lanterne à potence, câble** | **Clic droit maintenu** en passant à portée : on se balance (Z = élan et projection) ou on glisse |
| **Arbalète à grappin** | Clic droit vers le bord d'un toit (40 blocs) : on est tiré jusqu'en haut |
| **Saut de la foi** | Dans une **meule**, une **charrette de foin**, du foin, du feuillage, sur un auvent ou un étal, ou dans la **Vltava** : aucun dégât. Pendant la chute, bras en croix |
| **Roulade** | **Maj** en touchant le sol : jusqu'à 9 blocs sans mal |
| **Point de vue** | Tiens-toi sur une **gargouille** ou un **perchoir** : la caméra fait le tour, tous les gardes sont révélés 30 s |

### Assassin

Dès qu'un garde peut être tué, il est **entouré de rouge** et l'invite **« [R] … »** s'affiche. Appuie sur **R** : la caméra passe en vue extérieure, et le coup se joue.

| Assassinat | Quand | Le geste |
|---|---|---|
| **Au sol** | Il ne t'a pas repéré, devant toi, à 3 blocs | Tu passes derrière lui, une main le saisit, l'autre frappe |
| **Aérien** | Il est sous toi (toit, rebord, chute), même s'il t'a vu | Tu bondis en arc et retombes sur lui |
| **Depuis la cachette** | Tu es caché, il passe tout près | Tu l'attires dans la cachette |
| **Depuis le rebord** | Tu es suspendu, il marche au bord au-dessus | Tu le tires par-dessus le bord |
| **Double** | Une lame dans chaque main (F) | Un 2ᵉ garde distrait tombe aussi |

### Discrétion

| Pour te cacher | Comment |
|---|---|
| **La foule** | Marche parmi deux passants ou plus, sans courir : invisible |
| **Cachettes** | Hautes herbes, buissons, feuillage (accroupi) ; **meule**, **puits**, **cabine à rideau** (debout) ; **charrette de foin** (accroupi) ; **banc** (clic droit pour s'asseoir) |
| **Déguisement** | **Tenue de hallebardier** (clic droit) : les gardes ne te remarquent qu'à 2 blocs. Perdue si tu cours, grimpes ou frappes. Un hallebardier tué laisse sa tenue |
| **Perdre un poursuivant** | Cache-toi : il perd ta trace s'il est à plus de 3,5 blocs |

Les gardes voient mal dans leur dos et dans le noir ; un garde qui te repère sonne l'alarme.

### Objets

| Objet | Effet |
|---|---|
| **Lame de nacre** | Assassinats (touche R ou clic gauche) |
| **Couteaux de lancer** | Clic droit : tue à distance, sans bruit |
| **Fléchette soporifique** | Le garde s'endort 30 s (il ne voit plus rien) |
| **Fléchette empoisonnée** | Le garde s'effondre 4 s plus tard, sans un bruit |
| **Thalers** | Jetés au sol : la foule accourt, les gardes viennent voir |
| **Pétard** | Bruit et fumée : la foule s'enfuit, les gardes accourent |
| **Bombe de fumée** | Les gardes autour perdent ta trace, aveuglés 5 s |
| **Pain de seigle, pain d'épices, fromage, vin, bière** | À manger ; la bière donne de l'élan |

**Instinct** : V. **Siffler** : B. **Aide** : H. Ces touches se changent dans Options › Commandes › « L'Ombre — Infiltration ».

### Commandes du chat

| Commande | Effet |
|---|---|
| `/ombre prague` | La Vieille-Ville de 1600, avec sa mission |
| `/ombre kit` | Tout l'équipement de l'assassin |
| `/ombre vitrine` | Les 188 blocs du mod exposés en rangées |
| `/ombre parcours` | Grande carte d'entraînement au parkour, sans gardes |
| `/ombre arene` | Un petit fortin à infiltrer |
| `/ombre aide` | Le rappel des commandes dans le chat |

## Les blocs (trois onglets en mode créatif)

- **L'Ombre — Assassin et parkour** : poteau et corde de tyrolienne, monte-charge, perche, perchoir, meule, puits, cabine à rideau, poutres, corniches, enseignes, auvents (16 couleurs + rayés), échafaudage, lanternes, gouttière, balcons, banc, charrette, gargouille, charpente…
- **L'Ombre — Prague 1600** : tuiles rouges, ardoise, pierre de taille, pierre gothique noircie, crépis pastel, sgraffite, pavés, dalles, parquet, carrelage, tapisseries, marbres, brique, devantures, boiserie (avec escaliers et dalles) ; horloge astronomique, bannière impériale, colonne d'annonces, statue, sphère armillaire, alambic, étagère de fioles, tonneau de bière, pupitre, carrosse, fontaine, volets, étals, affiches de 1600, fanions…
- **L'Ombre — Objets et personnages** : la nourriture, les objets à lancer, la tenue, et les œufs des passants, des gardes et des personnages historiques.

## Pour les curieux

Compiler : `./gradlew build` (Java 21). Ressources (modèles, textures, noms, recettes) : `python3 outils/generer-ressources.py`.
