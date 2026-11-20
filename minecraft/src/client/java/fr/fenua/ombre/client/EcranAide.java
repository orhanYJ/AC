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
            new String[]{"Espace contre un mur", "S'accrocher au mur (en sautant vers lui, on s'accroche aussi au vol)"},
            new String[]{"Z / S", "Monter / descendre le long du mur (Ctrl : plus vite)"},
            new String[]{"Q / D", "Se déplacer sur le côté"},
            new String[]{"Espace", "Bondir vers la prise du dessus"},
            new String[]{"Q ou D + Espace", "Bondir sur le côté"},
            new String[]{"S + Espace", "Saut arrière vers le mur d'en face (on s'y raccroche seul)"},
            new String[]{"Maj", "Lâcher prise"},
            new String[]{"(en haut)", "On se rétablit tout seul par-dessus le bord"},
            new String[]{"#Course libre (Ctrl + Z)", ""},
            new String[]{"Contre un mur bas", "On s'y rétablit tout seul"},
            new String[]{"Contre un mur haut", "On l'escalade tout seul, sans Espace"},
            new String[]{"Au bord d'un toit", "On saute seul vers l'appui d'en face (5 blocs max)"},
            new String[]{"Au-dessus d'un grand vide", "On s'arrête net au bord ; Espace pour sauter quand même"},
            new String[]{"#Rebords", ""},
            new String[]{"(automatique)", "En tombant le long d'un mur, on saisit le bord qui passe aux mains"},
            new String[]{"Z ou Espace", "Se hisser"},
            new String[]{"Q / D", "Glisser aux mains le long du bord"},
            new String[]{"Q ou D + Espace", "Sauter vers le rebord voisin"},
            new String[]{"S", "Redescendre sur le mur"},
            new String[]{"#Course", ""},
            new String[]{"Saut en sprint", "Un bond qui porte plus loin ; les marches d'un bloc se montent seules"},
            new String[]{"Sprint + saut le long d'un mur", "Courir sur le mur (Z enfoncé, environ 1,5 s)"},
            new String[]{"Espace sur le mur", "Rebondir vers le mur d'en face"},
            new String[]{"Maj en touchant le sol", "Roulade : jusqu'à 9 blocs de chute sans mal"},
            new String[]{"Meule, foin, feuillage, auvent", "Saut de la foi : aucune chute n'y fait mal"},
            new String[]{"#Objets de parkour", ""},
            new String[]{"Tyrolienne", "Corde en main : clic droit sur deux poteaux. Pour partir : Espace près du poteau du haut"},
            new String[]{"  ou au vol", "Saute sur le câble : on s'y accroche et on glisse vers le bas"},
            new String[]{"  pendant la glisse", "Espace : sauter (assassinat aérien possible) ; Maj : lâcher"},
            new String[]{"Monte-charge", "Posé au pied d'un mur. Monte dessus et Espace (ou clic droit) : on monte au toit"},
            new String[]{"Perche, enseigne", "Saute dessus : on se balance. Z donne de l'élan et projette, Espace saute"},
            new String[]{"Poutre, corniche", "Pour courir et s'accrocher ; échafaudages et lampadaires s'escaladent"},
            new String[]{"Perchoir", "Point de vue : s'y tenir révèle les gardes pendant 30 s"},
            new String[]{"#Assassin", ""},
            new String[]{"R", "Assassiner le garde entouré de rouge (la lame doit être dans l'inventaire)"},
            new String[]{"  au sol", "Il ne t'a pas repéré et il est devant toi, à 3 blocs"},
            new String[]{"  aérien", "Il est sous toi (toit, rebord, chute) : tu plonges sur lui, même s'il t'a vu"},
            new String[]{"  depuis un rebord", "Suspendu sous un garde qui marche au bord du toit"},
            new String[]{"  depuis la cachette", "Caché (herbe, buisson, meule, puits, cabine), un garde passe tout près : tiré dedans"},
            new String[]{"  double", "Une lame dans chaque main (F) : un 2ᵉ garde distrait tout près tombe aussi"},
            new String[]{"Clic gauche (lame)", "Marche aussi : frapper un garde distrait le tue d'un coup"},
            new String[]{"Clic droit (couteaux)", "Couteau de lancer : tue à distance, sans bruit"},
            new String[]{"Clic droit (bombe)", "Fumée : les gardes autour perdent ta trace 5 s"},
            new String[]{"V", "Instinct : voir les ennemis à travers les murs (toutes les 15 s)"},
            new String[]{"B", "Siffler : attirer le garde le plus proche"},
            new String[]{"Maj dans l'herbe haute", "Cachette : on ne te voit qu'à 2 blocs"},
            new String[]{"Meule de foin", "On y entre, on y plonge de haut sans mal, on s'y cache même debout"},
            new String[]{"Puits", "Saute dedans : caché, les yeux au ras de la margelle ; un saut pour en sortir"},
            new String[]{"Cabine à rideau", "Entre par le rideau : caché debout"},
            new String[]{"#Commandes du chat", ""},
            new String[]{"/ombre parcours", "Grande carte d'entraînement au parkour (sans gardes)"},
            new String[]{"/ombre arene", "Un fortin à infiltrer, 9 gardes, une cible"},
            new String[]{"/ombre aide", "Le rappel des commandes dans le chat"},
            new String[]{"H", "Cet écran"}
    );

    public EcranAide() { super(Text.literal("L'Ombre — toutes les commandes")); }

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
