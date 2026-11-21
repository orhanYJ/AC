package fr.fenua.ombre.initiation;

import fr.fenua.ombre.Discretion;
import fr.fenua.ombre.Gardes;
import fr.fenua.ombre.Kit;
import fr.fenua.ombre.OmbreItems;
import fr.fenua.ombre.ac.Detection;
import fr.fenua.ombre.ac.Notoriete;
import fr.fenua.ombre.bloc.OmbreBlocs;
import fr.fenua.ombre.objet.Effets;
import fr.fenua.ombre.objet.Objets;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * L'initiation : la Ruelle d'Or du château de Prague, une suite de cours fermées par des grilles.
 * Chaque cour enseigne un geste ; quand l'étape est réussie, la grille s'ouvre.
 * Le moteur (étapes, grilles, échecs) est dans {@link Cours}.
 */
public final class Initiation {
    private Initiation() {}

    public static final String TAG = "ombre_init", AVEUGLE = "ombre_aveugle";

    /** Les cours, d'ouest en est : leur longueur (le long de x). La largeur est toujours de 15 (z = 0 à 14). */
    public static final int ENTREE = 0, ESCALADE = 1, ARRIERE = 2, REBORDS = 3, BONDS = 4, MUR = 5, PERCHES = 6, TYRO = 7,
            VUE = 8, ARMURERIE = 9, CACHETTES = 10, FOULE = 11, ASSASSINATS = 12, DISTANCE = 13, DEGUISE = 14, ZONE = 15,
            NOTORIETE = 16, VOL = 17, GRAPPIN = 18, FIN = 19;
    public static final int[] LEN = {12, 16, 16, 22, 40, 22, 32, 32, 22, 12, 24, 28, 32, 26, 22, 32, 16, 18, 22, 14};
    public static final String[] NOMS = {"L'entrée", "L'escalade", "Le saut arrière", "Les rebords", "La course libre", "La course sur mur",
            "Les perches", "Monte-charge et tyrolienne", "Le point de vue", "L'armurerie", "Les cachettes", "La foule",
            "La cour d'armes", "Les armes de jet", "Le déguisement", "La zone interdite", "La notoriété", "Le vol à la tire",
            "Le lance-grappin", "La sortie"};
    /** Début (x) de chaque cour ; un mur d'un bloc sépare deux cours. */
    public static final int[] S = new int[LEN.length];
    public static final int LONGUEUR;
    static {
        int x = 0;
        for (int i = 0; i < LEN.length; i++) { S[i] = x; x += LEN[i] + 1; }
        LONGUEUR = x - 1;
    }

    // ───────────────────────── les étapes ─────────────────────────

    static final List<Etape> ETAPES = new ArrayList<>();

    private static Etape e(int cour, String titre, String... lignes) {
        Etape s = new Etape(cour, titre, lignes);
        ETAPES.add(s);
        return s;
    }

