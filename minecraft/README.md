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

## Commencer : l'initiation

Crée un monde **Plat** (commandes autorisées), puis tape **`/ombre initiation`**. La **Ruelle d'Or** du château de Prague se construit devant toi : **20 cours fermées par des grilles, 30 leçons**. Maître Vojtěch (inventé) t'y apprend tout, une chose à la fois :

| Cours | Ce qu'on y apprend |
|---|---|
| 1 à 7 | L'entrée et la boussole, l'escalade et la gouttière, le saut arrière, les rebords, la course libre et les bonds, la course sur mur, les perches |
| 8 et 9 | Le monte-charge et la tyrolienne ; le point de vue, le saut de la foi, la carte et le voyage rapide |
| 10 à 12 | L'armurerie (roue des outils, instinct), les cachettes et le sifflet, la foule |
| 13 et 14 | La cour d'armes (assassinat dans le dos, aérien, depuis le rebord, double), les couteaux et les fléchettes |
| 15 à 20 | Le déguisement, la zone interdite, la notoriété et les avis de recherche, le vol à la tire, le lance-grappin, la sortie |

- En haut à gauche, un **panneau** explique la leçon en cours ; sur la **boussole**, un **losange doré** montre où aller. Quand la leçon est réussie, la grille s'ouvre.
- Les gardes de l'initiation sont **désarmés**. Dans les cours de discrétion, s'ils te repèrent, tu reviens simplement à l'entrée de la cour.
- Si tu meurs, tu réapparais à l'entrée de la cour où tu étais.
- **`/ombre initiation 14`** reprend à l'étape 14 (de 1 à 30).
- Une fois l'initiation finie : `/ombre prague`.

## Le cours de rappel

**`/ombre rappel`** construit le **Jardin royal** du château : 14 cours, 19 leçons, pour tout ce qui a changé depuis l'initiation — la carte, la poutre, l'arrêt au bord, les angles, les façades à fenêtres (grand saut latéral, bond en diagonale, glissade), le franchissement, le bond assisté, la descente au rebord, la cachette debout dans les buissons, la synchronisation et le saut de la foi filmés, la vue de l'aigle, la fléchette de folie, les plumes et la course des toits. `/ombre rappel 7` reprend à la leçon 7.

## Jouer

