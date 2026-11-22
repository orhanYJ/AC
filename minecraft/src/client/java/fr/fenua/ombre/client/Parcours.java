package fr.fenua.ombre.client;

import fr.fenua.ombre.ActionPayload;
import fr.fenua.ombre.bloc.CoteClient;
import fr.fenua.ombre.bloc.OmbreBlocs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Tout le parkour, calculé chez le joueur (dans Minecraft, c'est lui qui décide de ses mouvements).
 * 20 fois par seconde, on regarde où il est, ce qu'il presse, et on règle sa vitesse.
 *
 * Les états :
 *  LIBRE      : marche, course, saut. Le saut en sprint porte plus loin (bond).
 *  MUR        : accroché à un mur (Espace en poussant vers lui, ou en sautant contre lui), on reste en place. Z/S montent et descendent (vers le mur / à l'opposé),
 *               Q/D se déplacent sur le côté (en douceur, en passant les fenêtres aux mains), et passent les angles
 *               (saillants comme rentrants), caméra comprise. Z + Q/D + Espace : bond en diagonale ; Ctrl + Q/D + Espace :
 *               grand saut latéral ; Ctrl + S : glissade. Espace : bond vers le haut ; avec Q/D, bond sur le côté ;
 *               en tirant à l'opposé du mur, saut arrière vers le mur d'en face (on s'y raccroche seul).
 *               Maj : lâcher. En haut, on se rétablit tout seul.
 *  REBORD     : suspendu par les mains à un bord (on le saisit tout seul en tombant le long du mur). Q/D glissent, Q/D + Espace sautent vers le bord voisin,
 *               Z ou Espace se hissent, S redescend sur le mur, Maj lâche.
 *  HISSE      : le rétablissement par-dessus un bord.
 *  MUR_COURU  : course sur un mur, en sprint. Espace rebondit vers le mur d'en face.
 *  TYRO       : glisse le long d'une tyrolienne. Espace saute, Maj lâche.
 *  LIFT       : monte-charge (clic droit dessus) : on monte jusqu'au toit.
 *
 * Le CLIC DROIT maintenu sert à saisir les perches, les enseignes et les câbles de tyrolienne
 * (et à partir en tyrolienne ou en monte-charge, sur le bloc). Les murs et les rebords, eux,
 * s'attrapent sans lui.
 */
public final class Parcours {
    private Parcours() {}

    enum Etat { LIBRE, MUR, REBORD, HISSE, MUR_COURU, TYRO, LIFT, PERCHE, EXEC, GRAPPIN, SURPLOMB }

    /** Poses reprises par les animations (Animations). */
    public enum Pose { AUCUNE, MUR, REBORD, HISSE, MUR_COURU, TYRO, LIFT, PERCHE, BOND, FRANCHIR, PLONGEON, GRAPPIN, EXEC_SOL, EXEC_AERIEN, EXEC_CACHETTE, EXEC_REBORD }

    private static Etat etat = Etat.LIBRE;
    private static double murX, murZ;          // direction du mur (vers lui), vecteur unité
    private static int duree;                  // ticks passés dans l'état courant
    private static int sansPrise;              // ticks sans pouvoir s'accrocher (après un lâcher, un saut)
    private static int chercheMur;             // ticks où l'on s'accroche au premier mur touché (après un saut)
    private static int bondMur;                // ticks d'un bond vers le haut le long du mur
    private static int poseBond;               // ticks de la pose de bond
    private static int poseFranchir;           // ticks de la pose de franchissement
    private static int auBord;                 // ticks passés accroupi au bord, à pousser vers le vide
    private static double cible;               // hauteur visée (rétablissement, monte-charge)
    private static double hisseX0, hisseZ0, hisseAvance;   // rétablissement : d'où l'on part, de combien on avance
    private static boolean zTenu;
    private static double vLat, vVert, bondLat;  // escalade : vitesses lissées (on démarre et on s'arrête en douceur), élan d'un bond en diagonale              // arrivé au bord en montant : Z doit être relâché avant de passer par-dessus
    // Passage d'un angle de mur (en escalade ou suspendu) : quelques ticks où le corps contourne le coin
    private static final int VIRAGE = 6;
    private static int virage;
    private static boolean virSaillant;
    private static double virAx, virAz, virMx, virMz, virD1;
    private static float virYaw;
    private static boolean sautAvant, solAvant;
    private static boolean prise, priseAvant;    // le clic droit : la main qui attrape

    // Tyrolienne
    private static Vec3d cableA, cableB;
    private static double cableT, cableL, cableV;
    private static BlockPos cableFin;
    // Monte-charge
    private static double liftX, liftZ;
    // Perche
    private static Vec3d pivot;
    private static double balX, balZ, theta, omega;

    // Pour les animations
    static float phase;                        // avance quand on bouge (bras et jambes alternent)
    static int coteMur;                        // course sur mur : +1 mur à droite, -1 à gauche

    private static final double MAINS = 1.85;  // hauteur des mains au-dessus des pieds, bras levés

    public static Pose pose() {
        return switch (etat) {
            case MUR -> Pose.MUR;
            case REBORD -> Pose.REBORD;
            case HISSE, SURPLOMB -> Pose.HISSE;
            case MUR_COURU -> Pose.MUR_COURU;
            case TYRO -> Pose.TYRO;
            case LIFT -> Pose.LIFT;
            case PERCHE -> Pose.PERCHE;
            case EXEC -> switch (fr.fenua.ombre.Choregraphie.FAMILLE[execType]) {
                case fr.fenua.ombre.Choregraphie.F_AERIEN -> Pose.EXEC_AERIEN;
                case fr.fenua.ombre.Choregraphie.F_CACHETTE -> Pose.EXEC_CACHETTE;
                case fr.fenua.ombre.Choregraphie.F_REBORD -> Pose.EXEC_REBORD;
                default -> Pose.EXEC_SOL;
            };
            case GRAPPIN -> Pose.GRAPPIN;
            default -> poseFranchir > 0 ? Pose.FRANCHIR : poseBond > 0 ? Pose.BOND : plongeon ? Pose.PLONGEON : Pose.AUCUNE;
        };
    }

    static double balancement() { return theta; }

    /** Accroché à quelque chose (mur, rebord, perche, câble…) plutôt que libre. */
    static boolean accroche() { return etat != Etat.LIBRE; }


    /** Orientation du corps (en degrés) quand il fait face au mur, ou NaN. */
    static float corpsVersMur() {
        return switch (etat) {
            case MUR, REBORD, HISSE, LIFT -> (float) Math.toDegrees(Math.atan2(-murX, murZ));
            default -> Float.NaN;
        };
    }

    // ───────────────────────── la boucle ─────────────────────────

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null) return;
        Input in = p.input;
        boolean saut = in.jumping;
        boolean nouveauSaut = saut && !sautAvant;
        boolean sol = p.isOnGround();
        prise = client.options.useKey.isPressed() && mainLibre(p);
        try {
            if (p.getAbilities().flying || p.isSpectator() || p.isFallFlying() || p.hasVehicle() || p.isSleeping()
                    || (p.isTouchingWater() && etat != Etat.TYRO)) {
                etat = Etat.LIBRE;
                return;
            }
            if (sansPrise > 0) sansPrise--;
            if (chercheMur > 0) chercheMur--;
            if (poseBond > 0) poseBond--;
            if (poseFranchir > 0) poseFranchir--;
            duree++;
            switch (etat) {
                case LIBRE -> libre(client, p, in, nouveauSaut, sol);
                case MUR -> mur(client, p, in, nouveauSaut, sol);
                case REBORD -> rebord(p, in, nouveauSaut);
                case HISSE -> hisse(p);
                case MUR_COURU -> murCouru(p, in, nouveauSaut);
                case TYRO -> tyro(p, in, nouveauSaut);
                case LIFT -> lift(p);
                case PERCHE -> perche(p, in, nouveauSaut);
                case EXEC -> execution(client, p);
                case GRAPPIN -> grappin(p);
                case SURPLOMB -> surplomb(p, in);
            }
            plongeon = etat == Etat.LIBRE && !sol && (p.getVelocity().y < -0.75 || Cinema.plan() == Cinema.Plan.SAUT);
            float corps = corpsVersMur();
            if (!Float.isNaN(corps)) p.setBodyYaw(corps);
        } finally {
            sautAvant = saut;
            solAvant = sol;
            priseAvant = prise;
        }
    }

    private static void changer(Etat e) {
        etat = e;
        duree = 0;
    }

    // ───────────────────────── LIBRE ─────────────────────────

    private static void libre(MinecraftClient client, ClientPlayerEntity p, Input in, boolean nouveauSaut, boolean sol) {
        Vec3d v = p.getVelocity();
        double[] e = entree(p, in);
        double[] f = devant(p);

        // Le saut de la foi : une meule (ou l'eau) attend en contrebas et l'on quitte le bord, en sautant
        // (Minecraft a déjà lancé le saut quand on le voit : on y ajoute l'élan vers l'avant) ou en se laissant tomber
        if (solAvant && !sol && HudAC.sautDeLaFoi() && !Cinema.actif()) {
            if (v.y > 0.3) {
                double[] fs = devant(p);
                p.setVelocity(fs[0] * 0.34, v.y, fs[1] * 0.34);
            }
            Cinema.saut(p);
            return;
        }

        // Bond : le saut en sprint porte plus loin
        if (solAvant && !sol && v.y > 0.3 && p.isSprinting()) {
            p.setVelocity(v.x * 1.25, v.y + 0.04, v.z * 1.25);
            poseBond = 10;
            return;
        }

        // Depuis le sol : Espace en poussant contre un mur haut = on s'y accroche.
        // En sprint, c'est une course verticale : on monte le mur en courant, bien plus haut.
        if (sol && nouveauSaut) {
            double[] d = murAutour(p, norme(e) > 0.1 ? e : f);
            if (d != null && dot(e, d) > 0.5 && bloqueBuste(p, d[0] * 0.25, d[1] * 0.25, 1.0) && assezHaut(p, d[0], d[1])) {
                boolean course = p.isSprinting() || client.options.sprintKey.isPressed();
                accrocher(p, d);
                bondMur = course ? 11 : 5;
                p.setVelocity(d[0] * 0.04, course ? 0.8 : 0.42, d[1] * 0.04);
                if (course) p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.6f);
            }
            return;
        }
        if (sol) {
            // (Le bond assisté — s'élancer tout seul vers le toit d'en face — est retiré : les sauts restent à toi)
            if (franchir(p, in, f) || descendreAuBord(p, in, e)) return;
            auBord = 0;
            return;
        }

        // Clic droit maintenu : saisir une perche, une enseigne ou un câble de tyrolienne
        if (prise && sansPrise == 0) {
            if (attraperPerche(p)) return;
            if (v.y < 0.25 && attraperCable(p)) return;
        }

        if (sansPrise > 0 && chercheMur == 0) return;
        double vh = Math.sqrt(v.x * v.x + v.z * v.z);
        double[] pref = vh > 0.08 ? new double[]{v.x / vh, v.z / vh} : (norme(e) > 0.1 ? e : f);

        // En tombant le long d'un mur dont le haut passe aux mains : on s'accroche au rebord
        if (v.y < -0.05 && !in.sneaking) {
            double[] d = murAutour(p, pref);
            if (d != null && !Double.isNaN(hauteurRebord(p, p.getX() + d[0] * 0.6, p.getZ() + d[1] * 0.6, 0.9, 2.4))
                    && assezHaut(p, d[0], d[1])) {
                murX = d[0]; murZ = d[1];
                changer(Etat.REBORD);
                p.setVelocity(0, 0, 0);
                p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 0.8f);
                rattrape();
                return;
            }
        }

        // Course sur mur : en sprint, en l'air, un mur sur le côté, Z enfoncé
        if (in.movementForward > 0 && vh > 0.2 && v.y < 0.25 && chercheMur == 0) {
            for (int cote = -1; cote <= 1; cote += 2) {
                double sx = -f[1] * cote, sz = f[0] * cote;          // perpendiculaire au regard
                if (bloque(p, sx * 0.3, sz * 0.3) && bloqueBuste(p, sx * 0.6, sz * 0.6, 0.6) && assezHaut(p, sx, sz)) {
                    murX = sx; murZ = sz;
                    coteMur = -cote;
                    changer(Etat.MUR_COURU);
                    rattrape();
                    return;
                }
            }
        }

        // S'accrocher à un mur en l'air : en poussant vers lui, ou juste après un saut de mur à mur
        if (!in.sneaking) {
            double[] d = murAutour(p, pref);
            if (d != null && (chercheMur > 0 || dot(e, d) > 0.3) && bloqueBuste(p, d[0] * 0.25, d[1] * 0.25, 1.0)
                    && assezHaut(p, d[0], d[1])) {
                accrocher(p, d);
                p.setVelocity(0, Math.max(v.y, -0.1), 0);
            }
        }
    }

    // ───────────────────────── gestes de course libre (au sol) ─────────────────────────

    /** Le haut de ce qui se dresse en (x, z) entre les pieds et 2 blocs au-dessus, relatif aux pieds, ou NaN. */
    private static double hautObstacle(ClientPlayerEntity p, double x, double z) {
        World w = p.getWorld();
        double haut = Double.NaN;
        for (int y = (int) Math.floor(p.getY()); y <= (int) Math.floor(p.getY() + 2); y++) {
            BlockPos b = BlockPos.ofFloored(x, y, z);
            var forme = w.getBlockState(b).getCollisionShape(w, b);
            if (forme.isEmpty()) continue;
            double top = y + forme.getMax(Direction.Axis.Y) - p.getY();
            if (top > 0.05) haut = Double.isNaN(haut) ? top : Math.max(haut, top);
        }
        return haut;
    }

    /** La boîte du joueur, déplacée de (dx, dy, dz), est-elle libre ? */
    private static boolean libreEn(ClientPlayerEntity p, double dx, double dy, double dz) {
        return p.getWorld().isSpaceEmpty(p, p.getBoundingBox().offset(dx, dy, dz));
    }

    /**
     * Franchissement : en sprint, un muret, une barrière, un étal ou une charrette (de 0,6 à 1,6 de haut)
     * se passe d'un appui de main, s'il y a la place de retomber de l'autre côté.
     */
    private static boolean franchir(ClientPlayerEntity p, Input in, double[] f) {
        if (!p.isSprinting() || in.movementForward <= 0 || in.sneaking) return false;
        double h = hautObstacle(p, p.getX() + f[0] * 0.7, p.getZ() + f[1] * 0.7);
        if (Double.isNaN(h) || h < 0.6 || h > 1.6) return false;
        if (!libreEn(p, f[0] * 0.7, h + 0.05, f[1] * 0.7)) return false;        // de la place par-dessus
        boolean retombe = false;
        for (double d = 1.4; d <= 2.6; d += 0.4)
            if (libreEn(p, f[0] * d, 0, f[1] * d)) { retombe = true; break; }   // un obstacle mince, pas une marche
        if (!retombe) return false;
        p.setVelocity(f[0] * 0.36, 0.32 + h * 0.22, f[1] * 0.36);
        poseFranchir = 9;
        sansPrise = 10;
        p.playSound(SoundEvents.BLOCK_WOOD_STEP, 0.6f, 1.3f);
        rattrape();
        return true;
    }

    /**
     * Bond assisté : en sprint au bord d'un toit, si un appui attend en face (de 2 à 5 blocs, pas plus d'un bloc
     * au-dessus ni plus de 4 en dessous), on s'élance tout seul, juste assez fort pour l'atteindre.
     */
    private static boolean bondAssiste(ClientPlayerEntity p, Input in, double[] f) {
        if (!p.isSprinting() || in.movementForward <= 0 || in.sneaking) return false;
        World w = p.getWorld();
        // Le bord : plus rien sous les pieds juste devant, sur au moins 3 blocs
        double ax = p.getX() + f[0] * 0.75, az = p.getZ() + f[1] * 0.75;
        for (int k = 1; k <= 3; k++) {
            BlockPos b = BlockPos.ofFloored(ax, p.getY() - k + 0.5, az);
            if (!w.getBlockState(b).getCollisionShape(w, b).isEmpty()) return false;
        }
        if (!libreEn(p, f[0] * 0.75, 0, f[1] * 0.75)) return false;
        for (double d = 2.0; d <= 5.0; d += 0.5) {
            double x = p.getX() + f[0] * d, z = p.getZ() + f[1] * d;
            for (int dy = 1; dy >= -4; dy--) {
                BlockPos b = BlockPos.ofFloored(x, p.getY() + dy - 0.5, z);
                var forme = w.getBlockState(b).getCollisionShape(w, b);
                if (forme.isEmpty()) continue;
                double top = b.getY() + forme.getMax(Direction.Axis.Y);
                if (top - p.getY() > 1.05 || top - p.getY() < -4.2) break;
                if (!libreEn(p, f[0] * d, top - p.getY() + 0.01, f[1] * d)) break;
                double vh = MathHelper.clamp(d / 7.2 + 0.08, 0.38, 0.78);
                p.setVelocity(f[0] * vh, top - p.getY() > 0.3 ? 0.5 : 0.42, f[1] * vh);
                poseBond = 12;
                p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.1f);
                rattrape();
                return true;
            }
        }
        return false;
    }

    /**
     * Descente au rebord : accroupi au bord d'un toit, on pousse vers le vide un instant et l'on se laisse glisser
     * jusqu'à se suspendre par les mains au bord (de là : assassinat depuis le rebord, ou on lâche plus bas).
     */
    private static boolean descendreAuBord(ClientPlayerEntity p, Input in, double[] e) {
        if (!in.sneaking || norme(e) < 0.5) { auBord = 0; return false; }
        World w = p.getWorld();
        // La direction du vide : la plus proche de l'entrée, parmi les quatre
        Direction vers = Direction.getFacing(e[0], 0, e[1]);
        double dx = vers.getOffsetX(), dz = vers.getOffsetZ();
        // Le bord : la case devant est vide sur 2 blocs de profondeur, et l'on est tout contre elle
        double bordX = dx != 0 ? Math.floor(p.getX()) + (dx > 0 ? 1 : 0) : p.getX();
        double bordZ = dz != 0 ? Math.floor(p.getZ()) + (dz > 0 ? 1 : 0) : p.getZ();
        double ecart = dx != 0 ? Math.abs(bordX - p.getX()) : Math.abs(bordZ - p.getZ());
        if (ecart > 0.45) { auBord = 0; return false; }
        for (int k = 1; k <= 2; k++) {
            BlockPos b = BlockPos.ofFloored(bordX + dx * 0.5, p.getY() - k + 0.5, bordZ + dz * 0.5);
            if (!w.getBlockState(b).getCollisionShape(w, b).isEmpty()) { auBord = 0; return false; }
        }
        if (++auBord < 5) return false;
        // On passe par-dessus le bord, les mains sur l'arête, face au mur
        double nx = dx != 0 ? bordX + dx * 0.31 : p.getX(), nz = dz != 0 ? bordZ + dz * 0.31 : p.getZ();
        double ny = Math.floor(p.getY() + 0.01) - MAINS + 0.05;
        if (!w.isSpaceEmpty(p, p.getBoundingBox().offset(nx - p.getX(), ny - p.getY(), nz - p.getZ()))) { auBord = 0; return false; }
        p.setPosition(nx, ny, nz);
        p.setVelocity(0, 0, 0);
        p.setYaw(capDe(-dx, -dz));
        murX = -dx; murZ = -dz;
        changer(Etat.REBORD);
        zTenu = true;
        auBord = 0;
        p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 0.9f);
        rattrape();
        return true;
    }

    private static void accrocher(ClientPlayerEntity p, double[] d) {
        murX = d[0]; murZ = d[1];
        vLat = 0; vVert = 0; bondLat = 0;
        changer(Etat.MUR);
        bondMur = 0;
        p.playSound(SoundEvents.BLOCK_STONE_HIT, 0.5f, 1.2f);
        rattrape();
    }

    // ───────────────────────── MUR (escalade libre) ─────────────────────────

    private static void mur(MinecraftClient client, ClientPlayerEntity p, Input in, boolean nouveauSaut, boolean sol) {
        double[] e = entree(p, in);
        double tx = -murZ, tz = murX;                            // le long du mur
        double vers = dot(e, murX, murZ), cote = e[0] * tx + e[1] * tz;
        Vec3d v = p.getVelocity();

        if (in.sneaking) { virage = 0; lacher(10); return; }
        if (virage > 0) { virer(p); return; }

        // Le haut du mur est à portée (en montant, ou à la fin d'un bond) :
        // en sprint, ou sur un muret, on passe par-dessus d'un trait ; sinon on s'arrête suspendu au bord
        if (vers > 0.3 || bondMur > 0) {
            double h = hauteurRebord(p, p.getX() + murX * 0.6, p.getZ() + murZ * 0.6, -0.3, 1.9);
            if (!Double.isNaN(h)) {
                if (client.options.sprintKey.isPressed() || h - p.getY() < 1.4) { hisser(p, murX, murZ, h); return; }
                arriverAuBord(p, h);
                return;
            }
        }

        // La tête bute sous une saillie (auvent, balcon, corniche, enseigne de pierre) : comme dans Assassin's Creed,
        // c'est une prise. On saisit son bord, on passe dessus d'un rétablissement, et l'on continue à grimper.
        if ((vers > 0.3 || bondMur > 0) && !libreEn(p, 0, 0.12, 0) && passerSurplomb(p)) return;

        // Plus de mur devant ? (une fenêtre en retrait, ou le montant d'une ouverture à portée de main, suffisent)
        if (!appui(p, 0, 0) && !appui(p, tx * 0.9, tz * 0.9) && !appui(p, -tx * 0.9, -tz * 0.9)) {
            changer(Etat.LIBRE);
            return;
        }

        boolean vite = client.options.sprintKey.isPressed();
        if (nouveauSaut) {
            if (vers > 0.3 && Math.abs(cote) > 0.3) {            // bond en diagonale : vers le haut et sur le côté
                bondMur = 9;
                bondLat = Math.signum(cote) * 0.3;
                p.setVelocity(murX * 0.05 + tx * bondLat, 0.58, murZ * 0.05 + tz * bondLat);
                p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.2f);
            } else if (Math.abs(cote) > 0.5) {                   // bond sur le côté (grand saut en sprint)
                double s = Math.signum(cote), f = vite ? 0.62 : 0.45;
                p.setVelocity(tx * s * f + murX * 0.08, vite ? 0.44 : 0.4, tz * s * f + murZ * 0.08);
                sauter(p, 3, vite ? 18 : 14);
            } else if (vers < -0.5) {                            // saut arrière, vers le mur d'en face
                p.setVelocity(-murX * 0.55, 0.45, -murZ * 0.55);
                sauter(p, 5, 18);
            } else {                                             // bond vers le haut
                bondMur = 9;
                bondLat = 0;
                p.setVelocity(murX * 0.04, 0.62, murZ * 0.04);
                p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.3f);
            }
            return;
        }

        if (bondMur > 0) {                                       // en l'air le long du mur : la gravité agit
            bondMur--;
            if (bondLat != 0 && !appui(p, tx * Math.signum(bondLat) * 0.45, tz * Math.signum(bondLat) * 0.45)) bondLat = 0;
            p.setVelocity(murX * 0.04 + tx * bondLat, v.y, murZ * 0.04 + tz * bondLat);
            if (bondMur == 0) { vLat = bondLat; vVert = 0; bondLat = 0; }
            return;
        }

        // Ce que veulent les touches : monter, descendre (Ctrl + S : glissade), aller sur le côté
        double vy = vers > 0.3 ? (vite ? 0.2 : 0.13) : vers < -0.3 ? (vite ? -0.42 : -0.17) : 0;
        double grimpeur = ProfilClient.a("grimpeur") ? 1.3 : 1.0;          // compétence « Grimpeur »
        if (vy > 0) vy *= grimpeur;
        if (vy > 0 && gouttiere(p)) vy *= 1.8;                   // le long d'une gouttière, on monte vite
        if (vy < 0 && sol) { changer(Etat.LIBRE); return; }
        double lat = 0;
        if (Math.abs(cote) > 0.3) {
            double s = Math.signum(cote);
            // Un mur en travers (coin rentrant), ou le bout du mur (coin saillant) : on passe l'angle.
            // Une fenêtre ou un renfoncement d'un ou deux blocs : on le passe aux mains.
            if (murEnTravers(p, tx * s, tz * s)) { if (contourner(p, tx * s, tz * s, false)) return; }
            else if (bloqueBuste(p, tx * s * 0.15, tz * s * 0.15, 0.0) && passerSurplombCote(p, tx * s, tz * s)) return;
            else if (appui(p, tx * s * 0.45, tz * s * 0.45) || appui(p, tx * s * 1.2, tz * s * 1.2) || appui(p, tx * s * 1.9, tz * s * 1.9))
                lat = s * (vite ? 0.17 : 0.11) * grimpeur;
            else if (contourner(p, tx * s, tz * s, false)) return;
        }
        // On y va en douceur : la vitesse rejoint peu à peu celle voulue
        vLat += (lat - vLat) * 0.35;
        vVert += (vy - vVert) * (vy < vVert ? 0.5 : 0.4);
        if (Math.abs(vLat) < 0.005) vLat = 0;
        if (Math.abs(vVert) < 0.005) vVert = 0;
        // On reste plaqué contre la façade, mais sans s'enfoncer dans une fenêtre ; si l'on y est entré,
        // on s'en dégage pour pouvoir longer le mur
        boolean faceDevant = bloqueBuste(p, murX * 0.3, murZ * 0.3, 0.3) || bloque(p, murX * 0.3, murZ * 0.3);
        double colle = faceDevant ? 0.05 : 0;
        if (vLat != 0 && bloqueBuste(p, tx * Math.signum(vLat) * 0.12, tz * Math.signum(vLat) * 0.12, 0.3)) {
            double sl = Math.signum(vLat);
            // Une saillie sur la façade (un balcon, un auvent) : on s'arrête contre elle — on passera dessus en montant.
            // Le côté d'une embrasure de fenêtre, où l'on s'est enfoncé : on s'en dégage pour longer le mur.
            if (bloqueBuste(p, tx * sl * 0.12 - murX * 0.4, tz * sl * 0.12 - murZ * 0.4, 0.3)) vLat = 0;
            else colle = -0.12;
        }
        p.setVelocity(murX * colle + tx * vLat, vVert, murZ * colle + tz * vLat);
        if (vVert != 0 || vLat != 0) {
            phase += (float) (0.25 + 1.5 * Math.max(Math.abs(vLat), Math.abs(vVert)));
            if (duree % 6 == 0) p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.25f, 1.3f);
            if (vVert < -0.3 && duree % 2 == 0) {                // la glissade soulève la poussière du mur
                BlockPos b = BlockPos.ofFloored(p.getX() + murX * 0.7, p.getY() + 0.5, p.getZ() + murZ * 0.7);
                p.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, p.getWorld().getBlockState(b)),
                        p.getX() + murX * 0.35, p.getY() + 0.2, p.getZ() + murZ * 0.35, 0, 0.05, 0);
                if (duree % 4 == 0) p.playSound(SoundEvents.BLOCK_GRAVEL_STEP, 0.3f, 1.4f);
            }
        }
        if (duree % 10 == 0) rattrape();
    }

    // ───────────────────────── REBORD ─────────────────────────

    private static void rebord(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        double[] e = entree(p, in);
        double tx = -murZ, tz = murX;
        double vers = dot(e, murX, murZ), cote = e[0] * tx + e[1] * tz;
        if (vers <= 0.3) zTenu = false;

        if (in.sneaking) { virage = 0; lacher(12); return; }
        if (virage > 0) { virer(p); return; }
        if (nouveauSaut) {
            if (Math.abs(cote) > 0.5) {                          // saut vers le rebord voisin
                double s = Math.signum(cote);
                p.setVelocity(tx * s * 0.45 + murX * 0.06, 0.34, tz * s * 0.45 + murZ * 0.06);
                sauter(p, 3, 0);
            } else if (vers < -0.5) {                            // saut arrière
                p.setVelocity(-murX * 0.55, 0.45, -murZ * 0.55);
                sauter(p, 5, 18);
            } else {
                monterDuRebord(p);
            }
            return;
        }
        if (vers > 0.5 && duree > 6 && !zTenu) { monterDuRebord(p); return; }
        if (vers < -0.5 && duree > 6 && bloqueBuste(p, murX * 0.3, murZ * 0.3, 0.3)) {   // redescendre sur le mur
            changer(Etat.MUR);
            return;
        }
        // Glisser aux mains le long du bord
        double gx = 0, gz = 0;
        if (Math.abs(cote) > 0.3) {
            double s = Math.signum(cote);
            gx = tx * s * 0.12; gz = tz * s * 0.12;
            boolean enTravers = murEnTravers(p, tx * s, tz * s);
            if (enTravers || Double.isNaN(hauteurRebord(p, p.getX() + gx * 4 + murX * 0.6, p.getZ() + gz * 4 + murZ * 0.6, 1.0, 2.2))) {
                gx = 0; gz = 0;
                if (contourner(p, tx * s, tz * s, true)) return;   // l'angle : le bord continue de l'autre côté
            }
            else phase += 0.3f;
        }
        p.setVelocity(gx + murX * 0.02, 0, gz + murZ * 0.02);
        if (!bloque(p, murX * 0.3, murZ * 0.3)) changer(Etat.LIBRE);
        if (duree % 10 == 0) rattrape();
    }

    /** En montant le long du mur, les mains arrivent au bord : on s'y arrête, suspendu (de là, assassinat depuis le rebord). */
    private static void arriverAuBord(ClientPlayerEntity p, double haut) {
        changer(Etat.REBORD);
        zTenu = true;
        bondMur = 0;
        double y = haut - MAINS;
        if (p.getWorld().isSpaceEmpty(p, p.getBoundingBox().offset(0, y - p.getY(), 0))) p.setPosition(p.getX(), y, p.getZ());
        p.setVelocity(0, 0, 0);
        p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 0.9f);
        p.sendMessage(Text.literal("§7Au bord — §fZ§7 (à nouveau) ou §fEspace§7 : passer par-dessus · §fS§7 : redescendre"), true);
        rattrape();
    }

    private static void monterDuRebord(ClientPlayerEntity p) {
        double h = hauteurRebord(p, p.getX() + murX * 0.6, p.getZ() + murZ * 0.6, 0.5, 2.3);
        if (!Double.isNaN(h)) hisser(p, murX, murZ, h);
    }

    // ───────────────────────── SURPLOMB (passer sur une saillie) ─────────────────────────

    private static Vec3d spDepart, spDehors, spHaut, spArrivee;
    private static final int SP_DUREE = 11;

    /**
     * La saillie au-dessus de la tête : son dessus (au plus 1,4 au-dessus de la tête), son bord côté rue
     * (à moins de 1,3 du joueur), la place pour passer devant puis se tenir dessus. Si tout va, le geste commence.
     */
    private static boolean passerSurplomb(ClientPlayerEntity p) {
        World w = p.getWorld();
        double tete = p.getY() + p.getHeight();
        double dessus = Double.NaN;
        for (int y = (int) Math.floor(tete); y <= (int) Math.floor(tete + 1.4); y++) {
            double d = dessusEn(w, BlockPos.ofFloored(p.getX(), y, p.getZ()), p.getX(), p.getZ());
            if (Double.isNaN(d)) { if (!Double.isNaN(dessus)) break; continue; }
            dessus = d;
        }
        if (Double.isNaN(dessus) || dessus - tete > 1.4) return false;
        // Le bord de la saillie, en s'éloignant du mur
        double ox = -murX, oz = -murZ, bord = Double.NaN;
        for (double t = 0; t <= 1.3; t += 0.05) {
            double x = p.getX() + ox * t, z = p.getZ() + oz * t;
            if (w.isSpaceEmpty(new Box(x - 0.02, dessus - 0.08, z - 0.02, x + 0.02, dessus - 0.02, z + 0.02))) { bord = t; break; }
        }
        if (Double.isNaN(bord)) return false;                   // une saillie trop large : un plafond
        double sortie = bord + 0.34;
        double dy = dessus + 0.01 - p.getY();
        if (!libreEn(p, ox * sortie, 0, oz * sortie) || !libreEn(p, ox * sortie, dy, oz * sortie)) return false;
        if (!libreEn(p, 0, dy, 0)) return false;                 // pas la place de se tenir dessus
        spDepart = p.getPos();
        spDehors = spDepart.add(ox * sortie, 0, oz * sortie);
        spHaut = spDehors.add(0, dy, 0);
        spArrivee = spDepart.add(0, dy, 0);
        changer(Etat.SURPLOMB);
        p.setVelocity(0, 0, 0);
        p.playSound(SoundEvents.BLOCK_WOOD_STEP, 0.6f, 1.1f);
        return true;
    }

    /**
     * En longeant le mur, on arrive contre une saillie (balcon, auvent) dont le dessus est à portée :
     * on se hisse dessus, de côté. (Sinon Minecraft nous poussait hors de la saillie, devant elle, dans le vide.)
     */
    private static boolean passerSurplombCote(ClientPlayerEntity p, double ax, double az) {
        World w = p.getWorld();
        double x = p.getX() + ax * 0.55, z = p.getZ() + az * 0.55;
        double dessus = Double.NaN;
        for (int y = (int) Math.floor(p.getY()); y <= (int) Math.floor(p.getY() + p.getHeight() + 1.0); y++) {
            double d = dessusEn(w, BlockPos.ofFloored(x, y, z), x, z);
            if (Double.isNaN(d)) { if (!Double.isNaN(dessus)) break; continue; }
            dessus = d;
        }
        double dy = dessus + 0.01 - p.getY();
        if (Double.isNaN(dessus) || dessus - p.getY() > p.getHeight() + 0.9 || dessus - p.getY() < 0.3) return false;
        if (!libreEn(p, 0, dy, 0) || !libreEn(p, ax * 0.8, dy, az * 0.8)) return false;
        spDepart = p.getPos();
        spDehors = spDepart;
        spHaut = spDepart.add(0, dy, 0);
        spArrivee = spHaut.add(ax * 0.8, 0, az * 0.8);
        changer(Etat.SURPLOMB);
        p.setVelocity(0, 0, 0);
        p.playSound(SoundEvents.BLOCK_WOOD_STEP, 0.6f, 1.1f);
        return true;
    }

    /** Le dessus de ce qui est plein dans le bloc b, à l'aplomb du point (x, z) (le plancher d'un balcon, pas sa rambarde) ; NaN s'il n'y a rien. */
    private static double dessusEn(World w, BlockPos b, double x, double z) {
        var forme = w.getBlockState(b).getCollisionShape(w, b);
        if (forme.isEmpty()) return Double.NaN;
        double lx = x - b.getX(), lz = z - b.getZ(), haut = Double.NaN;
        for (Box c : forme.getBoundingBoxes())
            if (lx >= c.minX - 0.3 && lx <= c.maxX + 0.3 && lz >= c.minZ - 0.3 && lz <= c.maxZ + 0.3)
                haut = Double.isNaN(haut) ? c.maxY : Math.max(haut, c.maxY);
        return Double.isNaN(haut) ? Double.NaN : b.getY() + haut;
    }

    /** Le geste : les mains au bord, le corps se décale vers la rue, se hisse, et repasse au-dessus de la saillie. */
    private static void surplomb(ClientPlayerEntity p, Input in) {
        double k = Math.min(1, duree / (double) SP_DUREE);
        Vec3d ici;
        if (k < 0.3) ici = lerp(spDepart, spDehors, k / 0.3);
        else if (k < 0.75) ici = lerp(spDehors, spHaut, (k - 0.3) / 0.45);
        else ici = lerp(spHaut, spArrivee, (k - 0.75) / 0.25);
        p.setPosition(ici);
        p.setVelocity(0, 0, 0);
        p.fallDistance = 0;
        phase += 0.4f;
        if (duree >= SP_DUREE) {
            p.setPosition(spArrivee);
            rattrape();
            // Z toujours enfoncé : on reprend l'escalade dans la foulée ; sinon, on reste debout sur la saillie
            if (in.movementForward > 0 && bloqueBuste(p, murX * 0.3, murZ * 0.3, 0.3)) {
                vLat = 0; vVert = 0; bondMur = 0;
                changer(Etat.MUR);
            } else changer(Etat.LIBRE);
        }
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, double u) {
        double s = u * u * (3 - 2 * u);
        return a.add(b.subtract(a).multiply(s));
    }

    // ───────────────────────── HISSE (rétablissement) ─────────────────────────

    private static void hisser(ClientPlayerEntity p, double fx, double fz, double hauteur) {
        changer(Etat.HISSE);
        murX = fx; murZ = fz; cible = hauteur;
        virage = 0;
        hisseX0 = p.getX(); hisseZ0 = p.getZ();
        hisseAvance = avancePourSeRetablir(p, fx, fz, hauteur);
        p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 1.0f);
        sansPrise = 10;
        hisse(p);
        rattrape();
    }

    private static void hisse(ClientPlayerEntity p) {
        if (duree > 30) { changer(Etat.LIBRE); return; }
        if (p.getY() < cible + 0.02) {
            p.setVelocity(murX * 0.03, Math.min(0.42, cible + 0.08 - p.getY()) + 0.05, murZ * 0.03);
            return;
        }
        // On bascule par-dessus le bord, juste ce qu'il faut : au milieu d'une poutre, un peu en retrait sur un toit
        double fait = (p.getX() - hisseX0) * murX + (p.getZ() - hisseZ0) * murZ;
        double reste = hisseAvance - fait;
        if (reste <= 0.03 || (duree > 4 && bloque(p, murX * 0.1, murZ * 0.1))) {
            p.setVelocity(0, 0, 0);
            changer(Etat.LIBRE);
            return;
        }
        double v = Math.min(0.24, reste);
        p.setVelocity(murX * v, 0, murZ * v);
    }

    /**
     * De combien avancer, une fois les pieds au niveau du bord, pour se tenir bien dessus :
     * on mesure la largeur de ce qu'il y a sous les pieds dans la direction du mur
     * (une poutre ne fait que 6 pixels) et on vise son milieu, sans aller plus de 0,75 bloc au-delà du bord.
     */
    private static double avancePourSeRetablir(ClientPlayerEntity p, double fx, double fz, double hauteur) {
        World w = p.getWorld();
        double debut = Double.NaN, fin = Double.NaN;
        for (double t = 0; t <= 2.0; t += 0.05) {
            double x = p.getX() + fx * t, z = p.getZ() + fz * t;
            boolean plein = !w.isSpaceEmpty(new Box(x - 0.01, hauteur - 0.06, z - 0.01, x + 0.01, hauteur - 0.01, z + 0.01));
            if (plein && Double.isNaN(debut)) debut = t;
            if (!plein && !Double.isNaN(debut)) { fin = t; break; }
        }
        if (Double.isNaN(debut)) return 0.9;                     // rien de mesurable : la poussée d'autrefois
        if (Double.isNaN(fin)) fin = 2.0;
        return debut + Math.min((fin - debut) / 2, 0.75);
    }

    // ───────────────────────── les angles ─────────────────────────

    /**
     * Passer un angle en se déplaçant dans la direction (ax, az), le long du mur.
     * Coin rentrant : un mur nous barre la route ; il devient le mur auquel on s'accroche.
     * Coin saillant : le mur s'arrête ; on fait le tour du coin et on s'accroche à sa face de côté.
     * Suspendu à un rebord (rebord = vrai), il faut aussi que le bord continue de l'autre côté.
     */
    private static boolean contourner(ClientPlayerEntity p, double ax, double az, boolean rebord) {
        World w = p.getWorld();
        if (murEnTravers(p, ax, az)) {
            if (rebord && Double.isNaN(hauteurRebord(p, p.getX() + ax * 0.6, p.getZ() + az * 0.6, 1.0, 2.2))) return false;
            commencerVirage(p, ax, az, false);
            return true;
        }
        // Où le mur s'arrête-t-il ? On dépasse son arête d'un demi-corps, puis on avance le long de sa face de côté
        double bout = Double.NaN;
        for (double t = 0; t <= 1.0; t += 0.05)
            if (!bloqueBuste(p, murX * 0.3 + ax * t, murZ * 0.3 + az * t, 0.3)) { bout = t; break; }
        if (Double.isNaN(bout)) return false;
        double d1 = bout + 0.03;                                 // « bout » met déjà le corps juste au-delà de l'arête
        double ox = ax * d1 + murX * 0.65, oz = az * d1 + murZ * 0.65;
        Box arrivee = p.getBoundingBox().offset(ox, 0, oz);
        if (!w.isSpaceEmpty(p, p.getBoundingBox().offset(ax * d1, 0, az * d1)) || !w.isSpaceEmpty(p, arrivee)) return false;
        Box buste = new Box(arrivee.minX, arrivee.minY + 0.3, arrivee.minZ, arrivee.maxX, arrivee.maxY - 0.1, arrivee.maxZ).offset(-ax * 0.35, 0, -az * 0.35);
        if (w.isSpaceEmpty(p, buste)) return false;              // pas de face de côté (un mur trop mince)
        if (rebord && Double.isNaN(hauteurRebord(p, p.getX() + ox - ax * 0.6, p.getZ() + oz - az * 0.6, 1.0, 2.2))) return false;
        commencerVirage(p, -ax, -az, true);
        virAx = ax; virAz = az; virD1 = d1;
        return true;
    }

    /**
     * Une prise pour les mains, décalée de (dx, dz) le long du mur : le mur lui-même, ou ce qui est en retrait
     * dans son épaisseur (la vitre d'une fenêtre, un volet, le fond d'une niche).
     */
    private static boolean appui(ClientPlayerEntity p, double dx, double dz) {
        return bloqueBuste(p, murX * 0.3 + dx, murZ * 0.3 + dz, 0.3) || bloque(p, murX * 0.3 + dx, murZ * 0.3 + dz)
                || bloqueBuste(p, murX * 0.75 + dx, murZ * 0.75 + dz, 0.3);
    }

    /** Un mur barre-t-il la route dans la direction (ax, az) ? Un vrai mur : il dépasse d'au moins un demi-bloc (pas une gouttière). */
    private static boolean murEnTravers(ClientPlayerEntity p, double ax, double az) {
        if (!(bloqueBuste(p, ax * 0.2, az * 0.2, 0.3) && bloqueBuste(p, ax * 0.2 - murX * 0.5, az * 0.2 - murZ * 0.5, 0.3))) return false;
        // Un vrai mur est plein des pieds jusqu'au-dessus de la tête ; un balcon ou un auvent n'occupe qu'une tranche :
        // ce n'est pas un angle (sinon on pivotait, et l'on « grimpait » le flanc du balcon vers la rue)
        Box b = p.getBoundingBox().offset(ax * 0.2 - murX * 0.5, 0, az * 0.2 - murZ * 0.5);
        Box bas = new Box(b.minX, b.minY + 0.05, b.minZ, b.maxX, b.minY + 0.6, b.maxZ);
        Box haut = new Box(b.minX, b.maxY + 0.8, b.minZ, b.maxX, b.maxY + 1.4, b.maxZ);
        return !p.getWorld().isSpaceEmpty(p, bas) && !p.getWorld().isSpaceEmpty(p, haut);
    }

    private static float capDe(double x, double z) { return (float) Math.toDegrees(Math.atan2(-x, z)); }

    private static void commencerVirage(ClientPlayerEntity p, double nx, double nz, boolean saillant) {
        virMx = murX; virMz = murZ;
        virSaillant = saillant;
        virYaw = MathHelper.wrapDegrees(capDe(nx, nz) - capDe(murX, murZ)) / VIRAGE;
        murX = nx; murZ = nz;
        virage = VIRAGE;
        p.setVelocity(0, 0, 0);                                  // sinon la pesanteur du tick d'avant nous fait glisser
        p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.4f, 1.1f);
        rattrape();
    }

    /** Un tick du passage d'angle : la caméra pivote avec le corps ; au coin saillant, on longe puis on contourne. */
    private static void virer(ClientPlayerEntity p) {
        virage--;
        p.setYaw(p.getYaw() + virYaw);
        double v1 = virD1 / (VIRAGE / 2.0), v2 = 0.65 / (VIRAGE / 2.0);
        if (!virSaillant) p.setVelocity(murX * 0.03, 0, murZ * 0.03);
        else if (virage >= VIRAGE / 2) p.setVelocity(virAx * v1, 0, virAz * v1);
        else p.setVelocity(virMx * v2, 0, virMz * v2);
        phase += 0.3f;
    }

    // ───────────────────────── MUR_COURU ─────────────────────────

    private static void murCouru(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        Vec3d v = p.getVelocity();
        if (nouveauSaut) {                                       // rebond vers le mur d'en face
            double[] f = devant(p);
            p.setVelocity(-murX * 0.5 + f[0] * 0.3, 0.55, -murZ * 0.5 + f[1] * 0.3);
            sauter(p, 4, 14);
            return;
        }
        boolean finie = duree > 32 || in.movementForward <= 0 || !bloque(p, murX * 0.3, murZ * 0.3) || p.isOnGround();
        if (finie) { changer(Etat.LIBRE); sansPrise = 6; return; }

        double[] f = devant(p);
        double ax = f[0] - murX * (f[0] * murX + f[1] * murZ), az = f[1] - murZ * (f[0] * murX + f[1] * murZ);
        double n = Math.sqrt(ax * ax + az * az);
        if (n < 0.2) { changer(Etat.LIBRE); return; }            // on regarde le mur : ce n'est plus une course
        double vit = Math.max(Math.sqrt(v.x * v.x + v.z * v.z), 0.3);
        double vy = duree < 6 ? Math.max(v.y, 0.06) : -0.02 - duree * 0.003;
        p.setVelocity(ax / n * vit + murX * 0.04, vy, az / n * vit + murZ * 0.04);
        phase += 0.6f;
        if (duree % 4 == 0) {
            BlockPos b = BlockPos.ofFloored(p.getX() + murX * 0.7, p.getY() + 0.5, p.getZ() + murZ * 0.7);
            p.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, p.getWorld().getBlockState(b)),
                    p.getX() + murX * 0.35, p.getY() + 0.3, p.getZ() + murZ * 0.35, 0, 0, 0);
            p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.3f, 1.2f);
        }
        if (duree % 10 == 0) rattrape();
    }

    // ───────────────────────── TYROLIENNE ─────────────────────────

    public static void demarrerTyrolienne(BlockPos de, BlockPos vers) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || etat != Etat.LIBRE && etat != Etat.MUR) return;
        Vec3d a = Vec3d.ofBottomCenter(de).add(0, CoteClient.ACCROCHE, 0);
        Vec3d b = Vec3d.ofBottomCenter(vers).add(0, CoteClient.ACCROCHE, 0);
        double l = a.distanceTo(b);
        if (l < 2) return;
        // On s'accroche au premier point du câble où le corps tient (pas dans le toit)
        for (double t = 0.02; t < 0.4; t += 0.01) {
            Vec3d pieds = CoteClient.pointCable(a, b, t).subtract(0, MAINS, 0);
            Box boite = p.getBoundingBox().offset(pieds.subtract(p.getPos()));
            if (p.getWorld().isSpaceEmpty(p, boite)) {
                lancerCable(p, a, b, vers, t);
                return;
            }
        }
        p.sendMessage(Text.literal("§7Pas la place de s'accrocher au câble ici."), true);
    }

    private static void lancerCable(ClientPlayerEntity p, Vec3d a, Vec3d b, BlockPos vers, double t) {
        cableA = a; cableB = b; cableL = a.distanceTo(b); cableT = t; cableV = 0.25; cableFin = vers;
        p.setVelocity(CoteClient.pointCable(a, b, t).subtract(0, MAINS, 0).subtract(p.getPos()));
        changer(Etat.TYRO);
        p.playSound(SoundEvents.ENTITY_LEASH_KNOT_PLACE, 0.8f, 0.7f);
        rattrape();
    }

    private static void tyro(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        Vec3d dir = cableB.subtract(cableA).normalize();
        if (in.sneaking) { lacher(10); return; }
        if (nouveauSaut) {
            p.setVelocity(dir.x * cableV, 0.4, dir.z * cableV);
            sauter(p, 8, 0);
            return;
        }
        cableV = Math.min(0.95, cableV + 0.03 + Math.max(0, -dir.y) * 0.04);
        cableT += cableV / cableL;
        Vec3d pieds = CoteClient.pointCable(cableA, cableB, cableT).subtract(0, MAINS, 0);
        Box boite = p.getBoundingBox().offset(pieds.subtract(p.getPos()));
        if (cableT >= 1 - 0.6 / cableL || !p.getWorld().isSpaceEmpty(p, boite)) {
            // Arrivée : on se hisse sur le toit du poteau d'arrivée, ou on se laisse tomber devant
            double hx = dir.x, hz = dir.z, hn = Math.sqrt(hx * hx + hz * hz);
            if (cableFin != null && cableFin.getY() > p.getY() - 0.5 && hn > 0.01) {
                hisser(p, hx / hn, hz / hn, cableFin.getY());
            } else {
                p.setVelocity(dir.x * cableV * 0.6, 0.1, dir.z * cableV * 0.6);
                lacher(8);
            }
            return;
        }
        p.setVelocity(pieds.subtract(p.getPos()));
        phase += 0.1f;
        if (duree % 5 == 0) p.playSound(SoundEvents.BLOCK_CHAIN_STEP, 0.25f, 0.6f + (float) cableV);
        if (duree % 10 == 0) rattrape();
    }

    // ───────────────────────── MONTE-CHARGE ─────────────────────────

    public static void demarrerMonteCharge(BlockPos pos, Direction mur, int sommet) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || etat == Etat.LIFT) return;
        liftX = pos.getX() + 0.5; liftZ = pos.getZ() + 0.5;
        murX = mur.getOffsetX(); murZ = mur.getOffsetZ();
        cible = sommet;
        changer(Etat.LIFT);
        rattrape();
    }

    private static void lift(ClientPlayerEntity p) {
        if (p.getY() >= cible - 1.0) { hisser(p, murX, murZ, cible); return; }
        if (duree > 8 && p.verticalCollision && p.getVelocity().y <= 0.01) { lacher(5); return; }  // bloqué en chemin
        // Le contrepoids tombe : on démarre doucement, puis on file vers le haut, en se recentrant sur la corde
        double vy = duree < 4 ? 0.05 : Math.min(0.6, 0.1 + (duree - 4) * 0.035);
        p.setVelocity((liftX - p.getX()) * 0.4, vy, (liftZ - p.getZ()) * 0.4);
        if (duree % 3 == 0) p.playSound(SoundEvents.BLOCK_CHAIN_STEP, 0.35f, 0.7f + (float) vy);
        if (duree % 10 == 0) rattrape();
        if (duree > 120) changer(Etat.LIBRE);
    }

    // ───────────────────────── PERCHE ─────────────────────────

    private static boolean attraperPerche(ClientPlayerEntity p) {
        World w = p.getWorld();
        double mains = p.getY() + MAINS;
        for (int by = (int) Math.floor(mains - 0.9); by <= (int) Math.floor(mains + 0.9); by++)
            for (int bx = (int) Math.floor(p.getX() - 0.7); bx <= (int) Math.floor(p.getX() + 0.7); bx++)
                for (int bz = (int) Math.floor(p.getZ() - 0.7); bz <= (int) Math.floor(p.getZ() + 0.7); bz++) {
                    BlockPos b = new BlockPos(bx, by, bz);
                    BlockState s = w.getBlockState(b);
                    Direction.Axis axe;
                    double cy;
                    if (s.isOf(OmbreBlocs.PERCHE)) { axe = s.get(Properties.HORIZONTAL_AXIS); cy = by + 0.5; }
                    else if (s.getBlock() instanceof fr.fenua.ombre.bloc.Mobilier.Enseigne) { axe = s.get(Properties.HORIZONTAL_FACING).getAxis(); cy = by + 13 / 16.0; }
                    else if (s.getBlock() instanceof fr.fenua.ombre.bloc.Decor.Reverbere) { axe = s.get(Properties.HORIZONTAL_FACING).getAxis(); cy = by + 14 / 16.0; }
                    else continue;
                    if (Math.abs(mains - cy) > 0.9) continue;
                    // Le balancement se fait en travers de la barre
                    double sx = axe == Direction.Axis.X ? 0 : 1, sz = axe == Direction.Axis.X ? 1 : 0;
                    Vec3d v = p.getVelocity();
                    double vs = v.x * sx + v.z * sz;
                    if (Math.abs(vs) < 0.05) { double[] f = devant(p); vs = f[0] * sx + f[1] * sz; }
                    double signe = vs >= 0 ? 1 : -1;
                    balX = sx * signe; balZ = sz * signe;
                    pivot = axe == Direction.Axis.X ? new Vec3d(p.getX(), cy, bz + 0.5) : new Vec3d(bx + 0.5, cy, p.getZ());
                    theta = -0.45;
                    omega = Math.max(0.12, Math.abs(vs) / MAINS);
                    changer(Etat.PERCHE);
                    p.playSound(SoundEvents.BLOCK_WOOD_HIT, 0.6f, 1.2f);
                    rattrape();
                    return true;
                }
        return false;
    }

    private static void perche(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        if (in.sneaking) { lacher(10); return; }
        // Un pendule : la pesanteur ramène vers le bas, Z donne de l'élan
        omega += -(0.08 / MAINS) * Math.sin(theta);
        if (in.movementForward > 0 && omega * theta >= 0) omega *= 1.06;
        omega = MathHelper.clamp(omega, -0.3, 0.3);
        omega *= 0.995;
        theta += omega;
        boolean lancer = nouveauSaut || (in.movementForward > 0 && theta > 0.5 && omega > 0);
        if (lancer) {
            // Un élan fixe, réglé sur la physique de Minecraft : les mains arrivent à hauteur
            // d'une perche placée 5 blocs plus loin (voir CarteParcours, épreuve des perches)
            p.setVelocity(balX * 0.56, 0.34, balZ * 0.56);
            poseBond = 10;
            sauter(p, 7, 0);
            return;
        }
        Vec3d pieds = pivot.add(balX * MAINS * Math.sin(theta), -MAINS * Math.cos(theta), balZ * MAINS * Math.sin(theta));
        p.setVelocity(pieds.subtract(p.getPos()));
        if (duree % 10 == 0) rattrape();
        if (duree > 200) lacher(10);
    }

    // ───────────────────────── la main qui attrape (clic droit) ─────────────────────────

    /** En l'air, les mains passent près d'un câble : on s'y accroche et on glisse vers le bas. */
    private static boolean attraperCable(ClientPlayerEntity p) {
        long maintenant = System.currentTimeMillis();
        Vec3d mains = p.getPos().add(0, MAINS, 0);
        for (RenduCordes.Cable c : RenduCordes.CABLES.values()) {
            if (maintenant - c.vu() > 2000) continue;
            boolean versB = c.b().y <= c.a().y;                      // on glisse vers le bas
            Vec3d haut = versB ? c.a() : c.b(), bas = versB ? c.b() : c.a();
            BlockPos fin = versB ? c.poteauB() : c.poteauA();
            Vec3d d = bas.subtract(haut);
            double t = MathHelper.clamp(mains.subtract(haut).dotProduct(d) / d.lengthSquared(), 0, 1);
            if (t < 0.05 || t > 0.9) continue;
            if (CoteClient.pointCable(haut, bas, t).distanceTo(mains) > 1.1) continue;
            lancerCable(p, haut, bas, fin, t);
            return true;
        }
        return false;
    }

    /** Le clic droit sert au parkour sauf avec un bloc en main (on construit). */
    private static boolean mainLibre(ClientPlayerEntity p) {
        return !(p.getMainHandStack().getItem() instanceof net.minecraft.item.BlockItem);
    }

    /**
     * Le clic droit doit-il servir au parkour plutôt qu'à l'objet tenu ? (lu par mixin/ClicMixin)
     * Oui en l'air quand une perche, une enseigne ou un câble est à portée de main.
     */
    public static boolean clicPourParkour(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || !mainLibre(p) || p.getAbilities().flying || p.isSpectator()) return false;
        if (etat == Etat.PERCHE || etat == Etat.TYRO) return true;
        return etat == Etat.LIBRE && !p.isOnGround() && (percheProche(p) || cableProche(p));
    }

    /** Une gouttière à portée de main, contre le mur ? */
    private static boolean gouttiere(ClientPlayerEntity p) {
        World w = p.getWorld();
        for (BlockPos b : BlockPos.iterate(BlockPos.ofFloored(p.getX() - 0.8, p.getY(), p.getZ() - 0.8),
                BlockPos.ofFloored(p.getX() + 0.8, p.getY() + 1.8, p.getZ() + 0.8)))
            if (w.getBlockState(b).getBlock() instanceof fr.fenua.ombre.bloc.Decor.Gouttiere) return true;
        return false;
    }

    private static boolean percheProche(ClientPlayerEntity p) {
        World w = p.getWorld();
        for (BlockPos b : BlockPos.iterate(BlockPos.ofFloored(p.getX() - 1, p.getY() + 1, p.getZ() - 1),
                BlockPos.ofFloored(p.getX() + 1, p.getY() + 3, p.getZ() + 1))) {
            BlockState s = w.getBlockState(b);
            if (s.isOf(OmbreBlocs.PERCHE) || s.getBlock() instanceof fr.fenua.ombre.bloc.Mobilier.Enseigne
                    || s.getBlock() instanceof fr.fenua.ombre.bloc.Decor.Reverbere) return true;
        }
        return false;
    }

    private static boolean cableProche(ClientPlayerEntity p) {
        Vec3d mains = p.getPos().add(0, MAINS, 0);
        for (RenduCordes.Cable c : RenduCordes.CABLES.values()) {
            Vec3d d = c.b().subtract(c.a());
            double t = MathHelper.clamp(mains.subtract(c.a()).dotProduct(d) / d.lengthSquared(), 0, 1);
            if (CoteClient.pointCable(c.a(), c.b(), t).distanceTo(mains) < 1.5) return true;
        }
        return false;
    }

    // ───────────────────────── PLONGEON, SYNCHRONISATION ─────────────────────────

    private static boolean plongeon;

    /** Point de vue synchronisé : la caméra fait le tour du joueur, comme un aigle qui plane. */
    public static void synchro() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) Cinema.synchro(p);
    }

    // ───────────────────────── LANCE-GRAPPIN ─────────────────────────

    private static Vec3d grappinAncre, grappinBut;

    /** Le crochet du lance-grappin, s'il est accroché (pour dessiner la corde). */
    public static Vec3d ancreGrappin() { return etat == Etat.GRAPPIN ? grappinAncre : null; }

    public static void lancerGrappin() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || etat == Etat.GRAPPIN || etat == Etat.EXEC) return;
        net.minecraft.util.hit.HitResult h = p.raycast(40, 0, false);
        if (!(h instanceof net.minecraft.util.hit.BlockHitResult bh) || h.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
            p.sendMessage(Text.literal("§7Le crochet ne trouve rien où s'accrocher."), true);
            return;
        }
        World w = p.getWorld();
        BlockPos b = bh.getBlockPos();
        // Le haut du mur visé : on monte la colonne jusqu'au premier endroit libre sur 2 blocs
        int haut = Integer.MIN_VALUE;
        for (int y = b.getY(); y <= b.getY() + 8; y++) {
            BlockPos c = b.withY(y);
            if (!w.getBlockState(c).getCollisionShape(w, c).isEmpty()
                    && w.getBlockState(c.up()).getCollisionShape(w, c.up()).isEmpty()
                    && w.getBlockState(c.up(2)).getCollisionShape(w, c.up(2)).isEmpty()) { haut = y + 1; break; }
        }
        if (haut == Integer.MIN_VALUE) { p.sendMessage(Text.literal("§7Pas de bord où se hisser là-haut."), true); return; }
        double dx = bh.getPos().x - p.getX(), dz = bh.getPos().z - p.getZ(), n = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        murX = dx / n; murZ = dz / n;
        cible = haut;
        grappinAncre = new Vec3d(bh.getPos().x, haut, bh.getPos().z);
        grappinBut = new Vec3d(bh.getPos().x - murX * 0.7, haut - 1.2, bh.getPos().z - murZ * 0.7);
        changer(Etat.GRAPPIN);
        p.playSound(SoundEvents.ENTITY_FISHING_BOBBER_THROW, 0.8f, 0.8f);
        rattrape();
    }

    private static void grappin(ClientPlayerEntity p) {
        Vec3d d = grappinBut.subtract(p.getPos());
        double l = d.length();
        if (l < 1.0 || duree > 80 || (duree > 4 && p.horizontalCollision && p.getY() > cible - 2.2)) {
            hisser(p, murX, murZ, cible);
            return;
        }
        double v = Math.min(1.1, 0.25 + duree * 0.06);
        p.setVelocity(d.multiply(Math.min(v, l * 0.5) / l));
        if (duree % 4 == 0) p.playSound(SoundEvents.BLOCK_CHAIN_STEP, 0.3f, 1.2f);
        if (duree % 10 == 0) rattrape();
    }

    // ───────────────────────── ASSASSINAT MIS EN SCÈNE ─────────────────────────

    private static int execType, execDuree;
    private static Vec3d execCible;
    private static float execYaw;
    private static Etat etatAvantExec = Etat.LIBRE;

    /** Progression du geste d'assassinat, de 0 à 1 (pour les poses). */
    static float progresExec() { return execDuree == 0 ? 0 : Math.min(1f, (float) duree / execDuree); }

    /** Le serveur a lancé un assassinat : on joue le geste, la pose, et la caméra passe en vue extérieure. */
    public static void executer(fr.fenua.ombre.ExecutionPayload e) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity p = client.player;
        if (p == null) return;
        etatAvantExec = etat == Etat.REBORD ? Etat.REBORD : Etat.LIBRE;
        execType = e.type();
        execDuree = fr.fenua.ombre.ExecutionPayload.DUREE[e.type()];
        ExecClient.demarrer(e);
        execCible = new Vec3d(e.x(), e.y(), e.z());
        execYaw = e.yaw();
        Cinema.execution(p, execCible, execDuree + 24);        // on reste le temps de le voir tomber
        changer(Etat.EXEC);
        p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 0.8f);
    }

    private static void execution(MinecraftClient client, ClientPlayerEntity p) {
        // Le joueur suit la chorégraphie : sa place, et son orientation (la projection le fait pivoter)
        double k = ExecClient.k(0) + 1.0 / execDuree;
        if (fr.fenua.ombre.Choregraphie.suspendu(execType)) p.setVelocity(0, 0, 0);
        else {
            Vec3d but = ExecClient.posJoueur(k), v = but.subtract(p.getPos());
            p.setVelocity(v.x * 0.7, v.y * 0.7, v.z * 0.7);
        }
        p.setBodyYaw(ExecClient.lacet() + fr.fenua.ombre.Choregraphie.lacetJoueur(execType, Math.min(1, k)));
        p.setHeadYaw(p.getBodyYaw());
        if (duree % 5 == 0) rattrape();
        if (duree >= execDuree + 4) {
            changer(etatAvantExec == Etat.REBORD && fr.fenua.ombre.Choregraphie.suspendu(execType) ? Etat.REBORD : Etat.LIBRE);
        }
    }

    // ───────────────────────── gestes communs ─────────────────────────

    private static void sauter(ClientPlayerEntity p, int sans, int cherche) {
        changer(Etat.LIBRE);
        sansPrise = sans;
        chercheMur = cherche;
        p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.2f);
        rattrape();
    }

    private static void lacher(int sans) {
        changer(Etat.LIBRE);
        sansPrise = sans;
        chercheMur = 0;
    }

    /** Dit au serveur d'oublier la chute en cours (sinon il compterait les dégâts depuis le début). */
    private static void rattrape() {
        if (ClientPlayNetworking.canSend(ActionPayload.ID))
            ClientPlayNetworking.send(new ActionPayload(ActionPayload.RATTRAPE));
    }

    // ───────────────────────── géométrie ─────────────────────────

    /** Direction horizontale du regard (vecteur unité). */
    private static double[] devant(ClientPlayerEntity p) {
        float yaw = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        return new double[]{-MathHelper.sin(yaw), MathHelper.cos(yaw)};
    }

    /** Direction voulue par les touches (Z/Q/S/D), dans le monde, selon le regard. Longueur 0 à 1. */
    private static double[] entree(ClientPlayerEntity p, Input in) {
        float yaw = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double av = in.movementForward, co = in.movementSideways;
        double x = co * MathHelper.cos(yaw) - av * MathHelper.sin(yaw);
        double z = av * MathHelper.cos(yaw) + co * MathHelper.sin(yaw);
        double n = Math.sqrt(x * x + z * z);
        return n > 1 ? new double[]{x / n, z / n} : new double[]{x, z};
    }

    private static double norme(double[] v) { return Math.sqrt(v[0] * v[0] + v[1] * v[1]); }
    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1]; }
    private static double dot(double[] a, double x, double z) { return a[0] * x + a[1] * z; }

    /** Un mur dans l'une des quatre directions, la plus proche de « pref » (et à moins de 75° d'elle). */
    private static double[] murAutour(ClientPlayerEntity p, double[] pref) {
        double[] meilleur = null;
        double score = 0.25;
        for (Direction d : Direction.Type.HORIZONTAL) {
            double dx = d.getOffsetX(), dz = d.getOffsetZ();
            double s = dot(pref, dx, dz);
            if (s > score && bloqueBuste(p, dx * 0.25, dz * 0.25, 0.6)) {
                score = s;
                meilleur = new double[]{dx, dz};
            }
        }
        return meilleur;
    }

    /** En dessous de cette hauteur (depuis le sol où l'on est), un mur ne se grimpe pas : on saute par-dessus. */
    static final double MUR_MIN = 1.8;

    /**
     * Le mur dans la direction (dx, dz) est-il assez haut pour s'y accrocher ? On le mesure depuis le sol sous
     * le joueur : un muret d'un bloc, une barrière ou un mur de pierre (1,5) ne s'escaladent pas et n'offrent pas
     * de rebord — sinon, en retombant d'un saut à côté, on restait pendu les pieds au ras du sol.
     */
    private static boolean assezHaut(ClientPlayerEntity p, double dx, double dz) {
        World w = p.getWorld();
        // Le sol sous les pieds (jusqu'à 5 blocs plus bas ; au-delà, c'est un vrai vide : tout mur compte)
        double sol = Double.NaN;
        if (p.isOnGround()) sol = p.getY();
        else for (int y = (int) Math.floor(p.getY()); y >= (int) Math.floor(p.getY()) - 5 && Double.isNaN(sol); y--) {
            BlockPos b = BlockPos.ofFloored(p.getX(), y, p.getZ());
            var forme = w.getBlockState(b).getCollisionShape(w, b);
            if (!forme.isEmpty() && y + forme.getMax(Direction.Axis.Y) <= p.getY() + 0.01) sol = y + forme.getMax(Direction.Axis.Y);
        }
        if (Double.isNaN(sol)) return true;
        // Le mur, depuis ce sol : on monte tant qu'il est plein
        double x = p.getX() + dx * 0.6, z = p.getZ() + dz * 0.6;
        int y0 = (int) Math.floor(sol + 0.01);
        BlockPos pied = BlockPos.ofFloored(x, y0, z);
        if (w.getBlockState(pied).getCollisionShape(w, pied).isEmpty()) return true;   // un mur qui commence plus haut (surplomb)
        double haut = sol;
        for (int y = y0; y < y0 + 4; y++) {
            BlockPos b = BlockPos.ofFloored(x, y, z);
            var forme = w.getBlockState(b).getCollisionShape(w, b);
            if (forme.isEmpty()) break;
            haut = y + forme.getMax(Direction.Axis.Y);
            if (forme.getMax(Direction.Axis.Y) < 0.99) break;                   // une dalle, une barrière : ça s'arrête là
        }
        return haut - sol >= MUR_MIN;
    }

    /** Vrai si la boîte du joueur, décalée de (dx, dz), touche un bloc. */
    private static boolean bloque(ClientPlayerEntity p, double dx, double dz) {
        return !p.getWorld().isSpaceEmpty(p, p.getBoundingBox().offset(dx, 0, dz));
    }

    /** Même chose, mais seulement à partir de « bas » au-dessus des pieds : un vrai mur, pas une marche. */
    private static boolean bloqueBuste(ClientPlayerEntity p, double dx, double dz, double bas) {
        Box b = p.getBoundingBox();
        Box buste = new Box(b.minX, b.minY + bas, b.minZ, b.maxX, b.maxY - 0.1, b.maxZ).offset(dx, 0, dz);
        return !p.getWorld().isSpaceEmpty(p, buste);
    }

    /**
     * Hauteur du haut d'un mur situé en (x, z), s'il arrive entre (pieds + bas) et (pieds + haut)
     * et qu'il y a la place d'un corps au-dessus. Sinon : NaN (« pas de rebord »).
     */
    private static double hauteurRebord(ClientPlayerEntity p, double x, double z, double bas, double haut) {
        World w = p.getWorld();
        double res = Double.NaN;
        for (int y = (int) Math.floor(p.getY() + bas - 1); y <= (int) Math.floor(p.getY() + haut); y++) {
            BlockPos b = BlockPos.ofFloored(x, y, z);
            var forme = w.getBlockState(b).getCollisionShape(w, b);
            if (forme.isEmpty()) continue;
            double top = y + forme.getMax(Direction.Axis.Y);
            if (top < p.getY() + bas || top > p.getY() + haut) continue;
            BlockPos h1 = BlockPos.ofFloored(x, top + 0.5, z), h2 = h1.up();
            if (w.getBlockState(h1).getCollisionShape(w, h1).isEmpty() && w.getBlockState(h2).getCollisionShape(w, h2).isEmpty())
                res = top;
        }
        return res;
    }
}



