# L'Ombre du Fenua — Tahiti, 1844

Jeu d'infiltration 3D dans l'esprit des **anciens Assassin's Creed** (AC1/2, Brotherhood, Black Flag, Mirage) : parcours **en hauteur plus qu'en largeur**, sans dialogue. Il se déroule pendant la guerre franco-tahitienne (1844–1847). Tout le jeu tient dans **un seul fichier HTML** : `fenua.html`, environ 186 Ko.

- Langue : **tout en français** (interface, commentaires du code, réponses).
- Le propriétaire est **débutant en développement** : expliquer simplement, sans jargon inutile.
- Version en ligne (artefact Claude) : https://claude.ai/artifact/TQ5d2nfyt27Eg6cbivfR4G
- Jeu précédent sur le même moteur (Proche-Orient médiéval) : « L'Ombre de Bab el-Ward », https://claude.ai/artifact/JUe3Z3Ed7dPrjVsgM8xWWE

## Lancer et vérifier

- Ouvrir `fenua.html` dans un navigateur. Aucune compilation ; Three.js **r128** est chargé depuis cdnjs.
- **Après toute modification de niveau**, lancer `node outils/valider-niveaux.js`. Ce script vérifie :
  - que les rondes des gardes ne traversent ni mur ni eau ;
  - que les gardes postés en hauteur restent sur leur toit ;
  - que départ, sortie, flambeaux, cachettes et charges ne sont pas dans un mur ;
  - que les cordes ont un appui aux deux bouts ;
  - que les points de vue et les cibles en hauteur sont bien sur un sommet.

  Il doit afficher « 0 conflit ».
- Vérifier la syntaxe JS : extraire le `<script>` et le passer à `new Function(...)` sous Node.
- Tests en navigateur utilisés jusqu'ici : Playwright + Chromium avec `--use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader`, fenêtre 640×360, capture avec `timeout=150000` (le rendu logiciel est lent). Pour tester la logique, on ajoute sur **une copie** du fichier, juste avant `frame();` à la fin :
  ```js
  window.T={G,scene,camera,tryClimb,tryAction,computePrompt,updatePlayer,updateGuards,startLevel,snapCamera,inWater,wallClimb,poleNear,syncVista};
  ```
  On appelle ensuite `updatePlayer(0.05)` / `updateGuards(0.05)` en boucle. **Ne jamais livrer ce crochet.**

## Architecture du fichier

Un seul `<script>` dans une IIFE, découpé en sections commentées :

| § | Contenu |
|---|---|
| 0 | Outils (`clamp`, `lerp`, `angDelta`, `store` = localStorage) |
| 1 | Socle Three.js : rendu ACES, ciel dégradé, étoiles et Croix du Sud, lune, **6 PointLight réaffectées** aux flambeaux les plus proches (`LIGHT_POOL`), textures peintes en canvas (`TEX.*`), matériaux (`MAT.*`), `wallMats(type)` |
| 2 | **Données des niveaux** : `KIND`, `guard()`, `T()`, `R()`, `LEVELS`, `HAZARDS`, `DECO` |
| 3 | Géométrie : boîtes AABB, `circleVsBoxes`, `groundAt`, `lineOfSight` (tient compte de la hauteur) |
| 4 | Personnages : `humanoid(o)` (le résistant tatoué `tahitian:true`, les uniformes `french:true`), `guardLook(def,i)` |
| 5–6 | Sons (WebAudio), arsenal `TOOLS` |
| 7 | État `G`, `buildLevel`, décor et architecture (fare, falaises, jungle, palissades, eau, montagnes, forêt, frégate, marae…), escalade, points de vue |
| 8–9 | Lumière (`lightAt`), entrées clavier, souris et tactile |
| 10 | Actions : assassinats (`startTakedown`), assommer (`tryKO`), outils, corps, charges suspendues |
| 11 | Perception (cônes, bruit, alarme) |
| 12 | Boucle joueur : nage, plongée, escalade, piqué, cordes |
| 13 | Boucle gardes : patrouille, enquête, alerte, pièces, poison, réveil, animations au repos |
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
  | `fare` | toit de chaume visuel au-dessus | non |
  | `jungle` | lisière de forêt | non |
  | `cliff` | falaise, + 10 m visuels | non |
  | `palis` | palissade | non |
  | `tent` | toile | non |
  | `marae` | lieu sacré | non (volontairement) |
  | `crate` | caisse | marche normale |
  | `beam` | poutre, vergue ou hune praticable | on y marche |
  | `void` | mur invisible côté mer | — |

  Les murs d'enceinte (`isRingWall`) ne s'escaladent jamais.
