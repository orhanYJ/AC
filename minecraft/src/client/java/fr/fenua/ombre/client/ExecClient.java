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
        return e.getId() == victime && (k(0) < 1 + 45.0 / Choregraphie.DUREE[scene]
                || (e instanceof net.minecraft.entity.LivingEntity l && !l.isAlive() && k(0) < 1 + 1300.0 / Choregraphie.DUREE[scene]));
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
        if (Choregraphie.CHUTE[scene] >= 0 && k >= Choregraphie.CHUTE[scene]) return reelle;
        double[] q = Choregraphie.victime(scene, Math.min(1, k), pf, py);
        return monde(q[0], q[1], q[2]);
    }

    public static float lacetVictime(double k) { return lacet + Choregraphie.corps(scene, Math.min(1, k))[1]; }
    public static float bascule(double k) { return Choregraphie.corps(scene, Math.min(1, k))[0]; }

    // ───────────────────────── les poses ─────────────────────────

    private static void zero(ModelPart p) { p.pitch = 0; p.yaw = 0; p.roll = 0; }
    private static float l(double a, double b, double k) { return (float) Choregraphie.lisse(a, b, k); }
    private static float mix(float a, float b, float u) { return a + (b - a) * u; }

    /** Couché : les bras s'ouvrent sur le sol. */
    private static void bras(BipedEntityModel<?> m, float u) {
        if (u <= 0) return;
        m.rightArm.roll = mix(m.rightArm.roll, 1.25f, u); m.leftArm.roll = mix(m.leftArm.roll, -1.25f, u);
        m.rightLeg.roll = mix(m.rightLeg.roll, 0.12f, u); m.leftLeg.roll = mix(m.leftLeg.roll, -0.12f, u);
    }

    /** À genoux : les jambes repliées en arrière (le corps est abaissé par la chorégraphie). */
    private static void genoux(BipedEntityModel<?> m, float u) {
        m.rightLeg.pitch = 0.9f * u; m.leftLeg.pitch = 0.95f * u;
    }

    /** Les foulées de la course d'approche. */
    private static void course(BipedEntityModel<?> m, double k) {
        float w = (float) Math.sin(k * 55) * 0.9f;
        m.rightLeg.pitch = w; m.leftLeg.pitch = -w;
        m.rightArm.pitch = -w * 0.7f; m.leftArm.pitch = w * 0.7f;
    }

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
            case Choregraphie.BAILLON -> {
                float saisi = l(0.22, 0.34, k), couche = l(0.62, 0.9, k);
                m.rightArm.pitch = mix(-1.9f * saisi, 0, couche); m.leftArm.pitch = mix(-1.7f * saisi, 0, couche);
                m.rightArm.yaw = -0.5f * saisi * (1 - couche); m.leftArm.yaw = 0.5f * saisi * (1 - couche);
                bras(m, couche);
                m.head.pitch = -0.5f * saisi * (1 - mort);
                m.rightLeg.pitch = agite * 0.5f * saisi * (1 - mort); m.leftLeg.pitch = -agite * 0.5f * saisi * (1 - mort);
            }
            case Choregraphie.JARRET -> {
                float g = l(0.3, 0.4, k), h = l(0.64, 0.86, k);
                genoux(m, g * (1 - h));
                m.rightArm.pitch = -0.8f * g * (1 - h); m.leftArm.pitch = -0.6f * g * (1 - h);
                m.head.pitch = mix(-0.3f * g, 0.5f, mort) * (1 - h);
                bras(m, h);
            }
            case Choregraphie.FACE -> {
                float r = l(0.22, 0.4, k), h = l(0.6, 0.85, k);
                m.rightArm.pitch = mix(-0.7f * r, -1.3f, mort) * (1 - h); m.leftArm.pitch = mix(-0.6f * r, -1.2f, mort) * (1 - h);
                m.rightArm.yaw = -0.4f * mort * (1 - h); m.leftArm.yaw = 0.4f * mort * (1 - h);
                m.head.pitch = mix(-0.2f * r, 0.6f, mort);
                bras(m, h);
            }
            case Choregraphie.EPAULES, Choregraphie.BOND -> {
                double debut = scene == Choregraphie.EPAULES ? 0.42 : 0.4;
                float leve = l(debut - 0.16, debut - 0.04, k) * (1 - l(debut, debut + 0.08, k)), sol = l(debut, debut + 0.25, k);
                m.head.pitch = -0.8f * leve;
                m.rightArm.pitch = (-2.4f + agite) * leve + (-2.6f * sol) * (1 - mort); m.leftArm.pitch = (-2.2f - agite) * leve + (-2.6f * sol) * (1 - mort);
                bras(m, mort);
            }
            case Choregraphie.FOUDRE -> {
                float g = l(0.45, 0.58, k), h = l(0.66, 0.9, k);
                genoux(m, g * (1 - h));
                m.head.pitch = 0.5f * g * (1 - h);
                m.rightArm.pitch = -0.4f * g * (1 - h); m.leftArm.pitch = -0.3f * g * (1 - h);
                bras(m, h);
            }
            case Choregraphie.HERBES -> {
                float r = l(0.2, 0.34, k), g = l(0.52, 0.62, k), h = l(0.62, 0.88, k);
                genoux(m, g * (1 - h));
                m.rightArm.pitch = mix(-1.0f * r, -2.6f, h) * (1 - l(0.88, 1, k)); m.leftArm.pitch = mix(-0.8f * r, -2.6f, h) * (1 - l(0.88, 1, k));
                m.head.pitch = mix(-0.3f * r, 0.5f, mort);
            }
            case Choregraphie.CROC -> {
                float t = l(0.22, 0.38, k), d = l(0.4, 0.72, k);
                m.rightArm.pitch = mix(-2.6f * t, -3.0f + agite * (1 - mort), d); m.leftArm.pitch = mix(-2.6f * t, -3.0f - agite * (1 - mort), d);
                m.head.pitch = -0.5f * t * (1 - mort);
            }
            case Choregraphie.REBORD_LAME -> {
                float g = l(0.42, 0.56, k), h = l(0.56, 0.85, k);
                genoux(m, g * (1 - h));
                m.head.pitch = 0.6f * l(0.2, 0.32, k) * (1 - h);
                m.rightArm.pitch = -1.2f * mort * (1 - h); m.leftArm.pitch = -1.0f * mort * (1 - h);
                bras(m, h);
            }
            case Choregraphie.REBORD_VOL -> {
                float vol = l(0.28, 0.36, k);
                m.head.pitch = 0.6f * l(0.16, 0.28, k) * (1 - vol);
                m.rightArm.pitch = (-2.8f + agite) * vol; m.leftArm.pitch = (-2.8f - agite) * vol;
                m.rightLeg.pitch = agite * vol; m.leftLeg.pitch = -agite * vol;
            }
            case Choregraphie.TACLE -> {
                float choc = l(0.25, 0.4, k);
                m.rightArm.pitch = -2.8f * choc * (1 - mort); m.leftArm.pitch = -2.8f * choc * (1 - mort);
                m.head.pitch = -0.6f * choc * (1 - mort);
                bras(m, mort);
            }
            case Choregraphie.PASSAGE -> {
                float r = l(0.4, 0.6, k), h = l(0.55, 0.85, k);
                m.rightArm.roll = 1.1f * r * (1 - h); m.leftArm.roll = -1.1f * r * (1 - h);
                m.head.pitch = -0.5f * r * (1 - h);
                bras(m, h);
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
            case Choregraphie.BAILLON -> {
                float main = l(0.16, 0.28, k), couche = l(0.62, 0.9, k);
                m.leftArm.pitch = mix(-1.9f * main, -1.0f, couche); m.leftArm.yaw = 0.6f * main * (1 - couche);
                m.rightArm.pitch = mix(-0.5f * main, -1.3f, coup) * (1 - couche) - 0.8f * couche; m.rightArm.yaw = 0.5f * coup * (1 - couche);
                m.rightLeg.pitch = -1.5f * couche; m.leftLeg.pitch = -0.15f * couche;
                if (k < 0.2) course(m, k);
            }
            case Choregraphie.JARRET -> {
                float coupPied = l(0.22, 0.28, k) * (1 - l(0.32, 0.4, k)), leve = l(0.4, 0.5, k);
                m.rightLeg.pitch = -1.3f * coupPied;
                m.leftArm.pitch = -1.6f * l(0.3, 0.4, k);
                m.rightArm.pitch = mix(-3.0f * leve, -0.9f, coup);
                if (k < 0.22) course(m, k);
            }
            case Choregraphie.FACE -> {
                float prise = l(0.16, 0.26, k), pousse = l(0.56, 0.64, k) * (1 - l(0.75, 0.9, k));
                m.leftArm.pitch = mix(-1.5f * prise, -1.6f, pousse);
                m.rightArm.pitch = mix(-0.6f * prise, -1.5f, coup) * (1 - pousse) - 1.6f * pousse;
                if (k < 0.2) course(m, k);
            }
            case Choregraphie.EPAULES, Choregraphie.BOND -> {
                double pose = scene == Choregraphie.EPAULES ? 0.42 : 0.45;
                float vol = 1 - l(pose - 0.04, pose + 0.04, k), genou = l(pose + 0.03, pose + 0.25, k);
                m.rightArm.pitch = mix(-2.8f, mix(-2.9f, -0.6f, coup), genou); m.leftArm.pitch = mix(-2.5f, -1.0f, genou);
                m.rightLeg.pitch = -0.9f * vol - 1.4f * genou; m.leftLeg.pitch = -0.5f * vol - 0.2f * genou;
                if (scene == Choregraphie.BOND && k < 0.12) course(m, k);
            }
            case Choregraphie.FOUDRE -> {
                float vol = 1 - l(0.4, 0.46, k), frappe = l(0.44, 0.52, k);
                m.rightArm.pitch = mix(-3.0f, -0.8f, frappe); m.leftArm.pitch = mix(-3.0f, -0.9f, frappe);
                m.rightArm.yaw = 0.3f * frappe; m.leftArm.yaw = -0.3f * frappe;
                m.rightLeg.pitch = -0.8f * vol - 1.0f * (1 - vol); m.leftLeg.pitch = -0.4f * vol - 0.4f * (1 - vol);
            }
            case Choregraphie.HERBES -> {
                float sort = l(0.1, 0.3, k) * (1 - l(0.6, 0.85, k)), prise = l(0.3, 0.42, k);
                m.leftArm.pitch = -1.4f * prise * (1 - l(0.85, 1, k));
                m.rightArm.pitch = mix(-0.8f * sort, -1.5f, coup) * (1 - l(0.85, 1, k));
                m.rightLeg.pitch = -0.9f * (1 - sort) - 0.5f * sort; m.leftLeg.pitch = -0.5f * (1 - sort) + 0.3f * sort;
            }
            case Choregraphie.CROC -> {
                float tend = l(0.12, 0.22, k), tire = l(0.4, 0.72, k), leve = l(0.72, 0.78, k);
                float bras = mix(-1.4f * tend, -0.6f, tire);
                m.leftArm.pitch = bras; m.rightArm.pitch = mix(bras, mix(-2.8f, -0.9f, coup), leve);
                m.rightLeg.pitch = -0.9f; m.leftLeg.pitch = -0.5f;
            }
            case Choregraphie.REBORD_LAME -> {
                float monte = l(0.2, 0.3, k), frappe = l(0.3, 0.42, k);
                m.leftArm.pitch = -3.0f; m.leftArm.roll = -0.2f;
                m.rightArm.pitch = mix(-2.2f * monte, -3.1f, frappe) + 0.6f * l(0.5, 0.7, k); m.rightArm.roll = 0.15f;
                float b = (float) Math.sin(k * 20) * 0.12f;
                m.rightLeg.pitch = b; m.leftLeg.pitch = -b;
            }
            case Choregraphie.REBORD_VOL -> {
                float prise = l(0.16, 0.28, k), jet = l(0.3, 0.58, k);
                m.leftArm.pitch = -3.0f; m.leftArm.roll = -0.2f;
                m.rightArm.pitch = mix(-3.1f * prise, -0.4f, jet); m.rightArm.roll = 0.3f * (1 - jet);
                float b = (float) Math.sin(k * 20) * 0.15f;
                m.rightLeg.pitch = b + 0.4f * jet; m.leftLeg.pitch = -b + 0.2f * jet;
            }
            case Choregraphie.TACLE -> {
                float plonge = l(0.22, 0.3, k) * (1 - l(0.5, 0.58, k)), genou = l(0.52, 0.62, k);
                if (k < 0.25) course(m, k);
                else {
                    m.rightArm.pitch = mix(-2.6f * plonge, mix(-2.9f, -0.7f, coup), genou); m.leftArm.pitch = mix(-2.6f * plonge, -1.0f, genou);
                    m.rightLeg.pitch = 0.5f * plonge - 1.5f * genou; m.leftLeg.pitch = 0.3f * plonge - 0.15f * genou;
                }
            }
            case Choregraphie.PASSAGE -> {
                course(m, k);
                float lame = l(0.34, 0.46, k);
                m.rightArm.pitch = -1.4f; m.rightArm.yaw = mix(0.9f, -0.9f, lame); m.rightArm.roll = 0;
                m.leftArm.pitch = -0.4f; m.leftArm.yaw = -0.3f * lame;
            }
            default -> {}
        }
    }
}
