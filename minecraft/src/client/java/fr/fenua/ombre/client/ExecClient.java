package fr.fenua.ombre.client;

import fr.fenua.ombre.Choregraphie;
import fr.fenua.ombre.ExecutionPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * L'assassinat en cours, côté joueur : où en est la scène (k de 0 à 1), où dessiner la victime,
 * comment la plier. La victime est dessinée d'après la chorégraphie (et non d'après sa position reçue du serveur,
 * qui arrive par à-coups) : le geste reste fluide.
 */
public final class ExecClient {
    private ExecClient() {}

    private static int victime = -1, scene;
    private static double debut, pf, py, fx, fz;
    private static Vec3d a = Vec3d.ZERO;
    private static float lacet;

    public static void demarrer(ExecutionPayload e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        victime = e.victime();
        scene = e.type();
        debut = mc.world.getTime();
        a = new Vec3d(e.x(), e.y(), e.z());
        lacet = e.yaw();
        float r = lacet * MathHelper.RADIANS_PER_DEGREE;
        fx = -MathHelper.sin(r); fz = MathHelper.cos(r);
        Vec3d d = mc.player.getPos().subtract(a);
        pf = d.x * fx + d.z * fz;
        py = d.y;
    }

    public static int scene() { return scene; }

    /** Où en est la scène (de 0 à 1 ; au-delà, elle est finie). */
    public static double k(float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return 2;
        return (mc.world.getTime() + td - debut) / Choregraphie.DUREE[scene];
    }

    /** Cette créature est-elle la victime d'une scène en cours (ou qui vient de finir : on la garde à terre) ? */
    public static boolean victime(Entity e) {
        return e.getId() == victime && k(0) < 1 + 45.0 / Choregraphie.DUREE[scene];
    }

    /** Un point du repère de la scène, dans le monde. */
    public static Vec3d monde(double f, double s, double y) {
        return new Vec3d(a.x + fx * f - fz * s, a.y + y, a.z + fz * f + fx * s);
    }

    public static float lacet() { return lacet; }

    /** La cible du joueur, à cet instant. */
    public static Vec3d posJoueur(double k) {
        double[] q = Choregraphie.joueur(scene, Math.min(1, k), pf, py);
        return monde(q[0], q[1], q[2]);
    }

    /** Où dessiner la victime ; après la chute libre du rebord, là où elle est vraiment. */
    public static Vec3d posVictime(double k, Vec3d reelle) {
        if (scene == Choregraphie.REBORD && k >= Choregraphie.CHUTE_LIBRE) return reelle;
        double[] q = Choregraphie.victime(scene, Math.min(1, k), pf, py);
        return monde(q[0], q[1], q[2]);
    }

    public static float lacetVictime(double k) { return lacet + Choregraphie.corps(scene, Math.min(1, k))[1]; }
    public static float bascule(double k) { return Choregraphie.corps(scene, Math.min(1, k))[0]; }

    // ───────────────────────── les poses ─────────────────────────

    private static void zero(ModelPart p) { p.pitch = 0; p.yaw = 0; p.roll = 0; }
    private static float l(double a, double b, double k) { return (float) Choregraphie.lisse(a, b, k); }
    private static float mix(float a, float b, float u) { return a + (b - a) * u; }