- `coco(boxes,x,z,h)` : cocotier grimpable, collision de 0,7 m. Type `mast` : mât grimpable. Les deux se grimpent jusqu'en haut et se redescendent (`palmClimb`).
- Autres champs d'un niveau : `water:[{x,z,w,d}]` (lagon jouable, les soldats n'y entrent pas), `ocean:[…]` (visuel ; masque aussi les montagnes et la forêt de ce côté), `tapu:[{x,z,r}]` (aucun assassinat possible), `vista:{x,y,z}` (point de vue + objectif facultatif `id:"vista"`), `ground` (teinte du sol), `ropes:[R(a,b)]` (cordes de nape ; on ne les prend que **vers le bas** et **dans la direction du regard**, voir `ropeOK`).
- Gardes : `guard(x,z,{kind,path,y,wait,isTarget,oid,name})`. Les familles sont `soldat` (fusilier), `eclaireur` (voltigeur), `brute` (sapeur, impossible à tuer de face sauf s'il ramasse des pièces), `guetteur` (sentinelle), `torche` (porte-lanterne) et `cible` (officier).
- Flambeaux : `T(x,z,r,"lan")` pour une lanterne française ; sans style, c'est un flambeau de bambou.
- `HAZARDS[i]` : filets de noix de coco (point d'impact + poteau où couper la corde).
- `DECO[i]` : décor sans collision (`palm`, `vaa`, `ship`, `flag`, `cannon`, `marae`, `tapa`, `net`, `tiare`, `ahimaa`, `belfry`, `tent`, `ladder`, `waterfall`, `gable`, `lanterns`).

## Les 4 missions

1. **Matavai** : village occupé. Départ à la nage dans le lagon. Tour de guet (13 m, point de vue), canopée de cocotiers reliés par des cordes jusqu'au toit du poste. Cible : le sergent Aubry.
2. **Papeete** : ordres du gouverneur à voler, sapeur à la porte. **Frégate amarrée** : coque, grand mât, hune (15 m, point de vue), vergue, corde vers le quai. Cible : le capitaine Duval.
3. **Le fortin du récif** : blockhaus escaladable, cordes vers deux toits, tour de signaux au-dessus d'un tas de nīʻau, canon d'alarme à enclouer. Cibles : le capitaine Verneuil et l'ingénieur Morel.
4. **Fautaua** (décembre 1846) : éperons rocheux, piton de 16 m au-dessus du bassin de la cascade (saut de la foi), plans à brûler. Cible : le commandant Lorrain. L'épilogue rappelle que le fort est tombé malgré tout et que la guerre a pris fin en 1847.

## Mécaniques en place

- **Verticalité** : escalade libre d'une traite (`wallClimb`/`startWallClimb`), cocotiers et mâts, saut en course (Espace en courant, environ 3,5 m), cordes, piqué du saut de la foi (atterrissage silencieux dans l'eau ou le nīʻau), points de vue (`syncVista` : caméra qui tourne, tous les soldats marqués).
- **Eau** : nage plus lente ; C pour plonger (invisible) ; les soldats s'arrêtent au bord.
- **Onze assassinats** : dans le dos, aérien, embuscade, double, en pleine course, dans le vide (tirer une sentinelle), depuis la corde, fronde, hora (poison lent), accident, assommer (R, réveil après 45 s sauf si le corps est caché).
- **Outils** : coquillage, piastres, fronde, hora, fumée, poudre.
- **Autres** : lumière des flambeaux, corps à porter et cacher, alarme, notes de fin (Tūpāpaʻu → Tempête), réglage graphique (touche G), commandes tactiles.
- **Sauvegardes** : clés `fenua.best.<i>` et `fenua.q`.

## Règles à respecter

- **Histoire** : les lieux et la guerre sont réels ; **les personnages sont inventés**. Aucune personne historique n'est ciblée. Le ton reste factuel et respectueux.
- **Culture** : les marae sont tapu, on ne s'y bat pas et on ne les escalade pas. Les mots en reo tahiti (fare, vaʻa, nīʻau, tūpāpaʻu, ʻōtaha, hora, nape, tatau, pāreu, tiare, ahimaʻa, tiʻi…) sont **à faire relire par le propriétaire**, qui connaît la langue.
- **Pas d'anachronisme** : pas de grands immeubles façon Unity. La hauteur vient des cocotiers, mâts, tours de guet, blockhaus, pitons et falaises.
- **Perf mobile** : préférer `InstancedMesh` pour les éléments répétés, et garder le mode « graphismes allégés » fonctionnel.
- Garder **un seul fichier HTML autonome**. Seules sources externes autorisées pour la publication en artefact : cdnjs, jsdelivr, Google Fonts.

## Pistes ouvertes

- Grands arbres **ʻōrā** (banians) à plateformes, pour une vraie course dans les branches.
- Toits de fare praticables (ils ne sont que décoratifs), rebords et corniches où se suspendre.
- Brouillard de guerre sur la mini-carte, levé par les points de vue.
- Code mort à nettoyer, hérité de Bab el-Ward : `dome`, `tower`, `crane`, `pots`, `AMPHORA`.
- Test de performance sur téléphone réel et relecture du reo tahiti.
