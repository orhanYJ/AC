package fr.fenua.ombre.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** L'écran d'aide (touche H) : toutes les commandes, rangées par thème. Molette pour défiler. */
public class EcranAide extends Screen {
    private static final int TITRE = 0xFFD27F, TOUCHE = 0xFFFFFF, TEXTE = 0xB8C4CC, SECTION = 0x7FD8FF;
    private double defile;
    private int hauteurTotale = 2000;

    /** Chaque ligne : { touche, explication } ; une touche commençant par « # » est un titre de section. */
    private static final List<String[]> LIGNES = List.of(
            new String[]{"#Escalade", ""},
            new String[]{"Espace contre un mur", "S'accrocher (en sautant contre un mur, on s'y accroche aussi)"},
            new String[]{"Z / S", "Monter / descendre le long du mur (Ctrl : plus vite)"},
            new String[]{"Q / D", "Se déplacer sur le côté ; aux angles, on passe sur l'autre face (la caméra suit)"},
            new String[]{"Espace", "Bondir vers la prise du dessus"},
            new String[]{"Q ou D + Espace", "Bondir sur le côté (Ctrl en plus : grand saut latéral)"},
            new String[]{"Z + Q ou D + Espace", "Bond en diagonale, vers le haut et sur le côté"},
            new String[]{"Ctrl + S", "Glissade : on descend le long du mur très vite"},
            new String[]{"Fenêtres", "On s'agrippe à leur rebord : on les longe et on les passe aux mains"},
            new String[]{"S + Espace", "Saut arrière vers le mur d'en face (on s'y raccroche tout seul)"},
            new String[]{"Maj", "Lâcher prise"},
            new String[]{"(en haut)", "On s'arrête suspendu au bord ; Z à nouveau ou Espace : par-dessus. En sprint (Ctrl) : d'un trait"},
            new String[]{"#Rebords", ""},
            new String[]{"(automatique)", "En tombant le long d'un mur, on saisit le bord qui passe aux mains"},
            new String[]{"Z ou Espace", "Se hisser (sur une poutre, on s'arrête au milieu)"},
            new String[]{"Q / D", "Glisser aux mains le long du bord, et autour des angles"},
            new String[]{"Q ou D + Espace", "Sauter vers le rebord voisin"},
            new String[]{"S", "Redescendre sur le mur"},
            new String[]{"#Course", ""},
            new String[]{"Sprint vers un muret", "Franchissement : barrières, murets, étals, charrettes (0,6 à 1,6 bloc) d'un appui de main"},
            new String[]{"Sprint vers le bord d'un toit", "Bond assisté : s'il y a un toit en face (2 à 5 blocs), on s'élance tout seul"},
            new String[]{"Maj au bord, vers le vide", "Descente au rebord : on se laisse glisser et on reste suspendu par les mains"},
            new String[]{"Sprint + Espace contre un mur", "Course verticale : on monte le mur en courant, bien plus haut"},
            new String[]{"Saut en sprint", "Un bond qui porte plus loin ; les marches d'un bloc se montent seules"},
            new String[]{"Sprint + saut le long d'un mur", "Courir sur le mur (Z enfoncé, environ 1,5 s)"},
            new String[]{"Gouttière", "On y grimpe deux fois plus vite"},
            new String[]{"Espace sur le mur", "Rebondir vers le mur d'en face"},
            new String[]{"Maj en touchant le sol", "Roulade : jusqu'à 9 blocs de chute sans mal"},
            new String[]{"Meule, foin, feuillage, auvent", "Saut de la foi : aucune chute n'y fait mal"},
            new String[]{"#Objets de parkour", ""},
            new String[]{"Tyrolienne", "Corde en main : clic droit sur deux poteaux. Pour partir : clic droit sur celui du haut"},
            new String[]{"  ou au vol", "Saute sur le câble, clic droit maintenu : on glisse vers le bas"},
            new String[]{"  pendant la glisse", "Espace : sauter (assassinat aérien possible) ; Maj : lâcher"},
            new String[]{"Monte-charge", "Posé au pied d'un mur. Clic droit dessus : on monte jusqu'au toit"},
            new String[]{"Perche, enseigne, lanterne", "Saute dessus, clic droit maintenu : on se balance. Z : élan et projection"},
            new String[]{"Arbalète à grappin", "Clic droit vers le bord d'un toit (40 blocs) : on est tiré jusqu'en haut"},
            new String[]{"Poutre, corniche", "Pour courir et s'accrocher ; échafaudages et lampadaires s'escaladent"},
            new String[]{"Perchoir, gargouille", "Point de vue : s'y tenir révèle les gardes 30 s et ouvre le voyage rapide"},
            new String[]{"#Assassin", ""},
            new String[]{"R", "Assassiner le garde entouré de rouge (la lame doit être dans l'inventaire)"},
            new String[]{"(mise en scène)", "17 scènes, tirées au hasard dans chaque famille (au sol, en course, aérien, cachette, rebord)"},
            new String[]{"R en courant", "Assassinat en course, de plus loin (jusqu'à 4,6 blocs) : plaquage, lame au passage, bond"},
            new String[]{"/ombre scene <n>", "Imposer la scène du prochain assassinat (1 à 17, voir le mode d'emploi)"},
            new String[]{"  au sol", "Il ne t'a pas repéré et il est devant toi, à 3 blocs"},
            new String[]{"  aérien", "Il est sous toi (toit, rebord, chute) : tu plonges sur lui, même s'il t'a vu"},
            new String[]{"  depuis un rebord", "Suspendu sous un garde qui marche au bord du toit"},
            new String[]{"  depuis la cachette", "Caché (herbe, buisson, meule, puits, cabine), un garde passe tout près : tiré dedans"},
            new String[]{"  double", "Une lame dans chaque main (F) : un 2ᵉ garde distrait tout près tombe aussi"},
            new String[]{"Clic gauche (lame)", "Marche aussi : frapper un garde distrait le tue d'un coup"},
            new String[]{"Clic droit (couteaux)", "Couteau de lancer : tue à distance, sans bruit"},
            new String[]{"Clic droit (fléchettes)", "Soporifique : le garde s'endort 30 s. Empoisonnée : il tombe 4 s plus tard"},
            new String[]{"Clic droit (thalers)", "Jetés au sol : la foule accourt, les gardes viennent voir"},
            new String[]{"Clic droit (pétard)", "Bruit et fumée : la foule s'enfuit, les gardes accourent"},
            new String[]{"Tenue de hallebardier", "Déguisé : les gardes ne te voient qu'à 2 blocs (perdu si tu cours, grimpes, frappes)"},
            new String[]{"Clic droit (bombe)", "Fumée : les gardes autour perdent ta trace 5 s"},
            new String[]{"V", "Instinct : voir les ennemis à travers les murs (toutes les 15 s)"},
            new String[]{"B", "Siffler : attirer le garde le plus proche"},
            new String[]{"Hautes herbes, buissons", "Cachette : debout si la plante te couvre entièrement, sinon accroupi (Maj)"},
            new String[]{"Meule de foin", "On y entre, on y plonge de haut sans mal, on s'y cache même debout"},
            new String[]{"Puits", "Saute dedans : caché, les yeux au ras de la margelle ; un saut pour en sortir"},
            new String[]{"Cabine à rideau", "Entre par le rideau : caché debout"},
            new String[]{"Banc public", "Clic droit : assis, on se fond dans le décor. Maj pour se relever"},
            new String[]{"Charrette de foin", "On y plonge sans mal ; accroupi dedans, on est caché"},
            new String[]{"Dans la foule", "Marche parmi deux passants ou plus (sans courir) : invisible"},
            new String[]{"Clic droit sur Tycho, Kepler…", "Les personnages réels racontent leur histoire et donnent les missions"},
            new String[]{"#Comme dans Assassin's Creed", ""},
            new String[]{"G (maintenir)", "Roue des outils : le jeu se fige, montre un outil à la souris, relâche : il passe en main"},
            new String[]{"N", "Vue de l'aigle : la caméra s'envole ; vise un garde un instant pour le marquer (1 min, à travers les murs)"},
            new String[]{"  avis de recherche", "La vue de l'aigle signale aussi les avis de recherche alentour (✉ AVIS doré) : clic droit pour arracher"},
            new String[]{"Fléchette de folie", "Le garde touché attaque ses camarades pendant 15 s"},
            new String[]{"Plumes de corbeau", "Dix-sept plumes sur les hauteurs de Prague (colonne de lumière) ; le journal les compte"},
            new String[]{"Course des toits", "À Prague, au coin nord-ouest : anneau de flammes sur le toit. Balises à franchir, chrono, record"},
            new String[]{"J", "Carte : points de vue, cibles, journal. Clic (ou 1 à 9) sur un point de vue : voyage rapide"},
            new String[]{"Détection", "Un garde qui te voit se méfie peu à peu : « ? » blanc, puis jaune (il vient voir), « ! » rouge (alerte)"},
            new String[]{"  flèches du viseur", "Elles montrent d'où l'on t'observe et se remplissent avec le soupçon"},
            new String[]{"  pour s'en défaire", "Sors de sa vue, cache-toi, ou lance une bombe de fumée : le soupçon retombe"},
            new String[]{"Zone interdite", "Autour d'un étendard rouge de la garde (14 blocs). Barre jaune : tout près ; rouge : dedans"},
            new String[]{"  dedans", "Les gardes te repèrent deux fois plus vite"},
            new String[]{"Boussole (en haut)", "◆ cibles et personnages à voir, avec la distance ; ✦ points de vue ; ▼ gardes en alerte"},
            new String[]{"Notoriété", "Chaque alerte la fait monter, un innocent tué la fait bondir. À 75 : RECHERCHÉ"},
            new String[]{"  la faire baisser", "Clic droit sur un avis de recherche pour l'arracher (−30), ou se faire oublier"},
            new String[]{"Désynchronisation", "Tuer un passant : l'Ombre ne tue pas d'innocents (notoriété +50)"},
            new String[]{"Vol à la tire", "Accroupi dans le dos d'un passant, clic droit : sa bourse. Vu de face : « Au voleur ! »"},
            new String[]{"Enchaînement", "Après un assassinat, 2,5 s pour en enchaîner un autre au sol, même sur un garde qui t'a vu"},
            new String[]{"Saut de la foi", "Au bord d'un vide, une invite dorée dit quand une meule ou l'eau t'attend en bas ; si une prise est devant toi, regarde vers le bas pour sauter"},
                        new String[]{"#Histoire, quêtes, bourse", ""},
            new String[]{"Journal (à gauche)", "La mémoire en cours et ses étapes ; la quête secondaire en dessous"},
            new String[]{"◆ doré", "Où aller (boussole en haut, mini-carte en bas à droite)"},
            new String[]{"« ! » jaune", "Un habitant qui offre une quête secondaire : clic droit"},
            new String[]{"K", "Compétences : quatre branches (Ombre, Lame, Toits, Ruse), payées en points gagnés par les missions"},
            new String[]{"Marchands", "Armurier, apothicaire, fripier (grande place) : clic droit pour ouvrir l'étal"},
            new String[]{"Fouiller un corps", "Regarde un garde à terre et clic droit : sa bourse et ses affaires"},
            new String[]{"  corps découvert", "Un garde qui voit le corps d'un camarade devient méfiant et va voir"},
            new String[]{"Groupes de passants", "Des gens qui discutent en cercle : marche parmi eux pour disparaître"},
            new String[]{"/ombre memoire <n>", "Rejouer une mémoire de l'histoire (1 à 7)"},
            new String[]{"#Commandes du chat", ""},
            new String[]{"/ombre initiation", "Commence ici ! La Ruelle d'Or : 20 cours, 30 leçons pas à pas, de l'escalade à l'assassinat"},
            new String[]{"/ombre rappel", "Le cours de rappel : tout ce qui a changé depuis l'initiation (19 leçons, Jardin royal)"},
            new String[]{"/ombre initiation <n>", "Reprendre l'initiation à l'étape n (de 1 à 30)"},
            new String[]{"/ombre prague", "La Vieille-Ville de Prague en 1600 : foule, gardes, Týn, pont Charles, cibles"},
            new String[]{"/ombre kit", "Tout l'équipement de l'assassin"},
            new String[]{"/ombre vitrine", "Tous les blocs du mod exposés en rangées"},
            new String[]{"/ombre parcours", "Grande carte d'entraînement au parkour (sans gardes)"},
            new String[]{"/ombre arene", "Un fortin à infiltrer, 9 gardes, une cible"},
            new String[]{"/ombre aide", "Le rappel des commandes dans le chat"},
            new String[]{"H", "Cet écran"}
    );

