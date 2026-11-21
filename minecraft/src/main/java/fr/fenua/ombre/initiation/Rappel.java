package fr.fenua.ombre.initiation;

import fr.fenua.ombre.Discretion;
import fr.fenua.ombre.Gardes;
import fr.fenua.ombre.Kit;
import fr.fenua.ombre.OmbreItems;
import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.objet.Objets;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Le cours de rappel (/ombre rappel), dans le Jardin royal du château de Prague :
 * tout ce qui a changé depuis l'initiation, une cour par nouveauté.
 */
public final class Rappel {
    private Rappel() {}

    public static final int ENTREE = 0, POUTRE = 1, BORD = 2, ANGLES = 3, FACADE = 4, FRANCHIR = 5, BONDS = 6, DESCENTE = 7,
            BUISSONS = 8, CINEMA = 9, AIGLE = 10, FOLIE = 11, PLUMES = 12, PRAGUE = 13;
    public static final int[] LEN = {12, 18, 22, 20, 34, 30, 34, 20, 26, 22, 36, 28, 30, 16};
    public static final String[] NOMS = {"Le Jardin royal", "La poutre", "Le bord du mur", "Les angles", "La façade", "Le franchissement",
            "Le bond assisté", "La descente au rebord", "Les buissons", "Les scènes filmées", "La vue de l'aigle", "La fléchette de folie",
            "Les plumes et la course", "Retour à Prague"};

    static final List<Etape> ETAPES = new ArrayList<>();

    private static Etape e(int cour, String titre, String... lignes) {
        Etape s = new Etape(cour, titre, lignes);
        ETAPES.add(s);
        return s;
    }

