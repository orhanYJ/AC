package fr.fenua.ombre.client;

import fr.fenua.ombre.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Le parkour, calculé chez le joueur (c'est lui qui décide de ses mouvements dans Minecraft).
 * Chaque image du jeu (20 par seconde), on regarde où il est et on ajuste sa vitesse.
 *
 *  - Rétablissement : en haut d'une escalade, on passe par-dessus le rebord tout seul.
 *  - Rebord : en tombant le long d'un mur dont le haut arrive aux mains, on s'y accroche.
 *      Espace = se hisser, Q/D = se déplacer le long, Maj = lâcher, S + Espace = saut arrière.
 *  - Course sur mur : en sprintant, saute le long d'un mur : on court dessus ~1,5 s.
 *      Espace = rebondir de l'autre côté (enchaîner deux murs face à face).
 *  - Saut arrière : contre un mur (escalade ou rebord), S + Espace = se jeter en arrière.
 *  - Bond : un saut en sprint porte plus loin.
 */
public final class Parcours {
    private Parcours() {}

    private enum Etat { LIBRE, REBORD, MUR_COURU, HISSE }

    private static Etat etat = Etat.LIBRE;
    private static double murX, murZ;          // direction du mur (vers lui), pour le rebord et la course
    private static int duree;                  // ticks passés dans l'état courant
    private static int sansPrise;              // ticks pendant lesquels on ne peut pas se raccrocher
    private static int contactMur;             // ticks depuis le dernier contact de face avec un mur
    private static boolean sautAvant, solAvant;
    private static double cible;               // hauteur du rebord vers lequel on se hisse

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null) return;
        Input in = p.input;
        boolean saut = in.jumping;
        boolean nouveauSaut = saut && !sautAvant;
        boolean sol = p.isOnGround();
        try {
            if (p.getAbilities().flying || p.isSpectator() || p.isTouchingWater() || p.isFallFlying()
                    || p.hasVehicle() || p.isUsingItem() && etat == Etat.LIBRE) {
                etat = Etat.LIBRE;
                return;
            }
            if (sansPrise > 0) sansPrise--;
            double[] f = devant(p);
            boolean murDevant = bloque(p, f[0] * 0.25, f[1] * 0.25);
            contactMur = murDevant ? 0 : contactMur + 1;
            if (sol && etat != Etat.HISSE) etat = Etat.LIBRE;

            switch (etat) {
                case LIBRE -> libre(p, in, f, murDevant, nouveauSaut, sol);
                case REBORD -> rebord(p, in, nouveauSaut);
                case MUR_COURU -> murCouru(p, in, nouveauSaut);
                case HISSE -> hisse(p);
            }
        } finally {
            sautAvant = saut;
            solAvant = sol;
        }
    }

    // ───────────── en mouvement libre ─────────────

    private static void libre(ClientPlayerEntity p, Input in, double[] f, boolean murDevant, boolean nouveauSaut, boolean sol) {
        Vec3d v = p.getVelocity();

        // Bond : le saut en sprint porte plus loin (au moment où l'on quitte le sol)
        if (solAvant && !sol && v.y > 0.3 && p.isSprinting()) {
            p.setVelocity(v.x * 1.25, v.y + 0.04, v.z * 1.25);
            return;
        }

        // Saut arrière : contre un mur en l'air, S + Espace
        if (!sol && nouveauSaut && in.movementForward < 0 && contactMur <= 3) {
            sauterArriere(p, f[0], f[1]);
            return;
        }

        // Rétablissement en haut d'une escalade : pieds contre le mur, tête au-dessus
        if (murDevant && !sol && in.jumping) {
            double h = hauteurRebord(p, p.getX() + f[0] * 0.6, p.getZ() + f[1] * 0.6, 0.0, 1.05);
            if (!Double.isNaN(h)) { hisser(p, f[0], f[1], h); return; }
        }

        if (sol || sansPrise > 0) return;

        // Accroche au rebord : on tombe, le haut du mur passe à hauteur des mains
        if (murDevant && v.y < -0.05 && !in.sneaking && in.movementForward >= 0
                && !Double.isNaN(hauteurRebord(p, p.getX() + f[0] * 0.6, p.getZ() + f[1] * 0.6, 1.1, 2.1))) {
            etat = Etat.REBORD; duree = 0;
            murX = f[0]; murZ = f[1];
            p.setVelocity(0, 0, 0);
            p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 0.8f);
            rattrape();
            return;
        }

        // Course sur mur : en sprint (ou lancé), en l'air, un mur sur le côté, Z enfoncé
        double vh = Math.sqrt(v.x * v.x + v.z * v.z);
        if (in.movementForward > 0 && vh > 0.2 && v.y < 0.25 && duree >= 0) {
            for (int cote = -1; cote <= 1; cote += 2) {
                double sx = -f[1] * cote, sz = f[0] * cote;          // perpendiculaire au regard
                if (bloque(p, sx * 0.3, sz * 0.3) && bloque(p.getWorld(), p, sx, sz, 0.6)) {
                    etat = Etat.MUR_COURU; duree = 0;
                    murX = sx; murZ = sz;
                    rattrape();
                    return;
                }
            }
        }
    }

    // ───────────── suspendu à un rebord ─────────────

    private static void rebord(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        duree++;
        if (in.sneaking) {                                     // lâcher
            etat = Etat.LIBRE; sansPrise = 12; return;
        }
        if (nouveauSaut && in.movementForward < 0) {           // saut arrière
            sauterArriere(p, murX, murZ); return;
        }
        if (nouveauSaut) {                                     // se hisser
            double h = hauteurRebord(p, p.getX() + murX * 0.6, p.getZ() + murZ * 0.6, 0.5, 2.3);
            if (!Double.isNaN(h)) { hisser(p, murX, murZ, h); return; }
        }
        // Se déplacer aux mains le long du rebord (Q / D)
        double gx = 0, gz = 0;
        if (in.movementSideways != 0) {
            double s = in.movementSideways > 0 ? 1 : -1;       // positif = gauche
            gx = murZ * s * 0.12; gz = -murX * s * 0.12;
            // On ne file pas au-delà du bout du mur
            if (Double.isNaN(hauteurRebord(p, p.getX() + gx * 4 + murX * 0.6, p.getZ() + gz * 4 + murZ * 0.6, 1.0, 2.2))) { gx = 0; gz = 0; }
        }
        // Collé au mur, immobile en hauteur
        p.setVelocity(gx + murX * 0.02, 0, gz + murZ * 0.02);
        if (!bloque(p, murX * 0.3, murZ * 0.3)) etat = Etat.LIBRE;   // le mur a disparu
        if (duree % 10 == 0) rattrape();
    }

    // ───────────── course sur mur ─────────────

    private static void murCouru(ClientPlayerEntity p, Input in, boolean nouveauSaut) {
        duree++;
        Vec3d v = p.getVelocity();
        if (nouveauSaut) {                                     // rebond : on se repousse du mur
            double[] f = devant(p);
            p.setVelocity(-murX * 0.5 + f[0] * 0.3, 0.55, -murZ * 0.5 + f[1] * 0.3);
            p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.4f);
            etat = Etat.LIBRE; sansPrise = 4;
            return;
        }
        boolean finie = duree > 32 || in.movementForward <= 0 || !bloque(p, murX * 0.3, murZ * 0.3);
        if (finie) { etat = Etat.LIBRE; sansPrise = 6; return; }

        // On garde l'élan le long du mur, et on ne descend presque pas (un peu plus à la fin)
        double[] f = devant(p);
        double ax = f[0] - murX * (f[0] * murX + f[1] * murZ), az = f[1] - murZ * (f[0] * murX + f[1] * murZ);
        double n = Math.sqrt(ax * ax + az * az);
        if (n < 0.2) { etat = Etat.LIBRE; return; }            // on regarde le mur : ce n'est plus une course
        double vit = Math.max(Math.sqrt(v.x * v.x + v.z * v.z), 0.3);
        double vy = duree < 6 ? Math.max(v.y, 0.06) : -0.02 - duree * 0.003;
        p.setVelocity(ax / n * vit + murX * 0.04, vy, az / n * vit + murZ * 0.04);
        if (duree % 4 == 0) {
            BlockPos b = BlockPos.ofFloored(p.getX() + murX * 0.7, p.getY() + 0.5, p.getZ() + murZ * 0.7);
            p.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, p.getWorld().getBlockState(b)),
                    p.getX() + murX * 0.35, p.getY() + 0.3, p.getZ() + murZ * 0.35, 0, 0, 0);
            p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.3f, 1.2f);
        }
        if (duree % 10 == 0) rattrape();
    }

    // ───────────── gestes ─────────────

    /** Commence le rétablissement : on monte jusqu'au niveau du rebord, puis on bascule dessus. */
    private static void hisser(ClientPlayerEntity p, double fx, double fz, double hauteur) {
        etat = Etat.HISSE; duree = 0;
        murX = fx; murZ = fz; cible = hauteur;
        p.playSound(SoundEvents.BLOCK_STONE_STEP, 0.6f, 1.0f);
        sansPrise = 10;
        hisse(p);
        rattrape();
    }

    private static void hisse(ClientPlayerEntity p) {
        duree++;
        if (duree > 16) { etat = Etat.LIBRE; return; }
        if (p.getY() < cible + 0.02) {
            p.setVelocity(murX * 0.03, Math.min(0.42, cible + 0.08 - p.getY()) + 0.05, murZ * 0.03);
        } else {
            p.setVelocity(murX * 0.28, 0.05, murZ * 0.28);   // on bascule par-dessus le rebord
            etat = Etat.LIBRE;
        }
    }

    private static void sauterArriere(ClientPlayerEntity p, double fx, double fz) {
        p.setVelocity(-fx * 0.6, 0.5, -fz * 0.6);
        p.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.4f, 1.2f);
        etat = Etat.LIBRE; sansPrise = 10;
        rattrape();
    }

    /** Dit au serveur d'oublier la chute en cours (sinon il compterait les dégâts depuis le début). */
    private static void rattrape() {
        if (ClientPlayNetworking.canSend(fr.fenua.ombre.ActionPayload.ID))
            ClientPlayNetworking.send(new ActionPayload(ActionPayload.RATTRAPE));
    }

    // ───────────── outils de géométrie ─────────────

    /** Direction horizontale du regard (vecteur unité). */
    private static double[] devant(ClientPlayerEntity p) {
        float yaw = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        return new double[]{-MathHelper.sin(yaw), MathHelper.cos(yaw)};
    }

    /** Vrai si la boîte du joueur, décalée de (dx, dz), touche un bloc. */
    private static boolean bloque(ClientPlayerEntity p, double dx, double dz) {
        return !p.getWorld().isSpaceEmpty(p, p.getBoundingBox().offset(dx, 0, dz));
    }

    /** Même chose, mais seulement à hauteur de buste : un vrai mur, pas une marche. */
    private static boolean bloque(World w, ClientPlayerEntity p, double sx, double sz, double dist) {
        Box b = p.getBoundingBox();
        Box buste = new Box(b.minX, b.minY + 0.6, b.minZ, b.maxX, b.maxY - 0.2, b.maxZ).offset(sx * dist, 0, sz * dist);
        return !w.isSpaceEmpty(p, buste);
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
            double top = y + forme.getMax(net.minecraft.util.math.Direction.Axis.Y);
            if (top < p.getY() + bas || top > p.getY() + haut) continue;
            BlockPos h1 = BlockPos.ofFloored(x, top + 0.5, z), h2 = h1.up();
            if (w.getBlockState(h1).getCollisionShape(w, h1).isEmpty() && w.getBlockState(h2).getCollisionShape(w, h2).isEmpty())
                res = top;
        }
        return res;
    }
}
