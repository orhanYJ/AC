# L'Ombre du Fenua — Tahiti, 1844

Jeu d'infiltration 3D dans l'esprit des **anciens Assassin's Creed** (AC1/2, Brotherhood, Black Flag, Mirage) : parcours **en hauteur plus qu'en largeur**, sans dialogue. Il se déroule pendant la guerre franco-tahitienne (1844–1847). Tout le jeu tient dans **un seul fichier HTML** : `docs/fenua.html`, environ 480 Ko (dont 160 Ko de textures intégrées). Il vit dans `docs/` parce que c'est le dossier que GitHub Pages sait publier.

- Langue : **tout en français** (interface, commentaires du code, réponses).
- Le propriétaire est **débutant en développement** : expliquer simplement, sans jargon inutile.
- Version en ligne (GitHub Pages) : https://orhanyj.github.io/AC/
- Version en ligne (artefact Claude) : https://claude.ai/artifact/3Ke8xXAUFeKFt73GCtdwQG
- Jeu précédent sur le même moteur (Proche-Orient médiéval) : « L'Ombre de Bab el-Ward », https://claude.ai/artifact/JUe3Z3Ed7dPrjVsgM8xWWE

## Lancer et vérifier

- Ouvrir `docs/fenua.html` dans un navigateur. Aucune compilation ; Three.js **r128** est chargé depuis cdnjs.
- **Après toute modification de niveau**, lancer `node outils/valider-niveaux.js`. Ce script vérifie :
  - que les rondes des gardes ne traversent ni mur ni eau ;
  - que les gardes postés en hauteur restent sur leur toit ;
  - que départ, sortie, flambeaux, cachettes et charges ne sont pas dans un mur ;
  - que les cordes ont un appui aux deux bouts ;
  - que les points de vue et les cibles en hauteur sont bien sur un sommet ;
  - que les gardes au sol suivent le relief et les marches sans pente trop raide ;
  - qu'aucun bâtiment, flambeau, objet ou cachette dure n'est posé sur une colline, et qu'aucune colline ne monte dans l'eau.

  Il doit afficher « 0 conflit ».
- Vérifier la syntaxe JS, le crochet de test oublié et les sources externes : `node outils/verifier-script.js`.
- Les deux vérifications tournent aussi dans GitHub Actions (`.github/workflows/verifier.yml`) à chaque poussée.
- **Hors ligne**, cdnjs peut être bloqué : pour tester, récupérer Three.js r128 avec `npm pack three@0.128.0`, extraire `package/build/three.min.js` à côté de la **copie** de test et y remplacer l'URL cdnjs. Ne jamais livrer la copie.
- Tests en navigateur utilisés jusqu'ici : Playwright + Chromium avec `--use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader`, fenêtre 640×360, capture avec `timeout=150000` (le rendu logiciel est lent). Pour tester la logique, on ajoute sur **une copie** du fichier, juste avant `frame();` à la fin :
  ```js
  window.T={G,scene,camera,tryClimb,tryAction,computePrompt,updatePlayer,updateGuards,startLevel,snapCamera,
            inWater,wallClimb,poleNear,syncVista,ledgeGrab,ledgeUnder,startHang,releaseHang,hangPullUp,
            hangShimmy,hangPos,hangable,FACE_N,assassinationOn,startTakedown,spawnSmoke,hAt,groundAt,lineOfSight};
  ```
  On appelle ensuite `updatePlayer(0.05)` / `updateGuards(0.05)` en boucle. **Ne jamais livrer ce crochet.**

## Architecture du fichier

Un seul `<script>` dans une IIFE, découpé en sections commentées :