    static {
        e(ENTREE, "Le cours de rappel",
                "Bienvenue au §6Jardin royal§f du château. Ici, tout ce qui a changé depuis l'initiation.",
                "Comme avant : la grille s'ouvre quand la leçon est réussie ; le §6losange doré§f montre où aller.",
                "(Au passage : les cabines à rideau ne clignotent plus.)",
                "Ouvre la §6carte§f (§6J§f) : le vrai terrain vu du dessus. §6Molette§f : zoom ; §6clic maintenu§f : glisser.")
                .debut(c -> { c.p.removeCommandTag("ombre_carte"); Kit.donner(c.p); })
                .but(10, 0, 7).si(c -> c.p.getCommandTags().contains("ombre_carte")).ouvre();

        e(POUTRE, "Se hisser sur une poutre",
                "Saute du palier vers la poutre : tes mains la saisissent.",
                "§6Espace§f : tu te hisses et tu t'arrêtes §6pile au milieu§f de la poutre, au lieu de passer par-dessus.",
                "(L'eau, en dessous, ne fait pas mal. Pour un saut de la foi, il faudrait regarder vers le bas en sautant.)")
                .but(9, 5, 7).si(c -> c.p.isOnGround() && c.w.getBlockState(c.p.getBlockPos()).isOf(OmbreBlocs.POUTRE)).ouvre();

        e(BORD, "S'arrêter au bord",
                "Grimpe le mur avec §6Z§f, même sans le lâcher : en haut, tu restes §6suspendu au bord§f.",
                "§6Z§f à nouveau ou §6Espace§f : passer par-dessus. En sprint (§6Ctrl§f), on passe d'un trait.",
                "Nouveau : en §6sprint§f, §6Espace§f contre un mur, c'est une §6course verticale§f : on monte bien plus haut d'un coup.")
                .but(7, 6, 7).si(c -> c.suspendu(8, 5.5, 8.2));
        e(BORD, "L'assassinat depuis le bord",
                "Un mannequin monte la garde juste au-dessus de toi.",
                "Entouré de §crouge§f : §6R§f. Il passe par-dessus le bord — et c'est filmé, comme tout assassinat.")
                .debut(c -> c.garde("ombre_init_g1", 9, 8, 7, 90, false, true, 0))
                .but(8, 8, 7).si(c -> c.morts("ombre_init_g1")).ouvre();

        e(ANGLES, "Passer les angles",
                "Grimpe le pilier par sa face ouest, puis garde §6D§f (ou §6Q§f) enfoncé :",
                "aux angles, tu passes sur la face suivante et la caméra tourne avec toi.",
                "Fais le tour jusqu'à la §6face est§f. Ça marche aussi suspendu au bord, et dans les coins rentrants.")
                .but(10, 3, 7).si(c -> c.enHauteur(0.25) && c.x() > 10.1 && c.z() > 5 && c.z() < 10).ouvre();

        e(FACADE, "Longer une façade",
                "Accroche-toi à la façade et longe-la avec §6D§f : tu glisses en douceur, et tu passes les §6fenêtres§f aux mains.",
                "Au milieu, un trou de trois blocs : §6Ctrl§f + §6D§f + §6Espace§f, le §6grand saut latéral§f.",
                "(§6Z§f + §6D§f + §6Espace§f : un bond en diagonale.) Atteins le bout de la façade, en hauteur.")
                .but(30, 4, 1).si(c -> c.enHauteur(2) && c.x() >= 28);
        e(FACADE, "La glissade",
                "Pour redescendre vite : §6Ctrl§f + §6S§f. Tu glisses le long du mur dans un nuage de poussière.",
                "Pose le pied au sol.")
                .but(30, 0, 3).si(c -> c.p.isOnGround() && c.x() >= 26).ouvre();

        e(FRANCHIR, "Le franchissement",
                "Ces barrières et ces murets sont trop hauts pour un saut ordinaire.",
                "§6Sprinte§f droit dessus : tu les franchis d'un appui de main, sans t'arrêter.",
                "(Ça marche aussi sur les étals, les charrettes, les garde-corps des rues de Prague.)")
                .but(27, 0, 7).si(c -> c.x() >= 27).ouvre();

        e(BONDS, "Le bond assisté",
                "Monte sur le premier toit et §6sprinte§f vers le bord, §6Z§f enfoncé :",
                "s'il y a un toit en face, tu t'élances tout seul, juste assez fort pour l'atteindre (jusqu'à 5 blocs).",
                "Traverse les quatre toits. (Tomber dans l'eau ne fait pas mal.)")
                .but(31, 5, 7).si(c -> c.p.isOnGround() && c.x() >= 29 && c.y() >= 4.5).ouvre();

        e(DESCENTE, "La descente au rebord",
                "Monte sur la tour par l'échelle. Au bord, §6accroupi§f (§6Maj§f), pousse vers le vide un instant :",
                "tu te laisses glisser et tu restes §6suspendu par les mains§f. De là : assassinat depuis le rebord,",
                "ou §6Maj§f pour lâcher. Suspends-toi au bord est de la tour.")
                .but(11, 7, 7).si(c -> c.suspendu(9, 9.0, 10.6)).ouvre();

        e(BUISSONS, "Caché debout",
                "Une recrue fait sa ronde. Les grands buissons et les lilas te cachent §6même debout§f,",
                "s'ils te couvrent des pieds à la tête. (Les hautes herbes basses, elles, demandent d'être accroupi.)",
                "Rejoins le buisson marqué, debout, et restes-y sans te faire voir.")
                .debut(c -> c.garde("ombre_init_g1", 22, 0, 7, -90, true, false, 3))
                .but(8, 0, 4).si(c -> !c.p.isSneaking() && Discretion.dansCachette(c.p) && c.x() >= 6).rate(Ctx::repere).ouvre();

        e(CINEMA, "La synchronisation filmée",
                "Grimpe la tour et tiens-toi sur la §6gargouille§f : la caméra s'envole comme un aigle,",
                "fait le tour du point de vue, et la jauge « Synchronisation » se remplit.")
                .but(6, 18, 7).si(c -> Gardes.dejaVu(c.p, c.pos(6, 18, 7)));
        e(CINEMA, "Le saut de la foi filmé",
                "Avance au bord : l'invite dorée paraît. Saute ! La caméra te suit, tu plonges bras en croix,",
                "et le foin vole à l'arrivée.")
                .but(9, 1, 7).si(c -> c.w.getBlockState(c.p.getBlockPos()).isOf(OmbreBlocs.MEULE)).ouvre();

        e(AIGLE, "La vue de l'aigle",
                "Appuie sur §6N§f : la caméra s'envole au-dessus de toi. Ton corps attend en bas.",
                "Dirige le regard à la souris. §6N§f à nouveau pour revenir.")
                .debut(c -> c.p.removeCommandTag("ombre_aigle"))
                .but(4, 0, 7).si(c -> c.p.getCommandTags().contains("ombre_aigle"));
        e(AIGLE, "Marquer les gardes",
                "Trois mannequins se cachent derrière les murs, au fond. Depuis le ciel, §6vise-les un instant§f chacun :",
                "le réticule se remplit, et ils restent §6marqués§f une minute, visibles à travers les murs.")
                .debut(c -> {
                    c.garde("ombre_init_g1", 24, 0, 2, 0, false, true, 0);
                    c.garde("ombre_init_g1", 30, 0, 7, 0, false, true, 0);
                    c.garde("ombre_init_g1", 26, 0, 12, 0, false, true, 0);
                })
                .but(27, 0, 7).si(c -> {
                    var g = c.vivants("ombre_init_g1");
                    return !g.isEmpty() && g.stream().allMatch(m -> m.getCommandTags().contains("ombre_marque"));
                }).ouvre();

        e(FOLIE, "La fléchette de folie",
                "Deux gardes, au fond d'une fosse. Prends la §6fléchette de folie§f (roue §6G§f) et tire sur l'un d'eux :",
                "pendant 15 s, il s'en prend à son camarade, qui se défend.",
                "Attends que l'un des deux tombe.")
                .debut(c -> {
                    c.assurer(Objets.FLECHETTE_FOLIE, 4);
                    for (int z : new int[]{5, 9}) {
                        MobEntity g = c.garde("ombre_init_g1", 18, -3, z, 90, true, false, 2);
                        if (g != null) g.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                    }
                })
                .but(14, 0, 7).si(c -> c.vivants("ombre_init_g1").size() <= 1).ouvre();

        e(PLUMES, "Les plumes de corbeau",
                "Sur les hauteurs de Prague, §6quinze plumes de corbeau§f t'attendent : une colonne de lumière les signale.",
                "Le journal compte celles que tu as. Ici, entraîne-toi : ramasse les trois plumes de la cour.")
                .but(15, 6, 7).si(Ctx::plumesRamassees);
        e(PLUMES, "La course des toits",
                "À Prague, au coin nord-ouest de la ville, un §6anneau de flammes§f sur le toit marque le départ.",
                "Franchis les balises dans l'ordre, sans toucher la rue : passerelles, échelles, bonds.",
                "Le chrono s'affiche en bas, le record est gardé. Va à la grille.")
                .but(28, 0, 7).si(c -> c.x() >= 27).ouvre();

        e(PRAGUE, "Ce qui t'attend à Prague",
                "§6Les maisons se visitent§f : porte, échelle, boutiques du métier, chambres. Certaines ont un §6toit-terrasse§f",
                "derrière leur attique (une trappe y mène). Sur la grande place, des §6arcades§f, le §6marché§f et la fontaine ;",
                "deux §6tavernes§f, un §6chantier§f et sa grue, le §6jardin du couvent§f, le §6moulin§f, des radeaux sur la Vltava.",
                "Tape §6/ombre prague§f quand tu es prêt.")
                .but(10, 0, 7).si(c -> c.x() >= 9);
    }

    public static final Cours COURS = new Cours("rappel", "Rappel", "/ombre rappel", LEN, NOMS, ETAPES, -1,
            "Rappel terminé", "Tu sais tout ce qui a changé. Tape §6/ombre prague§f, et bonne chasse.",
            "Te revoilà, l'Ombre. Bien des choses ont changé : je vais te les montrer, une par une.");
}
