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

    /** Chaque ligne : { touche, explication } ; une touche commençant par « # » est un titre de section. */
    private static final List<String[]> LIGNES = List.of(
            new String[]{"#Escalade", ""},
            new String[]{"Espace contre un mur", "S'accrocher (en sautant contre un mur, on s'y accroche aussi)"},
            new String[]{"Z / S", "Monter / descendre le long du mur (Ctrl : plus vite)"},
            new String[]{"Q / D", "Se déplacer sur le côté"},
            new String[]{"Espace", "Bondir vers la prise du dessus"},
            new String[]{"Q ou D + Espace", "Bondir sur le côté"},
            new String[]{"S + Espace", "Saut arrière vers le mur d'en face (clic droit pour s'y raccrocher)"},
            new String[]{"Maj", "Lâcher prise"},
            new String[]{"(en haut)", "On se rétablit tout seul par-dessus le bord"},
            new String[]{"#Rebords", ""},
            new String[]{"(automatique)", "En tombant le long d'un mur, on saisit le bord qui passe aux mains"},
            new String[]{"Z ou Espace", "Se hisser"},
            new String[]{"Q / D", "Glisser aux mains le long du bord"},
            new String[]{"Q ou D + Espace", "Sauter vers le rebord voisin"},
            new String[]{"S", "Redescendre sur le mur"},
            new String[]{"#Course", ""},
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
            new String[]{"Perchoir", "Point de vue : s'y tenir révèle les gardes pendant 30 s"},
            new String[]{"#Assassin", ""},
            new String[]{"R", "Assassiner le garde entouré de rouge (la lame doit être dans l'inventaire)"},
            new String[]{"(mise en scène)", "Chaque assassinat a son geste ; la caméra passe en vue extérieure le temps du coup"},
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
            new String[]{"Maj dans l'herbe haute", "Cachette : on ne te voit qu'à 2 blocs"},
            new String[]{"Meule de foin", "On y entre, on y plonge de haut sans mal, on s'y cache même debout"},
            new String[]{"Puits", "Saute dedans : caché, les yeux au ras de la margelle ; un saut pour en sortir"},
            new String[]{"Cabine à rideau", "Entre par le rideau : caché debout"},
            new String[]{"Banc public", "Clic droit : assis, on se fond dans le décor. Maj pour se relever"},
            new String[]{"Charrette de foin", "On y plonge sans mal ; accroupi dedans, on est caché"},
            new String[]{"Dans la foule", "Marche parmi deux passants ou plus (sans courir) : invisible"},
            new String[]{"Clic droit sur Tycho, Kepler…", "Les personnages réels racontent leur histoire et donnent les missions"},
            new String[]{"#Commandes du chat", ""},
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
        defile = Math.max(0, Math.min(defile - v * 18, Math.max(0, LIGNES.size() * 12 + 80 - (height - 60))));
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
                ctx.drawTextWithShadow(textRenderer, Text.literal(l[1]), x0 + col, y, TEXTE);
            }
            y += 12;
        }
        ctx.disableScissor();
    }

    @Override public boolean shouldPause() { return true; }
}
