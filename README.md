# L'Ombre du Fenua — Tahiti, 1844

**▶ Jouer : https://orhanyj.github.io/AC/**

Jeu d'infiltration 3D dans l'esprit des anciens *Assassin's Creed* : le parcours
passe **en hauteur plus qu'en largeur**, et il n'y a aucun dialogue. L'action se
déroule pendant la guerre franco-tahitienne (1844–1847).

Tout le jeu tient dans **un seul fichier HTML autonome**, `docs/fenua.html`.
Aucune compilation : on l'ouvre dans un navigateur et ça marche. Three.js est
chargé depuis cdnjs, avec jsdelivr en secours.

## Les cinq missions

| | Lieu | Contrat |
|---|---|---|
| 1 | **Matavai** | Le village occupé. Le sergent Aubry, sur le toit du poste. |
| 2 | **Papeete** | Le port du gouverneur. Les ordres à voler, le capitaine Duval. |
| 3 | **Le fortin du récif** | Deux officiers sur deux toits opposés, une nuit. |
| 4 | **Fautaua** | Décembre 1846, les falaises. Le commandant Lorrain. |
| 5 | **Taravao** | Janvier 1847, l'isthme. Le dernier passage vers la presqu'île. |

## Comment on joue

**Au clavier** — Z Q S D pour se déplacer, Maj pour courir, C pour s'accroupir
et plonger, **Espace** pour tout ce qui monte (escalader, grimper, sauter,
prendre une corde, se suspendre à un rebord, se rétablir), E pour agir et
assassiner, R pour assommer, T pour siffler, 1 à 6 pour choisir un outil et F
pour le lancer. La souris tourne la caméra, la molette règle la distance.

**À la manette** — reconnue dès qu'on y touche : stick gauche pour marcher,
stick droit pour regarder, A grimper, X agir, Y assommer, B s'accroupir,
gâchette droite pour courir.

**Sur téléphone** — glisse le doigt **n'importe où** sur l'écran pour tourner la
caméra ; deux doigts règlent la distance ; un double appui remet la caméra
derrière l'épaule. Le manche, les boutons et la barre d'outils gardent la
priorité sur le reste de l'écran. Sensibilité, regard inversé et caméra qui suit
se règlent dans le menu Pause.

## Développer

```sh
# lancer : aucune compilation, on ouvre le fichier
open docs/fenua.html

# après toute modification de niveau — doit afficher « 0 conflit »
node outils/valider-niveaux.js

# syntaxe du script, crochet de test oublié, sources externes interdites
node outils/verifier-script.js
```

Les deux vérifications tournent aussi dans GitHub Actions à chaque poussée
(`.github/workflows/verifier.yml`).

`CLAUDE.md` décrit l'architecture du fichier, le format des niveaux et les
règles du projet.

## Mise en ligne

Le site est servi par **GitHub Pages**, en mode « Deploy from a branch »,
dossier **`/docs`** : Pages publie le contenu de `docs/`, où `index.html`
renvoie vers `fenua.html`. Chaque poussée est donc mise en ligne toute seule,
en une minute environ.

C'est pour cela que le jeu vit dans `docs/` et non à la racine : c'est le seul
des deux dossiers que Pages sait publier en plus de la racine.

Un seul réglage est à faire **une fois**, dans le dépôt :

> **Settings → Pages → Build and deployment**
> · Source : `Deploy from a branch`
> · Branch : `claude/jeu-infiltration-file-pxebtb` et dossier **`/docs`**
> · **Save**

Ce réglage ne peut pas être fait depuis un workflow : créer un site Pages
demande un droit d'administration que le jeton de GitHub Actions n'a pas
(`Resource not accessible by integration`).

## Histoire et culture

Les lieux et la guerre sont réels ; **les personnages et les missions sont
inventés**. Aucune personne historique n'est ciblée. Les marae sont tapu : on
n'y verse pas le sang et on ne les escalade pas.