    /** Les bras, les jambes et la tête de la victime. */
    public static void poseVictime(BipedEntityModel<?> m) {
        double k = Math.min(1, k(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true)));
        zero(m.rightArm); zero(m.leftArm); zero(m.rightLeg); zero(m.leftLeg); zero(m.head);
        float mort = l(Choregraphie.COUP[scene], Choregraphie.COUP[scene] + 0.12, k);
        float agite = (float) Math.sin(k * 45) * 0.45f;
        switch (scene) {
            case Choregraphie.DOS -> {
                float saisi = l(0.22, 0.34, k);
                m.rightArm.pitch = mix(-2.2f * saisi, -0.25f, mort); m.leftArm.pitch = mix(-2.0f * saisi, -0.2f, mort);
                m.rightArm.roll = 0.35f * saisi * (1 - mort); m.leftArm.roll = -0.35f * saisi * (1 - mort);
                m.head.pitch = mix(-0.5f * saisi, 0.4f, mort);
                m.rightLeg.pitch = -0.35f * saisi * (1 - mort); m.leftLeg.pitch = 0.2f * saisi * (1 - mort);
            }
            case Choregraphie.PROJECTION -> {
                float vol = l(0.24, 0.3, k) * (1 - l(0.54, 0.6, k)), sol = l(0.54, 0.62, k);
                m.rightArm.pitch = -2.6f * vol + agite * vol; m.leftArm.pitch = -2.6f * vol - agite * vol;
                m.rightArm.roll = 1.2f * sol * (1 - mort * 0.5f); m.leftArm.roll = -1.2f * sol * (1 - mort * 0.5f);
                m.rightLeg.pitch = 0.7f * vol; m.leftLeg.pitch = -0.7f * vol;
                m.rightLeg.roll = 0.15f * sol; m.leftLeg.roll = -0.15f * sol;
                m.head.pitch = -0.6f * vol;
            }
            case Choregraphie.AERIEN -> {
                float leve = l(0.28, 0.42, k) * (1 - l(0.48, 0.6, k)), sol = l(0.5, 0.66, k);
                m.head.pitch = -0.8f * leve;
                m.rightArm.pitch = -2.5f * leve; m.leftArm.pitch = -2.3f * leve;
                m.rightArm.roll = 1.3f * sol; m.leftArm.roll = -1.3f * sol;
                m.rightLeg.roll = 0.12f * sol; m.leftLeg.roll = -0.12f * sol;
            }
            case Choregraphie.CACHETTE -> {
                float surpris = l(0.2, 0.3, k), tire = l(0.3, 0.4, k) * (1 - mort);
                m.rightArm.pitch = -0.9f * surpris + (-0.9f + agite) * tire; m.leftArm.pitch = -0.9f * surpris + (-0.9f - agite) * tire;
                m.head.pitch = 0.5f * tire;
                m.rightLeg.pitch = -0.5f * tire; m.leftLeg.pitch = 0.4f * tire;
            }
            case Choregraphie.REBORD -> {
                float regarde = l(0.2, 0.32, k), chute = l(0.34, 0.45, k);
                m.head.pitch = 0.6f * regarde * (1 - chute);
                m.rightArm.pitch = (-2.8f + agite) * chute; m.leftArm.pitch = (-2.8f - agite) * chute;
                m.rightLeg.pitch = agite * chute; m.leftLeg.pitch = -agite * chute;
            }
            default -> {}
        }
    }

    /** Les bras et les jambes de l'Ombre. */
    public static void poseJoueur(BipedEntityModel<?> m) {
        double k = Math.min(1, k(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true)));
        zero(m.rightArm); zero(m.leftArm); zero(m.rightLeg); zero(m.leftLeg);
        float coup = l(Choregraphie.COUP[scene] - 0.08, Choregraphie.COUP[scene], k);
        switch (scene) {
            case Choregraphie.DOS -> {
                float main = l(0.18, 0.3, k), genou = l(0.6, 0.8, k);
                m.leftArm.pitch = mix(-1.9f * main, -1.0f, genou); m.leftArm.yaw = 0.45f * main;
                m.rightArm.pitch = mix(-2.9f * main, -1.0f, coup); m.rightArm.yaw = -0.3f * coup;
                m.rightLeg.pitch = -1.5f * genou; m.leftLeg.pitch = -0.15f * genou;
                if (k < 0.24) { float w = (float) Math.sin(k * 60) * 0.6f; m.rightLeg.pitch = w; m.leftLeg.pitch = -w; }
            }
            case Choregraphie.PROJECTION -> {
                float prise = l(0.14, 0.26, k), jet = l(0.28, 0.54, k), genou = l(0.7, 0.8, k), leve = l(0.72, 0.8, k);
                float bras = mix(-1.6f * prise, -3.0f, jet);
                m.rightArm.pitch = mix(bras, mix(-2.9f, -0.7f, coup), leve); m.leftArm.pitch = mix(bras, -1.2f, leve);
                m.rightLeg.pitch = mix(-0.4f * jet, -1.5f, genou); m.leftLeg.pitch = mix(0.4f * jet, -0.15f, genou);
            }
            case Choregraphie.AERIEN -> {
                float vol = 1 - l(0.46, 0.52, k), pose = l(0.48, 0.6, k);
                m.rightArm.pitch = mix(-3.0f, mix(-2.9f, -0.6f, coup), pose); m.leftArm.pitch = mix(-2.6f, -1.0f, pose);
                m.rightLeg.pitch = -1.2f * vol - 1.4f * pose; m.leftLeg.pitch = -0.8f * vol - 0.3f * pose;
            }
            case Choregraphie.CACHETTE -> {
                float tend = l(0.12, 0.3, k), tire = l(0.32, 0.68, k), leve = l(0.74, 0.8, k);
                float bras = mix(-1.8f * tend, -0.5f, tire);
                m.rightArm.pitch = mix(bras, mix(-2.7f, -0.9f, coup), leve); m.leftArm.pitch = bras;
                m.rightLeg.pitch = -0.9f; m.leftLeg.pitch = -0.5f;
            }
            case Choregraphie.REBORD -> {
                float monte = l(0.14, 0.32, k), tire = l(0.34, 0.6, k);
                m.leftArm.pitch = -3.0f; m.leftArm.roll = -0.2f;
                m.rightArm.pitch = mix(-3.1f * monte, -1.1f, tire); m.rightArm.roll = 0.3f;
                float b = (float) Math.sin(k * 20) * 0.15f;
                m.rightLeg.pitch = b; m.leftLeg.pitch = -b;
            }
            default -> {}
        }
    }
}
