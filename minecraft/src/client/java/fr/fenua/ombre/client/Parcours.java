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
 *  MUR        : accroché à un mur (clic droit), on reste en place. Z/S montent et descendent (vers le mur / à l'opposé),
 *               Q/D se déplacent sur le côté. Espace : bond vers le haut ; avec Q/D, bond sur le côté ;
 *               en tirant à l'opposé du mur, saut arrière vers le mur d'en face (clic droit pour s'y raccrocher).
 *               Maj : lâcher. En haut, on se rétablit tout seul.
 *  REBORD     : suspendu par les mains à un bord (clic droit en passant devant). Q/D glissent, Q/D + Espace sautent vers le bord voisin,
 *               Z ou Espace se hissent, S redescend sur le mur, Maj lâche.
 *  HISSE      : le rétablissement par-dessus un bord.
 *  MUR_COURU  : course sur un mur, en sprint. Espace rebondit vers le mur d'en face.
 *  TYRO       : glisse le long d'une tyrolienne. Espace saute, Maj lâche.
 *  LIFT       : monte-charge (clic droit dessus) : on monte jusqu'au toit.
 *
 * Le CLIC DROIT est la main qui attrape : au sol face à un mur, on s'y accroche ; en l'air, tant qu'il
 * est maintenu, on saisit ce qui passe à portée (mur, rebord, perche, enseigne, câble), et la course sur
 * mur ne part qu'avec lui. Sans clic droit, on ne s'accroche à rien. (Avec un bloc en main, il sert à construire.)
 *  PERCHE     : balancé sous une perche ou une enseigne ; Z donne de l'élan et projette, Espace saute, Maj lâche.
 */
public final class Parcours {
    private Parcours() {}

    enum Etat { LIBRE, MUR, REBORD, HISSE, MUR_COURU, TYRO, LIFT, PERCHE }

    /** Poses reprises par les animations (Animations). */
    public enum Pose { AUCUNE, MUR, REBORD, HISSE, MUR_COURU, TYRO, LIFT, PERCHE, BOND }

    private static Etat etat = Etat.LIBRE;
    private static double murX, murZ;          // direction du mur (vers lui), vecteur unité
    private static int duree;                  // ticks passés dans l'état courant
    private static int sansPrise;              // ticks sans pouvoir s'accrocher (après un lâcher, un saut)
    private static int bondMur;                // ticks d'un bond vers le haut le long du mur
    private static int poseBond;               // ticks de la pose de bond
    private static double cible;               // hauteur visée (rétablissement, monte-charge)
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
            case HISSE -> Pose.HISSE;
            case MUR_COURU -> Pose.MUR_COURU;
            case TYRO -> Pose.TYRO;
            case LIFT -> Pose.LIFT;
            case PERCHE -> Pose.PERCHE;
            default -> poseBond > 0 ? Pose.BOND : Pose.AUCUNE;
        };
    }

    static double balancement() { return theta; }


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
            if (poseBond > 0) poseBond--;
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
            }
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
        boolean nouvellePrise = prise && !priseAvant;
        double[] e = entree(p, in);
        double[] f = devant(p);

        // Bond : le saut en sprint porte plus loin
        if (solAvant && !sol && v.y > 0.3 && p.isSprinting()) {
            p.setVelocity(v.x * 1.25, v.y + 0.04, v.z * 1.25);
            poseBond = 10;
            return;
        }

        // Au sol, clic droit face à un mur : on s'y accroche (ou on s'y rétablit s'il est bas)
        if (sol && nouvellePrise) {
            double[] d = murAutour(p, f);
            if (d != null && bloqueBuste(p, d[0] * 0.35, d[1] * 0.35, 1.0)) {
                double h = hauteurRebord(p, p.getX() + d[0] * 0.6, p.getZ() + d[1] * 0.6, 0.9, 2.4);
                if (!Double.isNaN(h)) { hisser(p, d[0], d[1], h); return; }
                accrocher(p, d);
                bondMur = 5;
                p.setVelocity(d[0] * 0.04, 0.42, d[1] * 0.04);
            }
            return;
        }
        if (sol) return;

        // En l'air, on n'attrape rien sans le clic droit (la main qui saisit)
        if (!prise || sansPrise > 0) return;

        // Une perche ou une enseigne à portée de main, ou un câble de tyrolienne
        if (attraperPerche(p)) return;
        if (v.y < 0.25 && attraperCable(p)) return;
        double vh = Math.sqrt(v.x * v.x + v.z * v.z);
        double[] pref = vh > 0.08 ? new double[]{v.x / vh, v.z / vh} : (norme(e) > 0.1 ? e : f);

        // Le haut d'un mur passe aux mains : on s'accroche au rebord
        if (v.y < 0.1 && !in.sneaking) {
            double[] d = murAutour(p, pref);
            if (d != null && !Double.isNaN(hauteurRebord(p, p.getX() + d[0] * 0.6, p.getZ() + d[1] * 0.6, 0.9, 2.4))) {
                murX = d[0]; murZ = d[1];
                changer(Etat.REBORD);
                p.setVelocity(0, 0, 0);
                p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 0.8f);
                rattrape();
                return;
            }
        }

        // Course sur mur : en sprint, en l'air, un mur sur le côté, Z enfoncé
        if (in.movementForward > 0 && vh > 0.2 && v.y < 0.25) {
            for (int cote = -1; cote <= 1; cote += 2) {
                double sx = -f[1] * cote, sz = f[0] * cote;          // perpendiculaire au regard
                if (bloque(p, sx * 0.3, sz * 0.3) && bloqueBuste(p, sx * 0.6, sz * 0.6, 0.6)) {
                    murX = sx; murZ = sz;
                    coteMur = -cote;
                    changer(Etat.MUR_COURU);
                    rattrape();
                    return;
                }
            }
        }

        // S'accrocher au mur qu'on touche
        if (!in.sneaking) {
            double[] d = murAutour(p, pref);
            if (d == null) d = murAutour(p, norme(e) > 0.1 ? e : devant(p));
            if (d != null && bloqueBuste(p, d[0] * 0.25, d[1] * 0.25, 1.0)) {
                accrocher(p, d);
                p.setVelocity(0, Math.max(v.y, -0.1), 0);
            }
        }
    }

    private static void accrocher(ClientPlayerEntity p, double[] d) {
        murX = d[0]; murZ = d[1];
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

        if (in.sneaking) { lacher(10); return; }

        // Le haut du mur est à portée : on se rétablit (en montant, ou à la fin d'un bond)
        if (vers > 0.3 || bondMur > 0) {
            double h = hauteurRebord(p, p.getX() + murX * 0.6, p.getZ() + murZ * 0.6, -0.3, 1.9);
            if (!Double.isNaN(h)) { hisser(p, murX, murZ, h); return; }
        }

        // Plus de mur devant ?
        if (!bloqueBuste(p, murX * 0.3, murZ * 0.3, 0.3) && !bloque(p, murX * 0.3, murZ * 0.3)) {
            changer(Etat.LIBRE);
            return;
        }

        if (nouveauSaut) {
            if (Math.abs(cote) > 0.5) {                          // bond sur le côté
                double s = Math.signum(cote);
                p.setVelocity(tx * s * 0.45 + murX * 0.08, 0.4, tz * s * 0.45 + murZ * 0.08);
                sauter(p, 3);
            } else if (vers < -0.5) {                            // saut arrière, vers le mur d'en face
                p.setVelocity(-murX * 0.55, 0.45, -murZ * 0.55);
                sauter(p, 5);
            } else {                                             // bond vers le haut
                bondMur = 9;
                p.setVelocity(murX * 0.04, 0.62, murZ * 0.04);
                p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.3f);
            }
            return;
        }

        if (bondMur > 0) {                                       // en l'air le long du mur : la gravité agit
            bondMur--;
            p.setVelocity(murX * 0.04, v.y, murZ * 0.04);
            return;
        }

        boolean vite = client.options.sprintKey.isPressed();
        double vy = vers > 0.3 ? (vite ? 0.2 : 0.13) : vers < -0.3 ? -0.17 : 0;
        if (vy < 0 && sol) { changer(Etat.LIBRE); return; }
        double lat = 0;
        if (Math.abs(cote) > 0.3) {
            double s = Math.signum(cote);
            // On ne file pas au-delà du bout du mur
            if (bloqueBuste(p, murX * 0.3 + tx * s * 0.45, murZ * 0.3 + tz * s * 0.45, 0.3)) lat = s * (vite ? 0.15 : 0.1);
        }
        p.setVelocity(murX * 0.05 + tx * lat, vy, murZ * 0.05 + tz * lat);
        if (vy != 0 || lat != 0) {
            phase += 0.35f;
            if (duree % 6 == 0) p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.25f, 1.3f);
        }
        if (duree % 10 == 0) rattrape();
    }

    // ───────────────────────── REBORD ─────────────────────────

    private static void rebord(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        double[] e = entree(p, in);
        double tx = -murZ, tz = murX;
        double vers = dot(e, murX, murZ), cote = e[0] * tx + e[1] * tz;

        if (in.sneaking) { lacher(12); return; }
        if (nouveauSaut) {
            if (Math.abs(cote) > 0.5) {                          // saut vers le rebord voisin
                double s = Math.signum(cote);
                p.setVelocity(tx * s * 0.45 + murX * 0.06, 0.34, tz * s * 0.45 + murZ * 0.06);
                sauter(p, 3);
            } else if (vers < -0.5) {                            // saut arrière
                p.setVelocity(-murX * 0.55, 0.45, -murZ * 0.55);
                sauter(p, 5);
            } else {
                monterDuRebord(p);
            }
            return;
        }
        if (vers > 0.5 && duree > 6) { monterDuRebord(p); return; }
        if (vers < -0.5 && duree > 6 && bloqueBuste(p, murX * 0.3, murZ * 0.3, 0.3)) {   // redescendre sur le mur
            changer(Etat.MUR);
            return;
        }
        // Glisser aux mains le long du bord
        double gx = 0, gz = 0;
        if (Math.abs(cote) > 0.3) {
            double s = Math.signum(cote);
            gx = tx * s * 0.12; gz = tz * s * 0.12;
            if (Double.isNaN(hauteurRebord(p, p.getX() + gx * 4 + murX * 0.6, p.getZ() + gz * 4 + murZ * 0.6, 1.0, 2.2))) { gx = 0; gz = 0; }
            else phase += 0.3f;
        }
        p.setVelocity(gx + murX * 0.02, 0, gz + murZ * 0.02);
        if (!bloque(p, murX * 0.3, murZ * 0.3)) changer(Etat.LIBRE);
        if (duree % 10 == 0) rattrape();
    }

    private static void monterDuRebord(ClientPlayerEntity p) {
        double h = hauteurRebord(p, p.getX() + murX * 0.6, p.getZ() + murZ * 0.6, 0.5, 2.3);
        if (!Double.isNaN(h)) hisser(p, murX, murZ, h);
    }

    // ───────────────────────── HISSE (rétablissement) ─────────────────────────

    private static void hisser(ClientPlayerEntity p, double fx, double fz, double hauteur) {
        changer(Etat.HISSE);
        murX = fx; murZ = fz; cible = hauteur;
        p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 1.0f);
        sansPrise = 10;
        hisse(p);
        rattrape();
    }

    private static void hisse(ClientPlayerEntity p) {
        if (duree > 18) { changer(Etat.LIBRE); return; }
        if (p.getY() < cible + 0.02) {
            p.setVelocity(murX * 0.03, Math.min(0.42, cible + 0.08 - p.getY()) + 0.05, murZ * 0.03);
        } else {
            p.setVelocity(murX * 0.28, 0.05, murZ * 0.28);       // on bascule par-dessus le bord
            changer(Etat.LIBRE);
        }
    }

    // ───────────────────────── MUR_COURU ─────────────────────────

    private static void murCouru(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        Vec3d v = p.getVelocity();
        if (nouveauSaut) {                                       // rebond vers le mur d'en face
            double[] f = devant(p);
            p.setVelocity(-murX * 0.5 + f[0] * 0.3, 0.55, -murZ * 0.5 + f[1] * 0.3);
            sauter(p, 4);
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
            sauter(p, 8);
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
        for (int by = (int) Math.floor(mains - 0.6); by <= (int) Math.floor(mains + 0.6); by++)
            for (int bx = (int) Math.floor(p.getX() - 0.7); bx <= (int) Math.floor(p.getX() + 0.7); bx++)
                for (int bz = (int) Math.floor(p.getZ() - 0.7); bz <= (int) Math.floor(p.getZ() + 0.7); bz++) {
                    BlockPos b = new BlockPos(bx, by, bz);
                    BlockState s = w.getBlockState(b);
                    Direction.Axis axe;
                    double cy;
                    if (s.isOf(OmbreBlocs.PERCHE)) { axe = s.get(Properties.HORIZONTAL_AXIS); cy = by + 0.5; }
                    else if (s.isOf(OmbreBlocs.ENSEIGNE)) { axe = s.get(Properties.HORIZONTAL_FACING).getAxis(); cy = by + 13 / 16.0; }
                    else continue;
                    if (Math.abs(mains - cy) > 0.7) continue;
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
            double vs = MAINS * omega * Math.cos(theta) + 0.3, vy = MAINS * omega * Math.sin(theta) + 0.3;
            p.setVelocity(balX * vs, Math.max(vy, 0.35), balZ * vs);
            poseBond = 10;
            sauter(p, 7);
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
     * Oui si l'on est accroché, ou s'il y a quelque chose à attraper à portée de main.
     */
    public static boolean clicPourParkour(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || !mainLibre(p) || p.getAbilities().flying || p.isSpectator()) return false;
        if (etat != Etat.LIBRE) return true;
        // Un poteau, un monte-charge ou un bloc utile visé : on laisse Minecraft s'en occuper
        if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bh
                && client.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockState s = p.getWorld().getBlockState(bh.getBlockPos());
            if (s.isOf(OmbreBlocs.POTEAU) || s.isOf(OmbreBlocs.MONTE_CHARGE) || s.hasBlockEntity()) return false;
        }
        double[] f = devant(p);
        if (p.isOnGround()) return murAutour(p, f) != null && bloqueBuste(p, f[0] * 0.35, f[1] * 0.35, 1.0);
        for (Direction d : Direction.Type.HORIZONTAL)
            if (bloqueBuste(p, d.getOffsetX() * 0.3, d.getOffsetZ() * 0.3, 0.6)) return true;
        return percheProche(p) || cableProche(p);
    }

    private static boolean percheProche(ClientPlayerEntity p) {
        World w = p.getWorld();
        for (BlockPos b : BlockPos.iterate(BlockPos.ofFloored(p.getX() - 1, p.getY() + 1, p.getZ() - 1),
                BlockPos.ofFloored(p.getX() + 1, p.getY() + 3, p.getZ() + 1))) {
            BlockState s = w.getBlockState(b);
            if (s.isOf(OmbreBlocs.PERCHE) || s.isOf(OmbreBlocs.ENSEIGNE)) return true;
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

    // ───────────────────────── gestes communs ─────────────────────────

    private static void sauter(ClientPlayerEntity p, int sans) {
        changer(Etat.LIBRE);
        sansPrise = sans;
        p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.2f);
        rattrape();
    }

    private static void lacher(int sans) {
        changer(Etat.LIBRE);
        sansPrise = sans;
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