| § | Contenu |
|---|---|
| 0 | Outils (`clamp`, `lerp`, `angDelta`, `store` = localStorage) |
| 1 | Socle Three.js : rendu ACES, ciel dégradé, étoiles et Croix du Sud, lune, **6 PointLight réaffectées** aux flambeaux les plus proches (`LIGHT_POOL`), textures dessinées en canvas (`TEX.*`), remplacées par les **textures peintes** (`PEINTES`) dès qu'elles sont décodées, matériaux (`MAT.*`), `wallMats(type)` |
| 2 | **Données des niveaux** : `KIND`, `guard()`, `T()`, `R()`, `LEVELS`, `HAZARDS`, `DECO` |
| 3 | Géométrie : boîtes AABB, `circleVsBoxes`, `groundAt`, `lineOfSight` (tient compte de la hauteur) |
| 4 | Personnages : `humanoid(o)` (le résistant tatoué `tahitian:true`, les uniformes `french:true`), `guardLook(def,i)`. **Tout ce qui ne bouge pas est cuit en un seul mesh** : `baker(parent)` collecte des pièces (géométrie, couleur, position, rotation) et `mergeGeo` les fond en une géométrie à couleurs de sommets (`VC_MAT`) — un garde fait 15 objets au lieu de 37. Seuls restent à part le torse (il respire), les segments de membres (ils pivotent), et les pièces texturées du résistant. Les membres sont **articulés** : `limb()` rend un groupe haut (épaule, hanche) qui porte un sous-groupe `.lower` (coude, genou). Tout ce qui se tient dans la main — mousquet, hache, lanterne, lame — s'accroche à `armR.lower`. `poseWalk` plie genoux et coudes ; `straighten(ud,bras,jambes)` les tend pour les poses tenues. **Le résistant a son propre corps**, `heroMesh()` : un squelette bassin (`pelvis`) → buste (`spine`) → tête, avec visage (yeux, sourcils, nez, lèvres, oreilles), chignon, capuche de tapa ouverte sur le visage, pāreu et rabats, manteau en deux pans, tatouages sur bras et jambes, lame de nacre au brassard. Il est animé par `heroPose(p,dt)` : chaque image calcule la pose voulue (marche, course, accroupi, saut, mur, rebord, rétablissement, roulade, réception…) et chaque articulation (`POSE_J`) y glisse, si bien que les gestes se fondent |
| 5–6 | Sons (WebAudio), arsenal `TOOLS` |
| 7 | État `G`, `buildLevel`, décor et architecture (fare, falaises, jungle, palissades, eau, montagnes, forêt, frégate, marae, ʻōrā, ʻuru, fara, meiʻa, tarodière, feu de bivouac, faisceau d'armes, tonneaux, tiʻi, éboulis…), escalade, **rebords**, points de vue. `batch(geo,mat,liste)` rend un lot d'objets identiques en **un seul appel** : tout le petit décor répété passe par là |
| 8–9 | Lumière (`lightAt`), entrées clavier, souris et tactile |
| 10 | Actions : assassinats (`startTakedown`), assommer (`tryKO`), outils, corps, charges suspendues |
| 11 | Perception (cônes, bruit, alarme) |
| 12 | Boucle joueur (`moveInput`, `runHeld`) : nage, plongée, **escalade libre** (`p.wall`), **suspension aux rebords**, **bond** (`p.bond`), **franchissement**, **roulade**, réception, piqué, cordes. La vitesse est lissée (`p.vx`/`p.vz` tendent vers la vitesse voulue), pas appliquée d'un coup |
| 13 | Navigation (`buildNav`, `navFind` : grille de 50 cm et A*), boucle gardes : patrouille, **rencontres**, enquête, **fouille des cachettes**, **battue**, alerte **propre à chaque unité**, **combat** (`tirer`, `hitPlayer`), pièces, poison, réveil, animations au repos |
| 14–18 | Caméra, invites, HUD et mini-carte, écrans, boucle principale |

## Format d'un niveau (section 2)

- `b(x,z,w,d,y0,y1,type)` crée une boîte. Types et effets :

  | Type | Rendu / rôle | Escaladable |
  |---|---|---|
  | `stone` | chaux blanche | oui |
  | `wood` | planches | oui |
  | `rock` | basalte | oui |
  | `tower` | tour de guet | oui |
  | `hull` | coque de navire | oui |
  | `fare` | case de bambou ; **son toit de chaume se parcourt** (gradins `chaume` invisibles ajoutés par `fareTerraces`) | oui, sans corniche |
  | `jungle` | lisière de forêt | non |
  | `cliff` | falaise, + 10 m visuels | non |
  | `palis` | palissade | non |
  | `tent` | toile | non |
  | `marae` | lieu sacré | non (volontairement) |
  | `crate` | caisse | marche normale |
  | `beam` | poutre, vergue ou hune praticable | on y marche |
  | `ora` | tronc de banian, gros et grimpable | grimpe comme un cocotier |
  | `branch` | branche maîtresse de ʻōrā | on y marche et on y court |
  | `uru` | tronc d'arbre à pain grimpable, 1 m (posé par `arbre()`) | grimpe comme un cocotier |
  | `marche` | marche d'escalier en basalte (posée par `escalier()`) | on la monte sans rien presser |
  | `balcon` | galerie accrochée à une façade, avec garde-corps côté vide | on y monte d'un Espace, on y marche |
  | `auvent` | auvent de palmes sur poteaux | idem : un palier entre le sol et le toit |
  | `chaume` | gradin invisible d'un toit de fare, créé à la construction | on y marche |
  | `void` | mur invisible côté mer | — |

  Les murs d'enceinte (`isRingWall`) ne s'escaladent jamais.
- `coco(boxes,x,z,h)` : cocotier grimpable, collision de 0,7 m. Type `mast` : mât grimpable. Les deux se grimpent jusqu'en haut et se redescendent (`palmClimb`).
- `arbre(boxes,x,z,h,arms)` : un arbre à pain grimpable, fût d'un mètre, mêmes branches que l'ʻōrā en plus étroit. `limbs()` pose les branches pour les deux.
- `escalier(boxes,x,z,dir,len,h,w)` : des marches de moins de 40 cm depuis le pied (x,z), dans le sens `dir` (`"x+"`, `"x-"`, `"z+"`, `"z-"`). **Chaque marche doit faire au moins 50 cm de profondeur**, sinon le rayon du joueur bute sur la suivante. Les gardes les montent aussi.
- **Relief** : `hills:[{x,z,rx,rz,h,top,rot}]`. Ce ne sont pas des boîtes : `hillAt(x,z,hills)` rend la hauteur (bosse en cosinus elliptique, plateau `top` en fraction du rayon, ondulation douce ; deux collines qui se touchent se fondent en crête). Le sol (`groundAt`), la vue (`lineOfSight`, les cônes), la caméra et les gardes en tiennent compte. Au-delà de `SLOPE_MAX` (1,2, environ 50°) une pente ne se monte plus à pied : on glisse le long. Pente maximale d'une bosse ≈ `h·π/2 / ((1-top)·r)`. **Ni bâtiment, ni flambeau, ni objet, ni cachette dure sur une pente** — seules les hautes herbes et les fourrés y poussent ; le décor (`DECO`) est remonté tout seul à la hauteur du sol.
- `prairies:[{x,z,rx,rz}]` : des taches d'herbe sur le sol. Le reste de l'herbe vient tout seul : lisières de jungle et de falaise, pied des arbres, collines ; le sable reste nu au bord de l'eau.
- `ora(boxes,x,z,h,arms)` : un **ʻōrā** (banian). Le tronc (1,7 m, type `ora`) se grimpe comme un cocotier ; `arms` est une liste `[angle, portée, hauteur]` qui pose autant de boîtes `branch` praticables. La couronne cache comme les palmes (`onPalm`). Les rayons de préhension sont dans `POLE`.
- Champs propres au niveau : `epilogue` (texte affiché à la victoire), `fireflies:true` (lucioles), et sur une interaction `look:"captif"|"keg"` (l'allure de l'objet : un homme attaché, ou des tonneaux de poudre ; sans `look`, un bureau et des papiers).
- Autres champs d'un niveau : `water:[{x,z,w,d}]` (lagon jouable, les soldats n'y entrent pas), `ocean:[…]` (visuel ; masque aussi les montagnes et la forêt de ce côté), `tapu:[{x,z,r}]` (aucun assassinat possible), `vista:{x,y,z}` (point de vue + objectif facultatif `id:"vista"`), `ground` (teinte du sol), `ropes:[R(a,b)]` (cordes de nape ; on ne les prend que **vers le bas** et **dans la direction du regard**, voir `ropeOK`).
- **Les rondes en hauteur longent les corniches** (et non le milieu du toit) : c'est ce qui rend possible l'assassinat par-dessus le rebord. Un garde posté doit passer à moins de 2,4 m du point de suspension.
- Gardes : `guard(x,z,{kind,path,y,wait,isTarget,oid,name})`. Les familles sont `soldat` (fusilier), `eclaireur` (voltigeur), `brute` (sapeur, impossible à tuer de face sauf s'il ramasse des pièces), `guetteur` (sentinelle), `torche` (porte-lanterne) et `cible` (officier).
- Flambeaux : `T(x,z,r,"lan")` pour une lanterne française ; sans style, c'est un flambeau de bambou.
- `HAZARDS[i]` : filets de noix de coco (point d'impact + poteau où couper la corde).
- `DECO[i]` : décor sans collision — dont `{t:"buissons",x,z,r,n}` (un massif de n buissons dans un rayon r) — (`palm`, `vaa`, `ship`, `flag`, `cannon`, `marae`, `tapa`, `net`, `tiare`, `ahimaa`, `belfry`, `tent`, `ladder`, `waterfall`, `gable`, `lanterns`, `uru`, `fara`, `meia`, `taro`, `fire`, `faisceau`, `barrels`, `tiki`, `rocks`).

## Les 5 missions

Chaque niveau a désormais son relief : tertres de 1,5 à 4,5 m dans les espaces dégagés (Papeete, dans son enceinte, n'en a qu'un, le tertre du jardin).

1. **Matavai** : village occupé, **68 × 76**. Départ à la nage dans le lagon. Tour de guet (13 m, point de vue), canopée de cocotiers reliés par des cordes jusqu'au toit du poste, escalier extérieur, auvent et galerie. Au nord : deux collines (celle de l'est, 6 m, porte le camp d'un géomètre et une sentinelle ; sa face nord est trop raide), un hameau de trois fares aux toits praticables reliés par un arbre à pain, une plantation, et **une corde de 28 m du sommet de la colline au toit du poste**. Deux couloirs de hautes herbes longent la jungle. Cible : le sergent Aubry ; facultatif : le relevé des sentiers.
2. **Papeete** : ordres du gouverneur à voler, sapeur à la porte. **Frégate amarrée** : coque, grand mât, hune (15 m, point de vue), vergue, corde vers le quai. Cible : le capitaine Duval.
3. **Le fortin du récif** : blockhaus escaladable, cordes vers deux toits, tour de signaux au-dessus d'un tas de nīʻau, canon d'alarme à enclouer. Cibles : le capitaine Verneuil et l'ingénieur Morel.
4. **Fautaua** (décembre 1846) : éperons rocheux, piton de 16 m au-dessus du bassin de la cascade (saut de la foi), plans à brûler. Cible : le commandant Lorrain. L'épilogue rappelle que le fort est tombé malgré tout et que la guerre a pris fin en 1847.
5. **Taravao** (janvier 1847) : l'isthme, 84 × 84, le plus grand. Un fort palissadé (blockhaus, caserne, poudrière, batterie, chemin de ronde, deux tours d'angle) verrouille le passage vers la presqu'île. **Quatre voies** : par l'eau (lagon nord, chantier naval, coque en construction), par les arbres (deux ʻōrā dont les branches enjambent la rivière, une branche passe par-dessus la palissade), par la terre (deux gués sur la rivière, la porte gardée par un sapeur), par le ciel (colline de 9 m, piton de 15 m, plongeon dans la baie sud). Objectifs : délivrer le guide captif (`look:"captif"`), brûler la poudrière (`look:"keg"`), deux cibles (le lieutenant Salaün sur la batterie, le commandant Bréhat sur le blockhaus), enclouer les canons et le point de vue en facultatif. 14 hommes, 15 cachettes, 8 cordes, lucioles. L'épilogue clôt la guerre : les derniers chefs déposent les armes en février 1847.

## Mécaniques en place

- **Verticalité** : escalade libre prise par prise (`p.wall`, `startWall`, `updateWall`) : Z monte, S descend, Q et D longent le mur (les angles se passent), Espace bondit vers la prise du dessus, S + Espace se jette en arrière, C lâche. Lancée par Espace ou par l'élan, la montée continue toute seule jusqu'en haut (`w.auto`), puis on se rétablit dans la foulée ; courir fait grimper plus vite. Aussi : cocotiers, mâts et ʻōrā, branches praticables, saut en course (environ 3,5 m), cordes, piqué du saut de la foi (atterrissage silencieux dans l'eau ou le nīʻau), points de vue (`syncVista` : caméra qui tourne, tous les soldats marqués).
- **Course libre** (Maj + direction) : le corps choisit le geste (`freerun`). Un muret se franchit, un mur de 1,3 à 2,35 m se gravit d'un rétablissement (`mantleTarget`/`startMantle`), un mur plus haut qu'on regarde en face s'escalade avec l'élan, un tronc se saisit. Au bord d'un toit, si un appui attend en face — un toit, un palier, une corniche à saisir, à moins de 5 m —, on s'y jette tout seul (`landingAhead`/`startBond`) ; au-dessus d'un vide de plus de 4,5 m sans rien en face, on s'arrête net au bord (Espace pour sauter quand même). Espace à l'arrêt au bord vise aussi l'appui d'en face. Un saut pressé juste après avoir quitté le bord passe encore (`p.coyote`), pressé juste avant l'atterrissage il part à l'atterrissage (`p.jumpBuf`).
- **Déplacement** : la vitesse est lissée (`p.vx`/`p.vz`), donc on se lance et on s'arrête en un instant au lieu de démarrer sec ; en l'air on garde son élan. Un ressaut de moins de 55 cm se monte tout seul. En courant (plus de 4 m/s), un obstacle entre 55 cm et `VAULT_MAX` (1,35 m) se **franchit** d'un appui de main (`lowObstacle`/`startVault`), à condition qu'il y ait de la place pour retomber. Arriver lancé d'une chute de moins de 7 m déclenche une **roulade** : ni bruit fort, ni immobilisation — à l'arrêt, la même chute sonne.
- **Rebords** : tout ce qui est dans `CLIMBABLE` et mesure au moins 2,6 m (`hangable`) porte une corniche. On s'y suspend depuis le toit (`ledgeUnder`, Espace à l'arrêt en regardant le vide) ou **au vol** en descendant le long du mur (`ledgeGrab`). Suspendu : Q et D déplacent aux mains (`hangShimmy`, qui passe les angles via `CORNER`), Espace rétablit (`hangPullUp`), C ou X lâche (`releaseHang`). `p.noGrab` empêche de se raccrocher aussitôt après un saut ou un lâcher. Le toit coupe la vue de celui qui marche dessus : `lineOfSight` s'en charge tout seul. Suspendu, **Q ou D + Espace** saute vers la corniche voisine (`ledgeJump`) si elle est à moins de 4,4 m et à moins d'un étage d'écart ; sans direction, Espace rétablit.
- **Eau** : nage plus lente ; C pour plonger (invisible) ; les soldats s'arrêtent au bord.
- **Quinze assassinats** (`assassinationOn`, types dans `G.killTypes`) : `dos`, `aerien`, `embuscade`, `double`, `elan` (en course), `chute` (tirer une sentinelle dans le vide), `tyrolienne`, `rebord` (suspendu, on le tire par-dessus la corniche), `eau` (noyade depuis le lagon, sans aucun bruit — la seule prise qui marche aussi sur un sapeur avec les piastres et la fumée), `fumee` (dans un nuage de fumée, même de face), `distance` (fronde), `poison` (hora), `accident`, `ko` (R, réveil après 45 s sauf si le corps est caché), `contre` (pendant qu'un garde arme son coup). Les trois prises qui tirent la victime — `chute`, `rebord`, `eau` — passent par `g.pull` avec leur `type` et leur `label`.
- **Outils** : coquillage, piastres, fronde, hora, fumée, poudre.
- **Siffler** (T, croix bas à la manette, bouton tactile) : le garde le plus proche à moins de 12 m vient enquêter sur ta position, sans alarme, avec 4 s entre deux sifflements (`siffler`). Depuis une cachette, c'est l'embuscade toute prête — mais s'il arrive avant le coup, il fouille la cachette.
- **Manette** (`pollGamepad`, disposition standard) : stick gauche marcher, stick droit regarder, A grimper, X agir, Y assommer, B s'accroupir ou lâcher, RT/RB courir, LB outil suivant, LT lancer, croix bas siffler, croix haut lâcher un corps, Start pause. Reconnue dès qu'on y touche, sans réglage. La sensibilité `G.sens` et le regard inversé s'appliquent aussi à la souris et à la manette.
- **Cachettes** : sept familles, dont les **hautes herbes** (`herbe`, grands rectangles, accroupi, sur le plat comme sur les pentes). `SOFT_HIDE` (nīʻau, abri, filets) amortit une chute ; `STAND_HIDE` (tonneaux, pirogue retournée) cache un homme debout, les autres exigent d'être accroupi. `HIDE_NOTE` et `HIDE_STASH` donnent les textes. **Un garde qui enquête fouille les deux cachettes les plus proches de son point d'enquête** (`hidesNear`, `fouillerCachette`) : il va au coin de la cachette le plus proche du soupçon (utile pour les grands champs d'herbe), et t'y trouve si tu es à moins de 3,4 m de lui — de même pour les corps dissimulés. Tant qu'il reste une cachette à fouiller, c'est elle qui mène ses pas — sinon le point d'enquête le rappellerait à chaque image.
- **Gardes** : deux hommes qui se croisent en ronde s'arrêtent et se font face (`g.meet`, avec un délai `g.croiseT` pour ne pas recommencer aussitôt). Un mouvement aperçu les arrête : ils regardent avant d'enquêter. Hors de leur ronde, ils cherchent leur chemin sur la grille (`goTo`, recalculé si le but bouge ou s'ils piétinent) au lieu de se cogner aux murs ; un toit sans escalier est hors d'atteinte, ils vont au plus près en dessous. Les gardes postés en hauteur ne marchent jamais dans le vide. Arrivés au soupçon, ils fouillent deux cachettes puis font une **battue** (`battue`) autour ; perdu de vue en alerte, ils anticipent là où tu courais. **L'alarme gagne de proche en proche** (`triggerAlarm`) : à moins de 26 m ils accourent, jusqu'à 48 m ils viennent voir, au-delà ils ne bougent pas. À l'alerte, la sentinelle **ne quitte pas son poste**, le voltigeur court à 1,8× sa vitesse, le sapeur avance à 1,2×. Deux gardes ne se marchent pas dessus.
- **Combat** : être repéré ne fait plus perdre d'un coup. Le résistant a **trois forces** (`p.hp`, affichées ◆◆◆ ; une revient toutes les 10 s quand l'alarme est retombée). Le fusilier et la sentinelle épaulent (1,6 s), tirent — la chance baisse avec la distance et la course —, puis rechargent 5 à 7 s. Au corps à corps, chacun arme son coup (0,6 s, 0,85 s pour le sapeur) ; pendant ce temps, E le **contre** (assassinat `contre`), sauf le sapeur. Touché au mur ou au rebord, on lâche prise. Trois coups et c'est fini (« Abattu par… », « Terrassé par… »).
- **Autres** : lumière des flambeaux, corps à porter et cacher, alarme, notes de fin (Tūpāpaʻu → Tempête), réglage graphique (touche G), commandes tactiles.
- **Caméra sur mobile** : `#look` est une couche plein écran placée **avant `#hud` dans le HTML** et sans z-index, donc sous le HUD et sous `#touch` — le manche, les boutons et la barre d'outils reçoivent le doigt en premier, et tout le reste de l'écran tourne la caméra. Un doigt : regarder (pas rapporté à la taille de l'écran, même ressenti partout). Deux doigts : distance. Double appui : `recentreCamera()`. Réglages dans le menu Pause, mémorisés sous `fenua.sens`, `fenua.invy`, `fenua.autocam` ; le panneau d'aide ne s'affiche qu'une fois (`fenua.hint`). `G.autoCam` replace doucement la caméra derrière le joueur quand il court sans toucher l'écran.
- **Sauvegardes** : clés `fenua.best.<i>`, `fenua.q`, `fenua.sens`, `fenua.invy`, `fenua.autocam`, `fenua.hint`.

## Règles à respecter

- **Histoire** : les lieux et la guerre sont réels ; **les personnages sont inventés**. Aucune personne historique n'est ciblée. Le ton reste factuel et respectueux.
- **Culture** : les marae sont tapu, on ne s'y bat pas et on ne les escalade pas. Les mots en reo tahiti (fare, vaʻa, nīʻau, tūpāpaʻu, ʻōtaha, hora, nape, tatau, pāreu, tiare, ahimaʻa, tiʻi…) sont **à faire relire par le propriétaire**, qui connaît la langue.
- **Pas d'anachronisme** : pas de grands immeubles façon Unity. La hauteur vient des cocotiers, mâts, tours de guet, blockhaus, pitons et falaises.
- **Sol** : `MAT.terrain` sert au grand plan comme aux collines (`buildGround`). C'est le sable, où l'herbe (`TEX.herbe`) est mêlée dans le shader selon un attribut de sommet `aHerbe` calculé par `fondAt` ; même projection des coordonnées partout, donc aucune couture au pied des collines. Les touffes (`buildHerbe`, `BRIN_GEO` : trois plans croisés) et les palmes plient au vent dans le shader (`ventShader`, horloge `VENT_U`).
- **Ambiance** : un vent commun (`WIND`, `windGust`) penche arbres, étoffes, drapeaux et fumées dans la même direction, avec des rafales. Une lumière d'appoint froide (`fill`) détache les silhouettes du fond. Le feuillage porte un léger `emissive` pour ne jamais tomber au noir absolu la nuit. Une frange d'écume borde chaque plan d'eau jouable, et les rondes au sol creusent un sentier (`buildPaths`).
- **Textures peintes** : douze textures (sable, herbe, enduit, planches, chaume, basalte, bambou, rondins, écorce, tapa, feuillage, terre) viennent d'une grille générée par Gemini (`textures/grille-gemini.webp`, 4 × 4 cases). `python3 outils/decouper-grille.py` la découpe en `textures/<nom>.webp` (256 × 256, bords raccordés, teinte moyenne ramenée sur celle de l'ancienne texture dessinée pour que la lumière reste juste) ; `node outils/integrer-textures.js` les **intègre dans `docs/fenua.html`** (bloc `<script type="application/json" id="textures-peintes">` entre deux repères), pour que le fichier reste autonome et marche même ouvert depuis le disque. Dans le jeu, `PEINTES` dit quelle texture remplace quelle `TEX.*`, `PEINTES_REPEAT` resserre les motifs trop gros, `PEINTES_MELEE` garde une part de l'ancienne par-dessus (l'enduit garde ses briques). Une texture absente ou illisible laisse la texture dessinée en place. Le sol se répète tous les 7 m et le shader du terrain module sa clarté avec la même texture lue dix fois plus grand, pour casser la répétition. La terre battue (`TEX.terre`) est mêlée au sol dans le shader selon un second attribut de sommet `aTerre` : chemins de ronde, pied des bâtiments, pentes fortes des collines, plaques au hasard. Les stipes des cocotiers ont leur propre texture annelée (`TEX.stipe`, dessinée). La terre craquelée et les variantes de la ligne 4 (écorce, tapa, feuillage) ne servent pas encore.
- **Collines du fond** : passé la lisière, le sol se soulève en collines boisées (`collineFond`, `buildCollinesFond`) jusqu'à 35 m, plat côté mer ; les bosquets de la forêt s'y posent. Pur décor, jamais atteignable.
- **Perf mobile** : sur téléphone, c'est le **nombre d'objets** qui coûte, pas les triangles. Tout élément répété passe par `batch()` ou `InstancedMesh` ; les géométries partagées (`SPH0`, `SPH1`, `CYL6`, `SPH8`, `LEAF_GEO`, `BARREL`) portent `userData.shared` pour que `clearScene` ne les libère pas. Les flambeaux (`TORCH_ACC`/`flushTorches`), les cocotiers droits (`PALM_ACC`) et les poteaux de corde passent aussi en lots. Repère actuel : **630 à 1 180 objets** par niveau (Matavai, deux fois plus grand, et Taravao en haut), 160 à 205 k triangles (le sol en 150 × 150 et les collines du fond comptent) — l'herbe en fait une bonne part, et le mode allégé en plante quatre fois moins. Garder le mode « graphismes allégés » fonctionnel.
- Garder **un seul fichier HTML autonome**. Seules sources externes autorisées : cdnjs, jsdelivr, Google Fonts — `outils/verifier-script.js` refuse tout le reste. Three.js est chargé depuis cdnjs, avec **jsdelivr en secours** ; si aucune des deux ne répond, la page affiche un message en français au lieu d'un écran noir.
- **Mise en ligne** : GitHub Pages en mode « Deploy from a branch », sur la branche par défaut, dossier **`/docs`**. `docs/index.html` renvoie vers `docs/fenua.html`, `docs/.nojekyll` évite le passage par Jekyll. Tout ce qui doit être servi en ligne va donc dans `docs/`. Activer Pages est un réglage du dépôt : un workflow ne peut pas le faire (le jeton d'Actions n'a pas le droit de créer le site).

## Pistes ouvertes

- Voltigeurs qui escaladent à la poursuite du joueur ; fusiliers qui visent depuis les toits voisins.

- Brouillard de guerre sur la mini-carte, levé par les points de vue.
- Se déplacer aux mains d'une **boîte à l'autre** sans lâcher (aujourd'hui `hangShimmy` passe les angles d'une même boîte ; pour changer de bâtiment il faut sauter avec `ledgeJump`).
- Instancier aussi les cocotiers de premier plan et les fares, qui gardent chacun leurs objets pour pouvoir osciller.
- Test de performance sur téléphone réel et **relecture du reo tahiti** (`ʻōrā`, `ʻuru`, `fara`, `meiʻa`, `tiʻi`, `nīʻau`, `nape`, `hora`, `tūpāpaʻu`, `ʻōtaha`…).