    public EcranAide() { super(Text.literal("L'Ombre — Prague 1600 — toutes les commandes")); }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), b -> close())
                .dimensions(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        defile = Math.max(0, Math.min(defile - v * 18, Math.max(0, hauteurTotale - (height - 60))));
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, TITRE);
        ctx.enableScissor(0, 26, width, height - 32);
        int col = Math.min(170, width / 3);
        int x0 = Math.max(10, width / 2 - 240);
        int y = 32 - (int) defile;
        for (String[] l : LIGNES) {
            if (l[0].startsWith("#")) {
                y += 6;
                ctx.drawTextWithShadow(textRenderer, Text.literal("— " + l[0].substring(1) + " —"), x0, y, SECTION);
            } else {
                ctx.drawTextWithShadow(textRenderer, Text.literal(l[0]), x0 + 6, y, TOUCHE);
                // Les explications trop longues passent à la ligne
                var lignes = textRenderer.wrapLines(Text.literal(l[1]), Math.max(120, width - (x0 + col) - 10));
                for (int i = 0; i < lignes.size(); i++) {
                    ctx.drawTextWithShadow(textRenderer, lignes.get(i), x0 + col, y, TEXTE);
                    if (i < lignes.size() - 1) y += 10;
                }
            }
            y += 12;
        }
        hauteurTotale = y + (int) defile;
        ctx.disableScissor();
    }

    @Override public boolean shouldPause() { return true; }
}
