package fr.fenua.ombre.client;

import fr.fenua.ombre.bloc.OmbreBlocs;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Le « réalisateur » : pendant quelques secondes, il prend la caméra comme dans un film.
 *  - SYNCHRO : au point de vue, la caméra s'envole, tourne autour du joueur comme un aigle et redescend ;
 *    bandes noires de cinéma, cri de l'aigle, plumes, et la jauge « Synchronisation ».
 *  - SAUT : le saut de la foi, filmé de derrière et d'au-dessus ; le corps plonge bras en croix ;
 *    à l'arrivée, le foin vole.
 *  - EXEC : un assassinat, filmé de côté, la caméra tourne lentement autour de la scène.
 * La position de la caméra est appliquée par mixin/CameraMixin ; le corps qui bascule, par mixin/CorpsMixin.
 */
public final class Cinema {
    private Cinema() {}

    public enum Plan { AUCUN, SYNCHRO, SAUT, EXEC }

    private static Plan plan = Plan.AUCUN;
    private static int t, duree, fin, chute;
    private static Vec3d centre = Vec3d.ZERO, cible = Vec3d.ZERO;
    private static float yaw0;
    private static Perspective avant;

    public static boolean actif() { return plan != Plan.AUCUN; }
    public static Plan plan() { return plan; }

    // ───────────────────────── les débuts ─────────────────────────