    static {
        e(ENTREE, "Bienvenue, novice",
                "Bienvenue dans la §6Ruelle d'Or§f, au château de Prague.",
                "Chaque cour t'apprend un geste ; la grille s'ouvre quand tu l'as réussi.",
                "En haut de l'écran, la §6boussole§f : le §6losange doré§f montre où aller.",
                "La touche §6H§f ouvre l'aide complète, à tout moment. Avance jusqu'à la grille.")
                .but(10, 0, 7).si(c -> c.x() >= 9).ouvre();

        e(ESCALADE, "Escalade",
                "Cours vers le mur et appuie sur §6Espace§f : tu t'accroches.",
                "§6Z§f monte, §6S§f descend, §6Q§f / §6D§f sur le côté (les angles se passent), §6Maj§f lâche.",
                "Le long de la §6gouttière§f, on grimpe deux fois plus vite. En haut, tu t'arrêtes suspendu au bord :",
                "§6Z§f à nouveau ou §6Espace§f pour passer par-dessus (en sprint, §6Ctrl§f, on passe d'un trait).",
                "Monte sur le mur, puis saute dans le foin de l'autre côté.")
                .but(7, 8, 7).si(c -> c.sur(6, 7, 8)).ouvre();

        e(ARRIERE, "Bondir et sauter en arrière",
                "Accroché à un mur : §6Espace§f bondit vers le haut, §6Q§f/§6D§f + §6Espace§f sur le côté, §6Z§f + §6Q§f/§6D§f + §6Espace§f en diagonale.",
                "Le mur suspendu (passe dessous) ne se grimpe pas depuis le sol.",
                "Grimpe le mur d'en face, à mi-hauteur fais §6S§f + §6Espace§f : saut arrière,",
                "tu te raccroches au mur suspendu. Monte dessus.")
                .but(4, 10, 7).si(c -> c.sur(3, 4, 10)).ouvre();

        e(REBORDS, "Les rebords",
                "Monte sur le palier et saute vers le premier mur : tes mains saisissent le bord.",
                "Suspendu : §6Z§f ou §6Espace§f pour te hisser, §6Q§f / §6D§f pour glisser aux mains.",
                "§6Q§f / §6D§f + §6Espace§f : sauter au rebord voisin. §6S§f + §6Espace§f : saut arrière. §6Maj§f : lâcher.",
                "Atteins le haut du troisième mur. (L'eau ne fait pas mal.)")
                .but(16, 9, 7).si(c -> c.sur(15, 16, 9)).ouvre();

        e(BONDS, "Course libre et bonds",
                "Maintiens le §6sprint§f (Ctrl, ou double Z) : tu montes les marches d'un bloc sans sauter.",
                "En sprint, un saut porte bien plus loin.",
                "Saute de pilier en pilier : 2, 3 puis 4 blocs d'écart.",
                "Une §6roulade§f : garde §6Maj§f en touchant le sol, jusqu'à 9 blocs de chute sans mal.")
                .but(34, 4, 7).si(c -> c.sur(33, 36, 4)).ouvre();

        e(MUR, "Course sur mur",
                "Sprinte le long du grand mur, saute tout contre lui et garde §6Z§f enfoncé :",
                "tu cours sur le mur environ une seconde et demie, de quoi passer l'eau.",
                "Pendant la course, §6Espace§f rebondit vers l'autre côté.")
                .but(16, 0, 9).si(c -> c.x() >= 14 && c.p.isOnGround() && !c.p.isTouchingWater()).ouvre();

        e(PERCHES, "Les perches",
                "Monte sur le palier, saute vers la perche et §6maintiens le clic droit§f : tu t'y balances.",
                "§6Z§f donne de l'élan ; §6Espace§f (ou l'élan au plus haut) te projette vers la suivante.",
                "Le même clic droit maintenu saisit les §6enseignes§f et les §6lanternes à potence§f des rues.")
                .but(28, 3, 7).si(c -> c.sur(26, 30, 3)).ouvre();

        e(TYRO, "Le monte-charge",
                "Au pied de la tour, la plate-forme de bois : §6clic droit§f dessus.",
                "Le monte-charge te hisse jusqu'au sommet.")
                .but(3, 16, 7).si(c -> c.sur(2, 4, 16));
        e(TYRO, "La tyrolienne",
                "Au sommet, §6clic droit§f sur le poteau : tu glisses le long du câble.",
                "En l'air près d'un câble, §6clic droit maintenu§f t'y accroche aussi.",
                "Sur le câble : §6Espace§f saute (assassinat aérien possible), §6Maj§f lâche.")
                .but(26, 6, 7).si(c -> c.sur(24, 28, 6)).ouvre();

        e(VUE, "Le point de vue",
                "Grimpe la tour par la gouttière jusqu'à la §6gargouille§f, et tiens-toi dessus.",
                "Tu §6synchronises§f : la caméra fait le tour, les gardes alentour sont révélés 30 s,",
                "et le lieu s'ajoute à ta carte.")
                .but(6, 18, 7).si(c -> Gardes.dejaVu(c.p, c.pos(6, 18, 7)));
        e(VUE, "Le saut de la foi",
                "Au bord, une §6invite dorée§f annonce qu'une meule t'attend en bas.",
                "Saute ! La meule de foin t'accueille sans le moindre mal.",
                "(Accroupi dans la meule, tu es aussi caché.)")
                .but(9, 1, 7).si(c -> c.w.getBlockState(c.p.getBlockPos()).isOf(OmbreBlocs.MEULE));
        e(VUE, "Carte et voyage rapide",
                "Appuie sur §6J§f : la carte. Clique sur le point de vue (ou appuie sur §61§f).",
                "Tu y retournes d'un coup. Impossible si des gardes te cherchent.",
                "Puis redescends par un saut de la foi et passe la grille.")
                .debut(c -> c.p.removeCommandTag("ombre_voyage"))
                .but(6, 18, 7).si(c -> c.p.getCommandTags().contains("ombre_voyage")).ouvre();

        e(ARMURERIE, "La roue des outils",
                "Voici ton équipement. Maintiens §6G§f : la roue s'ouvre et le temps s'arrête.",
                "Montre la §6bombe de fumée§f avec la souris, puis relâche §6G§f : elle passe en main.")
                .debut(c -> { Kit.donner(c.p); c.dire("Prends ces armes. Chacune a sa place dans la roue."); })
                .but(6, 0, 7).si(c -> c.p.getMainHandStack().isOf(OmbreItems.BOMBE_FUMEE));
        e(ARMURERIE, "L'instinct",
                "Appuie sur §6V§f : l'§6Instinct§f révèle les ennemis à travers les murs, 10 s.",
                "Il se recharge en 15 s. §6B§f, lui, sert à §6siffler§f : tu verras bientôt.")
                .debut(c -> c.p.removeCommandTag("ombre_instinct"))
                .but(10, 0, 7).si(c -> c.p.getCommandTags().contains("ombre_instinct")).ouvre();

        e(CACHETTES, "Se cacher",
                "Une recrue fait sa ronde. Elle est désarmée : c'est l'entraînement.",
                "Cache-toi : §6accroupi (Maj)§f dans les hautes herbes, ou dans la meule, le puits, la cabine.",
                "Assis sur un banc (§6clic droit§f), tu te fonds aussi dans le décor.",
                "Si elle te repère (« ! »), tu reviens à l'entrée de la cour.")
                .debut(c -> c.garde("ombre_init_g1", 12, 0, 7, 90, true, false, 5))
                .but(4, 0, 3).si(c -> Discretion.dansCachette(c.p)).rate(Ctx::repere);
        e(CACHETTES, "Assassinat depuis la cachette",
                "Siffle (§6B§f) : la recrue vient voir d'où vient le bruit.",
                "Quand elle passe tout près, entourée de §crouge§f : §6R§f. Tu l'attires dans ta cachette.")
                .but(4, 0, 3).si(c -> c.morts("ombre_init_g1")).rate(Ctx::repere).ouvre();

        e(FOULE, "Se fondre dans la foule",
                "Ce garde surveille la rue : seul, il te repérerait de loin.",
                "Marche §6parmi les passants§f, deux autour de toi, sans courir : « Dans la foule ».",
                "Remonte la rue jusqu'à lui sans te faire repérer.")
                .debut(c -> {
                    c.garde("ombre_init_g1", 25, 0, 4, 90, false, false, 0);
                    for (int i = 0; i < 16; i++) c.passant(3 + (i % 8) * 3, 5 + (i / 8) * 3 + (i % 2), (i % 3) * 120f, true, 3);
                })
                .but(24, 0, 7).si(c -> c.x() >= 23).rate(Ctx::repere).ouvre();

        e(ASSASSINATS, "Assassinat dans le dos",
                "Ce mannequin regarde le mur. Approche §6par derrière§f (accroupi, c'est plus sûr).",
                "Quand il est entouré de §crouge§f : §6R§f. (Un clic gauche avec la lame marche aussi.)")
                .debut(c -> {
                    c.garde("ombre_init_g1", 5, 0, 11, 0, false, true, 0);
                    c.garde("ombre_init_g2", 14, 0, 8, 90, false, true, 0);   // à côté de la poutre : sinon elle le cache
                    c.garde("ombre_init_g3", 20, 4, 7, -90, false, true, 0);
                    c.garde("ombre_init_g4", 27, 0, 6, -90, false, true, 0);
                    c.garde("ombre_init_g4", 27, 0, 8, -90, false, true, 0);
                })
                .but(5, 0, 10).si(c -> c.morts("ombre_init_g1"));
        e(ASSASSINATS, "Assassinat aérien",
                "Grimpe sur la poutre par son pilier, avance jusqu'au-dessus du mannequin, en contrebas.",
                "Il est sous toi et entouré de §crouge§f : §6R§f, tu plonges sur lui.",
                "Ça marche aussi en sautant d'un toit, ou du câble d'une tyrolienne.")
                .but(12, 5, 8).si(c -> c.morts("ombre_init_g2"));
        e(ASSASSINATS, "Assassinat depuis le rebord",
                "Un mannequin monte la garde sur le rempart.",
                "Grimpe le rempart : arrivé en haut, tu restes §6suspendu au bord§f, ses pieds au-dessus de ta tête.",
                "§6R§f : tu le tires par-dessus le bord.")
                .but(19, 2, 7).si(c -> c.morts("ombre_init_g3"));
        e(ASSASSINATS, "Double assassinat",
                "Une §6lame dans chaque main§f : prends la seconde lame, appuie sur §6F§f (main gauche),",
                "puis reprends la première en main droite.",
                "Deux mannequins côte à côte : §6R§f sur l'un, l'autre tombe aussi.",
                "Juste après un assassinat, tu as 2,5 s pour en §6enchaîner§f un autre.")
                .debut(c -> c.assurer(OmbreItems.LAME, 1))
                .but(26, 0, 7).si(c -> c.morts("ombre_init_g4")).ouvre();

        e(DISTANCE, "Couteaux de lancer",
                "Un mannequin sur la tour, de l'autre côté du canal.",
                "Prends les §6couteaux de lancer§f (roue §6G§f), vise-le, §6clic droit§f : silencieux et sans approche.")
                .debut(c -> {
                    c.assurer(OmbreItems.COUTEAUX, 8);
                    c.assurer(Objets.FLECHETTE_SOPORIFIQUE, 4);
                    c.garde("ombre_init_g1", 19, 5, 3, 90, false, true, 0);
                    c.garde("ombre_init_g2", 17, 0, 11, 90, false, true, 0);
                })
                .but(19, 5, 3).si(c -> c.morts("ombre_init_g1"));
        e(DISTANCE, "Fléchette soporifique",
                "Le second mannequin : une §6fléchette soporifique§f (roue §6G§f, clic droit).",
                "Il s'endort 30 s et ne voit plus rien. L'§6empoisonnée§f, elle, tue 4 s plus tard, sans bruit.",
                "Ensuite, traverse le canal à la nage.")
                .but(17, 0, 11).si(c -> {
                    for (MobEntity m : c.vivants("ombre_init_g2")) if (!m.hasStatusEffect(Effets.ENDORMI)) return false;
                    return true;
                }).ouvre();

        e(DEGUISE, "Le déguisement",
                "Deux gardes tiennent le passage ; eux, ils voient très bien.",
                "Prends la §6tenue de hallebardier§f (roue §6G§f) et fais §6clic droit§f : tu l'enfiles.")
                .debut(c -> {
                    c.assurer(Objets.TENUE, 1);
                    c.garde("ombre_init_g1", 18, 0, 5, 90, false, false, 0);   // à 17 blocs : hors de vue depuis l'entrée
                    c.garde("ombre_init_g1", 18, 0, 9, 90, false, false, 0);
                })
                .but(5, 0, 7).si(c -> c.p.hasStatusEffect(Effets.DEGUISE)).rate(Ctx::repere);
        e(DEGUISE, "Passer la garde",
                "Déguisé, les gardes ne te remarquent qu'à 2 blocs.",
                "Passe entre eux en §6marchant§f : courir, grimper ou frapper fait tomber le déguisement.")
                .but(20, 0, 7).si(c -> c.x() >= 20).rate(Ctx::repere).ouvre();

        e(ZONE, "La zone interdite",
                "L'§cétendard rouge§f marque une zone interdite : barre §ejaune§f en approchant, §crouge§f dedans.",
                "Dedans, on te repère deux fois plus vite. « §e?§f » : un garde se méfie ; « §c!§f » : l'alerte.",
                "Les §6flèches autour du viseur§f montrent d'où l'on t'observe. Les herbes longent le mur sud.",
                "Rejoins le §6pupitre§f sans te faire repérer. (Repéré ? La §6fumée§f efface les soupçons.)")
                .debut(c -> {
                    c.garde("ombre_init_g1", 12, 0, 6, 0, true, false, 4);
                    c.garde("ombre_init_g1", 22, 0, 7, 180, true, false, 4);
                })
                .but(25, 0, 3).si(c -> c.p.squaredDistanceTo(c.pos(25, 0, 3).toBottomCenterPos()) < 2.6 * 2.6).rate(Ctx::repere).ouvre();

        e(NOTORIETE, "La notoriété",
                "Ta §cnotoriété§f (en haut à gauche) monte à chaque alerte. À 75 : §4RECHERCHÉ§f,",
                "les gardes te reconnaissent de loin. Te voilà justement recherché.",
                "Arrache les §6avis de recherche§f sur les murs (§6clic droit§f) jusqu'à la faire tomber à zéro.")
                .debut(c -> Notoriete.ajouter(c.p, 80 - Notoriete.get(c.p), "ton signalement circule"))
                .but(4, 1, 0).si(c -> Notoriete.get(c.p) <= 0).ouvre();

        e(VOL, "Le vol à la tire",
                "Ces passants regardent l'étal, ils te tournent le dos.",
                "Accroupi (§6Maj§f), juste derrière l'un d'eux : §6clic droit§f, et sa bourse est à toi.",
                "Vu de face, il crierait « Au voleur ! ». (Ne tue jamais un passant : désynchronisation.)")
                .debut(c -> {
                    c.p.removeCommandTag("ombre_vol");
                    for (int z = 5; z <= 9; z += 2) c.passant(9, z, -90, false, 0);
                })
                .but(8, 0, 7).si(c -> c.p.getCommandTags().contains("ombre_vol")).ouvre();

        e(GRAPPIN, "Le lance-grappin",
                "Prends le §6lance-grappin§f (roue §6G§f), vise le bord du toit, §6clic droit§f :",
                "le crochet mord, la corde te hisse d'un coup (40 blocs de portée).",
                "De là-haut, un saut de la foi dans la meule.")
                .debut(c -> c.assurer(Objets.GRAPPIN, 1))
                .but(11, 16, 7).si(c -> c.sur(10, 13, 16)).ouvre();

        e(FIN, "Initiation réussie",
                "Tu connais maintenant tous les gestes de l'Ombre.",
                "Tape §6/ombre prague§f : la Vieille-Ville, Tycho Brahe, Kepler, et la Confrérie du Corbeau t'attendent.")
                .but(7, 0, 7).si(c -> c.x() >= 4);
    }

    /** Le cours d'initiation (la Ruelle d'Or). */
    public static final Cours COURS = new Cours("initiation", "Initiation", "/ombre initiation", LEN, NOMS, ETAPES, NOTORIETE,
            "Initiation réussie", "Tu es prêt. Tape §6/ombre prague§f : la Vieille-Ville de 1600 t'attend.",
            "Je suis ton maître, novice. Suis mes leçons, cour après cour.");

    public static int nombre() { return COURS.nombre(); }
}