1. Crée un monde **Plat**, avec **« Autoriser les commandes : OUI »**, difficulté **Normale**.
2. Tape `/ombre prague` : la Vieille-Ville se construit au nord-est, tu es placé à son coin sud-ouest, et tu reçois l'équipement.
3. Va sur la grande place, au pied de l'**horloge astronomique** : fais un **clic droit** sur **Tycho Brahe** et **Johannes Kepler**. Ils t'expliquent la mission.
4. Les deux cibles (en or avec l'Instinct, touche **V**) :
   - **Ottokar von Rabenstein**, grand maître de la Confrérie du Corbeau, au sommet de la tour du pont Charles ;
   - **Melchior Kragl**, son alchimiste, dans son laboratoire en haut de la tour nord de l'église de Týn.

Les deux tours des cibles sont des **zones interdites** (étendards rouges) : la barre en haut de l'écran devient jaune quand tu approches, rouge quand tu y entres. Le journal, à gauche, se coche au fil de la mission ; la boussole, en haut, montre où aller.

**Touche H en jeu : l'écran qui récapitule toutes les commandes.**

### La carte `/ombre prague`

- **La Vieille-Ville** (5 × 5 îlots, 162 blocs de côté) : une vingtaine d'îlots de maisons bourgeoises **qu'on peut visiter** (porte, échelle d'étage en étage, boutique du métier de l'enseigne ou cuisine au rez-de-chaussée, chambres et salles meublées aux étages), aux façades ordonnées (des **arcades** sur la grande place, des **toits-terrasses** derrière un attique crénelé où mène une trappe, des **lucarnes**, des fenêtres sur tous les murs, corniche continue, auvent et enseigne pour les boutiques, lanterne et balcon pour les habitations), de vraies charrettes (roues, ridelles, brancards, chargées de foin, de tonneaux ou de pierres), façades pastel ou en sgraffite, pignons sur rue, toits de tuiles rouges, lanternes à potence, enseignes, auvents, balcons, gouttières pour grimper, poutres d'un toit à l'autre, cours avec puits et linge.
- **La grande place de la Vieille-Ville** : un vrai **marché** (douze étals de trois blocs, chacun avec son comptoir, ses tonneaux, sa toile rayée aux couleurs du métier et son marchand ; deux tentes), la **grande fontaine** et sa statue, les **tréteaux des comédiens**, des bancs, des charrettes, la foule, et la **tour de l'horloge astronomique** (cadran, galerie, flèche, gargouilles = points de vue). De petits étals aussi le long des rues.
- **Deux tavernes** à visiter (comptoir, tonneaux, tables, âtre), et des **passerelles** de bois entre les toits de la rangée nord.
- **Le chantier** : une maison à moitié bâtie, ses échafaudages, des tas de pierres et de bois, des ouvriers, et une **grue à roue** de l'époque — on grimpe au mât, et le bout de la flèche est un point de vue.
- **Le jardin du couvent** : allées, massifs de fleurs, haies, arbres fruitiers, kiosque, hautes herbes où se cacher, et des moines.
- **L'église de Týn** : sa nef et ses **deux tours à flèches** (près de 50 blocs), des gargouilles, des perchoirs aux pointes, et le laboratoire de l'alchimiste.
- **La Vltava** : un **moulin à eau** et sa grande roue, des **radeaux** qui forment un passage de parkour d'une rive à l'autre, des barques amarrées, un ponton. Le **pont Charles** avec ses statues, entre la **tour du pont** et la petite tour de l'autre rive (Malá Strana : deux rangées de maisons, un verger), où attend **l'empereur Rodolphe II**.
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
| S'accrocher | **Espace** en poussant vers le mur, ou en sautant contre lui. Seulement un vrai mur, d'**au moins 2 blocs** : un muret, une barrière ou un mur de pierre se sautent (ou se franchissent en courant) |
| Monter / descendre | **Z / S** (Ctrl : plus vite ; le long d'une **gouttière**, deux fois plus vite) |
| Arriver en haut | On s'arrête **suspendu au bord** (idéal pour l'assassinat depuis le rebord) ; **Z** à nouveau ou **Espace** pour passer par-dessus. En sprint (**Ctrl**), on passe d'un trait |
| Se déplacer sur le côté | **Q / D** — les angles se passent tout seuls (coin d'une maison ou coin rentrant), la caméra suit |
| Bondir vers le haut / sur le côté | **Espace** / **Q ou D + Espace** (avec **Ctrl** : grand saut latéral, jusqu'au mur suivant) |
| Bond en diagonale | **Z + Q ou D + Espace** : vers le haut et sur le côté à la fois |
| Glissade | **Ctrl + S** : on descend le long du mur très vite |
| Fenêtres, renfoncements | On s'agrippe à leur rebord : on les longe et on les passe aux mains sans s'arrêter |
| Saut arrière vers le mur d'en face | **S + Espace** (on s'y raccroche tout seul) |
| Lâcher | **Maj** |

### Rebords, course, chutes, prises au clic droit

| Geste | Touches |
|---|---|
| **Rebord** | Automatique en tombant le long d'un mur. Z ou Espace se hisser (on s'arrête pile sur une poutre étroite) · Q/D glisser, angles compris · Q/D + Espace sauter au rebord voisin · S redescendre · Maj lâcher |
| **Course sur mur** | En sprint, sauter le long d'un mur, Z enfoncé ; Espace rebondit vers le mur d'en face |
| **Perche, enseigne, lanterne à potence, câble** | **Clic droit maintenu** en passant à portée : on se balance (Z = élan et projection) ou on glisse |
| **Arbalète à grappin** | Clic droit vers le bord d'un toit (40 blocs) : on est tiré jusqu'en haut |
| **Saut de la foi** | Dans une **meule**, une **charrette de foin**, du foin, du feuillage, sur un auvent ou un étal, ou dans la **Vltava** : aucun dégât. Pendant la chute, bras en croix |
| **Roulade** | **Maj** en touchant le sol : jusqu'à 9 blocs sans mal |
| **Franchissement** | En sprint, une barrière, un muret, un étal ou une charrette se passe d'un appui de main |
| **Bond assisté** | En sprint au bord d'un toit, s'il y a un toit en face (jusqu'à 5 blocs), on s'élance tout seul |
| **Descente au rebord** | Accroupi (**Maj**) au bord d'un toit, pousse vers le vide : tu te suspends par les mains |
| **Course verticale** | En sprint, **Espace** contre un mur : on le monte en courant, bien plus haut |
| **Point de vue** | Tiens-toi sur une **gargouille** ou un **perchoir** : la caméra fait le tour, tous les gardes sont révélés 30 s, et le lieu s'ajoute à la carte pour le voyage rapide |

### Assassin

Dès qu'un garde peut être tué, il est **entouré de rouge** et l'invite **« [R] … »** s'affiche. Appuie sur **R** : la caméra passe en vue extérieure, et le coup se joue.

Chaque famille d'assassinat a **plusieurs scènes**, tirées au hasard (jamais deux fois de suite la même). Le nom du geste s'affiche après le coup. Pour revoir une scène précise : `/ombre scene <numéro>` (le prochain assassinat de la bonne famille la jouera).

| Famille | Quand | Les scènes (numéro pour `/ombre scene`) |
|---|---|---|
| **Au sol** | Il ne t'a pas repéré, devant toi, à 3 blocs | **1** la lame dans le dos (saisi, cambré, face contre terre) · **5** la projection (soulevé par-dessus l'épaule et plaqué derrière toi) · **6** la main sur la bouche (tiré en arrière, couché sur le dos) · **7** le coup au jarret (à genoux, puis la lame) · **8** le face-à-face (retourné, frappé de face, repoussé) |
| **En course** (nouveau) | Tu cours (**Ctrl**), il est à 1,2 – 4,6 blocs | **15** le plaquage (vous roulez à terre, il est dessous) · **16** la lame au passage (tu frappes sans t'arrêter, il tournoie et tombe) · **17** le bond (tu lui sautes sur le dos) |
| **Aérien** | Il est sous toi (toit, poutre, chute), même s'il t'a vu | **2** le plongeon (sur la poitrine) · **9** sur les épaules (tu atterris dessus et le couches) · **10** le coup de foudre (tu tombes derrière lui, la lame en avant) |
| **Depuis la cachette** | Tu es caché, il passe tout près | **3** attiré dans la cachette · **11** la lame dans les herbes (il se retourne, tu sors frapper et tu le ramènes) · **12** tiré par les pieds (il tombe, tu le traînes dedans) |
| **Depuis le rebord** | Tu es suspendu, il marche au bord au-dessus | **4** basculé du rebord (par la cheville) · **13** la lame d'en bas (il s'effondre sur le toit) · **14** par-dessus la tête (lancé dans le vide) |
| **Double** | Une lame dans chaque main (F) | Un 2ᵉ garde distrait tombe aussi |

### Discrétion

| Pour te cacher | Comment |
|---|---|
| **La foule** | Marche parmi deux passants ou plus, sans courir : invisible |
| **Cachettes** | Hautes herbes, grandes fougères, buissons (debout ou accroupi), feuillage ; **meule**, **puits**, **cabine à rideau** (debout) ; **charrette de foin** (accroupi) ; **banc** (clic droit pour s'asseoir) |
| **Déguisement** | **Tenue de hallebardier** (clic droit) : les gardes ne te remarquent qu'à 2 blocs. Perdue si tu cours, grimpes ou frappes. Un hallebardier tué laisse sa tenue |
| **Perdre un poursuivant** | Cache-toi : il perd ta trace s'il est à plus de 3,5 blocs |

Les gardes voient mal dans leur dos et dans le noir ; un garde qui te repère sonne l'alarme et ceux d'à côté accourent.

### Comme dans Assassin's Creed

| Fonction | Comment ça marche |
|---|---|
| **Détection progressive** | Un garde qui te voit ne donne pas l'alerte d'un coup : un **« ? »** blanc apparaît au-dessus de sa tête, devient jaune (il s'arrête, te regarde, puis vient voir), puis **« ! »** rouge : l'alerte. Autour du viseur, des **flèches** montrent d'où l'on t'observe et se remplissent. Sors de sa vue, cache-toi ou lance une bombe de fumée : le soupçon retombe |
| **Zones interdites** | Autour des **étendards rouges de la garde** (14 blocs). Une **barre jaune** en haut de l'écran : tu t'en approches ; **rouge** : tu es dedans, et les gardes te repèrent deux fois plus vite |
| **Roue des outils** | Maintiens **G** : le jeu se fige, montre un outil avec la souris, relâche — il passe dans ta main |
| **Boussole** | En haut de l'écran : les points cardinaux, les **cibles en losange doré** avec leur distance, les points de vue synchronisés, les gardes en alerte |
| **Journal de mission** | À gauche de l'écran ; les objectifs se cochent tout seuls (parler à Tycho et Kepler, éliminer les deux cibles, et en facultatif synchroniser un point de vue et arracher un avis de recherche) |
| **Carte et voyage rapide** | Touche **J** : le vrai terrain vu du dessus, dessiné comme un vieux plan sur parchemin (toits, rues, rivière ; ce que tu n'as pas encore vu reste vierge), avec les zones interdites en rouge, les points de vue, les cibles et ta flèche. Molette : zoom ; glisser : déplacer. Clic sur un point de vue (ou touches 1 à 9) : tu y retournes d'un coup — sauf si des gardes te cherchent |
| **Notoriété** | Monte à chaque alerte, bondit si un innocent meurt. À 75, tu es **RECHERCHÉ** : les gardes te reconnaissent de loin. **Arrache les avis de recherche** (clic droit) pour la faire baisser |
| **Désynchronisation** | Tuer un passant : l'écran se trouble, la notoriété bondit. L'Ombre ne tue pas d'innocents |
| **Vol à la tire** | Accroupi, dans le dos d'un passant : clic droit pour prendre sa bourse. Vu de face, il crie « Au voleur ! » |
| **Enchaînement** | Juste après un assassinat, tu as 2,5 s pour en enchaîner un autre au sol, même sur un garde qui t'a vu |
| **Vue de l'aigle** | Touche **N** : la caméra s'envole au-dessus de toi. Vise un garde un instant : il est marqué une minute, visible à travers les murs. **N** pour revenir |
| **Plumes de corbeau** | Quinze plumes cachées sur les hauteurs de Prague, signalées par une colonne de lumière ; le journal les compte, et la quinzième rapporte une bourse |
| **Course des toits** | Au coin nord-ouest de Prague, un anneau de flammes sur un toit : le départ. Franchis les balises dans l'ordre par les toits, les passerelles et les échelles, sans toucher la rue. Chrono et record |
| **Saut de la foi** | Au bord d'un vide, une invite dorée t'avertit quand une meule, du foin ou l'eau t'attend en bas. Quand une poutre ou un rebord est à portée devant toi, c'est un saut ordinaire vers lui : pour le saut de la foi, **regarde vers le bas** en sautant. Saute : la caméra te filme de derrière, tu plonges tête la première bras en croix, le foin jaillit à l'arrivée |
| **Scènes filmées** | Bandes noires de cinéma et caméra libre : à la synchronisation (l'aigle tourne autour du point de vue, jauge « Synchronisation »), au saut de la foi, et à chaque assassinat (plan de côté, pris du côté d'où l'on voit le mieux) |

### Objets

| Objet | Effet |
|---|---|
| **Lame de nacre** | Assassinats (touche R ou clic gauche) |
| **Couteaux de lancer** | Clic droit : tue à distance, sans bruit |
| **Fléchette soporifique** | Le garde s'endort 30 s (il ne voit plus rien) |
| **Fléchette empoisonnée** | Le garde s'effondre 4 s plus tard, sans un bruit |
| **Fléchette de folie** | Le garde touché attaque ses camarades pendant 15 s |
| **Thalers** | Jetés au sol : la foule accourt, les gardes viennent voir |
| **Pétard** | Bruit et fumée : la foule s'enfuit, les gardes accourent |
| **Bombe de fumée** | Les gardes autour perdent ta trace, aveuglés 5 s |
| **Pain de seigle, pain d'épices, fromage, vin, bière** | À manger ; la bière donne de l'élan |

**Instinct** : V. **Siffler** : B. **Roue des outils** : G. **Carte** : J. **Vue de l'aigle** : N. **Aide** : H. Ces touches se changent dans Options › Commandes › « L'Ombre — Infiltration ».

### Commandes du chat

| Commande | Effet |
|---|---|
| `/ombre initiation` | **Commence ici** : la Ruelle d'Or, 30 leçons pas à pas (`/ombre initiation <n>` pour reprendre à l'étape n) |
| `/ombre prague` | La Vieille-Ville de 1600, avec sa mission |
| `/ombre scene <1-17>` | Impose la scène du prochain assassinat (voir le tableau des assassinats) |
| `/ombre kit` | Tout l'équipement de l'assassin |
| `/ombre vitrine` | Les 189 blocs du mod exposés en rangées |
| `/ombre parcours` | Grande carte d'entraînement au parkour, sans gardes |
| `/ombre arene` | Un petit fortin à infiltrer |
| `/ombre aide` | Le rappel des commandes dans le chat |

## Les blocs (trois onglets en mode créatif)

- **L'Ombre — Assassin et parkour** : étendard de la garde (zone interdite), poteau et corde de tyrolienne, monte-charge, perche, perchoir, meule, puits, cabine à rideau, poutres, corniches, enseignes, auvents (16 couleurs + rayés), échafaudage, lanternes, gouttière, balcons, banc, charrette, gargouille, charpente…
- **L'Ombre — Prague 1600** : tuiles rouges, ardoise, pierre de taille, pierre gothique noircie, crépis pastel, sgraffite, pavés, dalles, parquet, carrelage, tapisseries, marbres, brique, devantures, boiserie (avec escaliers et dalles) ; horloge astronomique, bannière impériale, colonne d'annonces, statue, sphère armillaire, alambic, étagère de fioles, tonneau de bière, pupitre, carrosse, fontaine, volets, étals, affiches de 1600, fanions…
- **L'Ombre — Objets et personnages** : la nourriture, les objets à lancer, la tenue, et les œufs des passants, des gardes et des personnages historiques.

## Pour les curieux

Compiler : `./gradlew build` (Java 21). Ressources (modèles, textures, noms, recettes) : `python3 outils/generer-ressources.py`.