    private static void commencer(Plan p, int d) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (plan == Plan.AUCUN) avant = mc.options.getPerspective();
        plan = p; t = 0; duree = d; fin = 0; chute = 0;
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    /** Le point de vue vient d'être synchronisé. */
    public static void synchro(ClientPlayerEntity p) {
        commencer(Plan.SYNCHRO, 110);
        centre = p.getEyePos();
        yaw0 = p.getYaw();
        p.playSound(SoundEvents.ENTITY_PHANTOM_AMBIENT, 0.9f, 1.45f);          // le cri de l'aigle
        p.playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 1f, 0.6f);
        p.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.5f, 1.4f);
    }

    /** Le joueur s'élance dans le vide, une meule ou l'eau en contrebas. */
    public static void saut(ClientPlayerEntity p) {
        commencer(Plan.SAUT, 200);
        yaw0 = p.getYaw();
        p.playSound(SoundEvents.ENTITY_PHANTOM_AMBIENT, 0.7f, 1.6f);
        p.playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 1f, 0.9f);
    }

    /** Un assassinat se joue : on filme de côté le joueur et sa victime. */
    public static void execution(ClientPlayerEntity p, Vec3d victime, int d) {
        commencer(Plan.EXEC, d);
        cible = victime;
        double dx = victime.x - p.getX(), dz = victime.z - p.getZ();
        yaw0 = (dx * dx + dz * dz > 0.01 ? (float) Math.toDegrees(Math.atan2(-dx, dz)) : p.getYaw()) + 90f;
        centre = p.getPos().add(victime).multiply(0.5).add(0, 1.1, 0);
    }

    private static void finir() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (avant != null) mc.options.setPerspective(avant);
        avant = null;
        plan = Plan.AUCUN;
    }

    // ───────────────────────── chaque tick ─────────────────────────

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (plan == Plan.AUCUN || p == null) return;
        t++;
        switch (plan) {
            case SYNCHRO -> {
                // Des plumes qui montent en spirale autour du joueur
                double a = t * 0.35, r = 1.2 + t * 0.02;
                p.getWorld().addParticle(ParticleTypes.END_ROD, centre.x + Math.cos(a) * r, centre.y - 1 + t * 0.05, centre.z + Math.sin(a) * r, 0, 0.02, 0);
                p.getWorld().addParticle(ParticleTypes.CLOUD, centre.x - Math.cos(a) * r, centre.y - 1.2 + t * 0.04, centre.z - Math.sin(a) * r, 0, 0.01, 0);
                if (t == 40) p.playSound(SoundEvents.ENTITY_PHANTOM_AMBIENT, 0.6f, 1.3f);
                if (t == 78) p.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
                if (t >= duree) finir();
            }
            case SAUT -> {
                boolean arrive = p.isOnGround() || p.isTouchingWater()
                        || p.getWorld().getBlockState(p.getBlockPos()).isOf(OmbreBlocs.MEULE)
                        || p.getWorld().getBlockState(p.getBlockPos()).isOf(OmbreBlocs.CHARRETTE);
                if (!arrive) chute++;
                if (arrive && fin == 0 && t > 3) {
                    fin = 16;
                    atterrissage(p);
                }
                if (fin > 0 && --fin == 0) finir();
                else if (t >= duree) finir();
            }
            case EXEC -> { if (t >= duree) finir(); }
            default -> {}
        }
    }

    /** L'arrivée du saut de la foi : le foin (ou l'eau) jaillit. */
    private static void atterrissage(ClientPlayerEntity p) {
        var w = p.getWorld();
        boolean eau = p.isTouchingWater();
        for (int i = 0; i < 50; i++) {
            double dx = (w.random.nextDouble() - 0.5) * 2.2, dz = (w.random.nextDouble() - 0.5) * 2.2;
            if (eau) w.addParticle(ParticleTypes.SPLASH, p.getX() + dx, p.getY() + 0.8, p.getZ() + dz, dx * 0.2, 0.4, dz * 0.2);
            else w.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.getDefaultState()),
                    p.getX() + dx, p.getY() + 0.8 + w.random.nextDouble(), p.getZ() + dz, dx * 0.3, 0.5, dz * 0.3);
        }
        for (int i = 0; i < 12; i++)
            w.addParticle(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), (w.random.nextDouble() - 0.5) * 0.3, 0.15, (w.random.nextDouble() - 0.5) * 0.3);
        p.playSound(eau ? SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED : SoundEvents.BLOCK_GRASS_BREAK, 1.2f, 0.7f);
        if (!eau) p.playSound(SoundEvents.BLOCK_AZALEA_LEAVES_FALL, 1f, 0.8f);
    }

    /** Le plongeon : de combien le corps bascule en avant (en degrés), pour mixin/CorpsMixin. */
    public static float bascule(float td) {
        if (plan != Plan.SAUT) return 0;
        if (fin > 0) return -Math.max(0, fin - 8) * 12f;          // il se redresse dans le foin
        return -Math.min(110f, (chute + td) * 7f);
    }

    // ───────────────────────── la caméra ─────────────────────────

    /** Où placer la caméra maintenant : { x, y, z, yaw, pitch }, ou null pour la laisser faire. */
    public static double[] camera(float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (plan == Plan.AUCUN || p == null) return null;
        double u = MathHelper.clamp((t + td) / duree, 0, 1);
        Vec3d oeil, regard;
        switch (plan) {
            case SYNCHRO -> {
                // L'aigle : on s'éloigne et on monte en tournant une fois autour, puis on redescend derrière lui
                double e = u * u * (3 - 2 * u);
                double a = Math.toRadians(yaw0 + 360 * e);
                double ouv = Math.sin(Math.PI * u);
                double r = 3 + 11 * ouv, h = 0.6 + 7 * ouv;
                oeil = centre.add(Math.sin(a) * r, h, -Math.cos(a) * r);
                regard = centre.add(0, -0.4, 0);
            }
            case SAUT -> {
                Vec3d corps = p.getLerpedPos(td).add(0, 0.9, 0);
                double a = Math.toRadians(yaw0);
                double recul = 3.2, haut = 2.4 + Math.min(2.0, chute * 0.05);
                oeil = corps.add(Math.sin(a) * recul, haut, -Math.cos(a) * recul);
                regard = corps.add(-Math.sin(a) * 1.5, -1.2, Math.cos(a) * 1.5);
            }
            case EXEC -> {
                double a = Math.toRadians(yaw0 + 40 * u);
                oeil = centre.add(Math.sin(a) * 3.4, 0.6 + 0.4 * u, -Math.cos(a) * 3.4);
                regard = centre;
            }
            default -> { return null; }
        }
        oeil = degager(p, regard, oeil);
        Vec3d d = regard.subtract(oeil);
        double hz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, hz));
        return new double[]{oeil.x, oeil.y, oeil.z, yaw, pitch};
    }

    /** La caméra ne traverse pas les murs : on l'arrête juste devant ce qui la cacherait. */
    private static Vec3d degager(ClientPlayerEntity p, Vec3d depuis, Vec3d vers) {
        HitResult h = p.getWorld().raycast(new RaycastContext(depuis, vers, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, p));
        if (h.getType() == HitResult.Type.MISS) return vers;
        Vec3d dir = vers.subtract(depuis).normalize();
        return h.getPos().subtract(dir.multiply(0.3));
    }

    // ───────────────────────── à l'écran ─────────────────────────

    /** Bandes noires de cinéma et, pour la synchronisation, la jauge. Dessiné par-dessus tout le reste. */
    public static void dessiner(DrawContext ctx, MinecraftClient mc) {
        if (plan == Plan.AUCUN || plan == Plan.EXEC && duree < 4) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float entree = Math.min(1f, t / 8f), sortie = plan == Plan.SAUT && fin > 0 ? fin / 16f : Math.min(1f, (duree - t) / 8f);
        int bande = (int) (h * 0.12f * Math.min(entree, sortie));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 500);                  // par-dessus la barre d'objets
        dessinerBandes(ctx, mc, w, h, bande);
        ctx.getMatrices().pop();
    }

    private static void dessinerBandes(DrawContext ctx, MinecraftClient mc, int w, int h, int bande) {
        ctx.fill(0, 0, w, bande, 0xFF000000);
        ctx.fill(0, h - bande, w, h, 0xFF000000);
        if (plan != Plan.SYNCHRO || bande < 10) return;
        var tr = mc.textRenderer;
        float k = MathHelper.clamp(t / 75f, 0, 1);
        int lx = w / 2 - 80, ly = h - bande / 2 - 2;
        String mot = k < 1 ? "S Y N C H R O N I S A T I O N" : "✦  S Y N C H R O N I S É  ✦";
        ctx.drawCenteredTextWithShadow(tr, Text.literal(mot), w / 2, ly - 12, k < 1 ? 0xFFD8D8D8 : 0xFFF2C23A);
        ctx.fill(lx, ly, lx + 160, ly + 3, 0x80404040);
        ctx.fill(lx, ly, lx + (int) (160 * k), ly + 3, k < 1 ? 0xFFFFFFFF : 0xFFF2C23A);
    }
}
